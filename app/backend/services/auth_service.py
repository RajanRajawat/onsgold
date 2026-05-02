import secrets
from datetime import datetime, timedelta, timezone

from fastapi import HTTPException, status
from pymongo.errors import DuplicateKeyError

from core.config import get_settings
from core.security import create_access_token, hash_password, verify_password
from db.mongo import get_admin_collection, get_otp_collection, utc_now
from models.auth import UserResponse, UserRole
from services.email_service import EmailDeliveryError, send_admin_action_otp, send_admin_credentials, send_password_reset_otp


def normalize_utc_datetime(value: datetime) -> datetime:
    if value.tzinfo is None:
        return value.replace(tzinfo=timezone.utc)
    return value.astimezone(timezone.utc)


def serialize_admin(document: dict) -> UserResponse:
    role = get_admin_role(document)
    created_at = document.get("created_at") or document.get("password_changed_at") or utc_now()
    updated_at = document.get("updated_at") or document.get("password_changed_at") or created_at
    return UserResponse(
        id=str(document["_id"]),
        name=document["name"],
        email=document["email"],
        role=role,
        is_active=document.get("is_active", True),
        created_at=created_at,
        updated_at=updated_at,
    )


def get_admin_role(document: dict) -> str:
    role = document.get("role")
    if role:
        return role
    roles = document.get("roles") or []
    if UserRole.super_admin.value in roles:
        return UserRole.super_admin.value
    return UserRole.admin.value


def get_admin_password_hash(document: dict) -> str | None:
    return document.get("password_hash") or document.get("password")


async def create_admin_account(*, name: str, email: str, password: str, role: UserRole) -> UserResponse:
    now = utc_now()
    payload = {
        "name": name,
        "email": email,
        "password_hash": hash_password(password),
        "role": role.value,
        "roles": [role.value],
        "is_active": True,
        "created_at": now,
        "updated_at": now,
    }
    try:
        result = await get_admin_collection().insert_one(payload)
    except DuplicateKeyError as exc:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Admin with this email already exists.") from exc
    payload["_id"] = result.inserted_id
    return serialize_admin(payload)


async def list_admin_accounts() -> list[UserResponse]:
    documents = await get_admin_collection().find(
        {
            "$or": [
                {"role": {"$in": [UserRole.super_admin.value, UserRole.admin.value]}},
                {"roles": {"$in": [UserRole.super_admin.value, UserRole.admin.value]}},
            ]
        }
    ).sort("created_at", -1).to_list(length=500)
    return [serialize_admin(document) for document in documents]


def generate_otp() -> str:
    return f"{secrets.randbelow(900000) + 100000}"


def generate_temporary_password() -> str:
    return f"OnsGold@{secrets.randbelow(900000) + 100000}{secrets.token_hex(3)}"


async def store_admin_otp(*, current_email: str, purpose: str, otp: str, payload: dict):
    settings = get_settings()
    now = utc_now()
    await get_otp_collection().delete_many({"email": current_email, "purpose": purpose})
    await get_otp_collection().insert_one(
        {
            "email": current_email,
            "purpose": purpose,
            "otp": otp,
            "payload": payload,
            "created_at": now,
            "expires_at": now + timedelta(minutes=settings.otp_expire_minutes),
        }
    )


async def get_valid_admin_otp(*, current_email: str, purpose: str, otp: str) -> dict:
    record = await get_otp_collection().find_one({"email": current_email, "purpose": purpose, "otp": otp})
    expires_at = normalize_utc_datetime(record["expires_at"]) if record else None
    if not record or expires_at < utc_now():
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid or expired OTP.")
    return record


async def request_admin_create_otp(*, current_email: str, name: str, email: str):
    existing = await get_admin_collection().find_one({"email": email})
    if existing:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Admin with this email already exists.")
    otp = generate_otp()
    await store_admin_otp(
        current_email=current_email,
        purpose="admin_create",
        otp=otp,
        payload={"name": name, "email": email},
    )
    try:
        await send_admin_action_otp(current_email, otp, "creating a new admin account")
    except EmailDeliveryError as exc:
        await get_otp_collection().delete_many({"email": current_email, "purpose": "admin_create"})
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Unable to send the admin creation OTP email right now.",
        ) from exc


async def confirm_admin_create(*, current_email: str, email: str, otp: str) -> UserResponse:
    record = await get_valid_admin_otp(current_email=current_email, purpose="admin_create", otp=otp)
    payload = record.get("payload") or {}
    if payload.get("email") != email:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="OTP does not match this admin email.")
    password = generate_temporary_password()
    admin = await create_admin_account(
        name=payload["name"],
        email=payload["email"],
        password=password,
        role=UserRole.admin,
    )
    try:
        await send_admin_credentials(payload["email"], payload["name"], password)
    except EmailDeliveryError as exc:
        await get_admin_collection().delete_one({"email": payload["email"]})
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Admin account email could not be delivered, so the account was not created.",
        ) from exc
    await get_otp_collection().delete_many({"email": current_email, "purpose": "admin_create"})
    return admin


async def request_admin_delete_otp(*, current_email: str, target_email: str):
    admin = await get_admin_collection().find_one({"email": target_email})
    if not admin:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Admin not found.")
    if admin["email"] == current_email:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="You cannot delete your own account.")
    otp = generate_otp()
    await store_admin_otp(
        current_email=current_email,
        purpose="admin_delete",
        otp=otp,
        payload={"target_email": target_email},
    )
    try:
        await send_admin_action_otp(current_email, otp, f"deleting admin account {target_email}")
    except EmailDeliveryError as exc:
        await get_otp_collection().delete_many({"email": current_email, "purpose": "admin_delete"})
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Unable to send the admin deletion OTP email right now.",
        ) from exc


async def confirm_admin_delete(*, current_email: str, target_email: str, otp: str):
    record = await get_valid_admin_otp(current_email=current_email, purpose="admin_delete", otp=otp)
    if (record.get("payload") or {}).get("target_email") != target_email:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="OTP does not match this admin email.")
    await delete_admin_account(target_email, current_email)
    await get_otp_collection().delete_many({"email": current_email, "purpose": "admin_delete"})


async def request_admin_update_otp(*, current_email: str, target_email: str, new_email: str | None, new_password: str | None):
    admin = await get_admin_collection().find_one({"email": target_email})
    if not admin:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Admin not found.")
    otp = generate_otp()
    await store_admin_otp(
        current_email=current_email,
        purpose="admin_update",
        otp=otp,
        payload={"target_email": target_email},
    )
    try:
        await send_admin_action_otp(current_email, otp, f"updating admin credentials for {target_email}")
    except EmailDeliveryError as exc:
        await get_otp_collection().delete_many({"email": current_email, "purpose": "admin_update"})
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Unable to send the admin update OTP email right now.",
        ) from exc


async def confirm_admin_update(
    *,
    current_email: str,
    target_email: str,
    otp: str,
    new_email: str | None,
    new_password: str | None,
) -> UserResponse:
    record = await get_valid_admin_otp(current_email=current_email, purpose="admin_update", otp=otp)
    payload = record.get("payload") or {}
    if payload.get("target_email") != target_email:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="OTP does not match this admin account.")
    if not new_email and not new_password:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Enter a new email or password.")
    admin = await update_admin_account(
        identifier=target_email,
        current_admin_email=current_email,
        email=new_email,
        password=new_password,
    )
    await get_otp_collection().delete_many({"email": current_email, "purpose": "admin_update"})
    return admin


async def get_admin_by_identifier(identifier: str) -> dict:
    document = await get_admin_collection().find_one({"$or": [{"_id": identifier}, {"email": identifier}]})
    if document:
        return document
    document = await get_admin_collection().find_one({"email": identifier})
    if not document:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Admin not found.")
    return document


async def update_admin_account(
    *,
    identifier: str,
    current_admin_email: str,
    name: str | None = None,
    email: str | None = None,
    password: str | None = None,
    role: UserRole | None = None,
    is_active: bool | None = None,
) -> UserResponse:
    admin = await get_admin_collection().find_one({"email": identifier}) or await get_admin_collection().find_one({"_id": identifier})
    if not admin:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Admin not found.")
    updates = {"updated_at": utc_now()}
    if name is not None:
        updates["name"] = name
    if email is not None and email != admin["email"]:
        existing = await get_admin_collection().find_one({"email": email})
        if existing and existing["_id"] != admin["_id"]:
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Admin with this email already exists.")
        updates["email"] = email
    if password:
        updates["password_hash"] = hash_password(password)
        updates["password"] = updates["password_hash"]
    if role is not None:
        if role == UserRole.super_admin and get_admin_role(admin) != UserRole.super_admin.value:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Super admin accounts can only be created from the bootstrap script.")
        updates["role"] = role.value
        updates["roles"] = [role.value]
    if is_active is not None:
        if admin["email"] == current_admin_email and is_active is False:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="You cannot deactivate your own account.")
        updates["is_active"] = is_active
    await get_admin_collection().update_one({"_id": admin["_id"]}, {"$set": updates})
    admin.update(updates)
    return serialize_admin(admin)


async def delete_admin_account(identifier: str, current_admin_email: str):
    admin = await get_admin_collection().find_one({"email": identifier}) or await get_admin_collection().find_one({"_id": identifier})
    if not admin:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Admin not found.")
    if admin["email"] == current_admin_email:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="You cannot delete your own account.")
    await get_admin_collection().delete_one({"_id": admin["_id"]})


async def update_own_profile(*, current_email: str, name: str, email: str) -> UserResponse:
    admin = await get_admin_collection().find_one({"email": current_email, "is_active": True})
    if not admin:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Admin not found.")
    if email != current_email:
        existing = await get_admin_collection().find_one({"email": email})
        if existing and existing["_id"] != admin["_id"]:
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Admin with this email already exists.")
    updates = {
        "name": name,
        "email": email,
        "updated_at": utc_now(),
    }
    await get_admin_collection().update_one({"_id": admin["_id"]}, {"$set": updates})
    admin.update(updates)
    return serialize_admin(admin)


async def change_own_password(*, current_email: str, current_password: str, new_password: str):
    admin = await get_admin_collection().find_one({"email": current_email, "is_active": True})
    if not admin:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Admin not found.")
    password_hash = get_admin_password_hash(admin)
    if not password_hash or not verify_password(current_password, password_hash):
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Current password is incorrect.")
    hashed_password = hash_password(new_password)
    await get_admin_collection().update_one(
        {"_id": admin["_id"]},
        {"$set": {"password_hash": hashed_password, "password": hashed_password, "updated_at": utc_now()}},
    )


async def change_own_email(*, current_email: str, new_email: str, current_password: str) -> UserResponse:
    admin = await get_admin_collection().find_one({"email": current_email, "is_active": True})
    if not admin:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Admin not found.")
    password_hash = get_admin_password_hash(admin)
    if not password_hash or not verify_password(current_password, password_hash):
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Current password is incorrect.")
    if new_email != current_email:
        existing = await get_admin_collection().find_one({"email": new_email})
        if existing and existing["_id"] != admin["_id"]:
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Admin with this email already exists.")
    updates = {"email": new_email, "updated_at": utc_now()}
    await get_admin_collection().update_one({"_id": admin["_id"]}, {"$set": updates})
    admin.update(updates)
    return serialize_admin(admin)


async def authenticate_admin(email: str, password: str):
    admin = await get_admin_collection().find_one({"email": email, "is_active": True})
    password_hash = get_admin_password_hash(admin or {})
    if not admin or not password_hash or not verify_password(password, password_hash):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid email or password.")
    token = create_access_token(subject=admin["email"], role=get_admin_role(admin))
    return token, serialize_admin(admin)


async def create_password_reset_otp(email: str):
    admin = await get_admin_collection().find_one({"email": email, "is_active": True})
    if not admin:
        return

    settings = get_settings()
    otp = f"{secrets.randbelow(900000) + 100000}"
    now = utc_now()
    expires_at = now + timedelta(minutes=settings.otp_expire_minutes)
    await get_otp_collection().delete_many({"email": email, "purpose": "forgot_password"})
    await get_otp_collection().insert_one(
        {
            "email": email,
            "purpose": "forgot_password",
            "otp": otp,
            "created_at": now,
            "expires_at": expires_at,
        }
    )
    try:
        await send_password_reset_otp(email, otp)
    except EmailDeliveryError as exc:
        await get_otp_collection().delete_many({"email": email, "purpose": "forgot_password"})
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Unable to send the password reset OTP email right now.",
        ) from exc


async def verify_password_reset_otp(email: str, otp: str):
    record = await get_otp_collection().find_one({"email": email, "purpose": "forgot_password", "otp": otp})
    expires_at = normalize_utc_datetime(record["expires_at"]) if record else None
    if not record or expires_at < utc_now():
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid or expired OTP.")


async def reset_password(email: str, otp: str, new_password: str):
    await verify_password_reset_otp(email, otp)
    hashed_password = hash_password(new_password)
    result = await get_admin_collection().update_one(
        {"email": email, "is_active": True},
        {"$set": {"password_hash": hashed_password, "password": hashed_password, "updated_at": utc_now()}},
    )
    if result.matched_count == 0:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Admin not found.")
    await get_otp_collection().delete_many({"email": email, "purpose": "forgot_password"})

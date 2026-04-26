import base64
import binascii
from html import escape

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, status
from fastapi.encoders import jsonable_encoder
from pydantic import BaseModel, EmailStr, Field, field_validator

from core.config import get_settings
from core.dependencies import require_roles
from models.auth import MessageResponse, UserRole, validate_password_strength
from models.dashboard import ActivityLogResponse, BugReportRequest
from services.activity_log_service import list_activity_logs, log_activity
from services.auth_service import (
    confirm_admin_create,
    confirm_admin_delete,
    confirm_admin_update,
    list_admin_accounts,
    request_admin_create_otp,
    request_admin_delete_otp,
    request_admin_update_otp,
)
from services.email_service import get_super_admin_emails, send_email_sync

router = APIRouter(prefix="/admin", tags=["Admin"])

BUG_REPORT_MAX_IMAGE_BYTES = 4 * 1024 * 1024
BUG_REPORT_ALLOWED_MIMES = {
    "image/png": "png",
    "image/jpeg": "jpg",
    "image/webp": "webp",
    "image/gif": "gif",
}


def normalize_email(value: str) -> str:
    return value.strip().lower()


class AdminRegisterRequest(BaseModel):
    name: str = Field(..., min_length=2, max_length=120)
    email: EmailStr
    otp: str | None = Field(default=None, min_length=6, max_length=6)

    @field_validator("name", mode="before")
    @classmethod
    def strip_name(cls, value):
        return str(value).strip()

    @field_validator("email", mode="before")
    @classmethod
    def clean_email(cls, value):
        return normalize_email(str(value))


class TargetEmailRequest(BaseModel):
    target_email: EmailStr

    @field_validator("target_email", mode="before")
    @classmethod
    def clean_target_email(cls, value):
        return normalize_email(str(value))


class DeleteAdminRequest(BaseModel):
    email: EmailStr
    otp: str = Field(..., min_length=6, max_length=6)

    @field_validator("email", mode="before")
    @classmethod
    def clean_email(cls, value):
        return normalize_email(str(value))


class UpdateAdminCredentialsRequest(TargetEmailRequest):
    otp: str = Field(..., min_length=6, max_length=6)
    new_email: EmailStr | None = None
    new_password: str | None = None

    @field_validator("new_email", mode="before")
    @classmethod
    def clean_new_email(cls, value):
        if value is None or value == "":
            return None
        return normalize_email(str(value))

    @field_validator("new_password", mode="before")
    @classmethod
    def clean_new_password(cls, value):
        if value is None:
            return None
        value = str(value)
        return value or None

    @field_validator("new_password")
    @classmethod
    def validate_new_password(cls, value):
        if value is None:
            return None
        return validate_password_strength(value)


def decode_bug_screenshot(report: BugReportRequest) -> dict | None:
    if not report.image_base64 or not report.image_mime:
        return None
    extension = BUG_REPORT_ALLOWED_MIMES.get(report.image_mime)
    if not extension:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Unsupported screenshot format. Use PNG, JPG, WEBP, or GIF.",
        )
    try:
        content = base64.b64decode(report.image_base64, validate=True)
    except (binascii.Error, ValueError) as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid screenshot encoding. Please reattach the image.",
        ) from exc
    if not content:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Screenshot attachment is empty. Please reattach the image.",
        )
    if len(content) > BUG_REPORT_MAX_IMAGE_BYTES:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Screenshot attachment is too large. Max 4 MB.",
        )
    return {
        "filename": f"ons-gold-bug-screenshot.{extension}",
        "content": content,
        "mime_type": report.image_mime,
    }


@router.post("/request-register-otp", response_model=MessageResponse)
async def request_register_otp(
    payload: AdminRegisterRequest,
    current_admin: dict = Depends(require_roles([UserRole.super_admin.value])),
):
    await request_admin_create_otp(
        current_email=current_admin["email"],
        name=payload.name,
        email=str(payload.email),
    )
    return MessageResponse(message="OTP sent to your registered email address.")


@router.post("/register", response_model=MessageResponse, status_code=status.HTTP_201_CREATED)
async def register_admin(
    payload: AdminRegisterRequest,
    current_admin: dict = Depends(require_roles([UserRole.super_admin.value])),
):
    if not payload.otp:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Please enter the OTP sent to your email.")
    await confirm_admin_create(
        current_email=current_admin["email"],
        email=str(payload.email),
        otp=payload.otp,
    )
    await log_activity(
        action="ADMIN_CREATED",
        performed_by_email=current_admin.get("email", ""),
        performed_by_name=current_admin.get("name", ""),
        target=str(payload.email),
        detail=f"Admin account created for {payload.name}.",
        new_value={"name": payload.name, "email": str(payload.email)},
    )
    return MessageResponse(message=f"{payload.name} registered successfully. Credentials sent via email.")


@router.get("/all-admins")
async def all_admins(_: dict = Depends(require_roles([UserRole.super_admin.value]))):
    admins = []
    for admin in await list_admin_accounts():
        role = admin.role.value if hasattr(admin.role, "value") else str(admin.role)
        admins.append(
            {
                "_id": admin.id,
                "id": admin.id,
                "name": admin.name,
                "email": str(admin.email),
                "role": role,
                "roles": ["super_admin", "admin"] if role == UserRole.super_admin.value else ["admin"],
                "is_active": admin.is_active,
                "created_at": admin.created_at,
                "updated_at": admin.updated_at,
            }
        )
    return jsonable_encoder({"message": "Admins fetched successfully", "data": admins})


@router.post("/request-delete-otp", response_model=MessageResponse)
async def request_delete_otp(
    payload: TargetEmailRequest,
    current_admin: dict = Depends(require_roles([UserRole.super_admin.value])),
):
    await request_admin_delete_otp(current_email=current_admin["email"], target_email=str(payload.target_email))
    return MessageResponse(message="OTP sent to your email to confirm deletion.")


@router.post("/delete-admin", response_model=MessageResponse)
async def delete_admin(
    payload: DeleteAdminRequest,
    current_admin: dict = Depends(require_roles([UserRole.super_admin.value])),
):
    await confirm_admin_delete(
        current_email=current_admin["email"],
        target_email=str(payload.email),
        otp=payload.otp,
    )
    await log_activity(
        action="ADMIN_DELETED",
        performed_by_email=current_admin.get("email", ""),
        performed_by_name=current_admin.get("name", ""),
        target=str(payload.email),
        detail=f"Admin account deleted: {payload.email}.",
    )
    return MessageResponse(message=f"{payload.email} has been permanently deleted.")


@router.post("/request-edit-otp", response_model=MessageResponse)
async def request_edit_otp(
    payload: TargetEmailRequest,
    current_admin: dict = Depends(require_roles([UserRole.super_admin.value])),
):
    await request_admin_update_otp(
        current_email=current_admin["email"],
        target_email=str(payload.target_email),
        new_email=None,
        new_password=None,
    )
    return MessageResponse(message="OTP sent to your registered email address.")


@router.post("/update-admin-credentials", response_model=MessageResponse)
async def update_admin_credentials(
    payload: UpdateAdminCredentialsRequest,
    current_admin: dict = Depends(require_roles([UserRole.super_admin.value])),
):
    await confirm_admin_update(
        current_email=current_admin["email"],
        target_email=str(payload.target_email),
        otp=payload.otp,
        new_email=str(payload.new_email) if payload.new_email else None,
        new_password=payload.new_password,
    )
    await log_activity(
        action="ADMIN_CREDENTIALS_UPDATED",
        performed_by_email=current_admin.get("email", ""),
        performed_by_name=current_admin.get("name", ""),
        target=str(payload.new_email or payload.target_email),
        detail=f"Admin credentials updated for {payload.target_email}.",
        new_value={"new_email": str(payload.new_email) if payload.new_email else None, "password_changed": bool(payload.new_password)},
    )
    return MessageResponse(message="Admin credentials updated successfully.")


@router.get("/activity-logs", response_model=list[ActivityLogResponse])
async def get_activity_logs(
    filter_action: str | None = None,
    _: dict = Depends(require_roles([UserRole.super_admin.value])),
):
    return await list_activity_logs(filter_action=filter_action)


@router.post("/report-bug", response_model=MessageResponse)
async def report_bug(
    report: BugReportRequest,
    background_tasks: BackgroundTasks,
    current_admin: dict = Depends(require_roles([UserRole.super_admin.value, UserRole.admin.value])),
):
    settings = get_settings()
    recipients = sorted(set(await get_super_admin_emails() + settings.bug_report_recipients))
    attachment = decode_bug_screenshot(report)
    attachments = [attachment] if attachment else None
    severity_colors = {"Urgent": "#7c3aed", "High": "#dc2626", "Medium": "#d97706", "Low": "#16a34a"}
    severity_color = severity_colors.get(report.severity, "#64748b")
    screenshot_note = "\nScreenshot: attached to this email." if attachment else "\nScreenshot: not attached."
    body = (
        "A new bug report has been submitted via the ONS Gold Admin Portal.\n\n"
        f"Title: {escape(report.title)}\n"
        f"Severity: {escape(report.severity)} ({severity_color})\n"
        f"Reported By: {escape(current_admin.get('name', 'Admin'))} ({escape(current_admin.get('email', ''))})\n\n"
        "Description:\n"
        f"{escape(report.description)}"
        f"{screenshot_note}"
    )
    if recipients:
        background_tasks.add_task(
            send_email_sync,
            recipients=recipients,
            subject=f"[Bug Report] {report.title} - Severity: {report.severity}",
            body=body,
            attachments=attachments,
        )
    await log_activity(
        action="BUG_REPORTED",
        performed_by_email=current_admin.get("email", ""),
        performed_by_name=current_admin.get("name", ""),
        target=report.title,
        detail=f"Bug report submitted with severity {report.severity}.",
        extra={"title": report.title, "severity": report.severity, "has_screenshot": bool(attachment)},
    )
    return MessageResponse(message="Bug report submitted successfully. The team has been notified.")

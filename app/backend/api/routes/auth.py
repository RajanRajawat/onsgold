from fastapi import APIRouter, Depends, status

from core.dependencies import get_current_admin, require_roles
from models.auth import (
    ChangeOwnEmailRequest,
    ChangeOwnPasswordRequest,
    ConfirmAdminCreateRequest,
    ConfirmAdminDeleteRequest,
    ConfirmAdminUpdateRequest,
    ForgotPasswordRequest,
    LoginRequest,
    MessageResponse,
    RequestAdminCreateOtpRequest,
    RequestAdminDeleteOtpRequest,
    RequestAdminUpdateOtpRequest,
    ResetPasswordRequest,
    TokenResponse,
    UpdateOwnProfileRequest,
    UserRole,
    UserResponse,
    VerifyOtpRequest,
)
from services.activity_log_service import log_activity
from services.auth_service import (
    authenticate_admin,
    change_own_email,
    change_own_password,
    confirm_admin_create,
    confirm_admin_delete,
    confirm_admin_update,
    create_password_reset_otp,
    list_admin_accounts,
    request_admin_create_otp,
    request_admin_delete_otp,
    request_admin_update_otp,
    reset_password,
    serialize_admin,
    update_own_profile,
    verify_password_reset_otp,
)

router = APIRouter(prefix="/auth", tags=["Authentication"])


@router.post("/login", response_model=TokenResponse)
async def login(payload: LoginRequest):
    token, user = await authenticate_admin(payload.email, payload.password)
    role_value = user.role.value if hasattr(user.role, "value") else str(user.role)
    action = "SUPER_ADMIN_LOGIN" if role_value == UserRole.super_admin.value else "ADMIN_LOGIN"
    actor_name = user.name
    actor_email = str(user.email)
    await log_activity(
        action=action,
        performed_by_email=actor_email,
        performed_by_name=actor_name,
        target="-",
        detail=f"{'Super Admin' if action == 'SUPER_ADMIN_LOGIN' else 'Admin'} logged in: {actor_email}.",
    )
    return TokenResponse(access_token=token, user=user)


@router.get("/me", response_model=UserResponse)
async def me(current_admin=Depends(get_current_admin)):
    return serialize_admin(current_admin)


@router.post("/admins/request-create-otp", response_model=MessageResponse)
async def request_create_admin_otp(
    payload: RequestAdminCreateOtpRequest,
    current_admin=Depends(require_roles([UserRole.super_admin.value])),
):
    await request_admin_create_otp(
        current_email=current_admin["email"],
        name=payload.name,
        email=payload.email,
    )
    return MessageResponse(message="OTP sent to your super admin email.")


@router.post("/admins/confirm-create", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
async def confirm_create_admin(
    payload: ConfirmAdminCreateRequest,
    current_admin=Depends(require_roles([UserRole.super_admin.value])),
):
    return await confirm_admin_create(
        current_email=current_admin["email"],
        email=payload.email,
        otp=payload.otp,
    )


@router.get("/admins", response_model=list[UserResponse])
async def get_admins(
    _: dict = Depends(require_roles([UserRole.super_admin.value])),
):
    return await list_admin_accounts()


@router.post("/admins/request-delete-otp", response_model=MessageResponse)
async def request_delete_admin_otp(
    payload: RequestAdminDeleteOtpRequest,
    current_admin=Depends(require_roles([UserRole.super_admin.value])),
):
    await request_admin_delete_otp(current_email=current_admin["email"], target_email=payload.email)
    return MessageResponse(message="OTP sent to your super admin email.")


@router.post("/admins/confirm-delete", response_model=MessageResponse)
async def confirm_delete_admin(
    payload: ConfirmAdminDeleteRequest,
    current_admin=Depends(require_roles([UserRole.super_admin.value])),
):
    await confirm_admin_delete(
        current_email=current_admin["email"],
        target_email=payload.email,
        otp=payload.otp,
    )
    return MessageResponse(message="Admin deleted successfully.")


@router.post("/admins/request-update-otp", response_model=MessageResponse)
async def request_update_admin_otp(
    payload: RequestAdminUpdateOtpRequest,
    current_admin=Depends(require_roles([UserRole.super_admin.value])),
):
    await request_admin_update_otp(
        current_email=current_admin["email"],
        target_email=payload.target_email,
        new_email=payload.new_email,
        new_password=payload.new_password,
    )
    return MessageResponse(message="OTP sent to your super admin email.")


@router.post("/admins/confirm-update", response_model=UserResponse)
async def confirm_update_admin(
    payload: ConfirmAdminUpdateRequest,
    current_admin=Depends(require_roles([UserRole.super_admin.value])),
):
    return await confirm_admin_update(
        current_email=current_admin["email"],
        target_email=payload.target_email,
        otp=payload.otp,
        new_email=payload.new_email,
        new_password=payload.new_password,
    )


@router.patch("/me", response_model=UserResponse)
async def update_me(
    payload: UpdateOwnProfileRequest,
    current_admin=Depends(get_current_admin),
):
    updated = await update_own_profile(
        current_email=current_admin["email"],
        name=payload.name,
        email=payload.email,
    )
    await log_activity(
        action="PROFILE_UPDATED",
        performed_by_email=current_admin.get("email", ""),
        performed_by_name=current_admin.get("name", ""),
        target=payload.email,
        detail="Profile name/email updated.",
        old_value={"name": current_admin.get("name"), "email": current_admin.get("email")},
        new_value={"name": payload.name, "email": payload.email},
    )
    return updated


@router.patch("/me/password", response_model=MessageResponse)
async def update_my_password(
    payload: ChangeOwnPasswordRequest,
    current_admin=Depends(get_current_admin),
):
    await change_own_password(
        current_email=current_admin["email"],
        current_password=payload.current_password,
        new_password=payload.new_password,
    )
    await log_activity(
        action="PROFILE_PASSWORD_UPDATED",
        performed_by_email=current_admin.get("email", ""),
        performed_by_name=current_admin.get("name", ""),
        target=current_admin.get("email", ""),
        detail="Profile password updated.",
    )
    return MessageResponse(message="Password updated successfully.")


@router.patch("/me/email", response_model=UserResponse)
async def update_my_email(
    payload: ChangeOwnEmailRequest,
    current_admin=Depends(get_current_admin),
):
    updated = await change_own_email(
        current_email=current_admin["email"],
        new_email=payload.email,
        current_password=payload.current_password,
    )
    await log_activity(
        action="PROFILE_EMAIL_UPDATED",
        performed_by_email=current_admin.get("email", ""),
        performed_by_name=current_admin.get("name", ""),
        target=payload.email,
        detail="Profile email updated.",
        old_value={"email": current_admin.get("email")},
        new_value={"email": payload.email},
    )
    return updated


@router.post("/forgot-password/request", response_model=MessageResponse)
async def forgot_password_request(payload: ForgotPasswordRequest):
    await create_password_reset_otp(payload.email)
    return MessageResponse(message="If the account exists, an OTP has been sent to the email address.")


@router.post("/forgot-password/verify", response_model=MessageResponse)
async def forgot_password_verify(payload: VerifyOtpRequest):
    await verify_password_reset_otp(payload.email, payload.otp)
    return MessageResponse(message="OTP verified successfully.")


@router.post("/forgot-password/reset", response_model=MessageResponse)
async def forgot_password_reset(payload: ResetPasswordRequest):
    await reset_password(payload.email, payload.otp, payload.new_password)
    await log_activity(
        action="PASSWORD_RESET_COMPLETED",
        performed_by_email=payload.email,
        performed_by_name=payload.email,
        target=payload.email,
        detail="Password reset completed through forgot password flow.",
    )
    return MessageResponse(message="Password reset successful.")

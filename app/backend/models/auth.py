from datetime import datetime
from enum import Enum

from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_validator


class UserRole(str, Enum):
    super_admin = "super_admin"
    admin = "admin"


def normalize_email(value: str) -> str:
    return value.strip().lower()


def validate_password_strength(password: str) -> str:
    if len(password.encode("utf-8")) > 72:
        raise ValueError("Password must be 72 bytes or fewer.")
    if len(password) < 8:
        raise ValueError("Password must be at least 8 characters long.")
    if not any(char.isupper() for char in password):
        raise ValueError("Password must contain at least one uppercase letter.")
    if not any(char.islower() for char in password):
        raise ValueError("Password must contain at least one lowercase letter.")
    if not any(char.isdigit() for char in password):
        raise ValueError("Password must contain at least one number.")
    if not any(char in "!@#$%^&*()-_=+[]{}|;:',.<>?/" for char in password):
        raise ValueError("Password must contain at least one special character.")
    return password


class AdminBase(BaseModel):
    name: str = Field(..., min_length=2, max_length=120)
    email: EmailStr

    @field_validator("name", mode="before")
    @classmethod
    def strip_name(cls, value: str) -> str:
        return str(value).strip()

    @field_validator("email", mode="before")
    @classmethod
    def clean_email(cls, value: str) -> str:
        return normalize_email(value)


class CreateAdminRequest(AdminBase):
    password: str

    @field_validator("password")
    @classmethod
    def validate_password(cls, value: str) -> str:
        return validate_password_strength(value)


class UpdateAdminRequest(BaseModel):
    name: str | None = Field(default=None, min_length=2, max_length=120)
    email: EmailStr | None = None
    password: str | None = None
    role: UserRole | None = None
    is_active: bool | None = None

    @field_validator("name", mode="before")
    @classmethod
    def strip_optional_name(cls, value):
        if value is None:
            return None
        return str(value).strip()

    @field_validator("email", mode="before")
    @classmethod
    def clean_optional_email(cls, value):
        if value is None:
            return None
        return normalize_email(value)

    @field_validator("password")
    @classmethod
    def validate_optional_password(cls, value):
        if value is None or value == "":
            return None
        return validate_password_strength(value)


class RequestAdminCreateOtpRequest(AdminBase):
    pass


class ConfirmAdminCreateRequest(BaseModel):
    email: EmailStr
    otp: str = Field(..., min_length=6, max_length=6)

    @field_validator("email", mode="before")
    @classmethod
    def clean_email(cls, value: str) -> str:
        return normalize_email(value)


class RequestAdminDeleteOtpRequest(BaseModel):
    email: EmailStr

    @field_validator("email", mode="before")
    @classmethod
    def clean_email(cls, value: str) -> str:
        return normalize_email(value)


class ConfirmAdminDeleteRequest(ConfirmAdminCreateRequest):
    pass


class RequestAdminUpdateOtpRequest(BaseModel):
    target_email: EmailStr
    new_email: EmailStr | None = None
    new_password: str | None = None

    @field_validator("target_email", "new_email", mode="before")
    @classmethod
    def clean_optional_email(cls, value):
        if value is None:
            return None
        return normalize_email(value)

    @field_validator("new_password")
    @classmethod
    def validate_optional_new_password(cls, value):
        if value is None or value == "":
            return None
        return validate_password_strength(value)


class ConfirmAdminUpdateRequest(RequestAdminUpdateOtpRequest):
    otp: str = Field(..., min_length=6, max_length=6)


class UpdateOwnProfileRequest(BaseModel):
    name: str = Field(..., min_length=2, max_length=120)
    email: EmailStr

    @field_validator("name", mode="before")
    @classmethod
    def strip_name_value(cls, value):
        return str(value).strip()

    @field_validator("email", mode="before")
    @classmethod
    def clean_email_value(cls, value):
        return normalize_email(value)


class ChangeOwnPasswordRequest(BaseModel):
    current_password: str = Field(..., min_length=1, max_length=200)
    new_password: str

    @field_validator("new_password")
    @classmethod
    def validate_new_password(cls, value):
        return validate_password_strength(value)


class ChangeOwnEmailRequest(BaseModel):
    email: EmailStr
    current_password: str = Field(..., min_length=1, max_length=200)

    @field_validator("email", mode="before")
    @classmethod
    def clean_email_value(cls, value):
        return normalize_email(value)


class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=1, max_length=200)

    @field_validator("email", mode="before")
    @classmethod
    def clean_email(cls, value: str) -> str:
        return normalize_email(value)


class ForgotPasswordRequest(BaseModel):
    email: EmailStr

    @field_validator("email", mode="before")
    @classmethod
    def clean_email(cls, value: str) -> str:
        return normalize_email(value)


class VerifyOtpRequest(BaseModel):
    email: EmailStr
    otp: str = Field(..., min_length=6, max_length=6)

    @field_validator("email", mode="before")
    @classmethod
    def clean_email(cls, value: str) -> str:
        return normalize_email(value)


class ResetPasswordRequest(VerifyOtpRequest):
    new_password: str

    @field_validator("new_password")
    @classmethod
    def validate_password(cls, value: str) -> str:
        return validate_password_strength(value)


class UserResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    name: str
    email: EmailStr
    role: UserRole
    is_active: bool
    created_at: datetime
    updated_at: datetime


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserResponse


class MessageResponse(BaseModel):
    message: str

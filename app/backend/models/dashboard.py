from datetime import datetime
from typing import Any

from pydantic import BaseModel
from pydantic import Field, field_validator, model_validator


class AnalyticsResponse(BaseModel):
    total_products: int
    total_orders: int
    total_custom_requests: int
    new_orders: int


class ActivityLogResponse(BaseModel):
    id: str
    action: str
    performed_by_email: str | None = None
    performed_by_name: str | None = None
    target: str | None = None
    detail: str | None = None
    old_value: Any | None = None
    new_value: Any | None = None
    extra: Any | None = None
    created_at: datetime


class BugReportRequest(BaseModel):
    title: str = Field(..., min_length=3, max_length=200)
    severity: str = Field(..., pattern="^(High|Medium|Low|Urgent)$")
    description: str = Field(..., min_length=10, max_length=5000)
    image_base64: str | None = Field(default=None, max_length=5_600_000)
    image_mime: str | None = Field(default=None, max_length=50)

    @field_validator("title", "description", mode="before")
    @classmethod
    def strip_text(cls, value):
        return str(value).strip()

    @field_validator("image_mime", mode="before")
    @classmethod
    def normalize_mime(cls, value):
        if value is None:
            return None
        return str(value).strip().lower()

    @model_validator(mode="after")
    def validate_image_fields(self):
        if bool(self.image_base64) != bool(self.image_mime):
            raise ValueError("Screenshot payload is incomplete. Please reattach the image.")
        return self

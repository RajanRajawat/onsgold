from datetime import datetime
from enum import Enum

from pydantic import BaseModel, EmailStr, Field, field_validator

from core.security import sanitize_text


class InquiryStatus(str, Enum):
    new = "new"
    contacted = "contacted"
    quoted = "quoted"
    in_making = "in_making"
    closed = "closed"
    delivered = "delivered"


def normalize_inquiry_status_input(value: str | InquiryStatus) -> str:
    raw = str(value.value if isinstance(value, InquiryStatus) else value or "").strip().lower()
    raw = raw.replace("-", "_").replace(" ", "_")
    if raw == InquiryStatus.quoted.value:
        return InquiryStatus.in_making.value
    if raw in {
        InquiryStatus.new.value,
        InquiryStatus.contacted.value,
        InquiryStatus.in_making.value,
        InquiryStatus.closed.value,
        InquiryStatus.delivered.value,
    }:
        return raw
    return raw


class InquirySource(str, Enum):
    website = "website"
    whatsapp = "whatsapp"
    custom_order = "custom_order"


class InquiryProductInput(BaseModel):
    product_id: str = Field(..., min_length=3, max_length=40)
    quantity: int = Field(default=1, ge=1, le=999)


class OrderCreateRequest(BaseModel):
    customer_name: str | None = Field(default=None, max_length=120)
    phone: str | None = Field(default=None, max_length=20)
    notes: str | None = Field(default=None, max_length=1000)
    products: list[InquiryProductInput] = Field(..., min_length=1)
    inquiry_source: InquirySource = InquirySource.website

    @field_validator("customer_name", "notes", mode="before")
    @classmethod
    def sanitize_text_fields(cls, value):
        if value is None:
            return None
        return sanitize_text(value) or None

    @field_validator("phone", mode="before")
    @classmethod
    def clean_phone(cls, value: str | None) -> str | None:
        if value is None:
            return None
        cleaned = "".join(ch for ch in str(value) if ch.isdigit() or ch == "+")
        return cleaned or None


class OrderProductSnapshot(BaseModel):
    product_id: str
    title: str
    quantity: int
    price: float | None = None
    image: str | None = None


class OrderComment(BaseModel):
    comment: str = Field(..., min_length=1, max_length=2000)
    added_by: str
    added_by_name: str | None = None
    added_by_email: str | None = None
    created_at: datetime


class OrderResponse(BaseModel):
    id: str
    inquiry_id: str
    customer_name: str
    phone: str
    notes: str | None = None
    status: InquiryStatus
    inquiry_source: InquirySource
    products: list[OrderProductSnapshot]
    comments: list[OrderComment] = Field(default_factory=list)
    created_at: datetime
    updated_at: datetime
    whatsapp_url: str | None = None


class OrderStatusUpdate(BaseModel):
    status: InquiryStatus

    @field_validator("status", mode="before")
    @classmethod
    def normalize_status(cls, value):
        return normalize_inquiry_status_input(value)


class OrderCommentCreateRequest(BaseModel):
    comment: str = Field(..., min_length=1, max_length=2000)

    @field_validator("comment", mode="before")
    @classmethod
    def sanitize_comment(cls, value: str) -> str:
        return sanitize_text(value) or ""


class CustomRequestCreate(BaseModel):
    customer_name: str | None = Field(default=None, max_length=120)
    phone: str = Field(..., min_length=8, max_length=20)
    city: str = Field(..., min_length=2, max_length=100)
    jewelry_type: str = Field(..., min_length=2, max_length=100)
    budget: str = Field(..., min_length=2, max_length=80)
    description: str = Field(..., min_length=10, max_length=4000)
    purity: str = Field(..., min_length=2, max_length=40)
    image_urls: list[str] = Field(default_factory=list)
    inquiry_source: InquirySource = InquirySource.custom_order
    email: EmailStr | None = None

    @field_validator("customer_name", "city", "jewelry_type", "budget", "description", "purity", mode="before")
    @classmethod
    def sanitize_text_fields(cls, value: str | None) -> str | None:
        if value is None:
            return None
        return sanitize_text(value) or None

    @field_validator("phone", mode="before")
    @classmethod
    def clean_phone(cls, value: str) -> str:
        return "".join(ch for ch in str(value) if ch.isdigit() or ch == "+")


class CustomRequestResponse(BaseModel):
    id: str
    request_id: str
    customer_name: str
    phone: str
    city: str
    jewelry_type: str
    budget: str
    description: str
    purity: str
    image_urls: list[str]
    status: InquiryStatus
    comments: list[OrderComment] = Field(default_factory=list)
    created_at: datetime
    updated_at: datetime
    inquiry_source: InquirySource
    email: str | None = None
    whatsapp_url: str | None = None


class InquirySearchResponse(BaseModel):
    orders: list[OrderResponse]
    custom_requests: list[CustomRequestResponse]

import math
from datetime import datetime
from enum import Enum

from pydantic import BaseModel, Field, field_validator

from core.security import sanitize_text


class MetalType(str, Enum):
    gold = "gold"
    silver = "silver"


def _sanitize_optional_text(value):
    if value is None:
        return None
    sanitized = sanitize_text(value)
    return sanitized or None


class ProductBase(BaseModel):
    title: str = Field(..., min_length=2, max_length=160)
    category: str = Field(..., min_length=2, max_length=80)
    metal: MetalType | None = None
    description: str | None = Field(default=None, max_length=4000)
    purity: str | None = Field(default=None, min_length=2, max_length=40)
    weight: float | None = Field(default=None, gt=0)
    price: float | None = Field(default=None, ge=0)
    price_on_request: bool = False
    images: list[str] = Field(default_factory=list, min_length=1)

    @field_validator("title", "category", mode="before")
    @classmethod
    def sanitize_text_fields(cls, value: str) -> str:
        return sanitize_text(value) or ""

    @field_validator("description", "purity", mode="before")
    @classmethod
    def sanitize_optional_text_fields(cls, value):
        return _sanitize_optional_text(value)

    @field_validator("weight", mode="before")
    @classmethod
    def normalize_weight(cls, value):
        if value is None or value == "":
            return None
        try:
            weight = float(value)
        except (TypeError, ValueError) as exc:
            raise ValueError("Weight must be a valid number.") from exc
        if not math.isfinite(weight):
            raise ValueError("Weight must be a valid number.")
        return weight

    @field_validator("images")
    @classmethod
    def validate_images(cls, value: list[str]) -> list[str]:
        if not value:
            raise ValueError("At least one product image is required.")
        return value


class ProductCreate(ProductBase):
    product_id: str | None = None


class ProductUpdate(BaseModel):
    title: str | None = Field(default=None, min_length=2, max_length=160)
    category: str | None = Field(default=None, min_length=2, max_length=80)
    metal: MetalType | None = None
    description: str | None = Field(default=None, max_length=4000)
    purity: str | None = Field(default=None, min_length=2, max_length=40)
    weight: float | None = Field(default=None, gt=0)
    price: float | None = Field(default=None, ge=0)
    price_on_request: bool | None = None
    images: list[str] | None = None

    @field_validator("title", "category", mode="before")
    @classmethod
    def sanitize_optional_text(cls, value):
        if value is None:
            return None
        return sanitize_text(value)

    @field_validator("description", "purity", mode="before")
    @classmethod
    def sanitize_optional_text_fields(cls, value):
        return _sanitize_optional_text(value)

    @field_validator("weight", mode="before")
    @classmethod
    def normalize_optional_weight(cls, value):
        if value is None or value == "":
            return None
        try:
            weight = float(value)
        except (TypeError, ValueError) as exc:
            raise ValueError("Weight must be a valid number.") from exc
        if not math.isfinite(weight):
            raise ValueError("Weight must be a valid number.")
        return weight

    @field_validator("images")
    @classmethod
    def validate_optional_images(cls, value: list[str] | None) -> list[str] | None:
        if value is not None and not value:
            raise ValueError("At least one product image is required.")
        return value


class ProductResponse(ProductBase):
    id: str
    product_id: str
    slug: str
    created_at: datetime
    updated_at: datetime


class ProductListResponse(BaseModel):
    items: list[ProductResponse]
    total: int
    page: int
    page_size: int

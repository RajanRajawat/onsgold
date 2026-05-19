import asyncio
import csv
import io
import secrets
from typing import Any
from urllib.parse import quote

from fastapi import HTTPException, status

from core.config import get_settings
from core.logging import get_logger
from db.mongo import get_admin_collection, get_custom_request_collection, get_order_collection, get_product_collection, utc_now
from models.inquiry import (
    CustomRequestCreate,
    CustomRequestResponse,
    InquiryStatus,
    OrderComment,
    OrderCreateRequest,
    OrderCommentCreateRequest,
    OrderProductSnapshot,
    OrderResponse,
    normalize_inquiry_status_input,
)
from services.dashboard_service import invalidate_dashboard_cache
from services.email_service import notify_super_admins

logger = get_logger(__name__)
INQUIRY_ID_LENGTH = 6
INQUIRY_ID_MIN = 10 ** (INQUIRY_ID_LENGTH - 1)
INQUIRY_ID_RANGE = 9 * INQUIRY_ID_MIN


def normalize_inquiry_status(status_value: str | None) -> str:
    normalized = normalize_inquiry_status_input(status_value or InquiryStatus.new.value)
    if normalized in {
        InquiryStatus.new.value,
        InquiryStatus.contacted.value,
        InquiryStatus.in_making.value,
        InquiryStatus.closed.value,
        InquiryStatus.delivered.value,
    }:
        return normalized
    return InquiryStatus.new.value


def _schedule_notification(subject: str, body: str):
    task = asyncio.create_task(notify_super_admins(subject=subject, body=body))

    def _log_failure(completed_task: asyncio.Task):
        try:
            completed_task.result()
        except Exception:
            logger.exception("Background notification failed")

    task.add_done_callback(_log_failure)


async def _reference_exists(reference: str) -> bool:
    order = await get_order_collection().find_one({"inquiry_id": reference}, {"_id": 1})
    if order is not None:
        return True
    custom_request = await get_custom_request_collection().find_one({"request_id": reference}, {"_id": 1})
    return custom_request is not None


async def generate_inquiry_id() -> str:
    for _ in range(64):
        reference = f"{INQUIRY_ID_MIN + secrets.randbelow(INQUIRY_ID_RANGE):06d}"
        if not await _reference_exists(reference):
            return reference

    raise HTTPException(
        status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
        detail="Unable to generate a unique 6-digit order ID. Please try again.",
    )


def serialize_order_comment(comment: dict[str, Any], admin_name_lookup: dict[str, str] | None = None) -> OrderComment:
    legacy_added_by = str(comment.get("added_by") or "").strip()
    added_by_email = str(comment.get("added_by_email") or "").strip() or None
    if not added_by_email and "@" in legacy_added_by:
        added_by_email = legacy_added_by

    added_by_name = str(comment.get("added_by_name") or "").strip() or None
    if not added_by_name and added_by_email and admin_name_lookup:
        added_by_name = admin_name_lookup.get(added_by_email)

    display_name = added_by_name or (legacy_added_by if legacy_added_by and "@" not in legacy_added_by else "") or "Admin"
    return OrderComment(
        comment=comment["comment"],
        added_by=display_name,
        added_by_name=added_by_name,
        added_by_email=added_by_email,
        created_at=comment["created_at"],
    )


def serialize_order(
    document: dict,
    whatsapp_url: str | None = None,
    admin_name_lookup: dict[str, str] | None = None,
) -> OrderResponse:
    return OrderResponse(
        id=str(document["_id"]),
        inquiry_id=document["inquiry_id"],
        customer_name=document.get("customer_name") or "Name not provided",
        phone=document.get("phone") or "Name not provided",
        notes=document.get("notes"),
        status=normalize_inquiry_status(document.get("status")),
        inquiry_source=document["inquiry_source"],
        products=[OrderProductSnapshot(**item) for item in document["products"]],
        comments=[serialize_order_comment(item, admin_name_lookup) for item in document.get("comments", [])],
        created_at=document["created_at"],
        updated_at=document.get("updated_at", document["created_at"]),
        whatsapp_url=whatsapp_url,
    )


def serialize_custom_request(
    document: dict,
    whatsapp_url: str | None = None,
    admin_name_lookup: dict[str, str] | None = None,
) -> CustomRequestResponse:
    return CustomRequestResponse(
        id=str(document["_id"]),
        request_id=document["request_id"],
        customer_name=document.get("customer_name") or "Name not provided",
        phone=document["phone"],
        city=document["city"],
        jewelry_type=document["jewelry_type"],
        budget=document["budget"],
        description=document["description"],
        purity=document["purity"],
        image_urls=document.get("image_urls", []),
        status=normalize_inquiry_status(document.get("status")),
        comments=[serialize_order_comment(item, admin_name_lookup) for item in document.get("comments", [])],
        created_at=document["created_at"],
        updated_at=document.get("updated_at", document["created_at"]),
        inquiry_source=document["inquiry_source"],
        email=document.get("email"),
        whatsapp_url=whatsapp_url,
    )


async def get_comment_admin_name_lookup(documents: list[dict[str, Any]]) -> dict[str, str]:
    emails: set[str] = set()
    for document in documents:
        for comment in document.get("comments", []):
            added_by_email = str(comment.get("added_by_email") or "").strip()
            legacy_added_by = str(comment.get("added_by") or "").strip()
            if added_by_email:
                emails.add(added_by_email)
            elif "@" in legacy_added_by:
                emails.add(legacy_added_by)
    if not emails:
        return {}

    admins = await get_admin_collection().find(
        {"email": {"$in": sorted(emails)}},
        {"email": 1, "name": 1},
    ).to_list(length=len(emails))
    return {
        str(admin.get("email") or "").strip(): str(admin.get("name") or "").strip()
        for admin in admins
        if admin.get("email") and admin.get("name")
    }


def build_whatsapp_url(products: list[dict[str, Any]]) -> str:
    settings = get_settings()
    lines = ["Hey,", "", "I wanted to buy"]
    for product in products:
        title = str(product.get("title") or product.get("product_id") or "").strip()
        product_id = str(product.get("product_id") or "").strip()
        quantity = int(product.get("quantity") or 1)
        label = f"{title} - {product_id}"
        if quantity > 1:
            label = f"{title} x{quantity} - {product_id}"
        lines.append(label)
    lines.extend(["", "", "Thank you!"])
    return f"https://wa.me/{settings.whatsapp_number}?text={quote(chr(10).join(lines))}"


def build_custom_request_whatsapp_url(document: dict[str, Any]) -> str:
    settings = get_settings()
    lines = [
        "Hey, I wanted to place a custom order:",
        "",
        f"Request ID: {document['request_id']}",
        f"Name: {document['customer_name']}",
        f"Phone: {document['phone']}",
        f"City: {document['city']}",
        f"Jewelry Type: {document['jewelry_type']}",
        f"Budget: {document['budget']}",
        f"Preferred Metal / Purity: {document['purity']}",
        f"Description: {document['description']}",
    ]
    image_urls = list(document.get("image_urls", []))
    if image_urls:
        lines.extend(["", "Reference Images:"])
        lines.extend([f"{index + 1}. {url}" for index, url in enumerate(image_urls)])
    lines.extend(["", "Thank you!"])
    return f"https://wa.me/{settings.whatsapp_number}?text={quote(chr(10).join(lines))}"


async def create_order(payload: OrderCreateRequest) -> OrderResponse:
    requested_ids = [item.product_id for item in payload.products]
    products = await get_product_collection().find({"product_id": {"$in": requested_ids}}).to_list(length=len(requested_ids))
    product_map = {product["product_id"]: product for product in products}
    missing_ids = [product_id for product_id in requested_ids if product_id not in product_map]
    if missing_ids:
        missing = missing_ids[0]
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Product {missing} not found.",
        )

    snapshots: list[dict[str, Any]] = []
    for item in payload.products:
        product = product_map[item.product_id]
        snapshots.append(
            {
                "product_id": product["product_id"],
                "title": product["title"],
                "quantity": item.quantity,
                "price": product.get("price"),
                "image": product["images"][0] if product.get("images") else None,
            }
        )

    inquiry_id = await generate_inquiry_id()
    now = utc_now()
    customer_name = payload.customer_name or "Name not provided"
    phone = payload.phone or "Name not provided"
    should_persist = bool(payload.phone)
    document = {
        "inquiry_id": inquiry_id,
        "customer_name": customer_name,
        "phone": phone,
        "notes": payload.notes,
        "status": InquiryStatus.new.value,
        "inquiry_source": payload.inquiry_source.value,
        "products": snapshots,
        "comments": [],
        "created_at": now,
        "updated_at": now,
    }
    if should_persist:
        result = await get_order_collection().insert_one(document)
        document["_id"] = result.inserted_id
        invalidate_dashboard_cache()
    else:
        document["_id"] = inquiry_id

    body = "\n".join(
        [
            f"Inquiry ID: {inquiry_id}",
            f"Customer: {customer_name}",
            f"Phone: {phone}",
            f"Source: {payload.inquiry_source.value}",
            "Products:",
            *[f"- {item['title']} ({item['product_id']}) x{item['quantity']}" for item in snapshots],
            f"Notes: {payload.notes or '-'}",
            f"Created At: {now.isoformat()}",
        ]
    )
    _schedule_notification(subject=f"ONS Gold Inquiry {inquiry_id}", body=body)

    whatsapp_url = build_whatsapp_url(snapshots)
    return serialize_order(document, whatsapp_url=whatsapp_url)


async def create_custom_request(payload: CustomRequestCreate) -> CustomRequestResponse:
    request_id = await generate_inquiry_id()
    now = utc_now()
    document = payload.model_dump()
    customer_name = payload.customer_name or "Name not provided"
    document.update(
        {
            "request_id": request_id,
            "customer_name": customer_name,
            "status": InquiryStatus.new.value,
            "comments": [],
            "created_at": now,
            "updated_at": now,
            "inquiry_source": payload.inquiry_source.value,
        }
    )
    result = await get_custom_request_collection().insert_one(document)
    document["_id"] = result.inserted_id
    invalidate_dashboard_cache()

    body = "\n".join(
        [
            f"Request ID: {request_id}",
            f"Customer: {customer_name}",
            f"Phone: {payload.phone}",
            f"City: {payload.city}",
            f"Jewelry Type: {payload.jewelry_type}",
            f"Budget: {payload.budget}",
            f"Purity: {payload.purity}",
            f"Description: {payload.description}",
            f"Images: {', '.join(payload.image_urls) if payload.image_urls else '-'}",
            f"Created At: {now.isoformat()}",
        ]
    )
    _schedule_notification(subject=f"ONS Gold Custom Request {request_id}", body=body)
    whatsapp_url = build_custom_request_whatsapp_url(document)
    return serialize_custom_request(document, whatsapp_url=whatsapp_url)


async def list_orders(search: str | None = None):
    query: dict[str, Any] = {}
    if search:
        query["$or"] = [
            {"inquiry_id": {"$regex": search, "$options": "i"}},
            {"customer_name": {"$regex": search, "$options": "i"}},
            {"phone": {"$regex": search, "$options": "i"}},
            {"products.product_id": {"$regex": search, "$options": "i"}},
        ]
    documents = await get_order_collection().find(query).sort("created_at", -1).to_list(length=500)
    admin_name_lookup = await get_comment_admin_name_lookup(documents)
    return [serialize_order(item, admin_name_lookup=admin_name_lookup) for item in documents]


async def list_custom_requests(search: str | None = None):
    query: dict[str, Any] = {}
    if search:
        query["$or"] = [
            {"request_id": {"$regex": search, "$options": "i"}},
            {"customer_name": {"$regex": search, "$options": "i"}},
            {"phone": {"$regex": search, "$options": "i"}},
            {"city": {"$regex": search, "$options": "i"}},
        ]
    documents = await get_custom_request_collection().find(query).sort("created_at", -1).to_list(length=500)
    admin_name_lookup = await get_comment_admin_name_lookup(documents)
    return [serialize_custom_request(item, admin_name_lookup=admin_name_lookup) for item in documents]


async def update_order_status(inquiry_id: str, status_value: str):
    normalized_status = normalize_inquiry_status(status_value)
    result = await get_order_collection().update_one(
        {"inquiry_id": inquiry_id},
        {"$set": {"status": normalized_status, "updated_at": utc_now()}},
    )
    if result.matched_count == 0:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Inquiry not found.")
    invalidate_dashboard_cache()


async def update_custom_request_status(request_id: str, status_value: str):
    normalized_status = normalize_inquiry_status(status_value)
    result = await get_custom_request_collection().update_one(
        {"request_id": request_id},
        {"$set": {"status": normalized_status, "updated_at": utc_now()}},
    )
    if result.matched_count == 0:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Custom order request not found.")
    invalidate_dashboard_cache()


async def delete_order(inquiry_id: str):
    result = await get_order_collection().delete_one({"inquiry_id": inquiry_id})
    if result.deleted_count == 0:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Order not found.")
    invalidate_dashboard_cache()


async def delete_custom_request(request_id: str):
    result = await get_custom_request_collection().delete_one({"request_id": request_id})
    if result.deleted_count == 0:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Custom order request not found.")
    invalidate_dashboard_cache()


async def add_order_comment(inquiry_id: str, payload: OrderCommentCreateRequest, admin: dict):
    author_name = str(admin.get("name") or "").strip() or "Admin"
    author_email = str(admin.get("email") or "").strip() or None
    comment = {
        "comment": payload.comment,
        "added_by": author_name,
        "added_by_name": author_name,
        "added_by_email": author_email,
        "created_at": utc_now(),
    }
    result = await get_order_collection().update_one(
        {"inquiry_id": inquiry_id},
        {"$push": {"comments": comment}, "$set": {"updated_at": utc_now()}},
    )
    if result.matched_count == 0:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Order not found.")


async def add_custom_request_comment(request_id: str, payload: OrderCommentCreateRequest, admin: dict):
    author_name = str(admin.get("name") or "").strip() or "Admin"
    author_email = str(admin.get("email") or "").strip() or None
    comment = {
        "comment": payload.comment,
        "added_by": author_name,
        "added_by_name": author_name,
        "added_by_email": author_email,
        "created_at": utc_now(),
    }
    result = await get_custom_request_collection().update_one(
        {"request_id": request_id},
        {"$push": {"comments": comment}, "$set": {"updated_at": utc_now()}},
    )
    if result.matched_count == 0:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Custom order request not found.")


async def export_orders_csv() -> str:
    orders = await get_order_collection().find({}).sort("created_at", -1).to_list(length=1000)
    custom_requests = await get_custom_request_collection().find({}).sort("created_at", -1).to_list(length=1000)
    rows: list[dict[str, Any]] = []
    for item in orders:
        rows.append(
            {
                "order_id": item["inquiry_id"],
                "order_type": "catalog_order",
                "customer_name": item["customer_name"],
                "phone": item["phone"],
                "status": normalize_inquiry_status(item.get("status")),
                "inquiry_source": item["inquiry_source"],
                "products": ", ".join(
                    f'{product.get("title", product["product_id"])} ({product["product_id"]}) x{product["quantity"]}'
                    for product in item["products"]
                ),
                "custom_request": "",
                "created_at": item["created_at"],
            }
        )
    for item in custom_requests:
        rows.append(
            {
                "order_id": item["request_id"],
                "order_type": "custom_order",
                "customer_name": item["customer_name"],
                "phone": item["phone"],
                "status": normalize_inquiry_status(item.get("status")),
                "inquiry_source": item["inquiry_source"],
                "products": "",
                "custom_request": " | ".join(
                    [
                        f'Type: {item["jewelry_type"]}',
                        f'City: {item["city"]}',
                        f'Budget: {item["budget"]}',
                        f'Purity: {item["purity"]}',
                        f'Images: {len(item.get("image_urls", []))}',
                    ]
                ),
                "created_at": item["created_at"],
            }
        )
    rows.sort(key=lambda item: item["created_at"], reverse=True)
    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["order_id", "order_type", "customer_name", "phone", "status", "inquiry_source", "products", "custom_request", "created_at"])
    for item in rows:
        writer.writerow(
            [
                item["order_id"],
                item["order_type"],
                item["customer_name"],
                item["phone"],
                item["status"],
                item["inquiry_source"],
                item["products"],
                item["custom_request"],
                item["created_at"].isoformat(),
            ]
        )
    return output.getvalue()

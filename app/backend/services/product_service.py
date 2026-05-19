import random
import re
import secrets
import string
from time import monotonic
from typing import Any

from fastapi import HTTPException, status
from pymongo import ASCENDING, DESCENDING

from core.security import sanitize_text
from db.mongo import get_product_collection, utc_now
from models.product import ProductCreate, ProductResponse, ProductUpdate
from services.cloudinary_service import delete_images_by_urls

_RANDOM_PRODUCTS_CACHE_TTL_SECONDS = 30
_PRODUCT_LIST_CACHE_TTL_SECONDS = 20
_random_products_cache: dict[int, dict[str, float | list[ProductResponse] | int]] = {}
_product_list_cache: dict[tuple[Any, ...], dict[str, float | list[ProductResponse] | int]] = {}


def invalidate_product_caches():
    _random_products_cache.clear()
    _product_list_cache.clear()


def slugify(value: str) -> str:
    slug = re.sub(r"[^a-z0-9]+", "-", value.lower()).strip("-")
    return slug or "product"


def generate_product_id() -> str:
    alphabet = string.ascii_uppercase + string.digits
    return "P" + "".join(secrets.choice(alphabet) for _ in range(6))


def serialize_product(document: dict) -> ProductResponse:
    return ProductResponse(
        id=str(document["_id"]),
        product_id=document["product_id"],
        title=document["title"],
        category=document["category"],
        metal=document.get("metal"),
        description=document.get("description"),
        purity=document.get("purity"),
        weight=document.get("weight"),
        price=document.get("price"),
        price_on_request=document.get("price_on_request", False),
        images=document["images"],
        slug=document["slug"],
        created_at=document["created_at"],
        updated_at=document["updated_at"],
    )


async def create_product(payload: ProductCreate) -> ProductResponse:
    now = utc_now()
    title = sanitize_text(payload.title) or payload.title
    base_slug = slugify(title)
    slug = base_slug
    suffix = 1

    while await get_product_collection().find_one({"slug": slug}):
        suffix += 1
        slug = f"{base_slug}-{suffix}"

    product_id = payload.product_id
    if product_id:
        product_id = product_id.strip().upper()
        if not re.fullmatch(r"[A-Z0-9]{1,6}", product_id):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Product ID must be 1 to 6 uppercase letters or numbers.",
            )
        if await get_product_collection().find_one({"product_id": product_id}):
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Product ID already exists.")
    else:
        product_id = generate_product_id()
        while await get_product_collection().find_one({"product_id": product_id}):
            product_id = generate_product_id()

    document = payload.model_dump()
    document.update(
        {
            "title": title,
            "product_id": product_id,
            "slug": slug,
            "created_at": now,
            "updated_at": now,
        }
    )
    result = await get_product_collection().insert_one(document)
    document["_id"] = result.inserted_id
    invalidate_product_caches()
    return serialize_product(document)


async def get_product_or_404(identifier: str) -> dict:
    product = await get_product_collection().find_one(
        {"$or": [{"product_id": identifier}, {"slug": identifier}]}
    )
    if not product:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Product not found.")
    return product


async def update_product(identifier: str, payload: ProductUpdate) -> ProductResponse:
    product = await get_product_or_404(identifier)
    updates: dict[str, Any] = {k: v for k, v in payload.model_dump(exclude_unset=True).items()}
    if "title" in updates:
        updates["slug"] = slugify(updates["title"])
    unset_fields = {
        key: ""
        for key in ("metal", "description", "purity", "weight")
        if key in updates and updates[key] is None
    }
    for key in unset_fields:
        updates.pop(key, None)
    updates["updated_at"] = utc_now()
    unset_fields.update({"stock_status": "", "tags": ""})
    update_document: dict[str, Any] = {"$set": updates}
    if unset_fields:
        update_document["$unset"] = unset_fields
    await get_product_collection().update_one({"_id": product["_id"]}, update_document)
    product.update(updates)
    for key in unset_fields:
        product.pop(key, None)
    invalidate_product_caches()
    return serialize_product(product)


async def delete_product(identifier: str):
    product = await get_product_or_404(identifier)
    await delete_images_by_urls(product.get("images") or [])
    await get_product_collection().delete_one({"_id": product["_id"]})
    invalidate_product_caches()


async def list_products(
    *,
    category: str | None,
    metal: str | None,
    purity: str | None,
    min_weight: float | None,
    max_weight: float | None,
    min_price: float | None,
    max_price: float | None,
    search: str | None,
    latest: bool,
    sort: str,
    page: int,
    page_size: int,
):
    cache_key = (
        category,
        metal,
        purity,
        min_weight,
        max_weight,
        min_price,
        max_price,
        search,
        latest,
        sort,
        page,
        page_size,
    )
    cached = _product_list_cache.get(cache_key)
    now = monotonic()
    if cached and now < float(cached.get("expires_at") or 0.0):
        cached_items = cached.get("items") or []
        cached_total = int(cached.get("total") or 0)
        return cached_items, cached_total

    query: dict[str, Any] = {}
    if category:
        query["category"] = {"$regex": f"^{re.escape(category)}$", "$options": "i"}
    if metal:
        query["metal"] = {"$regex": f"^{re.escape(metal)}$", "$options": "i"}
    if purity:
        query["purity"] = {"$regex": f"^{re.escape(purity)}$", "$options": "i"}
    if min_weight is not None or max_weight is not None:
        query["weight"] = {}
        if min_weight is not None:
            query["weight"]["$gte"] = min_weight
        if max_weight is not None:
            query["weight"]["$lte"] = max_weight
    if min_price is not None or max_price is not None:
        query["price"] = {}
        if min_price is not None:
            query["price"]["$gte"] = min_price
        if max_price is not None:
            query["price"]["$lte"] = max_price
    if search:
        query["$or"] = [
            {"title": {"$regex": re.escape(search), "$options": "i"}},
            {"product_id": {"$regex": re.escape(search), "$options": "i"}},
            {"category": {"$regex": re.escape(search), "$options": "i"}},
        ]

    sort_options = {
        "latest": [("created_at", DESCENDING)],
        "title_asc": [("title", ASCENDING)],
        "title_desc": [("title", DESCENDING)],
        "weight_asc": [("weight", ASCENDING)],
        "weight_desc": [("weight", DESCENDING)],
        "price_asc": [("price", ASCENDING)],
        "price_desc": [("price", DESCENDING)],
    }
    sort_key = "latest" if latest else (sort or "latest")
    mongo_sort = sort_options.get(sort_key, sort_options["latest"])
    skip = (page - 1) * page_size
    total = await get_product_collection().count_documents(query)
    documents = await get_product_collection().find(query).sort(mongo_sort).skip(skip).limit(page_size).to_list(page_size)
    items = [serialize_product(item) for item in documents]
    _product_list_cache[cache_key] = {
        "items": items,
        "total": total,
        "expires_at": now + _PRODUCT_LIST_CACHE_TTL_SECONDS,
    }
    return items, total


async def list_random_products(*, limit: int):
    cache_key = limit
    cached = _random_products_cache.get(cache_key)
    now = monotonic()
    if cached and now < float(cached.get("expires_at") or 0.0):
        cached_items = cached.get("items") or []
        cached_total = int(cached.get("total") or 0)
        return cached_items, cached_total

    query: dict[str, Any] = {}

    total = await get_product_collection().count_documents(query)
    if total == 0:
        _random_products_cache[cache_key] = {
            "items": [],
            "total": 0,
            "expires_at": now + _RANDOM_PRODUCTS_CACHE_TTL_SECONDS,
        }
        return [], 0

    candidate_size = min(max(limit * 6, limit), 60)
    documents = await get_product_collection().find(query).sort("created_at", DESCENDING).limit(candidate_size).to_list(length=candidate_size)
    random.shuffle(documents)
    selected = documents[:limit]
    items = [serialize_product(item) for item in selected]
    _random_products_cache[cache_key] = {
        "items": items,
        "total": total,
        "expires_at": now + _RANDOM_PRODUCTS_CACHE_TTL_SECONDS,
    }
    return items, total

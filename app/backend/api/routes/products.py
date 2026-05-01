from fastapi import APIRouter, Depends, Query, status

from core.dependencies import require_roles
from models.auth import MessageResponse, UserRole
from models.product import ProductCreate, ProductListResponse, ProductResponse, ProductUpdate
from services.activity_log_service import log_activity
from services.dashboard_service import invalidate_dashboard_cache
from services.product_service import create_product, delete_product, get_product_or_404, list_products, list_random_products, serialize_product, update_product

router = APIRouter(prefix="/products", tags=["Products"])


@router.get("", response_model=ProductListResponse)
async def get_products(
    category: str | None = None,
    metal: str | None = None,
    purity: str | None = None,
    stock_status: str | None = None,
    min_weight: float | None = None,
    max_weight: float | None = None,
    min_price: float | None = None,
    max_price: float | None = None,
    search: str | None = None,
    latest: bool = False,
    sort: str = Query(default="featured"),
    featured: bool | None = None,
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=12, ge=1, le=25),
):
    items, total = await list_products(
        category=category,
        metal=metal,
        purity=purity,
        stock_status=stock_status,
        min_weight=min_weight,
        max_weight=max_weight,
        min_price=min_price,
        max_price=max_price,
        search=search,
        latest=latest,
        sort=sort,
        featured=featured,
        page=page,
        page_size=page_size,
    )
    return ProductListResponse(items=items, total=total, page=page, page_size=page_size)


@router.get("/random", response_model=ProductListResponse)
async def get_random_products(
    limit: int = Query(default=9, ge=1, le=18),
    featured: bool | None = None,
):
    items, total = await list_random_products(limit=limit, featured=featured)
    return ProductListResponse(items=items, total=total, page=1, page_size=limit)


@router.get("/{identifier}", response_model=ProductResponse)
async def get_product(identifier: str):
    product = await get_product_or_404(identifier)
    return serialize_product(product)


@router.post("", response_model=ProductResponse, status_code=status.HTTP_201_CREATED)
async def create_product_route(
    payload: ProductCreate,
    current_admin: dict = Depends(require_roles([UserRole.super_admin.value, UserRole.admin.value])),
):
    product = await create_product(payload)
    invalidate_dashboard_cache()
    await log_activity(
        action="PRODUCT_CREATED",
        performed_by_email=current_admin.get("email", ""),
        performed_by_name=current_admin.get("name", ""),
        target=product.product_id,
        detail=f"Product created: {product.title}.",
        new_value={"product_id": product.product_id, "title": product.title, "category": product.category},
    )
    return product


@router.put("/{identifier}", response_model=ProductResponse)
async def update_product_route(
    identifier: str,
    payload: ProductUpdate,
    current_admin: dict = Depends(require_roles([UserRole.super_admin.value, UserRole.admin.value])),
):
    before = serialize_product(await get_product_or_404(identifier))
    product = await update_product(identifier, payload)
    invalidate_dashboard_cache()
    await log_activity(
        action="PRODUCT_UPDATED",
        performed_by_email=current_admin.get("email", ""),
        performed_by_name=current_admin.get("name", ""),
        target=product.product_id,
        detail=f"Product updated: {product.title}.",
        old_value={"product_id": before.product_id, "title": before.title, "category": before.category, "stock_status": before.stock_status},
        new_value={"product_id": product.product_id, "title": product.title, "category": product.category, "stock_status": product.stock_status},
    )
    return product


@router.delete("/{identifier}", response_model=MessageResponse)
async def delete_product_route(
    identifier: str,
    current_admin: dict = Depends(require_roles([UserRole.super_admin.value, UserRole.admin.value])),
):
    before = serialize_product(await get_product_or_404(identifier))
    await delete_product(identifier)
    invalidate_dashboard_cache()
    await log_activity(
        action="PRODUCT_DELETED",
        performed_by_email=current_admin.get("email", ""),
        performed_by_name=current_admin.get("name", ""),
        target=before.product_id,
        detail=f"Product deleted: {before.title}.",
        old_value={"product_id": before.product_id, "title": before.title, "category": before.category},
    )
    return MessageResponse(message="Product deleted successfully.")

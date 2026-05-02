from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse

from core.dependencies import require_roles
from core.rate_limit import rate_limit
from models.auth import MessageResponse, UserRole
from models.inquiry import (
    CustomRequestCreate,
    CustomRequestResponse,
    OrderCommentCreateRequest,
    OrderCreateRequest,
    OrderResponse,
    OrderStatusUpdate,
)
from services.activity_log_service import log_activity
from services.dashboard_service import get_dashboard_analytics
from services.inquiry_service import (
    add_custom_request_comment,
    add_order_comment,
    create_custom_request,
    create_order,
    delete_custom_request,
    delete_order,
    export_orders_csv,
    list_custom_requests,
    list_orders,
    update_custom_request_status,
    update_order_status,
)

router = APIRouter(tags=["Inquiries"])


@router.post("/orders", response_model=OrderResponse)
async def create_order_route(
    payload: OrderCreateRequest,
    _: None = Depends(rate_limit("orders")),
):
    order = await create_order(payload)
    if payload.phone:
        await log_activity(
            action="CATALOG_ORDER_CREATED",
            performed_by_email=order.phone,
            performed_by_name=order.customer_name,
            target=order.inquiry_id,
            detail=f"Catalog order created by {order.customer_name}.",
            new_value={
                "customer_name": order.customer_name,
                "phone": order.phone,
                "products": [item.model_dump() for item in payload.products],
            },
        )
    return order


@router.post("/custom-requests", response_model=CustomRequestResponse)
async def create_custom_request_route(
    payload: CustomRequestCreate,
    _: None = Depends(rate_limit("custom_requests")),
):
    request = await create_custom_request(payload)
    await log_activity(
        action="CUSTOM_ORDER_CREATED",
        performed_by_email=request.phone,
        performed_by_name=request.customer_name,
        target=request.request_id,
        detail=f"Custom order created by {request.customer_name}.",
        new_value={
            "customer_name": request.customer_name,
            "phone": request.phone,
            "city": request.city,
            "jewelry_type": request.jewelry_type,
            "image_count": len(payload.image_urls),
        },
    )
    return request


@router.get("/admin/orders", response_model=list[OrderResponse])
async def get_orders_route(
    search: str | None = None,
    _: dict = Depends(require_roles([UserRole.super_admin.value, UserRole.admin.value])),
):
    return await list_orders(search=search)


@router.get("/admin/custom-requests", response_model=list[CustomRequestResponse])
async def get_custom_requests_route(
    search: str | None = None,
    _: dict = Depends(require_roles([UserRole.super_admin.value, UserRole.admin.value])),
):
    return await list_custom_requests(search=search)


@router.patch("/admin/custom-requests/{request_id}/status", response_model=MessageResponse)
async def update_custom_request_status_route(
    request_id: str,
    payload: OrderStatusUpdate,
    current_admin: dict = Depends(require_roles([UserRole.super_admin.value, UserRole.admin.value])),
):
    await update_custom_request_status(request_id, payload.status.value)
    await log_activity(
        action="CUSTOM_ORDER_STATUS_UPDATED",
        performed_by_email=current_admin.get("email", ""),
        performed_by_name=current_admin.get("name", ""),
        target=request_id,
        detail=f"Custom order status updated to {payload.status.value}.",
        new_value={"status": payload.status.value},
    )
    return MessageResponse(message="Custom order status updated.")


@router.post("/admin/custom-requests/{request_id}/comments", response_model=MessageResponse)
async def add_custom_request_comment_route(
    request_id: str,
    payload: OrderCommentCreateRequest,
    current_admin: dict = Depends(require_roles([UserRole.super_admin.value, UserRole.admin.value])),
):
    await add_custom_request_comment(request_id, payload, current_admin)
    await log_activity(
        action="CUSTOM_ORDER_COMMENT_ADDED",
        performed_by_email=current_admin.get("email", ""),
        performed_by_name=current_admin.get("name", ""),
        target=request_id,
        detail=f"Comment added to custom order {request_id}.",
        new_value={"comment": payload.comment},
    )
    return MessageResponse(message="Comment added.")


@router.patch("/admin/orders/{inquiry_id}/status", response_model=MessageResponse)
async def update_order_status_route(
    inquiry_id: str,
    payload: OrderStatusUpdate,
    current_admin: dict = Depends(require_roles([UserRole.super_admin.value, UserRole.admin.value])),
):
    await update_order_status(inquiry_id, payload.status.value)
    await log_activity(
        action="CATALOG_ORDER_STATUS_UPDATED",
        performed_by_email=current_admin.get("email", ""),
        performed_by_name=current_admin.get("name", ""),
        target=inquiry_id,
        detail=f"Catalog order status updated to {payload.status.value}.",
        new_value={"status": payload.status.value},
    )
    return MessageResponse(message="Inquiry status updated.")


@router.post("/admin/orders/{inquiry_id}/comments", response_model=MessageResponse)
async def add_order_comment_route(
    inquiry_id: str,
    payload: OrderCommentCreateRequest,
    current_admin: dict = Depends(require_roles([UserRole.super_admin.value, UserRole.admin.value])),
):
    await add_order_comment(inquiry_id, payload, current_admin)
    await log_activity(
        action="CATALOG_ORDER_COMMENT_ADDED",
        performed_by_email=current_admin.get("email", ""),
        performed_by_name=current_admin.get("name", ""),
        target=inquiry_id,
        detail=f"Comment added to catalog order {inquiry_id}.",
        new_value={"comment": payload.comment},
    )
    return MessageResponse(message="Comment added.")


@router.delete("/admin/orders/{inquiry_id}", response_model=MessageResponse)
async def delete_order_route(
    inquiry_id: str,
    current_admin: dict = Depends(require_roles([UserRole.super_admin.value, UserRole.admin.value])),
):
    await delete_order(inquiry_id)
    await log_activity(
        action="CATALOG_ORDER_DELETED",
        performed_by_email=current_admin.get("email", ""),
        performed_by_name=current_admin.get("name", ""),
        target=inquiry_id,
        detail=f"Catalog order deleted: {inquiry_id}.",
    )
    return MessageResponse(message="Order deleted.")


@router.delete("/admin/custom-requests/{request_id}", response_model=MessageResponse)
async def delete_custom_request_route(
    request_id: str,
    current_admin: dict = Depends(require_roles([UserRole.super_admin.value, UserRole.admin.value])),
):
    await delete_custom_request(request_id)
    await log_activity(
        action="CUSTOM_ORDER_DELETED",
        performed_by_email=current_admin.get("email", ""),
        performed_by_name=current_admin.get("name", ""),
        target=request_id,
        detail=f"Custom order request deleted: {request_id}.",
    )
    return MessageResponse(message="Custom order request deleted.")


@router.get("/admin/orders/export")
async def export_orders_route(
    _: dict = Depends(require_roles([UserRole.super_admin.value, UserRole.admin.value])),
):
    csv_content = await export_orders_csv()
    return StreamingResponse(
        iter([csv_content]),
        media_type="text/csv",
        headers={"Content-Disposition": "attachment; filename=ons-gold-orders.csv"},
    )


@router.get("/admin/analytics")
async def dashboard_analytics_route(
    _: dict = Depends(require_roles([UserRole.super_admin.value, UserRole.admin.value])),
):
    return await get_dashboard_analytics()

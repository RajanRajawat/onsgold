import asyncio
from time import monotonic

from db.mongo import get_custom_request_collection, get_order_collection, get_product_collection
from models.dashboard import AnalyticsResponse

_DASHBOARD_CACHE_TTL_SECONDS = 15
_dashboard_cache: dict[str, float | AnalyticsResponse | None] = {
    "expires_at": 0.0,
    "value": None,
}


def invalidate_dashboard_cache():
    _dashboard_cache["expires_at"] = 0.0
    _dashboard_cache["value"] = None


async def get_dashboard_analytics() -> AnalyticsResponse:
    cached = _dashboard_cache.get("value")
    expires_at = float(_dashboard_cache.get("expires_at") or 0.0)
    if isinstance(cached, AnalyticsResponse) and monotonic() < expires_at:
        return cached

    custom_new_count, total_products, total_orders, total_custom_requests, new_order_count = await asyncio.gather(
        get_custom_request_collection().count_documents(
            {
                "$or": [
                    {"status": "new"},
                    {"status": {"$exists": False}},
                ]
            }
        ),
        get_product_collection().count_documents({}),
        get_order_collection().count_documents({}),
        get_custom_request_collection().count_documents({}),
        get_order_collection().count_documents({"status": "new"}),
    )
    analytics = AnalyticsResponse(
        total_products=total_products,
        total_orders=total_orders,
        total_custom_requests=total_custom_requests,
        new_orders=new_order_count + custom_new_count,
    )
    _dashboard_cache["value"] = analytics
    _dashboard_cache["expires_at"] = monotonic() + _DASHBOARD_CACHE_TTL_SECONDS
    return analytics

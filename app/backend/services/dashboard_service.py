from db.mongo import get_custom_request_collection, get_order_collection, get_product_collection
from models.dashboard import AnalyticsResponse


async def get_dashboard_analytics() -> AnalyticsResponse:
    custom_new_count = await get_custom_request_collection().count_documents(
        {
            "$or": [
                {"status": "new"},
                {"status": {"$exists": False}},
            ]
        }
    )
    return AnalyticsResponse(
        total_products=await get_product_collection().count_documents({}),
        total_orders=await get_order_collection().count_documents({}),
        total_custom_requests=await get_custom_request_collection().count_documents({}),
        new_orders=await get_order_collection().count_documents({"status": "new"}) + custom_new_count,
        featured_products=await get_product_collection().count_documents({"featured": True}),
    )

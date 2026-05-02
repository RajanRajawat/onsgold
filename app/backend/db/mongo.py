from datetime import datetime, timezone

from motor.motor_asyncio import AsyncIOMotorClient, AsyncIOMotorCollection, AsyncIOMotorDatabase
from core.config import get_settings

_client: AsyncIOMotorClient | None = None


def get_client() -> AsyncIOMotorClient:
    global _client
    if _client is None:
        settings = get_settings()
        _client = AsyncIOMotorClient(
            settings.mongo_uri,
            tz_aware=True,
            tzinfo=timezone.utc,
        )
    return _client


def get_database() -> AsyncIOMotorDatabase:
    settings = get_settings()
    return get_client()[settings.mongo_db_name]


def get_admin_collection() -> AsyncIOMotorCollection:
    return get_database()["admins"]


def get_product_collection() -> AsyncIOMotorCollection:
    return get_database()["products"]


def get_order_collection() -> AsyncIOMotorCollection:
    return get_database()["orders"]


def get_custom_request_collection() -> AsyncIOMotorCollection:
    return get_database()["custom_requests"]


def get_otp_collection() -> AsyncIOMotorCollection:
    return get_database()["password_otps"]


def get_activity_log_collection() -> AsyncIOMotorCollection:
    return get_database()["activity_logs"]


def get_cronjob_collection() -> AsyncIOMotorCollection:
    return get_database()["cronjob"]


async def init_indexes():
    await get_admin_collection().create_index("email", unique=True)
    await get_admin_collection().create_index("role")
    await get_product_collection().create_index("product_id", unique=True)
    await get_product_collection().create_index("slug", unique=True)
    await get_product_collection().create_index("category")
    await get_product_collection().create_index("metal")
    await get_product_collection().create_index("purity")
    await get_product_collection().create_index("stock_status")
    await get_product_collection().create_index("tags")
    await get_product_collection().create_index("weight")
    await get_product_collection().create_index("price")
    await get_product_collection().create_index("created_at")
    await get_order_collection().create_index("inquiry_id", unique=True)
    await get_order_collection().create_index("status")
    await get_order_collection().create_index("created_at")
    await get_custom_request_collection().create_index("request_id", unique=True)
    await get_custom_request_collection().create_index("status")
    await get_custom_request_collection().create_index("created_at")
    await get_custom_request_collection().create_index("phone")
    await get_otp_collection().create_index("email")
    await get_otp_collection().create_index("expires_at", expireAfterSeconds=0)
    await get_activity_log_collection().create_index("created_at")


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


def close_mongo_connection():
    global _client
    if _client is not None:
        _client.close()
        _client = None

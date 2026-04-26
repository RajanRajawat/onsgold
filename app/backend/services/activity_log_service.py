from typing import Any

from db.mongo import get_activity_log_collection, utc_now
from models.dashboard import ActivityLogResponse


def serialize_activity_log(document: dict) -> ActivityLogResponse:
    return ActivityLogResponse(
        id=str(document["_id"]),
        action=document.get("action", ""),
        performed_by_email=document.get("performed_by_email"),
        performed_by_name=document.get("performed_by_name"),
        target=document.get("target"),
        detail=document.get("detail"),
        old_value=document.get("old_value"),
        new_value=document.get("new_value"),
        extra=document.get("extra"),
        created_at=document["created_at"],
    )


async def log_activity(
    *,
    action: str,
    performed_by_email: str | None = None,
    performed_by_name: str | None = None,
    target: str | None = None,
    detail: str | None = None,
    old_value: Any | None = None,
    new_value: Any | None = None,
    extra: Any | None = None,
):
    await get_activity_log_collection().insert_one(
        {
            "action": action,
            "performed_by_email": performed_by_email,
            "performed_by_name": performed_by_name,
            "target": target,
            "detail": detail,
            "old_value": old_value,
            "new_value": new_value,
            "extra": extra,
            "created_at": utc_now(),
        }
    )


async def list_activity_logs(filter_action: str | None = None, limit: int = 2000):
    query: dict[str, Any] = {}
    if filter_action:
        query["action"] = {"$regex": filter_action, "$options": "i"}
    documents = await get_activity_log_collection().find(query).sort("created_at", -1).limit(limit).to_list(length=limit)
    return [serialize_activity_log(item) for item in documents]

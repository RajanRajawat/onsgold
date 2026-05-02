from fastapi import HTTPException, status

from db.mongo import get_cronjob_collection, utc_now

_CRONJOB_DOCUMENT_ID = "cronjob_status"


async def ensure_cronjob_document():
    await get_cronjob_collection().update_one(
        {"_id": _CRONJOB_DOCUMENT_ID},
        {
            "$set": {
                "crontest": True,
                "db_working": True,
                "server_running": True,
                "updated_at": utc_now(),
            }
        },
        upsert=True,
    )


async def get_cronjob_test_status() -> dict[str, bool]:
    document = await get_cronjob_collection().find_one({"_id": _CRONJOB_DOCUMENT_ID})
    if not document:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Cronjob test document not found.",
        )

    server_running = bool(document.get("server_running"))
    db_working = bool(document.get("db_working"))
    crontest = bool(document.get("crontest"))

    if not (server_running and db_working and crontest):
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Cronjob test failed.",
        )

    return {"server_running": True}


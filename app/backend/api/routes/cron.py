from fastapi import APIRouter

from services.cron_service import get_cronjob_test_status

router = APIRouter(prefix="/test", tags=["Cron Test"])


@router.get("/cronjob")
async def cronjob_test():
    return await get_cronjob_test_status()

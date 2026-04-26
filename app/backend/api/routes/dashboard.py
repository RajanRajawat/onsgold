from fastapi import APIRouter, Depends

from core.dependencies import require_roles
from models.auth import UserRole
from services.dashboard_service import get_dashboard_analytics

router = APIRouter(prefix="/dashboard", tags=["Dashboard"])


@router.get("/login-summary")
async def login_summary():
    return await get_dashboard_analytics()


@router.get("/summary")
async def dashboard_summary(
    _: dict = Depends(require_roles([UserRole.super_admin.value, UserRole.admin.value])),
):
    return await get_dashboard_analytics()

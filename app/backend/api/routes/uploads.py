from fastapi import APIRouter, Depends, File, UploadFile

from core.dependencies import require_roles
from models.auth import UserRole
from services.cloudinary_service import upload_images

router = APIRouter(prefix="/uploads", tags=["Uploads"])


@router.post("/product-images")
async def upload_product_images(
    files: list[UploadFile] = File(...),
    _: dict = Depends(require_roles([UserRole.super_admin.value, UserRole.admin.value])),
):
    urls = await upload_images(files, "products")
    return {"urls": urls}


@router.post("/custom-request-images")
async def upload_custom_request_images(files: list[UploadFile] = File(...)):
    urls = await upload_images(files, "custom-requests")
    return {"urls": urls}

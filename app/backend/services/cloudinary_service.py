import asyncio
import re
from urllib.parse import urlparse

import cloudinary
import cloudinary.uploader
from fastapi import HTTPException, UploadFile, status

from core.config import get_settings
from core.logging import get_logger

_configured = False
logger = get_logger(__name__)


def configure_cloudinary():
    global _configured
    if _configured:
        return
    settings = get_settings()
    if not (settings.cloudinary_cloud_name and settings.cloudinary_api_key and settings.cloudinary_api_secret):
        return
    cloudinary.config(
        cloud_name=settings.cloudinary_cloud_name,
        api_key=settings.cloudinary_api_key,
        api_secret=settings.cloudinary_api_secret,
        secure=True,
    )
    _configured = True


def is_cloudinary_configured() -> bool:
    settings = get_settings()
    return bool(settings.cloudinary_cloud_name and settings.cloudinary_api_key and settings.cloudinary_api_secret)


def extract_public_id_from_url(url: str) -> str | None:
    if not url:
        return None
    parsed = urlparse(url)
    path_parts = [part for part in parsed.path.split("/") if part]
    try:
        upload_index = path_parts.index("upload")
    except ValueError:
        return None

    asset_parts = path_parts[upload_index + 1:]
    if not asset_parts:
        return None

    version_index = next((index for index, part in enumerate(asset_parts) if re.fullmatch(r"v\d+", part)), None)
    if version_index is not None:
        asset_parts = asset_parts[version_index + 1:]
    if not asset_parts:
        return None

    asset_parts[-1] = asset_parts[-1].rsplit(".", 1)[0]
    public_id = "/".join(part for part in asset_parts if part)
    return public_id or None


async def upload_image(file: UploadFile, folder_suffix: str) -> str:
    settings = get_settings()
    if not file.content_type or not file.content_type.startswith("image/"):
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Only image files are allowed.")

    content = await file.read()
    max_size = settings.max_upload_size_mb * 1024 * 1024
    if len(content) > max_size:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Uploaded file is too large.")

    if not (settings.cloudinary_cloud_name and settings.cloudinary_api_key and settings.cloudinary_api_secret):
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Cloudinary is not configured.",
        )

    configure_cloudinary()

    def _upload():
        result = cloudinary.uploader.upload(
            content,
            folder=f"{settings.cloudinary_folder}/{folder_suffix}",
            resource_type="image",
            overwrite=False,
            use_filename=True,
            unique_filename=True,
            transformation=[{"quality": "auto", "fetch_format": "auto"}],
        )
        return result["secure_url"]

    return await asyncio.to_thread(_upload)


async def upload_images(files: list[UploadFile], folder_suffix: str) -> list[str]:
    return [await upload_image(file, folder_suffix) for file in files]


async def delete_images_by_urls(urls: list[str]) -> None:
    if not urls:
        return
    if not is_cloudinary_configured():
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Cloudinary is not configured.",
        )

    configure_cloudinary()
    public_ids = [public_id for public_id in (extract_public_id_from_url(url) for url in urls) if public_id]
    if not public_ids:
        logger.warning("No Cloudinary public IDs could be extracted from product image URLs.")
        return

    def _destroy(public_id: str):
        return cloudinary.uploader.destroy(public_id, resource_type="image", invalidate=True)

    for public_id in public_ids:
        result = await asyncio.to_thread(_destroy, public_id)
        outcome = str(result.get("result", "")).lower()
        if outcome not in {"ok", "not found"}:
            logger.error("Cloudinary delete failed for public_id=%s result=%s", public_id, result)
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail="Unable to delete product images from Cloudinary.",
            )

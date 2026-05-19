import asyncio
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from api.routes.admin import router as admin_router
from api.routes.auth import router as auth_router
from api.routes.cron import router as cron_router
from api.routes.dashboard import router as dashboard_router
from api.routes.inquiries import router as inquiries_router
from api.routes.products import router as products_router
from api.routes.uploads import router as uploads_router
from core.config import get_settings
from core.logging import configure_logging, get_logger
from db.mongo import close_mongo_connection, init_indexes
from services.cron_service import ensure_cronjob_document

configure_logging()
logger = get_logger(__name__)
_index_init_task: asyncio.Task | None = None

_FIELD_LABELS = {
    "title": "title",
    "category": "category",
    "metal": "metal",
    "description": "description",
    "purity": "purity",
    "weight": "weight",
    "images": "product image",
    "customer_name": "name",
    "phone": "phone number",
    "notes": "notes",
    "city": "city",
    "jewelry_type": "jewelry type",
    "budget": "budget",
    "comment": "comment",
    "email": "email",
    "otp": "OTP",
    "name": "name",
    "password": "password",
    "current_password": "current password",
    "new_password": "new password",
}


def _friendly_validation_message(exc: RequestValidationError) -> str:
    error = exc.errors()[0] if exc.errors() else {}
    location = [part for part in error.get("loc", []) if part not in {"body", "query", "path"}]
    field_name = str(location[-1]) if location else ""
    label = _FIELD_LABELS.get(field_name, field_name.replace("_", " ").strip())
    error_type = str(error.get("type") or "")
    message = str(error.get("msg") or "").removeprefix("Value error, ").strip()

    if field_name == "images":
        return "Upload at least one product image."
    if error_type == "missing" and label:
        return f"Please enter {label}."
    if error_type.startswith("string_too_short") and label:
        return f"Please enter {label}."
    if error_type.startswith("list_too_short") and field_name == "images":
        return "Upload at least one product image."
    if "weight" in field_name and message:
        return message if message.endswith(".") else f"{message}."
    if message:
        return message if message.endswith(".") else f"{message}."
    return "Please check the form and try again."


async def _init_indexes_in_background():
    try:
        await init_indexes()
        await ensure_cronjob_document()
        logger.info("MongoDB indexes initialized")
    except Exception:
        logger.exception("MongoDB index initialization failed")


@asynccontextmanager
async def lifespan(_: FastAPI):
    global _index_init_task
    logger.info("Starting ONS Gold backend")
    _index_init_task = asyncio.create_task(_init_indexes_in_background())
    yield
    if _index_init_task and not _index_init_task.done():
        _index_init_task.cancel()
    close_mongo_connection()
    logger.info("Stopped ONS Gold backend")


settings = get_settings()

app = FastAPI(
    title=settings.app_name,
    version="2.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth_router, prefix="/api/v1")
app.include_router(admin_router, prefix="/api/v1")
app.include_router(products_router, prefix="/api/v1")
app.include_router(inquiries_router, prefix="/api/v1")
app.include_router(uploads_router, prefix="/api/v1")
app.include_router(dashboard_router, prefix="/api/v1")
app.include_router(cron_router)


@app.exception_handler(RequestValidationError)
async def request_validation_exception_handler(_, exc: RequestValidationError):
    return JSONResponse(
        status_code=422,
        content={"detail": _friendly_validation_message(exc)},
    )


@app.exception_handler(Exception)
async def unhandled_exception_handler(_, exc: Exception):
    logger.exception("Unhandled exception", exc_info=exc)
    return JSONResponse(
        status_code=500,
        content={"detail": "Internal server error."},
    )


@app.get("/health")
async def health_check():
    return {"status": "ok", "environment": settings.environment}

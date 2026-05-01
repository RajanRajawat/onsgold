import asyncio
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from api.routes.admin import router as admin_router
from api.routes.auth import router as auth_router
from api.routes.dashboard import router as dashboard_router
from api.routes.inquiries import router as inquiries_router
from api.routes.products import router as products_router
from api.routes.uploads import router as uploads_router
from core.config import get_settings
from core.logging import configure_logging, get_logger
from db.mongo import close_mongo_connection, init_indexes

configure_logging()
logger = get_logger(__name__)
_index_init_task: asyncio.Task | None = None


async def _init_indexes_in_background():
    try:
        await init_indexes()
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

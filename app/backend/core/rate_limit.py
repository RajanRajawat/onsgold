from collections import defaultdict, deque
from time import time

from fastapi import HTTPException, Request, status

from core.config import get_settings

_rate_limit_store: dict[str, deque[float]] = defaultdict(deque)


def rate_limit(key_prefix: str, max_requests: int | None = None, window_seconds: int | None = None):
    settings = get_settings()
    limit = max_requests or settings.rate_limit_public_forms
    window = window_seconds or settings.rate_limit_window_seconds

    async def dependency(request: Request):
        client_ip = request.client.host if request.client else "unknown"
        key = f"{key_prefix}:{client_ip}"
        now = time()
        timestamps = _rate_limit_store[key]

        while timestamps and now - timestamps[0] > window:
            timestamps.popleft()

        if len(timestamps) >= limit:
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail="Too many requests. Please try again later.",
            )

        timestamps.append(now)

    return dependency

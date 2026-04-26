from collections.abc import Iterable

from fastapi import Depends, HTTPException, Request, status
from fastapi.security import OAuth2PasswordBearer
from jose import JWTError, jwt

from core.config import get_settings
from db.mongo import get_admin_collection

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/auth/login")


def get_admin_role(admin: dict) -> str:
    role = admin.get("role")
    if role:
        return role
    roles = admin.get("roles") or []
    if "super_admin" in roles:
        return "super_admin"
    return "admin"


async def get_current_admin(token: str = Depends(oauth2_scheme)):
    settings = get_settings()
    auth_error = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials.",
        headers={"WWW-Authenticate": "Bearer"},
    )

    try:
        payload = jwt.decode(token, settings.jwt_secret_key, algorithms=[settings.jwt_algorithm])
        email = payload.get("sub")
        role = payload.get("role")
        if not email or not role:
            raise auth_error
    except JWTError as exc:
        raise auth_error from exc

    admin = await get_admin_collection().find_one({"email": email, "is_active": True})
    if not admin:
        raise auth_error
    return admin


def require_roles(roles: Iterable[str]):
    allowed_roles = set(roles)

    async def dependency(current_admin=Depends(get_current_admin)):
        if get_admin_role(current_admin) not in allowed_roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="You do not have permission for this action.",
            )
        return current_admin

    return dependency


async def get_client_ip(request: Request) -> str:
    return request.client.host if request.client else "unknown"

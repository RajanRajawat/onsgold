import asyncio
import os
import sys
from pathlib import Path

CURRENT_DIR = Path(__file__).resolve().parent
BACKEND_DIR = CURRENT_DIR.parent
if str(BACKEND_DIR) not in sys.path:
    sys.path.insert(0, str(BACKEND_DIR))

from core.security import hash_password  # noqa: E402
from db.mongo import get_admin_collection, init_indexes, utc_now  # noqa: E402
from models.auth import UserRole, normalize_email, validate_password_strength  # noqa: E402


async def create_super_admin():
    await init_indexes()

    email = normalize_email(input("Super admin email: "))
    existing_admin = await get_admin_collection().find_one({"email": email})
    if existing_admin:
        print("An admin with this email already exists.")
        return

    name = input("Super admin name: ").strip()

    while True:
        password = input("Password: ").strip()
        confirm_password = input("Confirm password: ").strip()
        if password != confirm_password:
            print("Passwords do not match.")
            continue
        try:
            validate_password_strength(password)
            hash_password(password)
            break
        except ValueError as exc:
            print(f"Password error: {exc}")

    now = utc_now()
    await get_admin_collection().insert_one(
        {
            "name": name,
            "email": email,
            "password_hash": hash_password(password),
            "role": UserRole.super_admin.value,
            "roles": [UserRole.super_admin.value, UserRole.admin.value],
            "is_active": True,
            "created_at": now,
            "updated_at": now,
        }
    )
    print(f"Super admin created: {email}")


if __name__ == "__main__":
    os.chdir(BACKEND_DIR)
    asyncio.run(create_super_admin())

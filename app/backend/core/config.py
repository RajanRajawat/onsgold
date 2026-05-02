from functools import lru_cache

from dotenv import load_dotenv
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

load_dotenv()


class Settings(BaseSettings):
    app_name: str = "ONS Gold Backend"
    environment: str = "development"
    mongo_uri: str = Field(..., alias="MONGO_URI")
    mongo_db_name: str = Field("ons_gold", alias="MONGO_DB_NAME")
    jwt_secret_key: str = Field(..., alias="JWT_SECRET_KEY")
    jwt_algorithm: str = Field("HS256", alias="JWT_ALGORITHM")
    access_token_expire_minutes: int = Field(1440, alias="ACCESS_TOKEN_EXPIRE_MINUTES")
    otp_expire_minutes: int = Field(10, alias="OTP_EXPIRE_MINUTES")
    super_admin_alert_email: str | None = Field(None, alias="SUPER_ADMIN_ALERT_EMAIL")
    smtp_host: str | None = Field(None, alias="SMTP_HOST")
    smtp_port: int = Field(587, alias="SMTP_PORT")
    smtp_username: str | None = Field(None, alias="SMTP_USERNAME")
    smtp_password: str | None = Field(None, alias="SMTP_PASSWORD")
    smtp_from_email: str | None = Field(None, alias="SMTP_FROM_EMAIL")
    smtp_from_name: str = Field("ONS Gold", alias="SMTP_FROM_NAME")
    smtp_use_tls: bool = Field(True, alias="SMTP_USE_TLS")
    smtp_use_ssl: bool = Field(False, alias="SMTP_USE_SSL")
    smtp_timeout_seconds: int = Field(20, alias="SMTP_TIMEOUT_SECONDS")
    bug_report_recipients_raw: str = Field("work@rajanrajawat.in", alias="BUG_REPORT_RECIPIENTS")
    cloudinary_cloud_name: str | None = Field(None, alias="CLOUDINARY_CLOUD_NAME")
    cloudinary_api_key: str | None = Field(None, alias="CLOUDINARY_API_KEY")
    cloudinary_api_secret: str | None = Field(None, alias="CLOUDINARY_API_SECRET")
    cloudinary_folder: str = Field("ons-gold", alias="CLOUDINARY_FOLDER")
    whatsapp_number: str = Field("9833348296", alias="WHATSAPP_NUMBER")
    frontend_base_url: str = Field("http://localhost:5500", alias="FRONTEND_BASE_URL")
    rate_limit_public_forms: int = Field(10, alias="RATE_LIMIT_PUBLIC_FORMS")
    rate_limit_window_seconds: int = Field(300, alias="RATE_LIMIT_WINDOW_SECONDS")
    max_upload_size_mb: int = Field(8, alias="MAX_UPLOAD_SIZE_MB")

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        populate_by_name=True,
        extra="ignore",
    )

    @property
    def bug_report_recipients(self) -> list[str]:
        return [item.strip() for item in self.bug_report_recipients_raw.split(",") if item.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()

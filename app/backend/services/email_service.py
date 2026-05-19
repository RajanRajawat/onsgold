import asyncio
import smtplib
import socket
import ssl
from email.message import EmailMessage
from typing import NamedTuple
from typing import TypedDict

from core.config import get_settings
from core.logging import get_logger
from db.mongo import get_admin_collection

logger = get_logger(__name__)


class EmailAttachment(TypedDict):
    filename: str
    content: bytes
    mime_type: str


class EmailDeliveryError(RuntimeError):
    pass


class SmtpAttempt(NamedTuple):
    host: str
    port: int
    use_tls: bool
    use_ssl: bool
    label: str


async def get_super_admin_emails() -> list[str]:
    admins = await get_admin_collection().find(
        {
            "$or": [
                {"role": "super_admin"},
                {"roles": "super_admin"},
            ],
            "is_active": True,
        }
    ).to_list(length=100)
    return [admin["email"] for admin in admins]


def _normalize_recipients(recipients: list[str]) -> list[str]:
    normalized: list[str] = []
    seen: set[str] = set()
    for recipient in recipients:
        email = str(recipient or "").strip().lower()
        if not email or email in seen:
            continue
        seen.add(email)
        normalized.append(email)
    return normalized


def _build_smtp_attempts() -> list[SmtpAttempt]:
    settings = get_settings()
    host = str(settings.smtp_host or "").strip()
    if not host:
        return []

    attempts: list[SmtpAttempt] = []
    seen: set[tuple[str, int, bool, bool]] = set()

    def add_attempt(port: int, use_tls: bool, use_ssl: bool, label: str):
        key = (host.lower(), int(port), bool(use_tls), bool(use_ssl))
        if key in seen:
            return
        seen.add(key)
        attempts.append(
            SmtpAttempt(
                host=host,
                port=int(port),
                use_tls=bool(use_tls),
                use_ssl=bool(use_ssl),
                label=label,
            )
        )

    primary_port = int(settings.smtp_port)
    primary_ssl = bool(settings.smtp_use_ssl or primary_port == 465)
    primary_tls = bool(settings.smtp_use_tls and not primary_ssl)
    add_attempt(primary_port, primary_tls, primary_ssl, "configured transport")

    # Some hosted environments block submission over 587. Try the common SSL transport as a safe fallback.
    if primary_port != 465:
        add_attempt(465, False, True, "implicit SSL fallback")
    if primary_port != 587:
        add_attempt(587, True, False, "STARTTLS fallback")

    return attempts


def _send_message_via_smtp(message: EmailMessage, attempt: SmtpAttempt, username: str | None, password: str | None, timeout_seconds: int):
    if attempt.use_ssl:
        with smtplib.SMTP_SSL(
            attempt.host,
            attempt.port,
            timeout=timeout_seconds,
            context=ssl.create_default_context(),
        ) as server:
            if username and password:
                server.login(username, password)
            server.send_message(message)
        return

    with smtplib.SMTP(attempt.host, attempt.port, timeout=timeout_seconds) as server:
        server.ehlo()
        if attempt.use_tls:
            server.starttls(context=ssl.create_default_context())
            server.ehlo()
        if username and password:
            server.login(username, password)
        server.send_message(message)


def send_email_sync(
    *,
    recipients: list[str],
    subject: str,
    body: str,
    attachments: list[EmailAttachment] | None = None,
    fail_silently: bool = False,
):
    settings = get_settings()
    recipients = _normalize_recipients(recipients)
    if not recipients:
        logger.info("Skipping email: no super admin recipients configured")
        return

    if not settings.smtp_host or not settings.smtp_from_email:
        message = "SMTP is not configured."
        logger.error("%s Email subject=%s recipients=%s", message, subject, recipients)
        if fail_silently:
            return
        raise EmailDeliveryError(message)

    message = EmailMessage()
    message["Subject"] = subject
    message["From"] = f"{settings.smtp_from_name} <{settings.smtp_from_email}>"
    message["To"] = ", ".join(recipients)
    message.set_content(body)
    for attachment in attachments or []:
        maintype, subtype = attachment["mime_type"].split("/", 1)
        message.add_attachment(
            attachment["content"],
            maintype=maintype,
            subtype=subtype,
            filename=attachment["filename"],
        )

    timeout_seconds = max(1, int(settings.smtp_timeout_seconds))
    attempts = _build_smtp_attempts()
    last_error: Exception | None = None

    for attempt in attempts:
        try:
            _send_message_via_smtp(
                message=message,
                attempt=attempt,
                username=settings.smtp_username,
                password=settings.smtp_password,
                timeout_seconds=timeout_seconds,
            )
            logger.info(
                "Email sent successfully subject=%s recipients=%s transport=%s@%s:%s",
                subject,
                recipients,
                attempt.label,
                attempt.host,
                attempt.port,
            )
            return
        except (smtplib.SMTPException, OSError, socket.timeout) as exc:
            last_error = exc
            logger.warning(
                "Email delivery attempt failed subject=%s recipients=%s transport=%s@%s:%s error=%s",
                subject,
                recipients,
                attempt.label,
                attempt.host,
                attempt.port,
                exc,
            )

    logger.error(
        "Email delivery failed after all SMTP attempts subject=%s recipients=%s final_error=%s",
        subject,
        recipients,
        last_error,
    )
    if fail_silently:
        return
    raise EmailDeliveryError("Unable to deliver email with the configured SMTP settings.") from last_error


async def notify_super_admins(subject: str, body: str):
    recipients = await get_super_admin_emails()
    await asyncio.to_thread(send_email_sync, recipients=recipients, subject=subject, body=body, fail_silently=True)


async def send_password_reset_otp(email: str, otp: str):
    settings = get_settings()
    body = f"Your ONS Gold password reset OTP is {otp}. It expires in {settings.otp_expire_minutes} minutes."
    await asyncio.to_thread(send_email_sync, recipients=[email], subject="ONS Gold Password Reset OTP", body=body)


async def send_admin_action_otp(email: str, otp: str, action: str):
    settings = get_settings()
    body = f"Your ONS Gold admin OTP for {action} is {otp}. It expires in {settings.otp_expire_minutes} minutes."
    await asyncio.to_thread(send_email_sync, recipients=[email], subject="ONS Gold Admin OTP", body=body)


async def send_admin_credentials(email: str, name: str, password: str):
    body = (
        f"Hello {name},\n\n"
        "Your ONS Gold admin account has been created.\n\n"
        f"Login email: {email}\n"
        f"Temporary password: {password}\n\n"
        "Please sign in and change your password from My Account."
    )
    await asyncio.to_thread(send_email_sync, recipients=[email], subject="ONS Gold Admin Account Created", body=body)

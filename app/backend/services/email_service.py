import asyncio
import smtplib
import socket
import ssl
from email.message import EmailMessage
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
    emails = [admin["email"] for admin in admins]
    settings = get_settings()
    if settings.super_admin_alert_email and settings.super_admin_alert_email not in emails:
        emails.append(settings.super_admin_alert_email)
    return emails


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

    try:
        if settings.smtp_port == 465 and not settings.smtp_use_tls:
            with smtplib.SMTP_SSL(settings.smtp_host, settings.smtp_port, timeout=20, context=ssl.create_default_context()) as server:
                if settings.smtp_username and settings.smtp_password:
                    server.login(settings.smtp_username, settings.smtp_password)
                server.send_message(message)
        else:
            with smtplib.SMTP(settings.smtp_host, settings.smtp_port, timeout=20) as server:
                server.ehlo()
                if settings.smtp_use_tls:
                    server.starttls(context=ssl.create_default_context())
                    server.ehlo()
                if settings.smtp_username and settings.smtp_password:
                    server.login(settings.smtp_username, settings.smtp_password)
                server.send_message(message)
        logger.info("Email sent successfully subject=%s recipients=%s", subject, recipients)
    except (smtplib.SMTPException, OSError, socket.timeout) as exc:
        logger.exception("Email delivery failed subject=%s recipients=%s", subject, recipients)
        if fail_silently:
            return
        raise EmailDeliveryError("Unable to deliver email with the configured SMTP settings.") from exc


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

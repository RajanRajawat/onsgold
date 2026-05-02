# ONS Gold Admin Android

Native Android client for the existing `admin.html` backend flows.

## Included

- admin login
- forgot password OTP flow
- dashboard overview
- products list with create, edit, delete, and image upload
- orders list with status updates, comments, delete, and CSV export share
- super admin management for admins
- activity logs
- account profile actions
- bug report submission with optional screenshot

## Open In Android Studio

Open the folder:

```text
app/android
```

## Backend URL

By default the app points to:

```text
https://onsgold.onrender.com/
```

To override it during build:

```bash
./gradlew assembleDebug -PONS_API_BASE=https://your-api-root/
```

The value should be the backend root, not `/api/v1`.

## Notes

- minimum SDK is `26`
- this project is Compose-based and intended for Android Studio / Gradle sync
- image uploads and bug screenshots use the device image picker
- admin-only and super-admin-only behaviors follow the current backend role checks

## Current scope

This is a mobile-native admin client. It does not wrap `admin.html` in a WebView.

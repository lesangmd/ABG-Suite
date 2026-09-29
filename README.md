# Khí Máu — Android

Offline-first Android package for MEDIPHARM ABG Suite.

- App name: Khí Máu
- Android app version: 1.2.0
- applicationId: com.medipharm.abgsuite
- minSdk: 31 (Android 12)
- target/compileSdk: 35
- Java: 17
- Runtime: bundled SQLite database containing a self-contained HTML/CSS/JavaScript ABG runtime
- Data version: ABG-2026.09.29
- Network policy: professional ABG functions work offline; Internet is used only for MEDIPHARM account authentication and explicit update checks
- Authentication: WordPress/MEDIPHARM login over HTTPS, followed by a device-local 90-day offline entitlement protected by Android Keystore HMAC-SHA256
- Passwords are never stored by the Android app
- App/data versioning are independent by contract

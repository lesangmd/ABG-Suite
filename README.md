# Khí Máu — Android

Offline-first Android package for MEDIPHARM ABG Suite.

- App name: Khí Máu
- Android app version: 1.2.2
- applicationId: com.medipharm.abgsuite
- minSdk: 31 (Android 12)
- target/compileSdk: 35
- Java: 17
- Runtime: bundled SQLite database containing a self-contained HTML/CSS/JavaScript ABG runtime
- Data version: ABG-2026.09.29
- Network policy: professional ABG functions work offline; Internet is used only for MEDIPHARM account authentication and explicit update checks
- Authentication: native Android credential form posts directly over HTTPS to the MEDIPHARM WordPress authentication endpoint; no website page is rendered in the app; successful authentication creates a device-local 90-day offline entitlement protected by Android Keystore HMAC-SHA256
- Passwords are never stored by the Android app
- App/data versioning are independent by contract

## v1.2.2 branding patch

- Canonical launcher identity: approved O₂/CO₂ exchange symbol + ABG wordmark.
- Native status/navigation chrome aligned to arterial red / oxygenation blue / cyan visual system.
- Business logic, Offline-First database, authentication, update channel and package ID are unchanged.

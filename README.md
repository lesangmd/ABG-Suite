# Khí Máu — Android

Offline-first Android package for MEDIPHARM ABG Suite.

- App name: Khí Máu
- Android app version: 1.5.0
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


## v1.2.3 focused clinical home

- Compact header: ABG identity, single-layer search, Account and Menu controls.
- Primary navigation reduced to the six clinical learning areas used by Web v6.3.6.
- Home screen restyled to the approved focused mockup: clinical hero, five modules, continue-learning summary and progress summary.
- Secondary home feed/profile blocks are suppressed to reduce visual noise.
- Offline-first data, authentication, update flow, calculations and package ID are unchanged.


## v1.3.0 synchronized dictionary home

- Android home aligned to MEDIPHARM ABG Web v6.5.0.
- Dictionary-first learning surface with keyword and A–Z filtering.
- Compact five-module clinical navigation and recent activity/resource cards.
- Full-screen native MEDIPHARM ABG login replaces the legacy AlertDialog layout.
- Offline-first database, authentication endpoint, calculations, update channel and package ID are unchanged.


## v1.4.0 action-first clinical home

- Home synchronized to Web v6.6.0.
- Featured resources removed.
- Primary actions: new ABG analysis, sample clinical cases, clinical Q&A, learning dictionary, review.
- Three sample cases surfaced on Home: DKA, acute respiratory acidosis/COPD, PE hypoxemia.
- Clinical Q&A surfaced as a first-class Home card.
- Analysis workspace remains calculation-compatible; Offline-First data and authentication are unchanged.


## v1.5.0 complete synchronized release

- Offline runtime is rebuilt from the installed MEDIPHARM ABG Web v6.7.3 source at build time.
- Android no longer replaces the Home UI with a divergent legacy overlay; it uses the same v6.7.3 responsive UI, dark theme, menu role separation and bottom dock.
- Native full-screen MEDIPHARM ABG authentication remains in place.
- Android system status/navigation bars synchronize with WebApp light/dark mode.
- PWA-install and Android-download actions are removed inside the native Android shell; account, logout and app-update actions remain native.
- Offline-First database, ABG calculation core, entitlement validation and package ID are preserved.

# MEDIPHARM ABG Android v1.3.0 — NEW PRODUCTION SIGNING BASELINE

## Baseline (2026-10-08)

- Package ID: `com.medipharm.abgsuite`
- Version: `1.3.0`; versionCode `10`
- New signing certificate SHA-256: `bdce73416f223b0bcedf6b8bf6f19da191c8667b9822c0db45674c4fed112fea`
- Signing algorithm: RSA-4096 / SHA256, PKCS#12
- Android app identity, brand image, login and offline clinical code retained.
- This signature has replaced the v1.2.x certificate for v1.3.0+.
- WebApp UI target: v6.11.7 mobile visual contract; offline data and calculation runtime currently remain v6.11.4. Do not mislabel the runtime as full 6.11.7 parity.

## IMPORTANT — Migration from v1.2.x

Android **will not** permit installing v1.3.0 with the new signing certificate directly over a v1.2.x APK. Users must back up/record any locally stored items, uninstall old version and install v1.3.0 as a fresh app; login/entitlement must be recreated. Do not claim to preserve local app data across uninstall. From v1.3.0 onward, upgrades must use the exact same new certificate and monotonically increasing version codes.

## Private signing custody

Never publish .p12, passwords, or keystore Base64 in this repository, issue, release or CI log. Keep two independent encrypted/offline backups. New certificate SHA-256 is public; private key is not.

Configure repository secret values in Settings → Secrets and variables → Actions:
- `ANDROID_KEYSTORE_B64`: Base64 encoding of the v1.3.0 keystore binary.
- `ANDROID_KEY_ALIAS`: alias in the confidential key recovery file.
- `ANDROID_KEYSTORE_PASSWORD`: confidential store password.
- `ANDROID_KEY_PASSWORD`: confidential key password.

When all four secrets have been added, run workflow `Build MEDIPHARM ABG Android v1.3.0`. It must fail closed unless `apksigner verify --print-certs` identifies the v1.3.0 pinned certificate. Do not upload an unsigned QA artifact as a release APK.

## Verification required to mark PRODUCTION

1. `apksigner verify --verbose --print-certs MEDIPHARM-ABG-v1.3.0.apk` returns verified=true and SHA-256 matches new baseline.
2. `aapt dump badging` confirms package ID, versionCode 10, versionName 1.3.0.
3. Install fresh on Android 12+; verify native login, local runtime, light/dark color synchronization, four native dock routes, no duplicate dock, no horizontal overflow.
4. Check airplane-mode use after first successful login, clinical calculations and knowledge/case content.
5. Retain the private signing key and access recovery procedure; do not publish to production until this checklist is complete.

## Change summary

- Mobile interface overlay aligned with the v6.11.7 WebApp design contract.
- Native bottom navigation fixed outside scrollable WebView.
- New release signing certificate for all future v1.3.0+ app updates.
- Clinical calculation JavaScript and source logo unchanged.

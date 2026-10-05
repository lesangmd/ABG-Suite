# NAH LAB SUITE — Android & Desktop

Public distribution channel for NAH LAB SUITE native clients.

## Latest versions

### Android
- Current version: **v1.1.3**
- Download APK: https://raw.githubusercontent.com/lesangmd/ABG-Suite/nah-lab-suite-public/NAH-LAB-SUITE/releases/android/NAH-LAB-SUITE-Android-latest.apk
- Source ZIP: https://raw.githubusercontent.com/lesangmd/ABG-Suite/nah-lab-suite-public/NAH-LAB-SUITE/source/android/NAH-LAB-SUITE-Android-latest-Source.zip

### Desktop Windows x64
- Current version: **v1.3.1**
- Download runtime ZIP: https://raw.githubusercontent.com/lesangmd/ABG-Suite/nah-lab-suite-public/NAH-LAB-SUITE/releases/desktop/NAH-LAB-SUITE-Desktop-latest-x64.zip
- Native source ZIP: https://raw.githubusercontent.com/lesangmd/ABG-Suite/nah-lab-suite-public/NAH-LAB-SUITE/source/desktop/NAH-LAB-SUITE-Desktop-latest-Source.zip

## Machine-readable latest manifest

https://raw.githubusercontent.com/lesangmd/ABG-Suite/nah-lab-suite-public/NAH-LAB-SUITE/latest.json

## Release policy

- Android package ID remains `vn.nah.iso15189suite`.
- Future Android production builds must use one persistent NAH release signing identity so upgrades install over older versions.
- Desktop releases preserve the local offline vault during runtime upgrades.
- SHA-256 values are published in `latest.json`.
- Stable `latest` source/binary URLs stay unchanged; only their accepted payload is advanced for a new release.
- Desktop native source is published separately from the generated WebApp runtime/content bundle.

> Note: Android v1.1.3 currently remains a validation/test-signed build. Lock the persistent NAH release signing key before the next production Android release.

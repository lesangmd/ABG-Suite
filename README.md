# Khí Máu — Android

Native Android WebView shell for MEDIPHARM ABG Suite.

- App name: Khí Máu
- Web source: MEDIPHARM ABG Suite v6.2.8
- Start URL: https://www.sachyhoc.com/phan-tich-khi-mau-app/
- Android app version: 1.1.1
- applicationId: com.medipharm.abgsuite
- minSdk: 31 (Android 12)
- target/compileSdk: 35
- Java: 17
- Session policy: persistent WebView cookies/local storage
- Account flow: the Android login action opens the embedded Medipharm membership form directly without using the trial-access query; successful authentication returns to Khí Máu
- Update flow: manual GitHub version check from the app menu; newer APK is downloaded to Downloads

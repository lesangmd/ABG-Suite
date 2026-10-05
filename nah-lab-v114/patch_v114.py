#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_v114.py <android_source_root>")
root=Path(sys.argv[1])

# Version.
p=root/"app/build.gradle"
s=p.read_text()
s=s.replace("versionCode 11003","versionCode 11004").replace("versionName '1.1.3'","versionName '1.1.4'")
p.write_text(s)

# Update manager: remove login-form injection completely; keep silent background check.
p=root/"app/src/main/java/vn/nah/iso15189suite/UpdateManager.java"
s=p.read_text()
s=s.replace('static final int CURRENT_VERSION_CODE = 11003;','static final int CURRENT_VERSION_CODE = 11004;')
s=s.replace('static final String CURRENT_VERSION = "1.1.3";','static final String CURRENT_VERSION = "1.1.4";')
s=s.replace('c.setRequestProperty("User-Agent", "NAHISOAndroid/1.1.3");','c.setRequestProperty("User-Agent", "NAHISOAndroid/1.1.4");')

start=s.find("    void injectCheckButton(WebView view) {")
if start >= 0:
    end=s.find("\n    void check(boolean userInitiated) {", start)
    if end < 0:
        raise SystemExit("injectCheckButton end anchor missing")
    s=s[:start] + s[end+1:]
p.write_text(s)

# MainActivity: stop inserting update UI into login page. Bump shell contract to force new Web v1.50 assets.
p=root/"app/src/main/java/vn/nah/iso15189suite/MainActivity.java"
s=p.read_text()
s=s.replace("NAHISOAndroid/1.1.3","NAHISOAndroid/1.1.4")
s=s.replace('offlineStore.ensureShellContract("android-v1.1.3-web-v1.47");',
            'offlineStore.ensureShellContract("android-v1.1.4-web-v1.50");')
s=s.replace("            injectLogoutCacheHook(view);\n            if (updateManager != null) updateManager.injectCheckButton(view);",
            "            injectLogoutCacheHook(view);")
# Defensive removal if formatting differs.
s=s.replace("if (updateManager != null) updateManager.injectCheckButton(view);","")
p.write_text(s)

# Release note.
p=root/"README.md"
p.write_text("""# NAH LAB SUITE Android v1.1.4 — Login Update UX Hotfix

- Parent: v1.1.3 Persistent Offline Vault & In-App Updater.
- Removes native injection of “Kiểm tra cập nhật” from the login form.
- Background update check remains silent after app start; popup is shown only when a newer Android version is available.
- No update-control text is rendered on the login screen.
- Shell contract moves to android-v1.1.4-web-v1.50 so stale cached login assets are invalidated once.
- Persistent Offline Vault, dataset-aware sync, deep hydration and in-app installer logic are otherwise unchanged.

""" + p.read_text())

# Static guards.
u=(root/"app/src/main/java/vn/nah/iso15189suite/UpdateManager.java").read_text()
m=(root/"app/src/main/java/vn/nah/iso15189suite/MainActivity.java").read_text()
if "injectCheckButton" in u or "injectCheckButton" in m:
    raise SystemExit("login update injection still present")
if "NAHISOAndroid/1.1.4" not in m:
    raise SystemExit("UA not bumped")
if "android-v1.1.4-web-v1.50" not in m:
    raise SystemExit("shell contract not bumped")

print("v1.1.4 patch complete")

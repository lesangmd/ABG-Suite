#!/usr/bin/env python3
from pathlib import Path
import base64, hashlib, sys

if len(sys.argv) != 3:
    raise SystemExit("usage: patch_v111.py <android_source_root> <logo_chunk_dir>")

root = Path(sys.argv[1])
logo_dir = Path(sys.argv[2])

# Version / native identity.
gradle = root / "app/build.gradle"
s = gradle.read_text(encoding="utf-8")
s = s.replace("versionCode 11000", "versionCode 11001")
s = s.replace("versionName '1.1.0'", "versionName '1.1.1'")
gradle.write_text(s, encoding="utf-8")

main = root / "app/src/main/java/vn/nah/iso15189suite/MainActivity.java"
s = main.read_text(encoding="utf-8")
s = s.replace("NAHISOAndroid/1.1.0", "NAHISOAndroid/1.1.1")

# Some early v1.1.0 source snapshots used in CI missed this helper; normalize it here.
if "private void openExternal(String url)" not in s:
    needle = "    private boolean networkAvailable() {\n"
    method = """    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(
                    this,
                    "Không tìm thấy trình duyệt để mở hệ thống.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

"""
    if needle not in s:
        raise SystemExit("openExternal insertion point missing")
    s = s.replace(needle, method + needle, 1)

old = "        offlineStore = new OfflineStore(this);\n        OfflineSyncJobService.schedule(this);"
new = """        offlineStore = new OfflineStore(this);
        // v1.1.1: only invalidate the cached WebApp shell when the native/web
        // startup-transport contract changes. Hydrated business data is retained.
        offlineStore.ensureShellContract("android-v1.1.1-web-v1.45");
        OfflineSyncJobService.schedule(this);"""
if new not in s:
    if old not in s:
        raise SystemExit("OfflineStore onCreate anchor missing")
    s = s.replace(old, new, 1)
main.write_text(s, encoding="utf-8")

store = root / "app/src/main/java/vn/nah/iso15189suite/OfflineStore.java"
s = store.read_text(encoding="utf-8")
method = """    synchronized void ensureShellContract(String contract) {
        if (contract == null || contract.isEmpty()) return;
        String current = getMeta("shell_contract");
        if (contract.equals(current)) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("shell_cache", null, null);
            db.delete("meta", "meta_key IN (?,?)", new String[]{"last_shell_sync_ms", "shell_contract"});
            ContentValues cv = new ContentValues();
            cv.put("meta_key", "shell_contract");
            cv.put("meta_value", contract);
            db.insertWithOnConflict("meta", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

"""
if "synchronized void ensureShellContract(String contract)" not in s:
    needle = "    synchronized void clearUserData() {\n"
    if needle not in s:
        raise SystemExit("OfflineStore clearUserData anchor missing")
    s = s.replace(needle, method + needle, 1)
store.write_text(s, encoding="utf-8")

# Exact approved original logo, byte-for-byte from the canonical 256 px asset.
chunks = sorted(logo_dir.glob("*.b64"))
if not chunks:
    raise SystemExit("logo chunks missing")
logo_b64 = "".join(p.read_text(encoding="utf-8").strip() for p in chunks)
logo = base64.b64decode(logo_b64, validate=True)
expected = "ea0f8a7dca706f5908e19b811e8af39f5c20021220ee17320b5d9816ebf2c487"
actual = hashlib.sha256(logo).hexdigest()
if actual != expected:
    raise SystemExit(f"logo SHA mismatch: {actual}")

nodpi = root / "app/src/main/res/drawable-nodpi"
nodpi.mkdir(parents=True, exist_ok=True)
(nodpi / "nah_logo_original.png").write_bytes(logo)

resources = {
"app/src/main/res/drawable/nah_logo_foreground.xml": """<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:left="13dp" android:top="13dp" android:right="13dp" android:bottom="13dp">
        <bitmap android:gravity="center" android:src="@drawable/nah_logo_original" />
    </item>
</layer-list>
""",
"app/src/main/res/drawable/nah_logo_legacy.xml": """<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:drawable="@color/white" />
    <item android:left="10dp" android:top="10dp" android:right="10dp" android:bottom="10dp">
        <bitmap android:gravity="center" android:src="@drawable/nah_logo_original" />
    </item>
</layer-list>
""",
"app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml": """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/white" />
    <foreground android:drawable="@drawable/nah_logo_foreground" />
</adaptive-icon>
""",
"app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml": """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/white" />
    <foreground android:drawable="@drawable/nah_logo_foreground" />
</adaptive-icon>
""",
"app/src/main/res/mipmap-anydpi/ic_launcher.xml": """<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:drawable="@color/white" />
    <item android:drawable="@drawable/nah_logo_legacy" />
</layer-list>
""",
"app/src/main/res/mipmap-anydpi/ic_launcher_round.xml": """<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:drawable="@color/white" />
    <item android:drawable="@drawable/nah_logo_legacy" />
</layer-list>
""",
}
for rel, content in resources.items():
    p = root / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(content, encoding="utf-8")

# Intentionally do not add a monochrome adaptive-icon layer. This preserves the
# original green/pink identity instead of letting Android themed-icons recolor it.
print("v1.1.1 patch complete")
print("approved logo sha256", actual)

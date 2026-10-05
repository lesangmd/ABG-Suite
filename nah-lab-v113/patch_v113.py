#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_v113.py <android_source_root>")
root=Path(sys.argv[1])

# Version
p=root/"app/build.gradle"
s=p.read_text()
s=s.replace("versionCode 11002","versionCode 11003").replace("versionName '1.1.2'","versionName '1.1.3'")
p.write_text(s)

# Manifest: direct package-installer handoff.
p=root/"app/src/main/AndroidManifest.xml"
s=p.read_text()
if "REQUEST_INSTALL_PACKAGES" not in s:
    s=s.replace('<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />',
                '<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />\n    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />')
p.write_text(s)

# Persistent vault semantics.
p=root/"app/src/main/java/vn/nah/iso15189suite/OfflineStore.java"
s=p.read_text()
if "revokeValidationKeepVault" not in s:
    anchor="    synchronized void clearUserData() {\n"
    method='''    synchronized void revokeValidationKeepVault() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("meta", "meta_key IN (?,?)", new String[]{"last_validated_ms", "active_session_ref"});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

'''
    if anchor not in s: raise SystemExit("clearUserData anchor missing")
    s=s.replace(anchor,method+anchor,1)
p.write_text(s)

# Dataset-manifest aware core sync; all vault payload stays encrypted.
p=root/"app/src/main/java/vn/nah/iso15189suite/HydrationManager.java"
s=p.read_text()
s=s.replace('static final String CORE_MIRROR_URL = REST_BASE + "desktop/mirror/full?scope=core";',
'''static final String CORE_MIRROR_URL = REST_BASE + "desktop/mirror/full?scope=core";
    static final String DATASET_MANIFEST_URL = REST_BASE + "desktop/dataset/manifest";''')
s=s.replace('static final String UA_MARKER = "NAHISOAndroid/1.1.2";','static final String UA_MARKER = "NAHISOAndroid/1.1.3";')
needle='''                HttpResult mirror = httpGet(CORE_MIRROR_URL, cookie, baseUserAgent, 45_000, 64 * 1024 * 1024);
                status = mirror.status;
'''
replacement='''                String remoteDatasetVersion = "";
                String remoteUserRef = "";
                try {
                    HttpResult manifest = httpGet(DATASET_MANIFEST_URL, cookie, baseUserAgent, 20_000, 2 * 1024 * 1024);
                    if (manifest.status == 200 && manifest.body != null) {
                        JSONObject meta = new JSONObject(new String(manifest.body, StandardCharsets.UTF_8));
                        remoteDatasetVersion = meta.optString("datasetVersion", "");
                        remoteUserRef = meta.optString("userRef", "");
                        String cachedUser = store.getMeta("user_ref");
                        if (cachedUser != null && !cachedUser.isEmpty() && !remoteUserRef.isEmpty() && !cachedUser.equals(remoteUserRef)) {
                            store.clearUserData();
                        } else if (!refreshShell && !remoteDatasetVersion.isEmpty()
                                && remoteDatasetVersion.equals(store.getMeta("dataset_version"))
                                && store.getEndpoint("bootstrap") != null) {
                            store.setLastValidatedNow();
                            store.setMeta("last_sync_ms", String.valueOf(System.currentTimeMillis()));
                            if (callback != null) callback.onFinished(true, 200);
                            return;
                        }
                    }
                } catch (Throwable ignored) { }

                HttpResult mirror = httpGet(CORE_MIRROR_URL, cookie, baseUserAgent, 45_000, 64 * 1024 * 1024);
                status = mirror.status;
'''
if needle not in s: raise SystemExit("core mirror anchor missing")
s=s.replace(needle,replacement,1)
needle2='''                        store.setMeta("snapshot_version", root.optString("version", ""));
                        store.setMeta("user_ref", incomingUser);
'''
replacement2='''                        store.setMeta("snapshot_version", root.optString("version", ""));
                        store.setMeta("user_ref", incomingUser);
                        if (!remoteDatasetVersion.isEmpty()) store.setMeta("dataset_version", remoteDatasetVersion);
'''
if needle2 not in s: raise SystemExit("core meta anchor missing")
s=s.replace(needle2,replacement2,1)
p.write_text(s)

# Update manager class.
u=root/"app/src/main/java/vn/nah/iso15189suite/UpdateManager.java"
u.write_text(r'''package vn.nah.iso15189suite;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class UpdateManager {
    static final int CURRENT_VERSION_CODE = 11003;
    static final String CURRENT_VERSION = "1.1.3";
    static final String MANIFEST_URL = "https://sachyhoc.com/wp-json/nah-iso15189/v1/app/update-manifest";
    static final int REQUEST_UNKNOWN_APPS = 1803;

    private final Activity activity;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private long downloadId = -1L;
    private Uri pendingInstallUri;
    private String expectedSha = "";
    private boolean receiverRegistered = false;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) return;
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            if (id != downloadId) return;
            DownloadManager dm = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = dm.getUriForDownloadedFile(id);
            if (uri == null) {
                Toast.makeText(activity, "Không thể mở gói cập nhật đã tải.", Toast.LENGTH_LONG).show();
                return;
            }
            verifyAndInstall(uri);
        }
    };

    UpdateManager(Activity activity) {
        this.activity = activity;
        IntentFilter f = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) activity.registerReceiver(receiver, f, Context.RECEIVER_NOT_EXPORTED);
        else activity.registerReceiver(receiver, f);
        receiverRegistered = true;
    }

    void close() {
        if (receiverRegistered) {
            try { activity.unregisterReceiver(receiver); } catch (Throwable ignored) { }
            receiverRegistered = false;
        }
        executor.shutdownNow();
    }

    void injectCheckButton(WebView view) {
        if (view == null) return;
        String js = "(function(){if(document.getElementById('nahAndroidUpdateBtn'))return;"
                + "var host=document.querySelector('.login-version')||document.querySelector('#loginForm');"
                + "if(!host)return;var b=document.createElement('button');b.id='nahAndroidUpdateBtn';b.type='button';"
                + "b.textContent='Kiểm tra cập nhật';b.style.cssText='margin:10px auto 0;display:block;border:0;background:transparent;color:#087a43;font:600 13px system-ui;cursor:pointer';"
                + "b.onclick=function(){location.href='nahlab-update://check';};host.parentNode.insertBefore(b,host.nextSibling);})();";
        try { view.evaluateJavascript(js, null); } catch (Throwable ignored) { }
    }

    void check(boolean userInitiated) {
        executor.execute(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(MANIFEST_URL).openConnection();
                c.setConnectTimeout(12_000);
                c.setReadTimeout(18_000);
                c.setRequestProperty("Accept", "application/json");
                c.setRequestProperty("User-Agent", "NAHISOAndroid/1.1.3");
                int code = c.getResponseCode();
                if (code != 200) throw new IllegalStateException("HTTP " + code);
                byte[] body;
                try (InputStream in = c.getInputStream()) { body = readAll(in, 2 * 1024 * 1024); }
                JSONObject root = new JSONObject(new String(body, java.nio.charset.StandardCharsets.UTF_8));
                JSONObject android = root.optJSONObject("platforms") == null ? null : root.optJSONObject("platforms").optJSONObject("android");
                if (android == null || !android.optBoolean("available", false)) {
                    if (userInitiated) toast("Chưa có gói cập nhật Android mới trên kênh ổn định.");
                    return;
                }
                int remoteCode = android.optInt("versionCode", 0);
                if (remoteCode <= CURRENT_VERSION_CODE) {
                    if (userInitiated) toast("Ứng dụng Android đang ở phiên bản mới nhất.");
                    return;
                }
                String version = android.optString("version", "");
                String url = android.optString("downloadUrl", "");
                String sha = android.optString("sha256", "").toLowerCase(Locale.ROOT);
                String notes = android.optString("releaseNotes", "");
                if (!isSafeUpdateUrl(url) || !sha.matches("^[a-f0-9]{64}$")) throw new IllegalStateException("Invalid update metadata");
                activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
                        .setTitle("Có phiên bản NAH LAB SUITE mới")
                        .setMessage("Android v" + version + (notes.isEmpty() ? "" : "\n\n" + notes))
                        .setNegativeButton("Để sau", null)
                        .setPositiveButton("Tải và cài đặt", (d,w) -> download(url, sha, version))
                        .show());
            } catch (Throwable e) {
                if (userInitiated) toast("Chưa thể kiểm tra cập nhật. Vui lòng thử lại khi có kết nối mạng.");
            }
        });
    }

    private void download(String url, String sha, String version) {
        try {
            expectedSha = sha;
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
            req.setTitle("NAH LAB SUITE Android v" + version);
            req.setDescription("Đang tải bản cập nhật");
            req.setMimeType("application/vnd.android.package-archive");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, "NAH-LAB-SUITE-Android-v" + version + ".apk");
            DownloadManager dm = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            downloadId = dm.enqueue(req);
            toast("Đang tải bản cập nhật.");
        } catch (Throwable e) {
            toast("Không thể tải bản cập nhật.");
        }
    }

    private void verifyAndInstall(Uri uri) {
        executor.execute(() -> {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                try (InputStream in = activity.getContentResolver().openInputStream(uri)) {
                    if (in == null) throw new IllegalStateException("download missing");
                    byte[] buf = new byte[32 * 1024];
                    int n;
                    while ((n = in.read(buf)) >= 0) if (n > 0) md.update(buf, 0, n);
                }
                String got = hex(md.digest());
                if (!got.equalsIgnoreCase(expectedSha)) {
                    toast("Gói cập nhật không vượt qua kiểm tra toàn vẹn SHA-256.");
                    return;
                }
                activity.runOnUiThread(() -> install(uri));
            } catch (Throwable e) {
                toast("Không thể xác minh gói cập nhật.");
            }
        });
    }

    private void install(Uri uri) {
        pendingInstallUri = uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !activity.getPackageManager().canRequestPackageInstalls()) {
            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(settings, REQUEST_UNKNOWN_APPS);
            return;
        }
        launchInstaller(uri);
    }

    void onActivityResult(int requestCode) {
        if (requestCode != REQUEST_UNKNOWN_APPS || pendingInstallUri == null) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || activity.getPackageManager().canRequestPackageInstalls()) {
            launchInstaller(pendingInstallUri);
        }
    }

    private void launchInstaller(Uri uri) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri, "application/vnd.android.package-archive");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        try { activity.startActivity(i); }
        catch (Throwable e) { toast("Không thể mở trình cài đặt Android."); }
    }

    private boolean isSafeUpdateUrl(String url) {
        try {
            URI u = new URI(url);
            String h = u.getHost() == null ? "" : u.getHost().toLowerCase(Locale.ROOT);
            return "https".equalsIgnoreCase(u.getScheme())
                    && ("sachyhoc.com".equals(h) || "www.sachyhoc.com".equals(h));
        } catch (Throwable e) { return false; }
    }

    private void toast(String s) {
        activity.runOnUiThread(() -> Toast.makeText(activity, s, Toast.LENGTH_LONG).show());
    }

    private static byte[] readAll(InputStream in, int max) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] b = new byte[16 * 1024]; int n;
        while ((n = in.read(b)) >= 0) {
            if (n == 0) continue;
            out.write(b, 0, n);
            if (out.size() > max) throw new IllegalStateException("too large");
        }
        return out.toByteArray();
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format(Locale.ROOT, "%02x", x & 0xff));
        return sb.toString();
    }
}
''')

# MainActivity persistent vault + updater.
p=root/"app/src/main/java/vn/nah/iso15189suite/MainActivity.java"
s=p.read_text()
s=s.replace("NAHISOAndroid/1.1.2","NAHISOAndroid/1.1.3")
s=s.replace('offlineStore.ensureShellContract("android-v1.1.2-web-v1.46");',
            'offlineStore.ensureShellContract("android-v1.1.3-web-v1.47");')
s=s.replace("    private OfflineStore offlineStore;","    private OfflineStore offlineStore;\n    private UpdateManager updateManager;")
s=s.replace("        OfflineSyncJobService.schedule(this);",
            "        OfflineSyncJobService.schedule(this);\n        updateManager = new UpdateManager(this);\n        handler.postDelayed(() -> updateManager.check(false), 12_000L);",1)
s=s.replace("offlineStore.clearUserData();","offlineStore.revokeValidationKeepVault();")
# Different-user safety remains in HydrationManager where clearUserData is intentional.
# Restore those intentional mismatch clears in HydrationManager only; MainActivity has no mismatch logic.
s=s.replace('''            if (isInternalUrl(url)) return false;
            openExternal(url);
            return true;''',
'''            if (url.startsWith("nahlab-update://check")) {
                if (updateManager != null) updateManager.check(true);
                return true;
            }
            if (isInternalUrl(url)) return false;
            openExternal(url);
            return true;''',1)
s=s.replace("            injectLogoutCacheHook(view);",
            "            injectLogoutCacheHook(view);\n            if (updateManager != null) updateManager.injectCheckButton(view);",1)
s=s.replace('''        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || filePathCallback == null) return;''',
'''        super.onActivityResult(requestCode, resultCode, data);
        if (updateManager != null) updateManager.onActivityResult(requestCode);
        if (requestCode != FILE_CHOOSER_REQUEST || filePathCallback == null) return;''',1)
s=s.replace("        destroyWebView();\n        super.onDestroy();",
            "        if (updateManager != null) updateManager.close();\n        destroyWebView();\n        super.onDestroy();",1)
# Logout hook endpoint now revokes validation only.
s=s.replace("__android_offline_clear__","__android_offline_lock__")
p.write_text(s)

# Background sync: expired auth locks vault, never erases it.
p=root/"app/src/main/java/vn/nah/iso15189suite/OfflineSyncJobService.java"
s=p.read_text().replace("store.clearUserData();","store.revokeValidationKeepVault();")
p.write_text(s)

# README
p=root/"README.md"
p.write_text("""# NAH LAB SUITE Android v1.1.3 — Persistent Offline Vault & In-App Updater

- Parent: v1.1.2 Fast Core Hydration.
- Requires Web v1.47.
- Logout/session expiry revokes offline authorization but retains encrypted hydrated data and forms.
- Same account re-login reuses the vault immediately after online identity validation.
- Different account login clears the previous account vault before new hydration.
- Core sync checks dataset manifest first and skips mirror download when datasetVersion is unchanged.
- Full/deep hydration remains silent and persistent.
- Automatic update check uses Web v1.47 app/update-manifest.
- Update APK is downloaded in-app, SHA-256 verified, then handed to Android Package Installer.
- Android still requires user approval for package installation; stable in-place upgrades require one persistent NAH release signing key.

""" + p.read_text())

print("v1.1.3 patch complete")

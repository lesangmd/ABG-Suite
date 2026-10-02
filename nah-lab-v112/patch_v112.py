#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_v112.py <android_source_root>")

root = Path(sys.argv[1])

# Version.
p = root / "app/build.gradle"
s = p.read_text(encoding="utf-8")
s = s.replace("versionCode 11001", "versionCode 11002")
s = s.replace("versionName '1.1.1'", "versionName '1.1.2'")
p.write_text(s, encoding="utf-8")

# OfflineStore: merge lightweight core cache without deleting deep rows.
p = root / "app/src/main/java/vn/nah/iso15189suite/OfflineStore.java"
s = p.read_text(encoding="utf-8")
merge_method = r'''
    synchronized void mergeEndpointCache(JSONObject cache) throws Exception {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            java.util.Iterator<String> keys = cache.keys();
            long now = System.currentTimeMillis();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = cache.opt(key);
                if (value == null) continue;
                String json = value instanceof String ? JSONObject.quote((String) value) : String.valueOf(value);
                ContentValues cv = new ContentValues();
                cv.put("cache_key", key);
                cv.put("body", crypto.encrypt(json.getBytes(StandardCharsets.UTF_8)));
                cv.put("updated_at", now);
                db.insertWithOnConflict("endpoint_cache", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

'''
if "synchronized void mergeEndpointCache(JSONObject cache)" not in s:
    anchor = "    synchronized void putEndpoint(String key, String json) {"
    if anchor not in s:
        raise SystemExit("OfflineStore merge insertion anchor missing")
    s = s.replace(anchor, merge_method + anchor, 1)

accessors = r'''    synchronized long lastDeepSyncMs() {
        return getMetaLong("last_deep_sync_ms", 0L);
    }

    synchronized long lastContentSyncMs() {
        return getMetaLong("last_content_sync_ms", 0L);
    }

'''
if "synchronized long lastDeepSyncMs()" not in s:
    anchor = "    synchronized void ensureShellContract(String contract) {"
    if anchor not in s:
        raise SystemExit("OfflineStore accessor insertion anchor missing")
    s = s.replace(anchor, accessors + anchor, 1)

s = s.replace(
    'db.delete("meta", "meta_key IN (?,?,?,?)", new String[]{"last_validated_ms", "last_sync_ms", "user_ref", "snapshot_version"});',
    'db.delete("meta", "meta_key IN (?,?,?,?,?,?)", new String[]{"last_validated_ms", "last_sync_ms", "last_deep_sync_ms", "last_content_sync_ms", "user_ref", "snapshot_version"});'
)
p.write_text(s, encoding="utf-8")

# HydrationManager: staged core-first, deep-later model.
p = root / "app/src/main/java/vn/nah/iso15189suite/HydrationManager.java"
p.write_text(r'''package vn.nah.iso15189suite;

import android.content.Context;
import android.net.Uri;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class HydrationManager {
    static final String APP_URL = "https://sachyhoc.com/nah-lab-iso/";
    static final String REST_BASE = "https://sachyhoc.com/wp-json/nah-iso15189/v1/";
    static final String CORE_MIRROR_URL = REST_BASE + "desktop/mirror/full?scope=core";
    static final String FULL_MIRROR_URL = REST_BASE + "desktop/mirror/full";
    static final String CONTENT_URL = REST_BASE + "desktop/content/bundle";
    static final String MANIFEST_URL = REST_BASE + "desktop/content/manifest";
    static final String UA_MARKER = "NAHISOAndroid/1.1.2";

    static final long CORE_MIN_REFRESH_MS = 60L * 1000L;
    static final long DEEP_REFRESH_MS = 12L * 60L * 60L * 1000L;
    private static final long CONTENT_REFRESH_MS = 24L * 60L * 60L * 1000L;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Pattern ASSET_PATTERN = Pattern.compile("(?:src|href)=[\\\"']([^\\\"']+)[\\\"']", Pattern.CASE_INSENSITIVE);
    private static final int MAX_RESOURCE_BYTES = 3 * 1024 * 1024;
    private static final int MAX_SHELL_TOTAL_BYTES = 18 * 1024 * 1024;

    interface Callback {
        void onFinished(boolean success, int httpStatus);
    }

    private static final class HttpResult {
        int status;
        String contentType;
        byte[] body;
        String finalUrl;
    }

    static void syncCore(Context context, OfflineStore store, String cookie, String baseUserAgent, boolean refreshShell, Callback callback) {
        EXECUTOR.execute(() -> {
            int status = 0;
            boolean success = false;
            try {
                if (refreshShell || !store.hasShell(APP_URL) || System.currentTimeMillis() - store.lastShellSyncMs() > 24L * 60L * 60L * 1000L) {
                    try { refreshShell(store, cookie, baseUserAgent); } catch (Throwable ignored) { }
                }
                if (cookie == null || cookie.trim().isEmpty()) {
                    if (callback != null) callback.onFinished(false, 401);
                    return;
                }

                HttpResult mirror = httpGet(CORE_MIRROR_URL, cookie, baseUserAgent, 45_000, 64 * 1024 * 1024);
                status = mirror.status;
                if (mirror.status == 200 && mirror.body != null) {
                    JSONObject root = new JSONObject(new String(mirror.body, StandardCharsets.UTF_8));
                    JSONObject endpointCache = root.optJSONObject("endpointCache");
                    if (endpointCache != null && endpointCache.length() > 0) {
                        String incomingUser = root.optString("userRef", "");
                        String cachedUser = store.getMeta("user_ref");
                        if (cachedUser != null && !cachedUser.isEmpty() && !incomingUser.isEmpty() && !cachedUser.equals(incomingUser)) {
                            store.clearUserData();
                        }
                        store.mergeEndpointCache(endpointCache);
                        store.putEndpoint("__core_mirror_meta__", root.toString());
                        store.setMeta("snapshot_version", root.optString("version", ""));
                        store.setMeta("user_ref", incomingUser);
                        store.setMeta("last_sync_ms", String.valueOf(System.currentTimeMillis()));
                        store.setLastValidatedNow();
                        success = true;
                    }
                }

                if (success && System.currentTimeMillis() - store.lastContentSyncMs() > CONTENT_REFRESH_MS) {
                    refreshStaticContent(store, cookie, baseUserAgent);
                }
            } catch (Throwable ignored) {
                success = false;
            }
            if (callback != null) callback.onFinished(success, status);
        });
    }

    static void syncDeep(Context context, OfflineStore store, String cookie, String baseUserAgent, boolean force, Callback callback) {
        EXECUTOR.execute(() -> {
            int status = 0;
            boolean success = false;
            try {
                if (!force && store.lastDeepSyncMs() > 0L && System.currentTimeMillis() - store.lastDeepSyncMs() < DEEP_REFRESH_MS) {
                    if (callback != null) callback.onFinished(true, 200);
                    return;
                }
                if (cookie == null || cookie.trim().isEmpty()) {
                    if (callback != null) callback.onFinished(false, 401);
                    return;
                }

                HttpResult mirror = httpGet(FULL_MIRROR_URL, cookie, baseUserAgent, 180_000, 96 * 1024 * 1024);
                status = mirror.status;
                if (mirror.status == 200 && mirror.body != null) {
                    JSONObject root = new JSONObject(new String(mirror.body, StandardCharsets.UTF_8));
                    JSONObject endpointCache = root.optJSONObject("endpointCache");
                    if (endpointCache != null && endpointCache.length() > 0) {
                        store.mergeEndpointCache(endpointCache);
                        store.putEndpoint("__full_mirror_meta__", root.toString());
                        store.setMeta("snapshot_version", root.optString("version", ""));
                        store.setMeta("user_ref", root.optString("userRef", ""));
                        long now = System.currentTimeMillis();
                        store.setMeta("last_sync_ms", String.valueOf(now));
                        store.setMeta("last_deep_sync_ms", String.valueOf(now));
                        store.setLastValidatedNow();
                        success = true;
                    }
                }
            } catch (Throwable ignored) {
                success = false;
            }
            if (callback != null) callback.onFinished(success, status);
        });
    }

    private static void refreshStaticContent(OfflineStore store, String cookie, String baseUserAgent) {
        boolean any = false;
        try {
            HttpResult content = httpGet(CONTENT_URL, cookie, baseUserAgent, 60_000, 64 * 1024 * 1024);
            if (content.status == 200 && content.body != null) {
                store.putEndpoint("__content_bundle__", new String(content.body, StandardCharsets.UTF_8));
                any = true;
            }
        } catch (Throwable ignored) { }
        try {
            HttpResult manifest = httpGet(MANIFEST_URL, cookie, baseUserAgent, 30_000, MAX_RESOURCE_BYTES + 1024);
            if (manifest.status == 200 && manifest.body != null) {
                store.putEndpoint("__content_manifest__", new String(manifest.body, StandardCharsets.UTF_8));
                any = true;
            }
        } catch (Throwable ignored) { }
        if (any) store.setMeta("last_content_sync_ms", String.valueOf(System.currentTimeMillis()));
    }

    private static void refreshShell(OfflineStore store, String cookie, String baseUserAgent) throws Exception {
        HttpResult shell = httpGet(APP_URL, cookie, baseUserAgent, 30_000, MAX_RESOURCE_BYTES + 1024);
        if (shell.status != 200 || shell.body == null || shell.body.length == 0) return;
        store.putShell(APP_URL, mimeOnly(shell.contentType, "text/html"), charsetOf(shell.contentType), shell.body);
        String html = new String(shell.body, StandardCharsets.UTF_8);
        Matcher m = ASSET_PATTERN.matcher(html);
        Set<String> urls = new HashSet<>();
        while (m.find() && urls.size() < 40) {
            String ref = m.group(1);
            if (ref == null || ref.startsWith("data:") || ref.startsWith("javascript:")) continue;
            try {
                URL abs = new URL(new URL(APP_URL), ref);
                if (!"https".equalsIgnoreCase(abs.getProtocol())) continue;
                if (!"sachyhoc.com".equalsIgnoreCase(abs.getHost()) && !"www.sachyhoc.com".equalsIgnoreCase(abs.getHost())) continue;
                String path = abs.getPath().toLowerCase(Locale.ROOT);
                if (!(path.endsWith(".js") || path.endsWith(".css") || path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".svg") || path.endsWith(".webmanifest") || path.endsWith("service-worker.js"))) continue;
                urls.add(abs.toString());
            } catch (Throwable ignored) { }
        }

        int total = shell.body.length;
        for (String url : urls) {
            if (total >= MAX_SHELL_TOTAL_BYTES) break;
            try {
                HttpResult r = httpGet(url, cookie, baseUserAgent, 30_000, MAX_RESOURCE_BYTES + 1024);
                if (r.status != 200 || r.body == null || r.body.length == 0 || r.body.length > MAX_RESOURCE_BYTES) continue;
                total += r.body.length;
                if (total > MAX_SHELL_TOTAL_BYTES) break;
                store.putShell(url, mimeOnly(r.contentType, guessMime(url)), charsetOf(r.contentType), r.body);
            } catch (Throwable ignored) { }
        }
        store.setMeta("last_shell_sync_ms", String.valueOf(System.currentTimeMillis()));
    }

    private static HttpResult httpGet(String url, String cookie, String baseUserAgent, int timeoutMs, int maxBytes) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setInstanceFollowRedirects(true);
        c.setConnectTimeout(Math.min(timeoutMs, 20_000));
        c.setReadTimeout(timeoutMs);
        c.setRequestMethod("GET");
        c.setRequestProperty("Accept-Encoding", "identity");
        c.setRequestProperty("Accept", "application/json,text/html,application/javascript,text/css,image/*,*/*;q=0.8");
        c.setRequestProperty("Cache-Control", "no-cache");
        String ua = (baseUserAgent == null ? "" : baseUserAgent.trim());
        if (!ua.contains(UA_MARKER)) ua = ua + " " + UA_MARKER;
        c.setRequestProperty("User-Agent", ua.trim());
        if (cookie != null && !cookie.trim().isEmpty()) c.setRequestProperty("Cookie", cookie);
        c.connect();
        HttpResult r = new HttpResult();
        r.status = c.getResponseCode();
        r.contentType = c.getContentType();
        r.finalUrl = c.getURL().toString();
        InputStream in = r.status >= 400 ? c.getErrorStream() : c.getInputStream();
        if (in != null) {
            try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[16 * 1024];
                int n;
                while ((n = input.read(buf)) >= 0) {
                    if (n == 0) continue;
                    out.write(buf, 0, n);
                    if (out.size() > maxBytes) throw new IllegalStateException("Response too large");
                }
                r.body = out.toByteArray();
            }
        }
        c.disconnect();
        return r;
    }

    static String normalizeRestKey(String fullUrl) {
        if (fullUrl == null || !fullUrl.startsWith(REST_BASE)) return null;
        String rel = fullUrl.substring(REST_BASE.length());
        rel = rel.replaceAll("([?&])(?:_ts|ts)=\\d+(&|$)", "$1");
        rel = rel.replace("?&", "?");
        while (rel.endsWith("?") || rel.endsWith("&")) rel = rel.substring(0, rel.length() - 1);
        return rel;
    }

    static String canonicalShellUrl(String url) {
        if (url == null) return null;
        try {
            Uri u = Uri.parse(url);
            if (("sachyhoc.com".equalsIgnoreCase(u.getHost()) || "www.sachyhoc.com".equalsIgnoreCase(u.getHost()))
                    && "/nah-lab-iso/".equals(u.getPath())) return APP_URL;
            int hash = url.indexOf('#');
            return hash >= 0 ? url.substring(0, hash) : url;
        } catch (Throwable ignored) {
            return url;
        }
    }

    private static String mimeOnly(String contentType, String fallback) {
        if (contentType == null || contentType.trim().isEmpty()) return fallback;
        int p = contentType.indexOf(';');
        return (p >= 0 ? contentType.substring(0, p) : contentType).trim();
    }

    private static String charsetOf(String contentType) {
        if (contentType == null) return null;
        Matcher m = Pattern.compile("charset=([^; ]+)", Pattern.CASE_INSENSITIVE).matcher(contentType);
        return m.find() ? m.group(1).replace("\"", "").trim() : null;
    }

    private static String guessMime(String url) {
        String u = url.toLowerCase(Locale.ROOT);
        if (u.contains(".js")) return "application/javascript";
        if (u.contains(".css")) return "text/css";
        if (u.contains(".png")) return "image/png";
        if (u.contains(".jpg") || u.contains(".jpeg")) return "image/jpeg";
        if (u.contains(".svg")) return "image/svg+xml";
        if (u.contains(".webmanifest")) return "application/manifest+json";
        return "application/octet-stream";
    }
}
''', encoding="utf-8")

# MainActivity.
p = root / "app/src/main/java/vn/nah/iso15189suite/MainActivity.java"
s = p.read_text(encoding="utf-8")
s = s.replace("private static final long FOREGROUND_SYNC_MS = 5L * 60L * 1000L;",
              "private static final long FOREGROUND_SYNC_MS = 5L * 60L * 1000L;\n    private static final long INITIAL_DEEP_SYNC_DELAY_MS = 30L * 1000L;")
s = s.replace("private boolean syncInFlight = false;",
              "private boolean coreSyncInFlight = false;\n    private boolean deepSyncInFlight = false;")
s = s.replace("requestSilentHydration(false);", "requestSilentCore(false);")
s = s.replace(
    "requestSilentHydration(true);",
    "requestSilentCore(true);\n                handler.postDelayed(() -> requestDeepHydration(false), INITIAL_DEEP_SYNC_DELAY_MS);"
)
s = s.replace("if (earlySyncProbe >= 24) return;", "if (earlySyncProbe >= 60) return;")
s = s.replace("handler.postDelayed(this, 5000L);", "handler.postDelayed(this, 1000L);")
s = s.replace('offlineStore.ensureShellContract("android-v1.1.1-web-v1.45");',
              'offlineStore.ensureShellContract("android-v1.1.2-web-v1.46");')
s = s.replace("handler.postDelayed(loginCookieProbe, 3500L);",
              "handler.postDelayed(loginCookieProbe, 1200L);\n        handler.postDelayed(() -> requestDeepHydration(false), INITIAL_DEEP_SYNC_DELAY_MS);")
s = s.replace("NAHISOAndroid/1.1.1", "NAHISOAndroid/1.1.2")

start = s.find("    private void requestSilentHydration(boolean refreshShell) {")
if start < 0:
    raise SystemExit("requestSilentHydration block missing")
end = s.find("\n    private WebResourceResponse cachedJson", start)
if end < 0:
    raise SystemExit("cachedJson anchor missing")
new_methods = r'''    private void requestSilentCore(boolean refreshShell) {
        if (coreSyncInFlight || !networkAvailable()) return;
        if (!refreshShell && offlineStore.lastSyncMs() > 0L
                && System.currentTimeMillis() - offlineStore.lastSyncMs() < HydrationManager.CORE_MIN_REFRESH_MS) return;
        String cookie = currentCookie();
        if (cookie == null || cookie.trim().isEmpty()) return;
        String ua = webView != null ? webView.getSettings().getUserAgentString() : HydrationManager.UA_MARKER;
        coreSyncInFlight = true;
        HydrationManager.syncCore(
                this,
                offlineStore,
                cookie,
                ua,
                refreshShell,
                (success, status) -> runOnUiThread(() -> {
                    coreSyncInFlight = false;
                    if ((status == 401 || status == 403) && offlineStore.lastSyncMs() > 0) {
                        offlineStore.clearUserData();
                    }
                }));
    }

    private void requestDeepHydration(boolean force) {
        if (deepSyncInFlight || coreSyncInFlight || !networkAvailable()) return;
        if (!force && offlineStore.lastDeepSyncMs() > 0L
                && System.currentTimeMillis() - offlineStore.lastDeepSyncMs() < HydrationManager.DEEP_REFRESH_MS) return;
        String cookie = currentCookie();
        if (cookie == null || cookie.trim().isEmpty()) return;
        String ua = webView != null ? webView.getSettings().getUserAgentString() : HydrationManager.UA_MARKER;
        deepSyncInFlight = true;
        HydrationManager.syncDeep(
                this,
                offlineStore,
                cookie,
                ua,
                force,
                (success, status) -> runOnUiThread(() -> {
                    deepSyncInFlight = false;
                    if ((status == 401 || status == 403) && offlineStore.lastSyncMs() > 0) {
                        offlineStore.clearUserData();
                    }
                }));
    }
'''
s = s[:start] + new_methods + s[end:]
s = s.replace("handler.postDelayed(() -> requestSilentCore(false), 1500L);",
              "handler.postDelayed(() -> requestSilentCore(false), 600L);")
s = s.replace("handler.postDelayed(() -> requestSilentHydration(false), 1500L);",
              "handler.postDelayed(() -> requestSilentCore(false), 600L);")
s = s.replace("handler.postDelayed(() -> requestSilentHydration(false), 400L);",
              "handler.postDelayed(() -> requestSilentCore(false), 400L);\n        handler.postDelayed(() -> requestDeepHydration(false), 10_000L);")
s = s.replace("// v1.1.1: invalidate only cached WebApp shell when the native/web transport",
              "// v1.1.2: invalidate only cached WebApp shell when the native/web transport")
p.write_text(s, encoding="utf-8")

# Background job: frequent core refresh, deep refresh only when stale.
p = root / "app/src/main/java/vn/nah/iso15189suite/OfflineSyncJobService.java"
s = p.read_text(encoding="utf-8")
start = s.find("        HydrationManager.syncAll(")
if start < 0:
    raise SystemExit("OfflineSyncJobService syncAll block missing")
end = s.find("        return true;", start)
if end < 0:
    raise SystemExit("OfflineSyncJobService return anchor missing")
block = r'''        final String syncCookie = cookie;
        HydrationManager.syncCore(
                this,
                store,
                syncCookie,
                HydrationManager.UA_MARKER,
                false,
                (success, status) -> {
                    if (status == 401 || status == 403) {
                        store.clearUserData();
                        jobFinished(params, false);
                        return;
                    }
                    if (success && (store.lastDeepSyncMs() == 0L
                            || System.currentTimeMillis() - store.lastDeepSyncMs() >= HydrationManager.DEEP_REFRESH_MS)) {
                        HydrationManager.syncDeep(
                                this,
                                store,
                                syncCookie,
                                HydrationManager.UA_MARKER,
                                false,
                                (deepSuccess, deepStatus) -> {
                                    if (deepStatus == 401 || deepStatus == 403) store.clearUserData();
                                    jobFinished(params, false);
                                });
                    } else {
                        jobFinished(params, false);
                    }
                });
'''
s = s[:start] + block + s[end:]
p.write_text(s, encoding="utf-8")

# README release note.
p = root / "README.md"
old = p.read_text(encoding="utf-8")
p.write_text("""# NAH LAB SUITE Android v1.1.2 — Fast Core Hydration & Deferred Deep Sync

- Parent: v1.1.1.
- Requires Web v1.46 for compact scope=core hydration.
- Immediate post-login/startup hydration stores Dashboard, bootstrap, module lists and common overview/knowledge data first.
- Full deep hydration is deferred until after initial UI use, then refreshed on a 12-hour cadence.
- 5-minute foreground and 15-minute background refreshes use the compact core mirror rather than rebuilding the full mirror.
- Static content bundle refresh is limited to a 24-hour cadence.
- Core rows merge into SQLite so deep detail cache is preserved.
- Retains AES/GCM Android Keystore encryption, 30-day validated offline reads, server-canonical writes, original approved launcher logo and silent synchronization.

""" + old, encoding="utf-8")

# Static guards.
main = (root / "app/src/main/java/vn/nah/iso15189suite/MainActivity.java").read_text(encoding="utf-8")
if "requestSilentHydration" in main:
    raise SystemExit("stale requestSilentHydration reference remains")
if 'android-v1.1.2-web-v1.46' not in main:
    raise SystemExit("shell contract missing")
if "NAHISOAndroid/1.1.2" not in main:
    raise SystemExit("MainActivity UA missing")

print("v1.1.2 patch complete")

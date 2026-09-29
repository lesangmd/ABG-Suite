package com.medipharm.abgsuite;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.SafeBrowsingResponse;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MainActivity extends Activity {
    private static final String START_URL = "https://www.sachyhoc.com/phan-tich-khi-mau-app/";
    private static final String REGISTER_URL = "https://www.sachyhoc.com/dangky";
    private static final String PRIMARY_HOST = "www.sachyhoc.com";
    private static final String ALT_HOST = "sachyhoc.com";
    private static final String APP_SCHEME = "medipharmabg";
    private static final String UPDATE_API = "https://api.github.com/repos/lesangmd/ABG-Suite/contents?ref=main";
    private static final Pattern APK_PATTERN = Pattern.compile("^MEDIPHARM-ABG-v(\\d+)\\.(\\d+)\\.(\\d+)\\.apk$");

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private WebView webView;
    private boolean authFlowActive = false;

    private final Runnable authCookiePoll = new Runnable() {
        @Override
        public void run() {
            if (!authFlowActive || webView == null) return;
            if (hasWordPressLoginCookie()) {
                authFlowActive = false;
                CookieManager.getInstance().flush();
                webView.loadUrl(START_URL);
                Toast.makeText(MainActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                return;
            }
            mainHandler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(11, 102, 116));
        getWindow().setNavigationBarColor(Color.rgb(243, 247, 249));

        FrameLayout root = new FrameLayout(this);
        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        setContentView(root);
        configureWebView();

        if (savedInstanceState == null) {
            webView.loadUrl(START_URL);
        } else {
            authFlowActive = savedInstanceState.getBoolean("authFlowActive", false);
            webView.restoreState(savedInstanceState);
            if (authFlowActive) startAuthCookiePolling();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setUserAgentString(s.getUserAgentString() + " MEDIPHARMABGAndroid/" + BuildConfig.VERSION_NAME);
        WebView.setWebContentsDebuggingEnabled(false);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
                String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);

                if (APP_SCHEME.equals(scheme)) {
                    return handleAppAction(uri);
                }

                if (("https".equals(scheme) || "http".equals(scheme)) &&
                        (PRIMARY_HOST.equals(host) || ALT_HOST.equals(host))) {
                    return false;
                }

                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (authFlowActive && hasWordPressLoginCookie()) {
                    authFlowActive = false;
                    mainHandler.removeCallbacks(authCookiePoll);
                    if (!START_URL.equals(url)) {
                        view.loadUrl(START_URL);
                    }
                } else if (authFlowActive && START_URL.equals(url)) {
                    authFlowActive = false;
                    mainHandler.removeCallbacks(authCookiePoll);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    showNetworkError();
                }
            }

            @Override
            public void onSafeBrowsingHit(WebView view, WebResourceRequest request,
                                          int threatType, SafeBrowsingResponse callback) {
                callback.backToSafety(true);
            }
        });
    }

    private boolean handleAppAction(Uri uri) {
        String action = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        switch (action) {
            case "login":
                beginAuthFlow(START_URL + "?access=member");
                return true;
            case "register":
                beginAuthFlow(REGISTER_URL);
                return true;
            case "check-update":
                checkForUpdates();
                return true;
            default:
                return true;
        }
    }

    private void beginAuthFlow(String url) {
        authFlowActive = true;
        startAuthCookiePolling();
        webView.loadUrl(url);
    }

    private void startAuthCookiePolling() {
        mainHandler.removeCallbacks(authCookiePoll);
        mainHandler.post(authCookiePoll);
    }

    private boolean hasWordPressLoginCookie() {
        String cookie = CookieManager.getInstance().getCookie(START_URL);
        return cookie != null && cookie.contains("wordpress_logged_in_");
    }

    private void checkForUpdates() {
        Toast.makeText(this, "Đang kiểm tra cập nhật…", Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(UPDATE_API).openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("User-Agent", "KhiMauAndroid/" + BuildConfig.VERSION_NAME);

                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) {
                    throw new IllegalStateException("HTTP " + code);
                }

                String json = readAll(connection.getInputStream());
                JSONArray files = new JSONArray(json);
                UpdateInfo latest = null;

                for (int i = 0; i < files.length(); i++) {
                    JSONObject item = files.optJSONObject(i);
                    if (item == null || !"file".equals(item.optString("type"))) continue;
                    String name = item.optString("name");
                    Matcher matcher = APK_PATTERN.matcher(name);
                    if (!matcher.matches()) continue;

                    Version version = new Version(
                            Integer.parseInt(matcher.group(1)),
                            Integer.parseInt(matcher.group(2)),
                            Integer.parseInt(matcher.group(3))
                    );
                    String downloadUrl = item.optString("download_url");
                    if (downloadUrl.isEmpty()) continue;

                    if (latest == null || version.compareTo(latest.version) > 0) {
                        latest = new UpdateInfo(version, name, downloadUrl);
                    }
                }

                UpdateInfo result = latest;
                mainHandler.post(() -> presentUpdateResult(result));
            } catch (Exception error) {
                mainHandler.post(() -> new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Cập nhật ứng dụng")
                        .setMessage("Chưa kiểm tra được phiên bản mới. Vui lòng kiểm tra kết nối Internet và thử lại.")
                        .setPositiveButton("Đóng", null)
                        .show());
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void presentUpdateResult(UpdateInfo latest) {
        Version current = Version.parse(BuildConfig.VERSION_NAME);
        if (latest == null || latest.version.compareTo(current) <= 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Cập nhật ứng dụng")
                    .setMessage("Khí Máu v" + BuildConfig.VERSION_NAME + " đang là phiên bản mới nhất.")
                    .setPositiveButton("Đóng", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Có phiên bản mới")
                .setMessage("Khí Máu v" + latest.version + " đã có trên GitHub. Tải xuống để cập nhật từ v" + BuildConfig.VERSION_NAME + ".")
                .setNegativeButton("Để sau", null)
                .setPositiveButton("Tải xuống", (dialog, which) -> downloadApk(latest))
                .show();
    }

    private void downloadApk(UpdateInfo update) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(update.downloadUrl));
            request.setTitle("Khí Máu v" + update.version);
            request.setDescription("Đang tải bản cập nhật");
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, update.fileName);

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);
            Toast.makeText(this, "Đã bắt đầu tải xuống. Mở thông báo tải về để cài đặt.", Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl)));
            } catch (Exception ignored) {
                Toast.makeText(this, "Không thể mở liên kết tải xuống.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private static String readAll(InputStream input) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) builder.append(line);
        }
        return builder.toString();
    }

    private void showNetworkError() {
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<style>body{font-family:sans-serif;background:#f3f7f9;color:#17363d;display:flex;min-height:100vh;" +
                "align-items:center;justify-content:center;margin:0;padding:24px;box-sizing:border-box}.c{max-width:520px;text-align:center}" +
                "button{border:0;border-radius:12px;padding:12px 18px;background:#0b6674;color:white;font-size:16px}</style></head>" +
                "<body><div class='c'><h2>Không thể kết nối</h2><p>Kiểm tra kết nối Internet rồi thử lại.</p>" +
                "<button onclick=\"location.href='" + START_URL + "'\">Thử lại</button></div></body></html>";
        webView.loadDataWithBaseURL(START_URL, html, "text/html", "UTF-8", null);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        outState.putBoolean("authFlowActive", authFlowActive);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
        if (authFlowActive) startAuthCookiePolling();
    }

    @Override
    protected void onDestroy() {
        authFlowActive = false;
        mainHandler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private static final class UpdateInfo {
        final Version version;
        final String fileName;
        final String downloadUrl;

        UpdateInfo(Version version, String fileName, String downloadUrl) {
            this.version = version;
            this.fileName = fileName;
            this.downloadUrl = downloadUrl;
        }
    }

    private static final class Version implements Comparable<Version> {
        final int major;
        final int minor;
        final int patch;

        Version(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        static Version parse(String value) {
            Matcher matcher = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$").matcher(value == null ? "" : value);
            if (!matcher.matches()) return new Version(0, 0, 0);
            return new Version(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))
            );
        }

        @Override
        public int compareTo(Version other) {
            if (major != other.major) return Integer.compare(major, other.major);
            if (minor != other.minor) return Integer.compare(minor, other.minor);
            return Integer.compare(patch, other.patch);
        }

        @Override
        public String toString() {
            return major + "." + minor + "." + patch;
        }
    }
}

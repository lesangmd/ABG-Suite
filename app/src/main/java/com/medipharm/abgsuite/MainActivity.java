package com.medipharm.abgsuite;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.SafeBrowsingResponse;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

public final class MainActivity extends Activity {
    private static final String LOGIN_ENDPOINT = "https://www.sachyhoc.com/wp-admin/admin-ajax.php?action=medipharm_abg_android_login";
    private static final String REGISTER_URL = "https://www.sachyhoc.com/dangky";
    private static final String UPDATE_API = "https://api.github.com/repos/lesangmd/ABG-Suite/contents?ref=main";
    private static final String LOCAL_BASE = "https://app.medipharm.local/";
    private static final String APP_SCHEME = "medipharmabg";
    private static final String DB_ASSET = "abg_offline.db";
    private static final String DB_FILE = "abg_offline.db";
    private static final String PREFS = "khi_mau_offline";
    private static final String PREF_ENTITLEMENT = "entitlement_payload";
    private static final String PREF_SIGNATURE = "entitlement_signature";
    private static final String KEY_ALIAS = "khi_mau_entitlement_hmac_v1";
    private static final Pattern APK_PATTERN = Pattern.compile("^MEDIPHARM-ABG-v(\\d+)\\.(\\d+)\\.(\\d+)\\.apk$");
    private static final String BRAND_ICON_DATA_URI = "data:image/webp;base64,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";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private WebView webView;
    private boolean localRuntime;
    private String dataVersion = "";
    private Dialog loginDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(7, 74, 168));
        getWindow().setNavigationBarColor(Color.rgb(255, 255, 255));

        FrameLayout root = new FrameLayout(this);
        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        setContentView(root);
        configureWebView();

        try {
            installBundledDatabase();
        } catch (Exception e) {
            showFatalLocalError("Không mở được dữ liệu ngoại tuyến của Khí Máu.");
            return;
        }

        if (getValidEntitlement() != null) {
            loadOfflineRuntime();
        } else {
            showNativeLogin(null);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
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
        s.setUserAgentString(s.getUserAgentString() + " MEDIPHARMABGAndroid/" + BuildConfig.VERSION_NAME + " OfflineFirst/1");
        WebView.setWebContentsDebuggingEnabled(false);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
                if (APP_SCHEME.equals(scheme)) {
                    return handleAppAction(uri);
                }
                if (localRuntime && ("https".equals(scheme) || "http".equals(scheme))) {
                    openExternal(uri);
                    return true;
                }
                return !"https".equals(scheme) && !"http".equals(scheme);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (localRuntime) {
                    Uri uri = request.getUrl();
                    String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
                    String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
                    if (("http".equals(scheme) || "https".equals(scheme)) && !"app.medipharm.local".equals(host)) {
                        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
                    }
                }
                return super.shouldInterceptRequest(view, request);
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
            case "account":
                showAccountDialog();
                return true;
            case "logout":
                logoutAndAuthenticate();
                return true;
            case "check-update":
                checkForUpdates();
                return true;
            case "login":
            case "retry-auth":
                showNativeLogin(null);
                return true;
            case "register":
                openExternal(Uri.parse(REGISTER_URL));
                return true;
            default:
                return true;
        }
    }

    private void showNativeLogin(String initialMessage) {
        localRuntime = false;
        if (loginDialog != null && loginDialog.isShowing()) {
            loginDialog.dismiss();
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(247, 251, 254));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(dp(24), dp(30), dp(24), dp(34));
        scroll.addView(page, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.app_icon_brand);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable logoBg = roundedDrawable(Color.WHITE, Color.rgb(220, 233, 243), 28);
        logo.setBackground(logoBg);
        logo.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(132), dp(132));
        logoLp.bottomMargin = dp(10);
        page.addView(logo, logoLp);

        TextView brand = new TextView(this);
        brand.setText("MEDIPHARM ABG");
        brand.setTextSize(25f);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.setTextColor(Color.rgb(7, 87, 164));
        brand.setGravity(Gravity.CENTER);
        page.addView(brand);

        TextView title = new TextView(this);
        title.setText("Đăng nhập");
        title.setTextSize(30f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.rgb(16, 42, 67));
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleLp.topMargin = dp(14);
        page.addView(title, titleLp);

        TextView subtitle = new TextView(this);
        subtitle.setText("Truy cập MEDIPHARM ABG");
        subtitle.setTextSize(16f);
        subtitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        subtitle.setTextColor(Color.rgb(39, 82, 118));
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subtitleLp.topMargin = dp(6);
        page.addView(subtitle, subtitleLp);

        TextView intro = new TextView(this);
        intro.setText("Xác thực tài khoản một lần để sử dụng các công cụ khí máu và nội dung học tập ngoại tuyến.");
        intro.setTextSize(14f);
        intro.setTextColor(Color.rgb(105, 128, 147));
        intro.setGravity(Gravity.CENTER);
        intro.setLineSpacing(0f, 1.18f);
        LinearLayout.LayoutParams introLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        introLp.topMargin = dp(8);
        introLp.bottomMargin = dp(20);
        page.addView(intro, introLp);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(roundedDrawable(Color.WHITE, Color.rgb(214, 229, 240), 22));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        page.addView(card, cardLp);

        TextView loginLabel = new TextView(this);
        loginLabel.setText("Tên đăng nhập hoặc email");
        loginLabel.setTextSize(14f);
        loginLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        loginLabel.setTextColor(Color.rgb(28, 53, 75));
        card.addView(loginLabel);

        EditText loginField = new EditText(this);
        loginField.setHint("Nhập tên đăng nhập hoặc email");
        loginField.setHintTextColor(Color.rgb(151, 169, 183));
        loginField.setTextColor(Color.rgb(23, 54, 79));
        loginField.setTextSize(16f);
        loginField.setSingleLine(true);
        loginField.setPadding(dp(14), 0, dp(14), 0);
        loginField.setBackground(roundedDrawable(Color.rgb(251, 253, 255), Color.rgb(207, 224, 236), 12));
        loginField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        LinearLayout.LayoutParams fieldLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        fieldLp.topMargin = dp(7);
        fieldLp.bottomMargin = dp(15);
        card.addView(loginField, fieldLp);

        TextView passwordLabel = new TextView(this);
        passwordLabel.setText("Mật khẩu");
        passwordLabel.setTextSize(14f);
        passwordLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        passwordLabel.setTextColor(Color.rgb(28, 53, 75));
        card.addView(passwordLabel);

        LinearLayout passwordRow = new LinearLayout(this);
        passwordRow.setOrientation(LinearLayout.HORIZONTAL);
        passwordRow.setGravity(Gravity.CENTER_VERTICAL);
        passwordRow.setPadding(dp(2), 0, dp(4), 0);
        passwordRow.setBackground(roundedDrawable(Color.rgb(251, 253, 255), Color.rgb(207, 224, 236), 12));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        rowLp.topMargin = dp(7);
        card.addView(passwordRow, rowLp);

        EditText passwordField = new EditText(this);
        passwordField.setHint("Nhập mật khẩu");
        passwordField.setHintTextColor(Color.rgb(151, 169, 183));
        passwordField.setTextColor(Color.rgb(23, 54, 79));
        passwordField.setTextSize(16f);
        passwordField.setSingleLine(true);
        passwordField.setPadding(dp(12), 0, dp(8), 0);
        passwordField.setBackgroundColor(Color.TRANSPARENT);
        passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout.LayoutParams passLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        passwordRow.addView(passwordField, passLp);

        Button revealButton = new Button(this);
        revealButton.setText("Hiện");
        revealButton.setTextSize(13f);
        revealButton.setAllCaps(false);
        revealButton.setTextColor(Color.rgb(7, 87, 164));
        revealButton.setBackground(roundedDrawable(Color.WHITE, Color.rgb(216, 229, 239), 10));
        LinearLayout.LayoutParams revealLp = new LinearLayout.LayoutParams(dp(72), dp(42));
        revealLp.rightMargin = dp(2);
        passwordRow.addView(revealButton, revealLp);
        revealButton.setOnClickListener(v -> {
            boolean visible = (passwordField.getInputType() & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
            passwordField.setInputType(InputType.TYPE_CLASS_TEXT | (visible ? InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD));
            revealButton.setText(visible ? "Hiện" : "Ẩn");
            passwordField.setSelection(passwordField.length());
        });

        TextView status = new TextView(this);
        status.setText(initialMessage == null ? "" : initialMessage);
        status.setTextSize(13f);
        status.setTextColor(Color.rgb(176, 54, 62));
        status.setGravity(Gravity.CENTER);
        status.setVisibility(initialMessage == null || initialMessage.isEmpty() ? View.GONE : View.VISIBLE);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statusLp.topMargin = dp(10);
        card.addView(status, statusLp);

        Button loginButton = new Button(this);
        loginButton.setText("Đăng nhập  →");
        loginButton.setTextSize(16f);
        loginButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        loginButton.setAllCaps(false);
        loginButton.setTextColor(Color.WHITE);
        loginButton.setBackground(roundedDrawable(Color.rgb(8, 116, 216), Color.rgb(8, 116, 216), 12));
        LinearLayout.LayoutParams loginLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        loginLp.topMargin = dp(16);
        card.addView(loginButton, loginLp);

        TextView forgot = new TextView(this);
        forgot.setText("Quên mật khẩu?");
        forgot.setTextSize(14f);
        forgot.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        forgot.setTextColor(Color.rgb(8, 116, 170));
        forgot.setGravity(Gravity.CENTER);
        forgot.setPadding(0, dp(14), 0, dp(8));
        card.addView(forgot);
        forgot.setOnClickListener(v -> openExternal(Uri.parse("https://www.sachyhoc.com/wp-login.php?action=lostpassword")));

        Button registerButton = new Button(this);
        registerButton.setText("ĐĂNG KÝ TÀI KHOẢN");
        registerButton.setTextSize(14f);
        registerButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        registerButton.setAllCaps(false);
        registerButton.setTextColor(Color.rgb(8, 100, 151));
        registerButton.setBackground(roundedDrawable(Color.WHITE, Color.rgb(187, 213, 230), 12));
        LinearLayout.LayoutParams registerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        registerLp.topMargin = dp(6);
        card.addView(registerButton, registerLp);

        TextView privacy = new TextView(this);
        privacy.setText("Mật khẩu không được lưu trong ứng dụng. Internet chỉ dùng cho xác thực và kiểm tra cập nhật.");
        privacy.setTextSize(11.5f);
        privacy.setTextColor(Color.rgb(125, 145, 160));
        privacy.setGravity(Gravity.CENTER);
        privacy.setLineSpacing(0f, 1.12f);
        LinearLayout.LayoutParams privacyLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        privacyLp.topMargin = dp(14);
        card.addView(privacy, privacyLp);

        TextView exit = new TextView(this);
        exit.setText("Đóng ứng dụng");
        exit.setTextSize(13f);
        exit.setTextColor(Color.rgb(100, 123, 140));
        exit.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams exitLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        exitLp.topMargin = dp(18);
        page.addView(exit, exitLp);
        exit.setOnClickListener(v -> finish());

        registerButton.setOnClickListener(v -> openExternal(Uri.parse(REGISTER_URL)));
        loginButton.setOnClickListener(v -> {
            String login = loginField.getText().toString().trim();
            String password = passwordField.getText().toString();
            if (login.isEmpty() || password.isEmpty()) {
                status.setText("Vui lòng nhập đầy đủ tài khoản và mật khẩu.");
                status.setVisibility(View.VISIBLE);
                return;
            }
            status.setText("Đang xác thực…");
            status.setTextColor(Color.rgb(8, 103, 216));
            status.setVisibility(View.VISIBLE);
            loginButton.setEnabled(false);
            registerButton.setEnabled(false);
            loginField.setEnabled(false);
            passwordField.setEnabled(false);
            authenticateCredentials(login, password, dialog, loginButton, registerButton, loginField, passwordField, status);
        });

        dialog.setContentView(scroll);
        loginDialog = dialog;
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(roundedDrawable(Color.rgb(247, 251, 254), Color.TRANSPARENT, 0));
            window.setStatusBarColor(Color.WHITE);
            window.setNavigationBarColor(Color.WHITE);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    private void authenticateCredentials(String login, String password, Dialog dialog,
                                         Button loginButton, Button registerButton,
                                         EditText loginField, EditText passwordField, TextView status) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(LOGIN_ENDPOINT).openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(12000);
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("User-Agent", "KhiMauAndroid/" + BuildConfig.VERSION_NAME);

                String body = "login=" + URLEncoder.encode(login, "UTF-8") +
                        "&password=" + URLEncoder.encode(password, "UTF-8");
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(bytes);
                    out.flush();
                }

                int code = connection.getResponseCode();
                InputStream stream = code >= 200 && code < 400 ? connection.getInputStream() : connection.getErrorStream();
                String json = stream == null ? "" : readAll(stream);
                JSONObject response = new JSONObject(json);
                JSONObject data = response.optJSONObject("data");

                if (!response.optBoolean("success", false) || data == null || !data.optBoolean("member", false)) {
                    String message = data != null ? data.optString("message", "Tài khoản hoặc mật khẩu chưa đúng.") : "Tài khoản hoặc mật khẩu chưa đúng.";
                    throw new LoginException(message);
                }

                saveEntitlement(data);
                mainHandler.post(() -> {
                    passwordField.setText("");
                    dialog.dismiss();
                    loginDialog = null;
                    Toast.makeText(MainActivity.this, "Đã xác thực tài khoản · Có thể sử dụng ngoại tuyến", Toast.LENGTH_LONG).show();
                    loadOfflineRuntime();
                });
            } catch (LoginException e) {
                mainHandler.post(() -> resetLoginUi(loginButton, registerButton, loginField, passwordField, status, e.getMessage()));
            } catch (Exception e) {
                mainHandler.post(() -> resetLoginUi(loginButton, registerButton, loginField, passwordField, status,
                        "Chưa kết nối được máy chủ MEDIPHARM. Vui lòng kiểm tra Internet và thử lại."));
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void resetLoginUi(Button loginButton, Button registerButton,
                              EditText loginField, EditText passwordField,
                              TextView status, String message) {
        loginButton.setEnabled(true);
        registerButton.setEnabled(true);
        loginField.setEnabled(true);
        passwordField.setEnabled(true);
        status.setText(message == null || message.isEmpty() ? "Đăng nhập chưa thành công." : message);
        status.setVisibility(View.VISIBLE);
        status.setTextColor(Color.rgb(165, 50, 50));
        passwordField.requestFocus();
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception ignored) {
            Toast.makeText(this, "Không thể mở liên kết.", Toast.LENGTH_LONG).show();
        }
    }

    private void loadOfflineRuntime() {
        executor.execute(() -> {
            try {
                String html = applyBrandTheme(readRuntimeHtml());
                mainHandler.post(() -> {
                    localRuntime = true;
                    webView.clearHistory();
                    webView.loadDataWithBaseURL(LOCAL_BASE, html, "text/html", "UTF-8", null);
                });
            } catch (Exception e) {
                mainHandler.post(() -> showFatalLocalError("Không đọc được dữ liệu ngoại tuyến. Hãy cài lại bản Khí Máu mới nhất."));
            }
        });
    }

    private String applyBrandTheme(String html) {
        String css = """
<style id='abg-focused-v123'>
:root{--abg-blue:#0867D8;--abg-blue-deep:#074AA8;--abg-cyan:#08BDD0;--abg-red:#EF2138;--abg-ink:#0f2740;--abg-muted:#607789;--abg-line:#d9e7f2}
body{background:#fff!important}
.nah-abg__hero{position:sticky!important;top:0!important;z-index:120!important;display:grid!important;grid-template-columns:auto 1fr auto!important;grid-template-areas:'brand actions actions' 'search search search'!important;gap:8px!important;padding:8px 10px 10px!important;border-bottom:1px solid var(--abg-line)!important;background:rgba(255,255,255,.97)!important;box-shadow:0 5px 18px rgba(16,42,67,.05)!important;overflow:visible!important}
.nah-abg__brand{grid-area:brand!important;display:flex!important;align-items:center!important;gap:8px!important;text-decoration:none!important}.nah-abg__logo{width:48px!important;height:48px!important;min-width:48px!important;border-radius:13px!important;background:#fff url(%ICON%) center/cover no-repeat!important;box-shadow:0 5px 16px rgba(8,103,216,.13)!important;overflow:hidden!important}.nah-abg__logo>*{display:none!important}.nah-abg__brand-copy{display:flex!important;flex-direction:column!important;gap:1px!important}.nah-abg__brand-title{font-size:1.35rem!important;line-height:1!important;font-weight:900!important;letter-spacing:.04em!important;color:var(--abg-blue-deep)!important}.nah-abg__brand-subtitle{display:block!important;font-size:.52rem!important;font-weight:800!important;letter-spacing:.11em!important;text-transform:uppercase!important;color:#5d748a!important}
.nah-abg__hero-actions{grid-area:actions!important;justify-self:end!important;display:flex!important;align-items:center!important;gap:5px!important;position:relative!important}.nah-abg__account-toggle,.nah-abg__menu-toggle{width:42px!important;height:42px!important;min-width:42px!important;min-height:42px!important;padding:0!important;display:inline-flex!important;align-items:center!important;justify-content:center!important;border:1px solid #cbddea!important;border-radius:11px!important;background:#fff!important;color:var(--abg-blue-deep)!important;text-decoration:none!important}.nah-abg__account-toggle svg,.nah-abg__menu-toggle svg{width:21px!important;height:21px!important;fill:none!important;stroke:currentColor!important;stroke-width:1.9!important;stroke-linecap:round!important;stroke-linejoin:round!important}
.nah-abg__app-search{grid-area:search!important;width:100%!important;max-width:none!important;margin:0!important}.nah-abg__app-search-box{display:grid!important;grid-template-columns:40px minmax(0,1fr)!important;align-items:center!important;width:100%!important;min-height:44px!important;padding:0!important;overflow:hidden!important;border:1px solid #c7d9e8!important;border-radius:11px!important;background:#fff!important;box-shadow:none!important}.nah-abg__app-search-box svg{grid-column:1!important;width:20px!important;height:20px!important;margin:auto!important;fill:none!important;stroke:#58728b!important;stroke-width:1.8!important}.nah-abg__app-search-box input{grid-column:2!important;width:100%!important;min-width:0!important;height:42px!important;margin:0!important;padding:0 10px 0 0!important;border:0!important;outline:0!important;background:transparent!important;box-shadow:none!important;border-radius:0!important;color:var(--abg-ink)!important;font-size:.86rem!important}.nah-abg__app-search-box input::placeholder{color:#8293a4!important}
.nah-abg__workspace-frame{display:block!important}.nah-abg__workspace-tabs.nah-abg__app-nav{position:sticky!important;top:110px!important;z-index:115!important;padding:6px 10px!important;border-bottom:1px solid var(--abg-line)!important;background:rgba(248,252,255,.97)!important}.nah-abg__workspace-current{display:flex!important;min-height:44px!important;width:100%!important;align-items:center!important;justify-content:space-between!important;padding:8px 12px!important;border:1px solid #c7d9e8!important;border-radius:10px!important;background:#fff!important;color:var(--abg-ink)!important;font-weight:800!important}.nah-abg__workspace-tab-list{display:none!important;position:absolute!important;left:10px!important;right:10px!important;top:calc(100% + 4px)!important;grid-template-columns:1fr!important;gap:4px!important;padding:6px!important;border:1px solid var(--abg-line)!important;border-radius:12px!important;background:#fff!important;box-shadow:0 16px 36px rgba(16,42,67,.16)!important;max-height:62vh!important;overflow:auto!important}.nah-abg__workspace-tab-list.is-open{display:grid!important}.nah-abg__workspace-tab{display:grid!important;grid-template-columns:28px minmax(0,1fr)!important;grid-template-rows:auto auto!important;column-gap:9px!important;min-height:46px!important;padding:8px 10px!important;border:0!important;border-radius:9px!important;background:transparent!important;text-align:left!important}.nah-abg__workspace-tab svg{display:block!important;grid-row:1 / span 2!important;width:22px!important;height:22px!important;align-self:center!important;fill:none!important;stroke:#315979!important;stroke-width:1.8!important;stroke-linecap:round!important;stroke-linejoin:round!important}.nah-abg__workspace-tab span{grid-column:2!important;font-size:.9rem!important;font-weight:820!important;color:var(--abg-ink)!important}.nah-abg__workspace-tab small{grid-column:2!important;display:block!important;font-size:.66rem!important;color:#73879a!important}.nah-abg__workspace-tab.is-active{background:#edf7ff!important}
.nah-abg__app-home{display:grid!important;gap:12px!important;padding:12px 10px 22px!important;background:#f6fafe!important}.nah-abg__home-hero{order:1!important;position:relative!important;display:grid!important;grid-template-columns:1fr!important;gap:12px!important;padding:20px 17px!important;min-height:0!important;border:1px solid rgba(8,103,216,.15)!important;border-radius:15px!important;background:radial-gradient(circle at 10% 10%,rgba(239,33,56,.07),transparent 34%),radial-gradient(circle at 90% 20%,rgba(8,189,208,.13),transparent 38%),linear-gradient(120deg,#fbfdff,#eef8ff)!important;box-shadow:0 8px 24px rgba(16,70,110,.05)!important;overflow:hidden!important}.nah-abg__home-eyebrow{display:block!important;margin-bottom:7px!important;color:var(--abg-blue)!important;font-size:.66rem!important;font-weight:900!important;letter-spacing:.14em!important}.nah-abg__home-hero h1{margin:0!important;font-size:2rem!important;line-height:1.02!important;letter-spacing:-.035em!important;color:#0c2137!important}.nah-abg__home-hero p{margin:10px 0 0!important;font-size:.9rem!important;line-height:1.5!important;color:#5d7287!important}.nah-abg__home-actions{display:grid!important;grid-template-columns:1fr!important;gap:8px!important;margin-top:17px!important}.nah-abg__home-actions .nah-abg__button{width:100%!important;min-height:46px!important;padding:9px 12px!important;border-radius:10px!important;font-size:.84rem!important}.nah-abg__button--primary{background:linear-gradient(135deg,#0754bd,#087fe2 58%,#08b9cc)!important;border-color:transparent!important;color:#fff!important}.nah-abg__home-hero-stats{display:none!important}.nah-abg__home-visual{display:none!important}
.nah-abg__home-explore{order:2!important;padding:13px!important;border:1px solid var(--abg-line)!important;border-radius:15px!important;background:#fff!important;box-shadow:0 5px 16px rgba(16,42,67,.035)!important}.nah-abg__home-explore>header{display:flex!important;align-items:center!important;justify-content:space-between!important;margin:0 0 10px!important}.nah-abg__home-explore>header h2{margin:0!important;font-size:1.12rem!important;color:var(--abg-ink)!important}.nah-abg__home-explore>header p,.nah-abg__home-explore>header span{display:none!important}.nah-abg__home-grid{display:grid!important;grid-template-columns:1fr!important;gap:8px!important}.nah-abg__home-card{position:relative!important;display:grid!important;grid-template-columns:46px minmax(0,1fr) 28px!important;grid-template-rows:auto auto!important;column-gap:11px!important;row-gap:2px!important;align-items:center!important;min-height:0!important;padding:11px 12px!important;border:1px solid var(--abg-line)!important;border-radius:12px!important;background:#fff!important;text-align:left!important}.nah-abg__home-card-icon{grid-row:1 / span 2!important;display:grid!important;place-items:center!important;width:46px!important;height:46px!important;margin:0!important;border-radius:13px!important;background:linear-gradient(135deg,#0c79dc,#08bdd0)!important;color:#fff!important}.nah-abg__home-card-icon svg{width:25px!important;height:25px!important;fill:none!important;stroke:currentColor!important;stroke-width:1.8!important;stroke-linecap:round!important;stroke-linejoin:round!important}.nah-abg__home-card--analysis .nah-abg__home-card-icon{background:linear-gradient(135deg,#ff5868,#e91837)!important}.nah-abg__home-card--classroom .nah-abg__home-card-icon{background:linear-gradient(135deg,#25c7c1,#05a5af)!important}.nah-abg__home-card--review .nah-abg__home-card-icon{background:linear-gradient(135deg,#8d54f2,#6837df)!important}.nah-abg__home-card--profile .nah-abg__home-card-icon{background:linear-gradient(135deg,#269be8,#0876dc)!important}.nah-abg__home-card strong{grid-column:2!important;font-size:.94rem!important;color:var(--abg-ink)!important}.nah-abg__home-card small{grid-column:2!important;margin:0!important;padding:0!important;font-size:.74rem!important;line-height:1.4!important;color:#647c90!important}.nah-abg__home-card em{display:none!important}.nah-abg__home-card::after{content:'→'!important;grid-column:3!important;grid-row:1 / span 2!important;display:grid!important;place-items:center!important;width:28px!important;height:28px!important;border-radius:50%!important;background:#edf7ff!important;color:var(--abg-blue)!important;font-weight:900!important}.nah-abg__home-topics{display:none!important}
.nah-abg__home-learning-dashboard{order:3!important;display:grid!important;grid-template-columns:1fr!important;gap:10px!important;padding:13px!important;border:1px solid var(--abg-line)!important;border-radius:15px!important;background:#fff!important;box-shadow:0 5px 16px rgba(16,42,67,.035)!important}.nah-abg__home-learning-dashboard>header{margin:0!important}.nah-abg__home-learning-dashboard>header h2{margin:0!important;font-size:1.05rem!important;color:var(--abg-ink)!important}.nah-abg__home-learning-dashboard>header p{display:none!important}.nah-abg__v43-snapshot{margin:0!important}.nah-abg__home-today{margin:0!important;padding:10px!important;border:1px solid #dfecf6!important;border-radius:11px!important;background:#f7fbff!important}.nah-abg__home-progress-v123{padding:11px!important;border:1px solid #dfecf6!important;border-radius:11px!important;background:#fbfdff!important}.nah-abg__home-progress-v123 h3{margin:0 0 8px!important;font-size:.94rem!important;color:var(--abg-ink)!important}.nah-abg__home-progress-v123 ul{list-style:none!important;margin:0!important;padding:0!important;display:grid!important;grid-template-columns:repeat(3,minmax(0,1fr))!important;gap:6px!important}.nah-abg__home-progress-v123 li{display:flex!important;flex-direction:column!important;align-items:center!important;gap:1px!important;padding:7px 5px!important;border-radius:9px!important;background:#f2f8fd!important;color:#6b8194!important;font-size:.67rem!important}.nah-abg__home-progress-v123 strong{font-size:.95rem!important;color:#123b67!important}
.nah-abg__learning-feed,.nah-abg__home-profile{display:none!important}
</style>
""".replace("%ICON%", BRAND_ICON_DATA_URI);

        String js = """
<script id='abg-focused-v123-script'>
(function(){
  var q=function(s,c){return (c||document).querySelector(s)}, qa=function(s,c){return Array.prototype.slice.call((c||document).querySelectorAll(s))};
  var brand=q('.nah-abg__brand-title'); if(brand) brand.textContent='ABG';
  var copy=q('.nah-abg__brand-copy'); if(copy&&!q('.nah-abg__brand-subtitle',copy)){var sub=document.createElement('small');sub.className='nah-abg__brand-subtitle';sub.textContent='Khí máu lâm sàng';copy.appendChild(sub);}
  var actions=q('.nah-abg__hero-actions'); if(actions&&!q('.nah-abg__account-toggle',actions)){var a=document.createElement('a');a.className='nah-abg__account-toggle';a.href='medipharmabg://account';a.setAttribute('aria-label','Tài khoản');a.innerHTML='<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="4"></circle><path d="M4 21c.8-4.2 3.5-6 8-6s7.2 1.8 8 6"></path></svg>';actions.insertBefore(a,actions.firstChild);}
  var icons={
    home:'<path d="M3 11.5 12 4l9 7.5V21h-6v-6H9v6H3z"></path>',
    analysis:'<rect x="5" y="3" width="14" height="18" rx="2"></rect><path d="M9 8h6M9 12h6M9 16h4"></path>',
    theory:'<path d="M4 5.5A3.5 3.5 0 0 1 7.5 2H11v18H7.5A3.5 3.5 0 0 0 4 23zM20 5.5A3.5 3.5 0 0 0 16.5 2H13v18h3.5A3.5 3.5 0 0 1 20 23z"></path>',
    cases:'<path d="M9 3v5a3 3 0 0 0 6 0V3M12 11v2a5 5 0 0 0 5 5h1"></path><circle cx="19" cy="18" r="2"></circle>',
    review:'<circle cx="12" cy="12" r="8"></circle><circle cx="12" cy="12" r="4"></circle><path d="M12 12 19 5"></path>',
    profile:'<path d="M5 20V10M10 20V4M15 20v-7M20 20V7"></path>'
  };
  var labels={home:['Trang chủ','Tổng quan'],analysis:['Phân tích khí máu','Diễn giải khí máu'],theory:['Học tập','Bài học theo chủ đề'],cases:['Ca lâm sàng','Thực hành tình huống'],review:['Ôn luyện','Câu hỏi và thẻ nhớ'],profile:['Tiến độ','Kết quả học tập']};
  qa('.nah-abg__workspace-tab').forEach(function(btn){var k=btn.getAttribute('data-workspace-tab');if(k==='qa'||k==='exam'){btn.hidden=true;return;}if(!labels[k])return;var sp=q('span',btn),sm=q('small',btn);if(sp)sp.textContent=labels[k][0];if(sm)sm.textContent=labels[k][1];if(!q('svg',btn)){var svg=document.createElementNS('http://www.w3.org/2000/svg','svg');svg.setAttribute('viewBox','0 0 24 24');svg.innerHTML=icons[k];btn.insertBefore(svg,btn.firstChild);}});
  var hero=q('.nah-abg__home-hero'); if(hero){var hc=q('.nah-abg__home-hero-copy',hero),h1=q('h1',hc),p=q('p',hc);if(hc&&!q('.nah-abg__home-eyebrow',hc)){var ey=document.createElement('span');ey.className='nah-abg__home-eyebrow';ey.textContent='KHÍ MÁU ĐỘNG MẠCH';hc.insertBefore(ey,hc.firstChild);}if(h1)h1.textContent='Khí máu lâm sàng';if(p)p.textContent='Phân tích toan–kiềm, thông khí và oxy hóa theo trình tự lâm sàng; kết nối bài học, ca bệnh và ôn luyện trong cùng một không gian.';var bs=qa('.nah-abg__home-actions button',hero);if(bs[0])bs[0].textContent='Phân tích khí máu →';if(bs[1]){bs[1].setAttribute('data-home-workspace','theory');bs[1].textContent='Học theo chủ đề →';}}
  var explore=q('.nah-abg__home-explore'); if(explore){var eh=q('header',explore);if(eh)eh.innerHTML='<h2>Các module chính</h2>';var cards=qa('.nah-abg__home-card',explore).slice(0,5);var defs=[
    ['analysis','analysis','Phân tích khí máu','Nhập số liệu và nhận kết quả diễn giải có cấu trúc.',icons.analysis],
    ['theory','theory','Học tập','Bài học theo chủ đề, kiến thức nền tảng và nâng cao.',icons.theory],
    ['cases','classroom','Ca lâm sàng','Thực hành với các tình huống lâm sàng có lời giải.',icons.cases],
    ['review','review','Ôn luyện','Câu hỏi, thẻ ghi nhớ và nội dung cần củng cố.',icons.review],
    ['profile','profile','Tiến độ','Theo dõi quá trình học tập, luyện tập và kết quả.',icons.profile]
  ];cards.forEach(function(card,i){var d=defs[i];if(!d)return;card.setAttribute('data-home-workspace',d[0]);card.removeAttribute('data-home-generator');card.className='nah-abg__home-card nah-abg__home-card--'+d[1];card.innerHTML='<span class="nah-abg__home-card-icon" aria-hidden="true"><svg viewBox="0 0 24 24">'+d[4]+'</svg></span><strong>'+d[2]+'</strong><small>'+d[3]+'</small>';});}
  var dash=q('.nah-abg__home-learning-dashboard'); if(dash){var dh=q('header h2',dash);if(dh)dh.textContent='Tiếp tục học';if(!q('.nah-abg__home-progress-v123',dash)){var prog=document.createElement('aside');prog.className='nah-abg__home-progress-v123';prog.innerHTML='<h3>Tiến độ học tập</h3><ul><li><strong>50</strong><span>bài học</span></li><li><strong>50</strong><span>ca lâm sàng</span></li><li><strong>220</strong><span>câu luyện</span></li></ul>';dash.appendChild(prog);}}
})();
</script>
""";
        String out = html.replace("#0b6674", "#0867d8").replace("#0B6674", "#0867D8");
        out = out.replace("</head>", css + "</head>");
        return out.replace("</body>", js + "</body>");
    }

    private void installBundledDatabase() throws Exception {
        File dir = new File(getFilesDir(), "offline");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("mkdir");
        File target = new File(dir, DB_FILE);
        String assetHash;
        try (InputStream in = getAssets().open(DB_ASSET)) {
            assetHash = sha256(in);
        }
        String targetHash = target.exists() ? sha256(new FileInputStream(target)) : "";
        if (!assetHash.equals(targetHash)) {
            File tmp = new File(dir, DB_FILE + ".tmp");
            try (InputStream in = getAssets().open(DB_ASSET); FileOutputStream out = new FileOutputStream(tmp)) {
                byte[] buffer = new byte[65536];
                int n;
                while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
                out.getFD().sync();
            }
            if (target.exists() && !target.delete()) throw new IllegalStateException("delete old db");
            if (!tmp.renameTo(target)) throw new IllegalStateException("rename db");
        }
        dataVersion = readMeta(target, "data_version");
    }

    private String readRuntimeHtml() throws Exception {
        File dbFile = new File(new File(getFilesDir(), "offline"), DB_FILE);
        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        try (Cursor cursor = db.rawQuery("SELECT content FROM runtime_assets WHERE key=?", new String[]{"runtime_html"})) {
            if (!cursor.moveToFirst()) throw new IllegalStateException("runtime missing");
            return new String(cursor.getBlob(0), StandardCharsets.UTF_8);
        } finally {
            db.close();
        }
    }

    private String readMeta(File dbFile, String key) {
        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        try (Cursor cursor = db.rawQuery("SELECT value FROM app_meta WHERE key=?", new String[]{key})) {
            return cursor.moveToFirst() ? cursor.getString(0) : "";
        } finally {
            db.close();
        }
    }

    private void saveEntitlement(JSONObject serverData) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("member", true);
        payload.put("userId", serverData.optLong("userId", 0));
        payload.put("displayName", serverData.optString("displayName", ""));
        payload.put("verifiedAt", serverData.optLong("verifiedAt", System.currentTimeMillis() / 1000L));
        payload.put("validUntil", serverData.optLong("validUntil", System.currentTimeMillis() / 1000L));
        payload.put("serverDataVersion", serverData.optString("dataVersion", ""));
        String text = payload.toString();
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(PREF_ENTITLEMENT, text)
                .putString(PREF_SIGNATURE, sign(text))
                .apply();
    }

    private JSONObject getValidEntitlement() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            String payload = prefs.getString(PREF_ENTITLEMENT, "");
            String signature = prefs.getString(PREF_SIGNATURE, "");
            if (payload.isEmpty() || signature.isEmpty() || !verify(payload, signature)) return null;
            JSONObject data = new JSONObject(payload);
            long now = System.currentTimeMillis() / 1000L;
            if (!data.optBoolean("member", false) || data.optLong("validUntil", 0) < now) return null;
            return data;
        } catch (Exception ignored) {
            return null;
        }
    }

    private SecretKey getOrCreateHmacKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) {
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build());
        return generator.generateKey();
    }

    private String sign(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(getOrCreateHmacKey());
        return Base64.encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
    }

    private boolean verify(String payload, String signature) throws Exception {
        return MessageDigest.isEqual(
                Base64.decode(sign(payload), Base64.NO_WRAP),
                Base64.decode(signature, Base64.NO_WRAP)
        );
    }

    private void showAccountDialog() {
        JSONObject entitlement = getValidEntitlement();
        if (entitlement == null) {
            showNativeLogin(null);
            return;
        }
        long until = entitlement.optLong("validUntil", 0) * 1000L;
        String name = entitlement.optString("displayName", "Tài khoản MEDIPHARM");
        String expiry = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
                new Locale("vi", "VN")).format(new Date(until));
        String message = name + "\n\nQuyền sử dụng ngoại tuyến đến: " + expiry +
                "\nDữ liệu cục bộ: " + (dataVersion.isEmpty() ? "—" : dataVersion) +
                "\nPhiên bản ứng dụng: " + BuildConfig.VERSION_NAME;
        new AlertDialog.Builder(this)
                .setTitle("Tài khoản")
                .setMessage(message)
                .setNegativeButton("Đóng", null)
                .setPositiveButton("Đăng xuất", (d, w) -> logoutAndAuthenticate())
                .show();
    }

    private void logoutAndAuthenticate() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .remove(PREF_ENTITLEMENT)
                .remove(PREF_SIGNATURE)
                .apply();
        showNativeLogin(null);
    }

    private void showFatalLocalError(String message) {
        localRuntime = false;
        String safe = message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        webView.loadDataWithBaseURL(LOCAL_BASE,
                "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>" +
                        "<body><h2>Khí Máu</h2><p>" + safe + "</p></body></html>",
                "text/html", "UTF-8", null);
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
                if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);
                JSONArray files = new JSONArray(readAll(connection.getInputStream()));
                UpdateInfo latest = null;
                for (int i = 0; i < files.length(); i++) {
                    JSONObject item = files.optJSONObject(i);
                    if (item == null || !"file".equals(item.optString("type"))) continue;
                    Matcher matcher = APK_PATTERN.matcher(item.optString("name"));
                    if (!matcher.matches()) continue;
                    Version version = new Version(
                            Integer.parseInt(matcher.group(1)),
                            Integer.parseInt(matcher.group(2)),
                            Integer.parseInt(matcher.group(3))
                    );
                    String downloadUrl = item.optString("download_url");
                    if (!downloadUrl.isEmpty() && (latest == null || version.compareTo(latest.version) > 0)) {
                        latest = new UpdateInfo(version, item.optString("name"), downloadUrl);
                    }
                }
                UpdateInfo result = latest;
                mainHandler.post(() -> presentUpdateResult(result));
            } catch (Exception e) {
                mainHandler.post(() -> new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Cập nhật ứng dụng")
                        .setMessage("Chưa kiểm tra được phiên bản mới. Vui lòng kết nối Internet và thử lại.")
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
                    .setMessage("Khí Máu v" + BuildConfig.VERSION_NAME + " đang là phiên bản mới nhất.\nDữ liệu: " +
                            (dataVersion.isEmpty() ? "—" : dataVersion))
                    .setPositiveButton("Đóng", null)
                    .show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Có phiên bản mới")
                .setMessage("Khí Máu v" + latest.version + " đã có trên GitHub.")
                .setNegativeButton("Để sau", null)
                .setPositiveButton("Tải xuống", (d, w) -> downloadApk(latest))
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
            ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
            Toast.makeText(this, "Đã bắt đầu tải xuống. Mở thông báo tải về để cài đặt.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            openExternal(Uri.parse(update.downloadUrl));
        }
    }

    private static String readAll(InputStream input) throws Exception {
        StringBuilder b = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) b.append(line);
        }
        return b.toString();
    }

    private static String sha256(InputStream input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = input) {
            byte[] buffer = new byte[65536];
            int n;
            while ((n = in.read(buffer)) > 0) digest.update(buffer, 0, n);
        }
        StringBuilder out = new StringBuilder();
        for (byte b : digest.digest()) out.append(String.format(Locale.ROOT, "%02x", b));
        return out.toString();
    }

    private GradientDrawable roundedDrawable(int fillColor, int strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radiusDp));
        if (Color.alpha(strokeColor) > 0) drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (loginDialog != null && loginDialog.isShowing()) loginDialog.dismiss();
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

    private static final class LoginException extends Exception {
        LoginException(String message) {
            super(message);
        }
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
            Matcher m = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$")
                    .matcher(value == null ? "" : value);
            return m.matches()
                    ? new Version(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)))
                    : new Version(0, 0, 0);
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
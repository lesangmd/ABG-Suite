package com.medipharm.abgsuite;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.InputType;
import android.util.Base64;
import android.view.View;
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
import android.widget.LinearLayout;
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

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private WebView webView;
    private boolean localRuntime;
    private String dataVersion = "";
    private AlertDialog loginDialog;
    // This Android-owned dock is independent of the scrolling clinical WebView.
    private WebView nativeDock;
    private void createNativeDock(LinearLayout shell) {
        nativeDock = new WebView(this);
        nativeDock.setBackgroundColor(Color.WHITE);
        nativeDock.setVerticalScrollBarEnabled(false);
        nativeDock.getSettings().setJavaScriptEnabled(true);
        nativeDock.getSettings().setDomStorageEnabled(false);
        nativeDock.getSettings().setAllowFileAccess(false);
        nativeDock.getSettings().setAllowContentAccess(false);
        nativeDock.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if ("medipharmabg".equals(u.getScheme()) && "navigate".equals(u.getHost())) {
                    navigateNativeDock(u.getQueryParameter("workspace"));
                }
                return true;
            }
        });
        shell.addView(nativeDock, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(70)));
        nativeDock.setVisibility(View.GONE);
        nativeDock.loadDataWithBaseURL(LOCAL_BASE, "<!doctype html><html lang=\"vi\"><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><style>*{box-sizing:border-box}html,body{margin:0;height:100%;overflow:hidden;font-family:Arial,sans-serif;background:#fff}nav{height:100%;display:grid;grid-template-columns:repeat(4,minmax(0,1fr));padding:4px 6px;border-top:1px solid #d8e6ee;gap:3px}a{display:flex;flex-direction:column;gap:2px;align-items:center;justify-content:center;min-width:0;text-decoration:none;border-radius:12px;font-size:clamp(10px,2.3vw,12px);font-weight:650;color:#637d91;-webkit-tap-highlight-color:transparent}a.active{background:#e9f4ff;color:#0874cc}svg{height:23px;width:23px;flex-shrink:0}span{white-space:nowrap;overflow:hidden;max-width:100%;text-overflow:ellipsis} @media(max-width:340px){nav{padding-inline:2px}a{font-size:10px}svg{width:21px;height:21px}}</style></head><body><nav><a data-target=\"home\" href=\"medipharmabg://navigate?workspace=home\" class=\"active\"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.9\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M3 11.5 12 4l9 7.5V21h-6v-6H9v6H3z\"/></svg><span>Trang chủ</span></a><a data-target=\"analysis\" href=\"medipharmabg://navigate?workspace=analysis\" class=\"\"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.9\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><rect x=\"5\" y=\"3\" width=\"14\" height=\"18\" rx=\"2\"/><path d=\"M9 8h6M9 12h6M9 16h4\"/></svg><span>Phân tích</span></a><a data-target=\"cases\" href=\"medipharmabg://navigate?workspace=cases\" class=\"\"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.9\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M9 3v5a3 3 0 0 0 6 0V3M12 11v2a5 5 0 0 0 5 5h1\"/><circle cx=\"19\" cy=\"18\" r=\"2\"/></svg><span>Ca lâm sàng</span></a><a data-target=\"theory\" href=\"medipharmabg://navigate?workspace=theory\" class=\"\"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.9\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M4 5.5A3.5 3.5 0 0 1 7.5 2H11v18H7.5A3.5 3.5 0 0 0 4 23zM20 5.5A3.5 3.5 0 0 0 16.5 2H13v18h3.5A3.5 3.5 0 0 1 20 23z\"/></svg><span>Học tập</span></a></nav></body></html>", "text/html", "UTF-8", null);
    }

    private void navigateNativeDock(String target) {
        if (!localRuntime || webView == null) return;
        if (!"home".equals(target) && !"analysis".equals(target) && !"cases".equals(target) && !"theory".equals(target)) return;
        webView.evaluateJavascript("(function(){var b=document.querySelector('.nah-abg__mobile-dock [data-home-workspace=\\\"" + target + "\\\"]');if(b)b.click();})();",null);
        if (nativeDock != null) nativeDock.evaluateJavascript("(function(){document.querySelectorAll('[data-target]').forEach(function(a){a.classList.toggle('active',a.getAttribute('data-target')==='" + target + "');});})();", null);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(11, 102, 116));
        getWindow().setNavigationBarColor(Color.rgb(243, 247, 249));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        webView = new WebView(this);
        root.addView(webView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        createNativeDock(root);
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


    private GradientDrawable loginBackground(int color, int radiusDp, int strokeColor) {
        GradientDrawable result = new GradientDrawable();
        result.setColor(color);
        result.setCornerRadius(dp(radiusDp));
        if (strokeColor != Color.TRANSPARENT) result.setStroke(dp(1), strokeColor);
        return result;
    }

    private TextView loginLabel(String text, int sp, int color, boolean strong) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(sp);
        label.setTextColor(color);
        if (strong) label.setTypeface(null, android.graphics.Typeface.BOLD);
        return label;
    }

    private View loginArtBackground() {
        return new View(this) {
            private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(android.graphics.Canvas canvas) {
                super.onDraw(canvas);
                float w = getWidth(), h = getHeight();
                canvas.drawColor(Color.rgb(242, 249, 255));
                android.graphics.Path wave = new android.graphics.Path();
                paint.setStyle(android.graphics.Paint.Style.FILL);
                paint.setColor(Color.rgb(211, 236, 252));
                wave.moveTo(0, 0); wave.lineTo(w, 0);
                wave.lineTo(w, h * .22f);
                wave.cubicTo(w*.70f, h*.02f, w*.38f, h*.09f, 0, h*.28f);
                wave.close();canvas.drawPath(wave,paint);
                wave.reset();paint.setColor(Color.rgb(255, 228, 232));
                wave.moveTo(0, h*.86f);
                wave.cubicTo(w*.25f,h*.75f,w*.55f,h*.85f,w,h*.68f);
                wave.lineTo(w,h);wave.lineTo(0,h);wave.close();canvas.drawPath(wave,paint);
                wave.reset();paint.setColor(Color.rgb(91, 198, 241));
                wave.moveTo(0,h*.94f);
                wave.cubicTo(w*.30f,h*.79f,w*.63f,h*.97f,w,h*.78f);
                wave.lineTo(w,h);wave.lineTo(0,h);wave.close();canvas.drawPath(wave,paint);
                wave.reset();paint.setColor(Color.rgb(0, 119, 220));
                wave.moveTo(0,h);
                wave.cubicTo(w*.33f,h*.88f,w*.64f,h*1.02f,w,h*.87f);
                wave.lineTo(w,h);wave.close();canvas.drawPath(wave,paint);
            }
        };
    }

    private void showNativeLogin(String initialMessage) {
        localRuntime = false;
        if (nativeDock != null) nativeDock.setVisibility(View.GONE);
        if (loginDialog != null && loginDialog.isShowing()) loginDialog.dismiss();

        final int ink=Color.rgb(17,44,72), muted=Color.rgb(96,116,136);
        FrameLayout scene = new FrameLayout(this);
        scene.addView(loginArtBackground(),new FrameLayout.LayoutParams(-1,-1));
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setPadding(dp(16),dp(20),dp(16),dp(20));
        FrameLayout.LayoutParams scrollParams=new FrameLayout.LayoutParams(-1,-1);
        scene.addView(scroll,scrollParams);
        FrameLayout center=new FrameLayout(this);
        scroll.addView(center,new android.widget.ScrollView.LayoutParams(-1,-2));

        LinearLayout card = new LinearLayout(this) {
            @Override protected void onMeasure(int widthMeasureSpec,int heightMeasureSpec) {
                int max=Math.min(View.MeasureSpec.getSize(widthMeasureSpec),dp(420));
                super.onMeasure(View.MeasureSpec.makeMeasureSpec(max,View.MeasureSpec.EXACTLY),heightMeasureSpec);
            }
        };
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(24),dp(17),dp(24),dp(22));
        card.setBackground(loginBackground(Color.WHITE,28,Color.rgb(220,233,242)));
        card.setElevation(dp(10));
        FrameLayout.LayoutParams cardParams=new FrameLayout.LayoutParams(-1,-2,android.view.Gravity.CENTER);
        cardParams.topMargin=dp(10);cardParams.bottomMargin=dp(10);
        center.addView(card,cardParams);

        TextView exit=loginLabel("✕  Thoát",13,muted,false);
        exit.setGravity(android.view.Gravity.END);
        exit.setPadding(dp(8),dp(4),0,dp(4));
        exit.setOnClickListener(v->finish());
        card.addView(exit,new LinearLayout.LayoutParams(-1,-2));

        android.widget.ImageView logo = new android.widget.ImageView(this);
        logo.setImageResource(R.drawable.abg_brand);
        logo.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoParams=new LinearLayout.LayoutParams(dp(132),dp(132));
        logoParams.gravity=android.view.Gravity.CENTER_HORIZONTAL;
        card.addView(logo,logoParams);

        TextView brand=loginLabel("MEDIPHARM ABG",24,Color.rgb(0,101,177),true);
        brand.setGravity(android.view.Gravity.CENTER);
        card.addView(brand,new LinearLayout.LayoutParams(-1,-2));

        TextView subtitle=loginLabel("Phân tích khí máu động mạch",14,muted,false);
        subtitle.setGravity(android.view.Gravity.CENTER);
        subtitle.setPadding(0,dp(4),0,dp(9));
        card.addView(subtitle,new LinearLayout.LayoutParams(-1,-2));

        TextView welcome=loginLabel("Truy cập MEDIPHARM ABG",19,ink,true);
        welcome.setGravity(android.view.Gravity.CENTER);
        welcome.setPadding(0,dp(8),0,dp(15));
        card.addView(welcome,new LinearLayout.LayoutParams(-1,-2));

        TextView userLabel=loginLabel("Tên đăng nhập hoặc email",14,ink,true);
        userLabel.setPadding(0,0,0,dp(5));
        card.addView(userLabel);
        EditText loginField=new EditText(this);
        loginField.setHint("Nhập tên đăng nhập hoặc email");
        loginField.setSingleLine(true);
        loginField.setTextSize(16);
        loginField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        loginField.setPadding(dp(14),0,dp(14),0);
        loginField.setBackground(loginBackground(Color.rgb(249,252,255),13,Color.rgb(208,222,232)));
        LinearLayout.LayoutParams fieldParam=new LinearLayout.LayoutParams(-1,dp(54));
        fieldParam.bottomMargin=dp(13);
        card.addView(loginField,fieldParam);

        TextView passwordLabel=loginLabel("Mật khẩu",14,ink,true);
        passwordLabel.setPadding(0,0,0,dp(5));
        card.addView(passwordLabel);
        EditText passwordField=new EditText(this);
        passwordField.setHint("Nhập mật khẩu");
        passwordField.setSingleLine(true);
        passwordField.setTextSize(16);
        passwordField.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordField.setPadding(dp(14),0,dp(14),0);
        passwordField.setBackground(loginBackground(Color.rgb(249,252,255),13,Color.rgb(208,222,232)));
        card.addView(passwordField,new LinearLayout.LayoutParams(-1,dp(54)));

        TextView forgot=loginLabel("Quên mật khẩu?",13,Color.rgb(0,119,205),false);
        forgot.setGravity(android.view.Gravity.END);
        forgot.setPadding(0,dp(9),0,dp(8));
        forgot.setOnClickListener(v->openExternal(Uri.parse("https://www.sachyhoc.com/wp-login.php?action=lostpassword")));
        card.addView(forgot,new LinearLayout.LayoutParams(-1,-2));

        TextView status=loginLabel(initialMessage == null?"":initialMessage,14,Color.rgb(176,57,61),false);
        status.setGravity(android.view.Gravity.CENTER);
        status.setPadding(0,0,0,dp(8));
        card.addView(status,new LinearLayout.LayoutParams(-1,-2));

        Button loginButton=new Button(this);
        loginButton.setText("Đăng nhập  →");
        loginButton.setAllCaps(false);
        loginButton.setTextColor(Color.WHITE);
        loginButton.setTextSize(16);
        loginButton.setTypeface(null,android.graphics.Typeface.BOLD);
        GradientDrawable primary=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(0,101,220),Color.rgb(5,180,227)});
        primary.setCornerRadius(dp(13));
        loginButton.setBackground(primary);
        card.addView(loginButton,new LinearLayout.LayoutParams(-1,dp(54)));

        Button registerButton=new Button(this);
        registerButton.setText("ĐĂNG KÝ TÀI KHOẢN");
        registerButton.setAllCaps(false);
        registerButton.setTextSize(14);
        registerButton.setTextColor(Color.rgb(0,102,182));
        registerButton.setBackground(loginBackground(Color.WHITE,13,Color.rgb(204,224,236)));
        LinearLayout.LayoutParams registerParams=new LinearLayout.LayoutParams(-1,dp(49));
        registerParams.topMargin=dp(10);
        card.addView(registerButton,registerParams);

        TextView info=loginLabel("Xác thực tài khoản một lần để sử dụng nội dung ngoại tuyến. Mật khẩu không lưu trên thiết bị.",12,muted,false);
        info.setGravity(android.view.Gravity.CENTER);
        info.setPadding(0,dp(16),0,0);
        card.addView(info,new LinearLayout.LayoutParams(-1,-2));

        AlertDialog dialog=new AlertDialog.Builder(this).setView(scene).setCancelable(false).create();
        loginDialog=dialog;
        registerButton.setOnClickListener(v->openExternal(Uri.parse(REGISTER_URL)));
        loginButton.setOnClickListener(v->{
            String login=loginField.getText().toString().trim();
            String password=passwordField.getText().toString();
            if(login.isEmpty()||password.isEmpty()){
                status.setText("Vui lòng nhập đầy đủ tài khoản và mật khẩu.");
                return;
            }
            status.setText("Đang xác thực…");
            status.setTextColor(Color.rgb(43,101,112));
            loginButton.setEnabled(false);registerButton.setEnabled(false);
            loginField.setEnabled(false);passwordField.setEnabled(false);
            authenticateCredentials(login,password,dialog,loginButton,registerButton,loginField,passwordField,status);
        });
        dialog.show();
        if(dialog.getWindow()!=null){
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(-1,-1);
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private void authenticateCredentials(String login, String password, AlertDialog dialog,
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
                String html = readRuntimeHtml();
                mainHandler.post(() -> {
                    localRuntime = true;
                    if (nativeDock != null) nativeDock.setVisibility(View.VISIBLE);
                    webView.clearHistory();
                    webView.loadDataWithBaseURL(LOCAL_BASE, html, "text/html", "UTF-8", null);
                });
            } catch (Exception e) {
                mainHandler.post(() -> showFatalLocalError("Không đọc được dữ liệu ngoại tuyến. Hãy cài lại bản Khí Máu mới nhất."));
            }
        });
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
        if (nativeDock != null) nativeDock.setVisibility(View.GONE);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (loginDialog != null && loginDialog.isShowing()) loginDialog.dismiss();
        mainHandler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        if (nativeDock != null) {
            nativeDock.stopLoading();
            nativeDock.setWebViewClient(null);
            nativeDock.destroy();
            nativeDock = null;
        }
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
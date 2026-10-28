package com.yandex.boards;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.DisplayCutout;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final String WHITEBOARD_URL =
        "https://boards.yandex.ru/whiteboard/?hash=e0469e6c2c6901788c0bd174fc2a7570";
    private static final String REDIRECT_URI = "yandexboards://callback";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Полноэкранный режим без навигационной панели
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Window window = getWindow();
            window.setDecorFitsSystemWindows(false);
            window.getInsetsController().hide(
                android.view.WindowInsets.Type.statusBars()
                | android.view.WindowInsets.Type.navigationBars());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        // Создаём WebView
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_NO_CACHE);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);

        // Обработка URL
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                webView.setVisibility(android.view.View.VISIBLE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return handleUrl(url);
            }

            // Для старых API
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }

            private boolean handleUrl(String url) {
                // OAuth callback
                if (url.startsWith(REDIRECT_URI)) {
                    handleCallback(url);
                    return true;
                }

                // boards.yandex.ru — в WebView
                if (url.contains("boards.yandex.ru")) {
                    view.loadUrl(url);
                    return true;
                }

                // Всё остальное — в браузере
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                       WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    Snackbar.make(view,
                        "Ошибка: " + error.getDescription(),
                        Snackbar.LENGTH_LONG).show();
                }
            }
        });

        setContentView(webView);

        // Проверяем токен
        String token = getSavedToken(this);
        if (token != null && !token.isEmpty()) {
            webView.loadUrl(WHITEBOARD_URL);
        } else {
            webView.loadUrl("file:///android_asset/www/index.html");
        }
    }

    // OAuth callback
    private void handleCallback(String url) {
        try {
            Uri uri = Uri.parse(url);
            String code = uri.getQueryParameter("code");
            if (code != null) {
                // Получаем verifier из JS
                webView.evaluateJavascript(
                    "localStorage.getItem('pkce_verifier')",
                    result -> {
                        String verifier = result.replace("\"", "");
                        exchangeToken(code, verifier);
                    }
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Обмен кода на токен
    private void exchangeToken(String code, String verifier) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("https://oauth.yandex.ru/token");
                java.net.HttpURLConnection conn =
                    (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String data = "grant_type=authorization_code" +
                    "&code=" + java.net.URLEncoder.encode(code, "UTF-8") +
                    "&client_id=e44bb58e1b5b4577bec9ff2abb6dad3d" +
                    "&code_verifier=" + java.net.URLEncoder.encode(verifier, "UTF-8");

                java.io.OutputStream os = conn.getOutputStream();
                os.write(data.getBytes("UTF-8"));
                os.close();

                java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                String token = extractToken(sb.toString());
                if (token != null) {
                    saveToken(this, token);
                    runOnUiThread(() -> {
                        webView.loadUrl("javascript:" +
                            "localStorage.setItem('yandex_token','" + token + "'); " +
                            "location.href='" + WHITEBOARD_URL + "';");
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String extractToken(String json) {
        try {
            int start = json.indexOf("\"access_token\":\"");
            if (start < 0) return null;
            start += 17;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    // Сохранение токена
    private static void saveToken(Activity act, String token) {
        act.getSharedPreferences("yb", Activity.MODE_PRIVATE)
            .edit().putString("t", token).apply();
    }

    private static String getSavedToken(Activity act) {
        return act.getSharedPreferences("yb", Activity.MODE_PRIVATE)
            .getString("t", null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Проверяем callback из Intent
        Intent intent = getIntent();
        if (intent != null) {
            Uri data = intent.getData();
            if (data != null && data.toString().startsWith(REDIRECT_URI)) {
                handleCallback(data.toString());
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

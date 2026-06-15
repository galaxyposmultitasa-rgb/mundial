package com.mundial2026;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {

    private WebView webView;
    private NotificationManager notifManager;
    private static final String CHANNEL_GOALS   = "goals";
    private static final String CHANNEL_ALERTS  = "alerts";
    private static final String CHANNEL_DAILY   = "daily";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Pantalla completa, status bar oscura
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(0xFF070e1c);
        getWindow().setNavigationBarColor(0xFF070e1c);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        // WebView
        webView = new WebView(this);
        setContentView(webView);

        // Configurar WebView
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString("Mundial2026App/1.0 Android");

        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        // Bridge JS → Java para notificaciones
        webView.addJavascriptInterface(new NotifBridge(), "AndroidNotif");

        // Cargar HTML local desde assets
        webView.loadUrl("file:///android_asset/index.html");

        // Canales de notificación
        createChannels();

        // Pedir permiso en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
    }

    private void createChannels() {
        notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel g = new NotificationChannel(CHANNEL_GOALS, "Goles",
                NotificationManager.IMPORTANCE_HIGH);
            g.setVibrationPattern(new long[]{0,300,100,300,100,300});
            g.enableVibration(true);
            notifManager.createNotificationChannel(g);

            NotificationChannel a = new NotificationChannel(CHANNEL_ALERTS, "Alertas",
                NotificationManager.IMPORTANCE_DEFAULT);
            a.setVibrationPattern(new long[]{0,200,100,200});
            a.enableVibration(true);
            notifManager.createNotificationChannel(a);

            NotificationChannel d = new NotificationChannel(CHANNEL_DAILY, "Diario",
                NotificationManager.IMPORTANCE_LOW);
            notifManager.createNotificationChannel(d);
        }
    }

    // Clase bridge accesible desde JS como window.AndroidNotif
    public class NotifBridge {

        @JavascriptInterface
        public void sendGoalNotif(String title, String body) {
            showNotif(title, body, CHANNEL_GOALS,
                new long[]{0,300,100,300,100,300});
        }

        @JavascriptInterface
        public void sendAlertNotif(String title, String body) {
            showNotif(title, body, CHANNEL_ALERTS,
                new long[]{0,200,100,200});
        }

        @JavascriptInterface
        public void sendDailyNotif(String title, String body) {
            showNotif(title, body, CHANNEL_DAILY, null);
        }

        @JavascriptInterface
        public void vibrate(int ms) {
            android.os.Vibrator v = (android.os.Vibrator)
                getSystemService(VIBRATOR_SERVICE);
            if (v != null) v.vibrate(ms);
        }

        @JavascriptInterface
        public boolean isAndroid() { return true; }
    }

    private int notifId = 1;
    private void showNotif(String title, String body, String channel, long[] pattern) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(channel.equals(CHANNEL_GOALS)
                ? NotificationCompat.PRIORITY_HIGH
                : NotificationCompat.PRIORITY_DEFAULT);
        if (pattern != null) b.setVibrate(pattern);
        if (notifManager != null)
            notifManager.notify(notifId++, b.build());
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.evaluateJavascript("if(typeof fetchLive==='function')fetchLive();", null);
    }
}

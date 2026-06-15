package com.mundial2026;

import android.app.Activity;
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
    private int notifId = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Pantalla completa
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(0xFF070e1c);
        getWindow().setNavigationBarColor(0xFF070e1c);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);           // ← localStorage habilitado
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);             // ← acceso a assets
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true); // ← JS puede leer assets
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUserAgentString("Mundial2026App/1.0 Android/" + Build.VERSION.SDK_INT);

        // Hardware acceleration
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        // Bridge JS → Java
        webView.addJavascriptInterface(new NotifBridge(), "AndroidNotif");

        // Cargar app
        webView.loadUrl("file:///android_asset/index.html");

        // Canales de notificación
        createChannels();

        // Permisos Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
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
            NotificationChannel g = new NotificationChannel("goals", "⚽ Goles",
                NotificationManager.IMPORTANCE_HIGH);
            g.setVibrationPattern(new long[]{0,300,100,300,100,300});
            g.enableVibration(true);
            notifManager.createNotificationChannel(g);

            NotificationChannel a = new NotificationChannel("alerts", "🔔 Alertas",
                NotificationManager.IMPORTANCE_DEFAULT);
            a.setVibrationPattern(new long[]{0,200,100,200});
            a.enableVibration(true);
            notifManager.createNotificationChannel(a);

            NotificationChannel d = new NotificationChannel("daily", "📅 Diario",
                NotificationManager.IMPORTANCE_LOW);
            notifManager.createNotificationChannel(d);
        }
    }

    public class NotifBridge {
        @JavascriptInterface
        public void sendGoalNotif(String title, String body) {
            postNotif(title, body, "goals", new long[]{0,300,100,300,100,300});
        }
        @JavascriptInterface
        public void sendAlertNotif(String title, String body) {
            postNotif(title, body, "alerts", new long[]{0,200,100,200});
        }
        @JavascriptInterface
        public void sendDailyNotif(String title, String body) {
            postNotif(title, body, "daily", null);
        }
        @JavascriptInterface
        public void vibrate(int ms) {
            try {
                android.os.Vibrator v = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (v != null) v.vibrate(ms);
            } catch(Exception e){}
        }
        @JavascriptInterface
        public boolean isAndroid() { return true; }
        @JavascriptInterface
        public String getVersion() { return "1.0"; }
    }

    private void postNotif(String title, String body, String channel, long[] vibration) {
        if (notifManager == null) return;
        try {
            NotificationCompat.Builder b = new NotificationCompat.Builder(this, channel)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setPriority("goals".equals(channel) 
                    ? NotificationCompat.PRIORITY_HIGH 
                    : NotificationCompat.PRIORITY_DEFAULT);
            if (vibration != null) b.setVibrate(vibration);
            notifManager.notify(notifId++, b.build());
        } catch(Exception e){}
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.evaluateJavascript(
                "(function(){try{if(typeof fetchLive==='function')fetchLive();" +
                "if(typeof renderCalendar==='function')renderCalendar();}catch(e){}})();", null);
        }
    }
}

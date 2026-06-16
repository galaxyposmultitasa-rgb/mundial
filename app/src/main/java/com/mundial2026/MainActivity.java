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
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(0xFF070e1c);
        getWindow().setNavigationBarColor(0xFF070e1c);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString("Mundial2026App/1.1 Android");

        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new NotifBridge(), "AndroidNotif");
        webView.loadUrl("file:///android_asset/index.html");

        createChannels();
        requestNotifPermission();
    }

    private void requestNotifPermission() {
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
            NotificationChannel g = new NotificationChannel(
                "goals", "⚽ Goles", NotificationManager.IMPORTANCE_HIGH);
            g.setVibrationPattern(new long[]{0,300,100,300,100,300});
            g.enableVibration(true);
            notifManager.createNotificationChannel(g);

            NotificationChannel a = new NotificationChannel(
                "alerts", "🔔 Alertas", NotificationManager.IMPORTANCE_DEFAULT);
            a.setVibrationPattern(new long[]{0,200,100,200});
            a.enableVibration(true);
            notifManager.createNotificationChannel(a);

            NotificationChannel d = new NotificationChannel(
                "daily", "📅 Recordatorio", NotificationManager.IMPORTANCE_LOW);
            notifManager.createNotificationChannel(d);
        }
    }

    public class NotifBridge {
        @JavascriptInterface
        public boolean isAndroid() { return true; }

        @JavascriptInterface
        public void sendGoalNotif(String title, String body) {
            post(title, body, "goals", new long[]{0,300,100,300,100,300},
                NotificationCompat.PRIORITY_HIGH);
        }

        @JavascriptInterface
        public void sendAlertNotif(String title, String body) {
            post(title, body, "alerts", new long[]{0,200,100,200},
                NotificationCompat.PRIORITY_DEFAULT);
        }

        @JavascriptInterface
        public void sendDailyNotif(String title, String body) {
            post(title, body, "daily", null,
                NotificationCompat.PRIORITY_LOW);
        }

        @JavascriptInterface
        public void vibrate(int ms) {
            try {
                android.os.Vibrator v =
                    (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (v != null) v.vibrate(ms);
            } catch (Exception e) {}
        }
    }

    private void post(String title, String body, String ch,
                      long[] vib, int priority) {
        if (notifManager == null) return;
        try {
            NotificationCompat.Builder b =
                new NotificationCompat.Builder(MainActivity.this, ch)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                    .setAutoCancel(true)
                    .setPriority(priority);
            if (vib != null) b.setVibrate(vib);
            notifManager.notify(notifId++, b.build());
        } catch (Exception e) {}
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
                "try{if(typeof renderCalendar==='function')renderCalendar();" +
                "if(typeof fetchLive==='function')fetchLive();}catch(e){}", null);
        }
    }
}

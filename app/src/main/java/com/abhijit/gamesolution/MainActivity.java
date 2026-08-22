package com.abhijit.gamesolution;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 100;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showUi();
    }

    @Override protected void onResume() {
        super.onResume();
        if (getWindow().getDecorView() != null) showUi();
    }

    private void showUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("GameSolution\nFloating App Restart Utility");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView status = new TextView(this);
        status.setPadding(0, 25, 0, 25);
        status.setText(buildStatus());
        root.addView(status);

        // Android 13+ can block sensitive Settings access for sideloaded apps.
        Button restricted = new Button(this);
        restricted.setText("1. Allow Restricted Settings");
        restricted.setOnClickListener(v -> openRestrictedSettingsHelp());
        root.addView(restricted);

        Button overlay = new Button(this);
        overlay.setText(Settings.canDrawOverlays(this) ? "✓ Overlay permission granted" : "2. Allow display over other apps");
        overlay.setOnClickListener(v -> openOverlaySettings());
        root.addView(overlay);

        Button usage = new Button(this);
        usage.setText(hasUsageAccess() ? "✓ Usage access granted" : "3. Allow usage access");
        usage.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        root.addView(usage);

        Button battery = new Button(this);
        battery.setText(isIgnoringBatteryOptimizations() ? "✓ Battery optimization ignored" : "4. Allow background operation");
        battery.setOnClickListener(v -> requestBatteryOptimizationExemption());
        root.addView(battery);

        Button notification = new Button(this);
        notification.setText(hasNotificationPermission() ? "✓ Notifications allowed" : "5. Allow notifications");
        notification.setOnClickListener(v -> requestNotificationPermission());
        root.addView(notification);

        Button start = new Button(this);
        start.setText("Start Floating Bubble");
        start.setEnabled(Settings.canDrawOverlays(this) && hasUsageAccess());
        start.setOnClickListener(v -> startFloatingService());
        root.addView(start);

        TextView note = new TextView(this);
        note.setText("The bubble stays above other apps. Tap it to show Restart App.\n\nFor Android 13+ sideloaded APKs, first open App Info → ⋮ → Allow restricted settings. Then grant Overlay and Usage Access.\n\nAndroid does not grant ordinary apps a permission to force-stop arbitrary apps. GameSolution therefore uses standard launcher relaunch APIs without root.");
        note.setPadding(0, 25, 0, 0);
        root.addView(note);
        setContentView(root);
    }

    private String buildStatus() {
        return "Setup status:\n" +
                "Overlay: " + (Settings.canDrawOverlays(this) ? "READY" : "REQUIRED") + "\n" +
                "Usage Access: " + (hasUsageAccess() ? "READY" : "REQUIRED") + "\n" +
                "Battery: " + (isIgnoringBatteryOptimizations() ? "OPTIMIZED" : "RESTRICTED") + "\n" +
                "Notifications: " + (hasNotificationPermission() ? "READY" : "OPTIONAL");
    }

    private boolean hasUsageAccess() {
        android.app.AppOpsManager appOps = (android.app.AppOpsManager) getSystemService(APP_OPS_SERVICE);
        if (appOps == null) return false;
        int mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), getPackageName());
        return mode == android.app.AppOpsManager.MODE_ALLOWED;
    }

    private boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isIgnoringBatteryOptimizations() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private void openRestrictedSettingsHelp() {
        Toast.makeText(this,
                "App Info खोलकर ⋮ → Allow restricted settings चुनें",
                Toast.LENGTH_LONG).show();
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission()) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    private void openOverlaySettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void startFloatingService() {
        if (!Settings.canDrawOverlays(this)) {
            openOverlaySettings();
            return;
        }
        if (!hasUsageAccess()) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            return;
        }
        requestNotificationPermission();
        Intent intent = new Intent(this, FloatingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent); else startService(intent);
    }
}

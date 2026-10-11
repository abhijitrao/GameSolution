package com.abhijit.gamesolution;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.text.InputType;
import android.view.WindowInsets;
import android.widget.Button;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 100;
    private LinearLayout root;
    private final int bg = Color.rgb(11, 15, 24), card = Color.rgb(22, 28, 40), card2 = Color.rgb(29, 36, 51);
    private final int primary = Color.rgb(105, 145, 255), text = Color.rgb(245, 247, 252), secondary = Color.rgb(165, 174, 194), success = Color.rgb(76, 205, 145);

    @Override protected void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg); showUi(); }
    @Override protected void onResume() { super.onResume(); if (root != null) showUi(); }

    private void showUi() {
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(bg); root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20), dp(18), dp(20), dp(28)); scroll.addView(root);
        root.setOnApplyWindowInsetsListener((v, insets) -> { int top = 0, bottom = 0; if (Build.VERSION.SDK_INT >= 30) { android.graphics.Insets i = insets.getInsets(WindowInsets.Type.systemBars()); top = i.top; bottom = i.bottom; } else { top = insets.getSystemWindowInsetTop(); bottom = insets.getSystemWindowInsetBottom(); } v.setPadding(dp(20), top + dp(18), dp(20), bottom + dp(28)); return insets; });

        TextView brand = text("GAME SOLUTION", 12, secondary, Typeface.BOLD); brand.setLetterSpacing(.16f); root.addView(brand);
        TextView title = text("Restart apps\nwithout the hassle.", 30, text, Typeface.BOLD); title.setPadding(0, dp(8), 0, dp(8)); root.addView(title);
        TextView subtitle = text("A lightweight floating utility for quickly relaunching the app currently in use.", 15, secondary, Typeface.NORMAL); subtitle.setLineSpacing(0, 1.12f); root.addView(subtitle, lp(-1, -2, 0, 0, 0, 18));

        LinearLayout statusCard = card(); statusCard.addView(text("SYSTEM STATUS", 11, secondary, Typeface.BOLD)); statusCard.addView(statusRow("Overlay permission", Settings.canDrawOverlays(this))); statusCard.addView(statusRow("Usage access", hasUsageAccess())); statusCard.addView(statusRow("Battery optimization", isIgnoringBatteryOptimizations())); statusCard.addView(statusRow("Notifications", hasNotificationPermission())); statusCard.addView(privateDnsStatusRow()); root.addView(statusCard, lp(-1, -2, 0, 0, 0, 18));

        TextView setup = text("SETUP", 11, secondary, Typeface.BOLD); setup.setLetterSpacing(.12f); root.addView(setup, lp(-1, -2, 0, 0, 0, 8));
        addAction("Allow Restricted Settings", "Required for sideloaded apps on Android 13+", !isRestrictedSettingReady(), v -> openRestrictedSettingsHelp());
        addAction("Display over other apps", "Required for the floating bubble", Settings.canDrawOverlays(this), v -> openOverlaySettings());
        addAction("Usage access", "Used only to identify the current app", hasUsageAccess(), v -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        addAction("Background operation", "Helps keep the floating utility alive", isIgnoringBatteryOptimizations(), v -> requestBatteryOptimizationExemption());
        addAction("Notifications", "Required for foreground-service status", hasNotificationPermission(), v -> requestNotificationPermission());
        addAction("Clear DNS-enabled app list", "Remove all saved app package names", getTrackedDnsAppCount() == 0, v -> confirmClearDnsEnabledApps());


        TextView actions = text("QUICK ACTION", 11, secondary, Typeface.BOLD); actions.setLetterSpacing(.12f); root.addView(actions, lp(-1, -2, 0, dp(10), 0, 8));
        Button start = button("START FLOATING BUBBLE", primary); start.setEnabled(Settings.canDrawOverlays(this) && hasUsageAccess()); start.setOnClickListener(v -> startFloatingService()); root.addView(start, lp(-1, dp(54), 0, 0, 0, 10));
        TextView hint = text("Once started, the bubble stays above other apps. Tap it to open Restart App and App Settings.", 13, secondary, Typeface.NORMAL); hint.setGravity(Gravity.CENTER); root.addView(hint, lp(-1, -2, 0, 4, 0, 0));
        TextView version = text("GameSolution 1.1", 12, Color.rgb(105, 115, 135), Typeface.NORMAL); version.setGravity(Gravity.CENTER); root.addView(version, lp(-1, -2, 0, 22, 0, 0));
        setContentView(scroll); root.requestApplyInsets();
    }

    private void addToggle(String title, String desc, boolean checked, final ToggleCallback callback) { LinearLayout item = new LinearLayout(this); item.setGravity(Gravity.CENTER_VERTICAL); item.setPadding(dp(16), dp(12), dp(10), dp(12)); item.setBackground(round(card, 16)); LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); TextView t = text(title, 15, text, Typeface.BOLD); TextView d = text(desc, 12, secondary, Typeface.NORMAL); d.setPadding(0, dp(3), 0, 0); copy.addView(t); copy.addView(d); item.addView(copy, new LinearLayout.LayoutParams(0, -2, 1)); Switch sw = new Switch(this); sw.setChecked(checked); sw.setOnCheckedChangeListener((buttonView, isChecked) -> callback.onChanged(isChecked)); item.addView(sw, new LinearLayout.LayoutParams(dp(56), -2)); item.setOnClickListener(v -> sw.setChecked(!sw.isChecked())); root.addView(item, lp(-1, -2, 0, 0, 0, 8)); }
    private interface ToggleCallback { void onChanged(boolean enabled); }
    private int getTrackedDnsAppCount() {
        java.util.Set<String> apps = getSharedPreferences("private_dns_settings", MODE_PRIVATE)
                .getStringSet("tracked_packages", java.util.Collections.<String>emptySet());
        return apps == null ? 0 : apps.size();
    }

    private void confirmClearDnsEnabledApps() {
        new AlertDialog.Builder(this)
                .setTitle("Clear DNS-enabled app list?")
                .setMessage("This removes all saved app package names from GameSolution. It will not change the device's current Private DNS setting.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear list", (dialog, which) -> {
                    getSharedPreferences("private_dns_settings", MODE_PRIVATE).edit()
                            .remove("tracked_packages")
                            .remove("pending_enable_package")
                            .apply();
                    Toast.makeText(this, "DNS-enabled app list cleared", Toast.LENGTH_SHORT).show();
                    recreate();
                })
                .show();
    }

    private void addAction(String title, String desc, boolean ready, View.OnClickListener listener) { LinearLayout item = new LinearLayout(this); item.setGravity(Gravity.CENTER_VERTICAL); item.setPadding(dp(16), dp(12), dp(12), dp(12)); item.setBackground(round(card, 16)); LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); TextView t = text(title, 15, text, Typeface.BOLD); TextView d = text(desc, 12, secondary, Typeface.NORMAL); d.setPadding(0, dp(3), 0, 0); copy.addView(t); copy.addView(d); item.addView(copy, new LinearLayout.LayoutParams(0, -2, 1)); TextView state = text(ready ? "READY" : "OPEN", 11, ready ? success : primary, Typeface.BOLD); state.setGravity(Gravity.CENTER); state.setPadding(dp(10), 0, dp(4), 0); item.addView(state, new LinearLayout.LayoutParams(dp(62), -1)); item.setOnClickListener(listener); root.addView(item, lp(-1, -2, 0, 0, 0, 8)); }
    private LinearLayout statusRow(String name, boolean ready) { LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0, dp(10), 0, 0); TextView n = text(name, 14, text, Typeface.NORMAL); row.addView(n, new LinearLayout.LayoutParams(0, -2, 1)); row.addView(text(ready ? "● READY" : "● REQUIRED", 12, ready ? success : Color.rgb(255, 184, 92), Typeface.BOLD)); return row; }
    private LinearLayout card() { LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(16), dp(16), dp(16), dp(16)); c.setBackground(round(card2, 18)); return c; }
    private Button button(String label, int color) { Button b = new Button(this); b.setText(label); b.setTextColor(Color.WHITE); b.setTextSize(13); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setAllCaps(false); b.setGravity(Gravity.CENTER); b.setBackground(round(color, 16)); return b; }
    private TextView text(String value, float size, int color, int style) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); v.setTypeface(Typeface.DEFAULT, style); return v; }
    private LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h); p.setMargins(dp(l), dp(t), dp(r), dp(b)); return p; }
    private GradientDrawable round(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private boolean hasUsageAccess() { android.app.AppOpsManager a = (android.app.AppOpsManager) getSystemService(APP_OPS_SERVICE); if (a == null) return false; return a.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), getPackageName()) == android.app.AppOpsManager.MODE_ALLOWED; }
    private boolean hasNotificationPermission() { return Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED; }
    private boolean isIgnoringBatteryOptimizations() { if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true; PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE); return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName()); }
    private boolean isRestrictedSettingReady() { return true; }
    private void openRestrictedSettingsHelp() { try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))); } catch (Exception ignored) {} }
    private void requestBatteryOptimizationExemption() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { try { startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName()))); } catch (Exception e) { startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); } } }
    private void requestNotificationPermission() { if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission()) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS); }
    private void openOverlaySettings() { startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))); }
    private String getPrivateDnsHostname() {
        return getSharedPreferences("private_dns_settings", MODE_PRIVATE).getString("hostname", "dns.adguard.com");
    }

    private String getSystemPrivateDnsMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return "unsupported";
        try {
            String mode = Settings.Global.getString(getContentResolver(), "private_dns_mode");
            return mode == null ? "unknown" : mode.toLowerCase(java.util.Locale.ROOT);
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    private String getSystemPrivateDnsSpecifier() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return "";
        try {
            String host = Settings.Global.getString(getContentResolver(), "private_dns_specifier");
            return host == null ? "" : host.trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private LinearLayout privateDnsStatusRow() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, 0);
        TextView label = text("Private DNS", 14, text, Typeface.NORMAL);
        row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        String mode = getSystemPrivateDnsMode();
        String configuredHost = getSystemPrivateDnsSpecifier();
        String status;
        int color;
        if ("unsupported".equals(mode)) {
            status = "UNSUPPORTED";
            color = secondary;
        } else if ("off".equals(mode)) {
            status = "DISABLED";
            color = Color.rgb(255, 184, 92);
        } else if ("hostname".equals(mode)) {
            if (configuredHost.equalsIgnoreCase(getPrivateDnsHostname())) {
                status = "ON • ADGUARD";
                color = success;
            } else if (!configuredHost.isEmpty()) {
                status = "ON • OTHER PROVIDER";
                color = Color.rgb(255, 184, 92);
            } else {
                status = "ON • HOSTNAME";
                color = Color.rgb(255, 184, 92);
            }
        } else if ("opportunistic".equals(mode)) {
            status = "ON • AUTOMATIC";
            color = success;
        } else {
            status = "STATUS UNKNOWN";
            color = Color.rgb(255, 184, 92);
        }
        TextView value = text(status, 12, color, Typeface.BOLD);
        value.setGravity(Gravity.CENTER);
        row.addView(value);
        if ("hostname".equals(mode) && !configuredHost.isEmpty() && !configuredHost.equalsIgnoreCase(getPrivateDnsHostname())) {
            TextView host = text("\\n" + configuredHost, 11, secondary, Typeface.NORMAL);
            host.setGravity(Gravity.END);
            row.addView(host);
        }
        return row;
    }
    private boolean isPrivateDnsSetupStarted() {
        return getSharedPreferences("private_dns_settings", MODE_PRIVATE).getBoolean("setup_started", false);
    }
    private void showPrivateDnsOptions() {
        if (Build.VERSION.SDK_INT < 28) {
            new AlertDialog.Builder(this).setTitle("Private DNS not supported")
                    .setMessage("System Private DNS requires Android 9 or newer. This device cannot use this feature.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        String[] options = {"Enable / Configure", "Change provider hostname", "Disable Private DNS"};
        new AlertDialog.Builder(this).setTitle("Private DNS / Ad Blocking")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showEnableDnsGuide();
                    else if (which == 1) editPrivateDnsHostname();
                    else openPrivateDnsSettings(false);
                }).setNegativeButton("Cancel", null).show();
    }

    private void showEnableDnsGuide() {
        String hostname = getPrivateDnsHostname();
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), dp(4));
        TextView intro = text("To enable ad blocking for all apps, enter this hostname in Android Private DNS settings.", 14, text, Typeface.NORMAL);
        intro.setLineSpacing(0, 1.1f);
        content.addView(intro, lp(-1, -2, 0, 0, 0, 12));
        LinearLayout hostRow = new LinearLayout(this);
        hostRow.setGravity(Gravity.CENTER_VERTICAL);
        hostRow.setPadding(dp(12), dp(8), dp(6), dp(8));
        hostRow.setBackground(round(card, 12));
        TextView host = text(hostname, 14, text, Typeface.BOLD);
        hostRow.addView(host, new LinearLayout.LayoutParams(0, -2, 1));
        Button copy = button("COPY", primary);
        copy.setMinHeight(dp(40));
        copy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("Private DNS hostname", hostname));
                Toast.makeText(this, "Hostname copied", Toast.LENGTH_SHORT).show();
            }
        });
        hostRow.addView(copy, new LinearLayout.LayoutParams(dp(86), dp(42)));
        content.addView(hostRow, lp(-1, -2, 0, 0, 0, 14));
        TextView guide = text("Steps:\\n1. Tap Open Settings below.\\n2. Select Private DNS provider hostname.\\n3. Paste the hostname shown above.\\n4. Tap Save.\\n\\nPrivate DNS affects all apps.", 13, secondary, Typeface.NORMAL);
        guide.setLineSpacing(dp(3), 1f);
        content.addView(guide);
        new AlertDialog.Builder(this).setTitle("Enable Ad Blocking")
                .setView(content)
                .setPositiveButton("Open Settings", (dialog, which) -> openPrivateDnsSettings(true))
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void editPrivateDnsHostname() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setText(getPrivateDnsHostname());
        input.setSelection(input.getText().length());
        new AlertDialog.Builder(this).setTitle("Provider hostname")
                .setMessage("Enter the Private DNS provider hostname.")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String hostname = input.getText().toString().trim();
                    if (hostname.isEmpty() || hostname.contains(" ") || hostname.contains("/")) {
                        new AlertDialog.Builder(this).setTitle("Invalid hostname")
                                .setMessage("Enter a valid hostname, for example dns.adguard.com.")
                                .setPositiveButton("OK", null).show();
                        return;
                    }
                    getSharedPreferences("private_dns_settings", MODE_PRIVATE).edit().putString("hostname", hostname).apply();
                    showUi();
                }).setNegativeButton("Cancel", null).show();
    }
    private void openPrivateDnsSettings(boolean enabling) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            showPrivateDnsOptions();
            return;
        }
        if (enabling) getSharedPreferences("private_dns_settings", MODE_PRIVATE).edit().putBoolean("setup_started", true).apply();
        try {
            Intent monitor = new Intent(this, PrivateDnsMonitorService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(monitor); else startService(monitor);
        } catch (Exception ignored) {}

        // Direct Private DNS navigation is unavailable on this device; open Wireless settings.
        try {
            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, "Could not open Wireless settings.", Toast.LENGTH_LONG).show();
        }
    }

    // TEMPORARY: test different Settings routes, then remove this action and method.
    private void showPrivateDnsIntentTest() {
        final String[] labels = {
                "1. PRIVATE_DNS_SETTINGS (direct)",
                "2. Settings + PrivateDnsSettings fragment",
                "3. Settings + network.PrivateDnsSettings fragment",
                "4. Settings + NetworkDashboardActivity",
                "5. Settings + NetworkDashboardFragment",
                "6. WIRELESS_SETTINGS",
                "7. NETWORK_OPERATOR_SETTINGS",
                "8. WIFI_SETTINGS",
                "9. INTERNET_CONNECTIVITY_SETTINGS",
                "10. DATA_USAGE_SETTINGS",
                "11. SETTINGS (general)",
                "12. Network settings via action string",
                "13. Settings + old NetworkDashboardSummary fragment",
                "14. Settings + NetworkDashboardActivity fragment"
        };
        final int[] selected = {0};
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("TEST — Private DNS routes")
                .setSingleChoiceItems(labels, 0, (d, which) -> selected[0] = which)
                .setPositiveButton("TRY SELECTED", null)
                .setNegativeButton("CLOSE", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Intent intent;
            switch (selected[0]) {
                case 0: intent = new Intent("android.settings.PRIVATE_DNS_SETTINGS"); break;
                case 1: intent = new Intent(Settings.ACTION_SETTINGS).putExtra(":settings:show_fragment", "com.android.settings.network.PrivateDnsSettings").putExtra(":settings:show_fragment_title", "Private DNS"); break;
                case 2: intent = new Intent(Settings.ACTION_SETTINGS).putExtra(":settings:show_fragment", "com.android.settings.network.PrivateDnsSettings"); break;
                case 3: intent = new Intent(Settings.ACTION_SETTINGS).putExtra(":settings:show_fragment", "com.android.settings.network.NetworkDashboardActivity"); break;
                case 4: intent = new Intent(Settings.ACTION_SETTINGS).putExtra(":settings:show_fragment", "com.android.settings.network.NetworkDashboardFragment"); break;
                case 5: intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS); break;
                case 6: intent = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS); break;
                case 7: intent = new Intent(Settings.ACTION_WIFI_SETTINGS); break;
                case 8: intent = new Intent("android.settings.INTERNET_CONNECTIVITY"); break;
                case 9: intent = new Intent(Settings.ACTION_DATA_USAGE_SETTINGS); break;
                case 10: intent = new Intent(Settings.ACTION_SETTINGS); break;
                case 11: intent = new Intent("android.settings.NETWORK_SETTINGS"); break;
                case 12: intent = new Intent(Settings.ACTION_SETTINGS).putExtra(":settings:show_fragment", "com.android.settings.network.NetworkDashboardSummary"); break;
                default: intent = new Intent(Settings.ACTION_SETTINGS).putExtra(":settings:show_fragment", "com.android.settings.network.NetworkDashboardActivity"); break;
            }
            try {
                startActivity(intent);
                Toast.makeText(this, "Launched route " + (selected[0] + 1) + ". Check the screen, then return and test another.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Route " + (selected[0] + 1) + " failed: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
            }
        }));
        dialog.show();
    }

    private void showExitDnsPrompt(Runnable exitAction) {
        if (!isPrivateDnsSetupStarted() || Build.VERSION.SDK_INT < 28) {
            exitAction.run();
            return;
        }
        new AlertDialog.Builder(this).setTitle("Ad Blocking")
                .setMessage("Keep Private DNS enabled for all apps, or open Android Settings to disable it?")
                .setPositiveButton("Disable", (dialog, which) -> {
                    // Keep the task alive while Android Settings opens; finishing immediately
                    // can interrupt navigation on some devices.
                    openPrivateDnsSettings(false);
                })
                .setNegativeButton("Keep Enabled", (dialog, which) -> exitAction.run())
                .setNeutralButton("Cancel", null)
                .show();
    }
    @Override public void onBackPressed() {
        showExitDnsPrompt(this::finish);
    }
    private void updateSecondaryBubbleService() { if (!Settings.canDrawOverlays(this) || !hasUsageAccess()) return; boolean enabled = BubbleSettings.isRecentAppBubbleEnabled(this) || BubbleSettings.isActivitySwitchBubbleEnabled(this); Intent intent = new Intent(this, SecondaryBubbleService.class); if (enabled) { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent); else startService(intent); } else { stopService(intent); } }
    private void startFloatingService() { if (!Settings.canDrawOverlays(this)) { openOverlaySettings(); return; } if (!hasUsageAccess()) { startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); return; } requestNotificationPermission(); BubbleSettings.setBubbleEnabled(this, true); Intent intent = new Intent(this, FloatingService.class); if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent); else startService(intent); updateSecondaryBubbleService(); try { Intent monitor = new Intent(this, PrivateDnsMonitorService.class); if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(monitor); else startService(monitor); } catch (Exception ignored) {} finishAndRemoveTask(); }
}

package com.abhijit.gamesolution;

import android.Manifest;
import android.app.Activity;
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
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
        TextView brand = text("GAME SOLUTION", 12, secondary, Typeface.BOLD); brand.setLetterSpacing(.16f); root.addView(brand); TextView title = text("Restart apps\nwithout the hassle.", 30, text, Typeface.BOLD); title.setPadding(0, dp(8), 0, dp(8)); root.addView(title); TextView subtitle = text("A lightweight floating utility for quickly relaunching the app currently in use.", 15, secondary, Typeface.NORMAL); subtitle.setLineSpacing(0, 1.12f); root.addView(subtitle, lp(-1, -2, 0, 0, 0, 18));
        LinearLayout statusCard = card(); statusCard.addView(text("SYSTEM STATUS", 11, secondary, Typeface.BOLD)); statusCard.addView(statusRow("Overlay permission", Settings.canDrawOverlays(this))); statusCard.addView(statusRow("Usage access", hasUsageAccess())); statusCard.addView(statusRow("Battery optimization", isIgnoringBatteryOptimizations())); statusCard.addView(statusRow("Notifications", hasNotificationPermission())); root.addView(statusCard, lp(-1, -2, 0, 0, 0, 18));
        TextView setup = text("SETUP", 11, secondary, Typeface.BOLD); setup.setLetterSpacing(.12f); root.addView(setup, lp(-1, -2, 0, 0, 0, 8)); addAction("Allow Restricted Settings", "Required for sideloaded apps on Android 13+", !isRestrictedSettingReady(), v -> openRestrictedSettingsHelp()); addAction("Display over other apps", "Required for the floating bubble", Settings.canDrawOverlays(this), v -> openOverlaySettings()); addAction("Usage access", "Used only to identify the current app", hasUsageAccess(), v -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))); addAction("Background operation", "Helps keep the floating utility alive", isIgnoringBatteryOptimizations(), v -> requestBatteryOptimizationExemption()); addAction("Notifications", "Required for foreground-service status", hasNotificationPermission(), v -> requestNotificationPermission());
        TextView settingsTitle = text("BUBBLE SETTINGS", 11, secondary, Typeface.BOLD); settingsTitle.setLetterSpacing(.12f); root.addView(settingsTitle, lp(-1, -2, 0, dp(10), 0, 8)); addAction("Bubble appearance", "Size and transparency", true, v -> showBubbleSettings());
        TextView actions = text("QUICK ACTION", 11, secondary, Typeface.BOLD); actions.setLetterSpacing(.12f); root.addView(actions, lp(-1, -2, 0, dp(10), 0, 8)); Button start = button("START FLOATING BUBBLE", primary); start.setEnabled(Settings.canDrawOverlays(this) && hasUsageAccess()); start.setOnClickListener(v -> startFloatingService()); root.addView(start, lp(-1, dp(54), 0, 0, 0, 10)); TextView hint = text("Once started, the bubble stays above other apps. Tap it to open Restart App and App Settings.", 13, secondary, Typeface.NORMAL); hint.setGravity(Gravity.CENTER); root.addView(hint, lp(-1, -2, 0, 4, 0, 0)); TextView version = text("GameSolution 1.1", 12, Color.rgb(105, 115, 135), Typeface.NORMAL); version.setGravity(Gravity.CENTER); root.addView(version, lp(-1, -2, 0, 22, 0, 0)); setContentView(scroll); root.requestApplyInsets();
    }
    private void showBubbleSettings() {
        final android.app.Dialog dialog = new android.app.Dialog(this); LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(22), dp(20), dp(22), dp(18)); box.setBackground(round(card2, 22));
        TextView title = text("Bubble Settings", 20, text, Typeface.BOLD); box.addView(title); box.addView(text("Adjust the floating bubble appearance", 13, secondary, Typeface.NORMAL), lp(-1, -2, 0, 4, 0, 18));
        TextView sizeValue = text("Size: " + BubbleSettings.getSize(this) + "dp", 14, text, Typeface.BOLD); box.addView(sizeValue); android.widget.SeekBar size = new android.widget.SeekBar(this); size.setMax(40); size.setProgress(BubbleSettings.getSize(this) - 40); box.addView(size, lp(-1, -2, 0, 0, 0, 14));
        TextView alphaValue = text("Transparency: " + BubbleSettings.getTransparencyPercent(this) + "%", 14, text, Typeface.BOLD); box.addView(alphaValue); android.widget.SeekBar alpha = new android.widget.SeekBar(this); alpha.setMax(80); alpha.setProgress(BubbleSettings.getTransparencyPercent(this) - 20); box.addView(alpha, lp(-1, -2, 0, 0, 0, 16));
        size.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() { public void onProgressChanged(android.widget.SeekBar s, int p, boolean f) { int value = 40 + p; sizeValue.setText("Size: " + value + "dp"); BubbleSettings.setSize(MainActivity.this, value); } public void onStartTrackingTouch(android.widget.SeekBar s) {} public void onStopTrackingTouch(android.widget.SeekBar s) {} });
        alpha.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() { public void onProgressChanged(android.widget.SeekBar s, int p, boolean f) { int value = 20 + p; alphaValue.setText("Transparency: " + value + "%"); BubbleSettings.setTransparency(MainActivity.this, value); } public void onStartTrackingTouch(android.widget.SeekBar s) {} public void onStopTrackingTouch(android.widget.SeekBar s) {} });
        Button done = button("DONE", primary); done.setOnClickListener(v -> dialog.dismiss()); box.addView(done, lp(-1, dp(50), 0, 4, 0, 0)); dialog.setContentView(box); android.view.Window w = dialog.getWindow(); if (w != null) { w.setBackgroundDrawableResource(android.R.color.transparent); w.setLayout((int)(getResources().getDisplayMetrics().widthPixels * .88f), -2); } dialog.show(); if (dialog.getWindow() != null) dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * .88f), -2);
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
    private void startFloatingService() { if (!Settings.canDrawOverlays(this)) { openOverlaySettings(); return; } if (!hasUsageAccess()) { startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); return; } requestNotificationPermission(); Intent intent = new Intent(this, FloatingService.class); if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent); else startService(intent); }
}

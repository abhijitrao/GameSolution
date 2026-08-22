package com.abhijit.gamesolution;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingService extends Service {
    private static final String CHANNEL_ID = "game_solution_overlay";
    private static final int BUBBLE_SIZE_DP = 58;
    private static final int MENU_WIDTH_DP = 292;
    private static final int MENU_GAP_DP = 10;
    private static final int DELETE_ZONE_SIZE_DP = 76;
    private static final int DELETE_TRIGGER_RADIUS_DP = 72;
    private WindowManager windowManager;
    private View bubbleView, menuView, deleteZoneView;
    private String targetPackage;
    private boolean dragging;
    private boolean deleteZoneActive;
    private float downX, downY;
    private int startX, startY;
    private final int navy = Color.rgb(16, 21, 34), card = Color.rgb(28, 35, 52), text = Color.rgb(245, 247, 252), secondary = Color.rgb(165, 174, 194), primary = Color.rgb(105, 145, 255), danger = Color.rgb(255, 105, 120);

    @Override public void onCreate() { super.onCreate(); createChannel(); startForeground(10, buildNotification()); if (Settings.canDrawOverlays(this)) showBubble(); else stopSelf(); }
    private Notification buildNotification() { return new Notification.Builder(this, CHANNEL_ID).setContentTitle("GameSolution").setContentText("Floating restart bubble is active").setSmallIcon(android.R.drawable.ic_menu_rotate).setOngoing(true).build(); }
    private void createChannel() { if (Build.VERSION.SDK_INT >= 26) { NotificationChannel c = new NotificationChannel(CHANNEL_ID, "GameSolution", NotificationManager.IMPORTANCE_LOW); ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c); } }

    private void showBubble() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        TextView bubble = new TextView(this);
        bubble.setText("G"); bubble.setTextColor(Color.WHITE); bubble.setTextSize(18); bubble.setTypeface(Typeface.DEFAULT, Typeface.BOLD); bubble.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(125, 155, 255), Color.rgb(72, 91, 205)}); bg.setShape(GradientDrawable.OVAL); bg.setStroke(dp(2), Color.argb(80, 255, 255, 255)); bubble.setBackground(bg); bubble.setElevation(14f);
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(dp(BUBBLE_SIZE_DP), dp(BUBBLE_SIZE_DP), overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START; params.x = getResources().getDisplayMetrics().widthPixels - dp(BUBBLE_SIZE_DP + 12); params.y = dp(220);
        bubble.setOnTouchListener((v, event) -> { switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: dragging = false; deleteZoneActive = false; downX = event.getRawX(); downY = event.getRawY(); startX = params.x; startY = params.y; return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - downX, dy = event.getRawY() - downY;
                if (Math.abs(dx) > dp(6) || Math.abs(dy) > dp(6)) { if (!dragging) { dragging = true; vibrate(12); removeMenu(); showDeleteZone(); } }
                params.x = clamp(startX + (int) dx, dp(4), getResources().getDisplayMetrics().widthPixels - dp(BUBBLE_SIZE_DP + 4)); params.y = clamp(startY + (int) dy, dp(48), getResources().getDisplayMetrics().heightPixels - dp(BUBBLE_SIZE_DP + 8)); windowManager.updateViewLayout(bubble, params); if (dragging) updateDeleteZoneState(params.x, params.y); return true;
            case MotionEvent.ACTION_UP:
                if (dragging) { boolean remove = deleteZoneActive; vibrate(remove ? 35 : 12); if (remove) { animateDeleteAndStop(bubble); return true; } hideDeleteZone(); } else { vibrate(18); openMenu(params.x, params.y); }
                return true;
        } return false; });
        bubbleView = bubble; windowManager.addView(bubbleView, params);
    }

    private void showDeleteZone() {
        if (deleteZoneView != null) return;
        TextView zone = new TextView(this);
        zone.setText("×"); zone.setTextColor(Color.WHITE); zone.setTextSize(30); zone.setTypeface(Typeface.DEFAULT, Typeface.BOLD); zone.setGravity(Gravity.CENTER);
        zone.setBackground(round(Color.rgb(95, 31, 43), 40)); zone.setElevation(18f); zone.setAlpha(0f); zone.setScaleX(0.65f); zone.setScaleY(0.65f);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(dp(DELETE_ZONE_SIZE_DP), dp(DELETE_ZONE_SIZE_DP), overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL; lp.y = dp(30);
        deleteZoneView = zone; windowManager.addView(deleteZoneView, lp);
        zone.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).setInterpolator(new DecelerateInterpolator()).start();
    }

    private void updateDeleteZoneState(int bubbleX, int bubbleY) {
        if (deleteZoneView == null) return;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        float bubbleCenterX = bubbleX + dp(BUBBLE_SIZE_DP) / 2f;
        float bubbleCenterY = bubbleY + dp(BUBBLE_SIZE_DP) / 2f;
        float zoneCenterX = screenWidth / 2f;
        float zoneCenterY = screenHeight - dp(30) - dp(DELETE_ZONE_SIZE_DP) / 2f;
        float distance = (float) Math.hypot(bubbleCenterX - zoneCenterX, bubbleCenterY - zoneCenterY);
        boolean active = distance <= dp(DELETE_TRIGGER_RADIUS_DP);
        if (active == deleteZoneActive) return;
        deleteZoneActive = active;
        if (active) { vibrate(28); deleteZoneView.animate().scaleX(1.25f).scaleY(1.25f).setDuration(120).start(); deleteZoneView.setBackground(round(danger, 40)); ((TextView) deleteZoneView).setText("🗑"); }
        else { deleteZoneView.animate().scaleX(1f).scaleY(1f).setDuration(120).start(); deleteZoneView.setBackground(round(Color.rgb(95, 31, 43), 40)); ((TextView) deleteZoneView).setText("×"); }
    }

    private void animateDeleteAndStop(View bubble) { hideDeleteZone(); bubble.animate().alpha(0f).scaleX(0.2f).scaleY(0.2f).setDuration(180).setInterpolator(new DecelerateInterpolator()).withEndAction(this::stopSelf).start(); }
    private void hideDeleteZone() { if (deleteZoneView == null || windowManager == null) return; View zone = deleteZoneView; deleteZoneView = null; zone.animate().alpha(0f).scaleX(0.65f).scaleY(0.65f).setDuration(120).withEndAction(() -> { try { windowManager.removeView(zone); } catch (Exception ignored) {} }).start(); deleteZoneActive = false; }

    private void openMenu(int bubbleX, int bubbleY) {
        removeMenu(); targetPackage = ForegroundAppResolver.getPreviousPackage(this, getPackageName());
        if (targetPackage == null) { Toast.makeText(this, "Unable to identify foreground app. Allow Usage Access.", Toast.LENGTH_SHORT).show(); return; }
        LinearLayout cardView = new LinearLayout(this); cardView.setOrientation(LinearLayout.VERTICAL); cardView.setPadding(dp(18), dp(14), dp(18), dp(14)); cardView.setBackground(round(navy, 20)); cardView.setElevation(22f);
        TextView eyebrow = label("CURRENT APP", 11, primary, Typeface.BOLD); cardView.addView(eyebrow); TextView title = label(getAppLabel(targetPackage), 17, text, Typeface.BOLD); title.setPadding(0, dp(3), 0, dp(1)); cardView.addView(title); TextView packageName = label(targetPackage, 11, secondary, Typeface.NORMAL); packageName.setSingleLine(true); packageName.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE); packageName.setPadding(0, 0, 0, dp(10)); cardView.addView(packageName);
        LinearLayout restart = option("↻", "Restart App", "Clear task and relaunch", text); cardView.addView(restart, rowLp(dp(56), dp(6))); restart.setOnClickListener(v -> { vibrate(25); String pkg = targetPackage; removeMenu(); boolean ok = RestartExecutor.restart(this, pkg); if (!ok) Toast.makeText(this, "Unable to relaunch " + pkg, Toast.LENGTH_SHORT).show(); });
        LinearLayout appSettings = option("⚙", "App Settings", "Open Android App Info", danger); cardView.addView(appSettings, rowLp(dp(56), dp(6))); appSettings.setOnClickListener(v -> { vibrate(25); String pkg = targetPackage; removeMenu(); boolean ok = RestartExecutor.openForceStopPage(this, pkg); if (!ok) Toast.makeText(this, "Unable to open App Info for " + pkg, Toast.LENGTH_SHORT).show(); });
        LinearLayout close = option("×", "Close", "Dismiss this menu", secondary); cardView.addView(close, rowLp(dp(50), 0)); close.setOnClickListener(v -> { vibrate(12); removeMenu(); });
        int screenWidth = getResources().getDisplayMetrics().widthPixels, menuWidth = dp(MENU_WIDTH_DP), gap = dp(MENU_GAP_DP); boolean bubbleOnLeft = bubbleX + dp(BUBBLE_SIZE_DP / 2) < screenWidth / 2; int menuX = bubbleOnLeft ? bubbleX + dp(BUBBLE_SIZE_DP) + gap : bubbleX - menuWidth - gap; menuX = clamp(menuX, dp(8), screenWidth - menuWidth - dp(8)); int menuY = clamp(bubbleY, dp(48), getResources().getDisplayMetrics().heightPixels - dp(330));
        WindowManager.LayoutParams menuParams = new WindowManager.LayoutParams(menuWidth, WindowManager.LayoutParams.WRAP_CONTENT, overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.TRANSLUCENT); menuParams.gravity = Gravity.TOP | Gravity.START; menuParams.x = menuX; menuParams.y = menuY; cardView.setOnTouchListener((v, event) -> { if (event.getActionMasked() == MotionEvent.ACTION_OUTSIDE) { removeMenu(); return true; } return false; }); menuView = cardView; windowManager.addView(menuView, menuParams);
    }

    private LinearLayout option(String icon, String title, String subtitle, int iconColor) { LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(10), 0, dp(8), 0); row.setBackground(round(card, 15)); row.setClickable(true); TextView i = label(icon, 22, iconColor, Typeface.BOLD); i.setGravity(Gravity.CENTER); row.addView(i, new LinearLayout.LayoutParams(dp(38), -1)); LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); copy.setGravity(Gravity.CENTER_VERTICAL); copy.addView(label(title, 14, text, Typeface.BOLD)); copy.addView(label(subtitle, 11, secondary, Typeface.NORMAL)); row.addView(copy, new LinearLayout.LayoutParams(0, -1, 1)); return row; }
    private LinearLayout.LayoutParams rowLp(int height, int bottomMargin) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, height); p.bottomMargin = dp(bottomMargin); return p; }
    private TextView label(String value, float size, int color, int style) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); v.setTypeface(Typeface.DEFAULT, style); return v; }
    private GradientDrawable round(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private String getAppLabel(String pkg) { try { ApplicationInfo info = getPackageManager().getApplicationInfo(pkg, 0); return getPackageManager().getApplicationLabel(info).toString(); } catch (PackageManager.NameNotFoundException e) { return pkg; } }
    private int overlayType() { return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE; }
    private int clamp(int value, int min, int max) { return Math.max(min, Math.min(value, max)); }
    private void vibrate(long durationMs) { try { if (Build.VERSION.SDK_INT >= 31) { VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE); if (vm != null) vm.getDefaultVibrator().vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)); } else { Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE); if (vibrator != null) vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)); } } catch (Exception ignored) {} }
    private void removeMenu() { if (menuView != null && windowManager != null) { try { windowManager.removeView(menuView); } catch (Exception ignored) {} menuView = null; } }
    @Override public void onDestroy() { hideDeleteZone(); removeMenu(); if (bubbleView != null && windowManager != null) { try { windowManager.removeView(bubbleView); } catch (Exception ignored) {} bubbleView = null; } super.onDestroy(); }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    @Override public IBinder onBind(Intent intent) { return null; }
}

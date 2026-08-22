package com.abhijit.gamesolution;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingService extends Service {
    private static final String CHANNEL_ID = "game_solution_overlay";
    private WindowManager windowManager;
    private View bubbleView;
    private View menuView;
    private String targetPackage;
    private boolean dragging;
    private float downX, downY;
    private int startX, startY;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(10, buildNotification());
        if (Settings.canDrawOverlays(this)) showBubble(); else stopSelf();
    }

    private Notification buildNotification() {
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("GameSolution")
                .setContentText("Floating restart bubble is active")
                .setSmallIcon(android.R.drawable.ic_menu_rotate)
                .setOngoing(true)
                .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "GameSolution", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
    }

    private void showBubble() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        TextView bubble = new TextView(this);
        bubble.setText("G");
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(18);
        bubble.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(63, 81, 181));
        bg.setShape(GradientDrawable.OVAL);
        bubble.setBackground(bg);
        bubble.setElevation(12f);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                dp(56), dp(56), overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = dp(12);
        params.y = dp(220);

        bubble.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    dragging = false;
                    downX = event.getRawX(); downY = event.getRawY();
                    startX = params.x; startY = params.y;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - downX;
                    float dy = event.getRawY() - downY;
                    if (Math.abs(dx) > dp(6) || Math.abs(dy) > dp(6)) dragging = true;
                    params.x = clamp(startX - (int) dx, dp(4), getResources().getDisplayMetrics().widthPixels - dp(60));
                    params.y = clamp(startY + (int) dy, dp(40), getResources().getDisplayMetrics().heightPixels - dp(70));
                    windowManager.updateViewLayout(bubble, params);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!dragging) openMenu(params.x, params.y);
                    return true;
            }
            return false;
        });
        bubbleView = bubble;
        windowManager.addView(bubbleView, params);
    }

    private void openMenu(int x, int y) {
        removeMenu();
        targetPackage = ForegroundAppResolver.getPreviousPackage(this, getPackageName());
        if (targetPackage == null) {
            Toast.makeText(this, "Unable to identify foreground app. Allow Usage Access.", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(10), dp(16), dp(8));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.WHITE);
        cardBg.setCornerRadius(dp(14));
        card.setBackground(cardBg);
        card.setElevation(18f);

        TextView title = new TextView(this);
        title.setText("Restart\n" + targetPackage);
        title.setTextColor(Color.DKGRAY);
        title.setTextSize(14);
        card.addView(title, new LinearLayout.LayoutParams(dp(250), dp(58)));

        TextView restart = createOption("Restart App");
        card.addView(restart, new LinearLayout.LayoutParams(dp(250), dp(48)));
        restart.setOnClickListener(v -> {
            String pkg = targetPackage;
            removeMenu();
            boolean ok = RestartExecutor.restart(this, pkg);
            if (!ok) Toast.makeText(this, "Unable to relaunch " + pkg, Toast.LENGTH_SHORT).show();
        });

        TextView forceStop = createOption("Force Stop & Restart");
        card.addView(forceStop, new LinearLayout.LayoutParams(dp(250), dp(52)));
        forceStop.setOnClickListener(v -> {
            String pkg = targetPackage;
            removeMenu();
            boolean ok = RestartExecutor.openForceStopPage(this, pkg);
            if (ok) {
                Toast.makeText(this, "Tap Force stop for the game, then open GameSolution again to relaunch it.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Unable to open App Info for " + pkg, Toast.LENGTH_SHORT).show();
            }
        });

        TextView close = createOption("Close");
        close.setTextColor(Color.GRAY);
        card.addView(close, new LinearLayout.LayoutParams(dp(250), dp(42)));
        close.setOnClickListener(v -> removeMenu());

        WindowManager.LayoutParams menuParams = new WindowManager.LayoutParams(
                dp(270), dp(245), overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        menuParams.gravity = Gravity.TOP | Gravity.END;
        menuParams.x = Math.max(dp(8), x + dp(70));
        menuParams.y = Math.max(dp(70), y);
        menuView = card;
        windowManager.addView(menuView, menuParams);
    }

    private TextView createOption(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.rgb(35, 35, 35));
        view.setTextSize(16);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(8), 0, 0, 0);
        view.setBackgroundColor(Color.TRANSPARENT);
        return view;
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private int clamp(int value, int min, int max) { return Math.max(min, Math.min(value, max)); }

    private void removeMenu() {
        if (menuView != null && windowManager != null) {
            try { windowManager.removeView(menuView); } catch (Exception ignored) {}
            menuView = null;
        }
    }

    @Override public void onDestroy() {
        removeMenu();
        if (bubbleView != null && windowManager != null) {
            try { windowManager.removeView(bubbleView); } catch (Exception ignored) {}
            bubbleView = null;
        }
        super.onDestroy();
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    @Override public IBinder onBind(Intent intent) { return null; }
}

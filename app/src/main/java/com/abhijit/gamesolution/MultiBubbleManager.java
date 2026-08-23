package com.abhijit.gamesolution;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Separate manager for secondary switch bubbles. The main bubble is not owned here. */
public final class MultiBubbleManager {
    public static final int MAX_ACTIVITY_BUBBLES = 3;
    private final Context context;
    private final WindowManager windowManager;
    private final List<View> activityBubbles = new ArrayList<>();
    private View recentBubble;

    public MultiBubbleManager(Context context, WindowManager windowManager) {
        this.context = context.getApplicationContext();
        this.windowManager = windowManager;
    }

    public void removeAllSecondaryBubbles() {
        removeRecentBubble();
        for (View view : new ArrayList<>(activityBubbles)) removeView(view);
        activityBubbles.clear();
    }

    public void removeRecentBubble() {
        if (recentBubble != null) {
            removeView(recentBubble);
            recentBubble = null;
        }
    }

    public void showRecentApp(String packageName) {
        if (packageName == null || packageName.equals(context.getPackageName())) return;
        removeRecentBubble();
        TextView bubble = createBubble(getAppIconLabel(packageName), 52);
        bubble.setOnClickListener(v -> bringAppToFront(packageName));
        recentBubble = bubble;
        addView(bubble, 52, 150);
    }

    public void showActivity(String packageName, String activityName) {
        if (packageName == null || activityName == null || activityName.isEmpty()) return;
        if (activityBubbles.size() >= MAX_ACTIVITY_BUBBLES) {
            removeView(activityBubbles.remove(0));
        }
        TextView bubble = createBubble(shortActivityName(activityName), 64);
        bubble.setTextSize(10);
        bubble.setPadding(4, 4, 4, 4);
        bubble.setOnClickListener(v -> bringAppToFront(packageName));
        activityBubbles.add(bubble);
        int index = activityBubbles.size() - 1;
        addView(bubble, 52, 230 + (index * 64));
    }

    private TextView createBubble(String label, int sizeDp) {
        TextView view = new TextView(context);
        view.setText(label);
        view.setTextColor(Color.WHITE);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setBackground(round(Color.rgb(72, 91, 205), 26));
        view.setElevation(14f);
        return view;
    }

    private void addView(View view, int xDp, int yDp) {
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                dp(52), dp(52), overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        p.x = dp(xDp);
        p.y = dp(yDp);
        try { windowManager.addView(view, p); } catch (Exception ignored) {}
    }

    private void removeView(View view) {
        try { windowManager.removeView(view); } catch (Exception ignored) {}
    }

    private void bringAppToFront(String packageName) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent == null) return;
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            context.startActivity(intent);
        } catch (Exception ignored) {}
    }

    private String getAppIconLabel(String packageName) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, 0);
            return context.getPackageManager().getApplicationIcon(info) != null ? "●" : "•";
        } catch (Exception ignored) { return "•"; }
    }

    private String shortActivityName(String activity) {
        int slash = activity.lastIndexOf('.');
        return slash >= 0 && slash + 1 < activity.length() ? activity.substring(slash + 1) : activity;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private int overlayType() {
        return android.os.Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

package com.abhijit.gamesolution;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Independent secondary bubbles. The main GameSolution bubble is never owned here. */
public final class MultiBubbleManager {
    private static final int DELETE_ZONE_SIZE_DP = 76;
    private static final int DELETE_TRIGGER_RADIUS_DP = 72;
    private static final int ACTIVITY_HEIGHT_DP = 42;
    private final Context context;
    private final WindowManager windowManager;
    private View recentBubble;
    private View activityBubble;
    private Handler trackerHandler;
    private Runnable tracker;
    private boolean recentRemoved;
    private boolean activityRemoved;
    private String lastRecentPackage;
    private String lastActivityPackage;
    private String lastActivityName;

    public MultiBubbleManager(Context context, WindowManager windowManager) {
        this.context = context.getApplicationContext();
        this.windowManager = windowManager;
    }

    public void startTracking(boolean recentEnabled, boolean activityEnabled) {
        stopTracking();
        trackerHandler = new Handler(Looper.getMainLooper());
        tracker = new Runnable() {
            @Override public void run() {
                if (recentEnabled && !recentRemoved) updateRecentApp();
                if (activityEnabled && !activityRemoved) updateCurrentActivity();
                if (trackerHandler != null) trackerHandler.postDelayed(this, 1000L);
            }
        };
        trackerHandler.post(tracker);
    }

    public void stopTracking() {
        if (trackerHandler != null && tracker != null) trackerHandler.removeCallbacks(tracker);
        trackerHandler = null;
        tracker = null;
    }

    public void removeAllSecondaryBubbles() {
        stopTracking();
        removeRecentBubble();
        removeActivityBubble();
    }

    public void removeRecentBubble() {
        if (recentBubble != null) removeView(recentBubble);
        recentBubble = null;
    }

    public void removeActivityBubble() {
        if (activityBubble != null) removeView(activityBubble);
        activityBubble = null;
    }

    public void showRecentApp(String packageName) {
        recentRemoved = false;
        lastRecentPackage = packageName;
        showRecentAppInternal(packageName);
    }

    public void showActivity(String packageName, String activityName) {
        activityRemoved = false;
        lastActivityPackage = packageName;
        lastActivityName = activityName;
        showActivityInternal(packageName, activityName);
    }

    private void updateRecentApp() {
        String packageName = ForegroundAppResolver.getPreviousPackage(context, context.getPackageName());
        if (packageName == null || packageName.equals(context.getPackageName())) return;
        if (packageName.equals(lastRecentPackage) && recentBubble != null) return;
        lastRecentPackage = packageName;
        showRecentAppInternal(packageName);
    }

    private void updateCurrentActivity() {
        ForegroundActivity current = findForegroundActivity();
        if (current == null) return;
        if (current.packageName.equals(context.getPackageName())) return;
        if (current.packageName.equals(lastActivityPackage)
                && current.activityName.equals(lastActivityName)
                && activityBubble != null) return;
        lastActivityPackage = current.packageName;
        lastActivityName = current.activityName;
        showActivityInternal(current.packageName, current.activityName);
    }

    private ForegroundActivity findForegroundActivity() {
        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (manager == null) return null;
        long end = System.currentTimeMillis();
        UsageEvents events = manager.queryEvents(end - 60_000L, end);
        if (events == null) return null;
        UsageEvents.Event event = new UsageEvents.Event();
        ForegroundActivity result = null;
        long latest = -1L;
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (event.getEventType() != UsageEvents.Event.ACTIVITY_RESUMED) continue;
            String pkg = event.getPackageName();
            String cls = event.getClassName();
            if (pkg == null || cls == null || pkg.equals(context.getPackageName())) continue;
            if (event.getTimeStamp() > latest) {
                latest = event.getTimeStamp();
                result = new ForegroundActivity(pkg, cls);
            }
        }
        return result;
    }

    private void showRecentAppInternal(String packageName) {
        if (packageName == null || packageName.equals(context.getPackageName())) return;
        if (recentBubble != null) removeView(recentBubble);
        ImageView bubble = createIconBubble(packageName);
        addBubble(bubble, packageName, false, 52);
        recentBubble = bubble;
    }

    private void showActivityInternal(String packageName, String activityName) {
        if (packageName == null || activityName == null || activityName.isEmpty()) return;
        if (activityBubble != null) removeView(activityBubble);
        TextView bubble = createTextBubble(shortActivityName(activityName));
        addBubble(bubble, packageName, true, 230);
        activityBubble = bubble;
    }

    private void addBubble(View view, String packageName, boolean displayOnly, int yDp) {
        int sw = context.getResources().getDisplayMetrics().widthPixels;
        int width = view instanceof TextView ? measureActivityWidth((TextView) view, sw) : dp(52);
        int height = view instanceof TextView ? dp(ACTIVITY_HEIGHT_DP) : dp(52);
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width, height, overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = Math.max(0, sw - width - dp(12));
        params.y = Math.max(dp(48), dp(yDp));

        view.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            int startX, startY;
            boolean moved, overDelete;
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX(); downY = event.getRawY();
                        startX = params.x; startY = params.y; moved = false; overDelete = false; return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - downX, dy = event.getRawY() - downY;
                        if (Math.abs(dx) > dp(6) || Math.abs(dy) > dp(6)) moved = true;
                        if (moved) {
                            int screenW = context.getResources().getDisplayMetrics().widthPixels;
                            int screenH = context.getResources().getDisplayMetrics().heightPixels;
                            int w = view.getWidth() > 0 ? view.getWidth() : params.width;
                            params.x = clamp(startX + (int) dx, 0, screenW - w);
                            params.y = clamp(startY + (int) dy, dp(48), screenH - params.height - dp(8));
                            try { windowManager.updateViewLayout(v, params); } catch (Exception ignored) {}
                            overDelete = isOverDeleteZone(params.x, params.y, w, params.height);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (moved) {
                            if (overDelete) {
                                if (v == recentBubble) { removeRecentBubble(); recentRemoved = true; }
                                else if (v == activityBubble) { removeActivityBubble(); activityRemoved = true; }
                            } else if (BubbleSettings.isSnapToEdge(context)) {
                                snapToEdge(v, params);
                            }
                            return true;
                        }
                        if (!displayOnly) bringAppToFront(packageName);
                        return true;
                    default: return true;
                }
            }
        });
        try { windowManager.addView(view, params); } catch (Exception ignored) {}
    }

    private int measureActivityWidth(TextView view, int screenWidth) {
        int max = Math.max(dp(72), screenWidth - dp(24));
        view.measure(View.MeasureSpec.makeMeasureSpec(max, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(dp(ACTIVITY_HEIGHT_DP), View.MeasureSpec.EXACTLY));
        return Math.min(max, Math.max(dp(72), view.getMeasuredWidth() + dp(18)));
    }

    private boolean isOverDeleteZone(int x, int y, int width, int height) {
        int sw = context.getResources().getDisplayMetrics().widthPixels;
        int sh = context.getResources().getDisplayMetrics().heightPixels;
        float cx = sw / 2f;
        float cy = sh - dp(30) - dp(DELETE_ZONE_SIZE_DP) / 2f;
        float closestX = Math.max(x, Math.min(cx, x + width));
        float closestY = Math.max(y, Math.min(cy, y + height));
        float dx = cx - closestX, dy = cy - closestY;
        return dx * dx + dy * dy <= (float) dp(DELETE_TRIGGER_RADIUS_DP) * dp(DELETE_TRIGGER_RADIUS_DP);
    }

    private void snapToEdge(View view, WindowManager.LayoutParams params) {
        int screenW = context.getResources().getDisplayMetrics().widthPixels;
        int width = view.getWidth() > 0 ? view.getWidth() : params.width;
        int target = params.x + width / 2 < screenW / 2 ? 0 : Math.max(0, screenW - width);
        int from = params.x;
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(from, target);
        animator.setDuration(220).setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> { params.x = (Integer) a.getAnimatedValue(); try { windowManager.updateViewLayout(view, params); } catch (Exception ignored) {} });
        animator.start();
    }

    private ImageView createIconBubble(String packageName) {
        ImageView view = new ImageView(context);
        try { ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, 0); Drawable icon = context.getPackageManager().getApplicationIcon(info); view.setImageDrawable(icon); } catch (Exception ignored) {}
        view.setPadding(dp(7), dp(7), dp(7), dp(7));
        view.setBackground(round(Color.rgb(72, 91, 205), 22));
        view.setElevation(14f);
        return view;
    }

    private TextView createTextBubble(String label) {
        TextView view = new TextView(context);
        view.setText(label); view.setTextColor(Color.WHITE); view.setTextSize(11); view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER); view.setSingleLine(true); view.setPadding(dp(12), 0, dp(12), 0);
        view.setBackground(round(Color.rgb(72, 91, 205), 14)); view.setElevation(14f); return view;
    }

    private void removeView(View view) { if (view != null) try { windowManager.removeView(view); } catch (Exception ignored) {} }
    private void bringAppToFront(String packageName) { try { Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName); if (intent == null) return; intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED); context.startActivity(intent); } catch (Exception ignored) {} }
    private String shortActivityName(String activity) { int dot = activity.lastIndexOf('.'); return dot >= 0 && dot + 1 < activity.length() ? activity.substring(dot + 1) : activity; }
    private GradientDrawable round(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private int overlayType() { return android.os.Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE; }
    private int dp(int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
    private static final class ForegroundActivity { final String packageName, activityName; ForegroundActivity(String p, String a) { packageName = p; activityName = a; } }
}

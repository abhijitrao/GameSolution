package com.abhijit.gamesolution;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Secondary bubbles. They are completely independent from the main GameSolution bubble. */
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
        for (View v : new ArrayList<>(activityBubbles)) removeView(v);
        activityBubbles.clear();
    }

    public void removeRecentBubble() {
        if (recentBubble != null) {
            removeView(recentBubble);
            recentBubble = null;
        }
    }

    /** Creates the recent-app icon once. Main bubble clicks never modify it. */
    public void showRecentApp(String packageName) {
        if (packageName == null || packageName.equals(context.getPackageName())) return;
        removeRecentBubble();
        ImageView bubble = createIconBubble(packageName);
        addBubble(bubble, packageName, dp(52), dp(150), false);
        recentBubble = bubble;
    }

    /** Creates a rectangular activity bubble whose width follows its activity label. */
    public void showActivity(String packageName, String activityName) {
        if (packageName == null || activityName == null || activityName.isEmpty()) return;
        if (activityBubbles.size() >= MAX_ACTIVITY_BUBBLES) {
            removeView(activityBubbles.remove(0));
        }
        TextView bubble = createTextBubble(shortActivityName(activityName));
        int index = activityBubbles.size();
        addBubble(bubble, packageName, WindowManager.LayoutParams.WRAP_CONTENT, dp(230 + index * 58), true);
        activityBubbles.add(bubble);
    }

    private void addBubble(View view, String packageName, int width, int yDp, boolean activityBubble) {
        final WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                width,
                dp(activityBubble ? 44 : 52),
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        p.x = dp(52);
        p.y = yDp;

        view.setClickable(true);
        view.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            int startX, startY;
            boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX();
                        downY = event.getRawY();
                        startX = p.x;
                        startY = p.y;
                        moved = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - downX;
                        float dy = event.getRawY() - downY;
                        if (Math.abs(dx) > dp(6) || Math.abs(dy) > dp(6)) moved = true;
                        if (moved) {
                            updatePosition(v, p, startX + (int) dx, startY + (int) dy);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            bringAppToFront(packageName);
                        } else if (BubbleSettings.isSnapToEdge(context)) {
                            snapToNearestEdge(v, p);
                        }
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        return true;

                    default:
                        return true;
                }
            }
        });

        try {
            windowManager.addView(view, p);
        } catch (Exception ignored) {
        }
    }

    private void updatePosition(View view, WindowManager.LayoutParams p, int x, int y) {
        int sw = context.getResources().getDisplayMetrics().widthPixels;
        int sh = context.getResources().getDisplayMetrics().heightPixels;
        int measuredWidth = view.getWidth() > 0 ? view.getWidth() : p.width;
        if (measuredWidth <= 0) measuredWidth = dp(52);
        int measuredHeight = view.getHeight() > 0 ? view.getHeight() : dp(44);
        p.x = Math.max(0, Math.min(sw - measuredWidth, x));
        p.y = Math.max(dp(48), Math.min(sh - measuredHeight - dp(8), y));
        try {
            windowManager.updateViewLayout(view, p);
        } catch (Exception ignored) {
        }
    }

    private void snapToNearestEdge(View view, WindowManager.LayoutParams p) {
        int sw = context.getResources().getDisplayMetrics().widthPixels;
        int width = view.getWidth() > 0 ? view.getWidth() : dp(52);
        int rightX = Math.max(0, sw - width);
        p.x = p.x + width / 2 < sw / 2 ? 0 : rightX;
        try {
            windowManager.updateViewLayout(view, p);
        } catch (Exception ignored) {
        }
    }

    private ImageView createIconBubble(String packageName) {
        ImageView view = new ImageView(context);
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, 0);
            Drawable icon = context.getPackageManager().getApplicationIcon(info);
            view.setImageDrawable(icon);
        } catch (Exception ignored) {
        }
        view.setPadding(dp(7), dp(7), dp(7), dp(7));
        view.setBackground(round(Color.rgb(72, 91, 205), 26));
        view.setElevation(14f);
        return view;
    }

    private TextView createTextBubble(String label) {
        TextView view = new TextView(context);
        view.setText(label);
        view.setTextColor(Color.WHITE);
        view.setTextSize(11);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setSingleLine(true);
        view.setPadding(dp(12), 0, dp(12), 0);
        view.setBackground(round(Color.rgb(72, 91, 205), 12));
        view.setElevation(14f);
        return view;
    }

    private void removeView(View view) {
        try {
            windowManager.removeView(view);
        } catch (Exception ignored) {
        }
    }

    private void bringAppToFront(String packageName) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent == null) return;
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            context.startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    private String shortActivityName(String activity) {
        int dot = activity.lastIndexOf('.');
        return dot >= 0 && dot + 1 < activity.length() ? activity.substring(dot + 1) : activity;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
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

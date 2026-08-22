package com.abhijit.gamesolution;

import android.content.Context;
import android.content.SharedPreferences;

public final class BubbleSettings {
    private static final String PREFS = "bubble_settings";
    private static final String SIZE = "size";
    private static final String TRANSPARENCY = "transparency";
    private static final String POS_X = "position_x_ratio";
    private static final String POS_Y = "position_y_ratio";
    private static final String SNAP = "snap_to_edge";
    private BubbleSettings() {}

    private static SharedPreferences prefs(Context context) { return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    public static int getSize(Context context) { return prefs(context).getInt(SIZE, 58); }
    public static void setSize(Context context, int size) { prefs(context).edit().putInt(SIZE, Math.max(40, Math.min(80, size))).apply(); }
    public static int getTransparencyPercent(Context context) { return prefs(context).getInt(TRANSPARENCY, 100); }
    public static int getTransparency(Context context) { return getTransparencyPercent(context); }
    public static void setTransparency(Context context, int percent) { prefs(context).edit().putInt(TRANSPARENCY, Math.max(20, Math.min(100, percent))).apply(); }

    public static boolean hasPosition(Context context) { return prefs(context).contains(POS_X) && prefs(context).contains(POS_Y); }
    public static float getPositionX(Context context) { return prefs(context).getFloat(POS_X, 0.9f); }
    public static float getPositionY(Context context) { return prefs(context).getFloat(POS_Y, 0.25f); }
    public static void setPosition(Context context, float xRatio, float yRatio) { prefs(context).edit().putFloat(POS_X, Math.max(0f, Math.min(1f, xRatio))).putFloat(POS_Y, Math.max(0f, Math.min(1f, yRatio))).apply(); }
    public static boolean isSnapToEdge(Context context) { return prefs(context).getBoolean(SNAP, false); }
    public static void setSnapToEdge(Context context, boolean enabled) { prefs(context).edit().putBoolean(SNAP, enabled).apply(); }
}
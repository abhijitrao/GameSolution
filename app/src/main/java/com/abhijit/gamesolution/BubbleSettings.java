package com.abhijit.gamesolution;

import android.content.Context;
import android.content.SharedPreferences;

public final class BubbleSettings {
    private static final String PREFS = "bubble_settings";
    private static final String SIZE = "size";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String SHAPE = "shape";
    private static final String TRANSPARENCY = "transparency";
    private static final String POS_X = "position_x_ratio";
    private static final String POS_Y = "position_y_ratio";
    private static final String SNAP = "snap_to_edge";
    private static final String SEMI_VISIBLE = "semi_icon_visible";
    private BubbleSettings() {}

    private static SharedPreferences prefs(Context context) { return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    public static int getSize(Context context) { return prefs(context).getInt(SIZE, 58); }
    public static void setSize(Context context, int size) { prefs(context).edit().putInt(SIZE, Math.max(5, Math.min(120, size))).apply(); }
    public static String getShape(Context context) { return prefs(context).getString(SHAPE, "CIRCLE"); }
    public static void setShape(Context context, String shape) { prefs(context).edit().putString(SHAPE, "OVAL".equals(shape) ? "OVAL" : "CIRCLE").apply(); }
    public static int getWidth(Context context) { return prefs(context).getInt(WIDTH, getSize(context)); }
    public static int getHeight(Context context) { return prefs(context).getInt(HEIGHT, getSize(context)); }
    public static void setWidth(Context context, int width) { prefs(context).edit().putInt(WIDTH, Math.max(5, Math.min(120, width))).apply(); }
    public static void setHeight(Context context, int height) { prefs(context).edit().putInt(HEIGHT, Math.max(5, Math.min(120, height))).apply(); }
    public static int getTransparencyPercent(Context context) { return prefs(context).getInt(TRANSPARENCY, 100); }
    public static int getTransparency(Context context) { return getTransparencyPercent(context); }
    public static void setTransparency(Context context, int percent) { prefs(context).edit().putInt(TRANSPARENCY, Math.max(20, Math.min(100, percent))).apply(); }

    public static boolean hasPosition(Context context) { return prefs(context).contains(POS_X) && prefs(context).contains(POS_Y); }
    public static float getPositionX(Context context) { return prefs(context).getFloat(POS_X, 0.9f); }
    public static float getPositionY(Context context) { return prefs(context).getFloat(POS_Y, 0.25f); }
    public static void setPosition(Context context, float xRatio, float yRatio) { prefs(context).edit().putFloat(POS_X, Math.max(-0.34f, Math.min(1f, xRatio))).putFloat(POS_Y, Math.max(0f, Math.min(1f, yRatio))).apply(); }
    public static boolean isSnapToEdge(Context context) { return prefs(context).getBoolean(SNAP, false); }
    public static void setSnapToEdge(Context context, boolean enabled) { prefs(context).edit().putBoolean(SNAP, enabled).apply(); }
    public static boolean isSemiIconVisible(Context context) { return prefs(context).getBoolean(SEMI_VISIBLE, false); }
    public static void setSemiIconVisible(Context context, boolean enabled) { prefs(context).edit().putBoolean(SEMI_VISIBLE, enabled).apply(); }
}
package com.abhijit.gamesolution;

import android.content.Context;
import android.content.SharedPreferences;

public final class BubbleSettings {
    private static final String PREFS = "bubble_settings";
    private static final String SIZE = "size";
    private static final String TRANSPARENCY = "transparency";
    private BubbleSettings() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static int getSize(Context context) {
        return prefs(context).getInt(SIZE, 58);
    }

    public static void setSize(Context context, int size) {
        prefs(context).edit().putInt(SIZE, Math.max(40, Math.min(80, size))).apply();
    }

    public static int getTransparencyPercent(Context context) {
        return prefs(context).getInt(TRANSPARENCY, 100);
    }

    public static void setTransparency(Context context, int percent) {
        prefs(context).edit().putInt(TRANSPARENCY, Math.max(20, Math.min(100, percent))).apply();
    }
}

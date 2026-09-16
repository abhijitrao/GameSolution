package com.abhijit.gamesolution;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Keeps the display awake for the currently foreground app when that app has
 * been enabled by the user. The enabled package list is persisted per app.
 */
public final class KeepScreenOnManager {
    private static final String PREFS = "keep_screen_on_settings";
    private static final String ENABLED_PACKAGES = "enabled_packages";
    private static final long CHECK_INTERVAL_MS = 1000L;

    private static Handler handler;
    private static Runnable checker;
    private static PowerManager.WakeLock wakeLock;
    private static String lockedPackage;

    private KeepScreenOnManager() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context context, String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        return prefs(context).getStringSet(ENABLED_PACKAGES, java.util.Collections.emptySet())
                .contains(packageName);
    }

    public static void setEnabled(Context context, String packageName, boolean enabled) {
        if (packageName == null || packageName.isEmpty()) return;
        SharedPreferences preferences = prefs(context);
        Set<String> packages = new HashSet<>(preferences.getStringSet(
                ENABLED_PACKAGES, java.util.Collections.emptySet()));
        if (enabled) packages.add(packageName); else packages.remove(packageName);
        preferences.edit().putStringSet(ENABLED_PACKAGES, packages).apply();
        refresh(context.getApplicationContext());
    }

    public static void start(Context context) {
        if (handler != null) return;
        final Context app = context.getApplicationContext();
        handler = new Handler(Looper.getMainLooper());
        checker = new Runnable() {
            @Override public void run() {
                refresh(app);
                if (handler != null) handler.postDelayed(this, CHECK_INTERVAL_MS);
            }
        };
        handler.post(checker);
    }

    public static void stop(Context context) {
        if (handler != null && checker != null) handler.removeCallbacks(checker);
        handler = null;
        checker = null;
        releaseWakeLock();
    }

    private static void refresh(Context context) {
        String currentPackage = ForegroundAppResolver.getCurrentPackage(
                context, context.getPackageName());
        if (currentPackage == null || !isEnabled(context, currentPackage)) {
            releaseWakeLock();
            return;
        }

        if (currentPackage.equals(lockedPackage) && wakeLock != null && wakeLock.isHeld()) return;

        releaseWakeLock();
        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager == null) return;
            wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                    context.getPackageName() + ":KeepScreenOn");
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire();
            lockedPackage = currentPackage;
        } catch (Exception ignored) {
            wakeLock = null;
            lockedPackage = null;
        }
    }

    private static void releaseWakeLock() {
        if (wakeLock != null) {
            try {
                if (wakeLock.isHeld()) wakeLock.release();
            } catch (Exception ignored) {}
        }
        wakeLock = null;
        lockedPackage = null;
    }
}

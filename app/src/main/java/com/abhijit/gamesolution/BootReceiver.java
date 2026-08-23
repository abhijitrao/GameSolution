package com.abhijit.gamesolution;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_USER_UNLOCKED.equals(action)) return;

        final Context appContext = context.getApplicationContext();
        if (!Settings.canDrawOverlays(appContext) || !BubbleSettings.isBubbleEnabled(appContext)) return;

        // On some Android/POS builds BOOT_COMPLETED is delivered before the launcher/user
        // is fully ready. Start after a short delay, and also handle USER_UNLOCKED.
        new Handler(Looper.getMainLooper()).postDelayed(() -> startFloatingService(appContext), 1500L);
    }

    private void startFloatingService(Context context) {
        if (!Settings.canDrawOverlays(context) || !BubbleSettings.isBubbleEnabled(context)) return;
        try {
            Intent service = new Intent(context, FloatingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
        } catch (Exception ignored) {
            // The receiver must not crash the system boot process.
        }
    }
}

package com.abhijit.gamesolution;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) &&
                !Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction())) return;
        if (!Settings.canDrawOverlays(context) || !BubbleSettings.isBubbleEnabled(context)) return;
        Intent service = new Intent(context, FloatingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(service);
        } else {
            context.startService(service);
        }
    }
}

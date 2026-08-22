package com.abhijit.gamesolution;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/** Best-effort restart for ordinary, non-root Android devices. */
public final class RestartExecutor {
    private RestartExecutor() {}

    public static boolean restart(Context context, String packageName) {
        if (packageName == null || packageName.equals(context.getPackageName())) return false;

        try {
            PackageManager pm = context.getPackageManager();
            Intent launch = pm.getLaunchIntentForPackage(packageName);
            if (launch == null) return false;
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            context.startActivity(launch);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}

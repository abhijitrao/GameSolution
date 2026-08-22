package com.abhijit.gamesolution;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import java.util.List;

/** Best-effort app restart for ordinary, non-root Android devices. */
public final class RestartExecutor {
    private RestartExecutor() {}

    public static boolean restart(Context context, String packageName) {
        if (packageName == null || packageName.equals(context.getPackageName())) return false;

        try {
            final PackageManager pm = context.getPackageManager();
            final Intent launch = findLaunchIntent(pm, packageName);
            if (launch == null) return false;

            // First move the target out of the foreground. Once it is in the background,
            // Android permits killBackgroundProcesses() for ordinary apps on many devices.
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(home);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                    if (am != null) am.killBackgroundProcesses(packageName);
                } catch (Exception ignored) {
                }

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                        context.startActivity(launch);
                    } catch (Exception ignored) {
                    }
                }, 250L);
            }, 250L);

            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Intent findLaunchIntent(PackageManager pm, String packageName) {
        Intent launch = pm.getLaunchIntentForPackage(packageName);
        if (launch != null) return launch;

        Intent launcherQuery = new Intent(Intent.ACTION_MAIN);
        launcherQuery.addCategory(Intent.CATEGORY_LAUNCHER);
        launcherQuery.setPackage(packageName);
        List<ResolveInfo> activities = pm.queryIntentActivities(
                launcherQuery, PackageManager.MATCH_DEFAULT_ONLY);
        if (!activities.isEmpty()) {
            ResolveInfo info = activities.get(0);
            Intent result = new Intent(Intent.ACTION_MAIN);
            result.addCategory(Intent.CATEGORY_LAUNCHER);
            result.setClassName(info.activityInfo.packageName, info.activityInfo.name);
            return result;
        }
        return null;
    }
}

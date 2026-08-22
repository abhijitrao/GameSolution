package com.abhijit.gamesolution;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.List;

/** Best-effort restart for ordinary, non-root Android devices. */
public final class RestartExecutor {
    private RestartExecutor() {}

    public static boolean restart(Context context, String packageName) {
        if (packageName == null || packageName.equals(context.getPackageName())) return false;

        try {
            PackageManager pm = context.getPackageManager();
            Intent launch = pm.getLaunchIntentForPackage(packageName);

            // Fallback for Android package-visibility/device-specific launcher behavior.
            if (launch == null) {
                Intent query = new Intent(Intent.ACTION_MAIN);
                query.addCategory(Intent.CATEGORY_LAUNCHER);
                query.setPackage(packageName);
                List<ResolveInfo> activities = pm.queryIntentActivities(query, PackageManager.MATCH_DEFAULT_ONLY);
                if (!activities.isEmpty()) {
                    ResolveInfo info = activities.get(0);
                    launch = new Intent(Intent.ACTION_MAIN);
                    launch.addCategory(Intent.CATEGORY_LAUNCHER);
                    launch.setClassName(info.activityInfo.packageName, info.activityInfo.name);
                }
            }

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

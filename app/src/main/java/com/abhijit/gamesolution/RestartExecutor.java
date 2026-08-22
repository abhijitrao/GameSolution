package com.abhijit.gamesolution;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import java.util.List;

/** Best-effort app task restart for ordinary, non-root Android devices. */
public final class RestartExecutor {
    private RestartExecutor() {}

    public static boolean restart(Context context, String packageName) {
        if (packageName == null || packageName.equals(context.getPackageName())) return false;
        try {
            final PackageManager pm = context.getPackageManager();
            final Intent launch = findLaunchIntent(pm, packageName);
            if (launch == null) return false;

            // Keep the working HOME transition, but explicitly create a fresh task.
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(home);

            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    context.startActivity(launch);
                } catch (Exception ignored) { }
            });
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean openForceStopPage(Context context, String packageName) {
        if (packageName == null || packageName.equals(context.getPackageName())) return false;
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
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

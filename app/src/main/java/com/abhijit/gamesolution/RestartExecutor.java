package com.abhijit.gamesolution;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
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
    private static final long RELAUNCH_INTERVAL_MS = 2000L;
    private static final int MAX_RELAUNCH_ATTEMPTS = 10;

    private RestartExecutor() {}

    public static boolean restart(Context context, String packageName) {
        if (packageName == null || packageName.equals(context.getPackageName())) return false;
        try {
            final PackageManager pm = context.getPackageManager();
            final Intent launch = findLaunchIntent(pm, packageName);
            if (launch == null) return false;

            // Keep the working HOME transition.
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(home);

            // Experiment 4: explicitly clear the target task and create a fresh task.
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            Handler handler = new Handler(Looper.getMainLooper());
            final int[] attempts = {0};
            Runnable relaunchCheck = new Runnable() {
                @Override
                public void run() {
                    if (isPackageForeground(context, packageName)) {
                        return;
                    }

                    if (attempts[0]++ >= MAX_RELAUNCH_ATTEMPTS) {
                        return;
                    }

                    try {
                        context.startActivity(launch);
                    } catch (Exception ignored) {
                    }

                    handler.postDelayed(this, RELAUNCH_INTERVAL_MS);
                }
            };

            // Give Home a moment to become foreground before the first launch.
            handler.postDelayed(relaunchCheck, 1500L);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isPackageForeground(Context context, String packageName) {
        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(
                Context.USAGE_STATS_SERVICE);
        if (manager == null) return false;

        long end = System.currentTimeMillis();
        UsageEvents events = manager.queryEvents(end - 10_000L, end);
        if (events == null) return false;

        UsageEvents.Event event = new UsageEvents.Event();
        long latestTimestamp = -1L;
        String latestPackage = null;

        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            int type = event.getEventType();
            if (type != UsageEvents.Event.ACTIVITY_RESUMED
                    && type != UsageEvents.Event.MOVE_TO_FOREGROUND) {
                continue;
            }
            if (event.getTimeStamp() > latestTimestamp) {
                latestTimestamp = event.getTimeStamp();
                latestPackage = event.getPackageName();
            }
        }
        return packageName.equals(latestPackage);
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

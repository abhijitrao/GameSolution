package com.abhijit.gamesolution;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;

/** Resolves the current and previous non-GameSolution app, ignoring launcher/home. */
public final class ForegroundAppResolver {
    private ForegroundAppResolver() {}

    public static String getCurrentPackage(Context context, String ownPackage) {
        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (manager == null) return null;
        long end = System.currentTimeMillis();
        long begin = end - 30 * 60_000L;
        UsageEvents events = manager.queryEvents(begin, end);
        String latestPackage = null;
        long latestTimestamp = -1L;
        if (events != null) {
            UsageEvents.Event event = new UsageEvents.Event();
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                String pkg = event.getPackageName();
                if (!isEligible(context, pkg, ownPackage)) continue;
                int type = event.getEventType();
                if (type != UsageEvents.Event.ACTIVITY_RESUMED && type != UsageEvents.Event.MOVE_TO_FOREGROUND) continue;
                if (event.getTimeStamp() > latestTimestamp) {
                    latestTimestamp = event.getTimeStamp();
                    latestPackage = pkg;
                }
            }
        }
        return latestPackage;
    }

    public static String getPreviousPackage(Context context, String ownPackage) {
        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (manager == null) return null;
        long end = System.currentTimeMillis();
        long begin = end - 30 * 60_000L;
        UsageEvents events = manager.queryEvents(begin, end);
        String latestPackage = null;
        String previousPackage = null;
        long latestTimestamp = -1L;
        if (events != null) {
            UsageEvents.Event event = new UsageEvents.Event();
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                String pkg = event.getPackageName();
                if (!isEligible(context, pkg, ownPackage)) continue;
                int type = event.getEventType();
                if (type != UsageEvents.Event.ACTIVITY_RESUMED && type != UsageEvents.Event.MOVE_TO_FOREGROUND) continue;
                long timestamp = event.getTimeStamp();
                if (timestamp <= latestTimestamp) continue;
                if (latestPackage != null && !latestPackage.equals(pkg)) previousPackage = latestPackage;
                latestPackage = pkg;
                latestTimestamp = timestamp;
            }
            if (previousPackage != null) return previousPackage;
        }

        long statsBegin = end - 24 * 60 * 60_000L;
        java.util.List<UsageStats> stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, statsBegin, end);
        if (stats != null) {
            UsageStats latest = null;
            UsageStats previous = null;
            for (UsageStats stat : stats) {
                String pkg = stat.getPackageName();
                if (!isEligible(context, pkg, ownPackage) || stat.getLastTimeUsed() <= 0L) continue;
                if (latest == null || stat.getLastTimeUsed() > latest.getLastTimeUsed()) {
                    previous = latest;
                    latest = stat;
                } else if (previous == null || stat.getLastTimeUsed() > previous.getLastTimeUsed()) {
                    previous = stat;
                }
            }
            if (previous != null) return previous.getPackageName();
        }
        return null;
    }

    private static boolean isEligible(Context context, String packageName, String ownPackage) {
        return packageName != null
                && !ownPackage.equals(packageName)
                && !isLauncher(context, packageName);
    }

    private static boolean isLauncher(Context context, String packageName) {
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_MAIN);
            intent.addCategory(android.content.Intent.CATEGORY_HOME);
            android.content.pm.ResolveInfo info = context.getPackageManager().resolveActivity(
                    intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
            return info != null && info.activityInfo != null
                    && packageName.equals(info.activityInfo.packageName);
        } catch (Exception ignored) {
            return false;
        }
    }
}

package com.abhijit.gamesolution;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.app.usage.UsageEvents;
import android.content.Context;

/** Resolves the app immediately before the current foreground app, ignoring GameSolution. */
public final class ForegroundAppResolver {
    private ForegroundAppResolver() {}

    public static String getPreviousPackage(Context context, String ownPackage) {
        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (manager == null) return null;
        long end = System.currentTimeMillis();
        UsageEvents events = manager.queryEvents(end - 30 * 60_000L, end);
        if (events != null) {
            UsageEvents.Event event = new UsageEvents.Event();
            String latestPackage = null;
            String previousPackage = null;
            long latestTimestamp = -1L;
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                String pkg = event.getPackageName();
                if (pkg == null || ownPackage.equals(pkg)) continue;
                int type = event.getEventType();
                if (type != UsageEvents.Event.ACTIVITY_RESUMED && type != UsageEvents.Event.MOVE_TO_FOREGROUND) continue;
                if (event.getTimeStamp() <= latestTimestamp) continue;
                if (latestPackage != null && !latestPackage.equals(pkg)) previousPackage = latestPackage;
                latestPackage = pkg;
                latestTimestamp = event.getTimeStamp();
            }
            if (previousPackage != null) return previousPackage;
        }
        long begin = end - 24 * 60 * 60_000L;
        java.util.List<UsageStats> stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin, end);
        if (stats != null) {
            UsageStats latest = null, previous = null;
            for (UsageStats stat : stats) {
                String pkg = stat.getPackageName();
                if (pkg == null || ownPackage.equals(pkg) || stat.getLastTimeUsed() <= 0L) continue;
                if (latest == null || stat.getLastTimeUsed() > latest.getLastTimeUsed()) { previous = latest; latest = stat; }
                else if (previous == null || stat.getLastTimeUsed() > previous.getLastTimeUsed()) previous = stat;
            }
            if (previous != null) return previous.getPackageName();
        }
        return null;
    }
}

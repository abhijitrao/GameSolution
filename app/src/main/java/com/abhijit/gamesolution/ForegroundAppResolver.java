package com.abhijit.gamesolution;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;

/** Resolves the most recently used package while ignoring GameSolution itself. */
public final class ForegroundAppResolver {
    private ForegroundAppResolver() {}

    public static String getPreviousPackage(Context context, String ownPackage) {
        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (manager == null) return null;

        // Prefer precise foreground events. Keep a wider window because some devices
        // may not expose a recent ACTIVITY_RESUMED event immediately after overlay use.
        long end = System.currentTimeMillis();
        long begin = end - 30 * 60_000L;
        UsageEvents events = manager.queryEvents(begin, end);
        if (events != null) {
            UsageEvents.Event event = new UsageEvents.Event();
            String latestPackage = null;
            long latestTimestamp = -1L;

            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                String pkg = event.getPackageName();
                if (pkg == null || ownPackage.equals(pkg)) continue;

                int type = event.getEventType();
                boolean foreground = type == UsageEvents.Event.ACTIVITY_RESUMED
                        || type == UsageEvents.Event.MOVE_TO_FOREGROUND;
                if (foreground && event.getTimeStamp() > latestTimestamp) {
                    latestTimestamp = event.getTimeStamp();
                    latestPackage = pkg;
                }
            }

            if (latestPackage != null) return latestPackage;
        }

        // Fallback for devices where UsageEvents is temporarily empty/incomplete.
        // queryUsageStats is less precise, but getLastTimeUsed still gives us the
        // most recently used application in the available history.
        long statsBegin = end - 24 * 60 * 60_000L;
        java.util.List<UsageStats> stats = manager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                statsBegin,
                end
        );
        if (stats != null && !stats.isEmpty()) {
            UsageStats latest = null;
            for (UsageStats stat : stats) {
                String pkg = stat.getPackageName();
                if (pkg == null || ownPackage.equals(pkg)) continue;
                if (latest == null || stat.getLastTimeUsed() > latest.getLastTimeUsed()) {
                    latest = stat;
                }
            }
            if (latest != null && latest.getLastTimeUsed() > 0L) {
                return latest.getPackageName();
            }
        }

        return null;
    }
}

package com.abhijit.gamesolution;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;

/** Resolves the most recently resumed package while ignoring GameSolution itself. */
public final class ForegroundAppResolver {
    private ForegroundAppResolver() {}

    public static String getPreviousPackage(Context context, String ownPackage) {
        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (manager == null) return null;

        long end = System.currentTimeMillis();
        long begin = end - 5 * 60_000L;
        UsageEvents events = manager.queryEvents(begin, end);
        if (events == null) return null;

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
        return latestPackage;
    }
}

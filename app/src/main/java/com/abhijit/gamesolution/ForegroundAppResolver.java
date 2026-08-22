package com.abhijit.gamesolution;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.SystemClock;

public final class ForegroundAppResolver {
    private ForegroundAppResolver() {}

    public static String getPreviousPackage(Context context, String ownPackage) {
        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (manager == null) return null;

        long end = System.currentTimeMillis();
        long begin = end - 60_000;
        UsageEvents events = manager.queryEvents(begin, end);
        UsageEvents.Event event = new UsageEvents.Event();
        String current = null;
        long latest = 0L;

        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            int type = event.getEventType();
            if ((type == UsageEvents.Event.ACTIVITY_RESUMED || type == UsageEvents.Event.MOVE_TO_FOREGROUND)
                    && event.getPackageName() != null
                    && !ownPackage.equals(event.getPackageName())
                    && event.getTimeStamp() >= latest) {
                latest = event.getTimeStamp();
                current = event.getPackageName();
            }
        }
        return current;
    }
}

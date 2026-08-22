package com.abhijit.gamesolution;

import android.app.ActivityManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.PixelFormat;
import android.net.TrafficStats;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.graphics.drawable.GradientDrawable;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayDeque;
import java.util.Locale;

public final class LiveMonitorDialog {
    private static View view;
    private static Handler handler;
    private static Runnable updater;

    private static TextView cpuValue, memoryValue, stateValue, pidValue, threadsValue;
    private static TextView activityValue, activityAgeValue, pssValue, privateDirtyValue;
    private static TextView javaHeapValue, nativeHeapValue, rxValue, txValue;
    private static TextView ramValue, availableRamValue, batteryValue, temperatureValue;
    private static TextView cpuHistoryValue, memoryHistoryValue;

    private static String lastActivity = "Not available";
    private static long lastActivityTimestamp = 0L;
    private static long activityStartTimestamp = 0L;
    private static long lastProcessJiffies = -1L;
    private static long lastTotalJiffies = -1L;
    private static long lastRx = -1L;
    private static long lastTx = -1L;
    private static final ArrayDeque<Integer> cpuHistory = new ArrayDeque<>();
    private static final ArrayDeque<Integer> memoryHistory = new ArrayDeque<>();

    private static final int NAVY = Color.rgb(16, 21, 34);
    private static final int CARD = Color.rgb(28, 35, 52);
    private static final int TEXT = Color.rgb(245, 247, 252);
    private static final int SECONDARY = Color.rgb(165, 174, 194);
    private static final int PRIMARY = Color.rgb(105, 145, 255);

    private LiveMonitorDialog() {}

    public static boolean isShowing() { return view != null; }

    public static void show(Context context, WindowManager wm, String packageName, Runnable onDismiss) {
        dismiss();
        resetRuntimeState();
        if (packageName == null || wm == null) return;

        try {
            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 14));
            root.setBackground(round(context, NAVY, 22));

            LinearLayout header = new LinearLayout(context);
            header.setGravity(Gravity.CENTER_VERTICAL);
            TextView title = text(context, "Live Monitor", 20, TEXT, Typeface.BOLD);
            header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
            TextView close = text(context, "×", 28, SECONDARY, Typeface.BOLD);
            close.setGravity(Gravity.CENTER);
            header.addView(close, new LinearLayout.LayoutParams(dp(context, 42), dp(context, 42)));
            root.addView(header);

            TextView app = text(context, safeLabel(context, packageName), 16, TEXT, Typeface.BOLD);
            root.addView(app, margin(context, -1, -2, 0, 4, 0, 0));
            TextView pkg = text(context, packageName, 10, SECONDARY, Typeface.NORMAL);
            pkg.setSingleLine(true);
            pkg.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            root.addView(pkg, margin(context, -1, -2, 0, 0, 0, 12));

            ScrollView scroll = new ScrollView(context);
            LinearLayout values = new LinearLayout(context);
            values.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(values);
            root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

            LinearLayout top = new LinearLayout(context);
            top.setOrientation(LinearLayout.HORIZONTAL);
            cpuValue = metricCard(context, top, "CPU", "N/A");
            memoryValue = metricCard(context, top, "Memory", "N/A");
            values.addView(top, margin(context, -1, -2, 0, 0, 0, 7));

            values.addView(section(context, "PROCESS"), margin(context, -1, -2, 0, 2, 0, 4));
            stateValue = rowValue(values, context, "State", "N/A");
            pidValue = rowValue(values, context, "PID", "N/A");
            threadsValue = rowValue(values, context, "Threads", "N/A");

            values.addView(section(context, "CURRENT ACTIVITY"), margin(context, -1, -2, 0, 6, 0, 4));
            activityValue = rowValue(values, context, "Activity", "Not available");
            activityAgeValue = rowValue(values, context, "Last detected", "N/A");

            values.addView(section(context, "MEMORY"), margin(context, -1, -2, 0, 6, 0, 4));
            pssValue = rowValue(values, context, "PSS", "N/A");
            privateDirtyValue = rowValue(values, context, "Private Dirty", "N/A");
            javaHeapValue = rowValue(values, context, "Java Heap", "N/A");
            nativeHeapValue = rowValue(values, context, "Native Heap", "N/A");

            values.addView(section(context, "NETWORK"), margin(context, -1, -2, 0, 6, 0, 4));
            rxValue = rowValue(values, context, "Received", "N/A");
            txValue = rowValue(values, context, "Sent", "N/A");

            values.addView(section(context, "SYSTEM"), margin(context, -1, -2, 0, 6, 0, 4));
            ramValue = rowValue(values, context, "RAM Used", "N/A");
            availableRamValue = rowValue(values, context, "RAM Available", "N/A");
            batteryValue = rowValue(values, context, "Battery", "N/A");
            temperatureValue = rowValue(values, context, "Temperature", "N/A");

            values.addView(section(context, "HISTORY"), margin(context, -1, -2, 0, 6, 0, 4));
            cpuHistoryValue = rowValue(values, context, "CPU", "—");
            memoryHistoryValue = rowValue(values, context, "Memory", "—");

            TextView footer = text(context, "● LIVE  •  Updating every 1 second", 11, PRIMARY, Typeface.BOLD);
            footer.setPadding(0, dp(context, 10), 0, 0);
            root.addView(footer);

            close.setOnClickListener(v -> {
                dismiss();
                if (onDismiss != null) onDismiss.run();
            });

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    dp(context, 340), dp(context, 620), overlayType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.CENTER;
            root.setOnTouchListener((v, e) -> {
                if (e.getActionMasked() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    if (onDismiss != null) onDismiss.run();
                    return true;
                }
                return false;
            });

            view = root;
            wm.addView(view, lp);
            handler = new Handler(Looper.getMainLooper());
            updater = new Runnable() {
                @Override public void run() {
                    if (view == null) return;
                    update(context, packageName);
                    handler.postDelayed(this, 1000L);
                }
            };
            update(context, packageName);
            handler.postDelayed(updater, 1000L);
        } catch (Exception ignored) {
            dismiss();
        }
    }

    private static void update(Context c, String pkg) {
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo systemMemory = new ActivityManager.MemoryInfo();
        if (am != null) am.getMemoryInfo(systemMemory);

        int pid = findPid(am, pkg);
        Debug.MemoryInfo processMemory = null;
        if (am != null && pid > 0) {
            try {
                Debug.MemoryInfo[] info = am.getProcessMemoryInfo(new int[]{pid});
                if (info != null && info.length > 0) processMemory = info[0];
            } catch (Exception ignored) {}
        }

        int cpu = readCpuPercent(pid);
        if (cpu >= 0) addHistory(cpuHistory, cpu);
        long pss = processMemory != null ? processMemory.getTotalPss() * 1024L : 0L;
        int memoryMb = processMemory != null ? (int) Math.round(processMemory.getTotalPss() / 1024.0) : -1;
        if (memoryMb >= 0) addHistory(memoryHistory, memoryMb);

        String state = isForeground(c, pkg) ? "Foreground" : "Background";
        stateValue.setText(state);
        pidValue.setText(pid > 0 ? String.valueOf(pid) : "Not available");
        threadsValue.setText(pid > 0 ? String.valueOf(readThreadCount(pid)) : "Not available");
        cpuValue.setText(cpu >= 0 ? cpu + "%" : "N/A");
        memoryValue.setText(formatBytes(pss));

        ActivitySnapshot snapshot = findActivity(c, pkg);
        if (snapshot.activity != null) {
            if (!snapshot.activity.equals(lastActivity)) activityStartTimestamp = snapshot.timestamp;
            lastActivity = snapshot.activity;
            lastActivityTimestamp = snapshot.timestamp;
        }
        activityValue.setText(lastActivity);
        if (lastActivityTimestamp > 0) {
            String suffix = isForeground(c, pkg) ? "Active for " + duration(activityStartTimestamp)
                    : "Last detected " + ago(lastActivityTimestamp);
            activityAgeValue.setText(suffix);
        } else {
            activityAgeValue.setText("Not available");
        }

        if (processMemory != null) {
            pssValue.setText(formatBytes(pss));
            privateDirtyValue.setText(formatBytes(processMemory.getTotalPrivateDirty() * 1024L));
            javaHeapValue.setText(formatBytes(processMemory.dalvikPss * 1024L));
            nativeHeapValue.setText(formatBytes(processMemory.nativePss * 1024L));
        } else {
            pssValue.setText("N/A");
            privateDirtyValue.setText("N/A");
            javaHeapValue.setText("N/A");
            nativeHeapValue.setText("N/A");
        }

        long uid = uidForPackage(c, pkg);
        long rx = uid >= 0 ? TrafficStats.getUidRxBytes((int) uid) : TrafficStats.UNSUPPORTED;
        long tx = uid >= 0 ? TrafficStats.getUidTxBytes((int) uid) : TrafficStats.UNSUPPORTED;
        rxValue.setText(formatDelta(rx, lastRx));
        txValue.setText(formatDelta(tx, lastTx));
        if (rx >= 0) lastRx = rx;
        if (tx >= 0) lastTx = tx;

        ramValue.setText(formatBytes(Math.max(0, systemMemory.totalMem - systemMemory.availMem))
                + " / " + formatBytes(systemMemory.totalMem));
        availableRamValue.setText(formatBytes(systemMemory.availMem));
        batteryValue.setText(readBattery(c));
        temperatureValue.setText(readTemperature(c));
        cpuHistoryValue.setText(sparkline(cpuHistory) + "  " + (cpu >= 0 ? cpu + "%" : "N/A"));
        memoryHistoryValue.setText(sparkline(memoryHistory) + "  " + (memoryMb >= 0 ? memoryMb + " MB" : "N/A"));
    }

    private static int readCpuPercent(int pid) {
        if (pid <= 0) return -1;
        long process = readProcessJiffies(pid);
        long total = readTotalJiffies();
        if (process < 0 || total < 0 || lastProcessJiffies < 0 || lastTotalJiffies < 0) {
            lastProcessJiffies = process;
            lastTotalJiffies = total;
            return -1;
        }
        long processDelta = process - lastProcessJiffies;
        long totalDelta = total - lastTotalJiffies;
        lastProcessJiffies = process;
        lastTotalJiffies = total;
        if (processDelta < 0 || totalDelta <= 0) return -1;
        double percent = (processDelta * 100.0 * Runtime.getRuntime().availableProcessors()) / totalDelta;
        return Math.max(0, Math.min(100, (int) Math.round(percent)));
    }

    private static long readProcessJiffies(int pid) {
        try (BufferedReader r = new BufferedReader(new FileReader("/proc/" + pid + "/stat"))) {
            String line = r.readLine();
            if (line == null) return -1;
            int close = line.lastIndexOf(')');
            if (close < 0) return -1;
            String[] parts = line.substring(close + 2).trim().split("\\s+");
            if (parts.length < 13) return -1;
            return Long.parseLong(parts[11]) + Long.parseLong(parts[12]);
        } catch (Exception ignored) { return -1; }
    }

    private static long readTotalJiffies() {
        try (BufferedReader r = new BufferedReader(new FileReader("/proc/stat"))) {
            String line = r.readLine();
            if (line == null || !line.startsWith("cpu ")) return -1;
            String[] p = line.trim().split("\\s+");
            long total = 0;
            for (int i = 1; i < p.length; i++) total += Long.parseLong(p[i]);
            return total;
        } catch (Exception ignored) { return -1; }
    }

    private static int readThreadCount(int pid) {
        try (BufferedReader r = new BufferedReader(new FileReader("/proc/" + pid + "/status"))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("Threads:")) return Integer.parseInt(line.substring(8).trim());
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private static ActivitySnapshot findActivity(Context c, String pkg) {
        try {
            UsageStatsManager u = (UsageStatsManager) c.getSystemService(Context.USAGE_STATS_SERVICE);
            if (u == null) return new ActivitySnapshot(null, 0);
            long end = System.currentTimeMillis();
            UsageEvents ev = u.queryEvents(end - 60_000L, end);
            if (ev == null) return new ActivitySnapshot(null, 0);
            UsageEvents.Event x = new UsageEvents.Event();
            long t = -1;
            String cls = null;
            while (ev.hasNextEvent()) {
                ev.getNextEvent(x);
                if (x.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED
                        && pkg.equals(x.getPackageName()) && x.getTimeStamp() > t) {
                    t = x.getTimeStamp();
                    cls = x.getClassName();
                }
            }
            return new ActivitySnapshot(cls, t);
        } catch (Exception ignored) { return new ActivitySnapshot(null, 0); }
    }

    private static boolean isForeground(Context c, String pkg) {
        ActivitySnapshot s = findActivity(c, pkg);
        return s.activity != null && s.timestamp > 0 && (System.currentTimeMillis() - s.timestamp) < 60_000L;
    }

    private static long uidForPackage(Context c, String pkg) {
        try { return c.getPackageManager().getApplicationInfo(pkg, 0).uid; }
        catch (Exception e) { return -1; }
    }

    private static String readBattery(Context c) {
        try {
            android.content.Intent i = c.registerReceiver(null, new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
            if (i == null) return "N/A";
            int level = i.getIntExtra("level", -1), scale = i.getIntExtra("scale", -1);
            return level >= 0 && scale > 0 ? Math.round(level * 100f / scale) + "%" : "N/A";
        } catch (Exception e) { return "N/A"; }
    }

    private static String readTemperature(Context c) {
        try {
            android.content.Intent i = c.registerReceiver(null, new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
            if (i == null) return "N/A";
            int t = i.getIntExtra("temperature", Integer.MIN_VALUE);
            return t == Integer.MIN_VALUE ? "N/A" : String.format(Locale.US, "%.1f°C", t / 10f);
        } catch (Exception e) { return "N/A"; }
    }

    private static String formatDelta(long current, long previous) {
        if (current == TrafficStats.UNSUPPORTED || current < 0) return "N/A";
        if (previous < 0 || current < previous) return formatBytes(current);
        return formatBytes(current) + "  (" + formatBytes(current - previous) + "/s)";
    }

    private static void addHistory(ArrayDeque<Integer> q, int value) {
        if (q.size() >= 30) q.removeFirst();
        q.addLast(value);
    }

    private static String sparkline(ArrayDeque<Integer> values) {
        if (values.isEmpty()) return "—";
        final char[] chars = {'▁','▂','▃','▄','▅','▆','▇','█'};
        int max = 1;
        for (int v : values) max = Math.max(max, v);
        StringBuilder b = new StringBuilder();
        for (int v : values) b.append(chars[Math.min(chars.length - 1, Math.max(0, v * (chars.length - 1) / max))]);
        return b.toString();
    }

    private static String duration(long start) {
        if (start <= 0) return "N/A";
        return formatDuration(System.currentTimeMillis() - start);
    }

    private static String ago(long timestamp) {
        return formatDuration(Math.max(0, System.currentTimeMillis() - timestamp)) + " ago";
    }

    private static String formatDuration(long millis) {
        long sec = millis / 1000;
        if (sec < 60) return sec + "s";
        long min = sec / 60;
        if (min < 60) return min + "m " + (sec % 60) + "s";
        long hr = min / 60;
        return hr + "h " + (min % 60) + "m";
    }

    private static void resetRuntimeState() {
        lastActivity = "Not available";
        lastActivityTimestamp = 0L;
        activityStartTimestamp = 0L;
        lastProcessJiffies = -1L;
        lastTotalJiffies = -1L;
        lastRx = -1L;
        lastTx = -1L;
        cpuHistory.clear();
        memoryHistory.clear();
    }

    private static TextView metricCard(Context c, LinearLayout parent, String title, String initial) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(c, 10), dp(c, 9), dp(c, 10), dp(c, 9));
        card.setBackground(round(c, CARD, 14));
        TextView label = text(c, title, 10, SECONDARY, Typeface.BOLD);
        label.setGravity(Gravity.CENTER);
        card.addView(label);
        TextView value = text(c, initial, 18, TEXT, Typeface.BOLD);
        value.setGravity(Gravity.CENTER);
        card.addView(value);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(c, 66), 1);
        lp.setMargins(0, 0, dp(c, 5), 0);
        parent.addView(card, lp);
        return value;
    }

    private static TextView rowValue(LinearLayout parent, Context c, String key, String value) {
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(c, 12), dp(c, 8), dp(c, 8), dp(c, 8));
        row.setBackground(round(c, CARD, 12));
        TextView a = text(c, key, 11, SECONDARY, Typeface.BOLD);
        row.addView(a, new LinearLayout.LayoutParams(0, -2, 1));
        TextView b = text(c, value, 12, TEXT, Typeface.BOLD);
        b.setGravity(Gravity.END);
        b.setMaxLines(3);
        row.addView(b, new LinearLayout.LayoutParams(0, -2, 1));
        parent.addView(row, margin(c, -1, -2, 0, 0, 0, 5));
        return b;
    }

    private static TextView section(Context c, String title) {
        TextView t = text(c, title, 10, PRIMARY, Typeface.BOLD);
        t.setLetterSpacing(.08f);
        return t;
    }

    private static String safeLabel(Context c, String pkg) {
        try {
            ApplicationInfo a = c.getPackageManager().getApplicationInfo(pkg, 0);
            return c.getPackageManager().getApplicationLabel(a).toString();
        } catch (Exception e) { return pkg; }
    }

    private static String formatBytes(long b) {
        if (b < 0) return "N/A";
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format(Locale.US, "%.1f KB", b / 1024f);
        if (b < 1024L * 1024L * 1024L) return String.format(Locale.US, "%.1f MB", b / (1024f * 1024f));
        return String.format(Locale.US, "%.2f GB", b / (1024f * 1024f * 1024f));
    }

    public static void dismiss() {
        if (handler != null && updater != null) handler.removeCallbacks(updater);
        handler = null;
        updater = null;
        if (view != null) {
            try {
                WindowManager wm = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
                if (wm != null) wm.removeView(view);
            } catch (Exception ignored) {}
            view = null;
        }
    }

    private static GradientDrawable round(Context c, int col, int r) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(col);
        d.setCornerRadius(dp(c, r));
        return d;
    }

    private static LinearLayout.LayoutParams margin(Context c, int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        return p;
    }

    private static TextView text(Context c, String s, float z, int col, int st) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(z);
        t.setTextColor(col);
        t.setTypeface(Typeface.DEFAULT, st);
        return t;
    }

    private static int dp(Context c, int v) { return (int) (v * c.getResources().getDisplayMetrics().density + .5f); }

    private static int overlayType() {
        return android.os.Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private static final class ActivitySnapshot {
        final String activity;
        final long timestamp;
        ActivitySnapshot(String activity, long timestamp) { this.activity = activity; this.timestamp = timestamp; }
    }
}

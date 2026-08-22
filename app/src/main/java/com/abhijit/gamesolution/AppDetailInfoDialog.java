package com.abhijit.gamesolution;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.text.DateFormat;
import java.util.Date;

public final class AppDetailInfoDialog {
    private AppDetailInfoDialog() {}

    public static void show(Context context, String packageName, WindowManager windowManager, View currentMenu, Runnable dismissCurrentMenu) {
        if (currentMenu != null && dismissCurrentMenu != null) dismissCurrentMenu.run();
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS | PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS | PackageManager.GET_PROVIDERS);
            ApplicationInfo ai = pi.applicationInfo;
            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(context, 20), dp(context, 18), dp(context, 20), dp(context, 16));
            root.setBackground(round(context, Color.rgb(16,21,34), 22));

            LinearLayout header = new LinearLayout(context);
            header.setGravity(Gravity.CENTER_VERTICAL);
            TextView title = text(context, "App Detail Info", 20, Color.WHITE, Typeface.BOLD);
            header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
            TextView close = text(context, "×", 28, Color.rgb(165,174,194), Typeface.BOLD);
            close.setGravity(Gravity.CENTER);
            header.addView(close, new LinearLayout.LayoutParams(dp(context, 42), dp(context, 42)));
            root.addView(header);

            TextView name = text(context, pm.getApplicationLabel(ai).toString(), 17, Color.WHITE, Typeface.BOLD);
            name.setPadding(0, dp(context, 4), 0, dp(context, 2));
            root.addView(name);
            TextView pkg = text(context, packageName, 11, Color.rgb(165,174,194), Typeface.NORMAL);
            pkg.setSingleLine(true);
            pkg.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            root.addView(pkg, margin(context, -1, -2, 0, 0, 14));

            ScrollView scroll = new ScrollView(context);
            LinearLayout info = new LinearLayout(context);
            info.setOrientation(LinearLayout.VERTICAL);
            add(info, context, "Version", safeVersion(pi));
            add(info, context, "Version code", String.valueOf(versionCode(pi)));
            add(info, context, "Package", packageName);
            add(info, context, "UID", String.valueOf(ai.uid));
            add(info, context, "Target SDK", String.valueOf(ai.targetSdkVersion));
            add(info, context, "Min SDK", String.valueOf(minSdk(pi)));
            add(info, context, "Installed", formatTime(pi.firstInstallTime));
            add(info, context, "Last updated", formatTime(pi.lastUpdateTime));
            add(info, context, "APK path", ai.sourceDir);
            add(info, context, "APK size", formatSize(new File(ai.sourceDir).length()));
            add(info, context, "Enabled", String.valueOf(ai.enabled));
            add(info, context, "Debuggable", String.valueOf((ai.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0));
            add(info, context, "System app", String.valueOf((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0));
            add(info, context, "Activities", String.valueOf(pi.activities == null ? 0 : pi.activities.length));
            add(info, context, "Services", String.valueOf(pi.services == null ? 0 : pi.services.length));
            add(info, context, "Receivers", String.valueOf(pi.receivers == null ? 0 : pi.receivers.length));
            add(info, context, "Providers", String.valueOf(pi.providers == null ? 0 : pi.providers.length));
            add(info, context, "Requested permissions", String.valueOf(pi.requestedPermissions == null ? 0 : pi.requestedPermissions.length));
            scroll.addView(info);
            root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

            TextView share = text(context, "SHARE APP INFO", 13, Color.WHITE, Typeface.BOLD);
            share.setGravity(Gravity.CENTER);
            share.setBackground(round(context, Color.rgb(105,145,255), 16));
            share.setPadding(0, dp(context, 14), 0, dp(context, 14));
            root.addView(share, margin(context, -1, 52, 0, 0, 10));

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(dp(context, 330), dp(context, 560), Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, android.graphics.PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.CENTER;
            root.setOnTouchListener((v,e) -> { if (e.getActionMasked() == android.view.MotionEvent.ACTION_OUTSIDE) { try { windowManager.removeView(root); } catch (Exception ignored) {} return true; } return false; });
            close.setOnClickListener(v -> { try { windowManager.removeView(root); } catch (Exception ignored) {} });
            share.setOnClickListener(v -> share(context, buildShareText(pm, pi, ai, packageName)));
            windowManager.addView(root, lp);
        } catch (Exception e) {
            Toast.makeText(context, "Unable to read app information", Toast.LENGTH_SHORT).show();
        }
    }

    private static String buildShareText(PackageManager pm, PackageInfo pi, ApplicationInfo ai, String pkg) {
        StringBuilder s = new StringBuilder();
        s.append("App Detail Info\n\n");
        s.append("Name: ").append(pm.getApplicationLabel(ai)).append('\n');
        s.append("Package: ").append(pkg).append('\n');
        s.append("Version: ").append(safeVersion(pi)).append('\n');
        s.append("Version code: ").append(versionCode(pi)).append('\n');
        s.append("UID: ").append(ai.uid).append('\n');
        s.append("Target SDK: ").append(ai.targetSdkVersion).append('\n');
        s.append("Min SDK: ").append(minSdk(pi)).append('\n');
        s.append("Installed: ").append(formatTime(pi.firstInstallTime)).append('\n');
        s.append("Last updated: ").append(formatTime(pi.lastUpdateTime)).append('\n');
        s.append("APK path: ").append(ai.sourceDir).append('\n');
        s.append("APK size: ").append(formatSize(new File(ai.sourceDir).length())).append('\n');
        s.append("Enabled: ").append(ai.enabled).append('\n');
        s.append("Debuggable: ").append((ai.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0).append('\n');
        s.append("System app: ").append((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0).append('\n');
        return s.toString();
    }
    private static void share(Context c, String value) { Intent i = new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_SUBJECT, "App Detail Info"); i.putExtra(Intent.EXTRA_TEXT, value); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); c.startActivity(Intent.createChooser(i, "Share app information")); }
    private static void add(LinearLayout p, Context c, String key, String value) { LinearLayout row = new LinearLayout(c); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(dp(c, 12), dp(c, 9), dp(c, 12), dp(c, 9)); row.setBackground(round(c, Color.rgb(28,35,52), 12)); TextView k=text(c,key,11,Color.rgb(165,174,194),Typeface.BOLD);TextView v=text(c,value,13,Color.WHITE,Typeface.NORMAL);v.setTextIsSelectable(true);v.setPadding(0,dp(c,3),0,0);row.addView(k);row.addView(v);p.addView(row,margin(c,-1,-2,0,0,6)); }
    private static TextView text(Context c,String s,float z,int col,int style){TextView v=new TextView(c);v.setText(s);v.setTextSize(z);v.setTextColor(col);v.setTypeface(Typeface.DEFAULT,style);return v;}
    private static GradientDrawable round(Context c,int col,int r){GradientDrawable d=new GradientDrawable();d.setColor(col);d.setCornerRadius(dp(c,r));return d;}
    private static LinearLayout.LayoutParams margin(Context c,int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(dp(c,l),dp(c,t),dp(c,r),dp(c,b));return p;}
    private static LinearLayout.LayoutParams margin(Context c,int w,int h,int l,int t,int r){return margin(c,w,h,l,t,r,0);}
    private static int dp(Context c,int v){return(int)(v*c.getResources().getDisplayMetrics().density+.5f);}
    private static String safeVersion(PackageInfo p){return p.versionName==null?"Unknown":p.versionName;}
    private static long versionCode(PackageInfo p){return Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode;}
    private static int minSdk(PackageInfo p){return Build.VERSION.SDK_INT>=24?p.applicationInfo.minSdkVersion:0;}
    private static String formatTime(long t){return DateFormat.getDateTimeInstance().format(new Date(t));}
    private static String formatSize(long bytes){if(bytes<1024)return bytes+" B";if(bytes<1024*1024)return String.format("%.1f KB",bytes/1024f);return String.format("%.1f MB",bytes/(1024f*1024f));}
}
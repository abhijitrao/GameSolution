package com.abhijit.gamesolution;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
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
    private static final int BG = Color.rgb(16,21,34), CARD = Color.rgb(28,35,52), WHITE = Color.WHITE;
    private static final int MUTED = Color.rgb(165,174,194), PRIMARY = Color.rgb(105,145,255);
    private AppDetailInfoDialog() {}

    public static void show(Context c, String pkg, WindowManager wm, View current, Runnable dismiss) {
        if (current != null && dismiss != null) dismiss.run();
        try {
            PackageManager pm = c.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS | PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS | PackageManager.GET_PROVIDERS);
            ApplicationInfo ai = pi.applicationInfo;

            LinearLayout root = new LinearLayout(c);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(c,18), dp(c,14), dp(c,18), dp(c,14));
            root.setBackground(round(c, BG, 22));

            LinearLayout header = new LinearLayout(c);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.addView(text(c, "App Detail Info", 20, WHITE, Typeface.BOLD), new LinearLayout.LayoutParams(0,-2,1));
            TextView close = text(c, "×", 28, MUTED, Typeface.BOLD);
            close.setGravity(Gravity.CENTER);
            header.addView(close, new LinearLayout.LayoutParams(dp(c,42),dp(c,42)));
            root.addView(header);

            TextView name = text(c, pm.getApplicationLabel(ai).toString(), 17, WHITE, Typeface.BOLD);
            root.addView(name, margin(c,-1,-2,0,2,0,0));
            TextView packageText = text(c, pkg, 11, MUTED, Typeface.NORMAL);
            packageText.setSingleLine(true);
            packageText.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            root.addView(packageText, margin(c,-1,-2,0,0,0,12));

            ScrollView scroll = new ScrollView(c);
            LinearLayout content = new LinearLayout(c);
            content.setOrientation(LinearLayout.VERTICAL);

            addTwoColumnRow(content,c,"Version",safeVersion(pi),"Version code",String.valueOf(versionCode(pi)));
            addTwoColumnRow(content,c,"Target SDK",String.valueOf(ai.targetSdkVersion),"Min SDK",String.valueOf(minSdk(pi)));
            addTwoColumnRow(content,c,"Activities",count(pi.activities),"Services",count(pi.services));
            addTwoColumnRow(content,c,"Receivers",count(pi.receivers),"Providers",count(pi.providers));
            add(content,c,"UID",String.valueOf(ai.uid));
            add(content,c,"Installed",formatTime(pi.firstInstallTime));
            add(content,c,"Last updated",formatTime(pi.lastUpdateTime));
            add(content,c,"APK size",formatSize(new File(ai.sourceDir).length()));
            add(content,c,"APK path",ai.sourceDir);
            add(content,c,"Status",(ai.enabled ? "Enabled" : "Disabled") + " • " + ((ai.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0 ? "Debuggable" : "Release") + " • " + ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0 ? "System app" : "User app"));

            int permissionCount = pi.requestedPermissions == null ? 0 : pi.requestedPermissions.length;
            LinearLayout permissionSection = expandableSection(c,"Permissions",permissionCount);
            LinearLayout permissionItems = new LinearLayout(c);
            permissionItems.setOrientation(LinearLayout.VERTICAL);
            permissionItems.setVisibility(View.GONE);
            if (pi.requestedPermissions != null) {
                for (String permission : pi.requestedPermissions) addExpandableItem(permissionItems,c,permission);
            }
            permissionSection.addView(permissionItems,new LinearLayout.LayoutParams(-1,-2));
            TextView permissionHeader = (TextView) permissionSection.getChildAt(0);
            permissionHeader.setOnClickListener(v -> toggleSection(permissionHeader,permissionItems,"Permissions",permissionCount));
            content.addView(permissionSection,margin(c,-1,-2,0,8,0,8));

            int componentCount = (pi.activities == null ? 0 : pi.activities.length) + (pi.services == null ? 0 : pi.services.length) + (pi.receivers == null ? 0 : pi.receivers.length) + (pi.providers == null ? 0 : pi.providers.length);
            LinearLayout componentSection = expandableSection(c,"Components",componentCount);
            LinearLayout componentItems = new LinearLayout(c);
            componentItems.setOrientation(LinearLayout.VERTICAL);
            componentItems.setVisibility(View.GONE);
            if (pi.activities != null) for (android.content.pm.ActivityInfo x : pi.activities) addExpandableItem(componentItems,c,"Activity  •  " + x.name);
            if (pi.services != null) for (android.content.pm.ServiceInfo x : pi.services) addExpandableItem(componentItems,c,"Service  •  " + x.name);
            if (pi.receivers != null) for (android.content.pm.ActivityInfo x : pi.receivers) addExpandableItem(componentItems,c,"Receiver  •  " + x.name);
            if (pi.providers != null) for (android.content.pm.ProviderInfo x : pi.providers) addExpandableItem(componentItems,c,"Provider  •  " + x.name);
            componentSection.addView(componentItems,new LinearLayout.LayoutParams(-1,-2));
            TextView componentHeader = (TextView) componentSection.getChildAt(0);
            componentHeader.setOnClickListener(v -> toggleSection(componentHeader,componentItems,"Components",componentCount));
            content.addView(componentSection);

            scroll.addView(content);
            root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

            TextView share = text(c,"SHARE APP INFO",13,WHITE,Typeface.BOLD);
            share.setGravity(Gravity.CENTER);
            share.setBackground(round(c,PRIMARY,16));
            share.setPadding(0,dp(c,13),0,dp(c,13));
            root.addView(share,margin(c,-1,50,0,10,0,0));

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(dp(c,340),dp(c,590),Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,android.graphics.PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.CENTER;
            root.setOnTouchListener((v,e) -> { if(e.getActionMasked()==MotionEvent.ACTION_OUTSIDE){remove(wm,root);return true;} return false; });
            close.setOnClickListener(v -> remove(wm,root));
            share.setOnClickListener(v -> share(c,buildShareText(pm,pi,ai,pkg)));
            wm.addView(root,lp);
        } catch(Exception e) { Toast.makeText(c,"Unable to read app information",Toast.LENGTH_SHORT).show(); }
    }

    private static void addTwoColumnRow(LinearLayout parent,Context c,String k1,String v1,String k2,String v2){
        LinearLayout row=new LinearLayout(c);row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(card(c,k1,v1),weightParams(c,1,0,0,6));
        row.addView(card(c,k2,v2),weightParams(c,1,6,0,0));
        parent.addView(row,margin(c,-1,-2,0,0,0,6));
    }
    private static LinearLayout.LayoutParams weightParams(Context c,float weight,int l,int t,int r){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,weight);p.setMargins(dp(c,l),dp(c,t),dp(c,r),0);return p;}
    private static LinearLayout card(Context c,String k,String v){LinearLayout x=new LinearLayout(c);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(c,12),dp(c,9),dp(c,8),dp(c,9));x.setBackground(round(c,CARD,12));x.addView(text(c,k,10,MUTED,Typeface.BOLD));x.addView(text(c,v,13,WHITE,Typeface.BOLD),margin(c,-1,-2,0,3,0,0));return x;}
    private static void add(LinearLayout p,Context c,String k,String v){p.addView(card(c,k,v),margin(c,-1,-2,0,0,0,6));}
    private static LinearLayout expandableSection(Context c,String title,int count){LinearLayout section=new LinearLayout(c);section.setOrientation(LinearLayout.VERTICAL);TextView header=text(c,"⌄  "+title+"  •  "+count,13,WHITE,Typeface.BOLD);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(c,12),0,dp(c,12),0);header.setBackground(round(c,CARD,14));section.addView(header,new LinearLayout.LayoutParams(-1,dp(c,48)));return section;}
    private static void toggleSection(TextView header,LinearLayout content,String title,int count){boolean open=content.getVisibility()!=View.VISIBLE;content.setVisibility(open?View.VISIBLE:View.GONE);header.setText((open?"⌃  ":"⌄  ")+title+"  •  "+count);}
    private static void addExpandableItem(LinearLayout p,Context c,String value){TextView item=text(c,"•  "+value,11,WHITE,Typeface.NORMAL);item.setPadding(dp(c,12),dp(c,7),dp(c,8),dp(c,7));item.setTextIsSelectable(true);p.addView(item);}
    private static void remove(WindowManager wm,View v){try{wm.removeView(v);}catch(Exception ignored){}}
    private static TextView text(Context c,String s,float size,int color,int style){TextView v=new TextView(c);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setTypeface(Typeface.DEFAULT,style);return v;}
    private static GradientDrawable round(Context c,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
    private static LinearLayout.LayoutParams margin(Context c,int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(dp(c,l),dp(c,t),dp(c,r),dp(c,b));return p;}
    private static int dp(Context c,int value){return (int)(value*c.getResources().getDisplayMetrics().density+.5f);}
    private static String count(Object[] array){return String.valueOf(array==null?0:array.length);}
    private static String buildShareText(PackageManager pm,PackageInfo pi,ApplicationInfo ai,String pkg){StringBuilder s=new StringBuilder("App Detail Info\n\n");s.append("Name: ").append(pm.getApplicationLabel(ai)).append('\n').append("Package: ").append(pkg).append('\n').append("Version: ").append(safeVersion(pi)).append('\n').append("Version code: ").append(versionCode(pi)).append('\n').append("UID: ").append(ai.uid).append('\n').append("Target SDK: ").append(ai.targetSdkVersion).append('\n').append("Min SDK: ").append(minSdk(pi)).append('\n').append("Installed: ").append(formatTime(pi.firstInstallTime)).append('\n').append("Last updated: ").append(formatTime(pi.lastUpdateTime)).append('\n').append("APK path: ").append(ai.sourceDir).append('\n').append("APK size: ").append(formatSize(new File(ai.sourceDir).length())).append('\n').append("Enabled: ").append(ai.enabled).append('\n').append("Debuggable: ").append((ai.flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0).append('\n').append("System app: ").append((ai.flags&ApplicationInfo.FLAG_SYSTEM)!=0).append('\n');if(pi.requestedPermissions!=null){s.append("\nPermissions:\n");for(String p:pi.requestedPermissions)s.append("• ").append(p).append('\n');}return s.toString();}
    private static void share(Context c,String value){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_SUBJECT,"App Detail Info");i.putExtra(Intent.EXTRA_TEXT,value);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(Intent.createChooser(i,"Share app information"));}
    private static String safeVersion(PackageInfo p){return p.versionName==null?"Unknown":p.versionName;}private static long versionCode(PackageInfo p){return Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode;}private static int minSdk(PackageInfo p){return Build.VERSION.SDK_INT>=24?p.applicationInfo.minSdkVersion:0;}private static String formatTime(long t){return DateFormat.getDateTimeInstance().format(new Date(t));}private static String formatSize(long b){if(b<1024)return b+" B";if(b<1024*1024)return String.format("%.1f KB",b/1024f);return String.format("%.1f MB",b/(1024f*1024f));}
}
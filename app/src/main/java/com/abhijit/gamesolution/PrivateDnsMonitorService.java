package com.abhijit.gamesolution;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.WindowManager;
import android.view.Gravity;
import android.view.View;
import android.graphics.PixelFormat;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.graphics.drawable.GradientDrawable;
import java.util.HashSet;
import java.util.Set;

public class PrivateDnsMonitorService extends Service {
 private static final String PREFS="private_dns_settings", TRACKED="tracked_packages", CHANNEL="private_dns_monitor";
 private final Handler handler=new Handler(Looper.getMainLooper());
 private String current=""; private long lastPrompt=0; private WindowManager wm; private View prompt;
 private final Runnable poll=new Runnable(){public void run(){try{checkApps();}catch(Exception ignored){} handler.postDelayed(this,1200);}};
 @Override public void onCreate(){super.onCreate();wm=(WindowManager)getSystemService(WINDOW_SERVICE);NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(Build.VERSION.SDK_INT>=26&&nm!=null)nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Private DNS app tracking",NotificationManager.IMPORTANCE_LOW));Notification.Builder n=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);n.setSmallIcon(android.R.drawable.ic_lock_lock).setContentTitle("GameSolution app tracking").setContentText("Monitoring app switches for Private DNS prompts").setOngoing(true);startForeground(4208,n.build());handler.post(poll);}
 @Override public int onStartCommand(Intent i,int f,int id){
  if(i!=null&&"com.abhijit.gamesolution.TRACK_PRIVATE_DNS_PACKAGE".equals(i.getAction())){
   String pkg=i.getStringExtra("package_name");
   if(pkg!=null&&!pkg.isEmpty()&&!pkg.equals(getPackageName())){
    saveTracked(pkg);
    current=pkg;
    if(!dnsEnabled())showPrompt(pkg,true);
   }
  }
  return START_STICKY;
 }
 @Override public void onDestroy(){handler.removeCallbacks(poll);removePrompt();super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
 private void checkApps(){if(Build.VERSION.SDK_INT<28)return;UsageStatsManager u=(UsageStatsManager)getSystemService(USAGE_STATS_SERVICE);if(u==null)return;long now=System.currentTimeMillis();UsageEvents es=u.queryEvents(now-4000,now);if(es==null)return;UsageEvents.Event e=new UsageEvents.Event();String latest=current;long time=0;while(es.hasNextEvent()){es.getNextEvent(e);int t=e.getEventType();boolean fg=t==UsageEvents.Event.MOVE_TO_FOREGROUND;if(Build.VERSION.SDK_INT>=29)fg=fg||t==UsageEvents.Event.ACTIVITY_RESUMED;if(fg&&e.getTimeStamp()>=time&&e.getPackageName()!=null){latest=e.getPackageName();time=e.getTimeStamp();}}if(latest.isEmpty()||latest.equals(current))return;String previous=current;current=latest;if(!previous.isEmpty()&&!previous.equals(getPackageName())&&tracked(previous)&&dnsEnabled())showPrompt(previous,false);if(!current.equals(getPackageName())&&tracked(current)&&!dnsEnabled())showPrompt(current,true);else if(!current.equals(getPackageName())&&dnsEnabled())saveTracked(current);}
 private boolean dnsEnabled(){try{String m=Settings.Global.getString(getContentResolver(),"private_dns_mode");return m!=null&&!m.equalsIgnoreCase("off");}catch(Exception e){return false;}}
 private Set<String> packages(){Set<String>s=getSharedPreferences(PREFS,MODE_PRIVATE).getStringSet(TRACKED,new HashSet<String>());return s==null?new HashSet<String>():new HashSet<String>(s);}
 private boolean tracked(String p){return packages().contains(p);}
 private void saveTracked(String p){if(p==null||p.isEmpty()||p.equals(getPackageName()))return;Set<String>s=packages();s.add(p);getSharedPreferences(PREFS,MODE_PRIVATE).edit().putStringSet(TRACKED,s).apply();}
 private void showPrompt(String pkg,boolean enable){if(prompt!=null||System.currentTimeMillis()-lastPrompt<15000||!Settings.canDrawOverlays(this))return;lastPrompt=System.currentTimeMillis();LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(14),dp(16),dp(12));GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(24,30,43));bg.setCornerRadius(dp(18));box.setBackground(bg);TextView title=new TextView(this);title.setText(enable?"Enable Ad Blocking again?":"Disable Private DNS?");title.setTextColor(Color.WHITE);title.setTextSize(17);box.addView(title);TextView body=new TextView(this);body.setText((enable?"Private DNS is currently disabled. Enable ad blocking for all apps?":"Private DNS is enabled. Disable it for all apps, including other apps?")+"\\n\\nCurrent app package: "+pkg);body.setTextColor(Color.LTGRAY);body.setTextSize(13);box.addView(body);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.END);Button no=new Button(this);no.setText(enable?"Not Now":"Keep Enabled");no.setOnClickListener(v->{if(enable)saveTracked(pkg);removePrompt();});Button yes=new Button(this);yes.setText(enable?"Enable DNS":"Disable");yes.setOnClickListener(v->{removePrompt();openSettings();});row.addView(no);row.addView(yes);box.addView(row);prompt=box;try{WindowManager.LayoutParams p=new WindowManager.LayoutParams(-1,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;p.y=dp(48);wm.addView(prompt,p);}catch(Exception ex){prompt=null;}}
 private void openSettings(){try{Intent i=new Intent(Settings.ACTION_WIRELESS_SETTINGS);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}catch(Exception e){try{Intent i=new Intent(Settings.ACTION_SETTINGS);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}catch(Exception ignored){}}}
 private void removePrompt(){if(prompt!=null&&wm!=null){try{wm.removeView(prompt);}catch(Exception ignored){}prompt=null;}}
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}

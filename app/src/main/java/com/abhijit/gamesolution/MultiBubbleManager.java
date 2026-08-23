package com.abhijit.gamesolution;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

public final class MultiBubbleManager {
    private static final int DELETE_ZONE_SIZE_DP = 76;
    private static final int DELETE_TRIGGER_RADIUS_DP = 72;
    private static final int ACTIVITY_HEIGHT_DP = 42;
    private final Context context;
    private final WindowManager windowManager;
    private final boolean disabledForMainService;
    private View recentBubble, activityBubble, deleteZoneView;
    private Handler trackerHandler;
    private Runnable tracker;
    private boolean recentRemoved, activityRemoved, deleteZoneActive;
    private String lastRecentPackage, lastActivityPackage, lastActivityName;

    public MultiBubbleManager(Context context, WindowManager windowManager) {
        this.disabledForMainService = context instanceof FloatingService;
        this.context = context.getApplicationContext();
        this.windowManager = windowManager;
    }

    public void configure(boolean recentEnabled, boolean activityEnabled) {
        if (disabledForMainService) return;
        if (!recentEnabled) { removeRecentBubble(); recentRemoved = true; } else if (recentBubble == null) recentRemoved = false;
        if (!activityEnabled) { removeActivityBubble(); activityRemoved = true; } else if (activityBubble == null) activityRemoved = false;
        startTracking(recentEnabled, activityEnabled);
    }

    public void startTracking(boolean recentEnabled, boolean activityEnabled) {
        if (disabledForMainService) return;
        stopTracking();
        trackerHandler = new Handler(Looper.getMainLooper());
        tracker = new Runnable() {
            @Override public void run() {
                if (recentEnabled && !recentRemoved) updateRecentApp();
                if (activityEnabled && !activityRemoved) updateCurrentActivity();
                if (trackerHandler != null) trackerHandler.postDelayed(this, 1000L);
            }
        };
        trackerHandler.post(tracker);
    }

    public void stopTracking() { if (disabledForMainService) return; if (trackerHandler != null && tracker != null) trackerHandler.removeCallbacks(tracker); trackerHandler = null; tracker = null; }
    public void removeAllSecondaryBubbles() { if (disabledForMainService) return; stopTracking(); removeRecentBubble(); removeActivityBubble(); hideDeleteZone(); }
    public void removeRecentBubble() { if (disabledForMainService) return; if (recentBubble != null) removeView(recentBubble); recentBubble = null; }
    public void removeActivityBubble() { if (disabledForMainService) return; if (activityBubble != null) removeView(activityBubble); activityBubble = null; }

    private void updateRecentApp() { String pkg=ForegroundAppResolver.getPreviousPackage(context,context.getPackageName()); if(pkg==null||pkg.equals(context.getPackageName()))return; if(pkg.equals(lastRecentPackage)&&recentBubble!=null)return; lastRecentPackage=pkg; showRecentAppInternal(pkg); }
    private void updateCurrentActivity() { ForegroundActivity current=findForegroundActivity(); if(current==null||current.packageName.equals(context.getPackageName()))return; if(current.packageName.equals(lastActivityPackage)&&current.activityName.equals(lastActivityName)&&activityBubble!=null)return; lastActivityPackage=current.packageName; lastActivityName=current.activityName; showActivityInternal(current.packageName,current.activityName); }
    private ForegroundActivity findForegroundActivity(){ UsageStatsManager manager=(UsageStatsManager)context.getSystemService(Context.USAGE_STATS_SERVICE); if(manager==null)return null; long end=System.currentTimeMillis(); UsageEvents events=manager.queryEvents(end-60_000L,end); if(events==null)return null; UsageEvents.Event event=new UsageEvents.Event(); ForegroundActivity result=null; long latest=-1L; while(events.hasNextEvent()){events.getNextEvent(event);if(event.getEventType()!=UsageEvents.Event.ACTIVITY_RESUMED)continue;String pkg=event.getPackageName(),cls=event.getClassName();if(pkg==null||cls==null||pkg.equals(context.getPackageName()))continue;if(event.getTimeStamp()>latest){latest=event.getTimeStamp();result=new ForegroundActivity(pkg,cls);}} return result; }

    public void showRecentApp(String packageName){if(disabledForMainService)return;recentRemoved=false;lastRecentPackage=packageName;showRecentAppInternal(packageName);}
    public void showActivity(String packageName,String activityName){if(disabledForMainService)return;activityRemoved=false;lastActivityPackage=packageName;lastActivityName=activityName;showActivityInternal(packageName,activityName);}
    private void showRecentAppInternal(String packageName){if(packageName==null||packageName.equals(context.getPackageName()))return;if(recentBubble!=null)removeView(recentBubble);ImageView bubble=createIconBubble(packageName);addBubble(bubble,packageName,false,150);recentBubble=bubble;}
    private void showActivityInternal(String packageName,String activityName){if(packageName==null||activityName==null||activityName.isEmpty())return;if(activityBubble!=null)removeView(activityBubble);TextView bubble=createTextBubble(shortActivityName(activityName));addBubble(bubble,packageName,true,230);activityBubble=bubble;}

    private void addBubble(View view,String packageName,boolean displayOnly,int yDp){int sw=context.getResources().getDisplayMetrics().widthPixels;int width=view instanceof TextView?measureActivityWidth((TextView)view,sw):dp(52);int height=view instanceof TextView?dp(ACTIVITY_HEIGHT_DP):dp(52);final WindowManager.LayoutParams params=new WindowManager.LayoutParams(width,height,overlayType(),WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);params.gravity=Gravity.TOP|Gravity.START;params.x=Math.max(0,sw-width-dp(12));params.y=Math.max(dp(48),dp(yDp));view.setOnTouchListener(new View.OnTouchListener(){float downX,downY;int startX,startY;boolean moved,overDelete;@Override public boolean onTouch(View v,MotionEvent e){switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:downX=e.getRawX();downY=e.getRawY();startX=params.x;startY=params.y;moved=false;overDelete=false;return true;case MotionEvent.ACTION_MOVE:float dx=e.getRawX()-downX,dy=e.getRawY()-downY;if(Math.abs(dx)>dp(6)||Math.abs(dy)>dp(6)){if(!moved)showDeleteZone();moved=true;}if(moved){int sw=context.getResources().getDisplayMetrics().widthPixels,sh=context.getResources().getDisplayMetrics().heightPixels;int w=view.getWidth()>0?view.getWidth():params.width;params.x=clamp(startX+(int)dx,0,sw-w);params.y=clamp(startY+(int)dy,dp(48),sh-params.height-dp(8));try{windowManager.updateViewLayout(v,params);}catch(Exception ignored){}overDelete=isOverDeleteZone(params.x,params.y,w,params.height);updateDeleteZoneVisual(overDelete);}return true;case MotionEvent.ACTION_UP:if(moved){hideDeleteZone();if(overDelete){if(v==recentBubble){removeRecentBubble();recentRemoved=true;}else if(v==activityBubble){removeActivityBubble();activityRemoved=true;}}else if(BubbleSettings.isSnapToEdge(context))snapToEdge(v,params);return true;}if(!displayOnly)bringAppToFront(packageName);return true;case MotionEvent.ACTION_CANCEL:hideDeleteZone();return true;default:return true;}}});try{windowManager.addView(view,params);}catch(Exception ignored){}}

    private void showDeleteZone(){if(deleteZoneView!=null)return;TextView zone=new TextView(context);zone.setText("×");zone.setTextColor(Color.WHITE);zone.setTextSize(30);zone.setTypeface(Typeface.DEFAULT,Typeface.BOLD);zone.setGravity(Gravity.CENTER);zone.setBackground(round(Color.rgb(95,31,43),40));zone.setElevation(18f);zone.setAlpha(0f);zone.setScaleX(.65f);zone.setScaleY(.65f);WindowManager.LayoutParams p=new WindowManager.LayoutParams(dp(DELETE_ZONE_SIZE_DP),dp(DELETE_ZONE_SIZE_DP),overlayType(),WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT);p.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;p.y=dp(30);deleteZoneView=zone;windowManager.addView(zone,p);zone.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).setInterpolator(new DecelerateInterpolator()).start();}
    private void updateDeleteZoneVisual(boolean active){if(deleteZoneView==null||active==deleteZoneActive)return;deleteZoneActive=active;TextView z=(TextView)deleteZoneView;if(active){z.setText("🗑");z.setBackground(round(Color.rgb(255,105,120),40));z.animate().scaleX(1.25f).scaleY(1.25f).setDuration(120).start();}else{z.setText("×");z.setBackground(round(Color.rgb(95,31,43),40));z.animate().scaleX(1f).scaleY(1f).setDuration(120).start();}}
    private void hideDeleteZone(){if(deleteZoneView==null)return;View z=deleteZoneView;deleteZoneView=null;deleteZoneActive=false;try{windowManager.removeView(z);}catch(Exception ignored){}}
    private boolean isOverDeleteZone(int x,int y,int width,int height){int sw=context.getResources().getDisplayMetrics().widthPixels,sh=context.getResources().getDisplayMetrics().heightPixels;float cx=sw/2f,cy=sh-dp(30)-dp(DELETE_ZONE_SIZE_DP)/2f;float px=Math.max(x,Math.min(cx,x+width)),py=Math.max(y,Math.min(cy,y+height));float dx=cx-px,dy=cy-py;return dx*dx+dy*dy<=(float)dp(DELETE_TRIGGER_RADIUS_DP)*dp(DELETE_TRIGGER_RADIUS_DP);}
    private int measureActivityWidth(TextView view,int screenWidth){int max=Math.max(dp(72),screenWidth-dp(24));view.measure(View.MeasureSpec.makeMeasureSpec(max,View.MeasureSpec.AT_MOST),View.MeasureSpec.makeMeasureSpec(dp(ACTIVITY_HEIGHT_DP),View.MeasureSpec.EXACTLY));return Math.min(max,Math.max(dp(72),view.getMeasuredWidth()+dp(18)));}
    private void snapToEdge(View view,WindowManager.LayoutParams p){int sw=context.getResources().getDisplayMetrics().widthPixels,w=view.getWidth()>0?view.getWidth():p.width,target=p.x+w/2<sw/2?0:Math.max(0,sw-w),from=p.x;android.animation.ValueAnimator a=android.animation.ValueAnimator.ofInt(from,target);a.setDuration(220).setInterpolator(new DecelerateInterpolator());a.addUpdateListener(v->{p.x=(Integer)v.getAnimatedValue();try{windowManager.updateViewLayout(view,p);}catch(Exception ignored){}});a.start();}
    private ImageView createIconBubble(String pkg){ImageView v=new ImageView(context);try{ApplicationInfo i=context.getPackageManager().getApplicationInfo(pkg,0);Drawable d=context.getPackageManager().getApplicationIcon(i);v.setImageDrawable(d);}catch(Exception ignored){}v.setPadding(dp(7),dp(7),dp(7),dp(7));v.setBackground(round(Color.rgb(72,91,205),22));v.setElevation(14f);return v;}
    private TextView createTextBubble(String label){TextView v=new TextView(context);v.setText(label);v.setTextColor(Color.WHITE);v.setTextSize(11);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setGravity(Gravity.CENTER);v.setSingleLine(true);v.setPadding(dp(12),0,dp(12),0);v.setBackground(round(Color.rgb(72,91,205),14));v.setElevation(14f);return v;}
    private void removeView(View v){if(v!=null)try{windowManager.removeView(v);}catch(Exception ignored){}}
    private void bringAppToFront(String pkg){try{Intent i=context.getPackageManager().getLaunchIntentForPackage(pkg);if(i==null)return;i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);context.startActivity(i);}catch(Exception ignored){}}
    private String shortActivityName(String a){int dot=a.lastIndexOf('.');return dot>=0&&dot+1<a.length()?a.substring(dot+1):a;}
    private GradientDrawable round(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
    private int overlayType(){return android.os.Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;}
    private int dp(int v){return Math.round(v*context.getResources().getDisplayMetrics().density);}
    private static final class ForegroundActivity{final String packageName,activityName;ForegroundActivity(String p,String a){packageName=p;activityName=a;}}
}

package com.abhijit.gamesolution;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public final class MultiBubbleManager {
    public static final int MAX_ACTIVITY_BUBBLES = 3;
    private final Context context; private final WindowManager windowManager;
    private final List<View> activityBubbles = new ArrayList<>(); private View recentBubble;
    public MultiBubbleManager(Context context, WindowManager windowManager) { this.context=context.getApplicationContext(); this.windowManager=windowManager; }
    public void removeAllSecondaryBubbles(){ removeRecentBubble(); for(View v:new ArrayList<>(activityBubbles))removeView(v); activityBubbles.clear(); }
    public void removeRecentBubble(){ if(recentBubble!=null){removeView(recentBubble);recentBubble=null;} }
    public void showRecentApp(String pkg){ if(pkg==null||pkg.equals(context.getPackageName()))return; removeRecentBubble(); ImageView b=createIconBubble(pkg); addBubble(b,pkg,52,150); recentBubble=b; }
    public void showActivity(String pkg,String activity){ if(pkg==null||activity==null||activity.isEmpty())return; if(activityBubbles.size()>=MAX_ACTIVITY_BUBBLES)removeView(activityBubbles.remove(0)); TextView b=createTextBubble(shortActivityName(activity)); int i=activityBubbles.size(); addBubble(b,pkg,52,230+i*64); activityBubbles.add(b); }
    private void addBubble(View view,String pkg,int xDp,int yDp){
        final WindowManager.LayoutParams p=new WindowManager.LayoutParams(dp(52),dp(52),overlayType(),WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT); p.gravity=Gravity.TOP|Gravity.START;p.x=dp(xDp);p.y=dp(yDp);
        final int sw=context.getResources().getDisplayMetrics().widthPixels, sh=context.getResources().getDisplayMetrics().heightPixels;
        view.setClickable(true); view.setOnTouchListener(new View.OnTouchListener(){float downX,downY;int startX,startY;boolean moved; public boolean onTouch(View v,MotionEvent e){switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:downX=e.getRawX();downY=e.getRawY();startX=p.x;startY=p.y;moved=false;return true;case MotionEvent.ACTION_MOVE:float dx=e.getRawX()-downX,dy=e.getRawY()-downY;if(Math.abs(dx)>dp(6)||Math.abs(dy)>dp(6))moved=true;if(moved){p.x=Math.max(0,Math.min(sw-p.width,startX+(int)dx));p.y=Math.max(dp(48),Math.min(sh-p.height-dp(8),startY+(int)dy));try{windowManager.updateViewLayout(v,p);}catch(Exception ignored){}}return true;case MotionEvent.ACTION_UP:if(!moved)bringAppToFront(pkg);return true;case MotionEvent.ACTION_CANCEL:return true;default:return true;}}});
        try{windowManager.addView(view,p);}catch(Exception ignored){}
    }
    private ImageView createIconBubble(String pkg){ImageView v=new ImageView(context);try{ApplicationInfo i=context.getPackageManager().getApplicationInfo(pkg,0);Drawable d=context.getPackageManager().getApplicationIcon(i);v.setImageDrawable(d);}catch(Exception ignored){}v.setPadding(dp(7),dp(7),dp(7),dp(7));v.setBackground(round(Color.rgb(72,91,205),26));v.setElevation(14f);return v;}
    private TextView createTextBubble(String label){TextView v=new TextView(context);v.setText(label);v.setTextColor(Color.WHITE);v.setTextSize(10);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setGravity(Gravity.CENTER);v.setPadding(dp(4),dp(4),dp(4),dp(4));v.setBackground(round(Color.rgb(72,91,205),26));v.setElevation(14f);return v;}
    private void removeView(View v){try{windowManager.removeView(v);}catch(Exception ignored){}}
    private void bringAppToFront(String pkg){try{Intent i=context.getPackageManager().getLaunchIntentForPackage(pkg);if(i==null)return;i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);context.startActivity(i);}catch(Exception ignored){}}
    private String shortActivityName(String a){int p=a.lastIndexOf('.');return p>=0&&p+1<a.length()?a.substring(p+1):a;}
    private GradientDrawable round(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private int overlayType(){return android.os.Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;}
    private int dp(int v){return Math.round(v*context.getResources().getDisplayMetrics().density);}
}

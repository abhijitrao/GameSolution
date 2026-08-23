from pathlib import Path
import re
import subprocess

GOOD = "8afaa7704b9908063a2458772621703216fcc61d"
ROOT = Path("app/src/main/java/com/abhijit/gamesolution")
p = ROOT / "FloatingService.java"

subprocess.check_call(["git", "fetch", "--no-tags", "origin", GOOD])
s = subprocess.check_output(["git", "show", f"{GOOD}:app/src/main/java/com/abhijit/gamesolution/FloatingService.java"], text=True)

s = s.replace("row.setPadding(dp(8),0,dp(8),0);", "row.setPadding(0,0,0,0);")
s = s.replace("d.setBounds(0,0,dp(32),dp(32));", "d.setBounds(0,0,dp(54),dp(54));")
s = s.replace("d.setBounds(0,0,dp(48),dp(48));", "d.setBounds(0,0,dp(54),dp(54));")
s = s.replace("if(++count>=5)break;", "if(++count>=4)break;")
s = s.replace("i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);", "i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);")
s = s.replace("BubbleSettings.isSemiIconVisible(this)", "false")
s = s.replace('"OVAL"', '"SQUARE"')
s = s.replace('TextView oval=label("SQUARE"', 'TextView square=label("SQUARE"')
s = s.replace('oval.setGravity(', 'square.setGravity(')
s = s.replace('oval.setBackground(', 'square.setBackground(')
s = s.replace('oval.setOnClickListener(', 'square.setOnClickListener(')
s = s.replace('shapeRow.addView(oval,', 'shapeRow.addView(square,')

old_size = '''  int widthDp=BubbleSettings.getShape(this).equals("SQUARE")?BubbleSettings.getWidth(this):BubbleSettings.getSize(this);\n  int heightDp=BubbleSettings.getShape(this).equals("SQUARE")?BubbleSettings.getHeight(this):BubbleSettings.getSize(this);'''
new_size = '''  boolean squareMode="SQUARE".equals(BubbleSettings.getShape(this));\n  int widthDp=squareMode?BubbleSettings.getWidth(this):BubbleSettings.getSize(this);\n  int heightDp=squareMode?BubbleSettings.getHeight(this):BubbleSettings.getSize(this);'''
if old_size not in s: raise SystemExit("bubble size marker not found")
s = s.replace(old_size, new_size, 1)
s = s.replace('bubble.setText("i");', 'bubble.setText(squareMode?"":"i");', 1)
s = s.replace('bg.setShape(GradientDrawable.OVAL);bg.setStroke', 'bg.setShape(squareMode?GradientDrawable.RECTANGLE:GradientDrawable.OVAL);if(squareMode)bg.setCornerRadius(dp(18));bg.setStroke', 1)

overlap_helper = ''' private boolean isBubbleOverDeleteZone(int x,int y,int width,int height){int sw=getResources().getDisplayMetrics().widthPixels,sh=getResources().getDisplayMetrics().heightPixels;float circleCx=sw/2f;float circleCy=sh-dp(30)-dp(DELETE_ZONE_SIZE_DP)/2f;float radius=dp(DELETE_TRIGGER_RADIUS_DP);float closestX=Math.max(x,Math.min(circleCx,x+width));float closestY=Math.max(y,Math.min(circleCy,y+height));float dx=circleCx-closestX,dy=circleCy-closestY;return dx*dx+dy*dy<=radius*radius;}\n'''
if 'private boolean isBubbleOverDeleteZone' not in s:
    marker = ' private void updateDeleteZoneState('
    if marker not in s: raise SystemExit("delete zone marker not found")
    s = s.replace(marker, overlap_helper + marker, 1)
s = re.sub(r'boolean active=Math\.hypot\(bx-zcx,by-zcy\)<=dp\(DELETE_TRIGGER_RADIUS_DP\);', 'boolean active=isBubbleOverDeleteZone(x,y,dp(size),dp(size));', s, count=1)

helper = ''' private void applyBubbleVisual(){if(!(bubbleView instanceof TextView))return;boolean square="SQUARE".equals(BubbleSettings.getShape(this));TextView bubble=(TextView)bubbleView;bubble.setText(square?"":"i");GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(125,155,255),Color.rgb(72,91,205)});if(square){bg.setShape(GradientDrawable.RECTANGLE);bg.setCornerRadius(dp(18));}else{bg.setShape(GradientDrawable.OVAL);}bg.setStroke(dp(2),Color.argb(80,255,255,255));bubble.setBackground(bg);bubble.setAlpha(BubbleSettings.getTransparency(this)/100f);}\n'''
if 'private void applyBubbleVisual()' not in s:
    marker = ' private void applyBubbleSettings()'
    if marker not in s: raise SystemExit("applyBubbleSettings marker not found")
    s = s.replace(marker, helper + marker, 1)

new_refresh = 'Runnable refresh=()->{boolean sq="SQUARE".equals(BubbleSettings.getShape(this));sb.setVisibility(sq?View.GONE:View.VISIBLE);sv.setVisibility(sq?View.GONE:View.VISIBLE);wv.setVisibility(sq?View.VISIBLE:View.GONE);wb.setVisibility(sq?View.VISIBLE:View.GONE);hv.setVisibility(sq?View.VISIBLE:View.GONE);hb.setVisibility(sq?View.VISIBLE:View.GONE);circle.setBackground(round(sq?Color.rgb(38,46,66):primary,12));square.setBackground(round(sq?primary:Color.rgb(38,46,66),12));};'
s, refresh_count = re.subn(r'Runnable\s+refresh\s*=\s*\(\)\s*->\s*\{.*?\};', new_refresh, s, count=1, flags=re.DOTALL)
if refresh_count != 1: raise SystemExit("settings refresh marker not found")
new_clicks = 'circle.setOnClickListener(v->{BubbleSettings.setShape(this,"CIRCLE");int n=BubbleSettings.getWidth(this);BubbleSettings.setSize(this,n);applyBubbleVisual();applyBubbleSettings();refresh.run();});square.setOnClickListener(v->{BubbleSettings.setShape(this,"SQUARE");BubbleSettings.setWidth(this,BubbleSettings.getSize(this));BubbleSettings.setHeight(this,BubbleSettings.getSize(this));applyBubbleVisual();applyBubbleSettings();refresh.run();});'
s, click_count = re.subn(r'circle\.setOnClickListener\(v->\{.*?\}\);\s*square\.setOnClickListener\(v->\{.*?\}\);', new_clicks, s, count=1, flags=re.DOTALL)
if click_count != 1: raise SystemExit("settings click marker not found")
transparency_listener = 'ab.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){int n=20+p;av.setText("Transparency: "+n+"%");BubbleSettings.setTransparency(FloatingService.this,n);applyBubbleVisual();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});'
s, ab_count = re.subn(r'ab\.setOnSeekBarChangeListener\(new SeekBar\.OnSeekBarChangeListener\(\)\{.*?\}\);', transparency_listener, s, count=1, flags=re.DOTALL)
if ab_count != 1: raise SystemExit("transparency listener marker not found")
s = s.replace('bubbleView=bubble;windowManager.addView(bubbleView,params);', 'BubbleSettings.setBubbleEnabled(this,true);bubbleView=bubble;windowManager.addView(bubbleView,params);', 1)
s = s.replace('private void animateDeleteAndStop(View bubble){hideDeleteZone();', 'private void animateDeleteAndStop(View bubble){BubbleSettings.setBubbleEnabled(this,false);hideDeleteZone();', 1)
s = s.replace('boolean ovalMode=BubbleSettings.getShape(this).equals("SQUARE");', 'boolean squareMode="SQUARE".equals(BubbleSettings.getShape(this));', 1)
s = s.replace('int widthDp=ovalMode?BubbleSettings.getWidth(this):BubbleSettings.getSize(this);int heightDp=ovalMode?BubbleSettings.getHeight(this):BubbleSettings.getSize(this);', 'int widthDp=squareMode?BubbleSettings.getWidth(this):BubbleSettings.getSize(this);int heightDp=squareMode?BubbleSettings.getHeight(this):BubbleSettings.getSize(this);', 1)

# Secondary bubbles are owned by the service but rendered independently of the main bubble.
secondary = '''\nmb = new MultiBubbleManager(this, windowManager);\nif (BubbleSettings.isRecentAppBubbleEnabled(this)) { String rp=ForegroundAppResolver.getPreviousPackage(this,getPackageName()); if(rp!=null) mb.showRecentApp(rp); }\nLiveMonitorDialog.setActivitySwitchCallback((pkg,activity)->{ if(BubbleSettings.isActivitySwitchBubbleEnabled(this) && mb!=null) mb.showActivity(pkg,activity); });\n'''
s = s.replace('windowManager=(WindowManager)getSystemService(WINDOW_SERVICE);', 'windowManager=(WindowManager)getSystemService(WINDOW_SERVICE);' + secondary, 1)
s = s.replace('private WindowManager windowManager;private View bubbleView', 'private WindowManager windowManager;private MultiBubbleManager mb;private View bubbleView', 1)
# Main-bubble clicks must not recreate or replace the independent recent-app bubble.
s = s.replace('private void openMenu(int bx,int by,int size){if(BubbleSettings.isRecentAppBubbleEnabled(this)&&mb!=null){String rp=ForegroundAppResolver.getPreviousPackage(this,getPackageName());if(rp!=null)mb.showRecentApp(rp);}else if(mb!=null)mb.removeRecentBubble();removeMenu();', 'private void openMenu(int bx,int by,int size){removeMenu();', 1)
s = s.replace('@Override public IBinder onBind(Intent intent){return null;}', '@Override public IBinder onBind(Intent intent){return null;}\n @Override public void onDestroy(){if(mb!=null)mb.removeAllSecondaryBubbles();LiveMonitorDialog.setActivitySwitchCallback(null);super.onDestroy();}', 1)

sp = ROOT / "BubbleSettings.java"
bt = sp.read_text(encoding="utf-8")
bt = bt.replace('"OVAL".equals(shape) ? "OVAL" : "CIRCLE"', '"SQUARE".equals(shape) ? "SQUARE" : "CIRCLE"')
sp.write_text(bt, encoding="utf-8")

# Add the Activity Switch callback/button to Live Monitor.
lp = ROOT / "LiveMonitorDialog.java"
ls = lp.read_text(encoding="utf-8")
if 'ActivitySwitchCallback' not in ls:
    ls = ls.replace('public final class LiveMonitorDialog {', 'public final class LiveMonitorDialog {\n    public interface ActivitySwitchCallback { void onSwitch(String packageName, String activityName); }\n    private static ActivitySwitchCallback activitySwitchCallback;\n    public static void setActivitySwitchCallback(ActivitySwitchCallback callback) { activitySwitchCallback = callback; }', 1)
if 'SWITCH BUBBLE' not in ls:
    old = 'root.addView(footer);'
    new = '''root.addView(footer);\n            TextView switchBubble = text(context, "SWITCH BUBBLE", 12, TEXT, Typeface.BOLD);\n            switchBubble.setGravity(Gravity.CENTER);\n            switchBubble.setBackground(round(context, PRIMARY, 14));\n            switchBubble.setPadding(0, dp(context, 10), 0, dp(context, 10));\n            switchBubble.setOnClickListener(v -> {\n                if (activitySwitchCallback != null && lastActivity != null && !"Not available".equals(lastActivity)) {\n                    activitySwitchCallback.onSwitch(packageName, lastActivity);\n                    switchBubble.setText("BUBBLE ADDED");\n                }\n            });\n            root.addView(switchBubble, margin(context, -1, dp(context, 44), 0, dp(context, 8), 0, 0));'''
    if old not in ls: raise SystemExit("Live Monitor footer marker not found")
    ls = ls.replace(old, new, 1)

activity_marker = '''            lastActivity = snapshot.activity;\n            lastActivityTimestamp = snapshot.timestamp;'''
activity_replacement = '''            boolean activityChanged = !snapshot.activity.equals(lastActivity);\n            lastActivity = snapshot.activity;\n            lastActivityTimestamp = snapshot.timestamp;\n            if (activityChanged && activitySwitchCallback != null) {\n                activitySwitchCallback.onSwitch(pkg, snapshot.activity);\n            }'''
if activity_marker in ls and 'boolean activityChanged = !snapshot.activity.equals(lastActivity);' not in ls:
    ls = ls.replace(activity_marker, activity_replacement, 1)

lp.write_text(ls, encoding="utf-8")
p.write_text(s, encoding="utf-8")
print("Prepared bubble with stable secondary switch bubbles and automatic activity tracking")

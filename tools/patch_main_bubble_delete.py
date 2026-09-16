from pathlib import Path

ROOT = Path("app/src/main/java/com/abhijit/gamesolution")
SERVICE = ROOT / "FloatingService.java"
s = SERVICE.read_text(encoding="utf-8")

# The overlay is positioned in full-display coordinates, while DisplayMetrics
# can report an application/compatibility height on some devices. Use the real
# physical display bounds for both dragging and delete-zone placement.
if "private void getRealScreenSize(int[] out)" not in s:
    marker = " private int clampBubbleX("
    if marker not in s:
        raise SystemExit("main bubble clamp marker not found")
    helper = " private void getRealScreenSize(int[] out){android.util.DisplayMetrics dm=new android.util.DisplayMetrics();if(windowManager!=null&&Build.VERSION.SDK_INT>=17){windowManager.getDefaultDisplay().getRealMetrics(dm);}else{dm=getResources().getDisplayMetrics();}out[0]=dm.widthPixels;out[1]=dm.heightPixels;}\n"
    s = s.replace(marker, helper + marker, 1)

# Use real display bounds when the bubble is created and while it is dragged.
s = s.replace(
    "int sw=getResources().getDisplayMetrics().widthPixels,sh=getResources().getDisplayMetrics().heightPixels;",
    "int[] screen=new int[2];getRealScreenSize(screen);int sw=screen[0],sh=screen[1];",
    2,
)

# The main drag clamp must use the same real height; otherwise the bubble gets
# stuck above the delete zone even though the delete zone is at the bottom.
s = s.replace(
    "params.y=clamp((int)(startY+dy),dp(48),sh-params.height-dp(8));",
    "params.y=clamp((int)(startY+dy),dp(48),Math.max(dp(48),sh-params.height-dp(8)));",
    1,
)

# Make the delete hit test use the real display dimensions too.
if "private boolean isBubbleOverDeleteZone" not in s:
    marker = " private void updateDeleteZoneState("
    if marker not in s:
        raise SystemExit("delete-zone state marker not found")
    helper = " private boolean isBubbleOverDeleteZone(int x,int y,int width,int height){int[] screen=new int[2];getRealScreenSize(screen);int sw=screen[0],sh=screen[1];float cx=sw/2f,cy=sh-dp(30)-dp(DELETE_ZONE_SIZE_DP)/2f,radius=dp(100);float closestX=Math.max(x,Math.min(cx,x+width)),closestY=Math.max(y,Math.min(cy,y+height));float dx=cx-closestX,dy=cy-closestY;return dx*dx+dy*dy<=radius*radius;}\n"
    s = s.replace(marker, helper + marker, 1)

# Replace the old center-point hit test with the forgiving overlap-based test.
old_hit = "boolean active=Math.hypot(bx-zcx,by-zcy)<=dp(DELETE_TRIGGER_RADIUS_DP);"
new_hit = "boolean active=isBubbleOverDeleteZone(x,y,dp(size),dp(size));"
if old_hit in s:
    s = s.replace(old_hit, new_hit, 1)

# Use the final bubble bounds on ACTION_UP instead of the last MOVE state.
old_up = "boolean remove=deleteZoneActive;"
new_up = "int bubbleWidth=bubble.getWidth()>0?bubble.getWidth():params.width;int bubbleHeight=bubble.getHeight()>0?bubble.getHeight():params.height;boolean remove=isBubbleOverDeleteZone(params.x,params.y,bubbleWidth,bubbleHeight);"
if old_up in s:
    s = s.replace(old_up, new_up, 1)

# Keep the hit area generous even if an older generated source still contains
# the original constant.
s = s.replace(
    "DELETE_TRIGGER_RADIUS_DP=72",
    "DELETE_TRIGGER_RADIUS_DP=100",
    1,
)

SERVICE.write_text(s, encoding="utf-8")
print("Main bubble drag now uses real display bounds and robust delete-zone hit testing")

from pathlib import Path

ROOT = Path("app/src/main/java/com/abhijit/gamesolution")
SERVICE = ROOT / "FloatingService.java"
s = SERVICE.read_text(encoding="utf-8")

# Make the delete target forgiving enough for real finger dragging. The visual
# delete zone remains unchanged; only its hit area is expanded.
s = s.replace(
    "DELETE_TRIGGER_RADIUS_DP=72",
    "DELETE_TRIGGER_RADIUS_DP=100",
    1,
)

# Use the actual bubble bounds and recalculate the delete hit on ACTION_UP.
# This avoids relying on the last ACTION_MOVE event when the finger reaches
# the delete zone between two touch samples.
old = "boolean remove=deleteZoneActive;"
new = "int bubbleWidth=bubble.getWidth()>0?bubble.getWidth():params.width;int bubbleHeight=bubble.getHeight()>0?bubble.getHeight():params.height;boolean remove=isBubbleOverDeleteZone(params.x,params.y,bubbleWidth,bubbleHeight);"
if old not in s:
    raise SystemExit("main bubble delete ACTION_UP marker not found")
s = s.replace(old, new, 1)

# Ensure the overlap-based hit test exists and is used during dragging.
if "private boolean isBubbleOverDeleteZone" not in s:
    marker = " private void updateDeleteZoneState("
    if marker not in s:
        raise SystemExit("delete-zone state marker not found")
    helper = " private boolean isBubbleOverDeleteZone(int x,int y,int width,int height){int sw=getResources().getDisplayMetrics().widthPixels,sh=getResources().getDisplayMetrics().heightPixels;float cx=sw/2f,cy=sh-dp(30)-dp(DELETE_ZONE_SIZE_DP)/2f,radius=dp(100);float closestX=Math.max(x,Math.min(cx,x+width)),closestY=Math.max(y,Math.min(cy,y+height));float dx=cx-closestX,dy=cy-closestY;return dx*dx+dy*dy<=radius*radius;}\n"
    s = s.replace(marker, helper + marker, 1)

SERVICE.write_text(s, encoding="utf-8")
print("Main bubble delete-zone drag fixed")

from pathlib import Path

ROOT = Path("app/src/main/java/com/abhijit/gamesolution")
SERVICE = ROOT / "FloatingService.java"

s = SERVICE.read_text(encoding="utf-8")

# Some Android devices expose a compatibility-scaled DisplayMetrics height to a
# Service. Overlay coordinates, however, use the real physical display bounds.
# This can make the main bubble stop well above the bottom of the visible screen.
helper = ''' private int screenWidth(){android.util.DisplayMetrics dm=new android.util.DisplayMetrics();if(Build.VERSION.SDK_INT>=17)getWindowManager().getDefaultDisplay().getRealMetrics(dm);else dm=getResources().getDisplayMetrics();return dm.widthPixels;}\n private int screenHeight(){android.util.DisplayMetrics dm=new android.util.DisplayMetrics();if(Build.VERSION.SDK_INT>=17)getWindowManager().getDefaultDisplay().getRealMetrics(dm);else dm=getResources().getDisplayMetrics();return dm.heightPixels;}\n'''
if "private int screenWidth()" not in s:
    marker = " private int dp(int v)"
    if marker not in s:
        raise SystemExit("dp helper marker not found")
    s = s.replace(marker, helper + marker, 1)

# Use real display dimensions for main-bubble drag/delete calculations.
s = s.replace(
    "int sw=getResources().getDisplayMetrics().widthPixels,sh=getResources().getDisplayMetrics().heightPixels;",
    "int sw=screenWidth(),sh=screenHeight();"
)

# The main bubble must be able to travel to the bottom physical display edge.
# Keep the status-bar-safe top bound, but do not use compatibility-scaled height.
SERVICE.write_text(s, encoding="utf-8")
print("Main bubble drag now uses real display bounds")

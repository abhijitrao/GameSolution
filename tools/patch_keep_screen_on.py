from pathlib import Path

ROOT = Path("app/src/main/java/com/abhijit/gamesolution")
SERVICE = ROOT / "FloatingService.java"

s = SERVICE.read_text(encoding="utf-8")

if "KeepScreenOnManager" not in s:
    marker = "private WindowManager windowManager;"
    if marker not in s:
        raise SystemExit("FloatingService windowManager marker not found")
    s = s.replace(marker, marker + "private KeepScreenOnManager keepScreenOnManager;", 1)

if "KeepScreenOnManager.start(this);" not in s:
    marker = "super.onCreate();"
    if marker not in s:
        raise SystemExit("FloatingService onCreate marker not found")
    s = s.replace(marker, marker + "KeepScreenOnManager.start(this);", 1)

if "KeepScreenOnManager.stop(this);" not in s:
    marker = "@Override public void onDestroy(){"
    if marker not in s:
        raise SystemExit("FloatingService onDestroy marker not found")
    s = s.replace(marker, marker + "KeepScreenOnManager.stop(this);", 1)


def find_method(source, signature):
    start = source.find(signature)
    if start < 0:
        return None
    brace = source.find("{", start)
    if brace < 0:
        return None
    depth = 0
    for i in range(brace, len(source)):
        ch = source[i]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return start, i + 1
    return None


method = find_method(s, "private LinearLayout buildRecentAppsRow()")
if method is None:
    raise SystemExit("buildRecentAppsRow method not found")

new_method = '''private LinearLayout buildRecentAppsRow(){
  LinearLayout container=new LinearLayout(this);
  container.setOrientation(LinearLayout.VERTICAL);
  container.setPadding(0,0,0,0);

  LinearLayout row=new LinearLayout(this);
  row.setOrientation(LinearLayout.HORIZONTAL);
  row.setGravity(Gravity.CENTER_VERTICAL);
  row.setPadding(0,0,0,0);
  row.setBackground(round(card,15));
  int count=0;
  try{
    android.app.usage.UsageStatsManager usm=(android.app.usage.UsageStatsManager)getSystemService(Context.USAGE_STATS_SERVICE);
    long now=System.currentTimeMillis();
    java.util.List<android.app.usage.UsageStats> stats=usm==null?null:usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY,now-86400000L,now);
    if(stats!=null){
      java.util.Collections.sort(stats,(a,b)->Long.compare(b.getLastTimeUsed(),a.getLastTimeUsed()));
      java.util.HashSet<String> added=new java.util.HashSet<>();
      for(android.app.usage.UsageStats usage:stats){
        String p=usage.getPackageName();
        if(p==null||p.equals(getPackageName())||p.equals(targetPackage)||isLauncherPackage(p)||!added.add(p))continue;
        Intent launch=getPackageManager().getLaunchIntentForPackage(p);
        if(launch==null)continue;
        TextView icon=label("",1,text,Typeface.NORMAL);
        icon.setGravity(Gravity.CENTER);
        try{
          android.graphics.drawable.Drawable d=getPackageManager().getApplicationIcon(p);
          d.setBounds(0,0,dp(54),dp(54));
          icon.setCompoundDrawables(null,d,null,null);
        }catch(Exception ignored){continue;}
        icon.setContentDescription(getAppLabel(p));
        final String pkgName=p;
        icon.setOnClickListener(v->{
          vibrate(20);
          removeMenu();
          try{
            Intent i=getPackageManager().getLaunchIntentForPackage(pkgName);
            if(i!=null){
              i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
              startActivity(i);
            }
          }catch(Exception ignored){}
        });
        row.addView(icon,new LinearLayout.LayoutParams(dp(54),dp(54)));
        if(++count>=4)break;
        TextView spacer=label("",1,text,Typeface.NORMAL);
        row.addView(spacer,new LinearLayout.LayoutParams(0,dp(54),1));
      }
    }
  }catch(SecurityException ignored){}catch(Exception ignored){}
  if(count==0)row.addView(label("No recent apps",12,secondary,Typeface.NORMAL),new LinearLayout.LayoutParams(-1,dp(54)));
  container.addView(row,new LinearLayout.LayoutParams(-1,dp(54)));

  final String initialPackage=targetPackage;
  final TextView keepTitle=label("Keep screen on for current App",14,text,Typeface.BOLD);
  final TextView keepState=label("OFF",12,secondary,Typeface.BOLD);
  keepTitle.setSingleLine(false);
  keepTitle.setGravity(Gravity.CENTER_VERTICAL);
  keepState.setGravity(Gravity.CENTER);
  LinearLayout keepRow=new LinearLayout(this);
  keepRow.setOrientation(LinearLayout.HORIZONTAL);
  keepRow.setGravity(Gravity.CENTER_VERTICAL);
  keepRow.setPadding(dp(14),dp(8),dp(10),dp(8));
  keepRow.setBackground(round(card,15));
  LinearLayout copy=new LinearLayout(this);
  copy.setOrientation(LinearLayout.VERTICAL);
  copy.addView(keepTitle);
  TextView keepApp=label(initialPackage==null?"No current app":getAppLabel(initialPackage),11,secondary,Typeface.NORMAL);
  keepApp.setSingleLine(true);
  keepApp.setEllipsize(android.text.TextUtils.TruncateAt.END);
  copy.addView(keepApp,new LinearLayout.LayoutParams(-1,-2));
  keepRow.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
  keepRow.addView(keepState,new LinearLayout.LayoutParams(dp(58),dp(38)));

  Runnable refreshKeep=()->{
    String current=ForegroundAppResolver.getCurrentPackage(this,getPackageName());
    if(current==null)current=targetPackage;
    boolean enabled=current!=null&&KeepScreenOnManager.isEnabled(this,current);
    keepState.setText(enabled?"ON":"OFF");
    keepState.setTextColor(enabled?Color.rgb(76,205,145):secondary);
    keepApp.setText(current==null?"No current app":getAppLabel(current));
  };
  keepRow.setOnClickListener(v->{
    String current=ForegroundAppResolver.getCurrentPackage(this,getPackageName());
    if(current==null)current=targetPackage;
    if(current==null||current.equals(getPackageName()))return;
    boolean enabled=!KeepScreenOnManager.isEnabled(this,current);
    KeepScreenOnManager.setEnabled(this,current,enabled);
    refreshKeep.run();
    vibrate(18);
  });
  refreshKeep.run();
  container.addView(keepRow,rowLp(-1,dp(62),0,dp(8),0,0));
  return container;
}'''

start, end = method
s = s[:start] + new_method + s[end:]
SERVICE.write_text(s, encoding="utf-8")
print("Keep Screen On option patched into the generated bubble app list")

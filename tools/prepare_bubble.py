from pathlib import Path
import subprocess

GOOD = "8afaa7704b9908063a2458772621703216fcc61d"
ROOT = Path("app/src/main/java/com/abhijit/gamesolution")
p = ROOT / "FloatingService.java"

# Checkout uses shallow history, so explicitly fetch the known-good source commit.
subprocess.check_call(["git", "fetch", "--no-tags", "origin", GOOD])
s = subprocess.check_output(["git", "show", f"{GOOD}:app/src/main/java/com/abhijit/gamesolution/FloatingService.java"], text=True)

s = s.replace("row.setPadding(dp(8),0,dp(8),0);", "row.setPadding(0,0,0,0);")
s = s.replace("d.setBounds(0,0,dp(32),dp(32));", "d.setBounds(0,0,dp(54),dp(54));")
s = s.replace("d.setBounds(0,0,dp(48),dp(48));", "d.setBounds(0,0,dp(54),dp(54));")
s = s.replace("if(++count>=5)break;", "if(++count>=4)break;")
s = s.replace("i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);", "i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);")
s = s.replace("BubbleSettings.isSemiIconVisible(this)", "false")
s = s.replace('"OVAL"', '"SQUARE"')

old_size = '''  int widthDp=BubbleSettings.getShape(this).equals("SQUARE")?BubbleSettings.getWidth(this):BubbleSettings.getSize(this);\n  int heightDp=BubbleSettings.getShape(this).equals("SQUARE")?BubbleSettings.getHeight(this):BubbleSettings.getSize(this);'''
new_size = '''  boolean squareMode="SQUARE".equals(BubbleSettings.getShape(this));\n  int widthDp=squareMode?BubbleSettings.getWidth(this):BubbleSettings.getSize(this);\n  int heightDp=squareMode?BubbleSettings.getHeight(this):BubbleSettings.getSize(this);'''
if old_size not in s:
    raise SystemExit("bubble size marker not found")
s = s.replace(old_size, new_size, 1)
s = s.replace('bubble.setText("i");', 'bubble.setText(squareMode?"":"i");', 1)
s = s.replace('bg.setShape(GradientDrawable.OVAL);bg.setStroke', 'bg.setShape(squareMode?GradientDrawable.RECTANGLE:GradientDrawable.OVAL);if(squareMode)bg.setCornerRadius(dp(16));bg.setStroke', 1)

old_refresh = 'Runnable refresh=()->{boolean ovalMode=BubbleSettings.getShape(this).equals("SQUARE");sb.setVisibility(ovalMode?View.GONE:View.VISIBLE);sv.setVisibility(ovalMode?View.GONE:View.VISIBLE);wv.setVisibility(ovalMode?View.VISIBLE:View.GONE);wb.setVisibility(ovalMode?View.VISIBLE:View.GONE);hv.setVisibility(ovalMode?View.VISIBLE:View.GONE);hb.setVisibility(ovalMode?View.VISIBLE:View.GONE);circle.setBackground(round(ovalMode?Color.rgb(38,46,66):primary,12));oval.setBackground(round(ovalMode?primary:Color.rgb(38,46,66),12));};'
new_refresh = 'Runnable refresh=()->{boolean sq="SQUARE".equals(BubbleSettings.getShape(this));sb.setVisibility(sq?View.GONE:View.VISIBLE);sv.setVisibility(sq?View.GONE:View.VISIBLE);wv.setVisibility(sq?View.VISIBLE:View.GONE);wb.setVisibility(sq?View.VISIBLE:View.GONE);hv.setVisibility(sq?View.VISIBLE:View.GONE);hb.setVisibility(sq?View.VISIBLE:View.GONE);circle.setBackground(round(sq?Color.rgb(38,46,66):primary,12));square.setBackground(round(sq?primary:Color.rgb(38,46,66),12));};'
if old_refresh not in s:
    raise SystemExit("settings refresh marker not found")
s = s.replace(old_refresh, new_refresh, 1)

old_clicks = 'circle.setOnClickListener(v->{BubbleSettings.setShape(this,"CIRCLE");int n=BubbleSettings.getWidth(this);BubbleSettings.setSize(this,n);applyBubbleSettings();refresh.run();});oval.setOnClickListener(v->{BubbleSettings.setShape(this,"SQUARE");BubbleSettings.setWidth(this,BubbleSettings.getSize(this));BubbleSettings.setHeight(this,BubbleSettings.getSize(this));applyBubbleSettings();refresh.run();});'
new_clicks = 'circle.setOnClickListener(v->{BubbleSettings.setShape(this,"CIRCLE");applyBubbleSettings();refresh.run();});square.setOnClickListener(v->{BubbleSettings.setShape(this,"SQUARE");BubbleSettings.setWidth(this,BubbleSettings.getSize(this));BubbleSettings.setHeight(this,BubbleSettings.getSize(this));applyBubbleSettings();refresh.run();});'
if old_clicks not in s:
    raise SystemExit("settings click marker not found")
s = s.replace(old_clicks, new_clicks, 1)

s = s.replace('boolean ovalMode=BubbleSettings.getShape(this).equals("SQUARE");', 'boolean squareMode="SQUARE".equals(BubbleSettings.getShape(this));', 1)
s = s.replace('int widthDp=ovalMode?BubbleSettings.getWidth(this):BubbleSettings.getSize(this);int heightDp=ovalMode?BubbleSettings.getHeight(this):BubbleSettings.getSize(this);', 'int widthDp=squareMode?BubbleSettings.getWidth(this):BubbleSettings.getSize(this);int heightDp=squareMode?BubbleSettings.getHeight(this):BubbleSettings.getSize(this);', 1)

sp = ROOT / "BubbleSettings.java"
bt = sp.read_text(encoding="utf-8")
bt = bt.replace('"OVAL".equals(shape) ? "OVAL" : "CIRCLE"', '"SQUARE".equals(shape) ? "SQUARE" : "CIRCLE"')
sp.write_text(bt, encoding="utf-8")
p.write_text(s, encoding="utf-8")
print("Prepared Circle/Square bubble implementation")

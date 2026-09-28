package com.example.electrodecutcompare;
import android.app.*;import android.content.*;import android.graphics.*;import android.graphics.drawable.*;import android.view.*;import android.widget.*;
final class ReviewUi {
 static final int BG=0xff050f1d,CARD=0xff102439,CYAN=0xff4de6ff,WHITE=0xfff2f8ff,MUTED=0xffc5d6e9,WARN=0xffffd447,RED=0xffff657c;
 static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
 static GradientDrawable bg(int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(18);g.setStroke(2,0xff326183);return g;}
 static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(c,12),dp(c,10),dp(c,12),dp(c,10));return l;}
 static TextView text(Context c,String s,int size,int color){TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(c,5),0,dp(c,5));t.setLineSpacing(dp(c,3),1);return t;}
 static TextView title(Context c,String s){TextView t=text(c,s,21,CYAN);t.setTypeface(null,Typeface.BOLD);return t;}
 static Button button(Context c,String s){Button b=new Button(c);b.setAllCaps(false);b.setText(s);b.setTextSize(17);b.setTextColor(WHITE);b.setBackground(bg(0xff125b87));b.setMinHeight(dp(c,54));b.setPadding(dp(c,9),dp(c,6),dp(c,9),dp(c,6));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(c,6),0,dp(c,6));b.setLayoutParams(p);return b;}
 static LinearLayout card(Context c){LinearLayout l=column(c);l.setBackground(bg(CARD));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(c,7),0,dp(c,7));l.setLayoutParams(p);return l;}
 static String f(String ko,String en,String pl,String uk){return ReviewText.of(ko,en,pl,uk);}
 static void error(Context c,String s){new AlertDialog.Builder(c).setTitle(f("확인 필요","Check required","Wymagana kontrola","Потрібна перевірка")).setMessage(s).setPositiveButton("OK",null).show();}
}

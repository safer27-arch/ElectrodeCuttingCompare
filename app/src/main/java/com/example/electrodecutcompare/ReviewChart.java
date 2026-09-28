package com.example.electrodecutcompare;
import android.content.Context;import android.graphics.*;import android.view.View;
/** Native, scalable charts: no embedded small-text screenshot. */
public final class ReviewChart extends View {
 final MotionCore.Comparison data;final boolean times;
 public ReviewChart(Context c,MotionCore.Comparison d,boolean times){super(c);data=d;this.times=times;setContentDescription(times?"Cycle time trend A and B":"Cycle trajectory overlay A and B");}
 @Override protected void onDraw(Canvas c){super.onDraw(c);draw(c,new RectF(0,0,getWidth(),getHeight()),data,times);}
 public static void draw(Canvas original,RectF area,MotionCore.Comparison d,boolean times){
  int save=original.save();original.clipRect(area);original.translate(area.left,area.top);original.scale(area.width()/1000f,area.height()/520f);Canvas c=original;Paint p=new Paint(3);c.drawColor(ReviewUi.CARD);
  p.setTextSize(30);p.setColor(ReviewUi.WHITE);p.setTypeface(Typeface.DEFAULT_BOLD);
  c.drawText(times?ReviewUi.f("실제 사이클 시간 · 초","Actual cycle duration · seconds","Rzeczywisty czas cyklu · sekundy","Фактичний час циклу · секунди"):ReviewUi.f("반복 궤적 · 가는 선=각 회차 / 굵은 선=중앙값","Cycle overlay · thin=cycles / bold=median","Trajektorie · cienkie=cykle / grube=mediana","Траєкторії · тонкі=цикли / жирні=медіана"),24,42,p);
  float l=80,r=964,t=100,b=420;p.setColor(0xff36516a);p.setStrokeWidth(2);for(int j=0;j<5;j++){float y=t+(b-t)*j/4;c.drawLine(l,y,r,y,p);}
  int[] colors={0xff45cfff,0xffffbc52};MotionCore.Analysis[] sides={d.a,d.b};
  if(times){double max=.001;int n=1;for(MotionCore.Analysis a:sides)for(MotionCore.Cycle cy:a.cycles){max=Math.max(max,cy.seconds);n=Math.max(n,cy.id);}max*=1.2;
   for(int k=0;k<2;k++){p.setColor(colors[k]);p.setStrokeWidth(4);float lx=0,ly=0;for(MotionCore.Cycle cy:sides[k].cycles){float x=l+(r-l)*(cy.id-1)/Math.max(1,n-1),y=b-(float)(cy.seconds/max)*(b-t);if(lx!=0)c.drawLine(lx,ly,x,y,p);c.drawCircle(x,y,5,p);lx=x;ly=y;}}
   p.setColor(ReviewUi.MUTED);p.setTextSize(24);c.drawText(String.format(java.util.Locale.ROOT,"%.3fs",max),4,t+8,p);c.drawText("0s",14,b,p);c.drawText("Cycle 1 → "+n,l,b+32,p);
  }else{
   double min=0,max=1;for(MotionCore.Analysis a:sides)for(MotionCore.Cycle cy:a.cycles)for(double v:cy.curve)if(Double.isFinite(v)){min=Math.min(min,v);max=Math.max(max,v);}double pad=Math.max(.05,(max-min)*.08);min-=pad;max+=pad;
   for(int k=0;k<2;k++){MotionCore.Analysis a=sides[k];p.setColor(colors[k]);p.setStrokeWidth(1.5f);p.setAlpha(75);for(MotionCore.Cycle cy:a.cycles)curve(c,p,cy.curve,l,t,r,b,min,max);p.setAlpha(255);p.setStrokeWidth(5);if(!a.cycles.isEmpty())curve(c,p,a.medianCurve,l,t,r,b,min,max);}
   p.setColor(ReviewUi.MUTED);p.setTextSize(23);c.drawText(String.format(java.util.Locale.ROOT,"%.1f",max),8,t+15,p);c.drawText(String.format(java.util.Locale.ROOT,"%.1f",min),8,b,p);c.drawText("0%",l,b+30,p);c.drawText("50%",(l+r)/2,b+30,p);c.drawText("100%",r-60,b+30,p);
  }
  p.setTextSize(27);p.setColor(colors[0]);c.drawText("A",90,493,p);p.setColor(colors[1]);c.drawText("B",170,493,p);p.setColor(ReviewUi.MUTED);p.setTextSize(22);c.drawText(times?"PTS · not playback speed":ReviewUi.f("각 영상 스트로크 기준 · 실제 mm 아님","Relative stroke per video · not physical mm","Względny skok filmu · nie fizyczne mm","Відносний хід відео · не фізичні мм"),230,493,p);original.restoreToCount(save);
 }
 private static void curve(Canvas c,Paint p,double[] v,float l,float t,float r,float b,double min,double max){for(int i=1;i<v.length;i++){if(!Double.isFinite(v[i-1])||!Double.isFinite(v[i]))continue;float x1=l+(r-l)*(i-1)/(v.length-1),x2=l+(r-l)*i/(v.length-1);float y1=b-(float)((v[i-1]-min)/(max-min))*(b-t),y2=b-(float)((v[i]-min)/(max-min))*(b-t);c.drawLine(x1,y1,x2,y2,p);}}
}

package com.example.electrodecutcompare;
import android.graphics.*; import java.util.*;
public final class AdvancedMotionAnalyzer{
 private AdvancedMotionAnalyzer(){}
 public static class Result{
  public Bitmap chart; public String summary; public float score; public int cycles; public float maxJerk, timingCv, intermittent;
  Result(Bitmap b,String s,float sc,int c,float j,float cv,float in){chart=b;summary=s;score=sc;cycles=c;maxJerk=j;timingCv=cv;intermittent=in;}
 }
 public static Result analyze(HighSpeedAnalyzer.Result a,HighSpeedAnalyzer.Result b,String profile,float[] history){
  Metrics ma=metrics(a),mb=metrics(b);
  float jerkDiff=ratio(ma.maxJerk,mb.maxJerk), timingDiff=Math.abs(ma.meanPeriod-mb.meanPeriod)/Math.max(.001f,(ma.meanPeriod+mb.meanPeriod)/2f);
  float jitterDiff=ratio(a.jitter,b.jitter), pathDiff=ratio(a.path,b.path);
  float intermittent=Math.max(ma.outlier,mb.outlier);
  float score=Math.min(100,100*(.28f*jerkDiff+.25f*timingDiff+.20f*jitterDiff+.17f*pathDiff+.10f*intermittent));
  String level=score<18?"정상 후보":score<35?"주의 후보":"이상 후보";
  String s=String.format(Locale.getDefault(),
   "v0.6 %s · %s\n자동 Cycle 후보 A %d회 / B %d회\nCycle 간격 A %.0fms / B %.0fms\nTiming 변동(CV) A %.1f%% / B %.1f%%\n최대 Jerk A %.3f / B %.3f\n간헐 이상 후보 %.1f%%\n종합 Motion Risk %.1f / 100\n※ 정상/주의/이상 확정은 해당 Cutter Profile의 Golden 데이터 축적 후 보정합니다.",
   profile,level,ma.cycles,mb.cycles,ma.meanPeriod,mb.meanPeriod,ma.cv*100,mb.cv*100,ma.maxJerk,mb.maxJerk,intermittent*100,score);
  return new Result(draw(ma,mb,history,score,profile),s,score,Math.max(ma.cycles,mb.cycles),Math.max(ma.maxJerk,mb.maxJerk),Math.max(ma.cv,mb.cv),intermittent);
 }
 static class Metrics{int cycles;float meanPeriod,cv,maxJerk,outlier;float[] jerk;}
 private static Metrics metrics(HighSpeedAnalyzer.Result r){
  Metrics z=new Metrics();int n=r.m.length;float[]v=new float[n],acc=new float[n],j=new float[n];float dt=n>1?Math.max(.001f,(r.t[n-1]-r.t[0])/1000f/(n-1)):.033f;
  for(int i=1;i<n;i++)v[i]=(r.m[i]-r.m[i-1])/dt;
  for(int i=2;i<n;i++)acc[i]=(v[i]-v[i-1])/dt;
  float mean=0;for(float x:r.m)mean+=x;mean/=Math.max(1,n);float sd=0;for(float x:r.m)sd+=(x-mean)*(x-mean);sd=(float)Math.sqrt(sd/Math.max(1,n));
  ArrayList<Integer> peaks=new ArrayList<>();float th=mean+.65f*sd;
  for(int i=3;i<n-3;i++)if(r.m[i]>th&&r.m[i]>=r.m[i-1]&&r.m[i]>r.m[i+1]&&(peaks.isEmpty()||i-peaks.get(peaks.size()-1)>3))peaks.add(i);
  z.cycles=Math.max(1,peaks.size());ArrayList<Float> periods=new ArrayList<>();
  for(int i=1;i<peaks.size();i++)periods.add((float)(r.t[peaks.get(i)]-r.t[peaks.get(i-1)]));
  if(periods.isEmpty())z.meanPeriod=r.t.length>0?r.t[r.t.length-1]:0;else{for(float p:periods)z.meanPeriod+=p;z.meanPeriod/=periods.size();}
  float pv=0;for(float p:periods)pv+=(p-z.meanPeriod)*(p-z.meanPeriod);z.cv=periods.size()>1?(float)Math.sqrt(pv/periods.size())/Math.max(1,z.meanPeriod):0;
  int out=0;for(int i=3;i<n;i++){j[i]=(acc[i]-acc[i-1])/dt;z.maxJerk=Math.max(z.maxJerk,Math.abs(j[i]));if(Math.abs(j[i])>0)out++;}
  float jm=0;for(float q:j)jm+=Math.abs(q);jm/=Math.max(1,n);int high=0;for(float q:j)if(Math.abs(q)>jm*3.0f)high++;z.outlier=high/(float)Math.max(1,n);z.jerk=j;return z;
 }
 private static float ratio(float a,float b){return Math.abs(a-b)/Math.max(.001f,(Math.abs(a)+Math.abs(b))/2f);}
 private static Bitmap draw(Metrics a,Metrics b,float[] hist,float score,String profile){
  int w=1200,h=820;Bitmap o=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(o);c.drawColor(Color.WHITE);Paint p=new Paint(1);
  p.setColor(Color.rgb(15,48,88));p.setTextSize(34);p.setFakeBoldText(true);c.drawText("v0.6 Multi-Cycle / Jerk / Preventive Trend",45,55,p);p.setFakeBoldText(false);p.setTextSize(22);p.setColor(Color.DKGRAY);c.drawText(profile+" · Cycle/속도변화/충격/간헐이상 통합 분석",45,92,p);
  int l=70,r=1135,t=150,bot=500;p.setStyle(Paint.Style.STROKE);p.setColor(Color.LTGRAY);c.drawRect(l,t,r,bot,p);
  float m=.001f;for(float q:a.jerk)m=Math.max(m,Math.abs(q));for(float q:b.jerk)m=Math.max(m,Math.abs(q));
  Paint pa=new Paint(1);pa.setStyle(Paint.Style.STROKE);pa.setStrokeWidth(4);pa.setColor(Color.rgb(30,90,210));Paint pb=new Paint(pa);pb.setColor(Color.rgb(220,65,45));
  int n=Math.max(a.jerk.length,b.jerk.length);for(int i=1;i<n;i++){float x1=l+(r-l)*(i-1f)/(n-1),x2=l+(r-l)*i/(n-1);if(i<a.jerk.length)c.drawLine(x1,(t+bot)/2-a.jerk[i-1]/m*150,x2,(t+bot)/2-a.jerk[i]/m*150,pa);if(i<b.jerk.length)c.drawLine(x1,(t+bot)/2-b.jerk[i-1]/m*150,x2,(t+bot)/2-b.jerk[i]/m*150,pb);}
  p.setStyle(Paint.Style.FILL);p.setColor(Color.DKGRAY);p.setTextSize(24);c.drawText(String.format(Locale.getDefault(),"Motion Risk %.1f/100 · A cycles %d · B cycles %d",score,a.cycles,b.cycles),70,555,p);
  c.drawText("Jerk: 파랑=A / 빨강=B",70,590,p);
  if(hist.length>0){p.setTextSize(20);c.drawText("최근 Risk Trend (최대 30회)",70,645,p);Paint tr=new Paint(1);tr.setStyle(Paint.Style.STROKE);tr.setStrokeWidth(4);tr.setColor(Color.rgb(80,80,80));for(int i=1;i<hist.length;i++){float x1=70+1000*(i-1f)/Math.max(1,hist.length-1),x2=70+1000*i/(float)Math.max(1,hist.length-1);c.drawLine(x1,780-hist[i-1]*1.2f,x2,780-hist[i]*1.2f,tr);}}
  return o;
 }
}
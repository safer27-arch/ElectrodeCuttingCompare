package com.example.electrodecutcompare;
import android.content.*;
public final class GoldenBaselineStore{
 private static final String P="golden_v07"; private GoldenBaselineStore(){}
 public static void save(Context c,String pf,AdvancedMotionAnalyzer.Result a,RoiQualityAnalyzer.Metrics r){
  SharedPreferences.Editor e=c.getSharedPreferences(P,0).edit(); String k=pf+"_";
  e.putBoolean(k+"ok",true).putFloat(k+"risk",a==null?0:a.score);
  if(r!=null)e.putBoolean(k+"roi",true).putFloat(k+"angle",r.entryAngleDeg).putFloat(k+"offset",r.nipOffsetPx).putFloat(k+"tip",r.tipJitterPx).putFloat(k+"grip",r.gripperJitterPx).putFloat(k+"q",r.qualityScore);
  e.apply();
 }
 public static Baseline get(Context c,String pf){SharedPreferences s=c.getSharedPreferences(P,0);String k=pf+"_";if(!s.getBoolean(k+"ok",false))return null;Baseline b=new Baseline();b.risk=s.getFloat(k+"risk",0);b.hasRoi=s.getBoolean(k+"roi",false);b.angle=s.getFloat(k+"angle",0);b.offset=s.getFloat(k+"offset",0);b.tip=s.getFloat(k+"tip",0);b.grip=s.getFloat(k+"grip",0);b.q=s.getFloat(k+"q",0);return b;}
 public static class Baseline{public float risk,angle,offset,tip,grip,q;public boolean hasRoi;}
}

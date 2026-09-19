package com.example.electrodecutcompare;
import android.content.*;
public final class CalibrationStore{
 private static final String P="calibration_v07"; private CalibrationStore(){}
 public static void set(Context c,String pf,float knownMm,float measuredPx){if(knownMm>0&&measuredPx>0)c.getSharedPreferences(P,0).edit().putFloat(pf,knownMm/measuredPx).apply();}
 public static float get(Context c,String pf){return c.getSharedPreferences(P,0).getFloat(pf,0);}
}

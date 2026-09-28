package com.example.electrodecutcompare;
import android.content.Context;
import android.content.SharedPreferences;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class MotionRegionStore {
 public static final class Settings {
  /** Center X/Y and half-width/height, normalized to the displayed, rotation-corrected frame. */
  public float[][] boxes=new float[3][4]; public boolean reverse;
  public boolean complete(){for(float[] b:boxes)if(b[2]<.01f||b[3]<.01f)return false;return true;}
 }
 private static String key(String uri,String profile){try{byte[] h=MessageDigest.getInstance("SHA-256").digest((uri+"\n"+profile).getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:h)s.append(String.format(Locale.ROOT,"%02x",b));return s.toString();}catch(Exception e){throw new IllegalStateException(e);}}
 public static Settings get(Context c,String uri,String profile){SharedPreferences p=c.getSharedPreferences("motion_regions_v200",0);String k=key(uri,profile);Settings s=new Settings();for(int i=0;i<3;i++)for(int j=0;j<4;j++)s.boxes[i][j]=p.getFloat(k+"_"+i+"_"+j,0);s.reverse=p.getBoolean(k+"_reverse",false);return s;}
 public static void save(Context c,String uri,String profile,Settings s){SharedPreferences.Editor e=c.getSharedPreferences("motion_regions_v200",0).edit();String k=key(uri,profile);for(int i=0;i<3;i++)for(int j=0;j<4;j++)e.putFloat(k+"_"+i+"_"+j,s.boxes[i][j]);e.putBoolean(k+"_reverse",s.reverse);e.apply();}
}

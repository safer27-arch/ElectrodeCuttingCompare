package com.example.electrodecutcompare;
import android.content.*; import java.util.*;
public final class TrendStore{
 private static final String P="trend_v06"; private TrendStore(){}
 public static void add(Context c,String profile,float score){
  SharedPreferences s=c.getSharedPreferences(P,0); String k="scores_"+profile; String old=s.getString(k,"");
  String v=(old.isEmpty()?"":old+",")+String.format(Locale.US,"%.2f",score); String[] a=v.split(",");
  if(a.length>30){StringBuilder b=new StringBuilder();for(int i=a.length-30;i<a.length;i++){if(b.length()>0)b.append(",");b.append(a[i]);}v=b.toString();}
  s.edit().putString(k,v).apply();
 }
 public static float[] get(Context c,String profile){String v=c.getSharedPreferences(P,0).getString("scores_"+profile,"");if(v.isEmpty())return new float[0];String[]a=v.split(",");float[]o=new float[a.length];for(int i=0;i<a.length;i++)try{o[i]=Float.parseFloat(a[i]);}catch(Exception e){}return o;}
}
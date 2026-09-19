package com.example.electrodecutcompare;
import android.content.*;
public final class AppStateStore{
 private static final String P="app_state_v06";
 private AppStateStore(){}
 public static void put(Context c,String k,String v){c.getSharedPreferences(P,0).edit().putString(k,v==null?"":v).apply();}
 public static String get(Context c,String k){return c.getSharedPreferences(P,0).getString(k,"");}
 public static void putInt(Context c,String k,int v){c.getSharedPreferences(P,0).edit().putInt(k,v).apply();}
 public static int getInt(Context c,String k,int d){return c.getSharedPreferences(P,0).getInt(k,d);}
}
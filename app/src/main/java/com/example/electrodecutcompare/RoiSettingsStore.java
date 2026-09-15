package com.example.electrodecutcompare;

import android.content.Context;
import android.content.SharedPreferences;

public final class RoiSettingsStore {
    private static final String PREF="roi_settings_v04";
    private RoiSettingsStore(){}

    public static class Roi {
        public float l,t,r,b;
        public Roi(float l,float t,float r,float b){this.l=l;this.t=t;this.r=r;this.b=b;}
        public float[] array(){ return new float[]{l,t,r,b}; }
    }

    public static Roi load(Context c,String machine,String name,float[] def){
        SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        String k=machine+"_"+name+"_";
        return new Roi(
                p.getFloat(k+"l",def[0]),p.getFloat(k+"t",def[1]),
                p.getFloat(k+"r",def[2]),p.getFloat(k+"b",def[3]));
    }

    public static void save(Context c,String machine,String name,Roi q){
        String k=machine+"_"+name+"_";
        c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit()
                .putFloat(k+"l",q.l).putFloat(k+"t",q.t)
                .putFloat(k+"r",q.r).putFloat(k+"b",q.b).apply();
    }

    public static void reset(Context c,String machine){
        SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        SharedPreferences.Editor e=p.edit();
        for(String k:p.getAll().keySet()) if(k.startsWith(machine+"_")) e.remove(k);
        e.apply();
    }
}

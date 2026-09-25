package com.example.electrodecutcompare;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class CycleProgressView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float progressA = 0f, progressB = 0f;
    private float rangeStart = .45f, rangeEnd = .55f;

    public CycleProgressView(Context c){ super(c); }
    public CycleProgressView(Context c, AttributeSet a){ super(c,a); }
    public CycleProgressView(Context c, AttributeSet a, int s){ super(c,a,s); }

    public void setProblemRange(float start, float end){
        rangeStart=Math.max(0f,Math.min(1f,start));
        rangeEnd=Math.max(rangeStart,Math.min(1f,end));
        invalidate();
    }
    public void setProgress(float a,float b){
        progressA=Math.max(0f,Math.min(1f,a));
        progressB=Math.max(0f,Math.min(1f,b));
        invalidate();
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float w=getWidth(), h=getHeight();
        float label=38f, left=label+10f, right=w-12f;
        float trackH=Math.max(18f,h*.20f);
        float yA=h*.20f, yB=h*.62f;
        drawTrack(c,left,right,yA,trackH,progressA,Color.rgb(43,231,255));
        drawTrack(c,left,right,yB,trackH,progressB,Color.rgb(255,159,67));
        p.setTextSize(Math.max(22f,h*.20f)); p.setFakeBoldText(true);
        p.setColor(Color.rgb(43,231,255)); c.drawText(LanguageManager.ts("A"),8f,yA+trackH*.82f,p);
        p.setColor(Color.rgb(255,190,90)); c.drawText(LanguageManager.ts("B"),8f,yB+trackH*.82f,p);
        p.setFakeBoldText(false);
    }

    private void drawTrack(Canvas c,float left,float right,float y,float hh,float progress,int fill){
        float radius=hh/2f;
        p.setStyle(Paint.Style.FILL); p.setColor(Color.rgb(36,54,73));
        c.drawRoundRect(left,y,right,y+hh,radius,radius,p);
        p.setColor(fill);
        c.drawRoundRect(left,y,left+(right-left)*progress,y+hh,radius,radius,p);
        float x1=left+(right-left)*rangeStart, x2=left+(right-left)*rangeEnd;
        p.setColor(Color.argb(85,255,212,59));
        c.drawRect(x1,y,x2,y+hh,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(4f); p.setColor(Color.rgb(255,212,59));
        c.drawRect(x1,y-2f,x2,y+hh+2f,p);
        p.setStyle(Paint.Style.FILL);
        float mx=left+(right-left)*progress;
        p.setColor(Color.WHITE); c.drawCircle(mx,y+hh/2f,Math.max(6f,hh*.32f),p);
    }
}

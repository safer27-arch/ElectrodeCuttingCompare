package com.example.electrodecutcompare;

import android.content.Context;
import android.graphics.*;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import java.util.*;

/**
 * v0.8: separates apparent global shake of fixed machine units from the cutter module motion.
 * Lightweight CPU implementation: fixed reference ROIs at image corners + center-side areas,
 * block matching for global translation, then residual motion in a central cutter ROI.
 */
public final class VibrationCompensatedAnalyzer {
    public static class Result {
        public float[] cutter, global, residual, velocity, acceleration, jerk;
        public long[] timeMs;
        public int[] phase;
        public float globalRms, residualRms, settleMs, repeatability, risk;
        public int[] top;
        public Bitmap chart;
        public String summary;
    }

    private static class Gray { int w,h; byte[] p; Gray(int w,int h){this.w=w;this.h=h;p=new byte[w*h];} }
    private static class Shift { int dx,dy; float score; Shift(int x,int y,float s){dx=x;dy=y;score=s;} }

    public static Result analyze(Context c, Uri uri, long duration, String profile) throws Exception {
        MediaMetadataRetriever r=new MediaMetadataRetriever();
        r.setDataSource(c,uri);
        int fps=30; try {String q=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE); if(q!=null)fps=Math.max(1,Math.round(Float.parseFloat(q)));}catch(Exception ignored){}
        int n=Math.max(55,Math.min(145,(int)Math.ceil(duration/1000.0*Math.min(fps,36))+1));
        int w=224,h=126;
        float[] cutter=new float[n-1], global=new float[n-1], residual=new float[n-1]; long[] tm=new long[n-1];
        Gray prev=null;
        try {
            for(int i=0;i<n;i++){
                long ms=Math.round(duration*i/(double)(n-1));
                Bitmap raw=r.getFrameAtTime(ms*1000L,MediaMetadataRetriever.OPTION_CLOSEST);
                if(raw==null)continue;
                Bitmap b=Bitmap.createScaledBitmap(raw,w,h,true); Gray cur=gray(b); b.recycle();
                if(prev!=null){
                    Shift s=globalShift(prev,cur);
                    global[i-1]=(float)Math.hypot(s.dx,s.dy);
                    cutter[i-1]=cutterMotion(prev,cur,s.dx,s.dy);
                    // Residual cutter motion after subtracting common fixed-unit/camera vibration.
                    residual[i-1]=Math.max(0,cutter[i-1]-global[i-1]*2.2f);
                    tm[i-1]=ms;
                }
                prev=cur;
            }
        } finally {try{r.release();}catch(Exception ignored){}}

        Result o=new Result(); o.cutter=cutter;o.global=global;o.residual=residual;o.timeMs=tm;
        o.velocity=smooth(residual); o.acceleration=diff(o.velocity); o.jerk=diff(o.acceleration);
        o.globalRms=rms(global); o.residualRms=rms(residual); o.phase=phases(o.velocity);
        o.settleMs=settleTime(o); o.repeatability=repeatability(o.velocity); o.risk=risk(o); o.top=top3(o);
        o.summary=summary(o,profile,fps); o.chart=draw(o,profile); return o;
    }

    private static Gray gray(Bitmap b){int w=b.getWidth(),h=b.getHeight();Gray g=new Gray(w,h);int[]px=new int[w*h];b.getPixels(px,0,w,0,0,w,h);for(int i=0;i<px.length;i++){int z=px[i];g.p[i]=(byte)((Color.red(z)*30+Color.green(z)*59+Color.blue(z)*11)/100);}return g;}
    private static int u(byte b){return b&255;}

    private static Shift globalShift(Gray a,Gray b){
        int bestX=0,bestY=0;float best=Float.MAX_VALUE;
        for(int dy=-4;dy<=4;dy++)for(int dx=-4;dx<=4;dx++){
            float s=0;int k=0;
            // Four fixed reference zones; central cutter area deliberately excluded.
            s+=sad(a,b,8,8,62,34,dx,dy);k++;
            s+=sad(a,b,a.w-70,8,62,34,dx,dy);k++;
            s+=sad(a,b,8,a.h-42,62,34,dx,dy);k++;
            s+=sad(a,b,a.w-70,a.h-42,62,34,dx,dy);k++;
            s/=k;if(s<best){best=s;bestX=dx;bestY=dy;}
        }
        return new Shift(bestX,bestY,best);
    }
    private static float sad(Gray a,Gray b,int x0,int y0,int ww,int hh,int dx,int dy){float s=0;int n=0;for(int y=y0;y<y0+hh;y+=3)for(int x=x0;x<x0+ww;x+=3){int xx=x+dx,yy=y+dy;if(xx<0||yy<0||xx>=b.w||yy>=b.h)continue;s+=Math.abs(u(a.p[y*a.w+x])-u(b.p[yy*b.w+xx]));n++;}return n==0?9999:s/n;}
    private static float cutterMotion(Gray a,Gray b,int dx,int dy){
        // Broad center ROI: cutter module. User ROI remains available for fine quality analysis.
        int x0=(int)(a.w*.20),x1=(int)(a.w*.82),y0=(int)(a.h*.20),y1=(int)(a.h*.82);float s=0;int n=0;
        for(int y=y0;y<y1;y+=2)for(int x=x0;x<x1;x+=2){int xx=x+dx,yy=y+dy;if(xx<0||yy<0||xx>=b.w||yy>=b.h)continue;int d=Math.abs(u(a.p[y*a.w+x])-u(b.p[yy*b.w+xx]));if(d>8)s+=d;n++;}return n==0?0:s/n/4f;
    }
    private static float[] smooth(float[]v){float[]o=new float[v.length];for(int i=0;i<v.length;i++){float s=0;int n=0;for(int k=-1;k<=1;k++){int q=i+k;if(q>=0&&q<v.length){s+=v[q];n++;}}o[i]=s/n;}return o;}
    private static float[] diff(float[]v){float[]o=new float[v.length];for(int i=1;i<v.length;i++)o[i]=v[i]-v[i-1];return o;}
    private static float rms(float[]v){float s=0;for(float x:v)s+=x*x;return (float)Math.sqrt(s/Math.max(1,v.length));}
    private static float max(float[]v){float m=.001f;for(float x:v)m=Math.max(m,Math.abs(x));return m;}
    private static int[] phases(float[]v){int[]p=new int[v.length];float th=max(v)*.20f;for(int i=0;i<v.length;i++){if(v[i]<th)p[i]=0;else p[i]=1;} // moving vs wait first
        // split moving runs into accelerate/high/decelerate using local slope
        for(int i=1;i<v.length-1;i++)if(p[i]>0){float d=v[i+1]-v[i-1];if(d>th*.18)p[i]=2;else if(d<-th*.18)p[i]=4;else p[i]=3;}return p;}
    private static float settleTime(Result r){int peak=0;for(int i=1;i<r.residual.length;i++)if(r.residual[i]>r.residual[peak])peak=i;float th=max(r.residual)*.16f;for(int i=peak+1;i<r.residual.length-3;i++)if(r.residual[i]<th&&r.residual[i+1]<th&&r.residual[i+2]<th){return r.timeMs[i]-r.timeMs[peak];}return r.timeMs.length>0?r.timeMs[r.timeMs.length-1]-r.timeMs[peak]:0;}
    private static float repeatability(float[]v){ArrayList<Integer>peaks=new ArrayList<>();float th=max(v)*.55f;for(int i=2;i<v.length-2;i++)if(v[i]>th&&v[i]>=v[i-1]&&v[i]>=v[i+1]&&(peaks.isEmpty()||i-peaks.get(peaks.size()-1)>5))peaks.add(i);if(peaks.size()<3)return 0;float mean=0;for(int i=1;i<peaks.size();i++)mean+=peaks.get(i)-peaks.get(i-1);mean/=peaks.size()-1;float s=0;for(int i=1;i<peaks.size();i++){float d=(peaks.get(i)-peaks.get(i-1))-mean;s+=d*d;}return (float)Math.sqrt(s/(peaks.size()-1))/Math.max(1,mean)*100;}
    private static float risk(Result r){float vibration=Math.min(40,r.globalRms*7);float jerk=Math.min(35,rms(r.jerk)*5);float rep=Math.min(25,r.repeatability*2);return Math.min(100,vibration+jerk+rep);}
    private static int[] top3(Result r){ArrayList<Integer>ids=new ArrayList<>();for(int i=0;i<r.residual.length;i++)ids.add(i);Collections.sort(ids,(a,b)->Float.compare(scoreAt(r,b),scoreAt(r,a)));ArrayList<Integer>o=new ArrayList<>();for(int i:ids){boolean far=true;for(int q:o)if(Math.abs(q-i)<5)far=false;if(far)o.add(i);if(o.size()==3)break;}int[]z=new int[o.size()];for(int i=0;i<z.length;i++)z[i]=o.get(i);return z;}
    private static float scoreAt(Result r,int i){return r.residual[i]+Math.abs(r.jerk[i])*1.8f+r.global[i]*.7f;}
    private static String phaseName(int p){return p==0?"대기/안정":p==2?"가속":p==3?"고속 이동":p==4?"감속/정지":"이동";}
    private static String summary(Result r,String pf,int fps){String lv=r.risk<20?"정상 후보":r.risk<40?"WATCH":"이상 후보";StringBuilder s=new StringBuilder();s.append("v0.8 Cutter Motion / Global Vibration 분리 · ").append(lv).append("\n");s.append(pf).append(String.format(Locale.getDefault()," · FPS≈%d · Risk %.1f/100",fps,r.risk));s.append(String.format(Locale.getDefault(),"\n고정부 공통진동 RMS %.2f · 보정 후 Cutter 운동 RMS %.2f",r.globalRms,r.residualRms));s.append(String.format(Locale.getDefault(),"\n정지 안정화 약 %.0f ms · Cycle 반복변동 %.1f%%",r.settleMs,r.repeatability));for(int k=0;k<r.top.length;k++){int i=r.top[k];s.append(String.format(Locale.getDefault(),"\nTop%d %.3fs · %s · Cutter %.2f / 공통진동 %.2f",k+1,r.timeMs[i]/1000f,phaseName(r.phase[i]),r.residual[i],r.global[i]));}s.append("\n※ 고정부 Reference 영역의 공통 이동을 먼저 제거한 뒤 Cutter 상대운동을 평가합니다.");return s.toString();}

    private static Bitmap draw(Result r,String pf){int w=1200,h=900,l=75,rr=1140,t=210,bot=600;Bitmap o=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(o);c.drawColor(Color.WHITE);Paint p=new Paint(1);p.setColor(Color.rgb(15,48,88));p.setTextSize(38);p.setFakeBoldText(true);c.drawText(LanguageManager.ts("v0.8 Easy Diagnostic · Cutter vs Vibration"),45,58,p);p.setFakeBoldText(false);p.setTextSize(23);p.setColor(Color.DKGRAY);c.drawText(LanguageManager.ts(pf+" · 고정부 흔들림 제거 후 Cutter 상대운동"),45,98,p);
        String lv=r.risk<20?"NORMAL":r.risk<40?"WATCH":"ANOMALY CANDIDATE";p.setTextSize(30);p.setFakeBoldText(true);p.setColor(r.risk<20?Color.rgb(20,130,70):r.risk<40?Color.rgb(210,135,0):Color.rgb(190,35,35));c.drawText(lv+String.format(Locale.getDefault(),"   Risk %.1f/100",r.risk),45,150,p);p.setFakeBoldText(false);
        p.setStyle(Paint.Style.STROKE);p.setColor(Color.LTGRAY);c.drawRect(l,t,rr,bot,p);float mr=max(r.residual),mg=max(r.global);Paint pc=new Paint(1);pc.setStyle(Paint.Style.STROKE);pc.setStrokeWidth(5);pc.setColor(Color.rgb(30,90,210));Paint pg=new Paint(pc);pg.setStrokeWidth(3);pg.setColor(Color.rgb(130,130,130));for(int i=1;i<r.residual.length;i++){float x1=l+(rr-l)*(i-1f)/(r.residual.length-1),x2=l+(rr-l)*i/(r.residual.length-1);c.drawLine(x1,bot-(bot-t-20)*r.residual[i-1]/mr,x2,bot-(bot-t-20)*r.residual[i]/mr,pc);c.drawLine(x1,bot-(bot-t-20)*r.global[i-1]/mg,x2,bot-(bot-t-20)*r.global[i]/mg,pg);}Paint mark=new Paint(1);mark.setColor(Color.MAGENTA);mark.setStrokeWidth(5);for(int k=0;k<r.top.length;k++){float x=l+(rr-l)*r.top[k]/(float)Math.max(1,r.residual.length-1);c.drawLine(x,t,x,bot,mark);p.setStyle(Paint.Style.FILL);p.setTextSize(22);p.setColor(Color.MAGENTA);c.drawText("#"+(k+1),x+4,t+25+24*k,p);}p.setColor(Color.DKGRAY);p.setTextSize(23);c.drawText(LanguageManager.ts("파랑 = 보정된 Cutter 운동   회색 = 고정부 공통진동   자홍 = Top3"),75,650,p);c.drawText(String.format(Locale.getDefault(),"Global vibration RMS %.2f   |   Settle %.0f ms   |   Repeatability %.1f%%",r.globalRms,r.settleMs,r.repeatability),75,695,p);
        // simple phase strip
        int y=745,hh=45;for(int i=0;i<r.phase.length;i++){float x1=l+(rr-l)*i/(float)r.phase.length,x2=l+(rr-l)*(i+1)/(float)r.phase.length;int col=r.phase[i]==0?Color.rgb(210,235,215):r.phase[i]==2?Color.rgb(255,226,150):r.phase[i]==4?Color.rgb(255,190,160):Color.rgb(185,215,245);p.setColor(col);c.drawRect(x1,y,x2,y+hh,p);}p.setColor(Color.DKGRAY);p.setTextSize(20);c.drawText(LanguageManager.ts("구간: 대기/안정 → 가속 → 고속이동 → 감속/정지 (영상 신호 기반 자동 분할)"),75,830,p);return o;}
}

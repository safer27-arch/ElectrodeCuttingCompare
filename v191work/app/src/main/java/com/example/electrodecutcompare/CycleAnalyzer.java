package com.example.electrodecutcompare;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CycleAnalyzer {
    public static class Result {
        public final float[] motion;
        public final float[] cx;
        public final float[] cy;
        public final Bitmap chart;
        public final String summary;
        public Result(float[] motion, float[] cx, float[] cy, Bitmap chart, String summary) {
            this.motion=motion; this.cx=cx; this.cy=cy; this.chart=chart; this.summary=summary;
        }
    }

    public static Result analyze(Context context, Uri uri, long durationMs, String label) {
        final int samples=31, sw=160, sh=90;
        float[] motion=new float[samples-1], cx=new float[samples-1], cy=new float[samples-1];
        MediaMetadataRetriever r=new MediaMetadataRetriever();
        Bitmap prev=null;
        try {
            r.setDataSource(context, uri);
            for(int i=0;i<samples;i++){
                long us=(long)((durationMs*1000.0)*i/(samples-1));
                Bitmap raw=r.getFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST);
                if(raw==null) raw=r.getFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if(raw==null) continue;
                Bitmap cur=Bitmap.createScaledBitmap(raw,sw,sh,true);
                if(prev!=null){
                    float sum=0, wx=0, wy=0, wsum=0;
                    for(int y=8;y<sh-6;y+=2){
                        for(int x=12;x<sw-8;x+=2){
                            int a=prev.getPixel(x,y), b=cur.getPixel(x,y);
                            int ga=(Color.red(a)*30+Color.green(a)*59+Color.blue(a)*11)/100;
                            int gb=(Color.red(b)*30+Color.green(b)*59+Color.blue(b)*11)/100;
                            float d=Math.abs(gb-ga);
                            sum+=d;
                            if(d>18){ wx+=x*d; wy+=y*d; wsum+=d; }
                        }
                    }
                    int k=i-1;
                    motion[k]=sum/(((sh-14)/2f)*((sw-20)/2f));
                    cx[k]=wsum>0 ? wx/wsum/sw : 0.5f;
                    cy[k]=wsum>0 ? wy/wsum/sh : 0.5f;
                }
                if(prev!=null) prev.recycle();
                prev=cur;
            }
        } finally {
            if(prev!=null) prev.recycle();
            try{r.release();}catch(Exception ignored){}
        }

        float avg=0,max=0;
        for(float v:motion){avg+=v; if(v>max)max=v;}
        avg/=Math.max(1,motion.length);
        float repeatability=100f;
        if(max>0){
            float dev=0;
            for(float v:motion) dev+=Math.abs(v-avg);
            dev/=motion.length;
            repeatability=Math.max(0,100f-(dev/max)*100f);
        }

        String[] phases={"CUT","GRIP","FORWARD","NIP","OPEN","RETURN"};
        StringBuilder sb=new StringBuilder();
        sb.append(String.format(Locale.getDefault(),"%s · 평균 Motion %.1f / Peak %.1f / 반복 안정도(참고) %.0f%%\n",
                label,avg,max,repeatability));
        sb.append("자동 단계 후보: ");
        for(int i=0;i<phases.length;i++){
            int idx=Math.round(i*(motion.length-1)/5f);
            sb.append(phases[i]).append(" ").append(Math.round(idx*durationMs/(float)motion.length)).append("ms");
            if(i<phases.length-1) sb.append(" → ");
        }
        sb.append("\n※ 단계 시점은 v0.2 휴리스틱 후보이며 실제 기구 인식 학습 전에는 품질 판정값으로 사용하지 마세요.");

        return new Result(motion,cx,cy,drawChart(motion,cx,cy,label),sb.toString());
    }

    private static Bitmap drawChart(float[] motion,float[] cx,float[] cy,String label){
        int w=1200,h=620;
        Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(out); c.drawColor(Color.WHITE);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.rgb(15,48,88)); p.setTextSize(34); p.setFakeBoldText(true);
        c.drawText(label+" Cycle Motion / Trajectory",45,55,p);
        p.setFakeBoldText(false); p.setTextSize(22); p.setColor(Color.DKGRAY);
        c.drawText("상단: 프레임 변화량   하단: 움직임 중심 궤적(촬영각 보정 전 참고값)",45,90,p);

        int left=70,right=1140,top=125,mid=350,bottom=565;
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(Color.LTGRAY);
        c.drawRect(left,top,right,mid,p); c.drawRect(left,mid+35,right,bottom,p);
        float max=1; for(float v:motion) if(v>max)max=v;
        p.setStrokeWidth(5); p.setColor(Color.rgb(28,94,180));
        for(int i=1;i<motion.length;i++){
            float x1=left+(right-left)*(i-1f)/(motion.length-1);
            float x2=left+(right-left)*i/(motion.length-1);
            float y1=mid-15-(mid-top-30)*(motion[i-1]/max);
            float y2=mid-15-(mid-top-30)*(motion[i]/max);
            c.drawLine(x1,y1,x2,y2,p);
        }
        p.setColor(Color.rgb(200,60,45)); p.setStrokeWidth(4);
        for(int i=1;i<cx.length;i++){
            float x1=left+cx[i-1]*(right-left), y1=mid+45+cy[i-1]*(bottom-mid-55);
            float x2=left+cx[i]*(right-left), y2=mid+45+cy[i]*(bottom-mid-55);
            c.drawLine(x1,y1,x2,y2,p);
        }
        p.setStyle(Paint.Style.FILL); p.setTextSize(20); p.setColor(Color.DKGRAY);
        String[] phases={"CUT","GRIP","FORWARD","NIP","OPEN","RETURN"};
        for(int i=0;i<phases.length;i++){
            float x=left+(right-left)*i/5f;
            c.drawText(phases[i],Math.min(x,right-80),bottom+35,p);
        }
        return out;
    }

    public static Bitmap compare(Result a, Result b){
        int w=1200,h=700;
        Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(out); c.drawColor(Color.WHITE);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.rgb(15,48,88)); p.setTextSize(34); p.setFakeBoldText(true);
        c.drawText("A/B Cycle Motion Comparison",45,55,p);
        p.setFakeBoldText(false); p.setTextSize(21); p.setColor(Color.DKGRAY);
        c.drawText("카메라 각도 차이를 고려하기 위해 절대 좌표보다 정규화된 Motion 패턴을 우선 비교",45,90,p);

        int left=70,right=1140,top=135,bottom=590;
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(Color.LTGRAY); c.drawRect(left,top,right,bottom,p);
        float maxA=1,maxB=1; for(float v:a.motion)maxA=Math.max(maxA,v); for(float v:b.motion)maxB=Math.max(maxB,v);
        Paint pa=new Paint(Paint.ANTI_ALIAS_FLAG); pa.setStyle(Paint.Style.STROKE); pa.setStrokeWidth(5); pa.setColor(Color.rgb(30,90,210));
        Paint pb=new Paint(Paint.ANTI_ALIAS_FLAG); pb.setStyle(Paint.Style.STROKE); pb.setStrokeWidth(5); pb.setColor(Color.rgb(220,65,45));
        for(int i=1;i<a.motion.length;i++){
            float x1=left+(right-left)*(i-1f)/(a.motion.length-1),x2=left+(right-left)*i/(a.motion.length-1);
            c.drawLine(x1,bottom-(bottom-top-20)*a.motion[i-1]/maxA,x2,bottom-(bottom-top-20)*a.motion[i]/maxA,pa);
        }
        for(int i=1;i<b.motion.length;i++){
            float x1=left+(right-left)*(i-1f)/(b.motion.length-1),x2=left+(right-left)*i/(b.motion.length-1);
            c.drawLine(x1,bottom-(bottom-top-20)*b.motion[i-1]/maxB,x2,bottom-(bottom-top-20)*b.motion[i]/maxB,pb);
        }
        p.setStyle(Paint.Style.FILL); p.setTextSize(24); p.setColor(Color.rgb(30,90,210)); c.drawText("● A",80,650,p);
        p.setColor(Color.rgb(220,65,45)); c.drawText("● B",180,650,p);
        p.setColor(Color.DKGRAY); c.drawText("※ 현재 단계는 공정 패턴 비교용이며 전극 Damage 확정 판정은 다음 학습/ROI 단계에서 보강",300,650,p);
        return out;
    }
}

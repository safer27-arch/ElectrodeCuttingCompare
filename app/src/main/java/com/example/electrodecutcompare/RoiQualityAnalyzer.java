package com.example.electrodecutcompare;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import java.util.Locale;

/**
 * v0.3 lightweight ROI analyzer.
 *
 * Important:
 * - This is intentionally CPU-only and does not use a heavy ML model.
 * - It estimates three process landmarks from contrast/edge/motion inside broad ROIs:
 *   Gripper, Electrode Tip, Nip.
 * - Values are "relative quality indicators" until the ROI/thresholds are tuned with
 *   the user's real machine videos.
 */
public final class RoiQualityAnalyzer {
    private RoiQualityAnalyzer(){}

    public static class PointF {
        public float x, y;
        public PointF(float x, float y){this.x=x; this.y=y;}
    }

    public static class Metrics {
        public final float entryAngleDeg;
        public final float nipOffsetPx;
        public final float gripperToTipPx;
        public final float tipJitterPx;
        public final float gripperJitterPx;
        public final float qualityScore;
        public final PointF gripper;
        public final PointF tip;
        public final PointF nip;
        public final Bitmap overlay;
        public final String summary;

        public Metrics(float entryAngleDeg,float nipOffsetPx,float gripperToTipPx,
                       float tipJitterPx,float gripperJitterPx,float qualityScore,
                       PointF gripper,PointF tip,PointF nip,Bitmap overlay,String summary){
            this.entryAngleDeg=entryAngleDeg;
            this.nipOffsetPx=nipOffsetPx;
            this.gripperToTipPx=gripperToTipPx;
            this.tipJitterPx=tipJitterPx;
            this.gripperJitterPx=gripperJitterPx;
            this.qualityScore=qualityScore;
            this.gripper=gripper; this.tip=tip; this.nip=nip;
            this.overlay=overlay; this.summary=summary;
        }
    }

    // Default broad ROI layout for the current cutter videos.
    // All coordinates are normalized 0..1 and can later be made user-adjustable.
    private static final float[] ROI_GRIPPER = {0.43f,0.22f,0.78f,0.64f};
    private static final float[] ROI_TIP     = {0.48f,0.43f,0.89f,0.82f};
    private static final float[] ROI_NIP     = {0.60f,0.30f,0.95f,0.84f};

    public static Metrics analyze(Context context, Uri uri, long durationMs, int percent,
                                  BitmapAnalysis.Transform cameraTransform,
                                  int targetW, int targetH, String label) {
        String machine = label.startsWith("설비 B") ? "B" : "A";
        final float[] roiGripper=RoiSettingsStore.load(context,machine,"GRIPPER",ROI_GRIPPER).array();
        final float[] roiTip=RoiSettingsStore.load(context,machine,"TIP",ROI_TIP).array();
        final float[] roiNip=RoiSettingsStore.load(context,machine,"NIP",ROI_NIP).array();
        long centerMs = Math.max(0, durationMs * percent / 100L);
        final int samples=9;
        final long spanMs=Math.min(900, Math.max(300, durationMs/5));
        PointF[] grip=new PointF[samples];
        PointF[] tip=new PointF[samples];
        PointF[] nip=new PointF[samples];
        Bitmap centerFrame=null;

        MediaMetadataRetriever r=new MediaMetadataRetriever();
        try{
            r.setDataSource(context,uri);
            for(int i=0;i<samples;i++){
                float u=(i-(samples-1)/2f)/((samples-1)/2f);
                long ms=Math.max(0,Math.min(durationMs-1,centerMs+(long)(u*spanMs/2)));
                Bitmap raw=r.getFrameAtTime(ms*1000L,MediaMetadataRetriever.OPTION_CLOSEST);
                if(raw==null) raw=r.getFrameAtTime(ms*1000L,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if(raw==null) continue;
                Bitmap fit=BitmapAnalysis.fitMaxWidth(raw,targetW);
                if(fit.getWidth()!=targetW || fit.getHeight()!=targetH)
                    fit=Bitmap.createScaledBitmap(fit,targetW,targetH,true);
                if(cameraTransform!=null)
                    fit=BitmapAnalysis.applyTransform(fit,targetW,targetH,cameraTransform);

                grip[i]=locateEdgeCenter(fit,roiGripper,true);
                tip[i]=locateElectrodeTip(fit,roiTip);
                nip[i]=locateEdgeCenter(fit,roiNip,false);
                if(i==samples/2) centerFrame=fit.copy(Bitmap.Config.ARGB_8888,true);
            }
        }finally{
            try{r.release();}catch(Exception ignored){}
        }

        PointF g=medianPoint(grip,targetW*0.60f,targetH*0.43f);
        PointF t=medianPoint(tip,targetW*0.70f,targetH*0.59f);
        PointF n=medianPoint(nip,targetW*0.80f,targetH*0.53f);

        // Use the Tip->Nip line as the entry direction.
        float dx=n.x-t.x, dy=n.y-t.y;
        float angle=(float)Math.toDegrees(Math.atan2(dy,Math.max(1e-5f,dx)));
        float offset=distancePointToHorizontalCenterline(t,n);
        float gt=dist(g,t);
        float tj=jitter(tip,t);
        float gj=jitter(grip,g);

        // Relative score: intentionally conservative and normalized for visual tracking.
        float diag=(float)Math.hypot(targetW,targetH);
        float penalty=0;
        penalty += Math.min(35, Math.abs(angle)*1.7f);
        penalty += Math.min(25, offset/diag*500f);
        penalty += Math.min(20, tj/diag*700f);
        penalty += Math.min(20, gj/diag*700f);
        float score=Math.max(0,100-penalty);

        if(centerFrame==null) centerFrame=Bitmap.createBitmap(targetW,targetH,Bitmap.Config.ARGB_8888);
        Bitmap overlay=drawOverlay(centerFrame,g,t,n,angle,offset,tj,gj,score,label,roiGripper,roiTip,roiNip);

        String summary=String.format(Locale.getDefault(),
                "%s · ROI 정밀분석(참고)\n진입각 %.2f° / Nip 상대 Offset %.1f px / Gripper→Tip %.1f px\nTip 흔들림 %.1f px / Gripper 흔들림 %.1f px / 안정 Score %.0f/100",
                label,angle,offset,gt,tj,gj,score);
        return new Metrics(angle,offset,gt,tj,gj,score,g,t,n,overlay,summary);
    }

    public static Bitmap compare(Metrics a, Metrics b){
        int w=1200,h=760;
        Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(out); c.drawColor(Color.WHITE);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.rgb(15,48,88)); p.setTextSize(36); p.setFakeBoldText(true);
        c.drawText(LanguageManager.ts("v0.4 A/B ROI Quality Comparison"),45,55,p);
        p.setFakeBoldText(false); p.setTextSize(22); p.setColor(Color.DKGRAY);
        c.drawText(LanguageManager.ts("촬영각 보정 후 전극 선단 / Gripper / Nip 상대값 비교"),45,92,p);

        String[] names={"진입각 |°|","Nip Offset px","Tip 흔들림 px","Gripper 흔들림 px","안정 Score"};
        float[] va={Math.abs(a.entryAngleDeg),a.nipOffsetPx,a.tipJitterPx,a.gripperJitterPx,a.qualityScore};
        float[] vb={Math.abs(b.entryAngleDeg),b.nipOffsetPx,b.tipJitterPx,b.gripperJitterPx,b.qualityScore};

        int left=300, right=1110, top=145, row=100;
        for(int i=0;i<names.length;i++){
            int y=top+i*row;
            p.setColor(Color.rgb(40,50,65)); p.setTextSize(24);
            c.drawText(names[i],55,y+32,p);
            float max=Math.max(1,Math.max(va[i],vb[i]));
            if(i==4) max=100;
            int barW=right-left;
            p.setColor(Color.rgb(225,232,240)); c.drawRect(left,y,right,y+28,p);
            p.setColor(Color.rgb(30,90,210)); c.drawRect(left,y,left+barW*(va[i]/max),y+12,p);
            p.setColor(Color.rgb(220,65,45)); c.drawRect(left,y+16,left+barW*(vb[i]/max),y+28,p);
            p.setTextSize(20); p.setColor(Color.rgb(30,90,210));
            c.drawText(String.format(Locale.getDefault(),"A %.1f",va[i]),right-150,y+12,p);
            p.setColor(Color.rgb(220,65,45));
            c.drawText(String.format(Locale.getDefault(),"B %.1f",vb[i]),right-150,y+31,p);
        }
        p.setTextSize(23); p.setColor(Color.rgb(30,90,210)); c.drawText(LanguageManager.ts("● 설비 A"),60,690,p);
        p.setColor(Color.rgb(220,65,45)); c.drawText(LanguageManager.ts("● 설비 B"),210,690,p);
        p.setColor(Color.DKGRAY); p.setTextSize(20);
        c.drawText(LanguageManager.ts("※ px 값은 현재 영상 내 상대지표입니다. 실제 mm 변환은 기준 치수 Calibration을 추가하면 가능합니다."),390,690,p);
        c.drawText(LanguageManager.ts("※ v0.4 ROI는 설비별 저장값을 사용합니다. 현재 지표는 NG 확정 판정용이 아닙니다."),60,730,p);
        return out;
    }

    private static PointF locateElectrodeTip(Bitmap b,float[] roi){
        int x0=(int)(roi[0]*b.getWidth()), y0=(int)(roi[1]*b.getHeight());
        int x1=(int)(roi[2]*b.getWidth()), y1=(int)(roi[3]*b.getHeight());
        float best=-1; int bx=(x0+x1)/2, by=(y0+y1)/2;
        // Prefer strong edge points toward the right side of the electrode ROI.
        for(int y=y0+2;y<y1-2;y+=2){
            for(int x=x0+2;x<x1-2;x+=2){
                float e=edgeMag(b,x,y);
                float rightBias=(x-x0)/(float)Math.max(1,x1-x0);
                float score=e*(0.55f+0.45f*rightBias);
                if(score>best){best=score; bx=x; by=y;}
            }
        }
        return new PointF(bx,by);
    }

    private static PointF locateEdgeCenter(Bitmap b,float[] roi,boolean preferLeftCenter){
        int x0=(int)(roi[0]*b.getWidth()), y0=(int)(roi[1]*b.getHeight());
        int x1=(int)(roi[2]*b.getWidth()), y1=(int)(roi[3]*b.getHeight());
        double sx=0,sy=0,sw=0;
        for(int y=y0+2;y<y1-2;y+=2){
            for(int x=x0+2;x<x1-2;x+=2){
                float e=edgeMag(b,x,y);
                if(e<35) continue;
                float pos=(x-x0)/(float)Math.max(1,x1-x0);
                float bias=preferLeftCenter ? (1.15f-0.3f*Math.abs(pos-0.45f)) : (0.8f+0.4f*pos);
                double w=e*bias;
                sx+=x*w; sy+=y*w; sw+=w;
            }
        }
        if(sw<=0) return new PointF((x0+x1)/2f,(y0+y1)/2f);
        return new PointF((float)(sx/sw),(float)(sy/sw));
    }

    private static float edgeMag(Bitmap b,int x,int y){
        int xm=Math.max(0,x-1), xp=Math.min(b.getWidth()-1,x+1);
        int ym=Math.max(0,y-1), yp=Math.min(b.getHeight()-1,y+1);
        int gx=gray(b.getPixel(xp,y))-gray(b.getPixel(xm,y));
        int gy=gray(b.getPixel(x,yp))-gray(b.getPixel(x,ym));
        return Math.min(255,Math.abs(gx)+Math.abs(gy));
    }

    private static int gray(int c){
        return (Color.red(c)*30+Color.green(c)*59+Color.blue(c)*11)/100;
    }

    private static PointF medianPoint(PointF[] p,float defX,float defY){
        float sx=0,sy=0; int n=0;
        for(PointF q:p) if(q!=null){sx+=q.x; sy+=q.y; n++;}
        if(n==0) return new PointF(defX,defY);
        return new PointF(sx/n,sy/n);
    }

    private static float jitter(PointF[] p,PointF center){
        float s=0; int n=0;
        for(PointF q:p) if(q!=null){float d=dist(q,center); s+=d*d; n++;}
        return n==0?0:(float)Math.sqrt(s/n);
    }

    private static float dist(PointF a,PointF b){
        return (float)Math.hypot(a.x-b.x,a.y-b.y);
    }

    // "Offset" = vertical deviation of tip relative to the local Tip->Nip entry line midpoint.
    private static float distancePointToHorizontalCenterline(PointF tip,PointF nip){
        return Math.abs(tip.y-nip.y);
    }

    private static Bitmap drawOverlay(Bitmap src,PointF g,PointF t,PointF n,
                                      float angle,float offset,float tj,float gj,float score,String label,float[] roiGripper,float[] roiTip,float[] roiNip){
        Bitmap out=src.copy(Bitmap.Config.ARGB_8888,true);
        Canvas c=new Canvas(out);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(5);

        p.setColor(Color.rgb(0,190,80));
        c.drawCircle(g.x,g.y,20,p);
        p.setColor(Color.rgb(255,160,0));
        c.drawCircle(t.x,t.y,20,p);
        p.setColor(Color.rgb(220,40,40));
        c.drawCircle(n.x,n.y,20,p);

        p.setColor(Color.rgb(255,210,0)); p.setStrokeWidth(4);
        c.drawLine(g.x,g.y,t.x,t.y,p);
        c.drawLine(t.x,t.y,n.x,n.y,p);

        // Draw broad default ROIs
        p.setStrokeWidth(2);
        drawRoi(c,p,src,roiGripper,Color.rgb(0,190,80));
        drawRoi(c,p,src,roiTip,Color.rgb(255,160,0));
        drawRoi(c,p,src,roiNip,Color.rgb(220,40,40));

        p.setStyle(Paint.Style.FILL); p.setColor(Color.argb(190,0,0,0));
        c.drawRect(12,12,src.getWidth()-12,118,p);
        p.setColor(Color.WHITE); p.setTextSize(Math.max(20,src.getWidth()/35f)); p.setFakeBoldText(true);
        c.drawText(label+" ROI",28,48,p);
        p.setFakeBoldText(false); p.setTextSize(Math.max(16,src.getWidth()/47f));
        c.drawText(String.format(Locale.getDefault(),"Angle %.2f°  Offset %.1fpx  TipJitter %.1fpx  GripJitter %.1fpx  Score %.0f",
                angle,offset,tj,gj,score),28,88,p);
        return out;
    }

    private static void drawRoi(Canvas c,Paint p,Bitmap b,float[] r,int color){
        p.setColor(color); p.setStyle(Paint.Style.STROKE);
        c.drawRect(r[0]*b.getWidth(),r[1]*b.getHeight(),r[2]*b.getWidth(),r[3]*b.getHeight(),p);
    }
}

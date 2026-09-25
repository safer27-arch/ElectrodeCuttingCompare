package com.example.electrodecutcompare;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * v1.8 Fast Engine / Cycle Intelligence analyzer.
 * Lightweight image-based repeatability analysis for the cutter's repeated left-right-left motion.
 * It is deliberately a relative video metric, not a calibrated displacement or NG judge.
 */
public final class CycleRepeatabilityAnalyzer {
    private CycleRepeatabilityAnalyzer() {}

    public static final class Result {
        public String label;
        public int fps;
        public int cycleCount;
        public float[] cycleTimesSec;
        public float meanCycleSec;
        public float cycleTimeCvPct;
        public float repeatabilityScore;
        public float[] meanPosition;
        public float[] stdPosition;
        public float[] meanSpeed;
        public List<float[]> cycles;
        public float[] cycleDeviationPct;
        public int[] worstCycleIndices;
        public float[] worstCycleDeviationPct;
        public float[] stageSpreadPct;
        public int worstStageIndex;
        public float cycleTimeTrendPct;
        public float[] cycleStartSec;
        public float[] cycleEndSec;
        public int excludedCycleCount;
        public int bestCycleIndex;
        public int[] worstStagePerCycle;
        public float[] worstStageDeviationPct;
        public String summary;
        public boolean reliable;
        public String engineName;
        public int sampledFrameCount;
        public double traceExtractSec;
    }

    public static final class CompareResult {
        public final Bitmap chart;
        public final String summary;
        public final float meanTrajectoryDifferencePct;
        public final int worstStartPct;
        public final int worstEndPct;
        public final float meanSpeedDifferencePct;
        public CompareResult(Bitmap chart, String summary, float diff, int start, int end, float speedDiff) {
            this.chart=chart; this.summary=summary; this.meanTrajectoryDifferencePct=diff;
            this.worstStartPct=start; this.worstEndPct=end; this.meanSpeedDifferencePct=speedDiff;
        }
    }

    private static final class Gray {
        final int w,h; final byte[] p;
        Gray(int w,int h){this.w=w;this.h=h;this.p=new byte[w*h];}
    }
    private static final class Shift { final int dx,dy; Shift(int dx,int dy){this.dx=dx;this.dy=dy;} }
    private static final class Trace {
        float[] x, energy; long[] timeMs; int fps;
        String engineName; int sampleCount; double extractSec;
    }
    private static final class Segment { int start,end; boolean invert; Segment(int s,int e,boolean i){start=s;end=e;invert=i;} }

    public static Result analyze(Context context, Uri uri, long durationMs, String label) throws Exception {
        return analyze(context,uri,durationMs,label,false);
    }

    public static Result analyze(Context context, Uri uri, long durationMs, String label, boolean fastMode) throws Exception {
        Trace t=extractTrace(context,uri,durationMs,fastMode);
        Result r=new Result(); r.label=label; r.fps=t.fps;
        r.engineName=t.engineName==null?"Frame Scan":t.engineName; r.sampledFrameCount=t.sampleCount; r.traceExtractSec=t.extractSec;
        List<Segment> raw=findCycles(t.x,t.energy);

        ArrayList<Segment> candidates=new ArrayList<>();
        ArrayList<Float> rawTimes=new ArrayList<>();
        for(Segment s:raw){
            if(s.end-s.start<5)continue;
            long st=t.timeMs[Math.max(0,Math.min(t.timeMs.length-1,s.start))];
            long en=t.timeMs[Math.max(0,Math.min(t.timeMs.length-1,s.end))];
            float dt=Math.max(0.001f,(en-st)/1000f);
            candidates.add(s); rawTimes.add(dt);
            if(candidates.size()>=14)break;
        }

        float[] rawTimeArray=new float[rawTimes.size()];
        for(int i=0;i<rawTimes.size();i++)rawTimeArray[i]=rawTimes.get(i);
        float medianTime=median(rawTimeArray);
        ArrayList<Segment> accepted=new ArrayList<>();
        ArrayList<Float> acceptedTimes=new ArrayList<>();
        for(int i=0;i<candidates.size();i++){
            float dt=rawTimes.get(i);
            boolean timeOk=medianTime<=0 || (dt>=medianTime*.58f && dt<=medianTime*1.65f);
            if(timeOk){accepted.add(candidates.get(i));acceptedTimes.add(dt);}
        }
        if(accepted.size()<2 && candidates.size()>=2){accepted.clear();accepted.addAll(candidates);acceptedTimes.clear();acceptedTimes.addAll(rawTimes);}
        r.excludedCycleCount=Math.max(0,candidates.size()-accepted.size());

        r.cycles=new ArrayList<>();
        ArrayList<Float> times=new ArrayList<>(), starts=new ArrayList<>(), ends=new ArrayList<>();
        for(int i=0;i<accepted.size();i++){
            Segment s=accepted.get(i);
            float[] c=resample(t.x,s.start,s.end,101);
            normalizeCycle(c,s.invert);
            r.cycles.add(c);
            times.add(acceptedTimes.get(i));
            starts.add(t.timeMs[Math.max(0,Math.min(t.timeMs.length-1,s.start))]/1000f);
            ends.add(t.timeMs[Math.max(0,Math.min(t.timeMs.length-1,s.end))]/1000f);
            if(r.cycles.size()>=10)break;
        }
        r.cycleCount=r.cycles.size();
        r.cycleTimesSec=toFloatArray(times);
        r.cycleStartSec=toFloatArray(starts);
        r.cycleEndSec=toFloatArray(ends);
        r.meanCycleSec=mean(r.cycleTimesSec);
        r.cycleTimeCvPct=cvPct(r.cycleTimesSec);
        r.meanPosition=meanCurve(r.cycles,101);
        r.stdPosition=stdCurve(r.cycles,r.meanPosition);
        r.meanSpeed=speed(r.meanPosition);
        r.reliable=r.cycleCount>=2;
        float spread=mean(r.stdPosition);
        float trajectoryPenalty=Math.min(72f,spread*420f);
        float timingPenalty=Math.min(28f,r.cycleTimeCvPct*2.2f);
        r.repeatabilityScore=r.reliable?Math.max(0f,100f-trajectoryPenalty-timingPenalty):0f;
        r.cycleDeviationPct=cycleDeviationPct(r.cycles,r.meanPosition);
        r.worstCycleIndices=topIndices(r.cycleDeviationPct,3);
        r.worstCycleDeviationPct=new float[r.worstCycleIndices.length];
        for(int i=0;i<r.worstCycleIndices.length;i++){int q=r.worstCycleIndices[i];r.worstCycleDeviationPct[i]=(q>=0&&q<r.cycleDeviationPct.length)?r.cycleDeviationPct[q]:0f;}
        r.bestCycleIndex=minIndex(r.cycleDeviationPct);
        r.stageSpreadPct=stageSpreadPct(r.stdPosition);
        r.worstStageIndex=maxIndex(r.stageSpreadPct);
        r.worstStagePerCycle=new int[r.cycleCount];
        r.worstStageDeviationPct=new float[r.cycleCount];
        computeWorstStagePerCycle(r);
        r.cycleTimeTrendPct=cycleTimeTrendPct(r.cycleTimesSec);
        r.summary=buildSummary(r);
        return r;
    }

    public static CompareResult compare(Result a, Result b) {
        int n=Math.min(a.meanPosition.length,b.meanPosition.length);
        float[] d=new float[n]; float sum=0,max=-1; int maxI=0;
        for(int i=0;i<n;i++){d[i]=Math.abs(a.meanPosition[i]-b.meanPosition[i]);sum+=d[i];if(d[i]>max){max=d[i];maxI=i;}}
        float meanDiff=n==0?0:sum/n*100f;
        float speedScale=Math.max(.001f,Math.max(maxAbs(a.meanSpeed),maxAbs(b.meanSpeed)));
        float speedSum=0; int sn=Math.min(a.meanSpeed.length,b.meanSpeed.length);
        for(int i=0;i<sn;i++)speedSum+=Math.abs(a.meanSpeed[i]-b.meanSpeed[i]);
        float speedDiff=sn==0?0:(speedSum/sn)/speedScale*100f;
        int radius=10; int start=Math.max(0,maxI-radius),end=Math.min(100,maxI+radius);
        String level=(!a.reliable||!b.reliable)?"Cycle 부족 · 분석 보류":meanDiff<8f?"거의 동일":meanDiff<15f?"약간 다름":meanDiff<25f?"차이 큼":"큰 차이 · 확인 필요";
        StringBuilder s=new StringBuilder();
        s.append("사이클 반복 재현성 A/B 비교 · ").append(level).append("\n");
        s.append(String.format(Locale.getDefault(),"A %d Cycle · 재현성 Score %.0f/100 (100=안정) · Time CV %.1f%%\n",a.cycleCount,a.repeatabilityScore,a.cycleTimeCvPct));
        s.append(String.format(Locale.getDefault(),"B %d Cycle · 재현성 Score %.0f/100 (100=안정) · Time CV %.1f%%\n",b.cycleCount,b.repeatabilityScore,b.cycleTimeCvPct));
        s.append(String.format(Locale.getDefault(),"A↔B 평균 궤적 차이 %.1f%% · 속도패턴 차이 %.1f%% · 최대 차이 구간 %d~%d%%\n",meanDiff,speedDiff,start,end));
        if(b.reliable&&a.reliable){
            if(b.repeatabilityScore+8<a.repeatabilityScore) s.append("확인 필요: B의 반복 궤적 퍼짐이 A보다 큽니다.\n");
            if(b.cycleTimeCvPct>a.cycleTimeCvPct+2.0f) s.append("확인 필요: B의 Cycle Time 변동이 A보다 큽니다.\n");
        }
        s.append("\nCycle Intelligence\n");
        s.append(worstCycleLine(b));
        s.append(String.format(Locale.getDefault(),"문제 집중 구간: %s · 구간 퍼짐 %.1f%%\n",stageName(b.worstStageIndex),safeAt(b.stageSpreadPct,b.worstStageIndex)));
        s.append(String.format(Locale.getDefault(),"Cycle Time Trend: 첫→마지막 상대 변화 %+.1f%%\n",b.cycleTimeTrendPct));
        s.append("※ 각 Cycle을 0~100%로 시간 정규화한 영상 기반 상대 비교입니다. 실제 mm/속도/NG 한계는 별도 검증이 필요합니다.");
        return new CompareResult(draw(a,b,meanDiff,speedDiff,start,end),s.toString(),meanDiff,start,end,speedDiff);
    }

    private static Trace extractTrace(Context c,Uri uri,long duration,boolean fastMode) throws Exception {
        // v1.8 Fast Engine: on Android 9+ try batch frame decoding first.
        // This avoids dozens of independent random seeks, which was the main cause of long inspection time.
        if(fastMode && Build.VERSION.SDK_INT>=28){
            try{
                Trace batch=extractTraceBatch(c,uri,duration,true);
                if(batch!=null && batch.x!=null && batch.x.length>=20) return batch;
            }catch(Throwable ignored){
                // Device/codec dependent. Fall back to the stable retriever path below.
            }
        }
        return extractTraceRetriever(c,uri,duration,fastMode);
    }

    private static Trace extractTraceBatch(Context c,Uri uri,long duration,boolean fastMode) throws Exception {
        long started=android.os.SystemClock.elapsedRealtime();
        MediaMetadataRetriever r=new MediaMetadataRetriever();
        r.setDataSource(c,uri);
        try{
            int fps=30;
            try{String q=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);if(q!=null)fps=Math.max(1,Math.round(Float.parseFloat(q)));}catch(Exception ignored){}
            int frameCount=0;
            try{String q=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT);if(q!=null)frameCount=Integer.parseInt(q);}catch(Exception ignored){}
            // Batch decoding is ideal for the short 4~10 s cutter clips used here. For very long clips,
            // random sampled retrieval uses less memory/decoding work.
            if(frameCount<24 || frameCount>420) return null;

            int cap=fastMode?92:145;
            int floor=fastMode?61:81;
            int sampleFps=fastMode?18:28;
            int desired=Math.max(floor,Math.min(cap,(int)Math.ceil(duration/1000.0*Math.min(fps,sampleFps))+1));
            desired=Math.max(12,Math.min(desired,frameCount));
            int[] sampleIndex=new int[desired];
            for(int i=0;i<desired;i++) sampleIndex[i]=Math.round((frameCount-1)*i/(float)Math.max(1,desired-1));

            int w=fastMode?168:208,h=fastMode?96:118;
            ArrayList<Float> xs=new ArrayList<>(), es=new ArrayList<>();
            ArrayList<Long> times=new ArrayList<>();
            Gray prev=null;
            int target=0;
            final int chunkSize=12;
            for(int chunkStart=0;chunkStart<frameCount && target<desired;chunkStart+=chunkSize){
                int count=Math.min(chunkSize,frameCount-chunkStart);
                List<Bitmap> frames=r.getFramesAtIndex(chunkStart,count);
                if(frames==null || frames.isEmpty()) throw new Exception("batch frame decode failed");
                while(target<desired && sampleIndex[target]<chunkStart+count){
                    int idx=sampleIndex[target];
                    if(idx<chunkStart){target++;continue;}
                    int local=idx-chunkStart;
                    if(local>=0 && local<frames.size()){
                        Bitmap raw=frames.get(local);
                        if(raw!=null){
                            Bitmap small=(raw.getWidth()==w && raw.getHeight()==h)?raw:Bitmap.createScaledBitmap(raw,w,h,true);
                            Gray cur=gray(small);
                            if(small!=raw) small.recycle();
                            if(prev!=null){
                                Shift sh=globalShift(prev,cur);
                                float[] xe=cutterX(prev,cur,sh.dx,sh.dy);
                                xs.add(xe[0]); es.add(xe[1]);
                                times.add(Math.round(duration*idx/(double)Math.max(1,frameCount-1)));
                            }
                            prev=cur;
                        }
                    }
                    target++;
                }
                for(Bitmap b:frames) if(b!=null && !b.isRecycled()) b.recycle();
            }
            if(xs.size()<20) return null;
            float[] x=new float[xs.size()],e=new float[es.size()];long[] tm=new long[times.size()];
            for(int i=0;i<x.length;i++){x[i]=xs.get(i);e[i]=es.get(i);tm[i]=times.get(i);}
            fillMissingTimes(tm,duration);
            float med=median(e); for(int i=0;i<x.length;i++)if(e[i]<Math.max(0.6f,med*.42f))x[i]=Float.NaN;
            fillMissing(x);x=smooth(x,2);normalize01(x);
            Trace t=new Trace();t.x=x;t.energy=smooth(e,1);t.timeMs=tm;t.fps=fps;
            t.engineName="Batch Frame Scan";t.sampleCount=x.length+1;t.extractSec=(android.os.SystemClock.elapsedRealtime()-started)/1000.0;
            return t;
        } finally {try{r.release();}catch(Exception ignored){}}
    }

    private static Trace extractTraceRetriever(Context c,Uri uri,long duration,boolean fastMode) throws Exception {
        long started=android.os.SystemClock.elapsedRealtime();
        MediaMetadataRetriever r=new MediaMetadataRetriever(); r.setDataSource(c,uri);
        int fps=30; try{String q=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);if(q!=null)fps=Math.max(1,Math.round(Float.parseFloat(q)));}catch(Exception ignored){}
        int cap=fastMode?82:145;
        int floor=fastMode?55:81;
        int sampleFps=fastMode?16:28;
        int n=Math.max(floor,Math.min(cap,(int)Math.ceil(duration/1000.0*Math.min(fps,sampleFps))+1));
        int w=fastMode?168:208,h=fastMode?96:118; float[] x=new float[n-1],e=new float[n-1]; long[] tm=new long[n-1];
        Gray prev=null;
        try{
            for(int i=0;i<n;i++){
                long ms=Math.round(duration*i/(double)(n-1));
                Bitmap raw=null;
                if(Build.VERSION.SDK_INT>=27){
                    try{raw=r.getScaledFrameAtTime(ms*1000L,MediaMetadataRetriever.OPTION_CLOSEST,w,h);}catch(Exception ignored){}
                }
                if(raw==null) raw=r.getFrameAtTime(ms*1000L,MediaMetadataRetriever.OPTION_CLOSEST);
                if(raw==null)raw=r.getFrameAtTime(ms*1000L,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if(raw==null)continue;
                Bitmap small=(raw.getWidth()==w&&raw.getHeight()==h)?raw:Bitmap.createScaledBitmap(raw,w,h,true);
                Gray cur=gray(small);
                if(small!=raw)small.recycle();
                if(!raw.isRecycled())raw.recycle();
                if(prev!=null){Shift sh=globalShift(prev,cur);float[] xe=cutterX(prev,cur,sh.dx,sh.dy);x[i-1]=xe[0];e[i-1]=xe[1];tm[i-1]=ms;}
                prev=cur;
            }
        } finally {try{r.release();}catch(Exception ignored){}}
        fillMissingTimes(tm,duration);
        float med=median(e); for(int i=0;i<x.length;i++)if(e[i]<Math.max(0.6f,med*.42f))x[i]=Float.NaN;
        fillMissing(x); x=smooth(x,2); normalize01(x);
        Trace t=new Trace();t.x=x;t.energy=smooth(e,1);t.timeMs=tm;t.fps=fps;
        t.engineName=fastMode?"Scaled Random Scan (fallback)":"Detailed Frame Scan";t.sampleCount=n;t.extractSec=(android.os.SystemClock.elapsedRealtime()-started)/1000.0;
        return t;
    }

    private static Gray gray(Bitmap b){int w=b.getWidth(),h=b.getHeight();Gray g=new Gray(w,h);int[]px=new int[w*h];b.getPixels(px,0,w,0,0,w,h);for(int i=0;i<px.length;i++){int z=px[i];g.p[i]=(byte)((Color.red(z)*30+Color.green(z)*59+Color.blue(z)*11)/100);}return g;}
    private static int u(byte b){return b&255;}
    private static Shift globalShift(Gray a,Gray b){int bx=0,by=0;float best=Float.MAX_VALUE;for(int dy=-3;dy<=3;dy++)for(int dx=-3;dx<=3;dx++){float s=0;s+=sad(a,b,8,8,54,28,dx,dy);s+=sad(a,b,a.w-62,8,54,28,dx,dy);s+=sad(a,b,8,a.h-36,54,28,dx,dy);s+=sad(a,b,a.w-62,a.h-36,54,28,dx,dy);if(s<best){best=s;bx=dx;by=dy;}}return new Shift(bx,by);}
    private static float sad(Gray a,Gray b,int x0,int y0,int ww,int hh,int dx,int dy){float s=0;int n=0;for(int y=y0;y<y0+hh;y+=3)for(int x=x0;x<x0+ww;x+=3){int xx=x+dx,yy=y+dy;if(xx<0||yy<0||xx>=b.w||yy>=b.h)continue;s+=Math.abs(u(a.p[y*a.w+x])-u(b.p[yy*b.w+xx]));n++;}return n==0?99999:s/n;}
    private static float[] cutterX(Gray a,Gray b,int dx,int dy){int x0=(int)(a.w*.16),x1=(int)(a.w*.87),y0=(int)(a.h*.16),y1=(int)(a.h*.86);float wx=0,ws=0,energy=0;int n=0;for(int y=y0;y<y1;y+=2)for(int x=x0;x<x1;x+=2){int xx=x+dx,yy=y+dy;if(xx<0||yy<0||xx>=b.w||yy>=b.h)continue;int d=Math.abs(u(a.p[y*a.w+x])-u(b.p[yy*b.w+xx]));if(d>9){float q=d-8;wx+=x*q;ws+=q;energy+=q;}n++;}float pos=ws>0?((wx/ws)-x0)/Math.max(1f,(x1-x0)):Float.NaN;return new float[]{pos,n==0?0:energy/n};}

    private static List<Segment> findCycles(float[] x,float[] energy){
        ArrayList<Integer> mins=extrema(x,true),maxs=extrema(x,false);
        List<Segment> a=segmentsFromExtrema(x,mins,true),b=segmentsFromExtrema(x,maxs,false);
        if(a.size()>=b.size()&&a.size()>0)return a; if(b.size()>0)return b;
        // Fallback: use strong motion peaks as cycle centers and midpoint boundaries.
        ArrayList<Integer> peaks=new ArrayList<>();float th=median(energy)*1.35f;int minSep=Math.max(8,energy.length/12);
        for(int i=2;i<energy.length-2;i++)if(energy[i]>th&&energy[i]>=energy[i-1]&&energy[i]>=energy[i+1]){if(peaks.isEmpty()||i-peaks.get(peaks.size()-1)>=minSep)peaks.add(i);else if(energy[i]>energy[peaks.get(peaks.size()-1)])peaks.set(peaks.size()-1,i);}
        ArrayList<Segment> out=new ArrayList<>();for(int i=0;i<peaks.size()-1;i++){int s=i==0?Math.max(0,peaks.get(i)-(peaks.get(i+1)-peaks.get(i))/2):(peaks.get(i-1)+peaks.get(i))/2;int e=(peaks.get(i)+peaks.get(i+1))/2;if(e-s>=6)out.add(new Segment(s,e,false));}return out;
    }
    private static ArrayList<Integer> extrema(float[] x,boolean min){ArrayList<Integer> ids=new ArrayList<>();float lo=min(x),hi=max(x),range=Math.max(.001f,hi-lo);int sep=Math.max(7,x.length/15);for(int i=3;i<x.length-3;i++){boolean ok=true;for(int k=1;k<=3;k++){if(min&&(x[i]>x[i-k]||x[i]>x[i+k]))ok=false;if(!min&&(x[i]<x[i-k]||x[i]<x[i+k]))ok=false;}if(!ok)continue;if(min&&x[i]>lo+range*.45f)continue;if(!min&&x[i]<hi-range*.45f)continue;if(ids.isEmpty()||i-ids.get(ids.size()-1)>=sep)ids.add(i);else{int q=ids.get(ids.size()-1);if((min&&x[i]<x[q])||(!min&&x[i]>x[q]))ids.set(ids.size()-1,i);}}return ids;}
    private static List<Segment> segmentsFromExtrema(float[] x,List<Integer> ids,boolean minStart){ArrayList<Segment> out=new ArrayList<>();float range=Math.max(.001f,max(x)-min(x));for(int i=0;i<ids.size()-1;i++){int s=ids.get(i),e=ids.get(i+1);if(e-s<6)continue;float opposite=minStart?max(x,s,e):min(x,s,e);float base=(x[s]+x[e])*.5f;boolean good=minStart?(opposite-base>range*.35f):(base-opposite>range*.35f);if(good)out.add(new Segment(s,e,!minStart));}return out;}

    private static float[] resample(float[] src,int s,int e,int n){float[]o=new float[n];for(int i=0;i<n;i++){float q=s+(e-s)*i/(float)(n-1);int q0=(int)Math.floor(q),q1=Math.min(e,q0+1);float f=q-q0;o[i]=src[q0]*(1-f)+src[q1]*f;}return o;}
    private static void normalizeCycle(float[] c,boolean invert){float lo=min(c),hi=max(c),r=Math.max(.001f,hi-lo);for(int i=0;i<c.length;i++){c[i]=(c[i]-lo)/r;if(invert)c[i]=1f-c[i];}}
    private static float[] meanCurve(List<float[]> cs,int n){float[]m=new float[n];if(cs==null||cs.isEmpty())return m;for(float[]c:cs)for(int i=0;i<n;i++)m[i]+=c[i];for(int i=0;i<n;i++)m[i]/=cs.size();return m;}
    private static float[] stdCurve(List<float[]> cs,float[]m){float[]s=new float[m.length];if(cs==null||cs.size()<2)return s;for(float[]c:cs)for(int i=0;i<m.length;i++){float d=c[i]-m[i];s[i]+=d*d;}for(int i=0;i<s.length;i++)s[i]=(float)Math.sqrt(s[i]/cs.size());return s;}
    private static float[] speed(float[]p){float[]v=new float[p.length];for(int i=1;i<p.length;i++)v[i]=(p[i]-p[i-1])*100f;return smooth(v,2);}
    private static float mean(float[]a){if(a==null||a.length==0)return 0;float s=0;for(float v:a)s+=v;return s/a.length;}
    private static float cvPct(float[]a){if(a==null||a.length<2)return 0;float m=mean(a);if(m<=0)return 0;float s=0;for(float v:a){float d=v-m;s+=d*d;}return (float)Math.sqrt(s/a.length)/m*100f;}
    private static float median(float[]a){if(a.length==0)return 0;float[]b=a.clone();Arrays.sort(b);return b[b.length/2];}
    private static float min(float[]a){float m=Float.MAX_VALUE;for(float v:a)if(!Float.isNaN(v))m=Math.min(m,v);return m==Float.MAX_VALUE?0:m;}
    private static float max(float[]a){float m=-Float.MAX_VALUE;for(float v:a)if(!Float.isNaN(v))m=Math.max(m,v);return m==-Float.MAX_VALUE?0:m;}
    private static float maxAbs(float[]a){float m=0;for(float v:a)m=Math.max(m,Math.abs(v));return m;}
    private static float min(float[]a,int s,int e){float m=Float.MAX_VALUE;for(int i=s;i<=e;i++)m=Math.min(m,a[i]);return m;}
    private static float max(float[]a,int s,int e){float m=-Float.MAX_VALUE;for(int i=s;i<=e;i++)m=Math.max(m,a[i]);return m;}
    private static float[] smooth(float[]a,int radius){float[]o=new float[a.length];for(int i=0;i<a.length;i++){float s=0;int n=0;for(int k=-radius;k<=radius;k++){int q=i+k;if(q>=0&&q<a.length&&!Float.isNaN(a[q])){s+=a[q];n++;}}o[i]=n==0?a[i]:s/n;}return o;}
    private static void fillMissing(float[]a){int first=-1;for(int i=0;i<a.length;i++)if(!Float.isNaN(a[i])){first=i;break;}if(first<0){Arrays.fill(a,.5f);return;}for(int i=0;i<first;i++)a[i]=a[first];int last=first;for(int i=first+1;i<a.length;i++)if(!Float.isNaN(a[i])){if(i-last>1){for(int k=last+1;k<i;k++){float f=(k-last)/(float)(i-last);a[k]=a[last]*(1-f)+a[i]*f;}}last=i;}for(int i=last+1;i<a.length;i++)a[i]=a[last];}
    private static void fillMissingTimes(long[]t,long duration){for(int i=0;i<t.length;i++)if(t[i]==0)t[i]=Math.round(duration*(i+1)/(double)t.length);}
    private static void normalize01(float[]a){float lo=min(a),hi=max(a),r=Math.max(.001f,hi-lo);for(int i=0;i<a.length;i++)a[i]=(a[i]-lo)/r;}

    private static String buildSummary(Result r){
        StringBuilder s=new StringBuilder();
        s.append(r.label).append(" · Cycle 반복 재현성\n");
        if(!r.reliable){
            s.append("검출 Cycle ").append(r.cycleCount).append("개 · 반복성 계산에 Cycle이 부족합니다. 촬영 시간을 늘리거나 커터가 여러 번 왕복하도록 촬영해 주세요.\n");
        }else{
            s.append(String.format(Locale.getDefault(),"검출 Cycle %d개 · 재현성 Score %.0f/100 (100=안정) · 평균 Cycle Time %.3fs · Time CV %.1f%%\n",r.cycleCount,r.repeatabilityScore,r.meanCycleSec,r.cycleTimeCvPct));
            s.append(String.format(Locale.getDefault(),"Fast Engine: %s · 샘플 %d · Trace %.1fs\n",r.engineName,r.sampledFrameCount,r.traceExtractSec));
            s.append("Cycle Time: ");
            for(int i=0;i<r.cycleTimesSec.length&&i<10;i++){if(i>0)s.append(" / ");s.append(String.format(Locale.getDefault(),"%.3fs",r.cycleTimesSec[i]));}
            s.append("\n");
            s.append(worstCycleLine(r));
            if(r.excludedCycleCount>0)s.append("Cycle 경계 검증으로 불완전/시간 이상 Cycle ").append(r.excludedCycleCount).append("개 제외\n");
            s.append(String.format(Locale.getDefault(),"문제 집중 구간: %s · 구간 퍼짐 %.1f%% · Cycle Time Trend %+.1f%%\n",stageName(r.worstStageIndex),safeAt(r.stageSpreadPct,r.worstStageIndex),r.cycleTimeTrendPct));
        }
        s.append("※ 커터의 좌→우→좌 반복 패턴을 자동 분리해 각 Cycle을 0~100%로 정규화합니다.");
        return s.toString();
    }


    private static float[] toFloatArray(List<Float> list){float[] a=new float[list.size()];for(int i=0;i<list.size();i++)a[i]=list.get(i);return a;}
    private static int minIndex(float[] a){if(a==null||a.length==0)return -1;int bi=0;for(int i=1;i<a.length;i++)if(a[i]<a[bi])bi=i;return bi;}
    private static void computeWorstStagePerCycle(Result r){
        int[][] ranges={{0,25},{25,45},{45,55},{55,80},{80,100}};
        if(r.cycles==null||r.meanPosition==null)return;
        for(int c=0;c<r.cycles.size();c++){
            float[] cy=r.cycles.get(c);float best=-1;int bestStage=0;
            for(int st=0;st<ranges.length;st++){
                float sum=0;int n=0;
                for(int i=ranges[st][0];i<=ranges[st][1]&&i<cy.length&&i<r.meanPosition.length;i++){sum+=Math.abs(cy[i]-r.meanPosition[i]);n++;}
                float v=n==0?0:(sum/n)*100f;
                if(v>best){best=v;bestStage=st;}
            }
            r.worstStagePerCycle[c]=bestStage;r.worstStageDeviationPct[c]=Math.max(0,best);
        }
    }
    public static String stageNameForIndex(int i){return stageName(i);}

    private static float[] cycleDeviationPct(List<float[]> cycles,float[] mean){
        if(cycles==null)return new float[0];
        float[] out=new float[cycles.size()];
        for(int c=0;c<cycles.size();c++){float[] x=cycles.get(c);float sum=0;int n=Math.min(x.length,mean.length);for(int i=0;i<n;i++)sum+=Math.abs(x[i]-mean[i]);out[c]=n==0?0:(sum/n)*100f;}
        return out;
    }
    private static int[] topIndices(float[] a,int count){
        int n=Math.min(count,a==null?0:a.length);int[] out=new int[n];Arrays.fill(out,-1);boolean[] used=new boolean[a.length];
        for(int k=0;k<n;k++){float best=-1;int bi=-1;for(int i=0;i<a.length;i++)if(!used[i]&&a[i]>best){best=a[i];bi=i;}if(bi>=0){out[k]=bi;used[bi]=true;}}
        return out;
    }
    private static float[] stageSpreadPct(float[] std){
        int[][] ranges={{0,25},{25,45},{45,55},{55,80},{80,100}};float[] out=new float[ranges.length];
        for(int r=0;r<ranges.length;r++){float sum=0;int n=0;for(int i=ranges[r][0];i<=ranges[r][1]&&i<std.length;i++){sum+=std[i];n++;}out[r]=(n==0?0:sum/n)*100f;}
        return out;
    }
    private static int maxIndex(float[] a){if(a==null||a.length==0)return 0;int bi=0;for(int i=1;i<a.length;i++)if(a[i]>a[bi])bi=i;return bi;}
    private static float cycleTimeTrendPct(float[] t){if(t==null||t.length<2)return 0;float m=mean(t);if(m<=0)return 0;return (t[t.length-1]-t[0])/m*100f;}
    private static float safeAt(float[] a,int i){return a!=null&&i>=0&&i<a.length?a[i]:0f;}
    private static String stageName(int i){String[] n={"대기/초기 0~25%","전진가속 25~45%","커팅/충격 45~55%","복귀가속 55~80%","안정화 80~100%"};return i>=0&&i<n.length?n[i]:n[0];}
    private static String worstCycleLine(Result r){
        StringBuilder s=new StringBuilder("Worst Cycle TOP3: ");
        if(r.worstCycleIndices==null||r.worstCycleIndices.length==0)return s.append("Cycle 부족\n").toString();
        for(int k=0;k<r.worstCycleIndices.length;k++){if(k>0)s.append(" · ");int idx=r.worstCycleIndices[k];s.append(k==0?"🥇 ":k==1?"🥈 ":"🥉 ").append("Cycle ").append(idx+1).append(String.format(Locale.getDefault()," (%.1f%%)",safeAt(r.worstCycleDeviationPct,k)));}
        return s.append("\n").toString();
    }

    private static Bitmap draw(Result a,Result b,float diff,float speedDiff,int worstStart,int worstEnd){
        int w=1200,h=2080;Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(out);c.drawColor(Color.WHITE);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.rgb(15,48,88));p.setTextSize(38);p.setFakeBoldText(true);c.drawText(LanguageManager.ts("v1.8 Fast Engine · Cycle Diagnosis"),45,55,p);p.setFakeBoldText(false);p.setTextSize(22);p.setColor(Color.DKGRAY);c.drawText(LanguageManager.ts("A/B 병렬 추출 + Batch Frame Scan + Worst Cycle TOP3 + 문제 동작구간"),45,92,p);
        drawOverlayPanel(c,p,a,60,135,1140,395,"A 기준영상 · Cycle 1~10 내부 재현성",Color.rgb(30,100,220));
        drawOverlayPanel(c,p,b,60,430,1140,690,"B 비교영상 · Cycle 1~10 내부 재현성",Color.rgb(220,75,50));
        drawComparePanel(c,p,a,b,60,725,1140,975,worstStart,worstEnd);
        drawSpeedPanel(c,p,a,b,60,1010,1140,1230,worstStart,worstEnd);
        drawHeatmapPanel(c,p,b,60,1265,1140,1515);
        drawRankingPanel(c,p,b,60,1550,1140,1795);
        drawCycleTimePanel(c,p,a,b,60,1830,1140,2010);
        p.setColor(Color.DKGRAY);p.setTextSize(18);c.drawText(LanguageManager.ts("※ 영상 기반 상대지표입니다. 센서 실측/치수/NG 한계값은 별도 검증이 필요합니다."),65,2050,p);
        return out;
    }

    private static void drawHeatmapPanel(Canvas c,Paint p,Result r,int l,int t,int rr,int bot){
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.LTGRAY);c.drawRect(l,t,rr,bot,p);
        p.setStyle(Paint.Style.FILL);p.setTextSize(24);p.setFakeBoldText(true);p.setColor(Color.DKGRAY);c.drawText(LanguageManager.ts("B Cycle 편차 Heatmap · 진할수록 평균 궤적에서 멀어짐"),l+10,t+30,p);p.setFakeBoldText(false);
        int top=t+48,bottom=bot-28;int rows=Math.max(1,r.cycles.size());float cellH=(bottom-top)/(float)rows;
        for(int row=0;row<r.cycles.size();row++){float[] cy=r.cycles.get(row);for(int i=0;i<cy.length;i++){float dev=Math.abs(cy[i]-r.meanPosition[i]);float q=Math.min(1f,dev/.22f);int red=(int)(245*q+30*(1-q));int green=(int)(205*(1-q)+55);int blue=(int)(65*(1-q)+35);p.setColor(Color.rgb(Math.min(255,red),Math.min(255,green),Math.min(255,blue)));float x1=l+(rr-l)*i/(float)cy.length,x2=l+(rr-l)*(i+1)/(float)cy.length,y1=top+row*cellH,y2=top+(row+1)*cellH;c.drawRect(x1,y1,x2,y2,p);}
            p.setColor(Color.DKGRAY);p.setTextSize(15);c.drawText("C"+(row+1),l+3,top+(row+.75f)*cellH,p);}
        p.setTextSize(16);p.setColor(Color.GRAY);c.drawText(LanguageManager.ts("0%"),l,bot-7,p);c.drawText(LanguageManager.ts("50%"),(l+rr)/2-18,bot-7,p);c.drawText(LanguageManager.ts("100%"),rr-45,bot-7,p);
    }

    private static void drawRankingPanel(Canvas c,Paint p,Result r,int l,int t,int rr,int bot){
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.LTGRAY);c.drawRect(l,t,rr,bot,p);p.setStyle(Paint.Style.FILL);
        p.setTextSize(24);p.setFakeBoldText(true);p.setColor(Color.DKGRAY);c.drawText(LanguageManager.ts("Worst Cycle TOP3 + 구간별 반복성"),l+10,t+30,p);p.setFakeBoldText(false);
        int y=t+68;p.setTextSize(22);for(int k=0;k<r.worstCycleIndices.length;k++){int idx=r.worstCycleIndices[k];String rank=LanguageManager.ts(k==0?"1위":k==1?"2위":"3위");p.setColor(k==0?Color.rgb(190,80,30):Color.DKGRAY);String stage=(r.worstStagePerCycle!=null&&idx>=0&&idx<r.worstStagePerCycle.length)?stageName(r.worstStagePerCycle[idx]):"구간 확인";c.drawText(String.format(Locale.getDefault(),"%s · Cycle %d · %.1f%% · %s",rank,idx+1,safeAt(r.worstCycleDeviationPct,k),stage),l+20,y,p);y+=34;}
        y+=8;String[] names={LanguageManager.ts("대기/초기"),LanguageManager.ts("전진가속"),LanguageManager.ts("커팅/충격"),LanguageManager.ts("복귀가속"),LanguageManager.ts("안정화")};float max=Math.max(.001f,max(r.stageSpreadPct));for(int i=0;i<names.length;i++){float val=safeAt(r.stageSpreadPct,i);float x2=l+200+(rr-l-240)*(val/max);p.setColor(i==r.worstStageIndex?Color.rgb(235,145,30):Color.rgb(80,130,190));c.drawRect(l+200,y-17,x2,y+4,p);p.setColor(Color.DKGRAY);p.setTextSize(18);c.drawText(names[i],l+20,y,p);c.drawText(String.format(Locale.getDefault(),"%.1f%%",val),rr-75,y,p);y+=29;}
    }

    private static void drawCycleTimePanel(Canvas c,Paint p,Result a,Result b,int l,int t,int rr,int bot){
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.LTGRAY);c.drawRect(l,t,rr,bot,p);p.setStyle(Paint.Style.FILL);p.setTextSize(24);p.setFakeBoldText(true);p.setColor(Color.DKGRAY);c.drawText(LanguageManager.ts("Cycle Time Trend · A 파랑 / B 빨강"),l+10,t+30,p);p.setFakeBoldText(false);
        int top=t+48,bottom=bot-35;float maxT=Math.max(.001f,Math.max(max(a.cycleTimesSec),max(b.cycleTimesSec)));Paint pa=new Paint(Paint.ANTI_ALIAS_FLAG);pa.setStyle(Paint.Style.STROKE);pa.setStrokeWidth(5);pa.setColor(Color.rgb(30,100,220));Paint pb=new Paint(pa);pb.setColor(Color.rgb(220,75,50));drawTimeLine(c,a.cycleTimesSec,l,top,rr,bottom,maxT,pa);drawTimeLine(c,b.cycleTimesSec,l,top,rr,bottom,maxT,pb);p.setStyle(Paint.Style.FILL);p.setTextSize(18);p.setColor(Color.DKGRAY);c.drawText(String.format(Locale.getDefault(),"A Trend %+.1f%% · B Trend %+.1f%%",a.cycleTimeTrendPct,b.cycleTimeTrendPct),l+20,bot-8,p);
    }
    private static void drawTimeLine(Canvas c,float[] a,int l,int top,int rr,int bottom,float maxT,Paint p){if(a==null||a.length<2)return;for(int i=1;i<a.length;i++){float x1=l+(rr-l)*(i-1)/(float)Math.max(1,a.length-1),x2=l+(rr-l)*i/(float)Math.max(1,a.length-1);float y1=bottom-(bottom-top)*(a[i-1]/maxT),y2=bottom-(bottom-top)*(a[i]/maxT);c.drawLine(x1,y1,x2,y2,p);}}

    private static void drawOverlayPanel(Canvas c,Paint p,Result r,int l,int t,int rr,int b,String title,int color){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.LTGRAY);c.drawRect(l,t,rr,b,p);p.setStyle(Paint.Style.FILL);p.setTextSize(24);p.setFakeBoldText(true);p.setColor(Color.DKGRAY);c.drawText(LanguageManager.ts(title),l+10,t+30,p);p.setFakeBoldText(false);int top=t+48,bot=b-30;Paint q=new Paint(Paint.ANTI_ALIAS_FLAG);q.setStyle(Paint.Style.STROKE);q.setStrokeWidth(2.3f);q.setColor(color);q.setAlpha(105);for(float[]cy:r.cycles){for(int i=1;i<cy.length;i++){float x1=l+(rr-l)*(i-1)/(float)(cy.length-1),x2=l+(rr-l)*i/(float)(cy.length-1);float y1=bot-(bot-top)*cy[i-1],y2=bot-(bot-top)*cy[i];c.drawLine(x1,y1,x2,y2,q);}}q.setAlpha(255);q.setStrokeWidth(6);for(int i=1;i<r.meanPosition.length;i++){float x1=l+(rr-l)*(i-1)/(float)(r.meanPosition.length-1),x2=l+(rr-l)*i/(float)(r.meanPosition.length-1);float y1=bot-(bot-top)*r.meanPosition[i-1],y2=bot-(bot-top)*r.meanPosition[i];c.drawLine(x1,y1,x2,y2,q);}p.setTextSize(18);p.setColor(Color.GRAY);c.drawText(LanguageManager.ts("0%"),l,bot+22,p);c.drawText(LanguageManager.ts("50%"),(l+rr)/2-20,bot+22,p);c.drawText(LanguageManager.ts("100%"),rr-50,bot+22,p);}
    private static void drawComparePanel(Canvas c,Paint p,Result a,Result b,int l,int t,int rr,int bot,int ws,int we){p.setStyle(Paint.Style.STROKE);p.setColor(Color.LTGRAY);p.setStrokeWidth(2);c.drawRect(l,t,rr,bot,p);int top=t+50,bottom=bot-35;p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(35,255,200,0));float x1=l+(rr-l)*ws/100f,x2=l+(rr-l)*we/100f;c.drawRect(x1,top,x2,bottom,p);p.setTextSize(24);p.setFakeBoldText(true);p.setColor(Color.DKGRAY);c.drawText(LanguageManager.ts("A 평균 vs B 평균 · 노랑=최대 차이 구간"),l+10,t+30,p);p.setFakeBoldText(false);Paint pa=new Paint(Paint.ANTI_ALIAS_FLAG);pa.setStyle(Paint.Style.STROKE);pa.setStrokeWidth(6);pa.setColor(Color.rgb(30,100,220));Paint pb=new Paint(pa);pb.setColor(Color.rgb(220,75,50));for(int i=1;i<a.meanPosition.length;i++){float xa=l+(rr-l)*(i-1)/(float)(a.meanPosition.length-1),xb=l+(rr-l)*i/(float)(a.meanPosition.length-1);c.drawLine(xa,bottom-(bottom-top)*a.meanPosition[i-1],xb,bottom-(bottom-top)*a.meanPosition[i],pa);}for(int i=1;i<b.meanPosition.length;i++){float xa=l+(rr-l)*(i-1)/(float)(b.meanPosition.length-1),xb=l+(rr-l)*i/(float)(b.meanPosition.length-1);c.drawLine(xa,bottom-(bottom-top)*b.meanPosition[i-1],xb,bottom-(bottom-top)*b.meanPosition[i],pb);}p.setStyle(Paint.Style.FILL);p.setTextSize(21);p.setColor(Color.rgb(30,100,220));c.drawText(LanguageManager.ts("● A 기준 평균"),l+20,bot-8,p);p.setColor(Color.rgb(220,75,50));c.drawText(LanguageManager.ts("● B 비교 평균"),l+190,bot-8,p);}
    private static void drawSpeedPanel(Canvas c,Paint p,Result a,Result b,int l,int t,int rr,int bot,int ws,int we){
        p.setStyle(Paint.Style.STROKE);p.setColor(Color.LTGRAY);p.setStrokeWidth(2);c.drawRect(l,t,rr,bot,p);
        int top=t+48,bottom=bot-30,mid=(top+bottom)/2;
        p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(35,255,200,0));float hx1=l+(rr-l)*ws/100f,hx2=l+(rr-l)*we/100f;c.drawRect(hx1,top,hx2,bottom,p);
        p.setTextSize(24);p.setFakeBoldText(true);p.setColor(Color.DKGRAY);c.drawText(LanguageManager.ts("평균 속도 패턴 · A 파랑 / B 빨강"),l+10,t+30,p);p.setFakeBoldText(false);
        p.setColor(Color.LTGRAY);p.setStrokeWidth(2);c.drawLine(l,mid,rr,mid,p);
        float scale=Math.max(.001f,Math.max(maxAbs(a.meanSpeed),maxAbs(b.meanSpeed)));
        Paint pa=new Paint(Paint.ANTI_ALIAS_FLAG);pa.setStyle(Paint.Style.STROKE);pa.setStrokeWidth(5);pa.setColor(Color.rgb(30,100,220));Paint pb=new Paint(pa);pb.setColor(Color.rgb(220,75,50));
        for(int i=1;i<a.meanSpeed.length;i++){float x1=l+(rr-l)*(i-1)/(float)(a.meanSpeed.length-1),x2=l+(rr-l)*i/(float)(a.meanSpeed.length-1);float y1=mid-a.meanSpeed[i-1]/scale*(bottom-top)*.42f,y2=mid-a.meanSpeed[i]/scale*(bottom-top)*.42f;c.drawLine(x1,y1,x2,y2,pa);}
        for(int i=1;i<b.meanSpeed.length;i++){float x1=l+(rr-l)*(i-1)/(float)(b.meanSpeed.length-1),x2=l+(rr-l)*i/(float)(b.meanSpeed.length-1);float y1=mid-b.meanSpeed[i-1]/scale*(bottom-top)*.42f,y2=mid-b.meanSpeed[i]/scale*(bottom-top)*.42f;c.drawLine(x1,y1,x2,y2,pb);}
        p.setStyle(Paint.Style.FILL);p.setTextSize(18);p.setColor(Color.GRAY);c.drawText(LanguageManager.ts("전진/복귀 방향 변화와 급가속·급감속의 상대 패턴을 비교"),l+10,bot-7,p);
    }

}

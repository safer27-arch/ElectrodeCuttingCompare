package com.example.electrodecutcompare;

import java.util.*;

/** Pure-Java, deterministic measurement core. No learned defect/NG classifier. */
public final class MotionCore {
    private MotionCore() {}
    public static final String ENGINE="2.0.0-ncc-pts-v1";
    public static final double MIN_NCC=.72, MIN_MARGIN=.035;
    public static final class Gray {
        public final int w,h; public final byte[] p;
        public Gray(int w,int h,byte[] p){this.w=w;this.h=h;this.p=p;}
        public int at(int x,int y){return p[y*w+x]&255;}
    }
    public static final class Patch {
        final float[] z; final int[] ox,oy; public final int rx,ry; public final double texture;
        public Patch(Gray f,int cx,int cy,int rx,int ry){
            this.rx=rx;this.ry=ry;
            int step=Math.max(1,(int)Math.ceil(Math.max(rx,ry)/4.0)), n=((2*rx)/step+1)*((2*ry)/step+1);
            z=new float[n];ox=new int[n];oy=new int[n];int k=0;double sum=0,ss=0;
            for(int y=-ry;y<=ry;y+=step)for(int x=-rx;x<=rx;x+=step){
                if(cx+x<0||cx+x>=f.w||cy+y<0||cy+y>=f.h)throw new IllegalArgumentException("ROI outside image");
                int v=f.at(cx+x,cy+y);z[k]=v;ox[k]=x;oy[k++]=y;sum+=v;ss+=v*v;
            }
            double mean=sum/n;texture=Math.sqrt(Math.max(0,ss/n-mean*mean));
            for(int i=0;i<n;i++)z[i]=(float)((z[i]-mean)/Math.max(texture,1));
        }
        double score(Gray f,int cx,int cy){
            if(cx<rx||cy<ry||cx>=f.w-rx||cy>=f.h-ry)return -1;
            double sum=0,ss=0,dot=0;
            for(int i=0;i<z.length;i++){int v=f.at(cx+ox[i],cy+oy[i]);sum+=v;ss+=(double)v*v;dot+=z[i]*v;}
            double var=ss/z.length-Math.pow(sum/z.length,2);
            return texture<6||var<25?-1:dot/(z.length*Math.sqrt(var));
        }
    }
    public static final class Match {
        public int x,y; public double score=-1,margin; public boolean ok;
    }
    public static Match locate(Gray f,Patch p,int px,int py,int radius){
        int l=Math.max(p.rx,px-radius),r=Math.min(f.w-p.rx-1,px+radius);
        int t=Math.max(p.ry,py-radius),b=Math.min(f.h-p.ry-1,py+radius);
        Match m=new Match(); int width=Math.max(0,r-l+1),height=Math.max(0,b-t+1);
        double[] scores=new double[width*height];int k=0;
        // Unit-step search avoids skipping a true high-frequency template at odd pixel offsets.
        for(int y=t;y<=b;y++)for(int x=l;x<=r;x++){
            double score=p.score(f,x,y);scores[k++]=score;
            if(score>m.score){m.x=x;m.y=y;m.score=score;}
        }
        double other=-1;int sep=Math.max(5,Math.min(p.rx,p.ry));k=0;
        for(int y=t;y<=b;y++)for(int x=l;x<=r;x++){
            double score=scores[k++];if(Math.hypot(x-m.x,y-m.y)>sep)other=Math.max(other,score);
        }
        m.margin=m.score-other;m.ok=m.score>=MIN_NCC&&m.margin>=MIN_MARGIN;
        return m;
    }
    public static final class Sample {
        public double sec,decoderPtsSec,x,y,screenX,screenY,commonX,commonY,ncc;public boolean valid;
        public Sample(double t,double x,double y,boolean valid){sec=t;decoderPtsSec=t;this.x=x;this.y=y;this.valid=valid;}
    }
    public static final class Cycle {
        public int id,start,end,peak,rankPhase; public double seconds,amplitude,startOffset,shapeDiff,timeDeltaPct,internalDiff,maxPhase,rankValue;
        public boolean timeOutlier; public double[] curve;
    }
    public static final class Analysis {
        public String role="",qualityReason="",timeSource="decoder PTS",note="";
        public List<Sample> samples=new ArrayList<>();public List<Cycle> cycles=new ArrayList<>();
        public double[] pos=new double[0],time=new double[0],medianCurve=new double[101],band=new double[101];
        public double validFraction,sampleHz,samplesPerCycle,medianTime,timeCv,strokePx,commonRms,spreadPct,analyzedSec,axisX,axisY;
        public int width,height,invalidSegments,partialEnds;public boolean usable=false,truncated=false;
    }
    public static final class Comparison {
        public Analysis a,b;public boolean valid,usingA;public String basis;
        public double rawShapePct,dtwShapePct,cycleTimeDeltaPct,bandWidthPct;
        public List<Cycle> ranked=new ArrayList<>();
    }
    public static Analysis analyze(List<Sample> input, boolean reverse){
        Analysis a=new Analysis();a.samples=input;
        int n=input.size();a.pos=new double[n];a.time=new double[n];
        if(n<15){a.qualityReason="FEW_FRAMES";return a;}
        double mx=0,my=0;int nv=0;
        for(Sample s:input)if(s.valid){mx+=s.x;my+=s.y;nv++;}
        a.validFraction=nv/(double)n;
        if(nv<8){a.qualityReason="TRACK_LOST";return a;}
        mx/=nv;my/=nv;double xx=0,yy=0,xy=0;
        for(Sample s:input)if(s.valid){xx+=(s.x-mx)*(s.x-mx);yy+=(s.y-my)*(s.y-my);xy+=(s.x-mx)*(s.y-my);}
        double angle=.5*Math.atan2(2*xy,xx-yy),ux=Math.cos(angle),uy=Math.sin(angle);
        if(Math.abs(ux)>=Math.abs(uy)?ux<0:uy<0){ux=-ux;uy=-uy;}if(reverse){ux=-ux;uy=-uy;}
        a.axisX=ux;a.axisY=uy;
        double common=0;
        for(int i=0;i<n;i++){Sample s=input.get(i);a.pos[i]=s.valid?s.x*ux+s.y*uy:Double.NaN;a.time[i]=s.sec;common+=s.commonX*s.commonX+s.commonY*s.commonY;}
        a.commonRms=Math.sqrt(common/n);
        // Interpolate at most two invalid samples. Larger gaps are never fabricated.
        for(int i=0;i<n;){if(Double.isFinite(a.pos[i])){i++;continue;}int st=i;while(i<n&&!Double.isFinite(a.pos[i]))i++;
            if(st>0&&i<n&&i-st<=2)for(int k=st;k<i;k++){double q=(a.time[k]-a.time[st-1])/(a.time[i]-a.time[st-1]);a.pos[k]=a.pos[st-1]*(1-q)+a.pos[i]*q;}}
        double[] finite=Arrays.stream(a.pos).filter(Double::isFinite).toArray();
        double lo=quantile(finite,.05),hi=quantile(finite,.95);a.strokePx=hi-lo;
        double[] dt=new double[n-1];for(int i=1;i<n;i++)dt[i-1]=a.time[i]-a.time[i-1];a.sampleHz=1/Math.max(.000001,median(dt));
        a.analyzedSec=a.time[n-1]-a.time[0];
        if(a.strokePx<4){a.qualityReason="NO_RESOLVED_MOTION";return a;}
        for(int i=0;i<n;i++)a.pos[i]=(a.pos[i]-lo)/a.strokePx;
        // Hysteresis: low endpoint -> opposite endpoint -> low endpoint. No energy-peak fallback.
        int state=0,st=-1,peak=-1,ret=-1,id=0;
        double minValue=Double.POSITIVE_INFINITY,maxValue=-1;
        for(int i=0;i<n;i++){
            double v=a.pos[i];if(!Double.isFinite(v))continue;
            if(state==0){if(v<=.16){st=i;minValue=v;state=1;}}
            else if(state==1){if(v<minValue){st=i;minValue=v;}if(v>=.78){state=2;peak=i;maxValue=v;}}
            else if(state==2){if(v>maxValue){peak=i;maxValue=v;}if(v<=.16){state=3;ret=i;minValue=v;}}
            else if(state==3){if(v<minValue){ret=i;minValue=v;}if(v>=.26){
                addCycle(a,st,ret,peak,++id);st=ret;state=1;minValue=a.pos[st];if(v>=.78){state=2;peak=i;maxValue=v;}
            }}
        }
        if(state==3&&ret>st)addCycle(a,st,ret,peak,++id);
        a.partialEnds=(a.cycles.isEmpty()?1:(a.cycles.get(0).start>0?1:0)+(a.cycles.get(a.cycles.size()-1).end<n-2?1:0));
        if(a.cycles.isEmpty()){a.qualityReason="FEW_COMPLETE_CYCLES";return a;}
        double[] periods=new double[a.cycles.size()];double minSamples=Double.POSITIVE_INFINITY;
        for(int i=0;i<periods.length;i++){Cycle c=a.cycles.get(i);periods[i]=c.seconds;minSamples=Math.min(minSamples,c.end-c.start+1);}
        a.samplesPerCycle=minSamples;a.medianTime=median(periods);a.timeCv=cv(periods);
        double mad=mad(periods,a.medianTime);
        for(Cycle c:a.cycles)c.timeOutlier=Math.abs(c.seconds-a.medianTime)>Math.max(3*1.4826*mad,2/a.sampleHz);
        for(int j=0;j<=100;j++){double[] vals=new double[a.cycles.size()];for(int i=0;i<vals.length;i++)vals[i]=a.cycles.get(i).curve[j];a.medianCurve[j]=median(vals);a.band[j]=1.4826*mad(vals,a.medianCurve[j]);}
        a.spreadPct=mean(a.band)*100;
        for(Cycle c:a.cycles){c.internalDiff=rmsDiff(c.curve,a.medianCurve)*100;}
        if(a.cycles.size()<3)a.qualityReason="FEW_COMPLETE_CYCLES";
        else if(a.validFraction<.85)a.qualityReason="TRACK_LOST";
        else if(a.samplesPerCycle<12)a.qualityReason="UNDERSAMPLED";
        else if(a.strokePx<8)a.qualityReason="SMALL_MOTION";
        else {a.usable=true;a.qualityReason="MEASURABLE_NOT_NG";}
        return a;
    }
    private static void addCycle(Analysis a,int st,int en,int peak,int id){
        if(st<0||en<=st||peak<=st||peak>=en){a.invalidSegments++;return;}
        int bad=0,maxGap=0,gap=0;double lo=10,hi=-10;
        for(int i=st;i<=en;i++){if(!a.samples.get(i).valid){bad++;gap++;maxGap=Math.max(maxGap,gap);}else gap=0;
            if(!Double.isFinite(a.pos[i])){a.invalidSegments++;return;}lo=Math.min(lo,a.pos[i]);hi=Math.max(hi,a.pos[i]);}
        if(en-st<5||bad/(double)(en-st+1)>.1||maxGap>2||hi-lo<.6||Math.abs(a.pos[en]-a.pos[st])>.22){a.invalidSegments++;return;}
        Cycle c=new Cycle();c.id=id;c.start=st;c.end=en;c.peak=peak;c.seconds=a.time[en]-a.time[st];c.amplitude=hi-lo;c.startOffset=a.pos[st];
        c.curve=resample(a.time,a.pos,st,en,101);a.cycles.add(c);
    }
    public static Comparison compare(Analysis a,Analysis b){
        Comparison r=new Comparison();r.a=a;r.b=b;r.valid=a.usable&&b.usable;
        // Stable measured reference is required. This is not an automatic assertion of normality.
        r.usingA=a.usable&&a.spreadPct<=12;r.basis=r.usingA?"A_MEDIAN":"B_INTERNAL_MEDIAN";
        if(a.cycles.size()>0&&b.cycles.size()>0){r.rawShapePct=rmsDiff(a.medianCurve,b.medianCurve)*100;r.dtwShapePct=dtw(a.medianCurve,b.medianCurve,10)*100;r.cycleTimeDeltaPct=(b.medianTime/Math.max(.00001,a.medianTime)-1)*100;}
        if(!b.usable)return r;
        double[] ref=r.usingA?a.medianCurve:b.medianCurve;
        for(Cycle c:b.cycles){c.shapeDiff=rmsDiff(c.curve,ref)*100;c.timeDeltaPct=(c.seconds/Math.max(.00001,r.usingA?a.medianTime:b.medianTime)-1)*100;
            double max=-1;int index=0;for(int j=0;j<=100;j++){double d=Math.abs(c.curve[j]-ref[j]);if(d>max){max=d;index=j;}}
            c.maxPhase=index/100.0;c.rankPhase=phase(c,index);c.rankValue=c.shapeDiff;r.ranked.add(c);
        }
        r.ranked.sort((x,y)->Double.compare(y.rankValue,x.rankValue));return r;
    }
    public static int phase(Cycle c,int index){
        double lo=Arrays.stream(c.curve).min().orElse(0),hi=Arrays.stream(c.curve).max().orElse(1),v=(c.curve[index]-lo)/Math.max(.001,hi-lo);
        int peak=0;for(int i=1;i<101;i++)if(c.curve[i]>c.curve[peak])peak=i;
        if(v>=.9)return 2;if(v<=.12)return index<peak?0:4;return index<peak?1:3;
    }
    public static Cycle representative(Analysis a){Cycle best=null;for(Cycle c:a.cycles)if(best==null||c.internalDiff<best.internalDiff)best=c;return best;}
    public static double[] resample(double[] t,double[] v,int st,int en,int n){double[] out=new double[n];int j=st;for(int i=0;i<n;i++){double q=t[st]+(t[en]-t[st])*i/(n-1.0);while(j<en-1&&t[j+1]<q)j++;double f=(q-t[j])/Math.max(1e-9,t[j+1]-t[j]);out[i]=v[j]*(1-f)+v[j+1]*f;}return out;}
    public static double rmsDiff(double[] a,double[] b){double s=0;for(int i=0;i<a.length;i++){double d=a[i]-b[i];s+=d*d;}return Math.sqrt(s/a.length);}
    public static double dtw(double[] a,double[] b,int band){int n=a.length,m=b.length;double[][] d=new double[n+1][m+1];int[][] len=new int[n+1][m+1];for(double[] row:d)Arrays.fill(row,Double.POSITIVE_INFINITY);d[0][0]=0;
        for(int i=1;i<=n;i++)for(int j=Math.max(1,i-band);j<=Math.min(m,i+band);j++){int pi=i-1,pj=j-1;if(d[i-1][j]<d[pi][pj]){pi=i-1;pj=j;}if(d[i][j-1]<d[pi][pj]){pi=i;pj=j-1;}d[i][j]=Math.abs(a[i-1]-b[j-1])+d[pi][pj];len[i][j]=len[pi][pj]+1;}return d[n][m]/Math.max(1,len[n][m]);}
    public static double mean(double[] a){return a.length==0?0:Arrays.stream(a).sum()/a.length;}
    public static double cv(double[] a){double m=mean(a),s=0;for(double x:a)s+=(x-m)*(x-m);return m==0?0:Math.sqrt(s/Math.max(1,a.length))/m*100;}
    public static double median(double[] a){return quantile(a,.5);}
    public static double mad(double[] a,double center){double[] d=new double[a.length];for(int i=0;i<a.length;i++)d[i]=Math.abs(a[i]-center);return median(d);}
    public static double quantile(double[] a,double q){if(a.length==0)return 0;double[] s=a.clone();Arrays.sort(s);double p=(s.length-1)*q;int l=(int)p,h=Math.min(s.length-1,l+1);return s[l]+(s[h]-s[l])*(p-l);}
}

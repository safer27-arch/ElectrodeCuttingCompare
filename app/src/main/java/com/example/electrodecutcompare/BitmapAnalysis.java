package com.example.electrodecutcompare;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;

public final class BitmapAnalysis {
    private BitmapAnalysis() {}

    public static class Transform {
        public float angleDeg;
        public float scale;
        public float dx;
        public float dy;
        public double score;
        public Transform(float angleDeg, float scale, float dx, float dy, double score) {
            this.angleDeg = angleDeg; this.scale = scale; this.dx = dx; this.dy = dy; this.score = score;
        }
    }

    public static Bitmap fitMaxWidth(Bitmap src, int maxWidth) {
        if (src.getWidth() <= maxWidth) return src;
        float s = maxWidth / (float) src.getWidth();
        return Bitmap.createScaledBitmap(src, maxWidth, Math.max(1, Math.round(src.getHeight()*s)), true);
    }

    private static int gray(int c) {
        return (Color.red(c)*30 + Color.green(c)*59 + Color.blue(c)*11) / 100;
    }

    private static int edge(Bitmap b, int x, int y) {
        int xm = Math.max(0,x-1), xp=Math.min(b.getWidth()-1,x+1);
        int ym = Math.max(0,y-1), yp=Math.min(b.getHeight()-1,y+1);
        int gx = gray(b.getPixel(xp,y)) - gray(b.getPixel(xm,y));
        int gy = gray(b.getPixel(x,yp)) - gray(b.getPixel(x,ym));
        return Math.min(255, Math.abs(gx)+Math.abs(gy));
    }

    private static double score(Bitmap a, Bitmap b, float angleDeg, float scale, float dx, float dy, int stride) {
        int w=a.getWidth(), h=a.getHeight();
        double rad=Math.toRadians(angleDeg), cs=Math.cos(rad), sn=Math.sin(rad);
        float cx=(w-1)/2f, cy=(h-1)/2f;
        double sum=0; int n=0;
        int margin=Math.max(8, Math.round(Math.min(w,h)*0.08f));
        for (int y=margin; y<h-margin; y+=stride) {
            for (int x=margin; x<w-margin; x+=stride) {
                double tx=x-cx-dx, ty=y-cy-dy;
                double xb=( cs*tx + sn*ty)/scale + cx;
                double yb=(-sn*tx + cs*ty)/scale + cy;
                int ix=(int)Math.round(xb), iy=(int)Math.round(yb);
                if (ix<1 || iy<1 || ix>=w-1 || iy>=h-1) continue;
                int ea=edge(a,x,y), eb=edge(b,ix,iy);
                // Static high-contrast structure is more useful than flat background.
                if (ea<18 && eb<18) continue;
                sum += Math.abs(ea-eb);
                n++;
            }
        }
        return n==0 ? Double.MAX_VALUE : sum/n;
    }

    public static Transform estimate(Bitmap aFull, Bitmap bFull) {
        int target=240;
        Bitmap a=fitMaxWidth(aFull,target);
        Bitmap b=Bitmap.createScaledBitmap(bFull,a.getWidth(),a.getHeight(),true);
        float sx=aFull.getWidth()/(float)a.getWidth();
        Transform best=new Transform(0,1,0,0,Double.MAX_VALUE);

        float[] scales={0.96f,1.00f,1.04f};
        for(float s:scales) for(float ang=-4; ang<=4.001; ang+=1f)
            for(float dy=-20;dy<=20;dy+=4) for(float dx=-20;dx<=20;dx+=4) {
                double sc=score(a,b,ang,s,dx,dy,4);
                if(sc<best.score) best=new Transform(ang,s,dx,dy,sc);
            }

        Transform coarse=best;
        for(float s=coarse.scale-0.02f;s<=coarse.scale+0.0201f;s+=0.01f)
            for(float ang=coarse.angleDeg-0.75f;ang<=coarse.angleDeg+0.751f;ang+=0.25f)
                for(float dy=coarse.dy-4;dy<=coarse.dy+4.01;dy+=2)
                    for(float dx=coarse.dx-4;dx<=coarse.dx+4.01;dx+=2) {
                        double sc=score(a,b,ang,s,dx,dy,3);
                        if(sc<best.score) best=new Transform(ang,s,dx,dy,sc);
                    }

        best.dx*=sx; best.dy*=sx;
        return best;
    }

    public static Bitmap applyTransform(Bitmap src, int targetW, int targetH, Transform t) {
        Bitmap base=Bitmap.createScaledBitmap(src,targetW,targetH,true);
        Bitmap out=Bitmap.createBitmap(targetW,targetH,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(out); c.drawColor(Color.BLACK);
        Matrix m=new Matrix();
        float cx=(targetW-1)/2f, cy=(targetH-1)/2f;
        m.postScale(t.scale,t.scale,cx,cy);
        m.postRotate(t.angleDeg,cx,cy);
        m.postTranslate(t.dx,t.dy);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        c.drawBitmap(base,m,p);
        return out;
    }

    public static Bitmap differenceOverlay(Bitmap a, Bitmap alignedB) {
        int w=Math.min(a.getWidth(),alignedB.getWidth());
        int h=Math.min(a.getHeight(),alignedB.getHeight());
        Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        int[] pa=new int[w], pb=new int[w], po=new int[w];
        for(int y=0;y<h;y++) {
            a.getPixels(pa,0,w,0,y,w,1); alignedB.getPixels(pb,0,w,0,y,w,1);
            for(int x=0;x<w;x++) {
                int ca=pa[x], cb=pb[x];
                int d=(Math.abs(Color.red(ca)-Color.red(cb))+Math.abs(Color.green(ca)-Color.green(cb))+Math.abs(Color.blue(ca)-Color.blue(cb)))/3;
                int r=Color.red(ca), g=Color.green(ca), bl=Color.blue(ca);
                if(d>55) { r=Math.min(255,(int)(r*0.45+150)); g=(int)(g*0.45); bl=(int)(bl*0.45); }
                else { r=(int)(r*0.72); g=(int)(g*0.72); bl=(int)(bl*0.72); }
                po[x]=Color.rgb(r,g,bl);
            }
            out.setPixels(po,0,w,0,y,w,1);
        }
        return out;
    }
}

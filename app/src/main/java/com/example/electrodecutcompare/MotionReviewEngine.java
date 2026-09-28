package com.example.electrodecutcompare;

import android.content.Context;
import android.graphics.Rect;
import android.media.*;
import android.net.Uri;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Sequential decoding, real presentation timestamps, persistent target template and two fixed references. */
public final class MotionReviewEngine {
 public interface Progress {void onProgress(int percent);}
 private MotionReviewEngine(){}
 public static MotionCore.Analysis run(Context c,Uri uri,MotionRegionStore.Settings settings,int maxFps,AtomicBoolean cancel,Progress progress)throws Exception{
  if(!settings.complete())throw new IllegalArgumentException("ROI_NOT_CONFIGURED");
  MediaExtractor ex=new MediaExtractor();MediaCodec codec=null;
  ArrayList<MotionCore.Sample> samples=new ArrayList<>();
  Tracker tracker=null;long durationUs=0,lastProcessed=-1,firstPts=-1,lastOutput=-1;int rotation=0;
  final int maxFrames=30*maxFps;final double maxSec=30.0;
  boolean truncated=false;
  try{
   ex.setDataSource(c,uri,null);MediaFormat format=null;
   for(int i=0;i<ex.getTrackCount();i++){MediaFormat f=ex.getTrackFormat(i);String mime=f.getString(MediaFormat.KEY_MIME);if(mime!=null&&mime.startsWith("video/")){format=f;ex.selectTrack(i);break;}}
   if(format==null)throw new IllegalArgumentException("VIDEO_TRACK_MISSING");
   if(format.containsKey(MediaFormat.KEY_DURATION))durationUs=format.getLong(MediaFormat.KEY_DURATION);
   if(format.containsKey("rotation-degrees"))rotation=format.getInteger("rotation-degrees");
   // Decode unrotated CPU YUV and explicitly rotate the luma plane to the displayed coordinates.
   format.setInteger("rotation-degrees",0);
   format.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
   codec=MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));codec.configure(format,null,null,0);codec.start();
   boolean inEnd=false,outEnd=false;MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
   long idleStart=android.os.SystemClock.elapsedRealtime();
   long minGapUs=(long)(950000.0/Math.max(1,maxFps));
   while(!outEnd){
    if(cancel.get()||Thread.currentThread().isInterrupted())throw new InterruptedException("CANCELLED");
    boolean progressed=false;
    if(!inEnd){int ix=codec.dequeueInputBuffer(1000);if(ix>=0){ByteBuffer buffer=codec.getInputBuffer(ix);int size=ex.readSampleData(buffer,0);if(size<0){codec.queueInputBuffer(ix,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inEnd=true;}else{codec.queueInputBuffer(ix,0,size,ex.getSampleTime(),0);ex.advance();}progressed=true;}}
    int ox=codec.dequeueOutputBuffer(info,10000);
    if(ox>=0){
     progressed=true;long pts=info.presentationTimeUs;boolean eos=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
     try{
      if(info.size>0 && pts>=0 && pts>lastOutput){
       if(firstPts<0)firstPts=pts;lastOutput=pts;
       if((pts-firstPts)/1e6>maxSec||samples.size()>=maxFrames){truncated=true;outEnd=true;}
       else if(lastProcessed<0||pts-lastProcessed>=minGapUs){
        Image image=codec.getOutputImage(ox);
        if(image==null)throw new IllegalStateException("YUV_UNAVAILABLE");
        MotionCore.Gray gray;try{gray=luma(image,rotation,maxFps>30?384:320);}finally{image.close();}
        if(tracker==null)tracker=new Tracker(gray,settings);
        MotionCore.Sample measured=tracker.step(gray,(pts-firstPts)/1e6);measured.decoderPtsSec=pts/1e6;samples.add(measured);lastProcessed=pts;
        int pct=durationUs>0?(int)Math.min(98,100*(pts-firstPts)/(double)Math.min(durationUs,(long)(maxSec*1e6))):0;
        progress.onProgress(pct);
       }
      }
     }finally{codec.releaseOutputBuffer(ox,false);}
     if(eos)outEnd=true;
    }
    if(progressed)idleStart=android.os.SystemClock.elapsedRealtime();
    if(android.os.SystemClock.elapsedRealtime()-idleStart>15000)throw new IllegalStateException("DECODER_TIMEOUT");
   }
  }finally{
   if(codec!=null){try{codec.stop();}catch(Exception ignored){}codec.release();}ex.release();
  }
  MotionCore.Analysis a=MotionCore.analyze(samples,settings.reverse);a.truncated=truncated;
  if(tracker!=null){a.width=tracker.w;a.height=tracker.h;}
  a.timeSource="MediaCodec PTS; video-relative origin; raw PTS retained";
  progress.onProgress(100);return a;
 }
 static MotionCore.Gray luma(Image image,int degrees,int limit){
  Rect crop=image.getCropRect();int sw=crop.width(),sh=crop.height();
  int r=((degrees%360)+360)%360,dw=r==90||r==270?sh:sw,dh=r==90||r==270?sw:sh;
  double scale=Math.min(1,limit/(double)Math.max(dw,dh));int w=Math.max(32,(int)Math.round(dw*scale)),h=Math.max(32,(int)Math.round(dh*scale));
  Image.Plane yp=image.getPlanes()[0];ByteBuffer b=yp.getBuffer();int row=yp.getRowStride(),pixel=yp.getPixelStride(),offset=b.position();byte[] out=new byte[w*h];
  for(int y=0;y<h;y++)for(int x=0;x<w;x++){
   int u=Math.min(dw-1,(int)((x+.5)*dw/w)),v=Math.min(dh-1,(int)((y+.5)*dh/h));int sx,sy;
   if(r==90){sx=v;sy=sh-1-u;}else if(r==180){sx=sw-1-u;sy=sh-1-v;}else if(r==270){sx=sw-1-v;sy=u;}else{sx=u;sy=v;}
   int index=offset+(sy+crop.top)*row+(sx+crop.left)*pixel;
   if(index>=b.limit())throw new IllegalStateException("YUV_STRIDE");out[y*w+x]=b.get(index);
  }
  return new MotionCore.Gray(w,h,out);
 }
 static final class Tracker {
  final int w,h;final MotionCore.Patch[] seed=new MotionCore.Patch[3];final int[] ix=new int[3],iy=new int[3],px=new int[3],py=new int[3];
  MotionCore.Gray previous;boolean previousGood=true;int count;
  Tracker(MotionCore.Gray f,MotionRegionStore.Settings s){w=f.w;h=f.h;for(int i=0;i<3;i++){
   float[] b=s.boxes[i];ix[i]=px[i]=Math.round(b[0]*(w-1));iy[i]=py[i]=Math.round(b[1]*(h-1));
   int rx=Math.max(4,Math.round(b[2]*w)),ry=Math.max(4,Math.round(b[3]*h));
   seed[i]=new MotionCore.Patch(f,ix[i],iy[i],rx,ry);if(seed[i].texture<6)throw new IllegalArgumentException("LOW_TEXTURE_ROI_"+(i+1));
  }
  if(Math.hypot(ix[1]-ix[2],iy[1]-iy[2])<Math.min(w,h)*.25)throw new IllegalArgumentException("REFERENCES_TOO_CLOSE");previous=f;}
  MotionCore.Sample step(MotionCore.Gray f,double time){
   MotionCore.Sample s=new MotionCore.Sample(time,0,0,true);
   if(count++==0){s.x=ix[0];s.y=iy[0];s.screenX=ix[0]/(double)w;s.screenY=iy[0]/(double)h;s.ncc=1;return s;}
   MotionCore.Match[] m=new MotionCore.Match[3];
   for(int i=1;i<3;i++)m[i]=MotionCore.locate(f,seed[i],ix[i],iy[i],Math.max(8,(int)(w*.045)));
   m[0]=MotionCore.locate(f,seed[0],px[0],py[0],Math.max(18,(int)(w*.2)));
   // Ambiguous/reacquired matches are excluded rather than patched with a synthetic path.
   s.valid=m[0].ok&&m[1].ok&&m[2].ok;
   double dx1=m[1].x-ix[1],dy1=m[1].y-iy[1],dx2=m[2].x-ix[2],dy2=m[2].y-iy[2];
   s.commonX=(dx1+dx2)/2;s.commonY=(dy1+dy2)/2;
   double refLen=Math.hypot(ix[2]-ix[1],iy[2]-iy[1]),curLen=Math.hypot(m[2].x-m[1].x,m[2].y-m[1].y);
   double scale=curLen/Math.max(.001,refLen),theta=Math.atan2(m[2].y-m[1].y,m[2].x-m[1].x)-Math.atan2(iy[2]-iy[1],ix[2]-ix[1]);
   theta=Math.atan2(Math.sin(theta),Math.cos(theta));
   if(Math.abs(scale-1)>.025||Math.abs(theta)>.07)s.valid=false;
   double dx=m[0].x-m[1].x,dy=m[0].y-m[1].y;
   s.x=ix[1]+(dx*Math.cos(theta)+dy*Math.sin(theta))/Math.max(.001,scale);
   s.y=iy[1]+(-dx*Math.sin(theta)+dy*Math.cos(theta))/Math.max(.001,scale);
   if(s.valid&&previousGood){
    try{MotionCore.Patch back=new MotionCore.Patch(f,m[0].x,m[0].y,seed[0].rx,seed[0].ry);
     MotionCore.Match rev=MotionCore.locate(previous,back,px[0],py[0],8);
     if(rev.score<.65||Math.hypot(rev.x-px[0],rev.y-py[0])>2.5)s.valid=false;
    }catch(IllegalArgumentException e){s.valid=false;}
   }
   s.screenX=m[0].x/(double)w;s.screenY=m[0].y/(double)h;s.ncc=Math.min(m[0].score,Math.min(m[1].score,m[2].score));
   // Only well-identified matches update the predictor; the original template remains fixed.
   if(m[0].ok){px[0]=m[0].x;py[0]=m[0].y;}previous=f;previousGood=s.valid;return s;
  }
 }
}

package com.example.electrodecutcompare;

import android.content.Context;import android.graphics.*;import android.media.MediaMetadataRetriever;import android.net.Uri;import android.os.Build;import android.text.*;
import java.io.*;import java.nio.charset.StandardCharsets;import java.text.SimpleDateFormat;import java.util.*;import java.util.zip.*;

/** Patrol-style: overview first, then complete measurements and text+photo composite pages; never bare photos. */
public final class ReviewReport {
 public static final class Meta {public String line="",equipment="",operator="",profile="",date="";public double elapsed;}
 public static final class Bundle {public File dir,xlsx,zip;public final List<File> images=new ArrayList<>();}
 private final Context context;private final MotionCore.Comparison d;private final Uri ua,ub;private final MotionRegionStore.Settings sa,sb;private final Meta meta;
 private Bundle out;private int page;
 private static final int W=1200,H=1760;
 public ReviewReport(Context c,MotionCore.Comparison d,Uri ua,Uri ub,MotionRegionStore.Settings sa,MotionRegionStore.Settings sb,Meta meta){context=c;this.d=d;this.ua=ua;this.ub=ub;this.sa=sa;this.sb=sb;this.meta=meta;}
 static String f(String a,String b,String c,String d){return ReviewText.of(a,b,c,d);}
 static String num(double n){return Double.isFinite(n)?String.format(Locale.ROOT,"%.2f",n):"—";}
 public Bundle create()throws Exception{
  out=new Bundle();String id=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.ROOT).format(new Date())+"_"+UUID.randomUUID().toString().substring(0,6);
  out.dir=new File(new File(context.getFilesDir(),"reports"),id);if(!out.dir.mkdirs())throw new IOException("Cannot create report");
  try{
   overview();charts();cyclePages(d.a,"A");cyclePages(d.b,"B");for(int i=0;i<Math.min(3,d.ranked.size());i++)photoPage(d.ranked.get(i),i+1);
   out.xlsx=new File(out.dir,"Cutter_"+id+".xlsx");ReviewXlsx.write(out.xlsx,sheets());
   File note=new File(out.dir,"README.txt");try(OutputStream os=new FileOutputStream(note)){os.write(("Cutter Motion Review 2.0.0\n"+MotionCore.ENGINE+"\n"+ReviewText.caveat()+"\n"+ReviewText.basis(d.usingA)+"\n"+f("유효 추적 비율은 정상 확률이 아닙니다. 긴 회차는 버리지 않고 시간 이상 표시로 남깁니다. 원본 영상은 첨부하지 않습니다.","Valid-track fraction is not a probability of normality. Long cycles are retained with a time-outlier flag. Original videos are not attached.","Udział poprawnych śledzeń nie jest prawdopodobieństwem normy. Długie cykle pozostają oznaczone. Filmy źródłowe nie są załączone.","Частка коректного відстеження не є ймовірністю норми. Довгі цикли збережені з позначкою. Оригінальні відео не додано.")).getBytes(StandardCharsets.UTF_8));}
   out.zip=new File(out.dir,"Cutter_"+id+"_Report.zip");try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(out.zip))){List<File> files=new ArrayList<>(out.images);files.add(out.xlsx);files.add(note);byte[] buffer=new byte[16384];for(File file:files){z.putNextEntry(new ZipEntry(file.getName()));try(InputStream in=new FileInputStream(file)){int n;while((n=in.read(buffer))>=0)z.write(buffer,0,n);}z.closeEntry();}}
   return out;
  }catch(Exception e){File[] partial=out.dir.listFiles();if(partial!=null)for(File file:partial)file.delete();out.dir.delete();throw e;}
 }
 private Bitmap page(String title){if(Thread.currentThread().isInterrupted())throw new IllegalStateException("CANCELLED");Bitmap b=Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(ReviewUi.BG);text(c,"CUTTER MOTION REVIEW · v2.0.0",48,32,1100,30,ReviewUi.CYAN,true);text(c,title,48,100,1100,48,ReviewUi.WHITE,true);text(c,meta.date+"  |  Lami · "+meta.profile,48,210,1100,26,ReviewUi.MUTED,false);text(c,f("라인 / 설비 / 점검자: ","Line / equipment / inspector: ","Linia / maszyna / kontroler: ","Лінія / обладнання / інспектор: ")+meta.line+" / "+meta.equipment+" / "+meta.operator,48,250,1100,27,ReviewUi.MUTED,false);return b;}
 private void finish(Bitmap b)throws IOException{Canvas c=new Canvas(b);text(c,ReviewText.caveat(),48,1595,1100,26,ReviewUi.WARN,false);text(c,MotionCore.ENGINE+" · "+(++page),48,1710,1100,23,ReviewUi.MUTED,false);File file=new File(out.dir,String.format(Locale.ROOT,"%02d_Report.jpg",page));try(OutputStream os=new FileOutputStream(file)){if(!b.compress(Bitmap.CompressFormat.JPEG,94,os))throw new IOException("JPEG failed");}finally{b.recycle();}out.images.add(file);}
 private void overview()throws Exception{
  Bitmap b=page(f("점검 결과 요약","Inspection overview","Podsumowanie kontroli","Підсумок перевірки"));Canvas c=new Canvas(b);
  text(c,ReviewText.basis(d.usingA),48,315,1100,32,ReviewUi.WARN,true);
  String state=f("A 측정 상태: ","A measurement: ","Pomiar A: ","Вимірювання A: ")+ReviewText.reason(d.a.qualityReason)+"\n"+f("B 측정 상태: ","B measurement: ","Pomiar B: ","Вимірювання B: ")+ReviewText.reason(d.b.qualityReason);
  text(c,state,48,382,1100,29,ReviewUi.WHITE,true);
  int y=540;String[][] rows=metricRows(d);for(String[] row:rows){text(c,row[0],48,y,690,28,ReviewUi.MUTED,false);text(c,row[1],770,y,370,32,ReviewUi.WHITE,true);y+=60;}
  MotionCore.Cycle a=MotionCore.representative(d.a),x=d.ranked.isEmpty()?MotionCore.representative(d.b):d.ranked.get(0);
  text(c,f("A 기준 | B 비교 · 표시 프레임","A reference | B test · displayed frames","A referencja | B test · klatki","A еталон | B тест · кадри"),48,1130,1100,32,ReviewUi.CYAN,true);
  if(a!=null&&x!=null){double q=x.maxPhase;photo(c,ua,d.a,sa,d.a.time[a.start]+a.seconds*q,new RectF(48,1190,582,1515));photo(c,ub,d.b,sb,d.b.time[x.start]+x.seconds*q,new RectF(618,1190,1152,1515));}
  else text(c,f("유효 사이클이 부족합니다. ROI와 촬영 조건을 확인하세요.","Not enough valid cycles. Check regions and recording conditions.","Za mało poprawnych cykli. Sprawdź obszary i nagranie.","Замало коректних циклів. Перевірте ділянки та умови зйомки."),48,1240,1100,34,ReviewUi.WARN,false);
  finish(b);
 }
 public static String[][] metricRows(MotionCore.Comparison d){
  String na=f("보류","Withheld","Wstrzymano","Відкладено");return new String[][]{
   {f("유효 추적 비율 A / B","Valid tracking A / B","Poprawne śledzenie A / B","Коректне відстеження A / B"),num(d.a.validFraction*100)+"% / "+num(d.b.validFraction*100)+"%"},
   {f("완전한 사이클 수 A / B","Complete cycles A / B","Pełne cykle A / B","Повні цикли A / B"),d.a.cycles.size()+" / "+d.b.cycles.size()},
   {f("사이클 시간 중앙값 A / B","Median cycle time A / B","Mediana czasu cyklu A / B","Медіанний час циклу A / B"),(d.a.cycles.isEmpty()?na:num(d.a.medianTime)+"s")+" / "+(d.b.cycles.isEmpty()?na:num(d.b.medianTime)+"s")},
   {f("B 내부 반복 퍼짐 (%스트로크)","B repeat spread (%stroke)","Rozrzut B (%skoku)","Розкид B (%ходу)"),d.b.usable?num(d.b.spreadPct)+"%":na},
   {f("A/B 형태 차이 (RMS)","A/B shape difference (RMS)","Różnica kształtu A/B (RMS)","Різниця форми A/B (RMS)"),d.valid?num(d.rawShapePct)+"%":na},
   {f("형태 비교 보조값 (제한 DTW)","Secondary shape metric (limited DTW)","Pomocnicza miara (ograniczone DTW)","Допоміжна міра (обмежене DTW)"),d.valid?num(d.dtwShapePct)+"%":na},
   {f("B 사이클 시간 변화 (A 대비)","B cycle duration change vs A","Zmiana czasu B względem A","Зміна часу B відносно A"),d.valid?num(d.cycleTimeDeltaPct)+"%":na},
   {f("시간 CV A / B","Time CV A / B","CV czasu A / B","CV часу A / B"),(d.a.cycles.size()<2?na:num(d.a.timeCv)+"%")+" / "+(d.b.cycles.size()<2?na:num(d.b.timeCv)+"%")}
  };
 }
 private void charts()throws Exception{Bitmap b=page(f("궤적과 시간 · 분리 비교","Shape and time · separate comparison","Kształt i czas · osobne porównanie","Форма та час · окреме порівняння"));Canvas c=new Canvas(b);ReviewChart.draw(c,new RectF(48,350,1152,850),d,false);ReviewChart.draw(c,new RectF(48,900,1152,1400),d,true);text(c,f("각 영상의 공통 스트로크로 정규화합니다. 회차마다 크기를 재조정하지 않아 스트로크 변화와 위치 밀림이 남습니다.","One common stroke scale per video. Cycles are not individually resized, so stroke variation and drift remain visible.","Jedna skala skoku na film. Bez osobnego skalowania cykli, aby zachować zmiany skoku i dryft.","Єдина шкала ходу для відео. Без окремого масштабування циклів, щоб зберегти зміну ходу та дрейф."),48,1450,1100,27,ReviewUi.MUTED,false);finish(b);}
 private void cyclePages(MotionCore.Analysis a,String role)throws Exception{
  for(int off=0;off<Math.max(1,a.cycles.size());off+=16){Bitmap b=page(role+" · "+f("모든 완료 사이클 (차이가 작은 회차 포함)","All completed cycles (including small differences)","Wszystkie pełne cykle (także małe różnice)","Усі повні цикли (включно з малими різницями)"));Canvas c=new Canvas(b);text(c,ReviewText.reason(a.qualityReason),48,330,1100,30,ReviewUi.WARN,true);text(c,"Cycle       Video start → end (s)       Δ Shape %       Time flag",48,420,1100,27,ReviewUi.CYAN,true);int y=490;
   for(int i=off;i<Math.min(off+16,a.cycles.size());i++){MotionCore.Cycle cy=a.cycles.get(i);String s=String.format(Locale.ROOT,"%3d             %.3f → %.3f              %.2f             %s",cy.id,a.time[cy.start],a.time[cy.end],cy.internalDiff,cy.timeOutlier?"CHECK":"—");text(c,s,48,y,1100,28,cy.timeOutlier?ReviewUi.WARN:ReviewUi.WHITE,false);y+=61;}
   text(c,f("표의 형태 차이는 해당 영상 중앙 궤적 대비입니다. 시간 특이 회차는 삭제하지 않고 표시합니다.","Table shape difference is relative to that video's median trajectory. Timing outliers are retained, not deleted.","Różnica kształtu względem mediany tego filmu. Cykle odstające czasowo pozostają w tabeli.","Різниця форми відносно медіани цього відео. Часові викиди залишаються в таблиці."),48,1500,1100,26,ReviewUi.MUTED,false);finish(b);}
 }
 private void photoPage(MotionCore.Cycle cy,int rank)throws Exception{
  Bitmap b=page("TOP "+rank+" · B Cycle "+cy.id);Canvas c=new Canvas(b);text(c,ReviewText.phase(cy.rankPhase)+" · "+num(cy.shapeDiff)+"% RMS",48,310,1100,34,ReviewUi.WARN,true);text(c,ReviewText.basis(d.usingA),48,390,1100,26,ReviewUi.MUTED,false);
  MotionCore.Analysis left=d.usingA?d.a:d.b;MotionCore.Cycle base=MotionCore.representative(left);Uri leftUri=d.usingA?ua:ub;MotionRegionStore.Settings leftSet=d.usingA?sa:sb;
  String[] labels={f("직전","Before","Przed","До"),f("최대 차이","Peak difference","Największa różnica","Максимальна різниця"),f("직후","After","Po","Після")};
  for(int i=0;i<3;i++){double q=Math.max(0,Math.min(1,cy.maxPhase+(i-1)*.08));int top=455+i*368;text(c,labels[i]+" · "+num(q*100)+"%",48,top,1100,29,ReviewUi.CYAN,true);double ta=left.time[base.start]+base.seconds*q,tb=d.b.time[cy.start]+cy.seconds*q;photo(c,leftUri,left,leftSet,ta,new RectF(48,top+45,582,top+315));photo(c,ub,d.b,sb,tb,new RectF(618,top+45,1152,top+315));text(c,(d.usingA?"A":"B median")+" "+num(ta)+"s           |           B C"+cy.id+" "+num(tb)+"s",48,top+320,1100,24,ReviewUi.MUTED,false);}
  finish(b);
 }
 private void photo(Canvas c,Uri uri,MotionCore.Analysis analysis,MotionRegionStore.Settings settings,double sec,RectF box){
  Paint p=new Paint(3);p.setColor(0xff162c44);c.drawRect(box,p);MediaMetadataRetriever m=new MediaMetadataRetriever();Bitmap bm=null;
  try{m.setDataSource(context,uri);if(Build.VERSION.SDK_INT>=27)bm=m.getScaledFrameAtTime((long)(sec*1e6),MediaMetadataRetriever.OPTION_CLOSEST,640,480);if(bm==null)bm=m.getFrameAtTime((long)(sec*1e6),MediaMetadataRetriever.OPTION_CLOSEST);if(bm==null)throw new IOException("No image");float k=Math.min(box.width()/bm.getWidth(),box.height()/bm.getHeight());float ww=bm.getWidth()*k,hh=bm.getHeight()*k;RectF image=new RectF(box.centerX()-ww/2,box.centerY()-hh/2,box.centerX()+ww/2,box.centerY()+hh/2);c.drawBitmap(bm,null,image,p);
   MotionCore.Sample nearest=null;double dist=Double.POSITIVE_INFINITY;for(MotionCore.Sample s:analysis.samples)if(Math.abs(s.sec-sec)<dist){dist=Math.abs(s.sec-sec);nearest=s;}
   if(nearest!=null&&nearest.valid&&dist<=.06){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(ReviewUi.WARN);float x=image.left+(float)nearest.screenX*ww,y=image.top+(float)nearest.screenY*hh,rx=settings.boxes[0][2]*ww,ry=settings.boxes[0][3]*hh;c.drawRect(x-rx,y-ry,x+rx,y+ry,p);}
  }catch(Exception e){text(c,f("프레임 표시 실패","Frame unavailable","Klatka niedostępna","Кадр недоступний"),box.left+10,box.top+60,(int)box.width()-20,29,ReviewUi.WARN,true);}finally{if(bm!=null)bm.recycle();try{m.release();}catch(Exception ignored){}}
 }
 private List<ReviewXlsx.Sheet> sheets(){
  List<ReviewXlsx.Sheet> list=new ArrayList<>();ReviewXlsx.Sheet overview=new ReviewXlsx.Sheet("Summary").row(f("항목","Item","Pozycja","Пункт"),f("값","Value","Wartość","Значення"));
  overview.row("Version",MotionCore.ENGINE).row("Date",meta.date).row("Line",meta.line).row("Equipment",meta.equipment).row("Inspector",meta.operator).row("Profile",meta.profile).row("Basis",ReviewText.basis(d.usingA)).row("A measurement",ReviewText.reason(d.a.qualityReason)).row("B measurement",ReviewText.reason(d.b.qualityReason)).row("A timing",d.a.timeSource).row("B timing",d.b.timeSource).row("A truncated",d.a.truncated).row("B truncated",d.b.truncated).row("A sample Hz",d.a.sampleHz).row("B sample Hz",d.b.sampleHz).row("A minimum samples per cycle",d.a.samplesPerCycle).row("B minimum samples per cycle",d.b.samplesPerCycle).row("A rejected segments",d.a.invalidSegments).row("B rejected segments",d.b.invalidSegments).row("A partial end fragments",d.a.partialEnds).row("B partial end fragments",d.b.partialEnds).row("A stroke (analysis px, not mm)",d.a.strokePx).row("B stroke (analysis px, not mm)",d.b.strokePx).row("A common-reference RMS (px)",d.a.commonRms).row("B common-reference RMS (px)",d.b.commonRms).row("Inspection seconds",meta.elapsed).row("Limitations",ReviewText.caveat());for(String[] row:metricRows(d))overview.row((Object[])row);list.add(overview);
  MotionCore.Analysis[] all={d.a,d.b};for(int k=0;k<2;k++){MotionCore.Analysis a=all[k];String role=k==0?"A":"B";
   ReviewXlsx.Sheet cs=new ReviewXlsx.Sheet(role+"_Cycles").row("Cycle","Video start (s)","Video end (s)","Duration (s)","Internal RMS (%stroke)","vs reference RMS (%)","Duration vs ref (%)","Stroke (video units)","Start drift (video units)","Time outlier");for(MotionCore.Cycle cy:a.cycles)cs.row(cy.id,a.time[cy.start],a.time[cy.end],cy.seconds,cy.internalDiff,k==1&&a.usable?cy.shapeDiff:null,k==1&&a.usable?cy.timeDeltaPct:null,cy.amplitude,cy.startOffset,cy.timeOutlier?"CHECK":"");list.add(cs);
   ReviewXlsx.Sheet tr=new ReviewXlsx.Sheet(role+"_Trace").row("Video time (s)","Decoder PTS (s)","Valid (1/0)","Relative X (analysis px)","Relative Y (analysis px)","Common X (analysis px)","Common Y (analysis px)","Template NCC (-1..1)");for(MotionCore.Sample s:a.samples)tr.row(s.sec,s.decoderPtsSec,s.valid?1:0,s.valid?s.x:null,s.valid?s.y:null,s.commonX,s.commonY,s.ncc);list.add(tr);
  }return list;
 }
 public static int text(Canvas c,String str,float x,float y,int width,int size,int color,boolean bold){TextPaint p=new TextPaint(3);p.setTextSize(size);p.setColor(color);p.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);StaticLayout l=StaticLayout.Builder.obtain(str,0,str.length(),p,width).setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).build();c.save();c.translate(x,y);l.draw(c);c.restore();return l.getHeight();}
}

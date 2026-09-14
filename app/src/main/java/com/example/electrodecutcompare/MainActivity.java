package com.example.electrodecutcompare;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_A=1001, PICK_B=1002;
    private Uri uriA, uriB;
    private long durationA=0, durationB=0;
    private TextView txtA,txtB,txtTimeA,txtTimeB,txtStatus;
    private SeekBar seekA,seekB;
    private ImageView imgA,imgB,imgDiff,imgCycle,imgRoiA,imgRoiB,imgRoiCompare;
    private ProgressBar progress;
    private Button btnSave;
    private Bitmap frameA, alignedB, diffBitmap, cycleBitmap, roiCompareBitmap;
    private BitmapAnalysis.Transform lastTransform;
    private RoiQualityAnalyzer.Metrics roiA, roiB;
    private CycleAnalyzer.Result cycleA, cycleB;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_main);
        txtA=findViewById(R.id.txtA); txtB=findViewById(R.id.txtB);
        txtTimeA=findViewById(R.id.txtTimeA); txtTimeB=findViewById(R.id.txtTimeB);
        txtStatus=findViewById(R.id.txtStatus); seekA=findViewById(R.id.seekA); seekB=findViewById(R.id.seekB);
        imgA=findViewById(R.id.imgA); imgB=findViewById(R.id.imgB); imgDiff=findViewById(R.id.imgDiff); imgCycle=findViewById(R.id.imgCycle);
        imgRoiA=findViewById(R.id.imgRoiA); imgRoiB=findViewById(R.id.imgRoiB); imgRoiCompare=findViewById(R.id.imgRoiCompare);
        progress=findViewById(R.id.progress); btnSave=findViewById(R.id.btnSave);

        findViewById(R.id.btnSelectA).setOnClickListener(v->pickVideo(PICK_A));
        findViewById(R.id.btnSelectB).setOnClickListener(v->pickVideo(PICK_B));
        findViewById(R.id.btnPreviewA).setOnClickListener(v->preview(uriA));
        findViewById(R.id.btnPreviewB).setOnClickListener(v->preview(uriB));
        findViewById(R.id.btnAnalyze).setOnClickListener(v->analyze());
        findViewById(R.id.btnCycle).setOnClickListener(v->analyzeCycle());
        findViewById(R.id.btnRoi).setOnClickListener(v->analyzeRoi());
        btnSave.setOnClickListener(v->saveResult());
        SeekBar.OnSeekBarChangeListener listener=new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean f){ updateTimeLabels(); }
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
        };
        seekA.setOnSeekBarChangeListener(listener); seekB.setOnSeekBarChangeListener(listener);
    }

    private void pickVideo(int req){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("video/*"); startActivityForResult(i,req);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;
        Uri u=data.getData();
        try{ getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION); }catch(Exception ignored){}
        if(requestCode==PICK_A){uriA=u; durationA=videoDuration(u); txtA.setText("선택됨: "+u.getLastPathSegment());}
        if(requestCode==PICK_B){uriB=u; durationB=videoDuration(u); txtB.setText("선택됨: "+u.getLastPathSegment());}
        updateTimeLabels();
    }

    private long videoDuration(Uri u){
        MediaMetadataRetriever r=new MediaMetadataRetriever();
        try{r.setDataSource(this,u); String d=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION); return d==null?0:Long.parseLong(d);}catch(Exception e){return 0;}finally{try{r.release();}catch(Exception ignored){}}
    }

    private void updateTimeLabels(){
        txtTimeA.setText(String.format(Locale.getDefault(),"대표 프레임: %d%% (%.2fs)",seekA.getProgress(),durationA*seekA.getProgress()/100000.0));
        txtTimeB.setText(String.format(Locale.getDefault(),"대표 프레임: %d%% (%.2fs)",seekB.getProgress(),durationB*seekB.getProgress()/100000.0));
    }

    private void preview(Uri uri){
        if(uri==null){Toast.makeText(this,"먼저 영상을 선택해 주세요.",Toast.LENGTH_SHORT).show();return;}
        Intent i=new Intent(this,VideoPreviewActivity.class); i.putExtra("uri",uri.toString()); startActivity(i);
    }

    private Bitmap frameAt(Uri uri,long duration,int percent){
        MediaMetadataRetriever r=new MediaMetadataRetriever();
        try{
            r.setDataSource(this,uri); long us=Math.max(0,(duration*1000L*percent)/100L);
            Bitmap b=r.getFrameAtTime(us,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if(b==null) b=r.getFrameAtTime(us,MediaMetadataRetriever.OPTION_CLOSEST);
            return b;
        }finally{try{r.release();}catch(Exception ignored){}}
    }

    private void analyze(){
        if(uriA==null||uriB==null){Toast.makeText(this,"A/B 영상을 모두 선택해 주세요.",Toast.LENGTH_SHORT).show();return;}
        btnSave.setEnabled(false); progress.setProgress(5); txtStatus.setText("대표 프레임 추출 중...");
        executor.execute(()->{
            try{
                Bitmap a=frameAt(uriA,durationA,seekA.getProgress()); Bitmap b=frameAt(uriB,durationB,seekB.getProgress());
                if(a==null||b==null) throw new Exception("프레임을 읽지 못했습니다.");
                int maxW=960; a=BitmapAnalysis.fitMaxWidth(a,maxW); b=BitmapAnalysis.fitMaxWidth(b,maxW);
                Bitmap finalA=a, finalB=b;
                runOnUiThread(()->{progress.setProgress(25); txtStatus.setText("촬영각도·위치 자동 보정 중... 저사양 기기에서는 수 초 걸릴 수 있습니다."); imgA.setImageBitmap(finalA);});
                BitmapAnalysis.Transform t=BitmapAnalysis.estimate(a,b);
                lastTransform=t;
                Bitmap aligned=BitmapAnalysis.applyTransform(b,a.getWidth(),a.getHeight(),t);
                runOnUiThread(()->{progress.setProgress(75); txtStatus.setText("Difference Map 계산 중..."); imgB.setImageBitmap(aligned);});
                Bitmap diff=BitmapAnalysis.differenceOverlay(a,aligned);
                frameA=a; alignedB=aligned; diffBitmap=diff;
                runOnUiThread(()->{
                    progress.setProgress(100); imgDiff.setImageBitmap(diff); btnSave.setEnabled(true);
                    txtStatus.setText(String.format(Locale.getDefault(),"완료 · 자동 보정값: 회전 %.2f°, 배율 %.3f, 이동 X %.1fpx / Y %.1fpx\n현재 빨간 영역은 '차이 후보'이며 불량 확정값이 아닙니다.",t.angleDeg,t.scale,t.dx,t.dy));
                });
            }catch(Exception e){ runOnUiThread(()->{progress.setProgress(0); txtStatus.setText("분석 실패: "+e.getMessage());}); }
        });
    }


    private void analyzeCycle(){
        if(uriA==null||uriB==null){Toast.makeText(this,"A/B 영상을 모두 선택해 주세요.",Toast.LENGTH_SHORT).show();return;}
        progress.setProgress(5); txtStatus.setText("Cycle 움직임 분석 중... (CPU 경량 분석)");
        executor.execute(()->{
            try{
                CycleAnalyzer.Result a=CycleAnalyzer.analyze(this,uriA,durationA,"설비 A");
                runOnUiThread(()->{progress.setProgress(45); txtStatus.setText("설비 B Cycle 분석 중...");});
                CycleAnalyzer.Result b=CycleAnalyzer.analyze(this,uriB,durationB,"설비 B");
                Bitmap cmp=CycleAnalyzer.compare(a,b);
                cycleA=a; cycleB=b; cycleBitmap=cmp;
                runOnUiThread(()->{
                    progress.setProgress(100); imgCycle.setImageBitmap(cmp); btnSave.setEnabled(true);
                    txtStatus.setText("Cycle 분석 완료\n"+a.summary+"\n\n"+b.summary);
                });
            }catch(Exception e){
                runOnUiThread(()->{progress.setProgress(0); txtStatus.setText("Cycle 분석 실패: "+e.getMessage());});
            }
        });
    }


    private void analyzeRoi(){
        if(uriA==null||uriB==null){Toast.makeText(this,"A/B 영상을 모두 선택해 주세요.",Toast.LENGTH_SHORT).show();return;}
        progress.setProgress(5); txtStatus.setText("v0.3 ROI 정밀분석 준비 중...");
        executor.execute(()->{
            try{
                // Ensure we have a camera transform based on the currently selected representative frames.
                Bitmap a=frameAt(uriA,durationA,seekA.getProgress());
                Bitmap b=frameAt(uriB,durationB,seekB.getProgress());
                if(a==null||b==null) throw new Exception("프레임을 읽지 못했습니다.");
                a=BitmapAnalysis.fitMaxWidth(a,960); b=BitmapAnalysis.fitMaxWidth(b,960);
                if(b.getWidth()!=a.getWidth()||b.getHeight()!=a.getHeight())
                    b=Bitmap.createScaledBitmap(b,a.getWidth(),a.getHeight(),true);

                BitmapAnalysis.Transform t=BitmapAnalysis.estimate(a,b);
                lastTransform=t;
                final int w=a.getWidth(), h=a.getHeight();
                runOnUiThread(()->{progress.setProgress(25); txtStatus.setText("설비 A: 전극 선단 / Gripper / Nip ROI 추적 중...");});

                // A is reference, so no camera transform.
                RoiQualityAnalyzer.Metrics ma=RoiQualityAnalyzer.analyze(
                        this,uriA,durationA,seekA.getProgress(),null,w,h,"설비 A");

                runOnUiThread(()->{progress.setProgress(55); txtStatus.setText("설비 B: 촬영각 보정 후 ROI 추적 중...");});
                RoiQualityAnalyzer.Metrics mb=RoiQualityAnalyzer.analyze(
                        this,uriB,durationB,seekB.getProgress(),t,w,h,"설비 B (보정)");

                Bitmap cmp=RoiQualityAnalyzer.compare(ma,mb);
                roiA=ma; roiB=mb; roiCompareBitmap=cmp;

                runOnUiThread(()->{
                    progress.setProgress(100);
                    imgRoiA.setImageBitmap(ma.overlay);
                    imgRoiB.setImageBitmap(mb.overlay);
                    imgRoiCompare.setImageBitmap(cmp);
                    btnSave.setEnabled(true);
                    txtStatus.setText("v0.3 ROI 분석 완료\n"+ma.summary+"\n\n"+mb.summary+
                            String.format(Locale.getDefault(),
                                    "\n\n카메라 보정: 회전 %.2f°, 배율 %.3f, X %.1fpx / Y %.1fpx"+
                                    "\n※ 현재 px/Score는 설비 변화 추세용 참고값입니다. 실제 mm 및 NG 기준은 Calibration/Golden 데이터로 확정해야 합니다.",
                                    t.angleDeg,t.scale,t.dx,t.dy));
                });
            }catch(Exception e){
                runOnUiThread(()->{progress.setProgress(0);txtStatus.setText("ROI 분석 실패: "+e.getMessage());});
            }
        });
    }

    private Bitmap composeResult(){
        if(frameA==null||alignedB==null||diffBitmap==null)return null;
        int w=frameA.getWidth(), gap=20, titleH=80, h=frameA.getHeight();
        int cycleExtra=(cycleBitmap==null?0:Math.round((w*3f)*cycleBitmap.getHeight()/cycleBitmap.getWidth())+gap);
        int roiExtra=(roiCompareBitmap==null?0:Math.round((w*3f)*roiCompareBitmap.getHeight()/roiCompareBitmap.getWidth())+gap);
        Bitmap out=Bitmap.createBitmap(w*3+gap*4,h+titleH+gap*2+cycleExtra+roiExtra,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(out); c.drawColor(Color.WHITE); Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); p.setColor(Color.rgb(12,45,87)); p.setTextSize(34); p.setFakeBoldText(true);
        c.drawText("전극 컷팅 A/B 비교 · A / 보정 B / Difference",gap,52,p);
        c.drawBitmap(frameA,gap,titleH+gap,p); c.drawBitmap(alignedB,w+gap*2,titleH+gap,p); c.drawBitmap(diffBitmap,w*2+gap*3,titleH+gap,p);
        int y=h+titleH+gap*2;
        if(cycleBitmap!=null){
            int cw=w*3+gap*2; int ch=Math.round(cw*(cycleBitmap.getHeight()/(float)cycleBitmap.getWidth()));
            Bitmap scaled=Bitmap.createScaledBitmap(cycleBitmap,cw,ch,true);
            c.drawBitmap(scaled,gap,y,p); y+=ch+gap;
        }
        if(roiCompareBitmap!=null){
            int cw=w*3+gap*2; int ch=Math.round(cw*(roiCompareBitmap.getHeight()/(float)roiCompareBitmap.getWidth()));
            Bitmap scaled=Bitmap.createScaledBitmap(roiCompareBitmap,cw,ch,true);
            c.drawBitmap(scaled,gap,y,p);
        }
        return out;
    }

    private void saveResult(){
        Bitmap result=composeResult(); if(result==null)return;
        String stamp=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault()).format(new Date());
        ContentValues v=new ContentValues(); v.put(MediaStore.Images.Media.DISPLAY_NAME,"CuttingCompare_"+stamp+".jpg"); v.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
        if(android.os.Build.VERSION.SDK_INT>=29) v.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/ElectrodeCuttingCompare");
        Uri u=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v);
        if(u==null){Toast.makeText(this,"저장 위치를 만들지 못했습니다.",Toast.LENGTH_LONG).show();return;}
        try(OutputStream os=getContentResolver().openOutputStream(u)){result.compress(Bitmap.CompressFormat.JPEG,92,os); Toast.makeText(this,"Pictures/ElectrodeCuttingCompare 에 저장했습니다.",Toast.LENGTH_LONG).show();}
        catch(Exception e){Toast.makeText(this,"저장 실패: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }

    @Override protected void onDestroy(){super.onDestroy(); executor.shutdownNow();}
}

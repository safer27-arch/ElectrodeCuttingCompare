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
import android.widget.Spinner;
import android.widget.ArrayAdapter;

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
    private ImageView imgA,imgB,imgDiff,imgCycle,imgHighSpeed,imgRoiA,imgRoiB,imgRoiCompare;
    private ProgressBar progress;
    private Button btnSave;
    private Spinner cutterProfile;
    private Bitmap frameA, alignedB, diffBitmap, cycleBitmap, highSpeedBitmap, advancedBitmap, diagnosticBitmap, roiCompareBitmap;
    private ImageView imgAdvanced, imgDiagnostic, imgEasyDiagnostic;
    private AdvancedMotionAnalyzer.Result lastAdvanced;
    private Bitmap easyDiagnosticBitmap;
    private BitmapAnalysis.Transform lastTransform;
    private RoiQualityAnalyzer.Metrics roiA, roiB;
    private CycleAnalyzer.Result cycleA, cycleB;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(true); setContentView(R.layout.activity_main);
        txtA=findViewById(R.id.txtA); txtB=findViewById(R.id.txtB);
        txtTimeA=findViewById(R.id.txtTimeA); txtTimeB=findViewById(R.id.txtTimeB);
        txtStatus=findViewById(R.id.txtStatus); seekA=findViewById(R.id.seekA); seekB=findViewById(R.id.seekB);
        imgA=findViewById(R.id.imgA); imgB=findViewById(R.id.imgB); imgDiff=findViewById(R.id.imgDiff); imgCycle=findViewById(R.id.imgCycle); imgHighSpeed=findViewById(R.id.imgHighSpeed);
        imgRoiA=findViewById(R.id.imgRoiA); imgRoiB=findViewById(R.id.imgRoiB); imgRoiCompare=findViewById(R.id.imgRoiCompare);
        progress=findViewById(R.id.progress); btnSave=findViewById(R.id.btnSave); imgAdvanced=findViewById(R.id.imgAdvanced); imgDiagnostic=findViewById(R.id.imgDiagnostic); imgEasyDiagnostic=findViewById(R.id.imgEasyDiagnostic);
        cutterProfile=findViewById(R.id.cutterProfile);
        cutterProfile.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"45° Cutter","0° Cutter","사용자 Cutter"}));
        cutterProfile.setSelection(AppStateStore.getInt(this,"profile",0));
        cutterProfile.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){AppStateStore.putInt(MainActivity.this,"profile",pos);}public void onNothingSelected(android.widget.AdapterView<?> p){}});

        findViewById(R.id.btnSelectA).setOnClickListener(v->pickVideo(PICK_A));
        findViewById(R.id.btnSelectB).setOnClickListener(v->pickVideo(PICK_B));
        findViewById(R.id.btnPreviewA).setOnClickListener(v->preview(uriA));
        findViewById(R.id.btnPreviewB).setOnClickListener(v->preview(uriB));
        findViewById(R.id.btnAnalyze).setOnClickListener(v->analyze());
        findViewById(R.id.btnCycle).setOnClickListener(v->analyzeCycle());
        findViewById(R.id.btnHighSpeed).setOnClickListener(v->analyzeHighSpeed());
        findViewById(R.id.btnAdvanced).setOnClickListener(v->analyzeAdvanced());
        findViewById(R.id.btnDiagnostic).setOnClickListener(v->analyzeDiagnostic());
        findViewById(R.id.btnEasyDiagnostic).setOnClickListener(v->analyzeEasyDiagnostic());
        findViewById(R.id.btnGolden).setOnClickListener(v->saveGolden());
        findViewById(R.id.btnCalibration).setOnClickListener(v->showCalibrationDialog());
        findViewById(R.id.btnRoi).setOnClickListener(v->analyzeRoi());
        findViewById(R.id.btnRoiSettings).setOnClickListener(v->{
            Intent i=new Intent(this,RoiSettingsActivity.class);
            if(uriA!=null)i.putExtra("uriA",uriA.toString());
            if(uriB!=null)i.putExtra("uriB",uriB.toString());
            startActivity(i);
        });
        btnSave.setOnClickListener(v->saveResult());
        SeekBar.OnSeekBarChangeListener listener=new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean f){ updateTimeLabels(); }
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
        };
        seekA.setOnSeekBarChangeListener(listener); seekB.setOnSeekBarChangeListener(listener);
        restoreState();
        ImageView[] zoomables={imgA,imgB,imgDiff,imgCycle,imgHighSpeed,imgRoiA,imgRoiB,imgRoiCompare,imgAdvanced,imgDiagnostic,imgEasyDiagnostic};
        for(ImageView z:zoomables)z.setOnClickListener(v->openZoom((ImageView)v));
    }

    private void pickVideo(int req){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("video/*"); startActivityForResult(i,req);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;
        Uri u=data.getData();
        try{ getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION); }catch(Exception ignored){}
        if(requestCode==PICK_A){uriA=u; durationA=videoDuration(u); txtA.setText("선택됨: "+u.getLastPathSegment());AppStateStore.put(this,"uriA",u.toString());}
        if(requestCode==PICK_B){uriB=u; durationB=videoDuration(u); txtB.setText("선택됨: "+u.getLastPathSegment());AppStateStore.put(this,"uriB",u.toString());}
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


    private void analyzeHighSpeed(){
        if(uriA==null||uriB==null){Toast.makeText(this,"A/B 영상을 모두 선택해 주세요.",Toast.LENGTH_SHORT).show();return;}
        progress.setProgress(5);txtStatus.setText("v0.5 고속 Event 동기화 분석 중...");
        executor.execute(()->{try{HighSpeedAnalyzer.Result a=HighSpeedAnalyzer.analyze(this,uriA,durationA);runOnUiThread(()->progress.setProgress(48));HighSpeedAnalyzer.Result b=HighSpeedAnalyzer.analyze(this,uriB,durationB);HighSpeedAnalyzer.CompareResult cr=HighSpeedAnalyzer.compare(a,b);highSpeedBitmap=cr.chart;runOnUiThread(()->{progress.setProgress(100);imgHighSpeed.setImageBitmap(cr.chart);txtStatus.setText(cr.summary);btnSave.setEnabled(true);});}catch(Exception e){runOnUiThread(()->txtStatus.setText("고속 분석 실패: "+e.getMessage()));}});
    }


    private String profileName(){Object o=cutterProfile.getSelectedItem();return o==null?"45° Cutter":o.toString();}
    private void analyzeAdvanced(){
        if(uriA==null||uriB==null){Toast.makeText(this,"A/B 영상을 모두 선택해 주세요.",Toast.LENGTH_SHORT).show();return;}
        progress.setProgress(5);txtStatus.setText("v0.6 Multi-Cycle / Jerk / Timing 분석 중...");
        executor.execute(()->{try{
            HighSpeedAnalyzer.Result a=HighSpeedAnalyzer.analyze(this,uriA,durationA);
            HighSpeedAnalyzer.Result b=HighSpeedAnalyzer.analyze(this,uriB,durationB);
            String pf=profileName();float[] history=TrendStore.get(this,pf);
            AdvancedMotionAnalyzer.Result ar=AdvancedMotionAnalyzer.analyze(a,b,pf,history);
            TrendStore.add(this,pf,ar.score);advancedBitmap=ar.chart; lastAdvanced=ar;
            runOnUiThread(()->{progress.setProgress(100);imgAdvanced.setImageBitmap(ar.chart);txtStatus.setText(ar.summary+"\n\n이미지/그래프를 누르면 전체화면 확대가 됩니다.");btnSave.setEnabled(true);});
        }catch(Exception e){runOnUiThread(()->{progress.setProgress(0);txtStatus.setText("v0.6 고급분석 실패: "+e.getMessage());});}});
    }

    private void analyzeEasyDiagnostic(){
        if(uriA==null||uriB==null){Toast.makeText(this,"A/B 영상을 모두 선택해 주세요.",Toast.LENGTH_SHORT).show();return;}
        progress.setProgress(5);txtStatus.setText("v0.8 고정부 공통진동과 Cutter 실제운동을 분리 분석 중...");
        executor.execute(()->{try{
            VibrationCompensatedAnalyzer.Result a=VibrationCompensatedAnalyzer.analyze(this,uriA,durationA,profileName()+" / A");
            runOnUiThread(()->progress.setProgress(50));
            VibrationCompensatedAnalyzer.Result b=VibrationCompensatedAnalyzer.analyze(this,uriB,durationB,profileName()+" / B");
            // Show B as current/evaluation machine; status contains both for immediate A/B interpretation.
            easyDiagnosticBitmap=b.chart;
            String verdict = "[설비 A]\n" + a.summary
                    + "\n\n[설비 B]\n" + b.summary
                    + "\n\n쉽게 보기: 회색 공통진동이 커져도 파란 Cutter 상대운동이 안정적이면 고정부 흔들림 영향으로 봅니다.";
            runOnUiThread(()->{progress.setProgress(100);imgEasyDiagnostic.setImageBitmap(b.chart);txtStatus.setText(verdict);btnSave.setEnabled(true);});
        }catch(Exception e){runOnUiThread(()->{progress.setProgress(0);txtStatus.setText("v0.8 진동분리 분석 실패: "+e.getMessage());});}});
    }

    private void analyzeDiagnostic(){
        if(uriA==null||uriB==null){Toast.makeText(this,"A/B 영상을 모두 선택해 주세요.",Toast.LENGTH_SHORT).show();return;}
        progress.setProgress(5);txtStatus.setText("v0.7 Smart Diagnostic · Top3 이상 순간 / Golden 비교 중...");
        executor.execute(()->{try{
            HighSpeedAnalyzer.Result a=HighSpeedAnalyzer.analyze(this,uriA,durationA);
            HighSpeedAnalyzer.Result b=HighSpeedAnalyzer.analyze(this,uriB,durationB);
            String pf=profileName(); AdvancedMotionAnalyzer.Result ar=AdvancedMotionAnalyzer.analyze(a,b,pf,TrendStore.get(this,pf)); lastAdvanced=ar;
            GoldenBaselineStore.Baseline g=GoldenBaselineStore.get(this,pf); float mm=CalibrationStore.get(this,pf);
            DiagnosticAnalyzer.Result dr=DiagnosticAnalyzer.analyze(a,b,ar,roiB,g,mm,pf); diagnosticBitmap=dr.chart;
            runOnUiThread(()->{progress.setProgress(100);imgDiagnostic.setImageBitmap(dr.chart);txtStatus.setText(dr.summary+"\n\n그래프를 누르면 전체화면 확대됩니다.");btnSave.setEnabled(true);});
        }catch(Exception e){runOnUiThread(()->{progress.setProgress(0);txtStatus.setText("통합 진단 실패: "+e.getMessage());});}});
    }

    private void saveGolden(){
        if(lastAdvanced==null){Toast.makeText(this,"먼저 v0.6 고급분석 또는 v0.7 통합진단을 실행해 주세요.",Toast.LENGTH_LONG).show();return;}
        new android.app.AlertDialog.Builder(this).setTitle("Golden 기준 등록")
          .setMessage(profileName()+"의 현재 결과를 정상 Golden 기준으로 저장합니다.\n\n정상 상태가 확인된 영상에서만 등록하세요. 기존 Golden은 교체됩니다.")
          .setNegativeButton("취소",null).setPositiveButton("Golden 등록",(d,w)->{GoldenBaselineStore.save(this,profileName(),lastAdvanced,roiB);Toast.makeText(this,""+profileName()+" Golden 기준을 저장했습니다.",Toast.LENGTH_LONG).show();}).show();
    }

    private void showCalibrationDialog(){
        android.widget.LinearLayout box=new android.widget.LinearLayout(this);box.setOrientation(android.widget.LinearLayout.VERTICAL);int pad=(int)(18*getResources().getDisplayMetrics().density);box.setPadding(pad,pad,pad,pad);
        android.widget.EditText mm=new android.widget.EditText(this);mm.setHint("실제 기준 길이 (mm), 예: 10");mm.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        android.widget.EditText px=new android.widget.EditText(this);px.setHint("영상에서 같은 길이 (px), 예: 250");px.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);box.addView(mm);box.addView(px);
        float old=CalibrationStore.get(this,profileName());String msg=old>0?String.format(Locale.getDefault(),"현재 %.6f mm/px",old):"현재 Calibration 미등록";
        new android.app.AlertDialog.Builder(this).setTitle("px → mm Calibration").setMessage(msg+"\n같은 평면의 알려진 실제 길이와 영상 픽셀 길이를 입력하세요.").setView(box).setNegativeButton("취소",null).setPositiveButton("저장",(d,w)->{try{float m=Float.parseFloat(mm.getText().toString());float p=Float.parseFloat(px.getText().toString());CalibrationStore.set(this,profileName(),m,p);Toast.makeText(this,String.format(Locale.getDefault(),"저장: %.6f mm/px",m/p),Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"숫자를 다시 입력해 주세요.",Toast.LENGTH_LONG).show();}}).show();
    }

    private void restoreState(){
        try{
            String a=AppStateStore.get(this,"uriA"),b=AppStateStore.get(this,"uriB");
            if(!a.isEmpty()){uriA=Uri.parse(a);durationA=videoDuration(uriA);txtA.setText("복원됨: "+uriA.getLastPathSegment());}
            if(!b.isEmpty()){uriB=Uri.parse(b);durationB=videoDuration(uriB);txtB.setText("복원됨: "+uriB.getLastPathSegment());}
            updateTimeLabels();
        }catch(Exception ignored){}
    }
    private void openZoom(ImageView view){
        if(view.getDrawable()==null){Toast.makeText(this,"먼저 분석을 실행해 주세요.",Toast.LENGTH_SHORT).show();return;}
        Bitmap b=null;
        if(view==imgA)b=frameA; else if(view==imgB)b=alignedB; else if(view==imgDiff)b=diffBitmap;
        else if(view==imgCycle)b=cycleBitmap; else if(view==imgHighSpeed)b=highSpeedBitmap; else if(view==imgAdvanced)b=advancedBitmap; else if(view==imgDiagnostic)b=diagnosticBitmap; else if(view==imgEasyDiagnostic)b=easyDiagnosticBitmap;
        else if(view.getDrawable() instanceof android.graphics.drawable.BitmapDrawable)b=((android.graphics.drawable.BitmapDrawable)view.getDrawable()).getBitmap();
        if(b==null)return;
        try{java.io.File f=new java.io.File(getCacheDir(),"zoom_result.jpg");java.io.FileOutputStream os=new java.io.FileOutputStream(f);b.compress(Bitmap.CompressFormat.JPEG,95,os);os.close();Intent i=new Intent(this,ZoomImageActivity.class);i.putExtra("path",f.getAbsolutePath());startActivity(i);}catch(Exception e){Toast.makeText(this,"확대 열기 실패",Toast.LENGTH_SHORT).show();}
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
                frameA=a;
                alignedB=BitmapAnalysis.applyTransform(b,a.getWidth(),a.getHeight(),t);
                diffBitmap=BitmapAnalysis.differenceOverlay(a,alignedB);
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
                    txtStatus.setText("v0.4 ROI 분석 완료\n"+ma.summary+"\n\n"+mb.summary+
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
        int advExtra=(advancedBitmap==null?0:Math.round((w*3f)*advancedBitmap.getHeight()/advancedBitmap.getWidth())+gap);
        int diagExtra=(diagnosticBitmap==null?0:Math.round((w*3f)*diagnosticBitmap.getHeight()/diagnosticBitmap.getWidth())+gap);
        Bitmap out=Bitmap.createBitmap(w*3+gap*4,h+titleH+gap*2+cycleExtra+roiExtra+advExtra+diagExtra,Bitmap.Config.ARGB_8888);
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
            Bitmap scaled=Bitmap.createScaledBitmap(roiCompareBitmap,cw,ch,true); c.drawBitmap(scaled,gap,y,p); y+=ch+gap;
        }
        if(advancedBitmap!=null){int cw=w*3+gap*2;int ch=Math.round(cw*(advancedBitmap.getHeight()/(float)advancedBitmap.getWidth()));Bitmap scaled=Bitmap.createScaledBitmap(advancedBitmap,cw,ch,true);c.drawBitmap(scaled,gap,y,p);y+=ch+gap;}
        if(diagnosticBitmap!=null){int cw=w*3+gap*2;int ch=Math.round(cw*(diagnosticBitmap.getHeight()/(float)diagnosticBitmap.getWidth()));Bitmap scaled=Bitmap.createScaledBitmap(diagnosticBitmap,cw,ch,true);c.drawBitmap(scaled,gap,y,p);}
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

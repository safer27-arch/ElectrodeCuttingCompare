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
    private TextView txtA,txtB,txtTimeA,txtTimeB,txtStatus,txtDashboard,statusIntegrated,summaryIntegrated,txtOverallVerdict,txtStageDashboard,txtTop3Dashboard,txtRepeatabilityDashboard,txtInspectionModeHint,txtFastHeadline,txtReferenceBanner;
    private TextView statusCompare,statusCycle,statusRepeatability,statusHighSpeed,statusAdvanced,statusEasy,statusDiagnostic,statusRoi;
    private SeekBar seekA,seekB;
    private ImageView imgA,imgB,imgDiff,imgCycle,imgRepeatability,imgHighSpeed,imgRoiA,imgRoiB,imgRoiCompare,imgTop1,imgTop2,imgTop3,imgProblemFinder,imgInspectionOverview;
    private ProgressBar progress,progressIntegrated;
    private ProgressBar progressCompare,progressCycle,progressRepeatability,progressHighSpeed,progressAdvanced,progressEasy,progressDiagnostic,progressRoi;
    private Button btnSave,btnReplayTop1,btnReplayTop2,btnReplayTop3;
    private View replayPanel;
    private Spinner cutterProfile,inspectionMode;
    private Bitmap frameA, alignedB, diffBitmap, cycleBitmap, repeatabilityBitmap, highSpeedBitmap, advancedBitmap, diagnosticBitmap, roiCompareBitmap;
    private ImageView imgAdvanced, imgDiagnostic, imgEasyDiagnostic;
    private AdvancedMotionAnalyzer.Result lastAdvanced;
    private Bitmap easyDiagnosticBitmap;
    private VibrationCompensatedAnalyzer.Result lastVibrationA,lastVibrationB;
    private BitmapAnalysis.Transform lastTransform;
    private RoiQualityAnalyzer.Metrics roiA, roiB;
    private CycleAnalyzer.Result cycleA, cycleB;
    private CycleRepeatabilityAnalyzer.Result repeatA, repeatB;
    private CycleRepeatabilityAnalyzer.CompareResult repeatCompare;
    private HighSpeedAnalyzer.Result cachedHighA,cachedHighB;
    private HighSpeedAnalyzer.CompareResult cachedHighCompare;
    private String lastInspectionModeName="빠른검사";
    private long integratedStartElapsedMs=0L;
    private double lastInspectionElapsedSec=0.0;
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
        txtDashboard=findViewById(R.id.txtDashboard); statusIntegrated=findViewById(R.id.statusIntegrated); summaryIntegrated=findViewById(R.id.summaryIntegrated); progressIntegrated=findViewById(R.id.progressIntegrated);
        txtOverallVerdict=findViewById(R.id.txtOverallVerdict); txtStageDashboard=findViewById(R.id.txtStageDashboard); txtTop3Dashboard=findViewById(R.id.txtTop3Dashboard); txtRepeatabilityDashboard=findViewById(R.id.txtRepeatabilityDashboard); txtFastHeadline=findViewById(R.id.txtFastHeadline); txtReferenceBanner=findViewById(R.id.txtReferenceBanner);
        imgTop1=findViewById(R.id.imgTop1); imgTop2=findViewById(R.id.imgTop2); imgTop3=findViewById(R.id.imgTop3); imgProblemFinder=findViewById(R.id.imgProblemFinder); imgInspectionOverview=findViewById(R.id.imgInspectionOverview);
        progressCompare=findViewById(R.id.progressCompare); statusCompare=findViewById(R.id.statusCompare);
        progressCycle=findViewById(R.id.progressCycle); statusCycle=findViewById(R.id.statusCycle);
        progressRepeatability=findViewById(R.id.progressRepeatability); statusRepeatability=findViewById(R.id.statusRepeatability); imgRepeatability=findViewById(R.id.imgRepeatability);
        progressHighSpeed=findViewById(R.id.progressHighSpeed); statusHighSpeed=findViewById(R.id.statusHighSpeed);
        progressAdvanced=findViewById(R.id.progressAdvanced); statusAdvanced=findViewById(R.id.statusAdvanced);
        progressEasy=findViewById(R.id.progressEasy); statusEasy=findViewById(R.id.statusEasy);
        progressDiagnostic=findViewById(R.id.progressDiagnostic); statusDiagnostic=findViewById(R.id.statusDiagnostic);
        progressRoi=findViewById(R.id.progressRoi); statusRoi=findViewById(R.id.statusRoi);
        cutterProfile=findViewById(R.id.cutterProfile);
        inspectionMode=findViewById(R.id.inspectionMode); txtInspectionModeHint=findViewById(R.id.txtInspectionModeHint);
        btnReplayTop1=findViewById(R.id.btnReplayTop1); btnReplayTop2=findViewById(R.id.btnReplayTop2); btnReplayTop3=findViewById(R.id.btnReplayTop3); replayPanel=findViewById(R.id.replayPanel);
        cutterProfile.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"45° Cutter","0° Cutter","사용자 Cutter"}));
        cutterProfile.setSelection(AppStateStore.getInt(this,"profile",0));
        cutterProfile.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){AppStateStore.putInt(MainActivity.this,"profile",pos);}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        inspectionMode.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"빠른검사 · Cycle 문제 우선 (추천)","표준검사 · Cycle + Event + 진동 + Top3","정밀검사 · 전체 8개 분석"}));
        inspectionMode.setSelection(AppStateStore.getInt(this,"inspectionMode",0));
        inspectionMode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){AppStateStore.putInt(MainActivity.this,"inspectionMode",pos);updateInspectionModeHint(pos);}public void onNothingSelected(android.widget.AdapterView<?> p){}});

        findViewById(R.id.btnIntegrated).setOnClickListener(v->analyzeIntegrated());
        btnReplayTop1.setOnClickListener(v->replayWorstCycle(0));
        btnReplayTop2.setOnClickListener(v->replayWorstCycle(1));
        btnReplayTop3.setOnClickListener(v->replayWorstCycle(2));
        findViewById(R.id.btnToggleExpert).setOnClickListener(v->toggleExpert());
        findViewById(R.id.btnGuide).setOnClickListener(v->startActivity(new Intent(this,GuideActivity.class)));
        findViewById(R.id.btnSelectA).setOnClickListener(v->pickVideo(PICK_A));
        findViewById(R.id.btnSelectB).setOnClickListener(v->pickVideo(PICK_B));
        findViewById(R.id.btnPreviewA).setOnClickListener(v->preview(uriA));
        findViewById(R.id.btnPreviewB).setOnClickListener(v->preview(uriB));
        findViewById(R.id.btnAnalyze).setOnClickListener(v->analyze());
        findViewById(R.id.btnCycle).setOnClickListener(v->analyzeCycle());
        findViewById(R.id.btnRepeatability).setOnClickListener(v->analyzeRepeatability());
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
        ImageView[] zoomables={imgA,imgB,imgDiff,imgCycle,imgRepeatability,imgHighSpeed,imgRoiA,imgRoiB,imgRoiCompare,imgAdvanced,imgDiagnostic,imgEasyDiagnostic,imgTop1,imgTop2,imgTop3,imgProblemFinder,imgInspectionOverview};
        for(ImageView z:zoomables)z.setOnClickListener(v->openZoom((ImageView)v));
    }

    private void card(ProgressBar p, TextView t, int value, String message){
        runOnUiThread(()->{ p.setProgress(value); t.setText(message); });
    }
    private void dashboard(String message){ runOnUiThread(()->txtDashboard.setText(highlight(message))); }
    private android.text.SpannableString highlight(String text){
        android.text.SpannableString sp=new android.text.SpannableString(text);
        String[] yellow={"핵심 차이","확인 필요","최대 차이","Worst Cycle","문제 집중 구간","TOP1","문제 Cycle","문제 동작","기준영상 점검","검사시간","기준영상 신뢰도","비교재생","최우선","편차"};
        String[] red={"큰 차이","재현성 저하","불안정","이상 후보","불완전 Cycle"};
        String[] green={"정상 후보","양호","완료","안정"};
        for(String k:yellow){int from=0; while((from=text.indexOf(k,from))>=0){int end=from+k.length(); sp.setSpan(new android.text.style.BackgroundColorSpan(Color.rgb(255,216,61)),from,end,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); sp.setSpan(new android.text.style.ForegroundColorSpan(Color.rgb(10,16,22)),from,end,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); sp.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),from,end,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); from=end;}}
        for(String k:red){int from=0; while((from=text.indexOf(k,from))>=0){int end=from+k.length(); sp.setSpan(new android.text.style.ForegroundColorSpan(Color.rgb(255,88,104)),from,end,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); sp.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),from,end,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); from=end;}}
        for(String k:green){int from=0; while((from=text.indexOf(k,from))>=0){int end=from+k.length(); sp.setSpan(new android.text.style.ForegroundColorSpan(Color.rgb(82,229,154)),from,end,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); sp.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),from,end,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); from=end;}}
        return sp;
    }

    private void setPanelState(TextView v,int state){
        if(v==null)return;
        if(state>=3){v.setBackgroundResource(R.drawable.panel_danger);v.setTextColor(Color.rgb(255,238,240));}
        else if(state==2){v.setBackgroundResource(R.drawable.panel_warning);v.setTextColor(Color.rgb(255,244,205));}
        else if(state==1){v.setBackgroundResource(R.drawable.panel_success);v.setTextColor(Color.rgb(226,255,240));}
        else {v.setBackgroundResource(R.drawable.panel_info);v.setTextColor(Color.WHITE);}
    }

    private void updateInspectionModeHint(int mode){
        if(txtInspectionModeHint==null)return;
        if(mode==0)txtInspectionModeHint.setText("빠른검사 v1.9.2 Visual Clarity Dashboard · Fast Engine · A/B 영상을 병렬로 읽고 Batch Frame Scan을 우선 사용합니다. 문제 Cycle TOP3 + 문제 동작구간을 먼저 계산하며 픽셀 Difference/ROI 정밀검사는 생략해 시간을 단축합니다.");
        else if(mode==1)txtInspectionModeHint.setText("표준검사 · Cycle Diagnosis에 Event, 공통진동 분리, Top3/Golden을 추가합니다. 일상 점검용 권장 모드입니다.");
        else txtInspectionModeHint.setText("정밀검사 · 기존 전체 분석 8개 + ROI/Jerk까지 수행합니다. 시간이 더 걸리지만 상세 원인 확인에 적합합니다.");
    }

    private void clearAnalysisCaches(){
        cachedHighA=null;cachedHighB=null;cachedHighCompare=null;lastAdvanced=null;lastVibrationA=null;lastVibrationB=null;
        repeatA=null;repeatB=null;repeatCompare=null;cycleA=null;cycleB=null;roiA=null;roiB=null;
        frameA=null;alignedB=null;diffBitmap=null;lastTransform=null;
        if(imgInspectionOverview!=null)imgInspectionOverview.setVisibility(View.GONE);
        setReplayButtonsEnabled(false);
    }

    private void ensureHighSpeedCache() throws Exception {
        if(cachedHighA==null)cachedHighA=HighSpeedAnalyzer.analyze(this,uriA,durationA);
        if(cachedHighB==null)cachedHighB=HighSpeedAnalyzer.analyze(this,uriB,durationB);
        if(cachedHighCompare==null)cachedHighCompare=HighSpeedAnalyzer.compare(cachedHighA,cachedHighB);
    }

    private void toggleExpert(){
        View panel=findViewById(R.id.expertPanel); Button b=findViewById(R.id.btnToggleExpert);
        boolean show=panel.getVisibility()!=View.VISIBLE; panel.setVisibility(show?View.VISIBLE:View.GONE);
        b.setText(show?"▲ 전문가 상세분석 닫기":"▼ 전문가 상세분석 보기");
    }

    private String levelFromRisk(float risk){ return risk<18f?"정상 후보":risk<35f?"주의 후보":"이상 후보"; }

    private String repeatabilityState(float score){
        return score>=90f?"안정":score>=75f?"주의":"불안정";
    }

    private void applyInspectionModeVisibility(int mode){
        // 빠른/표준검사에서 실행하지 않는 무거운 결과 이미지는 공간 자체를 숨깁니다.
        imgCycle.setVisibility(mode>=2?View.VISIBLE:View.GONE);
        imgRepeatability.setVisibility(View.GONE); // 분석 완료 시 표시
        imgHighSpeed.setVisibility(mode>=1?View.VISIBLE:View.GONE);
        imgAdvanced.setVisibility(mode>=2?View.VISIBLE:View.GONE);
        imgEasyDiagnostic.setVisibility(mode>=1?View.VISIBLE:View.GONE);
        imgDiagnostic.setVisibility(mode>=1?View.VISIBLE:View.GONE);
        int roiVis=mode>=2?View.VISIBLE:View.GONE;
        imgRoiA.setVisibility(roiVis); imgRoiB.setVisibility(roiVis); imgRoiCompare.setVisibility(roiVis);
    }

    private void updateFieldDashboard(){
        float risk=lastAdvanced==null?0f:lastAdvanced.score;
        String level;
        if(lastAdvanced!=null)level=levelFromRisk(risk);
        else if(repeatB!=null){
            String bState=repeatabilityState(repeatB.repeatabilityScore);
            if(repeatA!=null && repeatA.repeatabilityScore<75f) level="A 기준 재현성 낮음 · B "+bState;
            else level="B Cycle 반복성 "+bState;
        } else level="분석 완료";
        String extra=lastAdvanced!=null?String.format(Locale.getDefault(),"  |  Motion Risk %.1f/100",risk):(repeatB!=null?String.format(Locale.getDefault(),"  |  B 재현성 Score %.0f/100 (100=안정)",repeatB.repeatabilityScore):"");
        txtOverallVerdict.setText(highlight("A 기준 대비 B 비교 · "+level+extra));
        int verdictState=0;
        if(level.contains("불안정")||level.contains("이상"))verdictState=3;
        else if(level.contains("주의")||level.contains("낮음"))verdictState=2;
        else if(level.contains("안정")||level.contains("정상"))verdictState=1;
        setPanelState(txtOverallVerdict,verdictState);
        String s1="🟢",s2="🟢",s3="🟢",s4="🟢",s5="🟢";
        if(lastAdvanced!=null){
            if(risk>=35f){s3="🔴";s4="🔴";s5="🟡";} else if(risk>=18f){s3="🟡";s4="🟡";}
        }else if(repeatB!=null){
            String mark=repeatB.repeatabilityScore<75?"🔴":"🟡";
            int st=Math.max(0,Math.min(4,repeatB.worstStageIndex));
            if(st==0)s1=mark; else if(st==1)s2=mark; else if(st==2)s3=mark; else if(st==3)s4=mark; else s5=mark;
        }
        txtStageDashboard.setText("5단계 Cutter 추정 상태\n" +
                "① "+s1+" 대기   ② "+s2+" 전진가속\n" +
                "③ "+s3+" 커팅/충격   ④ "+s4+" 복귀가속   ⑤ "+s5+" 안정화\n" +
                "※ 빠른검사는 Cycle 반복 편차가 가장 큰 동작구간을 우선 표시합니다. 센서 실측 판정은 아닙니다.");
        String diag=statusDiagnostic.getText()==null?"":statusDiagnostic.getText().toString();
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("#([123]) A ([0-9.]+)s / B ([0-9.]+)s · 편차 ([0-9.]+)%").matcher(diag);
        int count=0; double[] secA=new double[3]; double[] secB=new double[3]; String[] types=new String[3];
        StringBuilder top=new StringBuilder("A 기준 대비 B 주요 차이 TOP 3\n");
        while(m.find()&&count<3){
            secA[count]=Double.parseDouble(m.group(2));
            secB[count]=Double.parseDouble(m.group(3));
            double pct=Double.parseDouble(m.group(4));
            String type=classifyDifference(secA[count],pct,count);
            types[count]=type;
            top.append(count==0?"🥇 1위 · ":count==1?"🥈 2위 · ":"🥉 3위 · ")
               .append(type).append("\n")
               .append("   A ").append(m.group(2)).append("s ↔ B ").append(m.group(3)).append("s · 차이 ").append(m.group(4)).append("%\n")
               .append("   확인: ").append(checkPoint(type)).append("\n");
            count++;
        }
        if(count==0 && repeatB!=null && repeatB.worstCycleIndices!=null && repeatB.worstCycleIndices.length>0){
            top=new StringBuilder("빠른검사 · B 문제 Cycle TOP 3\n");
            for(int k=0;k<repeatB.worstCycleIndices.length&&k<3;k++){
                int idx=repeatB.worstCycleIndices[k];
                int st=(repeatB.worstStagePerCycle!=null&&idx>=0&&idx<repeatB.worstStagePerCycle.length)?repeatB.worstStagePerCycle[idx]:repeatB.worstStageIndex;
                top.append(k==0?"🥇 1위 · ":k==1?"🥈 2위 · ":"🥉 3위 · ").append("Cycle ").append(idx+1).append(" · ").append(CycleRepeatabilityAnalyzer.stageNameForIndex(st)).append(" · ").append(String.format(Locale.getDefault(),"%.1f%%",repeatB.worstCycleDeviationPct[k])).append("\n");
            }
        }else if(count==0) top.append("통합검사 후 무엇이 다른지 1·2·3 순위로 표시합니다.");
        txtTop3Dashboard.setText(highlight(top.toString().trim()));
        imgTop1.setVisibility(count>0?View.VISIBLE:View.GONE); imgTop2.setVisibility(count>1?View.VISIBLE:View.GONE); imgTop3.setVisibility(count>2?View.VISIBLE:View.GONE);
        if(count>0) setTopPairFrame(imgTop1,secA[0],secB[0],1,types[0]);
        if(count>1) setTopPairFrame(imgTop2,secA[1],secB[1],2,types[1]);
        if(count>2) setTopPairFrame(imgTop3,secA[2],secB[2],3,types[2]);
    }

    private String classifyDifference(double sec,double pct,int rank){
        double ratio=durationA<=0?0.5:Math.max(0.0,Math.min(1.0,sec/(durationA/1000.0)));
        if(ratio<0.18) return "초기 전진/가속 동작 패턴 차이";
        if(ratio<0.42) return "커팅/충격 구간 동작 차이";
        if(ratio<0.72) return "복귀 동작 패턴 차이";
        return "정지·안정화 잔류 움직임 차이";
    }

    private String checkPoint(String type){
        if(type.contains("커팅")) return "Cutter 충격, 순간 흔들림, 전극 영향";
        if(type.contains("복귀")) return "복귀 속도·충격과 정착 거동";
        if(type.contains("안정화")) return "복귀 후 잔류진동과 안정화시간";
        return "전진 속도 변화와 반복 궤적";
    }

    private Bitmap frameAtSecond(Uri uri,double sec){
        if(uri==null)return null;
        MediaMetadataRetriever r=new MediaMetadataRetriever();
        try{
            r.setDataSource(this,uri);
            Bitmap b=r.getFrameAtTime((long)(sec*1000000.0),MediaMetadataRetriever.OPTION_CLOSEST);
            if(b==null)b=r.getFrameAtTime((long)(sec*1000000.0),MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            return b;
        }catch(Exception ignored){return null;}finally{try{r.release();}catch(Exception ignored){}}
    }

    private void setTopPairFrame(ImageView view,double secA,double secB,int rank,String type){
        if(uriA==null||uriB==null)return;
        Bitmap a=frameAtSecond(uriA,secA);
        Bitmap b=frameAtSecond(uriB,secB);
        if(a==null||b==null)return;
        try{
            a=BitmapAnalysis.fitMaxWidth(a,960);
            b=BitmapAnalysis.fitMaxWidth(b,960);
            if(b.getWidth()!=a.getWidth()||b.getHeight()!=a.getHeight())
                b=Bitmap.createScaledBitmap(b,a.getWidth(),a.getHeight(),true);
            if(lastTransform!=null){
                try{b=BitmapAnalysis.applyTransform(b,a.getWidth(),a.getHeight(),lastTransform);}catch(Exception ignored){}
            }
            int eachW=640;
            int imageH=Math.max(260,Math.round(eachW*(a.getHeight()/(float)Math.max(1,a.getWidth()))));
            Bitmap as=Bitmap.createScaledBitmap(a,eachW,imageH,true);
            Bitmap bs=Bitmap.createScaledBitmap(b,eachW,imageH,true);
            int header=82;
            Bitmap pair=Bitmap.createBitmap(eachW*2,imageH+header,Bitmap.Config.ARGB_8888);
            Canvas c=new Canvas(pair);
            c.drawColor(Color.rgb(8,19,31));
            Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.WHITE); p.setTextSize(29f); p.setFakeBoldText(true);
            c.drawText("TOP "+rank+"   A 기준 · "+String.format(Locale.getDefault(),"%.3fs",secA),18,34,p);
            c.drawText("B 비교 · "+String.format(Locale.getDefault(),"%.3fs",secB),eachW+18,34,p);
            p.setTextSize(22f); p.setColor(Color.rgb(255,235,59));
            String shortType=type==null?"주요 동작 차이":type;
            if(shortType.length()>24)shortType=shortType.substring(0,24)+"…";
            c.drawText(shortType,18,68,p);
            p.setColor(Color.rgb(60,210,230)); p.setStrokeWidth(4f);
            c.drawLine(eachW,0,eachW,imageH+header,p);
            c.drawBitmap(as,0,header,null);
            c.drawBitmap(bs,eachW,header,null);
            view.setImageBitmap(pair);
            view.setContentDescription("TOP "+rank+" A/B 비교 이미지 · 누르면 확대");
        }catch(Exception ignored){}
    }

    private void pickVideo(int req){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("video/*"); startActivityForResult(i,req);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;
        Uri u=data.getData();
        try{ getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION); }catch(Exception ignored){}
        clearAnalysisCaches();
        if(requestCode==PICK_A){uriA=u; durationA=videoDuration(u); txtA.setText("A 기준영상 · "+u.getLastPathSegment());AppStateStore.put(this,"uriA",u.toString());}
        if(requestCode==PICK_B){uriB=u; durationB=videoDuration(u); txtB.setText("B 비교영상 · "+u.getLastPathSegment());AppStateStore.put(this,"uriB",u.toString());}
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
        btnSave.setEnabled(false); progressCompare.setProgress(5); statusCompare.setText("대표 프레임 추출 중...");
        executor.execute(()->{
            try{
                Bitmap a=frameAt(uriA,durationA,seekA.getProgress()); Bitmap b=frameAt(uriB,durationB,seekB.getProgress());
                if(a==null||b==null) throw new Exception("프레임을 읽지 못했습니다.");
                int maxW=960; a=BitmapAnalysis.fitMaxWidth(a,maxW); b=BitmapAnalysis.fitMaxWidth(b,maxW);
                Bitmap finalA=a, finalB=b;
                runOnUiThread(()->{progressCompare.setProgress(25); statusCompare.setText("촬영각도·위치 자동 보정 중... 저사양 기기에서는 수 초 걸릴 수 있습니다."); imgA.setImageBitmap(finalA);});
                BitmapAnalysis.Transform t=BitmapAnalysis.estimate(a,b);
                lastTransform=t;
                Bitmap aligned=BitmapAnalysis.applyTransform(b,a.getWidth(),a.getHeight(),t);
                runOnUiThread(()->{progressCompare.setProgress(75); statusCompare.setText("Difference Map 계산 중..."); imgB.setImageBitmap(aligned);});
                Bitmap diff=BitmapAnalysis.differenceOverlay(a,aligned);
                frameA=a; alignedB=aligned; diffBitmap=diff;
                runOnUiThread(()->{
                    progressCompare.setProgress(100); imgDiff.setImageBitmap(diff); btnSave.setEnabled(true);
                    statusCompare.setText(String.format(Locale.getDefault(),"완료 · 자동 보정값: 회전 %.2f°, 배율 %.3f, 이동 X %.1fpx / Y %.1fpx\n현재 빨간 영역은 '차이 후보'이며 불량 확정값이 아닙니다.",t.angleDeg,t.scale,t.dx,t.dy));
                });
            }catch(Exception e){ runOnUiThread(()->{progressCompare.setProgress(0); statusCompare.setText("분석 실패: "+e.getMessage());}); }
        });
    }


    private void analyzeIntegrated(){
        if(uriA==null||uriB==null){Toast.makeText(this,"A/B 영상을 모두 선택해 주세요.",Toast.LENGTH_SHORT).show();return;}
        int mode=inspectionMode==null?0:inspectionMode.getSelectedItemPosition();
        lastInspectionModeName=mode==0?"빠른검사":mode==1?"표준검사":"정밀검사";
        integratedStartElapsedMs=android.os.SystemClock.elapsedRealtime();
        lastInspectionElapsedSec=0.0;
        applyInspectionModeVisibility(mode);
        progressIntegrated.setProgress(2);
        statusIntegrated.setText("통합검사 시작 · "+lastInspectionModeName+" · v1.9.2 Fast Engine");
        txtFastHeadline.setVisibility(View.GONE);
        if(txtReferenceBanner!=null) txtReferenceBanner.setVisibility(View.GONE);
        replayPanel.setVisibility(View.GONE);
        if(imgProblemFinder!=null) imgProblemFinder.setVisibility(View.GONE);
        summaryIntegrated.setText("검사 진행 중... A/B 병렬 Frame Scan → 문제 Cycle TOP3 → 문제 동작구간 순으로 계산합니다.");
        if(mode==0)statusDiagnostic.setText("Smart Diagnostic / Top3 / Golden · 빠른검사에서는 생략");

        if(mode==0){
            // Fast mode: Cycle diagnosis does not require the expensive pixel-level camera transform/difference map.
            // Skipping it removes one full decode/registration pass while keeping A/B cycle comparison intact.
            progressCompare.setProgress(100);
            statusCompare.setText("v1.8 빠른검사 · 픽셀 Difference/촬영각 정밀보정 생략 · Cycle 내부 좌표 정규화 사용");
            integratedMark(8,"Fast Engine 준비 · A/B 병렬 Frame Scan");
        }else{
            analyze();
            integratedMark(15,"A/B 대표 프레임·촬영각 보정 완료");
        }
        analyzeRepeatability(mode==0);
        integratedMark(mode==0?94:34,"Cycle 검증 · Worst Cycle TOP3 · 문제 동작구간 완료");

        if(mode>=2){
            analyzeCycle();
            integratedMark(44,"Cycle 패턴 상세 비교 완료");
        }
        if(mode>=1){
            analyzeHighSpeed();
            integratedMark(mode==1?52:54,"High-Speed Event 패턴 완료 · Cache 공유");
        }
        if(mode>=2){
            analyzeAdvanced();
            integratedMark(66,"Timing / Jerk / Multi-Cycle 완료 · Event Cache 재사용");
        }
        if(mode>=1){
            analyzeEasyDiagnostic();
            integratedMark(mode==1?70:76,"Cutter 상대운동 / 공통진동 분리 완료");
        }
        if(mode>=2){
            analyzeRoi();
            integratedMark(86,"ROI Tip / Gripper / Nip 완료 · 카메라 보정값 재사용");
        }
        if(mode>=1){
            analyzeDiagnostic();
            integratedMark(94,"Top3 / Golden 종합진단 완료 · Event/Jerk 결과 재사용");
        }
        executor.execute(()->runOnUiThread(()->{
            progressIntegrated.setProgress(100);
            lastInspectionElapsedSec=Math.max(0.0,(android.os.SystemClock.elapsedRealtime()-integratedStartElapsedMs)/1000.0);
            statusIntegrated.setText(String.format(Locale.getDefault(),"통합검사 완료 · %s · 검사시간 %.1f초 · 문제 Cycle/동작구간 Summary 생성 완료",lastInspectionModeName,lastInspectionElapsedSec));
            buildIntegratedSummary();
        }));
    }

    private void integratedMark(int percent,String label){
        executor.execute(()->runOnUiThread(()->{progressIntegrated.setProgress(percent);statusIntegrated.setText("통합검사 진행 · "+lastInspectionModeName+" · "+label);}));
    }

    private void buildIntegratedSummary(){
        String primary="분석 결과 확인";
        if(repeatB!=null && repeatB.worstCycleIndices!=null && repeatB.worstCycleIndices.length>0){
            int wi=repeatB.worstCycleIndices[0];
            int st=(repeatB.worstStagePerCycle!=null&&wi>=0&&wi<repeatB.worstStagePerCycle.length)?repeatB.worstStagePerCycle[wi]:repeatB.worstStageIndex;
            float dev=(repeatB.worstCycleDeviationPct!=null&&repeatB.worstCycleDeviationPct.length>0)?repeatB.worstCycleDeviationPct[0]:0f;
            primary=String.format(Locale.getDefault(),"최우선: Cycle %d · %s · 편차 %.1f%%",wi+1,CycleRepeatabilityAnalyzer.stageNameForIndex(st),dev);
        }
        String ref=(repeatA!=null&&repeatA.repeatabilityScore<75f)?" · ⚠ A 기준영상 점검":"";
        summaryIntegrated.setText(highlight(String.format(Locale.getDefault(),
                "검사 완료 · %s · 검사시간 %.1f초\n%s%s\n↓ 그래프/표에서 문제 위치 확인 → TOP1~3 A/B 비교재생",
                lastInspectionModeName,lastInspectionElapsedSec,primary,ref)));
        int summaryState=0;
        if(repeatA!=null&&repeatA.repeatabilityScore<75f)summaryState=2;
        else if(repeatB!=null&&repeatB.repeatabilityScore<60f)summaryState=3;
        else if(repeatB!=null&&repeatB.repeatabilityScore<80f)summaryState=2;
        else summaryState=1;
        setPanelState(summaryIntegrated,summaryState);
        setPanelState(statusIntegrated,1);
        dashboard("통합검사 완료 · "+profileName()+" · 그래프/표 중심 요약");
        updateFastHeadline();
        updateFieldDashboard();
        updateInspectionOverviewGraphic();
    }

    private void analyzeCycle(){
        if(uriA==null||uriB==null){Toast.makeText(this,"A/B 영상을 모두 선택해 주세요.",Toast.LENGTH_SHORT).show();return;}
        progressCycle.setProgress(5); statusCycle.setText("Cycle 움직임 분석 중... (CPU 경량 분석)");
        executor.execute(()->{
            try{
                CycleAnalyzer.Result a=CycleAnalyzer.analyze(this,uriA,durationA,"A 기준영상");
                runOnUiThread(()->{progressCycle.setProgress(45); statusCycle.setText("B 비교영상 Cycle 분석 중...");});
                CycleAnalyzer.Result b=CycleAnalyzer.analyze(this,uriB,durationB,"B 비교영상");
                Bitmap cmp=CycleAnalyzer.compare(a,b);
                cycleA=a; cycleB=b; cycleBitmap=cmp;
                runOnUiThread(()->{
                    progressCycle.setProgress(100); imgCycle.setVisibility(View.VISIBLE); imgCycle.setImageBitmap(cmp); btnSave.setEnabled(true);
                    statusCycle.setText("Cycle 분석 완료\n"+a.summary+"\n\n"+b.summary); updateInspectionOverviewGraphic();
                });
            }catch(Exception e){
                runOnUiThread(()->{progressCycle.setProgress(0); statusCycle.setText("Cycle 분석 실패: "+e.getMessage());});
            }
        });
    }

    private void analyzeRepeatability(){ analyzeRepeatability(false); }

    private void analyzeRepeatability(boolean fastMode){
        if(uriA==null||uriB==null){Toast.makeText(this,"A/B 영상을 모두 선택해 주세요.",Toast.LENGTH_SHORT).show();return;}
        progressRepeatability.setProgress(5); statusRepeatability.setText((fastMode?"v1.9.2 Fast Engine · A/B 병렬 추출 · ":"")+"Cycle Diagnosis 분석 중... Cycle 자동 분리 + 경계 검증");
        executor.execute(()->{
            try{
                CycleRepeatabilityAnalyzer.Result a;
                CycleRepeatabilityAnalyzer.Result b;
                if(fastMode){
                    runOnUiThread(()->{progressRepeatability.setProgress(18);statusRepeatability.setText("v1.9.2 Fast Engine · A/B 영상 병렬 Frame Scan 중...");});
                    java.util.concurrent.ExecutorService pair=java.util.concurrent.Executors.newFixedThreadPool(2);
                    try{
                        java.util.concurrent.Future<CycleRepeatabilityAnalyzer.Result> fa=pair.submit(()->CycleRepeatabilityAnalyzer.analyze(this,uriA,durationA,"A 기준영상",true));
                        java.util.concurrent.Future<CycleRepeatabilityAnalyzer.Result> fb=pair.submit(()->CycleRepeatabilityAnalyzer.analyze(this,uriB,durationB,"B 비교영상",true));
                        a=fa.get();
                        runOnUiThread(()->{progressRepeatability.setProgress(58);statusRepeatability.setText("A/B 병렬 추출 완료 · Cycle 경계 검증 + 문제 동작구간 계산 중...");});
                        b=fb.get();
                    }finally{pair.shutdownNow();}
                }else{
                    a=CycleRepeatabilityAnalyzer.analyze(this,uriA,durationA,"A 기준영상",false);
                    runOnUiThread(()->{progressRepeatability.setProgress(48);statusRepeatability.setText("B 비교영상 Cycle 자동 분리 · 불완전 Cycle 제외 · 문제 동작구간 계산 중...");});
                    b=CycleRepeatabilityAnalyzer.analyze(this,uriB,durationB,"B 비교영상",false);
                }
                CycleRepeatabilityAnalyzer.CompareResult cr=CycleRepeatabilityAnalyzer.compare(a,b);
                repeatA=a;repeatB=b;repeatCompare=cr;repeatabilityBitmap=cr.chart;
                runOnUiThread(()->{
                    progressRepeatability.setProgress(100);imgRepeatability.setVisibility(View.VISIBLE);imgRepeatability.setImageBitmap(cr.chart);statusRepeatability.setText(a.summary+"\n\n"+b.summary+"\n\n"+cr.summary);btnSave.setEnabled(true);updateRepeatabilityDashboard();
                });
            }catch(Exception e){runOnUiThread(()->{progressRepeatability.setProgress(0);statusRepeatability.setText("Cycle 반복 재현성 분석 실패: "+e.getMessage());});}
        });
    }

    private void updateRepeatabilityDashboard(){
        if(repeatCompare==null||repeatA==null||repeatB==null){txtRepeatabilityDashboard.setText("Cycle Diagnosis · 검사 대기");setReplayButtonsEnabled(false);return;}
        String bLevel=repeatabilityState(repeatB.repeatabilityScore);
        StringBuilder ranked=new StringBuilder();
        if(repeatB.worstCycleIndices!=null){
            for(int k=0;k<repeatB.worstCycleIndices.length;k++){
                int idx=repeatB.worstCycleIndices[k]; if(idx<0)continue;
                int stage=(repeatB.worstStagePerCycle!=null&&idx<repeatB.worstStagePerCycle.length)?repeatB.worstStagePerCycle[idx]:repeatB.worstStageIndex;
                float stageDev=(repeatB.worstStageDeviationPct!=null&&idx<repeatB.worstStageDeviationPct.length)?repeatB.worstStageDeviationPct[idx]:0f;
                if(k>0)ranked.append("\n");
                ranked.append(k==0?"🥇 1위 · ":k==1?"🥈 2위 · ":"🥉 3위 · ")
                      .append("문제 Cycle ").append(idx+1).append(" — ")
                      .append(CycleRepeatabilityAnalyzer.stageNameForIndex(stage)).append("\n")
                      .append(String.format(Locale.getDefault(),"   전체 궤적 편차 %.1f%% · 해당 동작구간 편차 %.1f%%",repeatB.worstCycleDeviationPct[k],stageDev));
            }
        }
        String[] stages={"대기/초기 0~25%","전진가속 25~45%","커팅/충격 45~55%","복귀가속 55~80%","안정화 80~100%"};
        int wi=Math.max(0,Math.min(stages.length-1,repeatB.worstStageIndex));
        float stageSpread=(repeatB.stageSpreadPct!=null&&wi<repeatB.stageSpreadPct.length)?repeatB.stageSpreadPct[wi]:0f;
        String validation=repeatB.excludedCycleCount>0?"불완전 Cycle "+repeatB.excludedCycleCount+"개 자동 제외":"Cycle 경계 검증 통과";
        String text=String.format(Locale.getDefault(),
                "v1.9 Easy Problem Finder · Cycle Diagnosis · %s\nA 기준: %d Cycle · 재현성 Score %.0f/100 (100=안정) · Time CV %.1f%%\nB 비교: %d Cycle · 재현성 Score %.0f/100 (100=안정) · Time CV %.1f%% · %s\nFast Engine: A %s %.1fs / B %s %.1fs · 병렬처리\n\n어느 Cycle이 문제인가? / 어떤 동작이 문제인가?\n%s\n\n전체 문제 집중 구간: %s · 퍼짐 %.1f%%\nCycle Time Trend: %+.1f%%\n\nA↔B 평균 궤적 차이 %.1f%% · 속도패턴 차이 %.1f%% · 최대 차이 %d~%d%%",
                validation,repeatA.cycleCount,repeatA.repeatabilityScore,repeatA.cycleTimeCvPct,
                repeatB.cycleCount,repeatB.repeatabilityScore,repeatB.cycleTimeCvPct,bLevel,
                repeatA.engineName,repeatA.traceExtractSec,repeatB.engineName,repeatB.traceExtractSec,
                ranked.length()==0?"Cycle 부족":ranked.toString(),stages[wi],stageSpread,repeatB.cycleTimeTrendPct,
                repeatCompare.meanTrajectoryDifferencePct,repeatCompare.meanSpeedDifferencePct,repeatCompare.worstStartPct,repeatCompare.worstEndPct);
        if(repeatA.repeatabilityScore<75f) text += "\n\n기준영상 점검: A 기준영상 자체 재현성 Score가 낮습니다. 정상 기준영상 재선정/재촬영을 권장합니다.";
        txtRepeatabilityDashboard.setText(highlight(text));
        updateFastHeadline();
        updateProblemFinderGraphic();
        updateInspectionOverviewGraphic();
        updateReferenceBanner();
        updateReplayButtons();
    }

    private void updateFastHeadline(){
        if(txtFastHeadline==null)return;
        if(repeatB==null||repeatB.worstCycleIndices==null||repeatB.worstCycleIndices.length==0){
            txtFastHeadline.setVisibility(View.GONE);
            return;
        }
        StringBuilder sb=new StringBuilder();
        sb.append("문제 Cycle TOP3 · Cycle 번호 → 문제 동작 → 편차\n");
        int shown=0;
        for(int k=0;k<repeatB.worstCycleIndices.length&&shown<3;k++){
            int idx=repeatB.worstCycleIndices[k];
            if(idx<0||idx>=repeatB.cycleCount)continue;
            int stage=(repeatB.worstStagePerCycle!=null&&idx<repeatB.worstStagePerCycle.length)?repeatB.worstStagePerCycle[idx]:repeatB.worstStageIndex;
            float dev=(repeatB.worstCycleDeviationPct!=null&&k<repeatB.worstCycleDeviationPct.length)?repeatB.worstCycleDeviationPct[k]:0f;
            sb.append(shown==0?"🥇 ":shown==1?"🥈 ":"🥉 ")
              .append("Cycle ").append(idx+1).append(" · ")
              .append(CycleRepeatabilityAnalyzer.stageNameForIndex(stage))
              .append(String.format(Locale.getDefault()," · 편차 %.1f%%",dev)).append("\n");
            shown++;
        }
        String state;
        if(repeatB.cycleCount<3) state="Cycle 부족 · 결과 참고";
        else if(repeatA!=null&&repeatA.repeatabilityScore<75f) state="기준영상 점검 필요";
        else if(repeatB.excludedCycleCount>0) state="경계검증 완료 · 불완전 Cycle "+repeatB.excludedCycleCount+"개 제외";
        else state="Cycle 경계검증 완료";
        sb.append("→ 바로 아래 파란 TOP1~3 버튼에서 A 기준 Cycle ↔ B 문제 Cycle을 즉시 좌우 비교재생\n");
        sb.append("분석 상태: ").append(state);
        if(lastInspectionElapsedSec>0) sb.append(String.format(Locale.getDefault()," · 검사시간 %.1f초",lastInspectionElapsedSec));
        txtFastHeadline.setText(highlight(sb.toString()));
        // v1.9.1: 같은 정보가 문제지도/표에 있으므로 메인 화면에서는 긴 텍스트를 숨깁니다.
        txtFastHeadline.setVisibility(View.GONE);
    }

    private float parseMetric(String text,String regex,float fallback){
        if(text==null)return fallback;
        try{
            java.util.regex.Matcher m=java.util.regex.Pattern.compile(regex).matcher(text);
            if(m.find())return Float.parseFloat(m.group(1));
        }catch(Exception ignored){}
        return fallback;
    }

    private String compactStage(){
        if(repeatB==null||repeatB.worstCycleIndices==null||repeatB.worstCycleIndices.length==0)return "Cycle 결과 대기";
        int idx=repeatB.worstCycleIndices[0];
        int st=(repeatB.worstStagePerCycle!=null&&idx>=0&&idx<repeatB.worstStagePerCycle.length)?repeatB.worstStagePerCycle[idx]:repeatB.worstStageIndex;
        return "C"+(idx+1)+" · "+CycleRepeatabilityAnalyzer.stageNameForIndex(st);
    }

    private String riskWord(float v){
        if(v<18f)return "낮음";
        if(v<35f)return "중간";
        return "큼";
    }

    private void updateInspectionOverviewGraphic(){
        if(imgInspectionOverview==null)return;
        final int n=8,w=840,h=1120;
        String[] names={"A 기준 신뢰도","B Cycle 반복성","A↔B 평균 궤적","고속 Event","Timing / Jerk","공통진동 / 안정화","Smart / Golden","ROI Tip / Nip"};
        boolean[] on=new boolean[n];
        float[] risk=new float[n];
        String[] key=new String[n];

        if(repeatA!=null){on[0]=true;risk[0]=Math.max(0f,100f-repeatA.repeatabilityScore);key[0]=String.format(Locale.getDefault(),"A Score %.0f/100",repeatA.repeatabilityScore);}
        if(repeatB!=null){on[1]=true;risk[1]=Math.max(0f,100f-repeatB.repeatabilityScore);key[1]=compactStage();}
        if(repeatCompare!=null){on[2]=true;risk[2]=Math.min(100f,repeatCompare.meanTrajectoryDifferencePct);key[2]="최대 "+repeatCompare.worstStartPct+"~"+repeatCompare.worstEndPct+"%";}
        if(cachedHighCompare!=null){on[3]=true;risk[3]=Math.min(100f,parseMetric(cachedHighCompare.summary,"Event 동기화 패턴 편차 ([0-9.]+)/100",0f));key[3]=String.format(Locale.getDefault(),"패턴 %.1f/100",risk[3]);}
        if(lastAdvanced!=null){on[4]=true;risk[4]=Math.min(100f,lastAdvanced.score);key[4]=String.format(Locale.getDefault(),"Motion %.1f/100",lastAdvanced.score);}
        if(lastVibrationB!=null){on[5]=true;risk[5]=Math.min(100f,lastVibrationB.risk);key[5]=String.format(Locale.getDefault(),"안정화 %.0fms",lastVibrationB.settleMs);}
        if(diagnosticBitmap!=null){on[6]=true;risk[6]=Math.min(100f,parseMetric(statusDiagnostic.getText()==null?"":statusDiagnostic.getText().toString(),"Motion Risk ([0-9.]+)/100",lastAdvanced==null?0f:lastAdvanced.score));GoldenBaselineStore.Baseline g=GoldenBaselineStore.get(this,profileName());key[6]=g==null?"Golden 미등록":"Golden 비교 완료";}
        if(roiB!=null){on[7]=true;risk[7]=Math.max(0f,Math.min(100f,100f-roiB.qualityScore));key[7]=String.format(Locale.getDefault(),"Offset %.1fpx · Jitter %.1f",roiB.nipOffsetPx,roiB.tipJitterPx);}

        Bitmap bm=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(bm); Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        c.drawColor(Color.rgb(5,14,25));
        p.setColor(Color.rgb(34,224,255));p.setTextSize(38);p.setFakeBoldText(true);c.drawText("전체 검사항목 한눈 요약",32,52,p);
        p.setFakeBoldText(false);p.setTextSize(22);p.setColor(Color.rgb(190,214,235));
        c.drawText("큰 차이부터 확인 → 아래 표에서 핵심 원인/확인 포인트",32,84,p);

        int left=255,right=802,top=120,row=70,barH=34;
        for(int i=0;i<n;i++){
            int y=top+i*row;
            p.setTextSize(23);p.setColor(Color.rgb(240,248,255));p.setFakeBoldText(true);c.drawText(names[i],32,y+25,p);p.setFakeBoldText(false);
            p.setColor(Color.rgb(31,53,76));c.drawRoundRect(left,y,right,y+barH,12,12,p);
            if(on[i]){
                float r=Math.max(0f,Math.min(100f,risk[i]));
                int col=r<18f?Color.rgb(82,229,154):r<35f?Color.rgb(255,170,61):Color.rgb(255,88,104);
                p.setColor(col);c.drawRoundRect(left,y,left+(right-left)*Math.max(.035f,r/100f),y+barH,12,12,p);
                p.setColor(Color.WHITE);p.setTextSize(22);p.setFakeBoldText(true);c.drawText(String.format(Locale.getDefault(),"%.0f",r),right-50,y+25,p);p.setFakeBoldText(false);
            }else{
                p.setColor(Color.rgb(145,165,185));p.setTextSize(19);c.drawText("미실행",right-74,y+24,p);
            }
        }

        int tableTop=705;
        p.setColor(Color.rgb(34,224,255));p.setTextSize(29);p.setFakeBoldText(true);c.drawText("우선 확인 TOP 4",32,tableTop-15,p);p.setFakeBoldText(false);
        p.setColor(Color.rgb(18,48,75));c.drawRoundRect(30,tableTop,810,tableTop+48,9,9,p);
        p.setTextSize(19);p.setFakeBoldText(true);p.setColor(Color.WHITE);
        c.drawText("검사항목",44,tableTop+31,p);c.drawText("상태",335,tableTop+31,p);c.drawText("핵심 확인",465,tableTop+31,p);p.setFakeBoldText(false);
        java.util.ArrayList<Integer> ids=new java.util.ArrayList<>();
        for(int i=0;i<n;i++)if(on[i])ids.add(i);
        java.util.Collections.sort(ids,(a,b)->Float.compare(risk[b],risk[a]));
        int shown=0;
        for(int q=0;q<ids.size()&&shown<4;q++,shown++){
            int i=ids.get(q),y=tableTop+55+shown*73;
            p.setColor(shown%2==0?Color.rgb(14,31,48):Color.rgb(18,39,59));c.drawRoundRect(30,y,810,y+65,9,9,p);
            p.setTextSize(21);p.setColor(Color.WHITE);p.setFakeBoldText(shown==0);c.drawText(names[i],44,y+40,p);p.setFakeBoldText(false);
            float r=risk[i];int col=r<18f?Color.rgb(82,229,154):r<35f?Color.rgb(255,216,61):Color.rgb(255,88,104);
            p.setColor(col);p.setFakeBoldText(true);c.drawText(riskWord(r),335,y+40,p);p.setFakeBoldText(false);
            p.setColor(Color.rgb(224,237,249));p.setTextSize(18);String k=key[i]==null?"확인":key[i];if(k.length()>24)k=k.substring(0,24)+"…";c.drawText(k,465,y+40,p);
        }
        p.setTextSize(17);p.setColor(Color.rgb(156,181,205));
        c.drawText("※ 색은 상대 차이/우선순위 표시입니다. 검증된 NG 기준 전에는 확정 불량 판정이 아닙니다.",32,1092,p);
        imgInspectionOverview.setImageBitmap(bm);
        imgInspectionOverview.setVisibility(View.VISIBLE);
    }

    private void updateProblemFinderGraphic(){
        if(imgProblemFinder==null)return;
        if(repeatB==null||repeatB.cycleCount<1){imgProblemFinder.setVisibility(View.GONE);return;}
        final int w=840,h=900;
        Bitmap bm=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(bm); Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        c.drawColor(Color.rgb(5,14,25));
        p.setColor(Color.rgb(34,224,255));p.setTextSize(38);p.setFakeBoldText(true);c.drawText("한눈에 보는 문제 지도",32,52,p);
        p.setFakeBoldText(false);p.setTextSize(21);p.setColor(Color.rgb(190,214,235));
        c.drawText("어느 동작이 다른가 → 어느 Cycle이 문제인가",32,84,p);

        String[] labels={"대기/초기","전진가속","커팅/충격","복귀가속","안정화"};
        int[] start={0,25,45,55,80}, end={25,45,55,80,100};
        float max=1f;if(repeatB.stageSpreadPct!=null)for(float v:repeatB.stageSpreadPct)max=Math.max(max,v);
        int chartL=210,chartR=800,top=125,rowH=72,barH=36;
        p.setTextSize(25);p.setFakeBoldText(true);p.setColor(Color.WHITE);c.drawText("동작별 차이",32,123,p);p.setFakeBoldText(false);
        for(int i=0;i<5;i++){
            float v=repeatB.stageSpreadPct!=null&&i<repeatB.stageSpreadPct.length?repeatB.stageSpreadPct[i]:0f;
            int y=top+32+i*rowH;
            p.setTextSize(23);p.setColor(Color.rgb(240,248,255));p.setFakeBoldText(true);c.drawText(labels[i],32,y+25,p);p.setFakeBoldText(false);
            p.setTextSize(16);p.setColor(Color.rgb(145,170,194));c.drawText(start[i]+"~"+end[i]+"%",122,y+24,p);
            p.setColor(Color.rgb(31,53,76));c.drawRoundRect(chartL,y,chartR,y+barH,12,12,p);
            float frac=Math.min(1f,v/max);boolean worst=i==repeatB.worstStageIndex;
            p.setColor(worst?Color.rgb(255,170,61):Color.rgb(54,172,235));c.drawRoundRect(chartL,y,chartL+(chartR-chartL)*Math.max(.035f,frac),y+barH,12,12,p);
            p.setTextSize(20);p.setFakeBoldText(true);p.setColor(worst?Color.rgb(255,232,150):Color.WHITE);c.drawText(String.format(Locale.getDefault(),"%.1f%%",v),chartR-73,y+26,p);p.setFakeBoldText(false);
        }

        int tableTop=545;
        p.setColor(Color.rgb(34,224,255));p.setTextSize(27);p.setFakeBoldText(true);c.drawText("문제 Cycle TOP3",32,tableTop-14,p);p.setFakeBoldText(false);
        p.setColor(Color.rgb(18,48,75));c.drawRoundRect(30,tableTop,810,tableTop+48,9,9,p);
        int[] cols={30,115,220,515,665,810};String[] heads={"TOP","Cycle","문제 동작","전체","구간"};
        p.setTextSize(18);p.setFakeBoldText(true);p.setColor(Color.WHITE);for(int i=0;i<heads.length;i++)c.drawText(heads[i],cols[i]+10,tableTop+31,p);p.setFakeBoldText(false);
        for(int k=0;k<3;k++){
            int y=tableTop+55+k*78;p.setColor(k%2==0?Color.rgb(14,31,48):Color.rgb(18,39,59));c.drawRoundRect(30,y,810,y+69,9,9,p);
            if(repeatB.worstCycleIndices==null||k>=repeatB.worstCycleIndices.length||repeatB.worstCycleIndices[k]<0)continue;
            int idx=repeatB.worstCycleIndices[k];int stage=(repeatB.worstStagePerCycle!=null&&idx<repeatB.worstStagePerCycle.length)?repeatB.worstStagePerCycle[idx]:repeatB.worstStageIndex;
            float total=(repeatB.worstCycleDeviationPct!=null&&k<repeatB.worstCycleDeviationPct.length)?repeatB.worstCycleDeviationPct[k]:0f;float local=(repeatB.worstStageDeviationPct!=null&&idx<repeatB.worstStageDeviationPct.length)?repeatB.worstStageDeviationPct[idx]:0f;
            p.setTextSize(22);p.setFakeBoldText(true);p.setColor(k==0?Color.rgb(255,216,61):Color.WHITE);c.drawText(String.valueOf(k+1),cols[0]+22,y+43,p);c.drawText(String.valueOf(idx+1),cols[1]+25,y+43,p);
            c.drawText(CycleRepeatabilityAnalyzer.stageNameForIndex(stage),cols[2]+10,y+43,p);c.drawText(String.format(Locale.getDefault(),"%.1f%%",total),cols[3]+10,y+43,p);c.drawText(String.format(Locale.getDefault(),"%.1f%%",local),cols[4]+10,y+43,p);p.setFakeBoldText(false);
        }
        p.setTextSize(17);p.setColor(Color.rgb(156,181,205));c.drawText("TOP3 버튼을 누르면 실제 A/B Cycle 영상을 즉시 비교합니다.",32,865,p);
        imgProblemFinder.setImageBitmap(bm);imgProblemFinder.setVisibility(View.VISIBLE);
    }

    private void updateReferenceBanner(){
        if(txtReferenceBanner==null){ return; }
        if(repeatA==null || repeatA.cycleCount<2){
            txtReferenceBanner.setVisibility(View.GONE);
            return;
        }
        if(repeatA.repeatabilityScore < 75f){
            txtReferenceBanner.setText(highlight(String.format(Locale.getDefault(),
                    "⚠ 기준영상 신뢰도 낮음 · A 재현성 Score %.0f/100\n정상 기준영상으로 사용하기 전에 재촬영/재선정을 권장합니다.",
                    repeatA.repeatabilityScore)));
            txtReferenceBanner.setTextColor(Color.rgb(255,216,61));
            txtReferenceBanner.setBackgroundResource(R.drawable.panel_warning);
        }else{
            txtReferenceBanner.setText(String.format(Locale.getDefault(),
                    "✓ 기준영상 반복성 양호 · A 재현성 Score %.0f/100 · %d Cycle 검증",
                    repeatA.repeatabilityScore, repeatA.cycleCount));
            txtReferenceBanner.setTextColor(Color.rgb(82,229,154));
            txtReferenceBanner.setBackgroundResource(R.drawable.panel_success);
        }
        txtReferenceBanner.setVisibility(View.VISIBLE);
    }

    private void setReplayButtonsEnabled(boolean enabled){
        btnReplayTop1.setEnabled(enabled);btnReplayTop2.setEnabled(enabled);btnReplayTop3.setEnabled(enabled);
        if(replayPanel!=null) replayPanel.setVisibility(enabled?View.VISIBLE:View.GONE);
    }

    private void updateReplayButtons(){
        setReplayButtonsEnabled(false);
        if(repeatA==null||repeatB==null||repeatB.worstCycleIndices==null)return;
        Button[] buttons={btnReplayTop1,btnReplayTop2,btnReplayTop3};
        boolean anyValid=false;
        for(int k=0;k<buttons.length;k++){
            buttons[k].setVisibility(View.GONE);
            if(k>=repeatB.worstCycleIndices.length)continue;
            int idx=repeatB.worstCycleIndices[k]; if(idx<0||idx>=repeatB.cycleCount)continue;
            int stage=(repeatB.worstStagePerCycle!=null&&idx<repeatB.worstStagePerCycle.length)?repeatB.worstStagePerCycle[idx]:repeatB.worstStageIndex;
            float dev=(repeatB.worstCycleDeviationPct!=null&&k<repeatB.worstCycleDeviationPct.length)?repeatB.worstCycleDeviationPct[k]:0f;
            String medal=k==0?"🥇 TOP1":k==1?"🥈 TOP2":"🥉 TOP3";
            buttons[k].setText(medal+" · Cycle "+(idx+1)+" · "+CycleRepeatabilityAnalyzer.stageNameForIndex(stage)+String.format(Locale.getDefault()," · 편차 %.1f%%\n▶ A 기준 ↔ B 문제 Cycle 바로 비교재생",dev));
            buttons[k].setVisibility(View.VISIBLE);
            buttons[k].setEnabled(true);
            anyValid=true;
        }
        if(replayPanel!=null) replayPanel.setVisibility(anyValid?View.VISIBLE:View.GONE);
    }

    private void replayWorstCycle(int rank){
        if(repeatA==null||repeatB==null||uriA==null||uriB==null||repeatB.worstCycleIndices==null||rank<0||rank>=repeatB.worstCycleIndices.length){Toast.makeText(this,"먼저 Cycle Diagnosis 분석을 실행해 주세요.",Toast.LENGTH_SHORT).show();return;}
        int bIdx=repeatB.worstCycleIndices[rank];
        int aIdx=repeatA.bestCycleIndex>=0?repeatA.bestCycleIndex:0;
        if(bIdx<0||aIdx<0||bIdx>=repeatB.cycleStartSec.length||aIdx>=repeatA.cycleStartSec.length){Toast.makeText(this,"재생 가능한 Cycle 구간이 부족합니다.",Toast.LENGTH_SHORT).show();return;}
        int stage=(repeatB.worstStagePerCycle!=null&&bIdx<repeatB.worstStagePerCycle.length)?repeatB.worstStagePerCycle[bIdx]:repeatB.worstStageIndex;
        Intent i=new Intent(this,CycleReplayActivity.class);
        i.putExtra("uriA",uriA.toString());i.putExtra("uriB",uriB.toString());
        i.putExtra("aStart",repeatA.cycleStartSec[aIdx]);i.putExtra("aEnd",repeatA.cycleEndSec[aIdx]);
        i.putExtra("bStart",repeatB.cycleStartSec[bIdx]);i.putExtra("bEnd",repeatB.cycleEndSec[bIdx]);
        i.putExtra("aCycle",aIdx+1);i.putExtra("bCycle",bIdx+1);i.putExtra("rank",rank+1);
        i.putExtra("stage",CycleRepeatabilityAnalyzer.stageNameForIndex(stage));
        i.putExtra("deviation",repeatB.worstCycleDeviationPct[rank]);
        startActivity(i);
    }


    private void analyzeHighSpeed(){
        if(uriA==null||uriB==null){Toast.makeText(this,"A/B 영상을 모두 선택해 주세요.",Toast.LENGTH_SHORT).show();return;}
        progressHighSpeed.setProgress(5);statusHighSpeed.setText("v0.5 고속 Event 동기화 분석 중... (동일 영상 재분석 시 Cache 재사용)");
        executor.execute(()->{try{ensureHighSpeedCache();HighSpeedAnalyzer.CompareResult cr=cachedHighCompare;highSpeedBitmap=cr.chart;runOnUiThread(()->{progressHighSpeed.setProgress(100);imgHighSpeed.setVisibility(View.VISIBLE);imgHighSpeed.setImageBitmap(cr.chart);statusHighSpeed.setText(cr.summary+"\n※ 동일 영상 재분석 시 Cache를 재사용합니다.");btnSave.setEnabled(true);updateInspectionOverviewGraphic();});}catch(Exception e){runOnUiThread(()->statusHighSpeed.setText("고속 분석 실패: "+e.getMessage()));}});
    }


    private String profileName(){Object o=cutterProfile.getSelectedItem();return o==null?"45° Cutter":o.toString();}
    private void analyzeAdvanced(){
        if(uriA==null||uriB==null){Toast.makeText(this,"A/B 영상을 모두 선택해 주세요.",Toast.LENGTH_SHORT).show();return;}
        progressAdvanced.setProgress(5);statusAdvanced.setText("v0.6 Multi-Cycle / Jerk / Timing 분석 중...");
        executor.execute(()->{try{
            ensureHighSpeedCache();
            HighSpeedAnalyzer.Result a=cachedHighA;
            HighSpeedAnalyzer.Result b=cachedHighB;
            String pf=profileName();float[] history=TrendStore.get(this,pf);
            AdvancedMotionAnalyzer.Result ar=AdvancedMotionAnalyzer.analyze(a,b,pf,history);
            TrendStore.add(this,pf,ar.score);advancedBitmap=ar.chart; lastAdvanced=ar;
            runOnUiThread(()->{progressAdvanced.setProgress(100);imgAdvanced.setVisibility(View.VISIBLE);imgAdvanced.setImageBitmap(ar.chart);statusAdvanced.setText(ar.summary+"\n\n이미지/그래프를 누르면 전체화면 확대가 됩니다.");btnSave.setEnabled(true);updateInspectionOverviewGraphic();});
        }catch(Exception e){runOnUiThread(()->{progressAdvanced.setProgress(0);statusAdvanced.setText("v0.6 고급분석 실패: "+e.getMessage());});}});
    }

    private void analyzeEasyDiagnostic(){
        if(uriA==null||uriB==null){Toast.makeText(this,"A/B 영상을 모두 선택해 주세요.",Toast.LENGTH_SHORT).show();return;}
        progressEasy.setProgress(5);statusEasy.setText("v0.8 고정부 공통진동과 Cutter 실제운동을 분리 분석 중...");
        executor.execute(()->{try{
            VibrationCompensatedAnalyzer.Result a=VibrationCompensatedAnalyzer.analyze(this,uriA,durationA,profileName()+" / A");
            runOnUiThread(()->progressEasy.setProgress(50));
            VibrationCompensatedAnalyzer.Result b=VibrationCompensatedAnalyzer.analyze(this,uriB,durationB,profileName()+" / B");
            lastVibrationA=a; lastVibrationB=b;
            // Show B as current/evaluation machine; status contains both for immediate A/B interpretation.
            easyDiagnosticBitmap=b.chart;
            String verdict = "[설비 A]\n" + a.summary
                    + "\n\n[설비 B]\n" + b.summary
                    + "\n\n쉽게 보기: 회색 공통진동이 커져도 파란 Cutter 상대운동이 안정적이면 고정부 흔들림 영향으로 봅니다.";
            runOnUiThread(()->{progressEasy.setProgress(100);imgEasyDiagnostic.setVisibility(View.VISIBLE);imgEasyDiagnostic.setImageBitmap(b.chart);statusEasy.setText(verdict); dashboard("종합 진단 · "+profileName()+"\nCutter/고정부 진동 분리 분석 완료");btnSave.setEnabled(true);updateInspectionOverviewGraphic();});
        }catch(Exception e){runOnUiThread(()->{progressEasy.setProgress(0);statusEasy.setText("v0.8 진동분리 분석 실패: "+e.getMessage());});}});
    }

    private void analyzeDiagnostic(){
        if(uriA==null||uriB==null){Toast.makeText(this,"A/B 영상을 모두 선택해 주세요.",Toast.LENGTH_SHORT).show();return;}
        progressDiagnostic.setProgress(5);statusDiagnostic.setText("v0.7 Smart Diagnostic · Top3 이상 순간 / Golden 비교 중...");
        executor.execute(()->{try{
            ensureHighSpeedCache();
            HighSpeedAnalyzer.Result a=cachedHighA;
            HighSpeedAnalyzer.Result b=cachedHighB;
            String pf=profileName(); AdvancedMotionAnalyzer.Result ar=lastAdvanced!=null?lastAdvanced:AdvancedMotionAnalyzer.analyze(a,b,pf,TrendStore.get(this,pf)); lastAdvanced=ar;
            GoldenBaselineStore.Baseline g=GoldenBaselineStore.get(this,pf); float mm=CalibrationStore.get(this,pf);
            DiagnosticAnalyzer.Result dr=DiagnosticAnalyzer.analyze(a,b,ar,roiB,g,mm,pf); diagnosticBitmap=dr.chart;
            runOnUiThread(()->{progressDiagnostic.setProgress(100);imgDiagnostic.setVisibility(View.VISIBLE);imgDiagnostic.setImageBitmap(dr.chart);statusDiagnostic.setText(dr.summary+"\n\n그래프를 누르면 전체화면 확대됩니다."); dashboard("종합 진단 · "+profileName()+" · Smart Diagnostic 완료");btnSave.setEnabled(true);updateInspectionOverviewGraphic();});
        }catch(Exception e){runOnUiThread(()->{progressDiagnostic.setProgress(0);statusDiagnostic.setText("통합 진단 실패: "+e.getMessage());});}});
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
        else if(view==imgCycle)b=cycleBitmap; else if(view==imgRepeatability)b=repeatabilityBitmap; else if(view==imgHighSpeed)b=highSpeedBitmap; else if(view==imgAdvanced)b=advancedBitmap; else if(view==imgDiagnostic)b=diagnosticBitmap; else if(view==imgEasyDiagnostic)b=easyDiagnosticBitmap;
        else if(view.getDrawable() instanceof android.graphics.drawable.BitmapDrawable)b=((android.graphics.drawable.BitmapDrawable)view.getDrawable()).getBitmap();
        if(b==null)return;
        try{java.io.File f=new java.io.File(getCacheDir(),"zoom_result.jpg");java.io.FileOutputStream os=new java.io.FileOutputStream(f);b.compress(Bitmap.CompressFormat.JPEG,95,os);os.close();Intent i=new Intent(this,ZoomImageActivity.class);i.putExtra("path",f.getAbsolutePath());startActivity(i);}catch(Exception e){Toast.makeText(this,"확대 열기 실패",Toast.LENGTH_SHORT).show();}
    }

    private void analyzeRoi(){
        if(uriA==null||uriB==null){Toast.makeText(this,"A/B 영상을 모두 선택해 주세요.",Toast.LENGTH_SHORT).show();return;}
        progressRoi.setProgress(5); statusRoi.setText("v0.3 ROI 정밀분석 준비 중...");
        executor.execute(()->{
            try{
                // Reuse the representative-frame camera transform when the integrated inspection already calculated it.
                Bitmap a=frameA;
                BitmapAnalysis.Transform t=lastTransform;
                if(a==null||alignedB==null||t==null){
                    a=frameAt(uriA,durationA,seekA.getProgress());
                    Bitmap b=frameAt(uriB,durationB,seekB.getProgress());
                    if(a==null||b==null) throw new Exception("프레임을 읽지 못했습니다.");
                    a=BitmapAnalysis.fitMaxWidth(a,960); b=BitmapAnalysis.fitMaxWidth(b,960);
                    if(b.getWidth()!=a.getWidth()||b.getHeight()!=a.getHeight()) b=Bitmap.createScaledBitmap(b,a.getWidth(),a.getHeight(),true);
                    t=BitmapAnalysis.estimate(a,b);
                    lastTransform=t; frameA=a; alignedB=BitmapAnalysis.applyTransform(b,a.getWidth(),a.getHeight(),t); diffBitmap=BitmapAnalysis.differenceOverlay(a,alignedB);
                }
                final int w=a.getWidth(), h=a.getHeight();
                final BitmapAnalysis.Transform roiTransform=t;
                runOnUiThread(()->{progressRoi.setProgress(25); statusRoi.setText("설비 A: 전극 선단 / Gripper / Nip ROI 추적 중...");});

                // A is reference, so no camera transform.
                RoiQualityAnalyzer.Metrics ma=RoiQualityAnalyzer.analyze(
                        this,uriA,durationA,seekA.getProgress(),null,w,h,"설비 A");

                runOnUiThread(()->{progressRoi.setProgress(55); statusRoi.setText("설비 B: 촬영각 보정 후 ROI 추적 중...");});
                RoiQualityAnalyzer.Metrics mb=RoiQualityAnalyzer.analyze(
                        this,uriB,durationB,seekB.getProgress(),roiTransform,w,h,"설비 B (보정)");

                Bitmap cmp=RoiQualityAnalyzer.compare(ma,mb);
                roiA=ma; roiB=mb; roiCompareBitmap=cmp;

                runOnUiThread(()->{
                    progressRoi.setProgress(100);
                    imgRoiA.setVisibility(View.VISIBLE);imgRoiA.setImageBitmap(ma.overlay);
                    imgRoiB.setVisibility(View.VISIBLE);imgRoiB.setImageBitmap(mb.overlay);
                    imgRoiCompare.setVisibility(View.VISIBLE);imgRoiCompare.setImageBitmap(cmp);
                    btnSave.setEnabled(true);
                    statusRoi.setText("v0.4 ROI 분석 완료\n"+ma.summary+"\n\n"+mb.summary+
                            String.format(Locale.getDefault(),
                                    "\n\n카메라 보정: 회전 %.2f°, 배율 %.3f, X %.1fpx / Y %.1fpx"+
                                    "\n※ 현재 px/Score는 설비 변화 추세용 참고값입니다. 실제 mm 및 NG 기준은 Calibration/Golden 데이터로 확정해야 합니다.",
                                    roiTransform.angleDeg,roiTransform.scale,roiTransform.dx,roiTransform.dy));
                    updateInspectionOverviewGraphic();
                });
            }catch(Exception e){
                runOnUiThread(()->{progressRoi.setProgress(0);statusRoi.setText("ROI 분석 실패: "+e.getMessage());});
            }
        });
    }

    private Bitmap composeResult(){
        if(frameA==null||alignedB==null||diffBitmap==null)return null;
        int w=frameA.getWidth(), gap=20, titleH=80, h=frameA.getHeight();
        int cycleExtra=(cycleBitmap==null?0:Math.round((w*3f)*cycleBitmap.getHeight()/cycleBitmap.getWidth())+gap);
        int repeatExtra=(repeatabilityBitmap==null?0:Math.round((w*3f)*repeatabilityBitmap.getHeight()/repeatabilityBitmap.getWidth())+gap);
        int roiExtra=(roiCompareBitmap==null?0:Math.round((w*3f)*roiCompareBitmap.getHeight()/roiCompareBitmap.getWidth())+gap);
        int advExtra=(advancedBitmap==null?0:Math.round((w*3f)*advancedBitmap.getHeight()/advancedBitmap.getWidth())+gap);
        int diagExtra=(diagnosticBitmap==null?0:Math.round((w*3f)*diagnosticBitmap.getHeight()/diagnosticBitmap.getWidth())+gap);
        Bitmap out=Bitmap.createBitmap(w*3+gap*4,h+titleH+gap*2+cycleExtra+repeatExtra+roiExtra+advExtra+diagExtra,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(out); c.drawColor(Color.WHITE); Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); p.setColor(Color.rgb(12,45,87)); p.setTextSize(34); p.setFakeBoldText(true);
        c.drawText("전극 컷팅 A/B 비교 · A / 보정 B / Difference",gap,52,p);
        c.drawBitmap(frameA,gap,titleH+gap,p); c.drawBitmap(alignedB,w+gap*2,titleH+gap,p); c.drawBitmap(diffBitmap,w*2+gap*3,titleH+gap,p);
        int y=h+titleH+gap*2;
        if(cycleBitmap!=null){
            int cw=w*3+gap*2; int ch=Math.round(cw*(cycleBitmap.getHeight()/(float)cycleBitmap.getWidth()));
            Bitmap scaled=Bitmap.createScaledBitmap(cycleBitmap,cw,ch,true);
            c.drawBitmap(scaled,gap,y,p); y+=ch+gap;
        }
        if(repeatabilityBitmap!=null){
            int cw=w*3+gap*2; int ch=Math.round(cw*(repeatabilityBitmap.getHeight()/(float)repeatabilityBitmap.getWidth()));
            Bitmap scaled=Bitmap.createScaledBitmap(repeatabilityBitmap,cw,ch,true);
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

package com.example.electrodecutcompare;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

public class CycleReplayActivity extends Activity {
    private TextureView textureA, textureB;
    private CycleProgressView progressView;
    private TextView progressText, modeNote, problemAlert;
    private Button pauseButton, syncButton, problemLoopButton;
    private MediaPlayer playerA, playerB;
    private Surface surfaceA, surfaceB;
    private Uri uriA, uriB;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int aStartMs, aEndMs, bStartMs, bEndMs;
    private int stageIndex = 2;
    private float problemStartPct = .45f, problemEndPct = .55f;
    private boolean preparedA=false, preparedB=false, paused=false;
    private boolean syncMode=true, problemLoop=false, restarting=false;
    private long lastSyncCorrectionMs=0L;

    private final Runnable loopCheck = new Runnable() {
        @Override public void run() {
            try {
                if (preparedA && preparedB && playerA != null && playerB != null) {
                    float pa = normalizedProgress(playerA.getCurrentPosition(), aStartMs, aEndMs);
                    float pb = normalizedProgress(playerB.getCurrentPosition(), bStartMs, bEndMs);
                    progressView.setProgress(pa,pb);
                    float delta=Math.abs(pa-pb)*100f;
                    progressText.setText(String.format(Locale.getDefault(),
                            "Cycle 진행률 · A %.0f%% | B %.0f%% | Δ %.0f%%",
                            pa*100f,pb*100f,delta));

                    boolean inProblem=(pa>=problemStartPct&&pa<=problemEndPct)||(pb>=problemStartPct&&pb<=problemEndPct);
                    if(inProblem){
                        problemAlert.setText("⚠ 지금 문제구간 진입 · "+stageShortName(stageIndex)+String.format(Locale.getDefault()," %.0f~%.0f%%",problemStartPct*100f,problemEndPct*100f));
                        problemAlert.setBackgroundResource(R.drawable.panel_danger);
                        problemAlert.setTextColor(android.graphics.Color.WHITE);
                    }else{
                        problemAlert.setText("문제구간 · "+stageShortName(stageIndex)+String.format(Locale.getDefault()," %.0f~%.0f%%",problemStartPct*100f,problemEndPct*100f));
                        problemAlert.setBackgroundResource(R.drawable.panel_warning);
                        problemAlert.setTextColor(android.graphics.Color.rgb(255,240,168));
                    }

                    if(!paused && !restarting){
                        int aLoopStart=loopStartA(), aLoopEnd=loopEndA();
                        int bLoopStart=loopStartB(), bLoopEnd=loopEndB();
                        if(playerA.getCurrentPosition()>=aLoopEnd-18 || playerB.getCurrentPosition()>=bLoopEnd-18){
                            restartBoth();
                        }else if(syncMode){
                            long now=SystemClock.elapsedRealtime();
                            if(delta>5f && now-lastSyncCorrectionMs>300){
                                float target=(pa+pb)*0.5f;
                                if(problemLoop) target=Math.max(problemStartPct,Math.min(problemEndPct,target));
                                seekPlayer(playerA,mapProgress(target,aStartMs,aEndMs));
                                seekPlayer(playerB,mapProgress(target,bStartMs,bEndMs));
                                lastSyncCorrectionMs=now;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            handler.postDelayed(this,45);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if(android.os.Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(true);
        setContentView(R.layout.activity_cycle_replay);

        textureA=findViewById(R.id.videoReplayA);
        textureB=findViewById(R.id.videoReplayB);
        progressView=findViewById(R.id.cycleProgressView);
        progressText=findViewById(R.id.txtReplayProgress);
        modeNote=findViewById(R.id.txtReplayModeNote);
        problemAlert=findViewById(R.id.txtProblemAlert);
        pauseButton=findViewById(R.id.btnReplayPause);
        syncButton=findViewById(R.id.btnReplaySync);
        problemLoopButton=findViewById(R.id.btnProblemLoop);

        TextView title=findViewById(R.id.txtReplayTitle);
        TextView info=findViewById(R.id.txtReplayInfo);
        TextView labelA=findViewById(R.id.txtReplayA);
        TextView labelB=findViewById(R.id.txtReplayB);

        String ua=getIntent().getStringExtra("uriA");
        String ub=getIntent().getStringExtra("uriB");
        if(ua!=null)uriA=Uri.parse(ua);
        if(ub!=null)uriB=Uri.parse(ub);
        aStartMs=Math.max(0,Math.round(getIntent().getFloatExtra("aStart",0f)*1000f));
        aEndMs=Math.max(aStartMs+80,Math.round(getIntent().getFloatExtra("aEnd",0f)*1000f));
        bStartMs=Math.max(0,Math.round(getIntent().getFloatExtra("bStart",0f)*1000f));
        bEndMs=Math.max(bStartMs+80,Math.round(getIntent().getFloatExtra("bEnd",0f)*1000f));

        int aCycle=getIntent().getIntExtra("aCycle",1);
        int bCycle=getIntent().getIntExtra("bCycle",1);
        int rank=getIntent().getIntExtra("rank",1);
        String stage=getIntent().getStringExtra("stage");
        if(stage==null)stage="문제 동작구간";
        stageIndex=Math.max(0,Math.min(4,getIntent().getIntExtra("stageIndex",2)));
        float dev=getIntent().getFloatExtra("deviation",0f);
        float[][] ranges={{0f,.25f},{.25f,.45f},{.45f,.55f},{.55f,.80f},{.80f,1f}};
        problemStartPct=ranges[stageIndex][0];problemEndPct=ranges[stageIndex][1];
        progressView.setProblemRange(problemStartPct,problemEndPct);

        title.setText("TOP "+rank+" · Cycle "+bCycle+" · "+stageShortName(stageIndex));
        labelA.setText("A 기준 · 정상 Cycle "+aCycle);
        labelB.setText("B 비교 · 문제 Cycle "+bCycle);
        info.setText(String.format(Locale.getDefault(),
                "문제 동작: %s · 편차 %.1f%%  |  A %.3f~%.3fs · B %.3f~%.3fs",
                stage,dev,aStartMs/1000f,aEndMs/1000f,bStartMs/1000f,bEndMs/1000f));
        problemAlert.setText("문제구간 · "+stageShortName(stageIndex)+String.format(Locale.getDefault()," %.0f~%.0f%%",problemStartPct*100f,problemEndPct*100f));

        TextureView.SurfaceTextureListener listenerA=new TextureView.SurfaceTextureListener(){
            public void onSurfaceTextureAvailable(SurfaceTexture st,int w,int h){prepareA(st);}
            public void onSurfaceTextureSizeChanged(SurfaceTexture st,int w,int h){}
            public boolean onSurfaceTextureDestroyed(SurfaceTexture st){return true;}
            public void onSurfaceTextureUpdated(SurfaceTexture st){}
        };
        TextureView.SurfaceTextureListener listenerB=new TextureView.SurfaceTextureListener(){
            public void onSurfaceTextureAvailable(SurfaceTexture st,int w,int h){prepareB(st);}
            public void onSurfaceTextureSizeChanged(SurfaceTexture st,int w,int h){}
            public boolean onSurfaceTextureDestroyed(SurfaceTexture st){return true;}
            public void onSurfaceTextureUpdated(SurfaceTexture st){}
        };
        textureA.setSurfaceTextureListener(listenerA);
        textureB.setSurfaceTextureListener(listenerB);
        if(textureA.isAvailable())prepareA(textureA.getSurfaceTexture());
        if(textureB.isAvailable())prepareB(textureB.getSurfaceTexture());

        findViewById(R.id.btnReplayRestart).setOnClickListener(v->{problemLoop=false;problemLoopButton.setText("⚠ 문제구간만 반복");paused=false;pauseButton.setText("⏸ 일시정지");restartBoth();});
        pauseButton.setOnClickListener(v->{
            paused=!paused;
            if(paused){pausePlayers();pauseButton.setText("▶ 계속재생");}
            else{applyPlaybackMode();startPlayers();pauseButton.setText("⏸ 일시정지");}
        });
        syncButton.setOnClickListener(v->{
            syncMode=!syncMode; paused=false;
            syncButton.setText(syncMode?"✓ 동기화 ON":"⇄ 원속도 비교");
            modeNote.setText(syncMode
                    ? "노란 구간 = 문제 동작구간 · 동기화 ON = A/B의 같은 0~100% 위치를 맞춰 비교"
                    : "원속도 비교 = 실제 Cycle Time 차이를 그대로 확인 · 필요하면 동기화를 다시 켜세요.");
            restartBoth();
        });
        problemLoopButton.setOnClickListener(v->{
            problemLoop=!problemLoop; paused=false;
            problemLoopButton.setText(problemLoop?"✓ 문제구간 반복 ON":"⚠ 문제구간만 반복");
            restartBoth();
        });
        findViewById(R.id.btnReplayClose).setOnClickListener(v->finish());
        handler.post(loopCheck);
    }

    private String stageShortName(int i){String[] n={"대기/초기","전진가속","커팅/충격","복귀가속","안정화"};return n[Math.max(0,Math.min(n.length-1,i))];}
    private float normalizedProgress(int currentMs,int startMs,int endMs){int d=Math.max(1,endMs-startMs);float p=(currentMs-startMs)/(float)d;return Math.max(0f,Math.min(1f,p));}
    private int mapProgress(float p,int start,int end){return start+Math.round((end-start)*Math.max(0f,Math.min(1f,p)));}
    private int loopStartA(){return problemLoop?mapProgress(problemStartPct,aStartMs,aEndMs):aStartMs;}
    private int loopEndA(){return problemLoop?mapProgress(problemEndPct,aStartMs,aEndMs):aEndMs;}
    private int loopStartB(){return problemLoop?mapProgress(problemStartPct,bStartMs,bEndMs):bStartMs;}
    private int loopEndB(){return problemLoop?mapProgress(problemEndPct,bStartMs,bEndMs):bEndMs;}

    private void prepareA(SurfaceTexture st){
        if(uriA==null||playerA!=null||st==null)return;
        try{
            surfaceA=new Surface(st);playerA=new MediaPlayer();playerA.setDataSource(this,uriA);playerA.setSurface(surfaceA);playerA.setVolume(0f,0f);playerA.setScreenOnWhilePlaying(true);
            playerA.setOnPreparedListener(mp->{preparedA=true;seekPlayer(mp,loopStartA());startIfReady();});
            playerA.prepareAsync();
        }catch(Exception e){problemAlert.setText("A 영상 준비 실패: "+e.getMessage());}
    }
    private void prepareB(SurfaceTexture st){
        if(uriB==null||playerB!=null||st==null)return;
        try{
            surfaceB=new Surface(st);playerB=new MediaPlayer();playerB.setDataSource(this,uriB);playerB.setSurface(surfaceB);playerB.setVolume(0f,0f);playerB.setScreenOnWhilePlaying(true);
            playerB.setOnPreparedListener(mp->{preparedB=true;seekPlayer(mp,loopStartB());startIfReady();});
            playerB.prepareAsync();
        }catch(Exception e){problemAlert.setText("B 영상 준비 실패: "+e.getMessage());}
    }
    private void startIfReady(){if(preparedA&&preparedB)restartBoth();}
    private void pausePlayers(){try{if(playerA!=null&&playerA.isPlaying())playerA.pause();}catch(Exception ignored){}try{if(playerB!=null&&playerB.isPlaying())playerB.pause();}catch(Exception ignored){}}
    private void startPlayers(){try{if(playerA!=null)playerA.start();}catch(Exception ignored){}try{if(playerB!=null)playerB.start();}catch(Exception ignored){}}
    private void seekPlayer(MediaPlayer p,int ms){try{if(android.os.Build.VERSION.SDK_INT>=26)p.seekTo(ms,MediaPlayer.SEEK_CLOSEST);else p.seekTo(ms);}catch(Exception ignored){}}

    private void applyPlaybackMode(){
        if(!preparedA||!preparedB)return;
        try{
            float speedA=1f,speedB=1f;
            if(syncMode){
                float durA=Math.max(1f,loopEndA()-loopStartA());
                float durB=Math.max(1f,loopEndB()-loopStartB());
                float common=Math.max(durA,durB);
                speedA=Math.max(.20f,Math.min(1f,durA/common));
                speedB=Math.max(.20f,Math.min(1f,durB/common));
            }
            playerA.setPlaybackParams(new PlaybackParams().setSpeed(speedA).setPitch(1f));
            playerB.setPlaybackParams(new PlaybackParams().setSpeed(speedB).setPitch(1f));
        }catch(Exception ignored){}
    }

    private void restartBoth(){
        if(!preparedA||!preparedB||restarting)return;
        restarting=true;
        pausePlayers();
        seekPlayer(playerA,loopStartA());seekPlayer(playerB,loopStartB());
        applyPlaybackMode();
        handler.postDelayed(()->{
            if(!paused){startPlayers();}
            restarting=false;
            lastSyncCorrectionMs=SystemClock.elapsedRealtime();
        },140);
    }

    @Override protected void onPause(){super.onPause();pausePlayers();paused=true;pauseButton.setText("▶ 계속재생");}
    @Override protected void onDestroy(){
        handler.removeCallbacksAndMessages(null);
        try{if(playerA!=null){playerA.stop();playerA.release();}}catch(Exception ignored){}
        try{if(playerB!=null){playerB.stop();playerB.release();}}catch(Exception ignored){}
        try{if(surfaceA!=null)surfaceA.release();}catch(Exception ignored){}
        try{if(surfaceB!=null)surfaceB.release();}catch(Exception ignored){}
        super.onDestroy();
    }
}

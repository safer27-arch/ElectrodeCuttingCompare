package com.example.electrodecutcompare;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;

import java.util.Locale;

public class CycleReplayActivity extends Activity {
    private VideoView videoA, videoB;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private int aStartMs,aEndMs,bStartMs,bEndMs;
    private boolean preparedA=false,preparedB=false,paused=false;
    private final Runnable loopCheck=new Runnable(){
        @Override public void run(){
            try{
                if(preparedA && !paused && videoA.getCurrentPosition()>=aEndMs-25){videoA.seekTo(aStartMs);videoA.start();}
                if(preparedB && !paused && videoB.getCurrentPosition()>=bEndMs-25){videoB.seekTo(bStartMs);videoB.start();}
            }catch(Exception ignored){}
            handler.postDelayed(this,45);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);setContentView(R.layout.activity_cycle_replay);
        videoA=findViewById(R.id.videoReplayA); videoB=findViewById(R.id.videoReplayB);
        TextView title=findViewById(R.id.txtReplayTitle),info=findViewById(R.id.txtReplayInfo),labelA=findViewById(R.id.txtReplayA),labelB=findViewById(R.id.txtReplayB);
        String ua=getIntent().getStringExtra("uriA"), ub=getIntent().getStringExtra("uriB");
        aStartMs=Math.max(0,Math.round(getIntent().getFloatExtra("aStart",0f)*1000f));
        aEndMs=Math.max(aStartMs+80,Math.round(getIntent().getFloatExtra("aEnd",0f)*1000f));
        bStartMs=Math.max(0,Math.round(getIntent().getFloatExtra("bStart",0f)*1000f));
        bEndMs=Math.max(bStartMs+80,Math.round(getIntent().getFloatExtra("bEnd",0f)*1000f));
        int aCycle=getIntent().getIntExtra("aCycle",1), bCycle=getIntent().getIntExtra("bCycle",1),rank=getIntent().getIntExtra("rank",1);
        String stage=getIntent().getStringExtra("stage"); if(stage==null)stage="문제 동작구간";
        float dev=getIntent().getFloatExtra("deviation",0f);
        title.setText("TOP "+rank+" · Cycle "+bCycle+" · "+stage);
        labelA.setText("A 기준 · 대표 정상 Cycle "+aCycle);
        labelB.setText("B 비교 · 문제 Cycle "+bCycle);
        info.setText(String.format(Locale.getDefault(),"B Cycle %d의 핵심 문제 동작: %s · 평균궤적 편차 %.1f%%\nA %.3f~%.3fs / B %.3f~%.3fs",bCycle,stage,dev,aStartMs/1000f,aEndMs/1000f,bStartMs/1000f,bEndMs/1000f));
        if(ua!=null)videoA.setVideoURI(Uri.parse(ua)); if(ub!=null)videoB.setVideoURI(Uri.parse(ub));
        videoA.setOnPreparedListener(mp->{preparedA=true;mute(mp);videoA.seekTo(aStartMs);startIfReady();});
        videoB.setOnPreparedListener(mp->{preparedB=true;mute(mp);videoB.seekTo(bStartMs);startIfReady();});
        Button restart=findViewById(R.id.btnReplayRestart),pause=findViewById(R.id.btnReplayPause);
        restart.setOnClickListener(v->{paused=false;pause.setText("⏸ 일시정지");restartBoth();});
        pause.setOnClickListener(v->{paused=!paused;if(paused){videoA.pause();videoB.pause();pause.setText("▶ 계속재생");}else{videoA.start();videoB.start();pause.setText("⏸ 일시정지");}});
        findViewById(R.id.btnReplayClose).setOnClickListener(v->finish());
        handler.post(loopCheck);
    }
    private void mute(MediaPlayer mp){try{mp.setVolume(0f,0f);}catch(Exception ignored){}}
    private void startIfReady(){if(preparedA&&preparedB)restartBoth();}
    private void restartBoth(){try{videoA.seekTo(aStartMs);videoB.seekTo(bStartMs);videoA.start();videoB.start();}catch(Exception ignored){}}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
}

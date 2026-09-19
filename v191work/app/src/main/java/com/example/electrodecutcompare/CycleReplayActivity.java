package com.example.electrodecutcompare;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import java.util.Locale;

public class CycleReplayActivity extends Activity {
    private VideoView videoA, videoB;
    private ProgressBar progressA, progressB;
    private TextView progressText, modeNote;
    private Button pauseButton, syncButton;
    private MediaPlayer playerA, playerB;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int aStartMs, aEndMs, bStartMs, bEndMs;
    private boolean preparedA = false, preparedB = false, paused = false, syncMode = false;

    private final Runnable loopCheck = new Runnable() {
        @Override public void run() {
            try {
                if (preparedA && preparedB) {
                    float pa = normalizedProgress(videoA.getCurrentPosition(), aStartMs, aEndMs);
                    float pb = normalizedProgress(videoB.getCurrentPosition(), bStartMs, bEndMs);
                    progressA.setProgress(Math.round(pa * 1000f));
                    progressB.setProgress(Math.round(pb * 1000f));
                    float delta = Math.abs(pa - pb) * 100f;
                    progressText.setText(String.format(Locale.getDefault(),
                            "Cycle 진행률 · A %.0f%% | B %.0f%% | Δ %.0f%%",
                            pa * 100f, pb * 100f, delta));

                    if (!paused) {
                        if (syncMode) {
                            if (pa >= 0.995f || pb >= 0.995f) restartBoth();
                        } else {
                            if (videoA.getCurrentPosition() >= aEndMs - 25) {
                                videoA.seekTo(aStartMs);
                                videoA.start();
                            }
                            if (videoB.getCurrentPosition() >= bEndMs - 25) {
                                videoB.seekTo(bStartMs);
                                videoB.start();
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            handler.postDelayed(this, 45);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cycle_replay);

        videoA = findViewById(R.id.videoReplayA);
        videoB = findViewById(R.id.videoReplayB);
        progressA = findViewById(R.id.progressReplayA);
        progressB = findViewById(R.id.progressReplayB);
        progressText = findViewById(R.id.txtReplayProgress);
        modeNote = findViewById(R.id.txtReplayModeNote);
        pauseButton = findViewById(R.id.btnReplayPause);
        syncButton = findViewById(R.id.btnReplaySync);

        TextView title = findViewById(R.id.txtReplayTitle);
        TextView info = findViewById(R.id.txtReplayInfo);
        TextView labelA = findViewById(R.id.txtReplayA);
        TextView labelB = findViewById(R.id.txtReplayB);

        String ua = getIntent().getStringExtra("uriA");
        String ub = getIntent().getStringExtra("uriB");
        aStartMs = Math.max(0, Math.round(getIntent().getFloatExtra("aStart", 0f) * 1000f));
        aEndMs = Math.max(aStartMs + 80, Math.round(getIntent().getFloatExtra("aEnd", 0f) * 1000f));
        bStartMs = Math.max(0, Math.round(getIntent().getFloatExtra("bStart", 0f) * 1000f));
        bEndMs = Math.max(bStartMs + 80, Math.round(getIntent().getFloatExtra("bEnd", 0f) * 1000f));

        int aCycle = getIntent().getIntExtra("aCycle", 1);
        int bCycle = getIntent().getIntExtra("bCycle", 1);
        int rank = getIntent().getIntExtra("rank", 1);
        String stage = getIntent().getStringExtra("stage");
        if (stage == null) stage = "문제 동작구간";
        float dev = getIntent().getFloatExtra("deviation", 0f);

        title.setText("TOP " + rank + " · Cycle " + bCycle + " · " + stage);
        labelA.setText("A 기준 · 대표 정상 Cycle " + aCycle);
        labelB.setText("B 비교 · 문제 Cycle " + bCycle);
        info.setText(String.format(Locale.getDefault(),
                "B Cycle %d의 핵심 문제 동작: %s · 평균궤적 편차 %.1f%%\nA %.3f~%.3fs / B %.3f~%.3fs\n두 진행률 막대의 차이가 커지는 순간을 직접 비교하세요.",
                bCycle, stage, dev, aStartMs / 1000f, aEndMs / 1000f, bStartMs / 1000f, bEndMs / 1000f));

        if (ua != null) videoA.setVideoURI(Uri.parse(ua));
        if (ub != null) videoB.setVideoURI(Uri.parse(ub));

        videoA.setOnPreparedListener(mp -> {
            playerA = mp;
            preparedA = true;
            mute(mp);
            videoA.seekTo(aStartMs);
            startIfReady();
        });
        videoB.setOnPreparedListener(mp -> {
            playerB = mp;
            preparedB = true;
            mute(mp);
            videoB.seekTo(bStartMs);
            startIfReady();
        });

        findViewById(R.id.btnReplayRestart).setOnClickListener(v -> {
            paused = false;
            pauseButton.setText("⏸ 일시정지");
            restartBoth();
        });

        pauseButton.setOnClickListener(v -> {
            paused = !paused;
            if (paused) {
                videoA.pause();
                videoB.pause();
                pauseButton.setText("▶ 계속재생");
            } else {
                applyPlaybackMode();
                videoA.start();
                videoB.start();
                pauseButton.setText("⏸ 일시정지");
            }
        });

        syncButton.setOnClickListener(v -> {
            syncMode = !syncMode;
            paused = false;
            pauseButton.setText("⏸ 일시정지");
            syncButton.setText(syncMode ? "✓ 진행률 동기화 ON" : "⇄ 진행률 동기화");
            modeNote.setText(syncMode
                    ? "※ 진행률 동기화 ON: 짧은 Cycle의 재생속도를 낮춰 A/B가 같은 0~100% 동작 위치를 함께 지나가도록 맞춥니다. 실제 Cycle Time 차이는 상단 정보에서 그대로 확인하세요."
                    : "※ 원속도 모드: 실제 Cycle Time 차이를 그대로 봅니다. 진행률 동기화 모드: 짧은 Cycle을 느리게 재생해 A/B의 0~100% 동작 위치를 맞춰 비교합니다.");
            restartBoth();
        });

        findViewById(R.id.btnReplayClose).setOnClickListener(v -> finish());
        handler.post(loopCheck);
    }

    private float normalizedProgress(int currentMs, int startMs, int endMs) {
        int duration = Math.max(1, endMs - startMs);
        float p = (currentMs - startMs) / (float) duration;
        if (p < 0f) return 0f;
        if (p > 1f) return 1f;
        return p;
    }

    private void mute(MediaPlayer mp) {
        try { mp.setVolume(0f, 0f); } catch (Exception ignored) {}
    }

    private void startIfReady() {
        if (preparedA && preparedB) restartBoth();
    }

    private void applyPlaybackMode() {
        if (!preparedA || !preparedB) return;
        try {
            float speedA = 1f;
            float speedB = 1f;
            if (syncMode) {
                float durA = Math.max(1f, aEndMs - aStartMs);
                float durB = Math.max(1f, bEndMs - bStartMs);
                float common = Math.max(durA, durB);
                speedA = Math.max(0.20f, Math.min(1f, durA / common));
                speedB = Math.max(0.20f, Math.min(1f, durB / common));
            }
            PlaybackParams pa = playerA.getPlaybackParams();
            pa.setSpeed(speedA);
            playerA.setPlaybackParams(pa);
            PlaybackParams pb = playerB.getPlaybackParams();
            pb.setSpeed(speedB);
            playerB.setPlaybackParams(pb);
        } catch (Exception ignored) {}
    }

    private void restartBoth() {
        try {
            applyPlaybackMode();
            videoA.seekTo(aStartMs);
            videoB.seekTo(bStartMs);
            videoA.start();
            videoB.start();
        } catch (Exception ignored) {}
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}

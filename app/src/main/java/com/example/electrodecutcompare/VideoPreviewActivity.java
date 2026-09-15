package com.example.electrodecutcompare;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

public class VideoPreviewActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(true);
        setContentView(R.layout.activity_video_preview);
        String uriText = getIntent().getStringExtra("uri");
        if (uriText == null) { finish(); return; }
        VideoView video = findViewById(R.id.videoView);
        MediaController controller = new MediaController(this);
        controller.setAnchorView(video);
        video.setMediaController(controller);
        video.setVideoURI(Uri.parse(uriText));
        video.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            video.start();
        });
    }
}

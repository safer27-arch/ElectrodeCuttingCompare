package com.example.electrodecutcompare;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

public class GuideActivity extends Activity {
    @Override protected void onCreate(Bundle b){
        super.onCreate(b); LanguageManager.init(this); setContentView(R.layout.activity_guide);
        if(android.os.Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(true);
        TextView body=findViewById(R.id.guideBody);
        body.setText(Html.fromHtml(content(), Html.FROM_HTML_MODE_LEGACY));
        findViewById(R.id.btnGuideClose).setOnClickListener(v->finish());
    }
    private String content(){ return LanguageManager.guideHtml(); }
}

package com.example.electrodecutcompare;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import java.util.Locale;

public class RoiSettingsActivity extends Activity {
    private static final float[] DG={.43f,.22f,.78f,.64f}, DT={.48f,.43f,.89f,.82f}, DN={.60f,.30f,.95f,.84f};
    private ImageView image;
    private TextView info;
    private Spinner machineSpinner, roiSpinner;
    private Bitmap base;
    private RoiSettingsStore.Roi roi;
    private String machine="A", roiName="GRIPPER";
    private String uriAString, uriBString;
    private float step=.015f;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        if (android.os.Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(true); setContentView(R.layout.activity_roi_settings);
        image=findViewById(R.id.roiImage); info=findViewById(R.id.roiInfo);
        machineSpinner=findViewById(R.id.machineSpinner); roiSpinner=findViewById(R.id.roiSpinner);

        machineSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"설비 A","설비 B"}));
        roiSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Gripper","전극 선단 Tip","Nip"}));

        AdapterView.OnItemSelectedListener listener=new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> p,android.view.View v,int pos,long id){ reloadSelection(); }
            public void onNothingSelected(AdapterView<?> p){}
        };
        machineSpinner.setOnItemSelectedListener(listener); roiSpinner.setOnItemSelectedListener(listener);

        findViewById(R.id.left).setOnClickListener(v->move(-step,0));
        findViewById(R.id.right).setOnClickListener(v->move(step,0));
        findViewById(R.id.up).setOnClickListener(v->move(0,-step));
        findViewById(R.id.down).setOnClickListener(v->move(0,step));
        findViewById(R.id.wminus).setOnClickListener(v->resize(-step,0));
        findViewById(R.id.wplus).setOnClickListener(v->resize(step,0));
        findViewById(R.id.hminus).setOnClickListener(v->resize(0,-step));
        findViewById(R.id.hplus).setOnClickListener(v->resize(0,step));
        findViewById(R.id.saveRoi).setOnClickListener(v->{ save(); Toast.makeText(this,"ROI 저장 완료",Toast.LENGTH_SHORT).show();});
        findViewById(R.id.resetRoi).setOnClickListener(v->{RoiSettingsStore.reset(this,machine);reloadSelection();Toast.makeText(this,"설비 "+machine+" ROI 초기화",Toast.LENGTH_SHORT).show();});

        uriAString=getIntent().getStringExtra("uriA"); uriBString=getIntent().getStringExtra("uriB");
        loadFrameForMachine();
    }

    private float[] def(){
        if("GRIPPER".equals(roiName)) return DG;
        if("TIP".equals(roiName)) return DT;
        return DN;
    }
    private void reloadSelection(){
        machine=machineSpinner.getSelectedItemPosition()==0?"A":"B";
        int r=roiSpinner.getSelectedItemPosition();
        roiName=r==0?"GRIPPER":r==1?"TIP":"NIP";
        roi=RoiSettingsStore.load(this,machine,roiName,def());
        loadFrameForMachine();
        draw();
    }
    private void loadFrameForMachine(){
        String u="A".equals(machine)?uriAString:uriBString;
        if(u==null||u.isEmpty())u="A".equals(machine)?uriBString:uriAString;
        loadFrame(u);
    }
    private void loadFrame(String s){
        if(s==null){ info.setText("먼저 메인 화면에서 영상을 선택한 뒤 ROI 설정을 여세요."); return; }
        MediaMetadataRetriever r=new MediaMetadataRetriever();
        try{
            r.setDataSource(this, Uri.parse(s));
            long d=Long.parseLong(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            Bitmap x=r.getFrameAtTime(d*500L,MediaMetadataRetriever.OPTION_CLOSEST);
            if(x!=null) base=BitmapAnalysis.fitMaxWidth(x,960);
        }catch(Exception e){info.setText("프레임 읽기 실패: "+e.getMessage());}
        finally{try{r.release();}catch(Exception ignored){}}
        draw();
    }
    private void move(float dx,float dy){
        if(roi==null)return; float w=roi.r-roi.l,h=roi.b-roi.t;
        roi.l=clamp(roi.l+dx,0,1-w); roi.r=roi.l+w;
        roi.t=clamp(roi.t+dy,0,1-h); roi.b=roi.t+h; draw();
    }
    private void resize(float dw,float dh){
        if(roi==null)return;
        float cx=(roi.l+roi.r)/2, cy=(roi.t+roi.b)/2;
        float w=clamp(roi.r-roi.l+dw,.08f,.90f), h=clamp(roi.b-roi.t+dh,.08f,.90f);
        roi.l=clamp(cx-w/2,0,1-w); roi.r=roi.l+w;
        roi.t=clamp(cy-h/2,0,1-h); roi.b=roi.t+h; draw();
    }
    private float clamp(float x,float a,float b){return Math.max(a,Math.min(b,x));}
    private void save(){ if(roi!=null) RoiSettingsStore.save(this,machine,roiName,roi); }
    private void draw(){
        if(roi==null)return;
        info.setText(String.format(Locale.getDefault(),"설비 %s · %s  [L %.3f T %.3f R %.3f B %.3f]",machine,roiName,roi.l,roi.t,roi.r,roi.b));
        if(base==null)return;
        Bitmap out=base.copy(Bitmap.Config.ARGB_8888,true); Canvas c=new Canvas(out);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(6);
        p.setColor("GRIPPER".equals(roiName)?Color.rgb(0,190,80):"TIP".equals(roiName)?Color.rgb(255,160,0):Color.rgb(220,40,40));
        c.drawRect(roi.l*out.getWidth(),roi.t*out.getHeight(),roi.r*out.getWidth(),roi.b*out.getHeight(),p);
        image.setImageBitmap(out);
    }
}

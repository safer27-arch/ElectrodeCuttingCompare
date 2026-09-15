package com.example.electrodecutcompare;
import android.app.*;import android.os.*;import android.graphics.*;import android.graphics.drawable.*;import android.view.*;import android.widget.*;import java.io.*;
public class ZoomImageActivity extends Activity{
 private ImageView image; private Matrix matrix=new Matrix(),saved=new Matrix(); private float oldDist=1f,startX,startY; private int mode=0;
 @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_zoom_image);image=findViewById(R.id.zoomImage);String p=getIntent().getStringExtra("path");if(p!=null)image.setImageBitmap(BitmapFactory.decodeFile(p));image.setScaleType(ImageView.ScaleType.MATRIX);
  image.setOnTouchListener((v,e)->{switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:saved.set(matrix);startX=e.getX();startY=e.getY();mode=1;break;case MotionEvent.ACTION_POINTER_DOWN:oldDist=dist(e);if(oldDist>10){saved.set(matrix);mode=2;}break;case MotionEvent.ACTION_MOVE:if(mode==1){matrix.set(saved);matrix.postTranslate(e.getX()-startX,e.getY()-startY);}else if(mode==2&&e.getPointerCount()>1){float d=dist(e),s=d/oldDist;matrix.set(saved);matrix.postScale(s,s,e.getX(0),e.getY(0));}image.setImageMatrix(matrix);break;case MotionEvent.ACTION_UP:case MotionEvent.ACTION_POINTER_UP:mode=0;}return true;});
  image.setOnClickListener(v->{});findViewById(R.id.zoomReset).setOnClickListener(v->{matrix.reset();image.setImageMatrix(matrix);});findViewById(R.id.zoomClose).setOnClickListener(v->finish());
 }
 private float dist(MotionEvent e){float x=e.getX(0)-e.getX(1),y=e.getY(0)-e.getY(1);return (float)Math.hypot(x,y);}
}
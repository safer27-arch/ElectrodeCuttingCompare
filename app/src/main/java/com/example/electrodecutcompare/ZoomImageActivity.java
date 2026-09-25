package com.example.electrodecutcompare;
import android.app.*;import android.os.*;import android.graphics.*;import android.view.*;import android.widget.*;
public class ZoomImageActivity extends Activity{
 private ImageView image; private Matrix matrix=new Matrix(),saved=new Matrix(); private Bitmap bitmap;
 private float oldDist=1f,startX,startY; private int mode=0;
 @Override protected void onCreate(Bundle b){
  super.onCreate(b);
  LanguageManager.init(this);
  if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(true);
  setContentView(R.layout.activity_zoom_image);
  image=findViewById(R.id.zoomImage); image.setScaleType(ImageView.ScaleType.MATRIX);
  String p=getIntent().getStringExtra("path");
  if(p!=null){bitmap=BitmapFactory.decodeFile(p);image.setImageBitmap(bitmap);}
  image.post(this::fitCenter);
  image.setOnTouchListener((v,e)->{
   switch(e.getActionMasked()){
    case MotionEvent.ACTION_DOWN:saved.set(matrix);startX=e.getX();startY=e.getY();mode=1;break;
    case MotionEvent.ACTION_POINTER_DOWN:oldDist=dist(e);if(oldDist>10){saved.set(matrix);mode=2;}break;
    case MotionEvent.ACTION_MOVE:
     if(mode==1){matrix.set(saved);matrix.postTranslate(e.getX()-startX,e.getY()-startY);}
     else if(mode==2&&e.getPointerCount()>1){float d=dist(e);matrix.set(saved);matrix.postScale(d/oldDist,d/oldDist,e.getX(0),e.getY(0));}
     image.setImageMatrix(matrix);break;
    case MotionEvent.ACTION_UP:case MotionEvent.ACTION_POINTER_UP:mode=0;break;
   }return true;
  });
  findViewById(R.id.zoomReset).setOnClickListener(v->fitCenter());
  findViewById(R.id.zoomClose).setOnClickListener(v->finish());
 }
 private void fitCenter(){
  if(bitmap==null||image.getWidth()<=0||image.getHeight()<=0)return;
  float sc=Math.min(image.getWidth()/(float)bitmap.getWidth(),image.getHeight()/(float)bitmap.getHeight());
  float dx=(image.getWidth()-bitmap.getWidth()*sc)/2f,dy=(image.getHeight()-bitmap.getHeight()*sc)/2f;
  matrix.reset();matrix.postScale(sc,sc);matrix.postTranslate(dx,dy);image.setImageMatrix(matrix);
 }
 private float dist(MotionEvent e){if(e.getPointerCount()<2)return 1f;float x=e.getX(0)-e.getX(1),y=e.getY(0)-e.getY(1);return (float)Math.hypot(x,y);}
}
package com.example.electrodecutcompare;
import android.content.*;import android.database.*;import android.net.Uri;import android.os.*;import android.provider.OpenableColumns;import java.io.*;
/** Read-only grant-scoped provider, confined to app-private reports. No external/shared root access. */
public final class ReportShareProvider extends ContentProvider {
 @Override public boolean onCreate(){return true;}
 public static Uri uri(Context c,File f)throws IOException{File root=new File(c.getFilesDir(),"reports").getCanonicalFile();File file=f.getCanonicalFile();String prefix=root.getPath()+File.separator;if(!file.getPath().startsWith(prefix)||!file.isFile())throw new IOException("Invalid report file");return new Uri.Builder().scheme("content").authority(c.getPackageName()+".reports").appendPath(file.getPath().substring(prefix.length())).build();}
 private File resolve(Uri u)throws FileNotFoundException{try{if(!"content".equals(u.getScheme())||!(getContext().getPackageName()+".reports").equals(u.getAuthority())||u.getPathSegments().size()!=1)throw new IOException();File root=new File(getContext().getFilesDir(),"reports").getCanonicalFile();File f=new File(root,u.getPathSegments().get(0)).getCanonicalFile();if(!f.getPath().startsWith(root.getPath()+File.separator)||!f.isFile())throw new IOException();return f;}catch(IOException e){throw new FileNotFoundException("Not a report file");}}
 @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException("Read only");return ParcelFileDescriptor.open(resolve(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
 @Override public String getType(Uri uri){String p=uri.getLastPathSegment();return p!=null&&p.endsWith(".jpg")?"image/jpeg":p!=null&&p.endsWith(".xlsx")?"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":"application/zip";}
 @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){try{File f=resolve(uri);String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor m=new MatrixCursor(cols);Object[] row=new Object[cols.length];for(int i=0;i<cols.length;i++){if(OpenableColumns.DISPLAY_NAME.equals(cols[i]))row[i]=f.getName();else if(OpenableColumns.SIZE.equals(cols[i]))row[i]=f.length();}m.addRow(row);return m;}catch(FileNotFoundException e){return null;}}
 @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException("Read only");}
 @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException("Read only");}
 @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException("Read only");}
}

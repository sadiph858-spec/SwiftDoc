package com.smartpdf.reader.utils;
import android.content.Context; import android.database.Cursor; import android.net.Uri;
import android.os.Build; import android.os.Environment; import android.provider.MediaStore;
import android.provider.OpenableColumns; import android.util.Log;
import com.smartpdf.reader.models.PDFFile;
import java.io.File; import java.io.FileOutputStream; import java.io.InputStream;
import java.text.SimpleDateFormat; import java.util.ArrayList; import java.util.Date; import java.util.List; import java.util.Locale;
public class FileUtils {
    private static final String TAG="FileUtils";
    private static final SimpleDateFormat FMT=new SimpleDateFormat("dd MMM yyyy",Locale.getDefault());
    public static List<PDFFile> scanPDFs(Context ctx){
        List<PDFFile> list=new ArrayList<>();
        Uri col=Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q?MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL):MediaStore.Files.getContentUri("external");
        String[]proj={MediaStore.Files.FileColumns.DISPLAY_NAME,MediaStore.Files.FileColumns.DATA,MediaStore.Files.FileColumns.DATE_MODIFIED,MediaStore.Files.FileColumns.SIZE};
        try(Cursor c=ctx.getContentResolver().query(col,proj,MediaStore.Files.FileColumns.MIME_TYPE+"=?",new String[]{"application/pdf"},MediaStore.Files.FileColumns.DATE_MODIFIED+" DESC")){
            if(c!=null&&c.moveToFirst()){int ni=c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME),pi=c.getColumnIndex(MediaStore.Files.FileColumns.DATA),di=c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED),si=c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
                do{try{String n=c.getString(ni),p=pi>=0?c.getString(pi):null;if(p==null||p.isEmpty()||!new File(p).exists())continue;list.add(new PDFFile(n,p,FMT.format(new Date(c.getLong(di)*1000L)),c.getLong(si)));}catch(Exception e){Log.e(TAG,""+e);}}while(c.moveToNext());}}
        catch(Exception e){Log.e(TAG,""+e);}return list;}
    public static String getPathFromUri(Context ctx,Uri uri){
        if(uri==null)return null;
        if("file".equals(uri.getScheme()))return uri.getPath();
        if("content".equals(uri.getScheme())){
            try(Cursor c=ctx.getContentResolver().query(uri,new String[]{MediaStore.Files.FileColumns.DATA},null,null,null)){
                if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(MediaStore.Files.FileColumns.DATA);if(i>=0){String p=c.getString(i);if(p!=null&&new File(p).exists())return p;}}}
            catch(Exception e){Log.e(TAG,""+e);}
            return copyToCache(ctx,uri);}
        return null;}
    public static String copyToCache(Context ctx,Uri uri){
        try{String n=getFileName(ctx,uri);if(n==null)n="tmp_"+System.currentTimeMillis()+".pdf";
            File d=new File(ctx.getCacheDir(),"pdfs");if(!d.exists())d.mkdirs();File dest=new File(d,n);
            try(InputStream is=ctx.getContentResolver().openInputStream(uri);FileOutputStream fo=new FileOutputStream(dest)){
                if(is==null)return null;byte[]b=new byte[8192];int len;while((len=is.read(b))!=-1)fo.write(b,0,len);}
            return dest.getAbsolutePath();}catch(Exception e){Log.e(TAG,""+e);return null;}}
    public static String getFileName(Context ctx,Uri uri){
        try(Cursor c=ctx.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
            if(c!=null&&c.moveToFirst())return c.getString(c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));}
        catch(Exception e){Log.e(TAG,""+e);}return uri.getLastPathSegment();}
    public static long getFileSize(Context ctx,Uri uri){
        try(Cursor c=ctx.getContentResolver().query(uri,new String[]{OpenableColumns.SIZE},null,null,null)){
            if(c!=null&&c.moveToFirst())return c.getLong(c.getColumnIndexOrThrow(OpenableColumns.SIZE));}
        catch(Exception e){Log.e(TAG,""+e);}return 0;}
    public static boolean isValidPDF(String p){if(p==null||p.isEmpty())return false;File f=new File(p);return f.exists()&&f.isFile()&&f.canRead()&&f.getName().toLowerCase().endsWith(".pdf")&&f.length()>0;}
    public static File getDownloadsDir(){return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);}
}

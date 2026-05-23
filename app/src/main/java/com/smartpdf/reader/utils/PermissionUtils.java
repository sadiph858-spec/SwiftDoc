package com.smartpdf.reader.utils;
import android.Manifest; import android.app.Activity; import android.content.Context;
import android.content.pm.PackageManager; import android.os.Build;
import androidx.core.app.ActivityCompat; import androidx.core.content.ContextCompat;
public class PermissionUtils {
    public static final int REQ=1001;
    public static boolean hasStoragePermission(Context c){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU)
            return ContextCompat.checkSelfPermission(c,Manifest.permission.READ_MEDIA_IMAGES)==PackageManager.PERMISSION_GRANTED;
        return ContextCompat.checkSelfPermission(c,Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED;}
    public static void requestStoragePermission(Activity a){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU)
            ActivityCompat.requestPermissions(a,new String[]{Manifest.permission.READ_MEDIA_IMAGES},REQ);
        else ActivityCompat.requestPermissions(a,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},REQ);}
    public static boolean shouldShowRationale(Activity a){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU)
            return ActivityCompat.shouldShowRequestPermissionRationale(a,Manifest.permission.READ_MEDIA_IMAGES);
        return ActivityCompat.shouldShowRequestPermissionRationale(a,Manifest.permission.READ_EXTERNAL_STORAGE);}
}

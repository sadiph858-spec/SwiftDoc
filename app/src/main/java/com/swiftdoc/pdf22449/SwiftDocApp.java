package com.swiftdoc.pdf22449;
import android.app.Application;
import com.google.android.gms.ads.MobileAds;
public class SwiftDocApp extends Application {
    public static String AD_UNIT_BANNER = "";
    public static String AD_UNIT_INTERSTITIAL = "";
    public static boolean ADS_ENABLED = false;
    @Override public void onCreate() {
        super.onCreate();
        if (ADS_ENABLED) MobileAds.initialize(this, s -> {});
    }
}

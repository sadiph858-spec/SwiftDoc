package com.smartpdf.reader;

import android.app.Application;
import com.google.android.gms.ads.MobileAds;

public class LeafPDFApp extends Application {
    // Ad Unit IDs - Replace with real IDs from AdMob
    // Using test IDs by default (safe for development)
    public static String AD_UNIT_BANNER   = "";  // e.g. "ca-app-pub-xxx/yyy"
    public static String AD_UNIT_INTERSTITIAL = "";
    public static boolean ADS_ENABLED = false;   // Set true when real ad IDs added

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize AdMob - only shows ads if ADS_ENABLED = true
        if (ADS_ENABLED) {
            MobileAds.initialize(this, initializationStatus -> {});
        }
    }
}

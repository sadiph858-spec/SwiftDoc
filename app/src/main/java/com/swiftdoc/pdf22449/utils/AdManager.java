package com.swiftdoc.pdf22449.utils;
import android.app.Activity; import android.content.Context; import android.view.View;
import android.widget.FrameLayout;
import com.google.android.gms.ads.*;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.swiftdoc.pdf22449.SwiftDocApp;

/**
 * AdManager - Centralized ad handling.
 * Ads are HIDDEN when no ad ID is configured.
 * To enable ads: set SwiftDocApp.ADS_ENABLED=true and fill AD_UNIT_BANNER/INTERSTITIAL
 */
public class AdManager {
    private static InterstitialAd interstitialAd;

    /** Load banner into container. Container is GONE when ads disabled. */
    public static void loadBanner(Context ctx, FrameLayout container) {
        if (!SwiftDocApp.ADS_ENABLED || SwiftDocApp.AD_UNIT_BANNER.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }
        container.setVisibility(View.VISIBLE);
        AdView adView = new AdView(ctx);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(SwiftDocApp.AD_UNIT_BANNER);
        container.removeAllViews();
        container.addView(adView);
        adView.loadAd(new AdRequest.Builder().build());
    }

    /** Load interstitial ad preemptively */
    public static void loadInterstitial(Activity activity) {
        if (!SwiftDocApp.ADS_ENABLED || SwiftDocApp.AD_UNIT_INTERSTITIAL.isEmpty()) return;
        InterstitialAd.load(activity, SwiftDocApp.AD_UNIT_INTERSTITIAL,
            new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                @Override public void onAdLoaded(InterstitialAd ad) { interstitialAd = ad; }
                @Override public void onAdFailedToLoad(LoadAdError e) { interstitialAd = null; }
            });
    }

    /** Show interstitial if ready */
    public static void showInterstitial(Activity activity) {
        if (interstitialAd != null) { interstitialAd.show(activity); interstitialAd = null; }
    }
}

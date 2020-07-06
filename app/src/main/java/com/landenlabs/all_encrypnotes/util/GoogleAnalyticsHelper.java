package com.landenlabs.all_encrypnotes.util;

import android.app.Application;
import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * Google Analytics helper class.
 *
 * @author Dennis Lang
 */
@SuppressWarnings("FieldCanBeLocal")
public class GoogleAnalyticsHelper {
    private final Context mContext;
    private GoogleAnalytics mGoogleAnalytics;
    private Tracker mTracker;

    public GoogleAnalyticsHelper(Application context, boolean isDebug) {
        mContext = context;
        int keyResId = context.getResources().getIdentifier("google_analytic_key", "string", context.getPackageName());
        if (keyResId > 0) {
            String googleAnalyticKey = context.getResources().getString(keyResId);

            mGoogleAnalytics = GoogleAnalytics.getInstance(context);
            mGoogleAnalytics.enableAutoActivityReports(context);

            // Use following when debugging Google Analtyics ---
            //   mGoogleAnalytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE.ordinal());
            //   mGoogleAnalytics.setLocalDispatchPeriod(10);

            mTracker = mGoogleAnalytics.newTracker(googleAnalyticKey);
            mTracker.enableAutoActivityTracking(true);
            // mTracker.setSampleRate(mSampling);

            mTracker.setScreenName("Home");

            // Enable if you want google to manage crash reports.
            // Disable similar call by Flurry.
            // mTracker.enableExceptionReporting(true);
        }
    }

    /**
     * Send Screen tracker and update last sent time.
     */
    public void sendScreen(String screenName) {
        mTracker.setScreenName(screenName);

        try {
            HitBuilders.ScreenViewBuilder hitBuilders = new HitBuilders.ScreenViewBuilder();
            mTracker.send(hitBuilders.build());
        } catch (Exception ignored) {
        }
    }

    /**
     * Send Event if enabled and screen event has been sent.
     */
    public void sendEvent(String cat, String action, String label) {
        try {
            // Google Analytics v4
            HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder()
                    .setCategory(cat)
                    .setAction(action)
                    .setLabel(label);

            mTracker.send(eventBuilder.build());

        } catch (Exception ignored) {
        }
    }
}
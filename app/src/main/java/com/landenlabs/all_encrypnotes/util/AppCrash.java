package com.landenlabs.all_encrypnotes.util;

import android.app.Application;
import android.content.Context;

import net.hockeyapp.android.Constants;
import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.CrashManagerListener;
import net.hockeyapp.android.metrics.MetricsManager;
import net.hockeyapp.android.objects.CrashManagerUserInput;
import net.hockeyapp.android.objects.CrashMetaData;

import java.lang.ref.WeakReference;

/**
 * Utility class to optionally initialize HockeyApp crash reporting
 * <br>
 * Populate description with additional information.
 *
 * @author Dennis Lang on 11/15/16.
 */

public class AppCrash extends CrashManagerListener {

    private final Context mContext;

    private AppCrash(Context context) {
        mContext = context;
    }

    public static void initalize(Application application, boolean isDebug) {
        int keyResId = ((Context) application).getResources().getIdentifier("hockeyapp_key", "string", ((Context) application).getPackageName());
        int pkgResId = ((Context) application).getResources().getIdentifier("hockeyapp_pkg", "string", ((Context) application).getPackageName());
        if (keyResId > 0 && pkgResId > 0) {
            final String HOCKEY_APP_ID = ((Context) application).getResources().getString(keyResId);
            final String HOCKEY_APP_PKG = ((Context) application).getResources().getString(pkgResId);

            // HockeyApp Crash reporting
            // HockeyLog.setLogLevel(Log.VERBOSE);     // For debug testing only !!!!!

            /*
             *  Initialize HockeyApp manually so we can modify the package name to a fixed package
             *  to force all trace to aggregate under one account.
             *
             *  Use the meta data to identify individual client information.
             *
             *  Call execute to post any previously generated traces.
             */
            AppCrash hockeyAppCrashListener = new AppCrash(application);
            CrashManager.initialize(application, HOCKEY_APP_ID, hockeyAppCrashListener);
            Constants.APP_PACKAGE = HOCKEY_APP_PKG;
            CrashMetaData crashMetaData = new CrashMetaData();
            crashMetaData.setUserDescription(((Context) application).getApplicationInfo().processName);
            crashMetaData.setUserID(((Context) application).getPackageName());
            CrashManager.handleUserInput(CrashManagerUserInput.CrashManagerUserInputAlwaysSend, crashMetaData, hockeyAppCrashListener,
                    new WeakReference<>(application), false);
            CrashManager.execute(application, hockeyAppCrashListener);

            MetricsManager.register(application, HOCKEY_APP_ID);
            MetricsManager.trackEvent("start");
        }
    }

    @Override
    public boolean shouldAutoUploadCrashes() {
        return true;
    }

    /**
     * Please all Log 'errors' in description.
     * @return Crash description
     */
    public String getDescription() {
        String description = "";
        String eol = System.getProperty("line.separator");

        description = addRes(description, "TargetSDK=", "targetSdkVersion");
        description = addRes(description, "CompilerSDK=", "compileSdkVersion");
        description = addRes(description, "BuildTools=", "buildToolsVersion");

        return description;
    }

    private String addRes(String inStr, String title, String resName) {
        int resId = mContext.getResources().getIdentifier(
                resName, "string", mContext.getPackageName());
        if (resId > 0) {
            inStr += title + mContext.getResources().getString(resId);
        }

        return inStr;
    }
}

package com.qiniu.pili.droid.streaming.demo;

import android.app.Application;
import android.content.Intent;

import com.faceunity.nama.FURenderer;
import com.qiniu.pili.droid.streaming.StreamingEnv;
import com.qiniu.pili.droid.streaming.demo.service.KeepAppAliveService;
import com.qiniu.pili.droid.streaming.demo.utils.AppStateTracker;

public class StreamingApplication extends Application {

    private boolean mIsServiceAlive;
    private Intent mServiceIntent;
    private static StreamingApplication streamingApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        streamingApplication = this;
        /**
         * init must be called before any other func
         */
        StreamingEnv.init(getApplicationContext());
        FURenderer.getInstance().setup(getApplicationContext());

        /**
         * track app background state to avoid possibly stopping microphone recording
         * in screen streaming mode on Android P+
         */
        AppStateTracker.track(this, new AppStateTracker.AppStateChangeListener() {
            @Override
            public void appTurnIntoForeground() {
                stopService();
            }

            @Override
            public void appTurnIntoBackGround() {
                startService();
            }

            @Override
            public void appDestroyed() {
                stopService();
            }
        });
    }

    public static StreamingApplication getInstance(){
        return streamingApplication;
    }

    /**
     * start foreground service to make process not turn to idle state
     *
     * on Android P+, it doesn't allow recording audio to protect user's privacy in idle state.
     */
    private void startService() {
        if (mServiceIntent == null) {
            mServiceIntent = new Intent(StreamingApplication.this, KeepAppAliveService.class);
        }
        startService(mServiceIntent);
        mIsServiceAlive = true;
    }

    private void stopService() {
        if (mIsServiceAlive) {
            stopService(mServiceIntent);
            mServiceIntent = null;
            mIsServiceAlive = false;
        }
    }
}

package com.qiniu.pili.droid.streaming.demo;

import android.app.Application;

import com.qiniu.pili.droid.streaming.StreamingEnv;

public class StreamingApplication extends Application {

    private static StreamingApplication streamingApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        streamingApplication = this;
        /**
         * init must be called before any other func
         */
        StreamingEnv.init(getApplicationContext());
    }

    public static StreamingApplication getInstance() {
        return streamingApplication;
    }
}

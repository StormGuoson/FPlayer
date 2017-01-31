package com.liu.finalplayer.activity;

import android.app.Application;

import io.vov.vitamio.Vitamio;

/**
 * Created by StormGuoson on 2017/1/12.
 */

public class AppI extends Application {
    private static AppI instance;

    public static AppI getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Vitamio.isInitialized(this);
        instance = this;
    }
}

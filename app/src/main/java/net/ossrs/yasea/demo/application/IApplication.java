package net.ossrs.yasea.demo.application;

import android.app.Application;
import android.util.Log;

import net.ossrs.yasea.demo.BuildConfig;
import net.ossrs.yasea.demo.bean.equipment.MyObjectBox;

import io.objectbox.BoxStore;
import io.objectbox.android.AndroidObjectBrowser;

public class IApplication extends Application {

    private String TAG = IApplication.class.getName();
    public static IApplication sApp;
    public static BoxStore boxStore;

    @Override
    public void onCreate() {
        super.onCreate();
        sApp = this;
        boxStore = MyObjectBox.builder().androidContext(this).build();
        if (BuildConfig.DEBUG) {
            boolean started = new AndroidObjectBrowser(boxStore).start(this);
            Log.i(TAG+"-ObjectBrowser-", "Started: " + started);
        }

    }

    public static IApplication getInstance() {
        return sApp;
    }

    public BoxStore getBoxStore() {
        return boxStore;
    }

}
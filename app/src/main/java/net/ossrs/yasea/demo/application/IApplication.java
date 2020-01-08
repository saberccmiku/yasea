package net.ossrs.yasea.demo.application;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import com.xuexiang.xupdate.XUpdate;
import com.xuexiang.xupdate.utils.UpdateUtils;

import net.ossrs.yasea.demo.BuildConfig;
import net.ossrs.yasea.demo.bean.equipment.MyObjectBox;
import net.ossrs.yasea.demo.net.update.OKHttpUpdateHttpService;

import io.objectbox.BoxStore;
import io.objectbox.android.AndroidObjectBrowser;

import static com.xuexiang.xupdate.entity.UpdateError.ERROR.CHECK_NO_NEW_VERSION;

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
            Log.i(TAG + "-ObjectBrowser-", "Started: " + started);
        }

        //设置版本更新出错的监听
        XUpdate.get()
                .debug(true)
                .isWifiOnly(true)                                               //默认设置只在wifi下检查版本更新
                .isGet(true)                                                    //默认设置使用get请求检查版本
                .isAutoMode(false)                                              //默认设置非自动模式，可根据具体使用配置
                .param("versionCode", UpdateUtils.getVersionCode(this))         //设置默认公共请求参数
                .param("appKey", getPackageName())
                .setOnUpdateFailureListener(error -> {
                    if (error.getCode() != CHECK_NO_NEW_VERSION) {          //对不同错误进行处理
                        Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show();
                    }
                })
                .supportSilentInstall(true)                                     //设置是否支持静默安装，默认是true
                .setIUpdateHttpService(new OKHttpUpdateHttpService())           //这个必须设置！实现网络请求功能。
                .setILogger((priority, tag, message, t) -> Log.d(TAG, "log: " + message))
                .init(this);

    }

    public static IApplication getInstance() {
        return sApp;
    }

    public BoxStore getBoxStore() {
        return boxStore;
    }

}
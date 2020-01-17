package net.ossrs.yasea.demo.base;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import net.ossrs.yasea.demo.R;
import net.ossrs.yasea.demo.application.IApplication;
import net.ossrs.yasea.demo.bean.equipment.Config;
import net.ossrs.yasea.demo.util.ResCode;
import net.ossrs.yasea.demo.util.permission.PermissionListener;
import net.ossrs.yasea.demo.util.permission.PermissionsUtil;
import net.ossrs.yasea.demo.view.SplashActivity;
import net.ossrs.yasea.demo.widget.LoadingDialog;

import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import butterknife.ButterKnife;
import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * @author fjy
 */
public abstract class BaseActivity extends RxAppCompatActivity {

    private String TAG = BaseActivity.class.getName();

    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置无标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //透明状态栏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //透明导航栏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        //避免进入页面EdiText自动弹出软键盘
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        @SuppressLint("HardwareIds") String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (!TextUtils.isEmpty(androidId) && androidId.equals("36c210b069ec60de")) {
            //由于横屏有两个方向的横法，而这个设置横屏的语句，假设不是默认的横屏方向，会把已经横屏的屏幕旋转180°。

            //所以能够先推断是否已经为横屏了。假设不是再旋转，不会让用户认为转的莫名其妙啦！代码例如以下：
            if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        } else {
            //设置竖屏代码：
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//竖屏
        }

        //设置横屏代码：
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//横屏


        setContentView(this.getLayoutId());
        ButterKnife.bind(this);
        PermissionListener listener = new PermissionListener() {
            @Override
            public void permissionGranted(@NonNull String[] permissions) {
                initView();
                load();
            }

            @Override
            public void permissionDenied(@NonNull String[] permissions) {

            }
        };
        PermissionsUtil.requestPermission(this, listener, NEEDED_PERMISSIONS);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 设置布局
     *
     * @return
     */
    public abstract int getLayoutId();

    /**
     * 初始化视图
     */
    public abstract void initView();

    /**
     * 加载数据
     */
    public void load() {

    }

    public Socket getSocketIO(String socketUrl) {
        Socket mSocket;
        try {
            Log.i(TAG, "数据交互socket系统开始连接.............");
            mSocket = IO.socket(socketUrl);
            mSocket.on(Socket.EVENT_CONNECT, args -> Log.i(TAG, "数据交互socket系统连接成功"))
                    .on(Socket.EVENT_DISCONNECT, args -> Log.i(TAG, "数据交互socket系统断开连接"))
                    .on(Socket.EVENT_ERROR, args -> Log.i(TAG, ResCode.CENTER_SERVER_EVENT_ERROR.getMsg()))
                    .on(Socket.EVENT_CONNECT_ERROR, args -> {
                        if (args[0] instanceof Throwable) {
                            ((Throwable) args[0]).printStackTrace();
                        }
                        Log.i(TAG, ResCode.CENTER_SERVER_EVENT_CONNECT_ERROR.getMsg() + args[0].toString());
                    });

            mSocket.connect();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return mSocket;
    }

    private class MyHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    private class MyTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            //这里不能返回null 否则会报空指针错误
            X509Certificate[] x509Certificates = new X509Certificate[0];
            return x509Certificates;
        }
    }


}

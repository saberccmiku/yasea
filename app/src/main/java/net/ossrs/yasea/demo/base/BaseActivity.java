package net.ossrs.yasea.demo.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import net.ossrs.yasea.demo.util.ResCode;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import butterknife.ButterKnife;
import io.socket.client.IO;
import io.socket.client.Socket;
import okhttp3.OkHttpClient;

/**
 * @author fjy
 */
public abstract class BaseActivity extends RxAppCompatActivity {

    private String TAG = BaseActivity.class.getName();

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
        setContentView(this.getLayoutId());
        ButterKnife.bind(this);
        initView();
        load();
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
                        Log.i(TAG, ResCode.CENTER_SERVER_EVENT_CONNECT_ERROR.getMsg()+args[0].toString());
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

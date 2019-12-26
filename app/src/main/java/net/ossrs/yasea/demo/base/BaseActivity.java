package net.ossrs.yasea.demo.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import net.ossrs.yasea.demo.util.Constants;

import java.net.URISyntaxException;

import butterknife.ButterKnife;
import io.socket.client.IO;
import io.socket.client.Socket;

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
            mSocket.on(Socket.EVENT_CONNECT, args -> Log.i(TAG, "数据交互socket系统连接成功")).
                    on(Socket.EVENT_DISCONNECT, args -> Log.i(TAG, "数据交互socket系统断开连接")).
                    on("event", args -> {

                    }).on(Socket.EVENT_ERROR, args -> Log.i(TAG, "数据交互socket系统异常")).
                    on(Socket.EVENT_CONNECT_ERROR, args -> Log.i(TAG, "数据交互socket系统连接异常"));

            mSocket.connect();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return mSocket;
    }


}

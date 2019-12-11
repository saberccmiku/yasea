package net.ossrs.yasea.demo.application;

import android.app.Application;
import android.util.Log;

import net.ossrs.yasea.demo.BuildConfig;
import net.ossrs.yasea.demo.bean.equipment.MyObjectBox;
import net.ossrs.yasea.demo.util.Constants;

import java.net.URISyntaxException;

import io.objectbox.BoxStore;
import io.objectbox.android.AndroidObjectBrowser;
import io.socket.client.IO;
import io.socket.client.Socket;

public class IApplication extends Application {

    private Socket mSocket;
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
            Log.i("ObjectBrowser", "Started: " + started);
        }

        try {
            Log.i(TAG, "socket开始连接.............");
            mSocket = IO.socket(Constants.SOCKET_SERVER_URL);
            mSocket.on(Socket.EVENT_CONNECT, args -> Log.i(TAG, "socket连接成功")).
                    on(Socket.EVENT_DISCONNECT, args -> Log.i(TAG, "socket断开连接")).
                    on("event", args -> {

                    }).on(Socket.EVENT_ERROR, args -> Log.i(TAG, "socket异常")).
                    on(Socket.EVENT_CONNECT_ERROR, args -> Log.i(TAG, "socket连接异常"));

            mSocket.connect();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }


    }

    public Socket getSocket() {
        return mSocket;
    }

    public static IApplication getInstance() {
        return sApp;
    }

    public BoxStore getBoxStore() {
        return boxStore;
    }


}
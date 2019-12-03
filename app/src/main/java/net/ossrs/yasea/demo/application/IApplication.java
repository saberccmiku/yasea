package net.ossrs.yasea.demo.application;

import android.app.Application;
import android.util.Log;

import net.ossrs.yasea.demo.util.Constants;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class IApplication extends Application {

    private Socket mSocket;
    private String TAG = IApplication.class.getName();

    @Override
    public void onCreate() {

        {
            try {
                Log.i(TAG, "socket开始连接.............");
                mSocket = IO.socket(Constants.CHAT_SERVER_URL);
                mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        mSocket.emit("text","1123");
                        Log.i(TAG, "socket连接成功");
                    }
                }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Log.i(TAG, "socket断开连接");
                    }
                }).on("event", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {

                    }
                }).on(Socket.EVENT_ERROR, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Log.i(TAG, "socket异常");
                    }
                }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Log.i(TAG, "socket连接异常");
                    }
                });
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        super.onCreate();
    }

    public Socket getSocket() {
        return mSocket;
    }

}

package net.ossrs.yasea.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

import net.ossrs.yasea.demo.application.IApplication;
import net.ossrs.yasea.demo.model.UserInfo;
import net.ossrs.yasea.demo.util.permission.PermissionListener;
import net.ossrs.yasea.demo.util.permission.PermissionsUtil;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.OnClick;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

@SuppressLint("Registered")
public class LoginActivity extends BaseActivity {

    private Socket mSocket;
    private IHandler iHandler;
    @BindView(R.id.et_user)
    EditText mUserNameView;

    @Override
    public int getLayoutId() {
        return R.layout.activity_login;
    }

    @Override
    public void initViews(Bundle savedInstanceState) {

        final String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
        };


        final PermissionListener permissionListener = new PermissionListener() {
            @Override
            public void permissionGranted(@NonNull String[] permissions) {
                ActivityCompat.requestPermissions(LoginActivity.this, permissions, 100);
            }

            @Override
            public void permissionDenied(@NonNull String[] permissions) {

            }
        };

        PermissionsUtil.requestPermission(LoginActivity.this, permissionListener, permissions);

        iHandler = new IHandler(this);
        IApplication app = (IApplication) getApplication();
        mSocket = app.getSocket();
        mSocket.on("join", onLogin);
    }

    @Override
    public void initToolBar() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocket.off();
        mSocket.disconnect();
    }

    /**
     * Attempts to sign in the account specified by the login form.
     * If there are form errors (invalid username, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    @OnClick(R.id.btn_sign_in)
    public void attemptLogin() {
        // Reset errors.
        mUserNameView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUserNameView.getText().toString().trim();

        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            mUserNameView.setError("用户名不能为空");
            mUserNameView.requestFocus();
            return;
        }

        // perform the user login attempt.
        Gson gson = new Gson();
        String s = gson.toJson(new UserInfo(username));
        mSocket.emit("text", s);
    }

    private Emitter.Listener onLogin = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            iHandler.sendEmptyMessage(1);
        }
    };

    private static class IHandler extends Handler {
        private WeakReference<LoginActivity> weakReference;
        private LoginActivity loginActivity;

        public IHandler(LoginActivity activity) {
            weakReference = new WeakReference<>(activity);
            loginActivity = weakReference.get();
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Intent intent = new Intent();
                    intent.setClass(loginActivity, MainActivity.class);
                    intent.putExtra("userName", loginActivity.mUserNameView.getText().toString());
                    loginActivity.startActivity(intent);
                    loginActivity.finish();
                    break;
                case -1:
                    Toast.makeText(loginActivity, "账号或者密码错误", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

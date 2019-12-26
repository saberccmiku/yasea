package net.ossrs.yasea.demo.view;

import android.Manifest;
import android.content.Intent;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;

import net.ossrs.yasea.demo.R;
import net.ossrs.yasea.demo.application.IApplication;
import net.ossrs.yasea.demo.base.BaseActivity;
import net.ossrs.yasea.demo.bean.equipment.Config;
import net.ossrs.yasea.demo.bean.equipment.ConfigPattern;
import net.ossrs.yasea.demo.bean.equipment.Config_;
import net.ossrs.yasea.demo.util.CommonUtil;
import net.ossrs.yasea.demo.util.Constants;
import net.ossrs.yasea.demo.util.ResCode;
import net.ossrs.yasea.demo.util.permission.PermissionListener;
import net.ossrs.yasea.demo.util.permission.PermissionsUtil;
import net.ossrs.yasea.demo.widget.LoadingDialog;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import io.objectbox.Box;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SplashActivity extends BaseActivity {

    private final String BTN_ACTIVE_ACTIVE = "立即激活";
    private final String BTN_ACTIVE_RETRY = "重试";
    private final String BTN_ACTIVE_CONFIG = "前往配置";
    private Box<Config> netConfigBox;
    private LoadingDialog dialog;
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE
    };


    @BindView(R.id.tv_status)
    TextView tvStatus;
    @BindView(R.id.btn_active)
    Button btnActive;

    @OnClick(R.id.ib_setting)
    public void toActiveActivity() {
        startActivity(new Intent(SplashActivity.this, ActiveActivity.class));
        this.finish();
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_splash;
    }

    @Override
    public void initView() {

        PermissionListener listener = new PermissionListener() {
            @Override
            public void permissionGranted(@NonNull String[] permissions) {

                dialog = new LoadingDialog(SplashActivity.this,R.style.mdialog);
                dialog.show();


                //获取数据库信息
                netConfigBox = IApplication.boxStore.boxFor(Config.class);
                //检查服务状态
                checkServer();
            }

            @Override
            public void permissionDenied(@NonNull String[] permissions) {
                SplashActivity.this.finish();
            }
        };
        PermissionsUtil.requestPermission(this, listener, NEEDED_PERMISSIONS);
    }

    private void checkServer() {
        if (CommonUtil.isNetworkAvailable(this)) {
            //检查配置信息
            checkConfig();
        } else {
            dialog.cancel();
            tvStatus.setText(ResCode.NETWORK_ERROR.getMsg());
        }
    }

    private void checkLiveServer() {

        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            List<Config> configList = netConfigBox.query()
                    .equal(Config_.title, ConfigPattern.MONITOR).build().find();
            String liveIp = null;
            String livePort = "0";
            for (Config config : configList) {
                if (config.getLabel().equals(ConfigPattern.SERVER)) {
                    liveIp = config.getInput();
                } else if (config.getLabel().equals(ConfigPattern.PORT)) {
                    livePort = config.getInput();
                }
            }
            emitter.onNext(CommonUtil.testServerConnect(liveIp, Integer.valueOf(livePort)));

        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        if (aBoolean) {
//                            RetrofitHelper.getAppAPI()//基础URL
//                                    .getRecommendedInfo()//接口后缀URL
//                                    .compose(RxLifecycle.bindUntilEvent(lifecycle(), ActivityEvent.DESTROY))//设计是否备份数据
//                                    .delay(1, TimeUnit.SECONDS)
//                                    //.map(RecommendInfo::getResult)//得到JSON子数组
//                                    .subscribeOn(Schedulers.io())//设计线程读写方式
//                                    .observeOn(AndroidSchedulers.mainThread())//指定线程运行的位置
//                                    .subscribe(new Observer<RecommendInfo>() {
//                                        @Override
//                                        public void onSubscribe(Disposable d) {
//
//                                        }
//
//                                        @Override
//                                        public void onNext(RecommendInfo recommendInfo) {
//                                            activeEngine();
//                                        }
//
//                                        @Override
//                                        public void onError(Throwable e) {
//                                            Toast.makeText(SplashActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
//                                        }
//
//                                        @Override
//                                        public void onComplete() {
//                                        }
//                                    });

                            checkCenterServer();

                        } else {
                            dialog.cancel();
                            tvStatus.setText(ResCode.LIVE_SERVER_ERROR.getMsg());
                            btnActive.setText(BTN_ACTIVE_RETRY);
                            btnActive.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });


    }

    /**
     * 检查数据解析服务系统
     */
    private void checkCenterServer() {

        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            String ip = null;
            String port = "0";
            List<Config> configList = netConfigBox.query()
                    .equal(Config_.title, ConfigPattern.NETWORK).build().find();
            for (Config config : configList) {
                if (config.getLabel().equals(ConfigPattern.SERVER)) {
                    ip = config.getInput();
                } else if (config.getLabel().equals(ConfigPattern.PORT)) {
                    port = config.getInput();
                }
            }
            emitter.onNext(CommonUtil.testServerConnect(ip, Integer.valueOf(port)));
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        if (aBoolean) {
                            //检查系统是否激活
                            activeEngine();
                        } else {
                            dialog.cancel();
                            tvStatus.setText(ResCode.CENTER_SERVER_ERROR.getMsg());
                            btnActive.setText(BTN_ACTIVE_RETRY);
                            btnActive.setVisibility(View.VISIBLE);
                        }

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    /**
     * 检查配置信息
     */
    private void checkConfig() {
        Observable.create((ObservableOnSubscribe<List<Config>>) emitter -> emitter.onNext(netConfigBox.getAll()))
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<Config>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(List<Config> list) {
                        boolean isComplete = true;//判断信息是否填写完整
                        if (list != null && list.size() != 0) {
                            for (Config config : list) {
                                if (!TextUtils.isEmpty(config.getTitle()) && TextUtils.isEmpty(config.getInput())) {
                                    isComplete = false;
                                    break;
                                }
                            }
                        } else {
                            isComplete = false;
                        }
                        if (!isComplete) {
                            dialog.cancel();
                            tvStatus.setText("配置信息不全,请前往配置");
                            btnActive.setText(BTN_ACTIVE_CONFIG);
                            btnActive.setVisibility(View.VISIBLE);
                        } else {
                            //检测直播服务服务
                            checkLiveServer();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @OnClick(R.id.btn_active)
    public void goActiveView() {
        if (!TextUtils.isEmpty(btnActive.getText())) {
            switch (btnActive.getText().toString()) {
                case BTN_ACTIVE_ACTIVE://前往激活
                    toActiveActivity();
                    break;
                case BTN_ACTIVE_RETRY://重试
                    //检测服务状态
                    dialog.show();
                    checkServer();
                    break;
                case BTN_ACTIVE_CONFIG://前往配置
                    toActiveActivity();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 检查设备是否激活
     */
    public void activeEngine() {
        if (!net.ossrs.yasea.demo.util.permission.CommonUtil.checkPermissions(this, NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
            return;
        }
        Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
            FaceEngine faceEngine = new FaceEngine();
            int activeCode = faceEngine.active(SplashActivity.this, Constants.APP_ID, Constants.SDK_KEY);
            emitter.onNext(activeCode);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Integer activeCode) {
                        if (activeCode == ErrorInfo.MOK || activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
                            List<Config> configList = netConfigBox.getAll();
                            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                            intent.putParcelableArrayListExtra("config", (ArrayList<? extends Parcelable>) configList);
                            startActivity(intent);
                            SplashActivity.this.finish();
                        } else {
                            String text = "人脸识别引擎未激活" + activeCode;
                            tvStatus.setText(text);
                            btnActive.setText(BTN_ACTIVE_ACTIVE);
                            btnActive.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }
}

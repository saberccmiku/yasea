package net.ossrs.yasea.demo.view;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.trello.rxlifecycle2.RxLifecycle;
import com.trello.rxlifecycle2.android.ActivityEvent;

import net.ossrs.yasea.demo.R;
import net.ossrs.yasea.demo.application.IApplication;
import net.ossrs.yasea.demo.base.BaseActivity;
import net.ossrs.yasea.demo.bean.RecommendInfo;
import net.ossrs.yasea.demo.bean.equipment.Config;
import net.ossrs.yasea.demo.net.RetrofitHelper;
import net.ossrs.yasea.demo.util.CommonUtil;
import net.ossrs.yasea.demo.util.ResCode;

import java.util.List;
import java.util.concurrent.TimeUnit;

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

        //获取数据库信息
        netConfigBox = IApplication.boxStore.boxFor(Config.class);
        if (CommonUtil.isNetworkAvailable(this)) {
            //检查配置信息
            checkConfig();
        } else {
            tvStatus.setText(ResCode.NETWORK_ERROR.getMsg());
        }
    }

    private void checkServer() {

        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            List<Config> configList = netConfigBox.getAll();
            String liveIp = null;
            String livePort = null;
            for (Config config : configList) {
                if (!TextUtils.isEmpty(config.getTitle()) && config.getTitle().equals("监控配置")) {
                    if (config.getLabel().equals("服务器")) {
                        liveIp = config.getInput();
                    } else if (config.getLabel().equals("端口号")) {
                        livePort = config.getInput();
                    }
                }
            }
            assert livePort != null;
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
                            RetrofitHelper.getAppAPI()//基础URL
                                    .getRecommendedInfo()//接口后缀URL
                                    .compose(RxLifecycle.bindUntilEvent(lifecycle(), ActivityEvent.DESTROY))//设计是否备份数据
                                    .delay(1, TimeUnit.SECONDS)
                                    //.map(RecommendInfo::getResult)//得到JSON子数组
                                    .subscribeOn(Schedulers.io())//设计线程读写方式
                                    .observeOn(AndroidSchedulers.mainThread())//指定线程运行的位置
                                    .subscribe(new Observer<RecommendInfo>() {
                                        @Override
                                        public void onSubscribe(Disposable d) {

                                        }

                                        @Override
                                        public void onNext(RecommendInfo recommendInfo) {
                                            tvStatus.setText(ResCode.EQUIPMENT_NOT_ACTIVE.getMsg());
                                            btnActive.setText(BTN_ACTIVE_ACTIVE);
                                            btnActive.setVisibility(View.VISIBLE);
                                        }

                                        @Override
                                        public void onError(Throwable e) {
                                            Toast.makeText(SplashActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onComplete() {
                                        }
                                    });
                        } else {
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
                            tvStatus.setText("配置信息不全,请前往配置");
                            btnActive.setText(BTN_ACTIVE_CONFIG);
                            btnActive.setVisibility(View.VISIBLE);
                        } else {
                            //检测服务
                            checkServer();
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
                    //检测服务
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
}

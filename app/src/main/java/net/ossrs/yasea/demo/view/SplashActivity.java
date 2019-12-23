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
import net.ossrs.yasea.demo.base.BaseActivity;
import net.ossrs.yasea.demo.bean.RecommendInfo;
import net.ossrs.yasea.demo.net.RetrofitHelper;
import net.ossrs.yasea.demo.util.CommonUtil;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SplashActivity extends BaseActivity {

    private final String BTN_ACTIVE_ACTIVE = "立即激活";
    private final String BTN_ACTIVE_RETRY = "重试";

    @BindView(R.id.tv_status)
    TextView tvStatus;
    @BindView(R.id.btn_active)
    Button btnActive;

    @Override
    public int getLayoutId() {
        return R.layout.activity_splash;
    }

    @Override
    public void initView() {

        //检测服务
        checkServer();
    }

    private void checkServer() {
        if (CommonUtil.isNetworkAvailable(this)) {
            Observable.create((ObservableOnSubscribe<Boolean>) emitter -> emitter.onNext(CommonUtil.testServerConnect("192.168.1.25", 80)))
                    .subscribeOn(Schedulers.io())
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
                                                tvStatus.setText("设备未激活");
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
                                tvStatus.setText("设备未激活");
                                btnActive.setText(BTN_ACTIVE_RETRY);
                                btnActive.setVisibility(View.VISIBLE);
                                Toast.makeText(SplashActivity.this, "直播服务不存在或未开启", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } else {
            Toast.makeText(this, "网络异常", Toast.LENGTH_SHORT).show();
        }

    }

    @OnClick(R.id.btn_active)
    public void goActiveView() {
        if (!TextUtils.isEmpty(btnActive.getText())) {
            switch (btnActive.getText().toString()) {
                case BTN_ACTIVE_ACTIVE:
                    startActivity(new Intent(SplashActivity.this, ActiveActivity.class));
                    this.finish();
                    break;
                case BTN_ACTIVE_RETRY:
                    //检测服务
                    checkServer();
                    break;
                default:
                    break;
            }
        }
    }
}

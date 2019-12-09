package net.ossrs.yasea.demo.view;

import android.content.Intent;
import android.widget.TextView;
import com.trello.rxlifecycle2.RxLifecycle;
import com.trello.rxlifecycle2.android.ActivityEvent;

import net.ossrs.yasea.demo.R;
import net.ossrs.yasea.demo.base.BaseActivity;
import net.ossrs.yasea.demo.bean.RecommendInfo;
import net.ossrs.yasea.demo.net.RetrofitHelper;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
public class SplashActivity extends BaseActivity {
    @Override
    public int getLayoutId() {
        return R.layout.activity_splash;
    }

    @BindView(R.id.tv_status)
    TextView tvStatus;

    @Override
    public void initView() {

        RetrofitHelper.getAppAPI()//基础URL
                .getRecommendedInfo()//接口后缀URL
                .compose(RxLifecycle.bindUntilEvent(lifecycle(), ActivityEvent.DESTROY))//设计是否备份数据
                //.map(RecommendInfo::getResult)//得到JSON子数组
                .subscribeOn(Schedulers.io())//设计线程读写方式
                .observeOn(AndroidSchedulers.mainThread())//指定线程运行的位置
                .subscribe(new Observer<RecommendInfo>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(RecommendInfo recommendInfo) {

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
        startActivity(new Intent(SplashActivity.this, ActiveActivity.class));
        this.finish();
    }
}

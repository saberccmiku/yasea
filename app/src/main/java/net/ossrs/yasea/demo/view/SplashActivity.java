package net.ossrs.yasea.demo.view;

import android.support.v7.app.ActionBar;

import net.ossrs.yasea.demo.R;
import net.ossrs.yasea.demo.base.BaseActivity;

import butterknife.OnClick;

public class SplashActivity extends BaseActivity {
    @Override
    public int getLayoutId() {
        return R.layout.activity_splash;
    }

    @Override
    public void initView() {
        //方式一：这句代码必须写在setContentView()方法的后面
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            getSupportActionBar().hide();
        }
    }

    @OnClick(R.id.status)
    public void goActivieView(){

    }
}

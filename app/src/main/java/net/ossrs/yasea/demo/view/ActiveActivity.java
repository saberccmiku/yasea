package net.ossrs.yasea.demo.view;

import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import net.ossrs.yasea.demo.R;
import net.ossrs.yasea.demo.adapter.CommonRecyclerAdapter;
import net.ossrs.yasea.demo.base.BaseActivity;

import butterknife.BindView;
import butterknife.OnClick;

public class ActiveActivity extends BaseActivity {

    @BindView(R.id.rv_active)
    RecyclerView rvActive;

    @Override
    public int getLayoutId() {
        return R.layout.activity_active;
    }

    @Override
    public void initView() {
        rvActive.setLayoutManager(new LinearLayoutManager(this));
    }

    @OnClick(R.id.back)
    public void back() {
        startActivity(new Intent(ActiveActivity.this, SplashActivity.class));
        ActiveActivity.this.finish();
    }
}

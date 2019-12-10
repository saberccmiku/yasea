package net.ossrs.yasea.demo.view;

import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;

import net.ossrs.yasea.demo.R;
import net.ossrs.yasea.demo.adapter.CommonRecyclerAdapter;
import net.ossrs.yasea.demo.adapter.CommonRecyclerViewHolder;
import net.ossrs.yasea.demo.application.IApplication;
import net.ossrs.yasea.demo.base.BaseActivity;
import net.ossrs.yasea.demo.bean.equipment.Config;
import net.ossrs.yasea.demo.bean.equipment.NetConfig;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import io.objectbox.Box;

public class ActiveActivity extends BaseActivity {

    private List<Config> configList;
    private CommonRecyclerAdapter<Config> adapter;

    @BindView(R.id.rv_active)
    RecyclerView rvActive;

    @Override
    public int getLayoutId() {
        return R.layout.activity_active;
    }

    @Override
    public void initView() {
        configList = new ArrayList<>();
        rvActive.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CommonRecyclerAdapter<Config>(this, R.layout.item_config_content, configList) {
            @Override
            public void convertView(CommonRecyclerViewHolder holder, Config config) {
                if (config.isTitle()){
                    LinearLayout llItemConfig = holder.getView(R.id.ll_item_config);
                    llItemConfig.setBackground(null);
                    holder.getView(R.id.et_input).setVisibility(View.GONE);
                }
                holder.setText(R.id.tv_label, config.getLabel());
                holder.setEditText(R.id.et_input, config.getInput());
            }
        };
        rvActive.setAdapter(adapter);
    }

    @OnClick(R.id.back)
    public void back() {
        startActivity(new Intent(ActiveActivity.this, SplashActivity.class));
        ActiveActivity.this.finish();
    }

    @Override
    public void load() {
        Box<NetConfig> netConfigBox = IApplication.boxStore.boxFor(NetConfig.class);
        //网络配置
        configList.add(new Config("网络配置", true));
        configList.add(new Config("服务器", "192.168.0.112"));
        configList.add(new Config("端口号", "44238"));
        //监控配置
        configList.add(new Config("监控配置", true));
        configList.add(new Config("服务器", "192.168.0.254"));
        configList.add(new Config("端口号", "1183"));
        //本机配置
        configList.add(new Config("本机配置", true));
        configList.add(new Config("序列号", "XXSQFE0023158"));
        configList.add(new Config("工位号", "15368"));
        configList.add(new Config("机器码", "1183"));
        adapter.notifyDataSetChanged();
    }
}

package net.ossrs.yasea.demo.view;

import android.Manifest;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.ossrs.yasea.demo.R;
import net.ossrs.yasea.demo.adapter.CommonRecyclerAdapter;
import net.ossrs.yasea.demo.adapter.CommonRecyclerViewHolder;
import net.ossrs.yasea.demo.application.IApplication;
import net.ossrs.yasea.demo.base.BaseActivity;
import net.ossrs.yasea.demo.bean.equipment.Config;
import net.ossrs.yasea.demo.bean.equipment.NetConfig;
import net.ossrs.yasea.demo.util.permission.CommonUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import io.objectbox.Box;

public class ActiveActivity extends BaseActivity {

    private List<Config> configList;
    private CommonRecyclerAdapter<Config> adapter;
    private boolean isActiveSuccess ;
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA
    };

    @BindView(R.id.rv_active)
    RecyclerView rvActive;
    @BindView(R.id.btn_operate)
    Button btnOperate;
    @BindView(R.id.iv_back)
    ImageButton ivBack;
    @BindView(R.id.ll_config)
    LinearLayout llConfig;
    @BindView(R.id.rl_operate_status)
    RelativeLayout rlOperateStatus;
    @BindView(R.id.iv_status)
    ImageView ivStatus;
    @BindView(R.id.tv_status)
    TextView tvStatus;
    @BindView(R.id.tv_equipment_status)
    TextView tvEquipmentStatus;


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
                if (config.isTitle()) {
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

    @OnClick(R.id.iv_back)
    public void back() {
        startActivity(new Intent(ActiveActivity.this, SplashActivity.class));
        ActiveActivity.this.finish();
    }

    @OnClick(R.id.btn_operate)
    public void operate() {
        if (!TextUtils.isEmpty(tvStatus.getText())) {
            switch (tvStatus.getText().toString()) {
                case "激活成功":
                    if (!CommonUtil.checkPermissions(ActiveActivity.this, NEEDED_PERMISSIONS)) {
                        ActivityCompat.requestPermissions(ActiveActivity.this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
                        return;
                    }
                    startActivity(new Intent(ActiveActivity.this, MainActivity.class));
                    ActiveActivity.this.finish();
                    break;
                case "激活失败":
                    llConfig.setVisibility(View.VISIBLE);
                    rlOperateStatus.setVisibility(View.GONE);
                    ivBack.setVisibility(View.VISIBLE);
                    btnOperate.setText("立即激活");
                    tvStatus.setText(null);
                    break;
                default:
                    break;
            }
        } else {
            llConfig.setVisibility(View.GONE);
            rlOperateStatus.setVisibility(View.VISIBLE);
            if (isActiveSuccess) {
                ivStatus.setBackgroundResource(R.drawable.ic_success);
                tvStatus.setText("激活成功");
                tvEquipmentStatus.setText("设备激活成功");

            } else {
                ivStatus.setBackgroundResource(R.drawable.ic_failed);
                tvStatus.setText("激活失败");
                tvEquipmentStatus.setText("错误原因");
                isActiveSuccess = true;//正式使用这个要去掉 测试使用
            }
            btnOperate.setText("完成");
            ivBack.setVisibility(View.GONE);
        }
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

package net.ossrs.yasea.demo.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
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
import android.widget.Toast;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;

import net.ossrs.yasea.demo.R;
import net.ossrs.yasea.demo.adapter.CommonRecyclerAdapter;
import net.ossrs.yasea.demo.adapter.CommonRecyclerViewHolder;
import net.ossrs.yasea.demo.application.IApplication;
import net.ossrs.yasea.demo.base.BaseActivity;
import net.ossrs.yasea.demo.bean.equipment.Config;
import net.ossrs.yasea.demo.bean.equipment.NetConfig;
import net.ossrs.yasea.demo.util.Constants;
import net.ossrs.yasea.demo.util.permission.CommonUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import io.objectbox.Box;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ActiveActivity extends BaseActivity {

    private List<Config> configList;
    private CommonRecyclerAdapter<Config> adapter;
    private boolean isActiveSuccess;
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
                    //保存数据
                    Box<NetConfig> netConfigBox = IApplication.boxStore.boxFor(NetConfig.class);
                    netConfigBox.put(new NetConfig());

                    startActivity(new Intent(ActiveActivity.this, RegisterAndRecognizeActivity.class));
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
                activeEngine(null);

            } else {
                ivStatus.setBackgroundResource(R.drawable.ic_failed);
                tvStatus.setText("激活失败");
                tvEquipmentStatus.setText("错误原因");
                isActiveSuccess = true;//正式使用这个要去掉 测试使用
            }
            btnOperate.setText("完成");
            ivBack.setVisibility(View.GONE);
        }

//        List<CommonRecyclerViewHolder> holderList = adapter.getHolderList();
//        for (CommonRecyclerViewHolder holder : holderList) {
//            SparseArray<View> viewSparseArray = holder.getmViews();
//            for (int i = 0; i < viewSparseArray.size(); i++) {
//                if (viewSparseArray.get(i) != null && viewSparseArray.get(i).getId() == R.id.tv_label) {
//                    System.out.println(((TextView) viewSparseArray.get(i)).getText());
//                } else if (viewSparseArray.get(i) != null && viewSparseArray.get(i).getId() == R.id.et_input)
//                {
//                    System.out.println(((EditText) viewSparseArray.get(i)).getText());
//                }
//            }
//
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            boolean isAllGranted = true;
            for (int grantResult : grantResults) {
                isAllGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
            }
            if (isAllGranted) {
                activeEngine(null);
            } else {
                Toast.makeText(this, "权限不足", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 激活引擎
     *
     * @param view
     */
    public void activeEngine(final View view) {
        if (!CommonUtil.checkPermissions(this, NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
            return;
        }
        if (view != null) {
            view.setClickable(false);
        }
        Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
            FaceEngine faceEngine = new FaceEngine();
            int activeCode = faceEngine.active(ActiveActivity.this, Constants.APP_ID, Constants.SDK_KEY);
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
                        if (activeCode == ErrorInfo.MOK) {
                            Toast.makeText(ActiveActivity.this, "激活成功", Toast.LENGTH_SHORT).show();
                        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
                            Toast.makeText(ActiveActivity.this, "已激活", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ActiveActivity.this, "激活失败:" + activeCode, Toast.LENGTH_SHORT).show();
                        }

                        if (view != null) {
                            view.setClickable(true);
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

    @Override
    public void load() {
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

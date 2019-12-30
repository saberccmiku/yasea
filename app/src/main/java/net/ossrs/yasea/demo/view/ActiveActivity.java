package net.ossrs.yasea.demo.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.google.gson.Gson;

import net.ossrs.yasea.demo.R;
import net.ossrs.yasea.demo.adapter.CommonRecyclerAdapter;
import net.ossrs.yasea.demo.adapter.CommonRecyclerViewHolder;
import net.ossrs.yasea.demo.application.IApplication;
import net.ossrs.yasea.demo.base.BaseActivity;
import net.ossrs.yasea.demo.bean.equipment.Config;
import net.ossrs.yasea.demo.bean.equipment.ConfigPattern;
import net.ossrs.yasea.demo.bean.equipment.ServerInfo;
import net.ossrs.yasea.demo.bean.equipment.ThWindowInfo;
import net.ossrs.yasea.demo.util.Constants;
import net.ossrs.yasea.demo.util.ResCode;
import net.ossrs.yasea.demo.util.permission.CommonUtil;
import net.ossrs.yasea.demo.widget.LoadingDialog;

import java.lang.reflect.Method;
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
import io.socket.client.Socket;

public class ActiveActivity extends BaseActivity {

    private List<Config> configList;
    private CommonRecyclerAdapter<Config> adapter;
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    private LoadingDialog dialog;
    private ServerInfo serverInfo;
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE
    };

    private final String TAG = ActiveActivity.class.getName();

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

    private Box<Config> netConfigBox;


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
                if (TextUtils.isEmpty(config.getTitle())) {
                    LinearLayout llItemConfig = holder.getView(R.id.ll_item_config);
                    llItemConfig.setBackground(null);
                    holder.getView(R.id.et_input).setVisibility(View.GONE);
                }
                holder.setText(R.id.tv_label, config.getLabel());
                holder.setEditText(R.id.et_input, config.getInput());
                //为EditText设置监听事件，保存修改后的属性值
                EditText editText = holder.getView(R.id.et_input);
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        config.setInput(s.toString());
                    }
                });
                //工位号手动检测按钮
                if (config.getLabel().equals(ConfigPattern.STATION)) {
                    Drawable drawable = getResources().getDrawable(R.drawable.ic_search, null);
                    drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                    editText.setCompoundDrawables(null, null, drawable, null);

                    //设置editText可点击不可编辑
                    editText.setCursorVisible(false);
                    editText.setFocusable(false);
                    editText.setFocusableInTouchMode(false);

                    editText.setOnClickListener(v -> checkCenterServerAndSelectStation());
                }
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
                    netConfigBox.put(configList);
                    Intent intent = new Intent(ActiveActivity.this, MainActivity.class);
                    intent.putParcelableArrayListExtra("config", (ArrayList<? extends Parcelable>) configList);
                    startActivity(intent);
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
            boolean isComplete = true;//判断信息是否填写完整
            for (Config config : configList) {
                if (!TextUtils.isEmpty(config.getTitle()) && TextUtils.isEmpty(config.getInput())) {
                    isComplete = false;
                }
                if (!isComplete) {
                    Toast.makeText(this, config.getLabel() + "未填写", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            //检测监控服务
            checkMonitorServer();

        }
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
                activeEngine();
            } else {
                Toast.makeText(this, "权限不足", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 激活引擎
     */
    public void activeEngine() {
        if (!CommonUtil.checkPermissions(this, NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
            return;
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
                        btnOperate.setClickable(false);
                    }

                    @Override
                    public void onNext(Integer activeCode) {
                        dialog.cancel();
                        String ivStatusText;
                        String tvEquipmentStatusText;
                        if (activeCode == ErrorInfo.MOK) {
                            ivStatus.setBackgroundResource(R.drawable.ic_success);
                            ivStatusText = ResCode.ACTIVE_SUCCESS.getMsg();
                            tvEquipmentStatusText = "设备激活成功";
                        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
                            ivStatus.setBackgroundResource(R.drawable.ic_success);
                            ivStatusText = ResCode.ACTIVE_SUCCESS.getMsg();
                            tvEquipmentStatusText = "激活成功";
                        } else {
                            ivStatus.setBackgroundResource(R.drawable.ic_failed);
                            ivStatusText = ResCode.ACTIVE_ERROR.getMsg();
                            tvEquipmentStatusText = "人脸识别引擎激活失败:" + activeCode;
                        }
                        tvStatus.setText(ivStatusText);
                        tvEquipmentStatus.setText(tvEquipmentStatusText);
                        btnOperate.setText("完成");
                        ivBack.setVisibility(View.GONE);
                        btnOperate.setClickable(true);

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        btnOperate.setClickable(true);
                    }
                });

    }

    @Override
    public void load() {
        //获取Config表
        dialog = new LoadingDialog(ActiveActivity.this, R.style.mdialog);
        dialog.show();
        netConfigBox = IApplication.boxStore.boxFor(Config.class);
        Observable.create((ObservableOnSubscribe<List<Config>>) emitter -> emitter.onNext(netConfigBox.getAll()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<Config>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(List<Config> list) {

                        if (list != null && list.size() != 0) {
                            configList.clear();
                            configList.addAll(list);
                            adapter.notifyDataSetChanged();
                        } else {
                            //加载默认数据配置
                            loadDefaultData();
                        }
                        dialog.cancel();
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
     * 加载默认数据配置
     */
    private void loadDefaultData() {

        configList.clear();
        //网络配置
        configList.add(new Config(ConfigPattern.NETWORK, 1));
        configList.add(new Config(ConfigPattern.NETWORK, ConfigPattern.SERVER, "192.168.1.25", 1));
        configList.add(new Config(ConfigPattern.NETWORK, ConfigPattern.PORT, "9099", 2));
        //监控配置
        configList.add(new Config(ConfigPattern.MONITOR, 2));
        configList.add(new Config(ConfigPattern.MONITOR, ConfigPattern.SERVER, "192.168.1.25", 1));
        configList.add(new Config(ConfigPattern.MONITOR, ConfigPattern.PORT, "80", 2));
        //本机配置
        //获取序列号
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class, String.class);
            @SuppressLint("HardwareIds") String serial = (String) (get.invoke(c, "ro.serialno", "unknown"));
            configList.add(new Config(ConfigPattern.LOCAL, 3));
            configList.add(new Config(ConfigPattern.LOCAL, ConfigPattern.SERIAL, serial, 1));
            configList.add(new Config(ConfigPattern.LOCAL, ConfigPattern.STATION, null, 2));

        } catch (Exception e) {
            e.printStackTrace();
        }

        adapter.notifyDataSetChanged();
    }

    private void checkMonitorServer() {
        dialog.show();
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            String liveIp = null;
            String livePort = null;
            for (Config config : configList) {
                if (!TextUtils.isEmpty(config.getTitle()) && config.getTitle().equals(ConfigPattern.MONITOR)) {
                    if (config.getLabel().equals(ConfigPattern.SERVER)) {
                        liveIp = config.getInput();
                    } else if (config.getLabel().equals(ConfigPattern.PORT)) {
                        livePort = config.getInput();
                    }
                }
            }
            assert livePort != null;
            emitter.onNext(net.ossrs.yasea.demo.util.CommonUtil.testServerConnect(liveIp, Integer.valueOf(livePort)));
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        btnOperate.setClickable(false);
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        if (aBoolean) {
                            //检测中央服务系统
                            checkCenterServer();
                        } else {
                            dialog.cancel();
                            btnOperate.setClickable(true);
                            Toast.makeText(ActiveActivity.this, ResCode.LIVE_SERVER_ERROR.getMsg(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        btnOperate.setClickable(true);
                    }
                });


    }


    /**
     * 检查数据解析服务系统
     */
    private void checkCenterServer() {
        dialog.show();
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            String ip = null;
            String port = "0";
            for (Config config : configList) {
                if (!TextUtils.isEmpty(config.getTitle()) && config.getTitle().equals(ConfigPattern.NETWORK)) {
                    if (config.getLabel().equals(ConfigPattern.SERVER)) {
                        ip = config.getInput();
                    } else if (config.getLabel().equals(ConfigPattern.PORT)) {
                        port = config.getInput();
                    }
                }
            }
            emitter.onNext(net.ossrs.yasea.demo.util.CommonUtil.testServerConnect(ip, Integer.valueOf(port)));
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        if (aBoolean) {
                            llConfig.setVisibility(View.GONE);
                            rlOperateStatus.setVisibility(View.VISIBLE);
                            //检查系统是否激活
                            activeEngine();
                        } else {
                            dialog.cancel();
                            btnOperate.setClickable(true);
                            Toast.makeText(ActiveActivity.this, ResCode.CENTER_SERVER_ERROR.getMsg(), Toast.LENGTH_SHORT).show();
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
    private void checkCenterServerAndSelectStation() {
        dialog.show();
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            serverInfo = new ServerInfo();
            for (Config config : configList) {
                if (!TextUtils.isEmpty(config.getTitle()) && config.getTitle().equals(ConfigPattern.NETWORK)) {
                    switch (config.getLabel()) {
                        case ConfigPattern.SERVER:
                            serverInfo.setIp(config.getInput());
                            break;
                        case ConfigPattern.PORT:
                            serverInfo.setPort(Integer.valueOf(config.getInput()));
                            break;
                        case ConfigPattern.SERIAL:
                            serverInfo.setSerial(config.getInput());
                            break;
                    }
                } else if (!TextUtils.isEmpty(config.getTitle()) && config.getTitle().equals(ConfigPattern.LOCAL)) {
                    if (ConfigPattern.SERIAL.equals(config.getLabel())) {
                        serverInfo.setSerial(config.getInput());
                    }
                }
            }
            emitter.onNext(net.ossrs.yasea.demo.util.CommonUtil.testServerConnect(serverInfo.getIp(), serverInfo.getPort()));
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        if (aBoolean) {
                            String socketUrl = "http://" + serverInfo.getIp() + ":" + serverInfo.getPort();
                            Socket socketIO = getSocketIO(socketUrl);
                            socketIO.connect();
                            Gson gson = new Gson();
                            String s = gson.toJson(serverInfo);
                            socketIO.emit("station", s);
                            socketIO.on("station", args -> {
                                Observable.create((ObservableOnSubscribe<ThWindowInfo>) emitter ->
                                        emitter.onNext(gson.fromJson(args[0].toString(), ThWindowInfo.class))
                                ).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new Observer<ThWindowInfo>() {
                                            @Override
                                            public void onSubscribe(Disposable d) {

                                            }

                                            @Override
                                            public void onNext(ThWindowInfo thWindowInfo) {
                                                dialog.cancel();
//                                                for (Config config : configList) {
//                                                    if (!TextUtils.isEmpty(config.getLabel()) && config.getLabel().equals(ConfigPattern.STATION)) {
//                                                        config.setInput(thWindowInfo.getName());
//                                                        adapter.notifyDataSetChanged();
//                                                        return;
//                                                    }
//                                                }
                                                for (int i = 0; i < configList.size(); i++) {
                                                    Config config = configList.get(i);
                                                    if (!TextUtils.isEmpty(config.getLabel()) && config.getLabel().equals(ConfigPattern.STATION)) {
                                                        config.setInput(thWindowInfo.getName());
                                                        adapter.notifyItemChanged(i);
                                                        return;
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onError(Throwable e) {

                                            }

                                            @Override
                                            public void onComplete() {

                                            }
                                        });

                            });

                        } else {
                            dialog.cancel();
                            Toast.makeText(ActiveActivity.this, ResCode.CENTER_SERVER_ERROR.getMsg(), Toast.LENGTH_SHORT).show();
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

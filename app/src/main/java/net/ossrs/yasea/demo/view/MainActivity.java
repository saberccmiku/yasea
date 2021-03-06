package net.ossrs.yasea.demo.view;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.FaceSimilar;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.VersionInfo;
import com.github.faucamp.simplertmp.RtmpHandler;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.seu.magicfilter.utils.MagicFilterType;

import net.ossrs.yasea.SrsCameraView;
import net.ossrs.yasea.SrsEncodeHandler;
import net.ossrs.yasea.SrsPublisher;
import net.ossrs.yasea.SrsRecordHandler;
import net.ossrs.yasea.demo.R;
import net.ossrs.yasea.demo.application.IApplication;
import net.ossrs.yasea.demo.base.BaseActivity;
import net.ossrs.yasea.demo.bean.DrawInfo;
import net.ossrs.yasea.demo.bean.FacePreviewInfo;
import net.ossrs.yasea.demo.bean.FaceRegisterInfo;
import net.ossrs.yasea.demo.bean.Result;
import net.ossrs.yasea.demo.bean.equipment.BaseConfig;
import net.ossrs.yasea.demo.bean.equipment.FaceFeatureInfo;
import net.ossrs.yasea.demo.bean.equipment.WindowInfo;
import net.ossrs.yasea.demo.faceserver.CompareResult;
import net.ossrs.yasea.demo.faceserver.FaceServer;
import net.ossrs.yasea.demo.util.CommonUtil;
import net.ossrs.yasea.demo.util.Constants;
import net.ossrs.yasea.demo.util.DrawHelper;
import net.ossrs.yasea.demo.util.ResCode;
import net.ossrs.yasea.demo.util.ToastUtil;
import net.ossrs.yasea.demo.util.ViewUtil;
import net.ossrs.yasea.demo.util.face.FaceHelper;
import net.ossrs.yasea.demo.util.face.FaceListener;
import net.ossrs.yasea.demo.util.face.RequestFeatureStatus;
import net.ossrs.yasea.demo.widget.FaceRectView;
import net.ossrs.yasea.demo.widget.ShowFaceInfoAdapter;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import io.objectbox.Box;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.socket.client.Socket;
import okhttp3.OkHttpClient;

public class MainActivity extends BaseActivity implements RtmpHandler.RtmpListener,
        SrsRecordHandler.SrsRecordListener, SrsEncodeHandler.SrsEncodeListener {

    private static final String TAG = "Yasea";
    private Button btnPublish;
    private Button btnSwitchCamera;
    private Button btnRecord;
    private Button btnSwitchEncoder;
    private Button btnPause;
    private String rtmpUrl;//直播服务
    private BaseConfig baseConfig;//所有配置信息
    private String recPath = Environment.getExternalStorageDirectory().getPath() + "/test.mp4";
    private SrsPublisher mPublisher;
    private SrsCameraView mCameraView;
    private static final float SIMILAR_THRESHOLD = 0.8F;
    private boolean isOnline = false;

    private FaceEngine faceEngine;
    private List<FaceInfo> faceInfoList;
    private boolean frThreadRunning = false;
    private static List<FaceRegisterInfo> faceRegisterInfoList;
    private List<CompareResult> compareResultList;
    private CompositeDisposable getFeatureDelayedDisposables = new CompositeDisposable();
    private boolean isSearchFromServerLib = false;

    /**
     * 注册人脸状态码，准备注册
     */
    private static final int REGISTER_STATUS_READY = 0;
    /**
     * 注册人脸状态码，注册中
     */
    private static final int REGISTER_STATUS_PROCESSING = 1;
    /**
     * 注册人脸状态码，注册结束（无论成功失败）
     */
    private static final int REGISTER_STATUS_DONE = 2;

    private int registerStatus = REGISTER_STATUS_DONE;

    /**
     * 当FR成功，活体未成功时，FR等待活体的时间
     */
    private static final int WAIT_LIVENESS_INTERVAL = 50;
    /**
     * 绘制人脸框的控件
     */
    private FaceRectView faceRectView;
    /**
     * 是否正在搜索人脸，保证搜索操作单线程进行
     */
    private boolean isProcessing = false;
    private FaceHelper faceHelper;
    private DrawHelper drawHelper;
    private ConcurrentHashMap<Integer, Integer> requestFeatureStatusMap = new ConcurrentHashMap<>();
    private Camera.Size previewSize;
    private ShowFaceInfoAdapter adapter;
    /**
     * 服务通信服务
     */
    private Socket socketIO;
    private Integer FACE_STATUS = 0;//0离线中1工作中2闲置中3人脸不匹配4异常
    private Integer TEMP_FACE_STATUS = -1;//用来标记上传服务器的状态，以免频繁发送请求
    private boolean IS_UPDATE_RECORD = false;//用来标记更新状态是否执行


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else {
            switch (id) {
                case R.id.cool_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.COOL);
                    break;
                case R.id.beauty_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.BEAUTY);
                    break;
                case R.id.early_bird_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.EARLYBIRD);
                    break;
                case R.id.evergreen_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.EVERGREEN);
                    break;
                case R.id.n1977_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.N1977);
                    break;
                case R.id.nostalgia_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.NOSTALGIA);
                    break;
                case R.id.romance_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.ROMANCE);
                    break;
                case R.id.sunrise_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.SUNRISE);
                    break;
                case R.id.sunset_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.SUNSET);
                    break;
                case R.id.tender_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.TENDER);
                    break;
                case R.id.toast_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.TOASTER2);
                    break;
                case R.id.valencia_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.VALENCIA);
                    break;
                case R.id.walden_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.WALDEN);
                    break;
                case R.id.warm_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.WARM);
                    break;
                case R.id.original_filter:
                default:
                    mPublisher.switchCameraFilter(MagicFilterType.NONE);
                    break;
            }
        }
        setTitle(item.getTitle());

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mPublisher.getCamera() == null) {
            //if the camera was busy and available again
            mPublisher.startCamera();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        btnPublish.setEnabled(true);
        mPublisher.resumeRecord();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPublisher.pauseRecord();
    }

    @Override
    protected void onDestroy() {
        if (mPublisher != null) {
            synchronized (mPublisher) {
                //告诉服务器直播关了
                stopAll();
                FACE_STATUS = 0;
                updateRecord(FACE_STATUS);
            }
        }

        if (getFeatureDelayedDisposables != null) {
            getFeatureDelayedDisposables.dispose();
            getFeatureDelayedDisposables.clear();
        }
        FaceServer.getInstance().unInit();

        if (socketIO != null) {
            socketIO.off();
            socketIO.disconnect();
        }
        super.onDestroy();

    }


    @Override
    public int getLayoutId() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        return R.layout.activity_main;
    }

    @Override
    public void initView() {

        //获取配置信息
        findLocalConfig();

        // response screen rotation event
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        // restore data.
        //满足摄像状态下的沉浸式，为其他布局设置一定的状态栏高度，避免和状态栏重叠
        int statusBarHeight = ViewUtil.getStatusBarHeight(this);
        tvWorkstation.setPadding(10, statusBarHeight + 10, 10, 10);
        tvOnline.setPadding(10, statusBarHeight + 10, 10, 10);

        btnPublish = (Button) findViewById(R.id.publish);
        btnSwitchCamera = (Button) findViewById(R.id.swCam);
        btnRecord = (Button) findViewById(R.id.record);
        btnSwitchEncoder = (Button) findViewById(R.id.swEnc);
        btnPause = (Button) findViewById(R.id.pause);
        btnPause.setEnabled(false);
        mCameraView = findViewById(R.id.glsurfaceview_camera);
        flVideo.setVisibility(View.GONE);
        llVideoBtn.setVisibility(View.GONE);
        mPublisher = new SrsPublisher(mCameraView);
        //     mPublisher.setFaceHandler(myHandler);
        //编码状态回调
        mPublisher.setEncodeHandler(new SrsEncodeHandler(this), myHandler);
        //rtmp推流状态回调
        mPublisher.setRtmpHandler(new RtmpHandler(this));
        mPublisher.setRecordHandler(new SrsRecordHandler(this));
        @SuppressLint("HardwareIds") String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (!TextUtils.isEmpty(androidId) && androidId.equals(Constants.Z20)) {
            //预览分辨率
//        mPublisher.setPreviewResolution(960, 540);
            mPublisher.setPreviewResolution(540, 960);
            //推流分辨率
//        mPublisher.setOutputResolution(540, 960);
            mPublisher.setOutputResolution(960, 540);
        } else {
            //预览分辨率
//        mPublisher.setPreviewResolution(960, 540);
            mPublisher.setPreviewResolution(960, 540);
            //推流分辨率
//        mPublisher.setOutputResolution(540, 960);
            mPublisher.setOutputResolution(540, 960);
        }

        //传输率
        mPublisher.setVideoHDMode();
        //开启美颜（其他滤镜效果在MagicFilterType中查看）
        mPublisher.switchCameraFilter(MagicFilterType.BEAUTY);
        //打开摄像头，开始预览（未推流）
        mPublisher.startCamera();
        List<Camera.Size> supportedPreviewSizes = mPublisher.getCamera().getParameters().getSupportedPreviewSizes();
        for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
            Log.d(TAG, "initView: height" + supportedPreviewSize.height);
            Log.d(TAG, "initView: width" + supportedPreviewSize.width);
        }
        //mPublisher.switchToSoftEncoder();//选择软编码
        mPublisher.switchToHardEncoder();//选择硬编码
        //本地人脸库初始化
        FaceServer.getInstance().init(this);
        faceRectView = findViewById(R.id.face_rect_view);
        faceInfoList = new ArrayList<>();
        faceRegisterInfoList = new ArrayList<>();
        mCameraView.setCameraCallbacksHandler(new SrsCameraView.CameraCallbacksHandler() {

            @Override
            public void onCameraParameters(Camera.Parameters params) {
                //params.setFocusMode("custom-focus");
                //params.setWhiteBalance("custom-balance");
                //etc...
            }


        });
        faceRectView = findViewById(R.id.face_rect_view);
        faceInfoList = new ArrayList<>();
        faceRegisterInfoList = new ArrayList<>();
        compareResultList = new ArrayList<>();
        RecyclerView recyclerShowFaceInfo = findViewById(R.id.recycler_view_person);
        adapter = new ShowFaceInfoAdapter(compareResultList, this);
        recyclerShowFaceInfo.setAdapter(adapter);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int spanCount = (int) (dm.widthPixels / (getResources().getDisplayMetrics().density * 100 + 0.5f));
        recyclerShowFaceInfo.setLayoutManager(new GridLayoutManager(this, spanCount));
        recyclerShowFaceInfo.setItemAnimator(new DefaultItemAnimator());

        mCameraView.setCameraCallbacksHandler(new SrsCameraView.CameraCallbacksHandler() {

            @Override
            public void onCameraParameters(Camera.Parameters params) {
                //params.setFocusMode("custom-focus");
                //params.setWhiteBalance("custom-balance");
                //etc...
            }


        });

        //1.初始化按钮监听事件
        initListener();
        activeEngine();
        initEngine();

        previewSize = mPublisher.getCamera().getParameters().getPreviewSize();
        btnPublish.setText("stop");
        //btnPublish.callOnClick();
        previewSize.width = mPublisher.getEncoder().getOutputWidth();
        previewSize.height = mPublisher.getEncoder().getOutputHeight();
        faceHelper = new FaceHelper.Builder()
                .faceEngine(faceEngine)
                .frThreadNum(MAX_DETECT_NUM)
                .previewSize(previewSize)
                .faceListener(faceListener)
                .build();

        drawHelper = getDrawHelper();
    }

    /**
     * 根据屏幕微调人脸检测框
     *
     * @return DrawHelper
     */
    private DrawHelper getDrawHelper() {
        //获取手机屏幕大小
        DisplayMetrics outMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        int widthPixels = outMetrics.widthPixels;
        int heightPixels = outMetrics.heightPixels;
        Log.i(TAG, "widthPixels = " + widthPixels + ",heightPixels = " + heightPixels);
        if (widthPixels == 1080 && heightPixels == 1920) {
            Log.i(TAG, "小米max2");
            return new DrawHelper(580, 600,
                    900, 1100,
                    0, mPublisher.getCameraId(),
                    false, false, false);
        } else if (widthPixels == 1440 && heightPixels == 2560) {
            Log.i(TAG, "华为");
            return new DrawHelper(600, 600,
                    1200, 1500,
                    0, mPublisher.getCameraId(),
                    false, false, false);
        } else if (widthPixels == 800 && heightPixels == 1232) {
            Log.i(TAG, "z20竖屏");
            return new DrawHelper(1200, 320,
                    600, 700,
                    0, mPublisher.getCameraId(),
                    false, false, false);
        } else if (widthPixels == 1280 && heightPixels == 752) {
            Log.i(TAG, "z20横屏");
            return new DrawHelper(1000, 400,
                    1200, 550,
                    0, mPublisher.getCameraId(),
                    false, false, false);
        } else {
            Log.i(TAG, "小米");
            return new DrawHelper(600, 550,
                    700, 700,
                    0, mPublisher.getCameraId(),
                    false, false, false);
        }
    }

    private void findLocalConfig() {

        Box<BaseConfig> baseConfigBox = IApplication.boxStore.boxFor(BaseConfig.class);
        List<BaseConfig> baseConfigBoxList = baseConfigBox.getAll();
        baseConfig = baseConfigBoxList.get(0);
        rtmpUrl = "rtmp://" + baseConfig.getMonitorIp() + "/hls/" + baseConfig.getLocalSerial();
        tvWorkstation.setText(baseConfig.getLocalStation());
        //链接中央服务的socket
        String socketUrl = "http://" + baseConfig.getNetworkIp() + ":" + baseConfig.getNetworkPort();
        if (socketIO == null) {
            synchronized (MainActivity.class) {
                socketIO = getSocketIO(socketUrl);
            }
        }
    }

    @BindView(R.id.ll_video_btn)
    LinearLayout llVideoBtn;

    @BindView(R.id.fl_video)
    FrameLayout flVideo;

    @BindView(R.id.tv_online)
    TextView tvOnline;

    @BindView(R.id.tv_workstation)
    TextView tvWorkstation;

    @BindView(R.id.tv_online_status)
    TextView tvOnlineStatus;

    @OnClick(R.id.tv_online_status)
    public void setOnline() {
        isOnline = !isOnline;
        Drawable topDrawable;
        if (isOnline) {
            startAll();
            if (btnSwitchEncoder.getText().toString().contentEquals("soft encoder")) {
                ToastUtil.showShort(MainActivity.this, "Use hard encoder");
            } else {
                ToastUtil.showShort(MainActivity.this, "Use soft encoder");
            }
            btnPublish.setText("stop");
            btnSwitchEncoder.setEnabled(false);
            btnPause.setEnabled(true);
            flVideo.setVisibility(View.VISIBLE);
            FACE_STATUS = 2;
            updateTvOnlineText(Constants.EMPTY);
            updateRecord(FACE_STATUS);
            topDrawable = getResources().getDrawable(R.drawable.ic_online, null);
            tvOnlineStatus.setText(Constants.OUT_ONLINE);

        } else {
            stopAll();
            btnPublish.setText("publish");
            btnRecord.setText("record");
            btnSwitchEncoder.setEnabled(true);
            btnPause.setEnabled(false);
            flVideo.setVisibility(View.GONE);
            topDrawable = getResources().getDrawable(R.drawable.ic_outline, null);
            tvOnlineStatus.setText("上线");
            //告诉服务器直播关了
            FACE_STATUS = 0;
            updateTvOnlineText(Constants.OUT_ONLINE);
            updateRecord(FACE_STATUS);
        }

        //设置上下线按钮
        topDrawable.setBounds(0, 0, topDrawable.getMinimumWidth(), topDrawable.getMinimumHeight());
        tvOnlineStatus.setCompoundDrawables(null, topDrawable, null, null);

    }

    @OnClick(R.id.tv_business)
    public void businessAction() {
        ToastUtil.showShort(MainActivity.this, "该功能还未开放");
    }

    /**
     * 将准备注册的状态置为{@link #REGISTER_STATUS_READY}
     *
     * @param view 注册按钮
     */
    @OnClick(R.id.tv_register)
    public void register(View view) {
        if (registerStatus == REGISTER_STATUS_DONE) {
            registerStatus = REGISTER_STATUS_READY;
        }
    }

    @OnClick(R.id.tv_setting)
    public void toActiveActivity() {
        stopAll();
        FACE_STATUS = 0;
        updateRecord(FACE_STATUS);
        startActivity(new Intent(this, ActiveActivity.class));
        this.finish();

    }

    private void initListener() {
        btnPublish.setOnClickListener(v -> {
            if (btnPublish.getText().toString().contentEquals("publish")) {
                startAll();

                if (btnSwitchEncoder.getText().toString().contentEquals("soft encoder")) {
                    ToastUtil.showShort(MainActivity.this, "Use hard encoder");
                } else {
                    ToastUtil.showShort(MainActivity.this, "Use soft encoder");
                }
                btnPublish.setText("stop");
                btnSwitchEncoder.setEnabled(false);
                btnPause.setEnabled(true);
            } else if (btnPublish.getText().toString().contentEquals("stop")) {
                //告诉服务器直播关了
                FACE_STATUS = 0;
                updateRecord(FACE_STATUS);
                stopAll();
                btnPublish.setText("publish");
                btnRecord.setText("record");
                btnSwitchEncoder.setEnabled(true);
                btnPause.setEnabled(false);
            }
        });
        btnPause.setOnClickListener(view -> {
            if (btnPause.getText().toString().equals("Pause")) {
                mPublisher.pausePublish();
                btnPause.setText("resume");
            } else {
                mPublisher.resumePublish();
                btnPause.setText("Pause");
            }
        });

        btnSwitchCamera.setOnClickListener(v -> mPublisher.switchCameraFace((mPublisher.getCameraId() + 1) % Camera.getNumberOfCameras()));

        btnRecord.setOnClickListener(v -> {
            if (btnRecord.getText().toString().contentEquals("record")) {
                if (mPublisher.startRecord(recPath)) {
                    btnRecord.setText("pause");
                }
            } else if (btnRecord.getText().toString().contentEquals("pause")) {
                mPublisher.pauseRecord();
                btnRecord.setText("resume");
            } else if (btnRecord.getText().toString().contentEquals("resume")) {
                mPublisher.resumeRecord();
                btnRecord.setText("pause");
            }
        });

        btnSwitchEncoder.setOnClickListener(v -> {
            if (btnSwitchEncoder.getText().toString().contentEquals("soft encoder")) {
                mPublisher.switchToSoftEncoder();
                btnSwitchEncoder.setText("hard encoder");
            } else if (btnSwitchEncoder.getText().toString().contentEquals("hard encoder")) {
                mPublisher.switchToHardEncoder();
                btnSwitchEncoder.setText("soft encoder");
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mPublisher.stopEncode();
        mPublisher.stopRecord();
        btnRecord.setText("record");
        mPublisher.setScreenOrientation(newConfig.orientation);
        if (btnPublish.getText().toString().contentEquals("stop")) {
            mPublisher.startEncode();
        }
        mPublisher.startCamera();
    }

    private static String getRandomAlphaString(int length) {
        String base = "abcdefghijklmnopqrstuvwxyz";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    private static String getRandomAlphaDigitString(int length) {
        String base = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    private void handleException(Exception e) {
        try {
            ToastUtil.showShort(MainActivity.this, e.getMessage());
            stopAll();
            btnPublish.setText("publish");
            btnRecord.setText("record");
            btnSwitchEncoder.setEnabled(true);
        } catch (Exception e1) {
            //
        }
    }

    // Implementation of SrsRtmpListener.

    @Override
    public void onRtmpConnecting(String msg) {
        ToastUtil.showShort(MainActivity.this, msg);
    }

    @Override
    public void onRtmpConnected(String msg) {
        ToastUtil.showShort(MainActivity.this, msg);
    }

    @Override
    public void onRtmpVideoStreaming() {
    }

    @Override
    public void onRtmpAudioStreaming() {
    }

    @Override
    public void onRtmpStopped() {
        ToastUtil.showShort(MainActivity.this, "Stopped");
    }

    @Override
    public void onRtmpDisconnected() {
        ToastUtil.showShort(MainActivity.this, "Disconnected");
    }

    @Override
    public void onRtmpVideoFpsChanged(double fps) {
        Log.i(TAG, String.format("Output Fps: %f", fps));
    }

    @Override
    public void onRtmpVideoBitrateChanged(double bitrate) {
        int rate = (int) bitrate;
        if (rate / 1000 > 0) {
            Log.i(TAG, String.format("Video bitrate: %f kbps", bitrate / 1000));
        } else {
            Log.i(TAG, String.format("Video bitrate: %d bps", rate));
        }
    }

    @Override
    public void onRtmpAudioBitrateChanged(double bitrate) {
        int rate = (int) bitrate;
        if (rate / 1000 > 0) {
            Log.i(TAG, String.format("Audio bitrate: %f kbps", bitrate / 1000));
        } else {
            Log.i(TAG, String.format("Audio bitrate: %d bps", rate));
        }
    }

    @Override
    public void onRtmpSocketException(SocketException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalStateException(IllegalStateException e) {
        handleException(e);
    }

    // Implementation of SrsRecordHandler.

    @Override
    public void onRecordPause() {
        ToastUtil.showShort(MainActivity.this, "Record paused");
    }

    @Override
    public void onRecordResume() {
        ToastUtil.showShort(MainActivity.this, "Record resumed");
    }

    @Override
    public void onRecordStarted(String msg) {
        ToastUtil.showShort(MainActivity.this, "Recording file: " + msg);
    }

    @Override
    public void onRecordFinished(String msg) {
        ToastUtil.showShort(MainActivity.this, "MP4 file saved: " + msg);
    }

    @Override
    public void onRecordIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRecordIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    // Implementation of SrsEncodeHandler.

    @Override
    public void onNetworkWeak() {
        ToastUtil.showShort(MainActivity.this, "Network weak");
    }

    @Override
    public void onNetworkResume() {
        ToastUtil.showShort(MainActivity.this, "Network resume");
    }

    @Override
    public void onEncodeIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }


    /**
     * 修改右上角的工作状态
     *
     * @param text 值
     */
    private void updateTvOnlineText(String text) {
        synchronized (MainActivity.class) {
            int color = R.color.exception;
            switch (text) {
                case Constants.OUT_ONLINE:
                    color = R.color.out_online;
                    break;
                case Constants.ONLINE:
                    color = R.color.online;
                    break;
                case Constants.EMPTY:
                    color = R.color.empty;
                    break;
                case Constants.FACE_NOT_FOUND:
                    color = R.color.not_fount;
                    break;
                case Constants.EXCEPTION:
                    color = R.color.exception;
                    break;
            }
            tvOnline.setTextColor(getResources().getColorStateList(color));
            tvOnline.setText(text);
        }
    }


    private final MyHandler myHandler = new MyHandler(this);

    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> weakReference;
        private MainActivity mainActivity;

        private MyHandler(MainActivity activity) {
            weakReference = new WeakReference<>(activity);
            mainActivity = weakReference.get();
        }

        @Override
        public void handleMessage(Message msg) {
            if (null != mainActivity) {
                switch (msg.what) {
                    case 1:
                        try {
                            byte[] data = (byte[]) msg.obj;  //转型
                            if (mainActivity.faceRectView != null) {
                                mainActivity.faceRectView.clearFaceInfo();
                            }
                            List<FacePreviewInfo> facePreviewInfoList = mainActivity.faceHelper.onPreviewFrame(data);
                            if (facePreviewInfoList != null && mainActivity.faceRectView != null && mainActivity.drawHelper != null) {
                                if (facePreviewInfoList.size() != 0) {
                                    List<DrawInfo> drawInfoList = new ArrayList<>();
                                    for (int i = 0; i < facePreviewInfoList.size(); i++) {
                                        String name = mainActivity.faceHelper.getName(facePreviewInfoList.get(i).getTrackId());
                                        drawInfoList.add(new DrawInfo(facePreviewInfoList.get(i).getFaceInfo().getRect(), GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE, LivenessInfo.UNKNOWN,
                                                name == null ? String.valueOf(facePreviewInfoList.get(i).getTrackId()) : name));
                                    }
                                    mainActivity.drawHelper.draw(mainActivity.faceRectView, drawInfoList);
                                } else {
                                    mainActivity.FACE_STATUS = 2;
                                    mainActivity.updateRecord(mainActivity.FACE_STATUS);
                                    mainActivity.updateTvOnlineText(Constants.EMPTY);
                                }
                            } else {
                                mainActivity.FACE_STATUS = 4;
                                mainActivity.updateRecord(mainActivity.FACE_STATUS);
                                mainActivity.updateTvOnlineText(Constants.EXCEPTION);
                            }
                            if (mainActivity.registerStatus == REGISTER_STATUS_READY && facePreviewInfoList != null && facePreviewInfoList.size() > 0) {
                                mainActivity.registerStatus = REGISTER_STATUS_PROCESSING;
                                Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
                                    boolean success = FaceServer.getInstance().register(mainActivity, data.clone(), mainActivity.previewSize.width, mainActivity.previewSize.height, "registered " + mainActivity.faceHelper.getCurrentTrackId());
                                    emitter.onNext(success);
                                })
                                        .subscribeOn(Schedulers.computation())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new Observer<Boolean>() {
                                            @Override
                                            public void onSubscribe(Disposable d) {

                                            }

                                            @Override
                                            public void onNext(Boolean success) {
                                                String result = success ? "register success!" : "register failed!";
                                                ToastUtil.showShort(mainActivity, result);
                                                mainActivity.registerStatus = REGISTER_STATUS_DONE;
                                            }

                                            @Override
                                            public void onError(Throwable e) {
                                                ToastUtil.showShort(mainActivity, "register failed!");
                                                mainActivity.registerStatus = REGISTER_STATUS_DONE;
                                            }

                                            @Override
                                            public void onComplete() {

                                            }
                                        });
                            }
                            mainActivity.clearLeftFace(facePreviewInfoList);

                            if (facePreviewInfoList != null && facePreviewInfoList.size() > 0 && mainActivity.previewSize != null) {

                                for (int i = 0; i < facePreviewInfoList.size(); i++) {
                                    if (mainActivity.livenessDetect) {
                                        mainActivity.livenessMap.put(facePreviewInfoList.get(i).getTrackId(), facePreviewInfoList.get(i).getLivenessInfo().getLiveness());
                                    }
                                    /**
                                     * 对于每个人脸，若状态为空或者为失败，则请求FR（可根据需要添加其他判断以限制FR次数），
                                     * FR回传的人脸特征结果在{@link FaceListener#onFaceFeatureInfoGet(FaceFeature, Integer)}中回传
                                     */
                                    if (mainActivity.requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId()) == null
                                            || mainActivity.requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId()) == RequestFeatureStatus.FAILED) {
                                        mainActivity.requestFeatureStatusMap.put(facePreviewInfoList.get(i).getTrackId(), RequestFeatureStatus.SEARCHING);
                                        mainActivity.faceHelper.requestFaceFeature(data, facePreviewInfoList.get(i).getFaceInfo(),
                                                mainActivity.previewSize.width, mainActivity.previewSize.height,
                                                FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(i).getTrackId());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //从远程
                        break;
                }
            }

        }
    }

    /**
     * 删除已经离开的人脸
     *
     * @param facePreviewInfoList 人脸和trackId列表
     */
    private void clearLeftFace(List<FacePreviewInfo> facePreviewInfoList) {
        Set<Integer> keySet = requestFeatureStatusMap.keySet();
        if (compareResultList != null) {
            for (int i = compareResultList.size() - 1; i >= 0; i--) {
                if (!keySet.contains(compareResultList.get(i).getTrackId())) {
                    compareResultList.remove(i);
                    adapter.notifyItemRemoved(i);
                }
            }
        }
        if (facePreviewInfoList == null || facePreviewInfoList.size() == 0) {
            requestFeatureStatusMap.clear();
            livenessMap.clear();
            return;
        }

        for (Integer integer : keySet) {
            boolean contained = false;
            for (FacePreviewInfo facePreviewInfo : facePreviewInfoList) {
                if (facePreviewInfo.getTrackId() == integer) {
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                requestFeatureStatusMap.remove(integer);
                livenessMap.remove(integer);
            }
        }

    }

    /**
     * 上传检测状态到服务器
     * 坐席状态（0离线1在线2人脸不匹配3异常）
     */
    public void updateRecord(int status) {
        if (!IS_UPDATE_RECORD) {
            //避免频繁发送同一状态给服务器，减少压力
            if (status != TEMP_FACE_STATUS) {
                IS_UPDATE_RECORD = true;//锁定线程
                if (CommonUtil.isNetworkAvailable(this)) {
                    Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
                        emitter.onNext(CommonUtil.testServerConnect(baseConfig.getNetworkIp(), Integer.valueOf(baseConfig.getNetworkPort())));
                    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Boolean>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(Boolean aBoolean) {
                            if (aBoolean) {
                                WindowInfo windowInfo = new WindowInfo();
                                windowInfo.setDevCode(baseConfig.getLocalSerial());
                                windowInfo.setAddress(rtmpUrl);
                                windowInfo.setStatus(status);
                                Gson gson = new Gson();
                                String windowInfoJson = gson.toJson(windowInfo);
                                socketIO.emit("updateRecord", windowInfoJson);
                                socketIO.on("updateRecord", args -> {
                                    IS_UPDATE_RECORD = false;
                                    TEMP_FACE_STATUS = status;
                                });

                            } else {
                                IS_UPDATE_RECORD = false;
                                TEMP_FACE_STATUS = status;
                                //Toast.makeText(MainActivity.this, ResCode.CENTER_SERVER_ERROR.getMsg(), Toast.LENGTH_SHORT).show();
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
                    ToastUtil.showShort(this, ResCode.NETWORK_ERROR.getMsg());
                }
            }
        }
    }


    private void searchFace(final FaceFeature frFace, final Integer requestId) {
        Observable
                .create((ObservableOnSubscribe<CompareResult>) emitter -> {
                    Log.i(TAG, "subscribe: fr search start = " + System.currentTimeMillis() + " trackId = " + requestId);
                    CompareResult compareResult = FaceServer.getInstance().getTopOfFaceLib(frFace);
                    Log.i(TAG, "subscribe: fr search end = " + System.currentTimeMillis() + " trackId = " + requestId);
                    if (compareResult == null) {
                        //从远程服务获取特征信息
                        if (!isSearchFromServerLib) {
                            //标注查询结束
                            isSearchFromServerLib = true;
                            searchFromServerLib(frFace, requestId);
                        }
                    } else {
                        emitter.onNext(compareResult);
                    }
                })
                .subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<CompareResult>() {

                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(CompareResult compareResult) {
                        if (compareResult == null || compareResult.getUserName() == null) {
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                            faceHelper.addName(requestId, Constants.FACE_NOT_FOUND);
                            FACE_STATUS = 3;
                            updateRecord(FACE_STATUS);
                            return;
                        }

//                        Log.i(TAG, "onNext: fr search get result  = " + System.currentTimeMillis() + " trackId = " + requestId + "  similar = " + compareResult.getSimilar());
                        if (compareResult.getSimilar() > SIMILAR_THRESHOLD) {
                            boolean isAdded = false;
                            if (compareResultList == null) {
                                requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                                faceHelper.addName(requestId, Constants.FACE_NOT_FOUND);
                                FACE_STATUS = 3;
                                updateRecord(FACE_STATUS);
                                updateTvOnlineText(Constants.FACE_NOT_FOUND);
                                return;
                            }
                            for (CompareResult compareResult1 : compareResultList) {
                                if (compareResult1.getTrackId() == requestId) {
                                    isAdded = true;
                                    break;
                                }
                            }
                            if (!isAdded) {
                                //对于多人脸搜索，假如最大显示数量为 MAX_DETECT_NUM 且有新的人脸进入，则以队列的形式移除
                                if (compareResultList.size() >= MAX_DETECT_NUM) {
                                    compareResultList.remove(0);
                                    adapter.notifyItemRemoved(0);
                                }
                                //添加显示人员时，保存其trackId
                                FACE_STATUS = 1;
                                updateRecord(FACE_STATUS);
                                updateTvOnlineText(Constants.ONLINE);
                                compareResult.setTrackId(requestId);
                                compareResultList.add(compareResult);
                                adapter.notifyItemInserted(compareResultList.size() - 1);
                            }
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.SUCCEED);
                            faceHelper.addName(requestId, compareResult.getUserName());

                        } else {
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                            faceHelper.addName(requestId, Constants.FACE_NOT_FOUND);
                            FACE_STATUS = 3;
                            updateRecord(FACE_STATUS);
                            updateTvOnlineText(Constants.FACE_NOT_FOUND);
                        }

                    }

                    @Override
                    public void onError(Throwable e) {
                        //updateTvOnlineText("直播中/离线(" + e.getMessage() + ")");
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    /**
     * 在特征库中搜索
     *
     * @param faceFeature 传入特征数据
     * @return 比对结果
     */
    public void searchFromServerLib(FaceFeature faceFeature, Integer requestId) {
        //本地库中不存在该人脸，请求服务器进行比对，比对成功后将人脸信息返回到本地存储
        String faceFeatureCode = Base64.encodeToString(faceFeature.getFeatureData(), Base64.DEFAULT);
        FaceFeatureInfo faceFeatureInfo = new FaceFeatureInfo(101, faceFeatureCode, baseConfig.getLocalWindowId());
        Gson gson = new Gson();
        String faceFeatureInfoJson = gson.toJson(faceFeatureInfo);
        socketIO.emit("searchFromServerLib", faceFeatureInfoJson);
        socketIO.on("searchFromServerLib", args -> Observable
                .create((ObservableOnSubscribe<Result<FaceFeatureInfo>>) emitter -> {
                    Type type = new TypeToken<Result<FaceFeatureInfo>>() {
                    }.getType();
                    Result<FaceFeatureInfo> result = gson.fromJson(args[0].toString(), type);
                    emitter.onNext(result);
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Result<FaceFeatureInfo>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Result<FaceFeatureInfo> faceFeatureInfoResult) {
                        //标注查询结束
                        isSearchFromServerLib = false;
                        if (faceFeatureInfoResult.isSuccess()) {
                            updateTvOnlineText(Constants.ONLINE);
                            FACE_STATUS = 1;
                            updateRecord(FACE_STATUS);
                            FaceFeatureInfo temp = faceFeatureInfoResult.getData();
                            CompareResult compareResult = new CompareResult(temp.getName(), temp.getSimilarValue(), temp.getImgUrl());
                            compareResult.setTrackId(requestId);
                            compareResultList.clear();
                            adapter.notifyDataSetChanged();
                            compareResultList.add(compareResult);
                            adapter.notifyItemInserted(compareResultList.size() - 1);

                        } else {
                            if (faceFeatureInfoResult.getMessage() != null && faceFeatureInfoResult.getMessage().equals("人脸不匹配")) {
                                FACE_STATUS = 3;
                            } else {
                                FACE_STATUS = 4;
                            }
                            updateTvOnlineText("直播中/" + faceFeatureInfoResult.getMessage());
                            updateRecord(FACE_STATUS);
                        }

                    }

                    @Override
                    public void onError(Throwable e) {
                        //标注查询结束
                        isSearchFromServerLib = false;
                    }

                    @Override
                    public void onComplete() {
                        //标注查询结束
                        isSearchFromServerLib = false;
                    }
                }));
    }

    /**
     * 激活引擎
     */
    private void activeEngine() {
        faceEngine = new FaceEngine();
        faceEngine.active(getApplicationContext(), Constants.APP_ID, Constants.SDK_KEY);
    }

    /**
     * 初始化引擎
     */
    private void initEngine() {
        faceEngine.init(this, FaceEngine.ASF_DETECT_MODE_VIDEO, FaceEngine.ASF_OP_0_ONLY,
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_RECOGNITION | FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_AGE | FaceEngine.ASF_FACE3DANGLE | FaceEngine.ASF_GENDER | FaceEngine.ASF_LIVENESS);
        VersionInfo versionInfo = new VersionInfo();
        faceEngine.getVersion(versionInfo);
        Log.i(TAG, "initEngine:  init: " + afCode + "  version:" + versionInfo);
    }

    /**
     * 提取特征值 format FaceEngine.CP_PAF_NV21
     */
    private void extractFaceFeature(byte[] nv21, int format, FaceInfo faceInfo, FaceFeature faceFeature) {
        int frCode = faceEngine.extractFaceFeature(nv21, mPublisher.getEncoder().getOutputWidth(), mPublisher.getEncoder().getOutputHeight(), format, faceInfo, faceFeature);
    }

    /**
     * 比较特征值
     */
    private float compareFaceFeature(FaceFeature sourceFaceFeature, FaceFeature targetFaceFeature, FaceSimilar faceSimilar) {
        faceEngine.compareFaceFeature(sourceFaceFeature, targetFaceFeature, faceSimilar);
        return faceSimilar.getScore();
    }

    /**
     * I420转nv21
     *
     * @param data
     * @param width
     * @param height
     * @return
     */
    public static byte[] I420Tonv21(byte[] data, int width, int height) {
        byte[] output = new byte[data.length];
        int frameSize = width * height;
        int qFrameSize = frameSize / 4;
        int tempFrameSize = frameSize * 5 / 4;
        System.arraycopy(data, 0, output, 0, frameSize);
        for (int i = 0; i < qFrameSize; ++i) {
            output[frameSize + i * 2] = data[tempFrameSize + i];
            output[frameSize + i * 2 + 1] = data[frameSize + i];
        }
        return output;
    }

    public static byte[] nv12ToNv21(byte[] nv21, int width, int height) {
        if (nv21 == null || nv21.length == 0 || width * height * 3 / 2 != nv21.length) {
            throw new IllegalArgumentException("invalid image params!");
        }
        final int ySize = width * height;
        int totalSize = width * height * 3 / 2;

        byte[] nv12 = new byte[nv21.length];
//复制Y
        System.arraycopy(nv21, 0, nv12, 0, ySize);
//UV互换
        for (int uvIndex = ySize; uvIndex < totalSize; uvIndex += 2) {
            nv12[uvIndex] = nv21[uvIndex + 1];
            nv12[uvIndex + 1] = nv21[uvIndex];
        }
        return nv12;
    }

    private int afCode = -1;
    private static final int MAX_DETECT_NUM = 10;
    private OkHttpClient mHttpClient = null;
    /**
     * 活体检测的开关
     */
    private boolean livenessDetect = true;

    private ConcurrentHashMap<Integer, Integer> livenessMap = new ConcurrentHashMap<>();

    final FaceListener faceListener = new FaceListener() {
        @Override
        public void onFail(Exception e) {
            Log.e(TAG, "onFail: " + e.getMessage());
        }

        @Override
        public void onFaceFeatureInfoGet(@Nullable FaceFeature faceFeature, Integer requestId) {

            //FR成功
            if (faceFeature != null) {
//                    Log.i(TAG, "onPreview: fr end = " + System.currentTimeMillis() + " trackId = " + requestId);

                //不做活体检测的情况，直接搜索
                if (!livenessDetect) {
                    searchFace(faceFeature, requestId);
                }
                //活体检测通过，搜索特征
                else if (livenessMap.get(requestId) != null && livenessMap.get(requestId) == LivenessInfo.ALIVE) {
                    searchFace(faceFeature, requestId);
                }
                //活体检测未出结果，延迟100ms再执行该函数
                else if (livenessMap.get(requestId) != null && livenessMap.get(requestId) == LivenessInfo.UNKNOWN) {
                    getFeatureDelayedDisposables.add(Observable.timer(WAIT_LIVENESS_INTERVAL, TimeUnit.MILLISECONDS)
                            .subscribe(aLong -> onFaceFeatureInfoGet(faceFeature, requestId)));
                }
                //活体检测失败
                else {
                    requestFeatureStatusMap.put(requestId, RequestFeatureStatus.NOT_ALIVE);
                }

            }
            //FR 失败
            else {
                requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
            }
        }

        //请求FR的回调
        @Override
        public void onFaceFeatureInfoGet(final byte[] nv21, @Nullable final FaceFeature faceFeature, final Integer requestId) {


        }
    };

    private void stopAll() {
        mPublisher.stopPublish();
        mPublisher.stopRecord();
        mPublisher.stopCamera();
        //faceHelper中可能会有FR耗时操作仍在执行，加锁防止crash
        //unInitEngine();
    }

    private void startAll() {
        mPublisher.startPublish(rtmpUrl);
        mPublisher.startCamera();
        //activeEngine();
        //initEngine();
    }
}

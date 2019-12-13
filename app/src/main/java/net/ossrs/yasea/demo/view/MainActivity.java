package net.ossrs.yasea.demo.view;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.FaceSimilar;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.VersionInfo;
import com.github.faucamp.simplertmp.RtmpHandler;
import com.seu.magicfilter.utils.MagicFilterType;

import net.ossrs.yasea.SrsCameraView;
import net.ossrs.yasea.SrsEncodeHandler;
import net.ossrs.yasea.SrsPublisher;
import net.ossrs.yasea.SrsRecordHandler;
import net.ossrs.yasea.demo.R;
import net.ossrs.yasea.demo.adapter.CommonRecyclerAdapter;
import net.ossrs.yasea.demo.base.BaseActivity;
import net.ossrs.yasea.demo.bean.DrawInfo;
import net.ossrs.yasea.demo.bean.FacePreviewInfo;
import net.ossrs.yasea.demo.bean.FaceRegisterInfo;
import net.ossrs.yasea.demo.faceserver.CompareResult;
import net.ossrs.yasea.demo.faceserver.FaceServer;
import net.ossrs.yasea.demo.net.ApiConstants;
import net.ossrs.yasea.demo.util.ConfigUtil;
import net.ossrs.yasea.demo.util.Constants;
import net.ossrs.yasea.demo.util.DrawHelper;
import net.ossrs.yasea.demo.util.face.FaceHelper;
import net.ossrs.yasea.demo.util.face.FaceListener;
import net.ossrs.yasea.demo.util.face.RequestFeatureStatus;
import net.ossrs.yasea.demo.widget.FaceRectView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends BaseActivity implements RtmpHandler.RtmpListener,
        SrsRecordHandler.SrsRecordListener, SrsEncodeHandler.SrsEncodeListener, View.OnClickListener {

    private static final String TAG = MainActivity.class.getName();

    //是否在线
    private boolean isOnLine;

    private Button btnPublish;
    private Button btnSwitchCamera;
    private Button btnRecord;
    private Button btnSwitchEncoder;
    private Button btnPause;

    private SharedPreferences sp;
    private String rtmpUrl = ApiConstants.rtmpUrl;
    private String recPath = Environment.getExternalStorageDirectory().getPath() + "/test.mp4";
    private SrsPublisher mPublisher;
    private SrsCameraView mCameraView;
    private static final float SIMILAR_THRESHOLD = 0.8F;

    private FaceEngine faceEngine;
//    private String appId = "H8QuDe8V8fg6oSQjCdwA8XBhGBJ2qiew4myUhPAhvY1d";
//    private String sdkKey = "B81vkewSmkXGXASVdfwCpX9MzquuiiTp4jK93tibGiLi";

    private List<FaceInfo> faceInfoList;

    private boolean frThreadRunning = false;
    private ExecutorService executor = Executors.newSingleThreadExecutor();


    private static List<FaceRegisterInfo> faceRegisterInfoList;


    private List<CompareResult> compareResultList;

    private SurfaceView surfaceView;

    private CompositeDisposable getFeatureDelayedDisposables = new CompositeDisposable();
    /**
     * 当FR成功，活体未成功时，FR等待活体的时间
     */
    private static final int WAIT_LIVENESS_INTERVAL = 50;
    /**
     * 绘制人脸框的控件
     */
    public FaceRectView faceRectView;
    /**
     * 是否正在搜索人脸，保证搜索操作单线程进行
     */
    private boolean isProcessing = false;
    private FaceHelper faceHelper;
    private DrawHelper drawHelper;
    private ConcurrentHashMap<Integer, Integer> requestFeatureStatusMap = new ConcurrentHashMap<>();
    private Camera.Size previewSize;

    @BindView(R.id.fl_video)
    FrameLayout flVideo;


    @Override
    public int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    public void initView() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // response screen rotation event
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        // restore data.
        sp = getSharedPreferences(Constants.SHARED_PREFERENCES, MODE_PRIVATE);
        rtmpUrl = sp.getString("rtmpUrl", rtmpUrl);
        //获取布局控件对象
        findView();

    }

    private void setMonitor() {
        //初始化播放控件
        initCamera();
        //初始化人脸识别引擎
        initEngine();
        //设置录播界面控件
        initCameraWidget();

    }

    @BindView(R.id.tv_online)
    TextView tvOnline;

    @OnClick(R.id.fab_online)
    public void setOnLine() {
        isOnLine = !isOnLine;
        if (!isOnLine) {
            tvOnline.setText("离线中");
            flVideo.setVisibility(View.GONE);
            if (mPublisher != null) {
                mPublisher.stopPublish();
                mPublisher.stopRecord();
                mPublisher.stopCamera();
            }
        } else {
            tvOnline.setText("工作中");
            flVideo.setVisibility(View.VISIBLE);
            //设置监控
            setMonitor();
        }
    }

    private void initCameraWidget() {

        previewSize = mCameraView.getCamera().getParameters().getPreviewSize();
        btnPublish.setText("发布");
        btnPublish.callOnClick();
        previewSize.width = mPublisher.getEncoder().getOutputWidth();
        previewSize.height = mPublisher.getEncoder().getOutputHeight();

        drawHelper = new DrawHelper(this.previewSize.width, this.previewSize.height, mCameraView.getmPreviewWidth(), mCameraView.getmPreviewHeight(), 0
                , mPublisher.getCameraId(), false);

        faceHelper = new FaceHelper.Builder()
                .faceEngine(faceEngine)
                .frThreadNum(MAX_DETECT_NUM)
                .previewSize(previewSize)
                .faceListener(faceListener)
                .currentTrackId(ConfigUtil.getTrackId(MainActivity.this.getApplicationContext()))
                .build();
    }


    private void initCamera() {

        mPublisher = new SrsPublisher(mCameraView);
        mPublisher.setEncodeHandler(new SrsEncodeHandler(this), myHandler);
        mPublisher.setRtmpHandler(new RtmpHandler(this));
        mPublisher.setRecordHandler(new SrsRecordHandler(this));
        mPublisher.setPreviewResolution(960, 540);
        mPublisher.setOutputResolution(960, 540);
        mPublisher.setVideoHDMode();
        mPublisher.startCamera();
        faceRectView = findViewById(R.id.face_rect_view);
        faceInfoList = new ArrayList<>();
        faceRegisterInfoList = new ArrayList<>();
        compareResultList = new ArrayList<>();
        mCameraView.setCameraCallbacksHandler(new SrsCameraView.CameraCallbacksHandler() {

            @Override
            public void onCameraParameters(Camera.Parameters params) {
                //params.setFocusMode("custom-focus");
                //params.setWhiteBalance("custom-balance");
                //etc...
            }


        });
    }

    private void findView() {
        flVideo.setVisibility(View.GONE);
        // initialize url.
        btnPublish = findViewById(R.id.publish);
        btnSwitchCamera = findViewById(R.id.swCam);
        btnRecord = findViewById(R.id.record);
        btnSwitchEncoder = findViewById(R.id.swEnc);
        btnPause = findViewById(R.id.pause);
        btnPause.setEnabled(false);
        mCameraView = findViewById(R.id.glsurfaceview_camera);


        //绑定点击事件
        btnPublish.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnSwitchCamera.setOnClickListener(this);
        btnRecord.setOnClickListener(this);
        btnSwitchEncoder.setOnClickListener(this);
    }

    private CommonRecyclerAdapter mAdapter;


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
        if (isOnLine && mPublisher.getCamera() == null) {
            //if the camera was busy and available again
            mPublisher.startCamera();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isOnLine) {
            final Button btn = (Button) findViewById(R.id.publish);
            btn.setEnabled(true);
            mPublisher.resumeRecord();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isOnLine) {
            mPublisher.pauseRecord();
        }
    }

    @Override
    protected void onDestroy() {
        //人脸识别销毁
        //faceHelper中可能会有FR耗时操作仍在执行，加锁防止crash
        if (faceHelper != null) {
            synchronized (faceHelper) {
                unInitEngine();
            }
            ConfigUtil.setTrackId(this, faceHelper.getCurrentTrackId());
            faceHelper.release();
        } else {
            unInitEngine();
        }
        if (getFeatureDelayedDisposables != null) {
            getFeatureDelayedDisposables.dispose();
            getFeatureDelayedDisposables.clear();
        }

        //视屏销毁

        if (isOnLine) {
            mPublisher.stopPublish();
            mPublisher.stopRecord();
        }
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mPublisher.stopEncode();
        mPublisher.stopRecord();
        btnRecord.setText("录制");
        mPublisher.setScreenOrientation(newConfig.orientation);
        if (btnPublish.getText().toString().contentEquals("停止")) {
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
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            mPublisher.stopPublish();
            mPublisher.stopRecord();
            btnPublish.setText("发布");
            btnRecord.setText("录制");
            btnSwitchEncoder.setEnabled(true);
        } catch (Exception e1) {
            //
        }
    }

    // Implementation of SrsRtmpListener.

    @Override
    public void onRtmpConnecting(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpConnected(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpVideoStreaming() {
    }

    @Override
    public void onRtmpAudioStreaming() {
    }

    @Override
    public void onRtmpStopped() {
        Toast.makeText(getApplicationContext(), "Stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpDisconnected() {
        Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(getApplicationContext(), "Record paused", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordResume() {
        Toast.makeText(getApplicationContext(), "Record resumed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordStarted(String msg) {
        Toast.makeText(getApplicationContext(), "Recording file: " + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordFinished(String msg) {
        Toast.makeText(getApplicationContext(), "MP4 file saved: " + msg, Toast.LENGTH_SHORT).show();
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
        Toast.makeText(getApplicationContext(), "Network weak", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNetworkResume() {
        Toast.makeText(getApplicationContext(), "Network resume", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEncodeIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }


    private void drawPreviewInfo(List<FacePreviewInfo> facePreviewInfoList) {
        if (facePreviewInfoList != null && faceRectView != null && drawHelper != null) {
            List<DrawInfo> drawInfoList = new ArrayList<>();
            for (int i = 0; i < facePreviewInfoList.size(); i++) {
                String name = faceHelper.getName(facePreviewInfoList.get(i).getTrackId());
                drawInfoList.add(new DrawInfo(facePreviewInfoList.get(i).getFaceInfo().getRect(), GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE, LivenessInfo.UNKNOWN,
                        name == null ? String.valueOf(facePreviewInfoList.get(i).getTrackId()) : name));
            }
            drawHelper.draw(faceRectView, drawInfoList);
        }
    }


    private final MyHandler myHandler = new MyHandler(this);

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.publish:
                if (btnPublish.getText().toString().contentEquals("发布")) {
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("rtmpUrl", rtmpUrl);
                    editor.apply();

                    mPublisher.startPublish(rtmpUrl);
                    mPublisher.startCamera();

                    if (btnSwitchEncoder.getText().toString().contentEquals("软编码")) {
                        Toast.makeText(getApplicationContext(), "Use hard encoder", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Use soft encoder", Toast.LENGTH_SHORT).show();
                    }
                    btnPublish.setText("停止");
                    btnSwitchEncoder.setEnabled(false);
                    btnPause.setEnabled(true);
                } else if (btnPublish.getText().toString().contentEquals("停止")) {
                    mPublisher.stopPublish();
                    mPublisher.stopRecord();
                    btnPublish.setText("发布");
                    btnRecord.setText("录制");
                    btnSwitchEncoder.setEnabled(true);
                    btnPause.setEnabled(false);
                }
                break;
            case R.id.pause:
                if (btnPause.getText().toString().equals("暂停")) {
                    mPublisher.pausePublish();
                    btnPause.setText("重启");
                } else {
                    mPublisher.resumePublish();
                    btnPause.setText("暂停");
                }
                break;
            case R.id.swCam:
                mPublisher.switchCameraFace((mPublisher.getCameraId() + 1) % Camera.getNumberOfCameras());
                break;
            case R.id.record:
                if (btnRecord.getText().toString().contentEquals("录制")) {
                    if (mPublisher.startRecord(recPath)) {
                        btnRecord.setText("暂停");
                    }
                } else if (btnRecord.getText().toString().contentEquals("暂停")) {
                    mPublisher.pauseRecord();
                    btnRecord.setText("重启");
                } else if (btnRecord.getText().toString().contentEquals("重启")) {
                    mPublisher.resumeRecord();
                    btnRecord.setText("暂停");
                }
                break;
            case R.id.swEnc:
                if (btnSwitchEncoder.getText().toString().contentEquals("软编码")) {
                    mPublisher.switchToSoftEncoder();
                    btnSwitchEncoder.setText("硬编码");
                } else if (btnSwitchEncoder.getText().toString().contentEquals("硬编码")) {
                    mPublisher.switchToHardEncoder();
                    btnSwitchEncoder.setText("软编码");
                }
                break;
            default:
                break;
        }
    }

    private static class MyHandler extends Handler {
        private MainActivity activity;

        public MyHandler(MainActivity activity) {
            WeakReference<MainActivity> mActivity = new WeakReference<>(activity);
            activity = mActivity.get();
        }

        @Override
        public void handleMessage(Message msg) {
            if (null != activity) {
                switch (msg.what) {
                    case 1:
                        try {
                            byte[] data = (byte[]) msg.obj;  //转型
                           /* YuvImage image = new YuvImage(data, ImageFormat.NV21, mPublisher.getEncoder().getOutputWidth(),mPublisher.getEncoder().getOutputHeight(), null);            //ImageFormat.NV21  640 480

                            File file = new File(Environment.getExternalStorageDirectory(),"123.jpg");


                            try {
                                FileOutputStream fileOutputStream = new FileOutputStream(file);
                                image.compressToJpeg(new Rect(0, 0, mPublisher.getEncoder().getOutputWidth(), mPublisher.getEncoder().getOutputHeight()), 100, fileOutputStream); // 将NV21格式图片，以质量70压缩成Jpeg，并得到JPEG数据流
                                fileOutputStream.close();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }*/
                            if (activity.faceRectView != null) {
                                activity.faceRectView.clearFaceInfo();
                            }
                            List<FacePreviewInfo> facePreviewInfoList = activity.faceHelper.onPreviewFrame(data);
                            if (facePreviewInfoList != null && facePreviewInfoList.size() > 0 && activity.faceRectView != null && activity.drawHelper != null) {
                                activity.drawPreviewInfo(facePreviewInfoList);

                            } else {
                                if (activity.compareResultList.size() > 0) {
                                    activity.compareResultList.clear();
                                    activity.mAdapter.notifyDataSetChanged();
                                }

                            }
                            if (facePreviewInfoList != null && facePreviewInfoList.size() > 0 && activity.previewSize != null) {
                                for (int i = 0; i < facePreviewInfoList.size(); i++) {
                                    if (activity.livenessDetect) {
                                        activity.livenessMap.put(facePreviewInfoList.get(i).getTrackId(), facePreviewInfoList.get(i).getLivenessInfo().getLiveness());
                                    }
                                    /**
                                     * 对于每个人脸，若状态为空或者为失败，则请求FR（可根据需要添加其他判断以限制FR次数），
                                     * FR回传的人脸特征结果在{@link FaceListener#onFaceFeatureInfoGet(FaceFeature, Integer)}中回传
                                     */
                                    if (activity.requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId()) == null
                                            || activity.requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId()) == RequestFeatureStatus.FAILED) {
                                        activity.requestFeatureStatusMap.put(facePreviewInfoList.get(i).getTrackId(), RequestFeatureStatus.SEARCHING);
                                        activity.faceHelper.requestFaceFeature(data, facePreviewInfoList.get(i).getFaceInfo(), activity.previewSize.width, activity.previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(i).getTrackId());
//                            Log.i(TAG, "onPreview: fr start = " + System.currentTimeMillis() + " trackId = " + facePreviewInfoList.get(i).getTrackId());
                                    }
                                }
                            }
                            //创建文件对象 通过cache目录
                            //  YuvImage image = new YuvImage(data, ImageFormat.NV21,mPublisher.getEncoder().getOutputWidth(),mPublisher.getEncoder().getOutputHeight(),null);
                        /*    faceInfoList.clear();
                            detectFaces(data,FaceEngine.CP_PAF_NV21,faceInfoList);
                            if(faceInfoList.size()>0){
                                FaceFeature faceFeature = new FaceFeature();
                                faceRecognizeRunnables.add(new FaceRecognizeRunnable(data, faceInfoList.get(0), mPublisher.getEncoder().getOutputWidth(), mPublisher.getEncoder().getOutputHeight(), FaceEngine.CP_PAF_NV21));
                                executor.execute(faceRecognizeRunnables.poll());
                            }else{
                                compareResultList.clear();
                                mAdapter.notifyDataSetChanged();
                            }*/
                            // fos.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //从远程
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                }
            }

        }
    }

    private void searchFace(final FaceFeature frFace, final Integer requestId) {
        Observable
                .create(new ObservableOnSubscribe<CompareResult>() {
                    @Override
                    public void subscribe(ObservableEmitter<CompareResult> emitter) {
//                        Log.i(TAG, "subscribe: fr search start = " + System.currentTimeMillis() + " trackId = " + requestId);
                        CompareResult compareResult = FaceServer.getInstance().getTopOfFaceLib(frFace);
//                        Log.i(TAG, "subscribe: fr search end = " + System.currentTimeMillis() + " trackId = " + requestId);
                        if (compareResult == null) {
                            emitter.onError(null);
                        } else {
                            emitter.onNext(compareResult);
                        }
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
                            faceHelper.addName(requestId, "VISITOR " + requestId);
                            return;
                        }

//                        Log.i(TAG, "onNext: fr search get result  = " + System.currentTimeMillis() + " trackId = " + requestId + "  similar = " + compareResult.getSimilar());
                        if (compareResult.getSimilar() > SIMILAR_THRESHOLD) {
                            boolean isAdded = false;
                            if (compareResultList == null) {
                                requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                                faceHelper.addName(requestId, "VISITOR " + requestId);
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
                                    //adapter.notifyItemRemoved(0);
                                }
                                //添加显示人员时，保存其trackId
                                compareResult.setTrackId(requestId);
                                compareResultList.add(compareResult);
                                // adapter.notifyItemInserted(compareResultList.size() - 1);
                            }
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.SUCCEED);
                            faceHelper.addName(requestId, compareResult.getUserName());

                        } else {
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                            faceHelper.addName(requestId, "VISITOR " + requestId);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
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
    public CompareResult getTopOfFaceLib(byte[] nv21, FaceFeature faceFeature) {
        if (faceEngine == null || isProcessing || faceFeature == null) {
            return null;
        }
        FaceFeature tempFaceFeature = new FaceFeature();
        FaceSimilar faceSimilar = new FaceSimilar();
        float maxSimilar = 0;
        int maxSimilarIndex = -1;
        isProcessing = true;
        if (faceRegisterInfoList != null && faceRegisterInfoList.size() > 0) {
            for (int i = 0; i < faceRegisterInfoList.size(); i++) {
                tempFaceFeature.setFeatureData(faceRegisterInfoList.get(i).getFeatureData());
                faceEngine.compareFaceFeature(faceFeature, tempFaceFeature, faceSimilar);
                if (faceSimilar.getScore() > maxSimilar) {
                    maxSimilar = faceSimilar.getScore();
                    maxSimilarIndex = i;
                }
            }
        }

        isProcessing = false;
        if (maxSimilarIndex != -1) {
            return new CompareResult(faceRegisterInfoList.get(maxSimilarIndex).getName(), maxSimilar);
        } else {
            //本地库中不存在该人脸，请求服务器进行比对，比对成功后将人脸信息返回到本地存储
            String faceFeatureCode = Base64.encodeToString(faceFeature.getFeatureData(), Base64.DEFAULT);
            String imageCode = Base64.encodeToString(nv21, Base64.DEFAULT);
            JSONObject json = new JSONObject();
            try {
                json.put("faceFeatureCode", faceFeatureCode);
                json.put("imageCode", imageCode);
                json.put("groupId", 101);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(JSON, json.toString());
            initHttpClient();
            JSONObject jsonObject = synRequest(body);
            try {
                if ("15".equals(jsonObject.getString("code"))) {

                } else if ("0".equals(jsonObject.getString("code"))) {
                    JSONObject data = jsonObject.getJSONObject("data");
                    FaceRegisterInfo faceRegisterInfo = new FaceRegisterInfo(faceFeature.getFeatureData(), data.getString("name"));
                    //    if(faceRegisterInfoList==null){
                    //        faceRegisterInfoList = new ArrayList<>();
                    //   }
                    //   faceRegisterInfoList.add(faceRegisterInfo);
                    return new CompareResult(data.getString("name"), data.getInt("similarValue"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void initHttpClient() {
        if (null == mHttpClient) {
            mHttpClient = new OkHttpClient.Builder()
                    .readTimeout(5, TimeUnit.SECONDS)//设置读超时
                    .writeTimeout(5, TimeUnit.SECONDS)////设置写超时
                    .connectTimeout(15, TimeUnit.SECONDS)//设置连接超时
                    .retryOnConnectionFailure(true)//是否自动重连
                    .build();
        }
    }

    private JSONObject synRequest(RequestBody requestBody) {
        Request request = new Request.Builder()
                .url("http://192.168.1.25:9089/faceSearch1")
                .post(requestBody)
                .build();
        Call call = mHttpClient.newCall(request);
        JSONObject json = new JSONObject();
        try {
            Response response = call.execute();
            String responseStr = response.body().string();
            json = new JSONObject(responseStr);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    private void asyRequest(RequestBody requestBody) {
        final Request request = new Request.Builder()
                .url("http://192.168.1.25:9089/faceSearch1")
                .post(requestBody)
                .build();
        Call call = mHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseStr = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(responseStr);
                    if ("15".equals(jsonObject.getString("code"))) {

                    } else if ("200".equals(jsonObject.getString("code"))) {
                        JSONObject data = jsonObject.getJSONObject("data");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    /**
     * 初始化引擎
     */
    private void initEngine() {
        faceEngine = new FaceEngine();
        afCode = faceEngine.init(this, FaceEngine.ASF_DETECT_MODE_VIDEO, ConfigUtil.getFtOrient(this),
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_RECOGNITION | FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_LIVENESS);
        VersionInfo versionInfo = new VersionInfo();
        faceEngine.getVersion(versionInfo);
        Log.i(TAG, "initEngine:  init: " + afCode + "  version:" + versionInfo);

        if (afCode != ErrorInfo.MOK) {
            Toast.makeText(this, "初始化引擎" + afCode, Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 识别人脸
     */
    private void detectFaces(byte[] nv21) {
        // TODO Auto-generated method stub
        if (faceRectView != null) {
            faceRectView.clearFaceInfo();
        }
        List<FacePreviewInfo> facePreviewInfoList = faceHelper.onPreviewFrame(nv21);
        if (facePreviewInfoList != null && faceRectView != null && drawHelper != null) {
            drawPreviewInfo(facePreviewInfoList);
        }
        //  registerFace(nv21, facePreviewInfoList);
        //  clearLeftFace(facePreviewInfoList);

        if (facePreviewInfoList != null && facePreviewInfoList.size() > 0 && previewSize != null) {
            for (int i = 0; i < facePreviewInfoList.size(); i++) {
                if (livenessDetect) {
                    livenessMap.put(facePreviewInfoList.get(i).getTrackId(), facePreviewInfoList.get(i).getLivenessInfo().getLiveness());
                }
                /**
                 * 对于每个人脸，若状态为空或者为失败，则请求FR（可根据需要添加其他判断以限制FR次数），
                 * FR回传的人脸特征结果在{@link FaceListener#onFaceFeatureInfoGet(FaceFeature, Integer)}中回传
                 */
                if (requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId()) == null
                        || requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId()) == RequestFeatureStatus.FAILED) {
                    requestFeatureStatusMap.put(facePreviewInfoList.get(i).getTrackId(), RequestFeatureStatus.SEARCHING);
                    faceHelper.requestFaceFeature(nv21, facePreviewInfoList.get(i).getFaceInfo(), previewSize.width, previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(i).getTrackId());
//                            Log.i(TAG, "onPreview: fr start = " + System.currentTimeMillis() + " trackId = " + facePreviewInfoList.get(i).getTrackId());
                }
            }
        }
    }

    /**
     * 提取特征值 format FaceEngine.CP_PAF_NV21
     */
    private void extractFaceFeature(FaceEngine faceEngine, byte[] nv21, int format, FaceInfo faceInfo, FaceFeature faceFeature) {
        int frCode = faceEngine.extractFaceFeature(nv21, mPublisher.getEncoder().getOutputWidth(), mPublisher.getEncoder().getOutputHeight(), format, faceInfo, faceFeature);
    }

    /**
     * 比较特征值
     */
    private float compareFaceFeature(FaceEngine faceEngine, FaceFeature sourceFaceFeature, FaceFeature targetFaceFeature, FaceSimilar faceSimilar) {
        faceEngine.compareFaceFeature(sourceFaceFeature, targetFaceFeature, faceSimilar);
        return faceSimilar.getScore();
    }

    /**
     * 销毁引擎
     */
    private void unInitEngine() {

        if (afCode == ErrorInfo.MOK) {
            afCode = faceEngine.unInit();
            Log.i(TAG, "unInitEngine: " + afCode);
        }
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
                            .subscribe(new Consumer<Long>() {
                                @Override
                                public void accept(Long aLong) {
                                    onFaceFeatureInfoGet(faceFeature, requestId);
                                }
                            }));
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

    };


}

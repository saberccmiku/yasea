package net.ossrs.yasea.demo.view;

import android.Manifest;
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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.arcsoft.face.AgeInfo;
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
import net.ossrs.yasea.demo.base.BaseActivity;
import net.ossrs.yasea.demo.bean.DrawInfo;
import net.ossrs.yasea.demo.bean.FacePreviewInfo;
import net.ossrs.yasea.demo.bean.FaceRegisterInfo;
import net.ossrs.yasea.demo.faceserver.CompareResult;
import net.ossrs.yasea.demo.net.ApiConstants;
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
        SrsRecordHandler.SrsRecordListener, SrsEncodeHandler.SrsEncodeListener {

    private static final String TAG = "Yasea";
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    private Button btnPublish;
    private Button btnSwitchCamera;
    private Button btnRecord;
    private Button btnSwitchEncoder;
    private Button btnPause;
    private String rtmpUrl = ApiConstants.rtmpUrl;
    private String recPath = Environment.getExternalStorageDirectory().getPath() + "/test.mp4";
    private SrsPublisher mPublisher;
    private SrsCameraView mCameraView;
    private static final float SIMILAR_THRESHOLD = 0.8F;
    private boolean isOnline = true;

    private FaceEngine faceEngine;
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
    private FaceRectView faceRectView;
    /**
     * 是否正在搜索人脸，保证搜索操作单线程进行
     */
    private boolean isProcessing = false;
    private FaceHelper faceHelper;
    private DrawHelper drawHelper;
    private ConcurrentHashMap<Integer, Integer> requestFeatureStatusMap = new ConcurrentHashMap<>();
    private Camera.Size previewSize;

    /**
     * 所需的所有权限信息
     */
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE
    };


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
        final Button btn = (Button) findViewById(R.id.publish);
        btn.setEnabled(true);
        mPublisher.resumeRecord();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPublisher.pauseRecord();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPublisher.stopPublish();
        mPublisher.stopRecord();
    }

    @Override
    public int getLayoutId() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        return R.layout.activity_main;
    }

    @Override
    public void initView() {
        // response screen rotation event
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        // restore data.
        btnPublish = (Button) findViewById(R.id.publish);
        btnSwitchCamera = (Button) findViewById(R.id.swCam);
        btnRecord = (Button) findViewById(R.id.record);
        btnSwitchEncoder = (Button) findViewById(R.id.swEnc);
        btnPause = (Button) findViewById(R.id.pause);
        btnPause.setEnabled(false);
        mCameraView = findViewById(R.id.glsurfaceview_camera);

        mPublisher = new SrsPublisher(mCameraView);
        //     mPublisher.setFaceHandler(myHandler);
        //编码状态回调
        mPublisher.setEncodeHandler(new SrsEncodeHandler(this), myHandler);
        //rtmp推流状态回调
        mPublisher.setRtmpHandler(new RtmpHandler(this));
        mPublisher.setRecordHandler(new SrsRecordHandler(this));
        //预览分辨率
        mPublisher.setPreviewResolution(960, 540);
        //推流分辨率
        mPublisher.setOutputResolution(540, 960);
        //传输率
        mPublisher.setVideoHDMode();
        //开启美颜（其他滤镜效果在MagicFilterType中查看）
        mPublisher.switchCameraFilter(MagicFilterType.BEAUTY);
        //打开摄像头，开始预览（未推流）
        mPublisher.startCamera();
        //mPublisher.switchToSoftEncoder();//选择软编码
        mPublisher.switchToHardEncoder();//选择硬编码
        //recyclerView=findViewById(R.id.recyclerView);
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

        //recyclerView=findViewById(R.id.recyclerView);
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

        //1.初始化按钮监听事件
        initListener();
        activeEngine();
        initEngine();

        previewSize = mPublisher.getCamera().getParameters().getPreviewSize();
        btnPublish.setText("publish");
        btnPublish.callOnClick();
        System.out.println("----------getOutputWidth--------------");
        System.out.println(mPublisher.getEncoder().getOutputWidth());
        System.out.println(mPublisher.getEncoder().getOutputHeight());
        System.out.println("----------getOutputWidth--------------");
        previewSize.width = 540;
        previewSize.height = 960;
//        previewSize.width = 540;
//        previewSize.height = 960;
        faceHelper = new FaceHelper.Builder()
                .faceEngine(faceEngine)
                .frThreadNum(MAX_DETECT_NUM)
                .previewSize(previewSize)
                .faceListener(faceListener)
                .build();
        ViewGroup.LayoutParams lp = mCameraView.getLayoutParams();

        drawHelper = new DrawHelper(620,
                580,
                760,
                760, 0
                , mPublisher.getCameraId(), true, true, false);

    }

    @BindView(R.id.fl_video)
    FrameLayout flVideo;

    @BindView(R.id.tv_online)
    TextView tvOnline;

    @OnClick(R.id.fab_online)
    public void setOnline() {
        isOnline = !isOnline;
        if (isOnline) {
            mPublisher.startPublish(rtmpUrl);
            mPublisher.startCamera();

            if (btnSwitchEncoder.getText().toString().contentEquals("soft encoder")) {
                Toast.makeText(getApplicationContext(), "Use hard encoder", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Use soft encoder", Toast.LENGTH_SHORT).show();
            }
            btnPublish.setText("stop");
            btnSwitchEncoder.setEnabled(false);
            btnPause.setEnabled(true);
            flVideo.setVisibility(View.VISIBLE);
            tvOnline.setText("工作中");
        } else {
            mPublisher.stopPublish();
            mPublisher.stopRecord();
            mPublisher.stopCamera();
            btnPublish.setText("publish");
            btnRecord.setText("record");
            btnSwitchEncoder.setEnabled(true);
            btnPause.setEnabled(false);
            flVideo.setVisibility(View.GONE);
            tvOnline.setText("离线中");
        }

    }

    private void initListener() {
        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnPublish.getText().toString().contentEquals("publish")) {
                    mPublisher.startPublish(rtmpUrl);
                    mPublisher.startCamera();

                    if (btnSwitchEncoder.getText().toString().contentEquals("soft encoder")) {
                        Toast.makeText(getApplicationContext(), "Use hard encoder", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Use soft encoder", Toast.LENGTH_SHORT).show();
                    }
                    btnPublish.setText("stop");
                    btnSwitchEncoder.setEnabled(false);
                    btnPause.setEnabled(true);
                } else if (btnPublish.getText().toString().contentEquals("stop")) {
                    mPublisher.stopPublish();
                    mPublisher.stopRecord();
                    btnPublish.setText("publish");
                    btnRecord.setText("record");
                    btnSwitchEncoder.setEnabled(true);
                    btnPause.setEnabled(false);
                }
            }
        });
        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btnPause.getText().toString().equals("Pause")) {
                    mPublisher.pausePublish();
                    btnPause.setText("resume");
                } else {
                    mPublisher.resumePublish();
                    btnPause.setText("Pause");
                }
            }
        });

        btnSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPublisher.switchCameraFace((mPublisher.getCameraId() + 1) % Camera.getNumberOfCameras());
            }
        });

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });

        btnSwitchEncoder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnSwitchEncoder.getText().toString().contentEquals("soft encoder")) {
                    mPublisher.switchToSoftEncoder();
                    btnSwitchEncoder.setText("hard encoder");
                } else if (btnSwitchEncoder.getText().toString().contentEquals("hard encoder")) {
                    mPublisher.switchToHardEncoder();
                    btnSwitchEncoder.setText("soft encoder");
                }
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
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            mPublisher.stopPublish();
            mPublisher.stopRecord();
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
        List<DrawInfo> drawInfoList = new ArrayList<>();
        for (int i = 0; i < facePreviewInfoList.size(); i++) {
            String name = faceHelper.getName(facePreviewInfoList.get(i).getTrackId());
            Integer liveness = livenessMap.get(facePreviewInfoList.get(i).getTrackId());
            drawInfoList.add(new DrawInfo(drawHelper.adjustRect(facePreviewInfoList.get(i).getFaceInfo().getRect()), GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE,
                    liveness == null ? LivenessInfo.UNKNOWN : liveness,
                    name == null ? String.valueOf(facePreviewInfoList.get(i).getTrackId()) : name));
        }
        drawHelper.draw(faceRectView, drawInfoList);
    }


    private final MyHandler myHandler = new MyHandler(this);

    private class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {


            MainActivity activity = mActivity.get();
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
                            if (faceRectView != null) {
                                faceRectView.clearFaceInfo();
                            }
                            List<FacePreviewInfo> facePreviewInfoList = faceHelper.onPreviewFrame(data);
                            if (facePreviewInfoList != null && facePreviewInfoList.size() > 0 && faceRectView != null && drawHelper != null) {
                                drawPreviewInfo(facePreviewInfoList);

                            } else {
                                if (compareResultList.size() > 0) {
                                    compareResultList.clear();
                                    //mAdapter.notifyDataSetChanged();
                                }

                            }
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
                                        faceHelper.requestFaceFeature(data, facePreviewInfoList.get(i).getFaceInfo(), previewSize.width, previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(i).getTrackId());
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
                        //         initRecycleView();
                        //   recyclerView.getAdapter().
                        //           recyclerView.getAdapter().notifyDataSetChanged();
                        break;
                    case 3:
                        break;
                }
            }

        }
    }


    private void searchFace(final byte[] nv21, final FaceFeature frFace, final Integer requestId) {

        Observable
                .create(new ObservableOnSubscribe<CompareResult>() {
                    @Override
                    public void subscribe(ObservableEmitter<CompareResult> emitter) {
//                        Log.i(TAG, "subscribe: fr search start = " + System.currentTimeMillis() + " trackId = " + requestId);
                        CompareResult compareResult = getTopOfFaceLib(nv21, frFace);
//                        Log.i(TAG, "subscribe: fr search end = " + System.currentTimeMillis() + " trackId = " + requestId);
                        if (compareResult == null) {
                            emitter.onError(null);
                        } else {
                            emitter.onNext(compareResult);
                        }
                    }
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
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
                                    // adapter.notifyItemRemoved(0);
                                }
                                //添加显示人员时，保存其trackId
                                compareResult.setTrackId(requestId);
                                compareResultList.add(compareResult);

                            }
                            //mAdapter.notifyDataSetChanged();
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.SUCCEED);
                            faceHelper.addName(requestId, compareResult.getUserName());
                            // mAdapter.notifyDataSetChanged();
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
                .url("http://192.168.0.61:8080/faceSearch1")
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
                .url("http://192.168.0.40:8080/faceSearch1")
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
        faceEngine.init(this, FaceEngine.ASF_DETECT_MODE_VIDEO, FaceEngine.ASF_OP_0_HIGHER_EXT,
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_RECOGNITION | FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_AGE | FaceEngine.ASF_FACE3DANGLE | FaceEngine.ASF_GENDER | FaceEngine.ASF_LIVENESS);
        VersionInfo versionInfo = new VersionInfo();
        faceEngine.getVersion(versionInfo);
        Log.i(TAG, "initEngine:  init: " + afCode + "  version:" + versionInfo);
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
     * 比较特征值
     */
    private void unInit() {
        if (faceEngine != null) {
            faceEngine.unInit();
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

        }

        //请求FR的回调
        @Override
        public void onFaceFeatureInfoGet(final byte[] nv21, @Nullable final FaceFeature faceFeature, final Integer requestId) {
            //FR成功
            if (faceFeature != null) {
//                    Log.i(TAG, "onPreview: fr end = " + System.currentTimeMillis() + " trackId = " + requestId);

                //不做活体检测的情况，直接搜索
                if (!livenessDetect) {
                    searchFace(nv21, faceFeature, requestId);
                }
                //活体检测通过，搜索特征
                else if (livenessMap.get(requestId) != null && livenessMap.get(requestId) == LivenessInfo.ALIVE) {
                    searchFace(nv21, faceFeature, requestId);
                }
                //活体检测未出结果，延迟100ms再执行该函数
                else if (livenessMap.get(requestId) != null && livenessMap.get(requestId) == LivenessInfo.UNKNOWN) {
                    getFeatureDelayedDisposables.add(Observable.timer(WAIT_LIVENESS_INTERVAL, TimeUnit.MILLISECONDS)
                            .subscribe(new Consumer<Long>() {
                                @Override
                                public void accept(Long aLong) {
                                    onFaceFeatureInfoGet(nv21, faceFeature, requestId);
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

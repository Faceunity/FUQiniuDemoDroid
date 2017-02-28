package com.qiniu.pili.droid.streaming.demo;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.NetworkInfo;
import com.qiniu.android.dns.http.DnspodFree;
import com.qiniu.android.dns.local.AndroidDnsServer;
import com.qiniu.android.dns.local.Resolver;
import com.qiniu.pili.droid.streaming.AudioSourceCallback;
import com.qiniu.pili.droid.streaming.CameraStreamingSetting;
import com.qiniu.pili.droid.streaming.CameraStreamingSetting.CAMERA_FACING_ID;
import com.qiniu.pili.droid.streaming.FrameCapturedCallback;
import com.qiniu.pili.droid.streaming.MediaStreamingManager;
import com.qiniu.pili.droid.streaming.MicrophoneStreamingSetting;
import com.qiniu.pili.droid.streaming.StreamStatusCallback;
import com.qiniu.pili.droid.streaming.StreamingPreviewCallback;
import com.qiniu.pili.droid.streaming.StreamingProfile;
import com.qiniu.pili.droid.streaming.StreamingSessionListener;
import com.qiniu.pili.droid.streaming.StreamingState;
import com.qiniu.pili.droid.streaming.StreamingStateChangedListener;
import com.qiniu.pili.droid.streaming.SurfaceTextureCallback;
import com.qiniu.pili.droid.streaming.demo.gles.FBO;
import com.qiniu.pili.droid.streaming.demo.ui.RotateLayout;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import com.faceunity.wrapper.faceunity;

/**
 * Created by jerikc on 15/7/6.
 */
public class StreamingBaseActivity extends Activity implements
        View.OnLayoutChangeListener,
        StreamStatusCallback,
        StreamingPreviewCallback,
        SurfaceTextureCallback,
        AudioSourceCallback,
        CameraPreviewFrameView.Listener,
        StreamingSessionListener,
        StreamingStateChangedListener {

    private static final String TAG = "StreamingBaseActivity";

    private static final int ZOOM_MINIMUM_WAIT_MILLIS = 33; //ms

    private Context mContext;

    protected Button mShutterButton;
    private Button mMuteButton;
    private Button mTorchBtn;
    private Button mCameraSwitchBtn;
    private Button mCaptureFrameBtn;
    private Button mEncodingOrientationSwitcherBtn;
    private Button mFaceBeautyBtn;
    private RotateLayout mRotateLayout;

    protected TextView mSatusTextView;
    private TextView mLogTextView;
    private TextView mStreamStatus;

    protected boolean mShutterButtonPressed = false;
    private boolean mIsTorchOn = false;
    private boolean mIsNeedMute = false;
    private boolean mIsNeedFB = false;
    private boolean isEncOrientationPort = true;

    protected static final int MSG_START_STREAMING  = 0;
    protected static final int MSG_STOP_STREAMING   = 1;
    private static final int MSG_SET_ZOOM           = 2;
    private static final int MSG_MUTE               = 3;
    private static final int MSG_FB                 = 4;

    protected String mStatusMsgContent;

    protected String mLogContent = "\n";

    private View mRootView;

    protected MediaStreamingManager mMediaStreamingManager;
    protected CameraStreamingSetting mCameraStreamingSetting;
    protected MicrophoneStreamingSetting mMicrophoneStreamingSetting;
    protected StreamingProfile mProfile;
    protected JSONObject mJSONObject;
    private boolean mOrientationChanged = false;

    protected boolean mIsReady = false;

    private int mCurrentZoom = 0;
    private int mMaxZoom = 0;

    private FBO mFBO = new FBO();

    private Screenshooter mScreenshooter = new Screenshooter();
    private Switcher mSwitcher = new Switcher();
    private EncodingOrientationSwitcher mEncodingOrientationSwitcher = new EncodingOrientationSwitcher();

    private int mCurrentCamFacingIndex;
    private TextView mItemHintText;

    protected Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_STREAMING:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // disable the shutter button before startStreaming
                            setShutterButtonEnabled(false);
                            boolean res = mMediaStreamingManager.startStreaming();
                            mShutterButtonPressed = true;
                            Log.i(TAG, "res:" + res);
                            if (!res) {
                                mShutterButtonPressed = false;
                                setShutterButtonEnabled(true);
                            }
                            setShutterButtonPressed(mShutterButtonPressed);
                        }
                    }).start();
                    break;
                case MSG_STOP_STREAMING:
                    if (mShutterButtonPressed) {
                        // disable the shutter button before stopStreaming
                        setShutterButtonEnabled(false);
                        boolean res = mMediaStreamingManager.stopStreaming();
                        if (!res) {
                            mShutterButtonPressed = true;
                            setShutterButtonEnabled(true);
                        }
                        setShutterButtonPressed(mShutterButtonPressed);
                    }
                    break;
                case MSG_SET_ZOOM:
                    mMediaStreamingManager.setZoomValue(mCurrentZoom);
                    break;
                case MSG_MUTE:
                    //mIsNeedMute = !mIsNeedMute;
                    //mMediaStreamingManager.mute(mIsNeedMute);
                    m_cur_filter_id++;
                    if(m_cur_filter_id>=m_filters.length){
                        m_cur_filter_id=0;
                    }
                    updateMuteButtonText();
                    break;
                case MSG_FB:
                    //mIsNeedFB = !mIsNeedFB;
                    //mMediaStreamingManager.setVideoFilterType(CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_BEAUTY);
                    mMediaStreamingManager.setVideoFilterType(CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_NONE);

                    m_cur_item_id++;
                    if(m_cur_item_id>=m_item_names.length){
                    	m_cur_item_id=0;
                    }
                    updateFBButtonText();
                    break;
                default:
                    Log.e(TAG, "Invalid message");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        } else {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Config.SCREEN_ORIENTATION == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            isEncOrientationPort = true;
        } else if (Config.SCREEN_ORIENTATION == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            isEncOrientationPort = false;
        }
        setRequestedOrientation(Config.SCREEN_ORIENTATION);

        setContentView(R.layout.activity_camera_streaming);
//
//        SharedLibraryNameHelper.getInstance().renameSharedLibrary(
//                SharedLibraryNameHelper.PLSharedLibraryType.PL_SO_TYPE_AAC,
//                getApplicationInfo().nativeLibraryDir + "/libpldroid_streaming_aac_encoder_v7a.so");
//
//        SharedLibraryNameHelper.getInstance().renameSharedLibrary(
//                SharedLibraryNameHelper.PLSharedLibraryType.PL_SO_TYPE_CORE, "pldroid_streaming_core");
//
//        SharedLibraryNameHelper.getInstance().renameSharedLibrary(
//                SharedLibraryNameHelper.PLSharedLibraryType.PL_SO_TYPE_H264, "pldroid_streaming_h264_encoder_v7a");

        String publishUrlFromServer = getIntent().getStringExtra(Config.EXTRA_KEY_PUB_URL);
        Log.i(TAG, "publishUrlFromServer:" + publishUrlFromServer);

        mContext = this;

        StreamingProfile.AudioProfile aProfile = new StreamingProfile.AudioProfile(44100, 96 * 1024);
        StreamingProfile.VideoProfile vProfile = new StreamingProfile.VideoProfile(30, 1000 * 1024, 48);
        StreamingProfile.AVProfile avProfile = new StreamingProfile.AVProfile(vProfile, aProfile);

        mProfile = new StreamingProfile();

        if (publishUrlFromServer.startsWith(Config.EXTRA_PUBLISH_URL_PREFIX)) {
            // publish url
            try {
                mProfile.setPublishUrl(publishUrlFromServer.substring(Config.EXTRA_PUBLISH_URL_PREFIX.length()));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else if (publishUrlFromServer.startsWith(Config.EXTRA_PUBLISH_JSON_PREFIX)) {
            try {
                mJSONObject = new JSONObject(publishUrlFromServer.substring(Config.EXTRA_PUBLISH_JSON_PREFIX.length()));
                StreamingProfile.Stream stream = new StreamingProfile.Stream(mJSONObject);
                mProfile.setStream(stream);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Invalid Publish Url", Toast.LENGTH_LONG).show();
        }

        mProfile.setVideoQuality(StreamingProfile.VIDEO_QUALITY_HIGH3)
                .setAudioQuality(StreamingProfile.AUDIO_QUALITY_MEDIUM2)
//                .setPreferredVideoEncodingSize(960, 544)
                .setEncodingSizeLevel(Config.ENCODING_LEVEL)
                .setEncoderRCMode(StreamingProfile.EncoderRCModes.QUALITY_PRIORITY)
                .setAVProfile(avProfile)
                .setDnsManager(getMyDnsManager())
                .setStreamStatusConfig(new StreamingProfile.StreamStatusConfig(3))
//                .setEncodingOrientation(StreamingProfile.ENCODING_ORIENTATION.PORT)
                .setSendingBufferProfile(new StreamingProfile.SendingBufferProfile(0.2f, 0.8f, 3.0f, 20 * 1000));

        CAMERA_FACING_ID cameraFacingId = chooseCameraFacingId();
        mCurrentCamFacingIndex = cameraFacingId.ordinal();
        mCameraStreamingSetting = new CameraStreamingSetting();
        mCameraStreamingSetting.setCameraId(Camera.CameraInfo.CAMERA_FACING_BACK)
                .setContinuousFocusModeEnabled(true)
                .setRecordingHint(false)
                .setCameraFacingId(cameraFacingId)
                .setBuiltInFaceBeautyEnabled(true)
                //.setBuiltInFaceBeautyEnabled(false)
                .setResetTouchFocusDelayInMs(3000)
                //.setFocusMode(CameraStreamingSetting.FOCUS_MODE_CONTINUOUS_PICTURE)
                .setCameraPrvSizeLevel(CameraStreamingSetting.PREVIEW_SIZE_LEVEL.SMALL)
                .setCameraPrvSizeRatio(CameraStreamingSetting.PREVIEW_SIZE_RATIO.RATIO_16_9)
                .setFaceBeautySetting(new CameraStreamingSetting.FaceBeautySetting(1.0f, 1.0f, 0.8f))
                //.setVideoFilter(CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_BEAUTY)
                .setVideoFilter(CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_NONE)
                ;
        mIsNeedFB = true;
        mMicrophoneStreamingSetting = new MicrophoneStreamingSetting();
        mMicrophoneStreamingSetting.setBluetoothSCOEnabled(false);

        initUIs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMediaStreamingManager.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mIsReady = false;
        mShutterButtonPressed = false;
        mHandler.removeCallbacksAndMessages(null);
        mMediaStreamingManager.pause();

        faceunity.fuOnDeviceLost();
        mFuNotifyPause = true;
        m_frame_id = 0;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaStreamingManager.destroy();
    }

    protected void setShutterButtonPressed(final boolean pressed) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mShutterButtonPressed = pressed;
                mShutterButton.setPressed(pressed);
            }
        });
    }

    protected void setShutterButtonEnabled(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mShutterButton.setFocusable(enable);
                mShutterButton.setClickable(enable);
                mShutterButton.setEnabled(enable);
            }
        });
    }

    protected void startStreaming() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_START_STREAMING), 50);
    }

    protected void stopStreaming() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_STOP_STREAMING), 50);
    }

    @Override
    public boolean onRecordAudioFailedHandled(int err) {
        return false;
    }

    @Override
    public boolean onRestartStreamingHandled(int err) {
        return mMediaStreamingManager.startStreaming();
    }

    @Override
    public Camera.Size onPreviewSizeSelected(List<Camera.Size> list) {
        Camera.Size size = null;
        if (list != null) {
            for (Camera.Size s : list) {
                if (s.height >= 480) {
                    size = s;
                    break;
                }
            }
        }
//        Log.e(TAG, "selected size :" + size.width + "x" + size.height);
        return size;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.i(TAG, "onSingleTapUp X:" + e.getX() + ",Y:" + e.getY());

        if (mIsReady) {
            setFocusAreaIndicator();
            mMediaStreamingManager.doSingleTapUp((int) e.getX(), (int) e.getY());
            return true;
        }
        return false;
    }

    @Override
    public boolean onZoomValueChanged(float factor) {
        if (mIsReady && mMediaStreamingManager.isZoomSupported()) {
            mCurrentZoom = (int) (mMaxZoom * factor);
            mCurrentZoom = Math.min(mCurrentZoom, mMaxZoom);
            mCurrentZoom = Math.max(0, mCurrentZoom);

            Log.d(TAG, "zoom ongoing, scale: " + mCurrentZoom + ",factor:" + factor + ",maxZoom:" + mMaxZoom);
            if (!mHandler.hasMessages(MSG_SET_ZOOM)) {
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SET_ZOOM), ZOOM_MINIMUM_WAIT_MILLIS);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        Log.i(TAG, "view!!!!:" + v);
    }

    //////////////////
    static final String[] m_item_names={"tiara.mp3", "item0208.mp3", "YellowEar.mp3", "PrincessCrown.mp3",
            "Mood.mp3", "Deer.mp3", "BeagleDog.mp3", "item0501.mp3", "ColorCrown.mp3", "item0210.mp3",  "HappyRabbi.mp3",
            "item0204.mp3", "hartshorn.mp3"};
    static final String[] m_item_hints = new String[m_item_names.length];
    static {
        int moodIndex = Arrays.asList(m_item_names).indexOf("Mood.mp3");
        int rabbitIndex = Arrays.asList(m_item_names).indexOf("item0204.mp3");
        m_item_hints[moodIndex] = "嘴角向上以及嘴角向下";
        m_item_hints[rabbitIndex] = "做咀嚼动作";
    }
    static final String[] m_filters = {"nature", "delta", "electric", "slowlived", "tokyo", "warm"};
    int m_frame_id=0;
    int[] m_items=new int[3];
    int m_created_item_id=-1;
    int m_cur_item_id=0;
    int m_cur_filter_id = 0;
    int m_tracking = -1;
    boolean m_is_sw_encoding;
    byte[] m_cur_image=null;
    int m_cur_texid = -1;

    private static boolean mFuNotifyPause;

    @Override
    public boolean onPreviewFrame(byte[] bytes, int width, int height) {
        // render item to yuv buffer
        //Log.i(TAG, "onPreviewFrame 2");
        if (m_items[0] != 0){
            if (m_is_sw_encoding){
                faceunity.fuItemSetParam(m_items[1], "filter_name", m_filters[m_cur_filter_id]);
                faceunity.fuItemSetParam(m_items[1], "color_level", m_faceunity_color_level);
                faceunity.fuItemSetParam(m_items[1], "blur_level", m_faceunity_blur_level);
                m_cur_texid = faceunity.fuRenderToNV21Image(bytes,width,height,m_frame_id++,m_items);
            }else{
                m_cur_image=bytes;
            }
        }
        return true;
    }
    
    @Override
    public void onSurfaceCreated() {
        try{
	        InputStream is=getAssets().open("v3.mp3");
	        byte[] v3data=new byte[is.available()];
	        is.read(v3data);
	        is.close();
            /**
             * fuSetup parameter explanation
             * @param v3data
             * @param null, old parameter, consider removed in the future
             * @param authpack.A(), auth key byte array content
             **/
            faceunity.fuSetup(v3data, null, authpack.A());
	    }catch(IOException e){
	    	Log.e(TAG, "IOException: "+e);
	    }
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        //Log.i(TAG, "onSurfaceChanged width:" + width + ",height:" + height);
        //mFBO.updateSurfaceSize(width, height);
    }

    @Override
    public void onSurfaceDestroyed() {
        //Log.i(TAG, "onSurfaceDestroyed");
        //mFBO.release();
    }

    public double m_faceunity_beautification_level=1.0;
    public double m_faceunity_color_level = 1.0;
    public int m_faceunity_blur_level = 5;

    @Override
    public int onDrawFrame(int texId, int texWidth, int texHeight, float[] transformMatrix) {
        // newTexId should not equal with texId. texId is from the SurfaceTexture.
        // Otherwise, there is no filter effect.
        if((m_created_item_id!=m_cur_item_id&&m_items[0]!=0) || mFuNotifyPause){
        	faceunity.fuDestroyItem(m_items[0]);
        	m_items[0]=0;
        	m_created_item_id=m_cur_item_id;
            mFuNotifyPause = false;
        }

        if(m_items[0]==0){
        	try{
                boolean useTmpItem = false;

                String tmpName = String.format("/sdcard/_item_%d.mp3", m_cur_item_id);
                File tmpItem = new File(tmpName);
                if (tmpItem.exists()){
                    FileInputStream fis = new FileInputStream(tmpItem);
                    byte[] item_data=new byte[fis.available()];
                    fis.read(item_data);
                    fis.close();
                    m_items[0]=faceunity.fuCreateItemFromPackage(item_data);
                    useTmpItem = true;
                }
                
                if (useTmpItem == false){
                    InputStream is = getAssets().open(m_item_names[m_cur_item_id]);
                    byte[] item_data = new byte[is.available()];
                    is.read(item_data);
                    is.close();
                    m_items[0] = faceunity.fuCreateItemFromPackage(item_data);
                }
	       	}catch(IOException e){
	       		Log.e(TAG, "IOException: "+e);
	       	}
        }

		int isTracking = faceunity.fuIsTracking();
        if (isTracking != m_tracking){
            m_tracking = isTracking;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (m_tracking == 0){
                        mItemHintText.setText("未检测到人脸");
                    }else{
                        mItemHintText.setText(m_item_hints[m_cur_item_id]);
                    }
                }
            });
        }


        // disable beautification for now
		if(m_items[1]==0){
        	try{
        		InputStream is=getAssets().open("face_beautification.mp3");
	        	byte[] item_data=new byte[is.available()];
	        	is.read(item_data);
	        	is.close();
	       		m_items[1]=faceunity.fuCreateItemFromPackage(item_data);
	       	}catch(IOException e){
	       		Log.e(TAG, "IOException: "+e);
	       	}
        }

        /*if (m_items[2] == 0) {
            try{
                InputStream is=getAssets().open("heart.mp3");
                byte[] item_data=new byte[is.available()];
                is.read(item_data);
                is.close();
                m_items[2]=faceunity.fuCreateItemFromPackage(item_data);
            }catch(IOException e){
                Log.e(TAG, "IOException: "+e);
            }
        }*/

        faceunity.fuItemSetParam(m_items[0], "isAndroid", 1.0);
        int newTexId=0;
        if (m_is_sw_encoding){
            if (m_cur_texid >= 0){
                newTexId = m_cur_texid;
            }else{
                newTexId=texId;
            }
        } else {
            if (m_cur_image != null) {
                faceunity.fuItemSetParam(m_items[1], "filter_name", m_filters[m_cur_filter_id]);
                faceunity.fuItemSetParam(m_items[1], "blur_level", m_faceunity_blur_level);
                faceunity.fuItemSetParam(m_items[1], "color_level", m_faceunity_color_level);
                newTexId=faceunity.fuDualInputToTexture(m_cur_image, texId, 0, texWidth, texHeight, m_frame_id++, m_items);
            }else{
                newTexId=texId;
            }
        }
        return newTexId;
    }

    @Override
    public void onAudioSourceAvailable(ByteBuffer byteBuffer, int size, long tsInNanoTime, boolean eof) {
//        for (int i = 0; i < size; i++) {
//            byteBuffer.put(i, (byte) 0x00);
//        }
    }

    @Override
    public void notifyStreamStatusChanged(final StreamingProfile.StreamStatus streamStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStreamStatus.setText("bitrate:" + streamStatus.totalAVBitrate / 1024 + " kbps"
                        + "\naudio:" + streamStatus.audioFps + " fps"
                        + "\nvideo:" + streamStatus.videoFps + " fps");
            }
        });
    }

    private void setTorchEnabled(final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String flashlight = enabled ? getString(R.string.flash_light_off) : getString(R.string.flash_light_on);
                mTorchBtn.setText(flashlight);
            }
        });
    }

    @Override
    public void onStateChanged(StreamingState streamingState, Object extra) {
        Log.i(TAG, "StreamingState streamingState:" + streamingState + ",extra:" + extra);

        switch (streamingState) {
            case PREPARING:
                mStatusMsgContent = getString(R.string.string_state_preparing);
                break;
            case READY:
                mIsReady = true;
                mMaxZoom = mMediaStreamingManager.getMaxZoom();
                mStatusMsgContent = getString(R.string.string_state_ready);
                // start streaming when READY
                startStreaming();
                break;
            case CONNECTING:
                mStatusMsgContent = getString(R.string.string_state_connecting);
                break;
            case STREAMING:
                mStatusMsgContent = getString(R.string.string_state_streaming);
                setShutterButtonEnabled(true);
                setShutterButtonPressed(true);
                break;
            case SHUTDOWN:
                mStatusMsgContent = getString(R.string.string_state_ready);
                setShutterButtonEnabled(true);
                setShutterButtonPressed(false);
                if (mOrientationChanged) {
                    mOrientationChanged = false;
                    startStreaming();
                }
                break;
            case IOERROR:
                mLogContent += "IOERROR\n";
                mStatusMsgContent = getString(R.string.string_state_ready);
                setShutterButtonEnabled(true);
                break;
            case UNKNOWN:
                mStatusMsgContent = getString(R.string.string_state_ready);
                break;
            case SENDING_BUFFER_EMPTY:
                break;
            case SENDING_BUFFER_FULL:
                break;
            case AUDIO_RECORDING_FAIL:
                break;
            case OPEN_CAMERA_FAIL:
                Log.e(TAG, "Open Camera Fail. id:" + extra);
                break;
            case DISCONNECTED:
                mLogContent += "DISCONNECTED\n";
                break;
            case INVALID_STREAMING_URL:
                Log.e(TAG, "Invalid streaming url:" + extra);
                break;
            case UNAUTHORIZED_STREAMING_URL:
                Log.e(TAG, "Unauthorized streaming url:" + extra);
                mLogContent += "Unauthorized Url\n";
                break;
            case CAMERA_SWITCHED:
//                mShutterButtonPressed = false;
                if (extra != null) {
                    Log.i(TAG, "current camera id:" + (Integer) extra);
                }
                Log.i(TAG, "camera switched");
                final int currentCamId = (Integer)extra;
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateCameraSwitcherButtonText(currentCamId);
                        //TODO: bug in the camera switching code: GL context got reset, resource in former context can't be released properly
                        faceunity.fuOnDeviceLost();
                        faceunity.fuOnCameraChange();
                        //destroying the item could cause GL texture double-frees
                        //if(m_items[0]!=0){
                        //	faceunity.fuDestroyItem(m_items[0]);
                        //}
                        m_items[0]=0;
                        m_items[1]=0;
                    }
                });
                break;
            case TORCH_INFO:
                if (extra != null) {
                    final boolean isSupportedTorch = (Boolean) extra;
                    Log.i(TAG, "isSupportedTorch=" + isSupportedTorch);
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isSupportedTorch) {
                                mTorchBtn.setVisibility(View.VISIBLE);
                            } else {
                                mTorchBtn.setVisibility(View.GONE);
                            }
                        }
                    });
                }
                break;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mLogTextView != null) {
                    mLogTextView.setText(mLogContent);
                }
                mSatusTextView.setText(mStatusMsgContent);
            }
        });
    }

    private void initUIs() {
        mRootView = findViewById(R.id.content);
        mRootView.addOnLayoutChangeListener(this);

        mMuteButton = (Button) findViewById(R.id.mute_btn);
        mShutterButton = (Button) findViewById(R.id.toggleRecording_button);
        mTorchBtn = (Button) findViewById(R.id.torch_btn);
        mCameraSwitchBtn = (Button) findViewById(R.id.camera_switch_btn);
        mCaptureFrameBtn = (Button) findViewById(R.id.capture_btn);
        mFaceBeautyBtn = (Button) findViewById(R.id.fb_btn);
        mSatusTextView = (TextView) findViewById(R.id.streamingStatus);

        mLogTextView = (TextView) findViewById(R.id.log_info);
        mStreamStatus = (TextView) findViewById(R.id.stream_status);

        mFaceBeautyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mHandler.hasMessages(MSG_FB)) {
                    mHandler.sendEmptyMessage(MSG_FB);
                }
            }
        });

        mMuteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mHandler.hasMessages(MSG_MUTE)) {
                    mHandler.sendEmptyMessage(MSG_MUTE);
                }
            }
        });

        mShutterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mShutterButtonPressed) {
                    stopStreaming();
                } else {
                    startStreaming();
                }
            }
        });

        mTorchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (!mIsTorchOn) {
                            mIsTorchOn = true;
                            mMediaStreamingManager.turnLightOn();
                        } else {
                            mIsTorchOn = false;
                            mMediaStreamingManager.turnLightOff();
                        }
                        setTorchEnabled(mIsTorchOn);
                    }
                }).start();
            }
        });

        mCameraSwitchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mHandler.removeCallbacks(mSwitcher);
                mHandler.postDelayed(mSwitcher, 100);
            }
        });

        mCaptureFrameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mHandler.removeCallbacks(mScreenshooter);
                mHandler.postDelayed(mScreenshooter, 100);
            }
        });


        mEncodingOrientationSwitcherBtn = (Button) findViewById(R.id.orientation_btn);
        mEncodingOrientationSwitcherBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.removeCallbacks(mEncodingOrientationSwitcher);
                mHandler.post(mEncodingOrientationSwitcher);
            }
        });

        SeekBar seekBarBeauty = (SeekBar) findViewById(R.id.colorLevel_seekBar);
        seekBarBeauty.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                CameraStreamingSetting.FaceBeautySetting fbSetting = mCameraStreamingSetting.getFaceBeautySetting();
                fbSetting.beautyLevel = progress / 100.0f;
                fbSetting.whiten = progress / 100.0f;
                fbSetting.redden = progress / 100.0f;

                //m_faceunity_beautification_level=(double)(progress/100.0f);
                m_faceunity_color_level = (double)(progress/50.f);
                mMediaStreamingManager.updateFaceBeautySetting(fbSetting);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        SeekBar seekBarBlur = (SeekBar) findViewById(R.id.blurRadius_seekBar);
        seekBarBlur.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                m_faceunity_blur_level = Math.round(progress/(100.0f/6.0f));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        initButtonText();
        mItemHintText = (TextView) findViewById(R.id.no_face);
    }

    private void initButtonText() {
        updateCameraSwitcherButtonText(mCameraStreamingSetting.getReqCameraId());
        mCaptureFrameBtn.setText("Capture");
        updateFBButtonText();
        updateMuteButtonText();
        updateOrientationBtnText();
    }

    private void updateOrientationBtnText() {
        if (isEncOrientationPort) {
            mEncodingOrientationSwitcherBtn.setText("Land");
        } else {
            mEncodingOrientationSwitcherBtn.setText("Port");
        }
    }

    protected void setFocusAreaIndicator() {
        if (mRotateLayout == null) {
            mRotateLayout = (RotateLayout)findViewById(R.id.focus_indicator_rotate_layout);
            mMediaStreamingManager.setFocusAreaIndicator(mRotateLayout,
                    mRotateLayout.findViewById(R.id.focus_indicator));
        }
    }

    private void updateFBButtonText() {
        if (mFaceBeautyBtn != null) {
            //mFaceBeautyBtn.setText(mIsNeedFB ? "FB Off" : "FB On");
            String itemNum = "Item" + Integer.toString(m_cur_item_id);
            mFaceBeautyBtn.setText(itemNum);
            if (m_tracking > 0){
                mItemHintText.setText(m_item_hints[m_cur_item_id]);
            }
        }
    }

    private void updateMuteButtonText() {
        if (mMuteButton != null) {
            String filterNum = "Filter:" + m_filters[m_cur_filter_id];
            mMuteButton.setText(filterNum);
        }
    }

    private void updateCameraSwitcherButtonText(int camId) {
        if (mCameraSwitchBtn == null) {
            return;
        }
        if (camId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mCameraSwitchBtn.setText("Back");
        } else {
            mCameraSwitchBtn.setText("Front");
        }
    }

    private void saveToSDCard(String filename, Bitmap bmp) throws IOException {
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File file = new File(Environment.getExternalStorageDirectory(), filename);
            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(file));
                bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
                bmp.recycle();
                bmp = null;
            } finally {
                if (bos != null) bos.close();
            }

            final String info = "Save frame to:" + Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + filename;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, info, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private static DnsManager getMyDnsManager() {
        IResolver r0 = new DnspodFree();
        IResolver r1 = AndroidDnsServer.defaultResolver();
        IResolver r2 = null;
        try {
            r2 = new Resolver(InetAddress.getByName("119.29.29.29"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return new DnsManager(NetworkInfo.normal, new IResolver[]{r0, r1, r2});
    }

    private CAMERA_FACING_ID chooseCameraFacingId() {
        if (CameraStreamingSetting.hasCameraFacing(CAMERA_FACING_ID.CAMERA_FACING_3RD)) {
            return CAMERA_FACING_ID.CAMERA_FACING_3RD;
        } else if (CameraStreamingSetting.hasCameraFacing(CAMERA_FACING_ID.CAMERA_FACING_FRONT)) {
            return CAMERA_FACING_ID.CAMERA_FACING_FRONT;
        } else {
            return CAMERA_FACING_ID.CAMERA_FACING_BACK;
        }
    }

    private class Switcher implements Runnable {
        @Override
        public void run() {
            mCurrentCamFacingIndex = (mCurrentCamFacingIndex + 1) % CameraStreamingSetting.getNumberOfCameras();

            CAMERA_FACING_ID facingId;
            if (mCurrentCamFacingIndex == CAMERA_FACING_ID.CAMERA_FACING_BACK.ordinal()) {
                facingId = CAMERA_FACING_ID.CAMERA_FACING_BACK;
            } else if (mCurrentCamFacingIndex == CAMERA_FACING_ID.CAMERA_FACING_FRONT.ordinal()) {
                facingId = CAMERA_FACING_ID.CAMERA_FACING_FRONT;
            } else {
                facingId = CAMERA_FACING_ID.CAMERA_FACING_3RD;
            }
            Log.i(TAG, "switchCamera:" + facingId);
            mMediaStreamingManager.switchCamera(facingId);
        }
    }

    private class EncodingOrientationSwitcher implements Runnable {

        @Override
        public void run() {
            Log.i(TAG, "isEncOrientationPort:" + isEncOrientationPort);
            stopStreaming();
            mOrientationChanged = !mOrientationChanged;
            isEncOrientationPort = !isEncOrientationPort;
            mProfile.setEncodingOrientation(isEncOrientationPort ? StreamingProfile.ENCODING_ORIENTATION.PORT : StreamingProfile.ENCODING_ORIENTATION.LAND);
            mMediaStreamingManager.setStreamingProfile(mProfile);
            setRequestedOrientation(isEncOrientationPort ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            mMediaStreamingManager.notifyActivityOrientationChanged();
            updateOrientationBtnText();
            Toast.makeText(StreamingBaseActivity.this, Config.HINT_ENCODING_ORIENTATION_CHANGED,
                    Toast.LENGTH_SHORT).show();
            Log.i(TAG, "EncodingOrientationSwitcher -");
        }
    }

    private class Screenshooter implements Runnable {
        @Override
        public void run() {
            final String fileName = "PLStreaming_" + System.currentTimeMillis() + ".jpg";
            mMediaStreamingManager.captureFrame(100, 100, new FrameCapturedCallback() {
                private Bitmap bitmap;

                @Override
                public void onFrameCaptured(Bitmap bmp) {
                    if (bmp == null) {
                        return;
                    }
                    bitmap = bmp;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                saveToSDCard(fileName, bitmap);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                if (bitmap != null) {
                                    bitmap.recycle();
                                    bitmap = null;
                                }
                            }
                        }
                    }).start();
                }
            });
        }
    }
}

package com.faceunity.wrapper;

import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;


public class FaceunityWrapper {
    private static final String TAG = FaceunityWrapper.class.getName();

    private Context mContext;

    public static boolean isInit = false;

    private byte[] mCameraNV21Byte;
    private byte[] mFuNV21Bytes;

    private int mFrameId = 0;

    private int mFacebeautyItem = 0; //美颜道具
    private int mEffectItem = 0; //贴纸道具
    private int[] itemsArray = {mFacebeautyItem, mEffectItem};

    private float mFacebeautyColorLevel = 0.2f;
    private float mFacebeautyBlurLevel = 6.0f;
    private float mFacebeautyCheeckThin = 1.0f;
    private float mFacebeautyEnlargeEye = 0.5f;
    private float mFacebeautyRedLevel = 0.5f;
    private int mFaceShape = 3;
    private float mFaceShapeLevel = 0.5f;

    private String mFilterName = EffectAndFilterSelectAdapter.FILTERS_NAME[0];

    private boolean isNeedEffectItem = true;
    private String mEffectFileName = EffectAndFilterSelectAdapter.EFFECT_ITEM_FILE_NAME[1];

    private int mCurrentCameraId;
    private boolean isNeedReadBack = false;
    private boolean isNeedUpdateEffectParam = true;
    private int inputImageOrientation;

    private HandlerThread mCreateItemThread;
    private Handler mCreateItemHandler;

    private int faceTrackingStatus = 0;

    private long lastOneHundredFrameTimeStamp = 0;
    private int currentFrameCnt = 0;
    private long oneHundredFrameFUTime = 0;

    public FaceunityWrapper(Context context) {
        mContext = context;
        setCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);
    }

    public synchronized void onSurfaceCreated() {
        if (isInit) {
            Log.e(TAG, "faceunity has been initialized");
            return;
        }

        mCreateItemThread = new HandlerThread("faceunity-efect");
        mCreateItemThread.start();
        mCreateItemHandler = new CreateItemHandler(mCreateItemThread.getLooper());

        try {
            InputStream is = mContext.getAssets().open("v3.mp3");
            byte[] v3data = new byte[is.available()];
            int len = is.read(v3data);
            is.close();
            faceunity.fuSetup(v3data, null, authpack.A());
            faceunity.fuSetMaxFaces(1);
            Log.e(TAG, "fuSetup version " + faceunity.fuGetVersion());
            Log.e(TAG, "fuSetup v3 len " + len);

            is = mContext.getAssets().open("face_beautification.mp3");
            byte[] itemData = new byte[is.available()];
            len = is.read(itemData);
            Log.e(TAG, "beautification len " + len);
            is.close();
            mFacebeautyItem = faceunity.fuCreateItemFromPackage(itemData);
            itemsArray[0] = mFacebeautyItem;

        } catch (IOException e) {
            e.printStackTrace();
        }
        isInit = true;
    }

    public synchronized void onSurfaceDestroyed() {
        if (!isInit) {
            Log.e(TAG, "faceunity no initialization");
            return;
        }

        mFrameId = 0;

        mCreateItemHandler.removeMessages(CreateItemHandler.HANDLE_CREATE_ITEM);
        mCreateItemHandler = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mCreateItemThread.quitSafely();
        } else {
            mCreateItemThread.quit();
        }
        mCreateItemThread = null;

        //Note: 切忌使用一个已经destroy的item
        faceunity.fuDestroyAllItems();
        itemsArray[1] = mEffectItem = 0;
        itemsArray[0] = mFacebeautyItem = 0;
        faceunity.fuOnDeviceLost();
        isNeedEffectItem = true;

        lastOneHundredFrameTimeStamp = 0;
        oneHundredFrameFUTime = 0;

        isInit = false;
    }

    public void setCameraId(int CameraId) {
        mCurrentCameraId = CameraId;
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCurrentCameraId, info);
        inputImageOrientation = info.orientation;
        isNeedUpdateEffectParam = true;
    }

    /**
     * 对纹理进行特效处理
     *
     * @param texId     YUV格式纹理
     * @param texWidth  纹理宽度
     * @param texHeight 纹理高度
     * @return 特效处理后的纹理
     */
    public int onDrawFrame(int texId, int texWidth, int texHeight) {
        if (++currentFrameCnt == 100) {
            currentFrameCnt = 0;
            long tmp = System.nanoTime();
            Log.e(TAG, "dualInput FPS : " + (1000.0f * MiscUtil.NANO_IN_ONE_MILLI_SECOND / ((tmp - lastOneHundredFrameTimeStamp) / 100.0f)));
            lastOneHundredFrameTimeStamp = tmp;
            Log.e(TAG, "dualInput cost time avg : " + oneHundredFrameFUTime / 100.f / MiscUtil.NANO_IN_ONE_MILLI_SECOND);
            oneHundredFrameFUTime = 0;
        }

        if (!isInit) {
            Log.e(TAG, "faceunity no initialization");
            return texId;
        }

        if (mCameraNV21Byte == null || mCameraNV21Byte.length == 0) {
            Log.e(TAG, "camera nv21 bytes null");
            return texId;
        }

        final int isTracking = faceunity.fuIsTracking();
        if (isTracking != faceTrackingStatus) {
            faceTrackingStatus = isTracking;
        }

        if (isNeedEffectItem) {
            isNeedEffectItem = false;
            mCreateItemHandler.sendEmptyMessage(CreateItemHandler.HANDLE_CREATE_ITEM);
        }

        if (isNeedUpdateEffectParam) {
            faceunity.fuItemSetParam(mEffectItem, "isAndroid", 1.0);
            faceunity.fuItemSetParam(mEffectItem, "rotationAngle", (360 - inputImageOrientation));
            isNeedUpdateEffectParam = false;
        }

        faceunity.fuItemSetParam(mFacebeautyItem, "color_level", mFacebeautyColorLevel);
        faceunity.fuItemSetParam(mFacebeautyItem, "blur_level", mFacebeautyBlurLevel);
        faceunity.fuItemSetParam(mFacebeautyItem, "filter_name", mFilterName);
        faceunity.fuItemSetParam(mFacebeautyItem, "cheek_thinning", mFacebeautyCheeckThin);
        faceunity.fuItemSetParam(mFacebeautyItem, "eye_enlarging", mFacebeautyEnlargeEye);
        faceunity.fuItemSetParam(mFacebeautyItem, "face_shape", mFaceShape);
        faceunity.fuItemSetParam(mFacebeautyItem, "face_shape_level", mFaceShapeLevel);
        faceunity.fuItemSetParam(mFacebeautyItem, "red_level", mFacebeautyRedLevel);

        boolean isOESTexture = true; //camera默认的是OES的
        int flags = isOESTexture ? faceunity.FU_ADM_FLAG_EXTERNAL_OES_TEXTURE : 0;
        flags = isNeedReadBack ? flags | faceunity.FU_ADM_FLAG_ENABLE_READBACK : flags;
        flags |= mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT ? 0 : faceunity.FU_ADM_FLAG_FLIP_X;

        long fuStartTime = System.nanoTime();
        int fuTex;
        if (isNeedReadBack) {
            fuTex = faceunity.fuDualInputToTexture(mCameraNV21Byte, texId, flags,
                    texWidth, texHeight, mFrameId++, itemsArray, texWidth, texHeight, mFuNV21Bytes);
        } else {
            fuTex = faceunity.fuDualInputToTexture(mCameraNV21Byte, texId, flags,
                    texWidth, texHeight, mFrameId++, itemsArray);
        }
        long fuEndTime = System.nanoTime();
        oneHundredFrameFUTime += fuEndTime - fuStartTime;

        return fuTex;
    }

    public void setPreviewFrameDate(byte[] data1, byte[] data2) {
        isNeedReadBack = true;
        mCameraNV21Byte = data1;
        mFuNV21Bytes = data2;
    }

    public void setPreviewFrameDate(byte[] data) {
        isNeedReadBack = false;
        mCameraNV21Byte = data;
    }

    class CreateItemHandler extends Handler {

        static final int HANDLE_CREATE_ITEM = 1;

        CreateItemHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLE_CREATE_ITEM:
                    try {
                        if (mEffectFileName.equals("none")) {
                            itemsArray[1] = mEffectItem = 0;
                        } else {
                            InputStream is = mContext.getAssets().open(mEffectFileName);
                            byte[] itemData = new byte[is.available()];
                            int len = is.read(itemData);
                            Log.e("FU", "effect len " + len);
                            is.close();
                            int tmp = itemsArray[1];
                            itemsArray[1] = mEffectItem = faceunity.fuCreateItemFromPackage(itemData);
                            isNeedUpdateEffectParam = true;
                            if (tmp != 0) {
                                faceunity.fuDestroyItem(tmp);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    public FaceunityControlView.OnViewEventListener initUIEventListener() {

        FaceunityControlView.OnViewEventListener eventListener = new FaceunityControlView.OnViewEventListener() {

            @Override
            public void onBlurLevelSelected(int level) {
                mFacebeautyBlurLevel = level;
            }

            @Override
            public void onCheekThinSelected(int progress, int max) {
                mFacebeautyCheeckThin = 1.0f * progress / max;
            }

            @Override
            public void onColorLevelSelected(int progress, int max) {
                mFacebeautyColorLevel = 1.0f * progress / max;
            }

            @Override
            public void onEffectItemSelected(String effectItemName) {
                if (effectItemName.equals(mEffectFileName)) {
                    return;
                }
                if (mCreateItemHandler != null) {
                    mCreateItemHandler.removeMessages(CreateItemHandler.HANDLE_CREATE_ITEM);
                }
                mEffectFileName = effectItemName;
                isNeedEffectItem = true;
            }

            @Override
            public void onEnlargeEyeSelected(int progress, int max) {
                mFacebeautyEnlargeEye = 1.0f * progress / max;
            }

            @Override
            public void onFilterSelected(String filterName) {
                mFilterName = filterName;
            }

            @Override
            public void onRedLevelSelected(int progress, int max) {
                mFacebeautyRedLevel = 1.0f * progress / max;
            }

            @Override
            public void onFaceShapeLevelSelected(int progress, int max) {
                mFaceShapeLevel = (1.0f * progress) / max;
            }

            @Override
            public void onFaceShapeSelected(int faceShape) {
                mFaceShape = faceShape;
            }
        };

        return eventListener;
    }

}

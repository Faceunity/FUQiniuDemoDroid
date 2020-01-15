package com.faceunity.beautycontrolview;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import com.faceunity.beautycontrolview.entity.Effect;
import com.faceunity.beautycontrolview.entity.Filter;
import com.faceunity.wrapper.faceunity;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import static com.faceunity.wrapper.faceunity.FU_ADM_FLAG_FLIP_X;

/**
 * 一个基于Faceunity Nama SDK的简单封装，方便简单集成，理论上简单需求的步骤：
 * <p>
 * 1.通过OnEffectSelectedListener在UI上进行交互
 * 2.合理调用FURenderer构造函数
 * 3.对应的时机调用onSurfaceCreated和onSurfaceDestroyed
 * 4.处理图像时调用onDrawFrame
 * <p>
 */
public class FURenderer implements OnFaceUnityControlListener {
    private static final String TAG = FURenderer.class.getSimpleName();
    public static final int oes = faceunity.FU_ADM_FLAG_EXTERNAL_OES_TEXTURE;

    private Context mContext;

    /**
     * 目录assets下的 *.bundle为程序的数据文件。
     * 其中 v3.bundle：人脸识别数据文件，缺少该文件会导致系统初始化失败；
     * face_beautification.bundle：美颜和美型相关的数据文件；
     * anim_model.bundle：优化表情跟踪功能所需要加载的动画数据文件；适用于使用Animoji和avatar功能的用户，如果不是，可不加载
     * ardata_ex.bundle：高精度模式的三维张量数据文件。适用于换脸功能，如果没用该功能可不加载
     * fxaa.bundle：3D绘制抗锯齿数据文件。加载后，会使得3D绘制效果更加平滑。
     * 目录effects下是我们打包签名好的道具
     */
    public static final String BUNDLE_v3 = "v3.bundle";
    public static final String BUNDLE_face_beautification = "face_beautification.bundle";
    public static final String BUNDLE_animoji_3d = "fxaa.bundle";

    /**
     * 单输入的类型
     */
    public static final int INPUT_NV21 = 0;
    public static final int INPUT_I420 = 1;
    public static final int INPUT_RGBA = 2;

    //美颜和滤镜的默认参数
    private boolean isNeedUpdateFaceBeauty = true;
    private float mFaceBeautyFilterLevel = 1.0f;//滤镜强度
    private Filter mFilterName = FilterEnum.ziran.filter();

    private float mFaceBeautyType = 0.0f;//美肤类型
    private float mFaceBeautyBlurLevel = 0.7f;//磨皮
    private float mFaceBeautyColorLevel = 0.5f;//美白
    private float mFaceBeautyRedLevel = 0.5f;//红润
    private float mBrightEyesLevel = 0.0f;//亮眼
    private float mBeautyTeethLevel = 0.0f;//美牙

    private float mFaceBeautyFaceShape = 4.0f;//脸型
    private float mFaceShapeLevel = 1.0f;//程度
    private float mFaceBeautyEnlargeEye = 0.4f;//大眼
    private float mFaceBeautyCheekThin = 0.4f;//瘦脸
    private float mChinLevel = 0.3f;//下巴
    private float mForeheadLevel = 0.3f;//额头
    private float mThinNoseLevel = 0.5f;//瘦鼻
    private float mMouthShape = 0.4f;//嘴形

    private int mFrameId = 0;

    private static final int ITEM_ARRAYS_FACE_BEAUTY_INDEX = 0;
    private static final int ITEM_ARRAYS_EFFECT = 1;
    private static final int ITEM_ARRAYS_EFFECT_ABIMOJI_3D = 2;
    private static final int ITEM_ARRAYS_COUNT = 3;
    //美颜和其他道具的handle数组
    private final int[] mItemsArray = new int[ITEM_ARRAYS_COUNT];
    //用于和异步加载道具的线程交互
    private HandlerThread mFuItemHandlerThread;
    private Handler mFuItemHandler;

    private boolean isNeedFaceBeauty = true;
    private boolean isNeedAnimoji3D = false;
    private Effect mDefaultEffect;//默认道具（同步加载）
    private int mMaxFaces = 4; //同时识别的最大人脸
    private boolean mIsCreateEGLContext; //是否需要手动创建EGLContext
    private int mInputTextureType = 0; //输入的图像texture类型，Camera提供的默认为EXTERNAL OES
    private int mInputImageFormat = 0;
    private boolean mNeedReadBackImage = false; //将传入的byte[]图像复写为具有道具效果的

    private int mInputImageOrientation = 270;
    private int mInputProp = 0;//输入道具的角度
    private int mCurrentCameraType = Camera.CameraInfo.CAMERA_FACING_FRONT;

    private ArrayList<Runnable> mEventQueue = new ArrayList<>();

    /**
     * 全局加载相应的底层数据包
     */
    public static void initFURenderer(Context context) {
        try {
            //获取faceunity SDK版本信息
            Log.e(TAG, "fu sdk version " + faceunity.fuGetVersion());

            /**
             * fuSetup faceunity初始化
             * 其中 v3.bundle：人脸识别数据文件，缺少该文件会导致系统初始化失败；
             *      authpack：用于鉴权证书内存数组
             * 首先调用完成后再调用其他FU API
             */
            InputStream v3 = context.getAssets().open(BUNDLE_v3);
            byte[] v3Data = new byte[v3.available()];
            v3.read(v3Data);
            v3.close();
            faceunity.fuSetup(v3Data, null, authpack.A());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取faceunity sdk 版本库
     */
    public static String getVersion() {
        return faceunity.fuGetVersion();
    }

    /**
     * FURenderer构造函数
     */
    private FURenderer(Context context, boolean isCreateEGLContext) {
        this.mContext = context;
        this.mIsCreateEGLContext = isCreateEGLContext;

        mFuItemHandlerThread = new HandlerThread("FUItemHandlerThread");
        mFuItemHandlerThread.start();
        mFuItemHandler = new FUItemHandler(mFuItemHandlerThread.getLooper());
    }

    /**
     * 创建及初始化faceunity相应的资源
     */
    public void onSurfaceCreated() {
        Log.e(TAG, "onSurfaceCreated");

        /**
         * fuCreateEGLContext 创建OpenGL环境
         * 适用于没OpenGL环境时调用
         * 如果调用了fuCreateEGLContext，在销毁时需要调用fuReleaseEGLContext
         */
        if (mIsCreateEGLContext) faceunity.fuCreateEGLContext();

        mFrameId = 0;
        /**
         *fuSetExpressionCalibration 控制表情校准功能的开关及不同模式，参数为0时关闭表情校准，1为主动校准，2为被动校准。
         * 被动校准：该种模式下会在整个用户使用过程中逐渐进行表情校准，用户对该过程没有明显感觉。该种校准的强度相比主动校准较弱。
         * 主动校准：老版本的表情校准模式。该种模式下系统会进行快速集中的表情校准，一般为初次识别到人脸之后的2-3秒钟。
         *          在该段时间内，需要用户尽量保持无表情状态，该过程结束后再开始使用。该过程的开始和结束可以通过 fuGetFaceInfo 接口获取参数 is_calibrating
         * 适用于使用Animoji和avatar功能的用户
         */
        faceunity.fuSetExpressionCalibration(1);
        faceunity.fuSetDefaultOrientation((360 - mInputImageOrientation) / 90);//设置多脸，识别人脸默认方向，能够提高首次识别的速度
        faceunity.fuSetMaxFaces(mMaxFaces);//设置多脸，目前最多支持8人。

        if (isNeedFaceBeauty) {
            mFuItemHandler.sendEmptyMessage(ITEM_ARRAYS_FACE_BEAUTY_INDEX);
        }

        if (isNeedAnimoji3D) {
            mFuItemHandler.sendEmptyMessage(ITEM_ARRAYS_EFFECT_ABIMOJI_3D);
        }

        //加载默认道具
        if (mDefaultEffect != null) {
            mItemsArray[ITEM_ARRAYS_EFFECT] = loadItem(mDefaultEffect);
            faceunity.fuSetMaxFaces(mDefaultEffect.maxFace());
        }
    }

    /**
     * 单输入接口texture接口
     *
     * @param tex
     * @param w
     * @param h
     * @return
     */
    public int onDrawFrameSingleInputTex(int tex, int w, int h) {
        if (tex <= 0 || w <= 0 || h <= 0) {
            Log.e(TAG, "onDrawFrame date null");
            return tex;
        }
        prepareDrawFrame();

        int flags = mInputImageFormat;
        if (mCurrentCameraType != Camera.CameraInfo.CAMERA_FACING_FRONT)
            flags |= FU_ADM_FLAG_FLIP_X;

        if (mNeedBenchmark) mFuCallStartTime = System.nanoTime();

        int fuTex = faceunity.fuRenderToTexture(tex, w, h, mFrameId++, mItemsArray, flags);
        if (mNeedBenchmark) mOneHundredFrameFUTime += System.nanoTime() - mFuCallStartTime;
        return fuTex;
    }

    /**
     * 单输入接口buffer接口
     *
     * @param img
     * @param w
     * @param h
     * @param type :输入的buffer类型
     * @return
     */
    public int onDrawFrameSingleInput(byte[] img, int w, int h, int type) {
        if (img == null || w <= 0 || h <= 0) {
            Log.e(TAG, "onDrawFrame date null");
            return 0;
        }
        prepareDrawFrame();

        int flags = mInputImageFormat;
        if (mCurrentCameraType != Camera.CameraInfo.CAMERA_FACING_FRONT)
            flags |= FU_ADM_FLAG_FLIP_X;

        if (mNeedBenchmark) mFuCallStartTime = System.nanoTime();

        int fuTex;
        switch (type) {
            case INPUT_I420:
                fuTex = faceunity.fuRenderToI420Image(img, w, h, mFrameId++, mItemsArray, flags);
                break;
            case INPUT_RGBA:
                fuTex = faceunity.fuRenderToRgbaImage(img, w, h, mFrameId++, mItemsArray, flags);
                break;
            default:
                //默认NV21
                fuTex = faceunity.fuRenderToNV21Image(img, w, h, mFrameId++, mItemsArray, flags);
                break;
        }
        if (mNeedBenchmark) mOneHundredFrameFUTime += System.nanoTime() - mFuCallStartTime;
        return fuTex;
    }

    /**
     * 单输入接口，自定义画面数据需要回写到的byte[]
     *
     * @param img
     * @param w
     * @param h
     * @param readBackImg 画面数据需要回写到的byte[]
     * @param readBackW
     * @param readBackH
     * @param type        :输入的buffer类型
     * @return
     */
    public int onDrawFrameSingleInput(byte[] img, int w, int h, byte[] readBackImg, int readBackW, int readBackH, int type) {
        if (img == null || w <= 0 || h <= 0 || readBackImg == null || readBackW <= 0 || readBackH <= 0) {
            Log.e(TAG, "onDrawFrame date null");
            return 0;
        }
        prepareDrawFrame();
        int flags = mInputImageFormat;
        if (mCurrentCameraType != Camera.CameraInfo.CAMERA_FACING_FRONT)
            flags |= FU_ADM_FLAG_FLIP_X;
        if (mNeedBenchmark) mFuCallStartTime = System.nanoTime();
        int fuTex;
        switch (type) {
            case INPUT_I420:
                fuTex = faceunity.fuRenderToI420Image(img, w, h, mFrameId++, mItemsArray, flags,
                        readBackW, readBackH, readBackImg);
                break;
            case INPUT_RGBA:
                fuTex = faceunity.fuRenderToRgbaImage(img, w, h, mFrameId++, mItemsArray, flags,
                        readBackW, readBackH, readBackImg);
                break;
            default:
                //默认NV21
                fuTex = faceunity.fuRenderToNV21Image(img, w, h, mFrameId++, mItemsArray, flags,
                        readBackW, readBackH, readBackImg);
                break;
        }
        if (mNeedBenchmark) mOneHundredFrameFUTime += System.nanoTime() - mFuCallStartTime;
        return fuTex;
    }

    /**
     * 双输入接口(fuDualInputToTexture)(处理后的画面数据并不会回写到数组)，由于省去相应的数据拷贝性能相对最优，推荐使用。
     *
     * @param img NV21数据
     * @param tex 纹理ID
     * @param w
     * @param h
     * @return
     */
    public int onDrawFrameDoubleI420Input(byte[] img, int tex, int w, int h) {
        if (tex <= 0 || img == null || w <= 0 || h <= 0) {
            Log.e(TAG, "onDrawFrame date null");
            return 0;
        }
        prepareDrawFrame();

        int flags = mInputTextureType | mInputImageFormat;
        if (mCurrentCameraType != Camera.CameraInfo.CAMERA_FACING_FRONT)
            flags |= FU_ADM_FLAG_FLIP_X;

        if (mNeedBenchmark) mFuCallStartTime = System.nanoTime();
        int fuTex = faceunity.fuDualInputToTexture(img, tex, flags, w, h, mFrameId++, mItemsArray);
        if (mNeedBenchmark) mOneHundredFrameFUTime += System.nanoTime() - mFuCallStartTime;
        return fuTex;
    }

    public int onDrawFrameDoubleInput(byte[] img, int tex, int w, int h) {
        if (tex <= 0 || img == null || w <= 0 || h <= 0) {
            Log.e(TAG, "onDrawFrame date null");
            return 0;
        }
        prepareDrawFrame();

        int flags = mInputTextureType | mInputImageFormat;
        if (mCurrentCameraType != Camera.CameraInfo.CAMERA_FACING_FRONT)
            flags |= FU_ADM_FLAG_FLIP_X;

        if (mNeedBenchmark) mFuCallStartTime = System.nanoTime();
        int fuTex = faceunity.fuDualInputToTexture(img, tex, flags, w, h, mFrameId++, mItemsArray);
        if (mNeedBenchmark) mOneHundredFrameFUTime += System.nanoTime() - mFuCallStartTime;
        return fuTex;
    }

    /**
     * 双输入接口(fuDualInputToTexture)，自定义画面数据需要回写到的byte[]
     *
     * @param img         NV21数据
     * @param tex         纹理ID
     * @param w
     * @param h
     * @param readBackImg 画面数据需要回写到的byte[]
     * @param readBackW
     * @param readBackH
     * @return
     */
    public int onDrawFrameDoubleInput(byte[] img, int tex, int w, int h, byte[] readBackImg, int readBackW, int readBackH) {
        if (tex <= 0 || img == null || w <= 0 || h <= 0 || readBackImg == null || readBackW <= 0 || readBackH <= 0) {
            Log.e(TAG, "onDrawFrame date null");
            return 0;
        }
        prepareDrawFrame();

        int flags = mInputTextureType | mInputImageFormat;
        if (mCurrentCameraType != Camera.CameraInfo.CAMERA_FACING_FRONT)
            flags |= FU_ADM_FLAG_FLIP_X;

        if (mNeedBenchmark) mFuCallStartTime = System.nanoTime();
        int fuTex = faceunity.fuDualInputToTexture(img, tex, flags, w, h, mFrameId++, mItemsArray,
                readBackW, readBackH, readBackImg);
        if (mNeedBenchmark) mOneHundredFrameFUTime += System.nanoTime() - mFuCallStartTime;
        return fuTex;
    }


    /**
     * 销毁faceunity相关的资源
     */
    public void onSurfaceDestroyed() {
        Log.e(TAG, "onSurfaceDestroyed");
        mFuItemHandler.removeCallbacksAndMessages(null);

        mFrameId = 0;
        isNeedUpdateFaceBeauty = true;
        mEventQueue.clear();
        Arrays.fill(mItemsArray, 0);
        faceunity.fuDestroyAllItems();
        faceunity.fuOnDeviceLost();
        faceunity.fuDone();
        if (mIsCreateEGLContext) faceunity.fuReleaseEGLContext();
    }

    /**
     * 每帧处理画面时被调用
     */
    private void prepareDrawFrame() {
        //计算FPS等数据
        benchmarkFPS();

        //获取人脸是否识别，并调用回调接口
        int isTracking = faceunity.fuIsTracking();
        if (mOnTrackingStatusChangedListener != null && mTrackingStatus != isTracking) {
            mOnTrackingStatusChangedListener.onTrackingStatusChanged(mTrackingStatus = isTracking);
        }

        //获取faceunity错误信息，并调用回调接口
        int error = faceunity.fuGetSystemError();
        if (mOnSystemErrorListener != null && error != 0) {
            String errorStr = faceunity.fuGetSystemErrorString(error);
            Log.e(TAG, "onSystemError: " + errorStr);
            mOnSystemErrorListener.onSystemError(errorStr);
        }

        //获取是否正在表情校准，并调用回调接口
        final float[] isCalibratingTmp = new float[1];
        faceunity.fuGetFaceInfo(0, "is_calibrating", isCalibratingTmp);
        if (mOnCalibratingListener != null && isCalibratingTmp[0] != mIsCalibrating) {
            mOnCalibratingListener.OnCalibrating(mIsCalibrating = isCalibratingTmp[0]);
        }

        //修改美颜参数
        if (isNeedUpdateFaceBeauty && mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX] != 0) {
            //filter_level 滤镜强度 范围0~1 SDK默认为 1
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "filter_level", mFaceBeautyFilterLevel);
            //filter_name 滤镜
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "filter_name", mFilterName.filterName());

            //heavy_blur 美肤类型 0:清晰美肤 1:朦胧美肤 SDK默认为 0
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "heavy_blur", mFaceBeautyType);
            //blur_level 磨皮 范围0~6 SDK默认为 6
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "blur_level", 6 * mFaceBeautyBlurLevel);
            //blur_blend_ratio 磨皮结果和原图融合率 范围0~1 SDK默认为 1
//          faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "blur_blend_ratio", 1);

            //color_level 美白 范围0~1 SDK默认为 1
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "color_level", mFaceBeautyColorLevel);
            //red_level 红润 范围0~1 SDK默认为 1
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "red_level", mFaceBeautyRedLevel);
            //eye_bright 亮眼 范围0~1 SDK默认为 0
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "eye_bright", mBrightEyesLevel);
            //tooth_whiten 美牙 范围0~1 SDK默认为 0
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "tooth_whiten", mBeautyTeethLevel);


            //face_shape_level 美型程度 范围0~1 SDK默认为1
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "face_shape_level", mFaceShapeLevel);
            //face_shape 脸型 0：女神 1：网红 2：自然 3：默认 SDK默认为 3
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "face_shape", mFaceBeautyFaceShape);
            //eye_enlarging 大眼 范围0~1 SDK默认为 0
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "eye_enlarging", mFaceBeautyEnlargeEye);
            //cheek_thinning 瘦脸 范围0~1 SDK默认为 0
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "cheek_thinning", mFaceBeautyCheekThin);
            //intensity_chin 下巴 范围0~1 SDK默认为 0.5    大于0.5变大，小于0.5变小
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "intensity_chin", mChinLevel);
            //intensity_forehead 额头 范围0~1 SDK默认为 0.5    大于0.5变大，小于0.5变小
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "intensity_forehead", mForeheadLevel);
            //intensity_nose 鼻子 范围0~1 SDK默认为 0
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "intensity_nose", mThinNoseLevel);
            //intensity_mouth 嘴型 范围0~1 SDK默认为 0.5   大于0.5变大，小于0.5变小
            faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "intensity_mouth", mMouthShape);
            isNeedUpdateFaceBeauty = false;
        }

        //queueEvent的Runnable在此处被调用
        while (!mEventQueue.isEmpty()) {
            mEventQueue.remove(0).run();
        }
    }

    //--------------------------------------对外可使用的接口----------------------------------------

    /**
     * 类似GLSurfaceView的queueEvent机制
     */
    public void queueEvent(Runnable r) {
        mEventQueue.add(r);
    }


    /**
     * 设置需要识别的人脸个数
     *
     * @param maxFaces
     */
    public void setMaxFaces(final int maxFaces) {
        if (mMaxFaces != maxFaces && maxFaces > 0) {
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    mMaxFaces = maxFaces;
                    faceunity.fuSetMaxFaces(maxFaces);
                }
            });
        }
    }

    /**
     * camera切换时需要调用
     *
     * @param currentCameraType     前后置摄像头ID
     * @param inputImageOrientation
     */
    public void onCameraChange(final int currentCameraType, final int inputImageOrientation) {
        this.onCameraChange(currentCameraType, inputImageOrientation, inputImageOrientation);
    }

    /**
     * camera切换时需要调用
     *
     * @param currentCameraType     前后置摄像头ID
     * @param inputImageOrientation
     */
    public void onCameraChange(final int currentCameraType, final int inputImageOrientation, final int inputProp) {
        if (mCurrentCameraType == currentCameraType && mInputImageOrientation == inputImageOrientation)
            return;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCurrentCameraType = currentCameraType;
                mInputImageOrientation = inputImageOrientation;
                mInputProp = inputProp;
                faceunity.fuOnCameraChange();
                updateEffectItemParams(mItemsArray[ITEM_ARRAYS_EFFECT]);
                faceunity.fuSetDefaultOrientation((360 - mInputImageOrientation) / 90);
                changeInputType();
            }
        });
    }

    public void setInputImageOrientation(int inputImageOrientation) {
        if (mInputImageOrientation != inputImageOrientation) {
            mInputImageOrientation = inputImageOrientation;
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    faceunity.fuSetDefaultOrientation((360 - mInputImageOrientation) / 90);
                }
            });
        }
    }

    /**
     * @param mCurrentCameraType 前后置摄像头ID
     */
    public void setCurrentCameraType(int mCurrentCameraType) {
        this.mCurrentCameraType = mCurrentCameraType;
    }

    /**
     * 音乐滤镜设置时间
     *
     * @param musicTime
     */
    public void onMusicFilterTime(final long musicTime) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_EFFECT], "music_time", musicTime);
            }
        });
    }

    /**
     * 本demo中切换输入接口时需要用到，用于解决绿屏问题。实际使用中无需使用。
     */
    public void changeInputType() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mItemsArray[ITEM_ARRAYS_EFFECT] > 0)
                    faceunity.fuDestroyItem(mItemsArray[ITEM_ARRAYS_EFFECT]);
                mFrameId = 0;
                if (mDefaultEffect != null)
                    mItemsArray[ITEM_ARRAYS_EFFECT] = loadItem(mDefaultEffect);
            }
        });
    }

    //--------------------------------------美颜参数与道具回调----------------------------------------

    @Override
    public void onEffectSelected(Effect effectItemName) {
        createItem(mDefaultEffect = effectItemName);
    }

    @Override
    public void onFilterLevelSelected(float progress) {
        isNeedUpdateFaceBeauty = true;
        mFaceBeautyFilterLevel = progress;
    }

    @Override
    public void onFilterSelected(Filter filterName) {
        isNeedUpdateFaceBeauty = true;
        this.mFilterName = filterName;
    }

    @Override
    public void onBeautySkinTypeSelected(float isAll) {
        isNeedUpdateFaceBeauty = true;
        mFaceBeautyType = isAll;
    }

    @Override
    public void onBlurLevelSelected(float level) {
        isNeedUpdateFaceBeauty = true;
        mFaceBeautyBlurLevel = level;
    }

    @Override
    public void onColorLevelSelected(float progress) {
        isNeedUpdateFaceBeauty = true;
        mFaceBeautyColorLevel = progress;
    }


    @Override
    public void onRedLevelSelected(float progress) {
        isNeedUpdateFaceBeauty = true;
        mFaceBeautyRedLevel = progress;
    }

    @Override
    public void onBrightEyesSelected(float progress) {
        isNeedUpdateFaceBeauty = true;
        mBrightEyesLevel = progress;
    }

    @Override
    public void onBeautyTeethSelected(float progress) {
        isNeedUpdateFaceBeauty = true;
        mBeautyTeethLevel = progress;
    }

    @Override
    public void onFaceShapeSelected(float faceShape) {
        isNeedUpdateFaceBeauty = true;
        this.mFaceBeautyFaceShape = faceShape;
    }

    @Override
    public void onEnlargeEyeSelected(float progress) {
        isNeedUpdateFaceBeauty = true;
        mFaceBeautyEnlargeEye = progress;
    }


    @Override
    public void onCheekThinSelected(float progress) {
        isNeedUpdateFaceBeauty = true;
        mFaceBeautyCheekThin = progress;
    }

    @Override
    public void onChinLevelSelected(float progress) {
        isNeedUpdateFaceBeauty = true;
        mChinLevel = progress;
    }

    @Override
    public void onForeheadLevelSelected(float progress) {
        isNeedUpdateFaceBeauty = true;
        mForeheadLevel = progress;
    }

    @Override
    public void onThinNoseLevelSelected(float progress) {
        isNeedUpdateFaceBeauty = true;
        mThinNoseLevel = progress;
    }

    @Override
    public void onMouthShapeSelected(float progress) {
        isNeedUpdateFaceBeauty = true;
        mMouthShape = progress;
    }

    //--------------------------------------IsTracking（人脸识别回调相关定义）----------------------------------------

    private int mTrackingStatus = 0;

    public interface OnTrackingStatusChangedListener {
        void onTrackingStatusChanged(int status);
    }

    private OnTrackingStatusChangedListener mOnTrackingStatusChangedListener;

    //--------------------------------------FaceUnitySystemError（faceunity错误信息回调相关定义）----------------------------------------

    public interface OnSystemErrorListener {
        void onSystemError(String error);
    }

    private OnSystemErrorListener mOnSystemErrorListener;

    //--------------------------------------mIsCalibrating（表情校准回调相关定义）----------------------------------------

    private float mIsCalibrating = 0;

    public interface OnCalibratingListener {
        void OnCalibrating(float isCalibrating);

    }

    private OnCalibratingListener mOnCalibratingListener;

    //--------------------------------------FPS（FPS相关定义）----------------------------------------

    private static final float NANO_IN_ONE_MILLI_SECOND = 1000000.0f;
    private static final float TIME = 5f;
    private int mCurrentFrameCnt = 0;
    private long mLastOneHundredFrameTimeStamp = 0;
    private long mOneHundredFrameFUTime = 0;
    private boolean mNeedBenchmark = false;
    private long mFuCallStartTime = 0;

    private OnFUDebugListener mOnFUDebugListener;

    public interface OnFUDebugListener {
        void onFpsChange(double fps, double renderTime);
    }

    private void benchmarkFPS() {
        if (!mNeedBenchmark) return;
        if (++mCurrentFrameCnt == TIME) {
            mCurrentFrameCnt = 0;
            long tmp = System.nanoTime();
            double fps = (1000.0f * NANO_IN_ONE_MILLI_SECOND / ((tmp - mLastOneHundredFrameTimeStamp) / TIME));
            mLastOneHundredFrameTimeStamp = tmp;
            double renderTime = mOneHundredFrameFUTime / TIME / NANO_IN_ONE_MILLI_SECOND;
            mOneHundredFrameFUTime = 0;

            if (mOnFUDebugListener != null) {
                mOnFUDebugListener.onFpsChange(fps, renderTime);
            }
        }
    }

    //--------------------------------------道具（异步加载道具）----------------------------------------

    public void createItem(Effect item) {
        if (item == null) return;
        mFuItemHandler.removeMessages(ITEM_ARRAYS_EFFECT);
        mFuItemHandler.sendMessage(Message.obtain(mFuItemHandler, ITEM_ARRAYS_EFFECT, item));
    }

    class FUItemHandler extends Handler {

        FUItemHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                //加载道具
                case ITEM_ARRAYS_EFFECT:
                    final Effect effect = (Effect) msg.obj;
                    final int newEffectItem = loadItem(effect);
                    queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            if (mItemsArray[ITEM_ARRAYS_EFFECT] > 0) {
                                faceunity.fuDestroyItem(mItemsArray[ITEM_ARRAYS_EFFECT]);
                            }
                            mItemsArray[ITEM_ARRAYS_EFFECT] = newEffectItem;
                            setMaxFaces(effect.maxFace());
                        }
                    });
                    break;
                //加载美颜bundle
                case ITEM_ARRAYS_FACE_BEAUTY_INDEX:
                    try {
                        InputStream beauty = mContext.getAssets().open(BUNDLE_face_beautification);
                        byte[] beautyData = new byte[beauty.available()];
                        beauty.read(beautyData);
                        beauty.close();
                        mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX] = faceunity.fuCreateItemFromPackage(beautyData);
                        isNeedUpdateFaceBeauty = true;
                        Log.e(TAG, "face beauty item handle " + mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                //加载animoji道具3D抗锯齿bundle
                case ITEM_ARRAYS_EFFECT_ABIMOJI_3D:
                    try {
                        InputStream animoji3D = mContext.getAssets().open(BUNDLE_animoji_3d);
                        byte[] animoji3DData = new byte[animoji3D.available()];
                        animoji3D.read(animoji3DData);
                        animoji3D.close();
                        mItemsArray[ITEM_ARRAYS_EFFECT_ABIMOJI_3D] = faceunity.fuCreateItemFromPackage(animoji3DData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    /**
     * fuCreateItemFromPackage 加载道具
     *
     * @param bundle（Effect本demo定义的道具实体类）
     * @return 大于0时加载成功
     */
    private int loadItem(Effect bundle) {
        int item = 0;
        try {
            if (bundle.effectType() == Effect.EFFECT_TYPE_NONE) {
                item = 0;
            } else {
                InputStream is = mContext.getAssets().open(bundle.path());
                byte[] itemData = new byte[is.available()];
                int len = is.read(itemData);
                Log.e(TAG, bundle.path() + " len " + len);
                is.close();
                item = faceunity.fuCreateItemFromPackage(itemData);
                updateEffectItemParams(item);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return item;
    }

    /**
     * 设置对道具设置相应的参数
     *
     * @param itemHandle
     */
    private void updateEffectItemParams(final int itemHandle) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                faceunity.fuItemSetParam(itemHandle, "isAndroid", 1.0);

                //rotationAngle 参数是用于旋转普通道具
                faceunity.fuItemSetParam(itemHandle, "rotationAngle", 360 - mInputProp);

                //这两句代码用于识别人脸默认方向的修改，主要针对animoji道具的切换摄像头倒置问题
                faceunity.fuItemSetParam(itemHandle, "camera_change", 1.0);
                faceunity.fuSetDefaultRotationMode((360 - mInputImageOrientation) / 90);
                //is3DFlipH 参数是用于对3D道具的镜像
                faceunity.fuItemSetParam(itemHandle, "is3DFlipH", mCurrentCameraType == Camera.CameraInfo.CAMERA_FACING_BACK ? 1 : 0);
                //isFlipExpr 参数是用于对人像驱动道具的镜像
                faceunity.fuItemSetParam(itemHandle, "isFlipExpr", mCurrentCameraType == Camera.CameraInfo.CAMERA_FACING_BACK ? 1 : 0);
                //loc_y_flip与loc_x_flip 参数是用于对手势识别道具的镜像
                faceunity.fuItemSetParam(itemHandle, "loc_y_flip", mCurrentCameraType == Camera.CameraInfo.CAMERA_FACING_BACK ? 1 : 0);
                faceunity.fuItemSetParam(itemHandle, "loc_x_flip", mCurrentCameraType == Camera.CameraInfo.CAMERA_FACING_BACK ? 1 : 0);
            }
        });
    }

    //--------------------------------------Builder----------------------------------------

    /**
     * FURenderer Builder
     */
    public static class Builder {

        private boolean createEGLContext = false;
        private Effect defaultEffect;
        private int maxFaces = 4;
        private Context context;
        private int inputTextureType = 0;
        private boolean needReadBackImage = false;
        private int inputImageFormat = 0;
        private int inputImageRotation = 90;
        private int inputProp = 90;
        private boolean isNeedAnimoji3D = false;
        private boolean isNeedFaceBeauty = true;

        private OnFUDebugListener onFUDebugListener;
        private OnTrackingStatusChangedListener onTrackingStatusChangedListener;
        private OnCalibratingListener onCalibratingListener;
        private OnSystemErrorListener onSystemErrorListener;

        public Builder(@NonNull Context context) {
            this.context = context;
        }

        public Builder createEGLContext(boolean createEGLContext) {
            this.createEGLContext = createEGLContext;
            return this;
        }

        public Builder defaultEffect(Effect defaultEffect) {
            this.defaultEffect = defaultEffect;
            return this;
        }

        public Builder maxFaces(int maxFaces) {
            this.maxFaces = maxFaces;
            return this;
        }

        public Builder inputTextureType(int textureType) {
            this.inputTextureType = textureType;
            return this;
        }

        public Builder needReadBackImage(boolean needReadBackImage) {
            this.needReadBackImage = needReadBackImage;
            return this;
        }

        public Builder inputImageFormat(int inputImageFormat) {
            this.inputImageFormat = inputImageFormat;
            return this;
        }

        public Builder inputImageOrientation(int inputImageRotation) {
            this.inputImageRotation = inputImageRotation;
            return this;
        }

        public Builder inputProp(int inputProp) {
            this.inputProp = inputProp;
            return this;
        }

        public Builder setNeedAnimoji3D(boolean needAnimoji3D) {
            this.isNeedAnimoji3D = needAnimoji3D;
            return this;
        }

        public Builder setNeedFaceBeauty(boolean needFaceBeauty) {
            isNeedFaceBeauty = needFaceBeauty;
            return this;
        }

        public Builder setOnFUDebugListener(OnFUDebugListener onFUDebugListener) {
            this.onFUDebugListener = onFUDebugListener;
            return this;
        }

        public Builder setOnTrackingStatusChangedListener(OnTrackingStatusChangedListener onTrackingStatusChangedListener) {
            this.onTrackingStatusChangedListener = onTrackingStatusChangedListener;
            return this;
        }

        public Builder setOnCalibratingListener(OnCalibratingListener onCalibratingListener) {
            this.onCalibratingListener = onCalibratingListener;
            return this;
        }

        public Builder setOnSystemErrorListener(OnSystemErrorListener onSystemErrorListener) {
            this.onSystemErrorListener = onSystemErrorListener;
            return this;
        }

        public FURenderer build() {
            FURenderer fuRenderer = new FURenderer(context, createEGLContext);
            fuRenderer.mMaxFaces = maxFaces;
            fuRenderer.mInputTextureType = inputTextureType;
            fuRenderer.mNeedReadBackImage = needReadBackImage;
            fuRenderer.mInputImageFormat = inputImageFormat;
            fuRenderer.mInputImageOrientation = inputImageRotation;
            fuRenderer.mInputProp = inputProp;
            fuRenderer.mDefaultEffect = defaultEffect;
            fuRenderer.isNeedAnimoji3D = isNeedAnimoji3D;
            fuRenderer.isNeedFaceBeauty = isNeedFaceBeauty;

            fuRenderer.mOnFUDebugListener = onFUDebugListener;
            fuRenderer.mOnTrackingStatusChangedListener = onTrackingStatusChangedListener;
            fuRenderer.mOnCalibratingListener = onCalibratingListener;
            fuRenderer.mOnSystemErrorListener = onSystemErrorListener;
            return fuRenderer;
        }

    }

    private static boolean isInit;

    public void loadItems() {
        if (!isInit) {
            isInit = true;
            initFURenderer(mContext);
        }

        onSurfaceCreated();
    }

    public void destroyItems() {
        onSurfaceDestroyed();
    }

}

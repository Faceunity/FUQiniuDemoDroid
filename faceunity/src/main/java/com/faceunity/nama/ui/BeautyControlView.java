package com.faceunity.nama.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.faceunity.nama.OnFaceUnityControlListener;
import com.faceunity.nama.R;
import com.faceunity.nama.entity.Effect;
import com.faceunity.nama.entity.EffectEnum;
import com.faceunity.nama.entity.Filter;
import com.faceunity.nama.entity.FilterEnum;
import com.faceunity.nama.ui.seekbar.DiscreteSeekBar;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tujh on 2017/8/15.
 */

public class BeautyControlView extends FrameLayout {
    private static final String TAG = BeautyControlView.class.getSimpleName();

    public static final float FINAL_CHANE = 1000;

    private Context mContext;

    private OnFaceUnityControlListener mOnFaceUnityControlListener;

    public void setOnFaceUnityControlListener(@NonNull OnFaceUnityControlListener onFaceUnityControlListener) {
        mOnFaceUnityControlListener = onFaceUnityControlListener;
    }

    private CheckGroup mBottomCheckGroup;
    private FrameLayout mBeautyMidLayout;

    private HorizontalScrollView mSkinBeautySelect;
    private BeautyBoxGroup mSkinBeautyBoxGroup;

    private HorizontalScrollView mFaceShapeSelect;
    private BeautyBoxGroup mFaceShapeBeautyBoxGroup;
    private BeautyBox mFaceShapeBox;
    private BeautyBox mChinLevelBox;
    private BeautyBox mForeheadLevelBox;
    private BeautyBox mThinNoseLevelBox;
    private BeautyBox mMouthShapeBox;

    private RecyclerView mEffectRecyclerView;
    private List<Effect> mEffects;

    private RecyclerView mFilterRecyclerView;
    private FilterRecyclerAdapter mFilterRecyclerAdapter;
    private List<Filter> mFilters;

    private FrameLayout mBeautySeekBarLayout;
    private DiscreteSeekBar mBeautySeekBar;
    private final List<Integer> FaceShapeIdList = Arrays.asList(R.id.face_shape_0_nvshen, R.id.face_shape_1_wanghong,
            R.id.face_shape_2_ziran, R.id.face_shape_3_default, R.id.face_shape_4);
    private RadioGroup mFaceShapeRadioGroup;

    private final Map<String, Float> mFilterNameLevelMap = new HashMap<>();

    private int mSkinDetect = 1;//肤色检测：开启
    private int mSkinType = 0;//磨皮类型：清晰磨皮
    private float mBlurLevel = 0.7f;//磨皮程度
    private float mColorLevel = 0.5f;//美白
    private float mRedLevel = 0.5f;//红润
    private float mBrightEyesLevel = 1000.7f;//亮眼
    private float mBeautyTeethLevel = 1000.7f;//美牙
    private int mBeautyShapeType = 4;//美型类型：精细变形
    private float mFaceBeautyEnlargeEye = 0.4f;//大眼
    private float mFaceBeautyCheekThin = 0.4f;//瘦脸
    private float mFaceBeautyEnlargeEyeOld = 0.4f;//大眼 old
    private float mFaceBeautyCheekThinOld = 0.4f;//瘦脸 old
    private float mChinLevel = 0.3f;//下巴
    private float mForeheadLevel = 0.3f;//额头
    private float mThinNoseLevel = 0.5f;//瘦鼻
    private float mMouthShapeLevel = 0.4f;//嘴形

    public BeautyControlView(Context context) {
        this(context, null);
    }

    public BeautyControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BeautyControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        LayoutInflater.from(context).inflate(R.layout.layout_beauty_control, this);

        initView();


        post(new Runnable() {
            @Override
            public void run() {
                updateViewSkinBeauty();
                updateViewFaceShape();
                mSkinBeautyBoxGroup.check(View.NO_ID);
                mFaceShapeBeautyBoxGroup.check(View.NO_ID);
            }
        });

        mEffects = EffectEnum.getEffects();
        mFilters = FilterEnum.getFilters();
    }

    private void initView() {
        initViewBottomRadio();

        mBeautyMidLayout = findViewById(R.id.beauty_mid_layout);
        initViewSkinBeauty();
        initViewFaceShape();
        initViewRecyclerView();

        initViewTop();
    }

    private void initViewBottomRadio() {
        mBottomCheckGroup = findViewById(R.id.beauty_radio_group);
        mBottomCheckGroup.setOnCheckedChangeListener(new CheckGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CheckGroup group, int checkedId) {
                clickViewBottomRadio(checkedId);
                changeBottomLayoutAnimator();
            }
        });
    }

    private void initViewSkinBeauty() {
        mSkinBeautySelect = findViewById(R.id.skin_beauty_select_block);

        mSkinBeautyBoxGroup = findViewById(R.id.beauty_box_skin_beauty);
        mSkinBeautyBoxGroup.setOnCheckedChangeListener(new BeautyBoxGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(BeautyBoxGroup group, int checkedId, boolean isChecked) {
                mFaceShapeRadioGroup.setVisibility(GONE);
                mBeautySeekBarLayout.setVisibility(GONE);
                if (checkedId == R.id.beauty_all_blur_box) {
                    mSkinDetect = isChecked ? 1 : 0;
                    setDescriptionShowStr(mSkinDetect == 0 ? "精准美肤 关闭" : "精准美肤 开启");
                    onChangeFaceBeautyLevel(checkedId, mSkinDetect);
                } else if (checkedId == R.id.beauty_type_box) {
                    mSkinType = isChecked ? 1 : 0;
                    setDescriptionShowStr(mSkinType == 0 ? "当前为 清晰磨皮 模式" : "当前为 朦胧磨皮 模式");
                    onChangeFaceBeautyLevel(checkedId, mSkinType);
                } else if (checkedId == R.id.beauty_blur_box) {
                    if (isChecked && mBlurLevel >= FINAL_CHANE) {
                        mBlurLevel -= FINAL_CHANE;
                        setDescriptionShowStr("磨皮 开启");
                    } else if (!isChecked && mBlurLevel < FINAL_CHANE) {
                        mBlurLevel += FINAL_CHANE;
                        setDescriptionShowStr("磨皮 关闭");
                    }
                    seekToSeekBar(mBlurLevel);
                    onChangeFaceBeautyLevel(checkedId, mBlurLevel);
                } else if (checkedId == R.id.beauty_color_box) {
                    if (isChecked && mColorLevel >= FINAL_CHANE) {
                        mColorLevel -= FINAL_CHANE;
                        setDescriptionShowStr("美白 开启");
                    } else if (!isChecked && mColorLevel < FINAL_CHANE) {
                        mColorLevel += FINAL_CHANE;
                        setDescriptionShowStr("美白 关闭");
                    }
                    seekToSeekBar(mColorLevel);
                    onChangeFaceBeautyLevel(checkedId, mColorLevel);
                } else if (checkedId == R.id.beauty_red_box) {
                    if (isChecked && mRedLevel >= FINAL_CHANE) {
                        mRedLevel -= FINAL_CHANE;
                        setDescriptionShowStr("红润 开启");
                    } else if (!isChecked && mRedLevel < FINAL_CHANE) {
                        mRedLevel += FINAL_CHANE;
                        setDescriptionShowStr("红润 关闭");
                    }
                    seekToSeekBar(mRedLevel);
                    onChangeFaceBeautyLevel(checkedId, mRedLevel);
                } else if (checkedId == R.id.beauty_bright_eyes_box) {
                    if (isChecked && mBrightEyesLevel >= FINAL_CHANE) {
                        mBrightEyesLevel -= FINAL_CHANE;
                        setDescriptionShowStr("亮眼 开启");
                    } else if (!isChecked && mBrightEyesLevel < FINAL_CHANE) {
                        mBrightEyesLevel += FINAL_CHANE;
                        setDescriptionShowStr("亮眼 关闭");
                    }
                    seekToSeekBar(mBrightEyesLevel);
                    onChangeFaceBeautyLevel(checkedId, mBrightEyesLevel);
                } else if (checkedId == R.id.beauty_teeth_box) {
                    if (isChecked && mBeautyTeethLevel >= FINAL_CHANE) {
                        mBeautyTeethLevel -= FINAL_CHANE;
                        setDescriptionShowStr("美牙 开启");
                    } else if (!isChecked && mBeautyTeethLevel < FINAL_CHANE) {
                        mBeautyTeethLevel += FINAL_CHANE;
                        setDescriptionShowStr("美牙 关闭");
                    }
                    seekToSeekBar(mBeautyTeethLevel);
                    onChangeFaceBeautyLevel(checkedId, mBeautyTeethLevel);
                }
                changeBottomLayoutAnimator();
            }
        });
    }

    private void updateViewSkinBeauty() {
        ((BeautyBox) findViewById(R.id.beauty_all_blur_box)).setChecked(mSkinDetect == 1);
        ((BeautyBox) findViewById(R.id.beauty_type_box)).setChecked(mSkinType == 1);
        ((BeautyBox) findViewById(R.id.beauty_blur_box)).setChecked(mBlurLevel < FINAL_CHANE);
        ((BeautyBox) findViewById(R.id.beauty_color_box)).setChecked(mColorLevel < FINAL_CHANE);
        ((BeautyBox) findViewById(R.id.beauty_red_box)).setChecked(mRedLevel < FINAL_CHANE);
        ((BeautyBox) findViewById(R.id.beauty_bright_eyes_box)).setChecked(mBrightEyesLevel < FINAL_CHANE);
        ((BeautyBox) findViewById(R.id.beauty_teeth_box)).setChecked(mBeautyTeethLevel < FINAL_CHANE);
    }

    private void initViewFaceShape() {
        mFaceShapeSelect = findViewById(R.id.face_shape_select_block);
        mFaceShapeBeautyBoxGroup = findViewById(R.id.beauty_box_face_shape);
        mFaceShapeBeautyBoxGroup.setOnCheckedChangeListener(new BeautyBoxGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(BeautyBoxGroup group, int checkedId, boolean isChecked) {
                mFaceShapeRadioGroup.setVisibility(GONE);
                mBeautySeekBarLayout.setVisibility(GONE);
                if (checkedId == R.id.face_shape_box) {
                    mFaceShapeRadioGroup.setVisibility(VISIBLE);
                } else if (checkedId == R.id.enlarge_eye_level_box) {
                    if (mBeautyShapeType == 4) {
                        if (isChecked && mFaceBeautyEnlargeEye >= FINAL_CHANE) {
                            mFaceBeautyEnlargeEye -= FINAL_CHANE;
                            setDescriptionShowStr("大眼 开启");
                        } else if (!isChecked && mFaceBeautyEnlargeEye < FINAL_CHANE) {
                            mFaceBeautyEnlargeEye += FINAL_CHANE;
                            setDescriptionShowStr("大眼 关闭");
                        }
                        seekToSeekBar(mFaceBeautyEnlargeEye);
                        onChangeFaceBeautyLevel(checkedId, mFaceBeautyEnlargeEye);
                    } else {
                        if (isChecked && mFaceBeautyEnlargeEyeOld >= FINAL_CHANE) {
                            mFaceBeautyEnlargeEyeOld -= FINAL_CHANE;
                            setDescriptionShowStr("大眼 开启");
                        } else if (!isChecked && mFaceBeautyEnlargeEyeOld < FINAL_CHANE) {
                            mFaceBeautyEnlargeEyeOld += FINAL_CHANE;
                            setDescriptionShowStr("大眼 关闭");
                        }
                        seekToSeekBar(mFaceBeautyEnlargeEyeOld);
                        onChangeFaceBeautyLevel(checkedId, mFaceBeautyEnlargeEyeOld);
                    }
                } else if (checkedId == R.id.cheek_thin_level_box) {
                    if (mBeautyShapeType == 4) {
                        if (isChecked && mFaceBeautyCheekThin >= FINAL_CHANE) {
                            mFaceBeautyCheekThin -= FINAL_CHANE;
                            setDescriptionShowStr("瘦脸 开启");
                        } else if (!isChecked && mFaceBeautyCheekThin < FINAL_CHANE) {
                            mFaceBeautyCheekThin += FINAL_CHANE;
                            setDescriptionShowStr("瘦脸 关闭");
                        }
                        seekToSeekBar(mFaceBeautyCheekThin);
                        onChangeFaceBeautyLevel(checkedId, mFaceBeautyCheekThin);
                    } else {
                        if (isChecked && mFaceBeautyCheekThinOld >= FINAL_CHANE) {
                            mFaceBeautyCheekThinOld -= FINAL_CHANE;
                            setDescriptionShowStr("瘦脸 开启");
                        } else if (!isChecked && mFaceBeautyCheekThinOld < FINAL_CHANE) {
                            mFaceBeautyCheekThinOld += FINAL_CHANE;
                            setDescriptionShowStr("瘦脸 关闭");
                        }
                        seekToSeekBar(mFaceBeautyCheekThinOld);
                        onChangeFaceBeautyLevel(checkedId, mFaceBeautyCheekThinOld);
                    }
                } else if (checkedId == R.id.chin_level_box) {
                    if (isChecked && mChinLevel >= FINAL_CHANE) {
                        mChinLevel -= FINAL_CHANE;
                        setDescriptionShowStr("下巴 开启");
                    } else if (!isChecked && mChinLevel < FINAL_CHANE) {
                        mChinLevel += FINAL_CHANE;
                        setDescriptionShowStr("下巴 关闭");
                    }
                    seekToSeekBar(mChinLevel, -50, 50);
                    onChangeFaceBeautyLevel(checkedId, mChinLevel);
                } else if (checkedId == R.id.forehead_level_box) {
                    if (isChecked && mForeheadLevel >= FINAL_CHANE) {
                        mForeheadLevel -= FINAL_CHANE;
                        setDescriptionShowStr("额头 开启");
                    } else if (!isChecked && mForeheadLevel < FINAL_CHANE) {
                        mForeheadLevel += FINAL_CHANE;
                        setDescriptionShowStr("额头 关闭");
                    }
                    seekToSeekBar(mForeheadLevel, -50, 50);
                    onChangeFaceBeautyLevel(checkedId, mForeheadLevel);
                } else if (checkedId == R.id.thin_nose_level_box) {
                    if (isChecked && mThinNoseLevel >= FINAL_CHANE) {
                        mThinNoseLevel -= FINAL_CHANE;
                        setDescriptionShowStr("瘦鼻 开启");
                    } else if (!isChecked && mThinNoseLevel < FINAL_CHANE) {
                        mThinNoseLevel += FINAL_CHANE;
                        setDescriptionShowStr("瘦鼻 关闭");
                    }
                    seekToSeekBar(mThinNoseLevel);
                    onChangeFaceBeautyLevel(checkedId, mThinNoseLevel);
                } else if (checkedId == R.id.mouth_shape_box) {
                    if (isChecked && mMouthShapeLevel >= FINAL_CHANE) {
                        mMouthShapeLevel -= FINAL_CHANE;
                        setDescriptionShowStr("嘴形 开启");
                    } else if (!isChecked && mMouthShapeLevel < FINAL_CHANE) {
                        mMouthShapeLevel += FINAL_CHANE;
                        setDescriptionShowStr("嘴形 关闭");
                    }
                    seekToSeekBar(mMouthShapeLevel, -50, 50);
                    onChangeFaceBeautyLevel(checkedId, mMouthShapeLevel);
                }
                changeBottomLayoutAnimator();
            }
        });
        mFaceShapeBox = findViewById(R.id.face_shape_box);
        mChinLevelBox = findViewById(R.id.chin_level_box);
        mForeheadLevelBox = findViewById(R.id.forehead_level_box);
        mThinNoseLevelBox = findViewById(R.id.thin_nose_level_box);
        mMouthShapeBox = findViewById(R.id.mouth_shape_box);
    }

    private void updateViewFaceShape() {
        if (mBeautyShapeType == 4) {
            ((BeautyBox) findViewById(R.id.enlarge_eye_level_box)).setChecked(mFaceBeautyEnlargeEye < FINAL_CHANE);
            ((BeautyBox) findViewById(R.id.cheek_thin_level_box)).setChecked(mFaceBeautyCheekThin < FINAL_CHANE);
        } else {
            ((BeautyBox) findViewById(R.id.enlarge_eye_level_box)).setChecked(mFaceBeautyEnlargeEyeOld < FINAL_CHANE);
            ((BeautyBox) findViewById(R.id.cheek_thin_level_box)).setChecked(mFaceBeautyCheekThinOld < FINAL_CHANE);
        }
        ((BeautyBox) findViewById(R.id.chin_level_box)).setChecked(mChinLevel < FINAL_CHANE);
        ((BeautyBox) findViewById(R.id.forehead_level_box)).setChecked(mForeheadLevel < FINAL_CHANE);
        ((BeautyBox) findViewById(R.id.thin_nose_level_box)).setChecked(mThinNoseLevel < FINAL_CHANE);
        ((BeautyBox) findViewById(R.id.mouth_shape_box)).setChecked(mMouthShapeLevel < FINAL_CHANE);

        if (mBeautyShapeType != 4) {
            mFaceShapeRadioGroup.check(FaceShapeIdList.get(mBeautyShapeType));
            mChinLevelBox.setVisibility(GONE);
            mForeheadLevelBox.setVisibility(GONE);
            mThinNoseLevelBox.setVisibility(GONE);
            mMouthShapeBox.setVisibility(GONE);
        } else {
            mFaceShapeRadioGroup.check(R.id.face_shape_4);
            mChinLevelBox.setVisibility(VISIBLE);
            mForeheadLevelBox.setVisibility(VISIBLE);
            mThinNoseLevelBox.setVisibility(VISIBLE);
            mMouthShapeBox.setVisibility(VISIBLE);
        }
    }

    private void initViewRecyclerView() {
        mEffectRecyclerView = findViewById(R.id.effect_recycle_view);
        mEffectRecyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
        mEffectRecyclerView.setAdapter(new EffectRecyclerAdapter());
        mFilterRecyclerView = findViewById(R.id.filter_recycle_view);
        mFilterRecyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
        mFilterRecyclerAdapter = new FilterRecyclerAdapter();
        mFilterRecyclerView.setAdapter(mFilterRecyclerAdapter);
    }

    private void initViewTop() {
        mFaceShapeRadioGroup = findViewById(R.id.face_shape_radio_group);
        mFaceShapeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.face_shape_4) {
                    mChinLevelBox.setVisibility(VISIBLE);
                    mForeheadLevelBox.setVisibility(VISIBLE);
                    mThinNoseLevelBox.setVisibility(VISIBLE);
                    mMouthShapeBox.setVisibility(VISIBLE);
                } else {
                    mChinLevelBox.setVisibility(GONE);
                    mForeheadLevelBox.setVisibility(GONE);
                    mThinNoseLevelBox.setVisibility(GONE);
                    mMouthShapeBox.setVisibility(GONE);
                }

                mBeautyShapeType = FaceShapeIdList.indexOf(checkedId);
                if (mOnFaceUnityControlListener != null) {
                    mOnFaceUnityControlListener.onFaceShapeTypeSelected(mBeautyShapeType);
                }
                float enlargeEye, CheekThin;
                if (mBeautyShapeType == 4) {
                    enlargeEye = mFaceBeautyEnlargeEye;
                    CheekThin = mFaceBeautyCheekThin;
                } else {
                    enlargeEye = mFaceBeautyEnlargeEyeOld;
                    CheekThin = mFaceBeautyCheekThinOld;
                }
                onChangeFaceBeautyLevel(R.id.enlarge_eye_level_box, enlargeEye);
                onChangeFaceBeautyLevel(R.id.cheek_thin_level_box, CheekThin);
                ((BeautyBox) findViewById(R.id.enlarge_eye_level_box)).setChecked(enlargeEye < FINAL_CHANE);
                ((BeautyBox) findViewById(R.id.cheek_thin_level_box)).setChecked(CheekThin < FINAL_CHANE);
                mFaceShapeBox.setChecked(checkedId != R.id.face_shape_3_default);
            }
        });

        mBeautySeekBarLayout = findViewById(R.id.beauty_seek_bar_layout);
        mBeautySeekBar = findViewById(R.id.beauty_seek_bar);
        mBeautySeekBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar SeekBar, int value, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                float valueF = (float) (value - SeekBar.getMin()) / 100;
                if (mBottomCheckGroup.getCheckedCheckBoxId() == R.id.beauty_radio_skin_beauty) {
                    onChangeFaceBeautyLevel(mSkinBeautyBoxGroup.getCheckedBeautyBoxId(), valueF);
                } else if (mBottomCheckGroup.getCheckedCheckBoxId() == R.id.beauty_radio_face_shape) {
                    onChangeFaceBeautyLevel(mFaceShapeBeautyBoxGroup.getCheckedBeautyBoxId(), valueF);
                } else if (mBottomCheckGroup.getCheckedCheckBoxId() == R.id.beauty_radio_filter) {
                    mFilterRecyclerAdapter.setFilterLevels(valueF);
                    if (mOnFaceUnityControlListener != null) {
                        mOnFaceUnityControlListener.onFilterLevelSelected(valueF);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar SeekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar SeekBar) {

            }
        });
    }

    private void updateTopView(int viewId) {
        mFaceShapeRadioGroup.setVisibility(GONE);
        mBeautySeekBarLayout.setVisibility(GONE);
        if (viewId == R.id.beauty_blur_box) {
            seekToSeekBar(mBlurLevel);
        } else if (viewId == R.id.beauty_color_box) {
            seekToSeekBar(mColorLevel);
        } else if (viewId == R.id.beauty_red_box) {
            seekToSeekBar(mRedLevel);
        } else if (viewId == R.id.beauty_bright_eyes_box) {
            seekToSeekBar(mBrightEyesLevel);
        } else if (viewId == R.id.beauty_teeth_box) {
            seekToSeekBar(mBeautyTeethLevel);
        } else if (viewId == R.id.face_shape_box) {
            mFaceShapeRadioGroup.setVisibility(VISIBLE);
        } else if (viewId == R.id.enlarge_eye_level_box) {
            seekToSeekBar(mBeautyShapeType == 4 ? mFaceBeautyEnlargeEye : mFaceBeautyEnlargeEyeOld);
        } else if (viewId == R.id.cheek_thin_level_box) {
            seekToSeekBar(mBeautyShapeType == 4 ? mFaceBeautyCheekThin : mFaceBeautyCheekThinOld);
        } else if (viewId == R.id.chin_level_box) {
            seekToSeekBar(mChinLevel, -50, 50);
        } else if (viewId == R.id.forehead_level_box) {
            seekToSeekBar(mForeheadLevel, -50, 50);
        } else if (viewId == R.id.thin_nose_level_box) {
            seekToSeekBar(mThinNoseLevel);
        } else if (viewId == R.id.mouth_shape_box) {
            seekToSeekBar(mMouthShapeLevel, -50, 50);
        }
        changeBottomLayoutAnimator();
    }

    private void onChangeFaceBeautyLevel(int viewId, float value) {
        boolean isClose = value >= 1000;
        if (viewId == R.id.beauty_all_blur_box) {
            if (mOnFaceUnityControlListener != null) {
                mOnFaceUnityControlListener.onSkinDetectSelected((int) value);
            }
        } else if (viewId == R.id.beauty_type_box) {
            if (mOnFaceUnityControlListener != null) {
                mOnFaceUnityControlListener.onSkinTypeSelected((int) value);
            }
        } else if (viewId == R.id.beauty_blur_box) {
            mBlurLevel = value;
            if (mOnFaceUnityControlListener != null) {
                mOnFaceUnityControlListener.onBlurLevelSelected(isClose ? 0 : mBlurLevel);
            }
        } else if (viewId == R.id.beauty_color_box) {
            mColorLevel = value;
            if (mOnFaceUnityControlListener != null) {
                mOnFaceUnityControlListener.onColorLevelSelected(isClose ? 0 : mColorLevel);
            }
        } else if (viewId == R.id.beauty_red_box) {
            mRedLevel = value;
            if (mOnFaceUnityControlListener != null) {
                mOnFaceUnityControlListener.onRedLevelSelected(isClose ? 0 : mRedLevel);
            }
        } else if (viewId == R.id.beauty_bright_eyes_box) {
            mBrightEyesLevel = value;
            if (mOnFaceUnityControlListener != null) {
                mOnFaceUnityControlListener.onBrightEyesSelected(isClose ? 0 : mBrightEyesLevel);
            }
        } else if (viewId == R.id.beauty_teeth_box) {
            mBeautyTeethLevel = value;
            if (mOnFaceUnityControlListener != null) {
                mOnFaceUnityControlListener.onBeautyTeethSelected(isClose ? 0 : mBeautyTeethLevel);
            }
        } else if (viewId == R.id.enlarge_eye_level_box) {
            if (mBeautyShapeType == 4) {
                mFaceBeautyEnlargeEye = value;
                if (mOnFaceUnityControlListener != null) {
                    mOnFaceUnityControlListener.onEnlargeEyeSelected(isClose ? 0 : mFaceBeautyEnlargeEye);
                }
            } else {
                mFaceBeautyEnlargeEyeOld = value;
                if (mOnFaceUnityControlListener != null) {
                    mOnFaceUnityControlListener.onEnlargeEyeSelected(isClose ? 0 : mFaceBeautyEnlargeEyeOld);
                }
            }
        } else if (viewId == R.id.cheek_thin_level_box) {
            if (mBeautyShapeType == 4) {
                mFaceBeautyCheekThin = value;
                if (mOnFaceUnityControlListener != null) {
                    mOnFaceUnityControlListener.onCheekThinSelected(isClose ? 0 : mFaceBeautyCheekThin);
                }
            } else {
                mFaceBeautyCheekThinOld = value;
                if (mOnFaceUnityControlListener != null) {
                    mOnFaceUnityControlListener.onCheekThinSelected(isClose ? 0 : mFaceBeautyCheekThinOld);
                }
            }
        } else if (viewId == R.id.chin_level_box) {
            mChinLevel = value;
            if (mOnFaceUnityControlListener != null) {
                mOnFaceUnityControlListener.onChinLevelSelected(isClose ? 0.5f : mChinLevel);
            }
        } else if (viewId == R.id.forehead_level_box) {
            mForeheadLevel = value;
            if (mOnFaceUnityControlListener != null) {
                mOnFaceUnityControlListener.onForeheadLevelSelected(isClose ? 0.5f : mForeheadLevel);
            }
        } else if (viewId == R.id.thin_nose_level_box) {
            mThinNoseLevel = value;
            if (mOnFaceUnityControlListener != null) {
                mOnFaceUnityControlListener.onThinNoseLevelSelected(isClose ? 0 : mThinNoseLevel);
            }
        } else if (viewId == R.id.mouth_shape_box) {
            mMouthShapeLevel = value;
            if (mOnFaceUnityControlListener != null) {
                mOnFaceUnityControlListener.onMouthShapeSelected(isClose ? 0.5f : mMouthShapeLevel);
            }
        }
    }

    private void clickViewBottomRadio(int viewId) {
        mBeautyMidLayout.setVisibility(GONE);
        mEffectRecyclerView.setVisibility(GONE);
        mSkinBeautySelect.setVisibility(GONE);
        mFaceShapeSelect.setVisibility(GONE);
        mFilterRecyclerView.setVisibility(GONE);

        mFaceShapeRadioGroup.setVisibility(GONE);
        mBeautySeekBarLayout.setVisibility(GONE);
        if (viewId == R.id.beauty_radio_effect) {
            mBeautyMidLayout.setVisibility(VISIBLE);
            mEffectRecyclerView.setVisibility(VISIBLE);
        } else if (viewId == R.id.beauty_radio_skin_beauty) {
            mBeautyMidLayout.setVisibility(VISIBLE);
            mSkinBeautySelect.setVisibility(VISIBLE);
            updateTopView(mSkinBeautyBoxGroup.getCheckedBeautyBoxId());
        } else if (viewId == R.id.beauty_radio_face_shape) {
            mBeautyMidLayout.setVisibility(VISIBLE);
            mFaceShapeSelect.setVisibility(VISIBLE);
            updateTopView(mFaceShapeBeautyBoxGroup.getCheckedBeautyBoxId());
        } else if (viewId == R.id.beauty_radio_filter) {
            mBeautyMidLayout.setVisibility(VISIBLE);
            mFilterRecyclerView.setVisibility(VISIBLE);
            mFilterRecyclerAdapter.setFilterProgress();
        }
    }

    private void seekToSeekBar(float value) {
        seekToSeekBar(value, 0, 100);
    }

    private void seekToSeekBar(float value, int min, int max) {
        if (value < FINAL_CHANE) {
            mBeautySeekBarLayout.setVisibility(VISIBLE);
            mBeautySeekBar.setMin(min);
            mBeautySeekBar.setMax(max);
            mBeautySeekBar.setProgress((int) (value * (max - min) + min));
        }
    }

    private class EffectRecyclerAdapter extends RecyclerView.Adapter<EffectRecyclerAdapter.EffectRecyclerHolder> {
        private int mSelectedEffectPosition = -1;

        @Override
        public EffectRecyclerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.layout_effect_recycler, parent, false);
            return new EffectRecyclerHolder(view);
        }

        @Override
        public void onBindViewHolder(EffectRecyclerHolder holder, final int position) {
            int iconId = mEffects.get(position).getIconId();
            holder.effectImg.setImageResource(iconId);
            int selResId = mSelectedEffectPosition == position ? R.drawable.effect_select : android.R.color.transparent;
            holder.effectImg.setBackgroundResource(selResId);
            holder.effectImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSelectedEffectPosition == position) {
                        return;
                    }
                    mSelectedEffectPosition = position;
                    Effect effect = mEffects.get(position);
                    if (mOnFaceUnityControlListener != null) {
                        mOnFaceUnityControlListener.onEffectSelected(effect);
                    }
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mEffects.size();
        }

        class EffectRecyclerHolder extends RecyclerView.ViewHolder {
            CircleImageView effectImg;

            EffectRecyclerHolder(View itemView) {
                super(itemView);
                effectImg = itemView.findViewById(R.id.effect_recycler_img);
            }
        }
    }

    public float getFaceBeautyFilterLevel(String filterName) {
        Float level;
        if (mFilterNameLevelMap.containsKey(filterName)) {
            level = mFilterNameLevelMap.get(filterName);
        } else {
            level = 1.0f;
        }
        setFaceBeautyFilterLevel(filterName, level);
        return level;
    }

    public void setFaceBeautyFilterLevel(String filterName, float filterLevel) {
        mFilterNameLevelMap.put(filterName, filterLevel);
        if (mOnFaceUnityControlListener != null) {
            mOnFaceUnityControlListener.onFilterLevelSelected(filterLevel);
        }
    }


    private class FilterRecyclerAdapter extends RecyclerView.Adapter<FilterRecyclerAdapter.FilterRecyclerHolder> {
        private int mSelectedFilterPosition = 6;

        @Override
        public FilterRecyclerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.layout_beauty_control_recycler, parent, false);
            return new FilterRecyclerHolder(view);
        }

        @Override
        public void onBindViewHolder(FilterRecyclerHolder holder, final int position) {
            int iconId = mFilters.get(position).getIconId();
            holder.filterImg.setBackgroundResource(iconId);
            String desc = mFilters.get(position).getDescription();
            holder.filterName.setText(desc);
            int selResId = mSelectedFilterPosition == position ? R.drawable.control_filter_select : android.R.color.transparent;
            holder.filterImg.setImageResource(selResId);
            holder.itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSelectedFilterPosition = position;
                    setFilterProgress();
                    mBeautySeekBarLayout.setVisibility(VISIBLE);
                    changeBottomLayoutAnimator();
                    notifyDataSetChanged();
                    if (mOnFaceUnityControlListener != null) {
                        String name = mFilters.get(mSelectedFilterPosition).getName();
                        mOnFaceUnityControlListener.onFilterNameSelected(name);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mFilters.size();
        }

        public void setFilterLevels(float level) {
            String name = mFilters.get(mSelectedFilterPosition).getName();
            setFaceBeautyFilterLevel(name, level);
        }

        public void setFilterProgress() {
            String name = mFilters.get(mSelectedFilterPosition).getName();
            seekToSeekBar(getFaceBeautyFilterLevel(name));
        }

        class FilterRecyclerHolder extends RecyclerView.ViewHolder {
            ImageView filterImg;
            TextView filterName;

            FilterRecyclerHolder(View itemView) {
                super(itemView);
                filterImg = itemView.findViewById(R.id.control_recycler_img);
                filterName = itemView.findViewById(R.id.control_recycler_text);
            }
        }
    }

    private ValueAnimator mBottomLayoutAnimator;

    private void changeBottomLayoutAnimator() {
        if (mBottomLayoutAnimator != null && mBottomLayoutAnimator.isRunning()) {
            mBottomLayoutAnimator.end();
        }
        final int startHeight = getHeight();
        measure(0, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        final int endHeight = getMeasuredHeight();
        if (startHeight == endHeight) {
            return;
        }
        mBottomLayoutAnimator = ValueAnimator.ofInt(startHeight, endHeight).setDuration(50);
        mBottomLayoutAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int height = (int) animation.getAnimatedValue();
                ViewGroup.LayoutParams params = getLayoutParams();
                params.height = height;
                setLayoutParams(params);
            }
        });
        mBottomLayoutAnimator.start();
    }

    private void setDescriptionShowStr(String str) {
        Toast.makeText(mContext, str, Toast.LENGTH_SHORT).show();
    }

}
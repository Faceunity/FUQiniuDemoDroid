package com.faceunity.nama;

import com.faceunity.nama.entity.Effect;

/**
 * FURenderer与界面之间的交互接口
 *
 * @author Richie on 2019.07.18
 */
public interface OnFaceUnityControlListener {
    /**
     * 选择道具贴纸
     *
     * @param effect 道具贴纸
     */
    void onEffectSelected(Effect effect);

    /**
     * 设置滤镜名称
     *
     * @param name 滤镜名称
     */
    void onFilterNameSelected(String name);

    /**
     * 调节滤镜强度
     *
     * @param level 滤镜程度
     */
    void onFilterLevelSelected(float level);

    /**
     * 精准磨皮
     *
     * @param isOn 0:关闭, 1:开启
     */
    void onSkinDetectSelected(int isOn);

    /**
     * 设置美肤类型
     *
     * @param skinType 0:清晰美肤, 1:朦胧美肤
     */
    void onSkinTypeSelected(int skinType);

    /**
     * 调节磨皮
     *
     * @param level 磨皮程度
     */
    void onBlurLevelSelected(float level);

    /**
     * 调节美白
     *
     * @param level 美白程度
     */
    void onColorLevelSelected(float level);

    /**
     * 调节红润
     *
     * @param level 红润程度
     */
    void onRedLevelSelected(float level);

    /**
     * 调节亮眼
     *
     * @param level 亮眼程度
     */
    void onBrightEyesSelected(float level);

    /**
     * 调节美牙
     *
     * @param level 美牙程度
     */
    void onBeautyTeethSelected(float level);

    /**
     * 选择脸型
     *
     * @param faceShape 0:女神, 1:网红, 2:自然, 3:默认, 4:精细变形
     */
    void onFaceShapeTypeSelected(int faceShape);

    /**
     * 调节大眼
     *
     * @param level 大眼程度
     */
    void onEnlargeEyeSelected(float level);

    /**
     * 调节瘦脸
     *
     * @param level 瘦脸程度
     */
    void onCheekThinSelected(float level);

    /**
     * 调节下巴
     *
     * @param level 下巴程度
     */
    void onChinLevelSelected(float level);

    /**
     * 调节额头
     *
     * @param level 额头程度
     */
    void onForeheadLevelSelected(float level);

    /**
     * 调节瘦鼻
     *
     * @param level 瘦鼻程度
     */
    void onThinNoseLevelSelected(float level);

    /**
     * 调节嘴形
     *
     * @param level 嘴形程度
     */
    void onMouthShapeSelected(float level);
}

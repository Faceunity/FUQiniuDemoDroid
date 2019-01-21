# FUQiniuDemoDroid

FUQiniuDemoDroid 是集成了 Faceunity 面部跟踪和虚拟道具功能和**[七牛直播推流](https://developer.qiniu.com/pili/sdk/3722/PLDroidMediaStreaming-historical-record)** SDK 的 Demo 。
本文是 FaceUnity SDK 快速对接牛短视频 SDK 的导读说明，关于 FaceUnity SDK 的更多详细说明，请参看 [FULiveDemo](https://github.com/Faceunity/FULiveDemoDroid/tree/dev).

## 快速集成方法
### 添加module
添加faceunity module到工程中，在app dependencies里添加compile project(':faceunity')
### 修改代码
#### 初始化与监听回调
在AVStreamingActivity的
initStreamingManager方法中添加
```
mFURenderer = new FURenderer.Builder(this).inputTextureType(faceunity.FU_ADM_FLAG_EXTERNAL_OES_TEXTURE).build();

mMediaStreamingManager.setSurfaceTextureCallback(new SurfaceTextureCallback() {

  @Override
  public void onSurfaceCreated() {
      //初始化并加载美颜道具、默认道具
      mFURenderer.loadItems(mCurrentCamFacingIndex == CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT.ordinal() ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK);
  }

  @Override
  public void onSurfaceChanged(int width, int height) {
  }

  @Override
  public void onSurfaceDestroyed() {
      //销毁道具
      mFURenderer.destroyItems();
  }

  @Override
  public int onDrawFrame(int texId, int width, int height, float[] floats) {
      //渲染道具到原始数据上
      return mFURenderer.onDrawFrame(texId, width, height, floats);
  }
});

mMediaStreamingManager.setStreamingPreviewCallback(new StreamingPreviewCallback() {
    @Override
    public boolean onPreviewFrame(byte[] data, int width, int height, int rotation, int fmt, long tsInNanoTime) {
        //获取camera数据用于人脸追踪
        return mFURenderer.onPreviewFrame(data, width, height, rotation, fmt, tsInNanoTime);
    }
});
```
### 修改默认美颜参数
修改faceunity中faceunity中以下代码
```
private float mFaceBeautyALLBlurLevel = 1.0f;//精准磨皮
private float mFaceBeautyType = 0.0f;//美肤类型
private float mFaceBeautyBlurLevel = 0.7f;//磨皮
private float mFaceBeautyColorLevel = 0.5f;//美白
private float mFaceBeautyRedLevel = 0.5f;//红润
private float mBrightEyesLevel = 1000.7f;//亮眼
private float mBeautyTeethLevel = 1000.7f;//美牙

private float mFaceBeautyFaceShape = 4.0f;//脸型
private float mFaceBeautyEnlargeEye = 0.4f;//大眼
private float mFaceBeautyCheekThin = 0.4f;//瘦脸
private float mFaceBeautyEnlargeEye_old = 0.4f;//大眼
private float mFaceBeautyCheekThin_old = 0.4f;//瘦脸
private float mChinLevel = 0.3f;//下巴
private float mForeheadLevel = 0.3f;//额头
private float mThinNoseLevel = 0.5f;//瘦鼻
private float mMouthShape = 0.4f;//嘴形
```
参数含义与取值范围参考[这里](http://www.faceunity.com/technical/android-beauty.html)，如果使用界面，则需要同时修改界面中的初始值。
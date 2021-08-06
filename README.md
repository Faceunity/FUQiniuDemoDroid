## FUQiniuDemoDroid 快速接入文档

FUQiniuDemoDroid 是集成了 Faceunity 面部跟踪和虚拟道具功能 和 **[七牛直播推流](https://developer.qiniu.com/pili/sdk/3715/PLDroidMediaStreaming-overview)** 的 Demo。

本文是 FaceUnity Nama SDK 快速对 七牛直播推流 SDK 的导读说明，SDK 版本为 **7.4.1.0**。关于 SDK 的详细说明，请参看 **[FULiveDemoDroid](https://github.com/Faceunity/FULiveDemoDroid/tree/master/doc)**。

## 快速集成方法

### 一、添加 SDK

### 1. build.gradle配置

#### 1.1 allprojects配置
```java
allprojects {
    repositories {
        ...
        maven { url 'http://maven.faceunity.com/repository/maven-public/' }
        ...
  }
}
```

#### 1.2 dependencies导入依赖
```java
dependencies {
...
implementation 'com.faceunity:core:7.4.1.0' // 实现代码
implementation 'com.faceunity:model:7.4.1.0' // 道具以及AI bundle
...
}
```

##### 备注

集成参考文档：FULiveDemoDroid 工程 doc目录

### 2. 其他接入方式-底层库依赖

```java
dependencies {
...
implementation 'com.faceunity:nama:7.4.0' //底层库-标准版
implementation 'com.faceunity:nama-lite:7.4.0' //底层库-lite版
...
}
```

  如需指定应用的 so 架构，请修改 app 模块 build.gradle：

  ```groovy
  android {
      // ...
      defaultConfig {
          // ...
          ndk {
              abiFilters 'armeabi-v7a', 'arm64-v8a'
          }
      }
  }
  ```

  如需剔除不必要的 assets 文件，请修改 app 模块 build.gradle：

  ```groovy
  android {
      // ...
      applicationVariants.all { variant ->
          variant.mergeAssetsProvider.configure {
              doLast {
                  delete(fileTree(dir: outputDir, includes: ['model/ai_face_processor_lite.bundle',
                                                             'model/ai_hand_processor.bundle',
                                                             'graphics/controller.bundle',
                                                             'graphics/fuzzytoonfilter.bundle',
                                                             'graphics/fxaa.bundle',
                                                             'graphics/tongue.bundle']))
              }
          }
      }
  }
  ```

### 

### 二、使用 SDK

#### 1. 初始化

在 `FURenderer` 类 的  `setup` 静态方法是对 FaceUnity SDK 一些全局数据初始化的封装，可以在 Application 中调用，也可以在工作线程调用，仅需初始化一次即可。

当前demo在 StreamingApplication 类中执行。

#### 2.创建

在 `FaceUnityDataFactory` 类 的  `bindCurrentRenderer` 方法是对 FaceUnity SDK 每次使用前数据初始化的封装。

在 AVStreamingActivity 类中 设置 SurfaceTextureCallback回调方法，且在onSurfaceCreated方法中执行。

```
            mMediaStreamingManager.setSurfaceTextureCallback(new SurfaceTextureCallback() {
                @Override
                public void onSurfaceCreated() {
                    if (mFURenderer != null) {
                        mFURenderer.setBeautyOn();
                        mControlFragment.bindDataFactory();
                        initCsvUtil(AVStreamingActivity.this);
                    }
                    mCameraNv21 = null;
                    mReadback = null;
                }

                @Override
                public void onSurfaceChanged(int width, int height) {
                    Log.d(TAG, "onSurfaceChanged() width = [" + width + "], height = [" + height + "]");
                }

                @Override
                public void onSurfaceDestroyed() {
                    Log.d(TAG, "onSurfaceDestroyed: ");
                    if (mFURenderer != null) {
                        mFURenderer.release();
                    }
                }

                /* Camera 切换时不进行美颜和编码，同时把数据缓存清空 */
                @Override
                public int onDrawFrame(int texId, int width, int height, float[] floats) {
                    // call on GLThread
                    if (mFURenderer == null || mIsSwitchingCamera || mCameraNv21 == null) {
                        return texId;
                    }
                    if (mReadback == null) {
                        mReadback = new byte[mCameraNv21.length];
                    }
                    int fuTexId = mFURenderer.onDrawFrameDualInput(mCameraNv21, texId, width, height, mReadback, width, height);
                    return fuTexId;
                }
            });
```

#### 3. 图像处理

在 `FURenderer` 类 的  `onDrawFrame` 方法是对 FaceUnity SDK 图像处理方法的封装，该方法有许多重载方法适用于不同的数据类型需求。

在 AVStreamingActivity 类中注册 SurfaceTextureCallback监听，在 onDrawFrame 方法中执行。（代码如上）

onDrawFrameSingleInput 是单输入，输入图像buffer数组或者纹理Id，输出纹理Id
onDrawFrameDualInput 双输入，输入图像buffer数组与纹理Id，输出纹理Id。性能上，双输入优于单输入

在onDrawFrameSingleInput 与onDrawFrameDualInput 方法内，在执行底层方法之前，都会执行prepareDrawFrame()方法(执行各个特效模块的任务，将美颜参数传给底层)。

#### 4. 销毁

在 `FURenderer` 类 的  `release` 方法是对 FaceUnity SDK 数据销毁的封装。

在 AVStreamingActivity 类中注册 SurfaceTextureCallback监听，在 onSurfaceDestroyed方法中执行。（代码如上）

#### 5. 切换相机
在 AVStreamingActivity  内部类 Switcher 的 run方法中执行。
在切换相机是需要 重新设置sdk参数，并且 调用 `FURenderer.release()`方法

```
private class Switcher implements Runnable {
    @Override
    public void run() {
        mCurrentCamFacingIndex = (mCurrentCamFacingIndex + 1) % CameraStreamingSetting.getNumberOfCameras();
        final CameraStreamingSetting.CAMERA_FACING_ID facingId;
        if (mCurrentCamFacingIndex == CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK.ordinal()) {
            facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK;
        } else if (mCurrentCamFacingIndex == CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT.ordinal()) {
            facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT;
        } else {
            facingId = CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_3RD;
        }
        if (mMediaStreamingManager.switchCamera(facingId)) {
            mCameraPreviewFrameView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    if (mFURenderer != null) {

                        if(facingId == CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT){
                            mFURenderer.setCameraFacing(CameraFacingEnum.CAMERA_FRONT);
                            mFURenderer.setInputOrientation(270);
                        } else if (facingId == CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_BACK){
                            mFURenderer.setCameraFacing(CameraFacingEnum.CAMERA_BACK);
                            mFURenderer.setInputOrientation(90);
                        }
                        mFURenderer.release();
                    }
                }
            });
        }
        mIsSwitchingCamera = true;
        mIsEncodingMirror = mCameraConfig.mEncodingMirror;
        mIsPreviewMirror = facingId == CameraStreamingSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT && mCameraConfig.mPreviewMirror;
    }
}
```

#### 6. 旋转手机

调用 `FURenderer` 类 的  `setDeviceOrientation` 方法，用于重新为 SDK 设置参数。

在 AVStreamingActivity   中

```java
1.implements SensorEventListener
2. void initStreamingManager(){
    mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
}

@Override
protected void onResume() {
    super.onResume();
    mMediaStreamingManager.resume();
    if (mSensorManager != null) {
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
}

@Override
protected void onPause() {
    super.onPause();
    normalPause();
    if (null != mSensorManager) {
        mSensorManager.unregisterListener(this);
    }
}

    @Override
    public void onSensorChanged(SensorEvent event) {
		//具体代码见  AVStreamingActivity
    }
```

上面一系列方法的使用，具体在 demo 中的 `AVStreamingActivity`类，参考该代码示例接入即可。

### 三、接口介绍

- IFURenderer 是核心接口，提供了创建、销毁、渲染等接口。
- FaceUnityDataFactory 控制四个功能模块，用于功能模块的切换，初始化
- FaceBeautyDataFactory 是美颜业务工厂，用于调整美颜参数。
- PropDataFactory 是道具业务工厂，用于加载贴纸效果。
- MakeupDataFactory 是美妆业务工厂，用于加载美妆效果。
- BodyBeautyDataFactory 是美体业务工厂，用于调整美体参数。

关于 SDK 的更多详细说明，请参看 **[FULiveDemoDroid](https://github.com/Faceunity/FULiveDemoDroid/)**。如有对接问题，请联系技术支持。
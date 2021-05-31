package com.example.shvr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES20;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;

import org.rajawali3d.cameras.Camera;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.StreamingTexture;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.ScreenQuad;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.RenderTarget;
import org.rajawali3d.renderer.Renderer;
import org.rajawali3d.scene.Scene;

import java.nio.ByteBuffer;

public class VRRenderer extends Renderer implements SensorEventListener {


    public VRRenderer(Context context, InitCall initCall) {
        super(context);
        mContext = context;
        setFrameRate(60);
        this.initCall = initCall;
        if (param == null)
            param = new InitCallParam();
        this.initCall.ctor(param);
    }


    /**
     * The camera for the left eye
     */
    private Camera mCameraLeft;
    /**
     * The camera for the right eye
     */
    private Camera mCameraRight;
    /**
     * Half the width of the viewport. The screen will be split in two.
     * One half for the left eye and one half for the right eye.
     */
    public int mViewportWidthHalf;
    /**
     * The texture that will be used to render the scene into from the
     * perspective of the left eye.
     */
    private RenderTarget mLeftRenderTarget;
    /**
     * The texture that will be used to render the scene into from the
     * perspective of the right eye.
     */
    private RenderTarget mRightRenderTarget;
    /**
     * Used to store a reference to the user's scene.
     */
    private Scene mUserScene;
    /**
     * The side by side scene is what will finally be shown onto the screen.
     * This scene contains two quads. The left quad is the scene as viewed
     * from the left eye. The right quad is the scene as viewed from the
     * right eye.
     */
    private Scene mSideBySideScene;
    /**
     * This screen quad will contain the scene as viewed from the left eye.
     */
    private ScreenQuad mLeftQuad;
    /**
     * This screen quad will contain the scene as viewed from the right eye.
     */
    private ScreenQuad mRightQuad;
    /**
     * The material for the left quad
     */
    private Material mLeftQuadMaterial;
    /**
     * The material for the right quad
     */
    private Material mRightQuadMaterial;
    /**
     * The distance between the pupils. This is used to offset the cameras.
     */
    private Context mContext;
    private InitCallParam param;
    private Sphere earthSphere;
    private static float radius = 67;
    private float cflength = 0;
    private StreamingTexture videoTexture;
    InitCall initCall;
    public static MediaPlayer mMediaPlayer;

    private static final int MAX_IMAGE_NUMBER = 25;//这个值代表ImageReader最大的存储图像
    private ImageReader mImageReader;
    private Surface mInputSurface;
    private MediaCodec mediaCodec;

    @Override
    public void initScene() {

        initMedeaPlayer();//    初始化 播放器
        initEarthSphere();//    初始换 场景中的球体
        mViewportWidthHalf = (int) (mDefaultViewportWidth * .5f); // 计算一边容器宽度
        if (param.isvr)         //    vr模式 调整 原始渲染画布大小防止出现 拉伸
            setOverrideViewportDimensions(mViewportWidthHalf, mDefaultViewportHeight);
        initCamreaAndScreemQuad(); //   初始换相机及 显示屏幕显示 块
        setFlength(param.flength);
        //    抵用后期预处理细腻些
        if (initCall != null) {
            initCall.init();
        }
        initScener();

    }

    private static final float NS2S = 1.0f / 1000000000.0f;

    private float timestamp;

    private float angle[] = new float[3];
    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private Sensor magneticSensor;
    private Sensor accelerometerSensor;
    private Sensor rotationSensor;

    void initScener() {
        sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);

    }

    int i = 0, j = 0;
    float[] mags = null;
    float[] accels = null;
    float[] orientationValues = new float[3];

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ROTATION_VECTOR:
                float[] quaternion = new float[4];
                SensorManager.getQuaternionFromVector(quaternion, event.values);
              //  Log.d("rrr", "quaternion [0]: "+quaternion[0]+"; +quaternion [1]="+quaternion[1]+";quaternion [2]="+quaternion[2]+";quaternion [3]="+quaternion[3]);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mags = event.values.clone();
                break;
            case Sensor.TYPE_ACCELEROMETER:
                accels = event.values.clone();
                break;
            case Sensor.TYPE_GYROSCOPE:
                if (timestamp != 0) {
                    final float dT = (event.timestamp - timestamp) * NS2S;
                    angle[0] += event.values[0] * dT;
                    angle[1] += event.values[1] * dT;
                    angle[2] += event.values[2] * dT;
                    // 将弧度转化为角度
                    float anglex = (float) Math.toDegrees(angle[0]);
                    float angley = (float) Math.toDegrees(angle[1]);
                    float anglez = (float) Math.toDegrees(angle[2]);
//                    if (i % 10 == 0) {
//                        Log.d("bbbbb", "------anglex 左右------------>" + anglex);
//                        Log.d("bbbbb", "angley  俯仰------------>" + angley);
//                        //Log.d("bbbbb","anglez------------>" + anglez);
//                        i = 0;
//                    }
                    i++;
                }
                timestamp = event.timestamp;
                break;
        }

        if (mags != null && accels != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            if (SensorManager.getRotationMatrix(R, I, accels, mags)) {
                SensorManager.getOrientation(R, orientationValues);
                if (orientationValues != null) {
                    orientationValues[0] = (float) Math.toDegrees(orientationValues[0]);
                    orientationValues[1] = (float) Math.toDegrees(orientationValues[1]);
                    orientationValues[2] = (float) Math.toDegrees(orientationValues[2]);
                    if (j % 30 == 0) {
                     //   Log.d("bbbbbss", "orientationValues[0] " + orientationValues[0] + "orientationValues[1] " + orientationValues[1] + "orientationValues[2] " + orientationValues[2]);


                        j = 0;
                    }
                    j++;
                }
            }
        }
    }

    @SuppressLint("WrongConstant")
    void initCode() {

        this.mImageReader = ImageReader.newInstance(mDefaultViewportWidth, mDefaultViewportHeight, PixelFormat.RGBA_8888, MAX_IMAGE_NUMBER);

        mInputSurface = mImageReader.getSurface();
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireNextImage();//获取下一个
                Image.Plane[] planes = image.getPlanes();
                int width = image.getWidth();//设置的宽
                int height = image.getHeight();//设置的高
                int pixelStride = planes[0].getPixelStride();//像素个数，RGBA为4
                int rowStride = planes[0].getRowStride();//这里除pixelStride就是真实宽度
                int rowPadding = rowStride - pixelStride * width;//计算多余宽度

                byte[] data = new byte[rowStride * height];//创建byte
                ByteBuffer buffer = planes[0].getBuffer();//获得buffer
                buffer.get(data);//将buffer数据写入byte中

                //到这里为止就拿到了图像数据，你可以转换为yuv420，或者录制成H264

                //这里我提供一段转换为Bitmap的代码

                //这是最终数据，通过循环将内存对齐多余的部分删除掉
                // 正常ARGB的数据应该是width*height*4，但是因为是int所以不用乘4
                int[] pixelData = new int[width * height];

                int offset = 0;
                int index = 0;
                for (int i = 0; i < height; ++i) {
                    for (int j = 0; j < width; ++j) {
                        int pixel = 0;
                        pixel |= (data[offset] & 0xff) << 16;     // R
                        pixel |= (data[offset + 1] & 0xff) << 8;  // G
                        pixel |= (data[offset + 2] & 0xff);       // B
                        pixel |= (data[offset + 3] & 0xff) << 24; // A
                        pixelData[index++] = pixel;
                        offset += pixelStride;
                    }
                    offset += rowPadding;
                }

                Bitmap bitmap = Bitmap.createBitmap(pixelData,
                        width, height,
                        Bitmap.Config.ARGB_8888);//创建bitmap

                image.close();//用完需要关闭
            }
        }, null);
    }

    public void release() {
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    /**
     * 初始换相机及 显示屏幕显示 块
     */
    private void initCamreaAndScreemQuad() {
        mCameraLeft = new Camera();
        mCameraLeft.setNearPlane(.1f);
        mCameraLeft.setFieldOfView(90);
        mCameraLeft.setNearPlane(0.1);
        mCameraLeft.setFarPlane(200);
        mCameraLeft.setLookAt(new Vector3(0, 0, -1));
        mCameraLeft.setPosition(0, 0, 0);
        mCameraLeft.setUpAxis(0, 1, 0);
        mCameraRight = new Camera();
        mCameraRight.setNearPlane(.1f);
        mCameraRight.setFieldOfView(90);
        mCameraRight.setNearPlane(0.1);
        mCameraRight.setFarPlane(200);
        mCameraRight.setLookAt(new Vector3(0, 0, -0.1));
        setPupilDistance(param.pupilDistance);
        mLeftQuadMaterial = new Material();
        mLeftQuadMaterial.setColorInfluence(0);
        mRightQuadMaterial = new Material();
        mRightQuadMaterial.setColorInfluence(0);
        mSideBySideScene = new Scene(this);
        mLeftQuad = new ScreenQuad();
        if (param.isvr) {
            mLeftQuad.setScaleX(.5);
            mLeftQuad.setX(-.25);
        }
        mLeftQuad.setMaterial(mLeftQuadMaterial);
        mLeftQuad.setRotX(180);
        mSideBySideScene.addChild(mLeftQuad);
        mRightQuad = new ScreenQuad();
        mRightQuad.setScaleX(.5);
        mRightQuad.setX(.25);
        mRightQuad.setMaterial(mRightQuadMaterial);
        mSideBySideScene.addChild(mRightQuad);
        mRightQuad.setRotX(180);
        mRightQuad.setVisible(param.isvr);
        addScene(mSideBySideScene);
        mCameraLeft.setProjectionMatrix(mViewportWidthHalf, mDefaultViewportHeight);
        mCameraRight.setProjectionMatrix(mViewportWidthHalf, mDefaultViewportHeight);
        mLeftRenderTarget = new RenderTarget("sbsLeftRT", mViewportWidthHalf, mDefaultViewportHeight);
        mLeftRenderTarget.setFullscreen(false);
        mRightRenderTarget = new RenderTarget("sbsRightRT", mViewportWidthHalf, mDefaultViewportHeight);
        mRightRenderTarget.setFullscreen(false);
        addRenderTarget(mLeftRenderTarget);
        addRenderTarget(mRightRenderTarget);

        try {
            mLeftQuadMaterial.addTexture(mLeftRenderTarget.getTexture());
            mRightQuadMaterial.addTexture(mRightRenderTarget.getTexture());
        } catch (ATexture.TextureException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始换 场景中的球体
     */
    private void initEarthSphere() {
        Material material = new Material();
        material.enableLighting(false);
        material.setDiffuseMethod(new DiffuseMethod.Toon());
        //   material.setDiffuseMethod(new DiffuseMethod.Lambert());
        material.setColor(0);
        try {
            material.addTexture(videoTexture);
        } catch (ATexture.TextureException error) {
            Log.d("DEBUG", "TEXTURE ERROR");
        }
        earthSphere = new Sphere(radius, 32, 32);
        earthSphere.setMaterial(material);
        earthSphere.setDoubleSided(true);
        //earthSphere.setScaleX(-1);
        earthSphere.setRotY(param.xOffset);
        getCurrentScene().addChild(earthSphere);
    }

    /**
     * 初始化 播放器
     */
    private void initMedeaPlayer() {
        if (mMediaPlayer == null) {
            if (param.url.isEmpty())
                mMediaPlayer = new MediaPlayer();
            else {
                mMediaPlayer = MediaPlayer.create(mContext, Uri.parse(param.url));
                if (param.autoplay)
                    mMediaPlayer.start();
            }
        }
        videoTexture = new StreamingTexture("Earth", mMediaPlayer);
    }

    /**
     * 设置相机 fov
     *
     * @param fov 30 -120；
     */
    void setFovFunc(double fov) {
        if (fov >= 120) {
            fov = 120;
            mCameraLeft.setFieldOfView(fov);
            mCameraRight.setFieldOfView(fov);
        } else {
            if (30 >= fov) {
                fov = 30;
            }
            mCameraLeft.setFieldOfView(fov);
            mCameraRight.setFieldOfView(fov);
        }
    }

    /**
     * 动态设计相机位置
     *
     * @param radius 0 - 67;
     */
    void cameraPostion(float radius) {
        float rx = (float) (mCameraLeft.getRotX() - Math.PI);
        float y = (radius * (float) Math.sin(rx));
        float z = Math.abs(radius * (float) Math.cos(rx));
        ;
        float x = (float) mCameraLeft.getX();
        mCameraLeft.setPosition(x, y, z);
        mCameraRight.setPosition(x, y, z);
    }

    @Override
    protected void onRender(final long ellapsedTime, final double deltaTime) {
        videoTexture.update();
        mUserScene = getCurrentScene();

        setRenderTarget(mLeftRenderTarget);
        getCurrentScene().switchCamera(mCameraLeft);
        mLeftRenderTarget.setOffsetX(1000);
        GLES20.glViewport(0, 0, mViewportWidthHalf, mDefaultViewportHeight);
        render(ellapsedTime, deltaTime);

        setRenderTarget(mRightRenderTarget);
        mUserScene.switchCamera(mCameraRight);
        render(ellapsedTime, deltaTime);
        switchSceneDirect(mSideBySideScene);
        GLES20.glViewport(0, 0, mDefaultViewportWidth, mDefaultViewportHeight);
        setRenderTarget(null);
        render(ellapsedTime, deltaTime);
        switchSceneDirect(mUserScene);
    }

    public void ChangeFov(double v) {
        double fov = mCameraLeft.getFieldOfView() + v;
        setFovFunc(fov);
    }

    public void rotatoX(float val) {
        float v = (float) Math.toDegrees(mCameraLeft.getRotX()) + -val;
        if (v > 90.0) {
            mCameraLeft.setRotZ(90);
            mCameraRight.setRotZ(90);
        } else if (v < -90.0) {
            mCameraLeft.setRotZ(-90);
            mCameraRight.setRotZ(-90);
        } else {
            mCameraLeft.rotate(1, 0, 0, -val);
            mCameraRight.rotate(1, 0, 0, -val);
        }
    }

    /**
     * 这是相机左右转动 位置
     *
     * @param val
     */
    public void setRotateX(double val) {
        if (val >= 90) val = 90;
        if (val <= -90) val = -90;
        mCameraLeft.setRotZ(-val);
        mCameraRight.setRotZ(-val);
    }

    public double getRotateX() {
        return Math.toDegrees(mCameraLeft.getRotZ());
    }

    /**
     * 设置球体 左右旋转的度数
     *
     * @return
     */
    public void setRotateY(float val) {
        earthSphere.setRotY(val);
    }

    /**
     * 获取球体 左右旋转的度数
     *
     * @return
     */
    public double getRotateY() {
        return Math.toDegrees(earthSphere.getRotY());
    }

    /**
     * 累加是选择
     *
     * @param val 单位度
     */
    public void RotateY(float val) {
        earthSphere.rotate(0, 1, 0, val);
    }

    /**
     * 设置 Flength
     *
     * @param val
     */
    public void setFlength(float val) {
        if (val < 0 || val > 99)
            return;
        param.flength = val;
        if (param.flength <= 34) {
            mCameraLeft.setPosition(0, 0, 0);
            mCameraRight.setPosition(0, 0, 0);
            setFovFunc(56 + val);
            cflength = 34;

        } else if (param.flength < 67) {
            setFovFunc(90);
            radius = param.flength - 34;
            cameraPostion(radius);
            cflength = param.flength;

        } else {
            setFovFunc(90 + param.flength - 67);
            radius = param.flength - 34;
            cameraPostion(radius);
            cflength = param.flength;
        }
        Log.d("sssss ", "val=" + val + " ;setFlength: " + param.flength + "; fov =" + mCameraLeft.getFieldOfView() + " ; radius =" + radius);
    }

    /**
     * 获取 Flenght
     *
     * @return
     */
    public float getFlength() {
        return param.flength;
    }

    /**
     * 设置 播放模式
     *
     * @param isvr
     */
    public void setVrState(boolean isvr) {
        if (param.isvr != isvr) {
            if (isvr) {
                mRightQuad.setVisible(true);
                setOverrideViewportDimensions(mViewportWidthHalf, mDefaultViewportHeight);
                mLeftQuad.setScaleX(.5);
                mLeftQuad.setX(-.25);

            } else {
                mRightQuad.setVisible(false);
                setOverrideViewportDimensions(mDefaultViewportWidth, mDefaultViewportHeight);
                mLeftQuad.setScaleX(1);
                mLeftQuad.setX(0);

            }
        }
        param.isvr = isvr;
    }

    /**
     * 获取 否是是VR 模式
     *
     * @return
     */
    public boolean getVRState() {
        return param.isvr;
    }

    /**
     * 这是瞳距
     *
     * @param pupilDistance
     */
    public void setPupilDistance(float pupilDistance) {
        param.pupilDistance = pupilDistance;
        if (mCameraLeft != null)
            mCameraLeft.setX(pupilDistance * -.5);
        if (mCameraRight != null)
            mCameraRight.setX(pupilDistance * .5);
    }

    /**
     * 获取瞳距
     *
     * @return
     */
    public float getPupilDistance() {
        return param.pupilDistance;
    }


    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}

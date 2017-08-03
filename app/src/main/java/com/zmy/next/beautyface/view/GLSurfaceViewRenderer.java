package com.zmy.next.beautyface.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import com.zmy.next.beautyface.utils.CameraUtils;
import com.zmy.next.beautylib.GlBeautyDefaultDrawer;
import com.zmy.next.beautylib.GlBeautyDrawerGroup;
import com.zmy.next.beautylib.utils.BeautyConfig;
import com.zmy.next.beautylib.utils.OpenGlUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by zmy on 2017/3/28.
 */

public class GLSurfaceViewRenderer extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    protected int textureId = OpenGlUtils.NO_TEXTURE;
    private GlBeautyDrawerGroup drawer;
    private SurfaceTexture surfaceTexture;
    private int rotation;
    private int mWidth;
    private int mHeight;
    private float mScaleX = 1, mScaleY = 1;
    private int rotatedFrameWidth = 720;
    private int rotatedFrameHeight = 1280;
    private int surfaceWidth;
    private int surfaceHeight;

    public GLSurfaceViewRenderer(Context context) {
        this(context, null);
    }

    public GLSurfaceViewRenderer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        final Point size;
//        size = videoLayoutMeasure.measure(widthMeasureSpec, heightMeasureSpec, rotatedFrameWidth, rotatedFrameHeight);
//        setMeasuredDimension(size.x, size.y);
//        LogUtil.e("zmy", "onMeasure(). New size: " + size.x + "x" + size.y);
//    }
//
//    @Override
//    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
//        LogUtil.e("zmy", "onLayout");
//        super.onLayout(changed, left, top, right, bottom);
//        if (changed) {
//            mWidth = getWidth();
//            mHeight = getHeight();
//        }
//
//        updateSurfaceSize();
//    }

    private void updateSurfaceSize() {
        if (rotatedFrameWidth != 0 && rotatedFrameHeight != 0 && getWidth() != 0
                && getHeight() != 0) {
            final float layoutAspectRatio = getWidth() / (float) getHeight();
            final float frameAspectRatio = rotatedFrameWidth / (float) rotatedFrameHeight;
            final int drawnFrameWidth;
            final int drawnFrameHeight;
            if (frameAspectRatio > layoutAspectRatio) {
                drawnFrameWidth = (int) (rotatedFrameHeight * layoutAspectRatio);
                drawnFrameHeight = rotatedFrameHeight;
            } else {
                drawnFrameWidth = rotatedFrameWidth;
                drawnFrameHeight = (int) (rotatedFrameWidth / layoutAspectRatio);
            }
            // Aspect ratio of the drawn frame and the view is the same.
            final int width = Math.min(getWidth(), drawnFrameWidth);
            final int height = Math.min(getHeight(), drawnFrameHeight);
            if (width != surfaceWidth || height != surfaceHeight) {
                surfaceWidth = width;
                surfaceHeight = height;
                getHolder().setFixedSize(width, height);
            }
        } else {
            surfaceWidth = surfaceHeight = 0;
            getHolder().setSizeFromLayout();
        }
    }


    public void setScale(boolean isScale) {
        if (isScale) {
            this.mScaleX = getScaleX();
            this.mScaleY = getScaleY();
        } else {
            this.mScaleX = 1;
            this.mScaleY = 1;
        }
    }

    private void init() {
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        setZOrderMediaOverlay(true);

        drawer = new GlBeautyDrawerGroup();
        if (BeautyConfig.isOpenBeautify) {
            drawer.setFilter(new GlBeautyDefaultDrawer());
        }
        rotation = ((Activity) getContext()).getWindowManager().getDefaultDisplay()
                .getRotation();

        CameraUtils.getInstance();
    }

    public void onBeautifyChange() {
        if (drawer == null) {
            return;
        }
        if (BeautyConfig.isOpenBeautify) {
            drawer.setFilter(new GlBeautyDefaultDrawer());
        } else {
            drawer.clearFilter();
        }
    }

    private Handler handler = new Handler();

    public void restartCamera() {
        initSurfaceTexture();
        if (drawer == null) {
            drawer = new GlBeautyDrawerGroup();
        } else {
            if (BeautyConfig.isOpenBeautify) {
                drawer.setFilter(new GlBeautyDefaultDrawer());
            }
        }
        CameraUtils.getInstance().restartCamera(rotation, rotatedFrameHeight, rotatedFrameWidth);
    }

    public void releaseCamera() {
        textureId = OpenGlUtils.NO_TEXTURE;
        mWidth = 0;
        mHeight = 0;
        CameraUtils.getInstance().releaseCamera();
        if (drawer != null) {
            drawer.release();
        }
        synchronized (this) {
            surfaceTexture = null;
        }
    }

    private void initSurfaceTexture() {
        if (textureId == OpenGlUtils.NO_TEXTURE) {
            textureId = OpenGlUtils.getExternalOESTextureID();
            if (textureId != OpenGlUtils.NO_TEXTURE) {
                surfaceTexture = new SurfaceTexture(textureId);
                surfaceTexture.setOnFrameAvailableListener(this);
                CameraUtils.getInstance().setSurfaceTexture(surfaceTexture);
            }
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.e("zmy", "onSurfaceCreated");
        GLES20.glClearColor(1.0f, 0, 0, 1.0f);
        initSurfaceTexture();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.e("zmy", "onSurfaceChanged width = " + width + " height=" + height);
        GLES20.glViewport(0, 0, width, height);
        mWidth = width;
        mHeight = height;
        restartCamera();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (surfaceTexture == null) {
            return;
        }
        synchronized (this) {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            surfaceTexture.updateTexImage();
            float[] mtx = new float[16];
            surfaceTexture.getTransformMatrix(mtx);

            if (drawer != null) {
                drawer.drawOes(textureId, mtx, rotatedFrameWidth, rotatedFrameHeight, 0, mHeight - (int) (mHeight * mScaleY),
                        (int) (mWidth * mScaleX), (int) (mHeight * mScaleY));
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e("zmy", "surfaceDestroyed");
        super.surfaceDestroyed(holder);
        releaseCamera();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
//        LogUtil.e("zmy", "onFrameAvailable");
        requestRender();
    }

    public void onDestroy() {
        releaseCamera();
        CameraUtils.getInstance().onDestroy();
        drawer = null;
    }

}

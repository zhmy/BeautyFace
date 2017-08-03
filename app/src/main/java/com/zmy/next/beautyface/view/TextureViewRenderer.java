package com.zmy.next.beautyface.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import com.zmy.next.beautyface.utils.CameraUtils;
import com.zmy.next.beautylib.GlBeautyDefaultDrawer;
import com.zmy.next.beautylib.GlBeautyDrawerGroup;
import com.zmy.next.beautylib.utils.OpenGlUtils;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * Created by zmy on 2017/3/27.
 */

public class TextureViewRenderer extends TextureView implements TextureView.SurfaceTextureListener, SurfaceTexture.OnFrameAvailableListener {

    private int rotation;
    private SurfaceTexture mSurface;
    private final GLCameraRenderThread mRenderThread = new GLCameraRenderThread();
    private int mWidth;
    private int mHeight;
    private Camera.Size mPreviewSize;
    private int rotatedFrameWidth = 720;
    private int rotatedFrameHeight = 1280;
    private int textureId;

    public TextureViewRenderer(Context context) {
        this(context, null);
    }

    public TextureViewRenderer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextureViewRenderer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
////        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
////        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
////        setMeasuredDimension(width, height);
////
////        if (CameraUtils.getInstance().mSupportedPreviewSizes != null) {
////            mPreviewSize = CameraUtils.getInstance().getOptimalPreviewSize(CameraUtils.getInstance().mSupportedPreviewSizes, width, height);
////        }
//
//        final Point size;
//            size =
//                    videoLayoutMeasure.measure(widthMeasureSpec, heightMeasureSpec, rotatedFrameWidth, rotatedFrameHeight);
//        setMeasuredDimension(size.x, size.y);
//        LogUtil.e("zmy", "onMeasure(). New size: " + size.x + "x" + size.y);
//    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        Log.e("zmy", "onLayout");
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            mWidth = getWidth();
            mHeight = getHeight();
        }

        updateSurfaceSize();
    }

    private void updateSurfaceSize() {
//        if (rotatedFrameWidth != 0 && rotatedFrameHeight != 0 && getWidth() != 0
//                && getHeight() != 0) {
//            final float layoutAspectRatio = getWidth() / (float) getHeight();
//            final float frameAspectRatio = rotatedFrameWidth / (float) rotatedFrameHeight;
//            final int drawnFrameWidth;
//            final int drawnFrameHeight;
//            if (frameAspectRatio > layoutAspectRatio) {
//                drawnFrameWidth = (int) (rotatedFrameHeight * layoutAspectRatio);
//                drawnFrameHeight = rotatedFrameHeight;
//            } else {
//                drawnFrameWidth = rotatedFrameWidth;
//                drawnFrameHeight = (int) (rotatedFrameWidth / layoutAspectRatio);
//            }
//            // Aspect ratio of the drawn frame and the view is the same.
//            final int width = Math.min(getWidth(), drawnFrameWidth);
//            final int height = Math.min(getHeight(), drawnFrameHeight);
//            if (width != surfaceWidth || height != surfaceHeight) {
//                surfaceWidth = width;
//                surfaceHeight = height;
//                getHolder().setFixedSize(width, height);
//            }
//        } else {
//            surfaceWidth = surfaceHeight = 0;
//            getHolder().setSizeFromLayout();
//        }
    }

    private void init() {
        setSurfaceTextureListener(this);
        rotation = ((Activity) getContext()).getWindowManager().getDefaultDisplay()
                .getRotation();
        rotatedFrameWidth = 0;
        rotatedFrameHeight = 0;
        textureId = OpenGlUtils.getExternalOESTextureID();
//        mSurface = new SurfaceTexture(textureId);
//        setSurfaceTexture(mSurface);
    }

    public void restartCamera() {
        CameraUtils.getInstance().restartCamera(rotation, mHeight, mWidth);
    }

    public void releaseCamera() {
        CameraUtils.getInstance().releaseCamera();
//        mRenderThread.interrupt();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e("zmy", "onSurfaceTextureAvailable width = " + width + " height = " + height);
        mSurface = surface;
//        surface.setOnFrameAvailableListener(this);
        CameraUtils.getInstance().setSurfaceTexture(mSurface);
        CameraUtils.getInstance().initCamera(rotation, height, width);
//        mRenderThread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e("zmy", "onSurfaceTextureSizeChanged");
        mWidth = width;
        mHeight = height;
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e("zmy", "onSurfaceTextureDestroyed");
        releaseCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//        LogUtil.e("zmy", "onSurfaceTextureUpdated");
//        synchronized (mRenderThread) {
//            mRenderThread.notify();
//        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Log.e("zmy", "onFrameAvailable");
//        synchronized (mRenderThread) {
//            mRenderThread.notify();
//        }
    }

    public class GLCameraRenderThread extends Thread {

        private EGL10 mEgl;
        private EGLDisplay mEglDisplay;
        private EGLConfig mEglConfig;
        private EGLContext mEglContext;
        private EGLSurface mEglSurface;

        private GlBeautyDrawerGroup drawer;
        private float[] texMatrix = new float[16];

        public GLCameraRenderThread() {

            drawer = new GlBeautyDrawerGroup();
            drawer.setFilter(new GlBeautyDefaultDrawer());
        }

        @Override
        public void run() {
            super.run();
            Log.e("zmy", "run");
            try {
                if (isInterrupted()) {
                    destroyGL();
                    return;
                }
                initGL();

                while (true) {
                    Log.e("zmy", "run inner");

                    mSurface.attachToGLContext(textureId);
                    mSurface.updateTexImage();
                    mSurface.getTransformMatrix(texMatrix);

                    drawFrame();

                    mSurface.detachFromGLContext();

                    eglSwapBuffers();
                    wait(); //Wait for next frame available

                }
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                destroyGL();
            }

        }

        private void eglSwapBuffers() {
            mEgl.eglSwapBuffers(mEglDisplay, mEglSurface);
        }

        private void drawFrame() {
            Log.e("zmy", "draw frame");
            GLES20.glClearColor(0 /* red */, 0 /* green */, 0 /* blue */, 0 /* alpha */);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            drawer.drawOes(textureId, texMatrix, mWidth, mHeight, 0, 0, mWidth, mHeight);
        }

        private void initGL() {
    /*Get EGL handle*/
            mEgl = (EGL10) EGLContext.getEGL();

    /*Get EGL display*/
            mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

    /*Initialize & Version*/
            int versions[] = new int[2];
            mEgl.eglInitialize(mEglDisplay, versions);

    /*Configuration*/
            int configsCount[] = new int[1];
            EGLConfig configs[] = new EGLConfig[1];
            int configSpec[] = new int[]{
                    EGL10.EGL_RENDERABLE_TYPE,
                    EGL14.EGL_OPENGL_ES2_BIT,
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_ALPHA_SIZE, 8,
                    EGL10.EGL_DEPTH_SIZE, 0,
                    EGL10.EGL_STENCIL_SIZE, 0,
                    EGL10.EGL_NONE};

            mEgl.eglChooseConfig(mEglDisplay, configSpec, configs, 1, configsCount);
            mEglConfig = configs[0];

    /*Create Context*/
            int contextSpec[] = new int[]{
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL10.EGL_NONE};

            mEglContext = mEgl.eglCreateContext(mEglDisplay, mEglConfig, EGL10.EGL_NO_CONTEXT, contextSpec);

    /*Create window surface*/
            mEglSurface = mEgl.eglCreateWindowSurface(mEglDisplay, mEglConfig, mSurface, null);

    /*Make current*/
            mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext);
        }

        public void destroyGL() {
            mEgl.eglDestroyContext(mEglDisplay, mEglContext);
            mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
            mEglContext = EGL10.EGL_NO_CONTEXT;
            mEglSurface = EGL10.EGL_NO_SURFACE;
        }
    }
}

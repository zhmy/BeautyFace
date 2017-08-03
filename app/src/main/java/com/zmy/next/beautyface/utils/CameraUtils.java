package com.zmy.next.beautyface.utils;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.abs;

/**
 * Created by zmy on 2017/3/27.
 */

public class CameraUtils {
    private static CameraUtils instance;
    private static Camera mCamera;
    private SurfaceTexture surfaceTexture;
    private int currentCameraId;
    public List<Camera.Size> mSupportedPreviewSizes;
    private CameraThread cameraThread;

    private CameraUtils() {
//        cameraThread = new CameraThread();
//        cameraThread.start();
    }

    public static CameraUtils getInstance() {
        if (instance == null) {
            instance = new CameraUtils();
        }
        return instance;
    }

    public void setSurfaceTexture(final SurfaceTexture surfaceTexture) {
        this.surfaceTexture = surfaceTexture;
    }

    public Camera initCamera(int rotation, int w, int h) {
        Log.e("zmy", "initCamera rotation = " + rotation + " w = " + w + " h = " + h);
        releaseCameraInner();
        if (mCamera != null) {
            return mCamera;
        }

        Log.e("zmy", "create camera");
        int cameraId = findFrontCamera();
        if (cameraId == -1) {
            cameraId = findBackCamera();
            if (cameraId == -1) {
                Log.e("zmy", "no camera");
                return null;
            }
        }
        try {
            mCamera = Camera.open(cameraId);
            setCameraDisplayOrientation(rotation, cameraId, mCamera);

            Camera.Parameters parameters = mCamera.getParameters();
            mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
//            Camera.Size size = getClosestSupportedSize(mSupportedPreviewSizes, w, h);
            Camera.Size size = getOptimalPreviewSize(mSupportedPreviewSizes, w, h);
            Log.e("zmy", "support size width = "+size.width+" heigit = "+size.height);
            parameters.setPreviewSize(size.width, size.height);
            mCamera.setParameters(parameters);
            mCamera.setPreviewTexture(surfaceTexture);
            mCamera.startPreview();

        } catch (Throwable throwable) {
            Log.e("zmy", "initCamera err = " + throwable.getMessage());
            throwable.printStackTrace();
            releaseCamera();
        }
        return mCamera;
    }

    public Camera.Size getClosestSupportedSize(
            List<Camera.Size> supportedSizes, final int requestedWidth, final int requestedHeight) {
        return Collections.min(supportedSizes, new ClosestComparator<Camera.Size>() {
            @Override
            int diff(Camera.Size size) {
                return abs(requestedWidth - size.width) + abs(requestedHeight - size.height);
            }
        });
    }


    // Helper class for finding the closest supported format for the two functions below. It creates a
    // comparator based on the difference to some requested parameters, where the element with the
    // minimum difference is the element that is closest to the requested parameters.
    private abstract class ClosestComparator<T> implements Comparator<T> {
        // Difference between supported and requested parameter.
        abstract int diff(T supportedParameter);

        @Override
        public int compare(T t1, T t2) {
            return diff(t1) - diff(t2);
        }
    }

    public void switchCamera() {

    }

    public void restartCamera(final int rotation, final int w, final int h) {
        if (cameraThread == null || cameraThread.getCameraHandler() == null) {
            if (mCamera == null) {
                initCamera(rotation, w, h);
            }
        } else {
            cameraThread.getCameraHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (mCamera == null) {
                        initCamera(rotation, w, h);
                    }
                }
            });
        }
    }

    public void releaseCamera() {
        if (cameraThread == null || cameraThread.getCameraHandler() == null) {
            releaseCameraInner();
        } else {
            cameraThread.getCameraHandler().post(new Runnable() {
                @Override
                public void run() {
                    releaseCameraInner();
                }
            });
        }
    }

    private void releaseCameraInner() {
        if (mCamera == null) {
            return;
        }
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    public void onDestroy() {
        releaseCamera();
        if (cameraThread != null && cameraThread.getCameraHandler() != null) {
            cameraThread.getCameraHandler().post(new Runnable() {
                @Override
                public void run() {
                    cameraThread.shutdown();
                }
            });
        }
    }

    private int findFrontCamera() {
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); // get cameras number

        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo); // get camerainfo
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                return camIdx;
            }
        }
        return -1;
    }

    public void setCameraDisplayOrientation(int rotation,
                                            int cameraId, Camera camera) {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);


    }


    public Camera.Size choosePreviewSize(Camera.Parameters parms, int w, int h) {
        if (parms == null) {
            return null;
        }

        List<Camera.Size> sizes = parms.getSupportedPreviewSizes();
        if (sizes == null) {
            return null;
        }

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;

        Camera.Size optimalSize = null;

        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            double ratioDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                double ratio = (double) size.width / size.height;
                double diffRat = Math.abs(ratio - targetRatio);
                if (diffRat <= ratioDiff && Math.abs(size.height - targetHeight) < minDiff) {
                    ratioDiff = diffRat;
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        if (optimalSize != null) {
            parms.setPreviewSize(optimalSize.width, optimalSize.height);
        }
        return optimalSize;
    }

    public Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;
        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    private int findBackCamera() {
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); // get cameras number

        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo); // get camerainfo
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                return camIdx;
            }
        }
        return -1;
    }
}

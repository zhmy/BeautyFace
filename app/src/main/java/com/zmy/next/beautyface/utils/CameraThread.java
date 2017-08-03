package com.zmy.next.beautyface.utils;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by zmy on 2017/3/30.
 */

public class CameraThread extends Thread {

    private Handler cameraHandler;
    @Override
    public void run() {
        super.run();
        Looper.prepare();
        cameraHandler = new Handler(Looper.myLooper());
        Looper.loop();
    }

    public Handler getCameraHandler() {
        return cameraHandler;
    }

    public void shutdown() {
        Looper.myLooper().quit();
    }
}

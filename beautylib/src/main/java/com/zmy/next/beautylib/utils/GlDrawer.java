package com.zmy.next.beautylib.utils;

/**
 * Created by zmy on 2017/8/2.
 */

public interface GlDrawer {
    /**
     * Functions for drawing frames with different sources. The rendering surface target is
     * implied by the current EGL context of the calling thread and requires no explicit argument.
     * The coordinates specify the viewport location on the surface target.
     */
    void drawOes(int oesTextureId, float[] texMatrix, int frameWidth, int frameHeight,
                 int viewportX, int viewportY, int viewportWidth, int viewportHeight);
    void drawRgb(int textureId, float[] texMatrix, int frameWidth, int frameHeight, int viewportX,
                 int viewportY, int viewportWidth, int viewportHeight);
    void drawYuv(int[] yuvTextures, float[] texMatrix, int frameWidth, int frameHeight,
                 int viewportX, int viewportY, int viewportWidth, int viewportHeight);

    /**
     * Release all GL resources. This needs to be done manually, otherwise resources may leak.
     */
    void release();
}

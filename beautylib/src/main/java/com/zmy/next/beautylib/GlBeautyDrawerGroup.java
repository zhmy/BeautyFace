package com.zmy.next.beautylib;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.zmy.next.beautylib.utils.GlRectDrawer;
import com.zmy.next.beautylib.utils.GlShader;
import com.zmy.next.beautylib.utils.GlUtil;

import java.nio.FloatBuffer;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by zmy on 2017/3/1.
 */

public class GlBeautyDrawerGroup extends GlRectDrawer {

    public static final String NO_FILTER_VERTEX_SHADER = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "}";
    public static final String NO_FILTER_FRAGMENT_SHADER = "" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            "uniform sampler2D inputImageTexture;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";


    // Vertex coordinates in Normalized Device Coordinates, i.e. (-1, -1) is bottom-left and (1, 1) is
    // top-right.
    protected static FloatBuffer mGLCubeBuffer = GlUtil.createFloatBuffer(new float[]{
            -1.0f, -1.0f, // Bottom left.
            1.0f, -1.0f, // Bottom right.
            -1.0f, 1.0f, // Top left.
            1.0f, 1.0f, // Top right.
    });

    // Texture coordinates - (0, 0) is bottom-left and (1, 1) is top-right.
    protected static FloatBuffer mGLTextureBuffer = GlUtil.createFloatBuffer(new float[]{
            0.0f, 0.0f, // Bottom left.
            1.0f, 0.0f, // Bottom right.
            0.0f, 1.0f, // Top left.
            1.0f, 1.0f // Top right.
    });


    // The keys are one of the fragments shaders above.
    protected final Map<String, GroupShader> groupShaders = new IdentityHashMap<String, GroupShader>();


    protected String vertexShader;
    protected String fragmentShader;

    protected int mIntputWidth;
    protected int mIntputHeight;


    private GlBeautyDrawerGroup mFilter;
    private final LinkedList<Runnable> mRunOnDraw;

    public void setFilter(final GlBeautyDrawerGroup... filters) {
        if (filters == null) {
            return;
        }
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                releaseFilter();
                mFilter = filters[0];
            }
        });
    }

    public void clearFilter() {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                release();
            }
        });
    }

    private void releaseFilter() {
        if (mFilter != null) {
            mFilter.release();
            mFilter = null;
        }
    }

    protected static class GroupShader {
        public final GlShader glShader;

        public final int mGLAttribPosition;
        public final int mGLUniformTexture;
        public final int mGLAttribTextureCoordinate;

        public GroupShader(String vertexShader, String fragmentShader) {
            this.glShader = new GlShader(vertexShader, fragmentShader);

            mGLAttribPosition = glShader.getAttribLocation("position");
            mGLUniformTexture = glShader.getUniformLocation("inputImageTexture");
            mGLAttribTextureCoordinate = glShader.getAttribLocation("inputTextureCoordinate");
        }
    }

    protected int width;
    protected int height;
    protected float[] texMatrix;
    protected int textureId;

    public GlBeautyDrawerGroup() {
        mRunOnDraw = new LinkedList<>();
        vertexShader = NO_FILTER_VERTEX_SHADER;
        fragmentShader = NO_FILTER_FRAGMENT_SHADER;
    }

    protected void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }

    protected void onDrawArraysPre() {
    }

    protected void onDrawArraysAfter() {
    }

    /**
     * Draw an OES texture frame with specified texture transformation matrix. Required resources are
     * allocated at the first call to this function.
     */
    @Override
    public void drawOes(int oesTextureId, float[] texMatrix, int frameWidth, int frameHeight,
                        int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
//        Log.e("zmy", "drawOes oesTextureId = "+oesTextureId+" frameWidth = "+frameWidth+" frameHeight = "+frameHeight +" viewportWidth = "+viewportWidth+" viewportHeight = "+viewportHeight);
        runPendingOnDrawTasks();

        this.width = viewportWidth;
        this.height = viewportHeight;
        this.texMatrix = texMatrix;
        this.textureId = oesTextureId;

        if (mFilter != null) {
            mFilter.drawOes(oesTextureId, texMatrix, frameWidth, frameHeight, viewportX, viewportY, viewportWidth, viewportHeight);
        } else {
            prepareShader(texMatrix);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            // updateTexImage() may be called from another thread in another EGL context, so we need to
            // bind/unbind the texture in each draw call so that GLES understads it's a new texture.
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId);
            onDrawArraysPre();
            drawRectangle(viewportX, viewportY, viewportWidth, viewportHeight);
            onDrawArraysAfter();
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        }
    }

    /**
     * Draw a RGB(A) texture frame with specified texture transformation matrix. Required resources
     * are allocated at the first call to this function.
     */
    @Override
    public void drawRgb(int textureId, float[] texMatrix, int frameWidth, int frameHeight,
                        int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
//        Log.e("zmy", "drawRgb oesTextureId = "+textureId+" frameWidth = "+frameWidth+" frameHeight = "+frameHeight +" viewportWidth = "+viewportWidth+" viewportHeight = "+viewportHeight);
        super.drawRgb(textureId, texMatrix, frameWidth, frameHeight, viewportX, viewportY, viewportWidth, viewportHeight);
    }

    /**
     * Draw a YUV frame with specified texture transformation matrix. Required resources are
     * allocated at the first call to this function.
     */
    @Override
    public void drawYuv(int[] yuvTextures, float[] texMatrix, int frameWidth, int frameHeight,
                        int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
//        Log.e("zmy", "drawYuv frameWidth = "+frameWidth+" frameHeight = "+frameHeight +" viewportWidth = "+viewportWidth+" viewportHeight = "+viewportHeight);
        super.drawYuv(yuvTextures, texMatrix, frameWidth, frameHeight, viewportX, viewportY, viewportWidth, viewportHeight);
    }

    protected void prepareShader(float[] texMatrix) {
        super.prepareShader(OES_FRAGMENT_SHADER_STRING, texMatrix);
    }

    @Override
    public void release() {
        super.release();
        releaseFilter();
    }
}


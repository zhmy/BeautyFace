package com.zmy.next.beautylib;

import android.opengl.GLES10;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import com.zmy.next.beautylib.utils.GlUtil;
import com.zmy.next.beautylib.utils.OpenGlUtils;

import java.nio.FloatBuffer;

/**
 * Created by zmy on 2017/3/1.
 */

public class GlBeautyDefaultDrawer extends GlBeautyDrawerGroup {

    private static class DefaultShader extends GroupShader {
        public final int mTextureTransformMatrixLocation;
        public final int mSingleStepOffsetLocation;
        public final int mParamsLocation;

        public DefaultShader(String vertexShader, String fragmentShader) {
            super(vertexShader, fragmentShader);
            mTextureTransformMatrixLocation = glShader.getUniformLocation("textureTransform");
            mSingleStepOffsetLocation = glShader.getUniformLocation("singleStepOffset");
            mParamsLocation = glShader.getUniformLocation("params");
        }
    }

    public GlBeautyDefaultDrawer() {
        super();
        vertexShader = OpenGlUtils.readShaderFromRawResource(R.raw.default_vertex);
        fragmentShader = OpenGlUtils.readShaderFromRawResource(R.raw.default_fragment);
    }

    @Override
    protected void prepareShader(float[] texMatrix) {
//        Log.e("zmy", "prepareShader");
        final DefaultShader shader;
        if (groupShaders.containsKey(fragmentShader)) {
            shader = (DefaultShader) groupShaders.get(fragmentShader);
        } else {
            // Lazy allocation.
            shader = new DefaultShader(vertexShader, fragmentShader);
            groupShaders.put(fragmentShader, shader);
            shader.glShader.useProgram();
            shader.glShader.setVertexAttribArray("position", 2, mGLCubeBuffer);
            shader.glShader.setVertexAttribArray("inputTextureCoordinate", 2, mGLTextureBuffer);
            GLES20.glUniform1i(shader.mGLUniformTexture, 0);
            GLES20.glUniform1f(shader.mParamsLocation, 0.33f);

            GLES20.glUniform2fv(shader.mSingleStepOffsetLocation, 1, FloatBuffer.wrap(new float[]{2.0f / width, 2.0f / height}));
            GlUtil.checkNoGLES2Error("Initialize fragment shader uniform values.");
        }
        shader.glShader.useProgram();

        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(shader.mTextureTransformMatrixLocation, 1, false, texMatrix, 0);

    }


    public void initCameraFrameBuffer(int width, int height) {
        Log.e("zmy", "initCameraFrameBuffer ");
        if (mFrameBuffers != null && (mFrameWidth != width || mFrameHeight != height))
            destroyFramebuffers();
        if (mFrameBuffers == null) {
            mFrameWidth = width;
            mFrameHeight = height;
            mFrameBuffers = new int[1];
            mFrameBufferTextures = new int[1];

            GLES20.glGenFramebuffers(1, mFrameBuffers, 0);
            GLES20.glGenTextures(1, mFrameBufferTextures, 0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mFrameBufferTextures[0]);
            GLES20.glTexImage2D(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0, GLES10.GL_RGBA, width, height, 0,
                    GLES10.GL_RGBA, GLES10.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_LINEAR);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES10.GL_TEXTURE_WRAP_S, GLES10.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GLES10.GL_TEXTURE_WRAP_T, GLES10.GL_CLAMP_TO_EDGE);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mFrameBufferTextures[0], 0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }
    }

    public void destroyFramebuffers() {
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(1, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(1, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
        mFrameWidth = -1;
        mFrameHeight = -1;
    }

    private int[] mFrameBuffers = null;
    private int[] mFrameBufferTextures = null;
    private int mFrameWidth = -1;
    private int mFrameHeight = -1;


    public int onDrawToTexture(final int textureId) {
        Log.e("zmy", "onDrawToTexture");
        if (mFrameBuffers == null)
            return OpenGlUtils.NO_TEXTURE;
        GLES20.glViewport(0, 0, mFrameWidth, mFrameHeight);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);

        final DefaultShader shader;
        if (groupShaders.containsKey(fragmentShader)) {
            shader = (DefaultShader) groupShaders.get(fragmentShader);
        } else {
            // Lazy allocation.
            shader = new DefaultShader(vertexShader, fragmentShader);
            groupShaders.put(fragmentShader, shader);
        }
        shader.glShader.useProgram();

        mGLCubeBuffer.position(0);
        shader.glShader.setVertexAttribArray("position", 2, mGLCubeBuffer);
        mGLTextureBuffer.position(0);
        shader.glShader.setVertexAttribArray("inputTextureCoordinate", 2, mGLTextureBuffer);

        GLES20.glUniformMatrix4fv(shader.mTextureTransformMatrixLocation, 1, false, texMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(shader.mGLUniformTexture, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glViewport(0, 0, width, height);
        return mFrameBufferTextures[0];
    }

    @Override
    public void release() {
        for (GroupShader shader : groupShaders.values()) {
            shader.glShader.release();
        }
        groupShaders.clear();
    }
}

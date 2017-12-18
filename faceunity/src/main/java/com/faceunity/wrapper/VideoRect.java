package com.faceunity.wrapper;

import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by tujh on 2017/5/12.
 */

public class VideoRect {

    public static final float[] IDENTITY_MATRIX;

    static {
        IDENTITY_MATRIX = new float[16];
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
    }

    private float vertices[] = {
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f
    };
    private float textureVertices[] = {
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f
    };
    private FloatBuffer verticesBuffer;
    private FloatBuffer textureBuffer;
    private final String vertexShaderCode =
            "attribute vec4 aPosition;" +
                    "attribute vec2 aTexPosition;" +
                    "uniform mat4 uMVPMatrix;" +
                    "varying vec2 vTexPosition;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * aPosition;" +
                    "  vTexPosition = aTexPosition;" +
                    "}";

    private final String fragmentShaderCode =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;" +
                    "uniform sampler2D uTexture;" +
                    "uniform vec4 uFragColor;" +
                    "varying vec2 vTexPosition;" +
                    "void main() {" +
//                    "  gl_FragColor = texture2D(uTexture, vTexPosition);" +
                    "  gl_FragColor = vec4(texture2D(uTexture, vTexPosition).rgb, 1.0);" +
                    "}";

    private int vertexShader;
    private int fragmentShader;
    private int program;


    private int fboId;
    private int fboTex = -1;
    private int renderBufferId;

    public VideoRect() {
        initializeBuffers();
        initializeProgram();
    }

    private void initializeBuffers() {
        ByteBuffer buff = ByteBuffer.allocateDirect(vertices.length * 4);
        buff.order(ByteOrder.nativeOrder());
        verticesBuffer = buff.asFloatBuffer();
        verticesBuffer.put(vertices);
        verticesBuffer.position(0);

        buff = ByteBuffer.allocateDirect(textureVertices.length * 4);
        buff.order(ByteOrder.nativeOrder());
        textureBuffer = buff.asFloatBuffer();
        textureBuffer.put(textureVertices);
        textureBuffer.position(0);

    }

    private void initializeProgram() {
        vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vertexShader, vertexShaderCode);
        GLES20.glCompileShader(vertexShader);

        fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fragmentShader, fragmentShaderCode);
        GLES20.glCompileShader(fragmentShader);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);

        GLES20.glLinkProgram(program);
    }

    public void draw(int texture, int cameraFaing) {
        GLES20.glUseProgram(program);

        int positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, verticesBuffer);
        GLES20.glEnableVertexAttribArray(positionHandle);

        int texturePositionHandle = GLES20.glGetAttribLocation(program, "aTexPosition");
        GLES20.glVertexAttribPointer(texturePositionHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(texturePositionHandle);

        int textureHandle = GLES20.glGetUniformLocation(program, "uTexture");
        GLES20.glUniform1i(textureHandle, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);

        final float[] mMVPMatrix = new float[16];
        float[] scratch = new float[16];
        final float[] mProjectionMatrix = new float[16];
        final float[] mViewMatrix = new float[16];
        final float[] mRotationMatrix = new float[16];
        Matrix.frustumM(mProjectionMatrix, 0, -1, 1, -1, 1, 3, 100);
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, cameraFaing == Camera.CameraInfo.CAMERA_FACING_FRONT ? 3 : -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        Matrix.multiplyMM(scratch, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        Matrix.setRotateM(mRotationMatrix, 0, cameraFaing == Camera.CameraInfo.CAMERA_FACING_FRONT ? 90 : 270, 0, 0, 1.0f);
        Matrix.multiplyMM(mMVPMatrix, 0, scratch, 0, mRotationMatrix, 0);

        int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        //单元矩阵
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mMVPMatrix, 0);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(texturePositionHandle);
    }

    public void createFBO(int width, int height) {
        if (fboTex == -1) {
            int[] temp = new int[1];
//generate fbo id
            GLES20.glGenFramebuffers(1, temp, 0);
            fboId = temp[0];
//generate texture
            GLES20.glGenTextures(1, temp, 0);
            fboTex = temp[0];
//generate render buffer
            GLES20.glGenRenderbuffers(1, temp, 0);
            renderBufferId = temp[0];
//Bind Frame buffer
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId);
//Bind texture
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTex);
//Define texture parameters
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//Bind render buffer and define buffer dimension
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderBufferId);
            GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);
//Attach texture FBO color attachment
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, fboTex, 0);
//Attach render buffer to depth attachment
            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, renderBufferId);
//we are done, reset
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }
    }

    public void glBindFramebufferFBOId() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId);
    }

    public int getFboTex() {
        return fboTex;
    }
}

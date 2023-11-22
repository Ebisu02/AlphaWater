package com.example.alphawater;

import android.opengl.GLES20;

import java.nio.FloatBuffer;

public class ShaderUtil {
    private int program_handle;
    private int backgroundTextureHandle;

    public ShaderUtil(String vertex_shader, String fragment_shader) {
        createProgram(vertex_shader, fragment_shader);
    }

    private void createProgram(String vertex_shader, String fragment_shader) {
        int vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, vertex_shader);
        int fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragment_shader);

        program_handle = GLES20.glCreateProgram();
        GLES20.glAttachShader(program_handle, vertexShaderHandle);
        GLES20.glAttachShader(program_handle, fragmentShaderHandle);
        GLES20.glLinkProgram(program_handle);
    }

    private int compileShader(int type, String shaderCode) {
        int shaderHandle = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shaderHandle, shaderCode);
        GLES20.glCompileShader(shaderHandle);
        return shaderHandle;
    }

    public void linkVertexBuffer(FloatBuffer vertexBuffer) {
        GLES20.glUseProgram(program_handle);
        int a_vertex_handle = GLES20.glGetAttribLocation(program_handle, "a_vertex");
        GLES20.glEnableVertexAttribArray(a_vertex_handle);
        GLES20.glVertexAttribPointer(a_vertex_handle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
    }

    public void linkNormalBuffer(FloatBuffer normalBuffer) {
        GLES20.glUseProgram(program_handle);
        int a_normal_handle = GLES20.glGetAttribLocation(program_handle, "a_normal");
        GLES20.glEnableVertexAttribArray(a_normal_handle);
        GLES20.glVertexAttribPointer(a_normal_handle, 3, GLES20.GL_FLOAT, false, 0, normalBuffer);
    }

    public void linkModelViewProjectionMatrix(float[] modelViewProjectionMatrix) {
        GLES20.glUseProgram(program_handle);
        int u_model_view_projection_matrix_handle = GLES20.glGetUniformLocation(program_handle,
                "u_modelViewProjectionMatrix");
        GLES20.glUniformMatrix4fv(u_model_view_projection_matrix_handle, 1, false, modelViewProjectionMatrix, 0);
    }

    public void linkCamera(float xCamera, float yCamera, float zCamera) {
        GLES20.glUseProgram(program_handle);
        int u_camera_handle = GLES20.glGetUniformLocation(program_handle, "u_camera");
        GLES20.glUniform3f(u_camera_handle, xCamera, yCamera, zCamera);
    }

    public void linkLightSource(float xLightPosition, float yLightPosition, float zLightPosition) {
        GLES20.glUseProgram(program_handle);
        int u_light_source_handle = GLES20.glGetUniformLocation(program_handle, "u_lightPosition");
        GLES20.glUniform3f(u_light_source_handle, xLightPosition, yLightPosition, zLightPosition);
    }

    public void linkTexture(int[] texture) {
        int u_texture_Handle = GLES20.glGetUniformLocation(program_handle, "u_texture0");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        GLES20.glUniform1i(u_texture_Handle, 0);
    }

    public void linkBackgroundTexture(int texture) {
        backgroundTextureHandle = texture;
        int u_background_texture_Handle = GLES20.glGetUniformLocation(program_handle, "u_backgroundTexture");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);  // Используйте другое текстурное юнит (1), если требуется
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, backgroundTextureHandle);
        GLES20.glUniform1i(u_background_texture_Handle, 1);  // Установите текстурный юнит на 1
    }

    public void useProgram() {
        GLES20.glUseProgram(program_handle);
    }
}
package com.example.alphawater;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
class OpenGLRenderer implements GLSurfaceView.Renderer {
    Context context;
    private int[] texture = new int [2];

    private float xCamera = 3f;
    private float yCamera = 3f;
    private float zCamera = 0f;
    private float xLightPosition = 10f;
    private float yLightPosition = 10f;
    private float zLightPosition = 10f;

    private float[] modelMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] modelViewMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] modelViewProjectionMatrix = new float[16];

    private int maxSizeX = 100;
    private int maxSizeZ = 100;
    private int sizeIndex;

    private float x0 =- 3.0f;
    private float z0 = -3.0f;
    private float dx = 0.05f;
    private float dz = 0.05f;

    private float[] x = new float [maxSizeX + 1];
    private float[][] y = new float [maxSizeZ + 1][maxSizeX + 1];
    private float[] z = new float [maxSizeZ + 1];
    private float[] vertexes  = new float [(maxSizeZ + 1) * (maxSizeX + 1) * 3];
    private float[][] normalsX = new float [maxSizeZ + 1][maxSizeX + 1];
    private float [][] normalsY = new float [maxSizeZ + 1][maxSizeX + 1];
    private float [][] normalsZ = new float [maxSizeZ + 1][maxSizeX + 1];
    private float [] normals = new float [(maxSizeZ + 1) * (maxSizeX + 1) * 3];

    private FloatBuffer floatBuffer;
    private FloatBuffer normalsBuffer;
    private ShortBuffer indexBuffer;

    private ShaderUtil shader;
    private String fragmentShader =
            "precision mediump float;" +
                    "uniform vec3 u_camera;" +
                    "uniform vec3 u_lightPosition;" +
                    "uniform sampler2D u_texture0;" +
                    "uniform sampler2D u_backgroundTexture;" +  // Текстура для фона
                    "varying vec3 v_vertex;" +
                    "varying vec3 v_normal;" +
                    "vec3 myrefract(vec3 IN, vec3 NORMAL, float k) {" +
                    " float nv = dot(NORMAL,IN);" +
                    " float v2 = dot(IN,IN);" +
                    " float knormal = (sqrt(((k * k - 1.0) * v2) / (nv * nv) + 1.0) - 1.0) * nv;" +
                    " vec3 OUT = IN + (knormal * NORMAL);" +
                    " return OUT;" +
                    "}" +
                    "void main() {" +
                    " vec3 n_normal = normalize(v_normal);" +
                    " vec3 lightvector = normalize(u_lightPosition - v_vertex);" +
                    " vec3 lookvector = normalize(u_camera - v_vertex);" +
                    " float ambient = 0.2;" +
                    " float k_diffuse = 0.8;" +
                    " float k_specular = 0.7;" +
                    " float diffuse = k_diffuse * max(dot(n_normal, lightvector), 0.0);" +
                    " vec3 reflectvector = reflect(-lightvector, n_normal);" +
                    " float specular = k_specular * pow( max(dot(lookvector,reflectvector),0.0), 40.0);" +
                    " vec4 one = vec4(1.0,1.0,1.0,1.0);" +
                    " vec4 lightColor = (ambient + diffuse + specular) * one;" +
                    " vec3 OUT = myrefract(-lookvector, n_normal, 1.2);" +
                    " float ybottom = -1.0;" +
                    " float xbottom = v_vertex.x + OUT.x * (ybottom - v_vertex.y) / OUT.y;" +
                    " float zbottom = v_vertex.z + OUT.z * (ybottom - v_vertex.y) / OUT.y;" +
                    " vec2 texCoord = vec2(xbottom, zbottom);" +
                    " vec4 waterColor = texture2D(u_texture0, texCoord);" +
                    " vec4 backgroundColor = texture2D(u_backgroundTexture, texCoord);"+  // Получите цвет фона из текстуры фона
                    " gl_FragColor = mix(backgroundColor, waterColor, waterColor.a * 0.75) * lightColor;"+  // Используйте mix для объединения фона и воды с учетом альфа-канала воды
                    "}";
    private String vertexShader =
            "uniform mat4 u_modelViewProjectionMatrix;"+
                    "attribute vec3 a_vertex;"+
                    "attribute vec3 a_normal;"+
                    "varying vec3 v_vertex;"+
                    "varying vec3 v_normal;"+
                    "void main() {"+
                    "v_vertex = a_vertex;"+
                    "vec3 n_normal = normalize(a_normal);"+
                    "v_normal = n_normal;"+
                    "gl_Position = u_modelViewProjectionMatrix * vec4(a_vertex, 1.0);"+
                    "}";

    public OpenGLRenderer(Context context) {
        this.context = context;
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setLookAtM(viewMatrix, 0, xCamera, yCamera, zCamera, 0, 0, 0, 0, 1, 0);
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);

        for (int i = 0; i <= maxSizeX; i++) {
            x[i] = x0 + i * dx;
        }

        for (int j = 0; j <= maxSizeZ; j++) {
            z[j] = z0 + j * dz;
        }

        ByteBuffer vb = ByteBuffer.allocateDirect((maxSizeZ + 1) * (maxSizeX + 1) * 3 * 4);
        vb.order(ByteOrder.nativeOrder());
        floatBuffer = vb.asFloatBuffer();
        floatBuffer.position(0);

        ByteBuffer nb = ByteBuffer.allocateDirect((maxSizeZ + 1) * (maxSizeX + 1) * 3 * 4);
        nb.order(ByteOrder.nativeOrder());
        normalsBuffer = nb.asFloatBuffer();
        normalsBuffer.position(0);

        short[] index;
        sizeIndex = 2 * (maxSizeX + 1) * maxSizeZ + (maxSizeZ - 1);
        index = new short [sizeIndex];
        int k = 0;
        int j = 0;
        while (j < maxSizeZ) {
            for (int i = 0; i <= maxSizeX; i++) {
                index[k] = chain(j, i);
                k++;
                index[k] = chain(j + 1, i);
                k++;
            }
            if (j < maxSizeZ - 1) {
                index[k] = chain(j + 1, maxSizeX);
                k++;
            }
            j++;
            if (j < maxSizeZ) {
                for (int i = maxSizeX; i >= 0; i--) {
                    index[k] = chain(j, i);
                    k++;
                    index[k] = chain(j + 1, i);
                    k++;
                }
                if (j < maxSizeZ - 1) {
                    index[k] = chain(j + 1,0);
                    k++;
                }
                j++;
            }
        }
        ByteBuffer bi = ByteBuffer.allocateDirect(sizeIndex * 2);
        bi.order(ByteOrder.nativeOrder());
        indexBuffer = bi.asShortBuffer();
        indexBuffer.put(index);
        indexBuffer.position(0);
        get_vertexes();
        get_normales();
    }

    private short chain(int j, int i) {
        return (short) (i + j * (maxSizeX + 1));
    }

    private void get_vertexes() {
        double time = System.currentTimeMillis();
        for (int j = 0; j <= maxSizeZ; j++) {
            for (int i = 0; i <= maxSizeX; i++){
                y[j][i] = 0.02f * (float) Math.cos(0.005 * time + 5 * (z[j] + x[i]));
            }
        }
        int k = 0;
        for (int j = 0; j <= maxSizeZ; j++) {
            for (int i = 0; i <= maxSizeX; i++) {
                vertexes[k] = x[i];
                k++;
                vertexes[k] = y[j][i];
                k++;
                vertexes[k] = z[j];
                k++;
            }
        }
        floatBuffer.put(vertexes);
        floatBuffer.position(0);
    }

    private void get_normales() {
        for (int j = 0; j < maxSizeZ; j++) {
            for (int i = 0; i < maxSizeX; i++) {
                normalsX[j][i] = -(y[j][i+1] - y[j][i]) * dz;
                normalsY[j][i] = dx * dz;
                normalsZ[j][i] = -dx * (y[j+1][i] - y[j][i]);
            }
        }
        for (int j = 0; j < maxSizeZ; j++) {
            normalsX[j][maxSizeX] = (y[j][maxSizeX - 1] - y[j][maxSizeX]) * dz;
            normalsY[j][maxSizeX] = dx * dz;
            normalsZ[j][maxSizeX] = -dx * (y[j + 1][maxSizeX] - y[j][maxSizeX]);
        }
        for (int i = 0; i < maxSizeX; i++) {
            normalsX[maxSizeZ][i] = -(y[maxSizeZ][i + 1] - y[maxSizeZ][i]) * dz;
            normalsY[maxSizeZ][i] = dx * dz;
            normalsZ[maxSizeZ][i] = dx * (y[maxSizeZ - 1][i] - y[maxSizeZ][i]);
        }
        normalsX[maxSizeZ][maxSizeX]= (y[maxSizeZ][maxSizeX - 1] - y[maxSizeZ][maxSizeX]) * dz;
        normalsY[maxSizeZ][maxSizeX] = dx * dz;
        normalsZ[maxSizeZ][maxSizeX] = dx * (y[maxSizeZ - 1][maxSizeX] - y[maxSizeZ][maxSizeX]);
        int k = 0;
        for (int j = 0; j <= maxSizeZ; j++) {
            for (int i = 0; i <= maxSizeX; i++) {
                normals[k] = normalsX[j][i];
                k++;
                normals[k] = normalsY[j][i];
                k++;
                normals[k] = normalsZ[j][i];
                k++;
            }
        }
        normalsBuffer.put(normals);
        normalsBuffer.position(0);
    }

    @SuppressLint("ResourceType")
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glGenTextures(1, texture, 0);
        InputStream stream;
        Bitmap bitmap;
        stream = context.getResources().openRawResource(R.drawable.water);
        bitmap = BitmapFactory.decodeStream(stream);

        gl.glBindTexture(GL10.GL_TEXTURE_2D, texture[0]);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
                GL10.GL_LINEAR);
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        //int backgroundTexture = loadBackgroundTexture(R.drawable.sand);
        int backgroundTexture = loadBackgroundTexture(R.drawable.bg);

        shader = new ShaderUtil(vertexShader, fragmentShader);
        shader.linkVertexBuffer(floatBuffer);
        shader.linkNormalBuffer(normalsBuffer);
        shader.linkTexture(texture);
        shader.linkBackgroundTexture(backgroundTexture);
    }

    private int loadBackgroundTexture(int resourceId) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

        InputStream stream;
        Bitmap bitmap;
        stream = context.getResources().openRawResource(resourceId);
        bitmap = BitmapFactory.decodeStream(stream);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();

        return textures[0];
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        float k = 0.055f;
        float left = -k * ratio;
        float right = k * ratio;
        float bottom = -k;
        float top = k;
        float near = 0.1f;
        float far = 10.0f;

        Matrix.frustumM(projectionMatrix, 0, left, right, bottom, top, near, far);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        shader.linkModelViewProjectionMatrix(modelViewProjectionMatrix);
        shader.linkCamera(xCamera, yCamera, zCamera);
        shader.linkLightSource(xLightPosition, yLightPosition, zLightPosition);
        get_vertexes();
        get_normales();
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, sizeIndex, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
    }
}
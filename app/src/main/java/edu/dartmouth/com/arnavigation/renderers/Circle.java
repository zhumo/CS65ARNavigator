package edu.dartmouth.com.arnavigation.renderers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.lang.Math;

import javax.microedition.khronos.opengles.GL10;

import edu.dartmouth.com.arnavigation.R;

public class Circle {

    private String TAG = Circle.class.getSimpleName();

    private final String VertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +

                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    // the matrix must be included as a modifier of gl_Position
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    private final String projectedVertexShaderCode =
            "uniform mat4 u_ModelView;" +
                    "uniform mat4 u_ModelViewProjection;" +
                    "attribute vec4 a_Position;" +
                    "attribute vec3 a_Normal;" +
                    "attribute vec2 a_TexCoord;" +
                    "varying vec3 v_ViewPosition;" +
                    "varying vec3 v_ViewNormal;" +
                    "varying vec2 v_TexCoord;" +
                    "void main(){" +
                    "  v_ViewPosition = (u_ModelView * a_Position).xyz;" +
                    "  v_ViewNormal = normalize((u_ModelView * vec4(a_Normal, 0.0)).xyz);" +
                    "  v_TexCoord = a_TexCoord;" +
                    "  gl_Position = u_ModelViewProjection * a_Position;" +
                    "}";

    private final String FragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    protected int mProgram;
    protected int mPositionHandle;
    protected int mColorHandle;
    protected int mMVPMatrixHandle;

    private FloatBuffer vertexBuffer;
    private int vertexBufferId;
    private int vertexCount;

    private IntBuffer drawOrderBuffer;
    private int drawOrderBufferId;
    private int drawOrderCount;

    // number of coordinates per vertex in this array
    static final int NUM_POINTS = 360;
    static final int DEGS_PER_CIRCLE = 360;
    static final int DEGS_PER_POINT = DEGS_PER_CIRCLE / NUM_POINTS;
    static final int COORDS_PER_VERTEX = 3;
    static final int VERTEX_BYTE_SIZE = COORDS_PER_VERTEX * 4;
    private float circleCoords[] = new float[NUM_POINTS * COORDS_PER_VERTEX];

    private int drawOrder[] = new int[COORDS_PER_VERTEX * (NUM_POINTS - 2)];

    private float[] fillColor;

    private float[] mModelMatrix = new float[16];
    private float[] mModelViewMatrix = new float[16];
    private float[] mModelViewProjectionMatrix = new float[16];
    private int mModelViewUniform;
    private int mModelViewProjectionUniform;
    private int mLightingParametersUniform;
    private int mMaterialParametersUniform;

    private float[] mViewLightDirection = new float[4];
    private static final float[] LIGHT_DIRECTION = new float[]{0.250f, 0.866f, 0.433f, 0.0f};

    private float mAmbient = 0.3f;
    private float mDiffuse = 1.0f;
    private float mSpecular = 1.0f;
    private float mSpecularPower = 6.0f;

    public Circle() {
        for(int i = 0; i < NUM_POINTS; i++) {
            float radians = (float) (i * DEGS_PER_POINT * 180.0f / Math.PI);
            circleCoords[i * 3] = (float) Math.sin(radians);
            circleCoords[i * 3 + 1] = (float) Math.cos(radians);
            circleCoords[i * 3 + 2] = 0.0f;
        }

        // (# of coordinate values * 4 bytes per float)
        ByteBuffer bb = ByteBuffer.allocateDirect(circleCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(circleCoords);
        vertexBuffer.position(0);

        // Triangle Fan creates n-2 triangles, where n is the number of points.
        for(int i = 0; i < (NUM_POINTS - 2); i++) {
            drawOrder[i * 3] = 0;
            drawOrder[i * 3 + 1] = i + 1;
            drawOrder[i * 3 + 2] = i + 2;
        }

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 4);
        dlb.order(ByteOrder.nativeOrder());
        drawOrderBuffer = dlb.asIntBuffer();
        drawOrderBuffer.put(drawOrder);
        drawOrderBuffer.position(0);
    }

    //creates a Line given 6 vertices and 4 color values
    public void createOnGLThread(float[] color, Context context) {

        //color should be a float[4] -> r,g,b,a
        if (color.length != 4) {
            Log.d("LINE_INVALID", "Line Color must have 4 values");
        } else {
            this.fillColor = color;
        }

        this.fillColor = color;

        //get shader references
        int vertexShader = loadShader(
                GLES20.GL_VERTEX_SHADER, projectedVertexShaderCode);
        int fragmentShader = ShaderUtil.loadGLShader(TAG, context,
                GLES20.GL_FRAGMENT_SHADER, R.raw.object_fragment);

        //create buffer references
        int[] buffers = new int[2];
        GLES20.glGenBuffers(2, buffers, 0);
        vertexBufferId = buffers[0];
        drawOrderBufferId = buffers[1];


        mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // creates OpenGL ES program executables

        mModelViewUniform = GLES20.glGetUniformLocation(mProgram, "u_ModelView");
        mModelViewProjectionUniform = GLES20.glGetUniformLocation(mProgram, "u_ModelViewProjection");

        mLightingParametersUniform = GLES20.glGetUniformLocation(mProgram, "u_LightingParameters");
        mMaterialParametersUniform = GLES20.glGetUniformLocation(mProgram, "u_MaterialParameters");

        Matrix.setIdentityM(mModelMatrix, 0);
    }

    public void drawWithIndices(float[] cameraView, float[] cameraPerspective, float lightIntensity) {

        ShaderUtil.checkGLError("BEFORE_DRAW", "Before draw");

        // Build the ModelView and ModelViewProjection matrices
        // for calculating object position and light.
        Matrix.multiplyMM(mModelViewMatrix, 0, cameraView, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mModelViewProjectionMatrix, 0, cameraPerspective, 0, mModelViewMatrix, 0);

        GLES20.glUseProgram(mProgram);


        // Set the vertex attributes.
        //GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId);
        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
        // GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        // Set the vertex attributes.
        GLES20.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_BYTE_SIZE, vertexBuffer);

        // Set the lighting environment properties.
        Matrix.multiplyMV(mViewLightDirection, 0, mModelViewMatrix, 0, LIGHT_DIRECTION, 0);
        normalizeVec3(mViewLightDirection);
        GLES20.glUniform4f(mLightingParametersUniform,
                mViewLightDirection[0], mViewLightDirection[1], mViewLightDirection[2], lightIntensity);

        // Set the object material properties.
        GLES20.glUniform4f(mMaterialParametersUniform, mAmbient, mDiffuse, mSpecular,
                mSpecularPower);


        // Set the ModelViewProjection matrix in the shader.
        GLES20.glUniformMatrix4fv(
                mModelViewUniform, 1, false, mModelViewMatrix, 0);
        GLES20.glUniformMatrix4fv(
                mModelViewProjectionUniform, 1, false, mModelViewProjectionMatrix, 0);

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);


        // Draw the lines
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, drawOrderBufferId);
        GLES20.glDrawElements(GLES20.GL_LINES, drawOrderCount, GLES20.GL_UNSIGNED_SHORT, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        GLES20.glLineWidth(5.0f);


        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);


        ShaderUtil.checkGLError("AFTER_DRAW", "After draw");
    }

    private static int loadShader(int type, String shaderCode) {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    private static void normalizeVec3(float[] v) {
        float reciprocalLength = 1.0f / (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        v[0] *= reciprocalLength;
        v[1] *= reciprocalLength;
        v[2] *= reciprocalLength;
    }

    public void updateModelMatrix(float[] modelMatrix, float scaleFactor) {
        float[] scaleMatrix = new float[16];
        Matrix.setIdentityM(scaleMatrix, 0);
        scaleMatrix[0] = scaleFactor;
        scaleMatrix[5] = scaleFactor;
        scaleMatrix[10] = scaleFactor;
        Matrix.multiplyMM(mModelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
    }
}

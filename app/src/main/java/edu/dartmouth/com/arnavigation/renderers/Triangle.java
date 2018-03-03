package edu.dartmouth.com.arnavigation.renderers;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import java.lang.String;

import edu.dartmouth.com.arnavigation.R;

/**
 * Created by jctwake on 3/2/18.
 */

public class Triangle {

    private final String vertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    // the matrix must be included as a modifier of gl_Position
                    // Note that the uMVPMatrix factor *must be first* in order
                    // for the matrix multiplication product to be correct.
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
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

    private FloatBuffer vertexBuffer;
    private int mProgram;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;

    // Shader location: object attributes.
    private int mPositionAttribute;
    private int mNormalAttribute;
    private int mTexCoordAttribute;

    // Shader location: texture sampler.
    private int mTextureUniform;

    // Shader location: environment properties.
    private int mLightingParametersUniform;

    // Shader location: material properties.
    private int mMaterialParametersUniform;

    private float[] mModelMatrix = new float[16];
    private float[] mModelViewMatrix = new float[16];
    private float[] mModelViewProjectionMatrix = new float[16];

    private int mModelViewUniform;
    private int mModelViewProjectionUniform;

    // Set some default material properties to use for lighting.
    private float mAmbient = 0.3f;
    private float mDiffuse = 1.0f;
    private float mSpecular = 1.0f;
    private float mSpecularPower = 6.0f;

    // Note: the last component must be zero to avoid applying the translational part of the matrix.
    private static final float[] LIGHT_DIRECTION = new float[] { 0.250f, 0.866f, 0.433f, 0.0f };
    private float[] mViewLightDirection = new float[4];

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static float triangleCoords[] = {
            // in counterclockwise order:
            0.0f,  0.01f, 0.49f,   // top
            -0.01f, -0f, 0.0f,   // bottom left
            0.01f, -0f, 0.0f    // bottom right
    };
    private final int vertexCount = triangleCoords.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    float color[] = { 0.63671875f, 0.76953125f, 0.22265625f, 0.0f };

    private String TAG = ObjectRenderer.class.getSimpleName();

    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     */
    public Triangle() {


    }

    public void createOnGLThread(Context context) {

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                triangleCoords.length * 4);
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        vertexBuffer.put(triangleCoords);
        // set the buffer to read the first coordinate
        bb.position();

        // prepare shaders and OpenGL program
        int vertexShader = loadShader(
                GLES20.GL_VERTEX_SHADER, projectedVertexShaderCode);
        int fragmentShader = ShaderUtil.loadGLShader(TAG, context,
                GLES20.GL_FRAGMENT_SHADER, R.raw.object_fragment);



        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables

        mModelViewUniform = GLES20.glGetUniformLocation(mProgram, "u_ModelView");
        mModelViewProjectionUniform = GLES20.glGetUniformLocation(mProgram, "u_ModelViewProjection");

        mLightingParametersUniform = GLES20.glGetUniformLocation(mProgram, "u_LightingParameters");
        mMaterialParametersUniform = GLES20.glGetUniformLocation(mProgram, "u_MaterialParameters");

        Matrix.setIdentityM(mModelMatrix, 0);

    }

    /**
     * Encapsulates the OpenGL ES instructions for drawing this shape.
     *
     * @param mvpMatrix - The Model View Project matrix in which to draw
     * this shape.
     */
    public void draw(float[] mvpMatrix) {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        ShaderUtil.checkGLError("CHECK_UNIFORM", "glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        ShaderUtil.checkGLError("CHECK_UNIFORM", "glUniformMatrix4fv");

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

    public void draw(float[] cameraView, float[] cameraPerspective, float lightIntensity) {

        ShaderUtil.checkGLError("BEFORE_DRAW", "Before draw");

        // Build the ModelView and ModelViewProjection matrices
        // for calculating object position and light.
        Matrix.multiplyMM(mModelViewMatrix, 0, cameraView, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mModelViewProjectionMatrix, 0, cameraPerspective, 0, mModelViewMatrix, 0);

        GLES20.glUseProgram(mProgram);


        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");

        GLES20.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

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

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);


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

    public void updateModelMatrix(float[] modelMatrix, float scaleFactor) {
        float[] scaleMatrix = new float[16];
        Matrix.setIdentityM(scaleMatrix, 0);
        scaleMatrix[0] = scaleFactor;
        scaleMatrix[5] = scaleFactor;
        scaleMatrix[10] = scaleFactor;
        Matrix.multiplyMM(mModelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
    }


    private static void normalizeVec3(float[] v) {
        float reciprocalLength = 1.0f / (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        v[0] *= reciprocalLength;
        v[1] *= reciprocalLength;
        v[2] *= reciprocalLength;
    }
}

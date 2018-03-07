package edu.dartmouth.com.arnavigation.renderers;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import java.lang.String;
import java.nio.ShortBuffer;

/**
 * Created by jctwake on 2/28/18.
 */

public class Line {

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

    private static final int VERTEX_SIZE = 3; //3 values per vertex: x, y, z
    private static final int VERTEX_BYTE_SIZE = VERTEX_SIZE * 4; //4 bytes per vertex

    private int vertexCount;
    private int indexCount;

    private FloatBuffer vertexBuffer;
    private int vertexBufferId;

    private ShortBuffer indexBuffer;
    private int indexBufferId;

    private float[] lineVertices;
    private short[] lineIndices;
    private float[] lineColor;

    // Temporary matrices allocated here to reduce number of allocations for each frame.
    private float[] mModelMatrix = new float[16];
    private float[] mModelViewMatrix = new float[16];
    private float[] mModelViewProjectionMatrix = new float[16];

    private int mModelViewUniform;
    private int mModelViewProjectionUniform;

    // Shader location: environment properties.
    private int mLightingParametersUniform;

    // Shader location: material properties.
    private int mMaterialParametersUniform;


    // Set some default material properties to use for lighting.
    private float mAmbient = 0.3f;
    private float mDiffuse = 1.0f;
    private float mSpecular = 1.0f;
    private float mSpecularPower = 6.0f;

    //line width
    private float LINE_WIDTH = 100.0f;

    // Note: the last component must be zero to avoid applying the translational part of the matrix.
    private static final float[] LIGHT_DIRECTION = new float[]{0.250f, 0.866f, 0.433f, 0.0f};
    private float[] mViewLightDirection = new float[4];

    private String TAG = Line.class.getSimpleName();

    public Line() {
    }

    //creates a Line given 6 vertices and 4 color values
    public void createOnGLThread(float[] color, Context context) {

        //color should be a float[4] -> r,g,b,a
        if (color.length != 4) {
            Log.d("LINE_INVALID", "Line Color must have 4 values");
        } else {
            lineColor = color;
        }

        lineColor = color;

        //get shader references
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, projectedVertexShaderCode);
        //int fragmentShader = ShaderUtil.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, R.raw.object_fragment);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FragmentShaderCode);

        //create buffer references
        int[] buffers = new int[2];
        GLES20.glGenBuffers(2, buffers, 0);
        vertexBufferId = buffers[0];
        indexBufferId = buffers[1];


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

    public Line(float[] color) { //use this constructor for hardcoded vertices
        lineColor = color;
        lineVertices = new float[]{
                -1.0f, -1.0f, 0,
                -1.0f, 1.0f, 0,
                1.0f, -1.0f, 0,
                1.0f, 1.0f, 0,
                1.0f, 1.0f, 10.0f
        };

        vertexCount = lineVertices.length / VERTEX_SIZE; //should be 2

        //create FloatBuffer from ByteBuffer, set position to 0
        ByteBuffer bb = ByteBuffer.allocateDirect(lineVertices.length * 4); //4 bytes per float
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(lineVertices);
        vertexBuffer.position(0);

        //get shader references
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FragmentShaderCode);


        mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);

    }

    public void setLineVertices(float[] vertices) {

        lineVertices = vertices;

        vertexCount = lineVertices.length / VERTEX_SIZE; //should be 2

        //create FloatBuffer from ByteBuffer, set position to 0
        ByteBuffer bb = ByteBuffer.allocateDirect(lineVertices.length * 4); //4 bytes per float
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(lineVertices);
        vertexBuffer.position(0);

        //load into GL buffer
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.length * 4, vertexBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

    }

    public void setLineIndices(short[] indices) {

        lineIndices = indices;
        indexCount = lineIndices.length / 2;

        ByteBuffer bbi = ByteBuffer.allocateDirect(indices.length * 2); //2 bytes per short
        bbi.order(ByteOrder.nativeOrder());
        indexBuffer = bbi.asShortBuffer();
        indexBuffer.put(indices);
        indexBuffer.position(0);

        //load into GL element buffer
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexCount * 2, indexBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void draw(float[] mvpMatrix) {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
                mPositionHandle, VERTEX_SIZE,
                GLES20.GL_FLOAT, false,
                VERTEX_BYTE_SIZE, vertexBuffer);

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, lineColor, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        //ShaderUtil.checkGLError("CHECK_UNIFORM", "glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        //ShaderUtil.checkGLError("CHECK_UNIFORM", "glUniformMatrix4fv");

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertexCount);
        GLES20.glLineWidth(5.0f);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
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
                mPositionHandle, VERTEX_SIZE, GLES20.GL_FLOAT, false, VERTEX_BYTE_SIZE, vertexBuffer);

        // Set the lighting environment properties.
//        Matrix.multiplyMV(mViewLightDirection, 0, mModelViewMatrix, 0, LIGHT_DIRECTION, 0);
//        normalizeVec3(mViewLightDirection);
//        GLES20.glUniform4f(mLightingParametersUniform,
//                mViewLightDirection[0], mViewLightDirection[1], mViewLightDirection[2], lightIntensity);
//
//        // Set the object material properties.
//        GLES20.glUniform4f(mMaterialParametersUniform, mAmbient, mDiffuse, mSpecular,
//                mSpecularPower);

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, lineColor, 0);

        // Set the ModelViewProjection matrix in the shader.
        GLES20.glUniformMatrix4fv(
                mModelViewUniform, 1, false, mModelViewMatrix, 0);
        GLES20.glUniformMatrix4fv(
                mModelViewProjectionUniform, 1, false, mModelViewProjectionMatrix, 0);

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);


        // Draw the lines
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
        GLES20.glDrawElements(GLES20.GL_LINES, indexCount, GLES20.GL_UNSIGNED_SHORT, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        GLES20.glLineWidth(5.0f);


        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);


        ShaderUtil.checkGLError("AFTER_DRAW", "After draw");
    }




    public void drawNoIndices(float[] cameraView, float[] cameraPerspective, float lightIntensity) {

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
                mPositionHandle, VERTEX_SIZE, GLES20.GL_FLOAT, false, VERTEX_BYTE_SIZE, vertexBuffer);

        // Set the lighting environment properties.
//        Matrix.multiplyMV(mViewLightDirection, 0, mModelViewMatrix, 0, LIGHT_DIRECTION, 0);
//        normalizeVec3(mViewLightDirection);
//        GLES20.glUniform4f(mLightingParametersUniform,
//                mViewLightDirection[0], mViewLightDirection[1], mViewLightDirection[2], lightIntensity);
//
//        // Set the object material properties.
//        GLES20.glUniform4f(mMaterialParametersUniform, mAmbient, mDiffuse, mSpecular,
//                mSpecularPower);

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, lineColor, 0);


        // Set the ModelViewProjection matrix in the shader.
        GLES20.glUniformMatrix4fv(
                mModelViewUniform, 1, false, mModelViewMatrix, 0);
        GLES20.glUniformMatrix4fv(
                mModelViewProjectionUniform, 1, false, mModelViewProjectionMatrix, 0);

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);


        // Draw the lines
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertexCount);
        GLES20.glLineWidth(LINE_WIDTH);


        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);


        ShaderUtil.checkGLError("AFTER_DRAW", "After draw");
    }

    public void updateModelMatrix(float[] modelMatrix, float scaleFactor, float rotation) {

        if (rotation != 0){
            Matrix.rotateM(mModelMatrix, 0, rotation, 0f, 1f, 0f);
        }

        float[] scaleMatrix = new float[16];
        Matrix.setIdentityM(scaleMatrix, 0);
        scaleMatrix[0] = scaleFactor;
        scaleMatrix[5] = scaleFactor;
        scaleMatrix[10] = scaleFactor;
        Matrix.multiplyMM(mModelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
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

    public void printLine(){


        for (int i = 0; i < vertexCount - 1; i++) {
            int h = i + 1;
            int j = i*3;
            int k = h * 3;
            int l = i*2;
            int m = l+1;

            String vertexString = "Vertex " + i + "(" + lineVertices[j] + ", " + lineVertices[j+1] + ", " + lineVertices[j+2] + ") \n" +
                    "Vertex " + h + "(" + lineVertices[k] + ", " + lineVertices[k+1] + ", " + lineVertices[k+2] + ")";

            Log.d("Vertex Connection", vertexString);

            String indexString = "Vertex " + lineIndices[l] + " connected to Vertex " + lineIndices[m] + "\n Connection " + h + "/" + indexCount;
            Log.d("Index Connection", indexString);
        }


//        Log.d("LINE_VERTICES", "First vertex: "+ lineVertices[0] + ", " + lineVertices[1] + ", " + lineVertices[2]
//                + "\n Second vertex: " + lineVertices[3] + ", " + lineVertices[4] + ", " + lineVertices[5]);
    }

    public int getSize(){
        return vertexCount;
    }
}

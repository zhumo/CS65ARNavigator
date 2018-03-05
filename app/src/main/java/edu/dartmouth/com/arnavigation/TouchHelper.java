package edu.dartmouth.com.arnavigation;

import android.opengl.Matrix;

import edu.dartmouth.com.arnavigation.math.Ray;
import edu.dartmouth.com.arnavigation.math.Vector2f;
import edu.dartmouth.com.arnavigation.math.Vector3f;

public class TouchHelper {
    public static Vector3f GetWorldCoords(Vector2f touchPoint, float screenWidth, float screenHeight, float[] projectionMatrix, float[] viewMatrix) {
        Ray touchRay = projectRay(touchPoint, screenWidth, screenHeight, projectionMatrix, viewMatrix);
        touchRay.direction.scale(0.5f);
        touchRay.origin.add(touchRay.direction);
        return touchRay.origin;
    }

    public static Ray projectRay(Vector2f touchPoint, float screenWidth, float screenHeight, float[] projectionMatrix, float[] viewMatrix) {
        float[] viewProjMtx = new float[16];
        Matrix.multiplyMM(viewProjMtx, 0, projectionMatrix, 0, viewMatrix, 0);
        Ray worldRay = screenPointToRay(touchPoint, new Vector2f(screenWidth, screenHeight), viewProjMtx);
        return worldRay;
    }

    public static Ray screenPointToRay(Vector2f point, Vector2f viewportSize, float[] viewProjMtx) {
        point.y = viewportSize.y - point.y;
        float x = point.x * 2.0F / viewportSize.x - 1.0F;
        float y = point.y * 2.0F / viewportSize.y - 1.0F;
        float[] farScreenPoint = new float[]{x, y, 1.0F, 1.0F};
        float[] nearScreenPoint = new float[]{x, y, -1.0F, 1.0F};
        float[] nearPlanePoint = new float[4];
        float[] farPlanePoint = new float[4];
        float[] invertedProjectionMatrix = new float[16];
        Matrix.setIdentityM(invertedProjectionMatrix, 0);
        Matrix.invertM(invertedProjectionMatrix, 0, viewProjMtx, 0);
        Matrix.multiplyMV(nearPlanePoint, 0, invertedProjectionMatrix, 0, nearScreenPoint, 0);
        Matrix.multiplyMV(farPlanePoint, 0, invertedProjectionMatrix, 0, farScreenPoint, 0);
        Vector3f direction = new Vector3f(farPlanePoint[0] / farPlanePoint[3], farPlanePoint[1] / farPlanePoint[3], farPlanePoint[2] / farPlanePoint[3]);
        Vector3f origin = new Vector3f(new Vector3f(nearPlanePoint[0] / nearPlanePoint[3], nearPlanePoint[1] / nearPlanePoint[3], nearPlanePoint[2] / nearPlanePoint[3]));
        direction.sub(origin);
        direction.normalize();
        return new Ray(origin, direction);
    }
}

/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.dartmouth.com.arnavigation.view_pages;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.nearby.Nearby;
import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.Trackable.TrackingState;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import edu.dartmouth.com.arnavigation.DisplayRotationHelper;
import edu.dartmouth.com.arnavigation.R;
import edu.dartmouth.com.arnavigation.location.GetNearbyPlacesRequest;
import edu.dartmouth.com.arnavigation.location.NearbyPlace;
import edu.dartmouth.com.arnavigation.renderers.BackgroundRenderer;
import edu.dartmouth.com.arnavigation.renderers.ObjectRenderer;
import edu.dartmouth.com.arnavigation.renderers.ObjectRenderer.BlendMode;
import edu.dartmouth.com.arnavigation.renderers.PlaneRenderer;
import edu.dartmouth.com.arnavigation.renderers.PointCloudRenderer;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using
 * the ARCore API. The application will display any detected planes and will allow the user to
 * tap on a plane to place a 3d model of the Android robot.
 */
public class MoCameraFragment extends Fragment implements GLSurfaceView.Renderer, SensorEventListener {
    private static final String TAG = MoCameraFragment.class.getSimpleName();

    private GLSurfaceView mSurfaceView;

    private Session mSession;
    private GestureDetector mGestureDetector;
    private DisplayRotationHelper mDisplayRotationHelper;

    private final BackgroundRenderer mBackgroundRenderer = new BackgroundRenderer();
    private final ObjectRenderer mVirtualObject = new ObjectRenderer();
    private final ObjectRenderer mVirtualObjectShadow = new ObjectRenderer();

    private final float[] mAnchorMatrix = new float[16];

    private final ArrayBlockingQueue<MotionEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(16);
    private final ArrayList<Anchor> mAnchors = new ArrayList<>();

    //location reference
    private LatLng mUserLatLng;
    private LatLng mDestination;
    private float mHeading;

    private List<NearbyPlace> nearbyPlaces;

    private float[] PLACE_MARKER_COLOR = new float[] {46, 194, 138, 1};

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] gravityData = new float[3];
    private float[] geomagneticData  = new float[3];
    private boolean hasGravityData = false;
    private boolean hasGeomagneticData = false;
    private double rotationInDegrees;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mDisplayRotationHelper = new DisplayRotationHelper(getContext());

        // Set up tap listener.
        mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                onSingleTap(e);
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });

        Exception exception = null;
        String message = null;
        try {
            mSession = new Session(getContext());
        } catch (UnavailableArcoreNotInstalledException e) {
            message = "Please install ARCore";
            exception = e;
        } catch (UnavailableApkTooOldException e) {
            message = "Please update ARCore";
            exception = e;
        } catch (UnavailableSdkTooOldException e) {
            message = "Please update this app";
            exception = e;
        } catch (Exception e) {
            message = "This device does not support AR";
            exception = e;
        }

        if (message != null) {
            showARErrorMessage(message);
            Log.e(TAG, "Exception creating session", exception);
            return;
        }

        // Create default config and check if supported.
        Config config = new Config(mSession);
        if (!mSession.isSupported(config)) {
            showARErrorMessage("This device does not support AR");
            return;
        }
        mSession.configure(config);

        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.camera_fragment, container, false);
        mSurfaceView = fragmentView.findViewById(R.id.surfaceview);

        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });

        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mSurfaceView.setRenderer(this);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mSession != null) {
            Toast.makeText(getContext(), "Loading...", Toast.LENGTH_SHORT).show();
            mSession.resume();
        }
        mSurfaceView.onResume();
        mDisplayRotationHelper.onResume();

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onPause() {
        super.onPause();
        mDisplayRotationHelper.onPause();
        mSurfaceView.onPause();
        if (mSession != null) {
            mSession.pause();
        }

        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, magnetometer);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(mSurfaceView == null) { return; }

        // When the fragment scrolls into or out of the viewpager,
        // it should act as if it were paused or resumed.
        if (isVisibleToUser) {
            onResume();
        } else {
            onPause();
        }
    }

    private void getNearbyPlaces() {
        new GetNearbyPlacesRequest(new GetNearbyPlacesRequest.OnPostExecute() {
            @Override
            public void onPostExecute(JSONObject responseJSON) {
            try {
                // Use these to debug the HTTP requests
                // Log.d("mztag", "Status: " + responseJSON.getString("status"));
                // Log.d("mztag", "Error Msg: " + responseJSON.getString("error_message"));
                if (responseJSON.getString("status").equals("OK")) {
                    JSONArray nearbyPlacesJSON = responseJSON.getJSONArray("results");

                    nearbyPlaces = new ArrayList<>();
                    for (int i = 0; i < nearbyPlacesJSON.length(); i++) {
                        JSONObject nearbyPlaceJSON = nearbyPlacesJSON.getJSONObject(i);
                        NearbyPlace nearbyPlace = new NearbyPlace(nearbyPlaceJSON);
                        nearbyPlaces.add(nearbyPlace);
                    }

                    nearbyPlacesChanged = true;
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Nearby Places Error");
                    builder.setMessage(responseJSON.getString("error_message"));
                    builder.setPositiveButton("OK", null);
                    builder.show();
                }
            } catch (JSONException e) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Nearby Places Error");
                builder.setMessage("Could not load nearby places");
                builder.setPositiveButton("OK", null);
                builder.show();
                e.printStackTrace();
            }
            }
        }).execute(mUserLatLng);
    }

    public void setUserLocation(LatLng newLatLng) {
        if (mUserLatLng == null && newLatLng != null) {
            mUserLatLng = newLatLng;
            getNearbyPlaces();
        } else {
            mUserLatLng = newLatLng;
        }
    }

    private void onSingleTap(MotionEvent e) {
        // Leaky bucket
        mQueuedSingleTaps.offer(e);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        mBackgroundRenderer.createOnGlThread(getContext());
        if (mSession != null) {
            mSession.setCameraTextureName(mBackgroundRenderer.getTextureId());
        }

        try {
            mVirtualObject.createOnGlThread(getContext(), "andy.obj", "andy.png");
            mVirtualObject.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read obj file");
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mDisplayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    private boolean nearbyPlacesChanged = false;

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mSession == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        mDisplayRotationHelper.updateSessionIfNeeded(mSession);

        try {
            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = mSession.update();
            Camera camera = frame.getCamera();

            // Handle taps. Handling only one tap per frame, as taps are usually low frequency
            // compared to frame rate.
            MotionEvent tap = mQueuedSingleTaps.poll();
            if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
//                for (HitResult hit : frame.hitTest(tap)) {
//                    // TODO: Check whether marker was tapped.
//                    Trackable trackable = hit.getTrackable();
//                    if (trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
//                    }
//
//                    mAnchors.add(hit.createAnchor());
//                    // Hits are sorted by depth. Consider only closest hit on a plane.
//                    break;
//                }
            }

            // Draw background.
            mBackgroundRenderer.draw(frame);

            if(nearbyPlacesChanged) {
                mAnchors.clear();
                for (int i = 0; i < nearbyPlaces.size(); i++) {
                    NearbyPlace nearbyPlace = nearbyPlaces.get(i);
                    Pose poseForNearbyPlace = nearbyPlace.getPose(mUserLatLng, mHeading, i);
                    Anchor nearbyPlaceAnchor = mSession.createAnchor(poseForNearbyPlace);
                    mAnchors.add(nearbyPlaceAnchor);
                }

//                NearbyPlace nearbyPlace = nearbyPlaces.get(0);
//                Log.d("mztag", "Place: " + nearbyPlace.name);
//                Log.d("mztag", "Current Loc: " + mUserLatLng.toString());
//                Log.d("mztag", "Current Heading: " + mHeading);
//                Pose poseForNearbyPlace = nearbyPlace.getPose(mUserLatLng, mHeading);
//                Pose poseForNearbyPlace = new Pose(
//                        new float[] {1.0f, 0.0f, -1.0f},
//                        new float[] {0.0f, 0.0f, 0.0f, 0.0f}
//                );
//                Anchor nearbyPlaceAnchor = mSession.createAnchor(poseForNearbyPlace);
//                mAnchors.add(nearbyPlaceAnchor);

                nearbyPlacesChanged = false;
            }

            // If not tracking, don't draw 3d objects.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            final float lightIntensity = frame.getLightEstimate().getPixelIntensity();

            // Visualize anchors created by touch.
            float scaleFactor = 1.0f;
            for (Anchor anchor : mAnchors) {
                if (anchor.getTrackingState() != TrackingState.TRACKING) {
                    continue;
                }
                // Get the current pose of an Anchor in world space. The Anchor pose is updated
                // during calls to session.update() as ARCore refines its estimate of the world.
                anchor.getPose().toMatrix(mAnchorMatrix, 0);

                mVirtualObject.updateModelMatrix(mAnchorMatrix, scaleFactor);
                mVirtualObject.draw(viewmtx, projmtx, lightIntensity);
            }

        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    private void showARErrorMessage(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Error");
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getActivity().finish();
            }
        });
        builder.show();
    }

    public void reset() {
        /* Not implemented. Should remove any path drawings. */
    }

    public void createNewDirections(List<List<HashMap<String, String>>> path){
        if (path.size() > 0) {
            //get last position latlng
            int lastPathIndex = path.size() - 1;
            List<HashMap<String, String>> lastPath = path.get(lastPathIndex);
            int lastPointIndex = lastPath.size() - 1;
            HashMap<String, String> lastPointHashMap = lastPath.get(lastPointIndex);
            mDestination = new LatLng(Double.parseDouble(lastPointHashMap.get("lat")),
                    Double.parseDouble(lastPointHashMap.get("lon")));
        }
        else {
            mDestination = mUserLatLng;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //obtained this from stack overflow
        switch (event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, gravityData, 0, 3);
                hasGravityData = true;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, geomagneticData, 0, 3);
                hasGeomagneticData = true;
                break;
            default:
                return;
        }

        if (hasGravityData && hasGeomagneticData) {
            float identityMatrix[] = new float[9];
            float rotationMatrix[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(rotationMatrix, identityMatrix,
                    gravityData, geomagneticData);

            if (success) {
                float orientationMatrix[] = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientationMatrix);
                float rotationInRadians = orientationMatrix[0];
                rotationInDegrees = (Math.toDegrees(rotationInRadians)+360)%360;

                float currentDegree = (float)rotationInDegrees;

                // do something with the rotation in degrees
                //Log.d("COMPASS", "Heading: " + currentDegree);
                mHeading = currentDegree;
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /* NOOP */ }
}
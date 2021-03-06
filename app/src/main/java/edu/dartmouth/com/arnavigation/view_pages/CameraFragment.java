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
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.GeomagneticField;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.AsyncTask;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable.TrackingState;

import edu.dartmouth.com.arnavigation.DisplayRotationHelper;
import edu.dartmouth.com.arnavigation.PlaceDetailsActivity;
import edu.dartmouth.com.arnavigation.R;
import edu.dartmouth.com.arnavigation.directions.LegObject;
import edu.dartmouth.com.arnavigation.location.NearbyPlace;
import edu.dartmouth.com.arnavigation.location.PlacesManager;
import edu.dartmouth.com.arnavigation.renderers.BackgroundRenderer;
import edu.dartmouth.com.arnavigation.renderers.Line;
import edu.dartmouth.com.arnavigation.renderers.PlaceMarker;

import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.content.Context.SENSOR_SERVICE;
import edu.dartmouth.com.arnavigation.TouchHelper;
import edu.dartmouth.com.arnavigation.math.Ray;
import edu.dartmouth.com.arnavigation.math.Vector2f;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using
 * the ARCore API. The application will display any detected planes and will allow the user to
 * tap on a plane to place a 3d model of the Android robot.
 */
public class CameraFragment extends Fragment implements GLSurfaceView.Renderer, SensorEventListener {
    private static final String TAG = CameraFragment.class.getSimpleName();

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView mSurfaceView;

    private Session mSession;
    private GestureDetector mGestureDetector;
    private DisplayRotationHelper mDisplayRotationHelper;

    private final BackgroundRenderer mBackgroundRenderer = new BackgroundRenderer();
    private final PlaceMarker mPlaceMarker = new PlaceMarker();

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] mAnchorMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];

    // Tap handling and UI.
    private final ArrayBlockingQueue<MotionEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(16);
    private final ArrayList<Anchor> mAnchors = new ArrayList<>();

    private Frame frame;
    private Camera camera;

    private final float[] PATH_COLOR = new float[]{0.0f, 0.75f, 1.0f, 0.8f}; //a nice blue

    //location reference
    private LatLng mUserLatLng;
    private LatLng mDestination;
    private float mHeading;

    //store route as legObject
    LegObject leg;
    private Anchor lineAnchor;
    private final float[] lineAnchorMatrix = new float[16];

    private Line lineRenderer = new Line();

    private boolean isLineCreated = false; //determines if the line is set, if true -> set anchor in onDraw
    private boolean isLineRendered = false;
    private boolean isLineRotated = true;
    private boolean nearbyPlacesChanged = false;

    //for getting heading using accelerometer and magnetometer
    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private float[] mOrientation = new float[3];
    private float[] mRotationMatrix = new float[9];
    private float magneticDeclination = 0; //initialize at 0

    private float hanoverAltMeters = 528.0f; //Use Hanover's Altitude for now (determining magnetic declination)

    TextView tv;

    private PlacesManager placesManager = PlacesManager.getInstance();
    private PlacesManager.OnPostNearbyPlacesRequest receiveNearbyPlacesResponseListener = new PlacesManager.OnPostNearbyPlacesRequest() {
        @Override
        public void onSuccessfulRequest() { nearbyPlacesChanged = true; }

        @Override
        public void onUnsuccessfulRequest(String errorStatus, String errorMessage) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Nearby Places Error");
            builder.setMessage(errorMessage);
            builder.setPositiveButton("OK", null);
            builder.show();
        }
    };

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

        //register sensors
        sensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

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

        // Set up renderer.
        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        mSurfaceView.setRenderer(this);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        tv = (TextView)fragmentView.findViewById(R.id.distanceText);
        tv.setVisibility(View.INVISIBLE);
        tv.setTextColor(Color.WHITE);

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

        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME);

        // Cause fragment to redraw the nearby places, since drawn objects are not saved on pause.
        nearbyPlacesChanged = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        mDisplayRotationHelper.onPause();
        mSurfaceView.onPause();
        if (mSession != null) {
            mSession.pause();
        }

        //unregister gyroscopic sensor
        sensorManager.unregisterListener(this, rotationVectorSensor);
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

    private void onSingleTap(MotionEvent e) {
        // Queue tap if there is space. Tap is lost if queue is full.
        mQueuedSingleTaps.offer(e);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Create the texture and pass it to ARCore session to be filled during update().
        mBackgroundRenderer.createOnGlThread(getContext());
        if (mSession != null) {
            mSession.setCameraTextureName(mBackgroundRenderer.getTextureId());
        }

        // Prepare the other rendering objects.
        try {
            mPlaceMarker.createOnGlThread(getContext(), "place_marker.obj", "place_marker_texture.png");
            mPlaceMarker.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read obj file");
        }

        //set up line renderer
        lineRenderer.createOnGLThread(PATH_COLOR, getContext());
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mDisplayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mSession == null) {
            if (leg != null){
                isLineCreated = true;
            }
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        mDisplayRotationHelper.updateSessionIfNeeded(mSession);

        try {
            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            frame = mSession.update();
            camera = frame.getCamera();

            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 2000.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            // Respond to tap events.
            MotionEvent tap = mQueuedSingleTaps.poll();
            if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
                float tappedXLoc = tap.getX();
                float tappedYLoc = tap.getY();

                // Projected ray.
                Ray tappedRay = TouchHelper.projectRay(
                    new Vector2f(tappedXLoc, tappedYLoc),
                    mSurfaceView.getMeasuredWidth(),
                    mSurfaceView.getMeasuredHeight(),
                    projmtx, viewmtx
                );

                for(NearbyPlace nearbyPlace : placesManager.nearbyPlaces) {
                    if (nearbyPlace.isTapped(tappedRay)) {
                        Intent placeDetailsIntent = new Intent(getContext(), PlaceDetailsActivity.class);
                        placeDetailsIntent.putExtra(PlaceDetailsActivity.PLACE_ID_KEY, nearbyPlace.placeId);
                        placeDetailsIntent.putExtra(PlaceDetailsActivity.ORIGIN_LAT_KEY, (float) mUserLatLng.latitude);
                        placeDetailsIntent.putExtra(PlaceDetailsActivity.ORIGIN_LNG_KEY, (float) mUserLatLng.longitude);
                        startActivity(placeDetailsIntent);

                        // Raycasting may determine that multiple objects were tapped,
                        // esp. when objects are behind one another. Therefore, we take the first one and
                        // assume the user meant to tap it.
                        break;
                    }
                }
            }

            // Draw background.
            mBackgroundRenderer.draw(frame);

            if(nearbyPlacesChanged || mAnchors.size() < placesManager.nearbyPlaces.size()) {
                mAnchors.clear();
                for (NearbyPlace nearbyPlace : placesManager.nearbyPlaces) {
                    Pose poseForNearbyPlace = nearbyPlace.getPose(mUserLatLng, mHeading);
                    Anchor nearbyPlaceAnchor = mSession.createAnchor(poseForNearbyPlace);
                    mAnchors.add(nearbyPlaceAnchor);
                }

                nearbyPlacesChanged = false;
            }

            if (isLineCreated == true){
                Pose cameraPose = camera.getPose();

                Pose newPose = Pose.makeInterpolated(cameraPose, leg.getLegPose(), 1.0f);

                //add anchor
                lineAnchor = mSession.createAnchor(newPose);

                isLineCreated = false; //set false so only one anchor at a time

                isLineRendered = true;
            }

            // If not tracking, don't draw 3d objects.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                return;
            }

            // Compute lighting from average intensity of the image.
            final float lightIntensity = frame.getLightEstimate().getPixelIntensity();

            // Draw place markers created by touch.
            for (Anchor anchor : mAnchors) {
                if (anchor.getTrackingState() != TrackingState.TRACKING) {
                    continue;
                }

                anchor.getPose().toMatrix(mAnchorMatrix, 0);
                // figure out a good algorithm to calculate the scaling factor dynamically based on the anchor's
                // distance from the camera. Unfortunately, until that time, hardcode the scale factor.
                // Alternatively, could explore ways to implement bounded scaling inside the PlaceMarker renderer.
                mPlaceMarker.updateModelMatrix(mAnchorMatrix, 0.01f);
//                mPlaceMarker.updateModelMatrix(mAnchorMatrix, calculateScaleFactor(anchor.getPose(), camera.getPose()));
                mPlaceMarker.draw(viewmtx, projmtx, lightIntensity);
            }

            //draw line with its anchor
            if (isLineRendered == true) {
                float scaleFactor = 1.0f;
                lineAnchor.getPose().toMatrix(lineAnchorMatrix, 0);

                if (isLineRotated == false){
                    lineRenderer.updateModelMatrix(lineAnchorMatrix, scaleFactor, leg.getRotation());
                    isLineRotated = true;
                }
                else {
                    lineRenderer.updateModelMatrix(lineAnchorMatrix, scaleFactor, 0);
                }
                lineRenderer.drawNoIndices(viewmtx, projmtx, lightIntensity);
            }


        } catch (Throwable t) {
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

    public void setUserLocation(LatLng newLatLng){
        if (mUserLatLng == null && newLatLng != null) {
            mUserLatLng = newLatLng;
            placesManager.getNearbyPlaces(mUserLatLng, receiveNearbyPlacesResponseListener);
            GeomagneticField field = new GeomagneticField((float)mUserLatLng.latitude, (float)mUserLatLng.longitude, hanoverAltMeters, System.currentTimeMillis());
            magneticDeclination = field.getDeclination();
        } else {
            mUserLatLng = newLatLng;
        }
    }

    public void reset() {
        /* Not implemented. Should remove any path drawings. */
        lineAnchor = null;
        isLineRendered = false;
        tv.setVisibility(View.INVISIBLE);
    }

    //called from main activity, parse directions into 3D line
    public void createNewDirections(List<List<HashMap<String, String>>> path){

        if (path != null) {

            if (path.size() > 0) {

                //get last position latlng
                int lastPathIndex = path.size() - 1;
                List<HashMap<String, String>> lastPath = path.get(lastPathIndex);
                int lastPointIndex = lastPath.size() - 1;
                HashMap<String, String> lastPointHashMap = lastPath.get(lastPointIndex);
                mDestination = new LatLng(Double.parseDouble(lastPointHashMap.get("lat")),
                        Double.parseDouble(lastPointHashMap.get("lon")));


                //try to draw stuff
                new ParsePathDirectionsTask().execute(path);
            } else {
                mDestination = mUserLatLng;
            }
        }
    }

    //async task to parse JSON response from request
    private class ParsePathDirectionsTask extends AsyncTask<List<List<HashMap<String, String>>>, Void, String> {
        @Override
        protected String doInBackground(List<List<HashMap<String, String>>> ... paths) {

            ArrayList<LatLng> waypoints = new ArrayList<LatLng>();

            if (paths[0].size() > 0) {

                for (List<HashMap<String, String>> path : paths[0]) {
                    waypoints = new ArrayList<LatLng>();

                    //add user location as first point
                    waypoints.add(mUserLatLng);

                    for (HashMap<String, String> point : path) {

                        //get values from json-created hashmap
                        Double lat = Double.parseDouble(point.get("lat"));
                        Double lng = Double.parseDouble(point.get("lon"));

                        //add to waypoints
                        waypoints.add(new LatLng(lat, lng));

                    }
                }

                LatLng[] waypointsArray = waypoints.toArray(new LatLng[waypoints.size()]);

                leg = new LegObject(waypointsArray);
                //create buffers from current user location
                leg.createTranslationBufferRelativeToLatLng(mUserLatLng, mHeading);
                leg.setLegPose(mUserLatLng, mHeading);

                lineRenderer.setLineVertices(leg.getVertices());
                lineRenderer.setLineIndices(leg.getIndices());

                lineRenderer.printLine();

            }

            return "done";
        }

        @Override
        protected void onPostExecute(String result){
            //tell line to be rendered and set distance text
            isLineCreated = true;
            tv.setVisibility(View.VISIBLE);
            int distance = (int)leg.getDistance();
            String distanceText = "" + distance + " m";
            tv.setText(distanceText);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ROTATION_VECTOR:

                //calculate heading using gyroscope and magnetic declination

                SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);

                float azimuth = (float) Math.toDegrees(SensorManager.getOrientation(mRotationMatrix, mOrientation)[0]);

                azimuth += magneticDeclination;

                mHeading = (((azimuth % 360) + 360) % 360);

            default:
                return;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /* NOOP */ }

    private static float BASE_SCALE_FACTOR = 1.0f;
    private static float MIN_SCALE_FACTOR = 0.001f;
    private float calculateScaleFactor(Pose anchorPose, Pose cameraPose) {
        float xdiff = anchorPose.tx() - cameraPose.tx();
        // float ydiff ... Ignore because all Ys for placemarkers are set to 1.
        float zdiff = anchorPose.tz() - cameraPose.tz();

        // Scale factor decreases as the distance between camera and anchor rises
        // but not at a fixed rate. Using sqrt will create some kind of lower bound.
        float scaleFactor = BASE_SCALE_FACTOR / (float) Math.sqrt(xdiff*xdiff + zdiff*zdiff);

        // Force scale factor to be at least the min. We don't want the markers to be too small.
        scaleFactor = Math.max(scaleFactor, MIN_SCALE_FACTOR);

        // Force scale factor to be at most the base factor. We don't want the markers to be too large.
        scaleFactor = Math.min(scaleFactor, BASE_SCALE_FACTOR);

        return scaleFactor;
    }
}
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

import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];

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

    //store route as legobjects
    private ArrayList<LegObject> legs;
    LegObject leg;
    private ArrayList<Line> lines;
    private Anchor lineAnchor;
    private final float[] lineAnchorMatrix = new float[16];

    private Line lineRenderer = new Line();

    private boolean isLineCreated = false; //determines if the line is set, if true -> set anchor in onDraw
    private boolean isLineRendered = false;
    private boolean nearbyPlacesChanged = false;

    //for getting heading using accelerometer and magnetometer
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] gravityData = new float[3];
    private float[] geomagneticData  = new float[3];
    private boolean hasGravityData = false;
    private boolean hasGeomagneticData = false;
    private double rotationInDegrees;

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
    private PlacesManager.OnPostPlaceDetailsRequest receivePlaceDetailsResponseListener = new PlacesManager.OnPostPlaceDetailsRequest() {
        @Override
        public void onSuccessfulRequest(NearbyPlace nearbyPlace) {
            Intent placeDetailsIntent = new Intent(getContext(), PlaceDetailsActivity.class);
            placeDetailsIntent.putExtra(PlaceDetailsActivity.PLACE_ID_KEY, nearbyPlace.placeId);
            placeDetailsIntent.putExtra(PlaceDetailsActivity.ORIGIN_LAT_KEY, (float) mUserLatLng.latitude);
            placeDetailsIntent.putExtra(PlaceDetailsActivity.ORIGIN_LNG_KEY, (float) mUserLatLng.longitude);
            startActivity(placeDetailsIntent);
        }

        @Override
        public void onUnsuccessfulRequest(String errorStatus, String errorMessage) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Place Details Error");
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

        sensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
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

        // Set up renderer.
        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
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

        lineRenderer.createOnGLThread(PATH_COLOR, getContext());
        //lineRenderer = new Line(PATH_COLOR);

        //hardcode for now
        lineRenderer.setLineVertices(new float[]{
                0.0f, -1.0f, 0,
                -1.0f, -0.8f, -2.0f,
                0.0f, -0.6f, -4.0f,
                1.0f, -0.4f, -6.0f,
                0.0f, 0.0f, -10.0f,
                -4.555f, -19.68561f, -50.0f
        });

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

            //Pose cameraPose = frame.getCamera().getPose();
            //Log.d("CAMERA_POSE", "qx: " + cameraPose.qx() + " qy: " + cameraPose.qy() + " qz: " + cameraPose.qz() + " qw: " + cameraPose.qy());

            //float cameraYRot = (float)(Math.asin(cameraPose.qy()) * 360/Math.PI);
            //Log.d("CAMERA_Y", "yRotation: " + cameraYRot);
            // Handle taps. Handling only one tap per frame, as taps are usually low frequency
            // compared to frame rate.

            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

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
//                Log.d("mztag", "Camera: " + camera.getPose().toString());

                for(NearbyPlace nearbyPlace : placesManager.nearbyPlaces) {
//                    Log.d("mztag", "Place: " + nearbyPlace.name);
//                    Log.d("mztag", "Projected Ray Origin: " + tappedRay.origin.toString());
//                    Log.d("mztag", "Projected Ray Direction: " + tappedRay.direction.toString());
                    if (nearbyPlace.isTapped(tappedRay)) {
//                        Log.d("mztag", "Tapped!");
//                        placesManager.getPlaceDetails(nearbyPlace, receivePlaceDetailsResponseListener);
                        // Raycasting may determine that multiple objects were tapped,
                        // esp. when objects are behind one another. Therefore, we take the first one and
                        // assume the user meant to tap it.
//                        break;
                    }
                }

//                // Randomly select one, because raycasting doesn't work right now.
//                int placeIndex = (int) (Math.floor(placesManager.nearbyPlaces.size() * Math.random()));
//                NearbyPlace nearbyPlace = placesManager.nearbyPlaces.get(placeIndex);
//                placesManager.getPlaceDetails(nearbyPlace, receivePlaceDetailsResponseListener);
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

//                // Use this when you need a test object right in front of you.
//                Pose pose = new Pose(
//                    new float[] {5.0f, 0.0f, 0.0f},
//                    new float[] {0.0f, 0.0f, 0.0f, 1.0f}
//                );
//                Anchor testAnchor = mSession.createAnchor(pose);
//                mAnchors.add(testAnchor);
//
//                Pose pose2 = new Pose(
//                        new float[] {1.0f, 0.0f, 0.0f},
//                        new float[] {0.0f, 0.0f, 0.0f, 1.0f}
//                );
//                Anchor testAnchor2 = mSession.createAnchor(pose2);
//                mAnchors.add(testAnchor2);

                nearbyPlacesChanged = false;
            }

            if (isLineCreated == true){
                Pose cameraPose = camera.getPose();

                Log.d("CAMERA_POSE", "qx: " + cameraPose.qx() + " qy: " + cameraPose.qy() + " qz: " + cameraPose.qz() + " qw: " + cameraPose.qy());

                Pose newPose = Pose.makeInterpolated(cameraPose, leg.getLegPose(), 1.0f);
                Log.d("INT_POSE", "qx: "+ newPose.qx() + " qy: " + newPose.qy() + " qz: " + newPose.qz() + " qw: " + newPose.qw());

                //add anchor
                lineAnchor = mSession.createAnchor(newPose);

                        //frame.getCamera().getPose().compose(Pose.makeRotation(xRot, yRot, zRot, wRot).extractRotation())));

                //mAnchors.add(mSession.createAnchor(leg.getLegPose()));

                isLineCreated = false; //set false so only one anchor at a time

                isLineRendered = true;
            }

            // If not tracking, don't draw 3d objects.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                return;
            }

            // Compute lighting from average intensity of the image.
            final float lightIntensity = frame.getLightEstimate().getPixelIntensity();

            // Visualize anchors created by touch.
            float scaleFactor = 1.0f;
            for (Anchor anchor : mAnchors) {
                if (anchor.getTrackingState() != TrackingState.TRACKING) {
                    continue;
                }
                anchor.getPose().toMatrix(mAnchorMatrix, 0);

                // Ball should be scaled down a lot.
                mPlaceMarker.updateModelMatrix(mAnchorMatrix, 0.01f);
                mPlaceMarker.draw(viewmtx, projmtx, lightIntensity);
            }

            //draw line with its anchor
            if (isLineRendered == true) {
                lineAnchor.getPose().toMatrix(lineAnchorMatrix, 0);
                lineRenderer.updateModelMatrix(lineAnchorMatrix, scaleFactor);
                lineRenderer.drawNoIndices(viewmtx, projmtx, lightIntensity);
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

    public void setUserLocation(LatLng newLatLng){
        if (mUserLatLng == null && newLatLng != null) {
            mUserLatLng = newLatLng;
            placesManager.getNearbyPlaces(mUserLatLng, receiveNearbyPlacesResponseListener);
        } else {
            mUserLatLng = newLatLng;
        }
    }

    public void reset() {
        /* Not implemented. Should remove any path drawings. */
        lineAnchor = null;
        isLineRendered = false;
    }

    public void createNewDirections(List<List<HashMap<String, String>>> path){

        Log.d("jt", "here " + path);

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

            legs = new ArrayList<LegObject>();

            lines = new ArrayList<Line>();

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
            isLineCreated = true;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        //obtained this from stack overflow
        switch (event.sensor.getType()) {
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
                rotationInDegrees = (Math.toDegrees(rotationInRadians) + 360) % 360;

                float currentDegree = (float) rotationInDegrees;

                // do something with the rotation in degrees
                //Log.d("COMPASS", "Heading: " + currentDegree);
                mHeading = currentDegree;
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /* NOOP */ }
}
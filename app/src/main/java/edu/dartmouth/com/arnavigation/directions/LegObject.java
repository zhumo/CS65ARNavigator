package edu.dartmouth.com.arnavigation.directions;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.ar.core.Pose;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

/**
 * Created by jctwake on 2/28/18.
 */

public class LegObject {

    private LatLng[] mWayPointsArray;

    private FloatBuffer translationBuffer;
    private ShortBuffer indexBuffer;

    private float[] vertices;
    private short[] indices;
    private float[]legVertices;
    private Pose legPose;

    private float latLngToScreenScale = 1000; //for now

    public LegObject(LatLng[] leg){
        mWayPointsArray = leg;
    }

    public LatLng[] getPoints(){
        return mWayPointsArray;
    }

    public LatLng getOrigin(){
        return mWayPointsArray[0];
    }

    public LatLng getEnd(){
        if (mWayPointsArray.length > 0) {
            return mWayPointsArray[mWayPointsArray.length - 1];
        }
        else return null;
    }

    public FloatBuffer getTranslationBuffer(){
        return translationBuffer;
    }

    public ShortBuffer getIndexBuffer(){
        return indexBuffer;
    }

    public int getCount(){
        return mWayPointsArray.length;
    }

    public float[] getLegVertices(){
        return legVertices;
    }

    public short[] getIndices(){ return indices;}

    public float[] getVertices(){return vertices;}

    public Pose getLegPose(){
        return legPose;
    }
    //gets lat difference of leg. Negative value means southbound, Positive means northbound
    public double getLatitudeDifference(){
        double latDiff = 0;
        double lastLat = mWayPointsArray[0].latitude;

        for (int i = 1; i < mWayPointsArray.length; i++){
            double thisLat = mWayPointsArray[i].latitude;
            latDiff += thisLat - lastLat;
            lastLat = thisLat;
        }

        return latDiff;
    }

    //gets longitude difference of leg. Negative value means eastbound, Positive means westbound
    public double getLongitudeDifference(){
        double longDiff = 0;
        double lastLon = mWayPointsArray[0].longitude;

        for (int i = 0; i < mWayPointsArray.length; i++){
            double thisLon = mWayPointsArray[i].longitude;
            longDiff += thisLon - lastLon;
            lastLon = thisLon;
        }

        return longDiff;
    }

    //returns the entire leg's vertices as a single float[6]
    //with reference to User Location
    public void setLegVerticesFromUserLatLng(LatLng userLatLng){
        float[] vertices = new float[6];

        double userToStartLatDiff = userLatLng.latitude - mWayPointsArray[0].latitude;
        double userToStartLonDiff = userLatLng.longitude - mWayPointsArray[0].longitude;
        double userToEndLatDiff = userLatLng.latitude - mWayPointsArray[mWayPointsArray.length-1].latitude;
        double userToEndLonDiff = userLatLng.longitude - mWayPointsArray[mWayPointsArray.length-1].longitude;

        vertices[0] = (float)userToStartLonDiff * latLngToScreenScale; //first x
        vertices[1] = 0.0f; //first y
        vertices[2] = (float)userToStartLatDiff * latLngToScreenScale; //first z

        vertices[3] = (float)userToEndLonDiff * latLngToScreenScale; //second x
        vertices[4] = 1.0f; //second y
        vertices[5] = (float)userToEndLatDiff * latLngToScreenScale; //second z


        Log.d("LEG_VERTICES", "First vertex: "+ vertices[0] + ", " + vertices[1] + ", " + vertices[2]
        + "\n Second vertex: " + vertices[3] + ", " + vertices[4] + ", " + vertices[5]);

        legVertices = vertices;
    }

    public void setLegPose(LatLng userPosition, float heading) {

        float[] results = new float[2];
        Location.distanceBetween(userPosition.latitude, userPosition.longitude,
                mWayPointsArray[1].latitude, mWayPointsArray[1].longitude, results);


        float distance = results[0];
        double bearing = (double)results[1];
        float bearing360 = results[1];
        bearing360 = (((bearing360 % 360) + 360) % 360); //way to calculate true modulus

        double offset = (double)bearing360 - heading;
        offset = (((offset % 360) + 360) % 360);

        double offsetRadians = offset * Math.PI / 180;

        float xRot = 0;
        float yRot = (float)Math.sin(offsetRadians/2);
        float zRot = 0;
        float wRot = (float)Math.cos(offsetRadians/2);

        Log.d("Bearing to first", "Bearing: " + bearing + " Bearing360: " + bearing360 + " heading: " + heading + " offset: " + offset + " offsetRadians: " + offsetRadians);

        //Log.d("PATH_POSE", "xRot: " + xRot + " yRot: " + yRot + " zRot: " + zRot + " wRot: " + wRot);

        float quaternionProof = (xRot * xRot) + (yRot * yRot) + (zRot * zRot) + (wRot * wRot);
        Log.d("QUATERNION_PROOF", "This value should equal 1: " + quaternionProof);

        //Log.d("RESULTS_LOCATION", "Distance: " + results[0] + " bearing: " + bearing);

        float[] translation = new float[3]; //three dimensional translation
        translation[0] = 0.0f; //(float)(distance * Math.cos(bearing));
        translation[1] = 0.0f;
        translation[2] = 0.0f; //(float)(distance * Math.sin(bearing));

        float[] rotation = new float[4]; //quaternion rotation
        //calculate using heading ...
        //hardcode for now
        rotation[0] = 0.0f;
        rotation[1] = (float)Math.sin(offsetRadians/2); //y rotation of bearing
        rotation[2] = 0.0f;
        rotation[3] = (float)Math.cos(offsetRadians/2);

        Log.d("LEG_POSE", "Leg pose translation \n x:" + translation[0] + " y:" + translation[1] + " z:" + translation[2]
                + "\n Leg pose rotation \n x:" +rotation[0] + " y:" + rotation[1] + " z:" + rotation[2] + " w:" + rotation[3]);

        legPose = new Pose(translation, rotation);
    }


    //creates a FloatBuffer of translations for OpenGL relative to input location
    public void createTranslationBufferRelativeToLatLng(LatLng refLatLng, float heading) {

        double startLat = refLatLng.latitude;
        double startLon = refLatLng.longitude;

        double startLatMeters = getLatInMeters(startLat);
        double startLonMeters = getLonInMeters(startLon);

        double latDiff;
        double lonDiff;

        //use float array for now, might need to upgrade to double
        vertices = new float[mWayPointsArray.length * 3]; //3 floats for each point

        //use int[] as index buffer, should be size - 1 * 2
        indices = new short[2 * (mWayPointsArray.length - 1)];

        float[] results = new float[2];
        float distance;
        float bearingFloat;
        double bearingRadians;
        double relativeAngle;

        float dx;
        float dy = -0.5f;
        float dz;

        float scaleFactor = 1.0f;

        LatLng thisLatLng;
        LatLng lastLatLng = refLatLng;



        for (int i = 0; i < mWayPointsArray.length; i++){
            int j = i * 3;
            //int h = j-3;

            thisLatLng = mWayPointsArray[i];

//            Location.distanceBetween(lastLatLng.latitude, lastLatLng.longitude, thisLatLng.latitude, thisLatLng.longitude, results);
//            distance = results[0];
//            bearingFloat = results[1];
//            bearingFloat = (((bearingFloat % 360) + 360) % 360);
////            relativeAngle = (double) bearingFloat - heading;
////
//            bearingRadians = (double)(bearingFloat * Math.PI / 180);
//
//            dx = (float)(distance * Math.sin(bearingRadians));
//            dz = (float)(distance * Math.cos(bearingRadians));
//
//            Log.d("LOCATION_CALC", "Distance: " + distance + " bearing: " + bearingFloat + " dx: " + dx + " dz: " + dz);

            double thisLatInMeters = getLatInMeters(thisLatLng.latitude);
            double thisLonInMeters = getLonInMeters(thisLatLng.longitude);

            latDiff = thisLatInMeters - startLatMeters;
            lonDiff = thisLonInMeters - startLonMeters;

            dx = (float)latDiff;
            dz = (float)lonDiff;



            Log.d("LOCATION_CALC", "dx: " + dx + " dz: " + dz);

            vertices[j] = dx * scaleFactor;
            vertices[j+1] = dy * scaleFactor;
            vertices[j+2] = dz * scaleFactor;


//
//            vertices[j] = (float)(distance * Math.cos(relativeAngle));
//            vertices[j+1] = -0.5f;
//            vertices[j+2] = (float)(distance * Math.sin(relativeAngle));


//            latDiff = mWayPointsArray[i].latitude - startLat;
//            lonDiff = mWayPointsArray[i].longitude - startLon;
//
//            vertices[j] = (float)latDiff * latLngToScreenScale;
//            vertices[j+1] = -0.3f;
//            vertices[j+2] = (float)lonDiff * latLngToScreenScale;

            if (i < mWayPointsArray.length - 1){
                int k = i*2;
                indices[k] = (short)i;
                int ii = i+1;
                indices[k+1] = (short)ii;
            }
        }

//        ByteBuffer bbf = ByteBuffer.allocateDirect(vertices.length * 4); //4 bytes per float
//        ByteBuffer bbi = ByteBuffer.allocateDirect(indices.length * 2); //2 bytes per short
//
//        bbf.order(ByteOrder.nativeOrder());
//        bbi.order(ByteOrder.nativeOrder());
//
//        translationBuffer = bbf.asFloatBuffer();
//        indexBuffer = bbi.asShortBuffer();
//
//        translationBuffer.put(vertices);
//        indexBuffer.put(indices);
//
//        translationBuffer.position(0);
//        indexBuffer.position(0);
    }


    private double getLatInMeters(double lat) {
        double latMeters = 111132.954 - 559.822 * Math.cos(2.0 * lat) + 1.175 * Math.cos(4.0 * lat);
        return latMeters;
    }

    private double getLonInMeters(double lon) {
        double lonMeters = (Math.PI/180) * 6367449 * Math.cos(lon);
        return lonMeters;
    }

}

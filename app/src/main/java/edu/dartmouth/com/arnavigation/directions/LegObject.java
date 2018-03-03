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

        setLegPose();
    }

    public void setLegPose() {

        float[] translation = new float[3]; //three dimensional translation
        translation[0] = legVertices[3] - legVertices[0];
        translation[1] = legVertices[4] - legVertices[1];
        translation[2] = legVertices[5] - legVertices[2];

        float[] rotation = new float[4]; //quaternion rotation
        //calculate using heading ...
        //hardcode for now
        rotation[0] = 0;
        rotation[1] = 0;
        rotation[2] = 0;
        rotation[3] = 1;

        legPose = new Pose(translation, rotation);
    }


    //creates a FloatBuffer of translations for OpenGL relative to input location
    public void createTranslationBufferRelativeToLatLng(LatLng refLatLng) {

        double startLat = refLatLng.latitude;
        double startLon = refLatLng.longitude;

        double latDiff;
        double lonDiff;

        //use float array for now, might need to upgrade to double
        vertices = new float[mWayPointsArray.length * 3]; //3 floats for each point

        //use int[] as index buffer, should be size - 1 * 2
        indices = new short[2 * (mWayPointsArray.length - 1)];


        for (int i = 0; i < mWayPointsArray.length; i++){
            int j = i * 3;

            latDiff = mWayPointsArray[i].latitude - startLat;
            lonDiff = mWayPointsArray[i].longitude - startLon;

            vertices[j] = (float)lonDiff * latLngToScreenScale;
            vertices[j+1] = (float)latDiff * latLngToScreenScale;
            vertices[j+2] = 0f;

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

}

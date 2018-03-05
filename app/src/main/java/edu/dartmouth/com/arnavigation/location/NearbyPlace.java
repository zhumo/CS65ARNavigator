package edu.dartmouth.com.arnavigation.location;

import android.graphics.Bitmap;
import android.location.Location;
import android.util.Log;
import android.view.MotionEvent;

import com.google.android.gms.maps.model.LatLng;
import com.google.ar.core.Pose;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class NearbyPlace {
    public double latitude;
    public double longitude;
    public String name;
    public String placeId;
    public String vicinity;

    public String iconUrl;
    public String photoReference;
    public int photoHeight;
    public int photoWidth;
    public Bitmap imageBitmap;

    public NearbyPlace(JSONObject placeData) {
        try {
            JSONObject geometry = placeData.getJSONObject("geometry");
            JSONObject location = geometry.getJSONObject("location");
            latitude = location.getDouble("lat");
            longitude = location.getDouble(("lng"));
            iconUrl = placeData.getString("icon");
            name = placeData.getString("name");
            placeId = placeData.getString("place_id");
            vicinity = placeData.getString("vicinity");
            JSONArray photos = placeData.getJSONArray("photos");
            JSONObject photo = photos.getJSONObject(0);
            photoReference = photo.getString("photo_reference");
            photoHeight = photo.getInt("height");
            photoWidth = photo.getInt("width");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // 0 = distance, 1 = initial bearing, 2 = final bearing.
    // (We only care about 1 and 2 here.)
    private float[] distanceResults = new float[3];
    private float[] translationMatrix = new float[3];
    private final float[] rotationMatrix = new float[] {0f,0f,0f,0f}; // We don't need to rotate the place marker.
    public Pose getPose(LatLng startLatLng, float heading) {
        Location.distanceBetween(startLatLng.latitude, startLatLng.longitude, latitude, longitude, distanceResults);
        float distance = distanceResults[0];
        float bearing = distanceResults[1];
        float angle = bearing - heading;
        float angleInRads = (float) Math.toRadians((double) angle);

        float xPos = (float) Math.cos(angleInRads) * distance;
        if(xPos > 3.0f) {
            xPos = 3.0f;
        } else if(xPos < -3.0f) {
            xPos = -3.0f;
        }
        float zPos = (float) Math.sin(angleInRads) * distance * -1.0f;
        if(zPos > 3.0f) {
            zPos = 3.0f;
        } else if(zPos < -3.0f) {
            zPos = -3.0f;
        }
        translationMatrix[0] = xPos;
        translationMatrix[1] = 0.0f;
        translationMatrix[2] = zPos;

        return new Pose(translationMatrix, rotationMatrix);
    }

    private static float TOLERANCE = 0.5f;
    public boolean isTapped(float[] worldCoordinates) {
        float tappedXLoc = worldCoordinates[0];
        float tappedYLoc = worldCoordinates[1];

        float xLowerBound = translationMatrix[0] - TOLERANCE;
        float xUpperBound = translationMatrix[0] + TOLERANCE;
        float yLowerBound = translationMatrix[1] - TOLERANCE;
        float yUpperBound = translationMatrix[1] + TOLERANCE;

        boolean withinX = tappedXLoc < xUpperBound && tappedXLoc > xLowerBound;
        boolean withinY = tappedYLoc < yUpperBound && tappedYLoc > yLowerBound;

        return withinX && withinY;
    }
}

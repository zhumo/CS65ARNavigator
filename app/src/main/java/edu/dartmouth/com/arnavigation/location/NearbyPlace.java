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

import edu.dartmouth.com.arnavigation.math.Ray;

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

    public Pose pose;

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
        if (pose == null) {
            Location.distanceBetween(startLatLng.latitude, startLatLng.longitude, latitude, longitude, distanceResults);
            float distance = distanceResults[0];
            float bearing = distanceResults[1];
            float angle = bearing - heading;
            float angleInRads = (float) Math.toRadians((double) angle);

            float xPos = (float) Math.cos(angleInRads) * distance;
            if (xPos > 3.0f) {
                xPos = 3.0f;
            } else if (xPos < -3.0f) {
                xPos = -3.0f;
            }
            float zPos = (float) Math.sin(angleInRads) * distance * -1.0f;
            if (zPos > 3.0f) {
                zPos = 3.0f;
            } else if (zPos < -3.0f) {
                zPos = -3.0f;
            }
            translationMatrix[0] = xPos;
            translationMatrix[1] = 0.0f;
            translationMatrix[2] = zPos;

            pose = new Pose(translationMatrix, rotationMatrix);
        }

        return pose;
    }

    private static float TOLERANCE = 0.5f;
    public boolean isTapped(Ray tappedRay) {
        tappedRay.direction.scale(pose.tz());
        tappedRay.origin.add(tappedRay.direction);

        Log.d("mztag", "Pose: " + pose.toString());
        Log.d("mztag", "Projected Ray Origin: (" + tappedRay.origin.x + ", " + tappedRay.origin.y + ", " + tappedRay.origin.z + ")");
        Log.d("mztag", "Projected Ray Direction: (" + tappedRay.direction.x + ", " + tappedRay.direction.y + ", " + tappedRay.direction.z + ")");

        float xLowerBound = pose.tx() - TOLERANCE;
        float xUpperBound = pose.tx() + TOLERANCE;
        float yLowerBound = pose.ty() - TOLERANCE;
        float yUpperBound = pose.ty() + TOLERANCE;

        boolean withinX = tappedRay.origin.x < xUpperBound && tappedRay.origin.x > xLowerBound;
        boolean withinY = tappedRay.origin.y < yUpperBound && tappedRay.origin.y > yLowerBound;

        return withinX && withinY;
    }
}

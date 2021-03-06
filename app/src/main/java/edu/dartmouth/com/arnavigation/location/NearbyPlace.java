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
import edu.dartmouth.com.arnavigation.math.Vector3f;

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

            // Currently, we are coercing any location that is too
            // far away to be at max 3 units within any direction. This
            // is a pretty blunt rule that messes up the placement of the markers.
            // TODO: Once you figure out the best algorithm to dynamically scale the marker, get rid of this constraint.
            float xPos = (float) Math.cos(angleInRads) * distance;
            if(Math.abs(xPos) > 10.0f) {
                xPos = 10.0f * Math.signum(xPos);
            }
            float zPos = (float) Math.sin(angleInRads) * distance * -1.0f;
            if(Math.abs(zPos) > 10.0f) {
                zPos = 10.0f * Math.signum(zPos);
            }
            translationMatrix[0] = xPos;
            translationMatrix[1] = 0.0f;
            translationMatrix[2] = zPos;

            pose = new Pose(translationMatrix, rotationMatrix);
        }

        return pose;
    }

    private static float TOLERANCE = 2.0f;
    public boolean isTapped(Ray tappedRay) {
        // Pythagorean distance
        float rayScale = (float) Math.sqrt(
                pose.tx()*pose.tx() + pose.ty()*pose.ty() + pose.tz()*pose.tz()
        );
        // The operations below will change the object, which we want to preserve. Therefore,
        // create copies of the relevant data
        Vector3f origin = new Vector3f(tappedRay.origin);
        Vector3f direction = new Vector3f(tappedRay.direction);
        Vector3f destination = new Vector3f(origin);
        Vector3f diff = new Vector3f(direction);
        diff.scale(rayScale);
        destination.add(diff);

        float xLowerBound = pose.tx() - TOLERANCE;
        float xUpperBound = pose.tx() + TOLERANCE;
        float yLowerBound = pose.ty() - TOLERANCE;
        float yUpperBound = pose.ty() + TOLERANCE;

        boolean withinX = destination.x < xUpperBound && destination.x > xLowerBound;
        boolean withinY = destination.y < yUpperBound && destination.y > yLowerBound;

        return withinX && withinY;
    }
}

package edu.dartmouth.com.arnavigation.location;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class NearbyPlace {
    public double latitude;
    public double longitude;
    public String iconUrl;
    public String name;
    public String placeId;
    public String vicinity;

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
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

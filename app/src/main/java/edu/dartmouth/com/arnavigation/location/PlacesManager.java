package edu.dartmouth.com.arnavigation.location;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlacesManager {
    private static PlacesManager instance = new PlacesManager();

    public static PlacesManager getInstance() { return instance; }

    public List<NearbyPlace> nearbyPlaces = new ArrayList<>();


    public void getNearbyPlaces(LatLng originLatLng, final OnPostNearbyPlacesRequest onPostRequestListener) {
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

                        onPostRequestListener.onSuccessfulRequest();
                    } else {
                        onPostRequestListener.onUnsuccessfulRequest(
                            responseJSON.getString("status"),
                            responseJSON.getString("error_message")
                        );
                    }
                } catch (JSONException e) {
                    onPostRequestListener.onUnsuccessfulRequest(
                        "JSON_ERROR",
                        "Could not load nearby places"
                    );
                }
            }
        }).execute(originLatLng);
    }

    public NearbyPlace getPlaceByPlaceID(String placeID) {
        for (NearbyPlace place : nearbyPlaces) {
            if (place.placeId.equals(placeID)) { return place; }
        }
        return null;
    }

    public void getPlaceDetails(final NearbyPlace place, final OnPostPlaceDetailsRequest onPostRequestListener) {
        new GetPlacePhotoRequest(new GetPlacePhotoRequest.OnPostExecute() {
            @Override
            public void onPostExecute(Boolean success) {
                onPostRequestListener.onSuccessfulRequest(place);
            }
        }).execute(place);
    }

    public interface OnPostNearbyPlacesRequest {
        void onSuccessfulRequest();

        void onUnsuccessfulRequest(String errorStatus, String errorMessage);
    }

    public interface OnPostPlaceDetailsRequest {
        void onSuccessfulRequest(NearbyPlace place);

        void onUnsuccessfulRequest(String errorStatus, String errorMessage);
    }
}

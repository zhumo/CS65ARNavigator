package edu.dartmouth.com.arnavigation.view_pages;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.dartmouth.com.arnavigation.directions.DirectionsParser;

public class NavigationMapFragment extends SupportMapFragment implements OnMapReadyCallback {

    private GoogleMap mMap;

    private LatLng mUserLatLng;
    private LatLng mDestination;

    private float WIDTH_PIXELS;
    private static final float POLYLINE_WIDTH = 15;
    private PolylineOptions polylineOptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        //get width of screen
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        WIDTH_PIXELS = displayMetrics.widthPixels;

        getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Ignore this error because we've ensured location permission exists when activity launches.
        mMap.setMyLocationEnabled(true);

        zoomToUser();
    }

    private void zoomToUser() {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mUserLatLng, 17.0f));
    }

    private void setMarkers(LatLng endPosition) {

        mMap.addMarker(new MarkerOptions().position(mUserLatLng).icon(
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        );

        mMap.addMarker(new MarkerOptions().position(endPosition).icon(
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        );

        setZoom(endPosition);
    }

    private void setZoom(LatLng position) {
        if (position != null){
            setNewBounds(position);
        }
    }

    private void setNewBounds(LatLng finalPosition) {
        double minLat = Double.min(mUserLatLng.latitude, finalPosition.latitude);
        double maxLat = Double.max(mUserLatLng.latitude, finalPosition.latitude);
        double minLng = Double.min(mUserLatLng.longitude, finalPosition.longitude);
        double maxLng = Double.max(mUserLatLng.longitude, finalPosition.longitude);

        LatLng sw = new LatLng(minLat, minLng);
        LatLng ne = new LatLng(maxLat, maxLng);

        LatLngBounds latLngBounds = new LatLngBounds(sw, ne);
        mMap.setLatLngBoundsForCameraTarget(latLngBounds);

        int zoomFactor = calculateZoomFactor(latLngBounds);

        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLngBounds.getCenter(), zoomFactor);
        mMap.animateCamera(update);
    }

    private int calculateZoomFactor(LatLngBounds bounds) {

        //calculate zoom using mercator line

        int GLOBE_WIDTH = 256;

        double west = bounds.southwest.longitude;
        double east = bounds.northeast.longitude;

        double angle = east - west;
        if (angle < 0) {
            angle += 360;
        }
        double zoomDouble = Math.floor(Math.log(WIDTH_PIXELS * 360 / angle / GLOBE_WIDTH) / Math.log(2));

        int zoom = (int)zoomDouble;
        zoom -= 2; //zoom out to include everything

        return zoom;
    }

    public void setUserLocation(LatLng newLocation) { mUserLatLng = newLocation; }

    public void createNewDirections(List<List<HashMap<String, String>>> path){

        if (path.size() > 0) {

            //get last position latlng
            int lastPathIndex = path.size() - 1;
            List<HashMap<String, String>> lastPath = path.get(lastPathIndex);
            int lastPointIndex = lastPath.size() - 1;
            HashMap<String, String> lastPointHashMap = lastPath.get(lastPointIndex);
            mDestination = new LatLng(Double.parseDouble(lastPointHashMap.get("lat")),
                    Double.parseDouble(lastPointHashMap.get("lon")));


            //run polyline task
            new ParseMapDirectionsTask().execute(path);
        }
        else {
            mDestination = mUserLatLng;
        }
    }


    private void addToPolyine(ArrayList<LatLng> waypoints) {
        //add new waypoints to polyline
        polylineOptions.addAll(waypoints);
        polylineOptions.width(POLYLINE_WIDTH);
        polylineOptions.geodesic(true);
        polylineOptions.color(Color.BLUE);
    }

    public void reset() {
        /* Not yet implemented. Should remove any polylines. */
        mMap.clear();
        zoomToUser();
    }


    //async task to parse JSON response from request
    private class ParseMapDirectionsTask extends AsyncTask<List<List<HashMap<String, String>>>, Void, String> {
        @Override
        protected String doInBackground(List<List<HashMap<String, String>>> ... paths) {

            //draw paths
            ArrayList waypoints = null;

            polylineOptions = new PolylineOptions();

            if (paths[0].size() > 0) {

                for (List<HashMap<String, String>> path : paths[0]) {
                    waypoints = new ArrayList();

                    for (HashMap<String, String> point : path) {

                        //get values from json-created hashmap
                        Double lat = Double.parseDouble(point.get("lat"));
                        Double lng = Double.parseDouble(point.get("lon"));

                        //add to waypoints
                        waypoints.add(new LatLng(lat, lng));
                    }

                    addToPolyine(waypoints);

                }

            }
            return "done";
        }

        @Override
        protected void onPostExecute(String result){

            if (polylineOptions != null) {
                mMap.clear();
                mMap.addPolyline(polylineOptions);
            }

            setMarkers(mDestination);
        }
    }

}


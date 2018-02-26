package edu.dartmouth.com.arnavigation;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import com.google.maps.android.SphericalUtil;
import com.google.maps.android.geometry.Bounds;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager locationManager;


    private static int LOCATION_PERMISSION_REQUEST_CODE = 0;

    private LatLng mUserLocation;
    private Marker mFirstLocationMarker;

    private EditText mLocationSearchText;
    private Spinner travelSpinner;

    private static final String API_KEY = "AIzaSyDCIgMjOYnQmPGmpL5AIzzfW8Uh9HwOPXc";
    private static final String HTTPS_URL = "https://maps.googleapis.com/maps/api/directions/";
    private static final String HTTP_URL = "https://maps.googleapis.com/maps/api/directions/";
    private static final String OUTPUT_TYPE = "json";
    private static final float POLYLINE_WIDTH = 15;
    private static final String[] TRAVEL_ENTRIES = {"Walking", "Driving"};
    private static final String[] TRAVEL_MODES = {"mode_walking", "mode_driving"};

    private float WIDTH_PIXELS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //get width of screen
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        WIDTH_PIXELS = displayMetrics.widthPixels;

        //get destination input
        mLocationSearchText = findViewById(R.id.locationSearchText);

        //get and set travel spinner
        travelSpinner = (Spinner) findViewById(R.id.travelSpinner);
        ArrayAdapter<String> travelAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, TRAVEL_ENTRIES);
        travelAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        travelSpinner.setAdapter(travelAdapter);


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        PermissionManager.ensurePermission(
                Manifest.permission.ACCESS_FINE_LOCATION,
                this,
                LOCATION_PERMISSION_REQUEST_CODE,
                new PermissionManager.OnHasPermission() {
                    @Override
                    public void onHasPermission() { setupMap();}
                }
        );
    }

    private void setupMap() {
        // Ignore this error because onCreate ensures location permission exists.
        mMap.setMyLocationEnabled(true);

        Criteria locationProviderCriteria = new Criteria();
        locationProviderCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        String locationProvider = locationManager.getBestProvider(locationProviderCriteria, true);
        // Ignore this error because onCreate ensures location permission exists.
        Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);

        // Move the camera to last known location
        LatLng lastKnownLatLng = new LatLng(
                lastKnownLocation.getLatitude(),
                lastKnownLocation.getLongitude()
        );

        //set userLocation to first instance
        mUserLocation = lastKnownLatLng;

        //set the map markers
        setMarkers(null);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastKnownLatLng, 17.0f));
    }

    private void setMarkers(LatLng endPosition){

        if (mFirstLocationMarker == null) {
            //add first marker
            mFirstLocationMarker = mMap.addMarker(new MarkerOptions().position(mUserLocation).icon(
                    BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }

        else if (endPosition != null){

            //set marker for both start and end locations

            mFirstLocationMarker = mMap.addMarker(new MarkerOptions().position(mUserLocation).icon(
                    BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            Marker endLocationMarker = mMap.addMarker(new MarkerOptions().position(endPosition).icon(
                    BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }

        setZoom(endPosition);
    }

    private void setZoom(LatLng position) {

        if (position == null){
            return;
        }

        else {
            boolean needAdjustment = false;

            VisibleRegion vr = mMap.getProjection().getVisibleRegion();

            if (!vr.latLngBounds.contains(position)){
                needAdjustment = true;
            }

            if (needAdjustment) {

                setNewBounds(position);
                //CameraUpdate update = CameraUpdateFactory.newLatLngZoom(midLatLng, mMap.get);
                //mMap.animateCamera(update);
            }

        }
    }

    private void setNewBounds(LatLng finalPosition){

        double minLat = Double.min(mUserLocation.latitude, finalPosition.latitude);
        double maxLat = Double.max(mUserLocation.latitude, finalPosition.latitude);
        double minLng = Double.min(mUserLocation.longitude, finalPosition.longitude);
        double maxLng = Double.max(mUserLocation.longitude, finalPosition.longitude);

        LatLng sw = new LatLng(minLat, minLng);
        LatLng ne = new LatLng(maxLat, maxLng);

        LatLngBounds latLngBounds = new LatLngBounds(sw, ne);
        mMap.setLatLngBoundsForCameraTarget(latLngBounds);

        int zoomFactor = calculateZoomFactor(latLngBounds);

        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLngBounds.getCenter(), zoomFactor);
        mMap.animateCamera(update);
    }

    private int calculateZoomFactor  (LatLngBounds bounds) {

        //calculate zoom using mercator line

        int GLOBE_WIDTH = 256;

        double west = bounds.southwest.longitude;
        double east = bounds.northeast.longitude;

        double angle = east - west;
        if (angle < 0){
            angle += 360;
        }
        double zoomDouble = Math.floor(Math.log(WIDTH_PIXELS * 360 / angle / GLOBE_WIDTH) / Math.log(2));

        int zoom = (int)zoomDouble;

        zoom -= 2; //zoom out to include everything

        return zoom;
    }

    public void activateCamera(View view) {
        Intent cameraActivityIntent = new Intent(this, CameraActivity.class);
        startActivity(cameraActivityIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if(resultCode == Activity.RESULT_OK) { setupMap(); }
            else { finish(); }
        }
    }

    public void locationSearchPressed(View v) {

        //check if empty string
        if (mLocationSearchText.getText().toString() == null) {
            Toast.makeText(getApplicationContext(), "Please enter a destination", Toast.LENGTH_SHORT).show();
            return;
        }

        else {
            //close search text if still open
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mLocationSearchText.getWindowToken(), 0);

            //if address, get geocode
            //assume address for now
            String url = getRequestURLAddress(mLocationSearchText.getText().toString());


            //start async task
            new RequestDirectionsTask().execute(url);
        }
    }

    private String getRequestURLAddress(String address){

        //create URL request string using address

        String origin = "origin=" + mUserLocation.latitude + "," + mUserLocation.longitude;
        String destination = "destination=" + address;
        String param = origin +"&" + destination +"&" + TRAVEL_MODES[travelSpinner.getSelectedItemPosition()];
        String urlRequest = HTTPS_URL + OUTPUT_TYPE + "?" + param + API_KEY;

        return urlRequest;
    }

    private String getRequestURLLatLng(LatLng destination){

        //create URL request string using specific latlng

        //not implemented yet

        return null;
    }

    private String requestDirectionsWithURL(String reqURL){

         //new RequestDirectionsTask().execute();

        String responseString = "";
        InputStream inputStream = null;
        HttpsURLConnection httpsURLConnection = null;

        try {
            //get url connection
            URL url = new URL(reqURL);
            httpsURLConnection = (HttpsURLConnection) url.openConnection();
            httpsURLConnection.connect();

            //set up file reading streams
            inputStream = httpsURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuffer stringBuffer = new StringBuffer();
            String line = "";

            //append to buffer
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }

            //get response string
            responseString = stringBuffer.toString();

            //close streams
            inputStream.close();
            inputStreamReader.close();
            bufferedReader.close();

        }catch (IOException e){
            e.printStackTrace();
        } finally {
            //disconnect from url
            httpsURLConnection.disconnect();
        }

        return responseString;
    }

    // ********** ASYNC TASKS *********** //

    //async task to get direction
    private class RequestDirectionsTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {

            String responseString = "";

            try {
                responseString = requestDirectionsWithURL(strings[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.d("RESPONSE", responseString);

            return responseString;
        }

        @Override
        protected void onPostExecute(String result){
            //parse JSON with new async task
            new ParseDirectionsTask().execute(result);
        }
    }

    //async task to parse JSON response from request
    private class ParseDirectionsTask extends AsyncTask<String, Void, List<List<HashMap<String, String>>>> {
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {

            JSONObject jsonObject = null;

            List<List<HashMap<String, String>>> routes = null;

            //use DirectionsParser class to parse JSONObject
            try {
                jsonObject = new JSONObject(strings[0]);
                DirectionsParser directionsParser = new DirectionsParser();
                routes = directionsParser.parse(jsonObject);
            } catch (JSONException je){
                je.printStackTrace();
            }

            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> paths){

            //draw paths
            ArrayList waypoints = null;

            PolylineOptions polylineOptions = null;

            for (List<HashMap<String, String>> path: paths){
                waypoints = new ArrayList();
                polylineOptions = new PolylineOptions();

                for (HashMap<String, String> point: path) {

                    //get values from json-created hashmap
                    Double lat = Double.parseDouble(point.get("lat"));
                    Double lng = Double.parseDouble(point.get("lon"));

                    //add to waypoints
                    waypoints.add(new LatLng(lat,lng));
                }

                //create polyline
                polylineOptions.addAll(waypoints);
                polylineOptions.width(POLYLINE_WIDTH);
                polylineOptions.geodesic(true);
                polylineOptions.color(Color.BLUE);
            }

            //get last position latlng
            int lastPathIndex = paths.size() - 1;
            List<HashMap<String, String>> lastPath = paths.get(lastPathIndex);
            int lastPointIndex = lastPath.size() - 1;
            HashMap<String, String> lastPointHashMap = lastPath.get(lastPointIndex);
            LatLng lastLatLng = new LatLng(Double.parseDouble(lastPointHashMap.get("lat")),
                    Double.parseDouble(lastPointHashMap.get("lon")));

            //handle polyline
            if (polylineOptions != null){
                mMap.clear();
                setMarkers(lastLatLng);
                mMap.addPolyline(polylineOptions);
            } else {
                Toast.makeText(getApplicationContext(), "Could not find directions.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

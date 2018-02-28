package edu.dartmouth.com.arnavigation;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.Button;
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

    private String locationProvider;
    private DirectionsManager directionsManager;

    private BroadcastReceiver updateReceiver;

    private static int LOCATION_PERMISSION_REQUEST_CODE = 0;

    private LatLng mUserLocation;

    private EditText mLocationSearchText;
    private Spinner travelSpinner;


    private Criteria locationProviderCriteria;
    private String bestLocationProvider;

    private static final String[] TRAVEL_ENTRIES = {"Walking", "Driving"};


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
        mLocationSearchText = (EditText) findViewById(R.id.locationSearchText);

        //get and set travel spinner
        travelSpinner = (Spinner) findViewById(R.id.travelSpinner);
        ArrayAdapter<String> travelAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, TRAVEL_ENTRIES);
        travelAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        travelSpinner.setAdapter(travelAdapter);

        directionsManager = new DirectionsManager(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        setUpdateReceiver();

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationProviderCriteria = new Criteria();
        locationProviderCriteria.setAccuracy(Criteria.ACCURACY_FINE);
    }


    @Override
    protected void onResume() {
        super.onResume();
        bestLocationProvider = locationManager.getBestProvider(locationProviderCriteria, true);
    }

    @Override
    protected void onDestroy(){
        //unregister receiver
        unregisterReceiver(updateReceiver);

        super.onDestroy();
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
                    public void onHasPermission() {
                        setupMap();
                    }
                }
        );
    }


    private void setupMap() {
        // Ignore this error because onCreate ensures location permission exists.
        mMap.setMyLocationEnabled(true);


        Criteria locationProviderCriteria = new Criteria();
        locationProviderCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        locationProvider = locationManager.getBestProvider(locationProviderCriteria, true);
        // Ignore this error because onCreate ensures location permission exists.


        //set userLocation to first instance
        mUserLocation = getUserLocation();

        //set the map markers
        setMarkers(null);

    }

    private LatLng getUserLocation(){
        // Ignore this error because onCreate ensures location permission exists.
        Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);


        // Move the camera to last known location
        LatLng lastKnownLatLng = new LatLng(
                lastKnownLocation.getLatitude(),
                lastKnownLocation.getLongitude()
        );


        //set userLocation to first instance
        mUserLocation = getUserLocation();

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mUserLocation, 17.0f));

        return lastKnownLatLng;
    }

    private void setUpdateReceiver(){

        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction("directions.update");
        //intentFilter.addAction("location.update");

        updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.getAction() == "directions.update"){
                    //update directions
                    PolylineOptions polylineOptions = directionsManager.getPolylineOptions();
                    LatLng lastLatLng = directionsManager.getLastLocation();

                    //handle polyline
                    if (polylineOptions != null){
                        mMap.clear();
                        setMarkers(lastLatLng);
                        mMap.addPolyline(polylineOptions);
                    } else {
                        Toast.makeText(getApplicationContext(), "Could not find directions.", Toast.LENGTH_SHORT).show();
                    }
                }

//                if (intent.getAction() == "location.update"){
//                    //update user location
//                }
            }
        };

        registerReceiver(updateReceiver, intentFilter);
    }


    private void setMarkers(LatLng endPosition) {
        mMap.addMarker(new MarkerOptions().position(endPosition).icon(
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        );

        setZoom(endPosition);
    }

    private void setZoom(LatLng position) {
        VisibleRegion vr = mMap.getProjection().getVisibleRegion();
        if (!vr.latLngBounds.contains(position)) {
            setNewBounds(position);
        }
    }

    private void setNewBounds(LatLng finalPosition) {
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

    public void activateCamera(View view) {
        Intent cameraActivityIntent = new Intent(this, CameraActivity.class);
        startActivity(cameraActivityIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                setupMap();
            } else {
                finish();
            }
        }
    }

    public void locationSearchPressed(View v) {
        //check if empty string
        if (mLocationSearchText.getText().toString() == null) {
            Toast.makeText(getApplicationContext(), "Please enter a destination", Toast.LENGTH_SHORT).show();
            return;
        }

        //close search text if still open
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mLocationSearchText.getWindowToken(), 0);

        //disable search button
        Button searchButton = findViewById(R.id.searchButton);
        searchButton.setEnabled(false);


        //if address, get geocode
        //assume address for now
        String address = mLocationSearchText.getText().toString();

        //update userLocation value
        mUserLocation = getUserLocation();

        //pass to DirectionsManager address function
        directionsManager.getDirectionsWithAddress(mUserLocation, address, travelSpinner.getSelectedItemPosition());

    }

    public void resetMapButtonClicked(View v) {
        mMap.clear();
        EditText destinationInput = (EditText) findViewById(R.id.locationSearchText);
        destinationInput.setText("");
        travelSpinner.setSelection(0);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mLocationSearchText.getWindowToken(), 0);

        mUserLocation = getUserLocation();
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mUserLocation, 17.0f));
    }

}


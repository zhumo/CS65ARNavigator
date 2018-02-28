package edu.dartmouth.com.arnavigation;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
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

public class NavigationMapFragment extends SupportMapFragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager locationManager;

    private String locationProvider;
    private DirectionsManager directionsManager;

    private BroadcastReceiver updateReceiver;

    private static int LOCATION_PERMISSION_REQUEST_CODE = 0;

    private LatLng mUserLocation;

    private Criteria locationProviderCriteria;
    private String bestLocationProvider;

    private float WIDTH_PIXELS;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        //get width of screen
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        WIDTH_PIXELS = displayMetrics.widthPixels;

        setUpdateReceiver();

        locationProviderCriteria = new Criteria();
        locationProviderCriteria.setAccuracy(Criteria.ACCURACY_FINE);

        directionsManager = new DirectionsManager(getContext());
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        getMapAsync(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        bestLocationProvider = locationManager.getBestProvider(locationProviderCriteria, true);
    }

    @Override
    public void onDestroy(){
        //unregister receiver
        getActivity().unregisterReceiver(updateReceiver);

        super.onDestroy();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        PermissionManager.ensurePermission(
                Manifest.permission.ACCESS_FINE_LOCATION,
                getActivity(),
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
                        Toast.makeText(getContext(), "Could not find directions.", Toast.LENGTH_SHORT).show();
                    }
                }

//                if (intent.getAction() == "location.update"){
//                    //update user location
//                }
            }
        };

        getActivity().registerReceiver(updateReceiver, intentFilter);
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
}


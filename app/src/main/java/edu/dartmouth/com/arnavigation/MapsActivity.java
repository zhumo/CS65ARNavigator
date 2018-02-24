package edu.dartmouth.com.arnavigation;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        if(hasLocationPermission()) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        } else {
            launchPermissionsActivity();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!hasLocationPermission()) { launchPermissionsActivity(); }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

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
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastKnownLatLng, 17.0f));
    }

    private void launchPermissionsActivity() {
        Intent permissionsActivityIntent = new Intent(this, PermissionsActivity.class);
        permissionsActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(permissionsActivityIntent);
    }

    public void cameraButtonClicked(View view) {
        Intent cameraActivityIntent = new Intent(this, CameraActivity.class);
        startActivity(cameraActivityIntent);
    }

    private boolean hasLocationPermission() {
        if(Build.VERSION.SDK_INT < 23) { return true; }

        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}

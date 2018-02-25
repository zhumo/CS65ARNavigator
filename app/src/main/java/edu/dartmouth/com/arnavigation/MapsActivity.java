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
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager locationManager;

    private RaiseGestureReceiver raiseGestureReceiver = new RaiseGestureReceiver();

    private static int LOCATION_PERMISSION_REQUEST_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent raiseGestureServiceIntent = new Intent(this, RaiseGestureService.class);
        startService(raiseGestureServiceIntent);

        registerReceiver(raiseGestureReceiver, new IntentFilter(RaiseGestureService.PHONE_RAISED_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        Intent raiseGestureServiceIntent = new Intent(this, RaiseGestureService.class);
        stopService(raiseGestureServiceIntent);

        unregisterReceiver(raiseGestureReceiver);
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
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastKnownLatLng, 17.0f));
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

    public class RaiseGestureReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent cameraActivityIntent = new Intent(MapsActivity.this, CameraActivity.class);
            startActivity(cameraActivityIntent);
        }
    }
}

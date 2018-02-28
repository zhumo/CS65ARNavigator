package edu.dartmouth.com.arnavigation;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import edu.dartmouth.com.arnavigation.directions.DirectionsManager;
import edu.dartmouth.com.arnavigation.location.LocationService;
import edu.dartmouth.com.arnavigation.permissions.PermissionManager;
import edu.dartmouth.com.arnavigation.view_pages.CameraFragment;
import edu.dartmouth.com.arnavigation.view_pages.NavigationMapFragment;
import edu.dartmouth.com.arnavigation.view_pages.NonSwipingViewPager;
import edu.dartmouth.com.arnavigation.view_pages.ViewPagerAdapter;

public class NavigationActivity extends AppCompatActivity {
    private static final String[] TRAVEL_ENTRIES = {"Walking", "Driving"};

    private EditText mLocationSearchText;
    private Spinner travelSpinner;

    private DirectionsManager directionsManager;

    public LatLng currentLatLng;

    NonSwipingViewPager viewPager;

    CameraFragment cameraFragment = new CameraFragment();
    NavigationMapFragment navigationMapFragment = new NavigationMapFragment();

    BroadcastReceiver newDirectionsReceiver;

    BroadcastReceiver updateLocationReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        // get destination input
        mLocationSearchText = findViewById(R.id.locationSearchText);

        // get and set travel spinner
        travelSpinner = findViewById(R.id.travelSpinner);
        ArrayAdapter<String> travelAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, TRAVEL_ENTRIES);
        travelAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        travelSpinner.setAdapter(travelAdapter);

        updateLocationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            LatLng newLatLng = new LatLng(
                intent.getExtras().getDouble(LocationService.LATITUDE_KEY),
                intent.getExtras().getDouble(LocationService.LONGITUDE_KEY)
            );
            currentLatLng = newLatLng;
            }
        };

        newDirectionsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String rawDirectionsJSON = intent.getStringExtra(DirectionsManager.DIRECTIONS_JSON_KEY);
                Log.d("mztag", rawDirectionsJSON);

                navigationMapFragment.createNewDirections(directionsManager.getPaths());
                cameraFragment.createNewDirections(directionsManager.getPaths());

//                try {
//                    JSONObject directionsJSON = new JSONObject(rawDirectionsJSON);
//                    if (directionsJSON.get("status").equals("OK")){
//                        navigationMapFragment.new
//                    } else {
//                        Toast.makeText(NavigationActivity.this, "Could not find directions.", Toast.LENGTH_SHORT).show();
//                    }
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                    Toast.makeText(NavigationActivity.this, "Could not find directions.", Toast.LENGTH_SHORT).show();
//                }
            }
        };

        PermissionManager.ensurePermissions(
            this,
            0,
            new PermissionManager.OnHasPermission() {
                @Override
                public void onHasPermission() { initialize(); }
            },
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        );
    }

    private void initialize() {
        // Set up directions manager with listener.
        directionsManager = new DirectionsManager(NavigationActivity.this);
        IntentFilter newDirectionsIntentFilter = new IntentFilter();
        newDirectionsIntentFilter.addAction(DirectionsManager.NEW_DIRECTIONS_ACTION);
        registerReceiver(newDirectionsReceiver, newDirectionsIntentFilter);

        // Start location service
        Intent startLocationServiceIntent = new Intent(NavigationActivity.this, LocationService.class);
        startService(startLocationServiceIntent);

        // Get last known location and initialize map fragment with that info
        Criteria locationProviderCriteria = new Criteria();
        locationProviderCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        String bestLocationProvider = locationManager.getBestProvider(locationProviderCriteria, true);

        Location lastKnownLocation = locationManager.getLastKnownLocation(bestLocationProvider);

        currentLatLng = new LatLng(
                lastKnownLocation.getLatitude(),
                lastKnownLocation.getLongitude()
        );
        navigationMapFragment.setUserLocation(currentLatLng);

        // Set up view pager
        viewPager = findViewById(R.id.navigation_view_pager);

        ArrayList<Fragment> fragments = new ArrayList<>();
        fragments.add(cameraFragment);
        fragments.add(navigationMapFragment);

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), fragments);
        viewPager.setAdapter(viewPagerAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter updateLocationIntentFilter = new IntentFilter();
        updateLocationIntentFilter.addAction(LocationService.UPDATE_LOCATION_ACTION);
        registerReceiver(updateLocationReceiver, updateLocationIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(updateLocationReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // don't kill the service for screen rotations
        if(isChangingConfigurations()) {
            Intent intent = new Intent(this, LocationService.class);
            stopService(intent);
        }

        unregisterReceiver(newDirectionsReceiver);
    }

    public void locationSearchPressed(View v) {
        //check if empty string
        if (mLocationSearchText.getText().toString() == null) {
            Toast.makeText(this, "Please enter a destination", Toast.LENGTH_SHORT).show();
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

        //pass to DirectionsManager address function
        directionsManager.getDirectionsWithAddress(currentLatLng, address, travelSpinner.getSelectedItemPosition());
    }

    public void resetButtonClicked(View v) {
        navigationMapFragment.reset();
        cameraFragment.reset();

        EditText destinationInput = findViewById(R.id.locationSearchText);
        destinationInput.setText("");
        travelSpinner.setSelection(0);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mLocationSearchText.getWindowToken(), 0);

        Button searchButton = findViewById(R.id.searchButton);
        searchButton.setEnabled(true);
    }

    public void switchViewsButtonClicked(View view) {
        int nextViewPageIndex = (viewPager.getCurrentItem() + 1) % 2;
        viewPager.setCurrentItem(nextViewPageIndex,true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK) { initialize(); }
        else { finish(); }
    }
}

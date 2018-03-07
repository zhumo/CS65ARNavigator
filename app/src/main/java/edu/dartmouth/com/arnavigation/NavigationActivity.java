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
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
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

    //private EditText mLocationSearchText;
    private Spinner travelSpinner;


    private DirectionsManager directionsManager;

    public LatLng currentLatLng;

    NonSwipingViewPager viewPager;

    CameraFragment cameraFragment = new CameraFragment();
    NavigationMapFragment navigationMapFragment = new NavigationMapFragment();

    BroadcastReceiver newDirectionsReceiver;
    BroadcastReceiver updateLocationReceiver;

    private String placeAddress;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        // get destination input
        //mLocationSearchText = findViewById(R.id.locationSearchText);

        updateLocationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            LatLng newLatLng = new LatLng(
                intent.getExtras().getDouble(LocationService.LATITUDE_KEY),
                intent.getExtras().getDouble(LocationService.LONGITUDE_KEY)
            );
            currentLatLng = newLatLng;
            navigationMapFragment.setUserLocation(newLatLng);
            cameraFragment.setUserLocation(newLatLng);
            }
        };

        newDirectionsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean directionsResultSuccess = intent.getExtras().getBoolean(DirectionsManager.DIRECTIONS_RESULTS_SUCCESS_KEY);
                if (directionsResultSuccess) {
                    navigationMapFragment.createNewDirections(directionsManager.getPaths());
                    cameraFragment.createNewDirections(directionsManager.getPaths());
                } else {
                    Toast.makeText(NavigationActivity.this, "Could not find directions.", Toast.LENGTH_SHORT).show();
                }

                // Re-enable search button now that results have come back.
                Button searchButton = findViewById(R.id.searchButton);
                searchButton.setEnabled(true);
            }
        };

        IntentFilter newDirectionsIntentFilter = new IntentFilter();
        newDirectionsIntentFilter.addAction(DirectionsManager.DIRECTIONS_RESULTS_ACTION);
        registerReceiver(newDirectionsReceiver, newDirectionsIntentFilter);

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

        // Get last known location and initialize map fragment with that info
        Criteria locationProviderCriteria = new Criteria();
        locationProviderCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        String bestLocationProvider = locationManager.getBestProvider(locationProviderCriteria, true);

        Location lastKnownLocation = locationManager.getLastKnownLocation(bestLocationProvider);

        if (lastKnownLocation != null) {
            currentLatLng = new LatLng(
                lastKnownLocation.getLatitude(),
                lastKnownLocation.getLongitude()
            );
            navigationMapFragment.setUserLocation(currentLatLng);
            cameraFragment.setUserLocation(currentLatLng);
        }
      
        // Set up view pager
        viewPager = findViewById(R.id.navigation_view_pager);

        ArrayList<Fragment> fragments = new ArrayList<>();
        fragments.add(cameraFragment);
        fragments.add(navigationMapFragment);

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), fragments);
        viewPager.setAdapter(viewPagerAdapter);


        //set up places autocomplete
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Log.d("PLACE", "Place: " + place.getName());
                placeAddress = place.getAddress().toString();
                //pass to DirectionsManager address function
                directionsManager.getDirectionsWithAddress(currentLatLng, placeAddress);
            }

            @Override
            public void onError(Status status) {
                Log.d("PLACE_ERROR", "Cannot find place with status: " + status.getStatusMessage());
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start location service
        Intent startLocationServiceIntent = new Intent(this, LocationService.class);
        startService(startLocationServiceIntent);

        IntentFilter updateLocationIntentFilter = new IntentFilter();
        updateLocationIntentFilter.addAction(LocationService.UPDATE_LOCATION_ACTION);
        registerReceiver(updateLocationReceiver, updateLocationIntentFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();

        stopService(new Intent(this, LocationService.class));
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
        if (placeAddress == null) {
            Toast.makeText(this, "Please enter a destination", Toast.LENGTH_SHORT).show();
            return;
        }

        //close search text if still open
        //InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.hideSoftInputFromWindow(mLocationSearchText.getWindowToken(), 0);

        //disable search button
        Button searchButton = findViewById(R.id.searchButton);
        searchButton.setEnabled(false);

        //if address, get geocode
        //assume address for now
        //String address = mLocationSearchText.getText().toString();

        //pass to DirectionsManager address function
        directionsManager.getDirectionsWithAddress(currentLatLng, placeAddress);
    }

    public void resetButtonClicked(View v) {
        navigationMapFragment.reset();
        cameraFragment.reset();

        //InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.hideSoftInputFromWindow(mLocationSearchText.getWindowToken(), 0);

        Button searchButton = findViewById(R.id.searchButton);
        searchButton.setEnabled(true);
    }

    public void switchViewsButtonClicked(View view) {
        FloatingActionButton clickedButton = (FloatingActionButton) view;
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) clickedButton.getLayoutParams();

        if(viewPager.getCurrentItem() == 0) {
            // Camera Fragment -> Map Fragment
            viewPager.setCurrentItem(1,true);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END, 0);
            clickedButton.setImageResource(android.R.drawable.ic_menu_camera);
        } else {
            // Map Fragment -> Camera Fragment
            viewPager.setCurrentItem(0,true);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
            clickedButton.setImageResource(android.R.drawable.ic_menu_mapmode);
        }

        clickedButton.setLayoutParams(layoutParams);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK) { initialize(); }
        else { finish(); }
    }


}

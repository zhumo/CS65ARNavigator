package edu.dartmouth.com.arnavigation;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;

import java.util.ArrayList;

import edu.dartmouth.com.arnavigation.views.NonSwipingViewPager;
import edu.dartmouth.com.arnavigation.views.ViewPagerAdapter;

public class NavigationActivity extends AppCompatActivity {
    private static final String[] TRAVEL_ENTRIES = {"Walking", "Driving"};

    private EditText mLocationSearchText;
    private Spinner travelSpinner;

    NonSwipingViewPager viewPager;

    CameraFragment cameraFragment = new CameraFragment();
    NavigationMapFragment navigationMapFragment = new NavigationMapFragment();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        PermissionManager.ensurePermissions(
            this,
            0,
            new PermissionManager.OnHasPermission() {
                @Override
                public void onHasPermission() {
                    setupViewPager();
                }
            },
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        );
    }

    private void setupViewPager() {
        viewPager = findViewById(R.id.navigation_view_pager);

        final ArrayList<Fragment> fragments = new ArrayList<>();
        fragments.add(cameraFragment);
        fragments.add(navigationMapFragment);

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), fragments);
        viewPager.setAdapter(viewPagerAdapter);

        //get destination input
        mLocationSearchText = findViewById(R.id.locationSearchText);

        //get and set travel spinner
        travelSpinner = findViewById(R.id.travelSpinner);
        ArrayAdapter<String> travelAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, TRAVEL_ENTRIES);
        travelAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        travelSpinner.setAdapter(travelAdapter);
    }

    public void locationSearchPressed(View v) {
//        //check if empty string
//        if (mLocationSearchText.getText().toString() == null) {
//            Toast.makeText(this, "Please enter a destination", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        //close search text if still open
//        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.hideSoftInputFromWindow(mLocationSearchText.getWindowToken(), 0);
//
//        //disable search button
//        Button searchButton = findViewById(R.id.searchButton);
//        searchButton.setEnabled(false);
//
//
//        //if address, get geocode
//        //assume address for now
//        String address = mLocationSearchText.getText().toString();
//
//        //update userLocation value
//        mUserLocation = getUserLocation();
//
//        //pass to DirectionsManager address function
//        directionsManager.getDirectionsWithAddress(mUserLocation, address, travelSpinner.getSelectedItemPosition());
    }

    public void resetMapButtonClicked(View v) {
//        navigationMapFragment.clear();
//        cameraFragment.clear();
//
//        EditText destinationInput = findViewById(R.id.locationSearchText);
//        destinationInput.setText("");
//        travelSpinner.setSelection(0);
//
//        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.hideSoftInputFromWindow(mLocationSearchText.getWindowToken(), 0);
//
//        mUserLocation = getUserLocation();
//        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mUserLocation, 17.0f));
    }

    public void switchViewsButtonClicked(View view) {
        if(viewPager.getCurrentItem() == 0) {
            viewPager.setCurrentItem(1,true);
        } else {
            viewPager.setCurrentItem(0,true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK) { setupViewPager(); }
        else { finish(); }
    }
}

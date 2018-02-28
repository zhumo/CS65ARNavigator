package edu.dartmouth.com.arnavigation.view_pages;

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

public class NavigationMapFragment extends SupportMapFragment implements OnMapReadyCallback {

    private GoogleMap mMap;

    private LatLng mUserLatLng;

    private float WIDTH_PIXELS;

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

    public void reset() {
        /* Not yet implemented. Should remove any polylines. */
        mMap.clear();
        zoomToUser();
    }
}


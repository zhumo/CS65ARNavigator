package edu.dartmouth.com.arnavigation.location;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import edu.dartmouth.com.arnavigation.permissions.PermissionManager;


public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    final public static String UPDATE_LOCATION_ACTION = "UpdateLocationAction";
    final public static String LATITUDE_KEY = "LATITUDE";
    final public static String LONGITUDE_KEY = "LONGITUDE";

    final static String STOP_SERVICE_BROADCAST_KEY="StopServiceBroadcastKey";
    final static int REQUEST_STOP_SERVICE = 1;

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    private UpdateBinder updateBinder;

    private NotificationManager notificationManager;
    final static int NOTIFICATION_ID = 101;

    private boolean isRunning;
    private long updateInterval = 1000; //1 sec

    private LatLng lastLatLng;

    public LocationService() {
    }

    @Override
    public void onCreate(){
        super.onCreate();

        isRunning = false;

        //connect to client
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onDestroy(){
        //disconnect client
        googleApiClient.disconnect();

        super.onDestroy();
    }


    private void startService() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        //showNotification();
        googleApiClient.connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        startService();
        return updateBinder;
    }

    //public binder class
    public class UpdateBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("API_CONNECT", "Connected to google api");
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(updateInterval);

        // Service doesn't get started unless permission is granted.
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("API_SUSPEND", "Connection has been suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("API_FAIL", "Connection failed");
    }


    @Override
    public void onLocationChanged(Location location) {
        Log.d("mztag", "onLocationChanged");
        processLocation(location);
    }


    private void processLocation(Location location){
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        if (lat != lastLatLng.latitude || lon != lastLatLng.longitude){
            //create new LatLng
            LatLng newLatLng = new LatLng(lat, lon);
            lastLatLng = newLatLng;

            //create update intent
            //send data to activity
            Intent local = new Intent();
            local.putExtra(LATITUDE_KEY, lat);
            local.putExtra(LONGITUDE_KEY, lon);
            local.setAction(UPDATE_LOCATION_ACTION);
            this.sendBroadcast(local);
        }
    }
}

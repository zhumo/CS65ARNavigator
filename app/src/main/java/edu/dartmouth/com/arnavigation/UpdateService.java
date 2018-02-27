package edu.dartmouth.com.arnavigation;

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
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;


public class UpdateService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    final static String ACTION = "UpdateServiceAction";
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

    public UpdateService() {
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
        public UpdateService getService() {
            return UpdateService.this;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Log.d("API_CONNECT", "Connected to google api");
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(updateInterval);

        //make sure permission is granted
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
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
            local.putExtra("EXTRA_LATITUDE", lat);
            local.putExtra("EXTRA_LONGITUDE", lon);
            local.setAction("location.update");
            this.sendBroadcast(local);
        }
    }
}

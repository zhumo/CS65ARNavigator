package edu.dartmouth.com.arnavigation;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

/**
 * Created by mozhu on 2/11/18.
 */

public class PermissionsActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ensureLocationPermission();
    }

    private void launchNextActivity() {
        Intent nextActivityIntent = new Intent(this, MapsActivity.class);
        nextActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(nextActivityIntent);
    }

    private void ensureLocationPermission() {
        if(Build.VERSION.SDK_INT < 23) { return; }

        if(hasLocationPermission()) {
            launchNextActivity();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchNextActivity();
        } else if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Important Permission Required");
            builder.setMessage("Please allow location tracking for this feature.");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) { ensureLocationPermission(); }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) { finish(); }
            });
            builder.show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Important Permission Required");
            builder.setMessage("Please enable location tracking in Settings in order to use this app.");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) { finish(); }
            });
            builder.show();
        }
    }

    private boolean hasLocationPermission() {
        if(Build.VERSION.SDK_INT < 23) { return true; }

        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}

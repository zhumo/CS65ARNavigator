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
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by mozhu on 2/11/18.
 */

public class PermissionsActivity extends Activity {

    public static String PERMISSION_KEY = "permission";

    private ArrayList<String> requestedPermissions;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestedPermissions = getIntent().getExtras().getStringArrayList(PERMISSION_KEY);
        requirePermissions(requestedPermissions, 0);
    }

    private void requirePermissions(ArrayList<String> requiredPermissions, int requestCode) {
        requestPermissions(new String[] {requiredPermissions.get(0)}, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        String requestedPermission = permissions[0];

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestedPermissions.remove(0);
            if(requestedPermissions.size() > 0) {
                requirePermissions(requestedPermissions, requestCode);
            } else {
                Intent resultIntent = new Intent();
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        } else if(shouldShowRequestPermissionRationale(requestedPermission)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Important Permission Required");
            builder.setMessage("This app requires some permissions to work. Please grant these permissions.");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) { requirePermissions(requestedPermissions, 0); }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                Intent resultIntent = new Intent();
                setResult(Activity.RESULT_CANCELED, resultIntent);
                finish();
                }
            });
            builder.show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Important Permission Required");
            builder.setMessage("This feature requires some permissions to work. Please grant these permissions in Settings.");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent resultIntent = new Intent();
                    setResult(Activity.RESULT_CANCELED, resultIntent);
                    finish();
                }
            });
            builder.show();
        }
    }
}

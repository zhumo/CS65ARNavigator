package edu.dartmouth.com.arnavigation;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

/**
 * Created by mozhu on 2/24/18.
 */

public class PermissionManager {
    public static void ensurePermission(String permission, Activity originatingActivity, int requestCode, OnHasPermission listener) {
        if(hasPermission(permission, originatingActivity)) {
            listener.onHasPermission();
        } else {
            Intent permissionsActivityIntent = new Intent(originatingActivity, PermissionsActivity.class);
            permissionsActivityIntent.putExtra(PermissionsActivity.PERMISSION_KEY, permission);

            originatingActivity.startActivityForResult(permissionsActivityIntent, requestCode);
        }
    }

    public static boolean hasPermission(String permission, Activity originatingActivity) {
        if(Build.VERSION.SDK_INT < 23) { return true; }

        return originatingActivity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public interface OnHasPermission {
        void onHasPermission();
    }
}

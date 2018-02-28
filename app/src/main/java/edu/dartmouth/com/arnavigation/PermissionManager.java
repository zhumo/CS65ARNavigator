package edu.dartmouth.com.arnavigation;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mozhu on 2/24/18.
 */

public class PermissionManager {

    public static void ensurePermissions(Activity originatingActivity, int requestCode, @Nullable OnHasPermission listener, String... permissions) {
        List<String> ungrantedPermissions = new ArrayList<>();

        for(String permission : permissions) {
            if(!hasPermission(originatingActivity, permission)) { ungrantedPermissions.add(permission); }
        }

        if(ungrantedPermissions.size() == 0) {
            if(listener != null) {
                listener.onHasPermission();
            }
        } else {
            Intent permissionsActivityIntent = new Intent(originatingActivity, PermissionsActivity.class);
            String[] permissionsToBeRequested = (String[]) ungrantedPermissions.toArray();
            permissionsActivityIntent.putExtra(PermissionsActivity.PERMISSION_KEY, permissionsToBeRequested);

            originatingActivity.startActivityForResult(permissionsActivityIntent, requestCode);
        }
    }

    public static boolean hasPermission(Activity originatingActivity, String permission) {
        if(Build.VERSION.SDK_INT < 23) { return true; }

        return originatingActivity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public interface OnHasPermission {
        void onHasPermission();
    }
}

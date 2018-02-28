package edu.dartmouth.com.arnavigation.permissions;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.Nullable;

import java.util.ArrayList;

/**
 * Created by mozhu on 2/24/18.
 */

public class PermissionManager {

    public static void ensurePermissions(Activity originatingActivity, int requestCode, @Nullable OnHasPermission listener, String... permissions) {
        ArrayList<String> ungrantedPermissions = new ArrayList<>();

        for(String permission : permissions) {
            if(!hasPermission(originatingActivity, permission)) { ungrantedPermissions.add(permission); }
        }

        if(ungrantedPermissions.size() == 0) {
            if(listener != null) {
                listener.onHasPermission();
            }
        } else {
            Intent permissionsActivityIntent = new Intent(originatingActivity, PermissionsActivity.class);
            permissionsActivityIntent.putExtra(PermissionsActivity.PERMISSION_KEY, ungrantedPermissions);

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

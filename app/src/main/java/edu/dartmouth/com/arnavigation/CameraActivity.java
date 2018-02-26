package edu.dartmouth.com.arnavigation;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Created by mozhu on 2/24/18.
 */

public class CameraActivity extends Activity {

    private static int CAMERA_PERMISSION_REQUEST_CODE = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PermissionManager.ensurePermission(
                Manifest.permission.CAMERA,
                this,
                CAMERA_PERMISSION_REQUEST_CODE,
                null
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if(resultCode == Activity.RESULT_OK) { /* NOOP */ }
            else { finish(); }
        }
    }
}

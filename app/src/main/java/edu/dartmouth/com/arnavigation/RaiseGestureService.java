package edu.dartmouth.com.arnavigation;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.FloatMath;
import android.util.Log;
import android.widget.TextView;

public class RaiseGestureService extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;

    public static String PHONE_RAISED_ACTION = "phoneRaised";

    @Override
    public void onCreate() {
        super.onCreate();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    long lastUpdate;
    boolean atRest = false;
    boolean raising = false;
    boolean raised = false;
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // By default, long is 0, not null.
            if(lastUpdate == 0) { lastUpdate = event.timestamp; }

            if (raised) {
                Log.d("mztag", "Raised!");
                Intent broadcastPhoneRaisedIntent = new Intent();
                broadcastPhoneRaisedIntent.setAction(PHONE_RAISED_ACTION);
                sendBroadcast(broadcastPhoneRaisedIntent);
                atRest = false;
                raising = false;
                raised = false;
            } else if (raising) {
                Log.d("mztag", "Raising!");
                checkIfRaised(event);
            } else if (atRest) {
                Log.d("mztag", "At Rest!");
                checkIfRaising(event);
            } else {
                Log.d("mztag", "None!");
                checkIfAtRest(event);
            }
        }
    }

    public void checkIfRaised(SensorEvent event) {
        float yAcc = event.values[1];
        float zAcc = event.values[2];
        if(yAcc > 8.5 && zAcc <= 0){
            if ((event.timestamp - lastUpdate) < 200) { return; } // Ignore noisy blips
            lastUpdate = event.timestamp;
            raised = true;
        } else {
            lastUpdate = event.timestamp;
            raised = false;
            raising = false;
            atRest = false;
        }
    }

    public void checkIfRaising(SensorEvent event) {
        float yAcc = event.values[1];
        float zAcc = event.values[2];
        if(yAcc > 5.5 && yAcc <= 8.5 && zAcc > 0 && zAcc <= 8.5){
            if ((event.timestamp - lastUpdate) < 400) { return; } // Ignore noisy blips
            lastUpdate = event.timestamp;
            raising = true;
        } else {
            lastUpdate = event.timestamp;
            raising = false;
            atRest = false;
        }
    }

    public void checkIfAtRest(SensorEvent event) {
        float yAcc = event.values[1];
        float zAcc = event.values[2];
        if(yAcc > 3.5 && yAcc <= 5.5 && zAcc < 10.5 && zAcc > 8.5){
            if ((event.timestamp - lastUpdate) < 200) { return; } // Ignore noisy blips
            lastUpdate = event.timestamp;
            atRest = true;
        } else {
            lastUpdate = event.timestamp;
            atRest = false;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /* NOOP */ }
}

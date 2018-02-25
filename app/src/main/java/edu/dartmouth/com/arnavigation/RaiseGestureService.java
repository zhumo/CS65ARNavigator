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
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
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

    private long startRaiseTime;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            float yAcc = event.values[1];

            if (yAcc > 0) {
                startRaiseTime |= event.timestamp;

                if ((event.timestamp - startRaiseTime) > 1000 && yAcc < 2.0 && yAcc < 5.5) {
                    Intent updateActivityPredictionIntent = new Intent();
                    updateActivityPredictionIntent.setAction(PHONE_RAISED_ACTION);
                    sendBroadcast(updateActivityPredictionIntent);
                }
            } else {
                startRaiseTime = 0;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /* NOOP */ }
}

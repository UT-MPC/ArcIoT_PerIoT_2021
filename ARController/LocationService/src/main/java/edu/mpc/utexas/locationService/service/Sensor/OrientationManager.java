package edu.mpc.utexas.locationService.service.Sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * The main code for computing the orientation is from the official Android Doc:
 * https://developer.android.com/guide/topics/sensors/sensors_position#sensors-pos-orient
 */
public class OrientationManager implements SensorEventListener {
    private final String tag = "OrientationManager";
    private SensorManager sensorManager;
    private SensorEventListener mCallback;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];


    public OrientationManager(SensorManager sensorService, SensorEventListener callback) {
        sensorManager = sensorService;
        mCallback = callback;
    }

    public boolean startListen() {
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_FASTEST, SensorManager.SENSOR_DELAY_FASTEST);
        }
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_FASTEST, SensorManager.SENSOR_DELAY_FASTEST);
        }
        return true;
    }
    public boolean stopListen() {
        sensorManager.unregisterListener(this);
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading,
                    0, accelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading,
                    0, magnetometerReading.length);
        }
        if (mCallback != null) {
            mCallback.onSensorChanged(event);
        }
    }


    public void updateOrientationAngles(float[] rotation, float[] orientation) {
        SensorManager.getRotationMatrix(rotation, null,
                accelerometerReading, magnetometerReading);
        SensorManager.getOrientation(rotation, orientation);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something if the accuracy changed
        if (mCallback != null) {
            mCallback.onAccuracyChanged(sensor, accuracy);
        }
    }


}

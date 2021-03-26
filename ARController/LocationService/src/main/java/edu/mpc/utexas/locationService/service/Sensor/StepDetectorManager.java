package edu.mpc.utexas.locationService.service.Sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class StepDetectorManager implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor sensor;
    private SensorEventListener mCallback;
    private final String mTag = "StepCounterManager: ";
    public StepDetectorManager(SensorManager sensorService, SensorEventListener callback) {
        sensorManager = sensorService;
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        mCallback = callback;
    }
    public void updateCallback(SensorEventListener callback) {mCallback = callback;}

    public boolean startListen() {
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            return true;
        } else {
            return false;
        }
    }

    public boolean startListen(int freq) {
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, freq);
            return true;
        } else {
            return false;
        }
    }

    public boolean stopListen() {
        if (sensor != null) {
            sensorManager.unregisterListener(this, sensor);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mCallback != null) {
            mCallback.onSensorChanged(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int acc) {
        if (mCallback != null) {
            mCallback.onAccuracyChanged(sensor, acc);
        }
    }
}

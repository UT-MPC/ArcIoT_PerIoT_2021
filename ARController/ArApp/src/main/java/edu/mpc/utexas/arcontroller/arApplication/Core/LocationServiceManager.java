package edu.mpc.utexas.arcontroller.arApplication.Core;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import androidx.core.content.ContextCompat;
import edu.mpc.utexas.arcontroller.arApplication.Core.Device.Device;
import edu.mpc.utexas.arcontroller.arApplication.Core.Device.DeviceManager;
import edu.mpc.utexas.arcontroller.arApplication.Core.ObjectRecognition.ObjectTargets;
import edu.mpc.utexas.arcontroller.arApplication.R;
import edu.mpc.utexas.locationService.service.Bluetooth.Beacon;
import edu.mpc.utexas.locationService.service.Landmark.Landmark;
import edu.mpc.utexas.locationService.service.LocationService;

public class LocationServiceManager {
//    private SLAM_BTService test;
    private final String TAG = "LocationServiceManager: ";

    private boolean mBound = false;
    private LocationService mSlamService;

    private Activity parentActvity;
//    private final String TARGET_NULL = "No device tracked";

    private Device trackedDevice;
    private DeviceManager mDeviceBuilder;

    private Dialog mAccAlert;

    private LocationService.LocationServiceCallback mCallback = new LocationService.LocationServiceCallback(){
        @Override
        public void receiveBeacon(Beacon bcn) {
            LocationServiceManager.this.receiveBeacon(bcn);
        }

        public void onMagSensorAccuracyChanged(int acc) {
            LocationServiceManager.this.onMagSensorAccuracyChanged(acc);
        }
    };

    private Dialog alertMagCalibration() {
        LayoutInflater factory = LayoutInflater.from(parentActvity);
        View view = factory.inflate(R.layout.magnetic_calibration_alert, null);
        ImageView gifImageView = view.findViewById(R.id.calibrationImage);
        Glide.with(parentActvity).load(R.drawable.magnetic_calibration).into(gifImageView);
        mAccAlert = new AlertDialog.Builder(parentActvity)
                .setCancelable(false)
                .setMessage(R.string.mag_calibration_text)
                .setView(view)
                .create();

        return mAccAlert;
    }

    private void showMagAccAlert() {
        if (mAccAlert == null) {
            mAccAlert = alertMagCalibration();
        }
        if (!mAccAlert.isShowing()) {
            mAccAlert.show();
        }
    }

    private void hideMagAccAlert() {
        if (mAccAlert != null && mAccAlert.isShowing()) {
            mAccAlert.cancel();
        }
    }

    private void onMagSensorAccuracyChanged(int acc) {
        switch (acc) {
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                Log.d(TAG, "Magnetic sensor accuracy low, please calibrate");
                showMagAccAlert();
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                Log.d(TAG, "Magnetic sensor accuracy Medium");
                showMagAccAlert();
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                Log.d(TAG, "Magnetic sensor accuracy High");
                hideMagAccAlert();
                break;
        }
    }

    public LocationServiceManager(Activity parentActvity) {

        this.parentActvity = parentActvity;
        mDeviceBuilder = new DeviceManager(this);
    }

    private void receiveBeacon(Beacon bcn) {
        // Register the device with the device builder when we receive the signal.
        mDeviceBuilder.updateDevice(bcn.getDeviceAddress(), (int)bcn.getData()[0]);
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override public void onServiceConnected(ComponentName className, IBinder service) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            mSlamService = binder.getService();
            mSlamService.addListeners(mCallback);
            mSlamService.setParentActvity(parentActvity);
            mSlamService.startSensors();
            mSlamService.startScan();
            mBound = true;
        }

        @Override public void onServiceDisconnected(ComponentName arg0) {
            mSlamService = null;
            mBound = false;
        }
    };


    private Device getTargetDevice(String deviceType, double rightD, double topD, double camD) {
        if (!mBound) return null;
        Landmark l = mSlamService.getTargetLandmark(rightD, topD, camD);
        if (l == null) {
            return null;
        } else {
            return mDeviceBuilder.getDevice(deviceType, l.addr);
        }
    }

    public void checkPermission(Context baseContext, Activity targetActivity) {
        LocationService.checkPermission(baseContext, targetActivity);
    }

    public void startService() {
        Intent serviceIntent = new Intent(parentActvity, LocationService.class);
        serviceIntent.putExtra("contextText", "Foreground Service Example in Android");
        serviceIntent.putExtra("targetActivity", parentActvity.getClass().getName());
        ContextCompat.startForegroundService(parentActvity, serviceIntent);

        if (!mBound) {
            // Bind to the service
            parentActvity.bindService(new Intent(parentActvity, LocationService.class), mConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

    public void resetService() {
        mSlamService.stopWithReset();
        unBind();
        startService();

    }

    public void unBind() {
        if (mBound) {
            parentActvity.unbindService(mConnection);
            mBound = false;
        }
    }

    public void stopService() {
        unBind();
        Intent serviceIntent = new Intent(parentActvity, LocationService.class);
        parentActvity.stopService(serviceIntent);
        mBound = false;
    }

    public boolean isBind() { return mBound;}

    public void trackPosUpdate(String deviceType, double rightD, double topD, double camD) {
        if (deviceType.contains("thingy")) {
            trackedDevice = getTargetDevice(deviceType, rightD, topD, camD);
        } else {
            createWithoutLoc(deviceType, "000000");
        }

    }

    public void createWithoutLoc(String deviceType, String addr) {
        trackedDevice = mDeviceBuilder.getDevice(deviceType, addr);
    }

    public void lostTrack() {
        trackedDevice = null;
    }

    public boolean isTracked() {
        if (trackedDevice == null) {
            return false;
        } else {
            return true;
        }
    }

    public Device getTrackedDevice() {
        return trackedDevice;
    }

    public void startAdvertising(byte[] payload) {
        if (mBound) {
            mSlamService.startAdvertising(payload);
        }
    }

    public void stopAdvertising() {
        if (mBound) {
            mSlamService.stopAdvertising();
        }
    }

    public void sendFeedback(String addr, boolean isPositive) {
        if (mBound) {
            mSlamService.provideFeedback(addr, isPositive);
        }
    }

    public void onDestory() {
        if (mBound) {
            stopService();
        }
    }

    public void userInteracted(Device interactedDevice) {
        if (parentActvity instanceof ObjectTargets) {
            ((ObjectTargets) parentActvity).userInteractedWithDevice(interactedDevice);
        }
    }

    public void saveSnapshot() {
        if (mBound) {
            mSlamService.saveToSnapshot();
        }
    }

    public void restoreSnapshot() {
        if (mBound) {
            mSlamService.initFromSnapshot();
        }
    }
}

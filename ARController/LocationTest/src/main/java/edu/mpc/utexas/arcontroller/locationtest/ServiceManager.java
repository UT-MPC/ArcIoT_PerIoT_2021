package edu.mpc.utexas.arcontroller.locationtest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.LinearLayout;

import java.util.Arrays;

import androidx.core.content.ContextCompat;
import edu.mpc.utexas.locationService.UI.DrawView;
import edu.mpc.utexas.locationService.service.Bluetooth.BleManager;
import edu.mpc.utexas.locationService.service.Landmark.Landmark;
import edu.mpc.utexas.locationService.service.LocationService;
import edu.mpc.utexas.locationService.service.Sensor.OrientationManager;
import edu.mpc.utexas.locationService.service.Sensor.StepCounterManager;
import edu.mpc.utexas.locationService.service.Sensor.StepDetectorManager;

import static edu.mpc.utexas.locationService.service.Bluetooth.Config.DISABLE_SCAN;
import static edu.mpc.utexas.locationService.service.Bluetooth.Config.ENABLE_SCAN;

/**
 * To get a stable scan report rate, the scanning of BLE beacons must be
 * put into the UI thread.
 * This class is a wrapper that put BLE related instance into UI thread but does not handle logic.
 *
 */
public class ServiceManager {
    private final String TAG = "UI_ServiceManager: ";

    private boolean mBound = false;
    private LocationService mSlamService;

    private Activity parentActvity;
    private DrawView mDrawer;

    private final boolean showGraph = true;

    private String lastInteractedAddr;

    private boolean isBleSending;
    private Handler bleHandler;
    private Runnable stopAdvRun = new Runnable() {
        @Override
        public void run() {
            stopAdvertising();
            isBleSending = false;
        }
    };


    public ServiceManager(Activity parentActvity) {
        this.parentActvity = parentActvity;
    }


    public void simOneStep() {
        if (mBound) {
            mSlamService.simOneStep();
        }
    }


    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override public void onServiceConnected(ComponentName className, IBinder service) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            mSlamService = binder.getService();
            mSlamService.setParentActvity(parentActvity);
            mSlamService.setShowGraph(showGraph);
            mSlamService.startSensors();
            mSlamService.setDrawer(mDrawer);
            mSlamService.startScan();
            mBound = true;
        }

        @Override public void onServiceDisconnected(ComponentName arg0) {
            mSlamService = null;
            mBound = false;
        }
    };


    public void stopScan() {
        if (mBound) {
            mSlamService.stopScan();
        }
    }
    public void startScan() {
        if (mBound) {
            mSlamService.startScan();
        }
    }


    public void checkPermission(Context baseContext, Activity targetActivity) {
        LocationService.checkPermission(baseContext, targetActivity);
    }

    public void startService() {
        Intent serviceIntent = new Intent(parentActvity, LocationService.class);
        serviceIntent.putExtra("contextText", "Foreground Service Example in Android");
        serviceIntent.putExtra("targetActivity", TestActivity.class.getName());
        ContextCompat.startForegroundService(parentActvity, serviceIntent);

        if (!mBound) {
            // Bind to the service
            parentActvity.bindService(new Intent(parentActvity, LocationService.class), mConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

    public DrawView newDrawer(Context ctx) {
        mDrawer = new DrawView(ctx);
        mDrawer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        return mDrawer;
    }

    public void resetService() {
        mSlamService.stopWithReset();
        unBind();
        startService();
        mDrawer.updateLoc(new double[]{0,0});
    }

    public void unBind() {
        if (mBound) {
            parentActvity.unbindService(mConnection);
            Log.d("locationService","unbind");
            mBound = false;
        }
    }
    public void stopService() {
        unBind();
        Intent serviceIntent = new Intent(parentActvity, LocationService.class);
        parentActvity.stopService(serviceIntent);
    }

    public void interactWithDevice() {
        if (!mBound) return;
        Landmark l = mSlamService.getTargetLandmark(0, 0, -0.5);
        if (l != null) {
            lastInteractedAddr = l.addr;
            mDrawer.markLandmark(l.addr, Color.GREEN);
            sendBleCmd(lastInteractedAddr);
        }
    }

    public void getFeedback(boolean isPositive) {
        if (mBound) {
            mSlamService.provideFeedback(lastInteractedAddr, isPositive);
        }
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

    public static byte[] macStr2Byte(String addr) {
        String[] macAddressParts = addr.split(":");
        byte[] macAddressBytes = new byte[6];
        for(int i=0; i<6; i++){
            Integer hex = Integer.parseInt(macAddressParts[i], 16);
            macAddressBytes[i] = hex.byteValue();
        }
        return macAddressBytes;
    }

    public void sendBleCmd(String addr) {
        byte[] addrBytes = macStr2Byte(addr);
        byte[] cmd = Arrays.copyOf(addrBytes, addrBytes.length + 1);
        cmd[addrBytes.length] = 02;

        if (isBleSending) {
            stopAdvertising();
            bleHandler.removeCallbacks(stopAdvRun);
        }
        startAdvertising(cmd);
        isBleSending = true;
        if (bleHandler == null) {
            bleHandler = new Handler();
        }
        bleHandler.postDelayed(stopAdvRun, 1000);
    }
}

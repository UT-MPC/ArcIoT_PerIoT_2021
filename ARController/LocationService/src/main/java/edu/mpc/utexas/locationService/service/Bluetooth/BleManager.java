package edu.mpc.utexas.locationService.service.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.mpc.utexas.locationService.service.Bluetooth.Config.SCAN_REPORT_DELAY;
import static edu.mpc.utexas.locationService.service.Bluetooth.Beacon.verifyBeacon;
import static edu.mpc.utexas.locationService.service.Bluetooth.Config.ENABLE_SCAN;
import static edu.mpc.utexas.locationService.service.Bluetooth.Config.DISABLE_SCAN;

import static edu.mpc.utexas.locationService.service.Bluetooth.Config.OPERATION_FAIL;
import static edu.mpc.utexas.locationService.service.Bluetooth.Config.OPERATION_SUCCEED;
import static edu.mpc.utexas.locationService.service.Bluetooth.Config.SCAN_INTERVAL_MS;
import static edu.mpc.utexas.locationService.service.Bluetooth.Config.SCAN_PERIOD_MS;

public class BleManager {
    private class AvgBeacon {
        double rssi;
        long count;
        Handler timer;
        boolean isStart;
        byte[] lastData;

        public AvgBeacon(double rssi, Handler timer, byte[] lastData) {
            this.rssi = rssi;
            this.count = 1;
            this.timer = timer;
            this.isStart = false;
            this.lastData = lastData;
        }
    }

    private static final String TAG = "BTManager";

    private BluetoothAdapter mBTAdapter;

    private BluetoothLeScanner mBTLeScanner;

    private List<ScanFilter> mScanFilters;

    private ScanSettings mScanSettings;
    private ScanCallback mScanCallback;

    private Map<String, AvgBeacon> timeFilter;
    private Handler mHandler = new Handler();
    private boolean mServiceEnabled = false;

    public boolean isScanning() {
        return mScanning;
    }

    private boolean mScanning = false;


    private Runnable startRunnable = new Runnable() {
        @Override
        public void run() {
            BleManager.this.startScan();
        }
    };

    private Runnable stopRunnable = new Runnable() {
        @Override
        public void run() {
            BleManager.this.stopScan();
        }
    };

    private BeaconCallback beaconCall;

    public BleManager() {
        this(null);
    }
    public BleManager(BeaconCallback beaconCallback) {
        ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
        scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);    // scan mode
        scanSettingsBuilder.setReportDelay(SCAN_REPORT_DELAY);    // scan mode
        this.mScanSettings = scanSettingsBuilder.build();
        this.mScanFilters = new ArrayList<>();
        //Add null scan filter to allow scan in sleep mode due updates in Android 8.1.
        this.mScanFilters.add(new ScanFilter.Builder().build());


        // Init. location updates
        timeFilter = new HashMap<>();

        // Init. callback for receiving a beacon
        if ( beaconCallback == null) this.beaconCall = new BeaconCallback();
        else this.beaconCall = beaconCallback;

    }

    public int scan(int command) {
        switch (command) {
            case ENABLE_SCAN:
                Log.d(TAG, "Enable Scan");
                startScan();
                mServiceEnabled = true;
                break;
            case DISABLE_SCAN:
                if (mServiceEnabled) {
                    Log.d(TAG, "Disable Scan");
                    mServiceEnabled = false;
                    stopScan();
                }
                break;
            default:
                Log.e(TAG, "receives unknown command (" + command + ").");
                return OPERATION_FAIL;
        }
        return OPERATION_SUCCEED;
    }


    public int startScan() {
        Log.d(TAG, "Start scanning.");
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        mScanCallback = new BleManager.BTScanCallback(this.beaconCall);
        mBTLeScanner = mBTAdapter.getBluetoothLeScanner();
        mBTLeScanner.startScan(mScanFilters, mScanSettings, mScanCallback);
        mScanning = true;
        mHandler.removeCallbacks(stopRunnable);
        mHandler.postDelayed(stopRunnable, SCAN_PERIOD_MS);
        return OPERATION_SUCCEED;
    }

    public int stopScan() {
        Log.d(TAG, "Stop scanning.");
        if (mScanning && mBTAdapter != null && mBTAdapter.isEnabled() && mBTLeScanner != null) {
            mBTLeScanner.stopScan(mScanCallback);
        }
        mScanCallback = null;
        mScanning = false;
        if (mServiceEnabled) {
            mHandler.removeCallbacks(startRunnable);
            mHandler.postDelayed(startRunnable, SCAN_INTERVAL_MS - SCAN_PERIOD_MS);
        }
        return OPERATION_SUCCEED;
    }
    private class BTScanCallback extends ScanCallback {
        private BeaconCallback beaconCall;
        public BTScanCallback (BeaconCallback bCall) {
            this.beaconCall = bCall;
        }
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            recoverBeacon(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                recoverBeacon(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE Scan Failed with code " + errorCode);
        }

        private void reportBeacon(String addr) {
            if (!mServiceEnabled) return;
            Beacon bcn = new Beacon();
            bcn.setDeviceAddress(addr);
            bcn.setRssi(timeFilter.get(addr).rssi);
            bcn.setEpTime(System.currentTimeMillis());
            bcn.setData(timeFilter.get(addr).lastData);
            this.beaconCall.callback(bcn);

            timeFilter.remove(addr);
        }

        private void recoverBeacon(ScanResult result) {
            //TODO: getTimestampNanos()
            if (verifyBeacon(result)) {
                final String addr = result.getDevice().getAddress();

                if (timeFilter.containsKey(addr) && timeFilter.get(addr).isStart) {
                    AvgBeacon rec = timeFilter.get(addr);
                    rec.rssi = (result.getRssi() + rec.count * rec.rssi) / (rec.count + 1);
                    rec.count++;
                    rec.lastData = result.getScanRecord().getBytes();
                    return;
                }

                Handler newT = new Handler();

                timeFilter.put(addr, new AvgBeacon(result.getRssi(), newT, result.getScanRecord().getBytes()));

                newT.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        reportBeacon(addr);
                    }
                }, Config.REPEAT_BEACON_FILTER_MS);
                timeFilter.get(addr).isStart = true;
            }
        }
    }
}

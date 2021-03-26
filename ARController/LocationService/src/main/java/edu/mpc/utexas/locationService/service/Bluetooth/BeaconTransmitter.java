package edu.mpc.utexas.locationService.service.Bluetooth;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import static android.bluetooth.le.AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY;
import static android.bluetooth.le.AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM;

public class BeaconTransmitter {
    private final String TAG = "BeaconTransmitter";
    private boolean mStarted;
    private BluetoothAdapter mBleAdapter;
    private BluetoothLeAdvertiser mBleAdvertiser;
    private AdvertiseCallback mAdvertiseCallback;

    private int txPowerLevel = ADVERTISE_TX_POWER_MEDIUM;
    private int advertisingMode = ADVERTISE_MODE_LOW_LATENCY;
    private boolean connectable = false;


    public BeaconTransmitter() {
        mBleAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBleAdapter != null) {
            mBleAdvertiser = mBleAdapter.getBluetoothLeAdvertiser();
        } else {
            Log.e(TAG, "In construct, cannot find BLE advertiser");
        }
    }

    public BeaconTransmitter(BluetoothAdapter adapter) {
        mBleAdapter = adapter;
        if (mBleAdapter != null) {
            mBleAdvertiser = mBleAdapter.getBluetoothLeAdvertiser();
        } else {
            Log.e(TAG, "In construct, cannot find BLE advertiser");
        }
    }

    public Map<Integer, byte[]> getManufacturerData(byte[] payload) {
        Map<Integer, byte[]> manufacturerData = new HashMap<>();
        manufacturerData.put(0xFFFE, payload);
        return manufacturerData;
    }

    public AdvertiseData getAdvertiseData(byte[] payload) {
        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        builder.setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false);
        Map<Integer, byte[]> manufacturerData = getManufacturerData(payload);
        if (manufacturerData.size() < 1) {
            return builder.build();
        }
        for (Map.Entry<Integer, byte[]> e : manufacturerData.entrySet()) {
            builder.addManufacturerData(e.getKey(), e.getValue());
        }

        return builder.build();
    }

    public AdvertiseSettings getAdvertiseSettings() {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        return builder.setAdvertiseMode(advertisingMode)
                .setConnectable(connectable)
                .setTxPowerLevel(txPowerLevel).build();
    }

    public void startAdvertising(byte[] payload) {
        try {
            mBleAdvertiser.startAdvertising(getAdvertiseSettings(), getAdvertiseData(payload), getAdvertiseCallback());
        } catch (Exception e) {
            Log.e(TAG, "In startAdvertising, cannot start advertising due to exception " + e);
        }

    }

    public void stopAdvertising() {
        if (!mStarted) {
            Log.d(TAG, "Skipping stop advertising -- not started");
            return;
        }
        Log.d(TAG, "Stopping advertising with object " + mBleAdvertiser);
        try {
            mBleAdvertiser.stopAdvertising(getAdvertiseCallback());
        }
        catch (IllegalStateException e) {
            Log.w(TAG, "Bluetooth is turned off. Transmitter stop call failed.");
        }
        mStarted = false;
    }

    private AdvertiseCallback getAdvertiseCallback() {
        if (mAdvertiseCallback == null) {
            mAdvertiseCallback = new AdvertiseCallback() {
                @Override
                public void onStartFailure(int errorCode) {
                    Log.e(TAG,"Advertisement start failed, code: " + errorCode);
                }

                @Override
                public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                    Log.i(TAG,"Advertisement start succeeded.");
                    mStarted = true;
                }
            };
        }
        return mAdvertiseCallback;
    }
}
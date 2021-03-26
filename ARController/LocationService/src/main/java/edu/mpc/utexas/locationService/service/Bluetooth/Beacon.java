package edu.mpc.utexas.locationService.service.Bluetooth;

import android.bluetooth.le.ScanResult;
import android.util.Log;

import java.util.Arrays;

public class Beacon {
    // these are the first four bytes expected in any BLEnd packet
    public static final byte[] beaconPrefix = {0x02, 0x01, 0x04, 0x1B, (byte) 0xFE}; //(byte) 0x8B <- STACON PREFIX

    String deviceAddress;
    long epTime;
    double rssi;

    byte[] data;

    public Beacon() {}

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public long getEpTime() {
        return epTime;
    }

    public void setEpTime(long epTime) {
        this.epTime = epTime;
    }

    public double getRssi() {
        return rssi;
    }

    public void setRssi(double rssi) {
        this.rssi = rssi;
    }

    public void setData(byte[] rawBytesWithPrefix) {
        this.data = Arrays.copyOfRange(rawBytesWithPrefix, beaconPrefix.length, rawBytesWithPrefix.length);
    }

    public byte[] getData() {
        return data;
    }

    public static boolean verifyBeacon(ScanResult scanResult) {
        byte[] rawBeacon = scanResult.getScanRecord().getBytes();
        for (int i = 0; i < beaconPrefix.length; ++i) {
            if (rawBeacon[i] != beaconPrefix[i]) {
                return false;
            }
        }
        return true;
    }


}
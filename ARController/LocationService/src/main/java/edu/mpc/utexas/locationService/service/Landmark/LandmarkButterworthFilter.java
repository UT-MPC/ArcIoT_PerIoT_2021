package edu.mpc.utexas.locationService.service.Landmark;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.mpc.utexas.locationService.service.Bluetooth.Beacon;
import edu.mpc.utexas.locationService.utility.RSSI_Helper;
import uk.me.berndporr.iirj.Butterworth;
import edu.mpc.utexas.locationService.service.Bluetooth.Config;

public class LandmarkButterworthFilter extends LandmarkFilter {
    LandmarkFilterType filterType;
    private final String TAG = "LandmarkLowpassFilter";
    private class StoredBeacon {
        public String macAddr;
        public Butterworth filter;
        public boolean changed;
        public boolean userMoved;
        public double filteredVal;
        public long lastEpTime;

        public StoredBeacon(String uid, Butterworth filter, long lastUpdate) {
            this.macAddr = uid;
            this.filter = filter;
            this.lastEpTime = lastUpdate;
            this.changed = true;
            this.userMoved = true;
            this.filteredVal = 0;
        }
    }
    private Map<String, StoredBeacon> devices;

    public LandmarkButterworthFilter() {
        this(LandmarkFilterType.RSSI);
    }

    public LandmarkButterworthFilter(LandmarkFilterType type) {
        filterType = type;
        this.devices = new HashMap<>();
    }

    @Override
    public void setFilterType(LandmarkFilterType type) {
        filterType = type;
    }

    @Override
    public void receiveBeacon(Beacon bcn) {
        if (filterType == LandmarkFilterType.Distance) {
            bcn.setRssi(RSSI_Helper.rssiToDistance(bcn.getRssi()));
        }
        if (devices.containsKey(bcn.getDeviceAddress())) {
            this.updateDevice(bcn);
        } else {
            this.registerDevice(bcn);
        }
    }

    private void registerDevice(Beacon bcn) {
        Butterworth btw = new Butterworth();
        btw.lowPass(Config.BUTTER_ORDER, Config.SAMPLE_FREQ,
                Config.SAMPLE_FREQ * Config.CUTOFF_RATE);
        StoredBeacon device = new StoredBeacon(bcn.getDeviceAddress(), btw, bcn.getEpTime());
        while (Math.abs(device.filteredVal - bcn.getRssi()) > Math.abs(bcn.getRssi() * Config.WARM_UP_ERROR_RATE)) {
            device.filteredVal = btw.filter(bcn.getRssi());
        }
        this.devices.put(bcn.getDeviceAddress(), device);
    }

    private void updateDevice(Beacon bcn) {
        if (devices.containsKey(bcn.getDeviceAddress())) {
            StoredBeacon device = devices.get(bcn.getDeviceAddress());
//            Log.d(TAG, "Before update " + RSSI_Helper.rssiToDistance(device.filteredVal) + " new " + bcn.getRssi());
            device.filteredVal = device.filter.filter(bcn.getRssi());
//            Log.d(TAG, "After update " + RSSI_Helper.rssiToDistance(device.filteredVal));
            device.changed = true;
            device.lastEpTime = bcn.getEpTime();
        } else {
            this.registerDevice(bcn);
        }
    }

    @Override
    public List<FilteredBeacon> getAllLandmark() {
        List<FilteredBeacon> out = new ArrayList<>();
        for (StoredBeacon device : devices.values()) {
                if (filterType == LandmarkFilterType.Distance) {
                    FilteredBeacon b = new FilteredBeacon(device.macAddr,
                            device.filteredVal,
                            device.lastEpTime);
                    out.add(b);
                } else {
                    FilteredBeacon b = new FilteredBeacon(device.macAddr,
                            RSSI_Helper.rssiToDistance(device.filteredVal),
                            device.lastEpTime);
                    out.add(b);
                }
        }
        return out;
    }

//    @Override
//    public List<FilteredBeacon> getUpdatedLandmark() {
//        List<FilteredBeacon> out = new ArrayList<>();
//        for (StoredBeacon device : devices.values()) {
//            if (device.changed) {
//                device.changed = false;
//                if (filterType == LandmarkFilterType.Distance) {
//                    FilteredBeacon b = new FilteredBeacon(device.macAddr,
//                            device.filteredVal,
//                            device.lastEpTime);
//                    out.add(b);
//                } else {
//                    FilteredBeacon b = new FilteredBeacon(device.macAddr,
//                            RSSI_Helper.rssiToDistance(device.filteredVal),
//                            device.lastEpTime);
//                    out.add(b);
//                }
//            }
//        }
//        return out;
//    }

    @Override
    public FilteredBeacon getLandmarkByAddr(String addr) {
        if (devices.containsKey(addr)) {
            StoredBeacon device = devices.get(addr);
            if (filterType == LandmarkFilterType.Distance) {
                FilteredBeacon b = new FilteredBeacon(device.macAddr,
                        device.filteredVal,
                        device.lastEpTime);
                return b;
            } else {
                FilteredBeacon b = new FilteredBeacon(device.macAddr,
                        RSSI_Helper.rssiToDistance(device.filteredVal),
                        device.lastEpTime);
                return b;
            }
        }
        return null;
    }

//    @Override
//    public void userMoved() {
//        for (StoredBeacon device: devices.values()) {
//            device.userMoved = true;
//        }
//    }
}

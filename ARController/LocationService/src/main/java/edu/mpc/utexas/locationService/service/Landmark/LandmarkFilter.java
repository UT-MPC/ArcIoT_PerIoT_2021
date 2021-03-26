package edu.mpc.utexas.locationService.service.Landmark;

import java.util.List;

import edu.mpc.utexas.locationService.service.Bluetooth.Beacon;

public abstract class LandmarkFilter {
    public enum LandmarkFilterType {RSSI, Distance}
    public abstract void receiveBeacon(Beacon bcn);
    public abstract void setFilterType(LandmarkFilterType type);
//    public abstract List<FilteredBeacon> getUpdatedLandmark();
    public abstract List<FilteredBeacon> getAllLandmark();
    public abstract FilteredBeacon getLandmarkByAddr(String addr);
//    public abstract void userMoved();
}

package edu.mpc.utexas.locationService.service.Landmark;

public class FilteredBeacon {
    public String addr;
    public double distance;
    public long lastUpdateTime;
    public FilteredBeacon(String a, double d, long t) {
        addr = a;
        distance = d;
        lastUpdateTime = t;
    }

}

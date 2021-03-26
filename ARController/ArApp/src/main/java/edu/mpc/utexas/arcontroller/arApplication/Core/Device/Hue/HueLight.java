package edu.mpc.utexas.arcontroller.arApplication.Core.Device.Hue;


public final class HueLight {
    private static final String TAG = "PhilipsHueLight";
    private String uuid;
    private String name;
    private String accessName;
    private HueLightState state;

    public HueLightState getState() {
        return state;
    }

    public void setState(HueLightState state) {
        this.state = state;
    }

    public HueLight() {

    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccessName() {
        return accessName;
    }

    public void setAccessName(String accessName) {
        this.accessName = accessName;
    }
}

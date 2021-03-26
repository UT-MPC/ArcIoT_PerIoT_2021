package edu.mpc.utexas.locationService.service.Landmark;

public class Landmark {
    public static final int INVALID = -1;
    public static final int TRUTH = 1;
    public static final int EST = 0;

    public int type;
    public double x, y;
    public double varX, varY;
    public String addr;

    public Landmark(int type, double x, double y, double varX, double varY) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.varX = varX;
        this.varY = varY;
    }
    public Landmark(int type, double x, double y, double varX, double varY, String addr) {
        this(type, x, y, varX, varY);
        this.addr = addr;
    }

}

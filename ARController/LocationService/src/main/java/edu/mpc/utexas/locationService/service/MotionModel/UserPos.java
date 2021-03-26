package edu.mpc.utexas.locationService.service.MotionModel;

public class UserPos {
    public double x, y;
    public double heading;

    public UserPos(double x, double y, double heading) {
        this.x = x;
        this.y = y;
        this.heading = heading;
    }

    public void updateLoc(double[] loc) {
        this.x = loc[0];
        this.y = loc[1];
    }

    public void updateHeading(double[] oriMatrix) {
        heading = oriMatrix[0];
    }
}

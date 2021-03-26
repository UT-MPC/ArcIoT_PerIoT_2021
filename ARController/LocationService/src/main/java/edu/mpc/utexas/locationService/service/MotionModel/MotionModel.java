package edu.mpc.utexas.locationService.service.MotionModel;

public abstract class MotionModel {

    public abstract void updatePos(double[] data);
    public abstract double[] getPos();
    public abstract MotionModel copy();
}

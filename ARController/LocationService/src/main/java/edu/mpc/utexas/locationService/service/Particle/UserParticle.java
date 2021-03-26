package edu.mpc.utexas.locationService.service.Particle;

import edu.mpc.utexas.locationService.service.MotionModel.MotionModel;

public class UserParticle extends Particle{
    private MotionModel mUser;
    private final String TAG = "UserParticle";
    public UserParticle(MotionModel user, double weight) {
        super(weight);
        this.mUser = user;
    }

    @Override
    public void updatePos(double[] data) {
        if (this.mUser != null) {
            // Update the user's location with uncertainty.
            this.mUser.updatePos(data);
        }
    }

    @Override
    public double getX() {
        return this.mUser.getPos()[0];
    }

    @Override
    public double getY() {
        return this.mUser.getPos()[1];
    }


    @Override
    public void reweight(String addr, double measurement) {
//        double dx = l.x - getX();
//        double dy = l.y - getY();
//        double dist = Math.sqrt(dx*dx + dy*dy);
//        double cov = l.varX * (dx / dist) + l.varY*(dy/dist);
////        Log.d(TAG, "In reweight, " + measurement + " measured at " + dist);
//        this.weight *= MathFunc.normalPDF(measurement, dist, cov);
    }

    @Override
    public UserParticle copy() {
        return new UserParticle(mUser.copy(), weight);
    }
}

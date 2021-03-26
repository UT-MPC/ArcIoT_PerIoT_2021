package edu.mpc.utexas.locationService.service.Particle;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.mpc.utexas.locationService.service.Landmark.Landmark;
import edu.mpc.utexas.locationService.service.MotionModel.MotionModel;

import static edu.mpc.utexas.locationService.utility.Constant.LDMK_FDBK_FACTOR;
import static edu.mpc.utexas.locationService.utility.Constant.LDMK_FEEDBACK_RESET_THRESHOLD;
import static edu.mpc.utexas.locationService.utility.Constant.LDMK_NEG_FDBK_MAX;
import static edu.mpc.utexas.locationService.utility.Constant.LDMK_PARTICLE_VAR_THRESHOLD;
import static edu.mpc.utexas.locationService.utility.Constant.USER_PARTICLE_MIN_COV;
import static edu.mpc.utexas.locationService.utility.MathFunc.normalPDF;

public class UserParticle_EKF extends Particle{
    private class SaveLandmark{
        public double x, y;
        public double[][] cov;
        public SaveLandmark(double x, double y, double varX, double varY) {
            this.x = x;
            this.y = y;
            this.cov = new double[][]{
                    {varX, 0},
                    {0, varY},
            };
        }

        public SaveLandmark copy() {
            SaveLandmark c = new SaveLandmark(x, y, cov[0][0], cov[1][1]);
            c.cov[0][1] = cov[0][1];
            c.cov[1][0] = cov[1][0];
            return c;
        }

    }
    private MotionModel mUser;
    private Map<String, SaveLandmark> landmarks;
    private final String TAG = "UserParticle_EKF";
    private int fixCnt = 0;


    @Override
    public UserParticle_EKF copy() {
        UserParticle_EKF newP = new UserParticle_EKF(mUser.copy(), weight);
        for (Map.Entry<String, SaveLandmark> l : landmarks.entrySet()) {
            newP.landmarks.put(l.getKey(), l.getValue().copy());
        }
        return newP;
    }

    public UserParticle_EKF(MotionModel user, double weight) {
        super(weight);
        this.mUser = user;
        this.landmarks = new HashMap<>();
    }

    public Map<String, Landmark> getLandmarks() {
        Map<String, Landmark> ret = new HashMap<>();
        for (Map.Entry<String, SaveLandmark> l : landmarks.entrySet()) {
            SaveLandmark landmark = l.getValue();
//            Log.d(TAG, "estimate landmark (" + landmark.x + ", " + landmark.y + ", " +
//                    landmark.cov[0][0] + ", " + landmark.cov[1][1]);
            ret.put(l.getKey(), new Landmark(Landmark.EST, landmark.x, landmark.y,
                    landmark.cov[0][0], landmark.cov[1][1], l.getKey()));
        }
        return ret;
    }

    public Landmark getLandmark(String addr) {
        if (!landmarks.containsKey(addr)) return null;
        SaveLandmark l = landmarks.get(addr);
        return new Landmark(Landmark.EST, l.x, l.y, l.cov[0][0], l.cov[1][1], addr);
    }

    public Set<String> getAllLandmarkAddrs() { return landmarks.keySet();}

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

    public void addLandmark(Landmark l) {
        if (l.type == Landmark.EST) {
            this.landmarks.put(l.addr, new SaveLandmark(l.x, l.y, l.varX, l.varY));
        }
    }

    @Override
    public void reweight(String addr , double measurement) {
        if (!landmarks.containsKey(addr)) return;
        SaveLandmark l = landmarks.get(addr);
        double dx = getX() - l.x;
        double dy = getY() - l.y;

        double errorCov = USER_PARTICLE_MIN_COV + measurement / 2.0;

        double dist = Math.max(0.001, Math.sqrt(dx * dx + dy * dy));
        double det = measurement - dist;
        double[] H = new double[]{ -dx/dist, -dy/dist};

        double[] HC = new double[]{l.cov[0][0] * H[0] + l.cov[0][1] * H[1],
                l.cov[1][0] * H[0] + l.cov[1][1] * H[1]};
        double cov = HC[0] * H[0] + HC[1] * H[1] + errorCov;

        double[] K = new double[]{HC[0] / cov, HC[1] / cov};

        double[][] updateCov = new double[][]{
                {K[0] * K[0] * cov, K[0] * K[1] * cov},
                {K[1] * K[0] * cov, K[1] * K[1] * cov},
        };

        l.x += K[0] * det;
        l.y += K[1] * det;
        l.cov = new double[][] {
                {l.cov[0][0] - updateCov[0][0], l.cov[0][1] - updateCov[0][1]},
                {l.cov[1][0] - updateCov[1][0], l.cov[1][1] - updateCov[1][1]}
        };
        double gain = Math.max(0.001, normalPDF(measurement, dist, cov));
//        Log.d(TAG, "Resampling: The measurement is " + measurement + ".; the dist is " + dist);
//
//        Log.d(TAG, "Resampling: The reweight gain for particle at (" + getX() + ", " +
//                            getY() + ") is " + gain);
        this.weight *= gain;

//        Log.d(TAG, "!!!" + measurement + " det=" + det + " gain" + gain);
//        Log.d(TAG, "!!!" + l.x + " , " + l.y);
        if (measurement < LDMK_PARTICLE_VAR_THRESHOLD/2 && Math.abs(det) > LDMK_PARTICLE_VAR_THRESHOLD) {
            fixCnt += 1;
            if (fixCnt == 2) {
                landmarkFix(addr, getX(), getY());
                fixCnt = 0;
            }
        } else {
            fixCnt = 0;
        }
    }

    private double landmarkObs(String addr, double x, double y, boolean isPositive) {
        SaveLandmark l = landmarks.get(addr);
        double dx = x - l.x;
        double dy = y - l.y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (!isPositive )
            dist = Math.max(LDMK_FDBK_FACTOR, LDMK_NEG_FDBK_MAX / dist);
        this.weight *= LDMK_FDBK_FACTOR / dist;
        return dist;
    }

    private void landmarkFix(String addr, double x, double y) {
        SaveLandmark l = landmarks.get(addr);
        l.x = x;
        l.y = y;
        l.cov = new double[][]{
            {LDMK_PARTICLE_VAR_THRESHOLD/2, 0},
            {0, LDMK_PARTICLE_VAR_THRESHOLD/2},
        };
    }

    @Override
    public boolean feedback(String deviceID, double deviceX, double deviceY, boolean isPositive) {
        if (!landmarks.containsKey(deviceID)) return true;
        if (landmarkObs(deviceID, deviceX, deviceY, isPositive) > LDMK_FEEDBACK_RESET_THRESHOLD) {
            if (isPositive) {
                landmarkFix(deviceID, deviceX, deviceY);
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public void removeLandmark(String addr) {
        landmarks.remove(addr);
    }
}

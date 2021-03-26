package edu.mpc.utexas.locationService.service.MotionModel;

import android.util.Log;
import android.util.Pair;
import android.webkit.HttpAuthHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.mpc.utexas.locationService.service.Landmark.Landmark;
import edu.mpc.utexas.locationService.service.Particle.Particle;
import edu.mpc.utexas.locationService.service.Particle.ParticleFilter;
import edu.mpc.utexas.locationService.service.Particle.UserParticle;
import edu.mpc.utexas.locationService.service.Particle.UserParticle_EKF;
import edu.mpc.utexas.locationService.utility.MathFunc;

import static edu.mpc.utexas.locationService.utility.Constant.LDMK_PARTICLE_VAR_THRESHOLD;
import static edu.mpc.utexas.locationService.utility.Constant.USER_PARTICLE_REFRESH_ON_OBSERVATION;
import static edu.mpc.utexas.locationService.utility.MathFunc.computeDeltaAngle;
import static edu.mpc.utexas.locationService.utility.MathFunc.computeHeading;
import static edu.mpc.utexas.locationService.utility.MathFunc.limitRadian;
import static edu.mpc.utexas.locationService.utility.RSSI_Helper.distCalibration;

public class MotionParticleFilter {

    private int nParticles;
    private double stdRange, healthThreshold;
    private List<Particle> particles;
    private Map<String, Pair<Double, Integer>> lastObservations;
    private String mode;
    private Map<String, Landmark> landmarks;

    private final String TAG = "MotionParitcleFilter";

    public MotionParticleFilter(int nParticles, double healthThreshold, String mode) {
        this.nParticles = nParticles;
        this.healthThreshold = healthThreshold;
        this.particles = new ArrayList<>();
        for (int i = 0; i < this.nParticles; ++i) {
            if (mode == "EKF") {
                this.particles.add(new UserParticle_EKF(new FixedStepModel(), 1));
            } else {
                this.particles.add(new UserParticle(new FixedStepModel(), 1));
            }

        }
        lastObservations = new HashMap<>();
        this.mode = mode;
    }

    public void updatePos(double[] control) {
        for (Particle p : particles) {
            p.updatePos(control);
        }
    }

    public double[] estimatePos() {
        double[] weights = new double[particles.size()];
        for (int i = 0; i < particles.size(); ++i) {
            weights[i] = particles.get(i).weight;
        }
        weights = MathFunc.normalize(weights);
        double x = 0, y = 0;
        for (int i = 0; i < particles.size(); ++i) {
            x += particles.get(i).getX() * weights[i];
            y += particles.get(i).getY() * weights[i];
//            Log.d(TAG, "particle weight " + weights[i]);
        }
        Log.d(TAG, "estimate the user is at " + x + " " + y);
        return new double[]{x, y};
    }

    public Map<String, Landmark> estimateLandmarks() {
        if (landmarks == null) {
            landmarks = resampleLandmarks();
        }
        return landmarks;
    }

    private Map<String, Landmark> resampleLandmarks() {
        if (particles == null || particles.size() == 0) return new HashMap<>();
        if (!(particles.get(0) instanceof UserParticle_EKF)) {
            return null;
        }
        double[] weights = new double[particles.size()];
        for (int i = 0; i < particles.size(); ++i) {
            weights[i] = particles.get(i).weight;
        }
        weights = MathFunc.normalize(weights);
        Set<String> addrs = ((UserParticle_EKF) particles.get(0)).getAllLandmarkAddrs();
        Map<String, Landmark> ret = new HashMap<>();
        for (String addr : addrs) {
            double x = 0, y = 0, varX = 0, varY = 0;
            for (int i = 0; i < particles.size(); ++i) {
                Landmark l = ((UserParticle_EKF)particles.get(i)).getLandmark(addr);
                x += weights[i] * l.x;
                y += weights[i] * l.y;
                varX += weights[i] * l.varX;
                varY += weights[i] * l.varY;
            }
            ret.put(addr, new Landmark(Landmark.EST, x, y, varX, varY, addr));
        }
        logLandmarks(ret);
        return ret;
    }

    private void logLandmarks(Map<String, Landmark> l) {
        Log.d(TAG, " Resample Landmarks!!");
        for (Map.Entry<String, Landmark>kv : l.entrySet()) {
            Log.d(TAG, "Landmark #" + kv.getKey() + "# at (" + kv.getValue().x + ", " + kv.getValue().y + ")");
        }

    }

    private void resampleParticles() {
        if (ParticleFilter.SeqIR_EffectiveNumber(this.particles) < healthThreshold) {
            Log.d(TAG, "In resampling do resample!!");
            List<Integer> indices = ParticleFilter.resampleLowVar(
                    this.particles,nParticles);
            List<Particle> copy = new ArrayList<>();
            for (int i : indices) {
                Particle newP = particles.get(i).copy();
                newP.setWeight(1);
                copy.add(newP);
            }
            this.particles = copy;
        }

        landmarks = resampleLandmarks();
    }

    public void updateFilter() {
        Log.d(TAG, "In resampling");
        for (Map.Entry<String, Pair<Double, Integer>> e : lastObservations.entrySet()) {
            Log.d(TAG, "In resampling do observation");
            for (Particle p : particles) {
                p.reweight(e.getKey(), e.getValue().first);
            }
        }

        lastObservations.clear();
        resampleParticles();
    }

    public void addInitializedLandmark(Landmark landmark) {
        if (landmark.type != Landmark.EST) return;
        if (!mode.equals( "EKF")) return;

        Log.d(TAG, "save landmark " + landmark.addr);
        for (Particle p : particles) {
            ((UserParticle_EKF)p).addLandmark(landmark);
        }
        landmarks = resampleLandmarks();
    }

    public void updateObs(String addr, double dist, UserPos userPos) {
        double[] landmarkLoc = new double[] {estimateLandmarks().get(addr).x, estimateLandmarks().get(addr).y};
        double landmarkHeading = computeHeading(new double[]{userPos.x, userPos.y}, landmarkLoc);
        double deltaAngle = computeDeltaAngle(landmarkHeading, userPos.heading);
        if (deltaAngle > Math.PI/2 && dist > LDMK_PARTICLE_VAR_THRESHOLD) {
            return;
        }
        dist = distCalibration(dist, deltaAngle);
        if (lastObservations.containsKey(addr)) {
            Pair<Double, Integer> prev = lastObservations.get(addr);
            double avg = (prev.first * prev.second + dist) / (prev.second + 1);
            lastObservations.put(addr, new Pair<>(avg, prev.second + 1));
            if (prev.second + 1 > USER_PARTICLE_REFRESH_ON_OBSERVATION) {
                updateFilter();
            }

        } else {
            lastObservations.put(addr, new Pair<>(dist, 1));
        }
    }

    public boolean feedback(String deviceID, double deviceX, double deviceY, boolean isPositive) {
        boolean flag = true;
        for (Particle p : particles) {
            if (!p.feedback(deviceID, deviceX, deviceY, isPositive)) {
                flag = false;
            }
        }
        resampleParticles();
        return flag;
    }

    public void resetLandmark(String addr) {
        landmarks.remove(addr);
        for (Particle p : particles) {
            p.removeLandmark(addr);
        }
    }
}

package edu.mpc.utexas.locationService.service.Landmark;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import edu.mpc.utexas.locationService.service.MotionModel.UserPos;

import static edu.mpc.utexas.locationService.utility.Constant.*;

public class LandmarkInitialRegistry {
    private final String TAG = "LandmarkRegistry: ";
    private class LandmarkRecord {
        public DistParticleLandmark particles;
        public Landmark estimated;
        public boolean updated;

        public LandmarkRecord(DistParticleLandmark particles, Landmark estimated) {
            this.particles = particles;
            this.estimated = estimated;
            this.updated = false;

        }
    }

    Map<String, LandmarkRecord> registry;

    public LandmarkInitialRegistry() {
        registry = new HashMap<>();
    }

    private DistParticleLandmark initParticle(String addr) {
        return new DistParticleLandmark(LDMK_PARTICLE_INIT_NUM, LDMK_PARTICLE_SD,
                                        LDMK_PARTICLE_INIT_RS_THRESHOLD, LDMK_PARTICLE_VAR_THRESHOLD,
                                        LDMK_PARTICLE_RND, addr);
    }

    /**
     *
     * @param mac  The mac of the received beacon
     * @param r     The distance to this beacon
     * @param pos   The position of the user
     */
    public void beaconParticleFilter(String mac, double r, UserPos pos) {
//        Log.d(TAG, "Landmark registry received a beacon");
        if (!registry.containsKey(mac)) {
            Log.d(TAG, " Dicover landmark " + mac);
            LandmarkRecord newL = new LandmarkRecord(initParticle(mac), null);
            registry.put(mac, newL);
        }
        registry.get(mac).particles.addMeasure(pos, r);
        registry.get(mac).estimated = registry.get(mac).particles.estimateLandmarkPos();
        registry.get(mac).updated = true;
    }

    public Landmark estimatePos(String mac) {
        if (registry.containsKey(mac)) {
            return registry.get(mac).estimated;
        }
        return null;
    }

    public Map<String, Landmark> getAllLandmarks() {
        Map<String, Landmark> ret = new HashMap<>();
        for (Map.Entry<String, LandmarkRecord> l : registry.entrySet()) {
            ret.put(l.getKey(), l.getValue().estimated);
        }
        return ret;
    }

    public Map<String, Landmark> getUpdatedLandmarks() {
        Map<String, Landmark> ret = new HashMap<>();
        for (Map.Entry<String, LandmarkRecord> l : registry.entrySet()) {
            if (l.getValue().updated) {
                ret.put(l.getKey(), l.getValue().estimated);
                l.getValue().updated =false;
            }
        }
        return ret;
    }

    public String debugVarPos() {
        String logStr = "";
        for (Map.Entry<String, LandmarkRecord> e : registry.entrySet()) {
            if (e.getValue().estimated != null) {
                if (e.getValue().estimated.type != Landmark.INVALID) {
                    logStr = "The position estimated for "
                            + e.getKey() + " : \n pos(" +
                            e.getValue().estimated.x + ", " + e.getValue().estimated.y + ") " +
                            "var(" + e.getValue().estimated.varX + ", " + e.getValue().estimated.varY + ")";
                } else {
                    logStr = "The position estimated invalid "
                            + e.getKey() + " : \n " + "var(" + e.getValue().estimated.varX + ", " + e.getValue().estimated.varY + ")";
                }
            }
            Log.d(TAG, logStr);
        }
        return logStr;
    }

    public void resetLandmark(String addr) {
        registry.remove(addr);
    }
}

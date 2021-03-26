package edu.mpc.utexas.locationService.service;

import android.util.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.mpc.utexas.locationService.Database.LocationServiceDatabase;
import edu.mpc.utexas.locationService.service.Bluetooth.Beacon;
import edu.mpc.utexas.locationService.service.Landmark.FilteredBeacon;
import edu.mpc.utexas.locationService.service.Landmark.Landmark;
import edu.mpc.utexas.locationService.service.Landmark.LandmarkButterworthFilter;
import edu.mpc.utexas.locationService.service.Landmark.LandmarkFilter;
import edu.mpc.utexas.locationService.service.Landmark.LandmarkInitialRegistry;
import edu.mpc.utexas.locationService.service.MotionModel.MotionParticleFilter;
import edu.mpc.utexas.locationService.service.MotionModel.UserPos;

import static edu.mpc.utexas.locationService.utility.Constant.LDMK_MAX_TRACK_DIST;
import static edu.mpc.utexas.locationService.utility.Constant.USER_PARTICLE_NUM;
import static edu.mpc.utexas.locationService.utility.Constant.USER_PARTICLE_RS_THRESHOLD;

public class BleSlamManager {
    private static final String TAG = "BleSlamManager";
    private String CHANNEL_ID = "BleSlamManager";

    // Random number generator

    private MotionParticleFilter mUserParticleFilter;
    private LandmarkInitialRegistry mLandmarkReg;
    private Set<String> initializedLandmarks;

    private LandmarkFilter rssiInitFilter;
    private UserPos userPos;

    private double lastRSSI;

    public BleSlamManager() {
        rssiInitFilter = new LandmarkButterworthFilter(LandmarkFilter.LandmarkFilterType.RSSI);
        mUserParticleFilter = new MotionParticleFilter(USER_PARTICLE_NUM, USER_PARTICLE_RS_THRESHOLD, "EKF");
        userPos = new UserPos(0, 0, 0);
        userPos.updateLoc(mUserParticleFilter.estimatePos());
        mLandmarkReg = new LandmarkInitialRegistry();
        initializedLandmarks = new HashSet<>();
    }

    public String testing_BLE_distance() {
        return "" + lastRSSI;
//        String text = "";
//        for (FilteredBeacon bb: rssiInitFilter.getAllLandmark()) {
//            if (bb.addr.equals( "C2:84:A9:53:29:BB")) { text += " " + "(18)"; }
//            if (bb.addr.equals("DB:18:83:20:34:23")) { text += " " + "(14)"; }
//            text += " " + String.format("%.2f", bb.distance);
//        }
//        return text;
    }

    public void receiveBeacon(Beacon bcn) {
        // Handling the BLE beacons.
//        Log.d(TAG, "Get RSSI" + bcn.getRssi());
        lastRSSI = bcn.getRssi();
        rssiInitFilter.receiveBeacon(bcn);
        FilteredBeacon bb= rssiInitFilter.getLandmarkByAddr(bcn.getDeviceAddress());
        // TODO: if the user is too far away from the beacon, this observation should be dropped.
        if (bb.distance > LDMK_MAX_TRACK_DIST) return;

        if (initializedLandmarks.contains(bcn.getDeviceAddress())) {
            mUserParticleFilter.updateObs(bb.addr, bb.distance, userPos);
        } else {
            // If this landmark has not been initialized
            mLandmarkReg.beaconParticleFilter(bb.addr, bb.distance, userPos);
            Landmark est = mLandmarkReg.estimatePos(bb.addr);
            if (est.type == Landmark.EST) {
                mUserParticleFilter.addInitializedLandmark(est);
                initializedLandmarks.add(bb.addr);
                mLandmarkReg.resetLandmark(bb.addr);
                Log.d(TAG, "Landmark " + bb.addr + " is initialized at location (" + est.x + ", " + est.y + ")");
            }

        }
    }

    public Map<String, Landmark> getAllLandmarks() {
        Map<String, Landmark> ret = mUserParticleFilter.estimateLandmarks();
        if (ret != null) {
            return ret;
        } else {
            return mLandmarkReg.getAllLandmarks();
        }
    }

    public String debugPos() {
        return mLandmarkReg.debugVarPos();
    }

    //TODO: Test the order of update the user's position. If it is before
    //TODO: the update of the position, the particle model is not normal. But if it is after
    //TODO: the measurement is off. Test the after model first because it's how the other guy did this.
    public double[] updateOneStep(double[] data) {
//        Log.d(TAG, "Update Step??");
        // Still receive update step events in the background.
        mUserParticleFilter.updatePos(data);
//        Map<String, Landmark> updatedLandmark = mLandmarkReg.getUpdatedLandmarks();
        mUserParticleFilter.updateFilter();
        userPos.updateLoc(mUserParticleFilter.estimatePos());
        return new double[]{userPos.x, userPos.y};
    }

    public void updateUserOri(double[] oriMatrix) {
        userPos.updateHeading(oriMatrix);
    }

    public UserPos getUserPos() {return userPos;}

    public void resetLandmark(String addr) {
        if (initializedLandmarks.contains(addr)) initializedLandmarks.remove(addr);
        mLandmarkReg.resetLandmark(addr);
        mUserParticleFilter.resetLandmark(addr);
        LocationServiceDatabase database = LocationServiceDatabase.getDatabase();
        if (database != null) {
            database.deleteLandmark(addr);
        }
    }

    public void humanFeedback(String deviceID, double deviceX, double deviceY, boolean isPositive) {
        if (mUserParticleFilter != null) {
            boolean isRemove = mUserParticleFilter.feedback(deviceID, deviceX, deviceY, isPositive);
            if (isRemove) {
                resetLandmark(deviceID);
            }
        }
    }

    public void initializeFromSavedLandmarks(List<Landmark> landmarks) {
        if (mUserParticleFilter != null) {
            for (Landmark l : landmarks) {
                initializedLandmarks.add(l.addr);
                mUserParticleFilter.addInitializedLandmark(l);
            }
        }
    }
}

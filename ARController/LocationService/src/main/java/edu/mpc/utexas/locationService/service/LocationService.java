package edu.mpc.utexas.locationService.service;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import edu.mpc.utexas.locationService.Database.LocationServiceDatabase;
import edu.mpc.utexas.locationService.UI.DebugActivity;
import edu.mpc.utexas.locationService.UI.DrawView;
import edu.mpc.utexas.locationService.service.Bluetooth.Beacon;
import edu.mpc.utexas.locationService.service.Bluetooth.BeaconCallback;
import edu.mpc.utexas.locationService.service.Bluetooth.BeaconTransmitter;
import edu.mpc.utexas.locationService.service.Bluetooth.BleManager;
import edu.mpc.utexas.locationService.service.Landmark.Landmark;
import edu.mpc.utexas.locationService.service.MotionModel.UserPos;
import edu.mpc.utexas.locationService.service.Particle.Particle;
import edu.mpc.utexas.locationService.service.Sensor.OrientationManager;
import edu.mpc.utexas.locationService.service.Sensor.StepDetectorManager;
import edu.mpc.utexas.locationService.utility.Constant;

import static edu.mpc.utexas.locationService.service.Bluetooth.Config.DISABLE_SCAN;
import static edu.mpc.utexas.locationService.service.Bluetooth.Config.ENABLE_SCAN;
import static edu.mpc.utexas.locationService.utility.Constant.LDMK_MAX_MATCH_DIST;
import static edu.mpc.utexas.locationService.utility.Constant.MIN_STEP_INTERVAL_MS;
import static edu.mpc.utexas.locationService.utility.Constant.REQUEST_ENABLE_BT;
import static edu.mpc.utexas.locationService.utility.MathFunc.bivariateNormalPDF;
import static edu.mpc.utexas.locationService.utility.MathFunc.computeDeltaAngle;
import static edu.mpc.utexas.locationService.utility.MathFunc.computeHeading;
import static edu.mpc.utexas.locationService.utility.MathFunc.floatToDouble;
import static edu.mpc.utexas.locationService.utility.MathFunc.polarToCartesian;

public class LocationService extends ForegroundService {
    public interface LocationServiceCallback {
        void receiveBeacon (Beacon bcn);
        void onMagSensorAccuracyChanged (int acc);
    }

    private static final String TAG = "LocationService";
    BluetoothAdapter mBTAdapter;

    private StepDetectorManager mStepDetectorManager;


    private int stepCnt;
    private long lastStepDetect;
    private OrientationManager mOriManager;
    private SensorManager mSensorManager;
    private double[] oriMatrix;

    private BleSlamManager mSlamManager;

    private BleManager mBleManager;
    private BeaconTransmitter mBeaconTransmitter;
    private Activity parentActvity;
    private boolean showGraph;
    private DrawView mDrawer;

    private Context parentCtx;

    private Map<String, Landmark> queriedLandmark;

    private List<LocationServiceCallback> mListeners;

    PowerManager mPowerManager;
    PowerManager.WakeLock mWakeLock;
    LocationServiceDatabase mDatabaseDao;
    int magSensorAcc;

    public LocationService() {
        super();
        parentCtx = this;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LocationService started");
        mSlamManager = new BleSlamManager();
        mSensorManager = (SensorManager) parentCtx.getSystemService(Context.SENSOR_SERVICE);
        stepCnt = 0;
        mStepDetectorManager = new StepDetectorManager(mSensorManager, new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                onStepChanged(event);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                LocationService.this.onAccuracyChanged(sensor, accuracy);
            }
        });

        mOriManager = new OrientationManager(mSensorManager, new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                onOriChanged(event);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                LocationService.this.onAccuracyChanged(sensor, accuracy);

            }
        });
        mBleManager = new BleManager(new BeaconCallback(){
            @Override
            public void callback(Beacon bcn) {
                super.callback(bcn);
                LocationService.this.receiveBeacon(bcn);
            }
        });
        mBeaconTransmitter = new BeaconTransmitter();
        queriedLandmark = new HashMap<>();
        mListeners = new ArrayList<>();

        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "ARController::LocationService");

        initializeFromDatabse();
    }

    private void saveToDatabase() {
        if (mDatabaseDao != null) {
            mDatabaseDao.saveLandmarks(mSlamManager.getAllLandmarks().values());
        }
    }

    public void saveToSnapshot() {
        LocationServiceDatabase.initializeDatabaseSnapshot(getApplicationContext());
        LocationServiceDatabase snapshotDao = LocationServiceDatabase.getSnapshotDatabase();
        if (snapshotDao != null) {
            snapshotDao.saveLandmarkSnapshot(mSlamManager.getAllLandmarks().values());
        }
    }

    public void initFromSnapshot() {
        LocationServiceDatabase.initializeDatabaseSnapshot(getApplicationContext());
        LocationServiceDatabase snapshotDao = LocationServiceDatabase.getSnapshotDatabase();
        if (snapshotDao != null) {
            mSlamManager.initializeFromSavedLandmarks(snapshotDao.getSnapshotLandmarks());
        }
    }

    private void initializeFromDatabse() {

        LocationServiceDatabase.initializeDatabase(getApplicationContext());
        mDatabaseDao = LocationServiceDatabase.getDatabase();

        if (mDatabaseDao != null) {
            mSlamManager.initializeFromSavedLandmarks(mDatabaseDao.getAllLandmarks());
        }

    }

    private void resetDatabase() {
        // Delete all saved landmarks
        if (mDatabaseDao != null) {
            mDatabaseDao.deleteAllLandmarks();
        }

    }

    public void setParentActvity(Activity act) {
        parentActvity = act;
    }

    public void setShowGraph(boolean doesShow) {
        showGraph = doesShow;
    }

    public void setDrawer(DrawView drawer) {
        mDrawer = drawer;
    }

    public void addListeners(LocationServiceCallback listener) {
        mListeners.add(listener);
    }

    public boolean startSensors() {
        if (mOriManager != null) {
//            mStepManager.startListen();
            mStepDetectorManager.startListen();
            mOriManager.startListen();
            Log.d(TAG, "Sensor Registered");
            return true;
        } else {
            return false;
        }
    }


    public void onOriChanged(SensorEvent event) {
//        Log.d(TAG, "Orientation Update");
        float[] rotation = new float[9];
        float[] oriM = new float[3];
        mOriManager.updateOrientationAngles(rotation, oriM);
        oriMatrix = floatToDouble(oriM);
        mSlamManager.updateUserOri(oriMatrix);
        String oriOut = "";
        for (double x : oriMatrix) oriOut += String.format("%.2f", x) + " ";
        if (showGraph && mDrawer != null) {
            mDrawer.updateOri(oriMatrix[0], magSensorAcc);
        }

    }
    public void simOneStep() {
        Log.d(TAG, "Step detected");
        double[] loc = mSlamManager.updateOneStep(oriMatrix);
        if (showGraph && mDrawer != null) {
            mDrawer.updateLoc(loc);
        }
        Log.d(TAG, "User location update: " + String.format("%.4f", loc[0]) + " , " + String.format("%.4f", loc[1]));
        if (parentActvity != null && parentActvity instanceof DebugActivity) {
            ((DebugActivity) parentActvity).updateLoc(String.format("%.2f", loc[0]) + " , " + String.format("%.2f", loc[1]));
        }
    }

    public void detectStep() {
        if (System.currentTimeMillis() - lastStepDetect > MIN_STEP_INTERVAL_MS) {
            stepCnt += 1;
            simOneStep();
            lastStepDetect = System.currentTimeMillis();
        }
    }

    public void onStepChanged(SensorEvent event) {
        detectStep();
//        int totalStepSinceReboot = (int) event.values[0];
//        Date time = new Date(TimeConverter.sensorTimestampToEpoch(event.timestamp));
        int totalStepSinceReboot = stepCnt;
        if (parentActvity != null && parentActvity instanceof DebugActivity) {
            ((DebugActivity) parentActvity).updateStep("" + totalStepSinceReboot);
        }
    }
    public void onAccuracyChanged(Sensor sensor, int acc) {
        if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magSensorAcc = acc;
            for (LocationServiceCallback bcnCall : mListeners) {
                bcnCall.onMagSensorAccuracyChanged(acc);
            }
        }
    }
    public void receiveBeacon(Beacon bcn) {
        // Handling the BLE beacons.
//        Log.d(TAG, "GET one beacon of " + bcn.getRssi() + " at time: " + bcn.getEpTime());
        mSlamManager.receiveBeacon(bcn);
        if (parentActvity != null && parentActvity instanceof DebugActivity) {
            ((DebugActivity) parentActvity).updateDist(mSlamManager.testing_BLE_distance());
        }
        if (parentActvity != null && parentActvity instanceof DebugActivity  && !showGraph) {
            ((DebugActivity) parentActvity).updatePos(mSlamManager.debugPos());
        }
        if (showGraph && mDrawer != null) {
            mDrawer.updateLandmarks(mSlamManager.getAllLandmarks());
        }

        for (LocationServiceCallback bcnCall : mListeners) {
            bcnCall.receiveBeacon(bcn);
        }
    }

    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    public void stopScan() {
        if (mBleManager != null) {
            mBleManager.scan(DISABLE_SCAN);
        }
    }
    public void startScan() {
        if (mBleManager != null) {
            mBleManager.scan(ENABLE_SCAN);
        }
    }

    public BleManager getmBleManager() {
        return mBleManager;
    }

    public static void checkPermission(Context baseContext, Activity targetActivity) {
        if (ContextCompat.checkSelfPermission(baseContext, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting MotionModel permission");
            ActivityCompat.requestPermissions(targetActivity,
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, Constant.REQUEST_ACT_REC);
        }

        // Permission check for Bluetooth
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            throw new RuntimeException("Device does not support Bluetooth.");
        }
        if (!adapter.isEnabled() || ContextCompat.checkSelfPermission(baseContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            Log.d(TAG, "Requesting Bluetooth permission.");
            ActivityCompat.requestPermissions(targetActivity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ENABLE_BT);
        }
    }

    private double landmarkMatchDist(Landmark l, double[] targetLoc, UserPos userPos) {
        double x = targetLoc[0], y = targetLoc[1];

        double matchDist = Math.sqrt((l.x - x) * (l.x - x)  + (l.y - y) * (l.y - y));

        double landmarkHeading = computeHeading(new double[]{userPos.x, userPos.y}, new double[]{l.x, l.y});
        double deltaAngle = computeDeltaAngle(landmarkHeading, userPos.heading);

        // Add angle Penalization.
        matchDist += (1 - Math.cos(deltaAngle/2.0)) * matchDist;
        return matchDist;

    }

    public Landmark getTargetLandmarkAt(double x, double y) {
        Log.d(TAG, "1Matching target at "+x+", "+y);
        UserPos user = mSlamManager.getUserPos();
        double minMatchDist = LDMK_MAX_MATCH_DIST;
        minMatchDist = 100000;  // Change for the experiment;
        Landmark target = null;
        boolean flag = false;
        for (Map.Entry<String, Landmark> landmark : mSlamManager.getAllLandmarks().entrySet()) {
            Landmark l = landmark.getValue();
            if (l.type == Landmark.EST) {
                double matchDist = landmarkMatchDist(l, new double[]{x,y}, user);
                if (matchDist < minMatchDist) {
                    minMatchDist = matchDist;
                    target = l;
                }
            }
            flag = true;
        }

        if (target != null) {
            Log.d(TAG, "2Matched target " + target.addr + " with matching factor " + minMatchDist);
            if (parentActvity != null && parentActvity instanceof DebugActivity) {
                ((DebugActivity) parentActvity).updateProb(String.format("%.4f", minMatchDist));
            }
            queriedLandmark.put(target.addr,
                    new Landmark(Landmark.TRUTH, x, y, 0,0, target.addr));
        } else {
            if (flag)
                Log.d(TAG, "3Match target fail due to distance too far");
            else
                Log.d(TAG, "4Match target fail due to no landmark");
        }
        return target;
    }

    public Landmark getTargetLandmark(double rightD, double topD, double camD) {
        UserPos user = mSlamManager.getUserPos();
        double[] deltaH = polarToCartesian(-camD, user.heading);
        double[] deltaR = polarToCartesian(rightD, user.heading - Math.PI/2);

        double x = user.x + deltaH[0] + deltaR[0];
        double y = user.y + deltaH[1] + deltaR[1];
        return getTargetLandmarkAt(x, y);

    }

    public Landmark getTargetLandmark(double distance, double angle) {
        UserPos user = mSlamManager.getUserPos();
        double heading = user.heading + angle;
        double[] delta = polarToCartesian(distance, heading);
        double x = user.x + delta[0];
        double y = user.y + delta[1];
        return getTargetLandmarkAt(x, y);
    }

    public void provideFeedback(String addr, boolean isPositive) {
        if (queriedLandmark.containsKey(addr)) {
            mSlamManager.humanFeedback(addr, queriedLandmark.get(addr).x, queriedLandmark.get(addr).y, isPositive);
        }
    }

    private void stopServices() {
        saveToDatabase();
        mOriManager.stopListen();
        mStepDetectorManager.stopListen();
        stopScan();
        mSlamManager = new BleSlamManager();
    }

    public void stopWithReset() {
        Log.d(TAG, "Stopping service with reset database");

        stopServices();
        resetDatabase();
        stopForeground(true);
        stopSelf();
    }

    private final Binder mBinder = new LocalBinder();

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        mWakeLock.acquire();

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        stopServices();

        return true;
    }


    public void startAdvertising(byte[] payload) {
        mBeaconTransmitter.startAdvertising(payload);
    }

    public void stopAdvertising() {
        mBeaconTransmitter.stopAdvertising();
    }

}

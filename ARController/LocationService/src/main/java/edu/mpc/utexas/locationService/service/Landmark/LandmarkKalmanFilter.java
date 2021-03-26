package edu.mpc.utexas.locationService.service.Landmark;

import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.commons.math3.filter.MeasurementModel;
import org.apache.commons.math3.filter.ProcessModel;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.mpc.utexas.locationService.service.Bluetooth.Beacon;
import edu.mpc.utexas.locationService.service.Bluetooth.Config;
import edu.mpc.utexas.locationService.utility.RSSI_Helper;

public class LandmarkKalmanFilter extends LandmarkFilter{
    private final String TAG = "LandmarkKalmanFilter";
    private class StoredBeacon {
        public String macAddr;
        public KalmanFilter filter;
        public boolean changed;
        public boolean userMoved;
        public long lastEpTime;

        public StoredBeacon(String uid, KalmanFilter filter, long lastUpdate) {
            this.macAddr = uid;
            this.filter = filter;
            this.lastEpTime = lastUpdate;
            this.changed = true;
            this.userMoved = true;
        }
    }

    private double R, Q;
    private Map<String, StoredBeacon> devices;

    private LandmarkFilterType filterType;
    public LandmarkKalmanFilter() {
        this(LandmarkFilterType.RSSI);
    }
    public LandmarkKalmanFilter(LandmarkFilterType type) {
        this.filterType = type;
        this.R = Config.KF_MeasureNoise;
        this.Q = Config.KF_ProcessNoise;
        this.devices = new HashMap<>();

    }

    @Override
    public void setFilterType (LandmarkFilterType type) {
        filterType = type;
    }

    @Override
    public void receiveBeacon(Beacon bcn) {
        if (filterType == LandmarkFilterType.Distance) {
            bcn.setRssi(RSSI_Helper.rssiToDistance(bcn.getRssi()));
        }
        if (devices.containsKey(bcn.getDeviceAddress())) {
            this.updateDevice(bcn);
        } else {
            this.registerDevice(bcn);
        }
    }

    private void registerDevice(Beacon bcn) {
        KalmanFilter filter = new KalmanFilter(
                getProcessModel(this.Q, bcn.getRssi(), this.Q),
                getMeasureModel(this.R));
//        NativeKF  filter = new NativeKF(this.R, this.Q, bcn.getRssi(), this.Q);
        this.devices.put(bcn.getDeviceAddress(),
                new StoredBeacon(bcn.getDeviceAddress(), filter, bcn.getEpTime()));
    }

    private void updateDevice(Beacon bcn) {
        this.updateDevice(bcn, 0.0);
    }
    private void updateDevice(Beacon bcn, double controlSignal) {
        if (devices.containsKey(bcn.getDeviceAddress())) {
            StoredBeacon device = devices.get(bcn.getDeviceAddress());
            device.filter.predict(new double[] {controlSignal});
            device.filter.correct(new double[]{bcn.getRssi()});
            device.changed = true;
            device.lastEpTime = bcn.getEpTime();
        } else {
            this.registerDevice(bcn);
        }
    }

    @Override
    public List<FilteredBeacon> getAllLandmark() {
        List<FilteredBeacon> out = new ArrayList<>();
        for (StoredBeacon device : devices.values()) {
                device.changed = false;
                device.userMoved = false;
                if (filterType == LandmarkFilterType.Distance) {
                    FilteredBeacon b = new FilteredBeacon(device.macAddr,
                            device.filter.getStateEstimation()[0],
                            device.lastEpTime);
                    out.add(b);
                } else {
                    FilteredBeacon b = new FilteredBeacon(device.macAddr,
                            RSSI_Helper.rssiToDistance(device.filter.getStateEstimation()[0]),
                            device.lastEpTime);
                    out.add(b);
                }
        }
        return out;
    }

//    @Override
//    public List<FilteredBeacon> getUpdatedLandmark() {
//        List<FilteredBeacon> out = new ArrayList<>();
//        for (StoredBeacon device : devices.values()) {
//            if (device.changed) {
//                device.changed = false;
//                device.userMoved = false;
//                if (filterType == LandmarkFilterType.Distance) {
//                    FilteredBeacon b = new FilteredBeacon(device.macAddr,
//                            device.filter.getStateEstimation()[0],
//                            device.lastEpTime);
//                    out.add(b);
//                } else {
//                    FilteredBeacon b = new FilteredBeacon(device.macAddr,
//                            RSSI_Helper.rssiToDistance(device.filter.getStateEstimation()[0]),
//                            device.lastEpTime);
//                    out.add(b);
//                }
//            }
//        }
//        return out;
//    }

    @Override
    public FilteredBeacon getLandmarkByAddr(String addr) {
        if (devices.containsKey(addr)) {
            StoredBeacon device = devices.get(addr);
            if (filterType == LandmarkFilterType.Distance) {
                FilteredBeacon b = new FilteredBeacon(device.macAddr,
                        device.filter.getStateEstimation()[0],
                        device.lastEpTime);
                return b;
            } else {
                FilteredBeacon b = new FilteredBeacon(device.macAddr,
                        RSSI_Helper.rssiToDistance(device.filter.getStateEstimation()[0]),
                        device.lastEpTime);
                return b;
            }
        }
        return null;
    }

    public double getError(String addr) {
        if (devices.containsKey(addr)) {
            return devices.get(addr).filter.getErrorCovariance()[0][0];
        }
        return -1;
    }


    /**
     * Get the default process model for Kalman Filter of the RSSI signal
     * @return
     */
    private ProcessModel getProcessModel(double KF_Q, double initRSSI, double initCov) {
        // State transition vector
        RealMatrix A = new Array2DRowRealMatrix(new double[] { 1d });
        // No control signal
        RealMatrix B = new Array2DRowRealMatrix(new double[] { 1d });
        //
        RealMatrix Q = new Array2DRowRealMatrix(new double[] { KF_Q });
        RealVector x = new ArrayRealVector(new double[] { initRSSI });
        RealMatrix cov = new Array2DRowRealMatrix(new double[] { initCov});
        return new DefaultProcessModel(A, B, Q, x, cov);
    }


    /**
     * Get the default measurement model for Kalman Filter of the RSSI signal
     * @return
     */
    private MeasurementModel getMeasureModel(double KF_R) {
        RealMatrix H = new Array2DRowRealMatrix(new double[] { 1d });
        RealMatrix R = new Array2DRowRealMatrix(new double[] { KF_R});
        return new DefaultMeasurementModel(H, R);
    }


}

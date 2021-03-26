package edu.mpc.utexas.locationService.service.Bluetooth;

public class Config {
    public static final int OPERATION_SUCCEED = 0;
    public static final int OPERATION_FAIL = 1;

    public static final int SCAN_PERIOD_MS = 10000;
    public static final int SCAN_INTERVAL_MS = 11000;
    public static final int SCAN_REPORT_DELAY = 0;

    public static final int REPEAT_BEACON_FILTER_MS = 100;

    public static final int ENABLE_SCAN = 0;
    public static final int DISABLE_SCAN = 1;

    public static final double KF_MeasureNoise = 0.08;
    public static final double KF_ProcessNoise = 0.5;

    // butterworth filter parameter
    public static final double CUTOFF_RATE = 0.1;
    public static final double SAMPLE_FREQ = 1000.0 / REPEAT_BEACON_FILTER_MS;
    public static final int BUTTER_ORDER = 5;
    public static final double WARM_UP_ERROR_RATE = 0.1;
}
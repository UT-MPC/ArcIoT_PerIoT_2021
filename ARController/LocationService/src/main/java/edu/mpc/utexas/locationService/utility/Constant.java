package edu.mpc.utexas.locationService.utility;

public class Constant {
    public static final String DB_LDMK_TABLE = "SavedLandmark";


    public static final int REQUEST_ACT_REC = 1;
    public static final int REQUEST_ENABLE_BT = 2;
    public static final double THINGY_TX_POWER = -50;
    public static final double RSSI_TX_POWER = THINGY_TX_POWER;
    public static final double RSSI_PATH_N = 2.5;
    public static final double RSSI_VERTICAL_DIST = 0;
    public static final double RSSI_ANGLE_CALIBRATION = 5;

    public static final int USER_PARTICLE_NUM = 30;
    public static final double USER_PARTICLE_RS_THRESHOLD = 15;
    public static final double USER_PARTICLE_MIN_COV = 0.01;
    public static final double USER_PARTICLE_REFRESH_ON_OBSERVATION = 10;

    public static final int LDMK_PARTICLE_INIT_NUM = 200;
    public static final double LDMK_PARTICLE_INIT_RS_THRESHOLD = 75;
    public static final double LDMK_PARTICLE_SD = 1;
    public static final int LDMK_PARTICLE_RND = 20;
    public static final double LDMK_PARTICLE_VAR_THRESHOLD = 1;
    public static final int LDMK_MIN_VALID_OBSERVATIONS = 20;
    public static final double LDMK_MAX_TRACK_DIST = 8;
    public static final double LDMK_NEG_FDBK_MAX = 4;
    public static final double LDMK_FDBK_FACTOR = 1;
    public static final double LDMK_MAX_MATCH_DIST = 5;
    public static final double LDMK_FEEDBACK_RESET_THRESHOLD = LDMK_NEG_FDBK_MAX / 2.5;

    public static final long MIN_STEP_INTERVAL_MS = 100;



}

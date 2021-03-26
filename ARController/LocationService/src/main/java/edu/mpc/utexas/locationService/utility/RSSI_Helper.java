package edu.mpc.utexas.locationService.utility;

import static edu.mpc.utexas.locationService.utility.Constant.RSSI_ANGLE_CALIBRATION;

public class RSSI_Helper {
    public static double rssiToDistance(double rssi) {
        double verticalDist = Constant.RSSI_VERTICAL_DIST;
        double spaceDist = Math.pow(10, (Constant.RSSI_TX_POWER - rssi) / (10 * Constant.RSSI_PATH_N));
        if (spaceDist <= verticalDist) {
            return 0;
        }
        return Math.sqrt(spaceDist * spaceDist - verticalDist * verticalDist);
    }

//    public static double doubleRssiToDistanceCalibrated(double rssi, double angle) {
//        double verticalDist = Constant.RSSI_VERTICAL_DIST;
//        double spaceDist = Math.pow(10, (Constant.RSSI_TX_POWER - rssi) / (10 * Constant.RSSI_PATH_N));
//        if (angle < Math.PI*3/2) {
//
//        }
//        if (spaceDist <= verticalDist) {
//            return 0;
//        }
//        return 0;
//    }

    /**
     *
     * @param dist
     * @param angle
     * @return
     */
    public static double distCalibration(double dist, double angle) {
//        return Math.max(0,dist - Math.sin(angle/2) * RSSI_ANGLE_CALIBRATION);
        return Math.max(0,dist + distAdjust(angle));
    }

    public static double distAdjust(double angle) {
        return -(Math.cos(angle/2 + Math.PI) + 1) * RSSI_ANGLE_CALIBRATION;
    }
}

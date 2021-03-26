package edu.mpc.utexas.locationService.utility;

import java.util.Date;

public class TimeConverter {
    static public long sensorTimestampToEpoch(long sensorT) {
        long timeInMillis = (new Date()).getTime()
                + (sensorT - System.nanoTime()) / 1000000L;
        return timeInMillis;
    }
}

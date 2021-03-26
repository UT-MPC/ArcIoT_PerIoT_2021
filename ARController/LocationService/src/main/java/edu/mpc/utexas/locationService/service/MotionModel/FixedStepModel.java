package edu.mpc.utexas.locationService.service.MotionModel;

/**
 *      East(y-pos)
 *          ^
 *          |
 *          |
 *    ------+------>North(x-pos)
 *          |
 *          |
 *          |
 */

import android.util.Log;

import java.util.Random;

import static edu.mpc.utexas.locationService.service.MotionModel.Config.*;
import static edu.mpc.utexas.locationService.utility.MathFunc.polarToCartesian;
import static edu.mpc.utexas.locationService.utility.MathFunc.randNormal;

public class FixedStepModel extends MotionModel {
    public double x;
    public double y;
    private Random rand;
    private final String TAG = "FixedStepModel";

    public FixedStepModel() {
        this.x = USER_START_X;
        this.y = USER_START_Y;
        rand = new Random();
    }

    public FixedStepModel(double x, double y){
        this.x = x;
        this.y = y;
        rand = new Random();
    }

    @Override
    public FixedStepModel copy() {
        return new FixedStepModel(x, y);
    }

    @Override
    public void updatePos(double[] oriMatrix) {
        this.samplePos(oriMatrix, USER_DEFAULT_STEP);
    }

    public void samplePos(double[] oriMatrix, double dist) {
        if (rand.nextDouble() > USER_STEP_DETECTION_ERROR_RATE) {
            double heading = randNormal(oriMatrix[0], USER_HEADING_STD);
            dist = randNormal(dist, USER_STEP_STD);
            double[] delta = polarToCartesian(dist, heading);
            this.x += delta[0];
            this.y += delta[1];
            return;
        }
        Log.d(TAG, "Sample no step.");
    }

    @Override
    public double[] getPos() {
        return new double[]{x,y};
    }
}

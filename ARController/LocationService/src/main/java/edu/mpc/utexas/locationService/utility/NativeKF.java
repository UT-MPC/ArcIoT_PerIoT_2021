package edu.mpc.utexas.locationService.utility;

import android.util.Log;
/**
 * This is a WRONG implementation. Deprecated. For understanding only.
 */

public class NativeKF {
    float R,Q,A,B,C,cov,x;
    public NativeKF(float R, float Q, float initX, float initCov) {
        this.R = R;
        this.Q = Q;
        this.A = 1;
        this.B = 1;
        this.C = 1;

        this.x = initX;
        this.cov = initCov;
    }
    public void correct(float[] XX) {
        float z = XX[0];
        float predX = this.A*this.x;
        float P_m = this.cov + this.Q;

        float K = P_m * this.C * (1 / (P_m + this.R));
        this.x = predX + K * (z - (this.C * predX));
        Log.d("???", "" + K);
        this.cov = (1 - K) * P_m;
    }
    public float[] getStateEstimation() {
        return new float[]{this.x};
    }
}

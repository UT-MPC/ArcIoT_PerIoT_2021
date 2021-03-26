package edu.mpc.utexas.locationService.utility;

import java.util.Arrays;
import java.util.Random;

public class MathFunc {
    static private Random rd = new Random();

    static public double radianToDegree(double rad) {
        return rad * (180.0 / Math.PI);
    }

    static public double computeHeading(double[] pointFrom, double[] pointTo) {
        double x = pointTo[0] - pointFrom[0];
        double y = pointTo[1] - pointFrom[1];
        return limitRadian(Math.atan2(y, x));
    }

    static public double computeDeltaAngle(double angle0, double angle1) {
        double d = limitRadian(angle0 - angle1);
        return Math.min(d, limitRadian(-d+2*Math.PI));
    }

    static public double limitRadian(double rad) {
        if (rad >= 0 && rad <= Math.PI*2) return rad;
        while (rad < 0) {
            rad += Math.PI*2;
        }
        while (rad > Math.PI * 2) {
            rad -= Math.PI*2;
        }
        return rad;
    }

    static public double[] polarToCartesian(double r, double rad) {
        double x = r * Math.cos(rad);
        double y = r * Math.sin(rad);
        return new double[]{x,y};
    }

    static public double euclideanDist(double x0, double y0, double x1, double y1) {
        return Math.sqrt((x0-x1) * (x0-x1) + (y0-y1) * (y0-y1));
    }

    static public double[] normalize(double[] seq) {
        double sum = 0;
        double[] out = Arrays.copyOf(seq, seq.length);
        for (double x : seq) sum += x;
        for (int i = 0; i < seq.length; ++i) {
            out[i] = out[i] / sum;
        }
        return out;
    }

    /**
     * Generate a normal sample
     *
     * @param mean Mean \mu
     * @param std Standard Deviation \sigma
     * @return sample x
     */
    static public double randNormal(double mean, double std) {
        return rd.nextGaussian() * std + mean;
    }

    static public double randDouble(double min, double max) {
        return  rd.nextDouble() * (max - min) + min;
    }

    /**
     *  Compute the pdf of a normal sample based on wikipedia.
     *  https://en.wikipedia.org/wiki/Normal_distribution
     *
     * @param x X
     * @param mean Mean \mu
     * @param std Standard Deviation \sigma
     * @return pdf
     */
    static public double normalPDF(double x, double mean, double std) {
        double stdX = (x - mean) / std;
        double stdPDF = Math.exp(-stdX * stdX / 2.0) / Math.sqrt(2.0 * Math.PI);
        return stdPDF/std;
    }

    static public double getVar(Double[] data) {
        double sum = 0;
        double sumQ = 0;
        for (double d : data) {
            sum += d;
            sumQ += d * d;
        }
        int n = data.length;
        return (sumQ - ((sum*sum)/n)) / n;
    }

    static public double[] floatToDouble(float[] in) {
        double[] out = new double[in.length];
        for (int i = 0; i < in.length; ++i) out[i] = in[i];
        return out;
    }

    /**
     *
     * @param ux
     * @param uy
     * @param sigX
     * @param sigY
     * @param rho
     * @param x
     * @param y
     * @return
     */
    static public double bivariateNormalPDF(double ux, double uy, double sigX, double sigY, double rho, double x, double y) {
        double z = (x-ux)*(x-ux)/sigX/sigX + (y-uy)*(y-uy)/sigY/sigY - 2*rho*(x-ux)*(y-uy)/sigX/sigY;
        double pdf = 1 / (2* Math.PI*sigX*sigY*Math.sqrt(1-rho*rho)) * Math.exp(-z / (2 * (1-rho*rho)));
        return pdf;
    }

}

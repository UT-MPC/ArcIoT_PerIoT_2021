package edu.mpc.utexas.locationService.service.Landmark;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import edu.mpc.utexas.locationService.service.MotionModel.UserPos;
import edu.mpc.utexas.locationService.service.Particle.Particle;
import edu.mpc.utexas.locationService.service.Particle.ParticleFilter;
import edu.mpc.utexas.locationService.utility.MathFunc;

import static edu.mpc.utexas.locationService.utility.Constant.LDMK_MIN_VALID_OBSERVATIONS;
import static edu.mpc.utexas.locationService.utility.MathFunc.computeDeltaAngle;
import static edu.mpc.utexas.locationService.utility.MathFunc.computeHeading;
import static edu.mpc.utexas.locationService.utility.MathFunc.limitRadian;
import static edu.mpc.utexas.locationService.utility.MathFunc.polarToCartesian;
import static edu.mpc.utexas.locationService.utility.RSSI_Helper.distAdjust;
import static edu.mpc.utexas.locationService.utility.RSSI_Helper.distCalibration;

/**
 * Estimate the position of a landmark with particle filter.
 *
 */
public class DistParticleLandmark {

    private static final String TAG = "DistParticleLandmark";
    private final int PARTICLE_PER_ANGLE = 5;
    private int nParticles, nRandomResample;
    private double stdRange, healthThreshold, varThreshold;
    private int nMeasurements;
    private List<Particle> particles;
    private String addr;

    /**
     *
     * @param numParticle       number of total particles to use
     * @param stdRange          The std deviation of the particles corresponding
     *                          to the distance measurement
     * @param healthThreshold   The threshold for the health of the particles.
     *                          If below this threshold, the particles will be resmapled.
     * @param varThreshold      The variance threshold.
     * @param nRandom           The number random particles in each resampling.
     * @param addr              The mac address of the landmark.
     *
     */
    public DistParticleLandmark(int numParticle, double stdRange,
                                double healthThreshold, double varThreshold, int nRandom, String addr) {
        this.nParticles = numParticle;
        this.stdRange = stdRange;
        this.healthThreshold = healthThreshold;
        this.varThreshold = varThreshold;
        this.nRandomResample = nRandom;
        this.nMeasurements = 0;
        this.addr = addr;

    }

    /**
     * Initialize a set of particles in circle. The range is randomized based on the stdRange
     *
     * @param nParticles number of the particles to output
     * @param x The x coordinate of the user
     * @param y The y coordinate of the user
     * @param r The measured distance from the landmark to the user.
     * @return randomly initialized particles based on the first measurement
     */
    private List<Particle> initRandomly(int nParticles, double x, double y, double r) {
        //delta of the angles
        double deltaAngle = 2 * Math.PI / nParticles;
        List<Particle> particles = new ArrayList<>(nParticles);
        for (int i  = 0; i < nParticles; ++i) {
            double angle = i * deltaAngle;
            double radius = MathFunc.randNormal(r, this.stdRange);
            double[] coord = MathFunc.polarToCartesian(radius, angle);
            particles.add(new Particle(x+coord[0], y+coord[1], 1));
        }

        return particles;
    }

    private List<Particle> initRandomNonCircle(int nParticles, UserPos pos, double r) {
        //delta of the angles
        double stepAng = 2 * Math.PI / nParticles * PARTICLE_PER_ANGLE;
        List<Particle> particles = new ArrayList<>(nParticles);
        double userH = pos.heading;
        for (int i  = 0; i < nParticles / PARTICLE_PER_ANGLE; ++i) {
            double angleD = i * stepAng;
            double particleHeading = limitRadian(userH + angleD);
            double radius = r;
            radius = distCalibration(radius, Math.min(angleD, limitRadian(-angleD+2*Math.PI)));
//            Log.d("!!!",  Math.min(angleD, limitRadian(-angleD+2*Math.PI)) +" " + radius);
            for (int j = 0; j < PARTICLE_PER_ANGLE; ++j) {
                double[] coord = MathFunc.polarToCartesian(MathFunc.randNormal(radius, stdRange), particleHeading);
                particles.add(new Particle(pos.x+coord[0], pos.y+coord[1], 1));
            }
        }

        return particles;
    }


    /**
     * Update the weight of all the particles based on the probability of being correct.
     * In this case, since distance is the only measurement, we check the pdf of the
     * measured r wrt. the estimated r.
     * https://en.wikipedia.org/wiki/Particle_filter
     *
     * @param userPos The pos of the user.
     * @param r The measured distance from the landmark to the user.
     */
    private void updateWeights(UserPos userPos, double r) {
        for (Particle p : particles) {
            double estimatedR = MathFunc.euclideanDist(p.getX(), p.getY(), userPos.x, userPos.y);

            double[] landmarkLoc = new double[] {p.getX(), p.getY()};
            double landmarkHeading = computeHeading(new double[]{userPos.x, userPos.y}, landmarkLoc);
            double deltaAngle = computeDeltaAngle(landmarkHeading, userPos.heading);
            double calibratedR = r + distAdjust(deltaAngle);
            double pdf = MathFunc.normalPDF(calibratedR, estimatedR, this.stdRange);
            p.weight *= pdf;
        }
    }

    /**
     * Add a new measurement point, update the estimated position.
     * @param pos
     * @param signalR   The range detected from the RSSI signal
     */
    public void addMeasure(UserPos pos, double signalR) {
        double x = pos.x;
        double y = pos.y;

        if (this.particles ==  null) {
            this.particles = initRandomNonCircle(this.nParticles, pos, signalR);
        } else {
            this.updateWeights(pos, signalR);
            // Determine if resampling is needed.
            if (ParticleFilter.SeqIR_EffectiveNumber(this.particles) < healthThreshold) {
                List<Integer> indices = ParticleFilter.resampleLowVar(
                        this.particles,nParticles - nRandomResample);
                List<Particle> copy = new ArrayList<>();
                for (int i : indices) {
                    // TODO: check this algorithm
                    double nx = MathFunc.randNormal(this.particles.get(i).getX(), this.stdRange / 4.);
                    double ny = MathFunc.randNormal(this.particles.get(i).getY(), this.stdRange / 4.);
                    copy.add(new Particle(nx, ny, 1));
                }
                this.particles = copy;
                this.particles.addAll(initRandomly(nRandomResample, x, y, signalR));
            }

        }
        this.nMeasurements++;
    }

    private Landmark averagePos() {
        double[] weights = new double[particles.size()];
        for (int i = 0; i < particles.size(); ++i)
            weights[i] = particles.get(i).weight;
        weights = MathFunc.normalize(weights);
        double x = 0, y = 0;
        for (int i = 0; i < particles.size(); ++i) {
            x += particles.get(i).getX() * weights[i];
            y += particles.get(i).getY() * weights[i];
        }
        return new Landmark(Landmark.EST, x, y, 0,0, addr);
    }

    public Landmark estimateLandmarkPos() {
        if (this.nMeasurements < LDMK_MIN_VALID_OBSERVATIONS) {
            return new Landmark(Landmark.INVALID, 0, 0, 0, 0);
        }
        List<Double> lX = new ArrayList<>(nParticles);
        List<Double> lY = new ArrayList<>(nParticles);
        for (Particle p : particles) {
            lX.add(p.getX());
            lY.add(p.getY());
        }
        double varX = MathFunc.getVar(lX.toArray(new Double[0]));
        double varY = MathFunc.getVar(lY.toArray(new Double[0]));
        if (varX > this.varThreshold || varY > this.varThreshold) {
            // not enough confidence.
            return new Landmark(Landmark.INVALID, 0, 0, varX, varY);
        }
        Landmark est = averagePos();
        est.varX = varX;
        est.varY = varY;
        return est;
    }
}

package edu.mpc.utexas.locationService.service.Particle;

import java.util.ArrayList;
import java.util.List;

import edu.mpc.utexas.locationService.utility.MathFunc;

/**
 * This file contains some major algorithms used in this project related to Particle Filter.
 * https://en.wikipedia.org/wiki/Particle_filter
 */
public class ParticleFilter {
    /**
     * Sequtial Importance Resampling from Wikipedia
     * @param particles the particles
     * @return the effective number
     */
    static public double SeqIR_EffectiveNumber(List<Particle> particles) {
        double[] weights = new double[particles.size()];
        for (int i = 0; i < particles.size(); ++i)
            weights[i] = particles.get(i).weight;
        double[] normalWeights = MathFunc.normalize(weights);
        double sum = 0;
        for (double w : normalWeights) {
            sum += w * w;
        }
        return 1. / sum;
    }

    /**
     * Resample the particles based on the low variance resampling algorithm.
     *
     * @param particles
     * @param nSamples
     * @return
     */
    static public List<Integer> resampleLowVar(List<Particle> particles, int nSamples) {
        double[] weights = new double[particles.size()];
        for (int i = 0; i < particles.size(); ++i)
            weights[i] = particles.get(i).weight;
        double[] normalized = MathFunc.normalize(weights);

        double l = particles.size();
        double rand = MathFunc.randDouble(0, 1 / l);
        double w = normalized[0];
        int i = 0;
        List<Integer> out = new ArrayList<>(nSamples);
        for (int m = 1; m <= nSamples; ++m) {
            double U = rand + (m - 1) / l;
            while (U > w) {
                i += 1;
                w += normalized[i];
            }
            out.add(i);
        }
        return out;
    }
}

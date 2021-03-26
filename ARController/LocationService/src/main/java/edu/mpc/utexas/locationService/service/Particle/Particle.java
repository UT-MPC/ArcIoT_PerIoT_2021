package edu.mpc.utexas.locationService.service.Particle;

/**
 * https://en.wikipedia.org/wiki/Particle_filter
 */
public class Particle {
    private double x,y;

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double weight;

    public Particle(double weight) {
        this.weight = weight;
    }

    public Particle(double x, double y, double weight) {
        this.x = x;
        this.y = y;
        this.weight = weight;
    }

    /**
     * data[0]: x, data[1]: y
     * @param data data
     */
    public void updatePos(double[] data) {
        this.x = data[0];
        this.y = data[1];
    }

    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }
    public void reweight(String addr, double measurement) { }

    public Particle copy() {
        return new Particle(x, y, weight);
    }

    /**
     *
     * @param deviceID
     * @param deviceX
     * @param deviceY
     * @param isPositive
     * @return Voting for whether to reset this landmark when receiving a negative feedback.
     */
    public boolean feedback(String deviceID, double deviceX, double deviceY, boolean isPositive) {
        return false;
    }

    public void removeLandmark(String addr) {}

}

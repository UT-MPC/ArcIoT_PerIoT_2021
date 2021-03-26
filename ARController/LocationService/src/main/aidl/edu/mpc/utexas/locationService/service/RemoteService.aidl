// RemoteService.aidl
package edu.mpc.utexas.locationService.service;

// Declare any non-default types here with import statements

interface RemoteService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    String getDevice(double angle, double distance);
}

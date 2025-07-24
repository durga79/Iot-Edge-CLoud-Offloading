package org.hyboff.simulation.util;

import java.util.Calendar;

/**
 * Simplified simulation clock utility for the HybOff implementation.
 * Replaces CloudSim functionality for our standalone simulation.
 */
public class SimulationClock {
    private static Calendar simulationStartTime;
    private static double currentTime = 0;
    private static boolean initialized = false;
    
    /**
     * Initialize the simulation
     */
    public static void init(int numUsers, Calendar calendarStartTime, boolean traceFlag) {
        simulationStartTime = calendarStartTime;
        currentTime = 0;
        initialized = true;
        Log.printLine("SimulationClock initialized with start time: " + simulationStartTime.getTime());
    }
    
    /**
     * Get the current simulation time
     */
    public static double clock() {
        return currentTime;
    }
    
    /**
     * Advance the simulation clock
     */
    public static void advance(double time) {
        currentTime += time;
    }
    
    /**
     * Check if the simulation has been initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Start the simulation with the given end time
     */
    public static void startSimulation(double endTime) {
        if (!initialized) {
            throw new IllegalStateException("Simulation must be initialized before starting");
        }
        
        Log.printLine("Starting simulation - will run until time " + endTime);
        while (currentTime < endTime) {
            currentTime += 1.0; // Increment by 1 time unit
            // In a real implementation, we would process events here
        }
        
        Log.printLine("Simulation completed at time " + currentTime);
    }
}

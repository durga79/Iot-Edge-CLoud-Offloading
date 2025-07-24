package org.hyboff.simulation.util;

/**
 * Simple logging utility class for the HybOff simulation.
 * This provides similar functionality to CloudSim's Log class.
 */
public class Log {
    private static boolean enabled = true;
    
    /**
     * Disable logging
     */
    public static void disable() {
        enabled = false;
    }
    
    /**
     * Enable logging
     */
    public static void enable() {
        enabled = true;
    }
    
    /**
     * Print a message without a line break
     */
    public static void print(String message) {
        if (enabled) {
            System.out.print(message);
        }
    }
    
    /**
     * Print a message with a line break
     */
    public static void printLine(String message) {
        if (enabled) {
            System.out.println(message);
        }
    }
    
    /**
     * Print a formatted message with a line break
     */
    public static void format(String format, Object... args) {
        if (enabled) {
            System.out.println(String.format(format, args));
        }
    }
}

package org.hyboff.simulation.energy;

import org.hyboff.simulation.util.Log;

/**
 * Energy Model for simulating energy consumption in fog devices
 * Supports different power states and battery constraints
 */
public class EnergyModel {
    
    // Power states
    public enum State {
        IDLE,       // Device is idle
        PROCESSING, // Device is processing tasks
        TRANSMITTING, // Device is transmitting data
        RECEIVING,  // Device is receiving data
        SLEEP      // Device is in sleep mode
    }
    
    // Energy parameters
    private double batteryCapacity;    // Battery capacity in Joules
    private double remainingEnergy;    // Remaining energy in Joules
    private double idlePower;          // Power consumption when idle (W)
    private double processingPower;    // Power consumption when processing (W)
    private double transmitPower;      // Power consumption when transmitting (W)
    private double receivePower;       // Power consumption when receiving (W)
    private double sleepPower;         // Power consumption when sleeping (W)
    
    private boolean batteryEnabled;    // Whether battery constraints are enabled
    private boolean logEnabled;        // Whether to log energy events
    
    private String deviceId;           // ID of the associated device
    
    // Energy consumption statistics
    private double totalEnergyConsumed;
    private double idleEnergyConsumed;
    private double processingEnergyConsumed;
    private double transmitEnergyConsumed;
    private double receiveEnergyConsumed;
    
    /**
     * Create an energy model with specified parameters
     */
    public EnergyModel(String deviceId, double batteryCapacity, boolean batteryEnabled) {
        this.deviceId = deviceId;
        this.batteryCapacity = batteryCapacity;
        this.remainingEnergy = batteryCapacity;
        // Disable battery constraints for simulation purposes to focus on task scheduling
        this.batteryEnabled = false; // Override to always disable battery constraints
        this.logEnabled = false; // Disable energy logging to reduce output noise
        
        // Set default power values
        this.idlePower = 1.0;          // 1W when idle
        this.processingPower = 5.0;    // 5W when processing
        this.transmitPower = 2.0;      // 2W when transmitting
        this.receivePower = 1.5;       // 1.5W when receiving
        this.sleepPower = 0.1;         // 0.1W when sleeping
        
        // Initialize statistics
        this.totalEnergyConsumed = 0;
        this.idleEnergyConsumed = 0;
        this.processingEnergyConsumed = 0;
        this.transmitEnergyConsumed = 0;
        this.receiveEnergyConsumed = 0;
    }
    
    /**
     * Consume energy based on state and duration
     * @param state Power state
     * @param amount Amount of energy to consume (in joules) or duration (in seconds)
     * @return True if energy was available and consumed, false if battery depleted
     */
    public boolean consumeEnergy(State state, double amount) {
        double energyToConsume;
        
        // Calculate energy consumption based on state
        switch (state) {
            case IDLE:
                energyToConsume = idlePower * amount;
                idleEnergyConsumed += energyToConsume;
                break;
            case PROCESSING:
                energyToConsume = processingPower * amount;
                processingEnergyConsumed += energyToConsume;
                break;
            case TRANSMITTING:
                energyToConsume = transmitPower * amount;
                transmitEnergyConsumed += energyToConsume;
                break;
            case RECEIVING:
                energyToConsume = receivePower * amount;
                receiveEnergyConsumed += energyToConsume;
                break;
            case SLEEP:
                energyToConsume = sleepPower * amount;
                idleEnergyConsumed += energyToConsume; // Count as idle
                break;
            default:
                energyToConsume = amount; // Direct energy amount
        }
        
        // Update total energy consumed
        totalEnergyConsumed += energyToConsume;
        
        // If battery constraints are enabled, check if we have enough energy
        if (batteryEnabled) {
            if (remainingEnergy < energyToConsume) {
                if (logEnabled) {
                    Log.printLine(deviceId + ": Energy depleted, cannot perform operation");
                }
                return false;
            }
            
            // Deduct energy from battery
            remainingEnergy -= energyToConsume;
        }
        
        return true;
    }
    
    /**
     * Get remaining battery percentage
     * @return Percentage of battery remaining (0-100)
     */
    public double getBatteryPercentage() {
        return (remainingEnergy / batteryCapacity) * 100.0;
    }
    
    /**
     * Get remaining energy in Joules
     * @return Remaining energy
     */
    public double getRemainingEnergy() {
        return remainingEnergy;
    }
    
    /**
     * Get total energy consumed in joules
     */
    public double getTotalEnergyConsumed() {
        return totalEnergyConsumed;
    }
    
    /**
     * Calculate energy efficiency (tasks per joule)
     * @param completedTasks Number of completed tasks
     * @return Energy efficiency in tasks per joule
     */
    public double getEnergyEfficiency(int completedTasks) {
        if (totalEnergyConsumed <= 0 || completedTasks <= 0) {
            return 0;
        }
        // Cast completedTasks to double to ensure floating-point division
        return ((double)completedTasks) / totalEnergyConsumed;
    }
    
    /**
     * Get energy consumed while idle
     * @return Idle energy consumed
     */
    public double getIdleEnergyConsumed() {
        return idleEnergyConsumed;
    }
    
    /**
     * Get energy consumed while processing
     * @return Processing energy consumed
     */
    public double getProcessingEnergyConsumed() {
        return processingEnergyConsumed;
    }
    
    /**
     * Get energy consumed while transmitting
     * @return Transmitting energy consumed
     */
    public double getTransmitEnergyConsumed() {
        return transmitEnergyConsumed;
    }
    
    /**
     * Get energy consumed while receiving
     * @return Receiving energy consumed
     */
    public double getReceiveEnergyConsumed() {
        return receiveEnergyConsumed;
    }
    
    /**
     * Set power consumption for a specific state
     * @param state Power state
     * @param watts Power consumption in watts
     */
    public void setPowerConsumption(State state, double watts) {
        switch (state) {
            case IDLE:
                idlePower = watts;
                break;
            case PROCESSING:
                processingPower = watts;
                break;
            case TRANSMITTING:
                transmitPower = watts;
                break;
            case RECEIVING:
                receivePower = watts;
                break;
            case SLEEP:
                sleepPower = watts;
                break;
        }
    }
    
    /**
     * Enable or disable battery constraints
     * @param enabled Whether battery constraints are enabled
     */
    public void setBatteryEnabled(boolean enabled) {
        this.batteryEnabled = enabled;
    }
    
    /**
     * Enable or disable energy logging
     * @param enabled Whether logging is enabled
     */
    public void setLogEnabled(boolean enabled) {
        this.logEnabled = enabled;
    }
    
    /**
     * Recharge battery by a percentage
     * @param percentage Percentage to recharge (0-100)
     */
    public void rechargeBattery(double percentage) {
        double energyToAdd = (percentage / 100.0) * batteryCapacity;
        remainingEnergy = Math.min(batteryCapacity, remainingEnergy + energyToAdd);
        
        if (logEnabled) {
            Log.printLine(deviceId + ": Battery recharged by " + percentage + "%, now at " + 
                         getBatteryPercentage() + "%");
        }
    }
    
    /**
     * Reset energy statistics
     */
    public void resetStatistics() {
        totalEnergyConsumed = 0;
        idleEnergyConsumed = 0;
        processingEnergyConsumed = 0;
        transmitEnergyConsumed = 0;
        receiveEnergyConsumed = 0;
    }
}

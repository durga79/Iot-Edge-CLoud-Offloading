package org.hyboff.simulation.model;

/**
 * Energy model for IoT and Fog devices
 * Tracks power consumption for different operations
 */
public class EnergyModel {
    // Energy consumption parameters (in Joules)
    private double idlePowerConsumption;   // Power when idle (J/s)
    private double busyPowerConsumption;   // Power when busy (J/s)
    private double transmitPowerConsumption; // Power when transmitting (J/s)
    private double receivePowerConsumption;  // Power when receiving (J/s)
    
    // Energy tracking
    private double totalEnergyConsumed;    // Total energy consumed (J)
    private double operatingTime;          // Time device has been operating (s)
    private double transmitTime;           // Time spent transmitting (s)
    private double receiveTime;            // Time spent receiving (s)
    private double processingTime;         // Time spent processing tasks (s)
    private double idleTime;               // Time spent idle (s)
    
    // Battery parameters (for IoT devices)
    private double batteryCapacity;        // Battery capacity (J)
    private double remainingBattery;       // Remaining battery (J)
    private boolean hasBattery;            // Whether device has battery
    
    /**
     * Create energy model for a fog device (no battery constraints)
     */
    public EnergyModel(double idlePower, double busyPower, double transmitPower, double receivePower) {
        this.idlePowerConsumption = idlePower;
        this.busyPowerConsumption = busyPower;
        this.transmitPowerConsumption = transmitPower;
        this.receivePowerConsumption = receivePower;
        this.totalEnergyConsumed = 0;
        this.operatingTime = 0;
        this.transmitTime = 0;
        this.receiveTime = 0;
        this.processingTime = 0;
        this.idleTime = 0;
        this.hasBattery = false;
    }
    
    /**
     * Create energy model for an IoT device (with battery constraints)
     */
    public EnergyModel(double idlePower, double busyPower, double transmitPower, double receivePower, 
                      double batteryCapacity) {
        this(idlePower, busyPower, transmitPower, receivePower);
        this.batteryCapacity = batteryCapacity;
        this.remainingBattery = batteryCapacity;
        this.hasBattery = true;
    }
    
    /**
     * Update energy consumption for idle state
     * @param duration Time spent in idle state (s)
     * @return Actual energy consumed (may be less if battery depleted)
     */
    public double consumeIdleEnergy(double duration) {
        double energyRequired = idlePowerConsumption * duration;
        double energyConsumed = consumeEnergy(energyRequired);
        idleTime += duration;
        return energyConsumed;
    }
    
    /**
     * Update energy consumption for processing tasks
     * @param duration Time spent processing (s)
     * @param utilizationRatio CPU utilization ratio (0-1)
     * @return Actual energy consumed (may be less if battery depleted)
     */
    public double consumeProcessingEnergy(double duration, double utilizationRatio) {
        // Scale power between idle and busy based on utilization
        double power = idlePowerConsumption + 
                      (busyPowerConsumption - idlePowerConsumption) * utilizationRatio;
        double energyRequired = power * duration;
        double energyConsumed = consumeEnergy(energyRequired);
        processingTime += duration;
        return energyConsumed;
    }
    
    /**
     * Update energy consumption for transmitting data
     * @param dataSize Size of data transmitted (bytes)
     * @param bandwidth Available bandwidth (bytes/s)
     * @return Actual energy consumed (may be less if battery depleted)
     */
    public double consumeTransmitEnergy(double dataSize, double bandwidth) {
        double duration = dataSize / bandwidth;
        double energyRequired = transmitPowerConsumption * duration;
        double energyConsumed = consumeEnergy(energyRequired);
        transmitTime += duration;
        return energyConsumed;
    }
    
    /**
     * Update energy consumption for receiving data
     * @param dataSize Size of data received (bytes)
     * @param bandwidth Available bandwidth (bytes/s)
     * @return Actual energy consumed (may be less if battery depleted)
     */
    public double consumeReceiveEnergy(double dataSize, double bandwidth) {
        double duration = dataSize / bandwidth;
        double energyRequired = receivePowerConsumption * duration;
        double energyConsumed = consumeEnergy(energyRequired);
        receiveTime += duration;
        return energyConsumed;
    }
    
    /**
     * Internal method to consume energy and track battery
     */
    private double consumeEnergy(double energyRequired) {
        double energyConsumed = energyRequired;
        
        // If device has battery, check if enough energy is available
        if (hasBattery) {
            if (remainingBattery >= energyRequired) {
                remainingBattery -= energyRequired;
            } else {
                // Not enough energy, consume what's left
                energyConsumed = remainingBattery;
                remainingBattery = 0;
            }
        }
        
        totalEnergyConsumed += energyConsumed;
        return energyConsumed;
    }
    
    /**
     * Check if device has enough battery for operation
     * @param estimatedEnergy Estimated energy needed
     * @return true if enough battery or no battery constraints
     */
    public boolean hasEnoughBattery(double estimatedEnergy) {
        if (!hasBattery) {
            return true;  // No battery constraints
        }
        return remainingBattery >= estimatedEnergy;
    }
    
    /**
     * Get battery level as percentage
     * @return Battery percentage (0-100) or -1 if no battery
     */
    public double getBatteryPercentage() {
        if (!hasBattery) {
            return -1;
        }
        return (remainingBattery / batteryCapacity) * 100;
    }
    
    /**
     * Get total energy consumed
     */
    public double getTotalEnergyConsumed() {
        return totalEnergyConsumed;
    }
    
    /**
     * Get energy efficiency (operations per joule)
     * @param operationsCompleted Number of operations completed
     * @return Operations per joule or 0 if no energy consumed
     */
    public double getEnergyEfficiency(int operationsCompleted) {
        if (totalEnergyConsumed == 0) {
            return 0;
        }
        return operationsCompleted / totalEnergyConsumed;
    }
    
    /**
     * Reset energy model for a new simulation
     */
    public void reset() {
        totalEnergyConsumed = 0;
        operatingTime = 0;
        transmitTime = 0;
        receiveTime = 0;
        processingTime = 0;
        idleTime = 0;
        if (hasBattery) {
            remainingBattery = batteryCapacity;
        }
    }
}

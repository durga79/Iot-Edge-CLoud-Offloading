package org.hyboff.simulation.network;

import java.util.Random;

/**
 * Wireless Network Model for simulating different wireless protocols
 * Supports LoRaWAN, NB-IoT, WiFi, and 5G characteristics
 */
public class WirelessNetworkModel {
    
    // Network protocol types
    public enum Protocol {
        LORAWAN,   // Low power, long range, low bandwidth
        NBIOT,     // NB-IoT - cellular IoT
        WIFI,      // WiFi - higher bandwidth, shorter range
        CELLULAR_5G // 5G - high bandwidth, low latency
    }
    
    // Network parameters
    private Protocol protocol;
    private double baseBandwidth;      // Base bandwidth in Kbps
    private double baseLatency;        // Base latency in ms
    private double basePacketLoss;     // Base packet loss rate (0-1)
    private double baseRange;          // Base range in meters
    private double baseEnergyPerBit;   // Base energy consumption per bit
    private double jitter;             // Jitter factor (0-1)
    
    // Environmental factors
    private double interference;       // Interference level (0-1)
    private double signalStrength;     // Signal strength factor (0-1)
    private double congestion;         // Network congestion level (0-1)
    
    private Random random;
    
    /**
     * Create a wireless network model with the specified protocol
     */
    public WirelessNetworkModel(Protocol protocol) {
        this.protocol = protocol;
        this.random = new Random(System.currentTimeMillis());
        
        // Set default parameters based on protocol
        switch (protocol) {
            case LORAWAN:
                // LoRaWAN: Very low bandwidth, high range, low energy
                this.baseBandwidth = 50;      // 0.3-50 Kbps
                this.baseLatency = 1000;      // ~1-10 seconds
                this.basePacketLoss = 0.05;   // 5% packet loss
                this.baseRange = 15000;       // 15km range
                this.baseEnergyPerBit = 0.01; // Very low energy
                this.jitter = 0.2;           // High jitter
                break;
                
            case NBIOT:
                // NB-IoT: Low bandwidth, good range, medium energy
                this.baseBandwidth = 200;     // 60-200 Kbps
                this.baseLatency = 300;       // ~300ms
                this.basePacketLoss = 0.02;   // 2% packet loss
                this.baseRange = 10000;       // 10km range
                this.baseEnergyPerBit = 0.05; // Low energy
                this.jitter = 0.1;           // Medium jitter
                break;
                
            case WIFI:
                // WiFi: High bandwidth, low range, high energy
                this.baseBandwidth = 54000;   // 54 Mbps (802.11g)
                this.baseLatency = 10;        // ~10ms
                this.basePacketLoss = 0.01;   // 1% packet loss
                this.baseRange = 100;         // 100m range
                this.baseEnergyPerBit = 0.2;  // Higher energy
                this.jitter = 0.05;          // Low jitter
                break;
                
            case CELLULAR_5G:
                // 5G: Very high bandwidth, medium range, high energy
                this.baseBandwidth = 1000000; // 1 Gbps
                this.baseLatency = 1;         // ~1ms
                this.basePacketLoss = 0.005;  // 0.5% packet loss
                this.baseRange = 500;         // 500m range
                this.baseEnergyPerBit = 0.3;  // High energy
                this.jitter = 0.02;          // Very low jitter
                break;
        }
        
        // Initialize environmental factors
        this.interference = 0.1;    // 10% interference by default
        this.signalStrength = 0.9;  // 90% signal strength by default
        this.congestion = 0.2;      // 20% congestion by default
    }
    
    /**
     * Calculate actual bandwidth based on current conditions
     * @param distance Distance between devices in meters
     * @return Actual bandwidth in Kbps
     */
    public double calculateBandwidth(double distance) {
        // Calculate distance factor (decreases with distance)
        double distanceFactor = calculateDistanceFactor(distance);
        
        // Apply environmental factors
        double effectiveBandwidth = baseBandwidth * distanceFactor * signalStrength * (1 - interference) * (1 - congestion);
        
        // Add some randomness (jitter)
        effectiveBandwidth *= (1 - jitter * random.nextDouble());
        
        return Math.max(effectiveBandwidth, baseBandwidth * 0.1); // Minimum 10% of base bandwidth
    }
    
    /**
     * Calculate actual latency based on current conditions
     * @param distance Distance between devices in meters
     * @param packetSize Packet size in bytes
     * @return Actual latency in ms
     */
    public double calculateLatency(double distance, int packetSize) {
        // Base latency increases with distance and packet size
        double distanceFactor = Math.min(distance / baseRange, 1.0);
        double packetFactor = packetSize / 1024.0; // Normalize to KB
        
        // Calculate effective latency
        double effectiveLatency = baseLatency * (1 + distanceFactor) * (1 + 0.2 * packetFactor);
        
        // Apply environmental factors
        effectiveLatency *= (1 + interference) * (1 + congestion) / signalStrength;
        
        // Add jitter
        effectiveLatency *= (1 + jitter * random.nextDouble());
        
        return effectiveLatency;
    }
    
    /**
     * Calculate packet loss probability based on current conditions
     * @param distance Distance between devices in meters
     * @return Packet loss probability (0-1)
     */
    public double calculatePacketLoss(double distance) {
        // For simulation purposes, significantly reduce packet loss to ensure tasks can be transmitted
        // This helps focus on the task scheduling and processing aspects of the simulation
        
        // Packet loss increases with distance but at a much lower rate
        double distanceFactor = calculateDistanceFactor(distance);
        
        // Calculate effective packet loss with reduced values
        double effectivePacketLoss = (basePacketLoss * 0.1) + (1 - distanceFactor) * 0.05;
        
        // Apply environmental factors with reduced impact
        effectivePacketLoss += interference * 0.02 + congestion * 0.03 - signalStrength * 0.02;
        
        // Clamp to valid range with a much lower maximum
        return Math.max(0, Math.min(0.1, effectivePacketLoss));
    }
    
    /**
     * Calculate energy consumption for transmitting data
     * @param dataSize Data size in bytes
     * @param distance Distance in meters
     * @return Energy consumption in Joules
     */
    public double calculateTransmissionEnergy(int dataSize, double distance) {
        // Energy increases with data size and distance
        double distanceFactor = Math.pow(distance / 100, 2); // Quadratic with distance
        double bits = dataSize * 8;
        
        // Calculate energy based on protocol and conditions
        double energy = bits * baseEnergyPerBit * (1 + distanceFactor * 0.5);
        
        // Apply environmental factors
        energy *= (1 + interference * 0.3) / signalStrength;
        
        return energy;
    }
    
    /**
     * Simulate packet transmission
     * @param distance Distance between devices in meters
     * @param packetSize Packet size in bytes
     * @return Transmission result with latency and success status
     */
    public TransmissionResult simulateTransmission(double distance, int packetSize) {
        // Check if distance exceeds range
        if (distance > baseRange * 1.5) {
            // Disabled verbose network logging
            // System.out.println("NETWORK: Transmission failed - Out of range (" + distance + " m)");
            return new TransmissionResult(false, 0, "Out of range");
        }
        
        // Calculate actual latency
        double latency = calculateLatency(distance, packetSize);
        
        // Calculate packet loss probability
        double lossProb = calculatePacketLoss(distance);
        
        // Determine if packet is lost
        boolean success = random.nextDouble() > lossProb;
        
        // Calculate energy consumption
        double energyConsumed = calculateTransmissionEnergy(packetSize, distance);
        
        // Disabled verbose network logging
        // System.out.println("NETWORK: Transmission of " + packetSize + " bytes over " + distance + 
        //                  " m | Protocol: " + protocol + " | Loss Prob: " + String.format("%.4f", lossProb) + 
        //                  " | Result: " + (success ? "Success" : "Failed"));
        
        return new TransmissionResult(success, latency, energyConsumed, 
                                     success ? "Success" : "Packet lost");
    }
    
    /**
     * Simulate packet transmission with dataSize and distance parameters
     * @param dataSize Data size in bytes
     * @param distance Distance between devices in meters
     * @return Transmission result with latency and success status
     */
    public TransmissionResult simulateTransmission(int dataSize, double distance) {
        // This overload allows calling with parameters in reverse order
        return simulateTransmission(distance, dataSize);
    }
    
    /**
     * Calculate signal strength factor based on distance
     */
    private double calculateDistanceFactor(double distance) {
        // Linear decrease with distance
        double factor = 1 - (distance / baseRange);
        return Math.max(0, factor);
    }
    
    /**
     * Set environmental interference level
     * @param level Interference level (0-1)
     */
    public void setInterference(double level) {
        this.interference = Math.max(0, Math.min(1, level));
    }
    
    /**
     * Set network congestion level
     * @param level Congestion level (0-1)
     */
    public void setCongestion(double level) {
        this.congestion = Math.max(0, Math.min(1, level));
    }
    
    /**
     * Set signal strength factor
     * @param level Signal strength (0-1)
     */
    public void setSignalStrength(double level) {
        this.signalStrength = Math.max(0, Math.min(1, level));
    }
    
    /**
     * Get the current protocol
     */
    public Protocol getProtocol() {
        return protocol;
    }
    
    /**
     * Get maximum theoretical range
     */
    public double getBaseRange() {
        return baseRange;
    }
    
    /**
     * Class to represent transmission result
     */
    public static class TransmissionResult {
        private boolean success;
        private double latency;
        private double energyConsumed;
        private String message;
        
        public TransmissionResult(boolean success, double latency, String message) {
            this.success = success;
            this.latency = latency;
            this.energyConsumed = 0;
            this.message = message;
        }
        
        public TransmissionResult(boolean success, double latency, double energyConsumed, String message) {
            this.success = success;
            this.latency = latency;
            this.energyConsumed = energyConsumed;
            this.message = message;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public double getLatency() {
            return latency;
        }
        
        public double getEnergyConsumed() {
            return energyConsumed;
        }
        
        public String getMessage() {
            return message;
        }
    }
}

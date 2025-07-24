package org.hyboff.simulation.model;

import org.hyboff.simulation.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * The Monitor module of HybOff architecture.
 * Responsible for monitoring the resource utilization of a fog device
 * and collecting information from neighboring fog devices.
 */
public class HybOffMonitor {
    private FogDevice fogDevice;
    private Map<String, ResourceStatus> neighborStatus;
    
    // Thresholds for resource utilization status
    public static final double HIGH_LOAD_THRESHOLD = 0.8;   // 80% utilization
    public static final double MEDIUM_LOAD_THRESHOLD = 0.5; // 50% utilization
    public static final double LOW_LOAD_THRESHOLD = 0.3;    // 30% utilization
    
    public HybOffMonitor(FogDevice fogDevice) {
        this.fogDevice = fogDevice;
        this.neighborStatus = new HashMap<>();
    }
    
    /**
     * Monitor local resource utilization and return the current status
     */
    public ResourceStatus monitorLocalResources() {
        double cpuUtilization = fogDevice.getUtilizationRatio();
        int availableRam = fogDevice.getAvailableRam();
        int availableStorage = fogDevice.getAvailableStorage();
        double availableBandwidth = fogDevice.getAvailableBandwidth();
        
        return new ResourceStatus(
            fogDevice.getId(),
            cpuUtilization,
            availableRam,
            availableStorage,
            availableBandwidth,
            determineLoadStatus(cpuUtilization)
        );
    }
    
    /**
     * Update the status of a neighboring fog device
     */
    public void updateNeighborStatus(String neighborId, ResourceStatus status) {
        neighborStatus.put(neighborId, status);
    }
    
    /**
     * Get the status of a specific neighboring fog device
     */
    public ResourceStatus getNeighborStatus(String neighborId) {
        return neighborStatus.get(neighborId);
    }
    
    /**
     * Get the status of all neighboring fog devices
     */
    public Map<String, ResourceStatus> getAllNeighborStatus() {
        return new HashMap<>(neighborStatus);
    }
    
    /**
     * Determine load status based on CPU utilization
     */
    public LoadStatus determineLoadStatus(double cpuUtilization) {
        if (cpuUtilization >= HIGH_LOAD_THRESHOLD) {
            return LoadStatus.HIGH;
        } else if (cpuUtilization >= MEDIUM_LOAD_THRESHOLD) {
            return LoadStatus.MEDIUM;
        } else if (cpuUtilization >= LOW_LOAD_THRESHOLD) {
            return LoadStatus.LOW;
        } else {
            return LoadStatus.VERY_LOW;
        }
    }
    
    /**
     * Check if local resources can handle a specific task
     */
    public boolean canHandleTask(Task task) {
        int requiredMips = task.getSize();
        return fogDevice.getAvailableMips() >= requiredMips;
    }
    
    /**
     * Print current resource status
     */
    public void printStatus() {
        ResourceStatus status = monitorLocalResources();
        Log.printLine("Fog Device " + fogDevice.getId() + " Status:");
        Log.printLine("  CPU Utilization: " + String.format("%.2f", status.cpuUtilization * 100) + "%");
        Log.printLine("  Load Status: " + status.loadStatus);
        Log.printLine("  Available RAM: " + status.availableRam + " MB");
        Log.printLine("  Available Storage: " + status.availableStorage + " MB");
        Log.printLine("  Available Bandwidth: " + String.format("%.2f", status.availableBandwidth) + " Mbps");
    }
    
    /**
     * Enum for load status categories
     */
    public enum LoadStatus {
        VERY_LOW,
        LOW,
        MEDIUM,
        HIGH
    }
    
    /**
     * Class to represent resource status of a fog device
     */
    public static class ResourceStatus {
        public final String deviceId;
        public final double cpuUtilization;
        public final int availableRam;
        public final int availableStorage;
        public final double availableBandwidth;
        public final LoadStatus loadStatus;
        
        public ResourceStatus(
                String deviceId,
                double cpuUtilization,
                int availableRam,
                int availableStorage,
                double availableBandwidth,
                LoadStatus loadStatus) {
            
            this.deviceId = deviceId;
            this.cpuUtilization = cpuUtilization;
            this.availableRam = availableRam;
            this.availableStorage = availableStorage;
            this.availableBandwidth = availableBandwidth;
            this.loadStatus = loadStatus;
        }
    }
}

package org.hyboff.simulation.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyboff.simulation.util.Log;

/**
 * The Communication module of HybOff architecture.
 * Responsible for handling communication between fog devices,
 * including exchanging resource status and offloading tasks.
 */
public class HybOffCommunicator {
    private FogDevice fogDevice;
    private Map<String, Double> communicationLatencies;  // Device ID -> latency in ms
    private int messageCount;  // Count of messages sent for overhead calculation
    
    // Constants for communication simulation
    private static final double BASE_LATENCY = 10.0;  // Base latency in ms
    private static final double DISTANCE_FACTOR = 0.1;  // ms per distance unit
    
    public HybOffCommunicator(FogDevice fogDevice) {
        this.fogDevice = fogDevice;
        this.communicationLatencies = new HashMap<>();
        this.messageCount = 0;
    }
    
    /**
     * Send resource status update to a master fog device
     */
    public void sendStatusToMaster(FogDevice master, HybOffMonitor.ResourceStatus status) {
        if (master == null || master == fogDevice) {
            return;
        }
        
        double latency = calculateCommunicationLatency(master);
        messageCount++;
        
        // In a real system, this would involve network communication
        // In our simulation, we directly update the master's information
        master.getMonitor().updateNeighborStatus(fogDevice.getId(), status);
        
        if (fogDevice.isLogEnabled()) {
            Log.printLine(fogDevice.getId() + " sent status update to " + master.getId() + 
                          " (latency: " + String.format("%.2f", latency) + " ms)");
        }
    }
    
    /**
     * Send resource status updates to all devices in the same cell
     */
    public void broadcastStatusInCell(List<FogDevice> cellMembers, HybOffMonitor.ResourceStatus status) {
        for (FogDevice device : cellMembers) {
            if (device != fogDevice) {
                double latency = calculateCommunicationLatency(device);
                messageCount++;
                
                // Update the target device with our status
                device.getMonitor().updateNeighborStatus(fogDevice.getId(), status);
                
                if (fogDevice.isLogEnabled()) {
                    Log.printLine(fogDevice.getId() + " sent status update to " + device.getId() + 
                                 " (latency: " + String.format("%.2f", latency) + " ms)");
                }
            }
        }
    }
    
    /**
     * Offload a task to another fog device
     */
    public boolean offloadTask(Task task, FogDevice targetDevice) {
        if (targetDevice == null) {
            return false;
        }
        
        double latency = calculateCommunicationLatency(targetDevice);
        messageCount += 2;  // One for task, one for acknowledgment
        
        // Simulate communication delay
        // In a real system, this would involve network transmission
        boolean success = targetDevice.receiveTask(task);
        
        if (success) {
            if (fogDevice.isLogEnabled()) {
                Log.printLine(fogDevice.getId() + " offloaded task " + task.getId() + " to " + 
                             targetDevice.getId() + " (latency: " + String.format("%.2f", latency) + " ms)");
            }
            
            // Increase task response time by communication latency
            task.addResponseTime(latency);
            
            return true;
        } else {
            if (fogDevice.isLogEnabled()) {
                Log.printLine(fogDevice.getId() + " failed to offload task " + task.getId() + " to " + 
                             targetDevice.getId() + " (target device rejected task)");
            }
            return false;
        }
    }
    
    /**
     * Get master fog device in the cell
     */
    public FogDevice findMasterInCell(List<FogDevice> cellMembers) {
        for (FogDevice device : cellMembers) {
            if (device.isMaster()) {
                return device;
            }
        }
        return null;
    }
    
    /**
     * Get all fog devices in the same cell
     */
    public List<FogDevice> getCellMembers(List<FogDevice> allDevices) {
        List<FogDevice> cellMembers = new ArrayList<>();
        int cellId = fogDevice.getCellId();
        
        for (FogDevice device : allDevices) {
            if (device.getCellId() == cellId) {
                cellMembers.add(device);
            }
        }
        
        return cellMembers;
    }
    
    /**
     * Calculate communication latency between this device and another fog device
     */
    private double calculateCommunicationLatency(FogDevice targetDevice) {
        String targetId = targetDevice.getId();
        
        // Use cached latency if available
        if (communicationLatencies.containsKey(targetId)) {
            return communicationLatencies.get(targetId);
        }
        
        // Calculate latency based on distance
        double distance = calculateDistance(fogDevice.getX(), fogDevice.getY(), 
                                           targetDevice.getX(), targetDevice.getY());
        
        // Latency model: base + distance-based component
        double latency = BASE_LATENCY + (distance * DISTANCE_FACTOR);
        
        // Cache the calculated latency
        communicationLatencies.put(targetId, latency);
        
        return latency;
    }
    
    /**
     * Calculate Euclidean distance between two points
     */
    private double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
    
    /**
     * Get the total count of messages sent by this communicator
     */
    public int getMessageCount() {
        return messageCount;
    }
    
    /**
     * Reset the message counter
     */
    public void resetMessageCount() {
        messageCount = 0;
    }
}

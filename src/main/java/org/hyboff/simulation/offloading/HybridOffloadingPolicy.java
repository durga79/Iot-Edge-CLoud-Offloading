package org.hyboff.simulation.offloading;

import org.hyboff.simulation.model.FogDevice;
import org.hyboff.simulation.model.IoTDevice;
import org.hyboff.simulation.model.Task;
import java.util.*;

/**
 * Implementation of the HybOff (Hybrid Offloading) algorithm as described in the paper.
 * This policy combines the strengths of static and dynamic approaches to achieve better
 * load balancing and resource utilization in fog environments.
 */
public class HybridOffloadingPolicy implements OffloadingPolicy {
    // Static offloading tables for each cell (cell ID -> static offloading map)
    private Map<Integer, Map<String, String>> staticOffloadingTables;
    
    // System load thresholds
    private static final double HIGH_LOAD_THRESHOLD = 0.8;  // 80% utilization
    private static final double MEDIUM_LOAD_THRESHOLD = 0.5; // 50% utilization
    private static final double LOW_LOAD_THRESHOLD = 0.3;   // 30% utilization
    
    public HybridOffloadingPolicy() {
        this.staticOffloadingTables = new HashMap<>();
    }
    
    @Override
    public FogDevice selectOffloadingTarget(FogDevice sourceDevice, Task task, List<FogDevice> availableDevices) {
        // Get devices in the same cell
        List<FogDevice> cellMembers = getCellMembers(sourceDevice, availableDevices);
        
        if (task.isUrgent()) {
            // For urgent tasks: use dynamic approach for quick response
            return selectDynamically(sourceDevice, task, cellMembers);
        } else {
            // For normal tasks: try static approach first, then fallback to dynamic
            FogDevice target = selectStatically(sourceDevice, cellMembers);
            
            // Check if the static target can handle the task
            if (target != null && target.hasResourcesToProcess(task)) {
                return target;
            } else {
                // Fallback to dynamic selection
                return selectDynamically(sourceDevice, task, cellMembers);
            }
        }
    }
    
    @Override
    public boolean shouldOffload(FogDevice device, Task task) {
        // The decision to offload follows the HybOff algorithm logic:
        // 1. If device is a master, it should process locally unless heavily loaded
        // 2. For urgent tasks, device should process locally if possible
        // 3. For regular tasks, follow the static offloading pattern if device is moderately loaded
        
        double currentLoad = device.getUtilizationRatio();
        
        // Masters try to handle tasks locally unless heavily loaded
        if (device.isMaster() && currentLoad < HIGH_LOAD_THRESHOLD) {
            return false;
        }
        
        // Urgent tasks are processed locally if possible
        if (task.isUrgent() && device.hasResourcesToProcess(task)) {
            return false;
        }
        
        // Static offloading decision for regular tasks
        if (!task.isUrgent()) {
            if (currentLoad > MEDIUM_LOAD_THRESHOLD) {
                return true; // Offload if moderately loaded
            }
        }
        
        // Offload if device cannot handle the task
        return !device.hasResourcesToProcess(task);
    }
    
    @Override
    public void updatePolicy(List<FogDevice> fogDevices) {
        // Group devices by cell ID
        Map<Integer, List<FogDevice>> devicesByCell = new HashMap<>();
        
        for (FogDevice device : fogDevices) {
            int cellId = device.getCellId();
            if (!devicesByCell.containsKey(cellId)) {
                devicesByCell.put(cellId, new ArrayList<>());
            }
            devicesByCell.get(cellId).add(device);
        }
        
        // Clear previous static tables
        staticOffloadingTables.clear();
        
        // Create static offloading table for each cell
        for (Map.Entry<Integer, List<FogDevice>> entry : devicesByCell.entrySet()) {
            int cellId = entry.getKey();
            List<FogDevice> cellDevices = entry.getValue();
            
            // Create static offloading table for this cell
            Map<String, String> cellTable = createStaticOffloadingTable(cellDevices);
            staticOffloadingTables.put(cellId, cellTable);
        }
    }
    
    /**
     * Select a target using the static offloading table for the cell
     */
    private FogDevice selectStatically(FogDevice source, List<FogDevice> cellMembers) {
        int cellId = source.getCellId();
        Map<String, String> cellTable = staticOffloadingTables.get(cellId);
        
        if (cellTable == null || !cellTable.containsKey(source.getId())) {
            return null;
        }
        
        String targetId = cellTable.get(source.getId());
        
        // Find the target device by ID
        for (FogDevice device : cellMembers) {
            if (device.getId().equals(targetId)) {
                return device;
            }
        }
        
        return null;
    }
    
    /**
     * Select a target dynamically based on current load and distance
     */
    private FogDevice selectDynamically(FogDevice source, Task task, List<FogDevice> cellMembers) {
        List<FogDevice> candidates = new ArrayList<>();
        
        // Filter out devices that cannot handle the task
        for (FogDevice device : cellMembers) {
            if (device != source && device.hasResourcesToProcess(task)) {
                candidates.add(device);
            }
        }
        
        if (candidates.isEmpty()) {
            return null;
        }
        
        // Calculate a score for each candidate based on load and distance
        FogDevice bestTarget = null;
        double bestScore = Double.MAX_VALUE;
        
        for (FogDevice candidate : candidates) {
            // Lower score is better
            double loadFactor = candidate.getUtilizationRatio();
            double distanceFactor = calculateDistance(source, candidate) / 1000.0; // Normalize distance
            
            // Give more weight to load for urgent tasks, more weight to distance for regular tasks
            double weightLoad = task.isUrgent() ? 0.3 : 0.7;
            double weightDistance = 1.0 - weightLoad;
            
            double score = (weightLoad * loadFactor) + (weightDistance * distanceFactor);
            
            if (score < bestScore) {
                bestScore = score;
                bestTarget = candidate;
            }
        }
        
        return bestTarget;
    }
    
    /**
     * Get all devices that belong to the same cell as the source device
     */
    private List<FogDevice> getCellMembers(FogDevice source, List<FogDevice> allDevices) {
        List<FogDevice> cellMembers = new ArrayList<>();
        int cellId = source.getCellId();
        
        for (FogDevice device : allDevices) {
            if (device.getCellId() == cellId) {
                cellMembers.add(device);
            }
        }
        
        return cellMembers;
    }
    
    /**
     * Create a static offloading table for a cell based on resource capacity
     */
    private Map<String, String> createStaticOffloadingTable(List<FogDevice> cellDevices) {
        if (cellDevices.size() <= 1) {
            return new HashMap<>(); // No offloading possible in a cell with only one device
        }
        
        Map<String, String> offloadingTable = new HashMap<>();
        
        // Find the master device
        FogDevice master = null;
        for (FogDevice device : cellDevices) {
            if (device.isMaster()) {
                master = device;
                break;
            }
        }
        
        if (master == null) {
            // If no master is found, just create a circular allocation
            for (int i = 0; i < cellDevices.size(); i++) {
                FogDevice source = cellDevices.get(i);
                FogDevice target = cellDevices.get((i + 1) % cellDevices.size());
                offloadingTable.put(source.getId(), target.getId());
            }
        } else {
            // If master exists, distribute load based on processing capacity
            List<FogDevice> sortedDevices = new ArrayList<>(cellDevices);
            sortedDevices.remove(master);
            
            // Sort by available processing power
            Collections.sort(sortedDevices, Comparator.comparingInt(FogDevice::getTotalMips).reversed());
            
            // Master offloads to the most powerful device
            offloadingTable.put(master.getId(), sortedDevices.get(0).getId());
            
            // Create a balanced offloading pattern among followers
            for (int i = 0; i < sortedDevices.size() - 1; i++) {
                FogDevice source = sortedDevices.get(i);
                FogDevice target = sortedDevices.get(i + 1);
                offloadingTable.put(source.getId(), target.getId());
            }
            
            // Last follower offloads to master to close the loop
            if (!sortedDevices.isEmpty()) {
                offloadingTable.put(sortedDevices.get(sortedDevices.size() - 1).getId(), master.getId());
            }
        }
        
        return offloadingTable;
    }
    
    /**
     * Calculate Euclidean distance between two fog devices
     */
    private double calculateDistance(FogDevice device1, FogDevice device2) {
        return Math.sqrt(Math.pow(device2.getX() - device1.getX(), 2) + 
                         Math.pow(device2.getY() - device1.getY(), 2));
    }
}

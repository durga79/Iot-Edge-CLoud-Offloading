package org.hyboff.simulation.offloading;

import org.hyboff.simulation.model.FogDevice;
import org.hyboff.simulation.model.IoTDevice;
import org.hyboff.simulation.model.Task;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Implementation of a Dynamic Offloading Algorithm (PoA).
 * This policy makes offloading decisions at runtime based on current system state.
 * As mentioned in the HybOff paper, dynamic approaches are more adaptive but have higher overhead.
 */
public class DynamicOffloadingPolicy implements OffloadingPolicy {
    private Random random = new Random(42); // Fixed seed for reproducibility
    
    // Thresholds for load-based decisions
    private static final double HIGH_LOAD_THRESHOLD = 0.8; // 80% utilization
    private static final double LOW_LOAD_THRESHOLD = 0.3;  // 30% utilization
    
    @Override
    public FogDevice selectOffloadingTarget(FogDevice sourceDevice, Task task, List<FogDevice> availableDevices) {
        List<FogDevice> potentialTargets = new ArrayList<>();
        
        // Filter devices with enough resources and low load
        for (FogDevice device : availableDevices) {
            if (device != sourceDevice && 
                device.hasResourcesToProcess(task) && 
                device.getUtilizationRatio() < HIGH_LOAD_THRESHOLD) {
                potentialTargets.add(device);
            }
        }
        
        if (potentialTargets.isEmpty()) {
            return null; // No suitable target found
        }
        
        if (task.isUrgent()) {
            // For urgent tasks, select the device with lowest latency (closest)
            return findClosestDevice(sourceDevice, potentialTargets);
        } else {
            // For regular tasks, select the device with lowest utilization
            return findLeastLoadedDevice(potentialTargets);
        }
    }
    
    @Override
    public boolean shouldOffload(FogDevice device, Task task) {
        // We should offload if:
        // 1. Device is highly loaded
        // 2. OR device doesn't have enough resources for the task
        // 3. OR task is not urgent and device is moderately loaded
        
        double currentLoad = device.getUtilizationRatio();
        
        if (currentLoad > HIGH_LOAD_THRESHOLD) {
            return true; // Device is highly loaded
        }
        
        if (!device.hasResourcesToProcess(task)) {
            return true; // Not enough resources
        }
        
        if (!task.isUrgent() && currentLoad > LOW_LOAD_THRESHOLD) {
            return random.nextDouble() < 0.7; // 70% chance to offload
        }
        
        return false;
    }
    
    @Override
    public void updatePolicy(List<FogDevice> fogDevices) {
        // Dynamic policy doesn't need pre-computation like static policy
        // It makes decisions based on real-time state
    }
    
    private FogDevice findLeastLoadedDevice(List<FogDevice> devices) {
        if (devices.isEmpty()) {
            return null;
        }
        
        return devices.stream()
            .min(Comparator.comparingDouble(FogDevice::getUtilizationRatio))
            .orElse(devices.get(0));
    }
    
    private FogDevice findClosestDevice(FogDevice source, List<FogDevice> targets) {
        if (targets.isEmpty()) {
            return null;
        }
        
        FogDevice closest = targets.get(0);
        double minDistance = calculateDistance(source, closest);
        
        for (FogDevice target : targets) {
            double distance = calculateDistance(source, target);
            if (distance < minDistance) {
                minDistance = distance;
                closest = target;
            }
        }
        
        return closest;
    }
    
    private double calculateDistance(FogDevice device1, FogDevice device2) {
        return Math.sqrt(Math.pow(device2.getX() - device1.getX(), 2) + 
                         Math.pow(device2.getY() - device1.getY(), 2));
    }
}

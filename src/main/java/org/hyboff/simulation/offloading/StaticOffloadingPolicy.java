package org.hyboff.simulation.offloading;

import org.hyboff.simulation.model.FogDevice;
import org.hyboff.simulation.model.IoTDevice;
import org.hyboff.simulation.model.Task;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Implementation of a Static Offloading Algorithm (SoA).
 * This policy makes offloading decisions based on a static allocation table.
 * As mentioned in the HybOff paper, SoA approaches are simple but inefficient in heterogeneous networks.
 */
public class StaticOffloadingPolicy implements OffloadingPolicy {
    // Static mapping of device to target based on device IDs
    private Map<String, String> staticAllocationTable;
    
    public StaticOffloadingPolicy() {
        staticAllocationTable = new HashMap<>();
    }
    
    @Override
    public FogDevice selectOffloadingTarget(FogDevice sourceDevice, Task task, List<FogDevice> availableDevices) {
        // In static offloading, we look for the pre-determined target
        String targetId = staticAllocationTable.get(sourceDevice.getId());
        
        if (targetId != null) {
            // Find the target device by ID
            for (FogDevice device : availableDevices) {
                if (device.getId().equals(targetId)) {
                    return device;
                }
            }
        }
        
        // If no static mapping exists or target not found, use least loaded device
        return findLeastLoadedDevice(availableDevices);
    }
    
    @Override
    public boolean shouldOffload(FogDevice device, Task task) {
        // In static offloading, we offload if the device is overloaded
        double utilizationThreshold = 0.8; // 80% utilization threshold
        return device.getUtilizationRatio() > utilizationThreshold;
    }
    
    @Override
    public void updatePolicy(List<FogDevice> fogDevices) {
        // Clear the existing allocation table
        staticAllocationTable.clear();
        
        // Group devices by cell ID
        Map<Integer, List<FogDevice>> devicesByCell = new HashMap<>();
        
        for (FogDevice device : fogDevices) {
            int cellId = device.getCellId();
            if (!devicesByCell.containsKey(cellId)) {
                devicesByCell.put(cellId, new java.util.ArrayList<>());
            }
            devicesByCell.get(cellId).add(device);
        }
        
        // For each cell, create a static allocation pattern
        for (List<FogDevice> cellDevices : devicesByCell.values()) {
            if (cellDevices.size() <= 1) {
                continue;  // Skip cells with only one device
            }
            
            // Sort devices by processing capacity (MIPS)
            Collections.sort(cellDevices, Comparator.comparingInt(FogDevice::getTotalMips).reversed());
            
            // Create a circular allocation pattern within the cell
            for (int i = 0; i < cellDevices.size(); i++) {
                FogDevice source = cellDevices.get(i);
                FogDevice target = cellDevices.get((i + 1) % cellDevices.size());
                staticAllocationTable.put(source.getId(), target.getId());
            }
        }
    }
    
    private FogDevice findLeastLoadedDevice(List<FogDevice> devices) {
        if (devices.isEmpty()) {
            return null;
        }
        
        FogDevice leastLoaded = devices.get(0);
        double minUtilization = leastLoaded.getUtilizationRatio();
        
        for (FogDevice device : devices) {
            if (device.getUtilizationRatio() < minUtilization) {
                minUtilization = device.getUtilizationRatio();
                leastLoaded = device;
            }
        }
        
        return leastLoaded;
    }
}

package org.hyboff.simulation.offloading;

import org.hyboff.simulation.model.FogDevice;
import org.hyboff.simulation.model.IoTDevice;
import org.hyboff.simulation.model.Task;
import java.util.List;

/**
 * Interface defining the offloading policy contract.
 * Different offloading strategies will implement this interface.
 */
public interface OffloadingPolicy {
    
    /**
     * Select a target fog device for task offloading
     * 
     * @param sourceDevice The source fog device where the task originates
     * @param task The task to be offloaded
     * @param availableDevices List of potential target devices
     * @return The selected target device, or null if no suitable target found
     */
    FogDevice selectOffloadingTarget(FogDevice sourceDevice, Task task, List<FogDevice> availableDevices);
    
    /**
     * Decide whether a task should be offloaded
     * 
     * @param device The current fog device
     * @param task The task to potentially offload
     * @return true if the task should be offloaded, false otherwise
     */
    boolean shouldOffload(FogDevice device, Task task);
    
    /**
     * Update the offloading policy based on the current state of fog devices
     * 
     * @param fogDevices List of all fog devices in the system
     */
    void updatePolicy(List<FogDevice> fogDevices);
}

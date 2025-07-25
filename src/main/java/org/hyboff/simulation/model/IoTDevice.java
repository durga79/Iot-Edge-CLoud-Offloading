package org.hyboff.simulation.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an IoT Device in the simulation.
 * These devices generate tasks that need to be processed by Fog devices.
 */
public class IoTDevice {
    private String id;
    private double x;
    private double y;
    private FogDevice connectedFogDevice;
    private List<Task> generatedTasks;
    
    public IoTDevice(String id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.generatedTasks = new ArrayList<>();
    }
    
    /**
     * Generate a task and send it to the connected fog device
     */
    public void generateTask(Task task) {
        // Set the source ID to this IoT device's ID
        task.setSourceId(this.id);
        generatedTasks.add(task);
    }
    
    /**
     * Send a task to the connected fog device
     */
    public boolean sendTaskToFog(Task task) {
        if (connectedFogDevice != null) {
            return connectedFogDevice.receiveTask(task);
        }
        return false;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public FogDevice getConnectedFogDevice() {
        return connectedFogDevice;
    }
    
    public void setConnectedFogDevice(FogDevice connectedFogDevice) {
        this.connectedFogDevice = connectedFogDevice;
    }
    
    public List<Task> getGeneratedTasks() {
        return generatedTasks;
    }
}

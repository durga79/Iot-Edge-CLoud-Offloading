package org.hyboff.simulation.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import org.hyboff.simulation.util.Log;

/**
 * Represents a Fog Device (Edge server) in the simulation.
 * Based on the HybOff paper's fog server specifications.
 * This is the core component of the fog computing simulation.
 */
public class FogDevice {
    private String id;
    private double x;
    private double y;
    private int totalMips;
    private int ram;
    private int storage;
    private int bandwidth;
    private int availableMips;
    private int availableRam;
    private int availableStorage;
    private double availableBandwidth;
    private int cellId;
    private boolean isMaster;
    private List<FogDevice> cellMembers;
    private int completedTasks;
    private int totalReceivedTasks;
    private double totalResponseTime;
    private int maxQueueSize;
    private boolean logEnabled;
    
    // HybOff modules
    private HybOffMonitor monitor;
    private HybOffCommunicator communicator;
    private HybOffScheduler scheduler;
    
    /**
     * Constructor for FogDevice
     * 
     * @param id Device identifier
     * @param totalMips Processing capacity in Million Instructions Per Second
     * @param ram Memory in MB
     * @param storage Storage capacity in MB
     * @param bandwidth Network bandwidth in Mbps
     * @param x X-coordinate for spatial positioning
     * @param y Y-coordinate for spatial positioning
     */
    public FogDevice(String id, int totalMips, int ram, int storage, int bandwidth, double x, double y) {
        this.id = id;
        this.totalMips = totalMips;
        this.ram = ram;
        this.storage = storage;
        this.bandwidth = bandwidth;
        this.x = x;
        this.y = y;
        this.availableMips = totalMips;
        this.availableRam = ram;
        this.availableStorage = storage;
        this.availableBandwidth = bandwidth;
        this.cellId = -1;
        this.isMaster = false;
        this.cellMembers = new ArrayList<>();
        this.completedTasks = 0;
        this.totalReceivedTasks = 0;
        this.totalResponseTime = 0;
        this.maxQueueSize = 100; // Default queue size
        this.logEnabled = false; // Logging disabled by default
        
        // Initialize HybOff modules
        this.monitor = new HybOffMonitor(this);
        this.communicator = new HybOffCommunicator(this);
        this.scheduler = new HybOffScheduler(this);
    }
    
    /**
     * Reset device state for a new simulation run
     */
    public void reset() {
        this.availableMips = totalMips;
        this.availableRam = ram;
        this.availableStorage = storage;
        this.availableBandwidth = bandwidth;
        this.completedTasks = 0;
        this.totalReceivedTasks = 0;
        this.totalResponseTime = 0;
        this.scheduler.reset();
    }
    
    /**
     * Set this fog device as a master node in its cell
     */
    public void setAsMaster() {
        this.isMaster = true;
    }
    
    /**
     * Set this fog device as a regular member (not master)
     */
    public void setAsMember() {
        this.isMaster = false;
    }
    
    /**
     * Set the cell ID this fog device belongs to
     */
    public void setCellId(int cellId) {
        this.cellId = cellId;
    }
    
    /**
     * Add another fog device as a member of this device's cell
     */
    public void addCellMember(FogDevice device) {
        if (!cellMembers.contains(device)) {
            cellMembers.add(device);
        }
    }
    
    /**
     * Enable or disable logging for this device
     */
    public void setLogEnabled(boolean enabled) {
        this.logEnabled = enabled;
    }
    
    /**
     * Receive a task from an IoT device or another fog device
     * Returns true if the task was accepted, false otherwise
     */
    public boolean receiveTask(Task task) {
        return scheduler.addTask(task);
    }
    
    /**
     * Process a task on this fog device
     */
    public void processTask(Task task) {
        // This method is now delegated to the scheduler
        if (logEnabled) {
            Log.printLine(id + " processing task " + task.getId());
        }
    }
    
    /**
     * Update task execution status
     */
    public void updateTaskExecution() {
        scheduler.updateTaskExecution();
    }
    
    /**
     * Check if this device has resources to process a given task
     */
    public boolean hasResourcesToProcess(Task task) {
        return availableMips >= task.getSize();
    }
    
    /**
     * Allocate resources for a task
     */
    public void allocateResource(int mips) {
        this.availableMips -= mips;
        if (availableMips < 0) {
            availableMips = 0;
        }
    }
    
    /**
     * Release resources after task completion
     */
    public void releaseResource(int mips) {
        this.availableMips += mips;
        if (availableMips > totalMips) {
            availableMips = totalMips;
        }
    }
    
    /**
     * Calculate the current utilization ratio (used / total)
     */
    public double getUtilizationRatio() {
        return (totalMips - availableMips) / (double) totalMips;
    }
    
    /**
     * Update task completion count and response time
     */
    public void taskCompleted(Task task) {
        completedTasks++;
        totalResponseTime += task.getResponseTime();
    }
    
    /**
     * Complete all tasks in the scheduler for accurate metrics
     * This is used at the end of simulation to ensure all tasks are counted
     */
    public void completeAllTasks() {
        // Force completion of any remaining tasks in the scheduler
        List<Task> remainingTasks = scheduler.getAllTasks();
        for (Task task : remainingTasks) {
            if (!task.isCompleted()) {
                task.complete();
                taskCompleted(task);
            }
        }
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
    
    public int getTotalMips() {
        return totalMips;
    }
    
    public int getAvailableMips() {
        return availableMips;
    }
    
    public int getRam() {
        return ram;
    }
    
    public int getAvailableRam() {
        return availableRam;
    }
    
    public int getStorage() {
        return storage;
    }
    
    public int getAvailableStorage() {
        return availableStorage;
    }
    
    public int getBandwidth() {
        return bandwidth;
    }
    
    public double getAvailableBandwidth() {
        return availableBandwidth;
    }
    
    public double getUtilizedMips() {
        return totalMips - availableMips;
    }
    
    public int getCellId() {
        return cellId;
    }
    
    public boolean isMaster() {
        return isMaster;
    }
    
    public List<FogDevice> getCellMembers() {
        return cellMembers;
    }
    
    public HybOffMonitor getMonitor() {
        return monitor;
    }
    
    public HybOffCommunicator getCommunicator() {
        return communicator;
    }
    
    public HybOffScheduler getScheduler() {
        return scheduler;
    }
    
    public int getCompletedTasks() {
        return completedTasks;
    }
    
    public int getTotalReceivedTasks() {
        return totalReceivedTasks;
    }
    
    public double getTotalResponseTime() {
        return totalResponseTime;
    }
    
    public void incrementTotalReceivedTasks() {
        this.totalReceivedTasks++;
    }
    
    public int getMaxQueueSize() {
        return maxQueueSize;
    }
    
    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }
    
    public boolean isLogEnabled() {
        return logEnabled;
    }
}

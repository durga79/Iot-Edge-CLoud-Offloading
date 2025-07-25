package org.hyboff.simulation.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import org.hyboff.simulation.util.Log;
import org.hyboff.simulation.security.SecurityManager;
import org.hyboff.simulation.network.WirelessNetworkModel;
import org.hyboff.simulation.energy.EnergyModel;
import org.hyboff.simulation.analytics.DataAnalytics;

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
    
    // Energy model
    private EnergyModel energyModel;
    
    // Network model
    private WirelessNetworkModel networkModel;
    
    // Security features
    private SecurityManager securityManager;
    private boolean securityEnabled;
    
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
        
        // Initialize energy model (default values for fog device)
        double batteryCapacity = 10000.0;  // 10,000 Joules battery capacity
        boolean batteryEnabled = true;     // Enable battery constraints
        this.energyModel = new EnergyModel(id, batteryCapacity, batteryEnabled);
        
        // Set power consumption for different states
        energyModel.setPowerConsumption(EnergyModel.State.IDLE, 10.0);       // 10 watts when idle
        energyModel.setPowerConsumption(EnergyModel.State.PROCESSING, 50.0); // 50 watts when processing
        energyModel.setPowerConsumption(EnergyModel.State.TRANSMITTING, 5.0); // 5 watts when transmitting
        energyModel.setPowerConsumption(EnergyModel.State.RECEIVING, 2.0);   // 2 watts when receiving
        
        // Initialize network model (default to WiFi)
        this.networkModel = new WirelessNetworkModel(WirelessNetworkModel.Protocol.WIFI);
        
        // Initialize security (default to basic security)
        this.securityManager = new SecurityManager(SecurityManager.AuthType.BASIC, 
                                                 SecurityManager.SecurityLevel.LOW);
        this.securityEnabled = true;
        this.securityManager.authenticateDevice(this.id); // Self-authenticate
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
        // Disabled verbose debug logging
        // System.out.println("DEBUG: " + id + " receiving task " + task.getId() + " of size " + task.getSize() + " MIPS");
        // System.out.println("DEBUG: " + id + " has " + availableMips + " available MIPS before processing");
        
        // Check if this is a security-enabled device
        if (securityEnabled) {
            // Authenticate the source device
            boolean authenticated = securityManager.authenticate(task.getSourceId(), task.getSourceId());
            if (!authenticated) {
                // Disabled verbose debug logging
                // System.out.println("DEBUG: " + id + " authentication failed for task " + task.getId());
                if (logEnabled) {
                    Log.printLine(id + ": Authentication failed for task from " + task.getSourceId());
                }
                return false;
            }
            
            // Add security overhead (time delay) based on security level
            double securityOverhead = securityManager.calculateSecurityOverhead(task.getSize());
            task.addResponseTime(securityOverhead);
        }
        
        // Simulate network reception
        if (networkModel != null) {
            // Assume a default distance if not specified
            double distance = 50.0; // meters
            
            // Simulate packet transmission
            WirelessNetworkModel.TransmissionResult result = 
                networkModel.simulateTransmission(task.getSize(), distance);
            
            if (!result.isSuccess()) {
                // Disabled verbose debug logging
                // System.out.println("DEBUG: " + id + " network transmission failed for task " + task.getId() + 
                //               " - Reason: " + result.getMessage());
                if (logEnabled) {
                    Log.printLine(id + ": Network transmission failed for task " + task.getId());
                }
                return false;
            }
            
            // Add network latency to response time
            task.addResponseTime(result.getLatency());
            // Disabled verbose network logging
            // System.out.println("NETWORK: Task " + task.getId() + " transmission successful with latency " + 
            //                  String.format("%.2f", result.getLatency()) + " ms");
            
            // Consume energy for receiving data
            energyModel.consumeEnergy(EnergyModel.State.RECEIVING, result.getEnergyConsumed());
        }
        
        // Set this device as the source fog for the task
        task.setSourceFogId(id);
        
        // Increment received task counter
        totalReceivedTasks++;
        
        // Try to add the task to the scheduler
        boolean taskAccepted = scheduler.addTask(task);
        // Disabled verbose debug logging
        // if (!taskAccepted) {
        //     System.out.println("DEBUG: " + id + " scheduler rejected task " + task.getId());
        // } else {
        //     System.out.println("DEBUG: " + id + " accepted task " + task.getId());
        // }
        return taskAccepted;
    }
    
    /**
     * Process tasks in scheduler
     */
    public void update() {
        // Process tasks in scheduler
        scheduler.updateTaskExecution();
        
        // Update monitor
        monitor.update();
        
        // Update energy consumption
        // Calculate utilization ratio (0-1)
        double utilizationRatio = monitor.getCpuUtilization();
        
        // Consume energy based on utilization
        double timeStep = 1.0; // 1 second per update
        if (utilizationRatio > 0) {
            energyModel.consumeEnergy(EnergyModel.State.PROCESSING, timeStep * utilizationRatio);
        } else {
            energyModel.consumeEnergy(EnergyModel.State.IDLE, timeStep);
        }
    }
    
    /**
     * Check if this device has resources to process a given task
     */
    public boolean hasResourcesToProcess(Task task) {
        boolean hasResources = availableMips >= task.getSize();
        // Disabled verbose debug logging
        // if (!hasResources) {
        //     System.out.println("DEBUG: " + id + " cannot process task " + task.getId() + 
        //                       ". Available MIPS: " + availableMips + ", Task size: " + task.getSize());
        // }
        return hasResources;
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
     * Complete all remaining tasks (used at simulation end)
     */
    public void completeAllTasks() {
        int completedCount = 0;
        
        // Get all tasks from scheduler
        List<Task> allTasks = scheduler.getAllTasks();
        
        // Complete all tasks that are not yet completed
        for (Task task : allTasks) {
            if (!task.isCompleted()) {
                task.complete();
                scheduler.addToCompletedTasks(task); // Add to completed tasks list
                completedCount++;
            }
        }
        
        if (completedCount > 0) {
            System.out.println(id + ": Completed " + completedCount + " remaining tasks");
        }
    }
    
    /**
     * Process a task directly (used by offloading controller)
     * @param task Task to process
     * @return True if task was accepted and processed
     */
    public boolean processTask(Task task) {
        // First receive the task
        if (!receiveTask(task)) {
            return false;
        }
        
        // Update to process the task immediately
        update();
        
        return true;
    }
    
    /**
     * Get the energy model for this device
     */
    public EnergyModel getEnergyModel() {
        return energyModel;
    }
    
    /**
     * Set a custom energy model for this device
     */
    public void setEnergyModel(EnergyModel energyModel) {
        this.energyModel = energyModel;
    }
    
    /**
     * Get the network model for this device
     */
    public WirelessNetworkModel getNetworkModel() {
        return networkModel;
    }
    
    /**
     * Set a custom network model for this device
     * @param protocol Wireless protocol to use
     */
    public void setNetworkModel(WirelessNetworkModel.Protocol protocol) {
        this.networkModel = new WirelessNetworkModel(protocol);
    }
    
    /**
     * Get the security manager for this device
     */
    public SecurityManager getSecurityManager() {
        return securityManager;
    }
    
    /**
     * Set security settings for this device
     * @param authType Authentication type
     * @param securityLevel Security level
     */
    public void setSecuritySettings(SecurityManager.AuthType authType, 
                                  SecurityManager.SecurityLevel securityLevel) {
        this.securityManager = new SecurityManager(authType, securityLevel);
        this.securityManager.authenticateDevice(this.id);
    }
    
    /**
     * Enable or disable security features
     */
    public void setSecurityEnabled(boolean enabled) {
        this.securityEnabled = enabled;
    }
    
    /**
     * Check if security is enabled
     */
    public boolean isSecurityEnabled() {
        return securityEnabled;
    }
    
    /**
     * Get energy statistics for this device
     */
    public String getEnergyStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("Energy statistics for device " + id + ":\n");
        sb.append("  Total energy consumed: " + String.format("%.2f", energyModel.getTotalEnergyConsumed()) + " J\n");
        
        // If device has battery, show battery level
        double batteryPercentage = energyModel.getBatteryPercentage();
        if (batteryPercentage >= 0) {
            sb.append("  Battery remaining: " + String.format("%.2f", batteryPercentage) + "%\n");
        }
        
        // Calculate energy efficiency (tasks per joule)
        int completedTasks = scheduler.getCompletedTasks().size();
        double energyEfficiency = energyModel.getEnergyEfficiency(completedTasks);
        sb.append("  Energy efficiency: " + String.format("%.4f", energyEfficiency) + " tasks/J\n");
        
        return sb.toString();
    }
    
    /**
     * Get network statistics for this device
     */
    public String getNetworkStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("Network statistics for device " + id + ":\n");
        sb.append("  Protocol: " + networkModel.getProtocol() + "\n");
        sb.append("  Max range: " + String.format("%.2f", networkModel.getBaseRange()) + " m\n");
        
        // Calculate effective bandwidth at different distances
        sb.append("  Bandwidth at 10m: " + String.format("%.2f", networkModel.calculateBandwidth(10)) + " Kbps\n");
        sb.append("  Bandwidth at 100m: " + String.format("%.2f", networkModel.calculateBandwidth(100)) + " Kbps\n");
        
        return sb.toString();
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
    
    /**
     * Get total MIPS capacity
     */
    public int getTotalMips() {
        return totalMips;
    }
    
    /**
     * Get used MIPS (total - available)
     */
    public int getUsedMips() {
        return totalMips - availableMips;
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

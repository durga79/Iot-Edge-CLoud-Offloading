package org.hyboff.simulation;

import org.hyboff.simulation.util.Log;
import org.hyboff.simulation.util.SimulationClock;
import org.hyboff.simulation.model.FogDevice;
import org.hyboff.simulation.model.IoTDevice;
import org.hyboff.simulation.model.Task;
import org.hyboff.simulation.offloading.HybOffController;
import org.hyboff.simulation.offloading.OffloadingPolicy;
import org.hyboff.simulation.offloading.StaticOffloadingPolicy;
import org.hyboff.simulation.offloading.DynamicOffloadingPolicy;
import org.hyboff.simulation.offloading.HybridOffloadingPolicy;
import org.hyboff.simulation.clustering.KMeansClustering;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * Main simulation class for the HybOff algorithm implementation
 * Based on "HybOff: a Hybrid Offloading approach to improve load balancing in fog environments"
 */
public class HybOffSimulation {
    private static List<FogDevice> fogDevices;
    private static List<IoTDevice> iotDevices;
    private static final int NUM_FOG_DEVICES = 19;
    private static final int NUM_IOT_DEVICES = 50;
    private static final int NUM_CELLS = 4;
    private static final int SIMULATION_TIME = 300; // in seconds
    
    public static void main(String[] args) {
        Log.printLine("Starting HybOff Simulation...");
        
        try {
            // Initialize SimulationClock
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;
            SimulationClock.init(num_user, calendar, trace_flag);
            
            // Create fog devices and IoT devices
            createFogDevices();
            createIoTDevices();
            
            // Run simulation with different offloading policies
            runSimulation(new StaticOffloadingPolicy());
            runSimulation(new DynamicOffloadingPolicy());
            runSimulation(new HybridOffloadingPolicy());
            
            Log.printLine("Simulation completed!");
            
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Simulation terminated due to an error.");
        }
    }
    
    private static void createFogDevices() {
        fogDevices = new ArrayList<>();
        Random random = new Random(42); // Fixed seed for reproducibility
        
        for (int i = 0; i < NUM_FOG_DEVICES; i++) {
            // CPU capacity in MIPS (Million Instructions Per Second)
            int mips = 1000 + random.nextInt(1000);
            
            // RAM in MB
            int ram = 512 + random.nextInt(1536);
            
            // Storage in MB
            int storage = 10000 + random.nextInt(20000);
            
            // Bandwidth in Mbps
            int bw = 100 + random.nextInt(900);
            
            // Create fog device with random coordinates for clustering
            double x = random.nextDouble() * 1000;
            double y = random.nextDouble() * 1000;
            
            FogDevice fogDevice = new FogDevice("fog_" + i, mips, ram, storage, bw, x, y);
            fogDevices.add(fogDevice);
        }
        
        // Apply K-means clustering to create cells
        KMeansClustering kmeans = new KMeansClustering(fogDevices, NUM_CELLS);
        kmeans.cluster();
    }
    
    private static void createIoTDevices() {
        iotDevices = new ArrayList<>();
        Random random = new Random(24); // Fixed seed for reproducibility
        
        for (int i = 0; i < NUM_IOT_DEVICES; i++) {
            // Generate random coordinates for IoT devices
            double x = random.nextDouble() * 1000;
            double y = random.nextDouble() * 1000;
            
            IoTDevice iotDevice = new IoTDevice("iot_" + i, x, y);
            
            // Connect to nearest fog device
            FogDevice nearestFog = findNearestFogDevice(x, y);
            iotDevice.setConnectedFogDevice(nearestFog);
            
            iotDevices.add(iotDevice);
        }
    }
    
    private static FogDevice findNearestFogDevice(double x, double y) {
        FogDevice nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (FogDevice fog : fogDevices) {
            double distance = calculateDistance(x, y, fog.getX(), fog.getY());
            if (distance < minDistance) {
                minDistance = distance;
                nearest = fog;
            }
        }
        
        return nearest;
    }
    
    private static double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
    
    private static void runSimulation(OffloadingPolicy policy) {
        Log.printLine("Running simulation with " + policy.getClass().getSimpleName());
        
        // Reset all devices to initial state
        for (FogDevice fog : fogDevices) {
            fog.reset();
        }
        
        // Initialize HybOff controller
        HybOffController controller = new HybOffController(fogDevices, policy);
        
        // Generate tasks from IoT devices
        generateAndProcessTasks(controller);
        
        // Print results
        printResults(policy);
    }
    
    private static void generateAndProcessTasks(HybOffController controller) {
        Random random = new Random(33); // Fixed seed for reproducibility
        
        // Simulate for SIMULATION_TIME seconds
        for (int time = 0; time < SIMULATION_TIME; time++) {
            // Every second, some IoT devices generate tasks
            for (IoTDevice iot : iotDevices) {
                // 10% chance to generate a task per second per device
                if (random.nextDouble() < 0.1) {
                    // Generate task with random size and deadline
                    int taskSize = 500 + random.nextInt(1500); // MI (Million Instructions)
                    int deadline = 5 + random.nextInt(10); // seconds
                    boolean isUrgent = random.nextDouble() < 0.2; // 20% are urgent tasks
                    
                    Task task = new Task("task_" + iot.getId() + "_" + time, taskSize, deadline, isUrgent);
                    iot.generateTask(task);
                    
                    // Process task using HybOff controller
                    controller.processTask(iot, task);
                }
            }
            
            // Update status of all fog devices
            controller.updateStatus();
        }
    }
    
    private static void printResults(OffloadingPolicy policy) {
        DecimalFormat df = new DecimalFormat("#.##");
        
        double totalResourceUtilization = calculateResourceUtilization();
        double loadBalancingMetric = calculateLoadBalancingMetric();
        double systemPerformance = calculateSystemPerformance();
        
        Log.printLine("========== Results for " + policy.getClass().getSimpleName() + " ==========");
        Log.printLine("Resource Utilization: " + df.format(totalResourceUtilization * 100) + "%");
        Log.printLine("Load Balancing Metric: " + df.format(loadBalancingMetric * 100) + "%");
        Log.printLine("System Performance: " + df.format(systemPerformance));
        Log.printLine("=======================================================");
    }
    
    private static double calculateResourceUtilization() {
        double totalUtilized = 0;
        double totalAvailable = 0;
        
        for (FogDevice fog : fogDevices) {
            totalUtilized += fog.getUtilizedMips();
            totalAvailable += fog.getTotalMips();
        }
        
        return totalUtilized / totalAvailable;
    }
    
    private static double calculateLoadBalancingMetric() {
        // Calculate the standard deviation of utilization across all fog devices
        double mean = 0;
        for (FogDevice fog : fogDevices) {
            mean += fog.getUtilizedMips() / fog.getTotalMips();
        }
        mean /= fogDevices.size();
        
        double sumSquaredDiff = 0;
        for (FogDevice fog : fogDevices) {
            double utilization = fog.getUtilizedMips() / fog.getTotalMips();
            sumSquaredDiff += Math.pow(utilization - mean, 2);
        }
        
        double stdDev = Math.sqrt(sumSquaredDiff / fogDevices.size());
        
        // Convert to a load balancing metric where higher is better (100% means perfect balance)
        return Math.max(0, 1 - stdDev);
    }
    
    private static double calculateSystemPerformance() {
        int completedTasks = 0;
        int totalTasks = 0;
        double avgResponseTime = 0;
        
        for (FogDevice fog : fogDevices) {
            completedTasks += fog.getCompletedTasks();
            totalTasks += fog.getTotalReceivedTasks();
            avgResponseTime += fog.getTotalResponseTime();
        }
        
        if (completedTasks == 0) {
            return 0;
        }
        
        double completionRate = (double) completedTasks / totalTasks;
        double normalizedResponseTime = 1.0 / (1 + avgResponseTime / completedTasks);
        
        return completionRate * normalizedResponseTime * 100;
    }
}

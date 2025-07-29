package org.hyboff.simulation;

import org.hyboff.simulation.model.*;
import org.hyboff.simulation.util.ChartGenerator;
import org.hyboff.simulation.util.Log;
import org.hyboff.simulation.util.ResultsExporter;
import org.hyboff.simulation.util.SimulationClock;
import org.hyboff.simulation.network.WirelessNetworkModel;
import org.hyboff.simulation.security.SecurityManager;
import org.hyboff.simulation.analytics.DataAnalytics;
import org.hyboff.simulation.clustering.KMeansClustering;
import org.hyboff.simulation.offloading.DynamicOffloadingPolicy;
import org.hyboff.simulation.offloading.HybOffController;
import org.hyboff.simulation.offloading.HybridOffloadingPolicy;
import org.hyboff.simulation.offloading.StaticOffloadingPolicy;
import org.hyboff.simulation.visualization.ResultsVisualizer;

import java.text.DecimalFormat;
import java.util.*;

/**
 * A simplified test simulation to verify the HybOff implementation.
 */
public class TestSimulation {

    public static void main(String[] args) {
        Log.printLine("Starting HybOff Test Simulation...");
        
        // Initialize SimulationClock
        int num_user = 1;
        Calendar calendar = Calendar.getInstance();
        boolean trace_flag = false;
        SimulationClock.init(num_user, calendar, trace_flag);
        
        // Create a small test scenario
        int fogDeviceCount = 10;
        int cellCount = 2;
        int iotDeviceCount = 20;
        
        // Create fog and IoT devices
        List<FogDevice> fogDevices = createFogDevices(fogDeviceCount, cellCount);
        List<IoTDevice> iotDevices = createIoTDevices(iotDeviceCount, fogDevices);
        
        // Print initial setup
        printNetworkSetup(fogDevices, iotDevices);
        
        // Run simulation with all three policies
        runWithPolicy("Static Offloading (SoA)", new StaticOffloadingPolicy(), fogDevices, iotDevices);
        resetDevices(fogDevices);
        
        runWithPolicy("Dynamic Offloading (PoA)", new DynamicOffloadingPolicy(), fogDevices, iotDevices);
        resetDevices(fogDevices);
        
        runWithPolicy("Hybrid Offloading (HybOff)", new HybridOffloadingPolicy(), fogDevices, iotDevices);
        
        Log.printLine("Test Simulation completed successfully!");
    }
    
    private static List<FogDevice> createFogDevices(int count, int cellCount) {
        List<FogDevice> fogDevices = new ArrayList<>();
        Random random = new Random(42); // Fixed seed for reproducibility
        
        for (int i = 0; i < count; i++) {
            // Increase MIPS capacity to handle larger tasks (min 3000, max 7000)
            int mips = 3000 + random.nextInt(4000);
            int ram = 1024 + random.nextInt(3072);
            int storage = 10000 + random.nextInt(40000);
            int bw = 100 + random.nextInt(900);
            
            double x = random.nextDouble() * 1000;
            double y = random.nextDouble() * 1000;
            
            FogDevice fogDevice = new FogDevice("fog_" + i, mips, ram, storage, bw, x, y);
            fogDevices.add(fogDevice);
        }
        
        // Apply K-means clustering
        KMeansClustering kmeans = new KMeansClustering(fogDevices, cellCount);
        kmeans.cluster();
        
        return fogDevices;
    }
    
    private static List<IoTDevice> createIoTDevices(int count, List<FogDevice> fogDevices) {
        List<IoTDevice> iotDevices = new ArrayList<>();
        Random random = new Random(24);
        
        for (int i = 0; i < count; i++) {
            double x = random.nextDouble() * 1000;
            double y = random.nextDouble() * 1000;
            
            IoTDevice iotDevice = new IoTDevice("iot_" + i, x, y);
            
            // Connect to nearest fog device
            FogDevice nearestFog = findNearestFogDevice(x, y, fogDevices);
            iotDevice.setConnectedFogDevice(nearestFog);
            
            iotDevices.add(iotDevice);
        }
        
        return iotDevices;
    }
    
    private static FogDevice findNearestFogDevice(double x, double y, List<FogDevice> fogDevices) {
        FogDevice nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (FogDevice fog : fogDevices) {
            double distance = Math.sqrt(Math.pow(fog.getX() - x, 2) + Math.pow(fog.getY() - y, 2));
            if (distance < minDistance) {
                minDistance = distance;
                nearest = fog;
            }
        }
        
        return nearest;
    }
    
    private static void printNetworkSetup(List<FogDevice> fogDevices, List<IoTDevice> iotDevices) {
        Log.printLine("\n==== Network Setup ====");
        Log.printLine("Fog Devices: " + fogDevices.size());
        
        // Count devices per cell
        Map<Integer, Integer> devicesPerCell = new HashMap<>();
        Map<Integer, FogDevice> mastersPerCell = new HashMap<>();
        
        for (FogDevice fog : fogDevices) {
            int cellId = fog.getCellId();
            devicesPerCell.put(cellId, devicesPerCell.getOrDefault(cellId, 0) + 1);
            
            if (fog.isMaster()) {
                mastersPerCell.put(cellId, fog);
            }
        }
        
        // Print cell information
        for (Map.Entry<Integer, Integer> entry : devicesPerCell.entrySet()) {
            int cellId = entry.getKey();
            int deviceCount = entry.getValue();
            FogDevice master = mastersPerCell.get(cellId);
            
            Log.printLine("Cell " + cellId + ": " + deviceCount + " fog devices, Master: " + 
                         (master != null ? master.getId() : "None"));
        }
        
        Log.printLine("IoT Devices: " + iotDevices.size());
        Log.printLine("======================\n");
    }
    
    private static void runWithPolicy(String policyName, 
                                     org.hyboff.simulation.offloading.OffloadingPolicy policy,
                                     List<FogDevice> fogDevices,
                                     List<IoTDevice> iotDevices) {
        Log.printLine("\n==== Running simulation with " + policyName + " ====");
        
        // Initialize HybOff controller
        HybOffController controller = new HybOffController(fogDevices, policy);
        
        // Simulate for 60 time steps
        Random random = new Random(33);
        for (int time = 0; time < 60; time++) {
            // Generate tasks from IoT devices
            for (IoTDevice iot : iotDevices) {
                // 20% chance to generate a task per time step
                if (random.nextDouble() < 0.2) {
                    // Generate tasks with sizes that fog devices can handle (300-2000 MIPS)
                    int taskSize = 300 + random.nextInt(1700);
                    int deadline = 5 + random.nextInt(20);
                    boolean isUrgent = random.nextDouble() < 0.2;
                    
                    Task task = new Task("task_" + iot.getId() + "_" + time, taskSize, deadline, isUrgent);
                    iot.generateTask(task);
                    
                    controller.processTask(iot, task);
                }
            }
            
            // Update fog devices
            for (FogDevice fog : fogDevices) {
                fog.update();
            }
            
            // Update controller status every 10 time steps
            if (time % 10 == 0) {
                controller.updateStatus();
            }
        }
        
        Log.printLine("\n*** Final Metrics for " + policyName + " ***\n");
        printFinalMetrics(fogDevices, policyName);
    }
    
    /**
     * Calculate load balancing metric based on task distribution
     * @param fogDevices List of fog devices
     * @return Load balancing factor (0-1)
     */
    private static double calculateLoadBalancing(List<FogDevice> fogDevices) {
        if (fogDevices.isEmpty()) {
            return 1.0; // Perfect balance if no devices
        }
        
        // Calculate utilization for each device
        double[] utilizations = new double[fogDevices.size()];
        int i = 0;
        double maxUtil = 0;
        double minUtil = Double.MAX_VALUE;
        
        for (FogDevice fog : fogDevices) {
            double util = fog.getMonitor().getCpuUtilization();
            utilizations[i++] = util;
            maxUtil = Math.max(maxUtil, util);
            minUtil = Math.min(minUtil, util);
        }
        
        // Calculate load balancing as 1 - (max-min)/max
        // This gives 1.0 for perfect balance and approaches 0 for poor balance
        return (maxUtil > 0) ? 1.0 - ((maxUtil - minUtil) / maxUtil) : 1.0;
    }
    
    private static void printFinalMetrics(List<FogDevice> fogDevices, String policyName) {
        Log.printLine("\n====== SIMULATION RESULTS: " + policyName + " ======");
        
        // Force completion of all tasks before calculating metrics
        for (FogDevice fog : fogDevices) {
            fog.completeAllTasks();
        }
        
        // Calculate resource utilization
        int totalTasks = 0;
        int completedTasks = 0;
        double totalResponseTime = 0;
        
        // Calculate resource utilization
        double totalMips = 0;
        double usedMips = 0;
        
        // Collect task data from all devices
        for (FogDevice fog : fogDevices) {
            HybOffScheduler scheduler = fog.getScheduler();
            List<Task> deviceTasks = scheduler.getAllTasks();
            List<Task> deviceCompletedTasks = scheduler.getCompletedTasks();
            
            totalTasks += deviceTasks.size();
            completedTasks += deviceCompletedTasks.size();
            totalResponseTime += scheduler.getTotalResponseTime();
            
            totalMips += fog.getTotalMips();
            usedMips += fog.getUsedMips();
        }
        
        // Print key metrics requested in the question paper
        System.out.println("===== KEY PERFORMANCE METRICS =====\n");
        
        // 1. Task Completion Rate
        double taskCompletionRate = (totalTasks > 0) ? ((double)completedTasks / totalTasks) * 100 : 0;
        System.out.println("1. Task Completion Rate: " + completedTasks + "/" + totalTasks + 
                         " (" + String.format("%.2f", taskCompletionRate) + "%)");
        
        // 2. Resource Utilization
        double resourceUtilization = (totalMips > 0) ? ((double)usedMips / totalMips) * 100 : 0;
        System.out.println("2. Resource Utilization: " + String.format("%.2f", resourceUtilization) + "%");
        
        // 3. Load Balancing
        double loadBalancing = calculateLoadBalancing(fogDevices) * 100;
        System.out.println("3. Load Balancing: " + String.format("%.2f", loadBalancing) + "%");
        
        // 4. Average Response Time
        double avgResponseTime = (completedTasks > 0) ? (totalResponseTime / completedTasks) : 0;
        System.out.println("4. Average Response Time: " + String.format("%.2f", avgResponseTime) + " ms");
        
        // 5. Energy Efficiency
        double totalEnergy = 0;
        for (FogDevice fog : fogDevices) {
            double energy = fog.getEnergyModel().getTotalEnergyConsumed();
            totalEnergy += energy;
            
            // Print individual device energy stats
            System.out.println(fog.getId() + " energy: " + String.format("%.2f", energy) + " J");
            
            // Calculate energy efficiency (tasks per joule)
            int deviceCompletedTasks = fog.getScheduler().getCompletedTasks().size();
            double energyEfficiency = energy > 0 ? deviceCompletedTasks / energy : 0;
            // Energy efficiency is calculated per task below
        }
        
        // Average energy metrics
        double avgEnergyPerDevice = totalEnergy / fogDevices.size();
        double energyPerTask = completedTasks > 0 ? totalEnergy / completedTasks : 0;
        double avgEnergyEfficiency = totalEnergy > 0 ? completedTasks / totalEnergy : 0;
        
        System.out.println("Total Energy Consumed: " + String.format("%.2f", totalEnergy) + " J");
        System.out.println("Average Energy per Device: " + String.format("%.2f", avgEnergyPerDevice) + " J");
        System.out.println("Energy per Task: " + String.format("%.2f", energyPerTask) + " J");
        System.out.println("Average Energy Efficiency: " + String.format("%.8f", avgEnergyEfficiency) + " tasks/J");
        
        // Network metrics
        System.out.println("\n====== NETWORK METRICS ======");
        int offloadedTasks = 0;
        for (FogDevice fog : fogDevices) {
            for (Task task : fog.getScheduler().getCompletedTasks()) {
                if (task.getSourceId() != null && !task.getSourceId().equals(fog.getId())) {
                    offloadedTasks++;
                }
            }
        }
        
        double offloadingRate = totalTasks > 0 ? offloadedTasks / (double) totalTasks : 0;
        System.out.println("Offloaded Tasks: " + offloadedTasks + "/" + totalTasks + 
                         " (" + String.format("%.2f", offloadingRate * 100) + "%)");
        
        // Security metrics if enabled
        boolean securityEnabled = false;
        for (FogDevice fog : fogDevices) {
            if (fog.isSecurityEnabled()) {
                securityEnabled = true;
                break;
            }
        }
        
        if (securityEnabled) {
            System.out.println("\n====== SECURITY METRICS ======");
            System.out.println("Security Enabled: Yes");
            
            // Count devices by security level
            int[] securityLevelCount = new int[4]; // NONE, LOW, MEDIUM, HIGH
            for (FogDevice fog : fogDevices) {
                if (fog.isSecurityEnabled()) {
                    SecurityManager.SecurityLevel level = fog.getSecurityManager().getSecurityLevel();
                    securityLevelCount[level.ordinal()]++;
                }
            }
            
            System.out.println("Security Level Distribution:");
            System.out.println("  NONE: " + securityLevelCount[0] + " devices");
            System.out.println("  LOW: " + securityLevelCount[1] + " devices");
            System.out.println("  MEDIUM: " + securityLevelCount[2] + " devices");
            System.out.println("  HIGH: " + securityLevelCount[3] + " devices");
            
            // Estimate security overhead
            double securityOverhead = 0.05 * avgResponseTime; // Estimate 5% overhead
            System.out.println("Estimated Security Overhead: " + String.format("%.2f", securityOverhead) + " ms/task");
        } else {
            System.out.println("\n====== SECURITY METRICS ======");
            System.out.println("Security Enabled: No");
        }
        
        // Create maps for task distribution, device utilization, and energy consumption for charts
        Map<String, Integer> taskDistribution = new HashMap<>();
        Map<String, Double> deviceUtilization = new HashMap<>();
        Map<String, Double> deviceEnergy = new HashMap<>();
        
        for (FogDevice fog : fogDevices) {
            int completedTaskCount = fog.getScheduler().getCompletedTasks().size();
            taskDistribution.put(fog.getId(), completedTaskCount);
            deviceUtilization.put(fog.getId(), fog.getUtilizationRatio() * 100);
            deviceEnergy.put(fog.getId(), fog.getEnergyModel().getTotalEnergyConsumed());
        }
        
        // Export results to CSV
        String resultsDir = ResultsExporter.exportResults(
                fogDevices,
                completedTasks,
                avgResponseTime,
                totalEnergy,
                avgResponseTime,
                taskDistribution,
                policyName);
        
        // Generate charts
        ChartGenerator.generateCharts(
                resultsDir,
                taskDistribution,
                deviceUtilization,
                deviceEnergy);
        
        Log.printLine("Results and charts saved to: " + resultsDir);
    }
    
    private static void resetDevices(List<FogDevice> fogDevices) {
        for (FogDevice fog : fogDevices) {
            fog.reset();
        }
    }
}

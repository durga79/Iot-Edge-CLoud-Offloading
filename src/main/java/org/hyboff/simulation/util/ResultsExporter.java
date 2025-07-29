package org.hyboff.simulation.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hyboff.simulation.model.FogDevice;

/**
 * Utility class to export simulation results to CSV files
 */
public class ResultsExporter {
    
    private static final String RESULTS_DIR = "simulation_results";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    
    /**
     * Exports simulation results to CSV files
     * 
     * @param fogDevices List of fog devices with their statistics
     * @param totalTasks Total number of processed tasks
     * @param avgUtilization Average resource utilization
     * @param totalEnergy Total energy consumption
     * @param avgLatency Average latency
     * @param taskDistribution Map of device IDs to number of tasks processed
     * @param simulationType Type of simulation (e.g., "iFogSim", "Custom")
     * @return The directory where results were saved
     */
    public static String exportResults(List<FogDevice> fogDevices, 
                                      int totalTasks,
                                      double avgUtilization,
                                      double totalEnergy,
                                      double avgLatency,
                                      Map<String, Integer> taskDistribution,
                                      String simulationType) {
        
        String timestamp = DATE_FORMAT.format(new Date());
        String resultDir = RESULTS_DIR + File.separator + simulationType + "_" + timestamp;
        
        // Create directory if it doesn't exist
        File dir = new File(resultDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // Export summary data
        exportSummary(resultDir, totalTasks, avgUtilization, totalEnergy, avgLatency, simulationType);
        
        // Export device-specific data
        exportDeviceData(resultDir, fogDevices);
        
        // Export task distribution data
        exportTaskDistribution(resultDir, taskDistribution);
        
        Log.printLine("Results exported to: " + dir.getAbsolutePath());
        return resultDir;
    }
    
    private static void exportSummary(String dir, int totalTasks, double avgUtilization, 
                                      double totalEnergy, double avgLatency, String simulationType) {
        try (FileWriter writer = new FileWriter(dir + File.separator + "summary.csv")) {
            // Write header
            writer.write("Simulation Type,Total Tasks,Avg Utilization (%),Total Energy,Avg Latency (ms)\n");
            
            // Write data
            writer.write(String.format("%s,%d,%.2f,%.2f,%.2f\n", 
                    simulationType, 
                    totalTasks, 
                    avgUtilization * 100, 
                    totalEnergy, 
                    avgLatency));
            
        } catch (IOException e) {
            Log.printLine("Error exporting summary data: " + e.getMessage());
        }
    }
    
    private static void exportDeviceData(String dir, List<FogDevice> fogDevices) {
        try (FileWriter writer = new FileWriter(dir + File.separator + "device_metrics.csv")) {
            // Write header
            writer.write("Device ID,Device Name,MIPS,RAM,Storage,Bandwidth,Utilization (%),Energy Consumption\n");
            
            // Write data for each device
            for (FogDevice device : fogDevices) {
                writer.write(String.format("%s,%s,%d,%d,%d,%d,%.2f,%.2f\n",
                        device.getId(),
                        device.getId(), // Use ID as name since there's no getName() method
                        device.getTotalMips(),
                        device.getRam(),
                        device.getStorage(),
                        device.getBandwidth(),
                        device.getUtilizationRatio() * 100,
                        device.getEnergyModel().getTotalEnergyConsumed()));
            }
            
        } catch (IOException e) {
            Log.printLine("Error exporting device data: " + e.getMessage());
        }
    }
    
    private static void exportTaskDistribution(String dir, Map<String, Integer> taskDistribution) {
        try (FileWriter writer = new FileWriter(dir + File.separator + "task_distribution.csv")) {
            // Write header
            writer.write("Device ID,Tasks Processed\n");
            
            // Write data
            for (Map.Entry<String, Integer> entry : taskDistribution.entrySet()) {
                writer.write(String.format("%s,%d\n", entry.getKey(), entry.getValue()));
            }
            
        } catch (IOException e) {
            Log.printLine("Error exporting task distribution data: " + e.getMessage());
        }
    }
}

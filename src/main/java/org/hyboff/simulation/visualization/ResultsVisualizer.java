package org.hyboff.simulation.visualization;

import org.hyboff.simulation.model.FogDevice;
import org.hyboff.simulation.util.Log;

import java.util.List;
import java.util.Map;
import java.text.DecimalFormat;
import java.util.HashMap;

/**
 * Class to visualize the results of the HybOff simulation.
 * Creates charts and graphs to compare the different offloading policies.
 */
public class ResultsVisualizer {
    
    // Data structures to store results for different policies
    private Map<String, Double> resourceUtilization;
    private Map<String, Double> loadBalancing;
    private Map<String, Double> systemPerformance;
    
    public ResultsVisualizer() {
        resourceUtilization = new HashMap<>();
        loadBalancing = new HashMap<>();
        systemPerformance = new HashMap<>();
    }
    
    /**
     * Add results for a specific offloading policy
     */
    public void addPolicyResults(String policyName, double utilization, double loadBalancing, double performance) {
        resourceUtilization.put(policyName, utilization);
        this.loadBalancing.put(policyName, loadBalancing);
        systemPerformance.put(policyName, performance);
    }
    
    /**
     * Create console-based visualization comparing the different metrics across policies
     */
    public void visualizeComparison() {
        DecimalFormat df = new DecimalFormat("#0.00");
        
        Log.printLine("\n============= HybOff Simulation Results =============\n");
        
        // Print resource utilization
        Log.printLine("===== Resource Utilization (%) =====");
        for (Map.Entry<String, Double> entry : resourceUtilization.entrySet()) {
            String bar = generateBar(entry.getValue() * 100, 50);
            Log.printLine(String.format("%-20s: %5s%% %s", 
                         entry.getKey(), 
                         df.format(entry.getValue() * 100), 
                         bar));
        }
        Log.printLine("");
        
        // Print load balancing
        Log.printLine("===== Load Balancing (%) =====");
        for (Map.Entry<String, Double> entry : loadBalancing.entrySet()) {
            String bar = generateBar(entry.getValue() * 100, 50);
            Log.printLine(String.format("%-20s: %5s%% %s", 
                         entry.getKey(), 
                         df.format(entry.getValue() * 100), 
                         bar));
        }
        Log.printLine("");
        
        // Print system performance
        Log.printLine("===== System Performance Score =====");
        for (Map.Entry<String, Double> entry : systemPerformance.entrySet()) {
            String bar = generateBar(entry.getValue(), 30);
            Log.printLine(String.format("%-20s: %5s %s", 
                         entry.getKey(), 
                         df.format(entry.getValue()), 
                         bar));
        }
        
        Log.printLine("\n============================================");
    }
    
    /**
     * Generate a text-based bar representation
     */
    private String generateBar(double value, int maxLength) {
        int barLength = (int) (value * maxLength / 100);
        if (value > 0 && barLength == 0) barLength = 1;
        
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            bar.append("#");
        }
        for (int i = barLength; i < maxLength; i++) {
            bar.append(" ");
        }
        bar.append("]");
        
        return bar.toString();
    }
    
    /**
     * Visualize the network topology with cells and offloading patterns
     */
    public void visualizeTopology(List<FogDevice> fogDevices) {
        Log.printLine("\n============= HybOff Network Topology =============\n");
        
        // Find min/max coordinates for scaling
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        
        for (FogDevice device : fogDevices) {
            minX = Math.min(minX, device.getX());
            maxX = Math.max(maxX, device.getX());
            minY = Math.min(minY, device.getY());
            maxY = Math.max(maxY, device.getY());
        }
        
        // Group by cell
        Map<Integer, List<FogDevice>> devicesByCell = new HashMap<>();
        
        for (FogDevice device : fogDevices) {
            int cellId = device.getCellId();
            if (!devicesByCell.containsKey(cellId)) {
                devicesByCell.put(cellId, new java.util.ArrayList<>());
            }
            devicesByCell.get(cellId).add(device);
        }
        
        // Print summary
        Log.printLine("Network contains " + fogDevices.size() + " fog devices in " + 
                     devicesByCell.size() + " cells.");
        
        // Print cell details
        for (Map.Entry<Integer, List<FogDevice>> entry : devicesByCell.entrySet()) {
            int cellId = entry.getKey();
            List<FogDevice> cellDevices = entry.getValue();
            
            Log.printLine("\nCell " + cellId + " ("+cellDevices.size()+" devices):");
            
            for (FogDevice device : cellDevices) {
                String deviceType = device.isMaster() ? "[MASTER]" : "[Member]";
                Log.printLine(String.format("  %s %-10s at (%.1f, %.1f), MIPS: %d, Utilization: %.2f%%", 
                             deviceType,
                             device.getId(),
                             device.getX(),
                             device.getY(),
                             device.getTotalMips(),
                             device.getUtilizationRatio() * 100));
            }
        }
        
        Log.printLine("\nBounding Box: (" + minX + "," + minY + ") to (" + 
                     maxX + "," + maxY + ")");
        Log.printLine("\n===================================================\n");
    }
    
    
}

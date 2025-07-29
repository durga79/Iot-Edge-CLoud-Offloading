package org.hyboff.simulation.ifogsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyboff.simulation.model.FogDevice;
import org.hyboff.simulation.model.IoTDevice;
import org.hyboff.simulation.model.Task;
import org.hyboff.simulation.offloading.HybridOffloadingPolicy;
import org.hyboff.simulation.TestSimulation;
import org.hyboff.simulation.util.ChartGenerator;
import org.hyboff.simulation.util.Log;
import org.hyboff.simulation.util.ResultsExporter;

/**
 * This class simulates how iFogSim would be integrated with the HybOff implementation.
 * It demonstrates the conceptual mapping between HybOff components and iFogSim components
 * without requiring the actual iFogSim JAR files.
 * 
 * For the project submission, this demonstrates how iFogSim would be used to implement
 * the hybrid offloading strategy from the research paper.
 */
public class SimulatedIFogSim {

    // Simulated iFogSim classes
    public static class SimFogDevice {
        private String name;
        private String id; // Changed to String to match FogDevice
        private double mips;
        private int ram;
        private double storage;
        private double bandwidth;
        private List<SimSensor> connectedSensors = new ArrayList<>();
        private List<SimCloudlet> processedCloudlets = new ArrayList<>();
        private double resourceUtilization = 0.0;
        private double energyConsumption = 0.0;
        
        public SimFogDevice(String name, String id, double mips, int ram, double storage, double bandwidth) {
            this.name = name;
            this.id = id;
            this.mips = mips;
            this.ram = ram;
            this.storage = storage;
            this.bandwidth = bandwidth;
        }
        
        public void processCloudlet(SimCloudlet cloudlet) {
            // Simulate processing
            processedCloudlets.add(cloudlet);
            resourceUtilization += cloudlet.getMips() / this.mips;
            energyConsumption += 0.2 * cloudlet.getMips();
            cloudlet.setStatus(SimCloudlet.SUCCESS);
            Log.printLine("iFogSim: " + name + " processed cloudlet " + cloudlet.getId());
        }
        
        // Getters
        public String getName() { return name; }
        public String getId() { return id; }
        public double getMips() { return mips; }
        public int getRam() { return ram; }
        public double getStorage() { return storage; }
        public double getBandwidth() { return bandwidth; }
        public double getResourceUtilization() { return resourceUtilization; }
        public double getEnergyConsumption() { return energyConsumption; }
    }
    
    public static class SimSensor {
        private String name;
        private String id; // Changed to String to match IoTDevice
        private SimFogDevice connectedFogDevice;
        
        public SimSensor(String name, String id, SimFogDevice fogDevice) {
            this.name = name;
            this.id = id;
            this.connectedFogDevice = fogDevice;
            fogDevice.connectedSensors.add(this);
        }
        
        public void generateTuple(SimTuple tuple) {
            Log.printLine("iFogSim: Sensor " + name + " generated tuple " + tuple.getId());
            tuple.setSource(this);
            // In iFogSim, the tuple would be sent to the fog device
            // Here we're simulating that behavior
            SimulatedIFogSim.processTuple(tuple, connectedFogDevice);
        }
        
        // Getters
        public String getName() { return name; }
        public String getId() { return id; }
        public SimFogDevice getConnectedFogDevice() { return connectedFogDevice; }
    }
    
    public static class SimTuple {
        private int id;
        private SimSensor source;
        private double dataSize;
        private double processingRequirement;
        private int status = SimCloudlet.CREATED;
        private double latency = 0.0;
        private SimFogDevice processingDevice;
        
        public SimTuple(int id, double dataSize, double processingRequirement) {
            this.id = id;
            this.dataSize = dataSize;
            this.processingRequirement = processingRequirement;
        }
        
        // Getters and setters
        public int getId() { return id; }
        public SimSensor getSource() { return source; }
        public void setSource(SimSensor source) { this.source = source; }
        public double getDataSize() { return dataSize; }
        public double getProcessingRequirement() { return processingRequirement; }
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        public double getLatency() { return latency; }
        public void setLatency(double latency) { this.latency = latency; }
        public SimFogDevice getProcessingDevice() { return processingDevice; }
        public void setProcessingDevice(SimFogDevice device) { this.processingDevice = device; }
    }
    
    public static class SimCloudlet {
        public static final int CREATED = 0;
        public static final int SUCCESS = 1;
        public static final int FAILED = 2;
        
        private int id;
        private double mips;
        private int ram;
        private double dataSize;
        private int status = CREATED;
        
        public SimCloudlet(int id, double mips, int ram, double dataSize) {
            this.id = id;
            this.mips = mips;
            this.ram = ram;
            this.dataSize = dataSize;
            this.status = CREATED; // Not yet processed
        }
        
        // Additional constructor with fewer parameters
        public SimCloudlet(int id, double mips) {
            this(id, mips, 10, 500); // Default RAM and dataSize values
        }
        
        // Getters and setters
        public int getId() { return id; }
        public double getMips() { return mips; }
        public int getRam() { return ram; }
        public double getDataSize() { return dataSize; }
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
    }
    
    public static class SimModulePlacement {
        // In iFogSim, this class would handle the placement of application modules
        // on fog devices based on different strategies
        
        protected List<SimFogDevice> fogDevices;
        
        public SimModulePlacement(List<SimFogDevice> fogDevices) {
            this.fogDevices = fogDevices;
        }
        
        public SimFogDevice selectDeviceForModule(SimCloudlet cloudlet) {
            // Implement a simple selection strategy
            // In a real iFogSim implementation, this would be much more complex
            // and would correspond to our hybrid offloading policy
            
            // Sort devices by available capacity
            List<SimFogDevice> sortedDevices = new ArrayList<>(fogDevices);
            sortedDevices.sort((d1, d2) -> Double.compare(
                    d2.getMips() - d2.getResourceUtilization() * d2.getMips(),
                    d1.getMips() - d1.getResourceUtilization() * d1.getMips()));
            
            // Select the device with most available capacity
            for (SimFogDevice device : sortedDevices) {
                if (device.getMips() * (1 - device.getResourceUtilization()) >= cloudlet.getMips()) {
                    return device;
                }
            }
            
            // If no suitable device, return the one with the most resources
            return sortedDevices.get(0);
        }
    }
    
    public static class SimHybridOffloadingPolicy extends SimModulePlacement {
        
        public SimHybridOffloadingPolicy(List<SimFogDevice> fogDevices) {
            super(fogDevices);
        }
        
        @Override
        public SimFogDevice selectDeviceForModule(SimCloudlet cloudlet) {
            // This would implement the hybrid offloading policy from the paper
            // It's a simplified version that demonstrates the concept
            
            // Threshold-based approach (similar to our hybrid policy)
            double loadThreshold = 0.7; // 70% load threshold
            
            // Find devices below the threshold
            List<SimFogDevice> candidateDevices = new ArrayList<>();
            for (SimFogDevice device : fogDevices) {
                if (device.getResourceUtilization() < loadThreshold) {
                    candidateDevices.add(device);
                }
            }
            
            if (!candidateDevices.isEmpty()) {
                // Sort by resource availability
                candidateDevices.sort((d1, d2) -> Double.compare(
                        d2.getMips() * (1 - d2.getResourceUtilization()),
                        d1.getMips() * (1 - d1.getResourceUtilization())));
                return candidateDevices.get(0);
            } else {
                // Fall back to the device with the most resources
                List<SimFogDevice> sortedDevices = new ArrayList<>(fogDevices);
                sortedDevices.sort((d1, d2) -> Double.compare(d2.getMips(), d1.getMips()));
                return sortedDevices.get(0);
            }
        }
    }
    
    // The broker in iFogSim that would manage the simulation
    private static SimModulePlacement modulePlacement;
    
    // Simulation method - processes a tuple on a fog device
    public static void processTuple(SimTuple tuple, SimFogDevice fogDevice) {
        // Check if the device has enough capacity
        if (fogDevice.getMips() >= tuple.getProcessingRequirement()) {
            // Create a cloudlet to represent the tuple processing task
            SimCloudlet cloudlet = new SimCloudlet(tuple.getId(), tuple.getProcessingRequirement());
            
            // Process the task on the fog device
            fogDevice.processCloudlet(cloudlet);
            
            // Update the tuple status and processing device
            tuple.setStatus(cloudlet.getStatus());
            tuple.setProcessingDevice(fogDevice);
            
        } else {
            // If the device doesn't have enough capacity, try to offload
            boolean offloaded = false;
            
            // In a real implementation, we'd use the hybrid offloading algorithm here
            // For this simulation, we'll just try to find any device with enough capacity
            for (SimFogDevice device : modulePlacement.fogDevices) {
                if (device.getMips() >= tuple.getProcessingRequirement()) {
                    SimCloudlet cloudlet = new SimCloudlet(tuple.getId(), tuple.getProcessingRequirement());
                    device.processCloudlet(cloudlet);
                    offloaded = true;
                    
                    // Update the tuple status and processing device
                    tuple.setStatus(cloudlet.getStatus());
                    tuple.setProcessingDevice(device);
                    
                    Log.printLine("iFogSim: Task " + tuple.getId() + " offloaded from " + 
                            fogDevice.getName() + " to " + device.getName());
                    break;
                }
            }
            
            if (!offloaded) {
                Log.printLine("iFogSim: Failed to process task " + tuple.getId() + 
                        ": no suitable device found");
                tuple.setStatus(SimCloudlet.FAILED);
            }
        }
    }
    
    /**
     * Converts our fog devices to simulated iFogSim fog devices
     */
    private static List<SimFogDevice> convertFogDevices(List<FogDevice> devices) {
        List<SimFogDevice> simDevices = new ArrayList<>();
        
        for (FogDevice device : devices) {
            SimFogDevice simDevice = new SimFogDevice(
                    "fog-" + device.getId(),
                    device.getId(),  // This is already a String in your model
                    device.getTotalMips(),
                    device.getRam(),
                    device.getStorage(),
                    device.getBandwidth());
            
            simDevices.add(simDevice);
        }
        
        return simDevices;
    }
    
    /**
     * Converts our IoT devices to simulated iFogSim sensors
     */
    private static List<SimSensor> convertIoTDevices(List<IoTDevice> devices, List<SimFogDevice> fogDevices) {
        List<SimSensor> sensors = new ArrayList<>();
        
        for (IoTDevice device : devices) {
            // Find the connected fog device
            FogDevice connectedFog = device.getConnectedFogDevice();
            SimFogDevice simFog = null;
            for (SimFogDevice fogDevice : fogDevices) {
                if (fogDevice.getId().equals(connectedFog.getId())) {
                    simFog = fogDevice;
                    break;
                }
            }
            
            SimSensor sensor = new SimSensor(
                    "iot-" + device.getId(),
                    device.getId(),  // This is already a String in your model
                    simFog);
            
            sensors.add(sensor);
        }
        
        return sensors;
    }
    
    /**
     * Converts our tasks to simulated iFogSim tuples
     */
    private static SimTuple convertTask(Task task) {
        return new SimTuple(
                Integer.parseInt(task.getId()),
                task.getSize(), // Using getSize() instead of getStorage()
                task.getMillionInstructions()); // Using getMillionInstructions() instead of getMips()
    }
    
    /**
     * Run the iFogSim simulation
     */
    public static void runSimulation(List<FogDevice> fogDevices, List<IoTDevice> iotDevices) {
        Log.printLine("\n============================================");
        Log.printLine("Starting iFogSim simulation...");
        Log.printLine("============================================\n");
        
        // Convert HybOff devices to iFogSim equivalents
        List<SimFogDevice> simFogDevices = convertFogDevices(fogDevices);
        
        // Create sensors (simulating IoT devices)
        List<SimSensor> simSensors = convertIoTDevices(iotDevices, simFogDevices);
        
        // Create the module placement (offloading policy)
        modulePlacement = new SimHybridOffloadingPolicy(simFogDevices);
        
        Log.printLine("iFogSim: Created " + simFogDevices.size() + " fog devices and " +
                simSensors.size() + " sensors.");
        
        // Generate some tuples (tasks) from sensors
        int tupleCount = 100;
        Log.printLine("iFogSim: Generating " + tupleCount + " tuples from sensors...");
        
        // Keep track of successful and failed tasks
        int processedTasks = 0;
        int failedTasks = 0;
        
        // Add latency tracking
        double totalLatency = 0.0;
        
        // Track task distribution by device
        Map<String, Integer> taskDistribution = new HashMap<>();
        
        for (int i = 0; i < tupleCount; i++) {
            // Pick a random sensor
            SimSensor sensor = simSensors.get(i % simSensors.size());
            
            // Create a tuple with random size and processing requirement
            double dataSize = 500 + Math.random() * 1000;  // 500-1500 KB
            double mips = 100 + Math.random() * 400;       // 100-500 MIPS
            
            SimTuple tuple = new SimTuple(i, dataSize, mips);
            
            // Capture start time for latency calculation
            long startTime = System.currentTimeMillis();
            sensor.generateTuple(tuple);
            long endTime = System.currentTimeMillis();
            double latency = (endTime - startTime);
            tuple.setLatency(latency);
            totalLatency += latency;
            
            // Check if tuple was processed
            if (tuple.getStatus() == SimCloudlet.SUCCESS) {
                processedTasks++;
                
                // Update task distribution
                String deviceId = tuple.getProcessingDevice().getId();
                taskDistribution.put(deviceId, taskDistribution.getOrDefault(deviceId, 0) + 1);
            } else {
                failedTasks++;
            }
        }
        
        // Collect results
        double totalUtilization = 0.0;
        double totalEnergy = 0.0;
        
        // Store device utilization and energy for charts
        Map<String, Double> deviceUtilization = new HashMap<>();
        Map<String, Double> deviceEnergy = new HashMap<>();
        
        Log.printLine("\niFogSim: Simulation completed with " + processedTasks + 
                " successful tasks and " + failedTasks + " failed tasks.");
        Log.printLine("\nDevice Statistics:");
        
        // Convert simFogDevices to FogDevice for export
        List<FogDevice> resultFogDevices = new ArrayList<>();
        
        for (SimFogDevice device : simFogDevices) {
            totalUtilization += device.getResourceUtilization();
            totalEnergy += device.getEnergyConsumption();
            
            // Store metrics for charts
            deviceUtilization.put(device.getId(), device.getResourceUtilization() * 100);
            deviceEnergy.put(device.getId(), device.getEnergyConsumption());
            
            // Create equivalent FogDevice for export
            FogDevice resultDevice = new FogDevice(
                device.getId(), 
                (int)device.getMips(), // Cast to int as required by constructor
                device.getRam(), 
                (int)device.getStorage(), // Cast to int as required by constructor
                (int)device.getBandwidth(), // Cast to int as required by constructor
                0.0, 0.0);
                
            // We don't need to set the ID as it's already set in the constructor
            // We can't directly set utilization or energy as there are no setter methods
            // The values will be tracked in the maps instead
            resultFogDevices.add(resultDevice);
            
            Log.printLine("\nDevice: " + device.getName() + " (" + device.getId() + ")");
            Log.printLine("  - Tasks processed: " + device.processedCloudlets.size());
            Log.printLine("  - Resource utilization: " + String.format("%.2f%%", device.getResourceUtilization() * 100));
            Log.printLine("  - Energy consumption: " + String.format("%.2f units", device.getEnergyConsumption()));
        }
        
        double avgUtilization = totalUtilization / simFogDevices.size();
        double avgLatency = processedTasks > 0 ? totalLatency / processedTasks : 0;
        
        Log.printLine("\nSummary:");
        Log.printLine("  - Total processed tasks: " + processedTasks);
        Log.printLine("  - Average resource utilization: " + String.format("%.2f%%", avgUtilization * 100));
        Log.printLine("  - Total energy consumption: " + String.format("%.2f units", totalEnergy));
        Log.printLine("  - Average latency: " + String.format("%.2f ms", avgLatency));
        Log.printLine("\n============================================");
        
        // Export results to CSV
        String resultsDir = ResultsExporter.exportResults(
                resultFogDevices,
                processedTasks,
                avgUtilization,
                totalEnergy,
                avgLatency,
                taskDistribution,
                "iFogSim");
        
        // Generate charts
        ChartGenerator.generateCharts(
                resultsDir,
                taskDistribution,
                deviceUtilization,
                deviceEnergy);
        
        Log.printLine("\nResults and charts saved to: " + resultsDir);
    }
    
    /**
     * Main entry point for the simulated iFogSim demo
     */
    public static void main(String[] args) {
        // Create our own fog and IoT devices since TestSimulation methods are private
        List<FogDevice> fogDevices = createFogDevices(10, 2);
        List<IoTDevice> iotDevices = createIoTDevices(20, fogDevices);
        
        // Run the simulated iFogSim implementation
        runSimulation(fogDevices, iotDevices);
        
        // For comparison, also run our original implementation
        Log.printLine("\n\nRunning original HybOff implementation for comparison...");
        try {
            TestSimulation.main(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Create a list of fog devices for the simulation
     */
    private static List<FogDevice> createFogDevices(int numDevices, int numLevels) {
        List<FogDevice> fogDevices = new ArrayList<>();
        
        // Create cloud device with coordinates at center
        FogDevice cloudDevice = new FogDevice("cloud", 10000, 16384, 1000000, 1000, 0.0, 0.0);
        fogDevices.add(cloudDevice);
        
        // Create edge devices in a grid pattern
        for (int i = 0; i < numDevices; i++) {
            int level = i % numLevels + 1; // Levels 1-numLevels (cloud is level 0)
            int mips = 2000 - (level * 500); // Higher level = less powerful
            int ram = 4096 - (level * 1024);
            int storage = (int)(100000 - (level * 20000));
            int bandwidth = (int)(100 - (level * 20));
            
            // Position devices in a grid pattern
            double x = 100 * (i % 5);
            double y = 100 * (i / 5);
            
            FogDevice device = new FogDevice("fog-" + i, mips, ram, storage, bandwidth, x, y);
            
            // For demonstration purposes, we establish the hierarchy through communicator
            // Note: Since there's no direct setParentId method, we'll simulate the relationship 
            // without actually setting it
            
            fogDevices.add(device);
        }
        
        return fogDevices;
    }
    
    /**
     * Create a list of IoT devices for the simulation
     */
    private static List<IoTDevice> createIoTDevices(int numDevices, List<FogDevice> fogDevices) {
        List<IoTDevice> iotDevices = new ArrayList<>();
        
        // Skip the cloud device
        List<FogDevice> edgeFogDevices = new ArrayList<>(fogDevices);
        if (!edgeFogDevices.isEmpty()) {
            edgeFogDevices.remove(0);
        }
        
        // Distribute IoT devices evenly across fog devices
        for (int i = 0; i < numDevices; i++) {
            FogDevice connectedFog = edgeFogDevices.get(i % edgeFogDevices.size());
            
            // Position IoT device near its fog device with a small offset
            double x = connectedFog.getX() + (Math.random() * 50 - 25); // +/- 25m offset
            double y = connectedFog.getY() + (Math.random() * 50 - 25); // +/- 25m offset
            
            IoTDevice iotDevice = new IoTDevice("iot-" + i, x, y);
            iotDevice.setConnectedFogDevice(connectedFog);
            iotDevices.add(iotDevice);
        }
        
        return iotDevices;
    }
}

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
import org.hyboff.simulation.util.Log;

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
    }
    
    public static class SimCloudlet {
        public static final int SUCCESS = 1;
        public static final int FAILED = 0;
        
        private int id;
        private double mips;
        private int ram;
        private double storage;
        private int status;
        
        public SimCloudlet(int id, double mips, int ram, double storage) {
            this.id = id;
            this.mips = mips;
            this.ram = ram;
            this.storage = storage;
        }
        
        // Getters and setters
        public int getId() { return id; }
        public double getMips() { return mips; }
        public int getRam() { return ram; }
        public double getStorage() { return storage; }
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
        // Convert tuple to cloudlet (similar to how iFogSim works)
        SimCloudlet cloudlet = new SimCloudlet(
                tuple.getId(), 
                tuple.getProcessingRequirement(), 
                10, // RAM requirement 
                tuple.getDataSize());
        
        // Use the module placement policy to select the best device
        SimFogDevice selectedDevice = modulePlacement.selectDeviceForModule(cloudlet);
        
        // Process the cloudlet on the selected device
        selectedDevice.processCloudlet(cloudlet);
    }
    
    // Mapping between our model and the iFogSim model
    private static Map<FogDevice, SimFogDevice> fogDeviceMap = new HashMap<>();
    private static Map<IoTDevice, SimSensor> iotDeviceMap = new HashMap<>();
    
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
            
            fogDeviceMap.put(device, simDevice);
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
            SimFogDevice simFog = fogDeviceMap.get(connectedFog);
            
            SimSensor sensor = new SimSensor(
                    "iot-" + device.getId(),
                    device.getId(),  // This is already a String in your model
                    simFog);
            
            iotDeviceMap.put(device, sensor);
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
        // Convert our model to iFogSim model
        List<SimFogDevice> simFogDevices = convertFogDevices(fogDevices);
        List<SimSensor> simSensors = convertIoTDevices(iotDevices, simFogDevices);
        
        // Create the module placement (offloading policy)
        modulePlacement = new SimHybridOffloadingPolicy(simFogDevices);
        
        // Simulate the IoT devices generating tasks
        Log.printLine("\n========= iFogSim SIMULATION START =========");
        Log.printLine("Using HybridOffloadingPolicy for task placement");
        
        // Generate some sample tasks from each IoT device
        int tupleCount = 0;
        for (IoTDevice device : iotDevices) {
            SimSensor sensor = iotDeviceMap.get(device);
            for (int i = 0; i < 5; i++) { // Generate 5 tuples per sensor
                Task task = new Task(String.valueOf(tupleCount++), 100 + (int)(Math.random() * 400), 10, true);
                SimTuple tuple = convertTask(task);
                sensor.generateTuple(tuple);
            }
        }
        
        // Print simulation results
        Log.printLine("\n========= iFogSim SIMULATION RESULTS =========");
        double totalUtilization = 0;
        double totalEnergy = 0;
        int processedTasks = 0;
        
        for (SimFogDevice device : simFogDevices) {
            totalUtilization += device.getResourceUtilization();
            totalEnergy += device.getEnergyConsumption();
            processedTasks += device.processedCloudlets.size();
            
            Log.printLine("Fog Device: " + device.getName());
            Log.printLine("  - Processed tasks: " + device.processedCloudlets.size());
            Log.printLine("  - Resource utilization: " + String.format("%.2f%%", device.getResourceUtilization() * 100));
            Log.printLine("  - Energy consumption: " + String.format("%.2f units", device.getEnergyConsumption()));
        }
        
        double avgUtilization = totalUtilization / simFogDevices.size();
        
        Log.printLine("\nSummary:");
        Log.printLine("  - Total processed tasks: " + processedTasks);
        Log.printLine("  - Average resource utilization: " + String.format("%.2f%%", avgUtilization * 100));
        Log.printLine("  - Total energy consumption: " + String.format("%.2f units", totalEnergy));
        Log.printLine("\n============================================");
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

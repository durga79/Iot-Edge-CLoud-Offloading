package org.hyboff.simulation.ifogsim;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;
import org.hyboff.simulation.TestSimulation;
import org.hyboff.simulation.offloading.HybridOffloadingPolicy;

/**
 * This class demonstrates how to use iFogSim for simulating the hybrid offloading strategy
 * from the HybOff implementation. This runs alongside the original simulation but uses 
 * iFogSim's architecture instead of our custom implementation.
 */
public class IFogSimHybridOffloadingDemo {

    private static boolean CLOUD_TRACE_FLAG = false;
    private static final int NUM_FOG_DEVICES = 6; // Number of fog devices
    private static final int NUM_IOT_DEVICES = 12; // Number of IoT devices (sensors)

    public static void main(String[] args) {
        try {
            // Print startup message
            System.out.println("========================================================");
            System.out.println("Starting HybOff simulation using iFogSim framework...");
            System.out.println("========================================================");
            
            // Print warning about iFogSim JAR dependency
            System.out.println("NOTE: This demo requires the iFogSim JAR file to be available");
            System.out.println("in the lib directory. If you see ClassNotFoundExceptions,");
            System.out.println("please download iFogSim from GitHub and build it locally.");
            System.out.println("Then place the JAR file in the lib directory and try again.");
            System.out.println("========================================================");
            
            // Initialize iFogSim
            Log.disable();
            int numUsers = 1;
            Calendar calendar = Calendar.getInstance();
            CloudSim.init(numUsers, calendar, CLOUD_TRACE_FLAG);
            FogBroker broker = new FogBroker("broker");
            
            // Create Application
            Application application = createApplication("HybOff_App", broker.getId());
            application.setUserId(broker.getId());
            
            // Create physical topology
            List<FogDevice> fogDevices = createFogDevices(broker.getId());
            
            // Create sensors and actuators
            List<Sensor> sensors = createSensors("sensor", broker.getId(), "HybOff_App");
            List<Actuator> actuators = createActuators("actuator", broker.getId(), "HybOff_App");
            
            // Configure controller
            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
            
            // Map modules to devices 
            // This is similar to how our offloading policy works in the main simulation
            moduleMapping.addModuleToDevice("taskProcessor", "cloud");
            for (FogDevice device : fogDevices) {
                if (device.getName().startsWith("edge")) {
                    moduleMapping.addModuleToDevice("taskProcessor", device.getName());
                }
            }
            
            // Create the controller
            Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);
            controller.submitApplication(application, 
                new ModulePlacementMapping(fogDevices, application, moduleMapping));
            
            // Start simulation
            System.out.println("Starting iFogSim simulation...");
            CloudSim.startSimulation();
            CloudSim.stopSimulation();
            
            // Print metrics
            System.out.println("========== RESULTS ==========");
            System.out.println("iFogSim Simulation completed!");
            
            // Print execution latency
            for (FogDevice device : fogDevices) {
                if (device.getName().startsWith("edge")) {
                    System.out.println(device.getName() + " average execution time: " 
                        + device.getAvgExecutionTime());
                }
            }
            
            System.out.println("========================================================");
            System.out.println("Returning to the original HybOff simulation...");
            System.out.println("========================================================");
            
            // Run the original simulation to compare results
            TestSimulation.main(args);
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("iFogSim integration requires the iFogSim JAR file to be present");
            System.out.println("in the lib directory. Please download and build iFogSim locally,");
            System.out.println("then place the JAR in the lib directory and try again.");
            
            System.out.println("\nContinuing with original HybOff simulation...");
            
            // Run original simulation as fallback
            try {
                TestSimulation.main(args);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * Creates the fog devices that will form the fog computing infrastructure.
     * This creates a hierarchy with cloud, proxy, and edge devices.
     */
    private static List<FogDevice> createFogDevices(int userId) throws Exception {
        List<FogDevice> devices = new ArrayList<FogDevice>();
        
        // Create the cloud device
        FogDevice cloud = createFogDevice("cloud", 44800, 40000, 10000, 10000, 0, 0.01, 16*103, 16*83.25);
        cloud.setParentId(-1);
        devices.add(cloud);
        
        // Create a proxy layer
        FogDevice proxy = createFogDevice("proxy", 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
        proxy.setParentId(cloud.getId());
        proxy.setUplinkLatency(100);
        devices.add(proxy);
        
        // Create edge devices - these match our FogDevice class in the main simulation
        for (int i = 0; i < NUM_FOG_DEVICES; i++) {
            FogDevice edge = createFogDevice("edge-" + i, 
                    1000 + (i * 100), // MIPS increases with each device 
                    1000, // RAM 
                    10000, // Uplink bandwidth
                    10000, // Downlink bandwidth
                    2, // Level in hierarchy
                    0.0, // Rate per MIPS
                    87.53, 82.44);
            edge.setParentId(proxy.getId());
            edge.setUplinkLatency(2); // 2ms latency to proxy
            devices.add(edge);
        }
        
        return devices;
    }
    
    /**
     * Creates a fog device with the specified characteristics.
     */
    private static FogDevice createFogDevice(String name, long mips, int ram, 
            long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
        
        List<org.cloudbus.cloudsim.Pe> peList = new ArrayList<>();
        peList.add(new org.cloudbus.cloudsim.Pe(0, new org.cloudbus.cloudsim.provisioners.PeProvisionerSimple(mips)));
        
        int hostId = FogUtils.generateEntityId();
        long storage = 1000000;
        int bw = 10000;
        
        org.cloudbus.cloudsim.power.PowerHost host = new org.cloudbus.cloudsim.power.PowerHost(
                hostId,
                new org.cloudbus.cloudsim.provisioners.RamProvisionerSimple(ram),
                new org.cloudbus.cloudsim.provisioners.BwProvisionerSimple(bw),
                storage,
                peList,
                new StreamOperatorScheduler(peList),
                new FogLinearPowerModel(busyPower, idlePower)
        );
        
        List<org.cloudbus.cloudsim.Host> hostList = new ArrayList<org.cloudbus.cloudsim.Host>();
        hostList.add(host);
        
        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;
        
        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem, costPerStorage, costPerBw
        );
        
        FogDevice fogdevice = null;
        try {
            fogdevice = new FogDevice(name, characteristics, 
                    new AppModuleAllocationPolicy(hostList), 
                    new LinkedList<org.cloudbus.cloudsim.Storage>(), 10, upBw, downBw, 0, ratePerMips);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        fogdevice.setLevel(level);
        return fogdevice;
    }
    
    /**
     * Creates sensors that generate data for the application.
     */
    private static List<Sensor> createSensors(String prefix, int userId, String appId) {
        List<Sensor> sensors = new ArrayList<>();
        
        for (int i = 0; i < NUM_IOT_DEVICES; i++) {
            // Distribute sensors across edge devices evenly
            int edgeDeviceId = i % NUM_FOG_DEVICES;
            
            // Create sensor with deterministic distribution
            // Similar to how our IoTDevice class generates tasks
            Sensor sensor = new Sensor(prefix + "-" + i, "SENSOR", userId, appId, 
                    new DeterministicDistribution(5)); // Generate tuple every 5 seconds
            
            // Connect sensor to edge device
            sensor.setGatewayDeviceId("edge-" + edgeDeviceId);
            sensor.setLatency(1.0); // 1 ms latency to gateway
            
            sensors.add(sensor);
        }
        
        return sensors;
    }
    
    /**
     * Creates actuators for the application.
     */
    private static List<Actuator> createActuators(String prefix, int userId, String appId) {
        List<Actuator> actuators = new ArrayList<>();
        
        for (int i = 0; i < NUM_FOG_DEVICES; i++) {
            // Create actuator for each edge device
            Actuator actuator = new Actuator(prefix + "-" + i, userId, appId, "ACTUATOR");
            actuator.setGatewayDeviceId("edge-" + i);
            actuator.setLatency(1.0); // 1 ms latency
            
            actuators.add(actuator);
        }
        
        return actuators;
    }
    
    /**
     * Creates the application model with modules and data flows.
     */
    private static Application createApplication(String appId, int userId) {
        Application application = Application.createApplication(appId, userId);
        
        // Add modules (equivalent to task types in our simulation)
        application.addAppModule("taskProcessor", 10); // computing capacity in MIPS
        application.addAppModule("taskManager", 10);
        
        // Add edges (data flow paths between modules)
        application.addAppEdge("SENSOR", "taskManager", 1000, 500, "SensorData", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("taskManager", "taskProcessor", 2000, 1000, "TaskData", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("taskProcessor", "ACTUATOR", 100, 50, "ResultData", Tuple.DOWN, AppEdge.ACTUATOR);
        
        // Add tuples to edges
        application.addTupleMapping("taskManager", "SensorData", "TaskData", new FractionalSelectivity(1.0));
        application.addTupleMapping("taskProcessor", "TaskData", "ResultData", new FractionalSelectivity(1.0));
        
        // Define application loops
        final AppLoop loop = new AppLoop(new ArrayList<String>() {{
            add("SENSOR");
            add("taskManager");
            add("taskProcessor");
            add("ACTUATOR");
        }});
        
        List<AppLoop> loops = new ArrayList<AppLoop>() {{
            add(loop);
        }};
        application.setLoops(loops);
        
        return application;
    }
}

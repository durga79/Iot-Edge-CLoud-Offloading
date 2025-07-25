package org.hyboff.simulation;

/**
 * Main entry point that provides options to run the simulation using either:
 * 1. The original custom implementation
 * 2. The iFogSim implementation (simulated)
 */
public class RunSimulation {
    
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("--ifogsim")) {
            runIFogSimVersion(args);
        } else {
            runCustomVersion(args);
        }
    }
    
    private static void runCustomVersion(String[] args) {
        System.out.println("===================================================");
        System.out.println("Running HybOff simulation with custom implementation");
        System.out.println("===================================================");
        
        try {
            TestSimulation.main(args);
        } catch (Exception e) {
            System.out.println("Error running custom simulation:");
            e.printStackTrace();
        }
    }
    
    private static void runIFogSimVersion(String[] args) {
        System.out.println("===================================================");
        System.out.println("Running HybOff simulation with simulated iFogSim implementation");
        System.out.println("===================================================");
        System.out.println("NOTE: This is a simulated version that demonstrates how iFogSim");
        System.out.println("would be used to implement the hybrid offloading strategy.");
        
        try {
            // Remove the --ifogsim argument before passing to the iFogSim demo
            String[] newArgs = new String[args.length - 1];
            if (args.length > 1) {
                System.arraycopy(args, 1, newArgs, 0, args.length - 1);
            }
            
            // Run simulated iFogSim implementation
            org.hyboff.simulation.ifogsim.SimulatedIFogSim.main(newArgs);
        } catch (Exception e) {
            System.out.println("Error running simulated iFogSim simulation:");
            e.printStackTrace();
            System.out.println("\nFalling back to custom implementation...");
            runCustomVersion(args);
        }
    }
}

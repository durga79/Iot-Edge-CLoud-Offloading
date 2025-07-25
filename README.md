# HybOff: A Hybrid Offloading Approach for IoT Task Offloading in Edge-Cloud Environments

## Project Overview
This project implements the concepts from the paper "Task offloading for multi-server edge computing in industrial Internet with joint load balance and fuzzy security" (Jin et al., Scientific Reports, 2024). The implementation demonstrates a novel approach for IoT task offloading in edge-cloud environments, focusing on improving load balancing, resource utilization, and security integration.

## Key Features
1. **Hybrid Offloading Algorithm**: Combines the benefits of both static and dynamic offloading approaches to optimize task distribution.
2. **Multi-Server Edge Computing**: Supports offloading tasks to multiple fog/edge devices based on their current load and capabilities.
3. **Load Balancing**: Implements sophisticated load balancing to prevent network congestion and improve resource utilization.
4. **Security Integration**: Incorporates security levels and authentication mechanisms similar to the "fuzzy security" concept.
5. **Energy Awareness**: Tracks and optimizes energy consumption across devices.
6. **Partial Offloading**: Supports decisions about which tasks should be offloaded and to which edge servers.

## Components
1. **IoT Devices**: Simulates IoT devices that generate computation-intensive tasks.
2. **Fog Devices**: Represents edge servers that can process offloaded tasks.
3. **Wireless Network Model**: Simulates network transmission with configurable packet loss and latency.
4. **Energy Model**: Tracks energy consumption of devices during task processing.
5. **Security Manager**: Handles authentication and security level enforcement.

## Technologies Used
- **Java**: Programming language for the entire simulation framework
- **Maven**: Dependency management and build tool

## Implementation Details
The project simulates an edge computing environment with multiple fog devices (edge servers) and IoT devices. The HybOff algorithm implements:

- Dynamic monitoring of resource utilization across fog devices
- Task scheduling based on device capabilities and current load
- Security-aware task offloading with authentication mechanisms
- Energy-efficient task processing with energy consumption tracking
- Wireless network simulation with configurable packet loss and latency
- Three different offloading policies for comparison:
  - Static Offloading (SoA): Fixed offloading decisions
  - Dynamic Offloading (PoA): Decisions based on current system state
  - Hybrid Offloading (HybOff): Combines static and dynamic approaches

## Performance Metrics
The implementation measures:

1. **Task Completion Rate**: Percentage of tasks successfully completed
2. **Resource Utilization**: How efficiently fog device resources are used
3. **Load Balancing**: Distribution of tasks across available fog devices
4. **Average Response Time**: Time from task creation to completion
5. **Energy Consumption**: Total and per-device energy usage
6. **Network Metrics**: Offloading rates and transmission statistics
7. **Security Metrics**: Security level distribution and overhead

## Requirements
- Java 8 or higher
- Maven 3.6 or higher

## Usage

### Linux/macOS

1. Clone the repository
   ```bash
   git clone https://github.com/your-repo/IoT-Edge-Cloud-Offloading.git
   cd IoT-Edge-Cloud-Offloading
   ```

2. Compile the project
   ```bash
   mvn clean compile
   ```

3. Run the simulation with the custom implementation
   ```bash
   mvn exec:java
   ```

4. Run the simulation with iFogSim integration (requires iFogSim JAR)
   ```bash
   mvn exec:java -Dexec.args="--ifogsim"
   ```

### Windows

#### Option 1: Using the run_simulation.bat Script for iFogSim Simulation

1. Simply double-click the `run_simulation.bat` file in the project directory
2. The script will automatically run the simulation with the iFogSim integration
3. All compilation and execution steps are handled automatically

#### Option 2: Using Command Line

1. Clone the repository
   ```cmd
   git clone https://github.com/your-repo/IoT-Edge-Cloud-Offloading.git
   cd IoT-Edge-Cloud-Offloading
   ```

2. Compile the project
   ```cmd
   mvn clean compile
   ```

3. Run the simulation with the custom implementation
   ```cmd
   mvn exec:java -Dexec.mainClass="org.hyboff.simulation.RunSimulation"
   ```

4. Run the simulation with the simulated iFogSim integration
   ```cmd
   mvn exec:java -Dexec.args="--ifogsim"
   ```
   
   This will use our built-in simulated iFogSim implementation that doesn't require downloading the external iFogSim JAR file.

5. Alternatively, if Maven exec plugin fails, you can run the packaged JAR:
   ```cmd
   mvn clean package
   java -jar target/IoT-Edge-Cloud-Offloading-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```
   
   To run with iFogSim:
   ```cmd
   java -jar target/IoT-Edge-Cloud-Offloading-1.0-SNAPSHOT-jar-with-dependencies.jar --ifogsim
   ```

### iFogSim Integration

This project includes two options for iFogSim integration:

#### Option 1: Simulated iFogSim (Recommended)
The project now includes a built-in simulated iFogSim implementation that doesn't require downloading the actual iFogSim JAR file. This implementation mimics iFogSim concepts using the existing project classes.

To use the simulated iFogSim:
1. Simply run the simulation with the `--ifogsim` flag as shown above
2. The simulation will automatically use the simulated iFogSim implementation
3. Results can be compared with the standard simulation to validate the integration concept

#### Option 2: Real iFogSim Integration (Optional)
If you want to use the actual iFogSim integration (requires modifications to the project):

1. Download iFogSim from [GitHub](https://github.com/Cloudslab/iFogSim)
2. Build iFogSim using the provided instructions
3. Copy the generated JAR file to the `lib` directory in this project
4. Rename it to `ifogsim.jar`
5. Restore the original iFogSim integration classes from backup
6. Re-add the system-scoped dependency in `pom.xml`
7. Run the simulation with the `--ifogsim` flag

### Customizing the simulation
You can modify the simulation parameters in the `TestSimulation.java` file:
- Number of fog devices
- Number of IoT devices
- Task generation patterns
- Security levels
- Network parameters

## Paper Citation
Jin, X., Zhang, S., Ding, Y., & Wang, Z. (2024). Task offloading for multi-server edge computing in industrial Internet with joint load balance and fuzzy security. Scientific Reports, 14, 27813. https://doi.org/10.1038/s41598-024-79464-2

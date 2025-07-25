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

### Clone the repository

#### Linux/macOS
```bash
git clone https://github.com/yourusername/IoT-Edge-Cloud-Offloading.git
cd IoT-Edge-Cloud-Offloading
```

#### Windows
```batch
git clone https://github.com/yourusername/IoT-Edge-Cloud-Offloading.git
cd IoT-Edge-Cloud-Offloading
```

### Compile the project

#### Linux/macOS
```bash
mvn clean compile
```

#### Windows
```batch
mvn clean compile
```

### Run the simulation

#### Linux/macOS
```bash
mvn exec:java -Dexec.mainClass="org.hyboff.simulation.TestSimulation"
```

#### Windows
```batch
mvn exec:java -Dexec.mainClass="org.hyboff.simulation.TestSimulation"
```

### Alternative method for Windows (if exec:java doesn't work)
```batch
mvn clean package
java -jar target/IoT-Edge-Cloud-Offloading-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Customizing the simulation
You can modify the simulation parameters in the `TestSimulation.java` file:
- Number of fog devices
- Number of IoT devices
- Task generation patterns
- Security levels
- Network parameters

## Paper Citation
Jin, X., Zhang, S., Ding, Y., & Wang, Z. (2024). Task offloading for multi-server edge computing in industrial Internet with joint load balance and fuzzy security. Scientific Reports, 14, 27813. https://doi.org/10.1038/s41598-024-79464-2

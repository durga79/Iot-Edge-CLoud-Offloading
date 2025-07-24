# HybOff: A Hybrid Offloading Approach for IoT Task Offloading in Edge-Cloud Environments

## Project Overview
This project is an implementation of the "HybOff: a Hybrid Offloading approach to improve load balancing in fog environments" paper (Journal of Cloud Computing, 2024). The implementation demonstrates a novel approach for IoT task offloading in edge-cloud environments, focusing on improving load balancing and resource utilization.

## Key Features
1. **Hybrid Offloading Algorithm**: Combines the benefits of both static and dynamic offloading approaches.
2. **Cell-Based Architecture**: Groups adjacent fog nodes into cells for efficient resource management.
3. **Master-Follower Structure**: Each cell has a master node that manages offloading decisions within the cell.
4. **Reduced Communication Overhead**: Minimizes message exchanges between servers.
5. **Improved Load Balancing**: Achieves better load balancing compared to traditional approaches.

## Components
1. **Big Data System**: Simulates processing of large volumes of IoT data across edge-cloud layers.
2. **IoT and Wireless Technologies**: Models different wireless communication technologies between IoT devices and edge/fog nodes.
3. **Service Distribution**: Implements task offloading strategies across IoT devices, edge nodes, and cloud.

## Technologies Used
- **iFogSim**: Simulation tool for fog computing environments
- **Java**: Programming language
- **Maven**: Dependency management and build tool

## Implementation Details
The project simulates a fog computing environment with multiple cells, each containing fog servers. The HybOff algorithm is implemented to:
- Monitor workload on fog servers
- Cluster servers into cells using K-means algorithm
- Elect master nodes in each cell
- Implement static offloading within cells
- Allow dynamic offloading for urgent tasks

## Performance Metrics
The implementation measures:
1. Resource utilization ratio of the fog system
2. Load balancing among fog servers
3. Overall system performance
4. Offloading message overhead
5. Service time for tasks

## Requirements
- Java 8 or higher
- Maven 3.6 or higher

## Usage
```bash
mvn clean install
mvn exec:java -Dexec.mainClass="org.hyboff.simulation.HybOffSimulation"
```

## Paper Citation
HybOff: a Hybrid Offloading approach to improve load balancing in fog environments. Journal of Cloud Computing (2024).
# Iot-Edge-CLoud-Offloading

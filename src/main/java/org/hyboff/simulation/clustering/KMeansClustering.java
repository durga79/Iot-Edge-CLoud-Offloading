package org.hyboff.simulation.clustering;

import org.hyboff.simulation.model.FogDevice;
import java.util.*;

/**
 * Implementation of K-means clustering algorithm for grouping fog devices into cells.
 * Based on the approach described in the HybOff paper.
 */
public class KMeansClustering {
    private List<FogDevice> fogDevices;
    private int k;  // Number of clusters/cells
    private List<Centroid> centroids;
    private Map<Centroid, List<FogDevice>> clusters;
    private int maxIterations = 100;
    private Random random = new Random(42); // Fixed seed for reproducibility
    
    public KMeansClustering(List<FogDevice> fogDevices, int k) {
        this.fogDevices = fogDevices;
        this.k = k;
        this.centroids = new ArrayList<>();
        this.clusters = new HashMap<>();
    }
    
    public void cluster() {
        // Initialize centroids randomly
        initializeCentroids();
        
        // Main K-means loop
        boolean converged = false;
        int iteration = 0;
        
        while (!converged && iteration < maxIterations) {
            // Clear previous clusters
            clusters.clear();
            for (Centroid centroid : centroids) {
                clusters.put(centroid, new ArrayList<>());
            }
            
            // Assign each fog device to the nearest centroid
            for (FogDevice device : fogDevices) {
                Centroid nearest = findNearestCentroid(device);
                clusters.get(nearest).add(device);
            }
            
            // Update centroids and check for convergence
            converged = true;
            for (Centroid centroid : centroids) {
                List<FogDevice> clusterDevices = clusters.get(centroid);
                if (clusterDevices.isEmpty()) {
                    continue;
                }
                
                // Calculate new centroid position
                double sumX = 0, sumY = 0;
                for (FogDevice device : clusterDevices) {
                    sumX += device.getX();
                    sumY += device.getY();
                }
                
                double newX = sumX / clusterDevices.size();
                double newY = sumY / clusterDevices.size();
                
                // Check if centroid moved
                if (Math.abs(centroid.getX() - newX) > 0.001 || Math.abs(centroid.getY() - newY) > 0.001) {
                    converged = false;
                    centroid.setX(newX);
                    centroid.setY(newY);
                }
            }
            
            iteration++;
        }
        
        // Assign cell IDs and select master nodes
        for (int i = 0; i < centroids.size(); i++) {
            Centroid centroid = centroids.get(i);
            List<FogDevice> cellMembers = clusters.get(centroid);
            
            if (!cellMembers.isEmpty()) {
                // Set cell ID for all members
                for (FogDevice device : cellMembers) {
                    device.setCellId(i);
                }
                
                // Select master node (closest to centroid)
                FogDevice master = findClosestToCenter(cellMembers, centroid);
                master.setAsMaster();
                
                // Connect all cell members to the master
                for (FogDevice device : cellMembers) {
                    if (device != master) {
                        device.setAsMember();
                        master.addCellMember(device);
                    }
                }
            }
        }
    }
    
    private void initializeCentroids() {
        centroids.clear();
        
        // Find min and max coordinates to initialize within the bounding box
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        
        for (FogDevice device : fogDevices) {
            minX = Math.min(minX, device.getX());
            minY = Math.min(minY, device.getY());
            maxX = Math.max(maxX, device.getX());
            maxY = Math.max(maxY, device.getY());
        }
        
        // Initialize k centroids randomly within the bounding box
        for (int i = 0; i < k; i++) {
            double x = minX + random.nextDouble() * (maxX - minX);
            double y = minY + random.nextDouble() * (maxY - minY);
            centroids.add(new Centroid(x, y));
        }
    }
    
    private Centroid findNearestCentroid(FogDevice device) {
        Centroid nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Centroid centroid : centroids) {
            double distance = calculateDistance(device.getX(), device.getY(), centroid.getX(), centroid.getY());
            if (distance < minDistance) {
                minDistance = distance;
                nearest = centroid;
            }
        }
        
        return nearest;
    }
    
    private FogDevice findClosestToCenter(List<FogDevice> devices, Centroid centroid) {
        FogDevice closest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (FogDevice device : devices) {
            double distance = calculateDistance(device.getX(), device.getY(), centroid.getX(), centroid.getY());
            if (distance < minDistance) {
                minDistance = distance;
                closest = device;
            }
        }
        
        return closest;
    }
    
    private double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
    
    // Inner class to represent a cluster centroid
    private static class Centroid {
        private double x;
        private double y;
        
        public Centroid(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        public double getX() {
            return x;
        }
        
        public void setX(double x) {
            this.x = x;
        }
        
        public double getY() {
            return y;
        }
        
        public void setY(double y) {
            this.y = y;
        }
    }
}

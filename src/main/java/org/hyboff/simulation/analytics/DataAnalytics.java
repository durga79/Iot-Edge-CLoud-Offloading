package org.hyboff.simulation.analytics;

import org.hyboff.simulation.model.FogDevice;
import org.hyboff.simulation.model.Task;
import org.hyboff.simulation.util.Log;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Data Analytics component for processing IoT data streams
 * Provides data aggregation, filtering, and analysis capabilities
 */
public class DataAnalytics {
    
    // Analytics modes
    public enum AnalyticsMode {
        BATCH,      // Batch processing
        STREAMING,  // Real-time streaming
        HYBRID      // Hybrid approach
    }
    
    // Data storage
    private Map<String, List<DataPoint>> dataStore;
    private Map<String, AggregatedData> aggregatedData;
    
    // Analytics configuration
    private AnalyticsMode mode;
    private int batchSize;
    private int windowSize; // For streaming analytics
    private double aggregationInterval; // Time interval for aggregation in ms
    
    // Performance metrics
    private int processedDataPoints;
    private int totalDataPoints;
    private double processingTime;
    private double lastAggregationTime;
    
    /**
     * Create a data analytics component with specified mode
     */
    public DataAnalytics(AnalyticsMode mode) {
        this.mode = mode;
        this.dataStore = new ConcurrentHashMap<>();
        this.aggregatedData = new ConcurrentHashMap<>();
        this.processedDataPoints = 0;
        this.totalDataPoints = 0;
        this.processingTime = 0;
        this.lastAggregationTime = 0;
        
        // Set default configuration based on mode
        switch (mode) {
            case BATCH:
                this.batchSize = 100;
                this.windowSize = 0; // Not used in batch mode
                this.aggregationInterval = 1000; // 1 second
                break;
            case STREAMING:
                this.batchSize = 1; // Process immediately
                this.windowSize = 10; // Sliding window of 10 data points
                this.aggregationInterval = 100; // 100 ms
                break;
            case HYBRID:
                this.batchSize = 20; // Small batches
                this.windowSize = 50; // Larger window
                this.aggregationInterval = 500; // 500 ms
                break;
        }
    }
    
    /**
     * Process a data point from an IoT device
     * @param sourceId Source device ID
     * @param timestamp Timestamp of data
     * @param value Data value
     * @param metadata Additional metadata
     */
    public void processDataPoint(String sourceId, long timestamp, double value, Map<String, Object> metadata) {
        // Create data point
        DataPoint dataPoint = new DataPoint(sourceId, timestamp, value, metadata);
        
        // Store data point
        if (!dataStore.containsKey(sourceId)) {
            dataStore.put(sourceId, new ArrayList<>());
        }
        dataStore.get(sourceId).add(dataPoint);
        totalDataPoints++;
        
        // Process based on mode
        switch (mode) {
            case BATCH:
                // Check if batch size reached
                if (dataStore.get(sourceId).size() >= batchSize) {
                    processBatch(sourceId);
                }
                break;
                
            case STREAMING:
                // Process immediately
                processStreamingData(sourceId, dataPoint);
                break;
                
            case HYBRID:
                // Process mini-batch if size reached
                if (dataStore.get(sourceId).size() >= batchSize) {
                    processBatch(sourceId);
                }
                // Also update streaming window
                processStreamingData(sourceId, dataPoint);
                break;
        }
        
        // Check if aggregation interval elapsed
        double currentTime = System.currentTimeMillis();
        if (currentTime - lastAggregationTime >= aggregationInterval) {
            aggregateAllData();
            lastAggregationTime = currentTime;
        }
    }
    
    /**
     * Process data from a completed task
     * @param task Completed task
     * @param device Device that processed the task
     */
    public void processTaskData(Task task, FogDevice device) {
        // Extract metadata from task
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("taskId", task.getId());
        metadata.put("taskSize", task.getSize());
        metadata.put("responseTime", task.getResponseTime());
        metadata.put("urgent", task.isUrgent());
        metadata.put("processingDevice", device.getId());
        
        // Process as data point
        processDataPoint(task.getSourceId(), System.currentTimeMillis(), 
                        task.getResponseTime(), metadata);
    }
    
    /**
     * Process a batch of data points
     * @param sourceId Source device ID
     */
    private void processBatch(String sourceId) {
        List<DataPoint> batch = dataStore.get(sourceId);
        if (batch.isEmpty()) {
            return;
        }
        
        long startTime = System.currentTimeMillis();
        
        // Apply batch processing algorithms
        // 1. Calculate statistics
        DoubleSummaryStatistics stats = batch.stream()
            .mapToDouble(DataPoint::getValue)
            .summaryStatistics();
        
        // 2. Detect anomalies (simple z-score method)
        double mean = stats.getAverage();
        double stdDev = calculateStdDev(batch, mean);
        List<DataPoint> anomalies = batch.stream()
            .filter(dp -> Math.abs(dp.getValue() - mean) > 2 * stdDev)
            .collect(Collectors.toList());
        
        // 3. Create aggregated data
        AggregatedData aggData = new AggregatedData(
            sourceId,
            System.currentTimeMillis(),
            batch.size(),
            stats.getMin(),
            stats.getMax(),
            stats.getAverage(),
            stdDev,
            anomalies.size()
        );
        
        // Store aggregated data
        aggregatedData.put(sourceId, aggData);
        
        // Update metrics
        long endTime = System.currentTimeMillis();
        processedDataPoints += batch.size();
        processingTime += (endTime - startTime);
        
        // Clear processed data if in batch mode
        if (mode == AnalyticsMode.BATCH) {
            batch.clear();
        }
    }
    
    /**
     * Process streaming data with sliding window
     * @param sourceId Source device ID
     * @param newPoint New data point
     */
    private void processStreamingData(String sourceId, DataPoint newPoint) {
        List<DataPoint> stream = dataStore.get(sourceId);
        
        // Keep only windowSize most recent points
        if (stream.size() > windowSize) {
            stream = stream.subList(stream.size() - windowSize, stream.size());
            dataStore.put(sourceId, new ArrayList<>(stream));
        }
        
        long startTime = System.currentTimeMillis();
        
        // Apply streaming analytics
        // 1. Calculate moving average
        double movingAvg = stream.stream()
            .mapToDouble(DataPoint::getValue)
            .average()
            .orElse(0);
        
        // 2. Detect trend (simple linear regression slope)
        double slope = 0;
        if (stream.size() >= 3) {
            slope = calculateTrendSlope(stream);
        }
        
        // 3. Real-time anomaly detection
        boolean isAnomaly = false;
        if (stream.size() >= 5) {
            double stdDev = calculateStdDev(stream.subList(0, stream.size() - 1), movingAvg);
            isAnomaly = Math.abs(newPoint.getValue() - movingAvg) > 3 * stdDev;
        }
        
        // Update metrics
        long endTime = System.currentTimeMillis();
        processedDataPoints++;
        processingTime += (endTime - startTime);
        
        // Log anomalies
        if (isAnomaly) {
            Log.printLine("ANALYTICS: Anomaly detected in data from " + sourceId + 
                         " - value: " + newPoint.getValue() + 
                         " (avg: " + movingAvg + ")");
        }
    }
    
    /**
     * Aggregate all data across sources
     */
    private void aggregateAllData() {
        // Process any pending batches
        for (String sourceId : dataStore.keySet()) {
            if (!dataStore.get(sourceId).isEmpty()) {
                processBatch(sourceId);
            }
        }
        
        // Create global aggregation across all sources
        if (!aggregatedData.isEmpty()) {
            double globalAvg = aggregatedData.values().stream()
                .mapToDouble(AggregatedData::getAverage)
                .average()
                .orElse(0);
                
            int totalAnomalies = aggregatedData.values().stream()
                .mapToInt(AggregatedData::getAnomalyCount)
                .sum();
                
            Log.printLine("ANALYTICS: Global average: " + String.format("%.2f", globalAvg) + 
                         ", Anomalies: " + totalAnomalies);
        }
    }
    
    /**
     * Calculate standard deviation for a list of data points
     */
    private double calculateStdDev(List<DataPoint> data, double mean) {
        return Math.sqrt(
            data.stream()
                .mapToDouble(dp -> Math.pow(dp.getValue() - mean, 2))
                .average()
                .orElse(0)
        );
    }
    
    /**
     * Calculate trend slope using simple linear regression
     */
    private double calculateTrendSlope(List<DataPoint> data) {
        int n = data.size();
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = data.get(i).getValue();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
    }
    
    /**
     * Get processing efficiency (points per ms)
     */
    public double getProcessingEfficiency() {
        if (processingTime == 0) {
            return 0;
        }
        return processedDataPoints / processingTime;
    }
    
    /**
     * Get aggregated data for a source
     */
    public AggregatedData getAggregatedData(String sourceId) {
        return aggregatedData.getOrDefault(sourceId, null);
    }
    
    /**
     * Get all aggregated data
     */
    public Map<String, AggregatedData> getAllAggregatedData() {
        return new HashMap<>(aggregatedData);
    }
    
    /**
     * Reset analytics state
     */
    public void reset() {
        dataStore.clear();
        aggregatedData.clear();
        processedDataPoints = 0;
        totalDataPoints = 0;
        processingTime = 0;
        lastAggregationTime = 0;
    }
    
    /**
     * Class representing a single data point
     */
    public static class DataPoint {
        private String sourceId;
        private long timestamp;
        private double value;
        private Map<String, Object> metadata;
        
        public DataPoint(String sourceId, long timestamp, double value, Map<String, Object> metadata) {
            this.sourceId = sourceId;
            this.timestamp = timestamp;
            this.value = value;
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        }
        
        public String getSourceId() {
            return sourceId;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public double getValue() {
            return value;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }
    
    /**
     * Class representing aggregated data
     */
    public static class AggregatedData {
        private String sourceId;
        private long timestamp;
        private int count;
        private double min;
        private double max;
        private double average;
        private double stdDev;
        private int anomalyCount;
        
        public AggregatedData(String sourceId, long timestamp, int count, 
                             double min, double max, double average, 
                             double stdDev, int anomalyCount) {
            this.sourceId = sourceId;
            this.timestamp = timestamp;
            this.count = count;
            this.min = min;
            this.max = max;
            this.average = average;
            this.stdDev = stdDev;
            this.anomalyCount = anomalyCount;
        }
        
        public String getSourceId() {
            return sourceId;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public int getCount() {
            return count;
        }
        
        public double getMin() {
            return min;
        }
        
        public double getMax() {
            return max;
        }
        
        public double getAverage() {
            return average;
        }
        
        public double getStdDev() {
            return stdDev;
        }
        
        public int getAnomalyCount() {
            return anomalyCount;
        }
    }
}

package org.hyboff.simulation.security;

import org.hyboff.simulation.model.FogDevice;
import org.hyboff.simulation.model.Task;
import org.hyboff.simulation.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Security Manager for handling authentication, authorization and secure task offloading
 */
public class SecurityManager {
    
    // Authentication types
    public enum AuthType {
        NONE,           // No authentication
        BASIC,          // Basic authentication (username/password)
        TOKEN_BASED,    // Token based authentication
        CERTIFICATE     // Certificate based authentication
    }
    
    // Security levels
    public enum SecurityLevel {
        NONE,           // No security (0)
        LOW,            // Low security (1)
        MEDIUM,         // Medium security (2)
        HIGH            // High security (3)
    }
    
    private AuthType authType;
    private SecurityLevel securityLevel;
    private boolean encryptionEnabled;
    private boolean integrityCheckEnabled;
    
    // Device authentication status
    private Map<String, Boolean> authenticatedDevices;
    
    // Trust scores between devices (0-100)
    private Map<String, Map<String, Integer>> trustScores;
    
    // Security overhead factors
    private double authOverheadMs;         // Authentication overhead in ms
    private double encryptionOverheadFactor; // Encryption overhead factor (multiplier)
    private double integrityCheckOverheadMs; // Integrity check overhead in ms
    
    private Random random;
    
    /**
     * Create a security manager with specified security settings
     */
    public SecurityManager(AuthType authType, SecurityLevel securityLevel) {
        this.authType = authType;
        this.securityLevel = securityLevel;
        this.authenticatedDevices = new HashMap<>();
        this.trustScores = new HashMap<>();
        this.random = new Random(42); // Fixed seed for reproducibility
        
        // Set security features based on security level
        switch (securityLevel) {
            case NONE:
                this.encryptionEnabled = false;
                this.integrityCheckEnabled = false;
                break;
            case LOW:
                this.encryptionEnabled = false;
                this.integrityCheckEnabled = true;
                break;
            case MEDIUM:
                this.encryptionEnabled = true;
                this.integrityCheckEnabled = true;
                break;
            case HIGH:
                this.encryptionEnabled = true;
                this.integrityCheckEnabled = true;
                break;
        }
        
        // Set overhead values based on authentication type
        switch (authType) {
            case NONE:
                this.authOverheadMs = 0;
                break;
            case BASIC:
                this.authOverheadMs = 5;
                break;
            case TOKEN_BASED:
                this.authOverheadMs = 10;
                break;
            case CERTIFICATE:
                this.authOverheadMs = 20;
                break;
        }
        
        // Set encryption and integrity check overhead
        this.encryptionOverheadFactor = securityLevel.ordinal() * 0.1; // 0%, 10%, 20%, 30%
        this.integrityCheckOverheadMs = securityLevel.ordinal() * 2;   // 0ms, 2ms, 4ms, 6ms
    }
    
    /**
     * Authenticate a device
     * @param deviceId Device ID to authenticate
     * @return True if authentication successful
     */
    public boolean authenticateDevice(String deviceId) {
        if (authType == AuthType.NONE) {
            authenticatedDevices.put(deviceId, true);
            return true;
        }
        
        // Simulate authentication process with success probability
        double successProb = 0.99; // 99% success rate
        boolean success = random.nextDouble() < successProb;
        
        if (success) {
            authenticatedDevices.put(deviceId, true);
            return true;
        } else {
            authenticatedDevices.put(deviceId, false);
            return false;
        }
    }
    
    /**
     * Authenticate a device against another device
     * @param sourceId Source device ID
     * @param targetId Target device ID
     * @return True if authentication successful
     */
    public boolean authenticate(String sourceId, String targetId) {
        // If either ID is null, authentication fails
        if (sourceId == null || targetId == null) {
            // Disabled verbose debug logging
            // System.out.println("DEBUG: Authentication failed due to null ID");
            return false;
        }
        
        // For simulation purposes, always auto-authenticate all devices
        // This ensures the simulation can focus on resource allocation and scheduling
        if (!isAuthenticated(sourceId)) {
            authenticatedDevices.put(sourceId, true);
        }
        
        if (!isAuthenticated(targetId)) {
            authenticatedDevices.put(targetId, true);
        }
        
        return true; // Always authenticate for simulation purposes
    }
    
    /**
     * Get the current security level
     * @return The security level enum value
     */
    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }
    
    /**
     * Check if a device is authenticated
     * @param deviceId Device ID to check
     * @return True if device is authenticated
     */
    public boolean isAuthenticated(String deviceId) {
        return authenticatedDevices.getOrDefault(deviceId, false);
    }
    
    /**
     * Authorize task offloading between devices
     * @param sourceDevice Source device ID
     * @param targetDevice Target device ID
     * @param task Task to offload
     * @return True if authorized
     */
    public boolean authorizeTaskOffloading(String sourceDevice, String targetDevice, Task task) {
        // If security is disabled, always authorize
        if (securityLevel == SecurityLevel.NONE) {
            return true;
        }
        
        // Check if both devices are authenticated
        if (!isAuthenticated(sourceDevice) || !isAuthenticated(targetDevice)) {
            return false;
        }
        
        // Check trust score
        int trustScore = getTrustScore(sourceDevice, targetDevice);
        
        // Authorization based on security level and trust score
        switch (securityLevel) {
            case LOW:
                // Low security: Just need basic trust
                return trustScore >= 30;
            case MEDIUM:
                // Medium security: Need moderate trust
                return trustScore >= 50;
            case HIGH:
                // High security: Need high trust
                return trustScore >= 70;
            default:
                return true;
        }
    }
    
    /**
     * Get trust score between two devices
     * @param device1 First device ID
     * @param device2 Second device ID
     * @return Trust score (0-100)
     */
    public int getTrustScore(String device1, String device2) {
        // Get the trust map for device1
        Map<String, Integer> deviceTrust = trustScores.get(device1);
        if (deviceTrust == null) {
            deviceTrust = new HashMap<>();
            trustScores.put(device1, deviceTrust);
        }
        
        // Get trust score for device2
        Integer score = deviceTrust.get(device2);
        if (score == null) {
            // Initialize with a default trust score (50)
            score = 50;
            deviceTrust.put(device2, score);
        }
        
        return score;
    }
    
    /**
     * Update trust score between devices based on interaction result
     * @param device1 First device ID
     * @param device2 Second device ID
     * @param positive Whether the interaction was positive
     */
    public void updateTrustScore(String device1, String device2, boolean positive) {
        int currentScore = getTrustScore(device1, device2);
        int newScore;
        
        if (positive) {
            newScore = Math.min(100, currentScore + 5);
        } else {
            newScore = Math.max(0, currentScore - 10);
        }
        
        trustScores.get(device1).put(device2, newScore);
    }
    
    /**
     * Calculate security overhead for task offloading
     * @param task Task being offloaded
     * @return SecurityOverhead object with time and computational overhead
     */
    public SecurityOverhead calculateSecurityOverhead(Task task) {
        double timeOverhead = 0;
        double computationalOverhead = 0;
        
        // Authentication overhead
        timeOverhead += authOverheadMs;
        computationalOverhead += authOverheadMs * 0.2; // 20% of time is computational
        
        // Encryption overhead (if enabled)
        if (encryptionEnabled) {
            double encryptionTime = task.getSize() * encryptionOverheadFactor;
            timeOverhead += encryptionTime;
            computationalOverhead += encryptionTime * 0.5; // 50% of time is computational
        }
        
        // Integrity check overhead (if enabled)
        if (integrityCheckEnabled) {
            timeOverhead += integrityCheckOverheadMs;
            computationalOverhead += integrityCheckOverheadMs * 0.3; // 30% of time is computational
        }
        
        return new SecurityOverhead(timeOverhead, computationalOverhead);
    }
    
    /**
     * Calculate security overhead for task size in bytes
     * @param sizeInBytes Size of data in bytes
     * @return Security overhead in milliseconds
     */
    public double calculateSecurityOverhead(int sizeInBytes) {
        // Convert to a simpler format for direct calculation
        double timeOverhead = 0;
        
        // Authentication overhead
        timeOverhead += authOverheadMs;
        
        // Encryption overhead (if enabled)
        if (encryptionEnabled) {
            timeOverhead += sizeInBytes * encryptionOverheadFactor;
        }
        
        // Integrity check overhead (if enabled)
        if (integrityCheckEnabled) {
            timeOverhead += integrityCheckOverheadMs;
        }
        
        return timeOverhead;
    }
    
    /**
     * Secure a task for offloading (encrypt, sign, etc.)
     * @param task Task to secure
     * @param source Source device
     * @param target Target device
     * @return True if securing was successful
     */
    public boolean secureTaskForOffloading(Task task, FogDevice source, FogDevice target) {
        if (securityLevel == SecurityLevel.NONE) {
            return true; // No security needed
        }
        
        // Check authentication
        if (!isAuthenticated(source.getId()) || !isAuthenticated(target.getId())) {
            if (source.isLogEnabled()) {
                Log.printLine(source.getId() + ": Security error - devices not authenticated");
            }
            return false;
        }
        
        // Check authorization
        if (!authorizeTaskOffloading(source.getId(), target.getId(), task)) {
            if (source.isLogEnabled()) {
                Log.printLine(source.getId() + ": Security error - task offloading not authorized");
            }
            return false;
        }
        
        // Apply security overhead
        SecurityOverhead overhead = calculateSecurityOverhead(task);
        task.addResponseTime(overhead.getTimeOverhead());
        
        // Simulate encryption failure (very rare)
        if (encryptionEnabled && random.nextDouble() < 0.001) {
            if (source.isLogEnabled()) {
                Log.printLine(source.getId() + ": Security error - encryption failed");
            }
            return false;
        }
        
        // Simulate integrity check failure (rare)
        if (integrityCheckEnabled && random.nextDouble() < 0.005) {
            if (source.isLogEnabled()) {
                Log.printLine(source.getId() + ": Security error - integrity check failed");
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * Verify and process a secured task
     * @param task Received task
     * @param source Source device
     * @param target Target device
     * @return True if verification was successful
     */
    public boolean verifySecuredTask(Task task, FogDevice source, FogDevice target) {
        if (securityLevel == SecurityLevel.NONE) {
            return true; // No security verification needed
        }
        
        // Apply security overhead for verification
        SecurityOverhead overhead = calculateSecurityOverhead(task);
        task.addResponseTime(overhead.getTimeOverhead() * 0.7); // Verification is slightly faster than securing
        
        // Simulate verification failure (very rare)
        if (random.nextDouble() < 0.002) {
            if (target.isLogEnabled()) {
                Log.printLine(target.getId() + ": Security error - task verification failed");
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * Class to represent security overhead
     */
    public static class SecurityOverhead {
        private double timeOverhead;
        private double computationalOverhead;
        
        public SecurityOverhead(double timeOverhead, double computationalOverhead) {
            this.timeOverhead = timeOverhead;
            this.computationalOverhead = computationalOverhead;
        }
        
        public double getTimeOverhead() {
            return timeOverhead;
        }
        
        public double getComputationalOverhead() {
            return computationalOverhead;
        }
    }
}

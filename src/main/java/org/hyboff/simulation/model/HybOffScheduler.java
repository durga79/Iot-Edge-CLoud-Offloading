package org.hyboff.simulation.model;

import org.hyboff.simulation.util.Log;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * The Scheduler module of HybOff architecture.
 * Responsible for managing the task queue and scheduling tasks for execution
 * according to priority and resource availability.
 */
public class HybOffScheduler {
    private FogDevice fogDevice;
    private PriorityQueue<Task> taskQueue;
    private List<Task> executingTasks;
    private List<Task> completedTasks;
    
    // Task execution metrics
    private double totalResponseTime;
    private int totalExecutedTasks;
    private int totalFailedTasks;
    
    public HybOffScheduler(FogDevice fogDevice) {
        this.fogDevice = fogDevice;
        
        // Create a priority queue that prioritizes urgent tasks and earlier deadlines
        this.taskQueue = new PriorityQueue<>(new Comparator<Task>() {
            @Override
            public int compare(Task t1, Task t2) {
                // First compare by urgency
                if (t1.isUrgent() && !t2.isUrgent()) {
                    return -1;
                } else if (!t1.isUrgent() && t2.isUrgent()) {
                    return 1;
                } else {
                    // Then compare by deadline (earlier deadline first)
                    return Integer.compare(t1.getDeadline(), t2.getDeadline());
                }
            }
        });
        
        this.executingTasks = new ArrayList<>();
        this.completedTasks = new ArrayList<>();
        this.totalResponseTime = 0;
        this.totalExecutedTasks = 0;
        this.totalFailedTasks = 0;
    }
    
    /**
     * Add a task to the queue
     */
    public boolean addTask(Task task) {
        if (canAcceptTask(task)) {
            taskQueue.add(task);
            return true;
        }
        return false;
    }
    
    /**
     * Check if the fog device can accept a new task
     */
    public boolean canAcceptTask(Task task) {
        // Check if there is enough space in the queue
        int maxQueueSize = fogDevice.getMaxQueueSize();
        if (taskQueue.size() >= maxQueueSize) {
            // Disabled verbose debug logging
            // System.out.println("DEBUG: " + fogDevice.getId() + " queue full. Queue size: " + taskQueue.size() + ", Max size: " + maxQueueSize);
            return false;
        }
        
        // Check if there are enough resources available to eventually process the task
        // We only need to ensure the device has the total capacity to handle the task,
        // not that it has immediate resources available (tasks can be queued)
        int availableMips = fogDevice.getAvailableMips();
        int totalMips = fogDevice.getTotalMips();
        int totalExecutingTaskSize = 0;
        
        for (Task executingTask : executingTasks) {
            totalExecutingTaskSize += executingTask.getSize();
        }
        
        // Calculate total queued task size for debugging
        int totalQueuedTaskSize = 0;
        for (Task queuedTask : taskQueue) {
            totalQueuedTaskSize += queuedTask.getSize();
        }
        
        // More lenient check: Only verify that the task size is within the device's total capacity
        // This allows tasks to be queued even if resources aren't immediately available
        boolean hasEnoughResources = task.getSize() <= totalMips;
        
        // Disabled verbose debug logging
        // System.out.println("DEBUG: " + fogDevice.getId() + " cannot accept task " + task.getId() + 
        //                 ". Available MIPS: " + availableMips + 
        //                 ", Executing tasks MIPS: " + totalExecutingTaskSize + 
        //                 ", Queued tasks MIPS: " + totalQueuedTaskSize + 
        //                 ", New task size: " + task.getSize());
        
        return hasEnoughResources;
    }
    
    /**
     * Update task execution status
     */
    public void updateTaskExecution() {
        // Process completed tasks first
        List<Task> newlyCompleted = new ArrayList<>();
        for (Task task : executingTasks) {
            // Calculate a fair share of MIPS for each executing task
            // This ensures tasks make progress even when multiple tasks are executing
            int availableMipsPerTask = Math.max(1, fogDevice.getAvailableMips() / Math.max(1, executingTasks.size()));
            
            // Add some guaranteed progress to ensure tasks complete
            int progressMips = Math.max(100, availableMipsPerTask);
            
            // Update task progress with allocated MIPS
            task.updateProgress(progressMips);
            
            if (task.isCompleted()) {
                newlyCompleted.add(task);
                
                double responseTime = task.getResponseTime();
                totalResponseTime += responseTime;
                totalExecutedTasks++;
                
                // Release resources
                fogDevice.releaseResource(task.getSize());
                
                // Disabled verbose task completion messages
                // System.out.println(fogDevice.getId() + " completed task " + task.getId() + 
                //              " (response time: " + String.format("%.2f", responseTime) + " ms)");
            }
        }
        
        // Remove completed tasks from executing list and add to completed list
        executingTasks.removeAll(newlyCompleted);
        completedTasks.addAll(newlyCompleted);
        
        // Start new tasks if resources are available
        startNewTasks();
        
        // Update task deadlines and check for violations
        checkDeadlineViolations();
    }
    
    /**
     * Start executing new tasks if resources are available
     */
    private void startNewTasks() {
        // Get available resources
        int availableMips = fogDevice.getAvailableMips();
        
        // Try to schedule as many tasks as possible
        while (!taskQueue.isEmpty()) {
            Task nextTask = taskQueue.peek();
            
            // Check if we can start this task
            if (nextTask.getSize() <= availableMips) {
                taskQueue.poll();  // Remove from queue
                executingTasks.add(nextTask);
                
                // Update resource usage
                availableMips -= nextTask.getSize();
                fogDevice.allocateResource(nextTask.getSize());
                
                // Disabled verbose logging
                // if (fogDevice.isLogEnabled()) {
                //     Log.printLine(fogDevice.getId() + " started executing task " + nextTask.getId());
                // }
            } else {
                break;  // Not enough resources for next task
            }
        }
    }
    
    /**
     * Check for deadline violations in the queue
     */
    private void checkDeadlineViolations() {
        List<Task> deadlineViolated = new ArrayList<>();
        
        for (Task task : taskQueue) {
            task.decrementDeadline();
            
            if (task.getDeadline() <= 0) {
                deadlineViolated.add(task);
                totalFailedTasks++;
                
                if (fogDevice.isLogEnabled()) {
                    Log.printLine(fogDevice.getId() + " - Task " + task.getId() + " deadline violated");
                }
            }
        }
        
        // Remove tasks with violated deadlines
        taskQueue.removeAll(deadlineViolated);
    }
    
    /**
     * Get metrics about task processing
     */
    public TaskMetrics getMetrics() {
        TaskMetrics metrics = new TaskMetrics();
        metrics.queuedTasks = taskQueue.size();
        metrics.executingTasks = executingTasks.size();
        metrics.completedTasks = completedTasks.size();
        metrics.failedTasks = totalFailedTasks;
        metrics.averageResponseTime = totalExecutedTasks > 0 ? totalResponseTime / totalExecutedTasks : 0;
        return metrics;
    }
    
    /**
     * Get the current queue length
     */
    public int getQueueLength() {
        return taskQueue.size();
    }
    
    /**
     * Get the number of executing tasks
     */
    public int getExecutingTaskCount() {
        return executingTasks.size();
    }
    
    /**
     * Get the number of completed tasks
     */
    public int getCompletedTaskCount() {
        return completedTasks.size();
    }
    
    /**
     * Get the list of completed tasks
     */
    public List<Task> getCompletedTasks() {
        return new ArrayList<>(completedTasks);
    }
    
    /**
     * Add a task to the completed tasks list if it's not already there
     * @param task The task to add to completed tasks
     */
    public void addToCompletedTasks(Task task) {
        if (!completedTasks.contains(task)) {
            completedTasks.add(task);
            // Update metrics
            totalExecutedTasks++;
            totalResponseTime += task.getResponseTime();
        }
    }
    
    /**
     * Get the total number of failed tasks
     */
    public int getFailedTaskCount() {
        return totalFailedTasks;
    }
    
    /**
     * Get the total response time
     */
    public double getTotalResponseTime() {
        return totalResponseTime;
    }
    
    /**
     * Reset scheduler state
     */
    public void reset() {
        taskQueue.clear();
        executingTasks.clear();
        completedTasks.clear();
        totalResponseTime = 0;
        totalExecutedTasks = 0;
        totalFailedTasks = 0;
    }
    
    /**
     * Get all tasks managed by this scheduler (queued, executing, and completed)
     * @return List of all tasks
     */
    public List<Task> getAllTasks() {
        List<Task> allTasks = new ArrayList<>();
        allTasks.addAll(taskQueue);
        allTasks.addAll(executingTasks);
        allTasks.addAll(completedTasks);
        return allTasks;
    }
    
    /**
     * Class to represent task processing metrics
     */
    public static class TaskMetrics {
        public int queuedTasks;
        public int executingTasks;
        public int completedTasks;
        public int failedTasks;
        public double averageResponseTime;
    }
}

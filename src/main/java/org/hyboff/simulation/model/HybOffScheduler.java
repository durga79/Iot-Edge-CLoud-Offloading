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
            return false;
        }
        
        return true;
    }
    
    /**
     * Update task execution status
     */
    public void updateTaskExecution() {
        // Process completed tasks first
        List<Task> newlyCompleted = new ArrayList<>();
        for (Task task : executingTasks) {
            // Use the total MIPS capacity for task progress update
            // This is a simplification, assuming one task uses all available MIPS
            // A more sophisticated model would divide resources among tasks
            task.updateProgress(fogDevice.getTotalMips());
            
            if (task.isCompleted()) {
                newlyCompleted.add(task);
                
                double responseTime = task.getResponseTime();
                totalResponseTime += responseTime;
                totalExecutedTasks++;
                
                // Release resources
                fogDevice.releaseResource(task.getSize());
                
                if (fogDevice.isLogEnabled()) {
                    Log.printLine(fogDevice.getId() + " completed task " + task.getId() + 
                                 " (response time: " + String.format("%.2f", responseTime) + " ms)");
                }
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
                
                if (fogDevice.isLogEnabled()) {
                    Log.printLine(fogDevice.getId() + " started executing task " + nextTask.getId());
                }
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

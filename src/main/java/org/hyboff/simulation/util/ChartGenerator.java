package org.hyboff.simulation.util;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * Utility class to generate charts for simulation results visualization
 */
public class ChartGenerator {

    /**
     * Generate charts for the simulation results
     * 
     * @param resultsDir Directory containing CSV result files
     * @param taskDistribution Map of device IDs to task counts
     * @param deviceUtilization Map of device IDs to utilization percentages
     * @param deviceEnergy Map of device IDs to energy consumption
     */
    public static void generateCharts(String resultsDir, 
                                     Map<String, Integer> taskDistribution,
                                     Map<String, Double> deviceUtilization,
                                     Map<String, Double> deviceEnergy) {
        
        try {
            // Generate task distribution chart
            generateTaskDistributionChart(resultsDir, taskDistribution);
            
            // Generate utilization chart
            generateUtilizationChart(resultsDir, deviceUtilization);
            
            // Generate energy consumption chart
            generateEnergyChart(resultsDir, deviceEnergy);
            
            Log.printLine("Charts generated successfully in " + resultsDir);
        } catch (Exception e) {
            Log.printLine("Error generating charts: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Generate a bar chart showing task distribution across devices
     */
    private static void generateTaskDistributionChart(String resultsDir, Map<String, Integer> taskDistribution) 
            throws IOException {
        
        // Create dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        // Add data
        for (Map.Entry<String, Integer> entry : taskDistribution.entrySet()) {
            dataset.addValue(entry.getValue(), "Tasks", entry.getKey());
        }
        
        // Create chart
        JFreeChart chart = ChartFactory.createBarChart(
                "Task Distribution Across Devices",
                "Device",
                "Number of Tasks",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);
        
        // Customize chart
        customizeBarChart(chart);
        
        // Save chart
        ChartUtils.saveChartAsPNG(new File(resultsDir + File.separator + "task_distribution.png"), 
                chart, 800, 600);
    }
    
    /**
     * Generate a bar chart showing resource utilization across devices
     */
    private static void generateUtilizationChart(String resultsDir, Map<String, Double> deviceUtilization) 
            throws IOException {
        
        // Create dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        // Add data
        for (Map.Entry<String, Double> entry : deviceUtilization.entrySet()) {
            dataset.addValue(entry.getValue(), "Utilization (%)", entry.getKey());
        }
        
        // Create chart
        JFreeChart chart = ChartFactory.createBarChart(
                "Resource Utilization Across Devices",
                "Device",
                "Utilization (%)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);
        
        // Customize chart
        customizeBarChart(chart);
        
        // Save chart
        ChartUtils.saveChartAsPNG(new File(resultsDir + File.separator + "resource_utilization.png"), 
                chart, 800, 600);
    }
    
    /**
     * Generate a bar chart showing energy consumption across devices
     */
    private static void generateEnergyChart(String resultsDir, Map<String, Double> deviceEnergy) 
            throws IOException {
        
        // Create dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        // Add data
        for (Map.Entry<String, Double> entry : deviceEnergy.entrySet()) {
            dataset.addValue(entry.getValue(), "Energy Consumption", entry.getKey());
        }
        
        // Create chart
        JFreeChart chart = ChartFactory.createBarChart(
                "Energy Consumption Across Devices",
                "Device",
                "Energy Consumption (units)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);
        
        // Customize chart
        customizeBarChart(chart);
        
        // Save chart
        ChartUtils.saveChartAsPNG(new File(resultsDir + File.separator + "energy_consumption.png"), 
                chart, 800, 600);
    }
    
    /**
     * Apply common customization to bar charts
     */
    private static void customizeBarChart(JFreeChart chart) {
        CategoryPlot plot = chart.getCategoryPlot();
        
        // Set background color
        plot.setBackgroundPaint(Color.white);
        plot.setRangeGridlinePaint(Color.lightGray);
        
        // Customize category axis
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryMargin(0.2);
        
        // Customize number axis
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        
        // Customize bars
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);
        renderer.setMaximumBarWidth(0.1);
    }
}

package com.sanjana.trafficsignal;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        long seed = 42;
        double avgVehiclesPerMinute = 4;

        List<Vehicle> traffic = TrafficGenerator.generate(
                SimulationConfig.SIMULATION_DURATION, avgVehiclesPerMinute, seed);

        // Baseline: Hybrid controller
        MetricsCollector hybridResults = SimulationRunner.run(
                new HybridController(), traffic, SimulationConfig.SIMULATION_DURATION);
        hybridResults.printSummary("Hybrid Controller");

        // Sweep: Priority + Aging at different weights
        double[] weightsToTest = {0.0, 0.01, 0.02, 0.05, 0.1, 0.2, 0.5};
        for (double weight : weightsToTest) {
            MetricsCollector result = SimulationRunner.run(
                    new PriorityAgingController(weight), traffic, SimulationConfig.SIMULATION_DURATION);
            result.printSummary("Priority + Aging (agingWeight=" + weight + ")");
        }
    }
}
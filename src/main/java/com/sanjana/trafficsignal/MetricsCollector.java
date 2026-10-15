package com.sanjana.trafficsignal;

import java.util.EnumMap;
import java.util.Map;

public class MetricsCollector {

    private static final int STARVATION_THRESHOLD = 90; // seconds, vehicle-level wait threshold

    private int totalVehiclesCleared = 0;
    private long totalWaitTime = 0;
    private int maxWaitTime = 0;
    private int starvationEvents = 0;

    private final Map<Side, Integer> vehiclesClearedPerSide = new EnumMap<>(Side.class);
    private final Map<Side, Integer> maxWaitPerSide = new EnumMap<>(Side.class);

    // New: tracks the longest gap between consecutive turns, per side
    private final Map<Side, Integer> maxGapPerSide = new EnumMap<>(Side.class);
    private int overallMaxGap = 0;

    public MetricsCollector() {
        for (Side side : Side.values()) {
            vehiclesClearedPerSide.put(side, 0);
            maxWaitPerSide.put(side, 0);
            maxGapPerSide.put(side, 0);
        }
    }
 
    public int getTotalVehiclesCleared() {
        return totalVehiclesCleared;
    }

    public void recordVehicleCleared(Vehicle v, int currentTime) {
        int waitTime = v.getWaitTime(currentTime);
        totalVehiclesCleared++;
        totalWaitTime += waitTime;
        maxWaitTime = Math.max(maxWaitTime, waitTime);

        Side side = v.getSide();
        vehiclesClearedPerSide.put(side, vehiclesClearedPerSide.get(side) + 1);
        maxWaitPerSide.put(side, Math.max(maxWaitPerSide.get(side), waitTime));

        if (waitTime > STARVATION_THRESHOLD) {
            starvationEvents++;
        }
    }

    // New: called every time a side actually gets a turn (goes green)
    public void recordTurnGap(Side side, int gapSinceLastTurn) {
        maxGapPerSide.put(side, Math.max(maxGapPerSide.get(side), gapSinceLastTurn));
        overallMaxGap = Math.max(overallMaxGap, gapSinceLastTurn);
    }

    public double getAverageWaitTime() {
        return totalVehiclesCleared == 0 ? 0 : (double) totalWaitTime / totalVehiclesCleared;
    }

    public void printSummary(String label) {
        System.out.println("=== " + label + " ===");
        System.out.println("Vehicles cleared: " + totalVehiclesCleared);
        System.out.printf("Average wait time: %.1f sec%n", getAverageWaitTime());
        System.out.println("Max wait time: " + maxWaitTime + " sec");
        System.out.println("Starvation events (vehicle wait > " + STARVATION_THRESHOLD + "s): " + starvationEvents);
        System.out.println("Max gap between turns (any side): " + overallMaxGap + " sec");
        for (Side side : Side.values()) {
            System.out.println("  " + side + ": cleared=" + vehiclesClearedPerSide.get(side)
                    + ", maxWait=" + maxWaitPerSide.get(side)
                    + ", maxTurnGap=" + maxGapPerSide.get(side));
        }
        System.out.println();
    }
}
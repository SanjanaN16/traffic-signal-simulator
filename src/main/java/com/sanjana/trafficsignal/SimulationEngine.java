package com.sanjana.trafficsignal;

import java.util.ArrayList;
import java.util.List;

public class SimulationEngine {

    private final SignalController controller;
    private final Intersection intersection;
    private final MetricsCollector metrics;
    private final List<Vehicle> schedule;
    private final int durationSeconds;

    private int scheduleIndex = 0;
    private Side currentSide = null;
    private int phaseEndTime = 0;
    private int maxPhaseEndTime = 0;
    private boolean inClearance = false;
    private int clearanceEndTime = 0;
    private int lastDischargeTime = 0;

    public SimulationEngine(SignalController controller, List<Vehicle> allVehicles, int durationSeconds) {
        this.controller = controller;
        this.intersection = new Intersection();
        this.metrics = new MetricsCollector();
        this.durationSeconds = durationSeconds;

        this.schedule = new ArrayList<>(allVehicles);
        schedule.sort((a, b) -> Integer.compare(a.getDetectionTime(), b.getDetectionTime()));
    }

    // Advances the simulation by exactly one second. Call this repeatedly from a UI timer.
    public void step() {
        if (isFinished()) return;

        int currentTime = intersection.getCurrentTime();

        while (scheduleIndex < schedule.size() && schedule.get(scheduleIndex).getDetectionTime() == currentTime) {
            intersection.addVehicle(schedule.get(scheduleIndex));
            scheduleIndex++;
        }

        intersection.update(currentTime);

        if (currentSide == null && !inClearance) {
            currentSide = controller.decideNextSide(intersection);

            Lane chosenLane = intersection.getLane(currentSide);
            int gap = currentTime - chosenLane.getLastGreenTime();
            metrics.recordTurnGap(currentSide, gap);

            intersection.setCurrentGreenSide(currentSide);
            int duration = controller.getGreenDuration(intersection, currentSide);
            phaseEndTime = currentTime + duration;
            maxPhaseEndTime = currentTime + SimulationConfig.MAX_GREEN;
            lastDischargeTime = currentTime;

        } else if (currentSide != null) {
            Lane activeLane = intersection.getLane(currentSide);
            if (currentTime - lastDischargeTime >= SimulationConfig.DISCHARGE_INTERVAL && !activeLane.isEmpty()) {
                Vehicle cleared = activeLane.removeVehicle();
                metrics.recordVehicleCleared(cleared, currentTime);
                lastDischargeTime = currentTime;
            }

            boolean plannedTimeUp = currentTime >= phaseEndTime;
            boolean safeToEnd = controller.shouldEndGreen(intersection, currentSide, currentTime);
            boolean hardCapReached = currentTime >= maxPhaseEndTime;

            if ((plannedTimeUp && safeToEnd) || hardCapReached) {
                currentSide = null;
                intersection.setCurrentGreenSide(null);
                inClearance = true;
                clearanceEndTime = currentTime + SimulationConfig.CLEARANCE_TIME;
            }

        } else {
            if (currentTime >= clearanceEndTime) {
                inClearance = false;
            }
        }

        intersection.advanceTime(1);
    }

    public boolean isFinished() {
        return intersection.getCurrentTime() >= durationSeconds;
    }

    public Intersection getIntersection() {
        return intersection;
    }

    public MetricsCollector getMetrics() {
        return metrics;
    }
}
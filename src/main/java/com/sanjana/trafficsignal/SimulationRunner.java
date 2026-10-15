package com.sanjana.trafficsignal;

import java.util.ArrayList;
import java.util.List;

public class SimulationRunner {

    public static MetricsCollector run(SignalController controller, List<Vehicle> allVehicles, int durationSeconds) {
        Intersection intersection = new Intersection();
        MetricsCollector metrics = new MetricsCollector();

        List<Vehicle> schedule = new ArrayList<>(allVehicles);
        schedule.sort((a, b) -> Integer.compare(a.getDetectionTime(), b.getDetectionTime()));
        int scheduleIndex = 0;

        Side currentSide = null;
        int phaseEndTime = 0;
        int maxPhaseEndTime = 0;
        boolean inClearance = false;
        int clearanceEndTime = 0;
        int lastDischargeTime = 0;

        while (intersection.getCurrentTime() < durationSeconds) {

            int currentTime = intersection.getCurrentTime();

            while (scheduleIndex < schedule.size() && schedule.get(scheduleIndex).getDetectionTime() == currentTime) {
                intersection.addVehicle(schedule.get(scheduleIndex));
                scheduleIndex++;
            }

            intersection.update(currentTime);

            if (currentSide == null && !inClearance) {
                currentSide = controller.decideNextSide(intersection);

                // Record the gap BEFORE setCurrentGreenSide overwrites lastGreenTime
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

        return metrics;
    }
}
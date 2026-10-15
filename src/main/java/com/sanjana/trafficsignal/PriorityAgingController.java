package com.sanjana.trafficsignal;

public class PriorityAgingController implements SignalController {

    private static final double QUEUE_WEIGHT = 1.0;
    private final double agingWeight;

    public PriorityAgingController(double agingWeight) {
        this.agingWeight = agingWeight;
    }

    @Override
    public Side decideNextSide(Intersection intersection) {
        Side bestSide = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Side side : Side.values()) {
            Lane lane = intersection.getLane(side);

            if (lane.isCompletelyClear()) {
                continue;
            }

            double score = computePriorityScore(intersection, side);

            if (score > bestScore) {
                bestScore = score;
                bestSide = side;
            }
        }

        return (bestSide != null) ? bestSide : Side.values()[0];
    }

    private double computePriorityScore(Intersection intersection, Side side) {
        Lane lane = intersection.getLane(side);
        int queueLength = lane.getQueueLength();

        int currentTime = intersection.getCurrentTime();
        int waitedSinceLastGreen = currentTime - lane.getLastGreenTime();

        return (QUEUE_WEIGHT * queueLength) + (agingWeight * waitedSinceLastGreen);
    }

    @Override
    public int getGreenDuration(Intersection intersection, Side side) {
        int queueLength = intersection.getLane(side).getQueueLength();

        double ratio = Math.min(1.0, (double) queueLength / SimulationConfig.VEHICLES_FOR_MAX_GREEN);
        int duration = SimulationConfig.MIN_GREEN
                + (int) (ratio * (SimulationConfig.MAX_GREEN - SimulationConfig.MIN_GREEN));

        return duration;
    }
}
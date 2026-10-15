package com.sanjana.trafficsignal;

public class HybridController implements SignalController {

    private static final Side[] ROTATION = { Side.NORTH, Side.EAST, Side.SOUTH, Side.WEST };
    private int rotationIndex = 0;

    @Override
    public Side decideNextSide(Intersection intersection) {
        // Try each side in fixed rotation order, skipping any that are
        // completely clear (no vehicles waiting AND none approaching)
        for (int i = 0; i < ROTATION.length; i++) {
            Side candidate = ROTATION[rotationIndex];
            rotationIndex = (rotationIndex + 1) % ROTATION.length; // advance for next call

            if (!intersection.getLane(candidate).isCompletelyClear()) {
                return candidate;
            }
        }
        // all sides completely clear — nothing to do, just return next in order anyway
        return ROTATION[rotationIndex];
    }

    @Override
    public int getGreenDuration(Intersection intersection, Side side) {
        int queueLength = intersection.getLane(side).getQueueLength();

        // scale duration linearly between MIN_GREEN and MAX_GREEN based on queue length
        double ratio = Math.min(1.0, (double) queueLength / SimulationConfig.VEHICLES_FOR_MAX_GREEN);
        int duration = SimulationConfig.MIN_GREEN
                + (int) (ratio * (SimulationConfig.MAX_GREEN - SimulationConfig.MIN_GREEN));

        return duration;
    }

    // shouldEndGreen() is inherited as-is from the SignalController default method —
    // no override needed, since gap-out logic is shared across all algorithms
}
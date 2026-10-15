package com.sanjana.trafficsignal;

public interface SignalController {
	Side decideNextSide(Intersection intersection);
	
	int getGreenDuration(Intersection intersection, Side side);
	default boolean shouldEndGreen(Intersection intersection, Side side, int currentTime) {
	    Lane lane = intersection.getLane(side);
	    return lane.getGapSinceLastDetection(currentTime) >= SimulationConfig.GAP_THRESHOLD;
	}

}

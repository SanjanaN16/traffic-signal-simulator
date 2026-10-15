package com.sanjana.trafficsignal;
import java.util.EnumMap;
import java.util.Map;

public class Intersection {
	private final Map<Side,Lane> lanes;
	private Side currentGreenSide;
	private int currentTime;
	public Intersection() {
		this.lanes=new EnumMap<>(Side.class);
		for(Side side: Side.values()) {
			lanes.put(side,  new Lane(side));
		}
		this.currentTime=0;
		this.currentGreenSide=null;
	}
	// Adds a newly arrived vehicle to the correct lane based on its side
    public void addVehicle(Vehicle v) {
    	lanes.get(v.getSide()).detectVehicle(v);
    }
    public void update(int currentTime) {
        for (Lane lane : lanes.values()) {
            lane.update(currentTime);
        }
    }
    public Lane getLane(Side side) {
    	return lanes.get(side);
    }
    public Map<Side, Lane> getAllLanes() {
        return lanes;
    }
    public Side getCurrentGreenSide() {
        return currentGreenSide;
    }
    public void setCurrentGreenSide(Side side) {
    	this.currentGreenSide=side;
    	if(side!=null) {
    		lanes.get(side).setLastGreenTime(currentTime);
    	}
    	
    }
    public int getCurrentTime() {
        return currentTime;
    }

    public void advanceTime(int seconds) {
        this.currentTime += seconds;
    }

}

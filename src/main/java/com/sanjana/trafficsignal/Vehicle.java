package com.sanjana.trafficsignal;

public class Vehicle {
    private static int nextId = 1;

    private final int id;
    private final int detectionTime; // when it enters the 50m detection zone
    private final Side side;
    private final double speed;      // meters per second

    public Vehicle(int detectionTime, Side side) {
        this(detectionTime, side, SimulationConfig.DEFAULT_VEHICLE_SPEED);
    }

    public Vehicle(int detectionTime, Side side, double speed) {
        this.id = nextId++;
        this.detectionTime = detectionTime;
        this.side = side;
        this.speed = speed;
    }

    public int getId() { return id; }
    public int getDetectionTime() { return detectionTime; }
    public Side getSide() { return side; }

    // Seconds needed to travel from the detection point to the stop line
    public int getTravelTimeToStopLine() {
        return (int) Math.round(SimulationConfig.DETECTION_DISTANCE / speed);
    }

    // The simulation time this vehicle actually reaches the stop line
    public int getStopLineArrivalTime() {
        return detectionTime + getTravelTimeToStopLine();
    }

    public int getWaitTime(int currentTime) {
        return currentTime - getStopLineArrivalTime();
    }

    @Override
    public String toString() {
        return "Vehicle#" + id + " [side=" + side + ", detected=" + detectionTime
                + ", stopLineArrival=" + getStopLineArrivalTime() + "]";
    }
}
package com.sanjana.trafficsignal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Lane {

    private final Side side;
    private final Queue<Vehicle> vehicles;
    private final List<Vehicle> pendingVehicles;
    private int lastGreenTime;
    private int lastDetectionTime;

    public Lane(Side side) {
        this.side = side;
        this.vehicles = new LinkedList<>();
        this.pendingVehicles = new ArrayList<>();
        this.lastGreenTime = 0;
        this.lastDetectionTime = -1;
    }

    public void detectVehicle(Vehicle v) {
        pendingVehicles.add(v);
        lastDetectionTime = v.getDetectionTime();
    }

    public void update(int currentTime) {
        List<Vehicle> arrived = new ArrayList<>();
        for (Vehicle v : pendingVehicles) {
            if (v.getStopLineArrivalTime() <= currentTime) {
                arrived.add(v);
            }
        }
        for (Vehicle v : arrived) {
            pendingVehicles.remove(v);
            vehicles.offer(v);
        }
    }

    public Vehicle removeVehicle() {
        return vehicles.poll();
    }

    public int getQueueLength() {
        return vehicles.size();
    }

    public boolean isEmpty() {
        return vehicles.isEmpty();
    }

    public boolean isCompletelyClear() {
        return vehicles.isEmpty() && pendingVehicles.isEmpty();
    }

    public int getLongestWaitTime(int currentTime) {
        Vehicle front = vehicles.peek();
        return (front == null) ? 0 : front.getWaitTime(currentTime);
    }

    public int getGapSinceLastDetection(int currentTime) {
        if (lastDetectionTime == -1) return Integer.MAX_VALUE;
        return currentTime - lastDetectionTime;
    }

    // New: gives the UI a read-only snapshot of currently queued vehicles,
    // so it can display each one individually (e.g. color-coded by wait time)
    public List<Vehicle> getVehiclesSnapshot() {
        return new ArrayList<>(vehicles);
    }

    public Side getSide() { return side; }
    public int getLastGreenTime() { return lastGreenTime; }
    public void setLastGreenTime(int time) { this.lastGreenTime = time; }

    @Override
    public String toString() {
        return side + " [" + getQueueLength() + " waiting, " + pendingVehicles.size() + " approaching]";
    }
}
package com.sanjana.trafficsignal;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TrafficGenerator {

    // Generates a fixed traffic pattern using a seeded random generator,
    // so both controllers are tested against IDENTICAL arrivals — a fair comparison.
    public static List<Vehicle> generate(int durationSeconds, double avgVehiclesPerMinutePerSide, long seed) {
        Random random = new Random(seed);
        List<Vehicle> vehicles = new ArrayList<>();
        double avgGapSeconds = 60.0 / avgVehiclesPerMinutePerSide;

        for (Side side : Side.values()) {
            int time = 0;
            while (time < durationSeconds) {
                // exponential inter-arrival time approximates real-world (Poisson) traffic arrivals
                double gap = -Math.log(1 - random.nextDouble()) * avgGapSeconds;
                time += (int) Math.round(gap);
                if (time < durationSeconds) {
                    vehicles.add(new Vehicle(time, side));
                }
            }
        }
        return vehicles;
    }
}
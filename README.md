# Traffic Signal Simulation: Round Robin vs Priority Scheduling with Aging

A Java simulation of a four-way intersection that compares two signal-control strategies on identical traffic: a fixed-order controller (Round Robin with variable green time) and a Priority-with-Aging controller. It includes a live JavaFX visualization.



## The problem

A fixed-time signal gives every side the same green time, whether or not anyone is waiting. A side with no vehicles can hold the green while the other three sides are crowded. This project explores signal control that responds to demand, and measures the trade-off between efficiency and fairness when it does.

## Controllers

Both controllers implement a common `SignalController` interface (Strategy pattern), so the same simulation engine runs either one.

### Hybrid (Round Robin with a variable quantum)
- Sides are served in a fixed order: North, East, South, West.
- A side with nothing queued and nothing approaching is skipped.
- Green duration scales linearly with queue length, between `MIN_GREEN` (10 s) and `MAX_GREEN` (40 s).
- Every side is guaranteed a turn each rotation, so no side can starve.

### Priority + Aging
- Each non-empty side gets a score: `score = queueLength + agingWeight * secondsSinceLastGreen`
- The highest score goes next. The aging term grows while a side waits, which is the standard fix for starvation in priority scheduling.
- Uses the same green-duration formula as Hybrid, so the only difference between the two is how the next side is chosen.

## OS concepts used

| Concept | Where |
|---|---|
| Round Robin scheduling | `HybridController`: fixed rotation over sides |
| Weighted time quantum | `getGreenDuration()`: quantum varies with demand |
| Priority scheduling | `PriorityAgingController.decideNextSide()` |
| Aging (starvation prevention) | `agingWeight * secondsSinceLastGreen` in the priority score |
| Dynamic time-slice extension with a hard cap | `shouldEndGreen()` and `MAX_GREEN` in the runner |
| Strategy pattern | `SignalController` with interchangeable implementations |

For context, real adaptive systems such as C-DAC's CoSiCoSt keep the phase order fixed and adjust green durations. The Hybrid controller follows the same idea, while Priority + Aging deliberately explores the less predictable alternative.

## Realism features (shared by both controllers)

- **Advance detection:** vehicles are detected 50 m before the stop line and move from an "approaching" list into the queue once they would have reached it. A side with an approaching vehicle is not treated as empty.
- **Gap-out:** a green phase is not cut off while a vehicle was detected in the last 4 seconds, so a car close to the stop line is not caught by a sudden change. The phase is still capped at `MAX_GREEN`.
- **Clearance interval:** a 3-second all-red buffer sits between phases.
- **Controlled comparison:** traffic comes from a seeded generator (exponential inter-arrival times, per side), so every controller sees exactly the same vehicles.

## Metrics

- Vehicles cleared
- Average and maximum vehicle wait (from stop-line arrival to discharge)
- Vehicles that waited more than 90 s (a threshold I chose, not a standard)
- **Maximum gap between turns** for each side: the longest time a side went without a green

The last two measure different things. Vehicle wait includes time spent queued behind other cars on the same side, while the turn gap measures only whether a side was ignored. Aging influences the second directly and the first only indirectly.

## Results

Setup: 4 vehicles per minute per side, 1 simulated hour, seed 42. All runs cleared about 926 vehicles.

| Controller | Avg wait (s) | Max wait (s) | Waits > 90 s | Max turn gap (s) |
|---|---|---|---|---|
| Hybrid | 59.0 | 144 | 165 | 139 |
| Priority + Aging, weight 0.0 | 53.2 | 185 | 127 | 187 |
| Priority + Aging, weight 0.05 | 56.4 | 169 | 141 | 177 |
| Priority + Aging, weight 0.1 | 56.0 | 151 | 143 | 170 |
| Priority + Aging, weight 0.2 | 57.1 | 152 | 149 | 142 |
| Priority + Aging, weight 0.5 | 57.0 | 147 | 154 | 152 |

What the data shows:

1. **Two kinds of fairness trade off.** As the aging weight rises from 0 to 0.2, the worst-case gap between turns falls from 187 s to 142 s, close to Hybrid's 139 s. Over the same range, the number of vehicles waiting more than 90 s rises from 127 to 149, because the controller spends time on quiet sides while a busy queue grows.
2. **Average wait differences are small** (53 to 59 s). These come from a single seed, so I treat them as indicative rather than conclusive.
3. **Hybrid has the lowest worst-case vehicle wait** (144 s) in this run, and a turn gap that the fixed order guarantees by construction.
4. **Scheduling only matters near capacity.** At 6 vehicles/min per side the intersection is oversaturated, queues grow without bound, and both controllers perform equally badly (average wait around 490 s). At 2 vehicles/min per side there is rarely more than one non-empty side, so there is little to decide.

## Bugs and lessons

- **Two clocks:** the runner kept its own time variable while `Intersection` kept another, frozen at 0. The aging term was always zero and changing the weight had no effect. Fixed by making `Intersection` the single owner of simulation time.
- **Unit mismatch:** `queueLength` is a count of vehicles and `secondsSinceLastGreen` is in seconds, so any aging weight of 0.5 or more let aging swamp the queue term. Identical results at weights 0.5 to 3.0 revealed this, and the useful range is roughly 0 to 0.5. Normalizing both terms to a common scale would be a cleaner fix.
- **Measuring the right thing:** the vehicle-wait metric alone made aging look harmful. Adding the per-side turn-gap metric showed it was working as designed.

## Project structure

```
src/main/java/com/sanjana/trafficsignal/
  Side, Vehicle, Lane, Intersection     core model
  SignalController                      strategy interface (+ default gap-out rule)
  HybridController                      Round Robin, variable green
  PriorityAgingController               priority scheduling with aging
  SimulationConfig                      shared timing constants
  TrafficGenerator                      seeded arrival generator
  MetricsCollector                      wait, throughput and turn-gap metrics
  SimulationRunner                      runs a full simulation, returns metrics
  SimulationEngine                      same logic as a steppable engine for the UI
  Main                                  console comparison / aging-weight sweep
  TrafficSignalApp                      JavaFX visualization
```

## Running it

Requirements: JDK 17 and Maven.

```
git clone https://github.com/SanjanaN16/traffic-signal-simulator.git
cd traffic-signal-simulator
```

**Console comparison** (prints metrics for Hybrid and a sweep of aging weights):

```
mvn compile
java -cp target/classes com.sanjana.trafficsignal.Main
```

Change `avgVehiclesPerMinute` and `seed` in `Main.java` to try other traffic levels.

**JavaFX visualization:**

```
mvn javafx:run
```

Pick a controller and press Start. Each side shows its queue as small cars colored by how long that car has waited: green under 20 s, yellow under 50 s, red beyond that. The visualization uses 4 vehicles/min per side, seed 42, and an aging weight of 0.2. A full simulated hour takes several minutes at the default speed, so use Stop whenever you like.

### Default configuration

| Constant | Value |
|---|---|
| `MIN_GREEN` / `MAX_GREEN` | 10 s / 40 s |
| `VEHICLES_FOR_MAX_GREEN` | 15 |
| `GAP_THRESHOLD` | 4 s |
| `CLEARANCE_TIME` | 3 s |
| `DETECTION_DISTANCE` | 50 m |
| `DEFAULT_VEHICLE_SPEED` | 11 m/s (about 40 km/h) |
| `DISCHARGE_INTERVAL` | 3 s per vehicle |
| `SIMULATION_DURATION` | 3600 s |

## Limitations

- **Single seed:** the reported results come from one traffic pattern (seed 42). Differences between controllers are small enough that a multi-seed run is needed before drawing firm conclusions.
- **Flat discharge rate:** each vehicle takes a fixed 3 s to clear. Real queues have a start-up delay for the first vehicles and settle near 2 s per vehicle.
- One lane per side, fixed vehicle speed, no turning movements, no pedestrians, and one side green at a time.
- The 90-second "starvation" threshold and the default timing constants are my own choices, not calibrated to a real intersection.
- Arrivals are a simple Poisson process, with no rush-hour variation.

## Future work

- Run many seeds and report the mean and spread of each metric.
- Add a start-up lost time model for queue discharge.
- Normalize the queue and aging terms, then re-run the weight sweep.
- Time-varying demand over a full day.
- Per-side charts in the UI.

## Author

Sanjana N, Information Science and Engineering, The National Institute of Engineering, Mysuru.

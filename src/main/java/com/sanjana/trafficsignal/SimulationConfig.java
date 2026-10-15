package com.sanjana.trafficsignal;

public class SimulationConfig {
	
	
	    public static final int MIN_GREEN = 10;          // seconds, minimum green time
	    public static final int MAX_GREEN = 40;          // seconds, maximum green time
	    public static final int VEHICLES_FOR_MAX_GREEN = 15;

	    public static final int GAP_THRESHOLD = 4;        // seconds of silence before ending green early
	    public static final int CLEARANCE_TIME = 3;  
	    public static final int DETECTION_DISTANCE = 50;       // meters upstream of stop line
	    public static final double DEFAULT_VEHICLE_SPEED = 11; 
	    public static final int DISCHARGE_INTERVAL = 3;     // seconds to clear one vehicle during green
	    public static final int SIMULATION_DURATION = 3600; //// yellow + all-red buffer between phases
	}



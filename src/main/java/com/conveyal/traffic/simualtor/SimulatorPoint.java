package com.conveyal.traffic.simualtor;

import com.conveyal.traffic.geom.Crossing;
import com.conveyal.traffic.geom.GPSPoint;
import com.conveyal.traffic.geom.TripLine;
import com.conveyal.traffic.stats.SpeedSample;
import com.vividsolutions.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.List;

public class SimulatorPoint {

    public GPSPoint gpsPoint;
    public SimulatorGpsSegment gpsSegment;
    public List<SpeedSample> speedSamples = new ArrayList<>();
    public List<SimulatorTripLine> tripLines = new ArrayList<>();
    public List<SimulatorCrossing> crossings = new ArrayList<>();
    public List<SimulatorCrossing> pendingCrossings = new ArrayList<>();

    public SimulatorPoint() {

    }

}

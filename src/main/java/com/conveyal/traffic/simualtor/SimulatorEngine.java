package com.conveyal.traffic.simualtor;

import com.conveyal.osmlib.OSM;
import com.conveyal.traffic.TrafficEngine;
import com.conveyal.traffic.geom.Crossing;
import com.conveyal.traffic.geom.GPSPoint;
import com.conveyal.traffic.geom.TripLine;
import com.conveyal.traffic.stats.SpeedSample;
import com.google.common.io.ByteStreams;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SimulatorEngine {

	private static final Logger log = Logger.getLogger( SimulatorEngine.class.getName());

	private TrafficEngine te;


    public SimulatorEngine() {

    	File dataCache = new File("data/cache");
    	dataCache.mkdirs();
    	
    	te = new TrafficEngine(new File(SimulatorApp.appProps.getProperty("application.data.cacheDirectory")), new File(SimulatorApp.appProps.getProperty("application.data.osmDirectory")), SimulatorApp.appProps.getProperty("application.vex"), true);
    }
    
    public TrafficEngine getTrafficEngine() { 
    	return te;
    }
    

    public SimulatorPoint simulate(GPSPoint gpsPoint) {

    	te.checkOsm(gpsPoint.lat, gpsPoint.lon);

		List<SpeedSample> speedSamples = te.updateAndGetSample(gpsPoint);

		SimulatorPoint simulatorPoint = new SimulatorPoint();

		simulatorPoint.gpsPoint = gpsPoint;

		if(speedSamples != null) {
			for(SpeedSample  sample : speedSamples) {
				simulatorPoint.speedSamples.add(new SimulatorSpeedSample(sample, (LineString)this.te.getStreetSegmentsById(sample.getSegmentId()).geometry));
			}

		}

		simulatorPoint.gpsSegment = new SimulatorGpsSegment(te.getDebugGpsSegment());

		List<Crossing> crossings = te.getDebugCrossings();
		if(crossings != null) {
			for(Crossing crossing : crossings) {
				simulatorPoint.crossings.add(new SimulatorCrossing(crossing));
			}

		}

		List<Crossing> pendingCrossings = te.getDebugPendingCrossings();
		if(pendingCrossings != null) {
			for(Crossing crossing : pendingCrossings) {
				simulatorPoint.pendingCrossings.add(new SimulatorCrossing(crossing));
			}

		}

		List<TripLine> tripLines = te.getDebugTripLine();
		if(tripLines != null) {
			for(TripLine tripLine : tripLines) {
				simulatorPoint.tripLines.add(new SimulatorTripLine(tripLine));
			}

		}

		return simulatorPoint;
    }



}

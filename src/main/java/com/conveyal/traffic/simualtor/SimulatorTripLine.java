package com.conveyal.traffic.simualtor;

import com.conveyal.traffic.geom.Crossing;
import com.conveyal.traffic.geom.TripLine;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;

/**
 * Created by kpw on 6/14/15.
 */
public class SimulatorTripLine {

    public String tripline;

    public SimulatorTripLine(TripLine tripLine) {
        EncodedPolylineBean encodedPolyline = PolylineEncoder.createEncodings(tripLine.geometry);
        tripline = encodedPolyline.getPoints();
    }
}

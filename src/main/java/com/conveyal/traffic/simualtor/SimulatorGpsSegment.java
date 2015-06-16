package com.conveyal.traffic.simualtor;

import com.conveyal.traffic.geom.GPSSegment;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;


public class SimulatorGpsSegment {

    public String segment;

    public SimulatorGpsSegment(GPSSegment debugGpsSegment) {
        if(debugGpsSegment != null && debugGpsSegment.geom != null) {
            EncodedPolylineBean encodedPolyline = PolylineEncoder.createEncodings(debugGpsSegment.geom);
            segment = encodedPolyline.getPoints();
        }
    }
}

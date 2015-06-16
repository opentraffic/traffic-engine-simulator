package com.conveyal.traffic.simualtor;

import com.conveyal.traffic.stats.SpeedSample;
import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.geocoder.google.Geometry;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;

/**
 * Created by kpw on 6/15/15.
 */
public class SimulatorSpeedSample extends SpeedSample {

    public String segmentGeom;

    public SimulatorSpeedSample (SpeedSample sample, LineString geometry) {
        super(sample.getTime(), sample.getSegmentId(), sample.getSpeed());

        EncodedPolylineBean encodedPolyline = PolylineEncoder.createEncodings(geometry);
        segmentGeom = encodedPolyline.getPoints();
    }

}

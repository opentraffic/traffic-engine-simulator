package com.conveyal.traffic.simualtor;

import com.conveyal.traffic.geom.Crossing;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;

/**
 * Created by kpw on 6/14/15.
 */
public class SimulatorCrossing {


    public String tripline;

    public SimulatorCrossing(Crossing crossing) {
        EncodedPolylineBean encodedPolyline = PolylineEncoder.createEncodings(crossing.tripline.geometry);
        tripline = encodedPolyline.getPoints();

    }
}

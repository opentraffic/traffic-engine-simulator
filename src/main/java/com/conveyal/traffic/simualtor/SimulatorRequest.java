package com.conveyal.traffic.simualtor;

import com.conveyal.traffic.geom.GPSPoint;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import org.geotools.factory.Hints;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class SimulatorRequest {

    long vehicleId = UUID.randomUUID().getLeastSignificantBits();

    private SimulatorEngine simluator = new SimulatorEngine();

    public String path;
    public List<SimulatorPoint> points = new ArrayList<>();

    List<StreetEdge> edges;

    public SimulatorRequest(Coordinate fromCoord, Coordinate toCoord, int error, int speed, int frequency) throws Exception {

        final CRSAuthorityFactory crsAuthorityFactory = CRS.getAuthorityFactory(false);
        CoordinateReferenceSystem targetCRS = crsAuthorityFactory.createCoordinateReferenceSystem("EPSG:" + getEPSGCodefromUTS(fromCoord));

        Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
        CRSAuthorityFactory factory = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG", hints);
        CoordinateReferenceSystem sourceCRS = factory.createCoordinateReferenceSystem("EPSG:4326");

        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);

        edges = SimulatorApp.routing.getEdges(fromCoord, toCoord);

        List<Coordinate> coords = new ArrayList<>();

        for(StreetEdge edge :  edges) {
            boolean firstPointForEdge = true;
            for(Coordinate coord : edge.getGeometry().getCoordinates()) {
                // skip first point of subsequent lines b/c start is coincident with end of last segment
                if(!firstPointForEdge || coords.size() == 0)
                    coords.add(coord);
                firstPointForEdge = false;
            }
        }

        Coordinate coordArray[] = coords.toArray(new Coordinate[coords.size()]);

        GeometryFactory gf = new GeometryFactory();
        LineString lineString = gf.createLineString(coordArray);

        // create encoded polyline
        EncodedPolylineBean encodedPolyline = PolylineEncoder.createEncodings(lineString);
        path = encodedPolyline.getPoints();

        LineString projectedLineString = (LineString)JTS.transform(lineString, transform);

        double pathLength = projectedLineString.getLength();

        double pathTime = pathLength / (speed * 0.277778);

        double numberSamples = pathTime / (frequency * 1.0);

        double distancePerSample = pathLength / numberSamples;

        LengthIndexedLine lengthIndexedLine = new LengthIndexedLine(projectedLineString);

        List<Coordinate>  simulatedCoords = new ArrayList<>();

        for(double d = 0; d <= pathLength; d = d + distancePerSample) {
            Coordinate projectedSimulatorCoordinate = lengthIndexedLine.extractPoint(d);
            Coordinate simulatorCoordinate =  JTS.transform(new Coordinate(projectedSimulatorCoordinate.x, projectedSimulatorCoordinate.y), null, transform.inverse());
            simulatedCoords.add(simulatorCoordinate);
        }

        for(int i = 0; i < simulatedCoords.size(); i++) {
            Coordinate coord = simulatedCoords.get(i);
            GPSPoint gpsPoint = new GPSPoint((long)((pathTime / numberSamples) * i * 1000000), vehicleId, coord.x, coord.y);

            SimulatorPoint simulatorPoint = this.simluator.simulate(gpsPoint);

            points.add(simulatorPoint);
        }
    }


    public static int getEPSGCodefromUTS(Coordinate refLonLat) {
        // define base EPSG code value of all UTM zones;
        int epsg_code = 32600;
        // add 100 for all zones in southern hemisphere
        if (refLonLat.x < 0) {
            epsg_code += 100;
        }
        // finally, add zone number to code
        epsg_code += getUTMZoneForLongitude(refLonLat.y);

        return epsg_code;
    }

    public static int getUTMZoneForLongitude(double lon) {

        if (lon < -180 || lon > 180)
            throw new IllegalArgumentException(
                    "Coordinates not within UTM zone limits");

        int lonZone = (int) ((lon + 180) / 6);

        if (lonZone == 60)
            lonZone--;
        return lonZone + 1;
    }
}

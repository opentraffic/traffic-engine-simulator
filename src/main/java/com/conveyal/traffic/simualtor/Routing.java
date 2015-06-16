package com.conveyal.traffic.simualtor;

import com.google.common.collect.Lists;

import com.vividsolutions.jts.geom.Coordinate;

import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.analyst.core.SlippyTile;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraversalRequirements;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.CandidateEdgeBundle;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.standalone.Router;


import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Routing with OpenTraffic data in a single area of the world.
 */
public class Routing {
	private static final Logger log = Logger.getLogger( Routing.class.getName());
	
	/** The graph to use for routing. */
    private Router graph;

    /** The graph path finder for the above graph */
    private GraphPathFinder gpf;

    /** The bounding box for this graph */
    private Rectangle boundingBox;

    /** Create a new router with the given bounding box */
    public Routing (Rectangle boundingBox) {
        this.boundingBox = boundingBox;
    }

    private boolean unbuilt = true;

    /** Get a trip plan, or null if the graph is not yet built */
    public TripPlan route (RoutingRequest request) {
        if (graph == null)
            return null;

        List<GraphPath> paths = gpf.graphPathFinderEntryPoint(request);
        
        for(GraphPath p : paths) {
       
        }
        return GraphPathToTripPlanConverter.generatePlan(paths, request);
    }

    public List<StreetEdge> getEdges(Coordinate fromCoord, Coordinate toCoord) {

        TraverseModeSet modes = new TraverseModeSet(TraverseMode.CAR);
        final RoutingRequest options = new RoutingRequest(modes);;
        TraversalRequirements reqs = new TraversalRequirements(options);

        GenericLocation fromLocation = new GenericLocation(new Coordinate(fromCoord.y, fromCoord.x));
        GenericLocation toLocation = new GenericLocation(new Coordinate(toCoord.y, toCoord.x));

        CandidateEdgeBundle fromEdges = graph.graph.streetIndex.getClosestEdges(fromLocation, reqs, null, null, false);
        CandidateEdgeBundle toEdges = graph.graph.streetIndex.getClosestEdges(toLocation, reqs, null, null, false);

        AStar astar = new AStar();
        final RoutingRequest req = new RoutingRequest(modes);

        final Vertex fromVertex = fromEdges.best.edge.getFromVertex();
        final Vertex toVertex = toEdges.best.edge.getFromVertex();

        req.setRoutingContext(graph.graph, fromVertex, toVertex);

        ShortestPathTree tree = astar.getShortestPathTree(req);
        GraphPath result = tree.getPaths().get(0);

        List<StreetEdge> edges = new ArrayList<>();

        for (Edge edge : result.edges) {
            if(edge instanceof StreetEdge) {
                StreetEdge streetEdge  = (StreetEdge)edge;
                edges.add(streetEdge);
            }
        }

        return edges;
    }

    public void buildIfUnbuilt () {
        if (unbuilt) build();
    }

    /** (Re)-build the graph (async method) */
    public void build () {
        unbuilt = false;
        new BuildGraphs(this).run();

    }

    /** Build graphs so we can do routing with traffic data */
    public static class BuildGraphs implements Runnable {
        private final Routing routing;

        public BuildGraphs (Routing routing) {
            this.routing = routing;
        }

        @Override
        public void run() {
            // gather input files
            List<File> infiles = Lists.newArrayList();

            // find all the OSM files we need
            File cacheDir = new File(SimulatorApp.appProps.getProperty("application.data.osmDirectory"));

            Z: for (File zfile : cacheDir.listFiles()) {
                int z = Integer.parseInt(zfile.getName());
                X: for (File xfile : zfile.listFiles()) {
                    int x = Integer.parseInt(xfile.getName());

                    log.log(Level.INFO, "x: " + x);

                    double west = SlippyTile.tile2lon(x, z);
                    double east = SlippyTile.tile2lon(x + 1, z);

                    // check if this could possibly contain the right file
                    if (east < routing.boundingBox.getMinX() || west > routing.boundingBox.getMaxX())
                        continue X;

                    Y: for (File yfile : xfile.listFiles()) {
                        int y = Integer.parseInt(yfile.getName().replace(".osm.pbf", ""));

                        double north = SlippyTile.tile2lat(y, z);
                        double south = SlippyTile.tile2lat(y + 1, z);

                        log.log(Level.INFO, "y: " + y + ", n: " + north + ", s: " + south);

                        log.log(Level.INFO, "min: " + routing.boundingBox.getMinY() + ", max: " + routing.boundingBox.getMaxY());

                        if (north < routing.boundingBox.getMinY() || south > routing.boundingBox.getMaxY())
                            continue Y;

                        infiles.add(yfile);
                    }
                }
            }

            log.log(Level.INFO, "Using " + infiles.size() + " tiles for graph");

            // phew. having figured out what tiles we need now build an OTP graph.
            // note we do not configure an updater: we don't need one, as we do the updates
            // ourselves.
            GraphBuilder gb = new GraphBuilder();
            OpenStreetMapModule osm = new OpenStreetMapModule();
            osm.setProviders(infiles.stream()
                            .map(osmFile -> new AnyFileBasedOpenStreetMapProviderImpl(osmFile))
                            .collect(Collectors.toList()));
            gb.addModule(osm);
            gb.serializeGraph = false;
            gb.run();
            Graph g = gb.getGraph();
            Router r = new Router("default", g);
            GraphPathFinder gpf = new GraphPathFinder(r);

            g.index(new DefaultStreetVertexIndexFactory());

            synchronized (routing) {
                routing.graph = r;
                routing.gpf = gpf;
            }

            log.log(Level.INFO, "graph built");
        }
    }
}

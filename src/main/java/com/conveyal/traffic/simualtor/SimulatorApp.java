package com.conveyal.traffic.simualtor;

import java.awt.Rectangle;
import java.io.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.conveyal.osmlib.OSM;
import com.google.common.io.ByteStreams;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import com.fasterxml.jackson.databind.ObjectMapper;

import static spark.Spark.*;

public class SimulatorApp {
	
	private static final Logger log = Logger.getLogger( SimulatorApp.class.getName());
	
	private static final ObjectMapper mapper = new ObjectMapper();
	
	public static Routing routing = new Routing(new Rectangle(-180, -90, 360, 180));
	
	public static Properties appProps = new Properties();

	private static Quadtree osmBoundsIndex = new Quadtree();

	public static void main(String[] args) {

		// load settings file
		loadSettings();

		// setup public folder
		staticFileLocation("/public");

		// routing requests 

		get("/route", (request, response) -> {

			response.header("Access-Control-Allow-Origin", "*");

			double fromLat = request.queryMap("fromLat").doubleValue();
			double fromLon = request.queryMap("fromLon").doubleValue();
			double toLat = request.queryMap("toLat").doubleValue();
			double toLon = request.queryMap("toLon").doubleValue();
			int frequency = request.queryMap("frequency").integerValue();
			int error = request.queryMap("error").integerValue();
			int speed = request.queryMap("speed").integerValue();

			checkOsm(fromLat, fromLon);
			checkOsm(toLat, toLon);

			routing.buildIfUnbuilt();

			Coordinate fromCoord = new Coordinate(fromLat, fromLon);
			Coordinate toCoord = new Coordinate(toLat, toLon);


			SimulatorRequest simualtorResult = new SimulatorRequest(fromCoord, toCoord, error, speed, frequency);

			return mapper.writeValueAsString(simualtorResult);
		});

	}

	public static void checkOsm(double lat, double lon) {
		Envelope env1 = new Envelope();
		env1.expandToInclude(lat, lon);
		if(osmBoundsIndex.query(env1).size() == 0)
			loadOSMTile(lat,lon, 11);
	}

	public static void loadOSMTile(int x, int y, int z) {

		File osmDirectory = new File(SimulatorApp.appProps.getProperty("application.data.osmDirectory"));
		File zDir = new File(osmDirectory, "" + z);
		File xDir = new File(zDir, "" + x);
		File yPbfFile = new File(xDir, y + ".osm.pbf");

		if(!yPbfFile.exists()) {
			xDir.mkdirs();

			Envelope env = tile2Envelope(x, y, z);

			Double south = env.getMinY() < env.getMaxY() ? env.getMinY() : env.getMaxY();
			Double west = env.getMinX() < env.getMaxX() ? env.getMinX() : env.getMaxX();
			Double north = env.getMinY() > env.getMaxY() ? env.getMinY() : env.getMaxY();
			Double east = env.getMinX() > env.getMaxX() ? env.getMinX() : env.getMaxX();

			String vexUrl = SimulatorApp.appProps.getProperty("application.vex");

			if (!vexUrl.endsWith("/"))
				vexUrl += "/";

			vexUrl += String.format("?n=%s&s=%s&e=%s&w=%s", north, south, east, west);

			HttpURLConnection conn;

			log.log(Level.INFO, "loading osm from: " + vexUrl);

			try {
				conn = (HttpURLConnection) new URL(vexUrl).openConnection();

				conn.connect();

				if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
					System.err.println("Received response code " +
							conn.getResponseCode() + " from vex server");

					return;
				}

				// download the file
				InputStream is = conn.getInputStream();
				OutputStream os = new FileOutputStream(yPbfFile);
				ByteStreams.copy(is, os);
				is.close();
				os.close();

				loadPbfFile(yPbfFile);

				osmBoundsIndex.insert(env, env);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public static void loadOSMTile(double lat, double lon, int z) {
		int xtile = (int)Math.floor( (lon + 180) / 360 * (1<<z) ) ;
		int ytile = (int)Math.floor( (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<z) ) ;

		loadOSMTile(xtile, ytile, z);
	}

	public static Envelope loadPbfFile(File pbfFile) {

		log.log(Level.INFO, "loading osm from: " + pbfFile.getAbsolutePath());

		// load pbf osm source and merge into traffic engine
		OSM osm = new OSM(null);
		osm.loadFromPBFFile(pbfFile.getAbsolutePath().toString());

		Envelope env = null;

		try {
			// add OSM an truncate geometries
			//env = te.addOsm(osm,  false);
		}
		catch (Exception e) {
			e.printStackTrace();
			log.log(Level.SEVERE, "Unable to load osm: " + pbfFile.getAbsolutePath());
		}

		return env;
	}

	public static double tile2lon(int x, int z) {
		return x / Math.pow(2.0, z) * 360.0 - 180;
	}

	public static double tile2lat(int y, int z) {
		double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
		return Math.toDegrees(Math.atan(Math.sinh(n)));
	}

	public static Envelope tile2Envelope(final int x, final int y, final int zoom) {
		double maxLat = tile2lat(y, zoom);
		double minLat = tile2lat(y + 1, zoom);
		double minLon = tile2lon(x, zoom);
		double maxLon = tile2lon(x + 1, zoom);
		return new Envelope(minLon, maxLon, minLat, maxLat);
	}

	public static void loadSettings() {
		try {
			FileInputStream in = new FileInputStream("application.conf");
			appProps.load(in);
			in.close();
			
		} catch (IOException e) {
			log.log(Level.WARNING, "Unable to load application.conf file: {0}", e.getMessage());
			e.printStackTrace();
		}
	}
}

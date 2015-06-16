var Traffic = Traffic || {};

(function(A, $, L) {

	A.app = {};

	A.app.instance = new Backbone.Marionette.Application();

	A.app.instance.addRegions({
		navbar: "#navbar",
		sidebar: "#side-panel"
	});

	A.app.instance.addInitializer(function(options){

		A.app.nav = new A.app.Nav();

		A.app.instance.navbar.show(A.app.nav);

		A.app.map = L.map('map').setView([10.3586888,123.8830313], 10);

		L.tileLayer('https://a.tiles.mapbox.com/v4/conveyal.gepida3i/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoiY29udmV5YWwiLCJhIjoiMDliQURXOCJ9.9JWPsqJY7dGIdX777An7Pw', {
		attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery �� <a href="http://mapbox.com">Mapbox</a>',
		maxZoom: 18 }).addTo(A.app.map);

	});

	A.app.SimulateSidebar = Marionette.Layout.extend({

		template: Handlebars.getTemplate('app', 'sidebar-simulate'),

		events : {
			'click #resetRoute' : 'resetRoute',
			'change #day' : 'getRoute',
			'change #hour' : 'getRoute',
			'click #stepPath' : 'stepPath'
		},

		resetRoute : function() {
			A.app.nav.resetRoute();
		},

		getRoute : function() {
			A.app.nav.getRoute();
		},

		stepPath : function() {
			A.app.nav.stepPath();
		},

		initialize : function() {

			var _this = this;
		},

		onRender : function () {
			this.$("#journeyInfo").hide();
		}

	});

	A.app.Nav = Marionette.Layout.extend({

		template: Handlebars.getTemplate('app', 'navbar'),

		events : {
			'click #simulate' : 'clickSimulate',
			'click #test' : 'clickTest'
		},

		initialize : function() {
			_.bindAll(this, 'onMapClick');
		},


		clickSimulate: function(evt) {

			this.startRouting();

			A.app.sidebar = new A.app.SimulateSidebar();
			A.app.instance.sidebar.show(A.app.sidebar);

			this.$("li").removeClass("active");
			this.$("#routing").addClass("active");

			if(A.app.map.hasLayer(A.app.dataOverlay))
				A.app.map.removeLayer(A.app.dataOverlay);

			if(A.app.map.hasLayer(A.app.segmentOverlay))
				A.app.map.removeLayer(A.app.segmentOverlay);

			if(A.app.map.hasLayer(A.app.pathOverlay))
				A.app.map.removeLayer(A.app.pathOverlay);
		},

		clickTest: function(evt) {

			this.endRouting();

			this.$("li").removeClass("active");
			this.$("#analysis").addClass("active");

			if(A.app.map.hasLayer(A.app.dataOverlay))
				A.app.map.removeLayer(A.app.dataOverlay);

			A.app.segmentOverlay = L.tileLayer('http://localhost:4567//tile/segment?z={z}&x={x}&y={y}').addTo(A.app.map);
		},

		resetRoute : function() {

			if(A.app.sidebar) {
				A.app.sidebar.$("#clickInfo").show();
				A.app.sidebar.$("#journeyInfo").hide();
			}



			if(this.startPoint != false) {
				if(A.app.map.hasLayer(this.startPoint))
					A.app.map.removeLayer(this.startPoint);
			}

			if(this.endPoint != false) {
				if(A.app.map.hasLayer(this.endPoint))
					A.app.map.removeLayer(this.endPoint);
			}

			if(A.app.map.hasLayer(A.app.pathOverlay))
				A.app.map.removeLayer(A.app.pathOverlay);

			this.startPoint = false;
			this.endPoint = false;
		},

		startRouting : function() {
			A.app.map.on("click", this.onMapClick);
			this.resetRoute();
		},

		endRouting : function() {
			A.app.map.off("click", this.onMapClick);
			this.resetRoute();
		},

		onMapClick : function(evt) {

			if(this.startPoint == false) {
				this.startPoint = L.circleMarker(evt.latlng, {fillColor: "#0D0", color: '#fff', fillOpacity: 1.0,opacity: 1.0, radius: 5}).addTo(A.app.map);
			}
			else if(this.endPoint == false) {
				this.endPoint = L.circleMarker(evt.latlng, {fillColor: "#D00", color: '#fff', fillOpacity: 1.0,opacity: 1.0, radius: 5}).addTo(A.app.map);
				this.getRoute();
			}


		},

		stepPath : function() {

			if(A.app.simulatedPoints.length == A.app.simulatedPointsIndex + 1)
				return;

			A.app.simulatedPointsIndex++;

			var point = A.app.simulatedPoints[A.app.simulatedPointsIndex];

			var crossingStyle = {
				"color": "#ff7800",
				"weight": 2,
				"opacity": 0.65
			};

			var triplineStyle = {
				"color": "#000",
				"weight": 2,
				"opacity": 0.3
			};

			var segmentStyle = {
				"color": "#f00",
				"weight": 4,
				"opacity": 0.3
			};

			var pendingCrossingStyle = {
				"color": "#7800ff",
				"weight": 2,
				"opacity": 0.65
			};

			var sampleStyle = {
				"color": "#aaaa00",
				"weight": 5,
				"opacity": 1.0
			};

			A.app.pointLayer.clearLayers()

			if(point.gpsSegment.segment) {
				var polyline = L.Polyline.fromEncoded(point.gpsSegment.segment);
				polyline.setStyle(segmentStyle);
				var bounds = polyline.getBounds();
				A.app.pointLayer.addLayer(polyline);
				A.app.map.fitBounds(bounds);
			}

			$.each( point.tripLines, function(i, tripline) {
				var polyline = L.Polyline.fromEncoded(tripline.tripline);
				polyline.setStyle(triplineStyle);
				A.app.pointLayer.addLayer(polyline);
			});

			$.each( point.speedSamples, function(i, speedSample) {
				var polyline = L.Polyline.fromEncoded(speedSample.segmentGeom);
				polyline.setStyle(sampleStyle);
				A.app.pointLayer.addLayer(polyline);
			});

			$.each( point.pendingCrossings, function(i, crossing) {
				var polyline = L.Polyline.fromEncoded(crossing.tripline);
				polyline.setStyle(pendingCrossingStyle);
				A.app.pointLayer.addLayer(polyline);
			});

			$.each( point.crossings, function(i, crossing) {
				var polyline = L.Polyline.fromEncoded(crossing.tripline);
				polyline.setStyle(crossingStyle);
				A.app.pointLayer.addLayer(polyline);
			});

		},

		getRoute : function() {

			if(A.app.map.hasLayer(A.app.pathOverlay))
				A.app.map.removeLayer(A.app.pathOverlay);

			if(A.app.map.hasLayer(A.app.pointLayer))
				A.app.map.removeLayer(A.app.pointLayer);

			var startLatLng = this.startPoint.getLatLng();
			var endLatLng = this.endPoint.getLatLng();

			var frequency = A.app.sidebar.$("#frequency").val();
			var error = A.app.sidebar.$("#error").val();
			var speed = A.app.sidebar.$("#speed").val();

			$.getJSON('http://localhost:4567/route?fromLat=' + startLatLng.lat + '&fromLon=' + startLatLng.lng + '&toLat=' + endLatLng.lat + '&toLon=' + endLatLng.lng + '&speed=' + speed + '&frequency=' + frequency + '&error=' + error, function(data){
					var encoded = data.path;
					A.app.simulatedPoints = data.points;

					A.app.simulatedPointsIndex = 0;

					A.app.pathOverlay = L.Polyline.fromEncoded(encoded);
					A.app.pathOverlay.addTo(A.app.map);

					if(!A.app.pointLayer || !A.app.map.hasLayer(A.app.pointLayer))
						A.app.pointLayer = L.layerGroup().addTo(A.app.map);


					A.app.sidebar.$("#clickInfo").hide();
					A.app.sidebar.$("#journeyInfo").show();
			});

		},

		onRender : function() {

			// Get rid of that pesky wrapping-div.
			// Assumes 1 child element present in template.
			this.$el = this.$el.children();

			this.$el.unwrap();
			this.setElement(this.$el);

		}
	});


})(Traffic, jQuery, L);


$(document).ready(function() {

	Traffic.app.instance.start();

});

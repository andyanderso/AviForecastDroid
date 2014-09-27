package com.sebnarware.avalanche;

import java.util.HashMap;
import java.util.Map;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Handler;
import android.util.Log;

import com.flurry.android.FlurryAgent;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class PolygonOverlay extends Overlay {
	
    private static final String TAG = "PolygonOverlay";

	private static final int OVERLAY_ALPHA = (int) (0.65 * 255);
	
	private static final Paint paintOutline = initializePaintOutline();
	private static final Paint paintOutlineSelected = initializePaintOutlineSelected();
	// the colors for each avi level, 0 through 5
	private static final Paint[] paintAviLevel = {
		initializePaintAviLevel(255, 255, 255),
		initializePaintAviLevel(80, 184, 72),
		initializePaintAviLevel(255, 242, 0),
		initializePaintAviLevel(247, 148, 30),		
		initializePaintAviLevel(237, 28, 36),		
		initializePaintAviLevel(35, 31, 32)
	};
	
//	private static long totalTimeMillis = 0;
//	private static long totalPathTimeMillis = 0;
//	private static long totalDrawTimeMillis = 0;
//	private static long totalDrawCalls = 0;

	private RegionData regionData;
	private boolean selected;
	private GeoPoint bbTopLeft;
	private GeoPoint bbBottomRight;
	private GeoPoint visibleTopLeftPrevious;
	private GeoPoint visibleBottomRightPrevious;
	private Path cachedPath;

	
	private static Paint initializePaintOutline() {
	    Paint paint = new Paint();
	    paint.setARGB(OVERLAY_ALPHA, 0, 0, 0);
	    paint.setStrokeWidth(2);
	    paint.setStrokeCap(Paint.Cap.ROUND);
	    paint.setAntiAlias(true);
	    paint.setDither(false);
	    paint.setStyle(Paint.Style.STROKE);
	    return paint;
	}

	private static Paint initializePaintOutlineSelected() {
	    Paint paint = new Paint();
	    paint.setARGB(OVERLAY_ALPHA, 0, 0, 128);
	    paint.setStrokeWidth(5);
	    paint.setStrokeCap(Paint.Cap.ROUND);
	    paint.setAntiAlias(true);
	    paint.setDither(false);
	    paint.setStyle(Paint.Style.STROKE);
	    return paint;
	}

	private static Paint initializePaintAviLevel(int r, int g, int b) {
	    Paint paint = new Paint();
	    paint.setARGB(OVERLAY_ALPHA, r, g, b);
	    return paint;
	}

	public PolygonOverlay(RegionData regionData) {
	    this.regionData = regionData;
	    this.selected = false;
	    
	    // calculate the georect bounding box for this polygon
	    GeoPoint[] polygon = this.regionData.getPolygon();
	    int left = polygon[0].getLongitudeE6();
	    int right = polygon[0].getLongitudeE6();
	    int top = polygon[0].getLatitudeE6();
	    int bottom = polygon[0].getLatitudeE6();
	    for (int i = 1; i < polygon.length; i++) {
		    left = Math.min(left, polygon[i].getLongitudeE6());
		    right = Math.max(right, polygon[i].getLongitudeE6());
		    top = Math.max(top, polygon[i].getLatitudeE6());
		    bottom = Math.min(bottom, polygon[i].getLatitudeE6());
	    }
	    this.bbTopLeft = new GeoPoint(top, left);
	    this.bbBottomRight = new GeoPoint(bottom, right);
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean geoRectsIntersect(GeoPoint rect1TopLeft, GeoPoint rect1BottomRight, GeoPoint rect2TopLeft, GeoPoint rect2BottomRight) {
		
		int latLow = Math.max(rect1BottomRight.getLatitudeE6(), rect2BottomRight.getLatitudeE6());
		int latHigh = Math.min(rect1TopLeft.getLatitudeE6(), rect2TopLeft.getLatitudeE6());
		boolean latOverlap = latLow <= latHigh;
		
		int lonLow = Math.max(rect1TopLeft.getLongitudeE6(), rect2TopLeft.getLongitudeE6());
		int lonHigh = Math.min(rect1BottomRight.getLongitudeE6(), rect2BottomRight.getLongitudeE6());
		boolean lonOverlap = lonLow <= lonHigh;

		boolean intersect = latOverlap && lonOverlap;

		return intersect;
	}
	
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		
	    // NOTE this method gets called twice (once for each with shadow true and false); we only need to 
		// draw once, so only take action for one of the two calls, for efficiency
	    if (!shadow) {
	    	
	    	// get the georect for the currently visible portion of the map
            GeoPoint visibleTopLeft = mapView.getProjection().fromPixels(0, 0);
            GeoPoint visibleBottomRight = mapView.getProjection().fromPixels(mapView.getWidth(), mapView.getHeight());
        	
            // check if the polygon is onscreen
            boolean polygonOnscreen = geoRectsIntersect(visibleTopLeft, visibleBottomRight, bbTopLeft, bbBottomRight);
			Log.d(TAG, "polygon for region: " + this.regionData.getRegionId() + " is onscreen: " + polygonOnscreen);

			// NOTE since drawing is expensive, only draw if the map position if the polygon is onscreen
			if (polygonOnscreen) {
		    	
				Log.i(TAG, "drawing polygon for region: " + this.regionData.getRegionId());
//	            final long startTimeMillis = System.currentTimeMillis();

	            // check if the map position is the same
	            boolean mapPositionTheSame = visibleTopLeft.equals(visibleTopLeftPrevious) && visibleBottomRight.equals(visibleBottomRightPrevious);
	            visibleTopLeftPrevious = visibleTopLeft;
	        	visibleBottomRightPrevious = visibleBottomRight;
	            
	        	Path path;
	            if (cachedPath != null && mapPositionTheSame) {
					Log.d(TAG, "using cached path for region: " + this.regionData.getRegionId());
					path = cachedPath;
	            } else {
					Log.d(TAG, "calculating path for region: " + this.regionData.getRegionId());
	            	// create a path from the array of geo points
			    	// because paths are in pixels, not geopoints, we have to rebuild this every time the map zooms or moves
		            // NOTE this is computationally expensive...
				    path = new Path();
				    GeoPoint[] polygon = this.regionData.getPolygon();
			    	Projection projection = mapView.getProjection();

				    for (int i = 0; i < polygon.length; i++) {
					    Point pointInPixels = projection.toPixels(polygon[i], null);
				    	if (i == 0) {
				    		path.moveTo(pointInPixels.x, pointInPixels.y);
				    	} else {
				    		path.lineTo(pointInPixels.x, pointInPixels.y);
				    	}
				    }
				    path.close();
	            	
				    // cache the result
	            	cachedPath = path;
	            }
	            
//	            final long middleTimeMillis = System.currentTimeMillis();

			    // get the appropriate avi level fill color (based on the forecast for the region and the timeframe mode)
			    TimeframeMode timeframeMode = MainActivity.getDataManager().getTimeframeMode();
			    int aviLevel = this.regionData.aviLevelForTimeframeMode(timeframeMode);
			    Paint paintFill = paintAviLevel[aviLevel]; 
			    
			    // draw the outline, and the fill
			    canvas.drawPath(path, (selected ? paintOutlineSelected : paintOutline));
			    canvas.drawPath(path, paintFill);
			    
//	            final long endTimeMillis = System.currentTimeMillis();
//	            final long elapsedTimeMillis = endTimeMillis - startTimeMillis;
//	            final long elapsedPathTimeMillis = middleTimeMillis - startTimeMillis;
//	            final long elapsedDrawTimeMillis = endTimeMillis - middleTimeMillis;
//	            totalTimeMillis += elapsedTimeMillis;
//	            totalPathTimeMillis += elapsedPathTimeMillis;
//	            totalDrawTimeMillis += elapsedDrawTimeMillis;
//	            totalDrawCalls++;
//	            double averageTimeMillis = ((double) totalTimeMillis) / totalDrawCalls;
//	            double averagePathTimeMillis = ((double) totalPathTimeMillis) / totalDrawCalls;
//	            double averageDrawTimeMillis = ((double) totalDrawTimeMillis) / totalDrawCalls;
//				Log.d(TAG, "finished drawing polygon for region: " + this.regionData.getRegionId() + "; elapsed time (ms): " + elapsedTimeMillis +
//						"; total draw calls: " + totalDrawCalls + "; total time (ms): " + totalTimeMillis + "; avg time (ms): " + 
//						averageTimeMillis + "; avg path time (ms): " + averagePathTimeMillis + "; avg draw time (ms): " + averageDrawTimeMillis);
			}
	    }
	}

	@Override
	public boolean onTap(GeoPoint point, final MapView mapView) {
		
		Log.d(TAG, "checking for tap in region: " + this.regionData.getRegionId());

		// first, project lat/lon points onto a flat surface, so we can run the point in polygon algorithm
		GeoPoint[] polygon = this.regionData.getPolygon();
		Point[] polygonInPixels = new Point[polygon.length];
		for (int i = 0; i < polygon.length; i++) {
			polygonInPixels[i] = mapView.getProjection().toPixels(polygon[i], null);
	    }

		Point pointInPixels = mapView.getProjection().toPixels(point, null);

		// do the point in polygon check
		boolean pointInPolygon = contains(polygonInPixels, pointInPixels);
		
		if (pointInPolygon) {
			Log.i(TAG, "onTap tap was in polygon of region: " + this.regionData.getRegionId());
			
			// draw the region as selected
			selected = true;
			mapView.invalidate();
			
			// start the web view activity
		    Intent intent = new Intent(MainActivity.getMainActivity(), WebViewActivity.class);
		    String url = this.regionData.getURL();
		    intent.putExtra(MainActivity.INTENT_EXTRA_WEB_VIEW_URL, url);
		    String title = String.format(mapView.getResources().getString(R.string.detailed_forecast_title_format), this.regionData.getDisplayName());
		    intent.putExtra(MainActivity.INTENT_EXTRA_WEB_VIEW_TITLE, title);
		    
			Log.i(TAG, "onTap starting web view activity; url: " + url);
			
			Map<String, String> eventParams = new HashMap<String, String>();
			eventParams.put("region", this.regionData.getRegionId());
        	FlurryAgent.logEvent("view_detailed_forecast", eventParams);
        	
			MainActivity.getMainActivity().startActivity(intent);
			
			// clear the selection after a short time
			final PolygonOverlay self = this;
			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
			  @Override
			  public void run() {
				  self.setSelected(false);
				  mapView.invalidate();
			  }
			}, 500);
		}

		return pointInPolygon;
	}
	
    // return true if the given point is contained inside the polygon
    // see: http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
	public boolean contains(Point[] polygon, Point point) {

		boolean pointInPolygon = false;

		for (int i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
			if ((polygon[i].y > point.y) != (polygon[j].y > point.y)
					&& (point.x < (polygon[j].x - polygon[i].x) * (point.y - polygon[i].y) / (polygon[j].y - polygon[i].y) + polygon[i].x)) {
				pointInPolygon = !pointInPolygon;
			}
		}

		return pointInPolygon;
	}

}

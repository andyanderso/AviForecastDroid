package com.sebnarware.avalanche;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.util.Log;

import com.google.android.maps.GeoPoint;

public class RegionData {
	
    private static final String TAG = "RegionData";

	private String regionId; 
	private String displayName;
	private String URL;
	private GeoPoint[] polygon;
	private ForecastDay[] forecast;

	public RegionData(String regionId, String displayName, String URL, GeoPoint[] polygon) {
	    this.regionId = regionId;
	    this.displayName = displayName;
	    this.URL = URL;
	    this.polygon = polygon;
	}
	
	public void setForecast(ForecastDay[] forecast) {
		this.forecast = forecast;
	}

	public String getRegionId() {
		return regionId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getURL() {
		return URL;
	}

	public GeoPoint[] getPolygon() {
		return polygon;
	}
	
	public ForecastDay[] getForecast() {
		return forecast;
	}
	
	public int aviLevelForTimeframeMode(TimeframeMode timeframeMode) {
		
		int offsetDays = 0;
		switch (timeframeMode) {
		case Today: 
			offsetDays = 0;
			break;
		case Tomorrow:
			offsetDays = 1;
			break;
		case TwoDaysOut:
			offsetDays = 2;
			break;
		}
		
		final long DAY_IN_MILLIS = 24 * 60 * 60 * 1000;
		Date targetDate = new Date((new Date()).getTime() + offsetDays * DAY_IN_MILLIS);
		String dateString = (new SimpleDateFormat("yyyy-MM-dd")).format(targetDate);

		return aviLevelForDateString(dateString);
	}

	private int aviLevelForDateString(String dateString) {
		
		int aviLevel = AviLevel.UNKNOWN; 
		boolean lookupMatch = false; 
		
		if (forecast != null) {
			for (int i = 0; i < forecast.length; i++) {
				if (dateString.equals(forecast[i].getDateString())) {
					aviLevel = forecast[i].getAviLevel();
					lookupMatch = true;
		        	Log.d(TAG, "aviLevelForDateString match found; region: " + regionId + "; date: " + dateString + "; avi level: " + aviLevel);
				}
			}
		}

		if (!lookupMatch) {
        	Log.d(TAG, "aviLevelForDateString match not found; region: " + regionId + "; date: " + dateString);
		}
		
		return aviLevel;
	}

}



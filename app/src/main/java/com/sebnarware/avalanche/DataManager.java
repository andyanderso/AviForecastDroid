package com.sebnarware.avalanche;

import java.util.HashMap;

import org.json.*;

import android.content.Context;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.loopj.android.http.JsonHttpResponseHandler;

public class DataManager {
	
    private static final String TAG = "DataManager";

	private TimeframeMode timeframeMode;
	private NetworkEngine networkEngine = new NetworkEngine();
	private HashMap<String, RegionData> regions = new HashMap<String, RegionData>();
	
	public DataManager() {
        this.timeframeMode = TimeframeMode.Today;
	}

	public TimeframeMode getTimeframeMode() {
		return timeframeMode;
	}

	public void setTimeframeMode(TimeframeMode timeframeMode) {
		this.timeframeMode = timeframeMode;
	}

	public void loadRegionsAndForecasts(final DataListener dataListener, final Context context) {
		
		networkEngine.loadRegions(new JsonHttpResponseHandler() {
	        @Override
	        public void onSuccess(JSONArray response) {

	        	Log.i(TAG, "loadRegions network success");
	        	
	        	try {
	        		// parse out each region
	        		for (int i = 0; i < response.length(); i++) {
	        			JSONObject regionJSON = response.getJSONObject(i);
	        			String regionId = regionJSON.getString("regionId");
	        			String displayName = regionJSON.getString("displayName");
	        			String URL = regionJSON.getString("URL");
	        			
	        			JSONArray pointsJSON = regionJSON.getJSONArray("points");
	        			GeoPoint[] polygon = new GeoPoint[pointsJSON.length()];
	        			for (int j = 0; j < pointsJSON.length(); j++) {
	        				JSONObject pointJSON = pointsJSON.getJSONObject(j);
	        				double lat = pointJSON.getDouble("lat");
	        				double lon = pointJSON.getDouble("lon");
	        				polygon[j] = new GeoPoint((int)(lat * 1E6), (int)(lon * 1E6));
	        			}
	        			
	        			// create the region data, and add it to our set
	        			RegionData regionData = new RegionData(regionId, displayName, URL, polygon);
	        			regions.put(regionId, regionData);
		        		Log.d(TAG, "loadRegions created region: " + regionId);
		        		
		        		// call the listener
		        		dataListener.regionAdded(regionData);
	        		}
	        		
	        		Log.i(TAG, "loadRegions total count of regions created: " + regions.size());
	        		
	        		// now, go load the forecasts
	        		// NOTE we don't call dataListener.dataFetchDone() here, as we have more fetching to do
	        		loadForecasts(dataListener, context);

	        	} catch (JSONException e) {
	            	Log.w(TAG, "loadRegions JSON parsing failure; error: " + e.toString());
	            	dataListener.dataFetchDone(e);
	        	}
	        }
	        
	        @Override
	        public void onFailure(Throwable error, String content) {
	        	Log.w(TAG, "loadRegions network failure; error: " + error.toString() + "; content: " + content);
            	dataListener.dataFetchDone(error);
	        }
	    });
	}

	public void loadForecasts(final DataListener dataListener, final Context context) {

		networkEngine.loadForecasts(new JsonHttpResponseHandler() {
	        @Override
	        public void onSuccess(JSONArray response) {

	        	Log.i(TAG, "loadForecasts network success");
	        	
	        	try {
	        		// parse out each region forecast
	        		int validForecastCount = 0;
	        		for (int i = 0; i < response.length(); i++) {
	        			JSONObject regionJSON = response.getJSONObject(i);
	        			String regionId = regionJSON.getString("regionId");
	        			
	        			ForecastDay[] forecast = null; 
	        			// NOTE forecast may be null, if no forecast is currently available for this region
	        			if (!regionJSON.isNull("forecast")) {
	        				
	        				JSONArray forecastJSON = regionJSON.getJSONArray("forecast");
	        			
		        			forecast = new ForecastDay[forecastJSON.length()];
		        			for (int j = 0; j < forecastJSON.length(); j++) {
		        				JSONObject forecastDayJSON = forecastJSON.getJSONObject(j);
		        				String date = forecastDayJSON.getString("date");
		        				int aviLevel = forecastDayJSON.getInt("aviLevel");
		        				forecast[j] = new ForecastDay(date, aviLevel);
		        			}

			        		Log.d(TAG, "loadForecasts loaded forecast for region: " + regionId);
			        		validForecastCount++;
	        			} else {
			        		Log.d(TAG, "loadForecasts null forecast for region: " + regionId);
	        			}
	        			
	        			// get the region for this forecast
	        			RegionData regionData = regions.get(regionId);
	        			
	        			// NOTE region might be null in the case of a brand new region added during a session
	        			if (regionData != null) {
		        			// set the forecast on the region
		        			regionData.setForecast(forecast);
		        			
			        		// call the listener
			        		dataListener.forecastUpdated(regionData);
	        			}
	        		}
	        		
	        		Log.i(TAG, "loadForecasts total count of non-null forecasts received: " + validForecastCount);
	            	dataListener.dataFetchDone(null);

	        	} catch (JSONException e) {
	            	Log.w(TAG, "loadForecasts JSON parsing failure; error: " + e.toString());
	            	dataListener.dataFetchDone(e);
	        	}
	        }
	        
	        @Override
	        public void onFailure(Throwable error, String content) {
	        	Log.w(TAG, "loadForecasts network failure; error: " + error.toString() + "; content: " + content);
            	dataListener.dataFetchDone(error);
	        }
	    });
	}

}

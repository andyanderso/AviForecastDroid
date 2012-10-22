package com.sebnarware.avalanche;

import java.util.HashMap;

import org.json.*;

import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.loopj.android.http.JsonHttpResponseHandler;

public class DataManager {
	
    private static final String TAG = "DataManager";

    private DataListener dataListener; 
	private NetworkEngine networkEngine = new NetworkEngine();
	private HashMap<String, RegionData> regions = new HashMap<String, RegionData>();
	
	public DataManager(DataListener dataListener) {
		this.dataListener = dataListener;
	}
	
	public void loadRegions() {
		
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
	        				polygon[j] = new GeoPoint((int)(lat * 1000000), (int)(lon * 1000000));
	        			}
	        			
	        			// create the region data, and add it to our set
	        			RegionData regionData = new RegionData(regionId, displayName, URL, polygon);
	        			regions.put(regionId, regionData);
		        		Log.i(TAG, "loadRegions created region: " + regionId);
		        		
		        		// call the listener
		        		dataListener.regionAdded(regionData);
	        		}
	        		
	        		Log.i(TAG, "loadRegions total count of regions created: " + regions.size());

	        	} catch (JSONException e) {
	            	Log.w(TAG, "loadRegions JSON parsing failure; error: " + e.toString());
	        	}
	        }
	        
	        @Override
	        public void onFailure(Throwable error, String content) {
	        	Log.w(TAG, "loadRegions network failure; error: " + error.toString() + "; content: " + content);
	        }
	    });
	}

	
	public void loadForecasts() {
		
		networkEngine.loadForecasts(new JsonHttpResponseHandler() {
	        @Override
	        public void onSuccess(JSONArray response) {

	        	Log.i(TAG, "loadForecasts network success");
	        	
	        	try {
	        		// parse out each region
	        		for (int i = 0; i < response.length(); i++) {
	        			JSONObject regionJSON = response.getJSONObject(i);
	        			String regionId = regionJSON.getString("regionId");
	        			
	        			JSONArray forecastJSON = regionJSON.getJSONArray("forecast");
	        			ForecastDay[] forecast = new ForecastDay[forecastJSON.length()];
	        			for (int j = 0; j < forecastJSON.length(); j++) {
	        				JSONObject forecastDayJSON = forecastJSON.getJSONObject(j);
	        				String date = forecastDayJSON.getString("date");
	        				int aviLevel = forecastDayJSON.getInt("aviLevel");
	        				forecast[j] = new ForecastDay(date, aviLevel);
	        			}
	        			
	        			// set the forecast on the region
	        			RegionData regionData = regions.get(regionId);
	        			regionData.setForecast(forecast);
		        		Log.i(TAG, "loadForecasts loaded forecast for region: " + regionId);
		        		
		        		// call the listener
		        		dataListener.forecastUpdated(regionData);
	        		}
	        		
	        		Log.i(TAG, "loadForecasts total count of regions created: " + regions.size());

	        	} catch (JSONException e) {
	            	Log.w(TAG, "loadForecasts JSON parsing failure; error: " + e.toString());
	        	}
	        }
	        
	        @Override
	        public void onFailure(Throwable error, String content) {
	        	Log.w(TAG, "loadForecasts network failure; error: " + error.toString() + "; content: " + content);
	        }
	    });
	}

}

//// parse out each region
//for (int i = 0; i < ((NSArray *)JSON).count; i++) {
//    
//    NSString * regionId = [[JSON objectAtIndex:i] valueForKeyPath:@"regionId"];
//    NSArray * forecast = [[JSON objectAtIndex:i] valueForKeyPath:@"forecast"];
//    
//    if (regionId) {
//        
//        // NOTE forecast may be nil, if no forecast is currently available for this region
//                                
//        numRegions++;
//        DLog(@"loaded forecast for regionId: %@", regionId);
//        
//        // invoke the callback for each region read successfully
//        dataBlock(regionId, forecast);
//    }
//}
//}
//
//DLog(@"read forecasts for %i regions", numRegions);





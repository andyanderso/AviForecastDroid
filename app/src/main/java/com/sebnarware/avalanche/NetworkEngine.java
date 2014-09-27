package com.sebnarware.avalanche;

import org.json.JSONArray;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.flurry.android.FlurryAgent;
import com.loopj.android.http.*;

public class NetworkEngine {
	
    private static final String TAG = "NetworkEngine";
    
    // BUGBUG have not gotten local access to work from either the emulator or from physical devices
//    // local - test data
//    private static final String regionsUrl = "http://10.0.2.2:5000/v1test/regions.json";
//    private static final String forecastsUrl = "http://10.0.2.2:5000/v1test/forecasts.json";
//    // local
//    private static final String regionsUrl = "http://10.0.2.2:5000/v1/regions.json";
//    private static final String forecastsUrl = "http://10.0.2.2:5000/v1/forecasts.json";
//    // staging - test data
//    private static final String regionsUrl = "http://aviforecast-staging.herokuapp.com/v1test/regions.json";
//    private static final String forecastsUrl = "http://aviforecast-staging.herokuapp.com/v1test/forecasts.json";
//    // staging
//    private static final String regionsUrl = "http://aviforecast-staging.herokuapp.com/v1/regions.json";
//    private static final String forecastsUrl = "http://aviforecast-staging.herokuapp.com/v1/forecasts.json";
    // production
    private static final String regionsUrl = "http://aviforecast.herokuapp.com/v1/regions.json";
    private static final String forecastsUrl = "http://aviforecast.herokuapp.com/v1/forecasts.json";
    
    
    private PersistentCache persistentCache = new PersistentCache();
    
    // NOTE on threading: "All requests are made outside of your apps main UI thread, but any callback logic
    // will be executed on the same thread as the callback was created using Androids Handler message passing."
	private AsyncHttpClient client = new AsyncHttpClient();
    
    public void loadRegions(JsonHttpResponseHandler responseHandler) {
    	Log.i(TAG, "loadRegions called");
    	loadFromNetworkOrCache(regionsUrl, responseHandler);
    }
    
    public void loadForecasts( JsonHttpResponseHandler responseHandler) {
    	Log.i(TAG, "loadForecasts called");
    	loadFromNetworkOrCache(forecastsUrl, responseHandler);
    }
    
    private void loadFromNetworkOrCache(String url, final JsonHttpResponseHandler responseHandler) {
    	
    	// NOTE use the last path element of the url as the key
    	// BUGBUG could come up with a better algorithm here to avoid collisions
    	final String key = url.substring(url.lastIndexOf("/") + 1);
    	
		client.get(url, new JsonHttpResponseHandler() {
	        @Override
	        public void onSuccess(JSONArray response) {
	        	// happy path, cache the result, then invoke the caller's success callback
	    		Log.i(TAG, "loadFromNetworkOrCache network success, caching response");
	        	persistentCache.putJsonArray(response, key);
	        	responseHandler.onSuccess(response);
	        }
	        
	        @Override
	        public void onFailure(Throwable error, String content) {
	        	// we had a network failure; try the cache
	        	JSONArray cachedResponse = persistentCache.getJsonArray(key);
	        	if (cachedResponse != null) {
	        		// cache hit; return the cached data via the success callback
		    		Log.i(TAG, "loadFromNetworkOrCache network failed, cache hit");
	            	FlurryAgent.logEvent("no_network_data_cache_hit");
	        		responseHandler.onSuccess(cachedResponse);
	        	} else {
	        		// cache miss; invoke the caller's failure callback
		    		Log.i(TAG, "loadFromNetworkOrCache network failed, cache miss");
	            	FlurryAgent.logEvent("no_network_data_cache_miss");
	        		responseHandler.onFailure(error, content);
	        	}
	        }
	    });

    }
    
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		boolean networkAvailable = activeNetworkInfo != null;
		Log.i(TAG, "isNetworkAvailable network availability: " + networkAvailable);
		return networkAvailable;
    }
}

package com.sebnarware.avalanche;

import java.util.List;

//import com.flurry.android.FlurryAgent;
//import com.sbstrm.appirater.Appirater;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ToggleButton;

public class MainActivity extends MapActivity implements DataListener {
	
    public final static String INTENT_EXTRA_WEB_VIEW_URL = "com.sebnarware.avalanche.WEB_VIEW_URL";

    private static final String TAG = "MainActivity";

    private static MainActivity mainActivity;
    private static DataManager dataManager;

    private static final String PREFS_ACCEPTED_DISCLAIMER = "AcceptedDisclaimer";
    private static final int DISCLAIMER_DIALOG = 1;
    private static final int INFO_DIALOG = 2;
    private static final int DEFAULT_MAP_ZOOM_LEVEL = 8;

	private MapView mapView;
	private MyLocationOverlay myLocationOverlay;
	private ToggleButton buttonToday;
	private ToggleButton buttonTomorrow;
	private ToggleButton buttonTwoDaysOut;
		
    public static DataManager getDataManager() {
		return MainActivity.dataManager;
	}

	public static MainActivity getMainActivity() {
		return mainActivity;
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
    	
    	Log.i(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        MainActivity.mainActivity = this;
        
        
        // check if the user has already accepted the disclaimer
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean(PREFS_ACCEPTED_DISCLAIMER, false)) {
        	// show it
    		showDialog(DISCLAIMER_DIALOG);
        }
        
        
        // BUGBUG crashing with java.lang.NoClassDefFoundError: com.sbstrm.appirater.R$string
        // start appirater
//        Appirater.appLaunched(this);
        
        
        // get our data
        // NOTE we store the data manager in a static activity variable, so that even if the activity gets 
        // restarted (for example, on an orientation change) we don't have to reload the region data
        if (MainActivity.dataManager == null) {
        	// load everything
            MainActivity.dataManager = new DataManager();
            MainActivity.dataManager.setDataListener(this);
            MainActivity.dataManager.loadRegions(); 
        } else {
        	// just load the forecasts
            MainActivity.dataManager.setDataListener(this);
            MainActivity.dataManager.loadForecasts(); 
        }
	    
        
        // map view
	    mapView = (MapView) findViewById(R.id.mapview);
	    mapView.setBuiltInZoomControls(true);

	    
	    // add an overlay that draws the blue dot at the user location
	    // NOTE use a overloaded version of MyLocationOverlay to deal with a bug on some phones; see http://joshclemm.com/blog/?p=148
	    myLocationOverlay = new FixedMyLocationOverlay(this, mapView);
	    mapView.getOverlays().add(myLocationOverlay);
	    mapView.invalidate();
	    
	    // when we get a first location fix, pan/zoom the map around the user's location
	    myLocationOverlay.runOnFirstFix(new Runnable() {
            public void run() {
            	// NOTE run this on the UI thread
            	runOnUiThread(new Runnable() {
            		public void run() {
            			// pan and zoom the map
            			mapView.getController().animateTo(myLocationOverlay.getMyLocation());
            			mapView.getController().setZoom(DEFAULT_MAP_ZOOM_LEVEL);
            		}
            	});
            }
        });
	    
	    
        // set up mode timeframe mode buttons
        buttonToday = (ToggleButton) findViewById(R.id.buttonToday);
        buttonTomorrow = (ToggleButton) findViewById(R.id.buttonTomorrow);
        buttonTwoDaysOut = (ToggleButton) findViewById(R.id.buttonTwoDaysOut);
        setTimeframeMode(TimeframeMode.Today);

        
        // set up legend with a click action
        ImageView imageViewLegend = (ImageView) findViewById(R.id.imageviewLegend);
        final Context self = this; 
        imageViewLegend.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick called for legend image view; starting danger scale activity");
                
    		    Intent intent = new Intent(self, DangerScaleActivity.class);
    			self.startActivity(intent);
            }
        });
    }
	 
	@Override
	protected void onDestroy()
	{
   	Log.i(TAG, "onDestroy called");
		super.onDestroy();		
	}
	
	@Override
	protected void onStart()
	{
    	Log.i(TAG, "onStart called");
		super.onStart();
//		FlurryAgent.onStartSession(this, "29QWBK7Z3ZYCY8CBHGM5");
	}
	 
	@Override
	protected void onStop()
	{
    	Log.i(TAG, "onStop called");
		super.onStop();		
//		FlurryAgent.onEndSession(this);
	}

	@Override
	protected void onResume() {
    	Log.i(TAG, "onResume called");
		super.onResume();

		// when our activity resumes, we want to start listening for location updates
		myLocationOverlay.enableMyLocation();
	}

	@Override
	protected void onPause() {
    	Log.i(TAG, "onPause called");
		super.onPause();
		
		// when our activity pauses, we want to stop listening for location updates
		myLocationOverlay.disableMyLocation();
	}
	
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	Log.i(TAG, "onConfigurationChanged called");
        super.onConfigurationChanged(newConfig);
    }

	@Override
	protected boolean isRouteDisplayed() {
	    return false;
	}
	
	@Override
	public void regionAdded(RegionData regionData) {
	    
	    // do nothing
	}

	@Override
	public void forecastUpdated(RegionData regionData) {
		
		// add the overlay
		// NOTE assumption here is that any previous overlay for this region has already been cleared out
	    PolygonOverlay overlay = new PolygonOverlay(regionData);
	    List<Overlay> mapOverlays = mapView.getOverlays();
	    mapOverlays.add(overlay);

	    // force a redraw
	    mapView.invalidate();
	}

	public void setTimeframeToToday(View view) {
		setTimeframeMode(TimeframeMode.Today);
	}
	
	public void setTimeframeToTomorrow(View view) {
		setTimeframeMode(TimeframeMode.Tomorrow);
	}

	public void setTimeframeToTwoDaysOut(View view) {
		setTimeframeMode(TimeframeMode.TwoDaysOut);
	}

	private void setTimeframeMode(TimeframeMode timeframeMode) {

		Log.i(TAG, "setTimeframeMode setting mode to: " + timeframeMode);
		
		dataManager.setTimeframeMode(timeframeMode);
		
		// update button toggle states
		switch (timeframeMode) {
			case Today:
				buttonToday.setChecked(true);
				buttonTomorrow.setChecked(false);
				buttonTwoDaysOut.setChecked(false);
				break;
			case Tomorrow:
				buttonToday.setChecked(false);
				buttonTomorrow.setChecked(true);
				buttonTwoDaysOut.setChecked(false);
				break;
			case TwoDaysOut:
				buttonToday.setChecked(false);
				buttonTomorrow.setChecked(false);
				buttonTwoDaysOut.setChecked(true);
				break;
		}
		
	    // force a redraw
	    mapView.invalidate();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu_main, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.menuitem_info:
	    		Log.i(TAG, "onOptionsItemSelected received click on info");
	    		showDialog(INFO_DIALOG);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case INFO_DIALOG:
			showInfoDialog();
			break;
		case DISCLAIMER_DIALOG:
			showDisclaimerDialog();
			break;
		}
		return super.onCreateDialog(id);
	}

	private void showInfoDialog() {
		Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.dialog_info_title);
		builder.setMessage(R.string.dialog_info_message);
		builder.setPositiveButton(R.string.dialog_info_positive_button, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Log.i(TAG, "info dialog ok clicked");
			}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private void showDisclaimerDialog() {
		Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.dialog_disclaimer_title);
		builder.setMessage(R.string.dialog_disclaimer_message);
		builder.setCancelable(false);
		final Context self = this;
		builder.setPositiveButton(R.string.dialog_disclaimer_positive_button, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Log.i(TAG, "disclaimer dialog ok clicked");
				
				// record that the user has accepted the disclaimer
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(self);
				Editor editor = prefs.edit();
				editor.putBoolean(PREFS_ACCEPTED_DISCLAIMER, true);
				editor.commit();
			}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
}

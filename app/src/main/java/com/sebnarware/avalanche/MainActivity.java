package com.sebnarware.avalanche;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.flurry.android.FlurryAgent;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

public class MainActivity extends MapActivity implements DataListener {
	
    public final static String INTENT_EXTRA_WEB_VIEW_URL = "com.sebnarware.avalanche.WEB_VIEW_URL";
    public final static String INTENT_EXTRA_WEB_VIEW_TITLE = "com.sebnarware.avalanche.WEB_VIEW_TITLE";

    private static final String TAG = "MainActivity";

    private static MainActivity mainActivity;
    private static DataManager dataManager;

    private static final String PREFS_ACCEPTED_DISCLAIMER = "AcceptedDisclaimer";
    private static final int DISCLAIMER_DIALOG = 1;
    private static final int INFO_DIALOG = 2;
    private static final int DEFAULT_MAP_ZOOM_LEVEL = 8;

	private MapView mapView;
	private MyLocationOverlay myLocationOverlay;
		
    public static DataManager getDataManager() {
		return MainActivity.dataManager;
	}

	public static MainActivity getMainActivity() {
		return mainActivity;
	}

	public static boolean areLocationProvidersEnabled(Context context) {
	    ContentResolver cr = context.getContentResolver();
	    String providersAllowed = Settings.Secure.getString(cr, Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
	    return providersAllowed != null && providersAllowed.length() > 0;
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	
    	Log.i(TAG, "onCreate called");
    	
       	// NOTE window feature requests must happen before content is added
    	// get access to the activity indicator
    	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    	// show our icon
    	requestWindowFeature(Window.FEATURE_LEFT_ICON);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.logo);
        
        
        // set up our singleton
        MainActivity.mainActivity = this;
        
        
        // check if the user has already accepted the disclaimer
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean(PREFS_ACCEPTED_DISCLAIMER, false)) {
        	// show it
    		showDialog(DISCLAIMER_DIALOG);
        }
        
        
        // start appirater
        Appirater.appLaunched(this);
        
        
        // check if location services are available, and warn the user if not
        if (!areLocationProvidersEnabled(this)) {
        	FlurryAgent.logEvent("no_location_providers");
        	Toast.makeText(this, R.string.toast_location_services_disabled, Toast.LENGTH_LONG).show();
        }
	    
        
        // configure the map view
	    mapView = (MapView) findViewById(R.id.mapview);
	    mapView.setBuiltInZoomControls(true);

	    
	    // add an overlay that draws the blue dot at the user location
	    // NOTE use a custom version of MyLocationOverlay to deal with a bug on some phones; see http://joshclemm.com/blog/?p=148
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

        
        // set up legend with a click action
        ImageView imageViewLegend = (ImageView) findViewById(R.id.imageviewLegend);
        final Context self = this; 
        imageViewLegend.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick called for legend image view; starting danger scale activity");
            	FlurryAgent.logEvent("view_danger_scale");
                
    		    Intent intent = new Intent(self, DangerScaleActivity.class);
    			self.startActivity(intent);
            }
        });
        

        // get our data
        // NOTE we store the data manager in a static activity variable, so that even if the activity gets 
        // recreated (for example, on an orientation change) or resumed we don't have to reload the region data
        setProgressBarIndeterminateVisibility(true);
    	FlurryAgent.logEvent("load_data", null, true);
        if (MainActivity.dataManager == null) {
        	// load everything
            MainActivity.dataManager = new DataManager();
            MainActivity.dataManager.loadRegionsAndForecasts(this, this);
        } else {
        	// just load the forecasts
            MainActivity.dataManager.loadForecasts(this, this); 
        }
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
		FlurryAgent.onStartSession(this, "29QWBK7Z3ZYCY8CBHGM5");
	}
	 
	@Override
	protected void onStop()
	{
    	Log.i(TAG, "onStop called");
		super.onStop();		
		FlurryAgent.onEndSession(this);
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
	}

	@Override
	public void dataFetchDone(Throwable error) {
    	FlurryAgent.endTimedEvent("load_data");
        setProgressBarIndeterminateVisibility(false);
        
        if (error != null) {
        	Toast.makeText(this, R.string.toast_data_fetch_failure, Toast.LENGTH_SHORT).show();
        	// BUGBUG we don't currently give a way to try to reload the regions... so we rely on the user 
        	// having loaded them at least once successfully, to get them into the persistent cache
        }

	    // force a redraw
	    mapView.invalidate();
	}

    public void onNavClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.buttonForecastMain:
                if (checked) {
                    Toast.makeText(getBaseContext(), "You are already on the Forecasts Page. Click on a region to get the detailed forecast.",
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.buttonSubmitObsMain:
                if (checked) {
                    Intent submitObsIntent = new Intent(getApplicationContext(), SubmitObs.class);
                    submitObsIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//<--return to SubmitObs activity and don't restart if it is already running.
                    startActivityForResult(submitObsIntent, 0);//<--start Submit Obs activity'
                    RadioButton submitObs = (RadioButton)findViewById(R.id.buttonSubmitObsMain);
                    RadioButton donate = (RadioButton)findViewById(R.id.buttonDonateMain);
                    RadioButton forecast = (RadioButton)findViewById(R.id.buttonForecastMain);
                    submitObs.setChecked(false);
                    donate.setChecked(false);
                    forecast.setChecked(true);
                }
                break;
            case R.id.buttonDonateMain:
                if (checked) {
                    Intent donateIntent = new Intent(getApplicationContext(), Donate.class);
                    donateIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//<--return to donate activity and don't restart if it is already running.
                    startActivity(donateIntent);//<--start Donate activity
                    RadioButton submitObs = (RadioButton)findViewById(R.id.buttonSubmitObsMain);
                    RadioButton donate = (RadioButton)findViewById(R.id.buttonDonateMain);
                    RadioButton forecast = (RadioButton)findViewById(R.id.buttonForecastMain);
                    submitObs.setChecked(false);
                    donate.setChecked(false);
                    forecast.setChecked(true);
                }
                    break;
        }
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
		
		Map<String, String> eventParams = new HashMap<String, String>();
		eventParams.put("timeframe", timeframeMode.toString());
    	FlurryAgent.logEvent("set_timeframe_mode", eventParams);

		dataManager.setTimeframeMode(timeframeMode);
				
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
	
	public void onInfoButtonClick(View view) {
		showInfoDialog();
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
    	FlurryAgent.logEvent("view_info");

		Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.dialog_info_title);
		String versionName = "";
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;  
		} catch (Exception e) {
			Log.w(TAG, "could not get package info");
		}
		builder.setMessage(String.format(getResources().getString(R.string.dialog_info_message), versionName));
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

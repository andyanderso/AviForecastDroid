package com.sebnarware.avalanche;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebSettings.ZoomDensity;
import android.app.Activity;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class Donate extends Activity {

    TextView txtDonate;
    Button btnDonate;
    SharedPreferences defaultAvCenter;
    int avyCenterId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

       // show our icon
        requestWindowFeature(Window.FEATURE_LEFT_ICON);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);

        setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.logo);

        // check if the user has already set a default center

        defaultAvCenter = PreferenceManager.getDefaultSharedPreferences(this);
        String defAvCenter = defaultAvCenter.getString("default_avalanche_center", "");
        if (defAvCenter == "") {
            // ask user to set default center
            Toast.makeText(getApplicationContext(),
                    "Please use to menu button in the upper right corner to set your default avalanche center", Toast.LENGTH_LONG).show();
        }

        //Preferences button setup
        ImageButton prefs = (ImageButton) findViewById(R.id.buttonPrefs);
        prefs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent prefsIntent = new Intent(Donate.this, SettingsActivity.class);
                startActivity(prefsIntent);
            }
        });

        avyCenterId = Integer.parseInt(defAvCenter);
        String[] avyCenterDonateLink = getResources().getStringArray(R.array.pref_default_avalanche_center_titles);
        String avyCenterName = avyCenterDonateLink[avyCenterId];
        txtDonate = (TextView) findViewById(R.id.textViewDonate);
        txtDonate.setText("Clicking on the 'Donate' button below will open the "+avyCenterName+"'s donation page in your browser so that you can donate directly to your favorite avalanche center.");
        btnDonate = (Button) findViewById(R.id.buttonDonateLink);
        btnDonate.setText("Donate to the "+avyCenterName);
    }

    public void onDonateClicked (View view) {
        defaultAvCenter = PreferenceManager.getDefaultSharedPreferences(this);
        String defAvCenter = defaultAvCenter.getString("default_avalanche_center", "");
        avyCenterId = Integer.parseInt(defAvCenter);
        String[] avyCenterDonateLink = getResources().getStringArray(R.array.avalanche_center_donate_links);
        String DonateLink = avyCenterDonateLink[avyCenterId];
        final Uri link = Uri.parse(DonateLink);
        Button donate = (Button) findViewById(R.id.buttonDonateLink);

        Intent donateIntent = new Intent(Intent.ACTION_VIEW, link);
        startActivity(donateIntent);

        finish();
    }

    public void onNavClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.buttonForecastDon:
                if (checked) {
                    Intent forecastsIntent = new Intent(getApplicationContext(), MainActivity.class);
                    forecastsIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//<--return to already running main activity
                    startActivity(forecastsIntent);//<--start main activity
                    RadioButton submitObs = (RadioButton)findViewById(R.id.buttonSubmitObsDon);
                    RadioButton donate = (RadioButton)findViewById(R.id.buttonDonateDon);
                    RadioButton forecast = (RadioButton)findViewById(R.id.buttonForecastDon);
                    submitObs.setChecked(false);
                    donate.setChecked(true);
                    forecast.setChecked(false);

                }
                break;
            case R.id.buttonSubmitObsDon:
                if (checked) {
                    Intent submitObsIntent = new Intent(getApplicationContext(), SubmitObs.class);
                    submitObsIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//<--return to SubmitObs activity and don't restart if it is already running.
                    startActivityForResult(submitObsIntent, 0);//<--start Submit Obs activity
                    RadioButton submitObs = (RadioButton)findViewById(R.id.buttonSubmitObsDon);
                    RadioButton donate = (RadioButton)findViewById(R.id.buttonDonateDon);
                    RadioButton forecast = (RadioButton)findViewById(R.id.buttonForecastDon);
                    submitObs.setChecked(false);
                    donate.setChecked(true);
                    forecast.setChecked(false);

                }
                break;
            case R.id.buttonDonateDon:
                if (checked) {
                    Toast.makeText(getBaseContext(), "You are already on the Donate Page",
                            Toast.LENGTH_SHORT).show();
                }

                break;

        }
    }
    @Override
    protected void onResume() {
        defaultAvCenter = PreferenceManager.getDefaultSharedPreferences(this);
        String defAvCenter = defaultAvCenter.getString("default_avalanche_center", "");
        avyCenterId = Integer.parseInt(defAvCenter);
        String[] avyCenterDonateLink = getResources().getStringArray(R.array.pref_default_avalanche_center_titles);
        String avyCenterName = avyCenterDonateLink[avyCenterId];
        txtDonate = (TextView) findViewById(R.id.textViewDonate);
        txtDonate.setText("Clicking on the 'Donate' button below will open the "+avyCenterName+"'s donation page in your browser so that you can donate directly to your favorite avalanche center.");
        btnDonate = (Button) findViewById(R.id.buttonDonateLink);
        btnDonate.setText("Donate to the "+avyCenterName);
        super.onResume();
    }

}

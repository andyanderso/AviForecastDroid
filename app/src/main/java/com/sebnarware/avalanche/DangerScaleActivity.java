package com.sebnarware.avalanche;

import android.os.Bundle;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebSettings.ZoomDensity;
import android.app.Activity;

public class DangerScaleActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {

       	// NOTE window feature requests must happen before content is added
    	// show our icon
    	requestWindowFeature(Window.FEATURE_LEFT_ICON);

    	super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_danger_scale);
        
        setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.logo);

        
        WebView webView = (WebView) findViewById(R.id.webview);
        
        // set a reasonable zoom and view mode
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setDefaultZoom(ZoomDensity.FAR);
        
        // enable zooming
        webView.getSettings().setBuiltInZoomControls(true);
                
        webView.loadUrl("file:///android_asset/danger_scale_front.jpg");
    }

}

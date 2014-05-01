package org.cs.washington.cse477;

import java.util.List;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class DeviceSetupActivity extends ActionBarActivity {

	protected WifiManager wifiManager;
	protected List<WifiConfiguration> wifiConfigurations;
	protected List<ScanResult> scanResults;
	protected Spinner networksSpinner;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_setup);
		
		// grab handle to networks spinner
		networksSpinner = (Spinner) findViewById(R.id.dev_setup_networks_spinner);
		
		// grab handle to system WifiManager
		wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);

		// collect scan results and store
		scanResults = wifiManager.getScanResults();
    	String[] wifiArray = new String[scanResults.size()];
    	for( int i = 0; i < scanResults.size(); i++) {
    		ScanResult wc = scanResults.get(i); 
    		wifiArray[i] = wc.SSID;
    	}
    	
    	// create and set adapter for network spinner
    	ArrayAdapter<String> aa = new ArrayAdapter<String>(this,
    			android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, wifiArray);
    	networksSpinner.setAdapter(aa);
    	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.device_setup, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public boolean refreshScan(View v) {
		return wifiManager.startScan();
	}
	
	public void connectToFlyport(View view) {
		
		//wifiConfigurations = wifiManager.getConfiguredNetworks();
    	String SSID = "FlyportSoftAP";
    	WifiConfiguration wificonf = new WifiConfiguration();
    	wificonf.SSID = "\"" + SSID + "\"";
    	wificonf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
    	 
    	wifiManager.addNetwork(wificonf);
    	
    	
    	for( WifiConfiguration i : wifiConfigurations ) {
    	    if(i.SSID != null && i.SSID.equals("\"" + SSID + "\"")) {
    	         wifiManager.disconnect();
    	         wifiManager.enableNetwork(i.networkId, true);
    	         wifiManager.reconnect();               

    	         break;
    	    }           
    	 }
    }
}

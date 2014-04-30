package org.cs.washington.cse477;

import java.util.List;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

public class DeviceSetupActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_setup);
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
	
	public void connectToFlyport(View view) {
		WifiManager wifiManager;
		List<WifiConfiguration> wifiList;
		wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
        wifiList = wifiManager.getConfiguredNetworks();
        
    	String SSID = "FlyportSoftAP";
    	WifiConfiguration wificonf = new WifiConfiguration();
    	wificonf.SSID = "\"" + SSID + "\"";
    	wificonf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
    	 
    	wifiManager.addNetwork(wificonf);
    	
    	
    	for( WifiConfiguration i : wifiList ) {
    	    if(i.SSID != null && i.SSID.equals("\"" + SSID + "\"")) {
    	         wifiManager.disconnect();
    	         wifiManager.enableNetwork(i.networkId, true);
    	         wifiManager.reconnect();               

    	         break;
    	    }           
    	 }
    }
}

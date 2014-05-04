package org.cs.washington.cse477;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.DefaultClientConnection;
import org.apache.http.message.BasicNameValuePair;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources.NotFoundException;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

public class DeviceSetupActivity extends ActionBarActivity {

	protected WifiManager wifiManager;
	protected List<WifiConfiguration> wifiConfigurations;
	protected List<ScanResult> scanResults;
	protected List<String> stringScanResults;
	protected Spinner networksSpinner;
	protected Spinner securitySpinner;
	protected ArrayAdapter<String> networksAdapter;
	protected ArrayAdapter<String> securityAdapter;
	protected WifiConnectionReceiver wifiWaiter;
	protected WifiReceiver receiverWifi;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_setup);
		
		// grab handle to networks spinner
		networksSpinner = (Spinner) findViewById(R.id.dev_setup_networks_spinner);
		
		// grab handle to system WifiManager
		wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);

		receiverWifi = new WifiReceiver();
	    registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		
	    wifiWaiter = new WifiConnectionReceiver();
	    //registerReceiver(wifiWaiter, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
	    
		// collect scan results and store
		scanResults = wifiManager.getScanResults();
		stringScanResults = new LinkedList<String>();
		Iterator<ScanResult> iter = scanResults.iterator();
		while (iter.hasNext()) {
			ScanResult sr = iter.next();
			stringScanResults.add(sr.SSID);
		}
		
		
    	String[] wifiArray = new String[scanResults.size()];
    	for( int i = 0; i < scanResults.size(); i++) {
    		ScanResult wc = scanResults.get(i); 
    		wifiArray[i] = wc.SSID;
    	}
    	
    	// create and set adapter for network spinner
    	networksAdapter = new ArrayAdapter<String>(this,
    			android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, stringScanResults);
    	networksSpinner.setAdapter(networksAdapter);
    	
    	// create and set adapter for security spinner
    	String[] secArray = new String[] {"WPA", "WPA2"};
    	securitySpinner = (Spinner) findViewById(R.id.dev_setup_security_spinner);
    	securityAdapter = new ArrayAdapter<String>(this,
    			android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, secArray);
    	securitySpinner.setAdapter(securityAdapter);
    	
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
	
	@Override
	protected void onPause() {
        unregisterReceiver(receiverWifi);
        super.onPause();
    }
	
	@Override
	protected void onResume() {
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        //registerReceiver(wifiWaiter, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        super.onResume();
    }
	
	public boolean refreshScan(View v) {
		return wifiManager.startScan();
	}
	
	public void connectToFlyport(View view) {
		
		// save info to send to FlyPort
		
		final String SSID_user = networksAdapter.getItem(networksSpinner.getSelectedItemPosition());
		final String pass_user = ((EditText) findViewById(R.id.dev_setup_password)).toString();
		
		
		wifiConfigurations = wifiManager.getConfiguredNetworks();
    	String SSID = "EchoSetup";
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
    	/*
    	// wait for connection
    	
    	
    	
    	// send POST to FlyPort to configure
    	// 192.168.1.13
    	HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost("http://192.168.1.13");
		
		try {
	        // Add your data
			post.addHeader("NETTYPE", "infra");
			post.addHeader("DHCPCL", "enabled");
			post.addHeader("SSID", SSID_user);
//			if (!pass_user.isEmpty()) {
//				post.addHeader("NETTYPE", "infra");
//			}
			post.addHeader("SECTYPE", "open");
			
			
	        // Execute HTTP Post Request
	        HttpResponse response = client.execute(post);

	    } catch (ClientProtocolException e) {
	        // TODO Auto-generated catch block
	    } catch (IOException e) {
	        // TODO Auto-generated catch block
	    }
		
		*/
		// go back to notifications
		Intent intent = new Intent(this, NotificationActivity.class);
    	startActivity(intent);
    	
    }
	
	StringBuilder sb = new StringBuilder();
	
	
	// Wifi Receiver to listen for wifi scan refresh requests
	class WifiReceiver extends BroadcastReceiver {
		// refresh wifi list
        public void onReceive(Context c, Intent intent) {
        	scanResults = wifiManager.getScanResults();
        	stringScanResults.clear();
        	Iterator<ScanResult> iter = scanResults.iterator();
    		while (iter.hasNext()) {
    			stringScanResults.add(iter.next().SSID);
    		}
    		networksAdapter.notifyDataSetChanged();
        }
    }
	
	class WifiConnectionReceiver extends BroadcastReceiver {
		public void onReceive(Context c, Intent intent) {
			
		}
	}
}

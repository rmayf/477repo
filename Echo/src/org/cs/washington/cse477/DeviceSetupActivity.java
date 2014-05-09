package org.cs.washington.cse477;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.DefaultClientConnection;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.AndroidHttpClient;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

public class DeviceSetupActivity extends ActionBarActivity {

	protected ConnectivityManager conMgr;
	protected WifiManager wifiManager;

	protected List<WifiConfiguration> wifiConfigurations;
	protected List<ScanResult> scanResults;
	protected List<String> stringScanResults;
	protected Spinner networksSpinner;
	protected Spinner securitySpinner;
	protected ArrayAdapter<String> networksAdapter;
	protected ArrayAdapter<String> securityAdapter;

	protected WifiConnectionEstablishedReceiver wifiWaiter;
	protected WifiScanResultsReceiver receiverWifi;

	private boolean connectedToFlyPort = false;
	private boolean postComplete = false;

	private static final String TAG = "DeviceSetupActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
		.permitAll().build();
		StrictMode.setThreadPolicy(policy);

		setContentView(R.layout.activity_device_setup);

		setupManagers();
		setupReceivers();
		setupUI();
		
	}

	protected void setupManagers() {
		// grab handle to ConnectivityManager
		conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		// grab handle to system WifiManager
		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
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
		unregisterReceivers();
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		unregisterReceivers();
		super.onStop();	
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		setupManagers();
		setupReceivers();
		setupUI();
	}

	public void refreshScan(View v) {
		wifiManager.startScan();
	}

	protected void setupUI() {
		
		// collect initial scan results and store
		scanResults = wifiManager.getScanResults();
		stringScanResults = new LinkedList<String>();
		Iterator<ScanResult> iter = scanResults.iterator();
		while (iter.hasNext()) {
			ScanResult sr = iter.next();
			stringScanResults.add(sr.SSID);
		}

		// grab handle to networks spinner
		networksSpinner = (Spinner) findViewById(R.id.dev_setup_networks_spinner);
		// create and set adapter for network spinner
		networksAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_dropdown_item,
				android.R.id.text1, stringScanResults);
		networksSpinner.setAdapter(networksAdapter);

		// create and set adapter for security spinner
		String[] secArray = new String[] { "Open", "WPA", "WPA2" };
		securitySpinner = (Spinner) findViewById(R.id.dev_setup_security_spinner);
		securityAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_dropdown_item,
				android.R.id.text1, secArray);
		securitySpinner.setAdapter(securityAdapter);
	}
	
	protected void setupReceivers() {
		receiverWifi = new WifiScanResultsReceiver();
		registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

		wifiWaiter = new WifiConnectionEstablishedReceiver();
		registerReceiver(wifiWaiter, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}
	
	protected void unregisterReceivers() {
		unregisterReceiver(receiverWifi);
		unregisterReceiver(wifiWaiter);
	}
	
	// Called on Configure Device! button click
	// begins process of:
	// 1. connecting to FlyPort
	// 2. send HTTP POST FlyPort
	// 3. reconnect phone to old WiFi network
	// 4. wait for push notification indicating setup complete
	int savedNetwork = -1;
	public void configureFlyport(View view) {
		// save info to send to FlyPort
		Log.v(TAG, "Configure pressed");
		if (wifiManager == null) {
			wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		}
		if (wifiManager == null) {
			Log.e(TAG, "failed to get WifiManager");
			return;
		}

		WifiInfo currentWifi = wifiManager.getConnectionInfo();
		if (currentWifi != null) {
			savedNetwork = currentWifi.getNetworkId();
		}

		// Connect to EchoSetup
		WifiConfiguration wificonf = new WifiConfiguration();
		final String SSID = "EchoSetup";
		wificonf.SSID = "\"" + SSID + "\"";
		wificonf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

		
		wifiConfigurations = wifiManager.getConfiguredNetworks();
		// remove saved configuration for any matching SSID
		for (WifiConfiguration i : wifiConfigurations) {
			if (i.SSID != null && i.SSID.equals("\"" + SSID + "\"")) {
				wifiManager.removeNetwork(i.networkId);
			}
		}
		
		// add new configuration to WiFi manager
		int newNetwork = wifiManager.addNetwork(wificonf);
		wifiManager.disconnect();
		wifiManager.enableNetwork(newNetwork, true);
		
		
//		
//		// connect to new configuration
//		for (WifiConfiguration i : wifiConfigurations) {
//			if (i.SSID != null && i.SSID.equals("\"" + SSID + "\"")) {
//				wifiManager.disconnect();
//				wifiManager.enableNetwork(i.networkId, true);
//				//wifiManager.reconnect();
//				break;
//			}
//		}
		
		Log.v(TAG, "connecting...");
		// After reconnecting, the WifiConnectionEstablishedReceiver will handle
		// sending the POST to the FlyPort
	}

	/*
	 * called to send POST to FlyPort
	 * 
	 * parses user input and calls HttpPOSTGen functions to perform the post
	 */
	public boolean sendConfigurationPOST() {
		// grab user input data
		final String SSID_user = networksAdapter.getItem(networksSpinner.getSelectedItemPosition());
		final String pass_user = ((EditText) findViewById(R.id.dev_setup_password)).getText().toString();
		final String encr_user = securityAdapter.getItem(securitySpinner.getSelectedItemPosition());
		
		String stat = "SSID: " + SSID_user + "\nEncryption: " + encr_user + "\nPassword: " + pass_user;
		Log.v(TAG,"sendConfigurationPOST():\n" + stat);
		int attempts = 0;
		final int MAX_ATTEMPTS = 3;
		boolean res = false;
		while (!res && attempts < MAX_ATTEMPTS) {
			try {
				attempts++;
				res = HttpPOSTGen.sendPOST(SSID_user, encr_user, pass_user);
				Log.v(TAG,"Attempt " + attempts + " result: " + res);
			} catch (Exception e) {
				Log.e(TAG,"Exception on attempt " + attempts);
			}
		}
		return res;
	}

	/*
	 * Broadcast Receivers for WiFi Events
	 */
	class WifiScanResultsReceiver extends BroadcastReceiver {
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

	class WifiConnectionEstablishedReceiver extends BroadcastReceiver {
		public void onReceive(Context c, Intent intent) {
			Log.v(TAG, "receiver called");

			NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
			if (activeNetwork != null) {
				boolean isWiFi = (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI);
				boolean isConnected = (activeNetwork.isConnected() && activeNetwork
						.getState() == NetworkInfo.State.CONNECTED);
				if (isWiFi && isConnected && !connectedToFlyPort) {
					WifiInfo wifi = wifiManager.getConnectionInfo();
					if (wifi.getSSID().equals("\"EchoSetup\"")) {
						Log.v(TAG, "Connected to EchoSetup!");
						
						connectedToFlyPort = true;
						// send post message to FlyPort device
						postComplete = sendConfigurationPOST();
						Log.v(TAG,"POST status: " + postComplete);
						
						if (savedNetwork != -1) {
							Log.v(TAG, "reconnected to saved wifi network");
							wifiManager.setWifiEnabled(true);
							wifiManager.enableNetwork(savedNetwork, false);
							wifiManager.reconnect();
						}
						
						Intent i = new Intent(c, NotificationActivity.class);
				    	startActivity(i);
						
					} else {
						Log.v(TAG, "Connected to " + wifi.getSSID());
					}
				} else if (isWiFi && !isConnected) {
					Log.v(TAG, "ActiveNetwork is WiFi, but state is: "
							+ activeNetwork.getDetailedState().toString() + " connected to FlyPort: " + connectedToFlyPort);
				} else if (!isWiFi) {
					Log.v(TAG, "ActiveNetwork is not WiFi");
				} else {
					Log.v(TAG, "ActiveNetwork is not WiFi or is not Connected");
				}
			} else {
				Log.v(TAG, "ActiveNetworkInfo == null");
			}
		}
	}
}



/*
 * WiFi configuration to connect to mark's home network (WPA2)
 */
//final String SSID = "RobotChicken";
//final String password = "X3bc57pl98";		
//wificonf.preSharedKey = "\"" + password + "\"";
//wificonf.status = WifiConfiguration.Status.ENABLED;
//wificonf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
//wificonf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
//wificonf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
//wificonf.allowedPairwiseCiphers
//		.set(WifiConfiguration.PairwiseCipher.TKIP);
//wificonf.allowedPairwiseCiphers
//		.set(WifiConfiguration.PairwiseCipher.CCMP);
//wificonf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);

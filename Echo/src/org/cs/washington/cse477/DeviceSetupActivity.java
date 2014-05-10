package org.cs.washington.cse477;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

	protected ConnectivityManager conMgr = null;
	protected WifiManager wifiManager = null;

	protected List<WifiConfiguration> wifiConfigurations;
	protected List<ScanResult> scanResults;
	protected List<String> stringScanResults;
	protected Spinner networksSpinner;
	protected Spinner securitySpinner;
	protected ArrayAdapter<String> networksAdapter;
	protected ArrayAdapter<String> securityAdapter;

	protected WifiConnectionEstablishedReceiver wifiWaiter= null;
	protected WifiScanResultsReceiver receiverWifi = null;

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
		super.onPause();
		unregisterReceivers();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceivers();	
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
		if (receiverWifi == null) {
			receiverWifi = new WifiScanResultsReceiver();
			registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		}

		if (wifiWaiter == null) {
			wifiWaiter = new WifiConnectionEstablishedReceiver();
			registerReceiver(wifiWaiter, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		}
	}
	
	protected void unregisterReceivers() {
		try {
			unregisterReceiver(receiverWifi);
			unregisterReceiver(wifiWaiter);
		} catch (IllegalArgumentException iae) {
			// do nothing...we don't really care if the receiver was already unregistered
		}
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
		// TODO: uncomment following three lines for in lab/with decive testing
//		final String SSID = "EchoSetup";
//		wificonf.SSID = "\"" + SSID + "\"";
//		wificonf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

		// TODO: comment out the following set of lines for in lab/weith device testing
		// DEBUG
		/*
		 * WiFi configuration to connect to mark's home network (WPA2)
		 */
		final String SSID = "RobotChicken";
		final String password = "X3bc57pl98";		
		wificonf.SSID = "\"" + SSID + "\"";
		wificonf.preSharedKey = "\"" + password + "\"";
		wificonf.status = WifiConfiguration.Status.ENABLED;
		wificonf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		wificonf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
		wificonf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
		wificonf.allowedPairwiseCiphers
				.set(WifiConfiguration.PairwiseCipher.TKIP);
		wificonf.allowedPairwiseCiphers
				.set(WifiConfiguration.PairwiseCipher.CCMP);
		wificonf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
		
		
		// fetch list of all configured networks on this phone 
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

	private static final String WIFI_CON_EST_TAG = "WifiConnectionEstablishedReceiver";
	class WifiConnectionEstablishedReceiver extends BroadcastReceiver {
		public void onReceive(Context c, Intent intent) {
			Log.v(WIFI_CON_EST_TAG, "onReceive() called");

			NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
			if (activeNetwork != null && !connectedToFlyPort) {
				boolean isWiFi = (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI);
				boolean isConnected = (activeNetwork.isConnected() && activeNetwork.getState() == NetworkInfo.State.CONNECTED);
				if (isWiFi && isConnected) {
					WifiInfo wifi = wifiManager.getConnectionInfo();
					if (wifi.getSSID().equals("\"RobotChicken\""/*"\"EchoSetup\""*/)) {
						Log.v(WIFI_CON_EST_TAG, "Connected to EchoSetup!");
						
						connectedToFlyPort = true;
						// send post message to FlyPort device
						//postComplete = sendConfigurationPOST();
						
						// DEBUG
						postComplete = true;
						
						Log.v(TAG,"POST status: " + postComplete);
						
						if (savedNetwork != -1) {
							Log.v(WIFI_CON_EST_TAG, "reconnecting to saved wifi network");
							wifiManager.enableNetwork(savedNetwork, true);
							//wifiManager.reconnect();
							reenableSavedNetworks();
						}
						
						Intent i = new Intent(c, NotificationActivity.class);
				    	startActivity(i);
						
					} else {
						Log.v(WIFI_CON_EST_TAG, "Connected to " + wifi.getSSID());
					}
				} else if (isWiFi && !isConnected) {
					Log.v(WIFI_CON_EST_TAG, "ActiveNetwork is WiFi, but state is: "
							+ activeNetwork.getDetailedState().toString() + " connected to FlyPort: " + connectedToFlyPort);
				} else if (!isWiFi) {
					Log.v(WIFI_CON_EST_TAG, "ActiveNetwork is not WiFi");
				} else {
					Log.v(WIFI_CON_EST_TAG, "ActiveNetwork is not WiFi or is not Connected");
				}
			} else {
				Log.v(WIFI_CON_EST_TAG, "ActiveNetworkInfo == null");
			}
		}
	}
	
	public void reenableSavedNetworks() {
		for (WifiConfiguration w : wifiManager.getConfiguredNetworks()) {
			wifiManager.enableNetwork(w.networkId, false);
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

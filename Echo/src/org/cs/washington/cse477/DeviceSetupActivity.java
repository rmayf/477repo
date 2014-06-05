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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

public class DeviceSetupActivity extends ActionBarActivity {

	protected ConnectivityManager conManager = null;
	protected WifiManager wifiManager = null;

	protected List<WifiConfiguration> wifiConfigurations = null;
	protected List<ScanResult> scanResults = null;
	protected List<String> stringScanResults = null;
	protected Spinner networksSpinner = null;
	protected Spinner securitySpinner = null;
	protected ArrayAdapter<String> networksAdapter = null;
	protected ArrayAdapter<String> securityAdapter = null;

	protected WifiConnectionEstablishedReceiver connectionEstablishedReceiver = null;
	protected WifiScanResultsReceiver scanResultReceiver = null;

	protected boolean connectedToFlyPort = false;
	protected boolean postComplete = false;

	protected int savedNetworkID = -1;
	protected String SSID_user = "";
	protected String pass_user = "";
	protected String encr_user = "";
	
	private static final String TAG = "DeviceSetupActivity";
	private static final String WIFI_CON_EST_TAG = "WifiConnectionEstablishedReceiver";
	private static final String WIFI_SCAN_TAG = "WifiScanResultsReceiver";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_device_setup);

		init();
	}

	/**
	 * initialize resources for this activity
	 */
	protected void init() {
		setupNetworkManagers();
		registerReceivers();
		initWifiScan(null);
		setupUI();
	}
	
	/**
	 * initialize network system service instances for wifi and connectivity
	 */
	protected void setupNetworkManagers() {
		// grab handle to ConnectivityManager
		if (conManager == null) {
			conManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		}
		if (conManager == null) {
			Log.e(TAG, "failed to acquire ConnectivityManager");
		}
		
		// grab handle to system WifiManager
		if (wifiManager == null) {
			wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		}
		if (wifiManager == null) {
			Log.e(TAG, "failed to acquire WifiManager");
		}
	}
	
	/**
	 * create and register custom broadcast receivers
	 */
	protected void registerReceivers() {
		if (scanResultReceiver == null) {
			scanResultReceiver = new WifiScanResultsReceiver();
			registerReceiver(scanResultReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			Log.v(TAG,"registered scanResultReceiver");
		}

		if (connectionEstablishedReceiver == null) {
			connectionEstablishedReceiver = new WifiConnectionEstablishedReceiver();
			registerReceiver(connectionEstablishedReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
			Log.v(TAG,"registered connectionEstablishedReceiver");
		}
	}
	
	/**
	 * unregister custom broadcast receivers
	 */
	protected void unregisterReceivers() {
		try {
			unregisterReceiver(scanResultReceiver);
		} catch (IllegalArgumentException iae) {
			// do nothing...we don't really care if the receiver was already unregistered or null
			Log.v(TAG,"failed to unregister scanResultReceiver with: " + iae.getMessage());
		}
		try {
			unregisterReceiver(connectionEstablishedReceiver);
		} catch (IllegalArgumentException iae) {
			// do nothing...we don't really care if the receiver was already unregistered or null
			Log.v(TAG,"failed to unregister connectionEstablishedReceiver with: " + iae.getMessage());
		}
	}

	
	/**
	 * initiate scan for available WiFi networks
	 * 
	 * called onClick of refresh button
	 */
	public void initWifiScan(View v) {
		if (wifiManager != null) {
			wifiManager.startScan();
		}
	}
	
	/**
	 * setup UI elements for DeviceSetupActivity
	 */
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
		networksSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> adview, View view, int pos,
					long id) {
				// on item selected, update SSID_user to selected item
				SSID_user = stringScanResults.get(pos);
				Log.v(TAG,"SSID_user = " + SSID_user);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// do nothing
			}
		});

		// create and set adapter for security spinner
		final String[] secArray = new String[] { "Open", "WPA", "WPA2" };
		securitySpinner = (Spinner) findViewById(R.id.dev_setup_security_spinner);
		securityAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_dropdown_item,
				android.R.id.text1, secArray);
		securitySpinner.setAdapter(securityAdapter);
		securitySpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> adview, View view, int pos,
					long id) {
				// on item selected, update encr_user to selected item
				encr_user = secArray[pos];
				Log.v(TAG,"encr_user = " + encr_user);
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// do nothing
			}
		});
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
		init();
	}

	class EchoSetupConnector extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			// if EchoSetup is in range, begin connection process
			if (checkEchoSetupInRange()) {
				// save current network id if present
				WifiInfo currentWifi = wifiManager.getConnectionInfo();
				if (currentWifi != null) {
					savedNetworkID = currentWifi.getNetworkId();
				}

				// configure a new WifiConfiguration for EchoSetup
				WifiConfiguration wificonf = new WifiConfiguration();
				final String SSID = "EchoSetup";
				wificonf.SSID = "\"" + SSID + "\"";
				wificonf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

				// fetch list of all configured networks on this phone and remove
				// any that match on SSID with EchoSetup
				wifiConfigurations = wifiManager.getConfiguredNetworks();
				for (WifiConfiguration i : wifiConfigurations) {
					if (i.SSID != null && i.SSID.equals("\"" + SSID + "\"")) {
						wifiManager.removeNetwork(i.networkId);
					}
				}
				// add new configuration to WiFi manager, disconnect,
				// and initiate connection to EchoSetup
				int newNetwork = wifiManager.addNetwork(wificonf);
				wifiManager.disconnect();
				wifiManager.enableNetwork(newNetwork, true);

				// reenable all configured networks 
				//enableAllConfiguredNetworks();
				
				Log.v(TAG, "connecting...");
			} else {
				initWifiScan(null);
				// notify user that EchoSetup not in range, press refresh
				Log.v(TAG, "could not detect EchoSetup...did not attempt to connect");
			}
			return null;
		}		
	}
	
	private boolean checkEchoSetupInRange() {
		boolean inRange = false;
		for (ScanResult s : scanResults) {
			if (s.SSID.equals("EchoSetup")) inRange = true;
		}
		return inRange;
	}
	
	/**
	 * re-enable all wifi networks
	 */
	public void reenableSavedWifiNetwork() {
		Log.v(TAG,"re-enabling saved wifi network");
		(new wifiConnectionRestorer()).execute();
	}
	
	protected void enableAllConfiguredNetworks() {
		for (WifiConfiguration w : wifiManager.getConfiguredNetworks()) {
			wifiManager.enableNetwork(w.networkId, false);
		}
	}
	
	class wifiConnectionRestorer extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			// reconnect to old network
			if (savedNetworkID != -1) {
				Log.v(WIFI_CON_EST_TAG, "reconnecting to saved wifi network");
				wifiManager.enableNetwork(savedNetworkID, true);	
			}
			enableAllConfiguredNetworks();
			return null;
		}		
	}
	
	/**
	 * Called on Configure Device button click
	 * 
	 * This method starts an AsyncTask to initiate the one click device setup
	 */
	public void configureFlyport(View view) {
		// set network password from user input
		pass_user = ((EditText) findViewById(R.id.dev_setup_password)).getText().toString();
		
		Log.v(TAG, "Attempting to connect with:\nSSID: " + SSID_user + "\nEncryption: " + encr_user + "\nPassword: " + pass_user);
		
		// connect to EchoSetup
		AsyncTask<Void, Void, Void> task = new EchoSetupConnector();
		task.execute();
		
		// connectionEstablishedReceiver handles starting an AsyncTask and sending Http POST
	}

	/**
	 * send POST to FlyPort
	 * 
	 * parses user input and calls HttpPOSTGen functions to perform the post
	 */
	public boolean sendConfigurationPOST(String SSID_user, String pass_user, String encr_user) {
		
		String stat = "SSID: " + SSID_user + "\nEncryption: " + encr_user + "\nPassword: " + pass_user;
		Log.v(TAG,"sendConfigurationPOST():\n" + stat);
		
		final String[] params = {SSID_user, pass_user, encr_user};
		boolean res = false;
		try {
			res =((new HttpPOSTAsyncTask()).execute(params)).get().booleanValue();
		} catch (Exception e) {
			Log.e(TAG,"Exception executing AsyncTask: " + e.getMessage());
			return false;
		}
		return res;
	}

	

	
/***************************************************************************************
 *                           Broadcast Receivers                                       *
 ***************************************************************************************/
	
	/**
	 * Broadcast Receiver for WiFi Scan Result Event
	 */
	class WifiScanResultsReceiver extends BroadcastReceiver {
		public void onReceive(Context c, Intent intent) {
			Log.v(WIFI_SCAN_TAG,"updating scan results spinner");
			scanResults = wifiManager.getScanResults();
			stringScanResults.clear();
			Iterator<ScanResult> iter = scanResults.iterator();
			while (iter.hasNext()) {
				stringScanResults.add(iter.next().SSID);
			}
			networksAdapter.notifyDataSetChanged();
		}
	}

		
	/**
	 * Broadcast Receiver for WiFi Connection Established Event
	 */
	class WifiConnectionEstablishedReceiver extends BroadcastReceiver {
		public void onReceive(Context c, Intent intent) {
			Log.v(WIFI_CON_EST_TAG, "onReceive() called");

			NetworkInfo activeNetwork = conManager.getActiveNetworkInfo();
			if (activeNetwork != null && !connectedToFlyPort) {
				boolean isWiFi = (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI);
				boolean isConnected = (activeNetwork.isConnected() && activeNetwork.getState() == NetworkInfo.State.CONNECTED);
				if (isWiFi && isConnected) {
					WifiInfo wifi = wifiManager.getConnectionInfo();
					if (wifi.getSSID().equals("\"EchoSetup\"")) {
						Log.v(WIFI_CON_EST_TAG, "Connected to EchoSetup!");
						

						connectedToFlyPort = true;

						// send post message to FlyPort device
						postComplete = sendConfigurationPOST(SSID_user,pass_user,encr_user);
						if (postComplete) {
							Log.v(WIFI_CON_EST_TAG,"configuration POST sent successfully");
						} else {
							Log.w(WIFI_CON_EST_TAG,"configuration POST failed to send successfully");
						}
						
						reenableSavedWifiNetwork();
						// start NotificationActivity after iniating reconnect
				    	startActivity(new Intent(c, NotificationActivity.class));
						
					} else {
						connectedToFlyPort = false;
						Log.v(WIFI_CON_EST_TAG, "Incorrect SSID, connected to " + wifi.getSSID());
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
	
	
}

///*
//* WiFi configuration to connect to mark's home network (WPA2)
//*/
//final String SSID = "RobotChicken";
//final String password = "X3bc57pl98";		
//wificonf.SSID = "\"" + SSID + "\"";
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
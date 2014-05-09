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
	private boolean setupComplete = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
				.permitAll().build();
		StrictMode.setThreadPolicy(policy);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_setup);

		setupManagers();
		setupUI();
		setupReceivers();
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
	protected void onResume() {
		setupReceivers();
		super.onResume();
	}

	protected boolean refreshScan(View v) {
		return wifiManager.startScan();
	}

	protected void setupUI() {
		
		// collect scan results and store
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
		String[] secArray = new String[] { "WEP", "WPA", "WPA2" };
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
	public void configureFlyport(View view) {
		// save info to send to FlyPort
		Log.e("DEBUG", "Configure pressed");
		if (wifiManager == null) {
			wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		}
		if (wifiManager == null) {
			Log.e("DEBUG", "failed to get WifiManager");
			return;
		}

		final String SSID = "EchoSetup";
//		final String SSID = "RobotChicken";
//		final String password = "X3bc57pl98";
		
		// Connect to EchoSetup
		WifiConfiguration wificonf = new WifiConfiguration();
		wificonf.SSID = "\"" + SSID + "\"";
		wificonf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		
//		wificonf.preSharedKey = "\"" + password + "\"";
//		wificonf.status = WifiConfiguration.Status.ENABLED;
//		wificonf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
//		wificonf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
//		wificonf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
//		wificonf.allowedPairwiseCiphers
//				.set(WifiConfiguration.PairwiseCipher.TKIP);
//		wificonf.allowedPairwiseCiphers
//				.set(WifiConfiguration.PairwiseCipher.CCMP);
//		wificonf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);

		wifiConfigurations = wifiManager.getConfiguredNetworks();

		// remove saved configuration for any matching SSID
		for (WifiConfiguration i : wifiConfigurations) {
			if (i.SSID != null && i.SSID.equals("\"" + SSID + "\"")) {
				wifiManager.removeNetwork(i.networkId);
			}
		}
		
		// add new configuration to WiFi manager
		wifiManager.addNetwork(wificonf);

		// connect to new configuration
		for (WifiConfiguration i : wifiConfigurations) {
			if (i.SSID != null && i.SSID.equals("\"" + SSID + "\"")) {
				wifiManager.disconnect();
				wifiManager.enableNetwork(i.networkId, true);
				wifiManager.reconnect();
				break;
			}
		}
		Log.e("DEBUG", "connecting...");
		// After reconnecting, the WifiConnectionEstablishedReceiver will handle
		// sending the POST to the FlyPort
	}

	public boolean sendConfigurationPOST() {
		String SSID_user = networksAdapter.getItem(networksSpinner.getSelectedItemPosition());
		String pass_user = ((EditText) findViewById(R.id.dev_setup_password)).toString();
		String encr_user = securityAdapter.getItem(securitySpinner.getSelectedItemPosition());
		
		String stat = SSID_user + " " + pass_user + " " + encr_user;
		Log.e("DEBUG",stat);
		
		int attempts = 0;
		final int MAX_ATTEMPTS = 3;
		boolean res = false;
		while (!res && attempts < MAX_ATTEMPTS) {
			try {
				attempts++;
				//res = postMessage(SSID_user, encr_user, pass_user);
			} catch (Exception e) {

			}
		}
		return res;
	}

	public boolean postMessage(String SSID, String encryption, String pass) {
		Log.e("DEBUG", "postMessage() called");
		// send POST to FlyPort to configure
		// 192.168.1.13
		AndroidHttpClient client = AndroidHttpClient.newInstance("Echo");
		
		HttpPost post = null;
		try {
			post = new HttpPost(new URI("http://192.168.1.13/"));
		} catch (Exception e) {

		}

		try {
			// Add your data
			post.addHeader("Content-Type", "application/x-www-form-urlencoded");
			post.addHeader("", "");

			/*
			 * NETTYPE=infra &DHCPCL=enabled &SSID=big+titays &SECTYPE=OPEN
			 * &WEP40KEY1= &WEP40KEY2= &WEP40KEY3= &WEP40KEY4= &WEP40KEYID=
			 * &WEP104KEY= &WPAPASS= &WPA2PASS=
			 */

			ArrayList<NameValuePair> nameValuePair = new ArrayList<NameValuePair>();
			nameValuePair.add(new BasicNameValuePair("NETTYPE", "infra"));
			nameValuePair.add(new BasicNameValuePair("DHCPCL", "enabled"));

			// set SSID
			nameValuePair.add(new BasicNameValuePair("SSID", SSID));

			// set security
			
			nameValuePair.add(new BasicNameValuePair("SECTYPE", "OPEN"));

			nameValuePair.add(new BasicNameValuePair("WEP40KEY1", ""));
			nameValuePair.add(new BasicNameValuePair("WEP40KEY2", ""));
			nameValuePair.add(new BasicNameValuePair("WEP40KEY3", ""));
			nameValuePair.add(new BasicNameValuePair("WEP40KEY4", ""));
			nameValuePair.add(new BasicNameValuePair("WEP40KEYID", ""));
			nameValuePair.add(new BasicNameValuePair("WEP104KEY", ""));
			nameValuePair.add(new BasicNameValuePair("WPAPASS", ""));
			nameValuePair.add(new BasicNameValuePair("WPA2PASS", ""));

			// add entity to post
			post.setEntity(new UrlEncodedFormEntity(nameValuePair));

			for (Header h : post.getAllHeaders()) {
				Log.e("DEBUG", h.toString());
			}

			// Execute HTTP Post Request
			HttpResponse response = client.execute(post);
			Log.e("DEBUG", "POST executed");
			return true;
		} catch (ClientProtocolException e) {
			Log.e("DEBUG", "ClientProtocolException");
		} catch (IOException e) {
			Log.e("DEBUG", "IOException");
			Log.e("DEBUG", e.getMessage());
		}

		return false;
		
		// go back to notifications
		// Intent intent_n = new Intent(getApplicationContext(),
		// NotificationActivity.class);
		// startActivity(intent_n);
		
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
			Log.e("DEBUG", "receiver called");

			NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
			if (activeNetwork != null) {
				boolean isWiFi = (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI);
				boolean isConnected = (activeNetwork.isConnected() && activeNetwork
						.getState() == NetworkInfo.State.CONNECTED);
				if (isWiFi && isConnected) {
					WifiInfo wifi = wifiManager.getConnectionInfo();
					if (wifi.getSSID().equals("EchoSetup")) {
						Log.e("DEBUG", "Connected to EchoSetup!");
						
						connectedToFlyPort = true;
						// send post message to FlyPort device
						do {
							setupComplete = sendConfigurationPOST();
						} while (!setupComplete);

					} else {
						Log.e("DEBUG", "Connected to " + wifi.getSSID());
					}
				} else if (isWiFi && !isConnected) {
					Log.e("DEBUG", "is WiFi, state is: "
							+ activeNetwork.getDetailedState().toString());
				} else if (!isWiFi) {
					Log.e("DEBUG", "not WiFi");
				} else {
					Log.e("DEBUG", "not WiFi or not Connected");
				}
			} else {
				Log.e("DEBUG", "ActiveNetworkInfo == null");
			}
		}
	}
}

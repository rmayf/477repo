package org.cs.washington.cse477;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.app.FragmentManager;
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
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.graphics.Color;

public class DeviceSetupActivity extends ActionBarActivity {

	private static final String LOG_TAG = "DeviceSetupActivity";
	private static final String WIFI_CON_EST_TAG = "WifiConnectionEstablishedReceiver";
	private static final String WIFI_SCAN_TAG = "WifiScanResultsReceiver";
	
	protected ConnectivityManager mConManager = null;
	protected WifiManager mWifiManager = null;

	protected List<WifiConfiguration> mWifiConfigurations = null;
	protected List<ScanResult> mScanResults = null;
	protected List<String> mStringScanResults = null;
	protected Spinner mNetworksSpinner = null;
	protected Spinner mSecuritySpinner = null;
	protected ArrayAdapter<String> mNetworksAdapter = null;
	protected ArrayAdapter<String> mSecurityAdapter = null;
	protected ImageView mRefreshImg = null;
	protected EditText mPassEditText = null;
	protected TextView mPassTextView = null;
	
	protected static WifiConnectionEstablishedReceiver mConnectionEstablishedReceiver = null;
	protected static WifiScanResultsReceiver mScanResultReceiver = null;

	protected boolean mConnectedToFlyPort = false;
	protected boolean mPostComplete = false;

	protected int mSavedNetworkID = -1;
	protected String mSSID_user = "";
	protected String mPass_user = "";
	protected String mEncr_user = "";
	protected static final String[] mSecurityArray = new String[] { "WPA2", "WPA", "Open" };
	
	protected final FragmentManager mFragmentManager = getFragmentManager();
	protected static final DeviceSetupErrorDialog mError_dlg = new DeviceSetupErrorDialog();
	
	/**
	 * called on activity creation
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_device_setup);

		/**
		 * Init
		 */
		mConManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		
		setupUI();
	}

	/**
	 * called when activity pauses, attempt to unregister receivers
	 */
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceivers();
	}
	
	/**
	 * called when activity resumes, register receivers
	 */
	@Override
	protected void onResume() {
		super.onResume();
		registerReceivers();
		initWifiScan(null);
		mRefreshImg.setImageAlpha(255);
	}
	
	/**
	 * setup UI elements for DeviceSetupActivity
	 */
	protected void setupUI() {
		
		mPassEditText = (EditText) findViewById(R.id.dev_setup_password);
		mRefreshImg = (ImageView) findViewById(R.id.dev_setup_refresh_networks_b);
		mPassTextView = (TextView) findViewById(R.id.dev_setup_password_prompt);
		
		// collect initial scan results and store
		mScanResults = mWifiManager.getScanResults();
		mStringScanResults = new LinkedList<String>();
		Iterator<ScanResult> iter = mScanResults.iterator();
		while (iter.hasNext()) {
			ScanResult sr = iter.next();
			mStringScanResults.add(sr.SSID);
		}

		// grab handle to networks spinner
		mNetworksSpinner = (Spinner) findViewById(R.id.dev_setup_networks_spinner);
		// create and set adapter for network spinner
		mNetworksAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_dropdown_item,
				android.R.id.text1, mStringScanResults);
		mNetworksSpinner.setAdapter(mNetworksAdapter);
		mNetworksSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> adview, View view, int pos,
					long id) {
				// on item selected, update SSID_user to selected item
				mSSID_user = mStringScanResults.get(pos);
				Log.v(LOG_TAG,"SSID_user = " + mSSID_user);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// do nothing
			}
		});

		// create and set adapter for security spinner
		mSecuritySpinner = (Spinner) findViewById(R.id.dev_setup_security_spinner);
		mSecurityAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_dropdown_item,
				android.R.id.text1, mSecurityArray);
		mSecuritySpinner.setAdapter(mSecurityAdapter);
		mSecuritySpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> adview, View view, int pos,
					long id) {
				// on item selected, update encr_user to selected item
				mEncr_user = mSecurityArray[pos];
				Log.v(LOG_TAG,"encr_user = " + mEncr_user);
				
				if (mEncr_user.equalsIgnoreCase("open")) {
					mPassEditText.setEnabled(false);
					mPassTextView.setTextColor(Color.LTGRAY);
				} else {
					mPassEditText.setEnabled(true);
					mPassTextView.setTextColor(Color.BLACK);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// do nothing
			}
		});
	}
	
	/**
	 * create and register custom broadcast receivers
	 */
	protected void registerReceivers() {
		if (mScanResultReceiver == null) {
			mScanResultReceiver = new WifiScanResultsReceiver();
			registerReceiver(mScanResultReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			Log.v(LOG_TAG,"registered scanResultReceiver");
		}

		if (mConnectionEstablishedReceiver == null) {
			mConnectionEstablishedReceiver = new WifiConnectionEstablishedReceiver();
			registerReceiver(mConnectionEstablishedReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
			Log.v(LOG_TAG,"registered connectionEstablishedReceiver");
		}
	}
	
	/**
	 * unregister custom broadcast receivers
	 */
	protected void unregisterReceivers() {
		try {
			unregisterReceiver(mScanResultReceiver);
		} catch (IllegalArgumentException iae) {
			// do nothing...we don't really care if the receiver was already unregistered or null
			Log.v(LOG_TAG,"failed to unregister scanResultReceiver with: " + iae.getMessage());
		}
		try {
			unregisterReceiver(mConnectionEstablishedReceiver);
		} catch (IllegalArgumentException iae) {
			// do nothing...we don't really care if the receiver was already unregistered or null
			Log.v(LOG_TAG,"failed to unregister connectionEstablishedReceiver with: " + iae.getMessage());
		}
	}
	
	/**
	 * initiate scan for available WiFi networks
	 * 
	 * called onClick of refresh button
	 */
	public void initWifiScan(View v) {
		Log.v(LOG_TAG,"initWifiScan() called");
		if (mWifiManager != null) {
			mWifiManager.startScan();
		}
		mRefreshImg.setImageAlpha(128);
	}

	class EchoSetupConnector extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			// if EchoSetup is in range, begin connection process
			if (checkEchoSetupInRange()) {
				// save current network id if present
				WifiInfo currentWifi = mWifiManager.getConnectionInfo();
				if (currentWifi != null) {
					mSavedNetworkID = currentWifi.getNetworkId();
				}

				// configure a new WifiConfiguration for EchoSetup
				WifiConfiguration wificonf = new WifiConfiguration();
				final String SSID = "EchoSetup";
				wificonf.SSID = "\"" + SSID + "\"";
				wificonf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

				// fetch list of all configured networks on this phone and remove
				// any that match on SSID with EchoSetup
				mWifiConfigurations = mWifiManager.getConfiguredNetworks();
				for (WifiConfiguration i : mWifiConfigurations) {
					if (i.SSID != null && i.SSID.equals("\"" + SSID + "\"")) {
						mWifiManager.removeNetwork(i.networkId);
					}
				}
				// add new configuration to WiFi manager, disconnect,
				// and initiate connection to EchoSetup
				int newNetwork = mWifiManager.addNetwork(wificonf);
				mWifiManager.disconnect();
				mWifiManager.enableNetwork(newNetwork, true);

				// reenable all configured networks 
				//enableAllConfiguredNetworks();
				
				Log.v(LOG_TAG, "connecting...");
			} else {
				// notify user that EchoSetup not in range, press refresh
				Log.v(LOG_TAG, "could not detect EchoSetup...did not attempt to connect");
				mError_dlg.show(getFragmentManager(), "devicesetuperrordialog");
			}
			return null;
		}		
	}
	
	private boolean checkEchoSetupInRange() {
		boolean inRange = false;
		for (ScanResult s : mScanResults) {
			if (s.SSID.equals("EchoSetup")) inRange = true;
		}
		return inRange;
	}
	
	/**
	 * re-enable all wifi networks
	 */
	public void reenableSavedWifiNetwork() {
		Log.v(LOG_TAG,"re-enabling saved wifi network");
		(new wifiConnectionRestorer()).execute();
	}
	
	protected void enableAllConfiguredNetworks() {
		for (WifiConfiguration w : mWifiManager.getConfiguredNetworks()) {
			mWifiManager.enableNetwork(w.networkId, false);
		}
	}
	
	class wifiConnectionRestorer extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			// reconnect to old network
			if (mSavedNetworkID != -1) {
				Log.v(WIFI_CON_EST_TAG, "reconnecting to saved wifi network");
				mWifiManager.enableNetwork(mSavedNetworkID, true);	
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
		mPass_user = ((EditText) findViewById(R.id.dev_setup_password)).getText().toString();
		
		Log.v(LOG_TAG, "Attempting to connect with:\nSSID: " + mSSID_user + "\nEncryption: " + mEncr_user + "\nPassword: " + mPass_user);
		
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
		Log.v(LOG_TAG,"sendConfigurationPOST():\n" + stat);
		
		final String[] params = {SSID_user, pass_user, encr_user};
		boolean res = false;
		try {
			res =((new HttpPOSTAsyncTask()).execute(params)).get().booleanValue();
		} catch (Exception e) {
			Log.e(LOG_TAG,"Exception executing AsyncTask: " + e.getMessage());
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
			mScanResults = mWifiManager.getScanResults();
			mStringScanResults.clear();
			Iterator<ScanResult> iter = mScanResults.iterator();
			while (iter.hasNext()) {
				mStringScanResults.add(iter.next().SSID);
			}
			mNetworksAdapter.notifyDataSetChanged();
			mRefreshImg.setImageAlpha(255);
		}
	}

		
	/**
	 * Broadcast Receiver for WiFi Connection Established Event
	 */
	class WifiConnectionEstablishedReceiver extends BroadcastReceiver {
		public void onReceive(Context c, Intent intent) {
			Log.v(WIFI_CON_EST_TAG, "onReceive() called");

			NetworkInfo activeNetwork = mConManager.getActiveNetworkInfo();
			if (activeNetwork != null && !mConnectedToFlyPort) {
				boolean isWiFi = (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI);
				boolean isConnected = (activeNetwork.isConnected() && activeNetwork.getState() == NetworkInfo.State.CONNECTED);
				if (isWiFi && isConnected) {
					WifiInfo wifi = mWifiManager.getConnectionInfo();
					if (wifi.getSSID().equals("\"EchoSetup\"")) {
						Log.v(WIFI_CON_EST_TAG, "Connected to EchoSetup!");
						

						mConnectedToFlyPort = true;

						// send post message to FlyPort device
						mPostComplete = sendConfigurationPOST(mSSID_user,mPass_user,mEncr_user);
						if (mPostComplete) {
							Log.v(WIFI_CON_EST_TAG,"configuration POST sent successfully");
						} else {
							Log.w(WIFI_CON_EST_TAG,"configuration POST failed to send successfully");
						}
						
						reenableSavedWifiNetwork();
						// start NotificationActivity after iniating reconnect
				    	startActivity(new Intent(c, NotificationActivity.class));
						
					} else {
						mConnectedToFlyPort = false;
						Log.v(WIFI_CON_EST_TAG, "Incorrect SSID, connected to " + wifi.getSSID());
					}
				} else if (isWiFi && !isConnected) {
					mConnectedToFlyPort = false;
					Log.v(WIFI_CON_EST_TAG, "ActiveNetwork is WiFi, but state is: "
							+ activeNetwork.getDetailedState().toString() + " connected to FlyPort: " + mConnectedToFlyPort);
				} else if (!isWiFi) {
					mConnectedToFlyPort = false;
					Log.v(WIFI_CON_EST_TAG, "ActiveNetwork is not WiFi");
				} else {
					mConnectedToFlyPort = false;
					Log.v(WIFI_CON_EST_TAG, "ActiveNetwork is not WiFi or is not Connected");
				}
			} else {
				mConnectedToFlyPort = false;
				Log.v(WIFI_CON_EST_TAG, "ActiveNetworkInfo == null");
			}
		}
	}
}
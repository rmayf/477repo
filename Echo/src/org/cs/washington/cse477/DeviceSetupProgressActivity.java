package org.cs.washington.cse477;

import java.util.List;
import java.util.concurrent.TimeUnit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DeviceSetupProgressActivity extends ActionBarActivity {
	
	private static final String LOG_TAG = "DeviceSetupActivity";
	private static final String WIFI_CON_EST_TAG = "WifiConnectionEstablishedReceiver";
	
	protected final Context mContext = this;
	
	protected Bundle mBundle;
	
	protected ConnectivityManager mConManager = null;
	protected WifiManager mWifiManager = null;
	protected List<WifiConfiguration> mWifiConfigurations = null;
	
	protected WifiConnectionEstablishedReceiver mConnectionEstablishedReceiver = null;
	
	protected int mSavedNetworkID = -1;
	protected String mSSID_user = "";
	protected String mPass_user = "";
	protected String mEncr_user = "";
	
	protected boolean mAttemptSetup = false;
	protected boolean mConnectingToEchoSetup = false;
	protected boolean mConnectedToEchoSetup = false;
	protected boolean mPOSTComplete = false;
	protected boolean mWiFiConnectionRestored = false;
	
	protected ProgressBar mProgress;
	protected int mProgressStatus = 0;
    protected Handler mHandler = new Handler();
	
    protected TextView mTextViewDebug = null;
    
	/**
	 * called on activity creation
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.activity_device_setup_progress);

		/**
		 * Init
		 */	
		
		mConManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		
		// setup UI elements
		setupUI();
		
		mBundle = getIntent().getBundleExtra("args");
		if (mBundle == null) {
			mAttemptSetup = false;
		} else {
			mSSID_user = mBundle.getString("SSID");
			mEncr_user = mBundle.getString("encryption");
			mPass_user = mBundle.getString("password");
			mAttemptSetup = mBundle.getBoolean("connect");
		}

	}

	private void resetState() {
		mAttemptSetup = false;
		mConnectingToEchoSetup = false;
		mConnectedToEchoSetup = false;
		mPOSTComplete = false;
		mWiFiConnectionRestored = false;
	}
	
	/**
	 * called when activity pauses, attempt to unregister receivers
	 */
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceivers();
		resetState();
		Log.v(LOG_TAG, "onPause(): " + mAttemptSetup + " " + mConnectingToEchoSetup + " " + mConnectedToEchoSetup + " " + mPOSTComplete + " " + mWiFiConnectionRestored);
	}
	
	/**
	 * called when activity resumes, register receivers
	 * 
	 * if mAttemptSetup was set in Bundle args, begin connection attempt
	 */
	@Override
	protected void onResume() {
		super.onResume();
		registerReceivers();
		Log.v(LOG_TAG, "onResume(): " + mAttemptSetup + " " + mConnectingToEchoSetup + " " + mConnectedToEchoSetup + " " + mPOSTComplete + " " + mWiFiConnectionRestored);
		// attempt to start connection, checking boolean to avoid unwanted attempts
		if (mAttemptSetup) {
			mAttemptSetup = false;
			mProgress.setProgress(0);
			mProgress.setMax(4);
			
			mTextViewDebug.setText("Launching EchoSetupConnetor...");
			
			// launch EchoSetupConnector AsyncTask
			AsyncTask<Void, String, Void> task = new EchoSetupConnector();
			task.execute();
			
		} else {
			Log.e(LOG_TAG, "argument Bundle not received, not attempting setup, returning to DeviceSetupActivity");
			Bundle args = new Bundle();
			args.putBoolean("setupError", true);
			args.putString("msg", "argument Bundle missing");
			startActivity((new Intent(this, DeviceSetupActivity.class))
					.putExtra("args",args));
		}
	}
	
	/**
	 * setup the UI
	 */
	protected void setupUI() {
		mProgress = (ProgressBar) findViewById(R.id.dev_setup_prog_bar);
		mTextViewDebug = (TextView) findViewById(R.id.dev_setup_prog_debug);
	}
	
	/**
	 * create and register custom broadcast receivers
	 */
	protected void registerReceivers() {
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
			unregisterReceiver(mConnectionEstablishedReceiver);
		} catch (IllegalArgumentException iae) {
			// do nothing...we don't really care if the receiver was already unregistered or null
			Log.v(LOG_TAG,"failed to unregister connectionEstablishedReceiver with: " + iae.getMessage());
		}
	}
	
	
/***************************************************************************************
 *                   AsyncTask Classes and setup functions                             *
 ***************************************************************************************/

	/**
	 * save old WiFi connection, attempt to connect to EchoSetup WiFi
	 */
	class EchoSetupConnector extends AsyncTask<Void, String, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			
			mTextViewDebug.setText("EchoSetupConnetor working...");
			
			// if EchoSetup is in range, begin connection process
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
			
			mConnectingToEchoSetup = true;
			Log.v(LOG_TAG, "Attempting to connect with:\nSSID: " + mSSID_user + "\nEncryption: " + mEncr_user + "\nPassword: " + mPass_user);
			mProgress.setProgress(1);
			
			mWifiManager.enableNetwork(newNetwork, true);
			
			publishProgress("EchoSetup WiFi Configuration enabled...");
			
			return null;
		}
		protected void onProgressUpdate(String... progress) {
			mTextViewDebug.setText(progress[0]);
	    }
	}

	/**
	 * restore original WiFi connection
	 */
	public class wifiConnectionRestorer extends AsyncTask<Void, String, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			mConnectedToEchoSetup = false;
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
			res =((new HttpPOSTAsyncTask()).execute(params)).get(30,TimeUnit.SECONDS).booleanValue();
		} catch (Exception e) {
			Log.e(LOG_TAG,"Exception executing AsyncTask: " + e.getMessage());
			return false;
		}
		return res;
	}
	
	/**
	 * re-enable all wifi networks
	 */
	public void reenableSavedWifiNetwork() {
		Log.v(LOG_TAG,"Reconnecting to saved WiFi");
		mTextViewDebug.setText("Reconnecting to saved WiFi");
		mProgress.setProgress(4);
		(new wifiConnectionRestorer()).execute();
	}
	
	protected void enableAllConfiguredNetworks() {
		for (WifiConfiguration w : mWifiManager.getConfiguredNetworks()) {
			mWifiManager.enableNetwork(w.networkId, false);
		}
	}

	
/***************************************************************************************
 *                           Broadcast Receivers                                       *
 ***************************************************************************************/	
	
	/**
	 * Broadcast Receiver for WiFi Connection Established Event
	 */
	class WifiConnectionEstablishedReceiver extends BroadcastReceiver {
		public void onReceive(Context c, Intent intent) {
			Log.v(WIFI_CON_EST_TAG, "onReceive() called, top");
			
			NetworkInfo activeNetwork = mConManager.getActiveNetworkInfo();
			if (mConnectingToEchoSetup && !mConnectedToEchoSetup && activeNetwork != null) {
				Log.v(WIFI_CON_EST_TAG, "onReceive() called, checking if connected to EchoSetup");
				
				boolean isWiFi = (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI);
				boolean isConnected = (activeNetwork.isConnected() && activeNetwork.getState() == NetworkInfo.State.CONNECTED);
				
				// on WiFi and connected to a network
				if (isWiFi && isConnected) {
					WifiInfo wifi = mWifiManager.getConnectionInfo();
					
					if (wifi.getSSID().equals("\"EchoSetup\"")) {
						mConnectingToEchoSetup = false;
						mConnectedToEchoSetup = true;
						
						mProgress.setProgress(2);
						mTextViewDebug.setText("Connected to EchoSetup, preparing to send POST");
						
						Log.v(WIFI_CON_EST_TAG, "Connected to EchoSetup!");
						
						mTextViewDebug.setText("Sending Configuration POST");
						
						// send post message to FlyPort device
						mPOSTComplete = sendConfigurationPOST(mSSID_user,mPass_user,mEncr_user);
						if (mPOSTComplete) {
							mProgress.setProgress(3);
							mTextViewDebug.setText("Configuration POST sent successfully");
							Log.v(WIFI_CON_EST_TAG,"configuration POST sent successfully");
							reenableSavedWifiNetwork();	
							startActivity(new Intent(mContext, SettingsActivity.class));
						} else {
							Log.e(WIFI_CON_EST_TAG,"configuration POST failed to send successfully");
							reenableSavedWifiNetwork();	
							
							Bundle args = new Bundle();
							args.putBoolean("setupError", true);
							args.putString("msg", "configuration POST failed");
							startActivity((new Intent(c, DeviceSetupActivity.class))
									.putExtra("args",args));
						}
//					} else if (mRestoringWifi && wifi.getNetworkId() == mSavedNetworkID) {
//						mRestoringWifi = false;
//						mWiFiConnectionRestored = true;
//						mProgress.setProgress(4);
//						mTextViewDebug.setText("Re-established saved WiFi connection");
//						startActivity(new Intent(mContext, SettingsActivity.class));
					} else {
						Log.v(WIFI_CON_EST_TAG, "Incorrect SSID, connected to " + wifi.getSSID());
					}
				} else {
					Log.v(WIFI_CON_EST_TAG, "Attempting to connect to EchoSetup, but ActiveNetwork is not WiFi or is not Connected");
				}
			} else {
				Log.v(WIFI_CON_EST_TAG, "Nothing to do in WiFiConnectionEstablishedReceiver");
			}
		}
	}
}
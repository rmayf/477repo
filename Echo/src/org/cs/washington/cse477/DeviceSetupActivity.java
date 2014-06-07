package org.cs.washington.cse477;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
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
	private static final String WIFI_SCAN_TAG = "WifiScanResultsReceiver";
	private static final String SETUP_NETWORK = "EchoSetup";
	private static final int DEFAULT_CAPACITY = 10;
	
	protected ConnectivityManager mConManager = null;
	protected WifiManager mWifiManager = null;

	protected List<WifiConfiguration> mWifiConfigurations = null;
	protected List<ScanResult> mScanResults = null;

	protected Spinner mNetworksSpinner = null;
	protected Spinner mSecuritySpinner = null;
	protected ArrayAdapter<ScanResult> mNetworksAdapter = null;
	protected ArrayAdapter<String> mSecurityAdapter = null;
	protected ImageView mRefreshImg = null;
	protected EditText mPassEditText = null;
	protected TextView mPassTextView = null;
	
	protected WifiScanResultsReceiver mScanResultReceiver = null;;

	protected boolean mConnectedToFlyPort = false;
	protected boolean mPostComplete = false;

	protected int mSavedNetworkID = -1;
	protected String mSSID_user = "";
	protected String mPass_user = "";
	protected String mEncr_user = "";
	protected static final String[] mSecurityArray = new String[] { "WPA2", "WPA", "Open" };
	
	protected final FragmentManager mFragmentManager = getFragmentManager();
	protected static final DeviceSetupErrorDialog mError_dlg = new DeviceSetupErrorDialog();
	protected static final ConfigureErrorDialog mConfError_dlg = new ConfigureErrorDialog();
	
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
		
		try {
			unregisterReceiver(mScanResultReceiver);
		} catch (IllegalArgumentException iae) {
			// do nothing...we don't really care if the receiver was already unregistered or null
			Log.v(LOG_TAG,"failed to unregister scanResultReceiver with: " + iae.getMessage());
		}
	}
	
	/**
	 * called when activity resumes, register receivers
	 */
	@Override
	protected void onResume() {
		super.onResume();
		
		Bundle args = getIntent().getBundleExtra("args");
		if (args != null && args.getBoolean("setupError")) {
			Log.e(LOG_TAG,"setupError encountered: " + args.getString("msg"));
			mConfError_dlg.show(getFragmentManager(), "configureerrordialog");
		}
		
		if (mScanResultReceiver == null) {
			mScanResultReceiver = new WifiScanResultsReceiver();
			registerReceiver(mScanResultReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			Log.v(LOG_TAG,"registered scanResultReceiver");
		}
		
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
		mScanResults = new ArrayList<ScanResult>(DEFAULT_CAPACITY);
		mScanResults.addAll(mWifiManager.getScanResults());
		
		Iterator<ScanResult> iter = mScanResults.iterator();
		while (iter.hasNext()) {
			ScanResult sr = iter.next();
			if (sr.SSID.equalsIgnoreCase("") || sr.SSID.isEmpty()) {
				try {
					iter.remove();
				} catch (Exception e) {
					Log.e(WIFI_SCAN_TAG, "error removing network with no SSID from mScanResults");
				}
			}
		}
		
		
		// grab handle to networks spinner
		mNetworksSpinner = (Spinner) findViewById(R.id.dev_setup_networks_spinner);
		// create and set adapter for network spinner
		mNetworksAdapter = new DeviceSetupNetworksAdapter(this, R.layout.networks_listview_item, mScanResults);
		mNetworksSpinner.setAdapter(mNetworksAdapter);
		mNetworksSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> adview, View view, int pos,
					long id) {
				// on item selected, update SSID_user to selected item
				try {
					mSSID_user = mScanResults.get(pos).SSID;
				} catch (Exception e) {
					Log.e(LOG_TAG, "failed to set mSSID_user....scan results may have changed size (" + pos + ", " + mScanResults.size() + ")");
				}
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
	 * Called on Configure Device button click
	 * 
	 * This method launches DeviceSetupProgressActivity if EchoSetup is in range
	 */
	public void configureFlyport(View view) {
		// set network password from user input
		mPass_user = ((EditText) findViewById(R.id.dev_setup_password)).getText().toString();
		
		if (EchoBoxInRange()) {
			Bundle b = new Bundle();
			b.putString("SSID", mSSID_user);
			b.putString("encryption", mEncr_user);
			if (mEncr_user.equalsIgnoreCase("open")) {
				b.putString("password", "");
			} else {
				b.putString("password", mPass_user);
			}
			b.putBoolean("connect", true);
			Intent progressIntent = new Intent(this, DeviceSetupProgressActivity.class);
			progressIntent.putExtra("args", b);
			Log.v(LOG_TAG, "Launching DeviceSetupProgressActivity to perform EchoBox setup");
			startActivity(progressIntent);
		} else {
			Log.v(LOG_TAG, "could not detect EchoSetup...did not attempt to connect");
			mError_dlg.show(getFragmentManager(), "devicesetuperrordialog");
		}
	}
	
	/**
	 * check if EchoBox WiFi setup network is detected by app
	 */
	protected boolean EchoBoxInRange() {
		boolean inRange = false;
		for (ScanResult s : mWifiManager.getScanResults()) {
			if (s.SSID.equalsIgnoreCase(SETUP_NETWORK)) inRange = true;
		}
		return inRange;
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
	
/***************************************************************************************
 *                           Broadcast Receivers                                       *
 ***************************************************************************************/
	
	/**
	 * Broadcast Receiver for WiFi Scan Result Event
	 */
	class WifiScanResultsReceiver extends BroadcastReceiver {
		public void onReceive(Context c, Intent intent) {
			Log.v(WIFI_SCAN_TAG,"updating scan results");
			mScanResults.clear();
			mScanResults.addAll(mWifiManager.getScanResults());
			
			Iterator<ScanResult> iter = mScanResults.iterator();
			while (iter.hasNext()) {
				ScanResult sr = iter.next();
				if (sr.SSID.equalsIgnoreCase("") || sr.SSID.isEmpty()) {
					try {
						iter.remove();
					} catch (Exception e) {
						Log.e(WIFI_SCAN_TAG, "error removing network with no SSID from mScanResults");
					}
				}
			}
			
			mNetworksAdapter.notifyDataSetChanged();
			mRefreshImg.setImageAlpha(255);
		}
	}
}
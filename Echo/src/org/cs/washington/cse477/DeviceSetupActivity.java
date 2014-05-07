package org.cs.washington.cse477;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
import android.content.res.Resources.NotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class DeviceSetupActivity extends ActionBarActivity {

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
	protected IntentFilter waiterIF;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy); 
		
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_setup);
		
		// grab handle to networks spinner
		networksSpinner = (Spinner) findViewById(R.id.dev_setup_networks_spinner);
		
		// grab handle to system WifiManager
		wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);

		receiverWifi = new WifiScanResultsReceiver();
	    registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		
	    /*
	    
	    waiterIF.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
	    waiterIF.addAction();
	    */
	    waiterIF = new IntentFilter();
	    waiterIF.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
	    waiterIF.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
	    waiterIF.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
	    waiterIF.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
	    
	    wifiWaiter = new WifiConnectionEstablishedReceiver();
	    registerReceiver(wifiWaiter, waiterIF);
	    
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
        unregisterReceiver(wifiWaiter);
        super.onPause();
    }
	
	@Override
	protected void onResume() {
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        registerReceiver(wifiWaiter, waiterIF);
        super.onResume();
    }
	
	public boolean refreshScan(View v) {
		return wifiManager.startScan();
	}
	
	public void connectToFlyport(View view) {
		// save info to send to FlyPort
		Log.e("DEBUG", "Configure pressed");
		final String SSID_user = networksAdapter.getItem(networksSpinner.getSelectedItemPosition());
		final String pass_user = ((EditText) findViewById(R.id.dev_setup_password)).toString();
		
		
		wifiConfigurations = wifiManager.getConfiguredNetworks();
    	String SSID = "EchoSetup";
    	WifiConfiguration wificonf = new WifiConfiguration();
    	wificonf.SSID = "\"" + SSID + "\"";
    	wificonf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
    	
    	// remove saved configuration for any matching SSID
    	for (WifiConfiguration i : wifiConfigurations) {
    		if (i.SSID != null && i.SSID.equals("\"" + SSID + "\"")) {
    			wifiManager.removeNetwork(i.networkId);
    		}
    	}
    	
    	// add new configuration to WiFi manager
    	wifiManager.addNetwork(wificonf);

    	// connect to new configuration
    	for( WifiConfiguration i : wifiConfigurations ) {
    	    if(i.SSID != null && i.SSID.equals("\"" + SSID + "\"")) {
    	         wifiManager.disconnect();
    	         wifiManager.enableNetwork(i.networkId, true);
    	         wifiManager.reconnect();               

    	         break;
    	    }           
    	 }
    	Log.e("DEBUG", "connecting to EchoSetup");
    	// wait for connection
    	
    	
    	
    	
    	
    }
	
	public void postMessage(View v/*Intent intent*/) {
			Log.e("DEBUG", "postMessage() called");
			// send POST to FlyPort to configure
	    	// 192.168.1.13
	    	HttpClient client = new DefaultHttpClient();
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
				 NETTYPE=infra
				 &DHCPCL=enabled
				 &SSID=big+titays
				 &SECTYPE=OPEN
				 &WEP40KEY1=
				 &WEP40KEY2=
				 &WEP40KEY3=
				 &WEP40KEY4=
				 &WEP40KEYID=
				 &WEP104KEY=
				 &WPAPASS=
				 &WPA2PASS=
				 */
				
				ArrayList<NameValuePair> nameValuePair = new ArrayList<NameValuePair>();
				nameValuePair.add(new BasicNameValuePair("NETTYPE","infra"));
				nameValuePair.add(new BasicNameValuePair("DHCPCL","enabled"));
				
				// set SSID
				nameValuePair.add(new BasicNameValuePair("SSID","CSE-Local"));

				// set security
				nameValuePair.add(new BasicNameValuePair("SECTYPE","OPEN"));
				
				nameValuePair.add(new BasicNameValuePair("WEP40KEY1",""));
				nameValuePair.add(new BasicNameValuePair("WEP40KEY2",""));
				nameValuePair.add(new BasicNameValuePair("WEP40KEY3",""));
				nameValuePair.add(new BasicNameValuePair("WEP40KEY4",""));
				nameValuePair.add(new BasicNameValuePair("WEP40KEYID",""));
				nameValuePair.add(new BasicNameValuePair("WEP104KEY",""));
				nameValuePair.add(new BasicNameValuePair("WPAPASS",""));
				nameValuePair.add(new BasicNameValuePair("WPA2PASS",""));
				
				// add entity to post
				post.setEntity(new UrlEncodedFormEntity(nameValuePair));
				
				for (Header h : post.getAllHeaders()) {
					Log.e("DEBUG",h.toString());
				}
				
				
		        // Execute HTTP Post Request
		        HttpResponse response = client.execute(post);
		        Log.e("DEBUG","POST executed");

		    } catch (ClientProtocolException e) {
		        Log.e("DEBUG","ClientProtocolException");
		    } catch (IOException e) {
		    	Log.e("DEBUG","IOException");
		    	Log.e("DEBUG",e.getMessage());
		    }
		
		// go back to notifications
		//Intent intent_n = new Intent(getApplicationContext(), NotificationActivity.class);
    	//startActivity(intent_n);
	}
	
	StringBuilder sb = new StringBuilder();
	
	
	// Wifi Receiver to listen for wifi scan refresh requests
	class WifiScanResultsReceiver extends BroadcastReceiver {
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
	
	
	class WifiConnectionEstablishedReceiver extends BroadcastReceiver {
		public void onReceive(Context c, Intent intent) {
			Log.e("DEBUG","receiver called");
			
			ConnectivityManager conMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo[] netInf = conMgr.getAllNetworkInfo();
			for(NetworkInfo inf : netInf){
			    if(inf.getTypeName().contains("WIFI"))
			    {
			    	WifiInfo wifiinfo = wifiManager.getConnectionInfo();
			    	
			    	Log.e("DEBUG",wifiinfo.getSSID());
			    	Log.e("DEBUG", inf.isConnected() ? "true" : "false");
			        
			    	if(inf.isConnected() && wifiinfo.getSSID().equals("EchoSetup")){
			        	Log.e("DEBUG","wifi match!");
			        	//Toast.makeText(getApplicationContext(), "WiFi is connected.",Toast.LENGTH_LONG).show();

			        }   
			        else{
			            Log.e("DEBUG","wifi mismatch");
			        	//Toast.makeText(getApplicationContext(), "WiFi NOT connected.",Toast.LENGTH_LONG).show();

			        }
			    }
			}
			
			/*
			SupplicantState wi = wifiManager.getConnectionInfo().getSupplicantState();
			if (wi != null && wi == SupplicantState.COMPLETED) {
				Log.e("DEBUG","Connected");
				postMessage();
			} else {
				Log.e("DEBUG","Not Connected");
			}
			*/
			
			/*
			final String action = intent.getAction();
		    if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
		    	Log.e("DEBUG","action matched");
		    	if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)){
		    		Log.e("DEBUG","calling postMessage()");
		    		postMessage();
		        } else {
		        	Log.e("DEBUG","connection not established...");
		            // wifi connection was lost
		        }
		    }
		    */
			
		}
	}
}

package org.cs.washington.cse477;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.os.StrictMode;
import android.util.Log;

public class HttpPOSTGen {
	private static final String TAG = "HttpPOSTGen";
	
	public static boolean sendPOST(String SSID, String encryption, String pass) {
		// allow for network requests in this thread
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
		.permitAll().build();
		StrictMode.setThreadPolicy(policy);
		
		Log.v(TAG, "postMessage() called");
		// send POST to FlyPort to configure
		// 192.168.1.13
		HttpClient client = new DefaultHttpClient();		
		HttpPost post = null;
		try {
			post = new HttpPost(new URI("http://192.168.1.13/"));
		} catch (Exception e) {
			Log.e(TAG, "HttpPost creation failed with: " + e.getMessage());
		}

		try {
			// Add your data
			post.addHeader("Content-Type", "application/x-www-form-urlencoded");

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
			if (encryption.equals("WPA")) {
				nameValuePair.add(new BasicNameValuePair("SECTYPE", "WPA"));
				nameValuePair.add(new BasicNameValuePair("WEP40KEY1", ""));
				nameValuePair.add(new BasicNameValuePair("WEP40KEY2", ""));
				nameValuePair.add(new BasicNameValuePair("WEP40KEY3", ""));
				nameValuePair.add(new BasicNameValuePair("WEP40KEY4", ""));
				nameValuePair.add(new BasicNameValuePair("WEP40KEYID", ""));
				nameValuePair.add(new BasicNameValuePair("WEP104KEY", ""));
				nameValuePair.add(new BasicNameValuePair("WPAPASS", pass));
				nameValuePair.add(new BasicNameValuePair("WPA2PASS", ""));
			} else if (encryption.equals("WPA2")) {
				nameValuePair.add(new BasicNameValuePair("SECTYPE", "WPA2"));
				nameValuePair.add(new BasicNameValuePair("WEP40KEY1", ""));
				nameValuePair.add(new BasicNameValuePair("WEP40KEY2", ""));
				nameValuePair.add(new BasicNameValuePair("WEP40KEY3", ""));
				nameValuePair.add(new BasicNameValuePair("WEP40KEY4", ""));
				nameValuePair.add(new BasicNameValuePair("WEP40KEYID", ""));
				nameValuePair.add(new BasicNameValuePair("WEP104KEY", ""));
				nameValuePair.add(new BasicNameValuePair("WPAPASS", ""));
				nameValuePair.add(new BasicNameValuePair("WPA2PASS", pass));
			} else {
				nameValuePair.add(new BasicNameValuePair("SECTYPE", "OPEN"));
				nameValuePair.add(new BasicNameValuePair("WEP40KEY1", ""));
				nameValuePair.add(new BasicNameValuePair("WEP40KEY2", ""));
				nameValuePair.add(new BasicNameValuePair("WEP40KEY3", ""));
				nameValuePair.add(new BasicNameValuePair("WEP40KEY4", ""));
				nameValuePair.add(new BasicNameValuePair("WEP40KEYID", ""));
				nameValuePair.add(new BasicNameValuePair("WEP104KEY", ""));
				nameValuePair.add(new BasicNameValuePair("WPAPASS", ""));
				nameValuePair.add(new BasicNameValuePair("WPA2PASS", ""));
			}

			// add entity to post
			post.setEntity(new UrlEncodedFormEntity(nameValuePair));

//			for (Header h : post.getAllHeaders()) {
//				Log.v("DEBUG", h.toString());
//			}

			// Execute HTTP Post Request
//			Log.v("DEBUG", "Printing Response Headers");
			HttpResponse response = client.execute(post);
//			for (Header h : response.getAllHeaders()) {
//				Log.v("DEBUG", h.toString());
//			}
			Log.v(TAG, "POST executed");
			return true;
		} catch (Exception e) {
			Log.e(TAG, "Exception");
			Log.e(TAG, e.getMessage());
		}

		return false;
		
		// go back to notifications
		// Intent intent_n = new Intent(getApplicationContext(),
		// NotificationActivity.class);
		// startActivity(intent_n);
		
	}
	
	// DEBUG to fetch the debug message
	public static String getPOSTMessage(HttpEntity ent) {
		StringBuilder sb = new StringBuilder();
		BufferedReader buf;
		try {
			buf = new BufferedReader(new InputStreamReader(ent.getContent()));
		} catch (Exception e) {
			return "";
		}
		String inputLine;
		try {
		       while ((inputLine = buf.readLine()) != null) {
		              sb.append(inputLine);
		       }
		       buf.close();
		  } catch (IOException e) {
		       e.printStackTrace();
		  }
		
		return (ent == null) ? null : sb.toString();
	}
}

package org.cs.washington.cse477;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import android.net.http.AndroidHttpClient;
import android.util.Log;

public class HttpPOSTGen {
	
	public static boolean sendPOST(String SSID, String encryption, String pass) {
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
}

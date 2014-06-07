package org.cs.washington.cse477;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.os.AsyncTask;
import android.util.Log;

public class HttpPOSTAsyncTask extends AsyncTask<String, Void, Boolean> {
	private static final String LOG_TAG = "HttpPOSTGen";
    private static final int MAX_ATTEMPTS = 3;
	private String SSID = null;
	private String password = null;
	private String encryption = null;
	
	@Override
	protected Boolean doInBackground(String... params) {
		if (params.length != 3) {
			return Boolean.valueOf(false);
		}
		SSID = params[0];
		password = params[1];
		encryption = params[2];
		
		return Boolean.valueOf(sendPOST());
	}
	
    private static String generateStrongPasswordHash(String password, String ssid)
    		throws NoSuchAlgorithmException, InvalidKeySpecException {
        int iterations = 4096;
        char[] chars = password.toCharArray();
        byte[] salt = ssid.getBytes();
        
        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 256);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return toHex(hash);
    }

    private static String toHex(byte[] array) throws NoSuchAlgorithmException {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if(paddingLength > 0)
        {
            return String.format("%0"  +paddingLength + "d", 0) + hex;
        }else{
            return hex;
        }
    }
	
	private boolean sendPOST() {
		int attempts = 0;
		boolean res = false;
		while (!res && attempts < MAX_ATTEMPTS) {
			attempts++;
			// send POST to FlyPort to configure
			HttpClient client = new DefaultHttpClient();		
			HttpPost post = null;
			try {
				post = new HttpPost(new URI("http://192.168.1.13/"));
			} catch (Exception e) {
				Log.e(LOG_TAG, "HttpPost creation failed with: " + e.getMessage());
				break;
			}
			
			try {
				// Add headers
				post.addHeader("Content-Type", "application/x-www-form-urlencoded");

				
				// create name-value pairs for POST
				/*
				 * NETTYPE=infra &DHCPCL=enabled &SSID=big+titays &SECTYPE=OPEN
				 * &WEP40KEY1= &WEP40KEY2= &WEP40KEY3= &WEP40KEY4= &WEP40KEYID=
				 * &WEP104KEY= &WPAPASS= &WPA2PASS=
				 */

				ArrayList<NameValuePair> nameValuePair = new ArrayList<NameValuePair>();
				nameValuePair.add(new BasicNameValuePair("NETTYPE", "infra")); // infrastructure mode
				nameValuePair.add(new BasicNameValuePair("DHCPCL", "enabled")); // DHCP enabled

				// set SSID
				nameValuePair.add(new BasicNameValuePair("SSID", SSID));

				// set security and key
				if (encryption.equals("WPA")) {
					nameValuePair.add(new BasicNameValuePair("SECTYPE", "WPA"));
					nameValuePair.add(new BasicNameValuePair("WEP40KEY1", ""));
					nameValuePair.add(new BasicNameValuePair("WEP40KEY2", ""));
					nameValuePair.add(new BasicNameValuePair("WEP40KEY3", ""));
					nameValuePair.add(new BasicNameValuePair("WEP40KEY4", ""));
					nameValuePair.add(new BasicNameValuePair("WEP40KEYID", ""));
					nameValuePair.add(new BasicNameValuePair("WEP104KEY", ""));
					nameValuePair.add(new BasicNameValuePair("WPAKEY", generateStrongPasswordHash(password, SSID)));
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
					nameValuePair.add(new BasicNameValuePair("WPA2KEY", generateStrongPasswordHash(password, SSID)));
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

				// add entity of name-value pairs to post
				post.setEntity(new UrlEncodedFormEntity(nameValuePair));

				// Execute HTTP Post Request
				HttpResponse resp = client.execute(post);

				// TODO: correct response code?
				if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					res = true;
				} else {
					Log.v(LOG_TAG, "HttpResponse status code != 200");
					res = false;
				}
			} catch (Exception e) {
				res = false;
				Log.e(LOG_TAG, "POST Execution Exception");
				Log.e(LOG_TAG, e.getMessage());
				break;
			}
		}
		
		return res;
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

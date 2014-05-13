package org.cs.washington.cse477;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

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
     
    private static String generateWPAKey(String password, String ssid) {
    	return null;
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
				String p = generateStrongPasswordHash(pass, SSID);
				Log.v(TAG, "passkey: " + p);
				nameValuePair.add(new BasicNameValuePair("WPAKEY", p));
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
				String p = generateStrongPasswordHash(pass, SSID);
				Log.v(TAG, "passkey: " + p);
				nameValuePair.add(new BasicNameValuePair("WPA2KEY", p));
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

			// Execute HTTP Post Request
			HttpResponse response = client.execute(post);
			Log.v(TAG, "POST executed");
			try {
				int sc = response.getStatusLine().getStatusCode();
				Log.v(TAG,"HttpResponse StatusCode: " + sc);
			} catch (Exception e) {
				
			}
			
			return true;
		} catch (Exception e) {
			Log.e(TAG, "POST Execution Exception");
			Log.e(TAG, e.getMessage());
		}

		return false;		
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

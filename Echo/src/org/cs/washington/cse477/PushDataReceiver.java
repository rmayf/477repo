package org.cs.washington.cse477;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
	 * 
	 * An inner class that extends BroadcastReceiver MUST be static otherwise
	 * the app crashes.
	 * 
	 * @author imathieu
	 * 
	 */
	public class PushDataReceiver extends BroadcastReceiver {
		private JSONObject pushData;
		public final static String LOG_TAG = "PushDataReceiver";

		public PushDataReceiver() {
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			// call refresh 
			
		/*	
			String stringPushData = intent.getExtras().getString("com.parse.Data");
			Log.v(LOG_TAG, "Intent received: " + stringPushData);
			try {
				pushData = new JSONObject(stringPushData);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
		}
	}
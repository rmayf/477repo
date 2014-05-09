package org.cs.washington.cse477;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class NotificationActivity extends ActionBarActivity {
	private static final String LOG_TAG = "NotificationActivity";
	private static final int MAX_REFRESH = 20;
//	protected List<String> notifications;
	protected ArrayAdapter<ParseObject> notifyAdapter;
	protected ListView notifyView;
	protected List<ParseObject> parseNotifications;
	//private AudioSampleFetcher asf;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		parseNotifications = getNotifications(MAX_REFRESH);

		setContentView(R.layout.activity_notification);

		setupNotificationListView();		
		//asf = new AudioSampleFetcher(getApplicationContext());
	}

	protected void setupNotificationListView() {
		notifyView = (ListView) findViewById(R.id.notification_listview);
		notifyAdapter = new NotificationListAdapter(this, parseNotifications);
		notifyView.setAdapter(notifyAdapter);
	}
	
	// gets the Event objects that are match targets. Subscriptions to these sounds
	// may be on or off
	public List<ParseObject> getNotifications(int numToFetch) {
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Event");
		query.setLimit(numToFetch);
		List<ParseObject> mNotifications = null;
		try {
			mNotifications = query.find();
			Log.v(LOG_TAG, "success getting: " + mNotifications.size() + " event objects");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mNotifications;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.notification, menu);
		return true;
	}

	public void receivePush(ParseObject n) {
		parseNotifications.add(0, n);
		notifyAdapter.notifyDataSetChanged();
	}
	
	/**
	 * refresh()
	 * 
	 * pull MAX_REFRESH newest notifications from parse and display
	 * clear() must be called AFTER getNotifications()
	 */
	public void refresh() {
		parseNotifications = getNotifications(MAX_REFRESH);
		parseNotifications.clear();
		notifyAdapter.notifyDataSetChanged();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		Intent intent;
		switch(item.getItemId()) {
		case R.id.notification_action_setup_device:
			intent = new Intent(this, DeviceSetupActivity.class);
	    	startActivity(intent);
			return true;
		case R.id.notification_action_settings:
			intent = new Intent(this, SettingsActivity.class);
	    	startActivity(intent);
			return true;
		case R.id.notification_action_refresh:
			refresh();
			return true;
			
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	/**
	 * 
	 * An inner class that extends BroadcastReceiver MUST be static otherwise
	 * the app crashes.
	 * 
	 * @author imathieu
	 * 
	 */
	protected static JSONObject pushData;
	public static class PushDataReceiver extends BroadcastReceiver {

		public final static String LOG_TAG = "PushDataReceiver";

		public PushDataReceiver() {	}

		@Override
		public void onReceive(Context context, Intent intent) {
			String stringPushData = intent.getExtras().getString("com.parse.Data");
			Log.v(LOG_TAG, "Intent received: " + stringPushData);
			try {
				pushData = new JSONObject(stringPushData);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

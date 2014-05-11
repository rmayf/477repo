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
	private static final int MAX_REFRESH = 20;
	protected ArrayAdapter<ParseObject> notifyAdapter;
	protected ListView notifyView;
	protected List<ParseObject> parseNotifications;
	
	private static final String TAG = "NotificationActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v(TAG,"after super.onCreate()");
		parseNotifications = getNotifications(MAX_REFRESH);

		setContentView(R.layout.activity_notification);

		setupNotificationListView();		
	}
	
	/**
	 * When the activity resumes, such as when it is opened via a push notification on
	 * the system tray, get the the latest data
	 */
	@Override
	protected void onResume() {
		super.onResume();
		refresh();
	}

	protected void setupNotificationListView() {
		notifyView = (ListView) findViewById(R.id.notification_listview);
		notifyAdapter = new NotificationListAdapter(this, parseNotifications);
		notifyView.setAdapter(notifyAdapter);
	}
	
	// gets the Event objects that are match targets. Subscriptions to these sounds
	// may be on or off
	public List<ParseObject> getNotifications(int numToFetch) {
		Log.v(TAG,"entering getNotifications");
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Event");
		query.orderByDescending("createdAt");
		query.setLimit(numToFetch);
		List<ParseObject> mNotifications = null;
		try {
			mNotifications = query.find();
			Log.v(TAG, "success getting: " + mNotifications.size() + " event objects");
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
		parseNotifications.clear();
		parseNotifications.addAll(getNotifications(MAX_REFRESH));
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
	
	
}

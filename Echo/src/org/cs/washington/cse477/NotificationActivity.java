package org.cs.washington.cse477;

import java.util.ArrayList;
import java.util.List;

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
	// logging tag
	private static final String TAG = "NotificationActivity";
	
	// ListView & Adapter & Data
	protected static ArrayAdapter<ParseObject> notifyAdapter;
	protected ListView notifyView;
	protected static List<ParseObject> parseNotifications;
	private static final int MAX_REFRESH = 20;
	
	/**
	 * onCreate(): initialize and fetch notifications
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_notification);
		
		init();
		
		refreshNotifications();
		
	}
	
	protected void init() {
		parseNotifications = new ArrayList<ParseObject>(MAX_REFRESH);
		notifyView = (ListView) findViewById(R.id.notification_listview);
		notifyAdapter = new NotificationListAdapter(this, parseNotifications);
		try {
			notifyView.setAdapter(notifyAdapter);
		} catch (Exception e) {
			Log.e(TAG,"failed to set notifications listview adapter with:\n" + e.getMessage());
		}
	}

	// gets the Event objects that are match targets. Subscriptions to these sounds
	// may be on or off
	public static void refreshNotifications() {
		Log.v(TAG,"refreshNotifications()");
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Event");
		query.orderByDescending("createdAt");
		query.setLimit(MAX_REFRESH);
		List<ParseObject> mNotifications = null;
		try {
			mNotifications = query.find();
			Log.v(TAG, "success getting: " + mNotifications.size() + " event objects");
		} catch (ParseException e) {
			Log.e(TAG,"failed to fetch notifications from Parse\n" + e.getMessage());
		}
		if (mNotifications != null) {
			parseNotifications.clear();
			for (ParseObject p : mNotifications) {
				parseNotifications.add(p);
			}
			//parseNotifications = mNotifications;
			notifyAdapter.notifyDataSetChanged();
		}
	}
	
	/**
	 * When the activity resumes, such as when it is opened via a push notification on
	 * the system tray, get the the latest data
	 */
	@Override
	protected void onResume() {
		super.onResume();
		refreshNotifications();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.notification, menu);
		return true;
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
			refreshNotifications();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}

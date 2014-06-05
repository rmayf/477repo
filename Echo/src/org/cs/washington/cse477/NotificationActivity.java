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

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class NotificationActivity extends ActionBarActivity {
	// logging tag
	private static final String LOG_TAG = "NotificationActivity";
	
	private static final int MAX_REFRESH = 20;
	
	// ListView & Adapter & Data
	protected ListView mNotifyView;
	protected static ArrayAdapter<ParseObject> mNotifyAdapter;
	protected static List<ParseObject> mParseNotifications;
	
	/**
	 * onCreate(): initialize and fetch notifications
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_notification);
		
		init();
		
		//refreshNotifications();
		
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
	
	/**
	 * initialize activity member data and set view adapter 
	 */
	protected void init() {
		mParseNotifications = new ArrayList<ParseObject>(MAX_REFRESH);
		mNotifyView = (ListView) findViewById(R.id.notification_listview);
		mNotifyAdapter = new NotificationListAdapter(this, mParseNotifications);
		try {
			mNotifyView.setAdapter(mNotifyAdapter);
		} catch (Exception e) {
			Log.e(LOG_TAG,"failed to set notifications listview adapter with:\n" + e.getMessage());
		}
	}

	/**
	 * fetch notifications list from parse asynchronously
	 */
	public static void refreshNotifications() {
		Log.v(LOG_TAG,"refreshNotifications()");
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Event");
		query.orderByDescending("createdAt");
		query.setLimit(MAX_REFRESH);
		query.findInBackground(new FindCallback<ParseObject>() {
			
			@Override
			public void done(List<ParseObject> objs, ParseException e) {
				if (e == null) {
					mParseNotifications.clear();
					mParseNotifications.addAll(objs);
					mNotifyAdapter.notifyDataSetChanged();
					Log.v(LOG_TAG, "success getting: " + mParseNotifications.size() + " event objects");
				} else {
					// ParseException, do nothing, log error
					Log.e(LOG_TAG,"failed to fetch notifications from Parse\n" + e.getMessage());
				}
			}
		});
	}

	/**
	 * add menu to view
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.notification, menu);
		return true;
	}

	/**
	 * handle menu clicks
	 */
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

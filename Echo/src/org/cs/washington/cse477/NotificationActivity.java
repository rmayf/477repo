package org.cs.washington.cse477;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cs.washington.cse477.ConfirmDeleteDialog.ConfirmDeleteListener;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class NotificationActivity extends ActionBarActivity 
								implements ConfirmDeleteListener {
	// logging tag
	private static final String LOG_TAG = "NotificationActivity";
	
	private static final int MAX_REFRESH = 20;
	
	// ListView & Adapter & Data
	protected ListView mNotifyView;
	protected static ArrayAdapter<ParseObject> mNotifyAdapter;
	protected static List<ParseObject> mParseNotifications;
	protected final FragmentManager mFragmentManager = getFragmentManager();
	
	/**
	 * initialize and fetch notifications
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_notification);
		
		/**
		 * Init
		 */
		mParseNotifications = new ArrayList<ParseObject>(MAX_REFRESH);
		mNotifyView = (ListView) findViewById(R.id.notification_listview);
		mNotifyAdapter = new NotificationListAdapter(this, mParseNotifications, mFragmentManager);
		mNotifyView.setAdapter(mNotifyAdapter);
		
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
				if (e == null && mParseNotifications != null && mNotifyAdapter != null) {
					// TODO: crashes if mParseNotifications is null, i.e. this activity has never been opened but a notification arrives
					mParseNotifications.clear();
					mParseNotifications.addAll(objs);
					mNotifyAdapter.notifyDataSetChanged();
					Log.v(LOG_TAG, "success getting: " + mParseNotifications.size() + " event objects");
				} else if (e == null) {
					Log.w(LOG_TAG,"Notification received, but did not attempt to update Notifications");
				} else {
					// ParseException, do nothing, log error
					Log.e(LOG_TAG,"failed to fetch notifications from Parse\n" + e.getMessage());
				}
			}
		});
	}
	
    /**
     * click handler for delete dialog -> confirm delete
     */
    @Override
	public void onDeleteDialogPositiveClick(DialogFragment dialog) {
		Log.v(LOG_TAG,"positive click delete dialog");
		Bundle args = dialog.getArguments();
		if (args != null) {
			String toDelete = args.getString("filename");
			String objId = args.getString("objectId");
			deleteEventFromParse(objId); 
			deleteEventFromNode(toDelete);
		}
    }
    
    private void deleteEventFromParse(String objId) {
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Event");
		query.whereEqualTo("objectId", objId);
		query.getFirstInBackground(new GetCallback<ParseObject>() {
			
			@Override
			public void done(ParseObject obj, ParseException e) {
				if (e == null) {
					Log.v(LOG_TAG, "attempting to delete object");
					obj.deleteInBackground(new DeleteCallback() {
						
						@Override
						public void done(ParseException delE) {
							if (delE == null) {
								Log.v(LOG_TAG, "successfully deleted object");
								refreshNotifications();
							} else {
								Log.e(LOG_TAG, "error deleting object");
							}
						}
					});
				} else {
					Log.v(LOG_TAG, "failed to find matching object to delete");
				}
			}
		});   	
    }
    
	private void deleteEventFromNode(String toDelete) {
		String urlAndPath = "http://" + AppInit.host + ":" + AppInit.port + "/deleteEventRecording?filename=";
		urlAndPath += toDelete;
		//urlAndPath += ".wav";
		AsyncTask<String,Void,Integer> httpReq = new AsyncHttpRequestToMatchServer();
		int statusCode = -1;
		try {
			statusCode = httpReq.execute(urlAndPath).get(10, TimeUnit.SECONDS).intValue();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (statusCode != -1) {
			Log.v(LOG_TAG, "status code from http response: " + statusCode);
		} 
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
		switch(item.getItemId()) {
		case R.id.notification_action_refresh:
			refreshNotifications();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}

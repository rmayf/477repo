package org.cs.washington.cse477;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationPushReceiver extends BroadcastReceiver {

	private static final String TAG = "NotificationPushReceiver";	
	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			Log.v(TAG, "push received, calling NotificationActivitiy.refreshNotifications()");
			NotificationActivity.refreshNotifications();
		} catch (Exception e) {
			Log.e(TAG, "error attempting to refresh notifications " + e.getMessage());
		}
	}

}

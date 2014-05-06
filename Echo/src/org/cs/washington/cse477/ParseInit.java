package org.cs.washington.cse477;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.PushService;

public class ParseInit extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		// Add your initialization code here
		Parse.initialize(this, "t1oV8LeSRZsCFmBSe0yudiZv17eHIJdaHtytj0ZP", "CgnLN9dMbywD4CXELdVcAOxV8FFC6La6c1sYRV0S");
		PushService.setDefaultPushCallback(this, NotificationActivity.class);
		ParseInstallation.getCurrentInstallation().saveInBackground();
		PushService.subscribe(this.getApplicationContext(), "cat", NotificationActivity.class);
	}

}

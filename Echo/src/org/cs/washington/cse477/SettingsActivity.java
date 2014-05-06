package org.cs.washington.cse477;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.PushService;

public class SettingsActivity extends ActionBarActivity {
	public static final String LOG_TAG = "SettingsActivity";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_reg);
		findViewById(R.id.makeSoundInstructions).setVisibility(View.GONE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.device_reg, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void saveSoundNameToParse(View view) {
		EditText soundNameText = (EditText) findViewById(R.id.soundNameText);
		String soundName = soundNameText.getText().toString();
		ParseObject sound = new ParseObject("Sound");
		sound.put("name", soundName);
		try {
			sound.save();
		} catch (ParseException e) {
			Log.e(LOG_TAG, "Parse Server not available");
			// server is unavailable
		}
		sound.fetchInBackground(new GetCallback<ParseObject>() {
			public void done(ParseObject object, ParseException e) {
				if (e == null) {
					// success, tell user to make sound then subscribe
					findViewById(R.id.makeSoundInstructions).setVisibility(View.VISIBLE);
					PushService.subscribe(getApplicationContext(), object.getObjectId(), NotificationActivity.class);
					Log.v(LOG_TAG, "Subscribing to channel: " + object.getObjectId());
				} else {
					// failure, tell user to try again
				}
			}
		});
	}
	
	
	// refresh notifications
}
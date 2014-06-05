package org.cs.washington.cse477;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.cs.washington.cse477.AddAudioSampleDialog.AddAudioSampleDialogListener;
import org.cs.washington.cse477.ConfirmDeleteDialog.ConfirmDeleteListener;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.PushService;
import com.parse.SaveCallback;

public class SettingsActivity extends ActionBarActivity implements
		AddAudioSampleDialogListener, ConfirmDeleteListener {
	
	private static final String LOG_TAG = "SettingsActivity";
	private static final String DLG_TAG = "AddAudioSampleDialog";
	private static final int MAX_REFRESH = -1;
	private static final int DEFAULT_CAPACITY = 10;
	
	protected ListView mSettingsView;
	protected ArrayAdapter<ParseObject> mSettingsListAdapter;
	protected List<ParseObject> mParseSounds;
	protected final FragmentManager mFragmentManager = getFragmentManager();
	protected static final AddAudioSampleDialog mAdd_dlg = new AddAudioSampleDialog();
	
	/**
	 * on activity creation, set the view and call init()
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_settings);
		
		/**
		 * Init
		 */
		mParseSounds = new ArrayList<ParseObject>(DEFAULT_CAPACITY);
		mSettingsView = (ListView) findViewById(R.id.settings_listview);
		mSettingsListAdapter = new SettingsListAdapter(this, mParseSounds, mFragmentManager);
		try {
			mSettingsView.setAdapter(mSettingsListAdapter);
		} catch (Exception e) {
			Log.e(LOG_TAG,"failed to set notifications listview adapter with:\n" + e.getMessage());
		}
		
		Switch toggle = (Switch) findViewById(R.id.loud_unknown_toggle);
		toggle.setChecked(getLoudUnknownSubscriptionStatus());
		toggle.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				boolean checked = Boolean.valueOf(((Switch) v).isChecked());
				Log.v(LOG_TAG,"checked: " + checked);
				if (checked) {
					PushService.subscribe(getApplicationContext(), ParseInit.loudUnknownChannel, NotificationActivity.class);
				} else {
					PushService.unsubscribe(getApplicationContext(), ParseInit.loudUnknownChannel);
				}
			}
		});
	}
	
	/**
	 * on activity resume, refresh sounds to display settings for
	 */
	@Override
	protected void onResume() {
		super.onResume();
		refreshSounds();
	}

	/**
	 * perform asynchronous refresh of sound objects to display settings for
	 */
	protected void refreshSounds() {
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Sound");
		query.setLimit(MAX_REFRESH);
		query.findInBackground(new FindCallback<ParseObject>() {
			
			@Override
			public void done(List<ParseObject> objs, ParseException e) {
				if (e == null) {
					mParseSounds.clear();
					mParseSounds.addAll(objs);
					mSettingsListAdapter.notifyDataSetChanged();
					Log.v(LOG_TAG, "success getting Sound objects");
				} else {
					// ParseException, do nothing, log error
					Log.e(LOG_TAG, "failed tp fetch Sound objects");
				}
			}
		});
	}

	/**
	 * fetch status of loud unknown notification setting
	 */
	public boolean getLoudUnknownSubscriptionStatus() {
		Set<String> setOfAllSubscriptions = PushService.getSubscriptions(getApplicationContext());
		Log.v(LOG_TAG, "current channels subscribed to: " + setOfAllSubscriptions.toString());
		return setOfAllSubscriptions.contains("loudUnknown");
	}

	/**
	 * add menu to view
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings, menu);
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
		case R.id.notification_action_add_sample:
			mAdd_dlg.show(getFragmentManager(), "addaudiosampledialog");
			return true;
		case R.id.settings_action_refresh:
			refreshSounds();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	
	/**
	 * click handler for add audio sample dialog -> ready
	 * 
	 * saves new sound object to Parse asynchronously, where this sound is associated
	 * with the next loud sample recorded by Echo device
	 */
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // User touched the dialog's positive button
    	Log.v(LOG_TAG, dialog.toString());
		// fetch user input
		EditText user_input_et = (EditText) dialog.getDialog().findViewById(R.id.add_audio_edit_text);
		if (user_input_et == null) {
			Log.e(DLG_TAG,"null EditText");
		} else {
			String user_input = user_input_et.getText().toString();
			ParseObject sound = new ParseObject("Sound");
			sound.put("name", user_input);
			sound.put("enabled", true);
			sound.put("useNextAsTarget", true);
			sound.saveInBackground(new SaveCallback() {
				
				@Override
				public void done(ParseException e) {
					if (e != null) {
						Log.e(LOG_TAG, "Parse error: " + e.getCode() + " " + e.getMessage());
					} else {
						refreshSounds();
					}
				}
			});
		}
    }
    
    /**
     * click handler for delete dialog -> confirm delete
     */
    @Override
	public void onDeleteDialogPositiveClick(DialogFragment dialog) {
		Log.v(LOG_TAG,"positive click delete dialog");
		Bundle args = dialog.getArguments();
		if (args != null) {
			deleteSound(args.getString("name")); 
		}
	}
	
	/**
	 * delete sound from Parse asynchronously
	 * 
	 * note: this method does not check if more than one object with the name exists,
	 *       and simply deletes the first match returned
	 *       
	 *       THIS COULD BE DANGEROUS!!!
	 */
	private boolean deleteSound(String toDelete) {		
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Sound");
		query.whereEqualTo("name", toDelete);
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
								refreshSounds();
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
		
		return true;
	}

}
package org.cs.washington.cse477;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.cs.washington.cse477.AddAudioSampleDialog.AdduAudioSampleDialogListener;
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
import android.widget.TextView;

import com.parse.CountCallback;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.PushService;
import com.parse.SaveCallback;

public class SettingsActivity extends ActionBarActivity implements
		AdduAudioSampleDialogListener, ConfirmDeleteListener {
	
	private static final String LOG_TAG = "SettingsActivity";
	private static final int MAX_REFRESH = -1;
	private static final int DEFAULT_CAPACITY = 10;
	
	protected ListView mSettingsView;
	protected ArrayAdapter<ParseObject> mSettingsListAdapter;
	protected List<ParseObject> mParseSounds;
	protected final FragmentManager mFragmentManager = getFragmentManager();
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_settings);
		
		init();
		
		//refreshSounds();
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		refreshSounds();
	}
	
	protected void init() {
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
				Set<String> setOfAllSubscriptions = PushService.getSubscriptions(getApplicationContext());
				Log.v(LOG_TAG, "current channels subscribed to: " + setOfAllSubscriptions.toString());
			}
		});
	}

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

	
	
	public boolean getLoudUnknownSubscriptionStatus() {
		//PushService.subscribe(this.getApplicationContext(), defaultChannel, NotificationActivity.class);
		Set<String> setOfAllSubscriptions = PushService.getSubscriptions(getApplicationContext());
		Log.v(LOG_TAG, "current channels subscribed to: " + setOfAllSubscriptions.toString());
		return setOfAllSubscriptions.contains("loudUnknown");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.notification_action_add_sample) {
			// pop-up dialog with instructions
			addNewSample();
			// when push received for new audio sample, goto audio upload activity
			//intent = new Intent(this, AudioUploadActivity.class);
			// put data to intent that we can pull out from within AudioUploadActivity
			//startActivity(intent);
			return true;
		} else if (itemId == R.id.notification_action_push_settings) {
			// push settings to cloud if any have changed
			// this can be done by comparing the new states to the old states
			return true;
		} else if (itemId == R.id.settings_action_refresh) {
			refreshSounds();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}


	protected static final AddAudioSampleDialog add_dlg = new AddAudioSampleDialog();
	private static final String DLG_TAG = "AddAudioSampleDialog";
	
	public void addNewSample() {
		//add_dlg = new AddAudioSampleDialog();
		add_dlg.show(getFragmentManager(), "addaudiosampledialog");
	}
	
	
	
	// The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the AddAudioSampleDialog.AddAudioSampleDialogListener interface
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
			saveSoundNameToParse(user_input);
		}
    }
    
    // Delete dialog positive click handler
    @Override
	public void onDeleteDialogPositiveClick(DialogFragment dialog) {
		Log.v(LOG_TAG,"positive click delete dialog");
		Bundle args = dialog.getArguments();
		if (args != null) {
			doDelete(args.getString("name")); 
		}
	}
    
	public void saveSoundNameToParse(String name) {
		ParseObject sound = new ParseObject("Sound");
		sound.put("name", name);
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
	
	private boolean doDelete(String toDelete) {		
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
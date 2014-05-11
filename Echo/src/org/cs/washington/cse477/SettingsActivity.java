package org.cs.washington.cse477;


import java.util.List;

import org.cs.washington.cse477.AddAudioSampleDialog.AdduAudioSampleDialogListener;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.PushService;

public class SettingsActivity extends ActionBarActivity implements
		AdduAudioSampleDialogListener, ConfirmDeleteDialog.ConfirmDeleteListener{
	
	public static final String LOG_TAG = "SettingsActivity";
	private static final int MAX_REFRESH = -1;
	protected ArrayAdapter<ParseObject> settingsListAdapter;
	protected List<ParseObject> parseSounds;
	
	protected ListView settingsView;
		
	// DEBUG
	protected TextView text;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		
		parseSounds = getSounds(MAX_REFRESH);
		setupSettingsListView();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		refresh();
	}
	
	public List<ParseObject> getSounds(int numToFetch) {
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Sound");
		query.setLimit(numToFetch);
		List<ParseObject> sounds = null;
		try {
			sounds = query.find();
			Log.v(LOG_TAG, "success getting Sound objects");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sounds;
	}
	/**
	 * refresh()
	 * 
	 * pull MAX_REFRESH newest notifications from parse and display
	 */
	public void refresh() {
		parseSounds.clear();
		parseSounds.addAll(getSounds(MAX_REFRESH));
		settingsListAdapter.notifyDataSetChanged();
	}
	
	// pull list of all targets being tracked by registered devices
	// display in the list, with toggle and play button and delete button
	protected void setupSettingsListView() {
		//text = (TextView) findViewById(R.id.settings_text_test);

		settingsView = (ListView) findViewById(R.id.settings_listview);
		
		settingsListAdapter = new SettingsListAdapter(this, parseSounds, getFragmentManager());
		settingsView.setAdapter(settingsListAdapter);
		
		
		
		// Add click handler here
		/*
		settingsView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				text.setText(settingsList.get(pos));
			}
			
		});
		*/
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch(item.getItemId()) {		
		case R.id.notification_action_add_sample:
			// pop-up dialog with instructions
			addNewSample();
			
			// when push received for new audio sample, goto audio upload activity
			//intent = new Intent(this, AudioUploadActivity.class);
			// put data to intent that we can pull out from within AudioUploadActivity
			//startActivity(intent);
			return true;
		case R.id.notification_action_push_settings:
			// push settings to cloud if any have changed
			// this can be done by comparing the new states to the old states
			return true;
		case R.id.settings_action_refresh:
			refresh();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	protected AddAudioSampleDialog add_dlg = null;
	private static final String DLG_TAG = "AddAudioSampleDialog";
	
	public void addNewSample() {
		add_dlg = new AddAudioSampleDialog();
		add_dlg.show(getFragmentManager(), "addaudiosampledialog");
	}
	
	// The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the AddAudioSampleDialog.AddAudioSampleDialogListener interface
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // User touched the dialog's positive button
    	Log.v(LOG_TAG, dialog.toString());
    	if (dialog == null) {
    		Log.e(DLG_TAG,"null dialog");
    	} else {
    		Log.v(LOG_TAG, dialog.toString());
    		// fetch user input
    		EditText user_input_et = (EditText) dialog.getDialog().findViewById(R.id.add_audio_edit_text);
    		if (user_input_et == null) {
    			Log.e(DLG_TAG,"null EditText");
    		} else {
	    		String user_input = user_input_et.getText().toString();
	    		saveSoundNameToParse(user_input);
	    		/*
	    		// DEBUG -- set a textview to show user's input
	    		text = (TextView) findViewById(R.id.settings_text_test);
	    		text.setText(user_input);
	    		*/
	    		
    		}
    	}
    }
    
	public void saveSoundNameToParse(String name) {
		ParseObject sound = new ParseObject("Sound");
		sound.put("name", name);
		sound.put("enabled", true);
		sound.put("useNextAsTarget", true);
		try {
			sound.save();
		} catch (ParseException e) {
			Log.e(LOG_TAG, "Parse error: " + e.getCode() + " " + e.getMessage());
			// server is unavailable
		}
		sound.fetchInBackground(new GetCallback<ParseObject>() {
			public void done(ParseObject object, ParseException e) {
				if (e == null) {
					// success, tell user to make sound
					refresh();
				} else {
					// failure, tell user to try again
				}
			}
		});
	}

}
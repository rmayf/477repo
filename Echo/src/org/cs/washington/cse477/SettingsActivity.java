package org.cs.washington.cse477;


import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;

import java.util.LinkedList;
import java.util.List;

import org.cs.washington.cse477.AddAudioSampleDialog.AdduAudioSampleDialogListener;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class SettingsActivity extends ActionBarActivity implements
		AdduAudioSampleDialogListener {
	
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
	// display in the list, with toggle and play button
	protected void setupSettingsListView() {
		//text = (TextView) findViewById(R.id.settings_text_test);

		settingsView = (ListView) findViewById(R.id.settings_listview);
		
		settingsListAdapter = new SettingsListAdapter(this, parseSounds);
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
    	if (dialog == null) {
    		Log.e(DLG_TAG,"null dialog");
    	} else {
    		// fetch user input
    		EditText user_input_et = (EditText) dialog.getDialog().findViewById(R.id.add_audio_edit_text);
    		if (user_input_et == null) {
    			Log.e(DLG_TAG,"null EditText");
    		} else {
	    		String user_input = user_input_et.getText().toString();
	
	    		// DEBUG -- set a textview to show user's input
	    		text = (TextView) findViewById(R.id.settings_text_test);
	    		text.setText(user_input);
	    		
	    		// TODO: add new sample code here
    		}
    	}
    	
    }
	
//	/**
//	 *  
//	 */
//	public static void fetchThenPlayTarget(View view)  {
//		String filename = "isaiahtest.ogg";
//		new FetchInBackgroundThenPlaySound().execute("getMatchAgainst", filename);
//	}
//	// eventually we will implement this because we are either fetching target sounds or
//	// recordings of events.
//	public static void fetchThenPlayEventRecording(View view) {
//		String filename = "donuts.m4a";
//		new FetchInBackgroundThenPlaySound().execute("getEventRecording", filename);
//	}
//	
//	/**
//	 * Fetches the file from the nodejs server in a background thread then plays the file
//	 * in the UI thread. Deletes the file from local storage when playback is done.
//	 * 
//	 */
//	private class FetchInBackgroundThenPlaySound extends AsyncTask<String, Void, String> {
//		
//		
//
//		//args[0] is the method you're calling on the server, either the string "getMatchAgainst"
//		// or the String "getEventRecording"
//		// args[1] is the file you want 
//		protected String doInBackground(String... args) {
//			URI uri = null;
//			String method = args[0];
//			String filename = args[1];
//			try {
//				uri = new URI("http://klement.cs.washington.edu:9999/" + method + "?filename=" + filename);
//			} catch (URISyntaxException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			};
//			HttpClient httpClient = new DefaultHttpClient();
//			HttpGet httpGet = new HttpGet(uri);
//			HttpResponse res = null;
//			try {
//				res = httpClient.execute(httpGet);
//			} catch (ClientProtocolException e) {
//				Log.e(LOG_TAG, "Protocol error: " + e.getMessage());
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				Log.e(LOG_TAG, "IOException error: " + e.getMessage());
//				e.printStackTrace();
//			}
//			if (res == null) {
//				//TODO alert user that the server is down
//				Log.e(LOG_TAG, "Server is down, cannot fetch audio file");
//				httpGet.abort();
//				// null is passed to onPostExecute() and it knows what to do with null input
//				return null;
//			}
//			Header[] headers = res.getAllHeaders();
//			int len = 0;
//			for (int i = 0; i < headers.length; i++) {
//				if (headers[i].getName().equalsIgnoreCase("content-length")) {
//					len = Integer.parseInt(headers[i].getValue());
//					Log.v(LOG_TAG, "file length in bytes: " + len);
//				}
//			}
//			HttpEntity entity = res.getEntity();
//			byte[] temp = new byte[1024];
//			int bytesRead = 0;
//			InputStream in = null;
//			FileOutputStream outputStream = null;
//			try {
//				outputStream = openFileOutput(filename, Context.MODE_WORLD_READABLE); // I don't know of any alternatives
//				in = new BufferedInputStream(entity.getContent());
//				while ((bytesRead = in.read(temp)) > 0) {
//					outputStream.write(temp, 0, bytesRead);
//				}
//				in.close();
//				outputStream.close();
//				in.close();
//			} catch (IOException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//			String path = getApplicationContext().getFilesDir().getAbsolutePath();
//			path += "/";
//			path += filename;
//			Log.v(LOG_TAG, "opening file at location: " + path);	
//			return path;
//		}
//		
//		protected void onPostExecute(String path) {
//			if (path == null) {
//				//TODO alert user that the server is down
//				return;
//			}
//			MediaPlayer mp = new MediaPlayer();
//			mp.setOnCompletionListener(new DeleteAfterPlay(path) {
//				public void onCompletion(MediaPlayer mp) {
//					mp.release();
//					if (deleteFromAppStorage(getPath())) {
//						Log.v(LOG_TAG, "successfully deleted file: " + getPath());
//					} else {
//						Log.e(LOG_TAG, "error deleting file: " + getPath());
//					}
//				}
//			});
//			try {
//				mp.setDataSource(path);
//			} catch (IllegalArgumentException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (SecurityException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IllegalStateException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			try {
//				mp.prepare();
//			} catch (IllegalStateException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			mp.start();	
//		}
//	}
//	/**
//	 * Partial implementation of the class that is called when media playback completes.
//	 * The unimplemented method onCompletion() is implemented in the callback.
//	 */
//	public abstract class DeleteAfterPlay implements MediaPlayer.OnCompletionListener {
//		private String path;
//		public DeleteAfterPlay(String path) {
//			this.path = path;
//		}
//		public String getPath() {
//			return path;
//		}
//	}
//	
//	// deletes a file from local storage
//	public boolean deleteFromAppStorage(String path) {
//		File toDelete = new File(path);
//		return toDelete.delete();
//	}
//	
//	public void saveSoundNameToParse(View view) {
//		EditText soundNameText = (EditText) findViewById(R.id.soundNameText);
//		String soundName = soundNameText.getText().toString();
//		ParseObject sound = new ParseObject("Sound");
//		sound.put("name", soundName);
//		try {
//			sound.save();
//		} catch (ParseException e) {
//			Log.e(LOG_TAG, "Parse Server not available");
//			// server is unavailable
//		}
//		sound.fetchInBackground(new GetCallback<ParseObject>() {
//			public void done(ParseObject object, ParseException e) {
//				if (e == null) {
//					// success, tell user to make sound then subscribe
//					findViewById(R.id.makeSoundInstructions).setVisibility(View.VISIBLE);
//					PushService.subscribe(getApplicationContext(), object.getObjectId(), NotificationActivity.class);
//					Log.v(LOG_TAG, "Subscribing to channel: " + object.getObjectId());
//				} else {
//					// failure, tell user to try again
//				}
//			}
//		});
//	}
}
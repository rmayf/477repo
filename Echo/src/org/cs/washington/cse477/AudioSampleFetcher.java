package org.cs.washington.cse477;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;

public class AudioSampleFetcher {	
	private static final String LOG_TAG = "AudioSampleFetcher";
	private Context context;
	
	public AudioSampleFetcher(Context context) {
		this.context = context;
	}

	public boolean fetchThenPlayTarget(String filename)  {
		try {
			new FetchInBackgroundThenPlaySound().execute("getMatchAgainst", filename);
			return true;
		} catch (Exception e) {
			Log.e(LOG_TAG, "error running FetchInBackgroundThenPlaySound()");
			return false;
		}
	}

	// eventually we will implement this because we are either fetching target sounds or
	// recordings of events.
	public boolean fetchThenPlayEventRecording(String filename) {
		try {
			new FetchInBackgroundThenPlaySound().execute("getEventRecording", filename);
			return true;
		} catch (Exception e) {
			Log.e(LOG_TAG, "error running FetchInBackgroundThenPlaySound()");
			return false;
		}
	}
	
	public abstract class MPOnCompleteListener implements MediaPlayer.OnCompletionListener {}
	
	/**
	 * Fetches the file from the nodejs server in a background thread then plays the file
	 * in the UI thread. Deletes the file from local storage when playback is done.
	 * 
	 */
	private class FetchInBackgroundThenPlaySound extends AsyncTask<String, Void, Void> {
		//args[0] is the method you're calling on the server, either the string "getMatchAgainst"
		// or the String "getEventRecording"
		// args[1] is the file you want 
		protected Void doInBackground(String... args) {
			String method = args[0];
			String filename = args[1];
			
			String url = "http://" + AppInit.host + ":" + AppInit.port + "/" + method + "?filename=" + filename;

			try {
				MediaPlayer mediaPlayer = new MediaPlayer();
				mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mediaPlayer.setOnCompletionListener(new MPOnCompleteListener() {
					
					@Override
					public void onCompletion(MediaPlayer mp) {
						
						mp.reset();
						mp.release();
						mp = null;
						
					}
				});
				mediaPlayer.setDataSource(url);
				mediaPlayer.prepare(); // might take long! (for buffering, etc)
				mediaPlayer.start();
			} catch (Exception e) {
				Log.e(LOG_TAG, "MediaPlayer encountered an error");
			}
			return null;
		}
	}
}

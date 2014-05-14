package org.cs.washington.cse477;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;

public class AudioSampleFetcher {	
	private static final String LOG_TAG = "AudioSampleFetcher";
	private Context context;
	private String host = "128.208.7.228";
	private int port = 9876;
	
	public AudioSampleFetcher(Context context) {
		this.context = context;
	}

	public void fetchThenPlayTarget(String filename)  {
		AsyncTask<String, Void, String> task = new FetchInBackgroundThenPlaySound();
		task.execute("getMatchAgainst", filename);
	}
	// eventually we will implement this because we are either fetching target sounds or
	// recordings of events.
	public void fetchThenPlayEventRecording(String filename) {
		new FetchInBackgroundThenPlaySound().execute("getEventRecording", filename);
	}
	
	/**
	 * Fetches the file from the nodejs server in a background thread then plays the file
	 * in the UI thread. Deletes the file from local storage when playback is done.
	 * 
	 */
	private class FetchInBackgroundThenPlaySound extends AsyncTask<String, Void, String> {
		//args[0] is the method you're calling on the server, either the string "getMatchAgainst"
		// or the String "getEventRecording"
		// args[1] is the file you want 
		protected String doInBackground(String... args) {
			URI uri = null;
			String method = args[0];
			String filename = args[1];
			try {
				//uri = new URI("http://klement.cs.washington.edu:9876/" + method + "?filename=" + filename);
				uri = new URI("http://" + host + ":" + port + "/" + method + "?filename=" + filename);
			} catch (URISyntaxException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			};
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(uri);
			HttpResponse res = null;
			try {
				res = httpClient.execute(httpGet);
			} catch (ClientProtocolException e) {
				Log.e(LOG_TAG, "Protocol error: " + e.getMessage());
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(LOG_TAG, "IOException error: " + e.getMessage());
				e.printStackTrace();
			}
			if (res == null) {
				//TODO alert user that the server is down
				Log.e(LOG_TAG, "Server is down, cannot fetch audio file");
				httpGet.abort();
				// null is passed to onPostExecute() and it knows what to do with null input
				return null;
			}
			
			HttpEntity entity = res.getEntity();
			byte[] temp = new byte[1024];
			int bytesRead = 0;
			InputStream in = null;
			FileOutputStream outputStream = null;
			try {

				outputStream = context.openFileOutput(filename, Context.MODE_WORLD_READABLE); // I don't know of any alternatives
				in = new BufferedInputStream(entity.getContent());
				while ((bytesRead = in.read(temp)) > 0) {
					outputStream.write(temp, 0, bytesRead);
				}
				in.close();
				outputStream.close();
				in.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			String path = context.getApplicationContext().getFilesDir().getAbsolutePath();
			path += "/";
			path += filename;
			Log.v(LOG_TAG, "opening file at location: " + path);	
			return path;
		}
		
		protected void onPostExecute(String path) {
			if (path == null) {
				//TODO alert user that the server is down
				return;
			}
			MediaPlayer mp = new MediaPlayer();
			mp.setOnCompletionListener(new DeleteAfterPlay(path) {
				public void onCompletion(MediaPlayer mp) {
					mp.release();
					if (deleteFromAppStorage(getPath())) {
						Log.v(LOG_TAG, "successfully deleted file: " + getPath());
					} else {
						Log.e(LOG_TAG, "error deleting file: " + getPath());
					}
				}
			});
			try {
				mp.setDataSource(path);
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				mp.prepare();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mp.start();	
		}
	}
	
	/**
	 * Partial implementation of the class that is called when media playback completes.
	 * The unimplemented method onCompletion() is implemented in the callback.
	 */
	public abstract class DeleteAfterPlay implements MediaPlayer.OnCompletionListener {
		private String path;
		public DeleteAfterPlay(String path) {
			this.path = path;
		}
		public String getPath() {
			return path;
		}
	}
	
	// deletes a file from local storage
	public boolean deleteFromAppStorage(String path) {
		File toDelete = new File(path);
		return toDelete.delete();
	}
	
	/*
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
	*/

}

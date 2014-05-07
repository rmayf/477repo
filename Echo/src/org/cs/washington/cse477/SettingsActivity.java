package org.cs.washington.cse477;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
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

	
	public void fetchThenPlaySound(View view)  {
		String filename = "isaiahtest.ogg";
		new FetchInBackgroundThenPlaySound().execute(filename);
	}
	
	private class FetchInBackgroundThenPlaySound extends AsyncTask<String, Void, String> {
		
		protected String doInBackground(String... filename) {
			URI uri = null;
			try {
				uri = new URI("http://klement.cs.washington.edu:9999/getMatchAgainst?filename=" + filename[0]);
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
			Header[] headers = res.getAllHeaders();
			int len = 0;
			for (int i = 0; i < headers.length; i++) {
				if (headers[i].getName().equalsIgnoreCase("content-length")) {
					len = Integer.parseInt(headers[i].getValue());
					Log.v(LOG_TAG, "file length in bytes: " + len);
				}
			}
			HttpEntity entity = res.getEntity();
			byte[] temp = new byte[1024];
			int bytesRead = 0;
			InputStream in = null;
			FileOutputStream outputStream = null;
			try {
				outputStream = openFileOutput(filename[0], Context.MODE_WORLD_READABLE);
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
			String path = getApplicationContext().getFilesDir().getAbsolutePath();
			path += "/";
			path += filename[0];
			Log.v(LOG_TAG, "opening file at location: " + path);	
			return path;
		}
		
		protected void onPostExecute(String path) {
			MediaPlayer mp = new MediaPlayer();
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
	
	public void putSoundInRaw() {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy); 
		URI uri = null;
		String filename = "isaiahtest.ogg";
		try {
			uri = new URI("http://klement.cs.washington.edu:9999/getMatchAgainst?filename=" + filename);
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
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
		Header[] headers = res.getAllHeaders();
		int len = 0;
		for (int i = 0; i < headers.length; i++) {
			if (headers[i].getName().equalsIgnoreCase("content-length")) {
				len = Integer.parseInt(headers[i].getValue());
				Log.v(LOG_TAG, "file length in bytes: " + len);
			}
		}
		HttpEntity entity = res.getEntity();
		byte[] temp = new byte[1024];
		int bytesRead = 0;
		InputStream in = null;
		FileOutputStream outputStream = null;
		try {
			outputStream = openFileOutput(filename, Context.MODE_WORLD_READABLE);
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
		String path = getApplicationContext().getFilesDir().getAbsolutePath();
		path += "/";
		path += filename;
		Log.v(LOG_TAG, "opening file at location: " + path);
		
		MediaPlayer mp = new MediaPlayer();
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
	
	public boolean deleteFromAppStorage(String path) {
		File toDelete = new File(path);
		return toDelete.delete();
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
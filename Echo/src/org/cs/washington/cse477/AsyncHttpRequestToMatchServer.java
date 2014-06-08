package org.cs.washington.cse477;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;
import android.util.Log;

public class AsyncHttpRequestToMatchServer extends
		AsyncTask<String, Void, Integer> {
	public static final String LOG_TAG = "AsyncHttpRequestToMatchServer";

	@Override
	protected Integer doInBackground(String... params) {
		URI uri = null;
		Log.v(LOG_TAG, "http request url: " + params[0]);
		try {
			uri = new URI(params[0]);
		} catch (URISyntaxException e) {
			Log.e(LOG_TAG, e.getMessage());
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
		if (res == null) {
			// TODO alert user that the server is down
			Log.e(LOG_TAG, "Server is down, cannot fetch audio file");
			httpGet.abort();
			// null is passed to onPostExecute() and it knows what to do with
			// null input
			return null;
		}
		Integer statusCode = res.getStatusLine().getStatusCode();
		Log.v(LOG_TAG, "http response: " + statusCode);
		return statusCode;
	}
}
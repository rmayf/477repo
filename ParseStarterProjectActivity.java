package com.parse.starter;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

public class ParseStarterProjectActivity extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sendkeyvalue);

		ParseAnalytics.trackAppOpened(getIntent());
		
	}
	
	public void sendMessage(View view) {
		EditText keyText = (EditText) findViewById(R.id.key_text);
		String key = keyText.getText().toString();
		EditText valueText = (EditText) findViewById(R.id.value_text);
		String value = valueText.getText().toString();
		ParseObject testObject = new ParseObject("KeyValuePair");
		testObject.put("key", key);
		testObject.put("value", value);
		testObject.saveInBackground();
	}
}

package com.parse.starter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

public class SignUpActivity extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.signup);

		ParseAnalytics.trackAppOpened(getIntent());
		
	}
	
	public void signUp(View view) {
		EditText emailText = (EditText) findViewById(R.id.email_text);
		String email = emailText.getText().toString();
		EditText passwordText = (EditText) findViewById(R.id.password_text);
		String password = passwordText.getText().toString();	
		ParseUser user = new ParseUser();
		user.setUsername(email);
		user.setPassword(password);
		user.signUpInBackground(new SignUpCallback() {
			  public void done(ParseException e) {
			    if (e == null) {
			      // Hooray! Let them use the app now.
			    	goToSendDataPage();
			    } else {
			      // Sign up didn't succeed. Look at the ParseException
			      // to figure out what went wrong
			    }
			  }
			});
	}
	
	private void goToSendDataPage() {
		Intent intent = new Intent(this, ParseStarterProjectActivity.class);
    	startActivity(intent);
	}
}

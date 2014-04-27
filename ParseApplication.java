package com.parse.starter;

import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseObject;
import com.parse.ParseUser;

import android.app.Application;

public class ParseApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		// Add your initialization code here
		Parse.initialize(this, "0JBJLFotV9xF1MvdmihvrH8Ozx8EG9CPNlHX2WFM", "SLLO91eO7kCcdIn51gMTHmTCum0WvhWybPZ2CUZC");
	}

}

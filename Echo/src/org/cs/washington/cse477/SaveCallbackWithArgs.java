package org.cs.washington.cse477;

import com.parse.SaveCallback;

public abstract class SaveCallbackWithArgs extends SaveCallback {
	private String str;
	
	public SaveCallbackWithArgs(String str) {
		this.str = str;
	}
	
	public String getString() {
		return str;
	}
}

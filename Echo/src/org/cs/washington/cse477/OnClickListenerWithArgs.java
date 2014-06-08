package org.cs.washington.cse477;

import android.view.View;

public abstract class OnClickListenerWithArgs implements View.OnClickListener {
	private String str = null;
	private String objectId = null;
	
	public OnClickListenerWithArgs(String str) {
		this.str = str;
	}
	public OnClickListenerWithArgs(String filename, String objectId) {
		str = filename;
		this.objectId = objectId;
	}
	
	public String getString() {
		return str;
	}
	
	public String getObjectIdString() {
		return objectId;
	}
}
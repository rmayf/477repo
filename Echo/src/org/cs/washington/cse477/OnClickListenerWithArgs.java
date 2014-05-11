package org.cs.washington.cse477;

import android.view.View;

public abstract class OnClickListenerWithArgs implements View.OnClickListener {
	private String str;
	
	public OnClickListenerWithArgs(String str) {
		this.str = str;
	}
	
	public String getString() {
		return str;
	}
}
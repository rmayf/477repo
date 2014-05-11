package org.cs.washington.cse477;

import com.parse.FindCallback;
import com.parse.ParseObject;

public abstract class FindCallbackWithArgs extends FindCallback<ParseObject> {
	private boolean bool;
	
	public FindCallbackWithArgs(boolean bool) {
		this.bool = bool;
	}
	
	public boolean getBool() {
		return bool;
	}
}

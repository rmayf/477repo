package org.cs.washington.cse477;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.os.Build;

public class NotificationActivity extends ActionBarActivity {

	protected List<String> notifications;
	protected ArrayAdapter<String> notifyAdapter;
	protected ListView notifyView;
	
	protected String[] dbg = {"notify 1", "notify 2", "notify 3"};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notification);
		notifyView = (ListView) findViewById(R.id.notification_listview);
		notifications = new LinkedList<String>();
		for (int i=0; i<dbg.length; i++) {
			notifications.add(dbg[i]);
		}
		notifyAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
				android.R.id.text1, notifications);
		notifyView.setAdapter(notifyAdapter);
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.notification, menu);
		return true;
	}

	public void receivePush(String n) {
		notifications.add(0, n);
		notifyAdapter.notifyDataSetChanged();
	}
	
	/**
	 * refresh()
	 * 
	 * pull 20 newest notifications from parse and display
	 */
	int k = 0;
	public void refresh() {
		//notifications.clear();
		
		// debug code
		notifications.add("notify " + Integer.toString(k));
		k++;
		
		notifyAdapter.notifyDataSetChanged();
	}
	
	public void addNewSample() {
		AddAudioSampleDialog d = new AddAudioSampleDialog();
		d.show(getFragmentManager(), "addaudiosampledialog");
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		Intent intent;
		switch(item.getItemId()) {
		case R.id.notification_action_setup_device:
			intent = new Intent(this, DeviceSetupActivity.class);
	    	startActivity(intent);
			return true;
		case R.id.notification_action_settings:
			intent = new Intent(this, SettingsActivity.class);
	    	startActivity(intent);
			return true;
		case R.id.notification_action_add_sample:
			// pop-up dialog with instructions
			addNewSample();
			
			// when push received for new audio sample, goto audio upload activity
			//intent = new Intent(this, AudioUploadActivity.class);
			// put data to intent that we can pull out from within AudioUploadActivity
			//startActivity(intent);
			return true;
		case R.id.notification_action_refresh:
			refresh();
			return true;
		
		/*
		case R.id.notification_action_audio_upload:
			intent = new Intent(this, AudioUploadActivity.class);
	    	startActivity(intent);
			return true;
		case R.id.notification_action_add_device:
			intent = new Intent(this, DeviceRegActivity.class);
	    	startActivity(intent);
			return true;
		case R.id.notification_action_device_status:
			intent = new Intent(this, DeviceStatusActivity.class);
	    	startActivity(intent);
			return true;
		case R.id.notification_action_logout:
			// logout and exit
			return true;
		*/
		default:
			return super.onOptionsItemSelected(item);
		}
	}	
}

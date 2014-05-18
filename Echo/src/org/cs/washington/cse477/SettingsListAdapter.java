package org.cs.washington.cse477;

import java.util.ArrayList;
import java.util.List;

import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;


public class SettingsListAdapter extends ArrayAdapter<ParseObject> {
	public static final String LOG_TAG = "SettingsListAdapter";
	private final Context context;
	private final List<ParseObject> values;
	private final List<Boolean> states;
	private FragmentManager fm;
	
	public SettingsListAdapter(Context context, List<ParseObject> values, FragmentManager fm) {
		super(context, R.layout.settings_listview_item, values);
		this.context = context;
		this.values = values;
		this.fm = fm;
		// store old values of the enabled state for each ParseObject
		states = new ArrayList<Boolean>(values.size());
		for (int i = 0; i < values.size(); i++) {
			states.add(i, values.get(i).getBoolean("enabled"));
		}
	}

	/**
	 * Method for owners of instances of this Adapter to fetch the list of objects
	 */
	public List<Boolean> getEnabledStates() {
		return states;
	}
	
	public List<ParseObject> getValues() {
		return values;
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = convertView;
        if (rowView == null) {
        	rowView = inflater.inflate(R.layout.settings_listview_item, parent, false);
        }
		
        // fetch current row's corresponding ParseObject
		ParseObject obj = values.get(position);
		final String objectId = obj.getObjectId();
		
		// set the TextView to display Sound object's name
		TextView textview = (TextView) rowView.findViewById(R.id.settings_text);
		final String text = (String) obj.get("name");
		textview.setText(text);

		
		Switch toggle = (Switch) rowView.findViewById(R.id.settings_toggle);
		toggle.setChecked(obj.getBoolean("enabled"));
		
		// Switch click handling
		// On toggle, store updated state of switch in states list
		// The states List is used to persist the enabled setting back to Parse at some future time
		toggle.setOnClickListener(new OnClickListenerWithArgs(text) {
			
			@Override
			public void onClick(View v) {
				Log.v(LOG_TAG,"position: " + position);
				boolean checked = Boolean.valueOf(((Switch) v).isChecked());
				Log.v(LOG_TAG,"checked: " + checked);
				states.set(position, Boolean.valueOf(((Switch) v).isChecked()));
				// update subscription information on parse
				ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Sound");
				Log.v(LOG_TAG, "updating subscription status for sound: " + getString() + 
						" to: " + (checked ? "on" : "off"));
				query.whereEqualTo("name", getString());
				query.findInBackground(new FindCallbackWithArgs(checked) {
					public void done(List<ParseObject> result, ParseException e) {
						if (e == null) {
							ParseObject sound = (ParseObject) result.get(0);
							sound.put("enabled", getBool());
							sound.saveInBackground();
						}
					}
				});
			}
		});		
		
		ImageView img = (ImageView) rowView.findViewById(R.id.settings_play);
		img.setClickable(true);
		img.setOnClickListener(new OnClickListener() {
		
			@Override
			public void onClick(View v) {
				// on click of the play button (image), initiate fetch of sound and playback
				Log.v(LOG_TAG,"clicked settings play");
				ParseInit.asf.fetchThenPlayTarget(objectId +".wav");
			}			
		});
		ImageView delete = (ImageView) rowView.findViewById(R.id.settings_delete);
		delete.setClickable(true);
		// text = "name" field of ParseObject
		delete.setOnClickListener(new OnClickListenerWithArgs(text) {
			
			@Override
			public void onClick(View v) {
				// show the dialog
				ConfirmDeleteDialog confirmDelete = new ConfirmDeleteDialog();
				Bundle b = new Bundle();
				b.putString("name", text);
				confirmDelete.setArguments(b);
				confirmDelete.show(fm, "confirm_delete");
				
				// do the delete
				//doDelete(getString());
			}
		});
		
		return rowView;
	}
}

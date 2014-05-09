package org.cs.washington.cse477;

import java.util.ArrayList;
import java.util.List;

import com.parse.ParseObject;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsListAdapter extends ArrayAdapter<ParseObject> {

	private final Context context;
	private final List<ParseObject> values;
	private final List<Boolean> states;
	
	public SettingsListAdapter(Context context, List<ParseObject> values) {
		super(context, R.layout.settings_listview_item, values);
		this.context = context;
		this.values = values;
		states = new ArrayList<Boolean>(values.size());
		
		for (int i = 0; i < values.size(); i++) {
			states.add(i, Boolean.valueOf(false));
		}
		Log.e("DEBUG","size of values: " + values.size());
		Log.e("DEBUG","size of states: " + states.size());
	}

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
		
		ParseObject obj = values.get(position);
		
		TextView textview = (TextView) rowView.findViewById(R.id.settings_text);
		textview.setText((String) obj.get("name"));

		Switch toggle = (Switch) rowView.findViewById(R.id.settings_toggle);
		toggle.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.e("DEBUG","position: " + position);
				Log.e("DEBUG","checked: " + Boolean.valueOf(((Switch) v).isChecked()));
				states.set(position, Boolean.valueOf(((Switch) v).isChecked()));
			}
		});
		/*
		boolean en = false;
		String enStr = (String) obj.get("enabled");
		if (enStr.equalsIgnoreCase("enabled")) {
			en = true;
		} else {
			en = false;
		}
		*/
		
		toggle.setChecked(states.get(position).booleanValue());		
		
		ImageView img = (ImageView) rowView.findViewById(R.id.settings_play);
		img.setClickable(true);
		img.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// on click of the play button (image), initiate fetch of sound and playback
				Log.e("DEBUG","clicked settings play");
			}			
		});
		
		return rowView;
	}
}

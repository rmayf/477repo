package org.cs.washington.cse477;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class SettingsListAdapter extends ArrayAdapter<String> {

	private final Context context;
	private final List<String> values;
	  
	public SettingsListAdapter(Context context, List<String> values) {
		super(context, R.layout.settings_listview_item, values);
		this.context = context;
		this.values = values;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.settings_listview_item, parent, false);
		TextView textview = (TextView) rowView.findViewById(R.id.settings_text);
		textview.setText(values.get(position));
		return rowView;
	}
}

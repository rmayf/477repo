package org.cs.washington.cse477;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class NotificationListAdapter extends ArrayAdapter<String> {

	private final Context context;
	private final List<String> values;
	  
	public NotificationListAdapter(Context context, List<String> values) {
		super(context, R.layout.notification_listview_item, values);
		this.context = context;
		this.values = values;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.notification_listview_item, parent, false);
		TextView textview = (TextView) rowView.findViewById(R.id.notification_text);
		textview.setText(values.get(position));
		return rowView;
	}
}

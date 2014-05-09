package org.cs.washington.cse477;

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
import android.widget.TextView;

public class NotificationListAdapter extends ArrayAdapter<ParseObject> {
	public static final String LOG_TAG = "NotificationListAdapter";
	
	private final Context context;
	private final List<ParseObject> values;
	  
	public NotificationListAdapter(Context context, List<ParseObject> values) {
		super(context, R.layout.notification_listview_item, values);
		this.context = context;
		this.values = values;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = convertView;
        if (rowView == null) {
        	rowView = inflater.inflate(R.layout.notification_listview_item, parent, false);
        }
		
		TextView textview = (TextView) rowView.findViewById(R.id.notification_text);
		final String text = (String)values.get(position).get("eventFilename");
		Log.v(LOG_TAG, "populating: " + text);
		textview.setText(text);
		
		ImageView img = (ImageView) rowView.findViewById(R.id.notification_play);
		img.setClickable(true);
		img.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// on click of the play button (image), initiate fetch of sound and playback
				Log.v("DEBUG","clicked notifications play for file: " + text);
				ParseInit.asf.fetchThenPlayEventRecording(text);
			}
		});

		return rowView;
	}
}

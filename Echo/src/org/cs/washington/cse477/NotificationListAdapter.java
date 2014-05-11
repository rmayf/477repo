package org.cs.washington.cse477;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

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
        ParseObject obj = values.get(position);
		TextView textview = (TextView) rowView.findViewById(R.id.notification_text);
		String filename = (String) obj.get("eventFilename");
		Date time = obj.getCreatedAt();
		String text = time.toString();
		boolean match = obj.getBoolean("match");
		if (match) {
			String matchFilename = obj.getString("matchFilename");
			ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Sound");
			query.whereEqualTo("objectId", matchFilename);
			try {
				List<ParseObject> results = query.find();
				if (results.size() == 1) {
					ParseObject result = results.get(0);
					String soundName = result.getString("name");
					text += ": ";
					text += soundName;
				} else {
					Log.e(LOG_TAG, "Expected one result from query but got: " + results.size());
					text += ": no match found";
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			text += ": no match found";
		}
		Log.v(LOG_TAG, "populating: " + text);
		textview.setText(text);
		
		ImageView img = (ImageView) rowView.findViewById(R.id.notification_play);
		img.setClickable(true);
		img.setOnClickListener(new OnClickListenerWithArgs(filename) {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// on click of the play button (image), initiate fetch of sound and playback
				Log.v("DEBUG","clicked notifications play for file: " + getString());
				ParseInit.asf.fetchThenPlayEventRecording(getString());
			}
		});

		return rowView;
	}
}

package org.cs.washington.cse477;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.ParseObject;

public class NotificationListAdapter extends ArrayAdapter<ParseObject> {
	public static final String TAG = "NotificationListAdapter";
	
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
    	rowView = inflater.inflate(R.layout.notification_listview_item, parent, false);
        ParseObject obj = values.get(position);
        String filename = (String) obj.get("eventFilename");
        
        TextView textview = (TextView) rowView.findViewById(R.id.notification_text);
		TextView dateview = (TextView) rowView.findViewById(R.id.notification_text_date);
		
		String text;
		Date date = obj.getCreatedAt();
		DateFormat df = new SimpleDateFormat("h:mm:ss aaa '('z')'  'on'  EEE MMM dd, yyyy",Locale.US);
		String dateText = df.format(date);
		
		boolean match = obj.getBoolean("match");
		if (match) {
			text = obj.getString("matchSoundName");
		} else {
			text = "Loud Unrecognized Event";
		}
		
		Log.v(TAG, "populating: " + text + " " + date);
		
		textview.setText(text);
		dateview.setText(dateText);
		
		final ImageView img = (ImageView) rowView.findViewById(R.id.notification_play);
		img.setClickable(true);
		if (!AppInit.asf.playingSound) {
			img.setImageResource(R.drawable.ic_action_play);
		} else if (AppInit.asf.playingSound && filename.equals(AppInit.asf.filePlaying)) {
			img.setImageResource(R.drawable.ic_action_stop);
		} else {
			img.setImageResource(R.drawable.ic_action_play);
		}
		img.setOnClickListener(new OnClickListenerWithArgs(filename) {
			
			@Override
			public void onClick(View v) {
				// on click of the play button (image), initiate fetch of sound and playback
				Log.v(TAG,"clicked notifications play for file: " + getString());
				img.setImageResource(R.drawable.ic_action_stop);
				AppInit.asf.fetchThenPlayEventRecording(getString());
			}
		});

		return rowView;
	}
}

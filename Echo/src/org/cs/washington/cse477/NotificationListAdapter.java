package org.cs.washington.cse477;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
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
	
	private FragmentManager fm;
	  
	public NotificationListAdapter(Context context, List<ParseObject> values, FragmentManager fm) {
		super(context, R.layout.notification_listview_item, values);
		this.context = context;
		this.values = values;
		this.fm = fm;
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
		// set the click listener for the play 
		final ImageView img = (ImageView) rowView.findViewById(R.id.notification_play);
		img.setClickable(true);
		img.setImageResource(R.drawable.ic_action_play);
		img.setOnClickListener(new OnClickListenerWithArgs(filename) {
			
			@Override
			public void onClick(View v) {
				// on click of the play button (image), initiate fetch of sound and playback
				Log.v(TAG,"clicked notifications play for file: " + getString());
				AppInit.asf.fetchThenPlayEventRecording(getString());
			}
		});
		ImageView deleteButtonImg = (ImageView) rowView.findViewById(R.id.notification_delete);
		deleteButtonImg.setClickable(true);
		deleteButtonImg.setOnClickListener(new OnClickListenerWithArgs(filename, obj.getObjectId()) {

			@Override
			public void onClick(View v) {
				ConfirmDeleteDialog confirmDelete = new ConfirmDeleteDialog();
				Bundle b = new Bundle();
				b.putString("filename", getString());
				b.putString("objectId", getObjectIdString());
				confirmDelete.setArguments(b);
				confirmDelete.show(fm, "confirm_delete");
			}
			
		});

		return rowView;
	}
}

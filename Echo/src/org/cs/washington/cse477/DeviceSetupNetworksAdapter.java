package org.cs.washington.cse477;

import java.util.List;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class DeviceSetupNetworksAdapter extends ArrayAdapter<ScanResult> {
	private static final String LOG_TAG = "DeviceSetupNetworksAdapter";
	
	private final Context mContext;
	private final List<ScanResult> mScanResults;
	  
	public DeviceSetupNetworksAdapter(Context context, int textViewResourceId, List<ScanResult> values) {
		super(context, textViewResourceId, values);
		mContext = context;
		mScanResults = values;
	}
	
	protected View getCustomView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = convertView;
        if (rowView == null) {
        	rowView = inflater.inflate(R.layout.networks_listview_item, parent, false);
        }

        TextView text = (TextView) rowView.findViewById(R.id.network_text);
        text.setText(mScanResults.get(position).SSID);
        Log.v(LOG_TAG,"position : " + position + " | SSID : " + text.getText());
        return rowView;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return getCustomView(position, convertView, parent);
	}	
	
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return getCustomView(position, convertView, parent);
	}
	
}
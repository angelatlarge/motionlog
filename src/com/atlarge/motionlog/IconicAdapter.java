package com.atlarge.motionlog;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class IconicAdapter extends android.widget.BaseAdapter implements SpinnerAdapter {
	private final int stringArrayResourceID;
	private final TypedArray stringIds;
	private final TypedArray icons;
//		private final int textViewResourceId;
	private final int viewBasicRowResourceID;
	private final int viewDroppedRowResourceID;
	private final Context context;
	private final int viewRowTextResourceID; 
	private final int viewRowIconResourceID;
	

	public IconicAdapter(
			Context _context, 
//				int _textViewResourceId, 
			int _stringArrayResourceID,
			int _iconArrayResourceID, 
			int _viewBasicRowResourceID, 
			int _viewDroppedRowResourceID, 
			int _viewRowTextResourceID, 
			int _viewRowIconResourceID
			) {
		super();
		context = _context;
//			textViewResourceId = _textViewResourceId;
		viewBasicRowResourceID = _viewBasicRowResourceID;
		viewDroppedRowResourceID = _viewDroppedRowResourceID;
		stringArrayResourceID = _stringArrayResourceID;
		Resources res = context.getResources();
		if (_stringArrayResourceID != 0) {
			// Real resource ID
			stringIds = res.obtainTypedArray(_stringArrayResourceID);
		} else {
			// Null resource id
			stringIds = null;
		}
		if (_iconArrayResourceID != 0) {
			// Real resource ID 
			icons = res.obtainTypedArray(_iconArrayResourceID);
		} else {
			// Nonexistent resource ID
			icons = null;
		}
		viewRowTextResourceID = _viewRowTextResourceID; 
		viewRowIconResourceID = _viewRowIconResourceID;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return getIconicView(position, convertView, parent, viewBasicRowResourceID);
	}
	
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return getIconicView(position, convertView, parent, viewDroppedRowResourceID);
	}	
	
	public String getPositionalString(int pos) {
		return context.getResources().getString(stringIds.getResourceId(pos, 0));
	}
	
	public int getStringId(int pos) {
		return stringIds.getResourceId(pos, 0);
	}
	private View getIconicView(int pos, View convertView, ViewGroup parent, int rowResourceID) {
		// This is for the regular view
		LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
		View rowView = inflater.inflate(rowResourceID, parent, false);
		if (stringIds != null) {
			// There is a string array
			TextView label=(TextView)rowView.findViewById(viewRowTextResourceID);
			if (label != null) {
				// Label present
				if (stringIds.length()>pos)	// out-of-bounds check
					label.setText(getPositionalString(pos));
			}
		}
		
		if (icons != null) {
			ImageView icon=(ImageView)rowView.findViewById(viewRowIconResourceID);
			if (icon != null) {
				// Icon present
				if (icons.length()>pos)	// out-of-bounds check
					icon.setImageDrawable(icons.getDrawable(pos));
			}
		}

		return rowView;
	}

	@Override
	public int getCount() {
		return stringIds.length();
	}

	@Override
	public Object getItem(int pos) {
		return getPositionalString(pos);
	}

	@Override
	public long getItemId(int pos) {
		return pos;
	}
}

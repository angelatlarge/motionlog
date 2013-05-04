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

/**
 * This class is an adapter that can be used with ListViews and Spinners
 * to produce iconified items.
 * 
 *  In spinners it can have different dropped-down view from the regular view
 *  such as displaying only an icon (like a button) normally, 
 *  but displaying an icon plus some text when dropped down
 */
public class IconicAdapter extends android.widget.BaseAdapter implements SpinnerAdapter {
	private final TypedArray mStringIds;
	private final TypedArray icons;
	private final int mBasicRowViewResourceID;
	private final int mDroppedRowViewResourceID;
	private final int mTextViewResourceID; 
	private final int mIconViewResourceID;
	private final Resources mRes;
	private final LayoutInflater mInflater;
	
	/**
	 * Pre-computed / memoized count of items we will display
	 */
	private final int mCount;
	

	/**
	 * Constructor
	 *
	 * @param context					Context. We need it to get access the resources
	 * @param stringArrayResourceID		Resource id of a string array providing the text for each item
	 * @param iconArrayResourceID		Resource id of an arraw of drawables, providing the drawables for each item
	 * @param basicRowViewResourceID	Resource id of a layout specitying "regular" view
	 * @param droppedRowViewResourceID	Resource id of a layout specitying "dropped-down" view
	 * @param mTextViewResourceID		Resource id of the text view in the row layour
	 * @param mIconViewResourceID		Resource id of the icon view in the row layout
	 */
	public IconicAdapter(
			Context context, 
			int stringArrayResourceID,
			int iconArrayResourceID, 
			int basicRowViewResourceID, 
			int droppedRowViewResourceID, 
			int textViewResourceID, 
			int iconViewResourceID
			) {
		super();
				
		// Save the parameters
		mTextViewResourceID = textViewResourceID; 
		mIconViewResourceID = iconViewResourceID;
		mBasicRowViewResourceID = basicRowViewResourceID;
		mDroppedRowViewResourceID = droppedRowViewResourceID;

		// Save the inflater, so that we don't have to save the context
		mInflater = (LayoutInflater)context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
				
		// Get the resources
		mRes = context.getResources();
				
		// Load the string array
		if (stringArrayResourceID != 0) {
			// We have a eal resource ID
			mStringIds = mRes.obtainTypedArray(stringArrayResourceID);
		} else {
			// Null resource id
			mStringIds = null;
		}
		
		// Load the icon array		
		if (iconArrayResourceID != 0) {
			// Real resource ID 
			icons = mRes.obtainTypedArray(iconArrayResourceID);
		} else {
			// Nonexistent resource ID
			icons = null;
		}
		
		// Compute the number of items
		if (mStringIds != null) {
			mCount =  (icons != null) ? Math.max(mStringIds.length(), icons.length()) : mStringIds.length() ; 
		} else {
			mCount =  (icons != null) ? icons.length() : 0; 
		}
	}
	
	/**
	 * Returns a regular view
	 * 
	 * We call the workhorse method getIconicView() specifying basic view resource id
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return getIconicView(position, convertView, parent, mBasicRowViewResourceID);
	}
	
	/**
	 * Returns a dropped-down view
	 * 
	 * We call the workhorse method getIconicView() specifying dropped-down view resource id
	 */
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return getIconicView(position, convertView, parent, mDroppedRowViewResourceID);
	}	
	
	public String getPositionalString(int pos) {
		return mRes.getString(mStringIds.getResourceId(pos, 0));
	}
	
	public int getStringId(int pos) {
		return mStringIds.getResourceId(pos, 0);
	}
	
	/**
	 * Constructs a view, and fills it with the specified string and drawable based on position
	 */
	private View getIconicView(int pos, View convertView, ViewGroup parent, int rowResourceID) {
		// Get the view and inflate it
		View rowView = mInflater.inflate(rowResourceID, parent, false);
		
		// Set the string is strings exist
		if (mStringIds != null) {
			// There is a string array
			TextView label=(TextView)rowView.findViewById(mTextViewResourceID);
			if (label != null) {
				// Label present
				if (mStringIds.length()>pos)	// out-of-bounds check
					label.setText(getPositionalString(pos));
			} // else: there is no TextView in this layout, nothing to do
		} // else : string array is null, nothing to do
		
		if (icons != null) {
			ImageView icon=(ImageView)rowView.findViewById(mIconViewResourceID);
			if (icon != null) {
				// ImageView is present in this layout
				if (icons.length()>pos)	// out-of-bounds check
					icon.setImageDrawable(icons.getDrawable(pos));
			} // else: there is no ImageView in this layout, nothing to do
		} // else : icon array is null, nothing to do

		// Return the layout view
		return rowView;
	}

	@Override
	public int getCount() {
		return mCount;
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

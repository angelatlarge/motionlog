package com.atlarge.motionlog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;

public class IconicPopupMenuListPopupWindow  {
	public static void showPopup(
			Context context, 
//			Point p, 
			ListAdapter adapter, 
			View anchor, 
			AdapterView.OnItemClickListener clickListener, 
			PopupWindow.OnDismissListener dismissListener
		) {
		
			ListPopupWindow listPopupWindow = new ListPopupWindow(context);
			listPopupWindow.setAnchorView(anchor);
			listPopupWindow.setAdapter(adapter);
			listPopupWindow.setModal(true);
//			listPopupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
//			listPopupWindow.setWidth(300);

            // Display mListPopupWindow on most left of the screen
            //~ mListPopupWindow.setHorizontalOffset(-1000);


            listPopupWindow.setOnItemClickListener(clickListener);
            listPopupWindow.setOnDismissListener(dismissListener);

            listPopupWindow.show();
            View parentView = (View)listPopupWindow.getListView().getParent();
            parentView.setLayoutParams(new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT));
//            		ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//            parentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//            Caused by: java.lang.ClassCastException: android.widget.PopupWindow$PopupViewContainer cannot be cast to android.widget.PopupWindow
//            PopupWindow parentView = (PopupWindow)listPopupWindow.getListView().getParent().getParent();
//			Caused by: java.lang.ClassCastException: android.view.ViewRootImpl cannot be cast to android.widget.PopupWindow            
//            parentView.setWidth(LayoutParams.WRAP_CONTENT);
            
	}
/*		
	private static void showStatusPopup(final Activity context, Point p) {

		// Create the linear layout for the items
		LinearLayout layout = new LinearLayout(XXX);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
	   LinearLayout viewGroup = (LinearLayout) context.findViewById(R.id.llStatusChangePopup);
	   LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	   View layout = layoutInflater.inflate(R.layout.status_popup_layout, null);

	   // Creating the PopupWindow
	   changeStatusPopUp = new PopupWindow(context);
	   changeStatusPopUp.setContentView(layout);
	   changeStatusPopUp.setWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
	   changeStatusPopUp.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
	   changeStatusPopUp.setFocusable(true);

	   // Some offset to align the popup a bit to the left, and a bit down, relative to button's position.
	   int OFFSET_X = -20;
	   int OFFSET_Y = 50;

	   //Clear the default translucent background
	   changeStatusPopUp.setBackgroundDrawable(new BitmapDrawable());

	   // Displaying the popup at the specified location, + offsets.
	   changeStatusPopUp.showAtLocation(layout, Gravity.NO_GRAVITY, p.x + OFFSET_X, p.y + OFFSET_Y);
	}	
*/
}

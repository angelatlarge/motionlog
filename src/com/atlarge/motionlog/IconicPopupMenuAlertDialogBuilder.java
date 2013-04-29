package com.atlarge.motionlog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListAdapter;

public class IconicPopupMenuAlertDialogBuilder  {
	public static void showPopup(
			Context context, 
//			Point p, 
			ListAdapter adapter, 
			DialogInterface.OnClickListener listener
		) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setAdapter(adapter, listener);
		AlertDialog dialog=builder.create();
//		WindowManager.LayoutParams layoutParams = window.getAttributes();
//		layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
//		window.setAttributes(layoutParams);
		dialog.show();
//		Window window = dialog.getWindow();
//		window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
//		dialog.getWindow().setLayout(300, 300); //Controlling width and height.
		dialog.getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT); //Controlling width and height.
/*		
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
*/		   		
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

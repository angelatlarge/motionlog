package com.atlarge.motionlog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.WindowManager;
import android.widget.ListAdapter;

public class IconicPopupMenuAlertDialogBuilder  {
	public static void showPopup(
			Context context, 
			ListAdapter adapter, 
			DialogInterface.OnClickListener listener
		) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setAdapter(adapter, listener);
		AlertDialog dialog=builder.create();
		dialog.show();
		dialog.getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT); //Controlling width and height.
	}
}

package com.atlarge.motionlog;

import android.content.Context;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;

public class IconicPopupMenuListPopupWindow  {
	public static void showPopup(
			Context context, 
			ListAdapter adapter, 
			View anchor, 
			AdapterView.OnItemClickListener clickListener, 
			PopupWindow.OnDismissListener dismissListener
		) {
		
			ListPopupWindow listPopupWindow = new ListPopupWindow(context);
			listPopupWindow.setAnchorView(anchor);
			listPopupWindow.setAdapter(adapter);
			listPopupWindow.setModal(true);

            listPopupWindow.setOnItemClickListener(clickListener);
            listPopupWindow.setOnDismissListener(dismissListener);

            listPopupWindow.show();
            View parentView = (View)listPopupWindow.getListView().getParent();
            parentView.setLayoutParams(new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT));
            
	}
}

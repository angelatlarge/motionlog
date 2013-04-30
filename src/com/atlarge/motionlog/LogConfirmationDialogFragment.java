package com.atlarge.motionlog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

public class LogConfirmationDialogFragment extends DialogFragment {
    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface DialogListener {
        public void onDialogPositiveClick(DialogFragment dialog, boolean doNotAskAgain);
        public void onDialogNegativeClick(DialogFragment dialog, boolean doNotAskAgain);
        public void onDialogCancel(DialogFragment dialog, boolean doNotAskAgain);
    }
	
    // Use this instance of the interface to deliver action events
    DialogListener mListener;
    View checkboxLayout;
    
    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (DialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString() + " must implement NoticeDialogListener");
        }
    }
	
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
		Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        
        // Create the checkbox and add it
        Context context = activity.getApplicationContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        checkboxLayout = inflater.inflate(R.layout.log_confirmation_extra, null);
        builder.setView(checkboxLayout);
        
        // Add regular IO
        builder.setMessage(R.string.log_confirmation_message)
               .setPositiveButton(R.string.proceed, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   CheckBox checkbox = (CheckBox)checkboxLayout.findViewById(R.id.checkbox_donotaskagain);
                	   mListener.onDialogPositiveClick(LogConfirmationDialogFragment.this, checkbox.isChecked());
                   }
               })
               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // User cancelled the dialog
                	   CheckBox checkbox = (CheckBox)checkboxLayout.findViewById(R.id.checkbox_donotaskagain);
                	   mListener.onDialogNegativeClick(LogConfirmationDialogFragment.this, checkbox.isChecked());
                   }
               });
        
        
        // Create the AlertDialog object
        return builder.create();
    }
    
    @Override
    public void onCancel (DialogInterface dialog) {
    	CheckBox checkbox = (CheckBox)checkboxLayout.findViewById(R.id.checkbox_donotaskagain);
    	mListener.onDialogCancel(LogConfirmationDialogFragment.this, checkbox.isChecked());
    }
}

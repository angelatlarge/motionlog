package com.atlarge.motionlog;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.SensorManager;


@SuppressWarnings("unused")
public class MainActivity extends Activity  implements OnItemSelectedListener {
	private static final int SENSORUPDATESPEED_NOSELECTION = -1;
    private AccelerometerLoggerService mService;
	private boolean mIsLogging = false;
	private int mSensorUpdateSpeed = SENSORUPDATESPEED_NOSELECTION;
	
	/********************************************************************/
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
	  	@Override
	  	public void onReceive(Context context, Intent intent) {
	  		// Get extra data included in the Intent
			Bundle extras = intent.getExtras();
			// Get the notification flag
			boolean forceNotifyFlag = false;
			if (extras != null) {  
				forceNotifyFlag =  extras.getBoolean(AccelerometerLoggerService.INTENTEXTRA_STATUS_FORCENOTIFYFLAG, false);
			}
			// Process the intent action
	  		if (intent.getAction().equals(AccelerometerLoggerService.ACTION_STATUS_LOGGING)) {
	  			updateButtonUI(true);
				int sensorRate = AccelerometerLoggerService.DEFAULT_SENSOR_RATE;
				if(extras != null) {
					Log.d("MainActivity", "updateUIFromIntent: found extras");		
					sensorRate = extras.getInt(AccelerometerLoggerService.INTENTEXTRA_UPDATERATE, AccelerometerLoggerService.DEFAULT_SENSOR_RATE);
				}
				updateDelayUI(sensorRate);
		  		mIsLogging = true;
				if (forceNotifyFlag)
					Toast.makeText(MainActivity.this, "Logging started", Toast.LENGTH_SHORT).show();	 	    	
	  		} else if (intent.getAction().equals(AccelerometerLoggerService.ACTION_STATUS_NOTLOGGING)) {
	  			updateButtonUI(false);
		  		mIsLogging = false;
				if (forceNotifyFlag)
					Toast.makeText(MainActivity.this, "Logging stopped", Toast.LENGTH_SHORT).show();	 	    	
	  		} else {
	  			// Captured unknown intent
	  		}
//	  		String message = intent.getStringExtra("message");
//	  		Log.d("receiver", "Got message: " + message);
	  	}
	};
	
	/********************************************************************/
	
	public class IconicSpinnerAdapter extends android.widget.BaseAdapter implements SpinnerAdapter {
		private final int stringArrayResourceID;
		private final String[] strings;
		private final TypedArray icons;
//		private final int textViewResourceId;
		private final int viewBasicRowResourceID;
		private final int viewDroppedRowResourceID;
		private final Context context;
		private final int viewRowTextResourceID; 
		private final int viewRowIconResourceID;
		

		public IconicSpinnerAdapter(
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
			Resources res = getResources();
			if (_stringArrayResourceID != 0) {
				// Real resource ID
				strings = res.getStringArray(_stringArrayResourceID);
			} else {
				// Null resource id
				strings = null;
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
			// TODO Auto-generated method stub
			return getIconicView(position, convertView, parent, viewDroppedRowResourceID);
		}	
		
		private View getIconicView(int position, View convertView, ViewGroup parent, int rowResourceID) {
/*			
			LinearLayout layout = new LinearLayout(context);
			layout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
			layout
*/			
			// This is for the regular view
			LayoutInflater inflater=getLayoutInflater();
			View rowView = inflater.inflate(rowResourceID, parent, false);
			if (strings != null) {
				// There is a string array
				TextView label=(TextView)rowView.findViewById(viewRowTextResourceID);
				if (label != null) {
					// Label present
					if (strings.length>position)	// out-of-bounds check
						label.setText(strings[position]);
				}
			}
			
			if (icons != null) {
				ImageView icon=(ImageView)rowView.findViewById(viewRowIconResourceID);
				if (icon != null) {
					// Icon present
					if (icons.length()>position)	// out-of-bounds check
						icon.setImageDrawable(icons.getDrawable(position));
				}
			}

			return rowView;
		}

		@Override
		public int getCount() {
			return strings.length;
		}

		@Override
		public Object getItem(int pos) {
			return strings[pos];
		}

		@Override
		public long getItemId(int pos) {
			return pos;
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		IntentFilter filter = new IntentFilter();
		filter.addAction(AccelerometerLoggerService.ACTION_STATUS_LOGGING);
		filter.addAction(AccelerometerLoggerService.ACTION_STATUS_NOTLOGGING);
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);
		
		// The speed spinner
		{
			Spinner spinnerSpeed = (Spinner) findViewById(R.id.spinner_updatefrequency);
			// Create an ArrayAdapter using the string array and a default spinner layout
			//~ ArrayAdapter<CharSequence> adapter = 
			//~		ArrayAdapter.createFromResource(this, R.array.sensor_update_speeds, android.R.layout.simple_spinner_item);
	//		ArrayAdapter<CharSequence> adapter = new LogSpeedAdapter(this, R.layout.speedspin, R.id.spinner_updatefrequency);
			IconicSpinnerAdapter adapter  = new IconicSpinnerAdapter(
					this, 
					R.array.sensor_update_speed_strings,
					R.array.sensor_update_speed_icons,
					R.layout.iconicspin_icononly,
					R.layout.iconicspin_iconicstring,
					R.id.iconicspin_text, 
					R.id.iconicspin_icon 
					);
			spinnerSpeed.setAdapter(adapter);
			spinnerSpeed.setOnItemSelectedListener(this);
		}
		
		{
			Spinner spinnerCaptureType = (Spinner) findViewById(R.id.spinnerCaptureType);
	        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
	                R.array.capture_type, android.R.layout.simple_spinner_item);
	        // Specify the layout to use when the list of choices appears
	        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	        // Apply the adapter to the spinner
	        spinnerCaptureType.setAdapter(adapter);
	        // Connect the listener
	//        spinner.setOnItemSelectedListener(this);
		
		}		
		// Specify the layout to use when the list of choices appears
		// Apply the adapter to the spinner
		// Connect the listener
		
		// We do not connect to service updating the UI here
		// because we do tat in onResume()
		Log.d("MainActivity", "onCreate");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
		
	@Override
	public void onPause() {
		super.onPause();
		Log.d("MainActivity", "onPause");
		
		if (!mIsLogging)
			stopService();
	}
	
	@Override
	public void onResume() {
		super.onResume();  // Always call the superclass method first
		Log.d("MainActivity", "onResume");
		
		// Connect to our service, which will update the UI
		connectToService();

	}
	
	@Override
	public void onRestart() {
		super.onRestart();  // Always call the superclass method first
		Log.d("MainActivity", "onRestart");
	}
	
	@Override
	public void onStop() {
	    super.onStop();  // Always call the superclass method first
		Log.d("MainActivity", "onStop");
		
		if (!mIsLogging)
			stopService();
	}
	
	
	private void connectToService() {
		Log.d("MainActivity", "Starting the service");
		
		Intent startIntent = new Intent(this, AccelerometerLoggerService.class);
		startIntent.putExtra(AccelerometerLoggerService.INTENTEXTRA_COMMAND, AccelerometerLoggerService.INTENTCOMMAND_RETURNSTATUS);
		ComponentName startResult = startService(startIntent);
		if (startResult==null) {
			Log.e("MainActivity", "Unable to start our service");
		} else {
			Log.d("MainActivity", "Started the service");
		}
	}

	private void stopService() {
		Log.d("MainActivity", "stopService()");
		Intent stopIntent = new Intent(this, AccelerometerLoggerService.class);
		boolean stopResult = stopService(stopIntent);
		if (stopResult) {
			Log.d("MainActivity", "Service stopped");
		} else {
			Log.d("MainActivity", "stopService() returned false");
		}
	}
	
	public void startStopButtonClick(View view) {
		Log.d("MainActivity", "startStopButton clicked");
		if (!mIsLogging) {
			startLogging();
		} else {
			stopLogging();
		}
	}
	
	private void startLogging() {
//		bindService(new Intent(this, AccelerometerLoggerService.class), mConnection, Context.BIND_AUTO_CREATE);
//		mIsBound = true;
		
		int sensorRate;
		if (mSensorUpdateSpeed == SENSORUPDATESPEED_NOSELECTION) {
			sensorRate = AccelerometerLoggerService.DEFAULT_SENSOR_RATE;
		} else if (mSensorUpdateSpeed == 0) {
			sensorRate = SensorManager.SENSOR_DELAY_NORMAL;
		} else if (mSensorUpdateSpeed == 1) {
			sensorRate = SensorManager.SENSOR_DELAY_UI;
		} else if (mSensorUpdateSpeed == 2) {
			sensorRate = SensorManager.SENSOR_DELAY_GAME;
		} else if (mSensorUpdateSpeed == 2) {
			sensorRate = SensorManager.SENSOR_DELAY_FASTEST;
		} else {
			sensorRate = AccelerometerLoggerService.DEFAULT_SENSOR_RATE;
		}
		Log.v("MainActivity", String.format("Using sensor rate: %d\n", sensorRate));
			
		// Communicate with the service via the startService command
		Intent intent = new Intent(this, AccelerometerLoggerService.class);
		intent.putExtra(AccelerometerLoggerService.INTENTEXTRA_COMMAND, AccelerometerLoggerService.INTENTCOMMAND_STARTLOGGING);
		intent.putExtra(AccelerometerLoggerService.INTENTEXTRA_UPDATERATE, sensorRate);
		intent.putExtra(AccelerometerLoggerService.INTENTEXTRA_STATUS_FORCENOTIFYFLAG, true);

		ComponentName startResult = startService(intent);
		if (startResult==null) {
			Log.e("MainActivity", "Unable to issue start logging command via startService");
		} else {
			Log.d("MainActivity", "Issued start logging command successfully");
		}
	}
    
	private void stopLogging() {
		// Communicate with the service via the startService command
		Intent intent = new Intent(this, AccelerometerLoggerService.class);
		intent.putExtra(AccelerometerLoggerService.INTENTEXTRA_COMMAND, AccelerometerLoggerService.INTENTCOMMAND_STOPLOGGING);
		intent.putExtra(AccelerometerLoggerService.INTENTEXTRA_STATUS_FORCENOTIFYFLAG, true);
		ComponentName startResult = startService(intent);
		if (startResult==null) {
			Log.e("MainActivity", "Unable to issue stop logging command via startService");
		} else {
			Log.d("MainActivity", "Issued stop logging command successfully");
		}
	}
	
	@Override
	public void onItemSelected(AdapterView<?> parent, View view,  int pos, long id) {
		// An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
		Log.v("MainActivity", String.format("New mSensorUpdateSpeed: %d\n", pos));
		mSensorUpdateSpeed = pos;
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		mSensorUpdateSpeed = SENSORUPDATESPEED_NOSELECTION;
	}

	private void updateButtonUI(boolean isLogging) {
		Button btn = (Button)findViewById(R.id.button_startstop);
		if (isLogging) {
			btn.setText("Stop");			
		} else {
			btn.setText("Start");			
		}
	}
	
	private void updateDelayUI(int sensorRate) {
		Spinner spinner = (Spinner) findViewById(R.id.spinner_updatefrequency);
	    switch (sensorRate) {
	    case SensorManager.SENSOR_DELAY_NORMAL : spinner.setSelection(0); break;
	    case SensorManager.SENSOR_DELAY_UI : spinner.setSelection(1); break;
	    case SensorManager.SENSOR_DELAY_GAME : spinner.setSelection(2); break;
	    case SensorManager.SENSOR_DELAY_FASTEST : spinner.setSelection(3); break;
	    default: spinner.setSelection(0); break;
	    }
	}
	
	@Override
	protected void onNewIntent (Intent intent) {
		super.onNewIntent(intent);
	}
	


}

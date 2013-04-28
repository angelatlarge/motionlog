package com.atlarge.motionlog;

import android.os.Bundle;
import android.os.IBinder;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


@SuppressWarnings("unused")
public class MainActivity extends Activity  implements OnItemSelectedListener, SensorEventListener, LogConfirmationDialogFragment.DialogListener  {
	private boolean mSingleGraph = false;
	private static final int SENSORUPDATESPEED_NOSELECTION = -1;
    private AccelerometerLoggerService mService;
	private boolean mIsLogging = false;
	private int mSensorUpdateSpeed = SENSORUPDATESPEED_NOSELECTION;
	private GraphViewBase[] mGVs = null;
	private static final boolean LOGCONFIRMATIONPROMPT_DEFAULT = true;
	private boolean mLogConfirmationPrompt = LOGCONFIRMATIONPROMPT_DEFAULT;
	int counter = 0;
	
	/********************************************************************/
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
	  	@Override
	  	public void onReceive(Context context, Intent intent) {
	  		// Get extra data included in the Intent
			Log.d("MainActivity", "onReceive");		
			Bundle extras = intent.getExtras();
			// Get the notification flag
			boolean forceNotifyFlag = false;
			if (extras != null) {  
				forceNotifyFlag =  extras.getBoolean(AccelerometerLoggerService.INTENTEXTRA_STATUS_FORCENOTIFYFLAG, false);
			}
			// Process the intent action
	  		if (intent.getAction().equals(AccelerometerLoggerService.ACTION_STATUS_LOGGING)) {
				Log.d("MainActivity", "onReceive: ACTION_STATUS_LOGGING");		
	  			updateButtonUI(true);
				int sensorRate = AccelerometerLoggerService.DEFAULT_SENSOR_RATE;
				if(extras != null) {
					sensorRate = extras.getInt(AccelerometerLoggerService.INTENTEXTRA_UPDATERATE, AccelerometerLoggerService.DEFAULT_SENSOR_RATE);
				}
				updateDelayUI(sensorRate);
		  		mIsLogging = true;
				if (forceNotifyFlag)
					Toast.makeText(MainActivity.this, "Logging started", Toast.LENGTH_SHORT).show();	 	    	
	  		} else if (intent.getAction().equals(AccelerometerLoggerService.ACTION_STATUS_NOTLOGGING)) {
				Log.d("MainActivity", "onReceive: ACTION_STATUS_NOTLOGGING");		
	  			updateButtonUI(false);
		  		mIsLogging = false;
				if (forceNotifyFlag)
					Toast.makeText(MainActivity.this, "Logging stopped", Toast.LENGTH_SHORT).show();	 	    	
	  		} else if (intent.getAction().equals(AccelerometerLoggerService.ACTION_SENSORCHANGED)) {
				Log.d("MainActivity", "onReceive: ACTION_SENSORCHANGED");		
				if(extras != null) {
					float[] values = extras.getFloatArray(AccelerometerLoggerService.INTENTEXTRA_SENSORVALUES);
					long timestamp = extras.getLong(AccelerometerLoggerService.INTENTEXTRA_SENSORTIMESTAMP, 0);
					processNewSensorValues(values, timestamp);
				}	  			
	  		} else {
	  			// Captured unknown intent
				Log.d("MainActivity", "onReceive: unknown action");		
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
		filter.addAction(AccelerometerLoggerService.ACTION_SENSORCHANGED);
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);
		
		// The speed spinner
		{
			Spinner spinnerSpeed = (Spinner) findViewById(R.id.spinner_updatefrequency);
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
//			spinnerSpeed.setOnItemSelectedListener(this);
		}
		
		{
			Spinner spinnerCaptureType = (Spinner) findViewById(R.id.spinnerCaptureType);
			IconicSpinnerAdapter adapter  = new IconicSpinnerAdapter(
					this, 
					R.array.capturetarget_strings,
					R.array.capturetarget_icons,
					R.layout.iconicspin_icononly,
					R.layout.iconicspin_iconicstring,
					R.id.iconicspin_text, 
					R.id.iconicspin_icon 
					);
			spinnerCaptureType.setAdapter(adapter);
//			
			
//	        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
//	                R.array.capture_type, android.R.layout.simple_spinner_item);
	        // Specify the layout to use when the list of choices appears
//	        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	        // Apply the adapter to the spinner
//	        spinnerCaptureType.setAdapter(adapter);
	        // Connect the listener
	//        spinner.setOnItemSelectedListener(this);
		
		}		
		// Specify the layout to use when the list of choices appears
		// Apply the adapter to the spinner
		// Connect the listener
		
		// We do not connect to service updating the UI here
		// because we do tat in onResume()
		Log.d("MainActivity", "onCreate");
		
		ActionBar actionBar = getActionBar();
		if (actionBar != null)
			actionBar.hide();	
	
		// Read the preferences
		readPreferences();
		
		// Create the GraphicViews
		createGraphViews();

	}

	private void createGraphViews() {
		View toolbar = findViewById(R.id.toolbar);
		LinearLayout layout  = (LinearLayout)toolbar.getParent();
		String[] axisNames = null;
		if (mSingleGraph) {
			mGVs = new GraphViewBitmap[1];
		} else {
			mGVs = new GraphViewBitmap[3];
			axisNames = getResources().getStringArray(R.array.sensor_axis_names);
		}
		int layoutIndex = 1;
		
		// Set the graph view ranges and colors
        SensorManager mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        Sensor mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        float maxRange = mAccelerometer.getMaximumRange();
        Log.d("MainActivity", String.format("Accelerometer maximum range: %f", maxRange));        
		
		DefaultColorIterator dci = new DefaultColorIterator();
		for (int i=0;i<mGVs.length; i++) {
			mGVs[i] = new GraphViewBitmap(this); 
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, mSingleGraph?300:100, (float)0.48);
			lp.gravity = Gravity.BOTTOM;
			mGVs[i].setLayoutParams(lp);
			mGVs[i].setBackgroundColor(0xFF000000);
			if (!mSingleGraph) {
				TextView tv = new TextView(this);
				tv.setTextSize(12);
				tv.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				tv.setText(axisNames[i]);
				layout.addView(tv, layoutIndex++);
				
				mGVs[i].setGraphCount(1);
				mGVs[i].setMaxRange(0, maxRange);
				mGVs[i].setGraphColor(0, dci.getNext());
			} else {
				mGVs[i].setGraphCount(3);
				for (int j=0; j<3; j++) 
					mGVs[i].setMaxRange(j, maxRange);
			}
			layout.addView(mGVs[i], layoutIndex++);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private void readPreferences() {
		SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
		boolean mLogConfirmationPrompt = sharedPref.getBoolean(getString(R.string.saved_logconfirmationprompt), LOGCONFIRMATIONPROMPT_DEFAULT);		
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
		
		if (mIsLogging) {
			stopLogging();
		} else {
			if (mLogConfirmationPrompt) {
			    DialogFragment newFragment = new LogConfirmationDialogFragment();
			    newFragment.show(getFragmentManager(), null);
			} else {
				startLogging();
			}
		}				
	}
	
	@Override
	public void onDialogPositiveClick(DialogFragment dialog, boolean doNotAskAgain) {
		if (doNotAskAgain) {
			mLogConfirmationPrompt = false;
			SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putBoolean(getString(R.string.saved_logconfirmationprompt), false);
			editor.commit();
		}
		
		startLogging();
	}

	@Override
	public void onDialogNegativeClick(DialogFragment dialog, boolean doNotAskAgain) {
		ToggleButton btn = (ToggleButton)findViewById(R.id.button_startstop);
		btn.setChecked(false);
	}

	@Override
	public void onDialogCancel(DialogFragment dialog, boolean doNotAskAgain) {
		ToggleButton btn = (ToggleButton)findViewById(R.id.button_startstop);
		btn.setChecked(false);
	}
	
	private void savePreferences() {
		SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		// TODO: Write this
		editor.commit();
	}
	
	private void startLogging() {
		// Clear the graph views
		if (mSingleGraph) {
			mGVs[0].clear();
		} else {
			DefaultColorIterator dci = new DefaultColorIterator();
			for (int i=0; i<3; i++) { 
				mGVs[i].clear();
			}
		}
		
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
//		Button btn = (Button)findViewById(R.id.button_startstop);
//		if (isLogging) {
//			btn.setText("Stop");			
//		} else {
//			btn.setText("Start");			
//		}
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

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	
	private void processNewSensorValues(float[] values, long timestamp) {
		if (mSingleGraph) {
			for (int i=0; i<values.length; i++) 
				mGVs[0].addReading(i, values[i], timestamp);
		} else {
			for (int i=0; i<values.length; i++) 
				mGVs[i].addReading(0, values[i], timestamp);
		}
		
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<event.values.length; i++) { 
			sb.append(event.values[i]);
			sb.append(" ");
		}
		Log.d("MainActivity", String.format("onSensorChanged, values: %s", sb.toString()));
		processNewSensorValues(event.values, event.timestamp); 
	}


}

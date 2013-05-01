package com.atlarge.motionlog;

import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import java.util.HashMap;


public class MainActivity extends Activity  implements 
			OnItemSelectedListener 
			,SensorEventListener 
			,LogConfirmationDialogFragment.DialogListener
{
	private boolean mSingleGraph = false;
    private AccelerometerLoggerService mService;
	private boolean mIsLogging = false;
	private int mSensorUpdateSpeed = AccelerometerLoggerService.DEFAULT_SENSOR_RATE;
	private int mLogTargetType = AccelerometerLoggerService.LOGTYPE_GRAPH;
	private GraphViewBase[] mGVs = null;
	private TextView[] mGVlabels = null;
	private static final boolean LOGCONFIRMATIONPROMPT_DEFAULT = true;
	private boolean mLogConfirmationPrompt = LOGCONFIRMATIONPROMPT_DEFAULT;
	private Bitmap mFileLoggingBitmap = null;
	private int counter = 0;
	private IconicAdapter mSpeedSpinnerAdapter;
	private IconicAdapter mLogTargetSpinnerAdapter;
	private StringResourceMapper mSpeedSpinnerMapping;
	private StringResourceMapper mLogTargetSpinnerMapping;
	private int mSensorEventsCounter;
	private String mLogFilename = null;
	
	private static final int STATUSSTRINGIDX_FILENAME = 0; 
	private static final int STATUSSTRINGIDX_EVENTS = 1; 
	private static final int STATUSSTRINGIDX_RATETOTAL = 2; 
	private static final int STATUSSTRINGIDX_RATELATEST = 3; 
	private HashMap<Integer,String> mStatsStrings;
	private TextView mStatsTextView;


	/********************************************************************/
/*	
	private class IconicPopupMenu extends PopupMenu {
		IconicPopupMenu(Context context, View anchor) {
			super(context, anchor);
			setForceShowIcon(true); //ADD THIS LINE
		}
	}
*/	
	/********************************************************************/
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		
	  	@Override
	  	public void onReceive(Context context, Intent intent) {
	  		// Get extra data included in the Intent
			//~ Log.d("MainActivity", "onReceive");		
			Bundle extras = intent.getExtras();
			StatusUpdatePacket wassup = null;
			// Get the notification flag
			boolean forceNotifyFlag = false;
			if (extras != null) {  
				forceNotifyFlag =  extras.getBoolean(AccelerometerLoggerService.INTENTEXTRA_STATUS_FORCENOTIFYFLAG, false);
				// Check for status update
				Object entry = extras.get(AccelerometerLoggerService.INTENTEXTRA_STATUSUPDATEPACKET);
				if ( entry != null ) {
					wassup = (StatusUpdatePacket)entry;
				}
			}
			// Process the intent action
	  		if (intent.getAction().equals(AccelerometerLoggerService.ACTION_STATUS_LOGGING)) {
				Log.d("MainActivity", "onReceive: ACTION_STATUS_LOGGING");		
				if(extras != null) {
					mSensorUpdateSpeed = extras.getInt(AccelerometerLoggerService.INTENTEXTRA_UPDATERATE, AccelerometerLoggerService.DEFAULT_SENSOR_RATE);
				}
		  		mIsLogging = true;
				if(extras != null) {
					Object objFilename = extras.get(AccelerometerLoggerService.INTENTEXTRA_LOGFILENAME);
					if (objFilename != null) {
						mLogFilename = (String)objFilename;
					}
				}
				if (forceNotifyFlag)
					Toast.makeText(MainActivity.this, "Logging started", Toast.LENGTH_SHORT).show();	 	    	
				updateUI();
	  		} else if (intent.getAction().equals(AccelerometerLoggerService.ACTION_STATUS_NOTLOGGING)) {
				Log.d("MainActivity", "onReceive: ACTION_STATUS_NOTLOGGING");		
		  		mIsLogging = false;
				if (forceNotifyFlag)
					Toast.makeText(MainActivity.this, "Logging stopped", Toast.LENGTH_SHORT).show();	 	    	
				updateUI();
	  		} else if (intent.getAction().equals(AccelerometerLoggerService.ACTION_SENSORCHANGED)) {
				Log.d("MainActivity", "onReceive: ACTION_SENSORCHANGED");		
				if(extras != null) {
					float[] values = extras.getFloatArray(AccelerometerLoggerService.INTENTEXTRA_SENSORVALUES);
					long timestamp = extras.getLong(AccelerometerLoggerService.INTENTEXTRA_SENSORTIMESTAMP, 0);
					processNewSensorValues(values, timestamp);
				}	  			
	  		} else if (intent.getAction().equals(AccelerometerLoggerService.ACTION_STATISTICS)) {
	  			// Do nothing, we will process the update packet anyway
				//~ Log.d("MainActivity", "onReceive: ACTION_STATISTICS");		
	  		} else {
	  			// Captured unknown intent
				Log.d("MainActivity", "onReceive: unknown action");		
	  		}
	  		
	  		if (wassup!= null) {
	  			updateLoggingStatistics(wassup);
	  		}
	  	}
	};
	
	/********************************************************************/
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("MainActivity", "onCreate");
		setContentView(R.layout.activity_main);
		IntentFilter filter = new IntentFilter();
		filter.addAction(AccelerometerLoggerService.ACTION_STATUS_LOGGING);
		filter.addAction(AccelerometerLoggerService.ACTION_STATUS_NOTLOGGING);
		filter.addAction(AccelerometerLoggerService.ACTION_SENSORCHANGED);
		filter.addAction(AccelerometerLoggerService.ACTION_STATISTICS);
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);
		
		// The speed spinner
		mSpeedSpinnerMapping = 
			new StringResourceMapper(
					new int[] {R.string.menu_SENSORDELAY_NORMAL ,R.string.menu_SENSORDELAY_UI,  R.string.menu_SENSORDELAY_GAME,  R.string.menu_SENSORDELAY_FASTEST},
					new int[] {SensorManager.SENSOR_DELAY_NORMAL,SensorManager.SENSOR_DELAY_UI, SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_FASTEST}
				);
		Spinner spinnerSpeed = (Spinner) findViewById(R.id.spinner_updatefrequency);
		mSpeedSpinnerAdapter  = new IconicAdapter(
				this, 
				R.array.sensor_update_speed_string_ids,
				R.array.sensor_update_speed_icons,
				R.layout.iconicspin_icononly,
				R.layout.iconicspin_iconicstring,
				R.id.iconicspin_text, 
				R.id.iconicspin_icon 
				);
		spinnerSpeed.setAdapter(mSpeedSpinnerAdapter);
		spinnerSpeed.setOnItemSelectedListener(this);
		
		// Capture type spinner
		mLogTargetSpinnerMapping = 
				new StringResourceMapper(
						new int[] {R.string.menu_CAPTURETARGET_GRAPH ,      R.string.menu_CAPTURETARGET_FILE,        R.string.menu_CAPTURETARGET_BOTH},
						new int[] {AccelerometerLoggerService.LOGTYPE_GRAPH,AccelerometerLoggerService.LOGTYPE_FILE, AccelerometerLoggerService.LOGTYPE_BOTH}
					);
		Spinner spinnerCaptureType = (Spinner) findViewById(R.id.spinnerCaptureType);
		mLogTargetSpinnerAdapter  = new IconicAdapter(
				this, 
				R.array.capturetarget_string_ids,
				R.array.capturetarget_icons,
				R.layout.iconicspin_icononly,
				R.layout.iconicspin_iconicstring,
				R.id.iconicspin_text, 
				R.id.iconicspin_icon 
				);
		spinnerCaptureType.setAdapter(mLogTargetSpinnerAdapter);
		spinnerCaptureType.setOnItemSelectedListener(this);
		
		// Specify the layout to use when the list of choices appears
		// Apply the adapter to the spinner
		// Connect the listener
		
		// We do not connect to service updating the UI here
		// because we do tat in onResume()
		
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
			mGVlabels = null;
		} else {
			mGVs = new GraphViewBitmap[3];
			mGVlabels = new TextView[3];
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
				mGVlabels[i] = new TextView(this);
				mGVlabels[i].setTextSize(12);
				mGVlabels[i].setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				mGVlabels[i].setText(axisNames[i]);
				layout.addView(mGVlabels[i], layoutIndex++);
				
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
			if (mLogConfirmationPrompt && ((mLogTargetType & AccelerometerLoggerService.LOGTYPE_FILE) > 0)) {
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
			for (int i=0; i<3; i++) { 
				mGVs[i].clear();
			}
		}
		
		Log.v("MainActivity", String.format("Using sensor rate: %d\n", mSensorUpdateSpeed));
			
		// Communicate with the service via the startService command
		Intent intent = new Intent(this, AccelerometerLoggerService.class);
		intent.putExtra(AccelerometerLoggerService.INTENTEXTRA_COMMAND, AccelerometerLoggerService.INTENTCOMMAND_STARTLOGGING);
		intent.putExtra(AccelerometerLoggerService.INTENTEXTRA_UPDATERATE, mSensorUpdateSpeed);
		intent.putExtra(AccelerometerLoggerService.INTENTEXTRA_LOGGINGTYPE, mLogTargetType);
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
		updateUI();
	}
	
	@Override
	public void onItemSelected(AdapterView<?> parent, View view,  int pos, long id) {
		// An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
		int nItemStringId;
		IconicAdapter adapter = null;
		StringResourceMapper mapper = null;
		
		if (parent.getId() == R.id.spinner_updatefrequency) {
			adapter = mSpeedSpinnerAdapter;
			mapper = mSpeedSpinnerMapping;
			nItemStringId = adapter.getStringId(pos);
			mSensorUpdateSpeed = mapper.toNumericId(nItemStringId);
			Log.v("MainActivity", String.format("New mSensorUpdateSpeed: %s\n", adapter.getPositionalString(pos)));
		} else if (parent.getId() == R.id.spinnerCaptureType) {
			adapter = mLogTargetSpinnerAdapter;
			mapper = mLogTargetSpinnerMapping;
			nItemStringId = adapter.getStringId(pos);
			mLogTargetType = mapper.toNumericId(nItemStringId);
			Log.v("MainActivity", String.format("New mLogTargetType: %s\n", adapter.getPositionalString(pos)));
		} else {
			Log.e("MainActivity", "Unknown source of on item selected"); 
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
	}

	private void setGVvisibility(int visibility) {
		for (int i=0; i<mGVs.length; i++) {
			if (mGVs[i] != null)
				mGVs[i].setVisibility(visibility);
			if (mGVlabels != null)
				if (mGVlabels[i] != null) 
					mGVlabels[i].setVisibility(visibility);
			
		}
	}
	
	private void ensureFileLoggingImageSet() {
		final int IMAGE_SIZE = 100; 
		final float ARROW_WINGTOP 		= 0.6f;
		final float ARROW_MIDDLE	 	= 0.5f;
		final float ARROW_WINGEXT 		= 0.3f;
		final float ARROW_WINGLEFT 		= ARROW_MIDDLE-ARROW_WINGEXT;
		final float ARROW_WINGRIGHT 	= ARROW_MIDDLE+ARROW_WINGEXT;
		final float ARROW_TIPY 			= 0.9f;
		final float ARROW_BODYEXT 		= 0.2f;
		final float ARROW_BODYLEFT 		= ARROW_MIDDLE-ARROW_BODYEXT;
		final float ARROW_BODYRIGHT 	= ARROW_MIDDLE+ARROW_BODYEXT;
		final float ARROW_BODYTOP 		= 0.15f;
		
		if (mFileLoggingBitmap != null) 
			return;
		
		// Initialize the bitmap and canvas
		ImageView iv = (ImageView)findViewById(R.id.logging_to_file_image);
		mFileLoggingBitmap = Bitmap.createBitmap(IMAGE_SIZE, IMAGE_SIZE, Bitmap.Config.ARGB_8888);
		mFileLoggingBitmap.eraseColor(Color.TRANSPARENT);
		Canvas canvas = new Canvas();
		canvas.setBitmap(mFileLoggingBitmap);

		// Draw the circle
		Paint paint = new Paint();
		paint.setARGB (0x20, 0xFF, 0xFF, 0xFF);
		paint.setStyle(Paint.Style.FILL);
		paint.setAntiAlias(true);
		canvas.drawOval(new RectF(0, 0, IMAGE_SIZE-1, IMAGE_SIZE-1), paint);
		
		// Draw the arrow using transparent paint
		paint = new Paint();
		paint.setXfermode(new android.graphics.PorterDuffXfermode(PorterDuff.Mode.CLEAR)); 
		paint.setAntiAlias(true);
		Path path = new Path();
		path.moveTo(IMAGE_SIZE*ARROW_MIDDLE, 	IMAGE_SIZE*ARROW_TIPY);
		path.lineTo(IMAGE_SIZE*ARROW_WINGRIGHT, IMAGE_SIZE*ARROW_WINGTOP); 	// Right wing
		path.lineTo(IMAGE_SIZE*ARROW_BODYRIGHT,	IMAGE_SIZE*ARROW_WINGTOP);	// Right wing armpit
		path.lineTo(IMAGE_SIZE*ARROW_BODYRIGHT,	IMAGE_SIZE*ARROW_BODYTOP);	// Top of the base, right
		path.lineTo(IMAGE_SIZE*ARROW_BODYLEFT,	IMAGE_SIZE*ARROW_BODYTOP);	// Top of the base, left
		path.lineTo(IMAGE_SIZE*ARROW_BODYLEFT,	IMAGE_SIZE*ARROW_WINGTOP);	// Left armpit
		path.lineTo(IMAGE_SIZE*ARROW_WINGLEFT, IMAGE_SIZE*ARROW_WINGTOP);	// Left wing
		path.close();
		canvas.drawPath(path, paint);
		
		// Set the image view image
		iv.setImageBitmap(mFileLoggingBitmap);
		
	}
	
	private void updateUI() {
		Spinner spinner = (Spinner) findViewById(R.id.spinner_updatefrequency);
		if (spinner != null) {
		    switch (mSensorUpdateSpeed) {
			    case SensorManager.SENSOR_DELAY_NORMAL : spinner.setSelection(0); break;
			    case SensorManager.SENSOR_DELAY_UI : spinner.setSelection(1); break;
			    case SensorManager.SENSOR_DELAY_GAME : spinner.setSelection(2); break;
			    case SensorManager.SENSOR_DELAY_FASTEST : spinner.setSelection(3); break;
			    default: spinner.setSelection(0); break;
		    }
			spinner.setEnabled(!mIsLogging);
		}
		
		LinearLayout llf = (LinearLayout)findViewById(R.id.layout_logging_to_file);
		if  (((mLogTargetType & AccelerometerLoggerService.LOGTYPE_GRAPH) > 0 ) | (!mIsLogging)) {
			// Logging to file only
			llf.setVisibility(View.GONE);
			setGVvisibility(View.VISIBLE);
		} else {
			// Logging at least in part to screen
			ensureFileLoggingImageSet();
			setGVvisibility(View.GONE);
			llf.setVisibility(View.VISIBLE);
		}
		
		spinner = (Spinner) findViewById(R.id.spinnerCaptureType);
		if (spinner != null)
			spinner.setEnabled(!mIsLogging);
		
		// Prepare for status updates
		mStatsStrings = null;
		mStatsTextView = null;
		if (mIsLogging) {
			if ((mLogTargetType & AccelerometerLoggerService.LOGTYPE_FILE) > 0 ) {
				// Logging at least in part to file
				mStatsStrings = new HashMap<Integer, String>();
				if (mLogTargetType == AccelerometerLoggerService.LOGTYPE_FILE) {
					// Logging to file only, longer strings
					mStatsStrings.put(STATUSSTRINGIDX_FILENAME, getString(R.string.label_statistics_filename_long_T));
					mStatsStrings.put(STATUSSTRINGIDX_EVENTS, getString(R.string.label_statistics_eventscount_long_T));
					mStatsStrings.put(STATUSSTRINGIDX_RATETOTAL, getString(R.string.label_statistics_totalrate_long_T));
					mStatsStrings.put(STATUSSTRINGIDX_RATELATEST, getString(R.string.label_statistics_latestrate_long_T));
					mStatsTextView = (TextView)findViewById(R.id.logging_to_file_textstatistics);
				} else {
					// Logging to file and screen, short strings					
					mStatsStrings.put(STATUSSTRINGIDX_FILENAME, getString(R.string.label_statistics_filename_short_T));
					mStatsStrings.put(STATUSSTRINGIDX_EVENTS, getString(R.string.label_statistics_eventscount_short_T));
					mStatsStrings.put(STATUSSTRINGIDX_RATETOTAL, getString(R.string.label_statistics_totalrate_short_T));
					mStatsStrings.put(STATUSSTRINGIDX_RATELATEST, getString(R.string.label_statistics_latestrate_short_T));
					mStatsTextView = (TextView)findViewById(R.id.logging_to_file_toptextnotice);
				}
				updateLoggingStatistics(null);			
			}
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d("MainActivity", "onOptionsItemSelected");
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.action_settings:
	    		Log.d("MainActivity", "onOptionsItemSelected: action_settings");
	    		showSettings();
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	private void showSettings() {
    	Intent intent = new Intent(this, SettingsActivity.class);
    	startActivity(intent);
	}		
	
	public void buttonsettings_click(View view) {
		showSettings();
	}

	private void updateLoggingStatistics(StatusUpdatePacket wassup) {
		Log.d("MainActivity", "updateLoggingStatus()");
		StringBuilder sb = new StringBuilder();
		
		if ( (mStatsTextView != null) && (mStatsStrings != null) ) {
			if ((mLogFilename != null) && (mLogFilename.length() > 0) && mStatsStrings.containsKey(STATUSSTRINGIDX_FILENAME)) {
				if (sb.length()>0) sb.append("\n");
				sb.append(String.format(mStatsStrings.get(STATUSSTRINGIDX_FILENAME), mLogFilename));
			}
			if ((wassup!=null) && mStatsStrings.containsKey(STATUSSTRINGIDX_EVENTS)) {
				if (sb.length()>0) sb.append("\n");
				sb.append(String.format(mStatsStrings.get(STATUSSTRINGIDX_EVENTS), wassup.eventsCount()));
			}
			if ((wassup!=null) && mStatsStrings.containsKey(STATUSSTRINGIDX_RATETOTAL)) {
				if (sb.length()>0) sb.append("\n");
				sb.append(String.format(mStatsStrings.get(STATUSSTRINGIDX_RATETOTAL), wassup.totalEventsRate()));
			}
			if ((wassup!=null) && mStatsStrings.containsKey(STATUSSTRINGIDX_RATELATEST)) {
				if (sb.length()>0) sb.append("\n");
				sb.append(String.format(mStatsStrings.get(STATUSSTRINGIDX_RATELATEST), wassup.latestEventsRate()));
			}
			mStatsTextView.setText(sb.toString());
		}
	}

/*	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO: Nothing for now
	}
*/

}
 
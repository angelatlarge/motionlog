package com.atlarge.motionlog;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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
	private boolean mIsLogging = false;
	private int mSensorUpdateSpeed = DataloggerService.DEFAULT_SENSOR_RATE;
	private int mLogTargetType = DataloggerService.LOGTYPE_GRAPH;
	private GraphViewBase[] mGVs = null;
	private TextView[] mGVlabels = null;
	private static final boolean LOGCONFIRMATIONPROMPT_DEFAULT = true;
	private Bitmap mFileLoggingBitmap = null;
	private IconicAdapter mSpeedSpinnerAdapter;
	private IconicAdapter mLogTargetSpinnerAdapter;
	private StringResourceMapper mSpeedSpinnerMapping;
	private StringResourceMapper mLogTargetSpinnerMapping;
	private String mLogFilename = null;
	
	private static final int STATUSSTRINGIDX_FILENAME = 0; 
	private static final int STATUSSTRINGIDX_EVENTS = 1; 
	private static final int STATUSSTRINGIDX_RATETOTAL = 2; 
	private static final int STATUSSTRINGIDX_RATELATEST = 3; 
	private HashMap<Integer,String> mStatsStrings;
	private TextView mStatsTextView;

	
	/********************************************************************/
    /**
     * Class for interacting with the main interface of the service.
     */
	
	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler());

	
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound = false;
    
	
    /** Messenger for communicating with service. */
    Messenger mService = null;
    
    private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.d("MainActivity", "onServiceConnected()");
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			mService = new Messenger(service);

			// We want to monitor the service for as long as we are
			// connected to it.
			try {
				Message msg = Message.obtain(null, DataloggerService.MSG_COMMAND_REGISTERCLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
				
				// Ask the service to update us on what it is doing
				msg = Message.obtain(null, DataloggerService.MSG_COMMAND_GETSTATUS);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				// In this case the service has crashed before we could even
				// do anything with it; we can count on soon being
				// disconnected (and then reconnected if it can be restarted)
				// so there is no need to do anything here.
			}
			
		}
/*		
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
            mBound = true;
			
			// We want to monitor the service for as long as we are
			// connected to it.
			try {
				Message msg = Message.obtain(null,
						DataloggerService.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
				
				// Give it some value as an example.
				msg = Message.obtain(null, DataloggerService.MSG_SET_VALUE, this.hashCode(), 0);
				mService.send(msg);
			} catch (RemoteException e) {
				// In this case the service has crashed before we could even
				// do anything with it; we can count on soon being
				// disconnected (and then reconnected if it can be restarted)
				// so there is no need to do anything here.
			}
			
        }
*/
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mIsBound = false;
        }
    };
	
	/********************************************************************/
	
	/**
	 * Handler of incoming messages from service.
	 */
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Log.d("MainActivity.IncomingHandler", "handleMessage()");
			
			Bundle bundle = msg.getData();
			if (bundle!=null)  
				bundle.setClassLoader(getClassLoader());
			
			switch (msg.what) {
			case DataloggerService.MSG_RESPONSE_STATUS:
				Log.d("MainActivity.IncomingHandler", "handleMessage(): MSG_RESPONSE_STATUS");
				if (bundle==null) {
					Log.d("MainActivity.IncomingHandler", "bundle is null");
				} else {
					DataloggerService.DataloggerStatusParams params = (DataloggerService.DataloggerStatusParams)bundle.getParcelable(DataloggerService.BUNDLEKEY_PARCELLABLE_PARAMS);
					if (params == null) {
						Log.e("MainActivity.IncomingHandler", "params are null");
					} else {
				  		mIsLogging = params.getLogging();
				  		mSensorUpdateSpeed = params.getSensorUpdateDelay();
				  		mLogTargetType = params.getLoggingType();
				  		mLogFilename = params.getFilename();
						updateUI();
						if (params.getStatusChanged()) {
							if (mIsLogging) {
								Toast.makeText(MainActivity.this, "Logging started", Toast.LENGTH_SHORT).show();
							} else {
								Toast.makeText(MainActivity.this, "Logging stopped", Toast.LENGTH_SHORT).show();
							}
						}
					}
				}
				break;
			case DataloggerService.MSG_RESPONSE_STATISTICS:
				// TODO: Do something
				break;
			case DataloggerService.MSG_RESPONSE_SENSOREVENT:
				if (bundle == null) {
					Log.e("MainActivity.IncomingHandler", "Null bundle");
				} else {
					long timespamp = bundle.getLong(DataloggerService.BUNDLEKEY_SENSOREVENT_TIMESTAMP);
					float[] values =  bundle.getFloatArray(DataloggerService.BUNDLEKEY_SENSOREVENT_VALUES);
					if (values == null) {
						Log.e("MainActivity.IncomingHandler", "Null values for BUNDLEKEY_SENSOREVENT_VALUES");
					} else {
						processNewSensorValues(values, timespamp);
					}
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	/********************************************************************/
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("MainActivity", "onCreate");
		setContentView(R.layout.activity_main);
		
		
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
						new int[] {DataloggerService.LOGTYPE_GRAPH,DataloggerService.LOGTYPE_FILE, DataloggerService.LOGTYPE_BOTH}
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
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		// Create the GraphicViews
		createGraphViews();

	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d("MainActivity", "onPause");
		
		if (!mIsLogging)
			disconnectFromService();
	}
	
	@Override
	public void onResume() {
		super.onResume();  // Always call the superclass method first
		Log.d("MainActivity", "onResume");
		
		// Connect to our service, which will update the UI
		connectToService();

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
			disconnectFromService();
	}
	
	
	private void connectToService() {
		// Start the service
		Intent startIntent = new Intent(this, DataloggerService.class);
		ComponentName startResult = startService(startIntent);
		if (startResult==null) {
			Log.e("MainActivity", "Unable to start our service");
		} else {
			Log.d("MainActivity", "Started the service");
		}
		// Bind to the service
       bindService(new Intent(this, DataloggerService.class), mConnection, 0);		
	}

	
	private void disconnectFromService() {
		Log.d("MainActivity", "disconnectFromService()");

		// Unbind
		if (mIsBound) {
			// If we have received the service, and hence registered with
			// it, then now is the time to unregister.
			if (mService != null) {
				try {
					Message msg = Message.obtain(null, DataloggerService.MSG_COMMAND_UNREGISTERCLIENT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					// There is nothing special we need to do if the service
					// has crashed.
				}
			}
			
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
		
		// Stop the service
		Intent stopIntent = new Intent(this, DataloggerService.class);
		boolean stopResult = stopService(stopIntent);
		if (stopResult) {
			Log.d("MainActivity", "Service stopped");
		} else {
			Log.d("MainActivity", "disconnectFromService() returned false");
		}
	}
	
	
	private boolean getUseLogConfirmationPrompt() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		boolean value = sharedPref.getBoolean(getString(R.string.prefkey_logwarn), true);
		Log.d("MainActivity", "useLogConfirmationPrompt returning " + (value?"true":"false"));
		return value;
	}
	
	private void setUseLogConfirmationPrompt(boolean value) {
		Log.d("MainActivity", "setting useLogConfirmationPrompt to " + (value?"true":"false"));
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putBoolean(getString(R.string.prefkey_logwarn), value);
		editor.commit();
	}
	
	public void startStopButtonClick(View view) {
		Log.d("MainActivity", "startStopButton clicked");
		
		if (mIsLogging) {
			stopLogging();
		} else {
			if (
				((mLogTargetType & DataloggerService.LOGTYPE_FILE) > 0)
				&&
				getUseLogConfirmationPrompt()
			){ // Logging to file
				DialogFragment newFragment = new LogConfirmationDialogFragment();
				newFragment.show(getFragmentManager(), null);
			} else {
				startLogging();
			}
		}				
	}
	
	
	
	@Override
	public void onDialogPositiveClick(DialogFragment dialog, boolean doNotAskAgain) {
		Log.d("MainActivity", "onDialogPositiveClick");
		if (doNotAskAgain) {
			Log.d("MainActivity", "doNotAskAgain is true");
			setUseLogConfirmationPrompt(false);
		} else {
			Log.d("MainActivity", "doNotAskAgain is false");
		}
		
		startLogging();
	}

	@Override
	public void onDialogNegativeClick(DialogFragment dialog, boolean doNotAskAgain) {
		Log.d("MainActivity", "onDialogNegativeClick");
		if (doNotAskAgain) {
			Log.d("MainActivity", "doNotAskAgain is true");
			this.setUseLogConfirmationPrompt(false);
		} else {
			Log.d("MainActivity", "doNotAskAgain is false");
		}
		ToggleButton startStopButton = (ToggleButton)findViewById(R.id.button_startstop);
		startStopButton.setChecked(false);
	}

	@Override
	public void onDialogCancel(DialogFragment dialog, boolean doNotAskAgain) {
		ToggleButton btn = (ToggleButton)findViewById(R.id.button_startstop);
		btn.setChecked(false);
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
		
		DataloggerService.DataloggerStartParams params = new DataloggerService.DataloggerStartParams(mSensorUpdateSpeed, mLogTargetType);
    	Bundle bundle = new Bundle();
    	bundle.putParcelable(DataloggerService.BUNDLEKEY_PARCELLABLE_PARAMS, params);
		Message msg = Message.obtain(null, DataloggerService.MSG_COMMAND_STARTLOGGING);
		msg.setData(bundle);
		msg.replyTo = mMessenger;
		try {
			mService.send(msg);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d("MainActivity", "Issued start logging command");
	}
    
	private void stopLogging() {
		// Communicate with the service via the startService command
		Message msg = Message.obtain(null, DataloggerService.MSG_COMMAND_STOPLOGGING);
		msg.replyTo = mMessenger;
		try {
			mService.send(msg);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d("MainActivity", "Issued stop logging command");
//		updateUI();
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
		if  (((mLogTargetType & DataloggerService.LOGTYPE_GRAPH) > 0 ) | (!mIsLogging)) {
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
			if ((mLogTargetType & DataloggerService.LOGTYPE_FILE) > 0 ) {
				// Logging at least in part to file
				mStatsStrings = new HashMap<Integer, String>();
				if (mLogTargetType == DataloggerService.LOGTYPE_FILE) {
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
				updateLoggingStatistics((DataloggerService.DataloggerStatisticsParams)null);			
			}
		}	
		
		// Update the button status
		ToggleButton startStopButton = (ToggleButton)findViewById(R.id.button_startstop);
		startStopButton.setChecked(mIsLogging);
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
		Log.d("MainActivity", "Log prompt is " + (getUseLogConfirmationPrompt()?"true":"false"));
		showSettings();
		Log.d("MainActivity", "Log prompt is " + (getUseLogConfirmationPrompt()?"true":"false"));
	}

	private void updateLoggingStatistics(DataloggerService.DataloggerStatisticsParams params) {
		Log.d("MainActivity", "updateLoggingStatistics()");
		StringBuilder sb = new StringBuilder();
		
		if ( (mStatsTextView != null) && (mStatsStrings != null) ) {
			if ((params!=null) && (mLogFilename != null) && (mLogFilename.length() > 0) && mStatsStrings.containsKey(STATUSSTRINGIDX_FILENAME)) {
				if (sb.length()>0) sb.append("\n");
				sb.append(String.format(mStatsStrings.get(STATUSSTRINGIDX_FILENAME), mLogFilename));
			}
			if ((params!=null) && mStatsStrings.containsKey(STATUSSTRINGIDX_EVENTS)) {
				if (sb.length()>0) sb.append("\n");
				sb.append(String.format(mStatsStrings.get(STATUSSTRINGIDX_EVENTS), params.getEventsCount()));
			}
			if ((params!=null) && mStatsStrings.containsKey(STATUSSTRINGIDX_RATETOTAL)) {
				if (sb.length()>0) sb.append("\n");
				sb.append(String.format(mStatsStrings.get(STATUSSTRINGIDX_RATETOTAL), params.getTotalRate()));
			}
			if ((params!=null) && mStatsStrings.containsKey(STATUSSTRINGIDX_RATELATEST)) {
				if (sb.length()>0) sb.append("\n");
				sb.append(String.format(mStatsStrings.get(STATUSSTRINGIDX_RATELATEST), params.getLatestRate()));
			}
			mStatsTextView.setText(sb.toString());
		}
	}

	private void updateLoggingStatistics(StatusUpdatePacket wassup) {
		Log.d("MainActivity", "updateLoggingStatistics()");
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
 
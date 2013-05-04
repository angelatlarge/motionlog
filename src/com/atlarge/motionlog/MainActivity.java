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
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
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


/*
 * Main (almost the only) activity in the application
 * 
 */
public class MainActivity extends Activity  implements 
			OnItemSelectedListener 
			,SensorEventListener 
			,LogConfirmationDialogFragment.DialogListener
{
	/** Are we displaying a single graph for all axis? */
	private boolean mSingleGraph = false;
	
	/** Are we logging something */
	private boolean mIsLogging = false;
	
	/** Are we logging but not using the service to do so? */
	private boolean mBypassService = false;
	
	/** The rate sensor events are delivered at passed to SensorManager.registerListener */
	private int mSensorUpdateSpeed = DataloggerService.DEFAULT_SENSOR_RATE;
	
	/** Logging to screen, file, or both? */
	private int mLogTargetType = DataloggerService.LOGTYPE_SCREEN;
	
	/** Filename of the logfile. Supplied by our service */
	private String mLogFilename = null;

	/** Default value for the confirmation prompt when logging to file
	 * In theory, preferences.xml has the default in it, which we use; however, 
	 * when using SharedPreferences.get____() a default must be provided nonetheless
	 */
	private static final boolean LOGCONFIRMATIONPROMPT_DEFAULT = true;

	/** Adapter for the spinner used to set mLogTargetType */
	private IconicAdapter mSpeedSpinnerAdapter;
	/** Adapter for the spinner spinner used to set mLogTargetType */	
	private IconicAdapter mLogTargetSpinnerAdapter;
	/** Mapping of spinner ids to numeric ids used in API calls for the mSpeedSpinnerAdapter setting and spinner */
	private StringResourceMapper mSpeedSpinnerMapping;
	/** Mapping of spinner ids to numeric ids used in API calls for the mLogTargetType setting and spinner */
	private StringResourceMapper mLogTargetSpinnerMapping;
	
	/** Image button used to start/stop the logging */
	ImageButton startStopButton = null;
	
	/** GraphView objects used to draw sensor values on screen */
	private GraphViewBase[] mGVs = null;
	/** Labels for the GraphView (axis labels) */
	private TextView[] mGVlabels = null;
	/** Bitmap drawn to illustrate that we are logging to file only */
	private Bitmap mFileLoggingBitmap = null;
	
	/**
	 * We have two types of displaying logging statistics when logging to file
	 * The format of the text displayed changes depending on which view is in use 
	 * We build an array of format strings, which we index via constants
	 *
	 * Constants for indexing the statistics format strings:
	 */
	private static final int STATUSSTRINGIDX_FILENAME = 0; 
	private static final int STATUSSTRINGIDX_EVENTS = 1; 
	private static final int STATUSSTRINGIDX_RATETOTAL = 2; 
	private static final int STATUSSTRINGIDX_RATELATEST = 3; 
	private static final int STATUSSTRINGIDX_COUNT = 4; 
	
	/** Statistics format strings storage */
	private final String[] mStatsStrings = new String[STATUSSTRINGIDX_COUNT];
	
	/** Text view for displaying statistics */
	private TextView mStatsTextView;

	
	private boolean mUseServiceForScreenLogging = false;
	/** Caching the global sensor manager */
    private SensorManager mSensorManager = null;
	/** Caching the accelerometer sensor */
	private Sensor mAccelerometer = null;
	
	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler());

	
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound = false;
    
	
    /** Messenger for communicating with service. */
    Messenger mService = null;
    
	/********************************************************************/
    /**
     * Class for interacting with the main interface of the service.
     */
	
    private ServiceConnection mConnection = new ServiceConnection() {
    	
    	/**
    	 * Executes when service connects to us.
    	 * 
    	 * Here register with the service to be updated 
    	 * (i.e. pass the service our Messenger)
    	 * and ask the service to tell us what it is doing
    	 * (send a MSG_COMMAND_GETSTATUS command)
    	 */
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.d("MainActivity.ServiceConnection", "onServiceConnected()");
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			mService = new Messenger(service);
			mIsBound = true;

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
		
		/**
		 * Gets called when the service disconnects
		 * 
		 * Here we just set some flags to prevent us from trying to disconnect
		 */
        public void onServiceDisconnected(ComponentName className) {
			Log.d("MainActivity.ServiceConnection", "onServiceDisconnected()");
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mIsBound = false;
        }
    };
		
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
			
			// Datalogger service updates on status
			case DataloggerService.MSG_RESPONSE_STATUS:
				Log.d("MainActivity.IncomingHandler", "handleMessage(): MSG_RESPONSE_STATUS");
				if (!mBypassService) {
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
				}
				break;
				
			// Datalogger service is sending logging statistics
			case DataloggerService.MSG_RESPONSE_STATISTICS:
				Log.d("MainActivity.IncomingHandler", "MSG_RESPONSE_STATISTICS");
				if (bundle==null) {
					Log.d("MainActivity.IncomingHandler", "bundle is null");
				} else {
					DataloggerService.DataloggerStatisticsParams params = (DataloggerService.DataloggerStatisticsParams)bundle.getParcelable(DataloggerService.BUNDLEKEY_PARCELLABLE_PARAMS);
					if (params == null) {
						Log.e("MainActivity.IncomingHandler", "Null params for BUNDLEKEY_PARCELLABLE_PARAMS");
					} else {
						updateLoggingStatistics(params);
					}
				}
				break;
				
			// Datalogger service is sending a sensor event 
			//	(we are logging to file+screen or to screen only via the service)
			case DataloggerService.MSG_RESPONSE_SENSOREVENT:
				Log.d("MainActivity.IncomingHandler", "MSG_RESPONSE_SENSOREVENT");
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
				
			// Unknown message
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	/********************************************************************/
	/* Activity lifecycle methods */
	
	/**
	 * Called to initialize the view when the view is created
	 */
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
						new int[] {DataloggerService.LOGTYPE_SCREEN,DataloggerService.LOGTYPE_FILE, DataloggerService.LOGTYPE_BOTH}
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

	/**
	 * Called at startup after OnCreate() or when the app comes back from after Pause()
	 */
	@Override
	public void onResume() {
		super.onResume();  // Always call the superclass method first
		Log.d("MainActivity", "onResume");
		
		// Connect to our service, which will update the UI
		if (mIsLogging && mBypassService) {
			// We were logging without the service
			// so we need to resume that
			startLoggingUsingLocal();
		}
		connectToService();
	}

	/**
	 * Called when app becomes partially obscured or about to be OnStop()ed
	 */
	@Override
	public void onPause() {
		super.onPause();
		Log.d("MainActivity", "onPause");
		
		if (!mIsLogging) {
			disconnectAndStopService();
		} else {
			// We are logging
			if (mBypassService) {
				// Local logging only. Kill the service
				disconnectAndStopService();
				// Stop the logging for now
				stopLoggingUsingLocal();
			} else {
				// Service is logging. Unbind from notifications
				unbindFromService();
			}
		}
	}
	
	/**
	 * Called when activity becomes invisible (obscured by something else)
	 */
	@Override
	public void onStop() {
	    super.onStop();  // Always call the superclass method first
		Log.d("MainActivity", "onStop");
	}
	
	/**
	 * Called after we come back from OnStop()
	 */
	@Override
	public void onRestart() {
		super.onRestart();  // Always call the superclass method first
		Log.d("MainActivity", "onRestart");
	}
	
	
	/********************************************************************/
	/* Intialization helpers */

	/**
	 * Install our "Settings" item into the options menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	

	/**
	 * Create and initialize the GraphViews used by the activity
	 */
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
		if (mSensorManager==null)
			mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		if (mAccelerometer==null)
			mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        float maxRange = mAccelerometer.getMaximumRange();
        //~ Log.d("MainActivity", String.format("Accelerometer maximum range: %f", maxRange));        
		
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
				
				mGVs[i].setSeriesCount(1);
				mGVs[i].setMaxYExtent(0, maxRange);
				mGVs[i].setSeriesColor(0, dci.getNext());
			} else {
				mGVs[i].setSeriesCount(3);
				for (int j=0; j<3; j++) 
					mGVs[i].setMaxYExtent(j, maxRange);
			}
			layout.addView(mGVs[i], layoutIndex++);
		}
	}
	
	/**
	 * This method creates the down arrow graphic used to indicate file logging
	 * 
	 * It is not called during the initialization but only when needed
	 */
	private void ensureFileLoggingImageExists() {
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
	
	
	/********************************************************************/
	/* Service management helpers */
	
	/**
	 * Starts our service and binds to it
	 */
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
		bindToService();
	}

	/**
	 * Binds to our service
	 */
	private void bindToService() {
		// Bind to the service
		Log.d("MainActivity", "bindToService()");
		bindService(new Intent(this, DataloggerService.class), mConnection, 0);		
	}
	
	/**
	 * Disconnects the binding to our service
	 */
	private void unbindFromService() {
		Log.d("MainActivity", "unbindFromService()");
		// Unbind
		if (mIsBound) {
			// If we have received the service, and hence registered with
			// it, then now is the time to unregister.
			if (mService != null) {
				try {
					Log.d("MainActivity", "unbindFromService: sending MSG_COMMAND_UNREGISTERCLIENT");
					Message msg = Message.obtain(null, DataloggerService.MSG_COMMAND_UNREGISTERCLIENT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					// There is nothing special we need to do if the service
					// has crashed.
				}
			}
			
			// Detach our existing connection.
			Log.d("MainActivity", "unbindFromService: calling unbind service");
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	/**
	 * Unbinds from our service and kills it
	 */
	private void disconnectAndStopService() {
		Log.d("MainActivity", "disconnectAndStopService()");

		unbindFromService();
		// Stop the service
		Intent stopIntent = new Intent(this, DataloggerService.class);
		boolean stopResult = stopService(stopIntent);
		if (stopResult) {
			Log.d("MainActivity", "Service stopped");
		} else {
			Log.d("MainActivity", "stopService() returned false");
		}
	}
	
	/********************************************************************
	/* Preferences settings getters and setters 
	 * 
	 * We have one preference currently: whether to warn the user 
	 * that logging to file can be a battery killer
	 */
	
	private boolean getUseLogConfirmationPrompt() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		boolean value = sharedPref.getBoolean(getString(R.string.prefkey_logwarn), LOGCONFIRMATIONPROMPT_DEFAULT);
		return value;
	}
	
	private void setUseLogConfirmationPrompt(boolean value) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putBoolean(getString(R.string.prefkey_logwarn), value);
		editor.commit();
	}

	/********************************************************************
	/* Configmation dialog callbacks 
	 * implementation of LogConfirmationDialogFragment.DialogListener
	 */
	
	@Override
	public void onDialogPositiveClick(DialogFragment dialog, boolean doNotAskAgain) {
		//~ Log.d("MainActivity", "onDialogPositiveClick");
		if (doNotAskAgain) {
			//~ Log.d("MainActivity", "doNotAskAgain is true");
			setUseLogConfirmationPrompt(false);
		} else {
			//~ Log.d("MainActivity", "doNotAskAgain is false");
		}
		
		startLogging();
	}

	@Override
	public void onDialogNegativeClick(DialogFragment dialog, boolean doNotAskAgain) {
		//~ Log.d("MainActivity", "onDialogNegativeClick");
		if (doNotAskAgain) {
			//~ Log.d("MainActivity", "doNotAskAgain is true");
			this.setUseLogConfirmationPrompt(false);
		} else {
			//~ Log.d("MainActivity", "doNotAskAgain is false");
		}
		updateStartStopButtonLooks(false);
	}

	@Override
	public void onDialogCancel(DialogFragment dialog, boolean doNotAskAgain) {
		this.updateStartStopButtonLooks(false);
	}
		
	
	/********************************************************************
	 * Spinner selection callbacks
	 * Implementation of OnItemSelectedListener 
	 */
	
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
			//~ Log.v("MainActivity", String.format("New mSensorUpdateSpeed: %s\n", adapter.getPositionalString(pos)));
		} else if (parent.getId() == R.id.spinnerCaptureType) {
			adapter = mLogTargetSpinnerAdapter;
			mapper = mLogTargetSpinnerMapping;
			nItemStringId = adapter.getStringId(pos);
			mLogTargetType = mapper.toNumericId(nItemStringId);
			//~ Log.v("MainActivity", String.format("New mLogTargetType: %s\n", adapter.getPositionalString(pos)));
		} else {
			Log.e("MainActivity", "Unknown source of on item selected"); 
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// We do not need to do anything here
	}
	
	/********************************************************************
	 * Button click implementation methods
	 */
	
	/**
	 * Settings button click event
	 */
	
	public void buttonsettings_click(View view) {
		//~ Log.d("MainActivity", "Log prompt is " + (getUseLogConfirmationPrompt()?"true":"false"));
		showSettings();
		//~ Log.d("MainActivity", "Log prompt is " + (getUseLogConfirmationPrompt()?"true":"false"));
	}
	
	/**
	 * Start/stop logging button click
	 */
	public void startStopButtonClick(View view) {
		Log.d("MainActivity", "startStopButton clicked");
		
		if (mIsLogging) {
			stopLogging();
		} else {
			if (
				((mLogTargetType & DataloggerService.LOGTYPE_FILE) > 0)
				&&
				getUseLogConfirmationPrompt()
			){ 
				// Logging to file (maybe to screen as well)
				DialogFragment newFragment = new LogConfirmationDialogFragment();
				newFragment.show(getFragmentManager(), null);
			} else {
				// No file logging
				startLogging();
			}
		}				
	}
	
	/********************************************************************
	 * Sensor event callbacks
	 * Implementation of SensorEventListener 
	 */
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Since we don't care about accuracy, we do not need to do anything here
		
	}
	
	/**
	 * Called when a sensor is updated
	 * We call an internal method to process sensor values
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<event.values.length; i++) { 
			sb.append(event.values[i]);
			sb.append(" ");
		}
		//~ Log.d("MainActivity", String.format("onSensorChanged, values: %s", sb.toString()));
		processNewSensorValues(event.values, event.timestamp); 
	}
	
	
	/********************************************************************
	 * Menu callbacks
	 */
	
	/**
	 * Options menu event handler
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//~ Log.d("MainActivity", "onOptionsItemSelected");
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.action_settings:
	    		//~ Log.d("MainActivity", "onOptionsItemSelected: action_settings");
	    		showSettings();
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	/********************************************************************
	 * Second-level UI methods: called by various button and menu events
	 */

	/**
	 * Shows the settings "dialog"
	 */
	private void showSettings() {
    	Intent intent = new Intent(this, SettingsActivity.class);
    	startActivity(intent);
	}		
	
	/********************************************************************
	 * Sensor updating method
	 * 
	 * Created to abstract over how we get the sensor info
	 * and how GraphicViews are set up
	 */
	
	private void processNewSensorValues(float[] values, long timestamp) {
		if (mSingleGraph) {
			for (int i=0; i<values.length; i++) 
				mGVs[0].addValue(i, values[i], timestamp);
		} else {
			for (int i=0; i<values.length; i++) 
				mGVs[i].addValue(0, values[i], timestamp);
		}
	}
	
	
	/********************************************************************
	 * UI updating methods
	 */

	/**
	 * This method updates the looks of the start/stop logging button
	 * Created to abstract over the start/stop button implementation
	 */
	private void updateStartStopButtonLooks(boolean isLogging) {
		if (startStopButton == null)
			startStopButton = (ImageButton)findViewById(R.id.button_startstop);
		startStopButton.setImageDrawable(getResources().getDrawable(isLogging?R.drawable.ic_dialog_playpause_pause:R.drawable.ic_dialog_playpause_play));
//		ToggleButton startStopButton = (ToggleButton)findViewById(R.id.button_startstop);
//		startStopButton.setChecked(isLogging?true:false);
	}
	
	/**
	 * Shows or hides the graph views
	 */
	private void setGVvisibility(int visibility) {
		for (int i=0; i<mGVs.length; i++) {
			if (mGVs[i] != null)
				mGVs[i].setVisibility(visibility);
			if (mGVlabels != null)
				if (mGVlabels[i] != null) 
					mGVlabels[i].setVisibility(visibility);
			
		}
	}
	
	/**
	 * Main UI maintenance method for updating UI as a result of settings changes
	 * and logging activity.
	 * 
	 * It updates the user interface based on whether we are logging or not
	 * and the logging parameters set.
	 * 
	 * Reads mSensorUpdateSpeed, mLogTargetType, and mIsLogging data members 
	 */
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
		if  (((mLogTargetType & DataloggerService.LOGTYPE_SCREEN) > 0 ) | (!mIsLogging)) {
			// Logging to file only
			llf.setVisibility(View.GONE);
			setGVvisibility(View.VISIBLE);
		} else {
			// Logging at least in part to screen
			ensureFileLoggingImageExists();
			setGVvisibility(View.GONE);
			llf.setVisibility(View.VISIBLE);
		}
		
		spinner = (Spinner) findViewById(R.id.spinnerCaptureType);
		if (spinner != null)
			spinner.setEnabled(!mIsLogging);
		
		ImageButton buttonSettings = (ImageButton)findViewById(R.id.button_settings);
		if (buttonSettings != null)
			spinner.setEnabled(!mIsLogging);
		
		// Prepare for status updates
		mStatsTextView = null;
		if (mIsLogging) {
			if ((mLogTargetType & DataloggerService.LOGTYPE_FILE) > 0 ) {
				// Logging at least in part to file
				if (mLogTargetType == DataloggerService.LOGTYPE_FILE) {
					// Logging to file only, longer strings
					mStatsStrings[STATUSSTRINGIDX_FILENAME] = getString(R.string.label_statistics_filename_long_T);
					mStatsStrings[STATUSSTRINGIDX_EVENTS] = getString(R.string.label_statistics_eventscount_long_T);
					mStatsStrings[STATUSSTRINGIDX_RATETOTAL] = getString(R.string.label_statistics_totalrate_long_T);
					mStatsStrings[STATUSSTRINGIDX_RATELATEST] = getString(R.string.label_statistics_latestrate_long_T);
					mStatsTextView = (TextView)findViewById(R.id.logging_to_file_textstatistics);
				} else {
					// Logging to file and screen, short strings					
					mStatsStrings[STATUSSTRINGIDX_FILENAME] = getString(R.string.label_statistics_filename_short_T);
					mStatsStrings[STATUSSTRINGIDX_EVENTS] = getString(R.string.label_statistics_eventscount_short_T);
					mStatsStrings[STATUSSTRINGIDX_RATETOTAL] = getString(R.string.label_statistics_totalrate_short_T);
					mStatsStrings[STATUSSTRINGIDX_RATELATEST] = getString(R.string.label_statistics_latestrate_short_T);
					mStatsTextView = (TextView)findViewById(R.id.logging_to_file_toptextnotice);
				}
				updateLoggingStatistics((DataloggerService.DataloggerStatisticsParams)null);			
			}
		}	
		
		// Update the button status
		updateStartStopButtonLooks(mIsLogging);
	}

	/********************************************************************
	 * Methods for handling statistics data packets 
	 */
	
	/**
	 * Helper method: creates linebreak-separated StringBuilder from strings
	 */
	private void appendLinebreakString(StringBuilder sb, String value) {
		if ((value==null) || (value.length()==0))
			return;
		if (sb.length()>0) sb.append("\n");
		sb.append(value);
	}

	/**
	 * Helper method: formats the string based on format stored in mStatsStrings, appends to StringBuilder
	 */
	private void appendFormatStatisticsString(StringBuilder sb, int formatIndex, Object... arguments) {
		if (mStatsStrings[formatIndex] != null) {
			appendLinebreakString(sb, String.format(mStatsStrings[formatIndex], arguments));
		}
	}
	
	/**
	 * Updates the UI with new logging statistics packet.
	 * 
	 * @param params 	Logging statistics packet in DataloggerService.DataloggerStatisticsParams format
	 */ 
	private void updateLoggingStatistics(DataloggerService.DataloggerStatisticsParams params) {
		Log.d("MainActivity", "updateLoggingStatistics()");
		final StringBuilder sb = new StringBuilder();
		
		
		if ( (mStatsTextView != null) && (mStatsStrings != null) ) {
			if ((mLogFilename != null) && (mLogFilename.length() > 0))
				appendFormatStatisticsString(sb, STATUSSTRINGIDX_FILENAME, mLogFilename);
			if (params!=null) {
				appendFormatStatisticsString(sb, STATUSSTRINGIDX_EVENTS, params.getEventsCount());
				appendFormatStatisticsString(sb, STATUSSTRINGIDX_RATETOTAL, params.getTotalRate());
				appendFormatStatisticsString(sb, STATUSSTRINGIDX_RATELATEST, params.getLatestRate());
			}
			mStatsTextView.setText(sb.toString());
		}
	}

	/**
	 * Updates the UI with new logging statistics packet.
	 * 
	 * @param wassup 	Logging statistics packet in StatusUpdatePacket format
	 */ 
	@SuppressWarnings("unused")
	private void updateLoggingStatistics(StatusUpdatePacket wassup) {
		Log.d("MainActivity", "updateLoggingStatistics()");
		StringBuilder sb = new StringBuilder();
		
		if ( (mStatsTextView != null) && (mStatsStrings != null) ) {
			if ((mLogFilename != null) && (mLogFilename.length() > 0))
				appendFormatStatisticsString(sb, STATUSSTRINGIDX_FILENAME, mLogFilename);
			if (wassup!=null) {
				appendFormatStatisticsString(sb, STATUSSTRINGIDX_EVENTS, wassup.eventsCount());
				appendFormatStatisticsString(sb, STATUSSTRINGIDX_RATETOTAL, wassup.totalEventsRate());
				appendFormatStatisticsString(sb, STATUSSTRINGIDX_RATELATEST, wassup.latestEventsRate());
			}
			mStatsTextView.setText(sb.toString());
		}
	}

	/********************************************************************
	 * Logging command helpers: lower level
	 */
	
	/**
	 * Starts logging without using our logging service
	 */
	private void startLoggingUsingLocal() {
		if (mSensorManager==null)
			mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		if (mAccelerometer==null)
			mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, mAccelerometer, mSensorUpdateSpeed);
	}

	/**
	 * Stops logging done without using our logging service
	 */
	private void stopLoggingUsingLocal() {
		mSensorManager.unregisterListener(this);
	}
	
	/**
	 * Start logging using our logging Service
	 */
	private void startLoggingUsingService() {
		DataloggerService.DataloggerStartParams params = new DataloggerService.DataloggerStartParams(mSensorUpdateSpeed, mLogTargetType);
		Bundle bundle = new Bundle();
		bundle.putParcelable(DataloggerService.BUNDLEKEY_PARCELLABLE_PARAMS, params);
		Message msg = Message.obtain(null, DataloggerService.MSG_COMMAND_STARTLOGGING);
		msg.setData(bundle);
		msg.replyTo = mMessenger;
		mBypassService = false;		
		try {
			mService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		//~ Log.d("MainActivity", "Issued start logging command");
	}
	
	/**
	 * Stop service-based logging
	 */
	private void stopLoggingUsingService() {
		// Communicate with the service via the startService command
		Message msg = Message.obtain(null, DataloggerService.MSG_COMMAND_STOPLOGGING);
		msg.replyTo = mMessenger;
		try {
			mService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		Log.d("MainActivity", "Issued stop logging command");
	}
	
	/********************************************************************
	 * Logging command helpers: high level
	 */
	
	/**
	 * Starts logging of sensor data
	 */
	private void startLogging() {
		Log.d("MainActivity", "stopLogging()");
		
		// Clear the graph views
		if (mSingleGraph) {
			mGVs[0].clear();
		} else {
			for (int i=0; i<3; i++) { 
				mGVs[i].clear();
			}
		}
		
		if (mUseServiceForScreenLogging || (mLogTargetType!=DataloggerService.LOGTYPE_SCREEN)) {
			startLoggingUsingService();
			// We don't need to update the UI here 
			// because we will update the UI when the service tells us 
			// that it started logging 
		} else {
			// Screen-only logging without the service
			Log.d("MainActivity", String.format("Screen-only logging with speed %d", mSensorUpdateSpeed));
			startLoggingUsingLocal();
			// Need to update the UI because there are no callbacks for starting logging, 
			// unlike in the case of using the service
			mIsLogging = true;
			mBypassService = true;
			updateUI();
		}
	}
    
	/**
	 * Stops logging of sensor data
	 */
	private void stopLogging() {
		Log.d("MainActivity", "stopLogging()");
		if (!mBypassService) {
			stopLoggingUsingService();
			// Like in the startLogging() case, 
			// the UI will be update on callback.
		} else {
			// Screen-only logging without the service
			stopLoggingUsingLocal();
			// Need to update the UI since no callbacks exist
			mIsLogging = false;
			mBypassService = false;
			updateUI();
		}
	}
	

}
 
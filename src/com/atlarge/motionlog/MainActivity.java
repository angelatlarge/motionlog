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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import android.hardware.SensorManager;


@SuppressWarnings("unused")
public class MainActivity extends Activity  implements OnItemSelectedListener {
	private static final int SENSORUPDATESPEED_NOSELECTION = -1;
    private AccelerometerLoggerService mService;
	private boolean mIsLogging = false;
	private int mSensorUpdateSpeed = SENSORUPDATESPEED_NOSELECTION;
	
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
	  	@Override
	  	public void onReceive(Context context, Intent intent) {
	  		// Get extra data included in the Intent
	  		if (intent.getAction().equals(AccelerometerLoggerService.ACTION_STATUS_LOGGING)) {
	  			updateButtonUI(true);
				Bundle extras = intent.getExtras();
				int sensorRate = AccelerometerLoggerService.DEFAULT_SENSOR_RATE;
				if(extras != null) {
					Log.d("MainActivity", "updateUIFromIntent: found extras");		
					sensorRate = extras.getInt(AccelerometerLoggerService.INTENTEXTRA_UPDATERATE, AccelerometerLoggerService.DEFAULT_SENSOR_RATE);
				}
				updateDelayUI(sensorRate);
		  		mIsLogging = true;
//		  		Toast.makeText(MainActivity.this, "Logging started", Toast.LENGTH_SHORT).show();	 	    	
	  		} else if (intent.getAction().equals(AccelerometerLoggerService.ACTION_STATUS_NOTLOGGING)) {
	  			updateButtonUI(false);
		  		mIsLogging = false;
//		  		Toast.makeText(MainActivity.this, "Logging stopped", Toast.LENGTH_SHORT).show();	 	    	
	  		} else {
	  			// Captured unknown intent
	  		}
//	  		String message = intent.getStringExtra("message");
//	  		Log.d("receiver", "Got message: " + message);
	  	}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		IntentFilter filter = new IntentFilter();
		filter.addAction(AccelerometerLoggerService.ACTION_STATUS_LOGGING);
		filter.addAction(AccelerometerLoggerService.ACTION_STATUS_NOTLOGGING);
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);
		
		// Populate the spinner
		Spinner spinner = (Spinner) findViewById(R.id.spinner_updatefrequency);
		// Create an ArrayAdapter using the string array and a default spinner layout
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
		        R.array.sensor_update_speeds, android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);
				
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

		// Communicate with the service via the startService command
		Intent intent = new Intent(this, AccelerometerLoggerService.class);
		intent.putExtra(AccelerometerLoggerService.INTENTEXTRA_COMMAND, AccelerometerLoggerService.INTENTCOMMAND_STARTLOGGING);
		intent.putExtra(AccelerometerLoggerService.INTENTEXTRA_UPDATERATE, sensorRate);
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

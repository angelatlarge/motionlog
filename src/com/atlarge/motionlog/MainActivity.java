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
	private boolean mIsLogging = false;
	private int mSensorUpdateSpeed = SENSORUPDATESPEED_NOSELECTION;
//	private boolean mIsBound = false;
//	private AccelerometerLoggerService mBoundService;
	
	
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
	  	@Override
	  	public void onReceive(Context context, Intent intent) {
	  		// Get extra data included in the Intent
	  		if (intent.getAction().equals(AccelerometerLoggerService.ACTION_LOGSTARTED)) {
		  		updateUI(true);
		  		mIsLogging = true;
		  		Toast.makeText(MainActivity.this, "log stated", Toast.LENGTH_SHORT).show();	 	    	
	  		} else if (intent.getAction().equals(AccelerometerLoggerService.ACTION_LOGSTOPPED)) {
		  		updateUI(false);
		  		mIsLogging = false;
		  		Toast.makeText(MainActivity.this, "log stopped", Toast.LENGTH_SHORT).show();	 	    	
	  		} else {
	  			// Captured unknown intent
	  		}
//	  		String message = intent.getStringExtra("message");
//	  		Log.d("receiver", "Got message: " + message);
	  	}
	};

/*		
	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  Because we have bound to a explicit
	        // service that we know is running in our own process, we can
	        // cast its IBinder to a concrete class and directly access it.
	        // Tell the user about this for our demo.
	        Toast.makeText(MainActivity.this, "connected", Toast.LENGTH_SHORT).show();	 	    	
	        mBoundService = ((AccelerometerLoggerService.LocalBinder)service).getService();
	        mIsLogging = true;
	        updateUI(mIsLogging);
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        // Because it is running in our same process, we should never
	        // see this happen.
	        Toast.makeText(MainActivity.this, "disconnected", Toast.LENGTH_SHORT).show();	 	    	
	        mBoundService = null;
	        mIsLogging = false;
	        updateUI(mIsLogging);
	    }
	};	
*/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		IntentFilter filter = new IntentFilter();
		filter.addAction(AccelerometerLoggerService.ACTION_LOGSTARTED);
		filter.addAction(AccelerometerLoggerService.ACTION_LOGSTOPPED);
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
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
		
	public void startStopButtonClick(View view) {
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
			sensorRate = SensorManager.SENSOR_DELAY_NORMAL;
		} else if (mSensorUpdateSpeed == 0) {
			sensorRate = SensorManager.SENSOR_DELAY_UI;
		} else if (mSensorUpdateSpeed == 1) {
			sensorRate = SensorManager.SENSOR_DELAY_GAME;
		} else if (mSensorUpdateSpeed == 0) {
			sensorRate = SensorManager.SENSOR_DELAY_FASTEST;
		} else {
			sensorRate = SensorManager.SENSOR_DELAY_NORMAL;
		}
					
		Intent intent = new Intent(this, AccelerometerLoggerService.class);
		intent.putExtra(AccelerometerLoggerService.INTENTEXTRA_UPDATERATE, sensorRate);
		startService(intent);
		
	}
    
	private void stopLogging() {
//	    if (mIsBound) {
//	        // Detach our existing connection.
//	        Toast.makeText(MainActivity.this, "unbinding", Toast.LENGTH_SHORT).show();	 	    	
//	        unbindService(mConnection);
//	        mIsBound = false;
//	    }
		Intent intent = new Intent(this, AccelerometerLoggerService.class);
		stopService(intent);		
	}
	
	private void updateUI(boolean isLogging) {
		Button btn = (Button)findViewById(R.id.button_startstop);
		if (isLogging) {
			btn.setText("Stop");			
		} else {
			btn.setText("Start");			
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
    


}

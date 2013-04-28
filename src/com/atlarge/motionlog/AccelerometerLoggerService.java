package com.atlarge.motionlog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

@SuppressWarnings("unused")
public class AccelerometerLoggerService extends Service implements SensorEventListener {
	public static final int LOGTYPE_FILE = 1;
	public static final int LOGTYPE_GRAPH = 2;
	public static final int LOGTYPE_BOTH = 3;
	
	public static final String ACTION_STATUS_LOGGING = "com.atlarge.motionlog.status.logging";
	public static final String ACTION_STATUS_NOTLOGGING = "com.atlarge.motionlog.status.notlogging";
	public static final String ACTION_SENSORCHANGED = "com.atlarge.motionlog.sensorchanged";
	public static final String APPLICATION_DIR = "com.atlarge.motionlog";
	public static final String INTENTEXTRA_COMMAND = "com.atlarge.servicecommand";
	public static final int INTENTCOMMAND_RETURNSTATUS	= 0x01;
	public static final int INTENTCOMMAND_STARTLOGGING	= 0x02;
	public static final int INTENTCOMMAND_STOPLOGGING	= 0x03;
	public static final String INTENTEXTRA_UPDATERATE = "com.atlarge.updaterate";
	public static final String INTENTEXTRA_LOGGINGTYPE = "com.atlarge.loggingtype";
	public static final String INTENTEXTRA_SENSORVALUES = "com.atlarge.sensorvalues";
	public static final String INTENTEXTRA_SENSORTIMESTAMP = "com.atlarge.sensortimestamp";
//	public static final String INTENTEXTRA_SENSOREVENT = "com.atlarge.sensorevent";
	public static final String INTENTEXTRA_STATUS_FORCENOTIFYFLAG = "com.atlarge.status.forcenotifyflag";
	public static final int DEFAULT_SENSOR_RATE = SensorManager.SENSOR_DELAY_NORMAL;
	public static final int DEFAULT_LOGTYPE = LOGTYPE_GRAPH;
	
	private static final int NOTIFICATIONID_INPROGRESS = 001;
	private static final int HANDLERTHREAD_PRIORITY = android.os.Process.THREAD_PRIORITY_DEFAULT;
	
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	private boolean logging = false;
    private File logFile;
	private FileOutputStream logOutputStream;
	private PrintWriter logWriter;
	private int mSensorRate = DEFAULT_SENSOR_RATE;
	private int mLoggingType = DEFAULT_LOGTYPE;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
	
	// Handler that receives messages from the thread
	@SuppressLint("HandlerLeak")
	private final class ServiceHandler extends Handler {
		private static final int MSG_STARTLOGGING = 0x01;
		private static final int MSG_STOPLOGGING = 0x02; 
		private static final int MSG_GETSTATUS = 0x03;
		
		public ServiceHandler(Looper looper) {
			super(looper);
		}
		
		@Override
		public void handleMessage(Message msg) {
			switch (msg.arg1) {
				case MSG_STARTLOGGING: 
					Log.d("AccelerometerLoggerService", "MSG_STARTLOGGING command received");
					break;
				case MSG_STOPLOGGING: 
					Log.d("AccelerometerLoggerService", "MSG_STOPLOGGING command received");
					break;
				case MSG_GETSTATUS: 
					Log.d("AccelerometerLoggerService", "MSG_GETSTATUS command received");
					break;
				default: 
					break;
			}			
		}
	}

	@Override
	public void onCreate() {
		Log.d("AccelerometerLoggerService", "onCreate()");
		// Start up the thread running the service.  Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block.  We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		HandlerThread thread = new HandlerThread("ServiceStartArguments", HANDLERTHREAD_PRIORITY);
		thread.start();

		// Get the HandlerThread's Looper and use it for our Handler 
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("AccelerometerLoggerService", "onStartCommand()");
//		Toast.makeText(this, "Service starting", Toast.LENGTH_SHORT).show();

		// Need to parse the intent for command
		boolean forceNotifyFlag = false;
	    Bundle extras = intent.getExtras();
	    if(extras != null) {
	    	int startCommand = extras.getInt(INTENTEXTRA_COMMAND, INTENTCOMMAND_RETURNSTATUS);
			switch (startCommand) {
			case INTENTCOMMAND_RETURNSTATUS:
				// We don't process this command since we retun status every time anyway
				Log.d("AccelerometerLoggerService", "INTENTCOMMAND_RETURNSTATUS command received");
				break;
			case INTENTCOMMAND_STARTLOGGING:
				Log.d("AccelerometerLoggerService", "INTENTCOMMAND_STARTLOGGING command received");
				// Pull out the update rate from the intent
				Log.d("AccelerometerLoggerService", String.format("Old sensor rate: %d, ", mSensorRate));
                mSensorRate = extras.getInt(INTENTEXTRA_UPDATERATE, DEFAULT_SENSOR_RATE);
				Log.d("AccelerometerLoggerService", String.format("new sensor rate: %d\n", mSensorRate));
				mLoggingType = extras.getInt(INTENTEXTRA_LOGGINGTYPE, DEFAULT_LOGTYPE);
				startLogging();
				break;
			case INTENTCOMMAND_STOPLOGGING:
				Log.d("AccelerometerLoggerService", "INTENTCOMMAND_STOPLOGGING command received");
				stopLogging();
				break;
			}
			// Check the new notify flag
			forceNotifyFlag =  extras.getBoolean(INTENTEXTRA_STATUS_FORCENOTIFYFLAG, false);
	    }

		performStatusUpdate(forceNotifyFlag);
		
		// If we get killed, after returning from here, restart
		return START_STICKY;
	}

	// We are not using a bound service
    @Override
    public IBinder onBind(Intent intent) {
		Log.d("AccelerometerLoggerService", "onBind()");
        return null;
    }
  
	@Override
	public void onDestroy() {
		Log.d("AccelerometerLoggerService", "onDestroy()");
//		Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show(); 
	}
	
    private boolean startLogging() {
		Log.d("AccelerometerLoggerService", "startLogging()");
				
		if ((mLoggingType & LOGTYPE_FILE) > 0) {
			// Logging to file as well
		    String state = Environment.getExternalStorageState();
		    if (!Environment.MEDIA_MOUNTED.equals(state)) {
				Toast.makeText(this, "External storage unavailable: can't start log", Toast.LENGTH_SHORT).show();
		        stopSelf();
		        return false;
		    } // else: we know the external storage is available
		    
	        // Create a directory
		    File logDir = new File(Environment.getExternalStorageDirectory(), APPLICATION_DIR);
		    if (!logDir.exists()) {
		        if (!logDir.mkdirs()) {
		        	// Trouble creating directory
					Toast.makeText(this, "Cannot create app directory", Toast.LENGTH_SHORT).show();
			        return false;
		        }
		    }

		    // Create a file
		    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HH-mm-ss", Locale.US);
		    Date now = new Date();
		    String logFN = formatter.format(now) + ".txt";
	        logFile = new File(logDir.getAbsolutePath(), logFN);
	        try {
	        	logOutputStream = new FileOutputStream(logFile);
		        logWriter = new PrintWriter(logOutputStream);
		        logWriter.println("Time (ns)\tX-axis\tY-axis\tZ-axis");
	        } catch (Exception e) {
	            e.printStackTrace();
				return false;
	        }    
		} // Logging to file
		
		// Here we know logging is going to start
		logging = true;
		
        // Create a notification
        notificationStart();
        
		// Register for sensor events
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, mSensorRate, mServiceHandler);

		return true;
    }

    private void stopLogging() {
		//* TODO: Would like to close the files when logging stops, 
		//			although as currently written, new file will be created every time the logging starts
    	if (mSensorManager != null) {
			mSensorManager.unregisterListener(this);
    	}
		notificationEnd();
		logging = false;
		// Updating activities performStatusUpdate() will be called elsewhere
    }
    
	private void performStatusUpdate(boolean forceNotificationFlag) {
		// Broadcast notification
		Intent intent = new Intent();
		if (logging) {
			intent.setAction(ACTION_STATUS_LOGGING);
			intent.putExtra(INTENTEXTRA_UPDATERATE, mSensorRate);
		} else {
			intent.setAction(ACTION_STATUS_NOTLOGGING);
		}		
		if (forceNotificationFlag) {
			intent.putExtra(INTENTEXTRA_STATUS_FORCENOTIFYFLAG, true);
		}
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		Log.v("AccelerometerLoggerService", "onSensorChanged");
        try {
    		if ((mLoggingType & LOGTYPE_FILE) > 0) {
    			Log.v("AccelerometerLoggerService", "saving to file");
	        	logWriter.print(event.timestamp);
	        	for (int i=0;i<event.values.length;i++)
	        		logWriter.format("\t%f", event.values[i]);
	        	logWriter.println();
    		}
    		if ((mLoggingType & LOGTYPE_GRAPH) > 0) {
        		// Notify the parent window
    			Log.v("AccelerometerLoggerService", "notifying parent");
        		Intent intent = new Intent();
        		intent.setAction(ACTION_SENSORCHANGED);
        		intent.putExtra(INTENTEXTRA_SENSORVALUES, event.values);
        		intent.putExtra(INTENTEXTRA_SENSORTIMESTAMP, event.timestamp);
        		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);        		
        	}
        } catch (Exception e) {
            e.printStackTrace();
        }    
	}
	
	private void notificationStart() {
		NotificationCompat.Builder mBuilder =
			    new NotificationCompat.Builder(this)
			    .setSmallIcon(R.drawable.ic_stat_notify_logging)
			    .setContentTitle("Motionlog logging")
//			    .setContentTitle("")
			    .setContentText("select to adjust")
			    .setOngoing (true);
		
		Intent resultIntent = new Intent(this, MainActivity.class);
		resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		// Because clicking the notification opens a new ("special") activity, there's
		// no need to create an artificial back stack.
		PendingIntent returnPendingIntent =
		    PendingIntent.getActivity(
		    this,
		    0,
		    resultIntent,
		    PendingIntent.FLAG_UPDATE_CURRENT
		);		
		
		mBuilder.setContentIntent(returnPendingIntent);
		
		// Sets an ID for the notification
		int mNotificationId = NOTIFICATIONID_INPROGRESS;
		// Gets an instance of the NotificationManager service
		NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// Builds the notification and issues it.
		mNotifyMgr.notify(mNotificationId, mBuilder.build());
		
	}
	
	private void notificationEnd() {
		NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotifyMgr.cancel(NOTIFICATIONID_INPROGRESS);
	}
	
}

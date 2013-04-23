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
import android.widget.Toast;

@SuppressWarnings("unused")
public class AccelerometerLoggerService extends Service implements SensorEventListener {
	public static final String ACTION_LOGSTARTED = "com.atlarge.motionlog.logstarted";
	public static final String ACTION_LOGSTOPPED = "com.atlarge.motionlog.logstopped";
	public static final String APPLICATION_DIR = "com.atlarge.motionlog";
	public static final String INTENTEXTRA_UPDATERATE = "com.atlarge.updaterate";
	public static final String INTENTEXTRA_SERVICERUNNING = "com.atlarge.servicerunning";
	private static final int NOTIFICATIONID_INPROGRESS = 001;
	public static final int DEFAULT_SENSOR_RATE = SensorManager.SENSOR_DELAY_NORMAL;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private File logFile;
	private FileOutputStream logOutputStream;
	private PrintWriter logWriter;
	private int mSensorRate = DEFAULT_SENSOR_RATE;

/*	
    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.local_service_started;
*/	
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
//    public class LocalBinder extends Binder {
//    	AccelerometerLoggerService getService() {
//            return AccelerometerLoggerService.this;
//        }
//    }
    
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
//    private final IBinder mBinder = new LocalBinder();
//    
/*    
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;

	// Handler that receives messages from the thread
	@SuppressLint("HandlerLeak")
	private final class ServiceHandler extends Handler {
		@SuppressLint("HandlerLeak")
		public ServiceHandler(Looper looper) {
			super(looper);
		}
		
		@Override
		public void handleMessage(Message msg) {
			// Normally we would do some work here, like download a file.
			// For our sample, we just sleep for 5 seconds.
			long endTime = System.currentTimeMillis() + 5*1000;
			while (System.currentTimeMillis() < endTime) {
				synchronized (this) {
					try {
						wait(endTime - System.currentTimeMillis());
					} catch (Exception e) {
					}
				}
			}
			// Stop the service using the startId, so that we don't stop
			// the service in the middle of handling another job
			stopSelf(msg.arg1);
		}
	}
*/
	@Override
	public void onCreate() {
//		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
		// Start up the thread running the service.  Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block.  We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		
//		HandlerThread thread = new HandlerThread("ServiceStartArguments",
//			 	android.os.Process.THREAD_PRIORITY_DEFAULT);
//		thread.start();
		
/*		
		// Get the HandlerThread's Looper and use it for our Handler 
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
*/		
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    Bundle extras = intent.getExtras();
	    if(extras != null) {
	    	mSensorRate = extras.getInt(INTENTEXTRA_UPDATERATE, DEFAULT_SENSOR_RATE);
	    }
				
		if (startLogging()) {
			return START_STICKY;
		} else {
			return 0;
		}
		// If we get killed, after returning from here, restart
		

/*		
		// For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the job
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		mServiceHandler.sendMessage(msg);
*/		
		
	}

    @Override
    public IBinder onBind(Intent intent) {
//        return mBinder;
    	return null;
    }
		  
    private boolean startLogging() {
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
		        stopSelf();
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
        }    
        
        // Create a notification
        startNotification();
        
		// Register for sensor events
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, mSensorRate);

        // Toast notification
//		Toast.makeText(this, String.format("Starting logging at rate %d", mSensorRate), Toast.LENGTH_SHORT).show();
		
		// Broadcast notification
		Intent i = new Intent();
        i.setAction(ACTION_LOGSTARTED);
		LocalBroadcastManager.getInstance(this).sendBroadcast(i);
		
		return true;
    }
    private void stopLogging() {
    	if (mSensorManager != null) {
			mSensorManager.unregisterListener(this);
	/*		
			try {
				synchronized (this) {
					wait(250); 
	            }
				logWriter.close();
				logWriter = null;
				logOutputStream.close();
				logOutputStream = null;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	*/		
//			Toast.makeText(this, "Logging stopped", Toast.LENGTH_SHORT).show();
    	}
		Intent i = new Intent();
        i.setAction(ACTION_LOGSTOPPED);
		LocalBroadcastManager.getInstance(this).sendBroadcast(i);
		stopNotification();
    }
    
	@Override
	public void onDestroy() {
		stopLogging();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
        try {
        	logWriter.print(event.timestamp);
        	for (int i=0;i<event.values.length;i++)
        		logWriter.format("\t%f", event.values[i]);
        	logWriter.println();
        } catch (Exception e) {
            e.printStackTrace();
        }    
	}
	
	private void startNotification() {
		NotificationCompat.Builder mBuilder =
			    new NotificationCompat.Builder(this)
			    .setSmallIcon(R.drawable.ic_stat_notify_logging)
			    .setContentTitle("Motionlog logging")
//			    .setContentTitle("")
			    .setContentText("select to adjust")
			    .setOngoing (true);
		
		Intent resultIntent = new Intent(this, MainActivity.class);
		resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		resultIntent.putExtra(INTENTEXTRA_UPDATERATE, mSensorRate);
		resultIntent.putExtra(INTENTEXTRA_SERVICERUNNING, true);
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
	
	private void stopNotification() {
		NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotifyMgr.cancel(NOTIFICATIONID_INPROGRESS);
	}

}

/*
		Context context = getApplicationContext();
		CharSequence text = "Logger service starting";
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(context, text, duration);
		toast.show();		
		
		// Create an output file
		try {
	    File file = new File(Environment.getExternalStoragePublicDirectory(
	            Environment.DIRECTORY_DOWNLOADS), "Motionlog.txt");
		FileOutputStream outputStream = new FileOutputStream(file);
        PrintWriter writer = new PrintWriter(outputStream);
        writer.println("Hi , How are you");
        writer.println("Hello");
        
        
        	writer.flush();
        	writer.close();
	        outputStream.close();		
		} catch (Exception e) {
		  e.printStackTrace();
		}		
		
		// Read accelerometer data and write it to file 
		
*/
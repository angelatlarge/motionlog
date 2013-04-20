package com.atlarge.motionlog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

public class AccelerometerLoggerService extends Service {
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;

	// Handler that receives messages from the thread
	private final class ServiceHandler extends Handler {
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

	@Override
	public void onCreate() {
		// Start up the thread running the service.  Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block.  We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		HandlerThread thread = new HandlerThread("ServiceStartArguments",
			Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		// Get the HandlerThread's Looper and use it for our Handler 
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

		// For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the job
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		mServiceHandler.sendMessage(msg);

		// If we get killed, after returning from here, restart
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// We don't provide binding, so return null
		return null;
	}
		  
	@Override
	public void onDestroy() {
		Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show(); 
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
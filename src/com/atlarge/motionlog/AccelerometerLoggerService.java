package com.atlarge.motionlog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;

public class AccelerometerLoggerService extends IntentService {
	
public AccelerometerLoggerService(String name) {
		super(name);
		
		// TODO Auto-generated constructor stub
	}

	/*	
	// Constructor
	public AccelerometerLoggerService() {
	}

	public int onStartCommand (Intent intent, int flags, int startId) {
		handleCommand(intent);
		return  START_STICKY;
	}
	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}
*/
	@Override
	protected void onHandleIntent(Intent intent) {
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
		
		
	}
	
	public void onDestroy () {
		//TODO: Write this
	}
}

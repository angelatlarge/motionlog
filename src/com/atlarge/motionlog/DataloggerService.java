package com.atlarge.motionlog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class DataloggerService extends Service implements SensorEventListener {
	public static final int LOGTYPE_GRAPH = 1;
	public static final int LOGTYPE_FILE = 2;
	public static final int LOGTYPE_BOTH = 3;
	
	public static final String APPLICATION_DIRECTORY = "com.atlarge.motionlog";
//	public static final String INTENTEXTRA_SENSOREVENT = "com.atlarge.sensorevent";
	public static final String INTENTEXTRA_STATUS_FORCENOTIFYFLAG = "com.atlarge.status.forcenotifyflag";
	public static final int DEFAULT_SENSOR_RATE = SensorManager.SENSOR_DELAY_NORMAL;
	public static final int DEFAULT_LOGTYPE = LOGTYPE_GRAPH;
	
	private static final int NOTIFICATIONID_INPROGRESS = 001;
	private static final int HANDLERTHREAD_PRIORITY = android.os.Process.THREAD_PRIORITY_DEFAULT;
	private static final int UPDATE_STATISTICS_EVERY_INITIAL = 2;
	
//	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	private boolean logging = false;
    private File logFile;
	private FileOutputStream logOutputStream;
	private PrintWriter logWriter;				// Using this as a flag for whether to log to file
	private int mSensorRate = DEFAULT_SENSOR_RATE;
	private int mLoggingType = DEFAULT_LOGTYPE;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private String mLogFilename = null;;
    private int mTotalSensorEventsCount;		// Number of sensor events this run
    private long mFirstTotalEventTimestamp;		// Timestamp of the first event
    private long mFirstLatestEventTimestamp;	// Timestamp of the since activity has been updated
    
    private int mUpdateStatisticsRatio = UPDATE_STATISTICS_EVERY_INITIAL;

    public static final String BUNDLE_PARCELLABLE_PARAMS = "com.atlarge.motionlog.BUNDLE_PARCELLABLE_PARAMS";
    
    /** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();

    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    public static final int MSG_COMMAND_REGISTERCLIENT = 0x01;
    
    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    public static final int MSG_COMMAND_UNREGISTERCLIENT = 0x02;

    /**
     * Command to the service to start logging
     */
    public static final int MSG_COMMAND_STARTLOGGING = 0x03;
	
    /**
     * Command to the service to stop logging
     */
	public static final int MSG_COMMAND_STOPLOGGING = 0x04; 
	
	/**
	 * Command to the service to interrogate it about what it is doing
	 */
	public static final int MSG_COMMAND_GETSTATUS = 0x05;
	
	/**
	 * Response to the status interrogation command
	 */
	public static final int MSG_RESPONSE_STATUS = 0x06;
	
	/**
	 * Message for updating our activities with sensor data
	 */
	public static final int MSG_RESPONSE_SENSOREVENT = 0x07;
	
    /**
     * Message for updating our activities with logging statistics
     */
	public static final int MSG_RESPONSE_STATISTICS = 0x07;

/*
*/
	protected static class DataloggerParams implements Parcelable {
		DataloggerParams() {};
		
		@Override
		public int describeContents() {
			return 0;
		}
		
		@Override
		public void writeToParcel(Parcel dest, int flags) {
		}
		
		public static final Parcelable.Creator<DataloggerParams> CREATOR
			= new Parcelable.Creator<DataloggerParams>() {
				public DataloggerParams createFromParcel(Parcel in) {
						return new DataloggerParams(in);
				}
			public DataloggerParams[] newArray(int size) {
					return new DataloggerParams[size];
			}
		};

		private DataloggerParams(Parcel in) {
		}
	}

	protected static class DataloggerConfigurableParams extends DataloggerParams implements Parcelable {
		protected final int mSensorUpdateDelay;
		protected final int mLoggingType;
		
		public DataloggerConfigurableParams(int sensorUpdateDelay, int loggingType) {
			mSensorUpdateDelay = sensorUpdateDelay;
			mLoggingType = loggingType;
		}
		public int getSensorUpdateDelay() { return mSensorUpdateDelay; }
		public int getLoggingType() { return mLoggingType; }		

		public static final Parcelable.Creator<DataloggerParams> CREATOR
			= new Parcelable.Creator<DataloggerParams>() {
				public DataloggerConfigurableParams createFromParcel(Parcel in) {
						return new DataloggerConfigurableParams(in);
				}
			public DataloggerConfigurableParams[] newArray(int size) {
					return new DataloggerConfigurableParams[size];
			}
		};
	
		DataloggerConfigurableParams(Parcel in) {
			super(in);
			mSensorUpdateDelay = in.readInt();
			mLoggingType = in.readInt();
		}
		
		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(mSensorUpdateDelay);
			dest.writeInt(mLoggingType);
		}
		
	};

	public static class DataloggerStartParams extends DataloggerConfigurableParams implements Parcelable {
		public DataloggerStartParams(int sensorUpdateDelay, int loggingType) {
			super(sensorUpdateDelay, loggingType);
		}
		
		public static final Parcelable.Creator<DataloggerStartParams> CREATOR
		= new Parcelable.Creator<DataloggerStartParams>() {
			public DataloggerStartParams createFromParcel(Parcel in) {
					return new DataloggerStartParams(in);
			}
		public DataloggerStartParams[] newArray(int size) {
				return new DataloggerStartParams[size];
		}
		};
		
		DataloggerStartParams(Parcel in) {
			super(in);
		}

	};
		
	public static class DataloggerStatusParams extends DataloggerConfigurableParams implements Parcelable {
		protected boolean mLogging;
		protected boolean mStatusChanged;
		protected String mFilename;
		public DataloggerStatusParams(boolean logging, boolean statusChanged, int sensorUpdateDelay, int loggingType, String filename) {
			super(sensorUpdateDelay, loggingType);
			mLogging = logging;
			mStatusChanged = statusChanged;
			mFilename = filename;
		}
		public boolean getLogging() { return mLogging; }
		public boolean getStatusChanged() { return mStatusChanged; }
		public String getFilename() { return mFilename; }
		
		public static final Parcelable.Creator<DataloggerStatusParams> CREATOR
			= new Parcelable.Creator<DataloggerStatusParams>() {
				public DataloggerStatusParams createFromParcel(Parcel in) {
						return new DataloggerStatusParams(in);
				}
			public DataloggerStatusParams[] newArray(int size) {
					return new DataloggerStatusParams[size];
			}
		};
		
		DataloggerStatusParams(Parcel in) {
			super(in);
			
			mLogging = in.readInt() > 0;
			mStatusChanged = in.readInt() > 0;
			mFilename = in.readString();
		}
		
		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(mLogging?1:0);
			dest.writeInt(mStatusChanged?1:0);
			dest.writeString(mFilename);
		}
	};
		
	public static class DataloggerStatisticsParams extends DataloggerParams {
		protected long mEventsCount;
		protected float mTotalRate;
		protected float mLatestRate;
		public DataloggerStatisticsParams(long eventsCount, float totalRate, float latestRate) { 
			mEventsCount = eventsCount;
			mTotalRate = totalRate;
			mLatestRate = latestRate;
		}
		public long getEventsCount() { return mEventsCount; }
		public float getTotalRate() { return mTotalRate; }
		public float getLatestRate() { return mLatestRate; }
		
		DataloggerStatisticsParams(Parcel in) {
			super(in);
			mEventsCount = in.readLong();
			mTotalRate = in.readFloat();
			mLatestRate = in.readFloat();
		}
		
		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeLong(mEventsCount);
			dest.writeFloat(mTotalRate);
			dest.writeFloat(mLatestRate);
		}
		
	};
		
	
    class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_COMMAND_REGISTERCLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_COMMAND_UNREGISTERCLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_COMMAND_STARTLOGGING:
					Log.d("AccelerometerLoggerService.ServiceHandler", "MSG_COMMAND_STARTLOGGING command received");
                	Bundle bundle =  msg.getData();
                	if (bundle==null) {
                		Log.e("AccelerometerLoggerService.ServiceHandler", "Bundle is null");
                		stopSelf();
                	} else {
	                	DataloggerStartParams dsp = (DataloggerStartParams)bundle.getParcelable(BUNDLE_PARCELLABLE_PARAMS);
	                	if (dsp == null) {
    						Log.e("AccelerometerLoggerService.ServiceHandler", "params are null");
    					} else {
							// Pull out the update rate from the intent
							Log.d("AccelerometerLoggerService", String.format("Old sensor rate: %d, ", mSensorRate));
			                mSensorRate = dsp.getSensorUpdateDelay();
							Log.d("AccelerometerLoggerService", String.format("New sensor rate: %d, ", mSensorRate));
							mLoggingType = dsp.getLoggingType();
							Log.d("AccelerometerLoggerService", String.format("New logging type: %d\n", mLoggingType));
							startLogging();
		                	sendStatusResponse(true);
		                    break;
    					}
                	}
                case MSG_COMMAND_STOPLOGGING:
					Log.d("AccelerometerLoggerService", "MSG_COMMAND_STOPLOGGING command received");
					stopLogging();
                	sendStatusResponse(true);
                    break;
                case MSG_COMMAND_GETSTATUS:
					Log.d("AccelerometerLoggerService.ServiceHandler", "MSG_COMMAND_GETSTATUS command received");
                	sendStatusResponse(false);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
        
        private void sendStatusResponse(boolean statusIsNew) {
        	DataloggerStatusParams params = new DataloggerStatusParams(logging, statusIsNew, mSensorRate, mLoggingType, mLogFilename);
        	Bundle bundle = new Bundle();
//        	bundle.setClassLoader(getClassLoader());
//        	bundle.putSerializable(BUNDLE_PARCELLABLE_PARAMS, params);
        	bundle.putParcelable(BUNDLE_PARCELLABLE_PARAMS, params);
            for (int i=mClients.size()-1; i>=0; i--) {
                try {
                	Message msg = Message.obtain(null, MSG_RESPONSE_STATUS);
                	msg.setData(bundle);
                    mClients.get(i).send(msg);
                } catch (RemoteException e) {
                    // The client is dead.  Remove it from the list;
                    // we are going through the list from back to front
                    // so this is safe to do inside the loop.
                    mClients.remove(i);
                }
            }
        }
        
        private void sendSensorEvent(SensorEvent event) {
            for (int i=mClients.size()-1; i>=0; i--) {
                try {
                    mClients.get(i).send(Message.obtain(null, MSG_RESPONSE_SENSOREVENT, event));
                    
                } catch (RemoteException e) {
                    // The client is dead.  Remove it from the list;
                    // we are going through the list from back to front
                    // so this is safe to do inside the loop.
                    mClients.remove(i);
                }
            }
        }
        
        private void sendStatisticsEvent(int eventsCount, float totalRate, float latestRate) {
        	DataloggerStatisticsParams params = new DataloggerStatisticsParams(eventsCount, totalRate, latestRate);
        	Bundle bundle = new Bundle();
        	bundle.putParcelable(BUNDLE_PARCELLABLE_PARAMS, params);
            for (int i=mClients.size()-1; i>=0; i--) {
                try {
                    mClients.get(i).send(Message.obtain(null, MSG_RESPONSE_SENSOREVENT, bundle));
                } catch (RemoteException e) {
                    // The client is dead.  Remove it from the list;
                    // we are going through the list from back to front
                    // so this is safe to do inside the loop.
                    mClients.remove(i);
                }
            }
        }
        
    }
	
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
//    final Messenger mMessenger = new Messenger(new ServiceHandler());
//    final Messenger mMessenger;
    Messenger mMessenger;

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
		Log.d("AccelerometerLoggerService", "onBind()");
        return mMessenger.getBinder();
    }
	
	@Override
	public void onCreate() {
		Log.d("AccelerometerLoggerService", "onCreate()");
		
		mServiceHandler = new ServiceHandler();
	    mMessenger = new Messenger(mServiceHandler);
		
/*	    
		// Start up the thread running the service.  Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block.  We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		HandlerThread thread = new HandlerThread("ServiceStartArguments", HANDLERTHREAD_PRIORITY);
		thread.start();

		// Get the HandlerThread's Looper and use it for our Handler 
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
*/		
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("AccelerometerLoggerService", "onStartCommand()");

		// Need to parse the intent for command
		if (intent == null) {
			// This happens when our service gets killed
			Log.e("AccelerometerLoggerService", "onStartCommand() with null intent");
		} else {
			// Regular start command
		}

		// If we get killed, after returning from here, restart
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		Log.d("AccelerometerLoggerService", "onDestroy()");
	}
	
    private boolean startLogging() {
		Log.d("AccelerometerLoggerService", "startLogging()");
				
		if ((mLoggingType & LOGTYPE_FILE) > 0) {
			// Logging to file as well
		    String state = Environment.getExternalStorageState();
		    if (!Environment.MEDIA_MOUNTED.equals(state)) {
				Toast.makeText(this, getString(R.string.error_cannotcreatedir), Toast.LENGTH_SHORT).show();
		        stopSelf();
		        return false;
		    } // else: we know the external storage is available
		    
	        // Create a directory
		    File logDir = new File(Environment.getExternalStorageDirectory(), APPLICATION_DIRECTORY);
		    if (!logDir.exists()) {
		        if (!logDir.mkdirs()) {
		        	// Trouble creating directory
					Toast.makeText(this, getString(R.string.error_cannotcreatedir), Toast.LENGTH_SHORT).show();
			        return false;
		        }
		    }

		    // Create a file
		    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HH-mm-ss", Locale.US);
		    Date now = new Date();
		    mLogFilename = formatter.format(now) + ".txt";
	        logFile = new File(logDir.getAbsolutePath(), mLogFilename);
	        try {
	        	logOutputStream = new FileOutputStream(logFile);
		        logWriter = new PrintWriter(logOutputStream);
		        logWriter.format(
		        	"%s\t%s\t%s\t%s\n", 
		        	getString(R.string.label_time), 
		        	getString(R.string.label_x_axis), 
		        	getString(R.string.label_y_axis), 
		        	getString(R.string.label_z_axis)
		        );
	        } catch (Exception e) {
	            e.printStackTrace();
				return false;
	        }    
		} else {
			// Not logging to file
			logWriter = null;
			logOutputStream = null;
		}
		
		// Here we know logging is going to start
		logging = true;
		
        // Create a notification
        notificationStart();
        
        // Reset the sensor events count
        mTotalSensorEventsCount = 0;
        // Reset the update rate
        mUpdateStatisticsRatio = UPDATE_STATISTICS_EVERY_INITIAL;
        
		// Register for sensor events
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, mSensorRate, mServiceHandler);

		return true;
    }

    private void stopLogging() {
    	if (mSensorManager != null) {
			mSensorManager.unregisterListener(this);
    	}
		notificationEnd();
		logging = false;
		logWriter = null;
		logOutputStream = null;
    }
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
//		Log.v("AccelerometerLoggerService", "onSensorChanged");
        try {
        	
        	// Write to file, if writing to file is desired
        	if (logWriter != null) {
//    			Log.v("AccelerometerLoggerService", "saving to file");
	        	logWriter.print(event.timestamp);
	        	for (int i=0;i<event.values.length;i++)
	        		logWriter.format("\t%f", event.values[i]);
	        	logWriter.println();
    		}
        	
        	Intent intent = null;
        	
        	// Send a notification to the main window, if that's what is called for
    		if ((mLoggingType & LOGTYPE_GRAPH) > 0) {
        		// Notify the parent window
    			//~ Log.v("AccelerometerLoggerService", "notifying parent");
    			mServiceHandler.sendSensorEvent(event);
        	}
    		
    		// Update sensor cound and first timestamp
        	if (mTotalSensorEventsCount++ == 0) {
        		mFirstTotalEventTimestamp = event.timestamp;
        	} else if ( (mTotalSensorEventsCount%mUpdateStatisticsRatio) == 0 ) {
        		// Send statistics update
    			Log.v("AccelerometerLoggerService", "sending statistics");
    			
    			long nsecsTotal, nsecsLatest;
    			nsecsTotal = event.timestamp - mFirstTotalEventTimestamp;
    			nsecsLatest = event.timestamp - mFirstLatestEventTimestamp;
    			float sensorRateTotal =  mTotalSensorEventsCount / (nsecsTotal/1000000000f);
    			mServiceHandler.sendStatisticsEvent(mTotalSensorEventsCount, sensorRateTotal, mUpdateStatisticsRatio / (nsecsLatest/1000000000f));
    			
    			// Remember the start of the last packet
        		mFirstLatestEventTimestamp = event.timestamp;
        		
        		// Update the update rate
        		mUpdateStatisticsRatio = Math.max((int)sensorRateTotal/2,1); 
    		}
        	
    		// If there is an intent to send, do send it
    		if (intent != null ) {
    			//~ Log.v("AccelerometerLoggerService", "broadcasting intent");
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
			    .setContentTitle(getString(R.string.notification_title))
			    .setContentText(getString(R.string.notification_text))
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
		Notification notification = mBuilder.build();
		mNotifyMgr.notify(mNotificationId, notification);
		// Set foreground
		startForeground (0, notification);		
	}
	
	private void notificationEnd() {
		NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotifyMgr.cancel(NOTIFICATIONID_INPROGRESS);
	}
	
}

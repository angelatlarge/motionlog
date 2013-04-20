package com.atlarge.motionlog;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
	private boolean flagStarted = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void startStopButtonClick(View view) {
		Button btn = (Button)findViewById(R.id.button_startstop);
		if (flagStarted) {
			startLogging();
			btn.setText("Stop");			
		} else {
			stopLogging();
			btn.setText("Start");			
		}
		flagStarted = !flagStarted;
	}
	
	private void startLogging() {
		Intent intent = new Intent(this, AccelerometerLoggerService.class);
		/*
		EditText editText = (EditText) findViewById(R.id.edit_message);
		String message = editText.getText().toString();
		intent.putExtra(EXTRA_MESSAGE, message);
		*/
		startService(intent);
	}

	private void stopLogging() {
		Intent intent = new Intent(this, AccelerometerLoggerService.class);
		stopService(intent);
	}
}

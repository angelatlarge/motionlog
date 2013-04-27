package com.atlarge.motionlog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class GraphViewLine extends GraphViewBase {
	protected float mScaleX = (float) 0.00002;
	
	private LinkedList<Datapoint> mDatapoints;
    private LinkedList<PointF> mDrawPoints;
	
	protected class Datapoint {
		float[] mData;
		long mTimestamp;
		int mPresent;
		
		Datapoint(int idx, float _data, long _timestamp) {
			mData = new float[mGraphCount];
			mData[idx] = _data;
			mTimestamp = _timestamp;
			mPresent = 1<<idx;
		}
		
		boolean isPresent(int idx) {
			return (mPresent & (1<<idx)) != 0;
		}
		
		float get(int idx) {
			return mData[idx];
		}
	}

	public GraphViewLine(Context context) {
		super(context);
		init(null, 0);
	}

	public GraphViewLine(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs, 0);
	}

	public GraphViewLine(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs, defStyle);
	}
	
	protected void init(AttributeSet attrs, int defStyle) {
		super.init(attrs, defStyle);
		
		mDatapoints = new LinkedList<Datapoint>();
		mDrawPoints = new LinkedList<PointF>();
		
	}

	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Log.d("GraphViewLine", "draw()");  

		// Draw the graphs
		Iterator<Datapoint> it = mDatapoints.descendingIterator();
		if (it.hasNext()) {
			float x2 = mWidth - 1;
			Datapoint dp = it.next();
			long lastTime = dp.mTimestamp;
			float y2 = (dp.get(0) + mMaxRange[0]) / (mMaxRange[0] * 2) * mHeight;
			while (it.hasNext()) { 
				dp = it.next();
				float y1 = (dp.get(0) + mMaxRange[0]) / (mMaxRange[0] * 2) * mHeight;
				float x1 = x2 - (lastTime - dp.mTimestamp) * mScaleX;
				Log.d("GraphViewLine", String.format("drawing a line from %f, %f, to %f,%f (%d)", x1, y1, x2, y2, (lastTime - dp.mTimestamp)));  
				canvas.drawLine(x1, y1, x2, y2, mReadingPaints[0]);
				y2=y1;
				x2=x1;
				lastTime = dp.mTimestamp;
			}

		}
	}

	/**
	 * Gets the example string attribute value.
	 * 
	 * @return The example string attribute value.
	 */
	public String getExampleString() {
		return mExampleString;
	}

	public void addReading(int readingIndex, float readingValue, long timestamp) {
		// Deal with the data
		Log.d("GraphViewLine", String.format("Adding datapoint %f %d", readingValue, timestamp/1000));
		mDatapoints.add(new Datapoint(readingIndex, readingValue, timestamp/1000));
//		while ((timestamp - mDatapoints.peek().mTimestamp) * mScaleX > mWidth) {
//			mDatapoints.removeFirst();
//		}
		
		invalidate();
		
	}
	
}

package com.atlarge.motionlog;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;

public class GraphViewBitmap extends GraphViewBase {
	protected final int SCROLL_VALUE = 2;
	protected Canvas[] mReadingsCanvas;
	protected Bitmap[] mReadingsBitmap;
	protected int mDrawBmpIndex = 0;
	protected int[] mReadingLag;
	protected float[] mLastReadingY;
	
	public GraphViewBitmap(Context context) {
		super(context);
		init(null, 0);
	}

	public GraphViewBitmap(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs, 0);
	}

	public GraphViewBitmap(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs, defStyle);
	}
	
	protected void init(AttributeSet attrs, int defStyle) {
		super.init(attrs, defStyle);
		
		// Create reading storage
		recreateReadingStorage();
	}

	protected void recreateReadingStorage() {
		mLastReadingY = new float[mGraphCount];
		mReadingLag = new int[mGraphCount]; 
		for (int i=0; i<mGraphCount; i++) {
			mLastReadingY[i] = Float.NEGATIVE_INFINITY;
			mMaxRange[i] = 1;
			mReadingLag[i] = 0;
		}				
	}
	
	@Override
	protected void onSizeChanged (int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		Log.d("GraphViewBitmap", String.format("onSizeChanged(%d,%d,%d,%d", w, h, oldw, oldh));
		
		// Create a bitmap for the readings
		if (mReadingsBitmap == null) {
			mReadingsCanvas	= new Canvas[2];
			mReadingsBitmap	= new Bitmap[2];
			for (int i=0; i<2; i++) {
				mReadingsBitmap[i] = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
				mReadingsBitmap[i].eraseColor(Color.TRANSPARENT);
				mReadingsCanvas[i] = new Canvas();
				mReadingsCanvas[i].setBitmap(mReadingsBitmap[i]);
			}
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Log.d("GraphViewBitmap", "draw()");  

		canvas.drawBitmap(mReadingsBitmap[mDrawBmpIndex], 0, 0, null);
	}

	public void clear() {
		if (mReadingsBitmap==null)
			return;
		for (int i=0; i<2;i++) {
			if (mReadingsBitmap[i] != null)
				mReadingsBitmap[i].eraseColor(Color.TRANSPARENT);
		}
	}
	
	public void setGraphCount(int value) {
		super.setGraphCount(value);
		recreateReadingStorage();
	}
	
	public void addReading(int readingIndex, float readingValue, long timestamp) {
		Log.d("GraphViewBitmap", String.format("Adding datapoint index %d value %f timestamp %d", readingIndex, readingValue, timestamp/1000));
		
		float newReadingY = 
				(readingValue + mMaxRange[readingIndex]) / (mMaxRange[readingIndex] * 2) * mHeight;
		boolean haveLastReading = mLastReadingY[readingIndex] != Float.NEGATIVE_INFINITY;
		if (haveLastReading) {
			// Draw something
			int nNewLineX1;
			boolean mScroll = mReadingLag[readingIndex] == 0;
			int OtherBmpIndex = (mDrawBmpIndex+1) % 2;
			int nDrawTargetBmpIndex;
			if (mScroll) {
				// Move the bitmap
				mReadingsBitmap[OtherBmpIndex].eraseColor(Color.TRANSPARENT);
				mReadingsCanvas[OtherBmpIndex].drawBitmap(mReadingsBitmap[mDrawBmpIndex], -SCROLL_VALUE, 0, null);
				nNewLineX1 = mWidth - SCROLL_VALUE - 1;
				nDrawTargetBmpIndex = OtherBmpIndex;
			} else {
				nDrawTargetBmpIndex = mDrawBmpIndex;
				nNewLineX1 = mWidth - 2 - mReadingLag[readingIndex] * SCROLL_VALUE;
			}
			
			// Draw the extra reading
			Log.d("GraphView", String.format("drawing a line from %d, %f, to %d,%f", nNewLineX1, mLastReadingY[readingIndex], mWidth-1, newReadingY));  
			mReadingsCanvas[nDrawTargetBmpIndex].drawLine(nNewLineX1, mLastReadingY[readingIndex], mWidth-1, newReadingY, mGraphPaints[readingIndex]);
			
			// Flip the bitmaps
			if (mScroll) {
				mDrawBmpIndex = OtherBmpIndex;
			}
			// Cue drawing update
			invalidate();
		}

		mLastReadingY[readingIndex] = newReadingY;
		
		// Update the readings lag
		if (mReadingLag[readingIndex] == 0) {
			for (int i=0; i<mGraphCount; i++) {
				if (i != readingIndex)
					mReadingLag[i]++;
			}
		} else {
			mReadingLag[readingIndex]--;
		}
	}
	
	
}

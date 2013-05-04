package com.atlarge.motionlog;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
//import android.util.Log;

/**
 * Class that draws a graph using off-screen bitmaps
 */
public class GraphViewBitmap extends GraphViewBase {
	/** How many pixels to scroll for each value (X values are taken to be consequtive) */
	protected final int SCROLL_VALUE = 2;
	
	/**
	 * Offscreen bitmap for drawing the graph
	 * We use two, and blit from one to the other for maximum speed
	 */ 
	//TODO: rename
	protected Bitmap[] mGraphBitmap = new Bitmap[2];
	/** Canvas used to draw on the off-screen bitmaps */
	//TODO: rename
	protected final Canvas[] mGraphCanvas = new Canvas[2];
	
	/** Index of the currently active (drawn to screen) bitmap */
	protected int mDrawBmpIndex = 0;
	
	/** For each series, how many values is it behind the series with the most values */
	protected int[] mSeriesLag;

	/** For each series, the Y coordinate of the last value */
	protected float[] mLastValueY;
	
	
	/********************************************************************/
	/* Construction and destruction */
	
	/**
	 * Constructor
	 */
	public GraphViewBitmap(Context context) {
		super(context);
		init(null, 0);
	}

	/**
	 * Constructor
	 */
	public GraphViewBitmap(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs, 0);
	}

	/**
	 * Constructor
	 */
	public GraphViewBitmap(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs, defStyle);
	}
	
	/********************************************************************/
	/* Initialization and de-initialization */

	/**
	 * Main initialization routine: called from constructors
	 */
	protected void init(AttributeSet attrs, int defStyle) {
		super.init(attrs, defStyle);
		
		// Create storage for values
		recreateValuesStorage();
	}

	/**
	 * Creates various storage arrays that depend on 
	 * the number of series being displayed (mSeriesCount)
	 */
	protected void recreateValuesStorage() {
		mLastValueY = new float[mSeriesCount];
		mSeriesLag = new int[mSeriesCount]; 
		for (int i=0; i<mSeriesCount; i++) {
			mLastValueY[i] = Float.NEGATIVE_INFINITY;
			mMaximumYExtent[i] = 1;
			mSeriesLag[i] = 0;
		}				
	}
	
	/********************************************************************/
	/* Event-responding methods (excluding draw) */

	/**
	 * Called when the size of the View changes
	 * 
	 * Need to recreate the bitmaps that depend on size
	 */
	@Override
	protected void onSizeChanged (int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		//~ Log.d("GraphViewBitmap", String.format("onSizeChanged(%d,%d,%d,%d", w, h, oldw, oldh));
		
		// (Re)create a bitmap for the series values
		for (int i=0; i<2; i++) {
			mGraphBitmap[i] = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
			mGraphBitmap[i].eraseColor(Color.TRANSPARENT);
			mGraphCanvas[i] = new Canvas();
			mGraphCanvas[i].setBitmap(mGraphBitmap[i]);
		}
	}
	

	/********************************************************************/
	/* Property access and other interface methods */
	
	/**
	 * Clear all the series values data
	 */
	@Override
	public void clear() {
		for (int i=0; i<2;i++) {
			if (mGraphBitmap[i] != null)
				mGraphBitmap[i].eraseColor(Color.TRANSPARENT);
		}
	}
	
	/**
	 * Set the number of series to draw
	 */
	@Override
	public void setSeriesCount(int value) {
		super.setSeriesCount(value);
		recreateValuesStorage();
	}
	
	/**
	 * Add a value to a series
	 */
	@Override
	public void addValue(int seriesIndex, float value, long timestamp) {
		//~ Log.d("GraphViewBitmap", String.format("Adding datapoint index %d value %f timestamp %d", seriesIndex, value, timestamp/1000));
		
		
		float newValueY = 
				(value + mMaximumYExtent[seriesIndex]) / (mMaximumYExtent[seriesIndex] * 2) * mHeight;
		boolean havePreviousValue = mLastValueY[seriesIndex] != Float.NEGATIVE_INFINITY;
		if (havePreviousValue) {
			// There is a previous value, so we can draw a line
			
			// Scroll the bitmap if necessary to make room for the new line
			int nNewLineX1;
			boolean mScroll = mSeriesLag[seriesIndex] == 0;
			int OtherBmpIndex = (mDrawBmpIndex+1) % 2;
			int nDrawTargetBmpIndex;
			if (mScroll) {
				// Move the bitmap
				mGraphBitmap[OtherBmpIndex].eraseColor(Color.TRANSPARENT);
				mGraphCanvas[OtherBmpIndex].drawBitmap(mGraphBitmap[mDrawBmpIndex], -SCROLL_VALUE, 0, null);
				nNewLineX1 = mWidth - SCROLL_VALUE - 1;
				nDrawTargetBmpIndex = OtherBmpIndex;
			} else {
				nDrawTargetBmpIndex = mDrawBmpIndex;
				nNewLineX1 = mWidth - 2 - mSeriesLag[seriesIndex] * SCROLL_VALUE;
			}
			
			// Draw the line to the new value
			mGraphCanvas[nDrawTargetBmpIndex].drawLine(nNewLineX1, mLastValueY[seriesIndex], mWidth-1, newValueY, mSeriesPaints[seriesIndex]);
			
			// Flip the bitmaps
			if (mScroll) {
				mDrawBmpIndex = OtherBmpIndex;
			}
			
			// Cue a drawing update
			invalidate();
		}

		// Save the new value as last seen value
		mLastValueY[seriesIndex] = newValueY;
		
		// Update the lag state for each series
		if (mSeriesLag[seriesIndex] == 0) {
			// This is one of the leading series. Increase the lag for all others
			for (int i=0; i<mSeriesCount; i++) {
				if (i != seriesIndex)
					mSeriesLag[i]++;
			}
		} else {
			// This is a lagging series, just decrease the lag
			mSeriesLag[seriesIndex]--;
		}
	}
	
	/********************************************************************/
	/* Drawing methods */

	/**
	 * Main drawing method
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		// Inherited method will draw the grid for us
		super.onDraw(canvas);
		//~ Log.d("GraphViewBitmap", "draw()");  

		// We will draw our off-screen bitmap
		canvas.drawBitmap(mGraphBitmap[mDrawBmpIndex], 0, 0, null);
	}
	
}

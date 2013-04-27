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
		mLastReadingY = new float[LINES_COUNT];
		mReadingLag = new int[LINES_COUNT]; 
		mMaxRange = new float[LINES_COUNT]; 
		for (int i=0; i<LINES_COUNT; i++) {
			mLastReadingY[i] = Float.NEGATIVE_INFINITY;
			mMaxRange[i] = 1;
			mReadingLag[i] = 0;
		}		
		
//		mTransparentPaint = new Paint(Paint.ANTI_ALIAS_FLAG); 
		mTransparentPaint = new Paint();
		mTransparentPaint.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR));
//		mTransparentPaint.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_OUT)); 
		mTransparentPaint.setColor(Color.TRANSPARENT);
		mTransparentPaint.setStyle(Paint.Style.FILL);
		
		
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
		// Draw the graphs
		//* TODO: Write this
	}

	/**
	 * Gets the example string attribute value.
	 * 
	 * @return The example string attribute value.
	 */
	public String getExampleString() {
		return mExampleString;
	}
	
	public void clear() {
		for (int i=0; i<2;i++) {
			mReadingsBitmap[i].eraseColor(Color.TRANSPARENT);
		}
	}

	public void addReading(int readingIndex, float readingValue, long timestamp) {
		Log.d("GraphViewBitmap", String.format("Adding datapoint %f %d", readingValue, timestamp/1000));
		
		float newReadingY = 
				(readingValue + mMaxRange[readingIndex]) / (mMaxRange[readingIndex] * 2) * mHeight;
		boolean haveLastReading = mLastReadingY[readingIndex] != Float.NEGATIVE_INFINITY;
		if (haveLastReading) {
			// Move the bitmap 
			int OtherBmpIndex = (mDrawBmpIndex+1) % 2;
			int nNewLineX1;
			if (mReadingLag[readingIndex] == 0) {
				mReadingsBitmap[OtherBmpIndex].eraseColor(Color.TRANSPARENT);
				mReadingsCanvas[OtherBmpIndex].drawBitmap(mReadingsBitmap[mDrawBmpIndex], -SCROLL_VALUE, 0, null);
//				mReadingsCanvas.drawRect(mWidth-SCROLL_VALUE - 1, 0, mWidth, mHeight, mReadingPaints[1]);//mTransparentPaint);
				nNewLineX1 = mWidth - SCROLL_VALUE - 1;
			} else {
				nNewLineX1 = mWidth - 2 - mReadingLag[readingIndex] * SCROLL_VALUE;
			}
			
			// Draw the extra reading
			Log.d("GraphView", String.format("drawing a line from %d, %f, to %d,%f", nNewLineX1, mLastReadingY[readingIndex], mWidth-1, newReadingY));  
			mReadingsCanvas[OtherBmpIndex].drawLine(nNewLineX1, mLastReadingY[readingIndex], mWidth-1, newReadingY, mReadingPaints[readingIndex]);
			mDrawBmpIndex = OtherBmpIndex;
			// Cue drawing update
			invalidate();
		}

		mLastReadingY[readingIndex] = newReadingY;
		
		// Update the readings lag
		if (mReadingLag[readingIndex] == 0) {
			for (int i=0; i<LINES_COUNT; i++) {
				if (i != readingIndex)
					mReadingLag[i]++;
			}
		} else {
			mReadingLag[readingIndex]--;
		}
	}
	
}

package com.atlarge.motionlog;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class GraphViewBase extends View {
	protected final int DEFAULT_GRAPH_COUNT	= 2;
	protected final float DEFAULT_GRID = 25;
	protected int mGraphCount = DEFAULT_GRAPH_COUNT;
	
	protected Paint mGridPaint;
	protected Paint mBorderPaint;
	protected Paint mCenterPaint;
	protected Paint[] mGraphPaints;

	protected Canvas mGridCanvas;
	protected Bitmap mGridBitmap;

	private static final float GRIDLABEL_SIZE = 12;
	protected TextPaint mGridLabelPaint;
	protected float mTextWidth;
	protected float mTextHeight;
	protected float mGridScreenWidth;
	protected float mGridLogicalSize;
	protected int mGridLegendDecimals;
	
	protected float[] mMaxRange;
	
	protected int mWidth;
	protected int mHeight;
	
	
	public GraphViewBase(Context context) {
		super(context);
		init(null, 0);
	}

	public GraphViewBase(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs, 0);
	}

	public GraphViewBase(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs, defStyle);
	}

	protected void init(AttributeSet attrs, int defStyle) {
		// Load attributes
//		final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.GraphView, defStyle, 0);
//		a.recycle();

		// Set up a default TextPaint object
		mGridLabelPaint = new TextPaint();
		mGridLabelPaint.setColor(0xFFFFFFFF);
		mGridLabelPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		mGridLabelPaint.setTextAlign(Paint.Align.LEFT);
		mGridLabelPaint.setTextSize(GRIDLABEL_SIZE);

		mBorderPaint = new Paint();
		mBorderPaint.setARGB (0xFF,0x40,0x40,0x40);
		mBorderPaint.setStyle(Paint.Style.STROKE);
		mBorderPaint.setStrokeWidth(1);
		
		mGridPaint = new Paint();
		mGridPaint.setARGB (0xFF,0x20,0x20,0x20);
		mGridPaint.setStyle(Paint.Style.STROKE);
		mGridPaint.setStrokeWidth(1);
		
		mCenterPaint = new Paint();
		mCenterPaint.setARGB (0xFF,0x80,0x80,0x80);
		mCenterPaint.setStyle(Paint.Style.STROKE);
		mCenterPaint.setStrokeWidth(1);
		
		// Create the paint for the readings
		recreateReadingPaints();

		// Max range storage
		recreateMaxRange();

	}

	protected void recreateReadingPaints() {
		mGraphPaints = new Paint[mGraphCount];
		for (int i=0; i<mGraphCount; i++) {
			mGraphPaints[i] = new Paint();
			mGraphPaints[i].setStyle(Paint.Style.STROKE);
			mGraphPaints[i].setStrokeWidth(1);
//			mGraphPaints[i].setAntiAlias(true);
		}		
		generateDefaultGraphColors();
	}
	
	protected void recreateMaxRange() {
		Log.d("GraphViewBase", String.format("recreateMaxRange with %d", mGraphCount));
		float[] newMaxRange = new float[mGraphCount];
		if (mMaxRange != null) {
			for (int i=0; i<mGraphCount; i++) {
				if (i<mMaxRange.length) {
					newMaxRange[i] = mMaxRange[i];
				} else {
					newMaxRange[i] = 1;
				}
			}
		}
		mMaxRange = newMaxRange; 
	}
	
	protected void generateDefaultGraphColors() {
		Log.d("GraphViewBase", String.format("generateDefaultGraphColors()"));
		DefaultColorIterator dci = new DefaultColorIterator();
		for (int idxColor = 0; idxColor<mGraphPaints.length; idxColor++) {
			mGraphPaints[idxColor].setColor(dci.getNext());
		}			
	}
	
	protected void onSizeChanged (int w, int h, int oldw, int oldh) {
		Log.d("GraphViewBase", String.format("onSizeChanged(%d,%d,%d,%d", w, h, oldw, oldh));
		mWidth = w;
		mHeight = h;
		
	}
	
	protected void destroyGrid() {
		mGridCanvas = null;
		mGridBitmap = null;
	}

	protected void ensureGridExists() {
		mGridBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		mGridCanvas = new Canvas();
		mGridCanvas.setBitmap(mGridBitmap);
		mGridBitmap.eraseColor(Color.TRANSPARENT);
		
		GraphTickMarks gtm = new GraphTickMarks(-mMaxRange[0], mMaxRange[0], (int)(mHeight/DEFAULT_GRID), false);
		mGridLogicalSize = gtm.tickSpacing();
		mGridScreenWidth = mHeight/(gtm.graphMax() - gtm.graphMin()) * mGridLogicalSize;
		mGridLegendDecimals = gtm.fractionalDigits();
		
		Log.d("GraphViewBase", String.format("recreateGrid: mGridLogicalSize %f, mGridScreenWidth %f, max-min: %f/%f ", mGridLogicalSize, mGridScreenWidth, gtm.graphMax(), gtm.graphMin())); 
		
		drawGrid(mGridCanvas);
	}
	
	protected void drawGrid(Canvas canvas) {
		// Draw the grid
		
		// Vertical lines
		for (float x=mGridScreenWidth;x<mWidth;x+=mGridScreenWidth) {
			canvas.drawLine(x, 0, x, mHeight, mGridPaint);
		}
		
		// Center line
		float nCenter = mHeight/2;
		canvas.drawLine(0, nCenter, mWidth, nCenter, mCenterPaint);
		
		// Horizontal lines
		float[] y = {nCenter-mGridScreenWidth, nCenter+mGridScreenWidth};
		while (y[0]>0) {		// Positive y is down
			canvas.drawLine(0, y[0], mWidth, y[0], mGridPaint);
			canvas.drawLine(0, y[1], mWidth, y[1], mGridPaint);
			y[0] -= mGridScreenWidth;
			y[1] += mGridScreenWidth;		
		}
		
		// Border
		canvas.drawRect(0, 0, mWidth-1, mHeight-1, mBorderPaint);
		
		// Horizontal labels
		Paint.FontMetrics fontMetrics = mGridLabelPaint.getFontMetrics();
		//~ mTextHeight = fontMetrics.bottom;
		
		
		y[0] = nCenter-mGridScreenWidth; y[1] = nCenter+mGridScreenWidth;  
		float logicalLabel = 0;
		logicalLabel += mGridLogicalSize;
		while (y[0]>=0) {		// Positive y is down
			// Draw the tick labels
			StringBuilder sb = new StringBuilder(String.format("%+." + ((Integer)(mGridLegendDecimals)).toString() + "f", logicalLabel));
			canvas.drawText(sb.toString(), GRIDLABEL_SIZE/2, y[0]+fontMetrics.bottom, mGridLabelPaint);
			sb.setCharAt(0, '-');
			canvas.drawText(sb.toString(), GRIDLABEL_SIZE/2, y[1]+fontMetrics.bottom, mGridLabelPaint);
			y[0] -= mGridScreenWidth*2;
			y[1] += mGridScreenWidth*2;		
			logicalLabel += mGridLogicalSize*2;
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Log.d("GraphViewBase", "draw()");  

		// Draw the grid from off-screen bitmap
		ensureGridExists();
		canvas.drawBitmap(mGridBitmap, 0, 0, null);
		
	}

	public void setMaxRange(int readingIndex, float maxRange) {
		if (readingIndex==0)
			destroyGrid();
		mMaxRange[readingIndex] = maxRange;
	}
	
	public void setGraphCount(int value) {
		mGraphCount = value;
		recreateReadingPaints();
		recreateMaxRange();
		clear();
	}
	
	public void setGraphColor(int idx, int a, int r, int g, int b) {
		mGraphPaints[idx].setARGB(a, r, g, b);
	}
	
	public void setGraphColor(int idx, int color) {
		mGraphPaints[idx].setColor(color);
	}
	
	public void clear() {
	}
	
	public void addReading(int readingIndex, float readingValue, long timestamp) {
	}
	
}

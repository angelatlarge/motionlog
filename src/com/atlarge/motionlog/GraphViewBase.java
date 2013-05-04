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

/**
 * Base glass for drawing graphs
*/ 
public class GraphViewBase extends View {
	
	/** Default size of the grid, in dp */
	protected final float DEFAULT_GRID_SIZE = 25;
	
	/** Number of series to create by default */
	protected final int DEFAULT_SERIES_COUNT	= 1;
	/** Number of series to display on the graph */
	protected int mSeriesCount = DEFAULT_SERIES_COUNT;

	/** Paint used to draw the borders */
	protected Paint mBorderPaint;
	
	/** Paint used to draw the horizontal zero line (x-axis) */
	protected Paint mCenterPaint;
	
	/** Array of paints use to draw each series */
	protected Paint[] mSeriesPaints;
	
	
	/** Paint used to draw the gridlines */
	protected Paint mGridlinePaint;

	/** Size of the text used to draw the grid labels */
	private static final float GRIDLABEL_SIZE = 12;
	
	/** Color used to draw the grid labels */
	private static final int GRIDLABEL_COLOR = 0xFFFFFFFF;
	
	/** Paint used to draw the grid labels */
	protected TextPaint mGridLabelPaint;

	/** Number of decimal digits drawn for each grid label */
	protected int mGridLegendDecimals;
	
	/** Calculated value of the width of the grid cell on screen */
	protected float mGridScreenWidth;
	
	/** The logical size of each grid cell */
	protected float mGridLogicalSize;
	
	/** Canvas used to draw the grid */
	protected Canvas mGridCanvas;

	/** Bitmap on which the grid is drawn. 
	 *  We blit this bitmap onto the drawing canvas for fast draws */
	protected Bitmap mGridBitmap;

	/** Maximum size of the Y axis
	 * if this value is "5" Y axis will be visible from -5 to +5
	 */
	protected float[] mMaximumYExtent;
	
	
	/** Memoized witdth of this View */
	protected int mWidth;
	
	/** Memoized heigth of this View */
	protected int mHeight;
	
	
	
	/**
	 * Constructor
	 */
	public GraphViewBase(Context context) {
		super(context);
		init(null, 0);
	}

	/**
	 * Constructor
	 */
	public GraphViewBase(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs, 0);
	}

	/**
	 * Constructor
	 */
	public GraphViewBase(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs, defStyle);
	}


	/**
	 * Initialization routine: called from constructors
	 */
	protected void init(AttributeSet attrs, int defStyle) {
		// We don't use attributes, so this is not done
		// Load attributes
		//		final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.GraphView, defStyle, 0);
		//		a.recycle();

		// Paint for grid labels
		mGridLabelPaint = new TextPaint();
		mGridLabelPaint.setColor(GRIDLABEL_COLOR);
		mGridLabelPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		mGridLabelPaint.setTextAlign(Paint.Align.LEFT);
		mGridLabelPaint.setTextSize(GRIDLABEL_SIZE);

		// Paint for the border labels
		mBorderPaint = new Paint();
		mBorderPaint.setARGB (0xFF,0x40,0x40,0x40);
		mBorderPaint.setStyle(Paint.Style.STROKE);
		mBorderPaint.setStrokeWidth(1);
		
		mGridlinePaint = new Paint();
		mGridlinePaint.setARGB (0xFF,0x20,0x20,0x20);
		mGridlinePaint.setStyle(Paint.Style.STROKE);
		mGridlinePaint.setStrokeWidth(1);
		
		mCenterPaint = new Paint();
		mCenterPaint.setARGB (0xFF,0x80,0x80,0x80);
		mCenterPaint.setStyle(Paint.Style.STROKE);
		mCenterPaint.setStrokeWidth(1);
		
		// Create the paint for the readings
		recreateReadingPaints();

		// Max range storage
		recreateMaximumExtentArray();

	}

	protected void recreateReadingPaints() {
		mSeriesPaints = new Paint[mSeriesCount];
		for (int i=0; i<mSeriesCount; i++) {
			mSeriesPaints[i] = new Paint();
			mSeriesPaints[i].setStyle(Paint.Style.STROKE);
			mSeriesPaints[i].setStrokeWidth(1);
			// Antialiasing doesn't look so good, so we don't use it
			// 		mGraphPaints[i].setAntiAlias(true);
		}		
		generateDefaultSeriesColors();
	}
	
	protected void recreateMaximumExtentArray() {
		//~ Log.d("GraphViewBase", String.format("recreateMaxRange with %d", mGraphCount));
		float[] newMaxRange = new float[mSeriesCount];
		if (mMaximumYExtent != null) {
			for (int i=0; i<mSeriesCount; i++) {
				if (i<mMaximumYExtent.length) {
					newMaxRange[i] = mMaximumYExtent[i];
				} else {
					newMaxRange[i] = 1;
				}
			}
		}
		mMaximumYExtent = newMaxRange; 
	}
	
	protected void generateDefaultSeriesColors() {
		//~ Log.d("GraphViewBase", String.format("generateDefaultGraphColors()"));
		DefaultColorIterator dci = new DefaultColorIterator();
		for (int idxColor = 0; idxColor<mSeriesPaints.length; idxColor++) {
			mSeriesPaints[idxColor].setColor(dci.getNext());
		}			
	}
	
	protected void onSizeChanged (int w, int h, int oldw, int oldh) {
		//~ Log.d("GraphViewBase", String.format("onSizeChanged(%d,%d,%d,%d", w, h, oldw, oldh));
		mWidth = w;
		mHeight = h;
		
	}
	
	protected void destroyGrid() {
		mGridCanvas = null;
		mGridBitmap = null;
	}

	protected void ensureGridExists() {
		if (mGridBitmap==null) {
			mGridBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
			mGridCanvas = new Canvas();
			mGridCanvas.setBitmap(mGridBitmap);
			mGridBitmap.eraseColor(Color.TRANSPARENT);
			
			GraphTickMarks gtm = new GraphTickMarks(-mMaximumYExtent[0], mMaximumYExtent[0], (int)(mHeight/DEFAULT_GRID_SIZE), false);
			mGridLogicalSize = gtm.tickSpacing();
			mGridScreenWidth = mHeight/(gtm.graphMax() - gtm.graphMin()) * mGridLogicalSize;
			mGridLegendDecimals = gtm.fractionalDigits();
			
			//~ Log.d("GraphViewBase", String.format("recreateGrid: mGridLogicalSize %f, mGridScreenWidth %f, max-min: %f/%f ", mGridLogicalSize, mGridScreenWidth, gtm.graphMax(), gtm.graphMin())); 
			
			drawGrid(mGridCanvas);
		}
	}
	
	protected void drawGrid(Canvas canvas) {
		// Draw the grid
		
		// Vertical lines
		for (float x=mGridScreenWidth;x<mWidth;x+=mGridScreenWidth) {
			canvas.drawLine(x, 0, x, mHeight, mGridlinePaint);
		}
		
		// Center line
		float nCenter = mHeight/2;
		canvas.drawLine(0, nCenter, mWidth, nCenter, mCenterPaint);
		
		// Horizontal lines
		float[] y = {nCenter-mGridScreenWidth, nCenter+mGridScreenWidth};
		while (y[0]>0) {		// Positive y is down
			canvas.drawLine(0, y[0], mWidth, y[0], mGridlinePaint);
			canvas.drawLine(0, y[1], mWidth, y[1], mGridlinePaint);
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
		//~ Log.d("GraphViewBase", "draw()");  

		// Draw the grid from off-screen bitmap
		ensureGridExists();
		canvas.drawBitmap(mGridBitmap, 0, 0, null);
		
	}

	public void setMaxYExtent(int readingIndex, float maxRange) {
		if (readingIndex==0)
			destroyGrid();
		mMaximumYExtent[readingIndex] = maxRange;
	}
	
	public void setSeriesCount(int value) {
		mSeriesCount = value;
		recreateReadingPaints();
		recreateMaximumExtentArray();
		clear();
	}
	
	public void setSeriesColor(int idx, int a, int r, int g, int b) {
		mSeriesPaints[idx].setARGB(a, r, g, b);
	}
	
	public void setSeriesColor(int idx, int color) {
		mSeriesPaints[idx].setColor(color);
	}
	
	public void clear() {
		// To be implemented by the derived classes
	}
	
	public void addReading(int readingIndex, float readingValue, long timestamp) {
		// To be implemented by the derived classes
	}
	
}

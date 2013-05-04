package com.atlarge.motionlog;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Base glass for drawing graphs
*/ 
public class GraphViewBase extends View {
	
	/** Color used to draw the grid labels */
	private static final int COLOR_GRIDLABEL = 0xFFFFFFFF;
	
	/** Color used to draw the grid labels */
	private static final int COLOR_XAXIS = 0xFF808080;

	/** Color used to draw the grid labels */
	private static final int COLOR_GRIDLINE = 0xFF202020;

	/** Color used to draw the border */
	private static final int COLOR_BORDER = 0xFF404040;
	
	
	/** Default size of the grid, in dp */
	protected final float DEFAULT_GRID_SIZE = 25;
	
	/** Number of series to create by default */
	protected final int DEFAULT_SERIES_COUNT	= 1;
	/** Number of series to display on the graph */
	protected int mSeriesCount = DEFAULT_SERIES_COUNT;

	/** Array of paints use to draw each series */
	protected Paint[] mSeriesPaints;
	
	/** Size of the text used to draw the grid labels */
	private static final float GRIDLABEL_SIZE = 12;
	
	
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
	
	
	/********************************************************************/
	/* Construction and destruction */
	
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

	/********************************************************************/
	/* Initialization and de-initialization */

	/**
	 * Main initialization routine: called from constructors
	 */
	protected void init(AttributeSet attrs, int defStyle) {
		// We don't use attributes, so this is not done
		// Load attributes
		//		final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.GraphView, defStyle, 0);
		//		a.recycle();

		
		// Create the paint for the readings
		recreateSeriesPaints();

		// Max range storage
		recreateMaximumExtentArray();

	}

	/**
	 * Dumps and creates the paints used to draw the series
	 */
	protected void recreateSeriesPaints() {
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
	
	/**
	 * Creates an array of maximum Y extents (need one for each series)
	 */
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
	
	/**
	 * Generates the default colors for each series
	 */
	protected void generateDefaultSeriesColors() {
		//~ Log.d("GraphViewBase", String.format("generateDefaultGraphColors()"));
		DefaultColorIterator dci = new DefaultColorIterator();
		for (int idxColor = 0; idxColor<mSeriesPaints.length; idxColor++) {
			mSeriesPaints[idxColor].setColor(dci.getNext());
		}			
	}

	/********************************************************************/
	/* Property access and other interface methods */
	
	/**
	 * Sets the maximum extent for a given series
	 */
	public void setMaxYExtent(int readingIndex, float maxRange) {
		if (readingIndex==0)
			destroyGrid();
		mMaximumYExtent[readingIndex] = maxRange;
	}
	
	/**
	 * Sets the number of series that we will draw
	 */
	public void setSeriesCount(int value) {
		mSeriesCount = value;
		recreateSeriesPaints();
		recreateMaximumExtentArray();
		clear();
	}
	
	/**
	 * Sets the color used to draw the series
	 */
	public void setSeriesColor(int idx, int a, int r, int g, int b) {
		mSeriesPaints[idx].setARGB(a, r, g, b);
	}
	
	/**
	 * Sets the color used to draw the series
	 */
	public void setSeriesColor(int idx, int color) {
		mSeriesPaints[idx].setColor(color);
	}
	
	/**
	 * Clear series data
	 */
	public void clear() {
		// To be implemented by the derived classes
	}
	
	/**
	 * Add a datapoint in the series
	 */
	public void addValue(int seriesIndex, float value, long timestamp) {
		// To be implemented by the derived classes
	}
	
	
	/********************************************************************/
	/* Grid initialization, de-initialization, and drawing */
	
	/**
	 * Deletes the offline grid bitmap so it will redrawn on the next draw() call
	 */
	protected void destroyGrid() {
		mGridCanvas = null;
		mGridBitmap = null;
	}

	/**
	 * Creates the grid if it doesn't exist yet
	 */
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
	
	/**
	 * Draws the grid onto the specified canvas
	 */
	protected void drawGrid(Canvas canvas) {
		// Draw the grid

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(1);
		
		// Center line
		/** Paint used to draw the horizontal zero line (x-axis) */
		paint.setColor(COLOR_XAXIS);
		float nCenter = mHeight/2;
		canvas.drawLine(0, nCenter, mWidth, nCenter, paint);
		
		// Vertical lines lines
		paint.setColor(COLOR_GRIDLINE);
		for (float x=mGridScreenWidth;x<mWidth;x+=mGridScreenWidth) {
			canvas.drawLine(x, 0, x, mHeight, paint);
		}
		// Horizontal lines
		float[] y = {nCenter-mGridScreenWidth, nCenter+mGridScreenWidth};
		while (y[0]>0) {		// Positive y is down
			canvas.drawLine(0, y[0], mWidth, y[0], paint);
			canvas.drawLine(0, y[1], mWidth, y[1], paint);
			y[0] -= mGridScreenWidth;
			y[1] += mGridScreenWidth;		
		}
		
		// Border
		paint.setColor(COLOR_BORDER);
		canvas.drawRect(0, 0, mWidth-1, mHeight-1, paint);
		
		// Horizontal labels
		TextPaint textPaint = new TextPaint();
		textPaint.setColor(COLOR_GRIDLABEL);
		textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		textPaint.setTextAlign(Paint.Align.LEFT);
		textPaint.setTextSize(GRIDLABEL_SIZE);
		Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
		y[0] = nCenter-mGridScreenWidth; y[1] = nCenter+mGridScreenWidth;  
		float logicalLabel = 0;
		logicalLabel += mGridLogicalSize;
		while (y[0]>=0) {		// Positive y is down
			// Draw the tick labels
			StringBuilder sb = new StringBuilder(String.format("%+." + ((Integer)(mGridLegendDecimals)).toString() + "f", logicalLabel));
			canvas.drawText(sb.toString(), GRIDLABEL_SIZE/2, y[0]+fontMetrics.bottom, textPaint);
			sb.setCharAt(0, '-');
			canvas.drawText(sb.toString(), GRIDLABEL_SIZE/2, y[1]+fontMetrics.bottom, textPaint);
			y[0] -= mGridScreenWidth*2;
			y[1] += mGridScreenWidth*2;		
			logicalLabel += mGridLogicalSize*2;
		}
	}
	
	
	/********************************************************************/
	/* Event-responding methods (excluding draw) */

	/**
	 * Called when the size of the View changes
	 * 
	 * We have to dump stored information that depends on the view size, 
	 * such as the generated grid
	 */
	protected void onSizeChanged (int w, int h, int oldw, int oldh) {
		//~ Log.d("GraphViewBase", String.format("onSizeChanged(%d,%d,%d,%d", w, h, oldw, oldh));
		mWidth = w;
		mHeight = h;
		destroyGrid();
	}
	
	/********************************************************************/
	/* Drawing methods */

	/**
	 * Main drawing method
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		//~ Log.d("GraphViewBase", "draw()");  

		// Draw the grid from off-screen bitmap
		ensureGridExists();
		canvas.drawBitmap(mGridBitmap, 0, 0, null);
		
	}
	
}

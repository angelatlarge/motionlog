package com.atlarge.motionlog;

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

/**
 * TODO: document your custom view class.
 */
public class GraphViewBase extends View {
	protected final int DEFAULT_GRAPH_COUNT	= 2;
	protected int mGraphCount = DEFAULT_GRAPH_COUNT;
	protected String mExampleString = "Example string"; // TODO: use a default from R.string...
	protected int mExampleColor = Color.RED; // TODO: use a default from
											// R.color...
	protected Paint mGridPaint;
	protected Paint mCenterPaint;
	protected Paint[] mGraphPaints;
	protected float mExampleDimension = 0; // TODO: use a default from R.dimen...

	protected Canvas mGridCanvas;
	protected Bitmap mGridBitmap;
	
	protected TextPaint mTextPaint;
	protected float mTextWidth;
	protected float mTextHeight;
	protected float mGridSize = 30;
	
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
		final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.GraphView, defStyle, 0);

		mExampleString = a.getString(R.styleable.GraphView_exampleString);
		if (mExampleString==null)
			mExampleString = "Test string";
		mExampleColor = a.getColor(R.styleable.GraphView_exampleColor,
				mExampleColor);
//		if (mExampleColor==null)
//			mExampleColor = Color.BLACK;
		// Use getDimensionPixelSize or getDimensionPixelOffset when dealing
		// with
		// values that should fall on pixel boundaries.
		mExampleDimension = a.getDimension(
				R.styleable.GraphView_exampleDimension, mExampleDimension);

//		if (a.hasValue(R.styleable.GraphView_exampleDrawable)) {
//			mExampleDrawable = a.getDrawable(R.styleable.GraphView_exampleDrawable);
//			mExampleDrawable.setCallback(this);
//		}

		a.recycle();

		// Set up a default TextPaint object
		mTextPaint = new TextPaint();
		mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		mTextPaint.setTextAlign(Paint.Align.LEFT);

		mGridPaint = new Paint();
		mGridPaint.setARGB (0xFF,0x20,0x20,0x20);
		mGridPaint.setStyle(Paint.Style.STROKE);
		mGridPaint.setStrokeWidth(1);
		
		mCenterPaint = new Paint();
		mCenterPaint.setARGB (0xFF,0x80,0x80,0x80);
		mCenterPaint.setStyle(Paint.Style.STROKE);
		mCenterPaint.setStrokeWidth(1);
		// Update TextPaint and text measurements from attributes
		invalidateTextPaintAndMeasurements();
		
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
		}		
		generateDefaultGraphColors();
	}
	
	protected void recreateMaxRange() {
		Log.d("GraphViewBase", String.format("recreateMaxRange with %d", mGraphCount));
		float[] newMaxRange = new float[mGraphCount];
		if (mMaxRange != null) {
			for (int i=0; i<mMaxRange.length; i++) {
				if (i<mGraphCount) {
					newMaxRange[i] = mMaxRange[i];
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
		
		mGridBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		mGridBitmap.eraseColor(Color.TRANSPARENT);
		mGridCanvas = new Canvas();
		mGridCanvas.setBitmap(mGridBitmap);
		
		drawGrid(mGridCanvas);
		
	}
	
	private void invalidateTextPaintAndMeasurements() {
		mTextPaint.setTextSize(mExampleDimension);
		mTextPaint.setColor(mExampleColor);
		mTextWidth = mTextPaint.measureText(mExampleString);

		Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
		mTextHeight = fontMetrics.bottom;
	}

	protected void drawGrid(Canvas canvas) {
		// Draw the grid
		
		// Vertical lines
		for (float x=mGridSize;x<mWidth;x+=mGridSize) {
			canvas.drawLine(x, 0, x, mHeight, mGridPaint);
		}
		
		// Center line
		float nCenter = mHeight/2;
		
		// Horizontal lines
		canvas.drawLine(0, nCenter, mWidth, nCenter, mCenterPaint);
		
		float[] y = {nCenter-mGridSize, nCenter+mGridSize};
		while (y[0]>0) {
			canvas.drawLine(0, y[0], mWidth, y[0], mGridPaint);
			canvas.drawLine(0, y[1], mWidth, y[1], mGridPaint);
			y[0] -= mGridSize;
			y[1] += mGridSize;
		}
		
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Log.d("GraphViewBase", "draw()");  

		canvas.drawBitmap(mGridBitmap, 0, 0, null);
//		drawGrid(canvas);
		
	}

	/**
	 * Gets the example string attribute value.
	 * 
	 * @return The example string attribute value.
	 */
	public String getExampleString() {
		return mExampleString;
	}

	/**
	 * Sets the view's example string attribute value. In the example view, this
	 * string is the text to draw.
	 * 
	 * @param exampleString
	 *            The example string attribute value to use.
	 */
	public void setExampleString(String exampleString) {
		mExampleString = exampleString;
		invalidateTextPaintAndMeasurements();
	}

	/**
	 * Gets the example color attribute value.
	 * 
	 * @return The example color attribute value.
	 */
	public int getExampleColor() {
		return mExampleColor;
	}

	/**
	 * Sets the view's example color attribute value. In the example view, this
	 * color is the font color.
	 * 
	 * @param exampleColor
	 *            The example color attribute value to use.
	 */
	public void setExampleColor(int exampleColor) {
		mExampleColor = exampleColor;
		invalidateTextPaintAndMeasurements();
	}

	/**
	 * Gets the example dimension attribute value.
	 * 
	 * @return The example dimension attribute value.
	 */
	public float getExampleDimension() {
		return mExampleDimension;
	}

	/**
	 * Sets the view's example dimension attribute value. In the example view,
	 * this dimension is the font size.
	 * 
	 * @param exampleDimension
	 *            The example dimension attribute value to use.
	 */
	public void setExampleDimension(float exampleDimension) {
		mExampleDimension = exampleDimension;
		invalidateTextPaintAndMeasurements();
	}

	public void setMaxRange(int readingIndex, float maxRange) {
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

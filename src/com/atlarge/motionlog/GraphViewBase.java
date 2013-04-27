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

/**
 * TODO: document your custom view class.
 */
public class GraphViewBase extends View {
	protected int mGraphCount = 2;
	protected final int LINES_COUNT	= 2;
	protected String mExampleString = "Example string"; // TODO: use a default from R.string...
	protected int mExampleColor = Color.RED; // TODO: use a default from
											// R.color...
	protected Paint mGridPaint;
	protected Paint[] mReadingPaints;
	protected float mExampleDimension = 0; // TODO: use a default from R.dimen...

	protected TextPaint mTextPaint;
	protected float mTextWidth;
	protected float mTextHeight;
	protected float mGridSize = 30;
	
	protected float[] mMaxRange;
	
	protected int mWidth;
	protected int mHeight;
	
	protected Paint mTransparentPaint;
	
	
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

		// Update TextPaint and text measurements from attributes
		invalidateTextPaintAndMeasurements();
		
		// Create the paint for the readings
		mReadingPaints = new Paint[LINES_COUNT];
		for (int i=0; i<LINES_COUNT; i++) {
			mReadingPaints[i] = new Paint();
			mReadingPaints[i].setStyle(Paint.Style.STROKE);
			mReadingPaints[i].setStrokeWidth(1);
		}		
		mReadingPaints[0].setARGB (0xFF, 0xFF, 0x00, 0x00);
		mReadingPaints[1].setARGB (0xFF, 0x00, 0xFF, 0x00);
		mReadingPaints[1].setStyle(Paint.Style.FILL);

		// Max range storage
		mMaxRange = new float[LINES_COUNT]; 

	}

	protected void onSizeChanged (int w, int h, int oldw, int oldh) {
		Log.d("GraphView", String.format("onSizeChanged(%d,%d,%d,%d", w, h, oldw, oldh));
		mWidth = w;
		mHeight = h;
	}
	
	private void invalidateTextPaintAndMeasurements() {
		mTextPaint.setTextSize(mExampleDimension);
		mTextPaint.setColor(mExampleColor);
		mTextWidth = mTextPaint.measureText(mExampleString);

		Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
		mTextHeight = fontMetrics.bottom;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Log.d("GraphView", "draw()");  

		// TODO: consider storing these as member variables to reduce
		// allocations per draw cycle.
		int paddingLeft = getPaddingLeft();
		int paddingTop = getPaddingTop();
		int paddingRight = getPaddingRight();
		int paddingBottom = getPaddingBottom();

		int contentWidth = getWidth() - paddingLeft - paddingRight;
		int contentHeight = getHeight() - paddingTop - paddingBottom;

		// Draw the text.
		canvas.drawText(mExampleString, paddingLeft
				+ (contentWidth - mTextWidth) / 2, paddingTop
				+ (contentHeight + mTextHeight) / 2, mTextPaint);

		// Draw the grid
		for (float x=mGridSize;x<contentWidth;x+=mGridSize) {
			canvas.drawLine(x, 0, x, contentHeight, mGridPaint);
		}
		for (float y=mGridSize;y<contentHeight;y+=mGridSize) {
			canvas.drawLine(0, y, contentWidth, y, mGridPaint);
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
	
	public void clear() {
	}
	
	public void addReading(int readingIndex, float readingValue, long timestamp) {
	}
	
}

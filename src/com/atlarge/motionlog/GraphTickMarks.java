package com.atlarge.motionlog;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

class GraphTickMarks {
	float mRange;
	float mTickspacing;
	float mGraphmin, mGraphmax;
	int mFract;		// Number of fractional digits to show
	List<Float> mTicks;
	int mTickCount;

	GraphTickMarks(float min, float max, int ticks) {
		mRange = niceNumber(max-min, false);
		mTickspacing = niceNumber(mRange/(ticks-1), true);
		mGraphmin = (float)Math.floor(min/mTickspacing) * mTickspacing;
		mGraphmax = (float)Math.ceil(max/mTickspacing) * mTickspacing;
		mFract = (int)Math.max(-Math.floor(Math.log10(mTickspacing)), 0);
		mTicks = new LinkedList<Float>();
		float nNextTick = mGraphmin;
		mTickCount = 1;
		while (nNextTick <= mGraphmax) {
			mTicks.add(nNextTick);
			nNextTick+=mTickspacing;
			mTickCount++;
		}
	}

	public float graphMin() {
		return mGraphmin;
	}
	
	public float graphMax() {
		return mGraphmax;
	}
	
	public float tickSpacing() {
		return mTickspacing;
	}
	
	public int tickCount() {
		return mTickCount;
	}

	public Iterator<Float> ticks() {
		return mTicks.listIterator();
	}
	
	public int fractionalDigits() {
		return mFract;
	}
	
	private float niceNumber(float n, boolean round) {
		double exp = Math.floor(Math.log10(n));	//exponent of n
		double nf;							// nice, rounded fraction
		double f = n/Math.pow(10, exp);			// fractional part of n
		if (round) {
			if (f < 1.5) nf = 1.;
			else if (f < 3.) nf = 2.;
			else if (f < 7.) nf = 5.;
			else nf = 10.;
		} else {
			if (f <= 1.) nf = 1.;
			else if (f <= 2.) nf = 2.;
			else if (f < 5.) nf = 5.;
			else nf = 10.;
		}
		return (float)(nf*Math.pow(10., exp));
	}
	
	
}
package com.atlarge.motionlog;

public class DefaultColorIterator {
	private int mIncrementValue = 0x100;
	private int mIncrementMult = 1;
	private int mColorMask = 1;

	 //Color: (alpha << 24) | (red << 16) | (green << 8) | blue.
	public int getNext() {
		int result = 0;
		int[] colors = {0, 0, 0};
		int nUseValue = mIncrementValue * mIncrementMult;
		if (nUseValue > 255) nUseValue = 255;
		for (int idxBit = 0; idxBit<4; idxBit++) {
			if ((mColorMask & (0x01<<idxBit)) > 0) { colors[idxBit] = nUseValue; }
		}
		// Set the color
		result = 0xFF << 24 | colors[0]<<16 | colors[1]<<8 | colors[2];

		// Prepare for the next color
		if ((mColorMask <<= 1) > 7) {
			if ((mColorMask&(mColorMask-1)) > 0) {
				// More than one bit is set, done with this nIncrementMult
				if ((mIncrementMult+=2 * mIncrementValue) > 0x100) {
					// Next nIncrementValue
					mIncrementValue /= 2;
					mIncrementMult = 1;
				} // else: nothing to do, already incremented nIncrementMult
				mColorMask = 1;
			} else {
				mColorMask = 3;	// Do doubles
			}
		
		} //else: nothing to do, already moved on to the next color
		
		return result;
	}	
}
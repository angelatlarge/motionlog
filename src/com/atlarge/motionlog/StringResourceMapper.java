package com.atlarge.motionlog;

import java.util.HashMap;
import java.util.NoSuchElementException;

import android.util.SparseIntArray;

@SuppressWarnings("unused")
class StringResourceMapper {
//	HashMap<Integer, Integer> map;
	SparseIntArray map;
	
	StringResourceMapper(int[] stringIDs, int[] numericIDs) {
//		map = new HashMap<Integer, Integer>();
		map = new SparseIntArray();
		for (int i=0; i<Math.min(stringIDs.length, numericIDs.length); i++)
			map.put(stringIDs[i], numericIDs[i]);
	}
	
	public int toNumericId(int stringId) {
		Integer result = map.get(stringId);
		if (result==null)
			throw new NoSuchElementException(String.format("String id %d not found in mapper", stringId)); 
		return result;
	}
	public int toStringId(int numericID) {
		int idx=map.indexOfValue(numericID);
		if (idx>0) 
			return map.keyAt(idx);
/*				
		for (int stringId : map.keySet() ) {
			if (map.get(stringId)==numericID)
				return stringId;
		}
*/		
		throw new NoSuchElementException(String.format("Numeric id %d not found in mapper", numericID)); 
	}
}
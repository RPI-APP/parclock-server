package com.centeksoftware.parclock.server.tools;

import java.util.Arrays;

/**
 * A class with custom functions for dealing with arrays
 * 
 * @author Daniel Centore
 *
 */
public class ArrayTools
{
	/**
	 * Finds the median value in an array
	 * 
	 * @param a The array
	 * @return The median value
	 */
	public static long findMedian(long[] a)
	{
		long[] b = Arrays.copyOf(a, a.length);
		Arrays.sort(b);
		
		return b[b.length / 2];
	}
	
}

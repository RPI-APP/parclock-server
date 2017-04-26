package com.centeksoftware.parclock.server.time;

import org.apache.commons.net.ntp.ClientTimeSource;
import org.apache.directory.server.ntp.time.ServerTimeSource;

/**
 * Handles keeping track of monotonically and linearly increasing time
 * 
 * @author Daniel Centore
 */
public class MonotonicTime implements ClientTimeSource, ServerTimeSource
{
	private static final int MILLION = 1_000_000;
	
	/**
	 * The difference between {@link System#currentTimeMillis()} and {@link System#nanoTime()} in
	 * milliseconds
	 */
	private long milliNanoDifference;
	
	/**
	 * Starts the monotonic time counter at the current UTC time. NOTE: This does not mean that the
	 * time remains UTC. This will count leap seconds while UTC will skip over them.
	 */
	public MonotonicTime()
	{
		milliNanoDifference = System.currentTimeMillis() - (System.nanoTime() / MILLION);
	}
	
	/**
	 * Starts the monotonic time counter at a start value
	 * 
	 * @param currentTime Starting time (in ms)
	 */
	public MonotonicTime(long currentTime)
	{
		updateTime(currentTime);
	}
	
	/**
	 * Updates the current time this stores
	 * 
	 * @param currentTime current "unix" time
	 */
	public void updateTime(long currentTime)
	{
		milliNanoDifference = currentTime - (System.nanoTime() / MILLION);
	}
	
	@Override
	public long currentTimeMillis()
	{
		long linearMs = (System.nanoTime() / MILLION) + milliNanoDifference;
		return linearMs;
	}
	
}

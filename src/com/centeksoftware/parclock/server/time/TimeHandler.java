package com.centeksoftware.parclock.server.time;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.commons.net.ntp.ClientTimeSource;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.directory.server.ntp.time.ServerTimeSource;

import com.centeksoftware.parclock.server.DrLogger;
import com.centeksoftware.parclock.server.tools.ArrayTools;

import lombok.Setter;


/**
 * Handles obtaining an additional time offset from a client (if applicable) and acting as a wrapper
 * around {@link MonotonicTime} to apply said offset.
 * 
 * @author Daniel Centore
 *
 */
public class TimeHandler implements ClientTimeSource, ServerTimeSource
{
	/**
	 * How many samples are required
	 */
	private static final int TIMES = 5;
	
	/**
	 * How many ms after first connection request before giving up on getting the time from a client
	 */
	private static final long TIME_BEFORE_SURRENDER = 15 * 1000;
	
	private MonotonicTime monotonicTime;
	private long currentOffset;
	private volatile boolean haveTime = false;
	private volatile long firstSyncRequest = 0;
	
	/**
	 * The time server we're using
	 */
	@Setter
	private TimeServer timeServer;
	
	public TimeHandler()
	{
		monotonicTime = new MonotonicTime();
		
		currentOffset = 0;
	}
	
	/**
	 * Updates the local offset after performing a full synchronization with the client.
	 * 
	 * @param url
	 * @param port
	 * @return Success? False if we have the time already or lost connection during this sync
	 */
	public synchronized boolean fullSync(String url, int port)
	{
		DrLogger.println("Syncing time with [" + url + ":" + port + "]");
		
		if (haveTime)
			return false;
		
		long offsets[] = new long[TIMES];
		
		for (int i = 0; i < TIMES; ++i)
		{
			long offset = getOffset(url, port, monotonicTime);
			
			DrLogger.println("Got offset: " + offset);
			
			if (offset == Integer.MAX_VALUE)
				return false;
			
			offsets[i] = offset;
		}
		
		currentOffset = ArrayTools.findMedian(offsets);
		
		setHaveTime();
		
		return true;
	}
	
	private static long getOffset(String server, int port, ClientTimeSource timeSource)
	{
		NTPUDPClient client = new NTPUDPClient(timeSource);
		// We want to timeout if a response takes longer than 10 seconds
		client.setDefaultTimeout(10000);
		try
		{
			client.open();
			
			InetAddress hostAddr = InetAddress.getByName(server);
			TimeInfo info = client.getTime(hostAddr, port);
			
			info.computeDetails();
			long offset = info.getOffset().longValue();
			
			client.close();
			
			return offset;
			
		} catch (IOException e)
		{
			// Usually just means we lost our connection.
			e.printStackTrace();
		} catch (Exception e)
		{
			// Something unexpected happened. Let's print about it.
			e.printStackTrace();
		}
		
		client.close();
		
		return Integer.MAX_VALUE;
	}
	
	@Override
	public long currentTimeMillis()
	{
		return monotonicTime.currentTimeMillis() + currentOffset;
	}
	
	/**
	 * Indicate to ourselves that we have the time
	 */
	private synchronized void setHaveTime()
	{
		haveTime = true;
		
		if (timeServer != null && !timeServer.isStarted())
		{
			DrLogger.printOk("Have time; Starting time server");
			try
			{
				timeServer.start();
				
				DrLogger.ok();
			} catch (IOException e)
			{
				DrLogger.fail();
				DrLogger.print(e);
			}
		}
	}
	
	/**
	 * Returns true if we have the time. If we don't have the time but we're ready to give up on
	 * getting it, then also returns true. Side effects: After first call, gives us 5 minutes before
	 * giving up on getting time from a surviving client.
	 * 
	 * @return
	 */
	public boolean haveTime()
	{
		if (haveTime)
			return true;
		
		if (firstSyncRequest == 0)
			firstSyncRequest = System.currentTimeMillis();
		else if (System.currentTimeMillis() - firstSyncRequest > TIME_BEFORE_SURRENDER)
		{
			setHaveTime();
			return true;
		}
		
		return false;
	}

	public void setTimeServer(TimeServer timeServer) {
		this.timeServer = timeServer;
	}
}

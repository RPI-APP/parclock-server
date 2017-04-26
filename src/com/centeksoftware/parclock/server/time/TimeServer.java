package com.centeksoftware.parclock.server.time;
import java.io.IOException;

import org.apache.directory.server.ntp.NtpServer;
import org.apache.directory.server.ntp.time.ServerTimeSource;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.terracotta.statistics.Time.TimeSource;


/**
 * Runs an NTP time server on a given port with a given time server
 * 
 * @author Daniel Centore 
 *
 */
public class TimeServer
{
	private NtpServer ntp;

	/**
	 * Creates a new NTP {@link TimeServer}
	 * @param port The port to run it on
	 * @param timeSource The {@link TimeSource} whose time we should host
	 */
	public TimeServer(int port, ServerTimeSource timeSource)
	{
		ntp = new NtpServer(timeSource);
		ntp.addTransports(new UdpTransport(port));
	}
	
	/**
	 * Start the NTP time server
	 * @throws IOException If that didn't happen
	 */
	public void start() throws IOException
	{
		ntp.start();
	}
	
	/**
	 * Stop the NTP time server
	 */
	public void stop()
	{
		ntp.stop();
	}
	
	/**
	 * Is the time server currently running?
	 * @return
	 */
	public boolean isStarted()
	{
		return ntp.isStarted();
	}
}

package com.centeksoftware.parclock.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

import com.centeksoftware.parclock.server.time.TimeHandler;

/**
 * Handles communication with an individual relay
 * 
 * @author Daniel Centore
 *
 */
public class PersonalRelayServer
{
	private static final String FIELD_DELIM = Character.toString((char) 0x1E);
	
	/**
	 * How long to instruct a relay to wait before attempting to reconnect
	 */
	private static final int BEFORE_RECONNECT = 10_000;
	
	private Socket socket;
	private RelayServer rs;
	private TimeHandler timeHandler;
	private int ntpPortClient;
	private Scanner scan = null;
	private DataOutputStream out = null;
	
	public PersonalRelayServer(Socket socket, RelayServer relayServer, TimeHandler timeHandler,
			int ntpPortClient)
	{
		this.socket = socket;
		this.rs = relayServer;
		this.timeHandler = timeHandler;
		this.ntpPortClient = ntpPortClient;
		
		Main.addConnection(socket.getInetAddress().getHostAddress());
	}
	
	/**
	 * IP address of this connection (or "NULL" if something went wrong)
	 * 
	 * @return
	 */
	public String ip()
	{
		try
		{
			return (socket == null ? "NULL" : socket.getInetAddress().getHostAddress());
		} catch (Exception e)
		{
			return "NULL";
		}
	}
	
	public void start()
	{
		new Thread()
		{
			public void run()
			{
				PersonalRelayServer.this.run();
			}
		}.start();
	}
	
	public void stopIndivServer()
	{
		// TODO
		System.out.println("Stopping socket...");
		
		try
		{
			if (scan != null)
				scan.close();
			
			if (out != null)
				out.close();
			
			if (socket != null)
				socket.close();
			
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void run()
	{
		try (Scanner scan = new Scanner(socket.getInputStream());)
		{
			while (scan.hasNext())
			{
				String line = scan.nextLine();
				String parts[] = line.split(FIELD_DELIM);
				
				if (parts.length != 3)
					continue;
				
				Main.submitData(parts[0], parts[1], Long.parseLong(parts[2]));
			}
			
			scan.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
		rs.labelStopped(this);
		Main.removeConnection(socket.getInetAddress().getHostAddress());
	}
	
	/**
	 * Performs the initial handshaking procedure with the relay where we decide how time should be
	 * exchanged.
	 * 
	 * @return True if we should begin normal operation. False if we should disconnect from the
	 *         relay. False is not necessarily an error condition and may just be because the relay
	 *         was instructed to perform the handshaking procedure again.
	 */
	public boolean handshake()
	{
		DrLogger.println(socket, "Performing handshake:");
		
		try
		{
			scan = new Scanner(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e1)
		{
			DrLogger.print(e1);
			return false;
		}
		
		try
		{
			// Client sends protocol version
			int protocolVersion = Integer.parseInt(scan.nextLine());
			
			DrLogger.println(socket, "Client's protocol version: [" + protocolVersion + "]");
			
			if (protocolVersion != Main.PROTOCOL_VERSION)
			{
				DrLogger.printOk(socket, "PROTOCOL CONFLICT. Sending notice");
				out.writeBytes("0\n");
				DrLogger.ok();
				return false;
			}
			
			DrLogger.printOk(socket, "Protocol matches. Sending confirmation");
			out.writeBytes("1\n");
			DrLogger.ok();
			
			// Client has time?
			
			String temp = scan.nextLine();
			boolean clientHasTime = temp.equals("1");
			
			DrLogger.println(socket, "Client has time?: [" + clientHasTime + "]");
			
			// Do we have the time?
			
			boolean weHaveTime = timeHandler.haveTime();
			
			DrLogger.println(socket, "We have time?: [" + weHaveTime + "]");
			
			if (weHaveTime)
			{
				// Tell client we don't need it
				out.writeBytes("0\n");
				
				DrLogger.println(socket, "Told client don't need time.");
				
				// Handshake complete. Begin normal operation.
				return true;
			}
			else
			{
				// Tell client we need it
				out.writeBytes("1\n");
				
				DrLogger.println(socket, "Told client we need time.");
				
				if (clientHasTime)
				{
					DrLogger.printOk(socket, "Waiting for permission to sync time");
					
					// Relay sends "1"
					// This is the signal for when we may connect to the relay's newly created time
					// server
					scan.nextLine();
					
					DrLogger.ok();
					
					DrLogger.println(socket, "Syncing time");
					
					// Sync with client time server
					boolean success = timeHandler.fullSync(
							socket.getInetAddress().getHostAddress(), ntpPortClient);
					
					if (!success)
						return false;
					
					DrLogger.println(socket, "Telling client to reconnect without delay.");
					
					// Tell client to reconnect right away
					out.writeBytes("0\n");
					
					return false;
				}
				else
				{
					// Tell client to try reconnecting in a few seconds
					DrLogger.println(socket, "Telling client to reconnect in " + BEFORE_RECONNECT
							/ 1000 + " secs");
					
					out.writeBytes(BEFORE_RECONNECT + "\n");
					
					return false;
				}
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
}

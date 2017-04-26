package com.centeksoftware.parclock.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import com.centeksoftware.parclock.server.time.TimeHandler;

/**
 * Handles waiting for a relay and initial communication with it
 * 
 * @author Daniel Centore
 *
 */
public class RelayServer
{
	private ServerSocket welcomeSocket = null;
	private ArrayList<PersonalRelayServer> runningServers = new ArrayList<>();
	private TimeHandler timeHandler;
	private int ntpPortClient;
	
	public RelayServer(int relayPort, TimeHandler timeHandler, int ntpPortClient)
	{
		this.timeHandler = timeHandler;
		this.ntpPortClient = ntpPortClient;
		
		try
		{
			welcomeSocket = new ServerSocket(relayPort);
		} catch (IOException e1)
		{
			DrLogger.print(e1);
			System.exit(-1);
			return;
		}
	}
	
	public void start()
	{
		new Thread()
		{
			public void run()
			{
				RelayServer.this.run();
			}
		}.start();
	}
	
	public void stopServer()
	{
		System.currentTimeMillis();
		try
		{
			DrLogger.printOk("Stopping welcome socket");
			
			welcomeSocket.close();
			
			DrLogger.ok();
		} catch (IOException e)
		{
			DrLogger.fail();
			DrLogger.print(e);
		}
		
		for (PersonalRelayServer ris : runningServers)
		{
			try
			{
				DrLogger.printOk("Telling server [" + ris.ip() + "] to stop");
				
				ris.stopIndivServer();
				
				DrLogger.ok();
			} catch (Exception e)
			{
				DrLogger.fail();
				DrLogger.print(e);
			}
		}
		
		DrLogger.printOk("Waiting for all servers to stop");
		
		while (!runningServers.isEmpty())
		{
			try
			{
				Thread.sleep(5);
			} catch (InterruptedException e)
			{
			}
		}
		
		DrLogger.ok();
	}
	
	public void labelStopped(PersonalRelayServer ris)
	{
		runningServers.remove(ris);
	}
	
	private void run()
	{
		while (true)
		{
			try
			{
				// Cleanup nicely if the program is shutting down
				if (welcomeSocket == null || welcomeSocket.isClosed() || Main.shuttingDown())
					return;
				
				DrLogger.println("Patiently awaiting next connection");
				
				Socket socket = welcomeSocket.accept();
				
				DrLogger.println(socket, "New connection!");
				
				PersonalRelayServer ris = new PersonalRelayServer(socket, this, timeHandler,
						ntpPortClient);
				
				boolean handshake = ris.handshake();
				
				if (!handshake)
				{
					DrLogger.printOk(socket, "Nuking connection...");
					socket.close();
					DrLogger.ok();
					continue;
				}
				
				ris.start();
				runningServers.add(ris);
				
				DrLogger.println(socket, "Started regular data connection");
				
			} catch (IOException e)
			{
				// This error is expected if we are shutting down
				if (Main.shuttingDown())
					return;
				else
					e.printStackTrace();
			}
		}
	}
}

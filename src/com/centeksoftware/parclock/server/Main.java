package com.centeksoftware.parclock.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;

import com.centeksoftware.parclock.server.time.TimeHandler;
import com.centeksoftware.parclock.server.time.TimeServer;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

import lombok.SneakyThrows;

/**
 * 
 * @author Daniel Centore
 *
 */
public class Main
{
	/**
	 * Current version of the protocol. Only relays with a matching version will be permitted to
	 * connect.
	 */
	public static final int PROTOCOL_VERSION = 12;
	
	/**
	 * MySQL Username
	 */
	private static String username = null;
	
	/**
	 * MySQL Password
	 */
	private static String password = null;
	
	/**
	 * JDBC connection string
	 */
	private static String connection = null;
	
	/**
	 * The NTP port on this server that we open for the relay to connect to
	 */
	private static int ntpPortServer = -1;
	
	/**
	 * The NTP port that the relay opens for us to connect to
	 */
	private static int ntpPortClient = -1;
	
	/**
	 * The main data communication port
	 */
	private static int relayPort = -1;
	
	private static Connection conn = null;
	private static int waitingForUpdate = 0; // Number of transactions waiting for a commit
	private static long lastCommit = -1;
	private static int betweenUpdates = 200;
	private static TimeServer timeServer;
	
	private static volatile boolean shuttingDown = false;
	
	// How long to wait for everything to be committed to DB
	private static final long SHUTDOWN_TIMEOUT = 5000;
	
	public static boolean shuttingDown()
	{
		return shuttingDown;
	}
	
	public static void main(String[] args)
			throws ClassNotFoundException, SQLException, FileNotFoundException
	{
		// Tell log4j to shut up
		Logger.getRootLogger().removeAllAppenders();
		Logger.getRootLogger().addAppender(new NullAppender());
		
		// == Load info from the config file == //
		DrLogger.printOk("Loading configuration file");
		
		Scanner scan = new Scanner(new File("./config.txt"));
		
		while (scan.hasNext())
		{
			String s = scan.nextLine();
			if (s.startsWith("#"))
				continue;
			
			String p[] = s.split(":");
			
			String param = p[0];
			String value = "";
			for (int i = 1; i < p.length; ++i)
				value += p[i] + (i == p.length - 1 ? "" : ":");
			value = value.trim();
			
			if (param.equalsIgnoreCase("username"))
				username = value;
			else if (param.equalsIgnoreCase("password"))
				password = value;
			else if (param.equalsIgnoreCase("connection"))
				connection = value;
			else if (param.equalsIgnoreCase("ntpPortServer"))
				ntpPortServer = Integer.parseInt(value);
			else if (param.equalsIgnoreCase("ntpPortClient"))
				ntpPortClient = Integer.parseInt(value);
			else if (param.equalsIgnoreCase("relayPort"))
				relayPort = Integer.parseInt(value);
			else if (param.equalsIgnoreCase("betweenCommits"))
				betweenUpdates = Integer.parseInt(value);
		}
		
		scan.close();
		
		DrLogger.ok();
		
		TimeHandler timeHandler = new TimeHandler();
		
		timeServer = new TimeServer(ntpPortServer, timeHandler);
		
		timeHandler.setTimeServer(timeServer);
		
		DrLogger.printOk("Connecting to database");
		try
		{
			Database.init(connection, username, password);
		} catch (FileNotFoundException e)
		{
			DrLogger.fail();
			DrLogger.print(e);
			System.exit(-1);
		}
		
		conn = Database.getConnection();
		conn.setAutoCommit(false);
		
		DrLogger.ok();
		
		// ===\/
		DrLogger.printOk("Starting relay server");
		
		final RelayServer rs = new RelayServer(relayPort, timeHandler, ntpPortClient);
		rs.start();
		
		DrLogger.ok();
		// ===/\
		
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				// Initiate shutdown sequence
				Main.shuttingDown = true;
				
				// Stop the NTP server
				timeServer.stop();
				
				// Stop the server
				rs.stopServer();
				
				// Wait for everything to be committed to the database or for a timeout of 30 secs
				long startTime = System.currentTimeMillis();
				while (waitingForUpdate > 0
						&& System.currentTimeMillis() - startTime < SHUTDOWN_TIMEOUT)
				{
					Main.sleep(5);
				}
			}
		});
		
		DrLogger.println("Server Ready");
		
		while (true)
		{
			updateTime(timeHandler.currentTimeMillis());
			
			if (waitingForUpdate > 0 && (lastCommit < 0
					|| System.currentTimeMillis() - lastCommit >= betweenUpdates))
			{
				DrLogger.printOk("Committing " + waitingForUpdate + " updates");
				
				lastCommit = System.currentTimeMillis();
				conn.commit();
				
				waitingForUpdate = 0;
				
				DrLogger.ok();
			}
			
			try
			{
				Thread.sleep(betweenUpdates / 2);
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static void log(String text)
	{
		String query = "INSERT INTO `log` (`text`) VALUES (?);";
		try (PreparedStatement statement = conn.prepareStatement(query);)
		{
			statement.setString(1, text);
			
			statement.execute();
			
			conn.commit();
		} catch (SQLException e)
		{
			DrLogger.print(e);
			
			return;
		}
	}
	
	public static void addConnection(String IP)
	{
		String query = "INSERT INTO `current_clients` (`ip`) VALUES (?);";
		try (PreparedStatement statement = conn.prepareStatement(query);)
		{
			statement.setString(1, IP);
			
			statement.execute();
			
			conn.commit();
		} catch (MySQLIntegrityConstraintViolationException e)
		{
			// Happens when we add duplicate connection id
			// e.printStackTrace();
			DrLogger.println(IP, "Duplicate connection ID added");
		} catch (SQLException e)
		{
			DrLogger.print(e);
			return;
		}
	}
	
	public static void removeConnection(String IP)
	{
		String query = "DELETE FROM `current_clients` WHERE (`ip`) = (?);";
		try (PreparedStatement statement = conn.prepareStatement(query);)
		{
			statement.setString(1, IP);
			
			statement.execute();
			
			conn.commit();
		} catch (SQLException e)
		{
			DrLogger.print(e);
			
			return;
		}
	}
	
	public static boolean updateTime(long timeMs)
	{
		String query = "UPDATE information SET `value` = (?) WHERE `title`=\"clock\"";
		try (PreparedStatement statement = conn.prepareStatement(query);)
		{
			statement.setLong(1, timeMs);
			
			statement.execute();
			
			++waitingForUpdate;
		} catch (SQLException e)
		{
			DrLogger.print(e);
			
			return false;
		}
		
		return true;
	}
	
	public static boolean submitData(String fieldUuid, String data, long timestampMs)
	{
		// == Obtain field info from the UUID == //
		String query = "SELECT id, name, type, enabled FROM data_field WHERE guid=? LIMIT 1";
		
		int fieldId;
		int fieldType;
		// boolean enabled = false; // Is the field enabled?
		// String name = null; // Friendly field name
		
		try (PreparedStatement statement = conn.prepareStatement(query);)
		{
			statement.setString(1, fieldUuid);
			
			try (ResultSet results = statement.executeQuery())
			{
				if (!results.first())
				{
					DrLogger.println("No field with UUID: [" + fieldUuid + "]");
					
					return false;
				}
				
				fieldId = results.getInt("id");
				fieldType = results.getInt("type");
				// enabled = (results.getInt("enabled") == 1 ? true : false);
				// name = results.getString("name");
			}
		} catch (SQLException e)
		{
			DrLogger.print(e);
			
			return false;
		}
		
		/*
		 * Note: The following code doesn't check if the instrument was disabled, just the field
		 * itself. It was decided to just let the enable/disable marker be nothing more. Disabling a
		 * field doesn't mean that you can no longer submit data or view data. It just moves it to
		 * the bottom of the list.
		 */
		
		// if (!enabled)
		// {
		// error(errorName, "Field must be enabled in order to insert new data Name:[" + name +
		// "] Uuid:[" + fieldUuid + "]", true);
		// return false;
		// }
		
		// == Insert the data == //
		String dataType = null;
		if (fieldType == 0)
			dataType = "int";
		else if (fieldType == 1)
			dataType = "float";
		else if (fieldType == 2)
			dataType = "string";
		
		query = "INSERT INTO `data_type_" + dataType
				+ "` (`data_field`, `data`, `timestamp`) VALUES (?, ?, ?);";
		try (PreparedStatement statement = conn.prepareStatement(query);)
		{
			statement.setInt(1, fieldId);
			
			if (fieldType == 0)
				statement.setInt(2, Integer.parseInt(data));
			else if (fieldType == 1)
				statement.setDouble(2, Double.parseDouble(data));
			else if (fieldType == 2)
				statement.setString(2, data);
			
			statement.setLong(3, timestampMs);
			
			statement.execute();
			
			++waitingForUpdate;
		} catch (SQLException e)
		{
			DrLogger.print(e);
			
			return false;
		}
		
		return true;
	}
	
	// Just so we can be lazy and not deal with a checked exception we're
	// never going to encounter.
	@SneakyThrows
	public static void sleep(long millis)
	{
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
	}
}

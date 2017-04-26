package com.centeksoftware.parclock.server;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;

/**
 * 
 * @author Daniel Centore
 *
 */
public class Database
{
	private static final BasicDataSource dataSource = new BasicDataSource();
	
	public static void init(String connection, String username, String password)
			throws FileNotFoundException
	{
		dataSource.setDriverClassName("com.mysql.jdbc.Driver");
		
		dataSource.setUrl(connection);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
	}
	
	public static Connection getConnection() throws SQLException
	{
		return dataSource.getConnection();
	}
}

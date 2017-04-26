package com.centeksoftware.parclock.server;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;

public class DrLogger
{
	public static String toString(Exception e)
	{
		StringWriter errors = new StringWriter();
		e.printStackTrace(new PrintWriter(errors));
		
		return errors.toString();
	}
	
	public static void println(String message)
	{
		_print("[SERVER]: " + message, true);
	}
	
	public static void printOk(String message)
	{
		_print("[SERVER]: " + message + "...", false);
	}
	
	public static void ok()
	{
		_print(" [OK]", true);
	}
	
	public static void fail()
	{
		_print(" [FAIL]", true);
	}
	
	public static void println(Socket socket, String message)
	{
		_print("[" + socket.getInetAddress().getHostAddress() + "]: " + message, true);
	}
	
	public static void printOk(Socket socket, String message)
	{
		_print("[" + socket.getInetAddress().getHostAddress() + "]: " + message + "...", false);
	}
	
	public static void print(Exception e)
	{
		_print("", true);
		_print(toString(e), true);
	}
	
	public static void print(Socket socket, String message)
	{
		print(socket.getInetAddress().getHostAddress(), message);
	}
	
	private static void print(String hostAddress, String message)
	{
		_print("[" + hostAddress + "]: " + message, false);
	}
	
	public static void println(String hostAddress, String message)
	{
		_print("[" + hostAddress + "]: " + message, true);
	}
	
	public static synchronized void _print(String s, boolean newline)
	{
		String text = s;
		
		if (newline)
			text += "\n";
		
		System.out.print(text);
	}
}

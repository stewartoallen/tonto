/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.*;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.StringTokenizer;

public final class Debug
{
	// ---( static fields )---
	private static Hashtable buggers = new Hashtable();
	private static PrintStream out = System.out;
	private static BufferedWriter log = null;
	private static Thread flusher;
	private static Thread hook;
	private static Vector listeners;

	public static void setLevels(String str)
	{
		if (str != null)
		{
			StringTokenizer st = new StringTokenizer(str,",");
			while (st.hasMoreTokens())
			{
				StringTokenizer st2 = new StringTokenizer(st.nextToken(),"=");
				String nm = st2.nextToken();
				int val = st2.hasMoreTokens() ?
					Integer.parseInt(st2.nextToken()) : 1;
				getInstance(nm).setLevel(val);
			}
		}
	}

	public static String getLevels()
	{
		StringBuffer sb = new StringBuffer();
		for (Enumeration e = buggers.keys(); e.hasMoreElements(); )
		{
			if (sb.length() > 0)
			{
				sb.append(",");
			}
			String nm = (String)e.nextElement();
			Debug d = (Debug)buggers.get(nm);
			sb.append(nm+"="+d.getLevel());
		}
		return sb.toString();
	}

	public static int getActiveLoggers()
	{
		return buggers.size();
	}

	public static Enumeration getLoggerNames()
	{
		return buggers.keys();
	}

	static
	{
		setLevels(System.getProperty("debug"));
	}

	// ---( static methods )---
	public static Debug getInstance(Class cname)
	{
		String nm = cname.getName();
		int idx = nm.lastIndexOf('$');
		if (idx < 0)
		{
			idx = nm.lastIndexOf('.');
		}
		if (idx >= 0)
		{
			nm = nm.substring(idx+1);
		}
		return getInstance(nm);
	}

	public static Debug getInstance(String name)
	{
		Debug d = (Debug)buggers.get(name);
		if (d == null)
		{
			d = new Debug(name, 0);
			buggers.put(name, d);
		}
		return d;
	}

	public static void setLevel(Class cname, int level)
	{
		getInstance(cname).setLevel(level);
	}

	public static void setLevel(String name, int level)
	{
		getInstance(name).setLevel(level);
	}

	// ---( constructors )---
	public Debug (String name, int dlvl)
	{
		this.name = name;
		this.level = dlvl;
	}

	// ---( instance fields )---
	private String name;
	private int level;

	// ---( instance methods )---
	public void setLevel(int level)
	{
		this.level = level;
	}

	public int getLevel()
	{
		return level;
	}

	public boolean debug(int lvl)
	{
		return (lvl <= level);
	}

	private static long lastLog = -1;

	public void log(int lvl, String msg)
	{
		if (lvl <= level)
		{
			String tm = Long.toString(System.currentTimeMillis() % 10000);
			/*
			String tm;
			long time = System.currentTimeMillis();
			if (lastLog >= 0)
			{
				tm = Long.toString(time - lastLog);
			}
			else
			{
				tm = Long.toString(0);
			}
			lastLog = time;
			*/
			if (tm.length() < 5)
			{
				tm = "00000".substring(tm.length())+tm;
			}
			log(tm +" ["+name+"] "+msg);
		}
	}

	private synchronized static void log(String msg)
	{
		if (log == null)
		{
			out.println(msg);
		}
		else
		{
			try
			{
				log.write(msg);
				log.newLine();
			}
			catch (Exception ex)
			{
				closeLog();
				out.println(msg);
			}
		}
		if (listeners != null)
		{
			for (int i=0; i<listeners.size(); i++)
			{
				((DebugListener)listeners.get(i)).debugAction(msg);
			}
		}
	}

	private static void closeLog()
	{
		try
		{
			if (flusher != null)
			{
				flusher.interrupt();
			}
			if (log != null)
			{
				log.flush();
				log.close();
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		log = null;
	}

	public static void logToFile(String file)
		throws IOException
	{
		if (log != null)
		{
			closeLog();
		}
		log = new BufferedWriter(new FileWriter(file), 8192);
		hook = new Thread() {
			public void run() {
				closeLog();
			}
		};
		Runtime.getRuntime().addShutdownHook(hook);
		flusher = new Thread() {
			public void run() {
				while (true)
				try
				{
					sleep(1000);
					log.flush();
				}
				catch (Exception ex)
				{
					//log("file flusher exited");
					return;
				}
			}
		};
		flusher.start();
	}

	public static void logToStream(PrintStream ps)
	{
		if (ps == null)
		{
			throw new IllegalArgumentException("null");
		}
		closeLog();
		out = ps;
	}

	public static void addListener(DebugListener l)
	{
		if (listeners == null)
		{
			listeners = new Vector();
		}
		if (!listeners.contains(l))
		{
			listeners.add(l);
		}
	}

	public static void removeListener(DebugListener l)
	{
		if (listeners == null || !listeners.contains(l))
		{
			return;
		}
		listeners.remove(l);
		if (listeners.size() == 0)
		{
			listeners = null;
		}
	}

	public static void removeAllListeners()
	{
		if (listeners != null)
		{
			listeners.setSize(0);
		}
	}

	// ---( interface methods )---

}


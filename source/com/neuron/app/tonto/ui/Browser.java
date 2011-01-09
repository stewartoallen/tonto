/*
 * Copyright 2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto.ui;

// ---( imports )---
import java.io.*;
import java.net.*;
import java.text.*;
import java.lang.reflect.*;
import com.neuron.app.tonto.Util;

public final class Browser
{
	// ---( static fields )---
	private static String exec[] = null;
	private static String clzNSW = "com.apple.cocoa.application.NSWorkspace";
	private static String clzMRJ = "com.apple.mrj.MRJFileUtils";
	private static File macNSW = new File(
		"/System/Library/Java/com/apple/cocoa/application/NSWorkspace.class"
	);
	private static Method methNSW, methMRJ;
	private static Object argNSW;

	// ---( static methods )---
	static
	{
		if (Util.onWindows())
		{
			exec = new String[] { "rundll32 url.dll,FileProtocolHandler {0}" };
		}
		else
		if (Util.onMacintosh())
		{
			try
			{
				Class nsw;
				if (macNSW.exists())
				{
					ClassLoader cl = new URLClassLoader(
						new URL[] { new File("/System/Library/Java").toURL() }
					);
					nsw = Class.forName(clzNSW, true, cl);
				}
				else
				{
					nsw = Class.forName(clzNSW);
				}
				Method sws = nsw.getMethod("sharedWorkspace", new Class[0]);
				argNSW = sws.invoke(null, new Object[0]);
				methNSW = nsw.getMethod("openURL", new Class[] { java.net.URL.class }); 
				Class mrj = Class.forName(clzMRJ);
				methMRJ = mrj.getMethod("openURL", new Class[] { String.class });
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		else
		{
			if (tryProcess("which mozilla"))
			{
				exec = new String[] {
					"mozilla -remote openURL({0})",
					"mozilla {0}",
				};
			}
			else
			if (tryProcess("which netscape"))
			{
				exec = new String[] {
					"netscape -remote openURL({0})",
					"netscape {0}",
				};
			}
			else
			if (tryProcess("which xterm") && tryProcess("which lynx"))
			{
				exec = new String[] { "xterm +sb -e lynx {0}" };
			}
		}
	}

	private static boolean tryProcess(String cmd)
	{
		try
		{
			return Runtime.getRuntime().exec(cmd).waitFor() == 0;
		}
		catch (Exception ex)
		{
			return false;
		}
	}

	public static void displayURL(String u)
	{
		final String url = u;

		new Thread() { public void run() {

		if (exec == null)
		{
			if (!Util.onMacintosh())
			{
				return;
			}
			try
			{
				if (methNSW != null)
				{
					if ( ((Boolean)methNSW.invoke(argNSW, new Object[] { new URL(url) })).booleanValue() )
					{
						return;
					}
				}
				if (methMRJ != null)
				{
					methMRJ.invoke(null, new Object[] { url });
				}
			}
			catch (Exception ex) { ex.printStackTrace(); }
		}
		else
		{
			for (int i=0; i<exec.length; i++)
			{
				String cmd = MessageFormat.format(exec[i], new Object[] { url });
				try {
					Process p = Runtime.getRuntime().exec(cmd);
					if (p.waitFor() == 0)
					{
						return;
					}
				} catch (Exception ex) { ex.printStackTrace(); }
			}
		}

		} }.start();
	}

	// ---( constructors )---
	private Browser ()
	{
	}

}


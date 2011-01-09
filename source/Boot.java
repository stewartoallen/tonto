/*
 * Copyright 2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

//package com.neuron;

// ---( imports )---
import java.io.*;
import java.awt.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

public class Boot
{
	public static void main(String args[])
	{
		try
		{
			URL u = Boot.class.getProtectionDomain().getCodeSource().getLocation();
			File jar = new File(unURL(u.getFile()));
			File dir = jar.getParentFile();

			// look for newest tonto jar
			String list[] = dir.list();
			File newest = null;
			for (int i=0; list != null && i < list.length; i++)
			{
				String nm = list[i].toLowerCase();
				if (nm.startsWith("tonto") && nm.endsWith(".jar"))
				{
					File nf = new File(dir, list[i]);
					if (newest == null || nf.lastModified() > newest.lastModified())
					{
						newest = nf;
					}
				}
			}

			if (newest == null)
			{
				throw new Exception("No bootable jar located");
			}

			System.out.println("Booting '"+newest+"'...");

			// create classloader, open jar, find main method
			URLClassLoader cl = new URLClassLoader(new URL[] { newest.toURL() });
			Class mainClass = cl.loadClass("Tonto");
			Method mainMethod = mainClass.getDeclaredMethod("main", new Class[] { args.getClass() });

			// launch Tonto
			mainMethod.invoke(null, new Object[] { args });
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			errorDialog("Unable to boot resource", ex);
		}
	}

	private static void errorDialog(String msg, Exception ex)
	{
		ex.printStackTrace();
	}

	public static String unURL(String url)
	{
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<url.length(); i++)
		{
			if (url.charAt(i) == '%' && i<url.length()-3)
			{
				sb.append((char)Integer.parseInt(url.substring(i+1, i+3),16));
				i += 2;
			}
			else
			{
				sb.append(url.charAt(i));
			}
		}
		return sb.toString();
	}
}


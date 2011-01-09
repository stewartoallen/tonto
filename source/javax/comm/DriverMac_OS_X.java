/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package javax.comm;

// ---( imports )---
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FilenameFilter;
import java.util.StringTokenizer;
import java.util.TooManyListenersException;

public class DriverMac_OS_X extends DriverLinux
{
	private final static String IGNORE_KEY = "javax.ignore.modem";

	public static void setIgnoreModems(boolean tf)
	{
		if (tf)
		{
			System.getProperties().put(IGNORE_KEY, "true");
		}
		else
		{
			System.getProperties().remove(IGNORE_KEY);
		}
	}

	protected String[][] getSerial()
	{
		return new String[][] {
			{"/dev", "cu."},
		};
	}

	protected boolean skipPort(File file)
	{
		return (System.getProperty(IGNORE_KEY) != null && file.toString().toLowerCase().indexOf("modem") >= 0);
	}
}


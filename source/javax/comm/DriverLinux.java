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

public class DriverLinux extends DriverGenUnix
{
	protected String[][] getSerial()
	{
		return new String[][] {
			{"/dev",     "ttyS"},
			{"/dev/usb", "ttyUSB"},
		};
	}
}


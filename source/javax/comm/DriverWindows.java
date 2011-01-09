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

public class DriverWindows extends DriverGenUnix
{
	public void run()
	{
		for (int i=0; i<12; i++)
		{
			CommPortIdentifier.addPortName("COM"+i,
				CommPortIdentifier.PORT_SERIAL, this);
		}
	}
}


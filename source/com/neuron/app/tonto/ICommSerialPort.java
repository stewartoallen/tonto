/*
 * Copyright 2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ICommSerialPort
{
	public String getName();

	public InputStream getInputStream() throws IOException;

	public OutputStream getOutputStream() throws IOException;

	public void sendBreak(int len) throws IOException;

	public void close();
}


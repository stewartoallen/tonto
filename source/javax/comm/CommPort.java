/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package javax.comm;

import java.io.*;

public abstract class CommPort
{
    protected String name;

    public void close()
    {
		try
		{
			CommPortIdentifier.getPortIdentifier(this).dropOwner();
		}
		catch (Exception ex)
		{
			//ex.printStackTrace();
			throw new RuntimeException("unexpected error on close");
		}
    }

    public abstract void disableReceiveFraming();

    public abstract void disableReceiveThreshold();

    public abstract void disableReceiveTimeout();

    public abstract void enableReceiveFraming(int framingByte)
        throws UnsupportedCommOperationException;

    public abstract void enableReceiveThreshold(int thresh)
        throws UnsupportedCommOperationException;

    public abstract void enableReceiveTimeout(int recvTimeout)
        throws UnsupportedCommOperationException;

    public abstract int getInputBufferSize();

    public abstract InputStream getInputStream()
        throws IOException;

    public String getName()
    {
        return name;
    }

    public abstract int getOutputBufferSize();

    public abstract OutputStream getOutputStream()
        throws IOException;

    public abstract int getReceiveFramingByte();

    public abstract int getReceiveThreshold();

    public abstract int getReceiveTimeout();

    public abstract boolean isReceiveFramingEnabled();

    public abstract boolean isReceiveThresholdEnabled();

    public abstract boolean isReceiveTimeoutEnabled();

    public abstract void setInputBufferSize(int size);

    public abstract void setOutputBufferSize(int size);
}


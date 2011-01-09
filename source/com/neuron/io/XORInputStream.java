/*
 * Copyright 2001-2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.io;

// ---( imports )---
import java.io.*;

public class XORInputStream extends BufferedInputStream
{
	// ---( static fields )---

	// ---( static methods )---

	// ---( constructors )---
	public XORInputStream (InputStream in, byte key[])
	{
		super(in);
		this.key = key;
		this.ptr = 0;
	}

	// ---( instance fields )---
	private byte[] key;
	private int ptr;

	// ---( instance methods )---
	public int read()
		throws IOException
	{
		int i = super.read();
		if (i >= 0)
		{
			if (ptr >= key.length)
			{
				ptr = 0;
			}
			return i ^ (key[ptr++] & 0xff);
		}
		else
		{
			return i;
		}
	}

	public int read(byte buf[], int off, int len)
		throws IOException
	{
		int r = super.read(buf, off, len);
		if (r > 0)
		{
			for (int i=0; i<r; i++)
			{
				if (ptr >= key.length)
				{
					ptr = 0;
				}
				buf[i+off] = (byte)(buf[i+off] ^ (key[ptr++] & 0xff));
			}
		}
		return r;
	}

	// ---( interface methods )---

}


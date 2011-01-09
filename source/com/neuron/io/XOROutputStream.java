/*
 * Copyright 2001-2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.io;

// ---( imports )---
import java.io.*;

public class XOROutputStream extends BufferedOutputStream
{
	// ---( static fields )---

	// ---( static methods )---
	// 0 - input file
	// 1 - key file
	// 2 - key offset
	// 3 - key len
	// 4 - output file
	public static void main(String args[])
		throws IOException
	{
		byte key[] = ByteOutputBuffer.readFully(
			new FileInputStream(args[1]));
		int off = Integer.parseInt(args[2]);
		int len = Integer.parseInt(args[3]);
		byte kb[] = new byte[len];
		System.arraycopy(key,off,kb,0,len);
		XOROutputStream xo = new XOROutputStream(
			new FileOutputStream(args[4]), kb);
		
		FileInputStream in = new FileInputStream(args[0]);
		byte buf[] = new byte[1024];
		int read = 0;
		while ((read = in.read(buf)) >= 0)
		{
			xo.write(buf,0,read);
		}
		xo.flush();
	}

	// ---( constructors )---
	public XOROutputStream (OutputStream os, byte key[])
	{
		super(os);
		this.key = key;
		this.ptr = 0;
	}

	// ---( instance fields )---
	private byte[] key;
	private int ptr;

	// ---( instance methods )---
	public void write(byte b[], int off, int len)
		throws IOException
	{
		for (int i=0; i<len; i++)
		{
			if (ptr >= key.length)
			{
				ptr = 0;
			}
			b[i+off] = (byte)(b[i+off] ^ (key[ptr++] & 0xff));
		}
		super.write(b, off, len);
	}

	public void write(byte b)
		throws IOException
	{
		if (ptr >= key.length)
		{
			ptr = 0;
		}
		super.write(b ^ (key[ptr++] & 0xff));
	}

	// ---( interface methods )---
}


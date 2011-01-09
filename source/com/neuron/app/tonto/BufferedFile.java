/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.*;

class BufferedFile
{
	// ---( static fields )---
	//private static Debug debug = Debug.getInstance(BufferedFile.class);

	// ---( static methods )---
	/*
	static
	{
		debug.setLevel(2);
	}
	*/

	// ---( constructors )---
	public BufferedFile ()
	{
		this(new byte[0]);
	}

	public BufferedFile (byte b[])
	{
		buf = b;
		flen = b.length;
		dirty = false;
		seek(0);
	}

	public BufferedFile (String f, String mode)
		throws IOException
	{
		file = new RandomAccessFile(f, mode);
		buf = new byte[(int)file.length()];
		file.seek(0);
		file.readFully(buf);
		if (mode.equalsIgnoreCase("r"))
		{
			file.close();
		}
		flen = buf.length;
		dirty = false;
		seek(0);
	}

	// ---( instance fields )---
	private RandomAccessFile file;
	private byte buf[];
	private long pos;
	private long flen;
	private boolean dirty;

	// ---( instance methods )---
	public void setLength(int len)
		throws IOException
	{
		if (file != null)
		{
			file.setLength(len);
		}
		flen = len;
	}

	public void readFully(byte b[])
		throws EOFException
	{
		if (pos+b.length > flen)
		{
			throw new EOFException();
		}
		System.arraycopy(buf,(int)pos,b,0,b.length);
		pos += b.length;
	}

	public int getByte()
	{
		return read();
	}

	public int skipBytes(int len)
	{
		pos += len;
		return len;
	}

	public long getFilePointer()
	{
		return pos;
	}

	public void seek(long pos)
	{
		this.pos = pos;
	}

	public long length()
	{
		if (file == null)
		{
			return flen;
		}
		try
		{
			return Math.max(file.length(), flen);
		}
		catch (Exception ex)
		{
			return flen;
		}
	}

	public int getShort()
	{
		short s = (short)(
			((buf[(int)pos++]&0xff) << 8) |
			((buf[(int)pos++]&0xff) << 0));
		return (int)s;
	}

	public int getInt()
	{
		return
			((buf[(int)pos++]&0xff) << 24) |
			((buf[(int)pos++]&0xff) << 16) |
			((buf[(int)pos++]&0xff) << 8)  |
			((buf[(int)pos++]&0xff) << 0);
	}

	public int read()
	{
		return buf[(int)pos++]&0xff;
	}

	public int read(byte b[], int offset, int len)
	{
		System.arraycopy(buf,(int)pos,b,offset,len);
		seek(pos+len);
		return len;
	}

	public void putByte(int b)
	{
		write(b);
	}

	public void putShort(int s)
	{
		write(s >> 8);
		write(s >> 0);
	}

	public void putInt(int i)
	{
		write(i >> 24);
		write(i >> 16);
		write(i >> 8);
		write(i >> 0);
	}

	public void write(int b)
	{
		dirty = true;
		checkWritePos(pos, 1);
		buf[(int)pos] = (byte)(b & 0xff);
		seek(pos+1);
	}

	public void write(byte b[])
	{
		write(b,0,b.length);
	}

	public void write(byte b[], int off, int len)
	{
		dirty = true;
		checkWritePos(pos, len);
		System.arraycopy(b,off,buf,(int)pos,len);
		seek(pos+len);
	}

	private void checkWritePos(long pos, int len)
	{
		if (pos+len > buf.length)
		{
			byte b[] = new byte[(int)(pos+len+16384)];
			System.arraycopy(buf,0,b,0,buf.length);
			buf = b;
		}
		flen = Math.max(flen, pos+len);
	}

	public int getCRC(int pos)
	{
		CRC16 crc = new CRC16();
		for (int i=0; i<pos; i++)
		{
			crc.update(buf[i]);
		}
		return crc.getValue();
	}

	public void close()
	{
		if (dirty && file != null)
		{
			try
			{
				file.seek(0);
				file.write(buf,0,(int)flen);
				file.close();
				dirty = false;
				file = null;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		try
		{
			if (file != null)
			{
				file.close();
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public byte[] toByteArray()
	{
		byte b[] = new byte[(int)flen];
		System.arraycopy(buf,0,b,0,b.length);
		return b;
	}

	// ---( interface methods )---

}


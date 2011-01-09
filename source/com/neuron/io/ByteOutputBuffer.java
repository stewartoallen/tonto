/*
 * Copyright 2001-2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.io;

import java.io.*;
import java.util.*;

/**
 * ByteOutputBuffer is very similar in spirit to Java's
 * ByteArrayOutputStream but is significantly more efficient
 * for large amounts of data. It provides utility methods
 * for efficiently writing the accumulated byte buffer to other
 * OutputStreams and a reset() method to re-usability. In addition,
 * it allows for stateful reading of the accumulated buffer even
 * while the buffer may still be in use.
 */
public final class ByteOutputBuffer extends OutputStream
{
	private int blen;
	private int idx;
	private int size;
    private int waiters; 
	private Vector v;
	private byte cur[];
	private boolean closed;

	/**
	 * Read an InputStream to it's end and return the
	 * resulting byte array.
	 */
	public static byte[] readFully(InputStream is)
		throws IOException
	{
		ByteOutputBuffer bob = new ByteOutputBuffer(4096);
		byte b[] = new byte[4096];
		int sz = 0;
		while ((sz = is.read(b)) >= 0)
		{
			bob.write(b,0,sz);
		}
		return bob.toByteArray();
	}

	/**
	 * Create a new ByteOutputBuffer with default initial capacity.
	 */
	public ByteOutputBuffer() {
		this(256);
	}

	/**
	 * Create a new ByteOutputBuffer with a specified initial capacity.
	 */
	public ByteOutputBuffer(int size)
	{
		this.blen = size;
		this.idx = 0;
		this.size = 0;
        this.waiters = 0;
		this.v = new Vector();
		this.cur = new byte[blen];
		this.closed = false;
	}
	
	/**
	 * Reset the ByteOutputBuffer for re-use. All data is discarded.
	 */
	public synchronized void reset()
	{
		idx = 0;
		size = 0;
		v.removeAllElements();
	}

	/**
	 * In ByteOutputBuffer this is a NOOP.
	 */
	public void flush() throws IOException { }

	/**
	 * Prevent further writing to this stream.
	 */
	public void close()
		throws IOException
	{
		synchronized (this)
		{
			this.notify();
			this.closed = true;
		}
	}

	/**
	 * Write a single byte to the stream.
	 */
	public synchronized void write(int b)
		throws IOException
	{
			if (closed)
				throw new IOException("Stream is already closed");

			if (idx >= blen)
				_roll();
			cur[idx++] = (byte)b;
			size++;

			// let read methods know data is ready
           if (waiters > 0)
              this.notify();
	}

	/**
	 * Write a byte array to the stream.
	 */
	public void write(byte b[])
		throws IOException
	{
		write(b, 0, b.length);
	}

	/**
	 * Write a portion of a byte array to the stream.
	 */
	public synchronized void write(byte b[], int off, int len)
		throws IOException
	{
			if (closed)
				throw new IOException("Stream is already closed");

			int rem;
			int olen = len;

			while ( (rem = (blen - idx)) < len )
			{
				System.arraycopy(b,off,cur,idx,rem);
				_roll();
				off += rem;
				len -= rem;
			}

			System.arraycopy(b,off,cur,idx,len);
			idx += len;

			size += olen;

			// let read methods know data is ready
           if (waiters > 0)
              this.notify();
	}

	/**
	 * Returns the number of bytes written to the stream since it
	 * was created or since it was last reset().
	 */
	public int size() { return size; }

	private void _roll()
	{
		v.addElement(cur);
		if (blen < 1048576) {
			blen *= 2;
		}
		cur = new byte[blen];
		idx = 0;
	}

	/**
	 * Write the entire contents of the stream to another OutputStream.
	 * This is more efficient than using the results of toByteArray()
	 * to write to another stream since it does not require the creation
	 * of a secondary accumulate array.
	 *
	 * @see toByteArray
	 */
	public synchronized void writeToStream(OutputStream out)
		throws IOException
	{
		int vl = v.size();
		for (int i=0; i<vl; i++) {
			out.write( (byte[])v.elementAt(i) );
		}
		out.write( cur, 0, idx );
	}

	/**
	 * Returns a byte array representation of the currently accumulated
	 * stream. If the intent is to then write this data to another OutputStream,
	 * it is much more efficient to use the writeToStream() method since this
	 * method must create a secondary unified array for storage.
	 *
	 * @see writeToStream
	 */
	public synchronized byte[] toByteArray()
	{
		int pos = 0;
		byte b[] = new byte[size];
		int vl = v.size();
		for (int i=0; i<vl; i++)
		{
			byte bb[] = (byte[])v.elementAt(i);
			int l = bb.length;
			System.arraycopy(bb,0,b,pos,l);
			pos += l;
		}
		System.arraycopy(cur,0,b,pos,idx);
		return b;
	}

	/**
	 * Return a string representation of stream's contents.
	 */
	public String toString() {
		return new String(toByteArray());
	}

	/**
	 * Return a string representation of stream's contents using a
	 * specified String encoding.
	 */
	public String toString(String encoding)
		throws Exception
	{
		return new String(toByteArray(), encoding);
	}

	// ---( read methods )---
	private int readPtr;
	private int readIdx;
	private int readOff;
	private byte readBuf[];

	/**
	 * Reset the read pointer to the beginning of the buffer.
	 */
	public void resetRead()
	{
		readIdx = 0;  // offset against all byte data
		readPtr = 0;  // offset into Vector of byte[]
		readOff = 0;  // offset into readBuf
		readBuf = null;
	}
	
	/**
	 * Return the next element from the buffer.
	 */
	public synchronized int read()
	{
			if (!_waitData()) { return -1; }
			int ret = readBuf[readOff++] & 0xff;
			readIdx++;

			if (readOff >= readBuf.length)
				_incrReadBuf();

			return ret;
	}

	/**
	 * Read from the buffer into the given byte array.
	 */
	public int read(byte b[])
	{
		return read(b, 0, b.length);
	}

	/**
	 * Read from the buffer into the given byte array at a specified
	 * offset into the byte array with a given length.
	 */
	public synchronized int read(byte b[], int off, int len)
	{
			if (!_waitData()) { return -1; }

			int toSend = Math.min(len, available());
			int read = 0;
			while (read < toSend) {
				int chunk = Math.min(len-read, _leftInBuf());
				System.arraycopy(readBuf, readOff, b, off+read, chunk);
				readOff += chunk;
				readIdx += chunk;
				if (readOff >= readBuf.length)
					_incrReadBuf();
				read += chunk;
			}

			return toSend;
	}

	private boolean _waitData()
	{
		if (_leftInBuf() > 0) {
			return true;
		}

		if (_buffersLeft() <= 0)
		{
			if (closed) {
				return _incrReadBuf();
			}

			try 
			{
				waiters++;
				this.wait();
				waiters--;
			} 
			catch (Exception e) 
			{ 
				e.printStackTrace();
				waiters--;
				return false; 
			}
		}

		if (readBuf == null)
			_incrReadBuf();

		return _leftInBuf() > 0;
	}

	/**
	 * Returns the remaining elements in the buffer from the
	 * current read pointer position.
	 */
	public byte[] getRemainder()
	{
		byte a[] = toByteArray();
		byte b[] = new byte[size - readIdx];
		System.arraycopy(a,readIdx,b,0,b.length);
		return b;
	}

	private int _buffersLeft() { return  v.size() - readPtr; }

	private int _leftInBuf() {
		if (readBuf == null) { return 0; }
		return Math.min(readBuf.length - readOff, size - readIdx);
	}

	/**
	 * Returns the number of bytes available for reading.
	 */
	public int available() {
		return size - readIdx;
	}

	private boolean _incrReadBuf()
	{
		if (readPtr < v.size()) {
			readOff = 0;
			readBuf = (byte[])v.elementAt(readPtr++);
			return true;
		} else
		if (readPtr == v.size()) {
			readOff = 0;
			readBuf = cur;
			readPtr++;
			return true;
		} else {
			return false;
		}
	}
}


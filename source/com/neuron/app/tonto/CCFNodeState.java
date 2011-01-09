/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.*;
import java.util.*;
import java.lang.reflect.*;

final class CCFNodeState
{
	private static Debug debug = Debug.getInstance("ccf");
	public final static int MAGIC = 0x4d4c5a4f;
	public final static int magicOffset = 8;

	// ---( instance fields )---
	CCFHeader head;
	private BufferedFile ra;
	private Hashtable cache;		// key=pos val=object
	private Hashtable strings;		// key=string val=pos
	private Stack resolveStack;
	private ITaskStatus status;
	private long ccflen;
	private long readbytes;
	private int lastPCT = -1;

	// ---( constructors )---
	CCFNodeState(CCFHeader head, ITaskStatus status)
	{
		this.head = head;
		this.status = status;
		this.cache = new Hashtable();
		this.strings = new Hashtable();
		this.resolveStack = new Stack();
	}

	BufferedFile buffer()
	{
		return ra;
	}

	void updateMeter(Object o, int len)
	{
		readbytes += len;
		if (ccflen > 0)
		{
			setMeter((int)((readbytes*100)/ccflen));
		}
	}

	void setMeter(ITaskStatus status)
	{
		this.status = status;
	}

	void setMeter(int pct)
	{
		if (status != null && pct != lastPCT)
		{
			status.taskStatus(Math.min(100,pct), null);
			lastPCT = pct;
		}
	}

	void notify(String msg)
	{
		if (status != null)
		{
			status.taskNotify(msg);
		}
	}

	private void addByClass(Hashtable src, Vector dst, Class cls)
	{
		for (Enumeration e = src.elements(); e.hasMoreElements(); )
		{
			Object o = e.nextElement();
			if (o.getClass() == cls)
			{
				dst.addElement(o);
			}
		}
	}

	private Hashtable all;

	boolean willEncode(CCFNode node)
	{
		if (node == null || all == null)
		{
			return true;
		}
		return all.get(node) != null;
	}

	byte[] encodeToBytes()
		throws IOException
	{
		ra = new BufferedFile();
		encode();
		return ra.toByteArray();
	}

	void encodeToFile(String file)
		throws IOException
	{
		File F = new File(file);
		File T = new File(file+".tmp-"+(Util.time()&0xffff));
		debug.log(3, "encode: F="+F+" T="+T);
		// encode to tmp
		ra = new BufferedFile(T.toString(), "rw");
		ra.setLength(0);
		encode();
		boolean rename = true;
		// rename previous ccf to .old
		if (F.exists())
		{
			File old = new File(file+".old");
			debug.log(3, "encode: F="+F+" T="+T+" old="+old+" :: old exists="+old.exists());
			if (old.exists())
			{
				old.delete();
			}
			rename = F.renameTo(new File(file+".old"));
			debug.log(3, "encode: rename = "+rename);
		}
		// check for file to small
		if (T.length() < 75)
		{
			throw new IOException("Unknown error writing new file");
		}
		// rename tmp to current
		if (!T.renameTo(F))
		{
			debug.log(3, "encode: unable to rename T to F");
			throw new IOException("Unable to rename temp to current");
		}
		if (!rename)
		{
			throw new IOException("Unable to rename previous file to .old");
		}
	}

	private void encode()
		throws IOException
	{
		try
		{

		notify("Encoding CCF");
		// clear caches
		cache.clear();
		strings.clear();
		// re-populate cache and strings
		all = new Hashtable();
		head.setFilePosition(0);
		addField(head, all);
		setMeter(0);
		setMeter(1);
		// build ordered write vector
		Vector ordered = new Vector();
		ordered.addElement(head);
		addByClass(all, ordered, CCFIcon.class);
		setMeter(2);
		addByClass(all, ordered, String.class);
		setMeter(3);
		addByClass(all, ordered, CCFIRCode.class);
		setMeter(4);
		addByClass(all, ordered, CCFActionList.class);
		setMeter(5);
		addByClass(all, ordered, CCFButton.class);
		setMeter(6);
		addByClass(all, ordered, CCFFrame.class);
		setMeter(7);
		addByClass(all, ordered, CCFPanel.class);
		setMeter(8);
		addByClass(all, ordered, CCFDevice.class);
		setMeter(9);
		addByClass(all, ordered, CCFTimer.class);
		setMeter(10);
		// update positions
		int end = 0;
		int cnt = 0;
		int sz = ordered.size();
		for (Enumeration e = ordered.elements(); e.hasMoreElements(); )
		{
			Object o = e.nextElement();
			if (o instanceof CCFNode)
			{
				CCFNode z = (CCFNode)o;
				z.setFilePosition(end);
				end += z.getLength();
			}
			else
			if (o instanceof String)
			{
				String s = (String)o;
				putStringLocation(end, s);
				end += (s.length()+1);
			}
			setMeter(10+(cnt/sz));
			cnt += 30;
		}
		// make even bytes in length
		end += (end % 2);
		head.crc1Pos = end;
		head.crc2Pos = end;
		// write elements
		for (Enumeration e = ordered.elements(); e.hasMoreElements(); )
		{
			Object o = e.nextElement();
			if (o instanceof CCFNode)
			{
				((CCFNode)o).encode(this, false);
			}
			else
			if (o instanceof String)
			{
				CCFNode.stringLengthEncode(ra, (String)o);
			}
			setMeter(10+(cnt/sz));
			cnt += 60;
		}
		setMeter(100);
		// write crc
		ra.seek(head.crc1Pos);
		ra.write(0);
		ra.seek(head.crc1Pos);
		ra.putShort(ra.getCRC(head.crc1Pos));

		}
		finally
		{
			ra.close();
		}
	}

	void setBuffer(byte b[])
	{
		this.ra = new BufferedFile(b);
	}

	void setBuffer(String file)
		throws IOException
	{
		this.ra = new BufferedFile(file, "r");
	}

	void decodeFromBytes(byte b[])
	{
		this.ra = new BufferedFile(b);
		decode();
	}

	void decodeFromFile(String file)
		throws IOException
	{
		this.ra = new BufferedFile(file, "r");
		decode();
	}

	private void checkLZO()
	{
		try
		{

		ra.seek(magicOffset);
		if (ra.getInt() == MAGIC)
		{
			debug.log(0, "detected compressed ccf");
			ra.seek(0);
			int blksize = ra.getInt();
			int filelen = ra.getInt();
			int magic = ra.getInt();
			int blk = 0;

			byte ipbuf[] = new byte[blksize];
			byte opbuf[] = new byte[blksize];

			BufferedFile nf = new BufferedFile(new byte[filelen]);

			while (true)
			{
				int iplen = ra.getInt();
				if (iplen <= 0)
				{
					break;
				}
				int rd = ra.read(ipbuf, 0, iplen);
				int rz = Util.decompress(ipbuf, 0, iplen, opbuf, 0, blksize);
				int pct = 100-(int)((double)iplen/(double)rz*100.0);
				debug.log(2, "lzo blk="+(blk++)+" in="+iplen+" out="+rz+" comp="+pct+"%");
				ra.skipBytes((iplen%4) > 0 ? (4-(iplen%4)) : 0);
				nf.write(opbuf,0,rz);
			}

			ra = nf;
		}
		ra.seek(0);

		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	private void decode()
	{
		notify("Decoding CCF");
		long time = Util.time();
		checkLZO();
		ccflen = ra.length();
		cache.clear();
		strings.clear();
		head.decode(this);
		processDeferred();
		head.buildTree(null);
		setMeter(100);
		ra.close();
		//ra = null;
		time = Util.time() - time;
		debug.log(2, "*** ccf decoded in "+time+" ms ***");
	}

	// encode setup
	void addField(Object o, Hashtable v)
	{
		addField(o, v, true);
	}

	void addField(Object o, Hashtable v, boolean isRef)
	{
		if (o != null && v.get(o) != null)
		{
			return;
		}
		if (o instanceof CCFNode)
		{
			CCFNode z = (CCFNode)o;
			z.setHeader(head);
			z.checkVersion();
			if (!(o instanceof CCFAction || o instanceof CCFChild))
			{
				v.put(z, z);
			}
			z.encodePrep(this, v);
		}
		else
		if (o instanceof CCFNode[])
		{
			CCFNode z[] = (CCFNode[])o;
			for (int i=0; i<z.length; i++)
			{
				addField(z[i], v);
			}
			return;
		}
		else
		if (o instanceof String && isRef)
		{
			v.put(o, o);
		}
	}

	// store an object keyed by location
	void putObjectAt(int location, CCFNode obj)
	{
		cache.put(new Integer(location), obj);
	}

	void removeObjectAt(int location)
	{
		cache.remove(new Integer(location));
	}

	CCFNode[] getOrderedNodes()
	{
		Vector v = new Vector();
		v.add(head);
		for (Enumeration e = cache.keys(); e.hasMoreElements(); )
		{
			Integer k = (Integer)e.nextElement();
			CCFNode n = (CCFNode)cache.get(k);
			v.add(n);
		}
		CCFNode n[] = new CCFNode[v.size()];
		v.copyInto(n);
		Arrays.sort(n, new Comparator() {
			public int compare(Object o1, Object o2) {
				if (o1 == o2 || o1 == null || o2 == null) { return 0; }
				int n1 = ((CCFNode)o1).getFilePosition();
				int n2 = ((CCFNode)o2).getFilePosition();
				if (n1 < n2) { return -1; }
				if (n1 > n2) { return 1; }
				return 0;
			}
			public boolean equals(Object o1) {
				return false;
			}
		});
		return n;
	}

	// retrieve an object by location
	CCFNode getObjectAt(int location)
	{
		return (CCFNode)cache.get(new Integer(location));
	}

	// store a string by location
	void putLocationString(String str, int loc)
	{
		strings.put(new Integer(loc), str);
	}

	// retrieve a string by location
	String getLocationString(int loc)
	{
		return (String)strings.get(new Integer(loc));
	}

	// retrieve a location given a string
	long getStringLocation(String str)
	{
		if (str == null)
		{
			return 0;
		}
		Long l = (Long)strings.get(str);
		return l != null ? l.longValue() : 0;
	}

	// store a location with the string as key
	void putStringLocation(long location, String str)
	{
		strings.put(str, new Long(location));
	}

	private int getCRC(String file, int pos)
		throws IOException
	{
		BufferedFile ra = new BufferedFile(file, "r");
		CRC16 crc = new CRC16();
		for (int i=0; i<pos; i++)
		{
			crc.update((byte)ra.read());
		}
		return crc.getValue();
	}

	CCFHeader getHeader()
	{
		return head;
	}

	void deferResolve(CCFNode src, String field, int pos, Class clz)
	{
		resolveStack.push(new defer(src, field, pos, clz));
	}

	// cycle through deferred fields
	void processDeferred()
	{
		while (!resolveStack.empty())
		{
			((defer)resolveStack.pop()).process();
		}
	}

	// used to defer object resolution
	class defer
	{
		private CCFNode src;
		private String field;
		private int pos;
		private Class clz;

		defer(CCFNode z, String f, int p, Class c)
		{
			src = z;
			field = f;
			pos = p;
			clz = c;
		}

		void process()
		{
			try
			{
				Object val = src.getItemByPos(CCFNodeState.this, pos, clz);
				//field.set(src, val);
				src.set(field, val);
			}
			catch (Exception ex)
			{
				error("defer", src, field, pos, ex);
				debug.log(1,"defer process error @ "+src);
			}
		}
	}

	boolean seek(long pos)
	{
		if (pos < head.attrPos || pos > ccflen)
		{
			return false;
		}
		else
		{
			ra.seek(pos);
			return true;
		}
	}

	boolean isValidPtr(long pos)
	{
		return pos > 0 && pos < ccflen;
	}

	CodecError error(String msg, CCFNode node, String fld, int pos, Exception ex)
	{
		CodecError err = new CodecError(msg, node, fld, pos, ex);
		node.error(err);
		return err;
	}

	class CodecError extends RuntimeException
	{
		private CCFNode node;
		private String fld;
		private int pos;
		private Exception ex;

		CodecError(String msg, CCFNode node, String fld, int pos, Exception ex)
		{
			super (msg);
			this.node = node;
			this.fld = fld;
			this.pos = pos;
			this.ex = ex;
		}

		public String toString()
		{
			return "err in "+node+"["+fld+"] @ "+pos+
				(ex != null ? " ("+super.getMessage()+":"+ex+")" : super.getMessage());
		}

		public String getMessage()
		{
			return toString();
		}

		public void printStackTrace()
		{
			if (ex != null)
			{
				ex.printStackTrace();
			}
			else
			{
				super.printStackTrace();
			}
		}
	}
}


/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.*;
import java.awt.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * The parent class of all CCF tree node elements.
 */
public abstract class CCFNode implements Cloneable
{
	// ---( static fields )---
	static Debug debug = Debug.getInstance("ccf");

	private static int nextZip = 0;
	private static Hashtable classCache = new Hashtable();

	private final static char BYTE_FIELD     = 'B';
	private final static char NODE_FIELD     = 'Z';
	private final static char INTEGER_FIELD  = 'N';
	private final static char STRING_FIELD   = 'S';
	private final static char IGNORE_FIELD   = 'X';
                                           
	private final static char FIELD_PTR      = '*';
	private final static char FIELD_COUNT    = '+';

	public final static char  TEXT_CENTER    = 0x00;
	public final static char  TEXT_LEFT      = 0x01;
	public final static char  TEXT_RIGHT     = 0x02;

	// ---( instance fields )---
	private int zipNum;
	private int filepos;
	private boolean fixedPos;
	private Hashtable cache;
	private CCFNode parent;
	private CCFHeader header;

	// ---( constructors )---
	CCFNode()
	{
		zipNum = nextZip++;
		Hashtable c = (Hashtable)classCache.get(getClass());
		if (c == null)
		{
			c = new Hashtable();
			classCache.put(getClass(), c);
		}
		cache = c;
	}

	// ---( utility methods )---
	final void setHeader(CCFHeader head)
	{
if (head == null)
{
	new Exception("set header to null").printStackTrace();
}
		this.header = head;
	}

	void setParent(CCFNode p)
	{
		this.parent = p;
		if (p instanceof CCFHeader)
		{
			setHeader((CCFHeader)p);
		}
		else
		if (p != null)
		{
			setHeader(p.getHeader());
		}
	}

	/**
	 *
	 */
	public CCFNode getParent()
	{
		return parent;
	}

	/**
	 *
	 */
	public CCFHeader getHeader()
	{
		if (header != null)
		{
			return header;
		}
		if (this instanceof CCFHeader)
		{
			return (CCFHeader)this;
		}
		CCFNode p = getParent();
		if (p == null)
		{
			return null;
		}
		return p.getHeader();
	}

	/**
	 *
	 */
	public CCFDevice getParentDevice()
	{
		if (this instanceof CCFDevice)
		{
			return (CCFDevice)this;
		}
		CCFNode p = getParent();
		if (p == null)
		{
			return null;
		}
		return p.getParentDevice();
	}

	/**
	 *
	 */
	public CCFPanel getParentPanel()
	{
		if (this instanceof CCFPanel)
		{
			return (CCFPanel)this;
		}
		CCFNode p = getParent();
		return p != null ? p.getParentPanel() : null;
	}

	// ---( internal helpers )---
	boolean usingColor()
	{
		return getHeader().hasColor();
	}

	void setFixedPosition(boolean fixed)
	{
		fixedPos = fixed;
	}

	int getFilePosition()
	{
		return filepos;
	}

	void setFilePosition(int pos)
	{
		filepos = pos;
	}

	// ---( encode/decode helpers )---
	String getFieldName(int index)
	{
		return getDecodeTable()[index][1];
	}

	Field getField(String name)
	{
		if (name == null)
		{
			return null;
		}
		try
		{
			Field f = (Field)cache.get(name);
			if (f != null)
			{
				return f;
			}
			if (useParentFields())
			{
				f = getClass().getSuperclass().getDeclaredField(name);
			}
			else
			{
				f = getClass().getDeclaredField(name);
			}
			cache.put(name, f);
			return f;
		}
		catch (RuntimeException rex)
		{
			throw rex;
		}
		catch (Exception ex)
		{
			throw error(ex);
		}
	}

	Object get(String name)
	{
		try
		{
			return getField(name).get(this);
		}
		catch (RuntimeException rex)
		{
			throw rex;
		}
		catch (Exception ex)
		{
			throw error(ex);
		}
	}

	void set(String name, Object value)
	{
		try
		{
			getField(name).set(this, value);
		}
		catch (RuntimeException rex)
		{
			throw rex;
		}
		catch (Exception ex)
		{
			throw error(ex);
		}
	}

	// ---( encode helpers )---
	String getEncodeField(int index)
	{
		return getEncodeTable()[index][1];
	}

	Object getEncodeValue(int index)
	{
		return get(getEncodeTable()[index][1]);
	}

	// ---( decode helpers )---
	String getDecodeField(int index)
	{
		return getDecodeTable()[index][1];
	}

	Object getDecodeValue(int index)
	{
		return get(getDecodeTable()[index][1]);
	}

	int getDecodeValueLength(int index)
	{
		return
			((Number)get(getDecodeTable()[index][2])).intValue() &
			0xffffffff;
	}

	int getDecodeValueVariance(int index)
	{
		return Integer.parseInt(getDecodeTable()[index][3]);
	}

	// ---( clone and utilities )---
	CCFNode getClone()
	{
		try
		{
			CCFNode clone = (CCFNode)clone();
			return clone;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return null;
		}
	}

	CCFActionList getClone(CCFActionList list)
	{
		if (list != null)
		{
			return (CCFActionList)list.getClone();
		}
		return null;
	}

	CCFChild[] getClone(CCFChild c[])
	{
		if (c == null)
		{
			return null;
		}
		CCFChild nc[] = new CCFChild[c.length];
		for (int i=0; i<c.length; i++)
		{
			nc[i] = (CCFChild)c[i].getClone();
		}
		return nc;
	}

	// ---( instance methods )---
	private boolean jumpDecode(int pos, CCFNodeState zs)
	{
		long oldpos = zs.buffer().getFilePointer();
		log(3,"jumpDecode("+pos+") <- ("+oldpos+")");
		if (zs.seek(pos))
		{
			decode(zs);
			zs.buffer().seek(oldpos);
			return true;
		}
		else
		{
			error("attempt to read outside file bounds ("+hex(pos)+")");
			traceParents();
			return false;
		}
	}

	private CCFNode readItem(CCFNodeState zs, Class clz)
	{
		return getItemByPos(zs, (int)zs.buffer().getFilePointer(), clz, false);
	}

	CCFNode getItemByPos(CCFNodeState zs, int pos, Class clz)
	{
		return getItemByPos(zs, pos, clz, true);
	}

	private CCFNode getItemByPos(CCFNodeState zs, int pos, Class clz, boolean jmp)
	{
		if (!zs.isValidPtr(pos))
		{
			error("invalid pointer ("+hex(pos)+") to "+Util.shortName(clz)+" in "+describe());
			return null;
		}
		CCFNode zp = zs.getObjectAt(pos);
		if (zp != null)
		{
			// required to advance file pos
			if (!jmp)
			{
				log(3,"**** skipping already read ("+zp+")", zs);
				zs.buffer().skipBytes(zp.getLength());
			}
			return zp;
		}
		try
		{
			zp = (CCFNode)clz.newInstance();
			zp.setHeader(zs.getHeader());
			zp.checkVersion();
			zs.putObjectAt(pos, zp);
		}
		catch (Exception ex)
		{
			error(ex);
			return null;
		}
		if (jmp)
		{
			// jump and decode
			if (!zp.jumpDecode(pos, zs))
			{
				log(3,"**** removing invalid ("+zp+"@"+pos+")", zs);
				zs.removeObjectAt(pos);
				return null;
			}
		}
		else
		{
			// decode in place
			zp.decode(zs);
		}
		return zp;
	}

	// sets the file position and returns the object length in bytes
	int getLength(int pos)
	{
		filepos = pos;
		return getLength();
	}

	int getFieldLen(String table[][], int i)
	{
		char typ = table[i][0].charAt(0);
		char len = table[i][0].charAt(1);
		if (typ == IGNORE_FIELD)
		{
			return 0;
		}
		if (len == FIELD_PTR)
		{
			return 4;
		}
		else
		if (len == FIELD_COUNT)
		{
			return 0;
		}
		else
		{
			if (typ == STRING_FIELD || typ == INTEGER_FIELD)
			{
				return (len - '0');
			}
			else
			if (typ == NODE_FIELD)
			{
				return 0;
			}
			else
			{
				throw error("unhandled encode option '"+typ+"'");
			}
		}
	}

	// returns the object length in bytes
	int getLength()
	{
		int length = 0;
		String encTable[][] = getEncodeTable();
		for (int i=0; i<encTable.length; i++)
		{
			char typ = encTable[i][0].charAt(0);
			char len = encTable[i][0].charAt(1);
			if (typ == IGNORE_FIELD)
			{
				continue;
			}
			if (len == FIELD_PTR)
			{
				length += 4;
			}
			else
			if (len == FIELD_COUNT)
			{
				if (typ == BYTE_FIELD)
				{
					byte b[] = (byte[])getEncodeValue(i);
					if (b != null)
					{
						length += b.length;
					}
				}
				else
				if (typ == NODE_FIELD)
				{
					CCFNode z[] = (CCFNode[])getEncodeValue(i);
					for (int j=0; z != null && j<z.length; j++)
					{
						length += z[j].getLength(filepos + length);
					}
				}
				else
				{
					throw error("unhandled encode option '"+typ+"'");
				}
			}
			else
			{
				if (typ == STRING_FIELD || typ == INTEGER_FIELD)
				{
					length += (len - '0');
				}
				else
				if (typ == NODE_FIELD)
				{
					CCFNode z = (CCFNode)getEncodeValue(i);
					if (z != null)
					{
						length += z.getLength(filepos + length);
					}
					else
					{
						error("field is missing required node");
					}
				}
				else
				{
					throw error("unhandled encode option '"+typ+"'");
				}
			}
		}
		return length;
	}

	void trace(int pos, String tab[][], int field, Exception ex)
	{
		ex.printStackTrace();
		debug.log(0,
			"errstack @ "+hex(pos)+" ("+myName()+","+
			tab[field][0]+","+tab[field][1]+") ("+ex+")"
		);
		CCFNode node = this;
		while (node != null)
		{
			debug.log(0, "  "+node.describe());
			node = node.getParent();
		}
	}

	void traceParents()
	{
		debug.log(0,"trace -->");
		CCFNode n = this;
		while (n != null)
		{
			debug.log(0, "  "+rpad(n.myName(),20)+" "+lpad(hex(n.hashCode()),10)+" "+n.describe());
			n = n.getParent();
		}
	}

	long encode(CCFNodeState zs)
	{
		return encode(zs,true);
	}

	long encode(CCFNodeState zs, boolean jump)
	{
		BufferedFile ra = zs.buffer();
		long pos = ra.getFilePointer();
		int i = 0;

		setHeader(zs.getHeader());
		try
		{

		log(2,"encoding", zs);
		ra.seek(filepos);
		preEncode(zs);
		String encTable[][] = getEncodeTable();

		for (i=0; i<encTable.length; i++)
		{
			char typ = encTable[i][0].charAt(0);
			char len = encTable[i][0].charAt(1);
			Object val = null;
			try
			{
				val = getEncodeValue(i);
			}
			catch (Exception ex)
			{
				throw new RuntimeException(ex.getMessage());
			}
			log(3,
				"   >>  @ "+hex(filepos)+" ("+myName()+","+
				encTable[i][0]+","+encTable[i][1]+") ("+val+")"
			);
			switch (typ)
			{
				case IGNORE_FIELD:
					continue;
				case BYTE_FIELD:
					ra.write((byte[])val);
					break;
				case NODE_FIELD:	
					if (len == FIELD_PTR)
					{
						CCFNode z = (CCFNode)val;
						if (z != null)
						{
							ra.putInt(z.filepos);
						}
						else
						{
							ra.putInt(0);
						}
					}
					else
					if (len == FIELD_COUNT)
					{
						CCFNode z[] = (CCFNode[])val;
						for (int j=0; z != null && j<z.length; j++)
						{
							z[j].encode(zs,false);
						}
					}
					else
					{
						CCFNode z = (CCFNode)val;
						z.encode(zs,false);
					}
					break;
				case INTEGER_FIELD:	
					switch (len)
					{
						case '1':
							ra.putByte(((Number)val).intValue());
							break;
						case '2':
							ra.putShort(((Number)val).intValue());
							break;
						case '4':
							ra.putInt(((Number)val).intValue());
							break;
						default:
							int cnt = len - '0';
							int valu = ((Number)val).intValue();
							for (int c=0; c<cnt; c++)
							{
								ra.putByte((valu >> ((cnt-c)*8)) & 0xff);
							}
							break;
					}
					break;
				case STRING_FIELD:
					String str = (String)val;
					if (len == FIELD_PTR)
					{
						ra.putInt(getStringEncodePos(zs,str));
					}
					else
					{
						stringEncode(ra, str);
					}
					break;
			}
		}

		}
		catch (Exception ex)
		{
			error(ex);
			trace((int)pos, getEncodeTable(), i, ex);
		}

		if (jump)
		{
			ra.seek(pos);
		}
		return ra.getFilePointer();
	}

	int getStringEncodePos(CCFNodeState zs, String s)
	{
		if (s == null)
		{
			return 0;
		}
		return (int)zs.getStringLocation(s);
	}

	void decode(CCFNodeState zs)
	{
		log(2,"begin decode", zs);
		int i = 0;
		int fp = 0;
		int delta = 0;

		try
		{

		preDecode(zs);
		BufferedFile in = zs.buffer();
		fp = (int)in.getFilePointer();
//		filepos = 0;
		filepos = fp;
		String decTable[][] = getDecodeTable();

		for (i=0; i<decTable.length; i++)
		{
			Object val = null;
			char typ = decTable[i][0].charAt(0);
			char len = decTable[i][0].charAt(1);
			String fnm = getDecodeField(i);
			Field fld = getField(fnm);
			long pos = in.getFilePointer();
			int defer = -1;
			switch (typ)
			{
				case BYTE_FIELD:
					{
						int fromVal = getDecodeValueLength(i);
						if (decTable[i].length > 3)
						{
							fromVal += getDecodeValueVariance(i);
						}
						byte b[] = new byte[fromVal];
						in.readFully(b);
						val = b;
						delta += b.length;
					}
					break;
				case NODE_FIELD:
					Class clz = fld.getType();
					if (len == FIELD_PTR)
					{
						int seek = in.getInt();
						if (seek > 0)
						{
							zs.deferResolve(this,fnm,seek,clz);
							defer = seek;
						}
						delta += 4;
					}
					else
					if (len == FIELD_COUNT)
					{
						CCFNode o[] = (CCFNode[])Array.newInstance(
							clz.getComponentType(), getDecodeValueLength(i));
						for (int j=0; j<o.length; j++)
						{
							o[j] = readItem(zs, clz.getComponentType());
							o[j].fixedPos = true;
						}
						val = o;
						delta += (in.getFilePointer()-pos);
					}
					else
					if (len == '1')
					{
						CCFNode z = readItem(zs, clz);
						z.fixedPos = true;
						val = z;
						delta += (in.getFilePointer()-pos);
					}
					break;
				case INTEGER_FIELD:
					switch (len)
					{
						case '1':
							if (decTable[i].length > 2)
							{
								val = new Integer(in.getByte() & 0xff);
							}
							else
							{
								val = new Integer(in.getByte());
							}
							break;
						case '2':
							if (decTable[i].length > 2)
							{
								val = new Integer(in.getShort() & 0xffff);
							}
							else
							{
								val = new Integer(in.getShort());
							}
							break;
						case '4':
							val = new Integer(in.getInt());
							break;
						default:
							int cnt = len - '0';
							int valu = 0;
							for (int c=0; c<cnt; c++)
							{
								valu += (in.getByte() & 0xff) << ((cnt-c)*8);
							}
							val = new Integer(valu);
							break;
					}
					delta += (len - '0');
					break;
				case STRING_FIELD:
					long repos = -1;
					if (len == FIELD_PTR)
					{
						int nupos = in.getInt();
						val = zs.getLocationString(nupos);
						if (val != null || nupos == 0)
						{
							break;
						}
						repos = in.getFilePointer();
						if (nupos > in.length())
						{
							val = null;
							log(0, "attempt to read string outside of file bounds", zs);
						}
						else
						{
							in.seek(nupos);
							val = stringLengthDecode(in);
							zs.putLocationString((String)val, nupos);
							in.seek(repos);
						}
						delta += 4;
					}
					else
					{
						val = stringDecode(in, (char)(len - '0'));
						delta += (len - '0');
					}
					break;
			}
			if (debug.debug(3))
			{
				log(3,
					" >>  @ "+hex(in.getFilePointer())+" ("+myName()+","+
					decTable[i][0]+","+decTable[i][1]+") "+
					"("+(val == null ? Integer.toString(defer) : val)+")"
				);
			}
			fld.set(this, val);	
		}

		}
		catch (RuntimeException rex)
		{
			trace(fp, getDecodeTable(), i, rex);
		}
		catch (Exception ex)
		{
			error(ex);
			trace(fp, getDecodeTable(), i, ex);
		}

		postDecode(zs);
		zs.updateMeter(this, delta);
		log(3, "end decode", zs);
	}

	String stringJumpDecode(int pos, CCFNodeState zs)
	{
		zs.buffer().seek(pos);
		return stringLengthDecode(zs.buffer());
	}

	String stringLengthDecode(BufferedFile in)
	{
		return stringDecode(in, in.read());
	}

	String stringDecode(BufferedFile in, int len)
	{
		char c[] = new char[len];
		for (int j=0; j<len; j++)
		{
			c[j] = (char)in.read();
		}
		return new String(c);
	}

	static void stringLengthEncode(BufferedFile out, String s)
	{
		out.write(s.length());
		stringEncode(out, s);
	}

	static void stringEncode(BufferedFile out, String s)
	{
		char c[] = new char[s.length()];
		s.getChars(0,c.length,c,0);
		for (int i=0; i<c.length; i++)
		{
			out.write(c[i]);
		}
	}

	// ---( utility methods )---
	public String toString()
	{
		return myName();
	}

	String myName()
	{
		return cname()+"-"+hex(zipNum);
	}

	private final static String pad="                                         ";

	final static String lpad(String str, int len)
	{
		int slen = str.length();
		if (slen < len)
		{
			return pad.substring(0,len-slen)+str;
		}
		if (slen > len)
		{
			return str.substring(0,len);
		}
		return str;
	}

	final static String rpad(String str, int len)
	{
		int slen = str.length();
		if (slen < len)
		{
			return str+pad.substring(0,len-slen);
		}
		if (slen > len)
		{
			return str.substring(0,len);
		}
		return str;
	}

	final static String hex(int val)
	{
		return "0x"+Integer.toHexString(val);
	}

	final static String hex(long val)
	{
		return "0x"+Long.toHexString(val);
	}

	final static boolean hasBits(int value, int bits)
	{
		return (value & bits) == bits;
	}

	void dump()
	{
		debug.log(0, "--( dump : "+this+" )--");
		String encTable[][] = getEncodeTable();
		int pos = filepos;
		for (int i=0; i<encTable.length; i++)
		{
			Field f = getField(getEncodeField(i));
			try
			{
				Object val = f.get(this);
				if (val instanceof Integer)
				{
					val = hex(((Integer)val).intValue());
				}
				else
				if (val == null)
				{
					val = "<null>";
				}
				else
				if (val instanceof String)
				{
					val = "'"+val+"'";
				}
				debug.log(0, lpad(hex(pos),7)+"  "+rpad(f.getName(),15)+" "+val);
				pos += getFieldLen(encTable, i);
			}
			catch (Exception ex)
			{
				debug.log(0, f.getName()+" = <inaccessible>");
			}
		}
	}

	void log(int lvl, String msg)
	{
		debug.log(lvl, msg);
	}

	void log(int lvl, String msg, CCFNodeState zs)
	{
		if (debug.debug(lvl))
		{
			try
			{
				debug.log(lvl, msg+" ("+myName()+") @ "+hex(zs.buffer().getFilePointer()));
			}
			catch (Exception ex)
			{
				debug.log(0, "** error tracing error **");
				debug.log(0, msg+" ("+myName()+") @ unknown");
				error(ex);
			}
		}
	}

	RuntimeException error(Exception ex)
	{
		log(0, "error -> "+ex.toString());
		return new RuntimeException(ex.getMessage());
	}

	RuntimeException error(String msg)
	{
		log(0, "error -> "+msg);
		return new RuntimeException(msg);
	}

	void errorOn(boolean bool, String msg)
	{
		if (bool)
		{
			error(msg);
		}
	}

	String cname()
	{
		String nm = getClass().getName();
		return nm.substring(nm.lastIndexOf(".")+1);
	}

	// ---( bit printing utilities )---
	static String get8BitSet(int b)
	{
		return "["+get8Bits(b)+"]";
	}

	static String get32Bits(int b)
	{
		return
			"["+get8Bits(b>>24)+","+get8Bits(b>>16)+","+
			get8Bits(b>>8)+","+get8Bits(b)+"]";
	}

	static String get16Bits(int b)
	{
		return "["+get8Bits(b>>8)+","+get8Bits(b&0xff)+"]";
	}

	static String get8Bits(int b)
	{
		return getBit(b,7)+getBit(b,6)+getBit(b,5)+getBit(b,4)+
			getBit(b,3)+getBit(b,2)+getBit(b,1)+getBit(b,0);
	}
	
	static String getBit(int b, int pos)
	{
		return (b & (1<<pos)) > 0 ? "O" : "-";
	}

	// ---( subclass override methods )---
	String describe()
	{
		return cname()+"@"+hex(filepos);
	}

	// overridden in subclasses of CCFAction because they
	// store all of their information in their parents fields.
	boolean useParentFields()
	{
		return false;
	}

	// overridden in CCFAction to prevent fields from being persisted
	// this allows culling of actions that point to dead items.
	void encodePrep(CCFNodeState zs, Hashtable dst)
	{
		String encTable[][] = getEncodeTable();
		for (int i=0; i<encTable.length; i++)
		{
			zs.addField(
				getEncodeValue(i), dst, encTable[i][0].charAt(1) == FIELD_PTR);
		}
	}

	// ---( abstract methods )---
	abstract void checkVersion()
		;

	abstract void preEncode(CCFNodeState st)
		;

	abstract void preDecode(CCFNodeState st)
		;

	abstract void postDecode(CCFNodeState st)
		;

	abstract String[][] getEncodeTable()
		;

	abstract String[][] getDecodeTable()
		;

	abstract void buildTree(CCFNode parent)
		;
}


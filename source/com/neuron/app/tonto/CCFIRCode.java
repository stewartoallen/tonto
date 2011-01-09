/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.*;
import com.neuron.irdb.impl.Pronto;
import com.neuron.irdb.impl.ProntoConstants;

/**
 * An abstraction for IRCodes sent by the Pronto.
 */
public class CCFIRCode extends CCFNode implements ProntoConstants
{
	private final static String[][] codec =
	{
		{ "N2", "size" },
		{ "S*", "name" },
		{ "B+", "data", "size", "-6" },
	};

	// ---( instance fields )---
	int     size;
	String  name;
	byte    data[];		// NEVER set directly!

	private boolean hasUDB;

	// ---( public API )---
	CCFIRCode()
	{
	}

	public CCFIRCode(CCFHeader header)
	{
		setHeader(header);
		hasUDB = header.hasUDB();
	}

	public CCFIRCode(CCFHeader header, String code)
	{
		this(header, "Learned", code);
	}

	public CCFIRCode(CCFHeader header, String name, String code)
	{
		this(header);
		setName(name);
		setCode(code);
	}

	/*
	public CCFNode getClone()
	{
debug.log(0, "cloning: "+this+" hdr="+getHeader());
		CCFIRCode code = (CCFIRCode)super.getClone();
debug.log(0, "clone == "+code+" hdr="+code.getHeader());
		return code;
	}
	*/

	// override in case size changes during preEncode
	int getLength()
	{
		CCFHeader hdr = getHeader();
		if (hdr == null)
		{
			return super.getLength();
		}
		// default to v2 encoding
		// setEncodingV2(true);
		//return super.getLength()+(isV1() ? 6 : 0);
		return super.getLength()+(hdr.hasUDB() && !hasUDB ? 6 : 0);
	}

	/**
	 * Get the name associated with this code.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Set the name associated with this code.
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	public void setUDB(boolean udb)
	{
		if (udb == hasUDB || data == null)
		{
			return;
		}
		if (udb)
		{
			// convert to udb
			debug.log(3, "ir convert v1 to v2 "+this);
			byte b[] = new byte[data.length+6];
			System.arraycopy(data,0,b,6,data.length);
			setData(b);
		}
		else
		{
			// convert to noudb
			debug.log(3, "ir convert v2 to v1 "+this);
			byte b[] = new byte[data.length-6];
			System.arraycopy(data,6,b,0,b.length);
			setData(b);
		}
		hasUDB = udb;
	}


	/**
	 * Get the Pronto ASCII Hex representation of this code.
	 */
	public String getCode()
	{
		if (data == null)
		{
			return "";
		}
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<data.length; i++)
		{
			int val = data[i] & 0xff;
			if (val <= 15)
			{
				sb.append("0");
			}
			sb.append(Integer.toHexString(val));
			if (i % 2 == 1 && i > 0 && i<(data.length-1))
			{
				sb.append(" ");
			}
		}
		return sb.toString();
	}

	/**
	 * Set the Pronto ASCII Hex representation of this code.
	 *
	 * @param hex new IRCode
	 */
	public void setCode(String hex)
	{
		StringBuffer strip = new StringBuffer();
		for (int i=0; i<hex.length(); i++)
		{
			if (!Character.isWhitespace(hex.charAt(i)))
			{
				strip.append(hex.charAt(i));
			}
		}
		setData(new byte[strip.length()/2]);
		for (int i=0; i<data.length; i++)
		{
			data[i] = (byte)Integer.parseInt(
				strip.substring(i*2, i*2+2), 16
			);
		}
	}

	// ---( instance methods )---
	boolean hasUDB()
	{
		return hasUDB;
	}

	byte[] getData()
	{
		return data;
	}

	void setData(byte d[])
	{
		data = d;
		size = d.length + 6;
	}

	// ---( override methods )---
	String describe()
	{
		return "IR,"+name+","+size+","+(hasUDB?"udb":"noudb");
	}

	// ---( abstract methods )---
	public void checkVersion()
	{
	}

	public void preEncode(CCFNodeState zs)
	{
		// check and convert to proper format for learned codes
		setUDB(getHeader().hasUDB());
	}

	public void preDecode(CCFNodeState zs)
	{
	}

	public void postDecode(CCFNodeState zs)
	{
		hasUDB = zs.getHeader().hasUDB();
		if (data == null)
		{
			throw new NullPointerException("IR data is null");
		}
		if (name == null)
		{
			name = "IR Code";
		}
	}

	String[][] getEncodeTable()
	{
		return codec;
	}

	String[][] getDecodeTable()
	{
		return codec;
	}	

	void buildTree(CCFNode parent)
	{
		setParent(parent);
	}
}


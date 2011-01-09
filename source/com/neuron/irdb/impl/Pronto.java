/*
 * Copyright 2000-2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.irdb.impl;

// ---( imports )---
import java.io.*;
import java.util.*;
import com.neuron.irdb.*;

public class Pronto extends IRSignal implements ProntoConstants
{
	public final static int VERSION1 = 1; // no UDB
	public final static int VERSION2 = 2; // UDB

	private byte[] raw;
	private boolean is0100;

	public Pronto(byte raw[])
	{
		decode(raw);
	}

	public Pronto(String raw)
	{
		this(raw, false);
	}

	public Pronto(String raw, int version)
	{
		this(raw, version == VERSION2);
	}

	public Pronto(String raw, boolean udb)
	{
		decode(raw, udb);
	}

	public Pronto(IRSignal sig)
	{
		super(sig);
		if (sig instanceof Pronto)
		{
			is0100 = ((Pronto)sig).is0100;
			raw = ((Pronto)sig).raw;
		}
	}

	public boolean isRawSignal()
	{
		return raw != null;
	}

	public byte[] getRawSignal()
	{
		return raw;
	}

	public String toString()
	{
		if (isRawSignal())
		{
			return "Pronto "+encode();
		}
		else
		{
			return super.toString();
		}
	}

	public boolean equals(Object o)
	{
		if (isRawSignal() && o instanceof Pronto)
		{
			Pronto p = (Pronto)o;
			if (p.isRawSignal())
			{
				byte cmp[] = p.getRawSignal();
				if (cmp.length != raw.length)
				{
					return false;
				}
				for (int i=0; i<cmp.length; i++)
				{
					if (cmp[i] != raw[i])
					{
						return false;
					}
				}
			}
		}
		return super.equals(o);
	}

	private String print(byte b[])
	{
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<b.length; i++)
		{
			if (i > 0) { sb.append(", "); }
			sb.append(Integer.toString((int)(b[i]&0xff)));
		}
		return sb.toString();
	}

	private int word(byte b[], int off)
	{
		return ((b[off*2]&0xff)<<8|(b[off*2+1]&0xff));
	}

	private void biphaseAddBit(StringBuffer sb, boolean bit)
	{
		sb.append(bit ? "01" : "10");
	}

	private void biphaseAddBits(StringBuffer sb, int v, int count)
	{
		for (int i=count-1; i>=0; i--)
		{
			biphaseAddBit(sb, ((v >>> i) & 0x1) == 0x1);
		}
	}

	private void decodeRC5(byte b[], boolean toggle)
	{
		// src: 5000 0000 0000 0001 SSSS CCCC (bytes)
		// dst: ss T SSSSS CCCCCC (bits) ss=(cmd > 64 ? 10 : 11) t=toggle
		// bi-phase: 0=10 1=01
		int type = word(b,0);
		int freq = word(b,1);  // usually 0
		int codO = word(b,2);  // once (always 0)
		int codR = word(b,3);  // repeat (always 1)
		StringBuffer bp = new StringBuffer();
		for (int i=0; i<codR; i++)
		{
			int sys  = word(b,4+(i*2)); // 0-31
			int cmd  = word(b,5+(i*2)); // 0-127
//System.out.println("rc5: sys="+sys+" cmd="+cmd);
			biphaseAddBit(bp, true);
			if (cmd > 64)
			{
				cmd -= 64;
				biphaseAddBit(bp, false);
			}
			else
			{
				biphaseAddBit(bp, true);
			}
			biphaseAddBit(bp, toggle);
			biphaseAddBits(bp, sys, 5);
			biphaseAddBits(bp, cmd, 6);
		}
		getPulseIndex().clear();
		getIntro().clear();
		getRepeat().clear();
		setFrequency(4194304/0x70);
		setMinRepeat(1);
		String bits = bp.toString();
//System.out.println("rc5: bits="+bits);
		if (bits.startsWith("0"))
		{
			bits = bits.substring(1);
		}
		int count = 1;
		char cc = bits.charAt(0);
		for (int i=1; i<bits.length(); i++)
		{
			if (cc == bits.charAt(i))
			{
				count++;
			}
			else
			{
				getRepeat().addTimePulse(count*0x70*0x20);
//System.out.println("rc5: add="+count);
				cc = bits.charAt(i);
				count = 1;
			}
		}
		getRepeat().addTimePulse(count*0x70*0x20);
//System.out.println("rc5: add="+count);
		getRepeat().addTimePulse(25*0x70*0x20); // delay
	}

	public boolean decode(byte b[])
	{
		int idx = 0;
		// decode pronto learned code
		if (b.length < 8)
		{
			this.raw = b;
			return false;
		}
		int maxlen = b.length;
		boolean israw = true;
		switch (word(b,0))
		{
			case 0x0000: israw = false; break;
			case 0x0100: israw = false; is0100 = true; break;
			case 0x5000: maxlen = word(b,3) == 2 ? 16 : 12; decodeRC5(b, false); break;
			//case 0x5000: israw = false; decodeRC5(b, false); return true;
			case 0x5001: maxlen = 16; break;
			case 0x6000: maxlen = 16; break;
			case 0x6001: maxlen = 16; break;
			case 0x7000: break; // TODO
			case 0x8000: maxlen = word(b,2) == 2 ? 16 : 8; break;
			case 0x9000: maxlen = 16; break;
		}
		if (israw)
		{
			if (b.length > maxlen)
			{
				byte nb[] = new byte[maxlen];
				System.arraycopy(b,0,nb,0,maxlen);
				b = nb;
			}
			this.raw = b;
			return false;
		}
		idx = 2;
		getPulseIndex().clear();
		getIntro().clear();
		getRepeat().clear();
		int fbas = ((b[idx++]<<8)|(b[idx++]&0xff));
		setFrequency(4194304/Math.max(fbas,1));
		int seq1 = ((b[idx++]<<8)|(b[idx++]&0xff)) * 2;
		int seq2 = ((b[idx++]<<8)|(b[idx++]&0xff)) * 2;
		for (int i=0; i<seq1; i++)
		{
			int b1 = b[idx++];
			int b2 = b[idx++];
			int v = ((b1&0xff)<<8) | (b2&0xff);
			int fv = fbas * v;
			getIntro().addTimePulse(fv);//(((b[idx++]&0xff)<<8)|(b[idx++]&0xff))*fbas);
		}
		for (int i=0; i<seq2; i++)
		{
			int b1 = b[idx++];
			int b2 = b[idx++];
			int v = ((b1&0xff)<<8) | (b2&0xff);
			int fv = fbas * v;
			getRepeat().addTimePulse(fv);//(((b[idx++]&0xff)<<8)|(b[idx++]&0xff))*fbas);
		}
		setMinRepeat(getIntro().cullRepeats()+getRepeat().cullRepeats()+1);
		cleanup();
		return true;
	}

	public boolean decode(String msg)
	{
		return decode(msg, false);
	}

	public boolean decode(String msg, boolean udb)
	{
		// decode pronto learned code
		StringTokenizer st = new StringTokenizer(msg, " \n");
		int len = st.countTokens();
		// skip udb index codes
		if (udb && len > 3)
		{
			len -= 3;
			for (int i=0; i<3; i++)
			{
				st.nextToken();
			}
		}
		byte b[] = new byte[len*2];
		for (int i=0; st.hasMoreTokens(); i += 2)
		{
			String t = st.nextToken();
			int v = value(t) & 0xffff;
			b[i] = (byte)((v >> 8) & 0xff);
			b[i+1] = (byte)(v & 0xff);
		}
		return decode(b);
	}

	public String encode()
	{
		return encode(VERSION1, false);
	}

	public String encode(int ver)
	{
		return encode(ver, false);
	}

	public String encode(int ver, boolean breaks)
	{
		StringBuffer sb = new StringBuffer();
		if (isRawSignal())
		{
			for (int i=0; i<raw.length; i+=2)
			{
				sb.append(hex((((raw[i]&0xff)<<8)|(raw[i+1]&0xff))&0xffff));
			}
			return sb.toString();
		}

		int freq = 4194304/getFrequency();
		int puls[] = getPulseIndex().getIndexValues();
		int base[] = getIntro().getPulses();
		int rept[] = getRepeat().getPulses();
		if (ver == VERSION2)
		{
			sb.append(hex(0));
			sb.append(hex(0));
			sb.append(hex(0));
			sb.append(is0100 ? hex(0x0100) : hex(0));
		}
		else
		{
			sb.append(is0100 ? hex(0x0100) : hex(0));
		}
		int minrep = Math.max(getMinRepeat(),1);
		sb.append(hex(freq));
		int typ = getRepeatType();
		switch (typ)
		{
			case REPEAT_NORM:
				sb.append(hex(base.length/2));
				sb.append(hex((rept.length*minrep)/2));
				break;
			case REPEAT_FULL:
				sb.append(hex(0));
				sb.append(hex(((base.length+rept.length)*minrep)/2));
				break;
			case REPEAT_NONE:
				sb.append(hex(((base.length+rept.length)*minrep)/2));
				sb.append(hex(0));
				break;
		}
		int pos = 8;
		for (int j=0; j<minrep; j++)
		{
			if ( (typ == REPEAT_NORM && j == 0) || typ != REPEAT_NORM )
			{
				for (int i=0; i<base.length; i++)
				{
					sb.append(hex(puls[base[i]]/freq));
					if (breaks && pos++ % 8 == 0)
					{
						sb.append("\n");
					}
				}
			}
			for (int i=0; i<rept.length; i++)
			{
				sb.append(hex(puls[rept[i]]/freq));
				if (breaks && pos++ % 8 == 0)
				{
					sb.append("\n");
				}
			}
		}
		return sb.toString();
	}

	private static String hex(int val)
	{
		String s = Integer.toString(val, 16);
		if (s.length() < 4)
		{
			return "0000".substring(s.length())+s+" ";
		}
		else
		{
			return s+" ";
		}
	}

	private static int value(String s)
	{
		return Integer.valueOf(s,16).intValue();
	}
}

class CodeTable extends Vector
{
	private int freqBase;

	public CodeTable(int freqBase)
	{
		this.freqBase = freqBase;
	}

	public int addPulse(int pulse)
	{
		for (int i=0; i<size(); i++)
		{
			int iv = ((Integer)elementAt(i)).intValue();
			if (Math.abs(pulse-iv) <= iv/10)
			{
				return i;
			}
		}
		addElement(new Integer(pulse));
		return size()-1;
	}

	public int getPulseTime(int index)
	{
		return getPulseTime(index, freqBase);
	}

	public int getPulseTime(int index, int freqBase)
	{
		long val = (long)((Integer)elementAt(index)).intValue();
		return (int)(val*freqBase);
	}

	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<size(); i++)
		{
			if (i > 0)
			{
				sb.append(",");
			}
			sb.append(elementAt(i)+"="+getPulseTime(i));
		}
		return sb.toString();
	}

	public int[] getPulses()
	{
		return getPulses(freqBase);
	}

	public int[] getPulses(int freqBase)
	{
		int sz = size();
		int ct[] = new int[sz];
		for (int i=0; i<sz; i++)
		{
			ct[i] = getPulseTime(i, freqBase);
		}
		return ct;
	}
}

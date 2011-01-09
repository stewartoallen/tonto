/*
 * Copyright 2000-2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.irdb;

// ---( imports )---
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.StringTokenizer;

public class IRBurstCode
{
	// ---( static fields )---

	// ---( static methods )---
	private static void debug(String msg)
	{
		System.out.println(msg);
	}

	public static IRBurstCode createCode(IRBurst ib)
	{
		IRBurstCode bc = new IRBurstCode(ib);
		return bc.valid ? bc : null;
	}

	// ---( constructors )---
	public IRBurstCode (IRSignal signal)
	{
		this.signal = signal;
	}

	private int len(PulsePair pp)
	{
		PulseIndex index = signal.getPulseIndex();
		return index.getPulse(pp.getBit0())+index.getPulse(pp.getBit1());
	}

	public IRBurstCode (IRBurst ib)
	{
		valid = true;
		signal = ib.getSignal();
		Vector trail = new Vector();
		PulsePair pp[] = ib.getPulsePairs();
		if (pp.length == 0)
		{
			valid = false;
			return;
		}
		for (int i=0; i<pp.length; i++)
		{
			trail.add(pp[i]);
			addPair(pp[i]);
		}
		bit0 = removeMostCommon();
		bit1 = removeMostCommon();
		if (bit0 == null || bit1 == null)
		{
			valid = false;
			return;
		}
		if (len(bit0) > len(bit1))
		{
			PulsePair tmp = bit0;
			bit0 = bit1;
			bit1 = tmp;
		}
		Vector body = new Vector();
		Enumeration e = trail.elements();
		PulsePair p = (PulsePair)e.nextElement();
		while (p != null && !(p.equals(bit0) || p.equals(bit1)))
		{
			head.add(p);
			p = e.hasMoreElements() ? (PulsePair)e.nextElement() : null;
		}
		while (p != null && (p.equals(bit0) || p.equals(bit1)))
		{
			body.add(p);
			p = e.hasMoreElements() ? (PulsePair)e.nextElement() : null;
		}
		while (p != null && !(p.equals(bit0) || p.equals(bit1)))
		{
			tail.add(p);
			p = e.hasMoreElements() ? (PulsePair)e.nextElement() : null;
		}
		if (e.hasMoreElements())
		{
//			debug("signal body is not pure");
			valid = false;
			return;
		}
		bits = body.size();
		for (long i=0; i<bits; i++)
		{
			if (body.get((int)i).equals(bit1))
			{
				code = code | (1l << (bits-i-1l));
			}
		}
	}

	private PulsePair removeMostCommon()
	{
		PulsePair p = null;
		int cnt = 0;
		for (Enumeration k = pairs.keys(); k.hasMoreElements(); )
		{
			PulsePair kp = (PulsePair)k.nextElement();
			int kv = ((Integer)pairs.get(kp)).intValue();
			if (kv > cnt)
			{
				p = kp;
				cnt = kv;
			}
		}
		if (p == null)
		{
			return null;
		}
		pairs.remove(p);
		return p;
	}

	// ---( instance fields )---
	private IRSignal signal;
	private boolean valid;
	private int bits;
	private long code;
	private PulsePair bit0;
	private PulsePair bit1;
	private Vector head = new Vector();
	private Vector tail = new Vector();
	private Hashtable pairs = new Hashtable();

	// ---( instance methods )---
	private boolean same(PulsePair p1, PulsePair p2)
	{
		if (p1 == null || p2 == null)
		{
			return p1 == p2;
		}
		return p1.equals(p2);
	}

	private boolean same(Vector v1, Vector v2)
	{
		if (v1 == null || v2 == null)
		{
			return v1 == v2;
		}
		if (v1.size() != v2.size())
		{
			return false;
		}
		for (int i=0; i<v1.size(); i++)
		{
			PulsePair p1 = ((PulsePair)v1.get(i));
			PulsePair p2 = ((PulsePair)v2.get(i));
			if (!p1.equals(p2))
			{
				return false;
			}
		}
		return true;
	}

	public boolean similar(IRBurstCode bc)
	{
		return
			bc.getBitCount() == getBitCount() &&
			same(bc.bit0, bit0) &&
			same(bc.bit1, bit1) &&
			same(bc.head, head) &&
			same(bc.tail, tail);
	}

	/**
	 * Swap the zero and one bits.
	 */
	public void swapBits()
	{
		for (long i=0; i<bits; i++)
		{
			long bv = (1l << (bits-i-1l));
			code = code ^ bv;
		}
		PulsePair temp = bit0;
		bit0 = bit1;
		bit1 = temp;
	}

	/**
	 * Return the number of bits in the code.
	 */
	public int getBitCount()
	{
		return bits;
	}

	/**
	 * Sets the number of bits in the code.
	 */
	public void setBitCount(int bc)
	{
		bits = bc;
	}

	/**
	 * Return the code derived from the pulse pairs.
	 */
	public long getValue()
	{
		return code;
	}

	/**
	 * Set the code to be encoded in pulse pairs.
	 */
	public void setValue(long nc)
	{
		code = nc;
	}

	/**
	 * Return the PulsePair associated with the off bit.
	 */
	public PulsePair getBit0()
	{
		return bit0;
	}

	/**
	 * Set the PulsePair associated with the off bit.
	 */
	public void setBit0(PulsePair pp)
	{
		bit0 = pp;
	}

	/**
	 * Return the PulsePair associated with the on bit.
	 */
	public PulsePair getBit1()
	{
		return bit1;
	}

	/**
	 * Set the PulsePair associated with the on bit.
	 */
	public void setBit1(PulsePair pp)
	{
		bit1 = pp;
	}

	/**
	 * Return the set of PulsePairs before the code.
	 */
	public PulsePair[] getHead()
	{
		return (PulsePair[])head.toArray();
	}

	public String getHeadString()
	{
		return seq(head);
	}

	/**
	 * Set the set of PulsePairs before the code.
	 */
	public void setHead(PulsePair pp[])
	{
		if (pp == null)
		{
			return;
		}
		head = new Vector();
		for (int i=0; i<pp.length; i++)
		{
			head.add(pp[i]);
		}
	}

	/**
	 * Set the set of PulsePairs before the code.
	 */
	public void setHead(Vector head)
	{
		this.head = head;
	}

	/**
	 * Return the set of PulsePairs after the code.
	 */
	public PulsePair[] getTail()
	{
		return (PulsePair[])head.toArray();
	}

	public String getTailString()
	{
		return seq(tail);
	}

	/**
	 * Set the set of PulsePairs after the code.
	 */
	public void setTail(Vector tail)
	{
		this.tail = tail;
	}

	/**
	 * Set the set of PulsePairs after the code.
	 */
	public void setTail(PulsePair pp[])
	{
		if (pp == null)
		{
			return;
		}
		tail = new Vector();
		for (int i=0; i<pp.length; i++)
		{
			tail.add(pp[i]);
		}
	}

	/**
	 * Creates an IRBurst from the parameters of the IRBurstCode.
	 */
	public IRBurst getIRBurst()
	{
		Vector d = new Vector();
		for (int i=0; i<head.size(); i++)
		{
			d.add(head.get(i));
		}
		for (long i=0; i<bits; i++)
		{
			long bv = (1l << (bits-i-1l));
			d.add((code & bv) == bv ? bit1 : bit0);
		}
		for (int i=0; i<tail.size(); i++)
		{
			d.add(tail.get(i));
		}
		Object o[] = d.toArray();
		PulsePair pp[] = new PulsePair[o.length];
		for (int i=0; i<o.length; i++)
		{
			pp[i] = (PulsePair)o[i];
		}
		int ib[] = new int[pp.length*2];
		for (int i=0; i<pp.length; i++)
		{
			ib[i*2+0] = pp[i].getBit0();
			ib[i*2+1] = pp[i].getBit1();
		}
		return new IRBurst(signal, ib);
	}

	// ---( private methods )---
	private void addPair(PulsePair np)
	{
		Integer cnt = (Integer)pairs.get(np);
		if (cnt != null)
		{
			pairs.put(np, new Integer(cnt.intValue()+1));
		}
		else
		{
			pairs.put(np, new Integer(1));
		}
	}

	private String bits(int num, long val)
	{
		char c[] = new char[num];
		for (long i=0; i<num; i++)
		{
			long bv = (1l << (num-i-1l));
			c[(int)i] = (val & bv) == bv ? '1' : '0';
		}
		return new String(c,0,c.length);
	}

	private String seq(Vector v)
	{
		StringBuffer sb = new StringBuffer();
		for (Enumeration e = v.elements(); e.hasMoreElements(); )
		{
			sb.append(e.nextElement().toString());
		}
		return sb.toString();
	}

	public void decode(String str)
	{
		Tokenizer st = new Tokenizer(str.trim());
		bits = Integer.parseInt(st.next());
		st.next(); // skip bits display
		code = Long.parseLong(st.next(),16);
		bit0 = parsePulsePair(st.next());
		bit1 = parsePulsePair(st.next());
		head = parsePulsePairs(st.next());
		tail = parsePulsePairs(st.next());
	}

	public static PulsePair parsePulsePair(String s)
	{
		s = s.trim();
		return new PulsePair(s.charAt(0)-'0', s.charAt(1)-'0');
	}

	public static Vector parsePulsePairs(String s)
	{
		s = s.trim();
		int l = s.length()/2;
		Vector pp = new Vector();
		for (int i=0; i<l; i++)
		{
			pp.add(new PulsePair(s.charAt(i*2)-'0', s.charAt(i*2+1)-'0'));
		}
		return pp;
	}

	public String getBitString()
	{
		return bits(bits,code);
	}

	public static long decodeBitString(String s)
	{
		s = s.trim();
		long l = 0;
		long b = s.length();
		for (long i=0; i<b; i++)
		{
			if (s.charAt((int)i) == '1')
			{
				l = l | (1l << (b-i-1l));
			}
		}
		return l;
	}

	public String toShortString()
	{
		return
			bits+","+Long.toHexString(code)+","+
			bit0+","+bit1+","+seq(head)+","+seq(tail);
	}

	public String toString()
	{
		return
			bits+","+bits(bits,code)+","+Long.toHexString(code)+","+
			bit0+","+bit1+","+seq(head)+","+seq(tail);
	}

	// ---( interface methods )---

	// ---( inner classes )---
	private class Tokenizer
	{
		private String str;
		private int pos;

		Tokenizer(String str)
		{
			this.str = str;
			this.pos = 0;
		}

		String next()
		{
			int np = str.indexOf(',',pos);
			if (np >= 0)
			{
				String ret = str.substring(pos, np);
				pos = np+1;
				return ret;
			}
			else
			{
				return str.substring(pos);
			}
		}
	}

}


/*
 * Copyright 2000-2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.irdb;

// ---( imports )---
import java.util.StringTokenizer;

public class PulseIndex
{
	// ---( static fields )---

	// ---( static methods )---

	// ---( constructors )---
	public PulseIndex ()
	{
		index = new IntArray();
	}

	/**
	 * @param values pulse length values
	 */
	public PulseIndex (int vals[])
	{
		this();
		addPulses(vals);
	}

	// for cloning
	private PulseIndex (IntArray idx)//, IntArray idxCnt)
	{
		index = idx;
	}

	// ---( interface methods )---

	// ---( instance fields )---
	private IntArray index;

	// ---( instance methods )---
	public String toString()
	{
		return index.getString(",");
	}

	public static PulseIndex parseIndexString(String str)
	{
		StringTokenizer st = new StringTokenizer(str,",");
		int v[] = new int[st.countTokens()];
		for (int i=0; st.hasMoreTokens(); i++)
		{
			v[i] = Integer.parseInt(st.nextToken());
		}
		return new PulseIndex(v);
	}

	public PulseIndex getClone()
	{
		return new PulseIndex(index.getClone());
	}

	public void setPulses(int vals[])
	{
		index.setSize(0);
		addPulses(vals);
	}

	public void addPulses(int vals[])
	{
		for (int i=0; i<vals.length; i++)
		{
			addPulse(vals[i]);
		}
	}

	/**
	 * returns the index of the pulse that was added or matched.
     *
	 * @param pulse length of pulse to add
	 */
	public int addPulse(int pulse)
	{
		for (int i=0; i<index.size(); i++)
		{
			int iv = index.get(i);
			if (Math.abs(pulse - iv) <= iv/10)
			{
				return i;
			}
		}
		index.append(pulse);
		return index.size()-1;
	}

	/**
	 * @param idx index of pulse
	 * @return length of pulse
	 */
	public int getPulse(int idx)
	{
		if (idx < 0 || idx > index.size()-1)
		{
			return 0;
		}
		return index.get(idx);
	}

	/**
	 * @param idx index of pulse
	 * @param value length of pulse
	 */
	public void setPulse(int idx, int value)
	{
		index.set(idx, value);
	}

	/**
	 * @return array of pulse lengths
	 */
	public int[] getIndexValues()
	{
		return index.getValues();
	}

	/**
	 * @return the number of unique pulses.
	 */
	public int size()
	{
		return index.size();
	}

	/**
	 * clear all pulses.
	 */
	public void clear()
	{
		index.setSize(0);
	}

	public void round(int closest)
	{
		int v[] = getIndexValues();
		for (int i=0; i<v.length; i++)
		{
			v[i] = round(v[i], closest);
		}
		setPulses(v);
	}

	public static int round(int num, int tick)
	{
		int n1 = num - (num % tick);
		int n2 = n1 + tick;
		if (Math.abs(num-n1) < Math.abs(num-n2))
		{
			return n1;
		}
		else
		{
			return n2;
		}
	}
}


/*
 * Copyright 2000-2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.irdb;

// ---( imports )---

public class IRBurst
{
	// ---( constructors )---
	IRBurst(IRSignal signal)
	{
		this.pulses = new IntArray();
		this.signal = signal;
		this.pulses = new IntArray();
	}

	IRBurst(IRSignal signal, int data[])
	{
		this(signal);
		setPulses(data);
	}

	// ---( instance fields )---
	private IRBurstCode burstCode;
	private IRSignal signal;
	private IntArray pulses;

	// ---( instance methods )---
	public IRBurst getClone(IRSignal sig)
	{
		return new IRBurst(sig, pulses.getValues());
	}

	public IRBurstCode[] split()
	{
		int p[] = getPulses();
		if (p.length > 0 && p.length % 2 == 0)
		{
			int b1[] = new int[p.length/2];
			int b2[] = new int[p.length/2];
			System.arraycopy(p,0,b1,0,b1.length);
			System.arraycopy(p,b1.length,b2,0,b2.length);
			IRBurstCode bc[] = new IRBurstCode[] {
				new IRBurst(signal, b1).getBurstCode(),
				new IRBurst(signal, b2).getBurstCode(),
			};
			if (bc[0] != null && bc[1] != null && bc[0].similar(bc[1]))
			{
				return bc;
			}
			else
			{
				return null;
			}
		}
		return null;
	}

	public boolean subtract(IRBurst burst)
	{
		if (burst == null)
		{
			return false;
		}
		int p1[] = getPulses();
		int p2[] = burst.getPulses();
		if (p1.length == 0 || p2.length == 0 || p2.length > p1.length)
		{
			return false;
		}
		int mlen = p1.length - p2.length;
		for (int i=0; i<mlen+1; i++)
		{
			if (match(p1, i, p2))
			{
				int np[] = new int[mlen];
				System.arraycopy(p1, 0, np, 0, i);
				System.arraycopy(p1, i+p2.length, np, i, p1.length-p2.length-i);
				setPulses(np);
				return true;
			}
		}
		return false;
	}

	private boolean match(int src[], int off, int match[])
	{
		if (src.length-off < match.length)
		{
			return false;
		}
		for (int i=0; i<match.length; i++)
		{
			if (src[i+off] != match[i])
			{
				return false;
			}
		}
		return true;
	}

	public boolean equals(Object o)
	{
		if (o instanceof IRBurst)
		{
			int c1[] = ((IRBurst)o).getPulses();
			int p1[] = ((IRBurst)o).getPulseIndex().getIndexValues();
			int c2[] = getPulses();
			int p2[] = getPulseIndex().getIndexValues();
			if (c1.length != c2.length)
			{
				return false;
			}
			for (int i=0; i<c1.length; i++)
			{
				if (!IRSignal.inRange(p1[c1[i]], p2[c2[i]], 20))
				{
					return false;
				}
			}
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Return the PulseIndex offset of the specified pulse element.
	 */
	public int get(int idx)
	{
		return pulses.get(idx);
	}

	/**
	 * Return the parent signal for this burst.
	 */
	public IRSignal getSignal()
	{
		return signal;
	}

	/**
	 * Return the pulse index for this burst.
	 */
	public PulseIndex getPulseIndex()
	{
		return signal.getPulseIndex();
	}

	public void setPulses(int p[])
	{
		burstCode = null;
		pulses.setSize(0);
		pulses.append(p);
	}

	public int[] getPulses()
	{
		return pulses.getValues();
	}

	public PulsePair[] getPulsePairs()
	{
		int p[] = getPulses();
		int len = p.length / 2;
		PulsePair pp[] = new PulsePair[len];
		for (int i=0; i<len; i++)
		{
			pp[i] = new PulsePair(p[i*2],p[i*2+1]);
		}
		return pp;
	}

	public int[] getPulseValues()
	{
		int idx[] = getPulseIndex().getIndexValues();
		int pul[] = getPulses();
		int val[] = new int[pul.length];
		for (int i=0; i<val.length; i++)
		{
			val[i] = idx[pul[i]];
		}
		return val;
	}

	/**
	 * Returns an IRBurstCode corresponding to this IRBurst or
	 * null of this burst is not compatible with the IRBurstCode encoding.
	 */
	public IRBurstCode getBurstCode()
	{
		if (burstCode == null)
		{
			burstCode = IRBurstCode.createCode(this);
		}
		return burstCode;
	}

	/**
	 * Returns the total length of the pulse.
	 */
	public int getBurstLength()
	{
		int val[] = getPulseValues();
		int len = 0;
		for (int i=0; i<val.length; i++)
		{
			len += val[i];
		}
		return len;
	}

	/**
	 * Returns the number of perfectly repeating segments in the burst
	 * and trims the burst to the first occurance. The burst must be
	 * 30 more pulses (15 or more pulse pairs).
	 */
	public int cullRepeats()
	{
		int p[] = getPulses();
		int l = p.length;
		if (l < 30 || (l % 2) != 0)
		{
			return 0;
		}
		burstCode = null;
		foo: for (int i=10; i>1; i--)
		{
			if (l % i != 0)
			{
				continue;
			}
			int cl = l/i;
			for (int j=0; j<cl; j++)
			{
				for (int k=1; k<i; k++)
				{
					if (p[j] != p[k*cl+j])
					{
						continue foo;
					}
				}
			}
			pulses.setSize(cl);
			return i-1;
		}
		return 0;
	}

	/**
	 * Return the length of the burst in pulses. Divide by two to
	 * get the number of pulse pairs.
	 */
	public int length()
	{
		return pulses.size();
	}

	public boolean empty()
	{
		return length() == 0;
	}

	/**
	 * Clear the burst.
	 */
	public void clear()
	{
		burstCode = null;
		setLength(0);
	}

	/**
	 * Set the burst length in pulses. Must be an even number to
	 * allow all pulses to be paired up.
	 */
	public void setLength(int length)
	{
		burstCode = null;
		pulses.setSize(length);
	}

	/**
	 * Add a pulse using the time length of the pulse.
	 *
	 * @param length length of pulse
	 */
	public void addTimePulse(int length)
	{
		burstCode = null;
		pulses.append(getPulseIndex().addPulse(length));
	}

	/**
	 * Add a pulse using the index position of the pulse.
	 *
	 * @param index index position of the pulse from the PulseIndex
	 */
	public void addIndexPulse(int idx)
	{
		burstCode = null;
		pulses.append(idx);
	}

	public void decodePulseString(String str)
	{
		str = str.trim();
		int d[] = new int[str.length()];
		for (int i=0; i<d.length; i++)
		{
			d[i] = str.charAt(i)-'0';
		}
		setPulses(d);
	}

	public String getPulseString()
	{
		return pulses.getString(null);
	}

	public String toString()
	{
//		return getPulseIndex().toString()+":"+getPulseString();
		return getPulseString();
	}
}


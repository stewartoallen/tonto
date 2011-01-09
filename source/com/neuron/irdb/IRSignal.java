/*
 * Copyright 2000-2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.irdb;

// ---( imports )---
import java.util.*;

/*
 * TODO:
 *
 *  - cleanup helper class names and methods
 *  - look for clean repeats in repeat segments when decoding (pronto)
 *  - look for intro in repeat segment then remove (pronto)
 */
public class IRSignal
{
	private final static int TYPE_OLD = 0; // {x1,x2,x3;y;z;etc}   no versioning
	private final static int TYPE_NEW = 1; // v;f;x1,x2,x3;y;z;etc versioning

	public final static int REPEAT_NORM = 0;
	public final static int REPEAT_FULL = 1;
	public final static int REPEAT_NONE = 2;

	private String name;
	private int type;
	private int freq;
	private PulseIndex index;
	private IRBurst intro;
	private IRBurst repeat;
	private int minRepeat;
	private int repeatType;

	// ---( static methods )---
	public static boolean inRange(int v1, int v2, int pct)
	{
		if (v1 == v2) { return true; }
		double PCT = ((double)pct/100.0);
		int diff = Math.abs(v1-v2);
		return (diff < (v1*PCT)) || (diff < (v2*PCT));
	}

	// ---( constructors )---
	public IRSignal()
	{
		name = "";
		index = new PulseIndex();
		intro = new IRBurst(this);
		repeat = new IRBurst(this);
		setFrequency(38500);
		index.setPulses(new int[] { 1500,3500,7000,50000 });
	}

	public IRSignal(IRSignal sig)
	{
		copyFrom(sig);
	}

	public IRSignal getClone()
	{
		return new IRSignal(this);
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name.replace('=','_').replace(' ','_');
	}
	
	// takes the form:
	//   {freq;p0,p1,p2,...;intro;repeat}
	// where freq is a frequency in Hertz
	// where intro and repeat use p# indexes in sequence
	//   eg: 010101020202030201
	public boolean decode(String raw)
	{
		if (raw == null)
		{
			return false;
		}
		raw = raw.trim();
		if (raw.length() == 0)
		{
			return false;
		}
		StringTokenizer st = new StringTokenizer(raw, "{;}", true);
		String stype = st.nextToken();
		if (stype.equals("{"))
		{
			// skip '{'
			type = TYPE_OLD;
		}
		else
		{
			type = Integer.parseInt(stype);
			st.nextToken(); // skip ';'
			setFlags(st.nextToken());
			st.nextToken(); // skip ';'
		}
		setFrequency(Integer.parseInt(st.nextToken()));
		st.nextToken(); // skip ';'
		StringTokenizer sl = new StringTokenizer(st.nextToken().trim(), ",");
		int pulses[] = new int[sl.countTokens()];
		for (int i=0; i<pulses.length; i++)
		{
			pulses[i] = Integer.parseInt(sl.nextToken());
		}
		index = new PulseIndex(pulses);
		st.nextToken(); // skip ';'
		String intStr = st.nextToken();
		if (intStr.equals(";"))
		{
			intStr = "";
		}
		else
		{
			st.nextToken(); // skip ';'
		}
		String repStr = st.nextToken();
		if (repStr.equals(";"))
		{
			repStr = "";
		}
		else
		{
			st.nextToken(); // skip ';'
		}
		if (intStr.indexOf(',') >= 0)
		{
			IRBurstCode code = createBurstCode();
			code.decode(intStr);
			intro = code.getIRBurst();
		}
		else
		{
			intro = new IRBurst(this);
			for (int i=0; i<intStr.length(); i++)
			{
				intro.addIndexPulse(intStr.charAt(i) - '0');
			}
		}
		if (repStr.indexOf(',') >= 0)
		{
			IRBurstCode code = createBurstCode();
			code.decode(repStr);
			repeat = code.getIRBurst();
		}
		else
		{
			repeat = new IRBurst(this);
			for (int i=0; i<repStr.length(); i++)
			{
				repeat.addIndexPulse(repStr.charAt(i) - '0');
			}
		}
		minRepeat = Integer.parseInt(st.nextToken());
		return true;
	}

	public String encode()
	{
		return toString();
	}

	// F = full repeat
	// N = no repeat
	// S = standard (default)
	public void setFlags(String flags)
	{
		if (flags == null)
		{
			return;
		}
		flags = flags.trim();
		if (flags.length() == 0)
		{
			return;
		}
		switch (flags.charAt(0))
		{
			case 'S': repeatType = REPEAT_NORM; break;
			case 'F': repeatType = REPEAT_FULL; break;
			case 'N': repeatType = REPEAT_NONE; break;
		}
	}

	public String getFlags()
	{
		switch (repeatType)
		{
			case REPEAT_NORM: return "S";
			case REPEAT_FULL: return "F";
			case REPEAT_NONE: return "N";
			default: return "";
		}
	}

	public int getRepeatType()
	{
		return repeatType;
	}

	public void setRepeatType(int type)
	{
		repeatType = type;
	}

	public void copyFrom(IRSignal sig)
	{
		setup(sig.getFrequency(),
			sig.getPulseIndex().getClone(),
			sig.getIntro().getClone(this),
			sig.getRepeat().getClone(this),
			sig.getMinRepeat(),
			sig.getRepeatType());
		setName(sig.getName());
	}

	private void setup(
		int fr, PulseIndex p, IRBurst i, IRBurst r, int mr, int rt)
	{
		freq = fr;
		index = p;
		intro = i;
		repeat = r;
		minRepeat = mr;
		repeatType = rt;
	}

	public int hashCode()
	{
		return intro.getPulseString().hashCode()+
			repeat.getPulseString().hashCode();
	}

	public boolean equals(Object o)
	{
		if (o instanceof IRSignal)
		{
			IRSignal s = (IRSignal)o;
			int freq = getFrequency();
			return inRange(freq, s.getFrequency(), 15) &&
				getIntro().equals(s.getIntro()) &&
				getRepeat().equals(s.getRepeat());
		}
		else
		{
			return false;
		}
	}

	// ---( IRSignal interface methods )---
	public int getFrequency()
	{
		return Math.max(freq,1);
	}

	public PulseIndex getPulseIndex()
	{
		return index;
	}
	
	public IRBurst getIntro()
	{
		return intro;
	}

	public IRBurst getRepeat()
	{
		return repeat;
	}

	public int getMinRepeat()
	{
		return minRepeat;
	}

	public int getMinSigTime()
	{
		return intro.getBurstLength() + repeat.getBurstLength();
	}

	public void setFrequency(int freq)
	{
		this.freq = freq;
	}

	public void setPulseIndex(PulseIndex index)
	{
		this.index = index;
	}
	
	public void setIntro(IRBurst intro)
	{
		this.intro = intro;
	}

	public void setRepeat(IRBurst repeat)
	{
		this.repeat = repeat;
	}

	public void setMinRepeat(int min)
	{
		this.minRepeat = min;
	}

	public IRBurst createBurst()
	{
		return new IRBurst(this);
	}

	public IRBurstCode createBurstCode()
	{
		return new IRBurstCode(this);
	}

	public void cleanup()
	{
		/*
		setFrequency(PulseIndex.round(getFrequency(), 250));
		getPulseIndex().round(250);
		*/
		IRBurst b = getIntro();
		IRBurst r = getRepeat();
		if (b.empty() && r.empty())
		{
			return;
		}
		if (!(b.empty() || r.empty()))
		{
			if (r.subtract(b))
			{
				setRepeatType(REPEAT_FULL);
			}
		}
		else
		if (b.empty())
		{
			IRBurstCode bc[] = r.split();
			if (bc != null)
			{
				setIntro(bc[0].getIRBurst());
				setRepeat(bc[1].getIRBurst());
				setRepeatType(REPEAT_FULL);
			}
		}
		else
		if (r.empty())
		{
			IRBurstCode bc[] = b.split();
			if (bc != null)
			{
				setIntro(bc[0].getIRBurst());
				setRepeat(bc[1].getIRBurst());
				setRepeatType(REPEAT_NONE);
			}
		}
	}

	// ---( instance methods )---
	public String toString()
	{
		IRBurstCode ic = intro.getBurstCode();
		IRBurstCode rc = repeat.getBurstCode();

		return
				TYPE_NEW+";"+
				getFlags()+";"+
				freq+";"+
				index+";"+
				(ic != null ? ic.toString() : intro.toString())+";"+
				(rc != null ? rc.toString() : repeat.toString())+";"+
				minRepeat;
	}

}


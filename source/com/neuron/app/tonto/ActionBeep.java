/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import javax.sound.midi.*;

/**
 * Action that emits a beep.
 */
public class ActionBeep extends CCFAction
{
	// ---( static fields )---

	// ---( static methods )---
	public static void main(String args[])
		throws Exception
	{
		/*
		Synthesizer s = MidiSystem.getSynthesizer();
		MidiChannel c[] = s.getChannels();
		for (int i=0; i<c.length; i++)
		{
			if (c[i] != null)
			{
				System.out.println("playing note on "+i);
				c[i].noteOn(60, 93);
			}
			Thread.currentThread().sleep(500);
		}
		System.exit(0);
		*/
	}

	// ---( constructors )---
	ActionBeep(CCFAction copy)
	{
		copy(copy);
	}

	public ActionBeep (int dur, int freq, int cycle)
	{
		type = ACT_BEEP;
		setBeep(dur, freq, cycle);
	}

	// ---( instance fields )---
	/**
	 * Get the duration of the Beep.
	 */
	public static int getDuration(int p2)
	{
		return range(((p2 >> 24) & 0xff) * 10, 0, 2550);
	}

	public int getDuration()
	{
		return range(((p2 >> 24) & 0xff) * 10, 0, 2550);
	}

	/**
	 * Get the frequency of the Beep.
	 */
	public static int getFrequency(int p2)
	{
		return range((p2 >> 8) & 0xffff, 0, 65535);
	}

	public int getFrequency()
	{
		return range((p2 >> 8) & 0xffff, 0, 65535);
	}

	/**
	 * Get the duty cycle of the Beep.
	 */
	public static int getDutyCycle(int p2)
	{
		return range(p2 & 0xff, 0, 100);
	}

	public int getDutyCycle()
	{
		return range(p2 & 0xff, 0, 100);
	}

	/**
	 * Set the beep parameters.
	 *
	 * @param dur duration of the beep in milliseconds
	 * in multiples of 10 up to 2550
	 * @param freq frequency of the beep in Hz up to 65535
	 * @param cycle duty cycle of the beep (0-100%)
	 */
	public void setBeep(int dur, int freq, int cycle)
	{
		p2 = createBeep(dur, freq, cycle);
	}

	// ---( instance methods )---
	void playBeep()
	{
	}

	int createBeep(int dur, int freq, int cycle)
	{
		return
			(((dur/10) & 0xff) << 24) |
			((freq & 0xffff) << 8) |
			((range(cycle,0,100) & 0xff));
	}

	private static int range(int num, int low, int high)
	{
		if (num < low) { return low; }
		if (num > high) { return high; }
		return num;
	}

	boolean useParentFields()
	{
		return true;
	}

	// ---( interface methods )---

}


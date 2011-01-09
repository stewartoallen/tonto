/*
 * Copyright 2000-2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.irdb;

// ---( imports )---

public class PulsePair
{
	// ---( static fields )---

	// ---( static methods )---

	// ---( constructors )---
	public PulsePair(int i0, int i1)
	{
		idx0 = i0;
		idx1 = i1;
	}

	// ---( instance fields )---
	private int idx0;
	private int idx1;

	// ---( instance methods )---
	public int getBit0()
	{
		return idx0;
	}

	public int getBit1()
	{
		return idx1;
	}

	public int hashCode()
	{
		return idx0*13 + idx1;
	}

	public boolean equals(Object o)
	{
		if (o instanceof PulsePair)
		{
			PulsePair p = (PulsePair)o;
			return p.idx0 == idx0 && p.idx1 == idx1;
		}
		return false;
	}

	public String toString()
	{
		return idx0+""+idx1;
	}

	// ---( interface methods )---

}


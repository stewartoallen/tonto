/*
 * Copyright 2000-2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.irdb;

// ---( imports )---

class IntArray
{
	private java.util.Vector vect;

	public IntArray()
	{
		vect = new java.util.Vector();
	}

	public IntArray(int vals[])
	{
		vect = new java.util.Vector();
		append(vals);
	}

	public IntArray getClone()
	{
		return new IntArray(getValues());
	}

	public void increment(int index, int val)
	{
		set(index, get(index) + val);
	}

	public void append(int i)
	{
		vect.addElement(new Integer(i));
	}

	public void append(int vals[])
	{
		for (int i=0; i<vals.length; i++)
		{
			append(vals[i]);
		}
	}

	public void remove(int idx)
	{
		vect.removeElementAt(idx);
	}

	public void setSize(int size)
	{
		vect.setSize(size);
	}

	public void insert(int index, int val)
	{
		vect.insertElementAt(new Integer(val), index);
	}

	public int get(int idx)
	{
		return ((Integer)vect.elementAt(idx)).intValue();
	}

	public void set(int index, int val)
	{
		vect.setElementAt(new Integer(val), index);
	}

	public int[] getValues()
	{
		int vals[] = new int[size()];
		for (int i=0; i<vals.length; i++)
		{
			vals[i] = get(i);
		}
		return vals;
	}

	public int size()
	{
		return vect.size();
	}

	public String getString(String sep)
	{
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<size(); i++)
		{
			if (i > 0 && sep != null)
			{
				sb.append(sep);
			}
			sb.append(vect.elementAt(i));
		}
		return sb.toString();
	}
}

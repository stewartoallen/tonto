/*
 * Copyright 2000-2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.irdb;

// ---( imports )---
import java.util.*;

public class IRRemote
{
	// ---( static fields )---
	public final static int SORT_NAME = 1;
	public final static int SORT_INTRO = 2;
	public final static int SORT_REPEAT = 3;

	// ---( static methods )---

	// ---( constructors )---
	public IRRemote (String model, String company, String desc)
	{
		setModel(model);
		setCompany(company);
		setDescription(desc);
	}

	public String toString()
	{
		return "["+model+","+company+","+desc+"]";
	}

	// ---( instance fields )---
	private String model;
	private String company;
	private String desc;
	private Vector keys = new Vector();

	// ---( instance methods )---
	public void sort(int type, boolean reverse)
	{
		if (type < 1 || type > 3)
		{
			return;
		}
		IRSignal s[] = new IRSignal[keys.size()];
		keys.toArray(s);
		final int stype = type;
		final int mult = (reverse ? -1 : 1);
		Arrays.sort(s, new Comparator() {
			public int compare(Object o1, Object o2) {
				if (!(o1 instanceof IRSignal && o2 instanceof IRSignal))
				{
					return 1;
				}
				IRSignal s1 = (IRSignal)o1;
				IRSignal s2 = (IRSignal)o2;
				IRBurstCode b1 = null;
				IRBurstCode b2 = null;
				switch (stype) {
					case SORT_NAME:
						String n1= s1.getName();
						String n2= s2.getName();
						if (n1 == null || n2 == null) {
							return 0;
						}
						return n1.compareTo(n2) * mult;
					case SORT_INTRO:
						b1 = s1.getIntro().getBurstCode();
						b2 = s2.getIntro().getBurstCode();
						break;
					case SORT_REPEAT:
						b1 = s1.getRepeat().getBurstCode();
						b2 = s2.getRepeat().getBurstCode();
						break;
				}
				long v1 = (b1 != null ? b1.getValue() : 0);
				long v2 = (b2 != null ? b2.getValue() : 0);
				if (v1 > v2) { return 1 * mult; }
				if (v2 > v1) { return -1 * mult; }
				return 0;
			}
			public boolean equals(Object o) {
				return o == this;
			}
		});
		keys.setSize(0);
		for (int i=0; i<s.length; i++)
		{
			keys.add(s[i]);
		}
	}

	private String valid(String nm)
	{
		if (nm == null)
		{
			return "";
		}
		return nm.replace(',','_');
	}

	public String getModel()
	{
		return model;
	}

	public void setModel(String model)
	{
		this.model = valid(model);
	}

	public String getCompany()
	{
		return company;
	}

	public void setCompany(String company)
	{
		this.company = valid(company);
	}

	public String getDescription()
	{
		return desc;
	}

	public void setDescription(String desc)
	{
		this.desc = valid(desc);
	}

	public void add(IRSignal key)
	{
		keys.add(key);
	}

	public boolean remove(int idx)
	{
		return keys.remove(idx) != null;
	}

	public void clear()
	{
		keys.clear();
	}

	public boolean remove(String name)
	{
		for (int i=0; i<keys.size(); i++)
		{
			if (((IRSignal)keys.get(i)).getName().equals(name))
			{
				keys.remove(i);
				return true;
			}
		}
		return false;
	}

	public IRSignal getByIndex(int idx)
	{
		return (IRSignal)keys.get(idx);
	}

	public IRSignal get(String name)
	{
		for (int i=0; i<keys.size(); i++)
		{
			if (((IRSignal)keys.get(i)).getName().equals(name))
			{
				return (IRSignal)keys.get(i);
			}
		}
		return null;
	}

	public Enumeration getKeys()
	{
		return keys.elements();
	}

	public int numKeys()
	{
		return keys.size();
	}

	// ---( interface methods )---

	// ---( inner classes )---
}


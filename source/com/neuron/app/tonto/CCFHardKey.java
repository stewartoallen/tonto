/*
 * Copyright 2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.lang.reflect.Field;

public class CCFHardKey
{
	// ---( static fields )---
	static Debug debug = Debug.getInstance("ccf");

	// ---( static methods )---

	// ---( constructors )---
	CCFHardKey (CCFDevice dev, String label, String name, String actions)
	{
		this.dev = dev;
		this.label = label;
		this.name = name;
		this.actions = actions;
	}

	// ---( instance fields )---
	private CCFDevice dev;
	private String label;
	private String name;
	private String actions;

	// ---( instance methods )---
	public String getLabel()
	{
		return label;
	}

	public boolean isNamable()
	{
		return name != null;
	}

	public String getName()
	{
		try
		{
			return isNamable() ? (String)(dev.getField(name).get(dev)) : null;
		}
		catch (Exception ex)
		{
			debug.log(0, "unable to get hard key name ("+label+")");
			return null;
		}
	}

	public void setName(String newname)
	{
		try
		{
			if (isNamable())
			{
				dev.getField(name).set(dev,newname);
			}
		}
		catch (Exception ex)
		{
			debug.log(0, "unable to set hard key name ("+label+")");
		}
	}

	public CCFActionList getActionList()
	{
		try
		{
			return (CCFActionList)(dev.getField(actions).get(dev));
		}
		catch (Exception ex)
		{
			debug.log(0, "unable to get hard key actions ("+label+")");
			return null;
		}
	}

	public void appendAction(CCFAction a)
	{
		CCFActionList al = getActionList();
		if (al == null)
		{
			al = new CCFActionList();
			setActionList(al);
		}
		al.appendAction(a);
	}

	public void setActionList(CCFActionList list)
	{
		try
		{
			dev.getField(actions).set(dev, list);
		}
		catch (Exception ex)
		{
			debug.log(0, "unable to set hard key actions ("+label+")");
		}
	}

	public void copyToDevice(CCFDevice ndev)
	{
		try
		{

		if (name != null)
		{
			ndev.getField(name).set(ndev, dev.getField(name).get(dev));
		}
		if (actions != null)
		{
			CCFActionList al = ((CCFActionList)dev.getField(actions).get(dev));
			ndev.getField(actions).set(ndev, al != null ? al.getClone() : null);
		}

		}
		catch (Exception ex)
		{
			debug.log(0, "unable to copy hard key ("+name+","+actions+") action to "+ndev);
		}
	}

	// ---( interface methods )---

}


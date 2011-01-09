/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.*;
import java.awt.*;
import java.util.*;

class Preferences extends Properties
{
	// ---( static fields )---

	// ---( static methods )---

	// ---( constructors )---
	Preferences ()
	{
	}

	// ---( instance fields )---

	// ---( instance methods )---
	public void load(File f)
		throws IOException
	{
		if (!f.exists())
		{
			return;
		}
		load(new FileInputStream(f));
	}

	public void save(File f)
		throws IOException
	{
		store(new FileOutputStream(f), "Tonto Preferences");
	}

	public Rectangle getRectangle(String prop, Rectangle def)
	{
		String val = getProperty(prop);
		if (val == null)
		{
			return def;
		}
		StringTokenizer st = new StringTokenizer(val,",");
		return new Rectangle(
			Integer.parseInt(st.nextToken()),
			Integer.parseInt(st.nextToken()),
			Integer.parseInt(st.nextToken()),
			Integer.parseInt(st.nextToken())
		);
	}

	public Dimension getDimension(String prop, Dimension def)
	{
		String val = getProperty(prop);
		if (val == null)
		{
			return def;
		}
		StringTokenizer st = new StringTokenizer(val,",");
		return new Dimension(
			Integer.parseInt(st.nextToken()),
			Integer.parseInt(st.nextToken())
		);
	}

	public int getInteger(String prop, int def)
	{
		String val = getProperty(prop);
		if (val == null)
		{
			return def;
		}
		return Integer.parseInt(val);
	}

	public double getDouble(String prop, double def)
	{
		String val = getProperty(prop);
		if (val == null)
		{
			return def;
		}
		return Double.parseDouble(val);
	}

	public boolean getBoolean(String prop, boolean def)
	{
		String val = getProperty(prop);
		if (val == null)
		{
			return def;
		}
		return val.equalsIgnoreCase("true");
	}

	public Object setProperty(String prop, String val)
	{
		if (val == null)
		{
			remove(prop);
			return null;
		}
		else
		{
			return super.setProperty(prop, val);
		}
	}

	public void setProperty(String prop, Rectangle rect)
	{
		put(prop, rect.x+","+rect.y+","+rect.width+","+rect.height);
	}

	public void setProperty(String prop, Dimension dim)
	{
		put(prop, dim.width+","+dim.height);
	}

	public void setProperty(String prop, int val)
	{
		put(prop, Integer.toString(val));
	}

	public void setProperty(String prop, double val)
	{
		put(prop, Double.toString(val));
	}

	public void setProperty(String prop, boolean val)
	{
		put(prop, val ? "true" : "false");
	}

	// ---( interface methods )---

}


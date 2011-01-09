/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

import java.awt.Dimension;
import java.util.Vector;

public class ProntoModel
{
	public final static int CUSTOM    = 0;
	public final static int TS1000    = 1;
	public final static int TSU2000   = 2;
	public final static int TSU6000   = 3;
	public final static int RC5000    = 4;
	public final static int RC5000i   = 5;
	public final static int RC5200    = 6;
	public final static int RC9200    = 7;
	public final static int RU890     = 8;
	public final static int RU940     = 9;
	public final static int RU970     = 10;
	public final static int USR5      = 11;
	public final static int RAV2K     = 12;
	public final static int RAV2KZ1   = 13;

	private final static ProntoModel units[] = {
		//                                     cap          mem  wid  hgt
		new ProntoModel (RC5000,   "RC5000",   0x0000001,   380, 240, 220),
		new ProntoModel (RC5000i,  "RC5000i",  0x0000001,  1404, 240, 220),
		new ProntoModel (RC5200,   "RC5200",   0x0000a01,  1220, 240, 270),
		new ProntoModel (RC9200,   "RC9200",   0x0000b01,  6900, 240, 270),
		new ProntoModel (TS1000,   "TS1000",   0x0020001,   380, 240, 220),
		new ProntoModel (TSU2000,  "TSU2000",  0x0060001,   960, 240, 220),
		new ProntoModel (TSU6000,  "TSU6000",  0x0060301,  5952, 240, 220),
		new ProntoModel (RU890,    "RU890",    0x0020001,   380, 240, 220),
		new ProntoModel (RU940,    "RU940",    0x0020001,  1404, 240, 220),
		new ProntoModel (RU970,    "RU970",    0x0060301,  5667, 240, 220),
		new ProntoModel (USR5,     "USR5",     0x0060201,   697, 240, 220),
		new ProntoModel (RAV2K,    "RAV2000",  0x0060001,   444, 240, 220),
		new ProntoModel (RAV2KZ1,  "RAV2000Z", 0x0060001,   772, 240, 220),
		new ProntoModel (CUSTOM,   "CUSTOM",   0x1020301, 99999, 240, 320),
	};

	private String name;
	private int model;
	private int cap;
	private int mem;
	private Dimension screen;

	private ProntoModel(int model, String name, int cap, int mem, int w, int h)
	{
		this.model = model;
		this.name = name;
		this.cap = cap;
		this.mem = mem * 1024;
		this.screen = new Dimension(w,h);
	}

	public static ProntoModel[] getModels()
	{
		return units;
	}

	public String getName()
	{
		return name;
	}

	public int getModel()
	{
		return model;
	}

	public int getCapability()
	{
		return cap;
	}

	public int getMemory()
	{
		return mem;
	}

	public Dimension getScreenSize()
	{
		return screen;
	}

	public static ProntoModel getModel(int model)
	{
		for (int i=0; i<units.length; i++)
		{
			if (units[i].model == model)
			{
				return units[i];
			}
		}
		throw new IllegalArgumentException();
	}

	public static ProntoModel getModelByName(String name)
	{
		if (name == null)
		{
			throw new IllegalArgumentException();
		}
		for (int i=0; i<units.length; i++)
		{
			if (units[i].name.equals(name))
			{
				return units[i];
			}
		}
		return units[4]; // TS1000
	}

	public static ProntoModel[] getModelByCapability(int cap)
	{
		Vector v = new Vector();
		for (int i=0; i<units.length; i++)
		{
			if (units[i].cap == cap)
			{
				v.add(units[i]);
			}
		}
		ProntoModel m[] = new ProntoModel[v.size()];
		v.copyInto(m);
		return m;
	}

	public boolean isCustom()
	{
		return model == CUSTOM;
	}

	public boolean isMarantz()
	{
		return (model == RC5000) || (model == RC5000i) || (model == RC5200) || (model == RC9200);
	}

	public String toString()
	{
		return name;
	}

}


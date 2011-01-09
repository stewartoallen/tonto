/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---

/**
 * A representation of valid CCF font sizes.
 */
public class CCFFont
{
	// ---( static fields )---
	public static CCFFont NONE     = new CCFFont(0);
	public static CCFFont SIZE_8   = new CCFFont(1);
	public static CCFFont SIZE_10  = new CCFFont(2);
	public static CCFFont SIZE_12  = new CCFFont(3);
	public static CCFFont SIZE_14  = new CCFFont(4);
	public static CCFFont SIZE_16  = new CCFFont(5);
	public static CCFFont SIZE_18  = new CCFFont(6);

	// ---( constructors )---
	private CCFFont (int size)
	{
		this.size = size;
	}

	public String toString()
	{
		return "Font["+size+"]";
	}

	// ---( public API )---
	public int getAWTSize()
	{
		return getAWTSize(size);
	}

	static int getAWTSize(int size)
	{
		switch (size)
		{
			case 0 : return 0;
			case 1 : return 8;
			case 2 : return 10;
			case 3 : return 12;
			case 4 : return 14;
			case 5 : return 16;
			case 6 : return 18;
		}
		return 12;
	}

	// ---( instance fields )---
	int size;

	// ---( instance methods )---
	int getFontSize()
	{
		return size;
	}

	void setFontSize(int sz)
	{
		size = sz;
	}

	// ---( static methods )---

	static CCFFont getFont(int num)
	{
		switch (num)
		{
			case 0 : return NONE;
			case 1 : return SIZE_8;
			case 2 : return SIZE_10;
			case 3 : return SIZE_12;
			case 4 : return SIZE_14;
			case 5 : return SIZE_16;
			case 6 : return SIZE_18;
		}
		return SIZE_8;
	}

	// ---( interface methods )---
}


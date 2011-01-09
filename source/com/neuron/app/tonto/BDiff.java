/*
 * Copyright 2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.*;
import java.util.*;

public class BDiff
{
	// ---( static fields )---

	// ---( static methods )---
	public static void main(String args[])
		throws Exception
	{
		FileInputStream f1 = new FileInputStream(args[0]);
		FileInputStream f2 = new FileInputStream(args[1]);
		int pos = 0;
		while (true)
		{
			int b1 = f1.read();
			int b2 = f2.read();
			if (b1 < 0 && b2 < 0)
			{
				break;
			}
			if (b1 < 0)
			{
				System.out.println("'"+args[0]+"' shorter");
				break;
			}
			if (b2 < 0)
			{
				System.out.println("'"+args[1]+"' shorter");
				break;
			}
			if (b1 != b2)
			{
				System.out.println(
					pad(pos,6)+pad("("+hex(pos)+")",9)+pad(hex(b1),6)+pad(hex(b2),6));
			}
			pos++;
		}
	}

	private static String hex(int val)
	{
		return "0x"+Integer.toHexString(val);
	}

	private static final String pstr = "                             ";

	private static String pad(int val, int pad)
	{
		return pad(Integer.toString(val), pad);
	}

	private static String pad(String s, int pad)
	{
		if (s.length() > pad)
		{
			return s.substring(0,pad);
		}
		if (s.length() == pad)
		{
			return s;
		}
		return s+pstr.substring(0,pad-s.length());
	}

	// ---( constructors )---

	// ---( instance fields )---

	// ---( instance methods )---

	// ---( interface methods )---

}


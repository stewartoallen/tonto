/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.*;
import java.util.*;

public class Regress
{
	// ---( static fields )---

	// ---( static methods )---
	public static void main(String args[])
		throws Exception
	{
		Regress r = new Regress(args[0]);
		r.test();
		Hashtable h = r.getErrors();
		if (h != null)
		{
			System.out.println("-- error summary --");
			for (Enumeration e = h.keys(); e.hasMoreElements(); )
			{
				File file = (File)e.nextElement();
				Throwable ex = (Exception)h.get(file);
				System.out.println("fail: "+file+" --> "+ex);
			}
			System.exit(1);
		}
		System.out.println("-- all passed --");
	}

	// ---( constructors )---
	public Regress (String d)
		throws IOException
	{
		this.dir = new File(d);
		if (!dir.exists())
		{
			throw new FileNotFoundException(d);
		}
	}

	// ---( instance fields )---
	private File dir;
	private Hashtable errors = new Hashtable();

	// ---( instance methods )---
	public void test()
	{
		test(dir);
	}

	private void test(File dir)
	{
		String l[] = dir.list();
		for (int i=0; i<l.length; i++)
		{
			File test = new File(dir, l[i]);
			if (test.isDirectory())
			{
				test(test);
			}
			else
			{
				if (l[i].toLowerCase().endsWith(".ccf"))
				{
					try
					{
						testCCF(test);
					}
					catch (Throwable ex)
					{
						errors.put(test, ex);
					}
				}
			}
		}
	}

	public void testCCF(File f)
		throws Throwable
	{
		long sz1 = f.length();
		long sz2 = 0;
		System.out.println("test : "+f);
		File tmp = File.createTempFile("out","ccf");
		tmp.delete();
		try
		{
			CCF ccf = new CCF();
			System.out.print("  read...");
			ccf.load(f.toString());
			System.out.print(" write...");
			ccf.save(tmp.toString());
			System.out.print(" reread...");
			ccf.load(tmp.toString());
			sz2 = tmp.length();
			if (Math.abs(sz2 - sz1) > sz1/20)
			{
				throw new RuntimeException("size mismatch: "+sz1+" -> "+sz2);
			}
		}
		catch (Throwable ex)
		{
			System.out.print(" FAIL ("+ex+")");
			throw ex;
		}
		finally
		{
			System.out.println();
			tmp.delete();
		}
	}

	public Hashtable getErrors()
	{
		return (errors.size() > 0 ? errors : null);
	}

	// ---( interface methods )---

}


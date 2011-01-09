/*
 * Copyright 2000-2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.irdb;

// ---( imports )---
import java.io.*;
import java.util.*;
import com.neuron.irdb.impl.*;

public class IRDatabase
{
	// ---( static fields )---
	private final static String pad = "                                             ";

	// ---( static methods )---
	public static IRDatabase create(String file)
		throws IOException
	{
		return new IRDatabase(file, false);
	}

	public static IRDatabase open(String file)
		throws IOException
	{
		return new IRDatabase(file, true);
	}

	public static void main(String args[])
		throws IOException
	{
		open(args[0]);
	}

	// ---( constructors )---
	private IRDatabase (String file, boolean open)
		throws IOException
	{
		this.file = file;
		this.remotes = new Vector();
		if (open)
		{
			try { read(file); } catch (Exception ex) { ex.printStackTrace(); }
		}
	}

	// ---( instance fields )---
	private String file;
	private Vector remotes;

	// ---( instance methods )---
	public void read(String file)
		throws IOException
	{
		if (!new File(file).exists())
		{
			return;
		}
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = null;
		IRRemote remote = null;
		while ( (line = br.readLine()) != null )
		{
			line = line.trim();
			if (line.length() == 0 || line.charAt(0) == '#')
			{
				continue;
			}
			if (line.charAt(0) == '[')
			{
				int idx = line.indexOf(']');
				if (idx < 0)
				{
					continue;
				}
				line = line.substring(1,idx);
				StringTokenizer st = new StringTokenizer(line, ",");
				String model = st.nextToken();
				String comp = st.hasMoreTokens() ? st.nextToken() : "unknown";
				String desc = st.hasMoreTokens() ? st.nextToken() : "";
				remote = new IRRemote(model, comp, desc);
				add(remote);
			}
			else
			if (remote != null)
			{
				int eq = line.indexOf('=');
				if (eq < 0)
				{
					continue;
				}
				String name = line.substring(0,eq).trim(); 
				String code = line.substring(eq+1).trim();
				if (code.startsWith("Pronto"))
				{
					Pronto pr = new Pronto(code.substring(7));
					pr.setName(name);
					remote.add(pr);
				}
				else
				{
					IRSignal sig = new IRSignal();
					sig.setName(name);
					sig.decode(code);
					remote.add(sig);
				}
			}
		}
	}

	public void write()
		throws IOException
	{
		write(file);
	}

	private String pad(String nm, int len)
	{
		if (nm.length() < len)
		{
			return nm+pad.substring(0,len-nm.length());
		}
		if (nm.length() > len)
		{
			return nm.substring(0,len);
		}
		return nm;
	}

	public void write(String file)
		throws IOException
	{
		int longest = 0;

		for (Enumeration e = remotes.elements(); e.hasMoreElements(); )
		{
			IRRemote r = (IRRemote)e.nextElement();
			for (Enumeration f = r.getKeys(); f.hasMoreElements(); )
			{
				IRSignal s = (IRSignal)f.nextElement();
				String nm = s.getName();
				if (nm != null && nm.length() > longest)
				{
					longest = nm.length();
				}
			}
		}

		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		for (Enumeration e = remotes.elements(); e.hasMoreElements(); )
		{
			IRRemote r = (IRRemote)e.nextElement();
			bw.write("["+r.getModel()+","+r.getCompany()+","+r.getDescription()+"]");
			bw.newLine();
			for (Enumeration f = r.getKeys(); f.hasMoreElements(); )
			{
				IRSignal s = (IRSignal)f.nextElement();
				bw.write("  "+pad(s.getName(),longest)+" = "+s.toString());
				bw.newLine();
			}
		}
		bw.flush();
		bw.close();

	}

	public void add(IRRemote remote)
	{
		remotes.add(remote);
	}

	public boolean remove(IRRemote remote)
	{
		return remotes.remove(remote);
	}

	public void clear()
	{
		remotes.clear();
	}

	public int size()
	{
		return remotes.size();
	}

	public IRRemote getByIndex(int idx)
	{
		return (IRRemote)remotes.get(idx);
	}

	public Enumeration getRemotes()
	{
		return remotes.elements();
	}
	
	// ---( interface methods )---

}


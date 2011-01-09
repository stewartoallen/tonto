/*
 * Copyright 2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.xml;

// ---( imports )---
import java.io.*;

// TODO: handle &amp; and the like
public class XMLParser
{
	// ---( static fields )---

	// ---( static methods )---
	public static void main(String args[])
		throws Exception
	{
		XMLParser p = new XMLParser();
		p.load(args[0]);
		p.print();
	}

	public static XMLNode parseStream(InputStream is)
		throws IOException
	{
		XMLParser p = new XMLParser();
		p.load(is);
		return p.getRootNode();
	}

	// ---( constructors )---
	public XMLParser ()
	{
	}

	// ---( instance fields )---
	private XMLNode root;

	// ---( instance methods )---
	public void print()
	{
		print(0, root);
	}

	private void print(int depth, XMLNode node)
	{
		for (int i=0; i<depth; i++)
		{
			System.out.print("  ");
		}
		System.out.println(node);
		XMLNode n[] = node.getAllNodes();
		if (n != null && n.length > 0)
		{
			for (int i=0; i<n.length; i++)
			{
				print(depth+1, n[i]);
			}
			for (int i=0; i<depth; i++)
			{
				System.out.print("  ");
			}
			System.out.println("</"+node.getName()+">");
		}
	}

	public XMLNode getRootNode()
	{
		return root;
	}

	public void load(InputStream is)
		throws IOException
	{
		skipUntil(is, '<');
		root = readTag(is);
	}

	public void load(String file)
		throws IOException
	{
		FileInputStream fi = new FileInputStream(file);
		load(fi);
		fi.close();
	}

	private XMLNode readTag(InputStream is)
		throws IOException
	{
		StringBuffer sb = new StringBuffer();
		StringBuffer da = new StringBuffer();
		XMLNode node = new XMLNode();
		int nc;

//System.out.println("01 read tag");
		nc = is.read();
		// close tag
		if (nc == '/')
		{
//System.out.println("02 end tag");
			while (nc != '>')
			{
				sb.append((char)nc);
				nc = is.read();
			}
//System.out.println("03 end tag name '"+sb+"'");
			node.setName(sb.toString());
			return node;
		}
		if (nc == '?')
		{
//System.out.println("a3 skip <? tag");
			skipUntil(is, "?>");
			skipUntil(is, '<');
			return readTag(is);
		}
		if (nc == '!')
		{
//System.out.println("b3 skip <!-- tag");
			skipUntil(is, "->");
			skipUntil(is, '<');
			return readTag(is);
		}

		// read tag name
		while ( nc != ' ' && nc != '/' && nc != '>' )
		{
			sb.append((char)nc);
			nc = is.read();
		}
		String name = sb.toString();
		node.setName(name);
		// read attrs
//System.out.println("04 '"+name+"' reading attrs");
		while (nc == ' ')
		{
			sb.setLength(0);
//System.out.println("05 next attr");
			while ( (nc = is.read()) == ' ' )
				;
//System.out.println("06 skipped space");
			while (nc != ' ' && nc != '=' && nc != '/' && nc != '>')
			{
				sb.append((char)nc);
				nc = is.read();
			}
//System.out.println("07 got attr name '"+sb+"'");
			if (nc == '=')
			{
				if (is.read() != '\"')
				{
					throw stateError("expected '\"' after '='");
				}
				String nm = sb.toString();
				sb.setLength(0);
//System.out.println("08 reading attr value");
				while ( (nc = is.read()) != '\"')
				{
					sb.append((char)nc);
				}
				if (nc != '\"')
				{
					throw stateError("expected '\"' to terminate value");
				}
				else
				{
					nc = ' ';
				}
//System.out.println("09 got attr value '"+sb+"'");
				node.addAttribute(nm, sb.toString());
			}
			else
			if (nc == ' ' || nc == '>')
			{
				if (sb.length() > 0)
				{
//System.out.println("10 attr only, no value '"+sb+"'");
					node.addAttribute(sb.toString(), null);
				}
				continue;
			}
			else
			{
				break;
			}
		}
//System.out.println("11 next="+nc+" char='"+((char)nc)+"'");
		if (nc == '/')
		{
//System.out.println("12 detect empty tag");
			// not allowed
			if ( (nc = is.read()) != '>')
			{
				throw stateError("expected '>' but got '"+((char)nc)+"'");
			}
//System.out.println("13 return empty tag '"+node+"'");
			return node;
		}
		else
		if (nc == '>')
		{
//System.out.println("14 end open tag");
			while (true)
			{
				loadUntil(da, is, '<');
//System.out.println("15 "+node+" reading next child");
				XMLNode next = readTag(is);
				if (next.getName().equals("/"+name))
				{
					if (da.length() > 0)
					{
						node.setValue(da.toString());
					}
					return node;
				}
				else
				if (next != null)
				{
					node.addChild(next);
				}
//System.out.println("16 got child '"+next+"'");
			}
		}
		if (da.length() > 0)
		{
			node.setValue(da.toString());
		}
//System.out.println("17 complete tag '"+node+"'");
		return node;
	}

	private void skipUntil(InputStream is, int ch)
		throws IOException
	{
		int nc;
		while ( (nc = is.read()) != ch && nc >= 0)
			;
		if (nc < 0)
		{
			throw new EOFException();
		}
	}

	private void loadUntil(StringBuffer sb, InputStream is, int ch)
		throws IOException
	{
		int nc;
		while ( (nc = is.read()) != ch && nc >= 0)
		{
			sb.append((char)nc);
		}
		if (nc < 0)
		{
			throw new EOFException();
		}
	}

	private void skipUntil(InputStream is, String str)
		throws IOException
	{
		byte b[] = str.getBytes();
		int r[] = new int[b.length];
		for (int i=0; i<r.length; i++)
		{
			r[i] = (byte)is.read();
			if (r[i] < 0)
			{
				throw new EOFException();
			}
		}
		int pos = 0;
		while (true)
		{
			int seek = pos;
			for (int i=0; i<b.length; i++)
			{
				if (b[i] != (byte)r[seek])
				{
					break;
				}
				seek++;
				if (seek >= r.length)
				{
					seek = 0;
				}
				if (seek == pos)
				{
					// did a loop. found it!
					return;
				}
			}
			r[pos] = is.read();
			if (r[pos] < 0)
			{
				throw new EOFException();
			}
			pos++;
			if (pos >= r.length)
			{
				pos = 0;
			}
		}
	}

	private IOException stateError(String msg)
	{
		return new IOException(msg);
	}

	// ---( interface methods )---

}


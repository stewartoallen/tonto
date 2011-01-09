/*
 * Copyright 2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.xml;

// ---( imports )---
import java.util.*;

public class XMLNode
{
	// ---( static fields )---

	// ---( static methods )---

	// ---( constructors )---
	XMLNode()
	{
	}

	XMLNode (String tag, String value)
	{
		this.tag = tag;
		this.val = value;
	}

	// ---( instance fields )---
	public void addChild(XMLNode node)
	{
		if (nodes == null)
		{
			nodes = new Vector(3);
		}
		nodes.add(node);
	}

	public void setName(String name)
	{
		this.tag = name;
	}

	public void setValue(String value)
	{
		this.val = value;
	}

	public String getName()
	{
		return tag;
	}

	public String getValue()
	{
		return val;
	}

	public XMLNode[] getAllNodes()
	{
		if (nodes == null)
		{
			return null;
		}
		XMLNode n[] = new XMLNode[nodes.size()];
		nodes.copyInto(n);
		return n;
	}

	public XMLNode[] getNodeArray(String tag)
	{
		Enumeration e = getNodeEnumeration(tag);
		if (e == null)
		{
			return null;
		}
		Vector v = new Vector();
		while (e.hasMoreElements())
		{
			v.add(e.nextElement());
		}
		XMLNode n[] = new XMLNode[v.size()];
		v.copyInto(n);
		return n;
	}

	public Enumeration getNodeEnumeration(String tag)
	{
		if (nodes == null)
		{
			return null;
		}
		final String xtag = tag;
		return new Enumeration()
		{
			private int next = 0;
			private boolean init = false;
			private XMLNode match = null;

			public boolean hasMoreElements()
			{
				init();
				return match != null;
			}

			public Object nextElement()
			{
				init();
				if (match != null)
				{
					XMLNode n = match;
					findNext();
					return n;
				}
				else
				{
					throw new IllegalArgumentException("no more elements");
				}
			}

			private void findNext()
			{
				while (next < nodes.size())
				{
					XMLNode t = (XMLNode)nodes.get(next++);
					if (t.getName().equals(xtag))
					{
						match = t;
						return;
					}
				}
				match = null;
			}

			private void init()
			{
				if (!init)
				{
					findNext();
					init = true;
				}
			}
		};
	}

	public XMLNode getNode(String tag)
	{
		if (nodes == null)
		{
			return null;
		}
		for (int i=0; i<nodes.size(); i++)
		{
			XMLNode n = (XMLNode)nodes.get(i);
			if (n.getName().equals(tag))
			{
				return n;
			}
		}
		return null;
	}

	public int getNodeCount()
	{
		return nodes != null ? nodes.size() : 0;
	}

	public int getAttributeCount()
	{
		return attrs != null ? attrs.size() : 0;
	}

	public void addAttribute(String name, String value)
	{
		if (attrs == null)
		{
			attrs = new Vector(3);
		}
		attrs.add(new XMLNode(name, value));
	}

	public XMLNode[] getAttributes()
	{
		if (attrs == null)
		{
			return null;
		}
		XMLNode n[] = new XMLNode[attrs.size()];
		attrs.copyInto(n);
		return n;
	}

	public String getAttribute(String name)
	{
		if (attrs == null)
		{
			return null;
		}
		for (int i=0; i<attrs.size(); i++)
		{
			XMLNode n = (XMLNode)attrs.get(i);
			if (n.getName().equals(name))
			{
				return n.getValue();
			}
		}
		return null;
	}

	public boolean hasAttribute(String name)
	{
		if (attrs == null)
		{
			return false;
		}
		for (int i=0; i<attrs.size(); i++)
		{
			XMLNode n = (XMLNode)attrs.get(i);
			if (n.getName().equals(name))
			{
				return true;
			}
		}
		return false;
	}

	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		if (attrs != null)
		{
			for (int i=0; i<attrs.size(); i++)
			{
				XMLNode at = (XMLNode)attrs.get(i);
				sb.append(" ");
				sb.append(at.getName());
				String v = at.getValue();
				if (v != null)
				{
					sb.append("=\""+v+"\"");
				}
			}
		}
		if (nodes == null || nodes.size() == 0)
		{
			if (val != null)
			{
				return "<"+tag+sb+">"+val+"</"+tag+">";
			}
			return "<"+tag+sb+" />";
		}
		else
		{
			return "<"+tag+sb+">";
		}
	}

	// ---( instance methods )---
	private String tag;
	private String val;
	private Vector attrs;
	private Vector nodes;

	// ---( interface methods )---
}


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
import java.awt.Point;
import java.util.Vector;

/**
 * A single Pronto screen containing buttons and frames.
 */
public class CCFPanel
	extends CCFNode
	implements IChildContainer, INamed, IListElement
{
	private final static String[][] encode = new String[][]
	{
		{ "Z*", "next" },
		{ "N4", "namePos" },
		{ "N1", "count1" },
		{ "N1", "count2" },
		{ "Z+", "child", "count1" },
        //-------------//
		{ "X*", "name" },
	};

	private final static String[][] decode = new String[][]
	{
		{ "Z*", "next" },
		{ "N4", "namePos" },
		{ "N1", "count1" },
		{ "N1", "count2" },
		{ "Z+", "child", "count1" },
	};

	final static int HIDDEN = 0x80000000;
	final static int NAME_MASK = 0x7fffffff;

	// ---( constructors )---
	CCFPanel()
	{
	}

	CCFPanel(String name, CCFHeader header)
	{
		setName(name);
		setParent(header);
	}

	CCFPanel(String name, CCFDevice parent)
	{
		setName(name);
		setParent(parent);
	}

	CCFNode getClone()
	{
		return getClone(false);
	}

	CCFNode getClone(boolean list)
	{
		CCFPanel p = (CCFPanel)super.getClone();
		if (list)
		{
			if (p.next != null)
			{
				p.next = (CCFPanel)p.next.getClone(list);
			}
		}
		else
		{
			p.setNextPanel(null);
		}
		p.child = getClone(p.child);
		p.buildTree(getParent());
		return p;
	}

	void delete(CCFNode find)
	{
		setChildren(delete(child, find));
	}

	CCFChild getPrevious(CCFChild node)
	{
		List l = getAllChildren();
		for (int i=0; i<l.size(); i++)
		{
			if (l.get(i) == node)
			{
				if (i > 0)
				{
					return (CCFChild)l.get(i-1);
				}
				else
				{
					return (CCFChild)l.get(l.size()-1);
				}
			}
		}
		return null;
	}

	CCFChild getNext(CCFChild node)
	{
		List l = getAllChildren();
		for (int i=0; i<l.size(); i++)
		{
			if (l.get(i) == node)
			{
				if (i < l.size()-1)
				{
					return (CCFChild)l.get(i+1);
				}
				else
				{
					return (CCFChild)l.get(0);
				}
			}
		}
		return null;
	}

	private List getAllChildren()
	{
		return getAllChildren(this, new Vector());
	}

	private List getAllChildren(IChildContainer c, List l)
	{
		CCFChild list[] = c.getChildren();
		for (int i=0; i<list.length; i++)
		{
			l.add(list[i]);
			if (list[i] instanceof IChildContainer)
			{
				getAllChildren((IChildContainer)child[i], l);
			}
		}
		return l;
	}

	static CCFChild[] delete(CCFChild ch[], CCFNode find)
	{
		if (find == null || ch == null)
		{
			return ch;
		}
		for (int i=0; i<ch.length; i++)
		{
			if (ch[i].child == find)
			{
				CCFChild nc[] = new CCFChild[ch.length-1];
				System.arraycopy(ch,0,nc,0,i);
				if (i < ch.length)
				{
					System.arraycopy(ch,i+1,nc,i,nc.length-i);
				}
				return nc;
			}
			else
			if (ch[i].isFrame())
			{
				ch[i].getFrame().delete(find);
			}
		}
		return ch;
	}

	static CCFChild[] add(CCFChild c[], CCFChild add)
	{
		if (c == null)
		{
			return new CCFChild[] { add };
		}
		CCFChild c2[] = new CCFChild[c.length+1];
		System.arraycopy(c,0,c2,0,c.length);
		c2[c.length] = add;
		return c2;
	}

	// ---( instance fields )---
	CCFPanel  next;
	int       namePos;
	int       count1;
	int       count2;		// same as count1
	CCFChild  child[];		// NEVER set directly!

	boolean   hidden;
	String    name;

	// ---( public API )---
	/**
	 * Return this panel's name.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Set this panel's name.
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	public IListElement getNextElement()
	{
		return next;
	}

	/**
	 * Get the next Panel in the linked list.
	 */
	public CCFPanel getNextPanel()
	{
		return next;
	}

	/**
	 * Set the next Panel in the linked list.
	 */
	public void setNextPanel(CCFPanel next)
	{
		if (next != null)
		{
			next.setParent(getParent());
		}
		this.next = next;
	}

	public boolean isTemplate()
	{
		return getParent() == getHeader();
	}

	public void insertBefore(CCFPanel panel)
	{
		getParentDevice().insertBefore(this, panel);
	}

	public void insertAfter(CCFPanel panel)
	{
		if (next == null)
		{
			next = panel;
		}
		else
		{
			panel.next = next;
			next = panel;
		}
		panel.buildTree(getParent());
	}

	/**
	 * Create a button. It must be added to the panel manually.
	 */
	public CCFButton createButton(String name)
	{
		return new CCFButton(this, name);
	}

	/**
	 * Create a frame. It must be added to the panel manually.
	 */
	public CCFFrame createFrame(String name)
	{
		return new CCFFrame(this, name);
	}

	/**
	 * Returns true if this panel is hidden.
	 */
	public boolean isHidden()
	{
		return hidden;
	}

	/**
	 * Sets whether or not this panel is hidden.
	 *
	 * @param hide new hidden/visible state of panel
	 */
	public void setHidden(boolean hide)
	{
		hidden = hide;
	}

	/**
	 * Return array of children of this panel.
	 */
	public CCFChild[] getChildren()
	{
		return child;
	}

	public boolean hasChildren()
	{
		return child != null && child.length > 0;
	}

	public CCFPanel getTemplate()
	{
		CCFHeader h = getHeader();
		CCFDevice d = getParentDevice();
		if (d == null || h == null || isTemplate())
		{
			return null;
		}
		CCFPanel t = d.isNormalDevice() ? h.deviceTemplate() : d.isMacroDevice() ? h.macroTemplate() : h.masterTemplate();
		if (t == null || !t.hasChildren())
		{
			t = h.masterTemplate();
		}
		return t;
	}

	/**
	 * Replace children with this array.
	 */
	public void setChildren(CCFChild c[])
	{
		child = c;
		count1 = c != null ? c.length : 0;
		count2 = count1;
		buildTree(getParent());
	}

	/**
	 * Add child to this panel.
	 */
	public void addChild(CCFChild c)
	{
		setChildren(add(child, c));
	}

	/**
	 * Add child to this panel.
	 */
	public void addButton(CCFButton b)
	{
		setChildren(add(child, b.getChildWrapper()));
	}

	/**
	 * Add child to this panel.
	 */
	public void addFrame(CCFFrame f)
	{
		setChildren(add(child, f.getChildWrapper()));
	}

	/**
	 * Delete this panel from it's parent Device.
	 */
	public void delete()
	{
		getParentDevice().delete(this);
		setParent(null);
	}

	public String toString()
	{
		return 
			name != null ? (hidden ? "("+name+")" : name) : "";
	}

	public boolean equals(Object o)
	{
		return o == this;
	}

	String getFQN()
	{
		return getParentDevice()+" : "+(name != null ? name : "");
	}

	CCFButton[] getButtons()
	{
		Vector v = new Vector();
		if (child != null)
		{
			for (int i=0; i<child.length; i++)
			{
				child[i].collectButtons(v);
			}
		}
		return (CCFButton[])v.toArray(new CCFButton[v.size()]);
	}

	/**
	 * Recursively search children for the first button with the specified name.
	 *
	 * @param name name of button to find
	 */
	public CCFButton getButtonByName(String name)
	{
		CCFButton b[] = getButtons();
		for (int i=0; i<b.length; i++)
		{
			if (b[i].name != null && b[i].name.equals(name))
			{
				return b[i];
			}
		}
		return null;
	}

	/**
	 * Recursively search children for the first button with the specified id.
	 *
	 * @param id id of button to find
	 */
	public CCFButton getButtonByID(String id)
	{
		CCFButton b[] = getButtons();
		for (int i=0; i<b.length; i++)
		{
			if (b[i].idtag != null && b[i].idtag.equals(id))
			{
				return b[i];
			}
		}
		return null;
	}

	// ---( override methods )---
	String describe()
	{
		return "Panel,"+(
			name != null ? (hidden ? "("+name+")" : name) : ""
		)+","+(child != null ? child.length : 0);
	}

	// ---( abstract methods )---
	void checkVersion()
	{
	}

	void preEncode(CCFNodeState zs)
	{
		namePos = getStringEncodePos(zs,name);
		if (hidden)
		{
			namePos = namePos | HIDDEN;
		}
	}

	void preDecode(CCFNodeState zs)
	{
	}

	void postDecode(CCFNodeState zs)
	{
		if (count1 != count2)
		{
			log(0,"panel: child count mismatch "+count1+" != "+count2, zs);
			int min = Math.min(Math.min(count1,count2),10);
			count1 = min;
			count2 = min;
		}
		hidden = ((namePos & HIDDEN) == HIDDEN);
		name = stringJumpDecode((namePos & NAME_MASK), zs);
		zs.putStringLocation(namePos, name);
		if (name != null && name.equals("EggDVD"))
		{
			zs.getHeader().setEggDVD(this);
		}
	}

	String[][] getEncodeTable()
	{
		return encode;
	}

	String[][] getDecodeTable()
	{
		return decode;
	}	

	void buildTree(CCFNode parent)
	{
		setParent(parent);
		if (child == null)
		{
			return;
		}
		for (int i=0; i<child.length; i++)
		{
			child[i].buildTree(this);
		}
	}
}


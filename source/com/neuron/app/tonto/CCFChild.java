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
import java.util.Vector;

/**
 * The container class for CCFButton and CCFFrame objects.
 * Buttons and Frames are the two component types that Panels
 * can contain. Further, Frames can contain other Child and Frame
 * objects. While CCFChild container only serves as a positional
 * holder for its contents, this somewhat awkward object structure
 * greatly simplifies CCF encoding and decoding. The public CCF API
 * may change in the future to eliminate this indirection.
 */
public class CCFChild extends CCFNode
{
	private final static String[][] encode =
	{
		{ "N2", "intX" },
		{ "N2", "intY" },
		{ "Z*", "child" },
		{ "N1", "type" },
	};

	private final static String[][] decode =
	{
		{ "N2", "intX" },
		{ "N2", "intY" },
		{ "N4", "childPos" },
		{ "N1", "type" },
	};

	// ---( static fields )---
	public final static int FRAME = 0;
	public final static int BUTTON = 1;

	// ---( instance fields )---
	int    intX;
	int    intY;
	int    childPos;
	int    type;
	CCFNode child;

	// ---( public API )---
	CCFChild()
	{
	}

	/**
	 * Create a Child that contains a Frame.
	 */
	CCFChild(CCFNode parent, CCFFrame frame)
	{
		setParent(parent);
		setFrame(frame);
		intX = 10;
		intY = 10;
	}

	/**
	 * Create a Child that contains a Button.
	 */
	CCFChild(CCFNode parent, CCFButton button)
	{
		setParent(parent);
		setButton(button);
		intX = 10;
		intY = 10;
	}

	void setFrame(CCFFrame frame)
	{
		type = FRAME;
		child = frame;
		child.setParent(this);
	}

	void setButton(CCFButton button)
	{
		type = BUTTON;
		child = button;
		child.setParent(this);
	}

	public CCFNode getClone()
	{
		CCFChild c = (CCFChild)super.getClone();
		c.child = c.child.getClone();
		c.buildTree(getParent());
		return c;
	}

	void dump()
	{
		super.dump();
		if (child != null)
		{
			child.dump();
		}
	}

	// ---( public API )---
	public boolean raise()
	{
		CCFChild c[] = ((IChildContainer)getParent()).getChildren();
		for (int i=0; i<c.length-1; i++)
		{
			if (c[i] == this)
			{
				CCFChild tmp = c[i+1];
				c[i+1] = this;
				c[i] = tmp;
				((IChildContainer)getParent()).setChildren(c);
				return true;
			}
		}
		return false;
	}

	public boolean lower()
	{
		CCFChild c[] = ((IChildContainer)getParent()).getChildren();
		for (int i=1; i<c.length; i++)
		{
			if (c[i] == this)
			{
				CCFChild tmp = c[i-1];
				c[i-1] = this;
				c[i] = tmp;
				((IChildContainer)getParent()).setChildren(c);
				return true;
			}
		}
		return false;
	}

	public void top()
	{
		while (raise())
			;
	}

	public void bottom()
	{
		while (lower())
			;
	}

	/**
	 * Return the bounds of the child.
	 */
	public Rectangle getBounds()
	{
		Point p = getLocation();
		Dimension d = getSize();
		return new Rectangle(p.x, p.y, d.width, d.height);
	}

	/**
	 * Set the bounds of the child.
	 */
	public void setBounds(Rectangle bounds)
	{
		setLocation(new Point(bounds.x, bounds.y));
		setSize(new Dimension(bounds.width, bounds.height));
	}

	/**
	 * Return the X,Y location of this object in the
	 * Panel or frame.
	 */
	public Point getLocation()
	{
		return new Point(intX, intY);
	}

	/**
	 * Set the X,Y location.
	 *
	 * @see #getLocation
	 * @param loc x,y location in the panel or frame
	 */
	public void setLocation(Point loc)
	{
		intX = loc.x;
		intY = loc.y;
	}

	/**
	 * Returns true of the child does not have an icon.
	 */
	public boolean isResizable()
	{
		if (child == null) { return true; }
		return (type == BUTTON  ?
			((CCFButton)child).isResizable() : ((CCFFrame)child).isResizable());
	}

	/**
	 * Get the size of the child.
	 */
	public Dimension getSize()
	{
		if (child == null) { return new Dimension(0,0); }
		return (type == BUTTON  ?
			((CCFButton)child).getSize() : ((CCFFrame)child).getSize());
	}

	/**
	 * Set the size of the child.
	 */
	public void setSize(Dimension size)
	{
		if (child == null) { return; }
		if (type == BUTTON)
		{
			((CCFButton)child).setSize(size);
		}
		else
		{
			((CCFFrame)child).setSize(size);
		}
	}

	/**
	 * Returns true if this child is a Button.
	 */
	public boolean isButton()
	{
		return type == BUTTON;
	}

	/**
	 * Returns true if this child is a Frame.
	 */
	public boolean isFrame()
	{
		return type == FRAME;
	}

	/**
	 * Returns the child type.
	 */
	public int getType()
	{
		return type;
	}

	/**
	 * Returns the wrapped button if it's a button and null otherwise.
	 */
	public CCFButton getButton()
	{
		return isButton() ? (CCFButton)child : null;
	}

	/**
	 * Returns the wrapped frame if it's a frame and null otherwise.
	 */
	public CCFFrame getFrame()
	{
		return isFrame() ? (CCFFrame)child : null;
	}

	/**
	 * Delete this child from it's parent.
	 */
	public void delete()
	{
		getParentPanel().delete(child);
	}

	// ---( override methods )---
	String describe()
	{
		return "Child,"+intX+"x"+intY+","+type;
	}

	void collectButtons(Vector v)
	{
		if (child == null)
		{
			return;
		}
		if (type == FRAME)
		{
			((CCFFrame)child).collectButtons(v);
		}
		else
		if (type == BUTTON)
		{
			v.addElement(child);
		}
	}

	// ---( abstract methods )---
	void checkVersion()
	{
	}

	void preEncode(CCFNodeState zs)
	{
	}

	void preDecode(CCFNodeState zs)
	{
	}

	void postDecode(CCFNodeState zs)
	{
		if (childPos > 0)
		{
			switch(type)
			{
				case FRAME:
					child = getItemByPos(zs, childPos, CCFFrame.class);
					break;
				case BUTTON:
					child = getItemByPos(zs, childPos, CCFButton.class);
					break;
				default:
					debug.log(0, "Invalid child type: "+type);
					break;
			}
		}
		if (child == null)
		{
			debug.log(0, "Null child "+describe());
			switch(type)
			{
				case FRAME:
					child = new CCFFrame(this, null);
					break;
				case BUTTON:
					child = new CCFButton(this, null);
					break;
			}
			
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
		if (child != null)
		{
			child.buildTree(this);
		}
	}
}


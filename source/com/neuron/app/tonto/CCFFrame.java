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
 * A container class similar to CCFPanel since CCFPanels
 * cannot nest. They are usually used to organize buttons
 * but they can also be assigned an Icon.
 */
public class CCFFrame extends CCFNode implements IChildContainer, INamed
{
	private final static String[][] nocolor =
	{
		{ "N2", "width" },
		{ "N2", "height" },
		{ "S*", "name" },
		{ "Z*", "icon" },
		{ "N4", "_reserve_1" },
		{ "N1", "fontSize" },
		{ "N1", "colors" },
		{ "N1", "count1" },
		{ "N1", "count2" },
		{ "Z+", "child", "count1" },
	};

	private final static String[][] color =
	{
		{ "N2", "width" },
		{ "N2", "height" },
		{ "S*", "name" },
		{ "Z*", "icon" },
		{ "N3", "_reserve_1" },
		{ "N1", "textOpt" },
		{ "N1", "fontSize" },
		{ "N4", "colors" },
		{ "N1", "count1" },
		{ "N1", "count2" },
		{ "Z+", "child", "count1" },
	};

	private String[][] codec = nocolor;
	private boolean inColor;

	// ---( instance fields )---
	int       width;
	int       height;
	String    name;
	CCFIcon   icon;
	int       _reserve_1;
	int       textOpt;		// netremote only
	int       fontSize;		// sz=00001111
	int       colors;		// bg=11111100 txt=00000011
	int       count1;
	int       count2;		// same as count1
	CCFChild  child[];		// NEVER set directly!

	CCFFrame()
	{
	}

	CCFFrame(CCFNode parent, String name)
	{
		setName(name);
		setParent(new CCFChild(parent, this));
		width = 90;
		height = 60;
		inColor = usingColor();
		setForeground(CCFColor.getNamedColor(CCFColor.BLACK,inColor));
		setBackground(CCFColor.getNamedColor(CCFColor.LIGHT_GRAY,inColor));
		checkVersion();
	}

	CCFNode getClone()
	{
		CCFFrame f = (CCFFrame)super.getClone();
		f.child = getClone(f.child);
		f.buildTree(getParent());
		return f;
	}

	public void setParent(CCFNode p)
	{
		super.setParent(p);
		if (!(p == null || p instanceof CCFChild))
		{
			throw new RuntimeException("parent ! CCFChild : "+p);
		}
	}

	// ---( public API )---
	public int getTextAlignment()
	{
		return textOpt & 0x3;
	}

	public void setTextAlignment(int align)
	{
		textOpt = (textOpt & 0xfc) | (align & 0x3);
	}

	public boolean getTextWrap()
	{
		return (textOpt & 0x80) == 0x80;
	}

	public void setTextWrap(boolean w)
	{
		textOpt = (textOpt & 0x7f) | (w ? 0x80 : 0x00);
	}

	public void convertToColor()
	{
		if (inColor)
		{
			return;
		}
		debug.log(3, "converting "+describe()+" to color");
		CCFColor map[] = getHeader().getColorMap();
		int fgIdx = CCFColor.getForegroundIndex(colors, false);
		int bgIdx = CCFColor.getBackgroundIndex(colors, false);
		colors = CCFColor.getComposite(map[fgIdx], map[bgIdx], true);
		inColor = getHeader().hasColor();
	}

	public void convertToGray()
	{
		if (!inColor)
		{
			return;
		}
		debug.log(3, "converting "+describe()+" to gray");
		int fgIdx = CCFColor.rgbIndexToGrayIndex(CCFColor.getForegroundIndex(colors, true));
		int bgIdx = CCFColor.rgbIndexToGrayIndex(CCFColor.getBackgroundIndex(colors, true));
		colors = CCFColor.getComposite(new CCFColor(fgIdx), new CCFColor(bgIdx), false);
		inColor = getHeader().hasColor();
	}

	/**
	 * Get the name of this frame.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Set the name of this frame.
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Create a button. It must be added to the frame manually.
	 */
	public CCFButton createButton(String name)
	{
		return new CCFButton(getParentPanel(), name);
	}

	/**
	 * Create a frame. It must be added to the frame manually.
	 */
	public CCFFrame createFrame(String name)
	{
		return new CCFFrame(getParentPanel(), name);
	}

	/**
	 * Get the CCFFont for this Button.
	 */
	public CCFFont getFont()
	{
		return CCFFont.getFont(fontSize);
	}

	/**
	 * Set the CCFFont for this Button.
	 *
	 * @param font new button font
	 */
	public void setFont(CCFFont font)
	{
		fontSize = font.getFontSize();
	}

	/**
	 * Get the frame background color.
	 */
	public CCFColor getBackground()
	{
		return CCFColor.getBackground(colors, inColor);
	}

	/**
	 * Set the frame background color.
	 *
	 * @param color background color
	 */
	public void setBackground(CCFColor color)
	{
		colors = CCFColor.getComposite(getForeground(), color, inColor);
	}
	
	/**
	 * Get the frame foreground color.
	 */
	public CCFColor getForeground()
	{
		return CCFColor.getForeground(colors, inColor);
	}

	/**
	 * Set the frame foreground color.
	 *
	 * @param color foreground color
	 */
	public void setForeground(CCFColor color)
	{
		colors = CCFColor.getComposite(color, getBackground(), inColor);
	}
	
	/**
	 * Get the background icon associated with this frame.
	 */
	public CCFIcon getIcon()
	{
		return icon;
	}

	/**
	 * Set the background icon associated with this frame.
	 *
	 * @param icon new background icon
	 */
	public void setIcon(CCFIcon icon)
	{
		this.icon = icon;
	}

	/**
	 * Gets the contents of this frame.
	 */
	public CCFChild[] getChildren()
	{
		return child;
	}

	/**
	 * Sets the contents of this frame.
	 *
	 * @param c new contents of this frame
	 */
	public void setChildren(CCFChild c[])
	{
		child = c;
		count1 = c != null ? c.length : 0;
		count2 = count1;
		buildTree(getParent());
	}

	/**
	 * Add child to this frame.
	 */
	public void addChild(CCFChild c)
	{
		setChildren(CCFPanel.add(child, c));
	}

	/**
	 * Add child to this frame.
	 */
	public void addButton(CCFButton b)
	{
		setChildren(CCFPanel.add(child, b.getChildWrapper()));
	}

	/**
	 * Add child to this frame.
	 */
	public void addFrame(CCFFrame f)
	{
		setChildren(CCFPanel.add(child, f.getChildWrapper()));
	}

	/**
	 * Returns the size of this frame in pixels.
	 */
	public Dimension getSize()
	{
		return new Dimension(width(), height());
	}

	/**
	 * Sets the new size for this frame. This has no effect
	 * if the frame uses an Icon since the Icons's size will
	 * override this setting.
	 */
	public void setSize(Dimension size)
	{
		width = size.width;
		height = size.height;
	}

	public CCFChild getChildWrapper()
	{
		return (CCFChild)getParent();
	}

	public Point getLocation()
	{
		return getChildWrapper().getLocation();
	}

	public void setLocation(Point p)
	{
		getChildWrapper().setLocation(p);
	}

	/**
	 * Returns true if this frame is resizable. The frame will
	 * not be resizable if it has an icon set.
	 */
	public boolean isResizable()
	{
		return icon == null;
	}

	/**
	 * Deletes this child from it's parent.
	 */
	public void delete()
	{
		getParentPanel().delete(this);
		setParent(null);
	}

	// ---( utility methods )---
	void delete(CCFNode find)
	{
		setChildren(CCFPanel.delete(child, find));
	}

	int width()
	{
		return icon != null ? icon.width : width;
	}

	int height()
	{
		return icon != null ? icon.height : height;
	}

	// ---( override methods )---
	String describe()
	{
		return "Frame,"+name+","+width+"x"+height+","+fontSize+","+colors;
	}

	void collectButtons(Vector v)
	{
		if (child == null)
		{
			return;
		}
		for (int i=0; i<child.length; i++)
		{
			child[i].collectButtons(v);
		}
	}

	// ---( abstract methods )---
	void checkVersion()
	{
		if (getHeader().hasColor())
		{
			codec = color;
		}
		else
		{
			codec = nocolor;
		}
		inColor = (codec == color);
	}

	void preEncode(CCFNodeState zs)
	{
		width = width();
		height = height();
	}

	void preDecode(CCFNodeState zs)
	{
	}

	void postDecode(CCFNodeState zs)
	{
		if (count1 != count2)
		{
			log(0,"frame: child count mismatch "+count1+" != "+count2, zs);
			int min = Math.min(Math.min(count1, count2), 10);
			count1 = min;
			count2 = min;
		}
	}

	String[][] getEncodeTable()
	{
		return codec;
	}

	String[][] getDecodeTable()
	{
		return codec;
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

	// ---( instance methods )---
	public String toString()
	{
		return "'"+name+"'-"+super.toString();
	}

}


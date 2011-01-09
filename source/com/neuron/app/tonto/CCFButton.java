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

/**
 * An on-screen button that can be assigned various icons
 * to represent active/inactive and selected/unselected states.
 */
public class CCFButton extends CCFNode implements INamed
{
	private final static String[][] nocolor =
	{
		{ "N2", "width" },
		{ "N2", "height" },
		{ "Z*", "actions" },
		{ "S*", "name" },
		{ "S*", "idtag" },
		{ "N1", "fontSize" },
		{ "N1", "_reserve_1" },
		{ "Z*", "iconIU" },
		{ "Z*", "iconIS" },
		{ "Z*", "iconAU" },
		{ "Z*", "iconAS"  },
		{ "N1", "colorIU" },
		{ "N1", "colorIS" },
		{ "N1", "colorAU" },
		{ "N1", "colorAS" },
	};

	private final static String[][] color =
	{
		{ "N2", "width" },
		{ "N2", "height" },
		{ "Z*", "actions" },
		{ "S*", "name" },
		{ "S*", "idtag" },
		{ "N1", "fontSize" },
		{ "N1", "textOpt" },
		{ "Z*", "iconIU" },
		{ "Z*", "iconIS" },
		{ "Z*", "iconAU" },
		{ "Z*", "iconAS"  },
		{ "N4", "colorIU" },
		{ "N4", "colorIS" },
		{ "N4", "colorAU" },
		{ "N4", "colorAS" },
	};

	private String[][] codec = nocolor;
	private boolean inColor;

	public final static int STATE_ACTIVE_UNSELECTED   = 1;
	public final static int STATE_ACTIVE_SELECTED     = 2;
	public final static int STATE_INACTIVE_UNSELECTED = 3;
	public final static int STATE_INACTIVE_SELECTED   = 4;

	private final static int defBWColors =
		CCFColor.getComposite(
			CCFColor.getNamedColor(CCFColor.WHITE, false),
			CCFColor.getNamedColor(CCFColor.DARK_GRAY, false), false);

	private final static int defCOColors =
		CCFColor.getComposite(
			CCFColor.getNamedColor(CCFColor.WHITE, true),
			CCFColor.getNamedColor(CCFColor.DARK_GRAY, true), true);

	// ---( instance fields )---
	int           width;
	int           height;
	CCFActionList actions;
	String        name;
	String        idtag;
	int           textOpt;		// netremote only
	int           fontSize;		// sz=00001111
	int           _reserve_1;
	CCFIcon       iconIU;
	CCFIcon       iconIS;
	CCFIcon       iconAU;
	CCFIcon       iconAS;
	int           colorIU;		// bg=11111100 txt=00000011
	int           colorIS;		// bg=11111100 txt=00000011
	int           colorAU;		// bg=11111100 txt=00000011
	int           colorAS;		// bg=11111100 txt=00000011

	CCFButton()
	{
	}

	CCFButton(CCFNode parent, String name)
	{
		setParent(new CCFChild(parent, this));
		setName(name);
		width = 50;
		height = 25;
		setFont(CCFFont.SIZE_14);
		inColor = usingColor();
		int colors = inColor ? defCOColors : defBWColors;
		colorIU = colors;
		colorIS = colors;
		colorAU = colors;
		colorAS = colors;
		checkVersion();
	}

	CCFNode getClone()
	{
		CCFButton b = (CCFButton)super.getClone();
		b.actions = getClone(b.actions);
		b.buildTree(getParent());
		return b;
	}

	public void setParent(CCFNode p)
	{
		super.setParent(p);
		if (!(p == null || p instanceof CCFChild))
		{
			throw new RuntimeException("parent ! CCFChild : "+p);
		}
	}

	public void copyColors(CCFButton src)
	{
		colorIU = src.colorIU;
		colorIS = src.colorIS;
		colorAU = src.colorAU;
		colorAS = src.colorAS;
		boolean uc = usingColor();
		boolean oc = src.usingColor();
		if (uc && !oc)
		{
			inColor = false;
			convertToColor();
		}
		else
		if (!uc && oc)
		{
			inColor = true;
			convertToGray();
		}
	}

	public void copyIcons(CCFButton src)
	{
		iconIU = src.iconIU;
		iconIS = src.iconIS;
		iconAU = src.iconAU;
		iconAS = src.iconAS;
		if (usingColor())
		{
			iconIU.convertToColor();
			iconIS.convertToColor();
			iconAU.convertToColor();
			iconAS.convertToColor();
		}
		else
		{
			iconIU.convertToGray();
			iconIS.convertToGray();
			iconAU.convertToGray();
			iconAS.convertToGray();
		}
	}

	public String toString()
	{
		//return name != null ? name : "";
		//return "'"+name+"'-"+super.toString();
		return getFQN(); // for alias lists
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
		debug.log(3,"converting "+describe()+" to color");
		CCFColor map[] = getHeader().getColorMap();
		int rgb[] = new int[] {
			map[0].getColorIndex(),
			map[1].getColorIndex(),
			map[2].getColorIndex(),
			map[3].getColorIndex(),
		};
		colorIU = CCFColor.ccfGrayToRGB(colorIU, rgb);
		colorIS = CCFColor.ccfGrayToRGB(colorIS, rgb);
		colorAU = CCFColor.ccfGrayToRGB(colorAU, rgb);
		colorAS = CCFColor.ccfGrayToRGB(colorAS, rgb);
		inColor = getHeader().hasColor();
	}

	public void convertToGray()
	{
		if (!inColor)
		{
			return;
		}
		debug.log(3,"converting "+describe()+" to gray");
		colorIU = CCFColor.ccfRGBToGray(colorIU);
		colorIS = CCFColor.ccfRGBToGray(colorIS);
		colorAU = CCFColor.ccfRGBToGray(colorAU);
		colorAS = CCFColor.ccfRGBToGray(colorAS);
		inColor = getHeader().hasColor();
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
	 * Get the CCFIconSet associated with this Button.
	 */
	public CCFIconSet getIconSet()
	{
		return new CCFIconSet(
			new CCFIcon[] {
				iconIU, iconIS,
				iconAU, iconAS
			},
			new int[] {
				colorIU, colorIS,
				colorAU, colorAS
			},
			codec == color
		);
	}

	/**
	 * Set the CCFIconSet associated with this Button.
	 *
	 * @param set new set of state icons
	 */
	public void setIconSet(CCFIconSet set)
	{
		iconIU = set.icons[0];
		iconIS = set.icons[1];
		iconAU = set.icons[2];
		iconAS = set.icons[3];
		colorIU = set.colors[0];
		colorIS = set.colors[1];
		colorAU = set.colors[2];
		colorAS = set.colors[3];
	}

	/**
	 * Returns the size of the button which is overridden by
	 * the size of the Button's active unclicked icon.
	 */
	public Dimension getSize()
	{
		return new Dimension(width(), height());
	}

	/**
	 * Sets the size of the button.
	 */
	public void setSize(Dimension size)
	{
		width = size.width;
		height = size.height;
	}

	public CCFChild getChildWrapper()
	{
		if (getParent() == null)
		{
			System.out.println(">>>>>> parent null for "+this);
		}
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

	public CCFAction[] getActions()
	{
		if (actions != null)
		{
			return actions.getActions();
		}
		else
		{
			return null;
		}
	}

	/**
	 * Returns the action list that is performed when this
	 * Button is clicked.
	 */
	public CCFActionList getActionList()
	{
		return actions;
	}

	public void appendAction(CCFAction a)
	{
		if (actions == null)
		{
			setActionList(new CCFActionList());
		}
		actions.appendAction(a);
	}

	/**
	 * Sets the action list that is performed when this
	 * Button is clicked.
	 */
	public void setActionList(CCFActionList actions)
	{
		this.actions = actions;
		this.actions.setParent(this);
	}

	/**
	 * Return the name of this Button.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Set the name of this Button.
	 *
	 * @param name button name
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Return the ID tag for this Button.
	 */
	public String getIDTag()
	{
		return idtag;
	}

	/**
	 * Set the ID tag for this Button.
	 *
	 * @param idtag ID Tag
	 */
	public void setIDTag(String idtag)
	{
		this.idtag = idtag;
	}

	/**
	 * Get the background color for this button. This is
	 * derived from the active/inactive state icon associated
	 * with the buttons current state. For display purposes.
	 */
	public CCFColor getBackground()
	{
		return CCFColor.getBackground(isActive() ? colorAU: colorIU, inColor);
	}

	/**
	 * Get the foreground color for this button. This is
	 * derived from the active/inactive state icon associated
	 * with the buttons current state. For display purposes.
	 */
	public CCFColor getForeground()
	{
		return CCFColor.getForeground(isActive() ? colorAU: colorIU, inColor);
	}

	/**
	 * Returns true of the button has an associated action list
	 * containing at least one item.
	 */
	public boolean isActive()
	{
		return actions != null && actions.size() > 0;
	}

	/**
	 * Returns true if this button is resizable in it's current
	 * state (eg: an icon for this state exists).
	 */
	public boolean isResizable()
	{
		return isActive() ?
			(iconAU == null) : (iconIU == null);
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
	String getFQN()
	{
		CCFPanel pp = getParentPanel();
		return (pp != null ? pp.getFQN() : "<deleted>")+" ("+(name != null ? name : "")+")";
	}

	int width()
	{
		return isActive() ?
			(iconAU != null ? iconAU.width : width) :
			(iconIU != null ? iconIU.width : width);
	}

	int height()
	{
		return isActive() ?
			(iconAU != null ? iconAU.height : height) :
			(iconIU != null ? iconIU.height : height);
	}

	// ---( override methods )---
	String describe()
	{
		return "Button,"+name;
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
		if (actions != null)
		{
			actions.buildTree(this);
		}
	}

	// ---( instance methods )---
}


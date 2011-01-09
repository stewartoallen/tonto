/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---

/**
 * A set of four icons associated with a CCFButton. These four
 * icons represent the four possible states a button can occupy:
 * active/unselected, active/selected, inactive/unselected, and
 * inactive/selected. An inactive button is one which posesses
 * no associated actions. It is hidden by the display. Active
 * buttons have at least one associated action.
 */
public class CCFIconSet
{
	// ---( static fields )---
	public final static int INACTIVE_UNSELECTED = 0;
	public final static int INACTIVE_SELECTED   = 1;
	public final static int ACTIVE_UNSELECTED   = 2;
	public final static int ACTIVE_SELECTED     = 3;

	private final static int[] states = {
		INACTIVE_UNSELECTED,
		INACTIVE_SELECTED,
		ACTIVE_UNSELECTED,
		ACTIVE_SELECTED,
	};

	// ---( static methods )---
	public static int[] getValidStates()
	{
		return states;
	}

	// ---( constructors )---
	CCFIconSet (CCFIcon i[], int c[], boolean ic)
	{
		icons = i;
		colors = c;
		inColor = ic;
	}

	CCFIconSet (boolean inColor)
	{
		this(new CCFIcon[4], new int[4], inColor);
	}

	// ---( instance fields )---
	CCFIcon icons[];
	int colors[];
	boolean inColor;

	public CCFIconSet getClone()
	{
		return new CCFIconSet(icons, colors, inColor);
	}

	// ---( instance methods )---
	public CCFIcon getIcon(int state)
	{
		return icons[state];
	}

	public CCFIcon getDefaultIcon()
	{
		return icons[ACTIVE_UNSELECTED] != null ? icons[ACTIVE_UNSELECTED] : icons[INACTIVE_UNSELECTED];
	}

	/**
	 * Set an icon for the specified button state.
	 *
	 * @param state specified button state
	 * @param icon new icon for this state
	 */
	public void setIcon(int state, CCFIcon icon)
	{
		icons[state] = icon;
	}

	/**
	 * Return the icon foreground color for this state.
	 *
	 * @param state specified button state
	 */
	public CCFColor getForeground(int state)
	{
		return CCFColor.getForeground(colors[state], inColor);
	}

	/**
	 * Return the icon background color for this state.
	 *
	 * @param state specified button state
	 */
	public CCFColor getBackground(int state)
	{
		return CCFColor.getBackground(colors[state], inColor);
	}

	/**
	 * Set the icon foreground color for this state.
	 *
	 * @param state specified button state
	 * @param color new color
	 */
	public void setForeground(int state, CCFColor color)
	{
		colors[state] = CCFColor.getComposite(color, getBackground(state), inColor);
	}

	/**
	 * Set the icon background color for this state.
	 *
	 * @param state specified button state
	 * @param color new color
	 */
	public void setBackground(int state, CCFColor color)
	{
		colors[state] = CCFColor.getComposite(getForeground(state), color, inColor);
	}

	// ---( interface methods )---

}


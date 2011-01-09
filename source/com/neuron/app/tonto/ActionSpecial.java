/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---

/**
 * Action that handles special state cases like jump back/forward and mouse mode.
 */
public class ActionSpecial extends CCFAction
{
	// ---( static fields )---

	// ---( static methods )---

	// ---( constructors )---
	ActionSpecial(CCFAction copy)
	{
		copy(copy);
		if (!isSpecialJump())
		{
			throw new IllegalArgumentException();
		}
	}

	public ActionSpecial (int code)
	{
		super(ACT_JUMP_PANEL, 0, code);
		if (!isSpecialJump())
		{
			throw new IllegalArgumentException();
		}
	}

	// ---( instance fields )---
	/**
	 * Get the current panel associated with this action.
	 */
	public int getSpecial()
	{
		return p2;
	}

	/**
	 * Set the current panel associated with this action.
	 */
	public void setSpecial(int code)
	{
		p2 = code;
	}

	// ---( instance methods )---
	boolean useParentFields()
	{
		return true;
	}

	// ---( interface methods )---

}


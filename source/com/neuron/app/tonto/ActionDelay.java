/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---

/**
 * Action that waits for a specified number of milliseconds.
 */
public class ActionDelay extends CCFAction
{
	// ---( static fields )---

	// ---( static methods )---

	// ---( constructors )---
	ActionDelay(CCFAction copy)
	{
		copy(copy);
	}

	public ActionDelay (int time)
	{
		type = ACT_DELAY;
		setDelay(time);
	}

	// ---( instance fields )---
	/**
	 * Get the current delay in milliseconds associated
	 * with this action.
	 */
	public int getDelay()
	{
		return p2;
	}

	/**
	 * Set the current delay associated with this action.
	 *
	 * @param time time in milliseconds
	 */
	public void setDelay(int time)
	{
		p2 = time;
	}

	// ---( instance methods )---
	boolean useParentFields()
	{
		return true;
	}

	// ---( interface methods )---

}


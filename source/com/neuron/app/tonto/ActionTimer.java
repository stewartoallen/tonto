/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---

/**
 * Action that activates a Timer.
 */
public class ActionTimer extends CCFAction
{
	// ---( static fields )---

	// ---( static methods )---

	// ---( constructors )---
	ActionTimer(CCFAction copy)
	{
		copy(copy);
	}

	public ActionTimer (CCFTimer timer)
	{
		type = ACT_TIMER;
		setTimer(timer);
	}

	// ---( instance fields )---
	/**
	 * Get the current timer associated with this action.
	 */
	public CCFTimer getTimer()
	{
		return (CCFTimer)action2;
	}

	/**
	 * Set the current timer associated with this action.
	 */
	public void setTimer(CCFTimer timer)
	{
		action2 = timer;
	}

	// ---( instance methods )---
	boolean useParentFields()
	{
		return true;
	}

	// ---( interface methods )---

}


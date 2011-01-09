/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---

/**
 * Action that switches active devices.
 */
public class ActionAliasDevice extends CCFAction
{
	// ---( static fields )---

	// ---( static methods )---

	// ---( constructors )---
	ActionAliasDevice(CCFAction copy)
	{
		copy(copy);
	}

	public ActionAliasDevice (CCFDevice dev)
	{
		type = ACT_ALIAS_DEV;
		setDevice(dev);
	}

	// ---( instance fields )---
	/**
	 * Get the current device associated with this action.
	 */
	public CCFDevice getDevice()
	{
		return (CCFDevice)action1;
	}

	/**
	 * Set the current device associated with this action.
	 */
	public void setDevice(CCFDevice device)
	{
		action1 = device;
	}

	// ---( instance methods )---
	boolean useParentFields()
	{
		return true;
	}

	// ---( interface methods )---
}


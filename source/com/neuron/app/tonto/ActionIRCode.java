/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---

/**
 * Action that sends an IR Code
 */
public class ActionIRCode extends CCFAction
{
	// ---( static fields )---

	// ---( static methods )---

	// ---( constructors )---
	ActionIRCode(CCFAction copy)
	{
		copy(copy);
	}

	public ActionIRCode (CCFIRCode code)
	{
		type = ACT_IRCODE;
		setIRCode(code);
	}

	// ---( instance fields )---
	/**
	 * Get the current IRCode associated with this action.
	 */
	public CCFIRCode getIRCode()
	{
		return (CCFIRCode)action2;
	}

	/**
	 * Set the current IRCode associated with this action.
	 */
	public void setIRCode(CCFIRCode code)
	{
		p1 = 0;
		action2 = code;
	}

	// ---( instance methods )---
	boolean useParentFields()
	{
		return true;
	}

	// ---( interface methods )---

}


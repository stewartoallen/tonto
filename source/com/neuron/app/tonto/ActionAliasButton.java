/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---

/**
 * Action that performs a virtual push of a button.
 */
public class ActionAliasButton extends CCFAction
{
	// ---( static fields )---

	// ---( static methods )---

	// ---( constructors )---
	ActionAliasButton(CCFAction copy)
	{
		copy(copy);
	}

	public ActionAliasButton (CCFButton button)
	{
		type = ACT_ALIAS_BUTTON;
		setButton(button);
	}

	// ---( instance fields )---
	/**
	 * Get the current button associated with this action.
	 */
	public CCFButton getButton()
	{
		return (CCFButton)action2;
	}

	/**
	 * Set the current button associated with this action.
	 */
	public void setButton(CCFButton button)
	{
		action2 = button;
		action1 = button.getParentDevice();
	}

	// ---( instance methods )---
	boolean useParentFields()
	{
		return true;
	}

	// ---( interface methods )---

}


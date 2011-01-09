/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---

/**
 * Action that switches the currently displayed panel.
 */
public class ActionJumpPanel extends CCFAction
{
	// ---( static fields )---

	// ---( static methods )---

	// ---( constructors )---
	ActionJumpPanel(CCFAction copy)
	{
		copy(copy);
	}

	public ActionJumpPanel (CCFPanel panel, boolean marantz)
	{
		type = marantz ? ACT_MARANTZ_JUMP : ACT_JUMP_PANEL;
		setPanel(panel);
	}

	// ---( instance fields )---
	/**
	 * Get the current panel associated with this action.
	 */
	public CCFPanel getPanel()
	{
		return (CCFPanel)action2;
	}

	/**
	 * Set the current panel associated with this action.
	 */
	public void setPanel(CCFPanel panel)
	{
		action2 = panel;
		action1 = panel != null ? panel.getParentDevice() : null;
	}

	// ---( instance methods )---
	boolean useParentFields()
	{
		return true;
	}

	// ---( interface methods )---

}


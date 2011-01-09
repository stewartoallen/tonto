/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---

/**
 * Action that performs a virtual key-press. Key are the
 * fixed buttons on the case of the Pronto. These are distinguished
 * from buttons which are graphics on the LCD touch display.
 */
public class ActionAliasKey extends CCFAction
{
	// ---( static fields )---
	public static final int KEY_LEFT      = 0;
	public static final int KEY_RIGHT     = 1;
	public static final int KEY_VOL_DOWN  = 2;
	public static final int KEY_VOL_UP    = 3;
	public static final int KEY_CHAN_DOWN = 4;
	public static final int KEY_CHAN_UP   = 5;
	public static final int KEY_MUTE      = 6;

	// ---( static methods )---

	// ---( constructors )---
	ActionAliasKey(CCFAction copy)
	{
		copy(copy);
	}

	/**
	 * Create a key alias action. The Device is required
	 * for the constructor since keys have different behaviour
	 * depending on the currently active device.
	 *
	 * @param dev CCFDevice for current key
	 * @param key key code ... see class statics
	 */
	public ActionAliasKey (CCFDevice dev, int key)
	{
		type = ACT_ALIAS_KEY;
		setDevice(dev);
		setKey(key);
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
	public void setDevice(CCFDevice dev)
	{
		action1 = dev;
	}

	/**
	 * Get the current key associated with this action.
	 */
	public int getKey()
	{
		return p2;
	}

	/**
	 * Set the current key associated with this action.
	 *
	 * @param key key code ... see class statics
	 */
	public void setKey(int key)
	{
		p2 = key;
	}

	// ---( instance methods )---
	boolean useParentFields()
	{
		return true;
	}

	// ---( interface methods )---
}


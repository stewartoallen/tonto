/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---

/**
 * Interface for CCF tree walk callbacks.
 */
public interface IWalker
{
	// ---( interface methods )---
	/**
	 * Called for each Device, Panel, Frame and Button walked
	 * in a CCF parse tree.
	 */
	public void onNode(CCFNode node)
		;
}


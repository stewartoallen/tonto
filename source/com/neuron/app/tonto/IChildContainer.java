/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---

public interface IChildContainer
{
	public CCFChild[] getChildren()
		;

	public void addButton(CCFButton b)
		;

	public void addFrame(CCFFrame f)
		;

	public void setChildren(CCFChild c[])
		;
}


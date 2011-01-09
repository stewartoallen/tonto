/*
 * Copyright 2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---

public class ScopeTask implements ITaskStatus
{
	// ---( static fields )---

	// ---( static methods )---

	// ---( constructors )---
	public ScopeTask (ITaskStatus status, int min, int max)
	{
		this.status = status;
		this.min = min;
		this.mult = (double)(max-min)/100.0;
	}

	// ---( instance fields )---
	private ITaskStatus status;
	private int min;
	private double mult;

	// ---( instance methods )---

	// ---( interface methods )---
	public void taskStatus(int pct, String msg)
	{
		if (status != null)
		{
			status.taskStatus(min+(int)(mult*(double)pct), msg);
		}
	}

	public void taskError(Throwable t)
	{
		if (status != null)
		{
			status.taskError(t);
		}
	}

	public void taskNotify(Object o)
	{
		if (status != null)
		{
			status.taskNotify(o);
		}
	}
}


/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---

public interface ITaskStatus
{
	public void taskStatus(int pct, String value)
		;

	public void taskError(Throwable error)
		;

	public void taskNotify(Object val)
		;
}


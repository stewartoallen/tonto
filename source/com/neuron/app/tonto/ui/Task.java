/*
 * Copyright 2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto.ui;

// ---( imports )---
import com.neuron.app.tonto.ITaskStatus;

public abstract class Task
{
	private String name;
	private int weight;

	public Task(String name)
	{
		this(name, 1);
	}

	public Task(String name, int weight)
	{
		this.name = name;
		this.weight = weight;
	}

	public String getName()
	{
		return name;
	}

	public int getWeight()
	{
		return weight;
	}

	public abstract void invoke(ITaskStatus status) ;
}


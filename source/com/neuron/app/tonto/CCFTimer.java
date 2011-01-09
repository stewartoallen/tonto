/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.*;

/**
 * A class representing timed and automated actions.
 * This class is not yet fully implemented.
 */
public class CCFTimer extends CCFNode
{
	// ---( instance fields )---
	CCFTimer   next;
	int        startDays;
	int        _reserve_1;
	int        startTime;
	int        endDays;
	int        _reserve_2;
	int        endTime;
	CCFAction  startAction;
	CCFAction  endAction;

	private final static String[][] codec =
	{
		{ "Z*", "next" },
		{ "N1", "startDays" },
		{ "N1", "_reserve_1" },
		{ "N2", "startTime" },
		{ "N1", "endDays" },
		{ "N1", "_reserve_2" },
		{ "N2", "endTime" },
		{ "Z1", "startAction" },
		{ "Z1", "endAction" },
	};

	CCFTimer()
	{
		this(null);
	}

	CCFTimer(CCFNode parent)
	{
		setParent(parent);
		startAction = new CCFAction();
		endAction = new CCFAction();
	}

	// ---( override methods )---
	String describe()
	{
		return time(startTime)+" to "+time(endTime);
	}

	String time(int t)
	{
		return (t/60)+":"+pad(t%60);
	}

	private String pad(int t)
	{
		if (t < 10)
		{
			return "0"+t;
		}
		else
		{
			return Integer.toString(t);
		}
	}

	// ---( abstract methods )---
	void checkVersion()
	{
	}

	void preEncode(CCFNodeState zs)
	{
		if (startAction == null)
		{
			startAction = new CCFAction();
		}
		if (endAction == null)
		{
			endAction = new CCFAction();
		}
	}

	void preDecode(CCFNodeState zs)
	{
	}

	void postDecode(CCFNodeState zs)
	{
		//dump();
	}

	String[][] getEncodeTable()
	{
		return codec;
	}

	String[][] getDecodeTable()
	{
		return codec;
	}	

	void buildTree(CCFNode parent)
	{
	}
}


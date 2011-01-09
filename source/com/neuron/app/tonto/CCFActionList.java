/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.*;
import java.util.Vector;

/**
 * A container for Actions that can be attached to buttons
 * and panels. By using a container for action lists, a series
 * of actions can be shared by multiple buttons or panels without
 * duplicating that list.
 */
public class CCFActionList extends CCFNode
{
	// ---( instance fields )---
	int        count1;
	int        count2;		// same as count1
	CCFAction  action[];	// NEVER set directly!

	public CCFActionList()
	{
	}

	CCFActionList(CCFNode parent)
	{
		setParent(parent);
	}

	private final static String[][] codec =
	{
		{ "N1", "count1" },
		{ "N1", "count2" },
		{ "Z+", "action", "count1" },
	};

	public String toString()
	{
		return "CCFActionList["+count1+"]";
	}

	public int size()
	{
		return count1;
	}

	public CCFNode getClone()
	{
		CCFActionList z = (CCFActionList)super.getClone();
		z.action = getClone(z.action);
		z.buildTree(getParent());
		return z;
	}

	private CCFAction[] getClone(CCFAction[] list)
	{
		if (list == null)
		{
			return null;
		}
		CCFAction nl[] = new CCFAction[list.length];
		for (int i=0; i<list.length; i++)
		{
			nl[i] = (CCFAction)list[i].getClone();
		}
		return nl;
	}

	boolean deleteMatching(CCFNode node)
	{
		boolean remake = false;
		for (int i=0; i<count1; i++)
		{
			if (action[i].match(node))
			{
				action[i] = null;
				remake = true;
			}
		}
		if (remake)
		{
			cullEmpty();
		}
		return remake;
	}

	void cullEmpty()
	{
		log(2,"culling empty for "+this);
		Vector v = new Vector();
		for (int i=0; i<count1; i++)
		{
			if (action[i] != null)
			{
				v.add(action[i]);
			}
		}
		CCFAction na[] = new CCFAction[v.size()];
		setActions((CCFAction[])v.toArray(na));
	}

	void checkCullEmpty(CCFNodeState zs)
	{
		if (action == null)
		{
			return;
		}
		boolean cull = false;
		for (int i=0; i<count1; i++)
		{
			if (!action[i].willEncode(zs))
			{
				debug.log(2,"culling "+action[i]+" from "+this);
				action[i] = null;
				cull = true;
			}
		}
		if (cull)
		{
			cullEmpty();
		}
	}

	// ---( public API )---
	/**
	 * Get the list of actions associated with this container.
	 */
	public CCFAction[] getActions()
	{
		CCFAction ret[] = new CCFAction[count1];
		for (int i=0; i<count1; i++)
		{
			ret[i] = action[i].decodeReplace();
		}
		return ret;
	}

	/**
	 * Set the list of actions associated with this container.
	 */
	public void setActions(CCFAction a[])
	{
		action = a;
		count1 = a.length;
		count2 = a.length;
	}

	public void appendAction(CCFAction a)
	{
		if (action == null)
		{
			action = new CCFAction[0];
		}
		CCFAction na[] = new CCFAction[action.length+1];
		System.arraycopy(action, 0, na, 0, action.length);
		na[na.length-1] = a;
		setActions(na);
	}

	// ---( abstract methods )---
	void checkVersion()
	{
	}

	void preEncode(CCFNodeState zs)
	{
		checkCullEmpty(zs);
		if (zs.getHeader().isNewMarantz() && count1 > 0)
		{
			int type = CCFAction.ACT_JUMP_PANEL;
			for (int i=action.length-1; i>= 0; i--)
			{
				if (action[i].isJump())
				{
					action[i].type = type;
				}
				type = CCFAction.ACT_MARANTZ_JUMP;
			}
		}
	}

	void preDecode(CCFNodeState zs)
	{
	}

	void postDecode(CCFNodeState zs)
	{
		if (action == null)
		{
			throw new NullPointerException("Action list is null");
		}
		if (count1 != count2)
		{
			int min = Math.min(Math.min(count1,count2),10);
			log(0,"count mismatch "+count1+" != "+count2+". using "+min, zs);
			count1 = min;
			count2 = min;
		}
		//checkCullEmpty(zs);
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
		setParent(parent);
		for (int i=0; i<count1; i++)
		{
			action[i].buildTree(this);
		}
	}

	// ---( override methods )---
	String describe()
	{
		return "ACTNList,"+count1;
	}
}



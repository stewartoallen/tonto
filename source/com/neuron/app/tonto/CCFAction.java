/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.*;
import java.util.*;

/**
 * The CCFAction objects are collected into CCFActionLists
 * and attached to buttons, panels or panel state changes.
 */
public class CCFAction extends CCFNode
{
	private final static String[][] codec =
	{
		{ "N1", "type" },
		{ "N4", "p1" },
		{ "N4", "p2" },
	};

	// ---( static fields )---
	public final static int ACT_DUMMY        = 0; // p1=?      p2=?
	public final static int ACT_IRCODE       = 1; //           p2=codePtr
	public final static int ACT_ALIAS_BUTTON = 2; // p1=devPtr p2=buttonPtr
	public final static int ACT_JUMP_PANEL   = 3; // p1=devPtr p2=panelPtr
	public final static int ACT_DELAY        = 4; //           p2=delay(ms)
	public final static int ACT_ALIAS_KEY    = 5; // p1=devPtr p2=keyCode
	public final static int ACT_ALIAS_DEV    = 6; // p1=devPtr
	public final static int ACT_TIMER        = 7; //           p2=timerPtr
	public final static int ACT_BEEP         = 8; //           p2=param code
	public final static int ACT_MARANTZ_JUMP = 9; // same as jump but for Marantz x200

	// for 32 beep bits:  aaaaaaaa bbbbbbbb bbbbbbbb cccccccc
	//   a = duration in milliseconds / 10 
	//   b = frequency in Hz
	//   c = duty cycle (0-100%)

	// instead of pointing to a panel, use these other features
	public final static int JUMP_FORWARD     = 0xbbbbbbbb;
	public final static int JUMP_BACK        = 0xcccccccc;
	public final static int JUMP_SCROLL_DOWN = 0xdddddddd;
	public final static int JUMP_SCROLL_UP   = 0xeeeeeeee;
	public final static int JUMP_MOUSE_MODE  = 0xffffffff;

	public final static String jumpSpecial[] = {
		"Forward", "Back", "Scroll Down", "Scroll Up", "Mouse Mode"
	};

	public final static int jumpSpecialID[] = {
		JUMP_FORWARD,
		JUMP_BACK,
		JUMP_SCROLL_DOWN,
		JUMP_SCROLL_UP,
		JUMP_MOUSE_MODE,
	};

	// FIXME tonto dependence
	public final static int KEY_LEFT     = 0;
	public final static int KEY_RIGHT    = 1;
	public final static int KEY_VOLDOWN  = 2;
	public final static int KEY_VOLUP    = 3;
	public final static int KEY_CHANDOWN = 4;
	public final static int KEY_CHANUP   = 5;
	public final static int KEY_MUTE     = 6;

	public final static String ProntoKeys[] = {
		"Left", "Right", "Vol-", "Vol+", "CH-", "CH+", "Mute"
	};

	public final static String ProntoProKeys[] = {
		"Left", "Right", "Vol-", "Vol+", "CH-", "CH+", "Mute",
		"Far Left", "Far Right"
	};

	public final static String MarantzKeys[] = {
		"Left", "Right", "Mute", "CH-", "CH+", "Vol-", "Vol+",
		"Menu", "End", "Exit", "Up Arrow", "Left Arrow",
		"Right Arrow", "Down Arrow", "Home", "Light", "Back", "Ahead"
	};

	public final static String CustomKeys[] = {
		"Left", "Right", "Vol-", "Vol+", "CH-", "CH+", "Mute",
		"Far Left", "Far Right", "Enter", "ArrowUp", "ArrowLeft",
		"ArrowRight", "ArrowDown"
	};

	// ---( instance fields )---
	int      type = ACT_DUMMY;
	int      p1 = 0;
	int      p2 = 5;
	CCFNode  action1;
	CCFNode  action2;

	private boolean valid;

	CCFAction()
	{
		setValid(true);
	}

	CCFAction(int type, int p1, int p2)
	{
		this();
		this.type = type;
		this.p1 = p1;
		this.p2 = p2;
		setFixedPosition(true);
	}

	CCFAction(int type, CCFNode action1, CCFNode action2)
	{
		this();
		this.type = type;
		this.action1 = action1;
		this.action2 = action2;
		setFixedPosition(true);
	}

	// ---( public API )---
	/**
	 * Returns this action type.
	 */
	public int getActionType()
	{
		return type;
	}

	// ---( utility methods )---
	boolean match(CCFNode node)
	{
		return (action1 == node) || (action2 == node);
	}

	boolean willEncode(CCFNodeState zs)
	{
		switch (type)
		{
			case ACT_ALIAS_BUTTON:
			case ACT_ALIAS_KEY:
			case ACT_ALIAS_DEV:
			case ACT_JUMP_PANEL:
			case ACT_MARANTZ_JUMP:
				if (action1 != null && action1.getParent() == null)
				{
					debug.log(0, "a1("+action1.getClass()+") has no parent");
					//new Exception("Trace").printStackTrace();
					return false;
				}
				if (action2 != null && action2.getParent() == null)
				{
					debug.log(0, "a2("+action2.getClass()+") has no parent");
					//new Exception("Trace").printStackTrace();
					return false;
				}
				break;
		}
		return valid && zs.willEncode(action1) && zs.willEncode(action2);
	}

	CCFNode getClone()
	{
		CCFAction a = (CCFAction)super.getClone();
		if ((type == ACT_IRCODE || type == ACT_TIMER) && action2 != null)
		{
			a.action2 = action2.getClone();
		}
		a.buildTree(getParent());
		return a;
	}

	void copy(CCFAction copy)
	{
		setParent(copy.getParent());
		type = copy.type;
		p1 = copy.p1;
		p2 = copy.p2;
		action1 = copy.action1;
		action2 = copy.action2;
		valid = copy.valid;
	}

	CCFAction decodeReplace()
	{
		switch (type)
		{
			case ACT_IRCODE       : return new ActionIRCode(this);
			case ACT_ALIAS_BUTTON : return new ActionAliasButton(this);
			case ACT_DELAY        : return new ActionDelay(this);
			case ACT_ALIAS_KEY    : return new ActionAliasKey(this);
			case ACT_ALIAS_DEV    : return new ActionAliasDevice(this);
			case ACT_TIMER        : return new ActionTimer(this);
			case ACT_BEEP         : return new ActionBeep(this);
			case ACT_JUMP_PANEL   :
			case ACT_MARANTZ_JUMP :
				if (isSpecialJump())
				{
					return new ActionSpecial(this);
				}
				else
				{
					return new ActionJumpPanel(this);
				}
		}
		debug.log(0, "unknown action ("+type+","+hex(p1)+","+hex(p2)+")");
		return this;
	}

	// ---( override methods )---
	// prevent references to deleted panels, buttons and devices
	void encodePrep(CCFNodeState zs, Hashtable cache)
	{
		if (action2 != null &&
			!(action2 instanceof CCFPanel) &&
			!(action2 instanceof CCFButton))
		{
			zs.addField(action2, cache);
		}
	}

	// ---( abstract methods )---
	void checkVersion()
	{
	}

	void preEncode(CCFNodeState zs)
	{
		// filepos vars only valid at time of encode!
		// this cannot be optimized away
		if (action1 != null)
		{
			p1 = action1.getFilePosition();
		}
		if (action2 != null)
		{
			p2 = action2.getFilePosition();
		}
		fixDevicePointer();
	}

	void preDecode(CCFNodeState zs)
	{
		action1 = null;
		action2 = null;
		setValid(true);
	}

	void postDecode(CCFNodeState zs)
	{
		switch (type)
		{
			case ACT_DUMMY:
				//dump();
				break;
			case ACT_DELAY:
			case ACT_BEEP:
				p1 = 0;
				break;
			case ACT_IRCODE:
				p1 = 0;
				action2 = getItemByPos(zs, p2, CCFIRCode.class);
				setValid(action2 != null);
				break;
			case ACT_ALIAS_BUTTON:
				action1 = getItemByPos(zs, p1, CCFDevice.class);
				action2 = getItemByPos(zs, p2, CCFButton.class);
				setValid(action1 != null && action2 != null);
				break;
			case ACT_JUMP_PANEL:
			case ACT_MARANTZ_JUMP:
				if (!isSpecialJump())
				{
					action1 = getItemByPos(zs, p1, CCFDevice.class);
					action2 = getItemByPos(zs, p2, CCFPanel.class);
					setValid(action1 != null && action2 != null);
				}
				break;
			case ACT_ALIAS_KEY:
				action1 = getItemByPos(zs, p1, CCFDevice.class);
				setValid(action1 != null);
				break;
			case ACT_ALIAS_DEV:
				action1 = getItemByPos(zs, p1, CCFDevice.class);
				setValid(action1 != null);
				break;
			case ACT_TIMER:
				p1 = 0;
				action2 = getItemByPos(zs, p2, CCFTimer.class);
				setValid(action2 != null);
				break;
		}
		//dump();
	}

	private void setValid(boolean valid)
	{
		this.valid = valid;
	}

	String[][] getDecodeTable()
	{
	 	return codec;
	}	

	String[][] getEncodeTable()
	{
		return codec;
	}

	void buildTree(CCFNode parent)
	{
		setParent(parent);
		if (action2 instanceof CCFIRCode)
		{
			action2.buildTree(this);
		}
	}

	static String[] getKeyNames(CCFDevice dev)
	{
		CCFHeader head = dev.getHeader();
		if (head.isCustom()) { return CustomKeys; }
		if (head.isNewMarantz()) { return MarantzKeys; }
		if (head.hasColor()) { return ProntoProKeys; }
		return ProntoKeys;
	}

	static String getKeyName(CCFDevice dev, int key)
	{
		String str[] = getKeyNames(dev);
		if (key >= str.length)
		{
			return "<INVALID>";
		}
		else
		{
			return str[key];
		}
	}

	public static String getJumpSpecialString(int val)
	{
		return jumpSpecial[((val>>24)&0xf)-0xb];
	}

	public static int getJumpSpecialIDFromString(String str)
	{
		for (int i=0; i<jumpSpecial.length; i++)
		{
			if (jumpSpecial[i].equals(str))
			{
				return jumpSpecialID[i];
			}
		}
		return 0;
	}

	public boolean isJump()
	{
		return type == ACT_JUMP_PANEL || type == ACT_MARANTZ_JUMP;
	}

	public boolean isSpecialJump()
	{
		return (((p2>>24)&0xf)-0xb) >= 0;
	}

	// ---( instance methods )---
	public String toString()
	{
		fixDevicePointer();
		CCFDevice dev = (CCFDevice)action1;
		switch (type)
		{
			case ACT_IRCODE:
				return "[IR] "+((CCFIRCode)action2).getName();
			case ACT_ALIAS_BUTTON:
				CCFButton b = (CCFButton)action2;
				return "[Button] "+dev+" : "+b.getParentPanel()+" : "+(b.name != null ? b.name : "");
			case ACT_JUMP_PANEL:
			case ACT_MARANTZ_JUMP:
				int js = ((p2>>24)&0xf)-0xb;
				CCFPanel pnl = (CCFPanel)action2;
				//return "[Jump"+(type==ACT_MARANTZ_JUMP?"*":"")+"] "+
				return (type == ACT_MARANTZ_JUMP ? "[Jump] " : "[Jump] ")+
					(js >= 0 ? jumpSpecial[js] :
					(dev != null ? dev.name : "()")+" : "+
					(pnl != null ? pnl.name : "()"));
			case ACT_DELAY:
				return "[Delay] "+p2+"ms";
			case ACT_ALIAS_KEY:
				String kn = getKeyName(dev,p2);
				return "[Key] "+dev.name+
					(kn != null ? (" : "+getKeyName(dev,p2)) : "");
			case ACT_ALIAS_DEV:
				if (dev == null)
				{
					return "<undefined>";
				}
				return "[Device] "+dev.name;
			case ACT_TIMER:
				return "[Timer] "+((CCFTimer)action2).describe();
			case ACT_BEEP:
				return "[Beep] "+ActionBeep.getDutyCycle(p2)+"% "+ActionBeep.getDuration(p2)+"ms "+ActionBeep.getFrequency(p2)+"hz";
			default:
				return "<undefined>";
		}
	}

	void fixDevicePointer()
	{
		if (type == ACT_JUMP_PANEL && isSpecialJump())
		{
			return;
		}

		switch (type)
		{
			case ACT_ALIAS_BUTTON:
			case ACT_JUMP_PANEL:
				CCFDevice dev = (CCFDevice)action1;
				CCFNode node = action2;
				if (node == null)
				{
					debug.log(0, "action node is null ("+type+":"+action1+":"+p1+")");
					traceParents();
					return;
				}
				CCFDevice pdev = node.getParentDevice();
				if (pdev != dev)
				{
					if (pdev == null)
					{
						debug.log(0, "parent device is null ("+type+":"+action1+":"+action2+")");
						traceParents();
						return;
					}
					debug.log(1, "relinking device pointer ("+action1+":"+action2+")");
					action1 = pdev;
					p1 = pdev.getFilePosition();
				}
			break;
		}
	}

	// ---( instance methods )---
}


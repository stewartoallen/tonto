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
 * A container for Panels like the Home Panels, Device Panels
 * and Macro Panels. This allows for the naming of keys, attachment
 * of action lists and panel navigation management.
 */
public class CCFDevice
	extends CCFNode
	implements INamed, IListElement
{
	// ---( static fields )---
	public final static int READ_ONLY         = 0x01;
	public final static int IS_TIMER_GROUP    = 0x02;
	public final static int NEEDS_PROGRAMMING = 0x10;
	public final static int HAS_SEPARATOR     = 0x20;
	public final static int IS_TEMPLATE       = 0x40;

	private final static String[][] nocolor = new String[][]
	{
		{ "Z*", "next" },
		{ "S*", "name" },
		{ "Z*", "iconUnselected" },
		{ "Z*", "iconSelected" },
		{ "Z*", "action" },
		{ "Z*", "keyLt" },
		{ "Z*", "keyRt" },
		{ "Z*", "keyVolDn" },
		{ "Z*", "keyVolUp" },
		{ "Z*", "keyChanDn" },
		{ "Z*", "keyChanUp" },
		{ "Z*", "keyMute" },
		{ "N4", "_reserve_1" },
		{ "S*", "keyNameLt" },
		{ "S*", "keyNameRt" },
		{ "Z*", "firstPanel" },
		{ "N1", "attr" },
	};

	private final static String[][] color = new String[][]
	{
		{ "Z*", "next" },
		{ "S*", "name" },
		{ "Z*", "iconUnselected" },
		{ "Z*", "iconSelected" },
		{ "Z*", "action" },
		{ "Z*", "keyLt" },
		{ "Z*", "keyRt" },
		{ "Z*", "keyVolDn" },
		{ "Z*", "keyVolUp" },
		{ "Z*", "keyChanDn" },
		{ "Z*", "keyChanUp" },
		{ "Z*", "keyMute" },
		{ "Z*", "keyFarLt" },
		{ "Z*", "keyFarRt" },
		{ "N4", "_reserve_1" },
		{ "S*", "keyNameLt" },
		{ "S*", "keyNameRt" },
		{ "S*", "keyNameFarLt" },
		{ "S*", "keyNameFarRt" },
		{ "Z*", "firstPanel" },
		{ "N1", "attr" },
		{ "N1", "rfExtender" },
		{ "N4", "_reserve_2" },
	};

	private final static String[][] marantz = new String[][]
	{
		{ "Z*", "next" },
		{ "S*", "name" },
		{ "Z*", "iconUnselected" },
		{ "Z*", "iconSelected" },
		{ "Z*", "action" },
		{ "Z*", "keyLt" },
		{ "Z*", "keyRt" },
		{ "Z*", "keyMute" },
		{ "Z*", "keyChanDn" },
		{ "Z*", "keyChanUp" },
		{ "Z*", "keyVolDn" },
		{ "Z*", "keyVolUp" },
		{ "Z*", "keyM" },
		{ "Z*", "keyReturn" },
		{ "Z*", "keyEX" },
		{ "Z*", "arrowUp" },
		{ "Z*", "arrowLt" },
		{ "Z*", "arrowRt" },
		{ "Z*", "arrowDn" },
		{ "N4", "_reserve_1" },
		{ "N4", "_reserve_2" },
		{ "S*", "keyNameLt" },
		{ "S*", "keyNameRt" },
		{ "Z*", "firstPanel" },
		{ "N1", "attr" },
		{ "N1", "rfExtender" },
		{ "N2", "_reserve_3" },
	};

	private final static String[][] custom = new String[][]
	{
		{ "Z*", "next" },
		{ "S*", "name" },
		{ "Z*", "iconUnselected" },
		{ "Z*", "iconSelected" },
		{ "Z*", "action" },
		{ "Z*", "keyLt" },
		{ "Z*", "keyRt" },
		{ "Z*", "keyVolDn" },
		{ "Z*", "keyVolUp" },
		{ "Z*", "keyChanDn" },
		{ "Z*", "keyChanUp" },
		{ "Z*", "keyMute" },
		{ "Z*", "keyFarLt" },
		{ "Z*", "keyFarRt" },
		{ "Z*", "keyReturn" },
		{ "S*", "keyNameLt" },
		{ "S*", "keyNameRt" },
		{ "S*", "keyNameFarLt" },
		{ "S*", "keyNameFarRt" },
		{ "Z*", "firstPanel" },
		{ "N1", "attr" },
		{ "N1", "rfExtender" },
		{ "Z*", "arrowUp" },
		{ "Z*", "arrowLt" },
		{ "Z*", "arrowRt" },
		{ "Z*", "arrowDn" },
	};

	private final static String[][] nocolorKeys =
	{
		{"Left",       "keyNameLt",    "keyLt"     ,""},
		{"Right",      "keyNameRt",    "keyRt"     ,""},
		{"Vol-",       null,           "keyVolDn"  },
		{"Vol+",       null,           "keyVolUp"  },
		{"Chan-",      null,           "keyChanDn" },
		{"Chan+",      null,           "keyChanUp" },
		{"Mute",       null,           "keyMute"   },
	};

	private final static String[][] colorKeys =
	{
		{"Left",       "keyNameLt",    "keyLt"     ,""},
		{"Right",      "keyNameRt",    "keyRt"     ,""},
		{"Vol-",       null,           "keyVolDn"  },
		{"Vol+",       null,           "keyVolUp"  },
		{"Chan-",      null,           "keyChanDn" },
		{"Chan+",      null,           "keyChanUp" },
		{"Mute",       null,           "keyMute"   },
		{"Far Left",   "keyNameFarLt", "keyFarLt"  },
		{"Far Right",  "keyNameFarRt", "keyFarRt"  },
	};

	private final static String[][] marantzKeys =
	{
		{"Left",       "keyNameLt",    "keyLt"     ,""}, // "Back" under Home and Macro
		{"Right",      "keyNameRt",    "keyRt"     ,""}, // "Display" in Home, "Ahead" in Macro
		{"Vol-",       null,           "keyVolDn"  },
		{"Vol+",       null,           "keyVolUp"  },
		{"Chan-",      null,           "keyChanDn" },
		{"Chan+",      null,           "keyChanUp" },
		{"Mute",       null,           "keyMute"   },
		{"M",          null,           "keyM"      },
		{"Return",     null,           "keyReturn" },
		{"Ex",         null,           "keyEX"     },
		{"UpArrow",    null,           "arrowUp"   },
		{"LeftArrow",  null,           "arrowLt"   },
		{"RightArrow", null,           "arrowRt"   },
		{"DownArrow",  null,           "arrowDn"   },
	};

	private final static String[][] customKeys =
	{
		{"Far Left",   "keyNameFarLt", "keyFarLt"  ,""},
		{"Left",       "keyNameLt",    "keyLt"     ,""},
		{"Right",      "keyNameRt",    "keyRt"     ,""},
		{"Far Right",  "keyNameFarRt", "keyFarRt"  ,""},
		{"Vol-",       null,           "keyVolDn"  },
		{"Vol+",       null,           "keyVolUp"  },
		{"Chan-",      null,           "keyChanDn" },
		{"Chan+",      null,           "keyChanUp" },
		{"Mute",       null,           "keyMute"   },
		{"UpArrow",    null,           "arrowUp"   },
		{"LeftArrow",  null,           "arrowLt"   },
		{"RightArrow", null,           "arrowRt"   },
		{"DownArrow",  null,           "arrowDn"   },
		{"Click",      null,           "keyReturn" },
	};

	private String[][] codec = nocolor;

	// ---( instance fields )---
	CCFDevice      next;
	String         name;
	CCFIcon        iconUnselected;
	CCFIcon        iconSelected;
	CCFActionList  action;
	CCFActionList  keyLt;
	CCFActionList  keyRt;
	CCFActionList  keyFarLt;        // TSU6000 only
	CCFActionList  keyFarRt;        // TSU6000 only
	CCFActionList  keyVolDn;
	CCFActionList  keyVolUp;
	CCFActionList  keyChanDn;
	CCFActionList  keyChanUp;
	CCFActionList  keyMute;
	CCFActionList  keyM;            // marantz x200 only
	CCFActionList  keyReturn;       // marantz x200 only
	CCFActionList  keyEX;           // marantz x200 only
	CCFActionList  arrowUp;         // marantz x200 only
	CCFActionList  arrowLt;         // marantz x200 only
	CCFActionList  arrowRt;         // marantz x200 only
	CCFActionList  arrowDn;         // marantz x200 only
	String         keyNameLt;
	String         keyNameRt;
	String         keyNameFarLt;    // TSU6000 only
	String         keyNameFarRt;    // TSU6000 only
	CCFPanel       firstPanel;
	int            attr;			// see statics above
	int            rfExtender;		// TSU6000 only (1-15, 0 == none)
	int            _reserve_1;
	int            _reserve_2;
	int            _reserve_3;

	CCFDevice()
	{
	}

	CCFDevice(CCFHeader parent)
	{
		setParent(parent);
	}

	public String toString()
	{
		return name != null ? name : "<unnamed>";
	}

	public CCFNode getClone()
	{
		CCFDevice d = (CCFDevice)super.getClone();
		d.setNextDevice(null);
		d.action = getClone(d.action);
		d.keyLt = getClone(d.keyLt);
		d.keyRt = getClone(d.keyRt);
		d.keyFarLt = getClone(d.keyFarLt);
		d.keyFarRt = getClone(d.keyFarRt);
		d.keyVolDn = getClone(d.keyVolDn);
		d.keyVolUp = getClone(d.keyVolUp);
		d.keyChanDn = getClone(d.keyChanDn);
		d.keyChanUp = getClone(d.keyChanUp);
		d.keyMute = getClone(d.keyMute);
		d.keyM = getClone(d.keyM);
		d.keyReturn = getClone(d.keyReturn);
		d.keyEX = getClone(d.keyEX);
		d.arrowUp = getClone(d.arrowUp);
		d.arrowDn = getClone(d.arrowDn);
		d.arrowLt = getClone(d.arrowLt);
		d.arrowRt = getClone(d.arrowRt);
		if (d.firstPanel != null)
		{
			d.firstPanel = (CCFPanel)d.firstPanel.getClone(true);
		}
		d.buildTree(getParent());
		return d;
	}

	CCFPanel[] getPanels()
	{
		Vector v = new Vector();
		CCFPanel first = firstPanel;
		while (first != null)
		{
			v.addElement(first);
			first = first.next;
		}
		return (CCFPanel[])v.toArray(new CCFPanel[v.size()]);
	}

	public CCFAction[] getActions()
	{
		if (action != null)
		{
			return action.getActions();
		}
		else
		{
			return null;
		}
	}

	void insertBefore(CCFPanel panel, CCFPanel npanel)
	{
		if (firstPanel == null || firstPanel == panel)
		{
			npanel.setParent(this);
			npanel.next = firstPanel;
			firstPanel = npanel;
			return;
		}
		CCFPanel root = firstPanel;
		while (root.next != panel)
		{
			if (root.next == null)
			{
				break;
			}
			root = root.next;
		}
		npanel.buildTree(this);
		npanel.next = panel;
		root.next = npanel;
	}

	void delete(CCFPanel panel)
	{
		if (firstPanel == null)
		{
			return;
		}
		if (panel == firstPanel)
		{
			firstPanel = panel.next;
			panel.next = null;
		}
		else
		{
			CCFPanel tmp = firstPanel;
			while (tmp != null && panel != tmp.next)
			{
				tmp = tmp.next;
			}
			if (tmp != null && panel == tmp.next)
			{
				tmp.next = panel.next;
				panel.next = null;
			}
		}
	}

	private CCFHardKey[] getHardKeys(String[][] data)
	{
		CCFHardKey keys[] = new CCFHardKey[data.length];
		for (int i=0; i<data.length; i++)
		{
			keys[i] = new CCFHardKey(this, data[i][0], data[i][1], data[i][2]);
		}
		return keys;
	}

	// ---( public API )---
	/**
	 * Return the first panel with the specified name.
	 *
	 * @param name name of panel to retrieve
	 */
	public CCFPanel getPanelByName(String name)
	{
		CCFPanel p = firstPanel;
		while (p != null)
		{
			if (p.name != null && p.name.equals(name))
			{
				return p;
			}
			p = p.getNextPanel();
		}
		return null;
	}

	public CCFHardKey[] getHardKeys()
	{
		checkVersion();
		if (codec == nocolor)
		{
			return getHardKeys(nocolorKeys);
		}
		else
		if (codec == color)
		{
			return getHardKeys(colorKeys);
		}
		else
		if (codec == marantz)
		{
			return getHardKeys(marantzKeys);
		}
		else
		if (codec == custom)
		{
			return getHardKeys(customKeys);
		}
		else
		{
			return new CCFHardKey[] { };
		}
	}

	public void setKeyActions(CCFHardKey[] keys)
	{
		for (int i=0; i<keys.length; i++)
		{
			keys[i].copyToDevice(this);
		}
	}

	public int getRFExtender()
	{
		return rfExtender;
	}

	public void setRFExtender(int rf)
	{
		rfExtender = rf;
	}

	public IListElement getNextElement()
	{
		return next;
	}

	/**
	 * Return the next device in the list.
	 */
	public CCFDevice getNextDevice()
	{
		return next;
	}

	/**
	 * Set the next device in the list. If the next device
	 * already exists, the list will become truncated with
	 * the new next device.
	 *
	 * @param device new next device
	 */
	public void setNextDevice(CCFDevice device)
	{
		if (device != null)
		{
			device.setParent(getParent());
		}
		next = device;
	}

	/**
	 * Append a device to the end of the device list.
	 *
	 * @param device new last device
	 */
	public void appendDevice(CCFDevice device)
	{
		CCFDevice dev = this;
		while (dev.next != null)
		{
			dev = dev.next;
		}
		dev.next = device;
		device.buildTree(getParent());
	}

	public void insertAfter(CCFDevice dev)
	{
		if (next == null)
		{
			next = dev;
		}
		else
		{
			dev.next = next;
			next = dev;
		}
		dev.buildTree(getParent());
	}

	public void insertBefore(CCFDevice dev)
	{
		getHeader().insertBefore(this, dev);
	}

	/**
	 * Return the status of a Device flag. The valid flags are
	 * READ_ONLY, HAS_SEPARATOR, and IS_TEMPLATE.
	 *
	 * @param flag name of flag to check
	 * @return true if the flag is set, false otherwise
	 */
	public boolean getFlag(int flag)
	{
		return (attr & flag) == flag;
	}

	/**
	 * Set the status of a Device flag.
	 *
	 * @see #getFlag
	 * @param flag name of flag to set
	 * @param set set on or off
	 */
	public void setFlag(int flag, boolean set)
	{
		if (set)
		{
			attr = attr | flag;
		}
		else
		{
			attr = attr & (flag ^ 0xff);
		}
	}

	/**
	 * Return the name of this device.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Set the name of this device.
	 *
	 * @param name new device name
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Return the first sub-panel.
	 */
	public CCFPanel getFirstPanel()
	{
		return firstPanel;
	}

	/**
	 * Set the first sub-panel.
	 *
	 * @param panel new first panel
	 */
	public void setFirstPanel(CCFPanel panel)
	{
		this.firstPanel = panel;
	}

	/**
	 * Create a new panel.
	 * The new panel is <b>not</b> added to the device.
	 */
	public CCFPanel createPanel(String name)
	{
		return new CCFPanel(name, this);
	}

	/**
	 * Add a panel to this Device.
	 */
	public void addPanel(CCFPanel panel)
	{
		if (panel == null)
		{
			return;
		}
		panel.buildTree(this);
		CCFPanel first = firstPanel;
		if (first == null)
		{
			firstPanel = panel;
			return;
		}
		while (first.next != null)
		{
			first = first.next;
		}
		first.next = panel;
	}

	/**
	 * Remove this device from the device list.
	 *
	 * @return the new root (if any) for this linked list.
	 */
	public CCFDevice delete()
	{
		CCFDevice dev = getHeader().delete(this);
		setParent(null);
		return dev;
	}

	boolean isHomeDevice()
	{
		return getHeader().isHomeDevice(this);
	}

	boolean isNormalDevice()
	{
		return getHeader().isNormalDevice(this);
	}

	boolean isMacroDevice()
	{
		return getHeader().isMacroDevice(this);
	}

	// ---( override methods )---
	String describe()
	{
		return "Device,"+name+","+attr;
	}

	// ---( abstract methods )---
	void checkVersion()
	{
		CCFHeader head = getHeader();

		if (head.isCustom())
		{
			codec = custom;
		}
		else
		if (head.isNewMarantz())
		{
			codec = marantz;
		}
		else
		if (head.hasColor())
		{
			codec = color;
		}
		else
		{
			codec = nocolor;
		}
	}

	void preEncode(CCFNodeState zs)
	{
		_reserve_1 = 0;
		_reserve_2 = 0;
		_reserve_3 = 0;
	}

	void preDecode(CCFNodeState zs)
	{
	}

	void postDecode(CCFNodeState zs)
	{
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
		CCFPanel panel = firstPanel;
		while(panel != null)
		{
			panel.buildTree(this);
			panel = panel.getNextPanel();
		}
		if (action != null)
		{
			action.buildTree(this);
		}
	}
}


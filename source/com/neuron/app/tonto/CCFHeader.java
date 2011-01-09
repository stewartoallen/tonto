/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.*;
import java.awt.*;
import java.util.*;

class CCFHeader extends CCFNode
{
    // ---( static fields )---
	// attribute bits
    final static int AT_FACTORY_CCF     = (1 << 0);
    final static int AT_CONFIG_READONLY = (1 << 1);
    final static int AT_HOME_READONLY   = (1 << 2);
    final static int AT_MASK            = AT_FACTORY_CCF | AT_CONFIG_READONLY | AT_HOME_READONLY;

	// capability bits
	final static int CA_DEFAULT         = (1 << 0);
	final static int CA_HAS_COLOR       = (1 << 8);
	final static int CA_HAS_UNKNOWN1    = (1 << 9);
	final static int CA_IS_NEW_MARANTZ  = (1 << 11);
	final static int CA_HAS_TIMERS      = (1 << 17);
	final static int CA_HAS_UDB         = (1 << 18);
	final static int CA_CUSTOM          = 0x01020301;

    final static String gray_notimers[][] =
        {
            { "S*", "version" },
            { "N4", "_reserve_1" },
            { "S8", "id1" },
            { "N4", "crc1Pos" },
            { "N2", "year" },
            { "N1", "month" },
            { "N1", "day" },
            { "N1", "_reserve_2" },
            { "N1", "hour" },
            { "N1", "minute" },
            { "N1", "seconds" },
            { "N4", "_reserve_3" },
            { "S4", "id2" },
            { "N4", "capability" },
            { "N4", "crc2Pos" },
            { "N4", "attrPos" },
            { "Z*", "firstHome" },
            { "Z*", "firstDevice" },
            { "Z*", "firstMacro" },
            { "N4", "attr" },
            { "Z*", "macroPanel" },
            { "N4", "_reserve_4" },
        };

    final static String gray_timers[][] =
        {
            { "S*", "version" },
            { "N4", "_reserve_1" },
            { "S8", "id1" },
            { "N4", "crc1Pos" },
            { "N2", "year" },
            { "N1", "month" },
            { "N1", "day" },
            { "N1", "_reserve_2" },
            { "N1", "hour" },
            { "N1", "minute" },
            { "N1", "seconds" },
            { "N4", "_reserve_3" },
            { "S4", "id2" },
            { "N4", "capability" },
            { "N4", "crc2Pos" },
            { "N4", "attrPos" },
            { "Z*", "firstHome" },
            { "Z*", "firstDevice" },
            { "Z*", "firstMacro" },
            { "Z*", "firstTimer" },
            { "N4", "attr" },
            { "Z*", "macroPanel" },
            { "N4", "_reserve_4" },
        };

    final static String color_notimers[][] =
        {
            { "S*", "version" },
            { "N4", "_reserve_1" },
            { "S8", "id1" },
            { "N4", "crc1Pos" },
            { "N2", "year" },
            { "N1", "month" },
            { "N1", "day" },
            { "N1", "_reserve_2" },
            { "N1", "hour" },
            { "N1", "minute" },
            { "N1", "seconds" },
            { "N4", "_reserve_3" },
            { "S4", "id2" },
            { "N4", "capability" },
            { "N4", "crc2Pos" },
            { "N4", "attrPos" },
            { "Z*", "firstHome" },
            { "Z*", "firstDevice" },
            { "Z*", "firstMacro" },
            { "N4", "attr" },
            { "Z*", "macroPanel" },
            { "N1", "channelID" },
        };

    final static String color_timers[][] =
        {
            { "S*", "version" },
            { "N4", "_reserve_1" },
            { "S8", "id1" },
            { "N4", "crc1Pos" },
            { "N2", "year" },
            { "N1", "month" },
            { "N1", "day" },
            { "N1", "_reserve_2" },  // used by 'custom' for transparent color
            { "N1", "hour" },
            { "N1", "minute" },
            { "N1", "seconds" },
            { "N4", "_reserve_3" },  // used by 'custom' for panel width/height
            { "S4", "id2" },
            { "N4", "capability" },
            { "N4", "crc2Pos" },
            { "N4", "attrPos" },
            { "Z*", "firstHome" },
            { "Z*", "firstDevice" },
            { "Z*", "firstMacro" },
            { "Z*", "firstTimer" },
            { "N4", "attr" },
            { "Z*", "macroPanel" },
            { "N4", "_reserve_4" },
            { "N3", "_reserve_5" },
            { "N1", "channelID" },
        };


    // ---( instance fields )---
    String      version;         // shows up in setup, properties panel
    String      id1              = "@\245Z@_CCF";
    String      id2              = "CCF\000";
    int         crc1Pos;         // file length - 2
    int         crc2Pos;         // duplicate of crc1pos
    int         year;            // last modified year
    int         month;           // last modified month
    int         day;             // last modified day of week (0-7)
    int         hour;            // last modified hour (24 hour)
    int         minute;          // last modified minute
    int         seconds;         // last modified second
    int         capability;      // capability bits
    int         attrPos;         // either 60 or 64. offset of attr.
    int         attr;            // see statics above (config, home RO)
    CCFDevice   firstHome;       // first home device in linked list
    CCFDevice   firstDevice;     // first normal device in linked list
    CCFDevice   firstMacro;      // first macro device in linked list
    CCFTimer    firstTimer;      // select remotes
    CCFPanel    macroPanel;      // default new macro editing panel
	int         channelID;       // tsu-6000 only
    int         _reserve_1;      // unknown, unused or _reserve_
    int         _reserve_2;      // unknown, unused or _reserve_
    int         _reserve_3;      // unknown, unused or _reserve_ (custom panel size)
    int         _reserve_4;      // 4 when attrPos=60 (ver1 <= 1)
    int         _reserve_5;      // unknown, unused or _reserve_
    int         _reserve_6;      // unknown, unused or _reserve_

	private CCFPanel eggDVD;     // for handling EggStream CCF's
    private String[][] detected;
	private CCFColor[] colorMap = CCFColor.defaultMap;

    CCFHeader()
    {
        clear();
    }

	CCFColor getTransparentColor()
	{
		return CCFColor.getColor(_reserve_2);
	}
	
	void setTransparentColor(CCFColor color)
	{
		_reserve_2 = color.getColorIndex();
	}

	boolean isTransparentColor(CCFColor color)
	{
		return color.getColorIndex() == _reserve_2;
	}

	void setScreenSize(int w, int h)
	{
		_reserve_3 = ((w & 0xffff) << 16) | (h & 0xffff);
	}

	Dimension getScreenSize(Dimension def)
	{
		int w = (_reserve_3 >>> 16) & 0xffff;
		int h = (_reserve_3) & 0xffff;
		if (w > 0 && h > 0)
		{
			return new Dimension(w,h);
		}
		return def;
	}

	CCFColor[] getColorMap()
	{
		return colorMap;
	}

	void setColorMap(CCFColor color[])
	{
		colorMap = color;
	}

    void clear()
    {
        version = "Tonto v"+Tonto.version();
		capability = 0x000000;
        attrPos = 64;
        firstHome = new CCFDevice(this);
        firstHome.name = "HOME";
        firstHome.firstPanel = new CCFPanel("Home", firstHome);
        firstDevice = null;
        firstMacro = null;
        firstTimer = null;
        macroPanel = new CCFPanel("macro", this);
        attr = AT_HOME_READONLY;
        detected = gray_timers;
    }

    void setFactoryCCF(boolean flag)
    {
        if (flag)
        {
            attr |= AT_FACTORY_CCF;
        }
        else
        {
            attr &= ~AT_FACTORY_CCF;
        }
    }

    void setConfigReadOnly(boolean flag)
    {
        if (flag)
        {
            attr |= AT_CONFIG_READONLY;
        }
        else
        {
            attr &= ~AT_CONFIG_READONLY;
        }
    }

    void setHomeReadOnly(boolean flag)
    {
        if (flag)
        {
            attr |= AT_HOME_READONLY;
        }
        else
        {
            attr &= ~AT_HOME_READONLY;
        }
    }

    boolean isFactoryCCF()
    {
        return (attr & AT_FACTORY_CCF) == AT_FACTORY_CCF;
    }

    boolean isConfigReadOnly()
    {
        return (attr & AT_CONFIG_READONLY) == AT_CONFIG_READONLY;
    }

    boolean isHomeReadOnly()
    {
        return (attr & AT_HOME_READONLY) == AT_HOME_READONLY;
    }

	void setEggDVD(CCFPanel eggDVDPanel)
	{
		eggDVD = eggDVDPanel;
	}

	CCFPanel getEggDVD()
	{
		return eggDVD;
	}

    // ---( override methods )---
    String describe()
    {
        return version+","+year+"-"+month+"-"+day+","+hour+":"+minute+":"+seconds;
    }

    // ---( utility methods )---
	boolean hasUDB()
	{
		return hasUDB(capability);
	}

	boolean hasTimers()
	{
		return hasTimers(capability);
	}

	boolean hasColor()
	{
		return hasColor(capability);
	}

	boolean isCustom()
	{
		return capability == CA_CUSTOM;
	}

	boolean isMarantz()
	{
		return isNewMarantz() || isOldMarantz();
	}

	boolean isNewMarantz()
	{
		return isNewMarantz(capability);
	}

	boolean isOldMarantz()
	{
		return isOldMarantz(capability);
	}

	static boolean hasTimers(int cap)
	{
		return (cap & CA_HAS_TIMERS) == CA_HAS_TIMERS;
	}

	static boolean hasUDB(int cap)
	{
		return (cap & CA_HAS_UDB) == CA_HAS_UDB || hasCustom(cap);
	}

	static boolean hasColor(int cap)
	{
		return (cap & CA_HAS_COLOR) == CA_HAS_COLOR;
	}

	static boolean hasCustom(int cap)
	{
		return (cap & CA_CUSTOM) == CA_CUSTOM;
	}

	static boolean isOldMarantz(int cap)
	{
		return cap == 0x1;
	}

	static boolean isNewMarantz(int cap)
	{
		return (cap & CA_IS_NEW_MARANTZ) == CA_IS_NEW_MARANTZ;
	}

    void setCapability(int cap)
    {
		capability = cap;
		attr = attr & AT_MASK;
		if (isNewMarantz(cap))
		{
			capability = capability & 0xffff;
		}
		if (hasColor())
		{
			detected = color_timers;
			attrPos = 64;
			_reserve_4 = 0;
		}
		else
		{
			detected = gray_timers;
			attrPos = 64;
			_reserve_4 = 0;
		}
        log(2, "set capability cap="+capability+" ap="+attrPos+" v4="+_reserve_4);
    }

    // ---( abstract methods )---
	void checkVersion()
	{
	}

    void preEncode(CCFNodeState zs)
    {
		if (!isCustom())
		{
			macroPanel.next = null;
		}
	}

    void preDecode(CCFNodeState zs)
    {
        // pre-detect ccf version
        BufferedFile buf = zs.buffer();
        long op = buf.getFilePointer();
        buf.seek(36);
		capability = buf.getInt();
        int crc = buf.getInt();
        attrPos = buf.getInt();
		if (hasColor())
		{
			if (attrPos == 64)
			{
				detected = color_timers;
			}
			else
			{
				debug.log(2, "header claims timers but attr pos mismatch @ "+attrPos);
				detected = color_notimers;
			}
		}
		else
		{
			if (attrPos == 64)
			{
				detected = gray_timers;
			}
			else
			{
				debug.log(2, "header claims timers but attr pos mismatch @ "+attrPos);
				detected = gray_notimers;
			}
		}
        buf.seek(op);
    }

    void postDecode(CCFNodeState zs)
    {
		printSummary();
        if (crc1Pos != crc2Pos)
        {
            log(1, "ccf: crc position mismatch "+hex(crc1Pos)+" != "+hex(crc2Pos), zs);
        }
		if (macroPanel == null)
		{
			macroPanel = new CCFPanel("macro", this);
		}
		setCapability(capability);
    }

	void printSummary()
	{
		ProntoModel m[] = ProntoModel.getModelByCapability(capability);
		if (m.length > 0)
		{
			StringBuffer sb = new StringBuffer("header type = ");
			for (int i=0; i<m.length; i++)
			{
				if (i > 0)
				{
					sb.append(", ");
				}
				sb.append(m[i].getName());
			}
			log(1, sb.toString());
		}
		else
		{
			log(1, "header"+
				" capa = "+get32Bits(capability)+
				" ("+Util.toHex(capability)+")");
			log(1, "header"+
				" timer="+hasTimers(capability)+
				" color="+hasColor(capability)+
				" attrpos="+attrPos+
				" end="+_reserve_4);
		}
        log(2, "header"+
            " attr = "+get32Bits(attr)+" ("+Util.toHex(attr)+")");
	}

	String summary()
	{
		return get32Bits(capability)+" "+get32Bits(attr)+" "+get16Bits(_reserve_4);
	}

    String[][] getEncodeTable()
    {
        return detected;
    }

    String[][] getDecodeTable()
    {
        return detected;
    }    

	void buildTree(CCFNode parent)
	{
		buildDeviceTree(firstHome);
		buildDeviceTree(firstDevice);
		buildDeviceTree(firstMacro);
		CCFPanel mp = macroPanel;
		while (mp != null)
		{
			mp.buildTree(this);
			mp = mp.next;
		}
	}

	private void buildDeviceTree(CCFDevice dev)
	{
		while (dev != null)
		{
			dev.buildTree(this);
			dev = dev.getNextDevice();
		}
	}

    // ---( instance methods )---
	void insertBefore(CCFDevice dev, CCFDevice newdev)
	{
		if (firstHome == dev)
		{
			newdev.buildTree(this);
			newdev.next = dev;
			firstHome = newdev;
			return;
		}
		if (firstDevice == dev)
		{
			newdev.buildTree(this);
			newdev.next = dev;
			firstDevice = newdev;
			return;
		}
		if (firstMacro == dev)
		{
			newdev.buildTree(this);
			newdev.next = dev;
			firstMacro = newdev;
			return;
		}
		if (checkInsertBefore(firstHome, dev, newdev))
		{
			return;
		}
		if (checkInsertBefore(firstDevice, dev, newdev))
		{
			return;
		}
		if (checkInsertBefore(firstMacro, dev, newdev))
		{
			return;
		}
	}

	boolean checkInsertBefore(CCFDevice root, CCFDevice dev, CCFDevice newdev)
	{
		if (root == null)
		{
			return false;
		}
		while (root.next != dev)
		{
			if (root.next == null)
			{
				return false;
			}
			root = root.next;
		}
		newdev.buildTree(this);
		newdev.next = dev;
		root.next= newdev;
		return true;
	}

    // returns new root if any
    CCFDevice delete(CCFDevice dev)
    {
        if (firstHome == dev)
        {
            firstHome = dev.next;
			dev.next = null;
            return firstHome;
        }
        else
        if (firstDevice == dev)
        {
            firstDevice = dev.next;
			dev.next = null;
            return firstDevice;
        }
        else
        if (firstMacro == dev)
        {
            firstMacro = dev.next;
			dev.next = null;
            return firstMacro;
        }
        else
        if (checkDelete(firstHome, dev))
        {
            return firstHome;
        }
        else
        if (checkDelete(firstDevice, dev))
        {
            return firstDevice;
        }
        else
        if (checkDelete(firstMacro, dev))
        {
            return firstMacro;
        }
        return null;
    }

    CCFDevice createDevice()
    {
        return new CCFDevice(this);
    }

    private boolean checkDelete(CCFDevice root, CCFDevice dev)
    {
        if (root == null || dev == null)
        {
            return false;
        }
        while (root != null && root.next != dev)
        {
            root = root.next;
        }
        if (root != null && root.next == dev)
        {
            root.next = dev.next;
			dev.next = null;
            return true;
        }
        return false;
    }

	boolean isHomeDevice(CCFDevice dev)
	{
		return inDeviceList(firstHome, dev);
	}

	boolean isNormalDevice(CCFDevice dev)
	{
		return inDeviceList(firstDevice, dev);
	}

	boolean isMacroDevice(CCFDevice dev)
	{
		return inDeviceList(firstMacro, dev);
	}

	CCFPanel masterTemplate()
	{
		return macroPanel;
	}

	CCFPanel deviceTemplate()
	{
		return macroPanel.next;
	}

	CCFPanel macroTemplate()
	{
		return (macroPanel.next == null ? null : macroPanel.next.next);
	}

	private boolean inDeviceList(CCFDevice root, CCFDevice srch)
	{
		if (root == null || srch == null)
		{
			return false;
		}
		while (root != null && root != srch)
		{
			root = root.next;
		}
		return (root == srch);
	}

    // ---( static methods )---
}


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

class CCFHeaderAttr extends CCFNode
{
    // ---( static fields )---
	// attribute bits
    final static int AT_CONFIG_READONLY = (1 << 1);
    final static int AT_HOME_READONLY   = (1 << 2);
    final static int AT_MASK            = AT_CONFIG_READONLY | AT_HOME_READONLY;

    final static String gray_notimers[][] =
        {
            { "N4", "attr" },
            { "N4", "_reserve_4" },
            { "Z*", "macroPanel" },
        };

    final static String gray_timers[][] =
        {
            { "N4", "attr" },
            { "Z*", "macroPanel" },
            { "N2", "_reserve_4" },
        };

    final static String color_notimers[][] =
        {
            { "N4", "attr" },
            { "Z*", "macroPanel" },
            { "N1", "channelID" },
        };

    final static String color_timers[][] =
        {
            { "N4", "attr" },
            { "Z*", "macroPanel" },
            { "N4", "channelID" },
        };

    // ---( instance fields )---
    int         attr;            // see statics above (config, home RO)
    CCFPanel    macroPanel;      // default new macro editing panel
	int         channelID;       // tsu-6000 only
    int         _reserve_1;       // unknown, unused or _reserve_
    int         _reserve_2;       // unknown, unused or _reserve_

    private String[][] detected;
	private CCFColor[] colorMap = CCFColor.defaultMap;

    CCFHeaderAttr()
    {
        clear();
    }

    void clear()
    {
        macroPanel = null;
        attr = AT_CONFIG_READONLY;
        detected = gray_timers;
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

    boolean isConfigReadOnly()
    {
        return (attr & AT_CONFIG_READONLY) == AT_CONFIG_READONLY;
    }

    boolean isHomeReadOnly()
    {
        return (attr & AT_HOME_READONLY) == AT_HOME_READONLY;
    }

    // ---( override methods )---
	/*
    String describe()
    {
    }
	*/

    // ---( utility methods )---

    // ---( abstract methods )---
	void checkVersion()
	{
	}

    void preEncode(CCFNodeState zs)
    {
	}

    void preDecode(CCFNodeState zs)
    {
    }

    void postDecode(CCFNodeState zs)
    {
    }

	void buildTree(CCFNode parent)
	{
	}

    String[][] getEncodeTable()
    {
        return detected;
    }

    String[][] getDecodeTable()
    {
        return detected;
    }    

    // ---( instance methods )---

    // ---( static methods )---
}


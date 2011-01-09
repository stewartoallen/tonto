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
import javax.swing.*;
import com.neuron.app.tonto.ui.*;

public class RenderGrid
{
	public int offX;
	public int offY;
	public int spaceX;
	public int spaceY;
	public Color major;
	public Color minor;
	public int minorTicks;

	RenderGrid(int ox, int oy, int sx, int sy, Color ma, Color mi, int mt)
	{
		offX = ox;
		offY = oy;
		spaceX = sx;
		spaceY = sy;
		major = ma;
		minor = mi;
		minorTicks = mt;
	}
}


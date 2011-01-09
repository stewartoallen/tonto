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

public class Renderer
{
	private final static Debug debug = Debug.getInstance("render");
	private final static JPanel panel = new JPanel();
	private static ImageFont fonts[];

	static
	{
		Class c = Renderer.class;
		try
		{
			fonts = new ImageFont[] {
				new ImageFont(c.getResourceAsStream("/font/tonto-08.iff")),
				new ImageFont(c.getResourceAsStream("/font/tonto-10.iff")),
				new ImageFont(c.getResourceAsStream("/font/tonto-12.iff")),
				new ImageFont(c.getResourceAsStream("/font/tonto-14.iff")),
				new ImageFont(c.getResourceAsStream("/font/tonto-16.iff")),
				new ImageFont(c.getResourceAsStream("/font/tonto-18.iff")),
			};
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	private ICCFProvider ccf;

	public Renderer(ICCFProvider ccf)
	{
		this.ccf = ccf;
	}

	public void draw(Graphics g, CCFPanel panel, int x, int y)
	{
		if (panel == null)
		{
			return;
		}
		CCFPanel template = panel.getTemplate();
		if (template != null && ccf.ccf().getModel().isCustom())
		{
			draw(g, panel.getTemplate(), x, y);
		}
		else
		{
			g.setColor(Color.white);
			Dimension d = ccf.ccf().getScreenSize();
			if (d == null)
			{
				Rectangle r = g.getClipBounds();
				g.fillRect(r.x, r.y, r.width, r.height);
			}
			else
			{
				g.fillRect(0, 0, d.width, d.height);
			}
		}
		draw(g, panel.getChildren(), x, y);
	}

	public void draw(Graphics g, CCFChild child[], int x, int y)
	{
		if (child == null || child.length == 0)
		{
			return;
		}
		for (int i=0; i<child.length; i++)
		{
			draw(g, child[i], x, y);
		}
	}
	
	public void draw(Graphics g, CCFChild child, int x, int y)
	{
		if (child == null)
		{
			return;
		}
		if (!g.getClipBounds().intersects(getBounds(child)))
		{
			return;
		}

		Point pos = child.getLocation();
		switch (child.getType())
		{
			case CCFChild.BUTTON:
				draw(g, child.getButton(), pos.x + x, pos.y + y);
				break;
			case CCFChild.FRAME:
				draw(g, child.getFrame(), pos.x + x, pos.y + y);
				break;
		}
	}
	
	public void draw(Graphics g, CCFButton button, int x, int y)
	{
		if (button == null)
		{
			return;
		}
		Dimension sz = button.getSize();
		clear(g, button.getBackground(), x, y, sz.width, sz.height);
		draw(g, button.getIconSet().getDefaultIcon(), x, y, sz.width, sz.height);
		draw(g, button.getName(), button.getFont(), button.getForeground(), x, y, sz.width, sz.height);
	}
	
	public void draw(Graphics g, CCFFrame frame, int x, int y)
	{
		if (frame == null)
		{
			return;
		}
		Dimension sz = frame.getSize();
		clear(g, frame.getBackground(), x, y, sz.width, sz.height);
		draw(g, frame.getIcon(), x, y, sz.width, sz.height);
		draw(g, frame.getName(), frame.getFont(), frame.getForeground(), x, y, sz.width, sz.height);
		draw(g, frame.getChildren(), x, y);
	}

	public void clear(Graphics g, CCFColor color, int x, int y, int w, int h)
	{
		if (ccf.ccf().isTransparentColor(color))
		{
			return;
		}
		g.setColor(color.getAWTColor(ccf.ccf().isColor()));
		g.fillRect(x,y,w,h);
	}

	public void draw(Graphics g, String label, CCFFont font, CCFColor color, int x, int y, int w, int h)
	{
		if (label == null || font == null || font == CCFFont.NONE)
		{
			return;
		}
		g.setColor(color.getAWTColor(ccf.ccf().isColor()));
		ImageFont fl = fonts[font.getFontSize()-1];
		fl.draw(label, x+(w-fl.getStringWidth(label))/2, y+(h-fl.getHeight())/2, g);
	}

	public void draw(Graphics g, CCFIcon icon, int x, int y, int w, int h)
	{
		if (icon == null)
		{
			return;
		}
		Image i = icon.getImage(panel);
		g.drawImage(i, x, y, panel);
	}

	// --( static methods )--

	public static CCFChild getClick(CCFPanel p, Point click)
	{
		return getClick(p.getChildren(), new Point(0,0), click);
	}

	public static CCFChild getClick(CCFChild c[], Point off, Point click)
	{
		if (c == null || c.length == 0)
		{
			return null;
		}
		for (int i=c.length-1; i>=0; i--)
		{
			Rectangle bounds = c[i].getBounds();		
			bounds.translate(off.x, off.y);
			if (bounds.contains(click))
			{
				if (c[i].isButton())
				{
					return c[i];
				}
				else
				{
					off.x = bounds.x;
					off.y = bounds.y;
					CCFChild cl = getClick(c[i].getFrame().getChildren(), off, click);
					return cl != null ? cl : c[i];
				}
			}
		}
		return null;
	}

	public static Rectangle getBounds(CCFChild c)
	{
		Point p = getLocation(c, new Point(0,0));
		Rectangle r = c.getBounds();
		r.x = p.x;
		r.y = p.y;
		return r;
	}

	private static Point getLocation(CCFNode node, Point p)
	{
		if (node == null || node instanceof CCFPanel)
		{
			return p;
		}
		if (node instanceof CCFChild)
		{
			Point np = ((CCFChild)node).getLocation();
			p.translate(np.x, np.y);
		}
		return getLocation(node.getParent(), p);
	}
}


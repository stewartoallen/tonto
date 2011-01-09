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
import java.awt.event.*;

public class RenderPanel
	extends JPanel
	implements MouseListener, MouseMotionListener, KeyListener
{
	private final Debug debug = Debug.getInstance("render");

	private ICCFEditor editor;
	private Renderer render;
	private CCFPanel panel;
	private Hashtable select;
	private Point last;
	private int scale;
	private RenderGrid grid;
	private boolean useGrid;
	private boolean useSnap;

	public RenderPanel(ICCFEditor editor, Renderer render, CCFPanel panel)
	{
		this.editor = editor;
		this.render = render;
		this.panel = panel;
		this.select = new Hashtable();
		this.scale = 1;

		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
	}

	// --( mouselistener )--
	public void mousePressed(MouseEvent me)
	{
		requestFocus();
		Point pt = me.getPoint();
		pt.x /= scale;
		pt.y /= scale;
		last = pt;
		CCFChild target = render.getClick(panel, pt);
		if (target != null && select.containsKey(target))
		{
			return;
		}
		if (!me.isShiftDown())
		{
			clearSelection();
		}
		if (target != null)
		{
			select.put(target, me.getPoint());
			repaint(target);
			editor.setSelection(this, select.keys());
		}
	}

	public void mouseReleased(MouseEvent me)
	{
	}

	public void mouseClicked(MouseEvent me)
	{
	}

	public void mouseEntered(MouseEvent me)
	{
	}

	public void mouseExited(MouseEvent me)
	{
	}

	// --( mousemotionlistener )--
	public void mouseDragged(MouseEvent me)
	{
		if (select.size() == 0)
		{
			return;
		}
		Rectangle rect = getBounds(select.keys());
		Point pt = me.getPoint();
		pt.x /= scale;
		pt.y /= scale;
		for (Enumeration e = select.keys(); e.hasMoreElements(); )
		{
			CCFChild c = (CCFChild)e.nextElement();
			Point pos = c.getLocation();
			pos.translate(pt.x-last.x, pt.y-last.y);
			/*
			if (useSnap && grid != null)
			{
				int modX = (pos.x-grid.offX)%grid.spaceX;// ((pos.x+grid.offX)-(grid.spaceX/2))%grid.spaceX+(grid.spaceX/2);
				int modY = (pos.y-grid.offY)%grid.spaceY;// ((pos.y+grid.offY)-(grid.spaceY/2))%grid.spaceY+(grid.spaceY/2);
				debug.log(0, "pos was : "+pos);
				pos.x -= modX;
				pos.y -= modY;
				debug.log(0, "pos is  : "+pos);
			}
			*/
			c.setLocation(pos);
		}
		last = pt;
		repaint(rect.union(getBounds(select.keys())));
	}

	public void mouseMoved(MouseEvent me)
	{
	}

	// --( keylistener )--
	public void keyPressed(KeyEvent ke)
	{
	}

	public void keyReleased(KeyEvent ke)
	{
		boolean alt = ke.isAltDown();
		boolean ctrl = ke.isControlDown();
		boolean shift = ke.isShiftDown();
		switch (ke.getKeyCode())
		{
			case KeyEvent.VK_C: if (ctrl) { copy();  } break;
			case KeyEvent.VK_X: if (ctrl) { cut();   } break;
			case KeyEvent.VK_V: if (ctrl) { paste(); } break;
		}
	}

	public void keyTyped(KeyEvent ke)
	{
	}

	// --( instance methods )--
	public int getScale()
	{
		return scale;
	}

	public void setScale(int sc)
	{
		this.scale = sc;
		revalidate();
	}

	public Dimension scale(Dimension d)
	{
		return new Dimension(d.width*scale, d.height*scale);
	}

	public Rectangle scale(Rectangle r)
	{
		return new Rectangle(r.x*scale, r.y*scale, r.width*scale, r.height*scale);
	}

	public Dimension getPreferredSize()
	{
		return scale(editor.ccf().getScreenSize());
	}

	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	}

	public Dimension getMaximumSize()
	{
		return getPreferredSize();
	}

	private void repaint(CCFChild c)
	{
		repaint(scale(render.getBounds(c)));
	}

	private Rectangle getBounds(Enumeration e)
	{
		Rectangle r = null;
		while (e.hasMoreElements())
		{
			Rectangle nr = scale(render.getBounds((CCFChild)e.nextElement()));
			if (r == null)
			{
				r = nr;
			}
			else
			{
				r = r.union(nr);
			}
		}
		return r;
	}

	public void repaint(Rectangle r)
	{
		int v = scale - 1;
		r.x -= v;
		r.y -= v;
		r.width += v*2;
		r.height += v*2;
		super.repaint(r);
	}

	public void paint(Graphics g)
	{
		Dimension d = getSize();
		((Graphics2D)g).scale((double)scale, (double)scale);
		render.draw(g, panel, 0, 0);
		if (useGrid && grid != null)
		{
			((Graphics2D)g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
			int c = 0;
			for (int x=grid.offX; x<d.width; x += grid.spaceX)
			{
				g.setColor(c++ % grid.minorTicks == 0 ? grid.major : grid.minor);
				g.drawLine(x,0,x,d.height);
			}
			c = 0;
			for (int y=grid.offY; y<d.height; y += grid.spaceY)
			{
				g.setColor(c++ % grid.minorTicks == 0 ? grid.major : grid.minor);
				g.drawLine(0,y,d.width,y);
			}
			((Graphics2D)g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.00f));
		}
		for (Enumeration e = select.keys(); e.hasMoreElements(); )
		{
			CCFChild c = (CCFChild)e.nextElement();
			Rectangle r = render.getBounds(c);
			g.setColor(Color.red);
			g.drawRect(r.x, r.y, r.width-1, r.height-1);
		}
	}

	private CCFNode[] enumToNodes(Enumeration e)
	{
		Vector v = new Vector();
		while (e.hasMoreElements()) { v.addElement(e.nextElement()); }
		CCFNode n[] = new CCFNode[v.size()];
		v.copyInto(n);
		return n;
	}

	// --( selection calls )--
	public void clearSelection()
	{
		Enumeration e = ((Hashtable)select.clone()).keys();
		select.clear();
		while (e.hasMoreElements())
		{
			repaint((CCFChild)e.nextElement());
		}
	}

	public void setGrid(RenderGrid grid)
	{
		this.grid = grid;
	}

	public void showGrid(boolean b)
	{
		useGrid = b;
		revalidate();
	}

	public void snapGrid(boolean b)
	{
		useSnap = b;
	}

	public void cut()
	{
		debug.log(0, "CUT");
		editor.setClipboard(enumToNodes(select.keys()));
	}

	public void copy()
	{
		debug.log(0, "COPY");
		editor.setClipboard(enumToNodes(select.keys()));
	}

	public void paste()
	{
		debug.log(0, "PASTE");
		CCFNode n[] = editor.getClipboard();
		for (int i=0; n != null && i<n.length; i++)
		{
			debug.log(0, "PASTE: "+n[i]);
		}
	}

	public void valign() { }

	public void halign() { }

	public void vdist() { }

	public void hdist() { }

	public void group() { }

	public void ungroup() { }

	public void lock() { }

	public void unlock() { }

}


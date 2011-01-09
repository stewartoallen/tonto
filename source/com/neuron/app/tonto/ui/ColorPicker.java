/*
 * Copyright 2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto.ui;

// ---( imports )---
import com.neuron.app.tonto.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class ColorPicker extends JPanel
{
	// ---( static fields )---
	private final static Dimension size = new Dimension(20,20);

	// ---( static methods )---
	public static void main(String args[])
	{
		Frame f = new Frame("foo");
		f.setBackground(Color.white);
		ColorPicker cp1 = new ColorPicker(f);
		ColorPicker cp2 = new ColorPicker(f);
		f.setLayout(new GridLayout(2,1,5,5));
		f.add(cp1);
		f.add(cp2);
		cp1.setColor(CCFColor.getColor(0), true);
		cp2.setColor(CCFColor.getColor(0), false);
		f.pack();
		f.show();
	}

	// ---( constructors )---
	public ColorPicker ()
	{
		this(null, true);
	}

	public ColorPicker (CCFColor color)
	{
		this(color, true, true, true);
	}

	public ColorPicker (CCFColor color, boolean ext, boolean edit)
	{
		this(color, ext, edit, true);
	}

	public ColorPicker (CCFColor color, boolean ext, boolean edit, boolean websafe)
	{
		this(null, true);
		setColor(color, ext);
		setEditable(edit);
		setWebSafe(websafe);
	}

	public ColorPicker (Window owner)
	{
		this(null, true);
	}

	public ColorPicker (Window owner, boolean websafe)
	{
		setBorder(new BevelBorder(BevelBorder.RAISED));
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent me) {
				if (edit) {
					chooseColor();
				}
			}
		});
		window = owner;
		setEditable(true);
		setWebSafe(websafe);
	}

	// ---( instance fields )---
	private CCFColor color;
	private boolean ext;
	private boolean edit;
	private boolean websafe;
	private Window window;

	// ---( instance methods )---
	public Dimension getMinimumSize()
	{
		return size;
	}

	public Dimension getPreferredSize()
	{
		return size;
	}

	public void setWebSafe(boolean websafe)
	{
		this.websafe = websafe;
	}

	public void setEditable(boolean edit)
	{
		this.edit = edit;
	}

	public void setColor(CCFColor color, boolean ext)
	{
		this.color = color;
		this.ext = ext;
		repaintOwner();
	}

	private void repaintOwner()
	{
		Component c = this;
		while (!(c instanceof Window || c == null))
		{
			c = c.getParent();
		}
		if (c != null)
		{
			c.repaint();
		}
	}

	public void paint(Graphics g)
	{
		setBackground(color.getAWTColor(ext));
		super.paint(g);
	}

	public void setOwner(Window owner)
	{
		window = owner;
	}

	public CCFColor getColor()
	{
		return color;
	}

	// ---( interface methods )---
	public void chooseColor()
	{
		Chooser ch = ext ? (Chooser)(new ColorChooser()) : (Chooser)(new GrayChooser());
		ch.select(getPostLocation(), color.getColorIndex());
	}

	private Point getPostLocation()
	{
		Dimension sz = getSize();
		Point p = getLocationOnScreen();
		return new Point(p.x + sz.width/2, p.y + sz.height/2);
	}

	private Window findOwner()
	{
		Component c = this;
		while (!(c instanceof Window || c == null))
		{
			c = c.getParent();
		}
		if (c != null)
		{
			return (Window)c;
		}
		else
		{
			return new Frame();
		}
	}

	class Chooser extends ModalWindow
	{
		private MouseListener adapter = new MouseAdapter() {
			public void mouseClicked(MouseEvent me) {
				clicked(me.getSource());
			}
		};

		private int value;
		private JPanel content;

		Chooser()
		{
			super(window != null ? window : findOwner());
			content = new JPanel();
			content.setBackground(Color.white);
			setLayout(new GridLayout(1,1));
			add(content);
		}

		int getValue()
		{
			return value;
		}

		void addBar(Bar bar)
		{
			content.add(bar);
			bar.addMouseListener(adapter);
		}

		JPanel getContent()
		{
			return content;
		}

		void clicked(Object o)
		{
			if (o instanceof Bar)
			{
				value = ((Bar)o).value;
			}
			dispose();
			setColor(CCFColor.getColor(value), ext);
		}

		void select(Point pos, int value)
		{
			setLocation(pos);
			this.value = value;
			show();
		}
	}

	class GrayChooser extends Chooser
	{
		GrayChooser()
		{
			getContent().setLayout(new GridLayout(1,4));
			Color c[] = CCFColor.getColorModel(false);
			for (int i=0; i<c.length; i++)
			{
				addBar(new Bar(50, c[i], i));
			}
			pack();
		}
	}

	class ColorChooser extends Chooser
	{
		ColorChooser()
		{
			getContent().setLayout(websafe ? new GridLayout(16,16) : new GridLayout(12,18));
			Color c[] = CCFColor.getColorModel(true);
			for (int i=0; i<Math.min(c.length,websafe ? 256 : 216); i++)
			{
				addBar(new Bar(20, c[i], i));
			}
			pack();
		}
	}

	class Bar extends JPanel
	{
		private int value;
		private Dimension size;

		Bar(int size, Color color, int value)
		{
			setBorder(new LineBorder(Color.black,2));
			setBackground(color);
			this.value = value;
			this.size = new Dimension(size,size);
		}

		public Dimension getMinimumSize()
		{
			return size;
		}

		public Dimension getPreferredSize()
		{
			return size;
		}
	}
}


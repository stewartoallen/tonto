/*
 * Copyright 2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto.ui;

// ---( imports )---
import java.io.*;
import java.awt.*;
import java.awt.font.*;
import java.awt.event.*;
import javax.swing.*;

public class TLabel extends JComponent
{
	// ---( static fields )---
	public final static int ANCHOR_NW     = 1;
	public final static int ANCHOR_N      = 2;
	public final static int ANCHOR_NE     = 3;
	public final static int ANCHOR_E      = 4;
	public final static int ANCHOR_SE     = 5;
	public final static int ANCHOR_S      = 6;
	public final static int ANCHOR_SW     = 7;
	public final static int ANCHOR_W      = 8;
	public final static int ANCHOR_CENTER = 9;

	private final static int[] map = new int[256];
	private final static RenderingHints hints = new RenderingHints(new java.util.Hashtable());

	private final static void addMap(int start, int len, int mapTo)
	{
		for (int i=0; i<len; i++)
		{
			map[i+start] = i+mapTo;
		}
	}

	static
	{
		hints.put(
			RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		hints.put(
			RenderingHints.KEY_FRACTIONALMETRICS,
			RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		addMap(32, 96, 3);
	}

	// ---( static methods )---
	public static void main(String args[])
		throws Exception
	{
		Font font = Font.createFont(Font.TRUETYPE_FONT,
			new FileInputStream("/tmp/pronto.ttf"));

		JFrame frame = new JFrame("font test");
		TLabel label = new TLabel("!! Tonto Font Test \125 !!", ANCHOR_CENTER);
		label.setFont(font.deriveFont(18.0f));
		frame.getContentPane().add("Center", label);
		frame.pack();
		frame.show();
	}

	// ---( constructors )---
	public TLabel (String text)
	{
		this(text,ANCHOR_CENTER);
	}

	public TLabel (String text, int anchor)
	{
		setText(text);
		setAnchor(anchor);
	}

	public void setText(String text)
	{
		this.text = text;
		this.ctext = new int[text.length()];
		for (int i=0; i<ctext.length; i++)
		{
			ctext[i] = map[text.charAt(i)&0xff];
			System.out.println("map ("+text.charAt(i)+") to ("+ctext[i]+")");
		}
	}

	public void setAnchor(int anchor)
	{
		this.anchor = anchor;
	}

	// ---( instance fields )---
	private int anchor;
	private int[] ctext;
	private String text;
	private Dimension size;
	private GlyphVector gv;

	// ---( instance methods )---
	public void paint(Graphics g)
	{
		if (size == null)
		{
			generateGlyph();
		}
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHints(hints);
		Dimension d = getSize();
		float x = 0f;
		float y = 0f;
		switch (anchor)
		{
			case ANCHOR_NW:
				g2.drawGlyphVector(gv, 0f, (float)size.height);
				break;
			case ANCHOR_N: 
				g2.drawGlyphVector(gv, (float)(d.width-size.width)/2f, (float)size.height);
				break;
			case ANCHOR_NE:
				g2.drawGlyphVector(gv, (float)(d.width-size.width), (float)size.height);
				break;
			case ANCHOR_E: 
				g2.drawGlyphVector(gv, (float)(d.width-size.width), (float)(d.height+size.height)/2f);
				break;
			case ANCHOR_SE:
				g2.drawGlyphVector(gv, (float)(d.width-size.width), (float)(d.height+size.height));
				break;
			case ANCHOR_S: 
				g2.drawGlyphVector(gv, (float)(d.width-size.width)/2f, (float)(d.height+size.height));
				break;
			case ANCHOR_SW:
				g2.drawGlyphVector(gv, 0f, (float)d.height);
				break;
			case ANCHOR_W: 
				g2.drawGlyphVector(gv, 0f, (float)(d.height+size.height)/2f);
				break;
			case ANCHOR_CENTER:
			default:
				g2.drawGlyphVector(gv, (float)(d.width-size.width)/2f, (float)(d.height+size.height)/2f);
				break;
		}
	}

	private void generateGlyph()
	{
		Graphics2D g2 = (Graphics2D)getGraphics();
		g2.setRenderingHints(hints);
		Font f = g2.getFont();
		FontRenderContext rc = g2.getFontRenderContext();
		gv = f.createGlyphVector(rc, ctext);
		Rectangle rect = gv.getVisualBounds().getBounds();
		size = new Dimension(rect.width, rect.height);
	}

	public Dimension getMinimumSize()
	{
		if (size == null)
		{
			generateGlyph();
		}
		return size;
	}

	public Dimension getPreferredSize()
	{
		return getMinimumSize();
	}

	public Dimension getMaximumSize()
	{
		return getMinimumSize();
	}

	// ---( interface methods )---
}


/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto.ui;

// ---( imports )---
import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;

public class Snarfont
{
	// ---( static fields )---

	// ---( static methods )---
	public static void main(String args[])
		throws Exception
	{
//		new Snarfont(args[0]).export(Integer.parseInt(args[1]));
		new Snarfont(args[0]).export(10, "tonto-08.iff");
		new Snarfont(args[0]).export(13, "tonto-10.iff");
		new Snarfont(args[0]).export(16, "tonto-12.iff");
		new Snarfont(args[0]).export(18, "tonto-14.iff");
		new Snarfont(args[0]).export(21, "tonto-16.iff");
		new Snarfont(args[0]).export(24, "tonto-18.iff");
	}

	// ---( constructors )---
	public Snarfont (String fontfile)
		throws Exception
	{
		font = Font.createFont(
			Font.TRUETYPE_FONT, new FileInputStream(fontfile));
	}

	// ---( instance fields )---
	private Font font;
	private Font scaledfont;
	private FontMetrics metrics;
	private FontChar chars[];
	private int ascent;
	private int descent;
	private int height;

	// ---( instance methods )---
	private void export(int fsize, String fname)
		throws IOException
	{
		scaledfont = font.deriveFont((float)fsize);
		JFrame frame = new JFrame("snarf");
		metrics = frame.getToolkit().getFontMetrics(scaledfont);
		ascent = metrics.getMaxAscent();
		descent = metrics.getMaxDescent();
		height = ascent + descent;
		Container cont = frame.getContentPane();
		cont.setLayout(new GridLayout(16,16));
		cont.setBackground(Color.white);
		chars = new FontChar[256];
		for (int i=0; i<256; i++)
		{
			chars[i] = new FontChar(i);
			cont.add(chars[i]);
		}
		frame.pack();
		frame.show();

		ImageFont m = new ImageFont();
		m.setHeight(height);
		for (int i=0; i<256; i++)
		{
			m.setChar(i, chars[i].width, chars[i].getPixels());
		}
		m.writeFont(new FileOutputStream(fname));

		frame.dispose();

		m = new ImageFont();
		m.readFont(new FileInputStream(fname));

		JFrame f = new JFrame("test");
		f.setContentPane(new ImageFontLabel(m, "This is a test of my ABC/123's!"));
		f.setSize(300,100);
		f.show();
	}

	// ---( interface methods )---
	private class FontChar extends JComponent
	{
		int ch;
		int width;
		Dimension dim;
		Image buf;

		FontChar(int ch)
		{
			this.ch = ch;
			this.width = Math.max(1, metrics.charWidth(ch));
			this.dim = new Dimension(width, height);
		}

		public Dimension getMinimumSize()   { return dim; }
		public Dimension getPreferredSize() { return dim; }
		public Dimension getMaximimSize()   { return dim; }

		public void paint(Graphics g)
		{
			if (buf == null)
			{
				synchronized (this)
				{
					//System.out.println(ch+" -> "+width+" x "+height);
					buf = createImage(width, height);
					Graphics bg = buf.getGraphics();
					bg.setFont(scaledfont);
					bg.setColor(Color.black);
					bg.drawChars(new char[] { (char)ch}, 0, 1, 0, ascent);
					notify();
				}
			}
			g.drawImage(buf, 0, 0, this);
		}

		public int[] getPixels()
		{
			synchronized (this)
			{
				if (buf == null)
				{
					try
					{
						wait();
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
			}
			int pix[] = new int[width*height];
			PixelGrabber px =
				new PixelGrabber(buf, 0, 0, width, height, pix, 0, width);
			try
			{
				if (px.grabPixels())
				{
					if ((px.getStatus() & ImageObserver.ABORT) != 0)
					{
						System.out.println("grab aborted!");
						System.exit(1);
					}
					return pix;
				}
				else
				{
					System.out.println("err grabbing pixels");
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			return null;
		}
	}

}


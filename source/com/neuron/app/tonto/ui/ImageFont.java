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
import java.util.*;
import java.awt.image.*;

public class ImageFont
{
	// ---( static fields )---
	private final static ColorModel cm = ColorModel.getRGBdefault();
	private final static int zero = (0xff << 16) | (0xff << 8) | (0xff);
	private final static int one  = (0xff << 24) | zero;

	private final static Component comp = new Panel();
	private final static MediaTracker mt = new MediaTracker(comp);

	// ---( static methods )---

	// ---( constructors )---
	public ImageFont ()
	{
	}

	public ImageFont (InputStream is)
		throws IOException
	{
		readFont(is);
	}

	public String toString()
	{
		return "font[h="+height+"]";
	}

	// ---( instance fields )---
	private int height = 1;
	private int width[] = new int[256];
	private int[][] data = new int[256][];
	private Hashtable cache = new Hashtable();

	// ---( instance methods )---
	public void setHeight(int h)
	{
		height = h;
	}

	public void setChar(int ch, int w, int d[])
	{
		if (ch < 0 || ch > 255)
		{
			throw new IllegalArgumentException();
		}
		width[ch] = w;
		data[ch] = d;
	}

	public void writeFont(OutputStream os)
		throws IOException
	{
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os));
		w.write("height "+height);
		w.newLine();
		w.newLine();
		for (int f=0; f<256; f++)
		{
			w.write("char "+f+" width "+width[f]);
			w.newLine();
			w.write("   ");
			for (int i=0; i<data[f].length; i++)
			{
				w.write(cm.getRed(data[f][i]) == 0 ? "1" : "-");
				if ((i+1)%width[f] == 0)
				{
					w.newLine();
					w.write("   ");
				}
			}
			w.newLine();
		}
		w.flush();
	}

	public void readFont(InputStream is)
		throws IOException
	{
		data = new int[256][];
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		// read height
		String line = r.readLine();
		height = Integer.parseInt(line.substring(7));
		for (int i=0; i<256; i++)
		{
			// skip empty line
			line = r.readLine();
			line = r.readLine();
			StringTokenizer st = new StringTokenizer(line," ");
			// skip 'char'
			st.nextToken();
			if (Integer.parseInt(st.nextToken()) != i)
			{
				throw new IOException("malformed font file @ "+i);
			}
			// skip 'width'
			st.nextToken();
			width[i] = Integer.parseInt(st.nextToken());
			int d[] = new int[width[i]*height];
			// TODO
			for (int j=0; j<height; j++)
			{
				line = r.readLine().trim();
				for (int k=0; k<width[i]; k++)
				{
					int pos = j*width[i]+k;
					if (line.charAt(k) == '-')
					{
						d[pos] = zero;
					}
					else
					{
						d[pos] = one;
					}
				}
			}
			data[i] = d;
		}
	}

	private Image getCharImage(int ch, int color)
	{
		color = color & 0xffffff;
		Integer key = new Integer( (ch << 24) | color );
		Image image = (Image)cache.get(key);
		if (image == null)
		{
			int odata[] = data[ch];
			int ndata[] = new int[odata.length];
			for (int i=0; i<ndata.length; i++)
			{
				ndata[i] = (odata[i] & 0xff000000) | color;
			}
			image = Toolkit.getDefaultToolkit().createImage(
				new MemoryImageSource(
					width[ch], height, cm, ndata, 0, width[ch]
				)
			);
			mt.addImage(image, ch);
			try
			{
				mt.waitForID(ch);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			cache.put(key, image);
		}
		return image;
	}

	public void draw(int ch, int x, int y, Graphics g, int color)
	{
		g.drawImage(getCharImage(ch, color), x, y, null);
	}

	public void draw(String str, int x, int y, Graphics g)
	{
		if (str == null)
		{
			return;
		}
		int color = g.getColor().getRGB();
		char ch[] = new char[str.length()];
		str.getChars(0, ch.length, ch, 0);
		for (int i=0; i<ch.length; i++)
		{
			draw(ch[i], x, y, g, color);
			x += (width[ch[i]]);
		}
	}

	public int getHeight()
	{
		return height;
	}

	public int getCharWidth(int ch)
	{
		return width[ch];
	}

	public int getStringWidth(String str)
	{
		if (str == null)
		{
			return 0;
		}
		int len = 0;
		char ch[] = new char[str.length()];
		str.getChars(0, ch.length, ch, 0);
		for (int i=0; i<ch.length; i++)
		{
			len += (width[ch[i]]);
		}
		return len;
	}

	// ---( interface methods )---

}


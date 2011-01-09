/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto.ui;

// ---( imports )---
import java.awt.*;
import java.util.*;
import javax.swing.*;
import com.neuron.app.tonto.CCFNode;

public class ImageFontLabel extends JLabel
{
	// ---( static fields )---

	// ---( static methods )---

	// ---( constructors )---
	public ImageFontLabel (ImageFont font, String str)
	{
		this(font, str, JLabel.CENTER);
	}

	public ImageFontLabel (ImageFont font, String str, int align)
	{
		super(str, align);
		setFont(font);
		setAlignment(CCFNode.TEXT_CENTER);
	}

	// ---( instance fields )---
	private ImageFont font;
	private Dimension dim;
	private int align;
	private String text[];
	private int width[];

	// ---( instance methods )---
	public void setText(String str)
	{
		super.setText(str);
		parseText();
	}

	public void setFont(ImageFont font)
	{
		this.font = font;
		if (font == null)
		{
			dim = new Dimension(0, 0);
			return;
		}
		if (getText() != null)
		{
			parseText();
		}
	}

	private void parseText()
	{
		StringTokenizer st = new StringTokenizer(getText(), "\n");
		text = new String[st.countTokens()];
		width = new int[text.length];
		int longest = 0;
		for (int i=0; st.hasMoreTokens(); i++)
		{
			text[i] = st.nextToken();
			if (text[i].length() > text[longest].length())
			{
				longest = i;
			}
			if (font != null)
			{
				width[i] = font.getStringWidth(text[i]);
			}
		}
		if (font != null && text.length > 0)
		{
			dim = new Dimension(width[longest], font.getHeight());
		}
	}

	public void setAlignment(int align)
	{
		switch(align)
		{
			case CCFNode.TEXT_LEFT: setHorizontalAlignment(LEFT); break;
			case CCFNode.TEXT_CENTER: setHorizontalAlignment(CENTER); break;
			case CCFNode.TEXT_RIGHT: setHorizontalAlignment(RIGHT); break;
		}
	}

	public void paint(Graphics g)
	{
		if (font == null)
		{
			return;
		}
		Dimension sz = getSize();
		int h = font.getHeight();
		int x = 0;
		int y = (sz.height-(h*text.length))/2;
		int a = getHorizontalAlignment();
		g.setColor(getForeground());
		for (int i=0; i<text.length; i++)
		{
			switch (a)
			{
				case LEFT:
					x = 0;
					font.draw(text[i], 0, y, g);
					break;
				case CENTER:
					font.draw(text[i], (sz.width - width[i])/2, y, g);
					break;
				case RIGHT:
					font.draw(text[i], sz.width - width[i], y, g);
					break;
			}
			y += h;
		}
	}

	public Dimension getMinimumSize()   { return dim; }
	public Dimension getPreferredSize() { return dim; }
	public Dimension getMaximumSize()   { return dim; }

	// ---( interface methods )---
}


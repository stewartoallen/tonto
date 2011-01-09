/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto.ui;

// ---( imports )---
import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;
import java.util.Stack;
import java.util.Hashtable;
import java.util.StringTokenizer;

public class AAPanel extends JPanel
{
	// ---( static fields )---

	// ---( static methods )---
	public static void main(String args[])
	{
		AAPanel p = new AAPanel();
		p.define('a', new Button("button1"), "fill=b;wy=1");
		p.define('b', new Button("button2"), "fill=b;wx=1");
		p.define('c', new Button("button3"), "fill=b;wx=1;wy=1");
		p.define('d', new Button("button4"), "fill=b;wx=1");
		p.define('e', new Button("button5"), "fill=b;wx=1;wy=1");
		p.define('x', new Button("button6"), "fill=b;wx=1;wy=1");
		p.define('f', new AAPanel(),         "fill=b;wx=1;wy=1;pad=3,3,3,3");
		p.setLayout(new String[] {
			"aaaaaaaaa",
			"         ",
			"b fffffff",
			"b f     f",
			"b f ccc f",
			"b f xxx f",
			"b f     f",
			"b fffffff",
			"b        ",
			"b ddd eee"
		});

		Frame f = new Frame("test");
		f.setLayout(new GridLayout(1,1));
		f.add(p);
		f.pack();
		f.setVisible(true);
	}

	// ---( constructors )---
	public AAPanel()
	{
		this(false);
	}

	public AAPanel(boolean managed)
	{
		this.managed = managed;
		layout = new GridBagLayout();
		option = new GridBagConstraints();
		common = new Hashtable();
		common.put("rem", new Integer(GridBagConstraints.REMAINDER));
		common.put("rel", new Integer(GridBagConstraints.RELATIVE));
		common.put("h",   new Integer(GridBagConstraints.HORIZONTAL));
		common.put("v",   new Integer(GridBagConstraints.VERTICAL));
		common.put("b",   new Integer(GridBagConstraints.BOTH));
		common.put("n",   new Integer(GridBagConstraints.NONE));
		common.put("+c",  new Integer(GridBagConstraints.CENTER));
		common.put("+n",  new Integer(GridBagConstraints.NORTH));
		common.put("+s",  new Integer(GridBagConstraints.SOUTH));
		common.put("+e",  new Integer(GridBagConstraints.EAST));
		common.put("+w",  new Integer(GridBagConstraints.WEST));
		common.put("+ne", new Integer(GridBagConstraints.NORTHEAST));
		common.put("+nw", new Integer(GridBagConstraints.NORTHWEST));
		common.put("+se", new Integer(GridBagConstraints.SOUTHEAST));
		common.put("+sw", new Integer(GridBagConstraints.SOUTHWEST));
		setLayout(layout);
	}

	// ---( instance fields )---
	private GridBagLayout layout;
	private GridBagConstraints option;
	private Hashtable common;

	private boolean done[];
	private Component comp[];
	private GridBagConstraints cons[];

	private boolean managed;

	// ---( instance methods )---
	private void checkSetup()
	{
		if (comp == null)
		{
			done = new boolean[256];
			comp = new Component[256];
			cons = new GridBagConstraints[256];
		}
	}

	public Component item(char ch)
	{
		return (comp[ch&0xff]);
	}
	
	private void ensureConstraints(int ch)
	{
		ch = (ch & 0xff);
		if (cons[ch] == null)
		{
			cons[ch] = new GridBagConstraints();
			cons[ch].gridx = -1;
		}
	}

	public void define(char ch)
	{
		define(ch, new AAPanel(true));
	}

	public void define(char ch, String opt)
	{
		define(ch, new AAPanel(true), opt);
	}

	public void define(char ch, Component c)
	{
		define(ch, c, null);
	}

	public void define(char ch, Component c, String conf)
	{
		define(Math.min(ch&0xff,255), c, conf);
	}

	private void define(int ch, Component c, String conf)
	{
		checkSetup();
		ensureConstraints(ch);
		comp[ch] = c;
		cons[ch] = config(conf, cons[ch]);
	}

	public void setLayout(String line[])
	{
		checkSetup();
		int maxw = 0;
		for (int i=0; i<line.length; i++)
		{
			maxw = Math.max(maxw, line[i].length());
		}
		for (int y=0; y<line.length; y++)
		{
			for (int x=0; x<line[y].length(); x++)
			{
				int ch = line[y].charAt(x) & 0xff;
				if (ch == ' ')
				{
					continue;
				}
				ensureConstraints(ch);
				if (cons[ch].gridx == -1)
				{
					cons[ch].gridx = x;
					cons[ch].gridy = y;
				}
				cons[ch].gridwidth = (x-cons[ch].gridx+1);
				cons[ch].gridheight = (y-cons[ch].gridy+1);
			}
		}
		for (int i=0; i<comp.length; i++)
		{
			if (comp[i] == null)
			{
				continue;
			}
			int container = -1;
			for (int j=0; j<comp.length; j++)
			{
				if (i == j || comp[j] == null || !(comp[j] instanceof AAPanel))
				{
					continue;
				}
				if (inside(cons[i], cons[j]))
				{
					if (container == -1 || inside(cons[j],cons[container]))
					{
						container = j;
					}
				}
			}
			if (container >= 0)
			{
				AAPanel outer = (AAPanel)comp[container];
				GridBagConstraints gbc = cons[container];
				outer.transfer(i, comp[i], cons[i], gbc.gridx, gbc.gridy);
				done[i] = true;
			}
		}
		finishLayout();
	}

	private void finishLayout()
	{
		if (comp == null)
		{
			return;
		}
		for (int i=0; i<comp.length; i++)
		{
			if (comp[i] != null && done[i] != true)
			{
				if (comp[i] instanceof AAPanel && ((AAPanel)comp[i]).managed)
				{
					((AAPanel)comp[i]).finishLayout();
				}
				layout.setConstraints(comp[i], cons[i]);
				add(comp[i]);
			}
		}
	}

	private boolean inside(GridBagConstraints in, GridBagConstraints out)
	{
		return
			in.gridx > out.gridx &&
			in.gridy > out.gridy &&
			(in.gridx + in.gridwidth) < (out.gridx + out.gridwidth) &&
			(in.gridy + in.gridheight) < (out.gridy + out.gridheight);
	}

	// transfer definition from this panel to an inner container
	// used by setLayout during nested processing
	private void transfer(
		int ch, Component c, GridBagConstraints conf, int ox, int oy)
	{
		try
		{
		checkSetup();
		ensureConstraints(ch);
		comp[ch] = c;
		cons[ch] = (GridBagConstraints)conf.clone();
		cons[ch].gridx -= ox;
		cons[ch].gridy -= oy;
		}
		catch (Exception ex)
		{
		ex.printStackTrace();
		}
	}

	private int common(String key, int def)
	{
		Integer i = (Integer)common.get(key);
		if (i != null)
		{
			return i.intValue();
		}
		else
		{
			return def;
		}
	}

	private int parse(String key)
	{
		return Integer.parseInt(key);
	}

	private double parseD(String key)
	{
		// not using parseDouble() since not compat w/ MS JVIEW
		return new Double(key).doubleValue();
	}

	private int parseSize(String key)
	{
		if (key.equals("*"))
		{
			return option.REMAINDER;
		}
		else
		{
			return parse(key);
		}
	}

	public void config(String opt)
	{
		option = config(opt, option);
	}

	private GridBagConstraints config(String opt, GridBagConstraints option)
	{
		if (opt == null)
		{
			return option;
		}
		StringTokenizer st = new StringTokenizer(opt.toLowerCase(), ";");
		while (st.hasMoreTokens())
		{
			StringTokenizer s2 = new StringTokenizer(st.nextToken(),"=");
			String key = s2.nextToken();
			String val = s2.hasMoreTokens() ? s2.nextToken() : "";
			if (key.equals("clr"))
			{
				option = new GridBagConstraints();
			}
			else
			if (key.equals("x"))
			{
				option.gridx = parse(val);
			}
			else
			if (key.equals("y"))
			{
				option.gridy = parse(val);
			}
			else
			if (key.equals("w"))
			{
				option.gridwidth = parseSize(val);
			}
			else
			if (key.equals("h"))
			{
				option.gridheight = parseSize(val);
			}
			else
			if (key.equals("pad"))
			{
				s2 = new StringTokenizer(val,",");
				option.insets = new Insets(
					parse(s2.nextToken()),
					parse(s2.nextToken()),
					parse(s2.nextToken()),
					parse(s2.nextToken())
				);
			}
			else
			if (key.equals("ix"))
			{
				option.ipadx = parse(val);
			}
			else
			if (key.equals("iy"))
			{
				option.ipady = parse(val);
			}
			else
			if (key.equals("wx"))
			{
				option.weightx = parseD(val);
			}
			else
			if (key.equals("wy"))
			{
				option.weighty = parseD(val);
			}
			else
			if (key.equals("fill"))
			{
				option.fill = common(val,option.NONE);
			}
			else
			if (key.equals("anchor"))
			{
				option.anchor = common("+"+val,option.CENTER);
			}
		}
		return option;
	}

	public Component add(Component c, String opt)
	{
		config(opt);
		layout.setConstraints(c, option);
		return super.add(c);
	}

	public void disable()
	{
		able(this, false);
	}

	public void enable()
	{
		able(this, true);
	}

	private void able(Container c, boolean b)
	{
		Component e[] = c.getComponents();
		for (int i=0; i<e.length; i++)
		{
			Component cc = e[i];
			cc.setEnabled(b);
			if (cc instanceof Container)
			{
				able((Container)cc, b);
			}
		}
	}

	public void clear()
	{
		clear(this);
	}

	private void clear(Container c)
	{
		Component e[] = c.getComponents();
		for (int i=0; i<e.length; i++)
		{
			Component cc = e[i];
			if (cc instanceof JTextComponent)
			{
				((JTextComponent)cc).setText("");
			}
			if (cc instanceof Container)
			{
				clear((Container)cc);
			}
		}
	}
}


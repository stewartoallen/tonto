/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto.ui;

// ---( imports )---
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

public class UIUtils
{
	// ---( static fields )---

	// ---( static methods )---
	public static AAPanel getOtherOKCancel(String other[], ActionListener l)
	{
		AAPanel p1 = new AAPanel(true);
		p1.add(getActionButton("OK", l),
			"x=0;y=0;pad=3,3,3,3;fill=b");
		for (int i=0; i<other.length; i++)
		{
			p1.add(getActionButton(other[i], l),
				"x="+(1+i)+";y=0;pad=3,3,3,3;fill=b");
		}
		p1.add(getActionButton("Cancel", l),
			"x="+(other.length+1)+";y=0;pad=3,3,3,3;fill=b");
		return p1;
	}

	public static JButton getActionButton(String label, ActionListener l)
	{
		JButton b = new JButton(label);
		b.addActionListener(l);
		return b;
	}

	public static AAPanel getOKCancel(ActionListener l)
	{
		AAPanel p1 = new AAPanel(true);
		p1.add(getActionButton("OK", l),     "x=0;y=0;pad=3,3,3,3;fill=b");
		p1.add(getActionButton("Cancel", l), "x=1;y=0;pad=3,3,3,3;fill=b");
		return p1;
	}

	// ---( constructors )---
	private UIUtils ()
	{
	}

	// ---( instance fields )---

	// ---( instance methods )---

	// ---( interface methods )---

}


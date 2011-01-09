/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public abstract class ActionButton extends JButton implements ActionListener
{
	public ActionButton(String label)
	{
		super(label);
		addActionListener(this);
	}

	public void actionPerformed(ActionEvent ae)
	{
		if (ae.getSource() == this)
		{
			action();
		}
	}

	public abstract void action();
}


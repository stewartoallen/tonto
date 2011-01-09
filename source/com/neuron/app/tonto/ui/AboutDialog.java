/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto.ui;

import com.neuron.app.tonto.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class AboutDialog extends StackedDialog implements ActionListener
{
	private Tonto tonto;

	public AboutDialog(Tonto tonto)
	{
		super("About Tonto");
		this.tonto = tonto;
		//setBackground(Color.black);
		
		JButton ok = new JButton("OK");
		ok.addActionListener(this);

		Runtime run = Runtime.getRuntime();
		long fmem = run.freeMemory() / 1024;
		long tmem = run.totalMemory() / 1024;

		JLabel ver = new JLabel("Version "+tonto.version(), JLabel.CENTER);
		JLabel mem = new JLabel("Memory "+fmem+"k of "+tmem+"k free", JLabel.CENTER);
		ver.setForeground(Color.black);
		mem.setForeground(Color.black);

		add(tonto.getTitleCanvas(), "x=0;y=0;fill=b;pad=3,3,3,3");
		add(ver, "x=0;y=1;fill=b;pad=3,3,3,3");
		add(mem, "x=0;y=2;fill=b;pad=3,3,3,3");
		add(ok,  "x=0;y=3;fill=b;pad=5,5,5,5");
	}

	public void actionPerformed(ActionEvent ae)
	{
		dispose();
	}
}


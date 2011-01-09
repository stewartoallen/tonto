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
import java.util.Stack;
import java.util.Hashtable;
import java.util.StringTokenizer;

public abstract class OKCancelDialog
	extends StackedDialog implements ActionListener
{
	private boolean ok;

	public OKCancelDialog(String title)
	{
		this(title, true);
	}

	public OKCancelDialog(String title, boolean bind)
	{
		super(title);
		setLayout(new BorderLayout());
		add("South", getButtons());
		if (bind)
		{
			bindOK();
		}
		bindCancel();
	}

	public void bindOK()
	{
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
			KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0,true), "OK");
		getActionMap().put("OK", new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				enterOK();
			}
		});
	}

	public void bindCancel()
	{
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0,true), "CANCEL");
		getActionMap().put("CANCEL", new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				escCancel();
			}
		});
	}

	public void setContents(Component com)
	{
		if (com != null)
		{
			add("Center", com);
		}	
	}

	public void setOK(boolean o)
	{
		ok = o;
	}

	public void enterOK()
	{
		setOK(true);
		doOK();
		dispose();
	}

	public void escCancel()
	{
		setOK(false);
		doCancel();
		dispose();
	}

	public void actionPerformed(ActionEvent ae)
	{
		String cmd = ae.getActionCommand();
		if (cmd.equals("OK"))
		{
			enterOK();
		}
		else
		if (cmd.equals("Cancel"))
		{
			escCancel();
		}
		else
		{
			if (handleCmd(cmd))
			{
				dispose();
			}
		}
	}

	public JComponent getButtons()
	{
		return UIUtils.getOKCancel(this);
	}

	public boolean handleCmd(String cmd)
	{
		// override here
		return false;
	}

	public boolean invoke()
	{
		show();
		return ok;
	}

	public abstract void doOK() ;

	public abstract void doCancel() ;
}


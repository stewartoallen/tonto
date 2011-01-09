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
import java.util.*;
import javax.swing.*;
import com.neuron.app.tonto.*;

public class StackedDialog extends AAPanel
{
	private static Debug debug = Debug.getInstance("tonto");

	private Component parent;
	private JDialog dialog;
	private String title;
	private boolean modal;
	private boolean disposeOnShow;

	public StackedDialog(String title)
	{
		this(title, true);
	}

	public StackedDialog(String title, boolean modal)
	{
		this.title = title;
		this.modal = modal;
	}

	public void setContentPane(Container c)
	{
		add(c, "fill=b;wx=1;wy=1");
		if (c instanceof JComponent)
		{
			JComponent cc = (JComponent)c;
			cc.getInputMap(cc.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0,true), "ESC");
			cc.getActionMap().put("ESC", new AbstractAction() {
				public void actionPerformed(ActionEvent ae) {
					dispose();
				}
			});
		}
	}

	public void setTitle(String title)
	{
		this.title = title;
		if (dialog != null)
		{
			dialog.setTitle(title);
		}
	}

	public void showHook(JDialog dialog)
	{
	}

	public void repack()
	{
		if (dialog != null)
		{
			dialog.pack();
		}
	}

	public void show()
	{
		Component nparent = parent();
		synchronized (this)
		{

		if (dialog == null || nparent != parent)
		{
			if (nparent instanceof JFrame)
			{
				dialog = new JDialog((JFrame)nparent, title, modal);
			}
			else
			{
				dialog = new JDialog((JDialog)nparent, title, modal);
			}
			parent = nparent;
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosed(WindowEvent we) {
					windowClose();
				}
			});
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			Container c = dialog.getContentPane();
			c.setLayout(new GridLayout(1,1));
			c.add(this);
		}

		}
		if (!disposeOnShow)
		{
			dialog.pack();
			dialog.setLocationRelativeTo(parent);
			if (modal)
			{
				dialogPush(dialog);
			}
			showHook(dialog);
			dialog.show();
		}
		disposeOnShow = false;
	}

	public void windowClose()
	{
		if (modal && dialog != null)
		{
			dialogPop(dialog);
		}
	}

	public void dispose()
	{
		if (dialog != null)
		{
			if (!dialog.isVisible())
			{
				debug.log(2,"dispose called when dialog not visible");
			}
			windowClose();
			dialog.dispose();
		}
		else
		{
			debug.log(2,"dispose called with null dialog");
			disposeOnShow = true;
		}
	}

	// ---( static methods )---
	private static JFrame jframe = new JFrame();
	private static Stack dialogs = new Stack();

	public static Component parent()
	{
		if (dialogs.empty())
		{
			return jframe;
		}
		else
		{
			return (JDialog)dialogs.peek();
		}
	}

	private static void dialogPush(JDialog d)
	{
		debug.log(3,"push "+Util.nickname(d));
		dialogs.push(d);
	}

	private static void dialogPop(JDialog d)
	{
		debug.log(3,"pop  "+Util.nickname(d));
		if (d == null)
		{
			return;
		}
		Component od = parent();
		if (od == d)
		{
			dialogs.pop();
		}
		else
		{
			if (dialogs.contains(d))
			{
				debug.log(3,"pop out of order: "+Util.nickname(d)+" != "+Util.nickname(od));
				dialogs.remove(d);
			}
		}
	}

	public static void setDefaultParent(JFrame frame)
	{
		jframe = frame;
	}
}


/*
 * Copyright 2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto.ui;

// ---( imports )---
import java.awt.*;
import java.awt.event.*;

public class ModalWindow extends Window
{
	// ---( static fields )---

	// ---( static methods )---
	public static void main(String args[])
	{
		Frame f = new Frame("root frame");
		f.setLayout(new GridLayout(2,2));
		f.add(new Button("1"));
		f.add(new Button("2"));
		f.add(new Button("3"));
		f.add(new Button("4"));
		f.pack();
		f.show();

		ModalWindow mw = new ModalWindow(f);
		mw.setLayout(new GridLayout(2,2));
		mw.add(new Button("1"));
		mw.add(new Button("2"));
		mw.add(new Button("3"));
		mw.add(new Button("4"));
		mw.pack();
		mw.show();
	}

	// ---( constructors )---
	public ModalWindow (Window owner)
	{
		super(owner);
		setup();
	}

	// ---( instance fields )---
	private EventQueue sysQ = Toolkit.getDefaultToolkit().getSystemEventQueue();
	private MyQ myQ = new MyQ();
	private boolean open = false;

	// ---( instance methods )---
	private void setup()
	{
		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent we) {
				if (!open)
				{
					open = true;
					sysQ.push(myQ);
				}
				toFront();
			}
			public void windowClosed(WindowEvent we) {
				if (open)
				{
					myQ.done();
					open = false;
				}
			}
		});
	}

	public void show()
	{
		Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
		Rectangle wp = getBounds();
		wp.x = Math.min(ss.width - wp.width, wp.x);
		wp.y = Math.min(ss.height - wp.height, wp.y);
		setBounds(wp);
		super.show();
	}

	// ---( interface methods )---

	class MyQ extends EventQueue
	{
		public void dispatchEvent(AWTEvent evt)
		{
			Object src = evt.getSource();
			if (!(src instanceof Component))
			{
				super.dispatchEvent(evt);
				return;
			}
			Component c = (Component)src;
			while (c != null && c != ModalWindow.this)
			{
				c = c.getParent();
			}
			if (c == ModalWindow.this)
			{
				super.dispatchEvent(evt);
			}
			else
			{
				if (evt instanceof MouseEvent)
				{
					MouseEvent me = (MouseEvent)evt;
					if (me.getID() == me.MOUSE_CLICKED)
					{
						dispose();
					}
					return;
				}
				else
				if (evt instanceof KeyEvent)
				{
					return;
				}
				super.dispatchEvent(evt);
			}
		}

		public void done()
		{
			pop();
		}
	}
}


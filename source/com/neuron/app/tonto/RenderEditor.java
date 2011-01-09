/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import com.neuron.app.tonto.ui.*;

public class RenderEditor extends JFrame implements ICCFEditor
{
	private final static Debug debug = Debug.getInstance("editor");
	private final Color LABELBG = new Color(128,128,128);
	private final Color majorTick = Color.blue;
	private final Color minorTick = new Color(0,128,255);

	public static void main(String args[])
		throws Exception
	{
		CCF ccf = new CCF();
		ccf.load(args[0]);

		RenderEditor editor = new RenderEditor(ccf);
		editor.show();
	}

	public RenderEditor(CCF ccf)
	{
		super("CCF Editor");

		this.ccf = ccf;
		this.render = new Renderer(ccf);
		this.devType = new JComboBox();
		this.devices = new DeviceList();    // 1
		this.panels = new PanelList();      // 2
		this.display = new PanelDisplay();  // 3
		this.props = new PropEditor();      // 4
		this.actions = new ActEditor();     // 5
		this.doList = new Stack();
		this.undoList = new Stack();

		JPanel devs = new JPanel();
		devs.setLayout(new BorderLayout(1,1));
		devs.add("North", label("Section", devType));
		devs.add("Center", label("Devices", scrollv(devices)));

		/* +-------+--------------------------------+-------+
		 * |   1   |                                |   4   |
		 * +-------+                3               +-------+
		 * |   2   |                                |   5   |
		 * +-------+--------------------------------+-------+ */

		JSplitPane sFolder = new JSplitPane(  // 1, 2
			JSplitPane.VERTICAL_SPLIT, devs, label("Panels", scrollv(panels))
		);
		JSplitPane sProps = new JSplitPane(   // 4, 5
			JSplitPane.VERTICAL_SPLIT, label("Properties", props), label("Actions", actions)
		);
		JSplitPane sWidget = new JSplitPane(  // 3, (4,5)
			JSplitPane.HORIZONTAL_SPLIT, scrollhv(display), sProps
		);
		JSplitPane sMaster = new JSplitPane(  // (1,2), (3, (4,5))
			JSplitPane.HORIZONTAL_SPLIT, sFolder, sWidget
		);

		getContentPane().setLayout(new GridLayout(1,1));
		getContentPane().add(sMaster);

		pack();
		setSize(1000,800);

		sProps.setDividerLocation(0.5f);
		sWidget.setResizeWeight(1.0d);
		sWidget.setDividerLocation(650);
		sFolder.setDividerLocation(200);
		sFolder.setResizeWeight(1.0d);
		sMaster.setDividerLocation(150);

		devices.setRoot(ccf.getFirstHomeDevice());

		devType.addItem("System");
		devType.addItem("Home");
		devType.addItem("Devices");
		devType.addItem("Macros");
		devType.setSelectedIndex(1);
		devType.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				switch (devType.getSelectedIndex())
				{
					case 0: setDeviceRoot(null); setPanelRoot(ccf().getMacroPanel()); break;
					case 1: setDeviceRoot(ccf().getFirstHomeDevice()); break;
					case 2: setDeviceRoot(ccf().getFirstDevice()); break;
					case 3: setDeviceRoot(ccf().getFirstMacroDevice()); break;
				}
			}
		});

		addKeyListener(new KeyAdapter()
		{
			public void keyPressed(KeyEvent ke)
			{
				switch (ke.getKeyChar())
				{
					case '1': display.setScale(1); break;
					case '2': display.setScale(2); break;
				}
				switch (ke.getKeyCode())
				{
					case KeyEvent.VK_G:
						toggleGrid = !toggleGrid;
						display.setGrid(new RenderGrid(0,0,10,10,majorTick,minorTick,2), toggleGrid);
						break;
					case KeyEvent.VK_S:
						toggleSnap = !toggleSnap;
						display.snapGrid(toggleSnap);
						break;
					case KeyEvent.VK_Q:
						quit();
						break;
				}
			}
		});

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent we)
			{
				quit();
			}
		});
	}

	private CCF ccf;
	private Renderer render;
	private CCFNode[] clipboard;
	private JComboBox devType;
	private DeviceList devices;
	private PanelList panels;
	private PanelDisplay display;
	private PropEditor props;
	private ActEditor actions;
	private Stack doList;
	private Stack undoList;
	private boolean toggleGrid;
	private boolean toggleSnap;

	public CCF ccf()
	{
		return ccf;
	}

	// --( instance methods )------------------------------------------------
	public void quit()
	{
		dispose();
		//System.exit(0);
	}

	public void setDeviceRoot(CCFDevice dev)
	{
		devices.setRoot(dev);
		setPanelRoot(null);
	}

	public void setPanelRoot(CCFPanel dev)
	{
		panels.setRoot(dev);
	}

	private JPanel label(String label, JComponent c)
	{
		JLabel l = new JLabel(label, JLabel.CENTER);
		l.setForeground(Color.white);
		JPanel n = new JPanel();
		n.setLayout(new GridLayout(1,1));
		n.setBackground(LABELBG);
		n.add(l);
		JPanel lp = new JPanel();
		lp.setLayout(new BorderLayout(2,2));
		lp.add("North", n);
		lp.add("Center", c);
		return lp;
	}

	private JScrollPane scrollhv(JComponent c)
	{
		return new JScrollPane(c, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	}

	private JScrollPane scrollv(JComponent c)
	{
		return new JScrollPane(c, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	}

	// --( editor interface )------------------------------------------------
	public void setClipboard(CCFNode node[])
	{
		clipboard = node;
	}

	public CCFNode[] getClipboard()
	{
		return clipboard;
	}

	public void pushEdit(IEditAction edit)
	{
		debug.log(0, "pushEdit : "+edit);
	}

	public void setSelection(Object src, Enumeration en)
	{
		display.clearSelections(src);
	}

	// ----------------------------------------------------------------------
	private class PanelDisplay extends AAPanel
	{
		private Vector panels;
		private Hashtable windows;
		private Hashtable renders;
		private Hashtable pos;
		private int xmax, ymax, scale = 1;

		PanelDisplay()
		{
			panels = new Vector();	
			windows = new Hashtable();
			renders = new Hashtable();
			pos = new Hashtable();
			setBackground(Color.white);
			setScale(1);
		}

		public void clearSelections(Object src)
		{
			for (Enumeration e = renders.elements(); e.hasMoreElements(); )
			{
				RenderPanel rp = (RenderPanel)e.nextElement();
				if (rp != src)
				{
					rp.clearSelection();
				}
			}
		}

		public void snapGrid(boolean b)
		{
			for (Enumeration e = renders.elements(); e.hasMoreElements(); )
			{
				((RenderPanel)e.nextElement()).snapGrid(b);
			}
		}

		public void setGrid(RenderGrid g, boolean b)
		{
			for (Enumeration e = renders.elements(); e.hasMoreElements(); )
			{
				RenderPanel rp = (RenderPanel)e.nextElement();
				rp.setGrid(g);
				rp.showGrid(b);
			}
			refresh(true);
		}

		public void setScale(int s)
		{
			this.scale = s;
			for (Enumeration e = renders.elements(); e.hasMoreElements(); )
			{
				((RenderPanel)e.nextElement()).setScale(s);
			}
			refresh(true);
		}

		public void refresh(boolean repaint)
		{
			JComponent p = (JComponent)getParent();
			if (p != null)
			{
				p.revalidate();
				if (repaint)
				{
					p.repaint();
				}
			}
		}

		public void addPanel(CCFPanel panel)
		{
			if (!panels.contains(panel))
			{
				JPanel lp = window(panel);
				panels.add(panel);
				windows.put(panel, lp);
				for (int y=0; ; y++)
				{
					for (int x=0; x<xmax; x++)
					{
						String xy = ("x="+x+";y="+y+";pad=5,5,5,5");
						if (pos.get(xy) == null)
						{
							pos.put(xy, panel);
							pos.put(panel, xy);
							add(lp, xy);
							refresh(false);
							return;
						}
					}
				}
			}
		}

		private JPanel window(CCFPanel panel)
		{
			final CCFPanel fp = panel;
			RenderPanel rp = new RenderPanel(RenderEditor.this, render, panel);
			rp.setScale(scale);
			CCFDevice dev = panel.getParentDevice();
			String dname = dev != null ? dev.getName() : "SYS";
			String label = dname+" : "+panel.getName();
			JLabel l = new JLabel(label, JLabel.CENTER);
			l.setForeground(Color.white);
			JButton b = new JButton("x");
			b.setFocusPainted(false);
			b.setBorderPainted(false);
			b.setMargin(new Insets(1,1,1,1));
			b.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent ae)
				{
					debug.log(0, "button : "+fp);
					removePanel(fp);
				}
			});
			AAPanel lp = new AAPanel();
			lp.setBorder(new EtchedBorder(EtchedBorder.RAISED));
			lp.setBackground(LABELBG);
			lp.add(l,  "x=1;y=1;pad=1,1,1,1;wx=1");
			lp.add(b,  "x=2;y=1;pad=1,1,1,1;wx=0");
			lp.add(rp, "x=1;y=2;pad=1,1,1,1;w=2;wx=1;xy=1");
			renders.put(panel, rp);
			return lp;
		}

		public void removePanel(CCFPanel panel)
		{
			panels.remove(panel);
			renders.remove(panel);
			remove((JComponent)windows.remove(panel));
			refresh(true);
			String pxy = (String)pos.remove(panel);
			pos.remove(pxy);
		}

		public void paint(Graphics g)
		{
			Dimension sz = getSize();
			Dimension ps = ccf().getScreenSize();
			xmax = Math.max((sz.width/(ps.width*scale)), 1);
			ymax = sz.height/(ps.height*scale);
			super.paint(g);
		}

		public Dimension getMinimumSize()
		{
			return getPreferredSize();
		}

		public Dimension getMaximumSize()
		{
			return getPreferredSize();
		}

		public Dimension getPreferredSize()
		{
			Rectangle bounds = new Rectangle(0,0,0,0);
			for (Enumeration e = windows.elements(); e.hasMoreElements(); )
			{
				bounds = bounds.union( ((JComponent)e.nextElement()).getBounds() );
			}
			return new Dimension(bounds.width, bounds.height);
		}
	}

	// ----------------------------------------------------------------------
	private class PropEditor extends JPanel
	{
		PropEditor()
		{
			setBackground(Color.white);
		}
	}

	// ----------------------------------------------------------------------
	private class ActEditor extends JPanel
	{
		ActEditor()
		{
			setBackground(Color.white);
		}
	}

	// ----------------------------------------------------------------------
	private class DeviceList extends ItemList
	{
		private CCFDevice root;

		public void setRoot(CCFDevice root)
		{
			int osize = getListSize();
			this.root = root;
			changeSize(osize, getListSize());
		}

		public IListElement getRootElement()
		{
			return root;
		}

		public void click(MouseEvent me)
		{
			select();
		}

		public void select()
		{
			setPanelRoot(((CCFDevice)getListElement(getSelectedIndex())).getFirstPanel());
		}
	}

	// ----------------------------------------------------------------------
	private class PanelList extends ItemList
	{
		private CCFPanel root;

		public void setRoot(CCFPanel root)
		{
			int osize = getListSize();
			this.root = root;
			changeSize(osize, getListSize());
		}

		public IListElement getRootElement()
		{
			return root;
		}

		public void click(MouseEvent me)
		{
			if (me.getClickCount() == 2)
			{
				display.addPanel((CCFPanel)getListElement(getSelectedIndex()));
			}
		}

		public void select()
		{
		}
	}

	// ----------------------------------------------------------------------
	private abstract class ItemList extends JList
	{
		private Vector listeners = new Vector();

		ItemList()
		{
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

			addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent me)
				{
					click(me);
				}
			});

			addListSelectionListener(new ListSelectionListener()
			{
				public void valueChanged(ListSelectionEvent lse)
				{
					select();
				}
			});

			setModel(new ListModel()
			{
				public void addListDataListener(ListDataListener l)
				{
					listeners.add(l);
				}

				public void removeListDataListener(ListDataListener l)
				{
					listeners.remove(l);
				}

				public Object getElementAt(int index)
				{
					return getListElement(index);
				}

				public int getSize()
				{
					return getListSize();
				}
			});
		}

		public void changeSize(int osize, int nsize)
		{
			for (Enumeration e = listeners.elements(); e.hasMoreElements(); )
			{
				ListDataListener l = (ListDataListener)e.nextElement();
				l.intervalRemoved(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, 0, osize));
				l.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, nsize));
			}
		}

		public Object getListElement(int index)
		{
			IListElement dev = getRootElement();
			for (int i=0; dev != null && i < index; i++)
			{
				dev = dev.getNextElement();
			}
			return dev;
		}

		public int getListSize()
		{
			IListElement dev = getRootElement();
			int sz = 0;
			while (dev != null)
			{
				dev = dev.getNextElement();
				sz++;
			}
			return sz;
		}

		public abstract IListElement getRootElement()
			;

		public abstract void click(MouseEvent me)
			;

		public abstract void select()
			;
	}
}



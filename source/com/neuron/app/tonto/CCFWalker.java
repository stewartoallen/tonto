/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.Stack;
import java.util.Hashtable;
import java.lang.reflect.Field;
import com.neuron.irdb.*;
import com.neuron.irdb.impl.*;

/**
 * A utility class for scanning a CCF tree. CCFWalker allows you to implement
 * a single one-method interface (IWalker) for walking a CCF structure. Plug
 * it into an instanceof IWalker and call walk() to be notified as each CCF 
 * tree node (device, panel, frame, button) is scanned.
 */
public class CCFWalker
{
	// ---( static fields )---
	private final static Hashtable cmd = new Hashtable();

	private final static int HEAD  = 0;
	private final static int VIEW  = 1;
	private final static int IREX1 = 2;
	private final static int LOAD  = 3;
	private final static int SORT  = 4;
	private final static int SUMM  = 5;
	private final static int IREX2 = 6;
	private final static int DBUG  = 7;
	private final static int XPRT  = 8;
	private final static int MPRT  = 9;

	static
	{
		cmd.put("-head",  new Integer(HEAD));
		cmd.put("-view",  new Integer(VIEW));
		cmd.put("-irex1", new Integer(IREX1));
		cmd.put("-irex2", new Integer(IREX2));
		cmd.put("-load",  new Integer(LOAD));
		cmd.put("-sort",  new Integer(SORT));
		cmd.put("-summ",  new Integer(SUMM));
		cmd.put("-dbug",  new Integer(DBUG));
		cmd.put("-xprt",  new Integer(XPRT));
		cmd.put("-mprt",  new Integer(MPRT));

		if (System.getProperty("ccf") != null)
		{
			Debug.getInstance("ccf").setLevel(3);
		}
	}

	// ---( static methods )---
	public static void main(String args[])
		throws Exception
	{
		if (args.length < 2)
		{
			usage();
			return;
		}
		Integer c = (Integer)cmd.get(args[0]);
		if (c == null)
		{
			usage();
			return;
		}
		for (int i=1; i<args.length; i++)
		{
			switch (c.intValue())
			{
				case HEAD:  head(args[i]); break;
				case VIEW:  view(args[i]); break;
				case IREX1: irex(args[i],true); break;
				case LOAD:  load(args[i]); break;
				case SORT:  sort(args[i]); break;
				case SUMM:  summ(args[i],i); break;
				case IREX2: irex(args[i],false); break;
				case DBUG:  dbug(args[i]); break;
				case XPRT:  xprt(args[1],args[2]); i=args.length; break;
				case MPRT:  mprt(args[1],args[2]); i=args.length; break;
				default: usage(); break;
			}
		}
		System.exit(0);
	}

	private static String[] subarr(String s[], int off, int len)
	{
		String ns[] = new String[len];
		System.arraycopy(s,off,ns,0,len);
		return ns;
	}

	private static void usage()
	{
		debug("CCFWalker <command> <ccf.file>");
		debug("   -head      = dump ccf header");
		debug("   -view      = walk the ccf tree");
		debug("   -irex[1|2] = extract ir codes 1) save 2) view");
		debug("   -load      = raw upload from pronto to file");
		debug("   -summ      = summarize headers of several ccfs");
		debug("   -dbug      = full debug load/parse of ccf");
		debug("   -xprt      = export ccf as zip/xml file");
		debug("   -mprt      = import ccf from zip/xml file");
	}

	// -- CCF Heaeder --
	private static void head(String file)
		throws IOException
	{
		CCF ccf = new CCF();
		Debug.getInstance("ccf").setLevel(1);
		ccf.load(file);
		ccf.header().dump();
	}

	private static void dbug(String file)
		throws IOException
	{
		CCF ccf = new CCF();
		Debug.getInstance("ccf").setLevel(3);
		ccf.load(file);
	}

	private static void xprt(String ccfile, String zip)
		throws IOException
	{
		CCF ccf = new CCF();
		ccf.load(ccfile);
		CCFPorter.exportZip(ccf, zip);
	}

	private static void mprt(String zip, String ccfile)
		throws IOException
	{
		CCF ccf = CCFPorter.importZip(zip);
		ccf.save(ccfile);
	}

	private static void view(String file)
		throws IOException
	{
		CCF ccf = new CCF();
		ccf.load(file);
		new CCFWalker(ccf).walk(new IWalker() {
			public void onNode(CCFNode node) {
				if (node instanceof CCFDevice) {
					debug(node.describe());
				}
				else
				if (node instanceof CCFPanel) {
					debug("  "+node.describe());
				}
				else
				if (node instanceof CCFIcon) {
					debug("    "+node.describe());
				}
			}
		});
	}

	private static void load(String file)
		throws Exception
	{
		Debug.getInstance("comm").setLevel(2);
		Debug.getInstance("xmodem").setLevel(2);
		CCF ccf = new CCF();
		ccf.setNotify(new ITaskStatus() {
			public void taskStatus(int pct, String val) {
				debug("** "+pct+", "+val);
			}
			public void taskError(Throwable t) {
				debug("** "+t);
			}
			public void taskNotify(Object o) {
				debug("** "+o);
			}
		});
		byte b[] = ccf.loadBytesFromPronto();
		new FileOutputStream(file).write(b);
	}

	private static void sort(String file)
		throws Exception
	{
		CCF ccf = new CCF();
		ccf.load(file);
		CCFNodeState state = ccf.getLastNodeState();
		CCFNode nodes[] = state.getOrderedNodes();
		int lastPos = 0;
		int wasted = 0;
		for (int i=0; i<nodes.length; i++)
		{
			CCFNode node = nodes[i];
			int pos = node.getFilePosition();
			int len = node.getLength();
			if (!(node instanceof CCFAction || node instanceof CCFChild) && pos != lastPos)
			{
				debug("** file gap int("+lastPos+" - "+pos+") "+
				"hex("+Integer.toHexString(lastPos)+" - "+Integer.toHexString(pos)+") "+
				"(len="+(pos-lastPos)+")");
				wasted += (pos-lastPos);
			}
			lastPos = pos + len;
			debug("["+i+"] pos="+pos+" len="+len+" >> "+node.describe());
		}
		debug("** wasted space: "+wasted);
		System.exit(0);
	}

	private static void summ(String file, int idx)
		throws Exception
	{
		if (idx == 1)
		{
			debug("capability                            "+
				  "attributes                            version4");
		}
		CCF ccf = new CCF();
		ccf.load(file);
		CCFHeader head = ccf.header();
		String fname = new File(file).getName();
		if (fname.toLowerCase().endsWith(".ccf"))
		{
			fname = fname.substring(0,fname.length()-4);
		}
		ProntoModel pm[] = ccf.getConformsMatch();
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<pm.length; i++)
		{
			sb.append(pm[i].getName()+(i<pm.length-1 ? ", " : ""));
		}
		debug(head.summary()+" "+CCFNode.rpad(fname,20)+" "+sb.toString());
	}

	private static void irex(String file, boolean store)
		throws IOException
	{
		CCF ccf = new CCF();
		ccf.load(file);
		if (store)
		{
			irex(ccf, IRDatabase.create("new.db"));
		}
		else
		{
			debug("---( "+file+" )---");
			IWalker walker = new IWalker() {
				int ov = 0;
				public void onNode(CCFNode node) {
					if (node instanceof CCFIRCode) {
						CCFIRCode ir = (CCFIRCode)node;
						debug((ir.hasUDB() ? "2":"1")+": "+ir.getCode());
					}
				}
			};
			new CCFWalker(ccf).walk(walker);
		}
	}

	public static void irex(CCF ccf, IRDatabase dbi)
		throws IOException
	{
		final IRDatabase db = dbi;
		final CCF c = ccf;

		new CCFWalker(ccf).walk(new IWalker() {
			private CCFDevice dev;
			private CCFPanel pan;
			private CCFButton btn;
			private IRRemote remote;
			public void onNode(CCFNode node) {
				if (node instanceof CCFButton) {
					btn = (CCFButton)node;
				}
				else
				if (node instanceof CCFPanel) {
					pan = (CCFPanel)node;
				}			
				else
				if (node instanceof CCFDevice) {
					dev = (CCFDevice)node;
					CCFDevice dev = (CCFDevice)node;
					remote = new IRRemote("unknown","unknown",dev.getName());
					db.add(remote);
				}
				else
				if (node instanceof CCFActionList) {
					processActions((CCFActionList)node);
				}
			}

			private void processActions(CCFActionList list) {
				if (list == null) {
					return;
				}
				CCFAction a[] = list.getActions();
				for (int i=0; a != null && i<a.length; i++) {
					if (a[i] instanceof ActionIRCode) {
						CCFIRCode ir = ((ActionIRCode)a[i]).getIRCode();
						try {
							Pronto p = new Pronto(ir.getCode(),c.usesUDB() ? Pronto.VERSION2 : Pronto.VERSION1);
							String nm = pan.getName()+"-"+btn.getName();
							p.setName(nm);
							remote.add(p);
						} catch (Exception ex) { }
					}
				}
			}
		});

		db.write();
	}

	private static void debug(String m)
	{
		System.out.println(m);
	}

	// ---( private instance methods )---
	private boolean process(CCFNode node)
	{
		return node != null && (walker2 != null ? walker2.processNode(node) : true);
	}

	private void walk(CCF ccf)
	{
		if (ccf == null)
		{
			return;
		}
		walk(ccf.getFirstHomeDevice());
		walk(ccf.getFirstDevice());
		walk(ccf.getFirstMacroDevice());
		walk(ccf.getMacroPanel());
	}

	private void walk(CCFDevice dev)
	{
		if (!process(dev))
		{
			return;
		}
		walker.onNode(dev);
		CCFHardKey key[] = dev.getHardKeys();
		for (int i=0; i<key.length; i++)
		{
			walk(key[i].getActionList());
		}
		walk(dev.getFirstPanel());
		walk(dev.getNextDevice());
	}

	private void walk(CCFPanel panel)
	{
		if (!process(panel))
		{
			return;
		}
		walker.onNode(panel);
		walk(panel.getChildren());
		walk(panel.getNextPanel());
	}

	private void walk(CCFChild c[])
	{
		if (c == null || c.length == 0)
		{
			return;
		}
		for (int i=0; i<c.length; i++)
		{
			walk(c[i]);
		}
	}

	private void walk(CCFChild child)
	{
		if (!process(child))
		{
			return;
		}
		walker.onNode(child);
		walk(child.getButton());
		walk(child.getFrame());
	}

	private void walk(CCFButton button)
	{
		if (!process(button))
		{
			return;
		}
		walker.onNode(button);
		walker.onNode(button.iconIU);
		walker.onNode(button.iconIS);
		walker.onNode(button.iconAU);
		walker.onNode(button.iconAS);
		walk(button.getActionList());
	}

	private void walk(CCFFrame frame)
	{
		if (!process(frame))
		{
			return;
		}
		walker.onNode(frame);
		walker.onNode(frame.icon);
		walk(frame.getChildren());
	}

	private void walk(CCFActionList list)
	{
		if (!process(list))
		{
			return;
		}
		walker.onNode(list);
		CCFAction a[] = list.getActions();
		for (int i=0; a != null && i<a.length; i++)
		{
			walk(a[i]);
		}
	}

	private void walk(CCFAction action)
	{
		if (!process(action))
		{
			return;
		}
		walker.onNode(action);
		if (action.getActionType() == CCFAction.ACT_IRCODE)
		{
			walk((CCFIRCode)action.action2);
		}
	}

	private void walk(CCFIRCode code)
	{
		if (!process(code))
		{
			return;
		}
		walker.onNode(code);
	}

	// ---( Constructors )---
	public CCFWalker(CCF ccf)
	{
		this.ccf = ccf;
	}

	// ---( instance fields )---
	private CCF ccf;
	private IWalker walker;
	private IWalker2 walker2;

	// ---( instance methods )---
	/**
	 * Initiate a CCF tree walk.
	 *
	 * @param walker object to be notified as nodes are walked.
	 */
	public void walk(IWalker walker)
	{
		this.walker = walker;
		if (walker instanceof IWalker2)
		{
			this.walker2 = (IWalker2)walker;
		}
		walk(ccf);
	}

	/**
	 * Initiate a CCF tree walk from a specific node.
	 *
	 * @param walker object to be notified as nodes are walked.
	 * @param node object to use as the root of the walk.
	 */
	public void walk(IWalker walker, CCFNode node)
	{
		this.walker = walker;
		if (walker instanceof IWalker2)
		{
			this.walker2 = (IWalker2)walker;
		}
		Class nclass = node.getClass();
		if (nclass == CCFDevice.class)     { walk((CCFDevice)node); }     else
		if (nclass == CCFPanel.class)      { walk((CCFPanel)node); }      else
		if (nclass == CCFFrame.class)      { walk((CCFFrame)node); }      else
		if (nclass == CCFButton.class)     { walk((CCFButton)node); }     else
		if (nclass == CCFChild.class)      { walk((CCFChild)node); }      else
		if (nclass == CCFActionList.class) { walk((CCFActionList)node); } else
		if (nclass == CCFAction.class)     { walk((CCFAction)node); }     else
		{
			// no match
		}
	}
}


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
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.awt.Dimension;

/**
 * The starting point for manipulating Pronto CCF files.
 * It is important to note that several of the data structures use
 * linked lists. The API's expose getters and setters for the first
 * element in the list, but it is up to you to maintain the integrity
 * of the lists. When you replace the first item in the list, you are
 * effectively replacing the entire list.
 */
public class CCF implements ICCFProvider
{
	// ---( static fields )---
	static Debug debug = Debug.getInstance("ccf");

	// ---( static methods )---

	// ---( constructors )---
	/**
	 * Create new in-memory CCF File for TS1000.
	 */
	public CCF()
	{
		this((ProntoModel)null);
	}

	public CCF ccf()
	{
		return this;
	}

	/**
	 * Create new in-memory CCF File.
	 */
	public CCF(ProntoModel model)
	{
		this(new CCFHeader());
		conformTo(model);
	}

	// used internally
	CCF(CCFHeader header)
	{
		if (header == null)
		{
			throw new IllegalArgumentException("header cannot be null");
		}
		this.header = header;
		this.useRemoteCapability = true;
		checkPanels();
	}

	CCFHeader header()
	{
		return header;
	}

	// ---( instance fields )---
	private CCFNodeState state;
	private CCFHeader header;
	private ITaskStatus status;
	private ProntoModel conforms;
	private boolean useRemoteCapability;

	// ---( instance methods )---
	public boolean usesUDB()
	{
		return header.hasUDB();
	}

	public boolean isColor()
	{
		return header.hasColor();
	}

	public boolean usesRemoteCapability()
	{
		return useRemoteCapability;
	}

	public void setUseRemoteCapability(boolean tf)
	{
		useRemoteCapability = tf;
	}

	public CCFNodeState getLastNodeState()
	{
		return state;
	}

	public CCFIRCode createIRCode(String code)
	{
		return new CCFIRCode(header, usesUDB() ? "0000 0000 0000 "+code : code);
	}

	public void setScreenSize(int w, int h)
	{
		header.setScreenSize(w, h);
	}

	public Dimension getScreenSize()
	{
		return getScreenSize(getConformsMatch()[0].getScreenSize());
	}

	public Dimension getScreenSize(Dimension def)
	{
		return header.getScreenSize(def);
	}

	public CCFColor getTransparentColor()
	{
		return header.getTransparentColor();
	}
	
	public void setTransparentColor(CCFColor color)
	{
		header.setTransparentColor(color);
	}

	public boolean isTransparentColor(CCFColor color)
	{
		return header.isTransparentColor(color);
	}

	public Dimension getScreenSize(ProntoModel def)
	{
		ProntoModel pm = getConformsTo(def);
		if (pm.isCustom())
		{
			return getScreenSize(pm.getScreenSize());
		}
		else
		{
			return pm.getScreenSize();
		}
	}

	/**
	 * Enforce that the CCF will conform to the requirements
	 * of a specific Pronto/Marantz model. This ensures that
	 * the remote will not report an 'Invalid Configuration'
	 * when the new image is downloaded.
	 */
	public void conformTo(ProntoModel model)
	{
		conforms = model;
		if (model != null)
		{
			conformTo(model.getCapability());
			checkPanels();
		}
	}

	private void checkPanels()
	{
		if (header.isMarantz())
		{
			if (getDeviceByName("SCROLL UP") == null)
			{
				appendHomeDevice(createDevice("SCROLL UP"));
			}
			if (getDeviceByName("SCROLL DOWN") == null)
			{
				appendHomeDevice(createDevice("SCROLL DOWN"));
			}
		}
		else
		if (header.isCustom())
		{
			CCFPanel mp = header.macroPanel;
			mp.setName("masterTemplate");
			if (mp.next == null)
			{
				mp.next = new CCFPanel("deviceTemplate", header);
			}
			if (mp.next.next == null)
			{
				mp.next.next = new CCFPanel("macroTemplate", header);
			}
			Dimension d = new Dimension(240,320);
			if (getScreenSize(d) == d)
			{
				setScreenSize(d.width, d.height);
			}
		}
	}

	public ProntoModel getConformsTo(ProntoModel def)
	{
		if (conforms != null)
		{
			return conforms;
		}
		ProntoModel m[] = ProntoModel.getModelByCapability(header.capability);
		if (m.length > 0)
		{
			for (int i=0; i<m.length; i++)
			{
				if (m[i] == def)
				{
					conforms = m[i];
					return conforms;
				}
			}
			conforms = m[0];
			return conforms;
		}
		return def;
	}

	public ProntoModel getModel()
	{
		return getConformsMatch()[0];
	}

	public ProntoModel[] getConformsMatch()
	{
		ProntoModel m[] = ProntoModel.getModelByCapability(header.capability);
		if (m.length > 0)
		{
			return m;
		}
		return new ProntoModel[] { ProntoModel.getModel(ProntoModel.TS1000) };
	}

	void conformTo(int capability)
	{
		header.setCapability(capability);
		resolveVersion();
	}

	/**
	 * Read CCF image from file.
	 *
	 * @param file name of file to read
	 */
	public void load(String file)
		throws IOException
	{
		state = new CCFNodeState(header, status);
		state.decodeFromFile(file);
		checkPanels();
	}

	/**
	 * Decode CCF from it's raw byte representation.
	 */
	public void decode(byte b[])
	{
		decode(b, status);
	}

	private void decode(byte b[], ITaskStatus status)
	{
		state = new CCFNodeState(header, status);
		state.decodeFromBytes(b);
		checkPanels();
	}

	/**
	 * Read CCF image from Pronto.
	 */
	public void loadFromPronto()
		throws Exception
	{

		decode(loadBytesFromPronto());
	}

	/**
	 * Read raw CCF image from Pronto.
	 */
	public byte[] loadBytesFromPronto()
		throws Exception
	{
		return getComm().getCCF(status);
	}

	/**
	 * Save CCF image to named file.
	 *
	 * @param file name of file to write image to
	 */
	public void save(String file)
		throws IOException
	{
		state = new CCFNodeState(header, status);
		state.encodeToFile(file);
	}

	/**
	 * Encode the ccf into it's raw byte representation.
	 */
	public byte[] encode()
		throws IOException
	{
		state = new CCFNodeState(header, status);
		return state.encodeToBytes();
	}

	/**
	 * Save CCF image to Pronto.
	 */
	public void saveToPronto()
		throws Exception
	{
		Comm c = Comm.scanForPronto(status);
		if (c != null)
		{
			throw new IOException("Unable to locate Pronto");
		}
		saveToPronto(c);
	}

	/**
	 * Save CCF image to Pronto.
	 */
	public void saveToPronto(Comm c)
		throws Exception
	{
		status(0, "Check Pronto Capability");
		if (usesRemoteCapability())
		{
			debug.log(2, "using remote's capability");
			int rpos = c.getPossible();
			if (rpos != header.capability)
			{
				if (!Util.confirmDialog(
					"Remote type mismatch",
					"This CCF and the remote's capabilities do not match.\n"+
					"Proceed with download anyway?"))
				{
					return;
				}
			}
			header.setCapability(rpos);
		}
		status(5, "Update CCF For Pronto");
		resolveVersion();
		status(10, "Saving to Pronto");
		ITaskStatus os = status;
		status = new ScopeTask(status, 10, 15);
		byte b[] = encode();
		if (b.length > conforms.getMemory())
		{
			throw new CCFException("Download aborted. CCF Size exceeds Remote's memory");
		}
		status = os;
		c.setCCF(b, new ScopeTask(status, 15, 100));
	}

	private void status(int pct, String msg)
	{
		if (status != null)
		{
			status.taskStatus(pct, msg);
		}
	}

	/**
	 * Return the first Home Device.
	 */
	public CCFDevice getFirstHomeDevice()
	{
		return header.firstHome;
	}

	/**
	 * Set the first Home Device.
	 */
	public void setFirstHomeDevice(CCFDevice dev)
	{
		header.firstHome = dev;
		if (dev != null)
		{
			dev.buildTree(header);
		}
	}

	/**
	 * Return the first regular Device.
	 */
	public CCFDevice getFirstDevice()
	{
		return header.firstDevice;
	}

	/**
	 * Return the first Device with the specified name.
	 *
	 * @param name name of device to locate.
	 */
	public CCFDevice getDeviceByName(String name)
	{
		CCFDevice srch = null;
		if ((srch = searchListForName(header.firstHome, name)) != null) { return srch; }
		if ((srch = searchListForName(header.firstDevice, name)) != null) { return srch; }
		if ((srch = searchListForName(header.firstMacro, name)) != null) { return srch; }
		return srch;
	}

	private CCFDevice searchListForName(CCFDevice first, String name)
	{
		while (first != null)
		{
			if (first.name != null && first.name.equals(name))
			{
				return first;
			}
			first = first.getNextDevice();
		}
		return null;
	}

	/**
	 * Create a new Device
	 */
	public CCFDevice createDevice()
	{
		return header.createDevice();
	}

	/**
	 * Create a new Device
	 */
	public CCFDevice createDevice(String name)
	{
		CCFDevice dev = header.createDevice();
		dev.setName(name);
		return dev;
	}

	/**
	 * Set the first regular Device.
	 */
	public void setFirstDevice(CCFDevice dev)
	{
		header.firstDevice = dev;
		if (dev != null)
		{
			dev.buildTree(header);
		}
	}

	/**
	 * Return the first macro Device.
	 */
	public CCFDevice getFirstMacroDevice()
	{
		return header.firstMacro;
	}

	/**
	 * Set the first macro Device.
	 */
	public void setFirstMacroDevice(CCFDevice dev)
	{
		header.firstMacro = dev;
		if (dev != null)
		{
			dev.buildTree(header);
		}
	}

	/**
	 * Get macro edit panel.
	 */
	public CCFPanel getMacroPanel()
	{
		return header.macroPanel;
	}

	/**
	 * Get custom master template.
	 */
	public CCFPanel getMasterTemplate()
	{
		return header.masterTemplate();
	}

	/**
	 * Get custom device template.
	 */
	public CCFPanel getDeviceTemplate()
	{
		return header.deviceTemplate();
	}

	/**
	 * Get custom macro template.
	 */
	public CCFPanel getMacroTemplate()
	{
		return header.macroTemplate();
	}

	/**
	 * Add a device to the end of the home device list.
	 */
	// TODO: trouble - appends panels to same-name devices. merge option.
	public void appendHomeDevice(CCFDevice dev)
	{
		if (dev == null)
		{
			return;
		}
		CCFDevice fd = findByName(header.firstHome, dev.getName());
		if (fd != null)
		{
			CCFPanel p = dev.getFirstPanel();
			while (p != null)
			{
				fd.addPanel((CCFPanel)p.getClone());
				p = p.getNextPanel();
			}
		}
		else
		if (!append(header.firstHome, dev))
		{
			header.firstHome = dev;
		}
	}

	/**
	 * Add a device to the end of the normal device list.
	 */
	public void appendDevice(CCFDevice dev)
	{
		if (!append(header.firstDevice, dev))
		{
			header.firstDevice = dev;
		}
	}

	/**
	 * Add a device to the end of the macro device list.
	 */
	public void appendMacroDevice(CCFDevice dev)
	{
		if (!append(header.firstMacro, dev))
		{
			header.firstMacro = dev;
		}
	}

	public void setNotify(ITaskStatus status)
	{
		this.status = status;
	}

	/**
	 * Merge another CCF into this one by appending
	 * all devices from the merged CCF.
	 */
	public void merge(CCF c)
	{
		CCFPanel ep = header.getEggDVD();
		if (ep != null && c.header.getEggDVD() != null)
		{
			mergeEggStreams(c, ep);
		}
		CCFDevice dev = c.getFirstHomeDevice();
		appendHomeDevice(dev);
		while (dev != null)
		{
			dev.setParent(header);
			dev = dev.getNextDevice();
		}
		dev = c.getFirstDevice();
		appendDevice(dev);
		while (dev != null)
		{
			dev.setParent(header);
			dev = dev.getNextDevice();
		}
		dev = c.getFirstMacroDevice();
		appendMacroDevice(dev);
		while (dev != null)
		{
			dev.setParent(header);
			dev = dev.getNextDevice();
		}
		resolveVersion();
	}

	private void mergeEggStreams(CCF target, CCFPanel egg)
	{
		final CCF walk = target;
		final CCFPanel eggDVD = egg;
		final CCFPanel oldEggDVD = target.header.getEggDVD();
		final CCFButton eggButtons[] = egg.getButtons();
		new CCFWalker(walk).walk(new IWalker() {
			public void onNode(CCFNode node) {
				if (node instanceof CCFActionList) {
					boolean changed = false;
					CCFAction a[] = ((CCFActionList)node).getActions();
					for (int i=0; a != null && i<a.length; i++)
					{
						if (a[i] instanceof ActionAliasButton)
						{
							ActionAliasButton aab = (ActionAliasButton)a[i];
							CCFButton b = aab.getButton();
							CCFPanel p = b.getParentPanel();
							if (p == oldEggDVD)
							{
								for (int j=0; j<eggButtons.length; j++)
								{
									if (stringEquals(eggButtons[j].name, b.name))
									{
										aab.setButton(eggButtons[j]);
										changed = true;
										break;
									}
								}
							}
						}
					}
					if (changed)
					{
						((CCFActionList)node).setActions(a);
					}
				}
			}
		});
		oldEggDVD.delete();
	}

	private void resolveVersion()
	{
		final boolean udb = header().hasUDB();
		final boolean color = header().hasColor();
		final boolean newmarantz = header().isNewMarantz();
		final CCFColor map[] = header().getColorMap();
		final int rgb[] = new int[] {
			map[0].getColorIndex(),
			map[1].getColorIndex(),
			map[2].getColorIndex(),
			map[3].getColorIndex(),
		};
		// post-process on merge
		new CCFWalker(this).walk(new IWalker() {
			public void onNode(CCFNode node) {
				if (node != null)
				{
					node.setHeader(header);
				}
				if (node instanceof CCFIcon) {
					if (color) {
						((CCFIcon)node).convertToColor();
					} else {
						((CCFIcon)node).convertToGray();
					}
				} else
				if (node instanceof CCFButton) {
					CCFButton b = (CCFButton)node;
					if (color) {
						b.convertToColor();
					} else {
						b.convertToGray();
					}
				} else
				if (node instanceof CCFFrame) {
					CCFFrame f = (CCFFrame)node;
					if (color) {
						f.convertToColor();
					} else {
						f.convertToGray();
					}
				} else
				if (node instanceof CCFIRCode) {
					((CCFIRCode)node).setUDB(udb);
				}
				/*
				} else
				if (node instanceof CCFActionList) {
					CCFAction a[] = ((CCFActionList)node).getActions();
					if (a != null)
					{
						for (int i=0; i<a.length; i++)
						{
							switch (a[i].type)
							{
								case CCFAction.ACT_JUMP_PANEL:
									if (newmarantz)
									{
										a[i].type = CCFAction.ACT_MARANTZ_JUMP;
									}
									break;
								case CCFAction.ACT_MARANTZ_JUMP:
									if (!newmarantz)
									{
										a[i].type = CCFAction.ACT_JUMP_PANEL;
									}
									break;
							}
						}
					}
				}
				*/
			}
		});
	}

	/**
	 * Return the version identifier.
	 */
	public String getVersionString()
	{
		return header.version;
	}

	/**
	 * Set the version identifier.
	 */
	public void setVersionString(String ver)
	{
		header.version = ver;
	}

	/**
	 * Return the modification date.
	 */
	public Date getModifiedDate()
	{
		Calendar c = Calendar.getInstance();
		c.set(c.YEAR, header.year);
		c.set(c.MONTH, header.month - 1);
		c.set(c.DAY_OF_MONTH, header.day);
		c.set(c.HOUR, header.hour);
		c.set(c.MINUTE, header.minute);
		c.set(c.SECOND, header.seconds);
		return c.getTime();
	}

	/**
	 * Set the modification date.
	 */
	public void setModifiedDate(Date date)
	{
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		header.year = c.get(c.YEAR);
		header.month = c.get(c.MONTH) + 1;
		header.day = c.get(c.DAY_OF_MONTH);
		header.hour = c.get(c.HOUR);
		header.minute = c.get(c.MINUTE);
		header.seconds = c.get(c.SECOND);
	}

	/**
	 * Update the modification date.
	 */
	public void setModified()
	{
		setModifiedDate(new Date());
	}

	/**
	 * Returns true if this configuration is a factory default.
	 */
	public boolean isFactoryCCF()
	{
		return header.isFactoryCCF();
	}

	/**
	 * Returns true if this configuration is read only.
	 */
	public boolean isConfigReadOnly()
	{
		return header.isConfigReadOnly();
	}

	/**
	 * Returns true if the home panel is read only.
	 */
	public boolean isHomeReadOnly()
	{
		return header.isHomeReadOnly();
	}

	/**
	 * Sets the configuration as factory default.
	 *
	 * @param flag new status of the factory flag
	 */
	public void setFactoryCCF(boolean flag)
	{
		header.setFactoryCCF(flag);
	}

	/**
	 * Sets the configuration read only flag.
	 *
	 * @param flag new status of the config read only flag
	 */
	public void setConfigReadOnly(boolean flag)
	{
		header.setConfigReadOnly(flag);
	}

	/**
	 * Sets the home panel read only flag.
	 *
	 * @param flag new status of the home panel read only flag
	 */
	public void setHomeReadOnly(boolean flag)
	{
		header.setHomeReadOnly(flag);
	}

	// ---( interface methods )---
	public String toString()
	{
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		return "CCF("+header.version+", mod="+df.format(getModifiedDate())+
			", hro="+isHomeReadOnly()+", cro="+isConfigReadOnly()+
			", cap="+header.capability+", ap="+header.attrPos+")";
	}

	// ---( tonto methods )---
	void loadFromPronto(Comm c)
		throws Exception
	{
		byte b[] = c.getCCF(new ScopeTask(status,0,90));
		decode(b, new ScopeTask(status,90,100));
	}

	// ---( internal methods )---
	private Comm getComm()
		throws Exception
	{
		Comm c = Comm.scanForPronto(null);
		if (c == null)
		{
			throw new IOException("Unable to locate Pronto");
		}
		return c;
	}

	private boolean append(CCFDevice root, CCFDevice next)
	{
		if (root == null)
		{
			return false;
		}
		if (next == null)
		{
			return true;
		}
		while (root.next != null)
		{
			root = root.getNextDevice();
		}
		root.setNextDevice(next);
		next.setParent(header);
		return true;
	}

	private CCFDevice findByName(CCFDevice root, String name)
	{
		while (root != null)
		{
			String nm = root.getName();
			if (nm == name || (name != null && nm != null && nm.equals(name)))
			{
				return root;
			}
			root = root.getNextDevice();
		}
		return null;
	}

	private boolean stringEquals(String s1, String s2)
	{
		return (s1 != null && s2 != null) && (s1 == s2 || s1.equals(s2));
	}

}


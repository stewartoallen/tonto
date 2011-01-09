/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package javax.comm;

import java.io.*;
import java.util.Enumeration;
import java.util.Vector;

public class CommPortIdentifier
{
	// ---( public static fields )---
    public static final int PORT_PARALLEL = 2;
    public static final int PORT_SERIAL = 1;

	// ---( private static fields )---
	private static CommPortIdentifier rootPort;
	private static boolean valid = true;

	// ---( static initializer )---
	static
	{
		String osname = System.getProperty("os.name");
		String init = osname.replace(' ', '_');
		if (osname.toLowerCase().indexOf("windows") >= 0)
		{
			init = "Windows";
		}
		try
		{
			Class cz = Class.forName("javax.comm.Driver"+init);
			if (cz != null)
			{
				((Runnable)cz.newInstance()).run();
			}
		}
		catch (Throwable ex)
		{
			System.err.println("Unable to find Comm driver for '"+init+"'");
		}
	}

	static void setInvalid()
	{
		valid = false;
	}

	// ---( private instance fields )---
	private int portType;
	private String portName;
	private CommDriver driver;
	private CommPortIdentifier nextPort;
	private Vector portListeners = new Vector(1,1);

	private boolean owned;
	private String ownerApp;
	private CommPort ownerPort;

	// ---( private constructor )---
	private CommPortIdentifier(int portType, String portName, CommDriver driver)
	{
		this.portType = portType;
		this.portName = portName;
		this.driver = driver;
		this.owned = false;
	}

	// ---( pacakge methods )---
	void dropOwner()
	{
		// TODO : implement ownership request and contention signals
		owned = false;
		ownerApp = null;
		ownerPort = null;
	}

	// ---( public methods )---
    public static void addPortName(
		String portName, int portType, CommDriver driver)
    {
		// TODO : security check
		if (rootPort == null)
		{
			rootPort = new CommPortIdentifier(portType, portName, driver);
		}
		else
		{
			CommPortIdentifier walk = rootPort;
			for (; walk.nextPort != null; walk = walk.nextPort)
				;
			walk.nextPort = new CommPortIdentifier(portType, portName, driver); 
		}
    }

    public void addPortOwnershipListener(
		CommPortOwnershipListener listener)
    {
		if (!portListeners.contains(listener))
		{
			portListeners.addElement(listener);
		}
    }

    public String getCurrentOwner()
    {
		if (owned)
		{
			return ownerApp;
		}
		else
		{
			return null;
		}
    }

	public String toString()
	{
		return "CommPort("+portName+")";
	}

    public String getName()
    {
		return portName;
    }

    public static CommPortIdentifier getPortIdentifier(CommPort port)
        throws NoSuchPortException
    {
		CommPortIdentifier walk = rootPort;
		while (walk != null)
		{
			if (walk.ownerPort == port)
			{
				return walk;
			}
			walk = walk.nextPort;
		}
		throw new NoSuchPortException();
    }

    public static CommPortIdentifier getPortIdentifier(String portName)
        throws NoSuchPortException
    {
		CommPortIdentifier walk = rootPort;
		while (walk != null)
		{
			if (walk.portName == portName || walk.portName.equals(portName))
			{
				return walk;
			}
			walk = walk.nextPort;
		}
		throw new NoSuchPortException();
    }

    public static Enumeration getPortIdentifiers()
    {
		if (!valid)
		{
			return null;
		}
		return new Enumeration() {
			CommPortIdentifier root = rootPort;

			public boolean hasMoreElements()
			{
				return root != null;
			}
			public Object nextElement()
			{
				Object ret = root;
				root = root.nextPort;
				return ret;
			}
		};
    }

    public int getPortType()
    {
		return portType;
    }

    public boolean isCurrentlyOwned()
    {
		return owned;
    }

    public CommPort open(FileDescriptor fd)
        throws UnsupportedCommOperationException
    {
		throw new UnsupportedCommOperationException();
    }

    public CommPort open(String appname, int timeout)
        throws PortInUseException
    {
		// TODO : implement ownership request, contention (signals) & timeout
		CommPort port = driver.getCommPort(portName, portType);
		owned = true;
		ownerPort = port;
		ownerApp = appname;
		return port;
    }

    public void removePortOwnershipListener(
		CommPortOwnershipListener listener)
    {
		portListeners.removeElement(listener);
    }
}


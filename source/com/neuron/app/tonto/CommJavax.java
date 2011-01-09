/*
 * Copyright 2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import javax.comm.*;
import java.util.Vector;
import java.util.Enumeration;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class CommJavax implements ICommSource
{
	static Debug debug = Debug.getInstance("comm");

	public ICommSerialPortID[] getSerialPorts()
	{
		Vector v = new Vector();
		Enumeration e = CommPortIdentifier.getPortIdentifiers();
		if (e == null)
		{
			return null;
		}
		while (e.hasMoreElements())
		{
			CommPortIdentifier id = (CommPortIdentifier)e.nextElement();
			if (id.getPortType() == id.PORT_SERIAL)
			{
				v.add(new PortID(id));
			}
		}
		PortID pi[] = new PortID[v.size()];
		v.copyInto(pi);
		return pi;
	}

	public ICommSerialPortID getSerialPort(String name)
	{
		ICommSerialPortID id[] = getSerialPorts();
		for (int i=0; i<id.length; i++)
		{
			if (id[i].getName().equals(name))
			{
				return id[i];
			}
		}
		throw new IllegalArgumentException("Port '"+name+"' not found");
	}

	// ---( inner class PortID )---
	class PortID implements ICommSerialPortID
	{
		private CommPortIdentifier id;

		PortID(CommPortIdentifier id)
		{
			this.id = id;
		}

		public String getName()
		{
			return id.getName();
		}

		public ICommSerialPort open()
			throws IOException
		{
			try
			{
				return new Port((SerialPort)id.open("Tonto", 1500));
			}
			catch (Exception ex)
			{
				throw new IOException(ex.getMessage());
			}
		}

		public boolean isCurrentlyOwned()
		{
			return id.isCurrentlyOwned();
		}

		public String toString()
		{
			return id.toString();
		}
	}

	// ---( inner class PortID )---
	class Port implements ICommSerialPort
	{
		private SerialPort port;

		Port(SerialPort port)
			throws IOException
		{
			this.port = port;
			try
			{
				port.setSerialPortParams(115200, port.DATABITS_8, port.STOPBITS_1, port.PARITY_NONE);
				port.setInputBufferSize(4096);
				port.enableReceiveTimeout(1500);
				//port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
				port.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);
				debug.log(3,"port="+port.getName()+"@"+port.getBaudRate()+","+port.getDataBits()+"-"+port.getParity()+"-"+port.getStopBits());
			}
			catch (Exception ex)
			{
				throw new IOException(ex.getMessage());
			}
		} 

		public String getName()
		{
			return port.getName();
		}

		public InputStream getInputStream()
			throws IOException
		{
			return port.getInputStream();
		}

		public OutputStream getOutputStream()
			throws IOException
		{
			return port.getOutputStream();
		}

		public void sendBreak(int len)
			throws IOException
		{
			port.sendBreak(len);
		}

		public void close()
		{
			port.close();
		}

		public String toString()
		{
			return port.toString();
		}
	}
}


/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package javax.comm;

// ---( imports )---
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FilenameFilter;
import java.util.StringTokenizer;
import java.util.TooManyListenersException;

public class DriverGenUnix implements Runnable, CommDriver
{
	// ---( static initializer )---
	static
	{
		try
		{
			/*
			System.out.println("start loading cygwin");
			System.loadLibrary("cygwin1");
			System.out.println("done loading cygwin");
			*/
		}
		catch (Throwable ex)
		{
			System.err.println("Unable to load Cygwin1 DLL");
			ex.printStackTrace();
		}
		try
		{
			System.loadLibrary("jnijcomm");
		}
		catch (Throwable ex)
		{
			System.err.println("Error loading JComm JNI library");
			ex.printStackTrace();
			CommPortIdentifier.setInvalid();
		}
	}
	
	// ---( Runnable interface methods )---
	public void run()
	{
		addPorts(System.getProperty("javax.comm.ports.serial"),
			CommPortIdentifier.PORT_SERIAL);
		addPorts(System.getProperty("javax.comm.ports.parallel"),
			CommPortIdentifier.PORT_PARALLEL);

		addPorts(getSerial(), CommPortIdentifier.PORT_SERIAL);
		addPorts(getParallel(), CommPortIdentifier.PORT_PARALLEL);
	}

	protected String[][] getSerial()
	{
		return new String[][] {
			{"/dev",     "ttyS"},
			{"/dev/usb", "ttyUSB"},
		};
	}

	protected String[][] getParallel()
	{
		return new String[][] { };
	}

	private void addPorts(String s[][], int type)
	{
		for (int i=0; i<s.length; i++)
		{
			addPorts(s[i][0], s[i][1], type);
		}
	}

	protected boolean skipPort(File file)
	{
		return false;
	}
	
	private void addPorts(String dir, String prefix, int type)
	{
		String f[] = new File(dir).list();
		if (f == null)
		{
			return;
		}
		for (int i=0; i<f.length; i++)
		{
			File test = new File(dir, f[i]);
			if (skipPort(test))
			{
				continue;
			}
			if (f[i].startsWith(prefix) && test.canRead() && test.canWrite())
			{
				CommPortIdentifier.addPortName(
					test.getAbsolutePath(), type, this);
			}
		}
	}

	private void addPorts(String portlist, int type)
	{
		if (portlist == null)
		{
			return;
		}
		StringTokenizer st = new StringTokenizer(portlist,",");
		while (st.hasMoreTokens())
		{
			File f = new File(st.nextToken());
			if (f.exists())
			{
				CommPortIdentifier.addPortName(
					f.getAbsolutePath(), type, this);
			}
		}
	}

	// ---( CommDriver interface methods )---
	public CommPort getCommPort(String portName, int portType)
	{
		if (portType == CommPortIdentifier.PORT_SERIAL)
		{
			return new Serial(portName);
		}
		else
		if (portType == CommPortIdentifier.PORT_SERIAL)
		{
			return new Parallel(portName);
		}
		else
		{
			throw new IllegalArgumentException("invalid port type");
		}
	}

	public void initialize()
	{
		// unused
	}

	// ---( native fd/io methods )---
	private native int available(int fd);

	private native void close(int fd);

	private native int read(int fd, byte b[], int off, int len, int time);

	private native void write(int fd, byte b[], int off, int len);

	// ---( Inner Classes )---
	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	private interface TimedIO
	{
		public int getTimeout();
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	private final class Serial extends SerialPort implements TimedIO
	{
		private int fd;
		private int timeout;

		Serial(String portName)
		{
			this.name = portName;
			this.fd = open(portName);
		}

		// ---( native methods )---
		private native int open(String port);

		private native void enableReceiveThreshold(int fd, int thresh);

		private native void enableReceiveTimeout(int fd, int time);

		private native int getReceiveThreshold(int fd);

		private native int getReceiveTimeout(int fd);

		private native int getBaudRate(int fd);

		private native int getDataBits(int fd);

		private native int getFlowControlMode(int fd);

		private native int getParity(int fd);

		private native int getStopBits(int fd);

		private native void sendBreak(int fd, int len);

		private native void setDTR(int fd, boolean state);

		private native void setFlowControlMode(int fd, int mode)
			throws UnsupportedCommOperationException;

		private native void setRTS(int fd, boolean state);

		private native void setSerialPortParams(
			int fd, int baud, int data, int stop, int parity)
			throws UnsupportedCommOperationException;

		// ---( TimedIO interface methods )---
		public int getTimeout()
		{
			return timeout;
		}

		// ---( CommPort interface methods )---
		public void close()
		{
			DriverGenUnix.this.close(fd);
			super.close();
		}

		public void disableReceiveFraming()
		{
			// not implemented
		}

		public void disableReceiveThreshold()
		{
			try { enableReceiveThreshold(0); } catch (Exception ex) { }
		}

		public void disableReceiveTimeout()
		{
			try { enableReceiveTimeout(0); } catch (Exception ex) { }
		}

		public void enableReceiveFraming(int framingByte)
			throws UnsupportedCommOperationException
		{
			// not implemented
		}

		public void enableReceiveThreshold(int thresh)
			throws UnsupportedCommOperationException
		{
			enableReceiveThreshold(fd, thresh);
		}

		public void enableReceiveTimeout(int recvTimeout)
			throws UnsupportedCommOperationException
		{
			timeout = recvTimeout;
			enableReceiveTimeout(fd, recvTimeout);
		}

		public int getInputBufferSize()
		{
			return 0;
		}

		public InputStream getInputStream()
			throws IOException
		{
			return new FDInputStream(fd, this);
		}

		public int getOutputBufferSize()
		{
			return 0;
		}

		public OutputStream getOutputStream()
			throws IOException
		{
			return new FDOutputStream(fd);
		}

		public int getReceiveFramingByte()
		{
			return 0;
		}

		public int getReceiveThreshold()
		{
			return getReceiveThreshold(fd);
		}

		public int getReceiveTimeout()
		{
			return getReceiveTimeout(fd);
		}

		public boolean isReceiveFramingEnabled()
		{
			return false;
		}

		public boolean isReceiveThresholdEnabled()
		{
			return getReceiveThreshold() > 0;
		}

		public boolean isReceiveTimeoutEnabled()
		{
			return getReceiveTimeout() > 0;
		}

		public void setInputBufferSize(int size)
		{
		}

		public void setOutputBufferSize(int size)
		{
		}

		// ---( SerialPort interface methods )---
		public void addEventListener(
			SerialPortEventListener serialporteventlistener)
			throws TooManyListenersException
		{
		}

		public int getBaudRate()
		{
			return getBaudRate(fd);
		}

		public int getDataBits()
		{
			return getDataBits(fd);
		}

		public int getFlowControlMode()
		{
			return getFlowControlMode(fd);
		}

		public int getParity()
		{
			return getParity(fd);
		}

		public int getStopBits()
		{
			return getStopBits(fd);
		}

		public boolean isCD()
		{
			return false;
		}

		public boolean isCTS()
		{
			return false;
		}

		public boolean isDSR()
		{
			return false;
		}

		public boolean isDTR()
		{
			return false;
		}

		public boolean isRI()
		{
			return false;
		}

		public boolean isRTS()
		{
			return false;
		}

		public void notifyOnBreakInterrupt(boolean state)
		{
		}

		public void notifyOnCarrierDetect(boolean state)
		{
		}

		public void notifyOnCTS(boolean state)
		{
		}

		public void notifyOnDataAvailable(boolean state)
		{
		}

		public void notifyOnDSR(boolean state)
		{
		}

		public void notifyOnFramingError(boolean state)
		{
		}

		public void notifyOnOutputEmpty(boolean state)
		{
		}

		public void notifyOnOverrunError(boolean state)
		{
		}

		public void notifyOnParityError(boolean state)
		{
		}

		public void notifyOnRingIndicator(boolean state)
		{
		}

		public void removeEventListener()
		{
		}

		public void sendBreak(int len)
		{
			sendBreak(fd, len);
		}

		public void setDTR(boolean state)
		{
		}

		public void setFlowControlMode(int mode)
			throws UnsupportedCommOperationException
		{
		}

		public void setRTS(boolean state)
		{
		}

		public void setSerialPortParams(
			int baud, int data, int stop, int parity)
			throws UnsupportedCommOperationException
		{
			setSerialPortParams(fd, baud, data, stop, parity);
		}
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	private final class Parallel extends ParallelPort
	{
		Parallel(String portName)
		{
			this.name = portName;
		}

		// ---( CommPort interface methods )---
		public void disableReceiveFraming()
		{
		}

		public void disableReceiveThreshold()
		{
		}

		public void disableReceiveTimeout()
		{
		}

		public void enableReceiveFraming(int framingByte)
			throws UnsupportedCommOperationException
		{
		}

		public void enableReceiveThreshold(int thresh)
			throws UnsupportedCommOperationException
		{
		}

		public void enableReceiveTimeout(int recvTimeout)
			throws UnsupportedCommOperationException
		{
		}

		public int getInputBufferSize()
		{
			return -1;
		}

		public InputStream getInputStream()
			throws IOException
		{
			return null;
		}

		public int getOutputBufferSize()
		{
			return -1;
		}

		public OutputStream getOutputStream()
			throws IOException
		{
			return null;
		}

		public int getReceiveFramingByte()
		{
			return -1;
		}

		public int getReceiveThreshold()
		{
			return -1;
		}

		public int getReceiveTimeout()
		{
			return -1;
		}

		public boolean isReceiveFramingEnabled()
		{
			return false;
		}

		public boolean isReceiveThresholdEnabled()
		{
			return false;
		}

		public boolean isReceiveTimeoutEnabled()
		{
			return false;
		}

		public void setInputBufferSize(int size)
		{
		}

		public void setOutputBufferSize(int size)
		{
		}

		// ---( ParallelPort interface methods )---
		public void addEventListener(
			ParallelPortEventListener listener)
			throws TooManyListenersException
		{
		}

		public int getMode()
		{
			return -1;
		}

		public int getOutputBufferFree()
		{
			return -1;
		}

		public boolean isPaperOut()
		{
			return false;
		}

		public boolean isPrinterBusy()
		{
			return false;
		}

		public boolean isPrinterError()
		{
			return false;
		}

		public boolean isPrinterSelected()
		{
			return false;
		}

		public boolean isPrinterTimedOut()
		{
			return false;
		}

		public void notifyOnBuffer(boolean notify)
		{
		}

		public void notifyOnError(boolean notify)
		{
		}

		public void removeEventListener()
		{
		}

		public void restart()
		{
		}

		public int setMode(int mode)
			throws UnsupportedCommOperationException
		{
			return -1;
		}

		public void suspend()
		{
		}
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	private final class FDInputStream extends InputStream
	{
		private int fd;
		private TimedIO timed;
		
		private FDInputStream(int fd, TimedIO timed)
		{
			this.fd = fd;
			this.timed = timed;
		}

		private void checkFD()
			throws IOException
		{
			if (fd <= 0)
			{
				throw new IOException("Stream was closed");
			}
		}

		public int available()
			throws IOException
		{
			return DriverGenUnix.this.available(fd);
		}

		public int read()
			throws IOException
		{
			checkFD();
			byte b[] = new byte[1];
			int read = 0;
			read = DriverGenUnix.this.read(fd, b, 0, 1, timed.getTimeout());
			if (read <= 0)
			{
				return -1;
			}
			return b[0] & 0xff;
		}

		public int read(byte b[])
			throws IOException
		{
			return read(b, 0, b.length);
		}

		public int read(byte b[], int off, int len)
			throws IOException
		{
			checkFD();
			return DriverGenUnix.this.read(fd, b, off, len, timed.getTimeout());
		}

		public void close()
			throws IOException
		{
			if (fd > 0)
			{
				DriverGenUnix.this.close(fd);
				fd = -1;
			}
		}
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	private final class FDOutputStream extends OutputStream
	{
		private int fd;
		
		private FDOutputStream(int fd)
		{
			this.fd = fd;
		}

		private void checkFD()
			throws IOException
		{
			if (fd <= 0)
			{
				throw new IOException("Stream was closed");
			}
		}

		public void write(int b)
			throws IOException
		{
			byte buf[] = { (byte)b };
			DriverGenUnix.this.write(fd, buf, 0, 1);
		}

		public void write(byte b[])
			throws IOException
		{
			write(b, 0, b.length);
		}

		public void write(byte b[], int off, int len)
			throws IOException
		{
			checkFD();
			DriverGenUnix.this.write(fd, b, off, len);
		}

		public void close()
			throws IOException
		{
			if (fd > 0)
			{
				DriverGenUnix.this.close(fd);
				fd = -1;
			}
		}
	}
}


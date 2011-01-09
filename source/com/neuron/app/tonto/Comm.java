/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.*;
import java.util.*;
import com.neuron.util.AnsiColor;
import com.neuron.io.ByteOutputBuffer;

class Comm implements IComm
{
	static Debug debug = Debug.getInstance("comm");

	// ---( static fields )---
	private final static byte[] ATN = { (byte)0 };
	private final static byte[] ATN2 = { (byte)24, (byte)24, (byte)24, (byte)24, (byte)24, };

	private final static int NUL = 0x00;
	private final static int ACK = 0x06;
	private final static int CR  = 0x0D;
	private final static int LF  = 0x0A;

	private final static int MODE_STANDBY = 1;
	private final static int MODE_WAITING = 2;
	private final static int MODE_NOPRONTO = 3;

	private static int loadDelay = 250;
	private static int commandDelay = 50;
	private static int attentionDelay = 50;

	//private static ICommSource source = Util.onWindows() ? (ICommSource)new CommGNU() : (ICommSource)new CommJavax();
	private static ICommSource source = new CommJavax();
	private static boolean onWindows98 = Util.onWindows98();

	// ---( instance fields )---
	private ICommSerialPortID comm;
	private ICommSerialPort port;

	private InputStream input;
	private OutputStream output;
	private int mode;
	private int CCFCapable[];
	private int CCFPossible = -1;
	private int CCFSize;
	private boolean isCCFDirty;
	private long lastCMDTime = 0;
	private String CCFDate;
	private String CCFTime;
	private OutputStream log;
	private boolean learning = false;
	private boolean sending = false;
	private StringBuffer logBuf;
	private Thread logThread;
	private Object learnLock = new Object();
	private long lastUpdate;

	// ---( constructors )---
	public Comm (String commPort)
		throws Exception
	{
		this(source.getSerialPort(commPort));
	}

	public Comm (ICommSerialPortID comm)
		throws Exception
	{
		this.comm = comm;
		open();
	}

	// ---( instance methods )---
	public void open()
		throws Exception
	{
		port = comm.open();
		input = port.getInputStream();
		output = port.getOutputStream();
	}

	public void reopen()
		throws Exception
	{
		close();
		try
		{
			open();
		}
		catch (Exception ex)
		{
			throw new IOException(ex.getMessage());
		}
	}

	public void close()
	{
		if (onWindows98)
		{
			if (output != null)
			{
				try {
					output.flush();
					output.close();
				} catch (Exception ex) { }
			}
			if (input != null)
			{
				try {
					input.close();
				} catch (Exception ex) { }
			}
		}
		if (port != null)
		{
			try {
				port.close();
			} catch (Exception ex) { }
		}
	}

	public boolean isProntoOK()
		throws Exception
	{
		drainInput();
		for (int i=0; i<2; i++)
		{
			send(ATN2);
			flush();
			switch (recv())
			{
				// case '.': // neo
				case '*':
				case '!':
				case '~':
					return true;
			}
			port.sendBreak(250);
			safeSleep(250);
			reopen();
		}
		return false;
	}

	public String getPortName()
	{
		return comm.getName();
	}

	private boolean sendReboot()
	{
		try
		{
			for (int i=0; i<3; i++)
			{
				send(ATN2);
				flush();
				if (recv() == '!')
				{
					send("reboot".getBytes());
					send(CR);
					flush();
					return true;
				}
			}
			return false;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return false;
		}
	}

	public void undeadPronto(String ccf, ITaskStatus status)
		throws IOException
	{
		File f = new File(ccf);
		byte b[] = readFile(f);

		if (sendReboot())
		{
			status.taskStatus(2, "Looking for Boot Message");
			findBytes("Boot".getBytes());
		}
		else
		{
			status.taskStatus(2, "Press RESET on Remote");
			findBytes("12\r\n14\r\n16\r\n".getBytes());
		}

		status.taskStatus(10, "Issuing Escape");
		send(27);
		flush();

		status.taskStatus(15, "Looking for CABERNET");
		findBytes("CABERNET> ".getBytes());
		status.taskStatus(20, "Issuing Download Command");

		send("dl ccf\r".getBytes());
		flush();

		for (int i=0; i<7; i++)
		{
			recv(); // drain 'dl ccf' echo
		}

		status.taskStatus(30, "Downloading new CCF");
		new Xmodem(this, new ScopeTask(status, 30,100)).sendFile(b);

		status.taskStatus(90, "Booting Remote");
		send("go\r".getBytes());
		flush();
		status.taskStatus(100, "Download Complete");
	}

	public void updateFirmware(Firmware fw, ITaskStatus status)
		throws IOException
	{
		updateFirmware(fw, null, status);
	}

	public void updateFirmware(Firmware fw, CCF ccf, ITaskStatus status)
		throws IOException
	{
		updateFirmware(fw, ccf, false, status);
	}

	public void updateFirmware(Firmware fw, CCF ccf, boolean force, ITaskStatus status)
		throws IOException
	{
		double sysFrm = 0.0;
		double appFrm = 0.0;

		String vs = fw.getDescription().replace(',',';').replace('-',';');
		StringTokenizer st = new StringTokenizer(vs, ";");
		while (st.hasMoreTokens())
		{
			String tok = st.nextToken().trim().toLowerCase();
			if (tok.startsWith("sys"))
			{
				sysFrm = getVersion(tok);
			}
			else
			if (tok.startsWith("app"))
			{
				appFrm = getVersion(tok);
			}
		}
		debug.log(0, "Firmware _SYS="+sysFrm+" _APP="+appFrm);

		byte intr[] = new byte[] { 0x1b, 0x00, 0x00, 0x0d, 0x0a };

		safeSleep(500);
		status.taskStatus(0, "Rebooting Pronto");
		if (sendReboot())
		{
			status.taskStatus(2, "Looking for Boot Message");
			findBytes("Boot".getBytes());
		}
		else
		{
			status.taskStatus(2, "Press RESET on Remote");
			findBytes("12\r\n14\r\n16\r\n".getBytes());
		}
		//findBytes("Booting.".getBytes());

		status.taskStatus(3, "Issuing Interrupt");
		send(intr);
		flush();

		status.taskStatus(4, "Looking for CABERNET");
		findBytes("CABERNET> ".getBytes());

		drainInput();

		double sysRem = 0.0;
		double appRem = 0.0;

		if (!force)
		{

		status.taskStatus(5, "Issuing Query");
		send("\000q\r\n".getBytes());
		flush();

		status.taskStatus(6, "Looking for Query echo");
		findBytes("q\n\r".getBytes());

		status.taskStatus(7, "Collecting Statistics");
		byte stat[] = collectUntil("CABERNET> ".getBytes());
		if (stat == null)
		{
			debug.log(0, "Error reading statistics (null)");
			stat = new byte[] { };
		}
		String stats = new String(stat);
		System.out.println("Pronto Statistics (((\n"+stats+"\n)))");

		st = new StringTokenizer(stats, "\n");
		while (st.hasMoreTokens())
		{
			String tok = st.nextToken().trim();
			if (tok.startsWith("_SYS") || tok.startsWith("_APP"))
			{
				StringTokenizer tok2 = new StringTokenizer(tok, ",");
				if (tok2.countTokens() > 3)
				{
					tok2.nextToken();
					tok2.nextToken();
					double vn = getVersion(tok2.nextToken().trim());
					if (tok.startsWith("_SYS"))
					{
						sysRem = vn;
					}
					else
					{
						appRem = vn;
					}
				}
			}
		}
		debug.log(0, "Pronto _SYS="+sysRem+" _APP="+appRem);

		if (!force && (sysRem <= 0 || appRem <= 0))
		{
			debug.log(0, "Error reading statistics");
			send(("\000go\r\n").getBytes());
			flush();
			throw new IOException("Unable to read remote's status");
		}

		}

		boolean upSys = force || (sysFrm > sysRem);
		boolean upApp = force || upSys || (appFrm > appRem);

		if (upSys || upApp)
		{
			status.taskStatus(9, "Upgrading: "+(upSys ? "_SYS" : "")+" "+(upApp ? "_APP" : ""));
		}
		else
		{
			status.taskStatus(9, "Firmware up to date");
		}

		/*
		if (ccf != null)
		{
			ccf.setFactoryCCF(true);
		}
		*/

		byte _sys[] = fw.get_SYS();
		byte _app[] = fw.get_APP();
		byte _ccf[] = (ccf != null ? ccf.encode() : fw.get_CCF());

		if (!force)
		{
		status.taskStatus(10, "Looking for CABERNET");
		findBytes("CABERNET> ".getBytes());
		drainInput(); // test fix for TSU1000 - chris
		}

		int pct = 10;
		int len = _ccf.length + (upApp ? _app.length : 0) + (upSys ? _sys.length : 0);
		debug.log(2, "total send: "+len);
		// ---( _SYS )---
		if (upSys)
		{
			int hi = pct+percent(_sys.length, len, 90);
			debug.log(2, "sys: pct="+pct+" hi="+hi+" len="+_sys.length);
			pct = updateSegment(status, "_SYS", _sys, pct, hi);
		}

		// ---( _APP )---
		if (upApp)
		{
			int hi = pct+percent(_app.length, len, 90);
			debug.log(2, "app: pct="+pct+" hi="+hi+" len="+_app.length);
			pct = updateSegment(status, "_APP", _app, pct, hi);
		}

		// ---( _CCF )---
		{
			int hi = pct+percent(_ccf.length, len, 90);
			debug.log(2, "ccf: pct="+pct+" hi="+hi+" len="+_ccf.length);
			pct = updateSegment(status, "_CCF", _ccf, pct, hi);
		}

		status.taskStatus(100, "Booting Pronto");
		send(("\000go\r\n").getBytes());
		flush();

		status.taskStatus(100, "Download Complete");
	}

	private int percent(int value, int total, int scale)
	{
		return ((value*100/total)*scale/100);
	}

	private int updateSegment(ITaskStatus status, String segName, byte seg[], int lo, int hi)
		throws IOException
	{
		drainInput();
		debug.log(0, "Sending "+segName+" Segment");
		status.taskStatus(lo, "Issuing Download Command");
		send("\000d\r\n".getBytes());
		flush();
		findBytes("d\n\r".getBytes());

		// look for two C's in a row
		int cnt = 0;
		for (int i=0; ;i++)
		{
			int rc = recv();
			if (rc == -1)
			{
				i += 5;
				continue;
			}
			else
			if (rc == 'C')
			{
				if (++cnt == 2)
				{
					break;
				}
			}
			else
			{
				cnt = 0;
			}
			if (i>25)
			{
				throw new IOException("Segment update for '"+segName+"' failed");
			}
		}

		status.taskStatus(lo, "Downloading "+segName);
		new Xmodem(this, new ScopeTask(status,lo,hi)).sendFile(seg);
		return hi;
	}

	private static double getVersion(String vstr)
	{
		vstr = vstr.trim().toLowerCase();
		double val = 0.0;
		int pos = vstr.indexOf("v");
		if (pos >= 0)
		{
			vstr = vstr.substring(pos+1,vstr.length());
			StringTokenizer st = new StringTokenizer(vstr, ".");
			double pow = 1.0;
			while (st.hasMoreTokens())
			{
				int tval = Integer.parseInt(st.nextToken());
				val += tval * pow;
				pow = pow / 100.0;
			}
		}
		return val;
	}

	final void findBytes(byte match[])
		throws IOException
	{
		int ch;
		int pos = 0;
		while (true)
		{
			ch = recv();
			if (ch != match[pos++])
			{
				pos = 0;
				continue;
			}
			if (pos == match.length)
			{
				break;
			}
		}
	}

	final byte[] collectUntil(byte match[])
		throws IOException
	{
		ByteOutputBuffer bob = new ByteOutputBuffer(4096);
		int ch;
		int pos = 0;
		int err = 0;
		while (true)
		{
			ch = recv();
			bob.write(ch);
			if (ch == -1)
			{
				if (err++ > 20)
				{
					return null;
				}
				continue;
			}
			if (ch != match[pos++])
			{
				pos = 0;
				err = 0;
				continue;
			}
			if (pos == match.length)
			{
				break;
			}
		}
		byte b[] = bob.toByteArray();
		byte r[] = new byte[b.length-match.length];
		System.arraycopy(b,0,r,0,r.length);
		return r;
	}

	public String queryPronto()
		throws IOException
	{
		for (int i=0; i<2; i++)
		{
			sendCMD("q ccf");
			String state = readString();
			if (state == null || state.length() <= 10)
			{
				debug.log(1, "query["+i+"] failed");
				safeSleep(500);
				continue;
			}
			debug.log(1, comm.getName()+" Reply '"+state+"'");
			StringTokenizer st = new StringTokenizer(state, " ");
			if (st.countTokens() < 4)
			{
				throw new IOException("Pronto inquiry failed");
			}
			isCCFDirty = (Integer.parseInt(st.nextToken()) != 0);
			CCFSize = Integer.parseInt(st.nextToken());
			CCFDate = st.nextToken();
			CCFTime = st.nextToken();
			drainInput();
			return state;
		}
		throw new IOException("Pronto not responding to query on "+comm.getName());
	}

	private int dehex(String hex)
	{
		return Integer.parseInt(hex.substring(2),16);
	}

	public void rebootPronto()
		throws IOException
	{
		sendCMD("reboot");
		drainInput();
	}

	public int[] getCCFCapable()
		throws IOException
	{
		for (int i=0; i<2; i++)
		{
			sendCMD("cap ccf");
			String cap = readString();
			if (cap == null || cap.length() < 6)
			{
				debug.log(1, "cap["+i+"] failed");
				continue;
			}
			debug.log(1, comm.getName()+" Reply '"+cap+"'");
			StringTokenizer st = new StringTokenizer(cap.substring(4));
			int ret[] = new int[st.countTokens()];
			for (int j=0; j<ret.length; j++)
			{
				ret[j] = dehex(st.nextToken());
			}
			CCFCapable = ret;
			drainInput();
			return CCFCapable;
		}
		throw new IOException("Pronto not responding to capability");
	}

	public int getCCFPossible()
		throws IOException
	{
		for (int i=0; i<2; i++)
		{
			sendCMD("possible ccf");
			String pos = readString();
			if (pos == null || pos.length() < 10)
			{
				debug.log(1, "pos["+i+"] failed");
				continue;
			}
			debug.log(1,comm.getName()+" Reply '"+pos+"' ("+pos.length()+")");
			CCFPossible = dehex(pos.substring(8));
			drainInput();
			return CCFPossible;
		}
		throw new IOException("Pronto not responding to possible");
	}

	private String readString()
		throws IOException
	{
		String string = null;
		char c[] = new char[128];
		for (int i=0; i<c.length; )
		{
			int ch = recv();
			if (ch == CR)
			{
				continue;
			}
			if (ch == -1 || ch == LF)
			{
				string = new String(c, 0, i);
				break;
			}
			c[i++] = (char)ch;
		}
		return string;
	}

	boolean isCCFDirty()
	{
		return isCCFDirty;
	}

	int getCCFSize()
	{
		return CCFSize;
	}

	String getCCFDate()
	{
		return CCFDate;
	}

	String getCCFTime()
	{
		return CCFTime;
	}

	int[] getCapable()
	{
		return CCFCapable;
	}

	int getPossible()
		throws IOException
	{
		if (CCFPossible < 0)
		{
			getCCFPossible();
		}
		return CCFPossible;
	}

	public String toString()
	{
		return "{"+(isCCFDirty?"dirty":"clean")+
			","+CCFSize+","+CCFDate+","+CCFTime+","+port.getName()+"}";
	}

	byte[] getCCF()
		throws Exception
	{
		return getCCF(null);
	}

	byte[] getCCF(ITaskStatus status)
		throws Exception
	{
		status.taskStatus(0, "Checking Pronto Status");
		queryPronto();

		for (int i=0; i<3; i++)
		{
			status.taskStatus(4, "Initiating Upload");
			sendCMD("ul ccf");
			safeSleep(loadDelay);
			status.taskStatus(6, "Upload in Progress");
			byte b[] = null;
			try
			{
				b = new Xmodem(this, new ScopeTask(status, 8, 100)).recvFile(CCFSize);
			}
			catch (RetryError err)
			{
				status.taskNotify("Command Failure. Retrying.");
				reopen();
				drainInput();
				safeSleep(1000);
				queryPronto();
				continue;
			}
			if (b != null)
			{
				if (b.length < CCFSize)
				{
					debug.log(0, "truncated ccf ("+b.length+" < "+CCFSize+")");
				}
				else
				if (b.length > CCFSize+128)
				{
					debug.log(0, "inflated ccf ("+b.length+" > "+CCFSize+")");
				}
				return b;
			}
			else
			{
				throw new IOException("No CCF Returned");
			}
		}
		throw new IOException("Too Many Retries");
	}

	public void waitLearning()
	{
		synchronized (learnLock)
		{
			if (!learning)
			{
				try
				{
					learnLock.wait();
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
			learning = false;
		}
	}

	void testIR(byte b[])
		throws Exception
	{
		sendCMD("irstart");
		new Xmodem(this, null).sendFile(b);
		send("irstop".getBytes());
		send(CR);
		flush();
		drainInput();
	}

	byte[] learnIR(ITaskStatus status)
		throws Exception
	{
		for (int i=0; i<1; i++)
		{

		try
		{
			synchronized (learnLock)
			{
				sendCMD("irlearn 5000");
				learning = true;
				learnLock.notify();
			}
			return new Xmodem(this, status).recvFile(CCFSize);
		}
		catch (RetryError err)
		{
			debug.log(0, "retry learnIR on err["+i+"]: "+err);
			reopen();
			drainInput();
			safeSleep(1000);
			queryPronto();
		}
		finally
		{
			synchronized (learnLock)
			{
				learning = false;
			}
		}

		}
		return null;
	}

	void setCCF(byte b[])
		throws IOException
	{
		setCCF(b, null);
	}

	void setCCF(byte b[], ITaskStatus status)
		throws IOException
	{
		/*
		status.taskStatus(0, "Checking Pronto Capability");
		getPossible();
		*/
		status.taskStatus(5, "Initiating Download");
		int reply = 0;
		for (int i=0; i<3; i++)
		{

			sendCMD("dl ccf");
			safeSleep(loadDelay);
			status.taskStatus(10, "Download in Progress");
			try
			{
				new Xmodem(this, new ScopeTask(status, 10, 100)).sendFile(b);
				status.taskNotify("Download Complete");
				return;
			}
			catch (Exception ex)
			{
				Tonto.debug(ex);
				status.taskNotify("Download Error... Retrying");
				safeSleep(500);
			}
		}
		throw new IOException("Unexpected response ("+reply+")");
	}

	private void drainInput()
		throws IOException
	{
		int count = 0;
		int drain;
		StringBuffer sb1 = new StringBuffer();
		StringBuffer sb2 = new StringBuffer();
		while ((drain = recv()) != -1)
		{
			sb1.append(Xmodem.hex(drain)+" ");
			sb2.append((char)drain);
			count++;
		}
		if (count > 0)
		{
			debug.log(2,"(---- drained ----)");
			debug.log(2,"num: "+sb1);
			debug.log(2,"raw: "+sb2);
			debug.log(2,"(-----------------)");
		}
	}

	private void sendCMD(String cmd)
		throws IOException
	{
		sendCMD(cmd, null);
	}

	public void sendAttention()
		throws IOException
	{
		send(ATN2);
		flush();
		safeSleep(100);
	}

	private void sendCMD(String cmd, String add)
		throws IOException
	{
		drainInput();
		safeSleep(commandDelay);
		int i = 0;
		for ( ; i<2; i++)
		{
			send(ATN2);
			flush();
			safeSleep(100);
			int resp = recv();
			safeSleep(attentionDelay+10);
			debug.log(2, comm.getName()+" said ("+Xmodem.hex(resp)+") on try #"+i);
			switch (resp)
			{
				case -1:
					safeSleep(500);
					mode = MODE_NOPRONTO;
					continue;
				//case '.': // neo
				case '*':
				case '!':
					mode = MODE_STANDBY;
					break;
				case '~':
					mode = MODE_WAITING;
					break;
				default:
					if (i == 0)
					{
						continue;
					}
					mode = MODE_NOPRONTO;
					debug.log(2, "ERR: "+resp+" on "+comm.getName());
					throw new IOException("Invalid probe response ("+resp+")");
			}
			break;
		}
		if (i == 2)
		{
			throw new IOException("Unable to raise Pronto");
		}
		debug.log(1, comm.getName()+" Request ("+cmd+") ("+add+")");
		send(cmd.getBytes());
		send(CR);
		if (add != null)
		{
			send(add.getBytes());
		}
		flush();
		lastCMDTime = Util.time();
		safeSleep(commandDelay);
	}

	// ---( static methods )---
	public static boolean isDriverOK()
	{
		try
		{
			return source.getSerialPorts() != null;
		}
		catch (Throwable t)
		{
			t.printStackTrace();
			return false;
		}
	}

	public static int getLoadDelay()
	{
		return loadDelay;
	}

	public static void setLoadDelay(int ld)
	{
		loadDelay = ld;
	}

	public static int getCommandDelay()
	{
		return commandDelay;
	}

	public static void setCommandDelay(int cd)
	{
		commandDelay = cd;
	}

	public static int getAttentionDelay()
	{
		return attentionDelay;
	}

	public static void setAttentionDelay(int ad)
	{
		attentionDelay = ad;
	}

	static byte[] readFile(File f)
		throws IOException
	{
		FileInputStream fi = new FileInputStream(f);
		byte b[] = new byte[(int)f.length()];
		int read = 0;
		while (read < b.length)
		{
			int chunk = fi.read(b, read, b.length - read);
			if (chunk < 0)
			{
				throw new IOException("Unexpected read error");
			}
			read += chunk;
		}
		return b;
	}

	static void safeSleep(int time)
		throws IOException
	{
		try
		{
			debug.log(3, "sleep "+time);
			Thread.currentThread().sleep(time);
		}
		catch (Exception xx)
		{
			throw new InterruptedIOException(xx.getMessage());
		}
	}

	static ICommSerialPortID[] getSerialPorts()
	{
		return source.getSerialPorts();
	}

	static Comm detectPronto(String commPort)
		throws Exception
	{
		return detectPronto(source.getSerialPort(commPort));
	}

	static Comm detectPronto(ICommSerialPortID id)
		throws Exception
	{
		debug.log(1, "probing '"+id+"'");
		for (int i=0; i<1; i++)
		{
			Comm c = null;
			try
			{
				c = new Comm(id);
				if (c.isProntoOK())
				{
					return c;
				}
				c.close();
				return null;
			}	
			catch (Exception ex)
			{
				// may not be a pronto
				if (ex.getMessage() == null)
				{
					debug.log(2,"<"+ex.getClass().getName()+"> on "+id.getName());
				}
				else
				{
					debug.log(2,"'"+ex.getMessage()+"' on "+id.getName());
				}
				if (c != null)
				{
					c.close();
				}
			}
		}
		return null;
	}

	static Comm scanForPronto(ITaskStatus status)
		throws Exception
	{
		return scanForPronto(status, null);
	}

	static Comm scanForPronto(ITaskStatus status, String first)
		throws Exception
	{
		long stime = Util.time();
		ICommSerialPortID ci = null;
		if (first != null)
		{
			first = first.trim();
			if (first.length() == 0)
			{
				first = null;
			}
		}
		if (first != null)
		{
			debug.log(1,"Default port "+first);
			try
			{
				ci = source.getSerialPort(first);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		Comm c = new PortProbe(ci).getComm();
		stime = Util.time() - stime;
		debug.log(1,"Comm Scan took "+stime+" ms");
		return c;
	}

	public static void main(String args[])
		throws Exception
	{
		/*
		System.out.println("v12.3  = "+getVersion("v12.3"));
		System.out.println("v2.3.0 = "+getVersion("v2.3.0"));
		System.out.println("v2.4.1 = "+getVersion("v2.4.1"));
		System.out.println("v2.4.11= "+getVersion("v2.4.11"));
		System.out.println("v2.1   = "+getVersion("v2.1"));
		System.out.println("v1.2   = "+getVersion("v1.2"));
		System.out.println("v1.2.3 = "+getVersion("v1.2.3"));
		System.out.println("v1.2.13= "+getVersion("v1.2.13"));

		if ( true ) { return; }
		*/

		debug.log(0, "driver ok: "+isDriverOK());

		Comm id = scanForPronto(null);
		if (id != null)
		{
			debug.log(0, "Pronto found : "+id);
			byte b[] = id.getCCF(new ITaskStatus() {
				public void taskStatus(int pct, String val) {
					debug.log(0,"getccfmsg: "+pct+", "+val);
				}
				public void taskError(Throwable t) {
					debug.log(0,"getccferr: "+t);
				}
				public void taskNotify(Object o) {
					debug.log(0,"getccfobj: "+o);
				}
			});
			FileOutputStream f = new FileOutputStream("foo.ccf");
			f.write(b);
			f.close();
		}
		else
		{
			debug.log(0,"Pronto not found on any ports");
		}
	}

	// ---( primitive IO methods )---
	private synchronized boolean checkLogging(boolean send)
	{
		if (debug.debug(3))
		{
			if (logBuf == null || sending != send)
			{
				if (logThread == null)
				{
					logThread = new Thread() {
						public void run() {
							lastUpdate = Util.time();
							while (true)
							try
							{
								sleep(1000);
								if (Util.time() - lastUpdate > 1000)
								{
									checkLogging(!sending);
								}
							}
							catch (Exception ex)
							{
								ex.printStackTrace();
								return;
							}
						}
					};
					logThread.start();
				}
				if (logBuf != null && logBuf.length() > 0)
				{
					debug.log(3, (sending ? "SEND " : "RECV ") + logBuf);
				}
				logBuf = new StringBuffer();
				lastUpdate = Util.time();
			}
			sending = send;
			return true;
		}
		else
		{
			return false;
		}
	}

	public int recv()
		throws IOException
	{
		int read = input.read();
		if (read == -1 && onWindows98)
		{
			debug.log(1, "Windows98 read do over on -1");
			read = input.read();
		}
		if (checkLogging(false))
		{
			fprint(read);
		}
		return read;
	}

	public int recv(byte b[])
		throws IOException
	{
		return recv(b, 0, b.length);
	}

	public int recv(byte b[], int off, int len)
		throws IOException
	{
		int ret = input.read(b, off, len);
		if (checkLogging(false))
		{
			if (ret > 0)
			{
				for (int i=0; i<ret; i++)
				{
					fprint(b[off+i]);
				}
			}
			else
			{
				fprint(-1);
			}
		}
		return ret;
	}

	public void send(int b)
		throws IOException
	{
		if (checkLogging(true))
		{
			fprint(b);
		}
		output.write(b);
	}

	public void send(byte b[])
		throws IOException
	{
		send(b, 0, b.length);
	}

	public void send(byte b[], int off, int len)
		throws IOException
	{
		for (int i=0; i<len; i++)
		{
			if (checkLogging(true))
			{
				fprint(b[off+i]);
			}
		}
		output.write(b, off, len);
	}

	public void flush()
		throws IOException
	{
		output.flush();
	}

	private void fprint(byte ch)
		throws IOException
	{
		if (ch >= 32 && ch <= 126)
		{
			fprint(((char)ch)+"  ");
		}
		else
		{
			fprint(Xmodem.hex(ch)+" ");
		}
	}

	private void fprint(int ch)
		throws IOException
	{
		if (ch < 0)
		{
			fprint("xxx ");
		}
		else
		{
			if (ch >= 32 && ch <= 126)
			{
				fprint(((char)ch)+"  ");
			}
			else
			{
				fprint(Xmodem.hex(ch)+" ");
			}
		}
	}

	private void fprint(String s)
		throws IOException
	{
		logBuf.append(s);
	}
}

class PortProbe
{
	private Comm found;
	private int spawn;
	private UnsatisfiedLinkError error;
	private Vector threads = new Vector();

	PortProbe()
		throws Exception
	{
		this(null);
	}

	PortProbe(ICommSerialPortID first)
		throws Exception
	{
		final PortProbe myProbe = this;
		ICommSerialPortID si[] = Comm.getSerialPorts();

		ITaskStatus status = new ITaskStatus() {
			public void taskStatus(int pct, String msg) {
				synchronized (myProbe) {
					if (--spawn == 0 || found != null)
					{
						myProbe.notify();
					}
				}
			}
			public void taskError(Throwable t) {
				error = (UnsatisfiedLinkError)t;
			}
			public void taskNotify(Object obj) {
				found = (Comm)obj;
			}
		};

		if (!(Tonto.canScanThreaded()))
		{
			Comm.debug.log(0, "Using serialized port scan");
			for (int i=0; i<si.length; i++)
			{
				ICommSerialPortID id = si[i];
				if (!id.isCurrentlyOwned())
				{
					try
					{
						found = Comm.detectPronto(id);
						if (found != null)
						{
							break;
						}
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
						continue;
					}
				}
				else
				{
					Comm.debug.log(1, "port '"+id+"' in use");
				}
			}
			return;
		}

		synchronized (myProbe)
		{
			spawn = si.length;
			if (first != null)
			{
				spawn++;
				Comm.debug.log(1, "Comm probe default "+first.getName());
				threads.add(new Detector(status, first));
			}
			for (int i=0; i<si.length; i++)
			{
				if (si[i] == first)
				{
					spawn--;
					continue;
				}
				ICommSerialPortID id = si[i];
				Comm.debug.log(1, "Comm probe "+id.getName());
				threads.add(new Detector(status, id));
			}
		}
	}

	private void killall()
	{
		for (Enumeration e = threads.elements(); e.hasMoreElements(); )
		{
			((Detector)e.nextElement()).interrupt();
		}
	}

	public Comm getComm()
		throws Exception
	{
		synchronized (this)
		{
			if (spawn <= 0)
			{
				return found;
			}
			if (error != null)
			{
				killall();
				throw error;
			}
			wait();
			killall();
			return found;
		}
	}

	private class Detector extends Thread
	{
		private ITaskStatus target;
		private ICommSerialPortID id;

		Detector(ITaskStatus target, ICommSerialPortID id)
		{
			this.target = target;
			this.id = id;
			start();
		}

		public void run()
		{
			if (id.isCurrentlyOwned())
			{
				Comm.debug.log(1, id.getName()+" is in use by another program");
				target.taskStatus(0, "Port in use");
			}
			try
			{
				target.taskNotify(Comm.detectPronto(id));
				target.taskStatus(0, "Port found");
			}
			catch (UnsatisfiedLinkError err)
			{
				target.taskError(err);
				target.taskStatus(0, "No Serial Communication Library");
			}
			catch (Throwable ex)
			{
				target.taskStatus(0, ex.toString());
			}
		}
	}
}


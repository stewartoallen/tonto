/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.*;
import javax.comm.*;
import com.neuron.io.ByteOutputBuffer;

// TODO: block err checking (state machine) in bakbuf reads
final class Xmodem
{
	private static Debug debug = Debug.getInstance("xmodem");
	private static int eotThresh = 1;

	// ---( static fields )---
	private final static int SOH = 1;  // begin 128 byte block
	private final static int STX = 2;  // begin 1024 byte block
	private final static int EOT = 4;  // end of transmission
	private final static int ACK = 6;  // got block
	private final static int NAK = 21; // 0x15 failed to get block
	private final static int CAN = 24; // 0x18 cancel transfer

	private final static byte pad0[] = new byte[1024];
	private final static char hex[] =
	{
		'0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};

	static
	{
		String eot = System.getProperty("xmodem.eot");
		if (eot != null)
		{
			try
			{
				eotThresh = Integer.parseInt(eot);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}

	// ---( instance fields )---
	private IComm comm;
	private ITaskStatus status;
	private int nakCount;
	private int eotCount;
	private int canCount;
	private boolean inErr;
	private byte[] bakbuf;
	private int bakptr;

	// ---( constructors )---
	public Xmodem (IComm comm)
	{
		this(comm, null);
	}

	public Xmodem (IComm comm, ITaskStatus status)
	{
		this.comm = comm;
		this.status = status;
		this.inErr = false;
	}

	// ---( static methods )---
	public static void main(String args[])
		throws Exception
	{
		timeTest();
	}

	public static void timeTest()
	{
		try
		{
			System.out.println("Xmodem timing test");
			//debug.setLevel(3);
			for (int i=0; i<50; i++)
			{
				System.out.println("test: "+i);
				new Tester().runTest();
			}
			System.out.println("Xmodem timing test done");
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	static class Tester
	{
		private void debug(String msg) {
			//debug.log(0, Util.nickname(this)+" "+msg);
		}

		class MyComm implements IComm
		{
			PipedInputStream is = new PipedInputStream();
			PipedOutputStream os = new PipedOutputStream();

			int sc, rc;

			MyComm()
			{
			}

			public void connect(MyComm mc)
				throws Exception
			{
				mc.is.connect(os);
				mc.os.connect(is);
			}
			
			public void sendAttention() throws IOException {
				debug("sendAttention");
			}
			public void send(int ch) throws IOException {
				debug("send["+(++sc)+"]: "+ch);
				os.write(ch);
			}
			public void send(byte b[]) throws IOException {
				sc += b.length;
				debug("send["+(sc)+"]: ["+b.length+"]");
				os.write(b, 0, b.length);
			}
			public void send(byte b[], int off, int len) throws IOException {
				sc += len;
				debug("send["+(sc)+"]: ["+len+"]");
				os.write(b, off, len);
			}

			public int  recv() throws IOException {
				int r = is.read();
				debug("recv["+(++rc)+"]: "+r);
				return r;
			}
			public int  recv(byte b[]) throws IOException {
				rc += b.length;
				int r = is.read(b, 0, b.length);
				debug("recv["+rc+"]: ["+r+"]");
				return r;
			}
			public int  recv(byte b[], int off, int len) throws IOException 
			{
				rc += len;
				int r = is.read(b, off, len);
				debug("recv["+rc+"]: ["+r+"]");
				return r;
			}

			public void flush() throws IOException { os.flush(); }
		}

		MyComm c1 = new MyComm();
		MyComm c2 = new MyComm();
		Xmodem sender = new Xmodem(c1);
		Xmodem receiver = new Xmodem(c2);

		abstract class TimedThread extends Thread
		{
			public void run()
			{
				debug(Util.nickname(this)+" started");
				long start = Util.time();
				try { go(); } catch (Exception ex) { ex.printStackTrace(); }
				long time = Util.time() - start;
				debug(Util.nickname(this)+" ran in "+time+" ms");
			}

			public abstract void go() throws Exception ;
		}

		public void runTest()
			throws Exception
		{
			c1.connect(c2);
			debug("test started");
			long start = Util.time();
			Thread t1 = new TimedThread() { public void go() throws Exception { sender.sendFile(new byte[1111*8]); } };
			Thread t2 = new TimedThread() { public void go() throws Exception { receiver.recvFile(); } };
			t2.start();
			t1.start();
			debug("threads started");
			t1.join();
			t2.join();
			long time = Util.time() - start;
			System.out.println("test took "+time+" ms");
		}
	}

	// ---( instance methods )---
	private int sendBlock(byte buf[], int offset, int blknum)
		throws IOException
	{
		int rem = buf.length - offset;
		if (rem == 0)
		{
			return 0;
		}
		int blksize = (rem >= 1024 ? 1024 : 128);
		int pad = Math.max(0, blksize - rem);
		int blklen = blksize - pad;
		outer: for (int i=0; i<6; i++)
		{
			send(blksize == 128 ? SOH : STX);
			send(blknum);
			send(255-blknum);
			send(buf, offset, blklen);
			CRC16 crc = new CRC16(buf, offset, blklen);
			if (pad > 0)
			{
				send(pad0, 0, pad);
				crc.update(pad0, 0, pad);
			}
			int crcV = crc.getValue();
			debug.log(2,"send blk="+blknum+" off="+offset+" len="+blklen+"/"+blksize+" pad="+pad+" crc="+crcV);
			send(crcV >> 8);
			send(crcV & 0xff);
			flush();
			for (int j=0; j<3; j++)
			{
				int next = nextByte();
				switch (next)
				{
					case ACK:
						return blklen;
					case NAK:
						nakCount++;
						debug.log(2,"  recv: NAK");
						continue;
					case CAN:
						debug.log(2,"  recv: CAN");
						if (j == 2)
						{
							throw new IOException("Transfer remotely cancelled");
						}
						continue;
					case -1:
						debug.log(2,"  recv: -1");
						continue;
					default:
						debug.log(1,"  error sending block "+blknum+" ("+next+",0x"+hex(next)+")");
						break outer;
				}
			}
			if (nakCount > 0) {
				try { Thread.currentThread().sleep(nakCount*10); } catch (Exception ex) { }
			}
		}
		throw new IOException("Unable to send block "+blknum);
	}

	private void ACK()
		throws IOException
	{
		send(ACK);
		flush();
		inErr = false;
	}

	private void EOT()
		throws IOException
	{
		send(EOT);
		flush();
		inErr = false;
	}

	private void NAK()
		throws IOException
	{
		NAK(false);
	}

	private void NAK(boolean force)
		throws IOException
	{
		if (bakbuf == null && (!inErr || force))
		{
			/*
			debug.log(2, "sending NAK ............");
			send(NAK);
			*/
			inErr = !force;
		}
	}

	private void NAK(String err)
		throws IOException
	{
		if (inErr)
		{
			debug.log(3, "ERR:"+err);
		}
		else
		{
			debug.log(3, "NAK:"+err);
		}
		NAK();
	}

	private void CAN()
		throws IOException
	{
		send(CAN);
		send(CAN);
		send(CAN);
		send(CAN);
		send(CAN);
		flush();
	}

	private void notify(Object val)
	{
		if (status != null)
		{
			status.taskNotify(val);
		}
	}

	private void progress(String msg, int val)
	{
		if (status != null)
		{
			status.taskStatus(val, msg);
		}
	}

	private String getHex(byte b[], int off, int len, int llen)
	{
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<len; i++)
		{
			sb.append(hex[(b[i+off]>>4)&0xf]);
			sb.append(hex[b[i+off]&0xf]);
			sb.append(" ");
			if (i % llen == llen-1) { sb.append("\n"); }
		}
		return sb.toString();
	}

	public static String hex(int i)
	{
		return new String(
			new char[] { hex[(i>>4)&0xf], (char)hex[i&0xf] }
		);
	}

	/*
	 * <type> <packet#> <255-packet#> [data...] <crc1> <crc2>
	 */
	private byte[] recvBlock(int blknum, int offset)
		throws IOException
	{
		int cmd = 0;
		int nod = 0;
		boolean skip = false;
		boolean started = false;
		long marker = System.currentTimeMillis();
		start: while (true)
		{
			if (!skip)
			{
				debug.log(2, "recv blk="+blknum+" off="+offset+" bak="+(bakbuf != null));
			}
			if (blknum == 1)
			{
				comm.send('C');
			}
			boolean drop = false;
			int blksize = 128;
			if (!skip)
			{
				cmd = nextByte();
			}
			skip = false;
			switch (cmd)
			{
				case SOH:
					blksize = 128;
					break;
				case STX:
					blksize = 1024;
					break;
				case CAN:
					if (inErr || canCount < 4)
					{
						canCount++;
						debug.log(3,"loop on CAN #"+canCount);
						continue;
					}
					NAK();
					debug.log(2,"transfer remotely cancelled :: CAN");
					throw new RetryError("Transfer remotely cancelled");
				case EOT:
					if (inErr)
					{
						debug.log(3,"loop on EOT in error");
						continue;
					}
					if (eotCount <= eotThresh)
					{
						NAK(true);
						eotCount++;
						continue;
					}
					else
					{
						ACK();
					}
					debug.log(1,"received EOT @ "+offset+" in block "+blknum);
					return null;
				case -1:
					debug.log(1,"no data @ "+offset+" in block "+blknum);
					if (!started && (System.currentTimeMillis() - marker) < 30000)
					{
						continue start;
					}
					if (++nod == 5)
					{
						throw new RetryError("No Data");
					}
					else
					{
						debug.log(2,"loop on -1");
						continue start;
					}
				case '!':
					debug.log(1,"spurious ! ... not a command");
					if (++nod >= 5)
					{
						throw new RetryError("Upload Command failed");
					}
				default:
					NAK("Invalid command ("+cmd+") ("+blksize+")");
					for (int cnt=0; ; )
					{
						cmd = nextByte();
						if (cmd == STX || (blksize == 128 && cmd == SOH))
						{
							debug.log(3,"loop/skip on dup block");
							skip = true;
							continue start;
						}
						if (cmd == -1 && ++cnt > 3)
						{
							throw new RetryError("End of Stream during Upload");
						}
					}
			}
			eotCount = 0;
			canCount = 0;
			int blk = nextByte();
			int blk2 = 255-nextByte();
			if (blk != blk2)
			{
				NAK("block id mismatch ("+blk+" != "+blk2+")");
				cmd = blk2;
				skip = true;
				continue start;
			}
			if (blk != blknum)
			{
				if (blk == blknum - 1)
				{
					// drop re-sent packets
					drop = true;
				}
				else
				{
					NAK("block sequence error ("+blk+" != "+blknum+")");
					cmd = blk2;
					skip = true;
					continue start;
				}
			}
			debug.log(3,"cmd="+hex(cmd)+" blk="+hex(blk)+" toRead="+blksize);
			byte data[] = new byte[blksize];
			int read = 0;
			while (read < blksize)
			{
				int got = nextRead(data, read, blksize - read);
				if (got < 0)
				{
					NAK("block length mismatch: "+blksize);
					continue start;
				}
				read += got;
			}
			int c1 = nextByte();
			int c2 = nextByte();
			int crc1 = (c1 << 8) | c2;
			//int crc1 = (nextByte() << 8) | nextByte();
			int crc2 = new CRC16(data, 0, blksize).getValue();
			if (crc1 != crc2)
			{
				if (bakbuf == null)
				{
					bakbuf = new byte[data.length+2];
					System.arraycopy(data,0,bakbuf,0,data.length);
					bakbuf[data.length] = (byte)c1;
					bakbuf[data.length+1] = (byte)c2;
					continue start;
				}
				NAK("crc mismatch ("+crc1+" != "+crc2+")");
				continue start;
			}
			debug.log(2, "recv blk="+blk+" off="+offset+" len="+blksize+" crc="+crc1);
			ACK();
			started = true;
			if (drop)
			{
				continue start;
			}
			return data;
		}
	}

	private char toChar(int ch)
	{
		if (ch >= 32 || ch <= 127) { return (char)ch; }
		return '-';
	}

	public void sendFile(byte b[])
		throws IOException
	{
		debug.log(1,"send file="+b.length);
		int off = 0;
		int i = 1;
		int lastPct = -1;
		int nodata = 0;
		int retry = 0;
		int maxTries = 8;
		loop: for (int x=0; x<maxTries; x++)
		{
			int recv = comm.recv();
			debug.log(2, "sendfile:recv: ("+recv+",0x"+hex(recv)+","+toChar(recv)+")");
			switch (recv)
			{
				case 'C': break loop;
				case -1 :
					if (++nodata >= 3 && ++retry < 2)
					{
						comm.sendAttention();
						nextByte();
						send("dl ccf\r".getBytes());
						flush();
					}
					break;
				default : nodata=0; break;
				//case -1 : x = (x*2)+1; break;
				//default : x++; break;
			}
			if (x >= maxTries - 1)
			{
				throw new IOException("Send terminated. No Xmodem stream.");
			}
		}
		while (true)
		{
			int sent = sendBlock(b, off, (i++)%256);
			if (sent == 0)
			{
				break;
			}
			off += sent;
			int newPct = (off*100)/b.length;
			if (newPct != lastPct)
			{
				lastPct = newPct;
				progress(null, newPct);
			}
		}
		int tries = 0;
		do
		{
			if (tries++ > 4)
			{
				CAN();
				throw new IOException("Send terminated due to retry errors");
			}
			EOT();
		}
		while (nextByte() != ACK)
			;
		debug.log(1,"send file complete");
	}

	public byte[] recvFile()
		throws IOException
	{
		return recvFile(0);
	}

	public byte[] recvFile(int size)
		throws IOException
	{
		bakbuf = null;
		bakptr = 0;
		int pri = Thread.currentThread().getPriority();
		debug.log(1,"recv size="+size);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		long time = System.currentTimeMillis();
		int blk = 1;
		int off = 0;
		try
		{
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			byte buf[] = null;
			while ((buf = recvBlock((blk++)%256, off)) != null)
			{
				if (blk == 2 && Util.getInt(buf, CCFNodeState.magicOffset) == CCFNodeState.MAGIC)
				{
					size = (Util.getInt(buf, 4)*3)/5;
					debug.log(1,"recv compressed ccf predict size "+size);
				}
				off += buf.length;
				os.write(buf, 0, buf.length);
				progress(null,(os.size()*100)/Math.max(size,1));
			}
			time = System.currentTimeMillis() - time;
			debug.log(1,"recv complete len="+off+
				" time="+time+" rate="+((off*1000)/time));
			return os.toByteArray();
		}
		catch (IOException ex)
		{
			debug.log(0,ex.getMessage());
			if (size > 0 && off >= size)
			{
				debug.log(1,"recv complete with errors @ "+off);
				NAK();
				ACK();
				CAN();
				return os.toByteArray();
			}
			CAN();
			int i=0;
			while (nextByte() != -1)
			{
				i++;
			}
			if (i > 0)
			{
				debug.log(1,"drained "+i+" bytes");
			}
			throw ex;
		}
		finally
		{
			Thread.currentThread().setPriority(pri);
		}
	}

	// ---( primitive IO )---
	public int nextByte()
		throws IOException
	{
		if (bakbuf != null)
		{
			int r = bakbuf[bakptr++] & 0xff;
//debug.log(0, "bakread: ("+r+") from "+bakbuf.length+" @ "+bakptr);
			if (bakptr >= bakbuf.length)
			{
				bakbuf = null;
				bakptr = 0;
			}
			return r;
		}
		return comm.recv();
	}

	public int nextRead(byte b[], int off, int len)
		throws IOException
	{
		if (bakbuf != null)
		{
			int max = Math.min(len, bakbuf.length - bakptr);
//debug.log(0, "bakread: "+off+","+len+","+max+" from "+bakbuf.length+" @ "+bakptr);
			System.arraycopy(bakbuf,bakptr,b,off,max);
			bakptr += max;
			if (bakptr >= bakbuf.length)
			{
				bakbuf = null;
				bakptr = 0;
			}
			return max;
		}
		return comm.recv(b, off, len);
	}

	public void flush()
		throws IOException
	{
		comm.flush();
	}

	public void send(int b)
		throws IOException
	{
		comm.send(b);
	}

	public void send(byte b[])
		throws IOException
	{
		comm.send(b, 0, b.length);
	}

	public void send(byte b[], int off, int len)
		throws IOException
	{
		comm.send(b, off, len);
	}
}


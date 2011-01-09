/*
 * Copyright 2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.*;
import java.util.*;

public class Firmware
{
	// ---( static fields )---
	private static int magicOffset = 8;
	private static byte[] magicCode = { (byte)0x43, (byte)0x4f, (byte)0x44, (byte)0x45 };
	private static byte[] magicNew = { (byte)0x40, (byte)0xa5, (byte)0x5a, (byte)0x40 };

	// ---( static methods )---
	public static void main(String args[])
		throws Exception
	{
		if (args.length == 0)
		{
			System.out.println("Firmware [-x | -i] <file(s)>");
			System.exit(1);
		}
		int off=0;
		boolean extract = false;
		if (args[0].equals("-x"))
		{
			off = 1;
			extract = true;
		}
		else
		if (args[0].equals("-i"))
		{
			install(args[1]);
			return;
		}

		printHeader();

		for (int i=off; i<args.length; i++)
		{
			try
			{

			Firmware f = new Firmware(args[i]);
			f.printDetail();
			if (extract)
			{
				String s = f.getDescription().replace(' ','_').replace(';','_').toLowerCase();
				File dir = new File("img_"+s);
				dir.mkdirs();
				writeSegment(dir, "boot.img",f.getBOOT());
				writeSegment(dir, "psos.img",f.getPSOS());
				writeSegment(dir, "_app.img",f.get_APP());
				writeSegment(dir, "_sys.img",f.get_SYS());
				writeSegment(dir, "_ccf.img",f.get_CCF());
				writeSegment(dir, "_udb.img",f.get_UDB());
				Hashtable h = extractCIBs(f.get_APP());
				if (h.size() > 0)
				{
					dir = new File(dir, "cib");
					dir.mkdirs();
					for (Enumeration e = h.keys(); e.hasMoreElements(); )
					{
						String k = (String)e.nextElement();
						Object o = h.get(k);
						if (o instanceof CCFIcon)
						{
							CCFIcon icon = (CCFIcon)o;
							if (k.endsWith(".cib"))
							{
								k = k.substring(0,k.length()-4)+".gif";
							}
							icon.saveGIF(new File(dir, k).toString());
						}
						else
						if (o instanceof byte[])
						{
							FileOutputStream fo = new FileOutputStream(new File(dir, k));
							fo.write((byte[])o);
							fo.close();
						}
					}
				}
			}

			}
			catch (Exception ex)
			{
				System.out.println("error firmware image ** "+args[i]+" **");
				ex.printStackTrace();
			}
		}

		System.exit(0);
	}

	private static void writeSegment(File dir, String name, byte b[])
		throws IOException
	{
		if (b != null)
		{
			FileOutputStream fo = new FileOutputStream(new File(dir, name));
			fo.write(b);
			fo.close();
		}
	}

	private static void install(String file)
		throws Exception
	{
		Debug.logToFile("install.log");
		Debug.getInstance("comm").setLevel(3);
		Debug.getInstance("xmodem").setLevel(3);
		Comm c = Comm.scanForPronto(null);
		c.updateFirmware(new Firmware(file), new ITaskStatus() {
			public void taskStatus(int pct, String value) {
				System.out.println("ts: "+pct+" "+value);
			}
			public void taskError(Throwable t) {
				System.out.println("te: "+t);
			}
			public void taskNotify(Object val) {
				System.out.println("tn: "+val);
			}
		});
		c.close();
	}

	// ---( constructors )---
	public Firmware (String file)
		throws IOException
	{
		this.file = file;
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		long len = raf.length();
		raf.seek(len-18);
		int offDesc = readInt(raf);
		offBOOT = readInt(raf);
		raf.seek(offDesc);
		desc = readString(raf);
		// TSU1000 firmware is slightly different, no boot image
		if (desc.startsWith("sys"))
		{
			offPSOS = offBOOT;
			offBOOT = 0;
		}
		else
		{
			raf.seek(offBOOT);
			lenBOOT = readInt(raf);
			offPSOS = offBOOT + lenBOOT + 4;
		}
		raf.seek(offPSOS);
		lenPSOS = readInt(raf);
		off_SYS = offPSOS + lenPSOS + 4;
		raf.seek(off_SYS);
		len_SYS = readInt(raf);
		off_APP = off_SYS + len_SYS + 4;
		raf.seek(off_APP);
		len_APP = readInt(raf);

		off_CCF = off_APP + len_APP + 4;
		if (hasCode(raf,off_CCF+12,0x434f4445))
		{
			raf.seek(off_CCF);
			len_CCF = readInt(raf);
		}
		else
		{
			off_CCF = 0;
			raf.close();
			return;
		}
		off_UDB = off_CCF + len_CCF + 4;
		if (off_UDB > 0 && off_UDB < len && (hasCode(raf,off_UDB+19,0x5f554442) || hasCode(raf,off_UDB+12,0x434f4445)))
		{
			raf.seek(off_UDB);
			len_UDB = readInt(raf);
		}
		else
		{
			off_UDB = 0;
		}

		raf.close();
	}

	private boolean hasCode(RandomAccessFile ra, int offset, int code)
		throws IOException
	{
		ra.seek(offset);
		return ra.readInt() == code;
	}

	// ---( instance fields )---
	private String file;
	private String desc;
	private int offBOOT, offPSOS, off_SYS, off_APP, off_CCF, off_UDB;
	private int lenBOOT, lenPSOS, len_SYS, len_APP, len_CCF, len_UDB;

	// ---( instance methods )---
	public String getDescription()
	{
		return desc;
	}

	public String[][] getSegmentReport()
	{
		return new String[][]
		{
			{ "BOOT", Integer.toString(offBOOT+4), Integer.toString(lenBOOT) },
			{ "PSOS", Integer.toString(offPSOS+4), Integer.toString(lenPSOS) },
			{ "_SYS", Integer.toString(off_SYS+4), Integer.toString(len_SYS) },
			{ "_APP", Integer.toString(off_APP+4), Integer.toString(len_APP) },
			{ "_CCF", Integer.toString(off_CCF+4), Integer.toString(len_CCF) },
			{ "_UDB", Integer.toString(off_UDB+4), Integer.toString(len_UDB) },
		};
	}

	public byte[] getBOOT()
		throws IOException
	{
		return lenBOOT > 0 ? readChunk(offBOOT+4, lenBOOT) : null;
	}

	public byte[] getPSOS()
		throws IOException
	{
		return readChunk(offPSOS+4, lenPSOS);
	}

	public byte[] get_SYS()
		throws IOException
	{
		return readChunk(off_SYS+4, len_SYS);
	}

	public byte[] get_APP()
		throws IOException
	{
		return readChunk(off_APP+4, len_APP);
	}

	public byte[] get_CCF()
		throws IOException
	{
		return len_CCF > 0 ? readChunk(off_CCF+4, len_CCF) : null;
	}

	public byte[] get_UDB()
		throws IOException
	{
		return len_UDB > 0 ? readChunk(off_UDB+4, len_UDB) : null;
	}

	public static Hashtable extractCIBs(byte b[])
	{
		Hashtable h = new Hashtable();
		if (b == null)
		{
			return h;
		}
		byte pat[] = { '.', 'c', 'i', 'b', '\0' };
		CCFHeader hdr = new CCFHeader();
		CCFNodeState ns = new CCFNodeState(hdr, null);
		for (int loc=0; loc >= 0; )
		{
			loc = findPattern(b, pat, loc);
			if (loc > 0)
			{
				int spos = 0;
				int npos = loc + 4;
				int nmod = 4 - (npos % 4);
				int dpos = npos + nmod;
				int dlen = ((b[dpos] & 0xff) << 8) | (b[dpos+1] & 0xff);
				int sbak = Math.max(0,(loc - Math.max(dlen, 100)));
				for (int i=loc; i>sbak; i--)
				{
					if ((((b[i] & 0xff) << 8) | (b[i+1] & 0xff)) == dlen)
					{
						spos = i;
						break;
					}
				}
				int slen = npos-spos-2;
				String nm = slen > 0 ? new String(b,spos+2,npos-spos-2) : null;
				if (nm == null || nm.length() <= 4)
				{
					break;
				}
				loc = dpos + dlen + 2;
				byte out[] = new byte[dlen+2];
				System.arraycopy(b,dpos,out,0,out.length);

				try 
				{
					ns.setBuffer(out);
					CCFIcon i = new CCFIcon(hdr);
					i.decode(ns);
					h.put(nm, i);
				}
				catch (Exception ex)
				{
					System.out.println("Image decode error on '"+nm+"'");
					ex.printStackTrace();
					h.put(nm, out);
				}

			}
		}
		return h;
	}

	private static int findPattern(byte b[], byte pat[], int off)
	{
		int ppos = 0;

		for (int i=off; i<b.length; i++)
		{
			if (b[i] == pat[ppos])
			{
				if (++ppos == pat.length)
				{
					return i-ppos+1;
				}
			}
			else
			{
				ppos = 0;
			}
		}

		return -1;
	}

	private byte[] readChunk(int off, int len)
		throws IOException
	{
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		raf.seek(off);
		byte b[] = new byte[len];
		raf.readFully(b);
		/*
		for (int i=0; i<magicCode.length; i++)
		{
			if (b[i+magicOffset] != magicCode[i])
			{
				throw new IOException("Invalid chunk magic");
			}
		}
		*/
		System.arraycopy(magicNew, 0, b, magicOffset, magicNew.length);
		return b;
	}

	public static void printHeader()
	{
		System.out.println(
			"BOOT          "+
			"PSOS          "+
			"_SYS          "+
			"_APP          "+
			"_CCF          "+
			"_UDB          "+"Description"
		);
	}

	public void printDetail()
	{
		System.out.println(
			printLHex(offBOOT)+","+printRHex(lenBOOT)+" "+
			printLHex(offPSOS)+","+printRHex(lenPSOS)+" "+
			printLHex(off_SYS)+","+printRHex(len_SYS)+" "+
			printLHex(off_APP)+","+printRHex(len_APP)+" "+
			printLHex(off_CCF)+","+printRHex(len_CCF)+" "+
			printLHex(off_UDB)+","+printRHex(len_UDB)+" "+desc
		);
	}

	private static String printLHex(int val)
	{
		return CCFNode.lpad(Integer.toHexString(val), 6);
	}

	private static String printRHex(int val)
	{
		return CCFNode.rpad(Integer.toHexString(val), 6);
	}

	private int readInt(RandomAccessFile ra)
		throws IOException
	{
		return ra.read() | (ra.read()<<8) | (ra.read()<<16) | (ra.read()<<24);
	}

	private String readString(RandomAccessFile ra)
		throws IOException
	{
		StringBuffer sb = new StringBuffer();
		for (int ch = 0; (ch = ra.read()) != 0; )
		{
			sb.append((char)ch);
		}
		return sb.toString();
	}

	// ---( interface methods )---

}


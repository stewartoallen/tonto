/*
 * Copyright 2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;
import com.neuron.io.ByteOutputBuffer;
import com.neuron.app.tonto.ui.StackedDialog;

public final class Util
{
	private final static Color darkBlue = new Color(0,0,128);

	// -------------------------------------------------------------------------------------
	// I / O
	// -------------------------------------------------------------------------------------
	public static byte[] readFile(String filename)
		throws IOException
	{
		RandomAccessFile raf = new RandomAccessFile(filename, "r");
		byte b[] = new byte[(int)raf.length()];
		raf.seek(0);
		raf.readFully(b);
		raf.close();
		return b;
	}

	public static byte[] readFully(InputStream is)
		throws IOException
	{
		ByteOutputBuffer bob = new ByteOutputBuffer();
		readFully(is, bob);
		return bob.toByteArray();
	}

	public static void readFully(InputStream is, OutputStream os)
		throws IOException
	{
		byte b[] = new byte[4096];
		int read;
		while ( (read = is.read(b)) >= 0 )
		{
			os.write(b, 0, read);
		}
	}

	// -------------------------------------------------------------------------------------
	// N U M B E R S
	// -------------------------------------------------------------------------------------
	public static int bound(int min, int max, int val)
	{
		if (val > max) { return max; }
		if (val < min) { return min; }
		return val;
	}

	public static String toHex(int val)
	{
		return "0x"+Integer.toHexString(val);
	}

	public static int unsign(byte b)
	{
		return b & 0xff;
	}

	// -------------------------------------------------------------------------------------
	// S Y S T E M
	// -------------------------------------------------------------------------------------
	public static void safeSleep(int time)
	{
		try
		{
			Thread.currentThread().sleep(time);
		}
		catch (Exception xx)
		{
			xx.printStackTrace();
		}
	}

	public static String sysprop(String prop)
	{
		return getProperty(prop);
	}

	public static String getProperty(String prop)
	{
		return System.getProperty(prop);
	}

	public static void setProperty(String prop, String val)
	{
		System.getProperties().put(prop, val);
	}

	public static boolean onWindows()
	{
		return sysprop("os.name").toLowerCase().indexOf("windows") >= 0;
	}

	public static boolean onWindows98()
	{
		return sysprop("os.name").toLowerCase().indexOf("98") >= 0;
	}

	public static boolean onMacintosh()
	{
		return sysprop("os.name").toLowerCase().startsWith("mac");
	}

	public static boolean isIBMJDK()
	{
		return sysprop("java.vm.vendor").toLowerCase().indexOf("ibm") >= 0;
	}

	public static boolean isSunJDK()
	{
		return sysprop("java.vm.vendor").toLowerCase().indexOf("sun") >= 0;
	}

	public static boolean isJDK13()
	{
		return sysprop("java.version").indexOf("1.3") >= 0;
	}

	public static boolean isJDK14()
	{
		return sysprop("java.version").indexOf("1.4") >= 0;
	}

	public static long time()
	{
		return System.currentTimeMillis();
	}

	public static String shortName(Class clz)
	{
		String nm = clz.getName();
		int idx = nm.lastIndexOf('.');
		if (idx >= 0)
		{
			return nm.substring(idx+1);
		}
		return nm;
	}

	public static String nickname(Object o)
	{
		if (o == null)
		{
			return "<null>";
		}
		return shortName(o.getClass())+":"+o.hashCode();
	}

	public static String unURL(String url)
	{
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<url.length(); i++)
		{
			if (url.charAt(i) == '%' && i<url.length()-3)
			{
				sb.append((char)Integer.parseInt(url.substring(i+1, i+3),16));
				i += 2;
			}
			else
			{
				sb.append(url.charAt(i));
			}
		}
		return sb.toString();
	}

	// -------------------------------------------------------------------------------------
	// S W I N G
	// -------------------------------------------------------------------------------------
	public static void setLabelBorder(String label, JComponent c)
	{
		TitledBorder b = new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), label);
		b.setTitleColor(darkBlue);
		c.setBorder(b);
	}

	public static void errorDialog(String msg, Throwable ex)
	{
		String title = (ex != null ? ex.getMessage() : null);
		if (title == null || title.length() == 0)
		{
			title = "Error";
		}
		JOptionPane.showMessageDialog(StackedDialog.parent(), msg, title, JOptionPane.ERROR_MESSAGE);
	}

	public static boolean confirmDialog(String title, String msg)
	{
		return JOptionPane.showConfirmDialog(StackedDialog.parent(),
			msg, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	}

	public static JPanel panelWrap(JComponent c)
	{
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(1,1));
		p.add(c);
		return p;
	}

	// -------------------------------------------------------------------------------------
	// O T H E R
	// -------------------------------------------------------------------------------------
	public static int decompress (
		byte src[], int src_off, int src_len,
		byte dst[], int dst_off, int dst_len
	)
		throws Exception
	{
		int len;
		int bptr;
		int iptr = src_off;
		int optr = dst_off;

		len = unsign(src[iptr++]);
		// if any bits set from [11110000]
		// COPY from SRC->DST where t=(0 to 238)
		// if the next byte is < 16 then an error has occurred
		if (len > 17)
		{
			len = len - 17;
			do
			{
				dst[optr++] = src[iptr++];
			}
			while (--len > 0);
			len = unsign(src[iptr++]);
			if (len < 16)
			{
				throw new Exception("Decompression Error");
			}
		}

		start:

		for ( ; ; len = unsign(src[iptr++]))
		{
			// if only bits set from [00001111]
			if (len < 16)
			{
				// series of 0's (equal to 255) + 15 + first non-0 value
				if (len == 0)
				{
					while (src[iptr] == 0)
					{
						len = len + 255;
						iptr++;
					}
					len = len + unsign(src[iptr++]) + 15;
				}
				len = len + 3;
				// COPY from SRC->DST where t=(3-xxxx)
				do
				{
					dst[optr++] = src[iptr++];
				}
				while (--len > 0);
				len = unsign(src[iptr++]);
				// if only bits set from [00001111]
				if (len < 16)
				{
					// len bits use [00001100]>>2 [00111111]<<2
					// output ptr - 2049 - (t >> 2) - (next char << 2)
					bptr = optr - 0x801 - (len >> 2) - (unsign(src[iptr++]) << 2);
					if (bptr < dst_off)
					{
						// look back preceeds dst output ptr
						throw new Exception("Lookback overrun");
					}
					len = 3;
					// COPY from DST->DST where pos=(-2049 to -2304) and t=(3)
					do
					{
						dst[optr++] = dst[bptr++];
					}
					while (--len > 0);
					// len bits use [00000011]
					len = src[iptr-2] & 3;
					if (len == 0)
					{
						continue;
					}
					// COPY from SRC->DST where t=(0-3)
					do
					{
						dst[optr++] = src[iptr++];
					}
					while (--len > 0);
					len = unsign(src[iptr++]);
				}
			}
			// more copying from dst to dst
			for ( ; ; len = unsign(src[iptr++]))
			{
				// if any bits set from [11000000]
				// pos bits use [00011100]>>2 [00011111]<<3
				// len bits use [11100000]>>5
				// COPY from DST->DST where pos=(-1 to -256) and t=(2-8)
				if (len >= 64)
				{
					// outpos - 1 - (0-255)
					bptr = optr - ((len >> 2) & 7) - (unsign(src[iptr++]) << 3) - 1;
					len = (len >> 5) - 1;
				}
				// if any bits set from [11100000]
				// pos bits use [--------] [11111100]>>2 [00111111]<<2
				// COPY from DST->DST where pos=(-1 to -256) and t=(2-xxxx)
				else
				if (len >= 32)
				{
					// if all bits [00011111] empty, use 0 seq to add 255's
					len = len & 31;
					if (len == 0)
					{
						while (src[iptr] == 0)
						{
							len = len + 255;
							iptr++;
						}
						len = len + unsign(src[iptr++]) + 31;
					}
					bptr = optr - (unsign(src[iptr++]) >> 2) - (unsign(src[iptr++]) << 6) - 1;
				}
				// if any bits set from [11110000]
				// pos bits use [00001000]<<11 [11111100]>>2 [00111111]<<2
				// len bits use [00000111]
				// COPY from DST->DST where pos=(-16384 to -16639)||(-1 to -256) and t=(2-xxxx)
				else if (len >= 16)
				{
					bptr = optr - ((len & 8) << 11);
					len = len & 7;
					if (len == 0)
					{
						while (src[iptr] == 0)
						{
							len = len + 255;
							iptr++;
						}
						len = len + 7 + unsign(src[iptr++]);
					}
					bptr = bptr - (unsign(src[iptr++]) >> 2) - (unsign(src[iptr++]) << 6);
					if (bptr == optr)
					{
						break start;
					}
					bptr = bptr - 0x4000;
				}
				// pos bits use [11111100]>>2 [00111111]<<2
				// COPY from DST->DST where pos=(-1 to -256) and t=(2)
				else
				{
					bptr = optr - (len >> 2) - (unsign(src[iptr++]) << 2) - 1;
					len = 0;
				}
				if (bptr < dst_off)
				{
					throw new Exception("Lookback overrun");
				}
				len = len + 2;
				// do DST->DST copy
				do
				{
					dst[optr++] = dst[bptr++];
				}
				while (--len > 0);
				len = src[iptr-2] & 3;
				if (len == 0)
				{
					break;
				}
				// COPY from SRC->DST where t=(0-3)
				do
				{
					dst[optr++] = src[iptr++];
				}
				while (--len > 0);
			}
		}

		optr = optr - dst_off;
		iptr = iptr - src_off;

		if (len != 1)
		{
			throw new Exception("Decompression Error");
		}
		if (iptr > src_len)
		{
			throw new Exception("Input too short");
		}
		if (iptr < src_len)
		{
			throw new Exception("Input not exhausted");
		}

		return optr;
	}

	public static int getInt(byte buf[], int off)
	{
		return
			((buf[(int)off++]&0xff) << 24) |
			((buf[(int)off++]&0xff) << 16) |
			((buf[(int)off++]&0xff) << 8)  |
			((buf[(int)off++]&0xff) << 0);
	}

}


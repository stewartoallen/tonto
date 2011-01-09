/*
 * Copyright 2000-2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.irdb.impl;

// ---( imports )---
import com.neuron.irdb.*;

public class RedRat extends IRSignal
{
	public RedRat(String raw)
	{
		decode(raw);
	}

	public RedRat(IRSignal sig)
	{
		super(sig);
	}

	public boolean decode(String msg)
	{
		if (msg == null || msg.length() == 0)
		{
			throw new RuntimeException("malformed code ("+msg+")");
		}
		// decode redrat
		{
			if (msg.charAt(0) == 'S' || msg.charAt(0) == 'P')
			{
				msg = msg.substring(1);
			}
			if (msg.charAt(0) != 'F' || msg.charAt(3) != 'L')
			{
				throw new RuntimeException("malformed code ("+msg+")");
			}
			int xpos = msg.indexOf('X', 4);
			int ppos = msg.indexOf('P', xpos);
			int rpos = msg.indexOf('R', ppos);

			int freq = 4000000/value(msg.substring(1,3));
			int nv = (xpos - 4)/4;
			int pulses[] = new int[nv];
			for (int i=0; i<nv; i++)
			{
				pulses[i] = value(msg.substring(4+(i*4),(8+(i*4))));
			}
			getPulseIndex().clear();
			IRBurst intro = getIntro();
			IRBurst repeat = getRepeat();
			intro.clear();
			repeat.clear();
			String rawsig = BinStringToRat(
				HexToBinString(msg.substring(xpos+1, ppos)));
			int npos1 = rawsig.indexOf("9");
			int npos2 = rawsig.indexOf("9", npos1+1);
			for (int i=0; i<npos1; i++)
			{
				intro.addTimePulse(pulses[rawsig.charAt(i) - '0']);
			}
			for (int i=npos1+1; i<npos2; i++)
			{
				repeat.addTimePulse(pulses[rawsig.charAt(i) - '0']);
			}
			int pause = value(msg.substring(ppos+1,rpos)) * 8;
			int repeats = value(msg.substring(rpos+1));
			if (pause > 0)
			{
				if (intro.length() > 0) { intro.addTimePulse(pause); }
				if (repeat.length() > 0) { repeat.addTimePulse(pause); }
			}
			setMinRepeat(repeats);
		}
		return true;
	}

	public String encode()
	{
		return getCodedString();
	}

	private static String hex(int i)
	{
		return hex(i,4);
	}

	private static String hex(int i, int w)
	{
		String hb = Integer.toHexString(i).toUpperCase();
		if (hb.length() < w)
		{
			return "00000000".substring(8-(w-hb.length()))+hb;
		}
		else
		{
			return hb;
		}
	}

	private static int value(String s)
	{
		return Integer.valueOf(s,16).intValue();
	}

	private boolean trimSignal()
	{
		return false;
		/*
		String code = getCodes();
		int siglen = findSigLength(code);
		if (siglen > 0)
		{
			setCodes(code.substring(0,siglen)+"99");
			return true;
		}
		else
		{
			return false;
		}
		*/
	}

	/*
	 * turns hex into binary string (ex:010101001110101)
	 */
	private static String HexToBinString(String hex)
	{
		hex = hex.toUpperCase();
		int ln = hex.length();
		StringBuffer sb = new StringBuffer(ln*4);
		for (int i=0; i<ln; i++)
		{
			char ch = hex.charAt(i);
			int cv = (ch-'0' < 10 ? ch-'0' : ch-'A'+10);
			sb.append((cv&0x8) > 0 ? "1" : "0");
			sb.append((cv&0x4) > 0 ? "1" : "0");
			sb.append((cv&0x2) > 0 ? "1" : "0");
			sb.append((cv&0x1) > 0 ? "1" : "0");
		}
		return sb.toString();
	}

	/*
	 * turns binary string into hex string
	 */
	private static String BinStringToHex(String bits)
	{
		while (bits.length() % 4 != 0)
		{
			bits += "0";
		}
		int ln = bits.length();
		/*
		if (ln % 4 != 0)
		{
			throw new RuntimeException("invalid bits length "+ln);
		}
		*/
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<ln; i+=4)
		{
			int cv = 0;
			cv |= (bits.charAt(i+0) == '0' ? 0 : 8);
			cv |= (bits.charAt(i+1) == '0' ? 0 : 4);
			cv |= (bits.charAt(i+2) == '0' ? 0 : 2);
			cv |= (bits.charAt(i+3) == '0' ? 0 : 1);
			sb.append(cv < 10 ? (char)('0'+cv) : (char)('A'+cv-10));
		}
		while (sb.length() % 4 != 0)
		{
			sb.append("0");
		}
		return sb.toString();
	}

	/*
	 * turns rat ircode into binary string
	 */
	private static String RatToBinString(String code)
	{
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<code.length(); i++)
		{
			switch (code.charAt(i))
			{
				case '0': sb.append("0100");   break;
				case '1': sb.append("00");     break;
				case '2': sb.append("1");      break;
				case '3': sb.append("0111");   break;
				case '4': sb.append("01010");  break;
				case '5': sb.append("01011");  break;
				case '6': sb.append("011010"); break;
				case '7': sb.append("011011"); break;
				case '9': sb.append("01100");  break;
			}
		}
		int gap = sb.length()%4;
		if (gap > 0)
		{
			sb.append("0000".substring(gap));
		}
		return sb.toString();
	}

	/*
	 * turns binary string into rat ircode
	 */
	private static String BinStringToRat(String bits)
	{
		int ln = bits.length();
		int lx = ln - 1;
		StringBuffer ret = new StringBuffer();
		int pos = 0;
		int eos = 0;
		for (int i=0; i<ln; )
		{
			// 0
			if (bits.charAt(i++) == '0')
			{
				if (i > lx) { break; }
				// 00
				if (bits.charAt(i++) == '0')
				{
					ret.append('1');
					if (i > lx) { break; }
				}
				else
				// 010
				if (bits.charAt(i++) == '0')
				{
					if (i > lx) { break; }
					if (bits.charAt(i++) == '0')
					{
						ret.append('0');
						if (i > lx) { break; }
					}
					else
					// 0101
					if (bits.charAt(i++) == '0')
					{
						ret.append('4');
						if (i > lx) { break; }
					}
					else
					{
						ret.append('5');
						if (i > lx) { break; }
					}
				}
				// 011
				else
				{
					// 0110
					if (bits.charAt(i++) == '0')
					{
						if (i > lx) { break; }
						// 01100
						if (bits.charAt(i++) == '0')
						{
							ret.append('9');
							eos++;
							if (eos >= 2) { break; }
							if (i > lx) { break; }
						}
						else
						// 01101
						{
							// 011010
							if (bits.charAt(i++) == '0')
							{
								ret.append('6');
								if (i > lx) { break; }
							}
							else
							// 011011
							{
								ret.append('7');
							}
						}
					}
					else
					// 0111
					{
						ret.append('3');
					}
				}
			}
			else
			// 1
			{
				ret.append('2');
			}
		}
		return ret.toString();
	}

	/*
	public static int findSigLength(String bits)
	{
		if (bits.indexOf("99") < 0)
		{
			return 0;
		}
		int ln = bits.length();
		int ln10 = ln/10;
		int bestLEN = 0;
		double bestPCT = 0.0;
		for (int i = ln-ln10; i > ln10; i--)
		{
			int match = 0;
			int srchlen = Math.min(i+i, ln) - i;
			for (int j=0; j<srchlen; j++)
			{
				if (bits.charAt(j) == bits.charAt(i+j))
				{
					match++;
				}
			}
			double pct = ((double)match)/((double)srchlen);
			if (pct > 0.975 && (pct > bestPCT || pct == 1.0))
			{
				bestPCT = pct;
				bestLEN = i;
			}
		}
		return bestLEN;
	}
	*/

	/*
	 * turns an array of ints into a String. Ex: [1,2,4,2,2,3]
	 * becomes '124223'.
	 */
	private static String IntsToString(int s[])
	{
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<s.length; i++)
		{
			sb.append((char)('0' + s[i]));
		}
		return sb.toString();
	}

	// ---( instance methods )---
	public String getSendString()
	{
		return getSendString(getMinRepeat());
	}

	public String getSendString(int repeat)
	{
		return "P"+getCodedString(repeat);
	}

	// returns in the F-L-X-P-R- format
	public String getCodedString()
	{
		return getCodedString(getMinRepeat());
	}

	public String getCodedString(int repeats)
	{
		PulseIndex idx = getPulseIndex().getClone();
		IRBurst in = getIntro().getClone(this);
		IRBurst rep = getRepeat().getClone(this);
		if (rep.length() == 0)
		{
			rep = in;
		}
		if (in.length() > 0 && in != rep &&
			in.get(in.length()-1) == rep.get(rep.length()-1))
		{
			in.setLength(in.length()-1);
		}
		int poff = rep.get(rep.length()-1);
		int pause = idx.getPulse(poff);
		idx.setPulse(poff, 0);
		rep.setLength(rep.length()-1);
		int intro[] = in.getPulses();
		int repeat[] = rep.getPulses();
		String signal = ((intro.length == 0) || (in == rep) ?
			IntsToString(repeat)+"99" :
			IntsToString(intro)+"9"+IntsToString(repeat)+"9");
		StringBuffer sb = new StringBuffer();
		int pulses[] = idx.getIndexValues();
		for (int i=0; i<pulses.length; i++)
		{
			sb.append(hex(pulses[i]));
		}	
		return "F"+hex(4000000/getFrequency(),2)+"L"+sb+"X"+
			BinStringToHex(RatToBinString(signal))+
			"P"+hex(pause/8)+"R"+hex(Math.max(repeats,1),2);
	}
}


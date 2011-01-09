/*
 * Copyright 2001-2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.util;

public class AnsiColor
{
    private static boolean useColor =
		System.getProperty("os.name").toLowerCase().indexOf("linux") >= 0 ||
		System.getProperty("os.hasColor", "false").equals("true");

	public static int CYAN    = 36; // color
	public static int PURPLE  = 35; // color
	public static int BLUE    = 34; // color
	public static int YELLOW  = 33; // color
	public static int GREEN   = 32; // color
	public static int RED     = 31; // color
	public static int BLACK   = 30; // color
	public static int WHITE   = 29; // color

	public static int BRIGHT  =  1; // mode
	public static int NORMAL  =  0; // mode

	public static boolean supportsColor()
	{
		return useColor;
	}

	public static String color(Object s, int mode, int color)
	{
		return color(s, mode, color, false);
	}

	public static String color(Object s, int mode, int color, boolean nested)
	{
	    if (!useColor) {
	        return s.toString();
		} else {
			if (nested) {
				return "["+mode+";"+color+"m"+s;
			} else {
				return "["+mode+";"+color+"m"+s+"[0m";
			}
		}
	}

	public static String cyan(Object s)   { return color(s, NORMAL, CYAN); }
	public static String purple(Object s) { return color(s, NORMAL, PURPLE); }
	public static String blue(Object s)   { return color(s, NORMAL, BLUE); }
	public static String yellow(Object s) { return color(s, NORMAL, YELLOW); }
	public static String green(Object s)  { return color(s, NORMAL, GREEN); }
	public static String red(Object s)    { return color(s, NORMAL, RED); }
	public static String black(Object s)  { return color(s, NORMAL, BLACK); }
	public static String white(Object s)  { return color(s, NORMAL, WHITE); }

	public static String CYAN(Object s)   { return color(s, BRIGHT, CYAN); }
	public static String PURPLE(Object s) { return color(s, BRIGHT, PURPLE); }
	public static String BLUE(Object s)   { return color(s, BRIGHT, BLUE); }
	public static String YELLOW(Object s) { return color(s, BRIGHT, YELLOW); }
	public static String GREEN(Object s)  { return color(s, BRIGHT, GREEN); }
	public static String RED(Object s)    { return color(s, BRIGHT, RED); }
	public static String BLACK(Object s)  { return color(s, BRIGHT, BLACK); }
	public static String WHITE(Object s)  { return color(s, BRIGHT, WHITE); }
}


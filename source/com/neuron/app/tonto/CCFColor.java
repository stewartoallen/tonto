/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.awt.Color;
import java.awt.image.ColorModel;

/**
 * A representation of valid CCF Colors.
 */
public class CCFColor
{
	// ---( static fields )---
	public final static int BLACK      = 0;
	public final static int DARK_GRAY  = 1;
	public final static int LIGHT_GRAY = 2;
	public final static int WHITE      = 3;

	private static ColorModel cm = ColorModel.getRGBdefault();
	private static int grayTint;

	public static CCFColor[] defaultMap = {
		new CCFColor(0),   // black
		new CCFColor(251), // dark gray
		new CCFColor(245), // light gray
		new CCFColor(215), // white
	};

	private final static int[][] rgb = {
		// auto-gen'd -- see makeExtAwtColor
		{ 0,   0,   0   }, // 0
		{ 51,  0,   0   }, // 1
		{ 102, 0,   0   }, // 2
		{ 153, 0,   0   }, // 3
		{ 204, 0,   0   }, // 4
		{ 255, 0,   0   }, // 5
		{ 0,   51,  0   }, // 6
		{ 51,  51,  0   }, // 7
		{ 102, 51,  0   }, // 8
		{ 153, 51,  0   }, // 9
		{ 204, 51,  0   }, // 10
		{ 255, 51,  0   }, // 11
		{ 0,   102, 0   }, // 12
		{ 51,  102, 0   }, // 13
		{ 102, 102, 0   }, // 14
		{ 153, 102, 0   }, // 15
		{ 204, 102, 0   }, // 16
		{ 255, 102, 0   }, // 17
		{ 0,   153, 0   }, // 18
		{ 51,  153, 0   }, // 19
		{ 102, 153, 0   }, // 20
		{ 153, 153, 0   }, // 21
		{ 204, 153, 0   }, // 22
		{ 255, 153, 0   }, // 23
		{ 0,   204, 0   }, // 24
		{ 51,  204, 0   }, // 25
		{ 102, 204, 0   }, // 26
		{ 153, 204, 0   }, // 27
		{ 204, 204, 0   }, // 28
		{ 255, 204, 0   }, // 29
		{ 0,   255, 0   }, // 30
		{ 51,  255, 0   }, // 31
		{ 102, 255, 0   }, // 32
		{ 153, 255, 0   }, // 33
		{ 204, 255, 0   }, // 34
		{ 255, 255, 0   }, // 35
		{ 0,   0,   51  }, // 36
		{ 51,  0,   51  }, // 37
		{ 102, 0,   51  }, // 38
		{ 153, 0,   51  }, // 39
		{ 204, 0,   51  }, // 40
		{ 255, 0,   51  }, // 41
		{ 0,   51,  51  }, // 42
		{ 51,  51,  51  }, // 43
		{ 102, 51,  51  }, // 44
		{ 153, 51,  51  }, // 45
		{ 204, 51,  51  }, // 46
		{ 255, 51,  51  }, // 47
		{ 0,   102, 51  }, // 48
		{ 51,  102, 51  }, // 49
		{ 102, 102, 51  }, // 50
		{ 153, 102, 51  }, // 51
		{ 204, 102, 51  }, // 52
		{ 255, 102, 51  }, // 53
		{ 0,   153, 51  }, // 54
		{ 51,  153, 51  }, // 55
		{ 102, 153, 51  }, // 56
		{ 153, 153, 51  }, // 57
		{ 204, 153, 51  }, // 58
		{ 255, 153, 51  }, // 59
		{ 0,   204, 51  }, // 60
		{ 51,  204, 51  }, // 61
		{ 102, 204, 51  }, // 62
		{ 153, 204, 51  }, // 63
		{ 204, 204, 51  }, // 64
		{ 255, 204, 51  }, // 65
		{ 0,   255, 51  }, // 66
		{ 51,  255, 51  }, // 67
		{ 102, 255, 51  }, // 68
		{ 153, 255, 51  }, // 69
		{ 204, 255, 51  }, // 70
		{ 255, 255, 51  }, // 71
		{ 0,   0,   102 }, // 72
		{ 51,  0,   102 }, // 73
		{ 102, 0,   102 }, // 74
		{ 153, 0,   102 }, // 75
		{ 204, 0,   102 }, // 76
		{ 255, 0,   102 }, // 77
		{ 0,   51,  102 }, // 78
		{ 51,  51,  102 }, // 79
		{ 102, 51,  102 }, // 80
		{ 153, 51,  102 }, // 81
		{ 204, 51,  102 }, // 82
		{ 255, 51,  102 }, // 83
		{ 0,   102, 102 }, // 84
		{ 51,  102, 102 }, // 85
		{ 102, 102, 102 }, // 86
		{ 153, 102, 102 }, // 87
		{ 204, 102, 102 }, // 88
		{ 255, 102, 102 }, // 89
		{ 0,   153, 102 }, // 90
		{ 51,  153, 102 }, // 91
		{ 102, 153, 102 }, // 92
		{ 153, 153, 102 }, // 93
		{ 204, 153, 102 }, // 94
		{ 255, 153, 102 }, // 95
		{ 0,   204, 102 }, // 96
		{ 51,  204, 102 }, // 97
		{ 102, 204, 102 }, // 98
		{ 153, 204, 102 }, // 99
		{ 204, 204, 102 }, // 100
		{ 255, 204, 102 }, // 101
		{ 0,   255, 102 }, // 102
		{ 51,  255, 102 }, // 103
		{ 102, 255, 102 }, // 104
		{ 153, 255, 102 }, // 105
		{ 204, 255, 102 }, // 106
		{ 255, 255, 102 }, // 107
		{ 0,   0,   153 }, // 108
		{ 51,  0,   153 }, // 109
		{ 102, 0,   153 }, // 110
		{ 153, 0,   153 }, // 111
		{ 204, 0,   153 }, // 112
		{ 255, 0,   153 }, // 113
		{ 0,   51,  153 }, // 114
		{ 51,  51,  153 }, // 115
		{ 102, 51,  153 }, // 116
		{ 153, 51,  153 }, // 117
		{ 204, 51,  153 }, // 118
		{ 255, 51,  153 }, // 119
		{ 0,   102, 153 }, // 120
		{ 51,  102, 153 }, // 121
		{ 102, 102, 153 }, // 122
		{ 153, 102, 153 }, // 123
		{ 204, 102, 153 }, // 124
		{ 255, 102, 153 }, // 125
		{ 0,   153, 153 }, // 126
		{ 51,  153, 153 }, // 127
		{ 102, 153, 153 }, // 128
		{ 153, 153, 153 }, // 129
		{ 204, 153, 153 }, // 130
		{ 255, 153, 153 }, // 131
		{ 0,   204, 153 }, // 132
		{ 51,  204, 153 }, // 133
		{ 102, 204, 153 }, // 134
		{ 153, 204, 153 }, // 135
		{ 204, 204, 153 }, // 136
		{ 255, 204, 153 }, // 137
		{ 0,   255, 153 }, // 138
		{ 51,  255, 153 }, // 139
		{ 102, 255, 153 }, // 140
		{ 153, 255, 153 }, // 141
		{ 204, 255, 153 }, // 142
		{ 255, 255, 153 }, // 143
		{ 0,   0,   204 }, // 144
		{ 51,  0,   204 }, // 145
		{ 102, 0,   204 }, // 146
		{ 153, 0,   204 }, // 147
		{ 204, 0,   204 }, // 148
		{ 255, 0,   204 }, // 149
		{ 0,   51,  204 }, // 150
		{ 51,  51,  204 }, // 151
		{ 102, 51,  204 }, // 152
		{ 153, 51,  204 }, // 153
		{ 204, 51,  204 }, // 154
		{ 255, 51,  204 }, // 155
		{ 0,   102, 204 }, // 156
		{ 51,  102, 204 }, // 157
		{ 102, 102, 204 }, // 158
		{ 153, 102, 204 }, // 159
		{ 204, 102, 204 }, // 160
		{ 255, 102, 204 }, // 161
		{ 0,   153, 204 }, // 162
		{ 51,  153, 204 }, // 163
		{ 102, 153, 204 }, // 164
		{ 153, 153, 204 }, // 165
		{ 204, 153, 204 }, // 166
		{ 255, 153, 204 }, // 167
		{ 0,   204, 204 }, // 168
		{ 51,  204, 204 }, // 169
		{ 102, 204, 204 }, // 170
		{ 153, 204, 204 }, // 171
		{ 204, 204, 204 }, // 172
		{ 255, 204, 204 }, // 173
		{ 0,   255, 204 }, // 174
		{ 51,  255, 204 }, // 175
		{ 102, 255, 204 }, // 176
		{ 153, 255, 204 }, // 177
		{ 204, 255, 204 }, // 178
		{ 255, 255, 204 }, // 179
		{ 0,   0,   255 }, // 180
		{ 51,  0,   255 }, // 181
		{ 102, 0,   255 }, // 182
		{ 153, 0,   255 }, // 183
		{ 204, 0,   255 }, // 184
		{ 255, 0,   255 }, // 185
		{ 0,   51,  255 }, // 186
		{ 51,  51,  255 }, // 187
		{ 102, 51,  255 }, // 188
		{ 153, 51,  255 }, // 189
		{ 204, 51,  255 }, // 190
		{ 255, 51,  255 }, // 191
		{ 0,   102, 255 }, // 192
		{ 51,  102, 255 }, // 193
		{ 102, 102, 255 }, // 194
		{ 153, 102, 255 }, // 195
		{ 204, 102, 255 }, // 196
		{ 255, 102, 255 }, // 197
		{ 0,   153, 255 }, // 198
		{ 51,  153, 255 }, // 199
		{ 102, 153, 255 }, // 200
		{ 153, 153, 255 }, // 201
		{ 204, 153, 255 }, // 202
		{ 255, 153, 255 }, // 203
		{ 0,   204, 255 }, // 204
		{ 51,  204, 255 }, // 205
		{ 102, 204, 255 }, // 206
		{ 153, 204, 255 }, // 207
		{ 204, 204, 255 }, // 208
		{ 255, 204, 255 }, // 209
		{ 0,   255, 255 }, // 210
		{ 51,  255, 255 }, // 211
		{ 102, 255, 255 }, // 212
		{ 153, 255, 255 }, // 213
		{ 204, 255, 255 }, // 214
		{ 255, 255, 255 }, // 215
		// web safe colors
		{ 192, 208, 240 }, // 216
		{ 144, 176, 240 }, // 217
		{ 112, 144, 208 }, // 218
		{ 80,  112, 208 }, // 219
		{ 48,  96,  224 }, // 220
		{ 64,  80,  224 }, // 221
		{ 32,  64,  192 }, // 222
		{ 32,  48,  176 }, // 223
		{ 160, 160, 208 }, // 224
		{ 112, 112, 160 }, // 225
		{ 80,  80,  144 }, // 226
		{ 48,  64,  144 }, // 227
		{ 48,  48,  128 }, // 228
		{ 32,  32,  112 }, // 229
		{ 16,  16,  112 }, // 230
		{ 16,  16,  80  }, // 231
		{ 208, 176, 208 }, // 232
		{ 176, 144, 176 }, // 233
		{ 144, 128, 144 }, // 234
		{ 160, 128, 160 }, // 235
		{ 144, 128, 144 }, // 236
		{ 144, 112, 144 }, // 237
		{ 128, 112, 128 }, // 238
		{ 128, 96,  128 }, // 239
		{ 255, 255, 255 }, // 240
		{ 240, 240, 240 }, // 241
		{ 224, 224, 224 }, // 242
		{ 208, 208, 208 }, // 243
		{ 192, 192, 192 }, // 244
		{ 176, 176, 176 }, // 245
		{ 160, 160, 160 }, // 246
		{ 144, 144, 144 }, // 247
		{ 128, 128, 128 }, // 248
		{ 112, 112, 112 }, // 249
		{ 96,  96,  96  }, // 250
		{ 80,  80,  80  }, // 251
		{ 64,  64,  64  }, // 252
		{ 48,  48,  48  }, // 253
		{ 32,  32,  32  }, // 254
		{ 16,  16,  16  }  // 255
	};

	private final static Color[] model = new Color[rgb.length];
	private final static Color[] oldModel = new Color[4];
	private       static Color[] tintModel = oldModel;

	static
	{
		for (int i=0; i<model.length; i++)
		{
			model[i] = new Color(rgb[i][0], rgb[i][1], rgb[i][2]);
		}
		oldModel[0] = model[0];
		oldModel[1] = model[251];
		oldModel[2] = model[245];
		oldModel[3] = model[215];
	}

	// ---( static methods )---

	// ---( constructors )---
	// color = 0-255
	CCFColor (int color)
	{
		this.color = color & 0xff;
	}

	public String toString()
	{
		return "Color["+color+"]";
	}

	// ---( instance fields )---
	private int color;
	// ---( public API )---
	public static int getGrayTint()
	{
		return grayTint;
	}

	public static void setGrayTint(int tint)
	{
		grayTint = tint;
		Color c = new Color(255-tint, 255, 255-tint);
		int r = c.getRed();
		int g = c.getGreen();
		int b = c.getBlue();
		tintModel = new Color[4];
		for (int i=0; i<oldModel.length; i++)
		{
			Color gc = oldModel[i];
			int rw = (gc.getRed()*r)/255;
			int gw = (gc.getGreen()*g)/255;
			int bw = (gc.getBlue()*b)/255;
			tintModel[i] = new Color(rw,gw,bw);
		}
	}

	public static int rgbIndexToGrayIndex(int val)
	{
		return rgbToGrayIndex(model[val].getRGB());
	}

	public static int rgbToGrayIndex(int val)
	{
		return (int)(((
			(cm.getRed(val)*30)+(cm.getGreen(val)*59)+
			(cm.getBlue(val)*11))/6400));
	}

	public static int grayToRGB(int gray, int rgb[])
	{
		return grayIndexToRGB(rgbToGrayIndex(gray), rgb);
	}

	public static int grayIndexToRGB(int gray, int rgb[])
	{
		return rgb[gray&0x03];
	}

	/**
	 * Return a suitable AWT Color to match this CCF Color.
	 */
	public Color getAWTColor(boolean ext)
	{
		return ext ? model[color] : tintModel[color&0x03];
	}

	public CCFColor makeGray()
	{
		return new CCFColor(rgbIndexToGrayIndex(color));
	}

	public CCFColor makeColor(int rgbindex[])
	{
		return new CCFColor(rgbindex[color&0x03]);
	}

	// ---( utility methods )---
	public int getColorIndex()
	{
		return color;
	}

	int getCCFBackground(boolean ext)
	{
		if (ext)
		{
			return (color << 0) | (color << 8) | (color << 16);
		}
		else
		{
			return (color << 2) | (color << 4) | (color << 6);
		}
	}

	int getCCFForeground(boolean ext)
	{
		return ext ? ((color & 0xff) << 24) : (color & 0x3);
	}

	static Color getAWTColor(int color, boolean ext)
	{
		return ext ? model[color&0xff] : tintModel[color&0x03];
	}

	public static Color[] getColorModel(boolean ext)
	{
		return ext ? model : oldModel;
	}

	// return CCF int equivalent of fg/bg combo
	static int getComposite(CCFColor fg, CCFColor bg, boolean ext)
	{
		return bg.getCCFBackground(ext) | fg.getCCFForeground(ext);
	}

	// return CCFColor by index
	// color = 0-255
	public static CCFColor getColor(int color)
	{
		return new CCFColor(color);
	}

	static CCFColor getNamedColor(int name, boolean ext)
	{
		if (ext)
		{
			switch (name)
			{
				case BLACK: return new CCFColor(0);
				case DARK_GRAY: return new CCFColor(252);
				case LIGHT_GRAY: return new CCFColor(244);
				case WHITE: 
				default: return new CCFColor(215);
			}
		}
		else
		{
			switch (name)
			{
				case BLACK: return new CCFColor(0);
				case DARK_GRAY: return new CCFColor(1);
				case LIGHT_GRAY: return new CCFColor(2);
				case WHITE: 
				default: return new CCFColor(3);
			}
		}
	}

	// return background CCFColor by masking int color
	static int getBackgroundIndex(int color, boolean ext)
	{
		return ext ? (color & 0xff) : ((color >> 2)&0x03);
	}

	static CCFColor getBackground(int color, boolean ext)
	{
		return getColor(getBackgroundIndex(color, ext));
	}

	// return foreground CCFColor by masking int color
	static int getForegroundIndex(int color, boolean ext)
	{
		return ext ? ((color >> 24) & 0xff) : (color & 0x03);
	}

	static CCFColor getForeground(int color, boolean ext)
	{
		return getColor(getForegroundIndex(color, ext));
	}

	static int ccfGrayToRGB(int gray, int rgb[])
	{
		return getComposite(
			getForeground(gray,false).makeColor(rgb),
			getBackground(gray,false).makeColor(rgb),true);
	}

	static int ccfRGBToGray(int color)
	{
		return getComposite(
			getForeground(color,true).makeGray(),
			getBackground(color,true).makeGray(),false);
	}

	/**
	 * @return color index that most closely matches RGB value.
	 */
	public static int getColorFromRGB(int r, int g, int b)
	{
		int best = -1;
		int diff = -1;
		for (int i=0; i<rgb.length; i++)
		{
			int rd = Math.abs(r-rgb[i][0]);
			int gd = Math.abs(g-rgb[i][1]);
			int bd = Math.abs(b-rgb[i][2]);
			if (rd <= 16 && gd <= 16 && bd <= 16)
			{
				int nd = (rd * gd * bd);
				if (diff < 0 || nd <= diff)
				{
					diff = nd;
					best = i;
				}
			}
		}
		if (best >= 0)
		{
			return best;
		}
		return (r/51) + ((g/51)*6) + ((b/51)*36);
	}

	// ---( interface methods )---
}


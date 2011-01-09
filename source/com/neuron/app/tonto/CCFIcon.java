/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.util.zip.*;
import javax.swing.*;
import com.neuron.io.ByteOutputBuffer;
import com.neuron.app.tonto.ui.PNGImageProducer;
import osbaldeston.image.BMP;

/**
 * CCF Icons come in three varieties: 2, 4 and 256 color.
 */
public class CCFIcon extends CCFNode
{
	public final static int MODE_2BIT  = 1;
	public final static int MODE_8BIT  = 2;
	public final static int MODE_32BIT = 3;

	private static Hashtable cache = new Hashtable();

	// type bit fields
	//  +------------------ compressed
	//  |  ++-------------- background (0-3)
	//  |  ||++------------ foreground (0-3)
	//  |  ||||+-----------   4 color
	//  |  ||||| +---------  32 bit color w/ alpha (custom)
	//  |  ||||| |      +-- 256 color
	//  |  ||||| |      |
	// [00000000,00000000]

	private final static int FG_MASK    = (1 <<  9) | (1 << 10);
	private final static int BG_MASK    = (1 << 11) | (1 << 12);
	private final static int COLOR_4    = (1 <<  8);
	private final static int COLOR_256  = (1 <<  0) | FG_MASK | COLOR_4;
	private final static int COLOR_FULL = (1 <<  7) | COLOR_256;
	private final static int COMPRESS   = (1 << 15);

	private final static String[][] codec_normal =
	{
		{ "N2", "size", "+" },
		{ "N2", "width" },
		{ "N2", "height" },
		{ "N2", "type", "+" },
		{ "B+", "data", "size", "-8" },
	};

	private final static String[][] codec_custom =
	{
		{ "N4", "size" },
		{ "N2", "width" },
		{ "N2", "height" },
		{ "N2", "type", "+" },
		{ "B+", "data", "size", "-10" },
	};

	private static String[][] codec = codec_normal;

	// ---( instance fields )---
	int    size;
	int    width;
	int    height;
	int    type;		// bg=[00011000][]  fg=[000001100][]  cmp=[10000000][]
	byte   data[];		// NEVER set directly!

	private Icon icon;
	private ImageProducer image;

	// ---( constructors )----
	CCFIcon()
	{
	}

	CCFIcon(CCFNode parent)
	{
		setParent(parent);
	}

	int getLength()
	{
		int l = super.getLength();
//System.out.println(" >> ("+this+") LEN = "+l);
		return l;
	}

	public String toString()
	{
		return "Icon"+get16Bits(type)+"("+width+"x"+height+")="+size;
	}

	// ---( utility methods )---
	static int getGray(int c)
	{
		return CCFColor.getAWTColor(c,false).getRGB();
	}

	static int getRGB(int c)
	{
		return CCFColor.getAWTColor(c,true).getRGB();
	}

	// ---( public API )----
	/**
	 * Create a CCFIcon from a file containing a GIF, BMP or JPG image.
	 *
	 * @param file file containing a JPG, GIF or BMP image
	 * @param mode true to create color icon (TSU-6000 only)
	 */
	public static CCFIcon create(String file, int mode)
		throws IOException
	{
		return create(Util.readFile(file), mode);
	}

	/**
	 * Create a CCFIcon from a stream containing a GIF, JPG or BMP image.
	 *
	 * @param stream stream containing a JPG, GIF or BMP image
	 * @param mode true to create color icon (TSU-6000 only)
	 */
	public static CCFIcon create(InputStream stream, int mode)
		throws IOException
	{
		return create(Util.readFully(stream), mode);
	}

	/**
	 * Create a CCFIcon from a byte array containing a GIF, JPG or BMP image.
	 *
	 * @param data byte array containing a JPG, GIF or BMP image
	 * @param mode true to create color icon (TSU-6000 only)
	 */
	public static CCFIcon create(byte data[], int mode)
		throws IOException
	{
		// BMP
		if (data[0] == 'B' && data[1] == 'M')
		{
			return create(new BMP(new ByteArrayInputStream(data)).getImage(), mode);
		}
		else
		// PNG
		if (data[0] == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G')
		{
			return create(Toolkit.getDefaultToolkit().createImage(
				new PNGImageProducer(new ByteArrayInputStream(data))), mode);
		}
		else
		// default to Java's toolkit creator
		{
			return create(Toolkit.getDefaultToolkit().createImage(data), mode);
		}
	}

	/**
	 * Create a CCFIcon from an AWT Image.
	 *
	 * @param image file containing GIF or JPEG image
	 * @param mode ccf type
	 */
	public static CCFIcon create(Image image, int mode)
	{
		CCFIcon find = (CCFIcon)cache.get(image);
		if (find != null)
		{
			return find;
		}
		find = new CCFIcon();
		find.setFromImage(image, mode, false);
		cache.put(image, find);
		return find;
	}

	/**
	 * Save Icon as a GIF image to a file.
	 */
	public void saveGIF(String file)
		throws IOException
	{
		FileOutputStream fo = new FileOutputStream(file);
		saveGIF(fo);
		fo.close();
	}

	/**
	 * Save Icon as a GIF image to a stream.
	 */
	public void saveGIF(OutputStream stream)
		throws IOException
	{
		new Acme.JPM.Encoders.GifEncoder(getImageProducer(), stream).encode();
	}

	/**
	 * Return the dimensions of this Icon.
	 */
	public java.awt.Dimension getSize()
	{
		return new java.awt.Dimension(width, height);
	}

	// ---( abstract methods )---
	void checkVersion()
	{
		if (getHeader().isCustom())
		{
			codec = codec_custom;
			size = (data != null) ? data.length + 10 : 10;
		}
		else
		{
			codec = codec_normal;
			size = (data != null) ? data.length + 8 : 8;
		}
//System.out.println("CCFIcon :: checkVersion custom="+(codec == codec_custom)+" >> "+this);
	}

	void preEncode(CCFNodeState zs)
	{
		// TODO: if ccf is not custom, make color or gray
		// TODO: if ccf is not color, make gray
		checkVersion();
	}

	void preDecode(CCFNodeState zs)
	{
		checkVersion();
	}

	void postDecode(CCFNodeState zs)
	{
		if (data == null)
		{
			throw new NullPointerException("icon data is null");
		}
		width = width & 0xffff;
		height = height & 0xffff;
		final int mw = 2000;
		final int mh = 2000;
		if (width > mw || height > mh)
		{
			log(1, "trimming oversized icon: "+describe());
			width = Math.min(width, mw);
			height = Math.min(height, mh);
		}
//System.out.println("CCFIcon :: postDecode w="+width+" h="+height+" type="+get16Bits(type)+" len="+data.length+" size="+size+" custom="+(codec == codec_custom));
		/*
		if (getHeader().hasColor())
		{
			convertToColor();
		}
		else
		{
			convertToGray();
		}
		*/
	}

	String[][] getEncodeTable()
	{
		return codec;
	}

	String[][] getDecodeTable()
	{
		return codec;
	}	
	
	void buildTree(CCFNode z)
	{
	}

	// ---( override methods )---
	String describe()
	{
		return "Icon,"+width+"x"+height+","+get16Bits(type)+","+
			(data != null ? data.length : -1)+",("+
			(isCompressed() ? "compressed " : "")+
			(isFullColor() ? "fullColor" : isColor() ? "color" : isGray() ? "gray" : "b&w")+")";
			
	}

	// ---( instance methods )---
	private void setImage(int w, int h, byte d[], boolean compr, boolean color)
	{
		setImage(w, h, d, (compr ? COMPRESS : 0) | (color ? COLOR_256 : COLOR_4));
	}

	private void setImage(int w, int h, byte d[], int t)
	{
//System.out.println("CCFIcon :: setImage("+w+","+h+","+d.length+","+t+")");
		width = w;
		height = h;
		type = t;
		setData(d);
	}

	private void setFullImage(int w, int h, byte d[], boolean color)
	{
		width = w;
		height = h;
		type = COLOR_FULL;
		setData(d);
	}

	private void setData(byte d[])
	{
		data = d;
//		size = d.length + 8;
		size = d.length + (isFullColor() ? 10 : 8);
	}

	public void setCompressed(boolean c)
	{
		if (!isCompressible() || isCompressed() == c)
		{
			return;
		}
//System.out.println("CCFIcon :: setCompressed("+c+")");
		setFromImage(
			Toolkit.getDefaultToolkit().createImage(getImageProducer()),
			isFullColor() ? MODE_32BIT : isColor() ? MODE_8BIT : MODE_2BIT, c);
	}

	public boolean isCompressible()
	{
		// TODO: fix color compression // return isGray() || isColor();
		return isFullColor() || (isGray() && !isColor());
	}

	public boolean isCompressed()
	{
		return (type & COMPRESS) == COMPRESS;
	}

	public boolean isFullColor()
	{
		return (type & COLOR_FULL) == COLOR_FULL;
	}

	public boolean isColor()
	{
		return (type & COLOR_256) == COLOR_256;
	}

	public boolean isGray()
	{
		return (type & COLOR_4) == COLOR_4;
	}

	public boolean isBW()
	{
		return !(isGray() || isColor() || isFullColor());
	}

	public void clearCache()
	{
		icon = null;
		image = null;
	}

	public void convertToGray()
	{
		if (!isColor())
		{
			return;
		}
//System.out.println("CCFIcon :: convertToGray :: "+this);
		debug.log(3,"converting "+this+" to gray");
		create4Color(width, height, getRGBImage(), false);
		clearCache();
	}

	public void convertToColor()
	{
		if (isColor())
		{
			return;
		}
//System.out.println("CCFIcon :: convertToColor :: "+this);
		CCFColor c[] = getHeader().getColorMap();
		debug.log(3,"converting "+this+" to color");
		int img[] = getRGBImage();
		int rgb[] = new int[] {
			c[0].getAWTColor(true).getRGB(),
			c[1].getAWTColor(true).getRGB(),
			c[2].getAWTColor(true).getRGB(),
			c[3].getAWTColor(true).getRGB(),
		};
		for (int i=0; i<img.length; i++)
		{
			int oc = img[i];
			img[i] = CCFColor.grayToRGB(img[i], rgb);
		}
		create256Color(width, height, img, false);
		clearCache();
	}

	public Icon getIcon(Component c)
	{
		if (icon == null)
		{
			icon = new ImageIcon(c.createImage(getImageProducer()));
		}
		return icon;
	}

	/*
	public Icon getScaledIcon(Component c, int w, int h)
	{
		return new ImageIcon(c.createImage(getImage()).getScaledInstance(w,h,Image.SCALE_FAST));
	}
	*/

	Image getImage(Component c)
	{
		return c.createImage(getImageProducer());
	}

	ImageProducer getImageProducer()
	{
		if (image == null)
		{
			image = new MemoryImageSource(width, height, getRGBImage(), 0, width);
		}
		return image;
	}

	public static CCFIcon composite(CCFIcon src, CCFIcon dst)
	{
		if (src == null)
		{
			return dst;
		}
//System.out.println("CCFIcon :: composite");
		int w = src.width;
		int h = src.height;
		int s[] = src.getRGBImage();
		int d[] = dst.getRGBImage();
		int max = Math.min(s.length, d.length);
		int tp = s[0];
		for (int i=0; i<max; i++)
		{
			if (s[i] == tp)
			{
				s[i] = d[i];
			}
		}
		CCFIcon ni = new CCFIcon();
		switch (src.type & COLOR_FULL)
		{
			case COLOR_FULL:
				ni.createFullColor(w,h,s);
				break;
			case COLOR_256:
				ni.create256Color(w,h,s,false);
				break;
			case COLOR_4:
				ni.create4Color(w,h,s,false);
				break;
			default:
//System.out.println("unmatched type : "+src.type);
				break;
		}
		/*
		if ((src.type & COLOR_256) == COLOR_256)
		{
			ni.create256Color(w,h,s,false);
		}
		else
		{
			ni.create4Color(w,h,s,false);
		}
		*/
		return ni;
	}

	int[] getRGBImage()
	{
//System.out.println("CCFIcon :: getRGBImage");
		switch (type & COLOR_FULL)
		{
			case COLOR_FULL:
				return loadFullColor(isCompressed());
			case COLOR_256:
				return load256Color(isCompressed());
			case COLOR_4:
				return load4Color(isCompressed());
			default:
//System.out.println("unmatched type : "+type);
				return load2Color((type >>  9) & 0x3, (type >> 11) & 0x3);
		}
		/*
		if ((type & COLOR_256) == COLOR_256)
		{
			return load256Color(isCompressed());
		}
		else
		if ((type & COLOR_4) == COLOR_4)
		{
			return load4Color(isCompressed());
		}
		else
		{
			int fg = (type >>  9) & 0x3;
			int bg = (type >> 11) & 0x3;
			return load2Color(fg,bg);
		}
		*/
	}

	private int rgbToGrayIndex(int val)
	{
		return CCFColor.rgbToGrayIndex(val);
	}

	// --( set internal data from Image )------------------------------------

	void setFromImage(Image i, int mode, boolean compress)
	{
//System.out.println("CCFIcon :: setFromImage: mode="+mode+" comp="+compress);
		if (i == null)
		{
			debug.log(0, "unable to load!");
		}
		else
		{
			MediaTracker m = new MediaTracker(new Frame());
			m.addImage(i, 1);
			try { m.waitForAll(); } catch (Throwable t) { t.printStackTrace(); }
			int w = i.getWidth(null);
			int h = i.getHeight(null);
			int img[] = new int[w*h];
			PixelGrabber pg = new PixelGrabber(i, 0, 0, w, h, img, 0, w);
			try
			{
				pg.grabPixels();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			if ((pg.getStatus() & ImageObserver.ABORT) != 0)
			{
				throw new RuntimeException("image load aborted");
			}
			switch (mode)
			{
				case MODE_2BIT:
					create4Color(w, h, img, compress);
					break;
				case MODE_8BIT:
					create256Color(w, h, img, compress);
					break;
				case MODE_32BIT:
					createFullColor(w, h, img);
					break;
				default:
//System.out.println("unmatched type : "+type);
					break;
			}
		}
		clearCache();
	}

	// --( creators )--------------------------------------------------------

	// create type 1 uncompressed (four color) from RGB source
	private void create4Color(int w, int h, int img[], boolean compr)
	{
		if (compr)
		{
			create4ColorCompressed(w, h, img);
			return;
		}
		ColorModel cm = ColorModel.getRGBdefault();
		int lineWidth = (w / 4) + (w % 4 > 0 ? 1 : 0);
		int linePad = lineWidth % 2;
		lineWidth += linePad;
		int bitWidth = lineWidth * 4;
		byte d[] = new byte[lineWidth * h];
		for (int i=0; i<img.length; i++)
		{
			int col = i % w;
			int pos = ((i/w)*bitWidth+col)/4;
			int gray = rgbToGrayIndex(img[i]);
			d[pos] |= ((gray & 0x3) << ((3-(col%4))*2));
		}
		setImage(w, h, d, false, false);
	}

	// create type 1 compressed (four color) from RGB source
	private void create4ColorCompressed(int w, int h, int img[])
	{
		int pos = 0;
		byte d[] = new byte[w*h/2];
		for (int i=0; i<img.length; pos++)
		{
			int val = img[i];
			if (i+3 < img.length)
			{
				if (val == img[i+1] && val == img[i+2] && val == img[i+3])
				{
					int j = i+3;
					for (; j<img.length && j-i<32 && img[j] == val; j++)
						;
					d[pos] = (byte)(0x80 | rgbToGrayIndex(val) | ((j-i-4)<<2));
					i = j;
					continue;
				}
			}
			int j = 0;
			for (; j<3 && j+i<img.length; j++)
			{
				d[pos] |= (rgbToGrayIndex(img[i+j]) << ((2-j)*2));
			}
			i += j;
		}
		pos += (pos % 2);
		byte nd[] = new byte[pos];
		System.arraycopy(d, 0, nd, 0, pos);
		setImage(w, h, nd, true, false);
	}

	// create type 2 uncompressed (256 color) from RGB source
	private void create256Color(int w, int h, int img[], boolean compr)
	{
		if (compr)
		{
			create256ColorCompressed(w, h, img);
			return;
		}
		ColorModel cm = ColorModel.getRGBdefault();
		int lpad = img.length % 16;
		byte d[] = new byte[img.length+(lpad > 0 ? (16-lpad) : 0)];
		for (int i=0; i<img.length; i++)
		{
			d[i] = (byte)CCFColor.getColorFromRGB(
				cm.getRed(img[i]),
				cm.getGreen(img[i]),
				cm.getBlue(img[i])
			);
		}
		setImage(w, h, d, false, true);
	}

	// create type 2 compressed (256 color) from RGB source
	// TODO: this appears to be buggy
	private void create256ColorCompressed(int w, int h, int img[])
	{
		//debug.log(0, "create compressed color: "+w+"x"+h+" imglen="+img.length);
		ColorModel cm = ColorModel.getRGBdefault();
		//int lpad = img.length % 16;
		//byte d[] = new byte[img.length+(lpad > 0 ? (16-lpad) : 0)];
		byte d[] = new byte[img.length];
		for (int i=0; i<img.length; i++)
		{
			d[i] = (byte)CCFColor.getColorFromRGB(
				cm.getRed(img[i]),
				cm.getGreen(img[i]),
				cm.getBlue(img[i])
			);
		}
		ByteOutputBuffer bob = new ByteOutputBuffer();
		boolean odd = img.length % 2 == 1;
		int pos = 0;
		int max = img.length - (odd ? 9 : 8);

		try {

		int x = 0;
		while (pos < img.length)
		{
			Segment s1 = new Segment(d, pos, max);
			Segment s2 = new Segment(d, pos+s1.len, max);
			if ((s1.rle || s2.rle) && s1.len > 0 && s2.len > 0)
			{
				//debug.log(0, x+"  segment: rle="+(s1.rle?"1":"0")+" rle="+(s2.rle?"1":"0")+" "+s1.len+","+s2.len);
				bob.write((byte)((s1.rle ? 0x00 : 0x80)+s1.len/2));
				bob.write((byte)((s2.rle ? 0x00 : 0x80)+s2.len/2));
				s1.write(bob);
				s2.write(bob);
				pos += (s1.len + s2.len);
			}
			else
			{
				int zlen = s1.len+s2.len;
				if (zlen > 4)
				{
					//debug.log(0, x+"  segment: rle=0 "+zlen+" @ "+pos);
					bob.write(0xff);
					bob.write((zlen/2) + 1);
					for (int i=pos; i<zlen; i++)
					{
						bob.write(d[i]);
					}
					pos += zlen;
					if (pos < max)
					{
						continue;
					}
				}

				if (odd)
				{
					//debug.log(0, x+"  segment: rle=0 + odd byte @ "+pos+" end");
					bob.write(0x80 + (img.length - pos)/2);
					bob.write(0x81);
				}
				else
				{
					//debug.log(0, x+"  segment: rle=0 "+(d.length-pos)+" @ "+pos+" end");
					bob.write(0xff);
					bob.write(((img.length - pos)/2) + 1);
				}
				for (int i=pos; i<img.length; i++)
				{
					bob.write(d[i]);
				}
				//debug.log(0, (x+1)+"  segment: eof");
				bob.write(0);
				bob.write(0);
				break;
			}
			x++;
		}

		File tf = File.createTempFile("tonto-color", ".comp");
		FileOutputStream fo = new FileOutputStream(tf);
		fo.write(bob.toByteArray());
		int rem = bob.size() % 16;
		if (rem >  0)
		{
			bob.write(new byte[16-rem]);
		}
		fo.close();

		} catch (Exception ex) { ex.printStackTrace(); System.exit(0); }

		setImage(w, h, bob.toByteArray(), true, true);
	}


	// create custom 32 bit color from RGB source
	private void createFullColor(int w, int h, int img[])
	{
		byte b[] = new byte[img.length*4];
		for (int i=0; i<img.length; i++)
		{
			b[i*4+0] = (byte)((img[i] >>> 24) & 0xff);
			b[i*4+1] = (byte)((img[i] >>> 16) & 0xff);
			b[i*4+2] = (byte)((img[i] >>>  8) & 0xff);
			b[i*4+3] = (byte)((img[i] >>>  0) & 0xff);
		}
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		try
		{
		GZIPOutputStream gz = new GZIPOutputStream(bo);
		gz.write(b);
		gz.close();
		bo.close();
		}
		catch (Exception ex) { ex.printStackTrace(); }
		setImage(w, h, bo.toByteArray(), COLOR_FULL | COMPRESS);
	}

	// --( loaders )---------------------------------------------------------

	// get two color RGB from native image
	int[] load2Color(int fg, int bg)
	{
//System.out.println("load2Color :: "+this);
		int b[] = new int[width*height];
		int realWidth = (data.length/height)*8;
		int padWidth = realWidth - width;
		if (data == null || data.length == 0)
		{
//System.out.println("data is null or empty :: "+this);
			return b;
		}
		for (int i=0; i<b.length; i++)
		{
			int col = i%width;
			int pos = ((i/width)*realWidth+col)/8;
			int val = ((data[pos] >> (7-col%8)) & 0x1) == 1 ? bg : fg;
			b[i] = getGray(val);
		}
		return b;
	}

	// get four color RGB from native image
	int[] load4Color(boolean compr)
	{
		int b[] = new int[width*height];
		if (data == null || data.length == 0)
		{
//System.out.println("data is null or empty :: "+this);
			return b;
		}
		if (compr)
		{
			int pos = 0;
			for (int i=0; i<data.length; i++)
			{
				int d = data[i];
				if ((d & 0x80) > 0)
				{
					int color = (d & 0x3);
					int count = ((d >> 2) & 0x1f) + 4;
					for (int j=0; j<count && pos<b.length; j++)
					{
						b[pos++] = getGray(color);
					}
				}
				else
				{
					if (pos < b.length-3)
					{
						b[pos++] = getGray(((d >> 4) & 0x3));
						b[pos++] = getGray(((d >> 2) & 0x3));
						b[pos++] = getGray(((d >> 0) & 0x3));
					}
				}
			}
		}
		else
		{
			int realWidth = (data.length*4)/height;
			int padWidth = realWidth - width;
			for (int i=0; i<b.length; i++)
			{
				int col = i%width;
				int pos = ((i/width)*realWidth+col)/4;
				if (pos >= data.length)
				{
					// TODO: bad ... this should never happen
					break;
				}
				int val = (data[pos] >> ((3-(col%4))*2)) & 0x3;
				b[i] = getGray(val);
			}
		}
		return b;
	}

	// get 256 (8-bit) color RGB image from native image
	int[] load256Color(boolean comp)
	{
		if (comp)
		{
			return load256ColorCompressed();
		}
		int b[] = new int[width*height];
		if (data == null || data.length == 0)
		{
//System.out.println("data is null or empty :: "+this);
			return b;
		}
		if (b.length != data.length)
		{
//System.out.println("array size mismatch "+b.length+" != "+data.length);
			if (data.length < b.length)
			{
				new Exception("array too small "+b.length+" > "+data.length).printStackTrace();
				return b;
			}
		}
		for (int i=0; i<b.length; i++)
		{
			b[i] = getRGB(data[i]&0xff);
		}
		return b;
	}

	// get 256 (8-bit) color RGB image from compressed native image
	int[] load256ColorCompressed()
	{
		//debug.log(0, "get color compressed: "+width+"x"+height+" data="+data.length);
		int len = width*height;
		int lpad = len % 16;
		int b[] = new int[len + 16-lpad];
		int inpos = 0;
		int outpos = 0;
		int x = 0;

		if (data == null || data.length == 0)
		{
//System.out.println("data is null or empty :: "+this);
			return b;
		}

		try {

		while (outpos < b.length)
		{
			int b1 = data[inpos++] & 0xff;
			int b2 = data[inpos++] & 0xff;
			if (b1 == 0 && b2 == 0)
			{
				//debug.log(0, (x++)+"   segment: eof");
				break;
			}
			else
			if (b1 == 0xff && b2 > 0)
			{
				b2 = (b2 - 1)*2;
				//debug.log(0, (x++)+"   segment: rle=0 "+b2);
				for (int i=0; i<b2; i++)
				{
					b[outpos++] = getRGB(data[inpos++]);
				}
			}
			else
			if (b1 > 0x7f && b2 == 0x81)
			{
				b1 = (b1-0x80)*2;
				//debug.log(0, (x++)+"   segment: !rle len="+b1+" odd byte");
				for (int i=0; i<b1; i++)
				{
					b[outpos++] = getRGB(data[inpos++]);
				}
				b[outpos++] = getRGB(data[inpos++]);
			}
			else
			if (b1 > 0x80 && b2 < 0x80)
			{
				b1 = (b1-0x80)*2;
				for (int i=0; i<b1; i++)
				{
					b[outpos++] = getRGB(data[inpos++]);
				}
				if (b2 > 0)
				{
					int d1 = getRGB(data[inpos++]);
					int d2 = getRGB(data[inpos++]);
					for (int i=0; i<b2; i++)
					{
						b[outpos++] = d1;
						b[outpos++] = d2;
					}
				}
				//debug.log(0, (x++)+"   segment: rle=0 rle=1 "+b1+" "+(b2*2));
			}
			else
			if (b1 < 0x80 && b2 > 0x80)
			{
				if (b1 > 0)
				{
					int d1 = getRGB(data[inpos++]);
					int d2 = getRGB(data[inpos++]);
					for (int i=0; i<b1; i++)
					{
						b[outpos++] = d1;
						b[outpos++] = d2;
					}
				}
				b2 = (b2-0x80)*2;
				for (int i=0; i<b2; i++)
				{
					b[outpos++] = getRGB(data[inpos++]);
				}
				//debug.log(0, (x++)+"   segment: rle=1 rle=0 "+(b1*2)+" "+b2);
			}
			else
			if (b1 < 0x80 && b2 < 0x80)
			{
				//debug.log(0, (x++)+"   segment: rle=1 rle=1 "+(b1*2)+" "+(b2*2));
				if (b1 > 0)
				{
					int d1 = getRGB(data[inpos++]);
					int d2 = getRGB(data[inpos++]);
					for (int i=0; i<b1; i++)
					{
						b[outpos++] = d1;
						b[outpos++] = d2;
					}
				}
				if (b2 > 0)
				{
					int d1 = getRGB(data[inpos++]);
					int d2 = getRGB(data[inpos++]);
					for (int i=0; i<b2; i++)
					{
						b[outpos++] = d1;
						b[outpos++] = d2;
					}
				}
			}
			else
			{
				new IllegalArgumentException("Unrecognized control: "+Integer.toHexString(b1)+" "+Integer.toHexString(b2)).printStackTrace();
				System.exit(0);
			}
		}
		//debug.log(0, "compressed in="+inpos+" out="+outpos);

		} catch (RuntimeException ex) { ex.printStackTrace(); throw ex; }

		return b;
	}

	// get 32-bit color RGB image with alpha channel from source
	int[] loadFullColor(boolean comp)
	{
		byte id[] = data;
		if (comp)
		{
			try
			{
			GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(data));
			id = Util.readFully(gz);
			}
			catch (Exception ex) { ex.printStackTrace(); }
		}
		int d[] = new int[id.length/4];
//System.out.println("CCFIcon :: loadFullColor(w="+width+",h="+height+",dl="+id.length+",t="+type+",s="+size+")");
		for (int i=0; i<d.length; i++)
		{
			d[i] =
				((id[i*4+0] & 0xff) << 24) |
				((id[i*4+1] & 0xff) << 16) |
				((id[i*4+2] & 0xff) <<  8) |
				((id[i*4+3] & 0xff) <<  0) ;
		}
		return d;
	}

	// --( color compressor helper )-----------------------------------------

	private class Segment
	{
		private boolean rle;
		private int len;
		private int spos;
		private byte data[];

		Segment(byte data[], int pos, int max)
		{
			if (pos >= max)
			{
				return;
			}
			this.data = data;
			this.spos = pos;
			int b1 = ((data[pos++]&0xff) << 8) | (data[pos++]&0xff);
			int b2 = ((data[pos++]&0xff) << 8) | (data[pos++]&0xff);
			//debug.log(0,"b1="+Integer.toHexString(b1)+" b2="+Integer.toHexString(b2)+" @ "+(pos - 4));
			if (b1 == b2)
			{
				while (pos < max && b1 == b2 && (pos-spos < 256))
				{
					b2 = ((data[pos++]&0xff) << 8) | (data[pos++]&0xff);
				}
				rle = true;
				len = pos - spos - 2;
			}
			else
			{
				int b3 = 0xffffff;
				//while (pos < max && (b1 != b2 || b2 != b3) && (pos-spos < 256))
				while (pos < max && b1 != b2 && (pos-spos < 256))
				{
					b1 = b2;
					//b2 = b3;
					b2 = ((data[pos++]&0xff) << 8) | (data[pos++]&0xff);
				}
				rle = false;
				len = pos - spos - 4;
			}
		}

		void write(OutputStream os)
			throws IOException
		{
			if (rle)
			{
				os.write(data[spos]);
				os.write(data[spos+1]);
			}
			else
			{
				for (int i=spos; i<spos+len; i++)
				{
					os.write(data[i]);
				}
			}
		}
	}

}


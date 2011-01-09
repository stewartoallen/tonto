/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

class CRC16
{
    private int crc;

    public CRC16()
	{
        crc = 0;
    }

    public CRC16(byte b[])
	{
		this(b, 0, b.length);
	}

    public CRC16(byte b[], int off, int len)
	{
		crc = 0;
		update(b, off, len);
	}

	public void update(byte b[])
	{
		update(b, 0, b.length);
	}

	public void update(byte b[], int off, int len)
	{
		for (int i=0; i<len; i++)
		{
			update(b[i+off]);
		}
	}

    public void update(byte aByte)
	{
		crc = crc ^ aByte << 8;

        for (int i=0; i<8; i++)
		{
            if ((crc & 0x8000) != 0)
			{
                crc = crc << 1 ^ 0x1021;
            }
			else
			{
                crc = (crc << 1);
            }
        }
        crc = crc & 0xffff;
    }

    public void reset()
	{
        crc = 0;
    }

	public int getValue()
	{
		return crc & 0xffff;
	}
}


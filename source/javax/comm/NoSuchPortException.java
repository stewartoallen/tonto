/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package javax.comm;

public class NoSuchPortException extends Exception
{
    NoSuchPortException()
	{
		super();
	}

    NoSuchPortException(String msg)
    {
        super(msg);
    }
}


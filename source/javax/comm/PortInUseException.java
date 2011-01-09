/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package javax.comm;

public class PortInUseException extends Exception
{
    public String currentOwner;

    PortInUseException(String owner)
    {
        super("Port alredy owned by "+owner);
        this.currentOwner = owner;
    }
}

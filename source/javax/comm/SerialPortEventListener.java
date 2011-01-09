/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package javax.comm;

import java.util.EventListener;

public interface SerialPortEventListener
    extends EventListener
{
    public abstract void serialEvent(SerialPortEvent event);
}

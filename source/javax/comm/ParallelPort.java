/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package javax.comm;

import java.util.TooManyListenersException;

public abstract class ParallelPort extends CommPort
{
    public static final int LPT_MODE_ANY = 0;
    public static final int LPT_MODE_ECP = 4;
    public static final int LPT_MODE_EPP = 3;
    public static final int LPT_MODE_NIBBLE = 5;
    public static final int LPT_MODE_PS2 = 2;
    public static final int LPT_MODE_SPP = 1;

    public abstract void addEventListener(
		ParallelPortEventListener listener)
        throws TooManyListenersException;

    public abstract int getMode();

    public abstract int getOutputBufferFree();

    public abstract boolean isPaperOut();

    public abstract boolean isPrinterBusy();

    public abstract boolean isPrinterError();

    public abstract boolean isPrinterSelected();

    public abstract boolean isPrinterTimedOut();

    public abstract void notifyOnBuffer(boolean notify);

    public abstract void notifyOnError(boolean notify);

    public abstract void removeEventListener();

    public abstract void restart();

    public abstract int setMode(int mode)
        throws UnsupportedCommOperationException;

    public abstract void suspend();
}


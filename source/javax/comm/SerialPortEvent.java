/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package javax.comm;

import java.util.EventObject;

public class SerialPortEvent extends EventObject
{
	// ---( public instance fields )---
    /**
     * @deprecated Field eventType is deprecated
     */
    public int eventType;
    public static final int BI = 10;
    public static final int CD = 6;
    public static final int CTS = 3;
    public static final int DATA_AVAILABLE = 1;
    public static final int DSR = 4;
    public static final int FE = 9;
    public static final int OE = 7;
    public static final int OUTPUT_BUFFER_EMPTY = 2;
    public static final int PE = 8;
    public static final int RI = 5;

	// ---( private instance fields )---
    private boolean oldVal;
    private boolean newVal;

	// ---( public constructor )---
    public SerialPortEvent(
		SerialPort srcport, int evtType, boolean oldVal, boolean newVal)
    {
        super(srcport);
        this.eventType = evtType;
        this.oldVal = oldVal;
        this.newVal = newVal;
    }

	// ---( public methods )---
    public int getEventType()
    {
        return eventType;
    }

    public boolean getNewValue()
    {
        return newVal;
    }

    public boolean getOldValue()
    {
        return oldVal;
    }
}


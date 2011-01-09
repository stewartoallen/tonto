/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package javax.comm;

import java.util.EventObject;

public class ParallelPortEvent extends EventObject
{
    /**
     * @deprecated Field eventType is deprecated
     */
    public int eventType;
    public static final int PAR_EV_BUFFER = 2;
    public static final int PAR_EV_ERROR = 1;

	private boolean oldVal;
	private boolean newVal;

    public ParallelPortEvent(
		ParallelPort srcport, int evtType, boolean oldVal, boolean newVal)
    {
        super(srcport);
		this.oldVal = oldVal;
		this.newVal = newVal;
    }

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


/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package javax.comm;

import java.util.EventListener;

public interface CommPortOwnershipListener
    extends EventListener
{
    public static final int PORT_OWNED = 1;
    public static final int PORT_OWNERSHIP_REQUESTED = 3;
    public static final int PORT_UNOWNED = 2;

    public abstract void ownershipChange(int type);
}

/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package javax.comm;

import java.util.TooManyListenersException;

public abstract class SerialPort extends CommPort
{
    public static final int DATABITS_5 = 5;
    public static final int DATABITS_6 = 6;
    public static final int DATABITS_7 = 7;
    public static final int DATABITS_8 = 8;
    public static final int FLOWCONTROL_NONE = 0;
    public static final int FLOWCONTROL_RTSCTS_IN = 1;
    public static final int FLOWCONTROL_RTSCTS_OUT = 2;
    public static final int FLOWCONTROL_XONXOFF_IN = 4;
    public static final int FLOWCONTROL_XONXOFF_OUT = 8;
    public static final int PARITY_NONE = 0;
    public static final int PARITY_ODD = 1;
    public static final int PARITY_EVEN = 2;
    public static final int PARITY_MARK = 3;
    public static final int PARITY_SPACE = 4;
    public static final int STOPBITS_1 = 1;
    public static final int STOPBITS_2 = 2;
    public static final int STOPBITS_1_5 = 3;

    public abstract void addEventListener(
		SerialPortEventListener serialporteventlistener)
        throws TooManyListenersException;

    public abstract int getBaudRate();

    public abstract int getDataBits();

    public abstract int getFlowControlMode();

    public abstract int getParity();

    public abstract int getStopBits();

    public abstract boolean isCD();

    public abstract boolean isCTS();

    public abstract boolean isDSR();

    public abstract boolean isDTR();

    public abstract boolean isRI();

    public abstract boolean isRTS();

    public abstract void notifyOnBreakInterrupt(boolean flag);

    public abstract void notifyOnCarrierDetect(boolean flag);

    public abstract void notifyOnCTS(boolean flag);

    public abstract void notifyOnDataAvailable(boolean flag);

    public abstract void notifyOnDSR(boolean flag);

    public abstract void notifyOnFramingError(boolean flag);

    public abstract void notifyOnOutputEmpty(boolean flag);

    public abstract void notifyOnOverrunError(boolean flag);

    public abstract void notifyOnParityError(boolean flag);

    public abstract void notifyOnRingIndicator(boolean flag);

    public abstract void removeEventListener();

    public abstract void sendBreak(int i);

    public abstract void setDTR(boolean flag);

    public abstract void setFlowControlMode(int i)
        throws UnsupportedCommOperationException;

    /**
     * @deprecated was advisory only.
     */
    public void setRcvFifoTrigger(int i)
    {
		// TODO or ignore
    }

    public abstract void setRTS(boolean flag);

    public abstract void setSerialPortParams(int i, int j, int k, int l)
        throws UnsupportedCommOperationException;
}


/*
 * Copyright 2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.IOException;

interface ICommSource
{
	public ICommSerialPortID[] getSerialPorts();

	public ICommSerialPortID getSerialPort(String name);
}


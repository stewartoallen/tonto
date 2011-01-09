/*
 * Copyright 2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.IOException;

public interface IComm
{
	public void sendAttention() throws IOException ;

	public void send(int ch) throws IOException ;

	public void send(byte b[]) throws IOException ;

	public void send(byte b[], int off, int len) throws IOException ;

	public int  recv() throws IOException ;

	public int  recv(byte b[]) throws IOException ;

	public int  recv(byte b[], int off, int len) throws IOException ;

	public void flush() throws IOException ;
}


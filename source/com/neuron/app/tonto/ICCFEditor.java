/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import com.neuron.app.tonto.ui.*;

public interface ICCFEditor extends ICCFProvider
{
	public void setClipboard(CCFNode node[])
		;

	public CCFNode[] getClipboard()
		;

	public void pushEdit(IEditAction edit)
		;

	public void setSelection(Object src, Enumeration e)
		;
}

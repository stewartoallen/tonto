/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto.ui;

import java.util.*;
import java.awt.*;
import javax.swing.*;

public class VectorComboModel extends AbstractListModel implements ComboBoxModel
{
	private Vector list;
	private Object sel;

	public VectorComboModel(Vector v)
	{
		list = v;
	}

	public Object getElementAt(int idx)
	{
		return list.elementAt(idx);
	}

	public int getSize()
	{
		return list.size();
	}

	public Object getSelectedItem()
	{
		return sel;
	}

	public void setSelectedItem(Object o)
	{
		sel = o;
	}
}


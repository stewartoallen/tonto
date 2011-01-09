/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.awt.MenuItem;

public abstract class TontoModule
{
	// ---( static fields )---

	// ---( static methods )---

	// ---( constructors )---
	public TontoModule ()
	{
	}

	// ---( instance fields )---
	private Tonto tonto;

	// ---( package methods )---
	void register(Tonto tonto)
	{
		this.tonto = tonto;
	}

	// ---( abstract methods )---
	public abstract String getModuleName()
		;

	public abstract float getModuleVersion()
		;

	public abstract String getModuleAuthor()
		;

	public abstract MenuItem getMenuItem()
		;

	// ---( instance methods )---
	public CCF getCCF()
	{
		return tonto.ccf();
	}

	public void showElementInCCFTree(Object obj)
	{
		// TODO
	}

	public void refreshCCFTree()
	{
		// TODO
	}

	public void showCCFPanel(CCFPanel panel)
	{
		// TODO
	}

	// ---( interface methods )---
}


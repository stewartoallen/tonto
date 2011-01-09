/*
 * Copyright 2002 Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto.ui;

// ---( imports )---
import java.awt.*;
import java.util.*;
import javax.swing.*;
import com.neuron.app.tonto.*;

public class TaskList extends OKCancelDialog implements ITaskStatus
{
	// ---( static fields )---

	// ---( static methods )---
	public static void main(String args[])
	{
		TaskList t = new TaskList("test list");
		t.addTasks(new Task[] {
			new Task("foo",2) {
				public void invoke(ITaskStatus status) {
					status.taskStatus(25,"abcdefg");
					sleep(1000);
					status.taskStatus(50,"hijklmn");
					sleep(1000);
					status.taskStatus(75,"opqrstu");
					sleep(1000);
				}
			},
			new Task("bar",1) {
				public void invoke(ITaskStatus status) {
					status.taskStatus(25,"barrific");
					sleep(1000);
					status.taskStatus(50,"barrific");
					sleep(2000);
					status.taskStatus(75,"squeemish");
					sleep(1000);
				}
			},
			new Task("xxx",1) {
				public void invoke(ITaskStatus status) {
					status.taskStatus(30,"too cool");
					sleep(1000);
					status.taskStatus(40,"sweet");
					sleep(1000);
					status.taskStatus(75,"almost done");
					sleep(2000);
				}
			},
		});
		t.invoke();
	}

	private static void sleep(long time)
	{
		try { Thread.currentThread().sleep(time); } catch (Exception ex) { }
	}

	// ---( constructors )---
	public TaskList (String title)
	{
		super(title);
		tasks = new Vector();
		current = new JProgressBar(0,100);
		current.setStringPainted(true);
		current.setValue(0);
		total = new JProgressBar(0,100);
		total.setStringPainted(true);
		total.setValue(0);
		task = new JLabel("--------------------");
		which = new JLabel("( x of y )");
		message = new JLabel("--------------------");
		task.setForeground(Color.black);
		message.setForeground(Color.black);
		AAPanel ap = new AAPanel();
		ap.define('L', new JLabel("Task"),    "pad=3,3,3,3");
		ap.define('c', current,               "pad=3,3,3,3;fill=b;wx=1");
		ap.define('t', total,                 "pad=3,3,3,3;fill=b;wx=1");
		ap.define('l', task,                  "pad=3,3,3,3;wx=1");
		ap.define('m', message,               "pad=3,3,3,3;wx=1");
		ap.define('w', which,                 "pad=3,3,3,3");
		Util.setLabelBorder("Current", current);
		Util.setLabelBorder("Total", total);
		ap.setLayout(new String[] {
			"L lw",
			"mmmm",
			"cccc",
			"tttt"
		});
		setContentPane(ap);
	}

	// ---( instance fields )---
	private Vector tasks;
	private JProgressBar current;
	private JProgressBar total;
	private JLabel task;
	private JLabel which;
	private JLabel message;
	private double pctPerTask;
	private int taskSum;
	private int taskCount;
	private int taskWeight;
	private int taskTotals;
	private Throwable error;

	// ---( instance methods )---
	public void addTask(Task task)
	{
		tasks.add(task);
	}

	public void addTasks(Task task[])
	{
		for (int i=0; i<task.length; i++)
		{
			addTask(task[i]);
		}
	}

	private void updateTotal(int pct)
	{
		int ttv = (int)((double)taskSum * pctPerTask) + (int)(((double)taskWeight*pctPerTask)*((double)pct/100.0));
		total.setValue(ttv);
		total.setString(ttv+"%");
	}

	public void taskStatus(int pct, String desc)
	{
		if (desc != null)
		{
			message.setText(desc);
		}
		int cv = current.getValue();
		if (pct - cv > 10)
		{
			for (int i=cv; i<pct; i += 10)
			{
				current.setValue(i);
				updateTotal(i);
				sleep(10);
			}
		}
		current.setValue(pct);
		current.setString(pct+"%");
		updateTotal(pct);
	}

	public void taskError(Throwable t)
	{
		if (error == null || t == null)
		{
			error = t;
		}
	}

	public void taskNotify(Object obj)
	{
		if (obj != null)
		{
			message.setText(obj.toString());
		}
	}

	public boolean invoke()
	{
		new Thread() {
			public void run() {
				taskCount = tasks.size();
				taskTotals = 0;
				for (int i=0; i<taskCount; i++) {
					taskTotals += ((Task)tasks.get(i)).getWeight();
				}
				pctPerTask = 100.0/(double)taskTotals;
				for (int i=0; i<taskCount; i++) {
					Task t = (Task)tasks.get(i);
					task.setText(t.getName());
					taskWeight = t.getWeight();
					which.setText("("+(i+1)+" of "+taskCount+")");
					t.invoke(TaskList.this);
					if (error != null)
					{
						escCancel();
						Util.errorDialog(error.getMessage(), null);
						break;
					}
					taskStatus(100, "Complete");
					taskSum += taskWeight;
				}
				TaskList.sleep(100);
				enterOK();
			}
		}.start();
		return super.invoke();
	}

	public JComponent getButtons()
	{
		JPanel p = new JPanel();
		p.add(new ActionButton("Cancel") {
			public void action() {
				escCancel();
			}
		});
		return p;
	}

	public void doOK()
	{
	}

	public void doCancel()
	{
	}

	public Dimension getMinimumSize()
	{
		Dimension d = super.getMinimumSize();
		return new Dimension(Math.max(250,d.width), d.height);
	}

	public Dimension getPreferredSize()
	{
		Dimension d = super.getPreferredSize();
		return new Dimension(Math.max(250,d.width), d.height);
	}

	// ---( interface methods )---

}


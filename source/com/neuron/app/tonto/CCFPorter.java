package com.neuron.app.tonto;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.awt.Point;
import java.awt.Dimension;
import com.neuron.xml.*;
import com.neuron.io.ByteOutputBuffer;

// TODO: eliminate pathing and id's and use guid's instead
// TODO: paths may contain dup name conflicts, use oid's instead?
public class CCFPorter
{
	private Hashtable npos = new Hashtable();
	private Stack nposList = new Stack();

	private Hashtable icon = new Hashtable();
	private Stack iconList = new Stack();

	private Hashtable acts = new Hashtable();
	private Stack actList = new Stack();

	private Hashtable irs = new Hashtable();
	private Stack irList = new Stack();

	private boolean iscolor = false;
	private int cpos = 0;
	private CCF ccf;

	private ByteOutputBuffer emitTo = new ByteOutputBuffer();

	private final static String[] actName = {
		"ir", "button", "panel", "delay", "key", "device", "timer", "beep", "panel"
	};

	private CCFPorter()
	{
	}

	private CCFPorter(CCF ccf)
	{
		this.ccf = ccf;
		this.iscolor = ccf.header().hasColor();
	}

	private void emit(int depth, String msg)
	{
		try
		{
			for (int i=0; i<depth; i++)
			{
				emitTo.write("  ".getBytes());
				//System.out.print("  ");
			}
			emitTo.write(msg.getBytes());
			emitTo.write((byte)'\n');
			//System.out.println(msg);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex.getMessage());
		}
	}

	public static void exportZip(CCF ccf, String file)
		throws IOException
	{
		exportZip(ccf, file, new TaskStatus());
	}

	public static void exportZip(CCF ccf, String file, ITaskStatus status)
		throws IOException
	{
		new CCFPorter(ccf).Export(file, status);
	}

	private void Export(String file, ITaskStatus status)
		throws IOException
	{
		FileOutputStream fo = new FileOutputStream(file);
		ZipOutputStream zo = new ZipOutputStream(fo);

		status.taskStatus(0, "Generating XML");
		emit(0, "<?xml version=\"1.0\" ?>");
		emit(0, "");
		emit(0, "<!-- Generated By: Tonto "+Tonto.version()+" -->");
		emit(0, "");
		emit(0, "<!-- colors='fg/bg' -->");
		emit(0, "<!-- button colors='inactive unselected, inactive selected, active unselected, active selected' -->");
		emit(0, "<!-- button icons='inactive unselected, inactive selected, active unselected, active selected' -->");
		emit(0, "<!-- device icons='unselected, selected' -->");
		emit(0, "");
		emit(ccf.header());

		status.taskStatus(5, "Generating XML");
		ZipEntry ze = new ZipEntry("ccf.xml");
		ze.setSize(emitTo.size());
		zo.putNextEntry(ze);
		emitTo.writeToStream(zo);
		zo.closeEntry();

		status.taskStatus(50, "Generating Icons");
		int sz = iconList.size();
		for (int i=0; i<sz; i++)
		{
			CCFIcon icon = (CCFIcon)iconList.get(i);
			ze = new ZipEntry("icon-"+i+".gif");
			zo.putNextEntry(ze);
			icon.saveGIF(zo);
			zo.closeEntry();
			status.taskStatus(50+((i*50)/sz), "Generating Icons");
		}

		status.taskStatus(100, "Completing");
		zo.close();
	}

	// convert strings to displayable format
	private String safe(String name)
	{
		if (name == null)
		{
			return null;
		}
		StringBuffer sb = new StringBuffer(name.length());
		for (int i=0; i<name.length(); i++)
		{
			char ch = name.charAt(i);
			if (ch >= 32 && ch <= 126)
			{
				if (ch == '\\')
				{
					sb.append("\\\\");
				}
				else
				if (ch == '/')
				{
					sb.append("\\/");
				}
				else
				if (ch == '"')
				{
					sb.append("&quot;");
				}
				else
				{
					sb.append(ch);
				}
			}
			else
			{
				sb.append("\\"+Integer.toString((int)ch, 8));
			}
		}
		return sb.toString();
	}

	private String unsafe(String name)
	{
		StringBuffer sb = new StringBuffer(name.length());
		int max = name.length();
		int pos = 0;
		while (pos < max)
		{
			char ch = name.charAt(pos++);
			if (ch == '\\')
			{
				if (pos == max)
				{
					sb.append('\\');
					pos++;
				}
				else
				if (name.charAt(pos) == '/')
				{
					sb.append(name.charAt(pos));
					pos++;
				}
				else
				if (pos < max-2)
				{
					sb.append((char)Integer.parseInt(name.substring(pos,pos+3),8));
					pos += 3;
				}
			}
			else
			if (ch == '&' && pos < max-5 && name.substring(pos, pos+5).equals("quot;"))
			{
				sb.append('"');
			}
			else
			{
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	private String atstr(String key, int v)
	{
		return atstr(key, Integer.toString(v));
	}

	private String atstr(String key, String s)
	{
		if (s == null)
		{
			return key+"=\"\"";
		}
		return key+"=\""+s+"\"";
	}

	private String atcolor(String name, int color)
	{
		return atstr(name,
			CCFColor.getForegroundIndex(color, iscolor)+"/"+
			CCFColor.getBackgroundIndex(color, iscolor));
	}

	private String atcolors(String name, int color[])
	{
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<color.length; i++)
		{
			sb.append(
				CCFColor.getForegroundIndex(color[i], iscolor)+"/"+
				CCFColor.getBackgroundIndex(color[i], iscolor)+
				(i<color.length-1 ? "," : "")
			);
		}
		return atstr(name, sb.toString());
	}

	private String aticon(String name, CCFIcon icon)
	{
		if (icon == null)
		{
			return atstr(name, null);
		}
		return atstr(name, iconPtr(icon));
	}

	private String aticons(String name, CCFIcon icon[])
	{
		if (icon == null)
		{
			return atstr(name, null);
		}
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<icon.length; i++)
		{
			sb.append(iconPtr(icon[i])+(i<icon.length-1?",":""));
		}
		return atstr(name, sb.toString());
	}

	private String atdim(CCFNode node)
	{
		if (node instanceof CCFButton)
		{
			CCFButton b = (CCFButton)node;
			CCFChild c = (CCFChild)node.getParent();
			return atstr("pos", c.intX+"x"+c.intY)+" "+atstr("dim",b.width+"x"+b.height);
		}
		else
		if (node instanceof CCFFrame)
		{
//System.out.println(">>>>>>> node = "+node);
			CCFFrame b = (CCFFrame)node;
			CCFChild c = (CCFChild)node.getParent();
			return atstr("pos", c.intX+"x"+c.intY)+" "+atstr("dim",b.width+"x"+b.height);
		}
		return "";
	}

	private String actionPtr(CCFActionList act)
	{
		if (act == null)
		{
			return atstr("actions", null);
		}
		Integer ptr = (Integer)acts.get(act);
		if (ptr == null)
		{
			ptr = new Integer(acts.size());
			actList.push(act);
			acts.put(act, ptr);
		}
		return atstr("actions", ptr.intValue());
	}

	private String irPtr(CCFIRCode ir)
	{
		if (ir == null)
		{
			return atstr("ir", null);
		}
		Integer ptr = (Integer)irs.get(ir);
		if (irs.get(ir) == null)
		{
			ptr = new Integer(irs.size());
			irList.push(ir);
			irs.put(ir, ptr);
		}
		return atstr("ir", ptr.intValue());
	}

	private String iconPtr(CCFIcon ic)
	{
		if (ic == null)
		{
			return "";
		}
		Integer ptr = (Integer)icon.get(ic);
		if (icon.get(ic) == null)
		{
			ptr = new Integer(icon.size());
			iconList.push(ic);
			icon.put(ic, ptr);
		}
		return ptr.toString();
	}

	private void emit(CCFHeader header)
	{
		emit(0,"<ccf "+atstr("modified",
				header.year+"/"+header.month+"/"+header.day+","+
				header.hour+":"+header.minute+":"+header.seconds
			)+" "+atstr("model",
				ProntoModel.getModelByCapability(header.capability)[0].getName()
			)+" "+atstr("version", header.version)+
			">");
		emit(1,"<homes>");
		emit(2,header.firstHome);
		emit(1,"</homes>");
		emit(1,"<devices>");
		emit(2,header.firstDevice);
		emit(1,"</devices>");
		emit(1,"<macros>");
		emit(2,header.firstMacro);
		emit(1,"</macros>");
		emit(1,header.macroPanel);
		emitActions();
		emitIRCodes();
		emitIcons();
		emit(0,"</ccf>");
	}

	private void emit(int z, CCFDevice device)
	{
		if (device == null)
		{
			return;
		}
		incrPos(device);
		emit(z,"<device "+atstr("name",safe(device.name))+
			(device.getFlag(device.READ_ONLY) ? " readonly" : "")+
			(device.getFlag(device.HAS_SEPARATOR) ? " separator" : "")+
			(device.getFlag(device.IS_TEMPLATE) ? " template" : "")+
			" "+actionPtr(device.action)+
			" "+aticons("icons", new CCFIcon[] { device.iconUnselected, device.iconSelected })+
			">");
		pushPos();
		emit(z+1, device.firstPanel);
		popPos();
		emit(z,"</device>");
		emit(z, device.next);
	}

	private void emit(int z, CCFPanel panel)
	{
		if (panel == null)
		{
			return;
		}
		incrPos(panel);
		emit(z, "<panel "+atstr("name",safe(panel.name))+(panel.isHidden() ? " hidden" : "")+">");
		pushPos();
		emit(z+1, panel.child);
		popPos();
		emit(z, "</panel>");
		emit(z, panel.next);
	}

	private void emit(int z, CCFFrame frame)
	{
		if (frame == null)
		{
			return;
		}
		incrPos(frame);
		emit(z, "<frame "+
			atstr("font",CCFFont.getAWTSize(frame.fontSize))+" "+
			atstr("name",safe(frame.name))+" "+
			atcolor("colors", frame.colors)+" "+
			aticon("icon", frame.icon)+" "+
			atdim(frame)+">");
		pushPos();
		emit(z+1, frame.child);
		popPos();
		emit(z, "</frame>");
	}

	private void emit(int z, CCFButton button)
	{
		incrPos(button);
		emit(z, "<button "+
			atstr("font",CCFFont.getAWTSize(button.fontSize))+" "+
			atstr("name",safe(button.name))+" "+
			atstr("id",safe(button.idtag))+" "+
			actionPtr(button.actions)+" "+
			atcolors("colors", new int[] { button.colorIU, button.colorIS, button.colorAU, button.colorAS })+" "+
			aticons("icons", new CCFIcon[] { button.iconIU, button.iconIS, button.iconAU, button.iconAS })+" "+
			atdim(button)+" />");
	}

	private void emit(int z, CCFChild c[])
	{
		if (c == null || c.length == 0)
		{
			return;
		}
		for (int i=0; i<c.length; i++)
		{
			switch (c[i].type)
			{
				case CCFChild.FRAME:
					emit(z, c[i].getFrame());
					break;
				case CCFChild.BUTTON:
					emit(z, c[i].getButton());
					break;
			}
		}
	}

	private String getPath(CCFNode node)
	{
		String path = null;
		while (node != null && !(node instanceof CCFHeader))
		{
			try
			{
				String nm = (String)node.getClass().getDeclaredField("name").get(node);
				if (nm == null)
				{
					throw new Exception();
				}
				CCFNode p = node.getParent();
				if (p instanceof IChildContainer)
				{
					CCFChild c[] = ((IChildContainer)p).getChildren();
					for (int i=0; i<c.length; i++)
					{
						if (c[i] != node)
						{
							String cn = ((INamed)c[i]).getName();
							if (cn != null && cn.equals(nm))
							{
								throw new Exception();
							}
						}
					}
				}
				path = safe(nm)+(path != null ? "/"+path : "");
			}
			catch (Exception ex)
			{
				path = "["+npos.get(node)+"]"+(path != null ? "/"+path : "");
			}
			if (node instanceof CCFDevice)
			{
				path = getDevTreeName((CCFDevice)node)+"/"+path;
			}
			node = node.getParent();
			if (node instanceof CCFChild)
			{
				node = node.getParent();
			}
		}
		return path;
	}

	private String getDevTreeName(CCFDevice dev)
	{
		if (devListHas(ccf.header().firstHome, dev))
		{
			return "HOME";
		}
		else
		if (devListHas(ccf.header().firstDevice, dev))
		{
			return "DEVICE";
		}
		else
		if (devListHas(ccf.header().firstMacro, dev))
		{
			return "MACRO";
		}
		else
		{
			throw new RuntimeException("unable to find tree for '"+dev+"'");
		}
	}

	private boolean devListHas(CCFDevice root, CCFDevice dev)
	{
		while (root != null)
		{
			if (root == dev)
			{
				return true;
			}
			root = root.next;
		}
		return false;
	}

	private void incrPos(CCFNode node)
	{
		npos.put(node, new Integer(++cpos));
	}

	private void pushPos()
	{
		nposList.push(new Integer(cpos));
		cpos = 0;
	}

	private void popPos()
	{
		cpos = ((Integer)nposList.pop()).intValue();
	}

	private void emitIcons()
	{
		emit(1, "<icons>");
		for (int i=0; i<iconList.size(); i++)
		{
			CCFIcon icon = (CCFIcon)iconList.get(i);
			emit(2, "<icon "+
				atstr("id", i)+" "+
				atstr("bits", icon.isColor() ? "8" : icon.isGray() ? "2" : "1")+" "+
				atstr("file", "icon-"+i+".gif")+
				(icon.isCompressed() ? " compressed" : "")+
				" />");
		}
		emit(1, "</icons>");
	}

	private void emitIRCodes()
	{
		emit(1, "<codes>");
		for (int i=0; i<irList.size(); i++)
		{
			CCFIRCode code = (CCFIRCode)irList.get(i);
			emit(2, "<ir "+atstr("id", i)+" "+atstr("name", code.getName())+" "+atstr("code", code.getCode())+" />");
		}
		emit(1, "</codes>");
	}

	private void emitAction(int d, CCFAction ca)
	{
		if (ca == null)
		{
			emit(d, "<action />");
			return;
		}
		String add = "";
		switch (ca.type)
		{
			case CCFAction.ACT_IRCODE:
				add=irPtr((CCFIRCode)ca.action2);
				break;
			case CCFAction.ACT_ALIAS_BUTTON:
				add=atstr("press", getPath(ca.action2));
				break;
			case CCFAction.ACT_JUMP_PANEL:
			case CCFAction.ACT_MARANTZ_JUMP:
				if (ca.isSpecialJump())
				{
					add=atstr("special", ca.getJumpSpecialString(ca.p2));
				}
				else
				{
					add=atstr("jump", getPath(ca.action2));
				}
				break;
			case CCFAction.ACT_DELAY:
				add=atstr("time", ca.p2);
				break;
			case CCFAction.ACT_ALIAS_KEY:
				CCFDevice dev = (CCFDevice)ca.action1;
				add=atstr("device", getPath(dev))+" "+
					atstr("key", CCFAction.getKeyName(dev, ca.p2));
				break;
			case CCFAction.ACT_ALIAS_DEV:
				add=atstr("jump", getPath(ca.action1));
				break;
			case CCFAction.ACT_TIMER:
				CCFTimer timer = (CCFTimer)ca.action2;
				emit(d, "<action "+
					atstr("type","timer")+" "+
					atstr("startDays",Integer.toString(timer.startDays))+" "+
					atstr("startTime",Integer.toString(timer.startTime))+" "+
					atstr("endDays",Integer.toString(timer.endDays))+" "+
					atstr("endTime",Integer.toString(timer.endTime))+">");
				emitAction(d+1, timer.startAction);
				emitAction(d+1, timer.endAction);
				emit(d, "</action>");
				return;
			case CCFAction.ACT_BEEP:
				ActionBeep ab = (ActionBeep)ca;//new ActionBeep(ca);
				add=atstr("duration", ab.getDuration())+" "+
					atstr("freq", ab.getFrequency())+" "+
					atstr("duty", ab.getDutyCycle());
				break;
		}
		emit(d, "<action "+atstr("type", actName[ca.type-1])+" "+add+" />");
	}

	private void emitActions()
	{
		emit(1, "<actions>");
		for (int i=0; i<actList.size(); i++)
		{
			CCFActionList al = (CCFActionList)actList.get(i);
			emit(2, "<sequence "+atstr("id", i)+">");
			CCFAction a[] = al.getActions();
			for (int j=0; j<a.length; j++)
			{
				emitAction(3, a[j]);
			}
			emit(2, "</sequence>");
		}
		emit(1, "</actions>");
	}

	public static CCF importZip(String file)
		throws IOException
	{
		return importZip(file, new TaskStatus());
	}

	public static CCF importZip(String file, ITaskStatus status)
		throws IOException
	{
		return new CCFPorter().Import(file, status);
	}

	private CCF Import(String zip, ITaskStatus status)
		throws IOException
	{
		ZipInputStream zi = new ZipInputStream(new FileInputStream(zip));
		Hashtable h = new Hashtable();
		XMLNode root = null;
		boolean color = true;

		status.taskStatus(0, "Scanning Zip");
		while (true)
		{
			ZipEntry ze = zi.getNextEntry();
			if (ze == null)
			{
				break;
			}
			String nm = ze.getName();
			if (nm.lastIndexOf('/') > 0)
			{
				nm = nm.substring(nm.lastIndexOf('/')+1);
			}
			String ln = nm.toLowerCase();
			if (ln.equals("ccf.xml"))
			{
				status.taskStatus(5, "Loading XML Manifest");
				XMLParser p = new XMLParser();
				p.load(zi);
				root = p.getRootNode();
			}
			else
			{
				h.put(nm, Util.readFully(zi));
			}
		}

		status.taskStatus(10, "Loading Icons");
		// get icons and ir before parsing the rest of the doc
		XMLNode in = root.getNode("icons");
		int max = h.size();
		int count = 0;
		for (Enumeration e = in.getNodeEnumeration("icon"); e.hasMoreElements(); )
		{
			XMLNode n = (XMLNode)e.nextElement();
			String nm = n.getAttribute("file");
			if (nm.lastIndexOf('/') > 0)
			{
				nm = nm.substring(nm.lastIndexOf('/')+1);
			}
			byte b[] = (byte[])h.get(nm);
			if (b == null)
			{
				throw new IllegalArgumentException("unable to find icon file '"+nm+"'");
			}
			int bits = Integer.parseInt(n.getAttribute("bits"));
			CCFIcon ic = CCFIcon.create(b, bits == 8 ? CCFIcon.MODE_8BIT : CCFIcon.MODE_2BIT);
			ic.setCompressed(n.hasAttribute("compressed"));
			icon.put(n.getAttribute("id"), ic);
			count++;
			status.taskStatus((10+((count*50)/max)), "Loading Icons");
		}

		// need header for ir codes
		ccf = new CCF();
		CCFHeader hdr = ccf.header();

		String time = root.getAttribute("modified");
		String model = root.getAttribute("model");
		String ver = root.getAttribute("version");

		if (model != null)
		{
			ProntoModel pm = ProntoModel.getModelByName(model);
			hdr.capability = pm != null ? pm.getCapability() : 0x1;
		}

		iscolor = ccf.header().hasColor();
		ccf.setVersionString(ver);


		// load up ir codes
		status.taskStatus(60, "Loading IR Codes");
		in = root.getNode("codes");
		if (in != null)
		{
			for (Enumeration e = in.getNodeEnumeration("ir"); e != null && e.hasMoreElements(); )
			{
				XMLNode n = (XMLNode)e.nextElement();
				String code = n.getAttribute("code");
				irs.put(n.getAttribute("id"), new CCFIRCode(hdr, n.getAttribute("name"), n.getAttribute("code")));
			}
		}

		status.taskStatus(65, "Loading Action Lists");
		// create empty action lists
		in = root.getNode("actions");
		if (in != null)
		{
			for (Enumeration e = in.getNodeEnumeration("sequence"); e != null && e.hasMoreElements(); )
			{
				XMLNode n = (XMLNode)e.nextElement();
				CCFActionList list = new CCFActionList();
				acts.put(n.getAttribute("id"), list);
				acts.put(list, n);
				actList.add(list);
			}
		}

		status.taskStatus(70, "Building CCF");
		if (time != null)
		{
			StringTokenizer st = new StringTokenizer(time,"/,:");
			if (st.countTokens() >= 6)
			{
				hdr.year = nextInt(st);
				hdr.month = nextInt(st);
				hdr.day = nextInt(st);
				hdr.hour = nextInt(st);
				hdr.minute = nextInt(st);
				hdr.seconds = nextInt(st);
			}
			else
			{
				throw new IllegalArgumentException("malformed date '"+time+"'");
			}
		}

		status.taskStatus(75, "Building CCF");
		ccf.setFirstHomeDevice(getDevices(hdr,root.getNode("homes")));
		ccf.setFirstDevice(getDevices(hdr,root.getNode("devices")));
		ccf.setFirstMacroDevice(getDevices(hdr,root.getNode("macros")));
		hdr.macroPanel = getPanels(hdr,root);

		hdr.buildTree(null);

		status.taskStatus(85, "Completing Actions");
		// resolve action list entries
		for (int i=0; i<actList.size(); i++)
		{
			CCFActionList list = (CCFActionList)actList.get(i);
			XMLNode node = (XMLNode)acts.get(list);
			Vector acc = new Vector();
			for (Enumeration en = node.getNodeEnumeration("action"); en != null && en.hasMoreElements(); )
			{
				XMLNode act = (XMLNode)en.nextElement();
				parseAction(ccf, root, act, acc);
			}
			if (acc.size() > 0)
			{
				CCFAction a[] = new CCFAction[acc.size()];
				acc.copyInto(a);
				list.setActions(a);
			}
		}

		return ccf;
	}

	private void parseAction(CCF ccf, XMLNode root, XMLNode act, Vector acc)
	{
		String type = act.getAttribute("type");
//System.out.println("act="+act+" action="+type);
		if (type.equals("device"))
		{
			CCFDevice dev = (CCFDevice)resolvePath(root,act.getAttribute("jump"));
			if (dev != null)
			{
				acc.add(new ActionAliasDevice(dev));
			}
		}
		else
		if (type.equals("panel"))
		{
			CCFPanel panel = (CCFPanel)resolvePath(root,act.getAttribute("jump"));
			if (panel != null)
			{
				acc.add(new ActionJumpPanel(panel, ccf.header().isNewMarantz()));
			}
			else
			{
				String special = act.getAttribute("special");
				if (special != null)
				{
					acc.add(new CCFAction(CCFAction.ACT_JUMP_PANEL,0,CCFAction.getJumpSpecialIDFromString(special)).decodeReplace());
				}
			}
		}
		else
		if (type.equals("button"))
		{
			CCFButton button = (CCFButton)resolvePath(root,act.getAttribute("press"));
			if (button != null)
			{
				acc.add(new ActionAliasButton(button));
			}
		}
		else
		if (type.equals("key"))
		{
			CCFDevice dev = (CCFDevice)resolvePath(root,act.getAttribute("device"));
			if (dev == null)
			{
				return;
			}
			String key = act.getAttribute("key");
			String name[] = CCFAction.getKeyNames(dev);
			for (int j=0; j<name.length; j++)
			{
				if (name[j].equals(key))
				{
					acc.add(new ActionAliasKey(dev, j));
					return;
				}
			}
		}
		else
		if (type.equals("ir"))
		{
			acc.add(new ActionIRCode((CCFIRCode)irs.get(act.getAttribute("ir"))));
		}
		else
		if (type.equals("delay"))
		{
			acc.add(new ActionDelay(Integer.parseInt(act.getAttribute("time"))));
		}
		else
		if (type.equals("beep"))
		{
			acc.add(new ActionBeep(
				Integer.parseInt(act.getAttribute("duration")),
				Integer.parseInt(act.getAttribute("freq")),
				Integer.parseInt(act.getAttribute("duty"))
			));
		}
		else
		if (type.equals("timer"))
		{
			CCFTimer nt = new CCFTimer();
			nt.startDays = Integer.parseInt(act.getAttribute("startDays"));
			nt.startTime = Integer.parseInt(act.getAttribute("startTime"));
			nt.endDays = Integer.parseInt(act.getAttribute("endDays"));
			nt.endTime = Integer.parseInt(act.getAttribute("endTime"));
			XMLNode a[] = act.getAllNodes();
			Vector nac = new Vector();
			parseAction(ccf, root, a[0], nac);
			parseAction(ccf, root, a[1], nac);
			nt.startAction = (CCFAction)nac.get(0);
			nt.endAction = (CCFAction)nac.get(1);
			acc.add(new ActionTimer(nt));
		}
	}

	private CCFNode resolvePath(XMLNode root, String path)
	{
//System.out.println(" >> resolvePath :: "+root+" :: "+path);
		try
		{

		if (root == null || path == null || path.length() == 0)
		{
			return null;
		}
		Object node = ccf.header();
		StringBuffer sb = new StringBuffer();
		int max = path.length();
		int pos = 0;
		while (pos < max)
		{
			char ch = path.charAt(pos++);
//System.out.println("sb="+sb+" pos="+pos+" max="+max+" ch='"+(char)ch+"' node="+node.getClass());
			if (ch == '\\' && pos < max && path.charAt(pos) == '/')
			{
				sb.append("\\/");
				pos++;
				if (pos != max)
				{
					continue;
				}
			}
			if (ch == '/' || pos == max)
			{
				if (pos == max)
				{
					sb.append(ch);
				}
				String key = unsafe(sb.toString());
//System.out.println("node="+node+" key="+key);
				if (node instanceof CCFHeader)
				{
					if (key.equals("HOME"))
					{
						node = ((CCFHeader)node).firstHome;
					}
					else
					if (key.equals("DEVICE"))
					{
						node = ((CCFHeader)node).firstDevice;
					}
					else
					if (key.equals("MACRO"))
					{
						node = ((CCFHeader)node).firstMacro;
					}
					else
					if (key.equals("PANEL"))
					{
						node = ((CCFHeader)node).macroPanel;
					}
					else
					{
						throw new RuntimeException("no header key match for '"+key+"'");
					}
				}
				else
				if (node instanceof CCFDevice)
				{
					CCFDevice d = (CCFDevice)node;
					if (key.startsWith("["))
					{
//System.out.println("dev parse index key ("+key+")");
						int idx = Integer.parseInt(key.substring(1,key.length()-1))-1;
						for (int i=0; i<idx; i++)
						{
							d = d.next;
						}
					}
					else
					{
						while (!d.name.equals(key))
						{
							d = d.next;
						}
					}
					if (pos < max)
					{
						node = d.firstPanel;
					}
					else
					{
						node = d;
					}
				}
				else
				if (node instanceof CCFPanel)
				{
					CCFPanel d = (CCFPanel)node;
					if (key.startsWith("["))
					{
//System.out.println("panel parse index key ("+key+")");
						int idx = Integer.parseInt(key.substring(1,key.length()-1))-1;
						for (int i=0; i<idx; i++)
						{
							d = d.next;
						}
					}
					else
					{
						while (!d.name.equals(key))
						{
							d = d.next;
						}
					}
					if (pos < max)
					{
						node = d.getChildren();
					}
					else
					{
						node = d;
					}
				}
				else
				if (node instanceof CCFChild[])
				{
//System.out.println("node="+node+" type="+node.getClass());
					CCFChild c[] = (CCFChild[])node;
					CCFChild m = null;
					if (key.startsWith("["))
					{
						String ns = key.substring(1,key.length()-1);
//System.out.println("child[] parse index key ("+key+") ns=("+ns+")");
						m = c[Integer.parseInt(key.substring(1,key.length()-1))-1];
					}
					else
					{
						for (int i=0; i<c.length; i++)
						{
							CCFChild t = c[i];
//System.out.println("child["+i+"]="+t+" type="+t.type);
							String nm = (t.type == CCFChild.FRAME ? t.getFrame().name : t.getButton().name);
//System.out.println("child["+i+"]="+t+" type="+t.type+" nm="+nm);
							if (nm != null && nm.equals(key))
							{
								m = t;
//System.out.println("child["+i+"]=m="+t+" type="+t.type);
								break;
							}
						}
					}
					if (m != null)
					{
						if (pos < max && m instanceof IChildContainer)
						{
							node = ((IChildContainer)m.child).getChildren();
						}
						else
						{
							node = m.child;
						}
						if (node instanceof CCFFrame && pos < max)
						{
							node = ((CCFFrame)node).getChildren();
						}
//System.out.println("   m != null so node="+node);
					}
					else
					{
						//node = null;
					}
				}
				else
				{
					throw new IllegalArgumentException("unhandled node '"+node+"' ("+node.getClass()+") in '"+path+"'");
				}
				sb.setLength(0);
			}
			else
			{
				sb.append(ch);
			}
		}
//System.out.println("returning node="+node+" pos="+pos+" max="+max);
		return (CCFNode)node;

		} catch (Exception ex)
		{
			System.out.println("error on root="+root+" path="+path);
			ex.printStackTrace();
			return null;
		}
	}

	private CCFDevice getDevices(CCFHeader hdr, XMLNode node)
	{
		if (node == null)
		{
			return null;
		}
		CCFDevice dev = null;
		CCFDevice cdev = null;
		for (Enumeration e = node.getNodeEnumeration("device"); e != null && e.hasMoreElements(); )
		{
			XMLNode nn = (XMLNode)e.nextElement();
			CCFDevice nd = new CCFDevice(hdr);
			nd.name = unsafe(nn.getAttribute("name"));
			nd.setFlag(nd.READ_ONLY, nn.hasAttribute("readonly"));
			nd.setFlag(nd.HAS_SEPARATOR, nn.hasAttribute("separator"));
			nd.setFlag(nd.IS_TEMPLATE, nn.hasAttribute("template"));
			nd.firstPanel = getPanels(nd, nn);
			if (dev == null)
			{
				dev = nd;
				cdev = nd;
			}
			else
			{
				cdev.next = nd;
				cdev = nd;
			}
		}
		return dev;
	}

	private CCFPanel getPanels(CCFNode dev, XMLNode node)
	{
		if (node == null)
		{
			return null;
		}
		CCFPanel pan = null;
		CCFPanel cpan = null;
		for (Enumeration e = node.getNodeEnumeration("panel"); e != null && e.hasMoreElements(); )
		{
			XMLNode nn = (XMLNode)e.nextElement();
			String nm = unsafe(nn.getAttribute("name"));
			CCFPanel np = (dev instanceof CCFDevice) ?((CCFDevice)dev).createPanel(nm) : new CCFPanel(nm, (CCFHeader)dev);
			np.setChildren(getChildren(np, nn));
			np.setHidden(nn.hasAttribute("hidden"));
			if (pan == null)
			{
				pan = np;
				cpan = np;
			}
			else
			{
				cpan.next = np;
				cpan = np;
			}
		}
		return pan;
	}

	private CCFChild[] getChildren(CCFNode cn, XMLNode xn)
	{
		if (cn == null || xn == null || !(cn instanceof CCFFrame || cn instanceof CCFPanel))
		{
			return null;
		}
		Vector chld = new Vector();
		XMLNode e[] = xn.getAllNodes();
		for (int i=0; e != null && i < e.length; i++)
		{
			XMLNode nn = e[i];
			String nm = nn.getName();
			if (nm.equals("button"))
			{
				chld.add(getButton(cn, nn));
			}
			else
			if (nm.equals("frame"))
			{
				chld.add(getFrame(cn, nn));
			}
		}
		CCFChild c[] = new CCFChild[chld.size()];
		chld.copyInto(c);
		return c;
	}
	
	private CCFChild getFrame(CCFNode p, XMLNode node)
	{
		CCFFrame f = new CCFFrame(p, unsafe(node.getAttribute("name")));
		CCFChild c = new CCFChild(p, f);
		f.fontSize = Math.max(0,(Integer.parseInt(node.getAttribute("font"))-6)/2);
		f.setSize(parseDim(node.getAttribute("dim")));
		f.setLocation(parsePos(node.getAttribute("pos")));
		StringTokenizer st = new StringTokenizer(node.getAttribute("colors"),",/");
		if (st.countTokens() >= 2)
		{
			f.colors = CCFColor.getComposite(new CCFColor(nextInt(st)), new CCFColor(nextInt(st)), iscolor);
		}
		else
		{
			throw new IllegalArgumentException("malformed colors '"+node.getAttribute("colors")+"'");
		}
		f.icon = getIcon(node.getAttribute("icon"));
		f.setChildren(getChildren(f,node));
		return c;
	}
	
	private CCFChild getButton(CCFNode p, XMLNode node)
	{
		CCFButton b = new CCFButton(p, unsafe(node.getAttribute("name")));
		CCFChild c = new CCFChild(p, b);
		c.setButton(b);
		b.fontSize = Math.max(0,(Integer.parseInt(node.getAttribute("font"))-6)/2);
		b.idtag = unsafe(node.getAttribute("id"));
		b.setSize(parseDim(node.getAttribute("dim")));
		b.setLocation(parsePos(node.getAttribute("pos")));
		b.actions = getActionList(node.getAttribute("actions"));
		StringTokenizer st = new StringTokenizer(node.getAttribute("colors"),",/");
		if (st.countTokens() >= 8)
		{
			b.colorIU = CCFColor.getComposite(new CCFColor(nextInt(st)), new CCFColor(nextInt(st)), iscolor);
			b.colorIS = CCFColor.getComposite(new CCFColor(nextInt(st)), new CCFColor(nextInt(st)), iscolor);
			b.colorAU = CCFColor.getComposite(new CCFColor(nextInt(st)), new CCFColor(nextInt(st)), iscolor);
			b.colorAS = CCFColor.getComposite(new CCFColor(nextInt(st)), new CCFColor(nextInt(st)), iscolor);
		}
		else
		{
			throw new IllegalArgumentException("malformed colors '"+node.getAttribute("colors")+"'");
		}
		st = new StringTokenizer(node.getAttribute("icons"),",",true);
		CCFIcon ic[] = new CCFIcon[4];
		int icp = 0;
		while (st.hasMoreTokens() && icp < ic.length)
		{
			String nt = st.nextToken();
			if (!nt.equals(","))
			{
				ic[icp] = getIcon(nt);
			}
			else
			{
				icp++;
			}
		}
		b.iconIU = ic[0];
		b.iconIS = ic[1];
		b.iconAU = ic[2];
		b.iconAS = ic[3];
		return c;
	}

	private int nextInt(StringTokenizer st)
	{
		return Integer.parseInt(st.nextToken());
	}

	private Point parsePos(String sz)
	{
		StringTokenizer st = new StringTokenizer(sz, "x");
		if (st.countTokens() < 2)
		{
			throw new IllegalArgumentException("malformed position '"+sz+"'");
		}
		return new Point(nextInt(st),nextInt(st));
	}

	private Dimension parseDim(String d)
	{
		StringTokenizer st = new StringTokenizer(d, "x");
		if (st.countTokens() < 2)
		{
			throw new IllegalArgumentException("malformed dimension '"+d+"'");
		}
		return new Dimension(nextInt(st),nextInt(st));
	}

	private CCFActionList getActionList(String key)
	{
		if (key == null || key.length() == 0)
		{
			return null;
		}
		return (CCFActionList)acts.get(key);
	}

	private CCFIcon getIcon(String key)
	{
		if (key == null || key.length() == 0)
		{
			return null;
		}
		return (CCFIcon)icon.get(key);
	}
}


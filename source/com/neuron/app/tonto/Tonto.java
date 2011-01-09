/*
 * Copyright (c) 2001-2002, Stewart Allen <stewart@neuron.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Artistic License.
 */

package com.neuron.app.tonto;

// ---( imports )---
import java.io.File;
import java.io.FilenameFilter;
import java.io.Writer;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.net.Socket;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.URLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.geom.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.util.zip.*;
import java.lang.reflect.Field;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.filechooser.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import javax.swing.plaf.metal.*;
import com.neuron.irdb.*;
import com.neuron.irdb.impl.*;
import com.neuron.io.*;
import com.neuron.app.tonto.ui.*;

/**
 * A UI Tool for creating, editing, loading and saving CCF Files.
 * This tool also communicates with the Pronto for upload/download
 * of files and provides a unique crash-recovery helper that can
 * automatically upload a fresh CCF file to a nearly-dead Pronto.
 */
public class Tonto implements ITaskStatus, TreeSelectionListener
{
	private static String version = null;
	private static boolean hasComm = Comm.isDriverOK();
	private static Debug debug = Debug.getInstance("tonto");

	// force other debug classes to pre-load/register
	private static Class class_xmodem = Xmodem.class;
	private static Class class_comm   = Comm.class;
	private static Class class_node   = CCFNode.class;

	// menu commands
	private final static int BASE_FILE              = 0x0010;
	private final static int BASE_EDIT              = 0x0020;
	private final static int BASE_ADD               = 0x0040;
	private final static int BASE_MISC              = 0x0080;
	private final static int BASE_UTIL              = 0x0100;
	private final static int BASE_FILE_NEW          = 0x0200;
	private final static int BASE_CONVERT           = 0x0400;
	private final static int BASE_OBJECT            = 0x0800;
	private final static int BASE_KEY_ALIAS         = 0x1000;
	private final static int BASE_KEY_PASTE         = 0x2000;
	private final static int BASE_KEY_COPY_ACTIONS  = 0x4000;
	private final static int BASE_KEY_PASTE_ACTIONS = 0x8000;

	private final static int FILE_NEW           = BASE_FILE + 1;
	private final static int FILE_CLOSE         = BASE_FILE + 2;
	private final static int FILE_LOAD          = BASE_FILE + 3;
	private final static int FILE_SAVE          = BASE_FILE + 4;
	private final static int FILE_SAVEAS        = BASE_FILE + 5;
	private final static int FILE_CCF_GET       = BASE_FILE + 6;
	private final static int FILE_CCF_PUT       = BASE_FILE + 7;
	private final static int FILE_PREFERENCES   = BASE_FILE + 8;
	private final static int FILE_EXIT          = BASE_FILE + 9;
	private final static int FILE_WINDOW        = BASE_FILE + 10;
	private final static int FILE_EXPORT        = BASE_FILE + 11;
	private final static int FILE_CLOSE_PANEL   = BASE_FILE + 12;
	private final static int FILE_CLOSE_WINDOW  = BASE_FILE + 13;
	private final static int FILE_MERGE         = BASE_FILE + 14;
	private final static int FILE_REVERT        = BASE_FILE + 15;

	private final static int EDIT_UNDO          = BASE_EDIT + 1;
	private final static int EDIT_REDO          = BASE_EDIT + 2;
	private final static int EDIT_CUT           = BASE_EDIT + 3;
	private final static int EDIT_COPY          = BASE_EDIT + 4;
	private final static int EDIT_DELETE        = BASE_EDIT + 5;
	private final static int EDIT_PASTE         = BASE_EDIT + 6;
	private final static int EDIT_PROPERTY      = BASE_EDIT + 7;
	private final static int EDIT_SELECT_ALL    = BASE_EDIT + 8;
	private final static int EDIT_UNSELECT_ALL  = BASE_EDIT + 9;

	private final static int ADD_DEVICE         = BASE_ADD + 1;
	private final static int ADD_PANEL          = BASE_ADD + 2;
	private final static int ADD_FRAME          = BASE_ADD + 3;
	private final static int ADD_BUTTON         = BASE_ADD + 4;

	private final static int ABOUT              = BASE_MISC + 1;
	private final static int TOGGLE_GRID        = BASE_MISC + 3;
	private final static int TOGGLE_SNAP        = BASE_MISC + 4;
	private final static int IR_DATABASE        = BASE_MISC + 5;
	private final static int COPY_ICONS         = BASE_MISC + 6;
	private final static int COPY_ACTIONS       = BASE_MISC + 7;
	private final static int COPY_KEYS          = BASE_MISC + 8;
	private final static int COPY_ALIAS         = BASE_MISC + 9;
	private final static int PASTE_ICONS        = BASE_MISC + 10;
	private final static int PASTE_ACTIONS      = BASE_MISC + 11;
	private final static int PASTE_KEYS         = BASE_MISC + 12;
	private final static int PASTE_ALIAS        = BASE_MISC + 13;
	private final static int DOCS_MAIN          = BASE_MISC + 14;
	private final static int DOCS_FAQ           = BASE_MISC + 15;
	private final static int DOCS_CHANGES       = BASE_MISC + 16;
	private final static int AUTOLOAD_ADD       = BASE_MISC + 17;
	private final static int AUTOLOAD_REMOVE    = BASE_MISC + 18;
	private final static int DEBUG_WINDOW       = BASE_MISC + 19;
	private final static int ICON_LIBRARY       = BASE_MISC + 20;
	private final static int ICON_REPLACE       = BASE_MISC + 21;
	private final static int HELP_CCFS          = BASE_MISC + 22;
	private final static int HELP_DISCRETE      = BASE_MISC + 23;
	private final static int TREE_EXPAND_ALL    = BASE_MISC + 24;
	private final static int TREE_COLLAPSE_ALL  = BASE_MISC + 25;
	private final static int VIEW_HIDE_SELECT   = BASE_MISC + 26;
	private final static int TAB_MASTER         = BASE_MISC + 27;
	private final static int DOCS_TUTOR         = BASE_MISC + 28;
	private final static int PASTE_REPLACE_ICONS= BASE_MISC + 29;
	private final static int THEME_MASTER       = BASE_MISC + 30;

	private final static int OBJECT_RAISE       = BASE_OBJECT + 1;
	private final static int OBJECT_LOWER       = BASE_OBJECT + 2;
	private final static int OBJECT_TOP         = BASE_OBJECT + 3;
	private final static int OBJECT_BOTTOM      = BASE_OBJECT + 4;
	private final static int ALIGN_TOP          = BASE_OBJECT + 5;
	private final static int ALIGN_BOTTOM       = BASE_OBJECT + 6;
	private final static int ALIGN_LEFT         = BASE_OBJECT + 7;
	private final static int ALIGN_RIGHT        = BASE_OBJECT + 8;
	private final static int ALIGN_HCENTER      = BASE_OBJECT + 9;
	private final static int ALIGN_VCENTER      = BASE_OBJECT + 10;
	private final static int OBJECTS_GROUP      = BASE_OBJECT + 11;
	private final static int OBJECTS_UNGROUP    = BASE_OBJECT + 12;
	private final static int SNAP_TOP_LEFT      = BASE_OBJECT + 13;
	private final static int SNAP_TOP           = BASE_OBJECT + 14;
	private final static int SNAP_TOP_RIGHT     = BASE_OBJECT + 15;
	private final static int SNAP_RIGHT         = BASE_OBJECT + 16;
	private final static int SNAP_BOTTOM_RIGHT  = BASE_OBJECT + 17;
	private final static int SNAP_BOTTOM        = BASE_OBJECT + 18;
	private final static int SNAP_BOTTOM_LEFT   = BASE_OBJECT + 19;
	private final static int SNAP_LEFT          = BASE_OBJECT + 20;
	private final static int OBJECT_TRANSPARENT = BASE_OBJECT + 21;

	private final static int UTIL_UPLOAD        = BASE_UTIL + 1;
	private final static int UTIL_DOWNLOAD      = BASE_UTIL + 2;
	private final static int EMAIL_LOGFILE      = BASE_UTIL + 3;
	private final static int UTIL_REBOOT        = BASE_UTIL + 4;
	private final static int UTIL_UNDEAD        = BASE_UTIL + 5;
	private final static int UTIL_LOCATE        = BASE_UTIL + 6;
	private final static int UTIL_NET_UPDATE    = BASE_UTIL + 7;
	private final static int UTIL_EMULATOR      = BASE_UTIL + 8;
	private final static int UTIL_EDITOR        = BASE_UTIL + 9;
	private final static int UTIL_FIRMWARE      = BASE_UTIL + 10;

	// move/resize cursors
	private final static Cursor CURSOR_DEFAULT  = new Cursor(Cursor.DEFAULT_CURSOR);
	private final static Cursor CURSOR_MOVE     = new Cursor(Cursor.MOVE_CURSOR);
	private final static Cursor CURSOR_WEST     = new Cursor(Cursor.W_RESIZE_CURSOR);
	private final static Cursor CURSOR_EAST     = new Cursor(Cursor.E_RESIZE_CURSOR);
	private final static Cursor CURSOR_NORTH    = new Cursor(Cursor.N_RESIZE_CURSOR);
	private final static Cursor CURSOR_SOUTH    = new Cursor(Cursor.S_RESIZE_CURSOR);
	private final static Cursor CURSOR_NW       = new Cursor(Cursor.NW_RESIZE_CURSOR);
	private final static Cursor CURSOR_NE       = new Cursor(Cursor.NE_RESIZE_CURSOR);
	private final static Cursor CURSOR_SW       = new Cursor(Cursor.SW_RESIZE_CURSOR);
	private final static Cursor CURSOR_SE       = new Cursor(Cursor.SE_RESIZE_CURSOR);

	// move/resize flags
	private final static int NORTH              = 1 << 0;
	private final static int SOUTH              = 1 << 1;
	private final static int EAST               = 1 << 2;
	private final static int WEST               = 1 << 3;
	private final static int NW                 = NORTH | WEST;
	private final static int NE                 = NORTH | EAST;
	private final static int SW                 = SOUTH | WEST;
	private final static int SE                 = SOUTH | EAST;

	// preference keys
	private final static int    MAX_UNDO               = 100;
	private final static String PREF_DEFAULT_MODEL     = "model";
	private final static String PREF_GRID_SIZE         = "grid";
	private final static String PREF_GRID_SNAP         = "grid.snap";
	private final static String PREF_GRID_SHOW         = "grid.show";
	private final static String PREF_GRID_MINOR_TICKS  = "grid.minor.ticks";
	private final static String PREF_GRID_MAJOR_COLOR  = "grid.minor.color";
	private final static String PREF_GRID_MINOR_COLOR  = "grid.major.color";
	private final static String PREF_DEBUG_LEVELS      = "debug";
	private final static String PREF_DEFAULT_PORT      = "port";
	private final static String PREF_SCAN_OTHER_PORTS  = "port.scan";
	private final static String PREF_SCAN_THREADED     = "port.scan.threaded";
	private final static String PREF_SCAN_MODEMS       = "port.scan.modems";
	private final static String PREF_COMM_TIMEOUTS     = "comm";
	private final static String PREF_RECENT_FILES      = "files";
	private final static String PREF_FONT_SCALING      = "fontscale";
	private final static String PREF_FONT_SIZE         = "tree.font.size";
	private final static String PREF_FONT_INTREE       = "tree.font.pronto";
	private final static String PREF_LOG_TO_CONSOLE    = "log.console";
	private final static String PREF_USE_AWT_DIALOG    = "oldFileDialogs";
	private final static String PREF_OBEY_REMOTE_CAP   = "obeyRemoteCapability";
	private final static String PREF_TEST_EMIT_IR      = "testEmitIR";
	private final static String PREF_SHOW_DEV_PROPS    = "show.device.properties";
	private final static String PREF_COLOR_MAP         = "color.map";
	private final static String PREF_COLOR_WEBSAFE     = "color.websafe";
	private final static String PREF_WORKING_DIR       = "workingDir";
	private final static String PREF_IMAGE_DIR         = "dir.images";
	private final static String PREF_IMAGE_EDITOR      = "image.editor";
	private final static String PREF_AUTOLOAD_FILES    = "autoload";
	private final static String PREF_SPLIT_HORIZONTAL  = "split.horizontal";
	private final static String PREF_NETWORK_UPDATES   = "network.update";
	private final static String PREF_UPDATE_ROOT       = "network.url";
	private final static String PREF_UUID              = "uuid";
	private final static String PREF_PREF_BOUNDS       = "geom.prefs";
	private final static String PREF_WINDOW_BOUNDS     = "geom.tonto";
	private final static String PREF_ICONLIB_BOUNDS    = "geom.iconlib";
	private final static String PREF_FRAMEPROP_BOUNDS  = "geom.frameprop";
	private final static String PREF_BUTTONPROP_BOUNDS = "geom.buttonprop";
	private final static String PREF_DEVICEPROP_BOUNDS = "geom.deviceprop";
	private final static String PREF_ALIAS_BOUNDS      = "geom.alias";
	private final static String PREF_JUMP_BOUNDS       = "geom.jump";
	private final static String PREF_IR_BOUNDS         = "geom.ir";
	private final static String PREF_DEBUG_BOUNDS      = "geom.debug";
	private final static String PREF_EMULATOR          = "emulator.path";
	private final static String PREF_EDITOR            = "editor.path";
	private final static String PREF_GRAY_TINT         = "colors.gray.tint";
	private final static String PREF_CENTER_DIALOGS    = "dialogs.center";
	private final static String ASK_NETWORK_UPDATE     = "ask.network.update";
	private final static String PREF_SELECTION_LAST    = "select.paint.last";

	private final static int numkey[] = new int[] {
		KeyEvent.VK_1,
		KeyEvent.VK_2,
		KeyEvent.VK_3,
		KeyEvent.VK_4,
		KeyEvent.VK_5,
		KeyEvent.VK_6,
		KeyEvent.VK_7,
		KeyEvent.VK_8,
		KeyEvent.VK_9,
		KeyEvent.VK_0,
	};

	private final static int[][] fontIBM = {
		{ 2, 100, 100 },
		{ 3, 100, 100 },
		{ 4, 100, 100 },
		{ 4, 100, 100 },
		{ 5, 100, 100 },
		{ 6, 100, 100 },
	};

	private final static int[][] fontSUN = {
		{ 1, 107, 100 },
		{ 2,  98, 100 },
		{ 3, 100, 100 },
		{ 2, 103, 105 },
		{ 2, 100, 100 },
		{ 3, 102, 104 },
	};

	private final static int[][] fontMAC = {
		{ 3, 100, 100 },
		{ 3, 100, 100 },
		{ 4, 100, 100 },
		{ 3, 100, 100 },
		{ 3, 100, 100 },
		{ 4, 100, 100 },
	};

	private final static int[][] fontUnknown = {
		{ 0, 100, 100 },
		{ 0, 100, 100 },
		{ 0, 100, 100 },
		{ 0, 100, 100 },
		{ 0, 100, 100 },
		{ 0, 100, 100 },
	};

	// shared state
	//private static CCF defaults;
	private static Tonto current;
	private static JTabbedPane windows;
	private static JFrame jframe;
	private static JToolBar tbar;
	private static Hashtable tools;
	private static Hashtable menus;
	private static Hashtable popmenus;
	private static MenuItem fileNew;
	private static Menu fileNewMenu;
	private static Menu convertMenu;
	private static Menu revertMenu;
	private static Menu recentMenu;
	private static Menu snapMenu;
	private static Menu orderMenu;
	private static Menu alignMenu;
	private static JPopupMenu rcFrame;
	private static JPopupMenu rcPanel;
	private static JPopupMenu rcButton;
	private static JPopupMenu rcDGroup;
	private static JPopupMenu rcDevice;
	private static JMenu rcDeviceAliasKey;
	private static JMenu rcDevicePasteKey;
	private static JMenu rcDeviceCopyActions;
	private static JMenu rcDevicePasteActions;
	private static Comm comm;
	private static CCFButton defaultGrayButton;
	private static CCFButton defaultColorButton;
	private static IRDBDialog irdb;
	private static IRDatabase database;
	private static Vector recentFiles;
	private static Preferences prefs;
	private static PrefsDialog prefsDialog;
	private static TimerEditor timerEditor;
	private static NameDialog dialogName;
	private static NameDialog dialogName2;
	private static Object clipboard;
	private static ImageIcon iconFolderOpened;
	private static ImageIcon iconFolderClosed;
	private static ImageIcon iconPanel;
	private static ImageIcon iconProperties;
	private static Image imageTontoSmall;
	private static Font fontPronto;
	private static int fontTable[][];
	private static ImageFont fonts[];
	private static File jarDir;
	private static int exitCode;
	private static int nextAction;
	private static Hashtable tontos;
	private static LineBorder border;
	private static Image splash;
	private static Canvas csplash;
	private static File logFile;
	private static File homeDir;
	private static File prefDir;
	private static File altPrefDir;
	private static File prefFile;
	private static File dbDir;
	private static File logDir;
	private static File ccfDir;
	private static File moduleDir;
	private static File defaultDB;
	private static String commMsg;
	private static Throwable commErr;
	private static Vector autoLoad;
	private static DebugListener console;
	private static SnapInfo snaps;
	private static Thread updateThread;

	// preferences, saved state
	private static boolean initShared = false;
	private static boolean isUpdating = false;
	private static boolean sessionUpdate;
	private static boolean prefNetworkUpdate;
	private static boolean prefUseAWTFileDialogs;
	private static boolean prefLogToConsole;
	private static boolean prefObeyRemoteCap;
	private static boolean prefUseProntoFont;
	private static boolean prefEmitIR;
	private static boolean prefShowWebSafe;
	private static boolean prefShowDeviceProps;
	private static boolean prefScanOtherPorts;
	private static boolean prefScanThreaded;
	private static boolean prefScanModems;
	private static boolean prefCenterDialogs;
	private static boolean prefSelectionLast;
	private static String prefUpdateRoot;
	private static int prefTreeFontSize;
	private static int prefHSplitPos;
	private static File prefWorkingDir;
	private static File prefImageDir;
	private static String prefEditor;
	//private static String prefEmulator;
	private static String prefImageEditor;
	private static String prefCommPort;
	private static String prefUUID;
	private static Rectangle prefGrid;
	private static boolean prefGridSnap;
	private static boolean prefGridShow;
	private static int prefGridMinorTicks;
	private static CCFColor prefGridMinorColor;
	private static CCFColor prefGridMajorColor;
	private static ProntoModel prefDefaultModel;
	private static CCFColor prefColorMap[];

	// ccf state vars
	private CCF ccf;
	private WeakHashMap wrappers = new WeakHashMap();
	private Hashtable panels;
	private String fileName;
	private Point nextPosition;
	private Stack undoStack;
	private Stack redoStack;
	private Dimension panelSize;
	private JTree tree;
	private JScrollPane treeScroll;
	private JTModel model;
	private JSplitPane split;
	private JLabel statusText;
	private JDesktopPane work;
	private Object treeSelection;
	private Component dragSelection;
	private ObjectPanel objectStatus;
	private Vector multiSelect;
	private IconLibrary icons;
	private SystemProps systemProps;
	private SystemProps xSystemProps;
	private DeviceProps deviceProps;
	private PanelProps panelProps;
	private FrameProps frameProps;
	private ButtonProps buttonProps;
	private ButtonPasteDialog buttonPaste;
	private Object lastAlias;
	private boolean isnew;
	private boolean changed;
	private boolean hideSelection;
	private int nextDoID;

	// temp vars
	private CCF newCCF;
	private TreeNode treeDrag;
	private Object treeTarget;
	private Border treeBorder;
	private boolean treeBefore;
	private CCFDevice currentDevice;
	private CCFPanel currentPanel;

	// ---( static methods )---
	public static void main(String args[])
		throws Exception
	{
		// look for running Tonto
		if (args.length > 0)
		{
			try
			{
				Socket s = new Socket(InetAddress.getLocalHost(), 9911);
				OutputStream os = s.getOutputStream();
				os.write("load ".getBytes());
				for (int i=0; i<args.length; i++)
				{
					os.write(URLEncoder.encode(args[i]).getBytes());
					os.write(' ');
				}
				os.flush();
				s.close();
				return;
			}
			catch (Exception ex) { }
		}

		// splash screen
		splash = loadImage("images/splash.jpg");
		Window w = new Window(new Frame("splash screen"));
		Dimension d = w.getToolkit().getScreenSize();
		MediaTracker m = new MediaTracker(w);
		m.addImage(splash,1);
		w.add(getTitleCanvas());
		w.pack();
		w.setLocation(d.width/2-120, d.height/2-100);
		m.waitForAll();
		w.show();

		// load first tab
		Tonto t = new Tonto();
		w.dispose();
		if (args.length > 0)
		{
			for (int i=0; i<args.length; i++)
			{
				t.load(args[i], t.autoLoad.size() > 0 || i > 0);
			}
		}
	}

	// -------------------------------------------------------------------------------------
	// C O N S T R U C T O R
	// -------------------------------------------------------------------------------------
	// the order of everything in this constructor is critical
	private Tonto()
	{
		String forceLoad = init();

		// was in shared ... conflicts w/ multiple windows
		systemProps = new SystemProps(false);
		xSystemProps = new SystemProps(true);
		deviceProps = new DeviceProps();
		panelProps = new PanelProps();
		buttonProps = new ButtonProps();
		frameProps = new FrameProps();
		buttonPaste = new ButtonPasteDialog();

		// init defaults
		isnew = false;
		changed = false;
		panelSize = new Dimension(240,220);
		nextPosition = new Point(2,2);
		undoStack = new Stack();
		redoStack = new Stack();
		multiSelect = new Vector();

		// setup globals
		StackedDialog.setDefaultParent(jframe);
		statusText = newLabel("                                   ");
		icons = new IconLibrary();
		model = new JTModel();
		panels = new Hashtable();
		treeBorder = new DragBorder();

		// setup ccf tree
		tree = new JTree(model);
		Font f = tree.getFont();
		if (prefUseProntoFont)
		{
			tree.setFont(getPFont());
		}
		tree.putClientProperty("JTree.lineStyle", "Angled");
		tree.setCellRenderer(new DefaultTreeCellRenderer()
		{
			public Component getTreeCellRendererComponent(
				JTree tree, Object val, boolean sel, boolean exp, boolean leaf,
				int row, boolean focus)
			{
				JLabel o = (JLabel)super.getTreeCellRendererComponent(
					tree, val, sel, exp, leaf, row, focus
				);
				if (val instanceof TreeProperties)
				{
					o.setIcon(iconProperties);
					o.setForeground(new Color(0,0,100));
				}
				else
				if (val instanceof CCFTreePanel)
				{
					o.setIcon(iconPanel);
				}
				if (!leaf)
				{
					o.setIcon(exp ? iconFolderOpened : iconFolderClosed);
				}
				if (val == treeTarget)
				{
					o.setBorder(treeBorder);
				}
				else
				{
					o.setBorder(null);
				}
				return o;
			}
		});
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.addTreeSelectionListener(this);
		tree.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent ke) {
				if (ke.getKeyChar() == KeyEvent.VK_ENTER) {
					if (treeSelection instanceof CCFTreePanel)
					{
						debug.log(2, "show panel : "+Util.nickname(treeSelection));
						showDeskPanel(((CCFTreePanel)treeSelection).getPanel());
					}
				}
			}
		});
		tree.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent me) {
				Object ott = treeTarget;
				boolean otb = treeBefore;
				Point p = me.getPoint();
				TreePath tp = tree.getPathForLocation(p.x,p.y);
				Rectangle r = tree.getPathBounds(tp);
				if (r != null)
				{
					treeBefore = (p.y-r.y) < (r.height/2);
				}
				Object o = (tp != null ? tp.getLastPathComponent() : null);
				if (treeDrag == null && o instanceof TreeNode)
				{
					if (!(o instanceof Deletable))
					{
						return;
					}
					treeDrag = (TreeNode)o;
				}
				else
				if (o != null && o != treeDrag && (o.getClass() == treeDrag.getClass() || treeDrag.getParent().getClass() == o.getClass()))
				{
					if (o.getClass() != treeDrag.getClass())
					{
						treeBefore = false;
					}
					tree.setCursor(CURSOR_MOVE);
					treeTarget = o;
				}
				else
				{
					treeTarget = null;
					tree.setCursor(CURSOR_DEFAULT);
				}
				if (treeTarget != ott || treeBefore != otb)
				{
					tree.repaint();
				}
				// auto-scroll tree
				JViewport vp = treeScroll.getViewport();
				Rectangle vis = vp.getViewRect();
				Dimension all = vp.getViewSize();
				int top = vis.y;
				int bottom = vis.y + vis.height - 30;
				if (top > 0 && p.y - 30 < top)
				{
					vp.setViewPosition(new Point(vis.x, Math.max(0, vis.y - 10)));
				}
				else
				if (p.y + 30 > bottom && bottom < all.height)
				{
					vp.setViewPosition(new Point(vis.x, Math.min(all.height-vis.height, vis.y + 10)));
				}
			}
		});
		tree.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e)
			{
				if (treeDrag != null && treeTarget != null)
				{
					if (treeDrag instanceof CCFTreePanel)
					{
						CCFDevice ddv = null;
						CCFPanel dst = null;
						if (treeTarget instanceof CCFTreeDevice)
						{
							ddv = ((CCFTreeDevice)treeTarget).getDevice();
							dst = ddv.firstPanel;
							while (dst != null && dst.next != null)
							{
								dst = dst.next;
							}
						}
						else
						{
							dst = ((CCFTreePanel)treeTarget).getPanel();
							ddv = dst.getParentDevice();
						}
						CCFTreePanel tpSrc = (CCFTreePanel)treeDrag;
						CCFPanel src = tpSrc.getPanel();
						// only do if not macro panel
						if (ddv != null && src.getParentDevice() != null)
						{
							TreeNode osp = tpSrc.getParent();
							TreeNode tnDst = (TreeNode)treeTarget;
							src.delete();
							if (dst == null)
							{
								ddv.addPanel(src);
							}
							else
							if (treeBefore)
							{
								dst.insertBefore(src);
							}
							else
							{
								dst.insertAfter(src);
							}
							if (tnDst instanceof CCFTreePanel)
							{
								tnDst.getParent().refresh();
							}
							else
							{
								tnDst.refresh();
							}
							osp.refresh();
							setCCFChanged();
						}
					}
					else
					if (treeDrag instanceof CCFTreeDevice)
					{
						CCFDevice dst = null;
						if (treeTarget instanceof CCFTreeDeviceFolder)
						{
							dst = ((CCFTreeDeviceFolder)treeTarget).getRootDevice();
						}
						else
						{
							dst = ((CCFTreeDevice)treeTarget).getDevice();
							if (dst == null)
							{
								treeTarget = ((CCFTreeDevice)treeTarget).getParent();
							}
						}
						CCFTreeDevice tpSrc = (CCFTreeDevice)treeDrag;
						CCFDevice src = tpSrc.getDevice();
						TreeNode osp = tpSrc.getParent();
						TreeNode tpDst = (TreeNode)treeTarget;
						src.delete();
						if (dst == null)
						{
							((CCFTreeDeviceFolder)treeTarget).setRootDevice(src);
						}
						else
						if (treeBefore)
						{
							dst.insertBefore(src);
						}
						else
						{
							dst.insertAfter(src);
						}
						if (tpDst instanceof CCFTreeDevice)
						{
							tpDst.getParent().refresh();
						}
						else
						{
							tpDst.refresh();
						}
						osp.refresh();
						setCCFChanged();
					}
				}
				treeDrag = null;
				treeTarget = null;
				tree.setCursor(CURSOR_DEFAULT);
				tree.repaint();
			}
			public void mousePressed(MouseEvent e)
			{
				int selRow = tree.getRowForLocation(e.getX(), e.getY());
				TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
				if (selRow != -1)
				{
					setTreeSelection(selPath.getLastPathComponent());
					tree.setSelectionRow(selRow);
				}
			}

			public void mouseClicked(MouseEvent e)
			{
				int selRow = tree.getRowForLocation(e.getX(), e.getY());
				TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
				if (selRow != -1)
				{
					setTreeSelection(selPath.getLastPathComponent());
					if (e.getClickCount() == 1)
					{
						if (isRightClick(e) && ccf != null)
						{
							rightClickMenu(treeSelection, e.getPoint());
						}
					}
					else
					if (e.getClickCount() == 2)
					{
						if (treeSelection instanceof CCFTreePanel)
						{
							showDeskPanel(((CCFTreePanel)treeSelection).getPanel());
						}
						else
						if (treeSelection instanceof CCFTreeDeviceProperty)
						{
							setTreeSelection(selPath.getPathComponent(selPath.getPathCount()-2));
							sendEvent(EDIT_PROPERTY);
						}
						else
						if (treeSelection instanceof CCFTreeSystemProperty)
						{
							editSystemProperties();
						}
					}
				}
			}
		});

		work = new JDesktopPane();
		work.setBackground(Color.lightGray);
		objectStatus = new ObjectPanel();

		treeScroll = new JScrollPane(tree);

		bindActionKeys(work);
		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, treeScroll, work);
		split.setDividerLocation(prefHSplitPos);	

		JPanel c = new JPanel();
		c.setLayout(new BorderLayout());
		c.add("Center", split);
		c.add("South", objectStatus);

		addWorkspace(this, c);

		if (forceLoad != null)
		{
			load(forceLoad);
		}
		else
		{
			file_new();
		}
	}

	private static void addWorkspace(Tonto t, JComponent c)
	{
		tontos.put(c, t);
		tontos.put(t, c);
		int idx = windows.getSelectedIndex();
		//windows.add("<unnamed>", c);
		windows.add(c, "<unnamed>", idx+1);
		windows.setSelectedComponent(c);
	}

	// -------------------------------------------------------------------------------------
	// S E T U P   A N D   P R E F E R E N C E S
	// -------------------------------------------------------------------------------------
	public static int irVersion()
	{
		return ccf().usesUDB() ? Pronto.VERSION2 : Pronto.VERSION1;
	}

	public static int iconMode()
	{
		return custom() ? CCFIcon.MODE_32BIT : color() ? CCFIcon.MODE_8BIT : CCFIcon.MODE_2BIT;
	}

	public static NameDialog nameDialog()
	{
		if (!custom())
		{
			return dialogName;
		}
		if (dialogName2 == null)
		{
			dialogName2 = new NameDialog(false);
		}
		return dialogName2;
	}

	public static boolean custom()
	{
		return ccf().getConformsTo(prefDefaultModel).isCustom();
	}

	public static boolean color()
	{
		return ccf().header().hasColor();
	}

	public static boolean udb()
	{
		return ccf().usesUDB();
	}

	public static CCFHeader header()
	{
		return ccf().header();
	}

	public static CCF ccf()
	{
		return state().ccf;
	}

	public static Tonto state()
	{
		return current;
	}

	public static IRDatabase database()
	{
		return database;
	}

	public static CCFDevice device()
	{
		return state().getCurrentDevice();
	}

	public static CCFPanel panel()
	{
		return state().getCurrentPanel();
	}

	public static Dimension panelSize()
	{
		return state().panelSize;
	}

	private static void browseForFile(String title, JTextField target)
	{
		File f = getFile(true, title, null);
		if (f != null && f.exists())
		{
			target.setText(f.toString());
		}
	}

	private static String init()
	{
		if (initShared)
		{
			return null;
		}

		// command-line listener
		startCommandListener();

		// static setup globals/defaults
		tontos = new Hashtable();
		prefUseAWTFileDialogs = true;
		prefGrid = new Rectangle(0,0,5,5);
		homeDir = new File(Util.sysprop("user.home"));
		prefDir = new File(homeDir, (Util.onWindows() || Util.onMacintosh()) ? "tonto" : ".tonto");
		altPrefDir = new File(homeDir, ".tonto");
		prefFile = new File(prefDir, "preferences");
		dbDir = new File(prefDir, "db");
		logDir = new File(prefDir, "logs");
		ccfDir = new File(prefDir, "ccf");
		moduleDir = new File(prefDir, "module");
		prefWorkingDir = prefDir;
		prefImageDir = prefDir;
		defaultDB = new File(dbDir, "default.db");
		recentFiles = new Vector();
		autoLoad = new Vector();
		menus = new Hashtable();
		popmenus = new Hashtable();
		tools = new Hashtable();
		border = new LineBorder(Color.black, 1);
		sessionUpdate = true;
		recentMenu = new Menu("Recent Files");
		snaps = new SnapInfo(0.0, 0.0);

		// fixup old installs, rename prefs dir
		// TODO: remove after the next major release
		if (Util.onMacintosh())
		{
			if (altPrefDir.exists() && !prefDir.exists())
			{
				altPrefDir.renameTo(prefDir);
			}
		}

		// create directories
		prefDir.mkdirs();
		dbDir.mkdirs();
		logDir.mkdirs();
		ccfDir.mkdirs();
		moduleDir.mkdirs();

		// clean old logs
		clearLogs();

		// select font table based on OS and JVM
		setFontTable();

		// load preferences
		loadPreferences();

		// setup logging if necessary
		setLogging(prefLogToConsole);

		// startup debug
		debug.log(0,"Tonto "+version()+" started "+new Date());
		debug.log(1,"os = "+Util.sysprop("os.name")+" "+Util.sysprop("os.arch")+" "+Util.sysprop("os.version"));
		debug.log(0,"java = "+Util.sysprop("java.vm.vendor")+" "+Util.sysprop("java.vm.version"));
		debug.log(1,"prefs = "+prefDir);

		// font and menu handles
		setupMacSpecific();

		// load fonts
		loadFonts();

		// load gallery ccf
		loadGalleryCCF();

		// setup desktop title fonts
		UIManager.put("InternalFrame.font", new FontUIResource(getPFont()));
		UIManager.put("InternalFrame.titleFont", new FontUIResource(getPFont()));
		UIManager.getLookAndFeelDefaults().put("InternalFrame.font", new FontUIResource(getPFont()));
		UIManager.getLookAndFeelDefaults().put("InternalFrame.titleFont", new FontUIResource(getPFont()));

		/*
		UIManager.put("Tree.line", Color.red);
		UIManager.getLookAndFeelDefaults().put("Tree.line", Color.red);
		*/

		// preload default ir database
		try
		{
			if (!defaultDB.exists())
			{
				defaultDB.createNewFile();
			}
		}
		catch (Exception ex)
		{
			debug(ex);
		}
		try
		{
			database = IRDatabase.open(defaultDB.toString());
		}
		catch (Exception ex)
		{
			debug(ex);
		}

		// setup shared dialogs
		dialogName = new NameDialog();
		timerEditor = new TimerEditor();
		prefsDialog = new PrefsDialog();
		irdb = new IRDBDialog();

		// load icons & images
		iconFolderOpened = loadIcon("/images/iconFolderOpened.gif");
		iconFolderClosed = loadIcon("/images/iconFolderClosed.gif");
		iconProperties = loadIcon("/images/iconProperties.gif");
		iconPanel = loadIcon("/images/iconPanel.gif");
		imageTontoSmall = loadImage("/images/tonto.gif");

		// menu setup
		MenuBar m = new MenuBar();

		// file new and convert sub-menus
		fileNewMenu = new Menu("New With Type");
		convertMenu = new Menu("Convert Type");
		ProntoModel units[] = ProntoModel.getModels();
		for (int i=0; i<units.length; i++)
		{
			fileNewMenu.add(newMenuItem(units[i].getName(), -1, BASE_FILE_NEW + units[i].getModel(), true));
			convertMenu.add(newMenuItem(units[i].getName(), -1, BASE_CONVERT + units[i].getModel(), true));
		}
		convertMenu.setEnabled(false);

		buildRevertMenu();

		fileNew = newMenuItem("New "+prefDefaultModel.getName(), KeyEvent.VK_N, FILE_NEW);

		Menu m_auto = new Menu("Auto Load");
			m_auto.add(newMenuItem("Add this file",     -1,            AUTOLOAD_ADD, true));
			m_auto.add(newMenuItem("Remove this file",  -1,            AUTOLOAD_REMOVE, true));

		Menu m_file = new Menu("File");
			m_file.add(fileNew);
			m_file.add(fileNewMenu);
			m_file.add(newMenuItem("Close",             -1,            FILE_CLOSE_WINDOW, true));
			m_file.addSeparator();
			m_file.add(newMenuItem("Open...",           KeyEvent.VK_O, FILE_LOAD, true));
			m_file.add(newMenuItem("Merge...",          KeyEvent.VK_M, FILE_MERGE, true));
			m_file.add(newMenuItem("Revert",            -1,            FILE_REVERT));
			m_file.add(newMenuItem("Upload from Remote",KeyEvent.VK_U, FILE_CCF_GET, hasComm));
			m_file.addSeparator();
			m_file.add(newMenuItem("Save",              KeyEvent.VK_S, FILE_SAVE));
			m_file.add(newMenuItem("Save As...",        -1,            FILE_SAVEAS));
			m_file.add(newMenuItem("Download to Remote",KeyEvent.VK_D, FILE_CCF_PUT, false));
			m_file.addSeparator();
			m_file.add(newMenuItem("Export Zip...",     -1,            FILE_EXPORT));
			m_file.addSeparator();
			m_file.add(recentMenu);
			m_file.add(m_auto);
			m_file.addSeparator();
			m_file.add(newMenuItem("Preferences...",    -1,            FILE_PREFERENCES, true));
			m_file.addSeparator();
			m_file.add(newMenuItem("Exit",              KeyEvent.VK_Q, FILE_EXIT, true));

		Menu m_edit = new Menu("Edit");
			m_edit.add(newMenuItem("Undo",              KeyEvent.VK_Z, EDIT_UNDO));
			m_edit.add(newMenuItem("Redo",              KeyEvent.VK_R, EDIT_REDO));
			m_edit.addSeparator();
			m_edit.add(newMenuItem("Cut",               KeyEvent.VK_X, EDIT_CUT));
			m_edit.add(newMenuItem("Copy",              KeyEvent.VK_C, EDIT_COPY));
			m_edit.add(newMenuItem("Delete",            KeyEvent.VK_DELETE, EDIT_DELETE));
			m_edit.add(newMenuItem("Paste",             KeyEvent.VK_V, EDIT_PASTE));
			m_edit.addSeparator();                     
			m_edit.add(newMenuItem("Select All",        KeyEvent.VK_A, EDIT_SELECT_ALL));
			m_edit.add(newMenuItem("Unselect All",      -1,            EDIT_UNSELECT_ALL));
			m_edit.addSeparator();                     
			m_edit.add(newMenuItem("Copy Alias",        -1,            COPY_ALIAS, false, true));
			m_edit.add(newMenuItem("Paste Alias",       -1,            PASTE_ALIAS, false, true));
			m_edit.addSeparator();                     
			m_edit.add(newMenuItem("Properties...",     KeyEvent.VK_P, EDIT_PROPERTY));

		snapMenu = new Menu("Snap");
			snapMenu.add(newMenuItem("Top Left",        -1,            SNAP_TOP_LEFT, true));
			snapMenu.add(newMenuItem("Top Right",       -1,            SNAP_TOP_RIGHT, true));
			snapMenu.add(newMenuItem("Bottom Right",    -1,            SNAP_BOTTOM_RIGHT, true));
			snapMenu.add(newMenuItem("Bottom Left",     -1,            SNAP_BOTTOM_LEFT, true));
			/*
			snapMenu.add(newMenuItem("Top",             -1,            SNAP_TOP, true));
			snapMenu.add(newMenuItem("Right",           -1,            SNAP_RIGHT, true));
			snapMenu.add(newMenuItem("Bottom",          -1,            SNAP_BOTTOM, true));
			snapMenu.add(newMenuItem("Left",            -1,            SNAP_LEFT, true));
			*/

		orderMenu = new Menu("Order");
			orderMenu.add(newMenuItem("Bring to Front", -1,            OBJECT_TOP, true));
			orderMenu.add(newMenuItem("Send to Back",   -1,            OBJECT_BOTTOM, true));
			orderMenu.add(newMenuItem("Bring Forward",  -1,            OBJECT_RAISE, true));
			orderMenu.add(newMenuItem("Send Backward",  -1,            OBJECT_LOWER, true));

		alignMenu = new Menu("Align");
			alignMenu.add(newMenuItem("Top",            -1,            ALIGN_TOP, true));
			alignMenu.add(newMenuItem("Bottom",         -1,            ALIGN_BOTTOM, true));
			alignMenu.add(newMenuItem("Left",           -1,            ALIGN_LEFT, true));
			alignMenu.add(newMenuItem("Right",          -1,            ALIGN_RIGHT, true));
			alignMenu.add(newMenuItem("H-Center",       -1,            ALIGN_HCENTER, true));
			alignMenu.add(newMenuItem("V-Center",       -1,            ALIGN_VCENTER, true));

		Menu m_objt = new Menu("Object");
			m_objt.add(newMenuItem("Add Device",        KeyEvent.VK_D, ADD_DEVICE, false, true));
			m_objt.add(newMenuItem("Add Panel",         KeyEvent.VK_P, ADD_PANEL, false, true));
			m_objt.add(newMenuItem("Add Frame",         KeyEvent.VK_F, ADD_FRAME, false, true));
			m_objt.add(newMenuItem("Add Button",        KeyEvent.VK_B, ADD_BUTTON, false, true));
			m_objt.addSeparator();                     
			m_objt.add(snapMenu);
			m_objt.add(orderMenu);
			m_objt.add(alignMenu);
			m_objt.addSeparator();                     
			m_objt.add(newMenuItem("Group",             KeyEvent.VK_G, OBJECTS_GROUP, false, true));
			m_objt.add(newMenuItem("Ungroup",           KeyEvent.VK_U, OBJECTS_UNGROUP, false, true));
			m_objt.addSeparator();                     
			m_objt.add(newMenuItem("Transparent",       KeyEvent.VK_T, OBJECT_TRANSPARENT, false, true));

		Menu m_remo = new Menu("Remote");
			m_remo.add(newMenuItem("Raw Download...",   -1,            UTIL_DOWNLOAD, hasComm));
			m_remo.add(newMenuItem("Raw Upload...",     -1,            UTIL_UPLOAD, hasComm));
			m_remo.addSeparator();                     
			m_remo.add(newMenuItem("Locate Remote...",  KeyEvent.VK_F, UTIL_LOCATE, hasComm));
			m_remo.add(newMenuItem("Reboot Remote",     -1,            UTIL_REBOOT, hasComm));
			m_remo.addSeparator();                     
			m_remo.add(newMenuItem("Undead Remote...",  -1,            UTIL_UNDEAD, hasComm));
			m_remo.add(newMenuItem("Update Firmware...",-1,            UTIL_FIRMWARE, hasComm));

		Menu m_view = new Menu("View");
			m_view.add(newMenuItem("Expand All",        -1,            TREE_EXPAND_ALL, true));
			m_view.add(newMenuItem("Collapse All",      -1,            TREE_COLLAPSE_ALL, true));
			m_view.addSeparator();
			m_view.add(newMenuItem("Toggle Grid",       -1,            TOGGLE_GRID, false));
			m_view.add(newMenuItem("Toggle Snap",       -1,            TOGGLE_SNAP, false));
			m_view.addSeparator();
			m_view.add(newMenuItem("Toggle Selection",  KeyEvent.VK_H, VIEW_HIDE_SELECT, true));

		Menu m_util = new Menu("Utility");
			m_util.add(convertMenu);
			m_util.addSeparator();                     
			m_util.add(newMenuItem("Launch Emulator",   KeyEvent.VK_E, UTIL_EMULATOR, false));
			m_util.add(newMenuItem("Launch Editor",     KeyEvent.VK_K, UTIL_EDITOR, false));
			m_util.addSeparator();                     
			m_util.add(newMenuItem("Check For Update",  -1,            UTIL_NET_UPDATE, true));
			m_util.add(revertMenu);

		Menu m_mods = new Menu("Modules");              
			m_mods.add(newMenuItem("IR Database",       KeyEvent.VK_I, IR_DATABASE, true));
			m_mods.addSeparator();                     
			m_mods.add(newMenuItem("Icon Library",      -1,            ICON_LIBRARY, false));
			m_mods.add(newMenuItem("Icon Replace",      -1,            ICON_REPLACE, false));
			m_mods.addSeparator();                     
			m_mods.add(newMenuItem("Theme Master",      -1,            THEME_MASTER, false));
			m_mods.add(newMenuItem("Tab Master",        -1,            TAB_MASTER, false));

		Menu m_link = new Menu("Links");              
			m_link.add(newMenuItem("FAQ",               -1,            DOCS_FAQ, true));
			m_link.add(newMenuItem("Guide",             -1,            DOCS_MAIN, true));
			m_link.add(newMenuItem("Tutorials",         -1,            DOCS_TUTOR, true));
			m_link.add(newMenuItem("Changelog",         -1,            DOCS_CHANGES, true));
			m_link.addSeparator();                     
			m_link.add(newMenuItem("CCF Library",       -1,            HELP_CCFS, true));
			m_link.add(newMenuItem("Discrete Codes",    -1,            HELP_DISCRETE, true));

		Menu m_help = new Menu("Help");
			m_help.add(newMenuItem("About Tonto",       -1,            ABOUT, true));
			m_help.addSeparator();                     
			m_help.add(newMenuItem("Email Logfile",     -1,            EMAIL_LOGFILE, true));
			m_help.add(newMenuItem("Log Window",        -1,            DEBUG_WINDOW, true));

		m.add(m_file);
		m.add(m_edit);
		m.add(m_objt);
		m.add(m_remo);
		m.add(m_view);
		m.add(m_util);
		m.add(m_mods);
		m.add(m_link);
		m.setHelpMenu(m_help);

		menuEnable(FILE_NEW, true);
		orderMenu.setEnabled(false);
		alignMenu.setEnabled(false);

		// right click menus
		rcDeviceAliasKey = new JMenu("Copy Alias", true);
		rcDevicePasteKey = new JMenu("Paste Alias", true);
		rcDeviceCopyActions = new JMenu("Copy Actions", true);
		rcDevicePasteActions = new JMenu("Paste Actions", true);

		rcDevice = new JPopupMenu();
			rcDevice.add(newPopMenuItem("Add Panel", ADD_PANEL));
			rcDevice.add(new JSeparator());
			rcDevice.add(rcDeviceCopyActions);
			rcDevice.add(rcDeviceAliasKey);
			rcDevice.add(newPopMenuItem("Copy Keys", COPY_KEYS));
			rcDevice.add(new JSeparator());
			rcDevice.add(rcDevicePasteActions);
			rcDevice.add(rcDevicePasteKey);
			rcDevice.add(newPopMenuItem("Paste Keys", PASTE_KEYS));
			rcDevice.add(new JSeparator());
			rcDevice.add(newPopMenuItem("Properties...", EDIT_PROPERTY));
			rcDevice.add(new JSeparator());
			rcDevice.add(newPopMenuItem("Delete", EDIT_DELETE));

		rcFrame = new JPopupMenu();
			rcFrame.add(newPopMenuItem("Add Frame", ADD_FRAME));
			rcFrame.add(newPopMenuItem("Add Button", ADD_BUTTON));
			rcFrame.add(new JSeparator());
			rcFrame.add(newPopToggleMenuItem("Show Grid", TOGGLE_GRID));
			rcFrame.add(newPopToggleMenuItem("Snap To Grid", TOGGLE_SNAP));
			rcFrame.add(new JSeparator());
			rcFrame.add(newPopMenuItem("Group", OBJECTS_GROUP));
			rcFrame.add(newPopMenuItem("UnGroup", OBJECTS_UNGROUP));
			rcFrame.add(new JSeparator());
			rcFrame.add(newPopMenuItem("Properties...", EDIT_PROPERTY));
			rcFrame.add(new JSeparator());
			rcFrame.add(newPopMenuItem("Delete", EDIT_DELETE));

		rcPanel = new JPopupMenu();
			rcPanel.add(newPopMenuItem("Add Frame", ADD_FRAME));
			rcPanel.add(newPopMenuItem("Add Button", ADD_BUTTON));
			rcPanel.add(new JSeparator());
			rcPanel.add(newPopToggleMenuItem("Show Grid", TOGGLE_GRID));
			rcPanel.add(newPopToggleMenuItem("Snap To Grid", TOGGLE_SNAP));
			rcPanel.add(new JSeparator());
			rcPanel.add(newPopMenuItem("Properties...", EDIT_PROPERTY));
			rcPanel.add(new JSeparator());
			rcPanel.add(newPopMenuItem("Delete", EDIT_DELETE));

		rcButton = new JPopupMenu();
			rcButton.add(newPopMenuItem("Copy Alias", COPY_ALIAS));
			rcButton.add(newPopMenuItem("Copy Icons", COPY_ICONS));
			rcButton.add(newPopMenuItem("Copy Actions", COPY_ACTIONS));
			rcButton.add(new JSeparator());
			rcButton.add(newPopMenuItem("Paste Alias", PASTE_ALIAS));
			rcButton.add(newPopMenuItem("Paste Icons", PASTE_ICONS));
			rcButton.add(newPopMenuItem("Paste Actions", PASTE_ACTIONS));
			rcButton.add(newPopMenuItem("Paste Replace Icons", PASTE_REPLACE_ICONS));
			rcButton.add(new JSeparator());
			rcButton.add(newPopMenuItem("Group", OBJECTS_GROUP));
			rcButton.add(new JSeparator());
			rcButton.add(newPopToggleMenuItem("Show Grid", TOGGLE_GRID));
			rcButton.add(newPopToggleMenuItem("Snap To Grid", TOGGLE_SNAP));
			rcButton.add(new JSeparator());
			rcButton.add(newPopMenuItem("Properties...", EDIT_PROPERTY));
			rcButton.add(new JSeparator());
			rcButton.add(newPopMenuItem("Delete", EDIT_DELETE));

		rcDGroup = new JPopupMenu();
			rcDGroup.add(newPopMenuItem("Add Device", ADD_DEVICE));

		// setup main frame
		jframe = new JFrame("Tonto "+version());
		jframe.setIconImage(imageTontoSmall);
		jframe.setDefaultCloseOperation(jframe.DO_NOTHING_ON_CLOSE);
		jframe.addWindowListener(new WindowListener()
			{
				public void windowActivated(WindowEvent e) { }
				public void windowClosed(WindowEvent e) { }
				public void windowClosing(WindowEvent e) { exitAll(); }
				public void windowDeactivated(WindowEvent e) { }
				public void windowDeiconified(WindowEvent e) { }
				public void windowIconified(WindowEvent e) { }
				public void windowOpened(WindowEvent e) { }
			}
		);

		// jframe bindings
		JComponent fc = (JComponent)jframe.getContentPane();
		Action close = new EventAction(FILE_CLOSE_PANEL);
		bindAction(fc, KeyEvent.VK_W,KeyEvent.META_MASK,close);
		bindAction(fc, KeyEvent.VK_W,KeyEvent.CTRL_MASK,close);
		bindAction(fc, KeyEvent.VK_DELETE, 0, new EventAction(EDIT_DELETE));

		// setup tabs
		windows = new JTabbedPane();
		windows.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent ce) {
				tabSelect();
			}
		});

		// setup toolbar
		tbar = new JToolBar();
		tbar.setFloatable(false);
		addTool("New",         "New",                          FILE_WINDOW,       "New Tab");
		addTool("Close",       "Close",                        FILE_CLOSE_WINDOW, "Close");
		addTool();                                                            
		addTool("Open",        "Open",                         FILE_LOAD,         "Open a File");
		addTool("Save",        "Save",                         FILE_SAVE,         "Save File");
		addTool("SaveAs",      "SaveAs",                       FILE_SAVEAS,       "Save File As");
		addTool();                                                               
		addTool("Upload",      "Export",                       FILE_CCF_GET,      "Upload from Remote");
		addTool("Download",    "Import",                       FILE_CCF_PUT,      "Download to Remote");
		addTool();                                                               
		addTool("Undo",        "Undo",                         EDIT_UNDO,         "Undo");
		addTool("Redo",        "Redo",                         EDIT_REDO,         "Redo");
		addTool();                                                               
		addTool("Cut",         "Cut",                          EDIT_CUT,          "Cut");
		addTool("Copy",        "Copy",                         EDIT_COPY,         "Copy");
		addTool("Paste",       "Paste",                        EDIT_PASTE,        "Paste");
		addTool("Delete",      "Delete",                       EDIT_DELETE,       "Delete");
		addTool("Properties",  "Properties",                   EDIT_PROPERTY,     "Properties");
		addTool();                                                               
		addTool("Group",       "Group",                        OBJECTS_GROUP,     "Group");
		addTool("UnGroup",     "UnGroup",                      OBJECTS_UNGROUP,   "UnGroup");
		addTool("Grid",        "Grid",                         TOGGLE_GRID,       "Toggle Grid");
		addTool("GridSnap",    "GridSnap",                     TOGGLE_SNAP,       "Toggle Grid Snap");
		addTool();                                                               
		addTool("Top",         "AlignTop",                     ALIGN_TOP,         "Align Top");
		addTool("Bottom",      "AlignBottom",                  ALIGN_BOTTOM,      "Align Bottom");
		addTool("Left",        "AlignLeft",                    ALIGN_LEFT,        "Align Left");
		addTool("Right",       "AlignRight",                   ALIGN_RIGHT,       "Align Right");
		addTool("Horiz",       "AlignJustifyVertical",         ALIGN_HCENTER,     "Center Horizontal");
		addTool("Vert",        "AlignJustifyHorizontal",       ALIGN_VCENTER,     "Center Vertical");
		addTool();                                                               
		addTool("Preferences", "Preferences",                  FILE_PREFERENCES,  "Preferences");
		addTool("Emulator",    "Emulator",                     UTIL_EMULATOR,     "Emulator");
		addTool("Editor",      "Editor",                       UTIL_EDITOR,       "Editor");
		addTool();                                                               
		addTool("Logfile",     "ComposeMail",                  EMAIL_LOGFILE,     "Send Logfile");
		addTool("Help",        "Help",                         DOCS_FAQ,          "FAQ");

		fc.setLayout(new BorderLayout());
		fc.add("North", tbar);
		fc.add("Center", windows);

		// finish setup
		jframe.setMenuBar(m);
		jframe.pack();
		jframe.setSize(900,700);
		Rectangle rect = prefs.getRectangle(PREF_WINDOW_BOUNDS, jframe.getBounds());
		jframe.setBounds(rect);
		jframe.setVisible(true);
		if (!jframe.getBounds().equals(rect))
		{
			debug.log(3, "0: window bounds mismatch: "+jframe.getBounds()+" != "+rect);
		}
		jframe.setBounds(rect);
		if (!jframe.getBounds().equals(rect))
		{
			debug.log(3, "1: window bounds mismatch: "+jframe.getBounds()+" != "+rect);
		}

		// check for comm libraries
		if (!hasComm)
		{
			infoDialog("Tonto cannot find the serial communication\nLibraries. Upload/Downoad has been disabled.");
		}

		// locate jar dir and start dynamic updating
		setupNetworkUpdates();

		initShared = true;

		// look for auto-load documents
		String forceLoad = null;
		if (autoLoad.size() > 0)
		{
			for (int i=0; i<autoLoad.size()-1; i++)
			{
				Tonto t = new Tonto();
				t.load((String)autoLoad.get(i));
			}
			forceLoad = (String)autoLoad.get(autoLoad.size()-1);
		}

		debug.log(0, "initialized shared state");

		return forceLoad;
	}
	
	private static void buildRevertMenu()
	{
		File running = getJar();
		Vector collect = new Vector();
		revertMenu = new Menu("Revert Tonto");
		revertMenu.setEnabled(true);
		File jd = getJarDir();
		if (jd != null && jd.isDirectory())
		{
			String l[] = jd.list();
			loop: for (int i=0; i<l.length; i++)
			{
				String ln = l[i].toLowerCase();
				if (ln.startsWith("tonto-") && ln.endsWith(".jar"))
				{
					File nf = new File(jd, l[i]);
					if (!nf.equals(running))
					{
						for (int j=0; j<collect.size(); j++)
						{
							if (nf.lastModified() > ((File)collect.get(j)).lastModified())
							{
								collect.insertElementAt(nf, j);
								continue loop;
							}
						}
						collect.add(nf);
					}
				}
			}
			for (int i=0; i<collect.size(); i++)
			{
				File f = (File)collect.get(i);
				if (i < 5)
				{
					String sn = f.getName();
					sn = sn.substring(6, sn.lastIndexOf("."));
					revertMenu.add(newMenuItem(sn, new RevertMenuAction(f, sn)));
				}
				else
				{
					debug.log(0, "deleting old version: "+f);
					f.delete();
				}
			}
		}
	}

	private static void addTool()
	{
		tbar.addSeparator();
	}

	private static void addTool(String name, String icon, int action, String desc)
	{
		ToolAction ta = new ToolAction(name, "/toolbar/general/"+icon+"16.gif", action, desc);
		tools.put(new Integer(action), ta);
		tbar.add(ta);
	}

	private static void tabSelect()
	{
		int idx = windows.getSelectedIndex();
		if (idx >= 0)
		{
			Component c = windows.getSelectedComponent();
			current = (Tonto)tontos.get(c);
			if (current != null)
			{
				current.updateCCFInfo();
				current.updateMenuState();
				debug.log(3, "tab change: "+idx+" current="+Util.nickname(current));
			}
		}
	}

	private static void setupMacSpecific()
	{
		if (!Util.onMacintosh())
		{
			return;
		}
		try
		{

		// install preference and about handlers
		com.apple.mrj.MRJApplicationUtils.registerPrefsHandler(
			new com.apple.mrj.MRJPrefsHandler() {
				public void handlePrefs() {
					sendEvent(FILE_PREFERENCES);
				}
			}
		);
		com.apple.mrj.MRJApplicationUtils.registerAboutHandler(
			new com.apple.mrj.MRJAboutHandler() {
				public void handleAbout() {
					sendEvent(ABOUT);
				}
			}
		);
		com.apple.mrj.MRJApplicationUtils.registerOpenDocumentHandler(
			new com.apple.mrj.MRJOpenDocumentHandler() {
				public void handleOpenFile(File file) {
					load(file.toString(), true);
				}
			}
		);
		com.apple.mrj.MRJApplicationUtils.registerQuitHandler(
			new com.apple.mrj.MRJQuitHandler() {
				public void handleQuit() {
					exitAll();
				}
			}
		);
		com.apple.mrj.MRJFileUtils.setDefaultFileCreator(new com.apple.mrj.MRJOSType("TNTO"));
		com.apple.mrj.MRJFileUtils.setDefaultFileType(new com.apple.mrj.MRJOSType("CCF "));

		}
		catch (Throwable tr)
		{
			debug(tr);
		}

		// install font if not present
		/*
		File lfd = new File(homeDir, "Library/Fonts");
		File ttf = new File(lfd, "pronto.ttf");
		InputStream is = getProntoFontStream();
		if (!lfd.isDirectory() || ttf.exists() || is == null)
		{
			return;
		}
		debug.log(1, "Installing Tonto font");
		try
		{
			OutputStream os = new FileOutputStream(ttf);
			byte b[] = new byte[1024];
			int read = 0;
			while ( (read = is.read(b)) >= 0)
			{
				os.write(b,0,read);
			}
			os.close();
			infoDialog("The Tonto Font was installed.\n You must restart Tonto.");
			exitAll();
		}
		catch (Exception ex)
		{
			debug(ex);
		}
		*/
	}

	private static void loadPreferences()
	{
		try
		{
			prefs = new Preferences();
			prefs.load(prefFile);
			prefUpdateRoot = prefs.getProperty(PREF_UPDATE_ROOT, "http://giantlaser.com/tonto/tonto");
			prefDefaultModel = ProntoModel.getModelByName(prefs.getProperty(PREF_DEFAULT_MODEL, "TS1000"));
			prefGrid = prefs.getRectangle(PREF_GRID_SIZE, prefGrid);
			prefGridSnap = prefs.getBoolean(PREF_GRID_SNAP, true);
			prefGridShow = prefs.getBoolean(PREF_GRID_SHOW, false);
			prefGridMinorTicks = prefs.getInteger(PREF_GRID_MINOR_TICKS, 4);
			prefGridMinorColor = new CCFColor(prefs.getInteger(PREF_GRID_MINOR_COLOR, 222));
			prefGridMajorColor = new CCFColor(prefs.getInteger(PREF_GRID_MAJOR_COLOR, 180));
			Debug.setLevels(prefs.getProperty(PREF_DEBUG_LEVELS,"comm=2,xmodem=2,ccf=1,tonto=2"));
			prefCommPort = prefs.getProperty(PREF_DEFAULT_PORT);
			prefScanOtherPorts = prefs.getBoolean(PREF_SCAN_OTHER_PORTS, true);
			prefScanThreaded = prefs.getBoolean(PREF_SCAN_THREADED, !(Util.onMacintosh() || Util.onWindows98()));
			prefScanModems = prefs.getBoolean(PREF_SCAN_MODEMS, false);
			String comm = prefs.getProperty(PREF_COMM_TIMEOUTS);
			if (comm != null)
			{
				StringTokenizer st = new StringTokenizer(comm,",");
				if (st.countTokens() >= 3)
				{
					Comm.setCommandDelay(Integer.parseInt(st.nextToken()));
					Comm.setAttentionDelay(Integer.parseInt(st.nextToken()));
					Comm.setLoadDelay(Integer.parseInt(st.nextToken()));
				}
			}
			String files = prefs.getProperty(PREF_RECENT_FILES);
			if (files != null)
			{
				StringTokenizer st = new StringTokenizer(files, File.pathSeparator);
				while (st.hasMoreTokens())
				{
					addRecentFile(st.nextToken());
				}
			}
			String fscale = prefs.getProperty(PREF_FONT_SCALING);
			if (fscale != null)
			{
				fontTable = cloneFontTable();
				StringTokenizer st = new StringTokenizer(fscale, ",");
				for (int i=0; i<18 && st.hasMoreTokens(); i++)
				{
					fontTable[i/3][i%3] = Integer.parseInt(st.nextToken());
				}
			}
			prefTreeFontSize = prefs.getInteger(PREF_FONT_SIZE, 10);
			prefUseProntoFont = prefs.getBoolean(PREF_FONT_INTREE, Util.isJDK14() || !Util.isSunJDK());
			prefLogToConsole = prefs.getBoolean(PREF_LOG_TO_CONSOLE, false);
			prefUseAWTFileDialogs = prefs.getBoolean(PREF_USE_AWT_DIALOG, true);
			prefObeyRemoteCap = prefs.getBoolean(PREF_OBEY_REMOTE_CAP, false);
			prefEmitIR = prefs.getBoolean(PREF_TEST_EMIT_IR, false);
			prefShowDeviceProps = prefs.getBoolean(PREF_SHOW_DEV_PROPS, true);
			prefShowWebSafe = prefs.getBoolean(PREF_COLOR_WEBSAFE, true);
			String pColorMap = prefs.getProperty(PREF_COLOR_MAP);
			prefColorMap = CCFColor.defaultMap;
			if (pColorMap != null)
			{
				StringTokenizer st = new StringTokenizer(pColorMap,",");
				if (st.countTokens() >= 4)
				{
					prefColorMap = new CCFColor[]
					{
						new CCFColor(Integer.parseInt(st.nextToken())),
						new CCFColor(Integer.parseInt(st.nextToken())),
						new CCFColor(Integer.parseInt(st.nextToken())),
						new CCFColor(Integer.parseInt(st.nextToken())),
					};
				}
			}
			prefWorkingDir = new File(prefs.getProperty(PREF_WORKING_DIR, prefWorkingDir.toString()));
			if (!prefWorkingDir.exists())
			{
				prefWorkingDir = prefDir;
			}
			prefImageDir = new File(prefs.getProperty(PREF_IMAGE_DIR, prefImageDir.toString()));
			if (!prefImageDir.exists())
			{
				prefImageDir = prefDir;
			}
			prefEditor = prefs.getProperty(PREF_EDITOR);
			//prefEmulator = prefs.getProperty(PREF_EMULATOR);
			prefImageEditor = prefs.getProperty(PREF_IMAGE_EDITOR);
			String aload = prefs.getProperty(PREF_AUTOLOAD_FILES, "");
			StringTokenizer st = new StringTokenizer(aload, ",");
			while (st.hasMoreTokens())
			{
				autoLoad.add(st.nextToken());
			}
			prefHSplitPos = prefs.getInteger(PREF_SPLIT_HORIZONTAL, 180);
			prefUUID = prefs.getProperty(PREF_UUID);
			if (prefUUID == null)
			{
				prefUUID = genUUID();
				prefs.put(PREF_UUID, prefUUID);
			}
			prefNetworkUpdate = prefs.getBoolean(PREF_NETWORK_UPDATES, false);
			CCFColor.setGrayTint(prefs.getInteger(PREF_GRAY_TINT, CCFColor.getGrayTint()));
			prefCenterDialogs = prefs.getBoolean(PREF_CENTER_DIALOGS, true);
			prefSelectionLast = prefs.getBoolean(PREF_SELECTION_LAST, false);
		}
		catch (Exception ex)
		{
			debug(ex);
		}
	}

	private static void savePreferences()
	{
		try
		{
			StringBuffer sb = new StringBuffer();
			for (int i=recentFiles.size(); i > 0; i--)
			{
				sb.append(recentFiles.get(i-1));
				sb.append(File.pathSeparator);
			}
			String recent = sb.toString();
			sb.setLength(0);
			for (int i=0; i<fontTable.length; i++)
			{
				sb.append(fontTable[i][0]+","+fontTable[i][1]+","+fontTable[i][2]+",");
			}
			String fontscale = sb.toString();
			sb.setLength(0);
			for (int i=0; i<autoLoad.size(); i++)
			{
				sb.append(autoLoad.get(i)+(i<autoLoad.size()-1 ? "," : ""));
			}
			String autoload = sb.toString();

			prefs.setProperty(PREF_UPDATE_ROOT, prefUpdateRoot);
			prefs.setProperty(PREF_DEFAULT_MODEL, prefDefaultModel.getName());
			prefs.setProperty(PREF_GRID_SIZE, prefGrid);
			prefs.setProperty(PREF_GRID_SNAP, prefGridSnap);
			prefs.setProperty(PREF_GRID_SHOW, prefGridShow);
			prefs.setProperty(PREF_GRID_MINOR_TICKS, prefGridMinorTicks);
			prefs.setProperty(PREF_GRID_MINOR_COLOR, prefGridMinorColor.getColorIndex());
			prefs.setProperty(PREF_GRID_MAJOR_COLOR, prefGridMajorColor.getColorIndex());
			prefs.setProperty(PREF_DEBUG_LEVELS, Debug.getLevels());
			prefs.setProperty(PREF_DEFAULT_PORT, prefCommPort != null ? prefCommPort : "");
			prefs.setProperty(PREF_SCAN_OTHER_PORTS, prefScanOtherPorts);
			prefs.setProperty(PREF_SCAN_THREADED, prefScanThreaded);
			prefs.setProperty(PREF_SCAN_MODEMS, prefScanModems);
			prefs.setProperty(PREF_COMM_TIMEOUTS, Comm.getCommandDelay()+","+Comm.getAttentionDelay()+","+Comm.getLoadDelay());
			prefs.setProperty(PREF_WINDOW_BOUNDS, jframe.getBounds());
			prefs.setProperty(PREF_RECENT_FILES, recent);
			prefs.setProperty(PREF_FONT_SCALING, fontscale);
			prefs.setProperty(PREF_FONT_INTREE, prefUseProntoFont);
			prefs.setProperty(PREF_FONT_SIZE, prefTreeFontSize);
			prefs.setProperty(PREF_LOG_TO_CONSOLE, prefLogToConsole);
			prefs.setProperty(PREF_COLOR_WEBSAFE, prefShowWebSafe);
			prefs.setProperty(PREF_COLOR_MAP, prefColorMap[0].getColorIndex()+","+prefColorMap[1].getColorIndex()+","+prefColorMap[2].getColorIndex()+","+prefColorMap[3].getColorIndex());
			prefs.setProperty(PREF_USE_AWT_DIALOG, prefUseAWTFileDialogs);
			prefs.setProperty(PREF_WORKING_DIR, prefWorkingDir.toString());
			prefs.setProperty(PREF_IMAGE_DIR, prefImageDir.toString());
			prefs.setProperty(PREF_OBEY_REMOTE_CAP, prefObeyRemoteCap);
			prefs.setProperty(PREF_TEST_EMIT_IR, prefEmitIR);
			prefs.setProperty(PREF_SHOW_DEV_PROPS, prefShowDeviceProps);
			prefs.setProperty(PREF_EDITOR, prefEditor);
			prefs.setProperty(PREF_IMAGE_EDITOR, prefImageEditor);
			prefs.setProperty(PREF_AUTOLOAD_FILES, autoload);
			prefs.setProperty(PREF_SPLIT_HORIZONTAL, state().split.getDividerLocation());
			prefs.setProperty(PREF_NETWORK_UPDATES, prefNetworkUpdate);
			prefs.setProperty(PREF_GRAY_TINT, CCFColor.getGrayTint());
			prefs.setProperty(PREF_CENTER_DIALOGS, prefCenterDialogs);
			prefs.setProperty(PREF_SELECTION_LAST, prefSelectionLast);
			prefs.save(prefFile);
		}
		catch (Exception ex)
		{
			debug(ex);
		}
	}

	private static void setLogging(boolean toConsole)
	{
		try
		{
			logFile = new File(logDir, "current.log");
			if (logFile.exists())
			{
				new File(logDir, "current.log").renameTo(new File(logDir, Util.time()+".log"));
			}
			Debug.logToFile(logFile.toString());
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			Debug.logToStream(System.out);
		}
		if (toConsole)
		{
			if (console == null)
			{
				console = new DebugListener() {
				public void debugAction(String msg) {
					System.out.println(msg);
				} };
			}
			Debug.addListener(console);
		}
		else
		{
			if (console != null)
			{
				Debug.removeListener(console);
			}
		}
	}

	private static void loadGalleryCCF()
	{
		debug.log(2, "loading gray gallery");
		try
		{
			// gray gallery
			CCF defaults = new CCF();
			byte defdata[] = ByteOutputBuffer.readFully(Tonto.class.getResourceAsStream("/ccf/gallery-gray.ccf"));
			defaults.decode(defdata);
			defaultGrayButton = defaults.getDeviceByName("buttons").getPanelByName("default").getButtonByID("default");
			// color gallery
			defaults = new CCF();
			defdata = ByteOutputBuffer.readFully(Tonto.class.getResourceAsStream("/ccf/gallery-color.ccf"));
			defaults.decode(defdata);
			defaultColorButton = defaults.getDeviceByName("buttons").getPanelByName("default").getButtonByID("default");
		}
		catch (Exception ex)
		{
			errorDialog(ex);
		}
	}

	private static void clearLogs()
	{
		long tm = Util.time();
		String f[] = logDir.list();
		for (int i=0; i<f.length; i++)
		{
			File ff = new File(logDir, f[i]);
			long lm = ff.lastModified();
			// delete older than 7 days
			if (tm - lm > 604800000)
			{
				debug.log(0, "deleting old log "+ff);
				ff.delete();
			}
		}
	}

	// -------------------------------------------------------------------------------------
	// NETWORK & UPDATING
	// -------------------------------------------------------------------------------------
	private static void startCommandListener()
	{
		if (Util.onMacintosh())
		{
			return;
		}
		new Thread()
		{
			public void run()
			{
				try
				{
					ServerSocket ss = new ServerSocket(9911, 5, InetAddress.getLocalHost());
					while (true)
					{
						Socket s = ss.accept();
						String cmd = new String(Util.readFully(s.getInputStream()));
						StringTokenizer st = new StringTokenizer(cmd);
						if (st.countTokens() >= 2)
						{
							if (st.nextToken().equals("load"))
							{
								while (st.hasMoreTokens())
								{
									load(URLDecoder.decode(st.nextToken()), true);
								}
							}
						}
						s.close();
					}
				}
				catch (Exception ex)
				{
					debug(ex);
				}
			}
		}.start();
	}

	private static void setupNetworkUpdates()
	{
		jarDir = getJarDir();
		if (!prefNetworkUpdate && prefs.getProperty(ASK_NETWORK_UPDATE) == null)
		{
			if (confirmDialog(
				"Enable Network Updates?",
				"Tonto has a feature to enable automatic updates\n"+
				"over the network when a new version is detected.\n"+
				"Would you like to enable this feature?"))
			{
				prefNetworkUpdate = true;
				sessionUpdate = true;
			}
			prefs.setProperty(ASK_NETWORK_UPDATE, "true");
		}
		if (sessionUpdate && prefNetworkUpdate && jarDir != null)
		{
			if (updateThread != null)
			{
				return;
			}
			updateThread = new Thread()
			{
				public void run()
				{
					debug.log(0, "Network updates started");
					Util.safeSleep(10*1000);
					while (prefNetworkUpdate && sessionUpdate)
					{
						checkForUpdates(false);
						Util.safeSleep(1000*60*60);
					}
				}
			};
			updateThread.setDaemon(true);
			updateThread.start();
		}
		else
		{
			prefNetworkUpdate = false;
		}
	}

	private static void checkForUpdates(boolean verbose)
	{
		synchronized (Tonto.class)
		{
			if (isUpdating)
			{
				return;
			}

			debug.log(3, "Looking for Tonto updates");
			try
			{
				URL u = new URL(prefUpdateRoot+"/VERSION?"+prefUUID);
				InputStream is = u.openStream();
				String cstring = new String(Util.readFully(is)).trim();
				double current = Double.parseDouble(cstring);
				double myver = Double.parseDouble(version());
				debug.log(3, "Current Tonto version: "+current);
				if (myver < current)
				{
					if (confirmDialog(
						"Upgrade to v"+cstring+"?",
						"A newer version of Tonto is available\n"+
						"Would you like to update now?\n"+
						"This will be done in the background\n"))
					{
						isUpdating = true;
						networkUpdate(cstring);
					}
					else
					{
						sessionUpdate = false;
					}
				}
				else
				if (verbose)
				{
					infoDialog("Tonto is up to date");
				}
			}
			catch (Exception ex)
			{
				debug(ex);
			}
		}
	}

	private static void networkUpdate(String current)
	{
		final String ver = current;
		new Thread() { public void run() {

		try
		{
			// check jar length
			URL u = new URL(prefUpdateRoot+"/bin/tonto.jar?"+prefUUID);
			URLConnection c = u.openConnection();
			int len = c.getContentLength();
			debug.log(0, "Network update "+version()+" to "+ver+", size="+len);
			// read jar stream to tmp file
			InputStream in = c.getInputStream();
			File update = new File(jarDir, "download.tmp");
			FileOutputStream out = new FileOutputStream(update);
			Util.readFully(in, out);
			out.close();
			// determine name of new tonto-*.jar
			for (int count = 1; ;count++)
			{
				File newjar = new File(jarDir, "tonto-"+ver+"-"+count+".jar");
				if (newjar.exists())
				{
					continue;
				}
				update.renameTo(newjar);
				debug.log(0, "Network update saved '"+newjar+"'");
				break;
			}
			// offer to reboot
			in.close();
			infoDialog("Tonto update complete. Restart Tonto\nto use the new version.");
		}
		catch (Exception ex)
		{
			debug(ex);
			errorDialog("Network update failed");
		}
		finally
		{
			synchronized (Tonto.class)
			{
				isUpdating = false;
			}
		}

		} }.start();
	}

	private static boolean checkUpdating()
	{
		synchronized (Tonto.class)
		{
			if (isUpdating)
			{
				return confirmDialog("Updating", "Tonto is updating. Cancel the update and exit?");
			}
			return true;
		}
	}

	// -------------------------------------------------------------------------------------
	// G L O B A L   O P E R A T I O N S   (ALL OPEN FILES)
	// -------------------------------------------------------------------------------------
	static class TontoEnum implements Enumeration
	{
		Tonto next;
		Enumeration tenum;

		TontoEnum()
		{
			tenum = tontos.elements();
			getNext();
		}

		public boolean hasMoreElements()
		{
			return next != null;
		}

		public Object nextElement()
		{
			Tonto ret = next;
			getNext();
			return ret;
		}

		void getNext()
		{
			while (tenum.hasMoreElements())
			{
				Object o = tenum.nextElement();
				if (o instanceof Tonto)
				{
					next = (Tonto)o;
					return;
				}
			}
			next = null;
		}
	}

	private static Enumeration getAllTontos()
	{
		return new TontoEnum();
	}

	private static boolean isFileOpen(String file)
	{
		for (Enumeration e=getAllTontos(); e.hasMoreElements(); )
		{
			String fn = ((Tonto)e.nextElement()).fileName;
			if (fn != null && file != null && fn.equals(file))
			{
				return true;
			}
		}
		return false;
	}

	private static void clearAllGrayIconCaches()
	{
		for (Enumeration e=getAllTontos(); e.hasMoreElements(); )
		{
			Tonto t = (Tonto)e.nextElement();
			t.icons.clearIconCaches();
			t.refreshAllPanels();
		}
	}

	private static void refreshAllTreeModels()
	{
		for (Enumeration e=getAllTontos(); e.hasMoreElements(); )
		{
			((Tonto)e.nextElement()).refreshTreeModel();
		}
	}

	private static void refreshAllMenus()
	{
		for (Enumeration e=getAllTontos(); e.hasMoreElements(); )
		{
			((Tonto)e.nextElement()).updateMenuState();
		}
	}

	private static void exitAll()
	{
		if (!checkUpdating())
		{
			return;
		}
		while (windows.getTabCount() > 0)
		{
			Component c = windows.getComponentAt(windows.getTabCount()-1);
			Tonto t = (Tonto)tontos.get(c);
			if (!t.close())
			{
				return;
			}
		}
		savePreferences();
		jframe.dispose();
		System.exit(exitCode);
	}

	// -------------------------------------------------------------------------------------
	// P U B L I C   S T A T I C   M E T H O D S
	// -------------------------------------------------------------------------------------
	public static String version()
	{
		if (version != null)
		{
			return version;
		}
		InputStream is = Tonto.class.getResourceAsStream("/version");
		if (is == null)
		{
			return null;
		}
		try
		{
			byte b[] = new byte[20];
			int len = is.read(b);
			version = new String(b, 0, len);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return version;
	}

	public static synchronized Component getTitleCanvas()
	{
		if (csplash == null)
		{
			csplash = new Canvas()
			{
				public void paint(Graphics g)
				{
					g.setColor(Color.lightGray);
					g.fillRect(0,0,244,204);
					g.drawImage(splash,2,2,null);
				}
				public Dimension getMinimumSize()
				{
					return new Dimension(244,204);
				}
				public Dimension getPreferredSize()
				{
					return getMinimumSize();
				}
			};
		}
		return csplash;
	}

	public static int getExitCode()
	{
		return exitCode;
	}

	public static boolean canScanThreaded()
	{
		return prefScanThreaded;
	}

	public static void debug(Throwable err)
	{
		if (err != null)
		{
			ByteOutputBuffer bob = new ByteOutputBuffer();
			PrintWriter writer = new PrintWriter(bob);
			err.printStackTrace(writer);
			writer.flush();
			debug.log(0, new String(bob.toByteArray()));
		}
	}

	// -------------------------------------------------------------------------------------
	// P R I V A T E   S T A T I C   M E T H O D S
	// -------------------------------------------------------------------------------------
	private void buttonLink()
	{
		debug.log(0, "button linking : "+clipboard+" to "+treeSelection);
		if (treeSelection instanceof ButtonBox && clipboard instanceof ButtonBox)
		{
			CCFButton bt = ((ButtonBox)treeSelection).getButton();
			CCFButton bs = ((ButtonBox)clipboard).getButton();
			bt.appendAction(new ActionAliasButton(bs));
		}
	}

	private void buttonLearnIR()
	{
		if (treeSelection instanceof ButtonBox)
		{
			CCFButton b = ((ButtonBox)treeSelection).getButton();
			Pronto p = learnIR();
			if (p != null)
			{
				b.appendAction(
					new ActionIRCode(new CCFIRCode(Tonto.ccf().header(), "Learned", p.encode(Tonto.irVersion())))
				);
			}
		}
	}

	private static void addRecentMenu(String name)
	{
		MenuItem open = new MenuItem(name);
		open.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				state().load(ae.getActionCommand(), true);
			}
		});
		recentMenu.insert(open, 0);
	}

	private static void addRecentFile(String name)
	{
		if (name == null || name.length() == 0 || !(new File(name).exists()) || name.toLowerCase().endsWith(".old"))
		{
			return;
		}
		int idx = recentFiles.indexOf(name);
		if (idx >= 0)
		{
			recentFiles.removeElementAt(idx);
			recentMenu.remove(idx);
		}
		while (recentFiles.size() > 10)
		{
			recentFiles.removeElementAt(10);
			recentMenu.remove(10);
		}
		recentFiles.insertElementAt(name, 0);
		addRecentMenu(name);
	}

	private static String genUUID()
	{
		long time = Util.time();
		long seed = new Random().nextLong();
		File dir = jarDir;
		while (dir != null)
		{
			debug.log(0, "using dir '"+dir+"' for uuid");
			File f[] = dir.listFiles();
			for (int i=0; f != null && i < f.length; i++)
			{
				seed *= (f[i].lastModified() + 13);
			}
			dir = dir.getParentFile();
		}

		return Long.toHexString(time)+"-"+Long.toHexString(seed);
	}

	private static File getJar()
	{
		try
		{
			return new File(Util.unURL(Tonto.class.getProtectionDomain().getCodeSource().getLocation().getFile()));
		}
		catch (Exception ex)
		{
			debug(ex);
			errorDialog("Unable to locate running jar");
			return null;
		}
	}

	private static File getJarDir()
	{
		try
		{
			return getJar().getParentFile();
		}
		catch (Exception ex)
		{
			debug(ex);
			errorDialog("Unable to find install dir for dynamic updates");
			return null;
		}
	}

	// -------------------------------------------------------------------------------------
	// P U B L I C   I N S T A N C E   M E T H O D S
	// -------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------------------
	// P R I V A T E   I N T E R N A L   M E T H O D S
	// -------------------------------------------------------------------------------------
	private void editSystemProperties()
	{
		if (custom())
		{
			xSystemProps.updateSystem();
		}
		else
		{
			systemProps.updateSystem();
		}
	}

	private Rectangle visibleBounds(Component c, Rectangle r)
	{
		Dimension size = c != null ? c.getSize() : panelSize;
		return new Rectangle(
			Util.bound(-r.width+1, size.width-1, r.x),
			Util.bound(-r.height+1, size.height-1, r.y),
			r.width, r.height
		);
	}

	private Rectangle getSelectionBounds()
	{
		Rectangle r = null;
		for (int i=0; i<multiSelect.size(); i++)
		{
			Component c = (Component)multiSelect.get(i);
			Rectangle b = c.getBounds();
			if (r == null)
			{
				r = b;
			}
			else
			{
				r.add(b);
			}
		}
		return r;
	}

	private void runEditor()
	{
		runWithCCF(prefEditor);
	}

	private String getEmulator()
	{
		if (ccf == null)
		{
			return null;
		}
		String s = prefs.getProperty(PREF_EMULATOR+"."+ccf.getConformsTo(prefDefaultModel).getName(), "").trim();
		return (s.length() > 0) ? s : null;
	}

	private void runEmulator()
	{
		String emu = getEmulator();
		if (emu != null)
		{
			runWithCCF(emu);
		}
	}

	private void runWithCCF(String app)
	{
		if (app == null || ccf == null)
		{
			return;
		}
		try
		{
			final File tmpFile = File.createTempFile("tonto",".ccf");
			ccf.save(tmpFile.toString());
			final Process p = Runtime.getRuntime().exec(app+" "+tmpFile.toString());

			new Thread() { public void run() {
				try {
					p.waitFor();
					sleep(2000);
					tmpFile.delete();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} }.start();
		}
		catch (Exception ex)
		{
			debug(ex);
		}
	}

	private void iconReplace(CCFIcon oi, CCFIcon ni)
	{
		if (oi == null)
		{
			return;
		}
		final CCFIcon oldIcon = oi;
		final CCFIcon newIcon = ni;
		new CCFWalker(ccf).walk(new IWalker()
		{
			public void onNode(CCFNode node)
			{
				if (node == null) { return; }

				boolean changed = false;

				try {

				Field f[] = node.getClass().getDeclaredFields();
				for (int i=0; i<f.length; i++)
				{
					if (f[i].getType() == CCFIcon.class && f[i].get(node) == oldIcon)
					{
						f[i].set(node, newIcon);
					}
				}

				}
				catch (Exception ex) { debug(ex); }

				if (changed) { setCCFChanged(); }
			}
		});
		setCCFChanged();
	}

	// -------------------------------------------------------------------------------------
	// U T I L I T I E S
	// -------------------------------------------------------------------------------------
	private CCFDevice getCurrentDevice()
	{
		return currentDevice;
	}

	private CCFPanel getCurrentPanel()
	{
		return currentPanel;
	}

	private Dimension max(Dimension d, int wmax, int hmax)
	{
		return new Dimension(
			wmax > 0 ? Math.max(d.width, wmax) : d.width,
			hmax > 0 ? Math.max(d.height, hmax) : d.height);
	}

	private boolean close()
	{
		if (checkChanges())
		{
			Component c = (Component)tontos.get(this);
			tontos.remove(this);
			tontos.remove(c);
			windows.remove(c);
			tabSelect();
			return true;
		}
		else
		{
			return false;
		}
	}

	private void exit()
	{
		exit(0);
	}

	private void exit(int val)
	{
		if (close())
		{
			exitCode = val;
			if (tontos.size() == 0 && checkUpdating())
			{
				savePreferences();
				jframe.dispose();
				System.exit(val);
			}
		}
	}

	// -------------------------------------------------------------------------------------
	// UNDO REDO
	// -------------------------------------------------------------------------------------
	class DoStart implements Doable
	{
		private int id;
		DoStart()
		{
			id = nextDoID++;
		}
		public void doIt() { }
		public void undoIt() { }
		public String toString() { return "DoStart:"+id; }
	}

	class DoEnd implements Doable
	{
		private int id;
		DoEnd(int id)
		{
			this.id = id;
		}
		public void doIt() { }
		public void undoIt() { }
		public String toString() { return "DoEnd:"+id; }
	}

	private int startMultiDo()
	{
		DoStart ds = new DoStart();
		pushDo(ds);
		return ds.id;
	}

	private void endMultiDo(int i)
	{
		pushDo(new DoEnd(i));
	}

	private void pushDo(Doable doit)
	{
		pushDo(doit, true);
	}

	private Doable pushDo(Doable doit, boolean isnew)
	{
		undoStack.push(doit);
		doit.doIt();
		if (isnew)
		{
			redoStack.setSize(0);
		}
		if (undoStack.size() > MAX_UNDO)
		{
			Doable d = (Doable)undoStack.remove(0);
			if (d instanceof DoStart)
			{
				int mid = ((DoStart)d).id;
				Doable nd;
				while (!((nd = (Doable)undoStack.remove(0)) instanceof DoEnd) || ((DoEnd)nd).id != mid)
				{
					// just dzoo it
				}
			}
		}
		setCCFChanged();
		updateMenuState();
		return doit;
	}

	private Doable popDo()
	{
		Doable d = null;
		if (!undoStack.empty())
		{
			d = (Doable)undoStack.pop();
			d.undoIt();
			redoStack.push(d);
			if (d instanceof DoEnd)
			{
				int mid = ((DoEnd)d).id;
				Doable nd;
				while (!((nd = popDo()) instanceof DoStart) || ((DoStart)nd).id != mid)
				{
					// just dzoo it
				}
			}
		}
		updateMenuState();
		objectStatus.updateAll();
		return d;
	}

	private void reDo()
	{
		if (!redoStack.empty())
		{
			if (pushDo((Doable)redoStack.pop(), false) instanceof DoStart)
			{
				while (!(pushDo((Doable)redoStack.pop(), false) instanceof DoEnd))
				{
					// just dzoo it
				}
			}
		}
		objectStatus.updateAll();
	}

	// -------------------------------------------------------------------------------------
	// COMMUNICATIONS
	// -------------------------------------------------------------------------------------
	private static void closeComm()
	{
		if (comm != null)
		{
			comm.close();
			comm = null;
		}
	}

	static Pronto learnIR()
	{
		if (getComm() == null)
		{
			showCommError();
			return null;
		}
		IRCapture irc = new IRCapture();
		if (irc.invoke())
		{
			debug.log(2,"signal accepted");
		}
		else
		{
			debug.log(2,"signal rejected");
			return null;
		}
		Pronto sig = irc.getIRCode();
		if (sig != null)
		{
			jframe.getToolkit().getSystemClipboard().setContents(new StringSelection(sig.encode()),null);
		}
		return sig;
	}
	
	private static void showCommError()
	{
		if (commMsg != null)
		{
			errorDialog(commMsg, commErr);
		}
	}

	private static void setCommError(String msg, Throwable err)
	{
		commMsg = msg;
		commErr = err;
	}

	private Task getCommTask(boolean detail)
	{
		final boolean d = detail;
		return new Task("Locate Remote") {
			public void invoke(ITaskStatus status) {
				try
				{
					status.taskStatus(0, "Scanning for Remote");
					if (comm == null)
					{
						if (!(prefCommPort == null || prefScanOtherPorts))
						{
							comm = Comm.detectPronto(prefCommPort);
						}
						else
						{
							comm = Comm.scanForPronto(status,prefCommPort);
						}
					}
					if (d && comm != null)
					{
						status.taskStatus(35, "Checking Pronto Status");
						comm.queryPronto();
						status.taskStatus(65, "Checking Pronto Capability");
						comm.getCCFPossible();
					}
					if (comm != null)
					{
						updateMenuState();
						prefCommPort = comm.getPortName();
						status.taskStatus(100, "Complete");
					}
					else
					{
						setCommError("Remote was not located on any ports", null);
						status.taskError(new Exception("Remote was not located on any ports"));
					}
				}
				catch (Throwable ex)
				{
					status.taskError(ex);
					handleCommError(ex);
				}
			}
		};
	}

	private static void handleCommError(Throwable th)
	{
		setCommError("The Pronto is not responding", th);
		th.printStackTrace();
	}

	private static Comm getCommLink()
	{
		if (comm == null)
		{
			throw new RuntimeException("comm uninitialized");
		}
		else
		{
			return comm;
		}
	}

	private static Comm getComm()
	{
		return getComm(false);
	}

	private static Comm getComm(boolean exam)
	{
		try
		{
			if (comm == null)
			{
				final boolean detail = exam;
				final TransferDialog ps = new TransferDialog("Scanning for Remote");
				new DialogThread(ps) {
					public void body() throws Exception { 
						if (!(prefCommPort == null || prefScanOtherPorts))
						{
							comm = Comm.detectPronto(prefCommPort);
						}
						else
						{
							comm = Comm.scanForPronto(ps,prefCommPort);
						}
						if (comm == null) {
							return;
						}
						if (detail) {
							ps.taskStatus(35, "Checking Pronto Status");
							comm.queryPronto();
							ps.taskStatus(65, "Checking Pronto Capability");
							comm.getCCFPossible();
						}
						ps.taskStatus(100, "Complete");
					}
				}.checkError();
			}
			if (comm != null)
			{
				state().updateMenuState();
				prefCommPort = comm.getPortName();
			}
			else
			{
				setCommError("Remote was not located on any ports", null);
			}
		}
		catch (Throwable th)
		{
			handleCommError(th);
		}
		return comm;
	}

	// -------------------------------------------------------------------------------------
	// GRAPHICS AND FONTS
	// -------------------------------------------------------------------------------------
	private ImageFont getIFont(int size)
	{
		if (size <= 0)
		{
			return null;
		}
		if (size >= 8)
		{
			size = (size-8)/2;
		}
		return fonts[size];
	}

	private ImageFontLabel getFontLabel(String text, int align, int size)
	{
		return new ImageFontLabel(getIFont(size), text, align);
	}

	private static void loadFonts()
	{
		Class c = Tonto.class;
		try
		{
			// load button bitmap fonts
			fonts = new ImageFont[] {
				new ImageFont(c.getResourceAsStream("/font/tonto-08.iff")),
				new ImageFont(c.getResourceAsStream("/font/tonto-10.iff")),
				new ImageFont(c.getResourceAsStream("/font/tonto-12.iff")),
				new ImageFont(c.getResourceAsStream("/font/tonto-14.iff")),
				new ImageFont(c.getResourceAsStream("/font/tonto-16.iff")),
				new ImageFont(c.getResourceAsStream("/font/tonto-18.iff")),
			};

			//fontPronto = new Font("Pronto2", Font.PLAIN, 12);
			fontPronto = Font.createFont(Font.TRUETYPE_FONT, getTontoFontStream());
			//System.out.println("fontPronto = "+fontPronto+" -> "+fontPronto.getFamily());
			if (!fontPronto.getFamily().equalsIgnoreCase("Pronto2"))
			{
				errorDialog("Tonto font is not accessible to the JVM.");
			}
			/*
			// only IBM JVM seems to honor the system ttf fonts
			if (Util.isIBMJDK() && !Util.isJDK14())
			{
				fontPronto = new Font("Tonto", Font.PLAIN, 12);
			}
			// fall back to loading the tonto font map
			else
			{
				fontPronto = Font.createFont(Font.TRUETYPE_FONT, getTontoFontStream());
			}
			// if all else failes, try loading the pronto font map
			if (!fontPronto.getFamily().equalsIgnoreCase("tonto"))
			{
				fontPronto = Font.createFont(Font.TRUETYPE_FONT, getProntoFontStream());
				if (!fontPronto.getFamily().equalsIgnoreCase("pronto"))
				{
					fontPronto = new Font("Sansserif", Font.PLAIN, 12);
					errorDialog("Tonto font is not accessible to the JVM.");
				}
			}
			*/
		}
		catch (Throwable ex)
		{
			debug(ex);
		}
		debug.log(1, "font = "+fontPronto);

	}

	private static InputStream getTontoFontStream()
	{
		//return Tonto.class.getResourceAsStream("/font/map.tonto0");
		return Tonto.class.getResourceAsStream("/font/map.pronto2");
	}

	private static InputStream getProntoFontStream()
	{
		return Tonto.class.getResourceAsStream("/font/map.pronto");
	}

	private static void setFontTable()
	{
		if (Util.onMacintosh())
		{
			fontTable = fontMAC;
			debug.log(2,"font.table = Mac");
		}
		else
		if (Util.isIBMJDK())
		{
			fontTable = fontIBM;
			debug.log(2,"font.table = IBM");
		}
		else
		if (Util.isSunJDK())
		{
			fontTable = fontSUN;
			debug.log(2,"font.table = Sun");
		}
		else
		{
			fontTable = fontUnknown;
			debug.log(2,"font.table = Unknown");
		}

	}

	private static int[][] cloneFontTable()
	{
		int nt[][] = new int[fontTable.length][3];
		for (int i=0; i<fontTable.length; i++)
		{
			nt[i][0] = fontTable[i][0];
			nt[i][1] = fontTable[i][1];
			nt[i][2] = fontTable[i][2];
		}
		return nt;
	}

	/*
	private void removeDependencies(CCFNode n)
	{
		if (ccf == null)
		{
			return;
		}
		final CCFNode node = n;
		new CCFWalker(ccf).walk(new IWalker() {
			public void onNode(CCFNode on) {
				if (on instanceof CCFActionList) {
					if ( ((CCFActionList)on).deleteMatching(node) ) {
						debug.log(1,"rmdep: remove matching "+Util.nickname(on.getParent()));
					}
				}
			}
		});
	}
	*/

	private void printKS(JComponent c)
	{
		debug.log(0,"printKS("+c+")");
		InputMap im = c.getInputMap(c.WHEN_FOCUSED);
		KeyStroke ks[] = im.allKeys();
		for (int i=0; ks != null && i<ks.length; i++)
		{
			debug.log(0, "1) ks["+i+"] = "+ks[i]);
		}
		im = c.getInputMap(c.WHEN_IN_FOCUSED_WINDOW);
		ks = im.allKeys();
		for (int i=0; ks != null && i<ks.length; i++)
		{
			debug.log(0, "2) ks["+i+"] = "+ks[i]);
		}
		im = c.getInputMap(c.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ks = im.allKeys();
		for (int i=0; ks != null && i<ks.length; i++)
		{
			debug.log(0, "3) ks["+i+"] = "+ks[i]);
		}
	}

	void addTabAdvance(JComponent c)
	{
		// tab key bindings
		InputMap im = c.getInputMap(c.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = c.getActionMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,0,false), "TAB");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,0,true), "TAB");
		am.put("TAB", new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				if (treeSelection instanceof ChildPanel) {
					CCFChild c = ((ChildPanel)treeSelection).getSource();
					setTreeSelection(getChildPanel(c.getParentPanel().getNext(c)));
				}
			}
		});
	}

	private static void bindAction(JComponent c, int keycode, int mods, Action action)
	{
		String nm = "bind-"+(nextAction++);
		c.getInputMap(c.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keycode, mods, true), nm);
		c.getActionMap().put(nm, action);
	}

	private void bindActionKeys(JComponent c)
	{
		// bind tab traversal keys
		bindAction(c, KeyEvent.VK_HOME, 0, new TabSelect(windows,0));
		bindAction(c, KeyEvent.VK_END, 0, new TabSelect(windows,-1));
		bindAction(c, KeyEvent.VK_PAGE_UP, 0, new TabPager(windows,-1));
		bindAction(c, KeyEvent.VK_PAGE_DOWN, 0, new TabPager(windows,1));

		// select first child
		bindAction(c, KeyEvent.VK_C, KeyEvent.ALT_MASK, new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				if (dragSelection instanceof PanelPanel)
				{
					CCFChild c[] = ((PanelPanel)dragSelection).getPanel().getChildren();
					if (c != null && c.length > 0)
					{
						Component o = getUIWrapper(c[0]);
						setDragSelection(o);
					}
				}
				else
				if (dragSelection instanceof FrameBox)
				{
					CCFChild c[] = ((FrameBox)dragSelection).getFrame().getChildren();
					if (c != null && c.length > 0)
					{
						Component o = getUIWrapper(c[0]);
						setDragSelection(o);
					}
				}
			}
		});
		// select parent
		bindAction(c, KeyEvent.VK_P, KeyEvent.ALT_MASK, new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				if (dragSelection != null)
				{
					setDragSelection(dragSelection.getParent());
				}
			}
		});

		// dump
		bindAction(c, KeyEvent.VK_I, 0,
			new AbstractAction() {
				public void actionPerformed(ActionEvent ae) {
					if (treeSelection instanceof CCFTreeDevice) {
						((CCFTreeDevice)treeSelection).getDevice().dump();
						((CCFTreeDevice)treeSelection).getDevice().traceParents();
					}
					else
					if (treeSelection instanceof CCFTreePanel) {
						((CCFTreePanel)treeSelection).getPanel().dump();
						((CCFTreePanel)treeSelection).getPanel().traceParents();
					}
					else
					if (treeSelection instanceof PanelPanel) {
						((PanelPanel)treeSelection).getPanel().dump();
						((PanelPanel)treeSelection).getPanel().traceParents();
					}
					else
					if (treeSelection instanceof ChildPanel) {
						((ChildPanel)treeSelection).getSource().dump();
						((ChildPanel)treeSelection).getSource().traceParents();
					}
				}
			});
		// hide selection
		bindAction(c, KeyEvent.VK_H, 0,
			new AbstractAction() {
				public void actionPerformed(ActionEvent ae) {
					eventViewHideSelection();
				}
			}
		);
		// transparency
		bindAction(c, KeyEvent.VK_X, 0,
			new AbstractAction() {
				public void actionPerformed(ActionEvent ae) {
					eventObjectTransparency();
				}
			}
		);
		// color panel
		bindAction(c, KeyEvent.VK_L, 0,
			new AbstractAction() {
				public void actionPerformed(ActionEvent ae) {
					if (treeSelection instanceof CCFTreeDevice) {
						CCFDevice d = ((CCFTreeDevice)treeSelection).getDevice();
						CCFPanel p = d.createPanel("colors");
						d.addPanel(p);
						for (int y=0; y<16; y++)
						{
							CCFFrame cf = p.createFrame(".");
							p.addFrame(cf);
							cf.setLocation(new Point(0, y*12));
							cf.setSize(new Dimension(14*16, 12));
							for (int x=0; x<16; x++)
							{
								CCFFrame f = cf.createFrame(".");
								cf.addFrame(f);
								f.setSize(new Dimension(14,12));
								f.setLocation(new Point(x*14,0));
								//f.setForeground(new CCFColor(y*16+x));
								f.setBackground(new CCFColor(x*16+y));
								f.setFont(CCFFont.SIZE_8);
							}
						}
						((CCFTreeDevice)treeSelection).refresh();
					}
				}
			});
		// character panels
		bindAction(c, KeyEvent.VK_M, 0,
			new AbstractAction() {
				public void actionPerformed(ActionEvent ae) {
					CCFDevice cd = device();
					if (cd == null)
					{
						return;
					}
					for (int i=0; i<3; i++)
					{
						CCFPanel p = cd.createPanel("symbols:"+i);
						for (int y=0; y<10; y++)
						{
							for (int x=0; x<10; x++)
							{
								int cv = i*100+y*10+x;
								if (cv < 256)
								{
									CCFButton cf = p.createButton(new String(new char[] { (char)cv }));
									CCFIconSet ic = cf.getIconSet();
									int vs[] = ic.getValidStates();
									for (int k=0; k<vs.length; k++)
									{
										ic.setForeground(vs[k], new CCFColor(0));
										ic.setBackground(vs[k], new CCFColor(245));
									}
									cf.setLocation(new Point(x*24, y*22));
									cf.setSize(new Dimension(23,21));
									cf.setFont(CCFFont.SIZE_12);
									cf.appendAction(new ActionDelay(100));
									p.addButton(cf);
								}
							}
						}
						cd.addPanel(p);
					}
					refreshTreeDevice(cd);
				}
			});

		// test new editor
		bindAction(c, KeyEvent.VK_F9, 0, 
			new AbstractAction() {
				public void actionPerformed(ActionEvent ae) { new RenderEditor(ccf()).show(); }});

		// F2 rename
		bindAction(c, KeyEvent.VK_F2, 0, 
			new AbstractAction() {
				public void actionPerformed(ActionEvent ae) { objectStatus.editName(); }});

		// F3 link
		bindAction(c, KeyEvent.VK_F3, 0, 
			new AbstractAction() {
				public void actionPerformed(ActionEvent ae) { buttonLink(); }});

		// I learn IR
		bindAction(c, KeyEvent.VK_L, 0, 
			new AbstractAction() {
				public void actionPerformed(ActionEvent ae) { buttonLearnIR(); }});

		// ESC deselect
		bindAction(c, KeyEvent.VK_ESCAPE, 0, 
			new AbstractAction() {
				public void actionPerformed(ActionEvent ae) { setDragSelection(null); }});

		// CTRL-T tiling
		bindAction(c, KeyEvent.VK_T, KeyEvent.CTRL_MASK,
			new AbstractAction() {
				public void actionPerformed(ActionEvent ae) { tilePanels(); }});

		// CTRL-SHIFT-W close all panels
		bindAction(c, KeyEvent.VK_W, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK,
			new AbstractAction() {
				public void actionPerformed(ActionEvent ae) { closeAllPanels(); }});
	}

	private void closeAllPanels()
	{
		for (Enumeration e = panels.elements(); e.hasMoreElements(); )
		{
			Object o = e.nextElement();
			if (o instanceof DeskPanel)
			{
				try { ((DeskPanel)o).dispose(); } catch (Exception ex) { }
			}
		}
		nextPosition = new Point(2,2);
	}

	private void tabMaster()
	{
		CCFPanel panel = panel();
		if (panel == null)
		{
			return;
		}
		new TabMaster(panel);
	}

	private void themeMaster()
	{
		new ThemeMaster();
	}

	private void tilePanels()
	{
		Dimension sz = null;
		Dimension d = work.getSize();
		int x = 0, y = 0, mx = 0, my = 0, off = 0;
		for (Enumeration e = panels.elements(); e.hasMoreElements(); )
		{
			Object o = e.nextElement();
			if (o instanceof DeskPanel)
			{
				DeskPanel dp = (DeskPanel)o;
				if (sz == null)
				{
					sz = dp.getSize();
					mx = d.width/sz.width;
					my = d.height/sz.height;
				}
				dp.setLocation(x*sz.width + off, y*sz.height + off);
				dp.moveToFront();
				if (++x >= mx)
				{
					x = 0;
					if (++y >= my)
					{
						y = 0;
						off += 25;
					}
				}
			}
		}
		if (sz != null)
		{
			nextPosition = new Point(x*sz.width+off,y*sz.height+off);
		}
	}

	private void setClipboard(Object o)
	{
		debug.log(2,"set clipboard "+Util.nickname(o));
		clipboard = o;
		updateMenuState();
	}

	private void setDragSelectionWithFocus(Component c)
	{
		setDragSelection(c, false);
		c.requestFocus();
	}

	private void setDragSelection(Component c)
	{
		setDragSelection(c, false);
	}

	private void setDragSelection(Component c, boolean multi)
	{
		if (c == dragSelection && !multi)
		{
			return;
		}
		hideSelection = false;
		debug.log(3,"set drag selection "+Util.nickname(c));
		tree.clearSelection();
		// multi-select
		if (multi && dragSelection instanceof Selectable && c instanceof Selectable)
		{
			if (c.getParent() != dragSelection.getParent())
			{
				return;
			}
			if (multiSelect.contains(c))
			{
				((Selectable)c).unselect();
				multiSelect.remove(c);
				c = multiSelect.size() > 0 ? (Component)multiSelect.get(0) : null;
			}
			else
			{
				((Selectable)c).select(false);
				dragSelection = c;
				multiSelect.add(c);
			}
		}
		else
		{
			if (c != null && multiSelect.size() > 1 && multiSelect.contains(c))
			{
				return;
			}
			// unselect old
			if (dragSelection != c && dragSelection instanceof Selectable)
			{
				((Selectable)dragSelection).unselect();
				for (int i=0; i<multiSelect.size(); i++)
				{
					((Selectable)multiSelect.get(i)).unselect();
				}
			}
			multiSelect.setSize(0);
			// select
			dragSelection = c;
			if (dragSelection instanceof Selectable)
			{
				multiSelect.add(c);
				((Selectable)c).select(true);
			}
			if (c != null)
			{
				setTreeSelection(c);
			}
			else
			{
				objectStatus.setObject(null);
			}
		}
		updateMenuState();
	}

	private void setTreeSelection(Object o)
	{
		if (o == treeSelection)
		{
			return;
		}
		debug.log(3,"set tree selection "+Util.nickname(o));
		treeSelection = o;
		updateMenuState();
		if (treeSelection instanceof CCFTreeDevice)
		{
			currentDevice = ((CCFTreeDevice)treeSelection).getDevice();
		}
		else
		if (treeSelection instanceof CCFTreePanel)
		{
			currentDevice = ((CCFTreePanel)treeSelection).getPanel().getParentDevice();
			currentPanel = ((CCFTreePanel)treeSelection).getPanel();
		}
		else
		if (treeSelection instanceof ChildPanel)
		{
			CCFChild src = ((ChildPanel)treeSelection).getSource();
			if (src != null)
			{
				currentDevice = src.getParentDevice();
				currentPanel = src.getParentPanel();
			}
		}
		else
		{
			currentDevice = null;
			currentPanel = null;
		}
		if (!(o instanceof Component))
		{
			setDragSelection(null);
		}
		else
		{
			/*
			((Component)o).requestFocus();
			if (o instanceof JComponent)
			{
				printKS((JComponent)o);
			}
			*/
		}
		objectStatus.setObject(o);
	}

	private void updateTreeSelection()
	{
		if (treeSelection instanceof TreeFolder)
		{
			((TreeFolder)treeSelection).fireRereadContents();
		}
	}

	private static boolean isRightClick(MouseEvent e)
	{
		int mod = e.getModifiers();
		/*
		debug.log(0,"click >> "+e);
		debug.log(0,"  alt     "+(mod & e.ALT_MASK));
		debug.log(0,"  b1      "+(mod & e.BUTTON1_MASK));
		debug.log(0,"  b2      "+(mod & e.BUTTON2_MASK));
		debug.log(0,"  b3      "+(mod & e.BUTTON3_MASK));
		debug.log(0,"  ctrl    "+(mod & e.CTRL_MASK));
		debug.log(0,"  meta    "+(mod & e.META_MASK));
		debug.log(0,"  shift   "+(mod & e.SHIFT_MASK));
		*/
		return ((mod & e.BUTTON3_MASK) != 0) || ((mod & (e.BUTTON2_MASK | e.ALT_MASK)) != 0);
	}

	private void rightClickMenu(Object rc, Point p)
	{
		if (rc instanceof Component)
		{
			Component c = (Component)rc;
			int x = p.x;
			int y = p.y;
			if (rc instanceof ChildPanel)
			{
				menuEnable(TOGGLE_GRID, true);
				menuEnable(TOGGLE_SNAP, true);
				menuToggle(TOGGLE_GRID, ((ChildPanel)rc).isGridShowing());
				menuToggle(TOGGLE_SNAP, ((ChildPanel)rc).isSnapped());
			}
			if (rc instanceof FrameBox)
			{
				rcFrame.show(c, x, y);
			}
			else
			if (rc instanceof PanelPanel)
			{
				rcPanel.show(c, x, y);
			}
			else
			if (rc instanceof ButtonBox)
			{
				rcButton.show(c, x, y);
			}
		}
		else
		{
			if (rc instanceof CCFTreePanel)
			{
				rcPanel.show(tree, p.x, p.y);
				//PanelPanel pp = getPanelPanel(((CCFTreePanel)rc).getPanel());
				boolean ps = isPanelShowing(((CCFTreePanel)rc).getPanel());
				menuEnable(ADD_BUTTON,  ps);//pp != null);
				menuEnable(ADD_FRAME,   ps);//pp != null);
				menuEnable(TOGGLE_GRID, ps);//pp != null);
				menuEnable(TOGGLE_SNAP, ps);//pp != null);
				//if (pp != null)
				if (ps)
				{
					PanelPanel pp = getPanelPanel(((CCFTreePanel)rc).getPanel());
					menuToggle(TOGGLE_GRID, pp.isGridShowing());
					menuToggle(TOGGLE_SNAP, pp.isSnapped());
				}
			}
			else
			if (rc instanceof CCFTreeDevice)
			{
				rcDevice.show(tree, p.x, p.y);
			}
			else
			if (rc instanceof CCFTreeDeviceFolder)
			{
				rcDGroup.show(tree, p.x, p.y);
			}
		}
	}

	private void setFileName(String fname)
	{
		fileName = fname;
	}

	private void updateTitle()
	{
		String pm = ccf != null ? ccf.getConformsTo(prefDefaultModel).getName() : "";
		String fn = (fileName != null ? new File(fileName).getName() : "<unnamed>")+(changed?" *":"");
		jframe.setTitle("Tonto "+version()+" ("+fn+") "+pm);
		windows.setTitleAt(windows.getSelectedIndex(), fn);
	}

	private void updateCCFInfo()
	{
		updateTitle();
		rcDeviceAliasKey.removeAll();
		rcDevicePasteKey.removeAll();
		rcDeviceCopyActions.removeAll();
		rcDevicePasteActions.removeAll();
		if (ccf == null)
		{
			return;
		}
		CCFHeader head = ccf.header();
		String keys[] =
			head.isNewMarantz() ? CCFAction.MarantzKeys :
			head.hasColor() ? CCFAction.ProntoProKeys : CCFAction.ProntoKeys;
		rcDevicePasteKey.add(newPopMenuItem("On Select", BASE_KEY_PASTE));
		rcDeviceCopyActions.add(newPopMenuItem("On Select", BASE_KEY_COPY_ACTIONS));
		rcDevicePasteActions.add(newPopMenuItem("On Select", BASE_KEY_PASTE_ACTIONS));
		for (int i=0; i<keys.length; i++)
		{
			rcDeviceAliasKey.add(newPopMenuItem(keys[i], BASE_KEY_ALIAS + i));
			rcDevicePasteKey.add(newPopMenuItem(keys[i], BASE_KEY_PASTE + i + 1));
			rcDeviceCopyActions.add(newPopMenuItem(keys[i], BASE_KEY_COPY_ACTIONS + i + 1));
			rcDevicePasteActions.add(newPopMenuItem(keys[i], BASE_KEY_PASTE_ACTIONS + i + 1));
		}
	}

	private String getProntoName(String def)
	{
		return nameDialog().getText(def);
	}

	private String getProntoName(String def, boolean ext)
	{
		return nameDialog().getText(def, ext);
	}

	private String getProntoName(String title, String def)
	{
		return nameDialog().getText(title, def);
	}

	private JLabel newLabel(String label)
	{
		return newLabel(label, true);
	}

	private JLabel newLabel(String label, boolean bevel)
	{
		JLabel l = new JLabel(label);
		l.setText(label);
		l.setFont(new Font("Helvetica", Font.PLAIN, 10));
		l.setForeground(Color.black);
		if (bevel)
		{
			l.setBorder(new BevelBorder(BevelBorder.LOWERED));
		}
		return l;
	}

	private static Font getPFont()
	{
		return getPFont(prefTreeFontSize);
	}

	private static Font getPFont(int size)
	{
		int idx = 0;
		switch (size)
		{
			case  8: idx = 0; break;
			case 10: idx = 1; break;
			case 12: idx = 2; break;
			case 14: idx = 3; break;
			case 16: idx = 4; break;
			case 18: idx = 5; break;
			default:
				return fontPronto.deriveFont(Font.PLAIN, (float)size);
		}
		Font f = fontPronto.deriveFont(Font.PLAIN, (float)fontTable[idx][0]+size);
		AffineTransform at = new AffineTransform();
		at.setToScale((float)fontTable[idx][1]/100f, (float)fontTable[idx][2]/100f);
		return f.deriveFont(at);
	}

	private static Image loadImage(String name)
	{
		URL url = Tonto.class.getClassLoader().getResource(name);
		if (url != null)
		{
			Image i = Toolkit.getDefaultToolkit().createImage(url);
			if (i == null && !name.startsWith("/"))
			{
				return loadImage("/"+name);
			}
			return i;
		}
		else
		{
			if (!name.startsWith("/"))
			{
				return loadImage("/"+name);
			}
			return null;
		}
	}

	private static ImageIcon loadIcon(String name)
	{
		URL url = Tonto.class.getResource(name);
		if (url != null)
		{
			return new ImageIcon(url);
		}
		else
		{
			return null;
		}
	}

	private static void menuEnable(int cmd, boolean enabled)
	{
		Integer i = new Integer(cmd);
		menuEnable(menus.get(i), enabled);
		menuEnable(popmenus.get(i), enabled);
		ToolAction ta = (ToolAction)tools.get(i);
		if (ta != null)
		{
			ta.setEnabled(enabled);
		}
	}

	private static void menuEnable(Object menu, boolean enabled)
	{
		if (menu instanceof MenuItem)
		{
			((MenuItem)menu).setEnabled(enabled);
		}
		else
		if (menu instanceof JMenuItem)
		{
			((JMenuItem)menu).setEnabled(enabled);
		}
		else
		if (menu instanceof Vector)
		{
			Vector v = (Vector)menu;
			for (Enumeration e=v.elements(); e.hasMoreElements(); )
			{
				menuEnable(e.nextElement(), enabled);
			}
		}
	}

	private static void menuToggle(int cmd, boolean on)
	{
		Vector list = (Vector)popmenus.get(new Integer(cmd));
		for (Enumeration e=list.elements(); e.hasMoreElements(); )
		{
			Object o = e.nextElement();
			if (o instanceof JCheckBoxMenuItem)
			{
				((JCheckBoxMenuItem)o).setSelected(on);
			}
			else
			if (o instanceof Vector)
			{
				Vector v = (Vector)o;
				for (Enumeration f=v.elements(); f.hasMoreElements(); )
				{
					((JCheckBoxMenuItem)f.nextElement()).setSelected(on);
				}
			}
		}
	}

	// -------------------------------------------------------------------------------------
	// LOAD, SAVE, UPLOAD, DOWNLOAD
	// -------------------------------------------------------------------------------------
	private boolean checkChanges()
	{
		if (changed)
		{
			String fn = (fileName != null ? "'"+new File(fileName).getName()+"' " : "");
			switch (JOptionPane.showConfirmDialog(StackedDialog.parent(),
				"Do you wish to save your changes?", "CCF "+fn+"was modified",
				JOptionPane.YES_NO_CANCEL_OPTION))
			{
				case JOptionPane.YES_OPTION: return save();
				case JOptionPane.NO_OPTION: return true; 
				case JOptionPane.CANCEL_OPTION: return false;
			}
		}
		return true;
	}

	private File getCCFLoad()
	{
		return getFile(true, "CCF Files (*.ccf, *.zcf)", new String[] { "ccf", "zcf" });
	}

	private boolean load()
	{
		if (!checkChanges())
		{
			return false;
		}
		File sel = getCCFLoad();
		if (sel != null)
		{
			load(sel.getAbsolutePath());
			clearDesktop();
			return true;
		}
		else
		{
			return false;
		}
	}

	public static void load(boolean nwind)
	{
		load(null, nwind);
	}

	public static void load(String file, boolean nwind)
	{
		if (nwind)// && !state().isnew)
		{
			if (isFileOpen(file) && !confirmDialog("Already open", "'"+file+"' is already open. Open Again?"))
			{
				return;
			}
			Tonto t = new Tonto();
			boolean exit = !(file != null ? t.load(file) : t.load());
			if (exit)
			{
				t.exit();
			}
		}
		else
		{
			if (file == null)
			{
				state().load();
			}
			else
			{
				state().load(file);
			}
		}
	}

	private boolean load(String file)
	{
		TransferDialog tf = new TransferDialog("Loading CCF", true);
		try
		{
			final String fl = file;
			final boolean zip = (file.toLowerCase().endsWith(".zcf"));
			new DialogThread(tf) {
				public void body() throws Exception { 
					if (zip)
					{
						newCCF = CCFPorter.importZip(fl,(ITaskStatus)dialog);
					}
					else
					{
						newCCF = new CCF();
						newCCF.setNotify((ITaskStatus)dialog);
						newCCF.load(fl);
					}
				}
			}.checkError();
			if (!checkMerge())
			{
				setFileName(file);
			}
			setTreeSelection(null);
			addRecentFile(file);
			updateCCFInfo();
			updateMenuState();
			isnew = false;
			return true;
		}
		catch (Throwable ex)
		{
			autoLoad.remove(file);
			errorDialog("'"+file+"' is not a valid CCF file", ex);
			return false;
		}
	}

	private boolean canRevert()
	{
		return fileName != null && new File(fileName+".old").exists();
	}

	private void revert()
	{
		if (canRevert() && confirmDialog("Revert", "Revert to last saved version of '"+new File(fileName).getName()+"'?"))
		{
			String savedName = fileName;
			ccf = null;
			load(fileName+".old");
			setFileName(savedName);
			updateTitle();
		}
	}

	private void save(String file)
	{
		if (file == null || ccf == null)
		{
			return;
		}
		String lf = file.toLowerCase();
		if (!(lf.endsWith(".ccf") || lf.endsWith(".zcf")))
		{
			file = file+".ccf";
		}
		if (new File(file).exists() && (fileName == null || !file.equals(fileName)))
		{
			if (!confirmDialog("Overwrite Existing", "File already exists. Overwrite?"))
			{
				return;
			}
		}
		TransferDialog tf = new TransferDialog("Saving CCF", false);
		try
		{
			final String fl = file;
			final boolean zip = file.toLowerCase().endsWith(".zcf");
			new DialogThread(tf) {
				public void body() throws Exception {
					if (zip)
					{
						CCFPorter.exportZip(ccf, fl, (ITaskStatus)dialog);
					}
					else
					{
						ccf.setNotify((ITaskStatus)dialog);
						ccf.save(fl);
					}
				}
			}.checkError();
			setCCFClean();
			addRecentFile(file);
			setFileName(file);
			updateCCFInfo();
			updateMenuState();
		}
		catch (Throwable ex)
		{
			errorDialog("error writing to '"+file+"'", ex);
			return;
		}
	}

	private boolean save()
	{
		if (fileName == null)
		{
			return saveAs();
		}
		else
		{
			save(fileName);
			return true;
		}
	}

	private File getCCFSave()
	{
		return getFile(false, "CCF Files (*.ccf, *.zcf)", new String[] { "ccf", "zcf" }, prefWorkingDir, fileName != null ? new File(fileName) : null);
	}

	private boolean saveAs()
	{
		File sel =  getCCFSave();
		if (sel != null)
		{
			save(sel.getAbsolutePath());
			return true;
		}
		else
		{
			return false;
		}
	}

	private boolean checkMerge()
	{
		if (newCCF == null)
		{
			debug.log(0, "No new CCF for merge");
			return false;
		}
		boolean merge = true;
		if (!isnew && ccf != null && new MergeDialog().getValue())
		{
			ccf.merge(newCCF);
			setCCFChanged();
		}
		else
		{
			ccf = newCCF;
			clearDesktop();
			setCCFClean();
			merge = false;
			isnew = false;
		}
		newCCF = null;
		updateMenuState();
		refreshTreeModel();
		icons.load(ccf);
		panelSize = ccf.getScreenSize(prefDefaultModel);
		return merge;
	}

	static File getFileOld(boolean load, String desc, String end[], File dir, File dfile)
	{
		final String e[] = end;
		FileDialog fd = new FileDialog(jframe, load ? "Open" : "Save", load ? FileDialog.LOAD : FileDialog.SAVE);
		fd.setDirectory(dir.toString());
		fd.setFilenameFilter(new MyFileFilter(end, desc));
		if (dfile != null)
		{
			fd.setFile(dfile.getName());
		}
		fd.show();
		String ndir = fd.getDirectory();
		String file = fd.getFile();
		if (ndir != null && file != null)
		{
			File sel = new File(ndir, file);
			if (dir == prefWorkingDir)
			{
				prefWorkingDir = sel.getParentFile();
			} else
			if (dir == prefImageDir)
			{
				prefImageDir = sel.getParentFile();
			}
			return sel;
		}
		else
		{
			return null;
		}
	}

	static File getFile(boolean load, String desc, String end[])
	{
		return getFile(load, desc, end, prefWorkingDir);
	}

	static File getFile(boolean load, String desc, String end[], File dir)
	{
		return getFile(load, desc, end, dir, null);
	}

	static class MyFileFilter extends FileFilter implements FilenameFilter
	{
		private String[] ok;
		private String desc;

		MyFileFilter(String ok[], String desc)
		{
			this.ok = ok;
			this.desc = desc;
		}

		public boolean accept(File f)
		{
			if (f.isDirectory() || ok == null || ok.length == 0)
			{
				return true;
			}
			String up = f.getName().toLowerCase();
			for (int i=0; i<ok.length; i++)
			{
				if (up.endsWith(ok[i].toLowerCase()))
				{
					return true;
				}
			}
			return false;
		}

		public boolean accept(File dir, String file)
		{
			if (ok == null || ok.length == 0)
			{
				return true;
			}
			for (int i=0; i<ok.length; i++)
			{
				if (file.toLowerCase().endsWith(ok[i].toLowerCase()))
				{
					return true;
				}
			}
			return false;
		}

		public String getDescription()
		{
			return desc;
		}
	}

	static File getFile(boolean load, String desc, String end[], File dir, File file)
	{
		if (prefUseAWTFileDialogs)
		{
			return getFileOld(load, desc, end, dir, file);
		}
		JFileChooser fc = new JFileChooser(dir);
		if (file != null)
		{
			fc.setSelectedFile(file);
		}
		fc.setFileFilter(new MyFileFilter(end,desc));
		int ret = (load ? fc.showOpenDialog(jframe) : fc.showSaveDialog(jframe));
		if (ret == fc.APPROVE_OPTION)
		{
			File sel = fc.getSelectedFile();
			if (dir == prefWorkingDir)
			{
				prefWorkingDir = sel.getParentFile();
			} else
			if (dir == prefImageDir)
			{
				prefImageDir = sel.getParentFile();
			}
			return sel;
		}
		return null;
	}

	static File[] getFiles(String desc, String end[], File dir)
	{
		if (prefUseAWTFileDialogs)
		{
			File f = getFileOld(true, desc, end, dir, null);
			if (f == null)
			{
				return null;
			}
			return new File[] { f };
		}
		JFileChooser fc = new JFileChooser(dir);
		fc.setMultiSelectionEnabled(true);
		fc.setFileFilter(new MyFileFilter(end,desc));
		int ret = fc.showOpenDialog(jframe);
		if (ret == fc.APPROVE_OPTION)
		{
			File sel[] = fc.getSelectedFiles();
			if (dir == prefWorkingDir)
			{
				prefWorkingDir = sel[0].getParentFile();
			} else
			if (dir == prefImageDir)
			{
				prefImageDir = sel[0].getParentFile();
			}
			return sel;
		}
		return null;
	}

	private void setCCFClean()
	{
		changed = false;
	}

	private void setCCFChanged()
	{
		boolean ut = !changed;
		isnew = false;
		changed = true;
		ccf.setModified();
		if (ut)
		{
			updateTitle();
		}
	}

	private void emailLogfile()
	{
		if (confirmDialog("Email Logfile", "Do you want to send the current log to stewart@neuron.com?"))
		{
			new Thread() { public void run() {

			try
			{
				String who = Util.sysprop("user.name");
				String email = prefs.getProperty("email", "");
				email = CCFNode.rpad(email, 30);
				StringValueDialog d = new StringValueDialog("Return Email Address", email, 0);
				if (!d.invoke())
				{
					return;
				}
				email = d.getValue().trim();
				if (email.length() == 0 || email.indexOf("@") < 0)
				{
					email = "stewart@neuron.com";
				}
				else
				{
					prefs.put("email", email);
				}
				debug.log(0, "Emailing logfile to stewart@neuron.com");

				// -----------------------------------------------------------
				/*
				URL u = new URL("http://localhost/giantlaser/tonto/email.php");
				URLConnection c = u.openConnection();
				c.setDoOutput(true);
				Writer out = new OutputStreamWriter(c.getOutputStream());
				out.write(
					"subject="+URLEncoder.encode("subject")+
					"&from="+URLEncoder.encode(email)+"&"+
					"text="+URLEncoder.encode("some text I thought up")+"\n");
				*/

				// -----------------------------------------------------------
				Socket s = new Socket("209.61.186.37", 25);
				InputStream in = s.getInputStream();
				Writer out = new OutputStreamWriter(s.getOutputStream());
				out.write("HELO neuron\n");
				out.write("MAIL FROM: tonto-debug@giantlaser.com\n");
				out.write("RCPT TO: stewart@neuron.com\n");
				out.write("DATA\n");
				out.write("To: stewart@neuron.com\n");
				out.write("Reply-To: "+email+"\n");
				out.write("Subject: tonto log from <"+who);
				out.write("@"+InetAddress.getLocalHost().getHostName()+">\n\n");
				out.write("=======( email: "+email+" )=======\n");
				// preferences
				writeHash(prefs, out, "Preferences");
				// system properties
				writeHash(System.getProperties(), out, "System Properties");
				// log file
				out.write("\n========( Log File )========\n");

				out.write("");

				if (logFile != null)
				{
					byte b[] = new BufferedFile(logFile.toString(), "r").toByteArray();
					out.write(new String(b));
				}
				else
				{
					out.write("file logging disabled");
				}
				out.write("\n.\n");

				out.flush();
				int count = 0;
				byte b[] = new byte[1024];
				while (in.read(b) >= 0 && count++ < 10)
					;

				out.close();
				in.close();
				s.close();

				infoDialog("Logfile sent to stewart@neuron.com");
			}
			catch (Exception ex)
			{
				debug.log(0, "Error emailing logfile: "+ex);
				errorDialog(ex);
			}

			} }.start();
		}
	}

	private void writeHash(Hashtable h, Writer w, String title)
		throws IOException
	{
		w.write("\n========( "+title+" )========\n");
		Vector v = new Vector(h.size());
		for (Enumeration e=h.keys(); e.hasMoreElements(); )
		{
			v.add(e.nextElement());
		}
		String s[] = new String[v.size()];
		v.copyInto(s);
		Arrays.sort(s);
		for (int i=0; i<s.length; i++)
		{
			w.write(s[i]+" = "+h.get(s[i])+"\n");
		}
	}

	private void upload(boolean nwind)
	{
		if (nwind)//!isnew && nwind)
		{
			Tonto t = new Tonto();
			if (!t.upload())
			{
				t.exit();
			}
		}
		else
		{
			upload();
		}
	}

	private boolean upload()
	{
		if (!checkChanges())
		{
			return false;
		}
		TaskList tl = new TaskList("Load CCF From Remote") {
			public void doCancel() {
				this.taskError(new Exception("Upload Cancelled"));
				newCCF = null;
				closeComm();
			}
		};
		tl.addTasks(new Task[] {
			getCommTask(false),
			new Task("Loading CCF",14) {
				public void invoke(ITaskStatus status) {
					try {
						Comm c = getCommLink();
						newCCF = new CCF();
						newCCF.setNotify(status);
						newCCF.loadFromPronto(c);
					} catch (Exception ex) {
						status.taskError(ex);
						newCCF = null;
					}
				}
			},
		});
		tl.invoke();
		checkMerge();
		updateCCFInfo();
		return !isnew;
	}

	private void rawUpload()
	{
		TaskList tl = new TaskList("Load CCF From Remote") {
			public void doCancel() {
				this.taskError(new Exception("Upload Cancelled"));
				newCCF = null;
				closeComm();
			}
		};
		tl.addTasks(new Task[] {
			getCommTask(false),
			new Task("Loading CCF",10) {
				public void invoke(ITaskStatus status) {
					try {
						Comm c = getCommLink();
						byte data[] = c.getCCF(status);
						File ccf_save = getCCFSave();
						if (ccf_save != null)
						{
							new FileOutputStream(ccf_save).write(data);
						}
					} catch (Exception ex) {
						status.taskError(ex);
					}
				}
			},
		});
		tl.invoke();
	}

	private void downloadFirmware(Firmware fw, boolean force)
	{
		if (prefCommPort == null)
		{
			errorDialog("Default port is not set in preferences");
			return;
		}
		final Firmware firm = fw;
		final boolean over = force;

		class FWTaskList extends TaskList
		{
			private Comm comm;
			FWTaskList()
			{
				super("Firmware Update");
			}
			public void doCancel() {
				this.taskError(new Exception("Firmware Flash Cancelled"));
				if (comm != null)
				{
					comm.close();
				}
			}
		}

		FWTaskList tl = new FWTaskList();

		tl.addTasks(new Task[] {
			new Task("Flashing",10) {
				public void invoke(ITaskStatus status) {
					try {
						closeComm();
						comm = new Comm(prefCommPort);
						comm.updateFirmware(firm, isnew ? null : ccf, over, status);
						infoDialog("The Remote will now reboot");
					} catch (Exception ex) {
						status.taskError(ex);
					}
				}
			},
		});
		tl.invoke();
	}

	private void download()
	{
		TaskList tl = new TaskList("Save CCF To Remote") {
			public void doCancel() {
				this.taskError(new Exception("Upload Cancelled"));
				closeComm();
			}
		};
		tl.addTasks(new Task[] {
			getCommTask(false),
			new Task("Check CCF") {
				public void invoke(ITaskStatus status) {
					try {
						Comm c = getCommLink();
						if (c.isCCFDirty() && !confirmDialog(
							"CCF Has Changed",
							"The CCF on the Remote has changed\n"+
							"without being saved. Do you wish to\n"+
							"proceed with the Download?"))
						{
							status.taskError(new Exception("aborted"));
						}
					} catch (Exception ex) {
						status.taskError(ex);
						closeComm();
					}
				}
			},
			new Task("Sending CCF",10) {
				public void invoke(ITaskStatus status) {
					try {
						Comm c = getCommLink();
						ccf.setUseRemoteCapability(prefObeyRemoteCap);
						ccf.setNotify(status);
						ccf.saveToPronto(c);
					} catch (CCFException ex) {
						//errorDialog("Download aborted\n"+ex.getMessage());
						status.taskError(ex);
					} catch (Exception ex) {
						ex.printStackTrace();
						status.taskError(ex);
					} finally {
						closeComm();
					}
				}
			},
		});
		if (tl.invoke())
		{
			infoDialog("The Remote will now reboot");
			updateMenuState();
		}
	}

	private void rawDownload()
	{
		TaskList tl = new TaskList("Load CCF From Remote") {
			public void doCancel() {
				this.taskError(new Exception("Upload Cancelled"));
				closeComm();
			}
		};
		tl.addTasks(new Task[] {
			getCommTask(false),
			new Task("Check CCF") {
				public void invoke(ITaskStatus status) {
					if (!confirmDialog(
						"CCF Has Changed",
						"The CCF on the Remote has changed\n"+
						"without being saved. Do you wish to\n"+
						"proceed with the Download?"))
					{
						status.taskError(new Exception("aborted"));
					}
				}
			},
			new Task("Sending CCF",11) {
				public void invoke(ITaskStatus status) {
					try {
						File ccf_load = getCCFLoad();
						if (ccf_load == null || !ccf_load.exists())
						{
							status.taskError(new Exception("aborted"));
							return;
						}
						Comm c = getCommLink();
						byte data[] = new BufferedFile(ccf_load.toString(), "r").toByteArray();
						c.setCCF(data, status);
					} catch (Exception ex) {
						status.taskError(ex);
					} finally {
						closeComm();
					}
				}
			},
		});
		if (tl.invoke())
		{
			infoDialog("The Remote will now reboot");
			updateMenuState();
		}
	}

	private void rebootPronto()
	{
		TaskList tl = new TaskList("Reboot Remote");
		tl.addTasks(new Task[] {
			getCommTask(false),
			new Task("Sending Reboot") {
				public void invoke(ITaskStatus status) {
					try {
						Comm c = getCommLink();
						status.taskStatus(50, "Send Command");
						c.rebootPronto();
						status.taskStatus(100, "Command Sent");
					} catch (Exception ex) {
						status.taskError(ex);
						closeComm();
					}
				}
			}
		});
		if (tl.invoke())
		{
			infoDialog("The Remote will now reboot");
			closeComm();
		}
	}

	private void firmware()
	{
		File f = getFile(true, "Firmware Updater", new String[] {"exe"});
		if (f != null)
		{
			try
			{
				new FirmwareDialog(f).show();
			} 
			catch (Exception ex)
			{
				errorDialog("Invalid Firmware Update", ex);
			}
		}
	}

	private void fileNew()
	{
		fileNew(prefDefaultModel.getModel());
	}

	private void fileNew(int type)
	{
		new Tonto().file_new(type);
	}

	private void file_new()
	{
		file_new(prefDefaultModel.getModel());
	}

	private void file_new(int type)
	{
		if (!checkChanges())
		{
			return;
		}
		ccf = new CCF(ProntoModel.getModel(type));
		panelSize = ccf.getScreenSize(prefDefaultModel);
		setFileName(null);
		nextPosition = new Point(2,2);
		clearDesktop();
		updateCCFInfo();
		updateMenuState();
		icons.load(ccf);
		setTreeSelection(null);
		setCCFClean();
		isnew = true;
	}

	private void file_close()
	{
		if (!checkChanges())
		{
			return;
		}
	}

	// -------------------------------------------------------------------------------------
	// MENUS
	// -------------------------------------------------------------------------------------
	public boolean enableMenuPaste()
	{
		return
			(treeSelection instanceof JTextComponent) ||
			(canPaste(treeSelection)) ||
			((treeSelection instanceof Parental) && canPaste(((Parental)treeSelection).getMyParent()));
	}

	private boolean canPaste(Object o)
	{
		return (o instanceof Pastable && ((Pastable)o).acceptPaste(clipboard));
	}

	private void updateMenuState()
	{
		CCFChild c[] = null;

		boolean ismulti = multiSelect.size() > 1;
		boolean autoload = (fileName != null && !autoLoad.contains(fileName));
		boolean gridsnap = (dragSelection instanceof ChildPanel || (treeSelection instanceof CCFTreePanel && ((CCFTreePanel)treeSelection).isPanelShowing()));

		orderMenu.setEnabled(dragSelection instanceof ChildPanel && !(dragSelection instanceof PanelPanel));
		alignMenu.setEnabled(ismulti);
		convertMenu.setEnabled(isLoaded());
		rcDevicePasteKey.setEnabled(clipboard instanceof CCFAction);
		rcDevicePasteActions.setEnabled(clipboard instanceof CCFActionList);

		menuEnable(FILE_REVERT, canRevert());
		menuEnable(FILE_SAVE, isLoaded());
		menuEnable(FILE_SAVEAS, isLoaded());
		menuEnable(FILE_EXPORT, isLoaded());
		menuEnable(FILE_CCF_GET, hasComm);
		menuEnable(FILE_CCF_PUT, hasComm && isLoaded());
		menuEnable(FILE_CLOSE_WINDOW, tontos.size() > 2);
		menuEnable(EDIT_UNDO, !undoStack.empty());
		menuEnable(EDIT_REDO, !redoStack.empty());
		menuEnable(EDIT_CUT, treeSelection instanceof Deletable);
		menuEnable(EDIT_COPY, treeSelection instanceof Copyable || treeSelection instanceof JTextComponent);
		menuEnable(EDIT_PASTE, enableMenuPaste());
		menuEnable(EDIT_DELETE, treeSelection instanceof Deletable);
		menuEnable(EDIT_PROPERTY, ccf != null && treeSelection instanceof Configurable);
		menuEnable(EDIT_SELECT_ALL, dragSelection instanceof ChildPanel);
		menuEnable(EDIT_UNSELECT_ALL, dragSelection != null);
		menuEnable(ADD_DEVICE, ccf != null && treeSelection instanceof CCFTreeDeviceFolder);
		menuEnable(ADD_PANEL,treeSelection instanceof CCFTreeDevice);
		menuEnable(ADD_FRAME, treeSelection instanceof FrameHost);
		menuEnable(ADD_BUTTON, treeSelection instanceof ButtonHost);
		menuEnable(ICON_LIBRARY, isLoaded());
		menuEnable(ICON_REPLACE, isLoaded());
		menuEnable(TAB_MASTER, currentDevice != null && currentPanel != null);
		menuEnable(THEME_MASTER, currentDevice != null);
		menuEnable(COPY_ALIAS, treeSelection instanceof ButtonBox || treeSelection instanceof CCFTreePanel || treeSelection instanceof CCFTreeDevice);
		menuEnable(PASTE_ALIAS, treeSelection instanceof ButtonBox && clipboard instanceof CCFAction);
		menuEnable(UTIL_EMULATOR, getEmulator() != null);
		menuEnable(UTIL_EDITOR, prefEditor != null);
		menuEnable(UTIL_FIRMWARE, hasComm);
		menuEnable(UTIL_UNDEAD, hasComm);
		menuEnable(AUTOLOAD_ADD, autoload);
		menuEnable(AUTOLOAD_REMOVE, !autoload && fileName != null);
		menuEnable(OBJECTS_GROUP, ismulti);
		menuEnable(OBJECTS_UNGROUP, dragSelection instanceof FrameBox && (c = ((FrameBox)dragSelection).getFrame().getChildren()) != null && c.length > 0);
		menuEnable(ALIGN_TOP, ismulti);
		menuEnable(ALIGN_BOTTOM, ismulti);
		menuEnable(ALIGN_LEFT, ismulti);
		menuEnable(ALIGN_RIGHT, ismulti);
		menuEnable(ALIGN_HCENTER, ismulti);
		menuEnable(ALIGN_VCENTER, ismulti);
		menuEnable(PASTE_ACTIONS, clipboard instanceof CCFActionList);
		menuEnable(PASTE_REPLACE_ICONS, clipboard instanceof CCFIconSet);
		menuEnable(PASTE_ICONS, clipboard instanceof CCFIconSet);
		menuEnable(PASTE_KEYS, clipboard instanceof CCFHardKey[]);
		menuEnable(TOGGLE_GRID, gridsnap);
		menuEnable(TOGGLE_SNAP, gridsnap);
		menuEnable(OBJECT_TRANSPARENT, dragSelection instanceof ButtonBox || dragSelection instanceof FrameBox);
	}

	private boolean isConnected()
	{
		return comm != null;
	}

	private boolean isLoaded()
	{
		return ccf != null;
	}

	private static void setRCMenu(JTextComponent tc)
	{
		tc.addMouseListener(new TextRCMenu(tc));
	}

	private static JMenuItem newPopMenuItem(String text, int cmd)
	{
		MenuAction action = new MenuAction(cmd);
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(action);
		item.addMenuKeyListener(action);
		item.setEnabled(true);
		popmenus.put(new Integer(cmd), item);
		return item;
	}

	private static JCheckBoxMenuItem newPopToggleMenuItem(String text, int cmd)
	{
		MenuAction action = new MenuAction(cmd);
		JCheckBoxMenuItem item = new JCheckBoxMenuItem(text);
		item.addActionListener(action);
		item.addMenuKeyListener(action);
		item.setEnabled(true);
		Integer i = new Integer(cmd);
		Vector v = (Vector)popmenus.get(i);
		if (v == null)
		{
			v = new Vector();
			popmenus.put(i, v);
		}
		v.add(item);
		return item;
	}

	private static MenuItem newMenuItem(String text, int hot, int cmd)
	{
		return newMenuItem(text, hot, cmd, false);
	}

	private static MenuItem newMenuItem(String text, int hot, int cmd, boolean en)
	{
		return newMenuItem(text, hot, cmd, en, false);
	}

	private static MenuItem newMenuItem(String text, int hot, int cmd, boolean en, boolean shift)
	{
		MenuAction action = new MenuAction(cmd);
		MenuItem item = hot >= 0 ?
			new MenuItem(text, new MenuShortcut(hot, shift)) :
			new MenuItem(text);
		item.addActionListener(action);
		item.setEnabled(en);
		menus.put(new Integer(cmd), item);
		return item;
	}

	private static MenuItem newMenuItem(String text, ActionListener al)
	{
		MenuItem item = new MenuItem(text);
		item.addActionListener(al);
		return item;
	}

	// -------------------------------------------------------------------------------------
	// TREE AND DESKTOP
	// -------------------------------------------------------------------------------------
	private void clearDesktop()
	{
		for (Enumeration e = panels.elements(); e.hasMoreElements(); )
		{
			Object o = e.nextElement();
			if (o instanceof JInternalFrame)
			{
				((JInternalFrame)o).dispose();
			}
		}
		panels.clear();
		refreshTreeModel();
	}

	private boolean isPanelShowing(CCFPanel panel)
	{
		DeskPanel dp = getDeskPanel(panel);
		return dp != null && dp.isShowing();
	}

	private PanelPanel getPanelPanel(CCFPanel panel)
	{
		DeskPanel dp = getDeskPanel(panel);
		return dp != null ? dp.getPanel() : null;
	}

	private DeskPanel getDeskPanel(CCFPanel panel)
	{
		return (DeskPanel)panels.get(panel);
	}

	private void showDeskPanel(CCFPanel panel)
	{
		if (panel == null)
		{
			return;
		}
		DeskPanel dp = (DeskPanel)panels.get(panel);
		if (dp != null)
		{
			dp.moveToFront();
			return;
		}
		PanelPanel pp = new PanelPanel(panel);
		String nm = panel.getName();
		dp = new DeskPanel(pp);
		panels.put(panel, dp);
		panels.put(dp, panel);
		work.add(dp);
		try { dp.setSelected(true); } catch (Exception ex) { errorDialog(ex); }
		Dimension psz = dp.getSize();
		Dimension wsz = work.getSize();
		if (nextPosition.x + psz.width > wsz.width)
		{
			nextPosition.move(2,nextPosition.y);
		}
		if (nextPosition.y + psz.height > wsz.height)
		{
			nextPosition.move(nextPosition.x,2);
		}
		dp.setLocation(nextPosition);
		nextPosition.translate(25,25);
		dp.addInternalFrameListener(new InternalFrameAdapter()
		{
			public void internalFrameClosed(InternalFrameEvent e) {
				CCFPanel pp = (CCFPanel)panels.get(e.getSource());
				removePanel(pp, false);
				debug.log(3, "closing desk panel "+pp);
			}
		});
		updateMenuState();
		if (panel != null && panel.isTemplate())
		{
			repaintAllPanels();
		}
	}

	// find tree node that is the parent or container of a specified object
	private Object getTreeWrapper(Object o)
	{
		return wrappers.get(o);
	}

	private ChildPanel getChildPanel(CCFChild child)
	{
		CCFPanel panel = child.getParentPanel();
		if (panel != null)
		{
			DeskPanel dp = (DeskPanel)panels.get(panel);
			if (dp != null)
			{
				PanelPanel pp = dp.getPanel();
				ChildPanel cp = pp.findChild(child);
				return cp;
			}
		}
		return null;
	}

	private void refreshTreeModel()
	{
		model.refresh();
	}

	void refreshTreeDevice(CCFDevice dev)
	{
		Object tp = getTreeWrapper(dev);
		if (tp != null && tp instanceof TreeFolder)
		{
			((TreeFolder)tp).refresh();
			tree.expandPath(((TreeFolder)tp).getTreePath());
			debug.log(2,"refreshTreeDevice: "+dev);
		}
	}

	private void refreshTreePanel(CCFPanel panel)
	{
		Object tp = getTreeWrapper(panel);
		if (tp != null)
		{
			if (tp instanceof TreeFolder)
			{
				((TreeFolder)tp).refresh();
			}
			tree.expandPath(((TreeFolder)tp).getTreePath());
			debug.log(2,"refreshTreePanel: "+panel);
		}
	}

	private void repaintPanel(CCFPanel panel)
	{
//debug.log(0, "refreshPanel: "+panel);
		DeskPanel dp = (DeskPanel)panels.get(panel);
		if (dp != null)
		{
			dp.repaint();
		}
	}

	private void refreshPanel(CCFPanel panel)
	{
//debug.log(0, "refreshPanel: "+panel);
		DeskPanel dp = (DeskPanel)panels.get(panel);
		if (dp != null)
		{
			dp.updateTitle();
			dp.refresh();
			debug.log(3,"refreshPanel: "+panel);
		}
	}

	private void repaintAllPanels()
	{
		for (Enumeration e = panels.keys(); e.hasMoreElements(); )
		{
			Object o = e.nextElement();
			if (o instanceof CCFPanel)
			{
				repaintPanel((CCFPanel)o);
			}
		}
	}
	
	private void refreshAllPanels()
	{
		for (Enumeration e = panels.keys(); e.hasMoreElements(); )
		{
			Object o = e.nextElement();
			if (o instanceof CCFPanel)
			{
				refreshPanel((CCFPanel)o);
			}
		}
	}
	
	private void closePanel()
	{
		JInternalFrame f = work.getSelectedFrame();
		if (f != null) {
			f.dispose();
		}
	}

	private ChildPanel getUIWrapper(CCFChild child)
	{
		return (ChildPanel)wrappers.get(child);
	}

	private void removePanel(CCFPanel panel, boolean dispose)
	{
		DeskPanel dp = (DeskPanel)panels.get(panel);
		if (dp == null)
		{
			return;
		}
		if (dispose)
		{
			dp.dispose();
		}
		panels.remove(panel);
		panels.remove(dp);
	}

	private CCFButton addNewButton(CCFPanel panel)
	{
		boolean color = color();
		CCFButton nb = panel.createButton("New");
		nb.copyColors(color ? defaultColorButton : defaultGrayButton);
		nb.copyIcons(color ? defaultColorButton : defaultGrayButton);
		pushDo(new DoAdd(nb.getChildWrapper(), panel));
		return nb;
	}

	private CCFFrame addNewFrame(CCFPanel panel)
	{
		CCFFrame nf = panel.createFrame("New");
		pushDo(new DoAdd(nf.getChildWrapper(), panel));
		return nf;
	}

	private void addNewButton(CCFFrame frame)
	{
		boolean color = color();
		CCFButton nb = frame.createButton("New");
		nb.copyColors(color ? defaultColorButton : defaultGrayButton);
		nb.copyIcons(color ? defaultColorButton : defaultGrayButton);
		pushDo(new DoAdd(nb.getChildWrapper(), frame));
	}

	private void addNewFrame(CCFFrame frame)
	{
		CCFFrame nf = frame.createFrame("New");
		pushDo(new DoAdd(nf.getChildWrapper(), frame));
	}

	private JButton newButton(String text, int cmd)
	{
		MenuAction action = new MenuAction(cmd);
		JButton button = new JButton(text);
		button.addActionListener(action);
		return button;
	}

	private JButton newButton(String text, ActionListener action, int key)
	{
		JButton button = new JButton(text);
		button.addActionListener(action);
		bindAction(button, key, 0, new ButtonAction(button));
		return button;
	}

	// -------------------------------------------------------------------------------------
	// DIALOGS
	// -------------------------------------------------------------------------------------
	static void infoDialog(String msg)
	{
		JOptionPane.showMessageDialog(StackedDialog.parent(),
			msg, "FYI", JOptionPane.INFORMATION_MESSAGE);
	}

	static void errorDialog(String msg)
	{
		Util.errorDialog(msg, null);
	}

	static void errorDialog(Throwable ex)
	{
		debug(ex);
		Util.errorDialog(ex.toString(), null);
	}

	static void errorDialog(String msg, Throwable ex)
	{
		debug(ex);
		Util.errorDialog(msg, ex);
	}

	static boolean confirmDialog(String title, String msg)
	{
		return Util.confirmDialog(title, msg);
	}

	// -------------------------------------------------------------------------------------
	// INTERFACES
	// -------------------------------------------------------------------------------------
	// ---( TreeSelectionListener interface methods )---
	public void valueChanged(TreeSelectionEvent e)
	{
		setTreeSelection(e.getPath().getLastPathComponent());
	}

	// ---( ITaskStatus interface methods )---
	public void taskStatus(int type, String msg)
	{
		statusText.setText(msg);
		statusText.repaint();
	}

	public void taskError(Throwable t)
	{
		errorDialog(t);
	}

	public void taskNotify(Object o)
	{
		if (o instanceof Integer)
		{
			sendEvent(((Integer)o).intValue());
		}
		else
		{
			debug.log(0, "task object: "+o);
		}
	}

	// -------------------------------------------------------------------------------------
	// E V E N T   M E T H O D S
	// -------------------------------------------------------------------------------------
	private void eventNewTonto()        { new Tonto(); }
	private void eventCloseTonto()      { exit(); }
	private void eventFileNew()         { fileNew(); }
	private void eventFileLoad()        { load(true); }
	private void eventFileMerge()       { load(false); }
	private void eventFileRevert()      { revert(); }
	private void eventFileSave()        { save(); }
	private void eventFileSaveAs()      { saveAs(); }
	private void eventFileClose()       { file_close(); }
	private void eventFileClosePanel()  { closePanel(); }
	private void eventFileUpload()      { upload(true); }
	private void eventFileDownload()    { download(); }
	private void eventFilePreferences() { prefsDialog.show(); }
	private void eventFileExit()        { exitAll(); }
	private void eventFileExport()
	{
		File fex = getFile(false, "Zip Files (*.zcf)", new String[] { "zcf" });
		if (fex != null)
		{
			String fn = fex.toString();
			if (!fn.toLowerCase().endsWith(".zcf"))
			{
				fn = fn+".zcf";
			}
			save(fn);
		}
	}

	private void eventEditUndo()        { popDo(); }
	private void eventEditRedo()        { reDo(); }
	private void eventEditCut()
	{
		debug.log(2, "edit|cut "+Util.nickname(treeSelection));
		if (treeSelection instanceof Deletable)
		{
			int id = startMultiDo();
			setClipboard(treeSelection);
			if (!multiSelect.contains(treeSelection))
			{
				((Deletable)treeSelection).delete();
			}
			Object o[] = multiSelect.toArray();
			for (int i=0; i<o.length; i++)
			{
				((Deletable)o[i]).delete();
			}
			setTreeSelection(null);
			endMultiDo(id);
		}
	}
	private void eventEditCopy()
	{
		debug.log(2, "edit|copy "+Util.nickname(treeSelection));
		if (treeSelection instanceof Copyable)
		{
			setClipboard(((Copyable)treeSelection).copy());
		}
	}
	private void eventEditPaste()
	{
		pasteClipboardTo(treeSelection);
	}
	private void eventEditDelete()
	{
		debug.log(2, "edit|delete "+Util.nickname(treeSelection));
		if (treeSelection instanceof Deletable)
		{
			int id = startMultiDo();
			if (!multiSelect.contains(treeSelection))
			{
				((Deletable)treeSelection).delete();
			}
			Object o[] = multiSelect.toArray();
			for (int i=0; i<o.length; i++)
			{
				((Deletable)o[i]).delete();
			}
			setTreeSelection(null);
			endMultiDo(id);
		}
	}
	private void eventEditProperty()
	{
		if (treeSelection instanceof Configurable)
		{
			((Configurable)treeSelection).editProperties();
		}
	}
	private void eventEditSelectAll()
	{
		if (dragSelection instanceof ChildPanel)
		{
			if (dragSelection instanceof ButtonBox)
			{
				((ChildPanel)dragSelection.getParent()).selectAll();
			}
			else
			{
				((ChildPanel)dragSelection).selectAll();
			}
		}
	}
	private void eventEditUnselectAll()
	{
		setDragSelection(null);
	}
	private void eventEditCopyAlias()
	{
		if (treeSelection instanceof CCFTreeDevice)
		{
			setClipboard(new ActionAliasDevice(((CCFTreeDevice)treeSelection).getDevice()));
		}
		else
		if (treeSelection instanceof CCFTreePanel)
		{
			setClipboard(new ActionJumpPanel(((CCFTreePanel)treeSelection).getPanel(), header().isNewMarantz()));
		}
		else
		if (treeSelection instanceof ButtonBox)
		{
			setClipboard(new ActionAliasButton(((ButtonBox)treeSelection).getButton()));
		}
	}
	private void eventEditPasteAlias()
	{
		if (treeSelection instanceof ButtonBox && clipboard instanceof CCFAction)
		{
			((ButtonBox)treeSelection).getButton().appendAction((CCFAction)((CCFAction)clipboard).getClone());
			((ButtonBox)treeSelection).refreshDeskPanel();
		}
	}
	private void eventPasteKeyAlias(int key)
	{
		CCFDevice dev = ((CCFTreeDevice)treeSelection).getDevice();
		if (key == 0)
		{
			if (dev.action == null)
			{
				dev.action = new CCFActionList();
			}
			dev.action.appendAction((CCFAction)clipboard);
		}
		else
		{
			CCFHardKey k[] = dev.getHardKeys();
			CCFActionList al = k[key-1].getActionList();
			if (al == null)
			{
				al = new CCFActionList();
			}
			al.appendAction((CCFAction)clipboard);
			k[key-1].setActionList(al);
		}
	}


	private void eventUndeadPronto()
	{
		if (isnew)
		{
			errorDialog("You must load a valid CCF before\nperforming an 'Undead' operation");
		}
		else
		{
			new UndeadDialog().show();
		}
	}

	private void eventLocatePronto()    { try { new CommStatusDialog(); } catch (Exception ex) { errorDialog(ex); } }
	private void eventRebootPronto()    { rebootPronto(); }
	private void eventRemoteFirmware()  { firmware(); }
	private void eventRunEmulator()     { runEmulator(); }
	private void eventRunEditor()       { runEditor(); }
	private void eventNetworkUpdate()   { checkForUpdates(true); }
	private void eventRawUpload()       { rawUpload(); }
	private void eventRawDownload()     { rawDownload(); }
	private void eventEmailLogfile()    { emailLogfile(); }
	private void eventAutoloadAdd()
	{
		if (fileName != null && !autoLoad.contains(fileName))
		{
			autoLoad.add(fileName);
			updateMenuState();
		}
	}
	private void eventAutoloadRemove()
	{
		if (fileName != null && autoLoad.contains(fileName))
		{
			autoLoad.remove(fileName);
			updateMenuState();
		}
	}
	private void eventConvertCCF(int type)
	{
		try
		{
			ccf.header().setColorMap(prefColorMap);
			ccf.conformTo(ProntoModel.getModel(type));
			panelSize = ccf.getScreenSize(prefDefaultModel);
			setCCFChanged();
			updateCCFInfo();
			refreshAllPanels();
			refreshTreeModel();
		}
		catch (Exception ex)
		{
			debug.log(0, "No ProntoModel matching type '"+type+"' for conversion");
		}
	}

	private void eventModuleIRDB()        { irdb.show(); }
	private void eventModuleIcons()       { icons.invoke(); }
	private void eventModuleIconSwap()    { new IconReplace().show(); }
	private void eventModuleTabMaster()   { tabMaster(); }
	private void eventModuleThemeMaster() { themeMaster(); }

	private JFrame debugFrame;

	private void eventHelpAbout()    { new AboutDialog(this).show(); }
	private void eventHelpDocs()     { Browser.displayURL("http://giantlaser.com/tonto/?x=doc_guide&y=1"); }
	private void eventHelpTutorial() { Browser.displayURL("http://giantlaser.com/tonto/?x=doc_tutor&y=1"); }
	private void eventHelpFAQ()      { Browser.displayURL("http://giantlaser.com/tonto/?x=doc_faq&y=1"); }
	private void eventHelpChanges()  { Browser.displayURL("http://giantlaser.com/tonto/tonto/changelog.txt?y=1"); }
	private void eventHelpCCFs()     { Browser.displayURL("http://remotecentral.com/cgi-bin/files/rcfiles.cgi?area=pronto&db=devices&br=&fc="); }
	private void eventHelpDiscrete() { Browser.displayURL("http://remotecentral.com/cgi-bin/files/rcfiles.cgi?area=pronto&db=discrete&br=&fc="); }
	private void eventHelpDebug()
	{
		if (debugFrame != null)
		{
			debugFrame.show();
			return;
		}
		final JFrame f = new JFrame("Debug Window");
		debugFrame = f;
		final JTextArea ta = new JTextArea(10,70);
		final DebugListener dl = new DebugListener() {
			public void debugAction(String msg) {
				ta.append(msg+"\n");
				ta.scrollRectToVisible(new Rectangle(0,ta.getHeight()-2,1,1));
			}
		};
		Debug.addListener(dl);
		JScrollPane sp = new JScrollPane(ta);
		f.getContentPane().add("Center", sp);
		f.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent we) {
				prefs.setProperty(PREF_DEBUG_BOUNDS, debugFrame.getBounds());
				debugFrame = null;
				Debug.removeListener(dl);
			}
		});
		f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		f.pack();
		Rectangle r = prefs.getRectangle(PREF_DEBUG_BOUNDS, null);
		if (r != null)
		{
			f.setBounds(r);
		}
		f.show();
	}

	private void eventObjectAddDevice()
	{
		if (treeSelection instanceof CCFTreeDeviceFolder)
		{
			((CCFTreeDeviceFolder)treeSelection).newDevice();
			tree.expandPath(((CCFTreeDeviceFolder)treeSelection).getTreePath());
			setCCFChanged();
		}
	}
	private void eventObjectAddPanel()
	{
		if (treeSelection instanceof CCFTreeDevice)
		{
			((CCFTreeDevice)treeSelection).newPanel();
			tree.expandPath(((CCFTreeDevice)treeSelection).getTreePath());
			setCCFChanged();
		}
	}
	private void eventObjectAddFrame()
	{
		if (treeSelection instanceof FrameHost)
		{
			((FrameHost)treeSelection).addFrame();
			setCCFChanged();
		}
	}
	private void eventObjectAddButton()
	{
		if (treeSelection instanceof ButtonHost)
		{
			((ButtonHost)treeSelection).addButton();
			setCCFChanged();
		}
	}
	private void eventObjectTop()
	{
		if (dragSelection instanceof ChildPanel)
		{
			((ChildPanel)dragSelection).getSource().top();
			((ChildPanel)dragSelection).refreshDeskPanel();
		}
	}
	private void eventObjectBottom()
	{
		if (dragSelection instanceof ChildPanel)
		{
			((ChildPanel)dragSelection).getSource().bottom();
			((ChildPanel)dragSelection).refreshDeskPanel();
		}
	}
	private void eventObjectRaise()
	{
		if (dragSelection instanceof ChildPanel)
		{
			((ChildPanel)dragSelection).getSource().raise();
			((ChildPanel)dragSelection).refreshDeskPanel();
		}
	}
	private void eventObjectLower()
	{
		if (dragSelection instanceof ChildPanel)
		{
			((ChildPanel)dragSelection).getSource().lower();
			((ChildPanel)dragSelection).refreshDeskPanel();
		}
	}
	private void eventObjectTransparency()
	{
		if (treeSelection instanceof FrameBox) {
			FrameBox fb = (FrameBox)treeSelection;
			CCFFrame fr = fb.getFrame();
			PanelPanel pp = fb.getRootPanel();
			CCFIcon icon = pp.getPicture(fb);
			CCFIcon trans = CCFIcon.composite(fr.getIcon(), icon);
			DoNodeUpdate up = new DoNodeUpdate(fr);
			fr.setIcon(trans);
			up.pushDoNewState();
			refreshPanel(fr.getParentPanel());
		} else
		if (treeSelection instanceof ButtonBox) {
			ButtonBox bb = (ButtonBox)treeSelection;
			CCFButton btn = bb.getButton();
			PanelPanel pp = bb.getRootPanel();
			CCFIconSet is = btn.getIconSet().getClone();
			CCFIcon icon = pp.getPicture(bb);

			Hashtable h = new Hashtable();
			int k[] = new int[] { CCFIconSet.ACTIVE_SELECTED, CCFIconSet.ACTIVE_UNSELECTED, CCFIconSet.INACTIVE_SELECTED, CCFIconSet.INACTIVE_UNSELECTED };
			for (int i=0; i<k.length; i++)
			{
				CCFIcon ci = is.getIcon(i);
				CCFIcon ri = ci == null ? null : (CCFIcon)h.get(ci);
				if (ri == null)
				{
					ri = CCFIcon.composite(ci, icon);
					if (ci != null)
					{
						h.put(ci, ri);
					}
				}
				is.setIcon(i, ri);
			}

			DoNodeUpdate up = new DoNodeUpdate(btn);
			btn.setIconSet(is);
			up.pushDoNewState();
			refreshPanel(btn.getParentPanel());
		}
	}
	private void eventPanelToggleGrid()
	{
		if (treeSelection instanceof ChildPanel)
		{
			((ChildPanel)treeSelection).toggleGrid();
		}
		else
		if (treeSelection instanceof CCFTreePanel)
		{
			PanelPanel pp = getPanelPanel(((CCFTreePanel)treeSelection).getPanel());
			if (pp != null)
			{
				pp.toggleGrid();
			}
		}
	}
	private void eventPanelToggleSnap()
	{
		if (treeSelection instanceof ChildPanel)
		{
			((ChildPanel)treeSelection).toggleSnap();
		}
		else
		if (treeSelection instanceof CCFTreePanel)
		{
			PanelPanel pp = getPanelPanel(((CCFTreePanel)treeSelection).getPanel());
			if (pp != null)
			{
				pp.toggleSnap();
			}
		}
	}
	private void eventTreeExpandAll()
	{
		eventTreeDeviceFolders(true);
	}
	private void eventTreeCollapseAll()
	{
		eventTreeDeviceFolders(false);
	}
	private void eventTreeDeviceFolders(boolean ex)
	{
		eventTreeDeviceFolders(ccf().header().firstHome, ex);
		eventTreeDeviceFolders(ccf().header().firstDevice, ex);
		eventTreeDeviceFolders(ccf().header().firstMacro, ex);
	}
	private void eventTreeDeviceFolders(CCFDevice dev, boolean ex)
	{
		while (dev != null)
		{
			TreeNode tn = (TreeNode)getTreeWrapper(dev);
			if (dev != null)
			{
				if (ex)
				{
					tree.expandPath(tn.getTreePath());
				}
				else
				{
					tree.collapsePath(tn.getTreePath());
				}
			}
			dev = dev.next;
		}
	}
	private void eventButtonCopyIcons()
	{
		if (treeSelection instanceof ButtonBox)
		{
			setClipboard(((ButtonBox)treeSelection).getButton().getIconSet());
		}
	}
	private void eventButtonPasteIcons()
	{
		if (treeSelection instanceof ButtonBox && clipboard instanceof CCFIconSet)
		{
			CCFButton button = ((ButtonBox)treeSelection).getButton();
			button.setIconSet((CCFIconSet)clipboard);
			refreshPanel(button.getParentPanel());
			setDragSelection(getChildPanel((CCFChild)button.getParent()));
		}
	}
	private void eventButtonPasteReplaceIcons()
	{
		if (treeSelection instanceof ButtonBox && clipboard instanceof CCFIconSet)
		{
			CCFButton button = ((ButtonBox)treeSelection).getButton();
			CCFIconSet dst = (CCFIconSet)clipboard;
			CCFIconSet src = button.getIconSet();
			if (confirmDialog("Global Icon Replace", "Proceed with globally replacing all matching icons?"))
			{
				int s[] = CCFIconSet.getValidStates();
				for (int i=0; i<s.length; i++)
				{
					iconReplace(src.getIcon(s[i]), dst.getIcon(s[i]));
				}
			}
			refreshAllPanels();
		}
	}
	private void eventButtonCopyActions()
	{
		if (treeSelection instanceof ButtonBox)
		{
			CCFButton b = ((ButtonBox)treeSelection).getButton();
			CCFActionList al = b.getActionList();
			if (al != null)
			{
				setClipboard(al.getClone());
			}
		}
	}
	private void eventButtonPasteActions()
	{
		if (treeSelection instanceof ButtonBox && clipboard instanceof CCFActionList)
		{
			CCFButton button = ((ButtonBox)treeSelection).getButton();
			button.setActionList((CCFActionList)((CCFActionList)clipboard).getClone());
			refreshPanel(button.getParentPanel());
		}
	}
	private void eventDeviceCopyKeys()
	{
		if (treeSelection instanceof CCFTreeDevice)
		{
			setClipboard(
				((CCFTreeDevice)treeSelection).getDevice().getHardKeys());
		}
	}
	private void eventButtonPasteKeys()
	{
		if (treeSelection instanceof CCFTreeDevice && clipboard instanceof CCFHardKey[])
		{
			CCFDevice device = ((CCFTreeDevice)treeSelection).getDevice();
			device.setKeyActions((CCFHardKey[])clipboard);
		}
	}

	private void eventDeviceKeyAlias(int key)
	{
		if (treeSelection instanceof CCFTreeDevice)
		{
			setClipboard(new ActionAliasKey(((CCFTreeDevice)treeSelection).getDevice(), key));
		}
	}

	private void eventDeviceCopyKeyActions(int key)
	{
		if (treeSelection instanceof CCFTreeDevice)
		{
			CCFActionList al = null;
			CCFDevice dev = ((CCFTreeDevice)treeSelection).getDevice();
			if (key == 0)
			{
				al = dev.action;
			}
			else
			{
				al = dev.getHardKeys()[key-1].getActionList();
			}
			if (al != null)
			{
				setClipboard((CCFActionList)al.getClone());
			}
		}
	}

	private void eventDevicePasteKeyActions(int key)
	{
		if (clipboard instanceof CCFActionList)
		{
			CCFActionList al = (CCFActionList)((CCFActionList)clipboard).getClone();
			CCFDevice dev = ((CCFTreeDevice)treeSelection).getDevice();
			if (key == 0)
			{
				dev.action = al;
			}
			else
			{
				dev.getHardKeys()[key-1].setActionList(al);
			}
		}
	}

	private void eventViewHideSelection()
	{
		hideSelection = !hideSelection;
		repaintAllPanels();
	}

	private void eventObjectsAlign(int key)
	{
		if (multiSelect.size() < 2)
		{
			return;
		}
		Rectangle r = getSelectionBounds();
		MultiDo dos = new MultiDo();
		for (int i=0; i<multiSelect.size(); i++)
		{
			Component c = (Component)multiSelect.get(i);
			Rectangle b = c.getBounds();
			Rectangle n = null;
			switch (key)
			{
				case ALIGN_TOP: n = new Rectangle(b.x, r.y, b.width, b.height); break;
				case ALIGN_BOTTOM: n = new Rectangle(b.x, r.y+r.height-b.height, b.width, b.height); break;
				case ALIGN_LEFT: n = new Rectangle(r.x, b.y, b.width, b.height); break;
				case ALIGN_RIGHT: n = new Rectangle(r.x+r.width-b.width, b.y, b.width, b.height); break;
				case ALIGN_HCENTER: n = new Rectangle(r.x+r.width/2-b.width/2, b.y, b.width, b.height); break;
				case ALIGN_VCENTER: n = new Rectangle(b.x, r.y+r.height/2-b.height/2, b.width, b.height); break;
			}
			dos.add(new DoBounds(c, b, n));
		}
		pushDo(dos);
	}

	private void eventObjectSnap(int key)
	{
		switch (key)
		{
			case SNAP_TOP_LEFT:     snaps = new SnapInfo(0.0, 0.0); break;
			case SNAP_TOP:          snaps = new SnapInfo(0.5, 0.0); break;
			case SNAP_TOP_RIGHT:    snaps = new SnapInfo(1.0, 0.0); break;
			case SNAP_RIGHT:        snaps = new SnapInfo(1.0, 0.5); break;
			case SNAP_BOTTOM_RIGHT: snaps = new SnapInfo(1.0, 1.0); break;
			case SNAP_BOTTOM:       snaps = new SnapInfo(0.5, 1.0); break;
			case SNAP_BOTTOM_LEFT:  snaps = new SnapInfo(0.0, 1.0); break;
			case SNAP_LEFT:         snaps = new SnapInfo(0.0, 0.5); break;
		}
	}

	private void eventObjectsGroup()
	{
		if (multiSelect.size() < 2)
		{
			return;
		}

		Rectangle r = getSelectionBounds();
		Rectangle r2 = new Rectangle(r);
		ChildPanel p = (ChildPanel)((ChildPanel)multiSelect.get(0)).getParent();
		CCFChild pc = p.getSource();
		CCFPanel pp = pc != null ? pc.getParentPanel() : ((PanelPanel)p).getPanel();
		while (!(p instanceof PanelPanel))
		{
			Point ppos = p.getLocation();
			r.x += ppos.x;
			r.y += ppos.y;
			p = (ChildPanel)p.getParent();
		}

		int id = startMultiDo();
		pushDo(new DoRefresher(pp, false));

		DoNodeUpdate upParent = pc != null ? new DoNodeUpdate(pc.child) : null;
		DoNodeUpdate upPanel = new DoNodeUpdate(pp);

		CCFFrame fr = pp.createFrame("");
		CCFChild fc = fr.getChildWrapper();
		fc.setBounds(r);
		CCFChild cc[] = new CCFChild[multiSelect.size()];
		for (int i=0; i<multiSelect.size(); i++)
		{
			CCFChild c = ((ChildPanel)multiSelect.get(i)).getSource();
			pushDo(new DoBounds(c, -r2.x, -r2.y, 0, 0));
			cc[i] = c;
			c.delete();
		}
		appendChildren(fr, cc);
		pp.addFrame(fr);

		upPanel.getNewState();
		pushDo(upPanel);
		if (upParent != null)
		{
			upParent.getNewState();
			pushDo(upParent);
		}

		pushDo(new DoRefresher(pp, true));
		endMultiDo(id);

		setDragSelection(getUIWrapper(fc));
	}

	private void eventObjectsUngroup()
	{
		if (!(dragSelection instanceof FrameBox))
		{
			return;
		}

		FrameBox fb = ((FrameBox)dragSelection);
		CCFFrame fr = fb.getFrame();
		CCFPanel pp = fr.getParentPanel();
		CCFChild nc[] = fr.getChildren();
		if (nc == null || nc.length == 0)
		{
			return;
		}

		CCFChild fc = fr.getChildWrapper();
		IChildContainer pn = (IChildContainer)fc.getParent();

		int id = startMultiDo();
		pushDo(new DoRefresher(pp, false));

		DoNodeUpdate oldFrame = new DoNodeUpdate(fr);
		DoNodeUpdate rootPanel = new DoNodeUpdate(pp);

		for (int i=0; i<nc.length; i++)
		{
			pushDo(new DoBounds(nc[i], fc.intX, fc.intY, 0, 0));
			nc[i].delete();
		}

		fb.delete();
		appendChildren(pn, nc);

		oldFrame.getNewState();
		rootPanel.getNewState();
		pushDo(oldFrame);
		pushDo(rootPanel);

		pushDo(new DoRefresher(pp, true));
		updateMenuState();

		endMultiDo(id);

		setDragSelection(null);
		for (int i=0; i<nc.length; i++)
		{
			Selectable s = (Selectable)getUIWrapper(nc[i]);
			multiSelect.add(s);
			s.select(false);
		}
		dragSelection = (Component)multiSelect.get(0);
	}

	// TODO: make relative to panel root. move to util segment.
	// allow multi-select to span containers (but not panels)
	private void appendChildren(IChildContainer cc, CCFChild nc[])
	{
		CCFChild oc[] = cc.getChildren();
		if (oc == null)
		{
			oc = new CCFChild[0];
		}
		CCFChild ca[] = new CCFChild[nc.length + oc.length];
		System.arraycopy(oc,0,ca,0,oc.length);
		System.arraycopy(nc,0,ca,oc.length,nc.length);
		cc.setChildren(ca);
	}

	// -------------------------------------------------------------------------------------
	// E V E N T   D I S P A T C H
	// -------------------------------------------------------------------------------------
	public static void sendEvent(int notify)
	{
		debug.log(3, "<"+Util.nickname(current)+"> sendEvent "+notify);
		switch (notify)
		{
			case FILE_WINDOW:        state().eventNewTonto(); break;
			case FILE_CLOSE_WINDOW:  state().eventCloseTonto(); break;
			case FILE_NEW:           state().eventFileNew(); break;
			case FILE_CLOSE:         state().eventFileClose(); break;
			case FILE_CLOSE_PANEL:   state().eventFileClosePanel(); break;
			case FILE_LOAD:          state().eventFileLoad(); break;
			case FILE_MERGE:         state().eventFileMerge(); break;
			case FILE_REVERT:        state().eventFileRevert(); break;
			case FILE_SAVE:          state().eventFileSave(); break;
			case FILE_SAVEAS:        state().eventFileSaveAs(); break;
			case FILE_CCF_GET:       state().eventFileUpload(); break;
			case FILE_CCF_PUT:       state().eventFileDownload(); break;
			case FILE_PREFERENCES:   state().eventFilePreferences(); break;
			case FILE_EXIT:          state().eventFileExit(); break;
			case FILE_EXPORT:        state().eventFileExport(); break;

			case EDIT_UNDO:          state().eventEditUndo(); break;
			case EDIT_REDO:          state().eventEditRedo(); break;
			case EDIT_CUT:           state().eventEditCut(); break;
			case EDIT_COPY:          state().eventEditCopy(); break;
			case EDIT_PASTE:         state().eventEditPaste(); break;
			case EDIT_DELETE:        state().eventEditDelete(); break;
			case EDIT_PROPERTY:      state().eventEditProperty(); break;
			case EDIT_SELECT_ALL:    state().eventEditSelectAll(); break;
			case EDIT_UNSELECT_ALL:  state().eventEditUnselectAll(); break;

			case IR_DATABASE:        state().eventModuleIRDB(); break;
			case ICON_LIBRARY:       state().eventModuleIcons(); break;
			case ICON_REPLACE:       state().eventModuleIconSwap(); break;
			case TAB_MASTER:         state().eventModuleTabMaster(); break;
			case THEME_MASTER:       state().eventModuleThemeMaster(); break;

			case ADD_DEVICE:         state().eventObjectAddDevice(); break;
			case ADD_PANEL:          state().eventObjectAddPanel(); break;
			case ADD_FRAME:          state().eventObjectAddFrame(); break;
			case ADD_BUTTON:         state().eventObjectAddButton(); break;
			case TOGGLE_GRID:        state().eventPanelToggleGrid(); break;
			case TOGGLE_SNAP:        state().eventPanelToggleSnap(); break;
			case TREE_EXPAND_ALL:    state().eventTreeExpandAll(); break;
			case TREE_COLLAPSE_ALL:  state().eventTreeCollapseAll(); break;
			case VIEW_HIDE_SELECT:   state().eventViewHideSelection(); break;

			case COPY_ICONS:         state().eventButtonCopyIcons(); break;
			case PASTE_ICONS:        state().eventButtonPasteIcons(); break;
			case PASTE_REPLACE_ICONS:state().eventButtonPasteReplaceIcons(); break;
			case COPY_ACTIONS:       state().eventButtonCopyActions(); break;
			case PASTE_ACTIONS:      state().eventButtonPasteActions(); break;
			case COPY_KEYS:          state().eventDeviceCopyKeys(); break;
			case PASTE_KEYS:         state().eventButtonPasteKeys(); break;

			case UTIL_LOCATE:        state().eventLocatePronto(); break;
			case UTIL_UNDEAD:        state().eventUndeadPronto(); break;
			case UTIL_UPLOAD:        state().eventRawUpload(); break;
			case UTIL_DOWNLOAD:      state().eventRawDownload(); break;
			case EMAIL_LOGFILE:      state().eventEmailLogfile(); break;
			case AUTOLOAD_ADD:       state().eventAutoloadAdd(); break;
			case AUTOLOAD_REMOVE:    state().eventAutoloadRemove(); break;
			case UTIL_REBOOT:        state().eventRebootPronto(); break;
			case UTIL_EMULATOR:      state().eventRunEmulator(); break;
			case UTIL_EDITOR:        state().eventRunEditor(); break;
			case UTIL_NET_UPDATE:    state().eventNetworkUpdate(); break;
			case UTIL_FIRMWARE:      state().eventRemoteFirmware(); break;
			case OBJECT_TRANSPARENT: state().eventObjectTransparency(); break;

			case ABOUT:              state().eventHelpAbout(); break;
			case DOCS_FAQ:           state().eventHelpFAQ(); break;
			case DOCS_MAIN:          state().eventHelpDocs(); break;
			case DOCS_TUTOR:         state().eventHelpTutorial(); break;
			case DOCS_CHANGES:       state().eventHelpChanges(); break;
			case DEBUG_WINDOW:       state().eventHelpDebug(); break;
			case HELP_CCFS:          state().eventHelpCCFs(); break;
			case HELP_DISCRETE:      state().eventHelpDiscrete(); break;

			case OBJECT_TOP:         state().eventObjectTop(); break;
			case OBJECT_BOTTOM:      state().eventObjectBottom(); break;
			case OBJECT_RAISE:       state().eventObjectRaise(); break;
			case OBJECT_LOWER:       state().eventObjectLower(); break;

			case ALIGN_TOP:
			case ALIGN_BOTTOM:
			case ALIGN_LEFT:
			case ALIGN_RIGHT:
			case ALIGN_HCENTER:
			case ALIGN_VCENTER:      state().eventObjectsAlign(notify); break;

			case SNAP_TOP_LEFT:
			case SNAP_TOP:
			case SNAP_TOP_RIGHT:
			case SNAP_RIGHT:
			case SNAP_BOTTOM_RIGHT:
			case SNAP_BOTTOM:
			case SNAP_BOTTOM_LEFT:
			case SNAP_LEFT:          state().eventObjectSnap(notify); break;

			case OBJECTS_GROUP:      state().eventObjectsGroup(); break;
			case OBJECTS_UNGROUP:    state().eventObjectsUngroup(); break;

			case COPY_ALIAS:         state().eventEditCopyAlias(); break;
			case PASTE_ALIAS:        state().eventEditPasteAlias(); break;
		}
		if ((notify & BASE_CONVERT) == BASE_CONVERT)
		{
			state().eventConvertCCF(notify ^ BASE_CONVERT);
		}
		else
		if ((notify & BASE_FILE_NEW) == BASE_FILE_NEW)
		{
			state().fileNew(notify ^ BASE_FILE_NEW);
		}
		else
		if ((notify & BASE_KEY_ALIAS) == BASE_KEY_ALIAS)
		{
			state().eventDeviceKeyAlias(notify ^ BASE_KEY_ALIAS);
		}
		else
		if ((notify & BASE_KEY_PASTE) == BASE_KEY_PASTE)
		{
			state().eventPasteKeyAlias(notify ^ BASE_KEY_PASTE);
		}
		else
		if ((notify & BASE_KEY_COPY_ACTIONS) == BASE_KEY_COPY_ACTIONS)
		{
			state().eventDeviceCopyKeyActions(notify ^ BASE_KEY_COPY_ACTIONS);
		}
		else
		if ((notify & BASE_KEY_PASTE_ACTIONS) == BASE_KEY_PASTE_ACTIONS)
		{
			state().eventDevicePasteKeyActions(notify ^ BASE_KEY_PASTE_ACTIONS);
		}
	}

	private void pasteClipboardTo(Object o)
	{
		debug.log(2,"edit|paste "+Util.nickname(clipboard)+" into "+Util.nickname(o));
		if (o instanceof Pastable)
		{
			Pastable p = (Pastable)o;
			if (p.acceptPaste(clipboard))
			{
				if (clipboard instanceof ChildPanel)
				{
					CCFChild c = ((ChildPanel)clipboard).getSource();
					Rectangle sbounds = c.getBounds();
					Dimension pbounds = (p instanceof ChildPanel ? ((ChildPanel)p).getSize() : panelSize);
					sbounds.x = Math.min(sbounds.x, pbounds.width-sbounds.width);
					sbounds.y = Math.min(sbounds.y, pbounds.height-sbounds.height);
					c.setLocation(new Point(sbounds.x,sbounds.y));
				}
				p.paste(clipboard);
				setCCFChanged();
				if (clipboard instanceof Copyable)
				{
					setClipboard(((Copyable)clipboard).copy());
				}
				else
				if (clipboard instanceof CCFNode)
				{
					setClipboard(((CCFNode)clipboard).getClone());
				}
				else
				{
					setClipboard(null);
				}
				return;
			}
		}
		if (o instanceof Parental)
		{
			debug.log(0, "edit|paste re-attempt paste to parent of "+o);
			pasteClipboardTo(((Parental)o).getMyParent());
		}
	}

	// -------------------------------------------------------------------------------------
	// I N N E R   C L A S S E S
	// -------------------------------------------------------------------------------------

	// ---( Inner Class ThemeMaster )---
	class ThemeMaster
	{
		private Hashtable frames = new Hashtable();
		private Hashtable buttons = new Hashtable();
		private int count = 0;

		ThemeMaster()
		{
			CCFDevice theme = device();
			if (theme == null)
			{
				errorDialog("No device selected for theme");
				return;
			}
			if (theme.getName() == null || !theme.getName().startsWith("Theme-"))
			{
				errorDialog("Selected device is not a theme");
				return;
			}
			CCFPanel panel = theme.getFirstPanel();
			while (panel != null)
			{
				addPanel(panel);
				panel = panel.getNextPanel();
			}
			new CCFWalker(ccf()).walk(new IWalker2() {
				public void onNode(CCFNode node) {
					if (node instanceof CCFFrame) {
						CCFFrame f = (CCFFrame)node;
						CCFFrame src = f.name != null ? (CCFFrame)frames.get(f.name) : null;
						if (src == null) {
							return;
						}
						f.colors = src.colors;
						f.icon = src.icon;
						count++;
					} else
					if (node instanceof CCFButton) {
						CCFButton b = (CCFButton)node;
						CCFButton src = b.idtag != null ? (CCFButton)buttons.get(b.idtag) : null;
						if (src == null) {
							return;
						}
						b.setIconSet(src.getIconSet());
						b.fontSize = src.fontSize;
						count++;
					}
				}
				public boolean processNode(CCFNode node) {
					if (node instanceof CCFDevice)
					{
						CCFDevice d = (CCFDevice)node;
						return (d.name != null && !d.name.startsWith("Theme-"));
					}
					else
					{
						return true;
					}
				}
			});
			infoDialog("Applied "+theme.getName()+" to "+count+" objects");
			refreshAllPanels();
		}

		private void addPanel(CCFPanel panel)
		{
			addChildren(panel.getChildren());
		}

		private void addChildren(CCFChild c[])
		{
			for (int i=0; c != null && i<c.length; i++)
			{
				switch (c[i].type)
				{
					case CCFChild.BUTTON: addButton(c[i].getButton()); break;
					case CCFChild.FRAME: addFrame(c[i].getFrame()); break;
				}
			}
		}

		private void addFrame(CCFFrame frame)
		{
			if (frame.getName() != null)
			{
				frames.put(frame.getName(), frame);
				addChildren(frame.getChildren());
			}
		}

		private void addButton(CCFButton button)
		{
			if (button.getIDTag() != null)
			{
				buttons.put(button.getIDTag(), button);
			}
		}
	}

	// ---( Inner Class TabMaster )---
	class TabMaster
	{
		private CCFDevice device;
		private CCFPanel panel;
		private Vector newPanels = new Vector();
		private Vector newButtons = new Vector();

		TabMaster(CCFPanel src)
		{
			this.panel = src;
			this.device = panel.getParentDevice();
			int tab = 1;
			int ref = startMultiDo();

			try
			{
				CCFTreeDevice td = (CCFTreeDevice)getTreeWrapper(device);
				while (true)
				{
					CCFPanel p = processTabPanel(panel, tab);
					if (p != null)
					{
						newPanels.add(p);
						p.setName("Tab-"+(tab++));
						pushDo(new DoAddPanel(td, device, p, false));
					}
					else
					{
						break;
					}
				}
				for (Enumeration e = newButtons.elements(); e.hasMoreElements(); )
				{
					CCFButton button = (CCFButton)e.nextElement();
					tab = Integer.parseInt(button.getIDTag().substring(4)) - 1;
					if (tab < newPanels.size())
					{
						button.appendAction(new ActionJumpPanel((CCFPanel)newPanels.get(tab), header().isMarantz()));
					}
				}
				infoDialog("Created "+newPanels.size()+" new panels in Device '"+device.getName()+"'");
			}
			catch (Exception ex) 
			{
				debug(ex);
			}

			endMultiDo(ref);
		}

		private CCFPanel processTabPanel(CCFPanel src, int tab)
		{
			CCFPanel dst = (CCFPanel)src.getClone();
			if (processTabChildren(dst.getChildren(), tab))
			{
				return dst;
			}
			else
			{
				return null;
			}
		}

		private boolean processTabChildren(CCFChild child[], int tab)
		{
			if (child == null || child.length == 0)
			{
				return false;
			}
			boolean ok = false;
			CCFChild nc[] = new CCFChild[child.length];
			System.arraycopy(child, 0, nc, 0, child.length);
			for (int i=0; i<nc.length; i++)
			{
				if (processTabButton(nc[i].getButton(), tab)) { ok = true; }
				if (processTabFrame(nc[i].getFrame(), tab)) { ok = true; }
			}
			return ok;
		}

		private boolean processTabFrame(CCFFrame frame, int tab)
		{
			if (frame == null)
			{
				return false;
			}
			if (frame.getFont() == CCFFont.NONE && shouldDelete(frame.getName(), tab))
			{
				frame.delete();
				return false;
			}
			return processTabChildren(frame.getChildren(), tab);
		}

		private boolean processTabButton(CCFButton button, int tab)
		{
			if (button == null)
			{
				return false;
			}
			String id = button.getIDTag();
			if (id != null && id.startsWith("Tab:"))
			{
				if (id.equals("Tab:"+tab))
				{
					button.getChildWrapper().top();
					return true;
				}
				else
				{
					button.iconAS = button.iconIS;
					button.iconAU = button.iconIU;
					button.colorAS = button.colorIS;
					button.colorAU = button.colorIU;
					newButtons.add(button);
				}
			}
			if (shouldDelete(button.getIDTag(), tab))
			{
				button.delete();
			}
			return false;
		}

		private boolean shouldDelete(String list, int tab)
		{
			if (list == null || list.indexOf("Tab ") != 0)
			{
				return false;
			}
			StringTokenizer st = new StringTokenizer(list.substring(3));
			boolean inclusion = false;
			while (st.hasMoreTokens())
			{
				String tok = st.nextToken();
				if (tok.length() >= 2)
				{
					int val = Integer.parseInt(tok.substring(1));
					switch (tok.charAt(0))
					{
						case '-':
							if (val == tab)
							{
								return true;
							}
							break;
						case '+':
							if (val == tab)
							{
								return false;
							}
							inclusion = true;
							break;
					}
				}
			}
			return inclusion;
		}
	}

	// ---( Inner Class DialogThread )---
	static class DialogThread extends Thread
	{
		protected Throwable ex;
		protected StackedDialog dialog;

		DialogThread(StackedDialog d)
		{
			this.dialog = d;
		}

		public void run()
		{
			try { body(); } catch (Throwable ex) { this.ex = ex; }
			dialog.dispose();
		}

		public void body() throws Exception { }

		public void checkError() throws Throwable
		{
			start();
			dialog.show();
			if (ex != null)
			{
				throw ex;
			}
		}
	}

	// ---( Inner Class ButtonAction )---
	class ButtonAction extends AbstractAction
	{
		private JButton button;

		ButtonAction(JButton button)
		{
			this.button = button;
		}

		public void actionPerformed(ActionEvent ae)
		{
			button.doClick();
		}
	}

	// ---( Inner Class MenuAction )---
	static class MenuAction implements ActionListener, MenuKeyListener
	{
		private int cmd;

		MenuAction(int cmd)
		{
			this.cmd = cmd;
		}

		public void actionPerformed(ActionEvent e)
		{
			sendEvent(cmd);
		}

		public void menuKeyPressed(MenuKeyEvent e) { }

		public void menuKeyReleased(MenuKeyEvent e) { }

		public void menuKeyTyped(MenuKeyEvent e) { }
	}

	// ---( Inner Class JTModel )---
	class JTModel implements TreeModel
	{
		private Vector listeners;
		private CCFTreeRoot root;

		JTModel()
		{
			listeners = new Vector();
			root = new CCFTreeRoot();
		}

		public void refresh()
		{
			root.refresh();
		}

		public void fireTreeChanged(TreeModelEvent te)
		{
			for (Enumeration e = listeners.elements(); e.hasMoreElements(); )
			{
				TreeModelListener tm = (TreeModelListener)e.nextElement();
				tm.treeStructureChanged(te);
			}
		}

		// ---( TreeModel interface methods )---
		public void addTreeModelListener(TreeModelListener l)
		{
			listeners.addElement(l);
		}

		public Object getChild(Object parent, int index)
		{
			if (parent instanceof TreeFolder)
			{
				return ((TreeFolder)parent).getChild(index);
			}
			else
			{
				return null;
			}
		}

		public int getChildCount(Object parent)
		{
			if (parent instanceof TreeFolder)
			{
				return ((TreeFolder)parent).getChildCount();
			}
			else
			{
				return 0;
			}
		}

		public int getIndexOfChild(Object parent, Object child)
		{
			if (parent instanceof TreeFolder)
			{
				return ((TreeFolder)parent).getIndexOf(child);
			}
			else
			{
				return -1;
			}
		}

		public Object getRoot()
		{
			return root;
		}

		public boolean isLeaf(Object node) {
			return !(node instanceof TreeFolder);
		}

		public void removeTreeModelListener(TreeModelListener l)
		{
			listeners.removeElement(l);
		}

		public void valueForPathChanged(TreePath path, Object newValue)
		{
			debug.log(2,"valueForPathChanged: "+path+" "+newValue);
		}
	}

	// -------------------------------------------------------------------------------------
	// TREE CLASSES
	// -------------------------------------------------------------------------------------

	// ---( Inner Class TreeNode )---
	abstract class TreeNode implements Parental
	{
		private TreeNode parent;

		TreeNode(TreeNode parent)
		{
			this.parent = parent;
		}

		public Object getMyParent()
		{
			return getParent();
		}

		public void setParent(TreeNode parent)
		{
			this.parent = parent;
		}

		public TreeNode getParent()
		{
			return parent;
		}

		public abstract void refresh()
			;

		public TreePath getTreePath()
		{
			Vector path = new Vector();
			TreeNode node = this;
			while (node != null)
			{
				path.add(node);
				node = node.parent;
			}
			Object o[] = new Object[path.size()];
			for (int i=0; i<o.length; i++)
			{
				o[i] = path.elementAt(o.length-i-1);
			}
			return new TreePath(o);
		}
	}

	// ---( Inner Class TreeFolder )---
	abstract class TreeFolder extends TreeNode
	{
		private Object contents[];
		private Hashtable cache;

		TreeFolder(TreeFolder parent)
		{
			this(parent, null);
		}

		TreeFolder(TreeFolder parent, Object contents[])
		{
			super(parent);
			this.contents = contents;
			this.cache = new Hashtable();
		}

		public abstract void refresh()
			;

		// forces a tree redraw from this node down if fireEvent true
		public void setContents(Object c[])
		{
			setContents(c, false);
		}

		public void setContents(Object c[], boolean fireEvent)
		{
			this.contents = c;
			this.cache = new Hashtable();
			if (fireEvent)
			{
				fireRereadContents();
			}
		}

		public void fireRereadContents()
		{
			Vector v = new Vector();
			TreeNode f = this;
			while (f != null)
			{
				v.addElement(f);
				f = f.getParent();
			}
			Object o[] = new Object[v.size()];
			for (int i=0; i<o.length; i++)
			{
				o[i] = v.elementAt(o.length-i-1);
			}
			model.fireTreeChanged(new TreeModelEvent(this, o));
		}

		public int getChildCount()
		{
			return contents != null ? contents.length : 0;
		}

		public Object getChild(int index)
		{
			return getCachedReplacement(contents[index]);
		}

		public int getIndexOf(Object child)
		{
			for (int i=0; i<contents.length; i++)
			{
				if (getChild(i).equals(child))
				{
					return i;
				}
			}
			return -1;
		}

		// override in subclass to wrap return
		public Object getReplacement(Object o)
		{
			return o;
		}

		public Object getCachedReplacement(Object o)
		{
			Object r = cache.get(o);
			if (r != null)
			{
				return r;
			}
			else
			{
				r = getReplacement(o);
				cache.put(o, r);
			}
			return r;
		}
	}

	// ---( Inner Class CCFTreeRoot )---
	class CCFTreeRoot extends TreeFolder
	{
		CCFTreeSystemFolder sys = new CCFTreeSystemFolder(this);
		CCFTreeDeviceFolder home = new CCFTreeDeviceFolder(1, this, "Home");
		CCFTreeDeviceFolder devices = new CCFTreeDeviceFolder(2, this, "Devices");
		CCFTreeDeviceFolder macros = new CCFTreeDeviceFolder(3, this, "Macros");

		CCFTreeRoot()
		{
			super(null);
			setContents(new Object[] { sys, home, devices, macros });
		}

		public void refresh()
		{
			if (ccf != null)
			{
				sys.refresh();
				/*
				home.setRootDevice(ccf.getFirstHomeDevice());
				devices.setRootDevice(ccf.getFirstDevice());
				macros.setRootDevice(ccf.getFirstMacroDevice());
				*/
				home.refresh();
				devices.refresh();
				macros.refresh();
				//tree.expandPath(new TreePath(new Object[] { this, sys }));
				tree.expandPath(new TreePath(new Object[] { this, home }));
				tree.expandPath(new TreePath(new Object[] { this, devices }));
				tree.expandPath(new TreePath(new Object[] { this, macros }));
			}
		}
	}

	// ---( Inner Class CCFTreeDeviceFolder )---
	class CCFTreeDeviceFolder extends TreeFolder implements Pastable
	{
		private String name;
		private int root;

		CCFTreeDeviceFolder(int root, TreeFolder parent, String name)
		{
			super(parent);
			this.root = root;
			this.name = name;
		}

		public CCFDevice getRootDevice()
		{
			switch (root)
			{
				case 1: return ccf.getFirstHomeDevice();
				case 2: return ccf.getFirstDevice();
				case 3: return ccf.getFirstMacroDevice();
			}
			return null;
		}

		public void setRootDevice(CCFDevice dev)
		{
			switch (root)
			{
				case 1: ccf.setFirstHomeDevice(dev); break;
				case 2: ccf.setFirstDevice(dev); break;
				case 3: ccf.setFirstMacroDevice(dev); break;
			}
			Vector v = new Vector();
			CCFDevice d = dev;
			while (d != null)
			{
				v.addElement(d);
				d = d.next;
			}
			setContents((CCFDevice[])v.toArray(new CCFDevice[v.size()]), true);
		}

		public void newDevice()
		{
			CCFDevice nd = ccf.createDevice();
			nd.setName("NewDevice");
			pushDo(new DoAddDevice(nd));
		}

		void addDevice(CCFDevice nd)
		{
			CCFDevice dev = getRootDevice();
			if (dev == null)
			{
				setRootDevice(nd);
			}
			else
			{
				dev.appendDevice(nd);
			}
			refresh();
		}

		public void refresh()
		{
			Vector visible = new Vector();
			int n = getChildCount();
			for (int i=0; i<n; i++)
			{
				TreeNode tn = (TreeNode)getChild(i);
				TreePath tp = tn.getTreePath();
				int row = tree.getRowForPath(tp);
				if (tree.isExpanded(row))
				{
					visible.add(tp);
				}
			}
			setRootDevice(getRootDevice());
			for (Enumeration e = visible.elements(); e.hasMoreElements(); )
			{
				tree.expandPath((TreePath)e.nextElement());
			}
		}

		public boolean acceptPaste(Object o)
		{
			return (o instanceof CCFDevice || o instanceof CCFTreeDevice);
		}

		public void paste(Object o)
		{
			if (o instanceof CCFDevice)
			{
				pushDo(new DoAddDevice((CCFDevice)o));
			}
			else
			if (o instanceof CCFTreeDevice)
			{
				pushDo(new DoAddDevice(((CCFTreeDevice)o).getDevice()));
			}
		}

		// ---( Inner Class DoAddDevice )---
		class DoAddDevice implements Doable
		{
			private CCFDevice device;

			DoAddDevice(CCFDevice device)
			{
				this.device = device;
			}

			public void doIt()
			{
				addDevice(device);
			}

			public void undoIt()
			{
				device.delete();
				refresh();
			}
		}

		public Object getReplacement(Object o)
		{
			if (o instanceof CCFDevice)
			{
				CCFTreeDevice td = (CCFTreeDevice)wrappers.get(o);
				if (td == null)
				{
					td = new CCFTreeDevice(this, (CCFDevice)o);
					wrappers.put(o, td);
				}
				else
				{
					td.setParent(this);
				}
				return td;
			}
			else
			{
				return o;
			}	
		}

		public String toString()
		{
			return name;
		}
	}

	// ---( Inner Class CCFTreeDevice )---
	class CCFTreeDevice
		extends TreeFolder
		implements Configurable, Deletable, Pastable, Copyable, Namable
	{
		private CCFDevice dev;
		private CCFTreeDeviceFolder folder;

		CCFTreeDevice(CCFTreeDeviceFolder parent, CCFDevice dev)
		{
			super(parent);
			this.folder = parent;
			this.dev = dev;
			readContents(false);
		}

		private CCFDevice getDevice()
		{
			return dev;
		}

		public void refresh()
		{
			readContents();
		}

		private void readContents()
		{
			readContents(true);
		}

		private void readContents(boolean update)
		{
			Vector v = new Vector();
			if (prefShowDeviceProps)
			{
				v.addElement(new CCFTreeDeviceProperty());
			}
			CCFPanel d = dev.getFirstPanel();
			while (d != null)
			{
				v.addElement(d);
				d = d.getNextPanel();
			}
			setContents(v.toArray(new Object[v.size()]), update);
		}

		public String getName()
		{
			return dev.getName();
		}

		public void setName(String name)
		{
			dev.setName(name);
			updateTreeSelection();
		}

		public void editProperties()
		{
			deviceProps.updateDevice(dev);
		}

		public void newPanel()
		{
			pushDo(new DoAddPanel(this, dev, dev.createPanel("NewPanel"), true));
		}

		public Object copy()
		{
			return dev.getClone();
		}

		public Object getReplacement(Object o)
		{
			if (o instanceof CCFPanel)
			{
				CCFTreePanel tp = (CCFTreePanel)wrappers.get(o);
				if (tp == null)
				{
					tp = new CCFTreePanel(this, (CCFPanel)o);
					wrappers.put(o, tp);
				}
				else
				{
					tp.setParent(this);
				}
				return tp;
			}
			else
			{
				return o;
			}	
		}

		public void delete()
		{
			pushDo(new MultiDo(new Doable[] {
				new DoDeleteDevice(dev, folder),
				//new DoRemoveDependencies(ccf, dev),
			}));
		}

		public boolean acceptPaste(Object o)
		{
			return (o instanceof CCFPanel) || (o instanceof CCFTreePanel);
		}

		public void paste(Object o)
		{
			if (o instanceof CCFPanel)
			{
				pushDo(new DoAddPanel(this, dev, (CCFPanel)o, false));
			}
			else
			if (o instanceof CCFTreePanel)
			{
				pushDo(new DoAddPanel(this, dev, ((CCFTreePanel)o).getPanel(), false));
			}
		}

		public String toString()
		{
			return dev.name;
		}
	}

	// ---( Inner Class CCFTreeSystemProperty )---
	class CCFTreeSystemProperty implements TreeProperties
	{
		public String toString()
		{
			return "Properties";
		}
	}

	// ---( Inner Class CCFTreeDeviceProperty )---
	class CCFTreeDeviceProperty implements TreeProperties
	{
		public String toString()
		{
			return "Properties";
		}
	}

	// ---( Inner Class CCFTreePanel )---
	class CCFTreePanel extends TreeNode
		implements Deletable, ButtonHost, FrameHost, Configurable, Copyable, Namable, Pastable
	{
		private CCFPanel panel;

		CCFTreePanel(CCFTreeSystemFolder folder, CCFPanel panel)
		{
			super(folder);
			this.panel = panel;
		}

		CCFTreePanel(CCFTreeDevice folder, CCFPanel panel)
		{
			super(folder);
			this.panel = panel;
		}

		boolean isPanelShowing()
		{
			return Tonto.this.isPanelShowing(panel);
		}

		void setPanel(CCFPanel panel)
		{
			this.panel = panel;
			refresh();
		}

		CCFPanel getPanel()
		{
			return panel;
		}

		public void refresh()
		{
			((TreeFolder)getParent()).fireRereadContents();
			tree.setSelectionPath(getTreePath());
		}

		public String getName()
		{
			return panel.getName();
		}

		public void setName(String name)
		{
			panel.setName(name);
			DeskPanel dp = (DeskPanel)panels.get(panel);
			if (dp != null)
			{
				dp.updateTitle();
			}
			refresh();
			setTreeSelection(getTreePath());
		}

		public void addButton()
		{
			addNewButton(panel);
		}

		public Object copy()
		{
			return panel.getClone();
		}

		public void addFrame()
		{
			addNewFrame(panel);
		}

		public void delete()
		{
			if (panel != null && panel.isTemplate())
			{
				return;
			}
			pushDo(new MultiDo(new Doable[] {
				new DoDeletePanel(panel),
				//new DoRemoveDependencies(ccf, panel),
			}));
		}

		public boolean acceptPaste(Object o)
		{
			return (o instanceof ButtonBox || o instanceof FrameBox);
		}

		public void paste(Object o)
		{
			if (o instanceof ButtonBox || o instanceof FrameBox)
			{
				DoNodeUpdate nu = new DoNodeUpdate(panel);
				CCFChild c = ((ChildPanel)o).getSource();
				panel.addChild(c);
				icons.addChild(c);
				nu.getNewState();
				pushDo(nu);
				setDragSelectionWithFocus(getChildPanel(c));
			}
		}

		public void editProperties()
		{
			panelProps.updatePanel(panel);
		}

		public String toString()
		{
			return panel.toString();
		}
	}

	// ---( Inner Class CCFTreeSystemFolder )---
	class CCFTreeSystemFolder extends TreeFolder implements Configurable
	{
		CCFTreeSystemFolder(TreeFolder parent)
		{
			super(parent, null);
		}

		public void refresh()
		{
			if (custom())
			{
				setContents(new Object[] {
					new CCFTreeSystemProperty(),
					new CCFTreePanel(this, ccf.getMasterTemplate()),
					new CCFTreePanel(this, ccf.getDeviceTemplate()),
					new CCFTreePanel(this, ccf.getMacroTemplate()),
					}, true);
			}
			else
			{
				setContents(new Object[] {
					new CCFTreeSystemProperty(), new CCFTreePanel(this, ccf.getMacroPanel()) },
					true);
			}
		}

		public void editProperties()
		{
			editSystemProperties();
		}

		public String toString()
		{
			return "System";
		}
	}

	// -------------------------------------------------------------------------------------
	// COMPONENT CLASSES
	// -------------------------------------------------------------------------------------

	// ---( Inner Class TextRCMenu )---
	static class TextRCMenu implements MouseListener, ActionListener
	{
		private JPopupMenu pop = new JPopupMenu();
		private JMenuItem cut = new JMenuItem("Cut");
		private JMenuItem copy = new JMenuItem("Copy");
		private JMenuItem paste = new JMenuItem("Paste");
		private JMenuItem selall = new JMenuItem("Select All");
		private JTextComponent text;

		TextRCMenu(JTextComponent tc)
		{
			pop.add(cut);
			pop.add(copy);
			pop.add(paste);
			pop.addSeparator();
			pop.add(selall);
			cut.addActionListener(this);
			copy.addActionListener(this);
			paste.addActionListener(this);
			selall.addActionListener(this);
			text = tc;
		}

		public void actionPerformed(ActionEvent ae)
		{
			if (ae.getSource() == cut)
			{
				text.cut();
			}
			else
			if (ae.getSource() == copy)
			{
				text.copy();
			}
			else
			if (ae.getSource() == paste)
			{
				text.paste();
			}
			else
			if (ae.getSource() == selall)
			{
				text.selectAll();
			}
		}

		public void mouseEntered(MouseEvent me) { }

		public void mouseExited(MouseEvent me) { }

		public void mousePressed(MouseEvent me) { }

		public void mouseReleased(MouseEvent me) { }

		public void mouseClicked(MouseEvent me) {
			if (isRightClick(me))
			{
				Point p = me.getPoint();
				pop.show((Component)me.getSource(), p.x, p.y);
			}
		}
	}

	// ---( Inner Class MyIconButton )---
	class MyIconButton extends IconButton
	{
		private StackedDialog dialog;
		private int width, height;

		MyIconButton(StackedDialog dialog)
		{
			this(dialog, 100, 100);
		}

		MyIconButton(StackedDialog dialog, int w, int h)
		{
			this.dialog = dialog;
			this.width = w;
			this.height = h;
		}

		public Dimension getMinimumSize()
		{
			return max(super.getMinimumSize(), width, height);
		}

		public Dimension getPreferredSize()
		{
			return max(super.getPreferredSize(), width, height);
		}

		public Dimension getMaximumSize()
		{
			return max(super.getMaximumSize(), width, height);
		}

		public void actionPerformed(ActionEvent ae)
		{
			super.actionPerformed(ae);
			dialog.repack();
		}
	}

	// ---( Inner Class MyLabel )---
	class MyLabel extends JLabel implements ChangeListener
	{
		Dimension d;

		MyLabel(String s)
		{
			super(s);
			setBorder(new BevelBorder(BevelBorder.LOWERED));
		}

		public Dimension getMinimumSize()
		{
			return getPreferredSize();
		}

		public void stateChanged(ChangeEvent ce)
		{
			setText(Integer.toString(((JSlider)ce.getSource()).getValue()));
		}

		public Dimension getPreferredSize()
		{
			if (d == null)
			{
				d = super.getPreferredSize();
			}
			return d;
		}
	}

	// ---( Inner Class MyTextArea )---
	// contains a workaround for a bug in Mac OSX cut/copy/paste
	class MyTextArea extends JTextArea
	{
		MyTextArea(int x, int y)
		{
			super(x,y);
			if (Util.onMacintosh()) {
			addKeyListener(new KeyAdapter() {
				 public void keyReleased(KeyEvent e) {
					if (e.getModifiers()==Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) {
						if (e.getKeyCode()==KeyEvent.VK_X){
							((JTextArea)e.getSource()).cut();
						} else
						if (e.getKeyCode()==KeyEvent.VK_C) {
							((JTextArea)e.getSource()).copy();
						} else
						if (e.getKeyCode()==KeyEvent.VK_V) {
							((JTextArea)e.getSource()).paste();
						} else
						if (e.getKeyCode()==KeyEvent.VK_A) {
							((JTextArea)e.getSource()).selectAll();
						}
					} 
				}
			});
			}
		}
	}

	// ---( Inner Class ColorChooser )---
	class ColorChooser extends JPanel
	{
		private ColorPicker fg = new ColorPicker();
		private ColorPicker bg = new ColorPicker();
		private JLabel lfg = new JLabel("Text", JLabel.CENTER);
		private JLabel lbg = new JLabel("Background", JLabel.CENTER);
		private boolean hasColor;

		ColorChooser()
		{
			this.setLayout(new GridLayout(2,2,3,2));
			this.add(lfg);
			this.add(lbg);
			this.add(fg);
			this.add(bg);
		}

		public void setWebSafe(boolean ws)
		{
			fg.setWebSafe(ws);
			bg.setWebSafe(ws);
		}

		public CCFColor getFG()
		{
			return fg.getColor();
		}
		
		public CCFColor getBG()
		{
			return bg.getColor();
		}
		
		public void setColors(int c)
		{
			boolean inColor = color();
			fg.setColor(CCFColor.getForeground(c, inColor), inColor);
			bg.setColor(CCFColor.getBackground(c, inColor), inColor);
		}

		public int getColors()
		{
			return CCFColor.getComposite(fg.getColor(), bg.getColor(), color());
		}
	}

	// ---( Inner Class IconButton )---
	class IconButton extends JButton implements ActionListener
	{
		private CCFIcon icon, oldIcon;
		private Icon cached;

		IconButton()
		{
			addActionListener(this);
		}

		IconButton(CCFIcon icon)
		{
			initButton(icon);
		}

		public void initButton(CCFIcon icon)
		{
			setCCFIcon(icon);
		}

		public CCFIcon getCCFIcon()
		{
			return icon;
		}

		public void setCCFIcon(CCFIcon icon)
		{
			this.icon = icon;
			if (icon == null)
			{
				setIcon(null);
			}
			else
			{
				cached = icon.getIcon(this);
				setIcon(cached);
			}
		}

		public Dimension getMinimumSize()
		{
			Dimension d = super.getMinimumSize();
			return new Dimension(
				Math.max(50, d.width), Math.max(30, d.height)
			);
		}

		public Dimension getPreferredSize()
		{
			return getMinimumSize();
		}

		public Dimension getMaximumSize()
		{
			return getMinimumSize();
		}

		public void revert()
		{
			setCCFIcon(oldIcon);
		}

		private boolean fit(Dimension smaller, Dimension larger)
		{
			return (smaller.width <= larger.width && smaller.height <= larger.height);
		}

		private Dimension iconSize;

		public void paint(Graphics g)
		{
			if (icon != null && cached != icon.getIcon(this))
			{
				cached = icon.getIcon(this);
				setIcon(cached);
			}

			super.paint(g);
		}

		// ---( ActionListener interface methods )---
		public void actionPerformed(ActionEvent ae)
		{
			if (icons.invoke(icon))
			{
				setCCFIcon(icons.getSelected());
			}
		}
	}

	// ---( Inner Class ActionPanel )---
	class ActionPanel extends JPanel
		implements ActionListener, MouseListener, MouseMotionListener, KeyListener
	{
		private JList list;
		private AAPanel left;
		private Vector listeners;
		private Vector actions = new Vector();
		private CCFActionList actionList;
		private NameAttr name;

		private boolean inDrag;
		private int dropStart;
		private int dropEnd;

		private JButton b_alias;
		private JButton b_delay;
		private JButton b_ir;
		private JButton b_beep;
		private JButton b_timer;
		private JButton b_jump;
		private JButton b_delete;
		private JButton b_clear;
		private JButton b_edit;
		private JButton b_cut;
		private JButton b_copy;
		private JButton b_paste;
		private JButton b_selall;

		class X extends JPanel {
			private Dimension d = new Dimension(20,20);
			public Dimension getMinimumSize()   { return d; }
			public Dimension getPreferredSize() { return d; }
			public Dimension getMaximumSize()   { return d; }
		}

		// ---( constructors )---
		ActionPanel()
		{
			this(false);
		}

		ActionPanel(boolean showName)
		{
			name = new NameAttr();
			listeners = new Vector();

			setLayout(new BorderLayout(5,5));

			left = new AAPanel();

			JPanel tleft = new JPanel();
			JPanel mleft = new JPanel();
			JPanel bleft = new JPanel();
			tleft.setLayout(new GridLayout(6,1,2,2));
			mleft.setLayout(new GridLayout(4,1,2,2));
			bleft.setLayout(new GridLayout(3,1,2,2));

			left.add(tleft,   "x=0;y=0;wx=1;wy=0;fill=b;pad=2,2,2,2;anchor=n");
			left.add(new X(), "x=0;y=1;wx=1;wy=1;fill=b;pad=2,2,2,2;anchor=n");
			left.add(mleft,   "x=0;y=2;wx=1;wy=0;fill=b;pad=2,2,2,2;anchor=c");
			left.add(new X(), "x=0;y=3;wx=1;wy=1;fill=b;pad=2,2,2,2;anchor=n");
			left.add(bleft,   "x=0;y=4;wx=1;wy=0;fill=b;pad=2,2,2,2;anchor=n");

			b_alias = newButton("Alias", this, KeyEvent.VK_L);
			b_delay = newButton("Delay", this, KeyEvent.VK_D);
			b_ir = newButton("IR", this, KeyEvent.VK_I);
			b_beep = newButton("Beep", this, KeyEvent.VK_B);
			b_timer = newButton("Timer", this, KeyEvent.VK_T);
			b_jump = newButton("Jump", this, KeyEvent.VK_J);

			b_cut = newButton("Cut", this, KeyEvent.VK_X);
			b_copy = newButton("Copy", this, KeyEvent.VK_C);
			b_paste = newButton("Paste", this, KeyEvent.VK_V);
			b_delete = newButton("Delete", this, KeyEvent.VK_E);

			b_edit = newButton("Edit", this, -1);
			b_selall = newButton("Select All", this, KeyEvent.VK_A);
			b_clear = newButton("Delete All", this, KeyEvent.VK_R);

			tleft.add(b_alias);
			tleft.add(b_delay);
			tleft.add(b_ir);
			tleft.add(b_beep);
			tleft.add(b_timer);
			tleft.add(b_jump);

			mleft.add(b_cut);
			mleft.add(b_copy);
			mleft.add(b_paste);
			mleft.add(b_delete);

			bleft.add(b_edit);
			bleft.add(b_selall);
			bleft.add(b_clear);

			b_edit.setEnabled(false);
			b_delete.setEnabled(false);
			b_cut.setEnabled(false);
			b_copy.setEnabled(false);
			b_paste.setEnabled(false);
			b_selall.setEnabled(false);

			list = new JList(new ListModel() {
				// ---( ListModel interface methods )---
				public void addListDataListener(ListDataListener l) {
					listeners.addElement(l);
				}
				public Object getElementAt(int index) {
					return (index >= 0 && index < actions.size()) ? actions.elementAt(index) : null;
				}
				public int getSize() {
					b_clear.setEnabled(actions.size() > 0);
					return actions.size();
				}
				public void removeListDataListener(ListDataListener l) {
					listeners.removeElement(l);
				}
			});
			list.setBorder(new BevelBorder(BevelBorder.LOWERED));
			list.setFont(getPFont());
			list.addMouseListener(this);
			list.addMouseMotionListener(this);
			list.setCellRenderer(new DefaultListCellRenderer() {
				public Component getListCellRendererComponent(
					JList list, Object val, int idx, boolean sel, boolean foc)
				{
					JComponent c = (JComponent)super.getListCellRendererComponent(list, val, idx, sel, foc);
					if (inDrag && sel)
					{
						c.setBorder(border);
					}
					return c;
				}
			});
			list.addKeyListener(ActionPanel.this);
			list.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					updateOptions();
				}
			});
			add("West", left);
			add("Center", new JScrollPane(list,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
			if (showName)
			{
				add("South", name);
			}
		}

		// ---( KeyListener interface methods )---
		public void keyTyped(KeyEvent ke) { }
		public void keyReleased(KeyEvent ke) { }

		public void keyPressed(KeyEvent ke)
		{
			if (ke.getKeyCode() == KeyEvent.VK_DELETE) { deleteItems(); } else
			if (ke.getKeyCode() == KeyEvent.VK_A && ke.isControlDown()) { selectAll(); } else
			if (ke.getKeyCode() == KeyEvent.VK_X && ke.isControlDown()) { cut(); } else
			if (ke.getKeyCode() == KeyEvent.VK_C && ke.isControlDown()) { copy(); } else
			if (ke.getKeyCode() == KeyEvent.VK_V && ke.isControlDown()) { paste(); }
		}

		public void selectAll()
		{
			list.setSelectionInterval(0,actions.size()-1);
		}

		public void cut()
		{
			copy();
			deleteItems();
		}

		public void copy()
		{
			Object o[] = list.getSelectedValues();
			if (o != null)
			{
				CCFAction a[] = new CCFAction[o.length];
				for (int i=0; i<a.length; i++)
				{
					a[i] = (CCFAction)((CCFAction)o[i]).getClone();
				}
				setClipboard(a);
			}
			updateOptions();
		}

		public void paste()
		{
			if ( !(clipboard instanceof CCFAction || clipboard instanceof CCFAction[]) )
			{
				return;
			}
			if (clipboard instanceof CCFAction[])
			{
				CCFAction c[] = (CCFAction[])clipboard;
				for (int i=0; i<c.length; i++)
				{
					if (c[i] instanceof CCFAction)
					{
						appendAction((CCFAction)((CCFAction)c[i]).getClone(), false);
					}
				}
			}
			else
			{
				appendAction((CCFAction)((CCFAction)clipboard).getClone(), false);
			}
		}

		// ---( MouseListener interface methods )---
		public void mouseClicked(MouseEvent e)
		{
			if (e.getClickCount() == 2)
			{
				Object o = list.getSelectedValue();
				if (o instanceof CCFAction)
				{
					editAction((CCFAction)o);
				}
			}
		}			
		public void mouseEntered(MouseEvent e) { }
		public void mouseExited(MouseEvent e) { }
		public void mousePressed(MouseEvent e) { }
		public void mouseReleased(MouseEvent e)
		{
			inDrag = false;
		}

		// ---( MouseMotionListener interface methods )---
		public void mouseDragged(MouseEvent m)
		{
			if (!inDrag)
			{
				inDrag = true;
				dropStart = list.getSelectedIndex();
			}
			else
			{
				dropEnd = list.getSelectedIndex();
				if (dropEnd >= 0 && Math.abs(dropEnd-dropStart) == 1)
				{
					Object v = actions.elementAt(dropEnd);
					actions.setElementAt(actions.elementAt(dropStart), dropEnd);
					actions.setElementAt(v, dropStart);
					dropStart = dropEnd;
					sendDataEvent(actions.size(), actions.size());
				}
			}
		}
		public void mouseMoved(MouseEvent m) { }

		// ---( ActionListener interface methods )---
		public void actionPerformed(ActionEvent ae)
		{
			Object src = ae.getSource();
			String cmd = ae.getActionCommand();
			if (src == b_cut) { cut(); } else
			if (src == b_copy) { copy(); } else
			if (src == b_paste) { paste(); } else
			if (src == b_selall) { selectAll(); } else
			if (cmd.equals("Delete"))
			{
				deleteItems();
			}
			else
			if (cmd.equals("IR"))
			{
				appendAction(new ActionIRCode(new CCFIRCode(ccf.header())));
			}
			else
			if (cmd.equals("Delay"))
			{
				appendAction(new ActionDelay(100));
			}
			else
			if (cmd.equals("Beep"))
			{
				appendAction(new ActionBeep(300,5000,90));
			}
			else
			if (cmd.equals("Alias"))
			{
				appendAction(new CCFAction(CCFAction.ACT_ALIAS_DEV,null,null));
			}
			else
			if (cmd.equals("Jump"))
			{
				appendAction(new ActionJumpPanel((CCFPanel)null, header().isNewMarantz()));
			}
			else
			if (cmd.equals("Timer"))
			{
				appendAction(new ActionTimer(new CCFTimer()));
			}
			else
			if (cmd.equals("Delete All"))
			{
				if (confirmDialog("Delete All Actions", "Are you sure you want to delete all actions?"))
				{
					clearActions();
				}
			}
			else
			if (cmd.equals("Edit"))
			{
				editAction((CCFAction)list.getSelectedValue());
				sendDataEvent(actions.size(), actions.size());
			}
		}

		// ---( instance methods )---
		public String getName()
		{
			return name.getName();
		}

		public void update(CCFActionList newlist)
		{
			update(newlist, null);
		}

		public void update(CCFActionList newlist, String keyname)
		{
			if (newlist == null)
			{
				newlist = new CCFActionList();
			}
			name.setName(keyname);
			if (ccf.header().isMarantz())
			{
				b_beep.setEnabled(false);
				b_timer.setEnabled(false);
			}
			else
			{
				b_beep.setEnabled(true);
				b_timer.setEnabled(true);
			}
			this.actionList = newlist;
			int olen = actions.size();
			if (newlist != null)
			{
				actions = new Vector();
				CCFAction a[] = newlist.getActions();
				if (a != null)
				{
					for (int i=0; i<a.length; i++)
					{
						actions.addElement(a[i]);
					}
				}
			}
			int nlen = actions.size();
			sendDataEvent(olen, nlen);
			updateOptions();
		}

		public CCFActionList save()
		{
			CCFAction a[] = new CCFAction[actions.size()];
			actions.copyInto(a);
			actionList.setActions(a);
			if (a.length > 0)
			{
				return actionList;
			}
			else
			{
				return null;
			}
		}

		private boolean hasJump()
		{
			for (int i=0; i<actions.size(); i++)
			{
				if (actions.get(i) instanceof ActionJumpPanel)
				{
					return true;
				}
			}
			return false;
		}

		private void updateOptions()
		{
			b_cut.setEnabled(list.getSelectedValue() != null);
			b_copy.setEnabled(list.getSelectedValue() != null);
			b_paste.setEnabled(clipboard instanceof CCFAction || clipboard instanceof CCFAction[]);
			b_selall.setEnabled(actions.size() > 1);
			b_edit.setEnabled(list.getSelectedValue() != null && list.getSelectedValues().length == 1);
			b_delete.setEnabled(list.getSelectedValue() != null);
			b_jump.setEnabled(ccf.header().isMarantz() || custom() || !hasJump());
		}

		private void deleteItems()
		{
			int oz = actions.size();
			int item = list.getSelectedIndex();
			Object o[] = list.getSelectedValues();
			for (int i=0; o != null && i<o.length; i++)
			{
				actions.remove(o[i]);
			}
			int nz = actions.size();
			sendDataEvent(oz, nz);
			list.setSelectedIndex(Math.max(item-1,0));
		}

		public void clearActions()
		{
			int sz = actions.size();
			actions.setSize(0);
			sendDataEvent(0, sz);
		}

		public void appendAction(CCFAction action)
		{
			appendAction(action, true);
		}

		public void appendAction(CCFAction action, boolean edit)
		{
			if (!edit || editAction(action))
			{
				actions.addElement(action);
				sendDataEvent(actions.size()-1, actions.size());
				list.setSelectedIndex(actions.size()-1);
			}
		}

		private void sendDataEvent(int olen, int nlen)
		{
			for (Enumeration e = listeners.elements(); e.hasMoreElements(); )
			{
				ListDataListener l = (ListDataListener)e.nextElement();
				l.intervalRemoved(
					new ListDataEvent(
						this, ListDataEvent.INTERVAL_REMOVED, 0, olen)
				);
				l.contentsChanged(
					new ListDataEvent(
						this, ListDataEvent.CONTENTS_CHANGED, 0, nlen)
				);
			}
		}

		public boolean editAction(CCFAction act)
		{
			StringValueDialog sd = null;
			switch (act.type)
			{
				case CCFAction.ACT_BEEP:
					BeepValueDialog bd = new BeepValueDialog((ActionBeep)act);
					if (bd.invoke())
					{
						act.p2 = bd.getBeepValue();
						return true;
					}
					return false;
				case CCFAction.ACT_DELAY:
					sd = new StringValueDialog("Delay", Integer.toString(act.p2), 0);
					if (sd.invoke())
					{
						act.p2 = Integer.parseInt(sd.getValue());
						return true;
					}
					return false;
				case CCFAction.ACT_IRCODE:
					CCFIRCode ir = (CCFIRCode)act.action2;
					if (ir == null)
					{
						ir = new CCFIRCode(ccf.header());
					}
					IRValueDialog id = new IRValueDialog(ir);
					if (id.invoke())
					{
						return true;
					}
					return false;
				case CCFAction.ACT_JUMP_PANEL:
				case CCFAction.ACT_MARANTZ_JUMP:
					JumpChoice jc = new JumpChoice(act);
					if (jc.invoke())
					{
						return jc.setActionValue(act);
					}
					return false;
				case CCFAction.ACT_ALIAS_BUTTON:
				case CCFAction.ACT_ALIAS_KEY:
				case CCFAction.ACT_ALIAS_DEV:
					return new AliasChoice(act).invoke(actionList);
				case CCFAction.ACT_TIMER:
					if (act instanceof ActionTimer)
					{
						ActionTimer at = (ActionTimer)act;
						if (timerEditor.invoke(at.getTimer()))
						{
							at.setTimer(timerEditor.getTimer());
							return true;
						}
					}
					return false;
			}
			return false;
		}

		public Dimension getMinimumSize()
		{
			return new Dimension(300,350);
		}
	}

	// ---( Inner Class ObjectPanel )---
	class ObjectPanel extends AAPanel implements ActionListener
	{
		private boolean apply;
		private Object object;
		private JLabel flabel = new JLabel("Font");
		private NameAttr name = new NameAttr(this);
		private GeomPanel geom = new GeomPanel(true);
		private JComboBox font = new JComboBox(new String[] {
			"<none>", "pronto 8", "pronto 10", "pronto 12", "pronto 14", "pronto 16", "pronto 18" } );

		ObjectPanel()
		{
			apply = false;
			JPanel pad = new JPanel();
			define('F', flabel, "pad=3,3,2,2;fill=b;wy=1");
			define('f', font,   "pad=3,3,2,2;fill=n;wy=1");
			define('n', name,   "pad=3,3,2,2;fill=n;wy=1");
			define('p', pad,    "pad=3,3,2,2;fill=b;wy=1;wx=1");
			define('g', geom,   "pad=3,3,2,2;fill=n;wy=1");
			setLayout(new String[] { "F f n p g" });
			setObject(null);
			font.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (hasfont()) {
						setFontSize(font.getSelectedIndex());
					}
				}
			});
			name.setFontState(font);
			apply = true;
			setBorder(new BevelBorder(BevelBorder.LOWERED));
		}

		public void setObject(Object object)
		{
			apply = false;
			this.object = object;
			if (object instanceof ChildPanel)
			{
				geom.setChild((ChildPanel)object);
			}
			else
			{
				geom.setChild(null);
			}
			if (object instanceof Namable)
			{
				name.setName(((Namable)object).getName());
			}
			else
			{
				name.setName("   ");
				name.setEnabled(false);
			}
			if (hasfont())
			{
				font.setEnabled(true);
				font.setSelectedIndex(getFontSize());
			}
			else
			{
				font.setEnabled(false);
				font.setSelectedIndex(0);
			}
			keepFocusOnSelection();
			apply = true;
		}

		private void keepFocusOnSelection()
		{
			if (dragSelection != null)
			{
				dragSelection.requestFocus();
			}
		}

		private boolean hasfont()
		{
			return (object instanceof FrameBox || object instanceof ButtonBox);
		}

		private int getFontSize()
		{
			if (object instanceof FrameBox) { return ((FrameBox)object).getFontSize(); }
			if (object instanceof ButtonBox) { return ((ButtonBox)object).getFontSize(); }
			return 0;
		}

		private void setFontSize(int idx)
		{
			if (!apply)
			{
				return;
			}
			int ido = startMultiDo();
			for (int i=0; i<multiSelect.size(); i++)
			{
				Object object = multiSelect.get(i);
				if (object instanceof FrameBox)
				{
					DoNodeUpdate du = new DoNodeUpdate(((FrameBox)object).getFrame());
					((FrameBox)object).setFontSize(idx);
					du.pushDoNewState();
				}
				if (object instanceof ButtonBox)
				{
					DoNodeUpdate du = new DoNodeUpdate(((ButtonBox)object).getButton());
					((ButtonBox)object).setFontSize(idx);
					du.pushDoNewState();
				}
			}
			keepFocusOnSelection();
			endMultiDo(ido);
		}

		public void actionPerformed(ActionEvent ae)
		{
			if (!apply)
			{
				return;
			}
			if (multiSelect.size() > 0)
			{
				int ido = startMultiDo();
				for (int i=0; i<multiSelect.size(); i++)
				{
					Object object = multiSelect.get(i);
					if (object instanceof Namable)
					{
						DoNodeUpdate du = new DoNodeUpdate(((ChildPanel)object).getSource().child);
						((Namable)object).setName(name.getName());
						du.pushDoNewState();
					}
				}
				endMultiDo(ido);
			}
			else
			{
				if (object instanceof Namable)
				{
					((Namable)object).setName(name.getName());
				}
			}
		}

		public void editName()
		{
			if (object instanceof Namable)
			{
				name.click();
			}
		}

		public void update()
		{
			geom.update();
			keepFocusOnSelection();
		}

		public void updateAll()
		{
			setObject(object);
			update();
		}
	}

	// ---( Inner Class GeomPanel )---
	class GeomPanel extends AAPanel implements ActionListener
	{
		private ChildPanel cp;
		private CCFChild child;
		private ActionButton set;
		private JTextField x = new MTextField(); //JTextField(3);
		private JTextField y = new MTextField(); //JTextField(3);
		private JTextField w = new MTextField(); //JTextField(3);
		private JTextField h = new MTextField(); //JTextField(3);

		class MTextField extends JTextField
		{
			private Dimension ps;

			MTextField()
			{
				super(3);
			}

			public Dimension getMinimumSize()
			{
				if (ps == null)
				{
					ps = getPreferredSize();
				}
				return ps;
			}
		}

		GeomPanel()
		{
			this(false);
		}

		GeomPanel(boolean live)
		{
			JLabel lx = new JLabel("X");
			JLabel ly = new JLabel("Y");
			JLabel lw = new JLabel("W");
			JLabel lh = new JLabel("H");
			set = new ActionButton("Set") {
				public void action() {
					commit();	
				}
			};

			define('X', lx,    "pad=2,2,2,2");
			define('Y', ly,    "pad=2,2,2,2");
			define('W', lw,    "pad=2,2,2,2");
			define('H', lh,    "pad=2,2,2,2");
			define('x',  x,    "pad=2,2,2,2;wx=1;fill=b");
			define('y',  y,    "pad=2,2,2,2;wx=1;fill=b");
			define('w',  w,    "pad=2,2,2,2;wx=1;fill=b");
			define('h',  h,    "pad=2,2,2,2;wx=1;fill=b");
			if (live)
			{
			define('s',  set,  "pad=2,2,2,2;wx=1;fill=b");
			}

			setLayout(new String[] { "X x Y y W w H h s" });
			if (!live)
			{
				Util.setLabelBorder("Location/Size", this);
			}

			if (live)
			{
				x.addActionListener(this);
				y.addActionListener(this);
				w.addActionListener(this);
				h.addActionListener(this);
			}
		}

		public void actionPerformed(ActionEvent ae)
		{
			commit();
		}

		public void setChild(ChildPanel cp)
		{
			if (cp == null || !(cp instanceof Movable))
			{
				x.setEnabled(false); x.setText("   ");
				y.setEnabled(false); y.setText("   ");
				w.setEnabled(false); w.setText("   ");
				h.setEnabled(false); h.setText("   ");
				set.setEnabled(false);
				child = null;
				return;
			}
			this.cp = cp;
			child = cp.getSource();
			x.setEnabled(true);
			y.setEnabled(true);
			w.setEnabled(child.isResizable());
			h.setEnabled(child.isResizable());
			set.setEnabled(true);
			update();
		}

		public void update()
		{
			if (child == null)
			{
				return;
			}
			Dimension sz = child.getSize();
			Point p = child.getLocation();
			x.setText(p.x+"");
			y.setText(p.y+"");
			w.setText(sz.width+"");
			h.setText(sz.height+"");
		}

		public void commit()
		{
			int nx = Integer.parseInt(x.getText());
			int ny = Integer.parseInt(y.getText());
			int nw = Integer.parseInt(w.getText());
			int nh = Integer.parseInt(h.getText());
			Rectangle bnd = child.getBounds();
			cp.move(nx-bnd.x, ny-bnd.y, nw-bnd.width, nh-bnd.height, false);
			//pushDo(new DoBounds(child, child.getBounds(), visibleBounds(cp.getParent(), new Rectangle(nx, ny, nw, nh))));
			refreshPanel(child.getParentPanel());
		}
	}

	// ---( Inner Class LabelBox )---
	static class LabelBox extends JPanel
	{
		private JComponent c;

		LabelBox(String label)
		{
			this(label, new JPanel());
		}

		LabelBox(String label, JComponent c)
		{
			setLayout(new GridLayout(1,1));
			setContent(c);
			Util.setLabelBorder(label, this);
		}

		public JComponent getContent()
		{
			return c;
		}

		public void setContent(JComponent nc)
		{
			
			if (c != null)
			{
				remove(c);
			}
			c = nc;
			add(nc);
		}

		public Dimension getMaximumSize()
		{
			Dimension dm = super.getMinimumSize();
			Dimension dx = super.getMaximumSize();
			return new Dimension(dx.width, dm.height);
		}
	}

	// ---( Inner Class NameAttr )---
	class NameAttr extends JPanel implements ActionListener
	{
		private JButton name;
		private JComboBox fontState;
		private ActionListener listener;

		NameAttr(ActionListener listener)
		{
			this((String)null);
			this.listener = listener;
		}

		NameAttr()
		{
			this("Name");
		}

		NameAttr(String label)
		{
			name = UIUtils.getActionButton(label, this);
			name.setFont(getPFont());
			name.setHorizontalAlignment(JButton.LEFT);
			setLayout(new BorderLayout(4,4));
			if (label != null)
			{
				add("Center", new LabelBox(label, name));
			}
			else
			{
				add("West", new JLabel("Name"));
				add("Center", name);
			}
		}

		void setFontState(JComboBox fontState)
		{
			this.fontState = fontState;
		}

		public void actionPerformed(ActionEvent ae)
		{
			if (ae.getSource() == name)
			{
				name.setText(getProntoName(name.getText(), fontState == null ? true : fontState.getSelectedIndex() % 2 == 0));
				setCCFChanged();
				if (listener != null)
				{
					listener.actionPerformed(ae);
				}
			}
		}

		public void click()
		{
			name.doClick();
		}

		public String getName()
		{
			return name.getText();
		}

		public void setName(String nm)
		{
			name.setEnabled(true);
			name.setText(nm != null ? nm : " ");
		}

		public void setEnabled(boolean en)
		{
			name.setEnabled(en);
		}

		private Dimension fixDim(Dimension d)
		{
			d.width = Math.max(d.width, 150);
			return d;
		}

		public Dimension getMinimumSize()
		{
			return fixDim(super.getMinimumSize());
		}

		public Dimension getPreferredSize()
		{
			return fixDim(super.getPreferredSize());
		}
	}

	// ---( Inner Class NameFontAttr )---
	class NameFontAttr extends JPanel implements ActionListener
	{
		private JButton name;
		private JComboBox font;
		private JComboBox align;
		private JCheckBox wrap;
		private int mode = 0;

		NameFontAttr()
		{
			setLayout(new GridLayout(1,2));
			name = UIUtils.getActionButton("foo", this);
			name.setFont(getPFont());
			name.setHorizontalAlignment(JButton.LEFT);
			font = new JComboBox(
				new String[] {
					"<none>",
					"pronto 8", "pronto 10", "pronto 12",
					"pronto 14", "pronto 16", "pronto 18"
				}
			);
			font.setEditable(false);
			align = new JComboBox(
				new String[] {
					"Left", "Center", "Right"
				}
			);
			align.setEditable(false);
			wrap = new JCheckBox("Wrap");
			add(new LabelBox("Name", name));
			add(new LabelBox("Font", font));
		}

		public void actionPerformed(ActionEvent ae)
		{
			if (ae.getSource() == name)
			{
				name.setText(getProntoName(name.getText(), font.getSelectedIndex() % 2 == 0));
				setCCFChanged();
			}
		}

		public String getName()
		{
			return name.getText();
		}

		public void setName(String nm)
		{
			name.setText(nm);
		}

		public CCFFont getCCFFont()
		{
			return CCFFont.getFont(font.getSelectedIndex());
		}

		public void setCCFFont(CCFFont nfont)
		{
			font.setSelectedIndex(nfont.size);
		}

		public int getAlignment()
		{
			switch (align.getSelectedIndex())
			{
				case 0: return CCFNode.TEXT_LEFT;
				case 1: return CCFNode.TEXT_CENTER;
				case 2: return CCFNode.TEXT_RIGHT;
				default: return CCFNode.TEXT_CENTER;
			}
		}

		public void setAlignment(int talign)
		{
			switch (talign)
			{
				case CCFNode.TEXT_LEFT: align.setSelectedIndex(0); break;
				case CCFNode.TEXT_CENTER: align.setSelectedIndex(1); break;
				case CCFNode.TEXT_RIGHT: align.setSelectedIndex(2); break;
			}
		}

		public boolean getWrap()
		{
			return wrap.isSelected();
		}

		public void setWrap(boolean b)
		{
			wrap.setSelected(b);
		}

		public void paint(Graphics g)
		{
			int newmode = custom() ? 1 : 0;
			if (newmode != mode)
			{
				mode = newmode;
				removeAll();
				switch (mode)
				{
					case 0:
						setLayout(new GridLayout(1,2));
						add(new LabelBox("Name", name));
						add(new LabelBox("Font", font));
						break;
					case 1:
						setLayout(new GridLayout(1,3));
						add(new LabelBox("Name", name));
						add(new LabelBox("Font", font));
						JPanel p = new JPanel();
						p.setLayout(new BorderLayout(3,3));
						p.add("Center", align);
						p.add("East", wrap);
						add(new LabelBox("Align", p));
						break;
				}
			}
			super.paint(g);
		}
	}

	// ---( Inner Class TimerEvent )---
	static class TimerEvent extends AAPanel implements ActionListener
	{
		int days;
		int time;
		CCFAction action;

		private JButton chooser = new JButton();
		private JTextField timeHour = new JTextField(2);
		private JTextField timeMin = new JTextField(2);
		private JCheckBox check[] = new JCheckBox[] {
			new JCheckBox("Mon"),
			new JCheckBox("Tue"),
			new JCheckBox("Wed"),
			new JCheckBox("Thu"),
			new JCheckBox("Fri"),
			new JCheckBox("Sat"),
			new JCheckBox("Sun"),
			new JCheckBox("Weekly"),
		};

		TimerEvent()
		{
			chooser.setFont(getPFont());
			chooser.addActionListener(this);
			timeHour.addActionListener(this);
			timeMin.addActionListener(this);

			JPanel checks = new JPanel();
			checks.setLayout(new GridLayout(2,4));
			for (int i=0; i<check.length; i++)
			{
				checks.add(check[i]);
				check[i].addActionListener(this);
			}

			JLabel labAct = new JLabel("Action", JLabel.LEFT);
			JLabel labTime = new JLabel("Time", JLabel.LEFT);
			JLabel col = new JLabel(":", JLabel.CENTER);

			define('1', labAct,    "pad=3,3,3,3;");
			define('2', chooser,   "pad=3,3,3,3;wx=1;fill=b");
			define('3', labTime,   "pad=3,3,3,3;");
			define('4',            "pad=3,3,3,3;wx=1;anchor=nw");
			define('5', timeHour,  "pad=2,2,2,2;");
			define('6', col,       "pad=2,2,2,2;");
			define('7', timeMin,   "pad=2,2,2,2;");
			define('8', checks,    "pad=3,3,3,3;wx=1;wy=1;anchor=nw");

			setLayout(new String[] {
				"                          ",
				" 111 22222222222222222222 ",
				"                          ",
				" 333 44444444444444444444 ",
				" 3 3 4 5 6 7            4 ",
				" 333 44444444444444444444 ",
				"                          ",
				" 888888888888888888888888 ",
				"                          ",
			});
		}

		private String padString(int val)
		{
			if (val < 10) { return " "+val; }
			return Integer.toString(val);
		}

		public void update(int days, int time, CCFAction action)
		{
			chooser.setText(action != null ? action.toString() : "<no action>");
			timeHour.setText(padString(time / 60));
			timeMin.setText(padString(time % 60));
			for (int i=0; i<8; i++)
			{
				int ck = (1 << i);
				check[i].setSelected((days & ck) == ck);
			}

			this.days = days;
			this.time = time;
			this.action = action;
		}

		int getInt(String s)
		{
			try
			{
				return Integer.parseInt(s.trim());
			}
			catch (Exception ex)
			{
				return 0;
			}
		}

		void update()
		{
			days = 0;
			for (int i=0; i<8; i++)
			{
				if (check[i].isSelected())
				{
					days |= (1 << i);
				}
			}
			time = (getInt(timeHour.getText())*60) + getInt(timeMin.getText());
			update(days, time, action);
		}

		public void actionPerformed(ActionEvent ae)
		{
			Object src = ae.getSource();
			if (src == chooser)
			{
				CCFAction act = (action != null ? action : new CCFAction());
				AliasChoice ac = new AliasChoice(act);
				if (ac.invoke())
				{
					action = act;
				}
			}
			update();
		}
	}

	// ---( Inner Class IRGraph )---
	class IRGraph extends JComponent
	{
		private IRSignal code;
		private Dimension size;
		private int signal[];
		private final int inset = 3;

		IRGraph(IRSignal code)
		{
			setSignal(code);
		}
	
		public void setSignal(IRSignal sig)
		{
			code = sig;
			if (code == null)
			{
				size = new Dimension(50,40);
				signal = new int[0];
			}
			else
			{
				int idx[] = code.getPulseIndex().getIndexValues();
				int shortest = -1;
				for (int i=0; i<idx.length; i++)
				{
					if (idx[i] != 0 && (shortest == -1 || (idx[i]) < shortest))
					{
						shortest = idx[i];
					}
				}
				IRBurst rept = code.getRepeat();
				rept.cullRepeats();
				int intro[] = code.getIntro().getPulses();
				int repet[] = rept.getPulses();
				signal = new int[intro.length + repet.length];
				int width = 0;
				int pos = 0;
				for (int i=0; i<intro.length; i++)
				{
					signal[pos] = Math.min(idx[intro[i]]/shortest+2,25);
					width += signal[pos++];
				}
				for (int i=0; i<repet.length; i++)
				{
					signal[pos] = Math.min(idx[repet[i]]/shortest+2,25);
					width += signal[pos++];
				}
				size = new Dimension(width+inset*2, 40);
			}
			repaint();
		}

		public void paint(Graphics g)
		{
			Dimension sz = getSize();
			if (code == null)
			{
				g.setColor(Color.red);
				g.drawString("Not a learned code format",5,sz.height-5);
				return;
			}
			int last = inset;
			int pos = inset;
			int top = inset;
			int bot = sz.height-1-inset;
			for (int i=0; i<signal.length; i++)
			{
				// vertical
				g.drawLine(pos,top,pos,bot);
				pos += signal[i];
				// horizontal
				if (i % 2 == 0)
				{
					g.drawLine(last,top,pos,top);
				}
				else
				{
					g.drawLine(last,bot,pos,bot);
				}
				last = pos;
			}
		}

		public Dimension getMinimumSize()
		{
			return size;
		}

		public Dimension getPreferredSize()
		{
			return size;
		}

		public Dimension getMaximumSize()
		{
			return size;
		}
	}

	// -------------------------------------------------------------------------------------
	// DESKTOP CLASSES
	// -------------------------------------------------------------------------------------

	// ---( Inner Class DeskPanel )---
	class DeskPanel extends JInternalFrame
	{
		private PanelPanel child;

		DeskPanel(PanelPanel child)
		{
			super("panel", false, true, false, true);
			this.child = child;
			Container c = getRootPane().getContentPane();
			c.setLayout(new GridLayout(1,1));
			c.add(child);
			addInternalFrameListener(new InternalFrameAdapter() {
				public void internalFrameActivated(InternalFrameEvent ife) {
					//debug.log(0, "ife: "+ife);
				}
			});
			updateTitle();
			pack();
			show();
		}

		public void updateTitle()
		{
			CCFPanel p = child.getPanel();
			CCFDevice d = p.getParentDevice();
			if (d != null)
			{
				setTitle(d.getName()+" - "+p.getName());
			}
			else
			{
				setTitle("SYS - "+p.getName());
			}
		}

		public PanelPanel getPanel()
		{
			return child;
		}

		public void refresh()
		{
			child.refresh();
			pack();
		}

		public void dispose()
		{
			super.dispose();
			updateMenuState();
			if (child.getPanel() != null && child.getPanel().isTemplate())
			{
				repaintAllPanels();
			}
		}
	}

	// ---( Inner Class ChildPanel )---
	abstract class ChildPanel extends JLayeredPane implements Selectable, Parental
	{
		private CCFChild source;
		private boolean selected;
		private ImageProducer cachedIP;
		private Image image;
		private ImageFontLabel label;
		private int nextID;

		private Point offset;
		private Rectangle bounds, endBounds;
		private int sides;
		private boolean resize;
		private boolean readonly;
		private Stack cursors;

		ChildPanel(int x, int y, int w, int h)
		{
			this(x, y, w, h, null);
		}

		public abstract void refresh(); 

		// TODO: is this a good idea?
		public Image createImage(ImageProducer ip)
		{
			if (ip == cachedIP)
			{
				return image;
			}
			else
			{
				cachedIP = ip;
				image = super.createImage(ip);
			}
			return image;
		}

		public String toString()
		{
			return "ChildPanel:"+Integer.toHexString(hashCode())+":"+source;
		}

		private void testActions(CCFAction a[])
		{
			for (int i=0; a != null && i < a.length; i++)
			{
				try {

				if (a[i] instanceof ActionJumpPanel)
				{
					showDeskPanel(((ActionJumpPanel)a[i]).getPanel());
				}
				else
				if (a[i] instanceof ActionIRCode)
				{
					if (prefEmitIR)
					{
						CCFIRCode ir = ((ActionIRCode)a[i]).getIRCode();
						getComm().testIR(ir.getData());
					}
				}
				else
				/*
				if (a[i] instanceof ActionDelay)
				{
					Thread.currentThread().sleep(((ActionDelay)a[i]).getDelay());
				}
				else
				*/
				if (a[i] instanceof ActionBeep)
				{
					Toolkit.getDefaultToolkit().beep();
				}
				else
				if (a[i] instanceof ActionAliasButton)
				{
					testButton(((ActionAliasButton)a[i]).getButton());
				}
				else
				if (a[i] instanceof ActionAliasDevice)
				{
					testActions(((ActionAliasDevice)a[i]).getDevice().getActions());
				}

				} catch (Exception ex) { debug(ex); }
			}
		}

		private void testButton(CCFButton b)
		{
			testActions(b.getActions());
		}

		private void move(int x, int y, int w, int h, boolean grid) 
		{
			if (dragSelection != null && dragSelection instanceof Movable)
			{
				Rectangle m = (grid ? new Rectangle(
					x * prefGrid.width, y * prefGrid.height,
					w * prefGrid.width, h * prefGrid.height
				) : new Rectangle(x,y,w,h));
				Point p = dragSelection.getLocation();
				p.translate(m.x, m.y);
				Dimension d = dragSelection.getSize();
				d.setSize(d.width+m.width, d.height+m.height);

				Rectangle oldBounds = dragSelection.getBounds();
				Rectangle newBounds = visibleBounds(dragSelection.getParent(), new Rectangle(p.x,p.y,d.width,d.height));
				int dx = newBounds.x - oldBounds.x;
				int dy = newBounds.y - oldBounds.y;
				int dw = newBounds.width - oldBounds.width;
				int dh = newBounds.height - oldBounds.height;

				MultiDo dos = new MultiDo();
				for (int i=0; i<multiSelect.size(); i++)
				{
					ChildPanel cp = (ChildPanel)multiSelect.get(i);
					Rectangle ob = cp.getBounds();
					Rectangle nb;
					if (cp instanceof Resizable && ((Resizable)cp).isResizable())
					{
						nb = new Rectangle(ob.x+dx,ob.y+dy,ob.width+dw,ob.height+dh);
					}
					else
					{
						nb = new Rectangle(ob.x+dx,ob.y+dy,ob.width,ob.height);
					}
					dos.add(new DoBounds(cp, ob, nb));
				}
				pushDo(dos);

				setCCFChanged();
			}
		}

		ChildPanel(int x, int y, int w, int h, CCFChild source)
		{
			setLayout(null);
			this.source = source;
			this.cursors = new Stack();
			this.label = getFontLabel("", JLabel.CENTER, 12);
			setBounds(x, y, w, h);
			addKeyListener(new KeyAdapter()
			{
				public void keyPressed(KeyEvent ke)
				{
					boolean shift = ke.isShiftDown();
					int move = ke.isAltDown() ? 0 : 1;
					int size = ke.isAltDown() ? 1 : 0;
					if (ke.isControlDown())
					{
						size -= 1;
					}
					switch (ke.getKeyCode())
					{
						case KeyEvent.VK_RIGHT: move( move ,  0    ,  size ,  0    , shift); break;
						case KeyEvent.VK_LEFT:  move(-move ,  0    , -size ,  0    , shift); break;
						case KeyEvent.VK_UP:    move( 0    , -move ,  0    , -size , shift); break;
						case KeyEvent.VK_DOWN:  move( 0    ,  move ,  0    ,  size , shift); break;
						case KeyEvent.VK_I:     buttonLearnIR(); break;
						case KeyEvent.VK_F3:    buttonLink(); break;
					}
					if (prefSelectionLast)
					{
						refreshDeskPanel();
					}
					else
					{
						repaint();
					}
				}
			});
			addFocusListener(new FocusAdapter()
			{
				public void focusGained(FocusEvent ke)
				{
					Component o = (Component)ke.getSource();
					if (!multiSelect.contains(o))
					{
						setDragSelection(o);
					}
				}
			});
			addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent m)
				{
					if (m.isAltDown() && m.isShiftDown())
					{
						Component c = getComponentAt(getPoint(m));
						if (c instanceof ButtonBox)
						{
							testButton(((ButtonBox)c).getButton());
						}
						return;
					}
					if (readonly)
					{
						return;
					}
					if (isRightClick(m))
					{
						if (ccf == null)
						{
							return;
						}
						rightClickMenu(getComponentAt(getPoint(m)), getPoint(m));
						return;
					}
					if (m.getClickCount() == 2)
					{
						sendEvent(EDIT_PROPERTY);
					}
				}
				public void mousePressed(MouseEvent m)
				{
					bounds = null;
					endBounds = null;
					selectDrag(m);
				}
				public void mouseReleased(MouseEvent m)
				{
					if (!(bounds == null || endBounds == null || bounds.equals(endBounds) || isRightClick(m)))
					{
						MultiDo dos = new MultiDo();
						for (int i=0; i<multiSelect.size(); i++)
						{
							ChildPanel c = (ChildPanel)multiSelect.get(i);
							dos.add(new DoBounds(c, c.bounds, c.endBounds));
						}
						pushDo(dos);
						if (prefSelectionLast)
						{
							refreshDeskPanel();
						}
					}
				}
				public void mouseEntered(MouseEvent m)
				{
					Component c = getComponentAt(getPoint(m));
					if (c != null && !readonly)
					{
						cursors.push(c.getCursor());
						c.setCursor(CURSOR_DEFAULT);
					}
				}
				public void mouseExited(MouseEvent m)
				{
					Component c = getComponentAt(getPoint(m));
					if (c != null && !cursors.empty() && !readonly)
					{
						c.setCursor((Cursor)cursors.pop());
					}
				}
			});
			addMouseMotionListener(new MouseMotionAdapter()
			{
				public void mouseDragged(MouseEvent m)
				{
					if (dragSelection == null || isRightClick(m) || readonly)
					{
						return;
					}
					if (!(dragSelection instanceof Movable))
					{
						return;
					}
					if (bounds == null)
					{
						return;
					}
					Point p = getAbsolutePoint(m);
					p.translate(-offset.x, -offset.y);
					Rectangle mm = null;
					if (!resize)
					{
						mm = new Rectangle(1,1,0,0);
					}
					else
					{
						switch (sides)
						{
							case NORTH:	mm = new Rectangle( 0, 1, 0,-1); break;
							case SOUTH:	mm = new Rectangle( 0, 0, 0, 1); break;
							case EAST:	mm = new Rectangle( 0, 0, 1, 0); break;
							case WEST:	mm = new Rectangle( 1, 0,-1, 0); break;
							case NW:	mm = new Rectangle( 1, 1,-1,-1); break;
							case NE:	mm = new Rectangle( 0, 1, 1,-1); break;
							case SW:	mm = new Rectangle( 1, 0,-1, 1); break;
							case SE:	mm = new Rectangle( 0, 0, 1, 1); break;
							default:    mm = new Rectangle( 1, 1, 0, 0); break;
						}
					}
					int nx = bounds.x + (mm.x*p.x);
					int ny = bounds.y + (mm.y*p.y);
					int nw = bounds.width + (mm.width*p.x);
					int nh = bounds.height + (mm.height*p.y);
					nx = Math.max(nx, -nw+2);
					ny = Math.max(ny, -nh+2);
					nx = Math.min(nx, panelSize.width-2);
					ny = Math.min(ny, panelSize.height-2);
					if (isSnapped())
					{
						int xd = (nx+(int)(snaps.w*nw)) % prefGrid.width;
						int yd = (ny+(int)(snaps.h*nh)) % prefGrid.height;
						nx = nx - xd + prefGrid.x;
						ny = ny - yd + prefGrid.y;
						if (mm.width != 0)
						{
							nw += xd;
							nw = nw - (nw % prefGrid.width);
						}
						if (mm.height != 0)
						{
							nh += yd;
							nh = nh - (nh % prefGrid.height);
						}
					}
					if (nw > 0 && nh > 0)
					{
						int dx = (nx-bounds.x);
						int dy = (ny-bounds.y); 
						int dw = (nw-bounds.width);
						int dh = (nh-bounds.height); 
						for (int i=0; i<multiSelect.size(); i++)
						{
							ChildPanel c = (ChildPanel)multiSelect.get(i);
							Rectangle r = c.bounds;
							if (c instanceof Resizable && ((Resizable)c).isResizable())
							{
								c.endBounds = new Rectangle(r.x+dx, r.y+dy, r.width+dw, r.height+dh);
							}
							else
							{
								c.endBounds = new Rectangle(r.x+dx, r.y+dy, r.width, r.height);
							}
							c.setBounds(c.endBounds);
						}
						setCCFChanged();
					}
				}
				public void mouseMoved(MouseEvent m)
				{
					if (readonly)
					{
						return;
					}
					final int sizerPad = 4;
					Point ep = getAbsolutePoint(m);
					Component c = getComponentAt(getPoint(m));
					if (c instanceof Movable)
					{
						Cursor cc = c.getCursor();
						Cursor nc = CURSOR_MOVE;
						if (c instanceof Resizable && ((Resizable)c).isResizable())
						{
							Dimension sz = c.getSize();
							Point p = getAbsoluteLocation(c);
							Point p2 = new Point(p.x+sz.width, p.y+sz.height);
							sides = 0;
							sides |= (ep.x - p.x  < sizerPad) ? WEST  : 0;
							sides |= (p2.x - ep.x < sizerPad) ? EAST  : 0;
							sides |= (ep.y - p.y  < sizerPad) ? NORTH : 0;
							sides |= (p2.y - ep.y < sizerPad) ? SOUTH : 0;
							resize = (sides > 0);
							switch (sides)
							{
								case NORTH: nc = CURSOR_NORTH; break;
								case SOUTH: nc = CURSOR_SOUTH; break;
								case EAST : nc = CURSOR_EAST ; break;
								case WEST : nc = CURSOR_WEST ; break;
								case NW   : nc = CURSOR_NW   ; break;
								case NE   : nc = CURSOR_NE   ; break;
								case SW   : nc = CURSOR_SW   ; break;
								case SE   : nc = CURSOR_SE   ; break;
							}
						}
						if (nc != cc && ((Selectable)c).isSelected())
						{
							c.setCursor(nc);
						}
					}
				}
			});
		}

		public void selectAll()
		{
			Component c[] = getComponents();
			if (c == null || c.length == 0)
			{
				return;
			}
			setDragSelection(null);
			for (int i=0; i<c.length; i++)
			{
				if (c[i] instanceof Selectable)
				{
					setDragSelection(c[i], true);
				}
			}
		}

		public void setBounds(int x, int y, int w, int h)
		{
			super.setBounds(x,y,w,h);
			if (source != null) { source.setBounds(new Rectangle(x,y,w,h)); }
			objectStatus.update();
		}

		public void setBounds(Rectangle r)
		{
			super.setBounds(r);
			if (source != null) { source.setBounds(r); }
			objectStatus.update();
		}

		public Dimension getSize()
		{
			return source != null ? source.getSize() : super.getSize();
		}

		public void setSize(Dimension sz)
		{
			super.setSize(sz);
			if (source != null) { source.setSize(sz); }
			objectStatus.update();
		}

		public Point getLocation()
		{
			return source != null ? source.getLocation() : super.getLocation();
		}

		public void setLocation(Point p)
		{
			super.setLocation(p);
			if (source != null) { source.setLocation(p); }
			objectStatus.update();
		}

		public boolean isFocusTraversable()
		{
			return true;
		}

		public void setReadOnly(boolean ro)
		{
			readonly = ro;
			Component c[] = getComponents();
			for (int i=0; c != null && i<c.length; i++)
			{
				if (c[i] instanceof ChildPanel)
				{
					((ChildPanel)c[i]).setReadOnly(ro);
				}
			}
		}

		public Object getMyParent()
		{
			return getParent();
		}

		public boolean isSnapped()
		{
			return ((ChildPanel)getParent()).isSnapped();
		}

		public void toggleSnap()
		{
			((ChildPanel)getParent()).toggleSnap();
		}

		public boolean isGridShowing()
		{
			return ((ChildPanel)getParent()).isGridShowing();
		}

		public int getScale()
		{
			try
			{
				return ((ChildPanel)getParent()).getScale();
			}
			catch (Exception ex)
			{
				debug.log(0,ex.toString());
				return 1;
			}
		}

		public void toggleGrid()
		{
			((ChildPanel)getParent()).toggleGrid();
		}

		public void setScale(int scale)
		{
			((ChildPanel)getParent()).setScale(scale);
		}

		public CCFChild getSource()
		{
			return source;
		}

		public void setImage(Image image)
		{
			this.image = image;
			if (image == null)
			{
				cachedIP = null;
			}
		}

		public ImageFontLabel getLabel()
		{
			return label;
		}

		public void setText(String text)
		{
			label.setText(text != null ? text : "");
		}

		public void setChildren(CCFChild child[])
		{
			removeAll();
			nextID = 0;
			for (int i=0; child != null && i<child.length; i++)
			{
				if (child[i] == null || child[i].child == null)
				{
					continue;
				}
				ChildPanel cp = (ChildPanel)wrappers.get(child[i]);
				if (cp == null)
				{
					wrappers.put(child[i], addChild(child[i], new Integer(nextID++)));
				}
				else
				{
					add(cp, new Integer(nextID++));
					cp.refresh();
				}
			}
		}

		public ChildPanel addChild(CCFChild child, Integer pos)
		{
			if (child.type == CCFChild.FRAME)
			{
				CCFFrame f = (CCFFrame)child.child;
				FrameBox fb = new FrameBox(f.width(),f.height(),child);
				add(fb, pos);
				return fb;
			}
			else
			{
				CCFButton b = (CCFButton)child.child;
				ButtonBox bb = new ButtonBox(b.width(),b.height(),child);
				add(bb, pos);
				return bb;
			}
		}

		public ChildPanel findChild(CCFChild child)
		{
			Component c[] = getComponents();
			for (int i=0; i<c.length; i++)
			{
				if (c[i] instanceof ChildPanel)
				{
					ChildPanel cp = (ChildPanel)c[i];
					if (cp.getSource() == child)
					{
						return cp;
					}
					cp = cp.findChild(child);
					if (cp != null)
					{
						return cp;
					}
				}
			}
			return null;
		}

		private boolean exclude = false;

		public void setExclude(boolean x)
		{
			exclude = x;
		}

		public void paint(Graphics g)
		{
			if (exclude)
			{
				return;
			}
			Dimension d = getSize();
			if (image != null)
			{
				g.drawImage(image, 0, 0, this);
			}
			else
			{
				if (custom())
				{
					if (!ccf.getTransparentColor().getAWTColor(true).equals(getBackground()))
					{
						g.setColor(getBackground());
						g.fillRect(0,0,d.width,d.height);
					}
					if (ChildPanel.this instanceof PanelPanel)
					{
						CCFPanel tp = ((PanelPanel)ChildPanel.this).getPanel().getTemplate();
						if (tp != null)
						{
							PanelPanel cp = getPanelPanel(tp);
							//Graphics2D g2 = (Graphics2D)g;
							//Composite c = g2.getComposite();
							//g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER ,0.5f));
							if (cp != null)
							{
								cp.paint(g);
							}
							//g2.setComposite(c);
						}
					}
				}
				else
				{
					g.setColor(getBackground());
					g.fillRect(0,0,d.width,d.height);
				}
			}
			if (label != null && label.getFont().getSize() > 4)
			{
				label.setBounds(0,0,d.width,d.height);
				label.paint(g);
			}
			super.paint(g);
			if (!prefSelectionLast && selected && !hideSelection)
			{
				g.setColor(Color.red);
				g.drawRect(0, 0, d.width-1, d.height-1);
			}
		}

		public Dimension getMinimumSize()
		{
			return getBounds().getSize();
		}

		public Dimension getPreferredSize()
		{
			return getMinimumSize();
		}

		public Dimension getMaximumSize()
		{
			return getMinimumSize();
		}

		public void select(boolean focus)
		{
			selected = true;
			if (prefSelectionLast)
			{
				refreshDeskPanel();
			}
			else
			{
				repaint();
			}
			if (focus)
			{
				requestFocus();
			}
		}

		public void unselect()
		{
			selected = false;
			if (prefSelectionLast)
			{
				refreshDeskPanel();
			}
			else
			{
				repaint();
			}
		}

		public boolean isSelected()
		{
			return selected;
		}

		private Point getPoint(MouseEvent me)
		{
			Point p = me.getPoint();
			int scale = getScale();
			p.x = p.x/scale;
			p.y = p.y/scale;
			return p;
		}

		private Point getAbsolutePoint(MouseEvent me)
		{
			Point p = getPoint(me);
			if (ChildPanel.this instanceof PanelPanel)
			{
				return p;
			}
			Component c = ChildPanel.this;
			while (c != null && !(c instanceof PanelPanel))
			{
				Point l = c.getLocation();
				int scale = getScale();
				p.translate(l.x/scale, l.y/scale);
				c = c.getParent();
			}
			return p;
		}

		public Point getAbsoluteLocation(Component c)
		{
			Point p = c.getLocation();
			int scale = getScale();
			p.x = p.x/scale;
			p.y = p.y/scale;
			c = c.getParent();
			while (c != null && !(c instanceof PanelPanel))
			{
				Point l = c.getLocation();
				p.translate(l.x/scale, l.y/scale);
				c = c.getParent();
			}
			return p;
		}

		public void selectDrag(MouseEvent m)
		{
			Point p = getAbsolutePoint(m);
			setDragSelection(getComponentAt(getPoint(m)),m.isShiftDown());
			for (int i=0; i<multiSelect.size(); i++)
			{
				ChildPanel c = (ChildPanel)multiSelect.get(i);
				c.bounds = c.getBounds();
			}
			controlClone(m);
			offset = p;
		}

		private void controlClone(MouseEvent m)
		{
			if (multiSelect.size() > 1 || dragSelection == null)
			{
				return;
			}
			if (m.isControlDown() &&
				dragSelection instanceof Copyable &&
				dragSelection instanceof ChildPanel)
			{
				ChildPanel scp = (ChildPanel)dragSelection;
				ChildPanel cpp = (ChildPanel)scp.getParent();
						   scp = (ChildPanel)((Copyable)scp).copy();
				if (cpp instanceof PanelPanel)
				{
					CCFPanel p = ((PanelPanel)cpp).getPanel();
					DoNodeUpdate nu = new DoNodeUpdate(p);
					p.addChild(scp.getSource());
					cpp.addChild(scp.getSource(), new Integer(nextID++));
					eventObjectTop();
					nu.getNewState();
					pushDo(nu);
				}
				else
				if (cpp instanceof FrameBox)
				{
					CCFFrame f = ((FrameBox)cpp).getFrame();
					DoNodeUpdate nu = new DoNodeUpdate(f);
					f.addChild(scp.getSource());
					cpp.addChild(scp.getSource(), new Integer(nextID++));
					eventObjectTop();
					nu.getNewState();
					pushDo(nu);
				}
			}
		}

		public PanelPanel getRootPanel()
		{
			ChildPanel cp = this;
			while (!(cp == null || cp instanceof PanelPanel))
			{
				cp = (ChildPanel)cp.getParent();
			}
			return (PanelPanel)cp;
		}

		public void refreshDeskPanel()
		{
			PanelPanel pp = getRootPanel();
			if (pp != null)
			{
				pp.refresh();
			}
		}
	}

	// ---( Inner Class PanelPanel )---
	class PanelPanel extends ChildPanel
		implements ButtonHost, FrameHost, Pastable, Selectable, Configurable
	{
		private CCFPanel panel;
		private boolean showGrid;
		private boolean snapGrid;
		private int scale = 1;

		PanelPanel(CCFPanel panel)
		{
			super(0, 0, panelSize.width, panelSize.height);
			this.panel = panel;
			this.showGrid = prefGridShow;
			this.snapGrid = prefGridSnap;
			boolean color = color();
			setBackground(CCFColor.getNamedColor(CCFColor.WHITE, color).getAWTColor(color));
			//setBackground(Color.white);
			ccfChangeEvent();
			// grid key binding
			bindAction(this, KeyEvent.VK_1, KeyEvent.ALT_MASK,
				new AbstractAction() {
					public void actionPerformed(ActionEvent ae) {
						setScale(1);
					} } );
			bindAction(this, KeyEvent.VK_2, KeyEvent.ALT_MASK,
				new AbstractAction() {
					public void actionPerformed(ActionEvent ae) {
						setScale(2);
					} } );
			bindAction(this, KeyEvent.VK_G, KeyEvent.CTRL_MASK,
				new AbstractAction() {
					public void actionPerformed(ActionEvent ae) {
						toggleGrid();
					} } );
			AbstractAction top = new EventAction(OBJECT_TOP);
			AbstractAction bottom = new EventAction(OBJECT_BOTTOM);
			AbstractAction raise = new EventAction(OBJECT_RAISE);
			AbstractAction lower = new EventAction(OBJECT_LOWER);
			bindAction(this, KeyEvent.VK_R, 0, raise);
			bindAction(this, KeyEvent.VK_L, 0, lower);
			bindAction(this, KeyEvent.VK_T, 0, top);
			bindAction(this, KeyEvent.VK_B, 0, bottom);
			bindAction(this, KeyEvent.VK_PAGE_UP, 0, raise);
			bindAction(this, KeyEvent.VK_PAGE_DOWN, 0, lower);
			bindAction(this, KeyEvent.VK_HOME, 0, top);
			bindAction(this, KeyEvent.VK_END, 0, bottom);
		}

		public String toString()
		{
			return "PanelPanel:"+Integer.toHexString(hashCode());
		}

		class EventAction extends AbstractAction
		{
			private int event;

			EventAction(int event)
			{
				this.event = event;
			}

			public void actionPerformed(ActionEvent ae)
			{
				sendEvent(event);
			}
		}

		public void editProperties()
		{
			panelProps.updatePanel(panel);
		}

		public void ccfChangeEvent()
		{
			setChildren(panel.child);
		}

		public void addButton()
		{
			addNewButton(panel);
		}

		public void addFrame()
		{
			addNewFrame(panel);
		}

		public CCFPanel getPanel()
		{
			return panel;
		}

		public void refresh()
		{
			boolean color = color();
			setBackground(CCFColor.getNamedColor(CCFColor.WHITE, color).getAWTColor(color));
			setSize(panelSize);
			setChildren(panel.child);
			repaint();
		}

		public void delete()
		{
			pushDo(new MultiDo(new Doable[] {
				new DoDeletePanel(panel),
				//new DoRemoveDependencies(ccf, panel),
			}));
		}

		public boolean acceptPaste(Object o)
		{
			return (o instanceof ButtonBox || o instanceof FrameBox);
		}

		public void paste(Object o)
		{
			if (o instanceof ButtonBox || o instanceof FrameBox)
			{
				DoNodeUpdate nu = new DoNodeUpdate(panel);
				CCFChild c = ((ChildPanel)o).getSource();
				panel.addChild(c);
				icons.addChild(c);
				nu.getNewState();
				pushDo(nu);
				setDragSelectionWithFocus(getChildPanel(c));
			}
		}

		public CCFIcon getPicture(ChildPanel ex)
		{
			ex.setExclude(true);

			Dimension sz = getSize();
			Dimension pictSize = ex.getSize();
			Point xlate = getAbsoluteLocation(ex);
			Image clipped = createImage(pictSize.width, pictSize.height);
			Graphics g = clipped.getGraphics();
			g.translate(-xlate.x, -xlate.y);
			super.paint(g);
			g.translate(xlate.x, xlate.y);
			ex.setExclude(false);
			return CCFIcon.create(clipped, iconMode());
		}

		public void paint(Graphics g)
		{
			((Graphics2D)g).scale((double)scale,(double)scale);
			/*
			AffineTransform at = new AffineTransform();
			at.setToScale(2.0f, 2.0f);
			((Graphics2D)g).setTransform(at);
			*/
			super.paint(g);
			if (isGridShowing())
			{
				Dimension d = getSize();
				((Graphics2D)g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
				int c = 0;
				int z = Math.max(prefGridMinorTicks+1, 1);
				Color ma = prefGridMajorColor.getAWTColor(true);
				Color mi = prefGridMinorColor.getAWTColor(true);
				for (int x=prefGrid.x; x<d.width; x += prefGrid.width)
				{
					g.setColor(c++ % z == 0 ? ma : mi);
					g.drawLine(x,0,x,d.height);
				}
				c = 0;
				for (int y=prefGrid.y; y<d.height; y += prefGrid.height)
				{
					g.setColor(c++ % z == 0 ? ma : mi);
					g.drawLine(0,y,d.width,y);
				}
				g.setPaintMode();
			}
			if (prefSelectionLast && !hideSelection)
			{
				drawSelections(this, g, 0, 0);
			}
		}

		private void drawSelections(Container p, Graphics g, int x, int y)
		{
			Component c[] = p.getComponents();
			for (int i=0; i<c.length; i++)
			{
				ChildPanel cp = (ChildPanel)c[i];
				Rectangle b = cp.getBounds();
				if (cp.selected)
				{
					g.setColor(Color.red);
					g.drawRect(x+b.x, y+b.y, b.width-1, b.height-1);
				}
				if (cp instanceof FrameBox)
				{
					drawSelections(cp, g, x+b.x, y+b.y);
				}
			}
		}

		public boolean isSnapped()
		{
			return snapGrid;
		}

		public void toggleSnap()
		{
			snapGrid = !snapGrid;
		}

		public boolean isGridShowing()
		{
			return showGrid;
		}

		public void toggleGrid()
		{
			showGrid = !showGrid;
			repaint();
		}

		public void setScale(int scale)
		{
			double delta = ((double)scale / (double)this.scale);
			this.scale = scale;
			repaint();
			Component c = this;
			for (int i=0; !(c instanceof DeskPanel) ; i++)
			{
				c = c.getParent();
			}
			if (c != null)
			{
				Dimension d = c.getSize();
				c.setSize((int)(d.width * delta), (int)(d.height * delta));
			}
		}

		public int getScale()
		{
			return scale;
		}

		public boolean isFocusTraversable()
		{
			return false;
		}
	}

	// ---( Inner Class FrameBox )---
	class FrameBox extends ChildPanel
		implements Selectable, Configurable, Deletable, ButtonHost,
		           FrameHost, Resizable, Movable, Pastable, Copyable, Namable
	{
		private CCFFrame frame;

		FrameBox(int w, int h, CCFChild child)
		{
			super(child.intX, child.intY, w, h, child);
			frame = (CCFFrame)child.child;
			readFrame();
			setChildren(frame.child);
		}

		public void readFrame()
		{
			Point p = frame.getLocation();
			Dimension d = frame.getSize();
			setBounds(p.x, p.y, d.width, d.height);
			setText(frame.getName());
			ImageFontLabel label = getLabel();
			label.setFont(getIFont(frame.getFont().getAWTSize()));
			boolean inColor = color();
			label.setForeground(frame.getForeground().getAWTColor(inColor));
			label.setAlignment(frame.getTextAlignment());
			setBackground(frame.getBackground().getAWTColor(inColor));
			setImage(frame.icon != null ? createImage(frame.icon.getImageProducer()) : null);
		}

		public void refresh()
		{
			setChildren(frame.child);
			readFrame();
		}

		public String getName()
		{
			return frame.getName();
		}

		public void setName(String name)
		{
			frame.setName(name);
			readFrame();
			repaint();
		}

		public int getFontSize()
		{
			return frame.fontSize;
		}

		public void setFontSize(int sz)
		{
			frame.fontSize = sz;
			readFrame();
			repaint();
		}

		public void addButton()
		{
			addNewButton(frame);
		}

		public void addFrame()
		{
			addNewFrame(frame);
		}

		public Object copy()
		{
			return new FrameBox(frame.width(),frame.height(),(CCFChild)(getSource().getClone()));
		}

		public void editProperties()
		{
			frameProps.updateFrame(this, frame);
			refreshPanel(frame.getParentPanel());
		}

		public CCFFrame getFrame()
		{
			return frame;
		}

		public boolean isResizable()
		{
			return frame.isResizable();
		}

		public void delete()
		{
			pushDo(new DoDeleteChild(frame.getChildWrapper()));
		}

		public boolean acceptPaste(Object o)
		{
			return (o instanceof ButtonBox || o instanceof FrameBox);
		}

		public void paste(Object o)
		{
			if (o instanceof ButtonBox || o instanceof FrameBox)
			{
				DoNodeUpdate nu = new DoNodeUpdate(frame);
				CCFChild c = ((ChildPanel)o).getSource();
				frame.addChild(c);
				icons.addChild(c);
				nu.getNewState();
				pushDo(nu);
				ChildPanel cp = getChildPanel(c);
				if (cp != null)
				{
					cp.select(true);
				}
			}
		}

		public void setBounds(Rectangle nb)
		{
			Rectangle ob = getBounds();
    		if ((ob.x != nb.x && ob.width != nb.width) || (ob.y != nb.y && ob.height != nb.height))
			{
				int dx = nb.x - ob.x;
				int dy = nb.y - ob.y;
				Component c[] = getComponents();
				for (int i=0; i<c.length; i++)
				{
					ChildPanel cp = (ChildPanel)c[i];
					Point p = cp.getLocation();
					cp.setLocation(new Point(p.x - dx, p.y - dy));
				}
			}
			super.setBounds(nb);
		}
	}

	// ---( Inner Class ButtonBox )---
	class ButtonBox extends ChildPanel
		implements Selectable, Deletable, Configurable, Resizable, Movable, Copyable, Namable, Pastable
	{
		private CCFButton button;

		ButtonBox(int w, int h, CCFChild child)
		{
			super(child.intX, child.intY, w, h, child);
			button = (CCFButton)child.child;
			readButton();
		}

		public void refresh()
		{
			readButton();
		}

		public CCFButton getButton()
		{
			return button;
		}

		public String getName()
		{
			return button.getName();
		}

		public void setName(String name)
		{
			button.setName(name);
			readButton();
			repaint();
		}

		public int getFontSize()
		{
			return button.fontSize;
		}

		public void setFontSize(int sz)
		{
			button.fontSize = sz;
			readButton();
			repaint();
		}

		private void readButton()
		{
			Point p = button.getLocation();
			Dimension d = button.getSize();
			setBounds(p.x, p.y, d.width, d.height);
			setText(button.getName());
			ImageFontLabel label = getLabel();
			label.setFont(getIFont(button.getFont().getAWTSize()));
			boolean inColor = color();
			label.setForeground(button.getForeground().getAWTColor(inColor));
			label.setAlignment(button.getTextAlignment());
			setBackground(button.getBackground().getAWTColor(inColor));
			if (button.isActive())
			{
				setImage(button.iconAU != null ? createImage(button.iconAU.getImageProducer()) : null);
			}
			else
			{
				setImage(button.iconIU != null ? createImage(button.iconIU.getImageProducer()) : null);
			}
		}

		public Object copy()
		{
			return new ButtonBox(button.width(),button.height(),(CCFChild)(getSource().getClone()));
		}

		public void editProperties()
		{
			buttonProps.updateButton(this, button);
		}

		public boolean isResizable()
		{
			return button.isResizable();
		}

		public void delete()
		{
			pushDo(new MultiDo(new Doable[] {
				new DoDeleteChild(button.getChildWrapper()),
				//new DoRemoveDependencies(ccf, button),
			}));
		}

		public boolean acceptPaste(Object o)
		{
			return (o instanceof ButtonBox);
		}

		public void paste(Object o)
		{
			if (o instanceof ButtonBox)
			{
				buttonPaste.invoke(((ButtonBox)o).getButton(), button);
			}
		}
	}

	// ---( Inner Class Selectable )---
	interface Selectable
	{
		public void select(boolean focus)
			;

		public void unselect()
			;

		public boolean isSelected()
			;
	}

	// -------------------------------------------------------------------------------------
	// DIALOG CLASSES
	// -------------------------------------------------------------------------------------

	// ---( Inner Class ButtonPasteDialog )---
	class ButtonPasteDialog extends OKCancelDialog
	{
		private CCFButton src, dst;
		private JCheckBox ckFont = new JCheckBox("Font");
		private JCheckBox ckText = new JCheckBox("Name");
		private JCheckBox ckLabel = new JCheckBox("ID Tag");
		private JCheckBox ckIcons = new JCheckBox("Icons");
		private JCheckBox ckColors = new JCheckBox("Colors");
		private JCheckBox ckActions = new JCheckBox("Actions");

		ButtonPasteDialog()
		{
			super("Paste Options");
			AAPanel ap = new AAPanel(true);
			ap.define('f', ckFont,     "pad=5,5,5,5;wx=1;fill=b");
			ap.define('l', ckText,     "pad=5,5,5,5;wx=1;fill=b");
			ap.define('t', ckLabel,    "pad=5,5,5,5;wx=1;fill=b");
			ap.define('i', ckIcons,    "pad=5,5,5,5;wx=1;fill=b");
			ap.define('c', ckColors,   "pad=5,5,5,5;wx=1;fill=b");
			ap.define('a', ckActions,  "pad=5,5,5,5;wx=1;fill=b");
			ap.setLayout(new String[] {
				" ff ii ",
				" ll cc ",
				" tt aa ",
			});
			setContents(ap);
			ckFont.setSelected(true);
			ckText.setSelected(true);
			ckLabel.setSelected(true);
			ckIcons.setSelected(true);
			ckColors.setSelected(true);
			ckActions.setSelected(false);
		}

		public void invoke(CCFButton src, CCFButton dst)
		{
			this.src = src;
			this.dst = dst;
			super.invoke();
		}

		public void doOK()
		{
			DoNodeUpdate nu = new DoNodeUpdate(dst);
			if (ckFont.isSelected())
			{
				dst.fontSize = src.fontSize;
			}
			if (ckText.isSelected())
			{
				dst.name = src.name;
			}
			if (ckLabel.isSelected())
			{
				dst.idtag = src.idtag;
			}
			if (ckIcons.isSelected())
			{
				dst.iconIU = src.iconIU;
				dst.iconIS = src.iconIS;
				dst.iconAU = src.iconAU;
				dst.iconAS = src.iconAS;
			}
			if (ckColors.isSelected())
			{
				dst.colorIU = src.colorIU;
				dst.colorIS = src.colorIS;
				dst.colorAU = src.colorAU;
				dst.colorAS = src.colorAS;
			}
			if (ckActions.isSelected())
			{
				dst.actions = (src.actions != null ? (CCFActionList)src.actions.getClone() : null);
			}
			nu.pushDoNewState();
			ButtonBox bb = (ButtonBox)getChildPanel(dst.getChildWrapper());
			bb.readButton();
			bb.getRootPanel().repaint();
		}

		public void doCancel()
		{
		}
	}

	// ---( Inner Class FirmwareDialog )---
	class FirmwareDialog extends OKCancelDialog
	{
		private Firmware firmware;
		private JCheckBox force = new JCheckBox("Reload current segments");

		FirmwareDialog(File ffile)
			throws IOException
		{
			super("Firmware Updater");
			firmware = new Firmware(ffile.toString());
			String desc = firmware.getDescription();
			String rep[][] = firmware.getSegmentReport();
			AAPanel firm = new AAPanel(true);
			Color red = Color.red;
			firm.add(getLabel(desc), "x=0;y=0;wx=1;fill=w;pad=15,5,15,5;w=3");
			firm.add(new Bar(),      "x=0;y=1;wx=1;fill=w;pad=15,5,15,5;w=3");
			firm.add(getLabel("Segment",red), "x=0;y=2;w=1;fill=w;anchor=w;wx=1;pad=5,15,15,5");
			firm.add(getLabel("Offset",red),  "x=1;y=2;w=1;fill=w;anchor=e;wx=0;pad=5,5,15,5");
			firm.add(getLabel("Length",red),  "x=2;y=2;w=1;fill=w;anchor=e;wx=0;pad=5,5,15,15");
			int y = 3;
			for (int i=0; i<rep.length; i++)
			{
				if (!rep[i][2].equals("0"))
				{
					firm.add(getLabel(rep[i][0]), "w=1;x=0;fill=w;anchor=w;wx=1;pad=5,15,5,5;y="+y);
					firm.add(getLabel(rep[i][1]), "w=1;x=1;fill=w;anchor=e;wx=0;pad=5,5,5,5;y="+y);
					firm.add(getLabel(rep[i][2]), "w=1;x=2;fill=w;anchor=e;wx=0;pad=5,5,5,15;y="+y);
					y++;
				}
			}
			firm.add(new Bar(), "x=0;wx=1;fill=w;anchor=c;pad=15,5,15,5;w=3;y="+(y++));
			firm.add(force,     "x=0;wx=1;fill=w;anchor=c;pad=5,5,5,5;w=3;y="+(y++));
			firm.add(getLabel("Click OK to update your remote's firmware",Color.blue), "x=0;wx=1;fill=w;anchor=c;pad=15,25,15,25;w=3;y="+y);
			force.setSelected(true);
			setContents(firm);
		}

		class Bar extends JPanel
		{
			Bar()
			{
				setBackground(Color.darkGray);
			}

			public Dimension getMinimumSize()
			{
				return getPreferredSize();
			}

			public Dimension getMaximumSize()
			{
				Dimension d = super.getMaximumSize();
				return new Dimension(d.width,3);
			}

			public Dimension getPreferredSize()
			{
				return new Dimension(300,3);
			}
		}

		private JLabel getLabel(String desc)
		{
			return getLabel(desc, Color.black);
		}

		private JLabel getLabel(String desc, Color color)
		{
			JLabel ml = new JLabel(desc);
			ml.setForeground(color);
			return ml;
		}

		public void doOK()
		{
			if (prefCommPort == null)
			{
				errorDialog("The default port is not set in Preferences");
				return;
			}
			if (confirmDialog("Are your sure?", "Are your sure you want to send this firmware to your remote?"))
			{
				if (ccf != null && !confirmDialog("CCF Type Check", "Are you sure the loaded CCF '"+fileName+"' matches the remote?"))
				{
					errorDialog("Aborting Update");
					return;
				}
				downloadFirmware(firmware, force.isSelected());
			}
		}

		public void doCancel()
		{
		}
	}

	// ---( Inner Class PrefsDialog )---
	static class PrefsDialog extends MemoryDialog
	{
		private ProntoModel models[] = ProntoModel.getModels();
		private JTextField emulators[] = new JTextField[models.length];
		private Hashtable dval = new Hashtable();
		private JTextField gval[] = new JTextField[5];
		private ColorPicker cmap[] = new ColorPicker[4];
		private ColorPicker gridMa = new ColorPicker(new CCFColor(CCFColor.BLACK), true, true);
		private ColorPicker gridMi = new ColorPicker(new CCFColor(CCFColor.BLACK), true, true);
		private JSlider grayTint = getSlider("Gray Tint",0,100,0,50,10,false);
		private JSlider vdebug[] = new JSlider[Debug.getActiveLoggers()];
		private JSlider commLOD = getSlider("Load Delay",      0, 1000, 250, 250, 50);
		private JSlider commCMD = getSlider("Command Delay",   0,  500,   0, 100, 25);
		private JSlider commATN = getSlider("Attention Delay", 0,  500,  25, 100, 25);
		private JComboBox coDefaultModel = new JComboBox();
		private JComboBox coFontSize = new JComboBox();
		private JCheckBox ckFontUsage = new JCheckBox("Use Pronto font in tree view");
		private JCheckBox ckTermLogging = new JCheckBox("Log to Console");
		private JCheckBox ckOldDialogs = new JCheckBox("Use Old-style File Dialogs");
		private JCheckBox ckObeyRemote = new JCheckBox("Use remote reported capability");
		private JCheckBox ckTestEmitIR = new JCheckBox("Emit IR during button test");
		private JCheckBox ckShowDevProp = new JCheckBox("Show Device Properties in Tree");
		private JCheckBox ckNetUpdate = new JCheckBox("Enable network updates");
		private JCheckBox ckScanOtherPorts = new JCheckBox("Scan other ports");
		private JCheckBox ckScanThreaded = new JCheckBox("Scan ports in parallel");
		private JCheckBox ckScanModems = new JCheckBox("Scan modem ports (dangerous)");
		private JCheckBox ckCenterDialog = new JCheckBox("Center Dialogs on parent");
		private JCheckBox ckGridSnap = new JCheckBox("Default snap to grid");
		private JCheckBox ckGridShow = new JCheckBox("Default show grid");
		private JCheckBox ckWebSafe = new JCheckBox("Show extended colors");
		private JCheckBox ckSelectLast = new JCheckBox("Paint selection on top");
		private JTextField defaultPort = new JTextField(20);
		private JTextField ceditor = new JTextField(20);
		private JTextField ieditor = new JTextField(20);
		private FileBrowse fbCCF = new FileBrowse("CCF Editor", ceditor);
		private FileBrowse fbIMG = new FileBrowse("Image Editor", ieditor);
		private boolean warnFontUse = false;
		private boolean warnFontSize = false;
		private int oldTint;

		public void actionPerformed(ActionEvent ae)
		{
			if (ae.getSource() == ckFontUsage && !warnFontUse)
			{
				infoDialog("This change will take effect after Tonto restarts");
				warnFontUse = true;
			}
			else
			if (ae.getSource() == coFontSize && !warnFontSize)
			{
				if (((Integer)coFontSize.getSelectedItem()).intValue() != prefTreeFontSize)
				{
					infoDialog("This change will take effect after Tonto restarts");
					warnFontSize = true;
				}
			}
			else
			{
				super.actionPerformed(ae);
			}
		}

		PrefsDialog()
		{
			super("Preferences", PREF_PREF_BOUNDS);
			JTabbedPane tabs = new JTabbedPane();

			// -- grid --
			ProntoModel pm[] = ProntoModel.getModels();
			for (int i=0; i<pm.length; i++)
			{
				coDefaultModel.addItem(pm[i].getName());
			}
			for (int i=8; i<=18; i+=2)
			{
				coFontSize.addItem(new Integer(i));
			}
			for (int i=0; i<gval.length; i++)
			{
				gval[i] = new JTextField(3);
			}
			AAPanel psize = new AAPanel();
			AAPanel pdefl = new AAPanel(true);
			AAPanel pfont = new AAPanel(true);
			AAPanel pgrid = new AAPanel(true);
			AAPanel pmisc = new AAPanel(true);
			AAPanel anote = new AAPanel(true);
			JLabel imLabel = new JLabel("Image Editor");
			JLabel ccLabel = new JLabel("CCF Editor");
			JLabel ticklab = new JLabel("Tick Colors");
			JLabel node = new JLabel("moved to the Helper tabs", JLabel.LEFT);
			psize.define('$',                      "wx=1;fill=b");
			psize.define('=',                      "wx=1;fill=b");
			psize.define(',', anote,               "wx=1;fill=b");
			psize.define('+', pgrid,               "wx=1;fill=b");
			psize.define('.', pdefl,               "wx=1;fill=b");
			psize.define(':', pfont,               "wx=1;fill=b");
			psize.define('-', pmisc,               "wx=1;fill=b");
			psize.define('p', coDefaultModel,      "pad=3,3,3,3");
			psize.define('t', coFontSize,          "pad=3,3,3,3");
			psize.define('X', label("X Offset"),   "pad=3,3,3,3");
			psize.define('Y', label("Y Offset"),   "pad=3,3,3,3");
			psize.define('W', label("Width"),      "pad=3,3,3,3");
			psize.define('H', label("Height"),     "pad=3,3,3,3");
			psize.define('R', label("Minor Ticks"),"pad=3,3,3,3");
			psize.define('x', gval[0],             "pad=3,3,3,3;fill=h");
			psize.define('y', gval[1],             "pad=3,3,3,3;fill=h");
			psize.define('w', gval[2],             "pad=3,3,3,3;fill=h");
			psize.define('h', gval[3],             "pad=3,3,3,3;fill=h");
			psize.define('r', gval[4],             "pad=3,3,3,3;fill=h");
			psize.define('l', ckTermLogging,       "pad=0,3,3,3;fill=h;wx=1");
			psize.define('d', ckOldDialogs,        "pad=0,3,3,3;fill=h;wx=1");
			psize.define('f', ckFontUsage,         "pad=0,3,3,3;fill=h;wx=1");
			psize.define('o', ckObeyRemote,        "pad=0,3,3,3;fill=h;wx=1");
			psize.define('i', ckTestEmitIR,        "pad=0,3,3,3;fill=h;wx=1");
			psize.define('s', ckShowDevProp,       "pad=0,3,3,3;fill=h;wx=1");
		 	psize.define('n', ckNetUpdate,         "pad=0,3,3,3;fill=h;wx=1");
		 	psize.define('e', ckCenterDialog,      "pad=0,3,3,3;fill=h;wx=1");
		 	psize.define('k', ckGridSnap,          "pad=0,3,3,3;fill=h;wx=1");
		 	psize.define('g', ckGridShow,          "pad=0,3,3,3;fill=h;wx=1");
		 	psize.define('b', ckWebSafe,           "pad=0,3,3,3;fill=h;wx=1");
		 	psize.define('S', ckSelectLast,        "pad=0,3,3,3;fill=h;wx=1");
		 	psize.define('1', gridMa,              "pad=3,3,3,3;fill=h");
		 	psize.define('2', gridMi,              "pad=3,3,3,3;fill=h");
		 	psize.define('3', ticklab,             "pad=3,3,3,3;fill=h");
			psize.define('N', node,                "pad=3,3,3,3;fill=h");
			psize.setLayout(new String[] {
				"....... :::::::::::",
				". ppp . : ttt     :",
				"....... :::::::::::",
				"+++++++++++++++++++",
				"+ X x Y y = kkkkk +",
				"+ W w H h = ggggg +",
				"+ R r     = 1 2 3 +",
				"+++++++++++++++++++",
				"-------------------",
				"- ddddd ooooo     -",
				"- fffff iiiii     -",
				"- sssss nnnnn     -",
				"- eeeee lllll     -",
				"- bbbbb SSSSS     -",
				"-------------------",
				",,,,,,,,,,,,,,,,,,,",
				", NNNNNNNNNNNNNNN ,",
				",,,,,,,,,,,,,,,,,,,",
			});
			Util.setLabelBorder("Default Model", pdefl);
			Util.setLabelBorder("Tree Font Size", pfont);
			Util.setLabelBorder("Panel Grid", pgrid);
			Util.setLabelBorder("Options", pmisc);
			Util.setLabelBorder("Emulators/Editors", anote);

			// -- debug --
			AAPanel pdebug = new AAPanel();
			pdebug.add(new JLabel("0=off   1=light   2=verbose   3=all", JLabel.CENTER), "fill=b;pad=3,3,6,3;wx=1;x=0;y=0");
			Enumeration e = Debug.getLoggerNames();
			for (int i=0; i<vdebug.length; i++)
			{
				String s = (String)e.nextElement();
				Debug d = Debug.getInstance(s);
				vdebug[i] = getSlider(s, 0, 3, d.getLevel(), 1, 1);
				dval.put(s, vdebug[i]);
				pdebug.add(vdebug[i], "fill=b;pad=3,3,3,3;wx=1;x=0;y="+(i+1));
			}

			// -- comm --
			AAPanel pcomm = new AAPanel();
			JLabel txt = new JLabel("All times in milliseconds", JLabel.CENTER);
			AAPanel dport = new AAPanel(true);
			pcomm.define('.',                     "fill=b;wx=1");
			pcomm.define(',', dport,              "fill=b;pad=3,3,3,3");
			pcomm.define(':',                     "fill=b");
			pcomm.define('D', new JLabel("Port"), "fill=b;pad=3,3,3,3");
			pcomm.define('d', defaultPort,        "fill=b;pad=3,3,3,3;wx=1");
			pcomm.define('x', ckScanOtherPorts,   "fill=b;pad=3,3,3,3;wx=1");
			pcomm.define('T', ckScanThreaded,     "fill=b;pad=3,3,3,3;wx=1");
			if (Util.onMacintosh()) {
			pcomm.define('m', ckScanModems,       "fill=b;pad=3,3,3,3;wx=1");
			}
			pcomm.define('t', txt,                "fill=b;pad=3,3,3,3;wx=1");
			pcomm.define('c', commCMD,            "fill=b;pad=3,3,3,3;wx=1");
			pcomm.define('a', commATN,            "fill=b;pad=3,3,3,3;wx=1");
			pcomm.define('l', commLOD,            "fill=b;pad=3,3,3,3;wx=1");
			Util.setLabelBorder("Default Comm Port", dport);
			ckScanOtherPorts.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					checkScanOptions();
				}
			});
			pcomm.setLayout(new String[] {
				".................",
				". ,,,,,,,,,,,,, .",
				". , DDD ddddd , .",
				". , xxxxxxxxx , .",
				". , TTTTTTTTT , .",
				". , mmmmmmmmm , .",
				". ,,,,,,,,,,,,, .",
				".               .",
				". ttttttttttttt .",
				".               .",
				". aaaaaaaaaaaaa .",
				". ccccccccccccc .",
				". lllllllllllll .",
				".................",
			});

			// -- colors --
			AAPanel pcolr = new AAPanel(true);
			AAPanel clabl = new AAPanel(true);
			AAPanel cgrid = new AAPanel(true);
			Util.setLabelBorder("CCF Color Mapping", clabl);
			Util.setLabelBorder("Grid line colors", cgrid);
			final ColorPicker clr_black = new ColorPicker(CCFColor.getNamedColor(CCFColor.BLACK,false),false,false);
			final ColorPicker clr_dgray = new ColorPicker(CCFColor.getNamedColor(CCFColor.DARK_GRAY,false),false,false);
			final ColorPicker clr_lgray = new ColorPicker(CCFColor.getNamedColor(CCFColor.LIGHT_GRAY,false),false,false);
			final ColorPicker clr_white = new ColorPicker(CCFColor.getNamedColor(CCFColor.WHITE,false),false,false);
			ActionButton ab = new ActionButton("Use Defaults") {
				public void action() {
					for (int i=0; i<4; i++) {
						cmap[i].setColor(CCFColor.defaultMap[i],true);
						grayTint.setValue(0);
					}
				}
			};
			JLabel tx0 = new JLabel("Simulate the greenish tint for gray models", JLabel.CENTER);
			JLabel tx1 = new JLabel("Set the color mapping for conversion", JLabel.CENTER);
			JLabel tx2 = new JLabel("of greyscale icons to color", JLabel.CENTER);
			for (int i=0; i<4; i++)
			{
				cmap[i] = new ColorPicker();
			}
			pcolr.define('+', clabl,             "pad=3,3,3,3;wx=1;fill=b");
			pcolr.define('.',                    "align=c");
			pcolr.define('-',                    "pad=6,6,20,6");
			pcolr.define('G', tx0,               "fill=h;pad=3,3,3,3");
			pcolr.define('T', tx1,               "fill=h;pad=3,3,3,3");
			pcolr.define('t', tx2,               "fill=h;pad=3,3,8,3");
			pcolr.define('l',                    "wx=1;fill=b");
			pcolr.define('r',                    "wx=1;fill=b");
			pcolr.define('0', clr_black,         "pad=3,3,3,3");
			pcolr.define('2', clr_dgray,         "pad=3,3,3,3");
			pcolr.define('4', clr_lgray,         "pad=3,3,3,3");
			pcolr.define('6', clr_white,         "pad=3,3,3,3");
			pcolr.define('1', cmap[0],           "pad=3,3,3,3");
			pcolr.define('3', cmap[1],           "pad=3,3,3,3");
			pcolr.define('5', cmap[2],           "pad=3,3,3,3");
			pcolr.define('7', cmap[3],           "pad=3,3,3,3");
			pcolr.define('a', new JLabel("-->"), "pad=3,3,3,3");
			pcolr.define('b', new JLabel("-->"), "pad=3,3,3,3");
			pcolr.define('c', new JLabel("-->"), "pad=3,3,3,3");
			pcolr.define('d', new JLabel("-->"), "fill=h;pad=3,3,3,3");
			pcolr.define('z', ab,                "pad=10,3,10,3");
			pcolr.define('g', grayTint,          "wx=1;fill=b");
			pcolr.setLayout(new String[] {
				"+++++++++++++",
				"+-----------+",
				"+- GGGGGGG -+",
				"+- ggggggg -+",
				"+-----------+",
				"+TTTTTTTTTTT+",
				"+ttttttttttt+",
				"+l 00 a 11 r+",
				"+l 22 b 33 r+",
				"+l 44 c 55 r+",
				"+l 66 d 77 r+",
				"+zzzzzzzzzzz+",
				"+++++++++++++",
			});

			grayTint.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent ce) {
					int v = grayTint.getValue();
					CCFColor.setGrayTint(v);
					clr_dgray.repaint();
					clr_lgray.repaint();
					clr_white.repaint();
				}
			} );

			// -- helpers --
			AAPanel phelp = new AAPanel(true);
			AAPanel emuls = new AAPanel(true);
			AAPanel pedit = new AAPanel(true);
			for (int i=0; i<models.length; i++)
			{
				JLabel tl = new JLabel(models[i].getName());
				emulators[i] = new JTextField(20);
				FileBrowse br = new FileBrowse(tl.getText(), emulators[i]);
				emuls.add(tl,           "pad=3,3,3,3;x=0;y="+i+";fill=h;wx=0");
				emuls.add(emulators[i], "pad=3,3,3,3;x=1;y="+i+";fill=h;wx=1");
				emuls.add(br,           "pad=3,3,3,3;x=2;y="+i+";fill=h;wx=0");
			}
			phelp.define(',', pedit,             "wx=1;fill=b");
			phelp.define('h', emuls,             "pad=3,3,3,3;fill=b;wx=1");
			phelp.define('e', ieditor,           "pad=3,3,3,3;fill=h;wx=1");
			phelp.define('E', imLabel,           "pad=3,3,3,3;fill=h");
			phelp.define('u', ceditor,           "pad=3,3,3,3;fill=h;wx=1");
			phelp.define('U', ccLabel,           "pad=3,3,3,3;fill=h");
			phelp.define('2', fbCCF,             "pad=3,3,3,3;fill=h");
			phelp.define('3', fbIMG,             "pad=3,3,3,3;fill=h");
			phelp.setLayout(new String[] {
				"hhhhhhhhhhhhh",
				"h           h",
				"hhhhhhhhhhhhh",
				",,,,,,,,,,,,,",
				",MMM mmmmm 1,",
				",UUU uuuuu 2,",
				",EEE eeeee 3,",
				",,,,,,,,,,,,,",
			});
			Util.setLabelBorder("Emulators", emuls);
			Util.setLabelBorder("Editors",   pedit);

			// -- layout --
			tabs.addTab("Setup", psize);
			tabs.addTab("Debug", pdebug);
			tabs.addTab("Comm", pcomm);
			tabs.addTab("Colors", pcolr);
			tabs.addTab("Helpers", phelp);

			coFontSize.addActionListener(this);
			ckFontUsage.addActionListener(this);

			setContents(tabs);
		}

		private void checkScanOptions()
		{
			ckScanThreaded.setEnabled(ckScanOtherPorts.isSelected());
			ckScanModems.setEnabled(ckScanOtherPorts.isSelected());
		}

		private JLabel label(String str)
		{
			return new JLabel(str, JLabel.RIGHT);
		}
		
		private JSlider getSlider(
			String label, int min, int max, int def, int major, int minor)
		{
			return getSlider(label, min, max, def, major, minor, true);
		}

		private JSlider getSlider(
			String label, int min, int max, int def, int major, int minor, boolean snap)
		{
			JSlider s = new JSlider(min,max,def);
			s.setPaintTicks(true);
			s.setPaintLabels(true);
			s.setSnapToTicks(snap);
			s.setMajorTickSpacing(major);
			s.setMinorTickSpacing(minor);
			Util.setLabelBorder(label, s);
			return s;
		}

		private int parse(JTextField t, int def, int lo)
		{
			try
			{
				return Util.bound(lo,50,Integer.parseInt(t.getText().trim()));
			}
			catch (Exception ex)
			{
				return def;
			}
		}

		public void doOK()
		{
			boolean devPrefChg = (ckShowDevProp.isSelected() != prefShowDeviceProps);
			boolean doNetUpdate = (ckNetUpdate.isSelected() != prefNetworkUpdate);
			prefDefaultModel = ProntoModel.getModelByName(coDefaultModel.getSelectedItem().toString());
			prefTreeFontSize = ((Integer)coFontSize.getSelectedItem()).intValue();
			fileNew.setLabel("New "+prefDefaultModel.getName());
			prefLogToConsole = ckTermLogging.isSelected();
			prefObeyRemoteCap = ckObeyRemote.isSelected();
			prefUseAWTFileDialogs = ckOldDialogs.isSelected();
			prefUseProntoFont = ckFontUsage.isSelected();
			prefEmitIR = ckTestEmitIR.isSelected();
			prefSelectionLast = ckSelectLast.isSelected();
			prefCenterDialogs = ckCenterDialog.isSelected();
			prefShowDeviceProps = ckShowDevProp.isSelected();
			prefShowWebSafe = ckWebSafe.isSelected();
			prefNetworkUpdate = ckNetUpdate.isSelected();
			prefScanOtherPorts = ckScanOtherPorts.isSelected();
			prefScanThreaded = ckScanThreaded.isSelected();
			prefScanModems = ckScanModems.isSelected();
			javax.comm.DriverMac_OS_X.setIgnoreModems(!prefScanModems);
			String imged = ieditor.getText().trim();
			String edit = ceditor.getText().trim();
			prefEditor = edit.length() > 0 ? edit : null;
			for (int i=0; i<models.length; i++)
			{
				String key = PREF_EMULATOR+"."+models[i].getName();
				String val = emulators[i].getText().trim();
				prefs.setProperty(key, val);
			}
			prefImageEditor = imged.length() > 0 ? imged : null;
			String dport = defaultPort.getText().trim();
			prefCommPort = dport.length() > 0 ? dport : null;
			prefGrid = new Rectangle(parse(gval[0], prefGrid.x ,0), parse(gval[1], prefGrid.y, 0), parse(gval[2], prefGrid.width, 2), parse(gval[3], prefGrid.height, 2));
			prefGridSnap = ckGridSnap.isSelected();
			prefGridShow = ckGridShow.isSelected();
			prefGridMinorTicks = parse(gval[4], prefGridMinorTicks, 0);
			prefGridMinorColor = gridMi.getColor();
			prefGridMajorColor = gridMa.getColor();
			for (Enumeration e = dval.keys(); e.hasMoreElements(); )
			{
				String k = (String)e.nextElement();
				Debug.getInstance(k).setLevel(((JSlider)dval.get(k)).getValue());
			}
			for (int i=0; i<4; i++)
			{
				prefColorMap[i] = cmap[i].getColor();
			}
			Comm.setLoadDelay(commLOD.getValue());
			Comm.setCommandDelay(commCMD.getValue());
			Comm.setAttentionDelay(commATN.getValue());
			setLogging(prefLogToConsole);
			if (devPrefChg)
			{
				refreshAllTreeModels();
			}
			if (doNetUpdate)
			{
				setupNetworkUpdates();
			}
			if (CCFColor.getGrayTint() != oldTint)
			{
				clearAllGrayIconCaches();
			}
			savePreferences();
			refreshAllMenus();
			state().repaintAllPanels();
		}

		public void doCancel()
		{
			CCFColor.setGrayTint(oldTint);
		}

		public void show()
		{
			oldTint = CCFColor.getGrayTint();
			grayTint.setValue(oldTint);
			coDefaultModel.setSelectedItem(prefDefaultModel.getName());
			coFontSize.setSelectedItem(new Integer(prefTreeFontSize));
			ckTermLogging.setSelected(prefLogToConsole);
			ckOldDialogs.setSelected(prefUseAWTFileDialogs);
			ckFontUsage.setSelected(prefUseProntoFont);
			ckObeyRemote.setSelected(prefObeyRemoteCap);
			ckTestEmitIR.setSelected(prefEmitIR);
			ckCenterDialog.setSelected(prefCenterDialogs);
			ckShowDevProp.setSelected(prefShowDeviceProps);
			ckWebSafe.setSelected(prefShowWebSafe);
			ckSelectLast.setSelected(prefSelectionLast);
			ckNetUpdate.setSelected(prefNetworkUpdate);
			ckScanOtherPorts.setSelected(prefScanOtherPorts);
			ckScanThreaded.setSelected(prefScanThreaded);
			ckScanModems.setSelected(prefScanModems);
			ieditor.setText(prefImageEditor != null ? prefImageEditor : "");
			ceditor.setText(prefEditor != null ? prefEditor : "");
			gridMa.setColor(prefGridMajorColor,true);
			gridMi.setColor(prefGridMinorColor,true);
			for (int i=0; i<models.length; i++)
			{
				String key = PREF_EMULATOR+"."+models[i].getName();
				emulators[i].setText(prefs.getProperty(key, ""));
			}
			defaultPort.setText(prefCommPort != null ? prefCommPort : "");
			checkScanOptions();
			gval[0].setText(prefGrid.x+"");
			gval[1].setText(prefGrid.y+"");
			gval[2].setText(prefGrid.width+"");
			gval[3].setText(prefGrid.height+"");
			gval[4].setText(prefGridMinorTicks+"");
			ckGridSnap.setSelected(prefGridSnap);
			ckGridShow.setSelected(prefGridShow);
			for (Enumeration e = dval.keys(); e.hasMoreElements(); )
			{
				String k = (String)e.nextElement();
				JSlider s = (JSlider)dval.get(k);
				s.setValue(Debug.getInstance(k).getLevel());
			}
			commLOD.setValue(Comm.getLoadDelay());
			commCMD.setValue(Comm.getCommandDelay());
			commATN.setValue(Comm.getAttentionDelay());
			for (int i=0; i<4; i++)
			{
				cmap[i].setColor(prefColorMap[i],true);
			}
			super.show();
		}
	}

	// ---( Inner Class DragBorder )---
	class DragBorder implements Border
	{
		public Insets getBorderInsets(Component c)
		{
			return new Insets(1,0,1,0);
		}
		
		public boolean isBorderOpaque()
		{
			return true;
		}
		
		public void paintBorder(Component c, Graphics g, int x, int y, int w, int h)
		{
			g.setColor(Color.black);
			if (treeBefore)
			{
				g.drawLine(x,y,x+w,y);
			}
			else
			{
				g.drawLine(x,y+h-1,x+w,y+h-1);
			}
		}
	}

	// ---( Inner Class IRSignalDialog )---
	class IRSignalDialog extends OKCancelDialog
	{
		private IRSignalPanel sp;

		IRSignalDialog(IRSignal sig)
		{
			super("IR Signal");

			sp = new IRSignalPanel();
			sp.setSignal(sig);
			setContents(sp);
		}

		public JComponent getButtons()
		{
			return UIUtils.getOtherOKCancel(new String[] { "Cleanup" }, IRSignalDialog.this);
		}

		public boolean handleCmd(String cmd)
		{
			if (cmd != null)
			{
				if (cmd.equals("Cleanup"))
				{
					sp.cleanup();
					return false;
				}
			}
			return false;
		}

		public void doOK()
		{
		}

		public void doCancel()
		{
		}

		public void bindOK()
		{
		}

		public IRSignal getValue()
		{
			if (invoke())
			{
				return sp.getSignal();
			}
			else
			{
				return null;
			}
		}
	}

	// ---( Inner Class IRCapture )---
	static class IRCapture extends OKCancelDialog implements Runnable
	{
		private Pronto learned;
		private JButton start = new JButton("Start");
		private JButton cancel = new JButton("Cancel");
		private JTextArea work = new JTextArea(8,40);
		private JComboBox num = new JComboBox();
		private JPanel sign = new JPanel();
		private Thread thread;

		IRCapture()
		{
			super("Capture IR");
			AAPanel cp = new AAPanel(true);

			JTextArea text = new JTextArea();
			text.setBorder(new BevelBorder(BevelBorder.LOWERED));
			text.setEditable(false);
			text.setLineWrap(true);
			text.setWrapStyleWord(true);
			text.setText(
				"We will sample the key at least two times to "+
				"insure the signal is clean. If the signals do "+
				"not match, you may be asked to press it a couple "+
				"more times until it does. The best signal will "+
				"be chosen in the case of no matches.");

			work.setBorder(new BevelBorder(BevelBorder.LOWERED));
			work.setEditable(false);

			start.setEnabled(true);
			cancel.setEnabled(true);
			start.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae)
				{
					start();
				}
			});
			cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae)
				{
					stop();
				}
			});

			JScrollPane scroll = new JScrollPane(work);
			scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

			JLabel labl = new JLabel("Max Retries", JLabel.RIGHT);
			for (int i=1; i<8; i++)
			{
				num.addItem(new Integer(i));
			}
			num.setSelectedItem(new Integer(4));

			cp.define('0', start,  "pad=5,5,0,0;fill=b;wx=1");
			cp.define('1', cancel, "pad=5,5,0,5;fill=b;wx=1");
			cp.define('a', labl,   "pad=5,5,0,5;fill=b");
			cp.define('b', num,    "pad=5,5,0,5;fill=b");
			cp.define('2', text,   "pad=5,5,0,5;fill=b");
			cp.define('3', scroll, "pad=5,5,0,5;fill=b;wx=1;wy=1");
			cp.define('4', sign,   "pad=5,5,5,5;fill=b");

			cp.setLayout(new String[] {
				" 0 1 a b ",
				" 2222222 ",
				" 3333333 ",
				" 4444444 ",
			});

			setContents(cp);
		}

		private void start()
		{
			// check for started thread
			work.setText("");
			start.setEnabled(false);
			cancel.setEnabled(true);
			thread = new Thread(IRCapture.this);
			thread.start();
		}

		private void stop()
		{
			start.setEnabled(true);
			cancel.setEnabled(false);
			// stop thread
			if (thread != null)
			{
				thread.interrupt();
			}
		}

		public void doOK() { }

		public void doCancel()
		{
			closeComm();
		}

		private int len(Pronto s)
		{
			if (s.isRawSignal())
			{
				return s.getRawSignal().length;
			}
			else
			{
				return s.getIntro().length() + s.getRepeat().length();
			}
		}

		public void run()
		{
			Pronto shortest = null;
			Vector signals = new Vector();
			try
			{
				int max = ((Integer)num.getSelectedItem()).intValue();
				for (int i=0; i<max; i++)
				{
					new RunInThread() { public void runIn() throws Exception {
						comm.waitLearning();
						work.append("Press Remote Key... ");
					} }.start();
					Pronto p = null;
					try
					{
						p = new Pronto(comm.learnIR(Tonto.state()));
					}
					catch (Exception ex)
					{
						debug(ex);
					}
					debug.log(2, "sig = "+p);
					if (p == null || p.getPulseIndex().getIndexValues().length == 0)
					{
						work.append("Invalid IR Signal\n");
					}
					else
					{
						if (p.isRawSignal())
						{
							work.append("Captured IR Signal ("+p.getRawSignal().length+")\n");
						}
						else
						{
							work.append("Captured IR Signal ("+p.getIntro().length()+","+p.getRepeat().length()+")\n");
						}
						for (int j=0; j<signals.size(); j++)
						{
							if (signals.get(j).equals(p))
							{
								work.append("Signal Match!\n");
								learned = p;
								return;
							}
						}
						signals.add(p);
						if (shortest == null || len(p) < len(shortest))
						{
							shortest = p;
						}
					}
					work.setCaretPosition(work.getText().length());
				}
				learned = shortest;
				if (shortest == null)
				{
					work.append("No good signals were captured. Try again.");
				}
				else
				{
					work.append("You may want to try this again until you "+
						"get a clean signal match.\n");
				}
			}
			catch (Throwable t)
			{
				debug(t);
			}
			finally
			{
				stop();
				work.setCaretPosition(work.getText().length());
			}
		}

		public Pronto getIRCode()
		{
			return learned;
		}
	}

	// ---( Inner Class TransferDialog )---
	static class TransferDialog extends StackedDialog implements ITaskStatus
	{
		private JProgressBar bar;
		private JTextField text;
		private Runnable run;

		TransferDialog(String title)
		{
			this(title, true);
		}

		TransferDialog(String title, boolean modal)
		{
			super(title, modal);

			bar = new JProgressBar(0,100);
			bar.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
			bar.setStringPainted(true);

			text = new JTextField(20);
			text.setEditable(false);
			text.setText(title);

			ActionButton ab = new ActionButton("Cancel") {
				public void action() {
					if (thread != null)
					{
						thread.interrupt();
					}
					dispose();
					closeComm();
				}
			};

			add(bar,  "x=0;y=0;wx=1;wy=1;fill=b;pad=5,5,5,5");
			add(text, "x=0;y=1;wx=1;wy=1;fill=b;pad=5,5,5,5");
			add(ab,   "x=0;y=2;wx=1;wy=0;fill=b;pad=5,5,5,5");
		}

		// ---( ITaskStatus interface methods )---
		public void taskStatus(int val, String msg)
		{
			int nv = val;
			int ov = bar.getValue();
			if (nv - ov > 10)
			{
				for (int i=ov; i<=nv; i += 10)
				{
					progress(i);
					try { Thread.currentThread().sleep(50); } catch (Exception ex) { }
				}
			}
			progress(nv);
			if (msg != null)
			{
				text.setText(msg);
			}
		}

		public void taskError(Throwable t)
		{
			debug.log(1,"task err: "+t);
		}

		public void taskNotify(Object t)
		{
			if (t instanceof Integer)
			{
				progress(((Integer)t).intValue());
			}
		}

		public void progress(int pct)
		{
			pct = Math.min(pct, 100);
			bar.setValue(pct);
			bar.setString(pct+"%");
			//bar.repaint();
		}

		public void showHook(JDialog d)
		{
			if (thread != null)
			{

			final Thread t = thread;
			d.addWindowListener(new WindowAdapter() {
				private int i = 0;
				public void windowActivated(WindowEvent we) {
					if (i++ == 0)
					{
						t.start();
					}
				}
			});

			}
		}

		private Thread thread;

		public void show(Thread tr)
		{
			thread = tr;
			show();
		}

		public void dispose()
		{
			super.dispose();
			if (thread != null)
			{
				thread.interrupt();
			}
		}
	}

	// ---( Inner Class SystemProps )---
	class SystemProps extends OKCancelDialog implements ActionListener
	{
		private JCheckBox factory = new JCheckBox("Default Configuration (restored after crash)");
		private JCheckBox config = new JCheckBox("Configuration is write protected");
		private JCheckBox home = new JCheckBox("Home panels are write protected");
		private JTextField version = new JTextField(12);
		private IconButton icon = new IconButton();
		private JComboBox channel = new JComboBox();
		private JTextField width = new JTextField(3);
		private JTextField height = new JTextField(3);
		private ColorPicker tcolor = new ColorPicker(CCFColor.getNamedColor(CCFColor.BLACK, true));

		SystemProps(boolean ext)
		{
			super("System Properties");

			channel.addItem("IR");
			channel.addItem("RF Channel 0");
			channel.addItem("RF Channel 1");
			channel.addItem("RF Channel 2");
			channel.addItem("RF Channel 3");

			// create panel
			JLabel vl = new JLabel("Version");
			JLabel tl = new JLabel("Remote");

			JLabel wl = new JLabel("Width");
			JLabel hl = new JLabel("Height");

			AAPanel set = new AAPanel();
			set.define('f', factory, "fill=b;pad=3,3,3,3");
			set.define('c', config,  "fill=b;pad=3,3,3,3");
			set.define('m', home,    "fill=b;pad=3,3,3,3");
			set.define('V', vl,      "fill=b;pad=3,3,3,3");
			set.define('v', version, "fill=b;pad=3,3,3,3");
			set.setLayout(new String[] {
				"ffffffff",
				"cccccccc",
				"mmmmmmmm",
				"VV vvvvv",
			});

			AAPanel geom = new AAPanel();
			geom.define('W', wl,      "fill=b;pad=3,3,3,3");
			geom.define('H', hl,      "fill=b;pad=3,3,3,3");
			geom.define('w', width,   "fill=b;pad=3,3,3,3");
			geom.define('h', height,  "fill=b;pad=3,3,3,3");
			geom.setLayout(new String[] {
				"WW w HH h",
			});

			AAPanel trans = new AAPanel();
			trans.define('T', tcolor,  "fill=b;pad=3,3,3,3");
			trans.setLayout(new String[] {
				"T",
			});

			AAPanel cp = new AAPanel();
			cp.add(new LabelBox("Channel ID", channel), "x=0;y=1;pad=3,3,0,3;fill=b;wx=1;wy=1");
			cp.add(new LabelBox("Settings", set),       "x=0;y=2;pad=3,3,0,3;fill=b;wx=1");
			if (ext)
			{
			cp.add(new LabelBox("Panel Size", geom),    "x=0;y=3;pad=3,3,0,3;fill=b;wx=1");
			cp.add(new LabelBox("Transparent Color", trans),   "x=0;y=4;pad=3,3,0,3;fill=b;wx=1");
			}
			cp.add(new LabelBox("Home Icon", icon),     "x=0;y=5;pad=3,3,0,3;fill=b;wx=1;wy=1");

			// create dialog
			setContents(cp);
		}

		public void doOK()
		{
			ccf.setFactoryCCF(factory.isSelected());
			ccf.setConfigReadOnly(config.isSelected());
			ccf.setHomeReadOnly(home.isSelected());
			ccf.setVersionString(version.getText());
			ccf.getFirstHomeDevice().iconUnselected = icon.getCCFIcon();
			ccf.header().channelID = channel.getSelectedIndex();
			setCCFChanged();
			updateTreeSelection();
			if (ccf.getConformsTo(prefDefaultModel).isCustom())
			{
				panelSize = new Dimension(
					Integer.parseInt(width.getText()),
					Integer.parseInt(height.getText())
				);
				if (!panelSize.equals(ccf.getScreenSize(prefDefaultModel)))
				{
					ccf.setScreenSize(panelSize.width, panelSize.height);
					refreshAllPanels();
				}
			}
			ccf.setTransparentColor(tcolor.getColor());
		}

		public void doCancel()
		{
			icon.revert();
		}

		public Object getValue() { return null; }

		public void updateSystem()
		{
			if (ccf == null)
			{
				return;
			}
			factory.setSelected(ccf.isFactoryCCF());
			config.setSelected(ccf.isConfigReadOnly());
			home.setSelected(ccf.isHomeReadOnly());
			version.setText(ccf.getVersionString());
			channel.setEnabled(color() || header().isNewMarantz());
			channel.setSelectedIndex(Math.min(ccf.header().channelID,3));
			if (ccf.getFirstHomeDevice() != null)
			{
				icon.initButton(ccf.getFirstHomeDevice().iconUnselected);
			}
			ProntoModel pm = ccf.getConformsTo(prefDefaultModel);
			width.setEditable(pm.isCustom());
			height.setEditable(pm.isCustom());
			tcolor.setEditable(pm.isCustom());
			Dimension sz = ccf.getScreenSize(prefDefaultModel);
			width.setText(Integer.toString(sz.width));
			height.setText(Integer.toString(sz.height));
			tcolor.setColor(ccf.getTransparentColor(), color());
			show();
		}
	}

	// ---( Inner Class DeviceProps )---
	class DeviceProps extends MemoryDialog
	{
		private CCFDevice dev;
		private JTabbedPane tabs = new JTabbedPane();
		private NameAttr name = new NameAttr();
		private JComboBox extender = new JComboBox();
		private AAPanel flags = new AAPanel();
		private ActionPanel action = new ActionPanel();

		private JCB dev_ro = new JCB("Read Only", CCFDevice.READ_ONLY);
		private JCB dev_sp = new JCB("Separator", CCFDevice.HAS_SEPARATOR);
		private JCB dev_tm = new JCB("Template", CCFDevice.IS_TEMPLATE);
		private JCB dev_tg = new JCB("Timer Group", CCFDevice.IS_TIMER_GROUP);
		private JCB dev_np = new JCB("Needs Programming", CCFDevice.NEEDS_PROGRAMMING);

		private JPanel icons = new JPanel();
		private IconButton selIcon = new IconButton();
		private IconButton unsIcon = new IconButton();

		private Hashtable panels = new Hashtable();
		private int state = 0;

		class JCB extends JCheckBox
		{
			private int prop;

			JCB(String label, int prop)
			{
				super(label);
				this.prop = prop;
			}

			public void update()
			{
				setSelected(dev.getFlag(prop));
			}

			public void ok()
			{
				dev.setFlag(prop, isSelected());
			}
		}

		DeviceProps()
		{
			super("... Actions", PREF_DEVICEPROP_BOUNDS);

			flags.add(dev_ro, "x=0;y=0;fill=b;wx=1");
			flags.add(dev_tm, "x=0;y=1;fill=b;wx=1");
			flags.add(dev_np, "x=0;y=2;fill=b;wx=1");
			flags.add(dev_tg, "x=0;y=3;fill=b;wx=1");
			flags.add(dev_sp, "x=0;y=4;fill=b;wx=1");
			Util.setLabelBorder("Flags", flags);

			extender.addItem("IR - No Extender");
			extender.addItem("RF - Extender 0");
			extender.addItem("RF - Extender 1");
			extender.addItem("RF - Extender 2");
			extender.addItem("RF - Extender 3");
			extender.addItem("RF - Extender 4");
			extender.addItem("RF - Extender 5");
			extender.addItem("RF - Extender 6");
			extender.addItem("RF - Extender 7");
			extender.addItem("RF - Extender 8");
			extender.addItem("RF - Extender 9");
			extender.addItem("RF - Extender A");
			extender.addItem("RF - Extender B");
			extender.addItem("RF - Extender C");
			extender.addItem("RF - Extender D");
			extender.addItem("RF - Extender E");
			extender.addItem("RF - Extender F");
			Util.setLabelBorder("RF Extender", extender);

			icons.setLayout(new GridLayout(2,1,2,2));
			icons.add(new LabelBox("Selected Icon", selIcon));
			icons.add(new LabelBox("Unselected Icon", unsIcon));

			tabs.setTabPlacement(SwingConstants.RIGHT);
			setContents(tabs);

			bindAction(tabs, KeyEvent.VK_PAGE_UP, 0, new TabPager(tabs,-1));
			bindAction(tabs, KeyEvent.VK_PAGE_DOWN, 0, new TabPager(tabs,1));
		}

		private void checkTabs()
		{
			CCFHeader hdr = ccf.header();
			int nstate;
			if (hdr.isMarantz()) { nstate = 3; } else
			if (hdr.hasColor())  { nstate = 2; } else
				                 { nstate = 1; }
			if (nstate != state)
			{
				state = nstate;
				panels.clear();

				AAPanel props = new AAPanel();
				props.add(name,        "x=0;y=0;fill=b;wx=1");
				props.add(flags,       "x=0;y=1;fill=b;wx=1");
				props.add(extender,    "x=0;y=2;fill=b;wx=1");
				props.add(icons,       "x=0;y=3;fill=b;wx=1");
				props.add(new JPanel(),"x=0;y=4;fill=b;wx=1;wy=1");

				tabs.removeAll();
				tabs.addTab("Properties", props);
				tabs.addTab("On Select", action);
				
				bindAction(tabs, numkey[0], Event.ALT_MASK, new TabSelect(tabs,0));
				bindAction(tabs, numkey[1], Event.ALT_MASK, new TabSelect(tabs,1));

				CCFHardKey key[] = dev.getHardKeys();
				for (int i=0; i<key.length; i++)
				{
					ActionPanel ap = new ActionPanel(key[i].isNamable());
					panels.put(key[i].getLabel(), ap);
					tabs.addTab(key[i].getLabel(), ap);
					if (i+2 < numkey.length)
					{
						bindAction(tabs, numkey[i+2], Event.ALT_MASK, new TabSelect(tabs,i+2));
					}
					else
					{
						bindAction(tabs, numkey[i+2-numkey.length], Event.ALT_MASK|Event.SHIFT_MASK, new TabSelect(tabs,i+2));
					}
				}
				bindAction(tabs, KeyEvent.VK_HOME, 0, new TabSelect(tabs,0));
				bindAction(tabs, KeyEvent.VK_END, 0, new TabSelect(tabs,tabs.getTabCount()-1));

				tabs.setSelectedIndex(0);
			}
		}

		public void doOK()
		{
			dev.name = name.getName();
			dev.action = action.save();
			dev.rfExtender = extender.getSelectedIndex();

			CCFHardKey key[] = dev.getHardKeys();
			for (int i=0; i<key.length; i++)
			{
				ActionPanel ap = (ActionPanel)panels.get(key[i].getLabel());
				if (ap == null)
				{
					continue;
				}
				key[i].setActionList(ap.save());
				if (key[i].isNamable())
				{
					key[i].setName(ap.getName());
				}
			}

			dev.iconSelected = selIcon.getCCFIcon();
			dev.iconUnselected = unsIcon.getCCFIcon();

			dev_ro.ok();
			dev_sp.ok();
			dev_tm.ok();
			dev_tg.ok();
			dev_np.ok();
			setCCFChanged();
			updateTreeSelection();
		}

		public void doCancel() { }

		public void updateDevice(CCFDevice dev)
		{
			this.dev = dev;
			checkTabs();
			setTitle(dev.getName()+" Properties");
			name.setName(dev.getName());
			action.update(dev.action);
			extender.setEnabled(color() || header().isNewMarantz());
			extender.setSelectedIndex(dev.rfExtender);

			CCFHardKey key[] = dev.getHardKeys();
			for (int i=0; i<key.length; i++)
			{
				ActionPanel ap = (ActionPanel)panels.get(key[i].getLabel());
				if (ap == null)
				{
					continue;
				}
				if (key[i].isNamable())
				{
					ap.update(key[i].getActionList(), key[i].getName());
				}
				else
				{
					ap.update(key[i].getActionList());
				}
			}

			selIcon.initButton(dev.iconSelected);
			unsIcon.initButton(dev.iconUnselected);

			tabs.setSelectedIndex(0);
			dev_ro.setEnabled(dev.isNormalDevice());
			dev_ro.update();
			dev_tm.setEnabled(dev.isNormalDevice());
			dev_tm.update();
			dev_np.setEnabled(dev.isNormalDevice());
			dev_np.update();
			dev_sp.setEnabled(!dev.isHomeDevice());
			dev_sp.update();
			dev_tg.setEnabled(dev.isMacroDevice());
			dev_tg.update();
			show();
		}
	}


	// ---( Inner Class PanelProps )---
	class PanelProps extends OKCancelDialog
	{
		private CCFPanel panel;
		private NameAttr name = new NameAttr();
		private JCheckBox hidden = new JCheckBox("Hidden");

		PanelProps()
		{
			super("... Actions");

			AAPanel flag = new AAPanel();
			flag.add(hidden, "x=0;y=0;fill=b;wx=1");
			Util.setLabelBorder("Flags", flag);

			AAPanel prop = new AAPanel();
			prop.add(name,         "x=0;y=0;fill=b;wx=1");
			prop.add(flag,         "x=0;y=1;fill=b;wx=1");
			prop.add(new JPanel(), "x=0;y=2;fill=b;wx=1;wy=1");

			setContents(prop);
		}

		public void doOK()
		{
			panel.setName(name.getName());
			panel.setHidden(hidden.isSelected());
			refreshPanel(panel);
			((CCFTreePanel)getTreeWrapper(panel)).getParent().refresh();
			setCCFChanged();
			updateTreeSelection();

		}

		public void doCancel() { }

		public void updatePanel(CCFPanel panel)
		{
			this.panel = panel;
			setTitle(panel.getName()+" Properties");
			name.setName(panel.getName());
			hidden.setSelected(panel.isHidden());
			show();
		}

		public Dimension getMinimumSize()
		{
			Dimension d = super.getMinimumSize();
			return new Dimension(Math.max(d.width,200), Math.max(d.height,125));
		}

		public Dimension getPreferredSize()
		{
			Dimension d = super.getPreferredSize();
			return new Dimension(Math.max(d.width,200), Math.max(d.height,125));
		}
	}

	// ---( Inner Class FrameProps )---
	class FrameProps extends MemoryDialog
	{
		private boolean did;
		private FrameBox box;
		private CCFFrame frame;
		private GeomPanel geom = new GeomPanel();
		private NameFontAttr name = new NameFontAttr();
		private IconButton icon = new IconButton();
		private ColorChooser color = new ColorChooser();

		FrameProps()
		{
			super("... Actions", PREF_FRAMEPROP_BOUNDS);

			JComponent ibtn = getButton("Icon and Color", icon, color);

			AAPanel prop = new AAPanel();
			prop.add(name, "y=0;fill=b;wx=1");
			prop.add(geom, "y=1;fill=b;wx=1");
			prop.add(ibtn, "y=2;fill=b;wx=1;wy=1");

			setContents(prop);
		}

		public JComponent getButtons()
		{
			return UIUtils.getOtherOKCancel(new String[] { "Apply" }, this);
		}

		public boolean handleCmd(String cmd)
		{
			if (cmd != null)
			{
				if (cmd.equals("Apply"))
				{
					doOK();
					return false;
				}
			}
			return false;
		}

		public void doOK()
		{
			if (did)
			{
				popDo();
			}
			int id = startMultiDo();
			DoNodeUpdate doUpdate = new DoNodeUpdate(frame);
			frame.name = name.getName();
			frame.icon = icon.getCCFIcon();
			frame.setFont(name.getCCFFont());
			frame.setForeground(color.getFG());
			frame.setBackground(color.getBG());
			frame.setTextAlignment(name.getAlignment());
			frame.setTextWrap(name.getWrap());
			geom.commit();
			doUpdate.getNewState();
			pushDo(doUpdate);
			endMultiDo(id);
			objectStatus.setObject(box);
			did = true;
		}

		public void doCancel()
		{
			if (did)
			{
				popDo();
			}
		}

		private JComponent getButton(
			String title, IconButton button, ColorChooser colors)
		{
			JPanel p = new JPanel();
			p.setLayout(new BorderLayout(2,2));
			p.add("Center", button);
			p.add("South", colors);
			return new LabelBox(title, p);
		}

		public void updateFrame(FrameBox box, CCFFrame frame)
		{
			this.did = false;
			this.box = box;
			this.frame = frame;
			String nm = frame.getName();
			setTitle("Frame "+(nm != null ? "'"+nm+"' " : "")+"Properties");
			name.setName(frame.getName());
			name.setCCFFont(frame.getFont());
			name.setAlignment(frame.getTextAlignment());
			name.setWrap(frame.getTextWrap());
			geom.setChild(box);
			icon.initButton(frame.icon);
			color.setColors(frame.colors);
			color.setWebSafe(prefShowWebSafe);
			show();
		}
	}

	// ---( Inner Class ButtonProps )---
	class ButtonProps extends MemoryDialog
	{
		private boolean did;
		private ButtonBox box;
		private CCFButton button;
		private JTabbedPane tabs = new JTabbedPane();
		private NameFontAttr name = new NameFontAttr();
		private NameAttr idtag = new NameAttr("ID Tag");
		private GeomPanel geom = new GeomPanel();
		private ActionPanel action = new ActionPanel();
		private IconButton IAUSIcon = new MyIconButton(this, 100, 50);
		private IconButton IASEIcon = new MyIconButton(this, 100, 50);
		private IconButton ACUSIcon = new MyIconButton(this, 100, 50);
		private IconButton ACSEIcon = new MyIconButton(this, 100, 50);
		private ColorChooser IAUSColor = new ColorChooser();
		private ColorChooser IASEColor = new ColorChooser();
		private ColorChooser ACUSColor = new ColorChooser();
		private ColorChooser ACSEColor = new ColorChooser();

		ButtonProps()
		{
			super("... Actions", PREF_BUTTONPROP_BOUNDS);

			JTabbedPane icons = new JTabbedPane(JTabbedPane.TOP);
			JPanel ay = new JPanel(new GridLayout(1,2,2,2));
			JPanel an = new JPanel(new GridLayout(1,2,2,2));
			an.add(getButton("Selected", IASEIcon, IASEColor));
			an.add(getButton("Unselected", IAUSIcon, IAUSColor));
			ay.add(getButton("Selected", ACSEIcon, ACSEColor));
			ay.add(getButton("Unselected", ACUSIcon, ACUSColor));
			icons.add("Active", ay);
			icons.add("Inactive", an);

			AAPanel prop = new AAPanel();
			prop.add(name,  "y=0;fill=b;wx=1;wy=0");
			prop.add(geom,  "y=1;fill=b;wx=1;wy=0");
			prop.add(icons, "y=2;fill=b;wx=1;wy=1");
			prop.add(idtag, "y=3;fill=b;wx=1;wy=0");

			// add tabs
			tabs.addTab("Properties", prop);
			tabs.addTab("Actions", action);
			tabs.setSelectedIndex(1);

			bindAction(tabs, numkey[0], Event.ALT_MASK, new AbstractAction() {
				public void actionPerformed(ActionEvent ae) {
					tabs.setSelectedIndex(0);
				}
			});
			bindAction(tabs, numkey[1], Event.ALT_MASK, new AbstractAction() {
				public void actionPerformed(ActionEvent ae) {
					tabs.setSelectedIndex(1);
				}
			});

			setContents(tabs);
		}

		public JComponent getButtons()
		{
			return UIUtils.getOtherOKCancel(new String[] { "Apply" }, this);
		}

		public boolean handleCmd(String cmd)
		{
			if (cmd != null)
			{
				if (cmd.equals("Apply"))
				{
					doOK();
					return false;
				}
			}
			return false;
		}

		public void doOK()
		{
			if (did)
			{
				popDo();
			}

			int id = startMultiDo();
			DoNodeUpdate doUpdate = new DoNodeUpdate(button);

			try {

			button.setName(name.getName());
			button.setFont(name.getCCFFont());
			button.setIDTag(idtag.getName());
			button.setTextAlignment(name.getAlignment());
			button.setTextWrap(name.getWrap());
			button.actions = action.save();
			button.iconIU = IAUSIcon.getCCFIcon();
			button.iconIS = IASEIcon.getCCFIcon();
			button.iconAU = ACUSIcon.getCCFIcon();
			button.iconAS = ACSEIcon.getCCFIcon();
			button.colorIU = IAUSColor.getColors();
			button.colorIS = IASEColor.getColors();
			button.colorAU = ACUSColor.getColors();
			button.colorAS = ACSEColor.getColors();
			geom.commit();

			} catch (Exception ex) { ex.printStackTrace(); }

			doUpdate.getNewState();
			pushDo(doUpdate);
			endMultiDo(id);
			objectStatus.setObject(box);
			did = true;
		}

		public void doCancel()
		{
			if (did)
			{
				popDo();
			}
		}

		private JComponent getButton(
			String title, IconButton button, ColorChooser colors)
		{
			JPanel p = new JPanel();
			p.setLayout(new BorderLayout(2,2));
			p.add("Center", button);
			p.add("South", colors);
			return new LabelBox(title, p);
		}

		public void updateButton(ButtonBox box, CCFButton button)
		{
			this.did = false;
			this.box = box;
			this.button = button;
			String nm = button.getName();
			setTitle("Button "+(nm != null ? "'"+nm+"' " : "")+"Properties");
			action.update(button.actions);
			name.setName(button.getName());
			name.setCCFFont(button.getFont());
			name.setAlignment(button.getTextAlignment());
			name.setWrap(button.getTextWrap());
			idtag.setName(button.getIDTag());
			geom.setChild(box);
			IAUSIcon.initButton(button.iconIU);
			IASEIcon.initButton(button.iconIS);
			ACUSIcon.initButton(button.iconAU);
			ACSEIcon.initButton(button.iconAS);
			IAUSColor.setColors(button.colorIU);
			IASEColor.setColors(button.colorIS);
			ACUSColor.setColors(button.colorAU);
			ACSEColor.setColors(button.colorAS);
			IAUSColor.setWebSafe(prefShowWebSafe);
			IASEColor.setWebSafe(prefShowWebSafe);
			ACUSColor.setWebSafe(prefShowWebSafe);
			ACSEColor.setWebSafe(prefShowWebSafe);
			show();
		}
	}

	// ---( Inner Class NameDialog )---
	static class NameDialog extends OKCancelDialog implements ActionListener
	{
		private JTextComponent field;
		private String result;
		private JTabbedPane tabs;

		NameDialog()
		{
			this(true);
		}

		NameDialog(boolean bind)
		{
			super("Object Name", bind);
			result = "";

			AAPanel c = new AAPanel();

			// hack to allow multi-line text (netremote)
			if (!bind)
			{
				field = new JTextArea(2,30);
			}
			else
			{
				field = new JTextField("");
			}
			field.setFont(getPFont());
			setRCMenu(field);

			tabs = new JTabbedPane();
			final Font font = getPFont();

			for (int p=0; p<7; p++)
			{
				JPanel keys = new JPanel();
				keys.setLayout(new GridLayout(6,6,2,2));
				for (int i=0; i<32; i++)
				{
					ActionButton b = new ActionButton((char)(i+(p*32)+32)+"") {
						public void action() {
							insert(this.getText());
						}
					};
					b.addActionListener(this);
					b.setFont(font);
					b.setForeground(Color.black);
					keys.add(b);
				}
				tabs.add("Set"+(p+1), keys);
			}

			// hack to allow multi-line text (netremote)
			if (bind)
			{
				c.define('f', field, "pad=5,5,5,5;fill=b;wx=1");
			}
			else
			{
				c.define('f', new JScrollPane(field), "pad=5,5,5,5;fill=b;wx=1");
			}
			c.define('t', tabs,  "pad=0,5,5,5;fill=b;wy=1");
			c.setLayout(new String[] { "f", "t" });

			setContents(c);

			Dimension d = getSize();
			setSize(d.width+75, d.height);
		}

		public void insert(String text)
		{
			field.replaceSelection(text);
		}

		public void doOK()
		{
			result = field.getText();
		}

		public void doCancel()
		{
		}

		public String getText(String defText)
		{
			return getText("Object Name", defText, true);
		}

		public String getText(String defText, boolean ext)
		{

			return getText("Object Name", defText, ext);
		}

		public String getText(String title, String defText)
		{
			return getText(title, defText, true);
		}

		public String getText(String title, String defText, boolean ext)
		{
			ext = ext || color() || header().isNewMarantz();
			setTitle(title);
			result = defText != null ? defText : "";
			field.setText(result);
			field.setCaretPosition(result.length());
			for (int i=3; i<7; i++)
			{
				tabs.setEnabledAt(i, ext);
			}
			show();
			return result;
		}

		public void showHook(JDialog d)
		{
			// try real damn hard to get text to auto-highlight on OSX
			SwingUtilities.invokeLater(new Runnable() { public void run() {
				field.requestFocus();
				field.setSelectionStart(0);
				field.setSelectionEnd(result.length());
			} } );
			d.addWindowListener(new WindowAdapter() {
				public void windowActivated(WindowEvent we) {
					field.requestFocus();
					field.setSelectionStart(0);
					field.setSelectionEnd(result.length());
				}
				public void windowOpened(WindowEvent we) {
					field.requestFocus();
					field.setSelectionStart(0);
					field.setSelectionEnd(result.length());
				}
			});
		}
	}

	// ---( Inner Class BeepValueDialog )---
	class BeepValueDialog extends OKCancelDialog
	{
		private ActionBeep beep;
		private JSlider dur, freq, cycle;

		BeepValueDialog(ActionBeep beep)
		{
			super("Beep");
			this.beep = beep;

			dur = new JSlider(0, 2550, beep.getDuration());
			freq = new JSlider(200, 10000, beep.getFrequency());
			cycle = new JSlider(0, 99, beep.getDutyCycle());

			dur.setPaintLabels(true);
			freq.setPaintLabels(true);
			cycle.setPaintLabels(true);

			MyLabel dl = new MyLabel("000");
			MyLabel fl = new MyLabel("00000");

			JLabel dll = new JLabel("ms");
			JLabel fll = new JLabel("Hz");

			dur.addChangeListener(dl);
			freq.addChangeListener(fl);

			AAPanel p = new AAPanel();
			p.define('1', dl,                                    "pad=5,5,3,3;fill=h");
			p.define('2', fl,                                    "pad=5,5,3,3;fill=h");
			p.define('3', dll,                                   "pad=5,3,3,3;fill=h");
			p.define('4', fll,                                   "pad=5,3,3,3;fill=h");
			p.define('D', new JLabel("Duration", JLabel.RIGHT),  "pad=5,5,3,3;fill=h");
			p.define('F', new JLabel("Frequency", JLabel.RIGHT), "pad=5,5,3,3;fill=h");
			p.define('C', new JLabel("Volume", JLabel.RIGHT),    "pad=5,5,3,3;fill=h");
			p.define('d', dur,   "pad=5,3,3,5;wx=1;fill=h");
			p.define('f', freq,  "pad=5,3,3,5;wx=1;fill=h");
			p.define('c', cycle, "pad=5,3,3,5;wx=1;fill=h");

			p.setLayout(new String[] {
				" DD ddddd 11 33 ",
				" FF fffff 22 44 ",
				" CC ccccc       ",
			});

			setContents(p);

			dl.setText(Integer.toString(dur.getValue()));
			fl.setText(Integer.toString(freq.getValue()));
		}

		public void doOK()
		{
		}

		public void doCancel()
		{
		}

		public int getBeepValue()
		{
			return beep.createBeep(dur.getValue(), freq.getValue(), cycle.getValue());
		}
	}

	// ---( Inner Class StringValueDialog )---
	class StringValueDialog extends OKCancelDialog
	{
		private JTextComponent text;
		private String value;

		StringValueDialog(String title, String value, int col)
		{
			super(title);
			if (col > 0)
			{
				text = new JTextArea(7, col);
				text.setFont(new Font("Courier", Font.PLAIN, 10));
			}
			else
			{
				text = new JTextField(value != null ? value.length() : 10);
				((JTextField)text).addActionListener(this);
			}
			setRCMenu(text);
			setValue(value);
			JPanel p = new JPanel();
			p.setLayout(new BorderLayout(5,5));
			p.add("West", new JLabel(title));
			if (col > 0)
			{
				p.add("Center", new JScrollPane(text));
			}
			else
			{
				p.add("Center", text);
			}
			p.setBorder(new EmptyBorder(3,3,3,3));
			setContents(p);
		}

		public void actionPerformed(ActionEvent ae)
		{
			if (ae.getSource() == text)
			{
				enterOK();
				return;
			}
			super.actionPerformed(ae);
		}
	
		public void doOK()
		{
			value = text.getText();
		}

		public void doCancel()
		{
		}

		public void setValue(String val)
		{
			text.setText(val != null ? val.trim() : "");
			value = val;
		}

		public String getValue()
		{
			return value;
		}
	}

	// ---( Inner Class AliasChoice )---
	static class AliasChoice extends MemoryDialog
	{
		private CCFAction act;
		private JList combo;
		private Object sel;
		private JScrollPane scroll;

		AliasChoice(CCFAction act)
		{
			super("Choose Action Alias", PREF_ALIAS_BOUNDS);
			this.act = act;
			Vector v = new Vector();
			combo = new JList(new VectorListModel(v));
			combo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			combo.setVisibleRowCount(10);
			combo.setFont(getPFont());
			combo.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (me.getClickCount() == 2) { enterOK(); }
				}
			});

			CCFHeader hdr = ccf().header().getHeader();
			addDeviceAliases(v, hdr.firstHome);
			addDeviceAliases(v, hdr.firstDevice);
			addDeviceAliases(v, hdr.firstMacro);
			Object last = state().lastAlias;
			if (act.action1 != null || last == null)
			{
				switch (act.type)
				{
					case CCFAction.ACT_ALIAS_KEY:
						combo.setSelectedValue(new DeviceKey((CCFDevice)act.action1, act.p2), true);
						break;
					case CCFAction.ACT_ALIAS_BUTTON:
						combo.setSelectedValue(act.action2,true);
						break;
					case CCFAction.ACT_ALIAS_DEV:
						combo.setSelectedValue(act.action1,true);
						break;
				}
			}
			else
			{
				combo.setSelectedValue(last,true);
			}
			scroll = new JScrollPane(combo, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			setVisible();
			setContents(scroll);
		}

		private void setVisible()
		{
			int idx = combo.getSelectedIndex();
			JViewport vp = scroll.getViewport();
			Rectangle r = combo.getCellBounds(idx,Math.min(idx+1,combo.getModel().getSize()-1));
			if (r != null)
			{
				vp.scrollRectToVisible(r);
			}
		}

		public Rectangle defaultSize()
		{
			return new Rectangle(0,0,400,200);
		}

		public void showHook(JDialog d)
		{
			d.addWindowListener(new WindowAdapter() {
				public void windowOpened(WindowEvent we) {
					setVisible();
				}
			});
			super.showHook(d);
		}

		private void addDeviceAliases(Vector v, CCFDevice d)
		{
			if (d == null)
			{
				return;
			}
			v.addElement(d);
			CCFHardKey keys[] = d.getHardKeys();
			for (int i=0; i<keys.length; i++)
			{
				v.addElement(new DeviceKey(d, i, keys[i].getName()));
			}
			CCFPanel p[] = d.getPanels();
			for (int i=0; i<p.length; i++)
			{
				CCFButton b[] = p[i].getButtons();
				for (int j=0; j<b.length; j++)
				{
					v.addElement(b[j]);
				}
			}
			addDeviceAliases(v, d.next);
		}

		public void doOK()
		{
			sel = combo.getSelectedValue();
			if (sel instanceof CCFButton)
			{
				CCFButton button = (CCFButton)sel;
				act.type = CCFAction.ACT_ALIAS_BUTTON;
				act.action1 = button.getParentDevice();
				act.action2 = button;
			}
			else
			if (sel instanceof DeviceKey)
			{
				DeviceKey dk = (DeviceKey)sel;
				act.type = CCFAction.ACT_ALIAS_KEY;
				act.action1 = dk.device;
				act.p2 = dk.key;
			}
			else
			if (sel instanceof CCFDevice)
			{
				act.type = CCFAction.ACT_ALIAS_DEV;
				act.action1 = (CCFDevice)sel;
			}
			state().lastAlias = sel;
		}

		public boolean invoke()
		{
			return super.invoke() && act.action1 != null;
		}

		public boolean invoke(CCFActionList list)
		{
			return super.invoke() && act.action1 != null && !actionReferences(act, list);
		}

		public void doCancel() { }

		public Object getValue()
		{
			return sel;
		}

		private boolean actionReferences(CCFAction act, CCFActionList list)
		{
			CCFActionList src = null;
			switch (act.type)
			{
				case CCFAction.ACT_ALIAS_BUTTON:
					src = ((CCFButton)act.action2).actions;
					break;
				case CCFAction.ACT_ALIAS_DEV:
					src = ((CCFDevice)act.action1).action;
					break;
				case CCFAction.ACT_ALIAS_KEY:
					src = ((CCFDevice)act.action1).getHardKeys()[act.p2].getActionList();
					break;
			}
			if (src == list)
			{
				errorDialog("Alias denied. This would create\nan infinite recursive loop.");
				debug.log(0, "attempted recursive alias: ("+act+") references ("+list+")");
				return true;
			}
			if (src != null && src.action != null)
			{
				for (int i=0; i<src.action.length; i++)
				{
					if (actionReferences(src.action[i], list))
					{
						return true;
					}
				}
			}
			return false;
		}
	}

	// ---( Inner Class JumpChoice )---
	class JumpChoice extends MemoryDialog
	{
		private JScrollPane scroll;
		private JList combo;
		private Object sel;

		JumpChoice(CCFAction act)
		{
			super("Choose JumpTo Panel", PREF_JUMP_BOUNDS);

			if (act.isSpecialJump())
			{
				sel = new JumpWrapper(act.p2);
			}
			else
			{
				sel = new PanelWrapper((CCFPanel)act.action2);
			}

			Vector v = new Vector();
			v.addElement(new JumpWrapper(CCFAction.JUMP_SCROLL_DOWN));
			v.addElement(new JumpWrapper(CCFAction.JUMP_SCROLL_UP));
			v.addElement(new JumpWrapper(CCFAction.JUMP_MOUSE_MODE));
			if (ccf.header().isMarantz())
			{
				v.addElement(new JumpWrapper(CCFAction.JUMP_FORWARD));
				v.addElement(new JumpWrapper(CCFAction.JUMP_BACK));
			}
			addPanelJumps(v, ccf.getFirstHomeDevice());
			addPanelJumps(v, ccf.getFirstDevice());
			addPanelJumps(v, ccf.getFirstMacroDevice());

			combo = new JList(new VectorListModel(v));
			combo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			combo.setVisibleRowCount(10);
			combo.setFont(getPFont());
			combo.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (me.getClickCount() == 2) { enterOK(); }
				}
			});

			scroll = new JScrollPane(combo, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			combo.setSelectedValue(sel, true);
			setVisible();
			setContents(scroll);
		}

		public Rectangle defaultSize()
		{
			return new Rectangle(0,0,400,200);
		}

		private void setVisible()
		{
			int idx = combo.getSelectedIndex();
			JViewport vp = scroll.getViewport();
			Rectangle r = combo.getCellBounds(idx,Math.min(idx+1,combo.getModel().getSize()-1));
			if (r != null)
			{
				vp.scrollRectToVisible(r);
			}
		}

		public void showHook(JDialog d)
		{
			d.addWindowListener(new WindowAdapter() {
				public void windowOpened(WindowEvent we) {
					setVisible();
				}
			});
			super.showHook(d);
		}

		public Dimension getPreferredSize()
		{
			return getMinimumSize();
		}

		public Dimension getMiminumSize()
		{
			Dimension d = super.getPreferredSize();
			return new Dimension(Math.min(250, d.width), d.height);
		}

		private void addPanelJumps(Vector v, CCFDevice d)
		{
			while (d != null)
			{
				CCFPanel p[] = d.getPanels();
				for (int i=0; i<p.length; i++)
				{
					PanelWrapper pw = new PanelWrapper(p[i]);
					if (sel.equals(p[i]))
					{
						sel = pw;
					}
					v.addElement(pw);
				}
				d = d.next;
			}	
		}

		public void doOK()
		{
			sel = combo.getSelectedValue();
		}

		public void doCancel() { }

		public boolean setActionValue(CCFAction act)
		{
			if (act == null || sel == null)
			{
				return false;
			}
			act.p1 = 0;
			act.p2 = 0;
			act.action1 = null;
			act.action2 = null;
			if (sel instanceof PanelWrapper)
			{
				CCFPanel p = ((PanelWrapper)sel).panel;
				if (p != null)
				{
					act.action1 = p.getParentDevice();
					act.action2 = p;
				}
				else
				{
					return false;
				}
			}
			else
			if (sel != null)
			{
				act.p2 = ((JumpWrapper)sel).value;
			}
			else
			{
				return false;
			}
			return true;
		}
	}

	// ---( Inner Class IconReplace )---
	class IconReplace extends OKCancelDialog
	{
		private IconButton oldButton = new MyIconButton(this);
		private IconButton newButton = new MyIconButton(this);

		IconReplace()
		{
			super("Replace Icon");
			AAPanel ap = new AAPanel();
			ActionButton replace = new ActionButton("Replace") {
				public void action() {
					doOK();
				}
			};
			JLabel txt = new JLabel("Replace Old Icon with New", JLabel.CENTER);
			ap.define('0',                     "fill=b;pad=3,3,3,3");
			ap.define('t', txt,                "fill=b;pad=3,3,3,3");
			ap.define('-', new JLabel("-->"),  "fill=b;pad=3,3,3,3");
			ap.define('o', oldButton,          "fill=b;pad=3,3,3,3;wx=1");
			ap.define('n', newButton,          "fill=b;pad=3,3,3,3;wx=1");
			ap.define('r', replace,            "fill=b;pad=3,3,3,3;wx=0");
			ap.setLayout(new String[] {
				"0000000000000",
				"0 ttttttttt 0",
				"0 ooo - nnn 0",
				"0 rrrrrrrrr 0",
				"0000000000000",
			});

			setContents(ap);
		}

		public void doOK()
		{
			debug.log(0, "replace OK");
			iconReplace(oldButton.getCCFIcon(), newButton.getCCFIcon());
			/*
			final CCFIcon oldIcon = oldButton.getCCFIcon();
			final CCFIcon newIcon = newButton.getCCFIcon();
			new CCFWalker(ccf).walk(new IWalker()
			{
				public void onNode(CCFNode node)
				{
					if (node == null) { return; }

					boolean changed = false;

					try {

					Field f[] = node.getClass().getDeclaredFields();
					for (int i=0; i<f.length; i++)
					{
						if (f[i].getType() == CCFIcon.class && f[i].get(node) == oldIcon)
						{
							f[i].set(node, newIcon);
						}
					}

					}
					catch (Exception ex) { debug(ex); }

					if (changed) { setCCFChanged(); }
				}
			});
			*/
			refreshAllPanels();
		}

		public void doCancel()
		{
		}

		public Dimension getMinimumSize()
		{
			return max(super.getMinimumSize(), 350, 350);
		}

		public Dimension getPreferredSize()
		{
			return max(super.getPreferredSize(), 350, 350);
		}

		public Dimension getMaximumSize()
		{
			return max(super.getMaximumSize(), 350, 350);
		}
	}

	// ---( Inner Class IconLibrary )---
	class IconLibrary extends MemoryDialog
	{
		private JList list;
		private CCFIcon selected;
		private ActionButton edit;
		private Hashtable hash = new Hashtable();
		private NoIcon noIcon = new NoIcon();
		private LineBorder b1 = new LineBorder(Color.black, 1);
		private LineBorder b2 = new LineBorder(Color.gray, 1);
		private Vector items = new Vector();
		private VectorListModel model = new VectorListModel(items);
		private JLabel geom = new MyLabel("000x000");
		private JLabel size = new MyLabel("000000");
		private JLabel bits = new MyLabel("00");
		private JCheckBox comp = new JCheckBox("compressed");

		// ---( Inner Class NoIcon )---
		class NoIcon extends JLabel
		{
			NoIcon()
			{
				super("No Icon", CENTER);
			}
			
			public Dimension getMinimumSize()
			{
				return max(super.getMinimumSize(), 0, 30);
			}

			public Dimension getPreferredSize()
			{
				return max(super.getPreferredSize(), 0, 30);
			}

			public Dimension getMaximumSize()
			{
				return max(super.getMaximumSize(), 0, 30);
			}
		};

		IconLibrary()
		{
			super("Icon Library", PREF_ICONLIB_BOUNDS);
			load(null);
			ActionButton load = new ActionButton("Load") {
				public void action() { iconLoad(); }
			};
			ActionButton save = new ActionButton("Save") {
				public void action() { iconSave(); }
			};
			edit = new ActionButton("Edit") {
				public void action() { iconEdit(); }
			};
			model = new VectorListModel(items);
			list = new JList(model);
			list.setCellRenderer(new DefaultListCellRenderer() {
				public Component getListCellRendererComponent(
					JList list, Object val, int idx, boolean sel, boolean foc)
				{
					selected = (CCFIcon)items.get(idx);
					JComponent c = selected != null ? (JComponent)hash.get(selected) : noIcon;
					c.setBorder(sel ? b1 : b2);
					c.setBackground((sel && c != noIcon) ? noIcon.getForeground() : Color.lightGray);
					return c;
				}
			});
			list.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					updateStats(list.getSelectedValue());
				}
			});
			comp.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					boolean c = comp.isSelected();
					if (c != selected.isCompressed()) {
						selected.setCompressed(c);
						updateStats(selected);
					}
				}
			});
			JScrollPane scroll = new JScrollPane(list,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			AAPanel ap = new AAPanel();
			JLabel xy = new JLabel("Dim"); xy.setForeground(Color.black);
			JLabel bs = new JLabel("Size"); bs.setForeground(Color.black);
			JLabel bl = new JLabel("Bits"); bl.setForeground(Color.black);
			AAPanel stat = new AAPanel(true);
			stat.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
			ap.define('0',         "fill=b;pad=3,3,3,3");
			ap.define('1', stat,   "fill=h;pad=3,3,3,3");
			ap.define('x', scroll, "fill=b;pad=3,3,3,3;wx=1;wy=1");
			ap.define('l', load,   "pad=3,3,3,3");
			ap.define('s', save,   "pad=3,3,3,3");
			ap.define('e', edit,   "pad=3,3,3,3");
			ap.define('2', geom,   "pad=3,3,3,3");
			ap.define('3', size,   "pad=3,3,3,3");
			ap.define('4', bits,   "pad=3,3,3,3");
			ap.define('5', bs,     "pad=3,3,3,3;fill=b");
			ap.define('6', bl,     "pad=3,3,3,3;fill=b");
			ap.define('7', comp,   "pad=3,6,3,3;fill=b");
			ap.define('8', xy,     "pad=3,3,3,3;fill=b");
			ap.setLayout(new String[] {
				"00000000000000000",
				"0 lll  sss  eee 0",
				"00000000000000000",
				"11111111111111111",
				"1 8 2 5 3 6 4 7 1",
				"11111111111111111",
				" xxxxxxxxxxxxxxx ",
			});

			setContents(ap);
		}

		void updateStats(Object o)
		{
			if (o instanceof CCFIcon)
			{
				CCFIcon c = (CCFIcon)o;
				geom.setText(c.width+"x"+c.height);
				size.setText(Integer.toString(c.data.length+8));
				bits.setText(c.isFullColor() ? "32" : c.isColor() ? "8" : c.isGray() ? "2" : "1");
				comp.setSelected(c.isCompressed());
				comp.setEnabled(c.isCompressible());
			}
			else
			{
				geom.setText("0x0");
				size.setText("0");
				bits.setText("0");
				comp.setSelected(false);
				comp.setEnabled(false);
			}
		}

		boolean invoke(CCFIcon def)
		{
			selected = def;
			list.setSelectedValue(def, true);
			return invoke();
		}

		public void clearIconCaches()
		{
			if (color())
			{
				return;
			}
			for (int i=0; i<items.size(); i++)
			{
				Object o = items.get(i);
				debug.log(0, "icon@"+i+"="+o);
				if (o instanceof CCFIcon)
				{
					((CCFIcon)o).clearCache();
				}
			}
		}

		public boolean invoke()
		{
			edit.setEnabled(prefImageEditor != null);
			model.refresh();
			return super.invoke();
		}

		void iconLoad()
		{
			File f[] = getFiles(
				"images (*.gif, *.jpg, *.bmp, *.png)",
				new String[] { "gif", "jpg", "bmp", "png" }, prefImageDir);
			if (f != null && f.length > 0)
			{
				for (int i=0; i<f.length; i++)
				try
				{
					CCFIcon ic = CCFIcon.create(f[i].getAbsolutePath(), iconMode());
					if (!items.contains(ic))
					{
						if (ic.width > panelSize.width*2 || ic.height > panelSize.height*2)
						{
							errorDialog("Icon is too large");
							return;
						}
						addIcon(ic);
						model.refresh();
						refreshTreeModel();
					}
					list.setSelectedValue(ic, true);
				}
				catch (Exception ex)
				{
					debug(ex);
				}
			}
		}

		void iconSave()
		{
			CCFIcon selected = (CCFIcon)list.getSelectedValue();
			if (selected == null)
			{
				return;
			}
			File f = getFile(false, "images (*.gif)", new String[] { "gif" }, prefImageDir);
			if (f != null)
			{
				try
				{
					if (!f.toString().toLowerCase().endsWith(".gif"))
					{
						f = new File(f.toString()+".gif");
					}
					selected.saveGIF(f.toString());
				}
				catch (Exception ex)
				{
					debug(ex);
				}
			}
		}

		void iconEdit()
		{
			CCFIcon selected = (CCFIcon)list.getSelectedValue();
			if (selected != null && prefImageEditor != null)
			{
				try
				{

				final File tmpFile = File.createTempFile("tonto",".gif");
				selected.saveGIF(tmpFile.toString());
				final Process p = Runtime.getRuntime().exec(prefImageEditor+" "+tmpFile.toString());

				new Thread() { public void run() {
					try {
						p.waitFor();
						sleep(2000);
						tmpFile.delete();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				} }.start();

				}
				catch (Exception ex)
				{
					debug(ex);
				}
			}
		}

		void load(CCF ccf)
		{
			selected = null;
			hash.clear();
			items.setSize(0);
			items.add(null);
			append(ccf);
			model.refresh();
		}

		void append(CCF ccf)
		{
			if (ccf != null)
			{
				CCFHeader hdr = ccf.header();
				addDevice(hdr.firstHome);
				addDevice(hdr.firstDevice);
				addDevice(hdr.firstMacro);
				addPanel(hdr.macroPanel);
				refreshTreeModel();
			}
		}

		void addDevice(CCFDevice d)
		{
			if (d != null)
			{
				addPanel(d.getFirstPanel());
				addDevice(d.getNextDevice());
				addIcon(d.iconSelected);
				addIcon(d.iconUnselected);
			}
		}

		void addPanel(CCFPanel p)
		{
			if (p != null)
			{
				addChildren(p.getChildren());
				addPanel(p.getNextPanel());
			}
		}

		void addChildren(CCFChild c[])
		{
			if (c != null)
			{
				for (int i=0; i<c.length; i++)
				{
					addChild(c[i]);
				}
			}
		}

		void addChild(CCFChild c)
		{
			if (c != null)
			{
				switch (c.getType())
				{
					case CCFChild.FRAME: addFrame(c.getFrame()); break;
					case CCFChild.BUTTON: addButton(c.getButton()); break;
				}
			}
		}

		void addFrame(CCFFrame f)
		{
			if (f != null)
			{
				addIcon(f.getIcon());
				addChildren(f.getChildren());
			}
		}

		void addButton(CCFButton b)
		{
			if (b != null)
			{
				addCCFIconSet(b.getIconSet());
			}
		}

		void addCCFIconSet(CCFIconSet is)
		{
			for (int i=0; i<4; i++)
			{
				addIcon(is.icons[i]);
			}
		}

		void addIcon(CCFIcon icon)
		{
			if (icon != null && hash.get(icon) == null)
			{
				hash.put(icon, new IconButton(icon));
				items.add(icon);
				if (color())
				{
					icon.convertToColor();
				}
				else
				{
					icon.convertToGray();
				}
			}
		}

		public CCFIcon getSelected()
		{
			return selected;
		}

		public void doOK()
		{
			selected = (CCFIcon)items.get(list.getSelectedIndex());
		}

		public void doCancel()
		{
		}

		public Dimension getMinimumSize()
		{
			return max(super.getMinimumSize(), 350, 350);
		}

		public Dimension getPreferredSize()
		{
			return max(super.getPreferredSize(), 350, 350);
		}

		public Dimension getMaximumSize()
		{
			return max(super.getMaximumSize(), 350, 350);
		}
	}

	// ---( Inner Class TimerEditor )---
	static class TimerEditor extends OKCancelDialog
	{
		private CCFTimer timer;
		private TimerEvent start;
		private TimerEvent end;

		TimerEditor()
		{
			super("Timer");
			AAPanel p = new AAPanel();
			setContents(p);

			start = new TimerEvent();
			end = new TimerEvent();

			p.add(new LabelBox("Start", start), "x=0;y=0;pad=2,2,2,2;fill=b;wx=1;wy=1");
			p.add(new LabelBox("End", end),     "x=0;y=1;pad=2,2,2,2;fill=b;wx=1;wy=1");
		}

		public boolean invoke(CCFTimer t)
		{
			if (t == null)
			{
				t = new CCFTimer();
			}
			timer = t;
			start.update(timer.startDays, timer.startTime, timer.startAction);
			end.update(timer.endDays, timer.endTime, timer.endAction);
			return invoke();
		}

		public CCFTimer getTimer()
		{
			return timer;
		}

		public void doOK()
		{
			start.update();
			end.update();
			timer.startDays = start.days;
			timer.startTime = start.time;
			timer.startAction = start.action;
			timer.endDays = end.days;
			timer.endTime = end.time;
			timer.endAction = end.action;
		}

		public void doCancel()
		{
		}
	}

	// ---( Inner Class ProntoScanDialog )---
	class ProntoScanDialog extends TransferDialog implements Runnable
	{
		private boolean running;
		private Comm comm;

		ProntoScanDialog()
		{
			super("Scanning for Remote");
		}

		public void run()
		{
			int val = 1;
			int dir = 1;
			while (running)
			{
				if (val <= 0 || val >= 100)
				{
					dir = -dir;
				}
				val += dir;
				progress(val);
				try { Thread.currentThread().sleep(20); } catch (Exception ex) { }
			}
		}

		public void show()
		{
			running = true;
			new Thread(this).start();
			super.show();
		}

		public void dispose()
		{
			running = false;
			super.dispose();
		}
	}

	// ---( Inner Class IRValueDialog )---
	class IRValueDialog extends MemoryDialog implements KeyListener
	{
		private CCFIRCode ircode;
		private NameAttr name;
		private JTextArea text;
		private IRGraph graph;
		private JComboBox dbase;
		private boolean ready;

		IRValueDialog(CCFIRCode ircode)
		{
			super("IR Signal", PREF_IR_BOUNDS);
			this.ircode = ircode;
			String nm = ircode.getName();
			name = new NameAttr();
			name.setName(nm != null ? nm : "Learned");
			text = new MyTextArea(8, 40);
			text.setFont(new Font("Courier", Font.PLAIN, 12));
			text.setLineWrap(true);
			text.setWrapStyleWord(true);
			text.addKeyListener(this);
			setRCMenu(text);
			graph = new IRGraph(null);
			dbase = new JComboBox();
			dbase.addActionListener(this);
			AAPanel p = new AAPanel();
			LabelBox ra = new LabelBox("Data", new JScrollPane(text));
			LabelBox si = new LabelBox("Signal", new JScrollPane(graph,
				JScrollPane.VERTICAL_SCROLLBAR_NEVER,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
			LabelBox db = new LabelBox("Database", dbase);
			p.add(name, "x=0;y=0;fill=b;wx=1;wy=0;pad=5,5,5,5");
			p.add(ra,   "x=0;y=1;fill=b;wx=1;wy=2;pad=5,5,5,5");
			p.add(si,   "x=0;y=2;fill=b;wx=1;wy=1;pad=0,5,5,5");
			p.add(db,   "x=0;y=3;fill=b;wx=1;wy=0;pad=0,5,5,5");
			setContents(p);
			setText(new Pronto(ircode.getCode(),ircode.hasUDB()).encode());
		}

		public JComponent getButtons()
		{
			return UIUtils.getOtherOKCancel(new String[] { "Learn", "Expert" }, IRValueDialog.this);
		}

		private void setText(String txt)
		{
			text.setText(txt);
			updateCode();
		}

		private String getText()
		{
			return text.getText().trim();
		}

		public boolean handleCmd(String cmd)
		{
			if (cmd != null)
			{
				if (cmd.equals("Learn"))
				{
					Pronto p = learnIR();
					if (p != null)
					{
						setText(p.encode());
					}
					return false;
				}
				else
				if (cmd.equals("Expert"))
				{
					IRSignal sig = null;
					try {
						sig = new IRSignalDialog(new Pronto(getText(),Pronto.VERSION1)).getValue();
					} catch (Exception ex) {
						sig = new IRSignalDialog(new IRSignal()).getValue();
					}
					if (sig != null)
					{
						setText(new Pronto(sig).encode());
					}
				}
			}
			return false;
		}

		public void actionPerformed(ActionEvent ae)
		{
			if (ae.getSource() == text)
			{
				doOK();
				dispose();
				return;
			}
			else
			if (ready && ae.getSource() == dbase)
			{
				debug.log(3,"dbase item selected: "+dbase.getSelectedItem());
				IRSel sel = (IRSel)dbase.getSelectedItem();
				if (sel != null)
				{
					setText(new Pronto(sel.sig).encode());
				}
			}
			super.actionPerformed(ae);
		}

		public void keyTyped(KeyEvent ev)
		{
			new Thread() { public void run() { try {
				sleep(150);
				updateCode();
			} catch (Exception ex) { } } }.start();
		}

		public void keyPressed(KeyEvent ev)
		{
		}
	
		public void keyReleased(KeyEvent ev)
		{
		}

		public boolean invoke()
		{
			ready = false;
			dbase.removeAllItems();
			if (database != null)
			{
				for (Enumeration r = database.getRemotes(); r.hasMoreElements(); )
				{
					IRRemote rem = (IRRemote)r.nextElement();
					for (Enumeration k = rem.getKeys(); k.hasMoreElements(); )
					{
						IRSignal sig = (IRSignal)k.nextElement();
						dbase.addItem(new IRSel(rem, sig));
					}
				}
			}
			dbase.setSelectedItem(null);
			ready = true;
			return super.invoke();
		}

		class IRSel
		{
			private IRRemote rem;
			private IRSignal sig;

			IRSel(IRRemote rem, IRSignal sig)
			{
				this.rem = rem;
				this.sig = sig;
			}

			public String toString()
			{
				return rem.getModel()+"-"+sig.getName();
			}
		}
	
		public void doOK()
		{
			ircode.setName(name.getName());
			ircode.setCode((ircode.hasUDB() ? "0000 0000 0000 " : "")+text.getText().trim());
		}

		public void doCancel()
		{
		}

		public void updateCode()
		{
			setCode(text.getText().trim());
		}

		public void setCode(String val)
		{
			Pronto p = new Pronto(val.trim(),Pronto.VERSION1);
			graph.setSignal(p.isRawSignal() ? null : p);
		}
	}

	// ---( Inner Class CommStatusDialog )---
	class CommStatusDialog extends StackedDialog implements ActionListener
	{
		CommStatusDialog()
			throws Exception
		{
			super("Remote Status");
			Comm comm = getComm(true);

			if (comm == null)
			{
				showCommError();
				return;
			}
			
			JButton ok = new JButton("OK");
			ok.addActionListener(this);
			JButton ref = new ActionButton("Refresh") {
				public void action() {
					dispose();
					closeComm();
					sendEvent(UTIL_LOCATE);
				}
			};

			AAPanel pp = new AAPanel(true);
			Util.setLabelBorder("Remote", pp);
			AAPanel cp = new AAPanel(true);
			Util.setLabelBorder("CCF", cp);

			JLabel lport = label("Port");
			JLabel lcapa = label("Feature");
			//JTextField dport = data(comm.getSerialPort().getName());
			JTextField dport = data(comm.getPortName());
			JTextField dcapa = data("0x"+Integer.toString(comm.getPossible(),16));

			JLabel lsize = label("Size");
			JLabel ldate = label("Date");
			JLabel ltime = label("Time");
			JLabel lstat = label("State");
			JTextField dsize = data(Integer.toString(comm.getCCFSize()));
			JTextField ddate = data(comm.getCCFDate());
			JTextField dtime = data(comm.getCCFTime());
			JTextField dstat = data((comm.isCCFDirty() ? "" : "un")+"changed");

			define('o', ok,    "pad=5,5,5,5;fill=b;wx=1");
			define('r', ref,   "pad=5,5,5,5;fill=b;wx=1");
			define('.', pp,    "pad=3,3,3,3;fill=b;wx=1;wy=1");
			define(',', cp,    "pad=3,3,3,3;fill=b;wx=1;wy=1");
			define('-',        "pad=3,3,3,3");
			define('1', lport, "pad=3,3,3,3;fill=b");
			define('2', dport, "pad=3,3,3,3;fill=b;wx=1");
			define('3', lcapa, "pad=3,3,3,3;fill=b");
			define('4', dcapa, "pad=3,3,3,3;fill=b;wx=1");
			define('5', lsize, "pad=3,3,3,3;fill=b");
			define('6', dsize, "pad=3,3,3,3;fill=b;wx=1");
			define('7', ldate, "pad=3,3,3,3;fill=b");
			define('8', ddate, "pad=3,3,3,3;fill=b;wx=1");
			define('9', ltime, "pad=3,3,3,3;fill=b");
			define('0', dtime, "pad=3,3,3,3;fill=b;wx=1");
			define('a', lstat, "pad=3,3,3,3;fill=b");
			define('b', dstat, "pad=3,3,3,3;fill=b;wx=1");


			setLayout(new String[] {
				"...............",
				".111 222222222.",
				".333 444444444.",
				"...............",
				",,,,,,,,,,,,,,,",
				",555 666666666,",
				",777 888888888,",
				",999 000000000,",
				",aaa bbbbbbbbb,",
				",,,,,,,,,,,,,,,",
				"---------------",
				"-     o r     -",
				"---------------",
			});
			new RunInThread() {
				public void runIn() {
					show();
				}
			}.start();
		}

		private JLabel label(String str)
		{
			return new JLabel(str, JLabel.RIGHT);
		}

		private JTextField data(String str)
		{
			JTextField l = new JTextField(str);
			l.setEditable(false);
			return l;
		}

		public void actionPerformed(ActionEvent ae)
		{
			dispose();
		}
	}

	// ---( Inner Class TextDialog )---
	class TextDialog extends StackedDialog implements ActionListener
	{
		TextDialog(String title, String res)
		{
			super(title);
			
			JButton ok = new JButton("OK");
			ok.addActionListener(this);

			JTextArea text = new JTextArea(30,80);
			text.setFont(new Font("Courier", Font.PLAIN, 12));
			text.setEditable(false);
			setRCMenu(text);

			InputStream is = getClass().getResourceAsStream(res);
			try
			{
				if (is != null)
				{
					byte b[] = new byte[1024];
					int read = 0;
					while ( (read = is.read(b)) >= 0)
					{
						text.append(new String(b,0,read));
					}
				}
				else
				{
					debug.log(1,"can't find resource '"+res+"'");
				}
			}
			catch (Exception ex)
			{
				errorDialog(ex);
			}

			text.setCaretPosition(0);

			JScrollPane scroll = new JScrollPane(text);

			add(scroll, "x=0;y=0;fill=b;pad=3,3,3,3;wx=1;wy=1");
			add(ok,     "x=0;y=1;fill=b;pad=5,5,5,5;wx=0;wy=0");
		}

		public void actionPerformed(ActionEvent ae)
		{
			dispose();
		}
	}

	// ---( Inner Class MergeDialog )---
	class MergeDialog extends StackedDialog
	{
		private boolean merge = false;

		MergeDialog()
		{
			super("Merge or Replace");
			JLabel l1 = new JLabel("Would you like to merge the new file into");
			JLabel l2 = new JLabel("the existing file or replace it?");
			JButton m = new ActionButton("Merge") {
				public void action() {
					merge = true;
					dispose();
				}
			};
			JButton r = new ActionButton("Replace") {
				public void action() {
					merge = false;
					dispose();
				}
			};
			define('1', l1, "wx=1;wy=1;fill=b;pad=5,5,3,5");
			define('2', l2, "wx=1;wy=1;fill=b;pad=0,5,5,5");
			define('m', m,  "wx=1;fill=b;pad=3,3,3,3");
			define('r', r,  "wx=1;fill=b;pad=3,3,3,3");
			setLayout(new String[] {
				"1111111",
				"2222222",
				"mmm rrr"
			});
		}

		public boolean getValue()
		{
			show();
			return merge;
		}
	}

	// ---( Inner Class UndeadDialog )---
	class UndeadDialog extends StackedDialog
	{
		private JTextField port = new JTextField(15);

		UndeadDialog()
		{
			super("Undead Remote");
			port.setText(prefCommPort != null ? prefCommPort : "");
			JTextArea ta = new JTextArea(20,55);
			ta.setEditable(false);
			ta.setLineWrap(true);
			ta.setWrapStyleWord(true);
			ta.setText(
				"READ THIS FIRST!!!\n\n"+
				"If your remote is rebooting after a failed CCF download, then "+
				"this utility may just be able to help. If your remote is rebooting after "+
				"a failed firmware update, then use the 'Firmware Update' menu instead. "+
				"Here's what to do:\n\n"+
				" (1) Load the CCF into Tonto that you want send to the Pronto\n"+
				" (2) Type the name of the device your Pronto is attached to into"+
				" the 'port' field below. This may have already been detected.\n"+
				" (3) Click 'Undead'.\n"+
				" (4) If your Pronto isn't already resetting itself, insert a paperclip"+
				" into the 'reset' hole on the back of the Pronto or remove and re-add"+
				" the batteries to force a cold start.\n"+
				" (5) Wait and Watch. It may take a couple of minutes.\n\n"+
				"If your hardware is not completely terminal and the correct port was "+
				"specified, then there is a good chance your Pronto will soon live again. "+
				"The most common problem with 'Undeading' a Pronto is getting the right port "+
				"name. On Windows, it's COM1-COM4. On Unix, it's usually /dev/something."
			);
			JScrollPane sta = new JScrollPane(ta,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			JButton u = new ActionButton("Undead") {
				public void action() {
					final String fname = Util.time()+".ccf";
					final TransferDialog td = new TransferDialog("Undead");
					final File file = new File(fname);
					try
					{
						if (ccf == null)
						{
							errorDialog("You must first load a CCF");
							return;
						}
						ccf.save(fname);
						new DialogThread(td) {
							public void body() throws Exception
							{
								closeComm();
								Comm nc = new Comm(port.getText().trim());
								try
								{
									nc.undeadPronto(fname, td);
									infoDialog("Undead Upload Succeeded!");
									if (prefCommPort == null)
									{
										prefCommPort = port.getText().trim();
									}
									dispose();
								}
								finally
								{
									nc.close();
								}
							}
						}.checkError();
					}
					catch (Throwable ex)
					{
						errorDialog("Undead failed", ex);
					}
					finally
					{
						file.delete();
						td.dispose();
					}
				}
			};
			JButton c = new ActionButton("Cancel") {
				public void action() {
					dispose();
				}
			};
			JLabel lport = new JLabel("Port", JLabel.RIGHT);
			AAPanel x = new AAPanel(true);
			Util.setLabelBorder("Undead", x);
			define('x', x,     "wx=1;wy=1;fill=b;pad=5,5,3,5");
			define('t', sta,   "wx=1;wy=1;fill=b;pad=5,5,3,5");
			define('l', lport, "wx=0;fill=b;pad=3,3,3,3");
			define('p', port,  "wx=1;fill=b;pad=3,3,3,3");
			define('u', u,     "wx=1;fill=b;pad=3,3,3,3");
			define('c', c,     "wx=1;fill=b;pad=3,3,3,3");
			setLayout(new String[] {
				"xxxxxxxxx",
				"xtttttttx",
				"xl pppppx",
				"xxxxxxxxx",
				" uuu ccc ",
			});
		}
	}

	// -------------------------------------------------------------------------------------
	// THE REST
	// -------------------------------------------------------------------------------------

	// ---( Inner Class DeviceKey )---
	static class DeviceKey
	{
		private CCFDevice device;
		private int key;
		private String name;

		DeviceKey(CCFDevice d, int k)
		{
			this(d, k, d.getHardKeys()[k].getName());
		}

		DeviceKey(CCFDevice d, int k, String n)
		{
			device = d;
			key = k;
			name = n;
		}

		public int hashCode()
		{
			return device.hashCode() + key;
		}

		public boolean equals(Object o)
		{
			if (o instanceof DeviceKey)
			{
				DeviceKey dk = (DeviceKey)o;
				return (dk.device == device && dk.key == key);
			}
			return false;
		}

		public String toString()
		{
			if (name != null)
			{
				return device+" : "+CCFAction.getKeyName(device,key)+" ("+name+")";
			}
			else
			{
				return device+" : "+CCFAction.getKeyName(device,key);
			}
		}
	}

	// ---( Inner Class PanelWrapper )---
	class PanelWrapper
	{
		private CCFPanel panel;

		PanelWrapper(CCFPanel p)
		{
			panel = p;
		}

		public int hashCode()
		{
			return panel.hashCode();
		}

		public boolean equals(Object o)
		{
			return
				(o instanceof PanelWrapper) &&
				(((PanelWrapper)o).panel == panel);
		}

		public String toString()
		{
			return panel != null ? " "+panel.getFQN() : "";
		}
	}

	// ---( Inner Class JumpWrapper )---
	class JumpWrapper
	{
		private int value;

		JumpWrapper(int val)
		{
			value = val;
		}

		public int hashCode()
		{
			return value;
		}

		public boolean equals(Object o)
		{
			return
				(o instanceof JumpWrapper) &&
				(((JumpWrapper)o).value == value);
		}

		public String toString()
		{
			return " (( "+CCFAction.getJumpSpecialString(value)+" ))";
		}
	}

	// ---( Inner Interface Copyable )---
	interface Copyable
	{
		public Object copy() ;
	}

	// ---( Inner Interface Deletable )---
	interface Deletable
	{
		public void delete() ;
	}

	// ---( Inner Interface Configurable )---
	interface Configurable
	{
		public void editProperties() ;
	}

	// ---( Inner Interface ButtonHost )---
	interface ButtonHost
	{
		public void addButton() ;
	}

	// ---( Inner Interface FrameHost )---
	interface FrameHost
	{
		public void addFrame() ;
	}

	// ---( Inner Interface Namable )---
	interface Namable
	{
		public String getName() ;
		public void setName(String name) ;
	}

	// ---( Inner Interface Resizable )---
	interface Resizable
	{
		public boolean isResizable() ;
		public Dimension getSize() ;
		public void setSize(Dimension size) ;
	}

	// ---( Inner Interface Movable )---
	interface Movable
	{
		public Point getLocation() ;
		public void setLocation(Point p) ;
	}

	// ---( Inner Interface Pastable )---
	interface Pastable
	{
		public boolean acceptPaste(Object o) ;
		public void paste(Object o) ;
	}

	// ---( Inner Interface Parental )---
	interface Parental
	{
		public Object getMyParent() ;
	}

	// ---( Inner Interface Doable )---
	interface Doable
	{
		public void doIt();

		public void undoIt();
	}

	// ---( Inner Interface TreeProperties )---
	interface TreeProperties
	{
	}

	// ---( Inner Class DoNodeUpdate )---
	class DoNodeUpdate implements Doable
	{
		private CCFNode node;
		private Hashtable oldState;
		private Hashtable newState;
		private boolean lastWasDo;

		DoNodeUpdate(CCFNode node)
		{
			this.node = node;
			this.lastWasDo = false;
			getOldState();
		}

		public void doIt()
		{
			if (!lastWasDo)
			{
				setNewState();
				reread();
				CCFPanel panel = node.getParentPanel();
				if (panel != null)
				{
					refreshPanel(panel);
				}
			}
			lastWasDo = true;
		}

		private void reread()
		{
			if (node instanceof CCFButton)
			{
				ButtonBox bb = (ButtonBox)getUIWrapper(((CCFButton)node).getChildWrapper());
				if (bb != null)
				{
					bb.readButton();
				}
			}
			else
			if (node instanceof CCFFrame)
			{
				FrameBox fb = (FrameBox)getUIWrapper(((CCFFrame)node).getChildWrapper());
				if (fb != null)
				{
					fb.readFrame();
				}
			}
		}

		public void undoIt()
		{
			if (lastWasDo)
			{
				setOldState();
				reread();
				refreshPanel(node.getParentPanel());
			}
			lastWasDo = false;
		}

		private Hashtable getState()
		{
			Hashtable h = new Hashtable();
			try {
				Field f[] = node.getClass().getDeclaredFields();
				for (int i=0; i<f.length; i++)
				{
					try {
					h.put(f[i], f[i].get(node));
					} catch (Exception ex) { }
				}

			} catch (Exception ex) { ex.printStackTrace(); }
			return h;
		}

		private void setState(Hashtable h)
		{
			try {
				for (Enumeration e = h.keys(); e.hasMoreElements(); )
				{
					try {
					Field f = (Field)e.nextElement();
					Object o = h.get(f);
					f.set(node, o);
					if (o instanceof CCFChild[])
					{
						CCFChild c[] = (CCFChild[])o;
						for (int i=0; i<c.length; i++)
						{
							c[i].setParent(node);
						}
					}
					} catch (Exception ex) { }
				}

			} catch (Exception ex) { ex.printStackTrace(); }
		}

		public void getOldState()
		{
			oldState = getState();
		}

		public void setOldState()
		{
			setState(oldState);
		}

		public void getNewState()
		{
			newState = getState();
		}

		public void setNewState()
		{
			setState(newState);
		}

		public void pushDoNewState()
		{
			getNewState();
			pushDo(this);
		}
	}

	// ---( Inner Class DoRefresher )---
	class DoRefresher implements Doable
	{
		private CCFPanel panel;
		private boolean ondo;

		DoRefresher(CCFPanel panel, boolean ondo)
		{
			this.panel = panel;
			this.ondo = ondo;
		}

		public void doIt()
		{
			if (ondo) { refreshPanel(panel); }
		}

		public void undoIt()
		{
			if (!ondo) { refreshPanel(panel); }
		}
	}

	// ---( Inner Class DoMove )---
	class DoAdd implements Doable
	{
		private CCFChild child;
		private IChildContainer parent;
		private CCFChild clone[];
		private Component oldDS;

		DoAdd(CCFChild child, IChildContainer parent)
		{
			this.child = child;
			this.parent = parent;
		}

		public void doIt()
		{
			clone = parent.getChildren();
			if (child.type == child.FRAME)
			{
				parent.addFrame(child.getFrame());
				icons.addFrame(child.getFrame());
			}
			else
			{
				parent.addButton(child.getButton());
				icons.addButton(child.getButton());
			}
			refreshPanel(child.getParentPanel());
			// TODO = save mult-selection as well?
			oldDS = dragSelection;
			setDragSelection(getUIWrapper(child));
		}

		public void undoIt()
		{
			parent.setChildren(clone);
			refreshPanel(child.getParentPanel());
			setDragSelection(oldDS);
		}
	}

	// ---( Inner Class DoAddPanel )---
	class DoAddPanel implements Doable
	{
		private CCFPanel panel;
		private CCFDevice device;
		private CCFTreeDevice node;
		private boolean show;

		DoAddPanel(CCFTreeDevice node, CCFDevice device, CCFPanel panel, boolean show)
		{
			this.node = node;
			this.device = device;
			this.panel = panel;
			this.show = show;
		}

		public void doIt()
		{
			device.addPanel(panel);
			node.refresh();
			if (show)
			{
				showDeskPanel(panel);
			}
		}

		public void undoIt()
		{
			removePanel(panel, true);
			panel.delete();
			node.refresh();
		}
	}

	// ---( Inner Class DoDeleteChild )---
	class DoDeleteChild implements Doable
	{
		private CCFChild child;
		private IChildContainer parent;
		private CCFChild clone[];
		private Component oldDS;

		public String toString()
		{
			return "DoDeleteChild:"+child;
		}

		DoDeleteChild(CCFChild child)
		{
			this.child = child;
			this.parent = (IChildContainer)child.getParent();
		}

		public void doIt()
		{
			clone = parent.getChildren();
			if (child.type == child.FRAME)
			{
				child.getFrame().delete();
			}
			else
			{
				child.getButton().delete();
			}
			refreshPanel(child.getParentPanel());
			setCCFChanged();
			setDragSelection(null);
		}

		public void undoIt()
		{
			parent.setChildren(clone);
			refreshPanel(child.getParentPanel());
		}
	}

	// ---( Inner Class DoDeletePanel )---
	class DoDeletePanel implements Doable
	{
		private CCFPanel before;
		private CCFPanel panel;
		private TreeNode treeParent;
		private CCFDevice device;
		private boolean showing;

		DoDeletePanel(CCFPanel panel)
		{
			this.panel = panel;
		}

		public void doIt()
		{
			treeParent = ((CCFTreePanel)getTreeWrapper(panel)).getParent();
			before = panel.next;
			device = panel.getParentDevice();
			showing = panels.get(panel) != null;
			removePanel(panel, true);
			panel.delete();
			treeParent.refresh();
		}

		public void undoIt()
		{
			device.insertBefore(before, panel);
			treeParent.refresh();
			if (showing)
			{
				showDeskPanel(panel);
			}
		}
	}

	// ---( Inner Class DoDeletePanel )---
	class DoDeleteDevice implements Doable
	{
		private CCFDevice before;
		private CCFDevice device;
		private CCFHeader header;
		private CCFTreeDeviceFolder folder;
		private Vector showing;
		private int type;

		private final int HOME = 1;
		private final int DEVICE = 2;
		private final int MACRO = 3;

		DoDeleteDevice(CCFDevice device, CCFTreeDeviceFolder folder)
		{
			this.device = device;
			this.folder = folder;
			this.type = (device.isHomeDevice() ? HOME : device.isNormalDevice() ? DEVICE : MACRO);
		}

		public void doIt()
		{
			showing = new Vector();
			CCFPanel pan = device.getFirstPanel();
			// close showing panels
			while (pan != null)
			{
				if (panels.get(pan) != null)
				{
					showing.add(pan);
				}
				removePanel(pan, true);
				pan = pan.getNextPanel();
			}

			before = device.next;
			header = device.getHeader();
			folder.setRootDevice(device.delete());
			folder.refresh();
		}

		public void undoIt()
		{
			if (before == null)
			{
				switch (type)
				{
					case HOME: if (header.firstHome != null) { header.firstHome.appendDevice(device); } else { header.firstHome = device; } break;
					case DEVICE: if (header.firstDevice != null) { header.firstDevice.appendDevice(device); } else { header.firstDevice = device; } break;
					case MACRO: if (header.firstMacro != null) { header.firstMacro.appendDevice(device); } else { header.firstMacro = device; } break;
				}
			}
			else
			{
				header.insertBefore(before, device);
			}
			folder.refresh();
			for (int i=0; i<showing.size(); i++)
			{
				showDeskPanel((CCFPanel)showing.get(i));
			}
		}
	}

	// ---( Inner Class DoRemoveDependencies )---
	class DoRemoveDependencies implements Doable, IWalker
	{
		private CCF ccf;
		private CCFNode node;
		private Hashtable clones;

		DoRemoveDependencies(CCF ccf, CCFNode node)
		{
			this.ccf = ccf;
			this.node = node;
		}

		public void doIt()
		{
			clones = new Hashtable();
			if (ccf != null)
			{
				new CCFWalker(ccf).walk(this);
			}
		}

		public void undoIt()
		{
			for (Enumeration e = clones.keys(); e.hasMoreElements(); )
			{
				CCFActionList al = (CCFActionList)e.nextElement();
				CCFAction a[] = (CCFAction[])clones.get(al);
				al.setActions(a);
			}
		}

		public void onNode(CCFNode on)
		{
			if (on instanceof CCFActionList)
			{
				CCFActionList al = (CCFActionList)on;
				clones.put(al, al.getActions());
				if (al.deleteMatching(node))
				{
					debug.log(1,"rmdep: remove matching "+Util.nickname(on.getParent()));
					new Exception("Trace").printStackTrace();
				}
			}
		}
	}

	// ---( Inner Class DoMove )---
	class DoBounds implements Doable
	{
		private CCFChild child;
		private Component box;
		private Rectangle start, end;

		DoBounds(CCFChild child, int xoff, int yoff, int woff, int hoff)
		{
			this.child = child;
			this.start = child.getBounds();
			this.end = new Rectangle(start.x + xoff, start.y + yoff, start.width + woff, start.height + hoff);
		}

		DoBounds(CCFChild child, Rectangle start, Rectangle end)
		{
			this.child = child;
			this.start = start;
			this.end = end;
		}

		DoBounds(Component box, Rectangle start, Rectangle end)
		{
			this.box = box;
			this.start = start;
			this.end = end;
		}

		public void doIt()
		{
			if (box != null) { box.setBounds(end); }
			if (child != null) { child.setBounds(end); }
		}

		public void undoIt()
		{
			if (box != null) { box.setBounds(start); }
			if (child != null) { child.setBounds(start); }
		}
	}

	// ---( Inner Class MultiDo )---
	class MultiDo implements Doable
	{
		private Vector dos;

		MultiDo()
		{
			dos = new Vector();
		}

		MultiDo(Doable d[])
		{
			this.dos = dos;
			dos = new Vector(d.length);
			for (int i=0; i<d.length; i++)
			{
				dos.add(d[i]);
			}
		}

		MultiDo(Vector dos)
		{
			this.dos = dos;
		}

		public void add(Doable d)
		{
			dos.add(d);
		}

		public void doIt()
		{
			for (int i=0; i<dos.size(); i++)
			{
				((Doable)dos.get(i)).doIt();
			}
		}

		public void undoIt()
		{
			for (int i=0; i<dos.size(); i++)
			{
				((Doable)dos.get(i)).undoIt();
			}
		}
	}

	// ---( Inner Class FileBrowse )---
	static class FileBrowse extends ActionButton
	{
		private String title;
		private JTextField target;

		FileBrowse(String title, JTextField target)
		{
			super("Browse");
			this.title = title;
			this.target = target;
		}

		public void action()
		{
			browseForFile(title, target);
		}
	}
	
	// ---( Inner Class RevertMenuAction )---
	static class RevertMenuAction implements ActionListener
	{
		private File touch;
		private String name;

		RevertMenuAction(File touch, String name)
		{
			this.touch = touch;
			this.name = name;
		}

		public void actionPerformed(ActionEvent ae)
		{
			touch.setLastModified(Util.time());
			infoDialog("Restart Tonto to use version "+name);
		}
	}

	// ---( Inner Class EventAction )---
	static class EventAction extends AbstractAction
	{
		private int evid;

		EventAction(int evid)
		{
			this.evid = evid;
		}

		public void actionPerformed(ActionEvent ae)
		{
			sendEvent(evid);
		}
	}

	// ---( Inner Class TabSelect )---
	static class TabSelect extends AbstractAction
	{
		private int idx;
		private JTabbedPane tabs;

		TabSelect(JTabbedPane tabs, int idx)
		{
			this.tabs = tabs;
			this.idx = idx;
		}

		public void actionPerformed(ActionEvent ae)
		{
			int max = tabs.getTabCount();
			if (max > 0)
			{
				if (idx < 0)
				{
					tabs.setSelectedIndex(max-1);
				}
				else
				{
					tabs.setSelectedIndex(idx);
				}
			}
		}
	}

	// ---( Inner Class TabPager )---
	class TabPager extends AbstractAction
	{
		private int inc;
		private JTabbedPane tabs;

		TabPager(JTabbedPane tabs, int inc)
		{
			this.tabs = tabs;
			this.inc = inc;
		}

		public void actionPerformed(ActionEvent ae)
		{
			int idx = tabs.getSelectedIndex() + inc;
			if (idx < 0)
			{
				idx = tabs.getTabCount()-1;
			}
			else
			if (idx >= tabs.getTabCount())
			{
				idx = 0;
			}
			tabs.setSelectedIndex(idx);
		}
	}

	// ---( Inner Class MemoryDialog )---
	static abstract class MemoryDialog extends OKCancelDialog
	{
		private JDialog dialog;
		private String sizepref;

		MemoryDialog(String title, String sizepref)
		{
			super(title);
			this.sizepref = sizepref;
		}

		public Rectangle defaultSize()
		{
			return null;
		}

		public void showHook(JDialog dialog)
		{
			this.dialog = dialog;
			fixSize();
		}

		private void fixSize()
		{
			Dimension minn = dialog.getPreferredSize();
			Dimension maxx = dialog.getToolkit().getScreenSize();
			Rectangle pref = prefs.getRectangle(sizepref, defaultSize());
			Dimension size = pref != null ? pref.getSize() : minn;
			size = new Dimension(Util.bound(minn.width,maxx.width-50,size.width), Util.bound(minn.height,maxx.height-50,size.height));
			dialog.setSize(size.width, size.height);
			if (!prefCenterDialogs && pref != null)
			{
				dialog.setLocation(new Point(pref.x, pref.y));
			}
			else
			{
				dialog.setLocationRelativeTo(jframe);
			}
		}

		public void repack()
		{
			super.repack();
			fixSize();
		}

		public void dispose()
		{
			prefs.setProperty(sizepref, dialog.getBounds());
			super.dispose();
		}
	}

	// ---( Inner Class SnapInfo )---
	static class SnapInfo
	{
		private double w, h;

		SnapInfo(double w, double h)
		{
			this.w = w;
			this.h = h;
		}
	}

	// ---( Inner Class ToolAction )---
	static class ToolAction extends AbstractAction
	{
		private int action;
		private String desc;

		ToolAction(String name, String icon, int action, String desc)
		{
			super(name, loadIcon(icon));
			this.action = action;
			this.desc = desc;
		}

		public void actionPerformed(ActionEvent ae)
		{
			sendEvent(action);
		}

		public Object getValue(String key)
		{
			if (key == SHORT_DESCRIPTION)
			{
				return desc;
			}
			return super.getValue(key);
		}
	}

	// ---( Inner Class VectorListModel )---
	static class VectorListModel extends AbstractListModel
	{
		private Vector list;

		VectorListModel(Vector list)
		{
			this.list = list;
		}

		public Object getElementAt(int index)
		{
			return list.get(index);
		}

		public int getSize()
		{
			return list.size();
		}

		public void refresh()
		{
			fireContentsChanged(this, 0, getSize());
		}
	}
}





// -------------------------------------------------------------------------------------
// IR DATABASE
// -------------------------------------------------------------------------------------

// ---( Inner Class IRDBDialog )---
class IRDBDialog extends StackedDialog
{
	private IRDBPanel db = new IRDBPanel(this);
	private IRRemotePanel remote = new IRRemotePanel(this);
	private IRSignalPanel signal = new IRSignalPanel(remote);
	private JSplitPane ud = new JSplitPane(JSplitPane.VERTICAL_SPLIT, db, remote);
	private JSplitPane lr = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, ud, signal);

	IRDBDialog()
	{
		super("IR Database", false);
		
		ud.setOneTouchExpandable(true);
		lr.setOneTouchExpandable(true);
		setContentPane(lr);
	}

	public void showHook(JDialog dialog)
	{
		dialog.setSize(800,600);
		ud.setDividerLocation(250);
		lr.setDividerLocation(400);
	}

	void edit(IRRemote rem)
	{
		remote.setRemote(rem);
	}

	void edit(IRSignal sig)
	{
		signal.setSignal(sig);
	}

	void refresh()
	{
		db.refresh();
		remote.refresh();
		signal.refresh();
	}

	void refreshDB()
	{
		db.refresh();
	}

	void refreshRemotes()
	{
		remote.refresh();
	}
}

// ---( Inner Class IRDBPanel )---
class IRDBPanel extends AAPanel
{
	private IRDatabase db;
	private IRDBTable tabl;
	private IRDBDialog dialog;

	IRDBPanel(IRDBDialog dialog)
	{
		this.dialog = dialog;

		try
		{
			db = Tonto.database();
		}
		catch (Exception ex)
		{
			Tonto.errorDialog(ex);
		}

		JButton impo = new ActionButton("CCF Import") {
			public void action() {
				if (Tonto.ccf() == null)
				{
					Tonto.errorDialog("Make sure a CCF is loaded first");
					return;
				}
				try
				{
					CCFWalker.irex(Tonto.ccf(), db);
				}
				catch (Exception ex)
				{
					Tonto.errorDialog(ex);
				}
				tabl.refresh();
			}
		};
		JButton add = new ActionButton("Add") {
			public void action() {
				db.add(new IRRemote("NewModel","unknown","New Remote Model"));
				tabl.refresh();
				tabl.edit(db.size()-1);
			}
		};
		JButton dup = new ActionButton("Dup") {
			public void action() {
				int idx = tabl.getSelectedRow();
				if (idx >= 0)
				{
					IRRemote sr = db.getByIndex(idx);
					IRRemote dr = new IRRemote(
						sr.getModel(), sr.getCompany(), "Copy "+sr.getDescription());
					for (Enumeration e = sr.getKeys(); e.hasMoreElements(); )
					{
						dr.add( ((IRSignal)e.nextElement()).getClone() );
					}
					db.add(dr);
					tabl.refresh();
					edit(dr);
				}
			}
		};
		JButton del = new ActionButton("Delete") {
			public void action() {
				int row = tabl.getSelectedRow();
				if (row < 0 || row >= db.size()) { return; }
				db.remove(db.getByIndex(row));
				tabl.refresh();
				edit(null);
			}
		};
		JButton clr = new ActionButton("Clear") {
			public void action() {
				clear();
			}
		};
		JButton load = new ActionButton("Load") {
			public void action() {
				File f = Tonto.getFile(true, "irdb (*.db)", new String[] { "db" });
				if (f == null || !f.exists())
				{
					return;
				}
				try
				{
					db.read(f.toString());
				}
				catch (Exception ex)
				{
					Tonto.errorDialog(ex);
				}
				tabl.refresh();
			}
		};
		JButton save = new ActionButton("Save") {
			public void action() {
				File f = Tonto.getFile(false, "irdb (*.db)", new String[] { "db" });
				if (f == null)
				{
					return;
				}
				try
				{
					String fn = f.toString();
					if (!fn.toLowerCase().endsWith(".db"))
					{
						fn = fn+".db";
					}
					db.write(fn);
				}
				catch (Exception ex)
				{
					Tonto.errorDialog(ex);
				}
			}
		};
		tabl = new IRDBTable(this, db);
		tabl.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		JScrollPane scro = new JScrollPane(tabl);

		JLabel title = new JLabel("Remote Control List", JLabel.CENTER);
		JPanel tp = Util.panelWrap(title);
		tp.setBackground(Color.darkGray);
		title.setForeground(Color.white);
		title.setFont(new Font("sansserif", Font.BOLD, 14));

		define('T', tp,    "pad=3,3,3,3;wx=1;fill=b;ix=2;iy=2");
		define('+',        "pad=3,3,3,3");
		define('-',        "pad=3,3,3,3");
		define('e', dup,   "pad=3,3,3,3");
		define('a', add,   "pad=3,3,3,3");
		define('d', del,   "pad=3,3,3,3");
		define('l', load,  "pad=3,3,3,3");
		define('c', clr,   "pad=3,3,3,3");
		define('i', impo,  "pad=3,3,3,3");
		define('s', save,  "pad=3,3,3,3");
		define('t', scro,  "fill=b;wx=1;wy=1;pad=3,3,3,3");
		setLayout(new String[] {
			"TTTTTTTT",
			"--------",
			"- aedi -",
			"--------",
			"tttttttt",
			"++++++++",
			"+ lcs  +",
			"++++++++",
		});
	}

	public void clear()
	{
		db.clear();
		refresh();
	}

	public void edit(IRRemote remote)
	{
		dialog.edit(remote);
	}

	public void refresh()
	{
		tabl.refresh();
	}
}

// ---( Inner Class IRRemotePanel )---
class IRRemotePanel extends AAPanel
{
	private IRDBDialog dialog;
	private IRRemote remote;
	private IRRemoteTable tabl;
	private JTextField model = new JTextField();
	private JTextField comp = new JTextField();
	private JTextField desc = new JTextField();

	IRRemote remote()
	{
		return remote;
	}

	IRRemotePanel(IRDBDialog dialog)
	{
		this.dialog = dialog;
		JLabel lm = new JLabel("Model", JLabel.RIGHT);
		JLabel lc = new JLabel("Company", JLabel.RIGHT);
		JLabel ld = new JLabel("Description", JLabel.RIGHT);
		JButton add = new ActionButton("Add") {
			public void action() {
				IRSignal sig = new IRSignal();
				sig.setName("newKey");
				remote().add(sig);
				tabl.refresh();
				tabl.edit(remote().numKeys()-1);
			}
		};
		JButton dup = new ActionButton("Dup") {
			public void action() {
				int idx = tabl.getSelectedRow();
				if (idx >= 0)
				{
					IRSignal sig = remote().getByIndex(idx);
					sig = sig.getClone();
					sig.setName("Copy_"+sig.getName());
					remote().add(sig);
					tabl.refresh();
					tabl.edit(remote().numKeys()-1);
				}
			}
		};
		JButton del = new ActionButton("Delete") {
			public void action() {
				int row = tabl.getSelectedRow();
				if (row < 0 || row >= remote().numKeys()) { return; }
				remote().remove(row);
				tabl.refresh();
				edit(null);
			}
		};
		JButton clean = new ActionButton("Cleanup") {
			public void action() {
				cleanup();
			}
		};
		JButton panels = new ActionButton("Create IR Panel") {
			public void action() {
				if (Tonto.device() == null) {
					Tonto.errorDialog("Please select a device or panel in the tree");
					return;
				}
				createIRPanels(Tonto.device());
			}
		};
		tabl = new IRRemoteTable(this);
		tabl.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		JScrollPane scro = new JScrollPane(tabl);

		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				updateDB();
			}
		};
		model.addActionListener(al);
		comp.addActionListener(al);
		desc.addActionListener(al);

		JLabel title = new JLabel("Remote Keys", JLabel.CENTER);
		JPanel tp = Util.panelWrap(title);
		tp.setBackground(Color.darkGray);
		title.setForeground(Color.white);
		title.setFont(new Font("sansserif", Font.BOLD, 14));

		define('T', tp,     "pad=3,3,3,3;wx=1;fill=b;ix=2;iy=2");
		define('1', lm,     "pad=3,3,3,3;fill=b");
		define('2', lc,     "pad=3,3,3,3;fill=b");
		define('3', ld,     "pad=3,3,3,3;fill=b");
		define('4', model,  "pad=3,3,3,3;wx=1;fill=b");
		define('5', comp,   "pad=3,3,3,3;wx=1;fill=b");
		define('6', desc,   "pad=3,3,3,3;wx=1;fill=b");
		define('+',         "pad=3,3,3,3;wx=1;fill=b");
		define('.',         "pad=3,3,3,3");
		define(',',         "pad=3,3,3,3");
		define('t', scro,   "fill=b;wx=1;wy=1;pad=3,3,3,3");
		define('a', add,    "pad=3,3,3,3");
		define('e', dup,    "pad=3,3,3,3");
		define('d', del,    "pad=3,3,3,3");
		define('C', clean,  "pad=3,3,5,3");
		define('P', panels, "pad=3,3,3,3");
		setLayout(new String[] {
			"TTTTTTTTT",
			"++++++++",
			"+ 1 44 +",
			"+ 2 55 +",
			"+ 3 66 +",
			"++++++++",
			"........",
			". aedp .",
			"........",
			"tttttttt",
			",,,,,,,,",
			",  CP  ,",
			",,,,,,,,",
		});

		enable(false);
	}

	void createIRPanels(CCFDevice dev)
	{
		int count = remote.numKeys();
		double mult = Math.sqrt((double)count / 2d);
		int cols = (int)(mult);
		int rows = (int)((double)count / mult);
		while (rows * cols < count)
		{
			rows++;
		}
		Dimension panelSize = Tonto.panelSize();
		int xinc = (panelSize.width-1) / cols;
		int yinc = (panelSize.height-1) / rows;
		String panelName = remote.getModel();
		CCFPanel p = dev.createPanel(panelName);
		int i = 0;
		for (int y=0; y<rows; y++)
		{
			for (int x=0; x<cols; x++)
			{
				if (i >= count)
				{
					break;
				}
				IRSignal sig = remote.getByIndex(i);
				CCFButton btn = p.createButton(sig.getName());
				btn.setLocation(new Point(x*xinc+1, y*yinc+1));
				btn.setSize(new Dimension(xinc-1, yinc-1));
				btn.setFont(CCFFont.SIZE_8);
				CCFIconSet iconset = btn.getIconSet();
				boolean color = Tonto.color();
				iconset.setBackground(CCFIconSet.ACTIVE_UNSELECTED,CCFColor.getNamedColor(CCFColor.BLACK, color));
				iconset.setForeground(CCFIconSet.ACTIVE_UNSELECTED,CCFColor.getNamedColor(CCFColor.WHITE, color));
				CCFActionList al = new CCFActionList();
				al.setActions(new CCFAction[] {new ActionIRCode(new CCFIRCode(Tonto.ccf().header(), sig.getName(), new Pronto(sig).encode(Tonto.irVersion()))) });
				btn.setActionList(al);
				p.addButton(btn);
				i++;
			}
		}
		dev.addPanel(p);
		Tonto.state().refreshTreeDevice(dev);
		Tonto.infoDialog("Created '"+panelName+"' panel in device '"+dev.getName()+"'");
	}

	void edit(IRSignal signal)
	{
		dialog.edit(signal);
	}

	void setRemote(IRRemote remote)
	{
		this.remote = remote;
		if (remote != null)
		{
			enable(true);
			model.setText(remote.getModel());
			comp.setText(remote.getCompany());
			desc.setText(remote.getDescription());
			tabl.setRemote(remote);
		}
		else
		{
			tabl.setRemote(null);
			enable(false);
			clear();
		}
	}

	void updateDB()
	{
		remote.setModel(model.getText());
		remote.setCompany(comp.getText());
		remote.setDescription(desc.getText());
		refresh();
	}

	void cleanup()
	{
		for (Enumeration e = remote.getKeys(); e.hasMoreElements(); )
		{
			((IRSignal)e.nextElement()).cleanup();
		}
		tabl.refresh();
		dialog.refresh();
	}

	void refresh()
	{
		tabl.refresh();
		dialog.refreshDB();
	}
}

// ---( Inner Class IRSignalPanel )---
class IRSignalPanel extends AAPanel
{
	private IRRemotePanel panel;
	private IRSignal sig;
	private JTextField name = new JTextField();
	private JTextField indx = new JTextField();
	private JTextField freq = new JTextField(5);
	private JTextField rept = new JTextField(3);
	private JComboBox type = new JComboBox();
	private IRBurstPanel intro;
	private IRBurstPanel repeat;
	private JButton learn;
	private JButton clean;

	IRSignalPanel()
	{
		this(null);
	}

	IRSignalPanel(IRRemotePanel panel)
	{
		this.panel = panel;

		JLabel lname = new JLabel("Name", JLabel.RIGHT);
		JLabel lfreq = new JLabel("Frequency", JLabel.RIGHT);
		JLabel lrept = new JLabel("Repeats", JLabel.RIGHT);
		JLabel lindx = new JLabel("Index", JLabel.RIGHT);

		intro = new IRBurstPanel(this, panel);
		repeat = new IRBurstPanel(this, panel);

		type.addItem("Normal");
		type.addItem("Full");
		type.addItem("Once");

		JLabel title = new JLabel("IR Signal", JLabel.CENTER);
		JPanel tp = Util.panelWrap(title);
		tp.setBackground(Color.darkGray);
		title.setForeground(Color.white);
		title.setFont(new Font("sansserif", Font.BOLD, 14));

		learn = new ActionButton("Learn") {
			public void action() {
				learn();
			}
		};
		clean = new ActionButton("Cleanup") {
			public void action() {
				cleanup();
			}
		};

		if (panel != null)
		{
		define('T', tp,        "pad=3,3,3,3;wx=1;fill=b;ix=2;iy=2");
		define('N', lname,     "pad=3,3,3,3;fill=b");
		define('n',  name,     "pad=3,3,3,3;fill=b;wx=1");
		define('l',  learn,    "pad=3,3,5,3");
		define('c',  clean,    "pad=3,3,5,3");
		define('-',            "pad=0,0,0,0");
		}
		define('I', lindx,     "pad=3,3,3,3;fill=b");
		define('F', lfreq,     "pad=3,3,3,3;fill=b");
		define('R', lrept,     "pad=3,3,3,3;fill=b");
		define('i',  indx,     "pad=3,3,3,3;fill=b;wx=1");
		define('f',  freq,     "pad=3,3,3,3;fill=b;wx=1");
		define('r',  rept,     "pad=3,3,3,3;fill=b;wx=1");
		define('t',  type,     "pad=3,3,3,3;fill=b;wx=1");
		define('1',  intro,    "pad=3,3,3,3;fill=b;wx=1;wy=1");
		define('2',  repeat,   "pad=3,3,3,3;fill=b;wx=1;wy=1");

		Util.setLabelBorder("Intro Signal", intro);
		Util.setLabelBorder("Body Signal", repeat);

		setLayout(new String[] {
			"TTTTTTTTTTTTTTTTT",
			"NN nnnnnnnnnnnnnn",
			"FF ffff RR rr ttt",
			"II iiiiiiiiiiiiii",
			"11111111111111111",
			"22222222222222222",
			"-----------------",
			"- llllll cccccc -",
			"-----------------",
		});

		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				updateSig();
				updateDB();
			}
		};
		name.addActionListener(al);
		indx.addActionListener(al);
		freq.addActionListener(al);
		rept.addActionListener(al);
		type.addActionListener(al);

		enable(false);
	}

	public void learn()
	{
		Pronto p = Tonto.learnIR();
		if (p != null)
		{
			String nm = sig.getName();
			sig.copyFrom(p);
			sig.setName(nm);
			refresh();
			intro.setBurst(sig.getIntro());
			repeat.setBurst(sig.getRepeat());
		}
	}

	public void updateDB()
	{
		if (panel != null)
		{
			panel.updateDB();
		}
	}

	public void updateSig()
	{
		sig.setName(name.getText());
		sig.setFrequency(Integer.parseInt(freq.getText().trim()));
		sig.setMinRepeat(Integer.parseInt(rept.getText().trim()));
		sig.setPulseIndex(PulseIndex.parseIndexString(indx.getText().trim()));
		sig.setRepeatType(type.getSelectedIndex());
	}

	public void refresh()
	{
		if (sig != null)
		{
			enable(true);
			name.setText(sig.getName());
			freq.setText(sig.getFrequency()+"");
			rept.setText(sig.getMinRepeat()+"");
			indx.setText(sig.getPulseIndex().toString());
			getToolkit().getSystemClipboard().setContents(new StringSelection(new Pronto(sig).encode()),null);
			intro.setBurst(sig.getIntro());
			repeat.setBurst(sig.getRepeat());
			type.setSelectedIndex(sig.getRepeatType());
		}
		else
		{
			enable(false);
			clear();
			intro.setBurst(null);
			repeat.setBurst(null);
		}
	}

	void setSignal(IRSignal sig)
	{
		this.sig = sig;
		refresh();
	}

	void cleanup()
	{
		sig.cleanup();
		refresh();
	}

	IRSignal getSignal()
	{
		return sig;
	}
}

// ---( Inner Class IRBurstPanel )---
class IRBurstPanel extends AAPanel
{
	private IRSignalPanel sig;
	private IRRemotePanel panel;
	private IRBurst burst;
	private IRBurstCode burstCode;
	private JTextArea pulses = new JTextArea(2,40);
	private JTextField nbits = new JTextField(4);
	private JTextField value = new JTextField(6);
	private JTextField rbits = new JTextField();
	private JTextField bit0 = new JTextField(2);
	private JTextField bit1 = new JTextField(2);
	private JTextField head = new JTextField(2);
	private JTextField tail = new JTextField(2);
	private IRBurstGraph graph = new IRBurstGraph();

	IRBurstPanel(IRSignalPanel sig, IRRemotePanel panel)
	{
		this.sig = sig;
		this.panel = panel;
		JLabel lpulse = new JLabel("Pulses", JLabel.RIGHT);
		JLabel lnbits = new JLabel("NumBits", JLabel.RIGHT);
		JLabel lvalue = new JLabel("HexValue", JLabel.RIGHT);
		JLabel lrbits = new JLabel("BitValue", JLabel.RIGHT);
		JLabel lbit0  = new JLabel("Bit-0", JLabel.RIGHT);
		JLabel lbit1  = new JLabel("Bit-1", JLabel.RIGHT);
		JLabel lhead  = new JLabel("Head", JLabel.RIGHT);
		JLabel ltail  = new JLabel("Tail", JLabel.RIGHT);
		JLabel lgraph = new JLabel("Graph", JLabel.RIGHT);

		pulses.setLineWrap(true);
		JScrollPane sp = new JScrollPane(pulses,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
		);

		define('P', lpulse,  "pad=3,3,3,3;fill=b");
		define('N', lnbits,  "pad=3,3,3,3;fill=b");
		define('V', lvalue,  "pad=3,3,3,3;fill=b");
		define('R', lrbits,  "pad=3,3,3,3;fill=b");
		define('2', lbit0,   "pad=3,3,3,3;fill=b");
		define('3', lbit1,   "pad=3,3,3,3;fill=b");
		define('H', lhead,   "pad=3,3,3,3;fill=b");
		define('T', ltail,   "pad=3,3,3,3;fill=b");
		define('G', lgraph,  "pad=3,3,3,3;fill=b");
		define('p',  sp,     "pad=3,3,3,3;fill=b;wx=1;wy=1");
		define('n',  nbits,  "pad=3,3,3,3;fill=b;wx=1");
		define('v',  value,  "pad=3,3,3,3;fill=b;wx=1");
		define('r',  rbits,  "pad=3,3,3,3;fill=b;wx=1");
		define('0',  bit0,   "pad=3,3,3,3;fill=b;wx=1");
		define('1',  bit1,   "pad=3,3,3,3;fill=b;wx=1");
		define('h',  head,   "pad=3,3,3,3;fill=b;wx=1");
		define('t',  tail,   "pad=3,3,3,3;fill=b;wx=1");
		define('g', graph,   "pad=3,3,3,3;fill=b;wx=1");

		setLayout(new String[] {
			"P pppppppppppp",
			"N nn 2 00 3 11",
			"V vv H hh T tt",
			"R rrrrrrrrrrrr",
			"G gggggggggggg",
		});

		pulses.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				new Thread() { public void run() {
					try { sleep(150); } catch (Exception ex) { }
					updateFromPulses();
				}}.start();
			}
		});
		ActionListener al2 = new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				updateFromCode();
			}
		};
		nbits.addActionListener(al2);
		value.addActionListener(al2);
		bit0.addActionListener(al2);
		bit1.addActionListener(al2);
		head.addActionListener(al2);
		tail.addActionListener(al2);
		rbits.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				updateFromRawBits();
			}
		});
	}

	void refresh()
	{
		setBurst(burst);
	}

	void setBurst(IRBurst burst)
	{
		this.burst = burst;
		if (burst != null)
		{
			pulses.setText(burst.toString());
			updateFromPulses();
			graph.setBurst(burst);
		}
		else
		{
			graph.setBurst(null);
		}
	}

	void updateFromPulses()
	{
		//debug("updateFromPulses: "+pulses.getText());
		burst.decodePulseString(pulses.getText());
		graph.setBurst(burst);
		burstCode = burst.getBurstCode();
		if (burstCode != null)
		{
			updateBurstCodeFields();
			if (panel != null)
			{
				panel.refresh();
			}
		}
		else
		{
			nbits.setText("");
			value.setText("");
			rbits.setText("");
			bit0.setText("");
			bit1.setText("");
			head.setText("");
			tail.setText("");
		}
	}

	void updateBurstCodeFields()
	{
		nbits.setText(burstCode.getBitCount()+"");
		value.setText(Long.toHexString(burstCode.getValue()));
		rbits.setText(burstCode.getBitString());
		bit0.setText(burstCode.getBit0().toString());
		bit1.setText(burstCode.getBit1().toString());
		head.setText(burstCode.getHeadString());
		tail.setText(burstCode.getTailString());
	}

	void updateFromCode()
	{
		//debug("updateFromCode: "+burstCode);
		if (burstCode == null)
		{
			burstCode = new IRBurstCode(burst);
		}
		burstCode.setBitCount(Integer.parseInt(nbits.getText()));
		burstCode.setValue(Long.parseLong(value.getText(),16));
		burstCode.setBit0(burstCode.parsePulsePair(bit0.getText()));
		burstCode.setBit1(burstCode.parsePulsePair(bit1.getText()));
		burstCode.setHead(burstCode.parsePulsePairs(head.getText()));
		burstCode.setTail(burstCode.parsePulsePairs(tail.getText()));
		burst.setPulses(burstCode.getIRBurst().getPulses());
		//debug("updateFromCode: bc="+burstCode+" b="+burst);
		setBurst(burst);
	}

	void updateFromRawBits()
	{
		//debug("updateFromRawBits: "+rbits.getText());
		if (burstCode == null)
		{
			burstCode = new IRBurstCode(burst);
		}
		String rb = rbits.getText().trim();
		burstCode.setBitCount(rb.length());
		burstCode.setValue(IRBurstCode.decodeBitString(rb));
		updateBurstCodeFields();
		burst.setPulses(burstCode.getIRBurst().getPulses());
		setBurst(burst);
	}
}

// ---( Inner Class IRDBTable )---
class IRDBTable extends JTable
{
	private IRDBPanel panel;
	private IRDatabase db;
	private AbstractTableModel model;
	private int lastIdx;

	IRDBTable(IRDBPanel panel, IRDatabase db)
	{
		this.panel = panel;

		addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent me) {
				edit(getSelectedRow());
			}
		});

		addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent me) {
				new RunInThread() { public void runIn() throws Exception {
					sleep(250);
					edit(getSelectedRow());
				}}.start();
			}
		});


		model = new AbstractTableModel() {
			public int getColumnCount() {
				return 3;
			}
			public int getRowCount() {
				IRDatabase db = database();
				if (db == null) {
					return 0;
				}
				return db.size();
			}
			public String getColumnName(int col) {
				switch (col) {
					case 0: return "Model";
					case 1: return "Company";
					case 2: return "Description";
					default: return "invalid";
				}
			}
			public Object getValueAt(int row, int col) {
				IRDatabase db = database();
				if (db == null) {
					return null;
				}
				IRRemote remote = db.getByIndex(row);
				switch (col) {
					case 0: return remote.getModel();
					case 1: return remote.getCompany();
					case 2: return remote.getDescription();
					default: return "invalid";
				}
			}
		};
		setModel(model);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setDatabase(db);
	}

	IRDatabase database()
	{
		return db;
	}

	void setDatabase(IRDatabase db)
	{
		this.db = db;
		refresh();
	}

	void refresh()
	{
		model.fireTableDataChanged();
		getSelectionModel().setSelectionInterval(lastIdx,lastIdx);
	}

	void edit(int row)
	{
		lastIdx = row;
		if (row >= 0)
		{
			panel.edit(db.getByIndex(row));
		}
		else
		{
			panel.edit(null);
		}
	}
}

// ---( Inner Class IRRemoteTable )---
class IRRemoteTable extends JTable
{
	private IRRemotePanel panel;
	private IRRemote remote;
	private AbstractTableModel model;
	private int lastIdx;
	private int lastCol = -1;
	private boolean reverse = false;

	IRRemoteTable(IRRemotePanel panel)
	{
		this.panel = panel;

		addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent me) {
				edit(getSelectedRow());
			}
		});

		addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent me) {
				new RunInThread() { public void runIn() throws Exception {
					sleep(250);
					edit(getSelectedRow());
				}}.start();
			}
		});

		model = new AbstractTableModel() {
			public int getColumnCount() {
				return 5;
			}
			public int getRowCount() {
				IRRemote rem = remote();
				return rem != null ? rem.numKeys() : 0;
			}
			public String getColumnName(int col) {
				switch (col) {
					case 0: return "Key";
					case 1: return "Frequency";
					case 2: return "PulseIndex";
					case 3: return "Intro";
					case 4: return "Repeat";
					default: return "invalid";
				}
			}
			public Object getValueAt(int row, int col) {
				IRSignal sig = remote().getByIndex(row);
				switch (col) {
					case 0: return sig.getName();
					case 1: return sig.getFrequency()+"";
					case 2: return sig.getPulseIndex().toString();
					case 3: return printBurst(sig.getIntro());
					case 4: return printBurst(sig.getRepeat());
					default: return "invalid";
				}
			}
		};
		setModel(model);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		TableColumnModel cm = getColumnModel();
		cm.getColumn(0).setPreferredWidth(125);
		cm.getColumn(1).setPreferredWidth(75);
		cm.getColumn(2).setPreferredWidth(150);
		cm.getColumn(3).setPreferredWidth(150);
		cm.getColumn(4).setPreferredWidth(300);

		getTableHeader().addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent ev) {
				sort(getTableHeader().columnAtPoint(ev.getPoint()));
			}
		});
	}

	private void sort(int col)
	{
		if (col == lastCol)
		{
			reverse = !reverse;
		}
		else
		{
			reverse = false;
		}
		switch (col)
		{
			case 0: remote.sort(remote.SORT_NAME, reverse); break;
			case 3: remote.sort(remote.SORT_INTRO, reverse); break;
			case 4: remote.sort(remote.SORT_REPEAT, reverse); break;
			default:
				return;
		}
		lastCol = col;
		refresh();
	}

	private String printBurst(IRBurst b)
	{
		IRBurstCode bc = b.getBurstCode();
		return bc != null ? bc.toShortString() : b.toString();
	}

	IRRemote remote()
	{
		return remote;
	}

	void setRemote(IRRemote remote)
	{
		this.remote = remote;
		edit(-1);
		refresh();
	}

	void refresh()
	{
		model.fireTableDataChanged();
		getSelectionModel().setSelectionInterval(lastIdx,lastIdx);
	}

	void edit(int idx)
	{
		if (idx == lastIdx)
		{
			return;
		}
		lastIdx = idx;
		if (idx >= 0)
		{
			panel.edit(remote.getByIndex(idx));
		}
		else
		{
			panel.edit(null);
		}
	}
}

// ---( Inner Class IRBurstGraph )---
class IRBurstGraph extends JComponent
{
	private IRBurst burst;
	private Dimension size;
	private int signal[];
	private final int inset = 3;

	IRBurstGraph()
	{
		setBurst(null);
	}

	public void setBurst(IRBurst sig)
	{
		burst = sig;
		if (burst == null)
		{
			size = new Dimension(0,30);
			signal = new int[0];
		}
		else
		{
			int idx[] = burst.getPulseIndex().getIndexValues();
			int shortest = -1;
			for (int i=0; i<idx.length; i++)
			{
				if (shortest == -1 || (idx[i] < shortest && idx[i] > 0))
				{
					shortest = idx[i];
				}
			}
			int data[] = burst.getPulses();
			signal = new int[data.length];
			int width = 0;
			int pos = 0;
			int il = idx.length-1;
			for (int i=0; i<data.length; i++)
			{
				signal[pos] = Math.min(idx[Math.min(data[i],il)]/shortest+2,25);
				width += signal[pos++];
			}
			size = new Dimension(width+inset*2, 40);
		}
		repaint();
	}

	public void paint(Graphics g)
	{
		Dimension sz = getSize();
		if (burst == null)
		{
			return;
		}
		int last = inset;
		int pos = inset;
		int top = inset;
		int bot = sz.height-1-inset;
		for (int i=0; i<signal.length; i++)
		{
			// vertical
			g.drawLine(pos,top,pos,bot);
			pos += signal[i];
			// horizontal
			if (i % 2 == 0)
			{
				g.drawLine(last,top,pos,top);
			}
			else
			{
				g.drawLine(last,bot,pos,bot);
			}
			last = pos;
		}
	}

	public Dimension getMinimumSize()
	{
		return size;
	}

	public Dimension getPreferredSize()
	{
		return size;
	}

	public Dimension getMaximumSize()
	{
		return size;
	}
}

// ---( Inner Class RunInThread )---
abstract class RunInThread extends Thread
{
	public abstract void runIn()
		throws Exception;

	public void run()
	{
		try
		{
			runIn();
		}
		catch (Exception ex)
		{
			Tonto.errorDialog(ex);
		}
	}
}

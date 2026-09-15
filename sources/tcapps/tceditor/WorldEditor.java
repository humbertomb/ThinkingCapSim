/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import devices.pos.Position;
import tc.shared.world.WMAObject;
import tc.shared.world.WMBeacon;
import tc.shared.world.WMCBeacon;
import tc.shared.world.WMConnector;
import tc.shared.world.WMDock;
import tc.shared.world.WMFArea;
import tc.shared.world.WMIcon;
import tc.shared.world.WMObject;
import tc.shared.world.WMStart;
import tc.shared.world.WMWall;
import tc.shared.world.WMWaypoint;
import tc.shared.world.WMZone;
import tc.shared.world.World;
import tc.shared.world.WorldDxf;
import wucore.utils.color.ColorTool;
import wucore.utils.color.WColor;
import wucore.utils.geom.Line2;
import wucore.utils.geom.Point2;
import wucore.utils.geom.Point3;
import wucore.utils.geom.Polygon2;

/**
 * Editor of {@link World} maps: the toolbar of tools, the 2D canvas, the
 * element tree, the property table and the status bar, together with the
 * undo/redo history, the file operations and the topology editor. It is a
 * panel hosted either by {@link WorldEditorWindow} (the stand-alone
 * application, with the File menu) or by {@link WorldEditorDialog} (edits a
 * world in memory, without the File menu). The static helpers let the editor
 * treat every element of a {@link World} uniformly: creation, deletion,
 * hit-testing, dragging (whole element or one of its handles) and a
 * name/value property view; the text snapshots of the whole world serve the
 * undo/redo.
 */
public class WorldEditor extends JPanel implements WorldCanvas.Listener
{

	private static final long		serialVersionUID = 1L;

	static public final String		TITLE		= "ThinkingCap World Editor";
	static public final String		MAPS_DIR	= "./conf/maps";
	static private final int		MAX_UNDO	= 200;

	/* Model */
	protected World					world;
	protected File					file;					// current file, null if unsaved
	protected boolean				dirty		= false;

	/* Undo/redo (text snapshots of the whole world) */
	protected List<String>			undoStack	= new ArrayList<String> ();
	protected List<String>			undoNames	= new ArrayList<String> ();
	protected List<String>			redoStack	= new ArrayList<String> ();
	protected List<String>			redoNames	= new ArrayList<String> ();
	protected String				current;				// snapshot of the current state

	/* GUI */
	protected WorldCanvas			canvas;
	protected JTree					tree;
	protected DefaultTreeModel		treeModel;
	protected DefaultMutableTreeNode	treeRoot;
	protected JTable				propTable;
	protected PropertyModel			propModel;
	protected StatusBar				statusBar;
	protected JLabel				selLabel;
	protected JToggleButton[]		toolButtons	= new JToggleButton[WorldCanvas.NTOOLS];
	protected Action				undoAction, redoAction, deleteAction, duplicateAction;
	protected JCheckBoxMenuItem[]	layerItems	= new JCheckBoxMenuItem[WorldItem.NKINDS];
	protected boolean				collapseTree	= true;		// the tree starts closed (and closes again with every world loaded)
	protected JCheckBoxMenuItem		gridItem, snapItem, labelsItem;
	protected boolean				syncing		= false;	// avoids selection feedback loops

	/* 3D view */
	protected View3DController		view3d;
	protected Host					host;
	protected Action				topolAction;			// opens the topology editor (enabled when the world has zones)

	/* ------------------------------------------------------------------ */

	/** What the window or dialog hosting the editor needs to know. */
	public interface Host
	{
		/** The file or the modified state changed (title). */
		void editorStateChanged (WorldEditor editor);
	}

	public WorldEditor (World world, File file, Host host)
	{
		super (new BorderLayout ());
		this.world	= world;
		this.file	= file;
		this.host	= host;
		this.current = snapshot (world);

		buildGUI ();
		refreshAll ();
		updateTitle ();
	}

	public World		getWorld ()			{ return world; }
	public File			getFile ()			{ return file; }
	public boolean		isDirty ()			{ return dirty; }
	public WorldCanvas	getCanvas ()		{ return canvas; }

	/** Title of the hosting window: file name (or untitled) and the modified mark. */
	public String getTitle ()
	{
		return ((file == null) ? "untitled" + World.SUFFIX : file.getName ()) + (dirty ? " *" : "");
	}

	/** Releases the windows the editor owns (the 3D view). */
	public void dispose ()
	{
		view3d.dispose ();
	}

	/* ------------------------------------------------------------------ */
	/* GUI construction                                                    */
	/* ------------------------------------------------------------------ */

	private void buildGUI ()
	{
		canvas = new WorldCanvas (world);
		canvas.setListener (this);

		// --- element tree
		treeRoot	= new DefaultMutableTreeNode ("World");
		treeModel	= new DefaultTreeModel (treeRoot);
		tree		= new JTree (treeModel);
		tree.setRootVisible (false);
		tree.setShowsRootHandles (true);
		tree.getSelectionModel ().setSelectionMode (TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener (new TreeSelectionListener ()
		{
			public void valueChanged (TreeSelectionEvent e)
			{
				if (syncing)			return;
				DefaultMutableTreeNode	node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent ();
				if ((node != null) && (node.getUserObject () instanceof WorldItem))
				{
					syncing = true;
					canvas.setSelection ((WorldItem) node.getUserObject ());
					syncing = false;
				}
			}
		});
		JScrollPane		treeScroll = new JScrollPane (tree);
		treeScroll.setBorder (BorderFactory.createTitledBorder ("Elements"));

		// --- property table
		propModel	= new PropertyModel ();
		propTable	= new JTable (propModel)
		{
			private static final long	serialVersionUID = 1L;
			private final FileCellEditor.Renderer	fileRenderer = new FileCellEditor.Renderer ();
			private final ColorCellEditor.Renderer	colorRenderer = new ColorCellEditor.Renderer ();
			private final javax.swing.DefaultCellEditor	boolEditor = new javax.swing.DefaultCellEditor (new javax.swing.JComboBox<String> (new String[] { "true", "false" }));

			// file-path properties get a text field with a "..." browse button
			public javax.swing.table.TableCellEditor getCellEditor (int row, int column)
			{
				if (column == 1)
				{
					String	name = propModel.nameAt (row);
					if (name.equals ("shape"))			return FileCellEditor.SHAPE;
					if (name.equals ("image"))			return FileCellEditor.IMAGE;
					if (isTextureProperty (name))		return FileCellEditor.TEXTURE;
					if (name.equals ("color"))			return ColorCellEditor.INSTANCE;
					if (isBooleanProperty (name))	return boolEditor;
					if (name.equals ("flow") && (propModel.item != null) && (propModel.item.kind == WorldItem.DOCK))
						return new javax.swing.DefaultCellEditor (new javax.swing.JComboBox<String> (flowNames ()));
					if (name.equals ("movement") && (propModel.item != null) && (propModel.item.kind == WorldItem.AOBJECT))
						return new javax.swing.DefaultCellEditor (new javax.swing.JComboBox<String> (movementNames ()));
					if (name.equals ("icon") && (propModel.item != null) && WorldItem.isObject (propModel.item.kind))
					{
						// choose among the icons defined in the world
						String[]	labels = new String[world.icons ().size ()];
						for (int i = 0; i < labels.length; i++)		labels[i] = world.icons ().get (i).label;
						return new javax.swing.DefaultCellEditor (new javax.swing.JComboBox<String> (labels));
					}
				}
				return super.getCellEditor (row, column);
			}

			public javax.swing.table.TableCellRenderer getCellRenderer (int row, int column)
			{
				if (column == 1)
				{
					String	name = propModel.nameAt (row);
					if (isFileProperty (name))									return fileRenderer;
					if (name.equals ("color"))									return colorRenderer;
				}
				return super.getCellRenderer (row, column);
			}

			// disabled properties (e.g. the dynamics of a static animated object) are shown greyed out
			public java.awt.Component prepareRenderer (javax.swing.table.TableCellRenderer renderer, int row, int column)
			{
				java.awt.Component	c = super.prepareRenderer (renderer, row, column);
				boolean				enabled = propModel.isEnabled (row);
				c.setEnabled (enabled);
				if (!isCellSelected (row, column))
					c.setForeground (enabled ? getForeground () : java.awt.Color.GRAY);
				return c;
			}
		};
		propTable.setRowHeight (22);
		propTable.getColumnModel ().getColumn (0).setPreferredWidth (90);
		propTable.getColumnModel ().getColumn (1).setPreferredWidth (200);
		propTable.putClientProperty ("terminateEditOnFocusLost", Boolean.TRUE);
		JScrollPane		propScroll = new JScrollPane (propTable);
		propScroll.setBorder (BorderFactory.createTitledBorder ("Properties"));
		selLabel	= new JLabel (" ");
		selLabel.setBorder (BorderFactory.createEmptyBorder (2, 6, 2, 6));
		JPanel			propPanel = new JPanel (new BorderLayout ());
		propPanel.add (selLabel, BorderLayout.NORTH);
		propPanel.add (propScroll, BorderLayout.CENTER);

		JSplitPane		right = new JSplitPane (JSplitPane.VERTICAL_SPLIT, treeScroll, propPanel);
		right.setResizeWeight (0.55);
		right.setPreferredSize (new Dimension (320, 600));

		JSplitPane		center = new JSplitPane (JSplitPane.HORIZONTAL_SPLIT, canvas, right);
		center.setResizeWeight (1.0);

		// --- status bar
		statusBar	= new StatusBar ();
		view3d		= new View3DController (this, canvas);

		add (buildToolBar (), BorderLayout.WEST);
		add (center, BorderLayout.CENTER);
		add (statusBar, BorderLayout.SOUTH);
	}

	private JToolBar buildToolBar ()
	{
		JToolBar		tb = new JToolBar (JToolBar.VERTICAL);
		tb.setFloatable (false);
		ButtonGroup		group = new ButtonGroup ();

		// tooltips name the tool; how to use it is shown in the status bar (right side)
		addTool (tb, group, WorldCanvas.T_SELECT,	ToolIcon.SELECT,	"Select",				"S");
		addTool (tb, group, WorldCanvas.T_PAN,		ToolIcon.PAN,		"Pan",					"H");
		tb.addSeparator ();
		addTool (tb, group, WorldCanvas.T_WALL,		ToolIcon.WALL,		"Wall",					"W");
		addTool (tb, group, WorldCanvas.T_ZONE,		ToolIcon.ZONE,		"Zone",					"Z");
		addTool (tb, group, WorldCanvas.T_FAREA,	ToolIcon.FAREA,		"Forbidden area",		"F");
		// icons: create a new one (action) and edit the selected object's / icon's (tool)
		Action	newIcon = new AbstractAction ("New icon", new ToolIcon (ToolIcon.NEW_ICON))
		{
			public void actionPerformed (ActionEvent e)		{ canvas.newIcon (); }
		};
		newIcon.putValue (Action.SHORT_DESCRIPTION, "New icon  [Ctrl+I]");
		tb.add (ToolButtons.flatButton (newIcon));
		getInputMap (JComponent.WHEN_IN_FOCUSED_WINDOW).put (KeyStroke.getKeyStroke (KeyEvent.VK_I, Toolkit.getDefaultToolkit ().getMenuShortcutKeyMaskEx ()), "newIcon");
		getActionMap ().put ("newIcon", newIcon);
		addTool (tb, group, WorldCanvas.T_ICON,		ToolIcon.ICON,		"Edit icon",			"I");
		toolButtons[WorldCanvas.T_ICON].setEnabled (false);
		addTool (tb, group, WorldCanvas.T_OBJECT,	ToolIcon.OBJECT,	"Object",				"O");
		addTool (tb, group, WorldCanvas.T_AOBJECT,	ToolIcon.AOBJECT,	"Animated object",		"A");
		tb.addSeparator ();
		addTool (tb, group, WorldCanvas.T_CONNECTOR,	ToolIcon.CONNECTOR,	"Connector",			"D");
		addTool (tb, group, WorldCanvas.T_WAYPOINT,	ToolIcon.WAYPOINT,	"Waypoint",				"P");
		addTool (tb, group, WorldCanvas.T_DOCK,		ToolIcon.DOCK,		"Dock",					"K");
		tb.addSeparator ();
		addTool (tb, group, WorldCanvas.T_BEACON,	ToolIcon.BEACON,	"Strip beacon",			"B");
		addTool (tb, group, WorldCanvas.T_CBEACON,	ToolIcon.CBEACON,	"Cylindrical beacon",	"C");
		addTool (tb, group, WorldCanvas.T_PATH,		ToolIcon.PATH,		"Path point",			"T");
		addTool (tb, group, WorldCanvas.T_START,	ToolIcon.START,		"Start point (one per robot)",	"R");
		tb.addSeparator ();

		deleteAction = new AbstractAction ("Delete", new ToolIcon (ToolIcon.DELETE))
		{
			public void actionPerformed (ActionEvent e)		{ canvas.deleteSelection (); }
		};
		deleteAction.putValue (Action.SHORT_DESCRIPTION, "Delete  [Del]");
		tb.add (ToolButtons.flatButton (deleteAction));

		tb.add (ToolButtons.flatButton (ToolButtons.zoomFit (canvas)));
		tb.add (ToolButtons.flatButton (ToolButtons.zoomIn (canvas)));
		tb.add (ToolButtons.flatButton (ToolButtons.zoomOut (canvas)));

		// --- topology editor and 3D view toggle, at the bottom of the toolbar
		tb.add (Box.createVerticalGlue ());
		tb.addSeparator ();
		tb.add (ToolButtons.flatButton (topolAction ()));
		tb.add (view3d.button ());

		toolButtons[WorldCanvas.T_SELECT].setSelected (true);
		return tb;
	}

	/** Shows or hides the Java 3D view window. */
	public void show3D (boolean show)		{ view3d.show (show); }

	/** Opens the editor of the hierarchical topological map of the world (edited in place; an accepted edition is undoable). */
	public void editTopology ()
	{
		World	w = canvas.getWorld ();
		if ((w == null) || (w.zones ().n () == 0))		return;
		boolean	created = false;
		if (!w.hasTopology ())
		{
			int	r = JOptionPane.showConfirmDialog (this, "The world map doesn't contain any topology. Create one?", TopolEditorDialog.TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (r != JOptionPane.YES_OPTION)		return;
			w.setTopology (new tclib.planning.htopol.HTopolMap (w));
			created = true;
		}
		TopolEditorDialog	dlg = new TopolEditorDialog (SwingUtilities.getWindowAncestor (this), w);
		if (dlg.showDialog ())
		{
			if (created || dlg.isModified ())		worldChanged (created ? "Create topology" : "Edit topology");
		}
		else if (created)
			w.setTopology (null);							// cancelled: the world stays without topology
	}

	/** The topology editor needs zones to work on: its button and menu item follow the world. */
	private void updateTopologyActions ()
	{
		boolean	enabled = (canvas.getWorld () != null) && (canvas.getWorld ().zones ().n () > 0);
		if (topolAction != null)		topolAction.setEnabled (enabled);
	}

	private Action topolAction ()
	{
		if (topolAction == null)
			topolAction = ToolButtons.action ("Topology Editor", ToolIcon.TOPOLOGY, "Topology Editor  [Ctrl+T]", new Runnable () { public void run () { editTopology (); } });
		return topolAction;
	}

	private void addTool (JToolBar tb, ButtonGroup group, final int tool, int icon, String tip, String key)
	{
		JToggleButton	b = new JToggleButton (new ToolIcon (icon));
		b.setToolTipText (tip + "  [" + key + "]");
		b.setFocusable (false);
		b.addActionListener (new java.awt.event.ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ canvas.setTool (tool); canvas.requestFocusInWindow (); }
		});
		group.add (b);
		tb.add (b);
		toolButtons[tool] = b;

		// keyboard shortcut (single letter, when the canvas has the focus)
		final String	name = "tool" + tool;
		canvas.getInputMap (JComponent.WHEN_FOCUSED).put (KeyStroke.getKeyStroke (Character.toLowerCase (key.charAt (0))), name);
		canvas.getActionMap ().put (name, new AbstractAction ()
		{
			public void actionPerformed (ActionEvent e)		{ selectTool (tool); }
		});
	}

	private void selectTool (int tool)
	{
		toolButtons[tool].setSelected (true);
		canvas.setTool (tool);
	}

	/**
	 * The menu bar of the editor (File, Edit, View, Help); the host installs it.
	 * @param withFile  false to leave out the File menu (the dialog, which edits a world in memory)
	 */
	public JMenuBar buildMenuBar (boolean withFile)
	{
		int			mask = Toolkit.getDefaultToolkit ().getMenuShortcutKeyMaskEx ();
		JMenuBar	mb = new JMenuBar ();

		// --- File
		JMenu		mfile = new JMenu ("File");
		mfile.setMnemonic (KeyEvent.VK_F);
		mfile.add (item ("New World", KeyStroke.getKeyStroke (KeyEvent.VK_N, mask), new AbstractAction ()
		{
			public void actionPerformed (ActionEvent e)		{ clearWorld (); }
		}));
		mfile.add (item ("Load World...", KeyStroke.getKeyStroke (KeyEvent.VK_O, mask), new AbstractAction ()
		{
			public void actionPerformed (ActionEvent e)		{ loadWorld (); }
		}));
		mfile.add (item ("Save World", KeyStroke.getKeyStroke (KeyEvent.VK_S, mask), new AbstractAction ()
		{
			public void actionPerformed (ActionEvent e)		{ saveWorld (false); }
		}));
		mfile.add (item ("Save World As...", KeyStroke.getKeyStroke (KeyEvent.VK_S, mask | InputEvent.SHIFT_DOWN_MASK), new AbstractAction ()
		{
			public void actionPerformed (ActionEvent e)		{ saveWorld (true); }
		}));
		mfile.addSeparator ();
		mfile.add (item ("Import DXF...", null, new AbstractAction ()
		{
			public void actionPerformed (ActionEvent e)		{ importDxf (); }
		}));
		mfile.add (item ("Export DXF...", null, new AbstractAction ()
		{
			public void actionPerformed (ActionEvent e)		{ exportDxf (); }
		}));
		mfile.addSeparator ();
		mfile.add (item ("Quit", KeyStroke.getKeyStroke (KeyEvent.VK_Q, mask), new AbstractAction ()
		{
			public void actionPerformed (ActionEvent e)		{ quit (); }
		}));
		if (withFile)		mb.add (mfile);

		// --- Edit
		JMenu		medit = new JMenu ("Edit");
		medit.setMnemonic (KeyEvent.VK_E);
		undoAction = new AbstractAction ("Undo", new ToolIcon (ToolIcon.UNDO, 16))
		{
			public void actionPerformed (ActionEvent e)		{ undo (); }
		};
		redoAction = new AbstractAction ("Redo", new ToolIcon (ToolIcon.REDO, 16))
		{
			public void actionPerformed (ActionEvent e)		{ redo (); }
		};
		medit.add (item (undoAction, KeyStroke.getKeyStroke (KeyEvent.VK_Z, mask)));
		medit.add (item (redoAction, KeyStroke.getKeyStroke (KeyEvent.VK_Z, mask | InputEvent.SHIFT_DOWN_MASK)));
		medit.addSeparator ();
		duplicateAction = new AbstractAction ("Duplicate")
		{
			public void actionPerformed (ActionEvent e)		{ canvas.duplicateSelection (); }
		};
		duplicateAction.setEnabled (false);
		medit.add (item (duplicateAction, KeyStroke.getKeyStroke (KeyEvent.VK_D, mask)));
		medit.addSeparator ();
		medit.add (item (deleteAction, KeyStroke.getKeyStroke (KeyEvent.VK_DELETE, 0)));
		// (Esc itself is handled by the canvas: it first cancels drawings / leaves the current tool, then deselects)
		medit.add (item ("Deselect", null, new AbstractAction ()
		{
			public void actionPerformed (ActionEvent e)		{ canvas.setSelection (null); selectTool (WorldCanvas.T_SELECT); }
		}));

		mb.add (medit);

		// --- View
		JMenu		mview = new JMenu ("View");
		mview.setMnemonic (KeyEvent.VK_V);
		mview.add (item ("Zoom to Fit", KeyStroke.getKeyStroke (KeyEvent.VK_0, mask), new AbstractAction ()
		{
			public void actionPerformed (ActionEvent e)		{ canvas.zoomToFit (); }
		}));
		mview.add (item ("Zoom In", KeyStroke.getKeyStroke (KeyEvent.VK_PLUS, mask), new AbstractAction ()
		{
			public void actionPerformed (ActionEvent e)		{ canvas.zoom (1.25); }
		}));
		mview.add (item ("Zoom Out", KeyStroke.getKeyStroke (KeyEvent.VK_MINUS, mask), new AbstractAction ()
		{
			public void actionPerformed (ActionEvent e)		{ canvas.zoom (0.8); }
		}));
		mview.addSeparator ();
		gridItem = new JCheckBoxMenuItem ("Show Grid", true);
		gridItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_G, mask));
		gridItem.addActionListener (new java.awt.event.ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ canvas.setGridVisible (gridItem.isSelected ()); }
		});
		mview.add (gridItem);
		snapItem = new JCheckBoxMenuItem ("Snap to Grid", false);
		snapItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_G, mask | InputEvent.SHIFT_DOWN_MASK));
		snapItem.addActionListener (new java.awt.event.ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ canvas.setSnapEnabled (snapItem.isSelected ()); }
		});
		mview.add (snapItem);
		labelsItem = new JCheckBoxMenuItem ("Show Labels", true);
		labelsItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_L, mask));
		labelsItem.addActionListener (new java.awt.event.ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ canvas.setLabelsVisible (labelsItem.isSelected ()); }
		});
		mview.add (labelsItem);
		mview.addSeparator ();
		JMenu		mlayers = new JMenu ("Layers");
		for (int k = 0; k < WorldItem.ICON; k++)
		{
			final int	kind = k;
			layerItems[k] = new JCheckBoxMenuItem (WorldItem.PLURALS[k], true);
			layerItems[k].addActionListener (new java.awt.event.ActionListener ()
			{
				public void actionPerformed (ActionEvent e)		{ canvas.setKindVisible (kind, layerItems[kind].isSelected ()); }
			});
			mlayers.add (layerItems[k]);
		}
		mlayers.addSeparator ();
		mlayers.add (item ("Show All Layers", null, new AbstractAction ()
		{
			public void actionPerformed (ActionEvent e)
			{
				for (int k = 0; k < WorldItem.ICON; k++) { layerItems[k].setSelected (true); canvas.setKindVisible (k, true); }
			}
		}));
		mview.add (mlayers);
		mview.addSeparator ();
		JMenuItem	mtopol = new JMenuItem (topolAction ());
		mtopol.setText ("Topology Editor...");
		mtopol.setIcon (null);
		mtopol.setToolTipText (null);
		mtopol.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_T, mask));
		mview.add (mtopol);
		mview.add (view3d.menuItem (mask));
		mb.add (mview);

		// --- Help
		JMenu		mhelp = new JMenu ("Help");
		mhelp.setMnemonic (KeyEvent.VK_H);
		mhelp.add (item ("Mouse and Keyboard...", KeyStroke.getKeyStroke (KeyEvent.VK_F1, 0), new AbstractAction ()
		{
			public void actionPerformed (ActionEvent e)		{ showHelp (); }
		}));
		mhelp.add (item ("About...", null, new AbstractAction ()
		{
			public void actionPerformed (ActionEvent e)
			{
				JOptionPane.showMessageDialog (WorldEditor.this,
					TITLE + "\n\nEditor of .world maps for the ThinkingCap simulator.\n(c) 2026 Humberto Martinez Barbera",
					"About", JOptionPane.INFORMATION_MESSAGE);
			}
		}));
		mb.add (mhelp);

		return mb;
	}

	private JMenuItem item (String name, KeyStroke key, Action action)
	{
		action.putValue (Action.NAME, name);
		return item (action, key);
	}

	private JMenuItem item (Action action, KeyStroke key)
	{
		JMenuItem	mi = new JMenuItem (action);
		if (key != null)		mi.setAccelerator (key);
		return mi;
	}

	private void showHelp ()
	{
		String	msg =
			"Tools (left toolbar, or press the letter with the map focused):\n" +
			"  S  Select / move: click an element, drag it, or drag its handles.\n" +
			"       Circular handle of waypoints, docks, objects and start = orientation.\n" +
			"  H  Pan. Also middle button, Alt+drag or Space+drag with any tool.\n" +
			"  W  Wall, D  Connector, Z  Zone: drag on the map.\n" +
			"  F  Forbidden area: click the vertices, double-click / Enter to close.\n" +
			"  O  Object, P  Waypoint, K  Dock, B  Strip beacon, C  Cylindrical beacon,\n" +
			"  T  Path point, R  Start point: click to place.\n" +
			"  I  Edit icon of the selected object / icon (or double-click an object): drag vertices, click a\n" +
			"       segment to insert a vertex, drag on empty space (Shift+drag from a vertex) to add a segment,\n" +
			"       right click / Del removes. Icons are shared: editing one changes every object using it.\n" +
			"       The 'New icon' button (Ctrl+I) creates an icon; its first click sets the reference point.\n" +
			"  Right click with a creation tool returns to Select.\n\n" +
			"Keyboard:  Del deletes, arrows nudge the selection, Esc deselects / cancels,\n" +
			"  Ctrl+Z / Ctrl+Shift+Z undo / redo, mouse wheel zooms, Ctrl+0 zoom to fit.\n\n" +
			"3D view: the button at the bottom of the toolbar (or Ctrl+3) opens a Java 3D window that\n" +
			"  follows every change and highlights the selection.\n\n" +
			"Properties: edit any value in the table and press Enter. Labels of zones, connectors,\n" +
			"  waypoints and docks must be unique. 'Defaults' (Edit menu) holds the default\n" +
			"  wall/connector sizes and textures written to the file.";
		JOptionPane.showMessageDialog (this, msg, "Mouse and Keyboard", JOptionPane.INFORMATION_MESSAGE);
	}

	/* ------------------------------------------------------------------ */
	/* WorldCanvas.Listener                                                */
	/* ------------------------------------------------------------------ */

	public void selectionChanged (WorldItem item)
	{
		propModel.setItem (item);
		selLabel.setText ((item == null) ? " " : WorldItem.NAMES[item.kind] + ":  " + describe (world, item));
		deleteAction.setEnabled ((item != null) && !WorldItem.isSettings (item.kind) && ((item.kind != WorldItem.START) || (world.n_starts () > 1)));
		if (duplicateAction != null)		duplicateAction.setEnabled ((item != null) && !WorldItem.isSettings (item.kind));
		// the icon tool only applies to elements that have an icon (objects) or to icons themselves
		boolean	hasIcon = (item != null) && (WorldItem.isObject (item.kind) || (item.kind == WorldItem.ICON));
		toolButtons[WorldCanvas.T_ICON].setEnabled (hasIcon);
		if (!hasIcon && (canvas.getTool () == WorldCanvas.T_ICON))		selectTool (WorldCanvas.T_SELECT);
		view3d.selectionChanged (item);
		if (!syncing)
		{
			syncing = true;
			selectInTree (item);
			syncing = false;
		}
	}

	public void worldChanged (String what)
	{
		pushUndo (what);
		updateTopologyActions ();
		refreshTree ();
		propModel.refresh ();
		selectionChanged (canvas.getSelection ());
		view3d.worldChanged ();
	}

	public void worldPreview ()
	{
		view3d.worldPreview ();
	}

	public void statusChanged (String text)
	{
		statusBar.setStatus (text);
	}

	public void toolFinished ()
	{
		selectTool (WorldCanvas.T_SELECT);
	}

	public void usageChanged (String text)
	{
		statusBar.setUsage (text);
	}

	public void toolRequested (int tool)
	{
		selectTool (tool);
	}

	/* ------------------------------------------------------------------ */
	/* Undo / redo                                                         */
	/* ------------------------------------------------------------------ */

	private void pushUndo (String what)
	{
		String	snap = snapshot (world);
		if (snap.equals (current))		return;			// nothing really changed

		undoStack.add (current);
		undoNames.add (what);
		if (undoStack.size () > MAX_UNDO) { undoStack.remove (0); undoNames.remove (0); }
		redoStack.clear ();
		redoNames.clear ();
		current	= snap;
		dirty	= true;
		updateTitle ();
		updateUndoActions ();
	}

	private void undo ()
	{
		if (undoStack.isEmpty ())		return;
		redoStack.add (current);
		redoNames.add (undoNames.get (undoNames.size () - 1));
		current = undoStack.remove (undoStack.size () - 1);
		undoNames.remove (undoNames.size () - 1);
		applySnapshot ();
	}

	private void redo ()
	{
		if (redoStack.isEmpty ())		return;
		undoStack.add (current);
		undoNames.add (redoNames.get (redoNames.size () - 1));
		current = redoStack.remove (redoStack.size () - 1);
		redoNames.remove (redoNames.size () - 1);
		applySnapshot ();
	}

	private void applySnapshot ()
	{
		WorldItem	sel = canvas.getSelection ();
		world = restore (current);
		canvas.setWorld (world);
		dirty = true;
		refreshAll ();
		canvas.setSelection (sel);		// kept if it still exists
		updateTitle ();
		updateUndoActions ();
		view3d.worldChanged ();
	}

	private void updateUndoActions ()
	{
		undoAction.setEnabled (!undoStack.isEmpty ());
		undoAction.putValue (Action.NAME, undoStack.isEmpty () ? "Undo" : "Undo " + undoNames.get (undoNames.size () - 1));
		redoAction.setEnabled (!redoStack.isEmpty ());
		redoAction.putValue (Action.NAME, redoStack.isEmpty () ? "Redo" : "Redo " + redoNames.get (redoNames.size () - 1));
	}

	/* ------------------------------------------------------------------ */
	/* File operations                                                     */
	/* ------------------------------------------------------------------ */

	/** Asks to save unsaved changes; false when the user cancels. */
	public boolean confirmDiscard ()
	{
		if (!dirty)						return true;
		int		r = JOptionPane.showConfirmDialog (this, "The world has unsaved changes. Save them first?",
					TITLE, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (r == JOptionPane.CANCEL_OPTION)	return false;
		if (r == JOptionPane.YES_OPTION)		return saveWorld (false);
		return true;
	}

	private JFileChooser chooser (String ext, String desc)
	{
		File		dir = (file != null) ? file.getParentFile () : new File (MAPS_DIR);
		if ((dir == null) || !dir.isDirectory ())		dir = new File (".");
		JFileChooser	fc = new JFileChooser (dir);
		fc.setFileFilter (new FileNameExtensionFilter (desc, ext));
		return fc;
	}

	/** File > New: replaces the world with an empty one. */
	public void clearWorld ()
	{
		if (!confirmDiscard ())			return;
		setWorld (newWorld (), null);
	}

	public void loadWorld ()
	{
		if (!confirmDiscard ())			return;
		JFileChooser	fc = chooser ("world", "World maps (*.world)");
		fc.setDialogTitle ("Load World");
		if (fc.showOpenDialog (this) != JFileChooser.APPROVE_OPTION)		return;
		loadWorld (fc.getSelectedFile ());
	}

	public void loadWorld (File f)
	{
		try
		{
			World	w = new World (f.getPath ());
			setWorld (w, f);
		} catch (Exception e)
		{
			e.printStackTrace ();
			JOptionPane.showMessageDialog (this, "Cannot load " + f.getName () + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
		}
	}

	private void setWorld (World w, File f)
	{
		collapseTree = true;			// a world just loaded shows its categories closed
		normalisePaths (w);				// old files may name their resources without the leading "./"
		world	= w;
		file	= f;
		dirty	= false;
		current	= snapshot (world);
		undoStack.clear ();	undoNames.clear ();	redoStack.clear ();	redoNames.clear ();
		canvas.setWorld (world);
		refreshAll ();
		updateTitle ();
		updateUndoActions ();
		view3d.worldChanged ();
		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()		{ canvas.zoomToFit (); }
		});
	}

	/** @return true if the world was written */
	public boolean saveWorld (boolean askName)
	{
		File	f = file;
		if (askName || (f == null))
		{
			JFileChooser	fc = chooser ("world", "World maps (*.world)");
			fc.setDialogTitle ("Save World");
			if (file != null)		fc.setSelectedFile (file);
			if (fc.showSaveDialog (this) != JFileChooser.APPROVE_OPTION)		return false;
			f = fc.getSelectedFile ();
			if (!f.getName ().toLowerCase ().endsWith (World.SUFFIX))
				f = new File (f.getParentFile (), f.getName () + World.SUFFIX);
			if (f.exists () && !f.equals (file))
			{
				int	r = JOptionPane.showConfirmDialog (this, f.getName () + " already exists. Overwrite?", TITLE, JOptionPane.YES_NO_OPTION);
				if (r != JOptionPane.YES_OPTION)		return false;
			}
		}
		try
		{
			world.toFile (f.getPath ());
			file	= f;
			dirty	= false;
			updateTitle ();
			statusChanged ("Saved " + f.getPath ());
			return true;
		} catch (Exception e)
		{
			e.printStackTrace ();
			JOptionPane.showMessageDialog (this, "Cannot save " + f.getName () + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	public void importDxf ()
	{
		if (!confirmDiscard ())			return;
		JFileChooser	fc = chooser ("dxf", "AutoCAD DXF (*.dxf)");
		fc.setDialogTitle ("Import DXF");
		if (fc.showOpenDialog (this) != JFileChooser.APPROVE_OPTION)		return;
		try
		{
			setWorld (WorldDxf.read (fc.getSelectedFile ().getPath ()), null);
			dirty = true;
			updateTitle ();
		} catch (Exception e)
		{
			e.printStackTrace ();
			JOptionPane.showMessageDialog (this, "Cannot import DXF:\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
		}
	}

	public void exportDxf ()
	{
		JFileChooser	fc = chooser ("dxf", "AutoCAD DXF (*.dxf)");
		fc.setDialogTitle ("Export DXF");
		if (fc.showSaveDialog (this) != JFileChooser.APPROVE_OPTION)		return;
		File	f = fc.getSelectedFile ();
		if (!f.getName ().toLowerCase ().endsWith (".dxf"))
			f = new File (f.getParentFile (), f.getName () + ".dxf");
		try
		{
			WorldDxf.write (world, f.getPath ());
			statusChanged ("Exported " + f.getPath ());
		} catch (Exception e)
		{
			e.printStackTrace ();
			JOptionPane.showMessageDialog (this, "Cannot export DXF:\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Closes the hosting window (after confirming unsaved changes). */
	public void quit ()
	{
		Window	w = SwingUtilities.getWindowAncestor (this);
		if (w != null)		w.dispatchEvent (new java.awt.event.WindowEvent (w, java.awt.event.WindowEvent.WINDOW_CLOSING));
	}

	private void updateTitle ()
	{
		if (host != null)		host.editorStateChanged (this);
	}

	/* ------------------------------------------------------------------ */
	/* Element tree                                                        */
	/* ------------------------------------------------------------------ */

	private void refreshAll ()
	{
		updateTopologyActions ();
		refreshTree ();
		propModel.setItem (canvas.getSelection ());
		selectionChanged (canvas.getSelection ());
	}

	private void refreshTree ()
	{
		// remember expanded categories
		boolean[]	expanded = new boolean[WorldItem.NKINDS];
		for (int i = 0; i < treeRoot.getChildCount (); i++)
		{
			DefaultMutableTreeNode	n = (DefaultMutableTreeNode) treeRoot.getChildAt (i);
			int						kind = ((Integer) n.getUserObject ()).intValue ();
			expanded[kind] = tree.isExpanded (new TreePath (n.getPath ()));
		}
		boolean		collapse = collapseTree || (treeRoot.getChildCount () == 0);
		collapseTree = false;

		syncing = true;
		treeRoot.removeAllChildren ();
		for (int kind = 0; kind < WorldItem.NKINDS; kind++)
		{
			if (kind == WorldItem.CBEACON)			continue;			// listed under the strip beacons category
			if (kind == WorldItem.BEHAVIOUR)		continue;			// listed under the default values category
			int						n = count (world, kind);
			DefaultMutableTreeNode	cat;
			if (kind == WorldItem.BEACON)
			{
				// both beacon types share the "Beacons" category
				int		nc = count (world, WorldItem.CBEACON);
				cat = new KindNode (kind, n + nc);
				for (int i = 0; i < n; i++)
					cat.add (new ItemNode (new WorldItem (WorldItem.BEACON, i)));
				for (int i = 0; i < nc; i++)
					cat.add (new ItemNode (new WorldItem (WorldItem.CBEACON, i)));
			}
			else if (kind == WorldItem.GEOMETRY)
			{
				// the default values hold one group of properties per settings kind
				cat = new KindNode (kind, n);
				cat.add (new ItemNode (new WorldItem (WorldItem.GEOMETRY, 0)));
				cat.add (new ItemNode (new WorldItem (WorldItem.BEHAVIOUR, 0)));
			}
			else
			{
				cat = new KindNode (kind, n);
				for (int i = 0; i < n; i++)
					cat.add (new ItemNode (new WorldItem (kind, i)));
			}
			treeRoot.add (cat);
		}
		treeModel.reload ();
		for (int i = 0; i < treeRoot.getChildCount (); i++)
		{
			DefaultMutableTreeNode	n = (DefaultMutableTreeNode) treeRoot.getChildAt (i);
			int						kind = ((Integer) n.getUserObject ()).intValue ();
			if (!collapse && expanded[kind])		tree.expandPath (new TreePath (n.getPath ()));
		}
		selectInTree (canvas.getSelection ());
		syncing = false;
	}

	private void selectInTree (WorldItem item)
	{
		if (item == null)
		{
			tree.clearSelection ();
			return;
		}
		for (int i = 0; i < treeRoot.getChildCount (); i++)
		{
			DefaultMutableTreeNode	cat = (DefaultMutableTreeNode) treeRoot.getChildAt (i);
			for (int j = 0; j < cat.getChildCount (); j++)
			{
				DefaultMutableTreeNode	n = (DefaultMutableTreeNode) cat.getChildAt (j);
				if (item.equals (n.getUserObject ()))
				{
					TreePath	path = new TreePath (n.getPath ());
					tree.setSelectionPath (path);
					tree.scrollPathToVisible (path);
					return;
				}
			}
		}
	}

	/** Category node: user object is the kind (Integer). */
	private class KindNode extends DefaultMutableTreeNode
	{
		private static final long	serialVersionUID = 1L;
		int		n;
		KindNode (int kind, int n)	{ super (Integer.valueOf (kind)); this.n = n; }
		public String toString ()
		{
			int	kind = ((Integer) getUserObject ()).intValue ();
			if (WorldItem.isSettings (kind))		return WorldItem.PLURALS[kind];
			if (kind == WorldItem.BEACON)											return "Beacons  (" + n + ")";
			return WorldItem.PLURALS[kind] + "  (" + n + ")";
		}
	}

	/** Element node: user object is the WorldItem. */
	private class ItemNode extends DefaultMutableTreeNode
	{
		private static final long	serialVersionUID = 1L;
		ItemNode (WorldItem it)		{ super (it); }
		public String toString ()	{ return describe (world, (WorldItem) getUserObject ()); }
	}

	/* ------------------------------------------------------------------ */
	/* Property table                                                      */
	/* ------------------------------------------------------------------ */

	private class PropertyModel extends AbstractTableModel
	{
		private static final long	serialVersionUID = 1L;
		WorldItem		item;
		String[]		names = new String[0];

		void setItem (WorldItem it)
		{
			if (propTable.isEditing ())		propTable.getCellEditor ().cancelCellEditing ();
			item	= valid (world, it) ? it : null;
			names	= (item == null) ? new String[0] : propertyNames (world, item);
			fireTableDataChanged ();
		}

		void refresh ()
		{
			if (!valid (world, item))		setItem (null);
			else									fireTableRowsUpdated (0, Math.max (0, names.length - 1));
		}

		String nameAt (int r)							{ return ((r >= 0) && (r < names.length)) ? names[r] : ""; }
		public int getRowCount ()						{ return names.length; }
		public int getColumnCount ()					{ return 2; }
		public String getColumnName (int c)				{ return (c == 0) ? "Property" : "Value"; }
		public boolean isCellEditable (int r, int c)	{ return (c == 1) && isEnabled (r); }
		boolean isEnabled (int r)						{ return (item == null) || isEnabledProperty (world, item, nameAt (r)); }

		public Object getValueAt (int r, int c)
		{
			if (item == null)			return "";
			if (c == 0)					return names[r];
			return value (names[r], getProperty (world, item, names[r]));
		}

		public void setValueAt (Object value, int r, int c)
		{
			if ((item == null) || (c != 1))		return;
			String	v = value (names[r], (value == null) ? "" : value.toString ());
			if (v.equals (value (names[r], getProperty (world, item, names[r]))))		return;
			try
			{
				setProperty (world, item, names[r], v);
				canvas.repaint ();
				worldChanged ("Edit " + names[r]);
			} catch (IllegalArgumentException e)
			{
				JOptionPane.showMessageDialog (WorldEditor.this, e.getMessage (), "Invalid value", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/* ================================================================== */
	/* Static helpers over the elements of a World                          */
	/* ================================================================== */

	static public final double		ARROW		= 0.5;		// Length of orientation handles (m)
	static public final double		MIN_RADIUS	= 0.15;		// Smallest virtual radius reachable with the handle (m)

	/* ------------------------------------------------------------------ */
	/* World creation and snapshots                                        */
	/* ------------------------------------------------------------------ */

	/** A world with all its collections created but empty. */
	static public World newWorld ()
	{
		return World.empty ();
	}

	/** Serialises the world to its JSON text (in memory, for undo/redo). */
	static public String snapshot (World w)
	{
		return w.toJsonText ();
	}

	/** Rebuilds a world from a {@link #snapshot(World)} string. */
	static public World restore (String snapshot)
	{
		return World.fromJsonText (snapshot);
	}

	/* ------------------------------------------------------------------ */
	/* Generic access                                                      */
	/* ------------------------------------------------------------------ */

	static public int count (World w, int kind)
	{
		switch (kind)
		{
		case WorldItem.ZONE:		return w.zones ().n ();
		case WorldItem.FAREA:		return w.fareas ().n ();
		case WorldItem.PATH:		return w.path ().size ();
		case WorldItem.WALL:		return w.walls ().n ();
		case WorldItem.OBJECT:		return w.objects ().size ();
		case WorldItem.AOBJECT:		return w.aobjects ().size ();
		case WorldItem.CONNECTOR:		return w.connectors ().n ();
		case WorldItem.BEACON:		return w.beacons ().size ();
		case WorldItem.CBEACON:		return w.cbeacons ().size ();
		case WorldItem.WAYPOINT:	return w.wps ().size ();
		case WorldItem.DOCK:		return w.docks ().size ();
		case WorldItem.ICON:		return w.icons ().size ();
		case WorldItem.START:		return w.n_starts ();
		case WorldItem.GEOMETRY:
		case WorldItem.BEHAVIOUR:	return 1;
		}
		return 0;
	}

	static public boolean valid (World w, WorldItem it)
	{
		return (it != null) && (it.index >= 0) && (it.index < count (w, it.kind));
	}

	/** Short text used in the element tree. */
	static public String describe (World w, WorldItem it)
	{
		if (!valid (w, it))				return "?";

		switch (it.kind)
		{
		case WorldItem.ZONE:		return w.zones ().at (it.index).label;
		case WorldItem.FAREA:		return w.fareas ().at (it.index).label;
		case WorldItem.PATH:		return "P" + it.index;		// the coordinates are shown in the property table
		case WorldItem.WALL:		return "LINE_" + it.index;
		case WorldItem.OBJECT:
		{
			WMObject	o = w.objects ().get (it.index);
			return "OBJECT_" + it.index + "  [" + o.iconId + "]" + ((o.shape != null) ? " " + shortName (o.shape) : "");
		}
		case WorldItem.AOBJECT:
		{
			WMAObject	o = w.aobjects ().get (it.index);
			return o.label + "  [" + o.iconId + "]" + ((o.dynamics != null) ? " " + o.dynamics.substring (o.dynamics.lastIndexOf ('.') + 1) : "");
		}
		case WorldItem.CONNECTOR:		return w.connectors ().at (it.index).label;
		case WorldItem.BEACON:		return w.beacons ().get (it.index).label;
		case WorldItem.CBEACON:		return w.cbeacons ().get (it.index).label;
		case WorldItem.WAYPOINT:	return w.wps ().get (it.index).label;
		case WorldItem.DOCK:		return w.docks ().get (it.index).label;
		case WorldItem.START:		return "START_" + (it.index + 1);		// the pose is shown in the property table
		case WorldItem.ICON:
		{
			WMIcon	ic = w.icons ().get (it.index);
			int		users = iconUsers (w, ic.label);
			return ic.label + "  (" + ic.n () + " seg, " + users + " obj)";
		}
		case WorldItem.GEOMETRY:	return "Geometries";
		case WorldItem.BEHAVIOUR:	return "Behaviours";
		}
		return "?";
	}

	static private String shortName (String path)
	{
		int		i = path.lastIndexOf ('/');
		return (i >= 0) ? path.substring (i + 1) : path;
	}

	static public String fmt (double v)
	{
		if (Math.abs (v - Math.rint (v)) < 1e-9)
			return Long.toString (Math.round (v));
		String s = String.format (Locale.US, "%.3f", v);
		while (s.endsWith ("0"))		s = s.substring (0, s.length () - 1);
		return s;
	}

	/* ------------------------------------------------------------------ */
	/* Creation and deletion                                               */
	/* ------------------------------------------------------------------ */

	/** A label not used by any zone, connector, waypoint or dock (they share a namespace in World.getType). */
	static public String uniqueLabel (World w, String prefix)
	{
		int		i = 0;
		while (true)
		{
			String	name = prefix + i;
			if ((w.getType (name) == World.NONE) && (indexOfLabel (w, WorldItem.BEACON, name) < 0)
					&& (indexOfLabel (w, WorldItem.CBEACON, name) < 0) && (indexOfLabel (w, WorldItem.FAREA, name) < 0)
					&& (indexOfLabel (w, WorldItem.AOBJECT, name) < 0))
				return name;
			i++;
		}
	}

	static private int indexOfLabel (World w, int kind, String label)
	{
		int		n = count (w, kind);
		for (int i = 0; i < n; i++)
		{
			String	l = label (w, new WorldItem (kind, i));
			if (label.equals (l))		return i;
		}
		return -1;
	}

	static public String label (World w, WorldItem it)
	{
		switch (it.kind)
		{
		case WorldItem.ZONE:		return w.zones ().at (it.index).label;
		case WorldItem.FAREA:		return w.fareas ().at (it.index).label;
		case WorldItem.CONNECTOR:		return w.connectors ().at (it.index).label;
		case WorldItem.BEACON:		return w.beacons ().get (it.index).label;
		case WorldItem.CBEACON:		return w.cbeacons ().get (it.index).label;
		case WorldItem.WAYPOINT:	return w.wps ().get (it.index).label;
		case WorldItem.DOCK:		return w.docks ().get (it.index).label;
		case WorldItem.AOBJECT:		return w.aobjects ().get (it.index).label;
		case WorldItem.ICON:		return w.icons ().get (it.index).label;
		}
		return null;
	}

	/** Number of objects referencing the icon. */
	static public int iconUsers (World w, String iconLabel)
	{
		return iconUserItems (w, iconLabel).size ();
	}

	/** Objects (static and animated) referencing the icon. */
	static public List<WorldItem> iconUserItems (World w, String iconLabel)
	{
		List<WorldItem>	v = new ArrayList<WorldItem> ();
		for (int i = 0; i < w.objects ().size (); i++)
			if (iconLabel.equals (w.objects ().get (i).iconId))		v.add (new WorldItem (WorldItem.OBJECT, i));
		for (int i = 0; i < w.aobjects ().size (); i++)
			if (iconLabel.equals (w.aobjects ().get (i).iconId))		v.add (new WorldItem (WorldItem.AOBJECT, i));
		return v;
	}

	/** The object (static or animated) an item refers to, or null when the item is not an object. */
	static public WMObject object (World w, WorldItem it)
	{
		if (!valid (w, it))						return null;
		if (it.kind == WorldItem.OBJECT)		return w.objects ().get (it.index);
		if (it.kind == WorldItem.AOBJECT)		return w.aobjects ().get (it.index);
		return null;
	}

	/** Creates an empty icon with a unique label and returns its item. */
	static public WorldItem addIcon (World w, String prefix)
	{
		WMIcon	ic = new WMIcon (w.uniqueIconLabel (prefix), new Line2[0]);
		w.icons ().add (ic);
		return new WorldItem (WorldItem.ICON, w.icons ().size () - 1);
	}

	/**
	 * Gives a reference pose to an icon that has none (legacy files): the pose
	 * of its first user object, or (x, y, 0) when no object uses it.
	 */
	static public void defaultIconPose (World w, WMIcon ic, double x, double y)
	{
		if (ic.hasPose ())				return;
		List<WorldItem>	users = iconUserItems (w, ic.label);
		if (users.size () > 0)
		{
			WMObject	u = object (w, users.get (0));
			ic.setPose (u.pos.x (), u.pos.y (), u.pos.z (), u.a);
		}
		else
			ic.setPose (x, y, 0.0, 0.0);
	}

	/** The default icon for new objects: a 0.4 m square, created on demand. */
	static public WMIcon defaultIcon (World w)
	{
		double		h = 0.2;
		Line2[]		sq = { new Line2 (-h, -h, h, -h), new Line2 (h, -h, h, h), new Line2 (h, h, -h, h), new Line2 (-h, h, -h, -h) };
		return w.registerIcon (sq, "box");
	}

	static public WorldItem addWall (World w, double x1, double y1, double x2, double y2)
	{
		WMWall		wall = new WMWall ();
		wall.edge		= new Line2 (x1, y1, x2, y2);
		wall.width		= w.walls ().defaultWidth ();
		wall.height		= w.walls ().defaultHeight ();
		wall.texture	= w.walls ().defaultTexture ();
		wall.label		= "LINE_" + w.walls ().n ();
		w.walls ().add (wall);
		return new WorldItem (WorldItem.WALL, w.walls ().n () - 1);
	}

	static public WorldItem addConnector (World w, double x1, double y1, double x2, double y2)
	{
		WMConnector		connector = new WMConnector ();
		connector.edge		= new Line2 (x1, y1, x2, y2);
		connector.path		= new Line2 (x1, y1, x2, y2);
		connector.width		= w.connectors ().defaultWidth ();
		connector.height		= w.connectors ().defaultHeight ();
		connector.texture	= w.connectors ().defaultTexture ();
		connector.label		= uniqueLabel (w, "Connector");
		w.connectors ().add (connector);
		return new WorldItem (WorldItem.CONNECTOR, w.connectors ().n () - 1);
	}

	static public WorldItem addZone (World w, double x1, double y1, double x2, double y2)
	{
		WMZone		zone = new WMZone ();
		zone.area		= new java.awt.geom.Rectangle2D.Double (Math.min (x1, x2), Math.min (y1, y2), Math.abs (x2 - x1), Math.abs (y2 - y1));
		zone.texture	= w.zones ().defaultTexture ();
		zone.label		= uniqueLabel (w, "Zone");
		w.zones ().add (zone);
		return new WorldItem (WorldItem.ZONE, w.zones ().n () - 1);
	}

	static public WorldItem addFArea (World w, List<Point2> pts)
	{
		WMFArea		fa = new WMFArea ();
		fa.polygon		= new Polygon2 ();
		for (Point2 p : pts)
			fa.polygon.addPoint (p.x (), p.y ());
		fa.texture		= w.fareas ().defaultTexture ();
		fa.label		= uniqueLabel (w, "FArea");
		w.fareas ().add (fa);
		return new WorldItem (WorldItem.FAREA, w.fareas ().n () - 1);
	}

	/** Object with a square icon of side <code>size</code> centred at (x, y). */
	static public WorldItem addObject (World w, double x, double y, double size)
	{
		WMObject	obj = new WMObject ();
		initObject (w, obj, x, y, "OBJECT_" + w.objects ().size ());
		w.objects ().add (obj);
		return new WorldItem (WorldItem.OBJECT, w.objects ().size () - 1);
	}

	/** Animated object (no dynamics yet) with the first icon of the world, centred at (x, y). */
	static public WorldItem addAObject (World w, double x, double y)
	{
		WMAObject	obj = new WMAObject ();
		initObject (w, obj, x, y, uniqueLabel (w, "aobj"));
		w.aobjects ().add (obj);
		return new WorldItem (WorldItem.AOBJECT, w.aobjects ().size () - 1);
	}

	static private void initObject (World w, WMObject obj, double x, double y, String label)
	{
		obj.pos			= new Point3 (x, y, 0.0);
		obj.a			= 0.0;
		obj.shape		= null;
		obj.color		= ColorTool.getColorFromName ("gray_dark");
		obj.usecolor	= false;
		obj.visible		= true;
		obj.label		= label;
		obj.setIcon ((w.icons ().size () > 0) ? w.icons ().get (0) : defaultIcon (w));
	}

	/** Adds a start point (START_n) with the orientation of the last one. */
	static public WorldItem addStart (World w, double x, double y)
	{
		WMStart	last = w.start (w.n_starts () - 1);
		w.addStart (x, y, last.z (), last.orientation);
		return new WorldItem (WorldItem.START, w.n_starts () - 1);
	}

	static public WorldItem addWaypoint (World w, double x, double y)
	{
		w.wps ().add (new WMWaypoint (new Position (x, y, 0.0), uniqueLabel (w, "wp")));
		return new WorldItem (WorldItem.WAYPOINT, w.wps ().size () - 1);
	}

	static public WorldItem addDock (World w, double x, double y)
	{
		w.docks ().add (new WMDock (new Position (x, y, 0.0, 0.0), uniqueLabel (w, "dock")));
		return new WorldItem (WorldItem.DOCK, w.docks ().size () - 1);
	}

	static public WorldItem addBeacon (World w, double x, double y)
	{
		w.beacons ().add (new WMBeacon (uniqueLabel (w, "b"), new Position (x, y, 0.0), 0.2));
		return new WorldItem (WorldItem.BEACON, w.beacons ().size () - 1);
	}

	static public WorldItem addCBeacon (World w, double x, double y)
	{
		w.cbeacons ().add (new WMCBeacon (x, y, 0.0, WMCBeacon.DEF_DIAMETER, WMCBeacon.DEF_HEIGHT, uniqueLabel (w, "cb")));
		return new WorldItem (WorldItem.CBEACON, w.cbeacons ().size () - 1);
	}

	static public WorldItem addPathPoint (World w, double x, double y)
	{
		w.path ().add (new Point3 (x, y, 0.0));
		return new WorldItem (WorldItem.PATH, w.path ().size () - 1);
	}

	/** Offset (m) applied to a duplicated element so that it does not hide the original. */
	static public final double		DUP_OFFSET	= 0.25;

	/**
	 * Duplicates an element (deep copy): the copy is appended to its own
	 * group, displaced {@link #DUP_OFFSET} m in x and y, and given a label
	 * that no other element of the same group uses. Returns the item of the
	 * copy, or null when the element cannot be duplicated.
	 */
	static public WorldItem duplicate (World w, WorldItem it)
	{
		WorldItem	n = null;

		if (!valid (w, it))				return null;

		switch (it.kind)
		{
		case WorldItem.ZONE:
		{
			String	dt = w.zones ().defaultTexture ();
			w.zones ().add (new WMZone (w.zones ().at (it.index).toJson (dt), dt));
			n = new WorldItem (WorldItem.ZONE, w.zones ().n () - 1);
			break;
		}
		case WorldItem.FAREA:
		{
			String	dt = w.fareas ().defaultTexture ();
			w.fareas ().add (new WMFArea (w.fareas ().at (it.index).toJson (dt), dt));
			n = new WorldItem (WorldItem.FAREA, w.fareas ().n () - 1);
			break;
		}
		case WorldItem.WALL:
		{
			double	dw = w.walls ().defaultWidth (), dh = w.walls ().defaultHeight ();
			String	dt = w.walls ().defaultTexture ();
			w.walls ().add (new WMWall (w.walls ().at (it.index).toJson (dw, dh, dt), dw, dh, dt));
			n = new WorldItem (WorldItem.WALL, w.walls ().n () - 1);
			break;
		}
		case WorldItem.CONNECTOR:
		{
			double	dw = w.connectors ().defaultWidth (), dh = w.connectors ().defaultHeight ();
			String	dt = w.connectors ().defaultTexture ();
			w.connectors ().add (new WMConnector (w.connectors ().at (it.index).toJson (dw, dh, dt), dw, dh, dt));
			n = new WorldItem (WorldItem.CONNECTOR, w.connectors ().n () - 1);
			break;
		}
		case WorldItem.OBJECT:
			w.objects ().add (new WMObject (w.objects ().get (it.index).toJson (), w.icons ()));
			n = new WorldItem (WorldItem.OBJECT, w.objects ().size () - 1);
			break;
		case WorldItem.AOBJECT:
			w.aobjects ().add (new WMAObject (w.aobjects ().get (it.index).toJson (), w.icons ()));
			n = new WorldItem (WorldItem.AOBJECT, w.aobjects ().size () - 1);
			break;
		case WorldItem.BEACON:
			w.beacons ().add (new WMBeacon (w.beacons ().get (it.index).toJson ()));
			n = new WorldItem (WorldItem.BEACON, w.beacons ().size () - 1);
			break;
		case WorldItem.CBEACON:
			w.cbeacons ().add (new WMCBeacon (w.cbeacons ().get (it.index).toJson ()));
			n = new WorldItem (WorldItem.CBEACON, w.cbeacons ().size () - 1);
			break;
		case WorldItem.WAYPOINT:
			w.wps ().add (new WMWaypoint (w.wps ().get (it.index).toJson ()));
			n = new WorldItem (WorldItem.WAYPOINT, w.wps ().size () - 1);
			break;
		case WorldItem.DOCK:
			w.docks ().add (new WMDock (w.docks ().get (it.index).toJson ()));
			n = new WorldItem (WorldItem.DOCK, w.docks ().size () - 1);
			break;
		case WorldItem.ICON:
			w.icons ().add (new WMIcon (w.icons ().get (it.index).toJson ()));
			n = new WorldItem (WorldItem.ICON, w.icons ().size () - 1);
			break;
		case WorldItem.PATH:
		{
			Point2	p = w.path ().get (it.index);
			w.path ().add (new Point3 (p.x (), p.y (), World.z (p)));
			n = new WorldItem (WorldItem.PATH, w.path ().size () - 1);
			break;
		}
		case WorldItem.START:
		{
			WMStart	st = w.start (it.index);
			w.addStart (st.x (), st.y (), st.z (), st.orientation);
			n = new WorldItem (WorldItem.START, w.n_starts () - 1);
			break;
		}
		}
		if (n == null)					return null;		// settings and unknown kinds

		translate (w, n, DUP_OFFSET, DUP_OFFSET);
		renameCopy (w, n);
		return n;
	}

	/** Gives the copy a label of the form "base-n" not used by any element of its own group. */
	static private void renameCopy (World w, WorldItem it)
	{
		String		label = (it.kind == WorldItem.ICON) ? w.icons ().get (it.index).label : label (w, it);
		String		base;
		int			dash;

		if (label == null)				return;			// walls, objects, path points and starts are numbered, not named

		base		= label;
		dash		= base.lastIndexOf ('-');
		if ((dash > 0) && (dash < base.length () - 1) && isNumber (base.substring (dash + 1)))
			base	= base.substring (0, dash);			// "palet-1" and "palet" share the same base

		for (int i = 1; ; i++)
		{
			String	name = base + "-" + i;
			if (!usedLabel (w, it, name))		{ setLabel (w, it, name); return; }
		}
	}

	static private boolean isNumber (String s)
	{
		for (int i = 0; i < s.length (); i++)
			if (!Character.isDigit (s.charAt (i)))		return false;
		return true;
	}

	/** True when another element of the same group (or of the shared namespace) already uses the label. */
	static private boolean usedLabel (World w, WorldItem it, String name)
	{
		if (it.kind == WorldItem.ICON)		return w.icon (name) != null;
		if (indexOfLabel (w, it.kind, name) >= 0)		return true;
		// zones, connectors, waypoints and docks share a namespace with the other named elements
		return (w.getType (name) != World.NONE) || (indexOfLabel (w, WorldItem.BEACON, name) >= 0)
				|| (indexOfLabel (w, WorldItem.CBEACON, name) >= 0) || (indexOfLabel (w, WorldItem.FAREA, name) >= 0)
				|| (indexOfLabel (w, WorldItem.AOBJECT, name) >= 0);
	}

	static private void setLabel (World w, WorldItem it, String name)
	{
		switch (it.kind)
		{
		case WorldItem.ZONE:		w.zones ().at (it.index).label = name;		break;
		case WorldItem.FAREA:		w.fareas ().at (it.index).label = name;		break;
		case WorldItem.CONNECTOR:	w.connectors ().at (it.index).label = name;	break;
		case WorldItem.BEACON:		w.beacons ().get (it.index).label = name;	break;
		case WorldItem.CBEACON:		w.cbeacons ().get (it.index).label = name;	break;
		case WorldItem.WAYPOINT:	w.wps ().get (it.index).label = name;		break;
		case WorldItem.DOCK:		w.docks ().get (it.index).label = name;		break;
		case WorldItem.AOBJECT:		w.aobjects ().get (it.index).label = name;	break;
		case WorldItem.ICON:		w.icons ().get (it.index).label = name;		break;
		}
	}

	static public boolean remove (World w, WorldItem it)
	{
		if (!valid (w, it))				return false;

		switch (it.kind)
		{
		case WorldItem.ZONE:		w.zones ().remove (it.index);		return true;
		case WorldItem.FAREA:		w.fareas ().remove (it.index);		return true;
		case WorldItem.PATH:		w.path ().remove (it.index);		return true;
		case WorldItem.WALL:		w.walls ().remove (it.index);		return true;
		case WorldItem.OBJECT:		w.objects ().remove (it.index);		return true;
		case WorldItem.AOBJECT:		w.aobjects ().remove (it.index);	return true;
		case WorldItem.CONNECTOR:		w.connectors ().remove (it.index);		return true;
		case WorldItem.BEACON:		w.beacons ().remove (it.index);		return true;
		case WorldItem.CBEACON:		w.cbeacons ().remove (it.index);	return true;
		case WorldItem.WAYPOINT:	w.wps ().remove (it.index);			return true;
		case WorldItem.DOCK:		w.docks ().remove (it.index);		return true;
		case WorldItem.ICON:
			if (iconUsers (w, w.icons ().get (it.index).label) > 0)		return false;		// still referenced
			w.icons ().remove (it.index);
			return true;
		case WorldItem.START:		return w.removeStart (it.index);		// the last one stays
		}
		return false;		// the settings cannot be removed
	}

	/* ------------------------------------------------------------------ */
	/* Geometry: hit testing, translation, handles                         */
	/* ------------------------------------------------------------------ */

	static public double segDist (Line2 l, double x, double y)
	{
		return segDist (l.orig ().x (), l.orig ().y (), l.dest ().x (), l.dest ().y (), x, y);
	}

	static public double segDist (double x1, double y1, double x2, double y2, double x, double y)
	{
		double		dx = x2 - x1, dy = y2 - y1;
		double		l2 = dx * dx + dy * dy;
		double		t = 0.0;
		if (l2 > 1e-12)
			t = Math.max (0.0, Math.min (1.0, ((x - x1) * dx + (y - y1) * dy) / l2));
		double		px = x1 + t * dx, py = y1 + t * dy;
		return Math.hypot (x - px, y - py);
	}

	/**
	 * Distance from (x, y) to the element. For areas (zones, forbidden areas)
	 * it is zero when the point is inside.
	 */
	static public double distance (World w, WorldItem it, double x, double y)
	{
		if (!valid (w, it))				return Double.MAX_VALUE;

		switch (it.kind)
		{
		case WorldItem.ZONE:
		{
			WMZone	z = w.zones ().at (it.index);
			if (z.area.contains (x, y))		return 0.0;
			double	d = Double.MAX_VALUE;
			for (Line2 l : z.toLines ())	d = Math.min (d, segDist (l, x, y));
			return d;
		}
		case WorldItem.FAREA:
		{
			Polygon2	p = w.fareas ().at (it.index).polygon;
			if (p.contains (x, y))			return 0.0;
			double	d = Double.MAX_VALUE;
			for (int i = 0; i < p.npoints; i++)
			{
				int	j = (i + 1) % p.npoints;
				d = Math.min (d, segDist (p.xpoints[i], p.ypoints[i], p.xpoints[j], p.ypoints[j], x, y));
			}
			return d;
		}
		case WorldItem.PATH:		return w.path ().get (it.index).distance (x, y);
		case WorldItem.WALL:		return segDist (w.walls ().at (it.index).edge, x, y);
		case WorldItem.OBJECT:
		case WorldItem.AOBJECT:
		{
			WMObject	o = object (w, it);
			double	d = o.pos.distance (x, y);
			for (Line2 l : o.absIcon ())	d = Math.min (d, segDist (l, x, y));
			return d;
		}
		case WorldItem.CONNECTOR:
		{
			WMConnector	dr = w.connectors ().at (it.index);
			return Math.min (segDist (dr.edge, x, y), segDist (dr.path, x, y));
		}
		case WorldItem.BEACON:
		{
			WMBeacon	b = w.beacons ().get (it.index);
			return Math.min (b.pos.distance (x, y), segDist (b.getLine (), x, y));
		}
		case WorldItem.CBEACON:
		{
			WMCBeacon	b = w.cbeacons ().get (it.index);
			return Math.max (0.0, b.pos.distance (x, y) - b.radius ());
		}
		case WorldItem.WAYPOINT:	return w.wps ().get (it.index).pos.distance (x, y);
		case WorldItem.DOCK:		return w.docks ().get (it.index).pos.distance (x, y);
		case WorldItem.START:		return Math.hypot (w.start (it.index).x () - x, w.start (it.index).y () - y);
		}
		return Double.MAX_VALUE;
	}

	/** Kinds in picking priority (small things first, areas last). */
	static public final int[]	PICK_ORDER	= {
		WorldItem.START, WorldItem.WAYPOINT, WorldItem.DOCK, WorldItem.PATH, WorldItem.CBEACON, WorldItem.BEACON,
		WorldItem.CONNECTOR, WorldItem.WALL, WorldItem.AOBJECT, WorldItem.OBJECT, WorldItem.FAREA, WorldItem.ZONE
	};

	/**
	 * Nearest element to (x, y) within <code>tol</code> metres, honouring
	 * PICK_ORDER and the visibility mask (one boolean per kind).
	 */
	static public WorldItem pick (World w, double x, double y, double tol, boolean[] visible)
	{
		for (int k = 0; k < PICK_ORDER.length; k++)
		{
			int			kind = PICK_ORDER[k];
			if ((visible != null) && !visible[kind])		continue;

			int			n = count (w, kind);
			double		best = tol;
			int			bi = -1;
			for (int i = 0; i < n; i++)
			{
				double	d = distance (w, new WorldItem (kind, i), x, y);
				if (d < best) { best = d; bi = i; }
			}
			// areas: only accept "inside" or near-border hits
			if (bi >= 0)			return new WorldItem (kind, bi);
		}
		return null;
	}

	static public void translate (World w, WorldItem it, double dx, double dy)
	{
		if (!valid (w, it))				return;

		switch (it.kind)
		{
		case WorldItem.ICON:
		{
			WMIcon	ic = w.icons ().get (it.index);
			if (ic.hasPose ())		ic.pos = new Point3 (ic.pos.x () + dx, ic.pos.y () + dy, ic.pos.z ());
			break;
		}
		case WorldItem.ZONE:
		{
			WMZone	z = w.zones ().at (it.index);
			z.area.setRect (z.area.getX () + dx, z.area.getY () + dy, z.area.getWidth (), z.area.getHeight ());
			break;
		}
		case WorldItem.FAREA:		w.fareas ().at (it.index).polygon.translate (dx, dy);	break;
		case WorldItem.PATH:		w.path ().get (it.index).add (dx, dy);					break;
		case WorldItem.WALL:		moveLine (w.walls ().at (it.index).edge, dx, dy);	w.walls ().recomputeBounds ();	break;
		case WorldItem.OBJECT:
		case WorldItem.AOBJECT:
		{
			WMObject	o = object (w, it);
			o.pos = new Point3 (o.pos.x () + dx, o.pos.y () + dy, o.pos.z ());
			break;
		}
		case WorldItem.CONNECTOR:
		{
			WMConnector	d = w.connectors ().at (it.index);
			moveLine (d.edge, dx, dy);
			moveLine (d.path, dx, dy);
			break;
		}
		case WorldItem.BEACON:
		{
			Position	p = w.beacons ().get (it.index).pos;
			p.x (p.x () + dx);	p.y (p.y () + dy);
			break;
		}
		case WorldItem.CBEACON:
		{
			Point3		p = w.cbeacons ().get (it.index).pos;
			p.x (p.x () + dx);	p.y (p.y () + dy);
			break;
		}
		case WorldItem.WAYPOINT:
		{
			Position	p = w.wps ().get (it.index).pos;
			p.x (p.x () + dx);	p.y (p.y () + dy);
			break;
		}
		case WorldItem.DOCK:
		{
			Position	p = w.docks ().get (it.index).pos;
			p.x (p.x () + dx);	p.y (p.y () + dy);
			break;
		}
		case WorldItem.START:		{ WMStart st = w.start (it.index); st.set (st.x () + dx, st.y () + dy, st.z (), st.orientation); break; }
		}
	}

	static private void moveLine (Line2 l, double dx, double dy)
	{
		l.set (l.orig ().x () + dx, l.orig ().y () + dy, l.dest ().x () + dx, l.dest ().y () + dy);
	}

	/**
	 * Control points of the element. Dragging one of them with
	 * {@link #setHandle} reshapes the element; the last handle of oriented
	 * elements (waypoints, docks, start, objects) is the orientation arrow tip.
	 */
	static public Point2[] handles (World w, WorldItem it)
	{
		if (!valid (w, it))				return new Point2[0];

		switch (it.kind)
		{
		case WorldItem.ZONE:
		{
			java.awt.geom.Rectangle2D	r = w.zones ().at (it.index).area;
			return new Point2[] { new Point2 (r.getMinX (), r.getMinY ()), new Point2 (r.getMaxX (), r.getMinY ()),
								  new Point2 (r.getMaxX (), r.getMaxY ()), new Point2 (r.getMinX (), r.getMaxY ()) };
		}
		case WorldItem.FAREA:
		{
			Polygon2	p = w.fareas ().at (it.index).polygon;
			Point2[]	h = new Point2[p.npoints];
			for (int i = 0; i < p.npoints; i++)		h[i] = new Point2 (p.xpoints[i], p.ypoints[i]);
			return h;
		}
		case WorldItem.PATH:		return new Point2[] { new Point2 (w.path ().get (it.index)) };
		case WorldItem.WALL:
		{
			Line2	l = w.walls ().at (it.index).edge;
			return new Point2[] { new Point2 (l.orig ()), new Point2 (l.dest ()) };
		}
		case WorldItem.OBJECT:
		{
			WMObject	o = object (w, it);
			return new Point2[] { new Point2 (o.pos.x (), o.pos.y ()), arrow (o.pos.x (), o.pos.y (), o.a) };
		}
		case WorldItem.AOBJECT:
		{
			// the heading handle sits on the virtual radius: dragging it rotates the object and resizes the radius
			WMAObject	o = w.aobjects ().get (it.index);
			double		r = Math.max (o.radius, MIN_RADIUS);
			return new Point2[] { new Point2 (o.pos.x (), o.pos.y ()), new Point2 (o.pos.x () + r * Math.cos (o.a), o.pos.y () + r * Math.sin (o.a)) };
		}
		case WorldItem.ICON:
		{
			WMIcon	ic = w.icons ().get (it.index);
			if (!ic.hasPose ())		return new Point2[0];
			return new Point2[] { new Point2 (ic.pos.x (), ic.pos.y ()), arrow (ic.pos.x (), ic.pos.y (), ic.a) };
		}
		case WorldItem.CONNECTOR:
		{
			WMConnector	d = w.connectors ().at (it.index);
			return new Point2[] { new Point2 (d.edge.orig ()), new Point2 (d.edge.dest ()), new Point2 (d.path.orig ()), new Point2 (d.path.dest ()) };
		}
		case WorldItem.BEACON:
		{
			WMBeacon	b = w.beacons ().get (it.index);
			return new Point2[] { new Point2 (b.pos.x (), b.pos.y ()), new Point2 (b.getLine ().dest ()) };
		}
		case WorldItem.CBEACON:
		{
			WMCBeacon	b = w.cbeacons ().get (it.index);
			return new Point2[] { new Point2 (b.pos), new Point2 (b.pos.x () + b.radius (), b.pos.y ()) };
		}
		case WorldItem.WAYPOINT:
		{
			Position	p = w.wps ().get (it.index).pos;
			return new Point2[] { new Point2 (p.x (), p.y ()), arrow (p.x (), p.y (), p.alpha ()) };
		}
		case WorldItem.DOCK:
		{
			Position	p = w.docks ().get (it.index).pos;
			return new Point2[] { new Point2 (p.x (), p.y ()), arrow (p.x (), p.y (), p.alpha ()) };
		}
		case WorldItem.START:		{ WMStart st = w.start (it.index); return new Point2[] { new Point2 (st.x (), st.y ()), arrow (st.x (), st.y (), st.orientation) }; }
		}
		return new Point2[0];
	}

	static private Point2 arrow (double x, double y, double a)
	{
		return new Point2 (x + ARROW * Math.cos (a), y + ARROW * Math.sin (a));
	}

	/** Moves handle <code>h</code> of the element to (x, y). */
	static public void setHandle (World w, WorldItem it, int h, double x, double y)
	{
		if (!valid (w, it))				return;

		switch (it.kind)
		{
		case WorldItem.ZONE:
		{
			java.awt.geom.Rectangle2D.Double	r = w.zones ().at (it.index).area;
			// opposite corner stays fixed
			double	ox = ((h == 0) || (h == 3)) ? r.getMaxX () : r.getMinX ();
			double	oy = ((h == 0) || (h == 1)) ? r.getMaxY () : r.getMinY ();
			r.setRect (Math.min (ox, x), Math.min (oy, y), Math.abs (ox - x), Math.abs (oy - y));
			break;
		}
		case WorldItem.FAREA:
		{
			Polygon2	p = w.fareas ().at (it.index).polygon;
			if ((h >= 0) && (h < p.npoints)) { p.xpoints[h] = x; p.ypoints[h] = y; }
			break;
		}
		case WorldItem.PATH:		w.path ().get (it.index).set (x, y);		break;
		case WorldItem.WALL:
		{
			Line2	l = w.walls ().at (it.index).edge;
			if (h == 0)		l.set (x, y, l.dest ().x (), l.dest ().y ());
			else			l.set (l.orig ().x (), l.orig ().y (), x, y);
			w.walls ().recomputeBounds ();
			break;
		}
		case WorldItem.OBJECT:
		{
			WMObject	o = object (w, it);
			if (h == 0)		translate (w, it, x - o.pos.x (), y - o.pos.y ());
			else			setObjectPose (o, o.pos.x (), o.pos.y (), o.pos.z (), Math.atan2 (y - o.pos.y (), x - o.pos.x ()));
			break;
		}
		case WorldItem.AOBJECT:
		{
			WMAObject	o = w.aobjects ().get (it.index);
			if (h == 0)		translate (w, it, x - o.pos.x (), y - o.pos.y ());
			else
			{
				setObjectPose (o, o.pos.x (), o.pos.y (), o.pos.z (), Math.atan2 (y - o.pos.y (), x - o.pos.x ()));
				o.radius = Math.max (MIN_RADIUS, Math.hypot (x - o.pos.x (), y - o.pos.y ()));
			}
			break;
		}
		case WorldItem.ICON:
		{
			WMIcon	ic = w.icons ().get (it.index);
			if (!ic.hasPose ())		break;
			if (h == 0)		translate (w, it, x - ic.pos.x (), y - ic.pos.y ());
			else			ic.a = Math.atan2 (y - ic.pos.y (), x - ic.pos.x ());
			break;
		}
		case WorldItem.CONNECTOR:
		{
			WMConnector	d = w.connectors ().at (it.index);
			Line2	l = (h < 2) ? d.edge : d.path;
			if ((h % 2) == 0)	l.set (x, y, l.dest ().x (), l.dest ().y ());
			else				l.set (l.orig ().x (), l.orig ().y (), x, y);
			break;
		}
		case WorldItem.BEACON:
		{
			WMBeacon	b = w.beacons ().get (it.index);
			if (h == 0)		{ b.pos.x (x);	b.pos.y (y); }
			else
			{
				b.pos.alpha (Math.atan2 (y - b.pos.y (), x - b.pos.x ()));
				b.width = 2.0 * b.pos.distance (x, y);
			}
			break;
		}
		case WorldItem.CBEACON:
		{
			WMCBeacon	b = w.cbeacons ().get (it.index);
			if (h == 0)		{ b.pos.x (x); b.pos.y (y); }
			else			b.diameter = 2.0 * Math.max (0.005, b.pos.distance (x, y));
			break;
		}
		case WorldItem.WAYPOINT:
		case WorldItem.DOCK:
		{
			Position	p = (it.kind == WorldItem.WAYPOINT) ? w.wps ().get (it.index).pos : w.docks ().get (it.index).pos;
			if (h == 0)		{ p.x (x);	p.y (y); }
			else			p.alpha (Math.atan2 (y - p.y (), x - p.x ()));
			break;
		}
		case WorldItem.START:
		{
			WMStart	st = w.start (it.index);
			if (h == 0)		st.set (x, y, st.z (), st.orientation);
			else			st.orientation = Math.atan2 (y - st.y (), x - st.x ());
			break;
		}
		}
	}

	/** Re-places an object keeping its local icon shape. */
	static public void setObjectPose (WMObject o, double x, double y, double z, double a)
	{
		o.pos	= new Point3 (x, y, z);
		o.a		= a;
		o.invalidate ();
	}

	/** Bounding box of everything in the world: {minx, miny, maxx, maxy}, or null if empty. */
	static public double[] bounds (World w)
	{
		double[]	b = { Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };
		boolean		any = false;

		for (int kind = 0; kind < WorldItem.DEFAULTS; kind++)
		{
			int		n = count (w, kind);
			for (int i = 0; i < n; i++)
			{
				WorldItem	it = new WorldItem (kind, i);
				Point2[]	hs = handles (w, it);
				if (WorldItem.isObject (kind))
				{
					hs = new Point2[0];
					for (Line2 l : object (w, it).absIcon ())
						hs = concat (hs, new Point2[] { l.orig (), l.dest () });
				}
				for (Point2 p : hs)
				{
					b[0] = Math.min (b[0], p.x ());	b[1] = Math.min (b[1], p.y ());
					b[2] = Math.max (b[2], p.x ());	b[3] = Math.max (b[3], p.y ());
					any = true;
				}
			}
		}
		return any ? b : null;
	}

	static private Point2[] concat (Point2[] a, Point2[] b)
	{
		Point2[]	r = new Point2[a.length + b.length];
		System.arraycopy (a, 0, r, 0, a.length);
		System.arraycopy (b, 0, r, a.length, b.length);
		return r;
	}

	/** Base elevation of an element (m), for 3D markers. */
	static public double elevation (World w, WorldItem it)
	{
		if (!valid (w, it))				return 0.0;
		switch (it.kind)
		{
		case WorldItem.ZONE:		return w.zones ().at (it.index).z;
		case WorldItem.FAREA:
		{
			Polygon2	p = w.fareas ().at (it.index).polygon;
			double	z = 0.0;
			for (int i = 0; i < p.npoints; i++)		z += p.zpoints[i];
			return (p.npoints > 0) ? z / p.npoints : 0.0;
		}
		case WorldItem.PATH:		return World.z (w.path ().get (it.index));
		case WorldItem.WALL:		return Math.min (w.walls ().at (it.index).edge.z1 (), w.walls ().at (it.index).edge.z2 ());
		case WorldItem.OBJECT:
		case WorldItem.AOBJECT:		return object (w, it).pos.z ();
		case WorldItem.CONNECTOR:		return Math.min (w.connectors ().at (it.index).edge.z1 (), w.connectors ().at (it.index).edge.z2 ());
		case WorldItem.BEACON:		return w.beacons ().get (it.index).pos.z ();
		case WorldItem.CBEACON:		return w.cbeacons ().get (it.index).pos.z ();
		case WorldItem.WAYPOINT:	return w.wps ().get (it.index).pos.z ();
		case WorldItem.DOCK:		return w.docks ().get (it.index).pos.z ();
		case WorldItem.START:		return w.start (it.index).z ();
		}
		return 0.0;
	}

	/* ------------------------------------------------------------------ */
	/* Property view (name / value strings)                                */
	/* ------------------------------------------------------------------ */

	static public String[] propertyNames (World w, WorldItem it)
	{
		if (!valid (w, it))				return new String[0];

		switch (it.kind)
		{
		case WorldItem.ZONE:		return new String[] { "label", "x", "y", "z", "width", "height", "texture" };
		case WorldItem.FAREA:		return new String[] { "label", "texture", "points" };
		case WorldItem.PATH:		return new String[] { "x", "y", "z" };
		case WorldItem.WALL:		return new String[] { "x1", "y1", "z1", "x2", "y2", "z2", "width", "height", "texture" };
		case WorldItem.OBJECT:		return new String[] { "x", "y", "z", "orientation", "icon", "image", "shape", "color", "usecolor" };
		case WorldItem.AOBJECT:		return new String[] { "label", "x", "y", "z", "orientation", "radius", "icon", "image", "shape", "color", "usecolor", "dynamics",
														  "movement", "speed", "acceleration", "mass", "coef_res", "coef_fric" };
		case WorldItem.ICON:		return new String[] { "label", "x", "y", "z", "orientation", "segments" };
		case WorldItem.CONNECTOR:		return new String[] { "label", "x1", "y1", "z1", "x2", "y2", "z2", "path x1", "path y1", "path z1", "path x2", "path y2", "path z2", "width", "height", "texture" };
		case WorldItem.BEACON:		return new String[] { "label", "x", "y", "z", "orientation", "width", "height" };
		case WorldItem.CBEACON:		return new String[] { "label", "x", "y", "z", "diameter", "height" };
		case WorldItem.WAYPOINT:	return new String[] { "label", "x", "y", "z", "orientation" };
		case WorldItem.DOCK:		return new String[] { "label", "x", "y", "z", "orientation", "flow" };
		case WorldItem.START:		return new String[] { "x", "y", "z", "orientation" };
		case WorldItem.GEOMETRY:	return new String[] { "wall width", "wall height", "wall texture", "connector width", "connector height", "connector texture", "zone texture", "farea texture" };
		case WorldItem.BEHAVIOUR:	return new String[] { "robot knowledge" };
		}
		return new String[0];
	}

	static public String getProperty (World w, WorldItem it, String name)
	{
		if (!valid (w, it))				return "";

		switch (it.kind)
		{
		case WorldItem.ZONE:
		{
			WMZone	z = w.zones ().at (it.index);
			if (name.equals ("label"))		return z.label;
			if (name.equals ("x"))			return fmt (z.area.getX ());
			if (name.equals ("y"))			return fmt (z.area.getY ());
			if (name.equals ("z"))			return fmt (z.z);
			if (name.equals ("width"))		return fmt (z.area.getWidth ());
			if (name.equals ("height"))		return fmt (z.area.getHeight ());
			if (name.equals ("texture"))	return z.texture;
			break;
		}
		case WorldItem.FAREA:
		{
			WMFArea	f = w.fareas ().at (it.index);
			if (name.equals ("label"))		return f.label;
			if (name.equals ("texture"))	return f.texture;
			if (name.equals ("points"))
			{
				StringBuffer	sb = new StringBuffer ();
				for (int i = 0; i < f.polygon.npoints; i++)
					sb.append ((i > 0) ? "; " : "").append (fmt (f.polygon.xpoints[i])).append (", ").append (fmt (f.polygon.ypoints[i])).append (", ").append (fmt (f.polygon.zpoints[i]));
				return sb.toString ();
			}
			break;
		}
		case WorldItem.PATH:
		{
			Point2	p = w.path ().get (it.index);
			if (name.equals ("x"))			return fmt (p.x ());
			if (name.equals ("y"))			return fmt (p.y ());
			if (name.equals ("z"))			return fmt (World.z (p));
			break;
		}
		case WorldItem.WALL:
		{
			WMWall	wl = w.walls ().at (it.index);
			if (name.equals ("x1"))			return fmt (wl.edge.orig ().x ());
			if (name.equals ("y1"))			return fmt (wl.edge.orig ().y ());
			if (name.equals ("z1"))			return fmt (wl.edge.z1 ());
			if (name.equals ("x2"))			return fmt (wl.edge.dest ().x ());
			if (name.equals ("y2"))			return fmt (wl.edge.dest ().y ());
			if (name.equals ("z2"))			return fmt (wl.edge.z2 ());
			if (name.equals ("width"))		return fmt (wl.width);
			if (name.equals ("height"))		return fmt (wl.height);
			if (name.equals ("texture"))	return wl.texture;
			break;
		}
		case WorldItem.OBJECT:
		case WorldItem.AOBJECT:
		{
			WMObject	o = object (w, it);
			if (name.equals ("x"))			return fmt (o.pos.x ());
			if (name.equals ("y"))			return fmt (o.pos.y ());
			if (name.equals ("z"))			return fmt (o.pos.z ());
			if (name.equals ("orientation"))		return fmt (Math.toDegrees (o.a));
			if (name.equals ("shape"))		return (o.shape == null) ? "" : o.shape;
			if (name.equals ("color"))		return toHex (o.color);
			if (name.equals ("usecolor"))	return Boolean.toString (o.usecolor);
			if (name.equals ("icon"))		return (o.iconId == null) ? "" : o.iconId;
			if (name.equals ("image"))		return (o.image == null) ? "" : o.image;
			if (o instanceof WMAObject)
			{
				WMAObject	ao = (WMAObject) o;
				if (name.equals ("label"))			return ao.label;
				if (name.equals ("radius"))			return fmt (ao.radius);
				if (name.equals ("dynamics"))		return (ao.dynamics != null) ? ao.dynamics : "";
				if (name.equals ("movement"))		return ao.movement.name ();
				if (name.equals ("speed"))			return fmt (ao.speed);
				if (name.equals ("acceleration"))	return fmt (ao.acceleration);
				if (name.equals ("mass"))			return fmt (ao.mass);
				if (name.equals ("coef_res"))		return fmt (ao.coef_res);
				if (name.equals ("coef_fric"))		return fmt (ao.coef_fric);
			}
			break;
		}
		case WorldItem.ICON:
		{
			WMIcon	ic = w.icons ().get (it.index);
			if (name.equals ("label"))		return ic.label;
			if (name.equals ("x"))			return ic.hasPose () ? fmt (ic.pos.x ()) : "";
			if (name.equals ("y"))			return ic.hasPose () ? fmt (ic.pos.y ()) : "";
			if (name.equals ("z"))			return ic.hasPose () ? fmt (ic.pos.z ()) : "";
			if (name.equals ("orientation"))	return ic.hasPose () ? fmt (Math.toDegrees (ic.a)) : "";
			if (name.equals ("segments"))	return segmentsText (ic.lines);
			break;
		}
		case WorldItem.CONNECTOR:
		{
			WMConnector	d = w.connectors ().at (it.index);
			if (name.equals ("label"))		return d.label;
			if (name.equals ("x1"))			return fmt (d.edge.orig ().x ());
			if (name.equals ("y1"))			return fmt (d.edge.orig ().y ());
			if (name.equals ("z1"))			return fmt (d.edge.z1 ());
			if (name.equals ("x2"))			return fmt (d.edge.dest ().x ());
			if (name.equals ("y2"))			return fmt (d.edge.dest ().y ());
			if (name.equals ("z2"))			return fmt (d.edge.z2 ());
			if (name.equals ("path x1"))	return fmt (d.path.orig ().x ());
			if (name.equals ("path y1"))	return fmt (d.path.orig ().y ());
			if (name.equals ("path z1"))	return fmt (d.path.z1 ());
			if (name.equals ("path x2"))	return fmt (d.path.dest ().x ());
			if (name.equals ("path y2"))	return fmt (d.path.dest ().y ());
			if (name.equals ("path z2"))	return fmt (d.path.z2 ());
			if (name.equals ("width"))		return fmt (d.width);
			if (name.equals ("height"))		return fmt (d.height);
			if (name.equals ("texture"))	return d.texture;
			break;
		}
		case WorldItem.BEACON:
		{
			WMBeacon	b = w.beacons ().get (it.index);
			if (name.equals ("label"))		return b.label;
			if (name.equals ("x"))			return fmt (b.pos.x ());
			if (name.equals ("y"))			return fmt (b.pos.y ());
			if (name.equals ("z"))			return fmt (b.pos.z ());
			if (name.equals ("orientation"))		return fmt (Math.toDegrees (b.pos.alpha ()));
			if (name.equals ("width"))		return fmt (b.width);
			if (name.equals ("height"))		return fmt (b.height);
			break;
		}
		case WorldItem.CBEACON:
		{
			WMCBeacon	b = w.cbeacons ().get (it.index);
			if (name.equals ("label"))		return b.label;
			if (name.equals ("x"))			return fmt (b.pos.x ());
			if (name.equals ("y"))			return fmt (b.pos.y ());
			if (name.equals ("z"))			return fmt (b.pos.z ());
			if (name.equals ("diameter"))	return fmt (b.diameter);
			if (name.equals ("height"))		return fmt (b.height);
			break;
		}
		case WorldItem.WAYPOINT:
		{
			WMWaypoint	p = w.wps ().get (it.index);
			if (name.equals ("label"))		return p.label;
			if (name.equals ("x"))			return fmt (p.pos.x ());
			if (name.equals ("y"))			return fmt (p.pos.y ());
			if (name.equals ("z"))			return fmt (p.pos.z ());
			if (name.equals ("orientation"))		return fmt (Math.toDegrees (p.pos.alpha ()));
			break;
		}
		case WorldItem.DOCK:
		{
			WMDock	d = w.docks ().get (it.index);
			if (name.equals ("label"))		return d.label;
			if (name.equals ("x"))			return fmt (d.pos.x ());
			if (name.equals ("y"))			return fmt (d.pos.y ());
			if (name.equals ("z"))			return fmt (d.pos.z ());
			if (name.equals ("orientation"))		return fmt (Math.toDegrees (d.pos.alpha ()));
			if (name.equals ("flow"))		return d.flow.name ();
			break;
		}
		case WorldItem.START:
		{
			WMStart	st = w.start (it.index);
			if (name.equals ("x"))			return fmt (st.x ());
			if (name.equals ("y"))			return fmt (st.y ());
			if (name.equals ("z"))			return fmt (st.z ());
			if (name.equals ("orientation"))		return fmt (Math.toDegrees (st.orientation));
			break;
		}
		case WorldItem.BEHAVIOUR:
			if (name.equals ("robot knowledge"))	return Boolean.toString (w.apw);
			break;
		case WorldItem.GEOMETRY:
			if (name.equals ("wall width"))		return fmt (w.walls ().defaultWidth ());
			if (name.equals ("wall height"))	return fmt (w.walls ().defaultHeight ());
			if (name.equals ("wall texture"))	return w.walls ().defaultTexture ();
			if (name.equals ("connector width"))		return fmt (w.connectors ().defaultWidth ());
			if (name.equals ("connector height"))	return fmt (w.connectors ().defaultHeight ());
			if (name.equals ("connector texture"))	return w.connectors ().defaultTexture ();
			if (name.equals ("zone texture"))	return w.zones ().defaultTexture ();
			if (name.equals ("farea texture"))	return w.fareas ().defaultTexture ();
			break;
		}
		return "";
	}

	/**
	 * Applies a property value typed by the user.
	 * @throws IllegalArgumentException with a user-readable message if the value is not acceptable.
	 */
	static public void setProperty (World w, WorldItem it, String name, String value)
	{
		if (!valid (w, it))				return;
		value = value.trim ();

		switch (it.kind)
		{
		case WorldItem.ZONE:
		{
			WMZone	z = w.zones ().at (it.index);
			if (name.equals ("label"))			z.label = checkLabel (w, it, value);
			else if (name.equals ("x"))			z.area.setRect (num (value), z.area.getY (), z.area.getWidth (), z.area.getHeight ());
			else if (name.equals ("y"))			z.area.setRect (z.area.getX (), num (value), z.area.getWidth (), z.area.getHeight ());
			else if (name.equals ("z"))			z.z = num (value);
			else if (name.equals ("width"))		z.area.setRect (z.area.getX (), z.area.getY (), Math.abs (num (value)), z.area.getHeight ());
			else if (name.equals ("height"))	z.area.setRect (z.area.getX (), z.area.getY (), z.area.getWidth (), Math.abs (num (value)));
			else if (name.equals ("texture"))	z.texture = token (value);
			return;
		}
		case WorldItem.FAREA:
		{
			WMFArea	f = w.fareas ().at (it.index);
			if (name.equals ("label"))			f.label = checkLabel (w, it, value);
			else if (name.equals ("texture"))	f.texture = token (value);
			else if (name.equals ("points"))
			{
				double[]	v = nums (value);
				if ((v.length < 9) || (v.length % 3 != 0))
					throw new IllegalArgumentException ("At least 3 points, as: x1, y1, z1; x2, y2, z2; x3, y3, z3");
				Polygon2	p = new Polygon2 ();
				for (int i = 0; i < v.length; i += 3)		p.addPoint (v[i], v[i + 1], v[i + 2]);
				f.polygon = p;
			}
			return;
		}
		case WorldItem.PATH:
		{
			Point2	p = w.path ().get (it.index);
			if (name.equals ("x"))				p.x (num (value));
			else if (name.equals ("y"))			p.y (num (value));
			else if (name.equals ("z"))			{ if (p instanceof Point3) ((Point3) p).z (num (value)); }
			return;
		}
		case WorldItem.WALL:
		{
			WMWall	wl = w.walls ().at (it.index);
			Line2	l = wl.edge;
			if (name.equals ("x1"))				l.set (num (value), l.orig ().y (), l.dest ().x (), l.dest ().y ());
			else if (name.equals ("y1"))		l.set (l.orig ().x (), num (value), l.dest ().x (), l.dest ().y ());
			else if (name.equals ("x2"))		l.set (l.orig ().x (), l.orig ().y (), num (value), l.dest ().y ());
			else if (name.equals ("y2"))		l.set (l.orig ().x (), l.orig ().y (), l.dest ().x (), num (value));
			else if (name.equals ("z1"))		l.setZ (num (value), l.z2 ());
			else if (name.equals ("z2"))		l.setZ (l.z1 (), num (value));
			else if (name.equals ("width"))		wl.width = num (value);
			else if (name.equals ("height"))	wl.height = num (value);
			else if (name.equals ("texture"))	wl.texture = token (value);
			w.walls ().recomputeBounds ();
			return;
		}
		case WorldItem.OBJECT:
		case WorldItem.AOBJECT:
		{
			WMObject	o = object (w, it);
			if (name.equals ("x"))				setObjectPose (o, num (value), o.pos.y (), o.pos.z (), o.a);
			else if (name.equals ("y"))			setObjectPose (o, o.pos.x (), num (value), o.pos.z (), o.a);
			else if (name.equals ("z"))			setObjectPose (o, o.pos.x (), o.pos.y (), num (value), o.a);
			else if (name.equals ("orientation"))		setObjectPose (o, o.pos.x (), o.pos.y (), o.pos.z (), Math.toRadians (num (value)));
			else if (name.equals ("shape"))		o.shape = (value.length () == 0) ? null : token (value);
			else if (name.equals ("image"))
			{
				o.image	= (value.length () == 0) ? null : token (value);
				wucore.utils.image.PlanImage.flush (o.image);		// the view reads the new file
			}
			else if (name.equals ("color"))		o.color = parseColor (value);
			else if (name.equals ("usecolor"))	o.usecolor = bool (value);
			else if (name.equals ("icon"))
			{
				WMIcon	ic = w.icon (token (value));
				if (ic == null)		throw new IllegalArgumentException ("Unknown icon '" + value + "'");
				o.setIcon (ic);
			}
			else if (o instanceof WMAObject)
			{
				WMAObject	ao = (WMAObject) o;
				if (name.equals ("label"))				ao.label = checkLabel (w, it, value);
				else if (name.equals ("radius"))		ao.radius = Math.max (0.0, num (value));
				else if (name.equals ("dynamics"))		ao.dynamics = (value.trim ().length () == 0) ? null : token (value);
				else if (name.equals ("movement"))		ao.movement = WMAObject.parseMovement (value);
				else if (name.equals ("speed"))			ao.speed = num (value);
				else if (name.equals ("acceleration"))	ao.acceleration = num (value);
				else if (name.equals ("mass"))			ao.mass = num (value);
				else if (name.equals ("coef_res"))		ao.coef_res = num (value);
				else if (name.equals ("coef_fric"))		ao.coef_fric = num (value);
			}
			return;
		}
		case WorldItem.ICON:
		{
			WMIcon	ic = w.icons ().get (it.index);
			if (name.equals ("label"))
			{
				String	old = ic.label;
				token (value);
				if (!value.equals (old) && (World.index (w.icons (), value) >= 0))
					throw new IllegalArgumentException ("Icon '" + value + "' already exists");
				ic.label = value;
				for (WMObject o : w.allObjects ())			// keep the references
					if (old.equals (o.iconId))		o.setIcon (ic);
			}
			else if (name.equals ("x"))			{ if (!ic.hasPose ()) ic.setPose (0, 0, 0, 0);	ic.pos = new Point3 (num (value), ic.pos.y (), ic.pos.z ()); }
			else if (name.equals ("y"))			{ if (!ic.hasPose ()) ic.setPose (0, 0, 0, 0);	ic.pos = new Point3 (ic.pos.x (), num (value), ic.pos.z ()); }
			else if (name.equals ("z"))			{ if (!ic.hasPose ()) ic.setPose (0, 0, 0, 0);	ic.pos = new Point3 (ic.pos.x (), ic.pos.y (), num (value)); }
			else if (name.equals ("orientation"))	{ if (!ic.hasPose ()) ic.setPose (0, 0, 0, 0);	ic.a = Math.toRadians (num (value)); }
			else if (name.equals ("segments"))
				ic.lines = parseSegments (value);
			return;
		}
		case WorldItem.CONNECTOR:
		{
			WMConnector	d = w.connectors ().at (it.index);
			Line2	e = d.edge, p = d.path;
			if (name.equals ("label"))			d.label = checkLabel (w, it, value);
			else if (name.equals ("x1"))		e.set (num (value), e.orig ().y (), e.dest ().x (), e.dest ().y ());
			else if (name.equals ("y1"))		e.set (e.orig ().x (), num (value), e.dest ().x (), e.dest ().y ());
			else if (name.equals ("x2"))		e.set (e.orig ().x (), e.orig ().y (), num (value), e.dest ().y ());
			else if (name.equals ("y2"))		e.set (e.orig ().x (), e.orig ().y (), e.dest ().x (), num (value));
			else if (name.equals ("path x1"))	p.set (num (value), p.orig ().y (), p.dest ().x (), p.dest ().y ());
			else if (name.equals ("path y1"))	p.set (p.orig ().x (), num (value), p.dest ().x (), p.dest ().y ());
			else if (name.equals ("path x2"))	p.set (p.orig ().x (), p.orig ().y (), num (value), p.dest ().y ());
			else if (name.equals ("path y2"))	p.set (p.orig ().x (), p.orig ().y (), p.dest ().x (), num (value));
			else if (name.equals ("z1"))		e.setZ (num (value), e.z2 ());
			else if (name.equals ("z2"))		e.setZ (e.z1 (), num (value));
			else if (name.equals ("path z1"))	p.setZ (num (value), p.z2 ());
			else if (name.equals ("path z2"))	p.setZ (p.z1 (), num (value));
			else if (name.equals ("width"))		d.width = num (value);
			else if (name.equals ("height"))	d.height = num (value);
			else if (name.equals ("texture"))	d.texture = token (value);
			return;
		}
		case WorldItem.BEACON:
		{
			WMBeacon	b = w.beacons ().get (it.index);
			if (name.equals ("label"))			b.label = checkLabel (w, it, value);
			else if (name.equals ("x"))			b.pos.x (num (value));
			else if (name.equals ("y"))			b.pos.y (num (value));
			else if (name.equals ("z"))			b.pos.z (num (value));
			else if (name.equals ("orientation"))		b.pos.alpha (Math.toRadians (num (value)));
			else if (name.equals ("width"))		b.width = Math.abs (num (value));
			else if (name.equals ("height"))	b.height = Math.abs (num (value));
			return;
		}
		case WorldItem.CBEACON:
		{
			WMCBeacon	b = w.cbeacons ().get (it.index);
			if (name.equals ("label"))			b.label = checkLabel (w, it, value);
			else if (name.equals ("x"))			b.pos.x (num (value));
			else if (name.equals ("y"))			b.pos.y (num (value));
			else if (name.equals ("z"))			b.pos.z (num (value));
			else if (name.equals ("diameter"))	b.diameter = Math.abs (num (value));
			else if (name.equals ("height"))	b.height = Math.abs (num (value));
			return;
		}
		case WorldItem.WAYPOINT:
		{
			WMWaypoint	p = w.wps ().get (it.index);
			if (name.equals ("label"))			p.label = checkLabel (w, it, value);
			else if (name.equals ("x"))			p.pos.x (num (value));
			else if (name.equals ("y"))			p.pos.y (num (value));
			else if (name.equals ("z"))			p.pos.z (num (value));
			else if (name.equals ("orientation"))		p.pos.alpha (Math.toRadians (num (value)));
			return;
		}
		case WorldItem.DOCK:
		{
			WMDock	d = w.docks ().get (it.index);
			if (name.equals ("label"))			d.label = checkLabel (w, it, value);
			else if (name.equals ("x"))			d.pos.x (num (value));
			else if (name.equals ("y"))			d.pos.y (num (value));
			else if (name.equals ("z"))			d.pos.z (num (value));
			else if (name.equals ("orientation"))		d.pos.alpha (Math.toRadians (num (value)));
			else if (name.equals ("flow"))		d.flow = WMDock.parseFlow (value);
			return;
		}
		case WorldItem.START:
		{
			WMStart	st = w.start (it.index);
			if (name.equals ("x"))				st.set (num (value), st.y (), st.z (), st.orientation);
			else if (name.equals ("y"))			st.set (st.x (), num (value), st.z (), st.orientation);
			else if (name.equals ("z"))			st.set (st.x (), st.y (), num (value), st.orientation);
			else if (name.equals ("orientation"))		st.orientation = Math.toRadians (num (value));
			return;
		}
		case WorldItem.BEHAVIOUR:
			if (name.equals ("robot knowledge"))	w.apw = bool (value);
			return;
		case WorldItem.GEOMETRY:
			if (name.equals ("wall width"))			w.walls ().setDefaults (num (value), w.walls ().defaultHeight (), w.walls ().defaultTexture ());
			else if (name.equals ("wall height"))	w.walls ().setDefaults (w.walls ().defaultWidth (), num (value), w.walls ().defaultTexture ());
			else if (name.equals ("wall texture"))	w.walls ().setDefaults (w.walls ().defaultWidth (), w.walls ().defaultHeight (), token (value));
			else if (name.equals ("connector width"))	w.connectors ().setDefaults (num (value), w.connectors ().defaultHeight (), w.connectors ().defaultTexture ());
			else if (name.equals ("connector height"))	w.connectors ().setDefaults (w.connectors ().defaultWidth (), num (value), w.connectors ().defaultTexture ());
			else if (name.equals ("connector texture"))	w.connectors ().setDefaults (w.connectors ().defaultWidth (), w.connectors ().defaultHeight (), token (value));
			else if (name.equals ("zone texture"))	w.zones ().setDefaultTexture (token (value));
			else if (name.equals ("farea texture"))	w.fareas ().setDefaultTexture (token (value));
			return;
		}
	}

	/* ------------------------------------------------------------------ */
	/* Icon editing                                                        */
	/*                                                                     */
	/* Icons are stored in local coordinates. The GUI edits them through a  */
	/* reference pose (rx, ry, ra): the world position where the icon origin */
	/* is displayed (an object's pose, or the anchor chosen for a new icon). */
	/* ------------------------------------------------------------------ */

	/** Endpoints closer than this are considered the same vertex (m). */
	static public final double		ICON_EPS	= 1e-4;

	static public boolean isBooleanProperty (String name)
	{
		return name.equals ("usecolor") || name.equals ("robot knowledge");
	}

	/** Names of the movement types of an animated object, the choices of the "movement" property. */
	static public String[] movementNames ()
	{
		WMAObject.Movement[]	types = WMAObject.Movement.values ();
		String[]				names = new String[types.length];
		for (int i = 0; i < types.length; i++)		names[i] = types[i].name ();
		return names;
	}

	/**
	 * False for the properties that do not apply in the current state of the
	 * element: the motion parameters (speed, acceleration, mass, coef_res,
	 * coef_fric) of an animated object with STATIC movement.
	 */
	static public boolean isEnabledProperty (World w, WorldItem it, String name)
	{
		if ((it == null) || (it.kind != WorldItem.AOBJECT) || !valid (w, it))		return true;
		if (name.equals ("speed") || name.equals ("acceleration") || name.equals ("mass") || name.equals ("coef_res") || name.equals ("coef_fric"))
			return w.aobjects ().get (it.index).isMoving ();
		return true;
	}

	/** Names of the dock flow types, the choices of the "flow" property. */
	static public String[] flowNames ()
	{
		WMDock.FlowType[]	types = WMDock.FlowType.values ();
		String[]			names = new String[types.length];
		for (int i = 0; i < types.length; i++)		names[i] = types[i].name ();
		return names;
	}

	static private Point2 toWorld (double lx, double ly, double rx, double ry, double ra)
	{
		return new Point2 (lx * Math.cos (ra) - ly * Math.sin (ra) + rx, lx * Math.sin (ra) + ly * Math.cos (ra) + ry);
	}

	static private Point2 toLocal (double x, double y, double rx, double ry, double ra)
	{
		double	dx = x - rx, dy = y - ry;
		return new Point2 (dx * Math.cos (-ra) - dy * Math.sin (-ra), dx * Math.sin (-ra) + dy * Math.cos (-ra));
	}

	/** Segments of the icon in world coordinates for the reference pose. */
	static public Line2[] iconWorldLines (WMIcon ic, double rx, double ry, double ra)
	{
		return ic.toAbsolute (new Point3 (rx, ry, 0.0), ra);
	}

	/**
	 * Distinct vertices of the icon (local coordinates): coincident segment
	 * endpoints are merged, so that a chain of segments behaves like a polyline.
	 */
	static public Point2[] iconVertices (WMIcon ic)
	{
		List<Point2>	v = new ArrayList<Point2> ();
		for (Line2 l : ic.lines)
		{
			addVertex (v, l.orig ());
			addVertex (v, l.dest ());
		}
		return v.toArray (new Point2[v.size ()]);
	}

	/** Same vertices, in world coordinates. */
	static public Point2[] iconWorldVertices (WMIcon ic, double rx, double ry, double ra)
	{
		Point2[]	v = iconVertices (ic);
		for (int i = 0; i < v.length; i++)		v[i] = toWorld (v[i].x (), v[i].y (), rx, ry, ra);
		return v;
	}

	static private void addVertex (List<Point2> v, Point2 p)
	{
		for (Point2 q : v)
			if (q.distance (p) < ICON_EPS)		return;
		v.add (new Point2 (p));
	}

	/** Index of the icon vertex near world point (x, y) within tol, or -1. */
	static public int pickIconVertex (WMIcon ic, double rx, double ry, double ra, double x, double y, double tol)
	{
		Point2[]	v = iconWorldVertices (ic, rx, ry, ra);
		int			best = -1;
		double		bd = tol;
		for (int i = 0; i < v.length; i++)
		{
			double	d = v[i].distance (x, y);
			if (d < bd) { bd = d; best = i; }
		}
		return best;
	}

	/** Index of the icon segment near world point (x, y) within tol, or -1. */
	static public int pickIconSegment (WMIcon ic, double rx, double ry, double ra, double x, double y, double tol)
	{
		Line2[]		ls = iconWorldLines (ic, rx, ry, ra);
		int			best = -1;
		double		bd = tol;
		for (int i = 0; i < ls.length; i++)
		{
			double	d = segDist (ls[i], x, y);
			if (d < bd) { bd = d; best = i; }
		}
		return best;
	}

	/** Moves vertex <code>vi</code> (and every segment endpoint lying on it) to world point (x, y). */
	static public void moveIconVertex (WMIcon ic, double rx, double ry, double ra, int vi, double x, double y)
	{
		Point2[]	v = iconVertices (ic);
		if ((vi < 0) || (vi >= v.length))		return;
		Point2		p = v[vi];
		Point2		q = toLocal (x, y, rx, ry, ra);
		for (Line2 l : ic.lines)
		{
			double	ox = l.orig ().x (), oy = l.orig ().y (), dx = l.dest ().x (), dy = l.dest ().y ();
			boolean	mo = l.orig ().distance (p) < ICON_EPS, md = l.dest ().distance (p) < ICON_EPS;
			if (mo || md)
				l.set (mo ? q.x () : ox, mo ? q.y () : oy, md ? q.x () : dx, md ? q.y () : dy);
		}
	}

	/** Splits segment <code>si</code> at world point (x, y); returns the index of the new vertex. */
	static public int splitIconSegment (WMIcon ic, double rx, double ry, double ra, int si, double x, double y)
	{
		if ((si < 0) || (si >= ic.lines.length))		return -1;
		Point2		q = toLocal (x, y, rx, ry, ra);
		Line2		l = ic.lines[si];
		double		zq = (l.z1 () + l.z2 ()) / 2.0;			// new vertex: mean elevation of the segment
		Line2[]		lines = new Line2[ic.lines.length + 1];
		System.arraycopy (ic.lines, 0, lines, 0, si + 1);
		lines[si]		= new Line2 (l.orig ().x (), l.orig ().y (), l.z1 (), q.x (), q.y (), zq);
		lines[si + 1]	= new Line2 (q.x (), q.y (), zq, l.dest ().x (), l.dest ().y (), l.z2 ());
		System.arraycopy (ic.lines, si + 1, lines, si + 2, ic.lines.length - si - 1);
		ic.lines = lines;
		Point2[]	v = iconVertices (ic);
		for (int i = 0; i < v.length; i++)
			if (v[i].distance (q) < ICON_EPS)		return i;
		return -1;
	}

	/**
	 * Removes vertex <code>vi</code>. If exactly two segments meet there they
	 * are merged into one; otherwise every segment touching it is deleted.
	 */
	static public void removeIconVertex (WMIcon ic, int vi)
	{
		Point2[]	v = iconVertices (ic);
		if ((vi < 0) || (vi >= v.length))		return;
		Point2		p = v[vi];
		List<Line2>	touching = new ArrayList<Line2> ();
		List<Line2>	rest = new ArrayList<Line2> ();
		for (Line2 l : ic.lines)
			if ((l.orig ().distance (p) < ICON_EPS) || (l.dest ().distance (p) < ICON_EPS))	touching.add (l);
			else																			rest.add (l);
		if (touching.size () == 2)
		{
			Line2	a = touching.get (0), b = touching.get (1);
			Point2	pa = (a.orig ().distance (p) < ICON_EPS) ? a.dest () : a.orig ();
			Point2	pb = (b.orig ().distance (p) < ICON_EPS) ? b.dest () : b.orig ();
			if (pa.distance (pb) > ICON_EPS)
				rest.add (new Line2 (pa.x (), pa.y (), (pa instanceof Point3) ? ((Point3) pa).z () : 0.0, pb.x (), pb.y (), (pb instanceof Point3) ? ((Point3) pb).z () : 0.0));
		}
		ic.lines = rest.toArray (new Line2[rest.size ()]);
	}

	/**
	 * Welds vertex <code>vi</code> to another vertex of the icon lying within
	 * <code>tol</code> (world units) of it, if any. Returns the world point the
	 * vertex was welded to, or null when no other vertex is close enough.
	 */
	static public Point2 weldIconVertex (WMIcon ic, double rx, double ry, double ra, int vi, double tol)
	{
		Point2[]	v = iconWorldVertices (ic, rx, ry, ra);
		if ((vi < 0) || (vi >= v.length))		return null;
		int			best = -1;
		double		bd = tol;
		for (int i = 0; i < v.length; i++)
		{
			if (i == vi)		continue;
			double	d = v[i].distance (v[vi]);
			if (d < bd) { bd = d; best = i; }
		}
		if (best < 0)		return null;
		moveIconVertex (ic, rx, ry, ra, vi, v[best].x (), v[best].y ());
		return v[best];
	}

	/**
	 * Removes the segments left empty after merging vertices (both endpoints on
	 * the same point) and duplicated segments. Returns the number of segments removed.
	 */
	static public int removeEmptyIconSegments (WMIcon ic)
	{
		List<Line2>	kept = new ArrayList<Line2> ();
		for (Line2 l : ic.lines)
		{
			if (l.orig ().distance (l.dest ()) < ICON_EPS)		continue;			// zero length
			boolean	dup = false;
			for (Line2 k : kept)
				if (((k.orig ().distance (l.orig ()) < ICON_EPS) && (k.dest ().distance (l.dest ()) < ICON_EPS))
						|| ((k.orig ().distance (l.dest ()) < ICON_EPS) && (k.dest ().distance (l.orig ()) < ICON_EPS)))
				{ dup = true; break; }
			if (!dup)		kept.add (l);
		}
		int	removed = ic.lines.length - kept.size ();
		if (removed > 0)		ic.lines = kept.toArray (new Line2[kept.size ()]);
		return removed;
	}

	/** Appends a segment given in world coordinates. */
	static public void addIconSegment (WMIcon ic, double rx, double ry, double ra, double x1, double y1, double x2, double y2)
	{
		Point2		a = toLocal (x1, y1, rx, ry, ra), b = toLocal (x2, y2, rx, ry, ra);
		Line2[]		lines = new Line2[ic.lines.length + 1];
		System.arraycopy (ic.lines, 0, lines, 0, ic.lines.length);
		lines[ic.lines.length] = new Line2 (a.x (), a.y (), 0.0, b.x (), b.y (), 0.0);
		ic.lines = lines;
	}

	static public void removeIconSegment (WMIcon ic, int si)
	{
		if ((si < 0) || (si >= ic.lines.length))		return;
		Line2[]		lines = new Line2[ic.lines.length - 1];
		System.arraycopy (ic.lines, 0, lines, 0, si);
		System.arraycopy (ic.lines, si + 1, lines, si, ic.lines.length - si - 1);
		ic.lines = lines;
	}

	/** Invalidates the cached absolute icons of the objects using an icon (after editing it). */
	static public void iconChanged (World w, WMIcon ic)
	{
		for (WMObject o : w.allObjects ())
			if (o.icon == ic)		o.invalidate ();
	}

	static public String segmentsText (Line2[] lines)
	{
		StringBuffer	sb = new StringBuffer ();
		for (int i = 0; i < lines.length; i++)
			sb.append ((i > 0) ? "; " : "").append (fmt (lines[i].orig ().x ())).append (", ").append (fmt (lines[i].orig ().y ())).append (", ").append (fmt (lines[i].z1 ()))
			  .append (", ").append (fmt (lines[i].dest ().x ())).append (", ").append (fmt (lines[i].dest ().y ())).append (", ").append (fmt (lines[i].z2 ()));
		return sb.toString ();
	}

	static public Line2[] parseSegments (String value)
	{
		if (value.trim ().length () == 0)		return new Line2[0];
		double[]	v = nums (value);
		if ((v.length < 6) || (v.length % 6 != 0))
			throw new IllegalArgumentException ("Segments in local coordinates, as: x1, y1, z1, x2, y2, z2; x1, y1, z1, x2, y2, z2; ...");
		Line2[]		lines = new Line2[v.length / 6];
		for (int i = 0; i < lines.length; i++)
			lines[i] = new Line2 (v[6 * i], v[6 * i + 1], v[6 * i + 2], v[6 * i + 3], v[6 * i + 4], v[6 * i + 5]);
		return lines;
	}

	/* Colour helpers: the editor shows colours as #rrggbb; files keep names or r:g:b (ColorTool). */

	static public String toHex (WColor c)
	{
		if (c == null)			return "#000000";
		return String.format ("#%02x%02x%02x", c.getRed (), c.getGreen (), c.getBlue ());
	}

	static public WColor parseColor (String s)
	{
		s = s.trim ();
		String	hex = s.startsWith ("#") ? s.substring (1) : s;
		if (hex.matches ("[0-9a-fA-F]{6}"))
			return new WColor (Integer.parseInt (hex.substring (0, 2), 16), Integer.parseInt (hex.substring (2, 4), 16), Integer.parseInt (hex.substring (4, 6), 16));
		if (hex.matches ("[0-9a-fA-F]{3}"))
			return new WColor (17 * Integer.parseInt (hex.substring (0, 1), 16), 17 * Integer.parseInt (hex.substring (1, 2), 16), 17 * Integer.parseInt (hex.substring (2, 3), 16));
		// colour names and r:g:b, as in the .world files
		for (int i = 0; i < ColorTool.COL_NAMES.length; i++)
			if (ColorTool.COL_NAMES[i].equalsIgnoreCase (s))		return ColorTool.COL_VALUES[i];
		if (s.matches ("\\d{1,3}:\\d{1,3}:\\d{1,3}"))
			return ColorTool.getColorFromName (s);
		throw new IllegalArgumentException ("Use a hexadecimal colour (#f045a7), a colour name (red, gray_dark...) or r:g:b");
	}

	/* Parsing helpers. The .world format separates fields with ", \t", so labels and textures cannot contain them. */

	static private double num (String s)
	{
		try { return Double.parseDouble (s.trim ()); }
		catch (NumberFormatException e) { throw new IllegalArgumentException ("Not a number: " + s); }
	}

	static private double[] nums (String s)
	{
		StringTokenizer		st = new StringTokenizer (s, ",; \t");
		List<Double>		v = new ArrayList<Double> ();
		while (st.hasMoreTokens ())		v.add (Double.valueOf (num (st.nextToken ())));
		double[]			r = new double[v.size ()];
		for (int i = 0; i < r.length; i++)		r[i] = v.get (i).doubleValue ();
		return r;
	}

	static private boolean bool (String s)
	{
		if (s.equalsIgnoreCase ("true") || s.equals ("1"))		return true;
		if (s.equalsIgnoreCase ("false") || s.equals ("0"))		return false;
		throw new IllegalArgumentException ("Expected true or false");
	}

	/**
	 * Writes every file a world names the same way ("./conf/..." for the ones that
	 * live in the project), so that what the editor shows is what the file gets.
	 */
	static public void normalisePaths (World w)
	{
		if (w == null)		return;
		for (tc.shared.world.WMWall l : w.walls ().edges ())			l.texture = FileCellEditor.normalise (l.texture);
		for (tc.shared.world.WMConnector c : w.connectors ().edges ())	c.texture = FileCellEditor.normalise (c.texture);
		for (tc.shared.world.WMZone z : w.zones ().areas ())			z.texture = FileCellEditor.normalise (z.texture);
		for (tc.shared.world.WMFArea f : w.fareas ().areas ())			f.texture = FileCellEditor.normalise (f.texture);
		for (tc.shared.world.WMObject o : w.allObjects ())
		{
			o.shape		= FileCellEditor.normalise (o.shape);
			o.image		= FileCellEditor.normalise (o.image);
		}
		w.walls ().setDefaults (w.walls ().defaultWidth (), w.walls ().defaultHeight (), FileCellEditor.normalise (w.walls ().defaultTexture ()));
		w.connectors ().setDefaults (w.connectors ().defaultWidth (), w.connectors ().defaultHeight (), FileCellEditor.normalise (w.connectors ().defaultTexture ()));
		w.zones ().setDefaultTexture (FileCellEditor.normalise (w.zones ().defaultTexture ()));
		w.fareas ().setDefaultTexture (FileCellEditor.normalise (w.fareas ().defaultTexture ()));
	}

	/** Whether a property of an element names a file (and so is written as "./conf/..."). */
	static public boolean isTextureProperty (String name)	{ return name.endsWith ("texture"); }
	static public boolean isFileProperty (String name)		{ return name.equals ("shape") || name.equals ("image") || isTextureProperty (name); }

	/** The value of a property as it is shown and stored: paths always as "./conf/...". */
	static private String value (String name, String v)
	{
		return isFileProperty (name) ? FileCellEditor.normalise (v) : v;
	}

	static private String token (String s)
	{
		if ((s.length () == 0) || (s.indexOf (',') >= 0) || (s.indexOf (' ') >= 0) || (s.indexOf ('\t') >= 0))
			throw new IllegalArgumentException ("Value cannot be empty or contain spaces or commas");
		return s;
	}

	static private String checkLabel (World w, WorldItem it, String s)
	{
		token (s);
		for (int kind = 0; kind < WorldItem.ICON; kind++)
		{
			int		n = count (w, kind);
			for (int i = 0; i < n; i++)
			{
				WorldItem	o = new WorldItem (kind, i);
				if (!o.equals (it) && s.equals (label (w, o)))
					throw new IllegalArgumentException ("Label '" + s + "' is already used by " + WorldItem.NAMES[kind].toLowerCase () + " " + i);
			}
		}
		return s;
	}
}

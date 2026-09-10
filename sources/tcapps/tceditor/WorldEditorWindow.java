/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
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
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import tc.shared.world.World;

/**
 * Editor for {@link World} maps (.world files). Shows a map, lets the user
 * create and edit every kind of element (walls, objects, forbidden areas,
 * zones, doors, waypoints, docks, beacons, path points and the start point)
 * with the mouse or through a property table, and saves the result back to the
 * .world format understood by the simulator.
 *
 * <pre>
 *   java tcapps.tceditor.WorldEditorWindow [file.world]
 * </pre>
 */
public class WorldEditorWindow extends JFrame implements WorldCanvas.Listener
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
	protected JLabel				statusLabel;
	protected JLabel				selLabel;
	protected JToggleButton[]		toolButtons	= new JToggleButton[14];
	protected Action				undoAction, redoAction, deleteAction;
	protected JCheckBoxMenuItem[]	layerItems	= new JCheckBoxMenuItem[WorldItem.NKINDS];
	protected JCheckBoxMenuItem		gridItem, snapItem, labelsItem;
	protected boolean				syncing		= false;	// avoids selection feedback loops

	/* 3D view */
	protected WorldView3DWindow		view3d;
	protected JToggleButton			view3dButton;
	protected JCheckBoxMenuItem		view3dItem;

	/* ------------------------------------------------------------------ */

	public WorldEditorWindow ()
	{
		this (WorldEdit.newWorld (), null);
	}

	public WorldEditorWindow (World world, File file)
	{
		super (TITLE);
		this.world	= world;
		this.file	= file;
		this.current = WorldEdit.snapshot (world);

		buildGUI ();
		refreshAll ();
		updateTitle ();

		setDefaultCloseOperation (DO_NOTHING_ON_CLOSE);
		addWindowListener (new WindowAdapter ()
		{
			public void windowClosing (WindowEvent e)		{ quit (); }
		});
		pack ();
		setSize (1200, 800);
		setLocationRelativeTo (null);
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
					if (name.endsWith ("texture"))		return FileCellEditor.TEXTURE;
					if (name.equals ("color"))			return ColorCellEditor.INSTANCE;
					if (WorldEdit.isBooleanProperty (name))	return boolEditor;
				}
				return super.getCellEditor (row, column);
			}

			public javax.swing.table.TableCellRenderer getCellRenderer (int row, int column)
			{
				if (column == 1)
				{
					String	name = propModel.nameAt (row);
					if (name.equals ("shape") || name.endsWith ("texture"))		return fileRenderer;
					if (name.equals ("color"))									return colorRenderer;
				}
				return super.getCellRenderer (row, column);
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
		statusLabel	= new JLabel (" ");
		statusLabel.setBorder (BorderFactory.createEmptyBorder (3, 8, 3, 8));

		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (buildToolBar (), BorderLayout.WEST);
		getContentPane ().add (center, BorderLayout.CENTER);
		getContentPane ().add (statusLabel, BorderLayout.SOUTH);
		setJMenuBar (buildMenuBar ());
	}

	private JToolBar buildToolBar ()
	{
		JToolBar		tb = new JToolBar (JToolBar.VERTICAL);
		tb.setFloatable (false);
		ButtonGroup		group = new ButtonGroup ();

		addTool (tb, group, WorldCanvas.T_SELECT,	ToolIcon.SELECT,	"Select / move  (Esc)",						"S");
		addTool (tb, group, WorldCanvas.T_PAN,		ToolIcon.PAN,		"Pan view  (also: middle button, Alt+drag, Space+drag)", "H");
		tb.addSeparator ();
		addTool (tb, group, WorldCanvas.T_WALL,		ToolIcon.WALL,		"Wall: drag from one end to the other",		"W");
		addTool (tb, group, WorldCanvas.T_DOOR,		ToolIcon.DOOR,		"Door: drag along the door opening",		"D");
		addTool (tb, group, WorldCanvas.T_ZONE,		ToolIcon.ZONE,		"Zone: drag a rectangle",					"Z");
		addTool (tb, group, WorldCanvas.T_FAREA,	ToolIcon.FAREA,		"Forbidden area: click the vertices, double-click to close", "F");
		addTool (tb, group, WorldCanvas.T_OBJECT,	ToolIcon.OBJECT,	"Object: click to place (edit icon, shape and colour in Properties)", "O");
		addTool (tb, group, WorldCanvas.T_ICON,		ToolIcon.ICON,		"Edit object icon: drag vertices, click a segment to insert one, drag on empty space to add a segment, right click to remove (also: double-click an object)", "I");
		tb.addSeparator ();
		addTool (tb, group, WorldCanvas.T_WAYPOINT,	ToolIcon.WAYPOINT,	"Waypoint: click to place",					"P");
		addTool (tb, group, WorldCanvas.T_DOCK,		ToolIcon.DOCK,		"Dock: click to place",						"K");
		addTool (tb, group, WorldCanvas.T_BEACON,	ToolIcon.BEACON,	"Strip beacon: click to place",				"B");
		addTool (tb, group, WorldCanvas.T_CBEACON,	ToolIcon.CBEACON,	"Cylindrical beacon: click to place",		"C");
		addTool (tb, group, WorldCanvas.T_PATH,		ToolIcon.PATH,		"Path point: click to append to the path",	"T");
		addTool (tb, group, WorldCanvas.T_START,	ToolIcon.START,		"Robot start point: click to place",		"R");
		tb.addSeparator ();

		deleteAction = new AbstractAction ("Delete", new ToolIcon (ToolIcon.DELETE))
		{
			public void actionPerformed (ActionEvent e)		{ canvas.deleteSelection (); }
		};
		deleteAction.putValue (Action.SHORT_DESCRIPTION, "Delete selected element  (Del)");
		tb.add (deleteAction).setHideActionText (true);

		Action	fit = new AbstractAction ("Zoom to fit", new ToolIcon (ToolIcon.ZOOM_FIT))
		{
			public void actionPerformed (ActionEvent e)		{ canvas.zoomToFit (); }
		};
		fit.putValue (Action.SHORT_DESCRIPTION, "Zoom to fit the whole map  (Ctrl+0)");
		tb.add (fit).setHideActionText (true);

		Action	zin = new AbstractAction ("Zoom in", new ToolIcon (ToolIcon.ZOOM_IN))
		{
			public void actionPerformed (ActionEvent e)		{ canvas.zoom (1.25); }
		};
		zin.putValue (Action.SHORT_DESCRIPTION, "Zoom in  (mouse wheel)");
		tb.add (zin).setHideActionText (true);

		Action	zout = new AbstractAction ("Zoom out", new ToolIcon (ToolIcon.ZOOM_OUT))
		{
			public void actionPerformed (ActionEvent e)		{ canvas.zoom (0.8); }
		};
		zout.putValue (Action.SHORT_DESCRIPTION, "Zoom out  (mouse wheel)");
		tb.add (zout).setHideActionText (true);

		// --- 3D view toggle, at the bottom of the toolbar
		tb.add (Box.createVerticalGlue ());
		tb.addSeparator ();
		view3dButton = new JToggleButton (new ToolIcon (ToolIcon.VIEW3D));
		view3dButton.setToolTipText ("Show / hide the synchronised 3D view (Java 3D)  [Ctrl+3]");
		view3dButton.setFocusable (false);
		view3dButton.addActionListener (new java.awt.event.ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ show3D (view3dButton.isSelected ()); }
		});
		tb.add (view3dButton);

		toolButtons[WorldCanvas.T_SELECT].setSelected (true);
		return tb;
	}

	/* ------------------------------------------------------------------ */
	/* 3D view                                                             */
	/* ------------------------------------------------------------------ */

	/** Shows or hides the Java 3D view window, creating it on first use. */
	public void show3D (boolean show)
	{
		if (show && (view3d == null))
		{
			try
			{
				view3d = new WorldView3DWindow (world, new Runnable ()
				{
					public void run ()		{ set3DToggles (false); }
				});
				view3d.setSize (900, 700);
				// place it beside the editor when there is room
				java.awt.Rectangle	r = getBounds ();
				java.awt.Rectangle	scr = getGraphicsConfiguration ().getBounds ();
				if (r.x + r.width + 900 <= scr.x + scr.width)	view3d.setLocation (r.x + r.width, r.y);
				else											view3d.setLocation (r.x + 60, r.y + 60);
			} catch (Throwable e)
			{
				e.printStackTrace ();
				view3d = null;
				set3DToggles (false);
				JOptionPane.showMessageDialog (this, "The 3D view cannot be created. Check that Java 3D and JOGL (jarlibs/jsdn_java3d.jar, jarlibs/jogl/*.jar) are in the classpath.\n\n" + e,
						TITLE, JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		if (view3d == null)				return;
		if (show)
		{
			view3d.setWorld (world);
			view3d.setSelection (canvas.getSelection ());
		}
		view3d.setVisible (show);
		set3DToggles (show);
	}

	private void set3DToggles (boolean on)
	{
		view3dButton.setSelected (on);
		if (view3dItem != null)		view3dItem.setSelected (on);
	}

	private void sync3D ()
	{
		if ((view3d != null) && view3d.isVisible ())		view3d.setWorld (world);
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

	private JMenuBar buildMenuBar ()
	{
		int			mask = Toolkit.getDefaultToolkit ().getMenuShortcutKeyMask ();
		JMenuBar	mb = new JMenuBar ();

		// --- File
		JMenu		mfile = new JMenu ("File");
		mfile.setMnemonic (KeyEvent.VK_F);
		mfile.add (item ("New World", KeyStroke.getKeyStroke (KeyEvent.VK_N, mask), new AbstractAction ()
		{
			public void actionPerformed (ActionEvent e)		{ newWorld (); }
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
		mb.add (mfile);

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
		medit.add (item (deleteAction, KeyStroke.getKeyStroke (KeyEvent.VK_DELETE, 0)));
		// (Esc itself is handled by the canvas: it first cancels drawings / leaves the current tool, then deselects)
		medit.add (item ("Deselect", null, new AbstractAction ()
		{
			public void actionPerformed (ActionEvent e)		{ canvas.setSelection (null); selectTool (WorldCanvas.T_SELECT); }
		}));
		medit.addSeparator ();
		medit.add (item ("Edit Default Values...", null, new AbstractAction ()
		{
			public void actionPerformed (ActionEvent e)		{ canvas.setSelection (new WorldItem (WorldItem.DEFAULTS, 0)); }
		}));
		medit.add (item ("Edit Start Point...", null, new AbstractAction ()
		{
			public void actionPerformed (ActionEvent e)		{ canvas.setSelection (new WorldItem (WorldItem.START, 0)); }
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
		for (int k = 0; k < WorldItem.DEFAULTS; k++)
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
				for (int k = 0; k < WorldItem.DEFAULTS; k++) { layerItems[k].setSelected (true); canvas.setKindVisible (k, true); }
			}
		}));
		mview.add (mlayers);
		mview.addSeparator ();
		view3dItem = new JCheckBoxMenuItem ("3D View", false);
		view3dItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_3, mask));
		view3dItem.addActionListener (new java.awt.event.ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ show3D (view3dItem.isSelected ()); }
		});
		mview.add (view3dItem);
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
				JOptionPane.showMessageDialog (WorldEditorWindow.this,
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
			"  W  Wall, D  Door, Z  Zone: drag on the map.\n" +
			"  F  Forbidden area: click the vertices, double-click / Enter to close.\n" +
			"  O  Object, P  Waypoint, K  Dock, B  Strip beacon, C  Cylindrical beacon,\n" +
			"  T  Path point, R  Start point: click to place.\n" +
			"  I  Edit object icon (or double-click an object): drag vertices, click a segment to insert\n" +
			"       a vertex, drag on empty space (Shift+drag from a vertex) to add a segment, right click / Del removes.\n" +
			"  Right click with a creation tool returns to Select.\n\n" +
			"Keyboard:  Del deletes, arrows nudge the selection, Esc deselects / cancels,\n" +
			"  Ctrl+Z / Ctrl+Shift+Z undo / redo, mouse wheel zooms, Ctrl+0 zoom to fit.\n\n" +
			"3D view: the button at the bottom of the toolbar (or Ctrl+3) opens a Java 3D window that\n" +
			"  follows every change and highlights the selection.\n\n" +
			"Properties: edit any value in the table and press Enter. Labels of zones, doors,\n" +
			"  waypoints and docks must be unique. 'Defaults' (Edit menu) holds the default\n" +
			"  wall/door sizes and textures written to the file.";
		JOptionPane.showMessageDialog (this, msg, "Mouse and Keyboard", JOptionPane.INFORMATION_MESSAGE);
	}

	/* ------------------------------------------------------------------ */
	/* WorldCanvas.Listener                                                */
	/* ------------------------------------------------------------------ */

	public void selectionChanged (WorldItem item)
	{
		propModel.setItem (item);
		selLabel.setText ((item == null) ? " " : WorldItem.NAMES[item.kind] + ":  " + WorldEdit.describe (world, item));
		deleteAction.setEnabled ((item != null) && (item.kind != WorldItem.START) && (item.kind != WorldItem.DEFAULTS));
		if (view3d != null)		view3d.setSelection (item);
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
		refreshTree ();
		propModel.refresh ();
		selectionChanged (canvas.getSelection ());
		sync3D ();
	}

	public void worldPreview ()
	{
		if ((view3d != null) && view3d.isVisible ())		view3d.worldChanged ();
	}

	public void statusChanged (String text)
	{
		statusLabel.setText ((text.length () == 0) ? " " : text);
	}

	public void toolFinished ()
	{
		selectTool (WorldCanvas.T_SELECT);
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
		String	snap = WorldEdit.snapshot (world);
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
		world = WorldEdit.restore (current);
		canvas.setWorld (world);
		dirty = true;
		refreshAll ();
		canvas.setSelection (sel);		// kept if it still exists
		updateTitle ();
		updateUndoActions ();
		sync3D ();
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

	private boolean confirmDiscard ()
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

	public void newWorld ()
	{
		if (!confirmDiscard ())			return;
		setWorld (WorldEdit.newWorld (), null);
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
		world	= w;
		file	= f;
		dirty	= false;
		current	= WorldEdit.snapshot (world);
		undoStack.clear ();	undoNames.clear ();	redoStack.clear ();	redoNames.clear ();
		canvas.setWorld (world);
		refreshAll ();
		updateTitle ();
		updateUndoActions ();
		sync3D ();
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
			World	w = WorldEdit.newWorld ();
			w.fromDxfFile (fc.getSelectedFile ().getPath ());
			// a DXF-loaded world has no forbidden areas collection: rebuild from a snapshot
			w = WorldEdit.restore (WorldEdit.snapshot (w));
			setWorld (w, null);
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
			world.toDxfFile (f.getPath ());
			statusChanged ("Exported " + f.getPath ());
		} catch (Exception e)
		{
			e.printStackTrace ();
			JOptionPane.showMessageDialog (this, "Cannot export DXF:\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
		}
	}

	public void quit ()
	{
		if (!confirmDiscard ())			return;
		if (view3d != null)				view3d.dispose ();
		dispose ();
		System.exit (0);
	}

	private void updateTitle ()
	{
		setTitle (TITLE + " - " + ((file == null) ? "untitled" + World.SUFFIX : file.getName ()) + (dirty ? " *" : ""));
	}

	/* ------------------------------------------------------------------ */
	/* Element tree                                                        */
	/* ------------------------------------------------------------------ */

	private void refreshAll ()
	{
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
		boolean		first = (treeRoot.getChildCount () == 0);

		syncing = true;
		treeRoot.removeAllChildren ();
		for (int kind = 0; kind < WorldItem.NKINDS; kind++)
		{
			int						n = WorldEdit.count (world, kind);
			DefaultMutableTreeNode	cat = new KindNode (kind, n);
			for (int i = 0; i < n; i++)
				cat.add (new ItemNode (new WorldItem (kind, i)));
			treeRoot.add (cat);
		}
		treeModel.reload ();
		for (int i = 0; i < treeRoot.getChildCount (); i++)
		{
			DefaultMutableTreeNode	n = (DefaultMutableTreeNode) treeRoot.getChildAt (i);
			int						kind = ((Integer) n.getUserObject ()).intValue ();
			boolean					exp = first ? (n.getChildCount () <= 40) : expanded[kind];
			if (exp)		tree.expandPath (new TreePath (n.getPath ()));
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
		if (item.kind < treeRoot.getChildCount ())
		{
			DefaultMutableTreeNode	cat = (DefaultMutableTreeNode) treeRoot.getChildAt (item.kind);
			if (item.index < cat.getChildCount ())
			{
				TreePath	path = new TreePath (((DefaultMutableTreeNode) cat.getChildAt (item.index)).getPath ());
				tree.setSelectionPath (path);
				tree.scrollPathToVisible (path);
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
			return ((kind == WorldItem.START) || (kind == WorldItem.DEFAULTS)) ? WorldItem.PLURALS[kind] : WorldItem.PLURALS[kind] + "  (" + n + ")";
		}
	}

	/** Element node: user object is the WorldItem. */
	private class ItemNode extends DefaultMutableTreeNode
	{
		private static final long	serialVersionUID = 1L;
		ItemNode (WorldItem it)		{ super (it); }
		public String toString ()	{ return WorldEdit.describe (world, (WorldItem) getUserObject ()); }
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
			item	= WorldEdit.valid (world, it) ? it : null;
			names	= (item == null) ? new String[0] : WorldEdit.propertyNames (world, item);
			fireTableDataChanged ();
		}

		void refresh ()
		{
			if (!WorldEdit.valid (world, item))		setItem (null);
			else									fireTableRowsUpdated (0, Math.max (0, names.length - 1));
		}

		String nameAt (int r)							{ return ((r >= 0) && (r < names.length)) ? names[r] : ""; }
		public int getRowCount ()						{ return names.length; }
		public int getColumnCount ()					{ return 2; }
		public String getColumnName (int c)				{ return (c == 0) ? "Property" : "Value"; }
		public boolean isCellEditable (int r, int c)	{ return c == 1; }

		public Object getValueAt (int r, int c)
		{
			if (item == null)			return "";
			return (c == 0) ? names[r] : WorldEdit.getProperty (world, item, names[r]);
		}

		public void setValueAt (Object value, int r, int c)
		{
			if ((item == null) || (c != 1))		return;
			String	v = (value == null) ? "" : value.toString ();
			if (v.equals (WorldEdit.getProperty (world, item, names[r])))		return;
			try
			{
				WorldEdit.setProperty (world, item, names[r], v);
				canvas.repaint ();
				worldChanged ("Edit " + names[r]);
			} catch (IllegalArgumentException e)
			{
				JOptionPane.showMessageDialog (WorldEditorWindow.this, e.getMessage (), "Invalid value", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/* ------------------------------------------------------------------ */

	static public void main (String[] args)
	{
		try
		{
			System.setProperty ("apple.laf.useScreenMenuBar", "true");
			UIManager.setLookAndFeel (UIManager.getSystemLookAndFeelClassName ());
		} catch (Exception e) { }

		final String	name = (args.length > 0) ? args[0] : null;
		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()
			{
				WorldEditorWindow	win = new WorldEditorWindow ();
				win.setVisible (true);
				if (name != null)		win.loadWorld (new File (name));
				else					win.canvas.zoomToFit ();
			}
		});
	}
}

/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import tc.DeployArch;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import tcapps.tcsimulator.arch.ArchCanvas;
import tcapps.tcsimulator.arch.ArchModel;
import tcapps.tcsimulator.arch.ArchModel.Block;
import tcapps.tcsimulator.arch.ArchModel.Property;

/**
 * Block editor of a deployment architecture ({@link DeployArch}), as a panel
 * that a window ({@link DeploymentWindow}) or a dialog
 * ({@link DeploymentDialog}) hosts: a toolbar on the left adds Linda spaces,
 * routers, modules and robots; the centre shows the block diagram
 * ({@link ArchCanvas}); on the right, like in the world editor, a tree with
 * the "Global" category (the global Linda space) and one category per robot
 * with its modules, and below it the property editor of the selected block.
 *
 * The panel edits the deployment it is given (the dialog hands it a copy).
 */
public class DeploymentPanel extends JPanel implements ArchCanvas.Listener
{
	private static final long		serialVersionUID = 1L;

	static public final String		TITLE		= "Deployment Architecture Editor";
	static public final String		DEPLOY_DIR	= ArchModel.DEPLOY_DIR;	// deployment architectures (.deploy)

	static public final java.awt.Color	C_FIXED	= new java.awt.Color (238, 238, 238);	// a cell that is filled in rather than typed

	/** What the window or dialog hosting the editor needs to know. */
	public interface Host
	{
		/** The file or the modified state changed (title). */
		void deploymentStateChanged (DeploymentPanel panel);
	}

	protected Host					host;

	protected ArchModel				model;
	protected ArchCanvas			canvas;
	protected JTree					tree;
	protected DefaultTreeModel		treeModel;
	protected DefaultMutableTreeNode	root, globalNode;
	protected JTable				propsTB;
	protected PropsModel			propsModel;
	protected javax.swing.border.TitledBorder	propsBorder;	// title: the selected block
	protected JTable				eventsTB;
	protected EventsModel			eventsModel;
	protected JButton				addEventBT, removeEventBT;
	protected JComboBox<String>		symbolCB;				// symbols offered in the events table
	protected JSplitPane			mainSP, rightSP;
	protected boolean				dividersSet;

	static public final int			RIGHT_WIDTH		= 320;		// tree + properties column (as WorldEditorWindow)
	static public final double		TREE_FRACTION	= 0.55;		// share of the tree in that column (as WorldEditorWindow)
	protected Action				lindaAC, routerAC, moduleAC, robotAC, deleteAC;
	protected boolean				syncing;				// tree <-> canvas selection in progress

	/** Rows of the property editor: the visible properties of the block ({@link ArchModel#propertiesOf}). */
	protected class PropsModel extends AbstractTableModel
	{
		private static final long	serialVersionUID = 1L;
		Block			block;
		List<Property>	rows	= new ArrayList<Property> ();

		void setBlock (Block b)
		{
			block	= b;
			rows.clear ();
			if (b != null)		rows.addAll (model.propertiesOf (b));
			fireTableDataChanged ();
		}

		Property propertyAt (int r)				{ return rows.get (r); }

		public int getRowCount ()				{ return rows.size (); }
		public int getColumnCount ()			{ return 2; }
		public String getColumnName (int c)		{ return (c == 0) ? "Property" : "Value"; }
		public boolean isCellEditable (int r, int c)	{ return c == 1; }

		public Object getValueAt (int r, int c)
		{
			Property	p = rows.get (r);
			return (c == 0) ? p.label : value (p, model.get (block, p.key));
		}

		public void setValueAt (Object v, int r, int c)
		{
			Property	p = rows.get (r);
			model.set (block, p.key, value (p, (v == null) ? "" : v.toString ()));
			fireTableCellUpdated (r, c);
			if (p.key.equals ("INFO"))		rebuild (block);				// the label of the block changed
			else							canvas.repaint ();
		}
	}

	/**
	 * Writes every file a deployment names the same way ("./conf/..." for the ones
	 * that live in the project), so that what the editor shows is what the file gets.
	 */
	static public void normalisePaths (ArchModel m)
	{
		List<Block>		blocks = new ArrayList<Block> ();

		if (m == null)		return;
		if (m.hasGlobalLinda ())		blocks.add (new Block (ArchModel.GLOBAL_LINDA, -1));
		for (int r : m.robots ())		blocks.add (new Block (ArchModel.ROBOT, r));
		blocks.addAll (m.allRobotBlocks ());
		for (Block b : blocks)
			for (Property p : m.propertiesOf (b))
				if (p.type == ArchModel.P_FILE)		m.set (b, p.key, FileCellEditor.normalise (m.get (b, p.key)));
	}

	/**
	 * The editor of a property naming a class: the classes of the development
	 * deriving from the one the property asks for -- a module and a robot are run
	 * as threads of the runtime, a router is one of the coordination layer -- so
	 * that one is chosen from what there is instead of being typed in.
	 *
	 * Only the classes that are there are offered: one that the development no
	 * longer builds is not kept in the list just because a deployment names it.
	 * A property that may be left blank offers that first.
	 */
	private TableCellEditor classEditor (Property p, String current)
	{
		String				base = p.classBase;
		String[]			not = p.classNot;
		String				kind = ArchModel.moduleTypeBase (model.get (propsModel.block, "TYPE"));
		List<String>		names;
		JComboBox<String>	cb;

		// a module of a kind is one of the classes of that kind, and a monitor is
		// still no module of an architecture even though it is a controller
		if ((propsModel.block != null) && (propsModel.block.kind == ArchModel.MODULE) && (kind != null))
			base	= kind;
		names	= new ArrayList<String> (DriverClasses.of (base, false, true, not));

		if (current == null)		current = "";
		current	= current.trim ();
		if (p.classBlank)			names.add (0, "");
		cb		= new JComboBox<String> (names.toArray (new String[0]));
		cb.setSelectedItem (current);
		cb.setToolTipText ("Classes of the development deriving from " + base);
		return new DefaultCellEditor (cb);
	}

	/** What a file property may be chosen from, or null when any file will do. */
	static private FileNameExtensionFilter fileFilter (Property p)
	{
		if ((p.fileExts == null) || (p.fileExts.length == 0))		return null;
		return new FileNameExtensionFilter (p.fileDesc, p.fileExts);
	}

	/** The value of a property as it is shown and stored: paths always as "./conf/...". */
	static private String value (Property p, String v)
	{
		return (p.type == ArchModel.P_FILE) ? FileCellEditor.normalise (v) : v;
	}

	/** Rows of the events table (CONNECT of a module): symbol, class, method. Edits are written back to the model. */
	protected class EventsModel extends AbstractTableModel
	{
		private static final long	serialVersionUID = 1L;
		Block			block;
		List<String[]>	rows	= new ArrayList<String[]> ();

		void setBlock (Block b)
		{
			block	= b;
			rows.clear ();
			if ((b != null) && model.hasEvents (b))		rows.addAll (model.events (b));
			fireTableDataChanged ();
		}

		public int getRowCount ()				{ return rows.size (); }
		public int getColumnCount ()			{ return 3; }
		public String getColumnName (int c)		{ return (c == 0) ? "Symbol" : (c == 1) ? "Class" : "Method"; }
		public Object getValueAt (int r, int c)	{ return rows.get (r)[c]; }
		// the class is not typed: it is what the symbol carries
		public boolean isCellEditable (int r, int c)	{ return (block != null) && model.hasEvents (block) && (c != 1); }

		public void setValueAt (Object v, int r, int c)
		{
			String		val = (v == null) ? "" : v.toString ().trim ();

			if (val.equals (rows.get (r)[c]))		return;
			rows.get (r)[c] = val;
			// the symbol says what the event carries, and what it carries says which
			// methods it can arrive at: the one method that takes it is no choice at all
			if (c == 0)
			{
				List<String>	ms;

				rows.get (r)[1]	= model.itemClassOf (val);
				ms				= model.methodsFor (block, rows.get (r)[1]);
				if (ms.size () == 1)								rows.get (r)[2] = ms.get (0);
				else if (!ms.contains (rows.get (r)[2]))			rows.get (r)[2] = "";
				fireTableRowsUpdated (r, r);
			}
			else	fireTableCellUpdated (r, c);
			model.setEvents (block, rows);
			eventsChanged ();
		}

		/** The methods the event of a row can arrive at, as its symbol decides. */
		List<String> methodsAt (int r)
		{
			if ((r < 0) || (r >= rows.size ()))		return new ArrayList<String> ();
			return model.methodsFor (block, rows.get (r)[1]);
		}

		void add ()
		{
			rows.add (new String[] { "", "", "" });
			fireTableRowsInserted (rows.size () - 1, rows.size () - 1);
		}

		void remove (int r)
		{
			rows.remove (r);
			fireTableRowsDeleted (r, r);
			model.setEvents (block, rows);
			eventsChanged ();
		}
	}

	/**
	 * The events of a module were edited: what a module is given is written under
	 * its block in the diagram, and how much of it there is says how much room the
	 * row of the module takes, so the diagram is laid out again there and then
	 * instead of waiting to be clicked on.
	 */
	private void eventsChanged ()
	{
		canvas.modelChanged ();
		updateTitle ();
	}

	private JPanel buildEventsPanel ()
	{
		eventsModel	= new EventsModel ();
		eventsTB	= new JTable (eventsModel)
		{
			private static final long	serialVersionUID = 1L;

			/**
			 * The method of an event is picked among the ones of the class of the
			 * module that take what the symbol carries, and can still be typed: a
			 * module whose class the development does not hold offers none.
			 */
			public TableCellEditor getCellEditor (int row, int column)
			{
				if (column == 2)
				{
					JComboBox<String>	cb = new JComboBox<String> (eventsModel.methodsAt (row).toArray (new String[0]));

					cb.setEditable (true);
					cb.setSelectedItem (eventsModel.getValueAt (row, column));
					return new DefaultCellEditor (cb);
				}
				return super.getCellEditor (row, column);
			}
		};
		eventsTB.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		eventsTB.setRowHeight (20);
		eventsTB.putClientProperty ("terminateEditOnFocusLost", Boolean.TRUE);
		eventsTB.getColumnModel ().getColumn (0).setPreferredWidth (70);
		symbolCB	= new JComboBox<String> ();
		symbolCB.setEditable (true);										// new symbols can still be typed
		eventsTB.getColumnModel ().getColumn (0).setCellEditor (new DefaultCellEditor (symbolCB));
		eventsTB.getColumnModel ().getColumn (1).setPreferredWidth (150);
		eventsTB.getColumnModel ().getColumn (1).setCellRenderer (new javax.swing.table.DefaultTableCellRenderer ()
		{
			private static final long	serialVersionUID = 1L;

			public java.awt.Component getTableCellRendererComponent (JTable t, Object value, boolean sel, boolean focus, int row, int col)
			{
				super.getTableCellRendererComponent (t, value, sel, focus, row, col);
				if (!sel)		setBackground (C_FIXED);
				setToolTipText (((value == null) || (value.toString ().trim ().length () == 0))
								? "No item class is known for this symbol" : value.toString ());
				return this;
			}
		});
		eventsTB.getColumnModel ().getColumn (2).setPreferredWidth (90);
		eventsTB.getSelectionModel ().addListSelectionListener (new javax.swing.event.ListSelectionListener ()
		{
			public void valueChanged (javax.swing.event.ListSelectionEvent e)		{ updateEventButtons (); }
		});
		JScrollPane	sp = new JScrollPane (eventsTB);
		sp.setVerticalScrollBarPolicy (JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);		// modules may register many events
		sp.setPreferredSize (new Dimension (RIGHT_WIDTH, 3 * 20 + 20));

		addEventBT		= new JButton ("Add");
		removeEventBT	= new JButton ("Remove");
		addEventBT.putClientProperty ("JComponent.sizeVariant", "small");
		removeEventBT.putClientProperty ("JComponent.sizeVariant", "small");
		addEventBT.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)
			{
				if (eventsTB.isEditing ())		eventsTB.getCellEditor ().stopCellEditing ();	// whatever was being typed belongs to its own row
				eventsModel.add ();
				int	r = eventsModel.getRowCount () - 1;
				eventsTB.setRowSelectionInterval (r, r);
				eventsTB.editCellAt (r, 0);
				eventsTB.requestFocusInWindow ();
			}
		});
		removeEventBT.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)
			{
				if (eventsTB.isEditing ())		eventsTB.getCellEditor ().cancelCellEditing ();
				int	r = eventsTB.getSelectedRow ();
				if (r >= 0)		eventsModel.remove (r);
				updateEventButtons ();
			}
		});
		JPanel		buttons = new JPanel (new FlowLayout (FlowLayout.RIGHT, 4, 2));
		buttons.add (addEventBT);
		buttons.add (removeEventBT);

		JPanel		pn = new JPanel (new BorderLayout ());
		pn.setBorder (BorderFactory.createTitledBorder ("Trigger Events"));
		pn.add (sp, BorderLayout.CENTER);
		pn.add (buttons, BorderLayout.SOUTH);
		return pn;
	}

	private void updateEventButtons ()
	{
		boolean	on = (eventsModel.block != null) && model.hasEvents (eventsModel.block);
		eventsTB.setEnabled (on);
		addEventBT.setEnabled (on);
		removeEventBT.setEnabled (on && (eventsTB.getSelectedRow () >= 0));
	}

	/** Puts the split dividers at the world editor proportions. */
	public void resetDividers ()
	{
		if (mainSP.getWidth () <= 0)		return;
		dividersSet	= true;
		mainSP.setDividerLocation (mainSP.getWidth () - mainSP.getDividerSize () - RIGHT_WIDTH);
		rightSP.setDividerLocation (TREE_FRACTION);
	}

	/**
	 * @param deploy  deployment architecture to edit (edited in place: hand over a copy to keep the original)
	 * @param host    window or dialog to notify of title changes (may be null)
	 */
	public DeploymentPanel (DeployArch deploy, Host host)
	{
		super (new BorderLayout ());
		this.host	= host;
		model	= new ArchModel (deploy);
		model.setStartNames (startNamesOf (model.getDeploy ()));
		buildGUI ();
		rebuild (null);
		updateTitle ();
	}

	/** A new deployment with one robot, to start from scratch. */
	static public DeployArch newDeploy ()			{ return DeployArch.create (); }

	public ArchCanvas	getCanvas ()				{ return canvas; }
	public DeployArch	getDeploy ()				{ return model.getDeploy (); }

	/** Title of the hosting window: file name (or untitled) and the modified mark. */
	public String getTitle ()
	{
		File	f = model.getDeploy ().getFile ();
		return ((f != null) ? f.getName () : "untitled." + DeployArch.EXTENSION) + (model.getDeploy ().isModified () ? " *" : "");
	}

	/** Commits whatever the user is typing in the tables (before saving or accepting). */
	public void stopEditing ()
	{
		if (propsTB.isEditing ())		propsTB.getCellEditor ().stopCellEditing ();
		if (eventsTB.isEditing ())		eventsTB.getCellEditor ().stopCellEditing ();
	}

	private void buildGUI ()
	{
		// --- centre: block diagram
		canvas	= new ArchCanvas (model);
		canvas.addListener (this);
		JScrollPane	canvasSP = new JScrollPane (canvas);
		canvasSP.setBorder (BorderFactory.createEmptyBorder ());

		// --- left: toolbar
		JToolBar	tb = new JToolBar (JToolBar.VERTICAL);
		tb.setFloatable (false);
		lindaAC		= ToolButtons.action ("Linda", ToolIcon.LINDA, "Add a global Linda space", new Runnable () { public void run () { select (model.addGlobalLinda ()); } });
		routerAC	= ToolButtons.action ("Router", ToolIcon.ROUTER, "Add the Linda router of the selected robot", new Runnable () { public void run () { select (model.addRouter (currentRobot ())); } });
		moduleAC	= ToolButtons.action ("Module", ToolIcon.MODULE, "Add a module to the selected robot", new Runnable () { public void run () { addModule (); } });
		robotAC		= ToolButtons.action ("Robot", ToolIcon.ROBOT, "Add a robot", new Runnable () { public void run () { select (model.addRobot ()); } });
		deleteAC	= ToolButtons.action ("Delete", ToolIcon.DELETE, "Delete the selected block  [Delete]", new Runnable () { public void run () { deleteSelection (); } });
		tb.add (ToolButtons.flatButton (lindaAC));
		tb.add (ToolButtons.flatButton (robotAC));
		tb.add (ToolButtons.flatButton (routerAC));
		tb.add (ToolButtons.flatButton (moduleAC));
		tb.addSeparator ();
		tb.add (ToolButtons.flatButton (deleteAC));
		tb.add (Box.createVerticalGlue ());

		// --- right: tree + properties
		root		= new DefaultMutableTreeNode ("Architecture");
		treeModel	= new DefaultTreeModel (root);
		tree		= new JTree (treeModel);
		tree.setRootVisible (false);
		tree.setShowsRootHandles (true);
		tree.getSelectionModel ().setSelectionMode (TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setCellRenderer (new javax.swing.tree.DefaultTreeCellRenderer ()
		{
			private static final long	serialVersionUID = 1L;
			public java.awt.Component getTreeCellRendererComponent (JTree t, Object value, boolean s, boolean exp, boolean leaf, int row, boolean focus)
			{
				super.getTreeCellRendererComponent (t, value, s, exp, leaf, row, focus);
				Object	o = ((DefaultMutableTreeNode) value).getUserObject ();
				if (o instanceof Block)
				{
					Block	b = (Block) o;
					if (b.kind == ArchModel.ROBOT)		setText (model.getRobotId (b.robot));
					else								setText (model.labelOf (b));
					setIcon (new ToolIcon (iconOf (b), 16));
				}
				return this;
			}
		});
		tree.addTreeSelectionListener (new TreeSelectionListener ()
		{
			public void valueChanged (TreeSelectionEvent e)		{ treeSelected (); }
		});
		JScrollPane	treeSP = new JScrollPane (tree);
		treeSP.setPreferredSize (new Dimension (300, 260));

		propsModel	= new PropsModel ();
		propsTB		= new JTable (propsModel)
		{
			private static final long	serialVersionUID = 1L;
			private final FileCellEditor.Renderer	fileRenderer = new FileCellEditor.Renderer ();
			private final DefaultCellEditor			boolEditor = new DefaultCellEditor (new JComboBox<String> (new String[] { "true", "false" }));
			private final Map<String, TableCellEditor>	editors = new HashMap<String, TableCellEditor> ();

			public TableCellEditor getCellEditor (int row, int column)
			{
				if (column == 1)
				{
					Property	p = propsModel.propertyAt (row);
					switch (p.type)
					{
					case ArchModel.P_BOOLEAN:	return boolEditor;
					case ArchModel.P_CLASS:		return classEditor (p, model.get (propsModel.block, p.key));
					case ArchModel.P_CHOICE:
					case ArchModel.P_FILE:
					{
						String			id = ArchModel.KIND_NAMES[propsModel.block.kind] + "/" + p.key + ((p.choices != null) ? "/" + String.join ("|", p.choices) : "");
						TableCellEditor	ed = editors.get (id);
						if (ed == null)
						{
							if (p.type == ArchModel.P_CHOICE)	ed = new DefaultCellEditor (new JComboBox<String> (p.choices));
							else								ed = new FileCellEditor ("Select " + p.label, p.fileDir, fileFilter (p));
							editors.put (id, ed);
						}
						return ed;
					}
					}
				}
				return super.getCellEditor (row, column);
			}

			public TableCellRenderer getCellRenderer (int row, int column)
			{
				if ((column == 1) && (propsModel.propertyAt (row).type == ArchModel.P_FILE))		return fileRenderer;
				return super.getCellRenderer (row, column);
			}
		};
		propsTB.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		propsTB.setRowHeight (20);
		propsTB.putClientProperty ("terminateEditOnFocusLost", Boolean.TRUE);
		propsTB.getColumnModel ().getColumn (0).setPreferredWidth (150);
		propsTB.getColumnModel ().getColumn (1).setPreferredWidth (150);
		JScrollPane	propsSP = new JScrollPane (propsTB);
		propsSP.setPreferredSize (new Dimension (300, 240));
		// properties and events grouped under one border titled with the selected block
		propsBorder	= BorderFactory.createTitledBorder (" ");
		JPanel		propsPN = new JPanel (new BorderLayout (0, 4));
		propsPN.setBorder (propsBorder);
		propsPN.add (propsSP, BorderLayout.CENTER);
		propsPN.add (buildEventsPanel (), BorderLayout.SOUTH);

		// same proportions as the world editor: right column 320 px, tree 55 % of its height
		rightSP		= new JSplitPane (JSplitPane.VERTICAL_SPLIT, treeSP, propsPN);
		rightSP.setResizeWeight (TREE_FRACTION);
		rightSP.setPreferredSize (new Dimension (RIGHT_WIDTH, 600));
		rightSP.setBorder (BorderFactory.createEmptyBorder ());

		mainSP		= new JSplitPane (JSplitPane.HORIZONTAL_SPLIT, canvasSP, rightSP);
		mainSP.setResizeWeight (1.0);
		mainSP.setBorder (BorderFactory.createEmptyBorder ());
		canvasSP.setMinimumSize (new Dimension (300, 200));
		rightSP.setMinimumSize (new Dimension (240, 200));
		// the divider positions are only meaningful once the panel has its real size
		addComponentListener (new java.awt.event.ComponentAdapter ()
		{
			public void componentShown (java.awt.event.ComponentEvent e)		{ resetDividers (); }
			public void componentResized (java.awt.event.ComponentEvent e)	{ if (!dividersSet && (getWidth () > 0)) resetDividers (); }
		});

		for (JComponent c : new JComponent[] { canvas, tree })
		{
			c.getInputMap (JComponent.WHEN_FOCUSED).put (KeyStroke.getKeyStroke (KeyEvent.VK_DELETE, 0), "delete");
			c.getInputMap (JComponent.WHEN_FOCUSED).put (KeyStroke.getKeyStroke (KeyEvent.VK_BACK_SPACE, 0), "delete");
			c.getActionMap ().put ("delete", deleteAC);
		}

		add (tb, BorderLayout.WEST);
		add (mainSP, BorderLayout.CENTER);
	}

	/**
	 * Menu bar of the editor: File (new, load, save) and, when
	 * <code>withQuit</code>, the Quit entry of a stand-alone window, plus View,
	 * which says whether the symbols of the blocks are written in the diagram.
	 */
	public JMenuBar buildMenuBar (boolean withQuit)
	{
		int			mask = java.awt.Toolkit.getDefaultToolkit ().getMenuShortcutKeyMaskEx ();
		JMenuBar	mb = new JMenuBar ();
		JMenu		mfile = new JMenu ("File");
		JMenu		mview = new JMenu ("View");
		JMenu		mhelp = new JMenu ("Help");

		mfile.add (menuItem ("New Deployment", KeyEvent.VK_N, mask, new Runnable () { public void run () { newDeployment (); } }));
		mfile.add (menuItem ("Load Deployment...", KeyEvent.VK_O, mask, new Runnable () { public void run () { loadDeployment (); } }));
		mfile.add (menuItem ("Save Deployment", KeyEvent.VK_S, mask, new Runnable () { public void run () { saveDeployment (false); } }));
		mfile.add (menuItem ("Save Deployment As...", KeyEvent.VK_S, mask | KeyEvent.SHIFT_DOWN_MASK, new Runnable () { public void run () { saveDeployment (true); } }));
		if (withQuit)
		{
			mfile.addSeparator ();
			mfile.add (menuItem ("Quit", KeyEvent.VK_Q, mask, new Runnable () { public void run () { quit (); } }));
		}
		mb.add (mfile);
		// the symbols of the blocks are written by default: they are part of the
		// drawing of an architecture, and they are left out when what is being
		// looked at is how it is wired together instead
		mview.add (check ("Show Events", canvas.getShowSymbols (), new java.awt.event.ItemListener ()
		{
			public void itemStateChanged (java.awt.event.ItemEvent e)
			{
				canvas.setShowSymbols (e.getStateChange () == java.awt.event.ItemEvent.SELECTED);
			}
		}));
		mb.add (mview);
		mhelp.add (menuItem ("Events", KeyEvent.VK_E, mask | KeyEvent.SHIFT_DOWN_MASK, new Runnable () { public void run () { showEventsHelp (); } }));
		mb.add (mhelp);
		return mb;
	}

	/** Opens the help of the events on this deployment. */
	protected void showEventsHelp ()
	{
		HelpWindow.show (this, "Events of a Deployment", eventsHelp ());
	}

	/* ------------------------------------------------------------------ */
	/* Help                                                                */
	/* ------------------------------------------------------------------ */

	/**
	 * What each symbol carries. The diagram of an architecture says which block
	 * is given what and which writes it; this says what it is.
	 */
	static private final String[][]	EVENT_HELP	=
	{
		{ "CONFIG",			"The description of the robot and of the world it runs in. The robot writes it when it starts and whenever it is asked again for it, and a module reads the properties of its robot -- its shape, its sensors, its drive -- out of it." },
		{ "EXECUTION",		"What a thread of the runtime is to do with itself: start, stop, step, reset, run on its own or be driven, and which parts of it are to be traced. It is what the Start of the simulator sends." },
		{ "SENSORS",		"The readings of the robot: its sonars, its infrared, its laser, its beacons and its odometry, as they came out of this turn." },
		{ "SENSORS_CTRL",	"Which families of sensors the robot is to read. Every family is read until something says otherwise, and the controller of the forklifts is what says it: it puts the laser out while it works inside a dock." },
		{ "LPS",			"The local perceptual space: where the robot believes it is and what it has around it, sensors fused and objects recognised. It is the one thing everything downstream of perception works on." },
		{ "MOTION",			"The speed and the turn asked of the robot, which is what a controller has to say and what the robot does with itself." },
		{ "GOAL",			"The task in hand and the place it is to be carried out at, as the planner hands it down to the rest." },
		{ "PLAN",			"The plan a robot is to carry out, which comes from outside the robot: the task sent from the simulator, or from whoever commands the fleet." },
		{ "PATH",			"The path worked out to the place of the goal, for a controller to follow." },
		{ "NAVIGATION",		"What navigation knows of the map it is working on, for perception to read its own surroundings against." },
		{ "STATUS",			"What a module has to report of itself -- idle, occupied, waiting, completed, failed -- with a line of its own. The Events table of the simulator is a log of these." },
		{ "OBJECT",			"The objects the robot has around it, as the robot itself sees them: the ones the world has moving about." },
		{ "BEHRESULT",		"Whether the behaviour a controller was running has finished, and why. A planner waits on it to go on with the plan." },
		{ "BEHINFO",		"What each behaviour of a controller is asking for, and how much it is heeded. The Behaviour Fusion window of the simulator draws these." },
		{ "BEHRULES",		"The rules the behaviours of a controller are to be fused by, sent to it from outside." },
		{ "BEHNAME",		"Which behaviour a controller is to run, named from outside." },
		{ "BEHDEBUG",		"Which behaviours of a controller are to be traced." },
		{ "CAMERA",			"What a camera watching the robots has to say of them, which is how the soccer robots are told where they are." },
		{ "DELROBOT",		"A robot has left the global space, which the space itself says when the connection of that robot drops. The rest stop counting on it." },
		{ "LINDACTRL",		"Control of the space itself rather than of a robot: how often the router sums up, what it holds, what is to be forgotten." },
		{ "GUICTRL",		"Control of the windows of a robot from outside it." },
		{ "PALLETCTRL",		"The pallet of a forklift: what it is to pick up and what it is to put down." },
		{ "COORD",			"Where each forklift of the fleet is, what it is doing and which place it has booked, so that two of them do not book the same one. It travels the global space." },
		{ "ZONE",			"The zone of the warehouse a forklift is in, as its navigation works it out." },
		{ "SYNC",			"Leave to go into a dock. The planner of a forklift says that it waits, that it has arrived or that it is leaving, and takes the answer of the warehouse. It is not registered as an event: the planner goes and reads it." },
	};

	/** The style of a page of help, in what the HTML of Swing understands of CSS. */
	static private final String		HELP_CSS	=
		"body { font-family: sans-serif; font-size: 11pt; color: #202020; margin: 12px 18px 18px 18px; }"
		+ "h1 { font-size: 17pt; color: #12324d; margin-bottom: 2px; }"
		+ "h2 { font-size: 12pt; color: #24507a; margin-top: 16px; margin-bottom: 4px; }"
		+ "p { margin-top: 3px; margin-bottom: 7px; }"
		+ "li { margin-bottom: 3px; }"
		+ ".lead { color: #555555; }"
		+ ".mono { font-family: monospaced; font-size: 10pt; color: #24507a; }"
		+ ".cls { font-family: monospaced; font-size: 9pt; color: #666666; }"
		+ ".sym { font-family: monospaced; font-size: 12pt; font-weight: bold; color: #12324d; }"
		+ ".wire { font-size: 9pt; color: #4a4a4a; }"
		+ ".none { font-size: 9pt; color: #999999; }";

	/** Escapes what goes into a page of help. */
	static private String esc (String s)
	{
		if (s == null)		return "";
		return s.replace ("&", "&amp;").replace ("<", "&lt;").replace (">", "&gt;");
	}

	/** What a symbol carries, in a line, or a line saying that nothing says. */
	static private String helpOf (String sym)
	{
		for (String[] e : EVENT_HELP)
			if (e[0].equals (sym))		return e[1];
		return "<span class=\"none\">Nothing is written down of this one yet.</span>";
	}

	/** The method an event of a block arrives at, as the deployment says it (blank when it does not). */
	private String methodOf (Block b, String sym)
	{
		for (String[] e : model.events (b))
			if (sym.equals (e[0]))				return e[2];
		for (String[] e : ArchModel.STD_EVENTS)
			if (sym.equals (e[0]))				return e[2];
		return "";
	}

	/** The blocks of this deployment that write a symbol, and the ones that are given it. */
	private String[] traffic (String sym)
	{
		StringBuilder	writes = new StringBuilder ();
		StringBuilder	reads = new StringBuilder ();

		if (ArchModel.isStandard (sym))
			reads.append ("<span class=\"none\">every module and every robot, by themselves</span>");
		for (Block b : model.allRobotBlocks ())
		{
			String	who;
			String	how;

			if (!model.hasSymbols (b))					continue;
			who		= esc (model.labelOf (b)) + " <span class=\"none\">of</span> " + esc (model.getRobotId (b.robot));
			if (model.produces (b).contains (sym))
			{
				if (writes.length () > 0)				writes.append (", ");
				writes.append (who);
			}
			if (model.inputs (b).contains (sym) && !ArchModel.isStandard (sym))
			{
				how		= methodOf (b, sym);
				if (reads.length () > 0)				reads.append (", ");
				reads.append (who);
				if (how.length () > 0)					reads.append (" <span class=\"mono\">").append (esc (how)).append ("</span>");
			}
		}
		return new String[] { writes.toString (), reads.toString () };
	}

	/** One symbol of the help: what it is, what it carries, and what this deployment does with it. */
	private void helpCard (StringBuilder h, String sym, boolean fixed)
	{
		String		item = model.itemClassOf (sym);
		String[]	t = traffic (sym);

		h.append ("<a name=\"").append (sym).append ("\"></a>");
		h.append ("<table width=\"100%\" cellpadding=\"5\" cellspacing=\"0\" bgcolor=\"").append (fixed ? "#eceff3" : "#f2f7f2").append ("\">");
		h.append ("<tr>");
		h.append ("<td width=\"64\" align=\"center\" bgcolor=\"").append (fixed ? "#5b6b7c" : "#2f7d4f").append ("\">");
		h.append ("<font color=\"#ffffff\" face=\"monospaced\" size=\"2\"><b>").append (fixed ? "FIXED" : "EVENT").append ("</b></font></td>");
		h.append ("<td><span class=\"sym\">").append (sym).append ("</span></td>");
		h.append ("<td align=\"right\"><span class=\"cls\">").append ((item.length () > 0) ? esc (item) : "&mdash;").append ("</span></td>");
		h.append ("</tr>");
		h.append ("<tr><td></td><td colspan=\"2\">").append (helpOf (sym)).append ("</td></tr>");
		h.append ("<tr><td></td><td colspan=\"2\"><span class=\"wire\"><b>written by</b> ")
		 .append ((t[0].length () > 0) ? t[0] : "<span class=\"none\">no block of this deployment</span>")
		 .append ("</span></td></tr>");
		h.append ("<tr><td></td><td colspan=\"2\"><span class=\"wire\"><b>given to</b> ")
		 .append ((t[1].length () > 0) ? t[1] : "<span class=\"none\">no block of this deployment</span>")
		 .append ("</span></td></tr>");
		h.append ("</table><br>");
	}

	/**
	 * The help of the events, as a page of HTML: how an event works, and what
	 * every symbol there is carries, with what this deployment does with it.
	 *
	 * It is written out of the deployment being edited and out of the classes of
	 * the development, so it says what is the case and not what was the case when
	 * it was written down.
	 */
	public String eventsHelp ()
	{
		StringBuilder			h = new StringBuilder ();
		java.util.List<String>	fixed = new ArrayList<String> ();
		java.util.TreeSet<String>	rest = new java.util.TreeSet<String> (model.offered ());

		// what no deployment can register: the two every thread is given, and the ones
		// a class takes by itself and nothing offers (a symbol that can be registered
		// is an event even when one class of the development polls it as well)
		for (String[] e : ArchModel.STD_EVENTS)			fixed.add (e[0]);
		for (Block b : model.allRobotBlocks ())
			for (String sym : model.wired (b))
				if (!fixed.contains (sym) && !rest.contains (sym))		fixed.add (sym);
		rest.removeAll (fixed);

		h.append ("<html><head><style>").append (HELP_CSS).append ("</style></head><body>");
		h.append ("<h1>Events of a Deployment</h1>");
		h.append ("<p class=\"lead\">A module of an architecture does not call another one: it leaves what it has in the ")
		 .append ("Linda space of its robot and is woken up by what it asked to be woken up by. An <i>event</i> is that asking.</p>");

		h.append ("<h2>How an event works</h2>");
		h.append ("<ol>");
		h.append ("<li>The deployment says, for a module, a <b>symbol</b> to be woken up by, the <b>item class</b> that symbol carries and the <b>method</b> it is to arrive at:");
		h.append ("<table cellpadding=\"6\" cellspacing=\"0\" bgcolor=\"#f6f6f6\" width=\"100%\"><tr><td><span class=\"mono\">")
		 .append ("{ \"symbol\": \"SENSORS\", \"itemClass\": \"tc.shared.linda.ItemSensors\", \"method\": \"notify_sensors\" }")
		 .append ("</span></td></tr></table></li>");
		h.append ("<li>When the robot starts, <span class=\"mono\">StdThread.setTDesc</span> registers one listener of the space for each of them, ")
		 .append ("and then the two every thread gets whether it asked for them or not.</li>");
		h.append ("<li>Whoever writes a tuple of that symbol into the space wakes the module up, and the runtime calls the method with the item as it came.</li>");
		h.append ("<li>The method has to be public and to take the space it came from and one item of that class: ")
		 .append ("<span class=\"mono\">public void notify_sensors (String space, ItemSensors item)</span>. ")
		 .append ("The editor offers the ones of the class of the module that do.</li>");
		h.append ("<li>Inside a robot the modules write with no space of their own; the router of the robot is what labels what goes out to the global space with the name of the robot, and what filters what comes back.</li>");
		h.append ("</ol>");

		h.append ("<h2>What is fixed and what is asked for</h2>");
		h.append ("<p><table cellpadding=\"4\" cellspacing=\"0\"><tr>")
		 .append ("<td bgcolor=\"#5b6b7c\" align=\"center\"><font color=\"#ffffff\" face=\"monospaced\" size=\"2\"><b>FIXED</b></font></td>")
		 .append ("<td>&nbsp;is had whatever the deployment says, and is written in bold in the diagram.</td></tr><tr>")
		 .append ("<td bgcolor=\"#2f7d4f\" align=\"center\"><font color=\"#ffffff\" face=\"monospaced\" size=\"2\"><b>EVENT</b></font></td>")
		 .append ("<td>&nbsp;is registered by the deployment, one row of the Trigger Events table.</td></tr></table></p>");
		h.append ("<p>A symbol is one kind of item, so the editor fills the class in from the symbol and does not let it be typed. ")
		 .append ("What a block writes is not asked for anywhere: it is read off the class it runs.</p>");

		h.append ("<h2>The symbols</h2>");
		for (String sym : fixed)		helpCard (h, sym, true);
		for (String sym : rest)			helpCard (h, sym, false);

		h.append ("<p class=\"none\">Written out of ").append (esc (getTitle ())).append (" and of the classes of the development.</p>");
		h.append ("</body></html>");
		return h.toString ();
	}


	private javax.swing.JCheckBoxMenuItem check (String text, boolean on, java.awt.event.ItemListener l)
	{
		javax.swing.JCheckBoxMenuItem	mi = new javax.swing.JCheckBoxMenuItem (text, on);

		mi.addItemListener (l);
		return mi;
	}

	/** Closes the window hosting the editor (the Quit entry of the menu). */
	protected void quit ()
	{
		Window	win = SwingUtilities.getWindowAncestor (this);
		if (win instanceof DeploymentWindow)		((DeploymentWindow) win).quit ();
		else if (win != null)						win.dispose ();
	}

	/* ------------------------------------------------------------------ */
	/* Edition                                                             */
	/* ------------------------------------------------------------------ */

	private JMenuItem menuItem (String name, int key, int mask, final Runnable body)
	{
		JMenuItem	mi = new JMenuItem (name);
		mi.setAccelerator (KeyStroke.getKeyStroke (key, mask));
		mi.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ body.run (); }
		});
		return mi;
	}

	/** Names of the start points (START_1, ...) of the world of a deployment; empty when it has no readable world. */
	static public List<String> startNamesOf (DeployArch d)
	{
		List<String>	names = new ArrayList<String> ();
		String			w = d.getWorldFile ();
		if (w == null)		return names;
		try
		{
			tc.shared.world.World	world = new tc.shared.world.World (w);
			for (int i = 0; i < world.n_starts (); i++)		names.add (DeployArch.startName (i));
		} catch (Exception e) { }
		return names;
	}

	/** Installs another deployment in the editor (New / Load). */
	public void setDeployment (DeployArch d)
	{
		model	= new ArchModel (d);
		normalisePaths (model);			// old files may name their resources without the leading "./"
		model.setStartNames (startNamesOf (d));
		canvas.setModel (model);
		rebuild (null);
		updateTitle ();
	}

	private void updateTitle ()
	{
		if (host != null)		host.deploymentStateChanged (this);
	}

	/** Asks what to do with unsaved changes; false when the user cancels. */
	public boolean confirmDiscard ()
	{
		if (!model.getDeploy ().isModified ())		return true;
		int		r = JOptionPane.showConfirmDialog (this, "The deployment has unsaved changes. Save them first?", TITLE,
					JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (r == JOptionPane.CANCEL_OPTION)	return false;
		if (r == JOptionPane.YES_OPTION)		return saveDeployment (false);
		return true;
	}

	private JFileChooser deployChooser (String title)
	{
		File			cur = model.getDeploy ().getFile ();
		File			dir = (cur != null) ? cur.getParentFile () : new File (DEPLOY_DIR);
		if ((dir == null) || !dir.isDirectory ())		dir = new File (".");
		JFileChooser	fc = new JFileChooser (dir);
		fc.setDialogTitle (title);
		fc.setFileFilter (new FileNameExtensionFilter ("Deployment architectures (*.deploy)", DeployArch.EXTENSION));
		return fc;
	}

	private void newDeployment ()
	{
		if (!confirmDiscard ())			return;
		setDeployment (DeployArch.create ());
	}

	private void loadDeployment ()
	{
		if (!confirmDiscard ())			return;
		JFileChooser	fc = deployChooser ("Load Deployment");
		if (fc.showOpenDialog (this) != JFileChooser.APPROVE_OPTION)		return;
		try
		{
			setDeployment (DeployArch.load (fc.getSelectedFile ()));
		} catch (Exception e)
		{
			e.printStackTrace ();
			JOptionPane.showMessageDialog (this, "Cannot load " + fc.getSelectedFile ().getName () + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
		}
	}

	public boolean saveDeployment (boolean saveAs)
	{
		DeployArch	d = model.getDeploy ();
		File		f = d.getFile ();
		if (saveAs || (f == null))
		{
			JFileChooser	fc = deployChooser (saveAs ? "Save Deployment As" : "Save Deployment");
			if (f != null)		fc.setSelectedFile (f);
			if (fc.showSaveDialog (this) != JFileChooser.APPROVE_OPTION)		return false;
			f = fc.getSelectedFile ();
			if (!f.getName ().toLowerCase ().endsWith ("." + DeployArch.EXTENSION))		f = new File (f.getPath () + "." + DeployArch.EXTENSION);
			if (f.exists () && (JOptionPane.showConfirmDialog (this, f.getName () + " already exists. Overwrite?", TITLE,
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION))		return false;
		}
		try
		{
			stopEditing ();
			d.save (f);
			updateTitle ();
			return true;
		} catch (Exception e)
		{
			e.printStackTrace ();
			JOptionPane.showMessageDialog (this, "Cannot save " + f.getName () + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	/** Robot the toolbar acts on: the one of the selection, or none (-1). */
	private int currentRobot ()
	{
		Block	sel = canvas.getSelection ();

		return ((sel != null) && (sel.robot >= 0)) ? sel.robot : -1;
	}

	/**
	 * Adds a module to the selected robot, of the kind the user picks: the kind
	 * names it and says which classes it may be, so it is asked for first rather
	 * than left to be worked out afterwards.
	 */
	private void addModule ()
	{
		int			r = currentRobot ();
		Object		type;

		if (r < 0)						return;
		type	= JOptionPane.showInputDialog (this, "Kind of module to add:", "Add Module",
											   JOptionPane.PLAIN_MESSAGE, null,
											   ArchModel.MODULE_TYPES, ArchModel.MODULE_TYPES[0]);
		if (type == null)				return;						// cancelled
		// with no class: which one of its kind it is has to be chosen, and a module
		// left blank says so when the deployment is run
		select (model.addModule (r, type.toString ()));
	}

	private void deleteSelection ()
	{
		Block	b = canvas.getSelection ();
		if ((b == null) || !model.isRemovable (b))		return;
		if (b.kind == ArchModel.ROBOT)
		{
			if (JOptionPane.showConfirmDialog (this, "Delete the robot " + model.getRobotId (b.robot) + " with all its modules?", TITLE,
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)		return;
		}
		model.remove (b);
		rebuild (null);
	}

	/** The model changed: rebuilds the tree, refreshes the diagram and selects a block. */
	private void rebuild (Block sel)
	{
		root.removeAllChildren ();
		globalNode	= new DefaultMutableTreeNode ("Global");
		root.add (globalNode);
		if (model.hasGlobalLinda ())		globalNode.add (new DefaultMutableTreeNode (new Block (ArchModel.GLOBAL_LINDA, -1)));
		for (int r : model.robots ())									// one category per robot
		{
			DefaultMutableTreeNode	robotNode = new DefaultMutableTreeNode (new Block (ArchModel.ROBOT, r));
			for (Block b : model.robotBlocks (r))		robotNode.add (new DefaultMutableTreeNode (b));
			root.add (robotNode);
		}
		treeModel.reload ();
		for (int i = 0; i < root.getChildCount (); i++)		tree.expandPath (new TreePath (((DefaultMutableTreeNode) root.getChildAt (i)).getPath ()));
		canvas.modelChanged ();
		select (sel);
		updateActions ();
		if (propsBorder != null)		updateTitle ();
	}

	static int iconOf (Block b)
	{
		switch (b.kind)
		{
		case ArchModel.GLOBAL_LINDA:
		case ArchModel.LOCAL_LINDA:	return ToolIcon.LINDA;
		case ArchModel.ROUTER:		return ToolIcon.ROUTER;
		case ArchModel.VROBOT:
		case ArchModel.ROBOT:		return ToolIcon.ROBOT;
		default:					return ToolIcon.MODULE;
		}
	}

	/** Selects a block everywhere (diagram, tree, property editor). */
	private void select (Block b)
	{
		if ((b != null) && (b.kind == ArchModel.ROBOT) && !model.hasRobot (b.robot))	b = null;
		if ((b != null) && (treeNode (b) == null))
		{
			if (!canvas.blockExists (b))		b = null;			// cannot be shown: nothing selected
			else								{ rebuild (b); return; }	// the tree is stale
		}
		canvas.setSelection (b);
		blockSelected (b);
	}

	private DefaultMutableTreeNode treeNode (Block b)
	{
		if (b == null)					return null;
		for (java.util.Enumeration<?> e = root.depthFirstEnumeration (); e.hasMoreElements (); )
		{
			DefaultMutableTreeNode	n = (DefaultMutableTreeNode) e.nextElement ();
			if (b.equals (n.getUserObject ()))		return n;
		}
		return null;
	}

	private void updateActions ()
	{
		Block	sel = canvas.getSelection ();
		int		r = currentRobot ();						// the robot of the selection, and nothing without one
		lindaAC.setEnabled (!model.hasGlobalLinda ());				// local spaces come with the robot
		// a router and a module belong to a robot: with none selected there is
		// nothing to add them to
		routerAC.setEnabled ((r >= 0) && !model.hasRouter (r));
		moduleAC.setEnabled (r >= 0);
		robotAC.setEnabled (true);
		deleteAC.setEnabled ((sel != null) && model.isRemovable (sel));
	}

	/** A name was edited in place on the diagram (robot name or module INFO): tree and properties follow. */
	public void blockRenamed (Block b)
	{
		rebuild (b);
	}

	/* --- selection synchronisation --- */

	public void blockSelected (Block b)
	{
		if (syncing)					return;
		syncing = true;
		try
		{
			DefaultMutableTreeNode	n = treeNode (b);
			if (n != null)
			{
				TreePath	p = new TreePath (n.getPath ());
				tree.setSelectionPath (p);
				tree.scrollPathToVisible (p);
			}
			else	tree.clearSelection ();
			showProperties (b);
		}
		finally { syncing = false; }
		updateActions ();
	}

	public void blockActivated (Block b)
	{
		if (propsModel.getRowCount () > 0)
		{
			propsTB.requestFocusInWindow ();
			propsTB.changeSelection (0, 1, false, false);
		}
	}

	private void treeSelected ()
	{
		if (syncing)					return;
		TreePath	p = tree.getSelectionPath ();
		Object		o = (p == null) ? null : ((DefaultMutableTreeNode) p.getLastPathComponent ()).getUserObject ();
		Block		b = (o instanceof Block) ? (Block) o : null;
		syncing = true;
		try
		{
			canvas.setSelection (b);
			showProperties (b);
		}
		finally { syncing = false; }
		updateActions ();
	}

	private void setPropsTitle (String t)
	{
		propsBorder.setTitle (t);
		((JComponent) propsTB.getParent ().getParent ().getParent ()).repaint ();
	}

	private void showProperties (Block b)
	{
		if (propsTB.isEditing ())		propsTB.getCellEditor ().stopCellEditing ();
		propsModel.setBlock (b);
		if (eventsTB.isEditing ())		eventsTB.getCellEditor ().stopCellEditing ();
		eventsModel.setBlock (b);
		symbolCB.removeAllItems ();
		for (String sym : model.offered ())		symbolCB.addItem (sym);
		updateEventButtons ();
		if (b == null)							setPropsTitle (" ");
		else if (b.kind == ArchModel.ROBOT)		setPropsTitle ("Robot " + model.getRobotId (b.robot));
		else if ((b.kind == ArchModel.MODULE) || (b.kind == ArchModel.ROUTER) || (b.kind == ArchModel.VROBOT))
												setPropsTitle (ArchModel.KIND_NAMES[b.kind] + ": " + model.labelOf (b));
		else									setPropsTitle (model.labelOf (b));
	}

	public ArchModel getModel ()	{ return model; }
}

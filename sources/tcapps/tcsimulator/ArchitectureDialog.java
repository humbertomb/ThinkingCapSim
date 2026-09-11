/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tcsimulator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
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

import tcapps.tceditor.FileCellEditor;
import tcapps.tceditor.ToolButtons;
import tcapps.tceditor.ToolIcon;
import tcapps.tcsimulator.arch.ArchCanvas;
import tcapps.tcsimulator.arch.ArchModel;
import tcapps.tcsimulator.arch.ArchModel.Block;
import tcapps.tcsimulator.arch.ArchModel.Property;

/**
 * Block editor of an architecture (.arch): a toolbar on the left adds Linda
 * spaces, routers, modules and the robot; the centre shows the block diagram
 * ({@link ArchCanvas}); on the right, like in the world editor, a tree with
 * the "Global" category (the global Linda space) and one category per robot
 * with its modules, and below it the property editor of the selected block.
 * The dialog edits a copy of the properties; {@link #showDialog()} returns
 * them when accepted, or null when cancelled.
 */
public class ArchitectureDialog extends JDialog implements ArchCanvas.Listener
{
	private static final long		serialVersionUID = 1L;

	protected ArchModel				model;
	protected ArchCanvas			canvas;
	protected JTree					tree;
	protected DefaultTreeModel		treeModel;
	protected DefaultMutableTreeNode	root, globalNode, robotNode;
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
	protected JButton				okBT, cancelBT;
	protected Properties			result;
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
			return (c == 0) ? p.label : model.get (block, p.key);
		}

		public void setValueAt (Object v, int r, int c)
		{
			Property	p = rows.get (r);
			model.set (block, p.key, (v == null) ? "" : v.toString ());
			fireTableCellUpdated (r, c);
			if (p.key.equals ("INFO"))		rebuild (block);				// the label of the block changed
			else							canvas.repaint ();
		}
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
		public boolean isCellEditable (int r, int c)	{ return (block != null) && model.hasEvents (block); }

		public void setValueAt (Object v, int r, int c)
		{
			rows.get (r)[c] = (v == null) ? "" : v.toString ().trim ();
			fireTableCellUpdated (r, c);
			model.setEvents (block, rows);
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
		}
	}

	private JPanel buildEventsPanel ()
	{
		eventsModel	= new EventsModel ();
		eventsTB	= new JTable (eventsModel);
		eventsTB.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		eventsTB.setRowHeight (20);
		eventsTB.putClientProperty ("terminateEditOnFocusLost", Boolean.TRUE);
		eventsTB.getColumnModel ().getColumn (0).setPreferredWidth (70);
		symbolCB	= new JComboBox<String> ();
		symbolCB.setEditable (true);										// new symbols can still be typed
		eventsTB.getColumnModel ().getColumn (0).setCellEditor (new DefaultCellEditor (symbolCB));
		eventsTB.getColumnModel ().getColumn (1).setPreferredWidth (150);
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
		pn.setBorder (BorderFactory.createTitledBorder ("Events"));
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
	 * @param props    properties of the architecture (copied, not modified)
	 * @param robotId  name of the robot (category of the tree, e.g. IFORK-1)
	 */
	public ArchitectureDialog (Frame owner, Properties props, String robotId)
	{
		super (owner, "Architecture Editor", true);
		model	= new ArchModel ((Properties) props.clone (), robotId);
		buildGUI ();
		rebuild (null);
		pack ();
		setMinimumSize (new Dimension (760, 520));
		setSize (1040, 780);
		setLocationRelativeTo (owner);
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
		lindaAC		= ToolButtons.action ("Linda", ToolIcon.LINDA, "Add a Linda space (global first, then the local one of the robot)", new Runnable () { public void run () { addLinda (); } });
		routerAC	= ToolButtons.action ("Router", ToolIcon.ROUTER, "Add the Linda router of the robot", new Runnable () { public void run () { select (model.addRouter ()); } });
		moduleAC	= ToolButtons.action ("Module", ToolIcon.MODULE, "Add a module to the robot", new Runnable () { public void run () { select (model.addModule ()); } });
		robotAC		= ToolButtons.action ("Robot", ToolIcon.ROBOT, "Add the robot (local Linda space and virtual robot)", new Runnable () { public void run () { select (model.addRobot ()); } });
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
					if (b.kind == ArchModel.ROBOT)		setText (model.getRobotId ());
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
					case ArchModel.P_CHOICE:
					case ArchModel.P_FILE:
					{
						String			id = ArchModel.KIND_NAMES[propsModel.block.kind] + "/" + p.key;
						TableCellEditor	ed = editors.get (id);
						if (ed == null)
						{
							if (p.type == ArchModel.P_CHOICE)	ed = new DefaultCellEditor (new JComboBox<String> (p.choices));
							else								ed = new FileCellEditor ("Select " + p.label, p.fileDir, new FileNameExtensionFilter (p.fileDesc, p.fileExts), false);
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
		// the divider positions are only meaningful once the dialog has its real size
		addComponentListener (new java.awt.event.ComponentAdapter ()
		{
			public void componentShown (java.awt.event.ComponentEvent e)		{ resetDividers (); }
			public void componentResized (java.awt.event.ComponentEvent e)	{ if (!dividersSet && (getWidth () > 0)) resetDividers (); }
		});

		// --- bottom: cancel / ok
		cancelBT	= new JButton ("Cancel");
		okBT		= new JButton ("OK");
		cancelBT.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ result = null; dispose (); }
		});
		okBT.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ accept (); }
		});
		JPanel		bottom = new JPanel (new FlowLayout (FlowLayout.RIGHT, 6, 6));
		bottom.add (cancelBT);
		bottom.add (okBT);
		getRootPane ().setDefaultButton (okBT);
		getRootPane ().getInputMap (JComponent.WHEN_IN_FOCUSED_WINDOW).put (KeyStroke.getKeyStroke (KeyEvent.VK_ESCAPE, 0), "cancel");
		getRootPane ().getActionMap ().put ("cancel", new javax.swing.AbstractAction ()
		{
			private static final long	serialVersionUID = 1L;
			public void actionPerformed (ActionEvent e)		{ result = null; dispose (); }
		});
		for (JComponent c : new JComponent[] { canvas, tree })
		{
			c.getInputMap (JComponent.WHEN_FOCUSED).put (KeyStroke.getKeyStroke (KeyEvent.VK_DELETE, 0), "delete");
			c.getInputMap (JComponent.WHEN_FOCUSED).put (KeyStroke.getKeyStroke (KeyEvent.VK_BACK_SPACE, 0), "delete");
			c.getActionMap ().put ("delete", deleteAC);
		}

		JPanel		content = new JPanel (new BorderLayout ());
		content.add (tb, BorderLayout.WEST);
		content.add (mainSP, BorderLayout.CENTER);
		content.add (bottom, BorderLayout.SOUTH);
		setContentPane (content);
	}

	/* ------------------------------------------------------------------ */
	/* Edition                                                             */
	/* ------------------------------------------------------------------ */

	/** Linda button: the global space when missing, otherwise the local one of the robot. */
	private void addLinda ()
	{
		if (!model.hasGlobalLinda ())		select (model.addGlobalLinda ());
		else if (!model.hasLocalLinda ())	select (model.addLocalLinda ());
	}

	private void deleteSelection ()
	{
		Block	b = canvas.getSelection ();
		if (b == null)					return;
		if (b.kind == ArchModel.ROBOT)
		{
			if (JOptionPane.showConfirmDialog (this, "Delete the robot " + model.getRobotId () + " with all its modules?", getTitle (),
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
		if (model.hasGlobalLinda ())		globalNode.add (new DefaultMutableTreeNode (new Block (ArchModel.GLOBAL_LINDA, "GLIN")));
		robotNode	= null;
		if (model.hasRobot ())
		{
			robotNode	= new DefaultMutableTreeNode (new Block (ArchModel.ROBOT, null));
			for (Block b : model.robotBlocks ())		robotNode.add (new DefaultMutableTreeNode (b));
			root.add (robotNode);
		}
		treeModel.reload ();
		for (int i = 0; i < root.getChildCount (); i++)		tree.expandPath (new TreePath (((DefaultMutableTreeNode) root.getChildAt (i)).getPath ()));
		canvas.modelChanged ();
		select (sel);
		updateActions ();
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
		if ((b != null) && (b.kind == ArchModel.ROBOT) && !model.hasRobot ())	b = null;
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
		lindaAC.setEnabled (!model.hasGlobalLinda () || !model.hasLocalLinda ());
		routerAC.setEnabled (!model.hasRouter ());
		robotAC.setEnabled (!model.hasVRobot ());				// only one robot for now
		deleteAC.setEnabled (canvas.getSelection () != null);
	}

	/** Asks for a new name of the robot (double click on its name in the diagram). */
	private void renameRobot ()
	{
		String	name = (String) JOptionPane.showInputDialog (this, "Robot name:", getTitle (), JOptionPane.PLAIN_MESSAGE, null, null, model.getRobotId ());
		if ((name == null) || (name.trim ().length () == 0) || name.trim ().equals (model.getRobotId ()))		return;
		model.setRobotName (name);
		rebuild (new Block (ArchModel.ROBOT, null));
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
		if (b.kind == ArchModel.ROBOT)
		{
			renameRobot ();
			return;
		}
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
		for (String sym : model.symbols ())		symbolCB.addItem (sym);
		updateEventButtons ();
		if (b == null)							setPropsTitle (" ");
		else if (b.kind == ArchModel.ROBOT)		setPropsTitle ("Robot " + model.getRobotId ());
		else if ((b.kind == ArchModel.MODULE) || (b.kind == ArchModel.ROUTER) || (b.kind == ArchModel.VROBOT))
												setPropsTitle (ArchModel.KIND_NAMES[b.kind] + ": " + model.labelOf (b));
		else									setPropsTitle (model.labelOf (b));
	}

	/* ------------------------------------------------------------------ */
	/* Result                                                              */
	/* ------------------------------------------------------------------ */

	private void accept ()
	{
		if (propsTB.isEditing ())		propsTB.getCellEditor ().stopCellEditing ();
		if (eventsTB.isEditing ())		eventsTB.getCellEditor ().stopCellEditing ();
		result	= model.getProperties ();
		dispose ();
	}

	public ArchModel getModel ()	{ return model; }

	/** Shows the dialog; returns the edited properties, or null when cancelled. */
	public Properties showDialog ()
	{
		result = null;
		setVisible (true);
		return result;
	}
}

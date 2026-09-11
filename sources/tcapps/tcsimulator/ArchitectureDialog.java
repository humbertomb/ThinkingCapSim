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
import java.util.List;
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
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import tcapps.tceditor.ToolButtons;
import tcapps.tceditor.ToolIcon;
import tcapps.tcsimulator.arch.ArchCanvas;
import tcapps.tcsimulator.arch.ArchModel;
import tcapps.tcsimulator.arch.ArchModel.Block;

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

	static public final String		PREFIX_ROW	= "Prefix";

	protected ArchModel				model;
	protected ArchCanvas			canvas;
	protected JTree					tree;
	protected DefaultTreeModel		treeModel;
	protected DefaultMutableTreeNode	root, globalNode, robotNode;
	protected JTable				propsTB;
	protected PropsModel			propsModel;
	protected JLabel				propsTitle;
	protected Action				lindaAC, routerAC, moduleAC, robotAC, deleteAC;
	protected JButton				okBT, cancelBT;
	protected Properties			result;
	protected boolean				syncing;				// tree <-> canvas selection in progress

	/** Rows of the property editor: the prefix (when renameable) plus the keys of the block. */
	protected class PropsModel extends AbstractTableModel
	{
		private static final long	serialVersionUID = 1L;
		Block			block;
		List<String>	keys	= new ArrayList<String> ();

		void setBlock (Block b)
		{
			block	= b;
			keys.clear ();
			if (b != null)
			{
				if ((b.kind == ArchModel.ROUTER) || (b.kind == ArchModel.MODULE) || (b.kind == ArchModel.VROBOT))		keys.add (PREFIX_ROW);
				keys.addAll (model.keysOf (b));
			}
			fireTableDataChanged ();
		}

		public int getRowCount ()				{ return keys.size (); }
		public int getColumnCount ()			{ return 2; }
		public String getColumnName (int c)		{ return (c == 0) ? "Property" : "Value"; }
		public boolean isCellEditable (int r, int c)	{ return c == 1; }

		public Object getValueAt (int r, int c)
		{
			String	k = keys.get (r);
			if (c == 0)						return k;
			if (k.equals (PREFIX_ROW))		return block.prefix;
			return model.get (block, k);
		}

		public void setValueAt (Object v, int r, int c)
		{
			String	k = keys.get (r);
			String	s = (v == null) ? "" : v.toString ();
			if (k.equals (PREFIX_ROW))
			{
				Block	nb = model.rename (block, s);
				if (nb.equals (block))
				{
					if (!s.trim ().toUpperCase ().equals (block.prefix))
						JOptionPane.showMessageDialog (ArchitectureDialog.this, "The prefix '" + s.trim ().toUpperCase () + "' is not valid or is already in use.", getTitle (), JOptionPane.WARNING_MESSAGE);
					return;
				}
				block = nb;
				rebuild (nb);
				return;
			}
			model.set (block, k, s);
			fireTableCellUpdated (r, c);
			if (k.equals ("INFO"))		rebuild (block);				// the label of the block changed
			else						canvas.repaint ();
		}
	}

	/**
	 * @param props    properties of the architecture (copied, not modified)
	 * @param robotId  name of the robot (category of the tree, e.g. IFORK-1)
	 */
	public ArchitectureDialog (Frame owner, Properties props, String robotId)
	{
		super (owner, "Architecture", true);
		model	= new ArchModel ((Properties) props.clone (), robotId);
		buildGUI ();
		rebuild (null);
		pack ();
		setMinimumSize (new Dimension (760, 520));
		setSize (980, 680);
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
		tb.add (ToolButtons.flatButton (routerAC));
		tb.add (ToolButtons.flatButton (moduleAC));
		tb.add (ToolButtons.flatButton (robotAC));
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
					else								setText (model.labelOf (b) + (((b.prefix != null) && !b.prefix.equals ("GLIN") && !b.prefix.equals ("LLIN")) ? "  [" + b.prefix + "]" : ""));
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
		treeSP.setPreferredSize (new Dimension (260, 260));

		propsModel	= new PropsModel ();
		propsTB		= new JTable (propsModel);
		propsTB.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		propsTB.setRowHeight (20);
		propsTB.putClientProperty ("terminateEditOnFocusLost", Boolean.TRUE);
		propsTB.getColumnModel ().getColumn (0).setPreferredWidth (90);
		propsTB.getColumnModel ().getColumn (1).setPreferredWidth (170);
		JScrollPane	propsSP = new JScrollPane (propsTB);
		propsSP.setPreferredSize (new Dimension (260, 240));
		propsTitle	= new JLabel (" ");
		propsTitle.setBorder (BorderFactory.createEmptyBorder (4, 4, 2, 4));
		JPanel		propsPN = new JPanel (new BorderLayout ());
		propsPN.add (propsTitle, BorderLayout.NORTH);
		propsPN.add (propsSP, BorderLayout.CENTER);

		JSplitPane	right = new JSplitPane (JSplitPane.VERTICAL_SPLIT, treeSP, propsPN);
		right.setResizeWeight (0.5);
		right.setBorder (BorderFactory.createEmptyBorder ());

		JSplitPane	main = new JSplitPane (JSplitPane.HORIZONTAL_SPLIT, canvasSP, right);
		main.setResizeWeight (1.0);
		main.setBorder (BorderFactory.createEmptyBorder ());

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
		content.add (main, BorderLayout.CENTER);
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

	private void showProperties (Block b)
	{
		if (propsTB.isEditing ())		propsTB.getCellEditor ().stopCellEditing ();
		propsModel.setBlock (b);
		if (b == null)							propsTitle.setText (" ");
		else if (b.kind == ArchModel.ROBOT)		propsTitle.setText ("Robot " + model.getRobotId ());
		else									propsTitle.setText (ArchModel.KIND_NAMES[b.kind] + ((b.prefix != null) ? "  [" + b.prefix + "]" : ""));
	}

	/* ------------------------------------------------------------------ */
	/* Result                                                              */
	/* ------------------------------------------------------------------ */

	private void accept ()
	{
		if (propsTB.isEditing ())		propsTB.getCellEditor ().stopCellEditing ();
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

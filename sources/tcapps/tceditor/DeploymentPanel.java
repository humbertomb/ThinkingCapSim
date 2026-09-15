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
	static public final String		DEPLOY_DIR	= "./conf/deploy";		// deployment architectures (.deploy)
	static public final String		ARCHS_DIR	= "./conf/archs";		// legacy .arch files (import)

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
		moduleAC	= ToolButtons.action ("Module", ToolIcon.MODULE, "Add a module to the selected robot", new Runnable () { public void run () { select (model.addModule (currentRobot ())); } });
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
					case ArchModel.P_CHOICE:
					case ArchModel.P_FILE:
					{
						String			id = ArchModel.KIND_NAMES[propsModel.block.kind] + "/" + p.key + ((p.choices != null) ? "/" + String.join ("|", p.choices) : "");
						TableCellEditor	ed = editors.get (id);
						if (ed == null)
						{
							if (p.type == ArchModel.P_CHOICE)	ed = new DefaultCellEditor (new JComboBox<String> (p.choices));
							else								ed = new FileCellEditor ("Select " + p.label, p.fileDir, new FileNameExtensionFilter (p.fileDesc, p.fileExts));
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
	 * Menu bar of the editor: File (new, load, save, import of a legacy
	 * execution architecture) and, when <code>withQuit</code>, the Quit entry
	 * of a stand-alone window.
	 */
	public JMenuBar buildMenuBar (boolean withQuit)
	{
		int			mask = java.awt.Toolkit.getDefaultToolkit ().getMenuShortcutKeyMaskEx ();
		JMenuBar	mb = new JMenuBar ();
		JMenu		mfile = new JMenu ("File");

		mfile.add (menuItem ("New Deployment", KeyEvent.VK_N, mask, new Runnable () { public void run () { newDeployment (); } }));
		mfile.add (menuItem ("Load Deployment...", KeyEvent.VK_O, mask, new Runnable () { public void run () { loadDeployment (); } }));
		mfile.add (menuItem ("Save Deployment", KeyEvent.VK_S, mask, new Runnable () { public void run () { saveDeployment (false); } }));
		mfile.add (menuItem ("Save Deployment As...", KeyEvent.VK_S, mask | KeyEvent.SHIFT_DOWN_MASK, new Runnable () { public void run () { saveDeployment (true); } }));
		mfile.addSeparator ();
		mfile.add (menuItem ("Import Execution...", KeyEvent.VK_I, mask, new Runnable () { public void run () { importExecutionArchitecture (); } }));
		if (withQuit)
		{
			mfile.addSeparator ();
			mfile.add (menuItem ("Quit", KeyEvent.VK_Q, mask, new Runnable () { public void run () { quit (); } }));
		}
		mb.add (mfile);
		return mb;
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

	/**
	 * Imports an execution architecture (.arch) as a new robot of the
	 * deployment: its local Linda space, router, modules and virtual robot.
	 * The global Linda space of the file is discarded (the deployment keeps
	 * its own, if any).
	 */
	private void importExecutionArchitecture ()
	{
		File		dir = new File (ARCHS_DIR);
		JFileChooser	fc = new JFileChooser (dir.isDirectory () ? dir : new File ("."));
		fc.setDialogTitle ("Import Execution");
		fc.setFileFilter (new FileNameExtensionFilter ("Architecture definition files (*.arch)", "arch"));
		if (fc.showOpenDialog (this) != JFileChooser.APPROVE_OPTION)		return;
		try
		{
			DeployArch	imported = DeployArch.importArch (fc.getSelectedFile ());
			if (imported.robots.isEmpty ())		return;
			select (model.addRobot (imported.robots.get (0)));
		} catch (Exception e)
		{
			e.printStackTrace ();
			JOptionPane.showMessageDialog (this, "Cannot import " + fc.getSelectedFile ().getName () + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Robot the toolbar acts on: the one of the selection, or the first one (created when there is none). */
	private int currentRobot ()
	{
		Block	sel = canvas.getSelection ();
		if ((sel != null) && (sel.robot >= 0))		return sel.robot;
		List<Integer>	robots = model.robots ();
		if (robots.size () > 0)		return robots.get (0);
		return model.addRobot ().robot;
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
		int		r = ((sel != null) && (sel.robot >= 0)) ? sel.robot : (model.robots ().size () > 0 ? model.robots ().get (0) : -1);
		lindaAC.setEnabled (!model.hasGlobalLinda ());				// local spaces come with the robot
		routerAC.setEnabled ((r < 0) || !model.hasRouter (r));
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
		for (String sym : model.symbols ())		symbolCB.addItem (sym);
		updateEventButtons ();
		if (b == null)							setPropsTitle (" ");
		else if (b.kind == ArchModel.ROBOT)		setPropsTitle ("Robot " + model.getRobotId (b.robot));
		else if ((b.kind == ArchModel.MODULE) || (b.kind == ArchModel.ROUTER) || (b.kind == ArchModel.VROBOT))
												setPropsTitle (ArchModel.KIND_NAMES[b.kind] + ": " + model.labelOf (b));
		else									setPropsTitle (model.labelOf (b));
	}

	public ArchModel getModel ()	{ return model; }
}

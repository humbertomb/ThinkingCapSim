/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;

import tc.shared.world.World;
import tclib.planning.htopol.GNodeFL;
import tclib.planning.htopol.GNodeSL;
import tclib.planning.htopol.HTopolMap;
import tclib.utils.graphs.GNode;
import tclib.utils.graphs.Graph;

/**
 * Editor of the hierarchical topological map of a world ({@link World#topology}, which must exist).
 * The toolbar on the left holds the tools (select, node, arc, delete, open
 * level); the centre is a tabbed pane with one tab per level: <i>Root</i>
 * (the zones of the world) plus one tab, named after the node, for every
 * node whose level is being edited; the properties of the selected node or
 * arc are edited on the right. The map is edited in place; Cancel restores
 * the state it had when the dialog was opened.
 */
public class TopolEditorDialog extends JDialog implements TopolCanvas.Listener
{
	private static final long		serialVersionUID = 1L;

	static public final String		TITLE			= "Topology Editor";
	static public final String		ROOT_TAB		= "Root";
	static public final int			RIGHT_WIDTH		= 300;

	protected World					world;
	protected String				snapshot;				// topology JSON when the dialog was opened
	protected boolean				accepted;
	protected boolean				modified;

	protected JTabbedPane			tabs;
	protected List<TopolCanvas>		canvases	= new ArrayList<TopolCanvas> ();
	protected JToggleButton[]		toolButtons	= new JToggleButton[3];
	protected JButton				deleteBT, openBT;
	protected JTable				propTable;
	protected PropertyModel			propModel;
	protected JPanel				propPanel;
	protected StatusBar				statusBar;

	public TopolEditorDialog (java.awt.Window owner, World world)
	{
		super (owner, TITLE, ModalityType.APPLICATION_MODAL);
		this.world		= world;
		this.snapshot	= tc.shared.world.WorldJson.toText (world.topology ().toJson ());

		buildGUI ();
		setSize (1040, 720);
		setMinimumSize (new Dimension (700, 480));
		setLocationRelativeTo (owner);
		setDefaultCloseOperation (DO_NOTHING_ON_CLOSE);
		addWindowListener (new java.awt.event.WindowAdapter ()
		{
			public void windowClosing (java.awt.event.WindowEvent e)		{ cancel (); }
		});
	}

	/** Shows the dialog; true when the edition was accepted (the topology of the world is already updated). */
	public boolean showDialog ()
	{
		setVisible (true);
		return accepted;
	}

	public boolean isModified ()			{ return modified; }

	/* ------------------------------------------------------------------ */
	/* GUI                                                                 */
	/* ------------------------------------------------------------------ */

	private void buildGUI ()
	{
		JPanel	content = new JPanel (new BorderLayout ());
		setContentPane (content);

		content.add (buildToolBar (), BorderLayout.WEST);

		// --- centre: one tab per level
		tabs	= new JTabbedPane ();
		tabs.addChangeListener (new javax.swing.event.ChangeListener ()
		{
			public void stateChanged (javax.swing.event.ChangeEvent e)		{ tabChanged (); }
		});
		addTab (ROOT_TAB, new TopolCanvas (world, world.topology (), null, null, this), false);

		// --- right: properties of the selection
		propModel	= new PropertyModel ();
		propTable	= new JTable (propModel)
		{
			private static final long	serialVersionUID = 1L;
			public TableCellEditor getCellEditor (int row, int column)
			{
				if ((column == 1) && propModel.isDoorRow (row))
					return new DefaultCellEditor (new JComboBox<String> (connectorLabels ()));
				return super.getCellEditor (row, column);
			}
		};
		propTable.setRowHeight (22);
		propTable.putClientProperty ("terminateEditOnFocusLost", Boolean.TRUE);
		propPanel	= new JPanel (new BorderLayout ());
		propPanel.setBorder (BorderFactory.createTitledBorder ("Properties"));
		propPanel.add (new JScrollPane (propTable), BorderLayout.CENTER);
		propPanel.setPreferredSize (new Dimension (RIGHT_WIDTH, 400));

		JSplitPane	split = new JSplitPane (JSplitPane.HORIZONTAL_SPLIT, tabs, propPanel);
		split.setResizeWeight (1.0);
		split.setOneTouchExpandable (true);
		content.add (split, BorderLayout.CENTER);

		// --- bottom: status bar and buttons
		statusBar	= new StatusBar ();
		JButton		okBT = new JButton ("OK"), cancelBT = new JButton ("Cancel");
		okBT.addActionListener (new ActionListener () { public void actionPerformed (ActionEvent e) { accept (); } });
		cancelBT.addActionListener (new ActionListener () { public void actionPerformed (ActionEvent e) { cancel (); } });
		JPanel		buttons = new JPanel (new FlowLayout (FlowLayout.RIGHT, 8, 4));
		buttons.add (cancelBT);
		buttons.add (okBT);
		JPanel		bottom = new JPanel (new BorderLayout ());
		bottom.add (statusBar, BorderLayout.CENTER);
		bottom.add (buttons, BorderLayout.EAST);
		content.add (bottom, BorderLayout.SOUTH);
		getRootPane ().setDefaultButton (okBT);

		// --- keys
		JComponent	root = getRootPane ();
		root.getInputMap (JComponent.WHEN_IN_FOCUSED_WINDOW).put (KeyStroke.getKeyStroke (KeyEvent.VK_ESCAPE, 0), "escape");
		root.getActionMap ().put ("escape", new AbstractAction ()
		{
			private static final long	serialVersionUID = 1L;
			public void actionPerformed (ActionEvent e)
			{
				TopolCanvas	c = current ();
				if ((c != null) && (c.getTool () != TopolCanvas.T_SELECT))		selectTool (TopolCanvas.T_SELECT);
				else if (propTable.isEditing ())									propTable.getCellEditor ().cancelCellEditing ();
			}
		});
		root.getInputMap (JComponent.WHEN_IN_FOCUSED_WINDOW).put (KeyStroke.getKeyStroke (KeyEvent.VK_DELETE, 0), "delete");
		root.getInputMap (JComponent.WHEN_IN_FOCUSED_WINDOW).put (KeyStroke.getKeyStroke (KeyEvent.VK_BACK_SPACE, 0), "delete");
		root.getActionMap ().put ("delete", new AbstractAction ()
		{
			private static final long	serialVersionUID = 1L;
			public void actionPerformed (ActionEvent e)		{ if (!propTable.isEditing ()) deleteSelection (); }
		});

		selectTool (TopolCanvas.T_SELECT);
		tabChanged ();
	}

	private JToolBar buildToolBar ()
	{
		JToolBar	tb = new JToolBar (JToolBar.VERTICAL);
		tb.setFloatable (false);
		tb.setBorder (BorderFactory.createEmptyBorder (4, 2, 4, 2));
		ButtonGroup	group = new ButtonGroup ();

		int[]		icons = { ToolIcon.SELECT, ToolIcon.NODE, ToolIcon.ARC };
		String[]	tips = { "Select  [S]", "Add node  [N]", "Add arc  [A]" };
		int[]		keys = { KeyEvent.VK_S, KeyEvent.VK_N, KeyEvent.VK_A };
		for (int i = 0; i < 3; i++)
		{
			final int	tool = i;
			toolButtons[i] = ToolButtons.flatToggle (new ToolIcon (icons[i]), tips[i]);
			toolButtons[i].addActionListener (new ActionListener () { public void actionPerformed (ActionEvent e) { selectTool (tool); } });
			group.add (toolButtons[i]);
			tb.add (toolButtons[i]);
			getRootPane ().getInputMap (JComponent.WHEN_IN_FOCUSED_WINDOW).put (KeyStroke.getKeyStroke (keys[i], 0), "tool" + i);
			getRootPane ().getActionMap ().put ("tool" + i, new AbstractAction ()
			{
				private static final long	serialVersionUID = 1L;
				public void actionPerformed (ActionEvent e)		{ if (!propTable.isEditing ()) selectTool (tool); }
			});
		}
		tb.addSeparator ();
		deleteBT	= ToolButtons.flatButton (ToolButtons.action ("Delete", ToolIcon.DELETE, "Delete the selected node or arc  [Del]", new Runnable () { public void run () { deleteSelection (); } }));
		openBT		= ToolButtons.flatButton (ToolButtons.action ("Open level", ToolIcon.SUBGRAPH, "Edit the level of the selected node (opens a tab)", new Runnable () { public void run () { openSelected (); } }));
		tb.add (deleteBT);
		tb.add (openBT);
		tb.addSeparator ();
		tb.add (ToolButtons.flatButton (ToolButtons.action ("Zoom to fit", ToolIcon.ZOOM_FIT, "Zoom to fit", new Runnable () { public void run () { TopolCanvas c = current (); if (c != null) c.zoomToFit (); } })));
		tb.add (ToolButtons.flatButton (ToolButtons.action ("Zoom in", ToolIcon.ZOOM_IN, "Zoom in", new Runnable () { public void run () { TopolCanvas c = current (); if (c != null) c.zoom (1.25); } })));
		tb.add (ToolButtons.flatButton (ToolButtons.action ("Zoom out", ToolIcon.ZOOM_OUT, "Zoom out", new Runnable () { public void run () { TopolCanvas c = current (); if (c != null) c.zoom (0.8); } })));
		tb.add (Box.createVerticalGlue ());
		return tb;
	}

	/* ------------------------------------------------------------------ */
	/* Tabs                                                                */
	/* ------------------------------------------------------------------ */

	protected TopolCanvas current ()
	{
		int	i = tabs.getSelectedIndex ();
		return ((i >= 0) && (i < canvases.size ())) ? canvases.get (i) : null;
	}

	private void addTab (String title, final TopolCanvas canvas, boolean closable)
	{
		canvases.add (canvas);
		tabs.addTab (title, canvas);
		int		index = tabs.getTabCount () - 1;
		if (closable)
		{
			// tab header with a small close button
			JPanel	header = new JPanel (new FlowLayout (FlowLayout.LEFT, 4, 0));
			header.setOpaque (false);
			header.add (new JLabel (title));
			JLabel	close = new JLabel ("×");
			close.setToolTipText ("Close this level");
			close.setBorder (BorderFactory.createEmptyBorder (0, 4, 0, 0));
			close.addMouseListener (new MouseAdapter ()
			{
				public void mouseClicked (MouseEvent e)		{ closeTab (canvas); }
			});
			header.add (close);
			tabs.setTabComponentAt (index, header);
		}
		tabs.setSelectedIndex (index);
	}

	/** Opens (or selects) the tab of the level owned by a node. */
	protected void openNodeTab (TopolCanvas from, GNode node)
	{
		for (int i = 0; i < canvases.size (); i++)
			if (canvases.get (i).getOwner () == node)		{ tabs.setSelectedIndex (i); return; }

		Graph	graph;
		String	zone;
		if (node instanceof GNodeFL)			{ graph = ((GNodeFL) node).getGraph (); zone = node.getLabel (); }
		else if (node instanceof GNodeSL)		{ graph = ((GNodeSL) node).createGraph (); zone = from.getZone (); }
		else									return;
		addTab (node.getLabel (), new TopolCanvas (world, graph, node, zone, this), true);
	}

	/** Closes a tab and those of the levels below it. */
	protected void closeTab (TopolCanvas canvas)
	{
		int	i = canvases.indexOf (canvas);
		if (i <= 0)		return;										// Root stays
		// levels opened from this one own nodes of its graph (or of graphs below): close them first
		for (int j = canvases.size () - 1; j > i; j--)
			if (isBelow (canvases.get (j), canvas))		closeTab (canvases.get (j));
		i = canvases.indexOf (canvas);
		canvases.remove (i);
		tabs.removeTabAt (i);
	}

	/** True when the level of a canvas hangs (directly or not) from the level of another. */
	private boolean isBelow (TopolCanvas c, TopolCanvas ancestor)
	{
		GNode	owner = c.getOwner ();
		if (owner == null)		return false;
		if (ancestor.getGraph ().getNode (owner.getLabel ()) == owner)		return true;
		for (int i = 0; i < canvases.size (); i++)
		{
			TopolCanvas	p = canvases.get (i);
			if ((p != c) && (p != ancestor) && (p.getGraph ().getNode (owner.getLabel ()) == owner))		return isBelow (p, ancestor);
		}
		return false;
	}

	/** Closes the tabs whose owner node no longer belongs to any open graph (after a deletion). */
	private void closeOrphanTabs ()
	{
		for (int j = canvases.size () - 1; j > 0; j--)
		{
			TopolCanvas	c = canvases.get (j);
			boolean		alive = false;
			for (TopolCanvas p : canvases)
				if ((p != c) && (p.getGraph ().getNode (c.getOwner ().getLabel ()) == c.getOwner ()))		{ alive = true; break; }
			if (!alive)		{ canvases.remove (j); tabs.removeTabAt (j); }
		}
	}

	private void tabChanged ()
	{
		TopolCanvas	c = current ();
		if ((c == null) || (propModel == null) || (statusBar == null))		return;			// still building the GUI
		c.refreshPlaces ();
		syncToolButtons (c.getTool ());
		propModel.setCanvas (c);
		updateButtons ();
		statusBar.setUsage (c.usageText ());
		if (c.isRoot ())								statusBar.setStatus ("Root level: the zones of the world");
		else if (c.getOwner () instanceof GNodeFL)		statusBar.setStatus ("Zone " + c.getOwner ().getLabel ());
		else											statusBar.setStatus ("Level of " + c.getOwner ().getLabel () + " (zone " + c.getZone () + ")");
	}

	/* ------------------------------------------------------------------ */
	/* Actions                                                             */
	/* ------------------------------------------------------------------ */

	protected void selectTool (int tool)
	{
		syncToolButtons (tool);
		for (TopolCanvas c : canvases)		c.setTool (tool);
	}

	private void syncToolButtons (int tool)
	{
		if ((tool >= 0) && (tool < toolButtons.length) && !toolButtons[tool].isSelected ())		toolButtons[tool].setSelected (true);
	}

	protected void deleteSelection ()
	{
		TopolCanvas	c = current ();
		if (c == null)		return;
		String	what = (c.getSelectedNode () != null) ? "node " + c.getSelectedNode ().getLabel () : "arc";
		if (c.deleteSelection ())
		{
			closeOrphanTabs ();
			graphChanged (c, "Delete " + what);
			selectionChanged (c);
		}
	}

	protected void openSelected ()
	{
		TopolCanvas	c = current ();
		if ((c != null) && (c.getSelectedNode () != null))		openNodeTab (c, c.getSelectedNode ());
	}

	private void updateButtons ()
	{
		TopolCanvas	c = current ();
		boolean		node = (c != null) && (c.getSelectedNode () != null);
		boolean		arc = (c != null) && c.hasArcSelected ();
		deleteBT.setEnabled (node || arc);
		openBT.setEnabled (node);
	}

	private void accept ()
	{
		if (propTable.isEditing ())		propTable.getCellEditor ().stopCellEditing ();
		modified	= !snapshot.equals (tc.shared.world.WorldJson.toText (world.topology ().toJson ()));
		accepted	= true;
		dispose ();
	}

	private void cancel ()
	{
		if (propTable.isEditing ())		propTable.getCellEditor ().cancelCellEditing ();
		// restore the topology the world had when the dialog was opened
		world.setTopology (new HTopolMap (world, tc.shared.world.WorldJson.parse (snapshot)));
		modified	= false;
		accepted	= false;
		dispose ();
	}

	/** Labels of the connectors (doors) of the world, for the Door selector. */
	protected String[] connectorLabels ()
	{
		String[]	labels = new String[world.connectors ().n ()];
		for (int i = 0; i < labels.length; i++)		labels[i] = world.connectors ().at (i).label;
		return labels;
	}

	/* ------------------------------------------------------------------ */
	/* TopolCanvas.Listener                                                */
	/* ------------------------------------------------------------------ */

	public void selectionChanged (TopolCanvas canvas)
	{
		if (canvas != current ())		return;
		propModel.setCanvas (canvas);
		updateButtons ();
	}

	public void graphChanged (TopolCanvas canvas, String what)
	{
		modified	= true;
		statusBar.setStatus (what);
		for (TopolCanvas c : canvases)		c.repaint ();
	}

	public void openNode (TopolCanvas canvas, GNode node)
	{
		openNodeTab (canvas, node);
	}

	public void usageChanged (String text)
	{
		statusBar.setUsage (text);
	}

	/* ------------------------------------------------------------------ */
	/* Properties of the selection                                         */
	/* ------------------------------------------------------------------ */

	private class PropertyModel extends AbstractTableModel
	{
		private static final long	serialVersionUID = 1L;
		TopolCanvas		canvas;
		String[]		names = new String[0];

		void setCanvas (TopolCanvas c)
		{
			if (propTable.isEditing ())		propTable.getCellEditor ().cancelCellEditing ();
			canvas	= c;
			GNode	node = (c != null) ? c.getSelectedNode () : null;
			if (node != null)
			{
				names = c.isRoot () ? new String[] { "Label", "Cell size (m)", "Dilation" } : new String[] { "Label" };
				propPanel.setBorder (BorderFactory.createTitledBorder ("Node " + node.getLabel ()));
			}
			else if ((c != null) && c.hasArcSelected ())
			{
				names = c.isRoot () ? new String[] { "From", "To", "Door" } : new String[] { "From", "To", "Weight" };
				propPanel.setBorder (BorderFactory.createTitledBorder ("Arc " + c.getArcFrom ().getLabel () + " → " + c.getArcTo ().getLabel ()));
			}
			else
			{
				names = new String[0];
				propPanel.setBorder (BorderFactory.createTitledBorder ("Properties"));
			}
			fireTableDataChanged ();
		}

		boolean isDoorRow (int r)						{ return (r < names.length) && names[r].equals ("Door"); }
		public int getRowCount ()						{ return names.length; }
		public int getColumnCount ()					{ return 2; }
		public String getColumnName (int c)				{ return (c == 0) ? "Property" : "Value"; }
		public boolean isCellEditable (int r, int c)	{ return (c == 1) && (r < names.length) && !names[r].equals ("Label") && !names[r].equals ("From") && !names[r].equals ("To"); }

		public Object getValueAt (int r, int c)
		{
			if ((canvas == null) || (r >= names.length))		return "";
			if (c == 0)		return names[r];
			GNode	node = canvas.getSelectedNode ();
			switch (names[r])
			{
			case "Label":			return node.getLabel ();
			case "Cell size (m)":	return String.valueOf (((GNodeFL) node).getCellSize ());
			case "Dilation":		return String.valueOf (((GNodeFL) node).getDilation ());
			case "From":			return canvas.getArcFrom ().getLabel ();
			case "To":				return canvas.getArcTo ().getLabel ();
			case "Door":			{ String d = ((GNodeFL) canvas.getArcFrom ()).getDoor (canvas.getArcTo ().getLabel ()); return (d == null) ? "" : d; }
			case "Weight":			return String.valueOf (canvas.getArcFrom ().getWeight (canvas.getArcTo ().index ()));
			}
			return "";
		}

		public void setValueAt (Object value, int r, int c)
		{
			if ((canvas == null) || (c != 1) || (r >= names.length))		return;
			String	v = (value == null) ? "" : value.toString ().trim ();
			try
			{
				switch (names[r])
				{
				case "Cell size (m)":	((GNodeFL) canvas.getSelectedNode ()).setCellSize (Double.parseDouble (v)); break;
				case "Dilation":		((GNodeFL) canvas.getSelectedNode ()).setDilation (Double.parseDouble (v)); break;
				case "Door":			((GNodeFL) canvas.getArcFrom ()).setDoor (canvas.getArcTo ().getLabel (), v.isEmpty () ? null : v); break;
				case "Weight":			canvas.getArcFrom ().setWeight (canvas.getArcTo ().index (), Integer.parseInt (v)); break;
				default:				return;
				}
				graphChanged (canvas, "Edit " + names[r]);
				fireTableRowsUpdated (r, r);
			}
			catch (NumberFormatException e)		{ statusBar.setStatus ("Invalid value: " + v); }
		}
	}

	/** Stand-alone test: edits the topology of a world file and prints the result. */
	public static void main (String[] args) throws Exception
	{
		final World	w = new World (args[0]);
		SwingUtilities.invokeAndWait (new Runnable ()
		{
			public void run ()
			{
				TopolEditorDialog	d = new TopolEditorDialog (null, w);
				if (d.showDialog ())		System.out.println (w.toJsonText ());
				System.exit (0);
			}
		});
	}
}

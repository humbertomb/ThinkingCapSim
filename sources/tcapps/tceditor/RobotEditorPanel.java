/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
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
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import tc.vrobot.RobotDef;

/**
 * Editor of a robot description ({@link RobotDef}), as a panel that a window
 * ({@link RobotEditorWindow}) or a dialog ({@link RobotEditorDialog}) hosts:
 * a toolbar on the left adds drawing lines, bumpers and sensors; the centre
 * shows the robot in plan view ({@link RobotCanvas}); on the right a tree with
 * one category per part of the description and, below it, the property editor
 * of the selected element.
 *
 * The panel edits the description it is given (the dialog hands it a copy).
 */
public class RobotEditorPanel extends JPanel implements RobotCanvas.Listener
{
	private static final long		serialVersionUID = 1L;

	static public final String		TITLE		= "Robot Editor";
	static public final String		ROBOTS_DIR	= "./conf/robots";
	static public final int			RIGHT_WIDTH		= 320;		// tree + properties column (as the world editor)
	static public final double		TREE_FRACTION	= 0.55;		// share of the tree in that column

	/** What the window or dialog hosting the editor needs to know. */
	public interface Host
	{
		/** The file or the modified state changed (title). */
		void robotStateChanged (RobotEditorPanel panel);
	}

	protected Host					host;
	protected RobotDef				robot;
	protected RobotCanvas			canvas;
	protected JTree					tree;
	protected DefaultTreeModel		treeModel;
	protected DefaultMutableTreeNode	treeRoot;
	protected JTable				propsTB;
	protected PropertyModel			propsModel;
	protected javax.swing.border.TitledBorder	propsBorder;
	protected JSplitPane			mainSP, rightSP;
	protected boolean				dividersSet, syncing, dirty;
	protected Action				lineAC, bumperAC, sensorAC, deleteAC;

	/* ------------------------------------------------------------------ */

	public RobotEditorPanel (RobotDef robot, Host host)
	{
		super (new BorderLayout ());
		this.robot	= robot;
		this.host	= host;
		buildGUI ();
		refreshTree ();
		updateTitle ();
	}

	public RobotDef		getRobot ()					{ return robot; }
	public RobotCanvas	getCanvas ()				{ return canvas; }
	public boolean		isDirty ()					{ return dirty || robot.isModified (); }

	/** Title of the hosting window: file name (or untitled) and the modified mark. */
	public String getTitle ()
	{
		File	f = robot.getFile ();
		return ((f != null) ? f.getName () : "untitled." + RobotDef.EXTENSION) + (isDirty () ? " *" : "");
	}

	/** Commits whatever the user is typing in the property table. */
	public void stopEditing ()
	{
		if (propsTB.isEditing ())		propsTB.getCellEditor ().stopCellEditing ();
	}

	/* ------------------------------------------------------------------ */
	/* GUI                                                                 */
	/* ------------------------------------------------------------------ */

	private void buildGUI ()
	{
		// --- centre: plan view of the robot
		canvas	= new RobotCanvas (robot);
		canvas.setListener (this);
		JScrollPane	canvasSP = new JScrollPane (canvas);
		canvasSP.setBorder (BorderFactory.createEmptyBorder ());

		// --- left: toolbar
		JToolBar	tb = new JToolBar (JToolBar.VERTICAL);
		tb.setFloatable (false);
		lineAC		= ToolButtons.action ("Line", ToolIcon.WALL, "Add a segment to the drawing of the robot", new Runnable () { public void run () { addLine (); } });
		bumperAC	= ToolButtons.action ("Bumper", ToolIcon.CONNECTOR, "Add a bumper", new Runnable () { public void run () { addBumper (); } });
		sensorAC	= ToolButtons.action ("Sensor", ToolIcon.BEACON, "Add a sensor to the selected family", new Runnable () { public void run () { addSensor (); } });
		deleteAC	= ToolButtons.action ("Delete", ToolIcon.DELETE, "Delete the selected element  [Delete]", new Runnable () { public void run () { deleteSelection (); } });
		tb.add (ToolButtons.flatButton (lineAC));
		tb.add (ToolButtons.flatButton (bumperAC));
		tb.add (ToolButtons.flatButton (sensorAC));
		tb.addSeparator ();
		tb.add (ToolButtons.flatButton (deleteAC));
		tb.addSeparator ();
		tb.add (ToolButtons.flatButton (ToolButtons.action ("Zoom to Fit", ToolIcon.ZOOM_FIT, "Frame the robot", new Runnable () { public void run () { canvas.zoomToFit (); } })));
		tb.add (ToolButtons.flatButton (ToolButtons.action ("Zoom In", ToolIcon.ZOOM_IN, "Zoom in", new Runnable () { public void run () { canvas.zoomIn (); } })));
		tb.add (ToolButtons.flatButton (ToolButtons.action ("Zoom Out", ToolIcon.ZOOM_OUT, "Zoom out", new Runnable () { public void run () { canvas.zoomOut (); } })));
		tb.add (Box.createVerticalGlue ());

		// --- right: tree of the description and properties of the selection
		treeRoot	= new DefaultMutableTreeNode ("Robot");
		treeModel	= new DefaultTreeModel (treeRoot);
		tree		= new JTree (treeModel);
		tree.setRootVisible (false);
		tree.setShowsRootHandles (true);
		tree.getSelectionModel ().setSelectionMode (TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener (new TreeSelectionListener ()
		{
			public void valueChanged (TreeSelectionEvent e)		{ treeSelected (); }
		});
		JScrollPane	treeSP = new JScrollPane (tree);
		treeSP.setPreferredSize (new Dimension (300, 260));

		propsModel	= new PropertyModel ();
		propsTB		= new JTable (propsModel);
		propsTB.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		propsTB.setRowHeight (20);
		propsTB.putClientProperty ("terminateEditOnFocusLost", Boolean.TRUE);
		propsTB.getColumnModel ().getColumn (0).setPreferredWidth (150);
		propsTB.getColumnModel ().getColumn (1).setPreferredWidth (150);
		JScrollPane	propsSP = new JScrollPane (propsTB);
		propsBorder	= BorderFactory.createTitledBorder (" ");
		JPanel		propsPN = new JPanel (new BorderLayout ());
		propsPN.setBorder (propsBorder);
		propsPN.add (propsSP, BorderLayout.CENTER);

		rightSP		= new JSplitPane (JSplitPane.VERTICAL_SPLIT, treeSP, propsPN);
		rightSP.setResizeWeight (TREE_FRACTION);
		rightSP.setPreferredSize (new Dimension (RIGHT_WIDTH, 600));
		rightSP.setBorder (BorderFactory.createEmptyBorder ());

		mainSP		= new JSplitPane (JSplitPane.HORIZONTAL_SPLIT, canvasSP, rightSP);
		mainSP.setResizeWeight (1.0);
		mainSP.setBorder (BorderFactory.createEmptyBorder ());
		canvasSP.setMinimumSize (new Dimension (300, 200));
		rightSP.setMinimumSize (new Dimension (240, 200));
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

	/** Puts the split dividers at the world editor proportions. */
	public void resetDividers ()
	{
		if (mainSP.getWidth () <= 0)		return;
		dividersSet	= true;
		mainSP.setDividerLocation (mainSP.getWidth () - mainSP.getDividerSize () - RIGHT_WIDTH);
		rightSP.setDividerLocation (TREE_FRACTION);
		canvas.zoomToFit ();
	}

	/**
	 * Menu bar of the editor: File (new, load, save) and, when
	 * <code>withQuit</code>, the Quit entry of a stand-alone window.
	 */
	public JMenuBar buildMenuBar (boolean withQuit)
	{
		int			mask = java.awt.Toolkit.getDefaultToolkit ().getMenuShortcutKeyMaskEx ();
		JMenuBar	mb = new JMenuBar ();
		JMenu		mfile = new JMenu ("File");

		mfile.add (menuItem ("New Robot", KeyEvent.VK_N, mask, new Runnable () { public void run () { newRobot (); } }));
		mfile.add (menuItem ("Load Robot...", KeyEvent.VK_O, mask, new Runnable () { public void run () { loadRobot (); } }));
		mfile.add (menuItem ("Save Robot", KeyEvent.VK_S, mask, new Runnable () { public void run () { saveRobot (false); } }));
		mfile.add (menuItem ("Save Robot As...", KeyEvent.VK_S, mask | KeyEvent.SHIFT_DOWN_MASK, new Runnable () { public void run () { saveRobot (true); } }));
		if (withQuit)
		{
			mfile.addSeparator ();
			mfile.add (menuItem ("Quit", KeyEvent.VK_Q, mask, new Runnable () { public void run () { quit (); } }));
		}
		mb.add (mfile);
		return mb;
	}

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

	/** Closes the window hosting the editor (the Quit entry of the menu). */
	protected void quit ()
	{
		java.awt.Window	win = SwingUtilities.getWindowAncestor (this);
		if (win instanceof RobotEditorWindow)		((RobotEditorWindow) win).quit ();
		else if (win != null)						win.dispose ();
	}

	/* ------------------------------------------------------------------ */
	/* Files                                                               */
	/* ------------------------------------------------------------------ */

	/** Installs another description in the editor (New / Load). */
	public void setRobot (RobotDef r)
	{
		robot	= r;
		dirty	= false;
		canvas.setRobot (robot);
		refreshTree ();
		showProperties (null);
		updateTitle ();
	}

	private void newRobot ()
	{
		if (!confirmDiscard ())			return;
		setRobot (RobotDef.create ());
	}

	private void loadRobot ()
	{
		if (!confirmDiscard ())			return;
		JFileChooser	fc = robotChooser ("Load Robot");
		if (fc.showOpenDialog (this) != JFileChooser.APPROVE_OPTION)		return;
		loadRobot (fc.getSelectedFile ());
	}

	public void loadRobot (File f)
	{
		try
		{
			setRobot (RobotDef.load (f));
		} catch (Exception e)
		{
			e.printStackTrace ();
			JOptionPane.showMessageDialog (this, "Cannot load " + f.getName () + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
		}
	}

	public boolean saveRobot (boolean saveAs)
	{
		File	f = robot.getFile ();

		stopEditing ();
		if (saveAs || (f == null))
		{
			JFileChooser	fc = robotChooser (saveAs ? "Save Robot As" : "Save Robot");
			if (f != null)		fc.setSelectedFile (f);
			if (fc.showSaveDialog (this) != JFileChooser.APPROVE_OPTION)		return false;
			f	= fc.getSelectedFile ();
			if (!f.getName ().toLowerCase ().endsWith ("." + RobotDef.EXTENSION))	f = new File (f.getPath () + "." + RobotDef.EXTENSION);
			if (f.exists () && (JOptionPane.showConfirmDialog (this, f.getName () + " already exists. Overwrite?", TITLE,
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION))		return false;
		}
		try
		{
			robot.save (f);
			dirty	= false;
			updateTitle ();
			return true;
		} catch (Exception e)
		{
			e.printStackTrace ();
			JOptionPane.showMessageDialog (this, "Cannot save " + f.getName () + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	private JFileChooser robotChooser (String title)
	{
		File			cur = robot.getFile ();
		File			dir = (cur != null) ? cur.getParentFile () : new File (ROBOTS_DIR);
		if ((dir == null) || !dir.isDirectory ())		dir = new File (".");
		JFileChooser	fc = new JFileChooser (dir);
		fc.setDialogTitle (title);
		fc.setFileFilter (new FileNameExtensionFilter ("Robot descriptions (*.robot)", RobotDef.EXTENSION));
		return fc;
	}

	/** Asks what to do with unsaved changes; false when the user cancels. */
	public boolean confirmDiscard ()
	{
		if (!isDirty ())				return true;
		int		r = JOptionPane.showConfirmDialog (this, "The robot description has unsaved changes. Save them first?", TITLE,
					JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (r == JOptionPane.CANCEL_OPTION)		return false;
		if (r == JOptionPane.YES_OPTION)		return saveRobot (false);
		return true;
	}

	private void updateTitle ()
	{
		if (host != null)		host.robotStateChanged (this);
	}

	/** The description changed: the view, the tree and the title follow. */
	private void changed ()
	{
		dirty	= true;
		canvas.robotChanged ();
		updateTitle ();
	}

	/* ------------------------------------------------------------------ */
	/* Creation and deletion                                               */
	/* ------------------------------------------------------------------ */

	/** Family the sensor tool acts on: the one of the selection, the laser range finders otherwise. */
	private String currentFamily ()
	{
		RobotItem	it = canvas.getSelection ();
		return ((it != null) && (it.family != null)) ? it.family : "lrf";
	}

	private void addLine ()
	{
		double	r = (robot.radius > 0.0) ? robot.radius : 0.25;
		robot.icon.add (new RobotDef.IconLine (-r / 2, -r / 2, r / 2, r / 2));
		changed ();
		refreshTree ();
		select (new RobotItem (RobotItem.LINE, robot.icon.size () - 1));
	}

	private void addBumper ()
	{
		double	r = (robot.radius > 0.0) ? robot.radius : 0.25;
		robot.bumpers.add (new RobotDef.Bumper (r, -r, r, r));
		changed ();
		refreshTree ();
		select (new RobotItem (RobotItem.BUMPER, robot.bumpers.size () - 1));
	}

	private void addSensor ()
	{
		String				fam = currentFamily ();
		RobotDef.Sensor		s = new RobotDef.Sensor ();

		s.rho	= (robot.radius > 0.0) ? robot.radius : 0.25;
		robot.family (fam).sensors.add (s);
		changed ();
		refreshTree ();
		select (new RobotItem (RobotItem.SENSOR, robot.family (fam).n () - 1, fam));
	}

	private void deleteSelection ()
	{
		RobotItem	it = canvas.getSelection ();

		if (it == null)					return;
		switch (it.kind)
		{
		case RobotItem.LINE:		robot.icon.remove (it.index);							break;
		case RobotItem.BUMPER:		robot.bumpers.remove (it.index);						break;
		case RobotItem.SENSOR:		robot.family (it.family).sensors.remove (it.index);		break;
		default:					return;								// the sections themselves are not removable
		}
		changed ();
		refreshTree ();
		select (null);
	}

	/* ------------------------------------------------------------------ */
	/* Tree                                                                */
	/* ------------------------------------------------------------------ */

	private void refreshTree ()
	{
		boolean		first = (treeRoot.getChildCount () == 0);
		List<String>	expanded = new ArrayList<String> ();

		if (!first)
			for (int i = 0; i < treeRoot.getChildCount (); i++)
			{
				DefaultMutableTreeNode	n = (DefaultMutableTreeNode) treeRoot.getChildAt (i);
				if (tree.isExpanded (new TreePath (n.getPath ())))		expanded.add (n.toString ());
			}

		syncing	= true;
		treeRoot.removeAllChildren ();
		treeRoot.add (new ItemNode (new RobotItem (RobotItem.PLATFORM, 0), "Platform"));
		treeRoot.add (new ItemNode (new RobotItem (RobotItem.KINEMATICS, 0), "Kinematics"));

		DefaultMutableTreeNode	lines = new DefaultMutableTreeNode ("Drawing  (" + robot.icon.size () + ")");
		for (int i = 0; i < robot.icon.size (); i++)		lines.add (new ItemNode (new RobotItem (RobotItem.LINE, i), "Line " + i));
		treeRoot.add (lines);

		DefaultMutableTreeNode	bumpers = new DefaultMutableTreeNode ("Bumpers  (" + robot.bumpers.size () + ")");
		for (int i = 0; i < robot.bumpers.size (); i++)	bumpers.add (new ItemNode (new RobotItem (RobotItem.BUMPER, i), "Bumper " + i));
		treeRoot.add (bumpers);

		for (String fam : RobotDef.FAMILIES)
		{
			RobotDef.Family			f = robot.family (fam);
			ItemNode				cat = new ItemNode (new RobotItem (RobotItem.FAMILY, 0, fam), RobotDef.familyName (fam) + "  (" + f.n () + ")");
			for (int i = 0; i < f.n (); i++)		cat.add (new ItemNode (new RobotItem (RobotItem.SENSOR, i, fam), fam + i));
			treeRoot.add (cat);
		}
		treeRoot.add (new ItemNode (new RobotItem (RobotItem.EXTRA, 0), "Other properties  (" + robot.extra.size () + ")"));

		treeModel.reload ();
		for (int i = 0; i < treeRoot.getChildCount (); i++)
		{
			DefaultMutableTreeNode	n = (DefaultMutableTreeNode) treeRoot.getChildAt (i);
			if (expanded.contains (n.toString ()))		tree.expandPath (new TreePath (n.getPath ()));
		}
		syncing	= false;
		selectInTree (canvas.getSelection ());
	}

	/** A node standing for one element of the description. */
	private class ItemNode extends DefaultMutableTreeNode
	{
		private static final long	serialVersionUID = 1L;
		String		label;
		ItemNode (RobotItem it, String label)		{ super (it); this.label = label; }
		public String toString ()					{ return label; }
	}

	private void selectInTree (RobotItem item)
	{
		if (item == null)			{ tree.clearSelection (); return; }
		for (java.util.Enumeration<?> e = treeRoot.depthFirstEnumeration (); e.hasMoreElements (); )
		{
			DefaultMutableTreeNode	n = (DefaultMutableTreeNode) e.nextElement ();
			if (item.equals (n.getUserObject ()))
			{
				TreePath	p = new TreePath (n.getPath ());
				tree.setSelectionPath (p);
				tree.scrollPathToVisible (p);
				return;
			}
		}
		tree.clearSelection ();
	}

	private void treeSelected ()
	{
		if (syncing)				return;
		TreePath	p = tree.getSelectionPath ();
		Object		o = (p == null) ? null : ((DefaultMutableTreeNode) p.getLastPathComponent ()).getUserObject ();
		RobotItem	it = (o instanceof RobotItem) ? (RobotItem) o : null;
		syncing	= true;
		try
		{
			canvas.setSelection (it);				// the view highlights what the tree selects
		}
		finally { syncing = false; }
		showProperties (it);
	}

	/** Selects an element everywhere (view, tree, property table). */
	private void select (RobotItem it)
	{
		canvas.setSelection (it);
	}

	/** The view moved or turned an element: the model changed and the table follows. */
	public void elementChanged (RobotItem item)
	{
		changed ();
		propsModel.refresh ();
	}

	/** The view changed the selection. */
	public void selectionChanged (RobotItem item)
	{
		if (!syncing)
		{
			syncing	= true;
			try { selectInTree (item); } finally { syncing = false; }
		}
		showProperties (item);
		deleteAC.setEnabled ((item != null) && ((item.kind == RobotItem.LINE) || (item.kind == RobotItem.BUMPER) || (item.kind == RobotItem.SENSOR)));
	}

	/* ------------------------------------------------------------------ */
	/* Properties                                                          */
	/* ------------------------------------------------------------------ */

	private void showProperties (RobotItem it)
	{
		stopEditing ();
		propsModel.setItem (it);
		propsBorder.setTitle ((it == null) ? " " : title (it));
		((JComponent) propsTB.getParent ().getParent ().getParent ()).repaint ();
	}

	private String title (RobotItem it)
	{
		switch (it.kind)
		{
		case RobotItem.LINE:		return "Drawing line " + it.index;
		case RobotItem.BUMPER:		return "Bumper " + it.index;
		case RobotItem.SENSOR:		return RobotDef.familyName (it.family) + ": " + it.family + it.index;
		case RobotItem.FAMILY:		return RobotDef.familyName (it.family);
		default:					return RobotItem.NAMES[it.kind];
		}
	}

	/** Names of the properties of an element. */
	public String[] propertyNames (RobotItem it)
	{
		if (it == null)				return new String[0];
		switch (it.kind)
		{
		case RobotItem.PLATFORM:	return new String[] { "name", "radius", "image", "shape", "lift" };
		case RobotItem.KINEMATICS:	return new String[] { "drive", "vmax", "rmax", "maxmotor", "maxsteer", "samax", "lamax", "ldmax",
														  "length", "base", "rwheel", "wheel", "gear", "pulses", "dtime",
														  "odom et", "odom er", "odom bias" };
		case RobotItem.LINE:
		case RobotItem.BUMPER:		return new String[] { "xi", "yi", "xf", "yf" };
		case RobotItem.SENSOR:		return new String[] { "rho", "theta", "height", "orientation", "step" };
		case RobotItem.FAMILY:		return new String[] { "driver", "range max", "range min", "cone", "cycle", "rays", "reflect", "beacons", "objects" };
		case RobotItem.EXTRA:
		{
			List<String>	keys = new ArrayList<String> (robot.extra.keySet ());
			return keys.toArray (new String[0]);
		}
		}
		return new String[0];
	}

	public String getProperty (RobotItem it, String name)
	{
		RobotDef.Kinematics		k = robot.kinematics;

		switch (it.kind)
		{
		case RobotItem.PLATFORM:
			if (name.equals ("name"))		return (robot.name != null) ? robot.name : "";
			if (name.equals ("radius"))		return RobotDef.fmt (robot.radius);
			if (name.equals ("image"))		return (robot.image != null) ? robot.image : "";
			if (name.equals ("shape"))		return (robot.shape != null) ? robot.shape : "";
			if (name.equals ("lift"))		return (robot.lift != null) ? robot.lift : "";
			break;
		case RobotItem.KINEMATICS:
			if (name.equals ("drive"))		return (k.drive != null) ? k.drive : "";
			if (name.equals ("vmax"))		return RobotDef.fmt (k.vmax);
			if (name.equals ("rmax"))		return RobotDef.fmt (k.rmax);
			if (name.equals ("maxmotor"))	return RobotDef.fmt (k.maxmotor);
			if (name.equals ("maxsteer"))	return RobotDef.fmt (k.maxsteer);
			if (name.equals ("samax"))		return RobotDef.fmt (k.samax);
			if (name.equals ("lamax"))		return RobotDef.fmt (k.lamax);
			if (name.equals ("ldmax"))		return RobotDef.fmt (k.ldmax);
			if (name.equals ("length"))		return RobotDef.fmt (k.length);
			if (name.equals ("base"))		return RobotDef.fmt (k.base);
			if (name.equals ("rwheel"))		return RobotDef.fmt (k.rwheel);
			if (name.equals ("wheel"))		return RobotDef.fmt (k.wheel);
			if (name.equals ("gear"))		return RobotDef.fmt (k.gear);
			if (name.equals ("pulses"))		return RobotDef.fmt (k.pulses);
			if (name.equals ("dtime"))		return String.valueOf (k.dtime);
			if (name.equals ("odom et"))	return RobotDef.fmt (k.odomET);
			if (name.equals ("odom er"))	return RobotDef.fmt (k.odomER);
			if (name.equals ("odom bias"))	return RobotDef.fmt (k.odomBias);
			break;
		case RobotItem.LINE:
		{
			RobotDef.IconLine	l = robot.icon.get (it.index);
			if (name.equals ("xi"))			return RobotDef.fmt (l.xi);
			if (name.equals ("yi"))			return RobotDef.fmt (l.yi);
			if (name.equals ("xf"))			return RobotDef.fmt (l.xf);
			if (name.equals ("yf"))			return RobotDef.fmt (l.yf);
			break;
		}
		case RobotItem.BUMPER:
		{
			RobotDef.Bumper		b = robot.bumpers.get (it.index);
			if (name.equals ("xi"))			return RobotDef.fmt (b.xi);
			if (name.equals ("yi"))			return RobotDef.fmt (b.yi);
			if (name.equals ("xf"))			return RobotDef.fmt (b.xf);
			if (name.equals ("yf"))			return RobotDef.fmt (b.yf);
			break;
		}
		case RobotItem.SENSOR:
		{
			RobotDef.Sensor		s = robot.family (it.family).sensors.get (it.index);
			if (name.equals ("rho"))			return RobotDef.fmt (s.rho);
			if (name.equals ("theta"))			return RobotDef.fmt (s.theta);
			if (name.equals ("height"))			return RobotDef.fmt (s.height);
			if (name.equals ("orientation"))	return RobotDef.fmt (s.orientation);
			if (name.equals ("step"))			return String.valueOf (s.step);
			break;
		}
		case RobotItem.FAMILY:
		{
			RobotDef.Family		f = robot.family (it.family);
			if (name.equals ("driver"))		return (f.driver != null) ? f.driver : "";
			if (name.equals ("range max"))	return RobotDef.fmt (f.rangemax);
			if (name.equals ("range min"))	return RobotDef.fmt (f.rangemin);
			if (name.equals ("cone"))		return RobotDef.fmt (f.cone);
			if (name.equals ("cycle"))		return String.valueOf (f.cycle);
			if (name.equals ("rays"))		return String.valueOf (f.rays);
			if (name.equals ("reflect"))	return RobotDef.fmt (f.reflect);
			if (name.equals ("beacons"))	return String.valueOf (f.beacons);
			if (name.equals ("objects"))	return String.valueOf (f.objects);
			break;
		}
		case RobotItem.EXTRA:
		{
			String	v = robot.extra.get (name);
			return (v != null) ? v : "";
		}
		}
		return "";
	}

	/** True when a property can be edited. */
	public boolean isEditable (RobotItem it, String name)
	{
		return true;
	}

	public void setProperty (RobotItem it, String name, String value)
	{
		RobotDef.Kinematics		k = robot.kinematics;

		value	= value.trim ();
		switch (it.kind)
		{
		case RobotItem.PLATFORM:
			if (name.equals ("name"))			robot.name = token (value);
			else if (name.equals ("radius"))	robot.radius = num (value);
			else if (name.equals ("image"))		robot.image = token (value);
			else if (name.equals ("shape"))		robot.shape = token (value);
			else if (name.equals ("lift"))		robot.lift = token (value);
			break;
		case RobotItem.KINEMATICS:
			if (name.equals ("drive"))			k.drive = token (value);
			else if (name.equals ("vmax"))		k.vmax = num (value);
			else if (name.equals ("rmax"))		k.rmax = num (value);
			else if (name.equals ("maxmotor"))	k.maxmotor = num (value);
			else if (name.equals ("maxsteer"))	k.maxsteer = num (value);
			else if (name.equals ("samax"))		k.samax = num (value);
			else if (name.equals ("lamax"))		k.lamax = num (value);
			else if (name.equals ("ldmax"))		k.ldmax = num (value);
			else if (name.equals ("length"))	k.length = num (value);
			else if (name.equals ("base"))		k.base = num (value);
			else if (name.equals ("rwheel"))	k.rwheel = num (value);
			else if (name.equals ("wheel"))		k.wheel = num (value);
			else if (name.equals ("gear"))		k.gear = num (value);
			else if (name.equals ("pulses"))	k.pulses = num (value);
			else if (name.equals ("dtime"))		k.dtime = (long) num (value);
			else if (name.equals ("odom et"))	k.odomET = num (value);
			else if (name.equals ("odom er"))	k.odomER = num (value);
			else if (name.equals ("odom bias"))	k.odomBias = num (value);
			break;
		case RobotItem.LINE:
		{
			RobotDef.IconLine	l = robot.icon.get (it.index);
			if (name.equals ("xi"))				l.xi = num (value);
			else if (name.equals ("yi"))		l.yi = num (value);
			else if (name.equals ("xf"))		l.xf = num (value);
			else if (name.equals ("yf"))		l.yf = num (value);
			break;
		}
		case RobotItem.BUMPER:
		{
			RobotDef.Bumper		b = robot.bumpers.get (it.index);
			if (name.equals ("xi"))				b.xi = num (value);
			else if (name.equals ("yi"))		b.yi = num (value);
			else if (name.equals ("xf"))		b.xf = num (value);
			else if (name.equals ("yf"))		b.yf = num (value);
			break;
		}
		case RobotItem.SENSOR:
		{
			RobotDef.Sensor		s = robot.family (it.family).sensors.get (it.index);
			if (name.equals ("rho"))				s.rho = num (value);
			else if (name.equals ("theta"))			s.theta = num (value);
			else if (name.equals ("height"))		s.height = num (value);
			else if (name.equals ("orientation"))	s.orientation = num (value);
			else if (name.equals ("step"))			s.step = (int) num (value);
			break;
		}
		case RobotItem.FAMILY:
		{
			RobotDef.Family		f = robot.family (it.family);
			if (name.equals ("driver"))			f.driver = token (value);
			else if (name.equals ("range max"))	f.rangemax = num (value);
			else if (name.equals ("range min"))	f.rangemin = num (value);
			else if (name.equals ("cone"))		f.cone = num (value);
			else if (name.equals ("cycle"))		f.cycle = (int) num (value);
			else if (name.equals ("rays"))		f.rays = (int) num (value);
			else if (name.equals ("reflect"))	f.reflect = num (value);
			else if (name.equals ("beacons"))	f.beacons = (int) num (value);
			else if (name.equals ("objects"))	f.objects = (int) num (value);
			break;
		}
		case RobotItem.EXTRA:
			if (robot.extra.containsKey (name))		robot.extra.put (name, value);
			break;
		}
		changed ();
	}

	static private String token (String v)			{ return (v.length () == 0) ? null : v; }

	static private double num (String v)
	{
		try { return Double.parseDouble (v.replace (',', '.')); }
		catch (Exception e) { throw new IllegalArgumentException ("'" + v + "' is not a number"); }
	}

	/** Rows of the property table: name and value of the selected element. */
	protected class PropertyModel extends AbstractTableModel
	{
		private static final long	serialVersionUID = 1L;
		RobotItem		item;
		String[]		names = new String[0];

		void setItem (RobotItem it)
		{
			item	= it;
			names	= propertyNames (it);
			fireTableDataChanged ();
		}

		public int getRowCount ()					{ return names.length; }
		public int getColumnCount ()				{ return 2; }
		public String getColumnName (int c)			{ return (c == 0) ? "Property" : "Value"; }
		public boolean isCellEditable (int r, int c)	{ return (c == 1) && (item != null) && isEditable (item, names[r]); }
		public Object getValueAt (int r, int c)		{ return (c == 0) ? names[r] : getProperty (item, names[r]); }

		public void setValueAt (Object v, int r, int c)
		{
			try
			{
				setProperty (item, names[r], (v == null) ? "" : v.toString ());
				fireTableRowsUpdated (r, r);
				if ((item.kind == RobotItem.PLATFORM) && names[r].equals ("name"))		refreshTree ();
			} catch (IllegalArgumentException e)
			{
				JOptionPane.showMessageDialog (RobotEditorPanel.this, e.getMessage (), TITLE, JOptionPane.ERROR_MESSAGE);
			}
		}

		void refresh ()								{ fireTableDataChanged (); }
	}
}

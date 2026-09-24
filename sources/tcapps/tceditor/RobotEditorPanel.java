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

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
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
	static public final int			OVERLAY_GAP		= 6;		// margin of what floats over the view (px)

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
	protected StatusBar				statusBar;						// where the cursor is
	protected boolean				dividersSet, syncing, dirty;
	protected Action				openAC, wheelAC, lineAC, bumperAC, sensorAC, groupAC, fusedAC, scanAC, deleteAC;
	protected RobotView3DWindow		view3d;					// created the first time it is shown
	protected javax.swing.JToggleButton			view3dBT;
	protected javax.swing.JToggleButton[]		viewBT;					// the three flat projections
	protected javax.swing.JCheckBoxMenuItem		view3dMI, gridMI, snapMI, snapVertexMI, imageMI;

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

		// the three flat projections, floating over the top right corner of the view
		final JScrollPane	csp = canvasSP;
		final JComponent	bar = buildViewBar ();
		javax.swing.JLayeredPane	viewPN = new javax.swing.JLayeredPane ()
		{
			private static final long	serialVersionUID = 1L;

			public void doLayout ()
			{
				Dimension	d = bar.getPreferredSize ();
				csp.setBounds (0, 0, getWidth (), getHeight ());
				bar.setBounds (getWidth () - d.width - OVERLAY_GAP, OVERLAY_GAP, d.width, d.height);
			}

			public Dimension getPreferredSize ()		{ return csp.getPreferredSize (); }
		};
		viewPN.add (canvasSP, javax.swing.JLayeredPane.DEFAULT_LAYER);
		viewPN.add (bar, javax.swing.JLayeredPane.PALETTE_LAYER);

		// --- left: toolbar
		JToolBar	tb = new JToolBar (JToolBar.VERTICAL);
		tb.setFloatable (false);
		openAC		= ToolButtons.action ("Load Robot", ToolIcon.ROBOT_FILE, "Load a robot description  [Ctrl+O]", new Runnable () { public void run () { loadRobot (); } });
		wheelAC		= ToolButtons.action ("Wheel", ToolIcon.WHEEL, "Add a wheel to the drive train", new Runnable () { public void run () { addWheel (); } });
		lineAC		= ToolButtons.action ("Line", ToolIcon.WALL, "Add a segment to the drawing of the robot", new Runnable () { public void run () { addLine (); } });
		bumperAC	= ToolButtons.action ("Bumper", ToolIcon.CONNECTOR, "Add a bumper", new Runnable () { public void run () { addBumper (); } });
		sensorAC	= ToolButtons.action ("Sensor", ToolIcon.BEACON, "Add a sensor to the selected family", new Runnable () { public void run () { addSensor (); } });
		groupAC		= ToolButtons.action ("Virtual sensor", ToolIcon.VIRTUAL, "Add a virtual sensor: a sector standing for a group of the real ones", new Runnable () { public void run () { addGroup (); } });
		fusedAC		= ToolButtons.action ("Fused sensor", ToolIcon.FUSED, "Add a fused sensor: one direction, read from the real sensors looking that way", new Runnable () { public void run () { addFused (); } });
		scanAC		= ToolButtons.action ("Laser reduction", ToolIcon.SCAN, "Add a reduced laser scan: the fan the rays of the laser are taken down to", new Runnable () { public void run () { addScan (); } });
		deleteAC	= ToolButtons.action ("Delete", ToolIcon.DELETE, "Delete the selected element  [Delete]", new Runnable () { public void run () { deleteSelection (); } });
		tb.add (ToolButtons.flatButton (openAC));
		tb.addSeparator ();
		tb.add (ToolButtons.flatButton (wheelAC));
		tb.addSeparator ();
		tb.add (ToolButtons.flatButton (lineAC));
		tb.add (ToolButtons.flatButton (bumperAC));
		tb.add (ToolButtons.flatButton (sensorAC));
		tb.add (ToolButtons.flatButton (groupAC));
		tb.add (ToolButtons.flatButton (fusedAC));
		tb.add (ToolButtons.flatButton (scanAC));
		tb.addSeparator ();
		tb.add (ToolButtons.flatButton (deleteAC));
		tb.addSeparator ();
		tb.add (ToolButtons.flatButton (ToolButtons.action ("Zoom to Fit", ToolIcon.ZOOM_FIT, "Frame the robot", new Runnable () { public void run () { canvas.zoomToFit (); } })));
		tb.add (ToolButtons.flatButton (ToolButtons.action ("Zoom In", ToolIcon.ZOOM_IN, "Zoom in", new Runnable () { public void run () { canvas.zoomIn (); } })));
		tb.add (ToolButtons.flatButton (ToolButtons.action ("Zoom Out", ToolIcon.ZOOM_OUT, "Zoom out", new Runnable () { public void run () { canvas.zoomOut (); } })));
		tb.add (Box.createVerticalGlue ());
		tb.addSeparator ();
		tb.add (view3dButton ());

		statusBar	= new StatusBar ();

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
		propsTB		= new JTable (propsModel)
		{
			private static final long	serialVersionUID = 1L;
			private final FileCellEditor.Renderer	fileRenderer = new FileCellEditor.Renderer ();
			private final DefaultCellEditor			boolEditor = new DefaultCellEditor (new JComboBox<String> (new String[] { "true", "false" }));
			private final javax.swing.table.TableCellRenderer	calcRenderer = new CalculatedRenderer ();

			// file-path properties get a text field with a "..." browse button, as in the world editor
			public javax.swing.table.TableCellEditor getCellEditor (int row, int column)
			{
				if (column == 1)
				{
					String	name = propsModel.nameAt (row);
					if (isShapeProperty (name))		return FileCellEditor.SHAPE;
					if (isImageProperty (name))		return FileCellEditor.IMAGE;
					if (isBooleanProperty (name))	return boolEditor;
					if (name.equals (DRIVER) || name.equals (DRIVE))
					{
						javax.swing.table.TableCellEditor	ed = classEditor (propsModel.item, name);
						if (ed != null)		return ed;
					}
					if (name.equals (SIM_MODE) && (propsModel.item != null))
					{
						javax.swing.table.TableCellEditor	ed = simModeEditor (propsModel.item.family);
						if (ed != null)		return ed;
					}
					if (name.equals (RESOLUTION))		return resolutionEditor (propsModel.item);
					if (name.equals (FUSION_MODE))		return fusionModeEditor ();
					if (name.equals (SCAN_MODE) && (propsModel.item != null))
						return reductionEditor (propsModel.item.index);
				}
				return super.getCellEditor (row, column);
			}

			public javax.swing.table.TableCellRenderer getCellRenderer (int row, int column)
			{
				if (column == 1)
				{
					String	name = propsModel.nameAt (row);
					if (isFileProperty (name))								return fileRenderer;
					// what cannot be typed in is shown on a grey ground: it is not for anybody to edit
					if ((propsModel.item != null) && !isEditable (propsModel.item, name))	return calcRenderer;
				}
				return super.getCellRenderer (row, column);
			}
		};
		propsTB.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		propsTB.setRowHeight (20);
		propsTB.putClientProperty ("terminateEditOnFocusLost", Boolean.TRUE);
		propsTB.getColumnModel ().getColumn (0).setPreferredWidth (150);
		propsTB.getColumnModel ().getColumn (1).setPreferredWidth (150);
		propsTB.getColumnModel ().getColumn (2).setPreferredWidth (UNITS_WIDTH);
		propsTB.getColumnModel ().getColumn (2).setMaxWidth (UNITS_WIDTH);
		JScrollPane	propsSP = new JScrollPane (propsTB);
		propsBorder	= BorderFactory.createTitledBorder (" ");
		JPanel		propsPN = new JPanel (new BorderLayout ());
		propsPN.setBorder (propsBorder);
		propsPN.add (propsSP, BorderLayout.CENTER);

		rightSP		= new JSplitPane (JSplitPane.VERTICAL_SPLIT, treeSP, propsPN);
		rightSP.setResizeWeight (TREE_FRACTION);
		rightSP.setPreferredSize (new Dimension (RIGHT_WIDTH, 600));
		rightSP.setBorder (BorderFactory.createEmptyBorder ());

		mainSP		= new JSplitPane (JSplitPane.HORIZONTAL_SPLIT, viewPN, rightSP);
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
		add (statusBar, BorderLayout.SOUTH);
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

		JMenu		mview = new JMenu ("View");
		mview.add (menuItem ("Zoom to Fit", KeyEvent.VK_0, mask, new Runnable () { public void run () { canvas.zoomToFit (); } }));
		mview.add (menuItem ("Zoom In", KeyEvent.VK_PLUS, mask, new Runnable () { public void run () { canvas.zoomIn (); } }));
		mview.add (menuItem ("Zoom Out", KeyEvent.VK_MINUS, mask, new Runnable () { public void run () { canvas.zoomOut (); } }));
		mview.addSeparator ();
		gridMI	= checkItem ("Grid", KeyEvent.VK_G, mask, canvas.isGridVisible (), new Runnable ()
		{
			public void run ()		{ canvas.setGridVisible (gridMI.isSelected ()); }
		});
		mview.add (gridMI);
		snapMI	= checkItem ("Snap to Grid", KeyEvent.VK_G, mask | java.awt.event.InputEvent.SHIFT_DOWN_MASK, canvas.isSnapEnabled (), new Runnable ()
		{
			public void run ()		{ canvas.setSnapEnabled (snapMI.isSelected ()); }
		});
		mview.add (snapMI);
		snapVertexMI	= checkItem ("Snap to Vertex", KeyEvent.VK_V, mask | java.awt.event.InputEvent.SHIFT_DOWN_MASK, canvas.isSnapVertexEnabled (), new Runnable ()
		{
			public void run ()		{ canvas.setSnapVertexEnabled (snapVertexMI.isSelected ()); }
		});
		mview.add (snapVertexMI);
		imageMI	= checkItem ("Robot Image", KeyEvent.VK_I, mask, canvas.isImageVisible (), new Runnable ()
		{
			public void run ()		{ canvas.setImageVisible (imageMI.isSelected ()); }
		});
		mview.add (imageMI);
		mview.addSeparator ();
		mview.add (view3dMenuItem (mask));
		mb.add (mview);

		return mb;
	}

	private javax.swing.JCheckBoxMenuItem checkItem (String name, int key, int mask, boolean on, final Runnable body)
	{
		javax.swing.JCheckBoxMenuItem	mi = new javax.swing.JCheckBoxMenuItem (name, on);

		mi.setAccelerator (KeyStroke.getKeyStroke (key, mask));
		mi.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ body.run (); }
		});
		return mi;
	}

	/* ------------------------------------------------------------------ */
	/* 3D view of the models of the robot                                  */
	/* ------------------------------------------------------------------ */

	/** Toolbar toggle of the 3D view (created once). */
	/**
	 * The three projections, as a small panel that floats over the view: a box
	 * drawn in isometry with the face each one looks at. Only the one from above
	 * when the robot has no 3D model.
	 */
	private JComponent buildViewBar ()
	{
		JPanel		bar = new JPanel (new java.awt.FlowLayout (java.awt.FlowLayout.CENTER, 2, 2));
		ButtonGroup	group = new ButtonGroup ();

		bar.setOpaque (true);
		bar.setBackground (new java.awt.Color (255, 255, 255, 215));
		bar.setBorder (BorderFactory.createLineBorder (new java.awt.Color (190, 195, 205)));
		viewBT	= new javax.swing.JToggleButton[RobotCanvas.V_NAMES.length];
		for (int i = 0; i < viewBT.length; i++)
		{
			final int	v = i;
			viewBT[i]	= new javax.swing.JToggleButton (new ViewIcon (i));
			viewBT[i].setToolTipText (viewTip (i));
			viewBT[i].setFocusable (false);
			viewBT[i].setMargin (new java.awt.Insets (2, 2, 2, 2));
			viewBT[i].putClientProperty ("JButton.buttonType", "square");
			viewBT[i].addActionListener (new ActionListener ()
			{
				public void actionPerformed (ActionEvent e)		{ setView (v); }
			});
			group.add (viewBT[i]);
			bar.add (viewBT[i]);
		}
		viewBT[RobotCanvas.V_TOP].setSelected (true);
		updateViewBar ();
		return bar;
	}

	static private String viewTip (int v)
	{
		switch (v)
		{
		case RobotCanvas.V_FRONT:	return "From the front: Y to the right, Z up";
		case RobotCanvas.V_SIDE:	return "From the side: X to the right, Z up";
		default:					return "From above: X to the right, Y up";
		}
	}

	/** Shows a projection of the robot, framing it again. */
	public void setView (int v)
	{
		if (!hasShape () && (v != RobotCanvas.V_TOP))		v = RobotCanvas.V_TOP;
		canvas.setView (v);
		canvas.zoomToFit ();
		if ((viewBT != null) && !viewBT[v].isSelected ())	viewBT[v].setSelected (true);
	}

	/** True when the description names a 3D model: without one there is nothing to see from the front or the side. */
	public boolean hasShape ()
	{
		return ((robot.shapeRobot != null) && (robot.shapeRobot.trim ().length () > 0))
			|| ((robot.shapeActuator != null) && (robot.shapeActuator.trim ().length () > 0));
	}

	/** Enables the projections the description allows, and comes back to the one from above when it has to. */
	protected void updateViewBar ()
	{
		boolean		on = hasShape ();

		if (viewBT == null)			return;
		for (int i = 0; i < viewBT.length; i++)
			viewBT[i].setEnabled ((i == RobotCanvas.V_TOP) || on);
		if (!on && (canvas.getView () != RobotCanvas.V_TOP))		setView (RobotCanvas.V_TOP);
	}

	public javax.swing.JToggleButton view3dButton ()
	{
		if (view3dBT == null)
		{
			view3dBT = ToolButtons.flatToggle (new ToolIcon (ToolIcon.VIEW3D), "3D view of the models of the robot  [Ctrl+3]");
			view3dBT.addActionListener (new ActionListener ()
			{
				public void actionPerformed (ActionEvent e)		{ show3D (view3dBT.isSelected ()); }
			});
		}
		return view3dBT;
	}

	/** View menu entry of the 3D view (created once). */
	public javax.swing.JCheckBoxMenuItem view3dMenuItem (int mask)
	{
		if (view3dMI == null)
			view3dMI = checkItem ("3D View", KeyEvent.VK_3, mask, false, new Runnable ()
			{
				public void run ()		{ show3D (view3dMI.isSelected ()); }
			});
		return view3dMI;
	}

	/** Shows or hides the 3D window, creating it on first use. */
	public void show3D (boolean show)
	{
		if (show && (view3d == null))
		{
			try
			{
				view3d	= new RobotView3DWindow (robot, new Runnable ()
				{
					public void run ()		{ set3DSelected (false); }		// the user closed the window
				});
				java.awt.Window	win = SwingUtilities.getWindowAncestor (this);
				if (win != null)
					view3d.setLocation (win.getX () + win.getWidth () + 10, win.getY ());
				view3d.fitView ();
				view3d.setSelection (canvas.getSelection ());
			} catch (Throwable e)
			{
				JOptionPane.showMessageDialog (this, "Cannot open the 3D view:\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
				set3DSelected (false);
				return;
			}
		}
		if (view3d != null)		view3d.setVisible (show);
		set3DSelected (show);
	}

	private void set3DSelected (boolean on)
	{
		if ((view3dBT != null) && (view3dBT.isSelected () != on))		view3dBT.setSelected (on);
		if ((view3dMI != null) && (view3dMI.isSelected () != on))		view3dMI.setSelected (on);
	}

	/** Closes the 3D view (the editor is going away). */
	public void dispose ()
	{
		if (view3d != null)		{ view3d.dispose (); view3d = null; }
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
		normalisePaths (r);				// old files may name their resources without the leading "./"
		robot	= r;
		dirty	= false;
		canvas.setRobot (robot);
		updateViewBar ();
		if (view3d != null)		view3d.setRobot (robot);
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
		if (view3d != null)		view3d.robotChanged ();
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

	private void addWheel ()
	{
		double				r = (robot.radius > 0.0) ? robot.radius : 0.25;
		RobotDef.Wheel		w = new RobotDef.Wheel ();

		w.x			= r / 2;
		w.radius	= r / 4;
		w.width		= w.radius / 3;
		w.z			= w.radius;						// resting on the floor
		w.traction	= true;
		robot.wheels.add (w);
		changed ();
		refreshTree ();
		select (new RobotItem (RobotItem.WHEEL, robot.wheels.size () - 1));
	}

	private void addLine ()
	{
		double		r = (robot.radius > 0.0) ? robot.radius : 0.25;
		double[]	a = canvas.newArea ();					// inside what the view shows, at its zoom

		if (a != null)		robot.icon.add (new RobotDef.IconLine (a[0], a[1], a[2], a[3]));
		else				robot.icon.add (new RobotDef.IconLine (-r / 2, -r / 2, r / 2, r / 2));
		changed ();
		refreshTree ();
		select (new RobotItem (RobotItem.LINE, robot.icon.size () - 1));
	}

	private void addBumper ()
	{
		double		r = (robot.radius > 0.0) ? robot.radius : 0.25;
		double[]	a = canvas.newArea ();					// inside what the view shows, at its zoom

		// across the view, as a bumper usually runs across the front of the robot
		if (a != null)		robot.bumpers.add (new RobotDef.Bumper (a[2], a[1], a[2], a[3]));
		else				robot.bumpers.add (new RobotDef.Bumper (r, -r, r, r));
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

	/**
	 * Adds a virtual sensor, looking forward from the rim of the robot.
	 *
	 * It is made as an arc over the range buffer (the weighted one, which takes an
	 * older reading as a farther one), since that is the one kind that stands on
	 * its own: the others read the virtual sensors of the fusion by index, which
	 * is nothing this editor can offer yet.
	 */
	private void addGroup ()
	{
		double				r = (robot.radius > 0.0) ? robot.radius : 0.25;
		RobotDef.Group		g = new RobotDef.Group ();

		g.rho		= r;
		g.rangemax	= Math.max (1.0, 4 * r);
		g.cone		= 30.0;
		g.mode		= 4;						// tclib.utils.fusion.FusionDesc.G_WBUF_ARC
		g.base		= 0.3;
		robot.groups.add (g);
		changed ();
		refreshTree ();
		select (new RobotItem (RobotItem.GROUP, robot.groups.size () - 1));
	}

	/**
	 * Adds a fused sensor, looking forward from the rim of the robot: one reading
	 * in one direction, taken from the real sensors that look that way.
	 *
	 * It is made as the nearer of the two (sonar and infrared), which is the mode
	 * that asks for nothing else, and as far and as wide as the others of its
	 * robot, since the fusion reads one range and one aperture for the whole lot.
	 */
	private void addFused ()
	{
		double				r = (robot.radius > 0.0) ? robot.radius : 0.25;
		RobotDef.Fused		f = new RobotDef.Fused ();

		f.rho		= r;
		f.rangemax	= robot.fused.isEmpty () ? Math.max (1.0, 8 * r) : robot.fused.get (0).rangemax;
		f.cone		= robot.fused.isEmpty () ? 20.0 : robot.fused.get (0).cone;
		f.mode		= 2;						// tclib.utils.fusion.FusionDesc.V_MIN
		robot.fused.add (f);
		changed ();
		refreshTree ();
		select (new RobotItem (RobotItem.FUSED, robot.fused.size () - 1));
	}

	/**
	 * Adds a reduced laser scan, looking forward from the rim of the robot: the
	 * fan the fusion takes the rays of the laser down to.
	 *
	 * It is made as the least of each bunch of rays, which is what a scan is read
	 * for, and as wide and as far as the others of its robot, since the fusion
	 * reads one fan for the whole lot.
	 */
	private void addScan ()
	{
		double				r = (robot.radius > 0.0) ? robot.radius : 0.25;
		RobotDef.Scanner	s = new RobotDef.Scanner ();

		s.rho		= r;
		s.rays		= robot.scans.isEmpty () ? 90 : robot.scans.get (0).rays;
		s.cone		= robot.scans.isEmpty () ? 180.0 : robot.scans.get (0).cone;
		s.rangemax	= robot.scans.isEmpty () ? Math.max (1.0, 40 * r) : robot.scans.get (0).rangemax;
		s.mode		= 0;						// tclib.utils.fusion.FusionDesc.S_MIN
		robot.scans.add (s);
		changed ();
		refreshTree ();
		select (new RobotItem (RobotItem.SCAN, robot.scans.size () - 1));
	}

	private void deleteSelection ()
	{
		List<RobotItem>		all;
		boolean				any = false;

		// a family holds its sensors so that they move together, not so that one
		// key takes them all away
		if (canvas.isCollectionSelected ())			return;
		all		= new ArrayList<RobotItem> (canvas.selected ());

		// from the last index to the first, so that removing one does not shift the next
		java.util.Collections.sort (all, new java.util.Comparator<RobotItem> ()
		{
			public int compare (RobotItem a, RobotItem b)		{ return b.index - a.index; }
		});
		for (RobotItem it : all)
			switch (it.kind)
			{
			case RobotItem.LINE:		robot.icon.remove (it.index);						any = true;		break;
			case RobotItem.BUMPER:		robot.bumpers.remove (it.index);					any = true;		break;
			case RobotItem.WHEEL:		robot.wheels.remove (it.index);						any = true;		break;
			case RobotItem.SENSOR:		robot.family (it.family).sensors.remove (it.index);	any = true;		break;
			case RobotItem.GROUP:		robot.groups.remove (it.index);						any = true;		break;
			case RobotItem.FUSED:		robot.fused.remove (it.index);						any = true;		break;
			case RobotItem.SCAN:		robot.scans.remove (it.index);						any = true;		break;
			default:					break;								// the sections themselves are not removable
			}
		if (!any)						return;
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

		DefaultMutableTreeNode	drive = new DefaultMutableTreeNode ("Drive train  (" + robot.wheels.size () + ")");
		drive.add (new ItemNode (new RobotItem (RobotItem.KINEMATICS, 0), "Kinematics"));
		for (int i = 0; i < robot.wheels.size (); i++)	drive.add (new ItemNode (new RobotItem (RobotItem.WHEEL, i), "Wheel " + i));
		treeRoot.add (drive);

		DefaultMutableTreeNode	lines = new DefaultMutableTreeNode ("Drawing  (" + robot.icon.size () + ")");
		for (int i = 0; i < robot.icon.size (); i++)		lines.add (new ItemNode (new RobotItem (RobotItem.LINE, i), "Line " + i));
		treeRoot.add (lines);

		DefaultMutableTreeNode	bumpers = new DefaultMutableTreeNode ("Bumpers  (" + robot.bumpers.size () + ")");
		// named as the description names them (bumxi0, bumyi0, ...)
		for (int i = 0; i < robot.bumpers.size (); i++)	bumpers.add (new ItemNode (new RobotItem (RobotItem.BUMPER, i), "bumper" + i));
		treeRoot.add (bumpers);

		// the virtual sensors: the ones of an area and the fused ones, each lot
		// named as the description names them (groupfeat0, virtulen0, ...), as a
		// sensor of a family is named after its family
		DefaultMutableTreeNode	virtual = new DefaultMutableTreeNode ("Virtual sensors  ("
										+ (robot.groups.size () + robot.fused.size () + robot.scans.size ()) + ")");
		ItemNode	areas = new ItemNode (new RobotItem (RobotItem.GROUPS, 0), "Area groups  (" + robot.groups.size () + ")");
		// what is worked out of the real sensors before anything reads them: the pair
		// a direction gives, and the fan a laser is taken down to
		ItemNode	filter = new ItemNode (new RobotItem (RobotItem.FILTERING, 0), "Sensor filtering  ("
										+ (robot.fused.size () + robot.scans.size ()) + ")");
		ItemNode	fused = new ItemNode (new RobotItem (RobotItem.FUSEDS, 0), "Sonar and infrared fusion  (" + robot.fused.size () + ")");
		ItemNode	scans = new ItemNode (new RobotItem (RobotItem.SCANS, 0), "Laser reduction  (" + robot.scans.size () + ")");

		for (int i = 0; i < robot.groups.size (); i++)	areas.add (new ItemNode (new RobotItem (RobotItem.GROUP, i), "group" + i));
		for (int i = 0; i < robot.fused.size (); i++)	fused.add (new ItemNode (new RobotItem (RobotItem.FUSED, i), "fusion" + i));
		for (int i = 0; i < robot.scans.size (); i++)	scans.add (new ItemNode (new RobotItem (RobotItem.SCAN, i), "scan" + i));
		filter.add (fused);
		filter.add (scans);
		virtual.add (areas);
		virtual.add (filter);
		treeRoot.add (virtual);

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

	/** A value that is not typed in -- worked out, or of no meaning here: shown on a grey ground. */
	static private class CalculatedRenderer extends javax.swing.table.DefaultTableCellRenderer
	{
		private static final long	serialVersionUID = 1L;

		static private final java.awt.Color		C_CALC = new java.awt.Color (238, 239, 242);

		public java.awt.Component getTableCellRendererComponent (JTable t, Object v, boolean sel, boolean focus, int r, int c)
		{
			java.awt.Component	comp = super.getTableCellRendererComponent (t, v, sel, focus, r, c);

			if (!sel)		comp.setBackground (C_CALC);
			return comp;
		}
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
	public void select (RobotItem it)
	{
		canvas.setSelection (it);
	}

	/** The view moved or turned an element: the model changed and the table follows. */
	public void elementChanged (RobotItem item)
	{
		changed ();
		propsModel.refresh ();
	}

	/** The view says where the cursor is. */
	public void cursorMoved (String where)
	{
		statusBar.setStatus (where);
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
		if (view3d != null)		view3d.setSelection (item);		// a sensor draws what it covers
		deleteAC.setEnabled (removable () > 0);
	}

	/** How many of the selected elements can be removed. */
	private int removable ()
	{
		int		n = 0;

		if (canvas.isCollectionSelected ())			return 0;
		for (RobotItem it : canvas.selected ())
			if ((it.kind == RobotItem.LINE) || (it.kind == RobotItem.BUMPER)
					|| (it.kind == RobotItem.SENSOR) || (it.kind == RobotItem.WHEEL)
					|| (it.kind == RobotItem.GROUP) || (it.kind == RobotItem.FUSED))
				n++;
		return n;
	}

	/* ------------------------------------------------------------------ */
	/* Properties                                                          */
	/* ------------------------------------------------------------------ */

	private void showProperties (RobotItem it)
	{
		stopEditing ();
		propsModel.setItem (it);
		propsBorder.setTitle ((it != null) ? title (it) : groupTitle ());
		((JComponent) propsTB.getParent ().getParent ().getParent ()).repaint ();
	}

	/** What the property table is headed with when a band picked several elements. */
	private String groupTitle ()
	{
		int		n = canvas.getGroup ().size ();

		return (n > 1) ? (n + " elements selected") : " ";
	}

	private String title (RobotItem it)
	{
		switch (it.kind)
		{
		case RobotItem.LINE:		return "Drawing line " + it.index;
		case RobotItem.BUMPER:		return "Bumpers: bumper" + it.index;
		case RobotItem.WHEEL:		return "Wheel " + it.index;
		case RobotItem.GROUP:		return "Area groups: group" + it.index;
		case RobotItem.GROUPS:		return "Area groups";
		case RobotItem.FUSED:		return "Sonar and infrared fusion: fusion" + it.index;
		case RobotItem.FUSEDS:		return "Sonar and infrared fusion";
		case RobotItem.SCAN:		return "Laser reduction: scan" + it.index;
		case RobotItem.SCANS:		return "Laser reduction";
		case RobotItem.SENSOR:		return RobotDef.familyName (it.family) + ": " + it.family + it.index;
		case RobotItem.FAMILY:		return RobotDef.familyName (it.family);
		default:					return RobotItem.NAMES[it.kind];
		}
	}

	/**
	 * Names of the properties of an element: what it says of itself, less
	 * anything the description does not keep (a transient field is of the running
	 * editor, not of the robot, so it is not for anybody to edit).
	 */
	public String[] propertyNames (RobotItem it)
	{
		List<String>	out = new ArrayList<String> ();

		for (String name : allPropertyNames (it))
			if (!isTransientProperty (name))		out.add (name);
		return out.toArray (new String[0]);
	}

	/**
	 * The properties of the kinematics: the model of the platform and, of the
	 * rest, only what that model reads. A differential drive says nothing of a
	 * steering wheel, and showing it would invite editing a value nobody uses.
	 */
	private String[] kinematicsNames ()
	{
		String[]		all = { DRIVE, MAX_SPEED, MAX_LAT_SPEED, MAX_TURN_RATE, MAX_STEER_RATE, MAX_ACCEL, MAX_DECEL,
								WHEEL_BASE, baseLabel (), STEER_OFFSET, SKID_FACTOR, WHEEL_DIAM,
								GEAR_RATIO, ENCODER_PULSES,
								"odom et", "odom er", "odom bias" };
		List<String>	out = new ArrayList<String> ();

		for (String name : all)
			if (RobotDef.usesKinematics (robot.kinematics.drive, kinKey (name)))	out.add (name);
		return out.toArray (new String[0]);
	}

	/**
	 * What the BASE of the platform is called: the track its two sides are built
	 * at, or, on a tricycle, how far its axle sits from the centre.
	 */
	private String baseLabel ()
	{
		return TRICYCLE.equals (robot.kinematics.drive) ? AXLE_OFFSET : TRACK;
	}

	/** True for a property held in a field the description does not keep. */
	static public boolean isTransientProperty (String name)
	{
		return (name != null) && TRANSIENT.contains (key (name));
	}

	static private String key (String name)					{ return name.replace (" ", "").toLowerCase (); }

	/** The transient fields of everything a description is made of. */
	static private final java.util.Set<String>	TRANSIENT = transientNames ();

	static private java.util.Set<String> transientNames ()
	{
		java.util.Set<String>	out = new java.util.HashSet<String> ();
		Class<?>[]				parts = { RobotDef.class, RobotDef.Sensor.class, RobotDef.Family.class,
										  RobotDef.Kinematics.class, RobotDef.Bumper.class, RobotDef.IconLine.class };

		for (Class<?> c : parts)
			for (java.lang.reflect.Field f : c.getDeclaredFields ())
				if (java.lang.reflect.Modifier.isTransient (f.getModifiers ()))		out.add (key (f.getName ()));
		return out;
	}

	private String[] allPropertyNames (RobotItem it)
	{
		if (it == null)				return new String[0];
		switch (it.kind)
		{
		case RobotItem.PLATFORM:	return new String[] { "name", "radius", "image", "robot shape", "actuator shape" };
		case RobotItem.KINEMATICS:	return kinematicsNames ();
		case RobotItem.LINE:
		case RobotItem.BUMPER:		return new String[] { "xi", "yi", "xf", "yf" };
		case RobotItem.WHEEL:		return new String[] { "x", "y", "z", "orientation",
														  "radius", "width",
														  "steerable", MAX_STEER, MAX_TURN, "traction", MAX_RPM };
		// a virtual sensor is read from the others, so it has no device and no step:
		// where it sits and what it covers is all of it
		case RobotItem.GROUP:		return new String[] { "rho", "theta", "height", "orientation", "elevation",
														  "range max", "range min", "cone" };
		case RobotItem.FUSED:		return new String[] { "rho", "theta", "height", "orientation", "elevation",
														  "range max", "range min", "cone" };
		case RobotItem.GROUPS:
		case RobotItem.SCANS:
		case RobotItem.FILTERING:	return new String[0];			// the lot of them says nothing of its own yet
		// how the fusion turns the sensors that look the same way into one reading
		// is of all the fused sensors at once, and not of any one of them
		case RobotItem.FUSEDS:		return new String[] { FUSION_MODE };
		// a reduced scan is a fan: where it is taken from, how wide it opens, how
		// far it reads and how many readings the rays of the laser come down to
		case RobotItem.SCAN:		return new String[] { SCAN_MODE, "rho", "theta", "height", "orientation", "elevation",
														  "range max", "cone", "rays" };
		case RobotItem.SENSOR:
			// the device it is read through comes first, then where it is and what it detects
			if (!RobotDef.hasOwnDetection (it.family))
				return new String[] { "step", "rho", "theta", "height", "orientation", "elevation" };
			if (it.family.equals ("lsb"))
				return new String[] { DRIVER, DRIVER_PARAMS, "step",
									  "rho", "theta", "height", "orientation", "elevation",
									  "range max", "range min", "cone", "rays", "reflect", "beacons" };
			if (it.family.equals ("trk"))
				return new String[] { DRIVER, DRIVER_PARAMS, "step",
									  "rho", "theta", "height", "orientation", "elevation",
									  "range max", "range min", "cone", "rays", "objects" };
			// a camera sees a rectangle: two fields of view, no cone and no near limit,
			// and one that hands over its frames says how many it takes in a second
			if (RobotDef.hasFov (it.family))
			{
				if (RobotDef.hasFrameRate (it.family))
					return new String[] { DRIVER, DRIVER_PARAMS, "step",
										  "rho", "theta", "height", "orientation", "elevation",
										  "range max", "hfov", "vfov", FRAME_RATE, RESOLUTION };
				return new String[] { DRIVER, DRIVER_PARAMS, "step",
									  "rho", "theta", "height", "orientation", "elevation",
									  "range max", "hfov", "vfov" };
			}
			return new String[] { DRIVER, DRIVER_PARAMS, "step",
								  "rho", "theta", "height", "orientation", "elevation",
								  "range max", "range min", "cone", "rays" };
		case RobotItem.FAMILY:
		{
			// how the simulator works their readings out comes first, where it has a say
			List<String>	names = new ArrayList<String> ();

			if (SimModes.has (it.family))		names.add (SIM_MODE);
			if (RobotDef.hasSimError (it.family))	names.add (SIM_ERROR);
			// only the firing cycle is of the whole family when its sensors say the rest
			if (RobotDef.hasOwnDetection (it.family))		names.add ("cycle");
			else
			{
				// the sonars and the infrared: what they all reach, and nothing of what only a scanner or a tracker says
				for (String n : new String[] { DRIVER, DRIVER_PARAMS, "range max", "range min", "cone", "cycle", "rays" })
					names.add (n);
			}
			return names.toArray (new String[0]);
		}
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
			if (name.equals ("image"))			return (robot.image != null) ? robot.image : "";
			if (name.equals ("robot shape"))	return (robot.shapeRobot != null) ? robot.shapeRobot : "";
			if (name.equals ("actuator shape"))	return (robot.shapeActuator != null) ? robot.shapeActuator : "";
			break;
		case RobotItem.KINEMATICS:
		{
			String	key = kinKey (name);					// what the description calls it

			if (name.equals (DRIVE))		return (k.drive != null) ? k.drive : "";
			if (RobotDef.isCalculated (key))						// the drive train says these
			{
				Double	v = robot.derived (key);
				return (v != null) ? RobotDef.fmt (v.doubleValue ()) : "";
			}
			if (key.equals ("lamax"))		return RobotDef.fmt (k.lamax);
			if (key.equals ("ldmax"))		return RobotDef.fmt (k.ldmax);
			if (key.equals ("rwheel"))		return RobotDef.fmt (k.rwheel);
			if (key.equals ("skid"))		return RobotDef.fmt (k.skid);
			if (key.equals ("gear"))		return RobotDef.fmt (k.gear);
			if (key.equals ("pulses"))		return RobotDef.fmt (k.pulses);
			if (key.equals ("odom et"))		return RobotDef.fmt (k.odomET);
			if (key.equals ("odom er"))		return RobotDef.fmt (k.odomER);
			if (key.equals ("odom bias"))	return RobotDef.fmt (k.odomBias);
			break;
		}
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
		case RobotItem.GROUP:
		case RobotItem.FUSED:
		{
			if (it.index >= canvas.sectors (it.kind).size ())		break;
			RobotDef.Sector		g = canvas.sectors (it.kind).get (it.index);
			if (name.equals ("rho"))			return RobotDef.fmt (g.rho);
			if (name.equals ("theta"))			return RobotDef.fmt (g.theta);
			if (name.equals ("height"))			return RobotDef.fmt (g.height);
			if (name.equals ("orientation"))	return RobotDef.fmt (g.orientation);
			if (name.equals ("elevation"))		return RobotDef.fmt (g.elevation);
			if (name.equals ("range max"))		return RobotDef.fmt (g.rangemax);
			if (name.equals ("range min"))		return RobotDef.fmt (g.rangemin);
			if (name.equals ("cone"))			return RobotDef.fmt (g.cone);
			break;
		}
		case RobotItem.FUSEDS:
			if (name.equals (FUSION_MODE))		return SimModes.fusionName (robot.fusionmode);
			break;
		case RobotItem.SCAN:
		{
			if (it.index >= robot.scans.size ())		break;
			RobotDef.Scanner	s = robot.scans.get (it.index);
			if (name.equals (SCAN_MODE))		return SimModes.reductionName (s.mode);
			if (name.equals ("rho"))			return RobotDef.fmt (s.rho);
			if (name.equals ("theta"))			return RobotDef.fmt (s.theta);
			if (name.equals ("height"))			return RobotDef.fmt (s.height);
			if (name.equals ("orientation"))	return RobotDef.fmt (s.orientation);
			if (name.equals ("elevation"))		return RobotDef.fmt (s.elevation);
			if (name.equals ("range max"))		return RobotDef.fmt (s.rangemax);
			if (name.equals ("cone"))			return RobotDef.fmt (s.cone);
			if (name.equals ("rays"))			return String.valueOf (s.rays);
			break;
		}
		case RobotItem.WHEEL:
		{
			RobotDef.Wheel		w = robot.wheels.get (it.index);
			if (name.equals ("x"))				return RobotDef.fmt (w.x);
			if (name.equals ("y"))				return RobotDef.fmt (w.y);
			if (name.equals ("z"))				return RobotDef.fmt (w.z);
			if (name.equals ("orientation"))	return RobotDef.fmt (w.orientation);
			if (name.equals ("radius"))			return RobotDef.fmt (w.radius);
			if (name.equals ("width"))			return RobotDef.fmt (RobotCanvas.kwidth (w));
			if (name.equals ("steerable"))		return String.valueOf (w.steerable);
			if (name.equals ("traction"))		return String.valueOf (w.traction);
			// how far and how fast say nothing of a wheel that is not steered, or does not drive
			if (name.equals (MAX_STEER))		return w.steerable ? RobotDef.fmt (w.maxsteer) : "";
			if (name.equals (MAX_TURN))			return w.steerable ? RobotDef.fmt (w.maxturning) : "";
			if (name.equals (MAX_RPM))			return w.traction ? RobotDef.fmt (w.maxrpm) : "";
			break;
		}
		case RobotItem.SENSOR:
		{
			RobotDef.Sensor		s = robot.family (it.family).sensors.get (it.index);
			if (name.equals ("rho"))			return RobotDef.fmt (s.rho);
			if (name.equals ("theta"))			return RobotDef.fmt (s.theta);
			if (name.equals ("height"))			return RobotDef.fmt (s.height);
			if (name.equals ("orientation"))	return RobotDef.fmt (s.orientation);
			if (name.equals ("elevation"))		return RobotDef.fmt (s.elevation);
			if (name.equals ("step"))			return String.valueOf (s.step);
			if (name.equals (DRIVER))			return (s.driver != null) ? s.driver : "";
			if (name.equals (DRIVER_PARAMS))	return (s.driverParams != null) ? s.driverParams : "";
			if (name.equals ("range max"))		return RobotDef.fmt (s.rangemax);
			if (name.equals ("range min"))		return RobotDef.fmt (s.rangemin);
			if (name.equals ("cone"))			return RobotDef.fmt (s.cone);
			if (name.equals ("rays"))			return String.valueOf (s.rays);
			if (name.equals ("reflect"))		return RobotDef.fmt (s.reflect);
			if (name.equals ("beacons"))		return String.valueOf (s.beacons);
			if (name.equals ("objects"))		return String.valueOf (s.objects);
			if (name.equals ("hfov"))			return RobotDef.fmt (s.hfov);
			if (name.equals ("vfov"))			return RobotDef.fmt (s.vfov);
			if (name.equals (FRAME_RATE))		return RobotDef.fmt (s.framerate);
			if (name.equals (RESOLUTION))		return (s.resolution != null) ? s.resolution : "";
			break;
		}
		case RobotItem.FAMILY:
		{
			RobotDef.Family		f = robot.family (it.family);
			if (name.equals (SIM_MODE))		return SimModes.name (it.family, f.simmode);
			// an error of none is not a number to show: the way it is worked out adds none
			if (name.equals (SIM_ERROR))	return usesSimError (it.family) ? percent (f.simerror) : "";
			if (name.equals (DRIVER))		return (f.driver != null) ? f.driver : "";
			if (name.equals (DRIVER_PARAMS))	return (f.driverParams != null) ? f.driverParams : "";
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

	/** True for the properties naming a 3D model file. */
	static public boolean isShapeProperty (String name)		{ return name.endsWith ("shape"); }
	/** The names the editor gives to the device a sensor is read through. */
	static public final String		DRIVER					= "driver class";
	/** The name the editor gives to the kinematics model of the platform. */
	static public final String		DRIVE					= "drive type";
	/** The names the editor gives to what a wheel can do. */
	static public final String		MAX_STEER				= "max steering";
	static public final String		MAX_TURN				= "max turning";
	/** The name the editor gives to the size of the driving wheel: it is a diameter, not a radius. */
	static public final String		WHEEL_DIAM				= "wheel diameter";
	static public final String		MAX_RPM					= "max rpm";
	static public final String		DRIVER_PARAMS			= "driver parameters";
	/** The name the editor gives to the way the simulator works the readings of a family out. */
	static public final String		SIM_MODE				= "simulation mode";
	/** How far off the simulator puts a reading, said in parts of a hundred. */
	static public final String		SIM_ERROR				= "simulation error";
	/** The name the editor gives to how the fusion works the fused sensors out. */
	static public final String		FUSION_MODE				= "fusion mode";
	/** How many frames a camera takes in a second. */
	static public final String		FRAME_RATE				= "frame rate";
	/** How large a frame of a camera is, in pixels. */
	static public final String		RESOLUTION				= "resolution";
	/** And to how it takes a bunch of laser rays down to one reading. */
	static public final String		SCAN_MODE				= "reduction mode";

	/** A share of the distance, as the parts of a hundred it is worth. */
	static private String percent (double share)	{ return String.format (java.util.Locale.US, "%.1f", share * 100.0); }
	/** And back: what is typed in hundredths is kept as the share the simulator reads. */
	static private double share (String value)		{ return num (value) / 100.0; }

	/*
	 * The names the editor gives to the kinematics, which the description knows by
	 * the short names its models read them with (vmax, lamax, lenght...). What each
	 * one stands for is in KIN_KEYS, and kinKey () is what turns one into the other,
	 * so that the editor can be read without the manual of the models at hand.
	 */
	static public final String		MAX_SPEED				= "max lin speed";			// vmax
	static public final String		MAX_LAT_SPEED			= "max lat speed";			// umax: only the models that
																						// go sideways are shown it
	static public final String		MAX_TURN_RATE			= "max turn rate";			// rmax
	static public final String		MAX_STEER_RATE			= "max steering rate";		// samax: how fast it steers, not how far
	static public final String		MAX_ACCEL				= "max acceleration";		// lamax
	static public final String		MAX_DECEL				= "max deceleration";		// ldmax
	static public final String		WHEEL_BASE				= "wheel base";				// length: between the two axles
	static public final String		TRACK					= "track width";			// base, on a platform with two sides
	static public final String		AXLE_OFFSET				= "rear axle offset";		// base, on a tricycle
	static public final String		STEER_OFFSET			= "steering wheel offset";	// rwheel
	static public final String		SKID_FACTOR				= "skid factor";			// skid
	static public final String		GEAR_RATIO				= "gear ratio";				// gear
	static public final String		ENCODER_PULSES			= "encoder pulses";			// pulses

	/** The kinematics model whose BASE is not a track but the distance from its axle to the centre. */
	static private final String		TRICYCLE				= "tc.vrobot.models.TricycleDrive";

	static private final java.util.Map<String, String>	KIN_KEYS = kinKeys ();

	static private java.util.Map<String, String> kinKeys ()
	{
		java.util.Map<String, String>	m = new java.util.HashMap<String, String> ();

		m.put (MAX_SPEED, "vmax");				m.put (MAX_LAT_SPEED, "umax");
		m.put (MAX_TURN_RATE, "rmax");
		m.put (MAX_STEER_RATE, "samax");		m.put (MAX_ACCEL, "lamax");
		m.put (MAX_DECEL, "ldmax");				m.put (WHEEL_BASE, "length");
		m.put (TRACK, "base");					m.put (AXLE_OFFSET, "base");
		m.put (STEER_OFFSET, "rwheel");			m.put (SKID_FACTOR, "skid");
		m.put (GEAR_RATIO, "gear");				m.put (ENCODER_PULSES, "pulses");
		return m;
	}

	/** The name the description knows a kinematics property by, whatever the editor calls it. */
	static public String kinKey (String label)
	{
		String		k = (label != null) ? KIN_KEYS.get (label) : null;

		return (k != null) ? k : label;
	}
	/** Width of the column of the units: enough for "deg/s" and no more. */
	static private final int		UNITS_WIDTH				= 44;
	/** True for the properties naming an image file. */
	static public boolean isImageProperty (String name)		{ return name.equals ("image"); }
	/** True for the properties naming a file. */
	static public boolean isFileProperty (String name)		{ return isShapeProperty (name) || isImageProperty (name); }

	/**
	 * Writes every file a description names the same way ("./conf/..." for the ones
	 * that live in the project), so that what the editor shows is what the file gets.
	 */
	static public void normalisePaths (RobotDef r)
	{
		if (r == null)		return;
		r.image				= FileCellEditor.normalise (r.image);
		r.shapeRobot		= FileCellEditor.normalise (r.shapeRobot);
		r.shapeActuator		= FileCellEditor.normalise (r.shapeActuator);
	}

	/** The value of a property as it is shown and stored: paths always as "./conf/...". */
	static private String value (String name, String v)
	{
		return isFileProperty (name) ? FileCellEditor.normalise (v) : v;
	}

	/**
	 * The chooser of a property whose value is a class: the classes of the
	 * development that derive from the one that part of the program asks for --
	 * the driver of a sensor, the kinematics model of the platform. Null when the
	 * property is not one of those, so that the plain field is used.
	 */
	private javax.swing.table.TableCellEditor classEditor (RobotItem it, String what)
	{
		JComboBox<String>	cb;
		List<String>		names;
		String				base, current;
		boolean				plain;								// built with no arguments, the way a driver is

		if (it == null)											return null;
		if (what.equals (DRIVE))
		{
			if (it.kind != RobotItem.KINEMATICS)				return null;
			base	= RobotDef.DRIVE_BASE;
			plain	= false;									// a model is built with the robot and its properties
		}
		else
		{
			if ((it.kind != RobotItem.SENSOR) && (it.kind != RobotItem.FAMILY))		return null;
			base	= RobotDef.driverBase (it.family);
			plain	= true;
		}
		if (base == null)										return null;

		names	= new ArrayList<String> (DriverClasses.of (base, plain));
		current	= getProperty (it, what);
		if ((current.length () > 0) && !names.contains (current))	names.add (0, current);
		if (plain)		names.add (0, "");						// a sensor may have no driver of its own,
																// while a platform always has a kinematics model

		cb		= new JComboBox<String> (names.toArray (new String[0]));
		cb.setSelectedItem (current);
		cb.setToolTipText ("Classes deriving from " + base);
		return new DefaultCellEditor (cb);
	}

	/**
	 * The editor of how the simulator works the readings of a family out: the ways
	 * it knows, by name. What a description says now comes first when it is not one
	 * of them -- a number written by hand is not to be lost by opening its editor.
	 */
	private javax.swing.table.TableCellEditor simModeEditor (String fam)
	{
		List<String>		names;
		String				current;
		JComboBox<String>	cb;

		if (!SimModes.has (fam))		return null;
		names	= SimModes.names (fam);
		current	= SimModes.name (fam, robot.family (fam).simmode);
		if (!names.contains (current))		names.add (0, current);
		cb		= new JComboBox<String> (names.toArray (new String[0]));
		cb.setSelectedItem (current);
		cb.setToolTipText ("How the simulator works a reading of this family out");
		return new DefaultCellEditor (cb);
	}

	/**
	 * The editor of how the fusion turns the sonar and the infrared that look the
	 * same way into the one reading of a fused sensor. What a description says now
	 * comes first when it is not one of the ways the fusion knows.
	 */
	private javax.swing.table.TableCellEditor fusionModeEditor ()
	{
		List<String>		names = SimModes.fusionNames ();
		String				current = SimModes.fusionName (robot.fusionmode);
		JComboBox<String>	cb;

		if (!names.contains (current))		names.add (0, current);
		cb		= new JComboBox<String> (names.toArray (new String[0]));
		cb.setSelectedItem (current);
		cb.setToolTipText ("How the fusion works the fused sensors out");
		return new DefaultCellEditor (cb);
	}

	/**
	 * The chooser of how large a frame of a camera is: the sizes that are usual,
	 * and whatever the description says now when it is not one of them. It takes a
	 * size typed by hand as well, since a camera may have any.
	 *
	 * Nothing said is a size of its own: the simulator then draws a frame as wide
	 * as it draws one and as tall as the two fields of view ask.
	 */
	private javax.swing.table.TableCellEditor resolutionEditor (RobotItem it)
	{
		List<String>		names = new ArrayList<String> ();
		String				current = (it != null) ? getProperty (it, RESOLUTION) : "";
		JComboBox<String>	cb;

		names.add ("");												// as the fields of view ask
		for (String r : RobotDef.RESOLUTIONS)		names.add (r);
		if ((current.length () > 0) && !names.contains (current))	names.add (1, current);
		cb		= new JComboBox<String> (names.toArray (new String[0]));
		cb.setEditable (true);										// any size, typed
		cb.setSelectedItem (current);
		cb.setToolTipText ("How large a frame is, as 640x480; empty: as wide as the simulator draws and as tall as the fields of view ask");
		return new DefaultCellEditor (cb);
	}

	/** A size of a frame as it is kept: as it was typed when it is a size, and nothing when it is not. */
	static private String resolution (String value)
	{
		int[]		res = RobotDef.resolutionOf (value);

		return (res != null) ? (res[0] + "x" + res[1]) : null;
	}

	/** The editor of how a bunch of laser rays is taken down to the one reading of a scan. */
	private javax.swing.table.TableCellEditor reductionEditor (int index)
	{
		List<String>		names = SimModes.reductionNames ();
		String				current = SimModes.reductionName ((index < robot.scans.size ()) ? robot.scans.get (index).mode : 0);
		JComboBox<String>	cb;

		if (!names.contains (current))		names.add (0, current);
		cb		= new JComboBox<String> (names.toArray (new String[0]));
		cb.setSelectedItem (current);
		cb.setToolTipText ("How a bunch of laser rays is taken down to one reading");
		return new DefaultCellEditor (cb);
	}

	/**
	 * True when the way the simulator works a family out reads the error it is
	 * given. Only a relative noise is a share of the distance; the ways that add
	 * nothing, and the two that model a device -- the lobes of a sonar and what an
	 * infrared reads off a surface -- never look at it, so the editor neither
	 * shows a number nor takes one.
	 *
	 * The gaussian noise is left as it is for now: what it spreads by is a
	 * separate property of its own (ERRORLRFGAUSS, ...).
	 */
	private boolean usesSimError (String fam)
	{
		String		mode;

		if ((fam == null) || !RobotDef.hasSimError (fam))		return false;
		mode	= SimModes.name (fam, robot.family (fam).simmode);
		return !SimModes.NONE.equals (mode) && !SimModes.RAYTRACING.equals (mode) && !SimModes.ABSORTION.equals (mode);
	}

	/** True when a property can be edited: what the wheels work out is not typed in. */
	public boolean isEditable (RobotItem it, String name)
	{
		if (isCalculated (it, name))		return false;
		if ((it != null) && (it.kind == RobotItem.FAMILY) && name.equals (SIM_ERROR))
			return usesSimError (it.family);
		if ((it != null) && (it.kind == RobotItem.WHEEL) && (it.index < robot.wheels.size ()))
		{
			RobotDef.Wheel	w = robot.wheels.get (it.index);

			if (name.equals (MAX_STEER) || name.equals (MAX_TURN))	return w.steerable;		// only a wheel that is steered
			if (name.equals (MAX_RPM))		return w.traction;		// only a wheel that drives
		}
		return true;
	}

	/**
	 * True for a property the description works out on its own -- what the drive
	 * train says of the kinematics. It is never typed in and never kept: a
	 * platform whose wheels cannot say it shows nothing.
	 */
	public boolean isCalculated (RobotItem it, String name)
	{
		if ((it == null) || (it.kind != RobotItem.KINEMATICS))		return false;
		return RobotDef.isCalculated (kinKey (name));
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
			else if (name.equals ("image"))
			{
				tc.vrobot.RobotImage.flush (robot.image);		// the view reads the new file
				robot.image = token (value);
			}
			else if (name.equals ("robot shape"))
			{
				ShapeLines.flush (robot.shapeRobot);			// the views read the new model
				robot.shapeRobot = token (value);
				updateViewBar ();
			}
			else if (name.equals ("actuator shape"))
			{
				ShapeLines.flush (robot.shapeActuator);
				robot.shapeActuator = token (value);
				updateViewBar ();
			}
			break;
		case RobotItem.KINEMATICS:
		{
			String	key = kinKey (name);					// what the description calls it

			if (name.equals (DRIVE))			k.drive = token (value);
			else if (key.equals ("lamax"))		k.lamax = num (value);
			else if (key.equals ("ldmax"))		k.ldmax = num (value);
			else if (key.equals ("rwheel"))		k.rwheel = num (value);
			else if (key.equals ("skid"))		k.skid = num (value);
			else if (key.equals ("gear"))		k.gear = num (value);
			else if (key.equals ("pulses"))		k.pulses = num (value);
			else if (key.equals ("odom et"))	k.odomET = num (value);
			else if (key.equals ("odom er"))	k.odomER = num (value);
			else if (key.equals ("odom bias"))	k.odomBias = num (value);
			break;
		}
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
		case RobotItem.GROUP:
		case RobotItem.FUSED:
		{
			if (it.index >= canvas.sectors (it.kind).size ())		break;
			RobotDef.Sector		g = canvas.sectors (it.kind).get (it.index);
			if (name.equals ("rho"))				g.rho = num (value);
			else if (name.equals ("theta"))			g.theta = num (value);
			else if (name.equals ("height"))		g.height = num (value);
			else if (name.equals ("orientation"))	g.orientation = num (value);
			else if (name.equals ("elevation"))		g.elevation = num (value);
			else if (name.equals ("range max"))		g.rangemax = num (value);
			// no farther than the far end, or neither the sector nor its handle would
			// show what was typed
			else if (name.equals ("range min"))		g.rangemin = Math.max (0.0, Math.min (num (value), g.rangemax));
			else if (name.equals ("cone"))			g.cone = num (value);
			break;
		}
		case RobotItem.FUSEDS:
			if (name.equals (FUSION_MODE))		robot.fusionmode = SimModes.fusionMode (value);
			break;
		case RobotItem.SCAN:
		{
			if (it.index >= robot.scans.size ())		break;
			RobotDef.Scanner	s = robot.scans.get (it.index);
			if (name.equals (SCAN_MODE))			s.mode = SimModes.reductionMode (value);
			else if (name.equals ("rho"))			s.rho = num (value);
			else if (name.equals ("theta"))			s.theta = num (value);
			else if (name.equals ("height"))		s.height = num (value);
			else if (name.equals ("orientation"))	s.orientation = num (value);
			else if (name.equals ("elevation"))		s.elevation = num (value);
			else if (name.equals ("range max"))		s.rangemax = num (value);
			else if (name.equals ("cone"))			s.cone = num (value);
			else if (name.equals ("rays"))			s.rays = (int) num (value);
			break;
		}
		case RobotItem.WHEEL:
		{
			RobotDef.Wheel		w = robot.wheels.get (it.index);
			if (name.equals ("x"))					w.x = num (value);
			else if (name.equals ("y"))				w.y = num (value);
			else if (name.equals ("z"))				w.z = num (value);
			else if (name.equals ("orientation"))	w.orientation = num (value);
			else if (name.equals ("radius"))		w.radius = num (value);
			else if (name.equals ("width"))			w.width = num (value);
			else if (name.equals ("steerable"))		w.steerable = flag (value);
			else if (name.equals ("traction"))		w.traction = flag (value);
			else if (name.equals (MAX_STEER))		{ if (w.steerable)	w.maxsteer = num (value); }
			else if (name.equals (MAX_TURN))		{ if (w.steerable)	w.maxturning = num (value); }
			else if (name.equals (MAX_RPM))			{ if (w.traction)	w.maxrpm = num (value); }
			break;
		}
		case RobotItem.SENSOR:
		{
			RobotDef.Sensor		s = robot.family (it.family).sensors.get (it.index);
			if (name.equals ("rho"))				s.rho = num (value);
			else if (name.equals ("theta"))			s.theta = num (value);
			else if (name.equals ("height"))		s.height = num (value);
			else if (name.equals ("orientation"))	s.orientation = num (value);
			else if (name.equals ("elevation"))		s.elevation = num (value);
			else if (name.equals ("step"))			s.step = (int) num (value);
			else if (name.equals (DRIVER))			s.driver = token (value);
			else if (name.equals (DRIVER_PARAMS))	s.driverParams = token (value);
			else if (name.equals ("range max"))		s.rangemax = num (value);
			else if (name.equals ("range min"))		s.rangemin = num (value);
			else if (name.equals ("cone"))			s.cone = num (value);
			else if (name.equals ("rays"))			s.rays = (int) num (value);
			else if (name.equals ("reflect"))		s.reflect = num (value);
			else if (name.equals ("beacons"))		s.beacons = (int) num (value);
			else if (name.equals ("objects"))		s.objects = (int) num (value);
			else if (name.equals ("hfov"))			s.hfov = num (value);
			else if (name.equals ("vfov"))			s.vfov = num (value);
			else if (name.equals (FRAME_RATE))		s.framerate = num (value);
			else if (name.equals (RESOLUTION))		s.resolution = resolution (value);
			break;
		}
		case RobotItem.FAMILY:
		{
			RobotDef.Family		f = robot.family (it.family);
			if (name.equals (SIM_MODE))			f.simmode = SimModes.mode (it.family, value);
			else if (name.equals (SIM_ERROR))	f.simerror = share (value);
			else if (name.equals (DRIVER))		f.driver = token (value);
			else if (name.equals (DRIVER_PARAMS))	f.driverParams = token (value);
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
	static private boolean flag (String v)			{ return Boolean.parseBoolean (v.trim ()); }

	/** True for the properties that are either true or false. */
	static public boolean isBooleanProperty (String name)
	{
		return name.equals ("steerable") || name.equals ("traction");
	}

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

		String nameAt (int r)						{ return names[r]; }

		public int getRowCount ()					{ return names.length; }
		public int getColumnCount ()				{ return 3; }
		public String getColumnName (int c)			{ return (c == 0) ? "Property" : (c == 1) ? "Value" : "Units"; }
		public boolean isCellEditable (int r, int c)	{ return (c == 1) && (item != null) && isEditable (item, names[r]); }

		public Object getValueAt (int r, int c)
		{
			if (c == 0)			return names[r];
			if (c == 2)			return Units.of (names[r]);
			return value (names[r], getProperty (item, names[r]));
		}

		public void setValueAt (Object v, int r, int c)
		{
			try
			{
				String	name = names[r];

				setProperty (item, name, value (name, (v == null) ? "" : v.toString ()));
				if (name.equals (DRIVE))		setItem (item);		// another model reads other properties
				// a way of working a reading out that adds nothing leaves the error
				// with nothing to say, so the row below is redrawn as well
				else if (name.equals (SIM_MODE))	fireTableDataChanged ();
				else							fireTableRowsUpdated (r, r);
				if ((item.kind == RobotItem.PLATFORM) && names[r].equals ("name"))		refreshTree ();
			} catch (IllegalArgumentException e)
			{
				JOptionPane.showMessageDialog (RobotEditorPanel.this, e.getMessage (), TITLE, JOptionPane.ERROR_MESSAGE);
			}
		}

		void refresh ()								{ fireTableDataChanged (); }
	}
}

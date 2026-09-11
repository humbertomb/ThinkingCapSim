/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tcsimulator;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import tc.ExecArch;
import tc.shared.linda.ItemDebug;
import tc.shared.world.World;
import tc.vrobot.RobotData;
import tc.vrobot.RobotDesc;
import tcapps.tceditor.StatusBar;
import tcapps.tceditor.ToolButtons;
import tcapps.tceditor.ToolIcon;
import tcapps.tceditor.View3DController;
import tcapps.tceditor.WorldCanvas;
import tcapps.tceditor.WorldEdit;
import tcapps.tceditor.WorldItem;
import tcapps.tcsimulator.simulator.Simulator;
import tcapps.tcsimulator.simulator.SimulatorDesc;
import tcapps.tcsimulator.simulator.SimulatorListener;
import tcapps.tcsimulator.simulator.objects.SimObject;
import tclib.planning.sequence.Sequence;
import wucore.utils.geom.Line2;
import wucore.utils.geom.Point3;

/**
 * Main window of the new ThinkingCap simulator. Everything the simulator
 * runs (modules, robot type, world, ...) comes from an architecture
 * definition file (conf/archs/*.arch) managed through {@link ExecArch};
 * the world shown is the one of the architecture's virtual robot. The
 * visualisation is shared with the editor: the 2D view is a read-only
 * {@link WorldCanvas} and the 3D view a {@link View3DController}.
 * Functionality will be added incrementally.
 */
public class TCSimulatorWindow extends JFrame implements WorldCanvas.Listener, SimulatorListener, WorldCanvas.Overlay
{
	private static final long		serialVersionUID = 1L;

	static public final String		TITLE		= "ThinkingCap Simulator";
	static public final String		MAPS_DIR	= "./conf/maps";
	static public final String		ARCHS_DIR	= "./conf/archs";

	protected ExecArch				arch;					// Architecture in use (never null)
	protected World					world;					// World of the architecture's virtual robot
	protected File					worldFile;
	protected boolean				worldModified;			// world changed since the architecture was loaded/saved

	protected ExecArch				running;				// Architecture being executed (null when none)
	protected Simulator				simulator;				// Simulation engine of the running architecture
	protected List<RobotView>		robots	= new ArrayList<RobotView> ();		// simulated robots being displayed
	protected Sequence				lastTasks;				// last task set edited (shown again when the dialog reopens)

	/** A simulated robot as seen by the window: description, last data and its index in the 3D view. */
	protected static class RobotView
	{
		RobotDesc		rdesc;
		SimulatorDesc	sdesc;
		RobotData		data;			// last data received (null until the first update)
		int				index3d	= -1;	// index in the 3D view (-1: not added yet)
	}

	protected WorldCanvas			canvas;
	protected StatusBar				statusBar;
	protected View3DController		view3d;
	protected Action				executeAction, startAction, stepAction, stopAction;

	public TCSimulatorWindow ()
	{
		super (TITLE);
		arch	= ExecArch.create ();
		world	= WorldEdit.newWorld ();

		buildGUI ();
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
		canvas.setEditable (false);
		canvas.setListener (this);
		canvas.setOverlay (this);

		statusBar	= new StatusBar ();
		view3d		= new View3DController (this, canvas);

		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (buildToolBar (), BorderLayout.WEST);
		getContentPane ().add (canvas, BorderLayout.CENTER);
		getContentPane ().add (statusBar, BorderLayout.SOUTH);
		setJMenuBar (buildMenuBar ());
	}

	private JToolBar buildToolBar ()
	{
		JToolBar	tb = new JToolBar (JToolBar.VERTICAL);
		tb.setFloatable (false);

		tb.add (ToolButtons.flatButton (openArchAction ()));
		tb.add (ToolButtons.flatButton (openWorldAction ()));
		tb.add (ToolButtons.flatButton (tasksAction ()));
		tb.addSeparator ();
		tb.add (ToolButtons.flatButton (ToolButtons.zoomFit (canvas)));
		tb.add (ToolButtons.flatButton (ToolButtons.zoomIn (canvas)));
		tb.add (ToolButtons.flatButton (ToolButtons.zoomOut (canvas)));
		tb.addSeparator ();
		executeAction	= ToolButtons.action ("Execute", ToolIcon.EXECUTE, "Execute the architecture (restarts it if running)  [F5]", new Runnable () { public void run () { execute (); } });
		startAction		= ToolButtons.action ("Start", ToolIcon.RUN, "Start  [F6]", new Runnable () { public void run () { command (ItemDebug.START); } });
		stepAction		= ToolButtons.action ("Step", ToolIcon.STEP, "Step  [F7]", new Runnable () { public void run () { command (ItemDebug.STEP); } });
		stopAction		= ToolButtons.action ("Stop", ToolIcon.STOP, "Stop  [F8]", new Runnable () { public void run () { command (ItemDebug.STOP); } });
		tb.add (ToolButtons.flatButton (executeAction));
		tb.add (ToolButtons.flatButton (startAction));
		tb.add (ToolButtons.flatButton (stepAction));
		tb.add (ToolButtons.flatButton (stopAction));
		updateExecutionState ();

		// --- 3D view toggle, at the bottom of the toolbar
		tb.add (Box.createVerticalGlue ());
		tb.addSeparator ();
		tb.add (view3d.button ());
		return tb;
	}

	private Action openArchAction ()
	{
		return ToolButtons.action ("Load Architecture...", ToolIcon.FOLDER, "Load architecture  [Ctrl+O]", new Runnable () { public void run () { loadArch (); } });
	}

	private Action openWorldAction ()
	{
		return ToolButtons.action ("Change World...", ToolIcon.WORLD, "Change the world of the architecture  [Ctrl+W]", new Runnable () { public void run () { loadWorld (); } });
	}

	private Action tasksAction ()
	{
		return ToolButtons.action ("Tasks...", ToolIcon.TASKS, "Tasks: edit and send a task set to the robot  [Ctrl+T]", new Runnable () { public void run () { editTasks (); } });
	}

	private JMenuBar buildMenuBar ()
	{
		int			mask = Toolkit.getDefaultToolkit ().getMenuShortcutKeyMaskEx ();
		JMenuBar	mb = new JMenuBar ();

		JMenu		mfile = new JMenu ("File");
		mfile.add (item ("New Architecture", KeyEvent.VK_N, mask, new Runnable () { public void run () { newArch (); } }));
		JMenuItem	load = new JMenuItem (openArchAction ());
		load.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_O, mask));
		mfile.add (load);
		mfile.add (item ("Save Architecture", KeyEvent.VK_S, mask, new Runnable () { public void run () { saveArch (false); } }));
		mfile.add (item ("Save Architecture As...", KeyEvent.VK_S, mask | KeyEvent.SHIFT_DOWN_MASK, new Runnable () { public void run () { saveArch (true); } }));
		mfile.addSeparator ();
		JMenuItem	wld = new JMenuItem (openWorldAction ());
		wld.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_W, mask));
		mfile.add (wld);
		mfile.addSeparator ();
		JMenuItem	quit = new JMenuItem ("Quit");
		quit.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_Q, mask));
		quit.addActionListener (new java.awt.event.ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ quit (); }
		});
		mfile.add (quit);
		mb.add (mfile);

		JMenu		mexec = new JMenu ("Execution");
		mexec.add (accel (new JMenuItem (executeAction), KeyEvent.VK_F5, 0));
		mexec.addSeparator ();
		mexec.add (accel (new JMenuItem (startAction), KeyEvent.VK_F6, 0));
		mexec.add (accel (new JMenuItem (stepAction), KeyEvent.VK_F7, 0));
		mexec.add (accel (new JMenuItem (stopAction), KeyEvent.VK_F8, 0));
		mexec.addSeparator ();
		JMenuItem	tasks = new JMenuItem (tasksAction ());
		tasks.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_T, mask));
		mexec.add (tasks);
		mexec.addSeparator ();
		mexec.add (item ("Terminate", KeyEvent.VK_F5, KeyEvent.SHIFT_DOWN_MASK, new Runnable () { public void run () { terminate (); } }));
		mb.add (mexec);

		JMenu		mview = new JMenu ("View");
		JMenuItem	fit = new JMenuItem (ToolButtons.zoomFit (canvas));
		fit.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_0, mask));
		mview.add (fit);
		mview.add (new JMenuItem (ToolButtons.zoomIn (canvas)));
		mview.add (new JMenuItem (ToolButtons.zoomOut (canvas)));
		mview.addSeparator ();
		mview.add (view3d.menuItem (mask));
		mb.add (mview);

		return mb;
	}

	private JMenuItem accel (JMenuItem mi, int key, int mask)
	{
		mi.setAccelerator (KeyStroke.getKeyStroke (key, mask));
		return mi;
	}

	private JMenuItem item (String name, int key, int mask, final Runnable body)
	{
		JMenuItem	mi = new JMenuItem (name);
		mi.setAccelerator (KeyStroke.getKeyStroke (key, mask));
		mi.addActionListener (new java.awt.event.ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ body.run (); }
		});
		return mi;
	}

	private JFileChooser chooser (File current, String defDir, String ext, String desc)
	{
		File		dir = (current != null) ? current.getParentFile () : new File (defDir);
		if ((dir == null) || !dir.isDirectory ())		dir = new File (".");
		JFileChooser	fc = new JFileChooser (dir);
		fc.setFileFilter (new FileNameExtensionFilter (desc, ext));
		return fc;
	}

	/* ------------------------------------------------------------------ */
	/* Architectures                                                       */
	/* ------------------------------------------------------------------ */

	public ExecArch getArch ()			{ return arch; }

	private boolean isModified ()		{ return worldModified || arch.isModified (); }

	/** Asks what to do with unsaved changes; false when the user cancels. */
	private boolean confirmDiscard ()
	{
		if (!isModified ())				return true;
		int		r = JOptionPane.showConfirmDialog (this, "The architecture has unsaved changes. Save them first?",
					TITLE, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (r == JOptionPane.CANCEL_OPTION)	return false;
		if (r == JOptionPane.YES_OPTION)		return saveArch (false);
		return true;
	}

	public void newArch ()
	{
		if (!confirmDiscard ())			return;
		setArch (ExecArch.create ());
	}

	public void loadArch ()
	{
		if (!confirmDiscard ())			return;
		JFileChooser	fc = chooser (arch.getFile (), ARCHS_DIR, "arch", "Architecture definition files (*.arch)");
		fc.setDialogTitle ("Load Architecture");
		if (fc.showOpenDialog (this) != JFileChooser.APPROVE_OPTION)		return;
		loadArch (fc.getSelectedFile ());
	}

	public void loadArch (File f)
	{
		try
		{
			setArch (ExecArch.load (f));
		} catch (Exception e)
		{
			e.printStackTrace ();
			JOptionPane.showMessageDialog (this, "Cannot load " + f.getName () + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Installs an architecture and shows the world of its virtual robot. */
	private void setArch (ExecArch a)
	{
		arch			= a;
		worldModified	= false;
		String	wname = arch.getWorldFile ();
		if (wname != null)		showWorld (new File (wname));
		else					showWorld (null);
		updateTitle ();
	}

	public boolean saveArch (boolean saveAs)
	{
		File	f = arch.getFile ();
		if (saveAs || (f == null))
		{
			JFileChooser	fc = chooser (f, ARCHS_DIR, "arch", "Architecture definition files (*.arch)");
			fc.setDialogTitle (saveAs ? "Save Architecture As" : "Save Architecture");
			if (f != null)		fc.setSelectedFile (f);
			if (fc.showSaveDialog (this) != JFileChooser.APPROVE_OPTION)		return false;
			f = fc.getSelectedFile ();
			if (!f.getName ().toLowerCase ().endsWith (".arch"))		f = new File (f.getPath () + ".arch");
			if (f.exists () && (JOptionPane.showConfirmDialog (this, f.getName () + " already exists. Overwrite?", TITLE,
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION))		return false;
		}
		try
		{
			arch.save (f);
			worldModified	= false;
			updateTitle ();
			return true;
		} catch (Exception e)
		{
			e.printStackTrace ();
			JOptionPane.showMessageDialog (this, "Cannot save " + f.getName () + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	/* ------------------------------------------------------------------ */
	/* Execution                                                           */
	/* ------------------------------------------------------------------ */

	/** Identifier of the simulated robot: architecture file name in upper case plus "-1" (e.g. IFORK-1). */
	protected String robotId ()
	{
		File	f = arch.getFile ();
		String	n = (f != null) ? f.getName () : "robot";
		int		dot = n.lastIndexOf ('.');
		if (dot > 0)		n = n.substring (0, dot);
		return n.toUpperCase () + "-1";
	}

	/**
	 * Executes the current architecture in simulation (its virtual robot runs
	 * as a SimRobot inside a new Simulator loaded with the architecture's
	 * world); a running execution is terminated first.
	 */
	public void execute ()
	{
		terminate ();
		simulator	= new Simulator ();
		running		= arch.runner (robotId (), simulator);		// loads the architecture's world into the simulator
		simulator.setVisualization (this);							// robots and objects are reported to this window
		running.start ();
		statusBar.setStatus ("Executing " + robotId () + " (" + ((arch.getFile () != null) ? arch.getFile ().getName () : "untitled") + ")");
		updateExecutionState ();
	}

	/** Stops the modules and Linda servers of the running architecture. */
	public void terminate ()
	{
		if (running == null)			return;
		running.terminate ();
		if (simulator != null)		simulator.closeVisualization3D ();		// stops the refresh thread
		running		= null;
		simulator	= null;
		synchronized (robots) { robots.clear (); }
		view3d.clearRobots ();
		canvas.repaint ();
		statusBar.setStatus ("Execution terminated");
		updateExecutionState ();
	}

	/** Sends a start/step/stop command to the modules (as the monitor's execution control). */
	public void command (int cmd)
	{
		if (running == null)			return;
		if (!running.sendCommand (cmd))
			JOptionPane.showMessageDialog (this, "The architecture has no local Linda space to send commands to.", TITLE, JOptionPane.WARNING_MESSAGE);
	}

	/** Opens the task set editor and sends the resulting plan to the running robot. */
	public void editTasks ()
	{
		TaskDialog	dlg = new TaskDialog (this, TaskDialog.placesOf (world), lastTasks);
		Sequence	seq = dlg.showDialog ();
		if (seq == null)				return;
		lastTasks = seq;
		if (running == null)
		{
			JOptionPane.showMessageDialog (this, "Execute the architecture before sending tasks.", TITLE, JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (!running.sendPlan (seq))
			JOptionPane.showMessageDialog (this, "The architecture has no local Linda space to send the plan to.", TITLE, JOptionPane.WARNING_MESSAGE);
		else
			statusBar.setStatus ("Plan sent to " + robotId () + ": " + seq);
	}

	private void updateExecutionState ()
	{
		boolean	on = (running != null);
		startAction.setEnabled (on);
		stepAction.setEnabled (on);
		stopAction.setEnabled (on);
	}

	/* ------------------------------------------------------------------ */
	/* SimulatorListener: robots and objects reported by the Simulator      */
	/* (called from the module / refresh threads)                           */
	/* ------------------------------------------------------------------ */

	public void setWorldmap (World map)
	{
		// the simulator works on its own World instance loaded from the same file the window shows
	}

	public int addRobot (RobotDesc rdesc, SimulatorDesc sdesc)
	{
		RobotView	rv = new RobotView ();
		rv.rdesc	= rdesc;
		rv.sdesc	= sdesc;
		synchronized (robots)
		{
			robots.add (rv);
			return robots.size () - 1;
		}
	}

	public void updateData (int roboindex, RobotData data)
	{
		synchronized (robots)
		{
			if ((roboindex < 0) || (roboindex >= robots.size ()))		return;
			robots.get (roboindex).data = data;
		}
	}

	public int addObject (SimObject object)					{ return -1; }		// scene objects: not displayed yet
	public int addObject (SimObject object, Point3 pos, double a)	{ return -1; }
	public void removeObject (int objindex)					{ }
	public void removeAllObjects ()								{ }
	public void updateObjectData (int objindex, Point3 pt, double a)	{ }

	/** End of a simulator refresh cycle: redraw the 2D view and move the 3D robots (on the event thread). */
	public void repaint ()
	{
		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()
			{
				canvas.repaint ();
				update3DRobots ();
			}
		});
	}

	private void update3DRobots ()
	{
		if (!view3d.isVisible ())		return;
		synchronized (robots)
		{
			for (RobotView rv : robots)
			{
				if (rv.data == null)		continue;
				if (rv.index3d < 0)		rv.index3d = view3d.addRobot (rv.rdesc, rv.sdesc, rv.data.real_x, rv.data.real_y, rv.data.real_a);
				view3d.updateRobot (rv.index3d, rv.data);
			}
		}
	}

	/* ------------------------------------------------------------------ */
	/* WorldCanvas.Overlay: robots on the 2D view                           */
	/* ------------------------------------------------------------------ */

	static private final Color		C_ROBOT		= new Color (30, 90, 200);
	static private final Color		C_ROBOT_FILL	= new Color (30, 90, 200, 60);

	public void paint (Graphics2D g, WorldCanvas c)
	{
		synchronized (robots)
		{
			for (RobotView rv : robots)
				if (rv.data != null)		drawRobot (g, c, rv);
		}
	}

	private void drawRobot (Graphics2D g, WorldCanvas c, RobotView rv)
	{
		double		x = rv.data.real_x, y = rv.data.real_y, a = rv.data.real_a;
		double		ca = Math.cos (a), sa = Math.sin (a);
		Line2[]		icon = rv.rdesc.icon;

		g.setStroke (new BasicStroke (2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		if ((icon != null) && (icon.length > 0))
		{
			// icon segments (robot frame) placed at the robot pose; closed outlines get a light fill
			Path2D	path = new Path2D.Double ();
			for (Line2 l : icon)
			{
				double	x1 = x + l.orig ().x () * ca - l.orig ().y () * sa, y1 = y + l.orig ().x () * sa + l.orig ().y () * ca;
				double	x2 = x + l.dest ().x () * ca - l.dest ().y () * sa, y2 = y + l.dest ().x () * sa + l.dest ().y () * ca;
				path.moveTo (c.toPixelX (x1), c.toPixelY (y1));
				path.lineTo (c.toPixelX (x2), c.toPixelY (y2));
			}
			g.setColor (C_ROBOT_FILL);
			g.fill (path);
			g.setColor (C_ROBOT);
			g.draw (path);
		}
		else
		{
			double	r = Math.max (3.0, rv.rdesc.RADIUS * c.getScale ());
			g.setColor (C_ROBOT_FILL);
			g.fillOval ((int) Math.round (c.toPixelX (x) - r), (int) Math.round (c.toPixelY (y) - r), (int) Math.round (2 * r), (int) Math.round (2 * r));
			g.setColor (C_ROBOT);
			g.drawOval ((int) Math.round (c.toPixelX (x) - r), (int) Math.round (c.toPixelY (y) - r), (int) Math.round (2 * r), (int) Math.round (2 * r));
		}
		// heading
		double	len = Math.max (0.5, rv.rdesc.RADIUS * 1.5);
		g.setColor (C_ROBOT);
		g.draw (new Line2D.Double (c.toPixelX (x), c.toPixelY (y), c.toPixelX (x + len * ca), c.toPixelY (y + len * sa)));
		g.fillOval (c.toPixelX (x) - 3, c.toPixelY (y) - 3, 6, 6);
	}

	/* ------------------------------------------------------------------ */
	/* Worlds                                                              */
	/* ------------------------------------------------------------------ */

	/** Changes the world of the architecture's virtual robot (VROBOT+"WORLD" property). */
	public void loadWorld ()
	{
		JFileChooser	fc = chooser (worldFile, MAPS_DIR, "world", "World maps (*.world)");
		fc.setDialogTitle ("Change World");
		if (fc.showOpenDialog (this) != JFileChooser.APPROVE_OPTION)		return;
		loadWorld (fc.getSelectedFile ());
	}

	public void loadWorld (File f)
	{
		if (!showWorld (f))				return;
		arch.setWorldFile (relativePath (f));
		worldModified	= true;
		updateTitle ();
	}

	/** Loads and displays a world (null: empty world); false if the file cannot be read. */
	private boolean showWorld (File f)
	{
		World	w;
		if (f == null)
			w = WorldEdit.newWorld ();
		else
		{
			try
			{
				w = new World (f.getPath ());
			} catch (Exception e)
			{
				e.printStackTrace ();
				JOptionPane.showMessageDialog (this, "Cannot load world " + f.getName () + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		world		= w;
		worldFile	= f;
		canvas.setWorld (world);
		canvas.zoomToFit ();
		view3d.worldChanged ();
		return true;
	}

	/** Path relative to the working directory when possible (as used in the .arch files), with '/' separators. */
	static private String relativePath (File f)
	{
		String	path = f.getAbsolutePath ();
		String	base = new File (".").getAbsoluteFile ().getParentFile ().getPath ();
		if (path.startsWith (base + File.separator))		path = path.substring (base.length () + 1);
		return path.replace (File.separatorChar, '/');
	}

	public World getWorld ()			{ return world; }
	public WorldCanvas getCanvas ()		{ return canvas; }

	private void updateTitle ()
	{
		File	f = arch.getFile ();
		setTitle (TITLE + " - " + ((f != null) ? f.getName () : "untitled.arch") + (isModified () ? " *" : ""));
	}

	public void quit ()
	{
		if (!confirmDiscard ())			return;
		terminate ();
		view3d.dispose ();
		dispose ();
		System.exit (0);
	}

	/* ------------------------------------------------------------------ */
	/* WorldCanvas.Listener                                                */
	/* ------------------------------------------------------------------ */

	public void selectionChanged (WorldItem item)
	{
		view3d.selectionChanged (item);
	}

	public void worldChanged (String what)		{ view3d.worldChanged (); }		// not expected: the canvas is read-only
	public void worldPreview ()					{ view3d.worldPreview (); }
	public void statusChanged (String text)		{ statusBar.setStatus (text); }
	public void usageChanged (String text)		{ statusBar.setUsage (text); }
	public void toolFinished ()					{ canvas.setTool (WorldCanvas.T_SELECT); }
	public void toolRequested (int tool)		{ }

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
				TCSimulatorWindow	win = new TCSimulatorWindow ();
				win.setVisible (true);
				if (name != null)		win.loadArch (new File (name));
				else					win.canvas.zoomToFit ();
			}
		});
	}
}

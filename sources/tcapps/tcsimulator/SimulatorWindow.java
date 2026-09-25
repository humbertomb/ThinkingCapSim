/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tcsimulator;

import tc.DeployArch;
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
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import tc.ExecArch;
import tc.shared.linda.ItemExecution;
import tc.shared.world.WMAObject;
import tc.shared.world.World;
import tc.vrobot.RobotData;
import tc.vrobot.RobotDesc;
import tcapps.tceditor.StatusBar;
import tcapps.tceditor.DeploymentDialog;
import tcapps.tceditor.ToolButtons;
import tcapps.tceditor.ToolIcon;
import tcapps.tceditor.View3DController;
import tcapps.tceditor.WorldCanvas;
import tcapps.tceditor.WorldEditor;
import tcapps.tceditor.WorldEditorDialog;
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
 * ({@link DeployArch}, conf/deploy/*.deploy);
 * the world shown is the one of the architecture's virtual robot. The
 * visualisation is shared with the editor: the 2D view is a read-only
 * {@link WorldCanvas} and the 3D view a {@link View3DController}.
 * Functionality will be added incrementally.
 */
public class SimulatorWindow extends JFrame implements WorldCanvas.Listener, SimulatorListener, WorldCanvas.Overlay
{
	private static final long		serialVersionUID = 1L;

	static public final String		TITLE		= "ThinkingCap Simulator";
	static public final String		MAPS_DIR	= "./conf/maps";
	static public final String		DEPLOY_DIR	= "./conf/deploy";		// deployment architectures (.deploy)

	protected DeployArch			deploy;					// Deployment architecture in use (never null)
	protected World					world;					// World of the architecture's virtual robot
	protected File					worldFile;
	protected boolean				worldModified;			// world changed since the architecture was loaded/saved

	protected List<ExecArch>		running;				// Robots being executed, one ExecArch each (null when none)
	protected Simulator				simulator;				// Simulation engine of the running architecture
	protected List<RobotView>		robots	= new ArrayList<RobotView> ();		// simulated robots being displayed
	protected List<ObjectView>		objects	= new ArrayList<ObjectView> ();		// simulated animated objects being displayed (null entries: removed)
	/**
	 * Where the animated objects of the world were before the execution, to put
	 * them back afterwards: while it runs, the objects of the canvas stand for
	 * where the simulation has them, and a hand may move them there.
	 */
	protected List<double[]>		aobjectsPoses	= new ArrayList<double[]> ();
	protected Sequence				lastTasks;				// last task set edited (shown again when the dialog reopens)

	protected boolean				showPath;				// draw the route every robot has taken
	protected boolean				showPose;				// and the robot itself along it, now and then

	/** How the route of a robot is kept and how often it is stamped with a pose of the robot. */
	static private final double		TRAIL_STEP	= 0.02;						// a step of the route is kept every two centimetres
	static private final double		TRAIL_TURN	= Math.toRadians (5.0);		// or every five degrees it turns on the spot
	static private final int		TRAIL_MAX	= 20000;					// and this many steps at most, the oldest going first
	static private final long		POSE_MS		= 3000;						// a pose of the robot every three seconds of it

	/** A simulated robot as seen by the window: description, last data and its index in the 3D view. */
	protected static class RobotView
	{
		RobotDesc		rdesc;
		SimulatorDesc	sdesc;
		String			name;			// robot identifier (null when the simulator did not give one)
		RobotData		data;			// last data received (null until the first update)
		int				index3d	= -1;	// index in the 3D view (-1: not added yet)

		/** Where it has been, in order, and which of those are stamped with a pose. */
		List<Pose>		trail	= new ArrayList<Pose> ();
		long			posed;			// when the last pose was stamped (ms; 0: none yet)

		/** One step of the route: where the robot was, and whether it is drawn there. */
		static class Pose
		{
			double		x, y, a;
			boolean		stamp;

			Pose (double x, double y, double a, boolean stamp)
			{
				this.x = x;		this.y = y;		this.a = a;		this.stamp = stamp;
			}
		}
	}

	/** A simulated animated object as seen by the window: the simulator object, its last pose and its index in the 3D view. */
	protected static class ObjectView
	{
		SimObject		obj;
		Point3			pos;			// last pose reported by the simulator
		double			a;
		int				index3d	= -1;	// index in the 3D view (-1: not added yet)
	}

	protected WorldCanvas			canvas;
	protected RobotMonitorPanel		monitorPanel;			// Robots / Events tabs (as in the TCMonitor)
	protected JSplitPane			splitPane;
	protected StatusBar				statusBar;
	protected View3DController		view3d;
	protected Action				executeAction, resetAction, startAction, stepAction, stopAction, tasksAction;

	public SimulatorWindow ()
	{
		super (TITLE);
		deploy	= DeployArch.create ();
		world	= WorldEditor.newWorld ();

		buildGUI ();
		updateTitle ();
		// the windows the modules open (with local graphics) are placed by this one
		tc.runtime.thread.StdThread.setHostFrame (this);

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
		// a waypoint can still be aimed: the path that reaches it depends on the way
		// it faces, and turning it is how one is tried out. The world of a simulation
		// is never written back, so what is turned here is lost with the execution
		canvas.setOrientable (true);
		// the lines of the floor are there to be seen and not to be picked: a click
		// on the field goes through them to the ball, the robot or the floor
		canvas.setKindSelectable (WorldItem.MARKING, false);
		canvas.setListener (this);
		canvas.setOverlay (this);

		statusBar	= new StatusBar ();
		view3d		= new View3DController (this, canvas);

		// world view on top, Robots / Events tables below (as the monitor's main panel)
		monitorPanel	= new RobotMonitorPanel ();
		monitorPanel.setWorld (world);
		splitPane		= new JSplitPane (JSplitPane.VERTICAL_SPLIT, canvas, monitorPanel);
		splitPane.setOneTouchExpandable (true);
		splitPane.setResizeWeight (1.0 - BOTTOM_FRACTION);		// keep the same proportion when the window is resized
		// initial proportion once the split pane has a real height
		splitPane.addComponentListener (new java.awt.event.ComponentAdapter ()
		{
			boolean	done = false;
			public void componentResized (java.awt.event.ComponentEvent e)
			{
				if (done || (splitPane.getHeight () <= 0))		return;
				done = true;
				SwingUtilities.invokeLater (new Runnable ()
				{
					public void run ()		{ resetDivider (); }
				});
			}
		});

		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (buildToolBar (), BorderLayout.WEST);
		getContentPane ().add (splitPane, BorderLayout.CENTER);
		getContentPane ().add (statusBar, BorderLayout.SOUTH);
		setJMenuBar (buildMenuBar ());
	}

	private JToolBar buildToolBar ()
	{
		JToolBar	tb = new JToolBar (JToolBar.VERTICAL);
		tb.setFloatable (false);

		tb.add (ToolButtons.flatButton (openArchAction ()));
		tb.add (ToolButtons.flatButton (openWorldAction ()));
		tb.addSeparator ();
		tb.add (ToolButtons.flatButton (editWorldAction ()));
		tb.add (ToolButtons.flatButton (editArchAction ()));
		tasksAction	= tasksAction ();
		tb.add (ToolButtons.flatButton (tasksAction));
		tb.addSeparator ();
		tb.add (ToolButtons.flatButton (ToolButtons.zoomFit (canvas)));
		tb.add (ToolButtons.flatButton (ToolButtons.zoomIn (canvas)));
		tb.add (ToolButtons.flatButton (ToolButtons.zoomOut (canvas)));
		tb.addSeparator ();
		executeAction	= ToolButtons.action ("Execute", ToolIcon.EXECUTE, "Execute the architecture (restarts it if running)  [F5]", new Runnable () { public void run () { execute (); } });
		resetAction		= ToolButtons.action ("Reset", ToolIcon.RESET, "Reset: the modules start afresh, without being taken out of execution  [F6]", new Runnable () { public void run () { command (ItemExecution.RESET); } });
		startAction		= ToolButtons.action ("Start", ToolIcon.RUN, "Start  [F7]", new Runnable () { public void run () { command (ItemExecution.START); } });
		stepAction		= ToolButtons.action ("Step", ToolIcon.STEP, "Step  [F8]", new Runnable () { public void run () { command (ItemExecution.STEP); } });
		stopAction		= ToolButtons.action ("Stop", ToolIcon.STOP, "Stop  [F9]", new Runnable () { public void run () { command (ItemExecution.STOP); } });
		tb.add (ToolButtons.flatButton (executeAction));
		tb.add (ToolButtons.flatButton (resetAction));
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

	static public final double		BOTTOM_FRACTION	= 0.22;		// initial share of the Robots/Events panel

	/** Puts the split divider so that the Robots/Events panel takes {@link #BOTTOM_FRACTION} of the height. */
	public void resetDivider ()
	{
		splitPane.setDividerLocation (1.0 - BOTTOM_FRACTION);
	}

	private Action openArchAction ()
	{
		return ToolButtons.action ("Load Deployment...", ToolIcon.FOLDER, "Load deployment  [Ctrl+O]", new Runnable () { public void run () { loadDeployment (); } });
	}

	private Action openWorldAction ()
	{
		return ToolButtons.action ("Change World...", ToolIcon.WORLD, "Change the world of the architecture  [Ctrl+W]", new Runnable () { public void run () { loadWorld (); } });
	}

	private Action editWorldAction ()
	{
		return ToolButtons.action ("Edit World...", ToolIcon.EDIT_WORLD, "Edit the world of the deployment", new Runnable () { public void run () { editWorld (); } });
	}

	private Action editArchAction ()
	{
		return ToolButtons.action ("Edit Deployment...", ToolIcon.ARCHITECTURE, "Edit the deployment: Linda spaces, robots and modules  [Ctrl+E]", new Runnable () { public void run () { editArchitecture (); } });
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
		mfile.add (item ("New Deployment", KeyEvent.VK_N, mask, new Runnable () { public void run () { newArch (); } }));
		mfile.add (item ("Load Deployment...", KeyEvent.VK_O, mask, new Runnable () { public void run () { loadDeployment (); } }));
		mfile.add (item ("Save Deployment", KeyEvent.VK_S, mask, new Runnable () { public void run () { saveArch (false); } }));
		mfile.add (item ("Save Deployment As...", KeyEvent.VK_S, mask | KeyEvent.SHIFT_DOWN_MASK, new Runnable () { public void run () { saveArch (true); } }));
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
		mexec.add (accel (new JMenuItem (resetAction), KeyEvent.VK_F6, 0));
		mexec.add (accel (new JMenuItem (startAction), KeyEvent.VK_F7, 0));
		mexec.add (accel (new JMenuItem (stepAction), KeyEvent.VK_F8, 0));
		mexec.add (accel (new JMenuItem (stopAction), KeyEvent.VK_F9, 0));
		mexec.addSeparator ();
		JMenuItem	tasks = new JMenuItem (tasksAction);
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
		// what a robot has done, which the live view of it does not say: where it went
		// and how it stood along the way
		mview.add (check ("Show robot path", showPath, new java.awt.event.ItemListener ()
		{
			public void itemStateChanged (java.awt.event.ItemEvent e)
			{
				showPath	= (e.getStateChange () == java.awt.event.ItemEvent.SELECTED);
				canvas.repaint ();
			}
		}));
		mview.add (check ("Show robot pose", showPose, new java.awt.event.ItemListener ()
		{
			public void itemStateChanged (java.awt.event.ItemEvent e)
			{
				showPose	= (e.getStateChange () == java.awt.event.ItemEvent.SELECTED);
				canvas.repaint ();
			}
		}));
		mview.addSeparator ();
		mview.add (view3d.menuItem (mask));
		mb.add (mview);

		return mb;
	}

	/** A menu item that is either on or off. */
	private javax.swing.JCheckBoxMenuItem check (String text, boolean on, java.awt.event.ItemListener l)
	{
		javax.swing.JCheckBoxMenuItem	mi = new javax.swing.JCheckBoxMenuItem (text, on);

		mi.addItemListener (l);
		return mi;
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
	/* Deployment architectures                                            */
	/* ------------------------------------------------------------------ */

	public DeployArch getDeploy ()		{ return deploy; }

	private boolean isModified ()		{ return worldModified || deploy.isModified (); }

	/** Asks what to do with unsaved changes; false when the user cancels. */
	private boolean confirmDiscard ()
	{
		if (!isModified ())				return true;
		int		r = JOptionPane.showConfirmDialog (this, "The deployment architecture has unsaved changes. Save them first?",
					TITLE, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (r == JOptionPane.CANCEL_OPTION)	return false;
		if (r == JOptionPane.YES_OPTION)		return saveArch (false);
		return true;
	}

	public void newArch ()
	{
		if (!confirmDiscard ())			return;
		terminate ();
		setDeploy (DeployArch.create ());
	}

	public void loadDeployment ()
	{
		if (!confirmDiscard ())			return;
		JFileChooser	fc = chooser (deploy.getFile (), DEPLOY_DIR, DeployArch.EXTENSION, "Deployment architectures (*.deploy)");
		fc.setDialogTitle ("Load Deployment");
		if (fc.showOpenDialog (this) != JFileChooser.APPROVE_OPTION)		return;
		loadDeployment (fc.getSelectedFile ());
	}

	/** Loads a deployment. */
	public void loadDeployment (File f)
	{
		terminate ();
		try
		{
			setDeploy (DeployArch.load (f));
		} catch (Exception e)
		{
			e.printStackTrace ();
			JOptionPane.showMessageDialog (this, "Cannot load " + f.getName () + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Installs a deployment and shows the world of its first robot. */
	private void setDeploy (DeployArch d)
	{
		deploy			= d;
		worldModified	= false;
		lastTasks		= null;					// the remembered task set names places and robots of the previous deployment
		String	wname = deploy.getWorldFile ();
		if (wname != null)		showWorld (new File (wname));
		else					showWorld (null);
		updateTitle ();
		updateTasksState ();					// another deployment, other planners (or none)
	}

	public boolean saveArch (boolean saveAs)
	{
		File	f = deploy.getFile ();
		if (saveAs || (f == null))
		{
			JFileChooser	fc = chooser (f, DEPLOY_DIR, DeployArch.EXTENSION, "Deployment architectures (*.deploy)");
			fc.setDialogTitle (saveAs ? "Save Deployment As" : "Save Deployment");
			if (f != null)		fc.setSelectedFile (f);
			if (fc.showSaveDialog (this) != JFileChooser.APPROVE_OPTION)		return false;
			f = fc.getSelectedFile ();
			if (!f.getName ().toLowerCase ().endsWith ("." + DeployArch.EXTENSION))		f = new File (f.getPath () + "." + DeployArch.EXTENSION);
			if (f.exists () && (JOptionPane.showConfirmDialog (this, f.getName () + " already exists. Overwrite?", TITLE,
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION))		return false;
		}
		try
		{
			deploy.save (f);
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

	/**
	 * Opens the block editor of the deployment. The dialog works on a copy;
	 * on OK it replaces the current deployment and the world is reloaded when
	 * the first robot now points to another one.
	 */
	public void editArchitecture ()
	{
		DeploymentDialog	dlg = new DeploymentDialog (this, deploy);
		DeployArch			result = dlg.showDialog ();
		if (result == null)				return;
		terminate ();												// the running robots no longer match the deployment
		setDeploy (result);											// the copy keeps file and saved state (the editor may have loaded or saved another file)
	}

	/* ------------------------------------------------------------------ */
	/* Execution                                                           */
	/* ------------------------------------------------------------------ */

	/** Identifier of the simulated robot: the name of the first robot of the deployment. */
	protected String robotId ()
	{
		return deploy.robots.isEmpty () ? DeployArch.DEFAULT_ROBOT : deploy.robots.get (0).name;
	}

	/** Names of the robots of the deployment, in order. */
	/**
	 * The class of the planner of a robot of the deployment, or null when it has
	 * none: the class of the module the deployment tagged as a Planner.
	 */
	protected String plannerOf (DeployArch.Robot rob)
	{
		if (rob == null)					return null;
		for (DeployArch.Module m : rob.modules)
			if ("Planner".equalsIgnoreCase (m.get ("TYPE")))		return m.get ("CLASS");
		return null;
	}

	/**
	 * The robots a task set can be sent to, each with the planner it runs: the
	 * ones the deployment gives a planner that understands some action. A robot
	 * with no planner has nothing to do with a task, and one whose planner
	 * understands nothing has nothing that could be asked of it.
	 */
	protected java.util.Map<String, String> planners ()
	{
		java.util.Map<String, String>	all = new java.util.LinkedHashMap<String, String> ();

		if (deploy == null)					return all;
		for (DeployArch.Robot rob : deploy.robots)
		{
			String	cls = plannerOf (rob);
			if ((cls != null) && (TaskDialog.actionsOf (cls).length > 0))		all.put (rob.name, cls);
		}
		return all;
	}

	protected String[] robotNames ()
	{
		String[]	n = new String[deploy.robots.size ()];
		for (int i = 0; i < n.length; i++)		n[i] = deploy.robots.get (i).name;
		return n;
	}

	/**
	 * Executes every robot of the deployment in simulation: each one is an
	 * ExecArch (its virtual robot runs as a SimRobot) sharing one Simulator
	 * loaded with the deployment world. The robots are started in order, each
	 * one once the previous is running (the first hosts the global Linda space
	 * the others connect to). A running execution is terminated first.
	 */
	public void execute ()
	{
		terminate ();
		List<String>	problems = deploy.validate ();
		if (!problems.isEmpty ())
		{
			JOptionPane.showMessageDialog (this, String.join ("\n", problems) + "\n\nEdit the deployment architecture to fix it.", TITLE, JOptionPane.WARNING_MESSAGE);
			return;
		}
		simulator	= new Simulator ();
		final List<ExecArch>	execs = new ArrayList<ExecArch> ();
		for (int i = 0; i < deploy.robots.size (); i++)
		{
			execs.add (new ExecArch (deploy, i, simulator));		// loads the world into the simulator and takes the start point of the robot
		}
		running		= execs;
		// while simulating, the animated objects are live: drawn (overlay / 3D) where
		// the simulation has them and not at their initial pose, and moved by hand
		// there to see what the modules make of it. The objects of the canvas follow
		// the simulation meanwhile, and are put back where the world has them after
		aobjectsPoses.clear ();
		for (WMAObject o : world.aobjects ())		aobjectsPoses.add (new double[] { o.pos.x (), o.pos.y (), o.pos.z (), o.a });
		canvas.setKindLive (WorldItem.AOBJECT, true);
		view3d.setAnimatedVisible (false);
		simulator.setVisualization (this);							// robots and objects are reported to this window
		monitorPanel.clear ();
		new Thread (new Runnable ()
		{
			public void run ()
			{
				for (ExecArch r : execs)
				{
					if (running != execs)		return;					// terminated meanwhile
					r.start ();
					// wait until its modules run (and its local Linda exists) before the next robot
					for (int i = 0; (i < 200) && !r.isRunning () && (running == execs); i++)
						try { Thread.sleep (50); } catch (InterruptedException e) { return; }
					if ((running == execs) && (r.getLocalLinda () != null))		monitorPanel.attach (r.getLocalLinda (), r.getRobotId ());
				}
			}
		}, "TCSim-launcher").start ();
		statusBar.setStatus ("Executing " + String.join (", ", robotNames ()) + " (" + ((deploy.getFile () != null) ? deploy.getFile ().getName () : "untitled") + ")");
		updateExecutionState ();
	}

	/** Stops the modules and Linda servers of the running robots. */
	public void terminate ()
	{
		if (running == null)			return;
		List<ExecArch>	execs = running;
		for (ExecArch r : execs)		r.sendCommand (ItemExecution.STOP);		// stop the modules before tearing them down
		running		= null;
		monitorPanel.detach ();
		for (int i = execs.size () - 1; i >= 0; i--)				// last first: the first robot hosts the global Linda space
		{
			ExecArch	r = execs.get (i);
			if (r.isRunning () || (r.getLocalLinda () != null))		r.terminate ();
		}
		if (simulator != null)		simulator.dispose ();		// stops the refresh and object threads
		simulator	= null;
		synchronized (robots) { robots.clear (); }
		synchronized (objects) { objects.clear (); }
		view3d.clearRobots ();
		view3d.clearObjects ();
		view3d.setAnimatedVisible (true);
		canvas.setKindLive (WorldItem.AOBJECT, false);
		// back where the world has them: what the simulation did to them is over
		for (int i = 0; (i < aobjectsPoses.size ()) && (i < world.aobjects ().size ()); i++)
		{
			double[]	q = aobjectsPoses.get (i);

			WorldEditor.setObjectPose (world.aobjects ().get (i), q[0], q[1], q[2], q[3]);
		}
		aobjectsPoses.clear ();
		canvas.repaint ();
		canvas.repaint ();
		statusBar.setStatus ("Execution terminated");
		updateExecutionState ();
	}

	/** Sends a start/step/stop command to the modules of every robot (as the monitor's execution control). */
	public void command (int cmd)
	{
		if (running == null)			return;
		boolean	sent = false;
		for (ExecArch r : running)		sent |= r.sendCommand (cmd);
		if (!sent)
			JOptionPane.showMessageDialog (this, "No robot has a local Linda space to send commands to yet.", TITLE, JOptionPane.WARNING_MESSAGE);
	}

	/** The running ExecArch of a robot name, or null. */
	private ExecArch runningRobot (String name)
	{
		if ((running == null) || (name == null))		return null;
		for (ExecArch r : running)		if (name.equals (r.getRobotId ()))		return r;
		return null;
	}

	/** Opens the task set editor and sends the resulting plan to the chosen robot. */
	public void editTasks ()
	{
		java.util.Map<String, String>	with = planners ();

		if (with.isEmpty ())
		{
			JOptionPane.showMessageDialog (this, "No robot of this deployment has a planner to send tasks to.", TITLE, JOptionPane.WARNING_MESSAGE);
			return;
		}
		TaskDialog	dlg = new TaskDialog (this, world, lastTasks, with);
		Sequence	seq = dlg.showDialog ();
		if (seq == null)				return;
		lastTasks = seq;
		String		robot = dlg.getRobot ();
		ExecArch	r = runningRobot (robot);
		if (r == null)
		{
			JOptionPane.showMessageDialog (this, "Execute the deployment before sending tasks to " + robot + ".", TITLE, JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (!r.sendPlan (seq))
			JOptionPane.showMessageDialog (this, "The robot " + robot + " has no local Linda space to send the plan to.", TITLE, JOptionPane.WARNING_MESSAGE);
		else
			statusBar.setStatus ("Plan sent to " + robot + ": " + seq);
	}

	private void updateExecutionState ()
	{
		boolean	on = (running != null);
		resetAction.setEnabled (on);
		startAction.setEnabled (on);
		stepAction.setEnabled (on);
		stopAction.setEnabled (on);
		updateTasksState ();
	}

	/**
	 * Tasks are offered only when there is somebody to send them to: a robot of
	 * the deployment with a planner that understands some action.
	 */
	private void updateTasksState ()
	{
		if (tasksAction != null)			tasksAction.setEnabled (!planners ().isEmpty ());
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
		return addRobot (rdesc, sdesc, null);
	}

	public int addRobot (RobotDesc rdesc, SimulatorDesc sdesc, String name)
	{
		RobotView	rv = new RobotView ();
		rv.rdesc	= rdesc;
		rv.sdesc	= sdesc;
		rv.name		= name;
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
			RobotView	rv = robots.get (roboindex);
			rv.data		= data;
			trail (rv, data);
		}
	}

	/**
	 * Keeps where a robot has been: a step of the route every couple of
	 * centimetres it moves or few degrees it turns, which is enough to draw the
	 * route without keeping a point per cycle, and one of those stamped every
	 * three seconds, which is where the robot itself is drawn.
	 *
	 * A robot standing still adds nothing, so the route of a robot that has
	 * stopped neither grows nor piles poses up on one spot.
	 */
	private void trail (RobotView rv, RobotData data)
	{
		long				now = System.currentTimeMillis ();
		RobotView.Pose		last = rv.trail.isEmpty () ? null : rv.trail.get (rv.trail.size () - 1);
		boolean				stamp;

		if (last != null)
		{
			double	dx = data.real_x - last.x, dy = data.real_y - last.y;
			double	da = Math.abs (wucore.utils.math.Angles.radnorm_180 (data.real_a - last.a));

			if ((Math.sqrt (dx * dx + dy * dy) < TRAIL_STEP) && (da < TRAIL_TURN))		return;
		}
		stamp	= (rv.posed == 0) || ((now - rv.posed) >= POSE_MS);
		if (stamp)		rv.posed = now;
		rv.trail.add (new RobotView.Pose (data.real_x, data.real_y, data.real_a, stamp));
		while (rv.trail.size () > TRAIL_MAX)		rv.trail.remove (0);
	}

	public int addObject (SimObject object)
	{
		return addObject (object, object.odesc.pos, object.odesc.a);
	}

	public int addObject (SimObject object, Point3 pos, double a)
	{
		ObjectView	ov = new ObjectView ();
		ov.obj	= object;
		ov.pos	= new Point3 (pos);
		ov.a	= a;
		synchronized (objects)
		{
			objects.add (ov);
			return objects.size () - 1;
		}
	}

	public void removeObject (int objindex)
	{
		synchronized (objects)
		{
			if ((objindex >= 0) && (objindex < objects.size ()))		objects.set (objindex, null);
		}
	}

	public void removeAllObjects ()
	{
		synchronized (objects) { objects.clear (); }
		SwingUtilities.invokeLater (new Runnable () { public void run () { view3d.clearObjects (); } });
	}

	public void updateObjectData (int objindex, Point3 pt, double a)
	{
		synchronized (objects)
		{
			if ((objindex < 0) || (objindex >= objects.size ()) || (objects.get (objindex) == null))		return;
			ObjectView	ov = objects.get (objindex);
			ov.pos	= new Point3 (pt);
			ov.a	= a;
		}
		// the object of the canvas stands for where the simulation has it, so that it
		// is picked and dragged there
		WMAObject	o = liveObject (objindex);

		if (o != null)		WorldEditor.setObjectPose (o, pt.x (), pt.y (), pt.z (), a);
	}

	/**
	 * The object of the world in the canvas that stands for a live object of the
	 * simulation: the same one, in the same place of the same file, read twice.
	 */
	private WMAObject liveObject (int objindex)
	{
		ObjectView	ov;

		synchronized (objects)
		{
			if ((objindex < 0) || (objindex >= objects.size ()))		return null;
			ov	= objects.get (objindex);
		}
		if ((ov == null) || (objindex >= world.aobjects ().size ()))	return null;

		WMAObject	o = world.aobjects ().get (objindex);

		// the same object of the same world, or nothing: names that do not agree say the lists do not either
		if ((o.label != null) && (ov.obj.odesc.label != null) && !o.label.equals (ov.obj.odesc.label))		return null;
		return o;
	}

	/** A live object was moved or turned by hand on the canvas: the simulation takes it from there. */
	public void liveChanged (WorldItem item)
	{
		if ((item == null) || (item.kind != WorldItem.AOBJECT) || (simulator == null))		return;

		WMAObject	o = liveObject (item.index);

		if (o == null)		return;
		simulator.placeObject (item.index, o.pos.x (), o.pos.y (), o.a);
		// and the overlay draws it there at once, without waiting for the simulation to say so
		synchronized (objects)
		{
			ObjectView	ov = objects.get (item.index);

			ov.pos	= new Point3 (o.pos);
			ov.a	= o.a;
		}
		canvas.repaint ();
	}

	/** End of a simulator refresh cycle: redraw the 2D view and move the 3D robots (on the event thread). */
	public void repaint ()
	{
		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()
			{
				canvas.repaint ();
				update3DRobots ();
				update3DObjects ();
			}
		});
	}

	private void update3DObjects ()
	{
		if (!view3d.isVisible ())		return;
		synchronized (objects)
		{
			for (ObjectView ov : objects)
			{
				if (ov == null)			continue;
				if (ov.index3d < 0)		ov.index3d = view3d.addObject (ov.obj.odesc, ov.pos.x (), ov.pos.y (), ov.pos.z (), ov.a);
				view3d.updateObject (ov.index3d, ov.pos.x (), ov.pos.y (), ov.pos.z (), ov.a);
			}
		}
	}

	private void update3DRobots ()
	{
		if (!view3d.isVisible ())		return;
		synchronized (robots)
		{
			for (RobotView rv : robots)
			{
				if (rv.data == null)		continue;
				if (rv.index3d < 0)		rv.index3d = view3d.addRobot (rv.rdesc, rv.sdesc, rv.data.real_x, rv.data.real_y, rv.data.real_a, rv.name);
				view3d.updateRobot (rv.index3d, rv.data);
			}
		}
	}

	/* ------------------------------------------------------------------ */
	/* WorldCanvas.Overlay: robots on the 2D view                           */
	/* ------------------------------------------------------------------ */

	static private final Color		C_ROBOT		= new Color (30, 90, 200);
	static private final Color		C_ROBOT_FILL	= new Color (30, 90, 200, 60);
	static private final Color		C_PATH		= new Color (200, 60, 40, 170);		// the route it has taken
	static private final float		POSE_ALPHA	= 0.28f;							// how faintly it is drawn along it

	public void paint (Graphics2D g, WorldCanvas c)
	{
		synchronized (objects)
		{
			for (ObjectView ov : objects)
				if (ov != null)				drawObject (g, c, ov);
		}
		synchronized (robots)
		{
			// what a robot has done goes under it: the route first, then the poses along
			// it, and the robot as it is now on top of both
			if (showPath)
				for (RobotView rv : robots)		drawPath (g, c, rv);
			if (showPose)
				for (RobotView rv : robots)		drawPoses (g, c, rv);
			for (RobotView rv : robots)
				if (rv.data != null)		drawRobot (g, c, rv);
		}
	}

	/** A live animated object: its icon at the simulated pose, its virtual radius and its name (as the robots). */
	private void drawObject (Graphics2D g, WorldCanvas c, ObjectView ov)
	{
		WMAObject	o = ov.obj.odesc;
		Color		col = wucore.utils.color.ColorTool.fromWColorToColor (o.color);
		double		x = ov.pos.x (), y = ov.pos.y ();
		WorldItem	sel = c.getSelection ();

		// the one picked on the canvas is drawn as picked, as the canvas draws its own
		if ((sel != null) && (sel.kind == WorldItem.AOBJECT) && (objects.indexOf (ov) == sel.index))		col = WorldCanvas.C_SEL;

		g.setStroke (new BasicStroke (1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor (col);
		if (drawObjectImage (g, c, o, x, y, ov.a))		// the bitmap already stands for the drawing of the object
			;
		else if (o.icon != null)
			for (Line2 l : o.icon.toAbsolute (ov.pos, ov.a))
				g.draw (new Line2D.Double (c.toPixelX (l.orig ().x ()), c.toPixelY (l.orig ().y ()), c.toPixelX (l.dest ().x ()), c.toPixelY (l.dest ().y ())));
		if (ov.obj.radius > 0.0)
		{
			double	r = ov.obj.radius * c.getScale ();
			g.setColor (new Color (col.getRed (), col.getGreen (), col.getBlue (), 90));
			g.setStroke (new BasicStroke (1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[] { 5f, 4f }, 0f));
			g.draw (new java.awt.geom.Ellipse2D.Double (c.toPixelX (x) - r, c.toPixelY (y) - r, 2 * r, 2 * r));
		}
		g.setColor (col);
		g.fillOval (c.toPixelX (x) - 3, c.toPixelY (y) - 3, 6, 6);
		if (o.label != null)
		{
			double	r = Math.max (ov.obj.radius, 0.15);
			int		tx = (int) Math.round (c.toPixelX (x + r * 0.7)) + 4, ty = (int) Math.round (c.toPixelY (y + r * 0.7)) - 4;
			g.setFont (g.getFont ().deriveFont (java.awt.Font.BOLD, 12f));
			g.setColor (new Color (255, 255, 255, 200));
			for (int dx = -1; dx <= 1; dx++)	for (int dy = -1; dy <= 1; dy++)	if ((dx != 0) || (dy != 0))	g.drawString (o.label, tx + dx, ty + dy);
			g.setColor (col);
			g.drawString (o.label, tx, ty);
		}
	}

	/**
	 * The bitmap of an object, drawn over the box its icon occupies and turned
	 * with it.
	 *
	 * @return whether the image was drawn
	 */
	private boolean drawObjectImage (Graphics2D g, WorldCanvas c, WMAObject o, double x, double y, double a)
	{
		java.awt.Image	img = wucore.utils.image.PlanImage.get (o.image);
		double[]		b;
		double			cx, cy, ox, oy;

		if (img == null)				return false;
		b	= wucore.utils.image.PlanImage.bounds (o.getLocalIcon ());
		if (b == null)					return false;
		cx	= (b[0] + b[2]) / 2;		cy = (b[1] + b[3]) / 2;			// centre of the box, in the frame of the object
		ox	= x + cx * Math.cos (a) - cy * Math.sin (a);
		oy	= y + cx * Math.sin (a) + cy * Math.cos (a);
		wucore.utils.image.PlanImage.draw (g, img, c.toPixelX (ox), c.toPixelY (oy),
											(b[2] - b[0]) * c.getScale (), (b[3] - b[1]) * c.getScale (), a);
		return true;
	}

	private void drawRobot (Graphics2D g, WorldCanvas c, RobotView rv)
	{
		double		x = rv.data.real_x, y = rv.data.real_y, a = rv.data.real_a;
		double		ca = Math.cos (a), sa = Math.sin (a);

		drawBody (g, c, rv, x, y, a);
		// heading: on past the circle of the robot 30% of its diameter
		double	len = rv.rdesc.RADIUS * (1.0 + WorldCanvas.HEADING);
		g.setColor (C_ROBOT);
		g.draw (new Line2D.Double (c.toPixelX (x), c.toPixelY (y), c.toPixelX (x + len * ca), c.toPixelY (y + len * sa)));
		g.fillOval (c.toPixelX (x) - 3, c.toPixelY (y) - 3, 6, 6);
		// name, beside the robot (top-right of its bounding circle), with a light halo for readability
		if (rv.name != null)
		{
			double	r = Math.max (0.5, rv.rdesc.RADIUS);
			int		tx = (int) Math.round (c.toPixelX (x + r * 0.7)) + 4, ty = (int) Math.round (c.toPixelY (y + r * 0.7)) - 4;
			g.setFont (g.getFont ().deriveFont (java.awt.Font.BOLD, 12f));
			g.setColor (new Color (255, 255, 255, 200));
			for (int dx = -1; dx <= 1; dx++)	for (int dy = -1; dy <= 1; dy++)	if ((dx != 0) || (dy != 0))	g.drawString (rv.name, tx + dx, ty + dy);
			g.setColor (C_ROBOT);
			g.drawString (rv.name, tx, ty);
		}
	}

	/**
	 * The body of a robot at a pose: its bitmap when it has one and, failing that,
	 * the drawing of its outline, or the circle of its radius when it has neither.
	 * What tells the robot apart from where it has been -- its heading, its centre
	 * and its name -- is not part of it.
	 */
	private void drawBody (Graphics2D g, WorldCanvas c, RobotView rv, double x, double y, double a)
	{
		double		ca = Math.cos (a), sa = Math.sin (a);
		Line2[]		icon = rv.rdesc.icon;

		g.setStroke (new BasicStroke (2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		if (drawRobotImage (g, c, rv, x, y, a))		// the bitmap already stands for the body of the robot
			;
		else if ((icon != null) && (icon.length > 0))
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
	}

	/** The route a robot has taken, as the line joining the steps of it that are kept. */
	private void drawPath (Graphics2D g, WorldCanvas c, RobotView rv)
	{
		Path2D		path;
		boolean		first = true;

		if (rv.trail.size () < 2)		return;
		path	= new Path2D.Double ();
		for (RobotView.Pose q : rv.trail)
		{
			if (first)		{ path.moveTo (c.toPixelX (q.x), c.toPixelY (q.y));		first = false; }
			else			path.lineTo (c.toPixelX (q.x), c.toPixelY (q.y));
		}
		g.setStroke (new BasicStroke (1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor (C_PATH);
		g.draw (path);
	}

	/**
	 * The robot itself along the route it has taken, at the poses that were
	 * stamped, and faintly: they are where it has been, not where it is.
	 */
	private void drawPoses (Graphics2D g, WorldCanvas c, RobotView rv)
	{
		java.awt.Composite	was = g.getComposite ();

		g.setComposite (java.awt.AlphaComposite.getInstance (java.awt.AlphaComposite.SRC_OVER, POSE_ALPHA));
		for (RobotView.Pose q : rv.trail)
			if (q.stamp)		drawBody (g, c, rv, q.x, q.y, q.a);
		g.setComposite (was);
	}

	/**
	 * The image of the robot, drawn over the box its drawing occupies and turned
	 * with it. When there is one it stands for the body of the robot, so the
	 * drawing of its outline is left out (the heading and the name are still
	 * drawn).
	 *
	 * @return whether the image was drawn
	 */
	private boolean drawRobotImage (Graphics2D g, WorldCanvas c, RobotView rv, double x, double y, double a)
	{
		java.awt.Image	img = tc.vrobot.RobotImage.get (rv.rdesc.image);
		double[]		b;
		double			bx, by, ox, oy;

		if (img == null)				return false;
		b	= tc.vrobot.RobotImage.box (rv.rdesc.icon, rv.rdesc.RADIUS);
		if (b == null)					return false;
		bx	= (b[0] + b[2]) / 2;		by = (b[1] + b[3]) / 2;			// centre of the box, in the frame of the robot
		ox	= x + bx * Math.cos (a) - by * Math.sin (a);
		oy	= y + bx * Math.sin (a) + by * Math.cos (a);
		tc.vrobot.RobotImage.draw (g, img, c.toPixelX (ox), c.toPixelY (oy),
									(b[2] - b[0]) * c.getScale (), (b[3] - b[1]) * c.getScale (), a);
		return true;
	}

	/* ------------------------------------------------------------------ */
	/* Worlds                                                              */
	/* ------------------------------------------------------------------ */

	/** Changes the world of the deployment (the one every robot of it simulates). */
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
		deploy.setWorldFile (relativePath (f));
		worldModified	= true;
		updateTitle ();
	}

	/** Loads and displays a world (null: empty world); false if the file cannot be read. */
	private boolean showWorld (File f)
	{
		World	w;
		if (f == null)
			w = WorldEditor.newWorld ();
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
		lastTasks	= null;					// the places of the remembered task set may not exist in this world
		monitorPanel.setWorld (world);
		canvas.setWorld (world);
		canvas.zoomToFit ();
		view3d.worldChanged ();
		return true;
	}

	/**
	 * Opens the world editor on the current world (a running execution is
	 * terminated first). The edited world replaces the one shown and is written
	 * back to its file (or to a new file chosen by the user when the deployment
	 * has none).
	 */
	public void editWorld ()
	{
		if (world == null)				return;
		terminate ();
		WorldEditorDialog	dlg = new WorldEditorDialog (this, world, worldFile);
		World				edited = dlg.showDialog ();
		if (edited == null)				return;

		File	f = worldFile;
		if (f == null)
		{
			JFileChooser	fc = chooser (null, MAPS_DIR, "world", "World maps (*.world)");
			fc.setDialogTitle ("Save World As");
			if (fc.showSaveDialog (this) != JFileChooser.APPROVE_OPTION)		return;
			f = fc.getSelectedFile ();
			if (!f.getName ().toLowerCase ().endsWith (World.SUFFIX))
				f = new File (f.getParentFile (), f.getName () + World.SUFFIX);
		}
		try
		{
			edited.toFile (f.getPath ());
		} catch (Exception e)
		{
			e.printStackTrace ();
			JOptionPane.showMessageDialog (this, "Cannot save world " + f.getName () + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
			return;
		}
		world		= edited;
		lastTasks	= null;					// places may have been renamed or removed
		worldFile	= f;
		monitorPanel.setWorld (world);
		canvas.setWorld (world);
		view3d.worldChanged ();
		if (deploy.getWorldFile () == null || !new File (deploy.getWorldFile ()).equals (f))
		{
			deploy.setWorldFile (relativePath (f));
			worldModified	= true;
			updateTitle ();
		}
		statusBar.setStatus ("World saved to " + f.getPath ());
	}

	/** Path relative to the working directory when possible (as a description says it), with '/' separators. */
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
		File	f = deploy.getFile ();
		setTitle (TITLE + " - " + ((f != null) ? f.getName () : "untitled." + DeployArch.EXTENSION) + (isModified () ? " *" : ""));
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

	public void worldChanged (String what)		{ view3d.worldChanged (); }		// the orientation of an element, which is all the canvas allows
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
			System.setProperty ("apple.laf.useScreenMenuBar", "false");		// menus inside the window, as in the dialogs
			UIManager.setLookAndFeel (UIManager.getSystemLookAndFeelClassName ());
		} catch (Exception e) { }

		final String	name = (args.length > 0) ? args[0] : null;
		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()
			{
				SimulatorWindow	win = new SimulatorWindow ();
				win.setVisible (true);
				if (name != null)		win.loadDeployment (new File (name));
				else					win.canvas.zoomToFit ();
			}
		});
	}
}

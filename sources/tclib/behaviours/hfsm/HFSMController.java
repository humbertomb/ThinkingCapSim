/*
 * (c) 2001 Humberto Martinez (BGController, which this one follows)
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.hfsm;

import tc.runtime.thread.ModuleConfig;

import tc.modules.*;
import tc.shared.lps.lpo.*;
import tc.shared.linda.*;
import tclib.behaviours.lua.Chaos;
import tclib.planning.sequence.*;

import devices.pos.*;
import wucore.utils.logs.*;
import wucore.utils.math.*;

/**
 * A controller driven by a machine of hierarchical states, in the place of the
 * BG program of {@link tclib.behaviours.bg.BGController}: the <code>PRG</code>
 * of the module is the <code>.xas</code> file of a machine, whose states and
 * transitions are Lua scripts, and it is those that are run on every cycle
 * instead of the behaviours of a BG program.
 *
 * The scripts reach the robot through the table they know as
 * <code>chaos</code> ({@link Chaos}): they read the objects of the LPS and
 * where the robot is, and they command the three velocities of the platform
 * (vlin, vlat, vrot) and a behaviour, which the controller then carries out.
 *
 * Settings:
 * <pre>
 *   PRG         the .xas file of the machine
 *   BEH         the folder the behaviours the states name are read from
 *               (default: what the machine says in its file, behpath, else
 *               the folder of the machine)
 *   LPOS        the objects of the LPS the scripts ask for by number,
 *               separated by commas (default Ball, Net1, Net2, Align, Looka)
 *   AUTO        run from the first cycle, with no plan
 * </pre>
 */
public class HFSMController extends Controller
{
	static public final String		PREFFIX			= "HFSM_";

	// The machine of states and the bridge its scripts speak through
	protected volatile HFSM			machine;
	protected java.io.File			file;						// the file the machine came from
	protected String				behs;						// the folder of the behaviours the settings say (BEH), or null
	protected Chaos					chaos;
	protected Tuple					ntuple;						// what the scripts need of the vision (BEH_NEEDS): the scan of the camera and the objects
	protected String				nsaid;						// what the vision was last told (scan and needs, as text); null: nothing yet

	// Controller debug
	protected tclib.behaviours.hfsm.gui.HFSMMonitorWindow	monitor;	// the diagram, with where the machine is
	protected LogPlot				c_plot;
	protected double[]				c_buffer;
	protected String[]				c_labels;

	// Goal and task related variables
	protected boolean				autostart;					// AUTO: it runs with no plan of anybody's
	protected boolean				has_goal;					// Is any goal available?
	protected boolean				has_plan;					// Has anybody said where to go? (a plan was received)
	protected boolean				new_goal;					// New goal received
	protected long					new_id;						// New task ID received
	protected Task					new_plan;					// New task received

	protected Task					plan;						// Current goal task
	protected long					idtask;						// Current task ID

	// Look-ahead related variables
	protected Path					path;						// Desired robot path
	protected Position				pos;						// Current robot location
	protected boolean				need_looka;					// Do we need a look-ahead point?
	protected Position				looka;						// Current look-ahead point
	protected int					looka_pts;					// Current look-ahead distance (points)
	protected double				path_dst;					// Current robot to desired path distance (m)

	// Constructors
	public HFSMController (ModuleConfig cfg, Linda linda)
	{
		super (cfg, linda);
	}

	// Instance methods
	protected void initialise (ModuleConfig cfg)
	{
		super.initialise (cfg);

		// Initialize local structures
		looka		= new Position ();
		pos			= new Position ();
		plan		= new Task ();

		has_goal	= false;
		has_plan	= false;
		new_plan	= new Task ();
		new_goal	= false;
		new_id		= 0;
		need_looka	= false;						// a machine of states drives on its own, with no path to follow

		idtask		= 0;
		looka_pts	= 15;

		// The bridge the scripts of the machine speak through
		chaos		= new Chaos ();

		// What the scripts need of the vision
		ntuple		= new Tuple (Tuple.BEHNEEDS, null);
		nsaid		= null;

		// Initialize debug modules
		c_buffer	= new double[3];
		c_labels	= new String[3];			// the three velocities of the control action
		c_labels[0]	= "vlin";
		c_labels[1]	= "vlat";
		c_labels[2]	= "vrot";
		c_plot		= new LogPlot ("Controller Output", "step", "m/s");

		// Load the machine of states
		parse (cfg);

		// Autostart the controller without a plan: it runs its machine from the
		// first cycle, and until somebody says where to go there is no goal to have
		// arrived at (see inGoal)
		autostart	= cfg.getBoolean ("AUTO", false);
		if (autostart)
		{
			has_goal	= true;
			has_plan	= false;
			need_looka	= false;
		}
	}

	/** Loads the machine the settings name, and the scripts of its states. */
	protected void parse (ModuleConfig cfg)
	{
		String			name = cfg.get ("PRG");
		String			lpos = cfg.get ("LPOS");

		behs	= cfg.get ("BEH");
		if (lpos != null)						chaos.lpoNames (lpos.split ("[,;\\s]+"));
		if (name == null)						return;

		if (load (new java.io.File (name)) && localgfx)
		{
			openMotionPlot (c_plot, c_labels);
			monitor	= tclib.behaviours.hfsm.gui.HFSMMonitorWindow.open (machine, cfg.robot (), new tclib.behaviours.hfsm.gui.HFSMMonitorWindow.Reload ()
			{
				public void reload ()						{ HFSMController.this.reload (); }
				public void load (java.io.File f)			{ HFSMController.this.load (f); }
			});
		}
	}

	/**
	 * Loads a machine from a file and runs it from the next cycle on, in the place
	 * of the one running: from its initial state, with an interpreter of its own,
	 * and through the same bridge. The monitor, if it is open, is told. False when
	 * the file cannot be read, and the machine running goes on.
	 */
	public boolean load (java.io.File f)
	{
		try
		{
			HFSM	m = new HFSM (f, chaos);

			m.debug (debug);
			if (behs != null)					m.behaviours (behs);

			System.out.println ("  [HFSM] Machine <" + m.root ().getName () + ">: " + m.summary ()
								+ ", behaviours from " + m.behaviours ());
			for (String p : m.problems ())
				System.out.println ("  [HFSM] " + p);

			file	= f;
			machine	= m;								// from the next cycle on
			nsaid	= null;								// another machine: the vision is told what this one needs
			if (monitor != null)				monitor.setMachine (m);
			return true;
		}
		catch (Exception e)
		{
			System.out.println ("  [HFSM] Cannot load <" + f + ">: " + e);
			return false;
		}
	}

	/** Reads the machine again from its file: it was written (the editor of the monitor). */
	public boolean reload ()
	{
		return (file != null) && load (file);
	}

	/** The machine being run, or null when none could be loaded. */
	public final HFSM				machine ()			{ return machine; }
	public final Chaos				chaos ()			{ return chaos; }

	/**
	 * Whether the robot has arrived where it was told to go, as
	 * {@link tclib.behaviours.bg.BGController} has it: nobody having said where to
	 * go is not arriving.
	 */
	protected int inGoal ()
	{
		double			dx, dy;
		double			dist, delta;

		if (!has_plan)												return ItemBehResult.T_NOTYET;

		dx		= plan.tpos.x () - pos.x ();
		dy		= plan.tpos.y () - pos.y ();
		dist	= Math.sqrt (dx * dx + dy * dy);										// [m]
		delta	= Math.abs (Angles.radnorm_180 (plan.tpos.alpha () - pos.alpha ()));	// [rad]

		if ((dist < plan.tol_pos) && (delta < plan.tol_head))
			return ItemBehResult.T_FINISHED;

		return ItemBehResult.T_NOTYET;
	}

	/**
	 * The machine starts afresh, at the initial state of every level, and there is no
	 * goal, no plan and no path any more (RESET). Whether it runs from the first
	 * cycle again is what AUTO says, as when the module was set up.
	 */
	protected void reset ()
	{
		super.reset ();

		if (machine != null)				machine.reset ();
		chaos.clear ();
		nsaid		= null;									// and the vision is told again what it needs
		has_goal	= autostart;
		has_plan	= false;
		new_goal	= false;
		path		= null;
		idtask		= 0;
		new_id		= 0;
	}

	/**
	 * Tells the vision what the scripts of the machine need of it (BEH_NEEDS): the
	 * scan of the camera asked for on this cycle (chaos.setScanType), SCAN_NONE when
	 * none was, and the objects it needs to keep seeing and how much
	 * (chaos.setNeeded), by the names the LPS knows them by. It is written when it
	 * is not what the vision was last told, and on the first cycle, so that a
	 * vision that starts scanning on its own is told to stop unless a script says
	 * otherwise. It is the same the program of a {@link tclib.behaviours.lua.LuaController} does.
	 */
	protected void needs ()
	{
		ItemBehNeeds.ScanTypes	scan = chaos.scanType ();
		String[]				names = chaos.lpoNames ();
		StringBuilder			said = new StringBuilder (scan.name ());
		java.util.List<Integer>	idx = new java.util.ArrayList<Integer> (chaos.needed ().keySet ());

		java.util.Collections.sort (idx);
		for (Integer i : idx)
			if ((i >= 0) && (i < names.length))
				said.append (' ').append (names[i]).append ('=').append (chaos.needed ().get (i));
		if (said.toString ().equals (nsaid))		return;

		// a new item every time: a shared Linda hands the reader the very object, and
		// one filled in again underneath it could be read half done
		ItemBehNeeds	nitem = new ItemBehNeeds ();

		nitem.changeScan (scan);
		for (Integer i : idx)
			if ((i >= 0) && (i < names.length))
				nitem.addNeed (names[i], chaos.needed ().get (i), System.currentTimeMillis ());
		nitem.set (System.currentTimeMillis ());
		ntuple.value	= nitem;
		linda.write (ntuple);
		nsaid	= said.toString ();
	}

	protected void controller ()
	{
		int					result;
		LPO					l_looka;
		double				vlin, vlat, vrot;

		if (!has_goal)
		{
			setMotion (0.0, 0.0, 0.0);
			return;
		}

		/* ---------- */
		/* LOOK-AHEAD */
		/* ---------- */

		// Compute look-ahead point
		pos.set (lps.cur);
		looka.set (pos);
		looka.valid (false);
		if (!new_goal && (path != null))
		{
			path.check_lookahead (pos, looka_pts);
			if (path.lookahead () != null)
			{
				path_dst = path.distance ();
				looka.set (path.lookahead ());
				looka.valid (true);
			}
		}

		// Update LPS
		l_looka = lps.find ("Looka");
		if (l_looka != null)
		{
			l_looka.locate (looka.x () - pos.x (), looka.y () - pos.y (), pos.alpha ());
			l_looka.active (looka.valid ());
		}

		/* ------- */
		/* MACHINE */
		/* ------- */

		// What the scripts of the machine are to read
		chaos.lps (lps);
		chaos.pose (pos);
		if (has_plan && (plan.tpos != null))
			chaos.desired ().set (plan.tpos);

		// One cycle of the machine: a transition if one is due, the script of the
		// state it ends in, and the behaviour that state chose -- of the machine
		// there is now, which the monitor may have changed for another
		HFSM		m = machine;

		if (m == null)							{ setMotion (0.0, 0.0, 0.0);	return; }
		m.step ();

		// What the scripts need of the vision
		needs ();

		// What the scripts commanded
		vlin	= chaos.linear ();
		vlat	= chaos.lateral ();
		vrot	= chaos.rotation ();

		// Set action
		result	= inGoal ();
		switch (result)
		{
		case ItemBehResult.T_FINISHED:
			vlin 	= 0.0;
			vlat	= 0.0;
			vrot	= 0.0;

			// Notify Linda Space the task has been finished
			setResult (result, ItemBehResult.F_OK, idtask);
			break;

		case ItemBehResult.T_FAILED:
			vlin 	= 0.0;
			vlat	= 0.0;
			vrot	= 0.0;

			// Notify Linda Space the task has failed
			setResult (result, ItemBehResult.F_BEHIND, idtask);
			break;

		case ItemBehResult.T_NOTYET:
		default:
			if (need_looka && !looka.valid ())
			{
				vlin 	= 0.0;
				vlat	= 0.0;
				vrot	= 0.0;
			}
		}
		setMotion (vlin, vlat, vrot);

		// Plot current control commands
		if (localgfx)
		{
			motionValues (c_buffer, vlin, vlat, vrot);
			c_plot.draw (c_buffer);
		}
	}

	protected void checkplan ()
	{
		if (idtask != new_id)
		{
			idtask = new_id;
			plan.set (new_plan);

			if (debug)		System.out.println ("  [HFSM] Working with task [" + plan + "] and ID: " + idtask);
		}
	}

	protected void newplan (ItemGoal goal)
	{
		new_plan.set (goal.task);
		new_id = goal.timestamp.longValue ();
	}

	protected void close_gfx ()
	{
		if (c_plot != null)		c_plot.close ();
		if (monitor != null)	{ monitor.close ();		monitor = null; }
	}

	public void step (long ctime)
	{
		if (state != RUN)												return;

		if (!auto || (machine == null) || (lps == null))					return;

		// Set last goal received as the current one
		checkplan ();

		// Run the machine of states
		controller ();

		if (debug)
			System.out.println ("  [HFSM] " + machine.where ()
								+ ((machine.lastTransition () != null) ? (" (" + machine.lastTransition () + ")") : "")
								+ ", cycle: " + (System.currentTimeMillis () - ctime) + " ms");
	}

	public void notify_config (String space, ItemConfig item)
	{
		super.notify_config (space, item);
	}

	public void notify_execution (String space, ItemExecution item)
	{
		super.notify_execution (space, item);
	}

	public void notify_goal (String space, ItemGoal goal)
	{
		LPOPoint			l_goal;

		// Update LPS
		l_goal = (LPOPoint) lps.find ("Goal");
		if (l_goal != null)
		{
			l_goal.update (pos, goal.task.tpos);
			l_goal.active (true);
		}

		newplan (goal);

		new_goal	= true;
		has_goal	= true;
		has_plan	= true;										// now there is somewhere to arrive at

		// the machine starts afresh with every new task
		if (machine != null)		machine.reset ();
	}

	public void notify_path (String space, ItemPath item)
	{
		path		= item.path;

		new_goal	= false;
	}
}

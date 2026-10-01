/*
 * (c) 2001 Humberto Martinez (BGController, which this one follows)
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.hfsm;

import tc.runtime.thread.ModuleConfig;

import tc.modules.*;
import tc.shared.lps.lpo.*;
import tc.shared.linda.*;
import tclib.behaviours.lua.LuaBridge;
import tclib.planning.sequence.*;

import tclib.utils.pos.*;
import wucore.utils.logs.*;
import wucore.utils.math.*;

/**
 * A controller driven by a machine of hierarchical states, in the place of the
 * BG program of {@link tclib.behaviours.bg.BGController}: the <code>PRG</code>
 * of the module is the <code>.hfsm</code> file of a machine, whose states and
 * transitions are Lua scripts, and it is those that are run on every cycle
 * instead of the behaviours of a BG program.
 *
 * This one knows nothing of what the scripts may say to the robot: it loads
 * the machine, runs it cycle after cycle, keeps it in step with the goals and
 * paths of the architecture and shows it in its monitor. What the scripts
 * speak through -- a bridge ({@link LuaBridge}), a table with what they may
 * read and command -- and what is made of it is for a subclass to say, through
 * {@link #bridge(ModuleConfig)}, {@link #before()} and {@link #after()}: with
 * none, the machine runs with the language alone and the robot stands still.
 *
 * Settings:
 * <pre>
 *   PRG         the .hfsm file of the machine
 *   BEH         the folder the behaviours the states name are read from
 *               (default: what the machine says in its file, behpath, else
 *               the folder of the machine)
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
	protected LuaBridge				bridge;						// what the scripts speak through, or null for the language alone

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

		// The bridge the scripts of the machine speak through, if the subclass has one
		bridge		= bridge (cfg);

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

		behs	= cfg.get ("BEH");
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
			HFSM	m = new HFSM (f, bridge);

			m.debug (debug);
			if (behs != null)					m.behaviours (behs);

			System.out.println ("  [HFSM] Machine <" + m.root ().getName () + ">: " + m.summary ()
								+ ", behaviours from " + m.behaviours ());
			for (String p : m.problems ())
				System.out.println ("  [HFSM] " + p);

			file	= f;
			machine	= m;								// from the next cycle on
			machineChanged ();
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
	/** What the scripts speak through, or null when they have the language alone. */
	public final LuaBridge			bridge ()			{ return bridge; }

	/* ------------------------------------------------------------------ */
	/* What a subclass gives the scripts                                   */
	/* ------------------------------------------------------------------ */

	/**
	 * The bridge the scripts of the machine speak to the robot through, made from
	 * the settings of the module; null, as here, for none: the scripts then have
	 * the language alone and command nothing.
	 */
	protected LuaBridge bridge (ModuleConfig cfg)	{ return null; }

	protected void before ()						{ }

	/**
	 * After every cycle of the machine: what the scripts commanded is taken out of
	 * the bridge and answered as the three velocities of the platform, linear and
	 * lateral in m/s and rotation in rad/s, in that order; whatever else they asked
	 * for (of the vision, say) is passed on from here too. Standing still here.
	 */
	protected double[] after ()						{ return new double[] { 0.0, 0.0, 0.0 }; }

	/** Another machine runs from now on (loaded, read again, or reset): whatever was said of the last one is to be said again. */
	protected void machineChanged ()				{ }

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
		delta	= Math.abs (Angles.radnorm_180 (plan.tpos.alpha - pos.alpha));	// [rad]

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
		if (bridge != null)					bridge.clear ();
		machineChanged ();
		has_goal	= autostart;
		has_plan	= false;
		new_goal	= false;
		path		= null;
		idtask		= 0;
		new_id		= 0;
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
		looka.valid = false;
		if (!new_goal && (path != null))
		{
			path.check_lookahead (pos, looka_pts);
			if (path.lookahead () != null)
			{
				path_dst = path.distance ();
				looka.set (path.lookahead ());
				looka.valid = true;
			}
		}

		// Update LPS
		l_looka = lps.find ("Looka");
		if (l_looka != null)
		{
			l_looka.locate (looka.x () - pos.x (), looka.y () - pos.y (), pos.alpha);
			l_looka.active (looka.valid);
		}

		/* ------- */
		/* MACHINE */
		/* ------- */

		// One cycle of the machine: what the scripts are to read goes in the bridge,
		// then a transition if one is due, the script of the state it ends in and the
		// behaviour that state chose -- of the machine there is now, which the monitor
		// may have changed for another -- and what the scripts commanded comes out
		HFSM		m = machine;

		if (m == null)			{ setMotion (0.0, 0.0, 0.0);	return; }
		before ();
		m.step ();

		double[]	v = after ();

		vlin	= v[0];
		vlat	= v[1];
		vrot	= v[2];

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
			if (need_looka && !looka.valid)
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

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
 *               (default ./conf/programs/lua)
 *   LPOS        the objects of the LPS the scripts ask for by number,
 *               separated by commas (default Ball, Net1, Net2, Align, Looka)
 *   AUTO        run from the first cycle, with no plan
 * </pre>
 */
public class HFSMController extends Controller
{
	static public final String		PREFFIX			= "HFSM_";

	// The machine of states and the bridge its scripts speak through
	protected HFSM					machine;
	protected Chaos					chaos;

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

		// Initialize debug modules
		c_buffer	= new double[3];
		c_labels	= new String[3];			// the three velocities of the control action
		c_labels[0]	= "vlin";
		c_labels[1]	= "vlat";
		c_labels[2]	= "vrot";
		c_plot		= new LogPlot ("Controller Output", "step", "values");

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
		String			behs = cfg.get ("BEH");
		String			lpos = cfg.get ("LPOS");

		if (name == null)						return;

		try
		{
			machine	= new HFSM (new java.io.File (name), chaos);
			machine.debug (debug);
			if (behs != null)					machine.behaviours (behs);
			if (lpos != null)					chaos.lpoNames (lpos.split ("[,;\\s]+"));

			System.out.println ("  [HFSM] Machine <" + machine.root ().getName () + ">: " + machine.summary ());
			for (String p : machine.problems ())
				System.out.println ("  [HFSM] " + p);

			if (localgfx)
			{
				c_plot.open (c_labels);
				monitor	= tclib.behaviours.hfsm.gui.HFSMMonitorWindow.open (machine, cfg.robot ());
			}
		}
		catch (Exception e)
		{
			machine	= null;
			System.out.println ("  [HFSM] Cannot load <" + name + ">: " + e);
		}
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
		// state it ends in, and the behaviour that state chose
		machine.step ();

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
			c_buffer[0] 	= Math.max (Math.min (vlin / rdesc.model.Vmax, 1.0), -1.0);
			c_buffer[1] 	= Math.max (Math.min (vlat / rdesc.model.Vmax, 1.0), -1.0);
			c_buffer[2] 	= Math.max (Math.min (vrot / rdesc.model.Rmax, 1.0), -1.0);
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

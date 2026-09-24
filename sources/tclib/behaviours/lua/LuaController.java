/*
 * (c) 2001 Humberto Martinez (BGController, which this one follows)
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.lua;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tc.runtime.thread.ModuleConfig;

import tc.modules.*;
import tc.shared.lps.lpo.*;
import tc.shared.linda.*;
import tclib.behaviours.lua.interpreter.LuaError;
import tclib.behaviours.lua.interpreter.LuaScript;
import tclib.behaviours.lua.interpreter.LuaState;
import tclib.planning.sequence.*;

import devices.pos.*;
import wucore.utils.logs.*;
import wucore.utils.math.*;

/**
 * A controller driven by one Lua script, as
 * {@link tclib.behaviours.hfsm.HFSMController} is driven by a machine of
 * hierarchical states: the <code>PRG</code> of the module is a
 * <code>.lua</code> file, and it is run once on every cycle.
 *
 * The script reaches the robot through the table it knows as
 * <code>chaos</code> ({@link Chaos}): it reads the objects of the LPS and where
 * the robot is, and it commands a speed, a turn rate and a behaviour, which the
 * controller then carries out. A behaviour the script chooses
 * (<code>chaos.setBehavior</code>) is another script of the library, read from
 * <code>BEH</code> and run right after the program, so a program can be no more
 * than the choosing of behaviours if that is all it wants to be.
 *
 * The program keeps what it leaves in the globals of the interpreter from one
 * cycle to the next, so it can remember (a timer, a state of its own) with no
 * help from here.
 *
 * Settings:
 * <pre>
 *   PRG         the .lua file of the program
 *   BEH         the folder the behaviours the program names are read from
 *               (default ./conf/programs/lua)
 *   LPOS        the objects of the LPS the script asks for by number,
 *               separated by commas (default Ball, Net1, Net2, Align, Looka)
 *   AUTO        run from the first cycle, waiting for nothing: no plan to
 *               follow, no command to start it and no LPS read yet (a passive
 *               module still runs when an event reaches it, which is what
 *               passive means)
 * </pre>
 */
public class LuaController extends Controller
{
	static public final String		PREFFIX			= "LUA_";

	/** Where the behaviours the program names are looked for, when nothing else is said. */
	static public final String		BEHAVIOURS		= "./conf/programs/lua";

	// The program and the bridge it speaks through
	protected LuaState				lua;
	protected volatile LuaScript	program;
	protected File					file;
	protected Chaos					chaos;
	protected String				behaviours = BEHAVIOURS;
	protected Map<String, LuaScript>	library = new HashMap<String, LuaScript> ();
	protected List<String>			missing = new ArrayList<String> ();
	protected long					steps;

	// Controller debug
	protected LogPlot				c_plot;
	protected LogFile				c_dump;
	protected double[]				c_buffer;
	protected String[]				c_labels;

	// Goal and task related variables
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

	protected tclib.behaviours.lua.gui.LuaMonitorWindow	monitor;	// the variables of the program while it runs
	protected boolean				autostart;					// AUTO: run from the first cycle, waiting for nothing
	protected boolean				dump;

	// Constructors
	public LuaController (ModuleConfig cfg, Linda linda)
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
		need_looka	= false;						// a program drives on its own, with no path to follow

		idtask		= 0;
		looka_pts	= 15;

		// The bridge the program speaks through, and the interpreter it runs in
		chaos		= new Chaos ();
		lua			= new LuaState ();
		lua.set ("chaos", chaos.table ());

		// Initialize debug modules
		c_buffer	= new double[2];
		c_labels	= new String[4];
		c_labels[0]	= "speed";
		c_labels[1]	= "turn";
		c_dump		= new LogFile (PREFFIX, ".log");
		c_plot		= new LogPlot ("Controller Output", "step", "values");

		dump		= false;

		// Load the program
		parse (cfg);

		// Autostart the controller: it runs its program from the first cycle and
		// waits for nothing -- no plan to follow (and so nothing to have arrived at,
		// see inGoal), no command to start it, and not even an LPS to read: a program
		// that asks for what is not there yet is given nothing and says so itself
		autostart	= cfg.getBoolean ("AUTO", false);
		if (autostart)
		{
			has_goal	= true;
			has_plan	= false;
			need_looka	= false;
			running		= true;						// nobody has to press Start
		}
	}

	/** Loads the program the settings name. */
	protected void parse (ModuleConfig cfg)
	{
		String			name = cfg.get ("PRG");
		String			behs = cfg.get ("BEH");
		String			lpos = cfg.get ("LPOS");

		c_dump.close ();

		if (name == null)						return;

		try
		{
			file	= new File (name);
			program	= lua.loadFile (file);
			if (behs != null)					behaviours (behs);
			if (lpos != null)					chaos.lpoNames (lpos.split ("[,;\\s]+"));

			System.out.println ("  [LUA] Program <" + file.getName () + ">, behaviours from " + behaviours);

			if (localgfx)
			{
				c_plot.open (c_labels);
				monitor	= tclib.behaviours.lua.gui.LuaMonitorWindow.open (lua, chaos, file, cfg.robot (),
																		  new tclib.behaviours.lua.gui.LuaMonitorWindow.Reload ()
				{
					public void reload ()					{ LuaController.this.reload (); }
					public void load (File f)				{ LuaController.this.load (f); }
				});
			}
			if (dump)							c_dump.open (c_labels);
		}
		catch (Exception e)
		{
			program	= null;
			System.out.println ("  [LUA] Cannot load <" + name + ">: " + e);
		}
	}

	/** The program being run, or null when none could be loaded. */
	public final LuaScript			program ()			{ return program; }
	public final File				file ()				{ return file; }
	public final LuaState			lua ()				{ return lua; }
	public final Chaos				chaos ()			{ return chaos; }
	/** The behaviours the program named and were nowhere to be found. */
	public final List<String>		missing ()			{ return missing; }
	public final long				steps ()			{ return steps; }

	/**
	 * Reads the program again, as it is in its file now: what somebody just wrote in
	 * the editor is what the robot runs from the next cycle on. It is read whole
	 * before it takes the place of the one in use, so a program that does not read
	 * leaves the robot running the one that does, and said out loud either way.
	 */
	public boolean reload ()
	{
		return load (file);
	}

	/**
	 * Runs another program from the next cycle on: it is read whole before it takes
	 * the place of the one in use, and the interpreter is left as it is, so what the
	 * one before left in the globals the new one finds -- and it starts as a
	 * behaviour that has just begun (isNew).
	 */
	public boolean load (File f)
	{
		if (f == null)							return false;

		try
		{
			LuaScript	fresh = lua.loadFile (f);
			boolean		other = (file == null) || !file.getAbsolutePath ().equals (f.getAbsolutePath ());

			file	= f;
			program	= fresh;
			library.clear ();									// the behaviours may have been changed too
			missing.clear ();
			if (other)							chaos.behaviour (null);		// another program is another behaviour
			System.out.println ("  [LUA] Program <" + f.getName () + (other ? "> running" : "> read again"));
			return true;
		}
		catch (Exception e)
		{
			System.out.println ("  [LUA] Cannot read <" + f.getName () + ">: " + e.getMessage () + " (the one running is kept)");
			return false;
		}
	}

	/** Where the behaviours the program names are looked for. */
	public void behaviours (String path)				{ behaviours = path;	library.clear (); }
	public String behaviours ()							{ return behaviours; }

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
	 * The program starts afresh: it is read again, and the interpreter it runs in is
	 * a new one, so nothing of what it left in a global is there any more -- which is
	 * what a program that remembers on its own is to be reset of. There is no goal,
	 * no plan and no path either; whether it runs from the first cycle again is what
	 * AUTO says, as when the module was set up (RESET).
	 */
	protected void reset ()
	{
		super.reset ();

		lua			= new LuaState ();
		lua.set ("chaos", chaos.table ());
		library.clear ();
		missing.clear ();
		steps		= 0;
		program		= null;
		if (file != null)					load (file);

		chaos.clear ();
		chaos.behaviour (null);								// the program begins again, as a behaviour just chosen
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
		double				speed, turn;

		if (!has_goal)
		{
			setMotion (0.0, 0.0);
			return;
		}

		/* ---------- */
		/* LOOK-AHEAD */
		/* ---------- */

		// Compute look-ahead point (there may be no LPS at all yet, when running on its own)
		if (lps != null)			pos.set (lps.cur);
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
		l_looka = (lps != null) ? lps.find ("Looka") : null;
		if (l_looka != null)
		{
			l_looka.locate (looka.x () - pos.x (), looka.y () - pos.y (), pos.alpha ());
			l_looka.active (looka.valid ());
		}

		/* ------- */
		/* PROGRAM */
		/* ------- */

		// What the program is to read
		chaos.lps (lps);
		chaos.pose (pos);
		if (has_plan && (plan.tpos != null))
			chaos.desired ().set (plan.tpos);

		// One cycle of the program, and the behaviour it chose, which is what moves the robot
		step ();

		// What the program commanded
		speed	= chaos.speed ();
		turn	= chaos.turn ();

		// Set action
		result	= inGoal ();
		switch (result)
		{
		case ItemBehResult.T_FINISHED:
			speed 	= 0.0;
			turn	= 0.0;

			// Notify Linda Space the task has been finished
			setResult (result, ItemBehResult.F_OK, idtask);
			break;

		case ItemBehResult.T_FAILED:
			speed 	= 0.0;
			turn	= 0.0;

			// Notify Linda Space the task has failed
			setResult (result, ItemBehResult.F_BEHIND, idtask);
			break;

		case ItemBehResult.T_NOTYET:
		default:
			if (need_looka && !looka.valid ())
			{
				speed 	= 0.0;
				turn	= 0.0;
			}
		}
		setMotion (speed, turn);

		// Plot current control commands
		if (localgfx || dump)
		{
			c_buffer[0] 	= Math.max (Math.min (speed / rdesc.model.Vmax, 1.0), -1.0);
			c_buffer[1] 	= Math.max (Math.min (turn / rdesc.model.Rmax, 1.0), -1.0);

			if (localgfx)
				c_plot.draw (c_buffer);
			if (dump)
				c_dump.write (c_buffer);
		}
	}

	/* ------------------------------------------------------------------ */
	/* Running the program                                                 */
	/* ------------------------------------------------------------------ */

	/**
	 * One cycle of the program: what it commanded is left in the bridge, and the
	 * behaviour it chose is run right after it.
	 */
	protected void step ()
	{
		String			mine = name ();

		chaos.clear ();
		steps++;

		// the program is itself the behaviour being run, so it is named as such
		// before it runs: a program that asks (getBehaviorInfo) is then told that it
		// has just started on the first cycle and how long it has been running on
		// the rest, which is what a behaviour of the library is told
		if (chaos.behaviour () == null)			chaos.behaviour (mine);

		run (program);

		// and the behaviour it chose for itself, if it chose one other than itself
		String			chosen = chaos.behaviour ();

		if ((chosen != null) && !chosen.equals (mine))		behaviour (chosen);
	}

	/** What the program is called: its file, without the suffix. */
	public String name ()
	{
		String			n = (file != null) ? file.getName () : "program";

		return n.toLowerCase ().endsWith (".lua") ? n.substring (0, n.length () - 4) : n;
	}

	/** Runs the behaviour of the library the program asked for, if it is there. */
	protected void behaviour (String name)
	{
		if ((name == null) || (name.length () == 0))		return;

		LuaScript		script = library.get (name);

		if (script == null)
		{
			if (missing.contains (name))		return;

			File		f = new File (behaviours, name + ".lua");

			if (!f.exists ())
			{
				missing.add (name);
				System.out.println ("  [LUA] Behaviour <" + name + "> not found in " + behaviours);
				return;
			}
			try { script = lua.loadFile (f); }
			catch (Exception e)
			{
				missing.add (name);
				System.out.println ("  [LUA] Behaviour <" + name + "> cannot be read: " + e.getMessage ());
				return;
			}
			library.put (name, script);
			if (debug)		System.out.println ("  [LUA] Behaviour <" + name + "> read from " + f);
		}
		run (script);
	}

	/**
	 * Runs one script. A script that fails does not stop the controller: it is said
	 * out loud and the cycle carries on, as a robot that stops is worse than a robot
	 * that misses a cycle.
	 */
	protected Object run (LuaScript script)
	{
		if (script == null)						return null;
		try { return lua.run (script); }
		catch (LuaError e)
		{
			System.out.println ("  [LUA] " + e.getMessage ());
			return null;
		}
		catch (RuntimeException e)
		{
			System.out.println ("  [LUA] " + script.name () + ": " + e);
			return null;
		}
	}

	/* ------------------------------------------------------------------ */
	/* The module                                                          */
	/* ------------------------------------------------------------------ */

	protected void checkplan ()
	{
		if (idtask != new_id)
		{
			idtask = new_id;
			plan.set (new_plan);

			if (debug)		System.out.println ("  [LUA] Working with task [" + plan + "] and ID: " + idtask);
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

		if (!auto || (program == null))									return;
		if ((lps == null) && !autostart)								return;		// on its own it waits for no perception

		// Set last goal received as the current one
		checkplan ();

		// Run the program
		controller ();

		if (debug)
			System.out.println ("  [LUA] " + ((chaos.behaviour () != null) ? chaos.behaviour () : "-")
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

		chaos.behaviour (null);									// the program starts afresh with every new task
	}

	public void notify_path (String space, ItemPath item)
	{
		path		= item.path;

		new_goal	= false;
	}
}

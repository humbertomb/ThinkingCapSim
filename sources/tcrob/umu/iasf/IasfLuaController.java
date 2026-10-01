/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcrob.umu.iasf;

import java.io.File;

import tc.runtime.thread.ModuleConfig;

import tc.modules.*;
import tc.shared.linda.*;
import tclib.behaviours.lua.Tc;
import tclib.behaviours.lua.interpreter.LuaError;
import tclib.behaviours.lua.interpreter.LuaScript;
import tclib.behaviours.lua.interpreter.LuaState;

import wucore.utils.logs.*;

/**
 * The same controller as {@link IasfJavaController}, written in Lua: the
 * <code>PRG</code> of the module is a <code>.lua</code> file, run once on every
 * cycle, which reads the LPS and says what the robot is to do through the table
 * <code>tc</code> ({@link Tc}): <code>tc.setVlin</code>, <code>tc.setVlat</code>
 * [m/s], <code>tc.setVrot</code> [deg/s] or <code>tc.setVelocities</code>.
 *
 * A velocity the program does not set on a cycle is 0. What the program wants to
 * remember from one cycle to the next it leaves in a global. A program that fails
 * is said out loud and the robot stands still on that cycle.
 *
 * Settings:
 * <pre>
 *   PRG         the .lua file of the program
 *   AUTO        run from the first cycle
 * </pre>
 */
public class IasfLuaController extends Controller
{
	static public final String		PREFFIX			= "CNTL_";

	// The program and what it runs in
	protected File					file;
	protected volatile LuaScript	program;
	protected LuaState				lua;
	protected Tc					tc;
	protected tclib.behaviours.lua.gui.LuaMonitorWindow	monitor;

	// Controller debug
	protected LogPlot				c_plot;
	protected double[]				c_buffer;
	protected String[]				c_labels;

	// Constructors
	public IasfLuaController (ModuleConfig cfg, Linda linda)
	{
		super (cfg, linda);
	}

	// Instance methods
	protected void initialise (ModuleConfig cfg)
	{
		super.initialise (cfg);

		// The library the program speaks through, and the interpreter it runs in
		tc			= new Tc ();
		lua			= new LuaState ();
		lua.set (tc.name (), tc.table ());

		// Initialize debug modules
		c_buffer	= new double[3];
		c_labels	= new String[3];			// the three velocities of the control action
		c_labels[0]	= "vlin";
		c_labels[1]	= "vlat";
		c_labels[2]	= "vrot";
		c_plot		= new LogPlot ("Controller Output", "step", "m/s");

		// Load the program
		parse (cfg);
	}

	/** Loads the program the settings name, and opens the windows of the debugging. */
	protected void parse (ModuleConfig cfg)
	{
		String			name = cfg.get ("PRG");
		String			wrong = null;

		if (name != null)
		{
			file	= new File (name);
			try
			{
				program	= lua.loadFile (file);
				System.out.println ("  [LUA] Program <" + file.getName () + ">");
			}
			catch (Exception e)
			{
				program	= null;
				wrong	= (e instanceof LuaError) ? e.getMessage () : e.toString ();
				System.out.println ("  [LUA] Cannot read <" + name + ">: " + wrong + " (nothing to run)");
			}
		}
		else
			System.out.println ("  [LUA] No program (PRG) for the controller: nothing to run");

		if (localgfx)
		{
			openMotionPlot (c_plot, c_labels);	// the window, with its scales as far as the platform goes
			if (file != null)
			{
				monitor	= tclib.behaviours.lua.gui.LuaMonitorWindow.open (lua, tc, file, cfg.robot (),
																		  new tclib.behaviours.lua.gui.LuaMonitorWindow.Reload ()
				{
					public void reload ()					{ IasfLuaController.this.load (file); }
					public void load (File f)				{ IasfLuaController.this.load (f); }
				});
				if ((monitor != null) && (wrong != null))	monitor.problem (wrong);
			}
		}
	}

	/**
	 * Runs another program (or the same one, as it is in its file now) from the
	 * next cycle on. It is read whole before it takes the place of the one in use,
	 * so a program that does not read leaves the robot running the one that does.
	 */
	public boolean load (File f)
	{
		if (f == null)							return false;

		try
		{
			LuaScript	fresh = lua.loadFile (f);

			file	= f;
			program	= fresh;
			System.out.println ("  [LUA] Program <" + f.getName () + "> read");
			if (monitor != null)				monitor.problem (null);
			return true;
		}
		catch (Exception e)
		{
			String		wrong = (e instanceof LuaError) ? e.getMessage () : e.toString ();

			System.out.println ("  [LUA] Cannot read <" + f.getName () + ">: " + wrong);
			if (monitor != null)				monitor.problem (wrong);
			return false;
		}
	}

	/**
	 * The program starts afresh: the interpreter forgets the globals the program
	 * left, and the program is read again from its file. Whether it runs from the
	 * first cycle again is what AUTO says, as when the module was set up.
	 */
	protected void reset ()
	{
		super.reset ();

		lua.clear ();
		lua.set (tc.name (), tc.table ());
		tc.clear ();
		program		= null;
		if (file != null)						load (file);
	}

	protected void controller ()
	{
		double				vlin, vlat, vrot;

		/* ----------- */
		/* CONTROLLER  */
		/* ----------- */

		// What the program is to read
		tc.lps (lps);
		tc.clear ();

		// One cycle of the program; one that fails stops the robot
		if (!run (program))
			tc.clear ();

		// What the program commanded (it says deg/s, the platform takes rad/s)
		vlin	= tc.linear ();
		vlat	= tc.lateral ();
		vrot	= tc.rotation ();

		// Set action
		setMotion (vlin, vlat, vrot);

		// Plot current control commands
		if (localgfx)
		{
			motionValues (c_buffer, vlin, vlat, vrot);
			c_plot.draw (c_buffer);
		}
	}

	/** Runs the program once; whether it ran to the end. */
	protected boolean run (LuaScript script)
	{
		if (script == null)						return false;
		try
		{
			lua.run (script);
			return true;
		}
		catch (LuaError e)
		{
			System.out.println ("  [LUA] " + e.getMessage ());
		}
		catch (RuntimeException e)
		{
			System.out.println ("  [LUA] " + script.name () + ": " + e);
		}
		return false;
	}

	protected void close_gfx ()
	{
		if (c_plot != null)		c_plot.close ();
		if (monitor != null)	{ monitor.close ();		monitor = null; }
	}

	public void step (long ctime)
	{
		if (state != RUN)						return;

		if (!auto || (lps == null))				return;

		// Run controller
		controller ();
	}
}

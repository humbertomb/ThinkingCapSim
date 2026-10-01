/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.lua;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import tc.shared.lps.LPS;
import tc.shared.lps.lpo.LPO;
import tc.shared.lps.lpo.LPOSensorGroup;
import tclib.behaviours.lua.interpreter.LuaFunction;
import tclib.behaviours.lua.interpreter.LuaTable;
import wucore.utils.math.Angles;

/**
 * The elements of a controller of ThinkingCap as a Lua script sees them: the
 * table it knows as <code>tc</code>, in the way {@link Chaos} is the one of the
 * soccer robots. It works in the units of ThinkingCap: distances in metres.
 *
 * <pre>
 *   tc.getGroups ()     the groups of sensors of the LPS (the object Group), as a
 *                       table: group0, group1 ... groupN, in metres; an empty
 *                       table when the LPS has no groups yet
 *   tc.setVlin (v)      how fast to go forward [m/s]
 *   tc.setVlat (v)      how fast to go sideways, to the left [m/s]
 *   tc.setVrot (v)      how fast to turn, to the left [deg/s]
 *   tc.setVelocities (vlin, vlat, vrot)   the three at once
 * </pre>
 *
 * The controller fills it in before every cycle ({@link #lps}), forgets what was
 * commanded ({@link #clear}: a velocity not said on a cycle is 0) and reads the
 * velocities out afterwards ({@link #linear}, {@link #lateral}, {@link #rotation}).
 */
public class Tc implements LuaBridge
{
	/** The object of the LPS the groups of sensors are in. */
	static public final String		GROUP		= "Group";
	/** What every group is called in the table getGroups answers: group0, group1 and so on. */
	static public final String		GROUP_		= "group";

	protected LPS					lps;
	protected LuaTable				table;
	protected java.util.Set<String>	warned		= new java.util.HashSet<String> ();

	// What the script commanded
	protected double				vlin;								// m/s
	protected double				vlat;								// m/s
	protected double				vrot;								// deg/s

	public Tc ()
	{
		table	= build ();
	}

	/* ------------------------------------------------------------------ */
	/* What the controller puts in and takes out                           */
	/* ------------------------------------------------------------------ */

	public final String				name ()						{ return "tc"; }
	public final LuaTable			table ()					{ return table; }

	public void lps (LPS lps)									{ this.lps = lps; }
	public LPS lps ()											{ return lps; }

	/** Forgets what the script commanded, before a new cycle: the robot stands still unless it says otherwise. */
	public void clear ()
	{
		vlin	= 0.0;
		vlat	= 0.0;
		vrot	= 0.0;
	}

	/** How fast the script asked to go forward [m/s]. */
	public double linear ()										{ return vlin; }
	/** How fast the script asked to go sideways, to the left of the robot [m/s]. */
	public double lateral ()									{ return vlat; }
	/** How fast the script asked to turn [rad/s], which is what the controller commands. */
	public double rotation ()									{ return vrot * Angles.DTOR; }

	/* The same three as the script said them: m/s, m/s and deg/s. */
	public double vlin ()										{ return vlin; }
	public double vlat ()										{ return vlat; }
	public double vrot ()										{ return vrot; }

	public String behaviour ()									{ return null; }
	public void entered ()										{ }
	public Map<String, Object> globals ()						{ return new TreeMap<String, Object> (); }

	/** What the script asked of the robot on this cycle, as the monitors show it. */
	public Map<String, Object> commands ()
	{
		Map<String, Object>		c = new LinkedHashMap<String, Object> ();

		c.put ("vlin", Double.valueOf (vlin));
		c.put ("vlat", Double.valueOf (vlat));
		c.put ("vrot", Double.valueOf (vrot));
		return c;
	}

	/**
	 * A velocity a script gave, or the one there was when it is not a number: a
	 * script that divided by zero asks for a speed that is not one, and a robot
	 * commanded with one loses its pose for good. It is said once.
	 */
	protected double sane (String what, double value, double old)
	{
		if (Double.isFinite (value))			return value;

		if (warned.add (what))
			System.out.println ("  [TC] " + what + " was given " + value + " and ignored");
		return old;
	}

	/* ------------------------------------------------------------------ */
	/* What a script can call                                              */
	/* ------------------------------------------------------------------ */

	/** The groups of sensors of the LPS, as getGroups answers them. */
	public LuaTable groups ()
	{
		LuaTable		t = new LuaTable ();
		LPO				o = (lps != null) ? lps.find (GROUP) : null;

		if (o instanceof LPOSensorGroup)
		{
			double[]	range = ((LPOSensorGroup) o).range;

			if (range != null)
				for (int i = 0; i < range.length; i++)
					t.set (GROUP_ + i, Double.valueOf (range[i]));
		}
		return t;
	}

	protected LuaTable build ()
	{
		LuaTable		c = new LuaTable ();

		c.set ("getGroups", new LuaFunction ("tc.getGroups")
		{
			public Object call (Object[] args)
			{
				return groups ();
			}
		});

		/* ---- what the robot is to do ---- */

		c.set ("setVlin", new LuaFunction ("tc.setVlin")
		{
			public Object call (Object[] args)		{ vlin = sane (name (), num (args, 0, 0.0), vlin);	return null; }
		});

		c.set ("setVlat", new LuaFunction ("tc.setVlat")
		{
			public Object call (Object[] args)		{ vlat = sane (name (), num (args, 0, 0.0), vlat);	return null; }
		});

		c.set ("setVrot", new LuaFunction ("tc.setVrot")
		{
			public Object call (Object[] args)		{ vrot = sane (name (), num (args, 0, 0.0), vrot);	return null; }
		});

		// the three at once: along, across and around (vlin, vlat, vrot)
		c.set ("setVelocities", new LuaFunction ("tc.setVelocities")
		{
			public Object call (Object[] args)
			{
				vlin	= sane (name (), num (args, 0, 0.0), vlin);
				vlat	= sane (name (), num (args, 1, 0.0), vlat);
				vrot	= sane (name (), num (args, 2, 0.0), vrot);
				return null;
			}
		});

		return c;
	}
}

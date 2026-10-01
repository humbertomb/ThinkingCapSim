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

/**
 * The elements of a controller of ThinkingCap as a Lua script sees them: the
 * table it knows as <code>tc</code>, in the way {@link Chaos} is the one of the
 * soccer robots. It works in the units of ThinkingCap: distances in metres.
 *
 * <pre>
 *   tc.getGroups ()     the groups of sensors of the LPS (the object Group), as a
 *                       table: group0, group1 ... groupN, in metres; an empty
 *                       table when the LPS has no groups yet
 * </pre>
 *
 * The controller fills it in before every cycle ({@link #lps}). What the script
 * commands is not asked of it: a controller reads it from the globals the script
 * leaves (see {@link tcrob.umu.iasf.IasfLuaController}), and tells it back here
 * ({@link #commanded}) for the monitors to show.
 */
public class Tc implements LuaBridge
{
	/** The object of the LPS the groups of sensors are in. */
	static public final String		GROUP		= "Group";
	/** What every group is called in the table getGroups answers: group0, group1 and so on. */
	static public final String		GROUP_		= "group";

	protected LPS					lps;
	protected LuaTable				table;
	protected Map<String, Object>	commands	= new LinkedHashMap<String, Object> ();

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

	/** What the script commanded on this cycle, as the controller read it, for the monitors. */
	public void commanded (String name, Object value)			{ commands.put (name, value); }

	public void clear ()										{ commands.clear (); }
	public String behaviour ()									{ return null; }
	public void entered ()										{ }
	public Map<String, Object> globals ()						{ return new TreeMap<String, Object> (); }
	public Map<String, Object> commands ()						{ return new LinkedHashMap<String, Object> (commands); }

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

		return c;
	}
}

/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.hfsm.lua;

/**
 * Something a Lua script can call: either a function written in Lua (the
 * interpreter makes those) or one written in Java, which is how the robot puts
 * what it can do within reach of a script (see
 * {@link tclib.behaviours.hfsm.Chaos}).
 *
 * A call takes the arguments it was given, missing ones being nil, and answers
 * one value, or an array of them when it answers several.
 */
public abstract class LuaFunction
{
	protected String				name;

	public LuaFunction ()					{ this ("?"); }
	public LuaFunction (String name)		{ this.name = name; }

	public final String		name ()			{ return name; }
	public final void		name (String n)	{ name = n; }

	public abstract Object call (Object[] args);

	/** The i-th argument, or nil when it was not given. */
	static public Object arg (Object[] args, int i)
	{
		return ((args != null) && (i < args.length)) ? args[i] : null;
	}

	/** The i-th argument as a number, or <code>def</code> when it was not given. */
	static public double num (Object[] args, int i, double def)
	{
		Double		d = Lua.tonumber (arg (args, i));

		return (d != null) ? d.doubleValue () : def;
	}

	static public String str (Object[] args, int i)
	{
		Object		v = arg (args, i);

		return (v == null) ? null : Lua.tostring (v);
	}

	public String toString ()				{ return "function: " + name; }
}

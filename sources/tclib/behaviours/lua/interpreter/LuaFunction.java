/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.lua.interpreter;

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

	/*
	 * A function that was asked for something it cannot give (an object of the LPS
	 * of a number there is not) answers nil and says why here, so that whoever looks
	 * at the program while it runs sees which variable it was that got the nil: the
	 * interpreter takes the complaint when the call comes back and hangs it on the
	 * local the value was declared into.
	 */
	private String					complaint;

	/** Says what was the matter with the call being answered. */
	public void complain (String what)		{ complaint = what; }

	/** What the last call complained of, or null when nothing was; it is forgotten once taken. */
	public String complained ()
	{
		String		c = complaint;

		complaint	= null;
		return c;
	}

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

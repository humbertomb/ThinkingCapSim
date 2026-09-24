/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.lua.interpreter;

import java.util.List;

/**
 * The part of the Lua standard library a behaviour needs: <code>math</code>,
 * <code>io.write</code> (and <code>print</code>), a little of
 * <code>string</code> and <code>table</code>, and the basic functions. Nothing
 * that reaches outside the robot -- no files, no processes -- is there.
 *
 * Where the output of <code>io.write</code> goes can be changed, so that a
 * window of the simulator can show what a machine says.
 */
public class LuaLib
{
	/** Where io.write and print leave their text. */
	public interface Output
	{
		public void write (String text);
	}

	static private Output			out = new Output ()
	{
		public void write (String text)			{ System.out.print (text); }
	};

	/** Sends what the scripts write somewhere else (null puts it back on the console). */
	static public void output (Output o)
	{
		out	= (o != null) ? o : new Output () { public void write (String text) { System.out.print (text); } };
	}

	static public Output output ()				{ return out; }

	/** Puts the library in a table of globals. */
	static public void open (final LuaTable g)
	{
		/* ---- basic ---- */

		g.set ("_VERSION", "Lua 5.1 (subset)");

		g.set ("print", new LuaFunction ("print")
		{
			public Object call (Object[] args)
			{
				StringBuffer	sb = new StringBuffer ();

				for (int i = 0; (args != null) && (i < args.length); i++)
					sb.append ((i > 0) ? "\t" : "").append (Lua.tostring (args[i]));
				out.write (sb.append ("\n").toString ());
				return null;
			}
		});

		g.set ("tostring", new LuaFunction ("tostring")
		{
			public Object call (Object[] args)	{ return Lua.tostring (arg (args, 0)); }
		});

		g.set ("tonumber", new LuaFunction ("tonumber")
		{
			public Object call (Object[] args)	{ return Lua.tonumber (arg (args, 0)); }
		});

		g.set ("type", new LuaFunction ("type")
		{
			public Object call (Object[] args)	{ return Lua.type (arg (args, 0)); }
		});

		g.set ("assert", new LuaFunction ("assert")
		{
			public Object call (Object[] args)
			{
				if (!Lua.truth (arg (args, 0)))
					throw new LuaError ((str (args, 1) != null) ? str (args, 1) : "assertion failed!");
				return arg (args, 0);
			}
		});

		g.set ("error", new LuaFunction ("error")
		{
			public Object call (Object[] args)	{ throw new LuaError (Lua.tostring (arg (args, 0))); }
		});

		g.set ("ipairs", new LuaFunction ("ipairs")
		{
			public Object call (Object[] args)
			{
				final LuaTable	t = table (arg (args, 0), "ipairs");

				return new Object[] { new LuaFunction ("ipairs_it")
				{
					public Object call (Object[] a)
					{
						int		i = (int) num (a, 1, 0.0) + 1;
						Object	v = t.get (i);

						return (v == null) ? null : new Object[] { Double.valueOf (i), v };
					}
				}, t, Double.valueOf (0.0) };
			}
		});

		g.set ("pairs", new LuaFunction ("pairs")
		{
			public Object call (Object[] args)
			{
				final LuaTable		t = table (arg (args, 0), "pairs");
				final List<Object>	keys = t.keys ();

				return new Object[] { new LuaFunction ("pairs_it")
				{
					public Object call (Object[] a)
					{
						Object		prev = arg (a, 1);
						int			i = (prev == null) ? 0 : (keys.indexOf (LuaTable.key (prev)) + 1);

						while ((i >= 0) && (i < keys.size ()))
						{
							Object	k = keys.get (i);
							Object	v = t.get (k);

							if (v != null)		return new Object[] { k, v };
							i++;
						}
						return null;
					}
				}, t, null };
			}
		});

		g.set ("unpack", new LuaFunction ("unpack")
		{
			public Object call (Object[] args)
			{
				LuaTable	t = table (arg (args, 0), "unpack");
				int			n = t.length ();
				Object[]	r = new Object[n];

				for (int i = 0; i < n; i++)		r[i] = t.get (i + 1);
				return r;
			}
		});

		/* ---- math ---- */

		LuaTable		math = new LuaTable ();

		math.set ("pi", Double.valueOf (Math.PI));
		math.set ("huge", Double.valueOf (Double.POSITIVE_INFINITY));
		one (math, "abs");		one (math, "sqrt");		one (math, "sin");		one (math, "cos");
		one (math, "tan");		one (math, "asin");		one (math, "acos");		one (math, "exp");
		one (math, "floor");	one (math, "ceil");		one (math, "deg");		one (math, "rad");
		// which way a number goes: a script that turns towards something asks for it
		// every time it writes vrot = k * sign (theta)
		math.set ("sign", new LuaFunction ("math.sign")
		{
			public Object call (Object[] args)
			{
				double		v = num (args, 0, 0.0);

				if (Double.isNaN (v))				return Double.valueOf (v);		// no sign to be had
				return Double.valueOf ((v < 0.0) ? -1.0 : 1.0);
			}
		});
		// an angle brought into half a turn either way, whichever unit it is written in:
		// what to do with the difference of two angles before comparing it with anything
		math.set ("normrad", new LuaFunction ("math.normrad")
		{
			public Object call (Object[] args)
			{
				return Double.valueOf (normalised (num (args, 0, 0.0)));
			}
		});
		math.set ("normdeg", new LuaFunction ("math.normdeg")
		{
			public Object call (Object[] args)
			{
				return Double.valueOf (normalisedDegrees (num (args, 0, 0.0)));
			}
		});
		math.set ("atan", new LuaFunction ("math.atan")
		{
			public Object call (Object[] args)
			{
				return Double.valueOf ((args != null) && (args.length > 1) && (args[1] != null)
									   ? Math.atan2 (num (args, 0, 0.0), num (args, 1, 1.0))
									   : Math.atan (num (args, 0, 0.0)));
			}
		});
		math.set ("atan2", new LuaFunction ("math.atan2")
		{
			public Object call (Object[] args)	{ return Double.valueOf (Math.atan2 (num (args, 0, 0.0), num (args, 1, 1.0))); }
		});
		math.set ("log", new LuaFunction ("math.log")
		{
			public Object call (Object[] args)
			{
				double		v = Math.log (num (args, 0, 0.0));

				return Double.valueOf (((args != null) && (args.length > 1) && (args[1] != null)) ? (v / Math.log (num (args, 1, Math.E))) : v);
			}
		});
		math.set ("pow", new LuaFunction ("math.pow")
		{
			public Object call (Object[] args)	{ return Double.valueOf (Math.pow (num (args, 0, 0.0), num (args, 1, 1.0))); }
		});
		math.set ("fmod", new LuaFunction ("math.fmod")
		{
			public Object call (Object[] args)	{ return Double.valueOf (num (args, 0, 0.0) % num (args, 1, 1.0)); }
		});
		math.set ("modf", new LuaFunction ("math.modf")
		{
			public Object call (Object[] args)
			{
				double		v = num (args, 0, 0.0);
				double		i = (v >= 0) ? Math.floor (v) : Math.ceil (v);

				return new Object[] { Double.valueOf (i), Double.valueOf (v - i) };
			}
		});
		math.set ("max", new LuaFunction ("math.max")
		{
			public Object call (Object[] args)
			{
				double		v = num (args, 0, 0.0);

				for (int i = 1; (args != null) && (i < args.length); i++)		v = Math.max (v, num (args, i, v));
				return Double.valueOf (v);
			}
		});
		math.set ("min", new LuaFunction ("math.min")
		{
			public Object call (Object[] args)
			{
				double		v = num (args, 0, 0.0);

				for (int i = 1; (args != null) && (i < args.length); i++)		v = Math.min (v, num (args, i, v));
				return Double.valueOf (v);
			}
		});
		// a value kept within bounds: what a script does to a speed or a turn rate
		// before commanding it, which is min and max one inside the other
		math.set ("limit", new LuaFunction ("math.limit")
		{
			public Object call (Object[] args)
			{
				double		v = num (args, 0, 0.0);
				double		lo = num (args, 1, v), hi = num (args, 2, v);

				if (lo > hi)						{ double t = lo;	lo = hi;	hi = t; }
				if (Double.isNaN (v))				return Double.valueOf (v);		// nothing to keep within
				return Double.valueOf (Math.max (lo, Math.min (hi, v)));
			}
		});
		math.set ("random", new LuaFunction ("math.random")
		{
			public Object call (Object[] args)
			{
				double		r = Math.random ();

				if ((args == null) || (args.length == 0))		return Double.valueOf (r);
				if (args.length == 1)							return Double.valueOf (Math.floor (r * num (args, 0, 1.0)) + 1);

				double		lo = num (args, 0, 1.0), hi = num (args, 1, 1.0);

				return Double.valueOf (Math.floor (lo + r * (hi - lo + 1)));
			}
		});
		math.set ("randomseed", new LuaFunction ("math.randomseed")
		{
			public Object call (Object[] args)	{ return null; }
		});
		g.set ("math", math);

		/* ---- io ---- */

		LuaTable		io = new LuaTable ();

		io.set ("write", new LuaFunction ("io.write")
		{
			public Object call (Object[] args)
			{
				StringBuffer	sb = new StringBuffer ();

				for (int i = 0; (args != null) && (i < args.length); i++)		sb.append (Lua.tostring (args[i]));
				out.write (sb.toString ());
				return null;
			}
		});
		io.set ("read", new LuaFunction ("io.read")
		{
			public Object call (Object[] args)	{ return null; }				// a behaviour has nobody to read from
		});
		g.set ("io", io);

		/* ---- string ---- */

		LuaTable		string = new LuaTable ();

		string.set ("len", new LuaFunction ("string.len")
		{
			public Object call (Object[] args)	{ return Double.valueOf (Lua.tostring (arg (args, 0)).length ()); }
		});
		string.set ("sub", new LuaFunction ("string.sub")
		{
			public Object call (Object[] args)
			{
				String		s = Lua.tostring (arg (args, 0));
				int			n = s.length ();
				int			i = (int) num (args, 1, 1.0);
				int			j = (int) num (args, 2, -1.0);

				if (i < 0)		i = Math.max (n + i + 1, 1);
				else if (i == 0)	i = 1;
				if (j < 0)		j = n + j + 1;
				else			j = Math.min (j, n);
				return (i > j) ? "" : s.substring (i - 1, j);
			}
		});
		string.set ("upper", new LuaFunction ("string.upper")
		{
			public Object call (Object[] args)	{ return Lua.tostring (arg (args, 0)).toUpperCase (); }
		});
		string.set ("lower", new LuaFunction ("string.lower")
		{
			public Object call (Object[] args)	{ return Lua.tostring (arg (args, 0)).toLowerCase (); }
		});
		string.set ("rep", new LuaFunction ("string.rep")
		{
			public Object call (Object[] args)	{ return Lua.tostring (arg (args, 0)).repeat (Math.max (0, (int) num (args, 1, 0.0))); }
		});
		string.set ("format", new LuaFunction ("string.format")
		{
			public Object call (Object[] args)
			{
				String		fmt = Lua.tostring (arg (args, 0));
				Object[]	rest = new Object[Math.max (0, ((args != null) ? args.length : 0) - 1)];

				for (int i = 0; i < rest.length; i++)
				{
					Object	v = args[i + 1];

					rest[i]	= (v instanceof Number) ? (Object) Double.valueOf (((Number) v).doubleValue ()) : (Object) Lua.tostring (v);
				}
				try { return String.format (java.util.Locale.US, fmt.replace ("%d", "%.0f"), rest); }
				catch (Exception e) { throw new LuaError ("bad format '" + fmt + "': " + e.getMessage ()); }
			}
		});
		g.set ("string", string);

		/* ---- table ---- */

		LuaTable		table = new LuaTable ();

		table.set ("insert", new LuaFunction ("table.insert")
		{
			public Object call (Object[] args)
			{
				LuaTable	t = table (arg (args, 0), "table.insert");

				if ((args.length > 2) && (args[2] != null))
				{
					int		at = (int) num (args, 1, 1.0);

					for (int i = t.length (); i >= at; i--)		t.set (i + 1, t.get (i));
					t.set (at, args[2]);
				}
				else
					t.set (t.length () + 1, arg (args, 1));
				return null;
			}
		});
		table.set ("remove", new LuaFunction ("table.remove")
		{
			public Object call (Object[] args)
			{
				LuaTable	t = table (arg (args, 0), "table.remove");
				int			n = t.length ();
				int			at = (int) num (args, 1, n);
				Object		old = t.get (at);

				for (int i = at; i < n; i++)	t.set (i, t.get (i + 1));
				t.set (n, null);
				return old;
			}
		});
		table.set ("getn", new LuaFunction ("table.getn")
		{
			public Object call (Object[] args)	{ return Double.valueOf (table (arg (args, 0), "table.getn").length ()); }
		});
		table.set ("concat", new LuaFunction ("table.concat")
		{
			public Object call (Object[] args)
			{
				LuaTable		t = table (arg (args, 0), "table.concat");
				String			sep = (str (args, 1) != null) ? str (args, 1) : "";
				StringBuffer	sb = new StringBuffer ();

				for (int i = 1; i <= t.length (); i++)
					sb.append ((i > 1) ? sep : "").append (Lua.tostring (t.get (i)));
				return sb.toString ();
			}
		});
		g.set ("table", table);

		/* ---- os (only the clock, which a behaviour may time itself with) ---- */

		LuaTable		os = new LuaTable ();

		os.set ("clock", new LuaFunction ("os.clock")
		{
			public Object call (Object[] args)	{ return Double.valueOf (System.currentTimeMillis () / 1000.0); }
		});
		os.set ("time", new LuaFunction ("os.time")
		{
			public Object call (Object[] args)	{ return Double.valueOf (System.currentTimeMillis () / 1000.0); }
		});
		g.set ("os", os);
	}

	/**
	 * A one-argument function of Math, by the name Lua gives it. The name is held in
	 * a variable of its own, as the function itself has one (and it is the long one).
	 */
	/**
	 * An angle in radians brought into -pi .. pi, which is where an angle is read
	 * from in a robot: half a turn either way and no more. A value that is not a
	 * number at all (an angle of an object that was never seen) is left as it is,
	 * as making one up would be worse.
	 */
	static public double normalised (double a)
	{
		if (!Double.isFinite (a))				return a;

		double		r = a % (2.0 * Math.PI);

		if (r > Math.PI)						r -= 2.0 * Math.PI;
		else if (r <= -Math.PI)					r += 2.0 * Math.PI;
		return r;
	}

	/**
	 * An angle in degrees brought into -180 .. 180, which is where an angle of the
	 * robot is read from. A value that is not a number at all is left as it is, as
	 * with the radians.
	 */
	static public double normalisedDegrees (double a)
	{
		if (!Double.isFinite (a))				return a;

		double		r = a % 360.0;

		if (r > 180.0)							r -= 360.0;
		else if (r <= -180.0)					r += 360.0;
		return r;
	}

	static private void one (LuaTable math, final String which)
	{
		math.set (which, new LuaFunction ("math." + which)
		{
			public Object call (Object[] args)
			{
				double		v = num (args, 0, 0.0);

				switch (which)
				{
				case "abs":		return Double.valueOf (Math.abs (v));
				case "sqrt":	return Double.valueOf (Math.sqrt (v));
				case "sin":		return Double.valueOf (Math.sin (v));
				case "cos":		return Double.valueOf (Math.cos (v));
				case "tan":		return Double.valueOf (Math.tan (v));
				case "asin":	return Double.valueOf (Math.asin (v));
				case "acos":	return Double.valueOf (Math.acos (v));
				case "exp":		return Double.valueOf (Math.exp (v));
				case "floor":	return Double.valueOf (Math.floor (v));
				case "ceil":	return Double.valueOf (Math.ceil (v));
				case "deg":		return Double.valueOf (Math.toDegrees (v));
				case "rad":		return Double.valueOf (Math.toRadians (v));
				}
				return null;
			}
		});
	}

	static private LuaTable table (Object v, String where)
	{
		if (v instanceof LuaTable)				return (LuaTable) v;
		throw new LuaError ("bad argument to '" + where + "' (table expected, got " + Lua.type (v) + ")");
	}
}

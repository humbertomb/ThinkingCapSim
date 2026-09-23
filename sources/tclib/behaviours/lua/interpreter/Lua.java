/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.lua.interpreter;

/**
 * What a Lua value is on the Java side, and the operations the language does on
 * them: nil is <code>null</code>, a boolean a {@link Boolean}, a number a
 * {@link Double}, a string a {@link String}, a table a {@link LuaTable} and a
 * function a {@link LuaFunction}.
 */
public class Lua
{
	/** Everything but nil and false is true, as in Lua. */
	static public boolean truth (Object v)
	{
		if (v == null)							return false;
		if (v instanceof Boolean)				return ((Boolean) v).booleanValue ();
		return true;
	}

	/** The name Lua gives to the type of a value. */
	static public String type (Object v)
	{
		if (v == null)							return "nil";
		if (v instanceof Boolean)				return "boolean";
		if (v instanceof Number)				return "number";
		if (v instanceof String)				return "string";
		if (v instanceof LuaTable)				return "table";
		if (v instanceof LuaFunction)			return "function";
		return "userdata";
	}

	/** The value as a number, or null when it is not one (a string that reads as a number is). */
	static public Double tonumber (Object v)
	{
		if (v instanceof Number)				return Double.valueOf (((Number) v).doubleValue ());
		if (v instanceof String)
			try { return Double.valueOf (Double.parseDouble (((String) v).trim ())); }
			catch (NumberFormatException e) { return null; }
		return null;
	}

	/** The value as a number, or an error naming what was expected. */
	static public double number (Object v, String what)
	{
		Double		d = tonumber (v);

		if (d == null)		throw new LuaError ("attempt to perform arithmetic on a " + type (v) + " value (" + what + ")");
		return d.doubleValue ();
	}

	/** The value as Lua writes it: a whole number without its decimals, nil as "nil". */
	static public String tostring (Object v)
	{
		if (v == null)							return "nil";
		if (v instanceof Boolean)				return ((Boolean) v).booleanValue () ? "true" : "false";
		if (v instanceof Number)				return number (((Number) v).doubleValue ());
		if (v instanceof String)				return (String) v;
		if (v instanceof LuaFunction)			return "function: " + ((LuaFunction) v).name ();
		return String.valueOf (v);
	}

	/** A number as Lua writes it (14 significant digits, no decimals when it is whole). */
	static public String number (double d)
	{
		if (Double.isNaN (d))					return "nan";
		if (Double.isInfinite (d))				return (d > 0) ? "inf" : "-inf";
		if ((d == Math.rint (d)) && (Math.abs (d) < 1e15))		return Long.toString ((long) d);
		return trim (String.format (java.util.Locale.US, "%.14g", Double.valueOf (d)));
	}

	static private String trim (String s)
	{
		if (s.indexOf ('.') < 0)				return s;
		int		e = s.indexOf ('e');
		if (e >= 0)								return s;
		while (s.endsWith ("0"))				s = s.substring (0, s.length () - 1);
		if (s.endsWith ("."))					s = s.substring (0, s.length () - 1);
		return s;
	}

	/* ---------------- operators ---------------- */

	static public Object add (Object a, Object b)		{ return Double.valueOf (number (a, "add") + number (b, "add")); }
	static public Object sub (Object a, Object b)		{ return Double.valueOf (number (a, "sub") - number (b, "sub")); }
	static public Object mul (Object a, Object b)		{ return Double.valueOf (number (a, "mul") * number (b, "mul")); }
	static public Object div (Object a, Object b)		{ return Double.valueOf (number (a, "div") / number (b, "div")); }
	static public Object pow (Object a, Object b)		{ return Double.valueOf (Math.pow (number (a, "pow"), number (b, "pow"))); }
	static public Object neg (Object a)					{ return Double.valueOf (-number (a, "unary -")); }

	/** Lua's modulo: the result has the sign of the divisor. */
	static public Object mod (Object a, Object b)
	{
		double		x = number (a, "mod"), y = number (b, "mod");

		return Double.valueOf (x - Math.floor (x / y) * y);
	}

	static public Object concat (Object a, Object b)
	{
		if (((a instanceof String) || (a instanceof Number)) && ((b instanceof String) || (b instanceof Number)))
			return tostring (a) + tostring (b);
		throw new LuaError ("attempt to concatenate a " + type ((a instanceof String) || (a instanceof Number) ? b : a) + " value");
	}

	/** Lua equality: numbers and strings by value, everything else by identity. */
	static public boolean eq (Object a, Object b)
	{
		if (a == b)								return true;
		if ((a == null) || (b == null))			return false;
		if ((a instanceof Number) && (b instanceof Number))
			return ((Number) a).doubleValue () == ((Number) b).doubleValue ();
		return a.equals (b);
	}

	/** Lua's <: numbers by value, strings alphabetically, anything else an error. */
	static public boolean lt (Object a, Object b)
	{
		if ((a instanceof Number) && (b instanceof Number))
			return ((Number) a).doubleValue () < ((Number) b).doubleValue ();
		if ((a instanceof String) && (b instanceof String))
			return ((String) a).compareTo ((String) b) < 0;
		throw new LuaError ("attempt to compare " + type (a) + " with " + type (b));
	}

	static public boolean le (Object a, Object b)
	{
		if ((a instanceof Number) && (b instanceof Number))
			return ((Number) a).doubleValue () <= ((Number) b).doubleValue ();
		if ((a instanceof String) && (b instanceof String))
			return ((String) a).compareTo ((String) b) <= 0;
		throw new LuaError ("attempt to compare " + type (a) + " with " + type (b));
	}

	static public Object len (Object a)
	{
		if (a instanceof String)				return Double.valueOf (((String) a).length ());
		if (a instanceof LuaTable)				return Double.valueOf (((LuaTable) a).length ());
		throw new LuaError ("attempt to get length of a " + type (a) + " value");
	}

	/* ---------------- tables ---------------- */

	static public Object index (Object t, Object k)
	{
		if (t instanceof LuaTable)				return ((LuaTable) t).get (k);
		if (t == null)							throw new LuaError ("attempt to index a nil value");
		throw new LuaError ("attempt to index a " + type (t) + " value");
	}

	static public void setIndex (Object t, Object k, Object v)
	{
		if (t instanceof LuaTable)				{ ((LuaTable) t).set (k, v); return; }
		if (t == null)							throw new LuaError ("attempt to index a nil value");
		throw new LuaError ("attempt to index a " + type (t) + " value");
	}
}

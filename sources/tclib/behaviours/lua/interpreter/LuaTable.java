/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.lua.interpreter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A Lua table: values by key, in the order they were first set, as both a record
 * (<code>t.x</code>) and an array (<code>t[1]</code>). Integral number keys are
 * kept as such, so that <code>t[1]</code> and <code>t[1.0]</code> are the same
 * place, as Lua has it.
 */
public class LuaTable
{
	protected Map<Object, Object>	fields	= new LinkedHashMap<Object, Object> ();

	public LuaTable ()						{ }

	/** A table with the given fields, given as name, value, name, value, ... */
	public LuaTable (Object... pairs)
	{
		for (int i = 0; (i + 1) < pairs.length; i += 2)
			set (pairs[i], pairs[i + 1]);
	}

	/** The key as the table keeps it: an integral number is kept as a Long. */
	static protected Object key (Object k)
	{
		if (k instanceof Number)
		{
			double	d = ((Number) k).doubleValue ();

			if ((d == Math.rint (d)) && !Double.isInfinite (d))		return Long.valueOf ((long) d);
			return Double.valueOf (d);
		}
		return k;
	}

	public Object get (Object k)
	{
		if (k == null)							return null;
		return fields.get (key (k));
	}

	public Object get (String k)				{ return fields.get (k); }
	public Object get (int k)					{ return fields.get (Long.valueOf (k)); }

	public void set (Object k, Object v)
	{
		if (k == null)							throw new LuaError ("table index is nil");
		if (v == null)							fields.remove (key (k));
		else									fields.put (key (k), v);
	}

	public void set (String k, Object v)		{ set ((Object) k, v); }
	public void set (int k, Object v)			{ set (Long.valueOf (k), v); }

	/** How long the array part is: the largest n with 1..n all set. */
	public int length ()
	{
		int		n = 0;

		while (fields.containsKey (Long.valueOf (n + 1)))		n++;
		return n;
	}

	public List<Object> keys ()					{ return new ArrayList<Object> (fields.keySet ()); }
	public Map<Object, Object> fields ()		{ return fields; }
	public void clear ()						{ fields.clear (); }

	public String toString ()
	{
		StringBuffer	sb = new StringBuffer ("{");
		boolean			first = true;

		for (Map.Entry<Object, Object> e : fields.entrySet ())
		{
			if (!first)		sb.append (", ");
			sb.append (e.getKey ()).append (" = ").append (Lua.tostring (e.getValue ()));
			first = false;
		}
		return sb.append ("}").toString ();
	}
}

/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.shared.world;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Map;

import wucore.utils.geom.Line2;
import wucore.utils.geom.Point2;
import wucore.utils.geom.Point3;

/**
 * Helpers for the JSON representation of the world (<code>.world</code>
 * files). Points are objects with <code>x, y, z</code>, segments with
 * <code>x1, y1, z1, x2, y2, z2</code>; angles are stored in degrees. Numbers
 * are rounded to {@link #DECIMALS} decimals when written.
 */
public final class WorldJson
{
	/** Decimals kept when writing coordinates and sizes. */
	static public final int			DECIMALS	= 4;

	static private final Gson		GSON		= new GsonBuilder ().disableHtmlEscaping ().serializeSpecialFloatingPointValues ().create ();

	private WorldJson ()			{ }

	/* ---- numbers ---- */

	/** Rounds a value to {@link #DECIMALS} decimals (integral values are written as integers). */
	static public Number num (double v)
	{
		double	f = Math.pow (10.0, DECIMALS);
		double	r = Math.round (v * f) / f;
		if (Double.isNaN (v) || Double.isInfinite (v))		return Double.valueOf (v);
		if ((r == Math.rint (r)) && (Math.abs (r) < 1e15))	return Long.valueOf ((long) r);
		return Double.valueOf (r);
	}

	static public double getDouble (JsonObject o, String key, double def)
	{
		JsonElement	e = o.get (key);
		return ((e != null) && e.isJsonPrimitive ()) ? e.getAsDouble () : def;
	}

	static public double getDouble (JsonObject o, String key)
	{
		JsonElement	e = o.get (key);
		if ((e == null) || !e.isJsonPrimitive ())
			throw new IllegalArgumentException ("World: missing number \"" + key + "\" in " + o);
		return e.getAsDouble ();
	}

	static public String getString (JsonObject o, String key, String def)
	{
		JsonElement	e = o.get (key);
		return ((e != null) && e.isJsonPrimitive ()) ? e.getAsString () : def;
	}

	static public boolean getBoolean (JsonObject o, String key, boolean def)
	{
		JsonElement	e = o.get (key);
		return ((e != null) && e.isJsonPrimitive ()) ? e.getAsBoolean () : def;
	}

	/** The array under a key, or an empty one when absent. */
	static public JsonArray getArray (JsonObject o, String key)
	{
		JsonElement	e = (o != null) ? o.get (key) : null;
		return ((e != null) && e.isJsonArray ()) ? e.getAsJsonArray () : new JsonArray ();
	}

	/** The object under a key, or an empty one when absent. */
	static public JsonObject getObject (JsonObject o, String key)
	{
		JsonElement	e = (o != null) ? o.get (key) : null;
		return ((e != null) && e.isJsonObject ()) ? e.getAsJsonObject () : new JsonObject ();
	}

	/* ---- geometry ---- */

	/** {x, y, z} */
	static public JsonObject point (double x, double y, double z)
	{
		JsonObject	o = new JsonObject ();
		o.addProperty ("x", num (x));
		o.addProperty ("y", num (y));
		o.addProperty ("z", num (z));
		return o;
	}

	static public JsonObject point (Point3 p)			{ return point (p.x (), p.y (), p.z ()); }
	static public JsonObject point (Point2 p)			{ return point (p.x (), p.y (), WMPath.z (p)); }

	/** Adds x, y, z to an existing object. */
	static public void putPoint (JsonObject o, double x, double y, double z)
	{
		o.addProperty ("x", num (x));
		o.addProperty ("y", num (y));
		o.addProperty ("z", num (z));
	}

	static public Point3 toPoint (JsonObject o)
	{
		return new Point3 (getDouble (o, "x"), getDouble (o, "y"), getDouble (o, "z", 0.0));
	}

	/** {x1, y1, z1, x2, y2, z2} */
	static public JsonObject line (Line2 l)
	{
		JsonObject	o = new JsonObject ();
		putLine (o, l);
		return o;
	}

	/** Adds x1..z2 to an existing object. */
	static public void putLine (JsonObject o, Line2 l)
	{
		o.addProperty ("x1", num (l.orig ().x ()));
		o.addProperty ("y1", num (l.orig ().y ()));
		o.addProperty ("z1", num (l.z1 ()));
		o.addProperty ("x2", num (l.dest ().x ()));
		o.addProperty ("y2", num (l.dest ().y ()));
		o.addProperty ("z2", num (l.z2 ()));
	}

	static public Line2 toLine (JsonObject o)
	{
		return new Line2 (getDouble (o, "x1"), getDouble (o, "y1"), getDouble (o, "z1", 0.0),
						  getDouble (o, "x2"), getDouble (o, "y2"), getDouble (o, "z2", 0.0));
	}

	/* ---- text ---- */

	/**
	 * Pretty-printed text with objects made only of primitives (points,
	 * segments, waypoints, ...) written on a single line, so that the file
	 * stays readable and compact.
	 */
	static public String toText (JsonElement e)
	{
		StringBuilder	sb = new StringBuilder ();
		write (sb, e, 0);
		sb.append ('\n');
		return sb.toString ();
	}

	static private boolean isLeaf (JsonElement e)
	{
		if (!e.isJsonObject ())		return false;
		for (Map.Entry<String, JsonElement> en : e.getAsJsonObject ().entrySet ())
			if (!en.getValue ().isJsonPrimitive () && !en.getValue ().isJsonNull ())		return false;
		return true;
	}

	static private void indent (StringBuilder sb, int level)
	{
		for (int i = 0; i < level; i++)		sb.append ("  ");
	}

	static private void write (StringBuilder sb, JsonElement e, int level)
	{
		if (e.isJsonObject ())
		{
			JsonObject	o = e.getAsJsonObject ();
			if (o.size () == 0)		{ sb.append ("{}"); return; }
			if (isLeaf (o))
			{
				sb.append ("{ ");
				boolean	first = true;
				for (Map.Entry<String, JsonElement> en : o.entrySet ())
				{
					if (!first)		sb.append (", ");
					first = false;
					sb.append (GSON.toJson (en.getKey ())).append (": ").append (GSON.toJson (en.getValue ()));
				}
				sb.append (" }");
				return;
			}
			sb.append ("{\n");
			boolean	first = true;
			for (Map.Entry<String, JsonElement> en : o.entrySet ())
			{
				if (!first)		sb.append (",\n");
				first = false;
				indent (sb, level + 1);
				sb.append (GSON.toJson (en.getKey ())).append (": ");
				write (sb, en.getValue (), level + 1);
			}
			sb.append ('\n');
			indent (sb, level);
			sb.append ('}');
		}
		else if (e.isJsonArray ())
		{
			JsonArray	a = e.getAsJsonArray ();
			if (a.size () == 0)		{ sb.append ("[]"); return; }
			sb.append ("[\n");
			for (int i = 0; i < a.size (); i++)
			{
				if (i > 0)		sb.append (",\n");
				indent (sb, level + 1);
				write (sb, a.get (i), level + 1);
			}
			sb.append ('\n');
			indent (sb, level);
			sb.append (']');
		}
		else
			sb.append (GSON.toJson (e));
	}

	static public JsonObject parse (String text)
	{
		JsonElement	e = JsonParser.parseString (text);
		if (!e.isJsonObject ())		throw new IllegalArgumentException ("World: the JSON text is not an object");
		return e.getAsJsonObject ();
	}
}

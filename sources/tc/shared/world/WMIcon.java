/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.shared.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.StringTokenizer;

import wucore.utils.geom.Line2;
import wucore.utils.geom.Point3;

/**
 * A 2D icon: a set of line segments expressed in local coordinates, relative
 * to a reference point (0, 0) and heading 0. Icons are shared: several
 * {@link WMObject} instances may reference the same icon by its label, each one
 * placing it at its own position and heading.
 *
 * File format (one property per icon):
 * <pre>
 *   ICON_i = label, n, x1, y1, x2, y2, ... (n segments, 4 numbers each)
 * </pre>
 */
public class WMIcon extends WMElement
{
	public Line2[]				lines;			// Segments in local coordinates (m)

	/* Constructors */

	public WMIcon ()
	{
		lines = new Line2[0];
	}

	public WMIcon (String label, Line2[] lines)
	{
		this.label	= label;
		this.lines	= (lines != null) ? lines : new Line2[0];
	}

	/** Parses "label, n, x1, y1, x2, y2, ..." */
	public WMIcon (String prop)
	{
		StringTokenizer		st = new StringTokenizer (prop, ", \t");

		label	= st.nextToken ();
		lines	= parseLines (st);
	}

	/** Parses "n, x1, y1, z1, x2, y2, z2, ..." from the current tokenizer position. */
	static public Line2[] parseLines (StringTokenizer st)
	{
		int			n = Integer.parseInt (st.nextToken ());
		Line2[]		lines = new Line2[n];
		for (int i = 0; i < n; i++)
		{
			double	x1 = Double.parseDouble (st.nextToken ());
			double	y1 = Double.parseDouble (st.nextToken ());
			double	z1 = Double.parseDouble (st.nextToken ());
			double	x2 = Double.parseDouble (st.nextToken ());
			double	y2 = Double.parseDouble (st.nextToken ());
			double	z2 = Double.parseDouble (st.nextToken ());
			lines[i] = new Line2 (x1, y1, z1, x2, y2, z2);
		}
		return lines;
	}

	/* Accessors */

	public final int		n ()				{ return lines.length; }

	/** Segments transformed to world coordinates for an instance at (pos, a). */
	public Line2[] toAbsolute (Point3 pos, double a)
	{
		double		c = Math.cos (a), s = Math.sin (a);
		double		px = (pos != null) ? pos.x () : 0.0, py = (pos != null) ? pos.y () : 0.0, pz = (pos != null) ? pos.z () : 0.0;
		Line2[]		abs = new Line2[lines.length];
		for (int i = 0; i < lines.length; i++)
		{
			Line2	l = lines[i];
			abs[i] = new Line2 (l.orig ().x () * c - l.orig ().y () * s + px, l.orig ().x () * s + l.orig ().y () * c + py, l.z1 () + pz,
								l.dest ().x () * c - l.dest ().y () * s + px, l.dest ().x () * s + l.dest ().y () * c + py, l.z2 () + pz);
		}
		return abs;
	}

	/** Local segments for a set of absolute ones of an instance at (pos, a). */
	static public Line2[] toLocal (Line2[] abs, Point3 pos, double a)
	{
		double		c = Math.cos (-a), s = Math.sin (-a);
		double		px = (pos != null) ? pos.x () : 0.0, py = (pos != null) ? pos.y () : 0.0, pz = (pos != null) ? pos.z () : 0.0;
		Line2[]		loc = new Line2[abs.length];
		for (int i = 0; i < abs.length; i++)
		{
			double	ox = abs[i].orig ().x () - px, oy = abs[i].orig ().y () - py;
			double	dx = abs[i].dest ().x () - px, dy = abs[i].dest ().y () - py;
			loc[i] = new Line2 (ox * c - oy * s, ox * s + oy * c, abs[i].z1 () - pz, dx * c - dy * s, dx * s + dy * c, abs[i].z2 () - pz);
		}
		return loc;
	}

	/** Radius of the bounding circle centred at the reference point. */
	public double radius ()
	{
		double		r = 0.0;
		for (Line2 l : lines)
		{
			r = Math.max (r, Math.hypot (l.orig ().x (), l.orig ().y ()));
			r = Math.max (r, Math.hypot (l.dest ().x (), l.dest ().y ()));
		}
		return r;
	}

	/** True if both icons have the same segments (same order, tolerance 1e-6). */
	public boolean sameGeometry (WMIcon other)
	{
		if ((other == null) || (other.lines.length != lines.length))		return false;
		for (int i = 0; i < lines.length; i++)
		{
			Line2	a = lines[i], b = other.lines[i];
			if ((Math.abs (a.orig ().x () - b.orig ().x ()) > 1e-6) || (Math.abs (a.orig ().y () - b.orig ().y ()) > 1e-6) || (Math.abs (a.z1 () - b.z1 ()) > 1e-6)
			 || (Math.abs (a.dest ().x () - b.dest ().x ()) > 1e-6) || (Math.abs (a.dest ().y () - b.dest ().y ()) > 1e-6) || (Math.abs (a.z2 () - b.z2 ()) > 1e-6))
				return false;
		}
		return true;
	}

	public WMIcon copy (String newLabel)
	{
		Line2[]		l = new Line2[lines.length];
		for (int i = 0; i < lines.length; i++)
			l[i] = new Line2 (lines[i].orig ().x (), lines[i].orig ().y (), lines[i].z1 (), lines[i].dest ().x (), lines[i].dest ().y (), lines[i].z2 ());
		return new WMIcon (newLabel, l);
	}

	/* JSON: {label, lines: [{x1, y1, z1, x2, y2, z2}, ...]} */

	public WMIcon (JsonObject o)
	{
		label	= World.getString (o, "label", "icon");
		JsonArray	arr = World.getArray (o, "lines");
		lines	= new Line2[arr.size ()];
		for (int i = 0; i < lines.length; i++)		lines[i] = World.toLine (arr.get (i).getAsJsonObject ());
	}

	public JsonObject toJson ()
	{
		JsonObject	o = new JsonObject ();
		JsonArray	arr = new JsonArray ();
		o.addProperty ("label", label);
		for (Line2 l : lines)		arr.add (World.line (l));
		o.add ("lines", arr);
		return o;
	}
}

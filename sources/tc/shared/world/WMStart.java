/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.shared.world;

import com.google.gson.JsonObject;

import java.util.StringTokenizer;

import wucore.utils.geom.Point3;

/**
 * A robot start point of the world: position (x, y, z) and orientation. A
 * world has at least one; in multi-robot simulations the i-th robot takes
 * the i-th start point (START_1, START_2, ...).
 */
public class WMStart
{
	public Point3		pos;
	public double		orientation;		// rad

	public WMStart (double x, double y, double z, double a)
	{
		pos			= new Point3 (x, y, z);
		orientation	= a;
	}

	/** Parses "x, y, z, angle(deg)" (or the older "x, y, angle" without z). */
	public WMStart (String prop)
	{
		StringTokenizer	st = new StringTokenizer (prop, ", \t");
		double	x = Double.parseDouble (st.nextToken ());
		double	y = Double.parseDouble (st.nextToken ());
		double	z = (st.countTokens () >= 2) ? Double.parseDouble (st.nextToken ()) : 0.0;
		double	a = st.hasMoreTokens () ? Math.toRadians (Double.parseDouble (st.nextToken ())) : 0.0;
		pos			= new Point3 (x, y, z);
		orientation	= a;
	}

	public double	x ()		{ return pos.x (); }
	public double	y ()		{ return pos.y (); }
	public double	z ()		{ return pos.z (); }

	public void set (double x, double y, double z, double a)
	{
		pos.set (x, y, z);
		orientation	= a;
	}

	/** Property value: "x, y, z, angle(deg)". */
	public String toProperty ()
	{
		return pos.x () + ", " + pos.y () + ", " + pos.z () + ", " + Math.toDegrees (orientation);
	}

	/* JSON: {x, y, z, orientation (deg)} */

	public WMStart (JsonObject o)
	{
		pos			= World.toPoint (o);
		orientation	= Math.toRadians (World.getDouble (o, "orientation", 0.0));
	}

	public JsonObject toJson ()
	{
		JsonObject	o = World.point (pos);
		o.addProperty ("orientation", World.num (Math.toDegrees (orientation)));
		return o;
	}
}

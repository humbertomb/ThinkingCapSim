/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.shared.world;

import com.google.gson.JsonObject;

import java.util.StringTokenizer;

import wucore.utils.geom.Point3;

/**
 * A robot start point of the world: position (x, y, z), orientation and how
 * wide it is drawn. A world has at least one; in multi-robot simulations the
 * i-th robot takes the i-th start point (START_1, START_2, ...).
 */
public class WMStart
{
	/** How wide a start point that says nothing of it is drawn (m): what they were all drawn as. */
	static public final double	RADIUS		= 0.25;

	public Point3		pos;
	public double		orientation;		// rad
	/**
	 * How wide it is drawn (m), which is as wide as the robot that starts there is.
	 * Nothing at all (0) is what the start points of every world said until now, and
	 * is drawn {@link #RADIUS} wide, as they were.
	 */
	public double		radius;

	/** How wide it is drawn: as wide as it says, or as wide as one that says nothing. */
	public double	radius ()	{ return (radius > 0.0) ? radius : RADIUS; }

	public WMStart (double x, double y, double z, double a)
	{
		this (x, y, z, a, 0.0);
	}

	public WMStart (double x, double y, double z, double a, double r)
	{
		pos			= new Point3 (x, y, z);
		orientation	= a;
		radius		= r;
	}

	/** Parses "x, y, z, angle(deg), radius" (radius and z may be missing, as the older "x, y, angle"). */
	public WMStart (String prop)
	{
		StringTokenizer	st = new StringTokenizer (prop, ", \t");
		double	x = Double.parseDouble (st.nextToken ());
		double	y = Double.parseDouble (st.nextToken ());
		double	z = (st.countTokens () >= 2) ? Double.parseDouble (st.nextToken ()) : 0.0;
		double	a = st.hasMoreTokens () ? Math.toRadians (Double.parseDouble (st.nextToken ())) : 0.0;
		double	r = st.hasMoreTokens () ? Double.parseDouble (st.nextToken ()) : 0.0;
		pos			= new Point3 (x, y, z);
		orientation	= a;
		radius		= r;
	}

	public double	x ()		{ return pos.x (); }
	public double	y ()		{ return pos.y (); }
	public double	z ()		{ return pos.z (); }

	public void set (double x, double y, double z, double a)
	{
		pos.set (x, y, z);
		orientation	= a;
	}

	/** Property value: "x, y, z, angle(deg)", and the radius after it when it says one. */
	public String toProperty ()
	{
		String	str = pos.x () + ", " + pos.y () + ", " + pos.z () + ", " + Math.toDegrees (orientation);

		return (radius > 0.0) ? (str + ", " + radius) : str;
	}

	/* JSON: {x, y, z, orientation (deg), radius} */

	public WMStart (JsonObject o)
	{
		pos			= World.toPoint (o);
		orientation	= Math.toRadians (World.getDouble (o, "orientation", 0.0));
		radius		= World.getDouble (o, "radius", 0.0);
	}

	public JsonObject toJson ()
	{
		JsonObject	o = World.point (pos);
		o.addProperty ("orientation", World.num (Math.toDegrees (orientation)));
		// one that says nothing of it is written saying nothing, as it was
		if (radius > 0.0)		o.addProperty ("radius", World.num (radius));
		return o;
	}
}

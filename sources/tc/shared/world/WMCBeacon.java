/*
 * Created on 08-feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import com.google.gson.JsonObject;


import wucore.utils.geom.Point3;

/**
 * Cylindrical (reflector) beacon: a vertical cylinder of a given diameter and
 * height whose base is at pos (x, y, z).
 *
 * @author Humberto Martinez Barbera
 */
public class WMCBeacon extends WMElement
{
	static public final double	DEF_DIAMETER	= 0.036;	// default diameter (m)
	static public final double	DEF_HEIGHT		= 0.5;		// default height (m)

	public Point3			pos;						// Centre of the base
	public double			diameter	= DEF_DIAMETER;
	public double			height		= DEF_HEIGHT;

	public WMCBeacon(double x, double y, double z, double diameter, double height, String label){
		this.pos		= new Point3 (x, y, z);
		this.diameter	= diameter;
		this.height		= height;
		this.label		= label;
	}

	public double x ()			{ return pos.x (); }
	public double y ()			{ return pos.y (); }
	public double z ()			{ return pos.z (); }
	public double radius ()		{ return diameter / 2.0; }

	/* JSON: {label, x, y, z, diameter, height} */

	public WMCBeacon (JsonObject o)
	{
		label		= World.getString (o, "label", "cb");
		pos			= World.toPoint (o);
		diameter	= World.getDouble (o, "diameter", DEF_DIAMETER);
		height		= World.getDouble (o, "height", DEF_HEIGHT);
	}

	public JsonObject toJson ()
	{
		JsonObject	o = new JsonObject ();
		o.addProperty ("label", label);
		World.putPoint (o, pos.x (), pos.y (), pos.z ());
		o.addProperty ("diameter", World.num (diameter));
		o.addProperty ("height", World.num (height));
		return o;
	}
}

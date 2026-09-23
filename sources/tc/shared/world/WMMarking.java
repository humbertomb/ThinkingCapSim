/*
 * (c) 2026 Humberto Martinez Barbera
 */
package tc.shared.world;

import com.google.gson.JsonObject;

import wucore.utils.color.ColorTool;
import wucore.utils.color.WColor;
import wucore.utils.geom.Line2;

/**
 * A marking of the world: a line on the floor, drawn with a colour and a width,
 * as the lines that mark a court or the lanes of a warehouse are. It is only a
 * visual guide: the simulation does not take it for an obstacle, although a
 * camera of a robot does see it.
 */
public class WMMarking extends WMElement
{
	// 2D components
	public Line2					edge;						// the line, with the elevation of its ends (m)

	// Visualization components
	public WColor					color;
	public double					width;						// how wide the line is drawn (m)

	public WMMarking ()
	{
		edge	= new Line2 ();
		color	= WColor.WHITE;
		width	= 0.05;
	}

	/* JSON: {x1, y1, z1, x2, y2, z2 [, color, width]} (missing values take the collection defaults) */

	public WMMarking (JsonObject o, WColor dcolor, double dwidth)
	{
		String		cname = World.getString (o, "color", null);

		edge	= World.toLine (o);
		color	= (cname != null) ? ColorTool.getColorFromName (cname) : dcolor;
		width	= World.getDouble (o, "width", dwidth);
	}

	/** Colour and width are only written when they differ from the defaults. */
	public JsonObject toJson (WColor dcolor, double dwidth)
	{
		JsonObject	o = World.line (edge);

		if ((color != null) && !color.equals (dcolor))		o.addProperty ("color", ColorTool.getNameFromColor (color));
		if (width != dwidth)								o.addProperty ("width", World.num (width));
		return o;
	}
}
